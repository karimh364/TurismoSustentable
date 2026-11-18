-- MIGRACION MANUAL NUEVA. No ejecutada por el asistente.
-- App detenida + respaldo completo. MariaDB activa. Importar el archivo completo.
-- Requiere la limpieza QA aprobada: cero alquileres; no inventa eventos retrospectivos.
-- Agrega UNA tabla y CUATRO FK; no modifica ni elimina filas/tablas existentes.
-- CREATE TABLE/PROCEDURE son DDL con commit implicito, no una limpieza reversible por ROLLBACK.
-- No desactiva FK, no usa create-drop y no forma parte del arranque de Spring Boot.
USE turismo_sustentable;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS turismo_sustentable.migrar_historial_alquiler_h1_20261118;
DELIMITER //
CREATE PROCEDURE turismo_sustentable.migrar_historial_alquiler_h1_20261118()
SQL SECURITY INVOKER
MODIFIES SQL DATA
BEGIN
    IF DATABASE() <> 'turismo_sustentable' OR @@SESSION.foreign_key_checks <> 1 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: usar turismo_sustentable con comprobacion de FK activada.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA='turismo_sustentable'
            AND TABLE_NAME='historial_estado_alquiler') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: la tabla ya existe. Comprobar estructura; no repetir la migracion.';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='turismo_sustentable'
            AND TABLE_NAME IN ('alquiler','estado_alquiler','usuario') AND ENGINE='InnoDB') <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: faltan las tablas padre InnoDB esperadas.';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='turismo_sustentable'
            AND TABLE_NAME IN ('alquiler','estado_alquiler','usuario')
            AND COLUMN_NAME='id' AND DATA_TYPE='int' AND COLUMN_TYPE NOT LIKE '%unsigned%') <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: las claves padre deben ser INT con signo. Revisar tipos.';
    END IF;
    IF EXISTS (SELECT 1 FROM alquiler) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: hay alquileres previos. No se fabrican sus historiales; revisar antes de migrar.';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA='turismo_sustentable' AND CONSTRAINT_NAME IN (
                'fk_hist_alquiler_cabecera','fk_hist_alquiler_anterior',
                'fk_hist_alquiler_nuevo','fk_hist_alquiler_actor')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ABORTADO: uno de los nombres de FK ya esta ocupado.';
    END IF;

    CREATE TABLE historial_estado_alquiler (
        id INT NOT NULL AUTO_INCREMENT,
        alquiler_id INT NOT NULL,
        estado_anterior_id INT NULL,
        estado_nuevo_id INT NOT NULL,
        fecha_hora_cambio DATETIME(6) NOT NULL,
        actor_usuario_id INT NULL,
        origen VARCHAR(20) NOT NULL,
        observaciones TEXT NULL,
        PRIMARY KEY (id),
        KEY ix_hist_alquiler_orden (alquiler_id, id),
        CONSTRAINT fk_hist_alquiler_cabecera FOREIGN KEY (alquiler_id)
            REFERENCES alquiler(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
        CONSTRAINT fk_hist_alquiler_anterior FOREIGN KEY (estado_anterior_id)
            REFERENCES estado_alquiler(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
        CONSTRAINT fk_hist_alquiler_nuevo FOREIGN KEY (estado_nuevo_id)
            REFERENCES estado_alquiler(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
        CONSTRAINT fk_hist_alquiler_actor FOREIGN KEY (actor_usuario_id)
            REFERENCES usuario(id) ON UPDATE RESTRICT ON DELETE RESTRICT,
        CONSTRAINT ck_hist_alquiler_actor CHECK (
            (origen='USUARIO' AND actor_usuario_id IS NOT NULL)
            OR (origen='SISTEMA' AND actor_usuario_id IS NULL)),
        CONSTRAINT ck_hist_alquiler_cambio CHECK (
            estado_anterior_id IS NULL OR estado_anterior_id <> estado_nuevo_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
END//
DELIMITER ;

CALL turismo_sustentable.migrar_historial_alquiler_h1_20261118();
DROP PROCEDURE turismo_sustentable.migrar_historial_alquiler_h1_20261118;

-- Debe mostrar ocho columnas y cuatro FK con RESTRICT. Tabla inicialmente vacia.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='turismo_sustentable' AND TABLE_NAME='historial_estado_alquiler'
ORDER BY ORDINAL_POSITION;
SELECT CONSTRAINT_NAME, REFERENCED_TABLE_NAME, UPDATE_RULE, DELETE_RULE
FROM information_schema.REFERENTIAL_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA='turismo_sustentable' AND TABLE_NAME='historial_estado_alquiler'
ORDER BY CONSTRAINT_NAME;
SELECT COUNT(*) AS movimientos_iniciales FROM historial_estado_alquiler;
SELECT
    (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA='turismo_sustentable' AND TABLE_TYPE='BASE TABLE') AS tablas,
    (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA='turismo_sustentable') AS claves_foraneas,
    (SELECT COUNT(*) FROM usuario) AS usuarios,
    (SELECT COUNT(*) FROM estacion) AS estaciones,
    (SELECT COUNT(*) FROM bicicleta) AS bicicletas;
