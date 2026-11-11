# Revisión de coherencia de la migración al DER v5

Fecha: 11 de noviembre de 2026.

Actualización de implementación (etapas 253/254): R04, R05 y el permiso visual
de CLIENTE de R06 aplicados por autorización expresa y verificados con pruebas
dirigidas. XML: 204 pruebas Java aprobadas, tres MySQL omitidas; salida aportada
de Node: 22 aprobadas y check sin error. No nueva prueba SQL o de navegador manual.
El diagnóstico inferior conserva lo encontrado y su criterio de cierre; los
demás hallazgos y la presentación de importes/fechas de R06 siguen pendientes.

## 1. Alcance y conclusión

Revisión estática del Mermaid v5 completo, su entregable TXT, reglas aprobadas
de estados, composición, pagos y auditoría, entidades/repositorios y consumidores
actuales de usuarios, estaciones, bicicletas y alquileres. Se rastrearon referencias
Java y frontend. No se modificó Java, no se ejecutaron pruebas ni se consultó la
base en esta revisión. No es una auditoría exhaustiva de seguridad o concurrencia.

La estructura nueva es coherente con el diseño aprobado, pero la migración sigue
incompleta. No se puede declarar todo el backend conforme al DER ni sustituir
únicamente los listados: creación, devolución, estadísticas e inventario todavía
contienen consumidores del modelo anterior.

El Mermaid y `docs/practica-3/entregables/DERv5.txt` tienen el mismo SHA-256:
`733AD851E7E1A943AA03B73D186C7B902CC18D567082707D30F4D550CFC38B0E`.
Se mantienen 29 entidades y 55 relaciones del diseño. Tener 24 clases @Entity
correspondientes no acredita conformidad completa de campos, restricciones o reglas.

## 2. Qué está comprobado y qué no

| Apartado | Evidencia disponible | Límite |
| --- | --- | --- |
| Identidad, roles y registro | Persona, catálogos, UsuarioRol y autenticación por nickname; regresiones registradas | No significa que todas las operaciones de ADMIN/TECNICO estén implementadas |
| Inventario y estaciones | Servicios, validaciones, capacidad, versión, estados/borrado/presencia separados | Integración con detalle activo, incidencias y preventivo no terminada |
| Reserva nueva | Cabecera, detalles, tipo/tarifa/cambio congelados e historias en una transacción; integración MySQL previa | Método interno, sin alta HTTP nueva, deuda o preventivo comprobados |
| Consulta por ID | Servicio propietario/ADMIN, DTO y GET; pruebas unitarias, MVC y lectura MySQL previa | No sustituye listados ni demuestra todas las combinaciones de seguridad sobre proxy real |
| Borrado lógico de alquiler | Guardas de estados nuevos y antiguos, con bloqueo y pruebas | Traducción de errores del controlador y auditoría administrativa pendientes |
| Mantenimiento/incidencias | Entidades, catálogos, relaciones e historiales preparados | No hay todavía servicios/controladores de operación completos |
| Finanzas | Reglas y DER aprobados | Faltan EstadoPago, Pago, Reembolso y ambos historiales financieros en Java |
| Tarjetas/suscripción | Clases y referencias retiradas; clean y regresiones completadas | No se verificó el esquema físico tras la retirada |

Los 173 casos de regresión y los 21 de reserva posteriores a la limpieza se
ejecutaron en tandas diferentes. Los tres casos MySQL aprobados corresponden
a una ejecución anterior a retirar Tarjeta/Suscripcion. No se presentan como
una única corrida actual ni como prueba completa de la aplicación.

## 3. Hallazgos y acciones de cierre

### R01. Los listados antiguos no admiten nuevas cabeceras

- `AlquilerController.getAlquiler` y `AdminController.getAllAlquileres` devuelven
  `AlquilerDTO` anterior. Su constructor invoca `alquiler.getEstado().name()` y
  `alquiler.getBicicleta().getId()` sin admitir los null de una cabecera nueva.
- Si esos listados reciben una nueva reserva, el mapeo puede fallar. No se afirma
  que se haya reproducido mediante HTTP en esta revisión; se deriva del código.
- `mis_alquileres.js` y la sección alquileres de `admin.js` consumen esos campos
  planos. Cambiar solo la respuesta rompe esas pantallas.
- Cierre: contrato de lectura, autorización de propietario/ADMIN, DTO, controlador,
  frontend y pruebas deben migrarse conjuntamente. No ocultar silenciosamente
  alquileres antiguos ni fabricar detalles/cotizaciones para hacerlos encajar.

### R02. El circuito que crea y finaliza desde HTTP sigue siendo anterior

