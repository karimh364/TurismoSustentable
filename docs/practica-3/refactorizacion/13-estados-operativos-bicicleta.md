# RN-BIC-01: estados operativos de la bicicleta

Regla vigente adicional aprobada el 8 de noviembre de 2026: RN-INV-03
(documento 26, sección 9) permite a ADMIN la baja EXTRAVIADA a DE_BAJA por
pérdida definitiva confirmada y trazada, con detalle cerrado y sin procesos
incompatibles, sin exigir Taller ni inventar recepción. La eliminación lógica
posterior requiere ese respaldo y no libera cupos ficticios. Sustituye las
prohibiciones absolutas de baja sin recuperación de la revisión del 6 de
noviembre. Su código queda pendiente de integrar incidencias.

Actualización aprobada el 8 de noviembre de 2026: RN-INV-02, sección 8 del
documento 26, reemplaza el archivo que conservaba ubicación. Eliminar exige
confirmación de retirada física y registra conjuntamente DE_BAJA,
borrado_bici true y estacion_id nulo, con auditoría e historial si cambia el
estado. Dar de baja sin eliminar sigue conservando la ocupación. Las menciones
anteriores al borrado sin salida corresponden a la regla sustituida.
No se agregan estados ni se implementa el cambio mediante esta anotación.

Fecha de aprobación: 25 de octubre de 2026.

Estado: seis estados iniciales aprobados y ampliación posterior RN-INC-01 con EXTRAVIADA. El catálogo de diseño tiene siete estados. RN-BIC-02 aprueba la administración y RN-INC-01 el circuito mínimo de incidencias; la precisión económica de su sección 8 también está aprobada. No implementado todavía en Java ni en una base de datos.

Actualización aprobada el 6 de noviembre de 2026: [RN-INV-01, sección 7](26-revision-estaciones-tipos-y-bicicletas.md) conserva borrado_bici independiente del estado y exige Taller para registrar DE_BAJA o borrado lógico. INACTIVA puede aplicarse en la estación actual; no se agregan estados ni se implementan estas reglas mediante este documento.

## 1. Estados aprobados

| Estado | Significado |
| --- | --- |
| DISPONIBLE | En una estación, apta para alquilar y sin una reserva incompatible |
| RESERVADA | Apartada para una contratación vigente, todavía sin retirar |
| EN_USO | Retirada y vinculada a un viaje en curso |
| MANTENIMIENTO | Fuera de alquiler por revisión o reparación |
| INACTIVA | Deshabilitada temporalmente por una decisión administrativa, sin representar una revisión ni una baja definitiva |
| DE_BAJA | Retirada definitivamente de la flota operativa, conservando su registro, relaciones e historial |
| EXTRAVIADA | Pérdida o robo confirmados y bicicleta todavía no recuperada, sin disponibilidad ni baja definitiva automática |

La diferencia entre INACTIVA y DE_BAJA fue explicada al usuario antes de aprobar el catálogo completo: la primera permite una futura vuelta al servicio bajo controles; la segunda expresa un retiro definitivo en el recorrido normal.

DE_BAJA no equivale a eliminar físicamente de la base una bicicleta ni sus registros asociados. El tratamiento de una eventual corrección administrativa de una baja errónea queda fuera del recorrido normal y requiere permisos específicos.

## 2. Relación con reservas y viajes

Las operaciones aprobadas de alquiler deben mantener coherencia entre el estado de la bicicleta y el de su detalle:

- Confirmar una contratación reserva la unidad disponible de forma exclusiva.
- Un retiro válido cambia la unidad reservada a EN_USO, registra el inicio real y la saca de la ocupación de la estación.
- Una cancelación o vencimiento sin retiro libera únicamente la reserva correspondiente, sin alterar reservas posteriores.
- Una devolución válida sin falla reportada ni preventiva vencida u otro impedimento termina el uso y deja la bicicleta DISPONIBLE en la estación de devolución.
- Una devolución válida con falla reportada termina el uso y deja la bicicleta en MANTENIMIENTO, fuera de alquiler para revisión según RN-ALQ-06.

La ampliación aprobada en [RN-MAN-04](19-propuesta-preventivo-por-calendario.md) impide nuevas reservas cuando venció la preventiva, aunque la tarea periódica no haya actualizado el estado visible. Conserva los viajes y las reservas ya aceptadas con su plazo original. Al devolver o liberar una reserva sin retiro, una preventiva vencida exige MANTENIMIENTO, aun sin reporte de falla, sin seguir cobrando uso al cliente. No duplica órdenes abiertas ni altera una INACTIVA o DE_BAJA para ofrecerla nuevamente.

Cambiar un estado manualmente no puede sustituir una devolución. Una bicicleta EN_USO no se libera, da de baja ni vuelve a ofrecerse a otro cliente para eludir el cierre de su viaje.

RN-INC-01 agrega un cierre excepcional específico por ADMIN con reporte, motivo y corte validado. Solo después de ese cierre y según la situación real se registra EXTRAVIADA o MANTENIMIENTO; no equivale a devolverla. Mientras solo haya un reporte pendiente, no se libera la unidad ni se elude el detalle activo. Se conserva la diferencia entre estado del contrato, presencia física y situación financiera.

## 3. Ubicación y capacidad

La disponibilidad para alquilar y la presencia física no son la misma cosa.

Una unidad RESERVADA sigue en su estación y ocupa un lugar, pero no puede contratarla otro cliente. Una unidad EN_USO no ocupa un lugar de estación porque ya se retiró.

