# Revisión de estaciones tipos y bicicletas

Fecha: 28 de octubre de 2026.

Estado: revisión del segundo bloque del DER lógico. El usuario aprobó expresamente el recorrido de baja de estación de la sección 5, con la condición de que sea exclusivamente lógica, nunca física. Se registra como RN-EST-01 y se aclara borrado_estacion en el Mermaid, sin agregar columnas, claves ni entidades. Los demás campos se contrastan con las reglas y el código existente; no se declara cerrado todo el diseño físico. No se modificaron Java ni la base de datos.

Actualización aprobada el 6 de noviembre de 2026: RN-INV-01, sección 7, distingue Taller interno de estación pública y conserva el borrado lógico de bicicleta separado de DE_BAJA. Se incorporan es_publica y borrado_bici al DER, sin nuevas entidades o relaciones. Sustituye la interpretación anterior que consideraba redundante el indicador de borrado; no modifica Java o la base de datos.

## 1. Base y límites

Se releyeron el diccionario de estados original y los apartados de visión, objetivos y requerimientos del PDF, se contrastó el DER v4 y se revisaron RN-ALQ-03, RN-ALQ-04, RN-BIC-01, RN-BIC-02, RN-MAN-04 y el cierre de incidencias aprobado. La visión exige estaciones, mapa, disponibilidad, mantenimiento y trazabilidad. La separación de estado operativo, presencia física y contrato ya está aprobada; no se vuelve a mezclar en esta revisión.

La aplicación actual conserva tipos y estados como enums, precio por bicicleta, un flag borradoBici y capacidad calculada excluyendo ese flag. El nuevo DER usa catálogos y tarifa por tipo. Por eso la implementación anterior no demuestra que las reglas nuevas estén cubiertas, aunque se reutilicen sus pantallas, fotos y controles de estación.

## 2. ESTACION

| Campo existente en v5 | Clave y condiciones de revisión |
| --- | --- |
| id | PK; identificador estable, no depende del nombre |
| nombre | UNIQUE; obligatorio, sin blanco; normalización y longitud se concretan en D06 |
| direccion | Obligatoria; no se declara única porque no identifica por sí sola una estación |
| capacidad | Entero positivo; nunca puede quedar por debajo de la ocupación física al modificarla |
| borrado_estacion | Indicador de baja lógica; no borra la fila ni sus referencias históricas; recorrido aprobado RN-EST-01 en la sección 5 |
| es_publica | Booleano obligatorio; true identifica una estación pública, false un Taller/depósito interno; RN-INV-01, sección 7 |
| latitud | Decimal obligatorio; rango de -90 a 90; precisión SQL pendiente |
| longitud | Decimal obligatorio; rango de -180 a 180; precisión SQL pendiente |
| version | Control de concurrencia propuesto; no sustituye coordinar los ingresos y cambios de capacidad |

No se agregan columnas espacios_libres, bicicletas_disponibles ni cantidad_ocupada: se calculan desde las bicicletas físicamente asociadas y las condiciones de disponibilidad. Tampoco se declara única la combinación de coordenadas: no se aprobó prohibir estaciones diferentes por proximidad o por coincidir en un punto.

La ocupación incluye toda bicicleta que siga físicamente allí, incluso RESERVADA, MANTENIMIENTO, INACTIVA, DE_BAJA o borrada lógicamente. Se aplica también al Taller. Cambiar estado o marcar borrado no libera su espacio. Los ingresos, traslados, devoluciones, cambios de capacidad y baja de estación deberán coordinar el acceso a la misma estación dentro de transacciones locales; un contador visual o una FK no garantizan por sí solos que no se exceda capacidad.

Con [RN-ALQ-11](30-propuesta-composicion-contratacion.md) aprobada, el origen común se guarda en ALQUILER.estacion_retiro_id, no en cada detalle. Para impedir la baja de una estación con unidades RESERVADO o PENDIENTE_RETIRO todavía sin retirar, la futura consulta deberá unir esos detalles con su cabecera y comprobar el origen. El traslado de la FK no elimina esa protección ni la comprobación de presencia física. El máximo inicial de diez bicicletas por alquiler no obliga a que una estación tenga diez disponibles ni permite superar su capacidad.

