# Prueba de extremo a extremo del cliente demo

`ClienteDemoPlaywrightTest` usa Chromium y la aplicación real en `localhost:8080`. No levanta otra base. La cuenta `cliente.demo` debe estar activa y tener solamente `CLIENTE`. Tiene que haber saldo simulado en el entorno `PRUEBA`, una cotización válida y las bicicletas `MB-05` y `PA-05` disponibles en `RG DEMO - Chacra II`.

### Ejecutar en PowerShell

1. Levantar MariaDB y la app con el perfil `refactor` y `ddl-auto=validate`. Dejar la aplicación corriendo en otra terminal.
2. Correr lo siguiente desde la carpeta del proyecto. No correr al mismo tiempo otra prueba que use esas bicicletas ni activar la suite MySQL de integración contra la base de la demo.

```powershell
$env:TURISMO_E2E_CLIENTE = 'true'
$env:TURISMO_E2E_DEMO_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Clave de cliente.demo' -AsSecureString)).Password
# Descomentá para abrir Chromium y ver las acciones con una pausa de 1,5 segundos.
# $env:TURISMO_E2E_VISIBLE = 'true'
try {
    .\gradlew.bat test --rerun --tests 'ClienteDemoPlaywrightTest'
} finally {
    Remove-Item Env:\TURISMO_E2E_CLIENTE, Env:\TURISMO_E2E_DEMO_PASSWORD, Env:\TURISMO_E2E_VISIBLE -ErrorAction SilentlyContinue
}
```

### Qué comprueba

- Visitante y cliente: mapa de estaciones y puntos turísticos, preguntas frecuentes, inicio inválido/válido, sesión y cierre, filtros de inventario, cambio de moneda, confirmación de reserva y accesos permitidos o denegados.
- Reserva: rechazo del retiro sin anticipo, cancelación de una unidad, cancelación de todas las pendientes y cancelación completa en una reserva separada. Verifica que un cliente no pueda cancelar en modo administrativo ni ocultar un alquiler.
- Viaje y reintegro: confirma el anticipo simulado, retira y devuelve dos unidades antes de su estimación, consulta el historial del conjunto y procesa el reintegro hasta `SALDADO`. Valida y abandona un borrador de incidencia para no dejar una revisión pendiente.
- Viaje y deuda: reserva una unidad con duración estimada de un minuto, espera a superar la fecha del servidor, devuelve, paga el adicional simulado y verifica `SALDADO`. No cambia relojes ni fechas en la base.

Son pruebas con datos demo persistentes: quedan los alquileres, pagos e historiales de prueba. Si un caso falla, se intenta devolver o cancelar únicamente sus propias unidades y resolver su reintegro. Si queda una deuda pendiente, el test informa el ID para revisarlo en Mis viajes. La suite no modifica usuarios, roles, otros alquileres ni el esquema. No cubre todos los estados alternativos del proveedor, el envío de correo ni la creación de cuentas o la resolución administrativa de incidencias.
