package DSII.TurismoSustentable.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: opera únicamente sobre las reservas que crea con la cuenta demo local. */
@EnabledIfEnvironmentVariable(named = "TURISMO_E2E_CLIENTE", matches = "true")
@Execution(ExecutionMode.SAME_THREAD)
class ClienteDemoPlaywrightTest {
    private static final String BASE = "http://localhost:8080";
    private static final String USUARIO = "cliente.demo";
    private static final String ESTACION = "RG DEMO - Chacra II";
    private static final String MONTANA = "MB-05";
    private static final String PASEO = "PA-05";
    private static final String ALQUILERES = "/api/alquileres";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final boolean VISIBLE = Boolean.parseBoolean(System.getenv("TURISMO_E2E_VISIBLE"));
    private static Playwright motor;
    private static Browser navegador;
    private BrowserContext contexto;
    private Page pagina;

    @BeforeAll static void iniciar() {
        assertFalse(System.getenv().getOrDefault("TURISMO_E2E_DEMO_PASSWORD", "").isBlank(),
                "Configurá TURISMO_E2E_DEMO_PASSWORD con la clave demo local.");
        motor = Playwright.create();
        navegador = motor.chromium().launch(new BrowserType.LaunchOptions().setHeadless(!VISIBLE)
                .setSlowMo(VISIBLE ? 1500 : 0));
    }

    @AfterAll static void cerrar() {
        try { if (navegador != null) navegador.close(); }
        finally { if (motor != null) motor.close(); }
    }

    @BeforeEach void prepararContexto() {
        contexto = navegador.newContext(new Browser.NewContextOptions().setLocale("es-AR")
                .setTimezoneId("America/Argentina/Ushuaia").setViewportSize(1440, 950));
        contexto.setDefaultTimeout(15_000);
        contexto.setDefaultNavigationTimeout(25_000);
        pagina = contexto.newPage();
    }

    @AfterEach void cerrarContexto() { if (contexto != null) contexto.close(); }

