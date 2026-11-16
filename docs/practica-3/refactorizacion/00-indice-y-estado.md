# Refactorización de Prácticas III: índice y estado

Actualizado: 16 de noviembre de 2026.

Estado más reciente: etapa 332 del documento 37, usuario confirma compilación y
todas las pruebas aprobadas. Última tanda corroborada en XML: 34 SQL aprobadas,
cero fallos/errores/omisiones, 16/11 a las 04:48 de Argentina. Incluye cinco casos
nuevos del adicional: deuda oculta saldada permite reservar, rechazo, pendiente,
concurrencia y rollback. Diez fuentes coinciden con etapa 331 y son anteriores
al reporte. Unitarias/HTTP informadas aprobadas por el usuario; sus XML fueron
reemplazados por la última ejecución SQL, no se inventa otro total corroborado.
Sin correcciones Java por esta salida. Reintegros y frontend siguen pendientes.

Verificación anterior: etapa 330 del documento 37, parte 5 HTTP del usuario
corroborada en siete XML: 154 pruebas aprobadas, cero fallos/errores/omisiones,
16/11 a las 04:13 de Argentina. ConsultaAlquilerControllerTest tiene 43 casos,
incluidos los 17 nuevos de liquidación. Los cinco contextos MVC afectados,
seguridad y servicio financiero pasan; fuentes idénticas a etapa 329 y anteriores
al reporte. No requiere corrección Java. Solo revisión y seguimiento MD por el
asistente, sin volver a ejecutar tests/app/SQL o escribir Git.

Verificación anterior: etapa 327 del documento 37, parte 5 del usuario corroborada
en XML: 29 pruebas SQL aprobadas, cero fallos/errores/omisiones, 16/11 a las 03:57
de Argentina. Incluye seis escenarios nuevos de liquidación/deuda/transacciones,
caso reforzado de revisión económica y regresión de los recorridos existentes.
Fuente idéntica a la implementada en etapa 326 y anterior al reporte; no requiere
corrección Java. Consulta financiera HTTP, adicionales, ejecución de reembolsos y
conexión frontend siguen pendientes; no equivale a DER/circuito completo cerrado.
Asistente solo revisa evidencia y actualiza MD, sin ejecutar pruebas/app/SQL o Git.

Verificación anterior: etapa 324 del documento 37, parte 9 del usuario corroborada
en ocho XML: 176 pruebas aprobadas sin fallos/errores/omisiones, 16/11 a las 03:19
de Argentina. Incluye deuda en ambos productores, HTTP 409 y delegación del cierre,
además de liquidación/consulta y regresión. No se requiere corrección por esta salida.
Alcance unitario/MVC con repositorios/servicios simulados según caso; consulta SQL
y conexión real de liquidación/deuda siguen sin ejercitarse en esta tanda.
Próximo bloque: ejecución segura de reembolsos simulados; consumidor frontend y
restantes operaciones/corte del legado pendientes. R01/R02 continúan abiertos.

Implementación anterior: etapa 323 del documento 37, conexión de liquidación y bloqueo
por deuda incorporados por delegación expresa en los ocho archivos propuestos.
Liquidación al cerrar el conjunto en la misma transacción; deuda consultada al
crear por ambos productores, con cuenta bloqueada y CLIENTE vigente. HTTP 409 y
regresión adaptados, incluida expectativa SQL de cierre con costos/fecha únicos.
Revisión estática delimitada por hashes, sin compilar, ejecutar tests/app/SQL ni
escribir Git. Parte 9 reservada al usuario; nueva consulta SQL y conexión real
siguen pendientes de verificación. No equivale a circuito financiero terminado.

Preparación anterior: etapa 322 del documento 37, instrucciones manuales preparadas
para conectar liquidación al cierre backend y control de deuda en creación nueva
y anterior. Incluye consulta SQL derivada sin filtrar borrados, bloqueo de cuenta,
HTTP 409 y adaptaciones/pruebas de regresión, incluida expectativa SQL del cierre.
NO incorporado ni ejecutado todavía por el asistente. Pruebas reales del bloqueo
de deuda y de la conexión seguirán después; no se declara circuito cerrado.

Verificación anterior: etapa 321 del documento 37, comprobación del usuario
corroborada en cinco XML: 137 pruebas aprobadas sin fallos/errores/omisiones,
16/11 a las 02:43 de Argentina. LiquidacionAlquilerServiceTest incorpora 37
casos; otras 100 de devolución, retiro, anticipo y autorización son regresión.
Verifica lógica de liquidación/consulta con repositorios simulados, no sus
transacciones o locks reales. Sin corrección Java requerida por esta salida.
Siguiente: conectar liquidación automática y control de deuda de forma coordinada,
consulta HTTP y comprobación real; aún no está activada desde devolución.

Preparación anterior: etapa 320 del documento 37, instrucciones manuales de
LiquidacionAlquilerServiceTest preparadas, todavía no creado ni ejecutado.
JUnit 5/Mockito y Clock fijo, sin Spring/SQL: suma de subtotales/centavos,
reintento, pendientes, cierres, resumen de dinero confirmado y permisos frescos.
No activa liquidación, deuda o endpoints. Validación real de transacciones y
concurrencia queda para la integración posterior a su conexión al circuito.

Verificación anterior: etapa 319 del documento 37, LiquidacionAlquilerService creado
manualmente y revisado completo. Package, siete dependencias, constructor,
liquidación interna MANDATORY, consulta autorizada y auxiliares presentes como
se propuso; solo diferencias de formato. Usuario informa compileJava exitoso en
2 s; archivo .class posterior a la fuente corroborado, sin repetir compilación.
Sin conexiones desde otros servicios/controladores todavía. Siguiente: pruebas
específicas antes de activar liquidación y control de deuda; no circuito cerrado.

Preparación anterior: etapa 318 del documento 37, instrucciones manuales del
servicio LiquidacionAlquilerService preparadas: liquidación interna transaccional
del conjunto y consulta autorizada que consume LiquidacionAlquilerDTO. Todavía
no creado, compilado ni conectado a devolución/controlador. Sin cambio productivo
por el asistente. Pruebas y conexión backend con control de deuda siguen pendientes;
no se da por implementado el circuito financiero por entregar esta clase.

Verificación anterior: etapa 317 del documento 37, LiquidacionAlquilerDTO creado
manualmente por el usuario y revisado: ruta/package correctos, once campos final,
constructor y asignaciones completos, once getters y moneda ARS, sin setters ni
anotaciones JPA. CompileJava exitoso en 2 s informado por el usuario; no repetido
por el asistente. DTO de salida correcto, todavía sin servicio/controlador consumidor
ni pruebas de liquidación. No se cambia Java por esta revisión.

Preparación anterior: etapa 316 del documento 37, preparación manual de la respuesta
LiquidacionAlquilerDTO tras releer RN-ALQ-02/08 y DER v5. Situación financiera
calculada, sin otra entidad, enum o columna. Instrucciones del DTO entregadas,
todavía no creado ni conectado al servicio/controlador; no hay operación de
liquidación implementada por este paso. Suma definitiva, revisiones económicas,
idempotencia, bloqueo por deuda y movimientos posteriores siguen pendientes.

Verificación anterior: etapa 315 del documento 37, parte 4 ejecutada por el usuario
y corroborada en XML: 23 pruebas SQL aprobadas, cero fallos/errores/omisiones,
16/11 a las 01:54 de Argentina. Seis casos nuevos de devolución y diecisiete
anteriores aprobados en esta tanda. Se verifican recepción/costos/ocupación e
historias, orden correctiva única en reintentos concurrentes, preventiva,
rollback tras flush, rechazos de destino y revisión económica pendiente.
No equivale a circuito completo ni frontend: sigue liquidación conjunta,
vencimientos/cancelaciones y conexión en pantallas. Sin corrección Java por esta salida.

Implementación anterior: etapa 314 del documento 37, seis pruebas SQL de devolución
incorporadas por delegación expresa en AlquilerMysqlIntegrationTest. Recuento
estático: 23 casos. Los diecisiete anteriores y todos sus auxiliares/configuración
se conservan sin cambios. Único archivo src modificado: ese test; producción,
Gradle, frontend y perfiles intactos. Compilación/ejecución pendientes del usuario
(parte 4); no se presenta todavía ningún resultado SQL nuevo como aprobado.

Preparación anterior: etapa 313 del documento 37, instrucciones manuales preparadas
para seis pruebas SQL de devolución en AlquilerMysqlIntegrationTest, conservando
sus diecisiete casos anteriores. Todavía NO incorporadas ni ejecutadas por el
asistente. No se modifica producción ni se inicia app/base. Se mantiene ejecución
optativa y advertencia create-drop sobre turismo_sustentable, con app detenida.

