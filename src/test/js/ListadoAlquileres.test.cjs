const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const carpetaStatic = path.join(__dirname, '../../main/resources/static');
const fuentes = Object.fromEntries(
    ['alquileres-vista.js', 'alquileres-operaciones.js', 'mis_alquileres.js', 'admin.js'].map(
        archivo => [
            archivo,
            fs.readFileSync(path.join(carpetaStatic, 'js', archivo), 'utf8')
        ]
    )
);

function datosMixtos() {
    function detalle(id, codigo) {
        return {
            id, bicicletaId: id - 100, codigoBicicleta: codigo,
            tipoBicicletaContratado: 'MONTANA',
            estado: 'RESERVADO', precioPorHoraAplicadoUsd: 10,
            duracionEstimadaSegundos: 5400,
            fechaHoraRetiro: null, fechaHoraDevolucionReal: null,
            fechaHoraFinUso: null,
            subtotalEstimadoUsd: 15, subtotalEstimadoArs: 22950,
            subtotalRealUsd: null, subtotalRealArs: null,
            fallaReportada: false
        };
    }

    const reservado = {
        id: 50, usuarioId: 1, modelo: 'DETALLES',
        estado: 'RESERVADO', estacionRetiro: 'Plaza QA',
        fechaHoraCreacion: '2026-11-10T12:00:00',
        fechaHoraVencimientoReserva: '2026-11-10T12:15:00',
        fechaHoraCierre: null, fechaHoraLiquidacion: null,
        cotizacionArsPorUsd: 1530,
        costoEstimadoUsd: 30, costoEstimadoArs: 45900,
        costoFinalUsd: null, costoFinalArs: null,
        detalles: [
            detalle(101, 'QA-DET-01'),
            detalle(102, 'QA-DET-02')
        ]
    };

    const cerrado = JSON.parse(JSON.stringify(reservado));
    cerrado.id = 38;
    cerrado.estado = 'FINALIZADO';
    cerrado.fechaHoraCierre = '2026-11-10T12:50:00';

    // Uso cerrado, liquidación pendiente por revisión de fallas.
    cerrado.detalles.forEach((unidad, indice) => {
        unidad.id = 201 + indice;
        unidad.bicicletaId = 3 + indice;
        unidad.codigoBicicleta = `QA-CERR-0${indice + 1}`;
        unidad.estado = 'DEVUELTO';
        unidad.fechaHoraRetiro = '2026-11-10T12:05:00';
        unidad.fechaHoraDevolucionReal = '2026-11-10T12:50:00';
        unidad.fechaHoraFinUso = '2026-11-10T12:50:00';
        unidad.fallaReportada = true;
    });

    return [
        reservado,
        {
            id: 40, usuarioId: 1, modelo: 'ANTERIOR',
            estado: 'INICIADO', codigoBicicleta: 'QA-ANT-01',
            fechaInicio: '2026-11-10T10:00:00', fechaFin: null,
            costoTotal: 18, costoTotalArs: 27540
        },
        {
            id: 39, usuarioId: 1, modelo: 'ANTERIOR',
            estado: 'FINALIZADO', codigoBicicleta: 'QA-FIN-01',
            fechaInicio: '2026-11-09T10:00:00',
            fechaFin: '2026-11-09T11:00:00',
            costoTotal: 12.34, costoTotalArs: 18880.20
        },
        cerrado
    ];
}

