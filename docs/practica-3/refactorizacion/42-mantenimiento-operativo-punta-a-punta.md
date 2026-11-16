# Mantenimiento operativo de punta a punta

16 de noviembre de 2026. Etapa 334. Alcance autorizado: únicamente mantenimiento,
sus conexiones necesarias, pantallas y pruebas. Checkpoint anterior: 56658b6.
No se implementa aquí el circuito de incidencias durante un viaje ni el retiro
global del modelo de alquiler anterior.

## Qué se implementó y dónde

### Servicio y reglas

MantenimientoService utiliza las entidades ORDEN_MANTENIMIENTO, sus dos
catálogos, HISTORIAL_ORDEN_MANTENIMIENTO y AUDITORIA_SISTEMA existentes del DER v5.
No crea entidades, estados, enums, tablas de trabajo o saldos nuevos.

- ADMIN crea órdenes PREVENTIVO/CORRECTIVO, con motivo y técnico inicial
  opcional. Solo bicicletas sin reserva/viaje incompatible y sin otra orden
  abierta. La bicicleta queda MANTENIMIENTO sin alterar su ubicación.
- ADMIN asigna/reasigna únicamente en CREADA o PAUSADA, a una cuenta activa
  con rol TECNICO. Asignar no inicia/reanuda el trabajo.
- TECNICO activo y asignado inicia, reanuda, pausa con motivo, registra
  diagnóstico/trabajo/observaciones/costo de repuestos y finaliza APTA o NO_APTA.
  El primer inicio se conserva al pausar, reasignar o reanudar.
- Finalizar exige diagnóstico, trabajo y resultado. El costo es opcional,
  no negativo, hasta diez enteros y dos decimales, en ARS cuando se informa.
  Es costo interno: no se añade a la deuda del cliente. Un costo desconocido
  no se inventa como cero; dejar el campo vacío conserva lo registrado.
- ADMIN cancela órdenes abiertas con motivo. Finalizar o cancelar no habilita
  automáticamente la bicicleta ni borra sus registros anteriores.
- ADMIN habilita de MANTENIMIENTO a DISPONIBLE desde la última revisión APTA,
  con versiones actuales de orden/bicicleta, motivo, ubicación física en
  estación activa, preventiva vigente y sin orden, viaje, reserva, falla nueva
  o incidencia pendiente incompatible. No reutiliza una inspección anterior a
  otra intervención ni inventa recepción física.
- ADMIN no sustituye al técnico; una cuenta con ambos roles debe estar además
  asignada para ejecutar trabajo técnico.

Las escrituras bloquean cuentas implicadas por ID, después bicicleta y orden.
La baja del técnico utiliza el mismo orden de bloqueos y protege todas sus
órdenes afectadas dentro de una única transacción. Todas las creaciones de
órdenes usan el bloqueo de la bicicleta para serializar la comprobación de
una única abierta; no se afirma que exista un índice SQL parcial nuevo.

Los comandos OperacionMantenimientoDTO llevan versión y admiten únicamente
los campos propios de cada acción. CrearOrdenMantenimientoDTO,
ActualizarIntervaloPreventivoDTO e InactivarUsuarioDTO separan entradas del
modelo persistido. Los DTO de respuesta contienen solo escalares inmutables,
incluida ubicación pública/interna para el técnico asignado; nunca entidades
JPA, datos privados del cliente, contraseñas ni tokens.

### Preventivo y conexión con inventario/alquiler

CalendarioPreventivo centraliza el cálculo antes presente en AlquilerService.
Se mantiene el criterio ya usado: fecha calendario UTC, intervalo positivo en
meses, ajuste al último día del mes, última preventiva FINALIZADA/APTA como
referencia o fecha real de alta. Sin referencia fiable exige revisión inicial.
No cambia duración, cotización, costos o liquidación del alquiler.

