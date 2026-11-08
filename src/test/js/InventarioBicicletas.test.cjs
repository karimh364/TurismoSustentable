// node --test src/test/js/InventarioBicicletas.test.cjs
// Ejecuta el admin.js real con DOM y HTTP simulados. No abre navegador ni usa MySQL.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const fuente = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/admin.js'), 'utf8');

function entorno() {
    const elementos = new Map();
    function nodo(id = '') {
        let valor = '';
        const clases = new Set();
        return {
            id, options: [], children: [], rows: [], cells: [], eventos: {},
            checked: false, disabled: ['bici-estado', 'bici-precio'].includes(id),
            required: false, textContent: '', innerHTML: '',
            get value() { return valor; }, set value(v) { valor = String(v); },
            replaceChildren(...opciones) {
                this.children = [...opciones]; this.options = [...opciones]; this.rows = [];
                this.value = (opciones.find(o => o.selected) || opciones[0])?.value ?? '';
            },
            add(opcion) {
                this.options.push(opcion);
                if (this.options.length === 1 || opcion.selected) this.value = opcion.value;
            },
            append(hijo) { this.children.push(hijo); },
            insertRow() { const fila = nodo(); this.rows.push(fila); return fila; },
            insertCell() { const celda = nodo(); this.cells.push(celda); return celda; },
            addEventListener(nombre, accion) { this.eventos[nombre] = accion; },
            classList: {
                add(c) { clases.add(c); },
                toggle(c, activo) { if (activo) clases.add(c); else clases.delete(c); },
                contains(c) { return clases.has(c); }
            }
        };
    }
    function campo(id) {
        if (!elementos.has(id)) elementos.set(id, nodo(id));
        return elementos.get(id);
    }
    const datos = {
        bicicleta: { id: 7, codigoBicicleta: 'QA-01', tipo: 'MONTANA', tipoBicicletaId: 1,
            estado: 'DISPONIBLE', estacionId: 2, ubicacion: 'Taller QA', precio: 10,
            version: 3, borradoBici: false },
        eliminada: { id: 8, codigoBicicleta: 'QA-ELIMINADA', tipo: 'MONTANA', tipoBicicletaId: 1,
            estado: 'DE_BAJA', estacionId: null, ubicacion: 'Sin Asignar', precio: 10,
            version: 4, borradoBici: true },
        estaciones: [
            { id: 1, nombre: 'Pública llena', esPublica: true, espaciosLibres: 0 },
            { id: 2, nombre: 'Taller QA', esPublica: false, espaciosLibres: 0 },
            { id: 3, nombre: 'Pública con lugar', esPublica: true, espaciosLibres: 2 }
        ]
    };
    const peticiones = [], avisos = [], respuestas = [];
    const contexto = vm.createContext({
        document: { addEventListener() {}, getElementById: campo, createElement: () => nodo() },
        Option: class {
            constructor(text, value, inicial, selected) {
                this.text = text; this.value = String(value); this.selected = !!selected; this.disabled = false;
            }
        },
        axios: {
            async get(url, opciones) {
                peticiones.push({ metodo: 'GET', url, opciones });
                if (url === '/api/admin/tipos-bicicleta') {
                    return { data: [{ id: 1, codigo: 'MONTANA', precioPorHoraUsd: 10 }] };
                }
                if (url === '/api/admin/estaciones') return { data: datos.estaciones };
                if (url === '/api/bicicletas/7') return { data: { ...datos.bicicleta } };
                if (url === '/api/bicicletas') {
                    return { data: opciones.params.incluirEliminadas
                        ? [datos.bicicleta, datos.eliminada] : [datos.bicicleta] };
                }
                throw new Error('GET inesperado: ' + url);
            },
            async delete(url, opciones) { peticiones.push({ metodo: 'DELETE', url, opciones }); },
            async patch(url, data) { peticiones.push({ metodo: 'PATCH', url, data }); },
            async post(url, data) { peticiones.push({ metodo: 'POST', url, data }); },
            async put(url, data) { peticiones.push({ metodo: 'PUT', url, data }); }
        },
        Swal: { async fire(...args) { avisos.push(args); return respuestas.shift() || { isConfirmed: false }; } },
        bootstrap: { Modal: { getOrCreateInstance: () => ({ show() {} }), getInstance: () => ({ hide() {} }) } },
        manejarErrorSesion: () => false
    });
    vm.runInContext(fuente, contexto, { filename: 'admin.js' });
    // Aislamos el repintado de estaciones: su backend/HTML tiene su propia suite.
    contexto.cargarEstaciones = async () => { await contexto.axios.get('/api/admin/estaciones'); };
    return { contexto, campo, datos, peticiones, avisos, respuestas };
}

