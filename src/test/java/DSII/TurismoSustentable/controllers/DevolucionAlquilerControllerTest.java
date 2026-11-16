package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.dto.AlquilerDetalleDTO;
import DSII.TurismoSustentable.dto.DevolverBicicletaDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.services.AlquilerService;
import DSII.TurismoSustentable.services.LiquidacionAlquilerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlquilerController.class)
@ContextConfiguration(classes = {AlquilerController.class, WebAuthorization.class,
        SecurityConfig.class, GlobalExceptionHandler.class})
class DevolucionAlquilerControllerTest {

    private static final String RUTA = "/api/alquileres/7/detalles/31/devolver";
    private static final String CUERPO = "{\"estacionDevolucionId\":200}";

    @Autowired private MockMvc mockMvc;
    @MockBean private AlquilerService servicio;
    @MockBean private LiquidacionAlquilerService liquidacionAlquilerService;
    @MockBean private UserDetailsService userDetailsService;

    @Test
    void visitanteRecibe401() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void tecnicoNoPuedeRecibirPorEstaOperacion() throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "ADMIN"})
    void clienteYAdminAccedenConLasRestriccionesDelServicio(String rol) throws Exception {
        when(servicio.devolverBicicleta(eq(7), eq(31), any())).thenReturn(detalleDevuelto());
        mockMvc.perform(post(RUTA).with(user("qa@example.com").authorities(new SimpleGrantedAuthority(rol)))
                        .contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("estado").value("DEVUELTO"))
                .andExpect(jsonPath("estacionDevolucionId").value(200))
                .andExpect(jsonPath("subtotalRealArs").value(7650.00));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"estacionDevolucionId\":0}", "{\"estacionDevolucionId\":null}",
            "{\"estacionDevolucionId\":200,\"fallaReportada\":null}",
            "{\"estacionDevolucionId\":200,\"fallaReportada\":true}",
            "{\"estacionDevolucionId\":200,\"fallaReportada\":true,\"descripcionFalla\":\"   \"}",
            "{\"estacionDevolucionId\":200,\"descripcionFalla\":\"No frena\"}"
    })
    @WithMockUser(authorities = "CLIENTE")
    void validaElCuerpoAntesDeLlamarAlServicio(String cuerpo) throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("validationErrors").isArray());
        verifyNoInteractions(servicio);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void normalizaLaDescripcionAntesDeEntregarlaAlServicio() throws Exception {
        when(servicio.devolverBicicleta(eq(7), eq(31), any())).thenReturn(detalleDevuelto());
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"estacionDevolucionId\":200,\"fallaReportada\":true,\"descripcionFalla\":\"  No frena  \"}"))
                .andExpect(status().isOk());
        ArgumentCaptor<DevolverBicicletaDTO> dto = ArgumentCaptor.forClass(DevolverBicicletaDTO.class);
        verify(servicio).devolverBicicleta(eq(7), eq(31), dto.capture());
        assertEquals("No frena", dto.getValue().getDescripcionFalla());
        assertTrue(dto.getValue().getFallaReportada());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fechaHoraFinUso", "subtotalRealArs", "estado", "usuarioId"})
    @WithMockUser(authorities = "CLIENTE")
    void noAceptaFechasPreciosEstadosOActoresDelNavegador(String campo) throws Exception {
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estacionDevolucionId\":200,\"" + campo + "\":\"inventado\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FALTA", "AJENO", "CONFLICTO", "CONFIGURACION"})
    @WithMockUser(authorities = "CLIENTE")
    void conservaElEstadoHttpDelFallo(String caso) throws Exception {
        RuntimeException fallo;
        int esperado;
        switch (caso) {
            case "FALTA": fallo = new EntityNotFoundException("No existe"); esperado = 404; break;
            case "AJENO": fallo = new AccessDeniedException("Ajeno"); esperado = 403; break;
            case "CONFLICTO": fallo = new IllegalStateException("Sin espacios"); esperado = 409; break;
            default: fallo = new ConfiguracionSistemaException("CONFIG_INTERNA_QA"); esperado = 500;
        }
        when(servicio.devolverBicicleta(eq(7), eq(31), any())).thenThrow(fallo);
        String respuesta = mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().is(esperado)).andReturn().getResponse().getContentAsString();
        if (esperado == 500) assertFalse(respuesta.contains("CONFIG_INTERNA_QA"));
    }

    private AlquilerDetalleDTO detalleDevuelto() {
        AlquilerDetalle detalle = new AlquilerDetalle();
        ReflectionTestUtils.setField(detalle, "id", 31);
        TipoBicicleta tipo = new TipoBicicleta("PASEO", new BigDecimal("10.00"), 3);
        Bicicleta bicicleta = new Bicicleta("QA-DEV-01", tipo, new EstadoBicicleta("DISPONIBLE"), null);
        bicicleta.setId(101);
        detalle.setBicicleta(bicicleta);
        detalle.setTipoBicicletaContratado(tipo);
        detalle.setEstado(new EstadoAlquilerDetalle("DEVUELTO"));
        Estacion estacion = new Estacion("Destino QA", "Calle QA", 2, 0.0, 0.0);
        ReflectionTestUtils.setField(estacion, "id", 200);
        detalle.setEstacionDevolucion(estacion);
        detalle.setFechaHoraDevolucionReal(LocalDateTime.of(2026, 11, 16, 10, 30));
        detalle.setSubtotalRealUsd(new BigDecimal("5.00"));
        detalle.setSubtotalRealArs(new BigDecimal("7650.00"));
        return new AlquilerDetalleDTO(detalle);
    }
}