    @Test void visitanteYClienteRecorrenMapaNavegacionPermisosYSalida() throws IOException {
        ir("/mis_alquileres.html");
        assertThat(pagina).hasURL(Pattern.compile(".*index\\.html\\?sesion=requerida.*"));
        estado(get(ALQUILERES), 401);

        for (Locator pregunta : pagina.locator("#accordionFAQ button").all()) {
            pregunta.click();
            assertThat(pregunta).hasAttribute("aria-expanded", "true");
        }
        popupEstacion();
        pagina.locator(".leaflet-popup-content button").filter(new Locator.FilterOptions().setHasText("Ver Bicicletas")).click();
        titulo("Iniciá sesión");
        boton(modal(), "Ir al ingreso").click();

        iniciarSesion(true);
        assertThat(pagina).hasURL(BASE + "/alquiler.html");
        JsonNode estacionBuscada = estacionDemo();
        int estacion = estacionBuscada.path("id").asInt();
        Locator buscarEstacion = pagina.locator("#buscar-estacion");
        Locator opcionesEstacion = pagina.locator("#estaciones-container button[data-estacion-id]");
        Locator opcionEstacion = pagina.locator("#estaciones-container button[data-estacion-id='" + estacion + "']");
        assertThat(opcionEstacion).isVisible();
        assertThat(pagina.locator("#bicicletas-container article")).hasCount(0);
        buscarEstacion.fill("chacra ii");
        assertThat(opcionEstacion).isVisible();
        buscarEstacion.fill("e2e-direccion-inexistente");
        assertThat(opcionesEstacion).hasCount(0);
        buscarEstacion.fill(estacionBuscada.path("direccion").asText());
        assertThat(opcionEstacion).isVisible();
        opcionEstacion.click();
        assertThat(pagina.locator("#estacion-elegida")).containsText(ESTACION);

        for (String restringida : List.of("/admin.html", "/mantenimiento.html")) {
            ir(restringida);
            assertThat(pagina).hasURL(Pattern.compile(".*index\\.html\\?acceso=denegado.*"));
            cerrarAviso("Acceso no permitido");
            perfilCliente();
        }
        for (String ruta : List.of("/api/admin/auditoria", "/api/admin/auditoria/administradores",
                "/api/admin/estadisticas/alquileres-todos", "/api/admin/estaciones", "/api/usuarios",
                "/api/mantenimiento/ordenes")) estado(get(ruta), 403);

        ir("/index.html");
        assertThat(pagina.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Panel Admin"))).hasCount(0);
        popupEstacion();
        pagina.locator(".leaflet-popup-content button").filter(new Locator.FilterOptions().setHasText("Ver Bicicletas")).click();
        assertThat(pagina).hasURL(BASE + "/alquiler.html?estacionId=" + estacion);
        assertThat(pagina.locator("#estacion-elegida")).containsText(ESTACION);
        assertThat(opcionEstacion).hasAttribute("aria-pressed", "true");
        assertThat(tarjeta(PASEO)).isVisible();
        assertThat(tarjeta(MONTANA)).isVisible();
        int disponiblesEnEstacion = 0;
        for (JsonNode bici : datos(get("/api/bicicletas/disponibles"), 200)) {
            if (bici.path("estacionId").asInt() == estacion) disponiblesEnEstacion++;
        }
        assertThat(pagina.locator("#bicicletas-container article")).hasCount(disponiblesEnEstacion);
        pagina.locator("#filtro-tipo").selectOption("PASEO");
        assertThat(tarjeta(PASEO)).isVisible();
        assertThat(tarjeta(MONTANA)).hasCount(0);
        assertThat(tarjeta(PASEO)).containsText("US$");
        pagina.locator("#switch-moneda").check();
        assertThat(pagina.locator("#label-moneda")).hasText("AR$");
        pagina.locator("#switch-moneda").uncheck();
        assertThat(pagina.locator("#crear-reserva")).isDisabled();
        seleccionar(PASEO);
        pagina.locator("#duracion-reserva").fill("37");
        pagina.locator("#crear-reserva").click();
        titulo("Confirmar reserva");
        assertThat(modal()).containsText(ESTACION);
        assertThat(modal()).containsText(PASEO);
        assertThat(modal()).containsText("37 minutos");
        assertThat(modal()).containsText("US$");
        boton(modal(), "Volver a editar").click();
        assertThat(pagina.locator("#seleccion-reserva")).containsText(PASEO);
        assertThat(pagina.locator("#duracion-reserva")).hasValue("37");

        ir("/mis_alquileres.html");
        boton(pagina.locator("body"), "Actualizar viajes y saldos").click();
        saldoEnPantalla();
        assertThat(pagina.locator("a[href='incidencias.html']")).isVisible();
        pagina.locator("a[href='incidencias.html']").click();
        assertThat(pagina).hasURL(BASE + "/incidencias.html");
        pagina.locator("#inc-filtro").selectOption("PENDIENTE");
        pagina.locator("#inc-actualizar").click();
        assertThat(pagina.locator("#inc-estado")).containsText("Incidencias actualizadas");
        assertThat(pagina.locator("#inc-nav-admin")).isHidden();
        assertThat(pagina.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Revisar solicitud"))).hasCount(0);

        Response salida = esperar("POST", "/api/logout", () -> pagina.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(Pattern.compile("Cerrar sesión", Pattern.CASE_INSENSITIVE))).click());
        assertEquals(200, salida.status());
        assertThat(pagina).hasURL(BASE + "/index.html");
        estado(get(ALQUILERES), 401);
        ir("/alquiler.html");
        assertThat(pagina).hasURL(Pattern.compile(".*index\\.html\\?sesion=requerida.*"));
    }

    @Test void cancelaUnaBicicletaYLuegoLasPendientes() throws IOException {
        iniciarSesion(false);
        try (Reserva reserva = new Reserva()) {
            reserva.crear(10, MONTANA, PASEO);
            historialEnPantalla(reserva, "Reservado");
            Locator primera = unidad(reserva, MONTANA);
            assertThat(boton(primera, "Cancelar por operación/seguridad")).hasCount(0);
            boton(primera, "Retirar").click();
            assertEquals(409, confirmar(reserva.detalle(MONTANA, "retirar")).status(),
                    "Sin anticipo aprobado, el retiro no debe comenzar.");
            cerrarAviso("No se pudo completar");

            // Es la reserva propia y recién creada. Las rutas administrativas deben denegar antes de operar.
            estado(contexto.request().post(BASE + "/api/admin/alquileres/" + reserva.id + "/detalles/"
                    + reserva.unidadId(MONTANA) + "/cancelar"), 403);
            estado(contexto.request().delete(BASE + reserva.ruta("")), 403);
            assertEquals("RESERVADO", reserva.consultar().path("estado").asText());

            boton(unidad(reserva, MONTANA), "Cancelar esta bicicleta").click();
            datos(confirmar(reserva.detalle(MONTANA, "cancelar")), 200);
            cerrarAviso("Reserva actualizada");
            JsonNode parcial = reserva.consultar();
            assertEquals("RESERVADO", parcial.path("estado").asText());
            assertEquals("CANCELADO", detalle(parcial, MONTANA).path("estado").asText());
            assertEquals("RESERVADO", detalle(parcial, PASEO).path("estado").asText());

            boton(fila(reserva), "Cancelar pendientes").click();
            datos(confirmar(reserva.ruta("/cancelar")), 200);
            cerrarAviso("Reserva actualizada");
            assertEquals("CANCELADO", reserva.consultar().path("estado").asText());
            historialEnPantalla(reserva, "Reservado", "Cancelado");
        }
        try (Reserva reserva = new Reserva()) {
            reserva.crear(10, MONTANA, PASEO);
            boton(fila(reserva), "Cancelar pendientes").click();
            datos(confirmar(reserva.ruta("/cancelar")), 200);
            cerrarAviso("Reserva actualizada");
            JsonNode cabecera = reserva.consultar();
            assertEquals("CANCELADO", cabecera.path("estado").asText());
            for (JsonNode d : cabecera.path("detalles")) assertEquals("CANCELADO", d.path("estado").asText());
        }
    }

    @Test void retiraDosDevuelveATiempoYResuelveElReintegro() throws IOException {
        iniciarSesion(false);
        try (Reserva reserva = new Reserva()) {
            reserva.crear(10, MONTANA, PASEO);
            pagarAnticipo(reserva);
            retirar(reserva, MONTANA);
            retirar(reserva, PASEO);
            // Abandonar el formulario evita dejar una revisión administrativa abierta en la cuenta demo.
            unidad(reserva, MONTANA).getByRole(AriaRole.LINK,
                    new Locator.GetByRoleOptions().setName("Reportar incidencia")).click();
            titulo("Reportar " + MONTANA);
            assertThat(modal().locator("#inc-informante")).hasCount(0);
            boton(modal(), "Enviar reporte").click();
            assertThat(modal()).containsText("Completá la descripción");
            modal().locator("#inc-descripcion").fill("Ensayo E2E: no enviar este borrador.");
            boton(modal(), "Volver").click();
            ir("/mis_alquileres.html");
            assertThat(unidad(reserva, MONTANA)).containsText("Devolución estimada");
            pagina.locator("#switch-moneda").check();
            assertThat(pagina.locator("#label-moneda")).hasText("AR$");
            assertThat(unidad(reserva, MONTANA)).isVisible();
            pagina.locator("#switch-moneda").uncheck();

            comprobarATiempo(devolver(reserva, MONTANA));
            assertEquals("EN_PROGRESO", reserva.consultar().path("estado").asText());
            comprobarATiempo(devolver(reserva, PASEO));
            assertEquals("FINALIZADO", reserva.consultar().path("estado").asText());
            historialEnPantalla(reserva, "Reservado", "En progreso", "Finalizado");

            JsonNode liquida = abrirLiquidacion(reserva);
            assertEquals("REEMBOLSO_PENDIENTE", liquida.path("situacionFinanciera").asText());
            assertTrue(liquida.path("reembolsoPendienteArs").decimalValue().signum() > 0);
            JsonNode resuelto = datos(esperar("POST", reserva.reembolso() + "/resolver",
                    () -> boton(modal(), "Procesar/consultar reintegro").click()), 200);
            cerrarAviso("Reembolso simulado");
            assertEquals("SALDADO", resuelto.path("liquidacion").path("situacionFinanciera").asText());
            assertEquals("SALDADO", abrirLiquidacion(reserva).path("situacionFinanciera").asText());
            cerrarAviso("Liquidación #" + reserva.id);
        }
    }

    @Test void devuelveDespuesDeLaEstimacionYPagaLaDeudaSimulada() throws Exception {
        iniciarSesion(false);
        try (Reserva reserva = new Reserva()) {
            reserva.crear(1, PASEO);
            pagarAnticipo(reserva);
            JsonNode enUso = retirar(reserva, PASEO);
            Instant estimada = instante(enUso, "fechaHoraDevolucionEstimada");
            long espera = Duration.between(Instant.now(), estimada.plusSeconds(12)).toMillis();
            assertTrue(espera < 95_000, "Relojes local y servidor incompatibles para la prueba.");
            System.out.println("E2E: esperando la devolución estimada real del alquiler #" + reserva.id);
            if (espera > 0) Thread.sleep(espera);
            JsonNode devuelta = devolver(reserva, PASEO);
            assertTrue(instante(devuelta, "fechaHoraDevolucionReal").isAfter(estimada));
            assertEquals("FINALIZADO", reserva.consultar().path("estado").asText());
            JsonNode liquidacion = abrirLiquidacion(reserva);
            assertEquals("SALDO_PENDIENTE", liquidacion.path("situacionFinanciera").asText());
            assertTrue(liquidacion.path("saldoPendienteArs").decimalValue().signum() > 0);
            boton(modal(), "Pagar saldo simulado").click();
            JsonNode pago = datos(confirmar("/api/pagos/adicional/" + reserva.id + "/resolver"), 200);
            assertEquals("APROBADO", pago.path("estadoPago").asText());
            cerrarAviso("Resultado del pago simulado");
            assertEquals("SALDADO", abrirLiquidacion(reserva).path("situacionFinanciera").asText());
            cerrarAviso("Liquidación #" + reserva.id);
        }
    }

    private void iniciarSesion(boolean probarRechazo) throws IOException {
        if (!pagina.url().startsWith(BASE + "/index.html")) ir("/index.html");
        pagina.locator("#login-nickname").fill(USUARIO);
        if (probarRechazo) {
            pagina.locator("#login-password").fill("e2e-clave-incorrecta-" + UUID.randomUUID());
            assertEquals(401, esperar("POST", "/api/login", () -> pagina.locator("#login-form button[type=submit]").click()).status());
            assertThat(pagina.getByText("Nombre de usuario o contraseña incorrectos.")).isVisible();
        }
        pagina.locator("#login-password").fill(System.getenv("TURISMO_E2E_DEMO_PASSWORD"));
        assertEquals(200, esperar("POST", "/api/login", () -> pagina.locator("#login-form button[type=submit]").click()).status(),
                "No se pudo iniciar sesión con la cuenta demo.");
        assertThat(pagina).hasURL(BASE + "/alquiler.html");
        perfilCliente();
    }

    private JsonNode perfilCliente() throws IOException {
        JsonNode perfil = datos(get("/api/usuarios/actual"), 200);
        assertEquals(USUARIO, perfil.path("nickname").asText());
        assertTrue(perfil.path("activo").asBoolean());
        assertEquals(JSON.valueToTree(List.of("CLIENTE")), perfil.path("roles"), "La prueba exige un cliente sin otros roles.");
        return perfil;
    }

    private JsonNode estacionDemo() throws IOException {
        for (JsonNode e : datos(get("/api/estaciones"), 200)) {
            if (ESTACION.equals(e.path("nombre").asText())) {
                assertTrue(e.path("id").asInt() > 0);
                return e;
            }
        }
        throw new AssertionError("Falta la estación pública demo " + ESTACION);
    }

    private void popupEstacion() {
        pagina.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Mapa").setExact(true)).click();
        Locator marcadores = pagina.locator("#map .leaflet-marker-icon");
        assertThat(marcadores.nth(4)).isVisible();
        boolean turismo = false;
        for (Locator marcador : marcadores.all()) {
            marcador.press("Enter");
            Locator popup = pagina.locator("#map .leaflet-popup-content");
            assertThat(popup).isVisible();
            String texto = popup.innerText();
            turismo |= texto.contains("Museo Virginia Choquintel") || texto.contains("Monumento Malvinas")
                    || texto.contains("Reserva Punta Popper") || texto.contains("Plaza Almirante Brown");
            if (texto.contains(ESTACION)) {
                assertThat(popup).containsText("Disponibles:");
                assertThat(popup).containsText("Espacios libres:");
                assertTrue(turismo, "El mapa también debe mostrar un punto turístico.");
                return;
            }
            pagina.locator("#map .leaflet-popup-close-button").click();
            assertThat(popup).hasCount(0); // Leaflet retiene el nodo durante la salida animada.
        }
        fail("No se encontró el marcador demo de " + ESTACION);
    }

    private void saldoEnPantalla() throws IOException {
        JsonNode saldo = datos(esperar("GET", "/api/pagos/saldo-simulado",
                () -> pagina.locator("#consultar-saldo-simulado").click()), 200);
        assertEquals("SIMULADA", saldo.path("plataforma").asText());
        assertEquals("PRUEBA", saldo.path("entorno").asText());
        assertThat(modal()).containsText("Saldo disponible:");
        cerrarAviso("Cuenta simulada (sin dinero real)");
    }

    private void pagarAnticipo(Reserva reserva) throws IOException {
        boton(fila(reserva), "Pagar/consultar anticipo simulado").click();
        assertThat(modal()).containsText("Pago SIMULADO");
        JsonNode pago = datos(confirmar("/api/pagos/anticipo/" + reserva.id + "/resolver"), 200);
        assertEquals("APROBADO", pago.path("estadoPago").asText());
        cerrarAviso("Resultado del pago simulado");
    }

    private JsonNode retirar(Reserva reserva, String codigo) throws IOException {
        boton(unidad(reserva, codigo), "Retirar").click();
        JsonNode d = datos(confirmar(reserva.detalle(codigo, "retirar")), 200);
        cerrarAviso("Operación registrada");
        assertEquals("EN_CURSO", d.path("estado").asText());
        assertTrue(instante(d, "fechaHoraDevolucionEstimada").isAfter(instante(d, "fechaHoraRetiro")));
        return d;
    }

    private JsonNode devolver(Reserva reserva, String codigo) throws IOException {
        boton(unidad(reserva, codigo), "Devolver").click();
        titulo("Devolver bicicleta");
        Locator selector = modal().locator("select");
        assertFalse(selector.locator("option").allTextContents().stream().anyMatch(s -> s.contains("Taller")),
                "La devolución de cliente no debe ofrecer talleres de la demo.");
        selector.selectOption(Integer.toString(reserva.estacion));
        modal().locator(".swal2-confirm").click();
        titulo("¿La bicicleta tiene una falla?");
        JsonNode d = datos(esperar("POST", reserva.detalle(codigo, "devolver"),
                () -> boton(modal(), "No").click()), 200);
        cerrarAviso("Bicicleta devuelta");
        assertEquals("DEVUELTO", d.path("estado").asText());
        assertEquals(reserva.estacion, d.path("estacionDevolucionId").asInt());
        return d;
    }

    private void historialEnPantalla(Reserva reserva, String... estados) {
        boton(fila(reserva), "Historial de estados").click();
        titulo("Historial del alquiler #" + reserva.id);
        assertThat(modal()).containsText("Fechas en hora de Río Grande");
        Locator eventos = modal().locator("ol li");
        assertThat(eventos).hasCount(estados.length);
        for (int i = 0; i < estados.length; i++) {
            assertThat(eventos.nth(i).locator("strong")).hasText(Pattern.compile(".*→\\s*" + estados[i]));
        }
        cerrarAviso("Historial del alquiler #" + reserva.id);
    }

    private JsonNode abrirLiquidacion(Reserva reserva) throws IOException {
        JsonNode resultado = datos(esperar("GET", reserva.reembolso(),
                () -> boton(fila(reserva), "Liquidación y reembolsos").click()), 200).path("liquidacion");
        titulo("Liquidación #" + reserva.id);
        assertThat(modal()).containsText(resultado.path("situacionFinanciera").asText());
        return resultado;
    }

    private Locator fila(Reserva r) {
        return pagina.locator("#container-activos article, #tbody-historial tr").filter(new Locator.FilterOptions()
                .setHas(pagina.locator("details[data-alquiler-detalles='" + r.id + "']")));
    }

    private Locator unidad(Reserva r, String codigo) {
        Locator detalles = fila(r).locator("details");
        assertThat(detalles.locator("summary")).isVisible();
        if (detalles.getAttribute("open") == null) detalles.locator("summary").click();
        return detalles.locator("li").filter(new Locator.FilterOptions().setHasText(codigo));
    }

    private Locator tarjeta(String codigo) {
        return pagina.locator("#bicicletas-container article").filter(new Locator.FilterOptions()
                .setHas(pagina.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(codigo).setExact(true))));
    }

