-- LIMPIEZA MANUAL, DE UNA SOLA VEZ. SOLO DATOS FICTICIOS DEL PARCIAL.
-- Requiere: app detenida, respaldo SQL completo ya exportado y MariaDB activa.
-- Ejecutar el archivo COMPLETO desde Importar en phpMyAdmin, con revision de FK activada.
-- Elimina alquileres 1,2,3,4 y sus detalles/incidencias/pagos/reembolsos e historiales propios.
-- Conserva usuarios, personas, roles, catalogos, estaciones, bicicletas, mantenimiento,
-- historial de bicicletas y auditorias. No reinicia AUTO_INCREMENT ni usa TRUNCATE.
-- Los saldos simulados se recalculan al desaparecer los pagos/reembolsos de prueba.
-- Los registros conservados pueden mencionar pruebas antiguas en sus observaciones:
-- NO se reescribe su pasado y NO se reutilizan los IDs eliminados.
-- No es una operacion de negocio ni debe reutilizarse en produccion.
-- Un error SQL dentro del procedimiento, antes del COMMIT, revierte su DML.
-- CREATE/DROP PROCEDURE quedan fuera de esa transaccion y solo gestionan este auxiliar.
-- No ejecutar en una sesion con otros cambios pendientes. No usar --force ni ignorar errores.
USE turismo_sustentable;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS turismo_sustentable.qa_limpiar_alquileres_1_4_h1_20261118;
DELIMITER //
CREATE PROCEDURE turismo_sustentable.qa_limpiar_alquileres_1_4_h1_20261118()
SQL SECURITY INVOKER
MODIFIES SQL DATA
BEGIN
    DECLARE v_id INT;
    DECLARE v_conservados_antes VARCHAR(1024);
    DECLARE v_conservados_despues VARCHAR(1024);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF DATABASE() <> 'turismo_sustentable' OR @@SESSION.foreign_key_checks <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: usar turismo_sustentable con revision de FK activada.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA='turismo_sustentable' AND TABLE_NAME='historial_estado_alquiler') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: ya existe historial de cabecera. Revisar limpieza antes de continuar.';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA='turismo_sustentable' AND ENGINE='InnoDB'
          AND TABLE_NAME IN ('alquiler','alquiler_detalle','historial_estado_detalle',
              'incidencia','historial_incidencia','pago','historial_estado_pago',
              'reembolso','historial_estado_reembolso')) <> 9 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: las nueve tablas afectadas deben existir y ser InnoDB.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TRIGGERS
        WHERE TRIGGER_SCHEMA='turismo_sustentable') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: hay triggers no contemplados. Revisar sus efectos antes de limpiar.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.KEY_COLUMN_USAGE
        WHERE REFERENCED_TABLE_SCHEMA='turismo_sustentable'
          AND TABLE_SCHEMA <> 'turismo_sustentable'
          AND REFERENCED_TABLE_NAME IN ('alquiler','alquiler_detalle','incidencia','pago','reembolso')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: otro esquema referencia estos registros. Revisar dependencias.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA='turismo_sustentable'
          AND REFERENCED_TABLE_NAME IN ('alquiler','alquiler_detalle','incidencia','pago','reembolso')
          AND DELETE_RULE NOT IN ('RESTRICT','NO ACTION')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: hay reglas de borrado distintas de las revisadas. No continuar.';
    END IF;

    SET TRANSACTION ISOLATION LEVEL READ COMMITTED;
    START TRANSACTION;

    IF (SELECT COUNT(*) FROM alquiler) <> 4
       OR (SELECT COUNT(*) FROM alquiler WHERE id IN (1,2,3,4)) <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: se esperaban exactamente los alquileres 1 a 4. No se borro nada.';
    END IF;
    -- Bloqueos de cabecera en orden estable, sin SELECT de resultados intermedios.
    SELECT id INTO v_id FROM alquiler WHERE id=1 FOR UPDATE;
    SELECT id INTO v_id FROM alquiler WHERE id=2 FOR UPDATE;
    SELECT id INTO v_id FROM alquiler WHERE id=3 FOR UPDATE;
    SELECT id INTO v_id FROM alquiler WHERE id=4 FOR UPDATE;

    IF EXISTS (SELECT 1 FROM alquiler a LEFT JOIN estado_alquiler e ON e.id=a.estado_alquiler_id
        WHERE a.id IN (1,2,3,4) AND (e.id IS NULL
            OR e.codigo NOT IN ('FINALIZADO','CANCELADO','VENCIDO')
            OR a.fecha_hora_cierre IS NULL OR a.fecha_hora_liquidacion IS NULL
            OR a.costo_final_ars IS NULL)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: todos los alquileres deben estar cerrados y liquidados.';
    END IF;
    IF (SELECT COUNT(*) FROM alquiler_detalle WHERE alquiler_id IN (1,2,3,4)) <> 4
       OR (SELECT COUNT(*) FROM pago WHERE alquiler_id IN (1,2,3,4)) <> 4
       OR (SELECT COUNT(*) FROM reembolso r JOIN pago p ON p.id=r.pago_id
           WHERE p.alquiler_id IN (1,2,3,4)) <> 2
       OR (SELECT COUNT(*) FROM incidencia i JOIN alquiler_detalle d ON d.id=i.alquiler_detalle_id
           WHERE d.alquiler_id IN (1,2,3,4)) <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: cambiaron las dependencias revisadas (4 detalles/4 pagos/2 reembolsos/1 incidencia).';
    END IF;
    IF EXISTS (SELECT 1 FROM alquiler_detalle d
        LEFT JOIN estado_alquiler_detalle e ON e.id=d.estado_alquiler_detalle_id
        WHERE d.alquiler_id IN (1,2,3,4) AND
            (e.id IS NULL OR e.codigo NOT IN ('DEVUELTO','CANCELADO','VENCIDO','CERRADO_POR_INCIDENCIA'))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: hay detalles activos o con estado desconocido.';
    END IF;
    IF EXISTS (SELECT 1 FROM pago p LEFT JOIN estado_pago e ON e.id=p.estado_pago_id
        WHERE p.alquiler_id IN (1,2,3,4) AND (p.plataforma <> 'SIMULADA'
            OR p.entorno <> 'PRUEBA' OR e.id IS NULL OR e.codigo <> 'APROBADO'
            OR p.moneda <> 'ARS' OR p.fecha_confirmacion IS NULL)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: los cuatro pagos deben ser simulados, de prueba y aprobados.';
    END IF;
    IF EXISTS (SELECT 1 FROM reembolso r JOIN pago p ON p.id=r.pago_id
        WHERE p.alquiler_id IN (1,2,3,4) AND
            (r.estado <> 'CONFIRMADO' OR r.moneda <> 'ARS' OR r.fecha_confirmacion IS NULL)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: hay reembolsos no confirmados. Resolverlos antes de limpiar.';
    END IF;
    IF EXISTS (SELECT 1 FROM incidencia i JOIN alquiler_detalle d ON d.id=i.alquiler_detalle_id
        WHERE d.alquiler_id IN (1,2,3,4) AND i.estado_revision='PENDIENTE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: hay una incidencia pendiente de revision.';
    END IF;
    IF EXISTS (SELECT 1 FROM alquiler a WHERE a.id IN (1,2,3,4)
        AND a.costo_final_ars <> (
            COALESCE((SELECT SUM(p.monto_ars) FROM pago p JOIN estado_pago e ON e.id=p.estado_pago_id
                WHERE p.alquiler_id=a.id AND e.codigo='APROBADO' AND p.moneda='ARS'
                  AND p.fecha_confirmacion IS NOT NULL),0)
            - COALESCE((SELECT SUM(r.monto_ars) FROM reembolso r JOIN pago p ON p.id=r.pago_id
                WHERE p.alquiler_id=a.id AND r.estado='CONFIRMADO' AND r.moneda='ARS'
                  AND r.fecha_confirmacion IS NOT NULL),0))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: existe deuda o reintegro pendiente. No se elimino el conjunto.';
    END IF;

    -- Control de conteos de las veinte tablas que NO se deben modificar.
    SELECT CONCAT_WS(':',
        (SELECT COUNT(*) FROM usuario), (SELECT COUNT(*) FROM persona),
        (SELECT COUNT(*) FROM usuario_rol), (SELECT COUNT(*) FROM rol),
        (SELECT COUNT(*) FROM genero), (SELECT COUNT(*) FROM pais),
        (SELECT COUNT(*) FROM tipo_documento), (SELECT COUNT(*) FROM estado_alquiler),
        (SELECT COUNT(*) FROM estado_alquiler_detalle), (SELECT COUNT(*) FROM estado_bicicleta),
        (SELECT COUNT(*) FROM estado_orden_mantenimiento), (SELECT COUNT(*) FROM estado_pago),
        (SELECT COUNT(*) FROM estacion), (SELECT COUNT(*) FROM bicicleta),
        (SELECT COUNT(*) FROM tipo_bicicleta), (SELECT COUNT(*) FROM tipo_mantenimiento),
        (SELECT COUNT(*) FROM orden_mantenimiento), (SELECT COUNT(*) FROM historial_orden_mantenimiento),
        (SELECT COUNT(*) FROM historial_estado_bicicleta), (SELECT COUNT(*) FROM auditoria_sistema)
    ) INTO v_conservados_antes;

    DELETE h FROM historial_estado_reembolso h
        JOIN reembolso r ON r.id=h.reembolso_id JOIN pago p ON p.id=r.pago_id
        WHERE p.alquiler_id IN (1,2,3,4);
    DELETE r FROM reembolso r JOIN pago p ON p.id=r.pago_id WHERE p.alquiler_id IN (1,2,3,4);
    IF ROW_COUNT() <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: cantidad inesperada de reembolsos. Se revierte la limpieza.';
    END IF;
    DELETE h FROM historial_estado_pago h JOIN pago p ON p.id=h.pago_id WHERE p.alquiler_id IN (1,2,3,4);
    DELETE FROM pago WHERE alquiler_id IN (1,2,3,4);
    IF ROW_COUNT() <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: cantidad inesperada de pagos. Se revierte la limpieza.';
    END IF;

    -- Romper solo el ciclo FK de los detalles que tambien se van a eliminar.
    UPDATE alquiler_detalle SET incidencia_corte_id=NULL
        WHERE alquiler_id IN (1,2,3,4) AND incidencia_corte_id IS NOT NULL;
    DELETE h FROM historial_incidencia h JOIN incidencia i ON i.id=h.incidencia_id
        JOIN alquiler_detalle d ON d.id=i.alquiler_detalle_id WHERE d.alquiler_id IN (1,2,3,4);
    DELETE i FROM incidencia i JOIN alquiler_detalle d ON d.id=i.alquiler_detalle_id
        WHERE d.alquiler_id IN (1,2,3,4);
    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: cantidad inesperada de incidencias. Se revierte la limpieza.';
    END IF;
    DELETE h FROM historial_estado_detalle h JOIN alquiler_detalle d ON d.id=h.alquiler_detalle_id
        WHERE d.alquiler_id IN (1,2,3,4);
    DELETE FROM alquiler_detalle WHERE alquiler_id IN (1,2,3,4);
    IF ROW_COUNT() <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: cantidad inesperada de detalles. Se revierte la limpieza.';
    END IF;
    DELETE FROM alquiler WHERE id IN (1,2,3,4);
    IF ROW_COUNT() <> 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: cantidad inesperada de alquileres. Se revierte la limpieza.';
    END IF;

    IF (SELECT COUNT(*) FROM alquiler) <> 0 OR (SELECT COUNT(*) FROM alquiler_detalle) <> 0
       OR (SELECT COUNT(*) FROM historial_estado_detalle) <> 0 OR (SELECT COUNT(*) FROM incidencia) <> 0
       OR (SELECT COUNT(*) FROM historial_incidencia) <> 0 OR (SELECT COUNT(*) FROM pago) <> 0
       OR (SELECT COUNT(*) FROM historial_estado_pago) <> 0 OR (SELECT COUNT(*) FROM reembolso) <> 0
       OR (SELECT COUNT(*) FROM historial_estado_reembolso) <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: quedaron registros fuera del conjunto esperado. Se revierte todo.';
    END IF;
    SELECT CONCAT_WS(':',
        (SELECT COUNT(*) FROM usuario), (SELECT COUNT(*) FROM persona),
        (SELECT COUNT(*) FROM usuario_rol), (SELECT COUNT(*) FROM rol),
        (SELECT COUNT(*) FROM genero), (SELECT COUNT(*) FROM pais),
        (SELECT COUNT(*) FROM tipo_documento), (SELECT COUNT(*) FROM estado_alquiler),
        (SELECT COUNT(*) FROM estado_alquiler_detalle), (SELECT COUNT(*) FROM estado_bicicleta),
        (SELECT COUNT(*) FROM estado_orden_mantenimiento), (SELECT COUNT(*) FROM estado_pago),
        (SELECT COUNT(*) FROM estacion), (SELECT COUNT(*) FROM bicicleta),
        (SELECT COUNT(*) FROM tipo_bicicleta), (SELECT COUNT(*) FROM tipo_mantenimiento),
        (SELECT COUNT(*) FROM orden_mantenimiento), (SELECT COUNT(*) FROM historial_orden_mantenimiento),
        (SELECT COUNT(*) FROM historial_estado_bicicleta), (SELECT COUNT(*) FROM auditoria_sistema)
    ) INTO v_conservados_despues;
    IF v_conservados_antes <> v_conservados_despues THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: cambiaron conteos de tablas a conservar. Se revierte todo.';
    END IF;

    COMMIT;
END//
DELIMITER ;

CALL turismo_sustentable.qa_limpiar_alquileres_1_4_h1_20261118();
DROP PROCEDURE turismo_sustentable.qa_limpiar_alquileres_1_4_h1_20261118;

-- Resultado esperado: nueve primeras filas en cero; usuarios=8, estaciones=13,
-- bicicletas=22 en la foto revisada. Las otras filas conservan sus cantidades previas.
SELECT 'alquiler' AS tabla, COUNT(*) AS filas FROM alquiler
UNION ALL SELECT 'alquiler_detalle', COUNT(*) FROM alquiler_detalle
UNION ALL SELECT 'historial_estado_detalle', COUNT(*) FROM historial_estado_detalle
UNION ALL SELECT 'incidencia', COUNT(*) FROM incidencia
UNION ALL SELECT 'historial_incidencia', COUNT(*) FROM historial_incidencia
UNION ALL SELECT 'pago', COUNT(*) FROM pago
UNION ALL SELECT 'historial_estado_pago', COUNT(*) FROM historial_estado_pago
UNION ALL SELECT 'reembolso', COUNT(*) FROM reembolso
UNION ALL SELECT 'historial_estado_reembolso', COUNT(*) FROM historial_estado_reembolso
UNION ALL SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL SELECT 'persona', COUNT(*) FROM persona
UNION ALL SELECT 'usuario_rol', COUNT(*) FROM usuario_rol
UNION ALL SELECT 'estacion', COUNT(*) FROM estacion
UNION ALL SELECT 'bicicleta', COUNT(*) FROM bicicleta
UNION ALL SELECT 'orden_mantenimiento', COUNT(*) FROM orden_mantenimiento
UNION ALL SELECT 'historial_orden_mantenimiento', COUNT(*) FROM historial_orden_mantenimiento
UNION ALL SELECT 'historial_estado_bicicleta', COUNT(*) FROM historial_estado_bicicleta
UNION ALL SELECT 'auditoria_sistema', COUNT(*) FROM auditoria_sistema;
