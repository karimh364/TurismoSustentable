package DSII.TurismoSustentable.security;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.repositories.projections.AccesoUsuarioProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccesoSesionServiceTest {

    private static final String EMAIL = "ana@example.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AccesoSesionService servicio;

    @Test
    void usaRolesVigentesSinModificarLaAutenticacionAnterior() {
        Authentication anterior = autenticacion("ADMIN", "CLIENTE");

        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(17, EMAIL, true, "CLIENTE")
                ));

        Authentication vigente =
                servicio.comprobar(anterior).orElseThrow();

        assertNotSame(anterior, vigente);
        assertNotSame(anterior.getPrincipal(), vigente.getPrincipal());

        assertEquals(Set.of("CLIENTE"), roles(vigente));
        assertEquals(Set.of("ADMIN", "CLIENTE"), roles(anterior));

        UsuarioAutenticado principal =
                (UsuarioAutenticado) vigente.getPrincipal();

        assertEquals(17, principal.getUsuarioId());
        assertEquals(EMAIL, principal.getUsername());
        assertNull(principal.getPassword());
        assertNull(vigente.getCredentials());
        assertEquals("detalle-prueba", vigente.getDetails());

        verify(usuarioRepository).buscarAccesoVigentePorId(17);
        verifyNoMoreInteractions(usuarioRepository);
    }

    @Test
    void adminNoRecibeClienteImplicitamente() {
        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(17, EMAIL, true, "ADMIN")
                ));

        Authentication vigente = servicio
                .comprobar(autenticacion("ADMIN", "CLIENTE"))
                .orElseThrow();

        assertEquals(Set.of("ADMIN"), roles(vigente));
    }

    @Test
    void conservaTodosLosRolesVigentesSinDuplicarlos() {
        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(17, EMAIL, true, "CLIENTE"),
                        fila(17, EMAIL, true, "TECNICO"),
                        fila(17, EMAIL, true, "CLIENTE")
                ));

        Authentication vigente = servicio
                .comprobar(autenticacion("CLIENTE"))
                .orElseThrow();

        assertEquals(Set.of("CLIENTE", "TECNICO"), roles(vigente));
    }

    @Test
    void rechazaCuentaInexistente() {
        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of());

        assertTrue(
                servicio.comprobar(autenticacion("CLIENTE")).isEmpty()
        );
    }

    @Test
    void rechazaCuentaInactivaAunqueLaSesionTengaRoles() {
        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(17, EMAIL, false, "ADMIN")
                ));

        assertTrue(
                servicio.comprobar(autenticacion("ADMIN")).isEmpty()
        );
    }

    @Test
    void cambioDeEmailExigeNuevaSesionSinBuscarOtraCuentaPorEmail() {
        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(17, "nuevo@example.com", true, "CLIENTE")
                ));

        assertTrue(
                servicio.comprobar(autenticacion("CLIENTE")).isEmpty()
        );

        verify(usuarioRepository).buscarAccesoVigentePorId(17);
        verifyNoMoreInteractions(usuarioRepository);
    }

    @Test
    void cuentaSinRolesNoConservaLosPermisosAnteriores() {
        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(17, EMAIL, true, null)
                ));

        Authentication vigente = servicio
                .comprobar(autenticacion("ADMIN"))
                .orElseThrow();

        assertTrue(vigente.isAuthenticated());
        assertTrue(vigente.getAuthorities().isEmpty());
    }

    @Test
    void rolDesconocidoEsUnErrorDeConfiguracion() {
        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(17, EMAIL, true, "SUPERADMIN")
                ));

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> servicio.comprobar(autenticacion("ADMIN"))
        );
    }

    @Test
    void noAceptaDatosDeOtraCuenta() {
        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenReturn(List.of(
                        fila(18, EMAIL, true, "ADMIN")
                ));

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> servicio.comprobar(autenticacion("CLIENTE"))
        );
    }

    @Test
    void sinIdentidadAutenticadaValidaNoConsultaLaBase() {
        assertTrue(servicio.comprobar(null).isEmpty());

        Authentication sinAutenticar =
                new UsernamePasswordAuthenticationToken(
                        "visitante", null
                );

        Authentication sinIdEstable =
                new UsernamePasswordAuthenticationToken(
                        "visitante",
                        null,
                        AuthorityUtils.createAuthorityList("ADMIN")
                );

        assertTrue(servicio.comprobar(sinAutenticar).isEmpty());
        assertTrue(servicio.comprobar(sinIdEstable).isEmpty());

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void falloDeBaseNoSeConvierteEnSesionInvalida() {
        DataAccessResourceFailureException fallo =
                new DataAccessResourceFailureException("Fallo simulado");

        when(usuarioRepository.buscarAccesoVigentePorId(17))
                .thenThrow(fallo);

        assertSame(
                fallo,
                assertThrows(
                        DataAccessResourceFailureException.class,
                        () -> servicio.comprobar(
                                autenticacion("CLIENTE")
                        )
                )
        );
    }

    private Authentication autenticacion(String... roles) {
        UsuarioAutenticado principal = new UsuarioAutenticado(
                17,
                EMAIL,
                "hash-simulado",
                true,
                AuthorityUtils.createAuthorityList(roles)
        );

        principal.eraseCredentials();

        UsernamePasswordAuthenticationToken autenticacion =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        autenticacion.setDetails("detalle-prueba");
        return autenticacion;
    }

    private Set<String> roles(Authentication autenticacion) {
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private AccesoUsuarioProjection fila(
            Integer id,
            String email,
            Boolean activo,
            String rol
    ) {
        return new AccesoUsuarioProjection() {
            public Integer getUsuarioId() { return id; }
            public String getEmail() { return email; }
            public Boolean getActivo() { return activo; }
            public String getRolCodigo() { return rol; }
        };
    }
}
