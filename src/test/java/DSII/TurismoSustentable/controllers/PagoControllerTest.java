package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.dto.SaldoSimuladoDTO;
import DSII.TurismoSustentable.services.PagoService;
import DSII.TurismoSustentable.dto.ResultadoAnticipoDTO;
import DSII.TurismoSustentable.dto.ResultadoAdicionalDTO;
import DSII.TurismoSustentable.dto.LiquidacionAlquilerDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.EstadoPago;
import DSII.TurismoSustentable.models.Pago;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PagoController.class)
@ContextConfiguration(classes = {
        PagoController.class,
        WebAuthorization.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("refactor")
class PagoControllerTest {

    private static final String RUTA = "/api/pagos/saldo-simulado";
    private static final String RUTA_ANTICIPO =
            "/api/pagos/anticipo/7";

    private static final String RUTA_RESOLVER =
            "/api/pagos/anticipo/7/resolver";

    private static final String RUTA_ADICIONAL = "/api/pagos/adicional/7";
    private static final String RESOLVER_ADICIONAL = "/api/pagos/adicional/7/resolver";

    private static final String CLAVE =
            "123e4567-e89b-42d3-a456-426614174000";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagoService pagoService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void visitanteRecibe401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(
            username = "qa.saldo@example.com",
            authorities = "CLIENTE"
    )
    void clienteConsultaSuSaldoSinElegirOtroUsuario() throws Exception {
        when(pagoService.consultarSaldoSimulado()).thenReturn(saldo());

        mockMvc.perform(get(RUTA).param("usuarioId", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.nickname").value("qa.listados"))
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.plataforma").value("SIMULADA"))
                .andExpect(jsonPath("$.entorno").value("PRUEBA"))
                .andExpect(jsonPath("$.saldoDisponibleArs").value(999925.00));

        verify(pagoService).consultarSaldoSimulado();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void administradorSinClienteRecibe403() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void tecnicoRecibe403() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isForbidden());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(
            username = "qa.saldo@example.com",
            authorities = {"ADMIN", "CLIENTE"}
    )
    void administradorConClientePuedeConsultarSuSaldo() throws Exception {
        when(pagoService.consultarSaldoSimulado()).thenReturn(saldo());

        mockMvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(1));

        verify(pagoService).consultarSaldoSimulado();
    }

    @ParameterizedTest
    @ValueSource(strings = {RUTA_ADICIONAL, RESOLVER_ADICIONAL})
    void adicionalSinSesionResponde401(String ruta) throws Exception {
        mockMvc.perform(post(ruta).header("Idempotency-Key", CLAVE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(pagoService);
    }

    @ParameterizedTest
    @CsvSource({
            RUTA_ADICIONAL + ",ADMIN",
            RUTA_ADICIONAL + ",TECNICO",
            RESOLVER_ADICIONAL + ",ADMIN",
            RESOLVER_ADICIONAL + ",TECNICO"
    })
    void adicionalNoPermiteRolesSinCliente(String ruta, String rol) throws Exception {
        mockMvc.perform(post(ruta).header("Idempotency-Key", CLAVE)
                        .with(user("qa.finanzas").authorities(AuthorityUtils.createAuthorityList(rol))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(pagoService);
    }

    @ParameterizedTest
    @CsvSource({
            RUTA_ADICIONAL + ",false",
            RUTA_ADICIONAL + ",true",
            RESOLVER_ADICIONAL + ",false",
            RESOLVER_ADICIONAL + ",true"
    })
    void adicionalUsaImporteDelServicioNoElEnviado(String ruta, boolean ambosRoles) throws Exception {
        boolean resolver = RESOLVER_ADICIONAL.equals(ruta);
        if (resolver) when(pagoService.resolverAdicional(7, CLAVE)).thenReturn(adicional("APROBADO"));
        else when(pagoService.iniciarAdicional(7, CLAVE)).thenReturn(adicional("CREADO"));
        String[] roles = ambosRoles ? new String[]{"ADMIN", "CLIENTE"} : new String[]{"CLIENTE"};

        mockMvc.perform(post(ruta).header("Idempotency-Key", CLAVE)
                        .with(user("qa.adicional").authorities(AuthorityUtils.createAuthorityList(roles)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"montoArs\":1,\"usuarioId\":999,\"estadoPago\":\"APROBADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alquilerId").value(7))
                .andExpect(jsonPath("$.pagoId").value(12))
                .andExpect(jsonPath("$.montoArs").value(200.0))
                .andExpect(jsonPath("$.estadoPago").value(resolver ? "APROBADO" : "CREADO"))
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.finalidad").value("ADICIONAL"))
                .andExpect(jsonPath("$.claveSolicitud").value(CLAVE))
                .andExpect(jsonPath("$.liquidacion.situacionFinanciera")
                        .value(resolver ? "SALDADO" : "SALDO_PENDIENTE"))
                .andExpect(jsonPath("$.liquidacion.saldoPendienteArs").value(resolver ? 0.0 : 200.0))
                .andExpect(jsonPath("$.usuarioId").doesNotExist())
                .andExpect(jsonPath("$.pago").doesNotExist());
        if (resolver) verify(pagoService, only()).resolverAdicional(7, CLAVE);
        else verify(pagoService, only()).iniciarAdicional(7, CLAVE);
    }

    @ParameterizedTest
    @ValueSource(strings = {RUTA_ADICIONAL, RESOLVER_ADICIONAL})
    @WithMockUser(authorities = "CLIENTE")
    void adicionalExigeCabeceraDeIdempotencia(String ruta) throws Exception {
        mockMvc.perform(post(ruta)).andExpect(status().isBadRequest());
        verifyNoInteractions(pagoService);
    }

    @ParameterizedTest
    @CsvSource({"false,400", "true,400", "false,404", "true,404", "false,409", "true,409"})
    @WithMockUser(authorities = "CLIENTE")
    void adicionalConservaErroresDelServicio(boolean resolver, int estado) throws Exception {
        ResponseStatusException fallo = new ResponseStatusException(HttpStatus.valueOf(estado), "Operación rechazada.");
        if (resolver) when(pagoService.resolverAdicional(7, CLAVE)).thenThrow(fallo);
        else when(pagoService.iniciarAdicional(7, CLAVE)).thenThrow(fallo);
        mockMvc.perform(post(resolver ? RESOLVER_ADICIONAL : RUTA_ADICIONAL).header("Idempotency-Key", CLAVE))
                .andExpect(status().is(estado))
                .andExpect(content().string("Operación rechazada."));
        if (resolver) verify(pagoService, only()).resolverAdicional(7, CLAVE);
        else verify(pagoService, only()).iniciarAdicional(7, CLAVE);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @WithMockUser(authorities = "CLIENTE")
    void adicionalDenegadoPorPropiedadConserva403(boolean resolver) throws Exception {
        AccessDeniedException fallo = new AccessDeniedException("Alquiler ajeno.");
        if (resolver) when(pagoService.resolverAdicional(7, CLAVE)).thenThrow(fallo);
        else when(pagoService.iniciarAdicional(7, CLAVE)).thenThrow(fallo);
        mockMvc.perform(post(resolver ? RESOLVER_ADICIONAL : RUTA_ADICIONAL).header("Idempotency-Key", CLAVE))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @WithMockUser(authorities = "CLIENTE")
    void adicionalNoExponeFallosInternos(boolean resolver) throws Exception {
        ConfiguracionSistemaException fallo = new ConfiguracionSistemaException("DETALLE_NO_PUBLICAR");
        if (resolver) when(pagoService.resolverAdicional(7, CLAVE)).thenThrow(fallo);
        else when(pagoService.iniciarAdicional(7, CLAVE)).thenThrow(fallo);
        mockMvc.perform(post(resolver ? RESOLVER_ADICIONAL : RUTA_ADICIONAL).header("Idempotency-Key", CLAVE))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se pudo completar la operación por un problema interno. "
                        + "Intentá nuevamente más tarde."));
    }

    private ResultadoAdicionalDTO adicional(String estado) {
        Alquiler alquiler = new Alquiler();
        alquiler.setId(7);
        LocalDateTime fecha = LocalDateTime.of(2026, 11, 16, 8, 0);
        Pago pago = new Pago(alquiler, new EstadoPago("CREADO"), "ADICIONAL",
                new BigDecimal("200.00"), fecha, "SIMULADA", "PRUEBA", "ADICIONAL:1:" + CLAVE);
        ReflectionTestUtils.setField(pago, "id", 12);
        pago.setEstadoPago(new EstadoPago(estado));
        boolean aprobado = "APROBADO".equals(estado);
        if (aprobado) pago.setFechaConfirmacion(fecha);
        LiquidacionAlquilerDTO resumen = new LiquidacionAlquilerDTO(7, 3,
                aprobado ? "SALDADO" : "SALDO_PENDIENTE", fecha.minusMinutes(5),
                new BigDecimal("2.00"), new BigDecimal("300.00"),
                new BigDecimal(aprobado ? "300.00" : "100.00"), new BigDecimal("0.00"),
                new BigDecimal(aprobado ? "300.00" : "100.00"),
                new BigDecimal(aprobado ? "0.00" : "200.00"), new BigDecimal("0.00"));
        return new ResultadoAdicionalDTO(pago, CLAVE, resumen);
    }

    private SaldoSimuladoDTO saldo() {
        return new SaldoSimuladoDTO(
                1,
                "qa.listados",
                new BigDecimal("1000000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("25.00"),
                new BigDecimal("999925.00")
        );
    }

    @Test
    void visitanteNoPuedeIniciarAnticipo() throws Exception {
        mockMvc.perform(
                        post(RUTA_ANTICIPO)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void administradorSinClienteNoPuedeIniciarAnticipo() throws Exception {
        mockMvc.perform(
                        post(RUTA_ANTICIPO)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void tecnicoNoPuedeIniciarAnticipo() throws Exception {
        mockMvc.perform(
                        post(RUTA_ANTICIPO)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(
            username = "qa.anticipo@example.com",
            authorities = "CLIENTE"
    )
    void clienteIniciaSinElegirImporteUsuarioOEstado() throws Exception {
        when(pagoService.iniciarAnticipo(7, CLAVE))
                .thenReturn(anticipoCreado());

        mockMvc.perform(
                        post(RUTA_ANTICIPO)
                                .header("Idempotency-Key", CLAVE)
                                .contentType("application/json")
                                .content(
                                        "{\"montoArs\":1,"
                                                + "\"usuarioId\":999,"
                                                + "\"estadoPago\":\"APROBADO\"}"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alquilerId").value(7))
                .andExpect(jsonPath("$.pagoId").value(11))
                .andExpect(jsonPath("$.montoArs").value(300.00))
                .andExpect(jsonPath("$.estadoPago").value("CREADO"))
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.claveSolicitud").value(CLAVE))
                .andExpect(jsonPath("$.sinImporteACobrar").value(false))
                .andExpect(jsonPath("$.usuarioId").doesNotExist());

        verify(pagoService).iniciarAnticipo(7, CLAVE);
    }

    @Test
    @WithMockUser(
            username = "qa.anticipo@example.com",
            authorities = {"ADMIN", "CLIENTE"}
    )
    void administradorConClientePuedeIniciarSuAnticipo() throws Exception {
        when(pagoService.iniciarAnticipo(7, CLAVE))
                .thenReturn(anticipoCreado());

        mockMvc.perform(
                        post(RUTA_ANTICIPO)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alquilerId").value(7));

        verify(pagoService).iniciarAnticipo(7, CLAVE);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void inicioExigeCabeceraDeIdempotencia() throws Exception {
        mockMvc.perform(post(RUTA_ANTICIPO))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void conflictoConservaEstadoHttpYMensaje() throws Exception {
        when(pagoService.iniciarAnticipo(7, CLAVE))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe un anticipo pendiente."
                ));

        mockMvc.perform(
                        post(RUTA_ANTICIPO)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Ya existe un anticipo pendiente."
                ));

        verify(pagoService).iniciarAnticipo(7, CLAVE);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void denegacionDelServicioSeConservaComo403() throws Exception {
        when(pagoService.iniciarAnticipo(7, CLAVE))
                .thenThrow(new AccessDeniedException(
                        "El alquiler pertenece a otra cuenta."
                ));

        mockMvc.perform(
                        post(RUTA_ANTICIPO)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isForbidden());

        verify(pagoService).iniciarAnticipo(7, CLAVE);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void configuracionInvalidaDevuelve500SinExponerDetalle() throws Exception {
        when(pagoService.iniciarAnticipo(7, CLAVE))
                .thenThrow(new ConfiguracionSistemaException(
                        "Detalle interno del catálogo."
                ));

        mockMvc.perform(
                        post(RUTA_ANTICIPO)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "No se pudo completar la operación por un problema interno. "
                                + "Intentá nuevamente más tarde."
                ));

        verify(pagoService).iniciarAnticipo(7, CLAVE);
    }

    private ResultadoAnticipoDTO anticipoCreado() {
        return anticipoConEstado("CREADO");
    }

    private ResultadoAnticipoDTO anticipoConEstado(String estado) {
        Alquiler alquiler = new Alquiler();
        alquiler.setId(7);

        LocalDateTime fecha =
                LocalDateTime.of(2026, 11, 15, 12, 5);

        Pago pago = new Pago(
                alquiler,
                new EstadoPago("CREADO"),
                "ANTICIPO",
                new BigDecimal("300.00"),
                fecha,
                "SIMULADA",
                "PRUEBA",
                "ANTICIPO:1:" + CLAVE
        );

        ReflectionTestUtils.setField(pago, "id", 11);
        pago.setEstadoPago(new EstadoPago(estado));

        if ("APROBADO".equals(estado)) {
            pago.setFechaConfirmacion(fecha);
        }

        return new ResultadoAnticipoDTO(
                7,
                new BigDecimal("300.00"),
                pago,
                CLAVE
        );
    }

    @Test
    void visitanteNoPuedeResolverAnticipo() throws Exception {
        mockMvc.perform(
                        post(RUTA_RESOLVER)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void administradorSinClienteNoPuedeResolverAnticipo() throws Exception {
        mockMvc.perform(
                        post(RUTA_RESOLVER)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void tecnicoNoPuedeResolverAnticipo() throws Exception {
        mockMvc.perform(
                        post(RUTA_RESOLVER)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeElegirElResultadoDelPago() throws Exception {
        when(pagoService.resolverAnticipo(7, CLAVE))
                .thenReturn(anticipoConEstado("PENDIENTE"));

        mockMvc.perform(
                        post(RUTA_RESOLVER)
                                .header("Idempotency-Key", CLAVE)
                                .contentType("application/json")
                                .content(
                                        "{\"estadoPago\":\"APROBADO\","
                                                + "\"montoArs\":1,"
                                                + "\"usuarioId\":999}"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagoId").value(11))
                .andExpect(jsonPath("$.estadoPago").value("PENDIENTE"))
                .andExpect(jsonPath("$.montoArs").value(300.00))
                .andExpect(jsonPath("$.claveSolicitud").value(CLAVE));

        verify(pagoService).resolverAnticipo(7, CLAVE);
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "CLIENTE"})
    void administradorConClientePuedeResolverSuAnticipo() throws Exception {
        when(pagoService.resolverAnticipo(7, CLAVE))
                .thenReturn(anticipoConEstado("APROBADO"));

        mockMvc.perform(
                        post(RUTA_RESOLVER)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("APROBADO"));

        verify(pagoService).resolverAnticipo(7, CLAVE);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void resolverExigeClaveDeIdempotencia() throws Exception {
        mockMvc.perform(post(RUTA_RESOLVER))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pagoService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void resolverConserva404YMensaje() throws Exception {
        when(pagoService.resolverAnticipo(7, CLAVE))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El intento no existe."
                ));

        mockMvc.perform(
                        post(RUTA_RESOLVER)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().string("El intento no existe."));

        verify(pagoService).resolverAnticipo(7, CLAVE);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void resolverAlquilerAjenoConserva403() throws Exception {
        when(pagoService.resolverAnticipo(7, CLAVE))
                .thenThrow(new AccessDeniedException(
                        "El alquiler pertenece a otra cuenta."
                ));

        mockMvc.perform(
                        post(RUTA_RESOLVER)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isForbidden());

        verify(pagoService).resolverAnticipo(7, CLAVE);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void resolverNoExponeDetallesDeFallosInternos() throws Exception {
        when(pagoService.resolverAnticipo(7, CLAVE))
                .thenThrow(new ConfiguracionSistemaException(
                        "Detalle interno al resolver."
                ));

        mockMvc.perform(
                        post(RUTA_RESOLVER)
                                .header("Idempotency-Key", CLAVE)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "No se pudo completar la operación por un problema interno. "
                                + "Intentá nuevamente más tarde."
                ));

        verify(pagoService).resolverAnticipo(7, CLAVE);
    }

}
