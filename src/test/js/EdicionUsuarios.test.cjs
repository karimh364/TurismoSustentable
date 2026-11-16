const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const fuente = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/admin.js'), 'utf8');

function entorno() {
    const campos = new Map();
    function campo(id) {
        if (!campos.has(id)) {
            let valor = '', html = '';
            campos.set(id, {
                id, disabled: false, options: [],
                get value() { return valor; },
                set value(v) { valor = String(v ?? ''); },
                get innerHTML() { return html; },
                set innerHTML(v) { html = String(v); this.options = []; },
                add(opcion) { this.options.push(opcion); },
                addEventListener() {}
            });
        }
        return campos.get(id);
    }
    const datos = {
        usuario: {id: 2, nombre: 'Técnico', apellido: 'Circuito', email: 'qa.tecnico@example.com',
            genero: 'FEMENINO', fechaNacimiento: '1990-01-01', roles: ['TECNICO'], activo: true},
        // IDs deliberadamente no secuenciales: no deben deducirse de los códigos.
        generos: [{id: 42, codigo: 'FEMENINO', nombre: 'Femenino'},
            {id: 7, codigo: 'MASCULINO', nombre: 'Masculino'},
            {id: 99, codigo: 'PREFIERO_NO_DECIR', nombre: 'Prefiero no decir'}]
    };
    const peticiones = [], avisos = [], sesiones = [];
    const modal = {mostrado: 0, ocultado: 0,
        show() { this.mostrado++; }, hide() { this.ocultado++; }};
    let get = async url => {
        if (url === '/api/usuarios/2') return {data: {...datos.usuario}};
        if (url === '/api/catalogos/generos') return {data: datos.generos};
        if (url === '/api/usuarios') return {data: [{...datos.usuario}]};
        throw new Error('GET inesperado: ' + url);
    };
    let put = async () => ({data: {}});
    const contexto = vm.createContext({
        console: {error() {}},
        document: {addEventListener() {}, getElementById: campo},
        Option: class {
            constructor(texto, valor) {
                this.text = texto; this.value = String(valor); this.disabled = false;
            }
        },
        axios: {
            async get(url) { peticiones.push({metodo: 'GET', url}); return get(url); },
            async put(url, data) { peticiones.push({metodo: 'PUT', url, data}); return put(url, data); }
        },
        bootstrap: {Modal: {getOrCreateInstance: () => modal, getInstance: () => modal}},
        Swal: {async fire(opciones) { avisos.push(opciones); return {isConfirmed: true}; }},
        manejarErrorSesion(error) {
            if ([401, 403].includes(error.response?.status)) { sesiones.push(error); return true; }
            return false;
        }
    });
    vm.runInContext(fuente, contexto, {filename: 'admin.js'});
    return {contexto, campo, datos, peticiones, avisos, sesiones, modal,
        setGet: f => get = f, setPut: f => put = f};
}

test('edición carga catálogo real y selecciona el ID correspondiente al código recibido', async () => {
    const e = entorno();
    await e.contexto.editarUsuario(2);
    assert.equal(e.campo('user-genero').value, '42');
    assert.equal(e.campo('user-genero').options.find(o => o.value === '42').text, 'Femenino');
    assert.equal(e.campo('user-fecha').value, '1990-01-01');
    assert.equal(e.modal.mostrado, 1);
    assert.ok(e.peticiones.some(p => p.url === '/api/catalogos/generos'));
});

test('el selector conserva también Prefiero no decir con su ID del catálogo', async () => {
    const e = entorno();
    e.datos.usuario.genero = 'PREFIERO_NO_DECIR';
    await e.contexto.editarUsuario(2);
    assert.equal(e.campo('user-genero').value, '99');
});

test('guardar envía los cinco campos del DTO con generoId numérico y no el código anterior', async () => {
    const e = entorno();
    await e.contexto.editarUsuario(2);
    e.campo('user-apellido').value = 'Circuito QA';
    await e.contexto.guardarUsuario();
    const envio = e.peticiones.find(p => p.metodo === 'PUT');
    assert.equal(envio.url, '/api/usuarios/2');
    assert.deepEqual(JSON.parse(JSON.stringify(envio.data)), {
        nombre: 'Técnico', apellido: 'Circuito QA', email: 'qa.tecnico@example.com',
        generoId: 42, fechaNacimiento: '1990-01-01'
    });
    assert.equal(e.modal.ocultado, 1);
    assert.equal(e.avisos.at(-1).icon, 'success');
});

test('catálogo vacío no abre un formulario incompleto ni permite un PUT con datos anteriores', async () => {
    const e = entorno();
    e.datos.generos = [];
    await e.contexto.editarUsuario(2);
    assert.equal(e.modal.mostrado, 0);
    assert.match(e.avisos[0].text, /opciones de género/);
    await e.contexto.guardarUsuario();
    assert.equal(e.peticiones.filter(p => p.metodo === 'PUT').length, 0);
});

