# Revisión de coherencia de la migración al DER v5

Fecha: 11 de noviembre de 2026.

Actualización financiera del 15 de noviembre (etapas 278-298): cinco entidades y
cinco repositorios financieros, precarga, saldo simulado, inicio y resolución del
anticipo con permisos/HTTP e historiales incorporados. Tanda anterior corroborada:
141 casos unitarios/MVC, sin fallos, errores u omisiones. Etapa 297 aplica por delegación puntual
los cinco casos de integración propuestos en etapa 296, conservando los siete
anteriores. Etapa 298: usuario ejecuta parte 5, BUILD SUCCESSFUL en 27 s y XML
corroborado con doce pruebas, cero fallos/errores/omisiones. Incluye persistencia,
historiales, repetición/concurrencia con misma clave, rollback de la transacción
envolvente explícita, aprobación tardía con obligación pendiente y rechazos de
UK/FK reales, en los escenarios preparados. No es circuito completo ni integración
con dinero real. Continúan pendientes
adicionales/deuda, ejecución de reembolsos, retiro/devolución nuevos y conexión del
circuito completo. No cierra R02 por tener modelos o superar pruebas unitarias.

Etapa 300: retiro por detalle aplicado por delegación puntual, en los seis
archivos de etapa 299, conservando productores anteriores. CLIENTE propietario,
rol vigente, cobertura neta, plazo original, retiro independiente, estados y
dos historiales con hora común. Sin cambios de entidades/FK del DER; constructor
de historial ampliado conservando llamadas antiguas. Parte 5 y pruebas nuevas
reservadas al usuario. No se considera verificado por la ejecución MySQL previa,
no prueba candado físico ni completa devolución/vencimiento/liquidación/frontend.

Etapa 301: parte 5 ejecutada por el usuario, BUILD SUCCESSFUL en 16 s. Ocho XML
corroborados del 15/11 a las 23:30 de Argentina: 164 tests, cero fallos/errores/
omisiones. Compilación y regresión anterior favorables, no cobertura específica
de retirarBicicleta ni nuevo ensayo MySQL. Los tests previos que mencionan retiro
en AlquilerServiceTest corresponden al recorrido anterior; no confundir sus
nombres con pruebas del método nuevo. Siguen pendientes sus pruebas propias.

Etapa 303: RetiroAlquilerServiceTest incorporado manualmente y 26 casos aprobados.
Nueve XML del 15/11 a las 23:56 de Argentina corroborados: 190 tests sin fallos,
errores u omisiones (26 nuevos + 164 de regresión). Se acreditan los escenarios
unitarios propuestos del retiro, con repositorios/trazabilidad simulados y Clock
controlado; no persistencia, filtros HTTP, bloqueos ni rollback reales del retiro.
Siguiente paso: pruebas del POST y después integración MySQL; R01/R02 no se cierran
ni se retira el legado por esta tanda de tests.

Etapa 305 (16/11): RetiroAlquilerControllerTest incorporado manualmente y sus
18 casos MVC aprobados. Diez XML corroborados a las 00:08 de Argentina, 208 tests
sin fallos/errores/omisiones. Acredita permisos HTTP, serialización y traducción
de errores en el slice con servicio simulado, junto con la regresión anterior.
No acredita sesión de navegador, datos reales de propietario/pago, transacción
o persistencia MySQL del retiro. Siguiente paso: integración SQL propia; R01/R02
permanecen abiertos hasta completar circuito y retirada coordinada del legado.

Etapa 307: ampliación de AlquilerMysqlIntegrationTest aplicada por delegación
puntual, con cinco casos nuevos de retiro y cuatro helpers de comprobación.
Helper de concurrencia generalizado y sus dos llamadas anteriores actualizadas,
sin duplicar infraestructura. Diecisiete @Test presentes; parte 5 de compilación
y ejecución reservada al usuario, sin nueva evidencia SQL del retiro todavía.
Producción, configuración, frontend, Gradle/Playwright y suites anteriores intactos
en esta etapa. R01/R02 permanecen abiertos; no confundir tests agregados con aprobados.

Etapa 308: integración SQL ejecutada por el usuario, 17 casos, 14 aprobados y tres
fallos por pérdida de fracciones de segundo entre DTO y lectura persistida. No
son rechazos UK/FK deliberados ni prueba de duplicación por sí mismos. Diagnóstico
con fuentes locales de Hibernate/Connector-J y saludo de servidor sin autenticar
ni ejecutar SQL: MariaDB 10.4.32 anunciado 5.5.5-10.4.32-MariaDB, tratado por el
driver MySQL como anterior al soporte de fracciones. Dialecto actual ya genera
datetime(6); agregar columnDefinition o truncar expectativas no ataca esa causa.
Propuesta manual en tres archivos: dependencia MariaDB gestionada por Boot,
conexión/dialecto del perfil refactor y URL explícita del test. No implementada
ni verificada aún. Se conserva configuración base/Playwright y assertions; R01/R02 abiertos.