Verificación anterior: etapa 312 del documento 37, comprobación del usuario
corroborada en diez XML: 182 pruebas aprobadas, cero fallos/errores/omisiones,
16/11 a las 01:37 de Argentina. Devolución: 33 unitarias y 20 MVC; regresión:
129 de retiro, reserva, anticipo, autorización, flujo anterior y modelos de
mantenimiento. Se acredita esta tanda con repositorios/servicio simulados según
el test, no concurrencia/rollback SQL real de la devolución ni navegador.
Siguiente paso: integración SQL propia de devolución; después liquidación
conjunta y conexión del circuito, sin considerar cerrado el bloque de alquileres.

Implementación anterior: etapa 311 del documento 37, devolución individual implementada
por delegación expresa: DTO de entrada, operación transaccional, permiso CLIENTE
propietario/ADMIN vigente, recepción en estación pública con capacidad, costo por
uso congelado, historias y enlace a una orden abierta cuando requiere revisión.
Pruebas unitarias/MVC de esa implementación verificadas ahora en etapa 312.
No se compila ni se consulta la base por el asistente. Frontend, liquidación conjunta, vencimientos,
cancelaciones e integración SQL de esta devolución siguen pendientes; no se cierra
el circuito ni se retira prematuramente el flujo anterior.

Verificación anterior: etapa 310 del documento 37, parte 4 ejecutada por el usuario
y corroborada en XML: diecisiete pruebas SQL aprobadas, cero fallos, errores u
omisiones, el 16/11 a las 01:14 de Argentina. Log confirma MariaDB103Dialect y
conector MariaDB 3.0.10. Los tres fallos temporales anteriores desaparecen sin
relajar assertions ni truncar fechas. Retiro backend verificado en los escenarios
unitarios/MVC anteriores y cinco SQL nuevos, no circuito completo ni navegador.
Avisos DDL iniciales corresponden a DROP FK de tablas inexistentes con create-drop;
1062/1452 son rechazos deliberados de su prueba de restricciones.
Última verificación: etapa 305, RetiroAlquilerControllerTest
incorporado manualmente y sus 18 casos MVC aprobados. Diez XML corroborados del
16/11 a las 00:08 de Argentina: 208 tests, cero fallos, errores u omisiones,
incluidos los 190 anteriores. Filtros/controlador/JSON/advice reales y servicio
simulado; integración MySQL del retiro todavía pendiente. Comprobación previa:
etapa 303, RetiroAlquilerServiceTest creado
por el usuario y sus 26 casos aprobados. Nueve XML corroborados del 15/11 a las
23:56 de Argentina: 190 pruebas aprobadas, cero fallos, errores u omisiones,
incluyendo las 164 de regresión anterior. Se verifica lógica del retiro en
unitarios con repositorios/trazabilidad simulados; HTTP y MySQL propios pendientes.
Comprobación previa: etapa 301, parte 5 de retiro ejecutada
por el usuario: BUILD SUCCESSFUL en 16 s y ocho XML corroborados, 164 pruebas
aprobadas sin fallos, errores u omisiones. Acredita compilación/regresión anterior,
no casos específicos del método retirarBicicleta recién incorporado. Siguiente
paso: pruebas específicas del retiro y luego persistencia/HTTP e integración
del circuito. Advertencias de IDs inválidos pertenecen a escenarios 400 esperados.
Tanda MySQL anterior: parte 5 de etapa 298 ejecutada por el
usuario y corroborada en XML: doce pruebas aprobadas, cero fallos, errores u
omisiones, el 15/11 a las 22:10 de Argentina. Incluye los cinco casos financieros
nuevos y los siete anteriores. Errores SQL 1062/1452 provocados deliberadamente
para comprobar UK/FK: sus rechazos son resultados esperados. Inicio y resolución
del anticipo también cuentan con la tanda anterior de 141 unitarios/MVC aprobados.
Checkpoint del bloque probado
creado localmente: ab05fb3, sin push. Compatibilidad temporal de lectura
ANTERIOR/DETALLES aprobada; consultas, servicio, controladores y presentación
conectados por delegación puntual. Regresión anterior de lectura corroborada:
94 casos Java aprobados, sin fallos, errores u omisiones. Consulta MVC incluye los 14
casos nuevos de listado (26 en total); consulta de servicio conserva 26 casos.
Última parte 3 Node: 52 casos aprobados sin fallos u omisiones, según salida
aportada; incluye los 15 nuevos de presentación sobre scripts reales. No es
validación visual en navegador. MySQL: siete casos aprobados, cero omitidos,
corroborados en XML de ejecución opt-in posterior. Primera tanda manual aprobada
según confirmación del usuario: sesión, permisos y listados vacíos. También
confirma la reserva sintética id 1 en ambas pantallas, detalles, importes y
cambio de moneda. Detecta dos incidencias visuales: reajuste horizontal de la
tabla ADMIN al desplegar y pérdida de apertura al cambiar moneda en Mis alquileres.
Sin correcciones visuales aplicadas. Prueba entre cuentas aprobada según el usuario:
qa.otro solo CLIENTE obtiene lista vacía y 403 al consultar el alquiler 1 ajeno;
la sesión válida se conserva. Salida SQL del cierre sintético: ambos detalles
VENCIDO con reales cero y sin retiro/devolución; bicicletas DISPONIBLE en estación 1.
Cabecera corroborada con salida aportada: VENCIDO, estimados 18/27540, finales
cero y cierre informado. Usuario confirma historial personal y listado ADMIN
coincidentes. Tanda manual de lectura terminada en ese alcance; no prueba el
circuito operativo ni vencimiento automático. Se retoma la base financiera manual.
Retiro de compatibilidad obligatorio al reemplazar alta/devolución anteriores.
Pruebas HTTP de ambos GET aprobadas en alcance MVC con servicio simulado.
Integración MySQL verifica filtros/orden y permisos del servicio real en los
escenarios preparados; no es una auditoría exhaustiva o prueba visual.
ListadoAlquileres.test.cjs aprobado en entorno simulado junto con las suites
existentes de inventario (15) y sesión (22). No se agregan reglas o entidades.
AlquilerMysqlIntegrationTest: cuatro casos nuevos y los tres anteriores aprobados
en ejecución separada de la regresión de 94 casos. Mantener opt-in, create-drop
y aplicación detenida al repetirlos. No se ejecuta SQL adicional en esta revisión.
Objetivo ratificado para el parcial del 16 de noviembre: todo el DER funcional,
circuito completo de ser posible; el final requiere proyecto completo y nube.
Esquema de simulación con saldo ficticio aprobado el 15 de noviembre: proveedor
separado del dominio, movimientos persistidos y sin tarjetas locales. No recorta
reglas financieras. Saldo inicial predeterminado ARS 1000000 ficticios aprobado
para pruebas; usuario exige también cuentas con menos saldo y cero.
Base financiera incorporada manualmente por el usuario y revisada: EstadoPago,
Pago, Reembolso y ambos historiales, con sus cinco repositorios. Sin diferencias
estructurales respecto del bloque del DER ni getters faltantes. compileJava
exitoso informado; diez class presentes con major 55 (Java 11). Sin arranque,
pruebas nuevas ni validación SQL/JPA ejecutados por el asistente. Mermaid y TXT
siguen idénticos y alineados con la simulación aprobada. Consulta de saldo propio
implementada y probada; escrituras de operaciones financieras todavía pendientes.
Precarga aplicada por delegación puntual: EstadoPagoRepository incorporado al
cargador y cinco códigos aprobados creados solo si faltan. Test existente ampliado
con mock y dos casos, conservando íntegros los doce anteriores. Parte 3 ejecutada
por el usuario y corroborada en XML: 14 aprobados, cero fallos, errores u omisiones
(2026-11-15T19:45:06.636Z). No se acredita aún carga real en MySQL: es unitario
con Mockito. Asistente no ejecutó Gradle, aplicación o SQL. Próximo paso: servicio
de pagos simulados, sin agregar saldo a Usuario o una tabla de billetera.
PasarelaSimulada, su test y propiedades locales incorporados por el usuario y
revisados sin correcciones. Ejecución anterior corroborada en ambos XML:
10 casos de pasarela + 14 de catálogos, 24 aprobados, cero fallos/errores/omisiones
(2026-11-15T20:19:55.686Z y 20:19:56.995Z). Valida respuestas, saldo cero sin
fallback y perfiles de pruebas; no carga real de MySQL ni cobros persistidos.
RECHAZADO por insuficiencia, PENDIENTE/CANCELADO por configuración de backend;
CREADO es inicial local. Adaptador sin escrituras; persistencia y transacciones
de cobros/reembolsos siguen pendientes.
Implementada primera operación de PagoService: GET /api/pagos/saldo-simulado, solo
CLIENTE vigente y para la identidad de sesión, perfil refactor. Saldo inicial
menos pagos APROBADO más reembolsos CONFIRMADO SIMULADA/PRUEBA en ARS, sin excluir
alquileres ocultos. Cinco archivos nuevos (DTO/servicio/controller y dos tests)
y una regla exacta de WebAuthorization aplicados manualmente y revisados sin
correcciones necesarias. Ejecución anterior: 59 aprobados, cero fallos, errores u
omisiones, corroborados en los cinco XML del 15/11, 22:07:33-38 UTC: servicio 9,
controller 5, pasarela 10, catálogos 14 y autorización 21. Mockito/MVC, sin MySQL
real ni prueba de concurrencia o cobro. El inicio local está implementado;
la resolución del proveedor y sus efectos todavía están pendientes.
Implementado manualmente el inicio del anticipo: ResultadoAnticipoDTO, ampliación de
PagoService y PagoController y regla POST CLIENTE. Registra solo CREADO y su
historial inicial, con monto derivado de detalles RESERVADO y clave UUID acotada
por cliente/finalidad. Cuatro archivos revisados sin errores de pegado o ubicación,
con getters, dependencias, transacción y permisos previstos. Dieciséis pruebas
unitarias en InicioAnticipoServiceTest y nueve MVC nuevas en PagoControllerTest,
conservando las cinco del GET, implementadas y aprobadas. Última ejecución del
15/11 a 22:58:12-18 UTC: inicio 16, saldo 9, controller 14, pasarela 10, catálogos
14 y autorización 21: 84 casos aprobados, sin fallos, errores u omisiones.
Sin pruebas reales nuevas de MySQL/concurrencia/rollback. Advertencia menor de
genéricos por respuesta secuencial de Mockito; no impide compilar o pasar tests.
El inicio local no cobra por sí mismo. Importe cero se informa sin crear pago ni
fingir aprobación; su retiro sigue pendiente. Sin modificar productores antiguos.
Aplicado POST /api/pagos/anticipo/{alquilerId}/resolver con la misma clave UUID,
CLIENTE propietario y bloqueo usuario/cabecera/pago. CREADO local incompatible se
cancela sin envío; PENDIENTE se concilia aun tras vencer/cerrar. Respuesta simulada
e historial en transacción; APROBADO vigente habilita solo detalles RESERVADO,
sin inicio de uso. Aprobación no habilitable sin uso registra obligación de
reembolso PENDIENTE, sin acreditar todavía dinero devuelto. Nuevas dependencias
son repositorios existentes; constructor de su test explícito también se ajusta.
Fecha explícita en historial de detalle y su llamada de reserva para coherencia UTC,
conservando constructor anterior. Aplicados solo los seis archivos previstos;
revisión estática sin cambios ajenos. Usuario ejecutó parte 5: BUILD SUCCESSFUL
en 10 s. Ocho XML del 15/11 a 23:28:17-22 UTC: 112 aprobados sin fallos,
errores u omisiones (los 84 anteriores más historial de detalle 7 y reserva 21).
No son pruebas del resolver ni de persistencia/concurrencia real MySQL. El
asistente no ejecutó Gradle, aplicación o SQL. Tests específicos del resolver
ejecutados por el usuario y corroborados en etapa 295. Creados ResolverAnticipoServiceTest (20 ejecuciones) y
nueve casos MVC del POST resolver, conservando los catorce actuales. Revisados
estáticamente y con alcance de archivos corroborado. Regresión dirigida del
15/11 a 23:59:43-48 UTC: 141 aprobados, sin fallos, errores u omisiones. Incluye
20 casos de resolver y 23 HTTP de pagos. Sin cambios de producción, Gradle,
SQL o datos nuevos; no acredita bloqueo/rollback reales. Caso
sin importe sigue identificado sin pago; su retiro se conectará sin inventar cobro.
Último recorrido MySQL/manual probado: lectura/listados de alquileres (271/277).
Base financiera y proveedor, saldo, inicio y resolución simulados probados en
unitarios/MVC (278-295) y los cinco escenarios financieros MySQL de etapa 298.
Aplicada ampliación de AlquilerMysqlIntegrationTest existente, conservando
sus siete casos, con cinco de pago, idempotencia/concurrencia, rollback de unidad
de trabajo, aprobación tardía/reembolso pendiente y restricciones UK/FK de PAGO.
Servicios/repositorios/pasarela reales; cotización existente simulada y Clock de
test controlado. Doce casos opt-in aprobados y corroborados en XML. Usa turismo_sustentable
con create-drop: app detenida, MySQL activo y datos descartables; no crea otra BD
ni agrega tablas/servicios nuevos. No se ejecuta Gradle, aplicación, SQL o escrituras Git por
el asistente. El test es una comprobación conservable, no código productivo
provisional. CargaCatalogosRefactor precarga catálogos reales solo en ese perfil;
su consolidación de nombre/perfil se evaluará al cerrar la migración, sin
duplicar cargadores. PasarelaSimulada es el proveedor de prueba aprobado,
no la integración definitiva con Mercado Pago.
Retiro por detalle aplicado en etapa 300: POST propio CLIENTE, bloqueos y
cobertura de anticipo, fecha UTC común, EN_CURSO/EN_USO, ubicación actual nula y
cabecera EN_PROGRESO. Cero requerido sin pago ficticio; reintento en EN_CURSO sin
duplicación. Compilación/regresión verificadas en etapa 301 y 26 pruebas unitarias
específicas aprobadas en etapa 303; 18 casos del endpoint aprobados en etapa 305.
Persistencia/concurrencia y rollback del retiro comprobados en etapa 310 mediante
los cinco casos nuevos de la clase SQL, junto con sus doce anteriores aprobados.
Siguiente bloque operativo: devolución individual. Continúan frontend, pruebas y
reemplazo coordinado del productor HTTP anterior, manteniendo pendientes los
demás circuitos del DER. Presencia de entidades no equivale a funcionalidades cerradas.
No confundirlo con alta/devolución nuevas.
Cambios recientes de Gradle/Playwright del usuario se conservan; todos
los tests Java están configurados para JDK 17 y main declara compatibilidad 11.
Regresión anterior: 204 Java aprobadas, tres MySQL omitidas y 22 Node aprobadas.
Referencia de seguimiento:
[revisión de coherencia y pendientes de cierre](40-revision-coherencia-migracion-der-v5.md).
El documento 37 conserva la bitácora; las notas de etapas anteriores no son
estados actuales ni invalidan reglas aprobadas posteriores.

