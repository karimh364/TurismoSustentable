// Presentación compartida. Los permisos, importes y transiciones los decide el servidor.
const OperacionesAlquiler = (() => {
    let usuario = null;
    let administracion = false;
    let recargar = async () => {};
    const pendientes = new Set();
    const t = valor => VistaAlquileres.texto(valor);
    const id = valor => VistaAlquileres.id(valor);
    const dinero = valor => VistaAlquileres.importe(null, valor, 'ARS');
    const cliente = () => !administracion && usuario && rolesDelUsuario(usuario).includes('CLIENTE');
    const propio = alquiler => cliente() && Number(usuario.id) === Number(alquiler.usuarioId);

    function configurar(perfil, refrescar, admin = false) { usuario = perfil; recargar = refrescar; administracion = admin; }
    function boton(texto, accion, alquilerId, detalleId) {
        return `<button type="button" class="btn btn-sm btn-outline-success m-1" onclick="OperacionesAlquiler.${accion}(${id(alquilerId)}${detalleId == null ? '' : ',' + id(detalleId)})">${texto}</button>`;
    }
    function accionesDetalle(a, d, admin = false) {
        let html = '';
        const pendiente = ['RESERVADO', 'PENDIENTE_RETIRO'].includes(d.estado);
        if (propio(a) && !admin && pendiente) {
            html += boton('Retirar', 'retirar', a.id, d.id);
            html += boton('Cancelar esta bicicleta', 'cancelar', a.id, d.id);
        }
        if (admin && pendiente) html += boton('Cancelar por operación/seguridad', 'cancelarAdmin', a.id, d.id);
        if (d.estado === 'EN_CURSO' && (admin || propio(a))) html += boton('Devolver', 'devolver', a.id, d.id);
        return html;
    }
    function acciones(a, admin = false) {
        let html = boton('Liquidación y reembolsos', 'finanzas', a.id);
        if (propio(a) && !admin && a.detalles.some(d => ['RESERVADO', 'PENDIENTE_RETIRO'].includes(d.estado))) {
            html += boton('Pagar/consultar anticipo simulado', 'anticipo', a.id);
            html += boton('Cancelar pendientes', 'cancelar', a.id);
        }
        return html;
    }
    async function ejecutar(clave, operacion) {
        if (pendientes.has(clave)) return;
        pendientes.add(clave);
        try { await operacion(); }
        catch (error) {
            if (manejarErrorSesion(error)) return;
            const dato = error.response?.data;
            await Swal.fire('No se pudo completar', typeof dato === 'string' ? dato
                : (Array.isArray(dato?.validationErrors) ? dato.validationErrors.map(v => String(v).replace(/^[^:]+:\s*/, '')).join(' ')
                    : error.message || 'Intentá nuevamente.'), 'error');
        } finally { pendientes.delete(clave); }
    }
    async function confirmar(texto) {
        return (await Swal.fire({title: 'Confirmar operación', text: texto, icon: 'question',
            showCancelButton: true, confirmButtonText: 'Confirmar', cancelButtonText: 'Volver'})).isConfirmed;
    }
    function cambiar(a, d, accion, datos) {
        return ejecutar('alquiler:' + id(a), async () => {
            await axios.post(`/api/alquileres/${id(a)}/detalles/${id(d)}/${accion}`, datos);
            await recargar();
            await Swal.fire('Operación registrada', 'Los datos se actualizaron desde el servidor.', 'success');
        });
    }
    async function retirar(a, d) {
        if (await confirmar('Confirmá el retiro real de esta bicicleta. El uso y su costo comienzan ahora.')) return cambiar(a, d, 'retirar');
    }
    async function cancelar(a, d) {
        if (!await confirmar('Se cancelarán únicamente las bicicletas pendientes de retiro. Las que ya están en uso continúan su viaje.')) return;
        return ejecutar('alquiler:' + id(a), async () => {
            await axios.post(`/api/alquileres/${id(a)}${d == null ? '' : '/detalles/' + id(d)}/cancelar`);
            await recargar();
            await Swal.fire('Reserva actualizada', 'Revisá el estado y la liquidación. Si el plazo ya venció, se registra como vencimiento.', 'success');
        });
    }
    async function cancelarAdmin(a, d) {
        const motivo = await Swal.fire({title: 'Cancelación administrativa', input: 'textarea',
            inputLabel: 'Motivo operativo o de seguridad (obligatorio)', showCancelButton: true,
            inputValidator: v => !v.trim() || v.trim().length > 1000 ? 'Ingresá un motivo de hasta 1000 caracteres.' : undefined});
        if (!motivo.isConfirmed) return;
        const destino = await Swal.fire({title: 'Estado de la bicicleta', input: 'select',
            inputOptions: {INACTIVA: 'Inactiva', MANTENIMIENTO: 'Mantenimiento'}, showCancelButton: true});
        if (!destino.isConfirmed) return;
        return ejecutar('alquiler:' + id(a), async () => {
            await axios.post(`/api/admin/alquileres/${id(a)}/detalles/${id(d)}/cancelar`,
                {motivo: motivo.value.trim(), estadoBicicleta: destino.value});
            await recargar();
            await Swal.fire('Cancelación registrada', 'La ubicación física se conserva. La operación quedó auditada.', 'success');
        });
    }
    async function devolver(a, d) {
        return ejecutar('alquiler:' + id(a), async () => {
            const {data} = await axios.get('/api/estaciones');
            const opciones = {};
            data.filter(e => e.esPublica !== false && e.espaciosLibres > 0).forEach(e => {
                opciones[id(e.id)] = `${e.nombre} · ${e.espaciosLibres} espacio(s)`;
            });
            if (!Object.keys(opciones).length) throw new Error('No hay estaciones públicas con espacio libre.');
            const estacion = await Swal.fire({title: 'Devolver bicicleta', input: 'select', inputOptions: opciones,
                inputPlaceholder: 'Elegí una estación pública', showCancelButton: true,
                inputValidator: v => !v ? 'Seleccioná una estación.' : undefined});
            if (!estacion.isConfirmed) return;
            const falla = await Swal.fire({title: '¿La bicicleta tiene una falla?', icon: 'question',
                showDenyButton: true, showCancelButton: true, confirmButtonText: 'Sí, reportar', denyButtonText: 'No'});
            if (!falla.isConfirmed && !falla.isDenied) return;
            let descripcion = null;
            if (falla.isConfirmed) {
                const detalle = await Swal.fire({title: 'Describí la falla', input: 'textarea', showCancelButton: true,
                    inputValidator: v => !v.trim() || v.trim().length > 2000 ? 'Ingresá una descripción de hasta 2000 caracteres.' : undefined});
                if (!detalle.isConfirmed) return;
                descripcion = detalle.value.trim();
            }
            await axios.post(`/api/alquileres/${id(a)}/detalles/${id(d)}/devolver`, {
                estacionDevolucionId: id(estacion.value), fallaReportada: falla.isConfirmed, descripcionFalla: descripcion
            });
            await recargar();
            await Swal.fire('Bicicleta devuelta', 'El uso de esta unidad terminó. Consultá la liquidación cuando cierre el conjunto.', 'success');
        });
    }
    async function pago(a, finalidad) {
        return ejecutar('pago:' + id(a), async () => {
            if (!cliente()) throw new Error('Solo el cliente titular puede pagar.');
            const claveLocal = `turismo:pago:${id(usuario.id)}:${id(a)}:${finalidad}`;
            let clave = localStorage.getItem(claveLocal);
            if (!clave) { clave = crypto.randomUUID(); localStorage.setItem(claveLocal, clave); }
            const ruta = `/api/pagos/${finalidad}/${id(a)}`;
            const config = {headers: {'Idempotency-Key': clave}};
            let resultado = (await axios.post(ruta, null, config)).data;
            if (!resultado.sinImporteACobrar && !['APROBADO', 'RECHAZADO', 'CANCELADO'].includes(resultado.estadoPago)) {
                if (!await confirmar(`Pago SIMULADO de ${dinero(resultado.montoArs)}. No utiliza dinero real. ¿Continuar o consultar el intento en proceso?`)) return;
                resultado = (await axios.post(ruta + '/resolver', null, config)).data;
            }
            if (['RECHAZADO', 'CANCELADO'].includes(resultado.estadoPago)) localStorage.removeItem(claveLocal);
            await recargar();
            await Swal.fire('Resultado del pago simulado', resultado.sinImporteACobrar ? 'No hay importe a cobrar.'
                : `${resultado.estadoPago}. Importe: ${dinero(resultado.montoArs)}. Un pago pendiente no habilita el retiro.`,
                resultado.estadoPago === 'APROBADO' || resultado.sinImporteACobrar ? 'success' : 'info');
        });
    }
    function anticipo(a) { return pago(a, 'anticipo'); }
    function adicional(a) { return pago(a, 'adicional'); }
    async function saldo() {
        return ejecutar('saldo', async () => {
            const {data} = await axios.get('/api/pagos/saldo-simulado');
            await Swal.fire('Cuenta simulada (sin dinero real)', 'Saldo disponible: ' + dinero(data.saldoDisponibleArs), 'info');
        });
    }
    async function finanzas(a) {
        return ejecutar('consulta:' + id(a), async () => {
            const {data} = await axios.get(`/api/pagos/reembolsos/${id(a)}`);
            const l = data.liquidacion;
            const lineas = data.reembolsos.map(r => `<li>#${id(r.id)} · ${t(r.estado)} · ${dinero(r.montoArs)}</li>`).join('');
            const res = await Swal.fire({title: `Liquidación #${id(a)}`, html: `
                <p>${t(l.situacionFinanciera)}</p><p>Costo final: ${dinero(l.costoFinalArs)}</p>
                <p>Pagos aprobados: ${dinero(l.pagosAprobadosArs)}<br>Reembolsos confirmados: ${dinero(l.reembolsosConfirmadosArs)}</p>
                <p>Deuda: ${dinero(l.saldoPendienteArs)}<br>A reintegrar: ${dinero(l.reembolsoPendienteArs)}</p><ul>${lineas}</ul>`,
                showCancelButton: true, confirmButtonText: 'Cerrar',
                showDenyButton: cliente() && (Number(l.saldoPendienteArs) > 0 || Number(l.reembolsoPendienteArs) > 0),
                denyButtonText: Number(l.saldoPendienteArs) > 0 ? 'Pagar saldo simulado' : 'Procesar/consultar reintegro'});
            if (res.isDenied) {
                // Esta consulta ya terminó; los endpoints vuelven a controlar titularidad y rol vigente.
                if (Number(l.saldoPendienteArs) > 0) await adicional(a);
                else await reintegrar(a);
            }
        });
    }
    async function reintegrar(a) {
        return ejecutar('reembolso:' + id(a), async () => {
            const {data} = await axios.post(`/api/pagos/reembolsos/${id(a)}/resolver`);
            await recargar();
            await Swal.fire('Reembolso simulado', data.reembolsos.map(r => `#${r.id}: ${r.estado}`).join('; ') || 'No hay reintegros pendientes.', 'info');
        });
    }
    function abiertos() {
        return typeof document.querySelectorAll === 'function'
            ? Array.from(document.querySelectorAll('details[data-alquiler-detalles][open]')).map(e => e.dataset.alquilerDetalles) : [];
    }
    function restaurarAbiertos(ids) {
        if (typeof document.querySelectorAll !== 'function') return;
        document.querySelectorAll('details[data-alquiler-detalles]').forEach(e => { e.open = ids.includes(e.dataset.alquilerDetalles); });
    }
    return {configurar, acciones, accionesDetalle, retirar, cancelar, cancelarAdmin, devolver, anticipo, adicional,
        finanzas, reintegrar, saldo, abiertos, restaurarAbiertos};
})();