- POST `/api/alquileres` acepta AlquilerRequestDTO y llama a crearAlquiler: una
  bicicleta, inicio inmediato y enum INICIADO. El método crearReserva no está
  expuesto. La política CLIENTE del POST está corregida; eso no migra el negocio.
- PATCH `/api/alquileres/finalizar/{id}` usa alquiler.bicicleta y el enum anterior,
  consulta cambio al devolver y conserva el cálculo anterior. No finaliza detalles.
- El cliente de cotización antiguo y respaldo 1400 siguen en uso por ese circuito
  y GET `/api/cotizacion`; no eliminarlos dejando llamadas rotas.
- Cierre: anticipo validado, retiro por detalle, devolución y liquidación con
  snapshots, cancelación/vencimiento y efectos de incidencias según sus reglas.
  No activar el POST nuevo saltando deuda, vencimiento preventivo o pago previo.
- El reemplazo de listados debe coordinarse con este productor de datos. Un
  listado que solo incluya filas nuevas mientras el POST produce antiguas no
  es una migración funcional completa.

### R03. Inventario y estadísticas conservan dependencias del alquiler anterior

- BicicletaService.comprobarSinAlquiler consulta únicamente
  existsByBicicletaAndEstado(..., INICIADO). La guarda adicional de estado no
  editable hoy rechaza RESERVADA/EN_USO, pero no reemplaza una comprobación de
  los detalles activos del DER. No se declara una explotación demostrada.
- AlquilerRepository cuenta INICIADO de cabecera y agrupa preferencias por
  bicicleta.tipo actual. Debe consultar detalles y tipo contratado, conservando
  la definición del indicador y sin duplicar totales por joins.
- La relación Bicicleta.alquileres mappedBy bicicleta apunta al FK anterior
  en cabecera: retirarla al retirar ese FK, o reemplazarla solo si hay un consumidor
  real de una colección inversa de detalles. No crear relaciones redundantes.
- Cierre: adaptar consultas/consumidores y sus pruebas antes de eliminar enum,
  columnas y métodos anteriores.

### R04. Limpieza segura identificada, distinta del legado activo

- GeneroUsuario.java es un enum sin referencias externas encontradas en main,
  test o recursos; el catálogo vigente es Genero.
- UsuarioDTO.alquileres y BicicletaDTO.alquileres tienen getter pero no se llenan;
  BicicletaDTO además conserva el antiguo mapeo comentado. No se encontraron
  consumidores Java/JS de esas propiedades aparte de sus propias declaraciones.
- Cierre propuesto: retirar enum, ambos campos/getters y comentario/imports
  sobrantes, probar serialización y regresiones afectadas. No confundirlos con
  AlquilerDTO, que todavía tiene consumidores activos y NO debe borrarse ahora.

### R05. Borrado HTTP confunde errores distintos

- AlquilerController.borrarAlquilerLogico captura Exception y responde 404.
  Eso también atraparía ConfiguracionSistemaException y AccessDeniedException
  del servicio. La protección de estados no corrige esta traducción HTTP.
- Cierre: conservar 403 de seguridad y 500 interno saneado; 404 solo cuando el
  recurso no existe. Agregar casos MVC del DELETE, no reutilizar como evidencia
  las pruebas del GET o de método llamado directamente.

### R06. Presentación de permisos, fechas e importes por actualizar

- index.js define puedeAlquilar como ADMIN o CLIENTE. ADMIN solo puede ver
  controles de alquiler aunque el POST esté correctamente denegado sin CLIENTE.
  Es desalineación de interfaz, no prueba de autorización backend concedida.
- mis_alquileres.js presenta precio null como cero y utiliza Math.max(1, minutos)
  para el texto de devolución. No distingue importe pendiente de importe cero;
  esa presentación no corresponde al cálculo aprobado por segundos sin mínimo.
- Cierre: separar navegación administrativa de acciones de CLIENTE, mostrar
  importes pendientes explícitos y estados/fechas por detalle. No alterar el
  backend probado para acomodarlo a la interfaz anterior.

### R07. Trazabilidad aprobada no implementada en todas las acciones

- Inventario/tarifas registran auditoría ADMIN; crear/editar/eliminar estación,
  editar/inactivar usuarios y ocultar alquileres no registran aún la evidencia
  equivalente en sus rutas/servicios revisados. El documento 35 incluye estas
  acciones relevantes. No afirmar cobertura por existir AuditoriaSistema.
- HistorialEstadoBicicleta admite actor nulo en el mapeo pero su único constructor
  exige actor y fija USUARIO. Aún no soporta crear correctamente eventos SISTEMA
  para vencimiento/preventivo; no inventar un ADMIN para esos procesos.
- Cierre: auditoría de acciones ADMIN críticas junto con la escritura y soporte
  de origen/actor SISTEMA en los procesos automáticos, con pruebas específicas.

