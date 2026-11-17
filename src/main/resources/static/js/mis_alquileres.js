// Variables Globales
let listaAlquileres = [];
let monedaActual = 'USD';

document.addEventListener("DOMContentLoaded", async function() {

    const usuario = await requerirSesion(['ADMIN', 'CLIENTE']);
    if (!usuario) return;
    const botonSaldo = document.getElementById('consultar-saldo-simulado');
    if (botonSaldo) botonSaldo.hidden = !rolesDelUsuario(usuario).includes('CLIENTE');
    if (!rolesDelUsuario(usuario).includes('CLIENTE') && typeof document.querySelectorAll === 'function') {
        document.querySelectorAll('a[href="alquiler.html"]').forEach(enlace => { enlace.hidden = true; });
    }
    OperacionesAlquiler.configurar(usuario, async () => {
        await cargarAlquileres();
        if (rolesDelUsuario(usuario).includes('CLIENTE')) await cargarPendientesFinancieros();
    });

    // Cargar los alquileres y nombre de usuario logueado
    await Promise.all([
        cargarNombreUsuario(usuario),
        cargarAlquileres()
    ]);
    if (rolesDelUsuario(usuario).includes('CLIENTE')) await cargarPendientesFinancieros();

    // Listener del Switch de Moneda
    const switchMoneda = document.getElementById('switch-moneda');
    if (switchMoneda) {
        switchMoneda.addEventListener('change', function(e) {
            monedaActual = e.target.checked ? 'ARS' : 'USD';
            document.getElementById('label-moneda').textContent = monedaActual === 'ARS' ? 'AR$' : 'US$';

            // Re-renderizamos para aplicar la nueva moneda en la tabla visualmente
            renderizarAlquileres();
        });
    }
});

// CARGA Y RENDERIZADO DE ALQUILERES
async function cargarAlquileres() {
    try {
        const response = await axios.get('/api/alquileres');
        if (!Array.isArray(response.data)) {
            throw new Error('Respuesta de alquileres inválida.');
        }
        listaAlquileres = response.data;
        renderizarAlquileres();
    } catch (error) {
        if (manejarErrorSesion(error)) return;
        console.error(error);
        Swal.fire('No se pudieron cargar tus viajes', 'Verificá tu conexión e intentá nuevamente.', 'error');
    }
}

function renderizarAlquileres() {
    const abiertos = typeof OperacionesAlquiler === 'undefined' ? [] : OperacionesAlquiler.abiertos();
    const containerActivos = document.getElementById('container-activos');
    const tbodyHistorial = document.getElementById('tbody-historial');
    const mensajeHistorial = document.getElementById('mensaje-historial');

    if (!containerActivos || !tbodyHistorial) return;

    const activos = [];
    const historial = [];

    listaAlquileres.forEach(alquiler => {
        VistaAlquileres.validar(alquiler);
        const id = VistaAlquileres.id(alquiler.id);
        const bicicletas = VistaAlquileres.bicicletas(
            alquiler, monedaActual, d => typeof OperacionesAlquiler === 'undefined' ? '' : OperacionesAlquiler.accionesDetalle(alquiler, d)
        );
        const importes = VistaAlquileres.importes(
            alquiler, monedaActual
        );

        const creacion = VistaAlquileres.fecha(
            alquiler.fechaHoraCreacion
        );

        const cierre = VistaAlquileres.fecha(
            alquiler.fechaHoraCierre
        );

        if (VistaAlquileres.activo(alquiler)) {
            activos.push(`
                <article class="alert alert-success text-start shadow-sm">
                    <h5>
                        Alquiler #${id}
                        · ${VistaAlquileres.texto(alquiler.estado)}
                    </h5>
                    ${bicicletas}
                    <div class="mt-2">
                        Creado: ${creacion}
                    </div>
                    <div>Vencimiento de reserva:
                        ${VistaAlquileres.fecha(alquiler.fechaHoraVencimientoReserva)}
                    </div>
                    <div class="my-2">${importes}</div>
                    ${typeof OperacionesAlquiler === 'undefined' ? '' : OperacionesAlquiler.acciones(alquiler)}
                </article>
            `);
        } else {
            historial.push(`
                <tr>
                    <td>${bicicletas}</td>
                    <td class="small">${creacion}</td>
                    <td class="small">${cierre}</td>
                    <td>${importes}</td>
                    <td>
                        <span class="badge bg-secondary">
                            ${VistaAlquileres.texto(alquiler.estado)}
                        </span>
                        ${typeof OperacionesAlquiler !== 'undefined' ? OperacionesAlquiler.acciones(alquiler) : ''}
                    </td>
                </tr>
            `);
        }
    });

    containerActivos.innerHTML = activos.join('')
        || '<p class="mb-0">No tenés reservas ni alquileres en curso.</p>';

    tbodyHistorial.innerHTML = historial.join('');

    if (mensajeHistorial) {
        mensajeHistorial.textContent = historial.length
            ? ''
            : 'No tenés alquileres cerrados.';
    }
    if (typeof OperacionesAlquiler !== 'undefined') OperacionesAlquiler.restaurarAbiertos(abiertos);
}

async function cargarPendientesFinancieros() {
    const destino = document.getElementById('pendientes-financieros');
    if (!destino) return;
    try {
        const {data} = await axios.get('/api/alquileres/pendientes-financieros');
        destino.innerHTML = data.length ? '<h3 class="h5">Saldos y reintegros pendientes (también de alquileres ocultos)</h3>' +
            data.map(l => `<div>Alquiler #${VistaAlquileres.id(l.alquilerId)} · ${VistaAlquileres.texto(l.situacionFinanciera)}
                <button class="btn btn-sm btn-outline-success m-1" onclick="OperacionesAlquiler.finanzas(${VistaAlquileres.id(l.alquilerId)})">Consultar / resolver</button></div>`).join('') : '';
    } catch (e) {
        if (!manejarErrorSesion(e)) destino.textContent = 'No se pudieron consultar los saldos pendientes.';
    }
}
