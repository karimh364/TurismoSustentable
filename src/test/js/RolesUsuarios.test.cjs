const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const carpeta = path.join(__dirname, '../../main/resources/static');
const fuente = fs.readFileSync(path.join(carpeta, 'js/roles-usuarios.js'), 'utf8');

function entorno() {
    const elementos = new Map(), peticiones = [], avisos = [], sesiones = [], redirecciones = [];
    const eventos = {};
    const campo = id => {
        if (!elementos.has(id)) elementos.set(id, {
            value: '', textContent: '', checked: false, disabled: false, atributos: new Map(), eventos: {},
            setAttribute(k, v) { this.atributos.set(k, v); }, removeAttribute(k) { this.atributos.delete(k); },
            addEventListener(k, f) { this.eventos[k] = f; }, focus() {}
        });
        return elementos.get(id);
    };
    const vista = {usuarioId: 42, nickname: 'roles.qa', version: 3, roles: ['CLIENTE']};
    let get = async () => ({data: {...vista}});
    let put = async (url, data) => ({data: {...vista, version: 4, roles: data.roles}});
    let perfil = async () => ({id: 17, roles: ['ADMIN']});
    let recarga = async () => true;
    let recargas = 0, perfiles = 0;
    const modal = {
        visible: false, aperturas: 0, cierres: 0,
        show() { this.visible = true; this.aperturas++; campo('modalRolesUsuario').eventos['shown.bs.modal']?.(); },
        hide() {
            if (!this.visible) return;
            let rechazado = false;
            campo('modalRolesUsuario').eventos['hide.bs.modal']?.({preventDefault() { rechazado = true; }});
            if (!rechazado) { this.visible = false; this.cierres++; }
        }
    };
    const contexto = vm.createContext({
        console, document: {getElementById: campo, addEventListener(k, f) { eventos[k] = f; }},
        axios: {
            async get(url) { peticiones.push({metodo: 'GET', url}); return get(url); },
            async put(url, data) { peticiones.push({metodo: 'PUT', url, data}); return put(url, data); }
        },
        bootstrap: {Modal: {getOrCreateInstance: () => modal}},
        Swal: {async fire(datos) { avisos.push(datos); }},
        obtenerUsuarioActual: async () => { perfiles++; return perfil(); },
        rolesDelUsuario: u => u.roles,
        cargarUsuarios: async () => { recargas++; return recarga(); },
        manejarErrorSesion: e => {
            if ([401, 403].includes(e.response?.status)) { sesiones.push(e); return true; }
            return false;
        },
        volverAlIngreso: () => redirecciones.push('login'),
        window: {location: {replace: url => redirecciones.push(url)}}
    });
    vm.runInContext(fuente, contexto, {filename: 'roles-usuarios.js'});
    eventos.DOMContentLoaded();
    return {contexto, campo, vista, modal, peticiones, avisos, sesiones, redirecciones,
        get recargas() { return recargas; }, get perfiles() { return perfiles; },
        setGet: f => get = f, setPut: f => put = f, setPerfil: f => perfil = f, setRecarga: f => recarga = f};
}
async function abrir(e) { await e.contexto.abrirRolesUsuario(e.vista.usuarioId); }
function seleccionar(e, roles, motivo = ' Cambio de funciones ') {
    for (const codigo of ['admin', 'cliente', 'tecnico']) e.campo('roles-' + codigo).checked = roles.includes(codigo.toUpperCase());
    e.campo('roles-motivo').value = motivo;
}
const puts = e => e.peticiones.filter(p => p.metodo === 'PUT');

