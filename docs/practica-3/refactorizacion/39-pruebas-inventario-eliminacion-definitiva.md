# Pruebas del inventario: RN-INV-02

Fecha: 8 de noviembre de 2026.

Estado: implementación preparada; todas las ejecuciones de este bloque quedan a
cargo del usuario. Este archivo no acredita resultados superados.

Actualización de resultados del 8 de noviembre: la ejecución automática y los
recorridos manuales realizados están detallados al final, con referencia al
diario. La matriz sigue siendo una especificación, no se marca íntegramente
aprobada sin distinguir el tipo de evidencia.

Ampliación RN-INV-04 (etapa 146): Eliminar admite estación pública o interna
activa como origen físico registrado. Las nuevas pruebas automáticas pasaron
(etapa 147, detalle en sección 9); su prueba manual sigue pendiente.
Los resultados de la sección 8 pertenecen a la
versión anterior, incluida la antigua restricción de Taller. Ver sección 9
para comprobar puntualmente el cambio sin repetir toda la ronda.

Los resultados ejecutados se registran en el documento 37, no se infieren de
esta matriz. RN-INV-03, aprobada el 8 de noviembre, añade baja/eliminación por
pérdida definitiva sin Taller y con incidencia/decisión respaldadas. Sigue
pendiente de integrar incidencias y no está cubierta como función implementada
por esta ronda de retirada física. No forzar EXTRAVIADA por SQL ni fingir
una recepción o confirmación de salida para intentar probarla.

## 1. Alcance y precauciones

- Probar solo la base local de QA turismo_sustentable y el perfil refactor.
- No usar Eliminar con el backend anterior todavía encendido: los recursos
  estáticos pueden actualizarse antes de reiniciar Java.
- Primero guardar y ejecutar las pruebas automáticas. No ejecutar test sin
  filtros: el test general de contexto no forma parte de esta ronda aislada.
- Después de resultados correctos se realizará un reinicio controlado. El
  perfil create-drop elimina las tablas/datos de prueba al detener/iniciar.
  Si se necesita conservar algo, acordar copia/perfil antes del reinicio.
- No cambiar ddl-auto, ejecutar DROP, inventar movimientos, restaurar eliminadas
  o quitar referencias para que una prueba pase.
- No se agregan tablas/columnas. EliminarBicicletaDTO es entrada HTTP, no entidad.

## 2. Pruebas automáticas Java

Desde la raíz del proyecto:

```powershell
.\gradlew.bat test --tests "CargaCatalogosRefactorTest" --tests "TipoBicicletaTest" --tests "BicicletaControllerTest" --tests "BicicletaServiceTest" --tests "AlquilerServiceTest" --tests "WebAuthorizationTest" --tests "EstacionControllerTest" --tests "EstacionServiceTest"
```

Esperado: BUILD SUCCESSFUL y cero fallos, errores y omisiones en los reportes
build/test-results/test y build/reports/tests/test/index.html. La salida
UP-TO-DATE conserva resultados; no equivale a una nueva ejecución.

Cobertura preparada:

- Borrado con motivo, versión y salidaFisicaConfirmada=true; rechazo si falta
  cualquier dato, la confirmación es false, el origen es inexistente/
  inhabilitado, la versión cambió o hay alquiler/estado incompatible.
- Transición a DE_BAJA y ubicación nula sin borrar físicamente la fila, junto
  con auditoría de ubicación anterior/nueva y actor real.
- Unidad que ya estaba DE_BAJA: eliminar sin duplicar transición de estado.
- Eliminar directamente una DISPONIBLE/INACTIVA válida desde pública o Taller,
  sin pulsar Dar de baja previamente. Dar de baja por separado sigue exigiendo
  Taller y conserva ubicación y ocupación.
- Estado de catálogo faltante: rechazo anterior a cualquier mutación.
- Listado habitual excluye eliminadas; incluirEliminadas=true las permite
  consultar. GET por ID sigue siendo administrativo, sin restauración/edición.
- ADMIN puede consultar/eliminar; anónimo recibe 401; CLIENTE y TECNICO 403,
  incluso agregando el parámetro para incluir eliminadas.
