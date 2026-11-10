package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.dto.AlquilerCabeceraDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.services.AlquilerService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlquilerController.class)
@ContextConfiguration(classes = {
        AlquilerController.class,
        WebAuthorization.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class ConsultaAlquilerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlquilerService alquilerService;

    @MockBean
    private AlquilerRepository alquilerRepository;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @AfterEach
    void consultaDelegaSinAccederDirectamenteARepositorios() {
        verifyNoInteractions(
                alquilerRepository,
                usuarioRepository
        );
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(get("/api/alquileres/50"))
                .andExpect(status().isUnauthorized());

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
                        .value("2026-11-10T12:00:00"))
                .andExpect(jsonPath("$.fechaHoraVencimientoReserva")
                        .value("2026-11-10T12:15:00"))
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
                "Plaza QA", "Calle QA", 10, 0.0, 0.0
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
