package DSII.TurismoSustentable.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/** Prueba opt-in contra la app refactor levantada. Usa únicamente stock y cuentas demo. */
@EnabledIfEnvironmentVariable(named = "TURISMO_E2E_HISTORIAL", matches = "true")
class HistorialAlquilerPlaywrightTest {
    private static final String BASE = "http://localhost:8080";
    private static final String ESTACION = "RG DEMO - Chacra II";
    private static final String MONTANA = "MB-05";
    private static final String PASEO = "PA-05";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final boolean MODO_VISIBLE = Boolean.parseBoolean(System.getenv("TURISMO_E2E_VISIBLE"));
    private static Playwright playwright;
    private static Browser navegador;

    @BeforeAll static void abrirNavegador() {
        assertFalse(System.getenv().getOrDefault("TURISMO_E2E_DEMO_PASSWORD", "").isBlank(),
                "Configurá TURISMO_E2E_DEMO_PASSWORD con la clave de las cuentas demo locales.");
        playwright = Playwright.create();
        navegador = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(!MODO_VISIBLE)
                .setSlowMo(MODO_VISIBLE ? 1500 : 0));
    }

    @AfterAll static void cerrarNavegador() {
        try { if (navegador != null) navegador.close(); }
        finally { if (playwright != null) playwright.close(); }
    }

    @Test void recorreRetiroYDevolucionParcialesConPermisosReales() throws IOException {
        try (Sesion cliente = entrar("cliente.demo", "CLIENTE", "/alquiler.html");
             Sesion admin = entrar("admin.demo", "ADMIN", "/admin.html");
             Sesion otro = entrar("parcial_playwright", "CLIENTE", "/alquiler.html");
             Sesion tecnico = entrar("tecnico.demo", "TECNICO", "/mantenimiento.html");
             BrowserContext visitante = nuevoContexto();
             ReservaDemo alquiler = new ReservaDemo(cliente)) {
            if (MODO_VISIBLE) cliente.pagina().bringToFront();
            JsonNode reserva = alquiler.crear(MONTANA, PASEO);
            int primero = detalle(reserva, MONTANA).path("id").asInt();
            int segundo = detalle(reserva, PASEO).path("id").asInt();
            String primerRetiro = alquiler.ruta("/detalles/" + primero + "/retirar");

            JsonNode inicial = alquiler.historialPropio("RESERVADO");
            comprobarActor(inicial.get(0), cliente);
            assertEquals(fecha(reserva, "fechaHoraCreacion"), fecha(inicial.get(0), "fechaHoraCambio"));
            comprobarDialogo(cliente.pagina(), filaCliente(cliente, alquiler.id), alquiler.id, inicial);

            assertEquals(401, visitante.request().get(alquiler.ruta("/historial")).status());
            assertEquals(403, tecnico.contexto().request().get(alquiler.ruta("/historial")).status());
            assertEquals(403, otro.contexto().request().get(alquiler.ruta("/historial")).status());
            assertEquals(inicial, leer(admin.contexto().request().get(alquiler.ruta("/historial")), 200));

            boton(filaCliente(cliente, alquiler.id), "Pagar/consultar anticipo simulado").click();
            assertThat(modal(cliente.pagina()).locator(".swal2-html-container")).containsText("Pago SIMULADO");
            JsonNode pago = leer(esperar(cliente.pagina(), "POST", BASE + "/api/pagos/anticipo/" + alquiler.id + "/resolver",
                    () -> modal(cliente.pagina()).locator(".swal2-confirm").click()), 200);
            assertEquals("APROBADO", pago.path("estadoPago").asText());
            cerrarAviso(cliente.pagina(), "Resultado del pago simulado");
            assertEquals(inicial, alquiler.historialPropio("RESERVADO"));

            JsonNode retirado = retirar(cliente, alquiler, MONTANA, primero);
            JsonNode iniciado = alquiler.historialPropio("RESERVADO", "EN_PROGRESO");
            comprobarActor(iniciado.get(1), cliente);
            assertEquals(fechaPersistida(retirado, "fechaHoraRetiro"), fecha(iniciado.get(1), "fechaHoraCambio"));
            JsonNode reintento = leer(cliente.contexto().request().post(primerRetiro), 200);
            assertEquals(fechaPersistida(retirado, "fechaHoraRetiro"), fecha(reintento, "fechaHoraRetiro"));
            retirar(cliente, alquiler, PASEO, segundo);
            assertEquals(iniciado, alquiler.historialPropio("RESERVADO", "EN_PROGRESO"));

            devolver(cliente, alquiler, MONTANA, primero);
            assertEquals(iniciado, alquiler.historialPropio("RESERVADO", "EN_PROGRESO"));
            JsonNode devuelto = devolver(cliente, alquiler, PASEO, segundo);
            JsonNode finalizado = alquiler.historialPropio("RESERVADO", "EN_PROGRESO", "FINALIZADO");
            comprobarActor(finalizado.get(2), cliente);
            assertEquals(fechaPersistida(devuelto, "fechaHoraDevolucionReal"), fecha(finalizado.get(2), "fechaHoraCambio"));
            comprobarDialogo(cliente.pagina(), filaCliente(cliente, alquiler.id), alquiler.id, finalizado);

            if (MODO_VISIBLE) admin.pagina().bringToFront();
            admin.pagina().reload();
            assertThat(admin.pagina()).hasURL(BASE + "/admin.html");
            // Bootstrap convierte el botón en una pestaña ARIA después de cargar.
            admin.pagina().locator("#adminTabs [data-bs-target='#tab-alquileres']").click();
            Locator filaAdmin = admin.pagina().locator("#tabla-alquileres-body tr")
                    .filter(new Locator.FilterOptions().setHas(admin.pagina().getByRole(AriaRole.CELL,
                            new Page.GetByRoleOptions().setName(Integer.toString(alquiler.id)).setExact(true))));
            comprobarDialogo(admin.pagina(), filaAdmin, alquiler.id, finalizado);
            alquiler.resolverReintegroSimulado();
            assertEquals(finalizado, alquiler.historialPropio("RESERVADO", "EN_PROGRESO", "FINALIZADO"));
        }
    }

    @Test void cancelaSoloAlCerrarLasDosUnidadesPendientes() throws IOException {
        try (Sesion cliente = entrar("cliente.demo", "CLIENTE", "/alquiler.html");
             ReservaDemo alquiler = new ReservaDemo(cliente)) {
            JsonNode reserva = alquiler.crear(MONTANA, PASEO);
            JsonNode inicial = alquiler.historialPropio("RESERVADO");
            int primero = detalle(reserva, MONTANA).path("id").asInt();
            boton(unidad(cliente, alquiler, MONTANA), "Cancelar esta bicicleta").click();
            confirmarPost(cliente, alquiler.ruta("/detalles/" + primero + "/cancelar"), "Reserva actualizada");
            assertEquals(inicial, alquiler.historialPropio("RESERVADO"));

            boton(filaCliente(cliente, alquiler.id), "Cancelar pendientes").click();
            confirmarPost(cliente, alquiler.ruta("/cancelar"), "Reserva actualizada");
            JsonNode cancelado = alquiler.historialPropio("RESERVADO", "CANCELADO");
            comprobarActor(cancelado.get(1), cliente);
            comprobarDialogo(cliente.pagina(), filaCliente(cliente, alquiler.id), alquiler.id, cancelado);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TURISMO_E2E_VENCIMIENTO", matches = "true")
    void elJobVenceLaReservaSinActorHumano() throws IOException, InterruptedException {
        try (Sesion cliente = entrar("cliente.demo", "CLIENTE", "/alquiler.html");
             ReservaDemo alquiler = new ReservaDemo(cliente)) {
            alquiler.crear(PASEO);
            alquiler.historialPropio("RESERVADO");
            long limite = System.nanoTime() + Duration.ofMinutes(16).toNanos();
            // El job usa el reloj real del servidor: no se acorta la reserva ni se escriben fechas por SQL.
            while ("RESERVADO".equals(alquiler.cabecera().path("estado").asText()) && System.nanoTime() < limite) {
                Thread.sleep(5000);
            }
            JsonNode vencido = alquiler.historialPropio("RESERVADO", "VENCIDO");
            assertEquals("SISTEMA", vencido.get(1).path("origen").asText());
            assertTrue(vencido.get(1).path("actorUsuarioId").isNull());
            assertTrue(vencido.get(1).path("actorNickname").isNull());
            cliente.pagina().reload();
            comprobarDialogo(cliente.pagina(), filaCliente(cliente, alquiler.id), alquiler.id, vencido);
        }
    }

    private static BrowserContext nuevoContexto() {
        // Incluso con el navegador en otra zona, el historial del negocio es de Río Grande.
        BrowserContext contexto = navegador.newContext(new Browser.NewContextOptions().setLocale("es-AR").setTimezoneId("UTC"));
        contexto.setDefaultTimeout(15_000);
        return contexto;
    }

    private static Sesion entrar(String nickname, String rol, String destino) throws IOException {
        BrowserContext contexto = nuevoContexto();
        try {
            Page pagina = contexto.newPage();
            if (MODO_VISIBLE) pagina.bringToFront();
            pagina.navigate(BASE + "/index.html");
            pagina.locator("#login-nickname").fill(nickname);
            pagina.locator("#login-password").fill(System.getenv("TURISMO_E2E_DEMO_PASSWORD"));
            Response login = esperar(pagina, "POST", BASE + "/api/login", () -> pagina.locator("#login-form button[type=submit]").click());
            assertEquals(200, login.status(), "No se pudo iniciar sesión con " + nickname);
            assertThat(pagina).hasURL(BASE + destino);
            JsonNode perfil = leer(contexto.request().get(BASE + "/api/usuarios/actual"), 200);
            assertEquals(nickname, perfil.path("nickname").asText());
            assertTrue(perfil.path("activo").asBoolean());
            assertEquals(List.of(rol), StreamSupport.stream(perfil.path("roles").spliterator(), false).map(JsonNode::asText).toList());
            return new Sesion(contexto, pagina, perfil);
        } catch (IOException | RuntimeException | AssertionError ex) {
            contexto.close();
            throw ex;
        }
    }

    private static Locator filaCliente(Sesion cliente, int id) {
        return cliente.pagina().locator("#container-activos article, #tbody-historial tr")
                .filter(new Locator.FilterOptions().setHas(cliente.pagina().locator("details[data-alquiler-detalles='" + id + "']")));
    }

    private static Locator unidad(Sesion cliente, ReservaDemo alquiler, String bici) {
        Locator detalles = filaCliente(cliente, alquiler.id).locator("details");
        assertThat(detalles.locator("summary")).isVisible();
        if (detalles.getAttribute("open") == null) detalles.locator("summary").click();
        return detalles.locator("li").filter(new Locator.FilterOptions().setHasText(bici));
    }

    private static JsonNode retirar(Sesion cliente, ReservaDemo alquiler, String bici, int detalleId) throws IOException {
        boton(unidad(cliente, alquiler, bici), "Retirar").click();
        return confirmarPost(cliente, alquiler.ruta("/detalles/" + detalleId + "/retirar"), "Operación registrada");
    }

    private static JsonNode devolver(Sesion cliente, ReservaDemo alquiler, String bici, int detalleId) throws IOException {
        Page pagina = cliente.pagina();
        boton(unidad(cliente, alquiler, bici), "Devolver").click();
        assertThat(modal(pagina).locator(".swal2-title")).hasText("Devolver bicicleta");
        modal(pagina).locator("select").selectOption(Integer.toString(alquiler.estacionId));
        modal(pagina).locator(".swal2-confirm").click();
        assertThat(modal(pagina).locator(".swal2-title")).hasText("¿La bicicleta tiene una falla?");
        JsonNode respuesta = leer(esperar(pagina, "POST", alquiler.ruta("/detalles/" + detalleId + "/devolver"),
                () -> boton(modal(pagina), "No").click()), 200);
        cerrarAviso(pagina, "Bicicleta devuelta");
        return respuesta;
    }

    private static JsonNode confirmarPost(Sesion sesion, String url, String exito) throws IOException {
        Page pagina = sesion.pagina();
        assertThat(modal(pagina).locator(".swal2-title")).hasText("Confirmar operación");
        JsonNode resultado = leer(esperar(pagina, "POST", url, () -> boton(modal(pagina), "Confirmar").click()), 200);
        cerrarAviso(pagina, exito);
        return resultado;
    }

    private static void comprobarDialogo(Page pagina, Locator fila, int id, JsonNode eventos) throws IOException {
        JsonNode recibido = leer(esperar(pagina, "GET", BASE + "/api/alquileres/" + id + "/historial",
                () -> boton(fila, "Historial de estados").click()), 200);
        assertEquals(eventos, recibido);
        Locator dialogo = modal(pagina);
        assertThat(dialogo.locator(".swal2-title")).hasText("Historial del alquiler #" + id);
        Locator lineas = dialogo.locator("ol > li");
        assertThat(lineas).hasCount(eventos.size());
        Map<String, String> nombres = Map.of("RESERVADO", "Reservado", "EN_PROGRESO", "En progreso",
                "FINALIZADO", "Finalizado", "CANCELADO", "Cancelado", "VENCIDO", "Vencido");
        for (int i = 0; i < eventos.size(); i++) {
            JsonNode evento = eventos.get(i);
            String antes = evento.get("estadoAnterior").isNull() ? "Creación" : nombres.get(evento.get("estadoAnterior").asText());
            String despues = nombres.get(evento.get("estadoNuevo").asText());
            assertThat(lineas.nth(i).locator("strong")).hasText(Pattern.compile(antes + "\\s*→\\s*" + despues));
            String valorApi = evento.path("fechaHoraCambio").asText();
            String visual = (String) pagina.evaluate("valor => new Date(valor).toLocaleString('es-AR', {timeZone: 'America/Argentina/Ushuaia'})", valorApi);
            assertThat(lineas.nth(i).locator("time")).hasText(visual);
            assertThat(lineas.nth(i)).containsText(evento.path("actorNickname").asText("Proceso automático del sistema"));
        }
        boton(dialogo, "Cerrar").click();
        assertThat(dialogo).isHidden();
    }

    private static void comprobarActor(JsonNode evento, Sesion sesion) {
        assertEquals("USUARIO", evento.path("origen").asText());
        assertEquals(sesion.perfil().path("id"), evento.path("actorUsuarioId"));
        assertEquals(sesion.perfil().path("nickname"), evento.path("actorNickname"));
    }

    private static LocalDateTime fecha(JsonNode dato, String propiedad) {
        OffsetDateTime fechaLocal = OffsetDateTime.parse(dato.path(propiedad).asText());
        assertEquals(ZoneOffset.ofHours(-3), fechaLocal.getOffset(), propiedad);
        return fechaLocal.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private static LocalDateTime fechaPersistida(JsonNode respuestaDeEscritura, String propiedad) {
        // El DTO inmediato puede traer nanosegundos; MariaDB guarda DATETIME(6).
        return fecha(respuestaDeEscritura, propiedad).truncatedTo(ChronoUnit.MICROS);
    }

    private static JsonNode detalle(JsonNode reserva, String bici) {
        return StreamSupport.stream(reserva.path("detalles").spliterator(), false)
                .filter(d -> bici.equals(d.path("codigoBicicleta").asText())).findFirst().orElseThrow();
    }

    private static Locator boton(Locator lugar, String nombre) {
        return lugar.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(nombre).setExact(true));
    }

    private static Locator modal(Page pagina) { return pagina.locator(".swal2-popup:visible"); }

    private static void cerrarAviso(Page pagina, String titulo) {
        Locator dialogo = modal(pagina);
        assertThat(dialogo.locator(".swal2-title")).hasText(titulo);
        dialogo.locator(".swal2-confirm").click();
        assertThat(dialogo).isHidden();
    }

    private static Response esperar(Page pagina, String metodo, String ruta, Runnable accion) {
        return pagina.waitForResponse(r -> ruta.equals(r.url()) && metodo.equals(r.request().method()), accion);
    }

    private static JsonNode leer(Response respuesta, int estado) throws IOException {
        assertEquals(estado, respuesta.status(), () -> respuesta.url() + ": " + respuesta.text());
        return JSON.readTree(respuesta.body());
    }

    private static JsonNode leer(APIResponse respuesta, int estado) throws IOException {
        try {
            assertEquals(estado, respuesta.status(), () -> respuesta.url() + ": " + respuesta.text());
            return JSON.readTree(respuesta.body());
        } finally { respuesta.dispose(); }
    }

    private record Sesion(BrowserContext contexto, Page pagina, JsonNode perfil) implements AutoCloseable {
        @Override public void close() { contexto.close(); }
    }

    private static final class ReservaDemo implements AutoCloseable {
        private final Sesion cliente;
        private int id;
        private int estacionId;

        private ReservaDemo(Sesion cliente) { this.cliente = cliente; }

        private String ruta(String sufijo) { return BASE + "/api/alquileres/" + id + sufijo; }

        private JsonNode crear(String... bicis) throws IOException {
            APIRequestContext api = cliente.contexto().request();
            JsonNode saldo = leer(api.get(BASE + "/api/pagos/saldo-simulado"), 200);
            assertEquals("SIMULADA", saldo.path("plataforma").asText());
            assertEquals("PRUEBA", saldo.path("entorno").asText());
            assertTrue(saldo.path("saldoDisponibleArs").decimalValue().signum() > 0, "Sin saldo simulado.");

            JsonNode disponibles = leer(api.get(BASE + "/api/bicicletas/disponibles"), 200);
            for (String codigo : bicis) {
                JsonNode bici = StreamSupport.stream(disponibles.spliterator(), false)
                        .filter(b -> codigo.equals(b.path("codigoBicicleta").asText()) && ESTACION.equals(b.path("ubicacion").asText()))
                        .findFirst().orElseThrow(() -> new AssertionError(codigo + " no está disponible en " + ESTACION));
                if (estacionId == 0) estacionId = bici.path("estacionId").asInt();
                assertTrue(estacionId > 0);
                assertEquals(estacionId, bici.path("estacionId").asInt());
            }

            Page pagina = cliente.pagina();
            for (String codigo : bicis) pagina.locator("#bicicletas-container article")
                    .filter(new Locator.FilterOptions().setHasText(codigo)).locator("input[type=checkbox]").check();
            pagina.locator("#duracion-reserva").fill("10");
            pagina.locator("#crear-reserva").click();
            JsonNode datos = leer(esperar(pagina, "POST", BASE + "/api/alquileres", () -> boton(modal(pagina), "Reservar").click()), 201);
            id = datos.path("id").asInt();
            assertTrue(id > 0);
            System.out.println("Reserva demo E2E: alquiler #" + id);
            assertEquals(cliente.perfil().path("id"), datos.path("usuarioId"));
            assertEquals(estacionId, datos.path("estacionRetiroId").asInt());
            assertEquals(bicis.length, datos.path("detalles").size());
            assertTrue(Duration.between(fecha(datos, "fechaHoraCreacion").toInstant(ZoneOffset.UTC), Instant.now())
                    .abs().compareTo(Duration.ofMinutes(2)) < 0, "La fecha API no representa el instante actual de reserva.");
            cerrarAviso(pagina, "Reserva creada");
            assertThat(pagina).hasURL(BASE + "/mis_alquileres.html");
            return datos;
        }

        private JsonNode cabecera() throws IOException { return leer(cliente.contexto().request().get(ruta("")), 200); }

        private JsonNode historialPropio(String... estados) throws IOException {
            JsonNode eventos = leer(cliente.contexto().request().get(ruta("/historial")), 200);
            assertTrue(eventos.isArray());
            assertEquals(estados.length, eventos.size(), eventos::toString);
            int ultimoId = 0;
            LocalDateTime ultimaFecha = null;
            for (int i = 0; i < estados.length; i++) {
                JsonNode evento = eventos.get(i);
                assertEquals(id, evento.path("alquilerId").asInt());
                assertEquals(estados[i], evento.path("estadoNuevo").asText());
                if (i == 0) assertTrue(evento.path("estadoAnterior").isNull());
                else assertEquals(estados[i - 1], evento.path("estadoAnterior").asText());
                assertTrue(evento.path("id").asInt() > ultimoId);
                assertFalse(evento.has("password") || evento.has("actorUsuario"));
                LocalDateTime fechaEvento = fecha(evento, "fechaHoraCambio");
                if (ultimaFecha != null) assertFalse(fechaEvento.isBefore(ultimaFecha));
                ultimaFecha = fechaEvento;
                ultimoId = evento.path("id").asInt();
            }
            assertEquals(estados[estados.length - 1], cabecera().path("estado").asText());
            return eventos;
        }

        private void resolverReintegroSimulado() throws IOException {
            APIRequestContext api = cliente.contexto().request();
            String url = BASE + "/api/pagos/reembolsos/" + id;
            JsonNode finanzas = leer(api.get(url), 200);
            if ("REEMBOLSO_PENDIENTE".equals(finanzas.path("liquidacion").path("situacionFinanciera").asText())) {
                finanzas = leer(api.post(url + "/resolver"), 200);
            }
            assertEquals("SALDADO", finanzas.path("liquidacion").path("situacionFinanciera").asText(),
                    () -> "Revisá la liquidación simulada del alquiler demo #" + id);
        }

        @Override public void close() throws IOException {
            if (id == 0) return;
            // En caso de fallo, cerrar solo los detalles de nuestra propia reserva por la API de negocio.
            APIRequestContext api = cliente.contexto().request();
            JsonNode actual = cabecera();
            for (JsonNode d : actual.path("detalles")) {
                if ("EN_CURSO".equals(d.path("estado").asText())) {
                    leer(api.post(ruta("/detalles/" + d.path("id").asInt() + "/devolver"), RequestOptions.create()
                            .setData(Map.of("estacionDevolucionId", estacionId, "fallaReportada", false))), 200);
                }
            }
            if (StreamSupport.stream(actual.path("detalles").spliterator(), false)
                    .anyMatch(d -> List.of("RESERVADO", "PENDIENTE_RETIRO").contains(d.path("estado").asText()))) {
                leer(api.post(ruta("/cancelar")), 200);
            }
            resolverReintegroSimulado();
        }
    }
}