- DER vigente: 29 entidades/55 relaciones; Mermaid y TXT idénticos. 29 entidades
  con clase Java; presencia de clases no implica circuito funcional ni auditoría
  integral de todos los campos o consumidores existentes.
- Reserva interna, consulta por ID y protección de borrado implementadas en el
  alcance probado. Alta/devolución HTTP, listados, estadísticas y controles de
  inventario aún mezclan consumidores antiguos. No activar creación nueva por HTTP.
- Tarjetas locales/suscripción retiradas y confirmadas en commit 25c54fa. Regresiones
  de limpieza: 173 casos y luego 21 de reserva en tandas distintas. Tres pruebas
  MySQL anteriores a esa limpieza; sin repetir SQL en esta revisión.
- Limpieza aplicada: enum GeneroUsuario y propiedades alquileres no completadas
  de DTO retirados; DELETE diferencia inexistencia/conflicto de fallo interno y
  denegación; inicio muestra alquiler solo con CLIENTE. Regresiones verificadas.
  Las lecturas nuevas deben coordinar consumidor y productor, no solo su JSON.
- Pendientes de circuito: pagos/deuda, retiro/devolución por detalle, vencimiento,
  cancelaciones, preventivo, incidencias, auditoría ADMIN y eventos SISTEMA.
- Pruebas dirigidas posteriores a etapa 253 verificadas; sin SQL o prueba manual
  de navegador nueva en esta tanda. Listados y circuito completo siguen pendientes.

## Antecedentes cronológicos (no representan el estado actual)

Etapa 198: AlquilerImportesTest creado
y revisado sin correcciones; cuarenta y ocho pruebas conjuntas verificadas
por XML sin fallos/errores/omisiones. Cálculo, persistencia y nuevo circuito
de cabecera/detalles siguen pendientes; no se sustituye el servicio anterior.

Etapa 195: Cinco fechas de cabecera de
Alquiler y sus getters/setters revisados sin correcciones; constructores anteriores
intactos. Cuarenta y cinco pruebas de regresión verificadas por XML sin fallos/
errores/omisiones. Asignación de fechas, cronología y reserva funcional pendientes
del nuevo servicio; sin reinicios o cambios a datos.

Etapa 193: AlquilerCotizacionTest creado
y revisado sin correcciones; cuarenta y cinco pruebas conjuntas verificadas
por XML sin fallos/errores/omisiones, incluidas seis específicas de cotización.
Persistencia y uso del snapshot en el nuevo servicio siguen pendientes.

Etapa 191: Cuatro campos del snapshot
de cotización de Alquiler y sus getters/setters revisados sin correcciones.
Treinta y nueve pruebas de regresión verificadas por XML sin fallos/errores/
omisiones; todavía sin pruebas específicas del snapshot o persistencia.
No se considera congelamiento funcional implementado: el servicio anterior
no los escribe/consume todavía. Campos opcionales durante la transición.

Etapa 186: HistorialEstadoDetalleTest
creado y revisado sin correcciones; ejecución conjunta verificada por XML:
veintitrés pruebas (siete del historial, ocho de incidencia y ocho de detalle),
sin fallos/errores/omisiones. Persistencia y flujo de servicio pendientes.

