-- MIGRACION MANUAL HISTORICA. YA APLICADA por el usuario el 17/11/2026.
-- NO volver a importar en la BD actual ni aplicar a una BD nueva del modelo v5.
-- No se ejecuta automaticamente al iniciar Spring Boot. Leer README.md.
-- Procedimiento indicado: compilar/probar, detener la app y respaldar estructura
-- y datos fuera del repositorio. El asistente no inspecciono la exportacion.
-- DDL con commit implicito, sin ROLLBACK. El asistente no ejecuto este ALTER.
--
-- Alcance: latitud y longitud de ESTACION, DOUBLE NOT NULL -> DECIMAL(9,6) NOT NULL.
-- Conservar nombres, ubicaciones, capacidad, uso, version, filas y relaciones.
-- Seis decimales; rangos del dominio validados en entidad y DTO, no por este DDL.
-- La precision fisica se concreta aqui; el DER logico indica decimal sin escala.
--
-- PRECONTROL SOLO LECTURA. Exigir cero en los tres controles antes del ALTER.
-- El 17/11, antes del cambio, SELECT del asistente obtuvo 12 estaciones y tres
-- ceros. Esa foto no sustituye ejecutar nuevamente el control antes de migrar.
-- Si coordenadas_que_cambian no es cero, detenerse: no redondear automaticamente.
-- El control siguiente corresponde al estado previo DOUBLE; no repetir el ALTER
-- por obtener ceros en una base ya migrada. Los comentarios no son guardas.
SELECT COUNT(*) AS estaciones,
       COALESCE(SUM(latitud IS NULL OR longitud IS NULL), 0) AS sin_coordenadas,
       COALESCE(SUM(latitud NOT BETWEEN -90 AND 90
                    OR longitud NOT BETWEEN -180 AND 180), 0) AS fuera_de_rango,
       COALESCE(SUM(
           CAST(CAST(latitud AS DECIMAL(9,6)) AS DOUBLE) <> latitud
           OR CAST(CAST(longitud AS DECIMAL(9,6)) AS DOUBLE) <> longitud
       ), 0) AS coordenadas_que_cambian
FROM turismo_sustentable.estacion;

-- PASO SEPARADO. Solo con controles correctos, app detenida y respaldo completo.
-- Los comentarios NO implementan guardas automaticas. No usar IF EXISTS o IGNORE.
ALTER TABLE `turismo_sustentable`.`estacion`
    MODIFY COLUMN `latitud` DECIMAL(9,6) NOT NULL,
    MODIFY COLUMN `longitud` DECIMAL(9,6) NOT NULL;

-- POSTCONTROL SOLO LECTURA: dos DECIMAL(9,6), ambos IS_NULLABLE=NO.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'turismo_sustentable' AND TABLE_NAME = 'estacion'
  AND COLUMN_NAME IN ('latitud', 'longitud')
ORDER BY COLUMN_NAME;

-- Conservar IDs y ubicaciones; cotejar con respaldo/precontrol antes de arrancar.
SELECT id, latitud, longitud FROM turismo_sustentable.estacion ORDER BY id;

-- Evidencia posterior: ambas columnas DECIMAL(9,6) NOT NULL; 12 estaciones,
-- 29 tablas y 55 FK tras el DDL. La prueba manual agrego QA Coordenadas, ID 13:
-- CREAR latitud -53.787601; ACTUALIZAR -53.787602, longitud -67.700201 constante.
-- Version 1 y auditorias 16/17 de admin.demo verificadas mediante SELECT.
-- El usuario confirmo rechazo de siete decimales y mapa correcto.
-- Ultima tanda Java: 331 casos/11 suites aprobados; JS aprobados segun usuario.
-- No equivale a una nueva regresion SQL automatizada integral.