- Regresiones de tarifas, alquileres, estaciones y capacidad física existentes.

Los dobles de repositorios no prueban el bloqueo real de MySQL ni el rollback
de una transacción JPA. La atomicidad se implementa con @Transactional y se
debe contrastar con los recorridos reales y su persistencia.

## 3. Pruebas automáticas del frontend sin navegador ni base

Se agregó src/test/js/InventarioBicicletas.test.cjs con Node Test Runner y un
DOM/Axios simulados que ejecutan el admin.js real. No instala dependencias.

Si Node está en PATH:

```powershell
node --check src/main/resources/static/js/admin.js
node --test src/test/js/InventarioBicicletas.test.cjs
```

Alternativa con el runtime disponible en esta computadora:

```powershell
& 'C:\Users\Karim\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --check 'src/main/resources/static/js/admin.js'
& 'C:\Users\Karim\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe' --test 'src/test/js/InventarioBicicletas.test.cjs'
```

Esperado: todas las pruebas correctas, sin escrituras HTTP reales. Cubren
destinos llenos, excepción de ubicación actual, disponibilidad faltante,
motivo de traslado, filtro, solo consulta de eliminadas, cancelación y payload
de eliminación con confirmación, errores y ausencia de cambios en las otras
peticiones. No sustituyen comprobar colores/foco y controles nativos en navegador.

## 4. Preparación manual después del reinicio acordado

1. Verificar arranque refactor en 8080 y catálogos precargados.
2. Registrar cuenta QA y asignar ADMIN sin quitar CLIENTE mediante el
   procedimiento manual ya utilizado. Reingresar para cargar permisos.
3. Crear una estación pública QA con capacidad 2 y un Taller QA con capacidad 5.
4. Crear QA-01 MONTANA en la pública y QA-02 PASEO en el Taller.
5. Mantener la aplicación encendida y recargar el panel con Ctrl+F5.
6. Para concurrencia, usar dos pestañas A/B con la misma sesión y formularios
   independientes. No se requiere una segunda cuenta ni otra sesión.
7. Anotar IDs y versiones. Si se cambia un código en una prueba, seguir la
   misma unidad por ID; no crear un duplicado para aparentar continuidad.

## 5. Matriz manual

| Caso | Acción | Resultado esperado |
| --- | --- | --- |
| INV02-01 | Abrir alta de bicicleta | Estación obligatoria; uso y cupos visibles; estado/tarifa bloqueados; motivo oculto |
| INV02-02 | Editar QA-02 en A y seleccionar la pública con 1 espacio; no guardar. Crear QA-03 en esa pública desde B | La pública queda llena; la información ya abierta de A puede quedar desactualizada |
| INV02-03 | Guardar el traslado pendiente de A con motivo | Rechazo por capacidad; QA-02 sigue en Taller y sin cambios parciales ni evento de éxito |
| INV02-04 | Abrir otro alta/traslado | La pública llena aparece deshabilitada; el Taller con lugar sigue permitido |
| INV02-05 | Editar QA-01, que ya está en la pública llena, y cambiar solo su código | La estación actual permanece habilitada y se guarda allí sin consumir otro lugar ni exigir motivo de traslado |
| INV02-06 | Elegir otro destino y después volver al actual | El motivo aparece al trasladar y se oculta/limpia al cancelar el cambio de ubicación |
| INV02-07 | Eliminar una unidad QA adicional desde la pública con motivo y confirmación de retirada real; no usar QA-01 que se necesita en los pasos siguientes | RN-INV-04: DE_BAJA, borrado true, ubicación nula; libera un lugar y conserva fila, historial y auditoría del origen público |
| INV02-08 | Trasladar QA-01 al Taller, con motivo | La pública libera un lugar; el Taller consume uno; cambio administrativo trazado |
| INV02-09 | Inactivar y habilitar esa unidad | Cambia estado con motivo e historial; no se mueve ni libera espacio |
| INV02-10 | Dar de baja QA-01 en Taller | DE_BAJA, borrado false, mismo Taller y misma ocupación; continúa en el listado habitual |
| INV02-11 | Abrir Eliminar y cancelar, o dejar motivo vacío | No se guarda ni libera lugar; el aviso explica la retirada real y el borrado lógico |
| INV02-12 | Confirmar Eliminar de QA-01 con motivo de retirada real | DE_BAJA, borrado true y estación nula; libera un lugar; no se borra la fila |
| INV02-13 | Alternar Mostrar eliminadas | Por defecto QA-01 no aparece; marcada aparece como Eliminada/Fuera del inventario y solo ofrece Ver |
| INV02-14 | Consultar esa eliminada | Datos bloqueados; no habilitar, editar ni restaurar; no se inventa una estación |
| INV02-15 | Eliminar QA-02 desde Taller sin darla de baja antes | Transición a DE_BAJA y salida/borrado juntos; exactamente un cambio de estado |
| INV02-16 | Abrir confirmación de eliminación en A; antes de confirmar, modificar esa misma unidad desde B | Al confirmar A, conflicto por versión sin salida/borrado parcial. Ejecutar con una unidad todavía vigente, antes de INV02-15, o una QA adicional |
| INV02-17 | Consultar API de inventario/eliminadas sin sesión o con CLIENTE/TECNICO | 401 anónimo, 403 rol sin permiso; las rutas de clientes no ofrecen eliminadas ni Taller |
| INV02-18 | Revisar historial/auditoría de las eliminaciones | Actor, motivo, fecha y ubicación anterior/nueva correctos; una DE_BAJA previa no añade otra transición ficticia |