test('carga roles/version desde el endpoint y muestra nickname como texto', async () => {
    const e = entorno(); e.vista.nickname = '<img src=x onerror=alert(1)>';
    await abrir(e);
    assert.equal(e.peticiones[0].url, '/api/usuarios/42/roles');
    assert.equal(e.campo('roles-cliente').checked, true); assert.equal(e.campo('roles-admin').checked, false);
    assert.equal(e.campo('roles-titulo').textContent, 'Roles de ' + e.vista.nickname);
    assert.equal(e.campo('roles-titulo').innerHTML, undefined);
    assert.equal(e.modal.aperturas, 1); assert.equal(e.campo('roles-guardar').disabled, false);
});
test('guarda roles motivo y versión sin actor ni datos personales y refresca desde servidor', async () => {
    const e = entorno(); await abrir(e); seleccionar(e, ['CLIENTE', 'TECNICO']);
    await e.contexto.guardarRolesUsuario();
    assert.deepEqual(JSON.parse(JSON.stringify(puts(e))), [{metodo: 'PUT', url: '/api/usuarios/42/roles',
        data: {version: 3, roles: ['CLIENTE', 'TECNICO'], motivo: 'Cambio de funciones'}}]);
    assert.equal(e.recargas, 1); assert.equal(e.perfiles, 1); assert.equal(e.modal.cierres, 1);
    assert.equal(e.avisos.at(-1).icon, 'success');
});
test('sin roles o motivo no envía PUT', async () => {
    const e = entorno(); await abrir(e);
    seleccionar(e, []); await e.contexto.guardarRolesUsuario();
    seleccionar(e, ['CLIENTE'], ' '); await e.contexto.guardarRolesUsuario();
    assert.equal(puts(e).length, 0); assert.equal(e.modal.cierres, 0);
});
test('una composición ya existente se informa sin inventar un cambio adicional', async () => {
    const e = entorno(); await abrir(e); seleccionar(e, ['CLIENTE']);
    e.setPut(async () => ({data: {...e.vista}})); await e.contexto.guardarRolesUsuario();
    assert.equal(e.avisos.at(-1).title, 'Sin cambios en los roles');
    assert.match(e.avisos.at(-1).text, /No se generó/);
});
test('cancelar descarta el contexto y no escribe', async () => {
    const e = entorno(); await abrir(e); seleccionar(e, ['ADMIN']); e.modal.hide();
    await e.contexto.guardarRolesUsuario(); assert.equal(puts(e).length, 0);
});
test('409 conserva selección y motivo sin éxito ni reintento automático', async () => {
    const e = entorno(); await abrir(e); seleccionar(e, ['ADMIN']);
    e.setPut(async () => { throw {response: {status: 409, data: 'Recargá los roles.'}}; });
    await e.contexto.guardarRolesUsuario();
    assert.equal(puts(e).length, 1); assert.equal(e.avisos.at(-1).text, 'Recargá los roles.');
    assert.equal(e.avisos.at(-1).icon, 'error'); assert.equal(e.modal.cierres, 0);
    assert.equal(e.campo('roles-admin').checked, true); assert.equal(e.recargas, 0);
    assert.equal(e.campo('roles-motivo').value, ' Cambio de funciones ');
});
test('401/403 se delegan al control de sesión sin éxito falso', async () => {
    for (const status of [401, 403]) {
        const e = entorno(); await abrir(e); seleccionar(e, ['ADMIN']);
        e.setPut(async () => { throw {response: {status}}; });
        await e.contexto.guardarRolesUsuario();
        assert.equal(e.sesiones.length, 1); assert.equal(e.avisos.length, 0); assert.equal(e.recargas, 0);
    }
});
test('doble clic no repite PUT y no permite cerrar mientras guarda', async () => {
    const e = entorno(); await abrir(e); seleccionar(e, ['CLIENTE', 'ADMIN']);
    let resolver; e.setPut(() => new Promise(r => { resolver = r; }));
    const primero = e.contexto.guardarRolesUsuario(); await e.contexto.guardarRolesUsuario();
    e.modal.hide(); assert.equal(e.modal.visible, true); assert.equal(puts(e).length, 1);
    assert.equal(e.campo('roles-guardar').disabled, true);
    resolver({data: {...e.vista, version: 4, roles: ['ADMIN', 'CLIENTE']}}); await primero;
    assert.equal(e.modal.visible, false); assert.equal(e.campo('roles-guardar').disabled, false);
});
test('retirar ADMIN propio vuelve al inicio sin consultar otra ruta administrativa ni logout', async () => {
    const e = entorno(); e.vista.usuarioId = 17; e.vista.roles = ['ADMIN', 'CLIENTE'];
    await abrir(e); seleccionar(e, ['CLIENTE']);
    e.setPerfil(async () => ({id: 17, roles: ['CLIENTE']}));
    await e.contexto.guardarRolesUsuario();
    assert.deepEqual(e.redirecciones, ['index.html']); assert.equal(e.recargas, 0);
    assert.match(e.avisos.at(-1).text, /sesión sigue activa/);
    assert.equal(e.peticiones.filter(p => p.url.includes('logout')).length, 0);
});
test('fallo de consulta después del PUT distingue guardado confirmado de pantalla desactualizada', async () => {
    const e = entorno(); await abrir(e); seleccionar(e, ['CLIENTE', 'TECNICO']);
    e.setPerfil(async () => { throw {response: {status: 503}}; });
    await e.contexto.guardarRolesUsuario();
    assert.equal(e.avisos.at(-1).icon, 'warning'); assert.match(e.avisos.at(-1).text, /servidor aceptó/);
    await e.contexto.guardarRolesUsuario(); assert.equal(puts(e).length, 1);
});
test('fallo del listado después del guardado no afirma que el PUT falló', async () => {
    const e = entorno(); await abrir(e); seleccionar(e, ['CLIENTE', 'TECNICO']);
    e.setRecarga(async () => false); await e.contexto.guardarRolesUsuario();
    assert.equal(e.avisos.at(-1).icon, 'warning'); assert.match(e.avisos.at(-1).text, /cambios se guardaron/);
});
test('red caída durante PUT no afirma ni éxito ni ausencia de escritura', async () => {
    const e = entorno(); await abrir(e); seleccionar(e, ['CLIENTE', 'TECNICO']);
    e.setPut(async () => { throw {request: {}}; }); await e.contexto.guardarRolesUsuario();
    assert.match(e.avisos.at(-1).text, /No se pudo confirmar/);
    assert.doesNotMatch(e.campo('roles-estado').textContent, /No se guardaron/);
    assert.equal(e.sesiones.length, 0); assert.equal(puts(e).length, 1);
});
test('no abre un formulario con versión ausente ni datos de otro usuario', async () => {
    for (const datos of [{usuarioId: 42, roles: ['CLIENTE'], nickname: 'qa'},
            {usuarioId: 99, roles: ['CLIENTE'], nickname: 'qa', version: 0}]) {
        const e = entorno(); e.setGet(async () => ({data: datos})); await abrir(e);
        assert.equal(e.modal.aperturas, 0); assert.equal(e.avisos.at(-1).icon, 'error');
        await e.contexto.guardarRolesUsuario(); assert.equal(puts(e).length, 0);
    }
});
test('dos clics durante la carga comparten la acción sin abrir dos diálogos', async () => {
    const e = entorno(); let resolver; e.setGet(() => new Promise(r => { resolver = r; }));
    const carga = abrir(e); await abrir(e); assert.equal(e.peticiones.length, 1);
    resolver({data: e.vista}); await carga; assert.equal(e.modal.aperturas, 1);
});
test('HTML carga módulo antes del consumidor y conecta diálogo/botón sin edición libre de entidades', () => {
    const html = fs.readFileSync(path.join(carpeta, 'admin.html'), 'utf8');
    const admin = fs.readFileSync(path.join(carpeta, 'js/admin.js'), 'utf8');
    assert.equal((html.match(/src="js\/roles-usuarios.js"/g) || []).length, 1);
    assert.ok(html.indexOf('js/roles-usuarios.js') < html.indexOf('js/admin.js'));
    assert.match(admin, /onclick="abrirRolesUsuario\(/);
    for (const id of ['modalRolesUsuario', 'roles-opciones', 'roles-cliente', 'roles-admin', 'roles-tecnico', 'roles-motivo']) {
        assert.equal((html.match(new RegExp(`id="${id}"`, 'g')) || []).length, 1);
    }
    assert.match(html, /onclick="guardarRolesUsuario\(\)"/);
});
