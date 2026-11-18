-- MIGRACION MANUAL HISTORICA. YA APLICADA por el usuario el 17/11/2026.
-- NO volver a importar sobre la BD actual ni ejecutar en una BD nueva del v5.
-- No se ejecuta automaticamente al iniciar Spring Boot. Leer README.md.
--
-- Alcance: renombrar dos columnas de USUARIO para coincidir con el DER v5.
-- Conservar tipos, nulabilidad, charset/collation, hashes e indice unico del token.
-- No generar contrasenas, recalcular hashes, borrar usuarios ni importar demos.
--
-- Precondiciones del procedimiento: app detenida y respaldo completo previo,
-- fuera del repositorio (turismo_sustentable_pre_hashes_2026-11-17.sql).
-- La exportacion del respaldo no fue inspeccionada por el asistente.
-- Antes del cambio se comprobaron nombres/tipos/indice: seis usuarios, cero
-- hashes de contrasena ausentes y cero tokens presentes. El indice del token
-- era UK_71sx3rqqapq5lx93qck2pq4t2, unico; CHANGE conserva el indice.
--
-- Actualizar junto con el mapeo JPA de Usuario y scripts/demo/cuentas-demo.sql.
-- Los atributos Java y los contratos de entrada permanecen sin renombrar.
-- DDL con commit implicito: no se revierte con ROLLBACK. No es idempotente;
-- estos comentarios no son guardas automaticas contra otro esquema.

ALTER TABLE `turismo_sustentable`.`usuario`
    CHANGE COLUMN `password` `password_hash`
        VARCHAR(255)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_general_ci
        NOT NULL,
    CHANGE COLUMN `token_recuperacion` `token_recuperacion_hash`
        VARCHAR(64)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_general_ci
        NULL DEFAULT NULL;

-- Solo lectura. Ejecutar estas SELECT por separado SIN repetir el ALTER.
-- Deben aparecer solo las dos columnas nuevas: NO / YES respectivamente.
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE,
       CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'turismo_sustentable'
  AND TABLE_NAME = 'usuario'
  AND COLUMN_NAME IN (
      'password', 'password_hash',
      'token_recuperacion', 'token_recuperacion_hash'
  )
ORDER BY COLUMN_NAME;

-- Mismo indice, con NON_UNIQUE=0 y la columna renombrada.
SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'turismo_sustentable'
  AND TABLE_NAME = 'usuario'
  AND COLUMN_NAME = 'token_recuperacion_hash';

-- Foto de la demostracion: seis usuarios, cero hashes de contrasena ausentes.
-- No mostrar contrasenas, hashes ni tokens en los resultados.
SELECT COUNT(*) AS usuarios,
       COALESCE(SUM(password_hash IS NULL OR TRIM(password_hash) = ''), 0)
           AS hashes_password_ausentes
FROM turismo_sustentable.usuario;
