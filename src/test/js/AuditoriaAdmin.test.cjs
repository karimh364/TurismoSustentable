const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const estatica = path.join(__dirname, '../../main/resources/static');
const fuente = fs.readFileSync(path.join(estatica, 'js/auditoria-admin.js'), 'utf8');
const html = fs.readFileSync(path.join(estatica, 'admin.html'), 'utf8');
const ruta = '/api/admin/auditoria';
const rutaAdministradores = `${ruta}/administradores`;

function evento(id = 35, datos = {}) {
    return {id, administradorId: 3, administradorNickname: 'admin.demo', entidadAfectada: 'USUARIO',
        entidadId: 8, referenciaEntidad: 'qa.roles', accion: 'CAMBIAR_ROLES',
        fechaRegistrada: '2026-11-18T08:43:58.9322', ...datos};
}

function pagina(eventos = [evento()], numero = 0, totalPaginas = 1, totalEventos = eventos.length) {
    return {data: {eventos, pagina: numero, tamano: 20, totalPaginas, totalEventos, referenciaHoraria: 'NO_DECLARADA'}};
}

function detalle(eventoActual = evento(), datos = {}) {
    return {data: {evento: eventoActual, referenciaHoraria: 'NO_DECLARADA', motivoRegistrado: true,
        detalleDisponible: true, cambios: [{campo: 'roles', antes: ['CLIENTE', 'TECNICO'],
            despues: ['TECNICO'], valoresReservados: false}], ...datos}};
}

function pendiente() {
    let resolver;
    const promesa = new Promise(resuelve => { resolver = resuelve; });
    return {promesa, resolver};
}

function entorno(responder = async () => pagina(), responderAdministradores = async () => ({data: []})) {
    const llamadas = [];
    const consultasAdministradores = [];
    const erroresSesion = [];
    let foco;
    function elemento(etiqueta, htmlNodo = '') {
        const eventos = new Map();
        const atributos = new Map();
        return {
            etiqueta, hijos: [], textContent: '', value: '', hidden: /\bhidden\b/.test(htmlNodo),
            disabled: /\bdisabled\b/.test(htmlNodo),
            set innerHTML(_) { assert.fail('La vista no debe insertar HTML con datos de auditoría.'); },
            addEventListener: (nombre, listener) => eventos.set(nombre, listener),
            disparar: (nombre, evento = {}) => eventos.get(nombre)?.(evento),
            setAttribute: (nombre, valor) => atributos.set(nombre, valor),
            getAttribute: nombre => atributos.get(nombre),
            append(...nodos) { this.hijos.push(...nodos); },
            replaceChildren(...nodos) { this.hijos = nodos; },
            focus() { foco = this; }
        };
    }
    const elementos = new Map(Array.from(html.matchAll(/<(\w+)\b[^>]*\bid="(auditoria-[^"]+)"[^>]*>/g),
        ([etiquetaHtml, etiqueta, id]) => [id, elemento(etiqueta, etiquetaHtml)]));
    const documento = elemento('document');
    documento.getElementById = id => elementos.get(id) ?? null;
    documento.createElement = elemento;
    const axios = {get(url, opciones) {
        if (url === rutaAdministradores) {
            consultasAdministradores.push(url);
            return responderAdministradores();
        }
        llamadas.push({url, params: opciones.params ? {...opciones.params} : undefined});
        return responder(url, opciones);
    }};
    for (const metodo of ['post', 'put', 'patch', 'delete']) axios[metodo] = () => {
        assert.fail('Auditoría solo puede consultar.');
    };
    vm.runInNewContext(fuente, {document: documento, axios, manejarErrorSesion(error) {
        erroresSesion.push(error);
        return [401, 403].includes(error.response?.status);
    }});
    documento.disparar('DOMContentLoaded');
    const nodo = id => elementos.get(`auditoria-${id}`);
    return {nodo, llamadas, consultasAdministradores, erroresSesion, foco: () => foco,
        abrir: () => nodo('tab').disparar('shown.bs.tab'),
        salir: () => nodo('tab').disparar('hide.bs.tab'),
        aplicar: () => nodo('filtros').disparar('submit', {preventDefault() {}}),
        boton: (indice = 0) => nodo('eventos').hijos[indice].hijos.at(-1).hijos[0]
    };
}

