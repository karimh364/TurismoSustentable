# Migraciones manuales y evidencia de estructura

Estos archivos registran cambios fisicos controlados de la BD. No constituyen
un inicializador, una carga demo ni una configuracion de Flyway/Liquibase.
Spring Boot no debe ejecutarlos automaticamente. El perfil refactor conserva
`ddl-auto=validate` y `spring.sql.init.mode=never`.

## Migracion 2026-11-17-01: alquiler anterior

Estado: **ya aplicada manualmente por el usuario** en `turismo_sustentable`,
con la app detenida y respaldo previo de estructura y datos confirmado.
El archivo [SQL](2026-11-17-01-retirar-alquiler-anterior.sql) es su registro.
**No volver a importarlo sobre la base actual.**

Antes de ejecutarlo se verificaron dos alquileres, ambos del modelo v5, sin
valores en las ocho columnas retiradas. Se identificaron los nombres locales
de las FK mediante information_schema. No se desactivo FOREIGN_KEY_CHECKS.

El cambio retiro solamente estas FK de ALQUILER:

- bicicleta_id -> BICICLETA.id.
- estacion_devolucion_id -> ESTACION.id.

Y estas ocho columnas: bicicleta_id, estacion_devolucion_id, estado, fecha_inicio,
fecha_fin, costo_total, costo_total_ars y precio_por_hora_aplicado.
No elimina tablas o filas, ni modifica importes o fechas del modelo vigente.

## Migracion 2026-11-17-02: obligatoriedad de la cabecera

Estado: **ya aplicada manualmente por el usuario**, despues de la migracion 01.
El [SQL definitivo](2026-11-17-02-obligatoriedad-cabecera-alquiler.sql) registra la
sentencia que funciono; **no volver a importarlo sobre la BD actual**.

Con la app detenida y respaldo completo confirmado fuera del repositorio
(`turismo_sustentable_pre_not_null_2026-11-17.sql`), se comprobaron dos alquileres
y cero cabeceras incompletas. Ocho columnas pasan a NOT NULL: estado_alquiler_id,
cotizacion_ars_por_usd, fuente_cotizacion, fecha_hora_aplicacion_cotizacion,
fecha_hora_creacion, fecha_hora_vencimiento_reserva y los dos costos estimados.
Se conservan sus tipos, precision, escala y charset/collation de la fuente.
Usuario y estacion de retiro ya eran obligatorios en la BD.

Permanecen opcionales fecha_hora_actualizacion_fuente, fecha_hora_cierre,
fecha_hora_cancelacion, fecha_hora_liquidacion y los dos costos finales.
No se inventan ceros, fechas o valores por defecto para completar registros.
NOT NULL no sustituye el control Java NotBlank de fuente_cotizacion.

El primer intento fallo con 1005/121 al recrear la FK con el nombre original.
InnoDB informo nombre duplicado durante la reconstruccion; la lectura posterior
confirmo esquema anterior, 55 FK y dos alquileres. La sentencia corregida retiro
la FK original y agrego fk_alquiler_estado_v5 dentro del mismo ALTER, conservando
estado_alquiler_id -> estado_alquiler.id y ON UPDATE/DELETE RESTRICT.
El nombre nuevo se comprobo libre; FOREIGN_KEY_CHECKS no se desactivo.
El archivo NO incluye el intento fallido como instruccion ejecutable.

El usuario compartio las ocho columnas con IS_NULLABLE=NO y las tres FK de
ALQUILER. Una lectura posterior del asistente corroborro ademas las seis columnas
opcionales con YES, 29 tablas, 55 FK, dos alquileres y cero cabeceras incompletas.
Arranque refactor/validate informado a las 21:23: JPA/Tomcat inicializados y
Started TurismoSustentableApplication, sin error de esquema/mapeo.
La comprobacion de nulabilidad es la consulta SQL; el arranque validate por si
solo no acredita todas las restricciones del DER.

En Java, Alquiler refuerza Bean Validation y mapeo JPA. Las ocho suites de la
ultima tanda del usuario tienen 144 casos, todos aprobados, sin errores u
omisiones; incluyen 17 de AlquilerCabeceraValidacionTest. No es integracion SQL.

## Condiciones para otro entorno

- No aplicar estas migraciones a una BD nueva creada con las entidades actuales:
  no contiene las columnas antiguas y ya tiene el mapeo obligatorio de cabecera.
- No aplicar a una restauracion/instalacion antigua sin verificar esquema,
  referencias y contenido; los nombres de FK pueden ser distintos.
- Obtener respaldo completo y detener las escrituras de la aplicacion antes de
  cualquier DDL. Si existen datos anteriores, definir su conservacion/migracion
  antes de eliminar columnas. No basta con que haya un estado v5 informado.
- Cada ALTER tiene commit implicito y no se revierte con ROLLBACK. El respaldo es
  parte del procedimiento de recuperacion, no una autorizacion para importar
  encima de una base en uso.
- Los SQL no son idempotentes ni contienen un mecanismo de precondiciones que aborte
  automaticamente. Sus comentarios no sustituyen esas verificaciones manuales.
- Si aparece un error, detenerse y comprobar el esquema; no repetir a ciegas,
  desactivar FK ni agregar IF EXISTS para ocultarlo.

Referencia tecnica: [DDL y commits implicitos en MariaDB](https://mariadb.com/docs/server/reference/sql-statements/transactions/sql-statements-that-cause-an-implicit-commit).

## Resultado comprobado en la base de demostracion

| Control posterior | Resultado |
| --- | ---: |
| Tablas | 29 |
| Columnas antiguas restantes | 0 |
| Relaciones antiguas restantes | 0 |
| FK totales | 55 |
| Alquileres conservados | 2 |
| Cabeceras sin detalles | 0 |

El usuario ejecuto la migracion y las primeras consultas de comprobacion. El
asistente corroboro los valores anteriores mediante consultas de solo lectura.
Las SELECT del final del archivo se pueden ejecutar por separado; no requieren
volver a ejecutar el ALTER. El conteo de dos alquileres es una foto de esa
demostracion, no una regla para futuras cargas o ambientes.

El arranque informado por el usuario el 17/11 a las 20:00 completo JPA y Tomcat
con el perfil refactor y configuracion validate, sin error de esquema/mapeo.
No equivale a una regresion SQL integral ni a fidelidad fisica total al DER.

Para continuar, consultar el [diagnostico y guia de cierre](../../docs/practica-3/refactorizacion/44-diagnostico-y-guia-cierre-der-v5.md).
La nulabilidad de esta cabecera queda alineada; siguen separados los cambios de
nombres de hashes, coordenadas y revision restante de restricciones del DER.
Las integraciones opt-in que usan create-drop no deben ejecutarse contra esta BD.
