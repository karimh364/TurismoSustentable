// node --test src/test/js/Sesion.test.cjs
// Pruebas sin navegador, servidor, MySQL ni credenciales reales.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const carpetaJS = path.join(__dirname, '../../main/resources/static/js');
const usuario = { nombre: 'Ana', roles: ['CLIENTE', 'ADMIN'] };
const errorHTTP = status => ({ response: { status } });

function entorno(valores = {}) {
    const datos = new Map(Object.entries(valores));
    const redirecciones = [];
    const avisos = [];
    const eventos = {};
    const aviso = {
        classList: { remove() {}, add() {} },
        textContent: ''
    };
    const boton = { disabled: false };
    const formulario = {
        innerHTML: '<input id="login-nickname">',
        addEventListener(nombre, accion) { eventos[nombre] = accion; },
        querySelector() { return boton; }
    };
    const elementos = new Map([
        ['login-form', formulario], ['login-nickname', { value: 'ana' }],
        ['login-password', { value: 'SoloParaTest!1' }], ['status-message', aviso]
    ]);
    const contexto = vm.createContext({
        console: { error() {} }, URLSearchParams,
        setTimeout: accion => accion(),
        sessionStorage: {
            getItem: clave => datos.get(clave) ?? null,
            setItem: (clave, valor) => datos.set(clave, String(valor)),
            removeItem: clave => datos.delete(clave)
        },
        axios: {
            get: async () => ({ data: usuario }),
            post: async () => ({ status: 200 })
        },
        Swal: {
            fire: async (...argumentos) => { avisos.push(argumentos); },
            mixin: () => ({ fire: async dato => { avisos.push([dato]); } })
        },
        window: {
            location: {
                pathname: '/index.html', search: '',
                replace: url => redirecciones.push(url)
            },
            addEventListener: (nombre, accion) => { eventos[nombre] = accion; }
        },
        document: {
            addEventListener: (nombre, accion) => { eventos[nombre] = accion; },
            getElementById: id => elementos.get(id) ?? null,
            querySelector: selector => selector.includes('button[type="submit"]') ? boton : null,
            querySelectorAll: () => []
        }
    });
    function cargar(archivo) {
        vm.runInContext(fs.readFileSync(path.join(carpetaJS, archivo), 'utf8'), contexto);
    }
    cargar('auth.js');
    return { contexto, datos, redirecciones, avisos, eventos, elementos,
        aviso, formulario, boton, cargar };
}

test('perfil confirmado actualiza nombre y rol prioritario desde el servidor', async () => {
    const e = entorno({ usuarioRol: 'CLIENTE', usuarioNombre: 'Viejo' });
    assert.equal(await e.contexto.obtenerUsuarioActual(), usuario);
    assert.equal(e.datos.get('usuarioLogueado'), 'true');
    assert.equal(e.datos.get('usuarioNombre'), 'Ana');
    assert.equal(e.datos.get('usuarioRol'), 'ADMIN');
});

test('401 borra solo los indicadores de sesión, no otros datos de la aplicación', async () => {
    const e = entorno({ usuarioLogueado: 'true', usuarioRol: 'ADMIN',
        usuarioNombre: 'Ana', estacionSeleccionada: '5' });
    e.contexto.axios.get = async () => { throw errorHTTP(401); };
    assert.equal(await e.contexto.obtenerUsuarioActual(), null);
    assert.equal(e.datos.size, 1);
    assert.equal(e.datos.get('estacionSeleccionada'), '5');
});

test('error de conexión no se interpreta como cierre de sesión', async () => {
    const e = entorno({ usuarioLogueado: 'true' });
    e.contexto.axios.get = async () => { throw new Error('Sin conexión'); };
    await assert.rejects(e.contexto.obtenerUsuarioActual(), /Sin conexión/);
    assert.equal(e.datos.get('usuarioLogueado'), 'true');
    assert.equal(e.redirecciones.length, 0);
});

test('consultas simultáneas de perfil comparten una petición, sin sondeo periódico', async () => {
    const e = entorno();
    let llamadas = 0;
    let terminar;
    e.contexto.axios.get = () => {
        llamadas++;
        return new Promise(resolve => { terminar = resolve; });
    };
    const primera = e.contexto.obtenerUsuarioActual();
    const segunda = e.contexto.obtenerUsuarioActual();
    terminar({ data: usuario });
    await Promise.all([primera, segunda]);
    assert.equal(llamadas, 1);
});

test('sin marcas locales se puede entrar si el servidor confirma la sesión', async () => {
    const e = entorno();
    assert.equal(await e.contexto.requerirSesion(['ADMIN']), usuario);
    assert.equal(e.redirecciones.length, 0);
});

test('rol insuficiente informa y redirige, pero no borra la sesión válida', async () => {
    const e = entorno();
    e.contexto.axios.get = async () => ({ data: { nombre: 'Ana', roles: ['CLIENTE'] } });
    assert.equal(await e.contexto.requerirSesion(['ADMIN']), null);
    assert.equal(e.datos.get('usuarioLogueado'), 'true');
    assert.equal(e.avisos[0][0].title, 'Acceso no permitido');
    assert.deepEqual(e.redirecciones, ['index.html']);
});

test('401 repetidos de operaciones protegidas generan una sola redirección', () => {
    const e = entorno({ usuarioLogueado: 'true' });
    assert.equal(e.contexto.manejarErrorSesion(errorHTTP(401)), true);
    assert.equal(e.contexto.manejarErrorSesion(errorHTTP(401)), true);
    assert.deepEqual(e.redirecciones, ['index.html?sesion=finalizada#login-anchor']);
    assert.equal(e.datos.has('usuarioLogueado'), false);
});

