# H1 — Historial de estado de la cabecera del alquiler

Implementación autorizada el 18/11/2026. Migración aplicada por el usuario y
estructura comprobada en MariaDB; reporte Java de 394 pruebas aprobadas.
El recorrido principal y la cancelación también pasaron en Playwright contra
la app levantada. Los casos no ejecutados se distinguen abajo.

**Convención horaria posterior:** la [guía 46](46-hora-rio-grande-alquiler.md)
describe el cambio UTC-3 y la migración 06. Las referencias a fecha UTC en
esta guía corresponden al reloj de negocio y, antes de 06, al almacenamiento.
Para instalar el código posterior a 06, seguir primero el orden de la guía 46.

## 1. Qué se agregó y dónde se conecta

- `models/HistorialEstadoAlquiler.java`: evento persistente de cabecera, separado
  del historial de cada bicicleta. FK a alquiler, estado anterior opcional solo
  al crear, estado nuevo obligatorio, fecha UTC explícita, actor, origen y
  observaciones. Sin setters ni cascadas de eliminación a usuarios/catálogos.
- `repositories/HistorialEstadoAlquilerRepository.java`: persistencia y consulta
  por alquiler ordenada por ID. El SELECT construye el DTO directamente: no
  carga Usuario.alquileres EAGER ni credenciales para mostrar el actor.
- `dto/HistorialEstadoAlquilerDTO.java`: id, alquilerId, estados anterior/nuevo,
  actorUsuarioId, actorNickname, origen, observaciones y fechaHoraCambio.
  No contiene datos personales, contraseñas, tokens ni la entidad Usuario.
- `services/TrazabilidadAlquilerService.java`: escritor único, propagación
  MANDATORY. Crear y cambiar comparten la transacción de negocio. No hay
  REQUIRES_NEW, historial confirmado por separado ni catch que oculte fallos.
  La operación que cambia estado ya mantiene el bloqueo de cabecera; si el
  código nuevo es igual al actual, no guarda una transición repetida.
- `services/AlquilerService.java`: registra creación, primer retiro y los cierres
  existentes. Extrae una comprobación privada de visibilidad común a cabecera e
  historial; se mantienen cuenta activa, roles vigentes y propiedad. No cambia
  importes, plazos, reglas de devolución/cancelación ni liquidación.
- `services/IncidenciaService.java`: cierre por incidencia registra al ADMIN que
  decide y la fecha de decisión, no retrotrae la historia a la hora del hecho.
  Una decisión posterior a la devolución/liquidación no fabrica otro cierre.
- `controllers/AlquilerController.java` y `configurations/WebAuthorization.java`:
  GET `/api/alquileres/{id}/historial`. CLIENTE titular o ADMIN vigente; TECNICO
  solo no puede consultar. Sin sesión 401, ajeno/sin permiso 403, alquiler no
  visible/inexistente 404, ID inválido 400. No hay POST/PUT/DELETE de historial.
- `static/js/alquileres-operaciones.js`: botón y diálogo compartidos en Mis
  alquileres y Administración. GET solo al abrir, protección de doble clic,
  textos escapados, etiquetas humanas, vacío y errores de sesión existentes.
  No dispara escrituras ni recarga la tabla al consultar; tampoco mezcla los
  estados de cabecera con los detalles ni con la situación financiera.
- `scripts/migraciones/2026-11-18-05-historial-estado-alquiler.sql`: crea una tabla
  de ocho columnas, cuatro FK RESTRICT e índice por alquiler/id; CHECK de
  origen/actor y de estados distintos. Requiere cero alquileres previos.
  `2026-11-18-05-verificar-historial-alquiler.sql` contiene solo SELECT.
- `entregables/DERv5.txt` y `refactorizacion/der-v5-propuesto.mmd`: misma fuente
  Mermaid actualizada, 30 entidades/59 relaciones FK. Las cuatro relaciones
  nuevas son cabecera, estado anterior, estado nuevo y actor humano opcional.

