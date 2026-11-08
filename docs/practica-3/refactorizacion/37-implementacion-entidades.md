# Implementación de entidades

## Bloque 1 Catálogos de identificación

Estado: entidades incorporadas y revisadas estáticamente.
Compilación exitosa según la salida de terminal aportada por el desarrollador.
Pendientes las pruebas de validación y persistencia.

- Pais representa PAIS y TipoDocumento representa TIPO_DOCUMENTO del DER v5.
- Ambas tienen id INTEGER autogenerado y código obligatorio y único.
- Pais admite códigos de dos letras mayúsculas. La precarga deberá
  utilizar códigos ISO 3166-1 alfa-2 reales.
- El nombre del país es obligatorio y admite hasta 100 caracteres.
- TipoDocumento admite un código de hasta 10 caracteres y un nombre
  obligatorio de hasta 50 caracteres.
- Los tipos iniciales aprobados son DNI y PASAPORTE.
- Los códigos se normalizan con trim y mayúsculas; los nombres con trim.
- Estas longitudes corresponden a los catálogos, no al número de documento.
- Las relaciones desde Persona se declaran en JPA en el bloque 3.
  La carga inicial de los catálogos sigue pendiente.
- No se modifican Usuario, autenticación ni las entidades anteriores.
- La compilación no verifica tablas, restricciones ni conexiones con MySQL.

## Bloque 2 Catálogo de género

Estado: entidad incorporada y revisada estáticamente.
Compilación exitosa según la salida de terminal aportada por el desarrollador.
Pendientes las pruebas de validación y persistencia.

- Genero representa GENERO del DER v5.
- Tiene id INTEGER autogenerado y codigo obligatorio y único,
  con capacidad de hasta 20 caracteres.
- El código se normaliza con trim y mayúsculas.
- Los valores iniciales serán FEMENINO, MASCULINO, NO_BINARIO,
  OTRO y PREFIERO_NO_DECIR.
- La relación desde Persona se declara en JPA en el bloque 3.
  La carga inicial del catálogo sigue pendiente.
- GeneroUsuario se conserva temporalmente porque Usuario,
  sus DTO y la carga inicial anterior todavía lo utilizan.
- No se ha conectado este catálogo al registro ni al frontend.

## Bloque 3 Datos personales y documento

Estado: entidad incorporada y revisada estáticamente.
Compilación exitosa según la salida de terminal aportada por el desarrollador.
Pendientes las pruebas de validación y persistencia.

- Persona representa PERSONA del DER v5, con id INTEGER autogenerado.
- Nombre y apellido son obligatorios, admiten hasta 50 caracteres
  y conservan las validaciones del modelo anterior.
- Fecha de nacimiento es obligatoria y anterior a la fecha actual.
  No se establece una edad mínima de contratación.
- Genero, TipoDocumento y Pais se relacionan mediante tres FK
  obligatorias, con ManyToOne y carga LAZY, sin cascadas.
- Se declara uq_persona_documento sobre la combinación
  tipo_documento_id, pais_emisor_id y numero_documento.
- Las FK y la unicidad compuesta están declaradas en JPA;
  su generación y funcionamiento en MySQL todavía no se verificaron.
- NumeroDocumento es String de hasta 50 caracteres. Conserva
  ceros iniciales y letras; se recortan espacios externos
  y se normaliza a mayúsculas.
- La normalización y validación específica por tipo y país
  se completarán antes de conectar el nuevo registro.
- El DTO de registro se incorpora en el bloque 4, etapa 2.
  Los restantes DTO, servicios, repositorios, la precarga y las pruebas
  de persistencia quedan pendientes.
- La relación desde Usuario se incorpora en el bloque 4.
  El registro actual todavía no crea ni asigna Persona a la cuenta.

## Bloque 4 Adaptación incremental de Usuario

### Etapa 1 Relación con Persona

Estado: relación incorporada y revisada estáticamente.
Compilación exitosa según la salida de terminal aportada por el desarrollador:
`.\gradlew.bat compileJava`, con BUILD SUCCESSFUL en 2 segundos.
Pendientes las pruebas de validación y persistencia.

- Punto de partida: commit a5b88ed, con Persona incorporada. El 2 de
  noviembre de 2026 se comprobó que la referencia de la rama en GitHub
  coincide con el commit local y que el árbol de trabajo estaba limpio.
- Se incorporó persona a Usuario mediante OneToOne con carga LAZY,
  optional false y JoinColumn persona_id, obligatoria y única.
- La FK pertenece a USUARIO y referencia PERSONA.id. Su unicidad
  impide vincular la misma fila Persona a varias cuentas; la unicidad
  documental de Persona es una restricción distinta y se conserva.
- Se agregaron getPersona y setPersona. La relación no tiene cascadas
  de persistencia o borrado sobre Persona ni sobre los catálogos compartidos.
- Nombre, apellido, fecha de nacimiento y GeneroUsuario permanecen
  temporalmente en Usuario para conservar las referencias Java actuales.
  Esta duplicación es transitoria y no es el modelo final del DER v5.
- Autenticación por email, enum Rol, DTO, correos y controladores no
  se modificaron mediante este primer paso.
- La obligatoriedad de Persona exige adaptar el registro y la carga
  inicial antes de volver a ejecutarlos: los constructores antiguos
  todavía no asignan Persona. Una compilación exitosa no garantiza
  que el registro existente siga funcionando con esta relación nueva.
- Quedan pendientes la separación definitiva de datos personales,
  los restantes DTO y su integración, la creación transaccional de persona y cuenta,
  el nickname y su autenticación, los roles múltiples y los restantes
  ajustes de Usuario previstos en el DER v5.
- La verificación de esta etapa solo comprende revisión estática y
  la compilación comunicada. No acredita pruebas de contexto, registro,
  autenticación ni comprobación de esquema o datos en MySQL.

### Etapa 2 Contrato de registro y reglas del nickname

Estado: reglas del nickname aprobadas expresamente el 2 de noviembre de 2026.
RegistroUsuarioDTO está incorporado y revisado estáticamente.
Compilación exitosa según la salida de terminal aportada por el desarrollador:
`.\gradlew.bat compileJava`, con BUILD SUCCESSFUL en 1 segundo.
Todavía no se conectó al controlador. Sus validaciones generales y su
serialización cuentan con las diez pruebas unitarias verificadas en la etapa 3;
esto no acredita la integración del registro ni la persistencia.

#### Nickname aprobado

- Entre 3 y 30 caracteres después de normalizar.
- Debe comenzar por una letra de a a z.
- Después admite letras de a a z, números de 0 a 9, punto,
  guion y guion bajo. No se agregan restricciones sobre separadores
  consecutivos o finales que no hayan sido aprobadas.
- Sin espacios internos, tildes, ñ ni otros caracteres fuera de ese conjunto.
- Se recortan espacios externos y se convierte a minúsculas con Locale.ROOT.
  Las mayúsculas del dato de entrada se aceptan mediante normalización.
- Debe ser único sin distinguir mayúsculas de minúsculas. Su unicidad es
  independiente de la del email y se implementará en Usuario, repositorio
  y servicio; no la verifica una anotación de longitud del DTO.
- Son reglas del proyecto, no una longitud universal exigida por Java,
  Spring o una plataforma externa. Ejemplos admitidos: ana_23, juan.perez
  y maria-lopez. Juan.Perez se normaliza a juan.perez.

#### DTO de entrada incorporado

- Campos: nombre, apellido, fechaNacimiento, generoId, tipoDocumentoId,
  numeroDocumento, paisEmisorId, nickname, email y password.
- No es una entidad JPA ni una tabla. UsuarioDTO continúa siendo el DTO
  de salida existente y no se reemplaza mediante este paso.
- Los datos personales conservan las restricciones de Persona. Los tres
  identificadores de catálogo son Integer obligatorios y positivos;
  su existencia y admisión se comprobarán en el servicio de registro.
- NumeroDocumento conserva capacidad de 50 caracteres, trim y mayúsculas;
  el formato específico según tipo y país sigue pendiente.
- Email conserva límite de 254 caracteres, trim, minúsculas, formato actual
  y la regla existente que impide comenzar con un número.
- Password conserva la política actual: de 6 a 64 caracteres, al menos
  una minúscula, una mayúscula y un carácter de !@#$&*, sin espacios.
  No se añade una exigencia de dígitos mediante este DTO.
- Password no se recorta ni se normaliza y está declarado como
  WRITE_ONLY para excluirlo de la serialización JSON del DTO. Esto no
  lo hashea: BCrypt seguirá aplicándose en el servicio antes de persistir.
- El cliente no elige id de cuenta, rol, activo, versión, hash de contraseña
  ni datos de recuperación mediante este contrato. El registro público
  deberá asignar CLIENTE y activo desde el backend.
- En esta etapa del DTO no se modificaron el formulario, UsuarioController,
  Usuario, la autenticación por email ni la recuperación. El nickname todavía
  no está disponible para iniciar sesión.
- Las diez pruebas unitarias de esta etapa fueron superadas. Siguen pendientes
  controles de duplicados, creación
  transaccional de Persona y Usuario e integración HTTP y frontend.

### Etapa 3 Pruebas unitarias del DTO sin Spring

Estado: clase incorporada, revisada y ejecutada. El informe JUnit de la
ejecución corregida confirma 10 pruebas, 0 fallos, 0 errores y 0 omitidas.
La primera ejecución no seleccionó pruebas por un problema con el filtro;
el segundo intento se completó satisfactoriamente el 2 de noviembre de 2026.

- Clase incorporada: DSII.TurismoSustentable.dto.RegistroUsuarioDTOTest,
  dentro de src/test/java, no de src/main/java.
- Se utilizaron JUnit 5, Bean Validation y Jackson disponibles mediante
  las dependencias actuales. No se requiere iniciar Spring, MySQL o XAMPP.
- Casos ejecutados: datos válidos; normalización; nicknames permitidos,
  inválidos y límites de longitud; emails inválidos; contraseñas inválidas
  y conservación de su valor; identificadores obligatorios y positivos;
  fecha de nacimiento futura; campos obligatorios; recepción de password
  y exclusión de ese campo al serializar el DTO a JSON.
- Los identificadores y demás valores de las pruebas son ficticios.
  Su aceptación en el DTO no acredita existencia en los catálogos,
  unicidad en la base, autenticación ni funcionamiento del registro HTTP.
- Ejecución realizada, limitada exclusivamente a esta clase:

```powershell
.\gradlew.bat test --tests "RegistroUsuarioDTOTest"
```

- No se ejecutará test sin ese filtro por ahora: el test existente
  TurismoSustentableApplicationTests carga Spring y aún no dispone
  de una configuración de base de datos de pruebas aislada.

#### Corrección del filtro de ejecución

El primer intento con el nombre completamente calificado que comienza
por DSII terminó con No tests found for given includes. No se ejecutaron
las validaciones y no se registra como un fallo de sus reglas ni como
una prueba superada.

Gradle interpreta los filtros que comienzan con mayúscula como nombres
simples de clase o clase y método. El paquete DSII, escrito con mayúsculas,
hizo inadecuado el comando inicial. Se sustituye por el nombre simple
RegistroUsuarioDTOTest, sin cambiar paquetes, código o dependencias.

Referencia: [filtros de pruebas de Gradle](https://docs.gradle.org/current/userguide/java_testing.html#simple_name_pattern),
consultada el 2 de noviembre de 2026. El resultado corregido se verificó
en el XML de JUnit, no solamente a partir del mensaje BUILD SUCCESSFUL.

#### Resultado y evidencia del segundo intento

La terminal aportada por el desarrollador informó BUILD SUCCESSFUL en
8 segundos. El informe de la suite RegistroUsuarioDTOTest confirma:

| Prueba | Resultado |
| --- | --- |
| normalizaDatosSinPerderCeros | Superada |
| aceptaNicknamesPermitidosYLimites | Superada |
| rechazaNicknamesInvalidos | Superada |
| rechazaEmailsInvalidos | Superada |
| exigeTodosLosCamposObligatorios | Superada |
| rechazaIdsAusentesONoPositivos | Superada |
| rechazaPasswordsInvalidasSinModificarlas | Superada |
| aceptaDatosValidos | Superada |
| rechazaFechaDeNacimientoFutura | Superada |
| recibePasswordPeroNoLaSerializa | Superada |

Evidencia local consultada:

- XML: `build/test-results/test/TEST-DSII.TurismoSustentable.dto.RegistroUsuarioDTOTest.xml`.
- Informe HTML: `build/reports/tests/test/index.html`.
- Fecha del informe: 2 de noviembre de 2026. Solo se encontró el XML de
  esta suite en los resultados de la ejecución revisada.

Los informes de build son generados y no se versionan. Este resumen
conserva el resultado junto con el código de las pruebas; la ejecución
puede repetirse mediante el comando filtrado indicado anteriormente.

El resultado verifica los escenarios implementados del DTO, no todas
las entradas posibles. No acredita existencia de países, tipos o géneros
en MySQL, restricciones de unicidad en la base, formato documental por
país, creación transaccional de cuentas, autenticación ni registro HTTP.

### Etapa 4 Nickname en Usuario y consultas de repositorio

Estado: incorporado manualmente y revisado el 2 de noviembre de 2026.
La compilación y la repetición de las diez pruebas del DTO fueron
superadas. Las consultas JPA y la unicidad física siguen pendientes
de verificación con una base aislada. Punto de partida: commit 0a4463c.

- Se agregó Usuario.nickname, inmediatamente después de Persona, con las reglas ya aprobadas: obligatorio,
  de 3 a 30 caracteres, primera letra de a a z y el resto de a a z,
  números, punto, guion o guion bajo. No se introducen nuevas restricciones.
- Su columna está declarada como nickname, VARCHAR de 30 caracteres, NOT NULL y UNIQUE.
  La unicidad es independiente de la del email, no una clave compuesta
  entre ambos campos. La restricción física sigue pendiente de prueba en MySQL.
- Se agregaron getNickname y setNickname después de setPersona; el setter aplica trim y
  minúsculas con Locale.ROOT, igual que el DTO.
- El callback de Usuario que normalizaba email se sustituyó por uno
  que normalice email y nickname en PrePersist y PreUpdate.
  No se normaliza ni modifica la contraseña en ese callback.
- UsuarioRepository incorporó findByNicknameIgnoreCase,
  existsByNicknameIgnoreCase y existsByNicknameIgnoreCaseAndIdNot.
  Se conservaron todas las consultas actuales de email, estado y recuperación.
- Las consultas permiten buscar sin distinguir mayúsculas y excluir
  la propia cuenta al comprobar duplicados en una futura modificación.
  No sustituyen la restricción UNIQUE ni su manejo transaccional.
- El login todavía continúa configurado por email: no se modificó
  WebAuthentication, WebAuthorization, UsuarioDTO ni el frontend en este paso.
- El registro y los constructores anteriores todavía no asignan Persona
  ni nickname. No se considera listo el flujo HTTP ni la carga inicial.
- Los datos personales duplicados, roles, hash y demás ajustes de Usuario
  se continuarán adaptando en los siguientes pasos; esta no es su versión final.
- Se revisaron Usuario, UsuarioRepository, RegistroUsuarioDTO, Persona
  y la clase de pruebas. El diff de los dos primeros coincide con los
  agregados y el reemplazo indicados; no se eliminaron miembros anteriores
  ni se duplicó el callback. La normalización y las reglas del nickname
  coinciden entre la entidad y el DTO.
- La terminal aportada por el desarrollador informó BUILD SUCCESSFUL
  en 3 segundos al ejecutar únicamente RegistroUsuarioDTOTest.
  Se contrastó el XML de resultados: 10 pruebas, 0 fallos, 0 errores y
  0 omitidas; timestamp 2026-11-02T14:07:47.759Z
  (11:07:47 en Buenos Aires).
- Las diez pruebas verifican el DTO, no el callback ni las validaciones
  de Usuario, las consultas JPA, el índice UNIQUE, la autenticación
  o la creación de cuentas en MySQL. No se ejecutó la aplicación
  ni el test de contexto de Spring durante esta revisión.
- Detalle de comentario sin efecto funcional: el texto anterior al
  callback todavía dice normalizar mail, aunque ahora normaliza
  email y nickname. No requiere cambios para compilar o continuar.

### Etapa 5 Fecha y hora de registro y versión de Usuario

Estado: incorporado manualmente y revisado el 2 de noviembre de 2026.
La compilación y las diez pruebas del DTO fueron superadas. Siguen
pendientes las pruebas de persistencia de fecha y bloqueo optimista.

- Se implementaron dos campos ya previstos en el DER v5 y en la revisión
  de personas, usuarios y roles del documento 25, sin nuevas tablas
  ni cambios de reglas de negocio.
- Usuario.fechaRegistro pasó de LocalDate a LocalDateTime y su columna
  se declaró fecha_registro, obligatoria y no actualizable por JPA.
  Se conservó CreationTimestamp para asignarla al insertar la cuenta.
- Se retiró la asignación LocalDate.now del constructor antiguo.
  El valor de alta se generará al persistir, no lo decidirá el formulario.
  Se adaptaron el getter y el setter al nuevo tipo.
- UsuarioDTO conservó el atributo fechaRegistro, cambiando su tipo
  y el del getter a LocalDateTime. Se mantuvo LocalDate para
  fechaNacimiento. La búsqueda de referencias encontró esos consumidores
  Java y ningún uso de fechaRegistro en el frontend actual.
- Se agregó Usuario.version de tipo Integer con Version y columna
  version obligatoria. Persistencia administrará su valor; se agregó
  únicamente un getter y no se asignó manualmente en constructores.
- Fecha de registro y versión llevan JsonProperty de acceso READ_ONLY
  en Usuario. Jackson ignorará los valores enviados para esos campos;
  no se confiará en ellos al deserializar la entidad en el flujo anterior.
  Esto no sustituye la futura separación de todos los DTO de entrada.
- La versión sirve para detectar actualizaciones concurrentes sobre
  una misma versión persistida. No es un historial ni sustituye
  auditoría, controles de autorización, las restricciones de unicidad
  o el tratamiento de los conflictos en los servicios.
- LocalDateTime no contiene una zona horaria. Este paso no establece
  por sí solo una política horaria global ni migra fechas antiguas.
- Verificación realizada: revisión de Usuario y UsuarioDTO y sus diffs,
  sin detectar errores en los cambios de esta etapa. La terminal aportada
  por el desarrollador informó BUILD SUCCESSFUL en 3 segundos con
  el filtro RegistroUsuarioDTOTest. El XML confirma 10 pruebas,
  0 fallos, 0 errores y 0 omitidas; timestamp 2026-11-02T15:03:58.938Z
  (12:03:58 en Buenos Aires).
- Estas pruebas ejercitan el DTO de registro, no la generación de fecha,
  el bloqueo optimista, el acceso JSON de Usuario ni MySQL.
  Las verificaciones de persistencia siguen pendientes de una base aislada.
- No se arrancó la aplicación ni se ejecutó el test de contexto
  de Spring en este paso. El registro, los constructores antiguos y
  la autenticación por nickname todavía requieren las adaptaciones
  indicadas en las etapas anteriores.
- El desarrollador informó 19 avisos amarillos en IntelliJ, entre ellos
  Field may be final. No se inspeccionó el listado completo del IDE,
  por lo que no se clasifican los 19 como equivalentes ni se declaran
  todos inocuos. UsuarioDTO contiene campos asignados solo en su
  constructor para los que esa sugerencia puede ser válida y opcional.
  No se convierten automáticamente en final los atributos persistentes
  de las entidades JPA ni se desactivan globalmente las inspecciones.
  Los avisos de deprecación de Gradle se distinguen de estas inspecciones
  y no motivan una actualización de versiones durante esta etapa.

### Etapa 6 Repositorios de Persona y catálogos de registro

Estado: cuatro interfaces incorporadas manualmente y revisadas el
2 de noviembre de 2026. Compilación y pruebas del DTO superadas;
pendientes las pruebas de las consultas con persistencia aislada.

- Se crearon PersonaRepository, PaisRepository, TipoDocumentoRepository
  y GeneroRepository en el paquete repositories existente.
  Cada interfaz extiende JpaRepository de su entidad y de Integer,
  coincidiendo con el tipo de sus claves primarias.
- Los tres catálogos tienen findByCodigo con retorno Optional de la
  entidad correspondiente. La consulta recibirá el código normalizado
  del catálogo, no un texto libre suministrado como relación por el cliente.
  findById, heredado, permitirá posteriormente resolver los identificadores
  enviados por los selectores del formulario.
- PersonaRepository tiene existsByTipoDocumento_IdAndPaisEmisor_IdAndNumeroDocumento,
  con parámetros Integer tipoDocumentoId, Integer paisEmisorId y
  String numeroDocumento. Los guiones bajos expresan la navegación
  desde cada relación hasta su atributo id, no nombres de columnas SQL.
- La comprobación documental usa la combinación completa ya aprobada
  en D01: tipo de documento, país emisor y número normalizado.
  No se declara el número único por sí solo ni se verifica aquí la
  autenticidad o el formato documental específico de cada país.
- Esta consulta previa no sustituye uq_persona_documento ni el manejo
  de colisiones concurrentes en el futuro servicio transaccional.
- Estos repositorios preparan la consulta y persistencia del registro;
  no crean endpoints, datos iniciales ni tablas distintas de las entidades
  ya implementadas. No se habilitan operaciones HTTP de borrado físico
  ni se cambia la política de bajas lógicas.
- No se conectará todavía RegistroUsuarioDTO con UsuarioController
  ni se moverá el login a nickname. Continúan pendientes la separación
  definitiva de datos personales, el tratamiento del hash y los roles.
- El desarrollador informó compilación y pruebas superadas. Se revisaron
  las cuatro interfaces y el XML de RegistroUsuarioDTOTest: 10 pruebas,
  0 fallos, 0 errores y 0 omitidas, con timestamp
  2026-11-02T15:18:01.838Z (12:18:01 en Buenos Aires).
- El resultado no verifica la interpretación de estas consultas por
  Spring Data, el acceso real a MySQL, las FK o la unicidad documental.
  Las pruebas de repositorios se realizarán con una base aislada.

### Etapa 7 Datos personales de Persona en UsuarioDTO

Estado: incorporado manualmente, revisado y probado el 2 de noviembre
de 2026. Doce pruebas unitarias superadas; integración JPA y HTTP
todavía pendientes. La revisión no modificó archivos Java.

- Se adaptó UsuarioDTO, que representa la respuesta de la API,
  no RegistroUsuarioDTO, que representa la solicitud de registro.
- El constructor obtiene nombre, apellido, fechaNacimiento y el código
  de género desde usuario.persona y sus relaciones, conforme a la
  separación aprobada de PERSONA y USUARIO. No lee esos datos
  desde los atributos duplicados del modelo anterior.
- UsuarioDTO incorporó nickname y su getter, tomando el valor de Usuario.
  Conservó el tipo LocalDateTime de fechaRegistro y los otros campos
  y getters actuales. No expone contraseña, hash ni token de recuperación.
- Si falta Persona, el constructor lanza IllegalStateException con
  un mensaje explícito. No inventa datos ni usa una alternativa
  silenciosa tomada de los campos antiguos. Esto detecta una cuenta
  incompleta; no es una respuesta HTTP de dominio ya implementada.
- El tratamiento actual del único rol se conservó por compatibilidad
  hasta la adaptación coordinada de ROL y USUARIO_ROL. No se considera
  implementada la colección de roles ni el rol TECNICO.
- Se incorporaron dos pruebas unitarias en UsuarioDTOTest, sin Spring
  ni base de datos: comprobar el origen de datos personales y nickname,
  y rechazar una cuenta sin Persona. Sus objetos son ficticios y
  no prueban existencia de catálogos, unicidad ni carga LAZY desde JPA.
- Se revisaron el DTO y UsuarioDTOTest y se confirmó la ubicación
  de ambos archivos en main y test respectivamente, sin duplicar
  el constructor ni perder getters anteriores.
- El desarrollador aportó BUILD SUCCESSFUL en 3 segundos ejecutando:

```powershell
.\gradlew.bat test --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

- Los dos XML de build/test-results/test confirman 12 pruebas,
  0 fallos, 0 errores y 0 omitidas: RegistroUsuarioDTOTest contiene
  diez pruebas (timestamp 2026-11-02T15:27:06.606Z) y UsuarioDTOTest
  contiene dos (timestamp 2026-11-02T15:27:06.862Z).
- Las dos pruebas nuevas son tomaDatosPersonalesDesdePersona y
  rechazaUsuarioSinPersona. Los datos personales antiguos de Usuario
  quedan sin completar en la primera, por lo que el resultado acredita
  que los valores comprobados se obtienen de la nueva relación Persona.
- No se ejecutaron SpringBootTest, la aplicación ni consultas a MySQL
  durante la revisión. Los informes generados no se versionan; se
  conserva este resumen y el código de las pruebas para repetirlas.
- Los atributos personales y constructores anteriores de Usuario
  se mantienen temporalmente para no romper otros consumidores.
  La retirada de esos atributos, los demás DTO/servicios/controladores
  y la consulta estadística de género siguen pendientes.
- No se habilita el registro HTTP ni se cambia el login. El mapeo
  de las relaciones LAZY deberá realizarse dentro del futuro servicio
  transaccional; las pruebas unitarias propuestas no lo garantizan.

### Etapa 8 Lecturas personales en correos y estadísticas

Estado: cambios incorporados manualmente y revisados estáticamente
el 2 de noviembre de 2026; compilación y doce pruebas de DTO superadas.
Pendientes las pruebas funcionales de correos, recuperación y JPQL.
Punto de partida: commit 665b603, que coincide
con el push informado por el desarrollador y la referencia local de
su rama remota. El árbol de trabajo estaba limpio al iniciar la revisión.

- Se cambiaron cuatro expresiones de CorreoService desde
  alquiler.getUsuario().getNombre() a
  alquiler.getUsuario().getPersona().getNombre(): dos en el comprobante
  de inicio y dos en el de finalización, incluidos el HTML y el nombre
  del destinatario. Se conservaron el escape HTML, los importes y el envío.
- La solicitud de recuperación en RecuperacionPasswordService y
  la bienvenida en UsuarioController toman también el nombre desde
  Persona. Email, token, expiración y validación de contraseña no se
  cambiaron en esta etapa; no se ejecutaron envíos reales en la revisión.
- En AlquilerRepository, contarPreferenciasBicicletaPorGenero pasó
  de a.usuario.genero a a.usuario.persona.genero.codigo en SELECT,
  GROUP BY y ORDER BY. El segundo elemento de cada fila será el código
  String del catálogo, no la entidad Genero ni el enum anterior.
- Se conservaron la firma del método y el filtro de alquileres FINALIZADO,
  sin modificar conteos ni consultas restantes. El controlador estadístico
  actual devuelve las filas sin convertir ese valor a GeneroUsuario.
- No se cambiaron los nombres de estaciones, países o tipos documentales.
  La separación de Persona no afecta esos getNombre.
- Los setters personales del controlador de usuarios, sus DTO de entrada,
  constructores y carga inicial anteriores siguen pendientes. No se
  eliminarán todavía atributos ni métodos personales de Usuario.
- La lectura de relaciones LAZY en los servicios deberá quedar cubierta
  por la adaptación transaccional y de consultas de las próximas etapas.
  Los dos métodos de alquiler actuales ya son transaccionales; este paso
  no declara resuelta la recuperación fuera de un contexto de persistencia,
  la entrega de correos después del commit ni la nueva estadística por detalle.
- Se revisó el diff de los cuatro archivos Java y se confirmó que
  coincide con las seis sustituciones y la consulta propuestas. No se
  detectaron errores de copiado ni cambios adicionales en esos archivos.
  git diff --check no informó problemas de espacios.
- El desarrollador aportó BUILD SUCCESSFUL en 2 segundos ejecutando
  únicamente UsuarioDTOTest y RegistroUsuarioDTOTest. Se contrastaron
  ambos XML: 10 pruebas de registro y 2 de salida, sin fallos, errores
  ni omitidas. Timestamps: 2026-11-02T15:54:27.143Z y
  2026-11-02T15:54:27.385Z, respectivamente (12:54:27 en Buenos Aires).
- Esas doce pruebas no ejecutan CorreoService, recuperación ni JPQL.
  La consulta y las asociaciones requieren pruebas de integración aisladas.
  No se arrancó la aplicación ni se conectó a la base anterior durante
  esta revisión. No se modificó Java ni se realizaron operaciones Git
  de escritura; solo se actualizó este registro de implementación.

### Etapa 9 DTO de entrada para modificar datos de usuario

Estado: ActualizarUsuarioDTO incorporado manualmente y revisado el
2 de noviembre de 2026. Compila y las doce pruebas anteriores siguen
pasando; no se presentan como pruebas específicas de este DTO.

- El formulario administrativo actual envía nombre, apellido, email,
  fechaNacimiento y genero como código del enum anterior. No envía
  contraseña, roles, estado de cuenta, nickname ni documento.
- Se creó ActualizarUsuarioDTO en el paquete dto, separado tanto
  de UsuarioDTO de salida como de RegistroUsuarioDTO de registro.
  Tiene nombre, apellido, email, fechaNacimiento y generoId.
- La nueva operación PUT exigirá esos cinco campos completos. Es un
  cambio explícito frente al método anterior que admitía campos omitidos;
  no se presenta como un PATCH parcial. Todavía no se conecta el DTO al
  endpoint, por lo que no se declara aplicado ese contrato HTTP.
- Nombre y apellido son obligatorios, de hasta 50 caracteres y con
  las reglas existentes de letras Unicode, espacios, apóstrofes y guiones.
  Los setters recortan espacios externos, sin modificar espacios internos.
- Email es obligatorio, de hasta 254 caracteres, con el mismo formato
  y la misma regla de no comenzar con número ya implementados en registro.
  El setter aplica trim y minúsculas con Locale.ROOT. La comprobación
  de unicidad, incluyendo cuentas inactivas y exclusión del propio id,
  corresponde al futuro servicio y a la restricción física, no al DTO.
- Fecha de nacimiento es obligatoria y pasada. No se añade edad mínima.
  generoId es un Integer obligatorio y positivo; el futuro servicio
  comprobará que el catálogo contiene ese identificador.
- No se admitirán mediante este DTO cambios de id, Persona vinculada,
  documento, nickname, rol, activo, contraseña, fecha de registro,
  versión ni token de recuperación. No se eliminan otros circuitos de
  modificación o recuperación; se delimita esta edición de perfil.
- No se tocará todavía admin.js: el envío actual de genero no equivale
  a generoId. La adaptación del selector, el DTO de salida para disponer
  de su identificador y la petición HTTP sigue pendiente. Al conectar
  el nuevo PUT, un cuerpo sin generoId debe ser rechazado por validación,
  no aceptado silenciosamente ignorando el cambio de género.
- Se prepara primero el DTO para mantener el trabajo manual en pasos
  acotados. Después se implementarán el servicio transaccional, su
  tratamiento de duplicados y concurrencia y la delegación del controlador.
  Los campos personales duplicados de Usuario aún no se retirarán.
- Se revisó el archivo completo: paquete, cinco campos, anotaciones,
  constructor, getters y setters coinciden con las instrucciones.
  El desarrollador informó pruebas superadas y los XML confirman
  diez pruebas en RegistroUsuarioDTOTest y dos en UsuarioDTOTest,
  sin fallos, errores ni omitidas. Timestamps:
  2026-11-02T16:07:21.469Z y 2026-11-02T16:07:21.744Z
  (13:07:21 en Buenos Aires).
- Esas doce pruebas no ejercitan ActualizarUsuarioDTO, sus errores
  HTTP ni el futuro servicio; se requieren pruebas específicas antes
  de dar por terminado el flujo de modificación.

### Etapa 10 Servicio transaccional de modificación de usuario

Estado: UsuarioService incorporado manualmente y revisado estáticamente
el 2 de noviembre de 2026. Compilación y doce pruebas anteriores de DTO
superadas; pendientes las pruebas específicas del servicio y de persistencia.

- Se creó un servicio con inyección por constructor de
  UsuarioRepository, GeneroRepository y EntityManager.
- Su método actualizarUsuario recibe id y ActualizarUsuarioDTO,
  devuelve UsuarioDTO y tiene Transactional, validación de método
  mediante Validated/Valid y autorización PreAuthorize exclusiva ADMIN.
  La configuración actual ya habilita prePostEnabled.
- Se exige id positivo, DTO presente y sus validaciones. El usuario
  debe existir, estar activo y tener Persona; no se inventarán datos
  personales ni se reemplazará el vínculo para subsanar una cuenta incompleta.
- El género se resuelve por findById del catálogo, no creando una fila
  nueva con un id enviado por el cliente. Se comprueba el email con
  existsByEmailIgnoreCaseAndIdNot incluyendo cuentas inactivas y
  excluyendo la propia cuenta. La restricción UNIQUE sigue siendo
  imprescindible frente a solicitudes concurrentes.
- Solo se modifican email en Usuario y nombre, apellido,
  fechaNacimiento y genero en Persona. No se copian de nuevo esos
  datos a los atributos duplicados anteriores.
- Si cambia el email, se limpian conjuntamente tokenRecuperacion
  y vencimientoTokenRecuperacion. Es una medida de seguridad para
  invalidar enlaces anteriores tras cambiar la dirección de recuperación;
  el DTO no recibe ni permite elegir esos valores. No verifica la
  titularidad del nuevo correo ni cambia el plazo de los enlaces nuevos.
- Se solicita OPTIMISTIC_FORCE_INCREMENT sobre Usuario para que
  el control de versión cubra también transacciones participantes que
  solo modifican Persona. No se añadirá una columna version a Persona
  ni se asignará manualmente Usuario.version. Esto no detecta por sí
  solo formularios antiguos que no envían una versión esperada.
- La Persona cargada dentro de la transacción es una entidad administrada:
  sus cambios se persistirán por detección de cambios, sin cascade REMOVE,
  sin reemplazarla y sin necesitar otro save sobre PersonaRepository.
  Se hará flush antes de construir el DTO dentro de la misma transacción.
- No se capturan errores de persistencia dentro del servicio para
  simular éxito: se propagarán y se revertirá la unidad de trabajo.
  Los conflictos pueden aparecer al hacer flush o al confirmar;
  el futuro manejo HTTP deberá cubrir ambas situaciones.
- La conexión del controlador, respuestas 400/404/409, auditoría ADMIN,
  retirada de duplicados y coherencia de las sesiones ante cambio de
  email siguen pendientes. El principal actual todavía utiliza email;
  la futura autenticación por nickname debe adaptarse de forma coordinada.
- Se revisó el archivo completo: inyección, anotaciones, búsquedas,
  comprobaciones, control de versión, cambios en Persona, limpieza
  conjunta del token y devolución del DTO coinciden con lo indicado.
- El desarrollador informó pruebas superadas. Los dos XML confirman
  10 pruebas de RegistroUsuarioDTOTest y 2 de UsuarioDTOTest,
  sin fallos, errores ni omitidas. Timestamps:
  2026-11-02T16:20:54.072Z y 2026-11-02T16:20:54.327Z
  (13:20:54 en Buenos Aires).
- Estas pruebas no acreditan la validación o seguridad por proxies,
  el funcionamiento del servicio, su transacción o el bloqueo optimista.
  Las pruebas específicas y la integración aislada se realizarán antes
  de dar por terminado este flujo y de arrancar la aplicación.
- Referencias técnicas consultadas el 2 de noviembre de 2026:
  [Hibernate 5.6 LockMode](https://docs.hibernate.org/orm/5.6/javadocs/org/hibernate/LockMode.html)
  y [JPA LockModeType](https://docs.oracle.com/javaee/7/api/javax/persistence/LockModeType.html).

### Etapa 11 Conexión del PUT de usuarios con el servicio

Estado: UsuarioController modificado manualmente y revisado el
2 de noviembre de 2026. Compilación y doce pruebas anteriores de DTO
superadas. Pendientes las pruebas específicas del PUT y del servicio.

- Se inyectó UsuarioService por constructor, conservando las
  tres dependencias anteriores porque otros métodos todavía las usan.
- Se reemplazó únicamente actualizarUsuario: recibe
  Valid RequestBody ActualizarUsuarioDTO y delega en el servicio.
  Se retiraron de ese método las búsquedas, validaciones duplicadas,
  cambios sobre atributos personales antiguos y modificación de contraseña.
  GET, POST, DELETE y la recuperación de contraseña se conservaron.
- El PUT nuevo requiere los cinco campos del DTO, incluido generoId.
  La petición actual de admin.js todavía envía genero como texto:
  el frontend sigue pendiente y no se considera compatible por este paso.
- La validación del cuerpo previa a ejecutar el método sigue usando
  el GlobalExceptionHandler existente. El controlador maneja localmente
  ConstraintViolationException del servicio como 400, EntityNotFoundException
  como 404, IllegalArgumentException como 400 e IllegalStateException como 409.
- Los conflictos de versión, tanto JPA OptimisticLockException como
  Spring OptimisticLockingFailureException, tienen prevista respuesta 409 con un mensaje
  para recargar los datos. Los conflictos DataIntegrityViolationException
  tienen prevista respuesta 409 sin exponer detalles SQL ni atribuir todos los errores
  de integridad exclusivamente a un email duplicado.
- El try del controlador cubre toda la llamada al proxy transaccional,
  incluidos los errores propagados al confirmar antes de retornar al caller.
  No se agregaron catch genéricos que conviertan fallos inesperados en 400.
  La autorización ADMIN y las excepciones de acceso de Spring Security
  no están incluidas en esas capturas.
- Esta etapa no modifica GlobalExceptionHandler ni afirma unificar
  todos los formatos de error de la API. Persisten pruebas específicas
  de servicio, validación y HTTP, auditoría y retirada de duplicados.
- Se revisaron el archivo completo y el diff: los seis imports nuevos,
  el campo final, los cuatro parámetros del constructor y sus asignaciones,
  la firma con Valid y el DTO de entrada, la delegación y las seis ramas
  catch coinciden con las instrucciones. No quedaron un segundo método
  PUT ni escrituras sobre los campos personales antiguos en ese método.
  Se conservaron el cierre de la clase y los demás endpoints.
- git diff --check no informó problemas. La clase compilada tiene
  fecha posterior a la última modificación del archivo fuente.
- El desarrollador informó pruebas superadas. Los XML confirman
  10 pruebas en RegistroUsuarioDTOTest y 2 en UsuarioDTOTest,
  sin fallos, errores ni omitidas. Timestamps:
  2026-11-02T16:53:35.503Z y 2026-11-02T16:53:35.758Z
  (13:53:35 en Buenos Aires).
- Esas pruebas no prueban la ruta HTTP, seguridad por proxy,
  commit/rollback ni las respuestas de error nuevas. La revisión
  confirma el copiado y la compilación, no la integración completa.
  No se arrancó la aplicación ni se conectó a la base anterior.
  Solo se actualizó este MD; no se modificó Java ni se escribió en Git.

### Etapa 12 Pruebas unitarias de modificación de usuario

Estado: UsuarioServiceTest creado manualmente en src/test, revisado
y ejecutado el 2 de noviembre de 2026. Diez pruebas nuevas superadas;
22 en total junto a las dos suites anteriores, sin fallos ni errores.
No se modificaron dependencias ni código productivo en esta etapa.

- Se utilizaron JUnit 5 y MockitoExtension incluidos en el starter de
  pruebas existente. UsuarioRepository, GeneroRepository y EntityManager
  se simulan; UsuarioService se construye mediante InjectMocks.
- Los fixtures son cuentas, personas, catálogos y DTO ficticios en
  memoria. El valor de password será una cadena explícitamente simulada,
  no una contraseña utilizable ni un hash BCrypt probado. Los tokens
  son cadenas ficticias de 64 caracteres, no enlaces reales.
- Se implementaron y superaron diez pruebas:
  1. Actualiza Persona y devuelve sus datos conservando identidad del
     vínculo, documento, nickname, rol, password y token si el email es igual;
     solicita el control de versión y llama a flush.
  2. Al cambiar el email, limpia conjuntamente hash del token y vencimiento.
  3. Rechaza un usuario inexistente sin iniciar escrituras.
  4. Rechaza un usuario inactivo sin iniciar escrituras.
  5. Rechaza una cuenta sin Persona sin iniciar escrituras.
  6. Rechaza un género no encontrado sin iniciar escrituras.
  7. Rechaza un email ocupado sin cambiar datos ni iniciar escrituras.
  8. Propaga un error de versión simulado al solicitar el lock, antes
     de mutar datos; no es una carrera de base de datos ejecutada.
  9. Propaga un conflicto de integridad simulado en flush; no afirma
     revertir objetos Java ni probar un rollback real.
  10. Valida explícitamente ActualizarUsuarioDTO con Bean Validation:
      datos válidos, cinco campos obligatorios y datos inválidos para
      cada uno (nombre, apellido, email, nacimiento futuro y género no positivo).
- La primera prueba comprueba que la consulta de duplicados utiliza
  el email normalizado y excluye el propio id. El resultado simulado
  no prueba una restricción UNIQUE ni la existencia de filas reales.
- La prueba con Mockito solo confirma la solicitud de
  OPTIMISTIC_FORCE_INCREMENT, no que Hibernate incremente la versión.
  Al invocar directamente el servicio construido en el test no actúan
  los proxies de Transactional, PreAuthorize ni Validated. La décima
  prueba ejecuta el validador explícitamente sobre el DTO, no el proxy.
- Se revisó la clase completa, su ubicación y los tres XML de JUnit.
  Los métodos, fixtures, mocks y verificaciones coinciden con lo indicado.
  No se añadieron stubs globales que afecten a los casos de rechazo.
  La clase compilada tiene fecha posterior a su archivo fuente y
  git diff --check no informó problemas de espacios.
- El desarrollador aportó BUILD SUCCESSFUL en 3 segundos ejecutando:

```powershell
.\gradlew.bat test --tests "UsuarioServiceTest" --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

- Los XML confirman 22 pruebas, 0 fallos, 0 errores y 0 omitidas:
  RegistroUsuarioDTOTest, 10 pruebas (2026-11-02T17:05:02.340Z);
  UsuarioDTOTest, 2 (2026-11-02T17:05:02.606Z);
  UsuarioServiceTest, 10 (2026-11-02T17:05:02.624Z).
  Las tres corresponden a las 14:05:02 de Buenos Aires.
- No se ejecutó SpringBootTest ni test sin filtros. No se arrancó
  la aplicación, se conectó a MySQL ni se enviaron correos en esta revisión.
  Solo se actualizó este MD: no se modificó Java ni se escribió en Git.
- Continúan pendientes pruebas HTTP de estados 400/404/409, seguridad
  por proxy e integración con una base aislada, además de las adaptaciones
  de frontend, auditoría, registro y retirada de campos duplicados.

### Etapa 13 Retirada de datos personales duplicados de Usuario

Estado: implementado y revisado estáticamente el 2 de noviembre de 2026;
compilación y 22 pruebas filtradas confirmadas el 3 de noviembre de 2026.
El desarrollador autorizó excepcionalmente
al asistente a aplicar únicamente este bloque, excluyendo el paso 5
de ejecución de comprobaciones con Gradle. Se mantiene el trabajo manual
para los siguientes bloques salvo nueva autorización.

Punto de partida: commit e5916e9, publicado por el desarrollador en
codex/refactorizacion-practicas-iii. Se comprobaron HEAD y referencia
local de la rama remota. Antes de editar Java solo había cambios en
este MD, correspondientes a la preparación de esta etapa.

- La búsqueda de consumidores confirmó que los constructores antiguos
  de Usuario solo son utilizados por initData del main. Sus datos
  personales ya no se leen desde los consumidores adaptados. Persona,
  DTO de entrada y DTO de salida conservan sus propios campos y métodos.
- Se reemplazó TurismoSustentableApplication por el arranque mínimo
  de Spring Boot, conservando paquete, nombre de clase, SpringBootApplication
  y main. Se retiraron el CommandLineRunner antiguo y sus dependencias.
  Esa carga usa el modelo anterior, incluido usuario sin Persona/nickname,
  bicicletas, tarjetas y alquileres que requieren revisión para el DER v5.
- La carga inicial anterior queda conservada en el historial Git del
  commit e5916e9 y en main estable; no se crea una copia Java duplicada
  dentro de src. Se preparará después una configuración de carga inicial
  explícita de desarrollo compatible con el nuevo modelo. Esta etapa
  no la reemplaza todavía ni afirma que existan datos precargados.
- No se borró ni modificó una base de datos mediante esta edición de código.
  No se ejecutó la aplicación: el perfil refactor conserva create-drop
  sobre turismo_sustentable y requiere verificar su activación y destino
  antes del primer arranque. La separación de ramas no aísla una base.
- En Usuario se retiraron nombre, apellido, fechaNacimiento y genero
  del enum anterior, incluidas sus anotaciones y sus ocho getters/setters.
  No se trasladaron de nuevo a la cuenta: las validaciones y columnas
  correspondientes de Persona se conservaron sin cambios.
- Se retiraron los dos constructores con datos personales y se mantuvo
  el constructor vacío requerido por JPA. Se incorporó otro constructor
  con Persona, nickname, email y passwordHasheada, utilizando los setters
  de los cuatro atributos. No hashea en la entidad ni recibe roles del formulario.
- Se eliminó el import de LocalDate en Usuario; LocalDateTime sigue
  siendo necesario. El enum GeneroUsuario no se borró: queda
  pendiente revisar y retirar ese archivo sin consumidores.
- UsuarioDTOTest utiliza el constructor nuevo en su primer caso,
  conservando la prueba de normalización y lectura desde Persona.
  El caso de cuenta sin Persona mantuvo el constructor vacío.
- Rol único anterior, Tarjeta, los mappings de hash y recuperación,
  equals/hashCode, id y demás ajustes de Usuario no se cambian aquí.
  Esta retirada no se presenta como la versión final completa del DER v5.
- El POST de registro todavía recibe la entidad Usuario y requiere
  sustituirse por RegistroUsuarioDTO y creación transaccional de Persona
  y cuenta. No se considera seguro ni terminado el registro por esta
  limpieza. El nuevo constructor no sustituye esa capa de servicio.
- Se revisaron estáticamente los diffs de los tres archivos Java y
  las referencias restantes: GeneroUsuario solo aparece en la declaración
  de su propio enum; los tres usos restantes de new Usuario corresponden
  al nuevo constructor y a los dos constructores vacíos de las pruebas.
- Durante la implementación no se ejecutó Gradle, javac, pruebas, la aplicación, SQL ni operaciones
  Git de staging, commit o push. La compilación y las tres suites filtradas
  quedaron a cargo del desarrollador. En ese momento no se registraron
  como superadas: los informes de la etapa 12 correspondían al código anterior
  a esta retirada. La ejecución posterior se documenta a continuación.
- El alcance de escritura fue el main, Usuario, la primera preparación
  de datos de UsuarioDTOTest y este registro de implementación. No se
  modificaron Persona, los otros modelos, DTO, servicios, controladores,
  repositorios, frontend, dependencias ni configuración de la base.

#### Comprobación posterior de la etapa 13

- El desarrollador ejecutó el 3 de noviembre de 2026 las tres suites
  filtradas y aportó BUILD SUCCESSFUL en 28 segundos. Se revisaron los
  XML generados después de las modificaciones de los tres archivos Java:
  RegistroUsuarioDTOTest, 10 pruebas (2026-11-03T07:43:35.456Z);
  UsuarioDTOTest, 2 (2026-11-03T07:43:36.133Z);
  UsuarioServiceTest, 10 (2026-11-03T07:43:36.160Z).
  Total: 22 pruebas, 0 fallos, 0 errores y 0 omitidas.
- Los informes corresponden a las 04:43:35-36 de Buenos Aires.
  Confirman la compilación y las comprobaciones unitarias existentes
  después de retirar los campos duplicados, no el registro HTTP ni
  la creación del esquema. No se arrancó la aplicación ni se conectó
  a una base de datos durante esta revisión del asistente.

### Etapa 14 Separación entre contraseña de entrada y hash persistido

Estado: implementado manualmente y revisado el 3 de noviembre de 2026.
Compilación y 22 pruebas existentes confirmadas; no hay todavía pruebas
específicas de las nuevas anotaciones. El asistente solo actualiza este MD.

- Se mantiene la regla aprobada del documento 25: RegistroUsuarioDTO
  valida la contraseña en claro antes del hasheado. Usuario almacena
  el resultado del PasswordEncoder, no vuelve a aplicarle esa política.
- El cambio aplicado se limita al bloque password de Usuario:
  NotBlank y Size con máximo 255 para el dato persistido, sin la expresión
  de composición ni el límite 6-64 de la contraseña en claro.
  JsonProperty WRITE_ONLY evita incluir este campo al serializar la entidad.
- Se conservan nombre del atributo, getters/setters, nombre actual de la
  columna y su capacidad 255. No se cambia BCrypt ni se genera un hash
  mediante estas anotaciones; el servicio debe entregarlo ya hasheado.
  No se modifica RegistroUsuarioDTO ni su política de contraseña.
- El POST actual todavía recibe Usuario, no RegistroUsuarioDTO.
  Retirar las reglas del campo persistido no completa ni asegura el
  registro público: se debe conectar el DTO y la creación transaccional
  de Persona, Usuario y rol antes de considerar funcional ese circuito.
  No se debe desplegar esta etapa intermedia como un registro terminado.
- La recuperación existente valida la contraseña en claro mediante
  validarPassword antes de llamar a PasswordEncoder. Su adaptación
  transaccional y las pruebas de integración siguen pendientes.
- Se comprobó el bloque completo: comentario, JsonProperty WRITE_ONLY,
  NotBlank para el hash, Size máximo 255 y Column conservando nombre y
  longitud. Se retiraron el mínimo 6, el máximo 64 y el Pattern de
  contraseña en claro únicamente de este campo de la entidad.
- El POST sigue recibiendo Usuario: no se interpreta el mensaje del
  desarrollador como una adaptación ya realizada del controlador.
- El desarrollador aportó BUILD SUCCESSFUL en 10 segundos. Los XML
  posteriores al cambio confirman 22 pruebas, 0 fallos, 0 errores y 0 omitidas:
  RegistroUsuarioDTOTest, 10 (2026-11-03T07:50:38.254Z);
  UsuarioDTOTest, 2 (2026-11-03T07:50:38.947Z);
  UsuarioServiceTest, 10 (2026-11-03T07:50:39.019Z).
  La clase compilada Usuario tiene fecha posterior a su archivo fuente.
- Estas suites no validan específicamente las anotaciones del hash ni su
  exclusión JSON en Usuario. La exclusión que prueba RegistroUsuarioDTOTest
  corresponde al DTO, no a la entidad. Persistencia y HTTP siguen pendientes.
  No se ejecutaron pruebas ni la aplicación desde el asistente.

### Etapa 15 Preparación del registro transaccional mediante DTO

Estado: RegistroUsuarioService incorporado manualmente, revisado y compilado
el 3 de noviembre de 2026. Pruebas propias del registro pendientes.
No se modifica Java desde el asistente ni se da por conectado el POST.

- El servicio de registro se separa del servicio de edición existente para
  incorporar el alta sin reemplazar UsuarioService ni afectar sus pruebas.
  Recibe RegistroUsuarioDTO con Validated, Valid y NotNull mediante el
  proxy de Spring; no recibirá entidades o roles suministrados por el cliente.
- Dependencias incorporadas: UsuarioRepository, PersonaRepository,
  GeneroRepository, TipoDocumentoRepository, PaisRepository y PasswordEncoder.
  Se comprueban email y nickname ocupados, existencia de los tres catálogos
  y duplicidad del documento compuesto antes de iniciar las escrituras.
- Se aplica BCrypt mediante el PasswordEncoder ya configurado, se crea
  Persona, se vincula a Usuario y se guarda la cuenta en una transacción.
  Los errores se propagarán, sin capturas que conviertan un fallo en éxito
  parcial. La atomicidad real requiere una prueba de integración posterior.
- La asignación CLIENTE utiliza transitoriamente el enum Rol actual y
  activo se fija en el backend. No se afirma haber implementado ROL ni
  USUARIO_ROL del DER v5; ese cambio debe conservarse como tarea pendiente.
- Los precontroles no sustituyen las restricciones UNIQUE físicas de email,
  nickname, documento compuesto y persona_id. Las colisiones simultáneas
  deberán manejarse también en la respuesta HTTP del siguiente bloque.
- El servicio devuelve UsuarioDTO dentro de la transacción y no envía
  correo. Al adaptar el controlador, la bienvenida se conservará después
  de que el servicio haya confirmado el alta, no antes de un posible rollback.
- Continúan pendientes: conectar POST /api/usuarios al servicio y al DTO,
  respuestas HTTP, pruebas propias del registro, carga inicial de catálogos,
  frontend, reglas documentales específicas por tipo/país y los roles del DER.
  Las 22 pruebas anteriores no ejercitan este servicio nuevo.
- Se revisó el archivo completo: paquete services dentro de src/main/java,
  imports, seis campos final y sus asignaciones de constructor, anotaciones,
  precontroles antes de guardar, hasheado antes de crear Usuario, uso de la
  Persona devuelta por save, saveAndFlush de la cuenta y mapeo a UsuarioDTO.
  No se añadieron capturas que impidan propagar el fallo transaccional.
- El archivo fuente tiene fecha 2026-11-03T07:56:35Z y su clase compilada
  2026-11-03T07:58:24Z. Los XML disponibles confirman 22 pruebas sin fallos,
  errores ni omitidas: RegistroUsuarioDTOTest, 10 (07:58:28.211Z);
  UsuarioDTOTest, 2 (07:58:28.742Z); UsuarioServiceTest, 10 (07:58:28.779Z),
  todas del 3 de noviembre de 2026.
- El último comando informado terminó BUILD SUCCESSFUL en 2 segundos con
  las cuatro tareas UP-TO-DATE: reutiliza resultados, no representa otra
  ejecución de las pruebas ni añade casos del registro. Los artefactos
  revisados son posteriores al archivo fuente y respaldan su compilación.
  No prueban el proxy de validación, rollback real ni la persistencia.
- No se ejecutó Gradle desde el asistente ni se modificó staging. El archivo
  nuevo tiene cambios en índice y en directorio de trabajo del desarrollador;
  se conservaron ambos estados, sin descartar ni preparar modificaciones.

### Etapa 16 Conexión del controlador al registro mediante DTO

Estado: cambios incorporados manualmente, revisados y compilados el 3 de
noviembre de 2026. Las 22 pruebas existentes pasan; no son pruebas HTTP
ni casos propios del servicio de registro. El asistente solo edita este MD.

- Se añadieron RegistroUsuarioDTO y RegistroUsuarioService a los
  imports, retirar PasswordEncoder de este controlador e inyectar el nuevo
  servicio mediante el constructor existente. Rol se conserva para DELETE.
- Solo se reemplaza el POST /api/usuarios: recibe Valid RequestBody
  RegistroUsuarioDTO y delega el alta en registrarUsuario. Se retiran del
  controlador el hasheado, los precontroles y la persistencia directa del alta.
  Los demás endpoints y UsuarioService se conservan.
- Los errores de validación del cuerpo previos a entrar al método siguen
  atendidos por GlobalExceptionHandler. Se implementaron respuestas locales 400
  para ConstraintViolationException e IllegalArgumentException y 409 para
  IllegalStateException y DataIntegrityViolationException. En este último
  caso el mensaje no expondrá SQL ni atribuirá toda colisión solo al email.
- La llamada al servicio se mantiene dentro del try para abarcar errores
  propagados desde el proxy y al confirmar la transacción. No se incorpora
  Transactional al controlador ni una captura genérica de excepciones.
- La bienvenida se solicita fuera del try del alta, después del retorno
  del servicio, utilizando email y nombre del DTO de salida. Se conserva
  el comportamiento actual de CorreoService, que devuelve false cuando
  faltan credenciales o captura un RestClientException; no se agrega un
  mecanismo de entrega garantizada ni se prueba el envío en esta etapa.
- El resultado previsto del alta exitosa es 201 con UsuarioDTO, sin hash
  de contraseña ni datos del token. El formulario actual todavía requiere
  adaptación a los campos y catálogos nuevos; conectar el backend no se
  presenta como registro de punta a punta ya comprobado.
- Quedan pruebas propias de servicio y HTTP, integración con base aislada,
  carga inicial y adaptación de roles al DER v5, además del frontend.
- Se revisaron el archivo completo y su diff: se retiraron import, campo,
  parámetro y asignación de PasswordEncoder únicamente del controlador;
  los dos imports nuevos y la inyección de RegistroUsuarioService están
  correctos. El POST tiene las cuatro ramas catch previstas y el envío de
  bienvenida utiliza nombre y email del DTO después de la llamada al servicio.
- GET, DELETE y PUT no presentan cambios en el diff. Se conserva Rol para
  la protección de DELETE y no se agrega Transactional al controlador.
  No se encontraron métodos POST duplicados ni cambios fuera de ese bloque,
  los imports y la inyección del constructor.
- El desarrollador informó BUILD SUCCESSFUL en 8 segundos. La clase
  compilada UsuarioController (2026-11-03T08:15:02Z) es posterior al fuente
  (08:14:32Z). Los XML confirman 22 pruebas, 0 fallos, 0 errores y 0 omitidas:
  RegistroUsuarioDTOTest, 10 (08:15:05.730Z); UsuarioDTOTest, 2 (08:15:06.325Z);
  UsuarioServiceTest, 10 (08:15:06.422Z), del 3 de noviembre de 2026.
- Los resultados respaldan compilación y regresión unitaria existente,
  no demuestran respuestas HTTP, validación por proxy, commit/rollback ni
  envío de correo. No se ejecutó Gradle ni la aplicación desde el asistente.
  git diff --check no informó problemas de espacios; no se escribió en Git.

### Etapa 17 Pruebas unitarias del servicio de registro

Estado: RegistroUsuarioServiceTest creado manualmente, revisado y ejecutado
el 3 de noviembre de 2026. Ocho pruebas nuevas superadas; 30 junto a las
tres suites anteriores. El asistente solo actualiza este MD.

- Se utiliza JUnit 5 con MockitoExtension, seis dependencias simuladas
  e InjectMocks sobre RegistroUsuarioService. No se arranca Spring ni una
  base de datos y no se envían correos. Los fixtures se preparan sin stubs
  globales para no dejar configuraciones no utilizadas en los rechazos.
- Ocho casos implementados y superados: alta correcta, email ocupado, nickname ocupado,
  documento compuesto ocupado, género inexistente, tipo de documento
  inexistente, país emisor inexistente y propagación de un conflicto de
  integridad al guardar la cuenta.
- En el alta se comprueban los datos normalizados, los catálogos elegidos,
  el vínculo con Persona, la delegación a PasswordEncoder y la asignación
  CLIENTE/activo. La salida esperada es UsuarioDTO con id simulado.
- El valor retornado por el encoder es un hash explícitamente simulado;
  no se presenta como un hash BCrypt utilizable ni prueba del algoritmo.
  Los identificadores y catálogos son ficticios, no filas cargadas en la base.
- Los rechazos previos a las escrituras no deben guardar Persona o Usuario
  ni llamar al encoder. La excepción simulada al guardar Usuario debe
  propagarse, pero el test no afirma revertir la Persona en una base real.
- Por ser una instancia construida por Mockito, estas pruebas no ejecutan
  los proxies Validated/Transactional. Tampoco comprueban SQL UNIQUE,
  rollback real, autorización ni respuestas HTTP del controlador.
- Se revisó el archivo completo: paquete y ubicación de test, los seis
  mocks, InjectMocks, fixtures, capturas de Persona/Usuario, mensajes de
  rechazo, helpers y propagación de la excepción coinciden con lo indicado.
  No se agregaron stubs generales que rompan los casos de rechazo temprano.
- El archivo fuente tiene fecha 2026-11-03T08:25:12Z y su clase compilada
  08:25:40Z. El desarrollador aportó BUILD SUCCESSFUL en 8 segundos usando
  filtros explícitos para las cuatro suites, sin ejecutar contextLoads.
- Los XML confirman 30 pruebas, 0 fallos, 0 errores y 0 omitidas:
  RegistroUsuarioDTOTest, 10 (2026-11-03T08:25:42.960Z);
  UsuarioDTOTest, 2 (08:25:43.461Z);
  RegistroUsuarioServiceTest, 8 (08:25:43.491Z);
  UsuarioServiceTest, 10 (08:25:44.382Z), todas del 3 de noviembre de 2026.
- git diff --check no informó problemas de espacios. No se ejecutaron
  Gradle, la aplicación, SQL ni envíos de correo desde el asistente; tampoco
  se modificaron el índice de Git, commits o ramas del desarrollador.
- El bloque queda comprobado a nivel unitario. Continúan pendientes
  pruebas HTTP e integración real, frontend, carga inicial y roles múltiples
  del DER v5. Las 30 pruebas no equivalen a registro de punta a punta probado.

### Etapa 18 Preparación de códigos para el catálogo de roles

Estado: implementado por el asistente el 3 de noviembre de 2026 con
autorización puntual del desarrollador; 30 pruebas filtradas superadas.
El desarrollador aclaró después que no desea conservar un enum de roles
en el diseño final; véase la aclaración al cierre de esta etapa.

- Punto de control confirmado: commit 6924569, Adaptar registro a Persona
  y DTO con pruebas unitarias, en codex/refactorizacion-practicas-iii.
  El desarrollador aportó el push satisfactorio; HEAD local coincide y
  el directorio de trabajo estaba limpio antes de actualizar este MD.
- El Rol anterior era un enum, no la entidad ROL del DER. Se renombró
  ese tipo a CodigoRol, incluyendo las referencias Java de src/main y
  src/test. Se libera así el nombre Rol para
  la futura entidad con id y codigo, sin convertir de golpe el tipo existente.
- CodigoRol enumera únicamente CLIENTE, ADMIN y TECNICO, los códigos
  ya aprobados. No se agrega una cuarta clase de usuario ni una tabla por
  cada código. Esta preparación no implementa aún el catálogo persistido.
- El cambio afecta al nombre del tipo y a sus referencias Java,
  no a las propiedades rol, los métodos getRol/setRol, los códigos de
  autoridades, cadenas JSON ni documentación histórica. La columna
  transitoria rol de Usuario sigue usando Enumerated STRING en esta fase.
- Se revisaron los usos explícitos: enum, Usuario, UsuarioController,
  RegistroUsuarioService, UsuarioServiceTest y RegistroUsuarioServiceTest.
  Antes de editar no existía CodigoRol.java. UsuarioDTO y WebAuthentication usan
  getRol().name() y no requieren cambiar sus cadenas por este renombrado.
- El atajo de IntelliJ no respondió al desarrollador, quien autorizó
  excepcionalmente al asistente a aplicar solo este bloque. Se utilizó
  apply_patch para mover Rol.java a CodigoRol.java y actualizar las
  referencias exactas de los cinco archivos consumidores. No se ejecutó
  una sustitución global de textos ni se renombraron campos o métodos.
- La próxima entidad Rol y las asignaciones USUARIO_ROL siguen pendientes.
  La preparación no transforma una cuenta en multirrol, no incorpora aún
  permisos técnicos ni resuelve la autenticación por nickname.
- Se ejecutaron las cuatro suites filtradas con JDK 11 del equipo.
  La primera invocación sin JAVA_HOME se interrumpió sin resultado; el
  segundo intento encontró un bloqueo de permisos en la caché de Gradle.
  Se obtuvo autorización para repetir el mismo comando con acceso a esa
  caché. JAVA_HOME se fijó únicamente para el proceso de comprobación,
  sin modificar variables persistentes ni la configuración del proyecto.
- Resultado confirmado: BUILD SUCCESSFUL en 13 segundos, cuatro tareas
  (tres ejecutadas y una UP-TO-DATE). Los XML posteriores al cambio tienen
  30 pruebas, 0 fallos, 0 errores y 0 omitidas:
  RegistroUsuarioDTOTest, 10 (2026-11-03T08:57:00.469Z);
  UsuarioDTOTest, 2 (08:57:01.026Z);
  RegistroUsuarioServiceTest, 8 (08:57:01.055Z);
  UsuarioServiceTest, 10 (08:57:02.086Z), del 3 de noviembre de 2026.
- Se revisaron los diffs y se comprobó que no quedan referencias Java
  al tipo Rol antiguo. Los métodos esAdministrador y puedeAlquilar y
  la protección de DELETE conservan las mismas condiciones. El registro
  público sigue asignando CLIENTE; agregar el código TECNICO no implementa
  por sí solo permisos ni altas técnicas.
- Se compararon hashes SHA-256 de 130 archivos versionados ajenos al bloque
  y permanecieron iguales. Solo se modificaron el enum renombrado, los
  cinco consumidores previstos y este MD. No se tocaron frontend, seguridad,
  DTO, repositorios, dependencias ni propiedades de la base de datos.
- No se arrancó la aplicación ni se ejecutaron contextLoads, SQL o envíos
  de correo. No se realizaron staging, commit o push. Las pruebas son
  unitarias y no demuestran integración con una base ni compatibilidad
  de todos los datos existentes. Se mantiene el trabajo manual en los
  siguientes bloques salvo nueva autorización.

#### Aclaración del diseño final de roles

- El desarrollador recordó expresamente que ROL será una entidad de base
  de datos y que no desea trabajar con un enum de roles en la solución final.
  Conservar CodigoRol como lista de códigos era una opción de implementación,
  no una exigencia del DER. No se impone esa opción.
- Se mantiene el DER aprobado: entidad Rol con id y codigo de tipo String,
  catálogo con CLIENTE, ADMIN y TECNICO, y asignaciones USUARIO_ROL con
  clave compuesta usuario_id/rol_id. No se agrega otra tabla para el enum.
- CodigoRol queda únicamente como puente transitorio del código anterior
  para que esta etapa compile. Se deberá retirar al reemplazar sus usos
  por el catálogo y las asignaciones; no se dará el bloque por finalizado
  mientras Usuario siga dependiendo del atributo de rol único.
- Esta aclaración no autoriza convertir de golpe todos los consumidores:
  la edición excepcional realizada se limita al bloque pedido. La entidad,
  asociaciones, autenticación y pruebas se adaptarán en los siguientes
  pasos coordinados con el desarrollador.

### Etapa 19 Entidad del catálogo Rol

Estado: Rol.java creado manualmente, revisado y compilado el 3 de noviembre
de 2026. Las 30 pruebas existentes pasan; todavía no prueban directamente
la entidad nueva ni su persistencia. El asistente solo actualiza este MD.

- Se contrastó el bloque con ROL del DER v5 y el documento 25: solo id
  como PK y codigo obligatorio y único. No se agregan nombre, descripción,
  campos de auditoría ni una tabla adicional para cada clase de usuario.
- La clase incorporada es una entidad JPA con Table rol, id Integer generado
  mediante IDENTITY y codigo String de capacidad 20, no un campo enum ni
  una anotación Enumerated. Los códigos aprobados son CLIENTE, ADMIN y TECNICO.
- Codigo utiliza NotBlank, Size máximo 20 y Pattern para admitir únicamente
  esos tres códigos. El constructor y setter normalizan trim y mayúsculas
  con Locale.ROOT. Estas validaciones Java no se confunden con un CHECK SQL:
  la obligatoriedad y unicidad física se declaran mediante Column.
- Se incluyen constructor vacío JPA, constructor con código, getters y setter
  de código; no se agrega setter público de id ni borrado físico en cascada.
- Esta etapa no conecta Rol a Usuario ni incorpora USUARIO_ROL. CodigoRol
  permanece transitoriamente solo porque sus consumidores aún no se
  adaptaron. No se lo conserva como dependencia de la entidad nueva.
- No se ejecuta la aplicación ni se crean filas de roles mediante el código
  propuesto. La carga inicial de catálogo, su repositorio, las asignaciones,
  el registro, DTO y autenticación se adaptarán en pasos siguientes.
- Se revisó el archivo completo: paquete models, imports javax, Entity y
  Table, generación del id, cuatro anotaciones de codigo, constructor JPA,
  constructor con código, getters y normalización coinciden con lo indicado.
  Rol no depende de CodigoRol ni declara relaciones prematuras con Usuario.
- CodigoRol sigue presente y sus usos Java continúan exclusivamente en
  el modelo transitorio y consumidores anteriores. No se borró ese archivo
  antes de adaptar Usuario, registro, seguridad y pruebas al catálogo.
- El desarrollador informó BUILD SUCCESSFUL en 7 segundos. La clase Rol
  compilada (2026-11-03T09:08:34Z) es posterior al fuente (09:08:20Z).
  Los XML confirman 30 pruebas, 0 fallos, 0 errores y 0 omitidas:
  RegistroUsuarioDTOTest, 10 (09:08:36.637Z); UsuarioDTOTest, 2 (09:08:37.172Z);
  RegistroUsuarioServiceTest, 8 (09:08:37.296Z);
  UsuarioServiceTest, 10 (09:08:38.167Z), del 3 de noviembre de 2026.
- No se ejecutó Gradle, la aplicación, SQL ni envíos desde el asistente.
  git diff --check no informó problemas. No se hicieron escrituras Git
  ni ediciones Java en esta revisión. Crear la clase no demuestra todavía
  que exista la tabla o que sus filas hayan sido precargadas.

### Etapa 20 Repositorio del catálogo Rol

Estado: RolRepository creado manualmente, revisado y compilado el 3 de
noviembre de 2026. No se modifica Java desde el asistente.

- Se incorporó una interfaz JpaRepository<Rol, Integer>, coherente con el
  tipo de la PK de la entidad. El método Optional<Rol> findByCodigo(String
  codigo) permite buscar, por ejemplo, la fila CLIENTE del catálogo.
- Se mantiene el mismo patrón que GeneroRepository. No se incorporan
  consultas SQL manuales, métodos innecesarios ni un endpoint público
  que permita crear o asignar roles desde el registro.
- Los códigos de búsqueda deberán usar la representación canónica en
  mayúsculas del catálogo. Declarar la interfaz no precarga las tres filas
  ni conecta por sí solo Rol con Usuario o con la autenticación.
- No se modifica Usuario ni se retira CodigoRol en esta etapa. La compilación no valida
  la consulta derivada contra una base; esa integración sigue pendiente.
- Se revisó el archivo completo: paquete repositories, imports Rol,
  JpaRepository y Optional, declaración de interfaz y firma findByCodigo
  coinciden con lo indicado. No se agregaron métodos ni anotaciones extra.
- El desarrollador informó BUILD SUCCESSFUL en 1 segundo, una tarea
  compileJava ejecutada. La clase compilada RolRepository tiene fecha
  2026-11-03T09:15:10Z, posterior al fuente (09:14:59Z).
  No se considera que se hayan repetido las 30 pruebas en esta ejecución.
- git diff --check no informó problemas. El asistente no ejecutó Gradle,
  la aplicación ni SQL y no realizó staging, commit o push.

### Etapa 21 Clase de clave compuesta de UsuarioRol

Estado: UsuarioRolId creado manualmente, revisado y compilado el 3 de
noviembre de 2026. El asistente solo actualiza este MD.

- Se conserva la PK aprobada de USUARIO_ROL: el par usuario_id/rol_id,
  sin identificador numérico adicional y con FK futuras a USUARIO y ROL.
  La clase representa ese valor compuesto, no una entidad o tabla nueva.
- Se incorporaron Embeddable, Serializable y serialVersionUID estático; campos
  Integer usuarioId y rolId con Column de nombres usuario_id y rol_id,
  no nulos, y validaciones NotNull/Positive para ambos identificadores.
- Tiene constructor vacío JPA, constructor con los dos identificadores,
  getters y equals/hashCode basados en ambas partes. No lleva Entity,
  GeneratedValue ni setters que faciliten cambiar una clave ya asignada.
- La próxima entidad UsuarioRol incorporará esta clase mediante EmbeddedId
  y coordinará los vínculos con MapsId. Las asociaciones no se declaran
  dentro de la clase de clave. Referencia técnica: [API JPA EmbeddedId](https://docs.oracle.com/javaee/7/api/javax/persistence/EmbeddedId.html).
- Crear esta clase por sí solo no materializa la PK ni las FK en la base
  ni evita duplicados persistidos sin la entidad que la use. La integración,
  validación del valor embebido y pruebas de igualdad seguirán después.
- En este paso no se modifica Usuario, Rol, registro, seguridad ni CodigoRol.
  Se ejecutó compileJava desde la terminal del desarrollador, sin arrancar
  la aplicación desde el asistente.
- Se revisó el archivo completo: paquete, seis imports, anotación,
  dos campos Integer con sus nombres de columna, validaciones, constructores,
  getters, comparación de ambas partes y hashCode coinciden con lo indicado.
  No tiene setters ni se agregó una tabla para la clave.
- El desarrollador informó BUILD SUCCESSFUL en 10 segundos, una tarea
  compileJava ejecutada. La clase compilada UsuarioRolId tiene fecha
  2026-11-03T19:01:07Z, posterior al archivo fuente (19:00:45Z).
  Esto confirma compilación, no pruebas de igualdad ni de mapeo con la base.
- git diff --check no informó problemas. El asistente no editó Java,
  ejecutó Gradle, SQL o la aplicación ni realizó operaciones de escritura Git.

### Etapa 22 Entidad de asignación UsuarioRol

Estado: UsuarioRol creado manualmente, revisado y compilado el 3 de
noviembre de 2026. La última invocación de compileJava reutilizó el
resultado existente. No se modifica Java desde el asistente.

- La entidad incorporada representa usuario_rol del DER: EmbeddedId de tipo
  UsuarioRolId, sin GeneratedValue, tercera columna id ni columnas de versión
  o fechas añadidas. NotNull y Valid sobre el identificador incluyen las
  validaciones de sus dos partes.
- Se vincula a Usuario y Rol mediante ManyToOne LAZY obligatorios,
  JoinColumn usuario_id/rol_id y MapsId usuarioId/rolId, exactamente con
  los nombres de los atributos del valor embebido. Cada columna es parte
  de la PK compuesta y FK al padre correspondiente; no se declara UNIQUE
  individual en esas dos columnas.
- No se incorpora cascade REMOVE ni otras cascadas sobre los padres.
  La asignación no recibe directamente datos de un formulario ni concede
  permisos por construirse; la autorización sigue siendo del servicio.
- Tiene constructor JPA vacío y otro con Usuario/Rol que exige sus
  identificadores positivos. La existencia real de las filas se comprobará
  en el servicio y mediante FK; un número positivo no la demuestra.
  El constructor arma la clave y se incluyen únicamente los getters.
- Para el futuro registro transaccional, el usuario debe obtener su id
  antes de construir esta asignación. Guardar Persona, Usuario y su rol
  seguirá siendo parte de la misma transacción, no tres altas independientes.
  Esta secuencia deberá coordinarse al adaptar el servicio.
- Referencia técnica: [API JPA MapsId](https://docs.oracle.com/javaee/7/api/javax/persistence/MapsId.html).
  La relación comparte la columna con la parte indicada de EmbeddedId;
  no son cuatro columnas ni claves adicionales.
- No se modifica aún Usuario, sus DTO, registro, autenticación ni CodigoRol.
  El repositorio de asignaciones y la conexión del rol múltiple siguen
  pendientes. Se solicita compileJava después de la creación manual;
  la compilación no prueba PK/FK reales ni operaciones de asignación.
- Las propuestas del documento 38 permanecen pendientes de confirmación
  y no se incorporan automáticamente al bloque actual de roles.
- Se revisó el archivo completo: paquete, imports javax, Entity/Table,
  EmbeddedId/Valid/NotNull, ambas relaciones y MapsId, nombres de columnas,
  comprobaciones del constructor, armado de la clave y tres getters coinciden
  con lo indicado. UsuarioRolId conserva los dos atributos compatibles.
- El desarrollador informó BUILD SUCCESSFUL en 1 segundo y una tarea
  UP-TO-DATE. No hubo una compilación nueva en esa última invocación,
  pero la clase UsuarioRol.class (2026-11-03T19:10:54Z) es posterior al fuente
  (19:10:31Z), por lo que el artefacto disponible respalda la compilación
  del archivo guardado. No se afirma haber ejecutado pruebas de este modelo.
- git diff --check no informó problemas. El asistente no ejecutó Gradle,
  SQL o la aplicación ni realizó staging, commit o push.

### Etapa 23 Repositorio de asignaciones UsuarioRol

Estado: UsuarioRolRepository creado manualmente, revisado y compilado el
3 de noviembre de 2026. El asistente solo actualiza este MD.

- Se incorporó JpaRepository<UsuarioRol, UsuarioRolId>: el segundo tipo es
  la clase de clave compuesta, no Integer ni un nuevo id autonumérico.
  Se conservan los métodos heredados de búsqueda por clave y guardado.
- Se agrega List<UsuarioRol> findByUsuario_Id(Integer usuarioId), navegando
  la relación usuario hasta su id para obtener sus asignaciones. El método
  no usa SQL concatenado ni recibe un nombre libre de atributo.
- La autorización, validación del usuario y restricciones de cambios de rol
  corresponderán al servicio. Declarar el repositorio no concede acceso a
  asignaciones ajenas ni incorpora endpoints de gestión pública.
- La consulta no carga automáticamente todas las asociaciones LAZY. Al
  consumir los datos del rol habrá que hacerlo en una transacción de servicio
  o adaptar expresamente la consulta; no se confiará en open-in-view.
- En este paso no se modifica Usuario, registro, DTO, autenticación ni
  CodigoRol. La compilación no prueba el SQL
  derivado ni las PK/FK de la tabla contra una base de datos.
- Se revisaron el archivo completo, ubicación, imports de entidad y clave,
  genéricos y firma findByUsuario_Id: coinciden con lo indicado. No hay
  métodos o anotaciones adicionales. El desarrollador informó compilación
  satisfactoria; la clase tiene fecha 2026-11-03T19:17:28Z, posterior al
  fuente (19:17:06Z). No se registra una nueva ejecución de las 30 pruebas.
- El asistente no editó Java, ejecutó Gradle o la aplicación ni realizó
  operaciones SQL, staging, commit o push en esta revisión.

### Etapa 24 Relación de Usuario con sus asignaciones de rol

Estado: tres inserciones aplicadas en Usuario.java por el asistente el
3 de noviembre de 2026, con autorización puntual del desarrollador.
Revisión estática realizada y comprobación del punto 4 completada por el
desarrollador: compilación y cuatro suites unitarias satisfactorias.
El asistente no ejecutó Gradle ni la aplicación. El mapeo con la base
de datos todavía no se ha probado.

- Se agregó una colección privada Set<UsuarioRol> usuarioRoles,
  inicializada con HashSet, OneToMany mappedBy usuario y fetch LAZY.
  El atributo usuario de UsuarioRol es el propietario de la relación.
  No se crea una columna adicional de roles en usuario ni otra tabla.
- Se agregó JsonIgnore a la colección y su import para no serializar ni
  deserializar libremente el grafo de asignaciones desde la entidad.
  Los contratos públicos seguirán siendo DTO y el registro no podrá elegir
  roles por enviar esa propiedad en JSON.
- Se incorporó solo el getter getUsuarioRoles, sin setter de reemplazo ni
  cascadas de borrado/orphanRemoval. El guardado o retirada de asignaciones
  deberá coordinarse explícitamente en los servicios con su autorización.
- No se agrega NotEmpty a esta colección en esta fase: el usuario debe
  obtener id antes de construir UsuarioRol con el constructor actual.
  La creación atómica de cuenta y rol obligatorio se garantizará en el
  servicio transaccional al adaptar el registro, sin dar por resuelta esa
  condición por la colección vacía inicial o por el mapping.
- Se conservan temporalmente el atributo CodigoRol rol, getRol/setRol,
  esAdministrador y puedeAlquilar. La revisión de consumidores confirma
  que registro, DTO, autenticación, DELETE y dos pruebas todavía los usan.
  No se mezcla la colección nueva con el enum para sumar autoridades ni
  se lo presenta como dos fuentes definitivas de permisos.
- Los próximos bloques adaptarán esos consumidores al catálogo y retirarán
  el atributo antiguo y CodigoRol. Esta edición aislada no habilita aún
  permisos múltiples ni resuelve sesiones existentes o login por nickname.
- Se solicita ejecutar las cuatro suites filtradas para compilar y comprobar
  regresión del código actual. No se arranca la aplicación ni se declara
  verificado el mapeo JPA o su persistencia mediante esas pruebas unitarias.
- La comparación contra el contenido anterior a esta intervención confirma
  exactamente tres inserciones: import JsonIgnore, colección anotada y getter.
  mappedBy usuario coincide con el atributo UsuarioRol.usuario; Set, HashSet
  y las anotaciones JPA ya contaban con los imports necesarios.
- Se revisó el resto de Usuario.java sin modificarlo: relación obligatoria
  con Persona, normalización de email/nickname, almacenamiento del hash y
  atributo Version se conservan. Esto no equivale a probar el módulo completo.
  El rol único anterior y la relación con Tarjeta siguen siendo transitorios.
- Pendiente detectado en esa revisión: equals/hashCode usan el email mutable.
  Si este cambia cuando el usuario está en una colección hash, puede fallar
  su búsqueda o retirada; además, dos usuarios distintos sin email comparan
  iguales. Se deberá definir y probar una identidad estable en un bloque
  separado, sin cambiarla silenciosamente durante la conexión de roles.
- Comprobación reservada al desarrollador:

```powershell
.\gradlew.bat test --tests "RegistroUsuarioServiceTest" --tests "UsuarioServiceTest" --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

- El desarrollador informó BUILD SUCCESSFUL en 6 segundos, tres tareas
  ejecutadas y una UP-TO-DATE. Se corroboraron los cuatro XML locales de
  build/test-results/test, correspondientes al 3 de noviembre de 2026,
  entre 19:37:03Z y 19:37:04Z: RegistroUsuarioDTOTest 10, UsuarioDTOTest 2,
  RegistroUsuarioServiceTest 8 y UsuarioServiceTest 10. Total: 30 pruebas,
  sin fallos, errores ni omisiones. Estos resultados verifican la regresión
  cubierta por las suites existentes, no la persistencia real de UsuarioRol
  ni la futura autorización con varios roles.
- El aviso de funcionalidades obsoletas de Gradle no impidió esta ejecución.
  No se modifican versiones de herramientas o dependencias en este bloque.

### Etapa 25 Dependencias del registro para asignar roles de catálogo

Estado: imports, atributos y constructor de RegistroUsuarioService.java
modificados por el asistente el 3 de noviembre de 2026, con autorización
puntual del desarrollador. Compilación del punto 4 completada por el
desarrollador. El asistente no ejecutó Gradle, SQL ni la aplicación.

- Se incorporaron los imports de RolRepository y UsuarioRolRepository, dos
  atributos final y sus parámetros/asignaciones en el constructor existente.
  Se conservan las seis dependencias anteriores; el constructor pasa a ocho.
  No se agrega otro constructor ni Autowired: la inyección de Spring seguirá
  utilizando el único constructor disponible.
- Este paso prepara la búsqueda del rol CLIENTE y el guardado explícito
  de UsuarioRol. No modifica todavía registrarUsuario, sus validaciones,
  el hash, la transacción, el DTO de salida ni la asignación transitoria
  de CodigoRol.CLIENTE. Los atributos nuevos quedarán sin uso hasta el
  siguiente bloque; no se afirma que ya se guarden asignaciones reales.
- La comprobación solicitada es compileJava desde la terminal del
  desarrollador, sin aplicación, SQL ni pruebas con contexto Spring.
- En el bloque siguiente se adaptará el método y sus pruebas unitarias
  conjuntamente: RolRepository deberá devolver CLIENTE con identificador
  válido; el Usuario guardado deberá tener id antes de construir UsuarioRol.
  Ambos repositorios necesitarán sus mocks al probar la nueva lógica.
  La falta del rol de catálogo deberá impedir un alta incompleta, no crear
  privilegios a partir de datos enviados por el cliente.
- Se revisaron los dos imports, atributos final y los ocho parámetros y
  asignaciones del constructor. registrarUsuario conserva su lógica anterior.
  El desarrollador informó BUILD SUCCESSFUL en 2 segundos, con compileJava
  ejecutada. La clase compilada tiene fecha 2026-11-03T19:59:06Z, posterior
  al fuente (19:50:31Z). No se registra una ejecución nueva de las pruebas.

### Etapa 26 Registro con asignación CLIENTE en usuario_rol

Estado: cambios aplicados el 3 de noviembre de 2026 por el asistente en
RegistroUsuarioService.java y RegistroUsuarioServiceTest.java, conforme a
la autorización puntual del desarrollador. Revisión estática realizada;
compilación y resultados de las cuatro suites filtradas corroborados tras
la comprobación del desarrollador: 32 pruebas sin fallos. La última
invocación reutilizó las cuatro tareas UP-TO-DATE; no ejecutó las pruebas
de nuevo. El asistente no ejecutó Gradle, SQL ni la aplicación.

- Se añadieron imports de Rol y UsuarioRol al servicio. Después de las
  comprobaciones actuales y antes de hashear o guardar, se busca CLIENTE
  mediante RolRepository.findByCodigo. Si no existe, se lanza
  IllegalStateException con el mensaje "El rol CLIENTE no está configurado."
  No se toma el rol ni su identificador del formulario ni se crea el catálogo
  dinámicamente al registrar una cuenta.
- Se conservan Persona, normalizaciones, validaciones, hash, activo y el puente
  CodigoRol.CLIENTE mientras el DTO y la autenticación dependan de él.
  No se conceden privilegios mediante una combinación de ambas fuentes.
- Se mantiene saveAndFlush del Usuario para contar con su id y construir
  UsuarioRol con ese Usuario y el Rol encontrado. Se guarda la asignación
  mediante save y se utiliza la instancia devuelta (que puede ser la instancia
  administrada tras un merge). Se añade a usuarioRoles para sincronizar la
  colección inversa y se ejecuta flush antes de crear el DTO de respuesta.
  No se modifica el constructor de UsuarioRol ni se agregan setters de ids.
- Se mantiene el único límite transaccional del método de registro. flush
  sincroniza cambios y adelanta la detección de restricciones; no confirma
  la transacción. Las excepciones de persistencia deben propagarse hasta
  salir del servicio para que la transacción pueda revertirse completa.
- Se adaptó la prueba existente: dos mocks de repositorios, rol CLIENTE real
  con id simulado 2 mediante ReflectionTestUtils solo en test, devolución
  del argumento al simular save de UsuarioRol, y captura/comprobación de
  los dos componentes de la clave, referencias y colección del Usuario.
  El id 2 no debe aparecer como supuesto en el código de producción.
- Se añadieron pruebas para catálogo sin CLIENTE (sin guardar ni hashear) y
  error al hacer flush de la asignación (excepción propagada). Las pruebas
  actuales de duplicados y catálogos inválidos deben seguir sin guardar
  asignaciones. Los stubs nuevos se ubican solo en los caminos que los usan,
  evitando stubbing innecesario con Mockito estricto.
- Con las dos pruebas nuevas se definen 10 casos de RegistroUsuarioServiceTest
  y 32 en las cuatro suites filtradas, corroborados en los informes locales.
  Los mocks no verifican rollback real, SQL,
  FK ni la seguridad por proxy; esas comprobaciones de integración siguen
  pendientes. No se ejecutará el test SpringBootTest sin base aislada.
- La carga inicial de CLIENTE/ADMIN/TECNICO sigue pendiente de su bloque de
  catálogos. Este cambio no habilita aún login por nickname, DTO multirrol
  ni autenticación basada en usuario_rol; CodigoRol no se elimina todavía.
- La revisión estática confirma que el servicio coincide con las inserciones
  propuestas, busca CLIENTE antes de hashear o guardar y conserva el método
  transaccional y CodigoRol.CLIENTE. El archivo de prueba define 10 casos;
  no se eliminó ninguno de los ocho anteriores y los otros siete métodos
  de prueba existentes, aparte del caso exitoso ampliado, conservan su
  contenido. Se revisaron los stubs de los caminos de éxito y error.
- La comparación de hashes de 140 archivos ajenos a este bloque no detectó
  cambios durante la intervención. Solo se editaron el servicio, su prueba
  y este MD. No se realizó staging, commit ni push. En esa revisión estática
  todavía no se había afirmado que las 32 pruebas pasaran; la evidencia de
  ejecución se corroboró posteriormente, como se detalla a continuación.
- Comprobación reservada al desarrollador:

```powershell
.\gradlew.bat test --tests "RegistroUsuarioServiceTest" --tests "UsuarioServiceTest" --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

- El desarrollador informó BUILD SUCCESSFUL en 1 segundo, con las cuatro
  tareas UP-TO-DATE. Esta invocación no constituye una ejecución nueva.
  Se revisaron los XML locales del 3 de noviembre de 2026 entre 20:49:12Z
  y 20:49:13Z: RegistroUsuarioDTOTest 10, UsuarioDTOTest 2,
  RegistroUsuarioServiceTest 10 y UsuarioServiceTest 10. Total: 32 pruebas,
  cero fallos, errores u omisiones. El informe de registro incluye
  rechazaRegistroSiNoExisteRolCliente y propagaConflictoAlPersistirLaAsignacion.
- Los fuentes del servicio y su prueba datan de 20:12:34Z/20:12:35Z; las
  clases compiladas correspondientes, de 20:49:10Z/20:49:11Z, y los XML son
  posteriores. Los artefactos disponibles respaldan la compilación y las
  pruebas del bloque modificado. No se pidió repetirlas innecesariamente
  ni se presentan como pruebas de base de datos o autorización real.

### Etapa 27 Consultas explícitas para datos personales y roles

Estado: tres imports y tres métodos agregados a UsuarioRepository.java
por el asistente el 3 de noviembre de 2026, con autorización puntual del
desarrollador. Revisión estática realizada; compilación del punto 3
corroborada el 4 de noviembre de 2026 tras la ejecución del desarrollador.
El asistente no ejecutó Gradle, SQL ni la aplicación. También se actualiza
este MD; la validación JPA contra una base sigue pendiente.

- Se revisaron UsuarioRepository, UsuarioDTO, los tres GET de UsuarioController,
  UsuarioService, RegistroUsuarioService y sus pruebas, WebAuthentication y
  los consumidores JavaScript. El DTO actual devuelve un único rol desde
  CodigoRol; index.js y admin.js todavía consumen ese campo. No se elimina
  ese contrato ni se presenta la autenticación multirrol como implementada.
- Se agregaron tres métodos nuevos con Query JPQL y EntityGraph sobre
  persona.genero y usuarioRoles.rol. Ambas rutas coinciden con los atributos
  actuales del modelo y abarcan las relaciones que necesitará UsuarioDTO.
  No se modifica el fetch LAZY global de las entidades.
- buscarActivosConDatosYRoles: select distinct u from Usuario u where
  u.activo = true; devuelve List<Usuario>.
- buscarPorIdConDatosYRoles: select distinct u from Usuario u where
  u.id = :id; devuelve Optional<Usuario>, con Param id de tipo Integer.
- buscarPorEmailConDatosYRoles: select distinct u from Usuario u where
  u.email = :email; devuelve Optional<Usuario>, con Param email de tipo String.
  No se concatenan valores recibidos dentro del JPQL. La selección por email
  del usuario actual continuará tomando la identidad del principal, no un
  email libre del formulario. No se cambia todavía el login a nickname.
- Los dos métodos individuales conservan la selección de los findById y
  findByEmail actuales; el filtro de activo del GET por id y los controles
  de acceso no se eliminan. La autorización no la otorga el repositorio.
  No se cambian las consultas de recuperación, escritura o autenticación.
- Se incorporaron tres imports nuevos: EntityGraph, Query y Param. Se agregaron
  los métodos sin borrar ni modificar los existentes. Todavía no se conectan
  al controlador o servicio, ni se cambia UsuarioDTO o CodigoRol.
- EntityGraph configura la carga para esas consultas concretas. No garantiza
  una única consulta SQL ni resuelve por sí solo los costes de las relaciones
  EAGER heredadas. Se consultó la documentación oficial de Spring Data JPA
  2.7 sobre [grafos de carga](https://docs.spring.io/spring-data/jpa/docs/2.7.18/reference/html/#jpa.entity-graph)
  y la API de [EntityGraph](https://docs.spring.io/spring-data/jpa/docs/current/api/org/springframework/data/jpa/repository/EntityGraph.html)
  que documenta rutas anidadas y soporte desde 1.9. No se actualizan librerías.
- La comprobación solicitada es compileJava. Esta tarea no valida el JPQL,
  la carga del grafo ni la persistencia contra una base real; esas pruebas
  de integración siguen pendientes. No se arranca el contexto SpringBootTest
  ni la aplicación contra la base actual para comprobar estos métodos.
- El siguiente bloque adaptará la representación de roles y sus consultas
  consumidoras de manera compatible con el front, sin elegir arbitrariamente
  un primer rol como autoridad ni sumar permisos del enum y la tabla.
- La comparación con el archivo anterior a esta intervención confirma
  únicamente las inserciones propuestas: tres imports y los tres métodos
  completos. No se borró ni modificó ningún método existente. Se comprobaron
  estáticamente los parámetros id/email, los tipos de retorno y las rutas
  persona.genero y usuarioRoles.rol frente a las entidades guardadas.
- La comparación de hashes de 141 archivos ajenos al repositorio y este MD
  no detectó cambios durante la intervención. No se modificaron el DTO,
  controladores, servicios, entidades, seguridad ni pruebas. No se realizó
  staging, commit ni push. La revisión no verifica el JPQL contra una base.
- Comprobación reservada al desarrollador:

```powershell
.\gradlew.bat compileJava
```

- El desarrollador informó BUILD SUCCESSFUL en 9 segundos, con una tarea
  compileJava ejecutada. Se revisó nuevamente el archivo completo: los tres
  imports, tres métodos, EntityGraph, JPQL, parámetros y tipos de retorno
  coinciden con lo indicado y se conservan los métodos anteriores.
  UsuarioRepository.class tiene fecha 2026-11-04T06:29:03Z, posterior al
  fuente (2026-11-03T21:41:00Z). No se registra una ejecución nueva de las
  suites unitarias ni se presenta compileJava como prueba de las consultas.

### Etapa 28 Representación multirrol del DTO y lecturas HTTP

Estado: cambios aplicados por el asistente el 4 de noviembre de 2026 en
UsuarioController.java, UsuarioDTO.java y UsuarioDTOTest.java, con
autorización puntual del desarrollador. Revisión estática realizada;
compilación y resultados del bloque 4 corroborados el 4 de noviembre de
2026: 34 pruebas sin fallos. El asistente no ejecutó Gradle, SQL ni la
aplicación.

- Se sustituyeron exclusivamente las consultas de los tres GET de UsuarioController:
  getUsuarios usa buscarActivosConDatosYRoles; getUsuarioPorId usa
  buscarPorIdConDatosYRoles; getUsuarioActual usa
  buscarPorEmailConDatosYRoles con el email del Principal. Se mantienen los
  filtros, respuestas HTTP y reglas de autorización existentes. No se cambia
  findById en DELETE ni ninguna consulta de escritura o autenticación.
- Se incorporaron a UsuarioDTO un atributo List<String> roles, su getter y el
  import Collectors. Se construye la lista desde usuario.getUsuarioRoles(),
  tomando el codigo del Rol de cada asignación, sin duplicados y ordenada
  alfabéticamente para una salida determinista. Ese orden no es una jerarquía
  de permisos ni se debe elegir su primer elemento como autoridad.
- Se mantienen por ahora el atributo singular rol, su getter y su asignación
  antigua, porque el front todavía los consume. El nuevo campo roles no se
  obtiene del enum ni usa CLIENTE como fallback ante una colección vacía.
  Esta compatibilidad es transitoria: no se suman las dos fuentes para
  autorizar y la autenticación sigue pendiente de su adaptación.
- Se aplicaron los cambios del DTO y los tres GET en el mismo bloque. Sus nuevas
  consultas solicitan cargar persona.genero y usuarioRoles.rol antes de
  construir el DTO, sin confiar en open-in-view para esas relaciones.
  Se verificó que los otros dos constructores de UsuarioDTO en producción
  están dentro de RegistroUsuarioService y UsuarioService transaccionales;
  el registro ya sincroniza la asignación con la colección en memoria.
  No se cambian las entidades a EAGER globalmente.
- Se añadieron dos pruebas a UsuarioDTOTest: todos los roles asignados aparecen
  ordenados e independientes del enum transitorio, y sin asignaciones la
  lista queda vacía sin inventar CLIENTE. Este caso de representación no
  autoriza cuentas válidas sin roles: el registro sigue creando su CLIENTE
  obligatorio y la futura autenticación deberá rechazar una cuenta sin roles.
- Los fixtures de prueba usan Rol y UsuarioRol reales, Usuario con id 17
  y ReflectionTestUtils solo para simular el id generado de Rol. Se mantienen
  intactas las dos pruebas existentes; no se añaden setters de ids a Rol.
  Se corroboraron 4 casos de UsuarioDTOTest y 34 en las cuatro suites
  filtradas, sin fallos, errores ni omisiones.
- Esta etapa no modifica login, sesiones, permisos, front, CodigoRol, bajas
  ni esquema. Las pruebas unitarias no validan las consultas JPQL, la carga
  JPA real, el aislamiento de sesión ni autorizaciones HTTP; esas pruebas
  de integración siguen pendientes en una base aislada.
- Se comparó el contenido anterior a esta intervención: UsuarioController
  coincide exactamente con los tres reemplazos propuestos, conservando
  findById en DELETE y el resto de su código. UsuarioDTO coincide con las
  cuatro adiciones previstas: import, atributo, mapeo y getter. Las dos
  pruebas anteriores conservan íntegramente su contenido y ahora se definen
  cuatro casos en UsuarioDTOTest. Esa revisión inicial fue estática; los
  resultados de ejecución se corroboraron después, como se detalla abajo.
- La comparación de hashes de 139 archivos ajenos a estos tres archivos
  y este MD no detectó cambios durante la intervención. No se realizó
  staging, commit ni push. git diff --check no informó errores de espacios;
  la advertencia de conversión LF/CRLF corresponde al índice documental
  previamente modificado y no motivó cambios de configuración Git.
- Comprobación reservada al desarrollador:

```powershell
.\gradlew.bat test --tests "RegistroUsuarioServiceTest" --tests "UsuarioServiceTest" --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

- El desarrollador informó BUILD SUCCESSFUL en 10 segundos, con tres tareas
  ejecutadas y una UP-TO-DATE. Se revisaron los cuatro XML locales del
  4 de noviembre de 2026 entre 06:48:47Z y 06:48:49Z: RegistroUsuarioDTOTest
  10, UsuarioDTOTest 4, RegistroUsuarioServiceTest 10 y UsuarioServiceTest 10.
  Se incluyen los casos nuevos incluyeTodosLosRolesSinUsarElEnum y
  noInventaRolesCuandoNoHayAsignaciones. Total: 34, cero fallos, errores
  y omisiones. Las clases del controlador/DTO/prueba (06:48:44Z-06:48:45Z)
  son posteriores a sus fuentes (06:45:37Z-06:45:39Z).
- La evidencia respalda la compilación y las suites unitarias indicadas;
  no demuestra el funcionamiento HTTP, JPQL o JPA real. El asistente no
  repitió las pruebas ni ejecutó el test con contexto SpringBootTest.

### Etapa 29 Cargador de credenciales y roles para Spring Security

Estado: UsuarioDetailsService.java creado por el asistente el 4 de noviembre
de 2026, con autorización puntual del desarrollador y conforme al código
propuesto. Revisión estática realizada y compilación del bloque 3 corroborada
tras la ejecución del desarrollador del 4 de noviembre de 2026.
El asistente no ejecutó Gradle, SQL ni la aplicación.
La prueba del servicio todavía no se creó.

- Se creó un servicio que implementa UserDetailsService, con UsuarioRepository
  por constructor y loadUserByUsername transaccional de solo lectura.
  Normalizar el identificador actual como email con trim y Locale.ROOT,
  rechazar identificadores nulos/vacíos y buscar mediante
  buscarPorEmailConDatosYRoles. Conservar el email como username del principal
  para no romper los consumidores actuales. El cambio a nickname sigue
  pendiente de su bloque coordinado; el nombre del método lo define Spring.
- Obtener todas las autoridades de usuarioRoles.rol.codigo, sin duplicados,
  sin leer CodigoRol, sin elegir solo el primer rol y sin fallback CLIENTE.
  Una cuenta sin asignaciones se rechaza mediante UsernameNotFoundException.
  Mantener los códigos ADMIN/CLIENTE/TECNICO sin prefijo ROLE_, compatibles
  con las reglas actuales hasAuthority/hasAnyAuthority.
- Devolver un User nuevo de Spring por invocación, con el hash ya guardado
  (sin volver a hashear ni comparar aquí la contraseña), activo como enabled
  y los otros tres indicadores del constructor en true. No se implementan
  vencimiento de cuenta/credenciales ni bloqueo nuevos, ni se agregan campos
  o tablas por esos indicadores. El proveedor de autenticación comprueba
  credenciales y enabled cuando se conecte el servicio al flujo.
- La transacción y la consulta con grafo cubren el acceso a las relaciones
  durante el armado del UserDetails, no durante una serialización HTTP.
  Este objeto interno contiene el hash y nunca debe devolverse como DTO
  público ni imprimirse con credenciales en logs.
- Se revisó la API oficial de [User de Spring Security 5.7.10](https://docs.spring.io/spring-security/site/docs/5.7.10/api/org/springframework/security/core/userdetails/User.html),
  incluidos los indicadores y la creación de una instancia nueva. No se
  actualizan dependencias ni se cambia el PasswordEncoder BCrypt existente.
- En este paso se solicita solo compileJava tras crear la clase. No se
  modifica WebAuthentication: su lambda actual sigue presente. Antes del
  cableado explícito se añadirán pruebas del servicio, incluyendo múltiples
  autoridades, ausencia de roles, identificación inválida y estado enabled.
  La compilación no es una prueba de login ni de configuración del proveedor.
- No se modifican WebAuthorization, front, sesiones, DTO, bajas, catálogos
  ni CodigoRol. La revocación de roles/inactivación en sesiones existentes,
  permisos específicos de TECNICO y pruebas HTTP/JPA reales siguen pendientes;
  crear este cargador no los resuelve automáticamente.
- La lectura posterior confirma coincidencia exacta con el código propuesto:
  paquete, imports, constructor, normalización, consulta con grafo, autoridades
  de las asignaciones, rechazo de identificador vacío/cuenta sin roles y
  construcción de User con activo como enabled. No usa CodigoRol ni encode.
  Es una revisión estática, no una ejecución del método ni una prueba de login.
- La comparación de hashes de 142 archivos existentes, excluido este MD,
  no detectó cambios durante la intervención. Solo se creó el servicio y
  se actualizó este documento. WebAuthentication conserva su contenido y
  no se realizó staging, commit ni push.
- Comprobación reservada al desarrollador:

```powershell
.\gradlew.bat compileJava
```

- El desarrollador informó BUILD SUCCESSFUL en 3 segundos, con una tarea
  compileJava ejecutada. Se revisó nuevamente el servicio completo: coincide
  con el código propuesto. La clase compilada tiene fecha
  2026-11-04T07:17:02Z, posterior al fuente (07:04:19Z). Esta evidencia
  corrobora compilación, no una prueba de login o una ejecución de sus ramas.

### Etapa 30 Pruebas unitarias de UsuarioDetailsService

Estado: UsuarioDetailsServiceTest.java creado por el asistente el 4 de
noviembre de 2026 dentro de src/test/java, en el paquete
DSII.TurismoSustentable.services, con autorización puntual del desarrollador.
Revisión estática realizada; compilación y ejecución de pruebas del punto 3
corroboradas tras la ejecución del desarrollador: 40 pruebas sin fallos.
No se ejecutó Gradle, SQL ni la aplicación desde el asistente.

- Se usan JUnit Jupiter y MockitoExtension, con UsuarioRepository simulado y
  UsuarioDetailsService inyectado por Mockito. No usar SpringBootTest ni
  arrancar un contexto, acceder a una base o enviar correos. Los fixtures
  de Persona, Usuario y asignaciones son objetos en memoria.
- Se incorporaron seis métodos de prueba: (1) email normalizado, username, hash,
  indicadores y autoridades ADMIN/TECNICO exactas sin agregar el rol legado;
  (2) activo false se transmite como enabled false; (3) usuario inexistente
  provoca UsernameNotFoundException; (4) ausencia de asignaciones también
  provoca esa excepción sin inventar CLIENTE; (5) email nulo, vacío o solo
  espacios se rechaza sin invocar el repositorio; (6) cada carga devuelve
  una instancia nueva y borrar sus credenciales en memoria no modifica el
  hash del Usuario ni el de una carga posterior.
- El segundo caso no comprueba que un proveedor real rechace el login;
  verifica el indicador entregado a Spring. El sexto usa CredentialsContainer
  sobre el UserDetails retornado, no sobre la entidad. No se compara una
  contraseña real ni se prueba BCrypt aquí: el hash del fixture es simulado.
- Reutilizar la consulta buscarPorEmailConDatosYRoles en los stubs. Los
  datos de preparación no se declaran globalmente como stubs para evitar
  configuraciones innecesarias en casos que fallan antes de consultar.
  ReflectionTestUtils se usa solo en los fixtures para el id generado de
  Rol; no se agregan setters ni se cambian las entidades.
- La comprobación indicada incluye las cinco suites unitarias por nombre
  simple, agregando UsuarioDetailsServiceTest a las cuatro existentes.
  Se corroboraron seis pruebas nuevas y 40 en total en los informes locales.
  No se repite el test de contexto ni se presume validado el grafo JPA,
  la transacción por proxy, el proveedor de autenticación o las sesiones.
- No se modifica UsuarioDetailsService, WebAuthentication, WebAuthorization,
  PasswordEncoder, front, DTO ni CodigoRol. El cableado explícito del cargador
  al login corresponde al bloque posterior a estas pruebas.
- La comparación del archivo creado con el código propuesto confirma
  coincidencia exacta y seis métodos Test definidos. Se revisaron paquete,
  imports, fixtures, ids positivos de Usuario/Rol, stubs y los caminos de
  error, sin iniciar un contexto Spring ni ejecutar las aserciones.
- La comparación de hashes de 143 archivos existentes, excluido este MD,
  no detectó cambios durante la intervención. Solo se creó la prueba y
  se actualizó este documento. No se realizó staging, commit ni push.
  Esa revisión inicial fue estática; los resultados de ejecución se
  corroboraron posteriormente, como se detalla a continuación.
- Comprobación reservada al desarrollador:

```powershell
.\gradlew.bat test --tests "UsuarioDetailsServiceTest" --tests "RegistroUsuarioServiceTest" --tests "UsuarioServiceTest" --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

- El desarrollador informó BUILD SUCCESSFUL en 9 segundos, con dos tareas
  ejecutadas y dos UP-TO-DATE. Se revisaron los cinco XML del 4 de noviembre
  de 2026 entre 07:34:54Z y 07:34:57Z: RegistroUsuarioDTOTest 10,
  UsuarioDTOTest 4, RegistroUsuarioServiceTest 10, UsuarioDetailsServiceTest 6
  y UsuarioServiceTest 10. Total: 40, sin fallos, errores ni omisiones.
  Los seis nombres de caso nuevos coinciden con los definidos en el fuente.
- UsuarioDetailsServiceTest.class (07:34:52Z) es posterior al fuente
  (07:30:25Z). Los artefactos respaldan compilación y las suites indicadas;
  no se afirma probado un login real, el grafo JPA o el cableado de seguridad.

### Etapa 31 Cableado de autenticación y protección de administradores

Estado: cambios aplicados por el asistente el 4 de noviembre de 2026 en
WebAuthentication.java y el DELETE de UsuarioController.java, con
autorización puntual del desarrollador. Revisión estática realizada;
compilación y resultados de las cinco suites del punto 3 corroborados
tras la ejecución del desarrollador: 40 pruebas sin fallos.
El asistente no ejecutó Gradle, SQL ni la aplicación.

- Se sustituyó la lambda antigua de WebAuthentication por delegación explícita
  a UsuarioDetailsService, con ese servicio y PasswordEncoder inyectados
  por el único constructor. Mantener Configuration, visibilidad de la clase,
  GlobalAuthenticationConfigurerAdapter e init(AuthenticationManagerBuilder).
  Encadenar userDetailsService(usuarioDetailsService).passwordEncoder(passwordEncoder)
  usando el bean BCrypt existente. No crear otro encoder, modificar hashes,
  cambiar a nickname ni alterar URLs, formulario o reglas de WebAuthorization.
- La revisión de consumidores detectó que DELETE protegía ADMIN
  mediante usuario.getRol() == CodigoRol.ADMIN. Se adaptó esa comprobación
  en el mismo bloque: se busca mediante buscarPorIdConDatosYRoles y se comprueba
  ADMIN en todas las asignaciones de usuarioRoles. Conservar respuesta 403,
  mensajes, filtros de existencia/inactivo y la baja lógica activo=false.
  No borrar Persona, Usuario, roles ni asignaciones físicamente.
- Se retiró solo el import CodigoRol de UsuarioController, que deja de usarse
  tras ese cambio. El enum y sus puentes de entidad/registro/DTO se mantienen
  temporalmente; esAdministrador y puedeAlquilar no tienen consumidores
  externos actuales según la búsqueda de referencias y siguen pendientes
  de limpieza. No se añade la propuesta MF09 de último ADMIN ni nuevas
  reglas de baja o concurrencia por esta modificación.
- Las autoridades para nuevas autenticaciones pasarán a provenir de la
  tabla de asignaciones. Una cuenta sin usuario_rol no podrá autenticarse
  aunque el campo legado diga ADMIN o CLIENTE. Antes de probar el arranque
  y login reales se deberá verificar el perfil y la carga inicial/migración
  de catálogos y asignaciones, sin introducir un fallback al enum.
- Se verificaron las firmas userDetailsService y passwordEncoder en el JAR
  local spring-security-config 5.7.10 con javap del JDK 11, sin ejecutar
  compilación, aplicación ni pruebas. Las consultas de documentación web
  de esas firmas no estuvieron disponibles; no se actualizaron dependencias.
- La comprobación indicada sigue siendo las cinco suites unitarias filtradas
  (40 casos existentes). Compilar y pasarlas no validará por sí solo el
  cableado del proveedor, BCrypt real, el DELETE HTTP o el contexto Spring;
  se añadirán comprobaciones específicas antes del ensayo completo del login.
- No se modifican front, DTO, catálogos, sesiones existentes ni rutas de
  TECNICO. El campo singular rol sigue siendo solo compatibilidad temporal
  de presentación; la revocación de sesiones y la actualización del front
  para todos los roles continúan pendientes. No se declara finalizada toda
  la migración de seguridad por conectar el servicio.
- La comparación posterior confirma que WebAuthentication coincide
  exactamente con el archivo propuesto y UsuarioController contiene solo
  los tres cambios previstos: import eliminado, consulta del DELETE con
  grafo y comprobación de ADMIN en todas las asignaciones. El resto del
  controlador, incluidos mensajes, filtros, respuestas HTTP y activo=false
  con save, conserva su contenido previo a esta intervención.
- La comparación de hashes de 142 archivos ajenos a esos dos archivos y
  este MD no detectó cambios durante la intervención. SecurityConfig,
  WebAuthorization, UsuarioDetailsService y las pruebas no se modificaron.
  No se realizó staging, commit ni push. La revisión estática no sustituye
  la compilación ni prueba que el contexto o el login real funcionen.
- Comprobación reservada al desarrollador:

```powershell
.\gradlew.bat test --tests "UsuarioDetailsServiceTest" --tests "RegistroUsuarioServiceTest" --tests "UsuarioServiceTest" --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

- El desarrollador informó BUILD SUCCESSFUL en 8 segundos, con dos tareas
  ejecutadas y dos UP-TO-DATE. Se revisaron los cinco XML del 4 de noviembre
  de 2026 entre 07:52:18Z y 07:52:20Z: RegistroUsuarioDTOTest 10,
  UsuarioDTOTest 4, RegistroUsuarioServiceTest 10, UsuarioDetailsServiceTest 6
  y UsuarioServiceTest 10. Total: 40, sin fallos, errores ni omisiones.
- WebAuthentication.class y UsuarioController.class tienen fecha
  2026-11-04T07:52:15Z, posterior a sus fuentes (07:49:32Z). Los resultados
  respaldan compilación y regresión de las suites existentes después de
  este cambio. Todavía no ejercitan init del configurador, comparación real
  con BCrypt, la protección del DELETE ni filtros HTTP o persistencia JPA.
  Las comprobaciones específicas de esos componentes siguen pendientes;
  no se arranca la aplicación contra la base actual ni se repiten pruebas
  desde el asistente para obtener este registro.

### Etapa 32 Prueba aislada del configurador de autenticación y BCrypt

Estado: paquete configurations y WebAuthenticationTest.java creados por el
asistente el 4 de noviembre de 2026 bajo src/test/java/DSII/TurismoSustentable,
con autorización puntual del desarrollador. Revisión estática realizada;
compilación y resultados del punto 3 corroborados tras la ejecución del
desarrollador: 43 pruebas sin fallos. El asistente no ejecutó Gradle,
SQL ni la aplicación.

- La prueba está en el mismo paquete que WebAuthentication para acceder
  a su clase de visibilidad de paquete, sin hacerla pública en producción.
  Usa JUnit Jupiter/MockitoExtension y únicamente UsuarioRepository como
  mock. No usa SpringBootTest, contexto de aplicación, MySQL o correos.
- Preparar el encoder mediante new SecurityConfig().passwordEncoder(),
  el UsuarioDetailsService real y AuthenticationManagerBuilder con un
  ObjectPostProcessor pasante solo para la prueba. Llamar a
  WebAuthentication.init(builder) y builder.build(), en lugar de configurar
  un proveedor separado que no ejercite el cableado de producción.
- Se inspeccionaron firmas y bytecode del JAR local spring-security-config
  5.7.10 con javap del JDK 11 para verificar ese armado sin un contexto.
  Esta inspección fue de solo lectura; no ejecutó los métodos, compiló ni
  probó el código propuesto.
- Se añadieron tres pruebas: contraseña correcta con email normalizado autentica
  y conserva todas las autoridades ADMIN/TECNICO, sin sumar el CLIENTE
  legado; contraseña incorrecta arroja BadCredentialsException; cuenta
  inactiva con contraseña correcta arroja DisabledException.
- El fixture guardará un hash BCrypt real creado con el mismo encoder.
  La prueba exitosa verificará nombre autenticado, autoridades, eliminación
  de las credenciales del resultado y conservación del hash del Usuario.
  No se fijará un hash literal dependiente de la sal aleatoria de BCrypt.
- Usar Persona/Usuario/Rol/UsuarioRol en memoria, con ids simulados solo
  para construir las asignaciones. Preparar el stub de búsqueda únicamente
  en los caminos que lo usan. ObjectPostProcessor pasante no modifica el
  postprocesado de la aplicación: pertenece únicamente a este test sin IoC.
- La comprobación incluyó WebAuthenticationTest y las cinco suites
  existentes por nombre simple. Se corroboraron tres pruebas nuevas y
  43 en total. No se cambian dependencias, fuerza
  de BCrypt ni código de producción por esta etapa.
- Esta es una prueba aislada de componentes: no valida descubrimiento de
  beans, transacciones por proxy, filtros HTTP, autorización de endpoints,
  sesiones o la consulta JPA real. La prueba del DELETE protector de ADMIN
  y el ensayo del contexto/base local aislada continúan pendientes.
- Se corroboró coincidencia exacta del archivo creado con el código
  propuesto: paquete de test, imports, BeforeEach, construcción del gestor,
  tres métodos Test, fixtures y stubs. La comparación de hashes de 144
  archivos existentes, excluido este MD, no detectó cambios durante la
  intervención. El código de producción y las pruebas anteriores quedaron
  intactos. No se realizó staging, commit ni push.
- La revisión inicial anterior fue estática. La evidencia de ejecución
  se corroboró posteriormente y se detalla a continuación.
- Comprobación reservada al desarrollador:

```powershell
.\gradlew.bat test --tests "WebAuthenticationTest" --tests "UsuarioDetailsServiceTest" --tests "RegistroUsuarioServiceTest" --tests "UsuarioServiceTest" --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

- El desarrollador informó BUILD SUCCESSFUL en 8 segundos, con dos tareas
  ejecutadas y dos UP-TO-DATE. Se revisaron los seis XML locales del
  4 de noviembre de 2026 entre 08:12:37Z y 08:12:40Z: WebAuthenticationTest
  3, RegistroUsuarioDTOTest 10, UsuarioDTOTest 4, RegistroUsuarioServiceTest
  10, UsuarioDetailsServiceTest 6 y UsuarioServiceTest 10. Total: 43,
  cero fallos, errores u omisiones. Los tres nombres de caso del configurador
  coinciden con los propuestos y figuran como satisfactorios.
- WebAuthenticationTest.class tiene fecha 2026-11-04T08:12:36Z, posterior
  al fuente (08:10:00Z). La evidencia respalda compilación y ejecución del
  armado manual del gestor mediante WebAuthentication.init y BCrypt real,
  usando repositorio simulado. Se verificaron credenciales, autoridades y
  estado habilitado de los casos definidos, no una sesión HTTP ni carga JPA.
  El asistente solo leyó esos resultados y actualizó este MD.

### Etapa 33 Reglas aprobadas para técnicos inactivos con órdenes

Fecha: 4 de noviembre de 2026.

Estado: diseño aprobado, pendiente de implementación. El usuario aprobó
primero la pausa administrativa excepcional al inactivar al técnico asignado
y después confirmó expresamente que PAUSADA pueda quedar temporalmente sin
técnico hasta su reasignación. Con ambas confirmaciones se consolidan las
reglas y la aclaración del DER v5, sin modificar originales, Java ni bases
de datos. La revisión de pruebas del bloque de usuarios puede retomarse;
la coordinación efectiva con mantenimiento requiere implementar ese módulo.

- Se contrastaron en lectura el PDF original (objetivos y requerimientos),
  la sección de mantenimiento del Diccionario de Estados original, RN-MAN-02,
  la revisión 27, la trazabilidad aprobada 36 y el Mermaid v5. El diccionario
  conserva cinco estados de orden; CREADA, EN_PROCESO y PAUSADA son abiertas.
  FINALIZADA/CANCELADA no se reabren por una baja de cuenta.
- El controlador actual permite inactivar cuentas TECNICO sin ADMIN, pero
  todavía no implementa la coordinación con el futuro módulo de órdenes.
  ADMIN + TECNICO sigue protegido por su rol ADMIN. No se cambió ese permiso.
- Recorrido aprobado: conservar la misma orden y su identificador. En CREADA,
  desasignar y conservar CREADA; en EN_PROCESO, pasar a PAUSADA con motivo
  administrativo y desasignar; en PAUSADA, conservar ese estado y desasignar.
  Las órdenes cerradas conservan estado, técnico histórico y demás datos.
  Pendiente de reasignación se representa por orden abierta sin tecnico_id,
  no mediante otra tabla, otro estado o una orden duplicada.
- Aprobado: ADMIN puede provocar excepcionalmente EN_PROCESO a PAUSADA
  solo como parte de inactivar al técnico asignado. La pausa exige motivo,
  fecha y registro del ADMIN real en el historial de la orden y en su
  auditoría administrativa. Conserva el trabajo, diagnóstico, costos y
  fecha de primer inicio; no cierra la orden ni habilita la bicicleta.
  Esta excepción acotada a RN-MAN-02 no concede a ADMIN un permiso general
  para pausar, ejecutar trabajo técnico, inventar diagnósticos o finalizar
  una orden. Su implementación queda pendiente.
- Segunda condición aprobada: PAUSADA puede quedar sin técnico hasta su
  reasignación. Se autoriza así la desasignación del recorrido descrito,
  sin permitir que una orden sin técnico inicie, reanude o registre trabajo.
- Conservar fecha_inicio, diagnóstico, trabajo, costos y todos los eventos
  anteriores. No fijar fecha_fin en una orden que sigue abierta ni habilitar
  la bicicleta: continúa su restricción de mantenimiento. En una orden ya
  PAUSADA, conservar el motivo previo y registrar la nueva desasignación
  con su contexto en un evento, sin inventar PAUSADA a PAUSADA como transición.
- Registrar técnico anterior, asignación nueva nula, estado anterior/nuevo
  cuando cambie, motivo, fecha y ADMIN real en HISTORIAL_ORDEN_MANTENIMIENTO
  y la auditoría ADMIN que corresponda. La pausa y la desasignación de una
  misma operación pueden conservarse en un evento completo según RN-TRZ-01.
  No atribuir la actuación a SISTEMA solo porque sus efectos sean automáticos.
- La baja, los cambios de las órdenes y sus evidencias deberán confirmarse
  juntos o revertirse, protegiendo la concurrencia. Reasignar seguirá siendo
  de ADMIN a un técnico activo con rol TECNICO, y reanudar seguirá requiriendo
  al técnico asignado; la asignación sola no reinicia automáticamente el trabajo.
- Se consolidaron RN-MAN-02 (sección 3.1), el diccionario de trabajo 08,
  la referencia de RN-MAN-03, las revisiones 22/27 y el índice 00. Ambos
  archivos Mermaid v5 aclaran tecnico_id opcional en CREADA o PAUSADA por
  baja del técnico; no se agregan campos, tablas, PK, FK o relaciones.
  El modelo conserva 29 entidades y 55 FK/relaciones. Los criterios de
  aceptación añadidos son especificaciones, no pruebas ejecutadas.
- Verificación documental: se comparó el Mermaid con su contenido anterior;
  solo cambió el comentario de tecnico_id. El TXT coincide íntegramente con
  el Mermaid y se contaron 29 entidades y 55 relaciones. Las huellas SHA-256
  de todos los archivos de src permanecieron iguales antes/después. Git
  diff --check no detectó errores de espacios; solo avisos de LF/CRLF.
- No se considera aprobado un tratamiento equivalente por quitar el rol
  TECNICO, ni se declara implementada la revocación de sesiones: esos
  alcances se revisarán aparte.
- No se ejecutaron pruebas, aplicación, SQL ni operaciones de escritura Git.
  Los originales consultados permanecen intactos; no se regeneró el Word
  exportado del diccionario. Se actualizan solo documentación de trabajo
  y el TXT/Mermaid del DER. La última evidencia de ejecución sigue siendo
  las 43 pruebas de la etapa 32, no una prueba del módulo de mantenimiento.

### Etapa 34 Pruebas de baja lógica de usuarios verificadas en aislamiento

Fecha: 4 de noviembre de 2026.

Estado: clase de prueba creada por el asistente con autorización puntual
del usuario, siguiendo el código propuesto. El usuario ejecutó el comando
filtrado y obtuvo BUILD SUCCESSFUL en 8 segundos; el asistente verificó
los siete XML locales. Resultado: 49 pruebas, sin fallos, errores ni
omisiones. Verificada la lógica aislada de los casos definidos, no HTTP
ni persistencia real o coordinación con mantenimiento.

- Se releyeron eliminarUsuarioLogico, su constructor, Usuario, Rol,
  UsuarioRol y los patrones de las pruebas existentes. El controlador ya
  consulta buscarPorIdConDatosYRoles, protege cualquier asignación ADMIN
  y realiza la baja mediante activo = false y save, sin borrado físico.
- Se creó en src/test/java/DSII/TurismoSustentable/controllers
  la clase UsuarioControllerTest, con JUnit Jupiter y Mockito. Sus seis
  casos cubren ADMIN protegido, ADMIN + TECNICO protegido, baja de CLIENTE,
  baja de TECNICO, usuario inexistente e intento sobre cuenta ya inactiva.
  Los casos ADMIN dejan el enum legado en su valor inicial: la decisión
  debe depender de las asignaciones UsuarioRol, no de ese enum temporal.
- Las bajas permitidas tienen aserciones para activo falso, respuesta 204, datos
  personales/de cuenta de la muestra y asignaciones conservados, y save
  de la misma instancia. Los rechazos tienen aserciones para estado/mensaje y que
  solo se haya consultado el repositorio, sin escrituras adicionales.
- Alcance: invocación directa del método del controlador y repositorio
  simulado; no prueba filtros HTTP, autorización de rutas, SQL/cascadas,
  sesiones ni efectos sobre órdenes. La regla aprobada de mantenimiento
  de la etapa 33 sigue pendiente de implementar en su módulo.
- La autorización puntual cubrió crear la clase propuesta y su paquete
  de pruebas y actualizar este diario, no modificar código de aplicación,
  pruebas anteriores, configuración, DER ni bases de datos. El usuario
  conserva el paso 3 de guardar/comprobar mediante el comando filtrado.
  No se requiere XAMPP ni se debe ejecutar la suite completa con el
  contexto no aislado.
- Revisión estática: la clase contiene los seis métodos @Test propuestos
  y no carga un contexto SpringBootTest. La comparación SHA-256 de src
  confirmó intactos todos sus archivos existentes; el único archivo nuevo
  es UsuarioControllerTest.java. No se detectaron espacios finales en la
  clase ni errores en git diff --check del diario. Esto no reemplaza la
  compilación ni la ejecución pendientes a cargo del usuario.

Comando propuesto para que ejecute el usuario:

```powershell
.\gradlew.bat test --tests "UsuarioControllerTest" --tests "WebAuthenticationTest" --tests "UsuarioDetailsServiceTest" --tests "RegistroUsuarioServiceTest" --tests "UsuarioServiceTest" --tests "UsuarioDTOTest" --tests "RegistroUsuarioDTOTest"
```

Comprobación informada por el usuario y contrastada en lectura:

- BUILD SUCCESSFUL en 8 segundos: dos tareas ejecutadas y dos UP-TO-DATE.
- Los siete XML del 4 de noviembre de 2026 tienen marcas entre
  09:10:51Z y 09:10:53Z: UsuarioControllerTest 6, WebAuthenticationTest 3,
  UsuarioDetailsServiceTest 6, RegistroUsuarioServiceTest 10,
  UsuarioServiceTest 10, UsuarioDTOTest 4 y RegistroUsuarioDTOTest 10.
  Total 49; en todos, cero fallos, errores u omisiones.
- Los seis nombres de caso de UsuarioControllerTest coinciden con los
  propuestos y figuran satisfactorios. Su .class tiene fecha UTC
  09:10:49Z, posterior al fuente de 09:07:34Z; la evidencia incluye la
  compilación de la clase nueva y su ejecución, no solo resultados viejos.
- La advertencia de funciones obsoletas de Gradle no impidió esta ejecución;
  no se actualizó Gradle ni se cambió configuración para ocultarla.
- El asistente solo leyó resultados/código y actualizó este diario. No
  volvió a ejecutar Gradle, aplicaciones o SQL ni realizó escrituras Git.
  La baja con efectos sobre órdenes sigue pendiente de implementación
  conforme a la etapa 33, no queda validada por estos seis casos.

### Etapa 35 UsuarioDTO sin dependencia del enum, verificado en aislamiento

Fecha: 4 de noviembre de 2026.

Estado: cambios aplicados por el asistente con autorización puntual del
usuario, siguiendo exactamente el bloque propuesto para UsuarioDTO y
UsuarioDTOTest. El usuario completó el paso 3 y obtuvo BUILD SUCCESSFUL
en 5 segundos. El asistente verificó los siete XML locales: 52 pruebas,
sin fallos, errores ni omisiones. El asistente no ejecutó Gradle, la
aplicación o SQL; no se verifica persistencia real ni navegación HTTP.

- Antes de editar se revisaron UsuarioDTO y sus pruebas, el registro y sus pruebas,
  index.js, admin.js y las rutas de WebAuthorization. La lista roles ya
  provenía de UsuarioRol; el singular rol todavía dependía del enum legado.
  index.js usa ese singular para navegación y guarda usuarioRol; admin.js
  lo usa para su control visual y la columna de usuarios.
- Cambio puntual aplicado: conservar el atributo/getter rol para esos
  consumidores, pero derivarlo exclusivamente de la lista roles. Para la
  presentación transitoria se elige ADMIN si está asignado; en su ausencia,
  CLIENTE si está asignado; en su ausencia, TECNICO si está asignado; sin
  asignaciones, null. No se toma el primer elemento alfabético ni se
  inventa CLIENTE ante una colección vacía.
- La prioridad es solo un puente de representación del frontend existente,
  no una jerarquía ni herencia de permisos. roles sigue incluyendo todos
  los códigos asignados y la seguridad sigue usando todas las autoridades
  del cargador de usuarios. No se suman permisos del enum y de la tabla.
- Se ampliaron las aserciones de los casos ADMIN + TECNICO y sin
  asignaciones, y se agregaron tres casos de DTO: CLIENTE, TECNICO y CLIENTE +
  TECNICO. Los roles se crean desde el catálogo en memoria; no se necesita
  cambiar RegistroUsuarioServiceTest para este paso porque el registro ya
  sincroniza la asignación CLIENTE antes de construir su respuesta.
- No se elimina todavía CodigoRol ni el campo legado de Usuario: el
  registro y dos pruebas de servicios aún los usan. Tampoco se modifica
  el frontend a multirrol, se cambia el login a nickname o se habilita
  la ruta de usuario actual para una cuenta exclusivamente TECNICO; ese
  recorrido y sus pantallas continúan pendientes de adaptación.
- Al aplicar este cambio, la evidencia previa eran las 49 pruebas de la
  etapa 34; la comprobación posterior del usuario se registra debajo.
  No se modificó el frontend, servicios,
  entidades, seguridad, configuración, DER ni otras pruebas. Además de
  los dos archivos Java indicados, solo se actualizó este diario.
- Revisión estática posterior: la comparación con los contenidos previos
  confirma exactamente el reemplazo del bloque singular y las inserciones
  de prueba propuestas, sin otros cambios de contenido en esos archivos.
  Antes de ejecutar se contaron siete métodos @Test en UsuarioDTOTest,
  sin declarar todavía resultados. La comparación SHA-256 de los archivos de src
  detectó cambios únicamente en UsuarioDTO.java y UsuarioDTOTest.java.
  Git diff --check no detectó errores de espacios. No se realizaron
  staging, commit o push ni se volvieron a ejecutar las suites anteriores.

Comprobación posterior informada por el usuario y contrastada en lectura:

- BUILD SUCCESSFUL en 5 segundos: tres tareas ejecutadas y una UP-TO-DATE.
- Los siete XML del 4 de noviembre de 2026 tienen marcas entre
  09:23:14Z y 09:23:16Z: UsuarioControllerTest 6, WebAuthenticationTest 3,
  UsuarioDetailsServiceTest 6, RegistroUsuarioServiceTest 10,
  UsuarioServiceTest 10, UsuarioDTOTest 7 y RegistroUsuarioDTOTest 10.
  Total 52; todos con cero fallos, errores u omisiones.
- Los siete casos del DTO figuran satisfactorios, incluidos los tres
  nuevos y las aserciones ampliadas sobre rol singular. UsuarioDTO.class
  tiene fecha UTC 09:23:12Z y UsuarioDTOTest.class 09:23:13Z, posteriores
  a sus fuentes de 09:21:38Z; se recompilaron los archivos modificados.
- El cambio queda verificado para esos casos aislados. No acredita
  consultas JPA, datos existentes migrados, sesiones o navegación real
  del frontend, ni habilita el flujo exclusivo de TECNICO.
- El asistente solo leyó resultados, fechas y código y actualizó el MD.
  No se modificaron Java ni otras pruebas durante esta corroboración,
  ni se ejecutaron aplicaciones, SQL u operaciones de escritura Git.

### Etapa 36 Enum de rol legado retirado y verificado en aislamiento

Fecha: 4 de noviembre de 2026.

Estado: bloque aplicado por el asistente con autorización puntual del
usuario, siguiendo la propuesta. Se modificaron Usuario, el registro y
las dos pruebas indicadas; tras retirar sus consumidores se eliminó
CodigoRol.java. El usuario completó el paso 5 con BUILD SUCCESSFUL en
6 segundos. El asistente contrastó los siete XML locales: 52 pruebas,
sin fallos, errores u omisiones. Se verifica este bloque en aislamiento,
no una migración o persistencia real. El asistente no ejecutó Gradle.

- Se releyeron Usuario, CodigoRol, RegistroUsuarioService y las dos pruebas
  de servicios afectadas. La búsqueda en src confirmó que los únicos usos
  restantes de CodigoRol están en esos cuatro archivos y el propio enum.
  esAdministrador y puedeAlquilar no tienen llamadas en el código actual.
- Se retiraron de Usuario el campo enum rol y su anotación, sus
  accesores getRol/setRol y los dos métodos de conveniencia antiguos sin
  consumidores. Se conserva usuarioRoles y su getter. No se eliminan los
  getters getRol de UsuarioRol ni UsuarioDTO: representan contratos distintos.
- En RegistroUsuarioService se quitaron únicamente el import de
  CodigoRol y usuario.setRol(CodigoRol.CLIENTE). Se conservan activo true,
  búsqueda de CLIENTE en el catálogo, persistencia transaccional de la
  asignación, sincronización de la colección y flush antes de devolver DTO.
- RegistroUsuarioServiceTest sustituyó la aserción del enum por la lista
  CLIENTE del DTO; se mantienen sus comprobaciones de la asignación real.
  UsuarioServiceTest incorporó una asignación CLIENTE válida en memoria
  en su preparación (usuario id 1 y rol id 2 con ReflectionTestUtils) y
  comprueba mediante aserciones la cantidad de asignaciones y los roles del
  resultado al actualizar. Ambas pruebas usan List.of desde java.util.List,
  disponible en JDK 11; los resultados posteriores se registran debajo.
- Solo después de quitar sus consumidores se eliminó el archivo
  CodigoRol.java, no Rol.java. No se retiran otros enums ni se modifica
  seguridad, frontend, controlador, DTO, DER o esquema de base de datos.
- Retirar el mapeo Java no migra asignaciones existentes ni garantiza que
  Hibernate elimine una columna antigua con ddl-auto=update. No se propone
  SQL destructivo ni se certifican datos reales migrados. Las cuentas que
  requieran acceso deben tener sus asignaciones en usuario_rol.
- Los cambios quedaron guardados en disco y se dejó al usuario el paso 5
  mediante el mismo comando filtrado de siete suites. Durante la edición
  el asistente no ejecutó Gradle, la aplicación, SQL ni escrituras Git.
- Revisión estática posterior: los cuatro archivos modificados coinciden
  exactamente con los reemplazos propuestos frente a sus contenidos al
  iniciar esta intervención. La comparación SHA-256 de src confirmó solo
  esos cuatro cambios y la eliminación de CodigoRol.java, sin archivos
  nuevos ni cambios en los demás fuentes/recursos. Rol.java, UsuarioRol,
  sus accesores y UsuarioDTO se conservan intactos en este paso. La búsqueda
  de CodigoRol y de los dos métodos retirados no devuelve referencias en
  src. Git diff --check no detectó errores de espacios. Estas comprobaciones
  estáticas no sustituyen compilar y ejecutar las pruebas, cuya evidencia
  posterior se registra a continuación.

Comprobación posterior informada por el usuario y contrastada en lectura:

- BUILD SUCCESSFUL en 6 segundos: tres tareas ejecutadas y una UP-TO-DATE.
- Los siete XML del 4 de noviembre de 2026 tienen marcas entre
  09:34:17Z y 09:34:19Z: UsuarioControllerTest 6, WebAuthenticationTest 3,
  UsuarioDetailsServiceTest 6, RegistroUsuarioServiceTest 10,
  UsuarioServiceTest 10, UsuarioDTOTest 7 y RegistroUsuarioDTOTest 10.
  Total 52; todos con cero fallos, errores u omisiones. Los dos casos
  modificados de registro/actualización figuran satisfactorios.
- Usuario.class y RegistroUsuarioService.class tienen fecha UTC 09:34:15Z;
  las dos clases de prueba modificadas, 09:34:16Z. Son posteriores a sus
  fuentes de 09:31:46Z/09:31:47Z, por lo que el resultado incluye recompilar
  los cuatro archivos, no solo consultar resultados anteriores.
- No existe CodigoRol.java en src ni CodigoRol.class en la salida principal
  de compilación. La búsqueda de sus referencias y de los dos métodos
  retirados sigue sin coincidencias en los fuentes Java de src.
- El asistente solo leyó resultados, fechas y código y actualizó este MD;
  no modificó otros archivos ni ejecutó Gradle, aplicaciones, SQL o
  escrituras Git durante esta corroboración. El acceso de cuentas existentes
  depende de sus asignaciones en usuario_rol; no se certifica su migración.

El archivo eliminado no estaba versionado en el índice de Git al empezar
este bloque. Para permitir recuperar su contenido, se conserva aquí como
antecedente únicamente; no es una clase vigente ni debe copiarse para
mantener dos fuentes de roles:

```java
package DSII.TurismoSustentable.models;

public enum CodigoRol {
    CLIENTE,
    ADMIN,
    TECNICO
}
```

### Etapa 37 Consulta propia de TECNICO configurada, regresión verificada

Fecha: 4 de noviembre de 2026.

Estado: cambio puntual aplicado por el usuario. El asistente revisó el
archivo y su diff: solo se agregó TECNICO al matcher GET del usuario actual.
El usuario ejecutó el comando filtrado con BUILD SUCCESSFUL en 4 segundos;
se contrastaron siete XML, 52 pruebas sin fallos, errores u omisiones.
Esta regresión no ejercita todavía la regla de autorización HTTP.

- Se revisaron WebAuthorization, getUsuarioActual, la autenticación y las
  pruebas existentes, y el bloque de roles del documento 25. La regla
  específica GET /api/usuarios/actual exigía ADMIN o CLIENTE;
  una cuenta exclusivamente TECNICO no la satisfacía antes de este paso.
- El usuario agregó únicamente TECNICO a hasAnyAuthority de ese matcher
  exacto, manteniéndolo antes de la regla general /api/usuarios/**, que
  sigue reservada a ADMIN. No se amplían los permisos de listado,
  consulta por ID, administración, alquiler ni métodos de escritura.
- El controlador conserva el email del Principal para identificar la
  cuenta consultada; no se acepta un ID o email libre desde el formulario.
  Este cambio no modifica la autenticación por email ni habilita el panel
  técnico o una nueva redirección de login; esos recorridos están pendientes.
- La comprobación propuesta utiliza el mismo comando filtrado de siete
  suites para compilar y detectar regresiones. Las 52 pruebas existentes
  no ejercitan la cadena de filtros HTTP de WebAuthorization: que pasen
  no certificará por sí solo el acceso/denegación del nuevo matcher.
  Su prueba HTTP aislada queda para un bloque específico posterior, sin
  ejecutar el contexto de aplicación que utiliza una base no aislada.
- Comprobación: los siete XML del 4 de noviembre de 2026 tienen marcas
  entre 09:44:49Z y 09:44:51Z. UsuarioControllerTest 6,
  WebAuthenticationTest 3, UsuarioDetailsServiceTest 6,
  RegistroUsuarioServiceTest 10, UsuarioServiceTest 10, UsuarioDTOTest 7
  y RegistroUsuarioDTOTest 10; total 52, sin fallos, errores u omisiones.
  El usuario informó dos tareas ejecutadas y dos UP-TO-DATE.
- WebAuthorization.class tiene fecha UTC 09:44:48Z, posterior al fuente
  de 09:44:35Z: se recompiló el cambio. Se verificó además que el matcher
  permanece antes de /api/usuarios/** y no se amplió otra regla. El
  asistente solo leyó fuentes, diff y resultados y actualizó este diario;
  no ejecutó Gradle, aplicación, SQL ni operaciones de escritura Git.

### Etapa 38 Pruebas HTTP aisladas de autorización verificadas

Fecha: 4 de noviembre de 2026.

Estado: dependencia y clase de prueba incorporadas por el usuario, quien
informó ejecución satisfactoria. El asistente revisó los ocho XML locales:
59 pruebas, sin fallos, errores u omisiones, incluidos los siete casos HTTP.
Se confirma el alcance MVC aislado definido debajo, no persistencia real
ni login completo. El asistente no ejecutó Gradle, la aplicación o SQL.

- Se agregó spring-security-test únicamente como testImplementation,
  sin versión explícita, bajo la gestión de dependencias actual de Boot.
  No se actualizan Spring Boot, Spring Security, Gradle ni JDK.
- Nueva clase incorporada: configurations.WebAuthorizationTest, con WebMvcTest
  y ContextConfiguration limitada a UsuarioController y WebAuthorization.
  El controlador y la cadena de filtros son reales; sus cuatro
  colaboradores y UserDetailsService son MockBean. Se simulan sesiones
  con WithMockUser y authorities sin prefijo ROLE_, no el proceso de login.
  No se utiliza SpringBootTest, servidor de aplicación ni repositorio real.
- Siete casos incorporados y satisfactorios: tres roles consultan su propio perfil con 200;
  anónimo recibe 401; CLIENTE y TECNICO reciben 403 al listar usuarios,
  consultar uno por ID o acceder a una ruta administrativa; ADMIN puede
  listar y consultar por ID. Se verifican también las llamadas esperadas
  o ausentes al repositorio simulado. La consulta propia ignora el email
  arbitrario enviado como parámetro y usa el Principal.
- Las cuentas y catálogos de la muestra se construyen en memoria con las
  entidades actuales; no se ejecutan inicializadores, Hibernate o SQL ni
  se envían correos. No se cambian las reglas de autorización de producción.
- El alcance verifica MVC y las reglas de acceso de esas rutas, no el
  recorrido completo de login, sesiones reales de navegador, revocación,
  JPQL, capacidad, mantenimiento ni migración de cuentas existentes.
- Se consultó documentación oficial de la línea 2.7 sobre
  [WebMvcTest](https://docs.spring.io/spring-boot/docs/2.7.18/api/org/springframework/boot/test/autoconfigure/web/servlet/WebMvcTest.html)
  y la API 5.7 de
  [WithMockUser](https://docs.spring.io/spring-security/site/docs/5.7.0/api/org/springframework/security/test/context/support/WithMockUser.html).
  Sus versiones documentales no son una propuesta de actualizar librerías.
  WebMvcTest limita la autoconfiguración al ámbito MVC; WithMockUser con
  authorities no agrega el prefijo ROLE_ que no utiliza esta aplicación.
- El usuario ejecutó las ocho suites explícitas, incluyendo
  WebAuthorizationTest, no la suite completa que contiene el contexto
  no aislado. Los ocho XML del 4 de noviembre de 2026 tienen marcas entre
  09:58:36Z y 09:58:40Z: WebAuthorizationTest 7, UsuarioControllerTest 6,
  WebAuthenticationTest 3, UsuarioDetailsServiceTest 6,
  RegistroUsuarioServiceTest 10, UsuarioServiceTest 10, UsuarioDTOTest 7
  y RegistroUsuarioDTOTest 10. Total 59; todos con cero fallos, errores
  u omisiones. Los siete nombres de caso HTTP coinciden con los propuestos.
- WebAuthorizationTest.class tiene fecha UTC 09:58:35Z, posterior al
  fuente de 09:58:10Z y a build.gradle de 09:57:21Z. Esta ejecución incluye
  compilar la prueba nueva con la dependencia añadida. El JAR resuelto es
  spring-security-test 5.7.10 y se comprobó mediante lectura del ZIP que
  contiene org/springframework/security/test/context/support/WithMockUser.class.
- El usuario informó siete marcas rojas cannot resolve symbol WithMockUser
  en IntelliJ pese a la ejecución correcta. El import y la dependencia son
  los propuestos; .idea/gradle.xml enlaza el proyecto actual con JVM 11.
  La evidencia apunta a falta de sincronización o indexación del IDE,
  no a un símbolo ausente del classpath de Gradle. Es un diagnóstico
  inferido: la recuperación visual del editor requiere confirmación del
  usuario y no se declara realizada.
- Se recomienda sincronizar el proyecto desde la ventana Gradle y esperar
  que finalice la indexación, según la documentación de
  [JetBrains](https://www.jetbrains.com/help/idea/work-with-gradle-projects.html).
  No se propone quitar el import, cambiar la versión, agregar dependencias
  a mano en los módulos ni borrar cachés o configuración del proyecto.
- Durante esta corroboración el asistente solo leyó fuentes, XML, fechas,
  configuración de enlace y el JAR; actualizó únicamente este diario.
  No cambió código, dependencias, archivos de IntelliJ, base de datos o Git.
- Después de la indicación de sincronizar, el usuario informó que las
  marcas rojas aparentemente desaparecieron. Se registra su confirmación,
  sin afirmar una inspección visual del IDE por parte del asistente.
  También informó BUILD SUCCESSFUL en 1 segundo con las cuatro tareas
  UP-TO-DATE. Los ocho XML conservan sus marcas de 09:58:36Z a 09:58:40Z:
  Gradle reutilizó la ejecución anterior de 59 pruebas correctas; no se
  contabiliza como una nueva ejecución ni como nuevos casos superados.
  El asistente no forzó repetir tareas ni borró cachés. Esta corroboración
  vuelve a actualizar únicamente este diario y no realiza escrituras Git.

### Etapa 39 Preparación de la consulta por nickname

Fecha: 4 de noviembre de 2026.

Estado: anotación incorporada por el usuario y compilación informada como
correcta. El asistente revisó el archivo completo, el diff y la fecha del
.class. Solo actualiza este diario; no modificó código ni ejecutó Gradle,
SQL o la aplicación. La última ejecución de pruebas sigue siendo la de
59 casos correctos de la etapa 38; no se declara una nueva ejecución aquí.

- Punto de partida comprobado: rama codex/refactorizacion-practicas-iii,
  HEAD 522cb9a, Adaptar roles a UsuarioRol y agregar pruebas de seguridad.
  El usuario informó push correcto y estado sincronizado; git status local
  estaba limpio antes de añadir esta nueva entrada documental. El asistente
  no hizo staging, commit, push ni operaciones sobre la base de datos.
- Se revisaron UsuarioRepository, UsuarioDetailsService, Usuario,
  RegistroUsuarioDTO, los consumidores del Principal y el bloque USUARIO
  aprobado en el documento 25. El nickname ya se normaliza con trim y
  minúsculas y tiene unicidad independiente del email. La autenticación
  actual sigue consultando email y el Principal se utiliza como email en
  los consumidores existentes; no se cambiarán a nickname silenciosamente.
- El repositorio ya declaraba findByNicknameIgnoreCase. El usuario reutilizó
  ese método, agregando únicamente EntityGraph con persona.genero y
  usuarioRoles.rol, en lugar de duplicar una consulta por nickname. El
  import EntityGraph ya existe y las rutas coinciden con las entidades.
  No se añaden atributos, JPQL, imports, filtros de activo ni otro método.
- La consulta conserva Optional<Usuario> y la comparación sin distinguir
  mayúsculas. No elimina espacios por sí sola; la normalización de entrada
  del futuro cargador se adaptará en su bloque. Se conserva la señal de
  cuenta inactiva para que el proveedor de autenticación la rechace;
  esta consulta no concede permisos ni permite cuentas sin roles.
- Los métodos por email, incluidos los de recuperación y del usuario
  actual, permanecen intactos. Tampoco cambian UsuarioDetailsService,
  WebAuthorization, WebAuthentication o el frontend en este paso. Añadir
  el grafo no activa todavía el login por nickname ni modifica contraseñas.
- El usuario agregó la anotación, guardó e informó compileJava correcto.
  Esa tarea comprueba compilación, no carga real del grafo, consultas SQL,
  índices/unicidad en la base o login. Las pruebas de persistencia siguen
  pendientes; no se ejecutará el contexto SpringBootTest no aislado.
- Corroboración del 4 de noviembre de 2026: el diff de UsuarioRepository
  respecto de 522cb9a contiene exclusivamente las cuatro líneas de la
  anotación propuesta, delante del método existente. No hay métodos
  duplicados ni cambios en imports, consultas o firmas anteriores. Las
  rutas del grafo corresponden a persona/genero y usuarioRoles/rol de
  las entidades actuales. UsuarioRepository.class tiene fecha UTC
  10:21:14Z, posterior al fuente de 10:21:03Z, consistente con la
  compilación informada. No se ha validado todavía contra una base real.

### Etapa 40 Ingreso por nickname verificado en servicio y gestor

Fecha: 4 de noviembre de 2026.

Estado: bloque aplicado por el asistente con autorización puntual del
usuario, siguiendo los siete cambios propuestos y preservando la edición
previa de UsuarioRepository. El usuario completó la parte 5 con BUILD
SUCCESSFUL en 8 segundos, cuatro tareas ejecutadas. El asistente contrastó
los ocho XML locales: 60 pruebas, sin fallos, errores ni omisiones. Se
verifican los casos de servicio/gestor y la regresión definida, no el POST
de login ni el recorrido de navegador o persistencia real. El asistente
no ejecutó Gradle, la aplicación, SQL o escrituras Git.

- Se releyeron UsuarioDetailsService, sus seis pruebas, WebAuthenticationTest,
  WebAuthorization, index.html, index.js, el foco del ingreso desde mapa.js
  y los consumidores del Principal en usuarios/alquileres. Para no dejar
  el contrato de ingreso a medias, el bloque se aplicó coordinado en
  esos siete archivos de servicio/configuración/frontend/pruebas.
- UsuarioDetailsService busca por findByNicknameIgnoreCase después de
  rechazar entrada nula/blanca y normalizar trim/minúsculas con Locale.ROOT.
  No hay fallback de búsqueda por email. Se conservan hash, todas las
  autoridades desde UsuarioRol, enabled según activo y rechazo sin roles.
- Compatibilidad interna explícita: UserDetails.getUsername y Principal
  conservan el email de la cuenta encontrada, aunque la credencial de
  ingreso sea el nickname. Así no se sustituyen por nickname las búsquedas
  de propietario que hoy usan email en UsuarioController, AlquilerController
  y AlquilerService. Es un puente de identidad de sesión, no permiso para
  autenticar por email ni cambio de las FK, hashes o recuperación.
- WebAuthorization lee el parámetro nickname en POST /api/login; password
  conserva su nombre. index.html utiliza input text y etiqueta Nombre
  de usuario (nickname), id login-nickname y autocomplete username.
  index.js lee ese campo, envía nickname y adapta el mensaje 401.
  mapa.js actualiza únicamente el ID al que dirige el foco. Las búsquedas
  confirmaron que no hay otros consumidores de login-email en src.
- UsuarioDetailsServiceTest y WebAuthenticationTest incorporan constante
  NICKNAME y preparan/verifican el nuevo método, manteniendo EMAIL en los datos
  de la cuenta y las aserciones de identidad autenticada. Se conserva la
  normalización probada con ANA.PEREZ rodeado de espacios y todos los casos
  de contraseña, roles e inactividad. Se renombra el caso de nickname
  nulo/blanco y su variable local, sin inventar validaciones nuevas.
- Se agregó el cuarto caso propuesto de WebAuthenticationTest: presentar
  email como credencial produce BadCredentialsException al no existir
  ese nickname; la verificación de consultas impide introducir un fallback.
  Este caso figura satisfactorio en la ejecución posterior. No se modifican WithMockUser del
  test HTTP: siguen usando el email como identidad interna de sesión.
- No se cambian correos, recuperación, registro, permisos, contratos de
  alquiler, principal de sesiones existentes ni redirecciones por rol.
  El recorrido visual exclusivo de TECNICO continúa pendiente; este bloque
  no crea su panel ni le otorga permisos de cliente o administración.
- El usuario ejecutó las ocho suites explícitas; los resultados se detallan
  debajo. Sus casos cubren el cargador real y gestor con BCrypt y
  las reglas GET ya definidas, pero todavía no comprueban el nombre del
  parámetro de login mediante POST HTTP: se preparará esa prueba específica
  en el bloque siguiente. También sigue pendiente el recorrido de navegador
  y la persistencia real; no se ejecutará SpringBootTest no aislado.
- Revisión estática posterior: la comparación SHA-256 de src con el estado
  anterior a esta intervención confirmó modificaciones solo en los siete
  archivos previstos, sin archivos nuevos o eliminados. Se preservaron
  UsuarioRepository, el resto de servicios/controladores, entidades,
  recuperación, configuración de base y las otras seis clases de prueba.
- Se cotejaron los siete contenidos contra los reemplazos propuestos y
  coinciden. Al guardar, apply_patch añadió un salto final de archivo que
  faltaba en WebAuthorization.java, index.html, index.js y mapa.js; se
  distingue este cambio de formato de la lógica y no se cambió Git/config.
  No quedan referencias a login-email en src. El ID del formulario y sus
  dos consumidores coinciden, y el parámetro enviado y el leído por Spring
  son nickname; password permanece intacto. Git diff --check no detectó
  errores de espacios, únicamente avisos de normalización LF/CRLF.
- UsuarioDetailsServiceTest conserva seis casos, con el nombre de entrada
  inválida adaptado. WebAuthenticationTest contiene cuatro, incluido el
  rechazo del email como credencial. Sus aserciones de identidad siguen
  esperando EMAIL. Ese recuento previo fue estático; la ejecución posterior
  y su alcance se registran a continuación.

Comprobación posterior informada por el usuario y contrastada en lectura:

- BUILD SUCCESSFUL en 8 segundos, con las cuatro tareas ejecutadas.
- Los ocho XML del 4 de noviembre de 2026 tienen marcas entre 10:43:20Z
  y 10:43:23Z: WebAuthorizationTest 7, UsuarioControllerTest 6,
  WebAuthenticationTest 4, UsuarioDetailsServiceTest 6,
  RegistroUsuarioServiceTest 10, UsuarioServiceTest 10, UsuarioDTOTest 7
  y RegistroUsuarioDTOTest 10. Total 60; todos con cero fallos, errores
  u omisiones. Figura satisfactorio rechazaEmailComoIdentificadorDeIngreso.
- UsuarioDetailsService.class y WebAuthorization.class tienen fecha UTC
  10:43:17Z, posterior a sus fuentes de 10:37:28Z/10:37:29Z. Las dos clases
  de prueba modificadas tienen fecha 10:43:19Z, posterior a sus fuentes de
  10:38:00Z. La comprobación incluye recompilación, no resultados anteriores.
- La muestra confirma nickname normalizado, hash conservado, autoridades,
  inactividad, credenciales incorrectas y rechazo de email como identificador
  en el cargador/gestor. El Principal continúa siendo el email esperado.
  No se presenta el test GET con WithMockUser como evidencia de un login
  HTTP real: falta probar POST /api/login con nickname y posterior consulta
  propia en la misma sesión simulada, además del navegador y la base real.
- El asistente únicamente leyó XML, fechas y documentación y actualizó
  este diario; no cambió código ni volvió a ejecutar pruebas o aplicaciones.

### Etapa 41 Pruebas de login HTTP y sesión verificadas

Fecha: 4 de noviembre de 2026.

Estado: bloque aplicado por el asistente con autorización puntual del
usuario, siguiendo lo propuesto únicamente en WebAuthorizationTest y
este diario. El usuario completó la parte 3 con BUILD SUCCESSFUL en
7 segundos: dos tareas ejecutadas y dos UP-TO-DATE. El asistente contrastó
los ocho XML locales: 62 pruebas, sin fallos, errores ni omisiones.
Se verifica el alcance MVC simulado definido debajo, no una base real.
El asistente no ejecutó Gradle, aplicaciones, SQL ni escrituras Git.

- Se amplió exclusivamente WebAuthorizationTest. Se mantienen sus
  siete casos y colaboradores simulados, sin modificar producción o añadir
  dependencias. El contexto MVC incorpora también SecurityConfig para
  utilizar el PasswordEncoder BCrypt real de la aplicación.
- Se agregaron la inyección de PasswordEncoder e imports de User,
  UsernameNotFoundException, MediaType, MockHttpSession, MvcResult,
  assertNotNull y post. Se actualizó el comentario del UserDetailsService
  simulado, porque ahora esta clase incluye peticiones de login.
- Primer caso nuevo, sin WithMockUser: construir la cuenta CLIENTE en
  memoria, hashear su contraseña con el encoder real y simular únicamente
  la respuesta de UserDetailsService a su nickname. POST /api/login enviará
  nickname/password como parámetros de formulario y deberá responder 200.
  Se recuperará la MockHttpSession creada por ese login, se comprobará
  que existe y se reutilizará en GET /api/usuarios/actual. La respuesta
  deberá ser 200 e identificar la misma cuenta, email, nickname y rol.
- Segundo caso nuevo, también anónimo: POST /api/login con el parámetro
  antiguo email y sin nickname deberá responder 401. Se simula
  UsernameNotFoundException al consultar el identificador vacío y se
  verifica esa llamada: no se hace pasar un retorno null artificial del
  mock por un rechazo de credenciales real. No debe consultar el repositorio.
- Se conservarán verificaciones de las llamadas esperadas y ausencia de
  llamadas extra. No se incorporará WithMockUser a estos dos casos, ya que
  la autenticación del positivo debe proceder del POST y su sesión.
- Alcance: filtros de login, parámetro configurado, BCrypt real, persistencia
  de la identidad en una sesión HTTP simulada y acceso posterior al perfil.
  UserDetailsService y el repositorio siguen simulados en esta clase: no
  es una prueba de base real, navegador, cookies reales o del cargador
  completo en contexto. El cargador/gestor ya tiene pruebas separadas.
- Se inspeccionaron en lectura con javap los binarios locales de Security
  5.7.10: el configurador obtiene el único PasswordEncoder del contexto y
  lo asigna a DaoAuthenticationProvider; User.withUsername y authorities
  están disponibles. No se ejecutó el código de la aplicación ni Gradle.
- El archivo quedó guardado y el usuario comprobó con las mismas ocho
  suites filtradas. No se ejecutó el SpringBootTest no aislado ni se
  requirió XAMPP. Los resultados se detallan a continuación.
- Revisión estática posterior: la comparación SHA-256 de src respecto al
  inicio de esta intervención confirmó un único archivo modificado,
  WebAuthorizationTest.java, sin archivos nuevos o eliminados. Se preserva
  todo el código de aplicación y las ediciones previas del usuario.
- Se verificó que al descontar los imports, campo, entrada de contexto,
  comentario y dos casos añadidos, el contenido anterior queda intacto.
  Se normalizó el salto final de archivo al guardar. Se conservan los
  siete casos y sus auxiliares y se cuentan nueve métodos @Test; los dos
  nuevos no tienen WithMockUser. Git diff --check no detectó errores de
  espacios. Estas verificaciones no sustituyen compilar y ejecutar los
  casos; los resultados posteriores se registran debajo.

Comprobación posterior informada por el usuario y contrastada en lectura:

- Los ocho XML del 4 de noviembre de 2026 tienen marcas entre 10:56:18Z
  y 10:56:22Z: WebAuthorizationTest 9, UsuarioControllerTest 6,
  WebAuthenticationTest 4, UsuarioDetailsServiceTest 6,
  RegistroUsuarioServiceTest 10, UsuarioServiceTest 10, UsuarioDTOTest 7
  y RegistroUsuarioDTOTest 10. Total 62; todos con cero fallos, errores u
  omisiones. Los dos casos nuevos de login y sesión figuran satisfactorios.
- WebAuthorizationTest.class tiene fecha UTC 10:56:17Z, posterior al
  fuente de 10:53:11Z: la ejecución incluye recompilar la ampliación.
  El positivo verifica el POST con nickname, creación de MockHttpSession
  y posterior consulta propia; el negativo, rechazo 401 del parámetro email
  antiguo con consulta vacía al cargador simulado y sin acceso al repositorio.
- Solo se leyeron resultados, fechas y fuentes y se actualizó este MD.
  No se ejecutaron de nuevo pruebas, aplicaciones, SQL ni escrituras Git.

### Etapa 42 Cargador local de catálogos creado y compilado

Fecha: 4 de noviembre de 2026.

Estado: clase creada por el usuario y compileJava informado con BUILD
SUCCESSFUL en 2 segundos, una tarea ejecutada. El asistente revisó el
archivo completo y la fecha del .class; actualiza solo este diario. No
se ejecutó la aplicación ni se verificaron filas en una base real. La
última ejecución de pruebas sigue siendo la de 62 casos de la etapa 41.

- Se revisaron el perfil refactor, los cuatro catálogos y repositorios,
  RegistroUsuarioDTO, el HTML/JS de registro y la ausencia de runners.
  El frontend de registro todavía envía genero como código y no envía
  nickname, tipoDocumentoId, numeroDocumento o paisEmisorId; no es
  compatible aún con el contrato nuevo. Tampoco hay carga inicial o
  endpoints de consulta de los catálogos. Son pendientes distintos.
- El usuario creó configurations.CargaCatalogosRefactor como Component,
  Profile refactor y CommandLineRunner, con transacción en run y las cuatro
  dependencias por constructor. El main no se modifica ni se vuelve a
  introducir allí una lista de cuentas, bicicletas o alquileres de ejemplo.
- Carga preparada: roles ADMIN/CLIENTE/TECNICO, los cinco géneros ya usados
  en el proyecto y los tipos DNI/PASAPORTE. PAIS iniciará solo con AR,
  Argentina, como muestra mínima de desarrollo para probar el registro;
  esto no fija una política de países admitidos ni constituye un listado
  mundial definitivo. La lista completa/importación sigue pendiente.
- Se reutilizan findByCodigo existentes; solo se insertan códigos ausentes,
  sin IDs fijos, sobrescribir datos existentes ni realizar bajas. La
  transacción agrupa los cambios de ese arranque. La repetición secuencial
  no duplica catálogos; no se afirma resolver arranques simultáneos en
  varias instancias. Las unicidades de los códigos permanecen en las entidades.
- El cargador solo se activa con el perfil refactor. Compilar no lo ejecuta
  ni crea tablas. El perfil actual contiene create-drop: antes de un futuro
  arranque deberá confirmarse la base local descartable y el perfil para
  evitar tocar turi u otra base con datos. No se proponen SQL ni arranque
  de aplicación en este bloque; no se cambia la configuración de conexión.
- El usuario creó el archivo, guardó y comprobó con compileJava. El fuente
  tiene fecha UTC 11:09:24Z y CargaCatalogosRefactor.class 11:09:37Z,
  consistente con la compilación informada. Se corroboraron perfil,
  constructor con las cuatro dependencias, transacción en run, códigos y
  ausencia de IDs fijos, modificaciones o borrados de registros existentes.
- Esto no verifica aún persistencia, carga real, condiciones de perfil en
  un arranque o registro desde navegador. El asistente no ejecutó Gradle,
  aplicación, SQL ni escrituras Git. Se continuará con las respuestas de
  catálogos y el formulario, sin iniciar todavía una base descartable.

### Etapa 43 DTO de opciones de catálogo creado y compilado

Fecha: 4 de noviembre de 2026.

Estado: DTO creado por el usuario y compileJava informado con BUILD
SUCCESSFUL en 2 segundos, una tarea ejecutada. El asistente revisó el
contenido y la fecha del .class y actualiza solo este diario. La última
ejecución de pruebas sigue siendo la de 62 casos de la etapa 41; no se
ejecutó la aplicación ni se verificó una base real.

- El usuario creó dto.CatalogoDTO con id Integer, codigo String y nombre
  String, constructor de tres argumentos y getters. Los campos finales
  representan una opción de salida; no hay setters, anotación Entity o
  tabla nueva ni cambios al DER o a las entidades de catálogo.
- El contrato se usará en listas de géneros, tipos de documento y países
  para que los combos muestren una etiqueta y envíen el ID real. El ID de
  un ejemplo JSON no será un identificador fijo de carga inicial.
- El DTO no consulta repositorios ni concede permisos. El mapeo de las
  etiquetas de género, que no tiene columna nombre, se hará explícitamente
  en el bloque consumidor; no se añade esa columna de forma implícita.
- Los endpoints y su autorización pública GET limitada se incorporarán
  después junto con sus pruebas. No se autorizará un comodín para roles,
  estados u otros catálogos ni se expondrán cuentas o datos personales.
- El usuario creó el archivo y ejecutó compileJava. CatalogoDTO.java tiene
  fecha UTC 11:13:30Z y su .class 11:13:47Z, consistente con la compilación
  informada. Se comprobaron los tres campos finales, constructor y getters
  conforme al código propuesto. No se cambió el formulario ni se ejecutaron
  Gradle, aplicación, SQL o escrituras Git por parte del asistente.

### Etapa 44 Consultas públicas de catálogos creadas y compiladas

Fecha: 4 de noviembre de 2026.

Estado: bloque aplicado por el asistente con autorización puntual del
usuario, exclusivamente en CatalogoController y WebAuthorization, además
de este diario. El usuario informó compileJava con BUILD SUCCESSFUL en
1 segundo y una tarea UP-TO-DATE; el asistente corroboró los fuentes y
sus salidas compiladas existentes. Esta última invocación reutilizó la
compilación, no se declara como una ejecución nueva de pruebas. La última
evidencia de pruebas sigue siendo la de 62 casos de la etapa 41. El
asistente no ejecutó Gradle, aplicación, SQL o escrituras Git.

- Se creó controllers.CatalogoController bajo /api/catalogos,
  con dependencias por constructor a GeneroRepository, TipoDocumentoRepository
  y PaisRepository. Los tres GET devolverán List<CatalogoDTO>, sin cambios
  a entidades, carga inicial o DER. Son lecturas simples de catálogos sin
  relaciones que cargar; no se agrega un ABM o flujo de escritura.
- GET /generos ordenará por codigo. Una traducción de presentación para
  los cinco códigos actuales conservará las etiquetas del formulario;
  si aparece otro código, se mostrará ese código. Los registros admitidos
  siguen siendo los consultados a la base: el mapa de etiquetas no genera
  filas, no filtra opciones ni es otra fuente de roles o permisos.
- GET /tipos-documento y GET /paises ordenarán por nombre y codigo y
  proyectarán ID real, código y nombre persistidos. No se devolverán
  entidades JPA directamente ni se fijarán IDs de ejemplo.
- WebAuthorization incorpora una excepción solo para GET de las rutas exactas
  /api/catalogos/generos, /api/catalogos/tipos-documento y
  /api/catalogos/paises, antes de las reglas generales de la API.
  No se utilizará permitAll para /api/catalogos/** ni /api/**. No se
  exponen roles o estados, usuarios, modificaciones o asignaciones.
- Los endpoints se requieren sin autenticación porque alimentarán el
  registro público; no cambian los permisos ya definidos para cuentas,
  alquileres o administración. El formulario aún no se conecta en este paso.
- El usuario conserva la comprobación con compileJava. La compilación no
  verifica consultas reales, respuestas HTTP o permisos; esas pruebas se
  prepararán en el bloque siguiente. No se requiere XAMPP ni se iniciará
  la aplicación con el perfil create-drop para esta comprobación.
- Revisión estática posterior: las huellas SHA-256 de src confirmaron
  únicamente el nuevo CatalogoController.java y la modificación de
  WebAuthorization.java respecto del inicio de esta intervención. Todos
  los demás archivos, incluidas las ediciones previas del usuario, quedaron
  intactos. No se modificaron DTO, repositorios, cargador, frontend o pruebas.
- Se comparó la configuración con su contenido previo: contiene solamente
  la inserción GET propuesta, con las tres rutas exactas y sin comodines,
  antes de las reglas generales; se conserva el login por nickname y los
  demás permisos. El controlador contiene exactamente los tres GetMapping
  propuestos y ningún endpoint de escritura o consulta de roles. Se releyó
  completo y git diff --check no detectó errores de espacios en los cambios
  existentes. Esto no certifica todavía compilación, consultas SQL o HTTP.

Corroboración posterior del 4 de noviembre de 2026:

- Se revisó el controlador completo y la excepción GET: coinciden con la
  propuesta y siguen limitados a las tres lecturas indicadas. Los nombres
  de campos/getters y Sort corresponden a las entidades/repositorios.
- CatalogoController.java y WebAuthorization.java tienen fecha UTC
  11:19:45Z; ambas salidas .class, 11:22:10Z. Es evidencia consistente de
  compilación previa al resultado UP-TO-DATE informado. No se forzó
  recompilación ni se ejecutaron consultas a la base.
- El asistente actualizó solo este diario. Las respuestas HTTP y sus
  permisos específicos aún requieren la comprobación del siguiente bloque.

### Criterio rector ratificado para continuar la implementación

Fecha: 4 de noviembre de 2026.

El usuario ratifica como objetivo implementar el DER v5 conforme al
diccionario de estados y las decisiones vigentes y conseguir recorridos
funcionales. Solicita evitar código descartable, sin uso concreto o que
desvíe tiempo del backend. Esta precisión no modifica el DER ni aprueba
nuevas reglas; en esta revisión no se cambia código.

- Antes de cada bloque se identifican las entidades, relaciones, regla
  aprobada y operación del sistema que lo justifican. Las ideas del
  documento 38 y demás propuestas no aprobadas no sustituyen esa base.
- Se prioriza cerrar módulos y recorridos con sus dependencias reales:
  entidades y FK, repositorios, servicio cuando haya reglas de negocio,
  endpoints y validaciones, autorización y trazabilidad aplicables.
  Los DTO y utilidades deben tener un consumidor y propósito concretos,
  no crear capas o funcionalidades solo para un supuesto uso futuro.
- Las pruebas se concentran en reglas, integridad, permisos, transiciones,
  dinero y trazabilidad pertinentes. Compilar o pasar pruebas con mocks
  no basta para declarar funcional un módulo contra la base real.
- Se registran los puentes temporales y su salida: el Principal por email
  es compatibilidad con consumidores actuales; la carga refactor y AR
  son apoyo local de desarrollo, no migración/catálogo productivo final.
  No se perpetuará el legado de Tarjeta/Suscripcion o los enums que deban
  reemplazarse; su retirada se hará al migrar consumidores, sin borrar
  código o datos necesarios por una limpieza indiscriminada.
- La documentación distingue diseño aprobado, implementado, probado y
  pendiente, vinculando reglas, código y evidencia. Se actualizará el
  estado útil del módulo sin introducir otros entregables o funciones
  ajenas al alcance. Los originales se conservan como antecedentes.

Aplicación inmediata: los tres catálogos y su DTO/controlador dan soporte
a PERSONA y al registro del DER v5; se completará ese uso en lugar de
dejarlos aislados. El objetivo próximo es cerrar identidad/catálogos y
registro en el backend y su conexión mínima necesaria, sin rediseño
estético ni ABM genérico de catálogos. Después continúa el núcleo de
estaciones, tipos y bicicletas, alquiler/detalles, pagos/reembolsos y
mantenimiento/incidencias, respetando sus dependencias y la trazabilidad
desde las operaciones que la requieren. Esto marca una dirección de
trabajo, no certifica esos módulos ni reemplaza decisiones pendientes.

### Etapa 45 Catálogos del registro verificados mediante pruebas HTTP aisladas

Fecha: 4 de noviembre de 2026.

Estado: el usuario creó CatalogoControllerTest y ejecutó las nueve suites
explícitas con BUILD SUCCESSFUL en 9 segundos, dos tareas ejecutadas y dos
UP-TO-DATE. El asistente revisó el archivo y los nueve XML generados:
67 pruebas, sin fallos, errores u omisiones. Las marcas de las suites
abarcan 11:40:02Z a 11:40:07Z del 4 de noviembre de 2026; el nuevo fuente
tiene fecha 11:39:34Z y su .class 11:40:01Z. Las demás clases de prueba
también tienen salidas compiladas posteriores a sus fuentes. No se
volvieron a ejecutar Gradle, aplicación o SQL por parte del asistente.

- Se creó controllers.CatalogoControllerTest, con WebMvcTest y
  ContextConfiguration limitada a CatalogoController, WebAuthorization y
  SecurityConfig. Los tres repositorios y UserDetailsService son MockBean;
  el controlador, DTO, serialización y filtros de seguridad son reales.
  No se carga el runner refactor ni una base o contexto SpringBootTest.
- Cinco casos: GET anónimo de géneros, tipos de documento y países con
  los campos id/codigo/nombre e IDs de la muestra; la excepción pública
  no habilita /api/catalogos/roles o POST de un catálogo sin sesión;
  CLIENTE no puede ejecutar POST/PUT/DELETE sobre esos catálogos.
- La muestra de género contiene dos registros, no los cinco del mapa,
  para comprobar que las opciones provienen del repositorio; se verifican
  las etiquetas Femenino y No Binario. Las otras dos respuestas conservan
  los nombres persistidos. Se verifican consultas Sort y ausencia de
  interacciones adicionales con los repositorios.
- Los IDs de fixtures se asignan con ReflectionTestUtils únicamente en
  memoria, sin persistir ni fijar IDs productivos. Verificar el Sort
  solicitado no demuestra el ordenamiento ejecutado por una base real.
- La prueba tiene un consumidor/objetivo concreto: las opciones y permisos
  que necesita el registro con PERSONA y sus FK del DER v5. No agrega un
  ABM, otras tablas, estados o cambios a reglas de negocio.
- Se corroboraron cinco casos de CatalogoControllerTest, nueve de
  WebAuthorizationTest, seis de UsuarioControllerTest, cuatro de
  WebAuthenticationTest, seis de UsuarioDetailsServiceTest, diez de
  RegistroUsuarioServiceTest, diez de UsuarioServiceTest, siete de
  UsuarioDTOTest y diez de RegistroUsuarioDTOTest. La ejecución con filtros
  no incluyó el SpringBootTest no aislado de la aplicación.
- La evidencia verifica serialización HTTP y filtros con repositorios
  simulados. No demuestra carga real de catálogos, ejecución SQL, registro
  completo desde el navegador o aislamiento transaccional en MySQL. Sigue
  pendiente conectar el formulario al contrato nuevo. El asistente solo
  actualiza este diario en la revisión posterior, sin modificar src o Git.

### Etapa 46 Campos del formulario de registro adecuados al contrato nuevo

Fecha: 4 de noviembre de 2026.

Estado: el asistente aplicó el bloque propuesto con autorización puntual
del usuario, exclusivamente en registro.html y este diario. Es una parte
del cierre del registro existente, no un rediseño visual ni un módulo
nuevo. Continúa vigente la evidencia de 67 pruebas de la etapa 45; no se
ejecutaron Gradle, aplicación, SQL o escrituras Git en esta intervención.

- El formulario anterior no coincide con RegistroUsuarioDTO: faltan
  nickname, tipoDocumentoId, numeroDocumento y paisEmisorId, y envía
  genero como código en lugar de generoId. Se conservan nombre, apellido,
  email, fecha de nacimiento, contraseña y novalidate; las validaciones
  autoritativas continúan en el backend.
- Se añadió reg-nickname, de 3 a 30 caracteres, con la ayuda de
  formato aprobada. No se cambia el inicio de sesión o la recuperación.
- El selector reg-genero deja de contener los cinco códigos escritos en
  HTML; se añaden reg-tipo-documento y reg-pais-emisor. Los tres quedan
  inicialmente deshabilitados y con opción vacía para recibir registros
  reales del backend, sin IDs o listas duplicadas en el frontend.
- Se añadió reg-numero-documento como texto obligatorio de hasta 50
  caracteres, acorde al DTO. No se convierte a número, no se pierden ceros
  iniciales ni se excluyen letras de pasaportes mediante reglas no aprobadas.
- catalogos-status y registro-submit permiten mostrar la carga y mantener
  bloqueado el envío hasta tener opciones válidas. El siguiente bloque
  adaptará registro.js para consultar los tres GET, habilitar el formulario,
  construir el payload por IDs y conservar mensajes y prevención de doble
  envío. No se declara el recorrido funcional tras modificar solo HTML.
- No se solicita iniciar la aplicación o MySQL con create-drop en este
  bloque. La comprobación funcional contra la base se hará después de
  conectar ambos archivos y confirmar el perfil y destino descartable.
- Comprobación estática posterior: no hay IDs repetidos y el anidamiento
  de etiquetas es coherente; se conservan novalidate y los atributos de
  nombre, apellido, email y contraseña. Todo lo situado fuera del formulario
  permanece igual, salvo el salto de línea final del archivo. Los tres
  selectores contienen solo una opción vacía y siguen deshabilitados y
  obligatorios; el botón de envío también queda deshabilitado según lo
  propuesto. Las huellas SHA-256 de src confirman que únicamente cambió
  registro.html respecto del inicio de esta intervención, sin eliminaciones.
  git diff --check no detectó errores de espacios. Esta revisión no equivale
  a una prueba visual en navegador ni al registro funcional contra la base.

### Etapa 47 JavaScript del registro conectado al contrato de catálogos y DTO

Fecha: 4 de noviembre de 2026.

Estado: el usuario reemplazó y guardó registro.js. El asistente revisó el
archivo completo y corroboró su correspondencia con el HTML y backend;
no modificó src en esta revisión. Al proponer el bloque se revisaron el
HTML, RegistroUsuarioDTO, UsuarioController, RegistroUsuarioService,
GlobalExceptionHandler y auth.js. No se ejecutaron Gradle, aplicación,
SQL, correos o escrituras Git. La última evidencia de pruebas sigue
siendo la de 67 casos de la etapa 45; aún no hay prueba funcional de este
recorrido contra una base real.

- Al cargar la página se consultarán en paralelo solo los tres GET públicos
  de géneros, tipos de documento y países. Cada respuesta debe ser una lista
  no vacía con IDs enteros positivos y etiquetas de texto. Las opciones usan
  el id real como valor y nombre como etiqueta; no se fijan IDs ni se ofrecen
  códigos, países o roles que no provengan del backend.
- Los selectores y el envío solo se habilitarán cuando estén cargados los
  tres catálogos. Si una consulta falla o un catálogo está vacío o mal formado,
  el formulario permanecerá bloqueado y mostrará un aviso para recargar la
  página. No se incorpora otro endpoint, dependencia o ABM de catálogos.
- El POST conservará nombre, apellido, email, fechaNacimiento y password y
  añadirá nickname, tipoDocumentoId, numeroDocumento y paisEmisorId. genero
  se sustituirá por generoId. Las selecciones se convertirán a IDs numéricos
  o null; una fecha vacía se enviará como null. El documento seguirá siendo
  texto y la contraseña no se recortará ni normalizará en el frontend.
- La validación de contenido y duplicados seguirá correspondiendo al DTO,
  servicio y restricciones de base. Se conservará novalidate y no se añadirán
  reglas de negocio o validaciones paralelas distintas a las aprobadas.
- Se impedirán envíos durante la carga o mientras haya un registro en curso.
  Se conservarán el aviso de éxito y la redirección al ingreso; ante errores
  se permitirá corregir y reintentar manualmente, sin reenvíos automáticos.
- Se conservará la lista validationErrors de HTTP 400 y también se mostrarán
  los mensajes de texto 400/409 del controlador, incluyendo conflictos de
  nickname o documento. No se atribuirán todos los conflictos solo al email.
  La lista se construirá con nodos y textContent, sin interpolar mensajes
  del servidor como HTML. No se imprimirá el error Axios completo en consola
  porque podría incluir la contraseña en la configuración del POST.
- La comprobación inmediata será guardar y revisar el reemplazo. No se
  pide iniciar MySQL ni la aplicación en este bloque: antes de la prueba
  funcional se confirmará el perfil refactor, destino local y carácter
  descartable de la base create-drop. Compilar Java no prueba este JavaScript.

Corroboración posterior al guardado del usuario:

- El análisis de sintaxis JavaScript fue correcto, sin ejecutar el script.
  Los IDs literales consultados y los tres selectores existen en el HTML;
  los diez campos del payload coinciden exactamente con RegistroUsuarioDTO.
- Las tres URL coinciden con CatalogoController y la excepción GET pública
  de WebAuthorization. Las opciones conservan los IDs del backend y el
  documento sigue siendo texto; no se altera la contraseña ni se permite
  elegir roles en el registro. Se revisaron bloqueo de carga/doble envío,
  manejo de mensajes 400/409 y construcción mediante textContent.
- El archivo guardado conserva la lógica propuesta. La diferencia de
  indentación en la expresión de numeroDocumento no cambia su significado
  ni requiere modificar código. El asistente solo actualizó este diario.
- Antes de iniciar la aplicación se pide al usuario confirmar que la base
  local turismo_sustentable existe y puede recrearse sin perder información
  a conservar. El perfil refactor mantiene create-drop; sin perfil, la URL
  por defecto apunta a turi. No se inició ningún servicio ni se asumieron
  conexiones, credenciales o variables de entorno activas del IDE.

### Etapa 48 Preparación del primer arranque local para verificar el registro

Fecha: 4 de noviembre de 2026.

Estado: el usuario confirmó que turismo_sustentable fue creada desde
phpMyAdmin y está vacía, sin tablas ni datos a conservar. Se dan
instrucciones para que el propio usuario inicie MySQL y la aplicación;
el asistente no ejecutó el arranque, SQL, Gradle o escrituras Git, ni
modificó código o propiedades. La confirmación del usuario no equivale
a una consulta de la base por parte del asistente.

- El arranque se hará desde la terminal del proyecto con bootRun y perfil
  refactor explícito. Se indicará además la URL JDBC local completa por
  argumento para fijar el destino turismo_sustentable y el puerto HTTP 8080.
  No se pasarán credenciales en el comando ni se pedirá publicarlas.
- Hibernate creará las tablas correspondientes a las entidades que existen
  actualmente. Esto no significa que ya esté implementado el DER v5 completo.
  CargaCatalogosRefactor se ejecutará por el perfil y cargará los catálogos
  de apoyo local ya definidos; no creará usuarios o bicicletas de demostración.
- Se revisó CorreoService: si faltan claves o remitente, el envío se omite
  con un aviso. No se requiere configurar el correo para esta comprobación
  inicial de arranque. No se solicita registrar todavía una cuenta ni enviar
  mensajes externos; el siguiente paso dependerá del resultado del inicio.
- Se pedirá al usuario corroborar el perfil refactor y la línea Started de
  TurismoSustentableApplication, compartiendo las últimas líneas pertinentes
  sin contraseñas o claves. La consola debe permanecer ejecutándose durante
  las pruebas: create-drop también elimina las tablas al detenerse.
- Quedan pendientes el inicio real, las consultas de catálogos, el registro,
  la persistencia de PERSONA/USUARIO/USUARIO_ROL y el login por nickname.
  Las 67 pruebas previas siguen siendo evidencia aislada, no esta prueba real.

### Etapa 49 Arranque local informado y catálogos HTTP reales corroborados

Fecha: 4 de noviembre de 2026.

Estado: el usuario inició bootRun y compartió el log. A las 09:05:04 hora
local consta Tomcat en el puerto 8080 y Started TurismoSustentableApplication
en 3.253 segundos. El 80 % EXECUTING indica que bootRun permanece activo;
no es un error ni un porcentaje de carga del sitio. El fragmento recibido
no incluye la línea del perfil activo, por lo que no se declara verificada
esa línea a partir de este fragmento.

- El aviso open-in-view es una advertencia de configuración, no un fallo
  de arranque. No se modifica esa propiedad en este bloque: su revisión
  debe considerar los consumidores LAZY actuales, sin desviar esta prueba.
- El primer intento de GET del asistente fue impedido por el acceso a
  sockets del entorno restringido, no por una respuesta HTTP del backend.
  Al usar la vía autorizada, las tres consultas anónimas reales devolvieron
  HTTP 200: géneros (cinco opciones), tipos de documento (DNI/PASAPORTE)
  y países (AR/Argentina). No se fijarán estos IDs en el frontend.
- La respuesta de países conserva el formato de lista JSON con un elemento;
  se corroboró su cuerpo sin la conversión de PowerShell que desenvuelve
  colecciones de un solo elemento. Los datos y etiquetas corresponden a
  los catálogos de apoyo definidos. Esto demuestra lecturas HTTP reales
  de catálogos en la aplicación local, no todo el DER v5 implementado.
- El asistente hizo únicamente GET de esos catálogos, sin POST, login,
  escrituras de datos, SQL directo, Gradle o arranques adicionales. Solo
  se actualiza este diario; no se modifican fuentes o configuración.
- Siguiente comprobación manual: abrir registro.html servido desde el
  puerto 8080, recargar sin caché, comprobar opciones y botón habilitado,
  y enviar el formulario vacío para verificar mensajes de validación del
  backend. Todavía no se solicita crear una cuenta válida ni enviar correo.
- Quedan pendientes la observación del navegador, validaciones HTTP del
  formulario real, registro válido, persistencia de PERSONA/USUARIO/
  USUARIO_ROL y login por nickname. Las 67 pruebas aisladas no se suman a
  estas consultas como si fueran nuevos casos automatizados.

### Etapa 50 Devolución de la prueba de registro vacío y ajuste de presentación

Fecha: 4 de noviembre de 2026.

Estado: el usuario informó que los tres combos cargan opciones y que al
enviar vacío se muestra la lista de validaciones, con prefijos técnicos
como tipoDocumentoId y avisos de obligatoriedad/tamaño para nickname y
contraseña. También percibe una recarga y movimientos de scroll. No se
capturó el tráfico del navegador para confirmar una navegación real; el
handler contiene preventDefault y el envío se hace mediante Axios.

- Se revisaron registro.js, registro.html, GlobalExceptionHandler, las
  anotaciones del DTO y el CSS pertinente. El backend construye cada
  mensaje como campo + ': ' + descripción; el frontend lo presenta literal.
  Identificar campos en la API es útil, pero no es necesario mostrar sus
  identificadores técnicos en la interfaz. No se cambia el contrato HTTP.
- Los mensajes repetidos de campo vacío se explican por NotBlank y Size
  evaluados sobre el mismo valor; no prueban un doble registro ni implican
  retirar restricciones. La propuesta prioriza solo en presentación el
  mensaje obligatorio para nickname/contraseña vacíos y deduplica la lista.
- El desplazamiento es compatible con la competencia entre scrollIntoView
  y el retorno automático de foco de SweetAlert2 al cerrar. Es una hipótesis
  apoyada en código y documentación oficial, no una reproducción visual.
  Referencia: https://sweetalert2.github.io/#return-focus y fuentes oficiales
  src/instanceMethods/close.js y src/globalState.js en su repositorio.
- Se propone ajustar únicamente registro.js: sustituir el popup de carga
  por el texto Registrando... en el botón ya bloqueado, conservar el popup
  de éxito y la redirección, quitar Swal.close del catch que ya no tendrá
  un popup de carga y restaurar Registrarme si la operación falla.
- mostrarErrores quitará solo los prefijos de los diez campos del contrato,
  presentará nickname como nombre de usuario y clave como contraseña,
  conservará textContent y dará foco programático a la lista sin scroll
  automático del foco. Solo después hará scrollIntoView hacia su inicio.
  No se alteran carga de catálogos, payload, seguridad o validaciones.
- La propuesta se comprobó sintácticamente y con una simulación en memoria
  del renderizado de los doce mensajes informados: diez avisos legibles,
  sin identificadores técnicos ni avisos secundarios de tamaño sobre los
  dos campos vacíos. Esto no prueba desplazamientos o accesibilidad en un
  navegador, ni suma casos a las 67 pruebas automatizadas existentes.
- El usuario aplicó manualmente el ajuste. En la revisión posterior se
  corroboró que no queda popup de carga ni Swal.close en el catch, que se
  conserva el popup de éxito y que mostrarErrores coincide con la propuesta.
  El asistente no editó src. El usuario informa que persiste el retorno de
  scroll al botón, por lo que el ajuste no se considera solución verificada
  de ese comportamiento en el navegador donde lo observa.

### Etapa 51 Diagnóstico del retorno de scroll sin nuevas modificaciones

Fecha: 4 de noviembre de 2026.

Estado: se revisó el archivo guardado y se hizo una comprobación mediante
la interfaz del navegador integrado, en la pestaña existente de registro
local. Se utilizó la habilidad computer-use y la API de navegador disponible.
Se conservan las fuentes; se actualiza únicamente este diario. No se
ejecutan Gradle, SQL, cambios de configuración o nuevas cuentas válidas.

- Antes de pulsar Registrarme se comprobó en el DOM únicamente si cada
  campo estaba vacío; los diez lo estaban. No se leyeron ni introdujeron
  contraseñas, documentos o datos personales. Se envió solo ese formulario
  vacío y se observaron diez avisos legibles, sin prefijos técnicos.
- En la observación inmediata el foco DOM estaba en status-message y
  scrollY era 58; al finalizar la animación seguía el foco en ese resumen,
  scrollY era 246, su borde superior estaba aproximadamente en 0 y el botón
  a 1017 píxeles del borde superior de la ventana. Una captura mostró el
  resumen de errores en esa posición. No se reprodujo el retorno al botón
  en esta prueba del navegador integrado.
- Este resultado no invalida lo observado por el usuario en su sesión o
  navegador. La causa específica sigue sin confirmarse; no se añaden
  temporizadores, nuevos desplazamientos o CSS como solución no comprobada.
  Se pide identificar si el caso ocurre en Brave u otro navegador o en el
  navegador integrado; una prueba privada con el formulario vacío puede
  ayudar a distinguir condiciones de sesión, caché o extensiones, sin dar
  ninguna de ellas por demostrada.
- Las pruebas visuales no se suman a los 67 casos automatizados y no
  demuestran todavía el alta válida o el login. No se detuvo la aplicación
  create-drop ni se modificaron reglas de negocio, payload o validaciones.

### Etapa 52 Salto al reenviar con errores visibles: colapso del resumen

Fecha: 4 de noviembre de 2026.

Estado: el usuario precisó que el caso sucede al pulsar Registrarme otra
vez, cuando la lista de errores ya está visible, tanto en Brave como en
el navegador integrado. Se corroboró que en el inicio del handler submit
se oculta y vacía status-message antes de esperar el POST. La corrección
puntual se propone para que el usuario la aplique, sin editar src en esta
intervención. Solo se actualiza este diario.

- Se repitió el envío vacío con la lista ya visible mediante la interfaz
  del navegador integrado. Una traza de lectura del DOM durante 2.5 segundos
  registró scrollY 0, 277 al alcanzar el botón, 0 y luego la animación hasta
  246. El foco del resumen se mantuvo en las muestras y no se observó un
  retorno de foco al botón. No se registraron datos personales o credenciales.
- Con viewport de 1244 píxeles de altura y un resumen de varios cientos de
  píxeles, ocultarlo antes del await reduce el documento y puede recortar el
  desplazamiento disponible. Al reinsertarlo se inicia el scroll programado.
  La secuencia y la condición informada por el usuario señalan ese colapso
  entre solicitudes como causa a corregir, no el retorno de foco del popup
  de carga, que ya fue retirado. No se afirma que se haya recargado la página.
- Se indica eliminar únicamente del handler submit el bloque que declara
  statusDiv, añade d-none y llama replaceChildren antes de crear el payload.
  Se conservará el resumen anterior mientras se procesa el siguiente envío.
  mostrarErrores seguirá reemplazando su contenido cuando llegue la nueva
  respuesta, sin dejar de validar, duplicar mensajes o aceptar datos inválidos.
- No se elimina replaceChildren de mostrarErrores ni se cambian su foco,
  desplazamiento, payload, validaciones, carga de catálogos o controles de
  doble envío. No se añaden temporizadores, nuevos estilos o dependencias.
- Comprobación pendiente: guardar, Ctrl+F5 y pulsar Registrarme dos veces
  con el formulario vacío, esperando la respuesta entre intentos. No se
  requiere reiniciar create-drop o crear cuentas. El ajuste todavía no se
  declara implementado o verificado y no cambia el recuento de 67 pruebas.

Corroboración posterior: el usuario informó que el salto quedó corregido.
Se releyó registro.js y se confirmó que el handler ya no oculta ni vacía
el resumen antes del POST y que mostrarErrores conserva replaceChildren.
La confirmación funcional de esta corrección corresponde al usuario.

### Etapa 53 Revisión de los diez campos y mensajes del registro

Fecha: 4 de noviembre de 2026.

Estado: revisión solicitada por el usuario, sin implementar correcciones.
Se revisaron formulario, JavaScript, DTO, controlador, manejador global,
servicio, Persona, Usuario, repositorios, PasswordEncoder, pruebas existentes
y las decisiones de identificación del documento 25. El DTO contiene 26
restricciones Bean Validation distribuidas entre los diez campos. El
formulario mantiene novalidate: los atributos HTML ayudan, pero no
sustituyen @Valid del endpoint ni las comprobaciones del servicio.

#### Comprobación realizada y límites de la evidencia

- Se ejecutaron 82 comprobaciones diagnósticas de propiedades contra la
  clase RegistroUsuarioDTO compilada actual y Hibernate Validator 6.2.5.Final,
  con JDK 11. Se comprobó que el .class es posterior al fuente vigente.
  Se usaron exclusivamente objetos en memoria, sin contexto Spring/JPA,
  peticiones HTTP, cuentas, correos, SQL, reinicios o modificaciones de datos.
- Se comprobó además BCryptPasswordEncoder 5.7.10 real con dos entradas
  sintéticas distintas. No se imprimieron hashes ni claves reales. El
  resultado específico se registra entre los hallazgos, no como alta válida.
- El programa temporal se creó únicamente en build/tmp, ignorado por Git,
  mediante apply_patch y se eliminó por la misma vía al terminar. Un intento
  previo con JShell sin salida útil fue interrumpido; no se usa como evidencia.
  No se cambiaron fuentes de aplicación ni pruebas. Solo persiste este diario.
- Los XML de las nueve suites conservan la evidencia previa de 67 pruebas
  correctas, incluidas diez de RegistroUsuarioDTOTest y diez de
  RegistroUsuarioServiceTest. No se ejecutó Gradle en esta revisión ni se
  suman los diagnósticos temporales al total de casos del proyecto.
- Los tests de servicio simulan repositorios y encoder: verifican rechazo
  de duplicados y catálogos inexistentes y flujo de guardado, no UNIQUE/FK,
  rollback real o carreras simultáneas en MySQL. No se declaran comprobadas
  esas garantías mediante altas reales en esta revisión.

#### Reglas actuales por campo

| Campo | Validación y normalización actuales |
| --- | --- |
| Nombre | NotBlank, máximo 50, letras Unicode y grupos separados por un espacio, apóstrofe recto o guion; trim de extremos. No admite números, separadores consecutivos ni extremos con separador. Persona repite las restricciones y columna no nula de 50. |
| Apellido | Mismas reglas y capacidad que nombre, en DTO y Persona. |
| Nickname | NotBlank, 3 a 30, primera letra a-z y resto a-z/0-9/punto/guion/guion bajo; trim y minúsculas. El servicio comprueba duplicados sin distinguir mayúsculas, incluidas cuentas inactivas; Usuario declara UNIQUE y longitud 30. |
| Email | NotBlank, máximo 254, Email más expresión con parte local ASCII y dominio con punto/sufijo de al menos dos letras; no empezar por número. Trim y minúsculas; duplicados incluidos inactivos y UNIQUE independiente en Usuario. No prueba existencia o titularidad de la casilla. |
| Fecha de nacimiento | No nula y estrictamente anterior a hoy; Persona repite esa condición. No hay edad mínima/máxima o antigüedad máxima aprobada; hoy/futuro se rechazan y una fecha extremadamente antigua supera Past. |
| Género | ID no nulo y positivo; el servicio exige que exista en GeneroRepository. Persona tiene FK y columna obligatoria. Las opciones vienen del catálogo real. |
| Tipo de documento | ID no nulo y positivo; el servicio exige existencia en TipoDocumentoRepository y Persona lo referencia con FK obligatoria. No implica validar todavía el formato del número según ese tipo. |
| Número de documento | NotBlank y máximo 50; trim y mayúsculas conservando letras y ceros iniciales. Comprobación de duplicado por tipo/país/número; Persona declara UNIQUE compuesto. No hay patrón específico por tipo/país ni limpieza de separadores internos. |
| País emisor | ID no nulo y positivo, existencia en PaisRepository y FK obligatoria. No es nacionalidad. AR es la muestra local actual, no una regla definitiva que excluya otros países. |
| Contraseña | NotBlank, 6 a 64, al menos a-z, A-Z y uno de !@#$&*, y comprobación ASCII de ausencia de espacios mediante S no blanca. No exige dígitos ni se hace trim. El servicio hashea; Usuario valida el hash como obligatorio y hasta 255, no con las reglas de texto en claro. |

#### Resultados y puntos pendientes

1. Documento incompleto: ABC!? y cadenas con espacios internos superan las
   reglas actuales. 12.345.678 y 12345678 conservan representaciones distintas
   tras la normalización. Deben acordarse formato y canonicalización por
   tipo/país para que la unicidad se aplique a la identidad documental deseada.
   No se impone aquí una longitud universal de pasaporte ni DNI extranjero;
   esas reglas no estaban cerradas en el documento 25. No se afirma verificar
   autenticidad de documentos o identidad real mediante estos campos.
2. Contraseña y Unicode: U+00A0 y U+2003 añadidos a una clave que cumple las
   demás condiciones fueron aceptados. La expresión S de Java sin modo
   Unicode no excluye todos los espacios que sugiere el mensaje al usuario.
   La prohibición de espacios ordinarios y tabulaciones sí se corroboró.
3. Límite de BCrypt: una entrada de 39 caracteres/73 bytes UTF-8 supera la
   validación del DTO. En el encoder instalado, una segunda entrada distinta
   solo después del byte 72 coincidió con el hash de la primera. Es necesario
   alinear el límite de entrada en bytes, sin confundirlo con longitud de
   columna o declarar defectuoso todo el uso de BCrypt. Cualquier corrección
   debe mantener coherencia con las demás entradas de contraseña cuando se
   implementen, sin migrar de algoritmo o normalizar claves por iniciativa.
4. Nombre y apellido: se aceptan tildes precompuestas, O'Connor y Jean-Luc;
   se rechazan dos espacios consecutivos, O’Connor con apóstrofe tipográfico
   y una tilde en forma descompuesta. El mensaje general de letras/espacios/
   apóstrofes/guiones no explica esas diferencias. Debe precisarse o revisar
   la normalización de datos personales, nunca aplicarla a contraseñas.
5. Nickname: ana.., ana-- y ana_ se admiten y los tests lo esperan; no es un
   defecto frente a la política aprobada, que no prohibió repeticiones o
   puntuación final. Tampoco se añade una reserva de nombres sin aprobación.
6. Email: se rechazaron maurio@sd, @mail@gmail.com, prefijo numérico,
   punto inicial, puntos consecutivos y dominio mal formado; 254 caracteres
   estructuralmente válidos se aceptaron y 255 se rechazaron. +ana y _ana
   son comienzos aceptados: la regla actual prohíbe un número, no exige
   comenzar por letra. El formato inválido con prefijo letra no dispara
   el aviso de empezar con un número.

#### Coherencia de la lista de errores

- Se ejecutó mostrarErrores del archivo actual en una simulación DOM en
  memoria: conserva mensajes de restricciones para valores no vacíos,
  elimina los prefijos de los diez campos, traduce nickname/clave,
  deduplica y prioriza solo obligatoriedad cuando esos dos campos están
  vacíos. El ejemplo pasó a tres avisos para usuario/contraseña/tipo, sin
  duplicados de tamaño; el conflicto de nickname se muestra como nombre
  de usuario ya en uso. Sigue usando textContent y no HTML del servidor.
- Es coherente listar simultáneamente errores de campos diferentes o de
  requisitos distintos de un campo no vacío. El orden es alfabético por
  mensaje, no el orden visual del formulario; puede mejorarse por claridad.
- La validación @Valid devuelve la lista 400 desde GlobalExceptionHandler;
  los controles de existencia dan mensajes 400 y los duplicados mensajes
  409 de texto, que registro.js admite. El servicio devuelve el primer
  conflicto encontrado, no una lista simultánea de todos los duplicados.
- Detalle técnico a corregir: la ausencia del rol CLIENTE usa también
  IllegalStateException y termina como 409 con mensaje de configuración.
  No es un dato que pueda corregir el visitante; debería tratarse como fallo
  interno con un aviso apropiado. No se modifica su manejo en esta revisión.
- Los mensajes de nombres y la afirmación sin espacios de contraseña
  necesitan alinearse con los casos anteriores. No se considera cerrada
  toda la validación solo porque el envío vacío ya muestra una lista clara.

Referencias primarias para los límites técnicos: documentación Java 11 de
Pattern (clases ASCII/Unicode) y OWASP Password Storage Cheat Sheet, apartado
Input Limits of bcrypt. Se verificó además el fuente de la dependencia
spring-security-crypto 5.7.10 almacenado localmente, sin modificar librerías.

- https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/regex/Pattern.html
- https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#input-limits-of-bcrypt

### Etapa 54 Correcciones de contraseña: instrucciones para registro y recuperación

Fecha: 4 de noviembre de 2026.

Estado: el usuario pidió avanzar con las correcciones de la revisión. Se
priorizan los dos límites técnicos de contraseña en los tres archivos
existentes que reciben o procesan claves nuevas; se dan instrucciones
manuales, sin modificar fuentes de aplicación ni pruebas en este paso.
Documento, nombres y clasificación de errores quedan como trabajo posterior.

- RegistroUsuarioDTO conservará NotBlank, Size de 6 a 64 y la composición
  actual. El Pattern de password incorporará el modo Unicode (?U) para que
  la clase no blanca S rechace también U+00A0 y U+2003. No se exige un dígito,
  no se restringe la clave a ASCII ni se hace trim o normalización.
- RegistroUsuarioService comprobará al inicio de registrarUsuario que el
  password válido recibido no supere 72 bytes UTF-8, antes de consultas o
  guardado. Usará StandardCharsets.UTF_8 e IllegalArgumentException: el
  controlador existente ya devuelve ese mensaje como 400 y registro.js
  lo presenta sin prefijos técnicos o cambios de contrato.
- RecuperacionPasswordService tiene un validador privado propio y la misma
  limitación anterior; se actualizará ese método, sin tocar tokens, fechas,
  consumo, búsqueda, hash o persistencia. Comprobará 6 a 64 mediante
  String.length, composición/ausencia de espacios con modo Unicode y el
  mismo máximo de 72 bytes. La longitud explícita mantiene la semántica de
  Size del DTO, evitando que el límite por puntos de código del antiguo
  cuantificador admita entradas distintas en recuperación.
- No se crea otra entidad, DTO, campo ficticio o tabla para esta restricción.
  Usuario sigue almacenando únicamente el hash hasta 255; BCrypt y el login
  se mantienen. La validación de bytes corresponde a la entrada del encoder,
  no a la longitud del hash ni al número de caracteres HTML.
- La propuesta se comprobó con 16 casos diagnósticos de Java 11: nulo,
  vacío, mínimo, máximo, ASCII, espacios normales/tabulación/salto de línea,
  NBSP/EM, composición, 72/73 bytes UTF-8 y unidades UTF-16 frente a puntos
  de código. Ambos recorridos propuestos coincidieron con lo esperado.
  Fue una simulación de las reglas, no una ejecución del código modificado:
  el programa temporal de build/tmp se eliminó al terminar.
- Se indicará al usuario guardar y ejecutar solo RegistroUsuarioDTOTest y
  RegistroUsuarioServiceTest para compilar los cambios y comprobar regresión
  básica. Los casos nuevos específicos deberán incorporarse después: no se
  afirma que los tests anteriores cubran ya los límites añadidos ni que se
  haya probado un token real o un alta. No se ejecutó Gradle, SQL o correo.
- Los textos de ayuda de registro y recuperación deberán informar también
  el máximo de bytes en un bloque posterior, junto con sus pruebas. No se
  declaran ya actualizados. Antes de una comprobación funcional habrá que
  reiniciar las clases Java; no se reinicia create-drop en este bloque.

### Etapa 55 Contraseña: puntuación y símbolos ampliados e implementación verificada

Fecha: 4 de noviembre de 2026.

Estado: el usuario aprobó ampliar la lista de caracteres especiales y
autorizó al asistente a implementar este bloque puntual en todos sus
consumidores. Al comenzar se corroboró que las instrucciones anteriores
de espacios Unicode y 72 bytes aún no estaban aplicadas; se incorporaron
en este mismo bloque de contraseña. No se modifican reglas de documento,
nombres, edad, roles, estados o DER, ni el tratamiento pendiente de errores
de configuración. Las instrucciones de la etapa 54 quedan integradas aquí.

#### Regla vigente y cambios realizados

- La condición de carácter especial admite puntuación o símbolos Unicode
  mediante las categorías P/S, en vez de la lista !@#$&*. Se aceptan, entre
  otros, ?, %, _, +, €, punto, comillas y barra inversa. Letras con tilde o ñ
  no satisfacen por sí solas ese requisito; siguen siendo letras. La prueba
  incluye también un símbolo emoji reconocido por los motores utilizados.
- Se conservan minúscula a-z, mayúscula A-Z y longitud de 6 a 64 unidades
  String de Java/JavaScript. No se añade obligación de dígitos, trim,
  conversión de mayúsculas o normalización de la contraseña.
- RegistroUsuarioDTO usa modo Unicode (?U) para rechazar espacios Unicode
  y las categorías de puntuación/símbolos en el Pattern del password. No se
  cambian los otros nueve campos ni su serialización WRITE_ONLY.
- RegistroUsuarioService controla al inicio el máximo de 72 bytes UTF-8,
  antes de consultas o persistencia. El exceso lanza IllegalArgumentException,
  que el controlador existente comunica como 400 y el frontend muestra con
  un mensaje legible. No se recorta silenciosamente la clave.
- RecuperacionPasswordService aplica la misma composición, espacios y
  límite en bytes. Usa String.length explícito para conservar la semántica
  de Size del registro; no se modificaron generación/búsqueda/consumo de
  tokens, vencimiento, activación, llamadas al encoder o guardado.
- restablecer-password.html alinea el JavaScript con puntuación/símbolos y
  White_Space Unicode, longitud de String y TextEncoder para los 72 bytes.
  Mantiene comparación exacta de confirmación, endpoint, campos del POST,
  manejo del token, mensajes de éxito y redirección.
- registro.html y restablecer-password.html muestran la nueva ayuda con
  ejemplos de símbolos y el límite adicional de 72 bytes. No se alteraron
  estructura del formulario, estilos, navegación u otros campos. registro.js
  sigue usando el backend como autoridad y no necesitó otro validador local.
- Usuario, SecurityConfig, BCrypt y hashes almacenados se mantienen intactos.
  No se añadió una entidad, tabla, dependencia, utilidad sin consumidor o
  nueva configuración. El login no aplica una migración a claves existentes.

#### Pruebas y comprobación del alcance

- RegistroUsuarioDTOTest suma un caso para los símbolos nuevos y amplía
  las entradas inválidas con espacios Unicode, tabulación/salto y letras
  que no cuentan como símbolos. Se comprueba que la clave no se modifica.
- RegistroUsuarioServiceTest suma rechazo de 73 bytes antes de cualquier
  interacción con repositorios/encoder y aceptación de 72 bytes sin truncar
  la entrada enviada al encoder. La preparación del mock usa el password
  de cada fixture, sin modificar el comportamiento de producción.
- Se creó RecuperacionPasswordServiceTest (tres casos con Mockito): rechazo
  de formato antes de consultar el token, rechazo de 73 bytes y aceptación
  de los símbolos nuevos/hasta 72 bytes conservando la entrada al encoder.
  Se comprueba el mantenimiento del consumo del token en el caso simulado
  válido; no es una prueba de concurrencia o de token contra MySQL.
- Con JDK 11 y dependencias locales se ejecutaron primero las tres suites
  específicas: 26 casos correctos. El acceso inicial a la caché de Gradle
  requirió la vía autorizada por permisos de archivo, no un cambio al proyecto.
- Después se ejecutaron las diez suites aisladas explícitas de catálogos,
  usuarios, seguridad y contraseña con --offline: BUILD SUCCESSFUL en 6 s,
  una tarea ejecutada y tres UP-TO-DATE. Los diez XML registran 73 casos,
  cero fallos, errores u omisiones, el 4 de noviembre de 2026. El total
  anterior era 67; los seis métodos nuevos explican la diferencia.
- Desglose: WebAuthentication 4, WebAuthorization 9, CatalogoController 5,
  UsuarioController 6, RegistroUsuarioDTO 11, UsuarioDTO 7,
  RecuperacionPasswordService 3, RegistroUsuarioService 12,
  UsuarioDetailsService 6 y UsuarioService 10. No se incluyó el
  SpringBootTest no aislado de la aplicación ni se ejecutó bootRun o SQL.
- Se comprobó además el script inline real de recuperación en Node con 24
  entradas, DOM/Axios/ventanas simulados, sin HTTP: aceptación de los nuevos
  símbolos, rechazo de letras sin símbolo, espacios y excesos de longitud,
  y límites 72/73 bytes. El programa temporal de build/tmp se eliminó al
  terminar; no se suman estos diagnósticos a los 73 casos JUnit.
- Las huellas SHA-256 respecto del inicio de esta intervención confirman
  exclusivamente ocho archivos de src modificados/agregados: el DTO, los
  dos servicios, los dos HTML, las dos pruebas de registro y la nueva prueba
  de recuperación. No hubo eliminaciones ni cambios a otros fuentes; se
  preservaron las modificaciones y el staging previos del usuario.
- git diff --check no detectó errores de espacios. No se hicieron escrituras
  Git, altas, correos reales, cambios de base o reinicios. La aplicación que
  ya estaba ejecutándose requiere reiniciar sus clases para la prueba real;
  con create-drop esa operación elimina/recrea los datos locales de prueba.
  No se declara realizada esa verificación funcional del navegador/MySQL.

### Etapa 56 Reinicio manual y siguiente definición de validación documental

Fecha: 4 de noviembre de 2026.

Estado: el usuario pidió instrucciones para reiniciar y continuar las
correcciones. Se releyeron el perfil refactor y las reglas de contraseña
implementadas. Se mantiene la evidencia de 73 pruebas aisladas y 24 casos
JavaScript simulados, sin una nueva ejecución de pruebas o arranque del
asistente en este paso. Solo se actualiza este diario.

- Para reiniciar bootRun, el usuario interrumpirá con Ctrl+C en la terminal
  que lo ejecuta y confirmará el cierre del proceso si el sistema lo solicita.
  MySQL debe seguir iniciado. La advertencia de create-drop permanece:
  detener/reiniciar elimina/recrea las tablas y datos locales de prueba.
- El nuevo bootRun usará explícitamente refactor, la URL JDBC local de
  turismo_sustentable y el puerto HTTP 8080, como en el arranque anterior.
  Después de Started TurismoSustentableApplication se pedirá recargar el
  navegador con Ctrl+F5 para los recursos frontend. No se debe iniciar una
  segunda instancia sin detener la anterior ni cambiar propiedades para
  solucionar un conflicto de puerto provocado por ella.
- El siguiente punto de contenido es número de documento y su representación
  única por tipo/país. No se inventa un patrón internacional o longitud
  universal. Se consulta si el alcance de DNI incluye documentos de otros
  países además de Argentina antes de imponerle reglas argentinas a todo
  el catálogo. La respuesta queda pendiente; no se restringen países o
  documentos admitidos en este paso.
- También siguen pendientes normalización/mensajes de nombre y apellido y
  clasificación de la ausencia de rol CLIENTE como fallo interno. La política
  de edad mínima/máxima no fue aprobada y no se incorpora silenciosamente.
  No se modifica el DER ni se consideran resueltos esos puntos por compilar
  o por la corrección previa de contraseña.

### Etapa 57 Documento normalizado, nombres y fallos internos del registro

Fecha: 4 de noviembre de 2026.

Estado: implementado y comprobado mediante pruebas aisladas. El usuario
autorizó expresamente los tres puntos de este bloque y confirmó DNI únicamente
para Argentina, pasaporte con letras/números y conversión visible a mayúsculas.
También aprobó ampliar la precarga de países. Esto concreta el diseño físico
de identificación del documento 25; no modifica entidades, claves ni relaciones
del DER v5 ni declara terminada la refactorización del resto del sistema.

#### Reglas incorporadas

- El número de documento sigue siendo texto, obligatorio y con capacidad
  máxima de 50 caracteres; conserva los ceros iniciales. No se lo convierte
  a entero ni se inventa una longitud oficial de DNI o pasaporte.
- DNI: únicamente dígitos ASCII y país emisor AR. Los puntos y espacios de
  presentación se eliminan antes de comprobar duplicados y persistir. Una
  letra o guion no se elimina para fabricar otro número: se rechaza.
- PASAPORTE: letras A-Z y números, mayúsculas y sin espacios. No se exige
  contener letras: también se admiten numeraciones exclusivamente numéricas.
  No se admiten puntos, guiones ni otros símbolos como parte del número.
  La restricción es DNI implica AR, no AR implica DNI: se conserva la
  posibilidad de un pasaporte argentino además de pasaportes extranjeros.
- La unicidad continúa siendo el conjunto tipo_documento_id, pais_emisor_id
  y numero_documento. RegistroUsuarioService consulta el número canónico y
  crea Persona con ese mismo valor; por ejemplo 00.123.456 y 00123456 se
  tratan como la misma identificación dentro del mismo tipo/país.
  Se conserva la restricción única existente como protección de persistencia.
  Esto no verifica autenticidad documental ni identifica a una persona que
  declare dos documentos distintos. No se implementó una migración de datos
  previos: si existieran números no normalizados, necesitarán revisión y
  normalización/deduplicación controlada antes de usar una base no descartable.
- Nombre y apellido: recorte de espacios externos, unificación de espacios
  Unicode repetidos y composición Unicode NFC, conservando tildes y forma
  de escribir las mayúsculas. Apóstrofes tipográficos y variantes de guion
  se convierten a sus separadores canónicos. Se permiten letras, marcas de
  acentuación y separaciones simples por espacio, apóstrofe o guion; no
  números, símbolos ajenos, separadores dobles o al principio/final.
  Se mantienen obligatoriedad y máximo 50. El mensaje describe esta regla.
  Se aplica en RegistroUsuarioDTO, ActualizarUsuarioDTO y Persona, no solo
  en la interfaz. No se habilita una nueva edición de documentos.

#### Implementación acotada

- Se crearon DocumentoIdentidad y NormalizacionNombre en validation,
  reutilizados inmediatamente en los DTO, servicio y entidad. No son
  componentes temporales sin consumidores. Los setters de Persona mantienen
  normalización del documento incluso al asignar su tipo después del número.
- registro.html conserva su formulario novalidate y su estructura visual.
  Se añadió ayuda asociada al campo documental, autocapitalize, spellcheck
  desactivado y bloqueo inicial hasta seleccionar tipo.
- registro.js usa los códigos de las opciones recibidas del backend y sus
  IDs reales, no IDs fijos. Para DNI fija Argentina y deshabilita el selector
  de país; para pasaporte lo habilita. El teclado sugerido de DNI es numérico.
  El evento input convierte realmente f a F en pasaporte, conserva el cursor
  y los ceros; restaura el último valor válido ante entradas incompatibles
  en vez de convertirlas silenciosamente en otra identificación.
  Cambiar de tipo limpia el número y país anteriores para evitar reutilizar
  por accidente el documento de otro tipo. Nombres/apellidos se normalizan
  visiblemente al salir del campo y al construir el payload.
- Se conserva el resumen de errores mientras se espera el POST: no se
  reintrodujo el ocultamiento que provocaba el salto de scroll. Continúan
  mensajes legibles, deduplicación de obligatorios, textContent, control
  de doble envío, tratamiento de HTTP y redirección al ingreso.
- CargaCatalogosRefactor sigue limitada al perfil refactor y conserva roles,
  géneros y tipos previos. Amplía PAIS con los códigos ISO alpha-2 del JDK
  y sus nombres en español, consultando codigo antes de insertar. Repetir
  la carga no duplica ni sobrescribe filas existentes. No se fijan IDs ni
  se agregan nacionalidad, países como entidades separadas o dependencias.
  El conjunto proviene del JDK instalado, no de una consulta online actual:
  [API Locale de Java 11](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Locale.html).
  La precarga real se ejecutará cuando el usuario arranque refactor; las
  pruebas de este bloque no la ejecutaron contra MySQL.
- Se creó ConfiguracionSistemaException, independiente de IllegalStateException,
  para no confundir configuración con conflictos de negocio. La falta del rol
  CLIENTE y tipos documentales de catálogo no soportados usan esta excepción.
  GlobalExceptionHandler responde 500 con un mensaje genérico y registra el
  detalle internamente, sin exponerlo al visitante. Un ID de catálogo inexistente
  o formato incorrecto sigue siendo 400; un duplicado, 409. El frontend ya
  muestra un aviso genérico ante 500 y bloquea el envío si no puede cargar o
  preparar las opciones requeridas. No se añadió un catch-all que oculte
  errores de seguridad ni se cambiaron todos los errores del proyecto.

#### Evidencia y límites

- Con JDK 11, --offline y catorce filtros explícitos se obtuvo BUILD SUCCESSFUL
  en 7 s: dos tareas ejecutadas y dos UP-TO-DATE. Los XML registran 93 casos,
  cero fallos, errores u omisiones. Las suites son CargaCatalogosRefactor 2,
  WebAuthentication 4, WebAuthorization 9, CatalogoController 5,
  UsuarioController 6, RegistroUsuarioDTO 11, UsuarioDTO 7,
  GlobalExceptionHandler 4, RecuperacionPasswordService 3,
  RegistroUsuarioService 16, UsuarioDetailsService 6, UsuarioService 10,
  DocumentoIdentidad 6 y NormalizacionNombre 4.
- Son 20 métodos nuevos respecto de los 73 anteriores: cuatro en el test
  del servicio de registro y cuatro nuevas clases de prueba (16 métodos).
  Comprueban normalización compartida, longitudes, ceros, tipo/país, rechazo
  de entradas incorrectas, búsqueda canónica de duplicados, guardado de
  pasaporte extranjero, idempotencia de países y HTTP 400/409/500. El
  test del manejador usa MockMvc standalone con repositorios/servicios
  simulados; el del cargador usa repositorios Mockito en memoria.
- Un primer intento de compilar las pruebas nuevas detectó un import
  estático ambiguo de startsWith entre Mockito y Hamcrest. Se corrigieron
  únicamente los imports de GlobalExceptionHandlerTest; la ejecución
  completa mencionada arriba pasó después de esa corrección.
- Además se ejecutaron 25 comprobaciones Node sobre el registro.js real,
  con DOM/Axios simulados y sin HTTP: modos DNI/pasaporte, conversión visible,
  cursor, espacios, ceros, límites, limpieza al cambiar tipo, nombres,
  payload con IDs no fijos, errores legibles, conservación del resumen,
  doble envío, fallo interno sin filtración, redirección, falta de AR y
  errores de catálogos/red. No equivalen a una prueba visual del navegador.
  El diagnóstico temporal de build/tmp se eliminó al terminar; no se suma
  al total JUnit ni agrega una herramienta/dependencia al proyecto.
- Las huellas SHA-256 comparadas con el inicio de esta intervención
  registran 16 archivos de src modificados/agregados (nueve existentes,
  tres fuentes de producción nuevos y cuatro tests nuevos), sin fuentes
  eliminados ni cambios en otros archivos de src. Se preservaron cambios
  y staging previos. git diff --check no reportó errores de espacios.
- No se ejecutó el SpringBootTest no aislado, bootRun, SQL, un registro real,
  envío de correos, reinicio o escritura Git. No se alteraron credenciales,
  contraseñas/hashes, recuperación, login, alquileres, columnas, claves o FK.
  Falta la comprobación funcional real del usuario tras reiniciar las clases
  y recargar recursos. Con el perfil actual create-drop, detener/reiniciar
  elimina/recrea las tablas y datos locales de prueba; no hacerlo sobre
  información que se quiera conservar sin respaldarla y revisar ese perfil.

### Etapa 58 Máximo propio del DNI y mensajes de contraseña comprensibles

Fecha: 5 de noviembre de 2026.

Estado: implementado y comprobado con pruebas aisladas. El usuario simplificó
las ayudas de documentos y contraseña y confirmó un máximo de ocho dígitos
para el DNI argentino y el mensaje de contraseña demasiado larga. Esta regla
sustituye el máximo genérico de 50 aplicado al DNI en la etapa 57, sin reducir
la capacidad de la columna compartida con pasaportes ni modificar el DER.

- DocumentoIdentidad conserva LONGITUD_MAXIMA=50 para almacenamiento y
  pasaportes y agrega LONGITUD_MAXIMA_DNI=8. El DNI se controla después
  de normalizar puntos/espacios, manteniendo sus ceros y la exigencia de
  país AR y formato numérico. Exceder ocho dígitos se rechaza, no se recorta.
  La aprobación fija un máximo, no una nueva longitud mínima de siete u ocho;
  en este paso se conserva la obligatoriedad previa sin inventar un mínimo.
- registro.js aplica el límite ocho a DNI y mantiene 50 para pasaporte.
  Un noveno dígito o pegado que exceda el máximo restaura el último valor
  válido; no transforma un documento largo en otro distinto truncándolo.
  Se conserva maxlength=50 del input compartido para permitir pegar un DNI
  con puntos/espacios y normalizarlo antes de contar sus ocho dígitos;
  el límite efectivo por tipo lo aplica el evento input, además del backend.
  Los dos textos de ayuda que editó el usuario se mantienen exactamente.
- RegistroUsuarioService y RecuperacionPasswordService mantienen su límite
  interno de 72 bytes y no alteran ni truncan contraseñas. Solo se cambia
  el mensaje de exceso a: «La contraseña es demasiado larga. Probá con una
  más corta.». El JavaScript de restablecer-password.html muestra el mismo
  aviso antes de enviar y conserva su validación interna. También se quita
  de la ayuda de esa pantalla la explicación técnica de bytes/UTF-8 que el
  usuario ya retiró del registro. No se cambia el algoritmo de hash, tokens,
  composición, límites 6/64, recuperación, confirmación o redirección.
- Se actualizan los tests que comparaban el mensaje técnico. El antiguo
  caso de capacidad 50 ahora corresponde únicamente a PASAPORTE; se agrega
  un caso de siete/ocho dígitos con formato y ceros y rechazo de nueve o
  más. RegistroUsuarioServiceTest suma rechazo de nueve antes de persistir,
  consultar duplicados de Persona o asignar roles. No se altera el valor
  inválido recibido para convertirlo en una identificación admitida.
- Con JDK 11 y --offline se ejecutaron las mismas catorce suites aisladas
  explícitas de la etapa anterior: BUILD SUCCESSFUL en 15 s, cuatro tareas
  ejecutadas. Los XML registran 95 casos, cero fallos, errores u omisiones;
  DocumentoIdentidad pasa de 6 a 7 y RegistroUsuarioService de 16 a 17.
  No se ejecutó el SpringBootTest que usa una base real.
- Se realizaron diez comprobaciones adicionales Node sobre los scripts
  reales de registro y recuperación, con DOM/Axios simulados: máximo DNI,
  ceros, normalización antes del límite, rechazo sin truncar, pasaporte sin
  regresión, mensaje comprensible de exceso, protección interna y flujo
  válido de recuperación. No hubo HTTP o MySQL. El programa temporal de
  build/tmp fue eliminado al terminar y no se suma al total JUnit.
- La comparación SHA-256 con el inicio de esta intervención confirma solo
  ocho archivos existentes de src modificados: el validador, registro.js,
  dos servicios, el HTML de recuperación y tres tests. No se tocaron otros
  fuentes ni se eliminaron archivos de src; el diario es el único documento
  actualizado. Se preservaron todos los cambios y el staging previos.
  No se hizo commit, push, SQL, envío real, arranque o reinicio. Sigue
  pendiente la comprobación del usuario en la aplicación reiniciada; el
  perfil refactor mantiene create-drop y sus datos de prueba descartables.

### Etapa 59 Revisión de cierre de validaciones de registro y recuperación

Fecha: 5 de noviembre de 2026.

Estado: revisión solicitada por el usuario, sin modificar fuentes de producción
o tests ni aprobar reglas nuevas. Se releyeron RegistroUsuarioDTO, los dos
servicios, controladores de usuarios/recuperación/catálogos, Persona, Usuario,
el manejador de errores, la seguridad y los tres formularios con su JavaScript.

- Registro conserva diez campos: nombre y apellido obligatorios/máximo 50
  con normalización común; nickname obligatorio/único, 3-30, inicio a-z y
  resto a-z, números, punto, guion/guion bajo, normalizado a minúsculas;
  email obligatorio/único, máximo 254, formato con dominio y sufijo, sin
  número inicial, normalizado; nacimiento obligatorio y anterior a hoy;
  género, tipo y país obligatorios con ID positivo y existente; documento
  normalizado por tipo/país y unicidad compuesta; password obligatoria,
  6-64, minúscula a-z, mayúscula A-Z, puntuación/símbolo, sin espacios y
  límite interno de bytes previo al hash. No se exige un número en la clave.
- El payload de registro coincide con los nombres/tipos del DTO; catálogos
  y POST de registro son públicos. @Valid produce validationErrors y el
  frontend los presenta en el resumen, quita nombres internos, usa texto
  seguro y deduplica los mensajes de obligatorios de nickname/password.
  Reglas del servicio y duplicados se presentan en ese mismo resumen,
  aunque se informa el primer conflicto de negocio encontrado, no todos
  simultáneamente. Los 500 se muestran con mensaje genérico. Los caracteres
  documentales bloqueados al escribir no generan una respuesta del backend.
- Recuperación usa correctamente POST /api/password/solicitar con email y
  POST /api/password/restablecer con token/password. El email se recorta y
  normaliza en el servicio; una dirección inexistente o cuenta inactiva
  recibe el mismo mensaje que una existente, sin revelar su registro.
  El token se genera aleatoriamente, se almacena hasheado, dura 30 minutos,
  exige cuenta activa y se elimina al cambiar la contraseña. La nueva clave
  usa la misma composición/límites que el registro; confirmarla se controla
  solo en la interfaz y no se persiste ni se envía como segundo password.
  En estas dos pantallas los avisos son SweetAlert, no una lista como en
  registro. El backend devuelve texto 400 ante clave/token inválidos.
- Observación pendiente de decisión: DNI tiene máximo ocho, pero no mínimo;
  el patrón [0-9]+ admite por ejemplo 1. Esto coincide con el máximo aprobado,
  no garantiza una longitud documental real. No se incorpora automáticamente
  el mínimo siete propuesto antes ni se cambian ceros/longitudes de pasaportes.
- Observación pendiente de corrección: solicitarRecuperacion no valida en
  backend formato ni máximo 254 del email. Un valor ausente/vacío retorna
  sin procesar y el controlador responde 200 genérico; uno mal formado se
  consulta sin un rechazo específico. El regex del navegador y maxlength
  no sustituyen esa validación al llamar directamente al endpoint. Validar
  formato no debe revelar si un email válido está o no registrado.
- Se reejecutaron las catorce suites aisladas explícitas con JDK 11,
  --offline y --rerun-tasks: BUILD SUCCESSFUL en 8 s, cuatro tareas
  ejecutadas. Los XML confirman 95 casos sin fallos, errores ni omisiones.
  Las pruebas de recuperación existentes cubren política de clave y consumo
  de token en un caso válido simulado, no todo el circuito de solicitud,
  vencimiento y reuso con MySQL/correo. No se declara realizada una prueba
  end-to-end ni visual nueva; tampoco se ejecutaron SQL, correo real,
  arranque/reinicio, SpringBootTest no aislado o escrituras Git.
- Para el cierre funcional falta resolver explícitamente las dos observaciones
  y comprobar en la aplicación reiniciada un registro válido, duplicados y
  errores visibles, más recuperación, expiración/reuso y login con la nueva
  contraseña. No se considera todo terminado solamente por pasar tests
  aislados; la advertencia de create-drop y datos descartables sigue vigente.

### Etapa 60 Solicitud de recuperación validada en el backend

Fecha: 5 de noviembre de 2026.

Estado: implementado a pedido del usuario para completar este bloque de
validaciones. No se impone un mínimo documental de siete por una suposición:
el DNI conserva obligatoriedad, formato numérico, país AR y máximo ocho.
La intervención actual se limita al email de solicitud de recuperación y
su presentación de errores, sin cambiar el DER o la política de contraseñas.

- Se creó SolicitarRecuperacionDTO, usado inmediatamente por el controlador:
  email obligatorio, máximo 254 y @Email con el formato de dominio/sufijo
  usado en el registro. El setter recorta espacios externos y convierte a
  minúsculas con Locale.ROOT antes de validar. No se introduce una restricción
  nueva sobre el primer carácter de un email al recuperar una cuenta.
- POST /api/password/solicitar recibe ahora @Valid @RequestBody del DTO en
  lugar de un Map opcional. Los campos inválidos se rechazan con 400 y el
  validationErrors del manejador existente antes de llamar al servicio.
  Un cuerpo ausente, nulo o JSON mal formado también se rechaza con 400;
  no se afirma que todos esos errores tengan el mismo formato de respuesta.
- Para un email de formato válido se conserva exactamente: «Si el correo
  está registrado, recibirás las instrucciones.». El estado HTTP y ese texto
  no distinguen cuenta existente, inexistente o inactiva. El envío y creación
  del token siguen correspondiendo solo a una cuenta activa existente.
- recuperar-password.html mantiene el mismo endpoint y payload {email}.
  Se agregan avisos locales específicos de obligatorio y exceso de longitud.
  El catch lee los 400 de validación del backend, quita el prefijo email:,
  filtra/deduplica mensajes y los presenta mediante SweetAlert con text,
  sin interpretar HTML. También admite un 400 de texto; ante una respuesta
  inesperada, falta de conexión o 500 mantiene un aviso genérico, sin
  mostrar objetos/detalles internos. Se conserva el email ante errores y
  el reset del formulario tras la respuesta exitosa genérica.
- RecuperacionPasswordService no fue modificado en producción. El método
  restablecer del controlador, su Map, reglas de contraseña, confirmación,
  consumo/vencimiento de tokens, hashes, envío de correo y datos de la base
  quedan intactos. No se agrega una tabla, dependencia o utilidad huérfana.

#### Verificación del bloque

- Se añadieron SolicitarRecuperacionDTOTest (cuatro métodos) y
  RecuperacionPasswordControllerTest (siete métodos con MockMvc standalone):
  correo ausente/vacío/mal formado, normalización, aceptación exacta de 254,
  rechazo de 255, rechazo de cuerpos inválidos antes del servicio y
  conservación del contrato de restablecimiento. No hay SpringBootTest o DB.
- RecuperacionPasswordServiceTest conserva sus tres pruebas de contraseña
  y suma dos de solicitud: una cuenta activa recibe el enlace simulado con
  token seguro, hash almacenado y vencimiento de 30 minutos; cuentas
  inexistentes/inactivas no envían ni guardan. El correo y repositorio son
  mocks: no se envía ningún mensaje real ni se escriben filas.
- Con JDK 11 y --offline se ejecutaron dieciséis suites aisladas explícitas:
  BUILD SUCCESSFUL en 7 s, tres tareas ejecutadas y una UP-TO-DATE. Los XML
  registran 108 casos sin fallos, errores u omisiones. Son los 95 anteriores
  más once métodos en las dos clases nuevas y dos en la prueba del servicio.
- Se ejecutaron doce comprobaciones Node sobre el script inline real de
  recuperar-password.html con DOM/Axios simulados: obligatorios, formato,
  longitud, endpoint/payload, respuesta genérica, 400 estructurados/de texto,
  limpieza de prefijos, deduplicación, mensaje como texto seguro, errores
  internos/red y conservación del valor ingresado ante un fallo. El programa
  temporal de build/tmp se eliminó al terminar, sin nuevas dependencias.
- La comparación SHA-256 con el comienzo de esta intervención confirma
  seis archivos de src modificados/agregados: DTO, controlador y HTML,
  dos tests nuevos y la ampliación del test del servicio. No se alteraron
  otros fuentes ni se borraron archivos de src. Este diario es el único
  documento actualizado; se preservaron modificaciones y staging previos.
- No se hizo SQL, alta de cuentas, envío real, cambio de base, arranque,
  reinicio, commit o push. La implementación y pruebas aisladas de este
  ajuste están completas. Para comprobar el cierre funcional en el entorno
  del usuario falta reiniciar sus clases, recargar el navegador y probar
  errores visibles y un circuito real de recuperación. No se confunden
  mocks con evidencia de entrega de un email. Antes de reiniciar recordar
  que refactor sigue con create-drop y datos locales descartables.

### Etapa 61 Pruebas reales locales y evidencia de pantallas

Fecha: 5 de noviembre de 2026.

Estado: el usuario pidió que el asistente hiciera las pruebas porque no tenía
usuarios registrados. Se completaron pruebas contra la aplicación, navegador
y MySQL reales, además de las suites aisladas ya registradas. No se cambió
código de producción o tests en esta intervención.

- Se detectó que localhost:8080 mantenía una instancia vieja: email=hola en
  recuperación todavía respondía 200. Se verificó proceso/clase/refactor/URL
  local y cero filas en usuario, persona, bicicleta, estacion, alquiler y
  tarjeta antes de detenerlo. Se inició el código actual en el mismo puerto
  con JDK 11 y perfil refactor. create-drop recreó tablas/catálogos; no se
  cambió su configuración ni se tocó otra base o aplicación.
- Se ejecutaron 52 comprobaciones HTTP/MySQL correctas: validaciones de
  registro, catálogos, formatos documentales, claves, recuperación, duplicados,
  altas completas, rol CLIENTE, login, logout, tokens consumidos/vencidos y
  contraseña anterior/nueva. Los rechazos no dejaron personas huérfanas.
  Se crearon dos cuentas ficticias: qa.registro y qa.pasaporte. Quedan dos
  personas, dos usuarios y dos asignaciones; ambas cuentas activas y sin
  token/vencimiento pendiente al terminar.
- Mailjet no tiene credenciales/remitente en el entorno local de arranque;
  el log confirmó que no se enviaron los correos. Se comprobó la generación
  del hash/vencimiento al solicitar recuperación, pero para ejercitar el
  restablecimiento se prepararon tokens conocidos y sus hashes exclusivamente
  en la cuenta QA, con control de su versión. Es una fixture de prueba,
  no recepción de un enlace real ni descifrado del hash. Se probaron por
  HTTP el cambio, rechazo del reuso, vencimiento y login posterior.
- En navegador real se verificaron mensajes del registro vacío, nombre
  inválido y email duplicado; límite de DNI y mayúsculas de pasaporte;
  mensajes locales y del backend en recuperación; respuesta genérica;
  y acceso con la contraseña nueva, con saludo «José María» en alquiler.html.
  Se guardaron capturas de la validación devuelta por el backend y de la
  respuesta genérica. No se manipuló el estado interno de la página para
  simular los resultados ni se cambiaron contraseñas mediante la UI.
- El [informe de resultados](../evidencias/2026-11-05-validaciones/resultados.md)
  conserva alcance, grupos de los 52 casos, preparación de fixtures, límites
  y enlaces a las capturas. El diagnóstico temporal de build/tmp se eliminó;
  no quedan programas auxiliares ni usuarios precargados en el código.
- Se dejó la instancia local actualizada ejecutándose, con los dos usuarios
  de prueba en la base descartable y el navegador conectado como qa.registro.
  No se borraron esas cuentas, no se envió correo externo y no se hicieron
  commits, push, despliegues o cambios de DER. Las 108 pruebas aisladas previas
  se mantienen como evidencia separada; las 52 comprobaciones reales no se
  suman a ese total JUnit. La entrega por Mailjet sigue pendiente de una
  configuración y casilla reales; no bloquea la constatación local de las
  validaciones, pero no debe darse por verificada.

## Bloque 5 Adaptación de estaciones

### Etapa 1 Versionado optimista de la entidad

Fecha: 5 de noviembre de 2026.

Estado: implementado manualmente por el usuario y revisado en el archivo.
El usuario informó compileJava correcto en 9 s, una tarea ejecutada. En esta
revisión no se repitió la compilación ni se reinició la aplicación.

- El bloque anterior quedó guardado en el commit 72943b5, «Completar registro,
  recuperacion y pruebas de usuarios», en codex/refactorizacion-practicas-iii.
  Al revisar, el único fuente modificado desde ese commit era Estacion.java.
- Se verificaron el import JsonProperty, el campo Integer version con
  @Version de javax.persistence, @Column(name="version", nullable=false),
  acceso JSON READ_ONLY y getter público sin setter. Los cambios coinciden
  con el paso propuesto; no se alteraron otros campos o relaciones.
- El atributo ya figura en ESTACION del DER v5. Hibernate administra su
  versión al persistir/actualizar; no reemplaza los bloqueos transaccionales
  que coordinan capacidad, ingresos o baja lógica.
- Esto no detecta por sí solo un formulario antiguo: la edición todavía
  recibe la entidad y no compara una versión esperada del cliente. Falta
  exponer la versión en EstacionDTO y adaptar luego el contrato/servicio de
  actualización. No se declara ese recorrido implementado o probado.
- El siguiente paso manual propuesto es agregar version al DTO de salida,
  copiarla en su constructor y exponer su getter, sin modificar conteos de
  disponibilidad/ocupación ni el frontend. Aún no se implementó ese paso
  durante esta revisión. Tampoco se verificó la nueva columna contra MySQL;
  se conservaron las cuentas QA, la instancia activa y la configuración.

### Etapa 2 Versión incluida en la respuesta de estaciones

Fecha: 5 de noviembre de 2026.

Estado: el usuario informó haber completado el paso y se verificó el archivo.
En esta revisión no se ejecutaron compilación, pruebas o reinicio; no se
presenta una nueva salida de compileJava que no fue aportada en este turno.

- EstacionDTO incorpora Integer version, la asignación desde
  estacion.getVersion() en el constructor y el getter público getVersion().
  El diff confirma únicamente esas tres incorporaciones en el DTO; los
  datos anteriores y cálculos de bicicletas/espacios permanecen iguales.
- La respuesta queda preparada para incluir la versión de la entidad una
  vez cargada por Hibernate. El cambio no agrega ni modifica una columna
  por sí mismo; tampoco comprueba que el formulario envíe una versión o
  que el servicio rechace una edición obsoleta. Ese control sigue pendiente.
- El siguiente paso manual propuesto es ActualizarEstacionDTO, con los
  cinco datos editables de la estación y una versión esperada obligatoria
  y no negativa. Se mantendrán los tipos actuales de las coordenadas y
  el tamaño implícito existente de 255 para nombre/dirección; sus rangos
  se alinearán con los aprobados en el documento 26. La entidad sigue
  siendo la propietaria de @Version, no el DTO de entrada.
- Ese archivo no se creó durante esta revisión: la conexión de ese DTO
  con el servicio, controlador y formulario se hará en los siguientes
  pasos. Se conserva la modalidad manual solicitada y no se alteraron
  fuentes, bases, sesiones, Git ni los usuarios de prueba.

### Etapa 3 Contrato de edición de estaciones

Fecha: 5 de noviembre de 2026.

Estado: ActualizarEstacionDTO fue creado manualmente por el usuario y se
revisó su contenido. No se ejecutaron nuevas pruebas, compilación o arranque
durante esta revisión; la validación HTTP del nuevo DTO aún no está conectada.

- Se verificaron el paquete dto, constructor vacío, seis atributos y sus
  getters/setters: nombre, direccion, capacidad, latitud, longitud y version.
  Nombre/dirección se normalizan unificando espacios y recortando extremos;
  son obligatorios y tienen máximo 255, como la capacidad SQL implícita actual.
  Capacidad es Integer obligatoria/mínimo uno. Coordenadas son Double
  obligatorios con rangos inclusivos [-90,90] y [-180,180]. La versión esperada
  es Integer obligatoria/no negativa, recibida con setter y sin @Version.
- El código coincide con la propuesta. No incorpora identificador, flag
  de baja o colecciones como datos editables y no modifica la entidad,
  contratos HTTP o frontend actuales por el solo hecho de existir.
- Se releyó el recorrido de EstacionController y los repositorios para
  preparar el servicio. Faltan una consulta de nombre ocupado por otra
  estación, incluyendo filas de baja, y el conteo de todas las bicicletas
  físicamente asociadas sin excluir borradoBici. Este último criterio ya
  figura en el documento 26: una baja lógica no equivale a una salida física.
- El siguiente paso manual propone existsByNombreIgnoreCaseAndIdNot en
  EstacionRepository y countByUbicacionActual en BicicletaRepository. No se
  agregaron en esta revisión. Se conservarán los métodos anteriores porque
  todavía tienen consumidores; el nuevo servicio usará estas dos consultas.
  No se declara implementada la ocupación física v5 en los otros flujos.
- Solo se actualizó este diario. Se preservaron el staging y los cambios
  del usuario; no se hizo SQL, reinicio, commit, push o modificación de fuentes.

### Etapa 4 Consultas necesarias para editar estaciones

Fecha: 5 de noviembre de 2026.

Estado: el usuario informó haber agregado, guardado y compilado las consultas;
se revisaron ambos archivos y sus diferencias. En esta revisión no se repitió
la compilación, ejecutó contexto Spring/SQL o reinició la instancia local.

- EstacionRepository incorpora existsByNombreIgnoreCaseAndIdNot(String,
  Integer), sin filtro de baja lógica. Excluye el ID editado y contempla
  también nombres de estaciones de baja. BicicletaRepository incorpora
  countByUbicacionActual(Estacion), sin excluir unidades por borradoBici.
  Ambos nombres corresponden a propiedades existentes de las entidades.
- Se conservaron los métodos y bloqueos anteriores con sus consumidores.
  No se declara verificada la derivación de consultas contra MySQL solo
  porque compileJava haya sido informado como exitoso.
- Se revisó el siguiente incremento: EstacionService transaccional y solo
  ADMIN, usando el bloqueo existente de estación, comprobación de versión
  esperada, nombre ocupado por otra fila y capacidad frente a ocupación
  física total. Todas las comprobaciones precederán a los setters y el
  flush se hará antes de construir el DTO de salida.
- El mismo incremento deberá corregir únicamente el conteo físico de
  espacios en EstacionDTO para incluir toda unidad aún asociada; el filtro
  de disponibilidad se conserva. Esto evita que el servicio controle una
  ocupación y el DTO informe otra al excluir bajas que no salieron.
- Son instrucciones para el siguiente paso manual, no código ya añadido.
  No se creó el servicio ni se cambió el conteo en esta intervención. Su
  conexión al controlador/formulario, pruebas y auditoría administrativa
  siguen pendientes. Solo se actualizó este diario, conservando fuentes,
  staging, sesión, base local y cuentas de prueba.

### Etapa 5 Servicio de edición y conteo físico coherente

Fecha: 5 de noviembre de 2026.

Estado: el usuario autorizó al asistente a implementar únicamente los puntos
1 y 2 de la propuesta, reservándose el punto 3 de guardar y comprobar.
Se aplicó el código propuesto y se revisó estáticamente, sin compilar,
ejecutar tests, arrancar/reiniciar la aplicación o realizar SQL.

- Se creó EstacionService con inyección por constructor de EstacionRepository
  y BicicletaRepository, @Service, @Validated y actualizarEstacion transaccional
  restringido a ADMIN. El ID es obligatorio/positivo y los datos obligatorios
  se validan mediante @Valid ActualizarEstacionDTO al invocar el bean.
- Se utiliza buscarActivaPorIdConBloqueo; una estación inexistente o de baja
  produce EntityNotFoundException. Se compara la versión esperada con la de
  la entidad mediante Objects.equals y se lanza OptimisticLockException si
  no coincide. No se permite asignar manualmente la versión de Hibernate.
- Se comprueba nombre ocupado por otra fila y capacidad frente a todas las
  bicicletas físicamente asociadas. Los conflictos producen IllegalStateException
  antes de cambiar datos. Solo después se asignan nombre, dirección, capacidad
  y coordenadas, se hace flush y se prepara el DTO de respuesta con la versión
  de la entidad. No se modifican baja lógica, ID o relaciones desde el DTO.
- EstacionDTO calcula ocupación con bicicletasEnEstacion.size(), incluyendo
  unidades dadas de baja que continúan físicamente allí. Se conserva intacto
  el filtro de bicicletasDisponibles, la lectura de versión y los demás campos.
  La misma ocupación física se utiliza en el servicio y en los espacios libres.
- La comparación SHA-256 con el inicio de esta intervención confirmó solo
  dos archivos de src modificados/agregados: EstacionService nuevo y el
  conteo de EstacionDTO. No se eliminaron fuentes ni se tocaron otros archivos
  de src. Se hizo revisión estática y git diff --check, no una comprobación
  de compilación, persistencia, seguridad o concurrencia en ejecución.
- Este diario es el único documento actualizado. No se tocaron controlador,
  frontend, repositorios, entidad, DTO de entrada, configuración, staging,
  commits, base local o cuentas QA. El usuario conserva la comprobación con
  compileJava. La conexión al controlador, respuestas HTTP, envío de versión
  desde el formulario, pruebas y auditoría administrativa siguen pendientes;
  no se presenta la edición de estaciones v5 como un circuito ya terminado.

### Etapa 6 Revisión de compilación y conexión propuesta al controlador

Fecha: 6 de noviembre de 2026.

Estado: el usuario aportó compileJava con BUILD SUCCESSFUL en 8 s, una
tarea ejecutada. Se releyeron EstacionService y EstacionDTO y se confirmó
que conservan el código implementado en la etapa 5. No se ejecutaron nuevas
pruebas o compilación en esta revisión ni se modificaron fuentes.

- El servicio sigue usando bloqueo de estación, versión esperada, nombre
  único, ocupación física total y flush antes del DTO. El conteo físico del
  DTO incluye todas las asociadas; su filtro de disponibilidad se conserva.
- EstacionController aún contiene la actualización anterior: recibe una
  entidad, cuenta con borradoBici=false y no llama al nuevo servicio. Se
  propone cambiar exclusivamente ese PUT para recibir ActualizarEstacionDTO
  y delegar en EstacionService, agregando su inyección y los imports necesarios.
- La propuesta elimina @Transactional únicamente del PUT: la transacción
  debe completarse en el servicio antes de que el controlador convierta
  excepciones en HTTP. Se conserva @Transactional del DELETE y su import.
  No se cambia todavía GET, POST, DELETE o la política de baja lógica.
- Se propone mapear ausencia de estación a 404, validaciones a 400 y
  conflictos de nombre/capacidad/versión/integridad a 409, sin exponer SQL.
  Las denegaciones de seguridad seguirán su recorrido 403, sin un catch
  genérico que las convierta en errores de datos.
- El frontend todavía no envía version. La instrucción manual siguiente
  será preparar primero el controlador y comprobar compilación, sin probar
  aún la edición desde el panel ni reiniciar. Después se conectará el envío
  de versión y la presentación de errores para evitar declarar ese circuito
  funcional antes de completar ambos lados.
- Solo se actualizó este diario. No hubo escritura de fuentes, staging,
  commit, push, arranque/reinicio, SQL o cambios a las cuentas de prueba.

### Etapa 7 Implementación de la delegación del PUT

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó al asistente a implementar únicamente los puntos
1 a 3 de la propuesta del controlador, reservándose el punto 4 de guardar y
comprobar. Se aplicó el cambio y se revisó estáticamente, sin compilar ni
ejecutar pruebas.

- Se agregaron los imports de ActualizarEstacionDTO, EstacionService y las
  excepciones específicas, y la inyección de EstacionService mediante
  @Autowired. El PUT recibe @Valid @RequestBody ActualizarEstacionDTO y
  delega la actualización en el servicio; ya no duplica las reglas de edición.
- Se eliminó @Transactional exclusivamente del PUT. La transacción se
  completa en el servicio antes de convertir errores en respuestas HTTP;
  se conservan intactos @Transactional del DELETE y su import.
- Se mapean las validaciones del servicio y los argumentos inválidos a 400,
  la estación inexistente o de baja a 404, y los conflictos de capacidad,
  nombre, versión o integridad a 409. Los mensajes de concurrencia e
  integridad no exponen SQL. La validación del cuerpo conserva el handler
  global existente. No se agrega catch genérico ni se interceptan las
  denegaciones de seguridad, que mantienen su recorrido 403.
- La comparación del contenido con el inicio de esta intervención confirmó
  que GET, POST y DELETE permanecen idénticos. La comparación SHA-256 de
  src confirmó un único archivo fuente modificado: EstacionController.java,
  sin altas ni eliminaciones de fuentes. git diff --check terminó sin errores.
- Este diario es el único documento actualizado. No se modificaron otros
  controladores, servicio, DTO, entidad, repositorios, frontend, configuración,
  staging, commits, base local o cuentas de prueba. No se arrancó ni reinició
  la aplicación. La compilación con compileJava queda a cargo del usuario.
- El frontend todavía no envía version: antes de probar la edición desde
  Administración hay que conectar ese dato y la presentación de errores.
  Las pruebas de este circuito y la auditoría administrativa siguen pendientes;
  este incremento no declara terminado el módulo de estaciones v5.

### Etapa 8 Compilación del controlador y enlace propuesto con el formulario

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó compileJava con BUILD SUCCESSFUL en 2 s, una
tarea ejecutada. Se releyó EstacionController y se confirmó que conserva
la delegación de la etapa 7. No se ejecutaron compilación o pruebas nuevas.

- Se revisaron el modal de estaciones en admin.html, abrirModalEstacion,
  editarEstacion y guardarEstacion en admin.js, ActualizarEstacionDTO y la
  salida EstacionDTO. El GET incluye version, pero el formulario actual
  todavía no la conserva ni la envía al PUT.
- El siguiente paso manual propuesto agrega un campo oculto estacion-version,
  lo limpia al preparar un alta y lo carga al editar. El valor 0 es válido:
  no debe confundirse con un campo sin versión.
- Antes de enviar una edición, guardarEstacion deberá comprobar que haya
  una versión entera no negativa e incluirla numéricamente solo en el PUT.
  La comprobación visual no sustituye el control del servicio ni @Version.
  No se modifica el contrato del POST ni se asigna una versión en el alta.
- Se propone sustituir únicamente el catch de guardarEstacion para mostrar
  los mensajes de texto del backend o los validationErrors existentes, sin
  los prefijos técnicos de campo, utilizando texto y no HTML. Si faltan
  detalles se conserva un mensaje genérico, sin inventar un motivo de fallo.
- Estas son instrucciones, no cambios ya implementados en el frontend.
  Solo se actualizó este diario. No hubo cambios de fuentes, configuración,
  staging, commits, SQL, arranque/reinicio o cuentas QA. La comprobación
  funcional de persistencia, concurrencia, permisos y mensajes sigue pendiente;
  compileJava no comprueba JavaScript ni demuestra ese circuito en ejecución.

### Etapa 9 Revisión del pegado y comprobaciones aisladas del formulario

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó que agregó y guardó los cambios de la etapa 8
y que compileJava terminó correctamente. Se revisaron los archivos y el
diff del frontend. Los cambios están en los lugares indicados: no hay
modificaciones ajenas al bloque en admin.html o admin.js respecto de HEAD.

- admin.html contiene una única entrada oculta estacion-version, dentro
  de modalEstacion e inmediatamente después de estacion-id. Se conserva
  la carga de Bootstrap, Axios, SweetAlert y admin.js al final de la página.
- abrirModalEstacion limpia la versión sin quitar los campos anteriores;
  editarEstacion carga e.version con ??, conservando el valor 0. El control
  de guardarEstacion está después de construir data y antes del try; agrega
  version numéricamente solo en una edición. El POST no la envía.
- El catch sustituido pertenece únicamente a guardarEstacion. Se conservan
  PUT, POST, cierre del modal, recarga de estaciones, aviso de éxito y las
  demás funciones de usuarios, estaciones, bicicletas y alquileres. Los
  errores se muestran como texto y no cierran ni limpian el formulario.
- Se ejecutó node --check sobre admin.js sin errores. Además se cargó el
  archivo en node:vm con DOM, Axios, Bootstrap y SweetAlert simulados. Se
  comprobaron nueve grupos: ubicación/unicidad del campo; limpieza al crear;
  carga de versión 0; PUT con versión numérica y datos previos; POST sin
  versión; bloqueo de versiones vacías, en blanco, negativas, decimales o
  no numéricas; mensaje de conflicto sin cerrar el modal; errores de
  validación sin prefijos ni duplicados; fallo de red con mensaje genérico.
  Todos pasaron. Las comprobaciones se ejecutaron en memoria, sin crear
  archivos de pruebas ni generar solicitudes HTTP reales.
- Se detectó un detalle preexistente y ajeno al pegado: editarEstacion usa
  e.latitud || '' y e.longitud || '', por lo que coordenadas válidas con
  valor 0 quedan vacías. Se recomienda sustituir ambos || por ?? en esas
  asignaciones. No se aplicó el cambio en esta revisión.
- Solo se actualizó este diario. No se editaron fuentes, configuración,
  staging o commits, ni se ejecutaron Gradle, SQL, arranques/reinicios o
  acciones en el navegador. La compilación Java fue informada por el usuario;
  los resultados aislados de JavaScript no sustituyen las pruebas reales
  de persistencia, concurrencia, permisos y presentación visual pendientes.

### Etapa 10 Coordenadas revisadas y pruebas del servicio propuestas

Fecha: 6 de noviembre de 2026.

Estado: se comprobó que el usuario sustituyó || por ?? únicamente en las
asignaciones de latitud y longitud de editarEstacion. node --check terminó
correctamente; una comprobación aislada con node:vm confirmó que ambas
coordenadas 0 y la versión 0 se conservan, y que coordenadas null dejan
los campos vacíos. No se generaron solicitudes HTTP reales ni SQL.

- Se releyeron EstacionService, ActualizarEstacionDTO, EstacionDTO, Estacion,
  los dos repositorios involucrados, build.gradle y el patrón de pruebas
  existente en UsuarioServiceTest. Todavía no existe EstacionServiceTest.
- El siguiente bloque manual propuesto es crear EstacionServiceTest en
  src/test/java/DSII/TurismoSustentable/services, usando JUnit 5 y Mockito.
  Comprobará edición válida con versión 0 y coordenadas 0, estación ausente,
  versión desactualizada, nombre ocupado y capacidad menor que la ocupación
  informada por el repositorio. En los rechazos se comprobará que no se
  alteraron los datos previos ni se solicitó flush.
- Son pruebas unitarias de la lógica del servicio con repositorios simulados:
  no prueban consultas SQL, incremento real de @Version, bloqueos simultáneos,
  rollback transaccional, validación mediante proxy, seguridad o respuestas
  HTTP. Esas comprobaciones siguen pendientes y no se consideran cubiertas
  por un test que invoque directamente al servicio construido con Mockito.
- Se dará un comando filtrado exclusivamente a EstacionServiceTest, sin
  ejecutar el test general de la aplicación que puede usar la base real.
  El archivo y su ejecución quedan a cargo del usuario; en esta intervención
  no se creó ningún test Java ni se ejecutó Gradle o se reinició la aplicación.
- Solo se actualizó este diario. Se conservaron fuentes, staging, commits,
  base de datos y cuentas QA. git diff --check no informó errores.

### Etapa 11 Implementación puntual de EstacionServiceTest

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó al asistente a crear exclusivamente el bloque
de pruebas propuesto en la etapa 10, reservándose la parte 3 de guardar y
comprobar. Se creó EstacionServiceTest.java con el código presentado, sin
compilar ni ejecutar pruebas.

- El archivo está en src/test/java/DSII/TurismoSustentable/services, con el
  package correcto, JUnit 5, MockitoExtension, los dos repositorios @Mock
  y EstacionService @InjectMocks. No carga un contexto Spring ni una base.
- Se preparan una estación con versión 0 y un DTO válido por prueba. Los
  campos ID y versión de la entidad se fijan con ReflectionTestUtils solo
  en el test: no se agregan setters ni cambios al modelo de producción.
- Se incluyen exactamente las cinco pruebas propuestas: actualización válida
  con versión y coordenadas 0; estación no encontrada; versión distinta;
  nombre ocupado; capacidad menor que el conteo físico simulado. Se verifica
  el uso del nuevo conteo sin excluir bajas y el flush en la edición válida.
- En los cuatro rechazos se comprueba que nombre, dirección, capacidad,
  coordenadas, versión y baja lógica no cambien ni se solicite flush. En
  los rechazos que preceden al conteo no se consulta BicicletaRepository.
- Se releyó el archivo y se contrastaron los nombres de métodos, tipos y
  constructor con el código vigente. La comparación SHA-256 de todo src
  con el inicio de esta intervención confirmó solo un alta, el nuevo test,
  sin modificar ni eliminar ningún archivo previo. Se preservó el staging.
- Este diario es el único documento actualizado. No se modificaron fuentes
  de producción, pruebas existentes, configuración, frontend, commits, base
  de datos o cuentas QA; no se ejecutó Gradle, SQL ni arranque/reinicio.
- El usuario conserva la ejecución filtrada: ./gradlew.bat test --tests
  "EstacionServiceTest". La compilación y el resultado están pendientes.
  Estos mocks no incrementan la versión como Hibernate: las comprobaciones
  de transacciones, concurrencia real, consultas, proxies y permisos HTTP
  siguen pendientes y no se declaran cubiertas por estas pruebas unitarias.

### Etapa 12 Resultado verificado de EstacionServiceTest

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó ./gradlew.bat test --tests "EstacionServiceTest"
e informó BUILD SUCCESSFUL en 8 s, tres tareas ejecutadas y una actualizada.
Se leyó el XML generado en build/test-results/test para corroborarlo:
tests=5, skipped=0, failures=0, errors=0; ejecución del 6 de noviembre.

- El reporte contiene las cinco pruebas creadas: actualización válida,
  estación no encontrada, versión desactualizada, nombre ocupado y capacidad
  menor que la ocupación física simulada. Todas finalizaron correctamente.
- El resultado comprueba la lógica aislada del servicio; no demuestra el
  incremento de versión de Hibernate, las consultas o bloqueos en MySQL,
  transacciones reales, autorización mediante proxy, validación del cuerpo
  HTTP o la interacción del panel con la aplicación ejecutándose.
- El siguiente bloque propuesto es verificar el contrato del PUT con un
  EstacionControllerTest aislado: respuesta satisfactoria y delegación al
  servicio, rechazo de cuerpos inválidos con el handler de validaciones,
  y conversión de las excepciones previstas a 400, 404 y 409. Sin arrancar
  la aplicación ni usar la base real. Ese archivo todavía no se creó.
- Solo se actualizó este diario; no se modificaron fuentes o configuración,
  staging, commits, base local o cuentas QA. No se ejecutaron nuevas pruebas,
  Gradle, SQL, arranques/reinicios o acciones de navegador en esta revisión.

### Etapa 13 Propuesta manual de EstacionControllerTest

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió continuar con el siguiente bloque. Se releyeron
EstacionController, ActualizarEstacionDTO, GlobalExceptionHandler y el
patrón standalone de RecuperacionPasswordControllerTest. Se preparan
instrucciones para un único archivo nuevo de pruebas, sin crearlo todavía.

- Se propone EstacionControllerTest en el package controllers de src/test,
  con JUnit 5, Mockito y MockMvc standalone. Se inyecta un EstacionService
  simulado en el controlador y se utiliza el handler global real existente.
  No se carga el contexto Boot, repositorios JPA o una base de datos.
- Los casos previstos son respuesta 200 y DTO normalizado enviado al
  servicio; campos obligatorios ausentes; valores inválidos; cuerpo ausente,
  nulo o JSON malformado; estación no encontrada con 404; conflictos de
  negocio con 409; ambas excepciones de versión con 409; integridad con
  mensaje genérico sin detalles internos; argumentos inválidos con 400.
- En los rechazos previos a la invocación se verificará que el servicio no
  recibió llamadas. Las respuestas conflictivas conservarán el mensaje de
  negocio o usarán los mensajes seguros ya definidos en el controlador.
- El servicio simulado no ejecuta @PreAuthorize, @Validated o transacciones.
  MockMvc standalone sin filtros de seguridad comprueba enlace HTTP,
  conversión JSON, @Valid del cuerpo y respuestas, no los permisos de la
  configuración real ni el control concurrente en MySQL. No se presentará
  este test como una comprobación de esos aspectos pendientes.
- Se dará un comando filtrado a EstacionControllerTest y EstacionServiceTest.
  No se creó ni ejecutó el archivo propuesto, no se modificaron fuentes,
  frontend, configuración, staging, commits o base, y no se reinició la app.
  Solo se actualizó este diario de implementación.

### Etapa 14 EstacionControllerTest creado y catorce pruebas verificadas

Fecha: 6 de noviembre de 2026.

Estado: el usuario creó EstacionControllerTest.java y ejecutó el comando
filtrado a EstacionControllerTest y EstacionServiceTest, con BUILD SUCCESSFUL
en 5 s, dos tareas ejecutadas y dos actualizadas. Se releyó el archivo y se
corroboró que contiene las nueve pruebas propuestas en su package correcto,
sin contexto Boot ni acceso a una base de datos.

- Se verificaron ambos XML de build/test-results/test: controlador, tests=9;
  servicio, tests=5. Ambos reportes tienen skipped=0, failures=0 y errors=0,
  y corresponden a la ejecución conjunta del 6 de noviembre de 2026. Total:
  catorce pruebas satisfactorias, no solamente una compilación exitosa.
- Las pruebas comprueban la lógica aislada del servicio y el contrato HTTP
  del PUT con servicio simulado: enlace de JSON, normalización, @Valid,
  cuerpo ausente/malformado, mensajes de validación y estados 200/400/404/409.
  No comprueban permisos reales, transacciones en MySQL, consultas derivadas,
  incremento real de @Version o la interacción del navegador con la app.
- Sigue pendiente una comprobación funcional controlada de la edición desde
  el panel y de su persistencia real: guardado, versión devuelta, rechazo de
  un formulario desactualizado y permisos. Antes de iniciar/reiniciar se debe
  tener presente la recreación de datos del perfil refactor con create-drop.
  No se ejecutó esa comprobación ni se declara terminado todo estaciones.
- Solo se actualizó este diario. Se preservaron los archivos agregados y el
  staging parcial del usuario; no se modificaron fuentes o configuración ni
  se ejecutaron nuevas pruebas, Gradle, Git de escritura, SQL o reinicios.

### Etapa 15 Preparación manual de la prueba funcional de estaciones

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió continuar con la comprobación real. Se releyeron
application-refactor.properties, el main, CargaCatalogosRefactor, la entidad
UsuarioRol y las referencias de login por nickname. El perfil apunta a
MySQL local, base turismo_sustentable, y conserva create-drop. El main no
crea datos; la carga de catálogos tampoco crea cuentas de administrador.

- La inspección de puertos no mostró listeners en 8080 o 3306. No se inició
  ni detuvo ningún proceso. Tampoco se consultaron o modificaron tablas.
- El siguiente paso propuesto al usuario es iniciar MySQL desde XAMPP
  (Apache solo para utilizar phpMyAdmin) y arrancar la aplicación con el
  perfil refactor y puerto 8080. Antes se advierte expresamente que ese
  perfil recrea las tablas al iniciar y las elimina al cerrar, y que no debe
  arrancarse de esa forma si hay datos locales que se quieran conservar.
- Se espera que el usuario confirme el arranque antes de continuar con una
  cuenta local de prueba y su asignación administrativa controlada. No se
  agrega un inicializador temporal ni se reintroducen usuarios en el main.
  No se asignaron roles, no se registró ninguna cuenta ni se ejecutó SQL.
- Las comprobaciones reales del PUT, incremento de versión, formulario
  desactualizado, mensajes y permisos siguen pendientes. Esta etapa es de
  preparación e instrucciones, no evidencia de un arranque o test realizado.
- Solo se actualizó este diario. Se preservaron código, configuración,
  pruebas, frontend, staging, commits, base y cuentas existentes. No se
  ejecutó Gradle ni hubo modificaciones en la aplicación en esta revisión.

### Etapa 16 Arranque informado y preparación de cuenta administrativa QA

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó Tomcat escuchando en 8080 y arranque de la
aplicación en 4,8 s, proceso 4920. El aviso de open-in-view no es un fallo
de inicio; no se propone desactivarlo en esta prueba, dado que los DTO y
consultas vigentes todavía pueden usar relaciones lazy.

- Se intentaron dos GET locales de solo lectura, a registro.html y a
  /api/estaciones, pero el entorno de herramientas denegó el acceso al
  socket. No se obtuvo respuesta HTTP ni un conteo válido de estaciones;
  no se interpreta la salida vacía como ausencia de datos. El arranque se
  documenta a partir del log aportado por el usuario, no de esos intentos.
- Se revisaron el formulario de registro, las claves de UsuarioRol y el
  login por nickname. Se proponen datos ficticios para una cuenta local
  admin.estaciones, inicialmente CLIENTE por el flujo de registro normal.
- El usuario registrará la cuenta desde registro.html sin iniciar sesión
  todavía. La asignación de ADMIN se guiará después sobre la fila concreta,
  sin introducir usuarios hardcodeados ni un inicializador temporal.
- No se registró ninguna cuenta, se asignaron roles ni se ejecutó SQL desde
  el asistente. El panel y las operaciones de estaciones siguen pendientes
  de comprobación real; el arranque por sí solo no acredita esos recorridos.
- Solo se actualizó este diario; no hubo modificaciones de fuentes,
  configuración, staging, commits, arranques/reinicios o ejecución de Gradle.

### Etapa 17 Cuenta QA verificada en MySQL sin modificar permisos

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó que registró la cuenta y pidió verificarla
directamente en la base. Se consultó MySQL local en 127.0.0.1:3306 mediante
el cliente de XAMPP, seleccionando explícitamente turismo_sustentable y
usando START TRANSACTION READ ONLY. El primer intento fue bloqueado por
el entorno; el reintento con permiso de acceso terminó correctamente.

- Se confirmó una única cuenta con nickname admin.estaciones: usuario ID 1,
  email admin.estaciones@example.com, activa, versión 0 y persona ID 1.
  La persona relacionada contiene nombre Admin y apellido Prueba.
- La unión con usuario_rol y rol confirmó exclusivamente CLIENTE para esta
  cuenta. Todavía no dispone de ADMIN. No se presupusieron IDs de roles ni
  se reemplazó o agregó ninguna asignación durante la consulta.
- No se consultaron ni mostraron password, hashes, tokens de recuperación
  o número de documento. No se registraron usuarios ni se ejecutaron INSERT,
  UPDATE, DELETE, DDL o cambios de permisos. El COMMIT cerró una transacción
  expresamente de solo lectura.
- La asignación administrativa controlada de esta cuenta QA sigue siendo
  el siguiente paso, antes de probar el panel. Las pruebas funcionales de
  estaciones continúan pendientes; esta revisión solo acredita el registro
  persistido y sus relaciones actuales.
- Solo se actualizó este diario. No se modificaron código, configuración,
  staging, commits o datos de la aplicación ni se ejecutó Gradle o reinicios.

### Etapa 18 Asignación manual de ADMIN propuesta para la cuenta QA

Fecha: 6 de noviembre de 2026.

Estado: tras aclarar el modelo usuario-rol y su PK compuesta, el usuario
pidió continuar. Se repitió una consulta local de solo lectura: usuario
admin.estaciones ID 1, email admin.estaciones@example.com, activo=1;
rol ADMIN ID 1, CLIENTE ID 2; asignación existente (usuario=1, rol=2).
El asistente no agregó ni eliminó asignaciones.

- Se propone ejecutar desde SQL de phpMyAdmin un INSERT SELECT acotado
  exclusivamente a la cuenta QA por nickname, email y activo, y al rol por
  codigo=ADMIN. Se utiliza turismo_sustentable explícitamente. No se fijan
  IDs de roles o usuarios en el INSERT ni se modifica CLIENTE.
- El LEFT JOIN con usuario_rol y el filtro de asignación ausente evitan
  repetir la inserción si ADMIN ya está asignado. La PK compuesta conserva
  además la restricción de no duplicar el mismo par usuario/rol.
- Se acompaña una consulta de lectura de las asignaciones de esa cuenta;
  después de la ejecución manual se espera ADMIN y CLIENTE. Si existía una
  sesión previa de CLIENTE, se debe cerrar e iniciar nuevamente para que
  Spring Security cargue las autoridades actualizadas.
- Esta es preparación de datos locales de prueba, no implementación del
  flujo administrativo de asignación de roles ni un cambio al registro
  público, que sigue asignando CLIENTE. La ejecución manual y la entrada
  al panel todavía no se comprobaron y no se presentan como realizadas.
- Solo se actualizó este diario. No hubo cambios de código, configuración,
  staging, commits, contraseñas, datos o permisos desde el asistente; no
  se ejecutaron SQL de escritura, Gradle o reinicios.

### Etapa 19 Acceso ADMIN observado y primera estación QA propuesta

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó que inició sesión y ve el panel administrativo.
Una consulta MySQL de solo lectura confirmó ADMIN y CLIENTE para usuario
admin.estaciones ID 1. La asignación manual fue realizada por el usuario;
el asistente no ejecutó escrituras. La consulta de estacion no devolvió
filas y bicicleta tiene cero filas en este entorno local de prueba.

- Se leyó el skill de control de aplicaciones y sus referencias de guía y
  confirmaciones. Siguiendo su preferencia por la interfaz específica de
  navegador, se observó la pestaña existente del navegador integrado con
  la API de navegador. La pantalla mostró admin.html, Admin Prueba activo
  y la columna Rol con ADMIN. No se presionaron botones, navegaron páginas,
  cambiaron campos, guardaron datos ni cerró sesión durante esa observación.
- Se contrastó admin.js: la fila muestra u.rol. UsuarioDTO conserva ese
  campo de compatibilidad priorizando ADMIN, pero también expone roles
  completo. La falta de CLIENTE en la grilla no indica pérdida de permisos.
- Se propone al usuario cambiar solo la celda a u.roles.join(', ') y el
  encabezado Rol a Roles, sin tocar getRol, la sesión o las reglas de
  seguridad. Es un ajuste de presentación aún no aplicado por el asistente;
  para cargarlo basta recargar la página, no reiniciar la aplicación.
- El siguiente paso funcional propuesto es crear desde Estaciones una fila
  ficticia Estación QA Centro, dirección Calle de Prueba 100, capacidad 5,
  latitud -53.7865 y longitud -67.7045. Tras la creación se verificará en
  MySQL la versión inicial antes de probar edición y formulario desactualizado.
  La creación no se ejecutó ni se da por correcta en esta intervención.
- La creación usa el POST existente como preparación; no acredita su
  adaptación completa a v5. Edición real, incremento de versión, concurrencia,
  permisos de clientes y demás recorridos siguen pendientes de evidencia.
- Solo se actualizó este diario. No se modificaron fuentes, configuración,
  staging, commits, datos o permisos desde el asistente, ni hubo Gradle o
  reinicios. El uso de navegador fue exclusivamente de lectura.

### Etapa 20 Estación QA creada y versión inicial verificada

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó que creó y guardó la estación desde el panel.
Se consultó MySQL local con turismo_sustentable explícita y transacción de
solo lectura. El listado confirmó exactamente una estación, identificada
por ID 1; bicicleta continúa sin filas. No hubo escrituras del asistente.

- Datos persistidos: nombre Estación QA Centro, dirección Calle de Prueba
  100, capacidad 5, latitud -53.7865, longitud -67.7045, version=0 y
  borrado_estacion=0. Queda registrada la versión inicial real, no simulada.
- La búsqueda inicial por nombre con literal acentuado no devolvió filas;
  se resolvió consultando el listado y la PK, que mostraron la fila esperada.
  No se concluye por ello que el registro faltara ni se atribuye una causa
  de codificación no diagnosticada. Las verificaciones siguientes usarán ID.
- Se verificaron también los cambios manuales de presentación: admin.js
  muestra u.roles.join(', ') y admin.html dice Roles. node --check no detectó
  errores de sintaxis. No se alteró la compatibilidad rol ni la seguridad.
- El siguiente paso manual propuesto es editar exclusivamente la dirección
  a Calle de Prueba 200 y la capacidad a 6 desde el lápiz del panel. Se
  conservarán nombre y coordenadas. Después se comprobará por ID 1 que los
  datos persistan y que la versión real pase de 0 a 1, sin reiniciar la app.
  Esa edición aún no se realizó ni se declara comprobada en esta etapa.
- La creación prepara el escenario utilizando el POST existente; no prueba
  aún la edición nueva, su concurrencia, la capacidad con stock, permisos
  de clientes o la adaptación completa del alta/baja al DER v5.
- Solo se actualizó este diario. No se modificaron fuentes, configuración,
  staging, commits o datos desde el asistente, ni se ejecutó Gradle, SQL de
  escritura, acciones de navegador o reinicios.

### Etapa 21 Edición real persistida e incremento de versión verificado

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó que editó la estación desde el panel siguiendo
la etapa 20. Una consulta MySQL de solo lectura a turismo_sustentable,
filtrada por PK id=1, confirmó dirección Calle de Prueba 200, capacidad 6
y version=1. En la etapa anterior la misma fila tenía dirección 100,
capacidad 5 y version=0. La tabla conserva una sola estación.

- Nombre Estación QA Centro, latitud -53.7865, longitud -67.7045 y
  borrado_estacion=0 permanecieron iguales. Se verificó persistencia de
  la edición y aumento real de versión, sin SQL de escritura del asistente.
- El siguiente caso propuesto es formulario desactualizado: abrir la edición
  de esta estación en dos pestañas autenticadas antes de guardar ninguna;
  guardar dirección Calle de Prueba 300 desde A y luego intentar Calle de
  Prueba 400 desde B, que conserva la versión anterior. Capacidad y demás
  campos permanecerán iguales. Si otra pestaña no conserva sessionStorage,
  se iniciará sesión con la misma cuenta antes de abrir los formularios.
- Se espera que A guarde y que B muestre el conflicto sin sobrescribir A.
  La verificación posterior deberá encontrar dirección 300 y version=2.
  No se realizó aún ese caso ni se presume que ya esté aprobado.
- Es una prueba secuencial de formulario obsoleto, no una prueba de carga
  o de dos transacciones realmente simultáneas. Capacidad con stock y
  permisos de clientes siguen pendientes de comprobación real.
- Solo se actualizó este diario. Se realizaron únicamente consultas SQL
  de lectura; no hubo cambios de fuentes, configuración, staging, commits,
  datos o sesiones desde el asistente ni ejecución de Gradle o reinicios.

### Etapa 22 Formulario desactualizado rechazado y datos de A conservados

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó que la pestaña B mostró el aviso de estación
modificada y rechazó el guardado. La consulta MySQL de solo lectura por
id=1 confirmó dirección Calle de Prueba 300 y version=2. Capacidad 6,
nombre, coordenadas y borrado_estacion=0 se conservaron; hay una estación
y cero bicicletas. No quedó persistida la dirección 400 propuesta desde B.

- Quedan comprobados conjuntamente el aviso informado desde el navegador
  y la conservación en MySQL de los datos de A. El intento rechazado de B
  no produjo un nuevo incremento de versión respecto del guardado de A.
  No se capturó el tráfico de red para afirmar un código HTTP observado.
- El caso demuestra rechazo de una edición con versión obsoleta en dos
  formularios reales. No se confunde con una prueba de dos transacciones
  simultáneas o de carga, que no se ejecutó.
- El siguiente escenario propuesto es capacidad con ocupación física real:
  cerrar B y recargar A; crear dos bicicletas QA disponibles en esta estación
  mediante el panel existente, con códigos QA-B01 y QA-B02, tipo Montaña y
  precio/hora ficticio 1; intentar después reducir capacidad a 1. Se espera
  rechazo con mensaje de dos bicicletas presentes y conservación de capacidad 6.
- Se revisaron los campos del modal y el envío de guardarBicicleta para
  preparar ese escenario. Se usa la flota legacy solo como datos locales
  de prueba: no se agregan modelos o lógica temporales ni se considera
  implementada la tarifa por tipo o el resto de la flota DER v5.
- Las bicicletas y el intento de reducción todavía no se crearon ni
  ejecutaron en esta etapa. Permisos de cliente, otros errores de edición
  y adaptación completa de altas/bajas continúan pendientes.
- Solo se actualizó este diario. No se modificaron fuentes, configuración,
  staging, commits, datos o sesiones desde el asistente; no hubo SQL de
  escritura, acciones de navegador, ejecución de Gradle o reinicios.

### Etapa 23 Capacidad inferior a la ocupación rechazada y verificada

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó que el panel rechazó capacidad 1 al detectar
dos bicicletas. Se verificó MySQL local en una transacción de solo lectura,
con base turismo_sustentable y consultas acotadas a la estación ID 1 y los
códigos QA-B01 y QA-B02. La tabla bicicleta contiene exactamente dos filas.

- QA-B01: ID 1, MONTANA, DISPONIBLE, precio_por_hora 6.00, estacion_id 1,
  borrado_bici=0. QA-B02: ID 2, MONTANA, DISPONIBLE, precio_por_hora 5.00,
  estacion_id 1, borrado_bici=0. Ambas están físicamente asociadas a la
  estación de prueba; no se presupusieron los precios ficticios propuestos
  de 1, ya que los valores realmente guardados por el usuario son 6 y 5.
- La estación conserva capacidad 6, dirección Calle de Prueba 300,
  version=2, nombre Estación QA Centro, latitud -53.7865, longitud -67.7045
  y borrado_estacion=0. El conteo físico directo de bicicleta por estacion_id
  es 2. El intento rechazado no redujo la capacidad ni incrementó la versión.
- Se acredita este caso de edición con ocupación real y aviso informado
  por el usuario. No se capturó la respuesta HTTP ni se presentan estos
  resultados como prueba de tarifas por tipo, bicicletas de baja todavía
  presentes, carreras simultáneas o autorización de clientes; esos casos
  no se ejecutaron en esta intervención.
- Solo se actualizó este diario. El asistente no creó o modificó bicicletas,
  estaciones, precios, roles o sesiones. No hubo cambios de fuentes,
  configuración, staging, commits, Gradle, SQL de escritura o reinicios.

### Etapa 24 Prueba propuesta de bicicleta de baja físicamente presente

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió continuar. Se revisaron el DELETE de bicicletas,
su llamada desde borrarBici y la regla de ocupación ya asentada en la
revisión de estaciones del DER v5. El código legacy marca borradoBici=true
y estado INACTIVA, conserva la fila y no elimina ubicacionActual.

- Se propone al usuario dar de baja exclusivamente QA-B02 desde el panel,
  conservando QA-B01. Son datos ficticios de este escenario descartable.
  No se implementa ni modifica el recorrido de baja de flota v5 en este paso.
- Después se recargará el panel, se abrirá una edición nueva de Estación
  QA Centro y se intentará capacidad 1, sin cambiar otros campos. Aunque
  la lista de bicicletas oculte la baja, la ocupación física deberá seguir
  siendo 2 mientras QA-B02 conserve estacion_id=1.
- Se espera el mismo rechazo por ocupación real y conservación de capacidad
  6. La consulta posterior deberá comprobar que ambas filas siguen presentes,
  que solo QA-B02 tiene baja lógica y que la capacidad no se alteró. No se
  da por realizada o aprobada la prueba antes de la respuesta del usuario.
- Se distingue explícitamente el conteo físico total del conteo de bicicletas
  disponibles o sin baja. Esta regla ya estaba aprobada; no se añade una
  entidad, estado nuevo o condición de negocio mediante esta prueba.
- Solo se actualizó este diario. El asistente no dio de baja bicicletas,
  modificó datos, roles o sesiones, ni ejecutó SQL, Gradle, reinicios o
  acciones de navegador. Se preservaron fuentes, configuración y staging.

### Etapa 25 Baja lógica presente y faltante operativo de retiro identificado

Fecha: 6 de noviembre de 2026.

**Aclaración posterior — ver etapa 26:** la prueba se realizó sobre una
bicicleta con borrado lógico legacy, no sobre el estado DE_BAJA de v5.
La explicación de esta etapa no debe interpretarse como equivalencia entre
borrado del registro y retiro definitivo del servicio.

Estado: el usuario informó que el intento de capacidad 1 volvió a rechazarse
tras dar de baja QA-B02, y preguntó cómo se resuelve su presencia física en
la operación real. La consulta MySQL de solo lectura confirmó QA-B01 ID 1
DISPONIBLE, estacion_id=1, borrado_bici=0; QA-B02 ID 2 INACTIVA,
estacion_id=1, borrado_bici=1; estación ID 1 con capacidad 6 y version=2.

- Queda verificado el rechazo por ocupación total incluso con una baja
  lógica físicamente presente. No hay orfandad referencial: la fila de
  QA-B02 y su FK a estación existen. Sí hay una limitación operativa real
  del código legacy: el GET de bicicletas excluye las bajas y el PUT las
  rechaza. El administrador no dispone de ese recorrido de salida desde
  el panel actual; no se da por terminado el módulo de flota.
- Se contrastó con la revisión aprobada de estaciones/tipos/bicicletas y
  el diccionario de estados propuesto: DE_BAJA no implica salida física
  automática, ni borra el historial. Mientras permanezca en la estación
  consume un espacio. La regla del servicio de estaciones es coherente.
- Para el bloque Bicicleta del DER v5 queda explícito conectar la operación
  ya prevista: ADMIN puede consultar las unidades DE_BAJA y registrar una
  salida física real, conservando el estado de baja y las referencias
  históricas. Al confirmar el retiro fuera de las estaciones se vacía la
  FK de presencia actual y se registra actor, fecha, motivo y ubicación
  anterior/nueva mediante AUDITORIA_SISTEMA. Solo entonces se libera espacio.
- Si se registra un traslado a otra estación, no se trata como desaparición:
  la estación receptora necesita sus comprobaciones de capacidad y el
  movimiento conserva trazabilidad. No se inventa una estación para la baja
  ni se libera la FK por pulsar la papelera.
- Se registra como recorrido pendiente de la implementación v5, no como
  nuevo estado o tabla aprobados ni como cambio ya aplicado. No se abre
  la edición genérica de las bajas ni se reactiva la bicicleta para moverla.
- Solo se actualizó este diario. No se modificaron código, configuración,
  DER, staging, commits, datos o roles desde el asistente; no hubo SQL de
  escritura, acciones de navegador, ejecución de Gradle o reinicios.

### Etapa 26 Corrección conceptual: DE_BAJA no equivale a borrado lógico

Fecha: 6 de noviembre de 2026.

Estado: el usuario aclaró expresamente que las bicicletas DE_BAJA y las
bicicletas borradas son conceptos distintos. Se releyó su DER v4 original:
BICICLETA contiene bit borrado_bici y estado_bicicleta_id como atributos
separados. El asistente reconoce que su explicación anterior los asimiló
indebidamente. No se toma ese supuesto como una condición aprobada.

- DE_BAJA pertenece al estado operativo y representa retiro definitivo del
  servicio. Borrado lógico del registro es otra decisión administrativa.
  Ninguno de esos datos representa por sí solo una salida física; la FK
  de estación sigue describiendo presencia real mientras no se registre
  el movimiento correspondiente.
- El código actual conserva borradoBici y un enum que todavía no incluye
  DE_BAJA. La QA-B02 de la prueba está INACTIVA con borrado_bici=1; no se
  afirma que se haya probado DE_BAJA en el backend ni su transición v5.
  Los resultados físicos y de capacidad observados permanecen válidos.
- Se detectó una divergencia del borrador v5 respecto de la separación del
  usuario: der-v5-propuesto.mmd omite borrado_bici y la sección 4 del documento
  26 había tratado su coexistencia con DE_BAJA como duplicación. Esa decisión
  de diseño necesita corrección/revisión; no puede usarse para volver a
  confundir ambos conceptos o eliminar automáticamente el flag del código.
- Queda pendiente reflejar la separación en los artefactos v5 y precisar
  las condiciones de borrado lógico y su gestión administrativa, sin
  atribuir al usuario una restricción aún no aprobada sobre presencia física,
  historial, procesos abiertos o recuperación del registro. No se agregan
  reglas por inferencia ni se implementan parches en esta explicación.
- Solo se actualizó este diario, incluyendo la advertencia en la etapa 25.
  No se modificaron DER, entregables, otros documentos, código, datos,
  permisos, configuración, staging o commits; no hubo SQL ni reinicios.

### Etapa 27 Propuesta de taller interno y cierre pendiente del borrado

Fecha: 6 de noviembre de 2026.

Estado: el usuario preguntó si cerrar esta distinción antes de continuar
y agregó una idea: permitir bajas definitivas, borrado lógico e inactivación
desde una ubicación Taller o Taller mantenimiento que funcione como depósito;
mantener en estaciones públicas bicicletas disponibles o pendientes de
atención técnica. Es una propuesta del usuario, no una regla ya implementada.

- Se utilizaron los skills de lectura de documentos y PDF, con el runtime
  de dependencias suministrado, para releer íntegramente el diccionario
  original y las páginas 2 a 5 completas del PDF de visión. Se contrastaron
  también las reglas aprobadas de reserva, preventiva y mantenimiento.
  No se hicieron cambios a esos originales ni una revisión de maquetación.
- Se propone conservar la separación de estado, borrado y presencia física:
  reincorporar borrado_bici al diseño de BICICLETA y representar taller como
  fila de ESTACION con un nuevo indicador es_publica. Esta distinción de
  dos clases no necesita una nueva tabla; el nombre Taller no será la regla.
- La ubicación interna no se ofrecería en mapa, contratación ni devolución
  por clientes, tanto en las consultas como en validaciones de escritura del
  backend. ADMIN la usaría para movimientos físicos autorizados, depósito y
  gestión de mantenimiento, con capacidad y auditoría coherentes.
- Se precisa una omisión de la idea inicial: RESERVADA también puede estar
  físicamente en una estación pública hasta su retiro. Se recomienda permitir
  además INACTIVA temporal allí para bloquear inmediatamente una bicicleta
  por motivo administrativo, sin esperar a que sea trasladada al taller.
  Esta excepción se presenta a aprobación, no se impone al usuario.
- Se propone gestionar DE_BAJA y borrado lógico desde el taller, conservando
  la distinción entre ambas acciones y exigiendo que no haya reservas, viajes,
  órdenes o incidencias abiertas de la unidad. Un borrado no simularía salida
  física ni borraría filas o historial; el inventario físico administrativo
  debe permitir consultar incluso las unidades archivadas que siguen allí.
- Las unidades conservan su ocupación hasta una salida física registrada.
  Movimientos requieren autorización, capacidad si hay estación receptora y
  auditoría de actor, fecha, motivo y ubicación anterior/nueva. No se cambia
  una FK automáticamente por pulsar borrar, dar de baja o inactivar.
- Se pide aprobar el conjunto antes de actualizar DER, diccionario o fuentes.
  No se considera cerrado este punto, no se agrega un estado o entidad por
  inferencia y no se crean inicializadores o código temporal para el taller.
- Solo se actualizó este diario. No se modificaron otros documentos,
  entregables, DER, código, configuración, staging, commits, sesiones o datos;
  no hubo SQL, acciones de navegador, ejecución de Gradle o reinicios.

### Etapa 28 Aclaración del usuario sobre las acciones y su ubicación

Fecha: 6 de noviembre de 2026.

Estado: el usuario resumió el criterio que está definiendo: para borrar
lógicamente o pasar a DE_BAJA la unidad tendría que estar en Taller;
INACTIVA puede aplicarse desde la misma estación; RESERVADA y DISPONIBLE
mantienen su recorrido existente. Se responde a esa interpretación sin
ejecutar cambios de modelo o datos.

- Se conserva la independencia entre estado operativo, borrado lógico y
  ubicación física. INACTIVA no traslada automáticamente; DE_BAJA tampoco
  archiva por sí misma ni un borrado representa retirar materialmente la bici.
- La nueva condición de ubicación no habilita eludir reservas, viajes,
  órdenes u otras operaciones abiertas. El taller sería una ubicación
  interna no ofrecida al cliente, con consulta administrativa de su stock
  físico incluso para registros archivados y trazabilidad de movimientos.
- No se añaden nuevas restricciones a DISPONIBLE o RESERVADA por inferencia
  en esta aclaración; se mantienen los controles de contratación y retiro
  ya aprobados. La ocupación continúa basada en la presencia física real.
- La confirmación final del conjunto y su reflejo en artefactos v5 siguen
  separados de esta explicación. No se modificaron DER, diccionario, fuentes,
  otros documentos, configuración, staging, commits, sesiones o datos.
  Solo se actualizó este diario; no hubo SQL, Gradle o reinicios.

### Etapa 29 RN-INV-01 aprobada e incorporada al DER v5

Fecha: 6 de noviembre de 2026.

Estado: el usuario aprobó expresamente el criterio para corregir el DER,
sin implementar todavía código. Se actualizó el modelo lógico editable
y su exportación TXT, y la documentación de diseño directamente afectada.

- ESTACION incorpora boolean es_publica obligatorio: true pública y false
  Taller/depósito interno. BICICLETA recupera boolean borrado_bici independiente
  del estado, inicialmente false. No se crean entidades, estados o FK nuevas.
- La sección 7 del documento 26 conserva RN-INV-01 con fecha de aprobación,
  representación, restricciones de clientes, permisos ADMIN, condiciones de
  Taller para DE_BAJA/archivo, INACTIVA en la estación actual, ocupación real,
  consulta de archivadas, auditoría y seis criterios de aceptación pendientes.
- Se corrige la propuesta de eliminar el flag en el documento 22 y se
  alinean 00, 08, 13, 20 y 21. Se conserva la diferencia entre disponibilidad,
  reserva, atención técnica, baja definitiva, borrado y presencia física.
- La condición nueva y exclusiva de Taller restringe la excepción anterior
  de baja de una EXTRAVIADA no recuperada: conserva EXTRAVIADA mientras esté
  ausente y no se inventa una recepción. Se explicitó en los documentos
  relacionados sin modificar el cierre del detalle o su liquidación.
- La comparación estructural antes/después del Mermaid confirmó 29 entidades
  y 55 relaciones, todas las PK/FK y atributos anteriores intactos, y solo
  dos atributos añadidos: ESTACION.es_publica y BICICLETA.borrado_bici. Cada
  relación sigue refiriendo entidades declaradas. No se eliminaron campos.
- Mermaid y DERv5.txt tienen el mismo contenido y SHA-256. La comprobación
  es estructural y de consistencia, no una renderización visual del diagrama
  ni una ejecución de SQL. git diff --check no informó errores de contenido.
- Los hashes de todo src y el diff staged se compararon con el inicio de
  esta intervención: sin modificaciones, altas o bajas en fuentes y con
  staging preservado. Los cambios Java y frontend anteriores del usuario
  se conservaron; no se mezcló esta corrección de diseño con implementación.
- No se tocaron los originales del usuario ni el Word entregado anteriormente.
  El índice aclara que no se regeneró el Word y remite a las condiciones
  vigentes de los documentos 08 y 26. No se crearon copias o fuentes paralelas.
- No hubo cambios de datos, clasificación de estaciones existentes, precarga
  de Taller, migraciones, ajustes de la QA-B02, configuración, commits o
  permisos. No se ejecutaron Gradle, SQL, navegador o reinicios. La regla
  nueva no se considera implementada o probada por actualizar el DER; las
  pruebas previas de edición no cubren es_publica o las nuevas condiciones.

### Etapa 30 Diferencia entre backend vigente y diseño de inventario aprobado

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió aclarar que borrado_bici ya existía, el tratamiento
actual de los estados y sus ubicaciones, el caso EXTRAVIADA y el tipo de
es_publica. Se releyeron EstadoBicicleta, BicicletaController y las referencias
de AlquilerService, y se contrastó RN-INV-01. No se proponen reglas nuevas.

- borradoBici ya existe en Java, en la base legacy y en el DER v4. La etapa
  29 lo reincorporó al DER v5, donde se había omitido indebidamente; no lo
  creó en el código o la base ni convirtió un estado en indicador de borrado.
- El enum vigente contiene solo DISPONIBLE, ALQUILADA, MANTENIMIENTO e
  INACTIVA. Crear fuerza DISPONIBLE; iniciar alquiler asigna ALQUILADA y
  vacía ubicación; finalizar vuelve a DISPONIBLE en la estación de devolución.
  El PUT protege ALQUILADA y no permite asignarla manualmente, pero todavía
  no aplica el nuevo circuito de estados y Taller del DER v5.
- El DELETE actual rechaza estado ALQUILADA y, en los demás casos, marca
  borradoBici=true y estado INACTIVA conservando la ubicación. No exige
  Taller; sus consultas ocultan las borradas y su PUT las rechaza. Es el
  comportamiento legacy observado en QA-B02, no la regla nueva aprobada.
- EXTRAVIADA y DE_BAJA todavía no están implementadas en el enum o como
  catálogo/recorrido del backend. La separación en siete estados, el cierre
  por incidencia, la recuperación y las restricciones de Taller son diseño
  aprobado, no funcionalidad ya disponible. Se explican ambas situaciones
  sin atribuir al código actual el flujo futuro.
- es_publica es un booleano previsto para ESTACION: true/1 pública y false/0
  interna/Taller. No es otro estado de bicicleta. Afectará mapa, consultas y
  validaciones de contratación/devolución; no basta ocultar un marcador.
  No se ha incorporado todavía a Java ni al circuito de la aplicación.
- Solo se actualizó este diario. No se modificaron DER, otros documentos,
  fuentes, configuración, staging, commits, datos o permisos, ni se ejecutaron
  SQL, Gradle, reinicios o acciones de navegador.

### Etapa 31 Primer paso manual propuesto para esPublica

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió continuar con el nuevo cambio de estaciones.
Se releyeron Estacion y EstacionDTO. La entidad todavía no tiene esPublica;
se ofrecen instrucciones manuales para agregarlo, sin editar fuentes.

- El bloque propuesto agrega en Estacion boolean esPublica = true, con
  @Column(name = "es_publica", nullable = false), y sus métodos isEsPublica
  y setEsPublica. No necesita imports nuevos porque javax.persistence.*
  ya está presente. No se cambian ID, versión, relaciones o constructores.
- El inicializador true conserva como públicos los objetos nuevos creados
  mediante los constructores anteriores. No es un DEFAULT SQL ni migra
  filas existentes; la persistencia real se comprobará cuando corresponda
  aplicar el esquema, respetando la advertencia de create-drop.
- El usuario comprobará con las catorce pruebas filtradas de controlador
  y servicio, sin MySQL. No se debe reiniciar aún ni crear Taller: DTO,
  consultas y validaciones de contratación/devolución todavía no están
  conectados a la clasificación, y el campo aislado no implementa RN-INV-01.
- El archivo y la ejecución quedan a cargo del usuario. En esta intervención
  solo se actualizó este diario; no se modificaron fuentes, DER, otros
  documentos, configuración, staging, commits, datos o permisos, ni hubo
  SQL, Gradle, navegador o reinicios.

### Etapa 32 esPublica incorporado puntualmente a Estacion

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó al asistente a implementar únicamente los
puntos 1 y 2 de la etapa 31, reservándose la parte 3 de guardar y comprobar.
Se agregó el atributo y sus dos accesores tal como se propusieron; no se
ejecutaron compilación o pruebas.

- Estacion contiene @Column(name = "es_publica", nullable = false) y
  private boolean esPublica = true debajo de borradoEstacion. Se agregan
  isEsPublica y setEsPublica antes de getVersion. No hay imports nuevos.
- Se conservaron íntegros ambos constructores, ID, versión, relaciones,
  validaciones, otros accesores, equals y hashCode. El valor inicial solo
  se aplica a objetos nuevos: no es una migración de datos o DEFAULT SQL.
- La comparación del archivo excluyendo exactamente los dos bloques
  agregados coincidió con su contenido inicial. Los hashes de todo src
  confirmaron un solo archivo modificado, Estacion.java, sin altas o bajas;
  la comparación del diff cached confirmó staging preservado.
- Solo se actualizó además este diario. No se modificaron DTO, servicio,
  consultas, controladores, pruebas, frontend, DER, configuración, commits,
  base local o permisos; no hubo SQL, Gradle, navegador o reinicios.
- La parte 3 queda a cargo del usuario con el comando filtrado a
  EstacionControllerTest y EstacionServiceTest. No se debe reiniciar ni
  crear todavía Taller: faltan DTO, consultas y validaciones del circuito
  público/interno. Este campo aislado no acredita implementar RN-INV-01.

### Etapa 33 Pruebas de entidad verificadas y DTO de salida propuesto

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó el comando filtrado a EstacionControllerTest
y EstacionServiceTest con BUILD SUCCESSFUL en 8 s, cuatro tareas ejecutadas.
Se leyeron los XML: controlador 9 tests y servicio 5; ambos sin errores,
fallos u omitidos, en la ejecución del 6 de noviembre de 2026.

- Se releyeron el nuevo atributo y accesores de Estacion, EstacionDTO y
  la prueba de respuesta válida del controlador. Las catorce pruebas previas
  siguen pasando, pero todavía no comprueban la clasificación en el DTO.
- Se propone un bloque manual para EstacionDTO: atributo boolean esPublica,
  asignación desde estacion.isEsPublica() en el constructor y getter
  isEsPublica. No se agregaría setter al DTO de salida ni se modificarían
  capacidad, ocupación, disponibilidad o versión.
- En EstacionControllerTest se propone ampliar la respuesta pública válida
  con jsonPath esPublica=true y agregar una prueba de respuesta con una
  entidad interna esPublica=false. Se reutilizarían los mocks y helpers
  existentes, sin imports, clases o dependencias adicionales. El total
  esperado al ejecutar ambos archivos sería quince pruebas.
- Estos casos solo comprobarán serialización y copia de la clasificación;
  no demuestran filtrado o autorización de clientes. La edición no expone
  todavía ese indicador como campo mutable en ActualizarEstacionDTO.
- El asistente solo actualizó este diario. No se modificaron DTO o tests,
  otras fuentes, configuración, DER, staging, commits, datos o permisos;
  no se ejecutaron nuevas pruebas, Gradle, SQL, navegador o reinicios.
  Se mantiene la instrucción de no reiniciar ni crear Taller hasta conectar
  consultas, entradas y validaciones de ubicaciones internas.

### Etapa 34 esPublica agregado al DTO y comprobaciones de respuesta preparadas

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó al asistente a implementar únicamente los puntos
1 y 2 del bloque propuesto en la etapa 33, reservándose la parte 3 de guardar
y comprobar. Se realizaron los cambios acordados sin compilar ni ejecutar
pruebas.

- EstacionDTO incorpora private boolean esPublica, copia el valor desde
  estacion.isEsPublica() en su constructor y expone isEsPublica. No se agregó
  setter. Se conservaron ID, versión y todos los cálculos de disponibilidad,
  ocupación y espacios, sin modificar el DTO de entrada o la clasificación.
- EstacionControllerTest agrega la comprobación jsonPath esPublica=true a
  la respuesta válida existente y una prueba devuelveEsPublicaFalseParaUnaEstacionInterna
  para una respuesta con esPublica=false. Reutiliza mocks, imports y helpers
  existentes. El Taller de ese caso es un objeto en memoria, no una fila real.
- Se comparó el contenido con el inicio, excluyendo exactamente los bloques
  agregados: el resto de EstacionDTO y los tests/helpers anteriores coincidió.
  Los hashes de todo src confirmaron solo esos dos archivos modificados,
  sin altas o bajas de fuentes. El diff staged se conservó sin cambios.
- Hay diez métodos @Test en el controlador y cinco en el servicio. El total
  esperado de la siguiente ejecución filtrada es quince, todavía pendiente
  de comprobar. Los nuevos casos comprueban copia/serialización, no filtrado,
  autorización de clientes o persistencia de la clasificación en MySQL.
- Solo se actualizó además este diario. No se modificaron entidad, servicio,
  repositorios, controlador de producción, frontend, DTO de entrada, DER,
  configuración, staging, commits, base local o permisos. No hubo Gradle,
  SQL, navegador o reinicios. El usuario conserva la parte 3 y se mantiene
  la instrucción de no reiniciar la app ni crear todavía un Taller real.

### Etapa 35 Quince pruebas verificadas y consulta pública propuesta

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó el comando filtrado a controlador y servicio con
BUILD SUCCESSFUL en 4 s, tres tareas ejecutadas y una actualizada. Se leyeron
los XML: EstacionControllerTest tiene 10 pruebas y EstacionServiceTest 5;
ambos con failures=0, errors=0 y skipped=0 en la ejecución del 6 de noviembre.

- Queda verificada la regresión anterior y la serialización de esPublica
  tanto true como false. El resultado no demuestra aún consultas filtradas,
  seguridad, clasificación persistida o implementación de Taller en MySQL.
- Se releyeron EstacionRepository y los GET del controlador. El listado
  actual todavía utiliza findByBorradoEstacionFalse, sin separar internas.
- El siguiente bloque manual propuesto agrega solo
  findByBorradoEstacionFalseAndEsPublicaTrue, devolviendo List<Estacion>,
  junto a la consulta existente. No requiere imports nuevos. Se conserva
  findByBorradoEstacionFalse para la futura consulta administrativa que
  incluirá ubicaciones internas activas.
- Después se conectarán las rutas de lectura pública y administrativa y
  los consumidores correspondientes; la mera declaración del método no
  cambia todavía el endpoint ni impide operar con Taller. Los controles de
  alquiler/devolución seguirán pendientes de implementación y prueba.
- Solo se actualizó este diario. El asistente no agregó consultas o fuentes,
  no modificó configuración, DER, staging, commits, datos o permisos ni
  ejecutó pruebas, Gradle, SQL, navegador o reinicios. La compilación del
  siguiente cambio queda a cargo del usuario y no sustituye validar la
  consulta derivada en un contexto JPA real. No reiniciar aún ni crear Taller.

### Etapa 36 Consulta pública revisada y separación de lecturas propuesta

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó compileJava con BUILD SUCCESSFUL en 1 s, una
tarea ejecutada. Se releyó EstacionRepository: conserva la consulta general
y agrega correctamente findByBorradoEstacionFalseAndEsPublicaTrue. No se
ejecutó una nueva compilación o consulta real de ese método.

- Se revisaron EstacionService, los consumidores /api/estaciones, el prefijo
  del controlador estadístico y las reglas de WebAuthorization. El siguiente
  bloque manual propuesto afecta servicio, controlador de estaciones y
  únicamente cargarEstaciones del JS administrativo.
- Se proponen dos lecturas en EstacionService, con transacción readOnly
  para construir los DTO mientras sus relaciones están disponibles:
  obtenerEstacionesPublicas con el nuevo método filtrado, y
  obtenerEstacionesAdministracion con la consulta general y @PreAuthorize
  ADMIN. Se conservan el constructor y actualizarEstacion sin cambios.
- El GET /api/estaciones delegará en la lectura pública. Se agregará
  GET /api/admin/estaciones dentro de EstacionController, sin alterar los
  otros endpoints. WebAuthorization ya protege /api/admin/**; no es necesario
  modificar el controlador de estadísticas o las reglas de seguridad.
- Solo cargarEstaciones de admin.js cambiará su GET al endpoint de
  Administración. Se conservarán el GET público usado para elegir una
  devolución normal, el mapa, alquiler y mis viajes, y POST/PUT/DELETE.
- El usuario ejecutará las quince pruebas existentes como regresión.
  Todavía deberán agregarse pruebas de las nuevas lecturas y permisos;
  los flujos de alquiler/devolución y consultas de bicicletas requieren
  sus controles propios. No basta separar estos GET para cerrar RN-INV-01.
- Solo se actualizó este diario. No se implementaron esos métodos o rutas
  ni se modificaron fuentes, configuración, DER, staging, commits, datos o
  permisos. No hubo Gradle, SQL, navegador o reinicios. Se mantiene la
  instrucción de no reiniciar ni crear Taller antes de conectar el circuito.

### Etapa 37 Lecturas pública y administrativa conectadas en el código

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó al asistente a implementar exclusivamente los
puntos 1 a 3 de la etapa 36, reservándose la parte 4 de guardar y comprobar.
Se aplicaron los bloques propuestos sin ejecutar compilación o pruebas.

- EstacionService incorpora imports List y Collectors y los métodos
  obtenerEstacionesPublicas y obtenerEstacionesAdministracion, ambos con
  transacción readOnly para preparar DTO dentro del contexto de persistencia.
  La lectura pública utiliza el filtro nuevo; la administrativa conserva
  todas las ubicaciones sin baja y exige ADMIN mediante @PreAuthorize.
- El GET /api/estaciones delega en el servicio público. Se agrega
  GET /api/admin/estaciones dentro de EstacionController para la lectura
  administrativa. No se modificó WebAuthorization: /api/admin/** ya exige
  ADMIN. Tampoco se modificó el controlador de estadísticas.
- Solo cargarEstaciones de admin.js cambia su GET al endpoint administrativo.
  Se conserva la consulta pública de selección de devolución en la función
  de finalizar alquiler, y todas las llamadas de escritura y demás funciones.
- La comparación del contenido excluyendo los bloques propuestos confirmó
  el constructor y actualizarEstacion intactos; imports/inyección y GET por
  ID, POST, PUT y DELETE del controlador intactos; y una sola sustitución de
  URL en el JS. Los hashes de todo src confirmaron tres archivos modificados,
  sin altas o bajas, y el diff staged permaneció igual.
- Solo se actualizó además este diario. No se cambiaron modelos, DTO,
  repositorios, pruebas, otros archivos frontend, DER, configuración,
  commits, datos o permisos. No hubo Gradle, ejecución de tests, SQL,
  navegador o reinicios; git diff --check no señaló errores de contenido.
- El usuario conserva la ejecución de las quince pruebas existentes como
  regresión. Nuevas pruebas de lecturas/permisos, consultas de bicicletas
  y validaciones de alquiler/devolución siguen pendientes. No se declara
  terminado RN-INV-01 ni verificado este incremento en ejecución real.
- No reiniciar ni crear Taller todavía. Antes de recargar el panel se debe
  coordinar el arranque del backend actualizado: una instancia anterior no
  conoce /api/admin/estaciones y podría responder 404 al JS nuevo. Este
  paso prepara los archivos para ese arranque, no actualiza Java en memoria.

### Etapa 38 Regresión de lecturas verificada y cuatro pruebas propuestas

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó el comando filtrado con BUILD SUCCESSFUL en 4 s,
cuatro tareas ejecutadas. Los XML actuales muestran 10 pruebas del controlador
y 5 del servicio, sin fallos, errores u omitidas, en la ejecución del
6 de noviembre de 2026. Se releyeron ambos archivos de pruebas y el diario.

- El siguiente bloque manual propuesto agrega dos pruebas a EstacionServiceTest:
  la lectura pública usa la consulta filtrada y no la general; la lectura
  administrativa usa la general y conserva la clasificación interna en el
  DTO. Los repositorios seguirán simulados; no es una prueba del predicado
  SQL, del estado de baja de filas reales o de sus anotaciones AOP.
- Se proponen dos pruebas GET en EstacionControllerTest con MockMvc standalone:
  /api/estaciones delega a la lectura pública y /api/admin/estaciones devuelve
  el listado administrativo del servicio simulado. Se agrega un pequeño
  helper reutilizado para construir DTO públicos e internos en memoria.
- Se requieren únicamente java.util.List en ambos archivos y el import
  estático get en el controlador de pruebas. Se conservan todos los tests
  anteriores. El total esperado después del cambio sería 19, no ejecutados
  todavía: 12 del controlador y 7 del servicio.
- Estos casos no incorporan filtros de Spring Security ni contexto JPA:
  no acreditan permisos efectivos o consultas en MySQL. Esas comprobaciones
  y las validaciones de alquiler/devolución continúan pendientes.
- Solo se actualizó este diario. No se agregaron pruebas o código, ni se
  modificaron DER, configuración, staging, commits, datos o permisos. No
  se ejecutó Gradle, SQL, navegador o reinicios. Se mantiene el trabajo con
  pruebas aisladas antes de recargar o reiniciar el entorno funcional.

### Etapa 39 Cuatro pruebas de lecturas incorporadas sin ejecutarlas

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó al asistente a implementar únicamente los
puntos 1 y 2 propuestos en la etapa 38, reservándose la parte 3 de guardar
y comprobar. Se agregaron exactamente las cuatro pruebas acordadas.

- EstacionServiceTest incorpora import List y dos pruebas: la lectura
  pública usa findByBorradoEstacionFalseAndEsPublicaTrue sin usar la general;
  la administrativa usa la general y conserva un DTO interno esPublica=false.
  En ambos casos se verifica que no interviene BicicletaRepository.
- EstacionControllerTest incorpora imports List y get, dos pruebas de los
  GET público y administrativo, y estacionParaListado como helper compartido
  para preparar DTO en memoria. Comprueban respuestas y delegación al método
  de servicio correspondiente, sin arrancar la app o crear registros reales.
- La comparación de contenido, excluyendo exactamente los bloques e imports
  agregados, coincidió con los tests y helpers anteriores. Los hashes de
  todo src confirmaron solo esos dos archivos de pruebas modificados, sin
  altas o bajas; se conservó el diff staged del usuario sin cambios.
- Hay doce métodos @Test del controlador y siete del servicio. El total
  esperado en la ejecución filtrada es diecinueve, pendiente de ejecutar
  por el usuario. No se presenta la revisión estática como prueba superada.
- No se cargan filtros de seguridad o repositorios JPA reales: estos casos
  no acreditan permisos, el predicado SQL, transacciones o concurrencia real.
  Las comprobaciones específicas de seguridad y del circuito de alquiler
  siguen pendientes, conforme al alcance de RN-INV-01.
- Solo se actualizó además este diario. No se modificaron código de
  producción, pruebas anteriores, frontend, DER, configuración, staging,
  commits, datos o permisos, ni se ejecutaron Gradle, SQL, navegador o
  reinicios. El usuario conserva la parte 3; no recargar o reiniciar aún.

### Etapa 40 Diecinueve pruebas verificadas y seguridad de lecturas pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó el comando filtrado con BUILD SUCCESSFUL en 3 s,
dos tareas ejecutadas y dos actualizadas. Se verificaron los XML de la
ejecución actual: EstacionControllerTest 12 pruebas y EstacionServiceTest 7,
ambos sin fallos, errores u omitidas. Las cuatro pruebas nuevas pasaron.

- Quedan comprobadas en forma aislada la selección de consulta del servicio
  y las rutas/respuestas de lectura pública y administrativa. No se presenta
  este resultado como prueba de los permisos o del predicado SQL real.
- Se releyó WebAuthorizationTest: usa @WebMvcTest y las configuraciones
  de seguridad reales con dependencias simuladas; tiene nueve pruebas
  previas y no registra todavía EstacionController en ese contexto.
- El siguiente bloque propuesto es ampliar ese mismo test, sin modificar
  seguridad productiva: registrar también EstacionController y simular
  EstacionService, EstacionRepository y BicicletaRepository. El contexto
  seguirá siendo de MVC/seguridad, sin repositorios JPA o MySQL reales.
- Se proponen tres pruebas nuevas: GET público anónimo 200, GET administrativo
  anónimo 401 y GET administrativo con ADMIN 200. Se agregará la ruta
  /api/admin/estaciones al helper existente que comprueba 403 para CLIENTE
  y TECNICO, verificando que no llegue a invocarse el servicio de estaciones.
  Esto permite comprobar cinco escenarios sin duplicar las dos pruebas
  de roles que ya existen. El total previsto de los tres archivos sería
  31 pruebas, pendiente de implementar y ejecutar: 12 + 7 + 12.
- Estos tests comprobarán los filtros HTTP con los controladores registrados;
  no la transacción, el predicado JPA o el proxy de servicio de una ejecución
  completa con MySQL. Alquiler/devolución seguirán necesitando sus controles.
- Solo se actualizó este diario. No se modificaron tests, fuentes, DER,
  configuración, staging, commits, datos o permisos, ni se ejecutaron
  Gradle, SQL, navegador o reinicios. No recargar/reiniciar el panel todavía.

### Etapa 41 Acceso técnico a la ubicación aprobado y siguiente bloque manual

Fecha: 6 de noviembre de 2026.

Estado: el usuario aprobó la separación entre administración de estaciones
y consulta técnica de la ubicación en las órdenes asignadas. Se asentó en
RN-MAN-02 y RN-INV-01: TECNICO podrá conocer el Taller de la bicicleta desde
su orden, pero no acceder al listado administrativo completo ni modificar
estaciones por ese permiso. ADMIN mantiene la gestión y CLIENTE/visitante
solo el listado público. La consulta técnica se implementará con mantenimiento;
no se crea un endpoint provisional ni se declara implementado ese módulo.

- Se releyó WebAuthorizationTest y se conserva el siguiente bloque manual
  propuesto en la etapa 40: registrar EstacionController y sus dependencias
  simuladas en el slice MVC existente; agregar tres pruebas de acceso y
  ampliar el helper de denegación que usan CLIENTE y TECNICO.
- La ampliación prevista verifica la ruta pública anónima (200), el listado
  administrativo anónimo (401), el acceso administrativo de ADMIN (200)
  y la denegación a CLIENTE/TECNICO (403). No prueba todavía la consulta
  técnica dentro de una orden asignada, que no está implementada.
- Se entregan instrucciones manuales sobre el archivo existente, sin
  reemplazar sus pruebas anteriores. Comando filtrado propuesto para el
  usuario: WebAuthorizationTest, EstacionControllerTest y EstacionServiceTest;
  total esperado 31 pruebas, pendiente de realizar los cambios y ejecutar.
- Solo se modificó documentación. No se editaron Java, frontend, DER,
  configuración, staging o commits; no se ejecutaron Gradle, SQL, navegador
  ni reinicios. La base y las fuentes existentes permanecen intactas.

### Etapa 42 Pruebas de acceso a estaciones implementadas, ejecución pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar exclusivamente el bloque
propuesto sobre WebAuthorizationTest y reservó para sí la parte 3, guardar
y comprobar. Se releyó el archivo antes de modificarlo y se aplicó el cambio
sin editar fuentes productivas ni sustituir las pruebas previas.

- Se agregaron los cuatro imports propuestos y EstacionController a
  @WebMvcTest y @ContextConfiguration. Se conservaron UsuarioController,
  WebAuthorization y SecurityConfig en la configuración del contexto.
- Se incorporaron @MockBean de EstacionService, EstacionRepository y
  BicicletaRepository. Estos sustituyen las dependencias del controlador;
  el contexto continúa siendo de MVC/seguridad, sin MySQL ni JPA reales.
- Se agregaron tres pruebas: visitante con lectura pública 200, visitante
  con listado administrativo 401 y ADMIN con listado administrativo 200.
  Los casos permitidos comprueban la llamada al método de consulta correcto;
  el caso anónimo denegado verifica que no se invoca el servicio.
- El helper de rutas administrativas conserva sus tres rutas anteriores y
  añade /api/admin/estaciones. Las pruebas existentes de CLIENTE y TECNICO
  comprueban así su denegación 403 y que no se invocan UsuarioRepository
  ni EstacionService. No se habilita el listado completo para TECNICO.
- La revisión estática confirmó doce @Test en el archivo, las nueve pruebas
  previas con su código íntegro y los helpers de perfil y creación de
  usuario sin cambios. La comparación SHA-256 de todas las fuentes mostró
  únicamente WebAuthorizationTest modificado; no se eliminó ninguna fuente.
  El diff de staging permaneció idéntico y diff --check no informó errores.
- También se actualizó este diario. No se modificaron seguridad productiva,
  controladores, servicios, repositorios, frontend, DER, configuración o
  datos; no se ejecutaron Gradle, SQL, navegador, reinicios ni commits.
- La parte 3 queda para el usuario: ejecutar las tres clases filtradas
  WebAuthorizationTest, EstacionControllerTest y EstacionServiceTest. Se
  esperan 31 pruebas en total, todavía sin acreditar su ejecución tras
  este cambio. No necesita reiniciar la aplicación para estos tests.
- La consulta técnica de ubicaciones dentro de órdenes asignadas conserva
  su condición de diseño aprobado, pendiente del módulo de mantenimiento;
  estas pruebas no la implementan ni verifican transacciones o SQL real.

### Etapa 43 Treinta y una pruebas verificadas y baja de estación pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó las tres clases filtradas y obtuvo BUILD SUCCESSFUL
en 5 s. Se leyeron los XML de esa ejecución: WebAuthorizationTest 12 pruebas,
EstacionControllerTest 12 y EstacionServiceTest 7; total 31, sin fallos,
errores u omitidas. Los reportes corresponden al 6 de noviembre, 23:07 UTC.

- Se acreditan los casos de lectura y los filtros HTTP de acceso descritos
  en las etapas anteriores, con sus dependencias simuladas. No se extiende
  el resultado a SQL real, a la consulta técnica de órdenes aún inexistente,
  a movimientos físicos ni a los controles pendientes de alquiler/devolución.
- Se releyeron los DTO, servicio y controlador de estaciones. Se identificó
  una brecha puntual que ya estaba pendiente: eliminarEstacionLogico usa
  countByUbicacionActualAndBorradoBiciFalse, a diferencia de la actualización
  de capacidad, que cuenta todas las bicicletas físicamente asociadas.
  Podría permitir la baja de una estación que solo conserva archivadas.
- El siguiente bloque propuesto es sustituir ese contador por
  countByUbicacionActual y corregir su comentario, conservando bloqueo,
  respuesta 409, baja lógica y demás comportamiento. Aplica la ocupación
  física ya aprobada en RN-INV-01, sin agregar reglas de negocio.
- La prueba específica de baja con una bicicleta archivada sigue pendiente;
  las 31 pruebas actuales no cubren el DELETE. Esta corrección puntual no
  completaría por sí sola la auditoría y las restantes condiciones v5 de baja.
- Solo se actualizó este diario. No se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. El próximo cambio se propone para implementación manual.

### Etapa 44 Contador de baja corregido y pruebas DELETE propuestas

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó haber realizado el cambio. Se releyó el método
eliminarEstacionLogico y se confirmó countByUbicacionActual(estacion), con
el comentario actualizado. Se conserva la búsqueda activa con bloqueo,
404 si no se encuentra, 409 si existe ocupación y baja lógica con 204
solo cuando el contador devuelve cero. No se ejecutó el endpoint.

- Se releyó EstacionControllerTest: conserva doce pruebas; la configuración
  standalone tiene EstacionService simulado y todavía no declara los dos
  repositorios requeridos por las operaciones DELETE actuales.
- El siguiente bloque manual propuesto agrega esos dos @Mock, los imports
  necesarios y tres pruebas nuevas sin reemplazar las previas: rechazo de
  baja con una bicicleta archivada físicamente asociada; baja exclusivamente
  lógica de una estación vacía; 404 cuando no existe una estación activa.
- La primera conserva una bicicleta archivada en memoria y simula que el
  contador total devuelve uno. Comprueba que se usa ese contador y no el
  filtrado por borrado, que la estación no se marca de baja y que no hay
  guardados ni eliminaciones. No acredita el predicado SQL del repositorio.
- El caso vacío comprueba el indicador, save y ausencia de operaciones
  adicionales sobre los repositorios; el caso ausente no cuenta bicicletas
  ni guarda. Los tests son MVC con repositorios simulados; no prueban el
  bloqueo o una transacción real, ni completan la auditoría v5 pendiente.
- Tras el cambio se esperan quince pruebas de EstacionControllerTest y
  treinta y cuatro junto con WebAuthorizationTest y EstacionServiceTest.
  Implementación y ejecución de ese bloque siguen pendientes del usuario.
- Solo se actualizó el diario. No se editaron fuentes, configuración,
  frontend, DER, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. Se conserva la modalidad de instrucciones manuales.

### Etapa 45 Pruebas DELETE implementadas, comprobación reservada al usuario

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar solamente el bloque
de pruebas propuesto y reservó la parte 3, guardar y comprobar. Se releyeron
EstacionControllerTest y el DELETE actual antes de editar. Se modificó
únicamente ese archivo de pruebas, además de esta anotación en el diario.

- Se agregaron los siete imports propuestos y @Mock de EstacionRepository
  y BicicletaRepository. Se conservan EstacionService simulado, InjectMocks,
  el armado de MockMvc y el manejador global de excepciones anteriores.
- Se agregaron tres pruebas antes de estacionParaListado: 409 con una
  bicicleta archivada físicamente asociada, 204 con baja exclusivamente
  lógica de estación vacía y 404 al no encontrar una estación activa.
- El caso ocupado crea los objetos solo en memoria y simula el conteo
  total en uno; exige countByUbicacionActual y prohíbe el contador que
  excluye archivadas. Comprueba que no se marca de baja la estación y
  que no hay guardados ni otras operaciones de repositorio. Esto no
  acredita el predicado SQL real del contador.
- El caso vacío comprueba borradoEstacion=true y save; la verificación de
  ausencia de interacciones adicionales rechaza una eliminación física.
  El caso ausente no permite contar bicicletas ni guardar datos.
- La comparación estática, retirando solamente las adiciones propuestas,
  recuperó íntegro el contenido anterior: las doce pruebas, helpers,
  imports previos y configuración. Ahora hay quince @Test en el archivo.
  La comparación SHA-256 de todas las fuentes confirmó que solo cambió
  EstacionControllerTest, sin archivos eliminados, y el staging se conservó.
- No se editaron fuentes productivas, seguridad, frontend, DER,
  configuración o datos; no se ejecutaron Gradle, SQL, navegador, reinicios
  ni commits. No se acredita todavía la ejecución de estas nuevas pruebas,
  ni se extiende su alcance a transacciones, bloqueos o auditoría v5.
- El usuario conserva la parte 3: ejecutar WebAuthorizationTest,
  EstacionControllerTest y EstacionServiceTest. Se esperan 34 pruebas en
  total (12 + 15 + 7), pendientes de comprobar tras esta implementación.

### Etapa 46 Treinta y cuatro pruebas verificadas y siguiente alta de estaciones

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó BUILD SUCCESSFUL en 1 s, con las cuatro tareas
UP-TO-DATE. Ese comando reutilizó resultados; no se describe como una
ejecución nueva. Se leyeron los XML disponibles del 6 de noviembre a las
23:16 UTC: WebAuthorizationTest 12, EstacionControllerTest 15 y
EstacionServiceTest 7; total 34, sin fallos, errores u omitidas. Se confirmó
además la presencia de las tres pruebas DELETE nuevas por sus nombres.

- Quedan acreditados los escenarios aislados de DELETE propuestos, con
  repositorios simulados; no la consulta SQL real, las transacciones, el
  bloqueo o la auditoría. No se vuelve a ejecutar Gradle para revisar esto.
- Se revisó el alta actual: crearEstacion todavía recibe la entidad cruda;
  no existe CrearEstacionDTO y el panel no tiene selector de esPublica.
  El próximo bloque propuesto es completar el alta de estaciones/talleres,
  comenzando por ese DTO con los campos permitidos y validación de
  clasificación obligatoria, conservando las reglas de RN-INV-01.
- El recorrido posterior será conectar la creación con EstacionService,
  el controlador y el selector administrativo, y añadir sus pruebas.
  No se propone un cambio libre de clasificación de estaciones existentes
  ni se modifica el DTO de actualización sin definir esos controles.
- Se verificó también que los alquileres actuales aún no rechazan Taller
  como origen/destino en el backend y que bicicletas/disponibles no filtra
  estaciones internas. Esos controles deberán completarse antes de una
  prueba funcional con Taller real. La devolución y las altas/traslados
  actuales de bicicletas también conservan contadores que excluyen
  archivadas y requieren alinearse con la ocupación física aprobada.
- Solo se actualizó este diario. No se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. El siguiente bloque se mantiene propuesto para trabajo manual.

### Etapa 47 Propuesta manual de CrearEstacionDTO

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió continuar. Se contrastaron ActualizarEstacionDTO,
Estacion y RN-INV-01; se confirmó que CrearEstacionDTO aún no existe.
Se entregan instrucciones para que el usuario cree esa clase en el paquete
dto existente, sin editar otros archivos de Java en este paso.

- Contendrá únicamente nombre, direccion, capacidad, latitud, longitud y
  esPublica. No recibe id, version, borradoEstacion ni asociaciones de bicicletas.
- Nombre y dirección conservan las reglas ya usadas al actualizar: no
  blancos, máximo 255 y normalización de espacios en sus setters. Capacidad
  obligatoria y mínima uno; coordenadas obligatorias dentro de sus rangos.
- esPublica es Boolean sin valor predeterminado y @NotNull: omitir la
  clasificación no equivale a elegir Taller. true identifica pública y
  false interna; false es válido. Se proponen getEsPublica/setEsPublica.
- No es una entidad ni crea otra tabla. Las restricciones no estarán
  conectadas al POST hasta adaptar servicio y controlador en el bloque
  siguiente. No se habilita el cambio libre de clasificación al editar.
- Se propone compileJava como comprobación manual, sin iniciar la app o
  usar MySQL. Creación y compilación del DTO pendientes del usuario.
  Solo se actualizó este diario; no se ejecutaron Gradle, SQL o reinicios,
  ni se modificaron fuentes, DER, frontend, configuración, staging o commits.

### Etapa 48 CrearEstacionDTO revisado y alta en servicio propuesta

Fecha: 6 de noviembre de 2026.

Estado: el usuario creó CrearEstacionDTO y reportó compileJava exitoso.
Se leyó la clase completa: contiene los seis campos, sus restricciones,
normalización y accesores propuestos. La diferencia informada está solo
en el mensaje de esPublica, que ahora menciona estación/taller interno;
no altera la validación. Se recomienda la expresión estación de uso
interno como denominación general de talleres/depósitos, sin agregar
tipos ni modificar las condiciones aprobadas para esas ubicaciones.

- Se revisaron EstacionService, EstacionRepository y el POST actual.
  Todavía no existe el método de alta del servicio ni la consulta de
  existencia por nombre sin excluir un identificador.
- El siguiente bloque manual propuesto agrega existsByNombreIgnoreCase
  al repositorio y crearEstacion al servicio, con @Transactional,
  @PreAuthorize ADMIN y @Valid/@NotNull en el DTO de entrada.
- El alta comprueba duplicados incluyendo las estaciones de baja lógica,
  construye una entidad nueva con los datos permitidos, asigna esPublica
  y usa saveAndFlush antes de convertir a EstacionDTO. Identificador,
  versión y baja lógica no se toman de la petición. La consulta previa
  no sustituye la restricción única de la base ni sus errores concurrentes.
- No se modifica todavía el POST ni el formulario; su conexión y manejo
  de errores seguirán en el bloque siguiente. No se habilita cambiar
  libremente la clasificación de una estación existente.
- El código se entrega para implementación manual y compileJava por el
  usuario. Solo se actualizó este diario; no se editaron fuentes, DER,
  frontend o configuración, ni se ejecutaron Gradle, SQL, reinicios o Git.

### Etapa 49 Alta en repositorio y servicio implementada, compilación pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar exclusivamente el
bloque propuesto y reservó la parte 3, guardar y comprobar. Se releyeron
repositorio, servicio y DTO antes de aplicar las adiciones. El DTO ya
contenía el mensaje estación de uso interno y se conservó sin cambios.

- EstacionRepository incorpora existsByNombreIgnoreCase(String nombre)
  después de findByNombre, conservando todos sus métodos anteriores.
- EstacionService importa CrearEstacionDTO e incorpora crearEstacion
  después del constructor, con @Transactional, @PreAuthorize ADMIN y
  @NotNull/@Valid en el parámetro. Comprueba nombre duplicado, construye
  una Estacion nueva con los cinco datos básicos, asigna esPublica,
  persiste con saveAndFlush y devuelve EstacionDTO.
- No se reciben id, version, borradoEstacion ni asociaciones desde el DTO.
  La búsqueda de duplicados no excluye bajas lógicas ni sustituye la
  restricción única de la base frente a concurrencia.
- La revisión estática confirmó que, retirando las adiciones propuestas,
  el contenido anterior de ambos archivos queda íntegro. La comparación
  SHA-256 de todas las fuentes mostró únicamente los dos archivos
  autorizados modificados, ninguno eliminado, y el staging sin cambios.
- Se actualizó además este diario. No se modificaron DTO, controlador,
  formulario, pruebas, seguridad, DER, configuración o datos; no se
  ejecutaron Gradle, SQL, navegador, reinicios ni commits.
- La parte 3 queda para el usuario con compileJava. No se acredita aún
  la compilación o ejecución de este método; el POST continúa usando
  su implementación anterior hasta conectarlo en el próximo bloque.

### Etapa 50 Compilación del alta confirmada y conexión POST propuesta

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó compileJava exitoso en 1 s, con la tarea
ejecutada. Se releyeron servicio y controlador y se confirmó la presencia
de las clases compiladas de EstacionService y EstacionRepository con
fecha de modificación 6 de noviembre, 20:29:58 local. No se ejecutaron
pruebas o compilación por parte del asistente.

- Se revisaron GlobalExceptionHandler y guardarEstacion del frontend:
  la validación MVC ya devuelve validationErrors con 400 y el formulario
  sabe mostrar esa lista o un mensaje de texto. No hay todavía selector
  de esPublica en el alta administrativa.
- El próximo bloque manual propuesto agrega el import CrearEstacionDTO
  al controlador y reemplaza exclusivamente crearEstacion: recibe el DTO
  con @Valid, delega al servicio y devuelve 201. Los duplicados lógicos
  y conflictos de integridad se traducen a 409, sin exponer detalles SQL;
  las violaciones de validación del servicio se traducen a 400. No se
  captura AccessDeniedException ni se agrega transacción al controlador.
- Los demás métodos y dependencias del controlador se conservan, porque
  GET por id y DELETE todavía requieren la entidad y los repositorios.
- Después será necesario conectar la clasificación al formulario de alta.
  Hasta entonces el formulario anterior omite esPublica y la nueva versión
  del POST rechazaría el alta con 400; no se considera funcionalmente
  cerrado este recorrido ni se propone reiniciar/probar el panel todavía.
- Solo se actualizó este diario; no se editaron fuentes, frontend, DER,
  configuración, staging, commits o datos, ni se ejecutaron Gradle, SQL,
  navegador o reinicios. El siguiente cambio queda para el usuario.

### Etapa 51 POST de estaciones conectado al servicio, compilación pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar exclusivamente la
conexión propuesta y reservó la parte 3, guardar y comprobar. Se releyó
EstacionController antes de editar y se conservaron sus cambios anteriores.

- Se agregó el import CrearEstacionDTO y se reemplazó solamente el método
  crearEstacion. Recibe @Valid @RequestBody CrearEstacionDTO, delega en
  estacionService.crearEstacion y devuelve 201 con el DTO de respuesta.
- El controlador ya no comprueba duplicados ni persiste directamente
  durante el alta. Traduce ConstraintViolationException del servicio a
  400, IllegalStateException a 409 y DataIntegrityViolationException a
  409 con texto seguro. La validación MVC sigue usando el manejador global.
  No se capturan denegaciones de acceso ni se agrega transacción al POST.
- La comparación estática confirmó que, excluyendo ese método y el nuevo
  import, todo el contenido anterior del controlador permanece idéntico.
  La comparación SHA-256 de las fuentes mostró únicamente ese archivo
  modificado, ninguno eliminado y el staging sin cambios. diff --check
  no informó errores.
- Se actualizó además este diario. DTO, servicio, repositorios, métodos
  GET/PUT/DELETE, frontend, pruebas, seguridad, DER y configuración no se
  modificaron. No se ejecutaron Gradle, SQL, navegador, reinicios o commits.
- La parte 3 queda para el usuario: compileJava. No se acredita aún su
  compilación ni la ejecución del POST después del cambio. El selector
  público/interno del formulario continúa pendiente; no reiniciar ni
  probar el alta desde el panel hasta conectar ese dato obligatorio.

### Etapa 52 Compilación del POST y selector de uso propuesto

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó compileJava exitoso en 1 s, tarea ejecutada.
Se confirmó la clase compilada del controlador con fecha de modificación
6 de noviembre, 20:34:49 local. Se leyeron el modal de estaciones y las
funciones actuales abrirModalEstacion, editarEstacion y guardarEstacion.

- Se propone agregar al modal un selector Uso de la estación, con opción
  vacía, Pública (true) y Uso interno (false), usando los estilos existentes.
  Un texto aclara que al editar solo se muestra, sin permitir cambios.
- En el alta se limpia y habilita; en la edición refleja esPublica y se
  bloquea. Si el dato no fuera booleano, no se inventa una clasificación.
- guardarEstacion validará la selección y enviará esPublica como booleano
  real únicamente en POST. Se conservan el control de version en PUT,
  los demás campos, endpoints y mensajes existentes. No se altera el
  DTO de actualización ni se permite cambiar libremente la clasificación.
- Es un bloque manual propuesto sobre admin.html y admin.js. Todavía no
  se editaron esos archivos ni se considera probado el alta. compileJava
  no verifica HTML/JavaScript; no se propone usarlo como evidencia de
  validación de estos cambios visuales.
- El siguiente paso será comprobar el alta y sus validaciones mediante
  pruebas aisladas. Antes de una prueba funcional con ubicaciones internas
  reales siguen pendientes los controles de alquiler/devolución y ofertas
  de bicicletas; no se propone reiniciar todavía el perfil create-drop.
- Solo se actualizó este diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios.

### Etapa 53 Selector de uso implementado, comprobación reservada al usuario

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar solamente el selector
y las tres incorporaciones JavaScript propuestas, reservando la parte 3.
Se releyeron los bloques antes de editar y se modificaron admin.html y
admin.js, además de esta anotación del diario.

- El modal de estaciones incorpora Uso de la estación después de capacidad,
  con opción vacía, Pública y Uso interno, label asociado y estilos existentes.
  Se aclara que en edición el dato se muestra sin permitir cambios.
- abrirModalEstacion limpia y habilita el selector; editarEstacion carga
  el booleano existente, sin inventarlo si no viene, y deshabilita el campo.
- guardarEstacion exige una selección válida únicamente al crear y envía
  esPublica como booleano real en POST. Conserva el control de version
  en PUT, demás campos, peticiones, mensajes y operaciones anteriores.
- La comparación estática recuperó íntegro el contenido previo de ambos
  archivos al retirar exclusivamente las adiciones propuestas; el selector
  tiene identificador único coincidente con sus referencias JavaScript.
  La comparación SHA-256 de todas las fuentes confirmó que solo cambiaron
  estos dos archivos, ninguno eliminado, y el staging permaneció idéntico.
- No se modificaron backend, pruebas, DER, configuración, datos o estilos
  CSS; no se ejecutaron Gradle, JavaScript, SQL, navegador, reinicios o commits.
- Se comprobó que existe el ejecutable Node del runtime local para ofrecer
  al usuario una comprobación --check de admin.js sin ejecutar el archivo
  ni abrir la aplicación. Su ejecución y resultado quedan pendientes del
  usuario. No comprueba por sí sola el HTML, comportamiento visual o alta.
- Continúan pendientes las pruebas aisladas del alta y los controles de
  alquiler/devolución y oferta de bicicletas internas antes de una prueba
  funcional con ubicaciones internas reales. No reiniciar todavía.

### Etapa 54 Sintaxis JavaScript comprobada por el usuario y pruebas de alta propuestas

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó Node --check sobre admin.js y mostró el retorno
al prompt sin errores. Se registra como comprobación de sintaxis, no como
prueba de DOM, HTML, peticiones HTTP, seguridad o persistencia.

- Se releyeron EstacionServiceTest, EstacionDTO y crearEstacion del servicio.
  La clase de pruebas conserva siete casos y aún no cubre el alta.
- El siguiente bloque manual propuesto añade el import CrearEstacionDTO,
  tres pruebas y dos helpers, sin reemplazar casos anteriores: alta pública,
  alta interna y rechazo de nombre duplicado sin persistir.
- Los casos válidos comprobarán nombre/dirección normalizados, datos
  permitidos y clasificación, con entidad inicialmente sin id/versión y
  sin baja lógica. saveAndFlush simulado asignará id y versión para
  comprobar que el DTO de respuesta usa el resultado del guardado.
- El rechazo verificará mensaje y ausencia de persistencia. Son pruebas
  unitarias con Mockito: no prueban el índice SQL, búsqueda real sin
  distinguir mayúsculas, transacción, validación por proxy ni permisos.
  La validación HTTP del DTO y los permisos de POST siguen pendientes.
- Se esperan diez pruebas de servicio y treinta y siete con las otras
  dos clases filtradas. Código y ejecución de este bloque pendientes del
  usuario. No se ejecutan pruebas o reinicios desde el asistente.
- Solo se actualizó este diario; fuentes, frontend, configuración, DER,
  staging, commits y datos permanecen sin modificaciones en este turno.

### Etapa 55 Pruebas unitarias del alta implementadas, ejecución pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar solamente las pruebas
de alta propuestas y reservó para sí guardar y comprobar. Se releyeron
EstacionServiceTest, CrearEstacionDTO y crearEstacion antes de editar.

- Se agregó el import CrearEstacionDTO y tres @Test: creaEstacionPublica,
  creaEstacionDeUsoInterno y rechazaAltaConNombreDuplicadoSinGuardar.
  Dos helpers nuevos construyen los datos de alta y comparten las
  comprobaciones de los dos recorridos válidos, sin tocar helpers anteriores.
- Los casos válidos comprueban normalización de nombre/dirección, campos
  básicos, conservación de esPublica true/false y ausencia inicial de
  id asignado, version, baja lógica y bicicletas asociadas. El guardado
  simulado asigna id y version, y se comprueba su reflejo en EstacionDTO,
  con disponibilidad cero y capacidad libre completa.
- El duplicado exige el mensaje previsto y comprueba que no se guarda ni
  se realizan operaciones adicionales. Ningún caso consulta bicicletas.
- La revisión estática, retirando solo las adiciones propuestas, recuperó
  íntegro el archivo anterior con sus siete pruebas, helpers y preparación.
  Ahora hay diez @Test. La comparación SHA-256 de las fuentes confirmó que
  solo cambió EstacionServiceTest, sin eliminaciones y sin cambios de staging.
- Se actualizó además este diario. No se modificaron código productivo,
  otros tests, frontend, DER, configuración o datos; no se ejecutaron
  Gradle, JavaScript, SQL, navegador, reinicios o commits.
- El usuario conserva la comprobación mediante el comando filtrado de
  WebAuthorizationTest, EstacionControllerTest y EstacionServiceTest.
  Total esperado 37 pruebas (12 + 15 + 10), pendiente de ejecutar.
- Son tests unitarios con repositorios simulados: no acreditan validación
  por proxy, autorización de métodos, búsqueda SQL real o índice único,
  transacción ni persistencia. Siguen las pruebas HTTP del alta.

### Etapa 56 Treinta y siete pruebas verificadas y validación HTTP del alta pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó el comando filtrado con BUILD SUCCESSFUL en 8 s,
tres tareas ejecutadas y una actualizada. Se leyeron los XML de esa
ejecución, del 6 de noviembre a las 23:56 UTC: WebAuthorizationTest 12,
EstacionControllerTest 15 y EstacionServiceTest 10; total 37, sin fallos,
errores u omitidas. Las tres pruebas nuevas del alta figuran por nombre
y pasaron; se conserva el alcance unitario descrito en la etapa anterior.

- Se revisaron el test del controlador, el POST actual y CrearEstacionDTO.
  El slice standalone ya dispone del manejador global y servicio simulado,
  pero todavía no tiene pruebas POST. Los repositorios se conservan como
  mocks para comprobar que el alta no accede directamente a ellos.
- El siguiente bloque propuesto es ampliar exclusivamente ese archivo de
  pruebas: respuestas 201 para clasificación pública e interna, recepción
  de datos normalizados y booleano false; 400 ante campos obligatorios
  ausentes, datos inválidos o cuerpo ilegible; 409 ante duplicado y
  conflicto de integridad sin exponer detalles SQL.
- Se aprovecharán los helpers existentes cuando corresponda, sin sustituir
  las quince pruebas anteriores ni cambiar código productivo. Son pruebas
  de MVC y validación con servicio simulado: no prueban SQL, transacciones,
  autorización real del método o filtros HTTP, que tienen su bloque propio.
- Solo se actualizó este diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. La ampliación del test queda pendiente de implementación.

### Etapa 57 Instrucciones manuales para siete pruebas HTTP del alta

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió continuar. Se releyeron el encabezado y los
helpers de EstacionControllerTest y el POST productivo. Se proponen dos
imports, siete pruebas y tres helpers nuevos en ese único archivo,
conservando sus quince pruebas y helpers actuales.

- Dos casos 201, pública e interna, comparten una comprobación de respuesta
  y captura del CrearEstacionDTO normalizado, incluyendo false válido.
- Los obligatorios se prueban ausentes y nulos, incluida la clasificación.
  Se prueban blancos, exceso de longitud, capacidad no positiva y ambos
  extremos inválidos de coordenadas, esperando validationErrors con 400.
- Cuerpo ausente, null, JSON malformado y tipos incompatibles devuelven
  400 sin llegar al servicio. Los rechazos de entrada no usan repositorios.
- Duplicado e integridad simulados devuelven 409; la integridad devuelve
  texto seguro sin detalles SQL. La creación delega en el servicio y no
  consulta directamente a los repositorios del controlador.
- Los nuevos helpers generan un mapa de alta independiente del PUT,
  envían POST y comparten las comprobaciones válidas. Se reutiliza
  estacionParaListado para el DTO simulado de respuesta.
- Total previsto tras implementar: 22 pruebas del controlador y 44 junto
  con WebAuthorizationTest (12) y EstacionServiceTest (10). No se acreditan
  ejecución, filtros de seguridad, SQL o transacciones con esta propuesta.
- Solo se actualizó el diario; no se editaron Java, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. Implementación y comprobación pendientes del usuario.

### Etapa 58 Siete pruebas HTTP del alta implementadas, ejecución pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar exclusivamente el
bloque propuesto, reservando la parte 3 de guardar y comprobar. Se
releyeron los puntos de inserción y el POST actual antes de editar.

- EstacionControllerTest incorpora los imports CrearEstacionDTO y post,
  siete @Test y los tres helpers datosAltaValidos, enviarAlta y
  comprobarAlta201. Se reutiliza estacionParaListado sin modificarlo.
- Los dos recorridos válidos comprueban 201, respuesta y DTO recibido
  normalizado, con todos sus campos y true/false conservados. El caso
  interno verifica que false no se interpreta como dato ausente.
- Se prueban doce combinaciones de campos obligatorios ausentes/nulos,
  diez valores inválidos y siete cuerpos/tipos ilegibles. Los errores
  de restricciones esperan 400 con validationErrors del campo, y los
  ilegibles 400; en ambos casos no se invoca servicio ni repositorios.
- Los dos conflictos simulados esperan 409: el duplicado conserva el
  mensaje del servicio y el de integridad devuelve el texto seguro
  previsto, sin detalles SQL. El POST delega en el servicio y no usa
  directamente los repositorios.
- La comparación estática, retirando únicamente las adiciones propuestas,
  recuperó íntegro el archivo anterior con sus quince pruebas, helpers
  y configuración. Ahora hay 22 @Test. La comparación SHA-256 de todas
  las fuentes confirmó únicamente EstacionControllerTest modificado,
  ningún archivo eliminado y staging sin cambios. diff --check sin errores.
- Se actualizó además este diario. No se modificaron código productivo,
  otros tests, frontend, DER, configuración o datos; no se ejecutaron
  Gradle, JavaScript, SQL, navegador, reinicios o commits.
- El usuario conserva la parte 3 mediante las tres clases filtradas:
  WebAuthorizationTest, EstacionControllerTest y EstacionServiceTest.
  Total esperado 44 pruebas (12 + 22 + 10), aún pendiente de ejecutar.
  Estos tests no acreditan filtros de seguridad, SQL o transacciones reales.

### Etapa 59 Cuarenta y cuatro pruebas verificadas y permisos POST pendientes

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó el comando filtrado y obtuvo BUILD SUCCESSFUL
en 7 s, con dos tareas ejecutadas y dos actualizadas. Se leyeron los XML
de la ejecución del 7 de noviembre a las 00:15 UTC, correspondiente al
6 de noviembre a las 21:15 en Argentina: WebAuthorizationTest 12,
EstacionControllerTest 22 y EstacionServiceTest 10; total 44, sin fallos,
errores u omitidas. Se confirmaron por nombre los siete casos POST nuevos.

- Quedan acreditados los escenarios MVC/validación descritos en la etapa
  anterior con el servicio simulado. No se presentan como prueba de SQL,
  persistencia, transacciones, navegador o autorización de métodos reales.
- Se releyeron WebAuthorizationTest y las reglas de WebAuthorization:
  POST /api/estaciones queda bajo la regla administrativa existente.
  El test de seguridad actual cubre lecturas, pero no esa operación POST.
- El próximo bloque propuesto amplía únicamente WebAuthorizationTest:
  dos pruebas nuevas, visitante 401 y ADMIN 201 con cuerpo válido y
  servicio simulado; el helper actual de rutas bloqueadas agregará el
  mismo POST para las pruebas ya existentes de CLIENTE y TECNICO (403),
  comprobando que no se invoca el servicio en los rechazos.
- Se prevén catorce pruebas de seguridad y 46 entre las tres clases,
  pendiente de implementar y ejecutar. No se modifican las reglas
  productivas ni se otorga administración de estaciones a TECNICO.
- Solo se actualizó el diario. No se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. Continúan pendientes las restricciones del circuito de
  alquiler/devolución y oferta de bicicletas internas antes de QA real.

### Etapa 60 Instrucciones manuales para permisos POST de estaciones

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió continuar. Se leyó WebAuthorizationTest completo
y se contrastó con la regla administrativa POST existente. Se entregan
instrucciones para modificar únicamente el archivo de pruebas.

- Agregar tres imports de DTO/entidad y una constante JSON válida con
  los seis campos del alta, compatible con Java 11 sin text blocks.
- Agregar dos pruebas: visitante sin sesión recibe 401 y ADMIN recibe
  201 con el DTO simulado por el servicio. La estación de prueba es un
  objeto en memoria con id/version simulados, no una inserción SQL.
- Ampliar comprobarRutasAdministrativasBloqueadas conservando sus cuatro
  rutas GET y añadiendo POST /api/estaciones con el mismo JSON válido.
  Las pruebas existentes de CLIENTE y TECNICO comprobarán 403 y que
  servicio y repositorios no reciben llamadas en esos rechazos.
- Se conservan los doce @Test anteriores, anotaciones, contexto y mocks.
  Total previsto catorce en WebAuthorizationTest y 46 entre las tres
  clases filtradas (14 + 22 + 10), pendiente de implementar y ejecutar.
- El alcance son los filtros HTTP reales con controlador real y servicio
  simulado; no SQL, persistencia, transacciones o autorización del método
  de servicio por su proxy. No se cambian reglas productivas ni permisos.
- Solo se actualizó el diario; no se editaron Java, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. La implementación sigue a cargo del usuario en este paso.

### Etapa 61 Pruebas de permisos POST implementadas, ejecución pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar exclusivamente el
bloque propuesto y reservó para sí la parte 3. Se releyó el archivo antes
de editar; solo se modificó WebAuthorizationTest y este diario.

- Se agregaron los tres imports de DTO/entidad y ALTA_ESTACION_JSON con
  los seis campos válidos del alta, usando cadenas compatibles con Java 11.
- Se agregaron visitanteNoPuedeCrearEstaciones (401, sin invocar servicio
  o repositorios) y administradorPuedeCrearEstaciones (201). El caso ADMIN
  usa una estación en memoria con id/version simulados, comprueba respuesta
  y delegación única al servicio, y prohíbe llamadas a los repositorios.
- comprobarRutasAdministrativasBloqueadas conserva sus cuatro GET y añade
  POST /api/estaciones con el mismo cuerpo válido, esperando 403. Lo usan
  los casos existentes de CLIENTE y TECNICO. Se amplió además la comprobación
  de ausencia de interacciones a ambos repositorios de estaciones/bicicletas.
- La revisión estática confirmó que, revirtiendo solamente las adiciones
  y la ampliación prevista del helper, se recupera todo el contenido anterior:
  las doce pruebas, otros helpers, contexto y mocks. Ahora hay catorce @Test.
  La comparación SHA-256 de fuentes mostró solo WebAuthorizationTest
  modificado, ningún archivo eliminado y el staging intacto. diff --check
  no informó errores.
- No se modificaron seguridad productiva, controladores, servicios,
  repositorios, otros tests, frontend, DER, configuración o datos. No se
  ejecutaron Gradle, JavaScript, SQL, navegador, reinicios o commits.
- La parte 3 queda al usuario con el comando filtrado de las tres clases.
  Total esperado 46 pruebas (14 + 22 + 10), todavía sin acreditar ejecución
  tras este cambio. El alcance son filtros HTTP con servicio simulado,
  no SQL, transacciones ni el proxy de autorización del servicio real.

### Etapa 62 Cuarenta y seis pruebas verificadas y siguiente protección del alquiler

Fecha: 6 de noviembre de 2026.

Estado: el usuario ejecutó las tres clases filtradas y obtuvo BUILD
SUCCESSFUL en 6 s, dos tareas ejecutadas y dos actualizadas. Se leyeron
los XML del 7 de noviembre a las 00:22 UTC, correspondientes al 6 de
noviembre a las 21:22 de Argentina: WebAuthorizationTest 14,
EstacionControllerTest 22 y EstacionServiceTest 10; total 46, sin fallos,
errores u omitidas. Pasaron los nuevos casos de visitante y ADMIN y los
casos existentes de CLIENTE/TECNICO que ahora incluyen POST denegado.

- Quedan acreditados los escenarios de permisos HTTP del alta, con
  servicio simulado. Esto no cierra toda RN-INV-01 ni acredita persistencia,
  transacciones, el proxy de servicio o el formulario en navegador.
- Se releyeron RN-INV-01, crearAlquiler, finalizarAlquiler y el listado de
  bicicletas disponibles. La creación solo comprueba origen no nulo, la
  devolución todavía admite una ubicación interna activa y cuenta solo
  bicicletas no archivadas; el listado disponible tampoco filtra internas.
- El siguiente bloque propuesto es una modificación puntual en
  AlquilerService: rechazar origen no público/inactivo antes de crear el
  viaje, rechazar destino interno antes de modificar el alquiler y contar
  toda ocupación física al devolver. No se alterarán tarifas, precios,
  emails, propietarios, seguridad o estados en ese bloque.
- Después se alineará la oferta de bicicletas con estaciones públicas
  activas. Hasta completar ambos controles y sus pruebas no se propone
  un alquiler/devolución de prueba con una ubicación interna real.
- No hay todavía tests de AlquilerService/Bicicleta en el árbol consultado;
  su cobertura específica se agregará sin iniciar MySQL. La implementación
  de los controles sigue pendiente, no se declara realizada en este turno.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios.

### Etapa 63 Instrucciones manuales para proteger retiro y devolución

Fecha: 6 de noviembre de 2026.

Estado: el usuario confirmó los tres criterios explicados y pidió continuar.
Se leyeron AlquilerService y AlquilerController completos y las búsquedas
activas con bloqueo de EstacionRepository. Se entrega un bloque puntual
de instrucciones sobre AlquilerService, sin cambiar las demás clases.

- En crearAlquiler, después del control de estación no nula y antes de
  duración/precio/mutaciones, se propone rechazar borradoEstacion=true
  o esPublica=false con IllegalStateException y mensaje comprensible.
- En finalizarAlquiler, se conserva la búsqueda activa con bloqueo por
  nombre, que ya excluye bajas lógicas, y se propone rechazar esPublica=false
  con IllegalArgumentException antes de contar o modificar el alquiler.
- En el mismo punto se sustituye el contador que excluye archivadas por
  countByUbicacionActual(nuevaEstacion). La comprobación existente de
  capacidad permanece y conserva su IllegalStateException por estación llena.
- AlquilerController actual devuelve 400 para esos rechazos de entrada
  (el POST captura Exception y el PATCH IllegalArgumentException), y
  mantiene 409 para estación llena en PATCH. No se proponen cambios en él.
- Son controles del flujo normal de alquiler/devolución, no un permiso
  para trasladar bicicletas al depósito mediante la devolución de un cliente.
  Los movimientos administrativos conservan su recorrido diferenciado.
- No se modifican precios, cotización, tiempos, correos, estados, titularidad,
  firma de endpoints o bloqueos existentes. No se afirma que estos cambios
  prueben concurrencia o completen todo el recorrido v5 del alquiler.
- Se propone compileJava por el usuario. Pruebas específicas y filtrado
  del listado de bicicletas disponibles seguirán después; no iniciar QA real.
- Solo se actualizó este diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. Implementación del bloque pendiente del usuario.

### Etapa 64 Controles de estaciones en alquiler implementados, cierre manual pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar únicamente los tres
controles propuestos, reservando la parte 3, y preguntó por las pruebas
manuales de estaciones. Se aclaró que ese bloque no se considera cerrado:
las 46 pruebas previas son automatizadas con dependencias simuladas y no
sustituyen la comprobación del formulario, mapa y persistencia reales.

- Se modificó únicamente AlquilerService: después del control de origen
  no nulo se rechaza estación de retiro borrada lógicamente o interna,
  antes de calcular importes o modificar bicicleta/alquiler.
- En la devolución se conserva la búsqueda activa con bloqueo y se
  rechaza esPublica=false antes de contar o registrar la devolución.
- Se cambió el contador de destino por countByUbicacionActual, incluyendo
  archivadas físicamente presentes. Se conserva el control de capacidad.
- La revisión estática, retirando los dos controles nuevos y restaurando
  contador/comentario previos, recuperó el resto del código íntegro,
  salvo la normalización del salto de línea final. No se alteraron
  permisos/propietario, transacciones/bloqueos, fechas, estados, precios,
  cotización, correos ni firmas. Las comprobaciones nuevas preceden
  a las mutaciones de la operación correspondiente.
- La comparación SHA-256 de fuentes mostró solamente AlquilerService
  modificado, ningún archivo eliminado y staging intacto. Se actualizó
  además este diario; no se modificaron otros fuentes, frontend, DER,
  configuración o datos. diff --check no informó errores.
- No se ejecutaron Gradle, SQL, navegador, reinicios o commits. El usuario
  conserva compileJava como parte 3. No se acredita aún la compilación
  ni pruebas específicas de estos controles con el cambio realizado.
- Seguimos completando RN-INV-01, no refactorizando ahora todo alquiler:
  faltan sus pruebas y el filtro de oferta en estaciones públicas activas.
  Después debe realizarse una verificación manual de esta entrega de
  estaciones: alta pública/interna, visibilidad pública y administrativa,
  edición/control de versión, capacidad y baja lógica, con revisión de
  persistencia. No dar el bloque por cerrado sin registrar esa evidencia.
- El reinicio del perfil refactor con create-drop debe hacerse de forma
  controlada, advirtiendo el descarte de los datos de prueba existentes;
  no se realizó todavía. Auditoría y otras condiciones v5 dependientes
  de entidades aún pendientes no se declaran implementadas por este bloque.

### Etapa 65 Compilación de controles confirmada y cierre de oferta pendiente

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó compileJava exitoso en 2 s, tarea ejecutada.
Se releyeron los tres controles en AlquilerService y se confirmó la clase
compilada con modificación del 6 de noviembre a las 21:40:16 local.
Compilar no acredita todavía el comportamiento de esos nuevos controles.

- Se revisaron BicicletaRepository, getBicicletasDisponibles y BicicletaDTO.
  El listado sigue usando el filtro antiguo que no comprueba la estación;
  el DTO accede al nombre de la ubicación. La búsqueda de referencias
  mostró que findByBorradoBiciFalseAndEstado solo se declara en ese
  repositorio y se usa en ese método del controlador.
- El siguiente bloque propuesto sustituirá esa consulta por otra de nombre
  explícito, con estado solicitado, bici no archivada, ubicación no nula,
  pública y no dada de baja. Se precargará ubicacionActual con EntityGraph
  para mapear el DTO sin depender de una carga diferida posterior.
- Se actualizará la llamada del listado, sin cambiar su URL, respuesta
  o permisos; la consulta anterior se reemplazará, no se dejará huérfana.
  El inventario administrativo general y el filtrado por tipo no se alterarán.
- Siguen pendientes los dos contadores de altas/traslados administrativos
  de BicicletaController que excluyen archivadas. Se propone alinearlos
  con countByUbicacionActual en el mismo bloque de consistencia de capacidad,
  conservando sus bloqueos y demás condiciones; no es una refactorización
  completa de bicicletas ni una implementación de sus nuevos estados.
- Después corresponderán las comprobaciones específicas y la prueba manual
  de esta entrega de estaciones ya comprometida. No se da por cerrado
  RN-INV-01 ni se propone reiniciar antes de preparar el descarte create-drop.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. La consulta y el cambio de contadores siguen propuestos.

### Etapa 66 Instrucciones de oferta pública y contadores de ingreso

Fecha: 6 de noviembre de 2026.

Estado: el usuario pidió continuar. Se leyeron BicicletaRepository y
BicicletaController completos, el mapeo de Bicicleta y el uso existente
de EntityGraph en UsuarioRepository. Se comprobó otra vez que la consulta
antigua de disponibles solo se usa en ese controlador.

- Se propone importar EntityGraph y sustituir la declaración antigua
  findByBorradoBiciFalseAndEstado por buscarPorEstadoEnEstacionesPublicas,
  con estado parametrizado, bici no archivada e inner join a estación
  pública no dada de baja. La ausencia de estación queda excluida por
  ese join; EntityGraph precarga ubicacionActual para construir el DTO.
- En getBicicletasDisponibles se reemplazará la llamada, manteniendo URL,
  forma de respuesta y estado DISPONIBLE. Los listados administrativos
  general/por tipo y el GET por id no se modificarán.
- En crearBicicleta y en la rama de traslado de actualizarBicicleta se
  sustituirá el conteo filtrado por countByUbicacionActual, conservando
  estación activa/bloqueada, comprobación de capacidad y demás lógica.
- No se eliminará en este bloque el contador antiguo del repositorio:
  dos pruebas existentes todavía lo mencionan para prohibir su uso. Su
  limpieza coordinada con esas verificaciones queda fuera de estos cambios.
- La lectura detectó además un riesgo previo de actualizarBicicleta:
  asigna código/estado antes de validar el destino y puede devolver 409
  dentro de una transacción sin excepción, con cambios parciales pendientes
  de flush. No se considera resuelto por cambiar contadores; debe revisarse
  antes de dar por válida una edición rechazada en la QA integral.
- Se entregan instrucciones manuales sobre los dos archivos. Comprobación
  propuesta compileJava; no acredita el JPQL ejecutado, filtros SQL,
  transacciones o pruebas de comportamiento. Siguen comprobaciones y QA manual.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. Implementación pendiente del usuario.

### Etapa 67 Oferta pública y contadores de ingreso implementados

Fecha: 6 de noviembre de 2026.

Estado: el usuario autorizó por esta vez implementar exclusivamente el
bloque propuesto y reservó la parte 3, guardar y comprobar. Se releyeron
repositorio y controlador antes de editar, conservando los cambios previos.

- BicicletaRepository incorpora el import EntityGraph y sustituye la
  declaración antigua de disponibles por buscarPorEstadoEnEstacionesPublicas.
  La consulta parametriza estado, exige bici no archivada y estación
  pública no dada de baja, excluye ubicación nula mediante inner join
  y precarga ubicacionActual para el DTO. La búsqueda de referencias
  confirmó que no queda ningún uso del método sustituido en src.
- getBicicletasDisponibles usa la nueva consulta con DISPONIBLE, conservando
  URL, tipo de respuesta y permisos. Los demás listados no se alteran.
- crearBicicleta y la rama de traslado de actualizarBicicleta usan ahora
  countByUbicacionActual para no ignorar ocupación de archivadas. Sus
  bloqueos, condiciones de capacidad, mensajes y resto de la lógica se conservan.
- Se conserva el contador antiguo del repositorio por las referencias
  negativas de las pruebas existentes, según lo propuesto; no se modifican
  esas pruebas ni se eliminan otros métodos.
- La revisión estática recuperó íntegro el contenido previo de ambos
  archivos al revertir solo la consulta/import, GET disponible y dos
  contadores. La comparación SHA-256 de todas las fuentes confirmó solo
  esos dos archivos modificados, ninguno eliminado y staging intacto.
  diff --check no informó errores.
- Se actualizó además este diario. No se modificaron otros fuentes,
  pruebas, frontend, DER, configuración o datos, ni se ejecutaron Gradle,
  SQL, navegador, reinicios o commits. La parte 3 queda para el usuario
  con compileJava; no se acredita compilación o ejecución JPQL todavía.
- Sigue pendiente el riesgo de modificaciones parciales de código/estado
  al rechazar una edición, documentado en la etapa 66; no se corrigió
  silenciosamente fuera del bloque autorizado. También siguen pendientes
  las pruebas específicas y la QA manual de esta entrega de estaciones.

### Etapa 68 Compilación vigente y corrección de edición parcial propuesta

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó compileJava exitoso en 1 s con la tarea
UP-TO-DATE; no se describe como una nueva ejecución del compilador.
Se confirmaron las clases de BicicletaController y BicicletaRepository
con modificación del 6 de noviembre a las 22:00:57 local. La compilación
vigente no acredita todavía la ejecución del JPQL o pruebas funcionales.

- Se releyó actualizarBicicleta completo. Antes de rechazar una estación
  de destino inválida/llena ya modifica código y estado de la entidad
  administrada; una respuesta normal 400/409 no provoca por sí sola
  rollback. Es el riesgo señalado en la etapa 66, no una regla nueva.
- El próximo bloque manual propuesto retira el setter temprano de código
  y únicamente la asignación temprana del estado, conservando la validación
  que prohíbe marcar ALQUILADA manualmente. Ambos setters se colocarán
  después de las comprobaciones de destino/capacidad y antes del bloque
  de tipo, manteniendo sus condiciones originales !estabaAlquilada.
- Los retornos de rechazo quedarán antes de modificar ubicación, código,
  estado, tipo o precio. No se cambian permisos, estados admitidos,
  bloqueos, mensajes, contadores o respuestas de las operaciones válidas.
- La implementación se propone solo dentro de actualizarBicicleta; no se
  crean clases, tablas o reglas adicionales. La comprobación del usuario
  será compileJava; se requerirán después pruebas de no mutación al rechazar.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. No se considera cerrada todavía la QA manual de estaciones.

### Etapa 69 Asignaciones de edición reubicadas, compilación pendiente

Fecha: 6 de noviembre de 2026.

Estado: tras aclarar que no se elimina la protección de una bicicleta
alquilada, el usuario autorizó por esta vez implementar exclusivamente
la reubicación propuesta y reservó la parte 3, guardar y comprobar.
Se releyó actualizarBicicleta antes de editar.

- Se retiró la asignación inicial de código y solamente el setter temprano
  del estado. Se conservaron íntegros los if de validación, incluido el
  rechazo de ALQUILADA manual, junto con sus mensajes y respuestas.
- Se colocaron las dos asignaciones después del bloque de estación y
  antes del bloque de tipo, manteniendo !estabaAlquilada y los controles
  de campo no nulo. No se eliminó la restricción de cambiar código o
  estado durante un alquiler; únicamente cambió el orden de aplicación.
- La revisión estática confirmó que los cinco retornos normales de error
  del método preceden a la primera mutación de bicicletaExistente. No
  hay cambios de código/estado antes de devolver 400/409 por destino o
  capacidad, ni antes del rechazo de ALQUILADA manual.
- La comparación, restaurando exclusivamente los setters a sus posiciones
  previas, recuperó íntegro el código anterior. La comparación SHA-256 de
  todas las fuentes mostró solamente BicicletaController modificado,
  ningún archivo eliminado y staging intacto. diff --check sin errores.
- Se actualizó además este diario. No se modificaron otros métodos,
  condiciones, permisos, bloqueos, contadores, precios, repositorios,
  pruebas, frontend, DER, configuración o datos. No se ejecutaron Gradle,
  SQL, JavaScript, navegador, reinicios o commits.
- El usuario conserva compileJava como parte 3. La causa de mutaciones
  anticipadas queda corregida en el código, pero no se acredita todavía
  compilación ni ejecución de pruebas de rechazo, transacción real o QA
  manual. Esas comprobaciones siguen pendientes antes de cerrar esta entrega.

### Etapa 70 Compilación vigente y pruebas de edición propuestas

Fecha: 6 de noviembre de 2026.

Estado: el usuario informó compileJava exitoso en 1 s con UP-TO-DATE.
Se confirmó BicicletaController.class con modificación del 6 de noviembre
a las 22:17:12 local y se releyó el método reordenado. No se describe el
comando como una compilación nueva ni como prueba de comportamiento.

- Se confirmó que todavía no existe BicicletaControllerTest ni una clase
  de pruebas de AlquilerService en el árbol de tests consultado.
- El próximo bloque propuesto creará únicamente BicicletaControllerTest,
  con JUnit/Mockito, los dos repositorios simulados y llamadas directas al
  controlador. No se cargarán Spring, JPA, MySQL ni una aplicación real.
- Se proponen seis pruebas: ALQUILADA manual rechazada sin cambios;
  destino con id inválido sin cambios; destino inexistente sin cambios;
  destino lleno contando ocupación de archivadas sin cambios; traslado
  válido con edición permitida; bicicleta alquilada conserva código,
  estado, tipo y ubicación vacía, permitiendo solo precio.
- Los rechazos compararán todos los campos originales y verificarán
  ausencia de guardado. El caso lleno comprobará el contador total. Los
  recorridos válidos verificarán datos y guardado con repositorio simulado.
- Total previsto 52 pruebas con las cuatro clases filtradas, pendiente
  de implementar/ejecutar. Es prueba de control de flujo y no mutación en
  memoria, no de rollback real, SQL o seguridad HTTP. Los controles de
  alquiler y la consulta pública siguen necesitando su evidencia específica
  y la QA manual de estaciones comprometida permanece pendiente.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios.

### Etapa 71 Instrucciones manuales de pruebas de edición de bicicleta

Fecha: 7 de noviembre de 2026.

Estado: el usuario pidió continuar. Se releyeron actualizarBicicleta,
Bicicleta, BicicletaDTO y el patrón JUnit/Mockito existente. Se confirmó
que BicicletaControllerTest no existe todavía y se entrega su código
completo para creación manual en el paquete controllers de test existente.

- Se mantienen los seis escenarios propuestos en la etapa 70, con
  repositorios simulados y llamadas directas al controlador. No hay
  contexto Spring, base de datos, navegador o transacción real.
- Los datos de prueba separan origen público y depósito interno. La
  bicicleta archivada del caso de capacidad permanece en ese depósito,
  coherente con RN-INV-01; su ocupación se simula con el contador total.
- Los cuatro rechazos comprueban código, estado, tipo, precio, ubicación,
  id y borrado sin cambios y prohíben guardar. Los casos válidos comprueban
  la entidad y el DTO; el alquilado conserva sus restricciones y solo
  permite precio. Los mocks se preparan por caso para evitar stubs sin uso.
- Se proponen helpers pequeños para estaciones, búsqueda de bicicleta y
  comparación de rechazos. No se modifican modelos, enums, repositorios
  ni lógica productiva para construir los tests.
- Se propone ejecutar las cuatro clases filtradas; total esperado 52
  pruebas (14 + 22 + 10 + 6), pendiente de implementar y comprobar.
  La validación del SQL, proxy transaccional y permisos HTTP no está en
  estos tests; la QA manual y evidencia específica de alquiler siguen pendientes.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. La creación del archivo sigue a cargo del usuario.

### Etapa 72 Cincuenta y dos pruebas verificadas y preparación de QA manual

Fecha: 7 de noviembre de 2026.

Estado: el usuario creó BicicletaControllerTest y ejecutó las cuatro clases
filtradas, con BUILD SUCCESSFUL en 15 s, dos tareas ejecutadas y dos
actualizadas. Se leyó el archivo completo: paquete, imports, seis casos,
datos de prueba y helpers están correctamente ubicados y coinciden con
la propuesta. No se modificó el archivo durante esta revisión.

- Se leyeron los XML del 7 de noviembre a las 19:50 UTC, correspondientes
  a las 16:50 de Argentina: BicicletaControllerTest 6, WebAuthorizationTest
  14, EstacionControllerTest 22 y EstacionServiceTest 10; total 52, sin
  fallos, errores u omitidas. Los seis casos nuevos figuran por nombre.
- Se acredita la conservación de datos en memoria en los rechazos y los
  dos recorridos permitidos, no rollback real, SQL o seguridad HTTP por
  esas seis pruebas. La corrección no se presenta como validación global
  de todas las reglas futuras v5 de bicicletas.
- Se revisaron Alquiler y AlquilerRequestDTO para el último bloque
  automatizado previsto de los controles de estaciones en alquiler:
  cuatro rechazos, retiro interno, retiro desde estación de baja lógica,
  devolución interna y devolución con ocupación física completa. Se
  verificarán ausencia de cambios/guardados y de correos, sin cotización
  HTTP ni MySQL. Sigue pendiente crear ese test, no se implementa aquí.
- La consulta JPQL de oferta pública no queda acreditada por los mocks;
  la comprobación real de su filtrado y la QA manual de estaciones siguen
  pendientes. No se añaden funcionalidades nuevas a esta entrega por este paso.
- Se preguntó al usuario si los datos actuales de turismo_sustentable son
  descartables o deben conservarse, para preparar el reinicio create-drop
  sin asumir autorización de descarte. Respuesta pendiente en este momento;
  no se reinició, exportó o modificó la base ni se accedió al navegador.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL o reinicios.

### Etapa 73 Descarte de datos de prueba autorizado para reinicio controlado

Fecha: 7 de noviembre de 2026.

Estado: el usuario respondió a la consulta de preparación de QA manual:
«Son de prueba y se pueden descartar». Queda autorizada la pérdida de
los datos de prueba actuales de turismo_sustentable al reiniciar de forma
controlada el perfil refactor, configurado con create-drop. No se extiende
la autorización a turi, otras bases, archivos o historial de Git.

- No se reinicia todavía: siguen pendientes las cuatro pruebas aisladas
  de los rechazos de alquiler/devolución propuestos en la etapa 72.
- Después se preparará el arranque con la configuración nueva y la QA
  manual de esta entrega de estaciones, registrando que los datos y
  cuentas de prueba deberán recrearse tras el descarte.
- No se ejecutó DROP, SQL, reinicio, respaldo o cambio de perfil. Solo
  se actualizó el diario; no se modificaron fuentes, configuración,
  frontend, DER, staging o commits ni se ejecutó Gradle.

### Etapa 74 Instrucciones manuales de cuatro rechazos en AlquilerService

Fecha: 7 de noviembre de 2026.

Estado: el usuario pidió avanzar con las pruebas pendientes. Se releyeron
AlquilerService y Alquiler completos, AlquilerRequestDTO y los accesores
de identidad/actividad de Usuario. Se confirmó que AlquilerServiceTest
todavía no existe. Se propone crear únicamente ese archivo de test.

- Cuatro casos: retiro interno, retiro desde estación de baja lógica,
  devolución interna y devolución con ocupación física igual a capacidad.
  Se comprueban mensajes, conservación de bicicleta/alquiler y ausencia
  de guardados y correos, sin ampliar reglas de negocio o casos ajenos.
- Los cinco colaboradores se simulan con Mockito. Un token CLIENTE en
  SecurityContextHolder permite comprobar los controles sobre un titular
  activo; el contexto se limpia después de cada prueba. No es un login real.
- Se usa spy del servicio únicamente para anular de forma segura la
  consulta de cotización con un stub lenient doReturn. Así, aunque una
  regresión dejara avanzar el flujo, nunca se invoca una API externa.
  Las comprobaciones esperan además que el método de cotización no se llame.
- Las fechas y montos de la fixture se mantienen fijos para comprobar
  ausencia de mutaciones, sin probar ahora el cálculo económico. El
  contador total simulado no acredita por sí solo su predicado SQL real.
- Se propone ejecutar las cinco clases filtradas; total previsto 56
  pruebas (4 + 6 + 14 + 22 + 10), pendiente de crear y ejecutar. No se
  carga Spring, JPA o MySQL ni se prueba un rollback transaccional real.
- Después se preparará el reinicio controlado autorizado en la etapa 73
  y la QA manual de esta entrega de estaciones; no se inicia todavía.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. La creación del test sigue a cargo del usuario.

### Etapa 75 Cincuenta y seis pruebas verificadas y arranque manual preparado

Fecha: 7 de noviembre de 2026.

Estado: el usuario creó AlquilerServiceTest y ejecutó las cinco clases
filtradas con BUILD SUCCESSFUL en 7 s, dos tareas ejecutadas y dos
actualizadas. Se leyó el archivo completo y se corroboraron paquete,
imports, spy seguro, contexto/limpieza, fixtures y los cuatro casos.

- Se leyeron los XML de esa ejecución del 7 de noviembre a las 20:16 UTC
  (17:16 de Argentina): AlquilerServiceTest 4, BicicletaControllerTest 6,
  WebAuthorizationTest 14, EstacionControllerTest 22 y EstacionServiceTest
  10; total 56, sin fallos, errores u omitidas. Los cuatro casos nuevos
  figuran por nombre y pasaron. No se modificaron tests en esta revisión.
- Se acreditan los rechazos y ausencia de mutaciones/guardados/correos en
  memoria según su alcance. No se extiende el resultado a JPA, consulta
  pública ejecutada, transacciones reales o comportamiento de navegador.
- Se releyó application-refactor.properties: MySQL local, base exclusiva
  turismo_sustentable, create-drop e inicialización SQL desactivada.
  Se revisaron main y CargaCatalogosRefactor para preparar datos de QA;
  el cargador precarga catálogos del perfil, no una cuenta ADMIN de prueba.
- El siguiente paso manual propuesto es detener solo la instancia local
  previa si sigue ejecutándose, verificar MySQL en XAMPP e iniciar bootRun
  con --spring.profiles.active=refactor --server.port=8080. El descarte
  de datos de prueba está autorizado en la etapa 73. No se ejecuta DROP
  manual ni se cambia la configuración. Las cuentas de QA se recrearán.
- Se revisará el arranque real y la validación de repositorios antes de
  guiar la primera prueba manual de estaciones. Esa prueba y persistencia
  siguen pendientes: no se declara cerrado el bloque ni realizada la QA.
- Solo se actualizó el diario. No se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. El arranque queda a cargo del usuario.

### Etapa 76 Arranque reportado y preparación de cuenta QA

Fecha: 7 de noviembre de 2026.

Estado: el usuario reportó Tomcat iniciado en 8080 y Started de la
aplicación a las 17:20:20, proceso 9288, con duración de arranque 3,686 s.
El aviso open-in-view no impidió el arranque; el 80 % de bootRun con
EXECUTING indica que el servidor continúa ejecutándose, no un test fallido.

- No se declara validado todavía el resultado real del filtrado JPQL o
  la persistencia de estaciones solo con el log de arranque.
- Se releyeron RegistroUsuarioDTO, la asignación forzada de CLIENTE en
  RegistroUsuarioService, UsuarioRol y los campos del formulario para
  preparar una cuenta QA nueva. El registro público no otorga ADMIN.
- El siguiente paso manual propuesto es registrar admin.estaciones con
  datos ficticios, documento DNI argentino y correo example.com; la clave
  la elige el usuario y no se solicita ni guarda aquí. No iniciar sesión
  hasta preparar después la asignación ADMIN exclusivamente de QA.
- Las dos FK de usuario_rol se confirmaron en el modelo actual; cualquier
  asignación posterior identificará usuario por nickname y rol por código,
  sin asumir ids. No se ejecuta ahora una asignación o SQL de privilegios.
- La creación de la cuenta, los roles persistidos y la QA de estaciones
  siguen pendientes de evidencia. Se recomienda ventana privada para
  evitar cookies y sessionStorage de la instancia anterior.
- Solo se actualizó el diario; no se editaron fuentes, frontend, DER,
  configuración, staging o commits, ni se ejecutaron Gradle, SQL, navegador
  o reinicios. Los logs de arranque son los aportados por el usuario.

### Etapa 77 Registro QA verificado y asignación ADMIN manual propuesta

Fecha: 7 de noviembre de 2026.

Estado: el usuario informó que creó la cuenta. Se consultó únicamente
turismo_sustentable mediante una transacción READ ONLY, sin leer hashes,
tokens o contraseñas. La conexión inicial del sandbox fue bloqueada con
10013; la consulta de lectura autorizada posterior finalizó correctamente.

- Se verificó usuario id 1, nickname admin.estaciones, correo ficticio
  admin.estaciones@example.com y activo. Tiene únicamente CLIENTE (rol id 2).
  El catálogo contiene ADMIN (id 1). No se asumen esos ids para el siguiente SQL.
- Se releyeron Rol y UsuarioRol: tabla rol, tabla usuario_rol con PK
  compuesta y las dos FK usuario_id/rol_id, sin columnas de asignación
  adicionales. La cuenta conserva CLIENTE como exige el registro público.
- El siguiente paso manual propuesto agrega exclusivamente ADMIN a esa
  cuenta activa mediante INSERT SELECT identificado por nickname y
  código de rol, con NOT EXISTS para evitar duplicar la asignación.
  Se especificará la base turismo_sustentable y una consulta posterior
  de verificación de ambos roles. No se modifica el registro público.
- Es preparación exclusiva de QA local, no un endpoint nuevo ni una
  funcionalidad administrativa/auditoría v5 declarada implementada.
  La asignación sigue pendiente de ejecución por el usuario.
- Después se indicará iniciar sesión por nickname; si ya había iniciado,
  debe cerrar y volver a entrar para renovar las autoridades de la sesión.
- Solo se actualizó el diario y se ejecutaron SELECT de comprobación.
  No se ejecutaron INSERT/UPDATE/DELETE, DDL, reinicios o Gradle, ni se
  editaron fuentes, frontend, DER, configuración, staging o commits.

### Etapa 78 ADMIN de QA confirmado e inicio de prueba de alta pública

Fecha: 7 de noviembre de 2026.

Estado: el usuario informó que ejecutó el cambio y accedió al panel en
Usuarios, viendo Admin Prueba con CLIENTE y ADMIN. La comprobación SQL
posterior en READ ONLY confirmó usuario id 1 activo y ambas asignaciones.
No se cambió ningún rol desde el asistente. El acceso/pantalla se registra
como observación del usuario, no como inspección automatizada del navegador.

- Se confirmó que la tabla estacion está vacía (cero filas), sin asumir
  que hubo altas todavía. Se revisaron el modal y las etiquetas actuales
  del formulario para guiar la primera prueba manual.
- PM-EST-01 propuesta: completar los datos de una estación y guardar
  dejando Uso de la estación sin selección. Debe aparecer el aviso y
  no completarse el alta; la observación del usuario queda pendiente.
- PM-EST-02 propuesta: en ese mismo formulario elegir Pública y guardar
  Plaza QA Centro, Calle de Prueba 100, capacidad 2, coordenadas
  -53.7865/-67.7045. Debe confirmar guardado y aparecer una fila. La
  persistencia, clasificación y versión se comprobarán después de la acción.
- Todavía no se declara superado ninguno de esos dos casos ni validado
  el mapa, estación interna, edición, capacidad, bajas o filtrado real.
- Solo se actualizó el diario y se ejecutaron consultas READ ONLY de roles
  y estaciones. No se ejecutaron escrituras SQL, Gradle, navegador o
  reinicios ni se editaron fuentes, frontend, DER, configuración, staging o commits.

### Etapa 79 PM-EST-01 superada y alta pública rechazada con 401

Fecha: 7 de noviembre de 2026.

Estado: el usuario confirmó que guardar sin elegir uso muestra el aviso
esperado. PM-EST-01 se registra superada según su observación. Al elegir
Pública, el frontend mostró su texto genérico de fallo; PM-EST-02 no se
considera superada. La consulta READ ONLY confirmó que no hay estaciones.

- El usuario aportó la consola: POST /api/estaciones recibe 401. La
  advertencia aria-hidden se registra aparte; no explica por sí sola
  el rechazo HTTP de autenticación. No se atribuye el fallo a capacidad,
  datos del DTO o persistencia sin evidencia adicional.
- Se releyeron admin.js, auth.js, el controlador, servicio y modelo de
  estación y las reglas HTTP. guardarEstacion no trata 401 específicamente;
  el control de sesión de admin.js ocurre al cargar la página. Una página
  abierta puede permanecer visible aunque la sesión deje de ser válida.
- GET público /api/estaciones fue comprobado por consulta de lectura local:
  devolvió 200 con []. Esto confirma conectividad y lectura pública vacía,
  no la sesión del navegador ni el resultado de POST autenticado.
- El intento de leer la terminal del chat indicó que no hay sesión adjunta;
  no se obtuvo un stacktrace del servidor. No se afirmó una excepción 500.
- Se propone primero cerrar e iniciar sesión de nuevo y reintentar una
  única alta QA. Si persiste 401, comprobar /api/usuarios/actual desde el
  mismo navegador para distinguir sesión perdida de otro fallo del flujo.
  No se propone relajar permisos ni hacer el POST público.
- Solo se actualizó el diario y se realizaron lecturas de fuentes, SELECT
  y un GET público. No se ejecutaron escrituras SQL, POST, Gradle,
  reinicios o cambios de código/configuración, staging o commits.

### Etapa 80 Desincronización de sesión visual confirmada en el código

Fecha: 7 de noviembre de 2026.

Estado: el usuario indicó que veía sesión iniciada pero recibía 401 al
entrar a mis_alquileres/admin. Se leyeron index.js completo, los controles
de admin.js y mis_alquileres.js y la configuración de autenticación/autorización.

- Causa confirmada de la incoherencia visual: index.checkSession usa
  exclusivamente usuarioLogueado/usuarioRol de sessionStorage para mostrar
  Sesión Activa, ocultar el login y habilitar enlaces; no valida ese estado
  con GET /api/usuarios/actual al cargar index.
- Al recibir 401, verificarAdmin redirige al index sin limpiar esas claves.
  El historial redirige también, y cargarNombreUsuario solo elimina el
  nombre. Así puede conservarse un indicador local de login que ya no
  representa una sesión reconocida por Spring Security.
- La pérdida/no reconocimiento de sesión real puede deberse a expiración,
  reinicio, logout en otra pestaña o cookie ausente; no se determina cuál
  ocurrió con la evidencia disponible. El 401 en páginas protegidas indica
  que esas solicitudes no acreditaron autenticación, no que falte ADMIN en BD.
- No se atribuye al aviso aria-hidden ni se propone relajar permisos.
  El ajuste propuesto es validar el estado real en index, centralizar la
  limpieza de indicadores ante 401 y mostrar un mensaje claro al guardar.
  Se debe distinguir 403 (permisos insuficientes) de 401, sin cerrar una
  sesión válida por cualquier error de red o autorización.
- No se implementó todavía ese ajuste ni se acreditó que un reingreso
  haya resuelto el POST. PM-EST-02 sigue sin superar hasta reintento válido
  y comprobación de persistencia. No se editan reglas de seguridad.
- Solo se actualizó el diario y se leyeron fuentes. No se modificaron
  Java/JS/HTML, configuración, DER, datos, staging o commits ni se ejecutaron
  Gradle, SQL, navegador, POST o reinicios.

### Etapa 81 Sesión real compartida y tratamiento de 401/403

Fecha: 7 de noviembre de 2026.

Estado: el usuario autorizó avanzar con la corrección de manejo de sesión.
Se conserva autenticación por nickname, Spring Security, HttpSession y cookie
JSESSIONID. No se incorpora JWT, almacenamiento de contraseñas en el navegador,
tablas de sesión, cambios al DER ni permisos nuevos.

- auth.js concentra consulta del perfil real, sincronización de nombre/rol,
  limpieza de las tres claves de presentación y comprobación de roles para
  entrar a las páginas. Las consultas de perfil simultáneas comparten una
  petición pendiente, sin guardar indefinidamente el resultado ni realizar
  sondeo periódico que mantenga viva la sesión. Se utiliza roles[] y se
  conserva compatibilidad con el campo rol anterior.
- index.js no muestra Sesión Activa por una marca de sessionStorage. Primero
  consulta al servidor, conserva el formulario cuando recibe 401 y lo restaura
  si el navegador vuelve desde una copia de la página. Se evitan envíos de
  login mientras se comprueba la sesión y durante un ingreso ya confirmado.
  Los enlaces de alquiler requieren CLIENTE/ADMIN y el administrativo ADMIN;
  la autorización efectiva sigue siendo del backend.
- admin.js, alquiler.js y mis_alquileres.js esperan la comprobación antes de
  cargar datos. Se retiran las marcas locales como condición definitiva de
  acceso y la consulta redundante a estadísticas solo para verificar ADMIN.
  El nombre reutiliza el perfil ya confirmado al entrar a la página.
- Los catch de peticiones protegidas utilizan el manejador común: 401 limpia
  indicadores y redirige una sola vez al ingreso con un aviso; 403 informa
  permisos insuficientes sin desconectar. Los errores 400/409 y de conexión
  continúan en sus manejadores propios. El historial ya no redirige al login
  por cualquier falla. No se repiten POST/PUT/PATCH/DELETE automáticamente.
  Los 401 esperados del login y de la comprobación pública de sesión tienen
  tratamiento separado para evitar avisos falsos o bucles de redirección.
- WebAuthorization conserva todos los antMatchers y roles. Solo cambia la
  presentación de errores en GET de admin.html, alquiler.html y
  mis_alquileres.html: sin autenticación redirige al ingreso y, sin permisos,
  al inicio con aviso. Las APIs conservan 401/403, sin redirigir sus respuestas
  a HTML. Estas comprobaciones también se realizan al restaurar páginas
  privadas desde la caché de navegación.
- Logout conserva POST, invalidación del servidor y aviso de éxito con su
  espera. Se confirma falta de autenticación únicamente con 401, no con 403.
  Limpia solo las claves de sesión; una falla al confirmar el cierre no se
  anuncia falsamente como éxito ni como prueba de que la sesión sigue activa.
- application.properties explicita server.servlet.session.timeout=30m, el
  valor predeterminado documentado de Spring Boot 2.7.14. Son treinta minutos
  sin peticiones que utilicen esa sesión, no treinta minutos de alquiler ni
  inactividad de teclado/ratón. No cambia el tiempo por defecto anterior. El
  servidor invalida la sesión y el frontend limpia su información al detectar
  el rechazo, no mediante un cronómetro que sustituya al servidor.
  Fuente: https://docs.spring.io/spring-boot/docs/2.7.14/reference/html/application-properties.html#application-properties.server.server.servlet.session.timeout
- No se activa límite de dispositivos/sesiones simultáneas. El usuario lo
  consultó, no fijó una política definitiva. Por ahora se conserva el
  comportamiento existente: sesiones independientes en distintos navegadores
  o dispositivos; logout cierra la sesión correspondiente, no todas. Cerrar
  una pestaña no equivale a ejecutar logout. El cierre remoto o exclusividad
  por cuenta requerirían una decisión adicional, no cambios improvisados.
- Se agregan tres pruebas de seguridad con MockMvc: redirección de las tres
  páginas sin sesión, rechazo de la página ADMIN manteniendo el perfil CLIENTE
  válido y logout con invalidación/borrado de cookie. src/test/js/Sesion.test.cjs
  incorpora 17 pruebas unitarias con Node test/vm, sin dependencias nuevas,
  navegador ni credenciales reales: perfil confirmado, marca vieja, 401/403,
  red, concurrencia de consultas, logout, recuperación del formulario, login
  rechazado y POST de estación no reenviado ni anunciado como éxito ante 401.

Comprobaciones ejecutadas por el asistente:

- Node --test src/test/js/Sesion.test.cjs: 17/17 superadas. Los cinco JS
  modificados también superaron node --check.
- Con JDK 11.0.19: Gradle test filtrado a WebAuthorizationTest (17),
  EstacionControllerTest (22), EstacionServiceTest (10), BicicletaControllerTest
  (6) y AlquilerServiceTest (4). BUILD SUCCESSFUL; XML confirmados:
  59 pruebas, cero fallos, errores u omisiones. No se ejecuta el test general
  que levanta la aplicación. Se autorizó acceso a la caché local de Gradle
  después de un primer intento bloqueado por permisos del entorno.
- git diff --check sin errores. No se modificó el staging ni se hizo commit
  o push. No se ejecutaron SQL ni reinicios de la aplicación ni se manipularon
  sesiones o datos reales mediante el navegador.

Pendiente de comprobación manual:

- Reiniciar la aplicación para cargar el cambio Java/configuración. El perfil
  refactor usa create-drop: ese reinicio elimina los datos QA actuales. El
  usuario autorizó descartarlos en Etapa 73; el asistente no reinició por su
  cuenta ni presenta esa prueba como realizada.
- Probar ingreso y navegación ADMIN/CLIENTE, cierre de sesión, Atrás y URL
  privada sin autenticación; verificar que no vuelva a aparecer Sesión Activa
  por marcas viejas. La expiración real de treinta minutos no se esperó en
  estas pruebas unitarias y no se declara verificada manualmente.
- PM-EST-02 continúa pendiente: repetir el alta con una sesión confirmada y
  comprobar la persistencia. Este ajuste no demuestra cuál fue la causa de
  pérdida de la sesión original ni sustituye las pruebas manuales de estaciones.

### Etapa 82 Reinicio informado y acceso anónimo a Administración comprobado

Fecha: 7 de noviembre de 2026.

- El usuario aportó el log del reinicio: PID 17448, Tomcat en puerto 8080 y
  Started TurismoSustentableApplication a las 18:58:50.736, sin error fatal.
  El aviso open-in-view y Gradle 80% EXECUTING no impidieron el arranque.
- PM-SES-01 superada según la comprobación manual del usuario: después de
  recargar el inicio con Ctrl+F5, entrar directamente a admin.html sin sesión
  devuelve al inicio con aviso de ingreso requerido y formulario visible,
  sin mostrar Sesión Activa ni una página de error 401. No se presenta como
  inspección automatizada del navegador ni se infiere el resultado de otras rutas.
- El usuario informó que volvió a registrar la cuenta QA. PM-SES-02 superada
  según su comprobación manual: inició sesión y pudo navegar tanto a
  Alquilar bicicletas como a Mis viajes, sin regresar al ingreso. No se
  presenta como prueba de crear/finalizar un alquiler ni de persistencia SQL.
- PM-SES-03 superada según el usuario: entrar a admin.html con la cuenta
  CLIENTE devuelve al inicio con el aviso de acceso no permitido; después
  pudo volver a Mis viajes sin introducir sus credenciales. Se comprueba que
  el rechazo por permisos no cierra esa sesión válida.
- PM-SES-04 superada según el usuario: cerró sesión, esperó la redirección e
  intentó abrir directamente mis_alquileres.html. Volvió al ingreso con el
  formulario visible y sin mostrar Sesión Activa. No se declara probado el
  vencimiento por tiempo ni todas las variantes de Atrás/dispositivos.
- Una consulta READ ONLY confirmó la nueva cuenta admin.estaciones, id 1,
  activa, con CLIENTE (rol_id 2) únicamente. El catálogo contiene ADMIN id 1,
  CLIENTE id 2 y TECNICO id 3. No se consultaron contraseñas ni se hicieron
  escrituras SQL. Se propone volver a agregar ADMIN sin retirar CLIENTE para
  retomar PM-EST-02; todavía no se declara realizada esa asignación.
- Se actualizó el diario y se ejecutaron SELECT de cuenta/roles. No se
  ejecutaron peticiones autenticadas, reinicios, Gradle ni cambios de código,
  staging o commits. La siguiente comprobación requiere la acción del usuario.

### Etapa 83 Ingreso ADMIN confirmado y reanudación del alta de estación

Fecha: 7 de noviembre de 2026.

- El usuario indicó que ejecutó la asignación ADMIN propuesta mediante
  INSERT SELECT con NOT EXISTS, conservando CLIENTE, y luego inició sesión.
  PM-SES-05 superada según su observación: ingresó al panel administrativo y
  la cuenta Admin Prueba muestra ambos roles en la pestaña Usuarios. No se
  presenta esa confirmación como consulta SQL posterior ni inspección del navegador.
- Se retoma PM-EST-02, pendiente desde el rechazo 401: crear Plaza QA Centro,
  Calle de Prueba 100, capacidad 2, uso Pública y coordenadas
  -53.7865/-67.7045. Se comprobará la persistencia después de que el usuario
  confirme la acción; todavía no se declara superada el alta ni su visualización
  en mapa, filtros de estaciones internas o reglas de capacidad.
- Solo se actualizó el diario y se leyó el formulario de estación vigente.
  No se ejecutaron SQL, peticiones autenticadas, reinicios, Gradle ni cambios
  de Java/JS/HTML, permisos, staging o commits.

### Etapa 84 Alta pública persistida y preparación de estación interna

Fecha: 7 de noviembre de 2026.

- PM-EST-02 superada: el usuario confirmó aviso de guardado, cierre del
  formulario y aparición de la estación en la tabla administrativa. Una
  consulta READ ONLY confirmó id 1, Plaza QA Centro, Calle de Prueba 100,
  capacidad 2, es_publica=1, latitud -53.7865, longitud -67.7045, version=0
  y borrado_estacion=0. El reintento autenticado quedó persistido; no se
  atribuye retrospectivamente una causa concreta al 401 anterior.
- Sigue PM-EST-03: crear Taller QA Central, Calle de Prueba 200, capacidad 5,
  uso interno y coordenadas -53.7855/-67.7055. Se espera que aparezca en la
  tabla administrativa; después se contrastará la lectura pública y el mapa.
  La creación interna y su filtrado todavía no se consideran comprobados.
- Solo se ejecutó SELECT de la estación indicada y se actualizó el diario.
  No hubo escrituras SQL por el asistente, navegación autenticada, reinicios,
  Gradle, cambios de código/DER/permisos ni modificaciones del staging o commits.

### Etapa 85 Alta interna y filtrado de la API pública comprobados

Fecha: 7 de noviembre de 2026.

- PM-EST-03 superada: el usuario informó la creación de la segunda estación.
  SELECT READ ONLY confirmó Taller QA Central id 2, Calle de Prueba 200,
  capacidad 5, es_publica=0, latitud -53.7855, longitud -67.7055, version=0,
  borrado_estacion=0. Plaza QA Centro conserva sus valores previos.
- GET anónimo http://localhost:8080/api/estaciones devolvió HTTP 200 y una
  sola estación: Plaza QA Centro id 1, esPublica=true, bicicletasDisponibles=0
  y espaciosLibres=2. Taller QA Central no aparece. Esto comprueba el filtrado
  de la lectura pública real, no solamente un filtro visual del frontend.
- Sigue contrastar visualmente el mapa desde el inicio: debe mostrar Plaza
  QA Centro como estación, sin el taller interno. No se declara inspección
  del mapa ni se modifican marcadores o datos de estaciones.
- Solo se realizaron SELECT, un GET público y actualización del diario. No
  se ejecutaron escrituras SQL, peticiones autenticadas, navegación del usuario,
  reinicios, Gradle ni modificaciones de código, DER, staging o commits.

### Etapa 86 Mapa confirmado y columna de uso administrativo propuesta

Fecha: 7 de noviembre de 2026.

- El usuario confirmó visualmente que el mapa solo muestra Plaza QA Centro,
  con cero bicicletas disponibles y dos espacios libres. No muestra Taller
  QA Central. Se completa la comprobación visual pendiente de la Etapa 85.
- El usuario detectó que la tabla de estaciones del panel no permite ver la
  clasificación sin abrir el formulario. Se confirmó en admin.html/admin.js
  que falta esa columna, aunque EstacionDTO y la API ya envían esPublica.
- Se propone agregar un encabezado Uso después de Capacidad y una celda
  correspondiente en cargarEstaciones, con Pública/Uso interno y badges
  Bootstrap existentes. No requiere modificar modelo, DTO, API, DER o CSS.
  Es una indicación para implementación manual; todavía no se presenta
  incorporada ni probada. Solo requiere guardar y recargar, no reiniciar
  el perfil create-drop ni perder los datos QA.
- Solo se leyeron archivos y se actualizó el diario. No se modificaron
  Java/JS/HTML, datos, permisos, staging o commits ni se ejecutaron pruebas.

### Etapa 87 Columna Uso agregada en la tabla administrativa de estaciones

Fecha: 7 de noviembre de 2026.

- El usuario autorizó implementar exclusivamente la propuesta de la Etapa 86.
  En admin.html se agregó el encabezado Uso después de Capacidad. En
  cargarEstaciones() de admin.js se agregó la celda correspondiente, usando
  esPublica: Pública con badge bg-success; Uso interno con bg-secondary.
  Se reutilizan Bootstrap y el dato del DTO existente, sin CSS o campos nuevos.
- Se compararon los dos archivos con una captura previa al cambio, normalizando
  finales de línea. Se comprobó que las únicas diferencias de código de esta
  intervención son ese encabezado y esa celda; se conservaron los cambios previos
  del usuario y los controles, acciones y consultas de la tabla.
- node --check admin.js superó la comprobación de sintaxis. Una comprobación
  aislada con Node/vm, GET simulado y estaciones pública/interna verificó los
  textos y colores, una sola consulta a /api/admin/estaciones y ocho celdas por
  fila alineadas con los ocho encabezados. No accedió al servidor ni a MySQL.
  git diff --check sin errores. No se repitieron Gradle ni la suite completa.
- El usuario confirmó visualmente que la columna Uso aparece y diferencia
  Pública/Uso interno. Se completa la comprobación visual de este cambio,
  sin afirmar que el asistente haya inspeccionado el navegador.
- Sigue la prueba manual de edición normal: en Plaza QA Centro comprobar
  que Uso se muestra Pública sin permitir cambios y guardar únicamente la
  dirección Calle de Prueba 120, manteniendo capacidad y coordenadas.
  Queda pendiente la acción del usuario y la verificación de persistencia/versión.
- Solo se editaron admin.html, el bloque indicado de admin.js y este diario.
  No se modificaron backend, DER, CSS, reglas, permisos o datos; tampoco se
  reinició la aplicación ni se hicieron escrituras Git o SQL.

### Etapa 88 Edición normal persistida y explicación del bloqueo de Uso

Fecha: 7 de noviembre de 2026.

- El usuario informó que cambió la dirección de Plaza QA Centro a Calle de
  Prueba 120 y el guardado fue correcto. SELECT READ ONLY confirmó id 1,
  nueva dirección, capacidad 2, es_publica=1, version=1 y borrado_estacion=0.
  La versión inicial era 0. Taller QA Central conserva dirección, capacidad,
  clasificación y version=0. Se registra superada la edición normal; no se
  confunde con haber probado todavía un conflicto entre dos formularios.
- Se revisó RN-INV-01 y el servicio vigente: Pública/Uso interno determina
  visibilidad y operaciones permitidas, no solo una etiqueta. La edición
  común no incorpora reclasificación: ActualizarEstacionDTO carece del campo,
  EstacionService no lo modifica y el formulario lo muestra deshabilitado.
- La razón del bloqueo actual es evitar una reclasificación operativa sin
  controles de operaciones pendientes y trazabilidad. El caso de reservas
  pendientes se explica como necesidad del DERv5, no como función ya migrada.
  RN-INV-01 define los efectos de cada clasificación, no una prohibición
  universal y definitiva de cambiarla. No se presenta la inmutabilidad como
  exigencia técnica de MySQL/JPA ni se agrega una nueva regla de negocio
  sin aprobación; habilitarla requeriría definir su recorrido administrativo.
- Solo se realizaron lecturas de código/documentación, SELECT y actualización
  de este diario. No se editaron fuentes ni se ejecutaron escrituras SQL,
  reinicios, Gradle, peticiones autenticadas, staging o commits.

### Etapa 89 Uso fijo conservado y prueba de formulario desactualizado preparada

Fecha: 7 de noviembre de 2026.

- El usuario decidió conservar por el momento el bloqueo de Uso en la edición.
  Se asentó esta precisión de alcance en RN-INV-01, documento 26, sin modificar
  el DER o el código y sin declarar una prohibición definitiva de reclasificar.
- Sigue la prueba manual de concurrencia: abrir la edición de Plaza QA Centro
  en dos pestañas A/B antes de guardar en cualquiera. Ambas parten de la misma
  versión; A cambia la dirección a Calle de Prueba 130 y guarda. B conserva su
  formulario anterior y procura guardar Calle de Prueba 140 sin recargar.
  Se espera éxito de A, rechazo de B por versión desactualizada y persistencia
  de 130, con capacidad y uso sin cambios. Todavía no se considera ejecutada
  ni superada esta comprobación real.
- Solo se leyeron archivos y se editaron los dos documentos mencionados. No
  se modificaron fuentes, datos, permisos o DER ni se ejecutaron SQL, reinicios,
  pruebas, peticiones autenticadas, staging o commits.

### Etapa 90 Conflicto entre formularios rechazado sin sobrescritura

Fecha: 7 de noviembre de 2026.

- El usuario confirmó que A guardó correctamente y B recibió el aviso de
  estación modificada y necesidad de recargar. SELECT READ ONLY confirmó
  Plaza QA Centro con Calle de Prueba 130, capacidad 2, es_publica=1,
  version=2, borrado_estacion=0. No se guardó la dirección 140 propuesta
  desde el formulario viejo. Taller QA Central conserva sus valores y versión 0.
  Se registra superada esta prueba real de formulario desactualizado.
- Se prepara la comprobación de capacidad mediante dos bicicletas QA ubicadas
  en Plaza QA Centro. Se utilizará la interfaz actual, sin anticipar que el
  catálogo de tipos/tarifas del DERv5 ya esté migrado. Primero se solicitará
  al usuario crear dos unidades con códigos diferentes y luego intentar reducir
  la capacidad por debajo de la ocupación. Todavía no se declara realizada.
- Una consulta inicial de inventario intentó leer ubicacion_actual_id, que
  no es una columna de esta tabla. No modificó datos; se revisó el mapeo
  @JoinColumn(name="estacion_id") y se corrigió la consulta de solo lectura.
- Solo se leyeron código/documentación, se hicieron consultas de lectura y se
  actualizó el diario. No hubo escrituras SQL, cambios de código/DER/permisos,
  reinicios, Gradle, peticiones autenticadas ni modificaciones de Git.

### Etapa 91 Bicicletas QA verificadas y reducción de capacidad preparada

Fecha: 7 de noviembre de 2026.

- El usuario informó que creó ambas bicicletas. SELECT READ ONLY confirmó
  QA-01 id 1 y QA-02 id 2, ambas MONTANA, DISPONIBLE, borrado_bici=0 y
  estacion_id=1 (Plaza QA Centro). Los precios guardados son USD 10.00 y
  USD 8.00 respectivamente; esa diferencia no afecta la prueba de ocupación
  ni se interpreta como aplicación de la tarifa por tipo del DERv5 pendiente.
- La consulta de ocupación física, sin excluir registros archivados, confirmó
  dos bicicletas presentes, capacidad 2 y version=2 para Plaza QA Centro.
- Sigue recargar Administración para descartar el formulario anterior, abrir
  una edición nueva y cambiar solo capacidad a 1. Se espera rechazo por las
  dos bicicletas presentes y conservación de capacidad 2. La prueba de rechazo
  real todavía no se considera ejecutada ni superada.
- Solo se hicieron consultas de lectura y se actualizó este diario. No se
  modificaron datos, fuentes, DER, permisos, staging o commits ni se ejecutaron
  reinicios, Gradle o peticiones autenticadas por el asistente.

### Etapa 92 Reducción bajo ocupación rechazada y sin cambios persistidos

Fecha: 7 de noviembre de 2026.

- El usuario confirmó que reducir capacidad de 2 a 1 se rechazó por las dos
  bicicletas presentes. SELECT READ ONLY posterior confirmó Plaza QA Centro
  con capacidad 2, version=2, dirección Calle de Prueba 130 y es_publica=1,
  y dos unidades físicas. No hubo modificación parcial ni incremento de
  versión por el intento rechazado. Se considera superada esta prueba manual.
- El inventario consultado conserva QA-01 y QA-02 en estacion_id=1, sin borrado
  lógico. No hay una bicicleta QA-03 dentro de los códigos consultados.
- Sigue cerrar el formulario rechazado e intentar crear QA-03 (MONTANA,
  DISPONIBLE, USD 10/h) en Plaza QA Centro, que ya está llena. El controlador
  vigente verifica ocupación física y responde conflicto sin guardar cuando
  no quedan espacios. Se espera rechazo y ausencia de esa nueva fila; aún
  no se considera probado manualmente.
- Solo se realizaron SELECT, lectura del controlador y actualización del
  diario. No hubo escrituras SQL, peticiones autenticadas, reinicios, Gradle,
  cambios de fuentes/DER/permisos ni modificaciones de Git.

### Etapa 93 Alta en estación llena rechazada sin crear la bicicleta

Fecha: 7 de noviembre de 2026.

- El usuario confirmó que intentar crear QA-03 en Plaza QA Centro se rechazó
  por falta de espacios libres. SELECT READ ONLY confirmó cero registros con
  codigo_bicicleta='QA-03' y que Plaza QA Centro conserva capacidad 2,
  version=2, borrado_estacion=0 y dos bicicletas físicamente presentes.
  Se registra superada la prueba manual de alta con capacidad completa.
- Se prepara la baja lógica de estación ocupada: cerrar el formulario de alta
  rechazado y solicitar eliminar únicamente Plaza QA Centro, confirmando la
  acción. Se espera rechazo por bicicletas asignadas y conservación de la
  estación activa. No se propone eliminar Taller QA Central ni se presenta
  el rechazo de baja como ya comprobado en esta ejecución.
- Solo se realizaron SELECT y lecturas del controlador/manejador frontend de
  baja, y se actualizó el diario. No hubo escrituras SQL ni peticiones de
  borrado por el asistente, cambios de código/DER, reinicios, Gradle o Git.

### Etapa 94 Baja de estación ocupada rechazada y baja vacía preparada

Fecha: 7 de noviembre de 2026.

- El usuario confirmó que la baja de Plaza QA Centro se rechazó por contener
  bicicletas. SELECT READ ONLY confirmó borrado_estacion=0, capacidad 2,
  version=2 y dos unidades físicas. QA-01 y QA-02 conservan estacion_id=1
  y borrado_bici=0. Taller QA Central permanece activo, interno y vacío.
  Se considera superado el rechazo manual de baja con ocupación física.
- Sigue la última prueba de esta ronda del CRUD actual de estaciones: crear
  una estación pública descartable Estación QA Descartable, Calle de Prueba
  300, capacidad 1, coordenadas -53.7845/-67.7065, sin bicicletas, y solicitar
  su baja lógica desde el panel. Se conservarán Plaza QA Centro y Taller QA
  Central como datos QA para el trabajo siguiente. Se espera desaparición de
  la lista operativa y conservación de la fila con borrado_estacion=1; la
  acción y su persistencia todavía no se consideran realizadas.
- Esta ronda no acredita que estén migradas todas las condiciones futuras
  del DERv5 (reservas, incidencias y auditoría pendientes). Se comprobará la
  baja lógica efectiva del CRUD vigente, no una eliminación física autorizada.
- Solo se hicieron consultas de lectura y se actualizó este diario. No se
  ejecutaron escrituras SQL, solicitudes DELETE ni cambios de código/DER,
  permisos, reinicios, Gradle, staging o commits por el asistente.

### Etapa 95 Baja lógica de estación vacía verificada y ronda manual cerrada

Fecha: 7 de noviembre de 2026.

- El usuario informó que creó la estación descartable, ejecutó su baja y
  dejó de verla en la tabla administrativa. La primera consulta por el nombre
  sugerido Estación QA Descartable no encontró esa fila; una lectura de todas
  las estaciones confirmó que el nombre realmente ingresado es QA Descartable.
- SELECT READ ONLY confirmó la conservación de la fila id 3, nombre QA
  Descartable, dirección Calle de Prueba 300, capacidad 1, es_publica=1,
  borrado_estacion=1 y version=1. Hay tres estaciones en total y una dada de
  baja. Por tanto se comprobó baja lógica, no eliminación física.
- Plaza QA Centro permanece activa, pública, con dirección Calle de Prueba
  130, capacidad 2 y version=2; Taller QA Central permanece activo, interno,
  con capacidad 5 y version=0. No se alteraron las dos ubicaciones anteriores.
- GET público /api/estaciones devolvió HTTP 200 y solo Plaza QA Centro,
  bicicletasDisponibles=2 y espaciosLibres=0. Excluye tanto el taller interno
  como la estación dada de baja. La ausencia en el listado administrativo se
  registra como observación manual del usuario, no como GET autenticado del
  asistente ni inspección automatizada del navegador.
- Se cierra esta ronda manual del CRUD vigente de estaciones: alta pública e
  interna, mapa, columna Uso, edición normal, formulario desactualizado,
  rechazo de capacidad bajo ocupación, alta de bicicleta con estación llena,
  rechazo de baja ocupada y baja lógica vacía. Las pruebas básicas de sesión
  asociadas también están asentadas. No se declara completado todo el DERv5,
  la concurrencia real de dos ingresos a la vez, la expiración de treinta
  minutos ni los módulos de reservas/auditoría pendientes de implementación.
- Solo se realizaron consultas SQL de lectura, un GET público y actualización
  del diario. No se modificaron fuentes, DER, permisos o datos ni se ejecutaron
  reinicios, Gradle, peticiones DELETE, staging o commits por el asistente.

### Etapa 96 Punto de guardado propuesto y siguiente bloque del DERv5

Fecha: 7 de noviembre de 2026.

- Tras cerrar la ronda manual, se revisaron rama y estado local: continúa
  codex/refactorizacion-practicas-iii, con cambios de estaciones, sesión,
  inventario, pruebas y documentación todavía sin registrar juntos en un
  commit. Existen archivos nuevos y archivos parcialmente preparados en el
  staging. No se modificó ese estado mediante comandos de escritura Git.
- Se propone al usuario registrar el avance probado en un commit y subirlo
  antes de iniciar otro bloque. Todavía no se declara realizado el commit,
  el push ni la sincronización remota; se esperará su resultado.
- Se contrastó el siguiente punto con el DERv5 y el documento 26: TIPO_BICICLETA
  tiene id, codigo único, precio_por_hora_usd e intervalo_preventivo_meses.
  La tarifa por tipo está aprobada; los valores concretos de tarifas e
  intervalos, y aspectos físicos aún señalados, no se fijan sin definición.
  El archivo TipoBicicleta.java actual todavía es enum, usado por el modelo
  anterior. No se lo sustituye en este turno ni se crean clases provisionales.
- Se actualizó el índice para reflejar el bloque verificado, el alcance
  parcial de RN-INV-01 y la ausencia actual de CodigoRol en src. Las frases
  históricas sobre implementación se conservan identificadas como antecedentes.
- Solo se hicieron lecturas de fuentes/DER y consultas de Git y se editaron
  documentos. No se modificaron Java/JS/HTML, datos o permisos ni se ejecutaron
  pruebas, reinicios, git add/commit/push o SQL.

### Etapa 97 Punto de guardado confirmado y primer paso técnico de tipos

Fecha: 7 de noviembre de 2026.

- El usuario aportó git status con working tree clean y rama actualizada
  respecto de origin/codex/refactorizacion-practicas-iii. La lectura local
  confirmó árbol limpio antes de esta entrada y último commit f6d684f,
  Adaptar estaciones públicas e internas y corregir manejo de sesión.
  No se ejecutó fetch ni se afirmó una comprobación independiente del servidor
  remoto. Esta actualización del diario es posterior a ese punto de guardado.
- Se revisaron los consumidores del enum TipoBicicleta: modelo Bicicleta,
  repositorio, controlador, una importación de estadísticas y pruebas de
  bicicleta/alquiler. El DERv5 necesita TipoBicicleta como entidad, no enum.
- Se indica como primer paso manual usar Refactor/Rename para renombrar el
  enum existente a CodigoTipoBicicleta, incluyendo archivo y referencias de
  main/test, sin cambiar MONTANA/PASEO/ELECTRICA. Es un paso técnico transitorio
  para liberar el nombre de la entidad definitiva y conservar compilación;
  no es un catálogo nuevo, una duplicación ni el diseño final. Se retirará
  al adaptar sus consumidores a la entidad y sus códigos.
- No se implementó el renombrado por el asistente. Se propone comprobarlo
  con BicicletaControllerTest y AlquilerServiceTest, que además compilan main
  y test. No se declara compilado ni probado hasta recibir/verificar el resultado.
  No se fijan tarifas/intervalos ni se cambian DER, estados o reglas aprobadas.
- Solo se realizaron lecturas y consultas Git y se editó este diario. No
  hubo modificaciones Java/JS, escrituras Git/SQL, reinicios o pruebas.

### Etapa 98 Renombrado puntual del enum de tipo implementado

Fecha: 7 de noviembre de 2026.

- El usuario autorizó al asistente implementar únicamente el renombrado de
  la Etapa 97, reservándose expresamente la parte 2 de comprobación.
- Se renombró models/TipoBicicleta.java a models/CodigoTipoBicicleta.java y
  su declaración public enum. Se mantienen MONTANA, PASEO y ELECTRICA con
  sus comentarios originales, sin copiar el enum ni crear la entidad nueva.
- Se actualizaron solo las referencias de tipo/importaciones en Bicicleta
  (campo, constructor, getter y setter), BicicletaRepository,
  BicicletaController y EstadisticasService; se ajustó la referencia del
  comentario de AlquilerRepository. También se actualizaron las referencias
  del mismo símbolo en BicicletaControllerTest y AlquilerServiceTest.
  No se modificaron reglas, consultas, rutas, valores, anotaciones JPA ni
  comportamiento de las pruebas. Son ocho archivos fuente contando el movido.
- Se compararon las fuentes con una captura previa: solo cambia el símbolo
  TipoBicicleta por CodigoTipoBicicleta, normalizando finales de línea y el
  salto final que apply_patch agregó en algunos archivos. No quedan referencias
  al símbolo antiguo dentro de src y no existe el archivo con el nombre previo.
  git diff --check no informa errores. Esta es revisión estática, no compilación.
- Se actualizó el índice para reflejar el nombre transitorio y la verificación
  pendiente. No se cambió DER, tabla/columna, frontend, tarifa ni intervalo.
  No se ejecutaron Gradle, compilación o pruebas, consultas/escrituras SQL,
  reinicios ni git add/commit/push. El estado previo del diario se conserva.
- Queda para el usuario ejecutar:
  .\gradlew.bat test --tests "BicicletaControllerTest" --tests "AlquilerServiceTest"
  Después se revisará su resultado antes de crear la entidad TipoBicicleta.

### Etapa 99 Renombrado probado y validación de tarifa pendiente de confirmar

Fecha: 7 de noviembre de 2026.

- El usuario ejecutó la parte 2: Gradle test filtrado a BicicletaControllerTest
  y AlquilerServiceTest, con BUILD SUCCESSFUL en ocho segundos y cuatro tareas
  ejecutadas. Se leyeron los XML: 6 y 4 pruebas respectivamente, cero fallos,
  errores u omisiones, timestamps 2026-11-07T23:10:53.350Z/.915Z. Se confirma
  el renombrado compilado y probado, sin repetir la ejecución por el asistente.
- Antes de proponer la validación de la entidad se contrastaron DERv5,
  documentos 26, 05 y 29, el texto del diccionario original y las páginas
  completas 9/10 del PDF de visión. Se utilizaron las guías de lectura de
  PDF/Word y extracción de texto, sin editar ni generar documentos binarios
  o evaluar su maquetación. No se confunden estados originales reemplazados
  por decisiones posteriores con el diseño vigente.
- El documento 26 deja pendiente la política de tarifa cero y precisión física
  del catálogo. Se propone para aprobación que precioPorHoraUsd sea estrictamente
  positivo, con hasta dos decimales (mínimo USD 0.01/h), sin tarifas gratuitas
  en esta etapa. No se presenta esa restricción como ya aprobada ni implementada.
  No modifica el cálculo por segundos, el redondeo final ni la congelación
  ya aprobados; tampoco fija importes concretos por tipo o mínimos de pasarela.
- El intervalo preventivo entero positivo en meses ya está previsto en el
  DER y sus valores iniciales siguen pendientes; no se inventan calendarios.
  Se esperará la definición de tarifa antes de dar el bloque de entidad completo.
- Solo se leyeron fuentes, documentos y XML de pruebas y se actualizaron el
  índice y este diario. No se crearon modelos, repositorios, servicios o
  controladores, ni se ejecutaron Gradle, SQL, reinicios o escrituras Git.

### Etapa 100 Tarifa positiva aprobada y entidad TipoBicicleta propuesta

Fecha: 7 de noviembre de 2026.

- El usuario aprobó tarifa USD/h mayor que cero, hasta dos decimales y rechazo
  de cero, negativos y exceso de decimales. Se incorporó la aprobación en los
  documentos 05/26 y el comentario del campo en Mermaid/TXT, sin alterar campos,
  claves, cardinalidades, estados, reglas de cálculo ni importes iniciales.
- Se prepara para creación manual models/TipoBicicleta.java como @Entity con
  @Table(name="tipo_bicicleta") y los cuatro campos del DER: Integer id IDENTITY,
  codigo obligatorio/único (VARCHAR 20, trim y mayúsculas Locale.ROOT),
  BigDecimal precioPorHoraUsd obligatorio, @DecimalMin("0.01") y @Digits(8,2),
  columna DECIMAL(10,2), e Integer intervaloPreventivoMeses obligatorio/positivo.
  El límite técnico de ocho dígitos enteros se explicita en la propuesta, no
  se presenta como precio comercial aprobado.
- El bloque incluye constructor vacío JPA, constructor de los tres datos y
  getters/setters sin setter de id. No redondea ni inventa precios/intervalos,
  no agrega FK/colecciones inversas ni version fuera del DER y no convierte
  el enum transitorio nuevamente en entidad.
- El asistente no creó la fuente: queda para el usuario pegar/guardar el
  código propuesto y ejecutar las dos clases filtradas de pruebas. Bicicleta
  continúa usando CodigoTipoBicicleta hasta el siguiente bloque de adaptación;
  no se afirma que crear la clase migre datos o cambie ya la tarifa vigente.
- No hace falta reiniciar el perfil create-drop para compilar esta clase; no
  se provoca una pérdida de datos QA para esta comprobación. La tabla se
  gestionará al iniciar JPA cuando corresponda ejecutar el siguiente modelo.
- Solo se actualizaron los cuatro documentos de diseño/DER y este diario.
  No se modificaron fuentes, datos, permisos, staging o commits ni se ejecutaron
  Gradle, SQL, reinicios o peticiones de aplicación. La nueva entidad y sus
  validaciones todavía no se consideran implementadas ni probadas.

### Etapa 101 Entidad TipoBicicleta revisada y prueba de validaciones propuesta

Fecha: 7 de noviembre de 2026.

- El usuario creó TipoBicicleta.java y aportó BUILD SUCCESSFUL en cuatro
  segundos de las dos clases filtradas. La lectura de la fuente confirmó
  ubicación/package, @Entity/@Table, los cuatro campos, sus validaciones,
  normalización del código y constructores/getters/setters como se propusieron.
  No se detectaron bloques faltantes o cambios de comportamiento respecto
  del código entregado. CodigoTipoBicicleta sigue presente como enum transitorio.
- Se verificaron XML de BicicletaControllerTest (6) y AlquilerServiceTest (4),
  timestamps 2026-11-07T23:29:24.079Z/.748Z, cero fallos, errores u omisiones.
  Esas pruebas compilan la entidad, pero no ejecutan sus validaciones nuevas,
  crean tablas ni acreditan la conexión del catálogo con Bicicleta.
- Se propone para implementación manual src/test/java/DSII/TurismoSustentable/
  models/TipoBicicletaTest.java: seis pruebas JUnit 5 con Validator real para
  código válido/normalizado, códigos obligatorios/longitud, precios válidos
  y extremos técnicos, rechazo de precios inválidos/nulos, ausencia de redondeo
  silencioso e intervalo entero positivo obligatorio. Los importes e intervalos
  de las pruebas son fixtures, no tarifas o calendarios comerciales aprobados.
- No se creó la clase de pruebas por el asistente ni se presenta como superada.
  Sigue que el usuario la pegue/guarde y ejecute el filtro indicado. No usa
  @SpringBootTest, aplicación, DB ni requiere reiniciar el perfil create-drop.
- Solo se leyeron fuentes/XML y se actualizó este diario; no se modificaron
  fuentes, datos o permisos ni se ejecutaron Gradle, SQL, reinicios o Git.

### Etapa 102 Validaciones de TipoBicicleta comprobadas y repositorio propuesto

Fecha: 7 de noviembre de 2026.

- Se revisó src/test/java/DSII/TurismoSustentable/models/TipoBicicletaTest.java:
  está en la carpeta/package de pruebas correcto, con seis @Test, Validator
  real, apertura/cierre de ValidatorFactory y todos los bloques propuestos.
  La entidad conserva los cuatro campos y sus anotaciones sin cambios.
- El usuario ejecutó el filtro de tres clases con BUILD SUCCESSFUL en tres
  segundos. Se comprobaron los XML: TipoBicicletaTest 6 pruebas, timestamp
  2026-11-07T23:37:04.666Z; BicicletaControllerTest 6 pruebas, .956Z del segundo
  23:37:03; AlquilerServiceTest 4 pruebas, 23:37:04.741Z. Total 16, cero fallos,
  errores u omisiones. No se volvió a ejecutar la suite por el asistente.
- Quedan comprobadas normalización de código, datos obligatorios, longitudes,
  tarifa positiva/escala/límite técnico, ausencia de redondeo silencioso e
  intervalo positivo. Estas pruebas no acreditan DDL/UNIQUE en DB ni persistencia
  o uso de la nueva entidad por Bicicleta, que aún utiliza el enum transitorio.
- Se propone crear manualmente TipoBicicletaRepository en repositories,
  extendiendo JpaRepository<TipoBicicleta, Integer>, con findByCodigo(String)
  y retorno Optional<TipoBicicleta>, siguiendo los catálogos existentes. No se
  agregan métodos de administración o bloqueo todavía sin un consumidor concreto.
  El asistente no creó el repositorio ni lo declara compilado o integrado.
- Siguen pendientes la precarga con tarifas/intervalos iniciales definidos
  expresamente y la adaptación de consumidores; no se toman los fixtures del
  test como valores de catálogo ni se reinicia create-drop para esta comprobación.
- Solo se leyeron fuentes y XML y se actualizó este diario. No se modificaron
  Java/JS, DB, permisos o DER ni se ejecutaron Gradle, SQL, reinicios o Git.

### Etapa 103 Repositorio revisado y valores iniciales del catálogo propuestos

Fecha: 7 de noviembre de 2026.

- Se revisó TipoBicicletaRepository.java: package/importaciones correctos,
  JpaRepository<TipoBicicleta, Integer> y findByCodigo(String) con Optional,
  sin bloques faltantes o cambio de la propuesta. Se conserva su estado de
  Git parcialmente preparado por el usuario, sin modificar staging.
- El usuario aportó BUILD SUCCESSFUL en cuatro segundos. XML verificados:
  TipoBicicletaTest 6, BicicletaControllerTest 6 y AlquilerServiceTest 4,
  timestamps 2026-11-07T23:44:17.548Z, 23:44:16.753Z y 23:44:17.693Z.
  Total 16 pruebas, cero fallos, errores u omisiones. No se ejecutó Gradle
  por el asistente. Esto no acredita todavía la persistencia del repositorio.
- Se revisó CargaCatalogosRefactor: carga actual de roles, géneros,
  documentos y países. Se propone ampliarla después de aprobar valores,
  reutilizando findByCodigo y guardando solo tipos ausentes, sin sobrescribir
  tarifas/intervalos existentes ni crear otra carga inicial paralela.
- Para el entorno de desarrollo refactor se proponen valores iniciales,
  todavía no aprobados: MONTANA USD 10.00/h y 3 meses; PASEO USD 8.00/h y
  2 meses; ELECTRICA USD 15.00/h y 1 mes. No se obtienen promediando precios
  por bicicleta ni se presentan como tarifa real de mercado, recomendación
  técnica de fabricante o intervalo obligatorio definitivo. Serán parámetros
  del catálogo, sin modificar el cálculo/contratos existentes por esta propuesta.
- Se esperará la decisión del usuario antes de indicar los bloques de carga.
  Solo se leyeron fuentes/XML y se actualizó el diario; no se modificaron
  fuentes, valores de DB, reglas/DER, permisos, staging o commits ni se
  ejecutaron SQL, reinicios, pruebas o peticiones de la aplicación.

### Etapa 104 Valores QA aprobados y ampliación puntual de carga propuesta

Fecha: 7 de noviembre de 2026.

- El usuario aprobó MONTANA 10.00 USD/h y 3 meses, PASEO 8.00 y 2 meses,
  ELECTRICA 15.00 y 1 mes, expresamente para las pruebas. Se registraron en
  documento 26 y se actualizó el comentario del intervalo en ambos DER, sin
  cambiar campos/claves/relaciones. No se toman como definición comercial.
- Se releyeron completos CargaCatalogosRefactor y CargaCatalogosRefactorTest.
  Se propone agregar tres importaciones, una dependencia final y su parámetro
  de constructor, tres llamadas dentro de run y un helper crearTipoBicicletaSiFalta
  que convierte precios String con BigDecimal y solo guarda códigos ausentes.
  Se conservan perfil refactor, @Transactional y los otros cuatro catálogos.
- Como parte necesaria del mismo cambio manual se indica agregar el mock de
  TipoBicicletaRepository al test existente: @InjectMocks debe recibir la nueva
  dependencia. Los imports comodín actuales ya incluyen ese repositorio. No
  hay construcciones manuales del cargador que requieran cambiar argumentos.
- Se indicará ejecutar CargaCatalogosRefactorTest junto con los tres filtros
  anteriores. Esto comprueba que no se rompan sus pruebas de países; la
  comprobación específica de nuevos tipos/idempotencia se añadirá después
  de revisar el bloque. No se presenta como persistencia real ya probada.
- Solo se actualizaron los documentos mencionados. El asistente no editó el
  cargador ni las pruebas, ni ejecutó Gradle, SQL, reinicios o escrituras Git.
  El usuario sigue a cargo de pegar, guardar y comprobar; no debe reiniciar
  create-drop para esta comprobación ni perder los datos QA existentes.

### Etapa 105 Precarga de tipos implementada por autorización puntual

Fecha: 7 de noviembre de 2026.

- El usuario autorizó implementar exclusivamente los puntos 1 a 4 de la
  propuesta, reservándose la parte 5 de guardar y comprobar.
- En CargaCatalogosRefactor se agregaron las importaciones de TipoBicicleta,
  su repositorio y BigDecimal, la dependencia final y su inyección por
  constructor. Se incorporaron al comienzo de run las tres llamadas con los
  valores aprobados para pruebas: MONTANA 10.00/3, PASEO 8.00/2 y ELECTRICA 15.00/1.
- Se agregó crearTipoBicicletaSiFalta fuera de run y antes del helper de
  documentos. Busca por código y solo guarda ausentes; construye BigDecimal
  desde String, sin usar double ni sobrescribir precios/intervalos existentes.
  Se conservaron @Profile("refactor"), @Transactional y las cargas de roles,
  géneros, documentos y países sin cambiar su contenido.
- En CargaCatalogosRefactorTest solo se agregó @Mock del nuevo repositorio
  antes de @InjectMocks. No se alteraron los tests existentes ni se agregaron
  todavía pruebas específicas de datos/idempotencia del nuevo catálogo.
- Se compararon ambos archivos con una captura previa, normalizando finales
  de línea/salto final: solo difieren los bloques propuestos y esa declaración
  @Mock. git diff --check sin errores. Esta es revisión estática, no ejecución
  del cargador, compilación o pruebas.
- Se deja al usuario ejecutar CargaCatalogosRefactorTest, TipoBicicletaTest,
  BicicletaControllerTest y AlquilerServiceTest mediante los filtros indicados.
  Los archivos ya se editaron en disco; la confirmación de parte 5 sigue pendiente.
- Solo se modificaron esos dos archivos fuente y este diario. No se editaron
  otros modelos, servicios, APIs, frontend, permisos o DER, ni se ejecutaron
  Gradle, SQL, reinicios, peticiones de aplicación o escrituras Git. No se
  cargaron tipos en la DB de la aplicación que sigue ejecutándose, ni se
  cambiaron los precios todavía utilizados por el modelo anterior.

### Etapa 106 Precarga compilada y pruebas específicas propuestas

Fecha: 7 de noviembre de 2026.

- El usuario ejecutó la parte 5 con BUILD SUCCESSFUL en siete segundos.
  Se verificaron XML: CargaCatalogosRefactorTest 2, TipoBicicletaTest 6,
  BicicletaControllerTest 6 y AlquilerServiceTest 4; total 18 pruebas, sin
  fallos, errores u omisiones. Timestamps UTC 2026-11-08T00:19:16.229Z y
  00:19:17.512Z/.348Z/.922Z (todavía 7 de noviembre en Argentina).
- Se releyeron los bloques de carga y el test actual: mantienen los valores
  y el mock propuestos. Las dos pruebas del cargador existentes comprueban
  países/perfil; todavía no aseveran los valores ni la idempotencia de tipos.
- Se proponen dos @Test nuevos en el mismo archivo, sin reemplazar los
  existentes: capturar los tres tipos guardados y comprobar códigos, tarifas
  e intervalos aprobados; simular un MONTANA existente con otros valores,
  ejecutar dos veces y verificar que se conservan y solo se agregan los otros
  dos tipos una vez. Los valores distintos del existente son fixtures para
  demostrar no sobrescritura, no cambios al catálogo aprobado.
- Se indicarán solo los imports de ArgumentCaptor/BigDecimal y los dos métodos,
  más el comando filtrado. No se han agregado por el asistente ni se declaran
  superadas estas pruebas específicas o la carga en una DB real.
- Solo se leyeron fuentes/XML y se actualizó este diario. No se ejecutaron
  Gradle, SQL, reinicios, peticiones, cambios de código/DER o escrituras Git.

### Etapa 107 Pruebas específicas de precarga agregadas por autorización puntual

Fecha: 7 de noviembre de 2026.

- El usuario autorizó implementar únicamente los puntos 1/2 de la propuesta
  de la Etapa 106 y se reservó la parte 3 de guardar y comprobar.
- Solo se editó CargaCatalogosRefactorTest.java: imports de ArgumentCaptor
  y BigDecimal, más cargaTiposConLosValoresAprobados y
  repetirLaCargaNoDuplicaNiSobrescribeTiposExistentes. La clase conserva sus
  dos pruebas anteriores, mocks, @BeforeEach y configuración de Mockito.
- La primera nueva prueba captura las tres llamadas a save y verifica los
  códigos, precios 10.00/8.00/15.00 e intervalos 3/2/1 aprobados. La segunda
  simula en memoria un MONTANA existente con 11.50 y 4 meses, ejecuta la carga
  dos veces y exige conservarlo, guardar solo los otros dos tipos una vez y
  no eliminar tipos. Esos valores alternativos son fixtures de no sobrescritura.
- Se comparó el archivo con una captura previa, normalizando finales de
  línea/salto final: solo se agregaron los dos imports y métodos propuestos.
  Se verificaron cuatro @Test y git diff --check sin errores. Es revisión
  estática; no se declara compilado ni superado el nuevo bloque.
- La parte 3 queda para el usuario: ejecutar los cuatro filtros indicados
  (CargaCatalogosRefactorTest, TipoBicicletaTest, BicicletaControllerTest y
  AlquilerServiceTest). Los archivos están editados en disco; no se necesita
  reiniciar el perfil create-drop para comprobarlos.
- Además de este diario, solo cambió el archivo de prueba mencionado. No se
  modificaron cargador, modelos, reglas, datos, permisos o DER, ni se ejecutaron
  Gradle, SQL, reinicios, peticiones de aplicación o escrituras Git.

### Etapa 108 Precarga de tipos probada y preparación del catálogo de estados

Fecha: 7 de noviembre de 2026.

- El usuario aportó BUILD SUCCESSFUL en tres segundos de los cuatro filtros.
  XML verificados: CargaCatalogosRefactorTest 4, TipoBicicletaTest 6,
  BicicletaControllerTest 6 y AlquilerServiceTest 4; total 20 pruebas, cero
  fallos, errores u omisiones, timestamps UTC 2026-11-08T00:23:11.835Z y
  00:23:12.773Z/.675Z/.794Z (7 de noviembre local). Quedan verificadas en
  memoria la precarga de valores y la no sobrescritura/duplicación de tipos.
  No se acredita todavía DDL/UNIQUE/persistencia de JPA ni aplicación de la
  tarifa por tipo al modelo Bicicleta, que sigue en la versión anterior.
- Se contrastó el siguiente paso con RN-BIC-01 y el DERv5: Bicicleta necesita
  dos FK a los catálogos de tipo y estado. El tipo ya tiene clase/repositorio
  y precarga; se prepara EstadoBicicleta antes de adaptar esas relaciones.
  Los siete códigos aprobados siguen siendo DISPONIBLE, RESERVADA, EN_USO,
  MANTENIMIENTO, INACTIVA, DE_BAJA y EXTRAVIADA. No se proponen otros estados.
- Primer paso técnico manual: renombrar el enum antiguo EstadoBicicleta a
  CodigoEstadoBicicleta, archivo y referencias main/test mediante Refactor/Rename,
  conservando todos sus valores actuales, incluido ALQUILADA. No sustituir
  ALQUILADA por EN_USO ni añadir estados en esta operación, pues eso exige
  adaptar conjuntamente consumidores y reglas en el bloque posterior.
  El enum será transitorio, no la representación del catálogo final.
- El asistente no hizo ese renombrado ni creó otra entidad en este turno.
  Se propone mantener los cuatro filtros de pruebas para comprobarlo. No se
  cambia DER, reglas o datos ni se reinicia el perfil create-drop.
- Solo se leyeron fuentes, decisiones y XML y se actualizó el diario. No hubo
  modificaciones Java/JS, ejecuciones Gradle/SQL, reinicios o escrituras Git.

### Etapa 109 Enum antiguo de estados renombrado por autorización puntual

Fecha: 7 de noviembre de 2026.

- El usuario autorizó implementar exclusivamente el primer paso de la Etapa
  108, reservándose la parte 2 de guardar y comprobar.
- Se movió models/EstadoBicicleta.java a models/CodigoEstadoBicicleta.java y
  se renombró su declaración public enum. Se conservaron exactamente los
  valores DISPONIBLE, ALQUILADA, MANTENIMIENTO e INACTIVA, sin añadir estados
  ni sustituir ALQUILADA por EN_USO antes de adaptar el modelo y sus consumidores.
- Se actualizaron las referencias del símbolo en Bicicleta, BicicletaRepository
  (incluidas las referencias de comentarios), BicicletaController,
  EstadisticasService, AlquilerService, EstacionDTO, BicicletaControllerTest y
  AlquilerServiceTest. Son nueve archivos fuente incluyendo el enum movido.
- Se compararon contra una captura previa, normalizando finales de línea/salto
  final: solo cambia EstadoBicicleta por CodigoEstadoBicicleta. Se conservaron
  las modificaciones anteriores del usuario. No quedan referencias al símbolo
  antiguo en src ni el archivo con el nombre previo; git diff --check sin errores.
- No se creó la entidad EstadoBicicleta, no se modificaron transiciones,
  importes, datos, frontend, consultas, anotaciones JPA o DER. El renombrado
  del tipo Java no migra los valores ni cambia la representación SQL/JSON.
- No se ejecutaron compilación, Gradle o pruebas: la comprobación de los
  cuatro filtros queda expresamente a cargo del usuario. Tampoco hubo SQL,
  reinicios, solicitudes de aplicación o escrituras de staging/commits.
- Además de este diario solo se editaron las fuentes relacionadas con el
  renombrado. La validación realizada por el asistente fue estática; no se
  presenta el nuevo bloque como compilado o probado hasta verificar la parte 2.

### Etapa 110 Renombrado de estados comprobado y catálogo definitivo propuesto

Fecha: 7 de noviembre de 2026.

- El usuario aportó BUILD SUCCESSFUL en cinco segundos. Los XML confirman
  CargaCatalogosRefactorTest 4, TipoBicicletaTest 6, BicicletaControllerTest 6 y
  AlquilerServiceTest 4: veinte pruebas, cero fallos, errores u omisiones.
  Timestamps UTC 2026-11-08T00:33:25.342Z y 00:33:26.271Z/.171Z/.291Z,
  todavía 7 de noviembre en Argentina. No se repitieron las pruebas.
- Se revisó el enum CodigoEstadoBicicleta con sus cuatro valores originales,
  ausencia del archivo/símbolo antiguo en src y catálogo del DER/RN-BIC-01.
  El renombrado está compilado y probado; no es aún la migración a FK.
- Se propone crear manualmente EstadoBicicleta.java como @Entity con tabla
  estado_bicicleta y únicamente id Integer IDENTITY/codigo único obligatorio,
  VARCHAR(20), normalizado trim/mayúsculas Locale.ROOT. @Pattern restringe el
  código a los siete estados ya aprobados; no añade otros ni admite ALQUILADA
  en el nuevo catálogo. Constructor vacío JPA y constructor de código,
  getters/setter de código; sin setter de id, enum nuevo o relaciones inversas.
- En el mismo bloque de catálogo se propone EstadoBicicletaRepository,
  JpaRepository<EstadoBicicleta, Integer> y findByCodigo con Optional, siguiendo
  RolRepository. Se verificó que ese archivo no existe antes de la propuesta.
- El código anterior seguirá usando CodigoEstadoBicicleta; las relaciones de
  Bicicleta, las transiciones, los valores persistidos y el frontend se adaptarán
  conjuntamente después. No se cambian ahora los valores del enum transitorio.
  Crear las dos clases no equivale a precargar siete filas ni migrar FK.
- El usuario creará/guardará los dos archivos y comprobará los mismos cuatro
  filtros, sin reiniciar create-drop. Esa comprobación compila el nuevo bloque
  pero no prueba todavía su validación, repositorio JPA o carga real en DB.
- Solo se leyeron fuentes/DER/decisiones y XML y se actualizó el diario. No
  se crearon archivos Java ni se ejecutaron Gradle, SQL, reinicios o Git.

### Etapa 111 Entidad y repositorio de estados revisados; precarga propuesta

Fecha: 7 de noviembre de 2026.

- El usuario creó EstadoBicicleta y EstadoBicicletaRepository y aportó BUILD
  SUCCESSFUL en tres segundos. Se revisaron ambos archivos: ubicación/package,
  @Entity/@Table, id Integer IDENTITY, código único/obligatorio, longitud,
  @Pattern de siete estados, normalización Locale.ROOT, constructores y
  JpaRepository/Optional coinciden con la propuesta y no faltan bloques.
- XML verificados: CargaCatalogosRefactorTest 4, TipoBicicletaTest 6,
  BicicletaControllerTest 6 y AlquilerServiceTest 4; veinte pruebas, cero
  fallos, errores u omisiones. Timestamps UTC 2026-11-08T00:36:41.210Z y
  00:36:42.125Z/.031Z/.146Z, todavía 7 de noviembre local. No se repitió Gradle.
  El nuevo catálogo está compilado, no probado en persistencia o validación aún.
- Se propone ampliar CargaCatalogosRefactor con imports de entidad/repositorio,
  dependencia final e inyección por constructor; dentro de run, antes de roles,
  un arreglo de los siete códigos aprobados y un bucle que guarda solo ausentes.
  Se conserva el cargador existente, perfil refactor, transacción y otros catálogos.
- Se indica añadir el mock correspondiente en CargaCatalogosRefactorTest para
  mantener la inyección de su constructor. No se alteran sus cuatro métodos.
  No hay instanciaciones manuales del cargador que requieran ajustar parámetros.
- El enum CodigoEstadoBicicleta y sus consumidores permanecen intactos: este
  bloque no renombra ALQUILADA, modifica transiciones o migra FK. El usuario
  pegará/guardará y comprobará con los cuatro filtros, sin reiniciar create-drop.
  No se considera implementada ni ejecutada esta nueva precarga hasta revisión.
- Solo se leyeron fuentes/XML y se actualizó el diario; no se modificaron
  fuentes, datos, permisos o DER ni se ejecutaron SQL, Gradle, reinicios o Git.

### Etapa 112 Precarga de estados implementada por autorización puntual

Fecha: 7 de noviembre de 2026.

- El usuario autorizó implementar exclusivamente los puntos 1 a 4 de la
  propuesta de la Etapa 111 y se reservó la parte 5 de guardar/comprobar.
- En CargaCatalogosRefactor se agregaron imports de EstadoBicicleta y su
  repositorio, la dependencia final y el sexto parámetro/asignación del
  constructor, conservando las cinco dependencias anteriores.
- Dentro de run, después de los tres tipos y antes de roles, se agregó el
  arreglo DISPONIBLE, RESERVADA, EN_USO, MANTENIMIENTO, INACTIVA, DE_BAJA y
  EXTRAVIADA. El bucle consulta por código y guarda solo los estados ausentes.
  Se conservan perfil refactor, transacción y todos los demás catálogos.
- En CargaCatalogosRefactorTest solo se agregó el mock del nuevo repositorio
  antes de @InjectMocks. Se mantienen sus cuatro pruebas y preparación;
  los imports comodín existentes ya incluyen ese repositorio.
- Se compararon ambos archivos con una captura previa, normalizando finales
  de línea/salto final: solo difieren los imports, dependencia/constructor,
  nuevo bucle y mock previstos. git diff --check sin errores. Es revisión
  estática, no compilación ni prueba de la nueva precarga.
- No se alteraron CodigoEstadoBicicleta, consumidores, transiciones,
  relaciones, precios o estados de bicicletas existentes. Tampoco se crearon
  filas de catálogo en la aplicación que sigue abierta: no hubo reinicio.
- La parte 5 queda para el usuario: ejecutar CargaCatalogosRefactorTest,
  TipoBicicletaTest, BicicletaControllerTest y AlquilerServiceTest. Los cambios
  ya están en disco; no hace falta reiniciar create-drop para comprobarlos.
- Además de este diario, solo se editaron los dos archivos mencionados. No
  se ejecutaron Gradle, compilación, pruebas, SQL, peticiones, reinicios ni
  escrituras Git, ni se modificaron reglas, permisos o DER.

### Etapa 113 Precarga de estados compilada y comprobaciones específicas propuestas

Fecha: 7 de noviembre de 2026.

- El usuario ejecutó la parte 5 con BUILD SUCCESSFUL en cinco segundos.
  XML verificados: CargaCatalogosRefactorTest 4, TipoBicicletaTest 6,
  BicicletaControllerTest 6 y AlquilerServiceTest 4, total veinte sin fallos,
  errores u omisiones. Timestamps UTC 2026-11-08T00:40:57.984Z y
  00:40:59.442Z/.170Z/.581Z (7 de noviembre local). El asistente no repitió Gradle.
- Se revisaron el bucle de siete estados y el mock nuevo, sin cambios respecto
  de la propuesta. Las cuatro pruebas actuales del cargador no aseveran todavía
  sus códigos/idempotencia ni las validaciones del nuevo EstadoBicicleta.
- Se proponen dos métodos manuales adicionales en CargaCatalogosRefactorTest,
  con los imports ya disponibles: capturar los siete estados, contrastar el
  conjunto aprobado y validar entidades reales, normalización y rechazo de
  códigos nulos/blancos/desconocidos/ALQUILADA; simular DISPONIBLE existente,
  ejecutar dos cargas y verificar conservación de su objeto y solo seis saves.
  No se alterarán los cuatro tests previos ni la preparación de repositorios.
- Son pruebas aisladas de catálogo, no de transiciones, reservas, historial,
  JPA/UNIQUE o base real. La nueva entidad admite EN_USO, pero el enum legado
  continúa sin modificaciones hasta el bloque de migración de consumidores.
- No se han agregado esos métodos por el asistente ni se declaran superados.
  Solo se leyeron fuentes/XML y se actualizó este diario; no se modificaron
  fuentes/DER/datos ni se ejecutaron SQL, Gradle, reinicios o escrituras Git.

### Etapa 114 Pruebas específicas de estados agregadas por autorización puntual

Fecha: 7 de noviembre de 2026.

- El usuario autorizó implementar únicamente los dos métodos propuestos en
  la Etapa 113 y se reservó la parte 2 de guardar/comprobar.
- Solo se editaron dos métodos nuevos al final de CargaCatalogosRefactorTest:
  cargaSoloEstadosAprobadosYValidos captura siete saves, contrasta sus códigos,
  usa Validator real para validar entidades y normalización y rechaza códigos
  nulos/blancos/desconocidos, ALQUILADA y longitud excesiva;
  repetirLaCargaNoDuplicaNiReemplazaEstadosExistentes simula DISPONIBLE previo,
  ejecuta dos veces y exige siete estados, el mismo objeto original, seis saves
  y ninguna eliminación de estados. Los demás repositorios siguen simulados.
- Se conservan los cuatro tests anteriores, imports, mocks y @BeforeEach.
  Se comparó la fuente con una captura previa, normalizando finales de línea
  y salto final: las únicas diferencias son esos dos métodos. Se cuentan seis
  @Test y git diff --check no informa errores. No se ejecutó el archivo.
- Queda al usuario la parte 2: cuatro filtros CargaCatalogosRefactorTest,
  TipoBicicletaTest, BicicletaControllerTest y AlquilerServiceTest. No se
  declara compilado ni superado el bloque nuevo antes de recibir/verificar
  su resultado; los cambios están guardados en disco, sin necesidad de reinicio.
- Además de este diario solo cambió el archivo de prueba indicado. No se
  modificaron cargador, modelos, reglas, datos, permisos o DER ni se ejecutaron
  Gradle, compilación, SQL, reinicios, peticiones o escrituras de Git.

### Etapa 115 Catálogos preparados y nuevo punto de guardado propuesto

Fecha: 7 de noviembre de 2026.

- El usuario informó BUILD SUCCESSFUL de la parte 2. Se verificaron los XML:
  CargaCatalogosRefactorTest 6, TipoBicicletaTest 6, BicicletaControllerTest 6 y
  AlquilerServiceTest 4, total 22 pruebas, cero fallos, errores u omisiones.
  Timestamps UTC 2026-11-08T00:46:35.763Z y 00:46:36.693Z/.599Z/.713Z,
  todavía 7 de noviembre local. No se repitió su ejecución por el asistente.
- Quedan comprobados los valores/códigos de ambas precargas, no duplicación
  ni reemplazo de existentes y las validaciones de tipos/estados cubiertas por
  esos tests. Son pruebas con repositorios simulados y Validator real: no
  acreditan persistencia JPA, restricciones UNIQUE ni tablas reales nuevas.
- Los catálogos tienen entidad/repositorio/carga, pero Bicicleta sigue usando
  los dos enums transitorios y su precio propio anterior. El DER requiere
  tipo_bicicleta_id y estado_bicicleta_id, fecha_alta y version. El siguiente
  bloque adaptará el modelo y sus consumidores; no se modifica ahora una
  relación aislada que deje compilación o contratos HTTP inconsistentes.
- Git muestra cambios pendientes, incluidos archivos parcialmente preparados
  por el usuario, en codex/refactorizacion-practicas-iii. Se propone registrar
  este avance en un commit y subirlo antes de la adaptación de Bicicleta.
  No se ejecutaron add/commit/push ni se declara ese punto todavía realizado.
- Se actualizó el índice para reflejar el estado efectivo del catálogo y sus
  límites de verificación. Solo se hicieron lecturas de XML/DER y consultas Git
  y se editaron documentos, sin cambios de fuentes, DB, permisos o reglas,
  reinicios, Gradle, SQL, peticiones de aplicación o escrituras de Git.

### Etapa 116 Punto de guardado confirmado y entrada de alta de bicicleta propuesta

Fecha: 7 de noviembre de 2026.

- El usuario informó working tree clean y rama actualizada contra origin.
  La revisión local confirmó árbol limpio antes de esta anotación y último
  commit be1f66c, Preparar catalogos de tipos y estados de bicicleta.
  No se hizo fetch ni se afirma una comprobación independiente del remoto.
- Se revisaron Bicicleta, BicicletaDTO, el alta del controlador y los campos
  del DERv5. El controlador todavía recibe directamente una entidad con enums
  y precio por unidad. Cambiar únicamente un atributo a @ManyToOne rompería
  firmas, serialización DTO, consultas, pruebas y solicitudes del frontend.
- Se propone como primera pieza permanente CrearBicicletaDTO, en main/dto,
  para recibir codigoBicicleta (obligatorio, hasta 255 caracteres como la columna
  actual, trim de extremos sin introducir otro formato), tipoBicicletaId y
  estacionId (Integer obligatorios/positivos). No contiene entidad anidada,
  precio, estado, id, borrado o version enviados por el visitante.
- Los dos IDs deberán resolverse contra registros válidos en el servicio,
  no se consideran válidos por superar @Positive solamente. La tarifa viene
  de TipoBicicleta; el estado inicial DISPONIBLE se conserva del alta actual,
  no se habilitan estados manuales de viaje por esta entrada.
- Este turno no crea la fuente ni cambia el endpoint o su JSON. El usuario
  pegará/guardará el DTO y comprobará los cuatro filtros. Después se adaptarán
  conjuntamente modelo/servicio/controlador/DTO de salida y consumidores que
  corresponda, para retirar enums y precio propio sin introducir puentes
  provisionales o afirmar que el modelo actual ya cumple todo el DERv5.
- Solo se leyeron fuentes/DER y consultas de Git y se editó este diario.
  No se modificaron fuentes, datos, reglas o permisos ni se ejecutaron Gradle,
  SQL, reinicios, peticiones o escrituras Git. Esta anotación es posterior
  al punto de guardado informado, por lo que el diario vuelve a quedar modificado.

### Etapa 117 DTO de alta revisado y entrada de actualización propuesta

Fecha: 7 de noviembre de 2026.

- Se revisó CrearBicicletaDTO en main/dto: package, los tres campos, anotaciones,
  constructor y getters/setters coinciden con la propuesta. No faltan bloques.
- El usuario aportó BUILD SUCCESSFUL en tres segundos. XML verificados:
  CargaCatalogosRefactorTest 6, TipoBicicletaTest 6, BicicletaControllerTest 6 y
  AlquilerServiceTest 4, total 22 sin fallos, errores u omisiones. Timestamps
  UTC 2026-11-08T00:55:58.505Z y 00:55:59.465Z/.377Z/.483Z. Compilan el DTO;
  no prueban todavía sus validaciones ni lo conectan al POST actual.
- Se propone Crear la clase ActualizarBicicletaDTO, todavía no existente:
  codigoBicicleta obligatorio, máximo 255 y trim; tipoBicicletaId obligatorio
  positivo; estacionId opcional y positivo cuando se informa; version
  obligatoria/no negativa. Mantiene la semántica actual: estación nula no
  solicita vaciar ubicación; solo las operaciones autorizadas registran una salida.
- Se propone motivo opcional con límite de entrada de 1000 caracteres y trim,
  destinado a AUDITORIA_SISTEMA, no a una columna nueva de Bicicleta. El motivo
  es opcional en edición rutinaria y el servicio deberá exigirlo cuando la
  acción lo requiera, incluido movimiento físico según documento 26. Se
  contrastaron documento 35 y campo del DER; no se vuelve obligatorio para
  toda edición. Las validaciones condicionales no están implementadas por el DTO.
- No se aceptan precio, estado o borrado en esta entrada de edición general.
  La tarifa pertenece al tipo y los estados/bajas tienen recorridos controlados.
  La versión prepara el control previsto por DER, no lo implementa por sí sola.
- El usuario creará/guardará el DTO y ejecutará los filtros indicados; no se
  cambia aún PUT, modelo o frontend. Solo se leyeron fuentes/decisiones/XML y
  se actualizó este diario; no hubo cambios Java, DB, reglas/DER, permisos,
  Gradle, SQL, reinicios o escrituras Git por el asistente.

### Etapa 118 DTO de actualización revisado y adaptación coordinada pendiente

Fecha: 7 de noviembre de 2026.

- Se leyó ActualizarBicicletaDTO.java en main/dto: package, los cinco campos,
  anotaciones, constructor y getters/setters coinciden con la propuesta. Se
  confirmó guardado en disco y presencia del archivo .class en build/classes.
  Fuente modificada a las 22:02:37 locales; no se detectó un archivo vacío o
  creado en test por error. Se conservó su estado Git AM y el del DTO de alta.
- La salida aportada indica cuatro tareas UP-TO-DATE, por lo que no se afirma
  una nueva ejecución de las pruebas a partir de esa salida. Se leyeron los
  reportes disponibles, timestamps UTC 2026-11-08T01:02:47.275Z y
  01:02:48.343Z/.243Z/.363Z: 6+6+6+4, veintidós casos sin fallos/errores.
  Es evidencia de compilación previa y resultado conservado, no una prueba
  directa de las validaciones nuevas de los DTO ni su integración HTTP.
- Los DTO de alta/actualización están preparados, todavía sin sustituir los
  parámetros de los endpoints. La adaptación central debe coordinar Bicicleta
  (FK a tipo/estado, fecha_alta/version, retirada de precio por unidad), DTO de
  salida, repositorios, servicios, controladores, solicitudes del frontend y
  pruebas afectadas. No se propone reemplazar un campo aisladamente dejando
  el resto incompatible ni crear métodos puente que mantengan dos fuentes de tarifa.
- Se plantea solicitar autorización puntual para ejecutar ese bloque coordinado
  por su alcance entre archivos, manteniendo las decisiones aprobadas, guardas
  existentes y límites de módulos pendientes. No se obtuvo esa autorización
  todavía ni se implementa la migración por este mensaje. Reinicio/create-drop
  y comprobaciones del usuario no se ejecutarán por cuenta del asistente.
- Solo se leyeron fuentes, reportes y referencias de auditoría/DER y se
  actualizó este diario. No hubo cambios de fuentes, DB, permisos, reglas/DER,
  Gradle, SQL, reinicios, peticiones o escrituras Git por el asistente.

### Etapa 119 Adaptación coordinada de Bicicleta al inventario del DERv5

Fecha de referencia del entorno: 7 de noviembre de 2026.

Autorización: el usuario pidió implementar este bloque coordinado y dejarle
la parte de guardar y comprobar. Se conservaron los cambios previos y el
estado de staging de sus DTO. No se cambiaron ramas, commits ni el remoto.

Implementación realizada en fuentes, pendiente de ejecución:

1. Bicicleta ahora referencia TipoBicicleta y EstadoBicicleta por FK
   obligatorias y LAZY. Agrega fecha_alta DATE, fijada por el servidor al
   crear, y version con @Version. Conserva estacion_id opcional, borrado_bici
   independiente y referencias a los alquileres existentes. Su igualdad/hash
   no depende ya del código editable, para no romper colecciones Set.
2. Se retiraron el atributo/métodos de precio propio y los dos enums
   transitorios sin consumidores restantes en src. No se mantienen precios
   duplicados ni métodos puente. Los archivos eliminados siguen recuperables
   desde Git; no se borraron datos ni clases de otros dominios.
3. POST/PUT reciben CrearBicicletaDTO/ActualizarBicicletaDTO con @Valid, no
   entidades enlazadas por JSON. Rechazan campos ajenos, incluidos estado,
   precio y borrado. La edición exige la versión leída; estacionId nulo no
   vacía la ubicación. Cambiar de estación exige motivo y capacidad real.
4. BicicletaService concentra las operaciones antes mezcladas en el controlador.
   Las escrituras validan actor ADMIN activo, catálogo, código, versión,
   alquiler asociado y restricciones de estado antes de modificar. Se bloquea
   la bicicleta y se bloquea la estación receptora antes de contar todas las
   unidades físicas, incluidas archivadas. READ_COMMITTED evita reutilizar un
   conteo de snapshot anterior al bloqueo. @Version cubre también escrituras
   concurrentes externas a estas rutas.
5. BicicletaDTO conserva tipo/estado como códigos y precio como dato de salida,
   derivado exclusivamente del tipo. Añade IDs de tipo/estación, version,
   fechaAlta y borradoBici. Se adaptaron EstacionDTO y las consultas/estadísticas
   para usar códigos de las entidades y no serializar entidades como etiquetas.
6. ADMIN puede consultar unidades archivadas. DELETE ahora exige cuerpo con
   version/motivo y ubicación real en un taller interno activo; conserva el
   estado, la ubicación y la ocupación. Dar DE_BAJA es una transición separada:
   no archiva automáticamente. No hay borrados físicos ni cascadas nuevas.
7. Se habilitaron acciones administrativas controladas de DISPONIBLE a INACTIVA
   y viceversa y baja desde inventario sin proceso activo. Para habilitar se
   exige ubicación registrada y activa. EN_USO/RESERVADA/MANTENIMIENTO/
   EXTRAVIADA no se liberan desde edición general. La edición ordinaria se
   limita a DISPONIBLE/INACTIVA; DE_BAJA queda consultable/archivable en taller.
   No se permite inventar viajes, reservas, pérdidas, revisiones u órdenes.
   Los recorridos especiales y las restricciones cruzadas de los módulos
   pendientes deberán conectarse al implementarlos; no se simula su existencia.
8. Se incorporaron HISTORIAL_ESTADO_BICICLETA y AUDITORIA_SISTEMA, ya previstas
   en el DER, con sus FK, repositorios y un servicio de trazabilidad que exige
   transacción existente. No se agregó HISTORIAL_ATENCION_MANTENIMIENTO.
   El alta registra estado inicial (anterior nulo), las transiciones registran
   ambos estados, fecha, actor real y origen USUARIO. Ediciones/movimientos/
   archivo/tarifa registran auditoría ADMIN con lista explícita de datos de
   inventario, sin serializar cuentas o secretos. Un archivo sin transición
   no inventa un cambio de estado. No se implementó todavía una pantalla de
   consulta de historiales ni la auditoría del resto de módulos.
9. GET /api/admin/tipos-bicicleta devuelve datos de catálogo por DTO.
   PUT /api/admin/tipos-bicicleta/{id}/tarifa modifica solamente la tarifa:
   positiva, hasta ocho dígitos enteros/dos decimales, sin redondeo de entrada.
   Usa bloqueo del tipo y compara el precio anterior enviado por el formulario
   para rechazar una edición obsoleta, sin agregar columnas ajenas al DER.
   El intervalo preventivo no se altera desde esta operación.
10. AlquilerService toma y congela la tarifa del tipo bajo el mismo bloqueo,
    usa EN_USO al retirar y DISPONIBLE al devolver, y registra la transición.
    Contratar o devolver el viaje propio conserva el historial de dominio,
    incluso si la cuenta también tiene ADMIN. La finalización de un contrato
    ajeno mediante el permiso ADMIN agrega además auditoría administrativa;
    tener dos roles no convierte todas sus actuaciones en administrativas.
    La devolución usa exclusivamente precioPorHoraAplicado: si falta no se
    reconstruye con una tarifa actual; se informa un fallo de configuración,
    antes de mutar el alquiler. Se bloquea el contrato para serializar dos
    finalizaciones simultáneas. Se conservan propiedad del alquiler, destinos
    públicos habilitados, capacidad física y cálculo económico existente.
    Los métodos modificados revierten también ante excepciones comprobadas.
    AlquilerController deja pasar la excepción de configuración al manejador
    global 500, en lugar de atribuírsela al visitante como 400.
11. admin.js/admin.html cargan IDs de tipos/estaciones desde backend, muestran
    la tarifa como solo lectura y ofrecen Tarifas por tipo por separado.
    El formulario envía los DTO y la versión leída; no estados/precios/entidades
    anidadas. Muestra los estados de proceso sin permitir edición general y
    ofrece acciones justificadas separadas. Las archivadas siguen visibles.
    Se conserva la estética Bootstrap existente y los errores se muestran como
    texto, sin insertar datos de inventario en HTML. No se tocaron login,
    recuperación, registro, pantallas de clientes ni configuración de sesión.
12. No cambió WebAuthorization: las rutas nuevas administrativas usan el
    prefijo /api/admin/** ya restringido a ADMIN. Se ampliaron sus pruebas
    para cubrir inventario/tipos/tarifas y denegación a otros roles.

Pruebas preparadas, NO ejecutadas por el asistente:

- BicicletaControllerTest se adaptó a entradas DTO y HTTP con MockMvc:
  validación, campos no permitidos, versión, archivo y tarifa decimal.
- Nuevo BicicletaServiceTest cubre integridad del inventario, motivos, capacidad,
  rechazo sin mutaciones, estados, auditoría/historial, archivo independiente,
  lectura administrativa y tarifa por tipo. Sustituye las antiguas comprobaciones
  de negocio que estaban en el test del controlador; no se pierden esas guardas.
- AlquilerServiceTest conserva los rechazos de estaciones y agrega tarifa
  congelada al iniciar/finalizar, tarifa histórica faltante sin mutaciones y
  prohibición de finalizar contratos ajenos. Los dobles evitan APIs/correos/DB.
- Los tests unitarios NO demuestran por sí solos bloqueos reales de MySQL/JPA.
  Quedan pendientes el arranque y la comprobación del esquema/FK, tarifas,
  versiones, historiales y pantallas con datos descartables.

Comprobación que realizará el usuario:

```powershell
.\gradlew.bat test --tests "CargaCatalogosRefactorTest" --tests "TipoBicicletaTest" --tests "BicicletaControllerTest" --tests "BicicletaServiceTest" --tests "AlquilerServiceTest" --tests "WebAuthorizationTest" --tests "EstacionControllerTest" --tests "EstacionServiceTest"
```

No se ejecutó Gradle, javac, Node, pruebas, SQL ni reinicios. Solo se inspeccionaron
fuentes/referencias/diff y se verificó whitespace con git diff --check. La base
en ejecución no ha sido migrada y sigue correspondiendo al proceso anterior.
Antes de probar estas modificaciones en navegador hará falta un reinicio
controlado del perfil refactor: create-drop eliminará sus datos de prueba.
No se inventaron fecha_alta ni tarifas para datos antiguos, ni se creó una
migración destructiva sobre la base en uso. No se completaron reservas,
alquiler_detalle, Mercado Pago, incidencias o mantenimiento; la compatibilidad
del alquiler actual es solo el enlace necesario para avanzar con inventario.
Los dos Mermaid se conservaron sin cambios; estas entidades/campos ya estaban
aprobados en ellos. No hay un nuevo diseño paralelo ni una nueva regla de pago.

### Etapa 120 Compilación y pruebas del inventario verificadas

Fecha: 8 de noviembre de 2026.

- El usuario informó BUILD SUCCESSFUL en 19 segundos y cuatro tareas ejecutadas
  con los ocho filtros de la etapa anterior. Se leyeron los XML de
  build/test-results/test, no solo el texto de la terminal.
- Resultado: CargaCatalogosRefactorTest 6, TipoBicicletaTest 6,
  BicicletaControllerTest 10, BicicletaServiceTest 20, AlquilerServiceTest 8,
  WebAuthorizationTest 19, EstacionControllerTest 22 y EstacionServiceTest 10.
  Total: 101 pruebas, cero fallos, cero errores y cero omisiones.
- Timestamps de las suites: 2026-11-08T12:32:56.192Z a
  2026-11-08T12:33:00.535Z (09:32:56 a 09:33:00, Argentina). Los .class de
  Bicicleta y BicicletaService son posteriores a sus fuentes modificadas.
  La evidencia corresponde a esta ejecución, no a reportes anteriores.
- Se contrastaron las FK, fecha_alta y version en Bicicleta, las referencias
  a catálogos y su precarga refactor. No quedan referencias en src a
  CodigoTipoBicicleta/CodigoEstadoBicicleta ni métodos de precio por unidad.
  git diff --check no detectó errores de whitespace; mostró únicamente el
  aviso habitual de conversión LF/CRLF del índice documental.
- El asistente no volvió a ejecutar Gradle ni inició/detuvo procesos, no hizo
  consultas o escrituras SQL y no modificó fuentes, configuración o Git.
  Solo se actualiza este diario con la evidencia de las pruebas del usuario.
- Siguiente paso: arranque controlado del perfil refactor con MySQL activo,
  para verificar creación del esquema, FK y precarga real antes de continuar
  las pruebas manuales de bicicletas, tarifas y trazabilidad. El perfil sigue
  usando create-drop: se advierte que reiniciarlo elimina los datos locales
  de prueba previamente autorizados como descartables. No se considera ya
  comprobada la base ni el navegador por haber pasado los tests con dobles.

### Etapa 121 Esquema y catálogos comprobados después del arranque

Fecha: 8 de noviembre de 2026.

- El usuario inició la aplicación y compartió Started TurismoSustentableApplication
  a las 09:51:23, puerto 8080, en 4.288 segundos. El indicador 80% de bootRun
  corresponde al proceso que sigue atendiendo peticiones, no a un fallo.
  El aviso open-in-view no impidió el arranque; no se cambió esa configuración.
- Se consultó MySQL local mediante SELECT exclusivamente, contra
  turismo_sustentable. Existen bicicleta, tipo_bicicleta, estado_bicicleta,
  historial_estado_bicicleta y auditoria_sistema.
- BICICLETA tiene id PK, codigo_bicicleta VARCHAR(255) NOT NULL UNIQUE,
  borrado_bici NOT NULL, fecha_alta DATE NOT NULL, version INT NOT NULL,
  tipo_bicicleta_id/estado_bicicleta_id obligatorias y estacion_id opcional.
  Ya no existe una columna de precio individual ni las columnas de enum anteriores.
- information_schema confirmó las ocho FK esperadas entre esas tres tablas
  operativas y sus referencias: tres de Bicicleta, cuatro del historial
  (bicicleta, estado anterior/nuevo, actor) y una de auditoría (administrador).
- Precarga real: MONTANA USD 10.00/3 meses, PASEO USD 8.00/2 meses,
  ELECTRICA USD 15.00/1 mes. Hay siete estados: DISPONIBLE, RESERVADA,
  EN_USO, MANTENIMIENTO, INACTIVA, DE_BAJA y EXTRAVIADA.
- Los conteos de usuarios, estaciones, bicicletas, historial y auditoría
  son todos cero, consistentes con el entorno de prueba recreado. La aplicación
  precarga catálogos, no una cuenta ADMIN ni inventario de demostración.
- No se consultaron credenciales, hashes o tokens. No se ejecutaron escrituras
  SQL, pruebas, reinicios o cambios Git por el asistente. Solo se documenta
  el resultado. Próximo paso: registro manual de una cuenta QA, asignación
  explícita de ADMIN sin quitar CLIENTE y pruebas del inventario desde el panel.
  La existencia de tablas/FK no demuestra todavía los recorridos de interfaz
  ni los bloqueos con solicitudes reales concurrentes.

### Etapa 122 Cuenta QA creada y asignación administrativa preparada

Fecha: 8 de noviembre de 2026.

- El usuario confirmó el registro manual. Se verificó por SELECT la cuenta
  id 1, nickname admin.bicicletas, activa, con rol CLIENTE. Existen los
  catálogos ADMIN (id 1), CLIENTE (id 2) y TECNICO (id 3).
- Se entrega al usuario un INSERT condicionado al nickname/cuenta activa y
  a la ausencia del vínculo ADMIN. Agrega solo usuario_rol, sin quitar CLIENTE,
  seguido de un SELECT de comprobación. No fue ejecutado por el asistente:
  la asignación y su resultado siguen pendientes.
- Si había una sesión abierta, deberá cerrarla e iniciar otra para que la
  autenticación cargue los nuevos permisos. No se reinicia la aplicación.
  No se leyeron secretos ni se hicieron escrituras SQL, cambios Java o Git.

### Etapa 123 Roles QA y dos ubicaciones de prueba verificados

Fecha: 8 de noviembre de 2026.

- El usuario informó que inició sesión con ambos roles y creó las dos estaciones
  desde el panel. Se comprobaron por SELECT la cuenta y las ubicaciones.
- admin.bicicletas (id 1) está activo y conserva ADMIN y CLIENTE.
- Estación id 1: dirección Calle de Prueba 100, pública, capacidad 2,
  coordenadas -53.7865/-67.7045, borrado_estacion false y version 0.
- Taller QA Central (id 2): dirección Calle de Prueba 200, uso interno,
  capacidad 5, coordenadas -53.787/-67.705, borrado_estacion false y version 0.
  La clasificación corresponde a es_publica true/false, no al nombre.
- Todavía no hay bicicletas. Próxima prueba: alta manual QA-01 del tipo
  MONTANA en la ubicación pública, comprobando estado inicial DISPONIBLE y
  tarifa del tipo USD 10.00/h de solo lectura. Alta e historial/auditoría
  de esa bicicleta permanecen pendientes, no se dan por comprobados.
- El asistente solo consultó datos no sensibles y actualizó este diario.
  No hubo escrituras SQL, cambios de fuentes, pruebas, reinicios o cambios Git.

### Etapa 124 Formulario de bicicleta distingue alta, edición y traslado

Fecha: 8 de noviembre de 2026.

- Durante la preparación del alta el usuario detectó campos readonly que
  recibían foco, una opción de estación ambigua y un motivo de traslado visible
  sin que hubiera una bicicleta existente. Aprobó los tres ajustes de presentación.
- admin.html: estado y tarifa del tipo ahora usan disabled, manteniendo sus
  valores visibles y el aspecto deshabilitado de Bootstrap existente. No se
  cambia su fuente: el backend determina estado inicial y tarifa del tipo.
- admin.js: prepararCatalogosBicicleta distingue alta de edición. En alta,
  Seleccioná una estación es una opción inicial deshabilitada; debe elegirse
  un destino real. En edición, Mantener ubicación actual permite enviar
  estacionId nulo con la semántica existente de no solicitar traslado.
- El grupo del motivo comienza oculto. Un único manejador de cambio lo muestra
  y marca obligatorio solo cuando se edita una unidad editable y se elige una
  estación distinta de la original. Se oculta, deshabilita y limpia al volver
  a la ubicación original, elegir mantener ubicación, abrir un alta o consultar
  una unidad no editable. Se evita arrastrar una justificación de otro contexto.
- No se cambiaron DTO, servicios, endpoints, reglas, permisos, DER o CSS general.
  Se conserva el rechazo de alta sin estación y de traslado sin motivo, tanto
  en las guardas frontend existentes como en las validaciones backend.
- Verificación ejecutada por el asistente: Node --check de admin.js correcto;
  ensayo en memoria con VM, DOM y Axios simulados correcto para alta, opción
  obligatoria, edición, traslado, retorno al origen, opción mantener ubicación,
  proceso no editable y reapertura de alta. El ensayo no hizo peticiones HTTP.
  Se verificaron atributos disabled y enlace onchange en el HTML. git diff
  --check sin errores de whitespace, con el aviso LF/CRLF ya conocido del índice.
- No se ejecutó Gradle ni se reinició la aplicación o modificaron datos/Git.
  La apariencia y el comportamiento en navegador quedan para la comprobación
  manual tras recargar el panel sin caché. Continúa pendiente el alta QA-01;
  no se afirma que exista ni que su historial/auditoría estén comprobados.

### Etapa 125 Estado con el mismo control bloqueado que el país del registro

Fecha: 8 de noviembre de 2026.

- El usuario confirmó que la tarifa vacía correspondía a no haber seleccionado
  un tipo: no había un fallo de cálculo. Se conserva la tarifa y su implementación.
- La inspección del navegador confirmó INPUT disabled con valor DISPONIBLE.
  No era editable; la selección visual del texto no equivalía a editarlo.
  En registro, el país usa SELECT disabled. Se corrigió solamente el estado
  para tener ese mismo tipo de control y comportamiento.
- admin.html ahora usa SELECT disabled para bici-estado. admin.js incorpora
  mostrarEstadoBicicleta, que coloca una sola opción con el código vigente;
  se usa al abrir alta y edición. No se ofrece un listado estático de posibles
  transiciones ni se permite cambiar estado desde el formulario.
- Node --check y git diff --check correctos (salvo aviso LF/CRLF ya conocido).
  Ensayo en memoria correcto para los siete códigos: una opción seleccionada
  con el estado vigente y el selector siempre deshabilitado.
- Se recargó el panel en el navegador integrado, se abrió Nueva Bicicleta y
  se verificó en su árbol de accesibilidad: estado como selector deshabilitado,
  DISPONIBLE visible, tipo MONTANA y tarifa 10. No se pulsó Guardar ni se
  enviaron escrituras por el asistente. La tabla ya mostraba QA-01,
  MONTANA, DISPONIBLE, USD 10 en Estación QA Centro; es evidencia de la
  interfaz, no una comprobación nueva de sus eventos históricos en MySQL.
- No se cambiaron el backend, el cálculo, precios, catálogo, DER o configuración.
  No hubo SQL, Gradle, reinicios de la aplicación o cambios Git. Se deja el
  formulario de prueba abierto para continuar la revisión manual del usuario.

### Etapa 126 Eliminada la opción redundante de conservar ubicación

Fecha: 8 de noviembre de 2026.

- El usuario aprobó retirar Mantener ubicación actual del selector de edición.
  admin.js ahora carga directamente las estaciones y selecciona el ID vigente.
  Dejar esa selección sin cambiarla conserva la ubicación, sin opción adicional.
- El alta mantiene Seleccioná una estación como opción inicial deshabilitada
  y sus validaciones de destino obligatorio. El motivo sigue apareciendo
  únicamente al solicitar un traslado real durante una edición permitida.
- Al consultar una bicicleta sin ubicación física, por ejemplo EN_USO, se
  muestra Sin estación asignada como opción informativa deshabilitada. No se
  selecciona por defecto una estación existente para aparentar que está allí.
  No se cambió la semántica backend de estacionId nulo ni los permisos.
- Verificación: Node --check y git diff --check correctos, salvo aviso LF/CRLF
  ya conocido. Ensayo en memoria con DOM/Axios simulados correcto para alta,
  edición con estación id 2 (distinta de la primera opción), ausencia de opción
  vacía redundante, motivo al cambiar/volver, unidad EN_USO sin estación y
  reapertura del alta. No se hicieron peticiones reales ni se guardaron datos.
- Solo se modificaron admin.js y este diario. No se tocaron Java, HTML, CSS,
  DER, SQL, configuración, procesos o Git. Comprobación visual pendiente
  del usuario tras recargar el panel sin reiniciar la aplicación.

### Etapa 127 Alcance aprobado para disponibilidad y eliminación definitiva

Fecha: 8 de noviembre de 2026.

- El usuario aprobó integrar salida física y borrado lógico en Eliminar, sin
  un botón adicional obligatorio, y los ajustes del selector/listado.
- Alcance: mostrar capacidad y bloquear destinos llenos (salvo ubicación actual);
  eliminar con confirmación real, motivo, ADMIN, versión, Taller previo y sin
  procesos incompatibles; dejar DE_BAJA, borrado_bici true y estacion_id nulo
  de forma atómica, conservando auditoría y la transición cuando corresponda;
  ocultar eliminadas por defecto y permitir su consulta administrativa.
- No se requiere Dar de baja antes de eliminar. Dar de baja sin eliminar sí
  conserva presencia y ocupación. No se aprueban restauración, reactivación
  de DE_BAJA, eliminación física de filas ni liberaciones masivas de datos previos.
- Sustituye la alternativa anterior de archivo sin cambios de estado/ubicación
  y la propuesta de endpoint separado de salida. RN-INV-02 detalla el acuerdo
  vigente en documento 26, sección 8. Documentos 13/20 remiten a esa revisión;
  los dos Mermaid reflejan la condición sin modificar entidades, campos o FK.
- En este turno solo se asienta la aprobación y se resume el bloque. No se
  modifican fuentes, pruebas, datos o configuración; tampoco se ejecuta SQL,
  Gradle, reinicios o cambios Git. El usuario aún decidirá quién implementa.

### Etapa 128 RN-INV-02 implementada; comprobaciones a cargo del usuario

Fecha: 8 de noviembre de 2026.

- El usuario autorizó implementar solo los tres ajustes aprobados y pidió
  explicación de archivos/motivos y conservar la ejecución de las pruebas.
  Se respetaron fuentes previas, archivos nuevos del bloque anterior y staging.

Código:

1. admin.js muestra uso y espaciosLibres ya provistos por EstacionDTO. Deshabilita
   destinos llenos o con disponibilidad no informada, excepto la ubicación
   actual. La edición consulta primero la bicicleta para identificar esa
   excepción; no consume un lugar adicional por mantenerla. No cambia la
   comprobación de capacidad con bloqueo en backend ni el conteo físico.
2. BicicletaService.listar recibe incluirEliminadas y usa consultas existentes:
   findByBorradoBiciFalse por defecto y findAll al incluirlas. El controlador
   expone @RequestParam explícito con default false en GET /api/bicicletas.
   GET por ID conserva consulta histórica administrativa sin habilitar edición.
3. admin.html incorpora Mostrar eliminadas; admin.js envía el parámetro, cambia
   Archivar/Archivada por Eliminar/Eliminada y conserva solo Ver para borradas.
   Una respuesta de un filtro anterior no repinta un filtro distinto ya elegido.
   Una eliminada con ubicación nula se presenta como Fuera del inventario.
4. Nuevo EliminarBicicletaDTO extiende OperacionBicicletaDTO para reutilizar
   version/motivo y exige salidaFisicaConfirmada mediante @NotNull/@AssertTrue.
   Es entrada HTTP: no entidad/columna. Las otras operaciones no exigen ese dato.
   El servicio también comprueba explícitamente true para llamadas internas.
5. El DELETE existente recibe ese DTO y delega a eliminarLogico. Se retiró el
   nombre/método archivar del código de este recorrido, sin crear un puente
   que mantenga el comportamiento anterior ni un endpoint separado de salida.
6. eliminarLogico conserva ADMIN activo, bloqueo de bicicleta, versión, motivo,
   Taller habilitado bloqueado y control de alquiler/procesos. Solo acepta
   las situaciones editables actuales DISPONIBLE/INACTIVA o una DE_BAJA
   sin alquiler activo. EN_USO/RESERVADA/MANTENIMIENTO/EXTRAVIADA se rechazan.
   Se resuelve el catálogo antes de mutar y en una sola transacción
   READ_COMMITTED se deja DE_BAJA, borrado true y ubicación nula.
7. Se conserva auditoría con acción ELIMINAR_SALIDA_DEFINITIVA, valores
   anteriores/nuevos (incluida estación previa), actor, motivo y fecha. Se
   registra historial de estado solo si cambió a DE_BAJA; no se duplica una
   transición DE_BAJA a DE_BAJA. No se eliminan filas ni relaciones.
8. La confirmación del panel explica retirada real, liberación y conservación
   histórica; Confirmar retiro y eliminar envía explícitamente el indicador.
   Cancelar no envía escrituras. Dar de baja por separado sigue sin liberar
   lugar; no se modifica la petición ni lógica de inactivar/habilitar.

Límites preservados:

- No se cambiaron entidades, esquema, repositorios, configuración de sesión,
  reglas de tarifas, cálculo económico o fuentes de alquileres/estaciones.
- Los módulos nuevos de reservas, incidencias y órdenes todavía no tienen su
  implementación completa: no se inventaron consultas a tablas/repositorios
  inexistentes. Se conserva el rechazo por estados de proceso y alquiler
  activo; al implementar esos módulos deberán integrarse sus guardas abiertas.
- No se liberaron ni reinterpretaron borradas previas con ubicación, no se
  permitió restauración ni reactivación de DE_BAJA. El cambio aplica al nuevo
  recorrido confirmado, no a una migración masiva de inventario.

Pruebas preparadas, no ejecutadas:

- BicicletaServiceTest: 33 casos en fuente. Eliminación directa y de una baja
  previa, confirmación, versión, motivos, Taller, bloqueos por proceso/contrato,
  rechazo anterior a mutaciones, catálogo faltante, trazabilidad y listados.
- BicicletaControllerTest: 15 casos en fuente. Contrato HTTP y validaciones
  heredadas del DTO, confirmación, default del filtro y conflicto 409.
- WebAuthorizationTest: 21 casos en fuente. Incluye ADMIN y rechazos anónimo/
  CLIENTE/TECNICO para eliminación y consulta de eliminadas. No se modificó
  WebAuthorization ni se ampliaron permisos.
- Nuevo src/test/js/InventarioBicicletas.test.cjs: 15 casos preparados con Node
  Test Runner, DOM y Axios simulados sobre el admin.js real. No requiere
  servidor, navegador, DB, credenciales o dependencias descargadas.
- Documento 39: comandos filtrados, preparación, 18 escenarios manuales y
  SELECT delimitados para registrar evidencia sin secretos. La suite Java
  indicada contiene 121 casos en fuente, no 121 ejecuciones acreditadas.

El asistente solo inspeccionó fuentes/referencias/diff y comprobó whitespace
con git diff --check (sin errores; avisos LF/CRLF ya conocidos). No ejecutó
Gradle, Node, javac, pruebas, SQL, navegador, reinicios o cambios Git.
El usuario debe guardar y ejecutar las suites antes del reinicio controlado.
El backend encendido sigue siendo el anterior; no usar Eliminar en ese proceso.
create-drop eliminará los datos QA al reiniciar, por lo que las comprobaciones
de interfaz del nuevo bloque se harán después de recrear el entorno de prueba.

### Etapa 129 Pruebas automáticas de RN-INV-02 aprobadas

Fecha: 8 de noviembre de 2026.

- El usuario ejecutó los ocho filtros Java: BUILD SUCCESSFUL en 11 segundos,
  cuatro tareas ejecutadas. Se leyeron los ocho XML actuales de test-results.
- Resultado verificado: CargaCatalogosRefactorTest 6, TipoBicicletaTest 6,
  BicicletaControllerTest 15, BicicletaServiceTest 33, AlquilerServiceTest 8,
  WebAuthorizationTest 21, EstacionControllerTest 22 y EstacionServiceTest 10.
  Total 121; cero fallos, errores u omisiones. Timestamps UTC entre
  2026-11-08T15:24:30.547Z y 15:24:34.830Z (12:24, Argentina).
- El usuario aportó la ejecución Node de InventarioBicicletas.test.cjs:
  15 pruebas, 15 aprobadas, cero fallos/cancelaciones/omisiones/todo.
  La comprobación --check de admin.js terminó sin errores. No se afirma
  un reporte XML independiente para Node: la evidencia es la salida aportada.
- El bloque suma 136 pruebas automáticas correctas. No sustituye verificar
  el movimiento, ocupación y persistencia reales bajo MySQL ni la interfaz
  con el backend nuevo arrancado.
- Se releyó application-refactor.properties: sigue create-drop. Próximo paso:
  detener el proceso anterior, iniciar refactor en 8080, recrear cuenta/roles
  y ubicaciones QA y seguir la matriz del documento 39. Se advierte nuevamente
  que el reinicio elimina los datos de prueba. No hubo reinicios, SQL,
  nuevas pruebas o cambios de código/configuración/Git por el asistente.

### Etapa 130 Nuevo arranque y preparación de QA para RN-INV-02

Fecha: 8 de noviembre de 2026.

- El usuario reinició: Started TurismoSustentableApplication a las 12:34:48,
  PID 17804, puerto 8080, en 3.289 segundos. El aviso open-in-view y el indicador
  80% de bootRun no corresponden a fallos del arranque.
- Después recreó la cuenta de prueba y agregó ADMIN mediante el procedimiento
  SQL manual indicado. Se verificó por SELECT admin.bicicletas, id 1,
  activo y con ADMIN/CLIENTE.
- Se verificaron las dos estaciones creadas desde el panel: id 1 pública,
  Calle de Prueba 100, capacidad 2, coordenadas -53.7865/-67.7045; id 2
  Taller QA Central interno, Calle de Prueba 200, capacidad 5, coordenadas
  -53.787/-67.705. Ambas sin borrado lógico y con version 0.
- No hay bicicletas en la base recreada. Próximo paso: QA-01 MONTANA en la
  pública (USD 10/h) y QA-02 PASEO en el Taller (USD 8/h), verificando los
  cupos visibles del selector. Persistencia, eventos y ocupación de esas
  unidades todavía pendientes; no se presume su alta por preparar los datos.
- El asistente solo realizó consultas de lectura sin secretos y actualizó
  este diario. No ejecutó escrituras SQL, reinicios, pruebas o cambios Git.

### Etapa 131 Altas reales de QA-01 y QA-02 verificadas

Fecha: 8 de noviembre de 2026.

- El usuario creó y guardó ambas unidades en el nuevo proceso de prueba.
  SELECT confirmó QA-01 id 1, MONTANA, tarifa de tipo USD 10.00, DISPONIBLE,
  borrado false, estación pública id 1, fecha_alta 2026-11-08 y version 0.
- QA-02 id 2: PASEO, tarifa de tipo USD 8.00, DISPONIBLE, borrado false,
  Taller id 2, misma fecha de alta y version 0.
- Ocupación comprobada contando todas las FK físicas: pública 1 presente/
  1 espacio libre de capacidad 2; Taller 1 presente/4 espacios libres de 5.
- Cada alta produjo un historial inicial con estado anterior nulo y nuevo
  DISPONIBLE, actor_usuario_id 1, origen USUARIO y observación Alta de
  inventario. Cada una produjo además auditoría CREAR, administrador_id 1,
  valores anteriores nulos y nuevos con código/tipo/estado/ubicación/borrado.
  Son eventos de la base real, no resultados de los mocks.
- Próxima comprobación: traslado iniciado hacia el último lugar libre en una
  pestaña A y ocupación de ese lugar mediante un alta distinta en B. El
  guardado posterior de A deberá rechazarse por capacidad. Ese escenario,
  las eliminaciones y sus efectos todavía no se consideran ejecutados.
- El asistente solo consultó datos de inventario y trazabilidad y actualizó
  este diario, sin escrituras SQL, pruebas automatizadas, reinicios o cambios Git.

### Etapa 132 Último lugar público ocupado durante la prueba entre pestañas

Fecha: 8 de noviembre de 2026.

- El usuario confirmó QA-03 creada en la estación pública desde B. SELECT
  verificó id 3, estación id 1, DISPONIBLE, borrado false y version 0.
- La pública contiene QA-01 y QA-03: 2 presentes, capacidad 2, espacios libres 0.
  QA-02 continúa en Taller id 2, DISPONIBLE, borrado false y version 0;
  el Taller mantiene 1 presente y 4 lugares libres.
- Se indica volver al formulario de traslado pendiente en A, sin recargar,
  y guardar. Se espera rechazo por capacidad desde backend. Ese guardado y la
  ausencia de cambios/eventos tras el rechazo quedan pendientes de comprobar.
- Solo se ejecutaron SELECT de inventario/capacidad y se actualizó este diario.
  No se manipularon pestañas ni se enviaron escrituras o reinicios por el asistente.

### Etapa 133 Traslado con disponibilidad desactualizada rechazado sin cambios

Fecha: 8 de noviembre de 2026.

- El usuario guardó en A el traslado pendiente de QA-02 después de que QA-03
  ocupara en B el último lugar público. Reportó: No se pudo completar /
  La estación de destino no tiene espacios libres. Corresponde a INV02-02/03.
- SELECT posterior confirmó QA-02 id 2 sin cambios: PASEO USD 8.00,
  DISPONIBLE, Taller id 2, borrado false y version 0.
- QA-02 conserva exactamente su historial inicial y auditoría CREAR (id 2
  en ambas tablas). No existe evento de traslado, transición o edición
  resultante del intento rechazado.
- La pública sigue en 2 de 2 lugares, libres 0. El Taller sigue con 1 de 5,
  libres 4. No se aplicó una ubicación ni una ocupación parcial.
- Este escenario verifica rechazo real de información antigua del formulario;
  no se presenta como prueba de dos transacciones simultáneas al mismo instante.
  Próximo paso: abrir un formulario nuevo y comprobar el destino lleno
  deshabilitado, seguido de la excepción de conservar la ubicación actual.
- El asistente solo hizo SELECT y actualizó la evidencia. No modificó datos,
  fuentes, sesiones, procesos o Git.

### Etapa 134 Destino lleno bloqueado y edición en ubicación actual aprobados

Fecha: 8 de noviembre de 2026.

- El usuario confirmó que, al reabrir QA-02, la pública con cero lugares
  aparece deshabilitada y el Taller actual con cuatro queda seleccionado
  y habilitado (INV02-04; evidencia informada por el usuario).
- Después editó la unidad id 1 en su estación pública llena y guardó
  únicamente el código. SELECT confirmó el valor real QA-01 EDIT (con espacio,
  no el guion sugerido), version 1, MONTANA USD 10.00, DISPONIBLE,
  borrado false, fecha_alta sin cambios y estacion_id 1.
- La pública mantiene dos presentes/capacidad dos y cero libres; el Taller
  mantiene una presente y cuatro libres. No se agregó ocupación por conservar
  la ubicación actual. Corresponde a INV02-05.
- Auditoría id 4 ACTUALIZAR del administrador 1 conserva código anterior
  QA-01 y nuevo QA-01 EDIT con el resto de atributos iguales. Motivo vacío
  válido para esta edición sin traslado. El historial de estados conserva
  solamente su alta inicial; no se inventó una transición por editar el código.
- Próximo paso: rechazo de eliminación desde ubicación pública (INV02-07),
  antes de trasladar la unidad al Taller. Sigue pendiente de ejecutar.
- Se realizaron solo SELECT y documentación, sin mutaciones de datos,
  código, sesión, procesos o Git por el asistente.

### Etapa 135 Eliminación desde ubicación pública rechazada

Fecha: 8 de noviembre de 2026.

- El usuario intentó eliminar la unidad id 1 QA-01 EDIT desde la estación
  pública y confirmó que la operación fue impedida (INV02-07).
- SELECT posterior: DISPONIBLE, estacion_id 1, borrado false y version 1.
  No se aplicó cambio de estado, ubicación o borrado.
- La pública conserva dos presentes y cero libres; el Taller una presente
  y cuatro libres. La auditoría de esa unidad mantiene únicamente CREAR id 1
  y ACTUALIZAR id 4; su historial conserva solo el alta inicial id 1.
  El rechazo no creó un evento de eliminación ni una salida ficticia.
- Próximo paso: elegir el Taller desde la edición, comprobar que se exige
  motivo y registrar el traslado con justificación. Sigue pendiente.
- El asistente solo consultó datos y actualizó el diario; no realizó
  escrituras SQL, cambios de fuentes, reinicios o cambios Git.

### Etapa 136 Traslado justificado al Taller verificado

Fecha: 8 de noviembre de 2026.

- El usuario informó que el formulario impidió guardar sin motivo y permitió
  hacerlo al completarlo. SELECT verificó QA-01 EDIT id 1 en Taller id 2,
  DISPONIBLE, MONTANA USD 10.00, borrado false y version 2.
- La pública ahora tiene una presente y un lugar libre; el Taller tiene dos
  presentes y tres libres. Se liberó el origen y se ocupó el destino real.
- Auditoría id 5 ACTUALIZAR, administrador 1: conserva ubicación anterior 1
  y nueva 2, motivo Traslado al taller QA y el resto de datos sin cambios.
  El historial de estado sigue con su alta inicial: trasladar no creó una
  transición ficticia. Se acredita INV02-08 y el motivo obligatorio informado.
- Próxima comprobación manual: inactivar/habilitar sin mover ni liberar lugar,
  seguida de DE_BAJA y eliminación definitiva; esos recorridos siguen pendientes.
- El asistente solo efectuó SELECT y documentación. No cambió datos,
  fuentes, configuración, procesos o Git.

### Etapa 137 Inactivación y habilitación conservan presencia física

Fecha: 8 de noviembre de 2026.

- El usuario completó ambas acciones en QA-01 EDIT id 1 y confirmó el resultado
  visual. SELECT verificó DISPONIBLE, Taller id 2, borrado false y version 4.
- Historial id 4: DISPONIBLE a INACTIVA, actor 1, origen USUARIO,
  Pausa de prueba. Historial id 5: INACTIVA a DISPONIBLE, mismo actor/origen,
  Fin de pausa de prueba. Auditorías id 6 y 7 CAMBIAR_ESTADO conservan
  estados anteriores/nuevos y motivos sin alterar ubicación o borrado.
- La pública mantiene una presente/un espacio libre; el Taller dos presentes/
  tres libres. Se acredita INV02-09 sin movimiento ni liberación ficticia.
- Próximo paso: Dar de baja la unidad en el Taller y comprobar DE_BAJA con
  borrado false, presencia/ocupación conservadas; la eliminación va después.
- El asistente solo realizó SELECT y documentación, sin modificar datos,
  fuentes, configuración, procesos o Git.

### Etapa 138 Baja definitiva sin salida física verificada

Fecha: 8 de noviembre de 2026.

- El usuario dio de baja QA-01 EDIT id 1 y pidió comprobar su ocupación.
  SELECT confirmó DE_BAJA, Taller id 2, borrado false y version 5.
- El Taller conserva dos presentes/capacidad cinco y tres lugares libres;
  la unidad dada de baja sigue ocupando exactamente su lugar. La pública
  conserva una presente/un lugar libre. Se acredita INV02-10.
- Historial id 6: DISPONIBLE a DE_BAJA, actor 1, origen USUARIO y motivo
  Fin de vida útil de prueba. Auditoría id 8 CAMBIAR_ESTADO conserva la misma
  ubicación y borrado false antes/después, con ADMIN real y justificación.
- El borrado lógico y la salida física todavía no se ejecutaron. Próximo paso:
  confirmar que cancelar Eliminar o dejar el motivo vacío no produce cambios,
  antes de registrar la salida definitiva de prueba.
- El asistente solo hizo SELECT y documentación, sin alterar datos,
  fuentes, configuración, procesos o Git.

### Etapa 139 Cancelación y motivo vacío no ejecutan la eliminación

Fecha: 8 de noviembre de 2026.

- El usuario confirmó ambas comprobaciones de INV02-11: Cancelar cierra sin
  eliminar y la confirmación sin motivo exige completarlo.
- SELECT posterior mantiene QA-01 EDIT id 1 DE_BAJA, Taller id 2, borrado false
  y version 5. Hay cuatro eventos de estado, seis de auditoría administrativa
  y ninguna acción ELIMINAR_SALIDA_DEFINITIVA para esa unidad.
- La ocupación sigue igual: pública una presente/un lugar libre; Taller dos
  presentes/tres libres. No hubo salida física ficticia ni cambios parciales.
- Próximo paso: confirmar la eliminación con motivo Retirada definitiva de
  prueba, sobre la unidad QA que ya está DE_BAJA. Se espera borrado true,
  ubicación nula y un lugar liberado, sin nueva transición DE_BAJA a DE_BAJA.
  La operación confirmada todavía no se considera ejecutada.
- Solo se realizaron SELECT y documentación; el asistente no envió
  escrituras, eliminaciones, reinicios ni cambios de código/Git.

### Etapa 140 Eliminación de una DE_BAJA libera lugar y conserva historia

Fecha: 8 de noviembre de 2026.

- El usuario confirmó Eliminar sobre QA-01 EDIT con el motivo solicitado.
  SELECT verificó que la fila id 1 sigue existiendo: MONTANA USD 10.00,
  DE_BAJA, borrado true, estacion_id nulo, fecha_alta sin cambios y version 6.
- El Taller pasó de dos presentes/tres libres a una presente/cuatro libres,
  capacidad cinco. La pública conserva una presente/un lugar libre.
  La consulta de no borradas devuelve solo QA-02 id 2 y QA-03 id 3.
- Auditoría id 9 ELIMINAR_SALIDA_DEFINITIVA, administrador 1: ubicación anterior
  2 y nueva nula, borrado anterior false y nuevo true, estado DE_BAJA conservado,
  motivo Retirada definitiva de prueba y fecha registrada.
- El historial mantiene cuatro eventos de esa bicicleta: alta, pausa,
  habilitación y baja previa. No se generó una transición DE_BAJA a DE_BAJA.
  Se acredita persistencia real de INV02-12/18 para una unidad ya dada de baja.
- Pendiente: consulta visual de Mostrar eliminadas/Ver sin edición, seguida
  del rechazo por versión al eliminar otra unidad y su eliminación directa
  desde DISPONIBLE en Taller sin una baja previa.
- El asistente solo verificó con SELECT y documentó; no ejecutó escrituras,
  eliminaciones, pruebas automatizadas, reinicios o cambios Git.

### Etapa 141 Excepción por pérdida definitiva aprobada y documentada

Fecha: 8 de noviembre de 2026.

- El usuario aprobó EXTRAVIADA a DE_BAJA por pérdida definitiva confirmada,
  decidida por ADMIN con motivo, incidencia de la misma unidad, detalle
  cerrado y ausencia de operaciones incompatibles. No exige Taller y mantiene
  ubicación nula, con historial de estado y auditoría de la decisión real.
- La eliminación lógica posterior requiere esa baja respaldada, sin simular
  recepción o salida física ni liberar espacio. Una DE_BAJA sin ubicación o
  un texto del navegador no son prueba suficiente. No restaura ni reactiva.
- Se conservan cierre excepcional y reglas económicas. No se introdujeron
  plazos automáticos, responsabilidad o cargos automáticos por la pérdida.
  Recuperación antes de la baja conserva MANTENIMIENTO y revisión; una
  recuperación posterior a DE_BAJA no revierte decisiones automáticamente.
- RN-INV-03 se incorporó al documento 26, sección 9, y se sincronizaron
  referencias en diccionario de trabajo, estados de bicicleta, administración
  e incidencias (08/13/20/21). Se actualizaron ambos Mermaid sin agregar
  entidades, columnas, estados ni relaciones.
- No se cambió el código: el recorrido físico sigue como se probó y la
  excepción queda pendiente al conectar incidencias, evitando una transición
  libre o un endpoint provisional sin comprobaciones reales.
- Se ordenaron numéricamente las etapas 136 a 140 del diario, conservando
  íntegramente sus contenidos y evidencias. No se reescribieron sus resultados.
- Continúan pendientes las pruebas visuales Mostrar eliminadas/Ver, el
  conflicto por versión al eliminar otra unidad y su eliminación directa
  desde DISPONIBLE en Taller, ya preparadas en el documento 39.
- Solo se editaron documentos Markdown y comentarios/descripción del DER;
  no se ejecutaron Java, Node, SQL, pruebas, reinicios, navegador o cambios Git.

### Etapa 142 Consulta de eliminadas y vista bloqueada comprobadas

Fecha: 8 de noviembre de 2026.

- El usuario confirmó INV02-13/14: Mostrar eliminadas presenta QA-01 EDIT
  marcada Eliminada y Fuera del inventario, solo ofrece Ver y sus campos
  están bloqueados; al desmarcar vuelve a ocultarse.
- SELECT posterior confirma id 1 DE_BAJA, ubicación nula, borrado true y
  version 6. Conserva cuatro eventos de estado y siete administrativos.
  Consultar la unidad no generó modificaciones ni eventos administrativos nuevos.
- QA-02 id 2 sigue DISPONIBLE, Taller id 2, borrado false y version 0.
  La pública mantiene una presente/un lugar libre y el Taller una/cuatro.
- Próximo escenario INV02-16: abrir confirmación de eliminación de QA-02 en A,
  sin aceptarla; editar solo su código en B; confirmar luego en A para probar
  rechazo de versión antigua. La preparación y el rechazo siguen pendientes.
  Su eliminación directa desde DISPONIBLE se comprobará después.
- El asistente solo consultó datos y actualizó este diario, sin escrituras
  SQL, cambios de fuentes, pruebas automatizadas, reinicios o cambios Git.

### Etapa 143 Edición concurrente de QA-02 guardada en B

Fecha: 8 de noviembre de 2026.

- El usuario confirmó el guardado en B con la confirmación de eliminación
  preparada previamente en A. SELECT verificó id 2, nuevo código QA-02-EDIT,
  DISPONIBLE, Taller id 2, borrado false y version 1 (antes 0).
- Auditoría id 10 ACTUALIZAR, administrador 1, conserva únicamente el cambio
  de código y mantiene tipo, estado, ubicación y borrado. Motivo vacío válido
  para edición sin traslado.
- Ocupación sin cambios: pública una presente/un lugar libre; Taller una
  presente/cuatro libres. No se ha eliminado la unidad.
- Se indica confirmar el diálogo original de A sin recargar ni reabrir.
  Se espera conflicto por su versión anterior; el rechazo aún está pendiente.
- El asistente solo realizó SELECT y documentación, sin alterar datos,
  código, pestañas, procesos o Git.

### Etapa 144 Eliminación con versión anterior rechazada sin cambios

Fecha: 8 de noviembre de 2026.

- El usuario confirmó el diálogo original de A después del cambio guardado
  en B. Reportó La bicicleta fue modificada. Recargá los datos. Se acredita
  el rechazo real de INV02-16 con un formulario de confirmación desactualizado.
- SELECT conserva QA-02-EDIT id 2 PASEO USD 8.00, DISPONIBLE, Taller id 2,
  borrado false y version 1. Se mantuvo la edición válida realizada en B.
- La auditoría de esa unidad solo contiene CREAR id 2 y ACTUALIZAR id 10;
  el historial de estado solo su alta id 2. No hay salida, eliminación ni
  cambio de estado producido por la confirmación obsoleta.
- La pública mantiene una presente/un lugar libre y el Taller una/cuatro.
- Próximo paso: abrir una confirmación nueva y eliminar QA-02-EDIT
  directamente desde DISPONIBLE en Taller, sin Dar de baja antes (INV02-15).
  La ejecución exitosa y sus efectos siguen pendientes de verificar.
- Solo se realizaron SELECT y documentación; el asistente no alteró datos,
  fuentes, pestañas, sesiones, procesos o Git.

### Etapa 145 Eliminación directa desde DISPONIBLE verificada

Fecha: 8 de noviembre de 2026.

- El usuario abrió una confirmación nueva y eliminó QA-02-EDIT sin darla de baja
  previamente (INV02-15). SELECT confirma la fila id 2 existente, PASEO USD 8.00,
  DE_BAJA, estacion_id nulo, borrado true, fecha_alta conservada y version 2.
- Historial id 7 registra exactamente una transición DISPONIBLE a DE_BAJA,
  actor 1, origen USUARIO y motivo Retirada directa de prueba, además de su
  alta inicial. No se perdió la edición anterior ni se duplicaron transiciones.
- Auditoría id 11 ELIMINAR_SALIDA_DEFINITIVA, administrador 1, conserva estado
  anterior DISPONIBLE/nuevo DE_BAJA, estación anterior 2/nueva nula y borrado
  anterior false/nuevo true, con motivo y fecha registrados.
- El Taller quedó vacío: cero presentes, cinco lugares libres. La pública
  mantiene QA-03 id 3 DISPONIBLE, no borrada, version 0, y un lugar libre.
  QA-01 EDIT id 1 conserva su eliminación previa (version 6).
- Quedan verificados los recorridos manuales principales del bloque físico
  RN-INV-02: capacidad obsoleta rechazada, destino lleno, ubicación actual,
  traslado, pausa/habilitación, baja sin salida, eliminación con y sin baja
  previa, consulta de eliminadas y conflicto de versión, con evidencia real.
- No se presenta como ejecución manual integral de todos los casos: el retorno
  del motivo al volver a la ubicación actual tiene prueba Node, y los permisos
  del nuevo bloque tienen pruebas WebAuthorization, sin repetir aquí toda
  la ronda manual multirrol. RN-INV-03 continúa pendiente del módulo de incidencias.
- Se actualizaron índice y guía con los resultados; no se ejecutaron nuevas
  pruebas, fuentes, SQL de escritura, reinicios o cambios Git por el asistente.
  Siguiente paso propuesto: guardar el avance validado en la rama actual,
  sin mezclar todavía un nuevo módulo.

### Etapa 146 Retirada definitiva desde pública o interna implementada

Fecha: 8 de noviembre de 2026.

- El usuario autorizó expresamente implementar el cambio puntual tras informar
  push y árbol limpio. Se verificó git status sin cambios antes de editar;
  no se ejecutaron add, commit, push ni otras escrituras Git.
- RN-INV-04 amplía solamente el origen de Eliminar con retirada física real.
  BicicletaService.eliminarLogico sustituye exigirTaller por el nuevo helper
  exigirUbicacionParaSalida: rechaza ubicación nula y reutiliza la búsqueda
  activa con bloqueo de estación, tanto pública como interna. No exige cupo
  libre para retirar una unidad ya presente. No se alteró exigirTaller ni
  su uso en Dar de baja, que conserva ubicación y ocupación.
- Se mantienen ADMIN activo, versión, motivo y salidaFisicaConfirmada=true,
  comprobación de alquiler y estados permitidos. Se conserva la transacción
  que deja DE_BAJA/borrado true/ubicación nula, auditoría con origen real e
  historial únicamente si cambia el estado. No se borran filas físicamente.
- admin.js cambia únicamente la confirmación desde el taller por desde su
  ubicación registrada. No cambian payload, endpoint, acciones ni permisos.
- BicicletaServiceTest sustituye el rechazo antiguo desde pública por éxito
  con trazabilidad; agrega rechazo de origen público inactivo/inexistente y
  regresión de Dar de baja desde pública. Conserva las pruebas de Taller y
  demás restricciones. El test Node comprueba el texto independiente del Taller.
- Se actualizan documento 26, índice, referencias 08/13/20, guía 39 y comentarios
  de ambos Mermaid. Continúan 29 entidades y 55 relaciones, mismas columnas,
  PK y FK. No se modifican originales PDF/Word/DERv4 ni otros módulos.
- La etapa 135 conserva su resultado histórico bajo la restricción anterior;
  no acredita el nuevo comportamiento. Se prepararon pruebas puntuales en la
  sección 9 del documento 39. Su ejecución automática y manual queda pendiente
  del usuario; los resultados anteriores no se atribuyen al código modificado.
- RN-INV-03 por pérdida definitiva sigue pendiente de incidencias. No se
  habilitan EXTRAVIADA ni bajas sin ubicación mediante este recorrido físico.
- El asistente no ejecutó pruebas, compilación, reinicios, SQL ni navegador.
  Solo se realizaron edición y verificaciones estáticas de alcance/coherencia.

### Etapa 147 Pruebas automáticas de RN-INV-04 comprobadas

Fecha: 8 de noviembre de 2026.

- El usuario ejecutó BicicletaServiceTest y BicicletaControllerTest con
  BUILD SUCCESSFUL y cuatro tareas ejecutadas. El asistente leyó los XML:
  35 y 15 pruebas respectivamente, cero fallos, errores u omisiones;
  timestamps 17:25:26.766Z y 17:25:25.347Z.
- La salida aportada confirma 15 pruebas Node aprobadas, cero fallos u
  omisiones, incluido el texto de retirada desde su ubicación registrada.
  El comando --check de admin.js no informó errores.
- Los resultados cubren el cambio desde pública y las regresiones preparadas;
  no prueban por sí solos persistencia o bloqueo real de MySQL. Se mantiene
  pendiente la comprobación manual INV04-01 a INV04-04 de la guía 39.
- Próximo paso: reinicio controlado del perfil refactor por el usuario, sabiendo
  que create-drop descarta los registros QA, y preparación de cuenta ADMIN,
  estaciones y unidades para la ronda puntual. No se ha efectuado ese reinicio
  ni se ha comprobado un nuevo arranque en esta etapa.
- Solo se consultaron reportes/archivos y se actualizaron índice, guía y diario.
  El asistente no ejecutó pruebas, compilación, SQL, navegador, reinicios o
  escrituras Git. Los cambios de la etapa 146 siguen pendientes de commit.

### Etapa 148 Preparación de la ronda manual RN-INV-04 verificada

Fecha: 8 de noviembre de 2026.

- El usuario informó el nuevo arranque: PID 14340, puerto 8080, Started a las
  14:27:20.683; luego registró la cuenta y preparó los datos desde la interfaz.
  No es un reinicio ejecutado por el asistente.
- SELECT inicial confirmó admin.bicicletas id 1 activo con CLIENTE. Se entregó
  al usuario el INSERT idempotente para agregar ADMIN y este informó su acceso
  al panel. SELECT posterior confirma ambos roles, sin quitar CLIENTE.
- SELECT verifica Plaza QA Retiro id 1 pública activa, capacidad 2, dos
  bicicletas presentes y cero lugares libres. Taller QA Retiro id 2 interno
  activo, capacidad 5, cero presentes y cinco lugares libres.
- QA-RET-01 id 1 MONTANA y QA-RET-02 id 2 PASEO están DISPONIBLE, borrado false,
  version 0, ambas ubicadas en la pública id 1. No se probó todavía su baja
  o eliminación; no se atribuyen a esta ronda resultados históricos anteriores.
- Próximo caso INV04-01: intentar Dar de baja QA-RET-01 desde la pública con
  motivo. Debe rechazar por exigir Taller y mantener estado, ubicación,
  borrado, versión y ocupación. La eliminación definitiva se probará después.
- Se actualizan índice, guía y diario. El asistente solo efectuó SELECT y
  lectura/edición documental, sin escrituras SQL, pruebas, navegador,
  reinicios, cambios de código o escrituras Git.

### Etapa 149 Baja desde pública rechazada sin cambios (INV04-01)

Fecha: 8 de noviembre de 2026.

- El usuario informó el rechazo de Dar de baja QA-RET-01 desde la estación
  pública. SELECT confirma id 1 DISPONIBLE, estación 1, borrado false y version 0.
- El historial solo tiene su alta inicial id 1, sin transición a DE_BAJA;
  auditoría solo CREAR id 1. No se registraron eventos de una baja exitosa.
- Plaza QA Retiro conserva dos presentes/cero libres; Taller QA Retiro cero
  presentes/cinco libres. Se acredita INV04-01 y que RN-INV-04 no debilitó
  la exigencia de Taller para Dar de baja por separado.
- Siguiente caso INV04-02: abrir Eliminar para esa unidad y cancelar, comprobar
  el nuevo texto y la ausencia de cambios. La eliminación confirmada desde
  pública todavía está pendiente y no se da por probada con este rechazo.
- El asistente solo realizó SELECT y actualizó documentación, sin modificar
  datos, código, procesos, navegador ni Git.

### Etapa 150 Cancelación de eliminación desde pública sin cambios (INV04-02)

Fecha: 8 de noviembre de 2026.

- El usuario confirmó el texto desde su ubicación registrada y canceló el
  diálogo Eliminar QA-RET-01. Se acredita la variante cancelar de INV04-02;
  no se presenta como otra prueba manual de motivo vacío.
- SELECT conserva QA-RET-01 id 1 DISPONIBLE, estación 1, borrado false y version
  0. Sigue teniendo un único evento de estado y uno administrativo, los de
  alta ya verificados en la etapa anterior. No se registró una eliminación.
- Plaza QA Retiro continúa con dos presentes/cero libres y Taller QA Retiro
  con cero presentes/cinco libres. Cancelar no liberó un espacio.
- Próximo caso INV04-03: abrir un diálogo nuevo y confirmar la retirada
  definitiva QA desde la pública con motivo. Su resultado y trazabilidad
  siguen pendientes de comprobar; no se ejecutó esa escritura por el asistente.
- Solo se realizaron SELECT y edición documental, sin cambios en datos,
  código, procesos, navegador o Git.

### Etapa 151 Eliminación desde pública con persistencia y trazabilidad verificadas

Fecha: 8 de noviembre de 2026.

- El usuario confirmó Eliminar QA-RET-01 desde la pública con motivo de
  retirada definitiva (INV04-03). SELECT confirma la fila id 1 conservada,
  MONTANA, DE_BAJA, borrado true, estación nula, fecha_alta 2026-11-08 y version 1.
- Historial id 3 conserva una sola transición DISPONIBLE a DE_BAJA además
  del alta id 1, actor 1, origen USUARIO y motivo registrado. Auditoría id 3
  ELIMINAR_SALIDA_DEFINITIVA identifica administrador 1, origen público
  estacionId=1 y destino null, estado/borrado anterior y nuevo, fecha y motivo.
  No se inventó un paso por Taller ni se borró físicamente la bicicleta.
- Plaza QA Retiro pasó de dos presentes/cero libres a una presente/un libre.
  Taller QA Retiro conserva cero presentes/cinco libres. QA-RET-02 id 2 sigue
  PASEO, DISPONIBLE, estación 1, borrado false y version 0.
- Se acredita persistencia y trazabilidad de la retirada desde pública incluso
  con origen inicialmente lleno. Pendiente confirmar visualmente Mostrar
  eliminadas/Ver para esta unidad y ejecutar INV04-04 con la segunda bicicleta.
- Solo SELECT y documentación por el asistente, sin escrituras SQL, cambios
  en fuentes, pruebas automáticas, navegador, reinicios o escrituras Git.

### Etapa 152 Consulta de eliminada y traslado/baja en Taller comprobados

Fecha: 8 de noviembre de 2026.

- El usuario confirmó la presentación de QA-RET-01 al mostrar eliminadas:
  marcada Eliminada, ubicación Fuera del inventario y solo Ver sin edición.
  Se completa la comprobación visual asociada a INV04-03; SELECT conserva
  id 1 DE_BAJA, borrado true, ubicación nula, version 1 y sus eventos previos.
- El usuario trasladó QA-RET-02 al Taller y después la dio de baja (INV04-04).
  SELECT confirma id 2 PASEO, DE_BAJA, borrado false, estación 2 Taller QA Retiro,
  fecha_alta 2026-11-08 conservada y version 2.
- Auditoría id 4 ACTUALIZAR registra estación 1 a 2, estado DISPONIBLE y borrado
  false conservados, administrador 1 y motivo Traslado de prueba al taller.
  No hay transición de estado ficticia producida por el traslado.
- Auditoría id 5 CAMBIAR_ESTADO e historial id 4 registran DISPONIBLE a DE_BAJA,
  ubicación 2 y borrado false conservados, actor/administrador 1, fecha y motivo
  Fin de vida útil de prueba. Se distingue baja operativa de eliminación.
- Plaza QA Retiro quedó vacía con dos espacios libres; Taller QA Retiro tiene
  una unidad presente y cuatro libres. La baja no liberó su lugar físico.
- Los casos puntuales INV04-01, INV04-02 (variante cancelar), INV04-03 e INV04-04
  quedan comprobados, con reporte del usuario y evidencia SELECT cuando aplica.
  Se mantienen las 50 pruebas Java y 15 Node registradas en etapa 147; no se
  ejecutó una nueva suite ni se afirma una nueva prueba simultánea de bloqueos.
  La validación de motivo vacío sigue cubierta automáticamente y por la ronda
  anterior, no se presenta como otra ejecución manual en esta ronda.
- RN-INV-04 queda verificada en este alcance. RN-INV-03 por pérdida y los demás
  módulos pendientes del DER no se dan por implementados. Próximo paso propuesto:
  guardar el cambio y la evidencia en la rama actual mediante commit/push.
- El asistente solo realizó SELECT y actualización documental; no alteró datos,
  código, procesos, navegador, pruebas o Git.

### Etapa 153 Catálogo de estados de detalle creado manualmente y revisado

Fecha: 8 de noviembre de 2026.

- Se verificó el checkpoint 632c210 del bloque RN-INV-04 y la referencia remota
  sin commits divergentes durante la revisión anterior. El usuario mantiene
  implementación manual salvo autorización expresa para un bloque puntual.
- El usuario creó EstadoAlquilerDetalle y EstadoAlquilerDetalleRepository y
  aportó compileJava BUILD SUCCESSFUL, una tarea ejecutada. Se leyeron ambos
  archivos: paquetes/rutas correctos, entidad estado_alquiler_detalle, PK Integer
  generada, código obligatorio/único de hasta 30 caracteres, validación de los
  siete códigos aprobados y normalización trim/mayúsculas con Locale.ROOT.
  Repositorio interface JpaRepository y búsqueda Optional por código correctos.
- No hay precarga ni consumidores de este catálogo todavía; no se modificó
  EstadoAlquiler enum, Alquiler, su servicio o los endpoints existentes.
  Compilar no acredita creación/UNIQUE en MySQL ni pruebas de validación.
- Siguiente bloque manual: añadir su repositorio al constructor del cargador
  refactor y crear solo estados faltantes; añadir el mock y pruebas específicas
  de códigos válidos/carga repetida. Se entregan instrucciones, no se declara
  implementada ni probada la precarga antes de revisar los cambios del usuario.
- El asistente solo leyó fuentes y actualizó documentación, sin editar Java,
  ejecutar compilación/pruebas, SQL, navegador, reinicios o escrituras Git.

### Etapa 154 Precarga de estados de detalle y pruebas incorporadas

Fecha: 8 de noviembre de 2026.

- El usuario autorizó expresamente implementar solo los bloques 1 y 2 de la
  propuesta anterior, reservándose guardar/comprobar (bloque 3). Se leyeron
  las fuentes y se verificó que no había bloques ya pegados en cargador/test.
  Se preservaron los cambios y el staging de las dos clases creadas por él.
- CargaCatalogosRefactor incorpora los imports de entidad/repositorio, el
  atributo final y su inyección por constructor, conservando las seis
  dependencias anteriores. Dentro de run, antes de roles, agrega los siete
  códigos aprobados de detalle y guarda solo los que no encuentra por código.
  Continúan Profile refactor y la transacción existentes; no se cambian otros
  catálogos, valores QA, entidades, endpoints o configuración de base.
- CargaCatalogosRefactorTest incorpora el mock para la nueva dependencia y
  dos pruebas: carga exacta/validación/normalización/rechazo de códigos ajenos;
  y dos ejecuciones con RESERVADO ya existente, conservándolo y guardando solo
  los seis faltantes. Se mantienen íntegramente las seis pruebas anteriores.
  La comprobación de carga repetida usa repositorios simulados, no una prueba
  de UNIQUE, concurrencia o persistencia real en MySQL.
- Se revisó el diff y diff --check sin errores. No se ejecutaron Gradle,
  compilación, pruebas, SQL o reinicios. El usuario debe ejecutar el filtro
  CargaCatalogosRefactorTest; ocho métodos preparados, sin resultado todavía.
  El proceso Java ya abierto no cargó este nuevo código por editar las fuentes.
- Se actualizan índice y estado parcial de RN-ALQ-09. No se modifican
  EstadoAlquiler enum, Alquiler, sus consumidores, DER, originales o Git.
  La precarga no implementa aún los recorridos de AlquilerDetalle.

### Etapa 155 Ocho pruebas del cargador verificadas y siguiente paso preparado

Fecha: 8 de noviembre de 2026.

- El usuario ejecutó el filtro CargaCatalogosRefactorTest con BUILD SUCCESSFUL,
  tres tareas ejecutadas y una actualizada. Se leyó su XML, timestamp
  2026-11-08T18:34:58.110Z: ocho pruebas, cero fallos, errores u omisiones,
  incluidas las dos nuevas sobre estados de detalle.
- Se acredita la validación y carga repetida con repositorios simulados,
  conservando las seis pruebas anteriores. No acredita DDL, UNIQUE, bloqueo
  concurrente ni precarga real del proceso MySQL, que no se reinició.
- La siguiente entidad aprobada es ESTADO_ALQUILER de la cabecera con RESERVADO,
  EN_PROGRESO, FINALIZADO, CANCELADO y VENCIDO. El nombre Java está ocupado por
  el enum legado (INICIADO, FINALIZADO, CANCELADO, RECHAZADO), todavía consumido
  por Alquiler, sus repositorio/servicio, BicicletaService y dos clases de pruebas.
- Se entregan instrucciones manuales para renombrar solo ese enum a
  CodigoEstadoAlquiler y actualizar referencias, incluidas dos constantes en
  cadenas JPQL. Es un paso temporal de compatibilidad, no un nuevo catálogo
  de negocio, una nueva tabla ni un cambio de valores persistidos. Se retirará
  con sus consumidores al conectar la nueva cabecera/detalles.
- La entidad EstadoAlquiler todavía no se crea y no se cambia el flujo de
  reservas/retiros. Se propone comprobar el renombre antes de crear la entidad
  para evitar que las referencias antiguas se resuelvan silenciosamente al
  nuevo tipo. No se ejecutó ese renombre ni otro cambio Java en esta etapa.
- El asistente solo leyó fuentes/reportes y actualizó los MD; no ejecutó
  pruebas, compilación, SQL, reinicios, navegador o escrituras Git.

### Etapa 156 Renombre temporal del enum de alquiler implementado

Fecha: 8 de noviembre de 2026.

- El usuario autorizó implementar exclusivamente el renombre y sus referencias,
  dejando a su cargo el bloque 3 de guardar/comprobar. Se preservaron los
  cambios previos y el staging de las clases del catálogo de detalle.
- EstadoAlquiler.java se movió a CodigoEstadoAlquiler.java y su declaración
  enum se renombró. Conserva INICIADO, FINALIZADO, CANCELADO y RECHAZADO, mismo
  orden y comportamiento. No se creó aún la nueva entidad EstadoAlquiler.
- Se actualizaron el tipo del atributo/constructor/getter/setter de Alquiler,
  los usos de AlquilerService y la comprobación de alquiler activo de
  BicicletaService. Se mantiene Enumerated STRING y sus valores persistidos;
  no se modifican columnas, transiciones, importes o respuestas HTTP.
- AlquilerRepository actualiza el parámetro de existsByBicicletaAndEstado y
  las constantes de FINALIZADO/INICIADO en sus dos cadenas JPQL. Se adaptaron
  import/referencias de AlquilerServiceTest y los dos casos de alquiler activo
  de BicicletaServiceTest, sin cambiar sus expectativas o escenarios.
- Una comparación estática de huellas antes/después, normalizando finales de
  línea, confirma que los siete archivos solo cambiaron por la sustitución
  exacta del identificador y el movimiento del enum. Búsqueda por palabra
  completa sin referencias residuales a EstadoAlquiler en src; EstadoAlquilerDetalle
  y su precarga no fueron alterados por este bloque. diff --check sin errores.
- El usuario debe ejecutar los filtros CargaCatalogosRefactorTest,
  AlquilerServiceTest, BicicletaServiceTest y BicicletaControllerTest. No se
  ejecutaron aquí compilación, pruebas, arranque, SQL o escrituras Git.
  Las pruebas unitarias no sustituyen la futura validación de JPQL al arrancar.
- Este enum es compatibilidad temporal del flujo anterior y debe retirarse
  con sus consumidores al conectar cabecera/detalles nuevos, no conservarse
  como catálogo paralelo definitivo. DER y reglas aprobadas no se modifican.

### Etapa 157 Renombre comprobado y entidad de estados de cabecera preparada

Fecha: 8 de noviembre de 2026.

- El usuario ejecutó los cuatro filtros indicados con BUILD SUCCESSFUL, tres
  tareas ejecutadas y una actualizada. Se leyeron sus XML: CargaCatalogosRefactorTest
  8, AlquilerServiceTest 8, BicicletaServiceTest 35 y BicicletaControllerTest 15;
  total 66, todos sin fallos, errores u omisiones. Timestamps del 8 de noviembre
  entre 18:41:24.169Z y 18:41:26.217Z. No se reejecutaron por el asistente.
- Se comprobó CodigoEstadoAlquiler con sus cuatro valores anteriores y ausencia
  de referencias a la palabra completa EstadoAlquiler en src. El nombre queda
  libre para la entidad del catálogo; no se comprobó aún JPQL mediante arranque.
- Se entregan instrucciones manuales para crear EstadoAlquiler como entidad
  estado_alquiler y EstadoAlquilerRepository, con PK Integer generada y código
  obligatorio/único de hasta 20 caracteres, normalizado trim/mayúsculas y
  restringido a RESERVADO, EN_PROGRESO, FINALIZADO, CANCELADO y VENCIDO.
- No se implementaron esos archivos ni su precarga en esta etapa. Alquiler y
  sus consumidores siguen temporalmente con CodigoEstadoAlquiler; no se agrega
  todavía la FK ni se cambian transiciones, reservas o importes. Compilar luego
  el nuevo catálogo no acreditará su carga en MySQL ni el módulo completo.
- Solo lectura de reportes/fuentes y actualización documental por el asistente,
  sin edición Java, compilación/pruebas, SQL, reinicios, navegador o cambios Git.

### Etapa 158 Entidad y repositorio de estados de cabecera revisados

Fecha: 8 de noviembre de 2026.

- El usuario creó EstadoAlquiler y EstadoAlquilerRepository, guardó y aportó
  compileJava BUILD SUCCESSFUL con una tarea ejecutada. Se leyeron ambos:
  clase Entity/Table estado_alquiler, PK Integer IDENTITY, código obligatorio
  y único de hasta 20 caracteres, cinco códigos aprobados y normalización
  trim/mayúsculas Locale.ROOT correctos. Repositorio interface JpaRepository
  con Optional findByCodigo correcto, ubicado en el paquete correspondiente.
- Búsqueda de referencias confirma que la entidad nueva solo aparece en su
  definición/repositorio. El flujo anterior mantiene CodigoEstadoAlquiler;
  no se sustituyeron referencias de legado por la entidad de forma accidental.
- La precarga de cabecera todavía no existe. Se entregan instrucciones manuales
  para agregar imports, repositorio/constructor y carga de cinco estados
  faltantes en CargaCatalogosRefactor, además del mock y dos pruebas análogas
  a las del detalle. Las ocho pruebas existentes se deben conservar; serían
  diez al incorporar las propuestas. No se declaran implementadas ni ejecutadas.
- Compilar no acredita DDL/UNIQUE ni persistencia de este catálogo. No se
  agregan FK o transiciones al alquiler ni se cambia DER, configuración o datos.
- Solo lectura de fuentes y edición documental por el asistente; sin cambios
  Java, compilación/pruebas, SQL, reinicios, navegador o escrituras Git.

### Etapa 159 Precarga de estados de cabecera y dos pruebas incorporadas

Fecha: 8 de noviembre de 2026.

- El usuario autorizó implementar solo los bloques 1 y 2 de la propuesta de
  precarga/pruebas, reservándose el bloque 3 de guardar y comprobar. Se leyó
  el cargador y su test antes de editar; no había partes ya pegadas de cabecera.
  Se preservaron los cambios previos y el staging de los archivos del usuario.
- CargaCatalogosRefactor agrega imports de EstadoAlquiler y su repositorio,
  atributo final e inyección por constructor, conservando las siete dependencias
  anteriores. Dentro de run, antes de estados de detalle, carga RESERVADO,
  EN_PROGRESO, FINALIZADO, CANCELADO y VENCIDO solo cuando no existen por código.
  Se mantienen el perfil refactor, la transacción y todas las precargas previas.
- CargaCatalogosRefactorTest incorpora el mock de la nueva dependencia y dos
  métodos: cinco códigos exactos válidos, normalización y rechazo de códigos
  ajenos/legados; y dos cargas con RESERVADO previo, conservándolo y guardando
  solo los cuatro faltantes. Se mantienen las ocho pruebas anteriores.
- No se modifican EstadoAlquiler, su repositorio, CodigoEstadoAlquiler, Alquiler
  o sus consumidores; tampoco endpoints, frontend, seguridad, DER o configuración.
  La precarga no conecta aún el catálogo nuevo al flujo de contratación.
- Se revisaron fuentes/diff y diff --check sin errores. Diez métodos de prueba
  preparados; ejecución pendiente del usuario mediante el filtro del cargador.
  No se ejecutaron Gradle, compilación, pruebas, SQL, reinicios, navegador o
  escrituras Git. El proceso Java abierto no se actualizó por editar las fuentes.
- Se actualizan índice y avance parcial de RN-ALQ-07; la carga repetida se
  comprobará con repositorios simulados, sin acreditar por ello UNIQUE,
  concurrencia o persistencia real del catálogo en MySQL.

### Etapa 160 Diez pruebas del cargador verificadas y checkpoint preparado

Fecha: 8 de noviembre de 2026.

- El usuario ejecutó CargaCatalogosRefactorTest con BUILD SUCCESSFUL, tres tareas
  ejecutadas y una actualizada. Se leyó el XML con timestamp
  2026-11-08T18:51:52.742Z: diez pruebas, cero fallos, errores u omisiones,
  incluidas carga/validación y repetición del nuevo catálogo de cabecera.
- Quedan comprobadas las precargas de ambos catálogos de alquiler mediante
  repositorios simulados. No se acredita DDL, UNIQUE real, evaluación JPQL
  de repositorios ni carga en la aplicación abierta: no hubo nuevo arranque.
- Se revisaron las relaciones de ALQUILER/ALQUILER_DETALLE en DER v5 y el
  modelo actual. La nueva cabecera requiere sus campos/estado y detalles; el
  detalle referencia además ORDEN_MANTENIMIENTO e INCIDENCIA, todavía ausentes.
  La siguiente implementación debe organizar estas dependencias sin inventar
  tablas paralelas ni sustituir FK por identificadores sueltos provisionales.
- Antes de esa transformación coordinada se indica guardar en Git el bloque
  ya comprobado: dos catálogos, repositorios, precarga, renombre del enum y
  pruebas/documentación. La rama sigue codex/refactorizacion-practicas-iii y
  HEAD 632c210; no se afirma otro commit o push hasta que el usuario lo ejecute.
- Solo se leyeron fuentes/reportes/estado Git y se actualizó documentación.
  El asistente no modificó Java, ejecutó pruebas/compilación, SQL, reinicios,
  navegador ni escrituras Git. El ciclo nuevo de alquiler sigue pendiente.

### Etapa 161 Checkpoint confirmado y dependencia de órdenes preparada

Fecha: 8 de noviembre de 2026.

- Se comprobó árbol limpio al inicio y commit b5d0893 Agregar catalogos de
  estados de alquiler y detalle con precarga y pruebas. HEAD y la referencia
  origin/codex/refactorizacion-practicas-iii no tienen commits divergentes (0/0).
  No se ejecutó fetch o escritura Git por el asistente.
- Revisión de DER v5 y RN-MAN-01/02/27: AlquilerDetalle referencia una orden de
  mantenimiento, que necesita TipoMantenimiento y EstadoOrdenMantenimiento.
  Las clases de estas dependencias todavía no existen. El detalle también
  referencia Incidencia, con relación inversa obligatoria al propio detalle;
  su incorporación debe coordinarse, no reemplazar esas FK por IDs provisionales.
- Se entregan instrucciones manuales para el primer catálogo de soporte:
  EstadoOrdenMantenimiento, tabla estado_orden_mantenimiento, PK Integer
  generada, código obligatorio/único máximo 20, normalización trim/mayúsculas
  y cinco estados CREADA, EN_PROCESO, PAUSADA, FINALIZADA, CANCELADA. Repositorio
  JpaRepository con Optional findByCodigo. Son estados ya aprobados, no nuevos.
- Esto prepara dependencias del mismo DER, no implementa aún mantenimiento,
  pantallas técnicas, generación automática o habilitación de bicicletas.
  Después deben incorporarse tipo/orden y las referencias de detalle/incidencia
  al transformar coordinadamente la cabecera y el flujo antiguo.
- No se crearon clases Java ni precarga en esta etapa; el usuario las creará
  y compilará. Se conservan catálogos actuales, enum de compatibilidad y reglas.
- El asistente solo leyó fuentes/estado Git y actualizó documentación; sin
  compilación/pruebas, SQL, reinicios, navegador o escrituras Git.

### Etapa 162 Estado de orden revisado y catálogo de tipos preparado

Fecha: 8 de noviembre de 2026.

- El usuario creó EstadoOrdenMantenimiento y su repositorio y aportó
  compileJava BUILD SUCCESSFUL, una tarea ejecutada. Se leyeron los archivos:
  rutas/paquetes correctos, Entity/Table, PK Integer IDENTITY, código único
  obligatorio máximo 20, cinco códigos aprobados y normalización Locale.ROOT.
  Repositorio interface JpaRepository y Optional findByCodigo correctos.
- Las referencias actuales solo incluyen esa entidad y repositorio; no hay
  precarga ni consumidores técnicos. No se acredita DDL, UNIQUE, validaciones
  ejecutadas ni reglas del módulo por compilar estas dos clases.
- Siguiente bloque manual: TipoMantenimiento y repositorio, conforme al DER
  y documentos 12/27: id generado, código obligatorio/único PREVENTIVO o
  CORRECTIVO, normalizado trim/mayúsculas, y descripcion de hasta 255 caracteres
  con trim. La descripción no es clave ni se copia a cada orden; se deja
  opcional porque la especificación revisada exige expresamente el código,
  sin imponer por este paso otra obligatoriedad de descripción.
- Se entregan instrucciones, no se crean esas clases ni su precarga. No se
  duplican intervalos preventivos de TipoBicicleta ni se mezclan tipo y estado
  de orden. Se mantienen las dependencias futuras de orden/detalle/incidencia.
- Solo lectura de fuentes y edición documental por el asistente, sin cambios
  Java, compilación/pruebas, SQL, reinicios, navegador o escrituras Git.

### Etapa 163 Tipo de mantenimiento revisado y precarga conjunta preparada

Fecha: 8 de noviembre de 2026.

- El usuario creó TipoMantenimiento y su repositorio y aportó compileJava
  BUILD SUCCESSFUL, una tarea ejecutada. Se revisaron fuentes: tabla
  tipo_mantenimiento, PK Integer IDENTITY, código obligatorio/único máximo
  20, PREVENTIVO/CORRECTIVO y normalización Locale.ROOT correctos; descripción
  opcional máximo 255 con trim; repositorio JpaRepository/Optional correcto.
- La precarga de ambos catálogos de mantenimiento todavía no existe. Se
  entregan instrucciones manuales para agregar las dos dependencias al cargador,
  conservar las ocho anteriores y cargar solo tipos/estados faltantes. Tipos
  PREVENTIVO/CORRECTIVO con descripciones de su finalidad, estados CREADA,
  EN_PROCESO, PAUSADA, FINALIZADA y CANCELADA; no se alteran otros catálogos.
- Se proponen dos pruebas conjuntas añadidas a las diez anteriores: carga de
  registros exactos y validación/normalización; y repetición sin duplicar o
  reemplazar tipos/estados existentes, incluida descripción conservada.
  Añadir los dos mocks es necesario por las nuevas dependencias de constructor.
  Doce métodos previstos, todavía no implementados o ejecutados.
- No se crean órdenes ni se aplican transiciones o revisiones automáticas.
  No se cambia el perfil, DER o configuración. Compilar las clases no prueba
  UNIQUE o persistencia real; la posterior prueba propuesta usa repositorios
  simulados y no necesita reiniciar la aplicación.
- El asistente solo leyó fuentes y actualizó documentación, sin edición Java,
  compilación/pruebas, SQL, reinicios, navegador o escrituras Git.

### Etapa 164 Precarga de mantenimiento pegada correctamente y doce pruebas verificadas

Fecha: 8 de noviembre de 2026.

- El usuario incorporó manualmente ambos bloques y ejecutó el filtro del
  cargador con BUILD SUCCESSFUL, tres tareas ejecutadas y una actualizada.
  XML timestamp 2026-11-08T19:10:40.994Z: doce pruebas, cero fallos, errores
  u omisiones, incluidas las dos nuevas de mantenimiento.
- Se leyeron cargador/test y su diff: imports y atributos dentro de la clase,
  diez dependencias correctamente recibidas/asignadas por constructor, carga
  de dos tipos/cinco estados dentro de run antes de roles. Se mantiene
  Profile refactor, transacción y todas las cargas anteriores, sin duplicados.
- En el test los dos mocks están junto a las dependencias existentes y los
  dos métodos dentro de la clase, separados de los anteriores. El diff conserva
  íntegras las diez pruebas previas. Se verifican códigos exactos, validación,
  normalización y repetición sin reemplazar objetos/descripción existentes.
- No se encontraron correcciones Java necesarias en este bloque. La prueba
  ejecutada compila y evalúa con JUnit/Mockito/Bean Validation, sin usar MySQL.
  No se acredita aún DDL/UNIQUE, carga real al arrancar o flujos de órdenes.
- Se aclaró el propósito: CargaCatalogosRefactor prepara catálogos persistidos
  al iniciar la aplicación bajo el perfil refactor; su test verifica esa
  lógica con repositorios simulados y no carga la base real.
- Solo inspección de fuentes/reportes y actualización documental por el
  asistente, sin cambios Java, ejecución de pruebas/compilación, SQL, reinicios,
  navegador o escrituras Git. Las entidades operativas siguientes siguen pendientes.

### Etapa 165 Entidad de orden de mantenimiento preparada para creación manual

Fecha: 8 de noviembre de 2026.

- Se revisaron el bloque ORDEN_MANTENIMIENTO del DER y el documento 27,
  conservando sus dieciséis campos y cuatro FK: bicicleta, tipo y estado
  obligatorios; técnico opcional inicialmente y en la pausa administrativa
  aprobada. No se duplica la estación de la bicicleta ni se limita a una
  sola orden histórica por unidad mediante UNIQUE.
- Se entregan instrucciones manuales de OrdenMantenimiento y repositorio
  JpaRepository simple. PK Integer IDENTITY y version Integer administrados;
  relaciones LAZY sin cascadas destructivas; fechaCreacion inicializada por
  backend al construir y no actualizable por JPA, sin setter público. Fechas
  de inicio/fin permanecen opcionales hasta su operación real.
- Resultado técnico opcional APTA/NO_APTA como códigos de representación del
  resultado aprobado, no un catálogo/enum o estado nuevo. Costos internos
  opcionales no negativos, sin inicializar pendientes a cero. Se propone como
  precisión física DECIMAL(12,2) y Digits(10,2), explicitada en las instrucciones;
  moneda admite solo ARS cuando se informa. No se registra cargo al cliente.
- Diagnóstico, descripción de trabajo, observaciones y motivo de pausa/cancelación
  usan TEXT opcional y trim; no se hacen obligatorios al crear. El servicio deberá
  controlar sus requisitos por transición, moneda cuando exista importe,
  fechas, técnico activo/asignado y una sola orden abierta. La entidad sola
  no implementa esas reglas ni sustituye trazabilidad e historial.
- No se crean los archivos por el asistente ni endpoints, órdenes reales o
  automatismos. Se indica compilar manualmente y revisar antes de agregar el
  historial y las operaciones. No se modifican catálogos, consumidores previos,
  DER, base o configuración. Continúa pendiente la conexión a alquiler/detalles.
- Solo lectura de fuentes y edición documental por el asistente; sin cambios
  Java, compilación/pruebas, SQL, reinicios, navegador o escrituras Git.

### Etapa 166 Orden revisada e historial preparado para creación manual

Fecha: 8 de noviembre de 2026.

- El usuario creó OrdenMantenimiento y su repositorio, guardó y aportó
  compileJava BUILD SUCCESSFUL, una tarea ejecutada. Se leyeron íntegramente:
  dieciséis campos coincidentes con el DER, cuatro FK LAZY sin cascadas, tres
  obligatorias y técnico opcional; fechas/resúmenes, costo DECIMAL(12,2), TEXT,
  normalización y version Integer correctos. Repositorio JpaRepository simple
  correctamente ubicado. Sin correcciones Java necesarias en ese bloque.
- Compilar no prueba DDL/UNIQUE, controles de transiciones, importe/moneda,
  roles, una sola orden abierta o registro automático de eventos. No se ejecutó
  un nuevo arranque ni se persistieron órdenes por este avance de modelos.
- Se contrastó el historial con RN-TRZ-01 (documento 36, secciones 3/4) y
  documento 27: once campos, FK a orden obligatoria, dos FK de estado opcionales
  por eventos de datos, FK de actor opcional solo para SISTEMA, acción, fecha,
  origen, observaciones y valores anteriores/nuevos. No una tabla adicional
  de atención ni el nombre histórico de historial solo de estados.
- Se entregan instrucciones manuales de HistorialOrdenMantenimiento y repositorio.
  Mapeo sin cascadas y sin setters públicos, columnas/joins no actualizables;
  fecha del backend. Constructor controla origen USUARIO/SISTEMA con actor
  coherente y rechaza estado anterior sin nuevo o transición al mismo código.
  Sin estados para eventos solo de datos. Código de acción se normaliza y su
  lista/contexto se validará en el servicio, no por un estado nuevo del catálogo.
- El constructor no determina roles, transiciones autorizadas o primer evento:
  siguen pendientes junto con el servicio y escritura atómica de orden/historial/
  auditoría cuando corresponda. Valores estructurados deben generarse con datos
  permitidos del backend, no secretos o una entidad libre enviada por el cliente.
- No se crearon archivos Java por el asistente; se solicita compilación manual
  y revisión posterior. Solo fuentes leídas y documentación actualizada, sin
  compilación/pruebas, SQL, reinicios, navegador o escrituras Git.

### Etapa 167 Historial de orden y repositorio revisados

Fecha: 8 de noviembre de 2026.

- El usuario creó HistorialOrdenMantenimiento y su repositorio, guardó y aportó
  compileJava BUILD SUCCESSFUL, una tarea ejecutada. Se leyeron ambos archivos
  completos y contrastaron sus once campos con DER v5/RN-TRZ-01.
- Tabla, PK Integer IDENTITY y cuatro FK correctas: orden obligatoria, estados
  anterior/nuevo opcionales según evento y actor opcional según origen. LAZY,
  sin cascadas destructivas ni unicidades que limiten la historia. Constructor
  protegido sin argumentos válido para JPA; público de registro con fecha del
  backend, normalización de acción/origen y observaciones correctas.
- Se revisaron las comprobaciones de origen/actor, estado anterior sin nuevo
  y transición al mismo código; getters sin setters y columnas/joins no
  actualizables por JPA. El repositorio es interface JpaRepository parametrizado
  con la entidad e Integer, en su paquete correcto. No se detectaron errores
  de pegado, campos omitidos o correcciones Java necesarias en este bloque.
- El formato de la condición Objects.equals difiere solo en sangría; no cambia
  su funcionamiento ni justifica un cambio de código por esta revisión.
- No se ejecutaron pruebas de constructor/validaciones, arranque/DDL o persistencia
  real. Compilar no demuestra esos controles en ejecución. Quedan pendientes
  las pruebas de entidades y el servicio que verificará acción/contexto, rol,
  primer evento, transiciones, datos permitidos y escritura atómica con la orden.
  updatable=false/no setters no son un bloqueo de SQL directo o borrado físico;
  las operaciones normales del módulo no deben exponer esas escrituras.
- Solo lectura de fuentes y actualización documental por el asistente, sin
  cambios Java, compilación/pruebas, SQL, reinicios, navegador o escrituras Git.

### Etapa 168 Pruebas unitarias de la entidad de orden preparadas

Fecha: 8 de noviembre de 2026.

- Se revisaron las entidades de orden/historial y el patrón JUnit 5 + Bean
  Validation de TipoBicicletaTest. Para mantener el bloque manual acotado se
  entregan primero instrucciones de OrdenMantenimientoTest; las pruebas del
  historial se incorporarán después, sin omitirlas ni dar sus controles por probados.
- Se proponen seis métodos: creación con campos iniciales y técnico opcional;
  rechazo de referencias obligatorias nulas; costos conocidos cero/positivo/
  máximo; rechazo de negativos, más de dos decimales y desbordamiento, sin
  redondeo silencioso; normalización/validación de resultado y moneda; trim
  de los cuatro textos. Fixture sin persistencia para probar esta entidad,
  no existencia real de FK o validaciones propias de entidades relacionadas.
- No se prueba ni aprueba importe sin moneda, resultado por transición,
  rol/asignación de técnico, cronología o unicidad de una orden abierta;
  son reglas del servicio todavía pendientes, no de estas seis pruebas.
- Se indica ejecutar filtro OrdenMantenimientoTest junto con las doce pruebas
  existentes del cargador: dieciocho previstas una vez creado el archivo.
  No se creó el test, no se ejecutó Gradle ni se consultó/modificó MySQL.
- Solo lectura de fuentes y actualización de documentación por el asistente,
  sin cambios Java, pruebas/compilación, reinicios, navegador o escrituras Git.

### Etapa 169 Dieciocho pruebas verificadas y pruebas de historial preparadas

Fecha: 8 de noviembre de 2026.

- El usuario creó OrdenMantenimientoTest y ejecutó los filtros de orden y
  cargador con BUILD SUCCESSFUL. Se leyó el archivo completo sin errores de
  pegado y los dos XML: seis pruebas de orden, timestamp 19:42:52.336Z, y doce
  del cargador, timestamp 19:42:50.928Z; dieciocho sin fallos, errores u omisiones.
  Las dos tareas actualizadas informadas por Gradle no se presentan como una
  ejecución nueva de cada tarea. No se reejecutaron pruebas por el asistente.
- Se entregan instrucciones manuales de HistorialOrdenMantenimientoTest con
  ocho casos: creación humana normalizada/con datos conservados; creación
  SISTEMA sin actor; transición efectiva; datos sin transición; orígenes o
  actores incoherentes; estados incoherentes/iguales; orden ausente; acción
  obligatoria y máximo 50. JUnit 5/Bean Validation, sin Spring o MySQL.
- El usuario debe crear el archivo y ejecutar los tres filtros: veintiséis
  pruebas previstas. El test del historial todavía no existe, no se crean
  clases por el asistente y no se dan por comprobados esos ocho casos.
- No se prueban aún permisos, lista de acciones autorizadas, primer evento,
  datos permitidos, transición de negocio o persistencia/atomicidad. El fixture
  solo verifica el constructor/atributos de historial, no FK reales o roles de
  usuarios referenciados. Se mantienen pendientes las operaciones del servicio.
- Solo lectura de fuentes/reportes y edición documental, sin cambios Java,
  compilación/pruebas, SQL, reinicios, navegador o escrituras Git.

### Etapa 170 Veintiséis pruebas verificadas y checkpoint del modelo preparado

Fecha: 8 de noviembre de 2026.

- El usuario creó HistorialOrdenMantenimientoTest y ejecutó los tres filtros
  indicados con BUILD SUCCESSFUL, dos tareas ejecutadas y dos actualizadas.
  Archivo leído completo: ocho casos y helper correctos, sin correcciones
  necesarias. Se verificaron tres XML: historial 8 (19:59:44.136Z), orden 6
  (19:59:44.154Z), cargador 12 (19:59:42.799Z), todos del 8 de noviembre;
  total veintiséis sin fallos, errores u omisiones. No se reejecutaron pruebas.
- Se acredita el alcance unitario de constructor, atributos, validaciones y
  carga en memoria. No se amplía a roles reales, transiciones autorizadas,
  primera creación, escritura atómica, DDL/FK/UNIQUE o persistencia MySQL.
- La base de modelos de soporte ya incluye tipos/estados, orden e historial,
  repositorios, precarga y pruebas, todavía sin servicio técnico o endpoints.
  Se indica guardar este incremento antes de continuar cabecera/detalles y
  sus relaciones con incidencia. No se declara completo el módulo de mantenimiento
  ni el nuevo circuito de alquiler; las dependencias no justifican atajos con
  FK sueltas o tablas duplicadas.
- La rama es codex/refactorizacion-practicas-iii y HEAD sigue b5d0893 antes
  del checkpoint indicado al usuario. El asistente no ejecutó add/commit/push,
  alteró Java, consultó SQL, arrancó/reinició procesos o usó navegador.
- Se actualizaron índice, diario y estado parcial del documento 27; las
  comprobaciones de integración y operaciones siguen pendientes.
