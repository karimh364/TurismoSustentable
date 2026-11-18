-- MIGRACION MANUAL HISTORICA. YA APLICADA en turismo_sustentable el 17/11/2026.
-- NO volver a importar en la BD actual ni ejecutar en una BD nueva del modelo v5.
-- No se ejecuta automaticamente al iniciar Spring Boot.
--
-- Alcance: retirar dos FK y ocho columnas del alquiler anterior, sin borrar filas.
-- Precondiciones comprobadas antes de la ejecucion original:
--   * Aplicacion detenida y respaldo SQL de estructura y datos confirmado.
--   * 2 alquileres; 0 sin estado v5; 0 con valores en las ocho columnas antiguas.
--   * FK locales identificadas por information_schema, con los nombres de abajo.
-- Estos nombres pueden variar en otro entorno. No copiarlos sin comprobarlos.
--
-- DDL con commit implicito: ROLLBACK no revierte esta migracion.
-- No es idempotente; no se usa IF EXISTS para ocultar diferencias de esquema.
-- Leer README.md antes de utilizar este archivo como referencia.

ALTER TABLE `turismo_sustentable`.`alquiler`
    DROP FOREIGN KEY `FKdxuemx5ere0m6csm2g27s5xhn`,
    DROP FOREIGN KEY `FKe22jodmu0283vv3ui1xqanyrc`,
    DROP COLUMN `bicicleta_id`,
    DROP COLUMN `estacion_devolucion_id`,
    DROP COLUMN `estado`,
    DROP COLUMN `fecha_inicio`,
    DROP COLUMN `fecha_fin`,
    DROP COLUMN `costo_total`,
    DROP COLUMN `costo_total_ars`,
    DROP COLUMN `precio_por_hora_aplicado`;

-- Comprobaciones posteriores, todas de solo lectura.
-- Estas SELECT se pueden ejecutar separadas del ALTER sobre la BD ya migrada.

SELECT COUNT(*) AS columnas_antiguas_restantes
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'turismo_sustentable'
  AND TABLE_NAME = 'alquiler'
  AND COLUMN_NAME IN (
      'bicicleta_id',
      'estacion_devolucion_id',
      'estado',
      'fecha_inicio',
      'fecha_fin',
      'costo_total',
      'costo_total_ars',
      'precio_por_hora_aplicado'
  );

SELECT COUNT(*) AS relaciones_antiguas_restantes
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = 'turismo_sustentable'
  AND TABLE_NAME = 'alquiler'
  AND COLUMN_NAME IN ('bicicleta_id', 'estacion_devolucion_id')
  AND REFERENCED_TABLE_NAME IS NOT NULL;

SELECT COUNT(*) AS claves_foraneas
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'turismo_sustentable'
  AND CONSTRAINT_TYPE = 'FOREIGN KEY';

SELECT COUNT(*) AS tablas
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'turismo_sustentable'
  AND TABLE_TYPE = 'BASE TABLE';

SELECT COUNT(*) AS alquileres_conservados
FROM turismo_sustentable.alquiler;

SELECT COUNT(*) AS cabeceras_sin_detalles
FROM turismo_sustentable.alquiler a
WHERE NOT EXISTS (
    SELECT 1
    FROM turismo_sustentable.alquiler_detalle d
    WHERE d.alquiler_id = a.id
);