No se agrega una colección de historiales a la cabecera para cargarla siempre,
ni otro controlador/endpoint de escritura, ni una tabla de catálogo adicional.
La FK actual sigue indicando el estado presente; la tabla nueva conserva sus
movimientos. El actor es una cuenta estable por FK, no un rol fijo ni un ADMIN
ficticio para procesos automáticos. El nickname mostrado es el actual de esa
cuenta; la identidad registrada es actor_usuario_id.

### Eventos esperados

| Hecho | Transición de cabecera | Origen/actor |
| --- | --- | --- |
| Crear reserva | null → RESERVADO | USUARIO, titular |
| Pagar anticipo | Ninguna | El pago tiene su propio historial |
| Primer retiro | RESERVADO → EN_PROGRESO | USUARIO, titular |
| Segundo retiro o repetir retiro | Ninguna adicional | Se conserva el evento original |
| Última devolución/cierre tras uso | EN_PROGRESO → FINALIZADO | USUARIO que confirma el cierre |
| Cancelar todas sin retiro | RESERVADO → CANCELADO | USUARIO, cliente o ADMIN de cancelación operativa |
| Vencer todas sin retiro | RESERVADO → VENCIDO | SISTEMA, actor null |
| Validar incidencia que cierra el conjunto | EN_PROGRESO → FINALIZADO | USUARIO, ADMIN que decide |

Cancelar/vencer una unidad mientras otra sigue en uso NO cierra el conjunto.
Si ya hubo uso y la última pendiente se cancela/vence, el conjunto termina
FINALIZADO conforme a la lógica existente; actor/observaciones explican el hecho
que completó ese cierre. Reembolsar, pagar saldo o revisar una incidencia de un
conjunto ya cerrado no repite el estado operativo. Borrado lógico tampoco borra
el historial ni inventa un cambio de estado; la API mantiene la visibilidad de
la cabecera y no expone un alquiler oculto mediante su historial.

## 2. Antes de arrancar: compilar y probar sin tocar la BD

App detenida. Archivos ya guardados. No usar clean ni iniciar create-drop.

```powershell
Set-Location -LiteralPath 'D:\Karim\Downloads\Turismo Sustentable\_versiones\TurismoSustentable'
.\gradlew.bat compileJava compileTestJava

$pruebasHistorialAlquiler = @(
    'HistorialEstadoAlquilerTest', 'TrazabilidadAlquilerServiceTest',
    'CrearReservaServiceTest', 'RetiroAlquilerServiceTest',
    'DevolucionAlquilerServiceTest', 'OperacionReservaServiceTest',
    'IncidenciaServiceTest', 'ConsultaAlquilerServiceTest',
    'ConsultaAlquilerControllerTest', 'AutorizacionAlquilerTest',
    'WebAuthorizationTest', 'VencimientoReservasJobTest',
    'LiquidacionAlquilerServiceTest', 'PagoServiceTest',
    'PagoAdicionalServiceTest', 'ReembolsoServiceTest'
)
$argumentosHistorialAlquiler = @('test')
foreach ($pruebaHistorialAlquiler in $pruebasHistorialAlquiler) {
    $argumentosHistorialAlquiler += @('--tests', $pruebaHistorialAlquiler)
}
.\gradlew.bat @argumentosHistorialAlquiler

$nodeQA = 'C:\Users\Karim\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
& $nodeQA --check '.\src\main\resources\static\js\alquileres-operaciones.js'
$pruebasJsHistorialAlquiler = @(Get-ChildItem -LiteralPath '.\src\test\js' -Filter '*.test.cjs' -File | ForEach-Object { $_.FullName })
& $nodeQA --test @pruebasJsHistorialAlquiler
```

Estas tandas seleccionan pruebas Java unitarias/HTTP y JavaScript, no los tests
Playwright de otra materia. Si falla algo, detenerse y compartir el error antes
de continuar; no aplicar la migración como forma de arreglar un error de Java.

## 3. Crear la tabla y arrancar

