package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.ActualizarRolesUsuarioDTO;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RolesUsuarioServiceTest {
    @Mock UsuarioRepository usuarios;
    @Mock UsuarioRolRepository asignaciones;
    @Mock RolRepository catalogo;
    @Mock AlquilerRepository alquileres;
    @Mock OrdenMantenimientoRepository ordenes;
    @Mock AuditoriaAdministrativaService auditoria;
    @Mock EntityManager em;
    @InjectMocks RolesUsuarioService servicio;
    Usuario admin, objetivo;
    Map<String, Rol> roles;

    @BeforeEach void preparar() {
        roles = new HashMap<>();
        int id = 1;
        for (String codigo : List.of("ADMIN", "CLIENTE", "TECNICO")) {
            Rol rol = new Rol(codigo);
            ReflectionTestUtils.setField(rol, "id", id++);
            roles.put(codigo, rol);
        }
        admin = usuario(17, "ADMIN", "CLIENTE");
        objetivo = usuario(42, "CLIENTE");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin.getEmail(), null, AuthorityUtils.createAuthorityList("ADMIN")));
        lenient().when(usuarios.buscarIdPorEmail(admin.getEmail())).thenReturn(Optional.of(17));
        lenient().when(usuarios.buscarCuentaParaRolesConBloqueo(17)).thenReturn(Optional.of(admin));
        lenient().when(usuarios.buscarCuentaParaRolesConBloqueo(42)).thenReturn(Optional.of(objetivo));
        lenient().when(usuarios.buscarPorIdConDatosYRoles(17)).thenReturn(Optional.of(admin));
        lenient().when(usuarios.buscarPorIdConDatosYRoles(42)).thenReturn(Optional.of(objetivo));
        lenient().when(usuarios.contarAdministradoresActivos()).thenReturn(2L);
        lenient().when(catalogo.buscarPorCodigoConBloqueo("ADMIN")).thenReturn(Optional.of(roles.get("ADMIN")));
        lenient().when(catalogo.findByCodigo(anyString())).thenAnswer(i -> Optional.ofNullable(roles.get(i.getArgument(0))));
        lenient().when(asignaciones.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().doAnswer(i -> {
            Usuario u = i.getArgument(0);
            ReflectionTestUtils.setField(u, "version", u.getVersion() + 1);
            return null;
        }).when(em).lock(any(Usuario.class), eq(LockModeType.PESSIMISTIC_FORCE_INCREMENT));
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void consultaRolesVersionYNicknameSinEscrituras() {
        var dto = servicio.consultar(42);
        assertEquals(42, dto.getUsuarioId()); assertEquals(0, dto.getVersion());
        assertEquals(List.of("CLIENTE"), dto.getRoles());
        verifyNoInteractions(asignaciones, em, auditoria);
    }
    @Test void agregaRolConservaClienteVersionYAuditoria() {
        var dto = servicio.actualizar(42, solicitud("CLIENTE", "TECNICO"));
        assertEquals(List.of("CLIENTE", "TECNICO"), dto.getRoles()); assertEquals(1, dto.getVersion());
        verify(asignaciones, never()).delete(any());
        verify(asignaciones).save(argThat(ur -> ur.getUsuario() == objetivo && ur.getRol() == roles.get("TECNICO")));
        verify(auditoria).registrarCambioRoles(admin, 42, Set.of("CLIENTE"), Set.of("CLIENTE", "TECNICO"), "Prueba QA");
        var orden = inOrder(catalogo, usuarios, auditoria, em, asignaciones);
        orden.verify(catalogo).buscarPorCodigoConBloqueo("ADMIN");
        orden.verify(usuarios).buscarCuentaParaRolesConBloqueo(17);
        orden.verify(usuarios).buscarCuentaParaRolesConBloqueo(42);
        orden.verify(auditoria).registrarCambioRoles(any(), eq(42), any(), any(), anyString());
        orden.verify(em).lock(objetivo, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        orden.verify(asignaciones).save(any()); orden.verify(asignaciones).flush();
        verifyNoInteractions(alquileres, ordenes);
    }
    @Test void retiroDeAdminPropioSeAuditaAntesDePerderElRolYConservaOtroAdmin() {
        doAnswer(i -> { assertTrue(codigos(admin).contains("ADMIN")); return null; })
                .when(auditoria).registrarCambioRoles(eq(admin), eq(17), any(), any(), anyString());
        var dto = servicio.actualizar(17, solicitud("CLIENTE"));
        assertEquals(List.of("CLIENTE"), dto.getRoles());
        verify(usuarios).contarAdministradoresActivos();
        verify(usuarios, times(1)).buscarCuentaParaRolesConBloqueo(17);
        verify(asignaciones).delete(argThat(ur -> "ADMIN".equals(ur.getRol().getCodigo())));
    }
    @Test void ultimoAdminNoPierdeElRol() {
        when(usuarios.contarAdministradoresActivos()).thenReturn(1L);
        assertConflicto(() -> servicio.actualizar(17, solicitud("CLIENTE")), "último administrador");
        assertTrue(codigos(admin).contains("ADMIN")); sinEscrituras();
    }
    @Test void noRetiraClienteConReservasViajesOPagosPendientes() {
        when(alquileres.contarOperacionesPendientesParaRetirarCliente(42)).thenReturn(1L);
        assertConflicto(() -> servicio.actualizar(42, solicitud("TECNICO")), "financieras");
        sinEscrituras();
    }
    @Test void noRetiraClienteConDeudaOReintegroSinSaldar() {
        when(alquileres.contarLiquidacionesSinSaldar(42)).thenReturn(1L);
        assertConflicto(() -> servicio.actualizar(42, solicitud("TECNICO")), "financieras");
        sinEscrituras();
    }
    @Test void noRetiraTecnicoConOrdenesAbiertas() {
        objetivo.getUsuarioRoles().add(new UsuarioRol(objetivo, roles.get("TECNICO")));
        when(ordenes.buscarBicicletasAbiertasDeTecnico(42)).thenReturn(List.of(9));
        assertConflicto(() -> servicio.actualizar(42, solicitud("CLIENTE")), "órdenes abiertas"); sinEscrituras();
    }
    @Test void retiraTecnicoSinTocarOrdenesSiNoTieneAsignacionesAbiertas() {
        objetivo.getUsuarioRoles().add(new UsuarioRol(objetivo, roles.get("TECNICO")));
        assertEquals(List.of("CLIENTE"), servicio.actualizar(42, solicitud("CLIENTE")).getRoles());
        verify(ordenes).buscarBicicletasAbiertasDeTecnico(42);
        verify(ordenes, never()).save(any());
    }
    @Test void mismaComposicionNoInventaAuditoriaNiVersionNueva() {
        assertEquals(0, servicio.actualizar(42, solicitud("CLIENTE")).getVersion()); sinEscrituras();
    }
    @Test void versionViejaRechazaSinReaplicarCambios() {
        var datos = solicitud("CLIENTE", "TECNICO"); datos.setVersion(7);
        assertConflicto(() -> servicio.actualizar(42, datos), "Recargá"); sinEscrituras();
    }
    @Test void cuentaInactivaNoRecibeCambios() {
        objetivo.setActivo(false);
        assertConflicto(() -> servicio.actualizar(42, solicitud("CLIENTE", "ADMIN")), "inactiva"); sinEscrituras();
    }
    @Test void cuentaInexistenteDa404() {
        when(usuarios.buscarCuentaParaRolesConBloqueo(42)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> servicio.actualizar(42, solicitud("CLIENTE"))).getStatus().value()); sinEscrituras();
    }
    @Test void adminRevocadoOInactivoNoPuedeCambiarRoles() {
        admin.getUsuarioRoles().clear();
        assertThrows(AccessDeniedException.class, () -> servicio.actualizar(42, solicitud("ADMIN"))); sinEscrituras();
        admin.getUsuarioRoles().add(new UsuarioRol(admin, roles.get("ADMIN"))); admin.setActivo(false);
        assertThrows(AccessDeniedException.class, () -> servicio.consultar(42));
    }
    @Test void sinAutoridadAdminNoConsultaCuentasNiCatalogo() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> servicio.consultar(42));
        assertThrows(AccessDeniedException.class, () -> servicio.actualizar(42, solicitud("CLIENTE")));
        verifyNoInteractions(usuarios, catalogo);
    }
    @Test void rechazaRolesVaciosRepetidosDesconocidosYMotivoSinEscrituras() {
        for (List<String> lista : List.of(List.<String>of(), List.of("CLIENTE", "CLIENTE"), List.of("ROOT"))) {
            var datos = solicitud("CLIENTE"); datos.setRoles(lista);
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> servicio.actualizar(42, datos)).getStatus().value());
        }
        var datos = solicitud("CLIENTE"); datos.setMotivo(" ");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> servicio.actualizar(42, datos)).getStatus().value());
        sinEscrituras(); verifyNoInteractions(usuarios, catalogo);
    }
    @Test void falloDeAuditoriaImpideAplicarAsignacionesYSePropagaParaRollback() {
        doThrow(new IllegalStateException("Fallo QA")).when(auditoria)
                .registrarCambioRoles(any(), anyInt(), any(), any(), anyString());
        assertThrows(IllegalStateException.class, () -> servicio.actualizar(42, solicitud("ADMIN", "CLIENTE")));
        assertEquals(Set.of("CLIENTE"), codigos(objetivo)); verifyNoInteractions(asignaciones, em);
    }
    @Test void configuracionExigeUnaTransaccionReadCommittedConRollback() throws Exception {
        var tx = RolesUsuarioService.class.getMethod("actualizar", Integer.class, ActualizarRolesUsuarioDTO.class)
                .getAnnotation(Transactional.class);
        assertEquals(Isolation.READ_COMMITTED, tx.isolation());
        assertTrue(Arrays.asList(tx.rollbackFor()).contains(Exception.class));
    }
    private Usuario usuario(int id, String... codigos) {
        Usuario u = new Usuario(); u.setId(id); u.setActivo(true);
        u.setNickname("roles." + id); u.setEmail("roles." + id + "@example.com");
        ReflectionTestUtils.setField(u, "version", 0);
        for (String codigo : codigos) u.getUsuarioRoles().add(new UsuarioRol(u, roles.get(codigo)));
        return u;
    }
    private Set<String> codigos(Usuario u) {
        Set<String> result = new TreeSet<>(); u.getUsuarioRoles().forEach(ur -> result.add(ur.getRol().getCodigo())); return result;
    }
    private ActualizarRolesUsuarioDTO solicitud(String... codigos) {
        var d = new ActualizarRolesUsuarioDTO(); d.setVersion(0); d.setRoles(Arrays.asList(codigos)); d.setMotivo(" Prueba QA "); return d;
    }
    private void sinEscrituras() { verifyNoInteractions(asignaciones, em, auditoria); }
    private void assertConflicto(org.junit.jupiter.api.function.Executable accion, String mensaje) {
        var error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(409, error.getStatus().value()); assertTrue(error.getReason().contains(mensaje));
    }
}