En el código actual se observa un control para no reducir capacidad por debajo del conteo que utiliza borradoBici=false. Al refactorizar deberá adaptarse a la presencia física aprobada, sin excluir DE_BAJA todavía presente. La fórmula del DTO que limita espacios libres a cero no sirve como comprobación de integridad de datos antiguos: las inconsistencias deben detectarse y corregirse con evidencia, no darse por resueltas al ocultar un número negativo.

## 3. TIPO_BICICLETA

| Campo existente en v5 | Clave y condiciones de revisión |
| --- | --- |
| id | PK; referenciado por muchas bicicletas |
| codigo | UNIQUE y obligatorio; conservar inicialmente MONTANA, PASEO y ELECTRICA al migrar el enum |
| precio_por_hora_usd | Tarifa vigente del tipo, no de cada unidad; mayor que cero y con hasta dos decimales, aprobado el 7 de noviembre de 2026; no se admiten tarifas gratuitas o negativas |
| intervalo_preventivo_meses | Entero positivo; configurado solo por ADMIN; valores iniciales para pruebas aprobados el 7 de noviembre de 2026, sin convertirlos en calendario comercial definitivo |

No se agrega una tabla cronómetro ni una columna de precio duplicada en BICICLETA. La tarifa aplicada se guarda en ALQUILER_DETALLE y un cambio del tipo no modifica contratos ya confirmados. Los cambios de tarifa e intervalo se auditan con actor, fecha y valores anterior/nuevo, sin guardar solamente la última intervención.

Precisión aprobada el 7 de noviembre de 2026: la tarifa por hora debe ser estrictamente positiva y tener hasta dos decimales; la menor tarifa representable es USD 0.01/h. Se rechazan cero, negativos y valores con más de dos decimales en lugar de redondearlos silenciosamente. No se modifica el cálculo por segundos, el redondeo de importes finales ni la regla de no enviar operaciones externas de importe cero.

Valores iniciales aprobados expresamente para pruebas el 7 de noviembre de 2026:

| Código | Tarifa USD/h | Intervalo preventivo |
| --- | --- | --- |
| MONTANA | 10.00 | 3 meses |
| PASEO | 8.00 | 2 meses |
| ELECTRICA | 15.00 | 1 mes |

Se incorporarán únicamente en CargaCatalogosRefactor, bajo el perfil refactor, y solo si falta el código; no deben sobrescribir valores ya existentes. No constituyen precios comerciales definitivos ni recomendaciones técnicas del fabricante. Su aprobación no aplica todavía la nueva tarifa al código anterior ni modifica contratos existentes.

Mapeo físico de la entidad creada y comprobada: id Integer con IDENTITY; codigo VARCHAR(20), obligatorio y único, normalizado a mayúsculas con Locale.ROOT; precio_por_hora_usd DECIMAL(10,2), obligatorio, con hasta ocho dígitos enteros y dos decimales; intervalo_preventivo_meses INTEGER obligatorio y positivo. El máximo USD 99,999,999.99/h es el límite técnico de esa columna, no una tarifa comercial propuesta. Se conservan los cuatro campos del DER sin agregar version o relaciones inversas en esta etapa. No se implementa todavía actualización administrativa del catálogo ni su estrategia de bloqueo/auditoría. Las pruebas de entidad no acreditan aún DDL/persistencia ni integración con Bicicleta; el avance efectivo está en el documento 37.

La última preventiva satisfactoria se obtiene de las órdenes; para una bicicleta nueva sin revisiones se utiliza su fecha real de alta. Crear, cancelar o cerrar sin éxito una orden no reinicia el calendario. Un legado sin referencia fiable requiere revisión inicial según RN-MAN-04. No se agrega por ahora otra columna de última preventiva que pueda contradecir el historial.

Los cambios de tarifa e intervalo también necesitan control de concurrencia al implementar. Puede utilizarse bloqueo transaccional o proponerse version para este catálogo mutable; no se afirma que la ausencia/presencia de esa columna implemente por sí sola la consistencia. No se añadió un campo nuevo sin contrastar la estrategia física del conjunto.

