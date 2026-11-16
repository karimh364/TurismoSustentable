# Circuito de alquiler y finanzas simuladas

Fecha: 16 de noviembre de 2026. Etapa 333. Implementación y pruebas autorizadas
expresamente por Karim en tres bloques. No es una migración completa del DER ni
una integración real con Mercado Pago.

## 1. Reembolso simulado

- ReembolsoService utiliza REEMBOLSO, PAGO e HISTORIAL_ESTADO_REEMBOLSO existentes
  del DER v5: no agrega otra entidad, saldo mutable, enum ni pago ficticio.
- Al liquidar el conjunto, registra obligaciones PENDIENTE por el excedente
  confirmado, distribuidas entre pagos aprobados. También se conecta a la
  aprobación tardía del anticipo y al resultado del adicional.
- La creación requiere una transacción existente y cabecera bloqueada. Las
  obligaciones pendientes/en proceso/fallidas ya representadas no se vuelven a
  crear. Los confirmados y comprometidos no pueden superar el pago de origen
  ni lo que se debe reintegrar al conjunto.
- El cliente titular solicita resolver su reintegro. El simulador puede devolver
  EN_PROCESO, CONFIRMADO o FALLIDO; una respuesta incierta no se inventa como
  fallo. Un fallo conocido se reintenta sobre la misma obligación, ID y referencia,
  sin duplicar acreditaciones. Un confirmado no vuelve a procesarse.
- El crédito del saldo simulado se deriva de los reembolsos CONFIRMADO. El PAGO
  original conserva su monto, finalidad, estado APROBADO y fecha de confirmación.
- Las historias automáticas son SISTEMA sin actor humano inventado. Los errores
  internos revierten estado e historias en la misma transacción.
- ReembolsoDTO y ResultadoReembolsoDTO exponen solo datos escalares; no entidades
  JPA, hashes o credenciales. PasarelaSimulada no realiza transferencias reales.

Configuración por nickname, en el perfil refactor:

```properties
# Opcional. Por defecto confirma automáticamente.
pagos.simulados.cuentas.qa.pendiente.reembolso-resultado=EN_PROCESO
pagos.simulados.cuentas.qa.pendiente.demora-segundos=20
# Para ensayar fallo conocido, usar reembolso-resultado=FALLIDO.
```

## 2. Operaciones de alquiler

- POST /api/alquileres usa CrearAlquilerDTO: estación, lista de bicicletas y
  duración estimada común en segundos. Devuelve 201 con AlquilerCabeceraDTO en
  RESERVADO. No retira unidades ni inventa un pago.
- Se retira el productor anterior AlquilerService.crearAlquiler y el DTO
  AlquilerRequestDTO, sustituidos de forma coordinada con el consumidor web.
  El payload anterior queda rechazado: no puede saltar reserva y anticipo.
  Archivo eliminado recuperable desde el checkpoint ed14bd8.
- El servicio de cotización compartido sustituye al respaldo fijo de 1400 del
  recorrido anterior. Sin cotización válida no se inventa una conversión.
- La creación también comprueba preventiva vencida y órdenes abiertas, además
  de las guardas previas de deuda, permisos, estación e inventario.
- El titular CLIENTE cancela una o todas las unidades no retiradas. No cancela
  unidades EN_CURSO ni detiene el uso de otra bicicleta. Se conserva la estimación
  histórica; el detalle sin uso se cierra con costo real cero.
- ADMIN tiene una cancelación administrativa separada por detalle: motivo
  obligatorio y destino INACTIVA o MANTENIMIENTO. Se conserva ubicación, se
  registra historia y auditoría administrativa. No concede permiso de alquilar.
- Al alcanzar el plazo original de quince minutos, las pendientes vencen aunque
  la solicitud sea de cancelación. No se renueva la ventana al pagar.
- VencimientoReservasJob, solo en refactor, busca hasta cien cabeceras con
  pendientes cada diez segundos. Cada cabecera se procesa en su propia
  transacción; un fallo aislado no abandona el lote. No requiere navegador ni
  sesión humana y no selecciona conjuntos cuyas unidades ya están todas en uso.