PreventivoMantenimientoJob revisa disponibles cada sesenta segundos en el perfil
refactor, por lotes con cursor de ID. No depende del navegador ni reinicia fechas.
Genera PREVENTIVO/CREADA sin técnico y MANTENIMIENTO, con origen SISTEMA y sin
actor humano. No toca reservas, viajes, INACTIVA, DE_BAJA o EXTRAVIADA. Las
guardas sincrónicas de reserva siguen impidiendo alquilar si aún no corrió la tarea.

Si al cerrar un CORRECTIVO sigue vencida la preventiva, genera la PREVENTIVO
pendiente sin duplicar una abierta y antes de habilitar. El correctivo no
reinicia ese calendario. ADMIN puede modificar el intervalo positivo por tipo,
con comparación del valor anterior y auditoría; no borra inspecciones ni libera
bicicletas automáticamente.

BicicletaService ahora comprueba órdenes abiertas y detalles de alquiler activos
al editar/retirar unidades. Reactivar una INACTIVA con revisión pendiente deriva
a MANTENIMIENTO y su orden, sin ofrecerla brevemente a clientes. Se conserva la
baja desde MANTENIMIENTO únicamente sin órdenes abiertas, sin viaje/reserva y
desde taller interno, con sus controles de inventario.

No se agrega un traslado ficticio ni se exige Taller para realizar toda revisión:
se conserva la presencia física registrada. Habilitar no traslada ni cambia la
capacidad de la estación; las ubicaciones internas siguen fuera del alquiler público.

### Inactivación de técnicos

UsuarioController delega la baja en UsuarioService, que integra la operación
de mantenimiento en su transacción. Se conserva DELETE /api/usuarios/{id},
con cuerpo opcional {motivo}; si hay órdenes abiertas requiere motivo.

- CREADA: conserva estado, queda sin técnico.
- EN_PROCESO: pasa a PAUSADA y queda sin técnico.
- PAUSADA: conserva estado y motivo de pausa, queda sin técnico.
- FINALIZADA/CANCELADA: no cambian ni pierden técnico histórico.

Se conservan fecha inicial, diagnóstico, trabajo y costos. Historial conserva
asignación anterior/nueva, cambio real de estado si existe y ADMIN responsable;
auditoría registra la baja. Un fallo revierte cuenta, órdenes y registros juntos.
Una cuenta ADMIN sigue protegida, incluso si también tiene TECNICO. No elimina
Persona, roles, usuario o historia; tampoco promete revocar sesiones globalmente.
Las operaciones de mantenimiento sí revalidan cuenta activa y rol persistido.

### Pantallas y navegación

Nueva pantalla mantenimiento.html y mantenimiento.js, accesible para ADMIN y
TECNICO desde Inicio y desde el panel ADMIN.

- ADMIN consulta todas las órdenes, crea, asigna, cancela, modifica intervalos,
  consulta historia y habilita después de una revisión favorable.
- TECNICO ve únicamente sus órdenes y su ubicación real, incluidas estaciones/
  talleres internos. Solo ofrece trabajo técnico para su propia asignación.
- La pantalla muestra diagnóstico, trabajos, observaciones, costo interno,
  resultado y fechas. No confunde orden terminada con bicicleta habilitada.
- Formularios incluyen motivo, validaciones y versiones; no envían libremente
  estado, actor, moneda o aptitud fuera de la acción correspondiente.
- Textos e historiales se escapan; doble clic no duplica solicitudes. Un 401/403
  se delega al manejo de sesión y un 409 exige recargar, sin reenvío automático.
- El botón de baja de usuarios en ADMIN solicita motivo y explica sus efectos.

## HTTP

