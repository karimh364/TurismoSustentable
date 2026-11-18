const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const carpeta = path.join(
    __dirname, '../../main/resources/static/js'
);

function entorno(valores = {}) {
    const datos = new Map(Object.entries(valores));
    const navegaciones = [];
    const avisos = [];
    const consultas = [];
    const atributos = new Map();

    const estado = {
        usuario: {nombre: 'QA', roles: ['CLIENTE']},
        error: null,
        pendiente: null,
        confirmar: false,
        desplazamientos: 0,
        enfoques: 0
    };

    const boton = {
        innerHTML: 'Ver Bicicletas',
        disabled: false,
        setAttribute(clave, valor) {
            atributos.set(clave, valor);
        },
        removeAttribute(clave) {
            atributos.delete(clave);
        }
    };

    const contexto = vm.createContext({
        console: {error() {}},
        setTimeout: accion => accion(),
        sessionStorage: {
            getItem: clave => datos.get(clave) ?? null,
            setItem: (clave, valor) => datos.set(clave, String(valor)),
            removeItem: clave => datos.delete(clave)
        },
        axios: {
            async get(url) {
                consultas.push(url);
                if (estado.error) throw estado.error;
                if (estado.pendiente) return estado.pendiente;
                return {data: estado.usuario};
            }
        },
        Swal: {
            async fire(...argumentos) {
                avisos.push(argumentos);
                return {isConfirmed: estado.confirmar};
            }
        },
        window: {
            addEventListener() {},
            location: {
                pathname: '/index.html',
                set href(valor) { navegaciones.push(valor); },
                replace(valor) { navegaciones.push(valor); }
            }
        },
        document: {
            addEventListener() {},
            getElementById(id) {
                if (id === 'login-anchor') {
                    return {
                        scrollIntoView() {
                            estado.desplazamientos++;
                        }
                    };
                }
                if (id === 'login-nickname') {
                    return {
                        focus() { estado.enfoques++; }
                    };
                }
                return null;
            }
        }
    });

    for (const archivo of ['auth.js', 'mapa.js']) {
        vm.runInContext(
            fs.readFileSync(path.join(carpeta, archivo), 'utf8'),
            contexto,
            {filename: archivo}
        );
    }

    return {
        contexto, datos, navegaciones, avisos,
        consultas, atributos, estado, boton
    };
}

test('CLIENTE confirmado puede avanzar sin marcas locales', async () => {
    const e = entorno();

    await e.contexto.alquilarBici(3, e.boton);

    assert.deepEqual(e.consultas, ['/api/usuarios/actual']);
    assert.deepEqual(e.navegaciones, ['alquiler.html?estacionId=3']);
    assert.equal(e.datos.get('estacionSeleccionada'), '3');
    assert.equal(e.boton.disabled, false);
    assert.equal(e.boton.innerHTML, 'Ver Bicicletas');
    assert.equal(e.atributos.has('aria-busy'), false);
});

for (const roles of [['ADMIN'], ['TECNICO'], []]) {
    test(`roles sin CLIENTE no avanzan: ${roles.join(',') || 'ninguno'}`, async () => {
        const e = entorno({
            usuarioLogueado: 'true',
            usuarioRol: 'CLIENTE'
        });

        e.estado.usuario.roles = roles;

        await e.contexto.alquilarBici(3, e.boton);

        assert.equal(e.consultas.length, 1);
        assert.equal(e.navegaciones.length, 0);
        assert.equal(e.datos.has('estacionSeleccionada'), false);
        assert.equal(e.datos.get('usuarioLogueado'), 'true');
        assert.equal(e.avisos[0][0].title, 'Acceso no permitido');
    });
}

test('ADMIN y CLIENTE puede avanzar aunque ADMIN sea el rol prioritario', async () => {
    const e = entorno();
    e.estado.usuario.roles = ['ADMIN', 'CLIENTE'];

    await e.contexto.alquilarBici(4);

    assert.equal(e.datos.get('usuarioRol'), 'ADMIN');
    assert.deepEqual(e.navegaciones, ['alquiler.html?estacionId=4']);
});