Etapa 309: corrección de conexión aplicada por autorización puntual en los tres
archivos previstos. Dependencia MariaDB sin versión explícita (gestionada por
Boot), URL/driver/dialecto en refactor y URL explícita del test. Sin modificar
sus diecisiete cuerpos de prueba, reglas, modelos, claves o comparaciones.
application.properties, configuración Java/Playwright y resto de Gradle conservados.
Parte 4 a cargo del usuario: todavía no se resolvió/ejecutó el conector ni se
repitió SQL; no dar por corregidos los tres fallos antes de comprobarlo.

Etapa 310: parte 4 SQL ejecutada por el usuario y XML corroborado, 17 tests sin
fallos/errores/omisiones. Log confirma MariaDB103Dialect y conector 3.0.10; fuente
del test comparada con snapshot anterior: solo cambia URL, sin relajar assertions.
Los tres fallos de precisión quedan resueltos en esta tanda, que también acredita
concurrencia e idempotencia, ocupación, historias y rollback interno del retiro
en los escenarios preparados. Avisos DDL de arranque son DROP FK sobre tablas
inexistentes con create-drop, no fallos de creación ni del test. No equivale a
DER completo, navegador, devolución o liquidación; R01/R02 siguen abiertos.

Etapa 311: devolución individual incorporada con autorización, sin ejecutar
compilación, tests, app o SQL. Usa el detalle existente del DER, tarifa/cotización
congeladas y recepción real; distingue demora/falla sin agregar estados. Respeta
capacidad y permisos vigentes CLIENTE propietario/ADMIN receptor, sin habilitar
al ADMIN solo a alquilar o retirar. Falla, incidencia técnica o preventiva vencida
mantienen MANTENIMIENTO con una orden abierta vinculada y sin asignación ficticia.
Una revisión económica pendiente no impide recibir ni prolonga el contador:
subtotales reales quedan nulos hasta decidir. La última devolución puede cerrar
el uso de cabecera, pero no fija totales ni fecha de liquidación ni mueve dinero.
Esta separación es un paso intermedio explícito, no una liquidación terminada.
Pruebas unitarias/MVC añadidas pendientes del usuario; SQL de devolución y conexión
del frontend pendientes. El programador preventivo y el módulo de revisión/asignación
de órdenes/incidencias no se consideran implementados por este enlace. No cambian
tablas, claves, columnas ni Mermaid/DERv5.txt; R01/R02 y sus dependencias siguen abiertos.

Etapa 312: usuario informa BUILD SUCCESSFUL en 16 s; diez XML corroborados del
16/11 a las 01:37 de Argentina, 182 tests aprobados sin fallos/errores/omisiones.
Devolución nueva: 33 unitarias y 20 MVC. Otras 129 son regresión de retiro,
reserva, anticipo, autorización, flujo previo y modelos de mantenimiento.
Compilación y escenarios unitarios/HTTP de devolución comprobados; todavía no
su integración SQL ni locks/rollback/concurrencia reales, frontend o liquidación
conjunta. No se suman a tandas anteriores como si fueran una misma ejecución.
Sin corrección Java requerida por esta salida; solo seguimiento documental.
Siguiente comprobación: persistencia SQL de devolución y sus efectos sobre
ocupación/historias/órdenes, antes de dar por cerrado ese recorrido. R01/R02 abiertos.

Etapa 314: usuario delega incorporación del bloque SQL propuesto. Solo se modifica
AlquilerMysqlIntegrationTest dentro de src: dos imports, seis tests y cinco
auxiliares. Comparación con snapshot anterior (normalizando finales de línea):
al retirar exclusivamente ese bloque y los imports, el contenido coincide íntegro.
Sus diecisiete pruebas, configuración y auxiliares previos no se reemplazan.
Recuento estático 23 tests, métodos nuevos únicos, delimitadores equilibrados y
diff --check sin errores; NO acredita compilación o ejecución. Parte 4 reservada
al usuario con app detenida y datos descartables en turismo_sustentable/create-drop.
Sin cambio productivo o financiero, nuevas dependencias, SQL ejecutado ni Git de
escritura. R01/R02 siguen abiertos y las seis pruebas nuevas siguen pendientes.

Etapa 315: ejecución del usuario corroborada en XML AlquilerMysqlIntegrationTest,
23 tests, 0 failures/errors/skipped, timestamp 2026-11-16T04:54:33.231Z (01:54 de
Argentina). Seis devoluciones nuevas y diecisiete casos previos PASSED en esta
tanda; fuente del test anterior a la ejecución. Usuario informa BUILD SUCCESSFUL
en 26 s, cuatro tareas ejecutadas. No requiere corregir producción o assertions.
Recepción, costos congelados y persistencia de fechas/ocupación/historias, una
correctiva para reintentos concurrentes, preventiva, rollback después de flush,
destinos rechazados e incidencia económica pendiente cubiertos por los casos.
Proveedor financiero sigue simulado, Clock/cotización controlados; no se acredita
dinero real, flujo completo de incidencias/mantenimiento ni navegador. R01/R02
permanecen abiertos. Siguiente bloque: liquidación conjunta, sin confundir cierre
operativo con dinero saldado ni adelantar reembolsos por cada devolución.

