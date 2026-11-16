# Guion de exposición y demostración

Turismo Sustentable. Karim Homsi. Prácticas Profesionalizantes III. 16/11/2026.

## Orden para unos treinta minutos

- 8 minutos: frontend funcionando, con los datos mínimos de prueba.
- 9 minutos: diapositivas 1 a 13. Las 14 a 16 son anexos para preguntas.
- 5 minutos: trazabilidad en MariaDB mediante las consultas del archivo SQL.
- 8 minutos: preguntas y debate. Ajustar el ritmo según el interés del profesor.

## Antes de salir

1. Mantener un respaldo SQL fuera del repositorio. No poner contraseñas ni backups en Git.
2. Iniciar MariaDB y la app con el comando de validate indicado más abajo.
3. Confirmar que la app responde en http://localhost:8080 y que no hay errores de arranque.
4. Probar las sesiones de qa.circuito (CLIENTE+ADMIN) y qa.tecnico (solo TECNICO). Las contraseñas son las elegidas al registrarlos; no se incluyen aquí.
5. Comprobar QA-CIR-01 disponible en Plaza Circuito QA, capacidad 2. Verificar los dos alquileres saldados y la orden #1 finalizada/apta.
6. Abrir el PPTX y el PDF de respaldo. Tener el DER completo disponible para ampliar.
7. Verificar conexión a los recursos web: Bootstrap, Axios, SweetAlert2 y la cotización pueden depender de Internet. No ejecutar pruebas MySQL destructivas antes de exponer.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=refactor --spring.jpa.hibernate.ddl-auto=validate"
```

El perfil mantiene create-drop por defecto. Este argumento lo reemplaza solo para ese arranque. No reutilizar el comando anterior si se necesitan conservar datos.

## Primera parte Frontend

1. Mostrar Inicio y explicar que la propuesta apunta a residentes y visitantes de Río Grande. No atribuir resultados ambientales medidos.
2. Entrar con la cuenta CLIENTE+ADMIN. Explicar que alquila por CLIENTE y que ADMIN solo no obtiene ese permiso.
3. Mostrar la bicicleta disponible, el tipo y la estación pública. Si se hace un viaje nuevo, reservar una unidad por 60 minutos, confirmar el anticipo ficticio y retirarla antes de 15 minutos.
4. Señalar la diferencia entre vencimiento de reserva, fecha estimada de devolución, devolución real y fin de uso.
5. Devolver en Plaza Circuito QA sin falla. Mostrar la liquidación y resolver el reintegro. Las cifras de un nuevo viaje serán distintas: no prometer que coincidan con las del ejemplo anterior.
6. Si falta tiempo, mostrar los alquileres #1 y #2 ya saldados y explicar el recorrido realizado. No fabricar una incidencia en vivo si no hay margen para revisarla.
7. Abrir la incidencia #1. Mostrar el hecho informado, la decisión y el corte validado, separados de la recepción física.
8. Mostrar la orden #1 y la reparación. En la ventana privada del técnico, abrir Historial y comprobar que no aparecen snapshots JSON. En ADMIN sí se conservan.

No hay que repetir todo mantenimiento en vivo: la orden terminada y sus eventos permiten seguir el recorrido sin arriesgar una demostración demasiado larga.

## Segunda parte Diapositivas

### 01 Turismo Sustentable

Presentar la propuesta y pasar primero al front. La imagen es conceptual.

### 02 Objetivo y alcance del parcial

La propuesta se dirige a residentes y visitantes de Río Grande. Se presenta una solución académica local, no un servicio municipal desplegado. El alcance original incluye QR y candado electrónico, pero el recorrido actual de retiro se confirma en la web. Los pagos usan saldo ficticio, sin tarjetas locales ni dinero real.

### 03 Roles y responsabilidades

Una cuenta puede tener varios roles. ADMIN por sí solo no puede alquilar. Una cuenta ADMIN+CLIENTE puede alquilar por su permiso CLIENTE y únicamente sobre sus propios alquileres. TECNICO debe estar activo y asignado a la orden. Las autorizaciones no dependen de que un botón se vea o no. Fuente adicional: WebAuthorization y los servicios de alquiler y mantenimiento.

### 04 Arquitectura de la aplicación

El navegador usa HTML, CSS, JavaScript, Bootstrap, Axios y cuadros SweetAlert2. Los endpoints reciben DTO con validaciones. Los servicios aplican reglas, control de acceso y transacciones. Spring Data JPA e Hibernate persisten en MariaDB local. No se accede directamente a la base desde el navegador. La sesión la gestiona Spring Security en el servidor. El precio definitivo y los estados los calcula el backend, nunca el JavaScript.

### 05 DER v5 y relaciones centrales

Vista parcial y simplificada para exponer. El modelo lógico completo contiene 29 entidades y 55 relaciones. El esquema físico aún conserva campos del alquiler anterior. No presentar esta diapositiva como las 55 relaciones completas. USUARIO tiene muchos ALQUILER; ALQUILER tiene uno a muchos ALQUILER_DETALLE; cada detalle referencia una BICICLETA y el TIPO_BICICLETA contratado. PAGO pertenece a ALQUILER y REEMBOLSO a un PAGO. El detalle puede tener reportes de INCIDENCIA, que pueden vincularse a una ORDEN_MANTENIMIENTO. El archivo DER_v5_completo.pdf incluye todas las entidades y relaciones del Mermaid.

### 06 Estados operativos y situación financiera

Los estados se guardan en catálogos relacionados por FK. La situación financiera se calcula, no agrega una tabla nueva. FINALIZADO significa que terminó el uso de todas las unidades, pero aún puede haber revisión económica, deuda o reembolso. Un reporte pendiente puede dejar SIN_LIQUIDAR. La devolución nunca se impide por una deuda previa. No confundir PAGO APROBADO con ALQUILER EN_PROGRESO. Un pago no acredita un retiro físico.

### 07 Catálogos y ocupación física

Los catálogos normalizan roles, estados y tipos. Tipo de mantenimiento y resultado técnico no son estados de la orden. APTA no habilita por sí sola la bicicleta. La tarifa vigente por tipo se copia al detalle al reservar. es_publica es un booleano que separa estaciones de clientes de espacios internos. La ocupación se calcula por presencia física, no por disponibilidad. Una bicicleta en mantenimiento ocupa un lugar si permanece registrada en esa estación. Exigir taller para toda reparación se propuso como mejora y se difirió.

### 08 Tarifa y cotización conservadas

Al reservar, cada detalle guarda la tarifa USD del tipo contratado y el alquiler guarda cotización ARS/USD, fuente y fecha. El proveedor actual es DolarAPI, dólar tarjeta, valor venta. El costo usa segundos completos, sin mínimo. USD y ARS se redondean por separado a dos decimales; no se convierte a pesos un USD ya redondeado. El siguiente ejemplo es la prueba manual #1, no una tarifa comercial definitiva ni una cotización de mercado vigente.

### 09 Incidencias durante el viaje

CLIENTE titular reporta avería, accidente, pérdida o robo. ADMIN también puede cargar un aviso recibido por otro canal. Reportar no corta por sí solo el tiempo. ADMIN valida con motivo y un corte entre el retiro y el reporte. Una avería validada puede cerrar el detalle sin simular devolución, dejar la bici en mantenimiento y vincular una orden. La recuperación posterior no reabre el uso ni modifica la fecha de corte. En la prueba #2 fueron seis segundos completos hasta el corte, ARS 27,30 y reembolso confirmado ARS 16.352,70. No presentar esa cifra como precio mínimo.

### 10 Mantenimiento y vuelta al servicio

El administrador crea y asigna una orden. El técnico asignado inicia, pausa o reanuda, registra diagnóstico, trabajo y costos internos, y finaliza APTA o NO_APTA. Solo puede existir una orden abierta por bicicleta. Al inactivar un técnico se pausa/desasigna según el estado, conservando su historial. El costo de repuestos es interno y no carga automáticamente al cliente. El administrador habilita después de una revisión favorable, ubicación válida y ausencia de impedimentos. La implementación actual permite reparación con presencia registrada también en una estación pública; la restricción exclusiva a internas está diferida.

### 11 Dos casos de uso documentados

Las fichas completas se entregan en el informe. CU-01 toma el caso original C-02 del PDF y lo actualiza a las decisiones vigentes: varias bicicletas, reserva de 15 minutos, pagos simulados y retiro web sin QR físico. CU-02 describe el objetivo del técnico de reparar una orden asignada. La asignación y habilitación ADMIN aparecen como precondición e intervención posterior, no como permisos del técnico. Cada ficha incluye precondiciones, disparador, flujo, alternativas, postcondiciones y criterios de aceptación.

### 12 Pruebas y controles de consistencia

Los números corresponden a la regresión dirigida de etapa 339, no al total único del proyecto: AuditoriaAdministrativaServiceTest 15, EstacionServiceTest 17, EstacionControllerTest 26, UsuarioServiceTest 12, UsuarioControllerTest 6, WebAuthorizationTest 21 y MantenimientoServiceTest 58 suman 155. Dos archivos de frontend suman 37 pruebas JavaScript de la tanda anterior. No sumar estas tandas con ejecuciones previas. Las unitarias no prueban por sí solas rollback SQL; la comprobación real de las nuevas auditorías se verifica por separado. El proyecto también posee integraciones MariaDB documentadas en los reportes 41 a 43. La prueba manual usó una cuenta CLIENTE+ADMIN y otra TECNICO, no CLIENTE puro. Las pruebas de Playwright de otra materia no se cuentan como evidencia de este bloque.

### 13 Estado de entrega y próximos pasos

Se demuestra un circuito local con dinero ficticio. La migración no está terminada al 100%. Permanecen consumidores y campos del modelo anterior, correo de comprobantes heredado, y preparación de producción. Etapa 339 incorpora auditoría de creación, edición y borrado lógico de estaciones y edición de usuarios, con 155 pruebas dirigidas aprobadas. Ese resultado no acredita por sí solo persistencia/rollback SQL ni cierre de seguridad transversal. Se difirieron las reparaciones exclusivamente en talleres y el ajuste visual del desplazamiento al abrir diálogos. No hay despliegue en nube ni candado físico. Nunca prometer reducción medida de emisiones o usuarios reales.

### 14 Anexo  Catálogos y estados

Referencia para preguntas del profesor. Los nombres son los códigos vigentes del diccionario. REEMBOLSO usa el atributo estado; INCIDENCIA usa estadoRevision. La situación financiera se deriva de liquidación, pagos y reembolsos. No hay una tabla ESTADO_SITUACION_FINANCIERA. Tipos y roles se explicaron antes.

### 15 Anexo  Endpoints del circuito nuevo

Son rutas de la API que reciben acciones del frontend. El verbo expresa una operación, no un acceso directo a una tabla. Las rutas de alquiler aplican propiedad y CLIENTE salvo la devolución administrativa autorizada. Los cambios de mantenimiento conservan ADMIN o TECNICO según la acción. El contrato exacto y sus DTO se revisan en los controllers. No se muestran las rutas heredadas como la interfaz definitiva.

### 16 Anexo  Claves y decisiones del modelo

USUARIO_ROL usa una PK compuesta usuario_id, rol_id. EmbeddedId reúne las dos columnas y MapsId vincula cada asociación con su parte de la clave. Evita repetir un rol para la misma cuenta. ALQUILER_DETALLE tiene una PK propia y una unicidad sobre alquiler_id/bicicleta_id. PERSONA tiene unicidad sobre tipo de documento, país emisor y número normalizado. Conservar tarifa y tipo contratados impide que cambiar un catálogo reescriba el historial. La tarifa del catálogo por tipo no reemplaza el precio aplicado guardado en el detalle.

## Tercera parte Base de datos

Usar Consultas_Demostracion_SOLO_LECTURA.sql. Ejecutar los bloques individualmente. No seleccionar password, tokens, hashes ni credenciales.

Mostrar primero cabecera y detalles, luego pagos/reembolsos y por último la orden/incidencia. Las fechas nuevas se guardan en UTC; la interfaz las muestra en hora local. Una hora distinta en la tabla no demuestra por sí sola un error.

## Respuestas breves para el debate

- ¿Por qué cabecera y detalle? Una reserva reúne varias bicicletas y cada una conserva su retiro, devolución y costo. La cabecera reúne cliente, origen y cotización común.
- ¿Por qué guardar el cambio aplicado? Para reconstruir el importe histórico aunque cambie la cotización o la tarifa vigente.
- ¿Por qué el pago conserva el monto original? Porque cobrar y devolver son movimientos distintos. El reembolso no reescribe el cobro.
- ¿Por qué FINALIZADO no significa SALDADO? Uno representa el fin del uso; el otro, la diferencia económica resuelta.
- ¿Qué pasa si reporta una falla? El reporte no corta por sí solo el cobro. ADMIN valida el fundamento y la fecha del corte.
- ¿Por qué no se habilita al terminar una reparación? El técnico informa aptitud y ADMIN controla la vuelta al servicio con las demás restricciones.
- ¿Se reparan bicicletas en estaciones públicas? Hoy se admite con presencia registrada. Exigir estación interna para todo trabajo quedó propuesto y diferido.
- ¿Una bici en mantenimiento ocupa espacio? Sí, mientras su ubicación física siga en esa estación. El estado no demuestra un traslado.
- ¿Está terminado el DER? El circuito nuevo está integrado y probado en el alcance mostrado. Falta retirar de forma coordinada el legado y preparar producción.
- ¿Los pagos son reales? No. Se simulan con saldo ficticio. Mercado Pago externo queda para una etapa posterior.
- ¿Hay QR y candado? Están previstos en el alcance inicial, pero el parcial usa retiro desde la web y no controla un candado físico.
- ¿Por qué tantas pruebas? Las reglas no se comprueban solo mirando botones. Se ensayan permisos, errores, conflictos, concurrencia y relaciones persistidas.

## Última comprobación con la rama principal

La etapa 339 incorpora auditoría ADMIN de estaciones y edición de usuarios, con 155 pruebas dirigidas aprobadas. Su comprobación manual/SQL se verifica aparte; esos casos no acreditan rollback SQL real. Este paquete no modifica servicios ni supone que la migración entera esté cerrada. Confirmar el estado final de la principal antes de exponer.