Etapa 184: HistorialEstadoDetalle y su
repositorio creados manualmente y revisados completos sin correcciones; compileJava
correcto según salida del usuario. Pruebas unitarias y persistencia pendientes;
todavía no se generan eventos desde servicios. Sin cambios al circuito de legado.

Etapa 182: IncidenciaTest revisado sin
correcciones; dieciséis pruebas conjuntas de incidencia/detalle verificadas por
XML sin fallos/errores/omisiones. Preparadas instrucciones de sus dos repositorios
JpaRepository, aún sin creación/compilación verificada. Reglas de servicio y
persistencia real pendientes.

Etapa 180: AlquilerDetalleTest creado
manualmente y revisado sin correcciones; ocho pruebas verificadas por XML,
sin fallos/errores/omisiones. Solo validación individual, no persistencia o
reglas de servicio. IncidenciaTest queda para el siguiente bloque.

Etapa 178: AlquilerDetalle e Incidencia
revisadas completas tras generar métodos: getters/setters correctos, sin
setters de id/version ni de fechaHoraRegistro en Incidencia. No requieren
correcciones Java; compilación correcta informada por el usuario. Persistencia
y reglas de servicio pendientes. El alquiler de legado y sus endpoints
permanecen sin modificar; el nuevo circuito todavía no es operativo.

Etapa 175: CrearAlquilerDTO creado
manualmente y revisado sin correcciones, compileJava correcto según salida
del usuario. Test creado manualmente y revisado, ocho pruebas unitarias/JSON
verificadas por XML sin fallos/errores/omisiones. Sin conexión todavía al nuevo
circuito; endpoint/servicio legado sin modificar. El máximo
de diez queda previsto como configuración de servicio, no límite rígido del DTO.
Checkpoint 94306f0 confirmado
con árbol limpio inicial y sin commits divergentes frente a la referencia remota.
Duración estimada común aprobada para todas las bicicletas del mismo alquiler:
se conserva en cada detalle, con retiros/devoluciones y costos reales individuales.
Se actualizan RN-ALQ-11 y comentarios de ambos Mermaid, sin cambios estructurales
o Java. Mínimos/máximos de estimación y límites acumulados siguen sin fijar.
Tests de orden e historial
creados manualmente y revisados; ocho pruebas de historial, seis de orden y
doce del cargador verificadas por XML (veintiséis sin fallos/errores/omisiones).
Checkpoint del modelo de soporte guardado; continúa pendiente la implementación
coordinada de cabecera/detalles e incidencia.
OrdenMantenimiento y repositorio
creados manualmente, revisados sin correcciones y compilados según salida del
usuario; sin servicio o flujo técnico todavía. HistorialOrdenMantenimiento y
repositorio también creados manualmente y revisados sin correcciones necesarias,
con compileJava correcto informado. DDL/persistencia real
y escritura transaccional de eventos siguen pendientes.
Checkpoint b5d0893 confirmado,
sin commits divergentes con la referencia remota y árbol limpio antes de esta
actualización documental de la etapa 161. EstadoOrdenMantenimiento y su
repositorio creados manualmente, revisados y compilados según salida del usuario;
TipoMantenimiento y su repositorio también creados manualmente. Precarga conjunta
de tipos/estados revisada en ambos archivos y doce pruebas verificadas por XML,
sin fallos/errores/omisiones. Sin correcciones Java necesarias; no se acredita
todavía persistencia/carga real ni implementación del módulo de órdenes.
EstadoAlquilerDetalle y su
repositorio fueron creados manualmente, revisados y compilados según salida
del usuario. Precarga del catálogo en refactor y dos pruebas incorporadas con
autorización puntual; ocho pruebas del cargador verificadas por XML, sin fallos,
errores u omisiones, incluidas las dos nuevas de estados de detalle.
No se reinició ni comprobó la precarga real en MySQL y no hay conexión a detalles
todavía. El enum legado se renombró con autorización a CodigoEstadoAlquiler,
incluidas referencias Java, pruebas y dos cadenas JPQL. Conserva los mismos
valores; es compatibilidad temporal, no el catálogo nuevo. Renombre comprobado
con 66 pruebas sin fallos/errores/omisiones en los cuatro XML. EstadoAlquiler
y su repositorio fueron creados manualmente, revisados y compilados según salida
del usuario. Precarga de sus cinco estados y dos pruebas incorporadas con
autorización puntual, conservando las ocho anteriores. Diez pruebas del cargador
verificadas por XML sin fallos/errores/omisiones, sin conectar aún el flujo legado
a la entidad nueva ni comprobar la carga real en MySQL. Checkpoint guardado;
sigue la organización de cabecera/detalles con sus dependencias del
DER, sin tablas paralelas o identificadores provisionales en lugar de FK.
El checkpoint de inventario se guardó en
632c210; las cifras y resultados siguientes corresponden a RN-INV-04:
50 pruebas Java sin fallos/errores/omisiones corroboradas por XML y 15 Node
correctas según salida aportada por el usuario. Retirada de QA-RET-01 desde
pública verificada: fila conservada, DE_BAJA, borrado true, ubicación nula,
una transición y auditoría del origen real. QA-RET-02 fue trasladada al Taller
y dada de baja sin eliminarse, con ubicación/ocupación e historia conservadas.
La pública quedó vacía/dos libres; Taller una presente/cuatro libres, según SELECT.
INV04-01 verificado: Dar de baja desde pública rechazado, sin cambios ni
eventos de éxito. INV04-02 verificado en su variante cancelar: texto correcto
y sin cambios/eventos nuevos. INV04-03 e INV04-04 verificados, incluida la
consulta visual de eliminadas informada por el usuario. Cambio puntual validado
y guardado mediante commit/push, sin dar por completo el DER.
Eliminar con retirada física confirmada admite
origen público o interno activo; Dar de baja sin eliminar sigue exigiendo
Taller. No cambia permisos, estados ni estructura del DER. La etapa 145 y
los resultados siguientes corresponden a la versión previa de RN-INV-02:
121 pruebas Java verificadas por XML y 15 Node informadas correctas. Se reinició
el entorno QA y se comprobaron altas, capacidad, traslado, pausa/habilitación,
baja y eliminación física con persistencia/trazabilidad, consulta visual de
eliminadas, conflicto de versión y eliminación directa desde DISPONIBLE.
Los casos exclusivamente automáticos se distinguen en el documento 39.
RN-INV-03 aprueba la excepción por pérdida
definitiva sin Taller (documento 26, sección 9); no está implementada y se
conectará con incidencias. No cambian las 29 entidades/55 relaciones.

## Cómo leer la documentación

- **Aprobada para implementar**: regla confirmada por el usuario que debe respetarse al diseñar el modelo, el código y las pruebas.
- **Propuesta o pendiente**: decisión todavía no confirmada. No debe implementarse silenciosamente como si estuviera aprobada.
- **Implementada**: requiere evidencia de cambios concretos en el código. Una aprobación en el chat no equivale a implementación.
- **Verificada**: requiere pruebas ejecutadas con su resultado y evidencia. Una prueba escrita en un documento no equivale a una prueba superada.

El estado actual del código y sus verificaciones se consulta en [37-implementacion-entidades.md](37-implementacion-entidades.md). Los documentos de diseño conservan sus antecedentes y aprobaciones históricas; sus frases originales sobre implementación no reemplazan ese registro. Las mejoras surgidas de la comparación de copias están en [38-mejoras-futuras-desde-version-companero.md](38-mejoras-futuras-desde-version-companero.md) y siguen pendientes de confirmación.

La corrección de inventario aprobada el 6 de noviembre, [RN-INV-01, sección 7 del documento 26](26-revision-estaciones-tipos-y-bicicletas.md), conserva BICICLETA.borrado_bici separado de DE_BAJA e incorpora ESTACION.es_publica para Taller interno. La exigencia inicial de Taller para ambas acciones es un antecedente: bajo RN-INV-02/RN-INV-04, Eliminar con retirada física puede hacerse desde pública o interna activa; Dar de baja sin eliminar sigue exigiendo Taller y conserva ocupación. INACTIVA no exige traslado. El 7 de noviembre se confirmó que, por el momento, Uso se fija al crear y no se reclasifica en la edición común. Los artefactos Mermaid y TXT siguen representando 29 entidades y 55 FK/relaciones. El estado de implementación y pruebas vigente es el de la cabecera y el diario 37; no implica completar los demás módulos del DER. La antigua propuesta de eliminar el flag queda corregida.

Bloques verificados el 7 de noviembre: las etapas 81 a 95 del documento 37 registran la corrección de sesión y la ronda manual de estaciones. Las etapas 97 a 115 documentan la preparación de TipoBicicleta/EstadoBicicleta como entidades, repositorios, precarga y valores QA expresamente aprobados; los últimos cuatro filtros suman 22 pruebas sin fallos, verificadas mediante XML. Las comprobaciones de catálogos son en memoria, no acreditan DDL/UNIQUE/persistencia real ni su uso por Bicicleta. El siguiente bloque es adaptar las relaciones de Bicicleta y sus consumidores, sin sustituir silenciosamente los estados/precios de operaciones existentes. No equivale a completar todo el DERv5 ni a haber probado la expiración real de sesión por tiempo.