## 4. BICICLETA y su catálogo de estados

| Campo existente en v5 | Clave y condiciones de revisión |
| --- | --- |
| id | PK; identidad interna estable de la unidad y sus referencias históricas |
| codigo_bicicleta | UNIQUE y obligatorio; normalización al implementar; la baja no elimina la fila ni permite reutilizar silenciosamente el código para otra bicicleta |
| tipo_bicicleta_id | FK obligatoria a TIPO_BICICLETA.id; no implica guardar otra tarifa en la unidad |
| estado_bicicleta_id | FK obligatoria a ESTADO_BICICLETA.id; no acepta un estado inexistente o una transición libre enviada por el cliente |
| borrado_bici | Booleano obligatorio, inicialmente false; archivo lógico independiente de estado y ubicación, con las condiciones de RN-INV-01 |
| estacion_id | FK opcional a ESTACION.id; representa presencia física actual, no el último lugar conocido |
| fecha_alta | Referencia real de ingreso a flota; obligatoria para altas nuevas; tratamiento del legado sin fecha pendiente en D06 |
| version | Control optimista propuesto; las operaciones deben proteger coherentemente la unidad y su contrato vigente |

ESTADO_BICICLETA conserva id como PK y codigo único, con los siete estados aprobados: DISPONIBLE, RESERVADA, EN_USO, MANTENIMIENTO, INACTIVA, DE_BAJA y EXTRAVIADA. No se agregan estados PREVENTIVA o CORRECTIVA: corresponden al tipo de orden.

Una DISPONIBLE requiere estación activa válida y aptitud técnica sin impedimentos. Una RESERVADA aún no retirada conserva su presencia y ocupación. Al retirar pasa a EN_USO y su estación actual queda vacía. Una MANTENIMIENTO puede estar dentro de una estación o fuera de ella; ese estado no demuestra un traslado. No se toma estacion_id nulo como prueba de pérdida o robo ni se inventa una estación para cerrar un incidente.

La aclaración del usuario y su aprobación del 6 de noviembre de 2026 corrigen el borrador: se conserva borrado_bici como en el DER v4. DE_BAJA es un estado operativo, no un borrado lógico. Ninguno sustituye el registro de un movimiento físico. No se elimina el flag del código ni se convierte automáticamente una bicicleta borrada en DE_BAJA. Las inconsistencias de legado se revisarán antes de migrarlas, sin perder filas o historial.

Se conservan las relaciones de la unidad con alquileres, órdenes e HISTORIAL_ESTADO_BICICLETA. Los cambios de estado conservan el historial correspondiente. Los traslados o salidas físicas que no cambien el estado también requieren trazabilidad: AUDITORIA_SISTEMA debe registrar sobre la BICICLETA su ubicación anterior/nueva, actor, fecha y motivo; no se genera una transición de estado ficticia para registrar un movimiento. No se necesita agregar otra tabla para el alcance actual si esos eventos se conservan de forma completa y consultable.

## 5. RN-EST-01 Baja lógica de estaciones aprobada

El 28 de octubre de 2026 el usuario aprobó este recorrido y remarcó «siempre y cuando sea una baja lógica, nada de borrado físico». Queda aprobada la política de baja de estación, sin extender esa aprobación a otras decisiones pendientes del inventario:

1. Solo ADMIN puede dar de baja lógicamente una estación, conservando motivo, actor y fecha en auditoría.
2. No se permite mientras tenga bicicletas físicamente asociadas, cualquiera sea su estado. Primero deben resolverse las reservas incompatibles y registrarse los movimientos físicos autorizados; no se trasladan unidades ficticiamente por dar de baja la estación.
3. Tampoco se permite mientras exista un detalle RESERVADO o PENDIENTE_RETIRO vigente cuyo retiro esté previsto allí. Se verifican los detalles persistidos y su vencimiento, no solamente el conteo de bicicletas, y no se cancela ni reembolsa automáticamente al intentar dar de baja.
4. Las referencias históricas no impiden la baja cuando ya no hay stock ni retiros pendientes: se conserva la fila para no romper alquileres y trazabilidad. No se exige borrar o terminar viajes que ya salieron solo por haber partido de esa estación.
5. Una estación de baja no admite nuevas reservas de retiro ni ingresos de bicicletas. La operación se coordina con reserva, retiro e ingreso para que solicitudes simultáneas no eludan las comprobaciones.

