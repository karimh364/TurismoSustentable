# Incidencias durante el alquiler: circuito operativo

16 de noviembre de 2026. Etapa 336. Implementación expresamente autorizada,
limitada a incidencias y sus conexiones necesarias. Conserva la corrección de
interfaz de mantenimiento de etapa 335, todavía sin commit. Base Git: 1d75fd1.

## Reglas y alcance

Se implementan RN-INC-01 (documento 21, incluida su sección 8), RN-INV-03
(documento 26, pérdida definitiva), RN-TRZ-01 (documento 36) y las conexiones con
el diccionario de estados, el DER v5, mantenimiento y liquidación existentes.
No se crean tablas, estados, catálogos o saldos nuevos. Se reutilizan INCIDENCIA,
HISTORIAL_INCIDENCIA, incidencia_corte_id y los modelos ya incorporados.

1. CLIENTE titular reporta durante EN_CURSO. ADMIN puede cargar un aviso recibido
   por otro canal e identificar opcionalmente al informante. Registrado por,
   informante, fecha comunicada y fecha de registro se conservan por separado.
2. El reporte queda PENDIENTE y requiere revisión económica, determinada por
   backend. No cierra el detalle, no cambia EN_USO, no congela unilateralmente el
   cobro ni impone una multa. Tipo, descripción y fecha son obligatorios; el lugar
   informado es opcional y no exige GPS o inventar una estación.
3. ADMIN valida o descarta con fundamento y versiones actuales. El corte validado
   debe estar entre retiro y registro del aviso, sin superar decisión o recepción
   real. El momento de revisar no prolonga el tiempo facturado. Se preserva la
   fecha informada aunque la revisión valide otra.
4. Si sigue EN_CURSO y se valida, pasa a CERRADO_POR_INCIDENCIA y conserva el vínculo
   al reporte determinante. No se inventa devolución. PERDIDA/ROBO dejan EXTRAVIADA;
   AVERIA/ACCIDENTE dejan MANTENIMIENTO y una orden correctiva sin duplicar abiertas.
   Crear una orden no acredita recogida ni aptitud de la unidad.
5. Una devolución normal antes de decidir conserva DEVUELTO, estación y hora reales.
   Actualiza la recepción en sus incidencias y sus vínculos de mantenimiento en
   la misma transacción. Mientras quedan revisiones económicas, sus subtotales
   siguen provisionales y el conjunto SIN_LIQUIDAR, aunque esté FINALIZADO.
6. Al resolver, si corresponde el corte se usa ese fin facturable; si se descarta,
   rige la recepción ordinaria, salvo otro fundamento ya aplicado. La cabecera
   solo se liquida al terminar todos los detalles y resolver todas las revisiones
   económicas. No reescribe una liquidación definitiva ni cambia el cierre físico
   anterior. Una segunda revisión no sustituye un corte por otro distinto.
7. Las otras bicicletas conservan su uso/reserva. El cálculo utiliza tarifa y
   cotización congeladas por detalle/alquiler; no consulta el dólar ni precio actual.
   Pagos y reembolsos originales se conservan. Si resulta devolución de dinero,
   se registra la obligación por el servicio ya existente: no anuncia acreditación
   ni crea un cobro por daños, reposición o responsabilidad.

No hay cierre automático a las 24 horas, atribución de culpa, seguros, asistencia
física automática ni localización de la bicicleta por completar el formulario.
Las correcciones libres/reapertura de decisiones no se habilitan: los reportes y
decisiones originales se conservan. Un reporte idéntico repetido devuelve su
registro anterior; los hechos nuevos requieren su propio reporte durante el uso.

## Recuperación, mantenimiento y pérdida definitiva

- Después del cierre excepcional, ADMIN registra la recepción física real,
  confirmada expresamente, en una estación activa con espacio o en un lugar real
  fuera de estación. Se conserva fecha, responsable, motivo y ubicación de esa
  recepción. Mientras siga EN_CURSO se utiliza la devolución normal, o se resuelve
  primero el cierre excepcional; no se usa Recuperar como una devolución ficticia.
- La recuperada pasa a MANTENIMIENTO, no a DISPONIBLE. Se genera/reutiliza su
  correctivo y se conservan las relaciones. No reinicia el tiempo ni reliquida.
- Si se recibió fuera de estación, ADMIN puede registrar después su ingreso real
  en una estación activa con cupo. Se actualiza la ubicación actual y su trazabilidad,
  sin sobrescribir el lugar/fecha de la recepción original. Esto evita dejar una
  unidad recibida sin un recorrido para registrar su ingreso posterior.
- TECNICO no puede iniciar/reanudar/registrar/finalizar trabajo de una bicicleta
  ausente: exige presencia en estación o recepción real vinculada a esa orden.
  Pausar y las asignaciones administrativas conservan sus permisos. Habilitar
  sigue requiriendo última APTA, estación activa real, preventiva vigente y ausencia
  de impedimentos; recibir o ubicar no habilita automáticamente.
