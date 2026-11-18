# Alquiler: hora de Río Grande en MariaDB, API y pantallas

Estado: el usuario aplicó la migración 06; las consultas posteriores confirmaron
la convención local, los ocho alquileres y sus historiales alineados. El primer
arranque falló en el mapeo de tipos de Hibernate 5.6: el conversor devolvía
`LocalDateTime` para JDBC. Se corrigió a `java.sql.Timestamp`, sin cambiar
columnas, fechas, offset ni SQL aplicado. Falta repetir compilación/prueba y
arranque; **no repetir la migración** si ya está `ALQUILER_UTC3_V1`.

## Qué se cambió

- Los `DATETIME` de `alquiler`, `alquiler_detalle`, `pago`, `reembolso` y sus
  cuatro historiales guardan hora de Río Grande. El conversor JPA es explícito
  y conserva la representación UTC en Java para cálculos, consultas y
  comparaciones con otros módulos. No cambia la zona global de MariaDB o Java.
- `AlquilerService` usa el mismo instante para cabecera, detalle e inventario.
  Los movimientos de inventario por reserva/retiro/devolución/cancelación o
  vencimiento se guardan localmente; en los cierres se indica alquiler/detalle.
  Incidencias, mantenimiento, auditoría y los demás eventos de inventario no
  cambian su propio almacenamiento. Si una incidencia cambia la cabecera o
  detalle del alquiler, esas dos entidades sí usan el conversor nuevo.
- La API de alquiler y finanzas serializa las fechas en `America/Argentina/Ushuaia`
  con offset explícito. Por ejemplo, `15:50:43.941144 UTC` se guarda
  `12:50:43.941144` y se envía `2026-11-18T12:50:43.941144-03:00`.
  Mis alquileres y Administración las muestran en hora de Río Grande sin
  depender de la zona del navegador. Las respuestas antiguas sin offset se
  siguen leyendo como UTC, como en el contrato anterior.
- El perfil `refactor` verifica la vista de revisión antes de iniciar las tareas
  programadas. No agrega una tabla ni modifica el DER relacional. No arrancar
  código nuevo con BD vieja, ni código viejo con fechas migradas. El perfil por
  defecto no es un modo alternativo de instalar o probar esta migración.

La prueba SQL pausada comprueba también la columna física y la comparación JPQL
al límite de vencimiento. No se afirma que haya corrido.
[Conversores Hibernate 5.6](https://docs.hibernate.org/orm/5.6/userguide/html_single/#basic-jpa-convert).

## Foto revisada y protecciones

Hay ocho alquileres cerrados (#5 a #12), 16 detalles, 20 historias de cabecera,
47 historias de detalle, cuatro pagos y cuatro reembolsos simulados, cada uno
con tres movimientos. No hay incidencias ni fechas externas no nulas en esos
historiales financieros. El informe previo identifica:

- 16 reservas de inventario ya en UTC-3, sin desplazamiento.
- 23 eventos UTC de inventario anclados por bicicleta, detalle, estado y
  fecha/motivo: 7 retiros, 7 devoluciones y 9 cancelaciones.
- Cuatro reservas antiguas con IDs 2, 5, 29 y 34 sin cabecera para corroborar.
  Se dejan intactas, como los demás eventos de inventario ajenos a esta lista.

Con datos diferentes, duplicados o motores/triggers distintos de los esperados
la migración aborta. Convierte solo fechas, sin cambiar montos, estados,
ubicaciones, IDs, versiones o precisión. El cambio de datos es transaccional y
verifica plazos y fechas cruzadas antes del COMMIT. La vista marcadora es DDL
y se instala después del COMMIT; si ese último paso falla, puede que los datos
ya estén convertidos. **Ante cualquier error, no reimportar ni arrancar hasta
verificar el estado.**
[Confirmaciones implícitas de DDL en MariaDB](https://mariadb.com/docs/server/reference/sql-statements/transactions/sql-statements-that-cause-an-implicit-commit).

## Guardar, compilar y probar

### 1. Detener y compilar

Los archivos ya están guardados. Detené la app con Ctrl+C y dejá MariaDB
levantada. No usar `clean`, `create-drop`, ni habilitar las integraciones SQL
pausadas. Desde la raíz del proyecto:

```powershell
.\gradlew.bat compileJava compileTestJava
.\gradlew.bat test --tests "HoraRioGrandeTest" --tests "CrearReservaServiceTest" --tests "RetiroAlquilerServiceTest" --tests "DevolucionAlquilerServiceTest" --tests "OperacionReservaServiceTest" --tests "ConsultaAlquilerControllerTest" --tests "RetiroAlquilerControllerTest" --tests "ConsultaAlquilerServiceTest" --tests "VencimientoReservasJobTest" --tests "PagoServiceTest" --tests "PagoAdicionalServiceTest" --tests "ReembolsoServiceTest" --tests "LiquidacionAlquilerServiceTest" --tests "IncidenciaServiceTest"

$nodeQA = 'C:\Users\Karim\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
& $nodeQA --test '.\src\test\js\ListadoAlquileres.test.cjs' '.\src\test\js\OperacionesAlquiler.test.cjs'
```

### 2. Migrar con la app detenida

1. Exportá un respaldo fresco de **estructura y datos**, fuera del repositorio.
   El anterior a la limpieza no conserva las últimas pruebas. No hagas
   operaciones de escritura concurrentes desde otra sesión.
2. En phpMyAdmin seleccioná `turismo_sustentable`, mantené activadas las FK e
   importá el [informe previo, solo lectura](../../../scripts/migraciones/2026-11-18-06-clasificar-hora-alquiler.sql).
   Confirmá los conteos y las cuatro clasificaciones de inventario indicadas.
   Si no coincide la foto #5–#12, no ejecutes la migración.
3. Importá completo y una sola vez el [SQL de migración](../../../scripts/migraciones/2026-11-18-06-migrar-hora-alquiler-rio-grande.sql).
   Esperá `ALQUILER_UTC3_V1` y `America/Argentina/Ushuaia`. En el #11 la
   creación queda `2026-11-18 12:50:43.941144`, el vencimiento `13:05:43.941144`,
   el cierre `12:51:17.611319` y el costo en ARS `136.94`.
4. Ejecutá la [verificación posterior, solo lectura](../../../scripts/migraciones/2026-11-18-06-verificar-hora-alquiler.sql).
   Debe haber 15 minutos por reserva y cero reservas sin histórico de inventario
   local. Los milisegundos de diferencia de reservas antiguas se conservan;
   las nuevas deben coincidir a la precisión de MariaDB.

### 3. Arrancar y comprobar el circuito real

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=refactor --spring.jpa.hibernate.ddl-auto=validate"
```

Ctrl+F5 en Mis alquileres y Administración. En el #11 el primer movimiento
debe verse a las 12:50:43 y llegar en JSON con `-03:00`.

Desde otra terminal repetir [Playwright de alquiler](45-historial-estado-alquiler.md#prueba-de-navegador-con-playwright-opt-in),
con `TURISMO_E2E_VISIBLE=true` si querés ver el navegador. Los dos casos rápidos
repiten el circuito y la cancelación con nuevas reservas demo. Playwright emula
un navegador con zona UTC para comprobar que el front muestra Río Grande.
Repetí los SELECT posteriores para comprobar las filas nuevas. Para comprobar
también el SELECT del job con MariaDB real, ejecutar el caso opcional de
vencimiento: debe esperar sus 15 minutos reales y luego registrar una sola
transición a `VENCIDO` de origen SISTEMA.
