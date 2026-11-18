const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const raiz = path.join(__dirname, '../../main/resources/static');
const fuenteAdmin = fs.readFileSync(path.join(raiz, 'js/admin.js'), 'utf8');
const fuenteMapa = fs.readFileSync(path.join(raiz, 'js/mapa.js'), 'utf8');

function admin() {
    const campos = new Map();
    const campo = id => {
        if (!campos.has(id)) campos.set(id, {value: '', innerHTML: '', disabled: false});
        return campos.get(id);
    };
    for (const [id, valor] of Object.entries({
        'estacion-id': '', 'estacion-nombre': 'Plaza QA',
        'estacion-direccion': 'Calle QA', 'estacion-capacidad': '5',
        'estacion-latitud': '0', 'estacion-longitud': '0',
        'estacion-es-publica': 'true', 'estacion-version': '0'
    })) campo(id).value = valor;
    const peticiones = [], avisos = [];
    const estado = {estaciones: [], error: null, cierres: 0};
    const contexto = vm.createContext({
        console: {error() {}},
        document: {addEventListener() {}, getElementById: campo},
        axios: {
            async get() { return {data: estado.estaciones}; },
            async post(url, data) {
                peticiones.push({metodo: 'POST', url, data});
                if (estado.error) throw estado.error;
                return {data: {}};
            },
            async put(url, data) {
                peticiones.push({metodo: 'PUT', url, data});
                if (estado.error) throw estado.error;
                return {data: {}};
            }
        },
        bootstrap: {Modal: {getInstance: () => ({hide() { estado.cierres++; }})}},
        Swal: {async fire(...argumentos) { avisos.push(argumentos); return {}; }},
        manejarErrorSesion: () => false
    });
    vm.runInContext(fuenteAdmin, contexto, {filename: 'admin.js'});
    return {contexto, campo, peticiones, avisos, estado};
}

for (const [latitud, longitud] of [['0', '0'], ['-53.123456', '-67.654321']]) {
    test(`alta envía coordenadas numéricas sin redondear: ${latitud}, ${longitud}`, async () => {
        const e = admin();
        e.campo('estacion-latitud').value = latitud;
        e.campo('estacion-longitud').value = longitud;
        await e.contexto.guardarEstacion();
        assert.equal(e.peticiones.length, 1);
        assert.equal(e.peticiones[0].metodo, 'POST');
        assert.equal(typeof e.peticiones[0].data.latitud, 'number');
        assert.equal(e.peticiones[0].data.latitud, Number(latitud));
        assert.equal(e.peticiones[0].data.longitud, Number(longitud));
        assert.equal(e.estado.cierres, 1);
    });
}

test('edición conserva versión y coordenadas decimales sin reclasificar el uso', async () => {
    const e = admin();
    e.campo('estacion-id').value = '7';
    e.campo('estacion-version').value = '3';
    e.campo('estacion-latitud').value = '-53.123456';
    e.campo('estacion-longitud').value = '-67.654321';
    await e.contexto.guardarEstacion();
    assert.equal(e.peticiones[0].metodo, 'PUT');
    assert.equal(e.peticiones[0].url, '/api/estaciones/7');
    assert.equal(e.peticiones[0].data.version, 3);
    assert.equal(e.peticiones[0].data.latitud, -53.123456);
    assert.equal(e.peticiones[0].data.longitud, -67.654321);
    assert.equal('esPublica' in e.peticiones[0].data, false);
});

for (const [campo, valor] of [
    ['latitud', ''], ['longitud', ''], ['latitud', '-53.1234567'],
    ['longitud', '-67.1234567'], ['latitud', '90.000001'],
    ['longitud', '-180.000001'], ['latitud', 'NaN'],
    ['longitud', 'Infinity'], ['latitud', '-53.1resto'],
    ['longitud', '1e-7']
]) {
    test(`no envía ${campo} inválida ni la redondea: ${valor || 'vacía'}`, async () => {
        const e = admin();
        e.campo(`estacion-${campo}`).value = valor;
        await e.contexto.guardarEstacion();
        assert.equal(e.peticiones.length, 0);
        assert.equal(e.estado.cierres, 0);
        assert.equal(e.avisos[0][0], 'Revisá las coordenadas');
        assert.ok(e.avisos[0][1].includes(campo));
        assert.ok(e.avisos[0][1].includes('6 decimales'));
    });
}