- Cancelar/vencer libera exclusividad pero no mueve la bicicleta ni libera su
  espacio físico. Si vence la preventiva, pasa a MANTENIMIENTO y se genera o
  vincula la orden correspondiente. Se impide liberar otra asignación vigente.
- Solo cuando todos los detalles son terminales cierra la cabecera:
  FINALIZADO si hubo retiro, VENCIDO si no hubo uso y alguna unidad venció,
  CANCELADO si todas se cancelaron sin uso. Entonces se intenta liquidar y
  registrar el reintegro. Una revisión económica pendiente sigue sin inventar
  costo definitivo. No hay cierre automático de un viaje a las 24 horas.

## 3. Pantallas conectadas

- alquiler.html/alquiler.js: selección múltiple de una misma estación, duración
  común, filtros y precios por hora; creación de reserva, seguida de Mis viajes.
  CLIENTE es obligatorio. La referencia ARS no usa un valor fijo de respaldo.
- mis_alquileres.html/mis_alquileres.js: anticipo simulado, retiro individual,
  cancelación de pendientes, devolución con estación pública y reporte de falla,
  liquidación, adicional por deuda y procesamiento/consulta del reintegro.
- Lista adicional de saldos/reintegros propios incluye alquileres ocultos por
  ADMIN: ocultar un registro no elimina deuda ni impide cobrar un reintegro.
- Consulta del saldo simulado desde Mis viajes; no es dinero real.
- admin.html/admin.js: consulta de conjuntos/detalles, recepción de devolución y
  cancelación administrativa con motivo. Las finanzas son solo consulta en el
  panel, incluso si esa cuenta también tiene CLIENTE.
- alquileres-operaciones.js es compartido por ambos consumidores. Centraliza
  acciones, confirmaciones y errores 401/403 sin repetir solicitudes inseguras.
  Usa una clave de idempotencia por cuenta/alquiler/finalidad, conservada ante
  timeout y respuestas pendientes. No envía precios o importes elegidos por JS.
- La conversión visual usa snapshots guardados. Cambiar moneda o recargar no
  cierra detalles ya desplegados; la tabla administrativa estabiliza sus anchos.
- Se escapan textos del servidor. Los mensajes de validación se muestran sin
  prefijos técnicos de campos. El backend vuelve a validar todo: ocultar botones
  no constituye autorización.
- Se conservan lectura y devolución del modelo anterior para sus consumidores
  todavía reales; las acciones nuevas no se aplican a esos registros.

## 4. HTTP y permisos

| Operación | Ruta | Permiso |
| --- | --- | --- |
| Crear reserva | POST /api/alquileres | CLIENTE titular por sesión |
| Cancelar pendientes | POST /api/alquileres/{id}/cancelar | CLIENTE propietario |
| Cancelar unidad pendiente | POST /api/alquileres/{id}/detalles/{detalleId}/cancelar | CLIENTE propietario |
| Cancelar administrativamente | POST /api/admin/alquileres/{id}/detalles/{detalleId}/cancelar | ADMIN, motivo y destino válidos |
| Retirar | POST /api/alquileres/{id}/detalles/{detalleId}/retirar | CLIENTE propietario y anticipo/plazo válidos |
| Devolver | POST /api/alquileres/{id}/detalles/{detalleId}/devolver | CLIENTE propietario o ADMIN receptor |
| Consultar reintegros/liquidación | GET /api/pagos/reembolsos/{id} | CLIENTE propietario o ADMIN |
| Procesar/consultar reintegro simulado | POST /api/pagos/reembolsos/{id}/resolver | CLIENTE propietario |
| Saldos/reintegros propios pendientes | GET /api/alquileres/pendientes-financieros | CLIENTE vigente |

Se mantienen los endpoints de anticipo, adicional, saldo y liquidación ya
implementados. Se comprueba sesión, cuenta activa y rol persistido según caso,
además de propiedad; ADMIN no hereda CLIENTE.

