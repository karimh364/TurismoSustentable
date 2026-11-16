package DSII.TurismoSustentable.controllers;
import DSII.TurismoSustentable.configurations.*;
import DSII.TurismoSustentable.services.MantenimientoService;
import DSII.TurismoSustentable.exceptions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(MantenimientoController.class)
@ContextConfiguration(classes={MantenimientoController.class,WebAuthorization.class,SecurityConfig.class,GlobalExceptionHandler.class})
@ActiveProfiles("refactor")
class MantenimientoControllerTest {
    @Autowired MockMvc http;
    @MockBean MantenimientoService servicio;
    @MockBean UserDetailsService usuarios;
    @ParameterizedTest @CsvSource({
        "ADMIN,iniciar,403","ADMIN,reanudar,403","ADMIN,pausar,403","ADMIN,trabajo,403","ADMIN,finalizar,403",
        "TECNICO,iniciar,200","TECNICO,reanudar,200","TECNICO,pausar,200","TECNICO,trabajo,200","TECNICO,finalizar,200",
        "ADMIN,asignar,200","ADMIN,cancelar,200","ADMIN,habilitar,200",
        "TECNICO,asignar,403","TECNICO,cancelar,403","TECNICO,habilitar,403",
        "CLIENTE,iniciar,403","CLIENTE,asignar,403","CLIENTE,habilitar,403"})
    void permisosPorOperacion(String rol,String accion,int esperado)throws Exception{
        http.perform(post("/api/mantenimiento/ordenes/7/"+accion).with(user("qa@example.com").authorities(new SimpleGrantedAuthority(rol)))
                .contentType("application/json").content("{\"version\":0}")).andExpect(status().is(esperado));
        if(esperado==200)verify(servicio).operar(eq(7),eq(accion),any());else verifyNoInteractions(servicio);
    }
    @ParameterizedTest @CsvSource({"ADMIN,200","TECNICO,200","CLIENTE,403"})
    void lecturaOrdenesSegunRol(String rol,int esperado)throws Exception{
        http.perform(get("/api/mantenimiento/ordenes").with(user("qa@example.com").authorities(new SimpleGrantedAuthority(rol)))).andExpect(status().is(esperado));
        if(esperado==200)verify(servicio).listar();else verifyNoInteractions(servicio);
    }
    @Test void visitanteSinSesionRecibe401()throws Exception{
        http.perform(get("/api/mantenimiento/ordenes")).andExpect(status().isUnauthorized());
        http.perform(post("/api/mantenimiento/ordenes/7/iniciar").contentType("application/json").content("{\"version\":0}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(servicio);
    }
    @ParameterizedTest @ValueSource(strings={"{\"version\":-1}","{}","{\"version\":0,\"estado\":\"FINALIZADA\"}","{\"version\":0,\"costoRepuestos\":-1}","{\"version\":0,\"costoRepuestos\":1.001}"})
    void payloadInvalidoNoLlegaAlServicio(String cuerpo)throws Exception{
        http.perform(post("/api/mantenimiento/ordenes/7/trabajo").with(user("qa@example.com").authorities(new SimpleGrantedAuthority("TECNICO")))
                .contentType("application/json").content(cuerpo)).andExpect(status().isBadRequest());verifyNoInteractions(servicio);
    }
    @Test void crea201SinPermitirQueClienteOTecnicoAdministreCatalogos()throws Exception{
        var admin=user("admin@example.com").authorities(new SimpleGrantedAuthority("ADMIN"));
        http.perform(post("/api/mantenimiento/ordenes").with(admin).contentType("application/json")
                .content("{\"bicicletaId\":1,\"tipo\":\"PREVENTIVO\",\"motivo\":\"Revisión\"}")).andExpect(status().isCreated());
        verify(servicio).crear(any());
        http.perform(put("/api/mantenimiento/tipos-bicicleta/1/intervalo").with(user("tec@example.com").authorities(new SimpleGrantedAuthority("TECNICO")))
                .contentType("application/json").content("{\"mesesAnteriores\":3,\"meses\":4,\"motivo\":\"Cambio\"}")).andExpect(status().isForbidden());
    }
    @Test void conserva409YNoExponeConfiguracionInterna()throws Exception{
        when(servicio.operar(eq(7),eq("cancelar"),any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT,"Recargá la orden."));
        var admin=user("admin@example.com").authorities(new SimpleGrantedAuthority("ADMIN"));
        http.perform(post("/api/mantenimiento/ordenes/7/cancelar").with(admin).contentType("application/json").content("{\"version\":0,\"motivo\":\"QA\"}"))
                .andExpect(status().isConflict()).andExpect(content().string("Recargá la orden."));
        when(servicio.consultar(7)).thenThrow(new ConfiguracionSistemaException("Dato interno secreto QA"));
        http.perform(get("/api/mantenimiento/ordenes/7").with(admin)).andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secreto"))));
    }
}