test('listado administrativo muestra cero como coordenada, no como dato ausente', async () => {
    const e = admin();
    e.estado.estaciones = [{id: 1, nombre: 'QA', direccion: 'QA', capacidad: 5,
        esPublica: true, latitud: 0, longitud: 0}];
    await e.contexto.cargarEstaciones();
    assert.match(e.campo('tabla-estaciones-body').innerHTML, /<td>0<\/td>\s*<td>0<\/td>/);
});

test('rechazo de precisión del backend muestra su mensaje sin anunciar éxito', async () => {
    const e = admin();
    e.estado.error = {response: {status: 400, data: {
        validationErrors: ['latitud: La latitud admite hasta 6 decimales.']
    }}};
    await e.contexto.guardarEstacion();
    assert.equal(e.estado.cierres, 0);
    assert.equal(e.avisos.length, 1);
    assert.equal(e.avisos[0][0].icon, 'error');
    assert.equal(e.avisos[0][0].text, 'La latitud admite hasta 6 decimales.');
});

async function marcadores(estaciones) {
    const todos = [];
    let listo;
    const contexto = vm.createContext({
        console: {error() {}},
        document: {addEventListener(evento, funcion) { listo = funcion; }},
        fetch: async () => ({json: async () => estaciones}),
        L: {
            map: () => ({setView() { return this; }}),
            tileLayer: () => ({addTo() { return this; }}),
            Icon: class {},
            marker(coordenadas) {
                return {addTo() { return this; }, bindPopup(html) {
                    todos.push({coordenadas, html}); return this;
                }};
            }
        }
    });
    vm.runInContext(fuenteMapa, contexto, {filename: 'mapa.js'});
    listo();
    await new Promise(setImmediate);
    return todos.filter(marcador => marcador.html.includes('Ver Bicicletas'));
}

test('mapa usa cero y seis decimales reales, sin ubicación de reemplazo', async () => {
    const puntos = await marcadores([
        {id: 1, latitud: 0, longitud: 0, capacidad: 5, bicicletasDisponibles: 1},
        {id: 2, latitud: -53.123456, longitud: -67.654321, capacidad: 5, bicicletasDisponibles: 1}
    ]);
    assert.equal(puntos.length, 2);
    assert.equal(puntos[0].coordenadas[0], 0);
    assert.equal(puntos[0].coordenadas[1], 0);
    assert.equal(puntos[1].coordenadas[0], -53.123456);
    assert.equal(puntos[1].coordenadas[1], -67.654321);
});

test('mapa no inventa posiciones ante coordenadas ausentes o inválidas', async () => {
    const puntos = await marcadores([
        {latitud: null, longitud: 0}, {latitud: 0},
        {latitud: 91, longitud: 0}, {latitud: 0, longitud: -181},
        {latitud: NaN, longitud: 0}, {latitud: 0, longitud: Infinity},
        {latitud: '-53.123456', longitud: -67.654321}
    ]);
    assert.equal(puntos.length, 0);
});

test('formulario informa precisión y rangos coherentes con el backend', () => {
    const html = fs.readFileSync(path.join(raiz, 'admin.html'), 'utf8');
    assert.match(html, /step="0\.000001" min="-90" max="90"[^>]*id="estacion-latitud"/);
    assert.match(html, /step="0\.000001" min="-180" max="180"[^>]*id="estacion-longitud"/);
    assert.ok(html.includes('Hasta 6 decimales') || html.includes('hasta 6 decimales'));
});