Etapa 321: servicio de liquidación y DTO creados manualmente y compilados en
etapas 317/319; su test manual ahora corroborado en XML. Cinco suites suman 137
pruebas aprobadas sin fallos/errores/omisiones (16/11, 02:43 de Argentina):
37 nuevas de liquidación/consulta y 100 de regresión. Se verifica suma de importes
guardados/centavos, idempotencia, pendientes, dinero confirmado y permisos vigentes
con repositorios simulados. No demuestra MANDATORY, snapshot/locks o rollback SQL
de la liquidación, ni consulta HTTP o activación dentro del circuito.
Sin corrección Java por esta tanda; siguiente conexión debe incluir cierre backend
sin segunda llamada obligatoria del navegador y bloqueo coherente por deuda,
incluido el productor anterior hasta retirarlo. R01/R02 siguen abiertos. Adicional,
procesamiento de reintegros, vencimientos/cancelaciones y frontend pendientes.

Etapa 323: usuario delega el bloque de conexión de etapa 322 y conserva parte 9.
Incorporados ocho archivos: tres productivos (AlquilerRepository/Service/Controller)
y cinco de tests. Liquidación interna llamada bajo cierre de cabecera y transacción
de devolución; deuda derivada de costo final y dinero confirmado bloquea creación
nueva/anterior, sin borrar la obligación por ocultar alquiler. Consulta no se agrega
a devolver/pagar, no se amplían roles ni se genera un cobro/reintegro desde devolución.
Se adaptan stubs y expectativas de integración sin eliminar los casos de uso/historia.
Comparación de 211 hashes src: solo esos ocho cambian, sin altas/bajas. Revisión
estática de métodos y diff --check completada; NO compilado ni ejecutado por asistente.
Estado actual: parte 9 del usuario pendiente, SQL de la conexión/deuda aún no probado.
R01/R02 siguen abiertos; consulta HTTP financiera, pagos adicionales, ejecución de
reintegros y otros recorridos pendientes no se consideran terminados por este cambio.

Etapa 324: parte 9 del usuario corroborada en ocho XML, 176 tests sin
fallos/errores/omisiones, 16/11 a las 03:19 de Argentina. Nuevas negativas de
deuda en productores anterior/v5 y HTTP 409 aprobadas; caso de devolución final
verifica delegación con cabecera cerrada. También pasan liquidación/consulta y
regresión acotada. La tanda prueba lógica/HTTP con mocks, no la consulta nativa
o MANDATORY/locks/rollback del circuito en base real. No corregir ni relajar
assertions por esta salida; siguiente comprobación SQL de conexión y deuda.
R01/R02 siguen abiertos: no se completa consulta financiera HTTP, cobro adicional,
reintegro, vencimientos/cancelaciones o frontend por aprobar esta regresión.

Etapas 326/327: ampliación acotada del test SQL incorporada por delegación y
parte 5 ejecutada por el usuario. XML del 16/11 a las 03:57 de Argentina:
29 pruebas aprobadas, cero fallos/errores/omisiones; fuente idéntica a la
implementación y anterior al reporte. Ahora se verifican en base real consulta
nativa de deuda (incluida cabecera oculta), alta v5/anterior bloqueada, saldado y
reembolso pendiente sin bloqueo, liquidación única con últimas devoluciones
concurrentes, rollback después del flush de liquidación y MANDATORY efectivo.
La revisión económica pendiente impide liquidar incluso tras devolver ambas
bicicletas. Los 23 casos anteriores también aprueban. No hace falta corregir
Java por esta salida. No implica HTTP/frontend financiero terminado ni ejecución
de adicionales o reintegros; R01/R02 siguen abiertos. Próximo bloque: consulta
financiera HTTP con permisos y pruebas, manual salvo delegación expresa.

Etapa 329: consulta financiera HTTP aplicada por delegación en siete archivos
propuestos, sin clases nuevas. AlquilerController expone el GET y delega al
servicio existente; WebAuthorization permite la ruta precisa a CLIENTE/ADMIN.
Propiedad, cuenta activa y roles vigentes siguen en el servicio. Cinco contextos
MVC adaptados y 17 ejecuciones HTTP nuevas en el test de consulta existente;
métodos anteriores conservados. Parte 5 del usuario aún pendiente: revisión
estática favorable, sin compilar o ejecutar nuevas pruebas/app/SQL. Las 29 SQL
anteriores no verifican este endpoint posterior. No se completa frontend,
adicionales/reintegros ni corte del legado; R01/R02 continúan abiertos.

Etapa 330: parte 5 del usuario corroborada en siete XML del 16/11 a las 04:13
de Argentina: 154 aprobadas sin fallos/errores/omisiones. Consulta HTTP tiene
43 casos (26 anteriores y 17 nuevos); cinco contextos MVC afectados, seguridad
y servicio de liquidación pasan. Fuentes idénticas a etapa 329 y previas al
reporte; no requiere corrección Java. Se acredita ruta, contrato JSON/permisos
HTTP y regresión del filtro, no nueva integración SQL ni navegador o movimientos
financieros. Consulta HTTP queda verificada; frontend, adicionales/reintegros
y demás operaciones/corte de legado siguen pendientes. R01/R02 no se cierran.

