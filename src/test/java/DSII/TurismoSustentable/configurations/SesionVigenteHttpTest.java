package DSII.TurismoSustentable.configurations;

import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.repositories.projections.AccesoUsuarioProjection;
import DSII.TurismoSustentable.security.AccesoSesionService;
import DSII.TurismoSustentable.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SesionVigenteHttpTest.EndpointsPrueba.class)
@ContextConfiguration(classes = {
        WebAuthorization.class,
        SecurityConfig.class,
        AccesoSesionService.class,
        SesionVigenteHttpTest.EndpointsPrueba.class
})
class SesionVigenteHttpTest {

    private static final String EMAIL = "sesion.qa@example.com";
    private static final String PERFIL = "/api/usuarios/actual";
    private static final String ADMIN = "/api/admin/sonda-vigencia";

    @Autowired
    private MockMvc http;

    @MockBean
    private UsuarioRepository usuarios;

    @MockBean
    private UserDetailsService credenciales;

    @Test
    void visitanteRecibe401SinConsultarCuentaNiCrearSesion()
            throws Exception {

        var resultado = http.perform(get(PERFIL))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertNull(resultado.getRequest().getSession(false));
        verifyNoInteractions(usuarios);
    }

    @Test
    void retirarAdminBloqueaLaSiguientePeticionPeroConservaCliente()
            throws Exception {

        MockHttpSession sesion = sesion("ADMIN", "CLIENTE");

        cuentaActiva("ADMIN", "CLIENTE");

        http.perform(get(ADMIN).session(sesion))
                .andExpect(status().isOk());

        // Simula que la BD ya no devuelve ADMIN para la misma cuenta.
        cuentaActiva("CLIENTE");

        http.perform(get(ADMIN).session(sesion))
                .andExpect(status().isForbidden());

        http.perform(get(PERFIL).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(17))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("CLIENTE"));

        http.perform(post("/api/alquileres").session(sesion))
                .andExpect(status().isCreated());