test('403 informa falta de permisos sin desconectar ni repetir avisos simultáneos', async () => {
    const e = entorno({ usuarioLogueado: 'true' });
    assert.equal(e.contexto.manejarErrorSesion(errorHTTP(403)), true);
    e.contexto.manejarErrorSesion(errorHTTP(403));
    assert.equal(e.avisos.length, 1);
    assert.equal(e.redirecciones.length, 0);
    assert.equal(e.datos.get('usuarioLogueado'), 'true');
    await Promise.resolve();
});

test('validaciones, conflictos y fallas de red siguen en los manejadores existentes', () => {
    const e = entorno();
    for (const error of [errorHTTP(400), errorHTTP(409), new Error('Red')]) {
        assert.equal(e.contexto.manejarErrorSesion(error), false);
    }
    assert.equal(e.avisos.length, 0);
});

test('logout confirmado por 401 limpia indicadores y conserva la notificación', async () => {
    const e = entorno({ usuarioLogueado: 'true', estacionSeleccionada: '5' });
    let cierres = 0;
    e.contexto.axios.post = async url => { assert.equal(url, '/api/logout'); cierres++; };
    e.contexto.axios.get = async () => { throw errorHTTP(401); };
    await e.contexto.logout();
    assert.equal(cierres, 1);
    assert.equal(e.datos.has('usuarioLogueado'), false);
    assert.equal(e.datos.get('estacionSeleccionada'), '5');
    assert.equal(e.avisos[0][0].icon, 'success');
    assert.deepEqual(e.redirecciones, ['index.html']);
});

test('403 después de logout no se anuncia falsamente como cierre confirmado', async () => {
    const e = entorno({ usuarioLogueado: 'true' });
    e.contexto.axios.get = async () => { throw errorHTTP(403); };
    await e.contexto.logout();
    assert.equal(e.datos.get('usuarioLogueado'), 'true');
    assert.equal(e.avisos[0][0].icon, 'error');
    assert.equal(e.redirecciones.length, 0);
});

test('inicio con marca vieja y 401 mantiene el formulario, no muestra sesión activa', async () => {
    const e = entorno({ usuarioLogueado: 'true', usuarioRol: 'ADMIN' });
    e.contexto.axios.get = async () => { throw errorHTTP(401); };
    e.contexto.window.location.search = '?sesion=finalizada';
    e.cargar('index.js');
    await e.contexto.checkSession();
    assert.doesNotMatch(e.formulario.innerHTML, /Sesión Activa/);
    assert.match(e.aviso.textContent, /Tu sesión finalizó/);
    assert.equal(e.datos.has('usuarioLogueado'), false);
    assert.equal(e.boton.disabled, false);
});

test('inicio sin marcas locales muestra sesión activa si el servidor la confirma', async () => {
    const e = entorno();
    e.cargar('index.js');
    await e.contexto.checkSession();
    assert.match(e.formulario.innerHTML, /Sesión Activa/);
    assert.match(e.formulario.innerHTML, /admin.html/);
});

test('inicio con error de red conserva indicadores, pero no afirma sesión activa', async () => {
    const e = entorno({ usuarioLogueado: 'true' });
    e.contexto.axios.get = async () => { throw new Error('Red'); };
    e.cargar('index.js');
    await e.contexto.checkSession();
    assert.doesNotMatch(e.formulario.innerHTML, /Sesión Activa/);
    assert.match(e.aviso.textContent, /No pudimos comprobar/);
    assert.equal(e.datos.get('usuarioLogueado'), 'true');
    assert.equal(e.redirecciones.length, 0);
});

test('restaurar inicio con Atrás recupera el formulario si la sesión terminó', async () => {
    const e = entorno();
    const original = e.formulario.innerHTML;
    e.cargar('index.js');
    await e.contexto.checkSession();
    e.elementos.delete('login-nickname');
    e.contexto.axios.get = async () => { throw errorHTTP(401); };
    await e.contexto.checkSession();
    assert.equal(e.formulario.innerHTML, original);
    assert.equal(typeof e.eventos.submit, 'function');
});

test('401 del login sigue siendo credenciales incorrectas, no sesión vencida', async () => {
    const e = entorno();
    e.cargar('index.js');
    e.contexto.axios.post = async () => { throw errorHTTP(401); };
    await e.eventos.submit({ preventDefault() {} });
    assert.match(e.aviso.innerHTML, /Nombre de usuario o contraseña incorrectos/);
    assert.equal(e.redirecciones.length, 0);
    assert.equal(e.boton.disabled, false);
});

test('guardar estación con 401 no anuncia éxito ni reenvía el POST', async () => {
    const e = entorno({ usuarioLogueado: 'true' });
    e.cargar('admin.js');
    for (const [id, value] of Object.entries({
        'estacion-id': '', 'estacion-nombre': 'Plaza QA',
        'estacion-direccion': 'Calle QA', 'estacion-capacidad': '2',
        'estacion-latitud': '0', 'estacion-longitud': '0',
        'estacion-es-publica': 'true'
    })) e.elementos.set(id, { value });
    let envios = 0;
    e.contexto.axios.post = async () => { envios++; throw errorHTTP(401); };
    await e.contexto.guardarEstacion();
    assert.equal(envios, 1);
    assert.equal(e.avisos.length, 0);
    assert.deepEqual(e.redirecciones, ['index.html?sesion=finalizada#login-anchor']);
});