Etapa 331: adicional del saldo liquidado aplicado por delegación, aún sin ejecutar
pruebas por el asistente. PagoService reutiliza resumen financiero, ledger y
trazabilidad; namespace ADICIONAL con UUID y bloqueos cuenta/cabecera/pago. Solo
CLIENTE titular puede iniciar/resolver, incluso para deuda de cabecera oculta;
ADMIN solo no. No se toma monto del navegador ni se modifican uso, fechas/costos
finales o tarifa/cotización. Rechazado/pendiente conserva deuda; aprobado la reduce
sin otro estado financiero persistido. CREADO desactualizado puede cancelarse sin
envío; resultado pendiente en gestión se concilia sin ocultar dinero confirmado.
Dos POST, DTO de respuesta efectivamente usado, 46 unitarias/22 HTTP/cinco SQL
nuevas preparadas (34 SQL esperadas con las anteriores). Constructor y fixtures
adaptados conservando métodos del anticipo. Guardar/compilar/probar reservados al
usuario; no hay nueva evidencia PASSED. Reintegros, frontend y demás operaciones
y corte del legado siguen pendientes; R01/R02 permanecen abiertos.

Etapa 332: usuario confirma guardado, compilación y todas las pruebas aprobadas;
última salida BUILD SUCCESSFUL en 30 s. XML actual del 16/11 a las 04:48 de
Argentina: 34 SQL aprobadas, cero fallos/errores/omisiones. Incluye cinco nuevas
del adicional y 29 previas: saldar deuda oculta permite reservar, rechazo/pendiente
conservan deuda y reintentos seguros, concurrencia sin duplicados y rollback tras
confirmación/historia. Diez fuentes idénticas a etapa 331 y previas al reporte.
No correcciones Java requeridas. Unitarias/MVC aprobadas según el usuario; sus
XML no se conservan tras la ejecución SQL, por lo que no se declara otro total
local corroborado. Pendientes reintegros, consumidor frontend, demás operaciones
y corte del legado. R01/R02 continúan abiertos.

Actualización de implementación (etapas 253/254): R04, R05 y el permiso visual
de CLIENTE de R06 aplicados por autorización expresa y verificados con pruebas
dirigidas. XML: 204 pruebas Java aprobadas, tres MySQL omitidas; salida aportada
de Node: 22 aprobadas y check sin error. No nueva prueba SQL o de navegador manual.
El diagnóstico inferior conserva lo encontrado y su criterio de cierre; los
demás hallazgos y la presentación de importes/fechas de R06 siguen pendientes.

Decisión posterior (etapa 255): usuario aprueba compatibilidad temporal de
lectura ANTERIOR/DETALLES, sin fabricar datos históricos. Retiro obligatorio al
completar el reemplazo del alta/devolución; no convertirlo en dos circuitos
definitivos. Etapa 256: consultas/servicio aplicados por delegación puntual,
pendientes de compilación y pruebas; endpoints/pantallas aún sin modificar.
Checkpoint anterior guardado en commit local ab05fb3, sin push. Esta preparación
del backend no cierra R01 ni habilita el nuevo alta HTTP. Etapa 257: usuario
informa compileJava exitoso; revisión estática de fuentes sin correcciones.
Preparadas instrucciones para ampliar ConsultaAlquilerServiceTest, todavía
sin implementación o ejecución de esas pruebas de listado acreditadas.

Etapa 258: por delegación puntual se agregan esas pruebas en la misma clase:
siete métodos, nueve casos contando variantes parametrizadas. Revisión estática
del diff y referencias, sin ejecución de Gradle por el asistente. Parte 3 a cargo
del usuario. R01 sigue abierto; las pruebas todavía no acreditan resultados ni
validación SQL, seguridad sobre proxy o conexión de endpoints/pantallas.

Etapa 259: parte 3 ejecutada por el usuario, XML corroborados: cuatro suites y
80 casos aprobados, sin fallos, errores u omisiones (26 consulta de servicio,
12 consulta HTTP por ID, 21 reserva y 21 autorización web). Los nueve nuevos
casos de listado están incluidos; no son 80 casos específicos de listado.
El servicio de lectura queda probado en ese alcance, pero R01 permanece abierto:
los GET de listado y sus pantallas siguen utilizando el mapeo anterior. No se
ejecutaron SQL, aplicación o pruebas nuevas por el asistente en esta verificación.

Etapa 260: preparadas instrucciones manuales para delegar ambos GET en el
servicio y adaptar las dos pantallas conjuntamente. Se conserva la URL y se
retiran dependencias directas de repositorios sin uso en los controladores.
Presentación compartida mediante alquileres-vista.js consumido por ambas
pantallas: textos escapados, fechas ausentes sin inventar, estimado/final
diferenciados y detalles de bicicletas. Fechas nuevas UTC según TiempoConfig;
las anteriores conservan interpretación previa. Sin aplicar Java/JS/HTML aún.
Regresión, pruebas específicas de los GET de listado y presentación, integración
SQL y comprobación manual pendientes. No declarar R01 cerrado por este plan.

