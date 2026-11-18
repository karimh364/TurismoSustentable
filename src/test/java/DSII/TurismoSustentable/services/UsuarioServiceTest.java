package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.ActualizarUsuarioDTO;
import DSII.TurismoSustentable.dto.UsuarioDTO;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.GeneroRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.AfterEach;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private GeneroRepository generoRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private AuditoriaAdministrativaService auditoria;

    @Mock
    private MantenimientoService mantenimiento;

    @Mock
    private Clock reloj;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private ActualizarUsuarioDTO datos;
    private Genero generoNuevo;
    private LocalDateTime vencimientoOriginal;
    private Usuario administrador;

    @BeforeEach
    void prepararDatos() {
        administrador = new Usuario();
        administrador.setId(99);
        lenient().when(auditoria.administradorActual()).thenReturn(administrador);
        Persona persona = new Persona(
                "Ana",
                "Perez",
                LocalDate.of(1995, 5, 20),
                new Genero("FEMENINO"),
                new TipoDocumento("DNI", "Documento Nacional de Identidad"),
                "00123456",
                new Pais("AR", "Argentina")
        );

        vencimientoOriginal = LocalDateTime.of(2030, 1, 1, 0, 0);

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setPersona(persona);
        usuario.setEmail("ana@example.com");
        usuario.setNickname("ana.perez");
        usuario.setPassword("hash-simulado-para-test");
        usuario.setTokenRecuperacion("a".repeat(64));
        usuario.setVencimientoTokenRecuperacion(vencimientoOriginal);

        Rol rolCliente = new Rol("CLIENTE");
        ReflectionTestUtils.setField(rolCliente, "id", 2);

        usuario.getUsuarioRoles().add(
                new UsuarioRol(usuario, rolCliente)
        );

        datos = new ActualizarUsuarioDTO();
        datos.setNombre(" Ana Maria ");
        datos.setApellido(" Lopez ");
        datos.setEmail(" ANA@EXAMPLE.COM ");
        datos.setFechaNacimiento(LocalDate.of(1994, 5, 20));
        datos.setGeneroId(2);

        generoNuevo = new Genero("OTRO");
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactivaCuentaTrasPrepararOrdenesYAuditaSinBorrarSusDatos() {
        prepararInactivacion();
        String email = usuario.getEmail();
        Persona persona = usuario.getPersona();
        doAnswer(inv -> { assertTrue(usuario.isActivo()); return null; })
                .when(mantenimiento).prepararOrdenesPorInactivacion(
                        eq(usuario), eq(administrador), eq("Licencia"), any(LocalDateTime.class));

        usuarioService.inactivarUsuario(1, " Licencia ");

        assertFalse(usuario.isActivo());
        assertEquals(email, usuario.getEmail());
        assertSame(persona, usuario.getPersona());
        assertEquals("hash-simulado-para-test", usuario.getPassword());
        assertEquals("CLIENTE", usuario.getUsuarioRoles().iterator().next().getRol().getCodigo());
        var orden = inOrder(usuarioRepository, mantenimiento, auditoria);
        orden.verify(usuarioRepository).buscarIdPorEmail(administrador.getEmail());
        orden.verify(usuarioRepository).buscarPorIdConBloqueo(1);
        orden.verify(usuarioRepository).buscarPorIdConBloqueo(99);
        orden.verify(mantenimiento).prepararOrdenesPorInactivacion(
                usuario, administrador, "Licencia", fechaInactivacion());
        orden.verify(usuarioRepository).flush();
        orden.verify(auditoria).registrarInactivacionUsuario(
                administrador, 1, "Licencia", fechaInactivacion());
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void protegeAdminAunqueTambienSeaTecnicoSinPrepararOrdenes() {
        prepararInactivacion();
        for (String codigo : List.of("ADMIN", "TECNICO")) {
            Rol rol = new Rol(codigo);
            ReflectionTestUtils.setField(rol, "id", "ADMIN".equals(codigo) ? 3 : 4);
            usuario.getUsuarioRoles().add(new UsuarioRol(usuario, rol));
        }
        var error = assertThrows(ResponseStatusException.class,
                () -> usuarioService.inactivarUsuario(1, "QA"));
        assertEquals(409, error.getStatus().value());
        assertEquals("No se puede inactivar a un usuario Administrador.", error.getReason());
        comprobarInactivacionSinEscrituras();
    }

    @Test
    void noInactivaCuentaYaInactiva() {
        prepararInactivacion();
        usuario.setActivo(false);
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> usuarioService.inactivarUsuario(1, null)).getStatus().value());
        comprobarInactivacionSinEscrituras();
    }

    @Test
    void inactivacionDeUsuarioInexistenteDa404() {
        prepararInactivacion();
        when(usuarioRepository.buscarPorIdConBloqueo(1)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> usuarioService.inactivarUsuario(1, null)).getStatus().value());
        comprobarInactivacionSinEscrituras();
    }

    @Test
    void inactivacionExigeSesionAdminAntesDeConsultarCuentas() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> usuarioService.inactivarUsuario(1, null));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "cliente@example.com", null, AuthorityUtils.createAuthorityList("CLIENTE")));
        assertThrows(AccessDeniedException.class, () -> usuarioService.inactivarUsuario(1, null));
        verifyNoInteractions(usuarioRepository, mantenimiento);
    }

    @Test
    void adminRevocadoOInactivoNoPuedeInactivarAunqueSesionConserveAutoridad() {
        prepararInactivacion();
        administrador.getUsuarioRoles().clear();
        assertThrows(AccessDeniedException.class, () -> usuarioService.inactivarUsuario(1, "QA"));
        comprobarInactivacionSinEscrituras();
        prepararInactivacion();
        administrador.setActivo(false);
        assertThrows(AccessDeniedException.class, () -> usuarioService.inactivarUsuario(1, "QA"));
        comprobarInactivacionSinEscrituras();
    }

    @Test
    void inactivacionValidaIdYLongitudAntesDePrepararOrdenes() {
        for (Integer id : java.util.Arrays.asList(null, 0, -1)) {
            assertEquals(400, assertThrows(ResponseStatusException.class,
                    () -> usuarioService.inactivarUsuario(id, null)).getStatus().value());
        }
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> usuarioService.inactivarUsuario(1, "x".repeat(2001))).getStatus().value());
        verifyNoInteractions(usuarioRepository, mantenimiento);
    }

    @Test
    void falloAlPrepararOrdenesImpideInactivacionYAuditoria() {
        prepararInactivacion();
        doThrow(new IllegalStateException("Fallo de mantenimiento")).when(mantenimiento)
                .prepararOrdenesPorInactivacion(any(), any(), any(), any());
        assertThrows(IllegalStateException.class, () -> usuarioService.inactivarUsuario(1, "QA"));
        assertTrue(usuario.isActivo());
        verify(usuarioRepository, never()).flush();
        verify(auditoria, never()).registrarInactivacionUsuario(any(), anyInt(), any(), any());
    }

    @Test
    void falloAuditoriaDeInactivacionSePropagaParaRevertirLaTransaccion() {
        prepararInactivacion();
        doThrow(new IllegalStateException("Fallo de auditoría")).when(auditoria)
                .registrarInactivacionUsuario(any(), anyInt(), any(), any());
        assertThrows(IllegalStateException.class, () -> usuarioService.inactivarUsuario(1, "QA"));
        verify(usuarioRepository).flush();
        // La reversión real de entidades se comprueba en integración, no con mocks.
    }

    private void prepararInactivacion() {
        administrador.setEmail("admin.inactivacion@example.com");
        administrador.setActivo(true);
        if (administrador.getUsuarioRoles().isEmpty()) {
            Rol rol = new Rol("ADMIN");
            ReflectionTestUtils.setField(rol, "id", 3);
            administrador.getUsuarioRoles().add(new UsuarioRol(administrador, rol));
        }
        usuario.setActivo(true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                administrador.getEmail(), null, AuthorityUtils.createAuthorityList("ADMIN")));
        lenient().when(usuarioRepository.buscarIdPorEmail(administrador.getEmail())).thenReturn(Optional.of(99));
        lenient().when(usuarioRepository.buscarPorIdConBloqueo(99)).thenReturn(Optional.of(administrador));
        lenient().when(usuarioRepository.buscarPorIdConBloqueo(1)).thenReturn(Optional.of(usuario));
        lenient().when(reloj.instant()).thenReturn(fechaInactivacion().toInstant(ZoneOffset.UTC));
    }

    private LocalDateTime fechaInactivacion() {
        return LocalDateTime.of(2026, 11, 18, 12, 0);
    }

    private void comprobarInactivacionSinEscrituras() {
        verifyNoInteractions(mantenimiento);
        verify(usuarioRepository, never()).flush();
        verify(auditoria, never()).registrarInactivacionUsuario(any(), anyInt(), any(), any());
    }

    @Test
    void actualizaPersonaSinAlterarOtrosDatosDeCuenta() {
        prepararBusquedasValidas();
        when(auditoria.datos(usuario)).thenAnswer(i ->
                usuario.getEmail() + "|" + usuario.getPersona().getNombre());
        Persona personaOriginal = usuario.getPersona();

        UsuarioDTO resultado = usuarioService.actualizarUsuario(1, datos);

        assertSame(personaOriginal, usuario.getPersona());
        assertEquals("Ana Maria", personaOriginal.getNombre());
        assertEquals("Lopez", personaOriginal.getApellido());
        assertEquals(datos.getFechaNacimiento(), personaOriginal.getFechaNacimiento());
        assertSame(generoNuevo, personaOriginal.getGenero());

        assertEquals("00123456", personaOriginal.getNumeroDocumento());
        assertEquals("DNI", personaOriginal.getTipoDocumento().getCodigo());
        assertEquals("AR", personaOriginal.getPaisEmisor().getCodigo());
        assertEquals("ana.perez", usuario.getNickname());
        assertEquals(1, usuario.getUsuarioRoles().size());
        assertEquals(List.of("CLIENTE"), resultado.getRoles());
        assertEquals("hash-simulado-para-test", usuario.getPassword());
        assertEquals("a".repeat(64), usuario.getTokenRecuperacion());
        assertEquals(vencimientoOriginal, usuario.getVencimientoTokenRecuperacion());

        assertEquals("Ana Maria", resultado.getNombre());
        assertEquals("ana@example.com", resultado.getEmail());
        assertEquals("OTRO", resultado.getGenero());

        verify(usuarioRepository).existsByEmailIgnoreCaseAndIdNot(
                "ana@example.com", 1
        );
        verify(entityManager).lock(
                usuario, LockModeType.OPTIMISTIC_FORCE_INCREMENT
        );
        verify(usuarioRepository).flush();
        verify(auditoria).registrar(administrador, "USUARIO", 1, "ACTUALIZAR",
                "ana@example.com|Ana", "ana@example.com|Ana Maria");
    }

    @Test
    void cambiarEmailInvalidaLaRecuperacionAnterior() {
        prepararBusquedasValidas();
        when(auditoria.datos(usuario)).thenAnswer(i -> usuario.getEmail());
        datos.setEmail(" NUEVO@EXAMPLE.COM ");

        UsuarioDTO resultado = usuarioService.actualizarUsuario(1, datos);

        assertEquals("nuevo@example.com", usuario.getEmail());
        assertEquals("nuevo@example.com", resultado.getEmail());
        assertNull(usuario.getTokenRecuperacion());
        assertNull(usuario.getVencimientoTokenRecuperacion());
        verify(auditoria).registrar(administrador, "USUARIO", 1, "ACTUALIZAR",
                "ana@example.com", "nuevo@example.com");
    }

    @Test
    void rechazaUsuarioInexistente() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> usuarioService.actualizarUsuario(1, datos)
        );

        comprobarQueNoSeInicioEscritura();
        verifyNoInteractions(generoRepository);
    }

    @Test
    void rechazaUsuarioInactivo() {
        usuarioExiste();
        usuario.setActivo(false);

        assertThrows(
                EntityNotFoundException.class,
                () -> usuarioService.actualizarUsuario(1, datos)
        );

        comprobarQueNoSeInicioEscritura();
        verifyNoInteractions(generoRepository);
    }

    @Test
    void rechazaUsuarioSinPersona() {
        usuarioExiste();
        usuario.setPersona(null);

        assertThrows(
                IllegalStateException.class,
                () -> usuarioService.actualizarUsuario(1, datos)
        );

        comprobarQueNoSeInicioEscritura();
        verifyNoInteractions(generoRepository);
    }

    @Test
    void rechazaGeneroInexistente() {
        usuarioExiste();
        when(generoRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(1, datos)
        );

        comprobarQueNoSeInicioEscritura();
    }

    @Test
    void rechazaEmailOcupadoSinModificarDatos() {
        prepararBusquedasValidas();
        datos.setEmail("ocupado@example.com");

        when(usuarioRepository.existsByEmailIgnoreCaseAndIdNot(
                "ocupado@example.com", 1
        )).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> usuarioService.actualizarUsuario(1, datos)
        );

        assertEquals("ana@example.com", usuario.getEmail());
        assertEquals("Ana", usuario.getPersona().getNombre());
        assertEquals("a".repeat(64), usuario.getTokenRecuperacion());
        comprobarQueNoSeInicioEscritura();
    }

    @Test
    void propagaConflictoDeVersionAntesDeModificarDatos() {
        prepararBusquedasValidas();
        datos.setEmail("nuevo@example.com");

        doThrow(new OptimisticLockException("Conflicto simulado"))
                .when(entityManager)
                .lock(usuario, LockModeType.OPTIMISTIC_FORCE_INCREMENT);

        assertThrows(
                OptimisticLockException.class,
                () -> usuarioService.actualizarUsuario(1, datos)
        );

        assertEquals("ana@example.com", usuario.getEmail());
        assertEquals("Ana", usuario.getPersona().getNombre());
        verify(usuarioRepository, never()).flush();
        comprobarQueNoSeAudito();
    }

    @Test
    void propagaConflictoDeIntegridadAlGuardar() {
        prepararBusquedasValidas();

        doThrow(new DataIntegrityViolationException("Conflicto simulado"))
                .when(usuarioRepository).flush();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> usuarioService.actualizarUsuario(1, datos)
        );
        comprobarQueNoSeAudito();
    }

    @Test
    void sinAdministradorVigenteNoConsultaNiEditaDatosPersonales() {
        when(auditoria.administradorActual()).thenThrow(new AccessDeniedException("ADMIN revocado"));
        assertThrows(AccessDeniedException.class, () -> usuarioService.actualizarUsuario(1, datos));
        verifyNoInteractions(usuarioRepository, generoRepository, entityManager);
        comprobarQueNoSeAudito();
    }

    @Test
    void falloDeAuditoriaSePropagaDespuesDelFlushSinDevolverExito() {
        prepararBusquedasValidas();
        when(auditoria.datos(usuario)).thenAnswer(i -> usuario.getPersona().getNombre());
        doThrow(new IllegalStateException("Auditoría no disponible")).when(auditoria)
                .registrar(administrador, "USUARIO", 1, "ACTUALIZAR", "Ana", "Ana Maria");

        assertThrows(IllegalStateException.class, () -> usuarioService.actualizarUsuario(1, datos));
        verify(usuarioRepository).flush();
    }

    @Test
    void validaLosCincoCamposDeActualizacion() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(datos).isEmpty());

            String[] campos = {
                    "nombre", "apellido", "email",
                    "fechaNacimiento", "generoId"
            };

            ActualizarUsuarioDTO vacio = new ActualizarUsuarioDTO();

            for (String campo : campos) {
                assertFalse(
                        validator.validateProperty(vacio, campo).isEmpty(),
                        "Debe exigir el campo: " + campo
                );
            }

            datos.setNombre("123");
            datos.setApellido("Lopez@");
            datos.setEmail("ana@ejemplo");
            datos.setFechaNacimiento(LocalDate.now().plusDays(1));
            datos.setGeneroId(0);

            for (String campo : campos) {
                assertFalse(
                        validator.validateProperty(datos, campo).isEmpty(),
                        "Debe rechazar el valor del campo: " + campo
                );
            }
        }
    }

    private void usuarioExiste() {
        when(usuarioRepository.findById(1))
                .thenReturn(Optional.of(usuario));
    }

    private void prepararBusquedasValidas() {
        usuarioExiste();
        when(generoRepository.findById(2))
                .thenReturn(Optional.of(generoNuevo));
    }

    private void comprobarQueNoSeInicioEscritura() {
        verify(usuarioRepository, never()).flush();
        verifyNoInteractions(entityManager);
        comprobarQueNoSeAudito();
    }

    private void comprobarQueNoSeAudito() {
        verify(auditoria, never()).registrar(any(), anyString(), anyInt(), anyString(), any(), any());
    }
}
