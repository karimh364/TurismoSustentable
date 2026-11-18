-- SOLO LECTURA. Ejecutar DESPUES de la migracion; todas las fechas seleccionadas son locales.
SELECT revision,zona FROM turismo_sustentable.v_convencion_hora_alquiler;

SELECT a.id,a.fecha_hora_creacion,a.fecha_hora_vencimiento_reserva,a.fecha_hora_cierre,a.costo_final_ars,
       TIMESTAMPDIFF(MINUTE,a.fecha_hora_creacion,a.fecha_hora_vencimiento_reserva) AS plazo_minutos
FROM turismo_sustentable.alquiler a ORDER BY a.id;

-- Debe dar cero en la foto migrada y luego de cerrar/saldar los casos E2E.
SELECT COUNT(*) AS alquileres_liquidados_con_saldo_pendiente
FROM turismo_sustentable.alquiler a WHERE a.fecha_hora_liquidacion IS NOT NULL
AND a.costo_final_ars <> COALESCE((SELECT SUM(p.monto_ars) FROM turismo_sustentable.pago p
    JOIN turismo_sustentable.estado_pago e ON e.id=p.estado_pago_id
    WHERE p.alquiler_id=a.id AND e.codigo='APROBADO' AND p.fecha_confirmacion IS NOT NULL),0)
    - COALESCE((SELECT SUM(r.monto_ars) FROM turismo_sustentable.reembolso r
    JOIN turismo_sustentable.pago p ON p.id=r.pago_id
    WHERE p.alquiler_id=a.id AND r.estado='CONFIRMADO' AND r.fecha_confirmacion IS NOT NULL),0);

-- Debe dar cero. La reserva antigua de inventario conserva sus milisegundos originales;
-- las reservas nuevas comparten el mismo instante que la cabecera y el detalle.
SELECT COUNT(*) AS reservas_sin_inventario_en_hora_local
FROM turismo_sustentable.alquiler_detalle d JOIN turismo_sustentable.alquiler a ON a.id=d.alquiler_id
WHERE NOT EXISTS (SELECT 1 FROM turismo_sustentable.historial_estado_bicicleta h
    JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_nuevo_id
    WHERE h.bicicleta_id=d.bicicleta_id AND n.codigo='RESERVADA'
      AND h.observaciones=CONCAT('Reserva del alquiler ',a.id,'.')
      AND ABS(TIMESTAMPDIFF(MICROSECOND,a.fecha_hora_creacion,h.fecha_hora_cambio)) < 1000000);

SELECT d.alquiler_id,d.id AS detalle,b.codigo_bicicleta,a.fecha_hora_creacion AS cabecera_local,
       hd.fecha_hora_cambio AS detalle_local,hb.fecha_hora_cambio AS inventario_local
FROM turismo_sustentable.alquiler_detalle d JOIN turismo_sustentable.alquiler a ON a.id=d.alquiler_id
JOIN turismo_sustentable.bicicleta b ON b.id=d.bicicleta_id
JOIN turismo_sustentable.historial_estado_detalle hd ON hd.alquiler_detalle_id=d.id AND hd.estado_anterior_id IS NULL
JOIN turismo_sustentable.historial_estado_bicicleta hb ON hb.bicicleta_id=b.id
  AND hb.observaciones=CONCAT('Reserva del alquiler ',a.id,'.')
ORDER BY d.alquiler_id,d.id;