Etapa 261: usuario delega el bloque puntual y reserva parte 5. Aplicada la
conexión de ambos controladores, scripts de página y HTML, y creado
alquileres-vista.js compartido. Retiradas inyecciones/imports y mapeos directos
de repositorios sin uso en esos controladores. Métodos de creación, borrado,
consulta por ID, devolución, cotización y resumen estadístico conservados.
Revisión estática de diff, referencias y segmentos conservados; no se ejecutan
compilación, pruebas, Node, aplicación o SQL. R01 sigue abierto hasta verificar
contratos HTTP, presentación, integración y prueba manual. Los 80 casos de la
etapa 259 no acreditan por sí solos esta conexión nueva.

Etapa 262: parte 5 ejecutada por el usuario, salida en adjunto corroborada con
XML actuales. Cuatro suites Java, 80 casos sin fallos/errores/omisiones; salida
Node con tres check sin error y 37 casos aprobados de inventario/sesión.
No son pruebas específicas de GET de listado, render nuevo o SQL: siguen
pendientes. No se ejecutaron pruebas adicionales ni se modificó fuente en
esta verificación. El resultado es regresión dirigida favorable, no cierre R01.

Etapa 263: confirmado el propósito del bloque: implementar lectura usable de
ALQUILER/ALQUILER_DETALLE del DER v5, preservar snapshots y diferenciar estado de
uso de liquidación, no agregar entidades ni reglas nuevas. Se preparan 14 casos
MVC manuales sobre los dos GET de listado, reutilizando la clase de consulta
existente y retirando mocks de repositorios ya ajenos a los controladores.
Servicio y estadísticas simulados; seguridad HTTP y serialización reales del
slice MVC. No acredita SQL, filtrado real por propietario ni proxy del servicio.
Pruebas de presentación y SQL siguen pendientes; no se edita Java en esta etapa.

Etapa 264: usuario delega únicamente el bloque MVC propuesto y reserva parte 3.
ConsultaAlquilerControllerTest incorpora los dos controladores, mock de
EstadisticasService y 14 casos de listado; retira mocks/imports de repositorios
que ya no son dependencias de los controladores. Pruebas por ID y su fixture
comparados con HEAD e intactos. Solo ese test y seguimiento Markdown cambiados
en esta etapa, sin ejecutar Gradle/Node/app/SQL ni modificar producción.
Casos nuevos agregados pero no acreditados como aprobados; R01 sigue abierto.

Etapa 265: parte 3 ejecutada por el usuario, XML actuales corroborados: cuatro
suites y 94 casos aprobados sin fallos, errores u omisiones (26 consulta MVC,
26 consulta de servicio, 21 reserva y 21 autorización web). Los 14 casos nuevos
de ambos GET de listado están aprobados en alcance MVC con servicio simulado.
Sin cambios de fuente o ejecuciones adicionales por el asistente. Presentación
específica, SQL y prueba manual siguen pendientes: no se considera R01 cerrado.

Etapa 266: preparado bloque manual de 15 pruebas Node de presentación en un
solo archivo, ListadoAlquileres.test.cjs. Carga scripts reales de vista compartida
y página cliente/admin con DOM, sesión y HTTP simulados, sin nuevas dependencias.
Cubre formatos, agrupación, moneda/importes persistidos y ausencia de cero
ficticio, fechas UTC/ausentes, acciones anteriores, errores y textos escapados.
No acredita maquetado visual, seguridad real de sesión, SQL o devoluciones reales.
Aún no creado o ejecutado: no modificar el estado de R01 por estas instrucciones.

Etapa 267: usuario delega solo el archivo de pruebas de presentación y reserva
parte 3. Creado ListadoAlquileres.test.cjs con los 15 casos propuestos; scripts
reales y entorno simulado, sin modificar producción ni los arneses existentes.
Revisión estática de contenido, rutas y referencias. No se ejecutó Node/check,
Gradle, aplicación o SQL; casos agregados, no aprobados. R01 permanece abierto.

Etapa 268: usuario ejecuta parte 3 y aporta salida Node: 52 casos aprobados,
cero fallos, cancelaciones u omisiones. Incluye 15 de listados, 15 de inventario
y 22 de sesión. Queda verificada la presentación en el entorno simulado descrito,
no el aspecto visual o la red/sesión/SQL reales. Solo seguimiento documental en
esta verificación, sin repetir ejecuciones. R01 conserva integración MySQL y
comprobación manual pendientes; la suite SQL actual aún no prueba los listados.

Etapa 269: preparada ampliación manual de la suite MySQL existente, cuatro casos
de listado/propiedad/borrado/orden, consulta administrativa, agrupación y snapshots,
y permisos sobre servicio real con sesión y roles persistidos diferenciados.
Reusar reserva real y crear snapshots anteriores terminales de prueba, incluidos
borrados lógicos, sin implementar cancelación/devolución o administración de roles.
Preparación por transacciones confirmadas y lecturas fuera de transacción de test,
con open-in-view=false. No asumir tabla vacía ni orden de métodos en lista ADMIN.
La suite conserva create-drop en turismo_sustentable y opt-in: aplicación detenida,
MySQL local activo y BD descartable. No se modifica o consulta ahora la BD.
Fuentes sin cambiar; instrucciones pendientes de aplicar y ejecutar por el usuario.