| Ruta | Método | Actor |
| --- | --- | --- |
| /api/mantenimiento/ordenes | GET | ADMIN todas; TECNICO asignadas |
| /api/mantenimiento/ordenes/{id} | GET | ADMIN o TECNICO asignado |
| /api/mantenimiento/ordenes/{id}/historial | GET | ADMIN o TECNICO asignado |
| /api/mantenimiento/tecnicos | GET | ADMIN, solo técnicos activos |
| /api/mantenimiento/ordenes | POST | ADMIN |
| /api/mantenimiento/ordenes/{id}/asignar | POST | ADMIN |
| /api/mantenimiento/ordenes/{id}/cancelar | POST | ADMIN |
| /api/mantenimiento/ordenes/{id}/habilitar | POST | ADMIN |
| /api/mantenimiento/ordenes/{id}/iniciar | POST | TECNICO asignado |
| /api/mantenimiento/ordenes/{id}/reanudar | POST | TECNICO asignado |
| /api/mantenimiento/ordenes/{id}/pausar | POST | TECNICO asignado |
| /api/mantenimiento/ordenes/{id}/trabajo | POST | TECNICO asignado |
| /api/mantenimiento/ordenes/{id}/finalizar | POST | TECNICO asignado |
| /api/mantenimiento/tipos-bicicleta/{id}/intervalo | PUT | ADMIN |

MantenimientoController mantiene 400/404/409 del caso de uso; configuración
interna conserva 500 saneado y permisos 401/403. Nunca se habilita el módulo
a CLIENTE solo ni se conceden a TECNICO los listados generales de inventario/
estaciones/usuarios.

## Verificación ejecutada

- Compilación Java principal y de pruebas correcta; no cambios en build.gradle.
- Regresión unitaria/HTTP: 59 clases, **845 aprobadas**, cero fallos/errores/
  omisiones. BUILD SUCCESSFUL 21 s, 16/11 a las 10:51 Argentina. Incluye 54
  casos de MantenimientoService, calendario, tarea automática, HTTP y suites
  existentes de usuarios, seguridad, registro, inventario, alquiler y finanzas.
- JavaScript: **78 aprobadas**, cero fallos; doce nuevas del módulo y regresión
  de sesión, inventario y alquileres. Sintaxis del nuevo archivo comprobada.
- Integración MariaDB conjunta: **60 aprobadas** (12 mantenimiento + 48 alquiler),
  cero fallos/errores/omisiones. BUILD SUCCESSFUL 1 min 2 s, 16/11 10:48 Argentina.
  Incluye concurrencia de creación y de inicio frente a inactivación, rollback
  tras escrituras reales, reasignación, snapshots, calendario, guardas de
  habilitación, permisos persistidos y compatibilidad con alquiler/finanzas.
- Tres recorridos Chromium con servidor HTTP real incluidos en esas 60: los dos
  de alquiler existentes y el nuevo ADMIN/TECNICO (inicio, finalización con costo,
  bicicleta todavía MANTENIMIENTO, habilitación ADMIN y consulta del resultado).
- Las primeras correcciones fueron de fixtures y selectores: constructor del
  catálogo, mocks de dependencias nuevas, recuentos aislados y selector por orden.
  No se relajó el modelo o la autorización para silenciar esos fallos.
- UsuarioControllerTest ahora prueba delegación/traducción HTTP; las reglas de
  inactivación se comprueban en el servicio y en SQL, donde están implementadas.

SQL utiliza turismo_sustentable descartable/create-drop, con autorización
previa y sin una app normal en 8080. Las tareas automáticas se desactivan durante
las suites controladas por Clock. Al finalizar se cierran servidores y se
eliminan las tablas/datos de prueba. No queda una app de demostración levantada.

Para repetir SQL/navegador, con la app detenida y MariaDB activa:

```powershell
$env:TURISMO_TEST_MYSQL = 'true'
$env:TURISMO_TEST_WEB = 'true'
try {
    .\gradlew.bat test --tests "MantenimientoMysqlIntegrationTest" --tests "AlquilerMysqlIntegrationTest" --rerun-tasks --no-daemon
}
finally {
    Remove-Item Env:\TURISMO_TEST_MYSQL,Env:\TURISMO_TEST_WEB -ErrorAction SilentlyContinue
}
```

