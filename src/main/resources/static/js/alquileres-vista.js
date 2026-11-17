const VistaAlquileres = (() => {
    const reemplazos = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    };

    function texto(valor) {
        return String(valor ?? '—').replace(
            /[&<>"']/g,
            caracter => reemplazos[caracter]
        );
    }

    function id(valor) {
        const numero = Number(valor);

        if (!Number.isSafeInteger(numero) || numero <= 0) {
            throw new Error('ID de alquiler inválido.');
        }

        return numero;
    }

    function validar(alquiler) {
        if (alquiler?.modelo !== 'DETALLES'
                || !Array.isArray(alquiler.detalles)
                || alquiler.detalles.length === 0) {
            throw new Error('Formato de alquiler incompleto o desconocido.');
        }
    }

    function fecha(valor) {
        if (!valor) return '—';

        // El backend entrega estas fechas en UTC.
        const cadena = String(valor);
        const iso = /(Z|[+-]\d{2}:\d{2})$/i.test(cadena)
            ? cadena
            : cadena + 'Z';

        const resultado = new Date(iso);

        return Number.isNaN(resultado.getTime())
            ? 'Fecha no disponible'
            : resultado.toLocaleString('es-AR');
    }

    function importe(
        usd,
        ars,
        moneda = 'USD',
        ausente = 'Pendiente de liquidación'
    ) {
        const valor = moneda === 'ARS' ? ars : usd;

        if (valor == null || valor === '') return ausente;

        const numero = Number(valor);

        if (!Number.isFinite(numero)) {
            throw new Error('Importe inválido recibido.');
        }

        const simbolo = moneda === 'ARS' ? 'AR$ ' : 'US$ ';

        return simbolo + numero.toLocaleString('es-AR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function activo(alquiler) {
        validar(alquiler);

        return ['RESERVADO', 'EN_PROGRESO'].includes(alquiler.estado);
    }

    function importes(alquiler, moneda = 'USD') {
        validar(alquiler);

        return `
            <div>Estimado: ${importe(
                alquiler.costoEstimadoUsd,
                alquiler.costoEstimadoArs,
                moneda,
                'No informado'
            )}</div>
            <div>Final: ${importe(
                alquiler.costoFinalUsd,
                alquiler.costoFinalArs,
                moneda
            )}</div>
        `;
    }

    function bicicletas(alquiler, moneda = 'USD', accionesDetalle = () => '') {
        validar(alquiler);

        const unidades = alquiler.detalles.map(detalle => `
            <li class="mb-3">
                <strong>${texto(detalle.codigoBicicleta)}</strong>
                · ${texto(detalle.estado)}
                <div>Tipo: ${texto(detalle.tipoBicicletaContratado)}</div>
                <div>Tarifa/h: ${importe(
                    detalle.precioPorHoraAplicadoUsd,
                    null,
                    'USD',
                    'No informada'
                )}</div>
                <div>Retiro: ${fecha(detalle.fechaHoraRetiro)}</div>
                <div>Devolución estimada: ${fecha(detalle.fechaHoraDevolucionEstimada)}</div>
                <div>Devolución real: ${fecha(detalle.fechaHoraDevolucionReal)}</div>
                <div>Fin de uso: ${fecha(detalle.fechaHoraFinUso)}</div>
                <div>Estimado: ${importe(
                    detalle.subtotalEstimadoUsd,
                    detalle.subtotalEstimadoArs,
                    moneda,
                    'No informado'
                )}</div>
                <div>Real: ${importe(
                    detalle.subtotalRealUsd,
                    detalle.subtotalRealArs,
                    moneda
                )}</div>
                ${detalle.fallaReportada
                    ? '<div>Falla reportada</div>'
                    : ''}
                ${accionesDetalle(detalle)}
            </li>
        `).join('');

        return `
            <div class="small">
                Estación de retiro: ${texto(alquiler.estacionRetiro)}
            </div>
            <div class="small">
                Cambio aplicado: ${texto(
                    alquiler.cotizacionArsPorUsd
                )} ARS/USD
            </div>
            <details class="mt-2" data-alquiler-detalles="${id(alquiler.id)}">
                <summary>${alquiler.detalles.length} bicicleta(s)</summary>
                <ul class="mt-2 text-start">${unidades}</ul>
            </details>
        `;
    }

    return {
        texto, id, validar, fecha, importe,
        activo, importes, bicicletas
    };
})();
