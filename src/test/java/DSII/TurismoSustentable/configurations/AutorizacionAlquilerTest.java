package DSII.TurismoSustentable.configurations;

import DSII.TurismoSustentable.security.SesionVigenteMockConfig;
import org.springframework.context.annotation.Import;
import DSII.TurismoSustentable.controllers.AlquilerController;
import DSII.TurismoSustentable.dto.CrearAlquilerDTO;
import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.services.AlquilerService;
import DSII.TurismoSustentable.services.LiquidacionAlquilerService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlquilerController.class)
@ContextConfiguration(classes = {
        AlquilerController.class,
        WebAuthorization.class,
        SecurityConfig.class
})
@Import(SesionVigenteMockConfig.class)
class AutorizacionAlquilerTest {

    private static final String SOLICITUD =
            "{\"estacionRetiroId\":1,\"bicicletaIds\":[1],\"duracionEstimadaSegundos\":3600}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlquilerService alquilerService;

    @MockBean
    private LiquidacionAlquilerService liquidacionAlquilerService;

    @MockBean
    private AlquilerRepository alquilerRepository;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void sinSesionNoPuedeContratar() throws Exception {
        mockMvc.perform(post("/api/alquileres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void administradorSinClienteNoPuedeContratar() throws Exception {
        mockMvc.perform(post("/api/alquileres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isForbidden());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void tecnicoSinClienteNoPuedeContratar() throws Exception {
        mockMvc.perform(post("/api/alquileres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isForbidden());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clientePuedeAccederALaCreacion() throws Exception {
        comprobarCreacionPermitida();
    }

    @Test
    @WithMockUser(authorities = {"ADMIN", "CLIENTE"})
    void administradorConClienteAccedePorSuRolCliente() throws Exception {
        comprobarCreacionPermitida();
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void denegacionDelServicioSeConservaComo403() throws Exception {
        when(alquilerService.crearReserva(any(CrearAlquilerDTO.class)))
                .thenThrow(new AccessDeniedException(
                        "El usuario ya no tiene rol CLIENTE."
                ));

        mockMvc.perform(post("/api/alquileres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void deudaDelServicioSeConservaComo409() throws Exception {
        when(alquilerService.crearReserva(any(CrearAlquilerDTO.class)))
                .thenThrow(new IllegalStateException("Saldo pendiente."));

        mockMvc.perform(post("/api/alquileres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isConflict());
    }

    private void comprobarCreacionPermitida() throws Exception {
        when(alquilerService.crearReserva(any(CrearAlquilerDTO.class))).thenReturn(7);
        // Servicio simulado: se prueba acceso y delegación, no un alquiler real.
        mockMvc.perform(post("/api/alquileres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SOLICITUD))
                .andExpect(status().isCreated());

        verify(alquilerService)
                .crearReserva(any(CrearAlquilerDTO.class));
        verify(alquilerService).obtenerAlquiler(7);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void payloadAnteriorNoPermiteSaltarReservaYAnticipo() throws Exception {
        mockMvc.perform(post("/api/alquileres").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bicicletaId\":1,\"duracionEnHoras\":1}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(alquilerService);
    }
}