## Fuentes que deben contrastarse antes de proponer cambios

Por indicación expresa del usuario del 28 de octubre de 2026, antes de cada nueva propuesta se revisarán el [diccionario de estados original](<D:/Karim/Downloads/Desarrollo de Software/3er Año/Prácticas 3/TP1 TURI/Diccionario de Estados.docx>) y el [PDF de visión del proyecto](<D:/Karim/Downloads/Desarrollo de Software/3er Año/Prácticas 3/TP1 TURI/TP1/TP1 Prácticas III - Karim Homsi.pdf>), además del [DER v4](<D:/Karim/Downloads/Desarrollo de Software/3er Año/Prácticas 3/TP1 TURI/DER/DERv4.txt>) cuando afecte al modelo. Se compararán con las decisiones ya aprobadas en esta carpeta.

Los documentos originales expresan la visión y sirven de referencia; sus diferencias con decisiones posteriores no se resolverán silenciosamente. Se señalará qué conserva la propuesta, qué corrige por una regla ya aprobada y qué necesita una nueva decisión. Los archivos originales no se reemplazan con la documentación de trabajo.

## Base principal y versión paralela del compañero

El 28 de octubre de 2026 el usuario ratificó que el desarrollo principal es este repositorio local, guiado por las correcciones del PDF, DER y diccionario de estados y por las decisiones aprobadas en esta carpeta.

Se inspeccionaron en modo lectura las copias entrega-v3.2-registro-imagenes-admin y entrega-v3.6-correos-y-libro-diario. La comparación de la v3.6 con la v3.2 confirmó nuevas funciones de correo, comprobantes y cierre diario, pero conserva diferencias con las reglas aprobadas de este repositorio. No se integraron esas copias ni se reprodujeron sus pruebas durante la revisión. El documento 38 registra propuestas de mejora, no una autorización para sustituir esta base ni importar su SQL.

Como referencia histórica, el diagnóstico anterior a la implementación tomó main en ab91d6a, su CommandLineRunner y el enum de rol único. Esa descripción no es el estado actual de la refactorización. El trabajo continúa en codex/refactorizacion-practicas-iii; su avance y comprobaciones están documentados en el archivo 37. Rol, UsuarioRol y su clave compuesta ya se incorporaron como entidades; CodigoRol ya no existe ni tiene referencias en src. TipoBicicleta y EstadoBicicleta existen como entidades con repositorios y precarga en refactor. En la etapa 119 se conectó Bicicleta a esos catálogos mediante FK, se incorporaron fecha_alta/version y se retiraron el precio por unidad y los enums transitorios CodigoTipoBicicleta/CodigoEstadoBicicleta. Se adaptaron inventario, consumidores, tarifa por tipo y compatibilidad con el alquiler existente, más su trazabilidad mínima prevista en el DER. La compilación, pruebas y comprobación del nuevo esquema quedan pendientes del usuario; esto no completa todavía los nuevos módulos de reservas, detalles de alquiler, incidencias, mantenimiento o pagos.

## Documentos y decisiones