Son tandas distintas: Gradle reemplaza XML con cada ejecución. Los 60 casos SQL
se corroboraron y registraron antes de la última tanda unitaria/HTTP de 845 casos,
que reemplazó sus XML. No se afirma que todos convivan en el último reporte.
Sin TURISMO_TEST_WEB se omiten los recorridos Chromium, no se simulan como aprobados.

## Uso para una demostración y límites

Levantar el perfil refactor y entrar a Mantenimiento desde Inicio o ADMIN.
Se necesita una cuenta ADMIN y otra con TECNICO, con roles asignados por el
procedimiento SQL ya utilizado y nuevo login para actualizar autoridades.
La precarga de catálogos no crea usuarios ni técnicos. Una cuenta con CLIENTE
y TECNICO mantiene ambos permisos; puede abrir mantenimiento.html directamente.

Orden de demostración: crear/revisar orden, asignar técnico, iniciar, registrar
trabajo, pausar/reanudar si corresponde, finalizar APTA, comprobar que sigue en
MANTENIMIENTO y habilitar como ADMIN. NO_APTA, revisión vieja o una obligación
preventiva pendiente impiden la habilitación.

El módulo queda conectado para estos recorridos. No equivale a DER completo:
incidencias de viaje, sus decisiones/recepciones/cortes económicos, cierre de
consumidores heredados y despliegue siguen pendientes. Los pagos continúan
simulados. No se hizo commit/push nuevo ni se tocaron los tres PNG o las pruebas
Playwright de la otra materia. Los intervalos de prueba no certifican la
seguridad mecánica de bicicletas reales.

## Corrección puntual de interfaz — etapa 335

16/11/2026. Aprobación expresa posterior al checkpoint 1d75fd1. Solo se modifican
tres archivos de producción: mantenimiento.html, mantenimiento.js y estilos.css.
Las reglas CSS nuevas están delimitadas por .maintenance-page, sin cambiar la
presentación de las otras páginas. No se modifica Java principal, base de datos,
DER, Gradle, pruebas de la otra materia ni los tres PNG existentes del usuario.

- Cabecera verde, marca, iconos, botones y pie coherentes con el panel existente.
  Navegación plegable; la barra permanece arriba al desplazar pero ocupa su altura
  real, para no tapar el encabezado cuando el nombre del usuario tiene varias líneas.
- Tabla de anchos estables en escritorio y presentación por tarjetas con rótulos
  en pantallas pequeñas. Textos largos se ajustan sin ensanchar la página.
- Actualizando/Guardando y spinner visibles; botones del módulo deshabilitados
  durante la operación. Las consultas simultáneas comparten una solicitud.
  Navegar/cerrar sesión no queda bloqueado por este mecanismo.
- Confirmaciones en español, acciones explícitas y formularios ajustados a móvil.
  Cancelar no escribe; error de actualización posterior a un guardado confirmado
  informa ambas cosas, sin reenviar la escritura ni afirmar que la lista está al día.
- Estados/eventos con nombres legibles, fechas UTC interpretadas como UTC y
  mostradas en hora local del navegador (formato de 24 horas). No se alteran las
  fechas persistidas ni los códigos que se envían al backend.
- Historial principal legible y snapshots técnicos conservados bajo un detalle
  desplegable. Etiquetas, textos externos escapados y controles de versión se
  mantienen. Se preservan los trabajos desplegados al recargar.
- Acceso por teclado, salto al contenido, foco visible y devolución del foco al
  botón o a la fila actualizada al cerrar el diálogo. Esto no es una certificación
  WCAG/Nielsen ni sustituye una revisión global o ensayos con lectores de pantalla.

El criterio para mostrar Habilitar bicicleta no se amplía: el backend sigue
validando revisión vigente, ubicación y todos los impedimentos antes de habilitar.
No se prometen autorizaciones por la mera presencia del botón.

### Verificación de esta corrección

- Sintaxis de mantenimiento.js correcta. Las **87 pruebas JavaScript** pasan
  (78 previas más nueve del ajuste), cero fallos u omisiones.