### R08. Reglas de módulos futuros son pendientes reales, no código muerto

- Sin servicios de mantenimiento/incidencia no están implementados el máximo de
  una orden abierta, trabajo por técnico asignado, revisión de incidencias,
  corte económico o recepción posterior a pérdida.
- Inactivar un usuario actualmente cambia activo desde UsuarioController; no
  pausa/desasigna sus órdenes según lo aprobado para TECNICO. Integrar esa
  dependencia al habilitar mantenimiento, sin declararla resuelta antes.
- Reserva nueva no comprueba aún deuda liquidada ni preventiva vencida. La
  existencia del intervalo en TipoBicicleta no implementa la política temporal.
- No eliminar estas entidades por no tener todavía endpoint: pertenecen al DER.
  Sí registrar su dependencia y no ofrecer una operación que requiera sus guardas
  antes de implementarlas.

### R09. Transición física y seguridad transversal pendientes

- Alquiler conserva campos/constructores antiguos y los nuevos permiten null
  para coexistir. La transición no es el esquema final: al retirar productores
  antiguos habrá que aplicar la obligatoriedad aprobada y eliminar columnas
  sustituidas de forma controlada. No aplicar NOT NULL ahora rompiendo el legado.
- USUARIO almacena hash de contraseña y hash de token, pero sus nombres físicos
  actuales no coinciden literalmente con password_hash/token_recuperacion_hash
  del DER. No se detectó contraseña/token originales guardados por esos servicios;
  distinguir una diferencia de nombre de una diferencia de protección del dato.
- Revalidación de cuenta activa y roles persistidos no es uniforme en toda la
  API; el nuevo servicio de alquiler sí la aplica. Seguridad actual también
  deshabilita CSRF. Requieren revisión transversal antes de considerar apto un
  despliegue, no una afirmación de seguridad completa por tests de roles.
- No se probaron concurrencia real entre conexiones ni todos los casos negativos
  del proxy del servicio. No desactivar open-in-view globalmente sin migrar
  también las lecturas fuera de servicio/transacción que aún lo requieren.

## 4. Documentación: diseño aprobado frente a estado de implementación

Los archivos 08, 22, 25 y 28 contienen fotos históricas con expresiones como
"no implementado" o cantidades de 28 entidades. Se conservan como antecedentes,
pero no deben usarse para deducir el estado actual del código. El modelo vigente
es el Mermaid v5 de 29 entidades y las decisiones posteriores aprobadas.

El documento 31 atribuía notificaciones repetidas a auditoría genérica; debe
interpretarse y corregirse conforme al documento 35: AUDITORIA_SISTEMA es solo
ADMIN, no un destino para toda notificación automática ni sustituto de historias
financieras. No agregar otra tabla o inventar una transición por esta revisión.

Seguimiento actual: este informe y el índice 00. Evidencia cronológica: documento
37. No se reabren reglas aprobadas por encontrar una descripción histórica vieja.

## 5. Orden de cierre propuesto

1. Limpieza R04 y tratamiento HTTP R05 en un bloque acotado verificable; corregir
   la presentación ADMIN/CLIENTE de R06 sin ampliar permisos.
2. Preparar el reemplazo coordinado de lecturas cliente/ADMIN y pantallas R01,
   incorporando los consumidores de inventario/estadísticas R03 al plan de retiro.
   No publicar un formato nuevo aislado mientras los productores siguen siendo
   incompatibles. Resolver explícitamente los registros de prueba anteriores al
   cambio, sin asumir una migración de datos de producción que no se ha hecho.
3. Completar el recorrido operativo/financiero nuevo con sus dependencias R02,
   R07 y R08. Cada entrega debe tener una operación usable y pruebas, no solo
   acumular modelos o métodos sin consumidor. Que pagos sea dependencia no permite
   prometer alquiler 100% cerrado antes de implementarlo.
4. Cambiar consumidores HTTP/frontend, dejar de producir el modelo anterior y
   retirar AlquilerDTO/AlquilerRequestDTO/enum/campos/consultas sustituidos cuando
   no queden usos. Verificar relaciones y esquema físico después del corte.
5. Verificación de extremo a extremo con CLIENTE/ADMIN/TECNICO según cada alcance,
   regresiones y concurrencia aplicables, documentación actual y checkpoint Git.

Condición para cerrar un bloque: regla aprobada + persistencia + servicio +
permisos/HTTP + consumidor aplicable + pruebas + retiro de lo sustituido. Una
dependencia aún pendiente debe aparecer aquí, no quedar implícita ni considerarse
terminada por compilar. No se autorizan eliminaciones o cambios Java por este informe.
