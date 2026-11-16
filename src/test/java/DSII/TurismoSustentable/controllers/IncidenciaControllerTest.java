package DSII.TurismoSustentable.controllers;
import DSII.TurismoSustentable.configurations.*;
import DSII.TurismoSustentable.services.IncidenciaService;
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
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(IncidenciaController.class)
@ContextConfiguration(classes={IncidenciaController.class,WebAuthorization.class,SecurityConfig.class,GlobalExceptionHandler.class})
@ActiveProfiles("refactor")
class IncidenciaControllerTest {
    @Autowired MockMvc http;@MockBean IncidenciaService servicio;@MockBean UserDetailsService usuarios;
    static final String REPORTE="{\"alquilerId\":1,\"detalleId\":2,\"versionDetalle\":0,\"tipo\":\"AVERIA\",\"descripcion\":\"Freno dañado\",\"fechaHoraHechoInformada\":\"2026-11-16T10:45:00-03:00\"}";
    static final String DECISION="{\"version\":0,\"versionBicicleta\":0,\"decision\":\"VALIDADA\",\"motivo\":\"Revisado\",\"fechaHoraCorteValidado\":\"2026-11-16T13:45:00Z\"}";
    static final String RECEPCION="{\"version\":0,\"versionBicicleta\":0,\"estacionId\":5,\"motivo\":\"Recuperación\",\"recepcionFisicaConfirmada\":true}";
    static final String PERDIDA="{\"version\":0,\"versionBicicleta\":0,\"motivo\":\"Búsqueda concluida\",\"perdidaDefinitivaConfirmada\":true}";
    @ParameterizedTest @CsvSource({"ADMIN,200","CLIENTE,200","TECNICO,403"})
    void reporteSegunRol(String rol,int esperado)throws Exception{
        http.perform(post("/api/incidencias").with(user("qa@test.com").authorities(new SimpleGrantedAuthority(rol))).contentType("application/json").content(REPORTE)).andExpect(status().is(esperado));
        if(esperado==200)verify(servicio).reportar(any());else verifyNoInteractions(servicio);
    }
    @ParameterizedTest @CsvSource({"ADMIN,200","CLIENTE,200","TECNICO,403"})
    void lecturaSegunRol(String rol,int esperado)throws Exception{
        var usuario=user("qa@test.com").authorities(new SimpleGrantedAuthority(rol));
        http.perform(get("/api/incidencias").with(usuario)).andExpect(status().is(esperado));
        http.perform(get("/api/incidencias/7").with(usuario)).andExpect(status().is(esperado));
        http.perform(get("/api/incidencias/7/historial").with(usuario)).andExpect(status().is(esperado));
        if(esperado==403)verifyNoInteractions(servicio);
    }
    @ParameterizedTest @CsvSource({"ADMIN,resolver,200","ADMIN,recibir,200","ADMIN,baja-perdida,200","ADMIN,eliminar-perdida,200",
        "CLIENTE,resolver,403","CLIENTE,recibir,403","CLIENTE,baja-perdida,403","CLIENTE,eliminar-perdida,403",
        "TECNICO,resolver,403","TECNICO,recibir,403","TECNICO,baja-perdida,403","TECNICO,eliminar-perdida,403",
        "ADMIN,ubicar,200","CLIENTE,ubicar,403","TECNICO,ubicar,403"})
    void comandosAdministrativos(String rol,String accion,int esperado)throws Exception{
        String cuerpo=accion.equals("resolver")?DECISION:Set.of("recibir","ubicar").contains(accion)?RECEPCION:PERDIDA;
        http.perform(post("/api/incidencias/7/"+accion).with(user("qa@test.com").authorities(new SimpleGrantedAuthority(rol))).contentType("application/json").content(cuerpo)).andExpect(status().is(esperado));
        if(esperado==403)verifyNoInteractions(servicio);
    }
    @Test void visitanteRecibe401YLaPaginaRedirigeAlIngreso()throws Exception{
        http.perform(get("/api/incidencias")).andExpect(status().isUnauthorized());
        http.perform(post("/api/incidencias").contentType("application/json").content(REPORTE)).andExpect(status().isUnauthorized());
        http.perform(get("/incidencias.html").servletPath("/incidencias.html")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/index.html?sesion=requerida#login-anchor"));verifyNoInteractions(servicio);
    }
    @Test void tecnicoNoEntraALaPaginaPrivada()throws Exception{
        http.perform(get("/incidencias.html").servletPath("/incidencias.html").with(user("tec@test.com").authorities(new SimpleGrantedAuthority("TECNICO")))).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/index.html?acceso=denegado#login-anchor"));
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"alquilerId\":1,\"detalleId\":2,\"versionDetalle\":0,\"tipo\":\"OTRO\",\"descripcion\":\"QA\",\"fechaHoraHechoInformada\":\"2026-11-16T13:00:00Z\"}",
        "{\"alquilerId\":1,\"detalleId\":2,\"versionDetalle\":0,\"tipo\":\"AVERIA\",\"descripcion\":\"QA\",\"fechaHoraHechoInformada\":\"2026-11-16T13:00:00\"}",
        "{\"alquilerId\":1,\"detalleId\":2,\"versionDetalle\":0,\"tipo\":\"AVERIA\",\"descripcion\":\"QA\",\"fechaHoraHechoInformada\":\"2026-11-16T13:00:00Z\",\"estadoRevision\":\"VALIDADA\"}"})
    void datosIncorrectosNoLleganAlServicio(String cuerpo)throws Exception{
        http.perform(post("/api/incidencias").with(user("qa@test.com").authorities(new SimpleGrantedAuthority("CLIENTE"))).contentType("application/json").content(cuerpo)).andExpect(status().isBadRequest());verifyNoInteractions(servicio);
    }
    @Test void confirmacionFalsaNoPermiteRecepcion()throws Exception{
        http.perform(post("/api/incidencias/7/recibir").with(user("admin@test.com").authorities(new SimpleGrantedAuthority("ADMIN"))).contentType("application/json").content(RECEPCION.replace("true","false"))).andExpect(status().isBadRequest());verifyNoInteractions(servicio);
    }
    @ParameterizedTest @ValueSource(ints={400,404,409})
    void conservaEstadoDelCasoDeUso(int codigo)throws Exception{
        when(servicio.resolver(eq(7),any())).thenThrow(new ResponseStatusException(HttpStatus.valueOf(codigo),"Aviso legible"));
        http.perform(post("/api/incidencias/7/resolver").with(user("admin@test.com").authorities(new SimpleGrantedAuthority("ADMIN"))).contentType("application/json").content(DECISION)).andExpect(status().is(codigo)).andExpect(content().string("Aviso legible"));
    }
    @Test void noExponeConfiguracionInterna()throws Exception{
        when(servicio.consultar(7)).thenThrow(new ConfiguracionSistemaException("secreto interno QA"));
        http.perform(get("/api/incidencias/7").with(user("admin@test.com").authorities(new SimpleGrantedAuthority("ADMIN")))).andExpect(status().isInternalServerError()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secreto"))));
    }
}