La baja conserva la fila de ESTACION y cambia borrado_estacion de false a true, junto con la auditoría de la operación. No ejecuta borrado físico ni cascadas de eliminación, ni borra bicicletas, contratos, pagos, historiales o autorías. Los valores anteriores/nuevos, el responsable ADMIN, la fecha y el motivo se registran mediante AUDITORIA_SISTEMA, sin agregar una tabla o guardar solamente la última modificación.

Las consultas históricas deben poder resolver la estación referenciada aunque esté de baja; el filtro de estaciones habilitadas se aplica al uso operativo, no como una eliminación de su historia. La baja tampoco vacía ubicaciones ni modifica datos anteriores para aparentar que nunca existió.

El formulario de edición no sustituye ese recorrido transaccional. No se agregan una entidad ESTADO_ESTACION, reactivación automática ni cierres temporales automáticos. La aprobación se documenta y se refleja en el modelo, pero no acredita que el servicio nuevo esté implementado o probado.

## 6. Criterios para implementar y probar

Estos casos están especificados, no ejecutados:

- CP-INV-01: modificar capacidad por debajo de la ocupación física se rechaza, incluida una bicicleta DE_BAJA que aún esté dentro.
- CP-INV-02: dos ingresos compiten por el último espacio y solo uno se confirma; el otro no deja efectos parciales.
- CP-INV-03: retirar una reservada elimina su ubicación actual; cancelar antes del retiro termina la exclusividad pero no libera espacio físico.
- CP-INV-04: modificar la tarifa del tipo cambia nuevas contrataciones, no los precios ya aplicados de sus detalles.
- CP-INV-05: vencimiento preventivo y una orden abierta se gestionan conforme a RN-MAN-04 sin crear duplicados ni poner una unidad restringida a disposición.
- CP-INV-06: estado y ubicación son comprobables por separado; un traslado sin cambio de estado conserva su evento de inventario.
- CP-INV-07: intentar dar de baja una estación con stock o retiro pendiente se rechaza sin cancelaciones automáticas.
- CP-INV-08: estación vacía con solo referencias históricas puede darse de baja lógicamente, conservando sus relaciones y auditoría.
- CP-INV-09: la baja cambia el indicador y registra actor, fecha y motivo; no elimina físicamente filas ni ejecuta cascadas. Las consultas históricas autorizadas siguen identificando la estación original.

Las longitudes, precisión decimal, índices, estrategia de bloqueo y datos iniciales se concretarán junto con D02, D05 y D06. La identificación y RN-EST-01 no aprueban automáticamente todos los campos físicos del inventario. Sigue revisar ORDEN_MANTENIMIENTO, TIPO_MANTENIMIENTO y sus estados e historial, respetando las reglas de mantenimiento ya aprobadas y conservando los parámetros numéricos todavía pendientes.

## 7. RN-INV-01 Taller interno y borrado lógico aprobados

Actualización del 8 de noviembre de 2026: RN-INV-02, sección 8, sustituye las
condiciones de archivo/borrado y salida separada que figuran a continuación.
La sección 7 conserva la decisión anterior como antecedente; para el nuevo
recorrido Eliminar rige la sección 8. El resto de permisos y restricciones continúa.

Fecha de aprobación: 6 de noviembre de 2026. Estado: aprobado para el diseño y reflejado en el DER lógico; implementación y pruebas del nuevo criterio pendientes.

### Representación y acceso

Se reutiliza ESTACION para ambas ubicaciones, sin crear otra tabla. es_publica es obligatorio: true corresponde a una estación pública y false a un Taller/depósito interno. El nombre visible Taller o Taller mantenimiento puede cambiar sin alterar esta clasificación. Las condiciones de actividad, dirección, coordenadas, capacidad positiva y baja lógica se conservan; no se aprueba capacidad ilimitada para el depósito.

