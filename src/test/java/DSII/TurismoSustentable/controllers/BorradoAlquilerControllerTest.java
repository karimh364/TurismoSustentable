package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.services.AlquilerService;
import DSII.TurismoSustentable.services.LiquidacionAlquilerService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import javax.persistence.EntityNotFoundException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlquilerController.class)
@ContextConfiguration(classes = {
        AlquilerController.class,
        WebAuthorization.class,
        SecurityConfig.class,
        GlobalExceptionHandler.class
})
class BorradoAlquilerControllerTest {

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

    @AfterEach
    void controladorNoAccedeDirectamenteALaBase() {
        verifyNoInteractions(
                alquilerRepository,
                usuarioRepository
        );
    }

    @Test
    void sinSesionResponde401() throws Exception {
        mockMvc.perform(delete("/api/alquileres/50"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeBorrar() throws Exception {
        mockMvc.perform(delete("/api/alquileres/50"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "TECNICO")
    void tecnicoNoPuedeBorrar() throws Exception {
        mockMvc.perform(delete("/api/alquileres/50"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(alquilerService);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void administradorRecibe204SiElServicioCompleta()
            throws Exception {
        mockMvc.perform(delete("/api/alquileres/50"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(alquilerService).borrarAlquilerLogico(50);
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void inexistenciaResponde404() throws Exception {
        doThrow(new EntityNotFoundException(
                "El alquiler con ID 50 no existe."
        )).when(alquilerService).borrarAlquilerLogico(50);

        mockMvc.perform(delete("/api/alquileres/50"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        "El alquiler con ID 50 no existe."
                ));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void estadoIncompatibleResponde409() throws Exception {
        doThrow(new IllegalStateException(
                "El alquiler continúa activo."
        )).when(alquilerService).borrarAlquilerLogico(50);

        mockMvc.perform(delete("/api/alquileres/50"))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "El alquiler continúa activo."
                ));
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void denegacionDelServicioNoSeConvierteEn404()
            throws Exception {
        doThrow(new AccessDeniedException(
                "Acceso denegado."
        )).when(alquilerService).borrarAlquilerLogico(50);

        mockMvc.perform(delete("/api/alquileres/50"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void falloInternoResponde500SinDetalleTecnico()
            throws Exception {
        doThrow(new ConfiguracionSistemaException(
                "DETALLE_INTERNO_NO_PUBLICAR"
        )).when(alquilerService).borrarAlquilerLogico(50);

        mockMvc.perform(delete("/api/alquileres/50"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "No se pudo completar la operación por un problema interno. "
                                + "Intentá nuevamente más tarde."
                ));
    }
}