En la simulación QA no hay bicicletas físicas. La confirmación representa ese
evento de prueba. En uso real, ADMIN solo debe confirmarla cuando la unidad
haya sido retirada; el software no puede demostrar por sí solo una salida material.

Para EN_USO, RESERVADA, MANTENIMIENTO y EXTRAVIADA, los rechazos se cubren en
pruebas automáticas; no cambiar estados por SQL para fingir procesos reales.
Los módulos nuevos de reservas/mantenimiento/incidencias siguen pendientes.
Cuando existan se integrarán además sus comprobaciones de operaciones abiertas.

## 6. Evidencia de persistencia: consultas de solo lectura

Ejecutar después de las operaciones, sin modificar la base. Los prefijos QA
delimitan el inventario de prueba; adaptar el filtro si se eligieron otros códigos.

```sql
USE turismo_sustentable;

SELECT b.id, b.codigo_bicicleta, eb.codigo AS estado,
       b.borrado_bici + 0 AS eliminada, b.estacion_id, b.version
FROM bicicleta b
JOIN estado_bicicleta eb ON eb.id = b.estado_bicicleta_id
WHERE b.codigo_bicicleta LIKE 'QA-%'
ORDER BY b.id;

SELECT e.id, e.nombre, e.capacidad,
       COUNT(b.id) AS bicicletas_presentes,
       GREATEST(e.capacidad - COUNT(b.id), 0) AS espacios_libres
FROM estacion e
LEFT JOIN bicicleta b ON b.estacion_id = e.id
GROUP BY e.id, e.nombre, e.capacidad
ORDER BY e.id;

SELECT h.id, h.bicicleta_id, anterior.codigo AS estado_anterior,
       nuevo.codigo AS estado_nuevo, h.fecha_hora_cambio,
       h.actor_usuario_id, h.origen, h.observaciones
FROM historial_estado_bicicleta h
JOIN bicicleta b ON b.id = h.bicicleta_id
LEFT JOIN estado_bicicleta anterior ON anterior.id = h.estado_anterior_id
JOIN estado_bicicleta nuevo ON nuevo.id = h.estado_nuevo_id
WHERE b.codigo_bicicleta LIKE 'QA-%'
ORDER BY h.id;

SELECT a.id, a.administrador_id, a.entidad_id, a.accion,
       a.valores_anteriores, a.valores_nuevos, a.motivo, a.fecha_operacion
FROM auditoria_sistema a
JOIN bicicleta b ON a.entidad_afectada = 'BICICLETA' AND b.id = a.entidad_id
WHERE b.codigo_bicicleta LIKE 'QA-%'
ORDER BY a.id;
```

