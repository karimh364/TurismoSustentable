# Diagnóstico y guía de cierre de la migración al DER v5

Fecha de referencia: 17 de noviembre de 2026.
Presentación prevista: miércoles 18 de noviembre, 19:30 (Argentina).

## 1. Finalidad y alcance

Consolidar el circuito funcional ya implementado, retirar la convivencia con el
alquiler anterior y conectar las funcionalidades aprobadas que todavía no llegan
a las pantallas. No iniciar otra refactorización general ni añadir entidades o
reglas de negocio sin aprobación.

Esta guía registra la revisión de solo lectura del 17/11 y el orden propuesto de
trabajo. Sus tareas pendientes NO están implementadas por aparecer aquí. Los
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
| Identidad/acceso | PERSONA, documentos y catálogos, registro CLIENTE, múltiples roles, login, recuperación, sesión y edición administrativa | Gestión administrativa de asignación/retiro de roles; uniformar cuenta activa y roles vigentes en rutas conservadas |
| Estaciones | Alta/edición/borrado lógico, pública/interna, capacidad, concurrencia, panel y mapa | Escapado de textos y uso del control común de sesión/roles en el mapa; precisión física de coordenadas |
| Bicicletas | Tipo/tarifa, ubicación, traslados con motivo, estados, baja/eliminación, consulta de eliminadas e historia | Retirar controles y relaciones dependientes de la cabecera antigua |
| Alquiler | Cabecera/detalles, reserva múltiple, retiro/devolución por unidad, cancelación y vencimiento, pantallas | Retirar por completo campos, enum, consultas, DTO, ruta de devolución, formularios y ramas de lectura anteriores |
| Finanzas | Anticipo, adicional, liquidación, deuda y reembolso simulados, idempotencia y pantallas | Regresión integral tras retirar legado; no confundir simulación con Mercado Pago real |
| Incidencias | Reportar/revisar/cortar, recibir/recuperar, pérdida definitiva, conexión con mantenimiento y liquidación | Retirar consulta de alquiler anterior y pulir selección de informante/contexto de cuentas multirrol |
| Mantenimiento | ADMIN gestiona, TECNICO asignado trabaja, historia, preventiva, habilitación posterior e inactivación de técnico | Retirar dependencia anterior; reparación exclusivamente en taller sigue siendo propuesta diferida |
| Auditoría | Escritura administrativa e historiales de dominio implementados | Consulta administrativa general con filtros y pantalla; no exponerla a CLIENTE/TECNICO |
| Estadísticas | Consultas migradas a detalles y tipo contratado, endpoint de resumen | Pantalla consumidora: no encontrada en la revisión |
| Correo | Bienvenida y recuperación conservadas | Inicio tiene un método sin llamadas encontradas; finalización todavía depende del alquiler anterior. Adaptar comprobantes al circuito vigente y retirar lo reemplazado |

La finalidad no es crear un CRUD de todas las tablas: historiales, pagos y estados
deben seguir las operaciones y permisos del dominio, sin edición arbitraria.

## 3. Comparación física con MariaDB

Lectura de information_schema de turismo_sustentable del 17/11:

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

- Verificar cuenta activa y roles persistidos después de inactivación/revocación,
  incluso con una sesión ya abierta, en las rutas que aún usan criterios antiguos.
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
- Alinear mapa con sesión/roles del servidor y explicar fallos de carga al usuario.
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
- Integraciones opt-in TURISMO_TEST_MYSQL todavía pueden usar create-drop. No
  ejecutarlas contra la BD demo que se desea preservar; aislar antes de repetirlas.
- Pagos/reembolsos simulados y dinero ficticio aprobados. Mercado Pago real y
  despliegue productivo no se declaran terminados ni se añaden a esta entrega.
- Exigir taller para iniciar reparación sigue siendo mejora expresamente diferida;
  no declararla incumplimiento de una regla vigente ni implementarla sin aprobación.
- Ocupación física depende de ubicación registrada, no solo del estado de la bici.
- No crear nuevas tablas, billeteras, infraestructura de correos o rediseño general
  como sustituto de cerrar los consumidores y relaciones existentes.

## 7. Primer paso manual preparado

Estado: instrucciones pendientes de aplicar por el usuario.

Subbloque A.2: retirar existsByBicicletaAndEstado de BicicletaService,
MantenimientoService e IncidenciaService; mantener contarAsignacionesActivas de
AlquilerDetalleRepository (RESERVADO, PENDIENTE_RETIRO y EN_CURSO). Quitar la
dependencia AlquilerRepository de los dos primeros y adaptar sus constructores y
tests. IncidenciaService conserva AlquilerRepository para bloquear cabeceras.
Eliminar exclusivamente la consulta antigua de AlquilerRepository.

No retirar aún CodigoEstadoAlquiler, AlquilerDTO, campos físicos o asociaciones:
todavía tienen otros consumidores y se eliminarán en sus pasos coordinados.
No significa que A quede cerrado por completar este subbloque.
