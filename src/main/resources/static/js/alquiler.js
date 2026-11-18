// El navegador orienta la selección; el servidor valida y fija los importes de la reserva.
const LIMITE_BICICLETAS_RESERVA = 10;
const TIPOS_RESERVA = new Map([
    ['MONTANA', {nombre: 'Montaña', imagen: 'montana.jpg'}],
    ['PASEO', {nombre: 'Paseo', imagen: 'paseo.jpg'}],
    ['ELECTRICA', {nombre: 'Eléctrica', imagen: 'electrica.jpg'}]
]);
const formatoImporteReserva = new Intl.NumberFormat('es-AR', {
    minimumFractionDigits: 2, maximumFractionDigits: 2
});
const campoReserva = id => document.getElementById(id);
let estaciones = [];
let bicicletas = [];
let estacionActiva = null;
const seleccionBicicletas = new Set();
let cotizacionReserva = null;
let consultandoCotizacion = false;
let creandoReserva = false;

document.addEventListener('DOMContentLoaded', async () => {
    const usuario = await requerirSesion(['CLIENTE']);
    if (!usuario) return;
    await cargarNombreUsuario(usuario);

    campoReserva('buscar-estacion').addEventListener('input', mostrarEstaciones);
    campoReserva('estaciones-container').addEventListener('click', evento => {
        const opcion = evento.target.closest('button[data-estacion-id]');
        if (opcion) return elegirEstacion(Number(opcion.dataset.estacionId), evento.detail === 0);
    });
    campoReserva('bicicletas-container').addEventListener('change', evento => {
        const id = evento.target.dataset.bicicletaId;
        if (id) seleccionarBicicleta(Number(id));
    });
    campoReserva('filtro-tipo').addEventListener('change', mostrarBicicletas);
    campoReserva('switch-moneda').addEventListener('change', mostrarBicicletas);
    campoReserva('crear-reserva').addEventListener('click', confirmarReserva);
    await cargarCatalogoReserva();
});

async function cargarCatalogoReserva() {
    try {
        const [bicis, catalogo] = await Promise.all([
            axios.get('/api/bicicletas/disponibles'), axios.get('/api/estaciones')
        ]);
        estaciones = catalogo.data.filter(e => Number.isSafeInteger(e.id) && e.id > 0 && e.esPublica === true)
            .sort((a, b) => a.nombre.localeCompare(b.nombre, 'es-AR'));
        bicicletas = bicis.data.filter(b => Number.isSafeInteger(b.id) && b.id > 0);
        consultandoCotizacion = true;

        // El mapa ya envía el ID en la URL. Una visita normal no hereda una elección anterior.
        const idMapa = new URLSearchParams(window.location.search).get('estacionId');
        estacionActiva = /^[1-9]\d*$/.test(idMapa ?? '')
            ? estaciones.find(e => e.id === Number(idMapa)) ?? null : null;
        campoReserva('buscar-estacion').disabled = false;
        mostrarEstaciones();
        if (estacionActiva) {
            mostrarEstacionElegida();
            irASeleccionDeBicicletas();
        } else if (idMapa !== null && estaciones.length) {
            campoReserva('estado-estaciones').textContent = 'La estación del enlace no está disponible. Elegí otra estación.';
        }
    } catch (e) {
        campoReserva('estado-estaciones').textContent = 'No pudimos cargar el catálogo. Recargá la página para volver a intentar.';
        await errorReserva(e, 'No se pudieron cargar las estaciones');
        return;
    }

    try {
        const {data} = await axios.get('/api/cotizacion');
        const cotizacion = Number(data);
        if (Number.isFinite(cotizacion) && cotizacion > 0) cotizacionReserva = cotizacion;
    } catch (e) {
        manejarErrorSesion(e);
    } finally {
        consultandoCotizacion = false;
        if (estacionActiva) {
            if (campoReserva('switch-moneda').checked && cotizacionReserva !== null) mostrarBicicletas();
            else mostrarAvisoCotizacion();
        }
    }
}

