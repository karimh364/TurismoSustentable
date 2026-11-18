-- SOLO LECTURA. Ejecutar despues de la migracion 05 y de recorrer el circuito.
USE turismo_sustentable;

SELECT (SELECT COUNT(*) FROM information_schema.TABLES
        WHERE TABLE_SCHEMA='turismo_sustentable' AND TABLE_TYPE='BASE TABLE') AS tablas,
       (SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA='turismo_sustentable') AS claves_foraneas;
-- Esperado en esta BD: 30 tablas / 59 FK. No acredita por si solo todo el DER.

SELECT a.id, e.codigo AS estado_actual, COUNT(h.id) AS movimientos
FROM alquiler a JOIN estado_alquiler e ON e.id=a.estado_alquiler_id
LEFT JOIN historial_estado_alquiler h ON h.alquiler_id=a.id
GROUP BY a.id,e.codigo ORDER BY a.id;

SELECT h.id,h.alquiler_id,anterior.codigo AS estado_anterior,nuevo.codigo AS estado_nuevo,
       h.fecha_hora_cambio AS fecha_guardada,h.origen,h.actor_usuario_id,u.nickname AS actor,h.observaciones
FROM historial_estado_alquiler h
LEFT JOIN estado_alquiler anterior ON anterior.id=h.estado_anterior_id
JOIN estado_alquiler nuevo ON nuevo.id=h.estado_nuevo_id
LEFT JOIN usuario u ON u.id=h.actor_usuario_id
ORDER BY h.alquiler_id,h.id;

-- Cada contador debe ser cero. Con cero alquileres no prueba todavia las transiciones.
SELECT COUNT(*) AS alquileres_sin_un_unico_evento_inicial
FROM alquiler a WHERE (SELECT COUNT(*) FROM historial_estado_alquiler h
    JOIN estado_alquiler e ON e.id=h.estado_nuevo_id
    WHERE h.alquiler_id=a.id AND h.estado_anterior_id IS NULL AND e.codigo='RESERVADO') <> 1;

SELECT COUNT(*) AS cabeceras_no_coinciden_con_su_ultimo_evento
FROM alquiler a WHERE NOT EXISTS (
    SELECT 1 FROM historial_estado_alquiler h WHERE h.alquiler_id=a.id
      AND h.id=(SELECT MAX(ultimo.id) FROM historial_estado_alquiler ultimo WHERE ultimo.alquiler_id=a.id)
      AND h.estado_nuevo_id=a.estado_alquiler_id);

SELECT COUNT(*) AS cadenas_de_estados_incoherentes
FROM historial_estado_alquiler h
LEFT JOIN historial_estado_alquiler previo ON previo.id=(
    SELECT MAX(p.id) FROM historial_estado_alquiler p WHERE p.alquiler_id=h.alquiler_id AND p.id<h.id)
WHERE (previo.id IS NULL AND h.estado_anterior_id IS NOT NULL)
   OR (previo.id IS NOT NULL AND NOT (h.estado_anterior_id <=> previo.estado_nuevo_id))
   OR h.estado_anterior_id=h.estado_nuevo_id;

SELECT COUNT(*) AS eventos_con_origen_actor_incoherentes
FROM historial_estado_alquiler
WHERE origen NOT IN ('USUARIO','SISTEMA')
   OR (origen='USUARIO' AND actor_usuario_id IS NULL)
   OR (origen='SISTEMA' AND actor_usuario_id IS NOT NULL);