Precisión de alcance aprobada el 7 de noviembre de 2026: por el momento, el uso se elige al crear la estación y se muestra sin permitir cambios en la edición común. No se incluye una operación de reclasificación pública/interna en esta etapa. Esto conserva el comportamiento implementado, no establece una imposibilidad técnica ni una prohibición definitiva; habilitar una reclasificación futura requerirá definir sus condiciones operativas y trazabilidad antes de modificar el código. No agrega tablas, columnas, claves ni estados.

El mapa, las opciones de contratación y las estaciones de devolución para CLIENTE incluyen solo estaciones públicas activas. El backend también debe rechazar el Taller como origen o devolución de una petición de cliente enviada directamente. Ocultarlo en el frontend no basta. ADMIN puede consultar las ubicaciones internas y registrar sus movimientos físicos autorizados; esos movimientos no se confunden con alquileres o devoluciones de clientes.

Precisión de permisos aprobada el 6 de noviembre de 2026: TECNICO puede consultar, dentro de sus órdenes de mantenimiento asignadas, la ubicación de la bicicleta, incluido un Taller interno. Este acceso de lectura no concede administración de estaciones, gestión de movimientos ni consulta del listado administrativo completo. La ruta /api/admin/estaciones se mantiene exclusiva de ADMIN; la consulta técnica se implementará con el módulo de mantenimiento y comprobará la asignación en el backend, conforme a RN-MAN-02. No se agrega por ahora una ruta técnica provisional ni se modifica el DER. Esta consulta todavía no está implementada.

Las FK conservadas no garantizan por sí solas la clasificación pública/interna ni las condiciones cruzadas de baja, archivo y ubicación. Esos controles deberán comprobar la ubicación vigente y confirmarse transaccionalmente con la unidad y su trazabilidad, coordinados con reservas, movimientos y cambios de capacidad. Actualizar el dibujo no los implementa en la base o el backend.

### Estado borrado y ubicación independientes

- Solo ADMIN puede registrar DE_BAJA o borrado_bici=true y únicamente cuando la bicicleta esté físicamente registrada en un Taller activo. Se exige motivo y se conservan las restricciones de reservas, viajes y órdenes abiertas y los recorridos de incidencias ya aprobados. No se cierran operaciones silenciosamente para permitir la acción.
- DE_BAJA retira definitivamente la unidad del servicio; no marca borrado_bici por sí solo. El borrado lógico archiva el registro, sin convertirlo automáticamente a DE_BAJA o INACTIVA. Se conservan los siete estados y no se agrega BORRADA al catálogo.
- INACTIVA puede aplicarse en la estación donde esté, incluida una pública, bajo RN-BIC-02. No exige traslado al Taller y no se usa para eludir una reserva, viaje, falla o revisión pendiente.
- DISPONIBLE y RESERVADA mantienen sus recorridos aprobados; una reservada sigue ocupando su estación hasta el retiro. MANTENIMIENTO puede permanecer en una estación pública esperando atención o traslado, o estar en el Taller. El trabajo efectivo conserva los controles de asignación y acceso físico a la unidad.
- La clasificación interna impide ofrecer una bicicleta del Taller a clientes, incluso si su estado es DISPONIBLE. También se excluyen las archivadas y los demás impedimentos ya aprobados. No se cambia de estado automáticamente por renombrar o clasificar una ubicación.
- Una EXTRAVIADA todavía no recuperada no satisface la condición de Taller: conserva EXTRAVIADA y no se inventa una recepción o ubicación para darla de baja o archivarla. Esta precisión restringe la excepción anterior de baja sin recepción de RN-INC-01, sin alterar el cierre del detalle ni su tratamiento económico. Una recuperación real mantiene su revisión y los demás controles antes de cualquier baja posterior.

### Presencia física y trazabilidad

El inventario físico consultable por ADMIN incluye las bicicletas que siguen en el Taller, incluso DE_BAJA o borradas lógicamente. El borrado puede excluirlas de listados operativos vigentes, pero no debe impedir su consulta histórica ni la gestión autorizada de su salida física. No oculta alquileres, pagos, deudas o reembolsos asociados.