function celdas(fila) {
    return fila.hijos.map(nodo => nodo.textContent);
}

test('carga al abrir la pestaña, respeta el orden del servidor y muestra la hora registrada hasta los segundos', async () => {
    const vista = entorno(async () => pagina([evento(), evento(34, {administradorNickname: '<b>QA</b>'})]));
    assert.deepEqual(vista.llamadas, []);
    await vista.abrir();
    assert.deepEqual(vista.llamadas, [{url: ruta, params: {pagina: 0, tamano: 20}}]);
    const [primero, segundo] = vista.nodo('eventos').hijos;
    assert.deepEqual(celdas(primero).slice(0, 5), ['#35', 'admin.demo (#3)', 'qa.roles · Usuario #8', 'Cambiar roles',
        '18/11/2026, 08:43:58']);
    assert.equal(celdas(segundo)[1], '<b>QA</b> (#3)');
    assert.equal(vista.nodo('estado').textContent, '2 eventos encontrados.');
    assert.equal(vista.nodo('tabla').hidden, false);
});

test('listado y detalle muestran la referencia actual de bicicleta y estación, o conservan tipo e ID', async () => {
    const registros = [evento(36, {entidadAfectada: 'BICICLETA', entidadId: 22, referenciaEntidad: 'BM-10'}),
        evento(35, {entidadAfectada: 'ESTACION', entidadId: 1, referenciaEntidad: 'Plaza <QA>'}),
        evento(34, {entidadAfectada: 'ESTACION', entidadId: 2, referenciaEntidad: null}),
        evento(33, {entidadAfectada: 'INCIDENCIA', entidadId: 3, referenciaEntidad: undefined})];
    const vista = entorno(async url => url === ruta ? pagina(registros)
        : detalle(registros.find(e => url === `${ruta}/${e.id}`), {detalleDisponible: false, cambios: []}));
    await vista.abrir();
    const etiquetas = ['BM-10 · Bicicleta #22', 'Plaza <QA> · Estación #1', 'Estación #2', 'Incidencia #3'];
    assert.deepEqual(vista.nodo('eventos').hijos.map(fila => celdas(fila)[2]), etiquetas);
    for (const [indice, etiqueta] of etiquetas.entries()) {
        await vista.boton(indice).disparar('click');
        assert.ok(vista.nodo('detalle-evento').textContent.includes(etiqueta));
    }
});

test('envía filtros normalizados, pagina sin perderlos y reinicia al limpiar', async () => {
    const vista = entorno(async (_, {params}) => pagina([evento()], params.pagina, 2, 21));
    vista.nodo('entidad').value = ' usuario ';
    vista.nodo('accion').value = 'cambiar roles';
    await vista.aplicar();
    assert.equal(vista.nodo('pagina').textContent, 'Página 1 de 2');
    assert.equal(vista.nodo('anterior').disabled, true);
    await vista.nodo('siguiente').disparar('click');
    assert.equal(vista.nodo('pagina').textContent, 'Página 2 de 2');
    assert.equal(vista.nodo('siguiente').disabled, true);
    await vista.nodo('anterior').disparar('click');
    await vista.nodo('limpiar').disparar('click');
    assert.deepEqual(vista.llamadas.map(llamada => llamada.params), [0, 1, 0].map(numero =>
        ({entidad: 'USUARIO', accion: 'CAMBIAR_ROLES', pagina: numero, tamano: 20})).concat({pagina: 0, tamano: 20}));
    assert.equal(vista.nodo('entidad').value, '');
    assert.equal(vista.nodo('accion').value, '');
});

