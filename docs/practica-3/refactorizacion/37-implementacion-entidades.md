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
