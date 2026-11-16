// Reserva de varias unidades de una misma estación. El servidor fija importes y permisos.
let bicicletas = [];
const seleccionBicicletas = new Set();
let cotizacionReserva = null;
let creandoReserva = false;
document.addEventListener('DOMContentLoaded', async () => {
    const usuario = await requerirSesion(['CLIENTE']);
    if (!usuario) return;
    await cargarNombreUsuario(usuario);
    try {
        const [bicis, estaciones] = await Promise.all([axios.get('/api/bicicletas/disponibles'), axios.get('/api/estaciones')]);
        bicicletas = bicis.data;
        estaciones.data.forEach(e => {
            const opcion = document.createElement('option');
            opcion.value = e.id; opcion.textContent = e.nombre;
            document.getElementById('filtro-estacion').appendChild(opcion);
        });
        ['filtro-estacion', 'filtro-tipo', 'switch-moneda'].forEach(n =>
            document.getElementById(n).addEventListener('change', mostrarBicicletas));
        try {
            const {data} = await axios.get('/api/cotizacion');
            if (Number(data) > 0) cotizacionReserva = Number(data);
        } catch (e) {
            if (manejarErrorSesion(e)) return;
            document.getElementById('aviso-reserva').textContent = 'Referencia ARS no disponible. Al confirmar se verificará nuevamente la cotización.';
        }
        mostrarBicicletas();
    } catch (e) { await errorReserva(e); }
});
function escaparReserva(v) {
    return String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
}
function unidadesSeleccionadas() { return bicicletas.filter(b => seleccionBicicletas.has(b.id)); }
function seleccionarBicicleta(id) {
    const bici = bicicletas.find(b => b.id === id);
    if (!bici) return;
    if (seleccionBicicletas.has(id)) seleccionBicicletas.delete(id);
    else if (unidadesSeleccionadas().some(b => b.estacionId !== bici.estacionId)) {
        Swal.fire('Una estación por reserva', 'Quitá la selección anterior para elegir otra estación.', 'info');
    } else seleccionBicicletas.add(id);
    mostrarBicicletas();
}
function mostrarBicicletas() {
    const estacion = document.getElementById('filtro-estacion').value;
    const tipo = document.getElementById('filtro-tipo').value;
    const ars = document.getElementById('switch-moneda').checked;
    document.getElementById('label-moneda').textContent = ars ? 'AR$' : 'US$';
    document.getElementById('bicicletas-container').innerHTML = bicicletas
        .filter(b => (estacion === 'todas' || Number(estacion) === b.estacionId) && (tipo === 'todos' || tipo === b.tipo))
        .map(b => {
            if (!Number.isSafeInteger(b.id) || b.id <= 0) throw new Error('Bicicleta sin identificación válida.');
            const precio = ars ? (cotizacionReserva == null ? null : Number(b.precio) * cotizacionReserva) : Number(b.precio);
            return `<div class="col-md-4"><article class="card h-100"><div class="card-body">
                <h5>${escaparReserva(b.codigoBicicleta)}</h5><p>${escaparReserva(b.tipo)} · ${escaparReserva(b.ubicacion)}</p>
                <p>${precio == null ? 'Referencia ARS no disponible' : (ars ? 'AR$ ' : 'US$ ') + precio.toLocaleString('es-AR', {minimumFractionDigits:2,maximumFractionDigits:2}) + '/hora'}</p>
                <label><input type="checkbox" onchange="seleccionarBicicleta(${b.id})" ${seleccionBicicletas.has(b.id) ? 'checked' : ''}> Seleccionar</label>
            </div></article></div>`;
        }).join('') || '<p>No hay bicicletas disponibles para estos filtros.</p>';
    document.getElementById('seleccion-reserva').textContent =
        unidadesSeleccionadas().map(b => b.codigoBicicleta).join(', ') || 'Ninguna bicicleta seleccionada';
}
function solicitudReserva() {
    const unidades = unidadesSeleccionadas();
    const minutos = Number(document.getElementById('duracion-reserva').value);
    if (!unidades.length) throw new Error('Seleccioná al menos una bicicleta.');
    if (!Number.isSafeInteger(minutos) || minutos <= 0 || minutos * 60 > 2147483647)
        throw new Error('Ingresá una duración estimada válida en minutos enteros.');
    if (!Number.isSafeInteger(unidades[0].estacionId) || unidades.some(b => b.estacionId !== unidades[0].estacionId))
        throw new Error('Todas las bicicletas deben pertenecer a la misma estación.');
    return {estacionRetiroId: unidades[0].estacionId, bicicletaIds: unidades.map(b => b.id), duracionEstimadaSegundos: minutos * 60};
}
async function confirmarReserva() {
    if (creandoReserva) return;
    creandoReserva = true;
    const boton = document.getElementById('crear-reserva');
    boton.disabled = true;
    try {
        const datos = solicitudReserva();
        const r = await Swal.fire({title:'Confirmar reserva',icon:'question',showCancelButton:true,
            text:'Duración estimada común. Tenés 15 minutos para pagar el anticipo y retirar. El uso comienza al retirar cada bicicleta.',
            confirmButtonText:'Reservar',cancelButtonText:'Volver'});
        if (!r.isConfirmed) return;
        const {data} = await axios.post('/api/alquileres', datos);
        await Swal.fire('Reserva creada', `Alquiler #${data.id}. Continuá con anticipo y retiro desde Mis viajes.`, 'success');
        window.location.assign('mis_alquileres.html');
    } catch (e) { await errorReserva(e); }
    finally { creandoReserva = false; boton.disabled = false; }
}
async function errorReserva(e) {
    if (manejarErrorSesion(e)) return;
    const dato = e.response?.data;
    await Swal.fire('No se pudo reservar', typeof dato === 'string' ? dato
        : Array.isArray(dato?.validationErrors) ? dato.validationErrors.map(v => String(v).replace(/^[^:]+:\s*/, '')).join(' ')
        : e.message || 'Intentá nuevamente.', 'error');
}