| Documento | Contenido | Estado |
| --- | --- | --- |
| [01-diagnostico-y-plan.md](01-diagnostico-y-plan.md) | Comparación de la aplicación actual con el PDF y el DER v4, riesgos y orden de trabajo | Diagnóstico y plan propuesto; consultar cada regla para sus decisiones vigentes |
| [02-regla-de-cobro.md](02-regla-de-cobro.md) | RN-ALQ-01: anticipo, costo real, reembolso, adicional sin multas y bloqueo de nuevas contrataciones por deuda | Aprobada para implementar; sin implementación ni pruebas de ejecución de la nueva regla |
| [03-alquiler-con-varias-bicicletas.md](03-alquiler-con-varias-bicicletas.md) | RN-ALQ-02: detalles independientes y liquidación final conjunta | Aprobada para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [04-reserva-y-vencimiento.md](04-reserva-y-vencimiento.md) | RN-ALQ-03: 15 minutos para pagar y retirar, liberación de unidades no retiradas y ajuste del anticipo | Aprobada para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [05-tarifa-moneda-y-cotizacion.md](05-tarifa-moneda-y-cotizacion.md) | RN-ALQ-04: tarifa USD y cotización congeladas al confirmar; movimientos de dinero en ARS | Aprobada para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [06-cancelacion-antes-del-retiro.md](06-cancelacion-antes-del-retiro.md) | RN-ALQ-05: cancelación de unidades no retiradas, liberación inmediata, sin penalización y ajuste conjunto | Aprobada para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [07-revision-diccionario-de-estados.md](07-revision-diccionario-de-estados.md) | Revisión del diccionario original y diferencias frente a las reglas confirmadas | Revisión de propuesta; no aprueba estados ni excepciones nuevas |
| [08-diccionario-estados-propuesto.md](08-diccionario-estados-propuesto.md) | Diccionario de trabajo con estados y transiciones de uso, situación financiera, bicicleta, pagos, reembolsos y mantenimiento | Reglas base, administración e incidencias aprobadas; representación física e integraciones expresamente indicadas pendientes, no implementado |
| [09-devolucion-demora-y-fallas.md](09-devolucion-demora-y-fallas.md) | RN-ALQ-06: DEVUELTO con condiciones independientes de demora y falla reportada; bloqueo para revisión técnica | Aprobada para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [10-estados-cabecera-alquiler.md](10-estados-cabecera-alquiler.md) | RN-ALQ-07: cabecera RESERVADO, EN_PROGRESO, FINALIZADO, CANCELADO y VENCIDO, separada de la situación financiera | Aprobada para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [11-situacion-financiera-alquiler.md](11-situacion-financiera-alquiler.md) | RN-ALQ-08: SIN_LIQUIDAR, SALDO_PENDIENTE, REEMBOLSO_PENDIENTE y SALDADO, independientes del uso y de cada pago | Aprobada para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [12-tipos-y-estados-mantenimiento.md](12-tipos-y-estados-mantenimiento.md) | RN-MAN-01: catálogo TIPO_MANTENIMIENTO, tipos PREVENTIVO/CORRECTIVO y cinco estados de orden | Aprobados tipos y estados; permisos manuales completados en RN-MAN-02, no implementado |
| [13-estados-operativos-bicicleta.md](13-estados-operativos-bicicleta.md) | RN-BIC-01: seis estados iniciales; RN-INC-01 incorpora EXTRAVIADA como séptimo estado | Aprobados para el diseño; no implementados ni probados |
| [14-estados-pago-y-reembolso.md](14-estados-pago-y-reembolso.md) | RN-PAG-01: cinco estados de pago, cuatro de reembolso y conservación del pago original con reembolsos vinculados | Aprobados para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [15-reintentos-pago-durante-reserva.md](15-reintentos-pago-durante-reserva.md) | RN-PAG-02: rechazos sin cancelar ni liberar bicicletas y reintentos dentro de los 15 minutos originales | Aprobada para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [16-estados-detalle-alquiler.md](16-estados-detalle-alquiler.md) | RN-ALQ-09: seis estados iniciales; RN-INC-01 incorpora CERRADO_POR_INCIDENCIA | Catálogo de siete estados aprobado para el diseño; sin implementación ni pruebas |
| [17-permisos-y-transiciones-mantenimiento.md](17-permisos-y-transiciones-mantenimiento.md) | RN-MAN-02: reparto ADMIN/TECNICO, transiciones manuales de órdenes y habilitación posterior con revisión favorable | Aprobados para implementar; criterios de aceptación documentados, todavía no ejecutados |
| [18-generacion-y-organizacion-ordenes.md](18-generacion-y-organizacion-ordenes.md) | RN-MAN-03: correctivo automático desde devolución con falla, una orden abierta por bicicleta y asignación inicial opcional | Tres reglas aprobadas para implementar; pruebas especificadas, todavía no ejecutadas |
| [19-propuesta-preventivo-por-calendario.md](19-propuesta-preventivo-por-calendario.md) | RN-MAN-04: preventivas automáticas según tipo y fecha de revisión, reutilizando órdenes y estados | Requisito y condiciones de aplicación aprobados; valores numéricos y detalles físicos/técnicos pendientes, no implementado |
| [20-propuesta-administracion-y-excepciones-bicicletas.md](20-propuesta-administracion-y-excepciones-bicicletas.md) | RN-BIC-02: inactivación, reactivación, baja y cancelación operativa de reservas | Reglas administrativas 1 a 6 aprobadas; incidencias incorporadas después por RN-INC-01, no implementada |
| [21-propuesta-minima-incidencias-en-viaje.md](21-propuesta-minima-incidencias-en-viaje.md) | RN-INC-01: cierre excepcional, corte validado, EXTRAVIADA y recuperación | Circuito mínimo aprobado para el diseño; precisión económica de la sección 8 también aprobada, no implementado |
| [22-revision-del-der-y-mapa-de-cambios.md](22-revision-del-der-y-mapa-de-cambios.md) | Comparación con las 21 entidades originales y ocho incorporaciones, incluyendo el historial de incidencia | DER lógico vigente de 29 entidades y 55 FK/relaciones; historial de orden renombrado/ampliado; diseño físico y código pendientes |
| [23-pasarela-mercadopago-y-pruebas.md](23-pasarela-mercadopago-y-pruebas.md) | Mercado Pago elegido, responsabilidades del sistema, pruebas y propuesta de adaptador local | Pasarela elegida para futura integración; modalidad y simulador pendientes, no implementado |
| [24-propuesta-historial-financiero.md](24-propuesta-historial-financiero.md) | Historial completo de estados de cada pago y reembolso, sin sobrescribir intentos ni duplicar dinero | Incorporación aprobada y conservada en el DER vigente; sin implementación en Java ni base de datos |
| [25-revision-personas-usuarios-y-roles.md](25-revision-personas-usuarios-y-roles.md) | Campos, claves, documento obligatorio, catálogos, nickname y roles | Las tres incorporaciones aprobadas e integradas al Mermaid, con unicidad documental compuesta y email/nickname independientes; diseño físico y código pendientes |
| [26-revision-estaciones-tipos-y-bicicletas.md](26-revision-estaciones-tipos-y-bicicletas.md) | Campos, capacidad física, tarifa por tipo, preventiva, RN-EST-01 y RN-INV-01 | Aprobados baja lógica de estación, Taller interno, borrado separado de DE_BAJA y sus condiciones; nuevos campos reflejados en el DER; código efectivo en documento 37 |
| [27-revision-ordenes-e-historial-mantenimiento.md](27-revision-ordenes-e-historial-mantenimiento.md) | Campos y relaciones de mantenimiento, transiciones, asignaciones y costos | Historial ampliado y renombrado a HISTORIAL_ORDEN_MANTENIMIENTO conforme a RN-TRZ-01; se mantienen diagnóstico y ARS; Java y base sin cambios |
| [28-revision-alquiler-y-detalles.md](28-revision-alquiler-y-detalles.md) | Claves, fechas, importes, cierre operativo, liquidación y trazabilidad por unidad | Se agrega incidencia_corte_id opcional, FK/UK, conforme a RN-TRZ-01; conserva unicidad alquiler/bicicleta y tipo contratado; Java y base sin cambios |
| [29-propuesta-precision-tiempo-y-redondeos.md](29-propuesta-precision-tiempo-y-redondeos.md) | RN-ALQ-10: segundos completos, redondeos por detalle y suma del conjunto | Aprobada y reflejada en comentarios del DER; ejemplos verificados aritméticamente, implementación Java y SQL pendientes |
| [30-propuesta-composicion-contratacion.md](30-propuesta-composicion-contratacion.md) | RN-ALQ-11: origen común, máximo configurable de diez y selección fija; listado de cierres restantes | Aprobada e incorporada al DER lógico: estación de retiro en cabecera; 28 entidades y 52 FK/relaciones; Java y base sin cambios |
| [31-revision-pagos-reembolsos-y-medios-guardados.md](31-revision-pagos-reembolsos-y-medios-guardados.md) | Revisión financiera y RN-PAG-03; antecedente descartado RN-PAG-04 | Cancelación durante el anticipo aprobada; perfiles y medios guardados descartados para esta versión, no pendientes de incorporación |
| [32-pago-externo-con-mercadopago.md](32-pago-externo-con-mercadopago.md) | RN-PAG-05: Checkout Pro con redirección, sin administración local de tarjetas; cambio de alcance frente al PDF | Experiencia de pago y alcance aprobados; 28 entidades y 52 FK/relaciones; API concreta, campos de correlación, integración y pruebas pendientes |
| [33-revision-trazabilidad-incidencias-y-ordenes.md](33-revision-trazabilidad-incidencias-y-ordenes.md) | D07-A y antecedente D07-B de historial de atención | D07-A aprobada e incorporada mediante RN-TRZ-01; D07-B sigue descartado, sin tabla de atención |
| [34-trazabilidad-ordenes-e-incidencias.md](34-trazabilidad-ordenes-e-incidencias.md) | Antecedente de campos del modelo de 28/52 previo a RN-TRZ-01 | Recomendación general retirada; consultar documentos 35/36 para estructura vigente y auditoría exclusiva de ADMIN |
| [35-auditoria-admin-y-brechas-de-trazabilidad.md](35-auditoria-admin-y-brechas-de-trazabilidad.md) | Auditoría exclusiva de ADMIN y cuatro mejoras de trazabilidad | Las cuatro aprobadas e incorporadas al DER lógico; no implementadas en Java ni SQL |
| [36-incorporacion-aprobada-trazabilidad-der.md](36-incorporacion-aprobada-trazabilidad-der.md) | RN-TRZ-01: campos, claves, relaciones, nulabilidad, fundamento del corte y pruebas previstas | Incorporación aprobada: 29 entidades y 55 FK/relaciones; se conservan las otras 25 entidades y se documentan controles que el dibujo no garantiza |
| [37-implementacion-entidades.md](37-implementacion-entidades.md) | Registro de implementación manual, revisiones, pruebas y pendientes del backend | Fuente del estado efectivo del código; no declara completado todo el DER |
| [38-mejoras-futuras-desde-version-companero.md](38-mejoras-futuras-desde-version-companero.md) | Mejoras técnicas, cola de correos, comprobantes, reporte financiero, último ADMIN y cierres opcionales | Propuestas documentadas a pedido; pendientes de confirmación; sin cambios al DER o Java |
| [39-pruebas-inventario-eliminacion-definitiva.md](39-pruebas-inventario-eliminacion-definitiva.md) | Ejecución filtrada Java/Node, matriz manual RN-INV-02 y consultas de evidencia sin secretos | Preparadas para el usuario; no ejecutadas por el asistente en este bloque |

Modelo editable asociado: [der-v5-propuesto.mmd](der-v5-propuesto.mmd). No sustituye ni sobrescribe DERv4.txt.

Exportación Mermaid sincronizada para entregar: [DERv5.txt](../entregables/DERv5.txt). La corrección de inventario no agrega un estado al catálogo. El Word de estados entregado anteriormente no se regeneró en esta corrección; las condiciones administrativas vigentes se consultan en RN-INV-01 y el diccionario de trabajo del documento 08.

## Próximo punto de definición

La identificación y el acceso, RN-EST-01, mantenimiento, los refuerzos de alquiler, RN-ALQ-10/RN-ALQ-11 y las cuatro mejoras de trazabilidad RN-TRZ-01 ya fueron aprobados y reflejados en el DER. El modelo vigente tiene 29 entidades y 55 FK/relaciones; estacion_retiro_id pertenece a ALQUILER y el máximo inicial es diez, configurable. RN-PAG-03 aprobó la cancelación durante el anticipo y RN-PAG-05 el checkout externo sin tarjetas locales. Sigue concretar esquema físico/transición y controles técnicos de integración D03/D06. Fuente/frescura de cotización, intervalos preventivos, duración/límites acumulados y alcance de QR siguen identificados, sin exigir todos ellos un rediseño de tablas. Esto no equivale a tener una base migrada ni las reglas nuevas funcionando en Java.

La revisión financiera está documentada en el archivo 31: conserva los dos historiales, desarrolla los controles de reembolso y registra RN-PAG-03 aprobada. PERFIL_PAGO, MEDIO_PAGO_GUARDADO y su FK desde PAGO fueron descartados, no quedan esperando aprobación. El documento 32 registra RN-PAG-05 y ajusta el requisito original de medios vinculados: el usuario elige cómo pagar en Mercado Pago, sin un ABM local de tarjetas. Checkout Pro está elegido como experiencia, pero su variante de API, la correlación de recursos y las pruebas siguen pendientes.

La trazabilidad aprobada se consolida en los documentos 35 y 36. HISTORIAL_ATENCION_MANTENIMIENTO sigue descartado y AUDITORIA_SISTEMA continúa exclusiva de ADMIN con administrador_id obligatorio, ahora también motivo. HISTORIAL_INCIDENCIA conserva los eventos del reporte; el historial de orden se amplía y renombra HISTORIAL_ORDEN_MANTENIMIENTO para cambios técnicos sin transición; ALQUILER_DETALLE incorpora incidencia_corte_id opcional, FK/UK, de una incidencia del mismo detalle. Los historiales mantienen actor y origen reales, sin administradores ficticios. Las cuatro mejoras están incorporadas al modelo de 29/55, no al código ni a la base.

| Bloque de cierre | Decisiones que reúne | Situación |
| --- | --- | --- |
| Origen y organización de mantenimiento | Generación de órdenes desde reportes, cantidad de órdenes abiertas, asignación inicial y preventiva por calendario | RN-MAN-03 y condiciones de RN-MAN-04 aprobados; intervalos numéricos y detalles de implementación pendientes |
| Administración y excepciones de bicicletas | Inactivación, reactivación, baja, intervención sobre reservas e incidencias durante un viaje | RN-BIC-02 y circuito mínimo RN-INC-01 aprobados; precisión económica de devolución previa a la revisión también aprobada; falta implementación |