Mientras estacion_id apunte al Taller, la unidad ocupa lugar. Solo una salida real registrada permite vaciar esa FK y liberar espacio. Un traslado con destino conserva la FK de la ubicación receptora, exige capacidad y respeta los impedimentos de la unidad. Cambiar estado o borrado no simula un movimiento. Las acciones administrativas conservan actor, fecha, motivo y valores anterior/nuevo en AUDITORIA_SISTEMA; las transiciones conservan además HISTORIAL_ESTADO_BICICLETA. El archivo sin transición no crea un cambio de estado ficticio.

No se aprueban por extensión nuevas reactivaciones de DE_BAJA o mecanismos de restauración de registros archivados. Tampoco se ejecutan migraciones, precargas de Taller o cambios de configuración mediante este documento.

### Criterios de aceptación de RN-INV-01

Estos escenarios se especifican, no se ejecutaron:

- CP-INV-10: un cliente no ve ubicaciones internas en mapa o selectores y no puede contratar o devolver en Taller mediante HTTP directo.
- CP-INV-11: una solicitud de DE_BAJA o borrado lógico desde una estación pública se rechaza sin cambios parciales; desde Taller exige ADMIN y los demás controles.
- CP-INV-12: una inactivación válida en una estación pública conserva la ubicación y no exige trasladar la unidad.
- CP-INV-13: dar de baja o archivar una unidad en Taller conserva su presencia, ocupación, referencias y acceso administrativo; solo su salida física real libera lugar.
- CP-INV-14: las nuevas acciones no liberan RESERVADA o EN_USO ni cierran órdenes o incidencias para eludir sus recorridos; no se inventa un Taller para una EXTRAVIADA ausente.
- CP-INV-15: estado, borrado y movimiento conservan la auditoría correspondiente sin borrar historia ni generar transiciones ficticias.
- CP-INV-16: TECNICO puede ver la ubicación de la bicicleta, incluido el Taller, en una orden asignada a él, pero no obtiene por ello acceso al listado administrativo ni permisos para modificar estaciones. La consulta de órdenes ajenas se rechaza conforme a RN-MAN-02.

## 8. RN-INV-02 Eliminación con salida definitiva y presentación del inventario

Fecha: 8 de noviembre de 2026. Recorrido físico implementado en la etapa 128;
pruebas automáticas verificadas en la 129 y comprobaciones manuales en las
etapas 130 a 145 del documento 37. RN-INV-04, sección 10, amplía el origen
de la retirada física: pública o interna activa. Esta ampliación está
implementada en la etapa 146, con pruebas automáticas en la 147 y comprobaciones
manuales en las etapas 148 a 152. La pérdida
definitiva de la sección 9 sigue pendiente de código.

Se mantienen las siete categorías de estado y las columnas existentes.
DE_BAJA y borrado_bici siguen siendo datos diferentes, pero el nuevo recorrido
Eliminar combina expresamente la retirada física definitiva y el borrado lógico.
Sustituye el archivo que conservaba estado/ubicación y el CP-INV-13 anterior
en lo referente al borrado. No modifica los originales PDF/Word/DERv4.

### Selector de estaciones

- Mostrar uso y espacios libres calculados por backend. Deshabilitar destinos
  sin capacidad, excepto la ubicación actual durante una edición.
- Conservar la comprobación transaccional del backend; el selector no garantiza
  que el espacio continúe libre al guardar. No permitir ingresos nuevos en
  estaciones borradas ni excluir físicamente presentes para simular cupos.

### Operaciones y salida física

- Inactivar es una pausa: conserva presencia física y ocupación.
- Dar de baja retira del servicio: conserva ubicación/ocupación mientras la
  unidad siga en Taller y no activa borrado_bici automáticamente.
- Eliminar exige ADMIN activo, versión, motivo y confirmación de que la unidad
  ya fue retirada físicamente del inventario. Requiere una ubicación previa
  registrada en una estación pública o interna habilitada y ausencia de
  procesos incompatibles (ampliación RN-INV-04).
  Esta es la causa de retirada física; RN-INV-03, sección 9, define la
  excepción aprobada por pérdida definitiva sin recepción.
