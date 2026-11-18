package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.security.SesionVigenteMockConfig;
import org.springframework.context.annotation.Import;
import DSII.TurismoSustentable.configurations.*;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest({AlquilerController.class, PagoController.class})
@ContextConfiguration(classes={AlquilerController.class, PagoController.class, WebAuthorization.class, SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("refactor")
@Import(SesionVigenteMockConfig.class)
class OperacionesReservaControllerTest {
    @Autowired MockMvc http;
    @MockBean AlquilerService alquileres;
    @MockBean PagoService pagos;
    @MockBean ReembolsoService reintegros;
    @MockBean LiquidacionAlquilerService liquidaciones;
    @MockBean UserDetailsService usuarios;

    @ParameterizedTest
    @CsvSource({
        "/api/alquileres/7/cancelar,CLIENTE,200",
        "/api/alquileres/7/cancelar,ADMIN,403",
        "/api/alquileres/7/cancelar,TECNICO,403",
        "/api/alquileres/7/detalles/9/cancelar,CLIENTE,200",
        "/api/alquileres/7/detalles/9/cancelar,ADMIN,403",
        "/api/pagos/reembolsos/7/resolver,CLIENTE,200",
        "/api/pagos/reembolsos/7/resolver,ADMIN,403",
        "/api/pagos/reembolsos/7/resolver,TECNICO,403"
    })
    void escriturasRespetanRol(String ruta, String rol, int estado) throws Exception {
        http.perform(post(ruta).with(user("qa@example.com").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(rol))))
                .andExpect(status().is(estado));
        if (estado == 403) verifyNoInteractions(alquileres, pagos, reintegros);
    }
    @ParameterizedTest
    @CsvSource({"CLIENTE,200","ADMIN,200","TECNICO,403"})
    void lecturaReembolsoNoConcedeDerechoAPagar(String rol, int estado) throws Exception {
        http.perform(get("/api/pagos/reembolsos/7").with(user("qa@example.com").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(rol))))
                .andExpect(status().is(estado));
        if (estado == 200) verify(reintegros).consultar(7);
        else verifyNoInteractions(reintegros);
    }
    @Test void visitanteNoCancelaNiResuelve() throws Exception {
        http.perform(post("/api/alquileres/7/cancelar")).andExpect(status().isUnauthorized());
        http.perform(post("/api/pagos/reembolsos/7/resolver")).andExpect(status().isUnauthorized());
        verifyNoInteractions(alquileres, reintegros);
    }
    @Test void cancelacionAdministrativaValidaMotivoAntesDeInvocarServicio() throws Exception {
        var admin = user("admin@example.com").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ADMIN"));
        http.perform(post("/api/admin/alquileres/7/detalles/9/cancelar").with(admin).contentType("application/json")
                .content("{\"motivo\":\" \",\"estadoBicicleta\":\"DISPONIBLE\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(alquileres);
        http.perform(post("/api/admin/alquileres/7/detalles/9/cancelar").with(admin).contentType("application/json")
                .content("{\"motivo\":\"Falla operativa\",\"estadoBicicleta\":\"INACTIVA\"}")).andExpect(status().isOk());
        verify(alquileres).cancelarReserva(eq(7), eq(9), any(), eq(true));
    }
    @Test void conflictoReembolsoConserva409SinExponerExcepcion() throws Exception {
        when(reintegros.resolver(7)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Todavía no está liquidado."));
        http.perform(post("/api/pagos/reembolsos/7/resolver").with(user("qa@example.com").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("CLIENTE"))))
                .andExpect(status().isConflict()).andExpect(content().string("Todavía no está liquidado."));
    }
}