Una vez cerrados o delimitados esos recorridos, se consolida el diccionario y se contrasta con el DER revisado. Un borrador del DER puede prepararse mientras se resuelven los pendientes, manteniéndolos identificados. El cierre del diccionario no se confunde con tener resueltos el proveedor de pagos, hardware, precisión monetaria, migración ni todos los detalles físicos del modelo: se trabajarán en su etapa sin darlos por aprobados ni omitir sus restricciones.

## Registro de evolución de las decisiones

Los párrafos siguientes conservan cada etapa y sus cantidades históricas. No sustituyen el estado vigente: RN-TRZ-01 está incorporada y el modelo actual tiene 29 entidades y 55 FK/relaciones, según los documentos 22 y 36.

El contraste se inició en el documento 22 y el modelo Mermaid asociado. En la etapa de identificación se conservaron las 21 entidades del original y se incorporaron TIPO_MANTENIMIENTO, REEMBOLSO, INCIDENCIA, los dos historiales financieros, TIPO_DOCUMENTO y PAIS, con un total entonces de 28 entidades. La tarifa por tipo y las tres incorporaciones de identificación/acceso fueron confirmadas expresamente. Esa etapa continuó con estaciones, tipos y bicicletas; no cerró automáticamente los demás campos, el diseño físico o la migración.

La cancelación voluntaria está aprobada en RN-ALQ-05 y la precisión de tiempo/redondeos se cerró después mediante RN-ALQ-10. La política de monedas y congelación está aprobada en RN-ALQ-04; permanecen pendientes el proveedor/tipo de cotización, su antigüedad admisible y el tratamiento de fallos. La escala monetaria acordada no resuelve por sí sola la precisión total de almacenamiento ni genera una migración.

Se recibió y revisó el diccionario de estados original. Sirve de base para ese bloque, pero sus multas, cierre forzoso de 24 horas, preaprobación de tarjeta y otras excepciones no se consideran reglas aprobadas por el solo hecho de figurar en el documento. La revisión 07 identifica las diferencias y el documento 08 mantiene la versión unificada con sus aprobaciones y pendientes por bloque.

El 28 de octubre el usuario confirmó los seis estados de ALQUILER_DETALLE: RESERVADO, PENDIENTE_RETIRO, EN_CURSO, DEVUELTO, CANCELADO y VENCIDO, documentados en RN-ALQ-09. Se conserva la separación entre estados del detalle, de bicicleta y de pago, además de las condiciones de devolución de RN-ALQ-06. Posteriormente aprobó por separado el reparto de permisos de mantenimiento y la habilitación administrativa de RN-MAN-02; las restantes excepciones no se aprueban por extensión.

El usuario aprobó DEVUELTO con condiciones independientes de demora y falla reportada, incluido el caso combinado, y que una bicicleta con falla reportada quede fuera de alquiler para revisión. Está documentado en RN-ALQ-06. Esta aprobación no convierte todo el documento 08 ni todo el flujo de mantenimiento en reglas aprobadas.

La cabecera tiene sus cinco estados aprobados en RN-ALQ-07 y las cuatro situaciones financieras independientes se aprobaron en RN-ALQ-08. Los seis estados operativos de la bicicleta se aprobaron en RN-BIC-01. El 28 de octubre se aprobaron los cinco estados de pago y los cuatro de reembolso en RN-PAG-01, con registro separado y vinculado al pago original.

El 28 de octubre se aprobó RN-PAG-02: un rechazo confirmado no cancela la reserva ni libera bicicletas; el cliente puede reintentar dentro de los 15 minutos originales, sin reiniciar el plazo y sin cancelar por el segundo rechazo. Esta decisión sustituye expresamente la cancelación inmediata por rechazo del flujo original del PDF y del Word. Siguen pendientes las decisiones expresamente indicadas en el documento 08 y la consolidación del DER revisado.

Se aprobó agregar TIPO_MANTENIMIENTO y los cinco estados de la orden en RN-MAN-01. Su relación con la orden se incorporó a un fragmento del modelo de trabajo. La distinción PREVENTIVO/CORRECTIVO se conserva y la bicicleta utiliza MANTENIMIENTO según RN-BIC-01. Los permisos y las transiciones del recorrido manual se completaron mediante la aprobación de RN-MAN-02.

El 28 de octubre el usuario aprobó el reparto: ADMIN gestiona órdenes y asignaciones, TECNICO actúa sobre sus órdenes asignadas y ADMIN autoriza después la vuelta al servicio con revisión técnica favorable, estación activa válida y sin impedimentos. Se documentó en RN-MAN-02 y se actualizó la sección 7 del documento 08.

Posteriormente aprobó RN-MAN-03: correctivo automático desde una devolución con falla, máximo de una orden abierta por bicicleta y posibilidad de CREADA sin técnico hasta la asignación administrativa. Después aprobó incorporar preventivas periódicas según el tipo como requisito de esta versión en RN-MAN-04, documentado en el archivo 19, y luego sus condiciones de aplicación. Los intervalos numéricos de ejemplo siguen sin ser definitivos.

El 4 de noviembre aprobó ambas condiciones para inactivar a un técnico con órdenes: pausa administrativa excepcional de EN_PROCESO a PAUSADA y desasignación temporal de las órdenes abiertas, conservando sus estados en CREADA/PAUSADA y sin modificar las cerradas. Se incorporó en RN-MAN-02, sección 3.1, el diccionario de trabajo, las revisiones del modelo y ambos archivos Mermaid v5. La misma orden conserva trabajo e historia, la bicicleta sigue en mantenimiento y otro técnico activo debe ser asignado antes de iniciar/reanudar. Solo se precisó la condición de tecnico_id; no hay nuevas tablas, claves ni relaciones. Diseño aprobado, no implementación ni pruebas; ver la etapa 33 del diario 37.

El 28 de octubre el usuario aprobó expresamente las condiciones de la sección 4 de RN-MAN-04: diferir la revisión de calendario durante un viaje o reserva ya aceptada, bloquear nuevas contrataciones vencidas, atender el preventivo sin duplicar un correctivo abierto, exigir revisión inicial cuando no haya referencia fiable y reservar los cambios de intervalo a ADMIN. Se actualizó la excepción al retorno normal a DISPONIBLE en las reglas relacionadas, sin extender la aprobación a pérdidas, accidentes ni otros permisos administrativos. Sus criterios CP-MAN-04-01 a CP-MAN-04-10 están documentados, no ejecutados. No se modificaron Java ni la base de datos.

Las reglas aprobadas se vincularán con entidades, operaciones y pruebas durante la implementación. El usuario eligió después Mercado Pago como pasarela; su modalidad, integración y la definición de hardware siguen pendientes. Las aprobaciones de negocio no prueban que esos servicios ya estén integrados.

El 28 de octubre el usuario aprobó expresamente los puntos 1 a 6 de RN-BIC-02: permisos administrativos, protección de viajes, cancelación administrativa de una reserva aún no retirada con anticipo e historial conservados, y capacidad basada en presencia física incluso tras una baja. No aprobó posponer pérdidas/accidentes: solicitó evaluar el efecto de aplazarlos y una alternativa para definirlos ahora. Se documentó RN-INC-01 como propuesta independiente, sin aprobar nuevos estados, tablas, cargos o cierre excepcional. Las aprobaciones administrativas no equivalen a implementación.

Posteriormente autorizó incorporar RN-INC-01, condicionado a analizar su coherencia con la visión y con el servicio cotidiano. Se releyeron los originales y se contrastó el patrón de reporte/atención con ayudas oficiales de operadores, sin copiar sus condiciones comerciales. Quedan aprobados para el diseño INCIDENCIA, EXTRAVIADA, CERRADO_POR_INCIDENCIA, el corte validado y la recuperación sin duplicar uso o dinero. El análisis detectó un punto adicional que no se da por aprobado: esperar la validación económica antes de liquidar si la bicicleta se devuelve con una incidencia previa pendiente. Está documentado y ejemplificado en la sección 8 de RN-INC-01. No se considera completamente cerrada esa precisión ni se implementó Java o una base de datos.

Después confirmó expresamente «Sí, esperar la revisión antes de liquidar»: una devolución con incidencia previa pendiente se registra inmediatamente y no sigue acumulando tiempo; el ajuste final espera la validación del corte. Se aprobó la sección 8 de RN-INC-01 y se actualizaron las reglas de costo y SIN_LIQUIDAR. El bloque de reglas de incidencias queda cerrado a nivel de negocio, sin confundirlo con implementación, pruebas, diseño físico o integración.

Se mantienen intactos el PDF y el DER v4 originales. Aún no se aprobó un DER físico definitivo ni se migró una base de datos.