    private void seleccionar(String codigo) { tarjeta(codigo).locator("input[type=checkbox]").check(); }
    private void ir(String ruta) { pagina.navigate(BASE + ruta, new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)); }
    private APIResponse get(String ruta) { return contexto.request().get(BASE + ruta); }
    private Locator modal() { return pagina.locator(".swal2-popup:visible"); }
    private void titulo(String titulo) { assertThat(modal().locator(".swal2-title")).hasText(titulo); }
    private void cerrarAviso(String titulo) {
        titulo(titulo);
        modal().locator(".swal2-confirm").click();
        assertThat(modal()).isHidden();
    }
    private static Locator boton(Locator zona, String titulo) {
        return zona.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(titulo).setExact(true));
    }
    private Response esperar(String metodo, String ruta, Runnable accion) {
        return pagina.waitForResponse(r -> r.url().equals(BASE + ruta) && r.request().method().equals(metodo), accion);
    }
    private Response confirmar(String ruta) {
        titulo("Confirmar operación");
        return esperar("POST", ruta, () -> boton(modal(), "Confirmar").click());
    }
    private static JsonNode datos(Response respuesta, int estado) throws IOException {
        assertEquals(estado, respuesta.status(), respuesta::url);
        return JSON.readTree(respuesta.body());
    }
    private static JsonNode datos(APIResponse respuesta, int estado) throws IOException {
        try {
            assertEquals(estado, respuesta.status(), respuesta::url);
            return JSON.readTree(respuesta.body());
        } finally { respuesta.dispose(); }
    }
    private static void estado(APIResponse respuesta, int esperado) {
        try { assertEquals(esperado, respuesta.status(), respuesta::url); }
        finally { respuesta.dispose(); }
    }
    private static Instant instante(JsonNode dato, String propiedad) {
        return OffsetDateTime.parse(dato.path(propiedad).asText()).toInstant();
    }
    private static void comprobarATiempo(JsonNode d) {
        assertFalse(instante(d, "fechaHoraDevolucionReal").isAfter(instante(d, "fechaHoraDevolucionEstimada")));
    }
    private static JsonNode detalle(JsonNode cabecera, String codigo) {
        for (JsonNode d : cabecera.path("detalles")) if (codigo.equals(d.path("codigoBicicleta").asText())) return d;
        throw new AssertionError("Falta el detalle de " + codigo);
    }

    private final class Reserva implements AutoCloseable {
        private int id;
        private int estacion;
        private JsonNode datosReserva;

        void crear(int minutos, String... codigos) throws IOException {
            JsonNode saldo = datos(get("/api/pagos/saldo-simulado"), 200);
            assertEquals("SIMULADA", saldo.path("plataforma").asText());
            assertEquals("PRUEBA", saldo.path("entorno").asText());
            assertTrue(saldo.path("saldoDisponibleArs").decimalValue().signum() > 0, "Sin saldo simulado demo.");
            estacion = estacionDemo().path("id").asInt();
            ir("/alquiler.html");
            pagina.locator("#estaciones-container button[data-estacion-id='" + estacion + "']").click();
            for (String codigo : codigos) seleccionar(codigo);
            pagina.locator("#duracion-reserva").fill(Integer.toString(minutos));
            pagina.locator("#crear-reserva").click();
            titulo("Confirmar reserva");
            datosReserva = datos(esperar("POST", ALQUILERES, () -> boton(modal(), "Confirmar reserva").click()), 201);
            id = datosReserva.path("id").asInt();
            assertTrue(id > 0);
            System.out.println("E2E cliente.demo: alquiler #" + id);
            assertEquals(perfilCliente().path("id"), datosReserva.path("usuarioId"));
            assertEquals(estacion, datosReserva.path("estacionRetiroId").asInt());
            assertEquals(codigos.length, datosReserva.path("detalles").size());
            cerrarAviso("Reserva creada");
            assertThat(pagina).hasURL(BASE + "/mis_alquileres.html");
        }

        String ruta(String sufijo) { return ALQUILERES + "/" + id + sufijo; }
        String reembolso() { return "/api/pagos/reembolsos/" + id; }
        int unidadId(String codigo) { return ClienteDemoPlaywrightTest.detalle(datosReserva, codigo).path("id").asInt(); }
        String detalle(String codigo, String accion) { return ruta("/detalles/" + unidadId(codigo) + "/" + accion); }
        JsonNode consultar() throws IOException { return datos(get(ruta("")), 200); }

        @Override public void close() throws IOException {
            if (id == 0) return;
            // Red de seguridad: no borra datos ni interviene en alquileres ajenos o previos.
            APIRequestContext api = contexto.request();
            boolean pendientes = false;
            for (JsonNode d : consultar().path("detalles")) {
                String estado = d.path("estado").asText();
                if ("EN_CURSO".equals(estado)) {
                    datos(api.post(BASE + ruta("/detalles/" + d.path("id").asInt() + "/devolver"), RequestOptions.create()
                            .setData(Map.of("estacionDevolucionId", estacion, "fallaReportada", false))), 200);
                }
                pendientes |= List.of("RESERVADO", "PENDIENTE_RETIRO").contains(estado);
            }
            if (pendientes) datos(api.post(BASE + ruta("/cancelar")), 200);
            JsonNode fin = datos(get(reembolso()), 200).path("liquidacion");
            if ("REEMBOLSO_PENDIENTE".equals(fin.path("situacionFinanciera").asText())) {
                datos(api.post(BASE + reembolso() + "/resolver"), 200);
            } else if (fin.path("saldoPendienteArs").decimalValue().signum() > 0) {
                System.out.println("E2E: quedó saldo simulado pendiente en el alquiler #" + id + ". Revisalo en Mis viajes.");
            }
        }
    }
}