test('el selector carga actores al abrir Auditoría y conserva el ID junto con los filtros y la paginación', async () => {
    const vista = entorno(async (_, {params}) => pagina([evento()], params.pagina, 2, 21), async () => ({data: [
        {administradorId: 3, administradorNickname: 'admin.demo'},
        {administradorId: 8, administradorNickname: '<b>qa.roles</b>'}
    ]}));
    const selector = vista.nodo('administrador');
    assert.equal(selector.disabled, true);
    assert.deepEqual(vista.consultasAdministradores, []);
    await vista.abrir();
    assert.equal(selector.disabled, false);
    assert.deepEqual(selector.hijos.map(o => [o.value, o.textContent]),
        [['3', 'admin.demo (#3)'], ['8', '<b>qa.roles</b> (#8)']]);

    selector.value = '3';
    vista.nodo('entidad').value = 'BICICLETA';
    vista.nodo('accion').value = 'CREAR';
    await vista.aplicar();
    await vista.nodo('siguiente').disparar('click');
    await vista.nodo('limpiar').disparar('click');
    assert.deepEqual(vista.llamadas.map(llamada => llamada.params), [
        {pagina: 0, tamano: 20},
        {entidad: 'BICICLETA', accion: 'CREAR', administradorId: 3, pagina: 0, tamano: 20},
        {entidad: 'BICICLETA', accion: 'CREAR', administradorId: 3, pagina: 1, tamano: 20},
        {pagina: 0, tamano: 20}
    ]);
    assert.equal(selector.value, '');
    await vista.abrir();
    assert.deepEqual(vista.consultasAdministradores, [rutaAdministradores]);
});

test('si falla la carga de administradores, los otros filtros siguen disponibles y se puede reintentar', async () => {
    let primerIntento = true;
    const vista = entorno(async () => pagina(), async () => {
        if (primerIntento) {
            primerIntento = false;
            throw new Error('Red no disponible');
        }
        return {data: [{administradorId: 3, administradorNickname: 'admin.demo'}]};
    });
    await vista.abrir();
    assert.equal(vista.nodo('administrador').disabled, true);
    assert.match(vista.nodo('administradores-estado').textContent, /No se pudo cargar/);
    assert.equal(vista.nodo('tabla').hidden, false);
    vista.nodo('entidad').value = 'USUARIO';
    await vista.aplicar();
    assert.equal(vista.llamadas.at(-1).params.entidad, 'USUARIO');
    await vista.abrir();
    assert.equal(vista.consultasAdministradores.length, 2);
    assert.equal(vista.nodo('administrador').disabled, false);
    assert.equal(vista.nodo('administradores-estado').textContent, '');
});

test('la carga de administradores también delega el rechazo de permisos', async () => {
    const vista = entorno(async () => pagina(), async () => { throw {response: {status: 403}}; });
    await vista.abrir();
    assert.equal(vista.erroresSesion[0].response.status, 403);
    assert.equal(vista.nodo('administrador').disabled, true);
});

test('valida filtros sin enviar una petición inválida y permite códigos nuevos', async () => {
    const vista = entorno();
    vista.nodo('accion').value = 'CAMBIAR/ROLES';
    await vista.aplicar();
    assert.deepEqual(vista.llamadas, []);
    assert.match(vista.nodo('estado').textContent, /Revisá los filtros/);
    assert.equal(vista.foco(), vista.nodo('accion'));
    vista.nodo('accion').value = 'ACCION_NUEVA';
    await vista.aplicar();
    assert.equal(vista.llamadas[0].params.accion, 'ACCION_NUEVA');
});

test('distingue carga, fallos y un listado vacío', async () => {
    const demorada = pendiente();
    let respuesta = demorada.promesa;
    const vista = entorno(() => respuesta);
    const primera = vista.abrir();
    assert.equal(vista.nodo('resultados').getAttribute('aria-busy'), 'true');
    assert.match(vista.nodo('estado').textContent, /Cargando/);
    demorada.resolver({data: {eventos: 'inválido'}});
    await primera;
    assert.match(vista.nodo('estado').textContent, /No se pudieron cargar/);
    assert.equal(vista.nodo('resultados').getAttribute('aria-busy'), 'false');
    respuesta = Promise.resolve(pagina([], 0, 0, 0));
    await vista.abrir();
    assert.match(vista.nodo('estado').textContent, /No hay eventos/);
    assert.equal(vista.nodo('tabla').hidden, true);
    assert.equal(vista.nodo('paginacion').hidden, true);
});