- ADMIN confirma pérdida definitiva solo sobre una PERDIDA/ROBO VALIDADA que
  fundamenta el detalle cerrado excepcionalmente, aún EXTRAVIADA y sin recepción,
  ubicación u operaciones incompatibles. Exige motivo, confirmación y versiones.
- La baja deja DE_BAJA, borrado_bici=false y ubicación nula. Conserva evento
  BAJA_PERDIDA y auditoría. La eliminación posterior exige comprobar ese respaldo
  de la misma incidencia/detalle/bicicleta, no basta DE_BAJA con estación nula.
  Marca borrado_bici=true sin simular una salida ni modificar cupos o deuda.
- Encontrar una DE_BAJA/eliminada no la restaura. El recorrido físico de inventario
  anterior permanece intacto; Gestionar pérdida deriva al caso respaldado para no
  pedir una falsa confirmación de retirada física.

## Código y relaciones

- IncidenciaService coordina reporte, consulta, revisión/corte, recepción, ingreso
  posterior y pérdida definitiva. Reutiliza CalculadoraAlquiler,
  LiquidacionAlquilerService, ReembolsoService y TrazabilidadInventarioService.
- Se agregan cuatro DTO de entrada: ReportarIncidenciaDTO, ResolverIncidenciaDTO,
  RecibirIncidenciaDTO y PerdidaDefinitivaDTO. Validan formato, longitudes,
  confirmaciones/versiones; rechazan campos extra. Las fechas de entrada llevan
  offset explícito y se convierten a UTC, con precisión microsegundo del motor.
- IncidenciaDTO y HistorialIncidenciaDTO son respuestas escalares, sin entidades
  JPA, contraseñas, tokens ni datos financieros de otro cliente. CLIENTE consulta
  solo sus reportes/historias; ADMIN tiene la vista de gestión. Los nombres/IDs de
  estaciones internas y ubicación actual no se exponen al cliente; su historial
  conserva eventos/motivos, no los snapshots internos de administración.
- Se amplían consultas de los repositorios existentes y la creación de fechas
  por Clock en Incidencia/HistorialIncidencia, sin cambiar el esquema ni FK.
- AlquilerService recibe una dependencia para conservar la recepción de reportes
  al devolver; no se reemplaza el circuito normal ni se modifica el modelo legado.
  MantenimientoService agrega la guarda de acceso físico. No cambian pagos,
  tarifas, cotización, catálogos o reglas de creación/retiro de alquiler.
- La carrera real revisión/devolución detectó que buscarActivoPorEmailConBloqueo
  precargaba Usuario.alquileres EAGER antes de bloquear la cabecera y podía retener
  una versión anterior. Se delimita esa consulta con EntityGraph FETCH a persona,
  sin cambiar Usuario ni su mapeo global. SQL verifica explícitamente que la
  colección no se precarga y que la carrera produce un cierre coherente.

Las escrituras se coordinan con bloqueos en orden cuenta → cabecera → detalle →
bicicleta → incidencia/estación/orden, según la operación. La cabecera serializa
reporte, revisión, devolución y liquidación; la bicicleta/estación protegen presencia
y capacidad frente a otras operaciones. Historia y auditoría ADMIN se confirman
con el negocio o se revierte todo. Repetir una decisión/recepción ya efectiva no
duplica efectos ni sustituye sus hechos; un conflicto diferente exige recargar.

HISTORIAL_INCIDENCIA registra estado inicial, transición real o evento de datos
con ambos estados nulos. Los snapshots tienen lista explícita de datos permitidos.
AUDITORIA_SISTEMA conserva únicamente actuaciones ADMIN; no se atribuyen al
administrador ficticiamente los reportes del cliente o la creación automática de orden.

## HTTP y pantallas

| Ruta | Método | Permiso |
| --- | --- | --- |
| /api/incidencias | GET | CLIENTE propias; ADMIN todas |
| /api/incidencias/{id} | GET | CLIENTE titular; ADMIN |
| /api/incidencias/{id}/historial | GET | CLIENTE titular; ADMIN, con detalle de gestión |
| /api/incidencias | POST | CLIENTE titular en uso; ADMIN por otro canal |
| /api/incidencias/{id}/resolver | POST | ADMIN |
| /api/incidencias/{id}/recibir | POST | ADMIN, después de cierre excepcional |
| /api/incidencias/{id}/ubicar | POST | ADMIN, ingreso tras recepción fuera de estación |
| /api/incidencias/{id}/baja-perdida | POST | ADMIN, pérdida definitiva respaldada |
| /api/incidencias/{id}/eliminar-perdida | POST | ADMIN, baja previa respaldada |

Se revalidan cuenta activa y rol vigente en BD y sesión, propiedad y versiones.
TECNICO no accede a las incidencias privadas del cliente por tener ese rol; trabaja
desde sus órdenes. Se mantienen 400/404/409, 401/403 y 500 interno saneado. Un
fallo de configuración/cotización guardada no se presenta como culpa del visitante.

Nueva incidencias.html/incidencias.js con la estética existente: menú plegable,
tarjetas responsive, filtros, carga visible, botones deshabilitados, confirmaciones,
fechas locales y datos escapados. Entradas desde las bicicletas EN_CURSO en Mis
alquileres y ADMIN; enlaces generales en ambas pantallas y Gestión de pérdida desde
inventario. La consulta financiera muestra lo guardado, sin recalcular ni habilitar
cobros administrativos. No promete asistencia de emergencia o recogida coordinada.

