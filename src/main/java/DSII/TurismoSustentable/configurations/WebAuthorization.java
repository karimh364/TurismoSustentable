package DSII.TurismoSustentable.configurations;

import DSII.TurismoSustentable.security.AccesoSesionService;
import DSII.TurismoSustentable.security.SesionVigenteFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpMethod;

@EnableWebSecurity
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebAuthorization {

    private static final RequestMatcher RUTAS_PUBLICAS =
            new OrRequestMatcher(
                    new AntPathRequestMatcher("/"),
                    new AntPathRequestMatcher("/index.html"),
                    new AntPathRequestMatcher("/registro.html"),
                    new AntPathRequestMatcher("/recuperar-password.html"),
                    new AntPathRequestMatcher("/restablecer-password.html"),
                    new AntPathRequestMatcher("/css/**"),
                    new AntPathRequestMatcher("/js/**"),
                    new AntPathRequestMatcher("/img/**"),
                    new AntPathRequestMatcher("/*.ico"),

                    new AntPathRequestMatcher("/api/usuarios", "POST"),
                    new AntPathRequestMatcher("/api/login", "POST"),
                    new AntPathRequestMatcher("/api/logout", "POST"),
                    new AntPathRequestMatcher("/api/password/solicitar", "POST"),
                    new AntPathRequestMatcher("/api/password/restablecer", "POST"),

                    new AntPathRequestMatcher("/api/estaciones", "GET"),
                    new AntPathRequestMatcher("/api/catalogos/generos", "GET"),
                    new AntPathRequestMatcher("/api/catalogos/tipos-documento", "GET"),
                    new AntPathRequestMatcher("/api/catalogos/paises", "GET")
            );

    private static final RequestMatcher PAGINAS_PRIVADAS =
            new OrRequestMatcher(
                    new AntPathRequestMatcher("/admin.html"),
                    new AntPathRequestMatcher("/incidencias.html"),
                    new AntPathRequestMatcher("/mantenimiento.html"),
                    new AntPathRequestMatcher("/alquiler.html"),
                    new AntPathRequestMatcher("/mis_alquileres.html")
            );

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            AccesoSesionService accesoSesionService
    ) throws Exception {

        // REGLAS DE AUTORIZACIÓN
        http.authorizeRequests()
                .requestMatchers(RUTAS_PUBLICAS).permitAll()

                // 3. PÁGINAS PRIVADAS
                .antMatchers("/alquiler.html").hasAuthority("CLIENTE")
                .antMatchers("/mis_alquileres.html").hasAuthority("CLIENTE")
                .antMatchers("/mantenimiento.html").hasAnyAuthority("ADMIN","TECNICO")
                .antMatchers("/incidencias.html").hasAnyAuthority("ADMIN","CLIENTE")

                .antMatchers(HttpMethod.GET,"/api/incidencias","/api/incidencias/*","/api/incidencias/*/historial")
                .hasAnyAuthority("ADMIN","CLIENTE")
                .antMatchers(HttpMethod.POST,"/api/incidencias").hasAnyAuthority("ADMIN","CLIENTE")
                .antMatchers("/api/incidencias/**").hasAuthority("ADMIN")

                .antMatchers(HttpMethod.GET,"/api/mantenimiento/ordenes","/api/mantenimiento/ordenes/**")
                .hasAnyAuthority("ADMIN","TECNICO")
                .antMatchers(HttpMethod.POST,"/api/mantenimiento/ordenes/*/iniciar",
                        "/api/mantenimiento/ordenes/*/reanudar","/api/mantenimiento/ordenes/*/pausar",
                        "/api/mantenimiento/ordenes/*/trabajo","/api/mantenimiento/ordenes/*/finalizar")
                .hasAuthority("TECNICO")
                .antMatchers("/api/mantenimiento/**").hasAuthority("ADMIN")

                .antMatchers(
                        "/admin.html"
                ).hasAuthority("ADMIN")

                // 4. DATOS DEL USUARIO ACTUAL
                // Esta regla debe estar antes de /api/usuarios/**
                .antMatchers(
                        HttpMethod.GET,
                        "/api/usuarios/actual"
                ).hasAnyAuthority("ADMIN", "CLIENTE", "TECNICO")

                // Saldo simulado propio: requiere CLIENTE.
                .antMatchers(
                        HttpMethod.GET,
                        "/api/pagos/saldo-simulado"
                ).hasAuthority("CLIENTE")

                .antMatchers(HttpMethod.GET, "/api/pagos/reembolsos/*")
                .hasAnyAuthority("CLIENTE", "ADMIN")

                // Pagos propios de anticipos y adicionales: requieren CLIENTE.
                .antMatchers(
                        HttpMethod.POST,
                        "/api/pagos/anticipo/*",
                        "/api/pagos/anticipo/*/resolver",
                        "/api/pagos/adicional/*",
                        "/api/pagos/adicional/*/resolver",
                        "/api/pagos/reembolsos/*/resolver"
                ).hasAuthority("CLIENTE")

                // 5. OPERACIONES PERMITIDAS PARA CLIENTES
                .antMatchers(
                        HttpMethod.GET,
                        "/api/alquileres",
                        "/api/cotizacion",
                        "/api/bicicletas/disponibles"
                ).hasAnyAuthority("ADMIN", "CLIENTE")

                // Consulta del alquiler y su liquidación.
                // Propiedad, cuenta activa y roles vigentes comprobados en el servicio.
                .antMatchers(
                        HttpMethod.GET,
                        "/api/alquileres/*",
                        "/api/alquileres/*/historial",
                        "/api/alquileres/*/liquidacion"
                ).hasAnyAuthority("CLIENTE", "ADMIN")

                .antMatchers(
                        HttpMethod.POST,
                        "/api/alquileres",
                        "/api/alquileres/*/detalles/*/retirar",
                        "/api/alquileres/*/detalles/*/cancelar",
                        "/api/alquileres/*/cancelar"
                ).hasAuthority("CLIENTE")

                // Devolver no equivale a alquilar: ADMIN puede recibir una unidad ajena.
                .antMatchers(
                        HttpMethod.POST,
                        "/api/alquileres/*/detalles/*/devolver"
                ).hasAnyAuthority("CLIENTE", "ADMIN")

                // 6. CONSULTAS DE USUARIOS: SOLO ADMIN
                .antMatchers(
                        HttpMethod.GET,
                        "/api/usuarios",
                        "/api/usuarios/**"
                ).hasAuthority("ADMIN")

                // 7. ENDPOINTS ADMINISTRATIVOS
                .antMatchers(
                        "/api/admin/**"
                ).hasAuthority("ADMIN")

                // Cualquier otro GET de la API será administrativo
                .antMatchers(
                        HttpMethod.GET,
                        "/api/**"
                ).hasAuthority("ADMIN")

                // Cualquier otra alta será administrativa
                .antMatchers(
                        HttpMethod.POST,
                        "/api/**"
                ).hasAuthority("ADMIN")

                // Todas las modificaciones restantes son administrativas
                .antMatchers(
                        HttpMethod.PUT,
                        "/api/**"
                ).hasAuthority("ADMIN")

                // Todos los borrados son administrativos
                .antMatchers(
                        HttpMethod.DELETE,
                        "/api/**"
                ).hasAuthority("ADMIN")

                // Cualquier ruta o metodo no declarado queda denegado
                .anyRequest().denyAll();

        // CONFIGURACION DEL LOGIN/LOGOUT (FormLogin) - Mantiene la configuración REST
        http.formLogin()
                .usernameParameter("nickname")
                .passwordParameter("password")
                .loginPage("/api/login");

        http.logout()
                .logoutUrl("/api/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(
                        new HttpStatusReturningLogoutSuccessHandler()
                );

        // CONFIGURACION REST (Reemplaza HTML por códigos HTTP)
        http.csrf().disable();
        http.headers().frameOptions().disable();

        http.exceptionHandling()
                .authenticationEntryPoint((req, res, exc) -> {
                    if (esPaginaPrivada(req)) {
                        res.sendRedirect(req.getContextPath()
                                + "/index.html?sesion=requerida#login-anchor");
                    } else {
                        res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    }
                })
                .accessDeniedHandler((req, res, exc) -> {
                    if (esPaginaPrivada(req)) {
                        res.sendRedirect(req.getContextPath()
                                + "/index.html?acceso=denegado#login-anchor");
                    } else {
                        res.sendError(HttpServletResponse.SC_FORBIDDEN);
                    }
                });

        http.formLogin().successHandler((req, res, auth) ->
                clearAuthenticationAttributes(req));

        http.formLogin().failureHandler((req, res, exc) ->
                res.sendError(HttpServletResponse.SC_UNAUTHORIZED));

        // El flujo REST no recupera peticiones guardadas después del login.
        http.requestCache().requestCache(new NullRequestCache());

        RequestMatcher rutasAComprobar =
                new AndRequestMatcher(
                        new OrRequestMatcher(
                                new AntPathRequestMatcher("/api/**"),
                                PAGINAS_PRIVADAS
                        ),
                        new NegatedRequestMatcher(RUTAS_PUBLICAS)
                );

        // Se ejecuta con el contexto de sesión cargado y antes de autorizar.
        http.addFilterBefore(
                new SesionVigenteFilter(
                        accesoSesionService,
                        rutasAComprobar
                ),
                AnonymousAuthenticationFilter.class
        );

        return http.build();
    }

    // Solo las páginas privadas redirigen; las APIs conservan 401/403.
    private boolean esPaginaPrivada(HttpServletRequest request) {
        return "GET".equals(request.getMethod())
                && PAGINAS_PRIVADAS.matches(request);
    }

    // Función de utilidad requerida por el successHandler
    private void clearAuthenticationAttributes (HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute (WebAttributes.AUTHENTICATION_EXCEPTION);
        }
    }
}
