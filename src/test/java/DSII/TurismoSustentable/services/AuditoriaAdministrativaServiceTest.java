package DSII.TurismoSustentable.services;

import java.math.BigDecimal;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditoriaAdministrativaServiceTest {

    @Mock UsuarioRepository usuarios;
    @Mock TrazabilidadInventarioService trazabilidad;
    private final ObjectMapper json = new ObjectMapper();
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-16T20:00:00Z"), ZoneOffset.UTC);
    private AuditoriaAdministrativaService servicio;
    private Usuario administrador;

    @BeforeEach
    void preparar() {
        servicio = new AuditoriaAdministrativaService(usuarios, trazabilidad, json, reloj);
        administrador = new Usuario();
        administrador.setId(99);
        administrador.setEmail("admin.auditoria@example.com");
        administrador.setActivo(true);
        Rol rol = new Rol("ADMIN");
        ReflectionTestUtils.setField(rol, "id", 1);
        administrador.getUsuarioRoles().add(new UsuarioRol(administrador, rol));
        sesion("ADMIN");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void identificaAdministradorDesdeSesionYRolPersistido() {
        when(usuarios.buscarPorEmailConDatosYRoles("admin.auditoria@example.com"))
                .thenReturn(Optional.of(administrador));
        assertSame(administrador, servicio.administradorActual());
        verifyNoInteractions(trazabilidad);
    }

    @Test
    void sinSesionNoConsultaNiAudita() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, servicio::administradorActual);
        verifyNoInteractions(usuarios, trazabilidad);
    }

    @Test
    void autoridadClienteNoPuedeUsarLaAuditoriaAdministrativa() {
        sesion("CLIENTE");
        assertThrows(AccessDeniedException.class, servicio::administradorActual);
        verifyNoInteractions(usuarios, trazabilidad);
    }

    @Test
    void autoridadAntiguaNoSustituyeElRolActualEnBase() {
        administrador.getUsuarioRoles().clear();
        when(usuarios.buscarPorEmailConDatosYRoles("admin.auditoria@example.com"))
                .thenReturn(Optional.of(administrador));
        assertThrows(AccessDeniedException.class, servicio::administradorActual);
        verifyNoInteractions(trazabilidad);
    }

    @Test
    void cuentaInactivaNoPuedeAuditar() {
        administrador.setActivo(false);
        when(usuarios.buscarPorEmailConDatosYRoles("admin.auditoria@example.com"))
                .thenReturn(Optional.of(administrador));
        assertThrows(AccessDeniedException.class, servicio::administradorActual);
        verifyNoInteractions(trazabilidad);
    }

    @Test
    void cuentaInexistenteNoPuedeAuditar() {
        when(usuarios.buscarPorEmailConDatosYRoles("admin.auditoria@example.com"))
                .thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, servicio::administradorActual);
        verifyNoInteractions(trazabilidad);
    }

    @Test
    void registraActorValoresYFechaUtcSinInventarMotivo() {
        servicio.registrar(administrador, "ESTACION", 5, "ACTUALIZAR", "ANTES", "DESPUES");
        verify(trazabilidad).administracion(administrador, "ESTACION", 5, "ACTUALIZAR",
                "ANTES", "DESPUES", null, LocalDateTime.of(2026, 11, 16, 20, 0));
        verifyNoInteractions(usuarios);
    }

    @Test
    void valoresSinCambioNoGeneranUnaAuditoriaFalsa() {
        servicio.registrar(administrador, "ESTACION", 5, "ACTUALIZAR", "IGUAL", "IGUAL");
        verifyNoInteractions(trazabilidad);
    }

    @Test
    void registrarNoPermiteUsarUnActorAdminDesdeSesionTecnica() {
        sesion("TECNICO");
        assertThrows(AccessDeniedException.class,
                () -> servicio.registrar(administrador, "ESTACION", 5, "ACTUALIZAR", "ANTES", "DESPUES"));
        verifyNoInteractions(trazabilidad);
    }

    @Test
    void registraCambioDeEmailPropioSinPerderLaIdentidadCapturada() {
        when(usuarios.buscarPorEmailConDatosYRoles("admin.auditoria@example.com"))
                .thenReturn(Optional.of(administrador));
        Usuario actor = servicio.administradorActual();
        actor.setEmail("nuevo.admin@example.com");
        servicio.registrar(actor, "USUARIO", 99, "ACTUALIZAR", "ANTES", "DESPUES");
        verify(usuarios).buscarPorEmailConDatosYRoles("admin.auditoria@example.com");
        verifyNoMoreInteractions(usuarios);
        verify(trazabilidad).administracion(actor, "USUARIO", 99, "ACTUALIZAR",
                "ANTES", "DESPUES", null, LocalDateTime.of(2026, 11, 16, 20, 0));
    }

    @Test
    void snapshotDeUsuarioContieneSoloDatosEditablesSinCredenciales() throws Exception {
        Genero genero = new Genero("OTRO");
        ReflectionTestUtils.setField(genero, "id", 2);
        administrador.setPersona(new Persona("Ana", "Prueba", LocalDate.of(1990, 1, 1),
                genero, new TipoDocumento("DNI", "DNI"), "12345678", new Pais("AR", "Argentina")));
        administrador.setPassword("HASH_SECRETO_DE_PRUEBA");
        administrador.setTokenRecuperacion("TOKEN_SECRETO_DE_PRUEBA");
        administrador.setVencimientoTokenRecuperacion(LocalDateTime.of(2030, 1, 1, 0, 0));

        String snapshot = servicio.datos(administrador);
        var datos = json.readTree(snapshot);
        assertEquals(6, datos.size());
        assertEquals(99, datos.get("id").asInt());
        assertEquals("admin.auditoria@example.com", datos.get("email").asText());
        assertEquals("Ana", datos.get("nombre").asText());
        assertEquals("Prueba", datos.get("apellido").asText());
        assertEquals("1990-01-01", datos.get("fechaNacimiento").asText());
        assertEquals(2, datos.get("generoId").asInt());
        assertFalse(snapshot.contains("SECRETO"));
        assertFalse(datos.has("password"));
        assertFalse(datos.has("tokenRecuperacion"));
        assertFalse(datos.has("vencimientoTokenRecuperacion"));
        assertFalse(datos.has("numeroDocumento"));
        verifyNoInteractions(usuarios, trazabilidad);
    }

    @Test
    void snapshotDeEstacionNoIncluyeVersionNiColecciones() throws Exception {
        Estacion estacion = new Estacion("Taller QA", "Calle 1", 2, new BigDecimal("0.0"), new BigDecimal("0.0"));
        ReflectionTestUtils.setField(estacion, "id", 5);
        estacion.setEsPublica(false);
        var datos = json.readTree(servicio.datos(estacion));
        assertEquals(8, datos.size());
        assertEquals(5, datos.get("id").asInt());
        assertEquals(2, datos.get("capacidad").asInt());
        assertFalse(datos.get("esPublica").asBoolean());
        assertFalse(datos.get("borradoEstacion").asBoolean());
        assertFalse(datos.has("version"));
        assertFalse(datos.has("bicicletasEnEstacion"));
    }

    @Test
    void falloDeSerializacionEsInternoYNoGeneraAuditoria() throws Exception {
        ObjectMapper fallido = mock(ObjectMapper.class);
        when(fallido.writeValueAsString(any())).thenThrow(new JsonProcessingException("Error QA") {});
        var servicioFallido = new AuditoriaAdministrativaService(usuarios, trazabilidad, fallido, reloj);
        assertThrows(ConfiguracionSistemaException.class, () -> servicioFallido.datos(administrador));
        verifyNoInteractions(trazabilidad);
    }

    @Test
    void falloDePersistenciaSePropagaParaAbortarLaOperacion() {
        doThrow(new IllegalStateException("Fallo de auditoría QA")).when(trazabilidad)
                .administracion(any(), anyString(), anyInt(), anyString(), any(), any(), any(), any());
        assertThrows(IllegalStateException.class,
                () -> servicio.registrar(administrador, "ESTACION", 5, "CREAR", null, "NUEVA"));
    }

    @Test
    void requiereLaTransaccionDelServicioNoUnaIndependiente() {
        Transactional configuracion = AuditoriaAdministrativaService.class.getAnnotation(Transactional.class);
        assertNotNull(configuracion);
        assertEquals(Propagation.MANDATORY, configuracion.propagation());
    }

    private void sesion(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin.auditoria@example.com", null, List.of(new SimpleGrantedAuthority(rol))));
    }
}
