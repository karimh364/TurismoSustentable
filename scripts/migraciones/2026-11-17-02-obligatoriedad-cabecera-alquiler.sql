-- MIGRACION MANUAL HISTORICA. YA APLICADA en turismo_sustentable el 17/11/2026.
-- NO volver a importar en la BD actual ni ejecutar en una BD nueva del modelo v5.
-- No se ejecuta automaticamente al iniciar Spring Boot. Leer README.md.
--
-- Alcance: ocho columnas de ALQUILER pasan a NOT NULL sin cambiar sus tipos.
-- Se conserva la relacion con ESTADO_ALQUILER, con nombre nuevo y mismas reglas.
-- No se eliminan filas, se rellenan datos ni se cambian importes o fechas.
--
-- Precondiciones comprobadas antes de la ejecucion original:
--   * App detenida; respaldo completo de estructura y datos fuera del repositorio:
--     turismo_sustentable_pre_not_null_2026-11-17.sql.
--   * Migracion 01 ya aplicada; dos alquileres y cero cabeceras incompletas.
--   * Tipos, precision, charset/collation y FK original comprobados por lectura.
--   * Nombre fk_alquiler_estado_v5 libre en el esquema.
-- El nombre de la FK original puede variar en otro entorno: comprobarlo antes.
--
-- El primer intento, que reutilizaba el nombre original en ADD CONSTRAINT,
-- fallo con error 1005/121 por nombre duplicado durante la reconstruccion.
-- Se comprobo que columnas, FK y dos alquileres permanecian antes del reintento.
-- Este archivo conserva SOLO la sentencia corregida que se aplico con exito.
-- FOREIGN_KEY_CHECKS no se desactivo. DDL con commit implicito, sin ROLLBACK.
-- No es idempotente y los comentarios no implementan guardas automaticas.

ALTER TABLE `turismo_sustentable`.`alquiler`
    DROP FOREIGN KEY `FK274n2ktofw0mt1egbnipbonxk`,
    MODIFY COLUMN `estado_alquiler_id` INT(11) NOT NULL,
    MODIFY COLUMN `cotizacion_ars_por_usd` DECIMAL(20,8) NOT NULL,
    MODIFY COLUMN `fuente_cotizacion`
        VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
    MODIFY COLUMN `fecha_hora_aplicacion_cotizacion` DATETIME(6) NOT NULL,
    MODIFY COLUMN `fecha_hora_creacion` DATETIME(6) NOT NULL,
    MODIFY COLUMN `fecha_hora_vencimiento_reserva` DATETIME(6) NOT NULL,
    MODIFY COLUMN `costo_estimado_usd` DECIMAL(21,2) NOT NULL,
    MODIFY COLUMN `costo_estimado_ars` DECIMAL(21,2) NOT NULL,
    ADD CONSTRAINT `fk_alquiler_estado_v5`
        FOREIGN KEY (`estado_alquiler_id`)
        REFERENCES `turismo_sustentable`.`estado_alquiler` (`id`)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT;

-- Comprobaciones de solo lectura. Ejecutables por separado SIN repetir el ALTER.
-- Resultado: ocho NO y seis YES; conservar los tipos indicados.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'turismo_sustentable'
  AND TABLE_NAME = 'alquiler'
  AND COLUMN_NAME IN (
      'estado_alquiler_id', 'cotizacion_ars_por_usd', 'fuente_cotizacion',
      'fecha_hora_aplicacion_cotizacion', 'fecha_hora_creacion',
      'fecha_hora_vencimiento_reserva', 'costo_estimado_usd', 'costo_estimado_ars',
      'fecha_hora_actualizacion_fuente', 'fecha_hora_cierre',
      'fecha_hora_cancelacion', 'fecha_hora_liquidacion',
      'costo_final_usd', 'costo_final_ars'
  )
ORDER BY IS_NULLABLE, COLUMN_NAME;

-- Tres FK; estado_alquiler_id -> estado_alquiler.id conserva ambas RESTRICT.
SELECT k.CONSTRAINT_NAME, k.COLUMN_NAME, k.REFERENCED_TABLE_NAME,
       k.REFERENCED_COLUMN_NAME, r.UPDATE_RULE, r.DELETE_RULE
FROM information_schema.KEY_COLUMN_USAGE k
JOIN information_schema.REFERENTIAL_CONSTRAINTS r
  ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
 AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
 AND r.TABLE_NAME = k.TABLE_NAME
WHERE k.CONSTRAINT_SCHEMA = 'turismo_sustentable'
  AND k.TABLE_NAME = 'alquiler'
ORDER BY k.CONSTRAINT_NAME;

-- Foto de la demostracion: 29 tablas, 55 FK, 2 alquileres y 0 incompletos.
SELECT
    (SELECT COUNT(*) FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = 'turismo_sustentable'
       AND TABLE_TYPE = 'BASE TABLE') AS tablas,
    (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
     WHERE CONSTRAINT_SCHEMA = 'turismo_sustentable') AS claves_foraneas,
    COUNT(*) AS alquileres,
    COALESCE(SUM(
        estado_alquiler_id IS NULL OR cotizacion_ars_por_usd IS NULL
        OR fuente_cotizacion IS NULL OR TRIM(fuente_cotizacion) = ''
        OR fecha_hora_aplicacion_cotizacion IS NULL
        OR fecha_hora_creacion IS NULL OR fecha_hora_vencimiento_reserva IS NULL
        OR costo_estimado_usd IS NULL OR costo_estimado_ars IS NULL
    ), 0) AS cabeceras_incompletas
FROM turismo_sustentable.alquiler;
