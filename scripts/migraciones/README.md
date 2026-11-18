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

En Java, Alquiler refuerza Bean Validation y mapeo JPA. Las ocho suites de esta
tanda del usuario tienen 144 casos, todos aprobados, sin errores u
omisiones; incluyen 17 de AlquilerCabeceraValidacionTest. No es integracion SQL.

## Migracion 2026-11-17-03: nombres de hashes de USUARIO

Estado: **ya aplicada manualmente por el usuario**. El
[SQL historico](2026-11-17-03-renombrar-hashes-usuario.sql) registra el cambio;
**no volver a importarlo sobre la BD actual**.

Se renombraron password -> password_hash y token_recuperacion ->
token_recuperacion_hash. CHANGE COLUMN conserva VARCHAR(255) obligatorio y
VARCHAR(64) opcional, utf8mb4/utf8mb4_general_ci, asi como el indice unico del
token UK_71sx3rqqapq5lx93qck2pq4t2. No se modifican los algoritmos ni se
recalculan hashes. No se cambia el vencimiento del token ni se agregan columnas.

Solo se adaptaron los dos mapeos Column de Usuario y las dos referencias a la
columna de contrasena en scripts/demo/cuentas-demo.sql; no se reimporto la demo.
Atributos Java, getters/setters, consultas derivadas, DTO de entrada, login y
recuperacion conservan sus contratos. El campo password del formulario sigue
siendo la contrasena de entrada, no el nombre fisico de la columna.

Se indico detener la app y guardar un respaldo completo previo fuera del repo
(turismo_sustentable_pre_hashes_2026-11-17.sql); el asistente no inspecciono la
exportacion. Antes se comprobaron seis usuarios y cero hashes ausentes.
El usuario compartio el resultado posterior y una lectura del asistente
corroboro solo los nombres nuevos, tipos/nulabilidad/collation conservados,
el indice unico del token y seis usuarios con cero hashes ausentes.

Doce XML de la tanda del usuario: 102 casos aprobados, cero fallos/errores/
omisiones, sobre usuarios, recuperacion, autenticacion, autorizacion y contrato
de salida. Arranque refactor/validate informado a las 21:44, JPA/Tomcat correctos.
El usuario confirmo login de cliente.demo con su contrasena habitual. Esta prueba
manual no acredita un restablecimiento real por correo ni integracion SQL nueva.

## Migracion 2026-11-17-04: coordenadas de ESTACION

Estado: **ya aplicada manualmente por el usuario**. El
[SQL de coordenadas](2026-11-17-04-coordenadas-estacion-decimal.sql) contiene
el registro historico del precontrol, ALTER y postcontrol. **No volver a
importarlo en la BD actual ni aplicarlo a una BD nueva del modelo vigente.**

Precision fisica elegida para este bloque: DECIMAL(9,6) NOT NULL en ambas
coordenadas (hasta tres enteros y seis decimales). La entidad Estacion y los DTO
de alta/edicion/salida usan BigDecimal, sin sobrecargas Double de compatibilidad.
Los rangos siguen siendo [-90,90] y [-180,180]; entidad y entradas rechazan
precision excesiva con Digits, sin redondear. El tipo decimal SQL por si solo no
impone esos rangos geograficos. JSON sigue entregando numeros, no cadenas.

Panel y mapa adaptados para conservar el cero y no inventar una ubicacion cuando
falta una coordenada. La carga demo usa DECIMAL(9,6) tambien en su tabla temporal;
no se reimporta inventario existente. No cambia el circuito de capacidad o uso.

El SELECT de solo lectura del asistente, repetido antes de migrar, obtuvo 12 estaciones,
cero coordenadas ausentes/fuera de rango y cero pares que cambian al convertir
a DECIMAL(9,6) y de vuelta a DOUBLE. Se indico app detenida y respaldo completo
actualizado fuera del repo (turismo_sustentable_pre_coordenadas_2026-11-17.sql);
el asistente no inspecciono la exportacion ni ejecuto el ALTER.
En otro entorno se exige ese control antes del DDL: si alguna coordenada cambia,
detenerse y resolverlo explicitamente; omitirlo puede causar redondeos.

Compilacion y pruebas ejecutadas por el usuario. Once XML corroborados:
331 casos Java aprobados, cero fallos/errores/omisiones, incluyendo entidad/DTO,
HTTP y regresion de estaciones, inventario, alquiler, mantenimiento e incidencias.
JavaScript aprobado segun el resultado informado por el usuario; no se suma a
tandas anteriores ni se declara una nueva integracion SQL automatizada.

El usuario compartio el postcontrol: dos DECIMAL(9,6) NOT NULL, 12 estaciones,
29 tablas, 55 FK y las mismas coordenadas con ceros finales de escala.
Luego confirmo la prueba manual de alta/edicion, rechazo de siete decimales y
mapa. Lectura posterior del asistente: QA Coordenadas, ID 13, Calle Prueba 123,
capacidad 1, publica, sin borrado, version 1; -53.787602 / -67.700201.
Auditorias 16 CREAR y 17 ACTUALIZAR de admin.demo conservan la latitud inicial
-53.787601 y su cambio a -53.787602. Quedan 13 estaciones por la nueva prueba,
29 tablas y 55 FK. Los conteos son fotos de demostracion, no reglas permanentes.

## Condiciones para otro entorno

- No aplicar estas migraciones a una BD nueva creada con las entidades actuales:
  no contiene las columnas antiguas, ya tiene el mapeo obligatorio de cabecera
  y los nombres de hashes actuales de USUARIO. La migracion 04 solo corresponde
  si las coordenadas siguen siendo DOUBLE y pasan su propio precontrol.
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
La nulabilidad de cabecera, los nombres de hashes y las coordenadas quedan
alineados en este alcance, con sus comprobaciones documentadas. Sigue la revision
restante de restricciones del DER y funcionalidades/consumidores pendientes.
Las integraciones opt-in que usan create-drop no deben ejecutarse contra esta BD.
