package DSII.TurismoSustentable.security;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.FilterChain;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SesionVigenteFilterTest {

    @Mock
    private AccesoSesionService servicio;

    @Mock
    private FilterChain chain;

    private SesionVigenteFilter filtro;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();

        // En esta prueba aislada, todas las peticiones requieren comprobación.
        filtro = new SesionVigenteFilter(servicio, peticion -> true);

        request = new MockHttpServletRequest(
                "GET",
                "/api/usuarios/actual"
        );
        request.setServletPath("/api/usuarios/actual");

        response = new MockHttpServletResponse();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void visitanteNoConsultaLaBaseNiCreaSesion() throws Exception {
        filtro.doFilter(request, response, chain);

        verifyNoInteractions(servicio);
        verify(chain).doFilter(request, response);
        assertNull(request.getSession(false));
    }

    @Test
    void autenticacionAnonimaNoSeCompruebaComoCuenta() throws Exception {
        identificar(new AnonymousAuthenticationToken(
                "clave-prueba",
                "anonymousUser",
                AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        ));

        filtro.doFilter(request, response, chain);

        verifyNoInteractions(servicio);
        verify(chain).doFilter(request, response);
    }

    @Test
    void rutaExcluidaConservaLaSesionSinConsultarElServicio()
            throws Exception {

        Authentication anterior = autenticacion("CLIENTE");
        identificar(anterior);

        SesionVigenteFilter excluida =
                new SesionVigenteFilter(servicio, peticion -> false);

        excluida.doFilter(request, response, chain);

        assertSame(
                anterior,
                SecurityContextHolder.getContext().getAuthentication()
        );
        verifyNoInteractions(servicio);
        verify(chain).doFilter(request, response);
    }

    @Test
    void actualizaElContextoSinModificarElAnterior() throws Exception {
        Authentication anterior = autenticacion("ADMIN", "CLIENTE");
        SecurityContext contextoAnterior = identificar(anterior);

        Authentication vigente = autenticacion("CLIENTE");

        when(servicio.comprobar(anterior))
                .thenReturn(Optional.of(vigente));

        AtomicReference<Authentication> recibida =
                new AtomicReference<>();

        filtro.doFilter(
                request,
                response,
                (peticion, respuesta) -> recibida.set(
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                )
        );

        assertSame(vigente, recibida.get());
        assertNotSame(
                contextoAnterior,
                SecurityContextHolder.getContext()
        );
        assertSame(anterior, contextoAnterior.getAuthentication());
        assertEquals(2, anterior.getAuthorities().size());

        verify(servicio).comprobar(anterior);
    }

    @Test
    void identidadInvalidaInvalidaSesionYLimpiaLaCookie()
            throws Exception {

        Authentication anterior = autenticacion("ADMIN");
        identificar(anterior);

        MockHttpSession session =
                (MockHttpSession) request.getSession();

        when(servicio.comprobar(anterior))
                .thenReturn(Optional.empty());

        filtro.doFilter(request, response, chain);

        assertTrue(session.isInvalid());
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );

        assertNotNull(response.getCookie("JSESSIONID"));
        assertEquals(0, response.getCookie("JSESSIONID").getMaxAge());
        assertEquals("/", response.getCookie("JSESSIONID").getPath());

        verify(chain).doFilter(request, response);
    }

    @Test
    void identidadInvalidaNoCreaOtraSesion() throws Exception {
        Authentication anterior = autenticacion("CLIENTE");
        identificar(anterior);

        when(servicio.comprobar(anterior))
                .thenReturn(Optional.empty());

        filtro.doFilter(request, response, chain);

        assertNull(request.getSession(false));
        assertNull(
                SecurityContextHolder.getContext().getAuthentication()
        );
    }

    @Test
    void cookieSeLimpiaConElContextPathCorrecto() throws Exception {
        request.setContextPath("/turismo");

        Authentication anterior = autenticacion("CLIENTE");
        identificar(anterior);

        when(servicio.comprobar(anterior))
                .thenReturn(Optional.empty());

        filtro.doFilter(request, response, chain);

        assertEquals(
                "/turismo",
                response.getCookie("JSESSIONID").getPath()
        );
    }

    @Test
    void falloDeBaseResponde503SinCerrarLaSesion() throws Exception {
        Authentication anterior = autenticacion("CLIENTE");
        SecurityContext contexto = identificar(anterior);

        MockHttpSession session =
                (MockHttpSession) request.getSession();

        when(servicio.comprobar(anterior))
                .thenThrow(new DataAccessResourceFailureException(
                        "Detalle interno de prueba"
                ));

        filtro.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertFalse(session.isInvalid());
        assertSame(contexto, SecurityContextHolder.getContext());
        assertSame(anterior, contexto.getAuthentication());
        assertNull(response.getCookie("JSESSIONID"));
        assertFalse(
                response.getContentAsString().contains("Detalle interno")
        );

        verifyNoInteractions(chain);
    }

    @Test
    void configuracionInvalidaResponde500SinCerrarLaSesion()
            throws Exception {

        Authentication anterior = autenticacion("ADMIN");
        identificar(anterior);

        MockHttpSession session =
                (MockHttpSession) request.getSession();

        when(servicio.comprobar(anterior))
                .thenThrow(new ConfiguracionSistemaException(
                        "Detalle interno de prueba"
                ));

        filtro.doFilter(request, response, chain);

        assertEquals(500, response.getStatus());
        assertFalse(session.isInvalid());
        assertSame(
                anterior,
                SecurityContextHolder.getContext().getAuthentication()
        );
        assertNull(response.getCookie("JSESSIONID"));
        assertFalse(
                response.getContentAsString().contains("Detalle interno")
        );

        verifyNoInteractions(chain);
    }

    @Test
    void noConfundeUnFalloPosteriorConUnFalloDeSesion() {
        Authentication anterior = autenticacion("CLIENTE");
        identificar(anterior);

        when(servicio.comprobar(anterior))
                .thenReturn(Optional.of(autenticacion("CLIENTE")));

        DataAccessResourceFailureException falloOperacion =
                new DataAccessResourceFailureException(
                        "Fallo de una operación posterior"
                );

        assertSame(
                falloOperacion,
                assertThrows(
                        DataAccessResourceFailureException.class,
                        () -> filtro.doFilter(
                                request,
                                response,
                                (peticion, respuesta) -> {
                                    throw falloOperacion;
                                }
                        )
                )
        );
    }

    private SecurityContext identificar(Authentication autenticacion) {
        SecurityContext contexto =
                SecurityContextHolder.createEmptyContext();

        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);

        return contexto;
    }

    private Authentication autenticacion(String... roles) {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                17,
                "ana@example.com",
                "hash-simulado",
                true,
                AuthorityUtils.createAuthorityList(roles)
        );

        principal.eraseCredentials();

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }
}