test('una respuesta tardía no reemplaza el filtro vigente y salir de la pestaña invalida cargas', async () => {
    const lenta = pendiente();
    const segunda = pendiente();
    const vista = entorno((_, {params}) => params.accion ? segunda.promesa : lenta.promesa);
    const primera = vista.abrir();
    vista.nodo('accion').value = 'CAMBIAR_ROLES';
    const filtrada = vista.aplicar();
    segunda.resolver(pagina([evento(34)]));
    await filtrada;
    lenta.resolver(pagina([evento(99)]));
    await primera;
    assert.equal(celdas(vista.nodo('eventos').hijos[0])[0], '#34');
    vista.salir();
    assert.equal(vista.nodo('eventos').hijos.length, 0);
});

test('consulta solo el evento elegido, muestra roles y no muestra ni usa texto sensible', async () => {
    const vista = entorno(async url => url === ruta ? pagina() : detalle(evento(), {
        motivo: 'texto no visible', valoresAnteriores: 'snapshot no visible', cambios: [
            ...detalle().data.cambios,
            {campo: 'email', antes: 'privado@example.test', despues: 'otro@example.test', valoresReservados: true},
            {campo: 'desconocido', antes: '<img>', despues: 'privado', valoresReservados: false}
        ]
    }));
    await vista.abrir();
    assert.equal(vista.nodo('detalle').hidden, true);
    assert.equal(vista.llamadas.length, 1);
    const boton = vista.boton();
    await boton.disparar('click');
    assert.equal(vista.llamadas.at(-1).url, `${ruta}/35`);
    assert.equal(vista.foco(), vista.nodo('detalle-titulo'));
    assert.equal(boton.getAttribute('aria-expanded'), 'true');
    assert.deepEqual(vista.nodo('cambios').hijos.map(celdas), [
        ['Roles', 'Cliente, Técnico', 'Técnico'], ['Correo electrónico', 'Campo modificado · valores reservados'],
        ['Campo no reconocido', 'No disponible', 'No disponible']
    ]);
    assert.equal(vista.nodo('detalle-evento').textContent,
        'admin.demo (#3) · qa.roles · Usuario #8 · Cambiar roles · 18/11/2026, 08:43:58');
    assert.equal(vista.nodo('detalle-motivo').textContent, 'Motivo registrado (texto reservado).');
    vista.nodo('detalle-cerrar').disparar('click');
    assert.equal(vista.nodo('detalle').hidden, true);
    assert.equal(boton.getAttribute('aria-expanded'), 'false');
    assert.equal(vista.foco(), boton);
});

test('traduce los estados de cuenta y estación y no inventa un valor anterior al alta', async () => {
    const vista = entorno(async url => url === ruta ? pagina() : detalle(evento(), {cambios: [
        {campo: 'activo', antes: true, despues: false, valoresReservados: false},
        {campo: 'esPublica', antes: null, despues: false, valoresReservados: false},
        {campo: 'borradoEstacion', antes: false, despues: true, valoresReservados: false},
        {campo: 'capacidad', antes: 0, despues: 15, valoresReservados: false}
    ]}));
    await vista.abrir();
    await vista.boton().disparar('click');
    assert.deepEqual(vista.nodo('cambios').hijos.map(celdas), [
        ['Estado de la cuenta', 'Activa', 'Inactiva'], ['Uso de la estación', '—', 'Uso interno'],
        ['Estado de la estación', 'Activa', 'Eliminada'], ['Capacidad', '0', '15']
    ]);
});