Etapa 270: usuario delega solo la ampliación propuesta y reserva parte 3.
Aplicados cuatro casos y cuatro auxiliares en AlquilerMysqlIntegrationTest con
sus imports; configuración, preparación/limpieza, tres casos anteriores y sus
auxiliares comparados con HEAD e intactos. No cambios de producción o datos.
Solo revisión estática y documentación; sin Gradle/Node/app/HTTP/SQL o Git de
escritura. Los siete casos siguen pendientes de ejecución real con opt-in;
no se acreditan aprobados por esta edición. R01 continúa abierto.

Etapa 271: usuario completa compileTestJava y ejecución opt-in MySQL. XML actual
AlquilerMysqlIntegrationTest con siete casos, cero fallos/errores/omisiones,
timestamp 2026-11-11T20:24:33.153Z. Incluye cuatro casos nuevos de listado y tres
anteriores de reserva/consulta por ID/rollback. En el alcance preparado quedan
comprobados filtros/orden, ambos formatos y snapshots, roles de sesión frente a
BD y cuenta activa sobre servicio real con open-in-view=false. No es una auditoría
global de seguridad o concurrencia. Ejecución separada de los 94 Java y 52 Node
anteriores; falta comprobación manual de pantallas para cerrar esta parte R01.
Sin cambios de fuente o ejecuciones adicionales del asistente en la verificación.

## 1. Alcance y conclusión

Revisión estática del Mermaid v5 completo, su entregable TXT, reglas aprobadas
de estados, composición, pagos y auditoría, entidades/repositorios y consumidores
actuales de usuarios, estaciones, bicicletas y alquileres. Se rastrearon referencias
Java y frontend. No se modificó Java, no se ejecutaron pruebas ni se consultó la
base en esta revisión. No es una auditoría exhaustiva de seguridad o concurrencia.

La estructura nueva es coherente con el diseño aprobado, pero la migración sigue
incompleta. No se puede declarar todo el backend conforme al DER ni sustituir
únicamente los listados: creación, devolución, estadísticas e inventario todavía
contienen consumidores del modelo anterior.

El Mermaid y `docs/practica-3/entregables/DERv5.txt` tienen el mismo SHA-256:
`733AD851E7E1A943AA03B73D186C7B902CC18D567082707D30F4D550CFC38B0E`.
Se mantienen 29 entidades y 55 relaciones del diseño. Tener 24 clases @Entity
correspondientes no acredita conformidad completa de campos, restricciones o reglas.

## 2. Qué está comprobado y qué no

| Apartado | Evidencia disponible | Límite |
| --- | --- | --- |
| Identidad, roles y registro | Persona, catálogos, UsuarioRol y autenticación por nickname; regresiones registradas | No significa que todas las operaciones de ADMIN/TECNICO estén implementadas |
| Inventario y estaciones | Servicios, validaciones, capacidad, versión, estados/borrado/presencia separados | Integración con detalle activo, incidencias y preventivo no terminada |
| Reserva nueva | Cabecera, detalles, tipo/tarifa/cambio congelados e historias en una transacción; integración MySQL previa | Método interno, sin alta HTTP nueva, deuda o preventivo comprobados |
| Consulta por ID | Servicio propietario/ADMIN, DTO y GET; pruebas unitarias, MVC y lectura MySQL previa | No sustituye listados ni demuestra todas las combinaciones de seguridad sobre proxy real |
| Borrado lógico de alquiler | Guardas de estados nuevos y antiguos, con bloqueo y pruebas | Traducción de errores del controlador y auditoría administrativa pendientes |
| Mantenimiento/incidencias | Entidades, catálogos, relaciones e historiales preparados | No hay todavía servicios/controladores de operación completos |
| Finanzas | Base de cinco entidades/cinco repositorios, precarga y proveedor simulado incorporados; 24 unitarias aprobadas el 15/11 | Servicio transaccional, persistencia JPA financiera, permisos/HTTP, frontend y pruebas del circuito pendientes |
| Tarjetas/suscripción | Clases y referencias retiradas; clean y regresiones completadas | No se verificó el esquema físico tras la retirada |

Los 173 casos de regresión y los 21 de reserva posteriores a la limpieza se
ejecutaron en tandas diferentes. Los tres casos MySQL aprobados corresponden
a una ejecución anterior a retirar Tarjeta/Suscripcion. No se presentan como
una única corrida actual ni como prueba completa de la aplicación.

## 3. Hallazgos y acciones de cierre

### R01. Los listados antiguos no admiten nuevas cabeceras

- `AlquilerController.getAlquiler` y `AdminController.getAllAlquileres` devuelven
  `AlquilerDTO` anterior. Su constructor invoca `alquiler.getEstado().name()` y
  `alquiler.getBicicleta().getId()` sin admitir los null de una cabecera nueva.
