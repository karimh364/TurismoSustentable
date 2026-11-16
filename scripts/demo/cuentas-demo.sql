-- SOLO DEMOSTRACION LOCAL. Cuatro cuentas ficticias; no usar en produccion.
-- Clave comun: Prueba123* (BCrypt verificado con el encoder del proyecto).
-- Conserva usuarios existentes e historiales. No crea saldos, pagos ni auditorias ficticias.
-- Importar en phpMyAdmin sobre turismo_sustentable, con los catalogos ya cargados.
USE turismo_sustentable;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS demo_cuentas_20261116;
DELIMITER //
CREATE PROCEDURE demo_cuentas_20261116()
BEGIN
    DECLARE v_genero INT;
    DECLARE v_pais INT;
    DECLARE v_documento INT;
    DECLARE v_hash VARCHAR(255) DEFAULT '$2a$10$hpZ6HZ3sM0x5itZG3aJ6uObrtbYLEJ7rWNmKx1lcQQRXktQnOPHue';
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS demo_cuentas_tmp;
        RESIGNAL;
    END;

    SET v_genero = (SELECT id FROM genero WHERE codigo='PREFIERO_NO_DECIR' LIMIT 1);
    SET v_pais = (SELECT id FROM pais WHERE codigo='AR' LIMIT 1);
    SET v_documento = (SELECT id FROM tipo_documento WHERE codigo='DNI' LIMIT 1);
    IF v_genero IS NULL OR v_pais IS NULL OR v_documento IS NULL
       OR (SELECT COUNT(*) FROM rol WHERE codigo IN ('ADMIN','CLIENTE','TECNICO')) <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Faltan catalogos: iniciar primero el perfil refactor con validate.';
    END IF;

    DROP TEMPORARY TABLE IF EXISTS demo_cuentas_tmp;
    CREATE TEMPORARY TABLE demo_cuentas_tmp (
        nickname VARCHAR(30) PRIMARY KEY,
        email VARCHAR(254) NOT NULL UNIQUE,
        nombre VARCHAR(50) NOT NULL,
        apellido VARCHAR(50) NOT NULL,
        documento VARCHAR(50) NOT NULL UNIQUE,
        codigo_rol VARCHAR(30) NOT NULL
    );
    INSERT INTO demo_cuentas_tmp VALUES
      ('admin.demo','admin.demo@example.com','Admin','Demostracion','99000001','ADMIN'),
      ('cliente.demo','cliente.demo@example.com','Cliente','Demostracion','99000002','CLIENTE'),
      ('tecnico.demo','tecnico.demo@example.com','Tecnico','Demostracion','99000003','TECNICO'),
      ('parcial_playwright','parcial_playwright@example.com','Parcial','Playwright','99000004','CLIENTE');

    START TRANSACTION;

    -- Si un nombre/email pertenece a otros datos, abortar en vez de sobrescribirlo.
    IF EXISTS (
        SELECT 1 FROM demo_cuentas_tmp d
        JOIN usuario u ON u.nickname=d.nickname OR u.email=d.email
        JOIN persona p ON p.id=u.persona_id
        WHERE u.nickname<>d.nickname OR u.email<>d.email
           OR p.numero_documento<>d.documento OR p.tipo_documento_id<>v_documento
           OR p.pais_emisor_id<>v_pais OR u.activo<>b'1'
           OR BINARY u.password<>BINARY v_hash
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existe una cuenta de demo con otros datos. No se modifico: revisar antes de continuar.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM demo_cuentas_tmp d
        JOIN persona p ON p.numero_documento=d.documento
           AND p.tipo_documento_id=v_documento AND p.pais_emisor_id=v_pais
        WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.nickname=d.nickname AND u.persona_id=p.id)
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Un documento ficticio ya esta en uso. No se crearon cuentas.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM demo_cuentas_tmp d JOIN usuario u ON u.nickname=d.nickname
        JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id
        WHERE r.codigo<>d.codigo_rol
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Una cuenta existente tiene otros roles. No se alteraron sus permisos.';
    END IF;

    INSERT INTO persona (nombre,apellido,fecha_nacimiento,numero_documento,genero_id,pais_emisor_id,tipo_documento_id)
    SELECT d.nombre,d.apellido,'1990-01-01',d.documento,v_genero,v_pais,v_documento
    FROM demo_cuentas_tmp d
    WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.nickname=d.nickname);

    INSERT INTO usuario (activo,email,fecha_registro,nickname,password,version,persona_id)
    SELECT b'1',d.email,UTC_TIMESTAMP(6),d.nickname,v_hash,0,p.id
    FROM demo_cuentas_tmp d
    JOIN persona p ON p.numero_documento=d.documento
       AND p.tipo_documento_id=v_documento AND p.pais_emisor_id=v_pais
    WHERE NOT EXISTS (SELECT 1 FROM usuario u WHERE u.nickname=d.nickname);

    INSERT INTO usuario_rol (usuario_id,rol_id)
    SELECT u.id,r.id FROM demo_cuentas_tmp d JOIN usuario u ON u.nickname=d.nickname
    JOIN rol r ON r.codigo=d.codigo_rol
    WHERE NOT EXISTS (SELECT 1 FROM usuario_rol ur WHERE ur.usuario_id=u.id AND ur.rol_id=r.id);

    COMMIT;
    DROP TEMPORARY TABLE demo_cuentas_tmp;
END//
CALL demo_cuentas_20261116()//
DROP PROCEDURE demo_cuentas_20261116//
DELIMITER ;

-- Deben aparecer cuatro filas y un solo rol por cuenta.
SELECT u.nickname,u.email,u.activo,r.codigo AS rol
FROM usuario u JOIN usuario_rol ur ON ur.usuario_id=u.id JOIN rol r ON r.id=ur.rol_id
WHERE u.nickname IN ('admin.demo','cliente.demo','tecnico.demo','parcial_playwright')
ORDER BY u.nickname;