test('género actual ausente del catálogo no se sustituye silenciosamente por otro', async () => {
    const e = entorno();
    e.datos.usuario.genero = 'NO_EXISTE';
    await e.contexto.editarUsuario(2);
    assert.equal(e.modal.mostrado, 0);
    assert.match(e.avisos[0].text, /género del usuario no está disponible/);
});

test('campos obligatorios vacíos no envían una actualización parcial', async () => {
    for (const id of ['user-nombre', 'user-apellido', 'user-email', 'user-genero', 'user-fecha']) {
        const e = entorno();
        await e.contexto.editarUsuario(2);
        e.campo(id).value = '';
        await e.contexto.guardarUsuario();
        assert.equal(e.peticiones.filter(p => p.metodo === 'PUT').length, 0, id);
        assert.equal(e.modal.ocultado, 0, id);
        assert.equal(e.avisos.at(-1).icon, 'error', id);
        assert.equal(e.campo('user-guardar').disabled, false, id);
    }
});

test('errores del backend se muestran con etiquetas legibles y como texto, conservando el formulario', async () => {
    const e = entorno();
    await e.contexto.editarUsuario(2);
    e.setPut(async () => { throw {response: {status: 400, data: {validationErrors: [
        'generoId: El género seleccionado no existe.',
        'apellido: No se admite <img src=x onerror=alert(1)>'
    ]}}}; });
    await e.contexto.guardarUsuario();
    const aviso = e.avisos.at(-1);
    assert.match(aviso.text, /Género: El género seleccionado no existe/);
    assert.match(aviso.text, /Apellido:/);
    assert.doesNotMatch(aviso.text, /generoId:/);
    assert.equal(aviso.html, undefined);
    assert.equal(e.modal.ocultado, 0);
    assert.equal(e.campo('user-apellido').value, 'Circuito');
});

test('409 informa el motivo y no oculta el formulario, refresca ni anuncia éxito', async () => {
    const e = entorno();
    await e.contexto.editarUsuario(2);
    e.setPut(async () => { throw {response: {status: 409, data: 'El usuario fue modificado. Recargá los datos.'}}; });
    await e.contexto.guardarUsuario();
    assert.equal(e.avisos.at(-1).icon, 'error');
    assert.match(e.avisos.at(-1).text, /usuario fue modificado/);
    assert.equal(e.modal.ocultado, 0);
    assert.equal(e.peticiones.filter(p => p.url === '/api/usuarios').length, 0);
});

test('401 y 403 del PUT se delegan al manejador de sesión sin mensajes de éxito', async () => {
    for (const estado of [401, 403]) {
        const e = entorno();
        await e.contexto.editarUsuario(2);
        e.setPut(async () => { throw {response: {status: estado}}; });
        await e.contexto.guardarUsuario();
        assert.equal(e.sesiones.length, 1);
        assert.equal(e.avisos.length, 0);
        assert.equal(e.modal.ocultado, 0);
    }
});

test('doble clic mientras se guarda produce un único PUT y libera el botón al terminar', async () => {
    const e = entorno();
    await e.contexto.editarUsuario(2);
    let terminar;
    e.setPut(() => new Promise(resolve => { terminar = resolve; }));
    const primero = e.contexto.guardarUsuario();
    await e.contexto.guardarUsuario();
    assert.equal(e.peticiones.filter(p => p.metodo === 'PUT').length, 1);
    assert.equal(e.campo('user-guardar').disabled, true);
    terminar({data: {}});
    await primero;
    assert.equal(e.campo('user-guardar').disabled, false);
});

test('guardado confirmado con recarga fallida se distingue de un PUT rechazado', async () => {
    const e = entorno();
    await e.contexto.editarUsuario(2);
    e.setGet(async () => { throw {response: {status: 500}}; });
    await e.contexto.guardarUsuario();
    assert.equal(e.modal.ocultado, 1);
    assert.equal(e.avisos.at(-1).icon, 'warning');
    assert.match(e.avisos.at(-1).text, /Se guardaron los cambios/);
    assert.equal(e.peticiones.filter(p => p.metodo === 'PUT').length, 1);
});

test('401 al refrescar después del PUT no duplica los mensajes del manejador de sesión', async () => {
    const e = entorno();
    await e.contexto.editarUsuario(2);
    e.setGet(async () => { throw {response: {status: 401}}; });
    await e.contexto.guardarUsuario();
    assert.equal(e.sesiones.length, 1);
    assert.equal(e.avisos.length, 0);
});

test('HTML usa catálogo dinámico, etiquetas asociadas y el botón de guardado identificado', () => {
    const html = fs.readFileSync(path.join(__dirname, '../../main/resources/static/admin.html'), 'utf8');
    const selector = html.match(/<select[^>]*id="user-genero"[^>]*>([\s\S]*?)<\/select>/);
    assert.ok(selector);
    assert.doesNotMatch(selector[1], /value="(?:FEMENINO|MASCULINO|NO_BINARIO|OTRO|PREFIERO_NO_DECIR)"/);
    assert.match(html, /for="user-genero"/);
    assert.match(html, /id="user-guardar"/);
});
