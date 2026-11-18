-- MIGRACION LOCAL 06. Ejecutar UNA VEZ con la app detenida, respaldo fresco y FK activadas.
-- Unicamente la foto revisada: alquileres 5 a 12, cerrados; despues usar el codigo nuevo, perfil refactor.
-- Primero ejecutar el informe 06-clasificar... y revisar sus cantidades. No combinar con la limpieza anterior.
-- Cuerpo de datos transaccional; la vista-marcador se crea DESPUES de confirmar, fuera de la transaccion.
-- Si aparece cualquier error: NO arrancar ni reintentar sin verificar el estado. No usar --force.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS turismo_sustentable.migrar_hora_alquiler_06;
DELIMITER //
CREATE PROCEDURE turismo_sustentable.migrar_hora_alquiler_06()
SQL SECURITY INVOKER
MODIFIES SQL DATA
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS turismo_sustentable.tmp_hora_alquiler_06;
        RESIGNAL;
    END;

    IF @@SESSION.foreign_key_checks <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: las comprobaciones de FK deben estar activadas.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='turismo_sustentable'
            AND TABLE_NAME='v_convencion_hora_alquiler') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: ya existe el marcador horario. No reaplicar.';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='turismo_sustentable'
            AND ENGINE='InnoDB' AND TABLE_NAME IN ('alquiler','alquiler_detalle','historial_estado_alquiler',
                'historial_estado_detalle','pago','historial_estado_pago','reembolso',
                'historial_estado_reembolso','historial_estado_bicicleta')) <> 9
       OR EXISTS (SELECT 1 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='turismo_sustentable') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: motores o triggers distintos de lo previsto.';
    END IF;

    START TRANSACTION;
    IF (SELECT COUNT(*) FROM turismo_sustentable.alquiler) <> 8
       OR (SELECT COUNT(*) FROM turismo_sustentable.alquiler WHERE id BETWEEN 5 AND 12) <> 8
       OR (SELECT COUNT(*) FROM turismo_sustentable.alquiler_detalle) <> 16
       OR (SELECT COUNT(*) FROM turismo_sustentable.historial_estado_alquiler) <> 20
       OR (SELECT COUNT(*) FROM turismo_sustentable.historial_estado_detalle) <> 47
       OR (SELECT COUNT(*) FROM turismo_sustentable.pago) <> 4
       OR (SELECT COUNT(*) FROM turismo_sustentable.reembolso) <> 4
       OR (SELECT COUNT(*) FROM turismo_sustentable.historial_estado_pago) <> 12
       OR (SELECT COUNT(*) FROM turismo_sustentable.historial_estado_reembolso) <> 12
       OR EXISTS (SELECT 1 FROM turismo_sustentable.incidencia) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: cambiaron los datos de la foto revisada; reclasificar.';
    END IF;
    IF EXISTS (SELECT 1 FROM turismo_sustentable.alquiler a
        JOIN turismo_sustentable.estado_alquiler e ON e.id=a.estado_alquiler_id
        WHERE e.codigo NOT IN ('FINALIZADO','CANCELADO') OR a.fecha_hora_cierre IS NULL
           OR a.fecha_hora_liquidacion IS NULL OR a.costo_final_ars IS NULL
           OR a.fecha_hora_vencimiento_reserva <> DATE_ADD(a.fecha_hora_creacion, INTERVAL 15 MINUTE))
       OR EXISTS (SELECT 1 FROM turismo_sustentable.pago p
        JOIN turismo_sustentable.estado_pago e ON e.id=p.estado_pago_id
        WHERE p.plataforma <> 'SIMULADA' OR p.entorno <> 'PRUEBA' OR e.codigo <> 'APROBADO'
           OR p.moneda <> 'ARS' OR p.fecha_confirmacion IS NULL)
       OR EXISTS (SELECT 1 FROM turismo_sustentable.reembolso WHERE estado <> 'CONFIRMADO'
           OR moneda <> 'ARS' OR fecha_confirmacion IS NULL)
       OR EXISTS (SELECT 1 FROM turismo_sustentable.historial_estado_pago WHERE fecha_hora_evento_externo IS NOT NULL)
       OR EXISTS (SELECT 1 FROM turismo_sustentable.historial_estado_reembolso WHERE fecha_hora_evento_externo IS NOT NULL) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: operaciones o fechas externas no clasificadas.';
    END IF;

    -- Cada reserva de inventario debe estar YA en UTC-3 frente a su cabecera UTC.
    -- Corte deliberadamente estricto: protege tambien contra doble conversion si faltara el marcador.
    IF EXISTS (SELECT 1 FROM turismo_sustentable.alquiler_detalle d
        JOIN turismo_sustentable.alquiler a ON a.id=d.alquiler_id
        WHERE (SELECT COUNT(*) FROM turismo_sustentable.historial_estado_bicicleta h
            JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_nuevo_id
            WHERE h.bicicleta_id=d.bicicleta_id AND n.codigo='RESERVADA'
              AND h.actor_usuario_id=a.usuario_id AND h.origen='USUARIO'
              AND h.observaciones=CONCAT('Reserva del alquiler ',a.id,'.')
              AND ABS(TIMESTAMPDIFF(MICROSECOND,DATE_SUB(a.fecha_hora_creacion,INTERVAL 3 HOUR),
                  h.fecha_hora_cambio)) < 1000000) <> 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: falta ancla de hora, o los alquileres ya fueron convertidos.';
    END IF;

    CREATE TEMPORARY TABLE turismo_sustentable.tmp_hora_alquiler_06 (
        detalle_id INT NOT NULL, tipo VARCHAR(12) NOT NULL, evento_id INT NOT NULL,
        PRIMARY KEY(detalle_id,tipo), UNIQUE KEY (evento_id)
    ) ENGINE=InnoDB;
    -- Marcar unicamente eventos con el mismo instante UTC que su operacion de alquiler.
    INSERT INTO turismo_sustentable.tmp_hora_alquiler_06(detalle_id,tipo,evento_id)
    SELECT d.id,'RETIRO',h.id FROM turismo_sustentable.alquiler_detalle d
      JOIN turismo_sustentable.historial_estado_bicicleta h ON h.bicicleta_id=d.bicicleta_id
       AND h.fecha_hora_cambio=d.fecha_hora_retiro
       AND h.observaciones=CONCAT('Retiro del alquiler ',d.alquiler_id,', detalle ',d.id,'.')
      JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_nuevo_id AND n.codigo='EN_USO'
    UNION ALL
    SELECT d.id,'DEVOLUCION',h.id FROM turismo_sustentable.alquiler_detalle d
      JOIN turismo_sustentable.historial_estado_bicicleta h ON h.bicicleta_id=d.bicicleta_id
       AND h.fecha_hora_cambio=d.fecha_hora_devolucion_real
       AND h.observaciones LIKE CONCAT('%alquiler ',d.alquiler_id,', detalle ',d.id,'.%')
      JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_anterior_id AND n.codigo='EN_USO'
    UNION ALL
    SELECT d.id,'CANCELACION',h.id FROM turismo_sustentable.alquiler_detalle d
      JOIN turismo_sustentable.estado_alquiler_detalle ed ON ed.id=d.estado_alquiler_detalle_id AND ed.codigo='CANCELADO'
      JOIN turismo_sustentable.historial_estado_bicicleta h ON h.bicicleta_id=d.bicicleta_id
       AND h.fecha_hora_cambio=d.fecha_hora_cierre_detalle AND h.observaciones=d.motivo_cierre
      JOIN turismo_sustentable.estado_bicicleta n ON n.id=h.estado_anterior_id AND n.codigo='RESERVADA';
    IF (SELECT COUNT(*) FROM turismo_sustentable.tmp_hora_alquiler_06 WHERE tipo='RETIRO') <> 7
       OR (SELECT COUNT(*) FROM turismo_sustentable.tmp_hora_alquiler_06 WHERE tipo='DEVOLUCION') <> 7
       OR (SELECT COUNT(*) FROM turismo_sustentable.tmp_hora_alquiler_06 WHERE tipo='CANCELACION') <> 9 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: no coincide la lista de 23 eventos UTC de inventario.';
    END IF;

    -- Los ocho conjuntos completos quedaron delimitados y las tablas relacionadas tienen FK.
    -- DATE_SUB conserva microsegundos y NULL. No se tocan montos, versiones, ubicaciones ni estados.
    UPDATE turismo_sustentable.alquiler SET
        fecha_hora_aplicacion_cotizacion = DATE_SUB(fecha_hora_aplicacion_cotizacion, INTERVAL 3 HOUR),
        fecha_hora_actualizacion_fuente = DATE_SUB(fecha_hora_actualizacion_fuente, INTERVAL 3 HOUR),
        fecha_hora_creacion = DATE_SUB(fecha_hora_creacion, INTERVAL 3 HOUR),
        fecha_hora_vencimiento_reserva = DATE_SUB(fecha_hora_vencimiento_reserva, INTERVAL 3 HOUR),
        fecha_hora_cierre = DATE_SUB(fecha_hora_cierre, INTERVAL 3 HOUR),
        fecha_hora_cancelacion = DATE_SUB(fecha_hora_cancelacion, INTERVAL 3 HOUR),
        fecha_hora_liquidacion = DATE_SUB(fecha_hora_liquidacion, INTERVAL 3 HOUR);
    UPDATE turismo_sustentable.alquiler_detalle SET
        fecha_hora_retiro = DATE_SUB(fecha_hora_retiro, INTERVAL 3 HOUR),
        fecha_hora_devolucion_estimada = DATE_SUB(fecha_hora_devolucion_estimada, INTERVAL 3 HOUR),
        fecha_hora_devolucion_real = DATE_SUB(fecha_hora_devolucion_real, INTERVAL 3 HOUR),
        fecha_hora_fin_uso = DATE_SUB(fecha_hora_fin_uso, INTERVAL 3 HOUR),
        fecha_hora_cierre_detalle = DATE_SUB(fecha_hora_cierre_detalle, INTERVAL 3 HOUR),
        fecha_reporte_falla = DATE_SUB(fecha_reporte_falla, INTERVAL 3 HOUR);
    UPDATE turismo_sustentable.historial_estado_alquiler SET
        fecha_hora_cambio = DATE_SUB(fecha_hora_cambio, INTERVAL 3 HOUR);
    UPDATE turismo_sustentable.historial_estado_detalle SET
        fecha_hora_cambio = DATE_SUB(fecha_hora_cambio, INTERVAL 3 HOUR);
    UPDATE turismo_sustentable.pago SET
        fecha_creacion = DATE_SUB(fecha_creacion, INTERVAL 3 HOUR),
        fecha_confirmacion = DATE_SUB(fecha_confirmacion, INTERVAL 3 HOUR);
    UPDATE turismo_sustentable.historial_estado_pago SET
        fecha_hora_cambio = DATE_SUB(fecha_hora_cambio, INTERVAL 3 HOUR),
        fecha_hora_evento_externo = DATE_SUB(fecha_hora_evento_externo, INTERVAL 3 HOUR);
    UPDATE turismo_sustentable.reembolso SET
        fecha_creacion = DATE_SUB(fecha_creacion, INTERVAL 3 HOUR),
        fecha_confirmacion = DATE_SUB(fecha_confirmacion, INTERVAL 3 HOUR);
    UPDATE turismo_sustentable.historial_estado_reembolso SET
        fecha_hora_cambio = DATE_SUB(fecha_hora_cambio, INTERVAL 3 HOUR),
        fecha_hora_evento_externo = DATE_SUB(fecha_hora_evento_externo, INTERVAL 3 HOUR);
    UPDATE turismo_sustentable.historial_estado_bicicleta h
    JOIN turismo_sustentable.tmp_hora_alquiler_06 t ON t.evento_id=h.id
    SET h.fecha_hora_cambio=DATE_SUB(h.fecha_hora_cambio, INTERVAL 3 HOUR);

    IF EXISTS (SELECT 1 FROM turismo_sustentable.tmp_hora_alquiler_06 t
        JOIN turismo_sustentable.historial_estado_bicicleta h ON h.id=t.evento_id
        JOIN turismo_sustentable.alquiler_detalle d ON d.id=t.detalle_id
        WHERE NOT (h.fecha_hora_cambio <=> CASE t.tipo WHEN 'RETIRO' THEN d.fecha_hora_retiro
             WHEN 'DEVOLUCION' THEN d.fecha_hora_devolucion_real ELSE d.fecha_hora_cierre_detalle END))
       OR EXISTS (SELECT 1 FROM turismo_sustentable.alquiler a
           WHERE a.fecha_hora_vencimiento_reserva <> DATE_ADD(a.fecha_hora_creacion, INTERVAL 15 MINUTE)
             OR NOT EXISTS (SELECT 1 FROM turismo_sustentable.historial_estado_alquiler h WHERE h.alquiler_id=a.id
                 AND h.estado_anterior_id IS NULL AND h.fecha_hora_cambio=a.fecha_hora_creacion)
             OR NOT EXISTS (SELECT 1 FROM turismo_sustentable.historial_estado_alquiler h WHERE h.alquiler_id=a.id
                 AND h.estado_nuevo_id=a.estado_alquiler_id AND h.fecha_hora_cambio=a.fecha_hora_cierre))
       OR EXISTS (SELECT 1 FROM turismo_sustentable.alquiler_detalle d WHERE d.fecha_hora_retiro IS NOT NULL
           AND d.fecha_hora_devolucion_estimada <> DATE_ADD(d.fecha_hora_retiro, INTERVAL d.duracion_estimada_segundos SECOND)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: fallaron las comprobaciones posteriores; se revierte el UPDATE.';
    END IF;
    DROP TEMPORARY TABLE turismo_sustentable.tmp_hora_alquiler_06;
    COMMIT;

    -- Solo se llega aqui si los datos fueron convertidos. DDL fuera del COMMIT de datos.
    CREATE SQL SECURITY INVOKER VIEW turismo_sustentable.v_convencion_hora_alquiler AS
        SELECT 'ALQUILER_UTC3_V1' AS revision, 'America/Argentina/Ushuaia' AS zona;
END//
DELIMITER ;

CALL turismo_sustentable.migrar_hora_alquiler_06();
DROP PROCEDURE turismo_sustentable.migrar_hora_alquiler_06;
SELECT revision,zona FROM turismo_sustentable.v_convencion_hora_alquiler;
SELECT id,fecha_hora_creacion,fecha_hora_vencimiento_reserva,fecha_hora_cierre,costo_final_ars
FROM turismo_sustentable.alquiler WHERE id IN (11,12) ORDER BY id;