No quitar borradas/DE_BAJA del conteo físico para fabricar cupos. Con el nuevo
recorrido, su salida real deja estacion_id nulo y por eso ya no se cuentan.
Los registros previos al cambio no se liberan masivamente: necesitan revisión
de su situación real antes de cualquier tratamiento específico.

## 7. Registro de resultados

Guardar por caso: fecha, ejecutor, preparación, pasos, resultado esperado,
resultado obtenido y evidencia (captura o consulta sin secretos). Marcar
pendiente/no ejecutado cuando corresponda, no aprobado por el solo hecho de
existir la prueba. No incluir contraseñas, hashes, tokens o credenciales.

## 8. Resultados de la ronda del 8 de noviembre de 2026

- Automáticos: 121 Java sin fallos/errores/omisiones verificados por XML y
  15 Node aprobados en la salida aportada por el usuario; etapa 129.
- Preparación y altas con sus eventos reales: etapas 130/131.
- INV02-02/03: rechazo por capacidad ocupada después de abrir el formulario;
  QA-02 permanece en origen y no hay eventos parciales, etapas 132/133.
- INV02-04/05: usuario confirmó destino lleno deshabilitado; edición del
  código en estación actual llena se verificó con versión/auditoría, etapa 134.
- INV02-07/08: eliminación pública rechazada bajo la restricción anterior y
  traslado justificado al Taller con ocupación correcta y sin falsa transición,
  etapas 135/136. Resultado histórico: RN-INV-04 sustituye esa expectativa de
  rechazo; la eliminación desde pública debe volver a probarse.
- INV02-09/10/11: pausa/habilitación, baja con presencia conservada y
  cancelación/motivo vacío sin eliminación, etapas 137/138/139.
- INV02-12/18: eliminación de una DE_BAJA con salida registrada y sin repetir
  el cambio de estado, etapa 140. Consulta de eliminadas/Ver bloqueado y
  ocultamiento al desmarcar (INV02-13/14), etapa 142.
- INV02-16: edición válida en B y rechazo posterior de la eliminación antigua
  de A sin pérdida de cambios ni liberación de espacio, etapas 143/144.
- INV02-15/18: eliminación directa desde DISPONIBLE deja DE_BAJA, borrado true,
  ubicación nula, transición única y auditoría; Taller cero presentes/cinco
  libres, etapa 145.
- INV02-01 cuenta con altas reales y pruebas de validación; no se declara una
  inspección visual exhaustiva adicional de todos los controles en esta ronda.
  El escenario completo INV02-06 está cubierto en Node; no se dio por ejecutado
  manualmente el retorno al origen. INV02-17 tiene evidencia automática de
  filtros reales Spring Security en WebAuthorizationTest, no una nueva ronda
  manual completa de cada rol en el proceso arrancado.
- RN-INV-03 es una regla aprobada para el módulo futuro de incidencias; no se
  implementó ni se probó una baja por pérdida definitiva mediante este bloque.

## 9. Comprobación puntual pendiente de RN-INV-04

Resultado automático del 8 de noviembre (etapa 147): el usuario ejecutó ambos
filtros Java con BUILD SUCCESSFUL y cuatro tareas ejecutadas. Los XML confirman
BicicletaControllerTest: 15 y BicicletaServiceTest: 35, todos sin fallos,
errores u omisiones; timestamps 17:25:25.347Z y 17:25:26.766Z. La salida Node
aportada confirma 15 aprobadas, cero fallos/omisiones; --check no informó errores.
El asistente corroboró los reportes sin volver a ejecutar las pruebas.
No se acredita todavía reinicio, prueba manual ni persistencia real del cambio.

Primero ejecutar, sin levantar ni reiniciar todavía la aplicación:

```powershell
.\gradlew.bat test --tests "BicicletaServiceTest" --tests "BicicletaControllerTest"
$nodeQA = 'C:\Users\Karim\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe'
& $nodeQA --check '.\src\main\resources\static\js\admin.js'
& $nodeQA --test '.\src\test\js\InventarioBicicletas.test.cjs'
```