Una unidad en MANTENIMIENTO o INACTIVA que siga ubicada en una estación también ocupa lugar. Cambiar el estado no equivale a trasladarla físicamente ni libera capacidad por sí solo.

DE_BAJA y borrado_bici son independientes y se registran solo desde un Taller interno activo bajo RN-INV-01. No cambian ni vacían la ubicación automáticamente. Una unidad archivada o DE_BAJA sigue ocupando su lugar en el Taller hasta que se registre una salida física real y debe seguir siendo consultable por ADMIN. INACTIVA no exige traslado al Taller. El Taller no se ofrece como origen o devolución de clientes; DISPONIBLE y RESERVADA conservan sus recorridos aprobados.

## 4. Relación con el mantenimiento

La bicicleta utiliza el estado general MANTENIMIENTO. El motivo PREVENTIVO/CORRECTIVO se conserva en TIPO_MANTENIMIENTO y el avance se conserva en el estado de cada ORDEN_MANTENIMIENTO conforme a [RN-MAN-01](12-tipos-y-estados-mantenimiento.md).

Una unidad no vuelve automáticamente a DISPONIBLE por crear, cancelar o finalizar una orden. Debe respetarse el resultado de la revisión y su aptitud, ubicación y restricciones vigentes.

El 28 de octubre se aprobó en [RN-MAN-02](17-permisos-y-transiciones-mantenimiento.md) que TECNICO registre el resultado de sus órdenes asignadas y ADMIN autorice después la transición de MANTENIMIENTO a DISPONIBLE, con revisión favorable vigente, estación activa válida y sin impedimentos. Esa aprobación no incluye las otras reactivaciones, la inactivación ni la baja administrativa.

La INACTIVA administrativa no es una vía para saltarse una revisión técnica pendiente. Las operaciones de habilitación deben verificar los impedimentos aplicables, no aceptar libremente el estado enviado por el frontend.

Una EXTRAVIADA recuperada pasa a MANTENIMIENTO con recepción y ubicación reales
y revisión antes de habilitar. No se habilita por encontrarla ni se inventa
una estación. Si permanece ausente, RN-INV-03 permite baja por pérdida
definitiva con incidencia confirmada, decisión/motivo de ADMIN, detalle cerrado
y sin operaciones incompatibles; conserva ubicación nula. No es automática ni
modifica por sí sola la situación financiera. Una recuperación posterior a
DE_BAJA no revierte la baja ni restaura el registro automáticamente.

## 5. Criterios de aceptación

Estos casos se especifican para pruebas futuras, no se ejecutaron.

| ID | Escenario | Resultado esperado |
| --- | --- | --- |
| CP-BIC-01-01 | Se confirma una reserva válida de una bicicleta disponible | Queda RESERVADA y no se ofrece a otro cliente durante la exclusividad |
| CP-BIC-01-02 | Se retira una unidad bajo una reserva y pago válidos | Queda EN_USO y ya no ocupa lugar en la estación de retiro |
| CP-BIC-01-03 | Se cancela o vence una reserva sin retiro | La unidad se libera respetando la reserva correspondiente; conserva su presencia física |
| CP-BIC-01-04 | Devolución válida sin falla reportada ni preventiva vencida u otro impedimento | Queda DISPONIBLE en la estación de devolución y termina el costo de uso |
| CP-BIC-01-05 | Devolución válida con falla reportada | Queda MANTENIMIENTO, termina el costo de uso y no se ofrece para otro alquiler |
| CP-BIC-01-06 | Una unidad MANTENIMIENTO o INACTIVA sigue en una estación | Sigue contándose como ocupación física, aunque no esté disponible para alquilar |
| CP-BIC-01-07 | Se intenta liberar o dar de baja una unidad EN_USO sin devolución | No se permite eludir el viaje activo mediante un cambio manual de estado |
| CP-BIC-01-08 | Una bicicleta se da de baja por el recorrido autorizado desde Taller | No se elimina su registro histórico, no se marca borrado lógico automáticamente ni se alteran sus referencias de alquileres/mantenimiento |
| CP-BIC-01-09 | Se cierra una orden pero la bicicleta aún tiene una restricción técnica | No se habilita automáticamente por el solo cierre de la orden |

## 6. Decisiones todavía pendientes

Los puntos administrativos 1 a 6 de [RN-BIC-02](20-propuesta-administracion-y-excepciones-bicicletas.md) fueron aprobados expresamente el 28 de octubre: solo ADMIN inactiva, reactiva, da de baja o cancela operativamente una unidad todavía reservada, con los controles de esa regla. INACTIVA no oculta impedimentos técnicos; la baja no elimina historia ni libera un espacio si la bicicleta sigue allí. Una bicicleta EN_USO no se libera mediante un cambio administrativo directo.

- [RN-INC-01](21-propuesta-minima-incidencias-en-viaje.md) ya incorpora el circuito mínimo de pérdidas/accidentes y EXTRAVIADA. La precisión económica de su sección 8 también está aprobada; quedan los detalles físicos de reporte/recepción, sin atribución automática de responsabilidad ni cargos por daños.
- Migración de ALQUILADA/INACTIVA y revisión de los indicadores actuales, conservando borrado_bici separado de DE_BAJA conforme a RN-INV-01. No se reinterpretan registros históricos automáticamente ni se elimina el flag por considerarlo redundante.

No se modificó código, base de datos ni los documentos originales. No se incorporó un cierre automático por 24 horas de demora.
