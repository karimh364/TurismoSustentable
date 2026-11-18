# Diagnóstico y guía de cierre de la migración al DER v5

Fecha de referencia: 17 de noviembre de 2026.
Última actualización: 18/11, después del cierre manual de sesión/inactivación y la pausa de integraciones SQL. El orden vigente de pendientes está en la sección 15.
Presentación prevista: miércoles 18 de noviembre, 19:30 (Argentina).

## 1. Finalidad y alcance

Consolidar el circuito funcional ya implementado, retirar la convivencia con el
alquiler anterior y conectar las funcionalidades aprobadas que todavía no llegan
a las pantallas. No iniciar otra refactorización general ni añadir entidades o
reglas de negocio sin aprobación.

Esta guía registra la revisión inicial de solo lectura del 17/11, los cambios
posteriores comprobados y el orden de trabajo restante. Sus tareas pendientes
NO están implementadas por aparecer aquí. Los
documentos anteriores conservan antecedentes cronológicos; esta guía no convierte
una prueba histórica en una verificación actual ni declara el proyecto terminado.

Referencias: [DER v5](der-v5-propuesto.mmd), [diccionario](08-diccionario-estados-propuesto.md),
[roles](25-revision-personas-usuarios-y-roles.md), [trazabilidad](35-auditoria-admin-y-brechas-de-trazabilidad.md),
[balance anterior](40-revision-coherencia-migracion-der-v5.md),
[alquiler y finanzas](41-circuito-alquiler-y-finanzas-simuladas.md),
[mantenimiento](42-mantenimiento-operativo-punta-a-punta.md) e
[incidencias](43-incidencias-durante-el-alquiler.md).

## 2. Estado funcional comprobado en el código

| Módulo | Implementado y conectado | Cierre pendiente |
| --- | --- | --- |
| Identidad/acceso | PERSONA, documentos y catálogos, registro CLIENTE, múltiples roles, login, recuperación, sesión y edición administrativa; filtro de cuenta/roles vigentes, rechazo sin logout e invalidación por cuenta inactiva comprobados; gestión de roles conectada en código (sección 16) | Compilar/probar la gestión administrativa de roles y su diálogo; regresión transversal restante, sin declarar seguridad de producción |
| Estaciones | Alta/edición/borrado lógico, pública/interna, capacidad, concurrencia, panel y mapa; coordenadas BigDecimal/DECIMAL(9,6) verificadas; acceso a alquiler desde mapa exige perfil CLIENTE confirmado por servidor | Escapado de textos y estados visibles de error de carga del mapa; revisión transversal restante |
| Bicicletas | Tipo/tarifa, ubicación, traslados con motivo, estados, baja/eliminación, consulta de eliminadas e historia; dependencia del alquiler anterior retirada | Regresión integral y controles transversales pendientes |
| Alquiler | Cabecera/detalles, reserva múltiple, retiro/devolución por unidad, cancelación y vencimiento, pantallas; retirada la compatibilidad anterior y sus ocho columnas/dos FK; cabecera obligatoria alineada en Java/BD | Revisión física restante, comprobantes v5 y cierre transversal; no declarar migración global terminada |
| Finanzas | Anticipo, adicional, liquidación, deuda y reembolso simulados, idempotencia y pantallas | Regresión integral tras retirar legado; no confundir simulación con Mercado Pago real |
| Incidencias | Reportar/revisar/cortar, recibir/recuperar, pérdida definitiva, conexión con mantenimiento y liquidación; consulta anterior retirada | Pulir selección de informante/contexto de cuentas multirrol |
| Mantenimiento | ADMIN gestiona, TECNICO asignado trabaja, historia, preventiva y habilitación posterior; UsuarioService coordina la inactivación y mantenimiento prepara únicamente órdenes, con auditoría y transacción compartida | Regresión SQL posterior pausada; reparación exclusivamente en taller sigue siendo propuesta diferida |
| Auditoría | Escritura administrativa e historiales de dominio implementados | Consulta administrativa general con filtros y pantalla; no exponerla a CLIENTE/TECNICO |
| Estadísticas | Consultas migradas a detalles y tipo contratado, endpoint de resumen | Pantalla consumidora: no encontrada en la revisión |
| Correo | Bienvenida y recuperación conservadas; métodos exclusivos del alquiler anterior retirados | Implementar/conectar comprobantes v5 con varias unidades e importes guardados; no se consideran reemplazados por haber retirado los anteriores |

La finalidad no es crear un CRUD de todas las tablas: historiales, pagos y estados
deben seguir las operaciones y permisos del dominio, sin edición arbitraria.

## 3. Comparación física con MariaDB — diagnóstico inicial

Foto de information_schema de turismo_sustentable del 17/11, ANTES de la
migración. No es el estado posterior; consultar los cierres de las secciones 7 y 8:

- 29 tablas, frente a 29 entidades del DER.
- 55 relaciones previstas representadas, pero 57 FK físicas.
- Las dos FK adicionales son ALQUILER.bicicleta_id y
  ALQUILER.estacion_devolucion_id, ambas del modelo anterior.
- 247 columnas físicas frente a 239 campos del Mermaid.
- Ocho columnas antiguas de ALQUILER: bicicleta_id, estacion_devolucion_id,
  estado, fecha_inicio, fecha_fin, costo_total, costo_total_ars y
  precio_por_hora_aplicado.
- USUARIO usa password y token_recuperacion donde el DER dice password_hash y
  token_recuperacion_hash. Se almacenan hashes: la diferencia de nombre NO
  significa que la contraseña esté en texto plano.
- ESTACION.latitud/longitud son double; el DER indica decimal. Concretar precisión
  y escala al alinear el diseño físico, sin redondear coordenadas inadvertidamente.
- Campos esenciales de la cabecera nueva aún permiten NULL por compatibilidad:
  estado, fechas iniciales, cotización/fuente e importes estimados. No extender la
  obligatoriedad a cierre, importes finales o fecha de actualización de proveedor
  cuando legítimamente deben permanecer pendientes/opcionales.
- Comprobadas PK compuesta de USUARIO_ROL, unicidad documental compuesta de
  PERSONA y unicidad alquiler/bicicleta e incidencia de corte en ALQUILER_DETALLE.

Comprobaciones de coherencia de los datos, sin escrituras:

| Control | Resultado |
| --- | --- |
| Alquileres nuevos / anteriores | 2 / 0 |
| Cabeceras nuevas sin detalles | 0 |
| Cabeceras nuevas sin datos esenciales de fechas/cotización/importes | 0 |
| Bicicletas con más de un detalle activo | 0 |
| Estaciones con presencia física superior a su capacidad | 0 |
| Bicicletas eliminadas con ubicación o sin DE_BAJA | 0 |
| Usuarios sin roles | 0 |

