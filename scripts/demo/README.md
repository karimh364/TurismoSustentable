# Demostracion local del parcial

Datos ficticios: no usar estas credenciales en produccion ni para AnyDesk.

## Cuentas

Importar cuentas-demo.sql desde phpMyAdmin, seleccionando turismo_sustentable.
No reemplaza tablas ni altera qa.circuito/qa.tecnico. Si hay conflictos con los
nombres, documentos o permisos propuestos, aborta sin sobrescribir esas cuentas.
Puede repetirse sobre las mismas cuentas creadas por este script sin duplicarlas.
Se genero/verifico el hash con BCryptPasswordEncoder 5.7.10 del proyecto; la
importacion y los logins deben comprobarse por el usuario.

Clave de las cuatro cuentas: `Prueba123*`.
El formulario de inicio de sesion pide **email**, no nickname.

| Nickname | Email para ingresar | Rol exclusivo |
| --- | --- | --- |
| admin.demo | admin.demo@example.com | ADMIN |
| cliente.demo | cliente.demo@example.com | CLIENTE |
| tecnico.demo | tecnico.demo@example.com | TECNICO |
| parcial_playwright | parcial_playwright@example.com | CLIENTE |

El tecnico nuevo vera las ordenes que ADMIN le asigne; no se reasignan historias
anteriores. El saldo ficticio de los clientes se calcula por la configuracion del
simulador: este SQL no agrega una tabla de saldo ni pagos inexistentes.

## Datos y reinicio

### Inventario adicional de Rio Grande

Importar `estaciones-bicicletas-rio-grande.sql` en turismo_sustentable. Agrega
10 estaciones NUEVAS (8 publicas y 2 talleres internos), 20 bicicletas y sus 20
historias iniciales SISTEMA. Importado por el usuario y corroborado por lectura
del asistente: 10/20/20 y ocupaciones correctas. No sobrescribe ni
borra inventario anterior: un nombre/codigo ya existente cancela el lote. No
reimportarlo para restaurar estados despues de usar las bicicletas.

La primera importacion guardo los datos pero phpMyAdmin informo Commands out of
sync al procesar resultados multiples. Las consultas de comprobacion se movieron
fuera del procedimiento. NO repetir la carga si ya existen los 10/20/20 registros.
Si quedo la rutina auxiliar de esa primera importacion, en una consulta nueva se
puede retirar solo ella con `DROP PROCEDURE IF EXISTS turismo_sustentable.demo_rg_inventario_20261116;`.
Eso no borra tablas ni datos. El script no desactiva FOREIGN_KEY_CHECKS.

| Estacion (prefijo RG DEMO) | Uso | Capacidad | Bicis iniciales | Libres |
| --- | --- | ---: | ---: | ---: |
| Centro | Publica | 3 | 3 | 0 |
| Museo | Publica | 5 | 5 | 0 |
| Costanera | Publica | 10 | 3 | 7 |
| Chacra II | Publica | 15 | 2 | 13 |
| Chacra IV | Publica | 3 | 0 | 3 |
| Chacra XIII | Publica | 5 | 2 | 3 |
| Margen Sur | Publica | 10 | 1 | 9 |
| Aeropuerto | Publica | 15 | 0 | 15 |
| Taller Industrial | Interno | 5 | 2 | 3 |
| Taller Margen Sur | Interno | 10 | 2 | 8 |

8 mountain MB-01 a MB-08; 7 paseo PA-01 a PA-07; 5 electricas EL-01 a EL-05.
Las 16 publicas estan DISPONIBLE; las cuatro en talleres estan INACTIVA,
fisicamente presentes y fuera del alquiler publico. No se inventan ordenes o
reservas para justificar otros estados. Fecha de alta: dia UTC de importacion.

El modelo exige capacidad minima 1: no se carga capacidad 0 incompatible con
las validaciones. Se cubren por separado cero bicis y cero espacios libres.
Las estaciones son ficticias y las coordenadas aproximadas para mostrar sectores
de la ciudad, no ubicaciones operativas ni geocodificacion exacta. Referencias:
[barrios del Municipio](https://www.riogrande.gob.ar/ciudad/barrios/),
[direcciones municipales](https://info.riogrande.gob.ar/telefonos_salud) y
[Museo Virginia Choquintel](https://cultura.riogrande.gob.ar/).
Las ubicaciones de las estaciones QA anteriores se conservan: revisar sus
coordenadas si al mostrar todas el mapa abarca otras ciudades.

### Respaldo y arranque

El 16/11 se comprobaron por lectura **29 tablas, todas con registros**. Los casos
manuales existentes aportan alquileres, pagos, reintegros, incidencia, orden y sus
historias. No se inventan otras transacciones para aparentar cobertura del DER.
Despues de importar, volver a exportar la BD en SQL y conservarlo fuera de Git.

Tras reiniciar Windows:

1. Iniciar MariaDB en XAMPP. Apache tambien si se necesita phpMyAdmin.
2. Abrir PowerShell en el proyecto y ejecutar:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\demo\iniciar-demo.ps1
```

3. Esperar Started TurismoSustentableApplication y abrir http://localhost:8080.
4. Mantener abierta la terminal y AnyDesk operativo; verificar el acceso remoto
   y que el equipo no se suspenda antes de salir.

No usar el arranque refactor sin validate: el archivo del perfil sigue con
create-drop. No ejecutar integraciones SQL contra la demostracion en uso.
validate conserva datos y comprueba estructura; no sustituye migraciones de
produccion. No se configura inicio automatico de Windows/MariaDB/AnyDesk.
