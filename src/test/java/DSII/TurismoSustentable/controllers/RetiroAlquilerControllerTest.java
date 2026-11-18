package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.security.SesionVigenteMockConfig;
import org.springframework.context.annotation.Import;
import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.dto.AlquilerDetalleDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.services.AlquilerService;
import DSII.TurismoSustentable.services.LiquidacionAlquilerService;

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

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlquilerController.class)
@ContextConfiguration(classes = {
        AlquilerController.class,
        WebAuthorization.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
@Import(SesionVigenteMockConfig.class)
class RetiroAlquilerControllerTest {

    private static final String RUTA =
            "/api/alquileres/7/detalles/31/retirar";

    private static final String PLANTILLA =
            "/api/alquileres/{alquilerId}/detalles/{detalleId}/retirar";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlquilerService alquilerService;

    @MockBean
    private LiquidacionAlquilerService liquidacionAlquilerService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void visitanteRecibe401SinEjecutarElServicio() throws Exception {
        mockMvc.perform(post(RUTA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(alquilerService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TECNICO"})
    void rolSinClienteRecibe403AntesDelServicio(String rol)
            throws Exception {

        mockMvc.perform(
                        post(RUTA).with(
                                user("qa.retiro").authorities(
                                        new SimpleGrantedAuthority(rol)
                                )
                        )
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(
            username = "qa.retiro@example.com",
            authorities = "CLIENTE"
    )
    void clienteRecibeElDetalleRetirado() throws Exception {
        comprobarRetiroPermitido();
    }

    @Test
    @WithMockUser(
            username = "qa.retiro@example.com",
            authorities = {"ADMIN", "CLIENTE"}
    )
    void administradorConClientePuedeRetirarLoPropio()
            throws Exception {

        comprobarRetiroPermitido();
    }

    @ParameterizedTest
    @CsvSource({
            "abc,31",
            "7,abc",
            "2147483648,31",
            "7,2147483648"
    })
    @WithMockUser(authorities = "CLIENTE")
    void identificadoresNoConvertiblesResponden400SinServicio(
            String alquilerId,
            String detalleId
    ) throws Exception {

        mockMvc.perform(post(PLANTILLA, alquilerId, detalleId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(alquilerService);
    }

    @ParameterizedTest
    @CsvSource({
            "0,31",
            "-1,31",
            "7,0",
            "7,-1"
    })
    @WithMockUser(authorities = "CLIENTE")
    void rechazoDeIdentificadoresNoPositivosSeConservaComo400(
            int alquilerId,
            int detalleId
    ) throws Exception {

        String mensaje =
                "Los identificadores del alquiler y la bicicleta "
                        + "del alquiler deben ser válidos.";

        when(alquilerService.retirarBicicleta(alquilerId, detalleId))
                .thenThrow(new IllegalArgumentException(mensaje));

        mockMvc.perform(post(PLANTILLA, alquilerId, detalleId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(mensaje));

        verify(alquilerService)
                .retirarBicicleta(alquilerId, detalleId);
        verifyNoMoreInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void detalleNoEncontradoResponde404() throws Exception {
        String mensaje =
                "La bicicleta indicada no pertenece a este alquiler.";

        when(alquilerService.retirarBicicleta(7, 31))
                .thenThrow(new EntityNotFoundException(mensaje));

        mockMvc.perform(post(RUTA))
                .andExpect(status().isNotFound())
                .andExpect(content().string(mensaje));

        verificarLlamadaAlRetiro();
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "CLIENTE"})
    void tenerAdminNoOcultaUnaDenegacionDelServicio()
            throws Exception {

        when(alquilerService.retirarBicicleta(7, 31))
                .thenThrow(new AccessDeniedException(
                        "No podés retirar bicicletas de un alquiler ajeno."
                ));

        mockMvc.perform(post(RUTA))
                .andExpect(status().isForbidden());

        verificarLlamadaAlRetiro();
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void reservaVencidaResponde409ConSuMensaje() throws Exception {
        String mensaje =
                "La reserva venció. Ya no podés retirar esta bicicleta.";

        when(alquilerService.retirarBicicleta(7, 31))
                .thenThrow(new IllegalStateException(mensaje));

        mockMvc.perform(post(RUTA))
                .andExpect(status().isConflict())
                .andExpect(content().string(mensaje));

        verificarLlamadaAlRetiro();
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void falloInternoResponde500SinExponerInformacionTecnica()
            throws Exception {

        when(alquilerService.retirarBicicleta(7, 31))
                .thenThrow(new ConfiguracionSistemaException(
                        "DETALLE_INTERNO_NO_PUBLICAR"
                ));

        mockMvc.perform(post(RUTA))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "No se pudo completar la operación por un problema interno. "
                                + "Intentá nuevamente más tarde."
                ));

        verificarLlamadaAlRetiro();
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void datosEnviadosEnElBodyNoSustituyenLaRespuestaDelServicio()
            throws Exception {

        when(alquilerService.retirarBicicleta(7, 31))
                .thenReturn(respuestaRetirada());

        mockMvc.perform(
                        post(RUTA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"usuarioId\":999,"
                                                + "\"estado\":\"DEVUELTO\","
                                                + "\"fechaHoraRetiro\":"
                                                + "\"2099-01-01T00:00:00\","
                                                + "\"precioPorHoraAplicadoUsd\":0.01}"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.fechaHoraRetiro")
                        .value("2026-11-15T12:05:00"))
                .andExpect(jsonPath("$.precioPorHoraAplicadoUsd")
                        .value(10.0));

        verificarLlamadaAlRetiro();
    }

    private void comprobarRetiroPermitido() throws Exception {
        when(alquilerService.retirarBicicleta(7, 31))
                .thenReturn(respuestaRetirada());

        mockMvc.perform(
                        post(RUTA).accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.bicicletaId").value(101))
                .andExpect(jsonPath("$.codigoBicicleta")
                        .value("QA-HTTP-RET-01"))
                .andExpect(jsonPath("$.tipoBicicletaContratado")
                        .value("MONTANA"))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.precioPorHoraAplicadoUsd")
                        .value(10.0))
                .andExpect(jsonPath("$.duracionEstimadaSegundos")
                        .value(2700))
                .andExpect(jsonPath("$.fechaHoraRetiro")
                        .value("2026-11-15T12:05:00"))
                .andExpect(jsonPath("$.fechaHoraDevolucionEstimada")
                        .value("2026-11-15T12:50:00"))
                .andExpect(jsonPath("$.fechaHoraDevolucionReal")
                        .value(nullValue()))
                .andExpect(jsonPath("$.fechaHoraFinUso")
                        .value(nullValue()))
                .andExpect(jsonPath("$.estacionDevolucionId")
                        .value(nullValue()))
                .andExpect(jsonPath("$.subtotalEstimadoUsd")
                        .value(7.5))
                .andExpect(jsonPath("$.subtotalEstimadoArs")
                        .value(11475.0))
                .andExpect(jsonPath("$.subtotalRealArs")
                        .value(nullValue()))
                .andExpect(jsonPath("$.fallaReportada").value(false))

                // No devolver entidades completas ni datos sensibles.
                .andExpect(jsonPath("$.alquiler").doesNotExist())
                .andExpect(jsonPath("$.bicicleta").doesNotExist())
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        verificarLlamadaAlRetiro();
    }

    private void verificarLlamadaAlRetiro() {
        verify(alquilerService).retirarBicicleta(7, 31);
        verifyNoMoreInteractions(alquilerService);
    }

    private AlquilerDetalleDTO respuestaRetirada() {
        LocalDateTime retiro =
                LocalDateTime.of(2026, 11, 15, 12, 5);

        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setActivo(true);

        Estacion origen = new Estacion(
                "Plaza QA", "Calle QA", 10, new BigDecimal("0.0"), new BigDecimal("0.0")
        );
        origen.setEsPublica(true);
        ReflectionTestUtils.setField(origen, "id", 1);

        // La tarifa actual difiere de la histórica del detalle.
        TipoBicicleta tipo = new TipoBicicleta(
                "MONTANA", new BigDecimal("90.00"), 3
        );
        ReflectionTestUtils.setField(tipo, "id", 1);

        Bicicleta bicicleta = new Bicicleta(
                "QA-HTTP-RET-01",
                tipo,
                new EstadoBicicleta("EN_USO"),
                null
        );
        bicicleta.setId(101);
        bicicleta.setFechaAlta(retiro.toLocalDate());

        Alquiler alquiler = new Alquiler();
        alquiler.setId(7);
        alquiler.setUsuario(usuario);
        alquiler.setUbicacionRetiro(origen);
        alquiler.setEstadoContratacion(
                new EstadoAlquiler("EN_PROGRESO")
        );
        alquiler.setFechaHoraCreacion(retiro.minusMinutes(5));
        alquiler.setFechaHoraVencimientoReserva(
                retiro.plusMinutes(10)
        );

        AlquilerDetalle detalle = new AlquilerDetalle();
        ReflectionTestUtils.setField(detalle, "id", 31);
        ReflectionTestUtils.setField(detalle, "version", 1);
        detalle.setAlquiler(alquiler);
        detalle.setBicicleta(bicicleta);
        detalle.setTipoBicicletaContratado(tipo);
        detalle.setEstado(new EstadoAlquilerDetalle("EN_CURSO"));
        detalle.setPrecioPorHoraAplicadoUsd(
                new BigDecimal("10.00")
        );
        detalle.setDuracionEstimadaSegundos(2700);
        detalle.setFechaHoraRetiro(retiro);
        detalle.setFechaHoraDevolucionEstimada(
                retiro.plusSeconds(2700)
        );
        detalle.setSubtotalEstimadoUsd(new BigDecimal("7.50"));
        detalle.setSubtotalEstimadoArs(new BigDecimal("11475.00"));

        return new AlquilerDetalleDTO(detalle);
    }
}
