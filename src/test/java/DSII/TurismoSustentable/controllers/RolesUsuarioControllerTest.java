package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.security.SesionVigenteMockConfig;
import DSII.TurismoSustentable.services.RolesUsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RolesUsuarioController.class)
@ContextConfiguration(classes = {RolesUsuarioController.class, WebAuthorization.class,
        SecurityConfig.class, GlobalExceptionHandler.class})
@Import(SesionVigenteMockConfig.class)
class RolesUsuarioControllerTest {
    private static final String RUTA = "/api/usuarios/42/roles";
    private static final String JSON = "{\"version\":3,\"roles\":[\"CLIENTE\",\"TECNICO\"],\"motivo\":\" Prueba QA \"}";
    @Autowired MockMvc http;
    @MockBean RolesUsuarioService servicio;
    @MockBean UserDetailsService credenciales;

    @Test void anonimoNoConsultaNiModificaRoles() throws Exception {
        http.perform(get(RUTA)).andExpect(status().isUnauthorized());
        http.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON)).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }
    @Test @WithMockUser(authorities = "CLIENTE")
    void clienteNoGestionaRoles() throws Exception {
        http.perform(get(RUTA)).andExpect(status().isForbidden());
        http.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON)).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }
    @Test @WithMockUser(authorities = "TECNICO")
    void tecnicoNoGestionaRoles() throws Exception {
        http.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON)).andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }
    @Test @WithMockUser(authorities = "ADMIN")
    void consultaDevuelveVistaEspecificaConVersionSinCredenciales() throws Exception {
        when(servicio.consultar(42)).thenReturn(vista());
        http.perform(get(RUTA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(42)).andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.roles[0]").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.persona").doesNotExist());
    }
    @Test @WithMockUser(authorities = {"ADMIN", "CLIENTE"})
    void guardaComposicionVersionYMotivoSinAceptarActorDesdeElCliente() throws Exception {
        when(servicio.actualizar(eq(42), any())).thenReturn(vista());
        http.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(4));
        verify(servicio).actualizar(eq(42), argThat(d -> d.getVersion() == 3
                && d.getRoles().size() == 2 && "Prueba QA".equals(d.getMotivo())));
    }
    @Test @WithMockUser(authorities = "ADMIN")
    void invalidoNoLlegaAlServicio() throws Exception {
        for (String json : new String[]{
                "{\"roles\":[\"CLIENTE\"],\"motivo\":\"QA\"}",
                "{\"version\":-1,\"roles\":[\"CLIENTE\"],\"motivo\":\"QA\"}",
                "{\"version\":0,\"roles\":[],\"motivo\":\"QA\"}",
                "{\"version\":0,\"roles\":[\"CLIENTE\",\"CLIENTE\"],\"motivo\":\"QA\"}",
                "{\"version\":0,\"roles\":[\"ROOT\"],\"motivo\":\"QA\"}",
                "{\"version\":0,\"roles\":[\"CLIENTE\"],\"motivo\":\" \"}",
                "{\"version\":0,\"roles\":[\"CLIENTE\"],\"motivo\":\"QA\",\"actorId\":99}"
        }) {
            http.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(servicio);
    }
    @Test @WithMockUser(authorities = "ADMIN")
    void conflictosDeNegocioYVersionConservan409YElMensaje() throws Exception {
        when(servicio.actualizar(eq(42), any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Hay órdenes abiertas."));
        http.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isConflict()).andExpect(content().string("Hay órdenes abiertas."));
    }
    @Test @WithMockUser(authorities = "ADMIN")
    void conflictoDePersistenciaNoSeAnunciaComoExito() throws Exception {
        when(servicio.actualizar(eq(42), any())).thenThrow(new OptimisticLockingFailureException("QA"));
        http.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(JSON)).andExpect(status().isConflict());
    }
    @Test @WithMockUser(authorities = "ADMIN")
    void rolRevocadoEnServicioSigueSiendo403() throws Exception {
        when(servicio.consultar(42)).thenThrow(new AccessDeniedException("ADMIN revocado"));
        http.perform(get(RUTA)).andExpect(status().isForbidden());
    }
    @Test @WithMockUser(authorities = "ADMIN")
    void cuentaAusenteConserva404YExplicacion() throws Exception {
        when(servicio.consultar(42)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe."));
        http.perform(get(RUTA)).andExpect(status().isNotFound()).andExpect(content().string("El usuario no existe."));
    }
    private RolesUsuarioDTO vista() {
        Usuario u = new Usuario(); u.setId(42); u.setNickname("roles.qa");
        ReflectionTestUtils.setField(u, "version", 4);
        Rol r = new Rol("CLIENTE"); ReflectionTestUtils.setField(r, "id", 2);
        u.getUsuarioRoles().add(new UsuarioRol(u, r));
        return new RolesUsuarioDTO(u);
    }
}