1. Mantener app detenida y MariaDB activa. Conservar el respaldo completo fuera
   del repo. La limpieza QA ya fue ejecutada por el usuario: NO repetirla.
2. En phpMyAdmin seleccionar turismo_sustentable → Importar → archivo
   `scripts/migraciones/2026-11-18-05-historial-estado-alquiler.sql` completo.
   Mantener comprobación de FK activada. Solo una vez.
3. Debe mostrar ocho columnas y cuatro FK RESTRICT, movimientos_iniciales=0 y
   30 tablas/59 FK. Usuarios=8, estaciones=13, bicicletas=22 conservados.
   El DROP PROCEDURE afecta solamente al auxiliar. El DDL tiene commit implícito:
   no prometer rollback de esquema ni forzar una reimportación ante un error.
4. Solo después, arrancar:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=refactor --spring.jpa.hibernate.ddl-auto=validate"
```

No usar update/create-drop para esquivar una migración fallida. Un arranque
correcto valida mapeos básicos; no sustituye las comprobaciones de FK/negocio.
Recargar con Ctrl+F5 las pantallas para tomar el JS nuevo. El botón Historial de
estados aparece en cada alquiler nuevo en ambas pantallas; no necesita nuevos
elementos HTML duplicados ni otra etiqueta script.

## 4. Prueba manual completa, sin SQL que fuerce estados o fechas

Usar los IDs reales que muestre la pantalla; AUTO_INCREMENT no se reinició.

1. CLIENTE activo: crear una reserva de dos bicicletas disponibles del mismo
   origen. Abrir Historial: una creación → Reservado, con nickname/ID de ese
   cliente y hora local. ADMIN ve la misma información desde su listado.
2. Pagar el anticipo simulado normalmente. El historial de cabecera sigue con
   un evento. Retirar la primera: aparece Reservado → En progreso. Retirar la
   segunda: sigue habiendo dos eventos en total. Recargar/repetir una petición
   de retiro ya confirmada no agrega otro evento ni cambia su fecha original.
3. Devolver una unidad: no hay cierre general mientras la otra esté en uso.
   Devolver la última: tercer evento En progreso → Finalizado. El actor es quien
   confirmó esa última devolución. Liquidar/reintegrar/pagar saldo no agrega
   otro movimiento operativo. La consulta de historial no modifica la tabla.
4. Crear otra reserva sin retirar y cancelar todos los pendientes normalmente:
   dos eventos, creación y Reservado → Cancelado, con actor humano. Variante
   ADMIN: cancelar cada pendiente por operación/seguridad; la última guarda
   el motivo administrativo en el historial general.
5. Crear otra sin retirar y dejar pasar sus quince minutos originales. El job
   procesa cada diez segundos por defecto: dos eventos, creación y Reservado →
   Vencido, origen SISTEMA y actor null. No falsear fechas por SQL. Se puede
   preparar esta reserva mientras se recorren los demás escenarios.
6. Nueva reserva/anticipo/retiro de una unidad: reportar incidencia como cliente
   y validarla como ADMIN mediante el circuito ya implementado. Si es la última
   unidad pendiente de cierre, tercer evento En progreso → Finalizado, con ADMIN
   y fecha de la decisión. La hora del corte se conserva en el detalle/incidencia,
   no se usa como fecha retroactiva del cambio de cabecera. Si la unidad ya había
   sido devuelta y el conjunto cerrado, decidir no agrega otro cierre general.
7. Permisos: otro CLIENTE activo no puede abrir el historial ajeno mediante
   `/api/alquileres/ID_REAL/historial` (403). TECNICO solo recibe 403; sin sesión
   recibe 401. ADMIN puede consultar el visible ajeno, sin obtener credenciales.
   Usar navegadores/perfiles separados para no compartir cookies de sesión.

Después importar/ejecutar el archivo de SELECT
`scripts/migraciones/2026-11-18-05-verificar-historial-alquiler.sql`:
los cuatro contadores de coherencia deben ser cero y los listados deben mostrar
las transiciones efectivamente realizadas. Compartir resultados y estados de
las pruebas, no solamente una foto de la cantidad de tablas.

### Prueba de navegador con Playwright (opt-in)

`HistorialAlquilerPlaywrightTest` usa la app real en `localhost:8080`, con
sesiones de navegador separadas para `cliente.demo`, `admin.demo`,
`parcial_playwright` (otro CLIENTE), `tecnico.demo` y visitante. No modifica
los ejercicios Playwright del parcial. Exige las bicicletas demo MB-05 y PA-05
disponibles en RG DEMO - Chacra II y confirma que los pagos sean SIMULADA/PRUEBA.

Crea reservas reales de demostración. Comprueba reserva, anticipo sin nuevo
evento, retiros parciales, reintento, devoluciones parciales, cierre, reintegro
simulado, cancelación parcial/total, permisos HTTP y el diálogo en ambas
pantallas. Vuelve a dejar las unidades en su estación. Si falla, intenta
cerrar sus propios alquileres por la API normal; no borra sus registros ni
historiales. Informa sus IDs para poder revisar el resultado. No usa SQL de
escritura, no modifica inventario ajeno ni utiliza dinero real. La validación
de incidencias sigue en sus pruebas Java y en el recorrido manual: esta tanda
no genera una orden nueva sobre una bicicleta demo.

Con MariaDB y la app refactor/validate levantadas, y Chromium de Playwright ya
instalado en el entorno del proyecto, ejecutar:

```powershell
$env:TURISMO_E2E_HISTORIAL = 'true'
$env:TURISMO_E2E_DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Clave de las cuentas demo' -AsSecureString)).Password
try {
    # Opcional: abrir Chromium y observar la automatización con slowMo de 1,5 s.
    # $env:TURISMO_E2E_VISIBLE = 'true'
    .\gradlew.bat test --rerun --tests "HistorialAlquilerPlaywrightTest"
} finally {
    Remove-Item Env:\TURISMO_E2E_HISTORIAL, Env:\TURISMO_E2E_DEMO_PASSWORD, Env:\TURISMO_E2E_VISIBLE -ErrorAction SilentlyContinue
}
```

Con `TURISMO_E2E_VISIBLE=true`, Playwright trae al frente la ventana del rol
que está usando para el recorrido y cierra Chromium al terminar. No hace
falta interactuar con esas ventanas. `--rerun` ejecuta la tarea de pruebas aunque
Gradle tenga un resultado anterior; no obliga a recompilar Java sin cambios.

Sin activar `TURISMO_E2E_HISTORIAL`, no corre contra la BD local. Para probar el
vencimiento automático, activar además `TURISMO_E2E_VENCIMIENTO=true` y filtrar
con `--tests "HistorialAlquilerPlaywrightTest.elJobVenceLaReservaSinActorHumano"`.
Ese caso espera el plazo real de 15 minutos y comprueba `SISTEMA` sin actor;
no adelanta el reloj ni inyecta estados. Quitar también esa variable al terminar.

Evidencia del 18/11: `BUILD SUCCESSFUL`, 2 casos aprobados, vencimiento
omitido por configuración. El alquiler #9 registró RESERVADO → EN_PROGRESO →
FINALIZADO; #10 registró RESERVADO → CANCELADO. Admin vio el historial de #9
y la API rechazó a visitante (401), otro cliente y técnico (403). MariaDB
confirmó que las reservas de las tres pasadas (#5 a #10) quedaron cerradas y
liquidadas, con los reintegros simulados confirmados cuando hubo anticipo.
MB-05 y PA-05 quedaron DISPONIBLES en su estación demo.

### Integración SQL automatizada: sigue pausada

AlquilerMysqlIntegrationTest agrega conteo de historia de cabecera a sus
comprobaciones de rollback y verifica creación/retiros parciales/reintentos.
IncidenciaMysqlIntegrationTest comprueba un único cierre bajo decisiones
simultáneas. Ambos conservan @Disabled y validate: NO habilitar TURISMO_TEST_MYSQL
ni retirar @Disabled sobre los datos locales conservados. Sus fixtures escriben
y confirman datos incluso con validate. La concurrencia/rollback reales no se
declaran ejecutados hasta retomar esas suites con un entorno autorizado.

## 5. Guardar en Git después de comprobar

No se hizo commit/push automáticamente. Para incluir solo este trabajo y el
registro de la limpieza previa (sin presentación, capturas ni traces), revisar:

```powershell
git status --short
git diff --check
git diff --stat
```

Después de completar las comprobaciones, agregar expresamente estos archivos.
La lista excluye presentación/capturas/traces. Si apareció algún cambio nuevo
ajeno dentro de estos mismos archivos, revisar el diff antes de agregarlo.

```powershell
$archivosHistorialAlquiler = @(
    'docs/practica-3/entregables/DERv5.txt',
    'docs/practica-3/refactorizacion/der-v5-propuesto.mmd',
    'docs/practica-3/refactorizacion/44-diagnostico-y-guia-cierre-der-v5.md',
    'docs/practica-3/refactorizacion/45-historial-estado-alquiler.md',
    'scripts/demo/2026-11-18-limpiar-alquileres-qa-1-4.sql',
    'scripts/migraciones/README.md',
    'scripts/migraciones/2026-11-18-05-historial-estado-alquiler.sql',
    'scripts/migraciones/2026-11-18-05-verificar-historial-alquiler.sql',
    'src/main/java/DSII/TurismoSustentable/models/HistorialEstadoAlquiler.java',
    'src/main/java/DSII/TurismoSustentable/repositories/HistorialEstadoAlquilerRepository.java',
    'src/main/java/DSII/TurismoSustentable/dto/HistorialEstadoAlquilerDTO.java',
    'src/main/java/DSII/TurismoSustentable/services/TrazabilidadAlquilerService.java',
    'src/main/java/DSII/TurismoSustentable/services/AlquilerService.java',
    'src/main/java/DSII/TurismoSustentable/services/IncidenciaService.java',
    'src/main/java/DSII/TurismoSustentable/controllers/AlquilerController.java',
    'src/main/java/DSII/TurismoSustentable/configurations/WebAuthorization.java',
    'src/main/resources/static/js/alquileres-operaciones.js',
    'src/test/java/DSII/TurismoSustentable/models/HistorialEstadoAlquilerTest.java',
    'src/test/java/DSII/TurismoSustentable/services/TrazabilidadAlquilerServiceTest.java',
    'src/test/java/DSII/TurismoSustentable/services/CrearReservaServiceTest.java',
    'src/test/java/DSII/TurismoSustentable/services/RetiroAlquilerServiceTest.java',
    'src/test/java/DSII/TurismoSustentable/services/DevolucionAlquilerServiceTest.java',
    'src/test/java/DSII/TurismoSustentable/services/OperacionReservaServiceTest.java',
    'src/test/java/DSII/TurismoSustentable/services/IncidenciaServiceTest.java',
    'src/test/java/DSII/TurismoSustentable/services/ConsultaAlquilerServiceTest.java',
    'src/test/java/DSII/TurismoSustentable/controllers/ConsultaAlquilerControllerTest.java',
    'src/test/java/DSII/TurismoSustentable/services/AlquilerMysqlIntegrationTest.java',
    'src/test/java/DSII/TurismoSustentable/services/IncidenciaMysqlIntegrationTest.java',
    'src/test/java/DSII/TurismoSustentable/e2e/HistorialAlquilerPlaywrightTest.java',
    'src/test/js/OperacionesAlquiler.test.cjs'
)
git add -- $archivosHistorialAlquiler
git --no-pager diff --cached --stat
git commit -m "Registrar y consultar el historial de estados del alquiler"
git push origin codex/refactorizacion-practicas-iii
git status
```

La migración no está aplicada por guardar el SQL en Git. Registrar su ejecución
y la evidencia de pruebas en la guía 44 cuando el usuario las confirme. B2
(consulta administrativa de auditoría) y otros pendientes no se dan por cerrados
con H1.