Son resultados de ese momento, no restricciones permanentes ni una auditoría de
todas las combinaciones posibles. Tener tablas pobladas o arrancar con validate
no acredita por sí solo fidelidad completa al DER ni corrección del circuito.

## 4. Pendientes concretos y orden de trabajo

### A. Retiro coordinado del alquiler anterior — prioridad principal

Estado actualizado: cumplida la retirada de controles/listados/devolución antigua,
DTO/enum, campos JPA y asociaciones/consultas dependientes. Ejecutada y comprobada
la eliminación física de las ocho columnas y dos FK. El paso 5 (comprobantes v5)
sigue pendiente, así como la regresión SQL aislada y otras diferencias físicas.
La lista siguiente conserva el plan y sus criterios, no una lista íntegra de
operaciones todavía sin hacer.

1. Respaldar BD y código; comprobar nuevamente que no aparecieron alquileres
   anteriores desde el diagnóstico. No borrarlos si aparecen: resolver su
   migración/conservación antes de eliminar estructura.
2. Quitar el control antiguo de asignación en inventario, mantenimiento e
   incidencias; mantener ALQUILER_DETALLE como fuente de reservas/viajes activos.
   Retirar inyecciones, mocks y consulta que queden sin uso; actualizar pruebas.
3. Unificar las consultas y contratos de salida en cabecera/detalles; retirar la
   mezcla ANTERIOR/DETALLES y sus pruebas de compatibilidad sustituyéndolas por
   pruebas de las reglas vigentes, no reduciendo cobertura de permisos/historia.
4. Retirar devolución antigua de HTML/JavaScript, ruta HTTP y autorización,
   método del servicio, DTO/enum antiguos y fixtures exclusivos del modelo
   retirado. Conservar devolución por detalle y sus controles.
5. Adaptar los comprobantes de correo a varias unidades, hechos e importes
   guardados del circuito nuevo. No recalcular tarifas ni fabricar fechas/pagos.
6. Retirar campos y asociaciones JPA antiguos, incluidos consumidores inversos;
   revisar constructores, getters/setters, imports y consultas.
7. Preparar una migración SQL explícita y comprobada, con respaldo, que alinee el
   esquema sin recrear la BD. Separar la ejecución sobre datos demo de las pruebas
   destructivas. Con ddl-auto=validate, modificar Java NO elimina columnas.

Aceptación: una única forma vigente de alquilar/consultar/devolver en backend y
frontend; sin llamadas ni código de compatibilidad ejecutable; datos e historias
conservados; SQL e integración aislada demuestran el esquema resultante.

### B. Seguridad transversal y gestión de roles

- Control central de cuenta activa/roles vigentes implementado para API privada
  y páginas protegidas. Revocación de rol e inactivación con sesión abierta
  comprobadas manualmente el 18/11; conservar la regresión de las rutas y las
  condiciones de dominio particulares (propiedad, asignación y versiones).
- Gestión ADMIN de roles con actor real, auditoría y conservación de al menos un
  rol; registro público continúa asignando exclusivamente CLIENTE.
- ADMIN no hereda CLIENTE ni TECNICO; cuentas multirrol conservan los permisos y
  condiciones de cada operación. No debilitar propiedad, asignación o versiones.
- Revisar CSRF y cabeceras: WebAuthorization deshabilita actualmente CSRF y
  frameOptions. No declarar seguridad de producción ni corregir a ciegas sin
  adaptar consumidores HTTP y pruebas.
- Homogeneizar tratamiento seguro de textos en el DOM; existen interpolaciones
  directas en listados administrativos y popups del mapa. La revisión estática no
  equivale a una explotación demostrada.
- Revisar carga EAGER y dependencia de Open Session in View; no deshabilitarlo
  globalmente antes de asegurar las consultas que usan las pantallas.

### C. Consumidores funcionales y presentación

- Conectar resumen estadístico existente a una vista ADMIN útil, sin inventar
  métricas financieras ni contar reservas como viajes realizados.
- Añadir consulta ADMIN de auditoría por entidad/id, fecha y responsable, sin
  credenciales o datos internos fuera de su alcance autorizado.
- Acceso de alquiler desde mapa alineado con perfil/roles del servidor y
  comprobado; quedan mensajes visibles ante fallos de carga y escapado de textos.
- Verificar conservación de detalles al refrescar/cambiar moneda, anchos estables,
  scroll de diálogos, cargas/vacíos/errores, etiquetas y navegación responsive.
- Resolver contexto de cuentas multirrol y evitar IDs manuales cuando procede una
  selección legible. No convertir mejoras visuales en cambios de permisos.

### D. Cierre y preparación de entrega

- Regresión Java/HTTP, siete suites JS e integración MariaDB en base aislada.
- Recorridos con CLIENTE, ADMIN y TECNICO separados; agregar multirrol como caso
  adicional, no sustituto del cliente puro.
- Recorrer alquiler normal, devolución parcial, cancelación/vencimiento, deuda y
  reembolso, incidencia, recepción, reparación y habilitación; comprobar también
  rechazos por permisos, capacidad y versiones.
- Consolidar documentación de estado final, comando de arranque, datos demo,
  contraseñas ficticias, límites de simulación y recuperación ante fallos.
- Ensayo del recorrido de exposición y verificación de dependencias externas
  (cotización, mapas/CDN y correo). Nunca inventar un respaldo fijo de cotización.
- Congelar cambios funcionales con margen el miércoles antes de las 19:30.

## 5. Evidencia y límites de esta revisión

El 17/11 se ejecutaron las siete suites src/test/js: 117 pruebas, 117 aprobadas,
cero fallos y omisiones. Son pruebas JavaScript, no certificación visual global.
Se inspeccionaron código, documentación y estructura/datos mediante consultas de
lectura. No se reinició la app, no se alteraron filas ni se ejecutó integración
destructiva. Los XML más recientes pertenecen a los ejercicios Playwright de la
otra materia; no se contaron como regresión completa del proyecto.

Las tandas Java/HTTP/SQL y manuales anteriores conservan su evidencia en los
documentos 37 y 41–43. No sumar resultados de distintas tandas como si fueran una
única ejecución actual. Repetir la regresión correspondiente después de cambios.

## 6. Decisiones y protecciones que se mantienen

- Implementaciones manuales por el usuario salvo delegación expresa de un bloque.
- Esta actualización documental no autoriza implementar Java, UI o ejecutar DDL.
- Conservar cambios de Gradle, tests com.tests, capturas, trazas y presentación de
  la otra materia; no mezclarlos en la limpieza de negocio.