- Si esos listados reciben una nueva reserva, el mapeo puede fallar. No se afirma
  que se haya reproducido mediante HTTP en esta revisión; se deriva del código.
- `mis_alquileres.js` y la sección alquileres de `admin.js` consumen esos campos
  planos. Cambiar solo la respuesta rompe esas pantallas.
- Cierre: contrato de lectura, autorización de propietario/ADMIN, DTO, controlador,
  frontend y pruebas deben migrarse conjuntamente. No ocultar silenciosamente
  alquileres antiguos ni fabricar detalles/cotizaciones para hacerlos encajar.

### R02. El circuito que crea y finaliza desde HTTP sigue siendo anterior

- POST `/api/alquileres` acepta AlquilerRequestDTO y llama a crearAlquiler: una
  bicicleta, inicio inmediato y enum INICIADO. El método crearReserva no está
  expuesto. La política CLIENTE del POST está corregida; eso no migra el negocio.
- PATCH `/api/alquileres/finalizar/{id}` usa alquiler.bicicleta y el enum anterior,
  consulta cambio al devolver y conserva el cálculo anterior. No finaliza detalles.
- El cliente de cotización antiguo y respaldo 1400 siguen en uso por ese circuito
  y GET `/api/cotizacion`; no eliminarlos dejando llamadas rotas.
- Cierre: anticipo validado, retiro por detalle, devolución y liquidación con
  snapshots, cancelación/vencimiento y efectos de incidencias según sus reglas.
  No activar el POST nuevo saltando deuda, vencimiento preventivo o pago previo.
- El reemplazo de listados debe coordinarse con este productor de datos. Un
  listado que solo incluya filas nuevas mientras el POST produce antiguas no
  es una migración funcional completa.

### R03. Inventario y estadísticas conservan dependencias del alquiler anterior

- BicicletaService.comprobarSinAlquiler consulta únicamente
  existsByBicicletaAndEstado(..., INICIADO). La guarda adicional de estado no
  editable hoy rechaza RESERVADA/EN_USO, pero no reemplaza una comprobación de
  los detalles activos del DER. No se declara una explotación demostrada.
- AlquilerRepository cuenta INICIADO de cabecera y agrupa preferencias por
  bicicleta.tipo actual. Debe consultar detalles y tipo contratado, conservando
  la definición del indicador y sin duplicar totales por joins.
- La relación Bicicleta.alquileres mappedBy bicicleta apunta al FK anterior
  en cabecera: retirarla al retirar ese FK, o reemplazarla solo si hay un consumidor
  real de una colección inversa de detalles. No crear relaciones redundantes.
- Cierre: adaptar consultas/consumidores y sus pruebas antes de eliminar enum,
  columnas y métodos anteriores.

### R04. Limpieza segura identificada, distinta del legado activo

- GeneroUsuario.java es un enum sin referencias externas encontradas en main,
  test o recursos; el catálogo vigente es Genero.
- UsuarioDTO.alquileres y BicicletaDTO.alquileres tienen getter pero no se llenan;
  BicicletaDTO además conserva el antiguo mapeo comentado. No se encontraron
  consumidores Java/JS de esas propiedades aparte de sus propias declaraciones.
- Cierre propuesto: retirar enum, ambos campos/getters y comentario/imports
  sobrantes, probar serialización y regresiones afectadas. No confundirlos con
  AlquilerDTO, que todavía tiene consumidores activos y NO debe borrarse ahora.

### R05. Borrado HTTP confunde errores distintos

- AlquilerController.borrarAlquilerLogico captura Exception y responde 404.
  Eso también atraparía ConfiguracionSistemaException y AccessDeniedException
  del servicio. La protección de estados no corrige esta traducción HTTP.
- Cierre: conservar 403 de seguridad y 500 interno saneado; 404 solo cuando el
  recurso no existe. Agregar casos MVC del DELETE, no reutilizar como evidencia
  las pruebas del GET o de método llamado directamente.

### R06. Presentación de permisos, fechas e importes por actualizar

- index.js define puedeAlquilar como ADMIN o CLIENTE. ADMIN solo puede ver
  controles de alquiler aunque el POST esté correctamente denegado sin CLIENTE.
  Es desalineación de interfaz, no prueba de autorización backend concedida.
- mis_alquileres.js presenta precio null como cero y utiliza Math.max(1, minutos)
  para el texto de devolución. No distingue importe pendiente de importe cero;
  esa presentación no corresponde al cálculo aprobado por segundos sin mínimo.
- Cierre: separar navegación administrativa de acciones de CLIENTE, mostrar
  importes pendientes explícitos y estados/fechas por detalle. No alterar el
  backend probado para acomodarlo a la interfaz anterior.

### R07. Trazabilidad aprobada no implementada en todas las acciones

- Inventario/tarifas registran auditoría ADMIN; crear/editar/eliminar estación,
  editar/inactivar usuarios y ocultar alquileres no registran aún la evidencia
  equivalente en sus rutas/servicios revisados. El documento 35 incluye estas
  acciones relevantes. No afirmar cobertura por existir AuditoriaSistema.