test('mantiene los metadatos para un evento sin cambios interpretables', async () => {
    const bici = evento(35, {entidadAfectada: 'BICICLETA', referenciaEntidad: null});
    const vista = entorno(async url => url === ruta ? pagina([bici]) : detalle(bici, {
        detalleDisponible: false, cambios: [], motivoRegistrado: false
    }));
    await vista.abrir();
    await vista.boton().disparar('click');
    assert.match(vista.nodo('detalle-evento').textContent, /Bicicleta #8/);
    assert.match(vista.nodo('detalle-estado').textContent, /Detalle de cambios no disponible/);
    assert.equal(vista.nodo('detalle-motivo').textContent, 'Sin motivo registrado.');
    assert.equal(vista.nodo('cambios-tabla').hidden, true);
});

test('cerrar un detalle en carga descarta la respuesta y un 404 no se confunde con falta de cambios', async () => {
    const demorada = pendiente();
    const vista = entorno(async url => {
        if (url === ruta) return pagina([evento(), evento(34)]);
        if (url.endsWith('/35')) return demorada.promesa;
        throw {response: {status: 404}};
    });
    await vista.abrir();
    const primera = vista.boton().disparar('click');
    assert.match(vista.nodo('detalle-estado').textContent, /Cargando/);
    vista.nodo('detalle-cerrar').disparar('click');
    await vista.boton(1).disparar('click');
    demorada.resolver(detalle());
    await primera;
    assert.equal(vista.nodo('detalle-titulo').textContent, 'Detalle del evento #34');
    assert.match(vista.nodo('detalle-estado').textContent, /El evento ya no está disponible/);
    assert.equal(vista.nodo('cambios-tabla').hidden, true);
});

test('delega 401/403 al manejo de sesión y limpia datos visibles ante un rechazo del detalle', async () => {
    const fallo = status => ({response: {status}});
    const anonima = entorno(async () => { throw fallo(401); });
    await anonima.abrir();
    assert.equal(anonima.erroresSesion[0].response.status, 401);
    assert.equal(anonima.nodo('tabla').hidden, true);

    const sinRol = entorno(async url => { if (url === ruta) return pagina(); throw fallo(403); });
    await sinRol.abrir();
    await sinRol.boton().disparar('click');
    assert.equal(sinRol.erroresSesion[0].response.status, 403);
    assert.equal(sinRol.nodo('tabla').hidden, true);
    assert.equal(sinRol.nodo('detalle').hidden, true);
    assert.match(sinRol.nodo('estado').textContent, /sesión actual/);
});

test('las sugerencias incluyen los códigos de alquiler, mantenimiento e incidencias ya auditados', () => {
    const vista = entorno();
    const entidades = vista.nodo('entidades').hijos;
    const acciones = vista.nodo('acciones').hijos;
    assert.equal(entidades.find(opcion => opcion.value === 'ALQUILER_DETALLE')?.textContent,
        'Detalle de alquiler');
    assert.equal(acciones.find(opcion => opcion.value === 'CANCELAR')?.textContent, 'Cancelar');
    for (const codigo of ['ACTUALIZAR_PREVENTIVO', 'DESASIGNAR_POR_INACTIVACION', 'UBICAR_RECUPERADA',
        'BAJA_PERDIDA', 'ELIMINAR_PERDIDA', 'RECEPCION_DEVOLUCION']) {
        assert.ok(acciones.some(opcion => opcion.value === codigo), `Falta la sugerencia ${codigo}`);
    }
    assert.deepEqual(vista.llamadas, []);
});

test('el panel enlaza la pestaña, describe el orden por ID y carga el script después de sesión', () => {
    assert.match(html, /id="auditoria-tab"[^>]*data-bs-toggle="tab"[^>]*data-bs-target="#tab-auditoria"/);
    assert.match(html, /id="tab-auditoria" role="tabpanel" aria-labelledby="auditoria-tab"/);
    assert.match(html, /id="auditoria-detalle-estado" role="status"/);
    assert.match(html, /id="auditoria-tabla" hidden/);
    assert.match(html, /<label[^>]*for="auditoria-administrador">Administrador<\/label>\s*<select[^>]*id="auditoria-administrador"[^>]*>\s*<option value="">Todos<\/option>/);
    assert.match(html, /<caption[^>]*>Eventos de auditoría ordenados por ID, de mayor a menor<\/caption>/);
    assert.equal((html.match(/src="js\/auditoria-admin.js"/g) ?? []).length, 1);
    assert.ok(html.indexOf('src="js/auth.js"') < html.indexOf('src="js/auditoria-admin.js"'));
});