- Perfil refactor actualmente con ddl-auto=validate: conservar datos demo.
- Integraciones opt-in TURISMO_TEST_MYSQL pausadas explícitamente con @Disabled
  y configuradas con validate desde el 18/11; ver sección 14. No reactivarlas
  mientras se deban conservar los datos demo: validate no impide sus escrituras.
- Pagos/reembolsos simulados y dinero ficticio aprobados. Mercado Pago real y
  despliegue productivo no se declaran terminados ni se añaden a esta entrega.
- Exigir taller para iniciar reparación sigue siendo mejora expresamente diferida;
  no declararla incumplimiento de una regla vigente ni implementarla sin aprobación.
- Ocupación física depende de ubicación registrada, no solo del estado de la bici.
- No crear nuevas tablas, billeteras, infraestructura de correos o rediseño general
  como sustituto de cerrar los consumidores y relaciones existentes.

## 7. Cierre verificado del alquiler anterior — 17/11

### Código y consumidores

- Inventario, mantenimiento e incidencias comprueban asignaciones activas mediante
  ALQUILER_DETALLE; retirada existsByBicicletaAndEstado y las inyecciones sin uso.
- Los listados de CLIENTE/ADMIN devuelven AlquilerCabeceraDTO con detalles, sin
  rama ANTERIOR. Las pantallas usan las fechas e importes guardados del v5.
- Retirados modal/funciones web anteriores, PATCH /api/alquileres/finalizar/{id},
  método finalizarAlquiler y su permiso. La devolución POST por detalle permanece.
- Eliminados AlquilerDTO y las cinco pruebas exclusivas de la devolución antigua;
  conservadas las pruebas del circuito vigente y agregadas pruebas de retirada
  de ruta y rechazo para visitante/roles.
- El borrado lógico solo permite FINALIZADO/CANCELADO/VENCIDO, rechaza estados
  inválidos y conserva importes, detalles y movimientos; no ejecuta liquidaciones.
- Retirados ocho campos, sus accesores, dos constructores y CodigoEstadoAlquiler;
  eliminada Bicicleta.alquileres, que apuntaba al FK anterior. Se conserva
  Usuario.alquileres, cuyo mappedBy usuario sigue siendo válido.
- Retiradas consultas por nombre/listados anteriores sin consumidores y adaptadas
  las pruebas de cotización, importes y reserva.
- Retiradas plantillas/métodos de comprobante anterior; bienvenida y recuperación
  permanecen. Los nuevos comprobantes de alquiler NO están implementados por ello.

### Tandas dirigidas comprobadas

| Subbloque | Evidencia |
| --- | --- |
| Desacoplar inventario/mantenimiento/incidencias | 245 Java en siete XML, sin fallos/errores/omisiones |
| Unificar contrato de listados | 137 Java en siete XML, sin fallos/errores/omisiones |
| Limpieza del frontend | 117 JS aprobadas en las siete suites; resultado adjunto del usuario revisado |
| Retirar devolución antigua del backend | 234 Java en diez XML, sin fallos/errores/omisiones |
| Borrado lógico exclusivamente v5 | 232 Java en nueve XML, sin fallos/errores/omisiones |
| Retirar campos/relaciones de entidades | clean compileTestJava informado correcto y 199 Java en diez XML corroborados, sin fallos/errores/omisiones |

Son ejecuciones distintas con casos solapados: NO se suman ni acreditan una nueva
integración SQL, una prueba visual global o regresión completa posterior.

### Migración física ejecutada por el usuario

Respaldo de estructura y datos confirmado antes del DDL, con la app detenida.
La comprobación previa obtuvo dos alquileres, cero sin estado v5 y cero con datos
en las columnas antiguas. Las dos FK se identificaron por information_schema.

El usuario ejecutó un ALTER sobre turismo_sustentable.alquiler que retiró ambas
FK y las ocho columnas. No se recreó la BD ni se desactivaron comprobaciones de FK.
El [SQL histórico](../../../scripts/migraciones/2026-11-17-01-retirar-alquiler-anterior.sql)
y sus [precauciones](../../../scripts/migraciones/README.md) quedan versionables.
Ya está aplicado: NO importarlo otra vez ni aplicarlo a una BD nueva del modelo v5.

Lectura posterior del asistente, sin modificaciones:

| Comprobación | Resultado |
| --- | ---: |
| Tablas conservadas | 29 |
| FK totales | 55 |
| Columnas antiguas restantes | 0 |
| Relaciones antiguas restantes | 0 |
| Alquileres conservados | 2 |
| Cabeceras sin detalles | 0 |