        assertFalse(sesion.isInvalid());
        verify(usuarios, times(4)).buscarAccesoVigentePorId(17);
    }

    @Test
    void adminSoloNoPuedeCrearAlquiler() throws Exception {
        cuentaActiva("ADMIN");
        MockHttpSession sesion = sesion("ADMIN");

        http.perform(get(ADMIN).session(sesion))
                .andExpect(status().isOk());

        http.perform(post("/api/alquileres").session(sesion))
                .andExpect(status().isForbidden());

        assertFalse(sesion.isInvalid());
    }

    @Test
    void cuentaMultirrolConservaAmbosPermisosAsignados()
            throws Exception {

        cuentaActiva("ADMIN", "CLIENTE");
        MockHttpSession sesion = sesion("ADMIN", "CLIENTE");

        http.perform(get(ADMIN).session(sesion))
                .andExpect(status().isOk());

        http.perform(post("/api/alquileres").session(sesion))
                .andExpect(status().isCreated());
    }

    @Test
    void misAlquileresRechazaAdminOTecnicoSinCerrarSesion() throws Exception {
        for (String rol : List.of("ADMIN", "TECNICO")) {
            cuentaActiva(rol);
            MockHttpSession sesion = sesion(rol);

            http.perform(get("/mis_alquileres.html").session(sesion))
                    .andExpect(status().isFound())
                    .andExpect(redirectedUrl(
                            "/index.html?acceso=denegado#login-anchor"
                    ));

            http.perform(get(PERFIL).session(sesion))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.roles[0]").value(rol));

            if ("ADMIN".equals(rol)) {
                http.perform(get("/admin.html").session(sesion))
                        .andExpect(status().isOk());
            }
            assertFalse(sesion.isInvalid());
        }
    }

    @Test
    void misAlquileresPermiteClienteYAdminConCliente() throws Exception {
        for (String[] roles : List.of(
                new String[]{"CLIENTE"}, new String[]{"ADMIN", "CLIENTE"}
        )) {
            cuentaActiva(roles);
            MockHttpSession sesion = sesion(roles);

            http.perform(get("/mis_alquileres.html").session(sesion))
                    .andExpect(status().isOk())
                    .andExpect(content().string("mis alquileres"));

            if (Arrays.asList(roles).contains("ADMIN")) {
                http.perform(get("/admin.html").session(sesion))
                        .andExpect(status().isOk());
            }
            assertFalse(sesion.isInvalid());
        }
    }

    @Test
    void retirarClienteBloqueaMisAlquileresPeroConservaAdmin() throws Exception {
        cuentaActiva("ADMIN", "CLIENTE");
        MockHttpSession sesion = sesion("ADMIN", "CLIENTE");

        http.perform(get("/mis_alquileres.html").session(sesion))
                .andExpect(status().isOk());

        cuentaActiva("ADMIN");

        http.perform(get("/mis_alquileres.html").session(sesion))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(
                        "/index.html?acceso=denegado#login-anchor"
                ));
        http.perform(get("/admin.html").session(sesion))
                .andExpect(status().isOk());
        http.perform(get(PERFIL).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
        assertFalse(sesion.isInvalid());
    }

    @Test
    void misAlquileresSinSesionRedirigeAlIngreso() throws Exception {
        http.perform(get("/mis_alquileres.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(
                        "/index.html?sesion=requerida#login-anchor"
                ));
        verifyNoInteractions(usuarios);
    }

    @Test
    void cuentaInactivadaPierdeSesionYRecibe401() throws Exception {
        when(usuarios.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(EMAIL, false, "ADMIN")
                ));

        MockHttpSession sesion = sesion("ADMIN");

        http.perform(get(ADMIN).session(sesion))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().maxAge("JSESSIONID", 0));

        assertTrue(sesion.isInvalid());
    }

    @Test
    void paginaPrivadaConCuentaInactivaRedirigeAlIngreso()
            throws Exception {

        when(usuarios.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(EMAIL, false, "ADMIN")
                ));

        MockHttpSession sesion = sesion("ADMIN");

        http.perform(get("/admin.html").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/index.html?sesion=requerida#login-anchor"
                ));

        assertTrue(sesion.isInvalid());
    }

    @Test
    void paginaPrivadaConRolRetiradoRedirigeSinCerrarSesion()
            throws Exception {

        cuentaActiva("CLIENTE");
        MockHttpSession sesion = sesion("ADMIN", "CLIENTE");

        http.perform(get("/admin.html").session(sesion))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/index.html?acceso=denegado#login-anchor"
                ));

        assertFalse(sesion.isInvalid());

        http.perform(get(PERFIL).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("CLIENTE"));
    }

    @Test
    void cuentaInexistenteNoConservaSuSesion() throws Exception {
        when(usuarios.buscarAccesoVigentePorId(17))
                .thenReturn(List.of());

        MockHttpSession sesion = sesion("CLIENTE");

        http.perform(get(PERFIL).session(sesion))
                .andExpect(status().isUnauthorized());

        assertTrue(sesion.isInvalid());
        verify(usuarios).buscarAccesoVigentePorId(17);
        verifyNoMoreInteractions(usuarios);
    }

    @Test
    void cambiarEmailExigeIngresarNuevamente() throws Exception {
        when(usuarios.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila("nuevo@example.com", true, "CLIENTE")
                ));

        MockHttpSession sesion = sesion("CLIENTE");

        http.perform(get(PERFIL).session(sesion))
                .andExpect(status().isUnauthorized());

        assertTrue(sesion.isInvalid());
        verify(usuarios).buscarAccesoVigentePorId(17);
        verifyNoMoreInteractions(usuarios);
    }

    @Test
    void identidadAntiguaSinIdNoPuedeConservarPermisos()
            throws Exception {

        User principalAnterior = new User(
                EMAIL,
                "",
                AuthorityUtils.createAuthorityList("ADMIN")
        );

        MockHttpSession sesion = guardarSesion(
                new UsernamePasswordAuthenticationToken(
                        principalAnterior,
                        null,
                        principalAnterior.getAuthorities()
                )
        );

        http.perform(get(ADMIN).session(sesion))
                .andExpect(status().isUnauthorized());

        assertTrue(sesion.isInvalid());
        verifyNoInteractions(usuarios);
    }

    @Test
    void cuentaSinRolesNoConservaAdminAnterior() throws Exception {
        cuentaActiva((String) null);
        MockHttpSession sesion = sesion("ADMIN");

        http.perform(get(ADMIN).session(sesion))
                .andExpect(status().isForbidden());

        assertFalse(sesion.isInvalid());
    }

    @Test
    void falloDeBaseNoCierraSesionYPermiteReintentar()
            throws Exception {

        MockHttpSession sesion = sesion("CLIENTE");

        when(usuarios.buscarAccesoVigentePorId(17))
                .thenThrow(new DataAccessResourceFailureException(
                        "DETALLE_INTERNO_SECRETO"
                ));

        var resultado = http.perform(get(PERFIL).session(sesion))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andReturn();

        assertFalse(sesion.isInvalid());
        assertFalse(resultado.getResponse().getContentAsString()
                .contains("DETALLE_INTERNO_SECRETO"));

        // La BD vuelve a responder; no se exige otro login.
        cuentaActiva("CLIENTE");

        http.perform(get(PERFIL).session(sesion))
                .andExpect(status().isOk());
    }

    @Test
    void rolDesconocidoResponde500SinCerrarSesion() throws Exception {
        cuentaActiva("SUPERADMIN");
        MockHttpSession sesion = sesion("ADMIN");

        http.perform(get(ADMIN).session(sesion))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(cookie().doesNotExist("JSESSIONID"));

        assertFalse(sesion.isInvalid());
    }

    @Test
    void estacionesYCatalogosPublicosNoCompruebanLaCuenta()
            throws Exception {

        MockHttpSession sesion = sesion("ADMIN");

        http.perform(get("/api/estaciones").session(sesion))
                .andExpect(status().isOk());

        for (String ruta : List.of(
                "/api/catalogos/generos",
                "/api/catalogos/tipos-documento",
                "/api/catalogos/paises"
        )) {
            http.perform(get(ruta).session(sesion))
                    .andExpect(status().isOk());
        }

        assertFalse(sesion.isInvalid());
        verifyNoInteractions(usuarios);
    }

    @Test
    void postDeEstacionNoSeConfundeConSuConsultaPublica()
            throws Exception {

        when(usuarios.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(EMAIL, false, "ADMIN")
                ));

        MockHttpSession sesion = sesion("ADMIN");

        http.perform(post("/api/estaciones").session(sesion))
                .andExpect(status().isUnauthorized());

        assertTrue(sesion.isInvalid());
        verify(usuarios).buscarAccesoVigentePorId(17);
    }

    @Test
    void logoutNoDependeDeConsultarLaBase() throws Exception {
        MockHttpSession sesion = sesion("CLIENTE");

        http.perform(post("/api/logout").session(sesion))
                .andExpect(status().isOk());

        assertTrue(sesion.isInvalid());
        verifyNoInteractions(usuarios);
    }

    private MockHttpSession sesion(String... roles) {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                17,
                EMAIL,
                "",
                true,
                AuthorityUtils.createAuthorityList(roles)
        );

        principal.eraseCredentials();

        return guardarSesion(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }

    private MockHttpSession guardarSesion(Authentication autenticacion) {
        SecurityContext contexto =
                SecurityContextHolder.createEmptyContext();

        contexto.setAuthentication(autenticacion);

        MockHttpSession sesion = new MockHttpSession();

        sesion.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                contexto
        );

        return sesion;
    }

    private void cuentaActiva(String... roles) {
        List<AccesoUsuarioProjection> filas =
                Arrays.stream(roles)
                        .map(rol -> fila(EMAIL, true, rol))
                        .collect(Collectors.toList());

        // Permite restablecer la respuesta después de simular una excepción,
        // sin invocar durante la preparación el comportamiento que fallaba.
        doReturn(filas).when(usuarios).buscarAccesoVigentePorId(17);
    }

    private AccesoUsuarioProjection fila(
            String email,
            Boolean activo,
            String rol
    ) {
        return new AccesoUsuarioProjection() {
            public Integer getUsuarioId() { return 17; }
            public String getEmail() { return email; }
            public Boolean getActivo() { return activo; }
            public String getRolCodigo() { return rol; }
        };
    }

    // Endpoints exclusivos de esta prueba: no existen en producción.
    // Permiten comprobar la cadena real sin ejecutar operaciones de negocio.
    @TestComponent
    @RestController
    public static class EndpointsPrueba {

        @GetMapping("/api/usuarios/actual")
        public Map<String, Object> perfil(Authentication autenticacion) {
            UsuarioAutenticado principal =
                    (UsuarioAutenticado) autenticacion.getPrincipal();

            return Map.of(
                    "id", principal.getUsuarioId(),
                    "roles", autenticacion.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toList())
            );
        }

        @GetMapping("/api/admin/sonda-vigencia")
        public String administracion() {
            return "permitido";
        }

        @PostMapping({"/api/alquileres", "/api/estaciones"})
        public ResponseEntity<Void> crear() {
            return ResponseEntity.status(201).build();
        }

        @GetMapping({
                "/api/estaciones",
                "/api/catalogos/generos",
                "/api/catalogos/tipos-documento",
                "/api/catalogos/paises"
        })
        public List<Object> consultaPublica() {
            return List.of();
        }

        @GetMapping("/mis_alquileres.html")
        public String misAlquileres() {
            return "mis alquileres";
        }

        @GetMapping("/admin.html")
        public String panel() {
            return "panel";
        }
    }
}