## 5. Evidencia ejecutada

- Compilación Java principal y de tests correcta.
- Regresión unitaria/HTTP: 55 clases, **758 casos aprobados**, cero fallos,
  errores u omisiones. BUILD SUCCESSFUL en 18 s (16/11, 06:28 Argentina).
  Incluye autorización, registro/recuperación, usuarios, inventario/estaciones,
  cotización, reserva, pagos, devolución, liquidación y los nuevos casos.
- JavaScript: **66 aprobadas**, cero fallos. Incluye selección de unidades,
  idempotencia, doble clic, permisos, errores, devoluciones, reintegro pendiente,
  expansión de detalles y regresión de inventario/sesión/listados.
- Última integración: **48 aprobadas**, cero fallos, errores u omisiones.
  BUILD SUCCESSFUL en 47 s (16/11, 09:57 Argentina). MariaDB real,
  transacciones, UK/FK, concurrencia, rollback, capacidad, estados e historias.
  Incluye dos pruebas Chromium con servidor HTTP real:
  1. CLIENTE crea una reserva de dos unidades, paga anticipo, retira ambas,
     devuelve, liquida y recibe un único reintegro. Comprueba saldo final.
  2. ADMIN sin CLIENTE cancela con motivo, conserva ubicación y consulta
     finanzas sin botones de cobro/retiro; resolver reintegro devuelve 403.
- Las dos pruebas de navegador utilizan Playwright ya presente en Gradle,
  Chromium sin ventana y usuarios de esta suite. No se modifica build.gradle
  ni las pruebas de la otra materia.
- Se corrigieron dos fixtures nuevos, no las reglas de producción: fechaAlta
  es inmutable por JPA y ocultar alquileres es permiso ADMIN, no CLIENTE.
- SQL usa turismo_sustentable descartable con create-drop, por autorización
  previa. Al terminar elimina las tablas/datos generados y cierra el servidor
  de prueba; no deja una aplicación ni una sesión de prueba ejecutándose.

Repetir la integración (solo con la app normal detenida y base descartable):

```powershell
$env:TURISMO_TEST_MYSQL = 'true'
$env:TURISMO_TEST_WEB = 'true' # Opcional: requiere Chromium instalado para Playwright.
try {
    .\gradlew.bat test --tests "AlquilerMysqlIntegrationTest" --rerun-tasks --no-daemon
}
finally {
    Remove-Item Env:\TURISMO_TEST_MYSQL,Env:\TURISMO_TEST_WEB -ErrorAction SilentlyContinue
}
```

Sin TURISMO_TEST_MYSQL la integración no se ejecuta. Sin TURISMO_TEST_WEB se
omiten únicamente las dos pruebas de navegador. Los XML de Gradle se reemplazan
con cada tanda: los totales anteriores pertenecen a ejecuciones separadas
corroboradas, no a una suma ficticia de un único reporte final.

## 6. Límites y siguiente cierre

Los tres bloques anteriores quedan conectados y probados para los escenarios
descritos. **No significa DER v5 completo al 100 %.** Siguen fuera de este alcance:

- Servicios/HTTP/pantallas completos de incidencias y mantenimiento, revisión
  económica, técnico asignado/pausa administrativa y decisiones por extravío.
  Crear/vincular una orden al liberar/devolver no implementa su ciclo técnico.
- Consumo definitivo de estadísticas/notificaciones heredadas y retiro total de
  AlquilerDTO, campos/enum/consultas anteriores cuando no queden consumidores.
  Ahora no quedan productores web de alquileres anteriores, pero sí lecturas/
  devolución de históricos; no borrar relaciones todavía utilizadas.
- Sustituir simulación por proveedor real; migraciones/precarga definitivas,
  configuración no destructiva y revisión de seguridad para despliegue.

No se añadieron entidades fuera del DER, no se modificó su estructura para
silenciar pruebas ni se efectuó otro commit/push. Los tres PNG de la otra materia
que ya estaban sin seguimiento no se tocaron. El checkpoint previo es ed14bd8.