El usuario ratificó nuevamente que deben tomarse como base su modelo original, DERv4.txt y todas las decisiones de refactorización. El documento 22 identifica la correspondencia entidad por entidad y distingue esas decisiones de propuestas técnicas nuevas. La tarifa vigente queda por TIPO_BICICLETA, con snapshot por detalle; no se tocó el precio por unidad de la aplicación anterior.

El 28 de octubre confirmó que se piensa integrar Mercado Pago y desarrollar con sus herramientas y dinero de prueba. Se asentó en el documento 23, manteniendo las reglas y los registros propios de PAGO, REEMBOLSO e INCIDENCIA. Un adaptador local que cumpla provisionalmente el contrato de pagos sigue siendo una propuesta para más adelante, no autorización para implementarlo ahora ni una integración ya realizada. No se abrieron cuentas ni se solicitaron credenciales.

El usuario remarcó después que el profesor exige movimientos y fechas, no solo el estado actual. Se verificó que el primer borrador de 24 entidades declaraba auditoría genérica pero no historiales dedicados para PAGO/REEMBOLSO, y se documentó la propuesta 24. Luego autorizó expresamente incorporar HISTORIAL_ESTADO_PAGO e HISTORIAL_ESTADO_REEMBOLSO con sus campos, claves y relaciones. En esa incorporación el Mermaid pasó a 26 entidades y 49 relaciones, conservando las 24 entidades previas. Esto registra la aprobación y el cambio del DER lógico, no tablas creadas ni trazabilidad ya funcionando.

Después autorizó revisar PERSONA, USUARIO, ROL y USUARIO_ROL y confirmó documento obligatorio al registrarse. Finalmente aprobó las tres incorporaciones del documento 25: TIPO_DOCUMENTO y PAIS; documento compuesto tipo/país/número único, admitiendo DNI o pasaporte; y nickname único para el login, manteniendo email único por separado. En ese paso el Mermaid quedó en 28 entidades y 51 FK/relaciones. Solo cambiaron PERSONA y USUARIO, se añadieron los dos catálogos y se mantuvieron las otras 24 entidades y 49 relaciones anteriores. No se implementaron todavía ese login, los roles múltiples ni TECNICO.

El usuario autorizó continuar la depuración del resto. El documento 26 revisa estaciones, tipos y bicicletas contra las fuentes y las reglas aprobadas. Después aprobó expresamente el recorrido de baja de estación, condicionado a que sea lógica y nunca física. Se registra RN-EST-01: ADMIN, motivo, sin stock físico ni retiros pendientes, conservando fila, referencias y auditoría. Se aclaró borrado_estacion en el Mermaid sin modificar claves, relaciones ni el total de 28 entidades. No se implementó ni probó todavía en Java o en la base de datos.

Luego autorizó continuar con mantenimiento. El documento 27 contrasta las fuentes originales con las reglas aprobadas y distingue historial de estados de reasignaciones y cambios de trabajo/costos, cuya trazabilidad debe registrar la auditoría. El usuario aprobó diagnostico separado, obligatorio al finalizar y no al crear, y moneda ARS para repuestos internos, sin cargos automáticos al cliente. Se añadió diagnostico y se precisaron costo_repuestos/moneda_repuestos; en ese paso permanecieron 28 entidades y 51 relaciones. No se implementó ni probó todavía ese comportamiento en Java o en la base.

Después solicitó análisis y devolución de alquiler y detalles. El documento 28 distingue recepción física, fin facturable, cierre y liquidación. Luego aprobó expresamente sus dos refuerzos: UQ_ALQUILER_BICICLETA sobre el par alquiler_id/bicicleta_id y tipo_bicicleta_contratado_id como FK obligatoria al catálogo de tipos. Se incorporaron al Mermaid; solo cambió ALQUILER_DETALLE y se añadió una relación, conservando las otras 27 entidades y 51 relaciones anteriores. El total actual es 28 entidades y 52 FK/relaciones. Redondeos, selección de unidades, cotización e integración siguen pendientes; no se ejecutaron SQL ni pruebas de negocio.

Luego se propuso RN-ALQ-10 y se contrastó con el cálculo anterior. El usuario autorizó adoptar la mejora: segundos completos, sin mínimo de uso ni bloques, USD referencial y ARS calculados de forma independiente antes de redondear, dos decimales con HALF_UP por detalle y total como suma de líneas. Se reflejó en los comentarios de duración e importes del DER, sin cambiar sus campos, tipos, claves o relaciones. Los ejemplos tienen verificación aritmética independiente; no se modificó Java ni SQL ni se operó dinero. La implementación corresponde al backend refactorizado, no se declara aplicada al servicio antiguo.

Al solicitar qué queda por cerrar del DER, se releyeron los originales y se documentó RN-ALQ-11 como propuesta: una estación de retiro por alquiler, entre una y cinco bicicletas al confirmar con máximo configurable y selección fija tras confirmar, conservando las cancelaciones aprobadas. Bajo el origen único se propone trasladar la FK de retiro del detalle a la cabecera. No se aplicó ese cambio ni se aprobaron límites por usuario, duración o tratamiento del anticipo en proceso. El documento 30 distingue los cierres estructurales de la configuración e integración que pueden completarse por módulos.

Después el usuario aprobó RN-ALQ-11, incluido el traslado de la estación de retiro a la cabecera, e indicó un máximo acordado con su compañero de diez bicicletas. Se adoptó diez en sustitución de la propuesta inicial de cinco, manteniendo la configuración del servicio y la disponibilidad real como condiciones diferentes. Se trasladaron estacion_retiro_id y su relación a ALQUILER en el Mermaid; ALQUILER_DETALLE conserva la devolución individual. Se actualizaron referencias, justificación y criterios de aceptación, sin nuevas entidades ni FK: permanecen 28 y 52, respectivamente. Permitir varios orígenes sería otra regla válida, no implementada; bajo la regla vigente otros puntos requieren otra contratación. Los límites acumulados por usuario, duración y anticipo en proceso siguen expresamente pendientes. No se modificaron Java, datos, originales ni GitHub.

El 1 de noviembre el usuario autorizó continuar la revisión. Se contrastaron las fuentes originales, el DER financiero y documentación oficial actual de Mercado Pago. El documento 31 propone cancelar unidades sin alterar el anticipo en proceso, conciliar su resultado y calcular un nuevo intento sobre unidades aún reservadas solo tras un rechazo/cancelación confirmados. Propone además dos entidades de referencias para medios guardados, con claves compuestas y FK opcional desde PAGO; todavía no fueron aprobadas ni incorporadas. Los controles de reembolso reservan importes pendientes para evitar duplicados. No se eligió un checkout, no se conectó una cuenta, no se procesó dinero y no se modificaron Mermaid, Java o MySQL.

Después de aclarar la diferencia entre editar el carrito y cancelar durante un cobro ya enviado, el usuario aprobó RN-PAG-03. Confirmó además pagar en la pantalla de Mercado Pago sin pedir ni administrar tarjetas en Turismo Sustentable. Se registró RN-PAG-05 en el documento 32 y se descartó RN-PAG-04 de medios guardados; las dos entidades nunca se agregaron. Se aclaró el cambio de alcance frente al PDF sin editar los originales y se reflejaron las decisiones en comentarios del Mermaid, conservando sus 28 entidades y 52 FK/relaciones. La experiencia elegida corresponde a Checkout Pro; su variante de API y correlaciones requieren revisión antes de cerrar el diseño físico. No se modificaron Java/MySQL ni se probaron pagos reales o de prueba.

Luego autorizó analizar el bloque de incidencias, reportes y órdenes. El documento 33 contrasta sus fuentes y encuentra que el detalle carece de una referencia directa a la incidencia que fundamenta el corte y que las FK operativas a órdenes no conservan por sí solas sus asociaciones anteriores. Se proponen incidencia_corte_id e HISTORIAL_ATENCION_MANTENIMIENTO, todavía no aprobados ni incorporados. La auditoría completa podría cubrir los cambios sin otra tabla; el historial se recomienda para relaciones históricas explícitas. No se modificaron Mermaid, Java, MySQL ni los originales ni se ejecutaron pruebas de negocio.

El 2 de noviembre, después de aclarar el recorrido habitual de una sola orden y la excepcionalidad de cancelarla/reemplazarla, el usuario descartó por ahora el historial de atención para no añadir complejidad. En ese momento D07-A seguía pendiente y el documento 34 revisó la estructura de 28/52. La recomendación posterior de auditoría general se retiró al confirmar el usuario su alcance exclusivamente ADMIN.

Después aprobó expresamente las cuatro mejoras resumidas del documento 35. Se registra RN-TRZ-01 en el documento 36: HISTORIAL_INCIDENCIA; ampliación y renombre a HISTORIAL_ORDEN_MANTENIMIENTO; motivo en auditoría; e incidencia_corte_id opcional con FK/UK al mismo detalle. El DER pasa a 29 entidades y 55 FK/relaciones, sin reactivar D07-B ni tocar las otras 25 entidades. D07-A queda aprobado, no pendiente de otra confirmación. Las reglas, permisos y precisiones físicas restantes siguen delimitados; no se modificaron Java, MySQL, frontend, originales ni GitHub.