- HistorialEstadoBicicleta admite actor nulo en el mapeo pero su único constructor
  exige actor y fija USUARIO. Aún no soporta crear correctamente eventos SISTEMA
  para vencimiento/preventivo; no inventar un ADMIN para esos procesos.
- Cierre: auditoría de acciones ADMIN críticas junto con la escritura y soporte
  de origen/actor SISTEMA en los procesos automáticos, con pruebas específicas.

### R08. Reglas de módulos futuros son pendientes reales, no código muerto

- Sin servicios de mantenimiento/incidencia no están implementados el máximo de
  una orden abierta, trabajo por técnico asignado, revisión de incidencias,
  corte económico o recepción posterior a pérdida.
- Inactivar un usuario actualmente cambia activo desde UsuarioController; no
  pausa/desasigna sus órdenes según lo aprobado para TECNICO. Integrar esa
  dependencia al habilitar mantenimiento, sin declararla resuelta antes.
- Reserva nueva no comprueba aún deuda liquidada ni preventiva vencida. La
  existencia del intervalo en TipoBicicleta no implementa la política temporal.
- No eliminar estas entidades por no tener todavía endpoint: pertenecen al DER.
  Sí registrar su dependencia y no ofrecer una operación que requiera sus guardas
  antes de implementarlas.

### R09. Transición física y seguridad transversal pendientes

- Alquiler conserva campos/constructores antiguos y los nuevos permiten null
  para coexistir. La transición no es el esquema final: al retirar productores
  antiguos habrá que aplicar la obligatoriedad aprobada y eliminar columnas
  sustituidas de forma controlada. No aplicar NOT NULL ahora rompiendo el legado.
- USUARIO almacena hash de contraseña y hash de token, pero sus nombres físicos
  actuales no coinciden literalmente con password_hash/token_recuperacion_hash
  del DER. No se detectó contraseña/token originales guardados por esos servicios;
  distinguir una diferencia de nombre de una diferencia de protección del dato.
- Revalidación de cuenta activa y roles persistidos no es uniforme en toda la
  API; el nuevo servicio de alquiler sí la aplica. Seguridad actual también
  deshabilita CSRF. Requieren revisión transversal antes de considerar apto un
  despliegue, no una afirmación de seguridad completa por tests de roles.
- No se probaron concurrencia real entre conexiones ni todos los casos negativos
  del proxy del servicio. No desactivar open-in-view globalmente sin migrar
  también las lecturas fuera de servicio/transacción que aún lo requieren.

## 4. Documentación: diseño aprobado frente a estado de implementación

Los archivos 08, 22, 25 y 28 contienen fotos históricas con expresiones como
"no implementado" o cantidades de 28 entidades. Se conservan como antecedentes,
pero no deben usarse para deducir el estado actual del código. El modelo vigente
es el Mermaid v5 de 29 entidades y las decisiones posteriores aprobadas.

El documento 31 atribuía notificaciones repetidas a auditoría genérica; debe
interpretarse y corregirse conforme al documento 35: AUDITORIA_SISTEMA es solo
ADMIN, no un destino para toda notificación automática ni sustituto de historias
financieras. No agregar otra tabla o inventar una transición por esta revisión.

Seguimiento actual: este informe y el índice 00. Evidencia cronológica: documento
37. No se reabren reglas aprobadas por encontrar una descripción histórica vieja.

## 5. Orden de cierre propuesto

1. Limpieza R04 y tratamiento HTTP R05 en un bloque acotado verificable; corregir
   la presentación ADMIN/CLIENTE de R06 sin ampliar permisos.
2. Preparar el reemplazo coordinado de lecturas cliente/ADMIN y pantallas R01,
   incorporando los consumidores de inventario/estadísticas R03 al plan de retiro.
   No publicar un formato nuevo aislado mientras los productores siguen siendo
   incompatibles. Resolver explícitamente los registros de prueba anteriores al
   cambio, sin asumir una migración de datos de producción que no se ha hecho.
3. Completar el recorrido operativo/financiero nuevo con sus dependencias R02,
   R07 y R08. Cada entrega debe tener una operación usable y pruebas, no solo
   acumular modelos o métodos sin consumidor. Que pagos sea dependencia no permite
   prometer alquiler 100% cerrado antes de implementarlo.
4. Cambiar consumidores HTTP/frontend, dejar de producir el modelo anterior y
   retirar AlquilerDTO/AlquilerRequestDTO/enum/campos/consultas sustituidos cuando
   no queden usos. Verificar relaciones y esquema físico después del corte.
5. Verificación de extremo a extremo con CLIENTE/ADMIN/TECNICO según cada alcance,
   regresiones y concurrencia aplicables, documentación actual y checkpoint Git.

Condición para cerrar un bloque: regla aprobada + persistencia + servicio +
permisos/HTTP + consumidor aplicable + pruebas + retiro de lo sustituido. Una
dependencia aún pendiente debe aparecer aquí, no quedar implícita ni considerarse
terminada por compilar. No se autorizan eliminaciones o cambios Java por este informe.