test('401 descarta marcas falsas sin navegar al alquiler', async () => {
    const e = entorno({
        usuarioLogueado: 'true',
        usuarioRol: 'CLIENTE',
        usuarioNombre: 'Anterior',
        estacionSeleccionada: '9'
    });

    e.estado.error = {response: {status: 401}};

    await e.contexto.alquilarBici(3, e.boton);

    assert.equal(e.navegaciones.length, 0);
    assert.equal(e.datos.has('usuarioLogueado'), false);
    assert.equal(e.datos.has('usuarioRol'), false);
    assert.equal(e.datos.get('estacionSeleccionada'), '9');
    assert.equal(e.avisos[0][0].title, 'Iniciá sesión');
    assert.equal(e.estado.desplazamientos, 0);
});

test('visitante puede confirmar ir al formulario de ingreso', async () => {
    const e = entorno();
    e.estado.error = {response: {status: 401}};
    e.estado.confirmar = true;

    await e.contexto.alquilarBici(3);

    assert.equal(e.estado.desplazamientos, 1);
    assert.equal(e.estado.enfoques, 1);
    assert.equal(e.navegaciones.length, 0);
});

for (const error of [
    new Error('Sin conexión'),
    {response: {status: 500}},
    {response: {status: 503}}
]) {
    test(`fallo ${error.response?.status || 'de red'} no autoriza ni cierra sesión`, async () => {
        const e = entorno({
            usuarioLogueado: 'true',
            usuarioRol: 'CLIENTE'
        });

        e.estado.error = error;

        await e.contexto.alquilarBici(3, e.boton);

        assert.equal(e.navegaciones.length, 0);
        assert.equal(e.datos.get('usuarioLogueado'), 'true');
        assert.equal(e.datos.has('estacionSeleccionada'), false);
        assert.equal(e.avisos[0][0].title, 'No se pudo comprobar el acceso');
        assert.equal(e.boton.disabled, false);
        assert.equal(e.atributos.has('aria-busy'), false);
    });
}

test('403 informa falta de permisos sin desconectar', async () => {
    const e = entorno({usuarioLogueado: 'true'});
    e.estado.error = {response: {status: 403}};

    await e.contexto.alquilarBici(3);

    assert.equal(e.navegaciones.length, 0);
    assert.equal(e.datos.get('usuarioLogueado'), 'true');
    assert.equal(e.avisos.length, 1);
    assert.equal(e.avisos[0][0].title, 'Acceso no permitido');
});

test('cada nuevo intento vuelve a consultar los roles', async () => {
    const e = entorno();

    await e.contexto.alquilarBici(3);

    e.estado.usuario = {nombre: 'QA', roles: ['ADMIN']};

    await e.contexto.alquilarBici(4);

    assert.equal(e.consultas.length, 2);
    assert.deepEqual(e.navegaciones, ['alquiler.html?estacionId=3']);
    assert.equal(e.datos.get('estacionSeleccionada'), '3');
    assert.equal(e.avisos[0][0].title, 'Acceso no permitido');
});

test('doble clic comparte la operación y muestra comprobación pendiente', async () => {
    const e = entorno();
    let resolver;

    e.estado.pendiente = new Promise(resolve => {
        resolver = resolve;
    });

    const primera = e.contexto.alquilarBici(3, e.boton);
    const segunda = e.contexto.alquilarBici(4, e.boton);

    assert.equal(e.consultas.length, 1);
    assert.equal(e.boton.disabled, true);
    assert.equal(e.atributos.get('aria-busy'), 'true');
    assert.equal(e.boton.innerHTML, 'Comprobando acceso…');

    resolver({data: e.estado.usuario});

    await Promise.all([primera, segunda]);

    assert.deepEqual(e.navegaciones, ['alquiler.html?estacionId=3']);
    assert.equal(e.boton.disabled, false);
    assert.equal(e.boton.innerHTML, 'Ver Bicicletas');
});

test('ID inválido no consulta sesión ni cambia la selección', async () => {
    for (const id of [0, -1, NaN, Infinity, '3', null]) {
        const e = entorno({estacionSeleccionada: '9'});

        await e.contexto.alquilarBici(id);

        assert.equal(e.consultas.length, 0);
        assert.equal(e.navegaciones.length, 0);
        assert.equal(e.datos.get('estacionSeleccionada'), '9');
    }
});

test('un fallo permite reintentar sin dejar el botón bloqueado', async () => {
    const e = entorno();
    e.estado.error = {response: {status: 503}};

    await e.contexto.alquilarBici(3, e.boton);

    e.estado.error = null;

    await e.contexto.alquilarBici(3, e.boton);

    assert.equal(e.consultas.length, 2);
    assert.deepEqual(e.navegaciones, ['alquiler.html?estacionId=3']);
    assert.equal(e.boton.disabled, false);
});