Se prepararon pruebas unitarias de eliminación desde pública con ubicación
anterior/nueva auditadas y sin exigir cupo libre; rechazo de origen público
inactivo/inexistente; Dar de baja desde pública todavía rechazado. Permanecen
las de Taller, ubicación ausente, confirmación, motivo, versión, alquiler,
estados incompatibles y permisos. Node comprueba el nuevo texto del diálogo.
Estas pruebas unitarias no acreditan bloqueo/concurrencia real contra MySQL.

Después de resultados correctos, acordar reinicio controlado: Java necesita
reinicio para aplicar el servicio; create-drop descartará los datos QA. No
usar el nuevo texto del frontend como evidencia de un backend ya actualizado.

Preparar con ADMIN una pública y un Taller activos, y dos bicicletas QA nuevas
DISPONIBLE en la pública; registrar su ocupación inicial y versiones.

Preparación corroborada con SELECT (etapa 148): Plaza QA Retiro id 1 activa,
pública, capacidad 2/dos presentes/cero libres; Taller QA Retiro id 2 activo,
interno, capacidad 5/cero presentes/cinco libres. QA-RET-01 id 1 MONTANA y
QA-RET-02 id 2 PASEO, ambas DISPONIBLE, borrado false, version 0 y estación 1.
La cuenta admin.bicicletas tiene ADMIN y CLIENTE. INV04-01 ejecutado y
corroborado con SELECT (etapa 149): rechazo sin cambios; QA-RET-01 conserva
DISPONIBLE, estación 1, borrado false, version 0 y solo sus eventos de alta.
Ocupación sin cambios. INV04-02 verificado en su variante cancelar (etapa 150):
texto correcto informado por el usuario y SELECT sin cambios en QA-RET-01,
ocupación o cantidad de eventos. No se afirma otra ejecución manual de motivo
vacío. INV04-03 con persistencia verificada (etapa 151): QA-RET-01 conserva fila,
DE_BAJA, borrado true, estación nula y version 1; historial id 3 y auditoría
id 3 documentan transición y salida desde estación pública 1, con actor, fecha
y motivo. La pública quedó con una presente/un lugar libre y Taller vacío.
El usuario confirmó luego Mostrar eliminadas/Ver sin edición (etapa 152).
INV04-04 verificado: QA-RET-02 trasladada a estación 2 y dada de baja sin
eliminarse, version 2; auditoría id 4 de traslado, id 5 de baja e historial
id 4 DISPONIBLE a DE_BAJA. La pública quedó vacía/dos libres; Taller una
presente/cuatro libres. La baja conservó ubicación y ocupación física.
Quedan comprobados los cuatro casos en el alcance indicado (INV04-02 en su
variante cancelar), sin afirmar una nueva ejecución manual de motivo vacío
ni una nueva prueba de concurrencia simultánea. No se ejecutaron otras suites.

| Caso | Acción | Esperado |
| --- | --- | --- |
| INV04-01 | Intentar Dar de baja la primera unidad todavía en la pública | Rechazo por requerir Taller; sin cambios ni eventos de éxito |
| INV04-02 | Abrir Eliminar sobre esa unidad y cancelar o dejar motivo vacío | Texto desde su ubicación registrada; no guarda ni libera espacio |
| INV04-03 | Confirmar su retirada definitiva con motivo | Fila conservada, DE_BAJA, borrado true, estación nula; pública libera exactamente un lugar; auditoría del origen público y una transición real |
| INV04-04 | Trasladar la segunda unidad al Taller con motivo y después Dar de baja | Traslado real auditado; DE_BAJA con borrado false y ubicación/ocupación conservadas; no se debilita la regla de Taller |

Activar Mostrar eliminadas en INV04-03: solo Ver, ubicación Fuera del inventario.
Registrar resultados y SELECT sin secretos en el diario; no marcar aprobado
ningún caso antes de ejecutarlo. No forzar estaciones inactivas ni estados
por SQL: los rechazos de origen inválido se cubren aquí con repositorios simulados.