- **Once escenarios Chromium aprobados**, cero fallos/errores/omisiones, BUILD
  SUCCESSFUL 48 s. XML corroborado con timestamp 2026-11-16T14:32:58.760Z.
  ADMIN y TECNICO en 320, 360, 768 y 1366 px: sin desborde horizontal, menú
  plegable, encabezado no tapado y acciones por rol. Además: teclado/cancelación,
  guardado en espera/foco y validación/historial dentro del ancho móvil.
- MantenimientoVistaWebTest usa los archivos reales y un servidor HTTP local de
  pruebas, pero las respuestas API están controladas. No arranca Spring, JPA o
  SQL ni cambia usuarios/datos. Comprueba presentación e interacción, no reemplaza
  los 60 casos MariaDB de etapa 334 ni afirma una nueva integración SQL.
- Capturas de las ocho variantes quedan en build/reports/tests/mantenimiento-ux
  (artefactos locales, no se agregan a Git). Se inspeccionaron escritorio y móvil.
- La primera tanda detectó una respuesta JSON mal escapada en el nuevo fixture de
  historial; se corrigió construyéndola con ObjectMapper, sin relajar assertions.
- Regresión posterior: **845 unitarias/HTTP**, 59 clases, cero fallos/errores/
  omisiones. BUILD SUCCESSFUL 1 min 3 s; XML desde 2026-11-16T14:34:20.118Z.
  Se excluyeron expresamente suites MySQL, la vista opt-in y tests de la otra
  materia. Esta tanda reemplazó el XML de los once Chromium, corroborado antes;
  las capturas permanecen. Las cantidades corresponden a tandas separadas.

Para repetir únicamente la prueba de interfaz, sin MySQL ni detener la app:

```powershell
$env:TURISMO_TEST_WEB = 'true'
try {
    .\gradlew.bat test --tests "MantenimientoVistaWebTest" --no-daemon
}
finally {
    Remove-Item Env:\TURISMO_TEST_WEB -ErrorAction SilentlyContinue
}
```

No hay commit/push de este ajuste. El siguiente bloque funcional continúa siendo
incidencias durante el alquiler; la revisión global y retiro del legado siguen
pendientes. No se amplía ese alcance por realizar esta corrección de interfaz.

## Mejora futura diferida: reparaciones en estaciones internas

16 de noviembre de 2026. El usuario autoriza dejar esta propuesta documentada y
posponer su implementación por el tiempo disponible para la entrega. No se
modifica Java, JavaScript, SQL, el DER ni el diccionario de estados por esta nota.
La propuesta no se presenta como una restricción ya implementada o vigente.

### Comportamiento actual que se conserva

- Una bicicleta físicamente registrada en una estación ocupa lugar también en
  MANTENIMIENTO. No es un espacio fantasma: se libera al registrar su salida real,
  no al cambiar el estado operativo.
- El circuito actual admite trabajo técnico con presencia física registrada;
  no exige que toda reparación ocurra en un taller interno.
- Habilitar después de una revisión APTA conserva la ubicación. Una bicicleta
  habilitada en una estación interna sigue excluida de la oferta a clientes.

### Propuesta para implementar posteriormente

1. Permitir que una averiada espere en MANTENIMIENTO en una estación pública,
   ocupando su lugar hasta el retiro físico efectivo.
2. Registrar por ADMIN el traslado real a una estación interna activa, con motivo,
   versiones, capacidad disponible y trazabilidad. Liberar origen y ocupar destino
   en la misma transacción, sin inventar movimientos por cambios de estado.
3. Exigir ubicación interna activa para iniciar el trabajo técnico. Precisar antes
   de implementar las condiciones de reanudación, registro de trabajo, finalización
   y movimientos durante una intervención ya iniciada.
4. Conservar la separación entre reparación, habilitación ADMIN y traslado. La
   vuelta a una estación pública requiere registrar el movimiento real; habilitar
   por sí solo no debe asignar una estación ni alterar su ocupación.

