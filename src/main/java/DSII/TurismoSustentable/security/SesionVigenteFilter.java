package DSII.TurismoSustentable.security;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class SesionVigenteFilter extends OncePerRequestFilter {

    private final AccesoSesionService accesoSesionService;
    private final RequestMatcher rutasAComprobar;

    public SesionVigenteFilter(
            AccesoSesionService accesoSesionService,
            RequestMatcher rutasAComprobar
    ) {
        this.accesoSesionService = accesoSesionService;
        this.rutasAComprobar = rutasAComprobar;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !rutasAComprobar.matches(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        Authentication anterior =
                SecurityContextHolder.getContext().getAuthentication();

        // La autorización existente resolverá los accesos anónimos.
        if (anterior == null
                || anterior instanceof AnonymousAuthenticationToken) {
            chain.doFilter(request, response);
            return;
        }

        Optional<Authentication> comprobada;

        try {
            comprobada = accesoSesionService.comprobar(anterior);

        } catch (DataAccessException error) {
            responderError(
                    response,
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "No se pudo comprobar el acceso. Intentá nuevamente."
            );
            return;

        } catch (ConfiguracionSistemaException error) {
            responderError(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error interno al comprobar el acceso."
            );
            return;
        }

        if (comprobada.isEmpty()) {
            invalidarSesion(request, response);

        } else {
            // No modificar el contexto anterior compartido por otras peticiones.
            SecurityContext nuevo =
                    SecurityContextHolder.createEmptyContext();

            nuevo.setAuthentication(comprobada.get());
            SecurityContextHolder.setContext(nuevo);
        }

        // Spring Security conserva la decisión final: permitir, 401 o 403.
        // No incluir esta llamada dentro del catch de comprobación de sesión.
        chain.doFilter(request, response);
    }

    private void invalidarSesion(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignorada) {
                // Otra petición pudo invalidar la misma sesión.
            }
        }

        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath(
                request.getContextPath().isEmpty()
                        ? "/"
                        : request.getContextPath()
        );

        response.addCookie(cookie);
    }

    private void responderError(
            HttpServletResponse response,
            int estado,
            String mensaje
    ) throws IOException {
        response.setStatus(estado);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");

        // Los mensajes son constantes internas, no contenido recibido del usuario.
        response.getWriter().write(
                "{\"message\":\"" + mensaje + "\"}"
        );
    }
}