test('alta muestra espacios y bloquea estaciones llenas, públicas o internas', async () => {
    const e = entorno();
    await e.contexto.abrirModalBicicleta();
    const select = e.campo('bici-estacion-select');
    assert.equal(select.value, '');
    assert.equal(select.options[0].text, 'Seleccioná una estación');
    assert.equal(select.options[0].disabled, true);
    assert.equal(select.options.find(o => o.value === '1').disabled, true);
    assert.equal(select.options.find(o => o.value === '2').disabled, true);
    const libre = select.options.find(o => o.value === '3');
    assert.equal(libre.disabled, false);
    assert.ok(libre.text.includes('Pública'));
    assert.ok(libre.text.includes('2 espacios libres'));
});

test('edición conserva seleccionada y habilitada la ubicación actual aunque esté llena', async () => {
    const e = entorno();
    await e.contexto.editarBici(7);
    const select = e.campo('bici-estacion-select');
    assert.equal(select.value, '2');
    assert.equal(select.options.length, 3);
    assert.ok(select.options.every(o => o.value !== ''));
    const actual = select.options.find(o => o.value === '2');
    assert.equal(actual.disabled, false);
    assert.ok(actual.text.includes('0 espacios libres'));
    assert.ok(actual.text.includes('Actual'));
    assert.equal(select.options.find(o => o.value === '1').disabled, true);
    assert.equal(e.campo('bici-motivo').disabled, true);
});

test('disponibilidad no informada no habilita un destino, pero permite conservar el actual', async () => {
    const e = entorno();
    e.datos.estaciones[2].espaciosLibres = undefined;
    await e.contexto.editarBici(7);
    assert.equal(e.campo('bici-estacion-select').options.find(o => o.value === '3').disabled, true);
    e.datos.estaciones[1].espaciosLibres = undefined;
    await e.contexto.editarBici(7);
    assert.equal(e.campo('bici-estacion-select').options.find(o => o.value === '2').disabled, false);
});

test('motivo aparece solo al elegir otro destino y se limpia al volver al actual', async () => {
    const e = entorno();
    await e.contexto.editarBici(7);
    e.campo('bici-estacion-select').value = 3;
    e.contexto.actualizarMotivoTraslado();
    assert.equal(e.campo('bici-motivo').required, true);
    assert.equal(e.campo('bici-motivo-grupo').classList.contains('d-none'), false);
    e.campo('bici-motivo').value = 'Traslado QA';
    e.campo('bici-estacion-select').value = 2;
    e.contexto.actualizarMotivoTraslado();
    assert.equal(e.campo('bici-motivo').disabled, true);
    assert.equal(e.campo('bici-motivo').value, '');
});

test('alta sin estación no envía POST y no muestra motivo de traslado', async () => {
    const e = entorno();
    await e.contexto.abrirModalBicicleta();
    e.campo('bici-codigo').value = 'QA-NUEVA'; e.campo('bici-tipo').value = 1;
    await e.contexto.guardarBicicleta();
    assert.equal(e.peticiones.filter(p => p.metodo === 'POST').length, 0);
    assert.equal(e.avisos.at(-1)[0], 'Faltan datos');
    assert.equal(e.campo('bici-motivo').disabled, true);
});

test('consulta habitual envía incluirEliminadas=false', async () => {
    const e = entorno();
    await e.contexto.cargarBicicletas();
    assert.equal(e.peticiones[0].opciones.params.incluirEliminadas, false);
    assert.equal(e.campo('tabla-bicicletas-body').rows.length, 1);
    const botones = e.campo('tabla-bicicletas-body').rows[0].cells[6].children.map(b => b.textContent);
    assert.ok(botones.includes('Eliminar'));
    assert.ok(!botones.includes('Archivar'));
});

test('mostrar eliminadas conserva consulta y no ofrece acciones de edición o eliminación', async () => {
    const e = entorno();
    e.campo('bici-incluir-eliminadas').checked = true;
    await e.contexto.cargarBicicletas();
    assert.equal(e.peticiones[0].opciones.params.incluirEliminadas, true);
    const fila = e.campo('tabla-bicicletas-body').rows[1];
    assert.equal(fila.cells[3].children[0].textContent, 'DE_BAJA · Eliminada');
    assert.equal(fila.cells[5].textContent, 'Fuera del inventario');
    assert.deepEqual(fila.cells[6].children.map(b => b.textContent), ['Ver']);
});

test('una respuesta tardía del filtro anterior no sobreescribe el listado elegido', async () => {
    const e = entorno();
    const getOriginal = e.contexto.axios.get;
    let resolverAnterior;
    e.contexto.axios.get = (url, opciones) => {
        if (url === '/api/bicicletas' && !opciones.params.incluirEliminadas) {
            return new Promise(resolve => { resolverAnterior = resolve; });
        }
        return getOriginal(url, opciones);
    };
    const anterior = e.contexto.cargarBicicletas();
    e.campo('bici-incluir-eliminadas').checked = true;
    await e.contexto.cargarBicicletas();
    resolverAnterior({ data: [e.datos.bicicleta] });
    await anterior;
    assert.equal(e.campo('tabla-bicicletas-body').rows.length, 2);
});