- En una sola transacción deja estado DE_BAJA, borrado_bici true y estacion_id
  nulo. No obliga a pulsar antes Dar de baja: desde una situación permitida
  puede registrar esa transición dentro de la propia eliminación.
- Registrar responsable, fecha, motivo y ubicación anterior/nueva en auditoría;
  agregar historial de estado solo si realmente cambia a DE_BAJA. Si ya estaba
  DE_BAJA, no duplicar una transición ficticia.
- No cerrar viajes, reservas, órdenes o incidencias para permitir la eliminación.
  EXTRAVIADA sin recuperación no satisface presencia física previa y no usa
  esta retirada física: requiere el recorrido específico de la sección 9.
- No se agrega un botón obligatorio de salida ni el endpoint separado propuesto
  anteriormente. La excepción propuesta para eliminar una unidad ya sin ubicación
  por una salida independiente no forma parte de esta alternativa.
- No se borran filas, relaciones, contratos, obligaciones ni eventos históricos.
  No se restauran eliminadas ni se reactiva DE_BAJA. El conteo físico continúa
  basado en ubicaciones reales, no en ignorar filas por su indicador.

### Listado y criterios de aceptación

- Sustituir el texto Archivar por Eliminar, explicando la confirmación real y
  el borrado lógico. Ocultar eliminadas en el listado habitual.
- Mostrar eliminadas permite a ADMIN consultar también esas filas y su historia;
  no concede edición, restauración, contratación ni nuevas facultades.
- Una eliminación física válida libera exactamente el lugar de la ubicación anterior
  y conserva evidencia de la salida. Un rechazo no deja cambios parciales.
- Una DE_BAJA todavía en Taller conserva ocupación hasta eliminarla con salida
  real confirmada. Una eliminación no convierte la falta física en espacio
  ficticio: la confirmación administrativa debe corresponder a la realidad.
- Los registros de pruebas o históricos creados con el recorrido anterior no se
  reinterpretan ni se liberan masivamente por modificar este documento. Cualquier
  tratamiento de filas previas requiere comprobar su situación física real.

La estructura del DER no cambia: continúan 29 entidades y 55 relaciones.
Los comentarios de ambos Mermaid se actualizan con la regla, no se ejecuta SQL.

## 9. RN-INV-03 Baja y eliminación por pérdida definitiva

Fecha de aprobación: 8 de noviembre de 2026. Implementación pendiente de incidencias.

### Alcance y diferencia con EXTRAVIADA

EXTRAVIADA representa pérdida o robo confirmados y unidad todavía no recuperada;
no significa retiro definitivo automático. No se ofrece para alquilar ni se
asocia a una estación ficticia. El mero reporte del cliente no crea ese estado:
se mantiene la revisión y cierre excepcional de RN-INC-01.

Si la unidad se recupera antes de la baja definitiva, se registra recepción y
ubicación reales con capacidad y pasa a MANTENIMIENTO. No se habilita por encontrarla.

### Baja administrativa sin recepción física

ADMIN activo puede resolver EXTRAVIADA a DE_BAJA por pérdida definitiva:

- Incidencia de pérdida/robo confirmada y vinculada al detalle de la misma
  bicicleta; decisión administrativa explícita y motivo obligatorio.
- Detalle del viaje cerrado por su recorrido excepcional, sin devolverlo
  ficticiamente, y ausencia de operaciones pendientes incompatibles.
- Control de versión y transacción con sus bloqueos al implementar.
- Estado DE_BAJA, ubicación nula y borrado_bici false hasta una eventual
  eliminación lógica posterior. No se requiere pasar por Taller ni se
  registra una salida física de un lugar que nunca ocupó.
- Historial EXTRAVIADA a DE_BAJA y auditoría ADMIN con causa de pérdida,
  decisión, incidencia vinculada, actor, fecha y motivo verificables.

No se fija un plazo automático de días ni una baja por simple demora.
No se cierran otros procesos para hacer pasar la validación. Esta decisión
no elimina deudas, pagos o reembolsos ni atribuye culpa/indemnizaciones.
El cierre operativo no se confunde con una liquidación o cobranza completadas.