function entorno(pagina, respuesta = [], fallo = null) {
    const peticiones = [];
    const avisos = [];
    const erroresSesion = [];
    const fechasInterpretadas = [];
    const eventos = {};

    const ids = [
        'container-activos', 'tbody-historial', 'mensaje-historial',
        'switch-moneda', 'label-moneda', 'tabla-alquileres-body'
    ];

    const elementos = new Map(ids.map(id => [
        id,
        {
            innerHTML: '', textContent: '', checked: false,
            eventos: {},
            addEventListener(nombre, accion) {
                this.eventos[nombre] = accion;
            }
        }
    ]));

    const urlEsperada = pagina === 'cliente'
        ? '/api/alquileres'
        : '/api/admin/estadisticas/alquileres-todos';

    const axios = {
        async get(url) {
            peticiones.push({ metodo: 'GET', url });

            if (url !== urlEsperada) {
                throw new Error('GET inesperado: ' + url);
            }
            if (fallo) throw fallo;

            return { data: respuesta };
        }
    };

    for (const metodo of ['post', 'put', 'patch', 'delete']) {
        axios[metodo] = async url => {
            peticiones.push({ metodo: metodo.toUpperCase(), url });
            throw new Error('Consultar no debe escribir datos.');
        };
    }

    class FechaRegistrada extends Date {
        constructor(...argumentos) {
            super(...argumentos);
            fechasInterpretadas.push(argumentos[0]);
        }
    }

    const contexto = vm.createContext({
        Date: FechaRegistrada,
        console: { error() {} },
        document: {
            addEventListener(nombre, accion) {
                eventos[nombre] = accion;
            },
            getElementById: id => elementos.get(id) ?? null
        },
        axios,
        Swal: {
            async fire(...argumentos) {
                avisos.push(argumentos);
                return { isConfirmed: false };
            }
        },
        requerirSesion: async () => ({
            nombre: 'QA', roles: ['ADMIN', 'CLIENTE']
        }),
        cargarNombreUsuario: async () => {},
        rolesDelUsuario: u => u.roles,
        manejarErrorSesion(error) {
            erroresSesion.push(error);
            return [401, 403].includes(error.response?.status);
        }
    });

    vm.runInContext(
        fuentes['alquileres-vista.js'],
        contexto,
        { filename: 'alquileres-vista.js' }
    );

    const archivoPagina = pagina === 'cliente'
        ? 'mis_alquileres.js'
        : 'admin.js';
    vm.runInContext(fuentes['alquileres-operaciones.js'], contexto, {filename: 'alquileres-operaciones.js'});

    vm.runInContext(
        fuentes[archivoPagina],
        contexto,
        { filename: archivoPagina }
    );

    const vista = vm.runInContext('VistaAlquileres', contexto);

    function comprobarSoloConsulta() {
        assert.deepEqual(peticiones, [
            { metodo: 'GET', url: urlEsperada }
        ]);
    }

    return {
        contexto, vista, peticiones, avisos, erroresSesion,
        fechasInterpretadas,
        campo: id => elementos.get(id),
        comprobarSoloConsulta,
        async cargar() {
            if (pagina === 'cliente') {
                await eventos.DOMContentLoaded();
            } else {
                // No iniciar usuarios, estaciones u otros módulos.
                await contexto.cargarAlquileres();
            }
            comprobarSoloConsulta();
        }
    };
}

function htmlGenerado(entorno, pagina) {
    return pagina === 'cliente'
        ? entorno.campo('container-activos').innerHTML
            + entorno.campo('tbody-historial').innerHTML
        : entorno.campo('tabla-alquileres-body').innerHTML;
}