### Código y comprobaciones que requeriría

- Revisar las guardas de MantenimientoService y los traslados de BicicletaService.
  Una orden creada no debe impedir el traslado al taller que exige el propio
  circuito futuro; tampoco se deben permitir movimientos incompatibles con un
  trabajo en curso. Resolver ambas condiciones conjuntamente.
- Ajustar los controles y mensajes de gestión de bicicletas/mantenimiento, sin
  depender de ocultar botones ni agregar estados o entidades innecesarios.
- Probar rechazo de inicio en pública, traslado a interna con cupo, reparación,
  habilitación y retorno registrado a pública. Comprobar permisos ADMIN/TECNICO,
  versiones, concurrencia/capacidad y rollback, así como la ocupación durante la
  espera y la exclusión de talleres de los alquileres y devoluciones de clientes.

La definición final y su implementación quedan pendientes. No se ha ejecutado
ninguna prueba de esta propuesta ni se declara resuelta por haberla documentado.

## Cierre manual y acceso al historial: etapa 338

16 de noviembre de 2026. Comprobaciones realizadas por el usuario desde las
pantallas, con una cuenta CLIENTE+ADMIN y otra exclusivamente TECNICO. No se
declara por ello una prueba manual con CLIENTE puro ni una cobertura de todos
los escenarios o permisos.

- Alquiler #1: reserva, anticipo simulado, retiro, devolución sin falla y
  liquidación; costo ARS 1547.00, pago ARS 16380.00 y reembolso confirmado
  ARS 14833.00. Situación SALDADO, sin deuda ni reintegro pendiente.
- Alquiler #2: avería reportada y validada, corte, recepción en estación,
  correctivo asignado, trabajo y resultado APTA por TECNICO; la bicicleta sigue
  MANTENIMIENTO hasta habilitación ADMIN y después vuelve a DISPONIBLE.
  Se conserva el corte al recibir/reparar. Lectura SQL del asistente corrobora
  seis segundos completos facturados, subtotal ARS 27.30 y ausencia de una
  devolución normal ficticia. El usuario confirma reembolso ARS 16352.70 y
  situación SALDADO sobre el pago original ARS 16380.00.
- La fecha de devolución estimada ya guardada/enviada por backend se muestra
  ahora en alquileres-vista.js; la recepción normal se etiqueta Devolución real.
- HistorialMantenimientoDTO solo incorpora valoresAnteriores/valoresNuevos cuando
  MantenimientoService verifica ADMIN vigente. Para TECNICO los campos se omiten
  del JSON; la pantalla también restringe el desplegable a ADMIN. Los eventos
  legibles y las historias persistidas no se borran ni se modifican.
- Regresión dirigida: tres XML corroborados, 109 casos Java aprobados sin fallos,
  errores u omisiones; salida del usuario con 37 JavaScript aprobados y sintaxis
  correcta. Incluye pruebas nuevas de filtrado/serialización, acceso ajeno y
  ocultación del detalle interno para TECNICO. No es otra ejecución SQL/global.
- Después del reinicio, el usuario confirma el historial sin JSON para TECNICO
  y con detalle interno para ADMIN.

### Arranque local conservando los datos de demostración

El archivo application-refactor.properties sigue configurado con create-drop.
Se descargó un respaldo SQL fuera del repositorio antes de detener la app.
Las tablas permanecieron tras ese cierre: se verificaron 29 tablas y datos de
prueba, por lo que NO se importó el respaldo encima ni se borró la base.

Para esta demostración se arrancó con una sobreescritura explícita:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=refactor --spring.jpa.hibernate.ddl-auto=validate"
```

validate comprueba el esquema existente sin recrearlo/eliminarlo. No reemplaza
las migraciones definitivas de producción. No iniciar con el comando anterior
de create-drop si se necesitan conservar estos datos. Las integraciones SQL
opt-in recrean el esquema: no ejecutarlas contra esta demostración en uso.
Los backups no se incorporan a Git.