### Eliminación lógica posterior

ADMIN podrá eliminar una DE_BAJA originada en esa pérdida definitiva sin exigir
Taller o confirmar una retirada material inexistente. Debe verificarse el
respaldo de la misma bicicleta/incidencia y la decisión de baja, no solamente
DE_BAJA con estacion_id nulo ni un motivo libre enviado por el navegador.

La eliminación conserva DE_BAJA, ubicación nula, historial y relaciones; marca
borrado_bici true y registra la actuación administrativa con causa de pérdida.
No duplica un cambio de estado ni incrementa los cupos de ninguna estación.
La confirmación en pantalla debe distinguir pérdida definitiva de retirada física.
Se mantienen los mismos botones generales, con el recorrido autorizado según
la situación y sus antecedentes; no se habilita edición libre de EXTRAVIADA.

Una recuperación posterior a DE_BAJA no la reactiva ni restaura automáticamente.
La eventual revisión excepcional no pertenece al recorrido normal aprobado.

### Estructura, implementación y criterios pendientes

- No se agregan tablas, estados o columnas en esta aprobación: siguen las
  29 entidades/55 relaciones del DERv5. La prueba de la causa deberá usar
  incidencias, su detalle, los historiales y la auditoría existentes en el diseño.
- Se implementará al conectar incidencias. Hoy el servicio de inventario
  conserva el bloqueo de EXTRAVIADA; no se agrega una excepción sin respaldo.
- Verificar: reporte sin validación no permite baja; viaje activo no se
  libera; baja por pérdida confirmada no exige Taller; eliminación posterior
  requiere su respaldo; no aparece ocupación ni salida ficticia; versiones,
  roles y eventos son coherentes; encontrarla no reabre el viaje ni la habilita.
- Se sustituyen las prohibiciones absolutas anteriores de baja sin recuperación,
  no los controles de recepción/capacidad del recorrido físico RN-INV-02.
  No se reinterpretan pérdidas ni bajas históricas automáticamente.

## 10. RN-INV-04 Retirada definitiva desde la ubicación registrada

Fecha de aprobación e implementación: 8 de noviembre de 2026, etapa 146 del
documento 37. Verificación: 50 pruebas Java y 15 Node en etapa 147; casos
manuales puntuales y persistencia en etapas 148 a 152. Alcance y evidencia
en sección 9 del documento 39, sin extender resultados a módulos pendientes.

- Eliminar con retirada física confirmada admite como origen la estación
  pública o interna activa donde ya está registrada la bicicleta. No obliga
  a simular un traslado al Taller antes de retirarla definitivamente.
- Se valida y bloquea la estación de origen dentro de la misma transacción.
  La falta de ubicación o un origen inexistente/inactivo impiden la operación.
  No se exige espacio libre para sacar una unidad ya presente.
- Se mantienen ADMIN activo, versión vigente, motivo, confirmación explícita,
  ausencia de alquiler activo y estados permitidos DISPONIBLE, INACTIVA o
  DE_BAJA. No se habilitan EN_USO, RESERVADA, MANTENIMIENTO ni EXTRAVIADA.
- El resultado conserva RN-INV-02: DE_BAJA, borrado_bici true y ubicación nula,
  fila e historia conservadas; auditoría con origen real, actor, fecha y motivo.
  Historial de estado solo si hay transición. No hay borrado físico de datos.
- Dar de baja sin eliminar sigue exigiendo Taller activo y no libera lugar.
  Una recogida para reparación es traslado, no eliminación definitiva.
- Se sustituye la restricción de Taller únicamente para Eliminar del recorrido
  físico, incluidos CP-INV-11 y la antigua expectativa INV02-07. Los rechazos
  anteriores se conservan como evidencia histórica, no como resultado vigente.
  RN-INV-03 por pérdida sigue pendiente de incidencias, sin atajos ni falsas
  confirmaciones físicas. Los módulos futuros deberán conectar sus controles
  de reservas, órdenes e incidencias; no se declaran integrados por este cambio.

No cambian entidades, columnas, PK, FK, relaciones, endpoints ni permisos.