for (const pagina of ['cliente', 'admin']) {
    test(`${pagina}: mezcla modelos sin mezclar bicicletas ni acciones`, async () => {
        const datos = datosMixtos();
        const original = JSON.stringify(datos);
        const e = entorno(pagina, datos);

        await e.cargar();

        const html = htmlGenerado(e, pagina);
        for (const codigo of [
            'QA-DET-01', 'QA-DET-02', 'QA-ANT-01',
            'QA-FIN-01', 'QA-CERR-01', 'QA-CERR-02'
        ]) {
            assert.ok(html.includes(codigo), codigo);
        }

        assert.doesNotMatch(html, /undefined|Invalid Date|NaN/);
        assert.equal(e.avisos.length, 0);
        assert.equal(JSON.stringify(datos), original);

        if (pagina === 'cliente') {
            const activos = e.campo('container-activos').innerHTML;
            const historial = e.campo('tbody-historial').innerHTML;

            assert.match(activos, /QA-DET-01/);
            assert.match(activos, /QA-ANT-01/);
            assert.doesNotMatch(activos, /QA-CERR-01|QA-FIN-01/);
            assert.match(historial, /QA-CERR-01/);
            assert.match(historial, /QA-FIN-01/);
            assert.doesNotMatch(historial, /QA-DET-01|QA-ANT-01/);

            assert.match(activos, /abrirDevolucionAnterior\(40\)/);
            assert.equal((activos.match(/abrirDevolucionAnterior\(/g) || []).length, 1);
            assert.match(activos, /OperacionesAlquiler.finanzas\(50\)/);
            assert.match(historial, /OperacionesAlquiler.finanzas\(38\)/);
            assert.doesNotMatch(historial, /OperacionesAlquiler\.(retirar|devolver|anticipo)\(/);
            assert.match(historial, /Pendiente de liquidación/);
        } else {
            const filas = [...html.matchAll(/<tr>([\s\S]*?)<\/tr>/g)]
                .map(coincidencia => coincidencia[1]);

            assert.equal(filas.length, 4);
            assert.match(filas[0], /QA-DET-01/);
            assert.match(filas[0], /QA-DET-02/);
            assert.doesNotMatch(filas[0], /QA-CERR-01|finalizarAlquilerAdmin/);
            assert.match(filas[3], /QA-CERR-01/);
            assert.match(filas[3], /QA-CERR-02/);
            assert.doesNotMatch(filas[3], /QA-DET-01|finalizarAlquilerAdmin/);
            assert.match(filas[3], /FINALIZADO/);
            assert.match(filas[3], /Pendiente de liquidación/);

            assert.match(html, /finalizarAlquilerAdmin\(40\)/);
            assert.equal((html.match(/finalizarAlquilerAdmin\(/g) || []).length, 1);
            assert.match(filas[0], /OperacionesAlquiler.cancelarAdmin\(50,101\)/);
            assert.doesNotMatch(html, /OperacionesAlquiler\.(retirar|anticipo|adicional)\(/);
        }
    });

    test(`${pagina}: lista vacía se informa sin error`, async () => {
        const e = entorno(pagina, []);
        await e.cargar();

        assert.equal(e.avisos.length, 0);

        if (pagina === 'cliente') {
            assert.match(
                e.campo('container-activos').innerHTML,
                /No tenés reservas ni alquileres en curso/
            );
            assert.equal(e.campo('tbody-historial').innerHTML, '');
            assert.equal(
                e.campo('mensaje-historial').textContent,
                'No tenés alquileres cerrados.'
            );
        } else {
            assert.match(
                e.campo('tabla-alquileres-body').innerHTML,
                /No hay alquileres visibles/
            );
        }
    });

    test(`${pagina}: formatos inválidos se informan como error`, async () => {
        for (const respuesta of [
            {},
            [{ id: 99, modelo: 'DESCONOCIDO' }]
        ]) {
            const e = entorno(pagina, respuesta);
            await e.cargar();

            if (pagina === 'cliente') {
                assert.equal(e.avisos.length, 1);
                assert.equal(
                    e.avisos[0][0],
                    'No se pudieron cargar tus viajes'
                );
            } else {
                assert.match(
                    e.campo('tabla-alquileres-body').innerHTML,
                    /No se pudieron cargar los alquileres/
                );
            }
        }
    });

    test(`${pagina}: 401 y 403 se delegan al manejador de sesión`, async () => {
        for (const status of [401, 403]) {
            const error = { response: { status } };
            const e = entorno(pagina, [], error);

            await e.cargar();

            assert.equal(e.erroresSesion.length, 1);
            assert.equal(e.erroresSesion[0], error);
            assert.equal(e.avisos.length, 0);
        }
    });

    test(`${pagina}: los textos recibidos se escapan`, async () => {
        const datos = datosMixtos();
        const ataque = '<img src=x onerror="alert(1)">';

        datos[0].estacionRetiro = ataque;
        datos[0].detalles[0].codigoBicicleta = ataque;
        datos[1].codigoBicicleta = ataque;

        const e = entorno(pagina, datos);
        await e.cargar();

        const html = htmlGenerado(e, pagina);
        assert.match(html, /&lt;img/);
        assert.doesNotMatch(html, /<img\b/);
        assert.doesNotMatch(html, /onerror="alert\(1\)"/);
        assert.equal(
            e.vista.texto(`<>&"'`),
            '&lt;&gt;&amp;&quot;&#39;'
        );
    });
}

test('el selector de moneda usa importes guardados sin nuevas consultas', async () => {
    const datos = datosMixtos();
    const original = JSON.stringify(datos);
    const e = entorno('cliente', datos);

    await e.cargar();
    assert.match(e.campo('container-activos').innerHTML, /US\$ 30,00/);

    const cambiar = e.campo('switch-moneda').eventos.change;
    cambiar({ target: { checked: true } });

    assert.equal(e.campo('label-moneda').textContent, 'AR$');
    assert.match(
        e.campo('container-activos').innerHTML,
        /AR\$ 45\.900,00/
    );

    cambiar({ target: { checked: false } });
    assert.equal(e.campo('label-moneda').textContent, 'US$');
    assert.match(e.campo('container-activos').innerHTML, /US\$ 30,00/);

    e.comprobarSoloConsulta();
    assert.equal(JSON.stringify(datos), original);
});

test('importe pendiente no es cero y falta de ARS no provoca conversión', () => {
    const e = entorno('cliente');

    assert.equal(
        e.vista.importe(null, null),
        'Pendiente de liquidación'
    );
    assert.equal(e.vista.importe(0, 0), 'US$ 0,00');
    assert.equal(e.vista.importe(0, 0, 'ARS'), 'AR$ 0,00');
    assert.equal(
        e.vista.importe(10, null, 'ARS'),
        'Pendiente de liquidación'
    );
    assert.match(
        e.vista.importes(datosMixtos()[3]),
        /Final: Pendiente de liquidación/
    );
    assert.equal(e.peticiones.length, 0);
});

test('fechas ausentes y UTC se interpretan sin alterar el modelo anterior', () => {
    const e = entorno('cliente');
    const fecha = '2026-11-10T12:00:00';

    assert.equal(e.vista.fecha(null, true), '—');
    assert.equal(e.fechasInterpretadas.length, 0);

    e.vista.fecha(fecha, true);
    assert.equal(e.fechasInterpretadas.at(-1), fecha + 'Z');

    e.vista.fecha(fecha, false);
    assert.equal(e.fechasInterpretadas.at(-1), fecha);

    for (const valor of [fecha + 'Z', fecha + '-03:00']) {
        e.vista.fecha(valor, true);
        assert.equal(e.fechasInterpretadas.at(-1), valor);
    }

    assert.equal(
        e.vista.fecha('dato-inválido', true),
        'Fecha no disponible'
    );
});

test('solo el anterior iniciado puede abrir la devolución anterior', async () => {
    const e = entorno('cliente', datosMixtos());
    await e.cargar();

    const llamadasModal = [];

    // Capturar el límite con el modal, no reemplazar la guarda.
    e.contexto.abrirModalDevolucion = async (...argumentos) => {
        llamadasModal.push(argumentos);
    };

    await e.contexto.abrirDevolucionAnterior(40);

    assert.deepEqual(llamadasModal, [[
        40, 'QA-ANT-01', '2026-11-10T10:00:00'
    ]]);

    for (const id of [50, 38, 39, 999]) {
        await e.contexto.abrirDevolucionAnterior(id);
    }

    assert.equal(llamadasModal.length, 1);
    assert.equal(e.avisos.length, 4);
    e.comprobarSoloConsulta();
});

test('ambos HTML cargan una sola vista compartida antes del consumidor', () => {
    for (const [archivo, consumidor, ids] of [
        [
            'mis_alquileres.html', 'js/mis_alquileres.js',
            [
                'container-activos', 'tbody-historial',
                'mensaje-historial', 'switch-moneda', 'label-moneda'
            ]
        ],
        ['admin.html', 'js/admin.js', ['tabla-alquileres-body']]
    ]) {
        const html = fs.readFileSync(
            path.join(carpetaStatic, archivo), 'utf8'
        );
        const scripts = [
            ...html.matchAll(/<script\b[^>]*\bsrc=["']([^"']+)["']/g)
        ].map(coincidencia => coincidencia[1]);

        assert.equal(
            scripts.filter(src => src === 'js/alquileres-vista.js').length,
            1
        );
        assert.equal(
            scripts.filter(src => src === consumidor).length,
            1
        );
        assert.ok(
            scripts.indexOf('js/alquileres-vista.js')
                < scripts.indexOf(consumidor)
        );

        for (const id of ids) {
            assert.ok(html.includes(`id="${id}"`), id);
        }
    }
});
