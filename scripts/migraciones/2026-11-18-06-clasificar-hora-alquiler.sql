-- SOLO LECTURA. Ejecutar ANTES de migrar la foto QA, con la app detenida.
-- Si los conteos ya no coinciden, no ejecutar la migracion: reclasificar primero.
SELECT 'alquiler' AS tabla,COUNT(*) AS filas,8 AS esperado FROM turismo_sustentable.alquiler
UNION ALL SELECT 'alquiler_detalle',COUNT(*),16 FROM turismo_sustentable.alquiler_detalle
UNION ALL SELECT 'historial_estado_alquiler',COUNT(*),20 FROM turismo_sustentable.historial_estado_alquiler
UNION ALL SELECT 'historial_estado_detalle',COUNT(*),47 FROM turismo_sustentable.historial_estado_detalle
UNION ALL SELECT 'pago',COUNT(*),4 FROM turismo_sustentable.pago
UNION ALL SELECT 'historial_estado_pago',COUNT(*),12 FROM turismo_sustentable.historial_estado_pago
UNION ALL SELECT 'reembolso',COUNT(*),4 FROM turismo_sustentable.reembolso
UNION ALL SELECT 'historial_estado_reembolso',COUNT(*),12 FROM turismo_sustentable.historial_estado_reembolso
UNION ALL SELECT 'incidencia (fuera del bloque)',COUNT(*),0 FROM turismo_sustentable.incidencia;

WITH clasificacion AS (
    SELECT h.id AS evento_id,d.alquiler_id,d.id AS detalle_id,'YA_UTC3' AS clase
    FROM turismo_sustentable.alquiler_detalle d JOIN turismo_sustentable.alquiler a ON a.id=d.alquiler_id
    JOIN turismo_sustentable.historial_estado_bicicleta h ON h.bicicleta_id=d.bicicleta_id
      AND h.observaciones=CONCAT('Reserva del alquiler ',a.id,'.')
      AND h.actor_usuario_id=a.usuario_id AND h.origen='USUARIO'
      AND ABS(TIMESTAMPDIFF(MICROSECOND,DATE_SUB(a.fecha_hora_creacion,INTERVAL 3 HOUR),h.fecha_hora_cambio)) < 1000000
    JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_nuevo_id AND n.codigo='RESERVADA'
    UNION ALL
    SELECT h.id,d.alquiler_id,d.id,'CONVERTIR_RETIRO' FROM turismo_sustentable.alquiler_detalle d
    JOIN turismo_sustentable.historial_estado_bicicleta h ON h.bicicleta_id=d.bicicleta_id
      AND h.fecha_hora_cambio=d.fecha_hora_retiro
      AND h.observaciones=CONCAT('Retiro del alquiler ',d.alquiler_id,', detalle ',d.id,'.')
    JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_nuevo_id AND n.codigo='EN_USO'
    UNION ALL
    SELECT h.id,d.alquiler_id,d.id,'CONVERTIR_DEVOLUCION' FROM turismo_sustentable.alquiler_detalle d
    JOIN turismo_sustentable.historial_estado_bicicleta h ON h.bicicleta_id=d.bicicleta_id
      AND h.fecha_hora_cambio=d.fecha_hora_devolucion_real
      AND h.observaciones LIKE CONCAT('%alquiler ',d.alquiler_id,', detalle ',d.id,'.%')
    JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_anterior_id AND n.codigo='EN_USO'
    UNION ALL
    SELECT h.id,d.alquiler_id,d.id,'CONVERTIR_CANCELACION' FROM turismo_sustentable.alquiler_detalle d
    JOIN turismo_sustentable.estado_alquiler_detalle ed ON ed.id=d.estado_alquiler_detalle_id AND ed.codigo='CANCELADO'
    JOIN turismo_sustentable.historial_estado_bicicleta h ON h.bicicleta_id=d.bicicleta_id
      AND h.fecha_hora_cambio=d.fecha_hora_cierre_detalle AND h.observaciones=d.motivo_cierre
    JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_anterior_id AND n.codigo='RESERVADA'
)
SELECT clase,COUNT(*) AS filas,COUNT(DISTINCT evento_id) AS eventos_distintos,
       GROUP_CONCAT(evento_id ORDER BY evento_id) AS ids
FROM clasificacion GROUP BY clase ORDER BY clase;
-- Esperado: YA_UTC3=16; RETIRO=7; DEVOLUCION=7; CANCELACION=9.

SELECT h.id,h.observaciones, h.fecha_hora_cambio AS fecha_sin_transformar
FROM turismo_sustentable.historial_estado_bicicleta h
WHERE h.observaciones IN ('Reserva del alquiler 1.','Reserva del alquiler 2.',
                         'Reserva del alquiler 3.','Reserva del alquiler 4.') ORDER BY h.id;
-- No se cambian esas cuatro filas ni otros historiales fuera de la lista de conversion.
