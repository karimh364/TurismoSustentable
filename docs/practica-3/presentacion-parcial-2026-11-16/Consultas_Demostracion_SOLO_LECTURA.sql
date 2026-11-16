-- Turismo Sustentable. Demostración de SOLO LECTURA.
-- Ejecutar bloques por separado. No hay INSERT, UPDATE, DELETE ni DDL.
USE turismo_sustentable;

-- 1. Entidades presentes y restricciones. No devuelve datos de credenciales.
SHOW TABLES;
SHOW CREATE TABLE usuario_rol;
SHOW CREATE TABLE alquiler_detalle;

-- 2. Roles de las dos cuentas de prueba.
SELECT u.id, u.nickname, r.codigo AS rol, u.activo
FROM usuario u
JOIN usuario_rol ur ON ur.usuario_id = u.id
JOIN rol r ON r.id = ur.rol_id
WHERE u.nickname IN ('qa.circuito','qa.tecnico')
ORDER BY u.nickname, r.codigo;

-- 3. Inventario: estado y presencia física se guardan por separado.
SELECT b.id, b.codigo_bicicleta, eb.codigo AS estado,
       e.nombre AS estacion_actual, e.es_publica, t.codigo AS tipo
FROM bicicleta b
JOIN estado_bicicleta eb ON eb.id = b.estado_bicicleta_id
JOIN tipo_bicicleta t ON t.id = b.tipo_bicicleta_id
LEFT JOIN estacion e ON e.id = b.estacion_id
WHERE b.codigo_bicicleta = 'QA-CIR-01';

-- 4. Cabeceras del recorrido de prueba. Importes guardados, no recalculados.
SELECT a.id, u.nickname, ea.codigo AS estado, a.cotizacion_ars_por_usd,
       a.fuente_cotizacion, a.costo_estimado_ars, a.costo_final_ars
FROM alquiler a
JOIN usuario u ON u.id = a.usuario_id
JOIN estado_alquiler ea ON ea.id = a.estado_alquiler_id
WHERE u.nickname = 'qa.circuito'
ORDER BY a.id;

-- 5. Detalles: fin de uso puede tener un fundamento distinto a devolución.
SELECT d.id, d.alquiler_id, b.codigo_bicicleta, ed.codigo AS estado_detalle,
       d.fecha_hora_retiro, d.fecha_hora_devolucion_estimada,
       d.fecha_hora_devolucion_real, d.fecha_hora_fin_uso,
       TIMESTAMPDIFF(SECOND,d.fecha_hora_retiro,d.fecha_hora_fin_uso) AS segundos,
       d.subtotal_real_usd, d.subtotal_real_ars, d.incidencia_corte_id
FROM alquiler_detalle d
JOIN bicicleta b ON b.id = d.bicicleta_id
JOIN estado_alquiler_detalle ed ON ed.id = d.estado_alquiler_detalle_id
WHERE b.codigo_bicicleta = 'QA-CIR-01'
ORDER BY d.alquiler_id, d.id;

-- 6. Pago original y reintegro independiente.
SELECT p.id AS pago_id, p.alquiler_id, ep.codigo AS estado_pago,
       p.finalidad, p.monto_ars, p.moneda, p.plataforma, p.entorno
FROM pago p
JOIN estado_pago ep ON ep.id = p.estado_pago_id
WHERE p.alquiler_id IN (1,2)
ORDER BY p.id;

SELECT r.id AS reembolso_id, r.pago_id, r.estado, r.monto_ars, r.moneda
FROM reembolso r JOIN pago p ON p.id = r.pago_id
WHERE p.alquiler_id IN (1,2)
ORDER BY r.id;

-- 7. Estructura para ubicar los enlaces de mantenimiento e incidencias.
-- Mostrar estas definiciones antes de explorar filas; no cambia ninguna tabla.
SHOW CREATE TABLE incidencia;
SHOW CREATE TABLE orden_mantenimiento;
SHOW CREATE TABLE historial_orden_mantenimiento;

-- 8. Auditoría administrativa sin volcar snapshots ni credenciales.
SELECT id, administrador_id, entidad_afectada, entidad_id, accion, fecha_operacion
FROM auditoria_sistema
ORDER BY id DESC
LIMIT 10;