Ambos alquileres permanecen FINALIZADO con un detalle cada uno. La lectura
conserva cotización 2047,50 ARS/USD y estimados de 16380,00 ARS por alquiler;
finales de 1547,00 ARS (#1) y 27,30 ARS (#2). Son datos de demostración, no tarifas
generales ni nuevos valores impuestos por una migración.

El arranque informado del 17/11 a las 20:00 completó JPA y Tomcat con refactor y
validate, sin error de esquema o mapeo. El aviso open-in-view es un pendiente
transversal; no indica fallo de arranque. El asistente no reinició la app ni
ejecutó este DDL: corroboró la BD mediante SELECT.

## 8. Obligatoriedad de cabecera comprobada — 17/11, 21:23

Alquiler incorpora NotNull en sus relaciones y datos obligatorios, NotBlank en
fuenteCotizacion y nullable=false/optional=false en los mapeos correspondientes.
Se conservan las reglas previas de signo, escala y tamaño, los estimados cero
válidos y los datos legítimamente pendientes. La versión autogestionada no lleva
NotNull de Bean Validation. crearReserva completa la cabecera antes de validarla
y guardarla; no se añadió otro servicio, endpoint o pantalla.

AlquilerCabeceraValidacionTest aporta 17 casos: diez campos obligatorios, seis
opcionales y una cabecera completa sin inventar cierre/liquidación/versión.
El usuario ejecutó la compilación y pruebas. Ocho XML corroborados, 144 casos
aprobados, cero fallos/errores/omisiones: esa suite y AlquilerCotizacionTest,
AlquilerImportesTest, BorradoAlquilerServiceTest, ConsultaAlquilerServiceTest,
CrearReservaServiceTest, DevolucionAlquilerServiceTest y RetiroAlquilerServiceTest.
No se suman a las tandas anteriores ni acreditan integración SQL nueva.

Migración 02 ejecutada manualmente por el usuario, con app detenida, respaldo
completo confirmado (turismo_sustentable_pre_not_null_2026-11-17.sql) y cero
cabeceras incompletas. Se preservan tipos, precisión, escala y charset/collation:

| Columnas de ALQUILER | Resultado físico |
| --- | --- |
| estado_alquiler_id | int(11) NOT NULL |
| cotizacion_ars_por_usd | decimal(20,8) NOT NULL |
| fuente_cotizacion | varchar(100), utf8mb4/utf8mb4_general_ci, NOT NULL |
| fecha_hora_aplicacion_cotizacion, fecha_hora_creacion, fecha_hora_vencimiento_reserva | datetime(6) NOT NULL |
| costo_estimado_usd, costo_estimado_ars | decimal(21,2) NOT NULL |
| fecha_hora_actualizacion_fuente, fecha_hora_cierre, fecha_hora_cancelacion, fecha_hora_liquidacion, costo_final_usd, costo_final_ars | NULL permitido, sin alterar tipos |

Usuario y estación de retiro ya eran NOT NULL en la BD. No hubo rellenos de
datos, defaults inventados o modificación de importes. NOT NULL no garantiza
texto no vacío: NotBlank sigue cubriendo esa condición en Java.

El primer ALTER propuesto falló con 1005/121: recrear la FK usando el nombre
original produjo un conflicto durante la reconstrucción. InnoDB identificó
el nombre duplicado; se comprobó que esquema anterior y datos permanecían.
La sentencia corregida usa fk_alquiler_estado_v5, previamente libre, conservando
estado_alquiler_id -> estado_alquiler.id y ambas reglas RESTRICT. Retirada y
creación de la FK se hicieron en el mismo ALTER, sin desactivar controles.
El [SQL definitivo histórico](../../../scripts/migraciones/2026-11-17-02-obligatoriedad-cabecera-alquiler.sql)
registra únicamente la variante aplicada con éxito. NO volver a importarlo.

Usuario y posterior lectura del asistente corroboraron las ocho columnas NO y
tres FK de ALQUILER; lectura del asistente confirmó también las seis opcionales
YES, 29 tablas, 55 FK, dos alquileres y cero cabeceras incompletas.
Arranque informado a las 21:23: refactor, JPA y Tomcat correctos, aplicación
iniciada en 4,941 segundos con validate. El asistente no ejecutó el ALTER,
reinició la app ni repitió pruebas: inspeccionó SQL y XML en modo lectura.
El arranque no sustituye las consultas de nulabilidad ni una regresión integral.

## 9. Nombres de hashes alineados y acceso comprobado — 17/11, 21:44

Migración 03 ejecutada manualmente por el usuario: USUARIO.password pasa a
password_hash y token_recuperacion a token_recuperacion_hash. No se recalculan
hashes ni se cambian contraseñas: se conservan BCrypt y SHA-256 del flujo actual.
Tipos y nulabilidad: varchar(255) NOT NULL y varchar(64) opcional, ambos
utf8mb4/utf8mb4_general_ci. El índice único del token conserva nombre
UK_71sx3rqqapq5lx93qck2pq4t2 y ahora apunta a token_recuperacion_hash.

Cambios de código limitados a dos mapeos Column de Usuario y las dos referencias
SQL de scripts/demo/cuentas-demo.sql. No se renombraron atributos, accesores,
parámetros HTTP o consultas derivadas; no se reimportaron las cuentas demo.
El [SQL histórico](../../../scripts/migraciones/2026-11-17-03-renombrar-hashes-usuario.sql)
está ya aplicado: no importarlo nuevamente ni usarlo como inicializador.
Se indicó app detenida y respaldo completo previo fuera del repositorio; el
asistente no inspeccionó el archivo de exportación ni ejecutó el DDL.

Usuario y posterior lectura del asistente corroboraron los nombres nuevos,
tipos/nulabilidad, índice único y seis usuarios con cero hashes ausentes.
Doce XML: 102 pruebas aprobadas, cero fallos/errores/omisiones, en
WebAuthenticationTest, WebAuthorizationTest, RecuperacionPasswordControllerTest,
UsuarioControllerTest, ContratoSalidaDTOTest, RegistroUsuarioDTOTest,
SolicitarRecuperacionDTOTest, UsuarioDTOTest, RecuperacionPasswordServiceTest,
RegistroUsuarioServiceTest, UsuarioDetailsServiceTest y UsuarioServiceTest.
No sumar esta tanda a ejecuciones anteriores solapadas.

Arranque informado a las 21:44 con refactor/validate, JPA y Tomcat correctos,
aplicación iniciada en 4,075 segundos. El usuario confirmó acceso de cliente.demo
con su contraseña habitual. No se afirma restablecimiento manual real por correo
ni regresión SQL integral; el asistente no volvió a ejecutar pruebas o la app.

## 10. Coordenadas decimales — migración y pruebas verificadas

Bloque delegado explícitamente por el usuario. DECIMAL(9,6) NOT NULL concreta la
precisión física que el DER dejaba abierta: hasta tres enteros y seis decimales.
No es una nueva regla sobre ubicación de estaciones. Migración 04 aplicada
manualmente por el usuario, sin ejecución de DDL desde el asistente.

- Estacion usa BigDecimal en atributos, constructor y accesores, Column(9,6),
  NotNull, rangos geográficos y Digits. Se retira Double sin sobrecarga transitoria.
- CrearEstacionDTO/ActualizarEstacionDTO usan BigDecimal con los mismos rangos y
  máximo de seis decimales. No hay setScale ni redondeo de entradas inválidas.
- EstacionDTO conserva BigDecimal; los números JSON siguen siendo números y
  los servicios/auditoría transportan el valor sin conversiones nuevas.
- Formulario ADMIN informa precisión/rangos y rechaza entradas inválidas antes
  de enviar, sin usar parseFloat para aceptar prefijos parciales. El backend
  mantiene su validación independiente y los errores se muestran con el
  manejador existente. El listado conserva el cero mediante operador nulo.
- Mapa usa coordenadas numéricas finitas en rango, incluido cero; si faltan o son
  inválidas no fabrica un punto de Río Grande. No cambia la política de sesión,
  permisos, ventanas emergentes o sitios turísticos estáticos de ese consumidor.
- SQL demo de inventario usa decimal también en su tabla temporal; no se importa.

Fixtures de veinte archivos de pruebas adaptadas al constructor/accesores
BigDecimal, sin eliminar casos o cambiar reglas de alquiler/mantenimiento/
incidencias. Se agregaron EstacionCoordenadasTest y tres casos HTTP adicionales
de precisión/contrato/error, y CoordenadasEstaciones.test.cjs para alta/edición,
rechazos, mensajes, mapa y cero. El usuario ejecutó compilación y pruebas.
Once XML corroborados: 331 casos aprobados, cero fallos/errores/omisiones:
WebAuthorizationTest (21), EstacionControllerTest (29), EstacionCoordenadasTest (21),
AuditoriaAdministrativaServiceTest (15), BicicletaServiceTest (35),
CrearReservaServiceTest (22), DevolucionAlquilerServiceTest (33),
EstacionServiceTest (17), IncidenciaServiceTest (54), MantenimientoServiceTest (58)
y RetiroAlquilerServiceTest (26). JavaScript aprobado según confirmación del
usuario; no se suma a tandas anteriores ni se declara integración SQL nueva.

Precontrol SQL de solo lectura repetido antes del cambio: doce estaciones, cero nulas/fuera de rango y
cero pares con cambio al convertir a DECIMAL(9,6) y retornar a DOUBLE.
El [SQL 04 histórico](../../../scripts/migraciones/2026-11-17-04-coordenadas-estacion-decimal.sql)
ya está aplicado: NO volver a importarlo ni usarlo como inicializador. Se indicó
app detenida y respaldo actualizado fuera del repositorio; el asistente no
inspeccionó la exportación. En otro entorno se exige verificar tipo y precontrol
antes de migrar; los comentarios no son guardas automáticas.

Postcontrol compartido por el usuario: dos DECIMAL(9,6) NOT NULL, doce estaciones,
29 tablas, 55 FK y las mismas ubicaciones con la nueva escala. Luego el usuario
confirmó alta/edición, rechazo de siete decimales y mapa funcionando.
Lectura posterior del asistente verificó:

| Dato de demostración | Resultado |
| --- | --- |
| Estación | ID 13, QA Coordenadas, Calle Prueba 123 |
| Capacidad / uso / borrado | 1 / pública / sin borrado lógico |
| Latitud / longitud actuales | -53.787602 / -67.700201 |
| Versión | 1 |
| Auditoría 16, CREAR, admin.demo | Latitud inicial -53.787601, longitud -67.700201 |
| Auditoría 17, ACTUALIZAR, admin.demo | Latitud -53.787601 -> -53.787602; longitud conservada |
| Conteos después de la prueba | 13 estaciones, 29 tablas, 55 FK |

Las trece estaciones incluyen el alta QA: no son una regla del dominio. El mapa
y el rechazo de siete decimales fueron comprobados manualmente por el usuario;
el asistente corroboró BD/auditoría y leyó XML, no operó el navegador ni repitió
pruebas. El log de las 21:44 fue ANTERIOR a coordenadas y no se usa como evidencia
de su arranque; el funcionamiento posterior consta por la prueba manual informada.

## 11. Continuación y límites del cierre

La retirada del alquiler anterior está comprobada en código y esquema para este
alcance. 29 tablas y 55 FK NO implican fidelidad física total ni que todas las
funcionalidades aprobadas estén conectadas.

Obligatoriedad de cabecera concretada entre Bean Validation/JPA y BD; cierre,
importes finales y fecha de actualización de fuente conservan su opcionalidad.
Los nombres de hashes de USUARIO y las coordenadas ya coinciden con el DER en
este alcance, con código, migraciones y pruebas comprobadas. Guardar el avance
de hashes/coordenadas con commit/push; después revisar las restricciones físicas
restantes y los controles/consumidores pendientes sin mezclar otros cambios DDL.
Cada cambio requiere comprobaciones previas, respaldo y pruebas acordes.

Continúan comprobantes v5 por correo, revisión transversal, administración de
roles, consulta de auditoría, pantalla estadística y pulido. El control central
de cuenta/roles vigente se implementó después de esta revisión inicial; ver
secciones 12/13. Integraciones SQL pausadas por decisión posterior del usuario
(sección 14), sin crear otra BD ni recrear los datos de demostración.

## 12. Ajuste de acceso a Mis alquileres — 18/11

Aprobado por el usuario: `/mis_alquileres.html` requiere CLIENTE. ADMIN sin
CLIENTE conserva la gestión desde `/admin.html`, pero no entra a la vista
personal; ADMIN + CLIENTE conserva ambos accesos. TECNICO sin CLIENTE tampoco
entra. Un rechazo por rol no cierra una sesión válida.

Aplicado en WebAuthorization, en la carga inicial de mis_alquileres.js y en la
comprobación de auth.js al restaurar una página con Atrás. No se modifican las
consultas administrativas ni los permisos de los endpoints de consulta existentes,
las operaciones de alquiler, las entidades ni el esquema. El listado personal
continúa filtrado por el usuario conectado; permitir consulta no autoriza alquilar.

Se agregaron cuatro pruebas HTTP con el filtro/servicio de vigencia reales y
repositorio simulado: rechazo sin logout, acceso CLIENTE/multirrol, retirada de
CLIENTE conservando ADMIN y acceso anónimo. También ocho casos JavaScript de
carga inicial/restauración con los cuatro perfiles. Ejecución de estas pruebas y
verificación manual del ajuste pendientes del usuario; no se reutilizan como
evidencia los resultados aprobados antes de este cambio. No requiere migración SQL.

## 13. Separación de la inactivación de usuarios — 18/11

Bloque autorizado por el usuario al preparar la prueba de sesión de una cuenta
inactiva. La regla que protege las cuentas ADMIN se conserva, pero pertenece a
UsuarioService, no a mantenimiento. No se habilita inactivar administradores.

- UsuarioService coordina la inactivación con READ_COMMITTED y rollback ante
  excepciones. Comprueba ADMIN en sesión y en la cuenta bloqueada vigente; adquiere
  los bloqueos de actor/objetivo en orden de ID, antes de bicicletas/órdenes.
  Valida existencia, estado y protección de la cuenta; normaliza el motivo,
  prepara órdenes, cambia activo, hace flush y solicita la auditoría de usuario.
- MantenimientoService reemplaza inactivarUsuario por
  prepararOrdenesPorInactivacion, con propagación MANDATORY. Solo pausa las órdenes
  EN_PROCESO y desasigna las abiertas, conservando trabajo, costos, fechas y
  cerradas. Se consultan asignaciones incluso si se retiró TECNICO previamente.
  Sus historiales y auditorías de órdenes siguen en mantenimiento; no altera
  activo ni registra INACTIVAR de USUARIO.
- AuditoriaAdministrativaService registra INACTIVAR de USUARIO utilizando la
  trazabilidad existente: actor, motivo y snapshots históricos activo=true/false.
  UsuarioService comparte una única fecha UTC del Clock existente con órdenes y
  auditoría; no se agregan tablas, catálogos, endpoints ni inicializadores.
- Cuenta objetivo ADMIN devuelve 409 con «No se puede inactivar a un usuario
  Administrador.». Falta de permisos sigue siendo 403 y sesión ausente 401 en el
  filtro HTTP. El controlador y admin.js existentes ya transmiten/muestran el
  mensaje 409; no se modifica auth.js ni se interpreta como logout.

Pruebas adaptadas: UsuarioServiceTest, MantenimientoServiceTest,
AuditoriaAdministrativaServiceTest, UsuarioControllerTest, WebAuthorizationTest
y EdicionUsuarios.test.cjs. Se agregan dos escenarios SQL en
MantenimientoMysqlIntegrationTest; se conservan sus escenarios de rollback tras
escrituras y concurrencia entre inicio técnico e inactivación.

Estado al implementar: revisión estática de llamadas y diferencias; compilación,
ejecución de pruebas y prueba manual pendientes del usuario. No se ejecutaron
tests ni escrituras SQL. La integración citada conserva create-drop y URL
turismo_sustentable: NO habilitar TURISMO_TEST_MYSQL sobre los datos de demostración;
aislar primero esa prueba en un trabajo separado.

Preparación indicada al implementar: qa.sesion quedó con ADMIN después de la retirada de
CLIENTE. Devolverle CLIENTE y retirar únicamente ADMIN antes de intentar
inactivarla. Mantener su sesión abierta en un contexto separado del de admin.demo;
este último inactiva qa.sesion desde Usuarios. En la siguiente petición protegida
de la cuenta temporal se espera sesión invalidada y redirección al ingreso;
admin.demo debe seguir operativo. No modificar roles ni estado de las cuentas
de demostración. En ese momento todavía no se había ejecutado esta preparación.

Actualización de evidencia manual, informada por el usuario el 18/11: el intento
de inactivar qa.sesion con ADMIN mostró el mensaje específico de cuenta protegida
y mantuvo ambas cuentas activas. Tras preparar qa.sesion como CLIENTE sin ADMIN,
admin.demo pudo inactivarla. La recarga de Mis alquileres en su sesión existente
redirigió al ingreso con «Necesitás iniciar sesión para acceder a esa página»;
el nuevo login fue rechazado con el aviso genérico de credenciales incorrectas,
y admin.demo continuó navegando. No se atribuyen a esta prueba los escenarios de
integración/rollback/concurrencia todavía pendientes de ejecutar.

Se reemplazó el aviso de éxito fijo sobre órdenes de técnicos por un mensaje
neutral de cuenta inactivada y conservación de datos/historiales. Se agregó su
caso en EdicionUsuarios.test.cjs; ejecución de esta nueva prueba pendiente del usuario.

## 14. Pausa de integraciones SQL para conservar datos — 18/11

Por decisión del usuario no se crea otra BD en este bloque. Se modifican únicamente
AlquilerMysqlIntegrationTest, MantenimientoMysqlIntegrationTest e
IncidenciaMysqlIntegrationTest: ddl-auto pasa de create-drop a validate y se agrega
@Disabled con motivo explícito a nivel de clase. Se conservan sus casos, fixtures,
URL local e interruptores TURISMO_TEST_MYSQL/TURISMO_TEST_WEB existentes.

Con @Disabled estas clases quedan omitidas incluso si TURISMO_TEST_MYSQL=true.
No deben contarse como pruebas SQL aprobadas por obtener BUILD SUCCESSFUL: sus
casos siguen pendientes de ejecución. Continúan disponibles las pruebas unitarias,
HTTP con mocks y JavaScript. La aplicación refactor conserva validate sin cambios.

La pausa protege los datos actuales: quitar create-drop evita la recreación
automática de tablas, pero validate no evita las inserciones/modificaciones que
realizan los fixtures y operaciones de estas pruebas con transacciones reales.
No se ejecutaron tests, comandos SQL ni cambios de estructura/datos en este bloque.
Las referencias a create-drop en las secciones históricas describen el estado
anterior, no la configuración vigente de estas tres clases.

Para reactivarlas más adelante: obtener autorización expresa para usar datos
descartables y respaldar lo que deba conservarse, revisar la URL y preparar un
esquema/catálogos compatibles con validate. Revisar también los fixtures HTTP con
el control de sesión vigente y las suposiciones de esquema/datos limpios. Solo
entonces retirar @Disabled de las clases que se vayan a ejecutar y habilitar
TURISMO_TEST_MYSQL para la tanda explícita; TURISMO_TEST_WEB sigue siendo opt-in
adicional. No restaurar create-drop ni borrar datos como paso implícito. La
compilación de estas clases y su ejecución se distinguen de su aprobación efectiva.

El usuario confirmó compileTestJava correcto después de esta pausa. Esa
compilación no acredita ejecución ni aprobación de los casos SQL omitidos.

## 15. Orden vigente de cierre: backend antes de pulido visual — 18/11

Objetivo reiterado por el usuario: cerrar el circuito existente de alquiler,
finanzas simuladas, incidencias y mantenimiento; completar backend y relaciones
del DER v5 antes de priorizar estética. No significa CRUD arbitrario de estados,
historiales o pagos. Las entidades existentes siguen siendo la base del trabajo.

Estado cerrado en este alcance: identidad estable de sesión, filtro de cuenta y
roles vigentes, separación ADMIN/CLIENTE en Mis alquileres/mapa, inactivación de
usuario coordinada desde UsuarioService y mensaje neutral de éxito. El usuario
confirmó además la nueva inactivación de qa.sesion y el mensaje actualizado.
No se vuelve a planificar lo anterior como funcionalidad faltante.

| Orden | Bloque pendiente | Alcance / criterio de cierre |
| --- | --- | --- |
| B1 — comprobado en el alcance informado | Gestión administrativa de roles | GET/PUT de roles, protecciones, versión, auditoría y diálogo conectados; usuario confirmó compilación/tandas Java-JS y escenarios manuales de asignación, concurrencia de formulario, retiro/desbloqueo por reserva, orden y deuda, y ADMIN propio. Límites SQL y caso de último ADMIN descritos en sección 16. Sin tablas nuevas. |
| B2 | Consulta administrativa de auditoría | DTO y consulta con filtros/paginación sobre AUDITORIA_SISTEMA, autorización ADMIN y prueba de exclusión CLIENTE/TECNICO. No permitir editar/borrar eventos ni exponer credenciales. Escritura ya existente no equivale a consulta implementada. |
| B3 | Comprobantes del circuito v5 | Reutilizar CorreoService/Mailjet, varias unidades e importes/fechas guardados; disparar para hechos confirmados sin inventar pagos ni enviar por una transacción revertida. Fallos de correo no revierten una operación ya confirmada. Actualmente solo están bienvenida y recuperación. |
| B4 | Revisión final del backend/DER y protecciones HTTP | Contrastar campos/relaciones/restricciones restantes y consumidores, buscar referencias huérfanas, revisar permisos/propiedad, contratos y validaciones. Revisar CSRF/cabeceras con adaptación coordinada de consumidores y pruebas, no activar a ciegas ni declarar protección productiva completa. Proponer cada corrección según evidencia, sin DDL automático. |
| F1 | Conexión de pantallas de gestión pendientes | Consumir la gestión de roles y auditoría nuevas desde ADMIN y el resumen estadístico existente. No declararlas completas por tener solo endpoints. |
| F2 | Pulido y regresión del circuito visible | Textos seguros en DOM/mapa, errores/cargas/vacíos, contexto multirrol, detalles abiertos, anchos, scroll y responsive; ensayo del circuito con roles separados y multirrol. |

Para B1 se identificaron antes del servicio los bordes no resueltos por el DER:
retirar CLIENTE con reserva/viaje o gestión financiera pendiente, retirar TECNICO
con órdenes abiertas asignadas y conservar al menos un ADMIN activo del sistema.
El usuario aprobó expresamente el 18/11: rechazar con 409 y explicación
cuando el retiro deje operaciones sin acceso o gestión sin administrador;
resolver las operaciones/reasignar primero, sin cancelaciones, pausas ni cambios
de roles implícitos. Implementación delegada después de esa aprobación; ver sección 16.

La consulta paginada y las operaciones de rol utilizarán DTO adecuados; no se
crean entidades nuevas para datos que ya representan USUARIO_ROL y
AUDITORIA_SISTEMA. Los detalles de endpoint/DTO se concretarán al implementar cada
bloque. Las implementaciones continúan manuales salvo delegación expresa.

Se mantienen fuera de estos bloques Mercado Pago real/despliegue productivo y la
propuesta diferida de exigir taller para iniciar mantenimiento. No se retoman
ahora pruebas SQL ni se crea BD de pruebas por decisión del usuario. La falta de
ejecución de integración posterior y las limitaciones de verificación quedan
visibles; no asignar un porcentaje global ni declarar fidelidad total al DER a
partir de conteos de tablas o un arranque con validate.

## 16. Gestión administrativa de roles conectada — 18/11

Bloque delegado expresamente por el usuario, incluyendo las tres protecciones de
retiro de CLIENTE, TECNICO y último ADMIN activo. Al implementar, el asistente no
ejecutó compilaciones, tests ni SQL; el usuario realizó después las comprobaciones
automáticas/manuales indicadas en la evidencia al final de esta sección.

### Código y conexión

- RolesUsuarioService concentra exclusivamente USUARIO_ROL. No se agregan reglas
  a mantenimiento ni a la edición de Persona; UsuarioService conserva sus rutas
  existentes. La separación evita inflar su constructor con repositorios ajenos
  a la actualización de datos personales/inactivación.
- RolesUsuarioController expone GET y PUT /api/usuarios/{id}/roles; aprovecha la
  autorización HTTP existente para consultas de usuarios y escrituras ADMIN,
  más comprobaciones ADMIN en controlador y servicio. El filtro de sesión vigente
  sigue verificando cuenta/roles en cada petición protegida.
- ActualizarRolesUsuarioDTO admite versión no negativa, lista no vacía de códigos
  CLIENTE/ADMIN/TECNICO sin duplicados y motivo normalizado de hasta 2000 caracteres.
  Rechaza campos ajenos como actorId. RolesUsuarioDTO devuelve únicamente usuarioId,
  nickname, versión y roles, sin entidades, datos personales o credenciales.
- GET consulta una cuenta activa; PUT solo modifica asignaciones de una cuenta
  activa y valida la versión leída. La misma composición no altera versión ni
  inventa auditoría. No modifica cuenta activa, datos personales, alquileres,
  órdenes ni movimientos financieros.
- admin.html incorpora un diálogo propio, roles-usuarios.js cargado una sola vez
  después de auth.js y antes de admin.js; la tabla de usuarios tiene el botón Roles.
  Selección, motivo y mensajes usan los datos/versión devueltos por el servidor.
  No se requieren SQL ni edición libre de claves numéricas de ROL para la gestión.

### Atomicidad, retiro y auditoría

Todos los cambios de roles toman primero el mismo bloqueo de la fila ADMIN del
catálogo existente y después actor/objetivo por ID ascendente. El conteo del
último ADMIN activo ocurre después del bloqueo, con READ_COMMITTED; no se basa
en contar sin serializar dos retiros simultáneos. Las cuentas no precargan sus
alquileres EAGER al esperar estos bloqueos. La operación y su auditoría se
confirman en una única transacción con rollback ante excepción.

Para retirar CLIENTE se comprueban reservas/viajes, detalles vigentes, liquidación
o costo final ausentes, pagos CREADO/PENDIENTE y reembolsos PENDIENTE/EN_PROCESO.
Una segunda consulta compara costo final ARS con pagos aprobados menos reembolsos
confirmados en ambos sentidos: deuda o reintegro pendiente. No se filtra borrado
lógico para ocultar obligaciones ni se recalculan precios/cotizaciones. Un intento
FALLIDO histórico no bloquea por sí solo si el saldo ya quedó resuelto; si todavía
se debe un reintegro lo detecta la comparación de importes confirmados.

Para retirar TECNICO se reutiliza la consulta de órdenes abiertas asignadas;
si existen se devuelve 409 sin pausarlas/desasignarlas. Retirar ADMIN exige que
permanezca otro administrador activo. No se permite dejar la cuenta sin roles.
Los rechazos no escriben asignaciones ni auditorías de éxito.

CAMBIAR_ROLES se audita sobre USUARIO con actor de sesión, motivo y JSON de listas
ordenadas anteriores/nuevas. Se registra antes de aplicar las asignaciones dentro
de la MISMA transacción para permitir al actor retirarse su ADMIN cuando hay otro:
no se invalida su autorización retrospectivamente ni se confirma una auditoría
si falla la escritura. La actualización fuerza el incremento de @Version de
Usuario (PESSIMISTIC_FORCE_INCREMENT) porque modificar solo la tabla intermedia
no basta como contrato de versión del formulario; la respuesta toma la versión
después del flush, sin calcularla manualmente en frontend.
Referencia técnica: [Hibernate 5.6, LockMode](https://docs.hibernate.org/orm/5.6/javadocs/org/hibernate/LockMode.html).

El diálogo conserva datos ante 409 y no reintenta el PUT automáticamente; para
recargar, cerrar y volver a abrir Roles. Bloquea doble clic y cierre mientras
guarda, muestra textos recibidos como texto y distingue escritura aceptada de
fallo posterior al actualizar la pantalla. Ante timeout sin respuesta no afirma
que no se haya escrito: pide recargar. Si el actor pierde su propio ADMIN,
confirma su perfil y vuelve al inicio sin cerrar la sesión ni consultar de nuevo
el listado ADMIN. Las demás sesiones reflejan el cambio en su siguiente petición.

### Comprobación pendiente

Pruebas nuevas: ActualizarRolesUsuarioDTOTest, RolesUsuarioServiceTest,
RolesUsuarioControllerTest y RolesUsuarios.test.cjs. AuditoriaAdministrativaServiceTest
incluye snapshot de roles/actor/motivo. En MantenimientoMysqlIntegrationTest se
preparan escenarios de persistencia/PK/versiones, rollback después de escrituras
y dos solicitudes con la misma versión; la clase sigue @Disabled/validate.
No se declara concurrencia SQL verificada por los mocks ni se reactiva esa suite.

Compilar con compileTestJava y ejecutar las nuevas pruebas junto con
UsuarioServiceTest, UsuarioControllerTest, AuditoriaAdministrativaServiceTest,
WebAuthorizationTest y SesionVigenteHttpTest. Ejecutar RolesUsuarios.test.cjs
junto con EdicionUsuarios.test.cjs/Sesion.test.cjs y las suites JS de regresión.
Una compilación correcta no sustituye la ejecución ni la prueba manual.

Tanda manual propuesta, solo con cuentas/datos QA nuevos, sin alterar roles de
admin.demo, cliente.demo, tecnico.demo ni parcial_playwright:

1. Registrar qa.roles como CLIENTE y entrar con admin.demo a Usuarios > Roles.
   Agregar TECNICO conservando CLIENTE, con motivo; comprobar listado y auditoría.
   Hasta implementar B2, la auditoría se comprueba por consulta SQL de solo lectura,
   no mediante una pantalla administrativa general que todavía no existe.
2. Abrir Roles sobre esa misma cuenta en dos pestañas ADMIN antes de guardar.
   Guardar A; B debe recibir 409 y conservar su formulario hasta recargarlo.
3. No permitir guardar todos los roles desmarcados ni motivo vacío. No confundir
   el aviso del frontend con probar el rechazo HTTP; las pruebas HTTP cubren ambos.
4. Con cuenta QA que tenga reserva/viaje o finanzas pendientes, retirar CLIENTE
   debe rechazarse. Resolver normalmente esas operaciones antes de reintentar.
5. Asignar una orden QA abierta a un TECNICO QA y comprobar el rechazo de retiro
   de TECNICO; resolver/reasignar la orden y repetir sin modificar técnicos demo.
6. Agregar ADMIN a qa.roles, iniciar sesión con ella en otro contexto y retirarse
   su propio ADMIN conservando CLIENTE y existiendo admin.demo activo. Debe guardar,
   auditar, volver al inicio y mantener acceso personal sin volver a loguearse.
7. No retirar roles de todos los administradores demo para forzar el caso del
   último ADMIN; esa comprobación destructiva de preparación queda para datos
   descartables. La condición de rechazo está cubierta unitariamente, no se
   declara demostrada en concurrencia real de BD con esta tanda manual.

Después de comprobar B1, el siguiente backend propuesto sigue siendo B2 (consulta
administrativa de auditoría); F1/F2 no se declaran terminados por este diálogo.

Evidencia manual informada por el usuario el 18/11: el panel muestra Roles;
asignó TECNICO conservando CLIENTE a qa.roles y la sesión de esa cuenta pudo
seguir entrando a Mis alquileres y abrir Mantenimiento sin volver a loguearse.
Confirmó las validaciones de roles/motivo y la prueba en dos pestañas: A retiró
TECNICO sin órdenes, B recibió el rechazo de versión desactualizada y no pisó A.
Al cerrar/reabrir el diálogo, GET mostró únicamente CLIENTE.

Observación de frontend pendiente: ese GET actualiza el diálogo, pero la tabla
de usuarios de la pestaña B conserva su listado anterior hasta F5. No confundir
esa presentación desactualizada con una escritura aceptada; sincronizar fila o
listado al consultar/recargar roles es parte del pulido pendiente. No se corrigió
automáticamente el código por este reporte.

El usuario confirmó también que qa.roles, con CLIENTE y TECNICO y una reserva
activa, no pudo perder CLIENTE: el guardado devolvió el rechazo específico de
operaciones pendientes. Después confirmó la cancelación normal de esa reserva
sin pago/retiro y el retiro de CLIENTE conservando TECNICO: Mis alquileres quedó
denegado y Mantenimiento siguió accesible sin volver a iniciar sesión. Con ello
queda comprobado ese bloqueo y desbloqueo, no el caso financiero aislado.
El usuario confirmó el rechazo de retiro de TECNICO a qa.roles cuando tenía una
orden abierta asignada, con el mensaje específico de finalizar/reasignar primero.
Luego confirmó la finalización de esa orden y el retiro de TECNICO conservando
CLIENTE. Queda comprobado ese bloqueo y desbloqueo. Luego confirmó que, tras
agregar ADMIN a qa.roles, esa cuenta pudo entrar al panel y retirarse su propio
ADMIN conservando CLIENTE: guardó, volvió al inicio sin logout, pudo acceder a
Mis alquileres sin nuevas credenciales y no pudo volver a Administración.
admin.demo siguió operativo.

La tanda manual principal queda comprobada en ese alcance. Continúa pendiente
la concurrencia SQL de las suites pausadas; no se declara el caso de último ADMIN
probado manualmente ni se alteran cuentas demo para forzarlo. Las pruebas nuevas
Java/JavaScript tienen sus comandos indicados; posteriormente el usuario confirmó
que repitió la compilación y las tandas automáticas y dieron OK. No se atribuyen
conteos exactos ni ejecución SQL a esa confirmación.

Para comprobar finanzas sin viaje activo, el usuario generó y devolvió normalmente
el alquiler 4 de qa.roles. Informó SALDO_PENDIENTE, costo final ARS 3054,19,
pagos aprobados ARS 511,88 y deuda ARS 2542,31, sin importe a reintegrar. Luego
confirmó que no pudo retirar CLIENTE conservando TECNICO: recibió el rechazo
específico de operaciones/gestiones financieras pendientes. Después confirmó el
pago por la simulación normal, la liquidación SALDADO y el retiro de CLIENTE
conservando TECNICO. Queda comprobado el bloqueo por deuda sin viaje activo y su
liberación después de resolverla, sin deuda QA pendiente de ese alquiler. No se
forzaron fechas ni importes por SQL y no se declara probado un reintegro aislado
solo por este escenario de deuda. El último ADMIN está cubierto por la tanda
unitaria informada; no se forzó ese estado retirando roles de cuentas demo.

Siguiente paso: guardar el avance de B1 y la pausa de integraciones pendiente
con commit/push; después retomar B2, consulta administrativa de auditoría. La
sincronización de la tabla al reabrir un diálogo tras 409 permanece anotada en F2.
