# Pruebas reales de registro y recuperación

Fecha: 5 de noviembre de 2026.

Resultado: **52 comprobaciones correctas contra HTTP y MySQL**, más la
verificación del navegador integrado descrita abajo. No son peticiones
simuladas ni se suman a las 108 pruebas JUnit/Mockito de la etapa anterior.

## Entorno y preparación

- Aplicación: http://localhost:8080, Spring Boot 2.7.14, JDK 11.0.19.
- Perfil: refactor. Base local MySQL: turismo_sustentable.
- Se detectó una instancia anterior que respondía 200 ante email=hola en
  /api/password/solicitar. Antes de reiniciar se verificó que usuario,
  persona, bicicleta, estacion, alquiler y tarjeta tenían cero filas.
- Se detuvo exclusivamente el proceso de esta aplicación, comprobando su
  clase, perfil y URL local; se inició la versión compilada actual. El
  perfil conserva create-drop: se recrearon tablas y catálogos al arrancar.
  No se modificaron propiedades, fuentes Java, credenciales, Git o Render.
- El entorno de arranque no tiene las tres variables necesarias de Mailjet.
  El log confirmó «Correo no enviado: Mailjet todavía no está configurado».
  No se enviaron mensajes externos; la entrega real queda fuera de esta
  evidencia y no se presenta como comprobada.

## Comprobaciones HTTP y MySQL

| Grupo | Casos | Resultado |
| --- | ---: | --- |
| Catálogos públicos de género, documentos y 249 países | 1 | Correcto |
| Registro vacío y valores inválidos de los campos, incluidos tipo/país documental y contraseña | 29 | HTTP 400, mensajes esperados |
| Solicitud de recuperación: vacío, nulo, formato inválido, dominio incompleto, puntos inválidos y exceso de longitud | 6 | HTTP 400, mensajes esperados |
| Ausencia de altas parciales después de los rechazos | 1 | Cero personas y usuarios antes de las altas válidas |
| Altas válidas: DNI argentino normalizado y pasaporte extranjero en mayúsculas | 2 | HTTP 201 y rol CLIENTE |
| Duplicados de email, nickname y documento escrito con puntos | 3 | HTTP 409; no altas adicionales |
| Relaciones persistidas | 1 | Dos PERSONA, dos USUARIO y dos USUARIO_ROL |
| Login por nickname y logout | 2 | Perfil accesible con sesión; 401 después de cerrarla |
| Inicio de recuperación para correo existente/desconocido | 1 | Mismo HTTP 200/texto; hash y vencimiento solo para la cuenta existente |
| Restablecimiento, token inválido, clave débil, consumo/reuso, credenciales y vencimiento | 6 | Comportamientos esperados |
| **Total** | **52** | **Sin fallos** |

Entre los casos inválidos de registro se comprobaron nombre con números,
apellido con símbolos, longitud de nombre, nickname que comienza por número
o fuera de 3-30, email sin dominio completo/con dos arrobas/con inicio numérico/
demasiado largo, nacimiento futuro, género ausente/no positivo/inexistente,
tipo o país inexistente, documento vacío, DNI de nueve dígitos/con letras/
país distinto de AR, pasaporte con guion y contraseñas sin minúscula, mayúscula
o símbolo, con espacios, fuera de 6-64 o superiores al límite interno del hash.

## Restablecimiento sin proveedor de correo

Se solicitó primero la recuperación mediante el endpoint real y se verificó
en MySQL que se creó un hash de 64 caracteres con vencimiento. Como Mailjet
no está configurado, el token original del enlace no está disponible para
recibirlo en una casilla: no se intentó descifrar su hash.

Para probar el resto del flujo se usó una fixture controlada únicamente en
la cuenta qa.registro: token aleatorio conocido, hash SHA-256 correspondiente
y el vencimiento generado por la solicitud. Se actualizó también la versión
de esa fila. El endpoint real cambió la contraseña, dejó hash BCrypt de 60
caracteres y anuló token/vencimiento. El reuso devolvió 400; el login con la
clave anterior devolvió 401 y con la nueva, 200.

Para el caso vencido se preparó otra fixture en esa misma cuenta con fecha
pasada. El endpoint real respondió que el enlace venció, limpió sus datos y
conservó la contraseña vigente. No se tocaron otras cuentas ni se modificó
el código del generador, encoder, controlador o servicio para estas pruebas.

Esto demuestra la integración HTTP/MySQL del restablecimiento con fixtures,
no la recepción del mensaje de Mailjet ni un circuito completo usando un
enlace recibido en una casilla real. Concurrencia, carga y auditoría general
de seguridad tampoco se dan por verificadas con estos casos.

## Verificación en navegador

Con las páginas reales y sin reemplazar Axios o el backend se comprobó:

- Registro vacío: diez mensajes legibles en el resumen, sin nombres internos.
- Nombre inválido: mensaje específico del backend.
- Email duplicado: conflicto del backend visible en el mismo listado.
- DNI: el noveno dígito no queda en el campo.
- Pasaporte: ab123 se convierte realmente en AB123 y permite elegir Chile.
- Recuperación vacía y con maurio@sd: avisos específicos del formulario.
- Recuperación con a..b@example.com: el navegador envía, el backend rechaza
  y SweetAlert muestra «Ingresá una dirección de correo válida», sin email:.
- Correo válido desconocido: respuesta genérica y formulario limpiado.
- Login con qa.registro y su contraseña nueva: redirección a alquiler.html
  y saludo «Bienvenido/a, José María».

Evidencias:

- [Error de validación recibido del backend](recuperacion-error-backend.png).
- [Respuesta genérica de recuperación](recuperacion-respuesta-generica.png).

## Datos de prueba que quedan

| Cuenta | Documento | Rol | Estado final |
| --- | --- | --- | --- |
| qa.registro | DNI argentino ficticio, con ceros iniciales | CLIENTE | Activa; contraseña cambiada y probada; sin token pendiente |
| qa.pasaporte | Pasaporte ficticio emitido por Chile | CLIENTE | Activa; sin token pendiente |

Los emails son qa.registro@example.com y qa.pasaporte@example.com, utilizados
solo como fixtures locales. No se agregaron usuarios al main ni a los
cargadores. La aplicación se deja ejecutándose y el navegador quedó en la
cuenta QA. Las cuentas no se borraron: siguen disponibles hasta que el usuario
las gestione o el siguiente arranque create-drop recree la base descartable.
El programa temporal de build/tmp se eliminó al terminar.

## Conclusión acotada

Las validaciones y la conexión entre frontend, backend y persistencia de este
bloque quedan comprobadas en local. No se encontraron fallos en los casos
ejecutados. La entrega real del correo requiere configurar Mailjet y comprobar
una casilla de prueba; no queda certificada por estas pruebas locales.