function escaparReserva(v) {
    return String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}

function normalizarBusqueda(valor) {
    return String(valor ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
}

function bicicletasEnEstacion(id) { return bicicletas.filter(b => b.estacionId === id); }

function mostrarEstaciones() {
    const palabras = normalizarBusqueda(campoReserva('buscar-estacion').value).split(/\s+/).filter(Boolean);
    const visibles = estaciones.filter(estacion => {
        const texto = normalizarBusqueda(`${estacion.nombre} ${estacion.direccion ?? ''}`);
        return palabras.every(palabra => texto.includes(palabra));
    });
    campoReserva('estaciones-container').innerHTML = visibles.map(estacion => {
        const disponibles = bicicletasEnEstacion(estacion.id).length;
        return `<div class="col-sm-6 col-lg-4">
            <button type="button" class="estacion-opcion" data-estacion-id="${estacion.id}" aria-pressed="${estacionActiva?.id === estacion.id}">
                <span class="d-block fw-bold">${escaparReserva(estacion.nombre)}</span>
                <span class="d-block text-muted small mb-2">${escaparReserva(estacion.direccion)}</span>
                <span class="estacion-disponibilidad small fw-semibold">${disponibles === 1 ? '1 bicicleta disponible' : `${disponibles} bicicletas disponibles`}</span>
            </button></div>`;
    }).join('');
    campoReserva('estado-estaciones').textContent = !estaciones.length ? 'No hay estaciones públicas disponibles.'
        : !visibles.length ? 'No encontramos estaciones con esa búsqueda.'
        : visibles.length === 1 ? '1 estación encontrada.' : `${visibles.length} estaciones encontradas.`;
}

async function elegirEstacion(id, desdeTeclado = false) {
    if (creandoReserva) return;
    const estacion = estaciones.find(e => e.id === id);
    if (!estacion) return;
    if (estacionActiva?.id !== id) {
        if (seleccionBicicletas.size) {
            const resultado = await Swal.fire({title: '¿Cambiar de estación?', icon: 'question',
                text: 'La reserva solo puede incluir bicicletas de una estación. Si cambiás, se quitarán las bicicletas seleccionadas.',
                showCancelButton: true, focusCancel: true, confirmButtonText: 'Cambiar de estación',
                cancelButtonText: 'Conservar selección', confirmButtonColor: '#198754', scrollbarPadding: false});
            if (!resultado.isConfirmed) return;
        }
        seleccionBicicletas.clear();
        estacionActiva = estacion;
        campoReserva('filtro-tipo').value = 'todos';
        // No reconstruir la lista al cambiar la selección, así se conserva su posición.
        campoReserva('estaciones-container').querySelectorAll('button[data-estacion-id]').forEach(boton => {
            boton.setAttribute('aria-pressed', String(Number(boton.dataset.estacionId) === id));
        });
        mostrarEstacionElegida();
    }
    irASeleccionDeBicicletas(desdeTeclado);
}

function irASeleccionDeBicicletas(desdeTeclado = false) {
    const titulo = campoReserva('titulo-bicicletas');
    if (desdeTeclado) titulo.focus({preventScroll: true});
    titulo.scrollIntoView({block: 'start'});
}

function mostrarEstacionElegida() {
    campoReserva('seleccion-bicicletas').hidden = false;
    campoReserva('estacion-elegida').textContent = estacionActiva.nombre;
    campoReserva('direccion-elegida').textContent = estacionActiva.direccion ?? '';
    mostrarBicicletas();
}

function unidadesSeleccionadas() { return bicicletas.filter(b => seleccionBicicletas.has(b.id)); }

function seleccionarBicicleta(id) {
    if (creandoReserva) return;
    const bici = bicicletas.find(b => b.id === id);
    if (!bici || bici.estacionId !== estacionActiva?.id) return;
    if (seleccionBicicletas.has(id)) seleccionBicicletas.delete(id);
    else if (seleccionBicicletas.size < LIMITE_BICICLETAS_RESERVA) seleccionBicicletas.add(id);
    actualizarSeleccionReserva();
}

function actualizarSeleccionReserva() {
    const cantidad = seleccionBicicletas.size;
    campoReserva('bicicletas-container').querySelectorAll('input[data-bicicleta-id]').forEach(casilla => {
        casilla.checked = seleccionBicicletas.has(Number(casilla.dataset.bicicletaId));
        casilla.disabled = !casilla.checked && cantidad >= LIMITE_BICICLETAS_RESERVA;
        casilla.closest('article').classList.toggle('is-seleccionada', casilla.checked);
    });
    campoReserva('cantidad-seleccionada').textContent = `${cantidad} de ${LIMITE_BICICLETAS_RESERVA} seleccionadas`;
    campoReserva('seleccion-reserva').textContent = unidadesSeleccionadas().map(b => b.codigoBicicleta).join(', ')
        || 'Ninguna bicicleta seleccionada';
    campoReserva('crear-reserva').disabled = creandoReserva || !cantidad;
}

function mostrarAvisoCotizacion() {
    campoReserva('aviso-reserva').textContent = cotizacionReserva !== null ? '' : consultandoCotizacion
        ? 'Consultando la referencia en ARS…' : 'Referencia en ARS no disponible. Al confirmar se verificará la cotización.';
}

function mostrarBicicletas() {
    if (!estacionActiva) return;
    const tipo = campoReserva('filtro-tipo').value;
    const ars = campoReserva('switch-moneda').checked && cotizacionReserva !== null;
    campoReserva('label-moneda').textContent = ars ? 'AR$' : 'US$';
    mostrarAvisoCotizacion();

    const deEstacion = bicicletasEnEstacion(estacionActiva.id);
    const visibles = deEstacion.filter(b => tipo === 'todos' || tipo === b.tipo);
    campoReserva('estado-bicicletas').textContent = !deEstacion.length ? 'No hay bicicletas disponibles en esta estación.'
        : !visibles.length ? 'No hay bicicletas disponibles de este tipo en la estación.'
        : visibles.length === 1 ? '1 bicicleta disponible.' : `${visibles.length} bicicletas disponibles.`;
    campoReserva('bicicletas-container').innerHTML = visibles.map(b => {
        const tipoBici = TIPOS_RESERVA.get(b.tipo);
        const imagen = tipoBici ? `<img src="img/${tipoBici.imagen}" class="bicicleta-foto" alt="" loading="lazy">` : '';
        const precio = Number(b.precio) * (ars ? cotizacionReserva : 1);
        return `<div class="col-sm-6 col-lg-4"><article class="card bicicleta-reserva h-100 shadow-sm">
            ${imagen}<div class="card-body">
                <h4 class="h5 mb-1">${escaparReserva(b.codigoBicicleta)}</h4>
                <p class="text-muted mb-2">${escaparReserva(tipoBici?.nombre ?? b.tipo)}</p>
                <p class="fw-semibold mb-1">${importeReserva(precio, ars)}/hora</p>
                <label class="d-flex align-items-center gap-2">
                    <input type="checkbox" class="form-check-input mt-0" data-bicicleta-id="${b.id}" aria-label="Seleccionar ${escaparReserva(b.codigoBicicleta)}"> Seleccionar
                </label>
            </div></article></div>`;
    }).join('');
    actualizarSeleccionReserva();
}

function solicitudReserva() {
    const unidades = unidadesSeleccionadas();
    const minutos = Number(campoReserva('duracion-reserva').value);
    if (!unidades.length) throw new Error('Seleccioná al menos una bicicleta.');
    if (unidades.length > LIMITE_BICICLETAS_RESERVA) throw new Error('Podés seleccionar hasta 10 bicicletas.');
    if (!Number.isSafeInteger(minutos) || minutos <= 0 || minutos * 60 > 2147483647)
        throw new Error('Ingresá una duración estimada válida en minutos enteros.');
    if (!estacionActiva || unidades.some(b => b.estacionId !== estacionActiva.id))
        throw new Error('Todas las bicicletas deben pertenecer a la misma estación.');
    return {estacionRetiroId: estacionActiva.id, bicicletaIds: unidades.map(b => b.id), duracionEstimadaSegundos: minutos * 60};
}

function importeReserva(valor, ars) {
    return `${ars ? 'AR$' : 'US$'} ${formatoImporteReserva.format(valor)}`;
}

function resumenReserva(datos) {
    const minutos = datos.duracionEstimadaSegundos / 60;
    const unidades = unidadesSeleccionadas();
    // Como en el servidor, se redondea cada subtotal y después se suman.
    const total = cotizacion => unidades.reduce((centavos, b) => {
        const subtotal = Number(b.precio) * datos.duracionEstimadaSegundos * cotizacion / 3600;
        return centavos + Math.round((subtotal + Number.EPSILON) * 100);
    }, 0) / 100;
    const detalle = unidades.map(b => escaparReserva(b.codigoBicicleta)).join(', ');
    const ars = cotizacionReserva === null ? 'Referencia en ARS no disponible.' : importeReserva(total(cotizacionReserva), true);
    return `<dl class="reserva-resumen mb-3">
        <dt>Estación de retiro</dt><dd>${escaparReserva(estacionActiva.nombre)}${estacionActiva.direccion ? ` · ${escaparReserva(estacionActiva.direccion)}` : ''}</dd>
        <dt>Bicicletas (${unidades.length})</dt><dd>${detalle}</dd>
        <dt>Duración estimada</dt><dd>${minutos} ${minutos === 1 ? 'minuto' : 'minutos'}</dd>
        <dt>Total estimado</dt><dd><strong>${importeReserva(total(1), false)}</strong> · ${ars}</dd>
        </dl>
        <p class="text-start small mb-0">El servidor valida disponibilidad y cotización al reservar. El importe final depende del uso real. Tenés 15 minutos para gestionar el anticipo y retirar las bicicletas.</p>`;
}

async function confirmarReserva() {
    if (creandoReserva) return;
    if (!campoReserva('duracion-reserva').reportValidity()) return;
    creandoReserva = true;
    const boton = campoReserva('crear-reserva');
    boton.disabled = true;
    try {
        const datos = solicitudReserva();
        const r = await Swal.fire({title: 'Confirmar reserva', icon: 'question', showCancelButton: true,
            html: resumenReserva(datos), confirmButtonText: 'Confirmar reserva', cancelButtonText: 'Volver a editar',
            confirmButtonColor: '#198754', scrollbarPadding: false});
        if (!r.isConfirmed) return;
        boton.textContent = 'Guardando reserva…';
        boton.setAttribute('aria-busy', 'true');
        const {data} = await axios.post('/api/alquileres', datos);
        await Swal.fire('Reserva creada', `Alquiler #${data.id}. Continuá con anticipo y retiro desde Mis viajes.`, 'success');
        window.location.assign('mis_alquileres.html');
    } catch (e) { await errorReserva(e); }
    finally {
        creandoReserva = false;
        boton.textContent = 'Reservar selección';
        boton.removeAttribute('aria-busy');
        actualizarSeleccionReserva();
        boton.focus();
    }
}

async function errorReserva(e, titulo = 'No se pudo reservar') {
    if (manejarErrorSesion(e)) return;
    const dato = e.response?.data;
    await Swal.fire(titulo, typeof dato === 'string' ? dato
        : Array.isArray(dato?.validationErrors) ? dato.validationErrors.map(v => String(v).replace(/^[^:]+:\s*/, '')).join(' ')
        : e.message || 'Intentá nuevamente.', 'error');
}
