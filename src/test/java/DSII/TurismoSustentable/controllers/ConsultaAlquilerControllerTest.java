package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.security.SesionVigenteMockConfig;
import org.springframework.context.annotation.Import;
import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.dto.AlquilerCabeceraDTO;
import DSII.TurismoSustentable.dto.LiquidacionAlquilerDTO;
import DSII.TurismoSustentable.dto.HistorialEstadoAlquilerDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.services.AlquilerService;
import DSII.TurismoSustentable.services.EstadisticasService;
import DSII.TurismoSustentable.services.LiquidacionAlquilerService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({
        AlquilerController.class,
        AdminController.class
})
@ContextConfiguration(classes = {
        AlquilerController.class,
        AdminController.class,
        WebAuthorization.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@Import(SesionVigenteMockConfig.class)
class ConsultaAlquilerControllerTest {

    private static final String LISTADO_PROPIO =
            "/api/alquileres";

    private static final String LISTADO_ADMIN =
            "/api/admin/estadisticas/alquileres-todos";

    private static final String CONSULTA_LIQUIDACION =
            "/api/alquileres/50/liquidacion";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlquilerService alquilerService;

    @MockBean
    private LiquidacionAlquilerService liquidacionAlquilerService;

    @MockBean
    private EstadisticasService estadisticasService;

    @MockBean
    private UserDetailsService userDetailsService;

    @AfterEach
    void consultasNoEjecutanEstadisticas() {
        verifyNoInteractions(estadisticasService);
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/alquileres/50"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(alquilerService);
    }

    @Test
    void historialSinSesionResponde401SinConsultar() throws Exception {
        mockMvc.perform(get("/api/alquileres/50/historial")).andExpect(status().isUnauthorized());
        verifyNoInteractions(alquilerService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "ADMIN"})
    void historialEsConsultaPermitidaParaClienteYAdminSinDatosSensibles(String rol) throws Exception {
        when(alquilerService.historialAlquiler(50)).thenReturn(List.of(new HistorialEstadoAlquilerDTO(
                1, 50, null, "RESERVADO", 7, "qa.historial", "USUARIO", "Reserva creada.",
                LocalDateTime.of(2026, 11, 18, 12, 0))));
        mockMvc.perform(get("/api/alquileres/50/historial").with(user("qa").authorities(new SimpleGrantedAuthority(rol))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alquilerId").value(50))
                .andExpect(jsonPath("$[0].estadoAnterior").value(nullValue()))
                .andExpect(jsonPath("$[0].estadoNuevo").value("RESERVADO"))
                .andExpect(jsonPath("$[0].actorUsuarioId").value(7))
                .andExpect(jsonPath("$[0].actorNickname").value("qa.historial"))
                .andExpect(jsonPath("$[0].actorUsuario").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
        verify(alquilerService).historialAlquiler(50);
        verifyNoInteractions(liquidacionAlquilerService);
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void tecnicoNoPuedeConsultarHistorialDeAlquiler() throws Exception {
        mockMvc.perform(get("/api/alquileres/50/historial")).andExpect(status().isForbidden());
        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void historialAjenoConserva403DelServicio() throws Exception {
        when(alquilerService.historialAlquiler(50)).thenThrow(new AccessDeniedException("Alquiler ajeno."));
        mockMvc.perform(get("/api/alquileres/50/historial")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void historialInexistenteConserva404() throws Exception {
        when(alquilerService.historialAlquiler(50)).thenThrow(new EntityNotFoundException("No existe."));
        mockMvc.perform(get("/api/alquileres/50/historial")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void historialIdInvalidoConserva400() throws Exception {
        when(alquilerService.historialAlquiler(0)).thenThrow(new IllegalArgumentException("ID inválido."));
        mockMvc.perform(get("/api/alquileres/0/historial")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void noExisteEndpointParaModificarHistoria() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/alquileres/50/historial").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void tecnicoSinOtrosRolesResponde403() throws Exception {
        mockMvc.perform(get("/api/alquileres/50"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteAccedeYRecibeCabeceraConDetalles() throws Exception {
        comprobarConsultaPermitida();
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void administradorPuedeConsultar() throws Exception {
        comprobarConsultaPermitida();
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "CLIENTE"})
    void cuentaConAmbosRolesPuedeConsultar() throws Exception {
        comprobarConsultaPermitida();
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void denegacionDelServicioSeConservaComo403() throws Exception {
        when(alquilerService.obtenerAlquiler(50))
                .thenThrow(
                        new AccessDeniedException(
                                "No tiene permiso para consultar este alquiler."
                        )
                );

        mockMvc.perform(get("/api/alquileres/50"))
                .andExpect(status().isForbidden());

        verify(alquilerService).obtenerAlquiler(50);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void alquilerInexistenteResponde404() throws Exception {
        when(alquilerService.obtenerAlquiler(50))
                .thenThrow(
                        new EntityNotFoundException(
                                "El alquiler no existe."
                        )
                );

        mockMvc.perform(get("/api/alquileres/50"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        "El alquiler no existe."
                ));

        verify(alquilerService).obtenerAlquiler(50);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @WithMockUser(authorities = "CLIENTE")
    void idNoPositivoResponde400(int id) throws Exception {
        when(alquilerService.obtenerAlquiler(id))
                .thenThrow(
                        new IllegalArgumentException(
                                "Debe indicar un alquiler válido."
                        )
                );

        mockMvc.perform(get("/api/alquileres/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Debe indicar un alquiler válido."
                ));

        verify(alquilerService).obtenerAlquiler(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "2147483648"})
    @WithMockUser(authorities = "CLIENTE")
    void idQueNoPuedeConvertirseAIntegerResponde400(
            String id
    ) throws Exception {
        mockMvc.perform(get("/api/alquileres/{id}", id))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void falloInternoResponde500SinExponerDetalleTecnico()
            throws Exception {

        when(alquilerService.obtenerAlquiler(50))
                .thenThrow(
                        new ConfiguracionSistemaException(
                                "DETALLE_INTERNO_NO_PUBLICAR"
                        )
                );

        mockMvc.perform(get("/api/alquileres/50"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "No se pudo completar la operación por un problema interno. "
                                + "Intentá nuevamente más tarde."
                ));

        verify(alquilerService).obtenerAlquiler(50);
    }

    @ParameterizedTest
    @ValueSource(strings = {LISTADO_PROPIO, LISTADO_ADMIN})
    void listadosSinSesionResponden401(String ruta)
            throws Exception {

        mockMvc.perform(get(ruta))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(alquilerService);
    }

    @ParameterizedTest
    @CsvSource({
            LISTADO_PROPIO + ",CLIENTE,200",
            LISTADO_PROPIO + ",ADMIN,200",
            LISTADO_PROPIO + ",TECNICO,403",
            LISTADO_ADMIN + ",CLIENTE,403",
            LISTADO_ADMIN + ",ADMIN,200",
            LISTADO_ADMIN + ",TECNICO,403"
    })
    void listadosRespetanPermisosDeRol(
            String ruta,
            String rol,
            int estadoEsperado
    ) throws Exception {

        if (estadoEsperado == 200) {
            prepararRespuestaListado(ruta, List.of());
        }

        var resultado = mockMvc.perform(
                        get(ruta)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(user("qa.listados").authorities(
                                        new SimpleGrantedAuthority(rol)
                                ))
                )
                .andExpect(status().is(estadoEsperado));

        if (estadoEsperado == 200) {
            resultado
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    ))
                    .andExpect(content().json("[]"));

            verificarConsultaListado(ruta);
        } else {
            verifyNoInteractions(alquilerService);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {LISTADO_PROPIO, LISTADO_ADMIN})
    @WithMockUser(authorities = {"ADMIN", "CLIENTE"})
    void listadosSerializanCabeceraYDetallesSinCamposAnteriores(
            String ruta
    ) throws Exception {

        prepararRespuestaListado(
                ruta,
                List.of(crearRespuesta())
        );

        mockMvc.perform(
                        get(ruta).accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.length()").value(1))

                // Modelo del DER v5: cabecera con detalles.
                .andExpect(jsonPath("$[0].modelo").value("DETALLES"))
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[0].estado").value("RESERVADO"))
                .andExpect(jsonPath("$[0].cotizacionArsPorUsd")
                        .value(1530.0))
                .andExpect(jsonPath("$[0].costoEstimadoArs")
                        .value(22950.0))
                .andExpect(jsonPath("$[0].costoFinalUsd")
                        .value(nullValue()))
                .andExpect(jsonPath("$[0].fechaHoraLiquidacion")
                        .value(nullValue()))
                .andExpect(jsonPath("$[0].detalles.length()").value(1))
                .andExpect(jsonPath(
                        "$[0].detalles[0].precioPorHoraAplicadoUsd"
                ).value(10.0))
                .andExpect(jsonPath(
                        "$[0].detalles[0].fechaHoraRetiro"
                ).value(nullValue()))

                // No serializar entidades completas.
                .andExpect(jsonPath("$[0].usuario").doesNotExist())
                .andExpect(jsonPath("$[0].detalles[0].bicicleta")
                        .doesNotExist())

                // La cabecera no vuelve a publicar campos del modelo anterior.
                .andExpect(jsonPath("$[0].codigoBicicleta")
                        .doesNotExist())
                .andExpect(jsonPath("$[0].costoTotal").doesNotExist())
                .andExpect(jsonPath("$[0].costoTotalArs").doesNotExist())
                .andExpect(jsonPath("$[0].fechaInicio").doesNotExist())
                .andExpect(jsonPath("$[0].fechaFin").doesNotExist());

        verificarConsultaListado(ruta);
    }

    @ParameterizedTest
    @CsvSource({
            LISTADO_PROPIO + ",true",
            LISTADO_ADMIN + ",true",
            LISTADO_PROPIO + ",false",
            LISTADO_ADMIN + ",false"
    })
    @WithMockUser(authorities = "ADMIN")
    void fallosDeListadoConservanSuEstado(
            String ruta,
            boolean denegacion
    ) throws Exception {

        RuntimeException fallo = denegacion
                ? new AccessDeniedException("Permiso retirado.")
                : new ConfiguracionSistemaException(
                        "DETALLE_INTERNO_NO_PUBLICAR"
                );

        if (LISTADO_PROPIO.equals(ruta)) {
            when(alquilerService.listarMisAlquileres())
                    .thenThrow(fallo);
        } else {
            when(alquilerService.listarAlquileresAdministracion())
                    .thenThrow(fallo);
        }

        var resultado = mockMvc.perform(get(ruta))
                .andExpect(status().is(denegacion ? 403 : 500));

        if (!denegacion) {
            resultado.andExpect(content().string(
                    "No se pudo completar la operación por un problema interno. "
                            + "Intentá nuevamente más tarde."
            ));
        }

        verificarConsultaListado(ruta);
    }

    @Test
    void liquidacionSinSesionResponde401() throws Exception {
        mockMvc.perform(get(CONSULTA_LIQUIDACION))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                liquidacionAlquilerService,
                alquilerService
        );
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void liquidacionTecnicoSinOtrosRolesResponde403() throws Exception {
        mockMvc.perform(get(CONSULTA_LIQUIDACION))
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                liquidacionAlquilerService,
                alquilerService
        );
    }

    @ParameterizedTest
    @CsvSource({
            "CLIENTE,false",
            "ADMIN,false",
            "ADMIN,true"
    })
    void liquidacionPermiteRolesDeLectura(
            String rol,
            boolean ambosRoles
    ) throws Exception {

        when(liquidacionAlquilerService.consultarLiquidacion(50))
                .thenReturn(crearRespuestaLiquidacion("SALDADO"));

        List<SimpleGrantedAuthority> autoridades = ambosRoles
                ? List.of(
                        new SimpleGrantedAuthority("ADMIN"),
                        new SimpleGrantedAuthority("CLIENTE")
                )
                : List.of(new SimpleGrantedAuthority(rol));

        mockMvc.perform(
                        get(CONSULTA_LIQUIDACION)
                                .accept(MediaType.APPLICATION_JSON)
                                .with(user("qa.finanzas")
                                        .authorities(autoridades))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alquilerId").value(50))
                .andExpect(jsonPath("$.situacionFinanciera")
                        .value("SALDADO"))
                .andExpect(jsonPath("$.moneda").value("ARS"));

        verify(liquidacionAlquilerService, only())
                .consultarLiquidacion(50);
        verifyNoInteractions(alquilerService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SIN_LIQUIDAR",
            "SALDO_PENDIENTE",
            "REEMBOLSO_PENDIENTE",
            "SALDADO"
    })
    @WithMockUser(authorities = "CLIENTE")
    void liquidacionSerializaLasSituacionesSinInventarImportes(
            String situacion
    ) throws Exception {

        LiquidacionAlquilerDTO respuesta =
                crearRespuestaLiquidacion(situacion);

        when(liquidacionAlquilerService.consultarLiquidacion(50))
                .thenReturn(respuesta);

        var resultado = mockMvc.perform(
                        get(CONSULTA_LIQUIDACION)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.alquilerId").value(50))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.situacionFinanciera")
                        .value(situacion))
                .andExpect(jsonPath("$.moneda").value("ARS"))
                .andExpect(jsonPath("$.pagosAprobadosArs")
                        .value(20655.0))
                .andExpect(jsonPath("$.reembolsosConfirmadosArs")
                        .value(0.0))
                .andExpect(jsonPath("$.netoConfirmadoArs")
                        .value(20655.0))

                // No devolver entidades completas ni datos privados.
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.alquiler").doesNotExist())
                .andExpect(jsonPath("$.pagos").doesNotExist());

        if ("SIN_LIQUIDAR".equals(situacion)) {
            resultado
                    .andExpect(jsonPath("$.fechaHoraLiquidacion")
                            .value(nullValue()))
                    .andExpect(jsonPath("$.costoFinalUsd")
                            .value(nullValue()))
                    .andExpect(jsonPath("$.costoFinalArs")
                            .value(nullValue()))
                    .andExpect(jsonPath("$.saldoPendienteArs")
                            .value(nullValue()))
                    .andExpect(jsonPath("$.reembolsoPendienteArs")
                            .value(nullValue()));
        } else {
            resultado
                    .andExpect(jsonPath("$.fechaHoraLiquidacion")
                            .value("2026-11-16T04:30:45-03:00"))
                    .andExpect(jsonPath("$.costoFinalUsd")
                            .value(respuesta.getCostoFinalUsd()
                                    .doubleValue()))
                    .andExpect(jsonPath("$.costoFinalArs")
                            .value(respuesta.getCostoFinalArs()
                                    .doubleValue()))
                    .andExpect(jsonPath("$.saldoPendienteArs")
                            .value(respuesta.getSaldoPendienteArs()
                                    .doubleValue()))
                    .andExpect(jsonPath("$.reembolsoPendienteArs")
                            .value(respuesta.getReembolsoPendienteArs()
                                    .doubleValue()));
        }

        verify(liquidacionAlquilerService, only())
                .consultarLiquidacion(50);
        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void liquidacionDenegadaPorElServicioConserva403() throws Exception {
        when(liquidacionAlquilerService.consultarLiquidacion(50))
                .thenThrow(new AccessDeniedException(
                        "No podés consultar la liquidación de un alquiler ajeno."
                ));

        mockMvc.perform(get(CONSULTA_LIQUIDACION))
                .andExpect(status().isForbidden());

        verify(liquidacionAlquilerService, only())
                .consultarLiquidacion(50);
        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void liquidacionInexistenteResponde404() throws Exception {
        when(liquidacionAlquilerService.consultarLiquidacion(50))
                .thenThrow(new EntityNotFoundException(
                        "El alquiler no existe."
                ));

        mockMvc.perform(get(CONSULTA_LIQUIDACION))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El alquiler no existe."));

        verify(liquidacionAlquilerService, only())
                .consultarLiquidacion(50);
        verifyNoInteractions(alquilerService);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @WithMockUser(authorities = "CLIENTE")
    void liquidacionConIdNoPositivoResponde400(int id) throws Exception {
        when(liquidacionAlquilerService.consultarLiquidacion(id))
                .thenThrow(new IllegalArgumentException(
                        "Debe indicar un alquiler válido."
                ));

        mockMvc.perform(
                        get("/api/alquileres/{id}/liquidacion", id)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Debe indicar un alquiler válido."
                ));

        verify(liquidacionAlquilerService, only())
                .consultarLiquidacion(id);
        verifyNoInteractions(alquilerService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "2147483648"})
    @WithMockUser(authorities = "CLIENTE")
    void liquidacionConIdNoConvertibleResponde400(String id)
            throws Exception {

        mockMvc.perform(
                        get("/api/alquileres/{id}/liquidacion", id)
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(
                liquidacionAlquilerService,
                alquilerService
        );
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void liquidacionDelCircuitoAnteriorResponde409() throws Exception {
        when(liquidacionAlquilerService.consultarLiquidacion(50))
                .thenThrow(new IllegalStateException(
                        "Este alquiler pertenece al circuito anterior."
                ));

        mockMvc.perform(get(CONSULTA_LIQUIDACION))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Este alquiler pertenece al circuito anterior."
                ));

        verify(liquidacionAlquilerService, only())
                .consultarLiquidacion(50);
        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void liquidacionConFalloInternoResponde500SinExponerDetalles()
            throws Exception {

        when(liquidacionAlquilerService.consultarLiquidacion(50))
                .thenThrow(new ConfiguracionSistemaException(
                        "DETALLE_INTERNO_NO_PUBLICAR"
                ));

        mockMvc.perform(get(CONSULTA_LIQUIDACION))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "No se pudo completar la operación por un problema interno. "
                                + "Intentá nuevamente más tarde."
                ));

        verify(liquidacionAlquilerService, only())
                .consultarLiquidacion(50);
        verifyNoInteractions(alquilerService);
    }

    private LiquidacionAlquilerDTO crearRespuestaLiquidacion(
            String situacion
    ) {
        BigDecimal costoUsd = null;
        BigDecimal costoArs = null;
        BigDecimal saldo = null;
        BigDecimal reembolso = null;

        switch (situacion) {
            case "SIN_LIQUIDAR":
                break;

            case "SALDO_PENDIENTE":
                costoUsd = new BigDecimal("18.00");
                costoArs = new BigDecimal("27540.00");
                saldo = new BigDecimal("6885.00");
                reembolso = new BigDecimal("0.00");
                break;

            case "REEMBOLSO_PENDIENTE":
                costoUsd = new BigDecimal("9.00");
                costoArs = new BigDecimal("13770.00");
                saldo = new BigDecimal("0.00");
                reembolso = new BigDecimal("6885.00");
                break;

            case "SALDADO":
                costoUsd = new BigDecimal("13.50");
                costoArs = new BigDecimal("20655.00");
                saldo = new BigDecimal("0.00");
                reembolso = new BigDecimal("0.00");
                break;

            default:
                throw new IllegalArgumentException(
                        "Situación de prueba desconocida."
                );
        }

        return new LiquidacionAlquilerDTO(
                50,
                3,
                situacion,
                costoArs == null
                        ? null
                        : LocalDateTime.of(2026, 11, 16, 7, 30, 45),
                costoUsd,
                costoArs,
                new BigDecimal("20655.00"),
                new BigDecimal("0.00"),
                new BigDecimal("20655.00"),
                saldo,
                reembolso
        );
    }

    private void prepararRespuestaListado(
            String ruta,
            List<AlquilerCabeceraDTO> respuesta
    ) {
        if (LISTADO_PROPIO.equals(ruta)) {
            when(alquilerService.listarMisAlquileres())
                    .thenReturn(respuesta);
        } else {
            when(alquilerService.listarAlquileresAdministracion())
                    .thenReturn(respuesta);
        }
    }

    private void verificarConsultaListado(String ruta) {
        if (LISTADO_PROPIO.equals(ruta)) {
            verify(alquilerService).listarMisAlquileres();
        } else {
            verify(alquilerService).listarAlquileresAdministracion();
        }

        verifyNoMoreInteractions(alquilerService);
    }

    private void comprobarConsultaPermitida() throws Exception {
        when(alquilerService.obtenerAlquiler(50))
                .thenReturn(crearRespuesta());

        mockMvc.perform(
                        get("/api/alquileres/50")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.estado").value("RESERVADO"))
                .andExpect(jsonPath("$.estacionRetiroId").value(1))
                .andExpect(jsonPath("$.estacionRetiro").value("Plaza QA"))
                .andExpect(jsonPath("$.fechaHoraCreacion")
                        .value("2026-11-10T09:00:00-03:00"))
                .andExpect(jsonPath("$.fechaHoraVencimientoReserva")
                        .value("2026-11-10T09:15:00-03:00"))
                .andExpect(jsonPath("$.cotizacionArsPorUsd")
                        .value(1530.0))
                .andExpect(jsonPath("$.fuenteCotizacion")
                        .value("Fuente histórica QA"))
                .andExpect(jsonPath("$.costoEstimadoUsd").value(15.0))
                .andExpect(jsonPath("$.costoEstimadoArs").value(22950.0))
                .andExpect(jsonPath("$.costoFinalUsd").value(nullValue()))
                .andExpect(jsonPath("$.fechaHoraLiquidacion")
                        .value(nullValue()))

                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].id").value(101))
                .andExpect(jsonPath("$.detalles[0].version").value(2))
                .andExpect(jsonPath("$.detalles[0].bicicletaId").value(1))
                .andExpect(jsonPath("$.detalles[0].codigoBicicleta")
                        .value("QA-HTTP-01"))
                .andExpect(jsonPath("$.detalles[0].tipoBicicletaContratado")
                        .value("MONTANA"))
                .andExpect(jsonPath("$.detalles[0].estado")
                        .value("RESERVADO"))
                .andExpect(jsonPath("$.detalles[0].precioPorHoraAplicadoUsd")
                        .value(10.0))
                .andExpect(jsonPath("$.detalles[0].duracionEstimadaSegundos")
                        .value(5400))
                .andExpect(jsonPath("$.detalles[0].subtotalEstimadoUsd")
                        .value(15.0))
                .andExpect(jsonPath("$.detalles[0].subtotalEstimadoArs")
                        .value(22950.0))
                .andExpect(jsonPath("$.detalles[0].fechaHoraRetiro")
                        .value(nullValue()))
                .andExpect(jsonPath("$.detalles[0].subtotalRealUsd")
                        .value(nullValue()))

                // No devolver las entidades completas.
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.detalles[0].bicicleta")
                        .doesNotExist());

        verify(alquilerService).obtenerAlquiler(50);
        verifyNoMoreInteractions(alquilerService);
    }

    private AlquilerCabeceraDTO crearRespuesta() {
        Usuario usuario = new Usuario();
        usuario.setId(1);

        Estacion estacion = new Estacion(
                "Plaza QA", "Calle QA", 10, new BigDecimal("0.0"), new BigDecimal("0.0")
        );
        estacion.setEsPublica(true);
        ReflectionTestUtils.setField(estacion, "id", 1);

        TipoBicicleta tipo = new TipoBicicleta(
                "MONTANA", new BigDecimal("90.00"), 3
        );
        ReflectionTestUtils.setField(tipo, "id", 1);

        Bicicleta bicicleta = new Bicicleta(
                "QA-HTTP-01",
                tipo,
                new EstadoBicicleta("RESERVADA"),
                estacion
        );
        bicicleta.setId(1);

        LocalDateTime creacion =
                LocalDateTime.of(2026, 11, 10, 12, 0);

        Alquiler alquiler = new Alquiler();
        alquiler.setId(50);
        ReflectionTestUtils.setField(alquiler, "version", 3);
        alquiler.setUsuario(usuario);
        alquiler.setEstadoContratacion(
                new EstadoAlquiler("RESERVADO")
        );
        alquiler.setUbicacionRetiro(estacion);
        alquiler.setFechaHoraCreacion(creacion);
        alquiler.setFechaHoraVencimientoReserva(
                creacion.plusMinutes(15)
        );
        alquiler.setCotizacionArsPorUsd(
                new BigDecimal("1530.00")
        );
        alquiler.setFuenteCotizacion("Fuente histórica QA");
        alquiler.setFechaHoraAplicacionCotizacion(creacion);
        alquiler.setCostoEstimadoUsd(new BigDecimal("15.00"));
        alquiler.setCostoEstimadoArs(new BigDecimal("22950.00"));

        AlquilerDetalle detalle = new AlquilerDetalle();
        ReflectionTestUtils.setField(detalle, "id", 101);
        ReflectionTestUtils.setField(detalle, "version", 2);

        detalle.setAlquiler(alquiler);
        detalle.setBicicleta(bicicleta);
        detalle.setTipoBicicletaContratado(tipo);
        detalle.setEstado(new EstadoAlquilerDetalle("RESERVADO"));
        detalle.setPrecioPorHoraAplicadoUsd(
                new BigDecimal("10.00")
        );
        detalle.setDuracionEstimadaSegundos(5400);
        detalle.setSubtotalEstimadoUsd(new BigDecimal("15.00"));
        detalle.setSubtotalEstimadoArs(new BigDecimal("22950.00"));

        return new AlquilerCabeceraDTO(
                alquiler, List.of(detalle)
        );
    }
}
