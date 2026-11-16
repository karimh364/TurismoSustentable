-- DEMOSTRACION: 10 estaciones ficticias y 20 bicicletas NUEVAS en Rio Grande.
-- Coordenadas APROXIMADAS; no representan estaciones municipales existentes.
-- Capacidad minima del modelo: 1. Usamos 3/5/10/15, con vacias y llenas.
-- Conserva datos existentes. Si un nombre/codigo existe, ABORTA sin sobrescribir.
USE turismo_sustentable;
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS demo_rg_inventario_20261116;
DELIMITER //
CREATE PROCEDURE demo_rg_inventario_20261116()
BEGIN
    DECLARE v_fecha DATETIME(6);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS demo_rg_bicis_tmp;
        DROP TEMPORARY TABLE IF EXISTS demo_rg_estaciones_tmp;
        RESIGNAL;
    END;

    IF (SELECT COUNT(*) FROM tipo_bicicleta WHERE codigo IN ('MONTANA','PASEO','ELECTRICA'))<>3
       OR (SELECT COUNT(*) FROM estado_bicicleta WHERE codigo IN ('DISPONIBLE','INACTIVA'))<>2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Faltan catalogos de tipos/estados. No se cargo inventario.';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS demo_rg_estaciones_tmp;
    DROP TEMPORARY TABLE IF EXISTS demo_rg_bicis_tmp;
    CREATE TEMPORARY TABLE demo_rg_estaciones_tmp (
        clave VARCHAR(12) PRIMARY KEY,
        nombre VARCHAR(255) NOT NULL UNIQUE,
        direccion VARCHAR(255) NOT NULL,
        capacidad INT NOT NULL,
        publica TINYINT NOT NULL,
        latitud DOUBLE NOT NULL,
        longitud DOUBLE NOT NULL
    );
    CREATE TEMPORARY TABLE demo_rg_bicis_tmp (
        codigo VARCHAR(255) PRIMARY KEY,
        tipo VARCHAR(30) NOT NULL,
        estacion_clave VARCHAR(12) NOT NULL,
        estado VARCHAR(30) NOT NULL
    );

    -- Direcciones de referencia; los puntos del mapa NO son geocodificaciones exactas.
    INSERT INTO demo_rg_estaciones_tmp VALUES
      ('CEN','RG DEMO - Centro','Av. San Martin y Belgrano, Rio Grande',3,1,-53.7866,-67.7046),
      ('MUS','RG DEMO - Museo','Alberdi 555, entorno del Museo Virginia Choquintel, Rio Grande',5,1,-53.7885,-67.7065),
      ('COS','RG DEMO - Costanera','Libertad y Peron, sector Costanera, Rio Grande',10,1,-53.7920,-67.7130),
      ('CII','RG DEMO - Chacra II','Luisa Rosso 779, sector Chacra II, Rio Grande',15,1,-53.7723,-67.7310),
      ('CIV','RG DEMO - Chacra IV','Av. San Martin y Ricardo Balbin, sector Chacra IV, Rio Grande',3,1,-53.7724,-67.7526),
      ('CXIII','RG DEMO - Chacra XIII','Taparello 389, sector Chacra XIII, Rio Grande',5,1,-53.7519,-67.7497),
      ('SUR','RG DEMO - Margen Sur','El Alambrador 212, entorno de Margen Sur, Rio Grande',10,1,-53.8062,-67.7110),
      ('AER','RG DEMO - Aeropuerto','Echelaine y Los Calafate, barrio Aeropuerto, Rio Grande',15,1,-53.7734,-67.7460),
      ('TP','RG DEMO - Taller Industrial','Islas Malvinas y Combate de Montevideo, sector industrial, Rio Grande',5,0,-53.7928,-67.7363),
      ('TMS','RG DEMO - Taller Margen Sur','Tolhuin y Wonska, sector Austral, Rio Grande',10,0,-53.8170,-67.7190);

    -- Talleres: INACTIVA en deposito. No se inventan reparaciones ni reservas.
    INSERT INTO demo_rg_bicis_tmp VALUES
      ('MB-01','MONTANA','CEN','DISPONIBLE'),
      ('PA-01','PASEO','CEN','DISPONIBLE'),
      ('EL-01','ELECTRICA','CEN','DISPONIBLE'),
      ('MB-02','MONTANA','MUS','DISPONIBLE'),
      ('MB-03','MONTANA','MUS','DISPONIBLE'),
      ('PA-02','PASEO','MUS','DISPONIBLE'),
      ('PA-03','PASEO','MUS','DISPONIBLE'),
      ('EL-02','ELECTRICA','MUS','DISPONIBLE'),
      ('MB-04','MONTANA','COS','DISPONIBLE'),
      ('PA-04','PASEO','COS','DISPONIBLE'),
      ('EL-03','ELECTRICA','COS','DISPONIBLE'),
      ('MB-05','MONTANA','CII','DISPONIBLE'),
      ('PA-05','PASEO','CII','DISPONIBLE'),
      ('MB-06','MONTANA','CXIII','DISPONIBLE'),
      ('EL-04','ELECTRICA','CXIII','DISPONIBLE'),
      ('PA-06','PASEO','SUR','DISPONIBLE'),
      ('MB-07','MONTANA','TP','INACTIVA'),
      ('PA-07','PASEO','TP','INACTIVA'),
      ('MB-08','MONTANA','TMS','INACTIVA'),
      ('EL-05','ELECTRICA','TMS','INACTIVA');

    START TRANSACTION;
    SET v_fecha=UTC_TIMESTAMP(6);
    IF EXISTS (SELECT 1 FROM estacion e JOIN demo_rg_estaciones_tmp d ON d.nombre=e.nombre)
       OR EXISTS (SELECT 1 FROM bicicleta b JOIN demo_rg_bicis_tmp d ON d.codigo=b.codigo_bicicleta) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ya existe un nombre o codigo de este lote. No se sobrescribio ni duplico inventario.';
    END IF;
    IF EXISTS (
        SELECT e.clave FROM demo_rg_estaciones_tmp e
        LEFT JOIN demo_rg_bicis_tmp b ON b.estacion_clave=e.clave
        GROUP BY e.clave,e.capacidad HAVING COUNT(b.codigo)>e.capacidad
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El lote supera la capacidad de una estacion. No se cargo.';
    END IF;
    IF EXISTS (
        SELECT 1 FROM demo_rg_bicis_tmp b
        LEFT JOIN demo_rg_estaciones_tmp e ON e.clave=b.estacion_clave WHERE e.clave IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='El lote tiene una ubicacion inexistente o supera la capacidad. No se cargo.';
    END IF;

    INSERT INTO estacion (nombre,direccion,capacidad,borrado_estacion,es_publica,latitud,longitud,version)
    SELECT nombre,direccion,capacidad,b'0',publica,latitud,longitud,0 FROM demo_rg_estaciones_tmp;

    INSERT INTO bicicleta (codigo_bicicleta,tipo_bicicleta_id,estado_bicicleta_id,fecha_alta,version,estacion_id,borrado_bici)
    SELECT d.codigo,t.id,s.id,DATE(v_fecha),0,e.id,b'0'
    FROM demo_rg_bicis_tmp d
    JOIN tipo_bicicleta t ON t.codigo=d.tipo
    JOIN estado_bicicleta s ON s.codigo=d.estado
    JOIN demo_rg_estaciones_tmp destino ON destino.clave=d.estacion_clave
    JOIN estacion e ON e.nombre=destino.nombre;

    INSERT INTO historial_estado_bicicleta
        (bicicleta_id,estado_anterior_id,estado_nuevo_id,actor_usuario_id,origen,fecha_hora_cambio,observaciones)
    SELECT b.id,NULL,b.estado_bicicleta_id,NULL,'SISTEMA',v_fecha,
           'Alta inicial por carga demo Rio Grande; sin alquiler ni intervencion tecnica previa.'
    FROM bicicleta b JOIN demo_rg_bicis_tmp d ON d.codigo=b.codigo_bicicleta;

    COMMIT;
    DROP TEMPORARY TABLE demo_rg_bicis_tmp;
    DROP TEMPORARY TABLE demo_rg_estaciones_tmp;
END//
CALL demo_rg_inventario_20261116()//
DROP PROCEDURE demo_rg_inventario_20261116//
DELIMITER ;

-- Comprobaciones FUERA del procedimiento: no devuelve varios juegos de filas en CALL.
SELECT e.nombre,CASE WHEN e.es_publica=b'1' THEN 'PUBLICA' ELSE 'TALLER INTERNO' END AS uso,
       e.capacidad,COUNT(b.id) AS bicicletas_presentes,e.capacidad-COUNT(b.id) AS espacios_libres
FROM estacion e LEFT JOIN bicicleta b ON b.estacion_id=e.id
WHERE e.nombre IN ('RG DEMO - Centro','RG DEMO - Museo','RG DEMO - Costanera',
    'RG DEMO - Chacra II','RG DEMO - Chacra IV','RG DEMO - Chacra XIII',
    'RG DEMO - Margen Sur','RG DEMO - Aeropuerto',
    'RG DEMO - Taller Industrial','RG DEMO - Taller Margen Sur')
GROUP BY e.id,e.nombre,e.es_publica,e.capacidad ORDER BY e.nombre;

SELECT b.codigo_bicicleta,t.codigo AS tipo,s.codigo AS estado,e.nombre AS estacion
FROM bicicleta b JOIN tipo_bicicleta t ON t.id=b.tipo_bicicleta_id
JOIN estado_bicicleta s ON s.id=b.estado_bicicleta_id JOIN estacion e ON e.id=b.estacion_id
WHERE b.codigo_bicicleta IN ('MB-01','MB-02','MB-03','MB-04','MB-05','MB-06','MB-07','MB-08',
    'PA-01','PA-02','PA-03','PA-04','PA-05','PA-06','PA-07','EL-01','EL-02','EL-03','EL-04','EL-05')
ORDER BY b.codigo_bicicleta;
