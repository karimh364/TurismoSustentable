// Variables Globales
let listaAlquileres = [];
let monedaActual = 'USD';

document.addEventListener("DOMContentLoaded", async function() {

    const usuario = await requerirSesion(['ADMIN', 'CLIENTE']);
    if (!usuario) return;

    // Cargar los alquileres y nombre de usuario logueado
    await Promise.all([
        cargarNombreUsuario(usuario),
        cargarAlquileres()
    ]);

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

// Formateador de moneda para el historial
function formatearPrecioHistorial(precioUSD, precioARS) {
    return VistaAlquileres.importe(
        precioUSD,
        precioARS,
        monedaActual
    );
}

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
    const containerActivos = document.getElementById('container-activos');
    const tbodyHistorial = document.getElementById('tbody-historial');
    const mensajeHistorial = document.getElementById('mensaje-historial');

    if (!containerActivos || !tbodyHistorial) return;

    const activos = [];
    const historial = [];

    listaAlquileres.forEach(alquiler => {
        const nuevo = VistaAlquileres.nuevo(alquiler);
        const id = VistaAlquileres.id(alquiler.id);
        const bicicletas = VistaAlquileres.bicicletas(
            alquiler, monedaActual
        );
        const importes = VistaAlquileres.importes(
            alquiler, monedaActual
        );

        const inicio = VistaAlquileres.fecha(
            nuevo ? alquiler.fechaHoraCreacion : alquiler.fechaInicio,
            nuevo
        );

        const fin = VistaAlquileres.fecha(
            nuevo ? alquiler.fechaHoraCierre : alquiler.fechaFin,
            nuevo
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
                        ${nuevo ? 'Creado' : 'Inicio'}: ${inicio}
                    </div>
                    ${nuevo ? `
                        <div>Vencimiento de reserva:
                            ${VistaAlquileres.fecha(
                                alquiler.fechaHoraVencimientoReserva,
                                true
                            )}
                        </div>
                    ` : ''}
                    <div class="my-2">${importes}</div>
                    ${nuevo
                        ? '<small>Consulta del alquiler.</small>'
                        : `
                            <button class="btn btn-danger"
                                onclick="abrirDevolucionAnterior(${id})">
                                Devolver bicicleta
                            </button>
                        `}
                </article>
            `);
        } else {
            historial.push(`
                <tr>
                    <td>${bicicletas}</td>
                    <td class="small">${inicio}</td>
                    <td class="small">${fin}</td>
                    <td>${importes}</td>
                    <td>
                        <span class="badge bg-secondary">
                            ${VistaAlquileres.texto(alquiler.estado)}
                        </span>
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
}

async function abrirDevolucionAnterior(id) {
    const alquiler = listaAlquileres.find(
        elemento => elemento.id === id
    );

    if (!alquiler
            || VistaAlquileres.nuevo(alquiler)
            || alquiler.estado !== 'INICIADO') {
        await Swal.fire(
            'Acción no disponible',
            'Recargá los alquileres antes de continuar.',
            'warning'
        );
        return;
    }

    await abrirModalDevolucion(
        alquiler.id,
        alquiler.codigoBicicleta,
        alquiler.fechaInicio
    );
}

// LÓGICA DEL MODAL DE DEVOLUCIÓN
async function abrirModalDevolucion(idAlquiler, codigoBici, fechaInicioISO) {
    // 1. Setear datos básicos visuales
    document.getElementById('modal-dev-id').value = idAlquiler;
    document.getElementById('modal-dev-bici').textContent = `Bicicleta ${codigoBici}`;

    // 2. Calcular tiempo exacto transcurrido
    const fechaInicio = new Date(fechaInicioISO);
    const ahora = new Date();
    const difMilisegundos = ahora - fechaInicio;
    const minutosUso = Math.max(1, Math.floor(difMilisegundos / 60000)); // Para que como mínimo cobre 1 minuto

    // Mostrar horas y minutos si es más de 60
    let textoTiempo = `${minutosUso} minutos`;
    if (minutosUso >= 60) {
        const horas = Math.floor(minutosUso / 60);
        const minsRestantes = minutosUso % 60;
        textoTiempo = `${horas}h ${minsRestantes}m`;
    }
    document.getElementById('modal-dev-tiempo').textContent = textoTiempo;

    // 3. Cargar estaciones disponibles (Consumiendo el DTO de Spring Boot)
    const select = document.getElementById('select-estacion-destino');
    select.innerHTML = '<option value="">Cargando estaciones...</option>';

    try {
        const response = await axios.get('/api/estaciones');
        select.innerHTML = '<option value="" disabled selected>Seleccioná una estación con lugar...</option>';

        let hayLugares = false;

        response.data.forEach(est => {
            // USAMOS LA PROPIEDAD espaciosLibres
            const lugaresLibres = est.espaciosLibres;

            const option = document.createElement('option');
            option.value = est.nombre; // Guardamos el nombre para enviarlo al backend

            if (lugaresLibres > 0) {
                option.textContent = `${est.nombre} (${lugaresLibres} lugares libres)`;
                hayLugares = true;
            } else {
                option.textContent = `${est.nombre} (LLENA - Sin lugar)`;
                option.disabled = true;
            }
            select.appendChild(option);
        });

        if (!hayLugares) {
            Swal.fire('Atención', 'Actualmente no hay estaciones con lugares disponibles.', 'warning');
        }

    } catch (error) {
        if (manejarErrorSesion(error)) return;
        select.innerHTML = '<option value="">Error al cargar estaciones</option>';
        console.error(error);
    }

    // 4. Mostrar el modal
    const modal = new bootstrap.Modal(document.getElementById('modalDevolucion'));
    modal.show();
}

// CONFIRMAR DEVOLUCIÓN (PATCH AL BACKEND)
async function confirmarDevolucion() {
    const id = document.getElementById('modal-dev-id').value;
    const estacionDestino = document.getElementById('select-estacion-destino').value;

    if (!estacionDestino) {
        Swal.fire('Atención', 'Por favor, seleccioná en qué estación vas a dejar la bicicleta.', 'warning');
        return;
    }

    try {
        Swal.fire({ title: 'Procesando devolución...', didOpen: () => Swal.showLoading() });

        // Enviamos al backend dónde la dejó el usuario.
        const payload = {
            ubicacionDestino: estacionDestino
        };

        const response = await axios.patch(`/api/alquileres/finalizar/${id}`, payload);

        // Cerrar modal
        const modalEl = document.getElementById('modalDevolucion');
        const modal = bootstrap.Modal.getInstance(modalEl);
        modal.hide();

        // Mostrar éxito calculando la moneda que tenga elegida
        const precioFinal = formatearPrecioHistorial(response.data.costoTotal, response.data.costoTotalArs);

        await Swal.fire({
            icon: 'success',
            title: '¡Bici devuelta con éxito!',
            text: `Tu viaje finalizó correctamente. Costo final exacto: ${precioFinal}`,
            confirmButtonColor: '#198754'
        });

        cargarAlquileres(); // Recargar historial para que pase a Finalizados

    } catch (error) {
        if (manejarErrorSesion(error)) return;
        console.error(error);
        Swal.fire('Error', 'No se pudo finalizar el viaje. Intentá nuevamente.', 'error');
    }
}
