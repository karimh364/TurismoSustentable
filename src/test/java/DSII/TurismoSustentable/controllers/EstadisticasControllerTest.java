package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.services.AlquilerService;
import DSII.TurismoSustentable.services.EstadisticasService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@ContextConfiguration(classes = {
        AdminController.class,
        WebAuthorization.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class EstadisticasControllerTest {

    private static final String RESUMEN =
            "/api/admin/estadisticas/resumen";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EstadisticasService estadisticasService;

    @MockBean
    private AlquilerService alquilerService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(authorities = "ADMIN")
    void administradorRecibeResumenConFormatoActual() throws Exception {
        comprobarResumenConDatos();
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "CLIENTE"})
    void administradorConRolClienteTambienPuedeConsultar() throws Exception {
        comprobarResumenConDatos();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "TECNICO"})
    void otrosRolesNoAccedenAlResumen(String rol) throws Exception {
        mockMvc.perform(get(RESUMEN).with(
                        user("qa@example.com").authorities(
                                new SimpleGrantedAuthority(rol)
                        )
                ))
                .andExpect(status().isForbidden());

        verifyNoInteractions(estadisticasService, alquilerService);
    }

    @Test
    void visitanteSinSesionRecibe401() throws Exception {
        mockMvc.perform(get(RESUMEN))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(estadisticasService, alquilerService);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void sinDatosDevuelveListasVaciasYContadorCero() throws Exception {
        when(estadisticasService.getPreferenciasBicicletaPorGenero())
                .thenReturn(List.of());
        when(estadisticasService.getBicicletasAlquiladasActuales())
                .thenReturn(0L);
        when(estadisticasService.getBicicletasPorEstado())
                .thenReturn(List.of());

        mockMvc.perform(get(RESUMEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferencias_por_genero_y_tipo").isEmpty())
                .andExpect(jsonPath("$.bicicletas_alquiladas_actualmente").value(0))
                .andExpect(jsonPath("$.estados_de_bicicletas").isEmpty());

        verifyNoInteractions(alquilerService);
    }

    private void comprobarResumenConDatos() throws Exception {
        when(estadisticasService.getPreferenciasBicicletaPorGenero())
                .thenReturn(List.<Object[]>of(
                        new Object[]{"MONTANA", "MASCULINO", 3L}
                ));
        when(estadisticasService.getBicicletasAlquiladasActuales())
                .thenReturn(2L);
        when(estadisticasService.getBicicletasPorEstado())
                .thenReturn(List.<Object[]>of(
                        new Object[]{"EN_USO", 2L}
                ));

        mockMvc.perform(get(RESUMEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferencias_por_genero_y_tipo[0][0]")
                        .value("MONTANA"))
                .andExpect(jsonPath("$.preferencias_por_genero_y_tipo[0][1]")
                        .value("MASCULINO"))
                .andExpect(jsonPath("$.preferencias_por_genero_y_tipo[0][2]")
                        .value(3))
                .andExpect(jsonPath("$.bicicletas_alquiladas_actualmente")
                        .value(2))
                .andExpect(jsonPath("$.estados_de_bicicletas[0][0]")
                        .value("EN_USO"))
                .andExpect(jsonPath("$.estados_de_bicicletas[0][1]")
                        .value(2));

        verify(estadisticasService).getPreferenciasBicicletaPorGenero();
        verify(estadisticasService).getBicicletasAlquiladasActuales();
        verify(estadisticasService).getBicicletasPorEstado();
        verifyNoInteractions(alquilerService);
    }
}