## Verificación y repetición

- Compilación Java principal y de pruebas correcta; sintaxis de los cuatro JS
  nuevos/modificados del bloque comprobada. build.gradle conserva su SHA256
  A44AD6F879EA8B518ECC10A4D08C9E8C836C5DE75429DA1080A96D15D99475B6.
- **933 unitarias/HTTP aprobadas**, 61 clases, cero fallos/errores/omisiones.
  BUILD SUCCESSFUL 20 s. XML desde 2026-11-16T15:23:43.253Z. Incluye las 845
  anteriores, 54 de IncidenciaService, 32 HTTP de incidencias y dos guardas de
  mantenimiento por recepción real. Permisos vigentes, validaciones, propiedad,
  fechas/corte, datos internos, snapshots, reintentos y guardas económicas.
- **103 JavaScript aprobadas**, cero fallos/omisiones: 87 anteriores más 16 de
  incidencias, con regresión de sesión, inventario, alquiler y mantenimiento.
- **78 integración MariaDB aprobadas**, cero fallos/errores/omisiones: 48 alquiler,
  12 mantenimiento y 18 incidencias. BUILD SUCCESSFUL 1 min 20 s, 16/11 12:22 de
  Argentina. XML de incidencias 15:21:25.874Z, alquiler 15:21:46.932Z y mantenimiento
  15:22:13.646Z. Incluye cuatro recorridos reales Chromium (dos alquiler, uno
  mantenimiento y uno incidencias CLIENTE/ADMIN).
- Nuevos SQL: FK/hechos/historias, devolución anterior y precisión económica,
  descarte y corte válido, múltiples reportes, uso parcial, recuperación sin
  volver a facturar, recepción externa/ingreso/revisión/habilitación, pérdida y
  eliminación respaldadas, rollback tras escrituras reales, reporte/decisiones
  concurrentes, carrera de devolución/revisión y capacidad de recepción.
- Navegador nuevo con HTTP/Spring/BD reales: CLIENTE reporta desde móvil, ADMIN
  valida corte y registra recuperación, CLIENTE consulta su estado y liquidación.
  Se verifican ausencia de errores JS y desborde horizontal. Capturas de móvil
  y escritorio inspeccionadas, sin afirmar certificación integral de accesibilidad.
- Las primeras correcciones fueron fixtures de roles/selectores/campos; no se
  relajaron permisos o assertions. La carrera que falló en una repetición motivó
  el ajuste de EntityGraph y su nueva prueba explícita, no aceptar un error 500.

Las pruebas MySQL son opt-in y utilizan turismo_sustentable con create-drop,
como las anteriores. Antes de ejecutarlas se verificaron puerto 8080 libre y
cero tablas en esa base. Al terminar se cerraron los servidores de prueba y se
corroboró nuevamente cero tablas. No queda una app de demostración levantada.
No se usa la BD de otra materia ni se modifica Gradle o sus pruebas Playwright.
Las cantidades anteriores corresponden a tandas separadas: Gradle reemplaza sus
XML en cada ejecución, no se afirma que todos estén en el último reporte.
Después se repitió únicamente el escenario Chromium de incidencias para capturas
de una entrada normal a la página: aprobado, BUILD SUCCESSFUL 27 s. Sus capturas
móvil/escritorio se inspeccionaron con la navegación en su posición inicial;
el último XML corresponde a ese caso único, no reemplaza los recuentos anteriores.

```powershell
# App normal detenida, MariaDB activa y datos de turismo_sustentable descartables.
$env:TURISMO_TEST_MYSQL = 'true'
$env:TURISMO_TEST_WEB = 'true'
try {
    .\gradlew.bat test --tests "IncidenciaMysqlIntegrationTest" --tests "MantenimientoMysqlIntegrationTest" --tests "AlquilerMysqlIntegrationTest" --no-daemon
}
finally {
    Remove-Item Env:\TURISMO_TEST_MYSQL,Env:\TURISMO_TEST_WEB -ErrorAction SilentlyContinue
}
```

IncidenciaMysqlIntegrationTest es una suite de verificación del circuito, no una
entidad o un camino alternativo para la aplicación real. Sin las variables se
omite; no se presenta una prueba omitida como aprobada. Las capturas se generan
en build/reports/tests/incidencias-web y no se agregan al repositorio.

## Límites y siguiente paso

Se completa este recorrido académico con los controles descritos. No se declara
terminado todo el DER ni certificación de accesibilidad, seguridad mecánica o
prestación real. Pagos siguen simulados; despliegue y retiro coordinado de campos,
estadísticas y consumidores heredados siguen en el cierre global de migración.
No se tocó la documentación original de visión ni se agregaron reglas de cobro.
Los tres PNG del usuario y las pruebas de la otra materia se conservan intactos.
No se hace commit/push en esta autorización: quedan las etapas 335 y 336 para
un checkpoint posterior.