test('cancelar eliminación no envía escrituras y la confirmación explica la retirada real', async () => {
    const e = entorno();
    e.respuestas.push({ isConfirmed: false });
    await e.contexto.borrarBici(7);
    assert.equal(e.peticiones.filter(p => p.metodo === 'DELETE').length, 0);
    const aviso = e.avisos[0][0];
    assert.equal(aviso.confirmButtonText, 'Confirmar retiro y eliminar');
    assert.ok(aviso.text.includes('retirada físicamente'));
    assert.ok(aviso.text.includes('DE_BAJA'));
    assert.ok(aviso.inputValidator(' '));
    assert.equal(aviso.inputValidator('Retirada definitiva'), undefined);
});

test('confirmar eliminación envía versión, motivo normalizado y confirmación física', async () => {
    const e = entorno();
    e.respuestas.push({ isConfirmed: true, value: '  Retirada definitiva  ' });
    await e.contexto.borrarBici(7);
    const peticion = e.peticiones.find(p => p.metodo === 'DELETE');
    assert.equal(peticion.url, '/api/bicicletas/7');
    assert.equal(peticion.opciones.data.version, 3);
    assert.equal(peticion.opciones.data.motivo, 'Retirada definitiva');
    assert.equal(peticion.opciones.data.salidaFisicaConfirmada, true);
    assert.ok(e.peticiones.some(p => p.url === '/api/bicicletas'));
    assert.ok(e.peticiones.some(p => p.url === '/api/admin/estaciones'));
});

test('inactivar no envía el dato exclusivo de eliminación ni cambia su mensaje de presencia física', async () => {
    const e = entorno();
    e.respuestas.push({ isConfirmed: true, value: 'Pausa operativa' });
    await e.contexto.operarBicicleta(7, 'INACTIVA');
    const peticion = e.peticiones.find(p => p.metodo === 'PATCH');
    assert.equal(peticion.url, '/api/admin/bicicletas/7/estado/INACTIVA');
    assert.equal(peticion.data.version, 3);
    assert.equal(Object.hasOwn(peticion.data, 'salidaFisicaConfirmada'), false);
    assert.equal(e.peticiones.filter(p => p.metodo === 'DELETE').length, 0);
});

test('dar de baja informa que la unidad sigue ocupando espacio', async () => {
    const e = entorno();
    e.respuestas.push({ isConfirmed: false });
    await e.contexto.operarBicicleta(7, 'DE_BAJA');
    assert.ok(e.avisos[0][0].text.includes('no libera su espacio físico'));
});

test('un rechazo 409 se informa como error sin anunciar eliminación ni refrescar como si hubiera guardado', async () => {
    const e = entorno();
    e.contexto.axios.delete = async () => { throw { response: { status: 409, data: 'La bicicleta fue modificada.' } }; };
    e.respuestas.push({ isConfirmed: true, value: 'Retiro QA' });
    await e.contexto.borrarBici(7);
    assert.equal(e.avisos.at(-1)[0], 'No se pudo completar');
    assert.equal(e.avisos.at(-1)[1], 'La bicicleta fue modificada.');
    assert.ok(!e.peticiones.some(p => p.url === '/api/bicicletas'));
});

test('una eliminada se consulta con campos bloqueados y sin asignarle una estación ficticia', async () => {
    const e = entorno();
    e.datos.bicicleta = { ...e.datos.eliminada, id: 7 };
    await e.contexto.editarBici(7);
    assert.equal(e.campo('bici-estacion-select').value, '');
    assert.equal(e.campo('bici-estacion-select').disabled, true);
    assert.equal(e.campo('bici-guardar').disabled, true);
    assert.equal(e.campo('bici-motivo').disabled, true);
});

test('guardar sin mover conserva el ID de la estación llena y no envía estado ni precio', async () => {
    const e = entorno();
    await e.contexto.editarBici(7);
    e.campo('bici-codigo').value = 'QA-EDITADA';
    await e.contexto.guardarBicicleta();
    const peticion = e.peticiones.find(p => p.metodo === 'PUT');
    assert.equal(peticion.data.estacionId, 2);
    assert.equal(peticion.data.version, 3);
    assert.equal(peticion.data.motivo, '');
    assert.equal(Object.hasOwn(peticion.data, 'estado'), false);
    assert.equal(Object.hasOwn(peticion.data, 'precioPorHora'), false);
});
