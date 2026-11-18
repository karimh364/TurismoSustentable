package DSII.TurismoSustentable.services;

import java.math.BigDecimal;

import DSII.TurismoSustentable.dto.ActualizarEstacionDTO;
import DSII.TurismoSustentable.dto.CrearEstacionDTO;
import DSII.TurismoSustentable.dto.EstacionDTO;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.models.Bicicleta;
import DSII.TurismoSustentable.models.Usuario;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.access.AccessDeniedException;

import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstacionServiceTest {

    @Mock
    private EstacionRepository estacionRepository;

    @Mock
    private BicicletaRepository bicicletaRepository;

    @Mock
    private AuditoriaAdministrativaService auditoria;

    @InjectMocks
    private EstacionService estacionService;

    private Estacion estacion;
    private ActualizarEstacionDTO datos;
    private Usuario administrador;

    @BeforeEach
    void prepararDatos() {
        administrador = new Usuario();
        administrador.setId(99);
        lenient().when(auditoria.administradorActual()).thenReturn(administrador);
        estacion = new Estacion(
                "Plaza Original",
                "Calle 1",
                6,
                new BigDecimal("-34.0"),
                new BigDecimal("-58.0")
        );

        ReflectionTestUtils.setField(estacion, "id", 1);
        ReflectionTestUtils.setField(estacion, "version", 0);

        datos = new ActualizarEstacionDTO();
        datos.setNombre(" Plaza Nueva ");
        datos.setDireccion(" Calle 2 ");
        datos.setCapacidad(8);
        datos.setLatitud(new BigDecimal("0.0"));
        datos.setLongitud(new BigDecimal("0.0"));
        datos.setVersion(0);
    }

    @Test
    void actualizaCamposPermitidosConVersionCero() {
        prepararEstacionEncontrada();
        when(auditoria.datos(estacion)).thenAnswer(i ->
                estacion.getNombre() + "|" + estacion.getCapacidad());

        when(bicicletaRepository.countByUbicacionActual(estacion))
                .thenReturn(0L);

        EstacionDTO resultado =
                estacionService.actualizarEstacion(1, datos);

        assertEquals(1, resultado.getId());
        assertEquals("Plaza Nueva", resultado.getNombre());
        assertEquals("Calle 2", resultado.getDireccion());
        assertEquals(8, resultado.getCapacidad());
        assertEquals(new BigDecimal("0.0"), resultado.getLatitud());
        assertEquals(new BigDecimal("0.0"), resultado.getLongitud());
        assertEquals(Integer.valueOf(0), resultado.getVersion());

        assertFalse(estacion.isBorradoEstacion());

        verify(estacionRepository)
                .existsByNombreIgnoreCaseAndIdNot("Plaza Nueva", 1);

        verify(bicicletaRepository)
                .countByUbicacionActual(estacion);

        verify(bicicletaRepository, never())
                .countByUbicacionActualAndBorradoBiciFalse(any());

        verify(estacionRepository).flush();
        verify(auditoria).registrar(administrador, "ESTACION", 1, "ACTUALIZAR",
                "Plaza Original|6", "Plaza Nueva|8");
    }

    @Test
    void rechazaEstacionNoEncontrada() {
        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> estacionService.actualizarEstacion(1, datos)
        );

        comprobarQueNoSeModifico();
        verifyNoInteractions(bicicletaRepository);
    }

    @Test
    void rechazaVersionDesactualizada() {
        prepararEstacionEncontrada();
        datos.setVersion(1);

        assertThrows(
                OptimisticLockException.class,
                () -> estacionService.actualizarEstacion(1, datos)
        );

        comprobarQueNoSeModifico();
        verifyNoInteractions(bicicletaRepository);

        verify(estacionRepository, never())
                .existsByNombreIgnoreCaseAndIdNot(anyString(), anyInt());
    }

    @Test
    void rechazaNombreOcupadoPorOtraEstacion() {
        prepararEstacionEncontrada();

        when(estacionRepository.existsByNombreIgnoreCaseAndIdNot(
                "Plaza Nueva", 1
        )).thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> estacionService.actualizarEstacion(1, datos)
        );

        comprobarQueNoSeModifico();
        verifyNoInteractions(bicicletaRepository);
    }

    @Test
    void rechazaCapacidadMenorQueLaOcupacionFisica() {
        prepararEstacionEncontrada();
        datos.setCapacidad(2);

        when(bicicletaRepository.countByUbicacionActual(estacion))
                .thenReturn(3L);

        assertThrows(
                IllegalStateException.class,
                () -> estacionService.actualizarEstacion(1, datos)
        );

        comprobarQueNoSeModifico();

        verify(bicicletaRepository)
                .countByUbicacionActual(estacion);
    }

    @Test
    void lecturaPublicaUtilizaLaConsultaFiltrada() {
        when(estacionRepository
                .findByBorradoEstacionFalseAndEsPublicaTrue())
                .thenReturn(List.of(estacion));

        List<EstacionDTO> resultado =
                estacionService.obtenerEstacionesPublicas();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).isEsPublica());

        verify(estacionRepository)
                .findByBorradoEstacionFalseAndEsPublicaTrue();

        verify(estacionRepository, never())
                .findByBorradoEstacionFalse();

        verifyNoInteractions(bicicletaRepository);
    }

    @Test
    void lecturaAdministrativaIncluyeUbicacionesInternas() {
        Estacion taller = new Estacion(
                "Taller QA", "Calle 2", 8, new BigDecimal("0.0"), new BigDecimal("0.0")
        );

        taller.setEsPublica(false);
        ReflectionTestUtils.setField(taller, "id", 2);
        ReflectionTestUtils.setField(taller, "version", 0);

        when(estacionRepository.findByBorradoEstacionFalse())
                .thenReturn(List.of(estacion, taller));

        List<EstacionDTO> resultado =
                estacionService.obtenerEstacionesAdministracion();

        assertEquals(2, resultado.size());
        assertTrue(resultado.get(0).isEsPublica());
        assertFalse(resultado.get(1).isEsPublica());
        assertEquals("Taller QA", resultado.get(1).getNombre());

        verify(estacionRepository).findByBorradoEstacionFalse();

        verify(estacionRepository, never())
                .findByBorradoEstacionFalseAndEsPublicaTrue();

        verifyNoInteractions(bicicletaRepository);
        verifyNoInteractions(auditoria);
    }

    @Test
    void creaEstacionPublica() {
        comprobarAlta(true);
    }

    @Test
    void creaEstacionDeUsoInterno() {
        comprobarAlta(false);
    }

    @Test
    void rechazaAltaConNombreDuplicadoSinGuardar() {
        CrearEstacionDTO alta = datosDeAlta(true);

        when(estacionRepository.existsByNombreIgnoreCase("Estación QA"))
                .thenReturn(true);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> estacionService.crearEstacion(alta)
        );

        assertEquals(
                "Ya existe una estación con ese nombre.",
                error.getMessage()
        );

        verify(estacionRepository)
                .existsByNombreIgnoreCase("Estación QA");
        verify(estacionRepository, never())
                .saveAndFlush(any(Estacion.class));
        verifyNoMoreInteractions(estacionRepository);
        verifyNoInteractions(bicicletaRepository);
        comprobarQueNoSeAudito();
    }

    @Test
    void eliminaEstacionVaciaConBloqueoYAuditoriaSinBorradoFisico() {
        prepararEstacionEncontrada();
        when(auditoria.datos(estacion)).thenAnswer(i -> Boolean.toString(estacion.isBorradoEstacion()));

        estacionService.eliminarEstacionLogico(1);

        assertTrue(estacion.isBorradoEstacion());
        var orden = inOrder(estacionRepository, bicicletaRepository, auditoria);
        orden.verify(auditoria).administradorActual();
        orden.verify(estacionRepository).buscarActivaPorIdConBloqueo(1);
        orden.verify(bicicletaRepository).countByUbicacionActual(estacion);
        orden.verify(auditoria).datos(estacion);
        orden.verify(estacionRepository).saveAndFlush(estacion);
        orden.verify(auditoria).datos(estacion);
        orden.verify(auditoria).registrar(administrador, "ESTACION", 1, "ELIMINAR", "false", "true");
        verify(estacionRepository, never()).delete(any(Estacion.class));
    }

    @Test
    void borradoRechazaTambienBicicletasArchivadasFisicamentePresentes() {
        prepararEstacionEncontrada();
        Bicicleta archivada = new Bicicleta();
        archivada.setBorradoBici(true);
        archivada.setUbicacionActual(estacion);
        estacion.getBicicletasEnEstacion().add(archivada);
        when(bicicletaRepository.countByUbicacionActual(estacion)).thenReturn(1L);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> estacionService.eliminarEstacionLogico(1));

        assertTrue(error.getMessage().contains("contiene 1 bicicletas"));
        assertFalse(estacion.isBorradoEstacion());
        verify(bicicletaRepository, never()).countByUbicacionActualAndBorradoBiciFalse(any());
        verify(estacionRepository, never()).saveAndFlush(any(Estacion.class));
        comprobarQueNoSeAudito();
    }

    @Test
    void borrarEstacionInexistenteNoEscribeNiAudita() {
        when(estacionRepository.buscarActivaPorIdConBloqueo(1)).thenReturn(Optional.empty());
        EntityNotFoundException error = assertThrows(EntityNotFoundException.class,
                () -> estacionService.eliminarEstacionLogico(1));
        assertEquals("La estación con ID 1 no existe o está inactiva.", error.getMessage());
        verifyNoInteractions(bicicletaRepository);
        verify(estacionRepository, never()).saveAndFlush(any(Estacion.class));
        comprobarQueNoSeAudito();
    }

    @Test
    void sinAdministradorVigenteNoConsultaNiModificaLaEstacion() {
        when(auditoria.administradorActual()).thenThrow(new AccessDeniedException("ADMIN revocado"));
        assertThrows(AccessDeniedException.class, () -> estacionService.actualizarEstacion(1, datos));
        verifyNoInteractions(estacionRepository, bicicletaRepository);
        comprobarQueNoSeAudito();
    }

    @Test
    void borradoPropagaFalloDeAuditoriaEnLaMismaOperacion() {
        prepararEstacionEncontrada();
        when(auditoria.datos(estacion)).thenAnswer(i -> Boolean.toString(estacion.isBorradoEstacion()));
        doThrow(new IllegalStateException("Auditoría no disponible")).when(auditoria)
                .registrar(administrador, "ESTACION", 1, "ELIMINAR", "false", "true");
        assertThrows(IllegalStateException.class, () -> estacionService.eliminarEstacionLogico(1));
        verify(estacionRepository).saveAndFlush(estacion);
    }

    @Test
    void editarPropagaFalloDeAuditoriaSinDevolverExito() {
        prepararEstacionEncontrada();
        when(auditoria.datos(estacion)).thenAnswer(i -> estacion.getNombre());
        doThrow(new IllegalStateException("Auditoría no disponible")).when(auditoria)
                .registrar(administrador, "ESTACION", 1, "ACTUALIZAR", "Plaza Original", "Plaza Nueva");
        assertThrows(IllegalStateException.class, () -> estacionService.actualizarEstacion(1, datos));
        verify(estacionRepository).flush();
    }

    @Test
    void crearPropagaFalloDeAuditoriaSinDevolverAltaExitosa() {
        when(estacionRepository.saveAndFlush(any(Estacion.class))).thenReturn(estacion);
        when(auditoria.datos(estacion)).thenReturn("NUEVA");
        doThrow(new IllegalStateException("Auditoría no disponible")).when(auditoria)
                .registrar(administrador, "ESTACION", 1, "CREAR", null, "NUEVA");

        assertThrows(IllegalStateException.class, () -> estacionService.crearEstacion(datosDeAlta(true)));
        verify(estacionRepository).saveAndFlush(any(Estacion.class));
    }

    private CrearEstacionDTO datosDeAlta(boolean esPublica) {
        CrearEstacionDTO alta = new CrearEstacionDTO();
        alta.setNombre("  Estación   QA  ");
        alta.setDireccion("  Calle   QA  ");
        alta.setCapacidad(4);
        alta.setLatitud(new BigDecimal("0.0"));
        alta.setLongitud(new BigDecimal("0.0"));
        alta.setEsPublica(esPublica);
        return alta;
    }

    private void comprobarAlta(boolean esPublica) {
        CrearEstacionDTO alta = datosDeAlta(esPublica);
        when(auditoria.datos(any(Estacion.class))).thenReturn("ESTACION_NUEVA");

        when(estacionRepository.existsByNombreIgnoreCase("Estación QA"))
                .thenReturn(false);

        when(estacionRepository.saveAndFlush(any(Estacion.class)))
                .thenAnswer(invocacion -> {
                    Estacion creada = invocacion.getArgument(0);

                    assertEquals(0, creada.getId());
                    assertNull(creada.getVersion());
                    assertFalse(creada.isBorradoEstacion());
                    assertEquals(esPublica, creada.isEsPublica());
                    assertTrue(creada.getBicicletasEnEstacion().isEmpty());

                    // Simular los datos generados por JPA, sin usar una base real.
                    ReflectionTestUtils.setField(creada, "id", 2);
                    ReflectionTestUtils.setField(creada, "version", 0);
                    return creada;
                });

        EstacionDTO resultado = estacionService.crearEstacion(alta);

        assertEquals(2, resultado.getId());
        assertEquals("Estación QA", resultado.getNombre());
        assertEquals("Calle QA", resultado.getDireccion());
        assertEquals(4, resultado.getCapacidad());
        assertEquals(new BigDecimal("0.0"), resultado.getLatitud());
        assertEquals(new BigDecimal("0.0"), resultado.getLongitud());
        assertEquals(esPublica, resultado.isEsPublica());
        assertEquals(Integer.valueOf(0), resultado.getVersion());
        assertEquals(0, resultado.getBicicletasDisponibles());
        assertEquals(4, resultado.getEspaciosLibres());

        verify(estacionRepository)
                .existsByNombreIgnoreCase("Estación QA");
        verify(estacionRepository).saveAndFlush(any(Estacion.class));
        verifyNoMoreInteractions(estacionRepository);
        verifyNoInteractions(bicicletaRepository);
        verify(auditoria).registrar(administrador, "ESTACION", 2, "CREAR", null, "ESTACION_NUEVA");
    }

    private void prepararEstacionEncontrada() {
        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(estacion));
    }

    private void comprobarQueNoSeModifico() {
        assertEquals("Plaza Original", estacion.getNombre());
        assertEquals("Calle 1", estacion.getDireccion());
        assertEquals(6, estacion.getCapacidad());
        assertEquals(new BigDecimal("-34.0"), estacion.getLatitud());
        assertEquals(new BigDecimal("-58.0"), estacion.getLongitud());
        assertEquals(Integer.valueOf(0), estacion.getVersion());
        assertFalse(estacion.isBorradoEstacion());

        verify(estacionRepository, never()).flush();
        comprobarQueNoSeAudito();
    }

    private void comprobarQueNoSeAudito() {
        verify(auditoria, never()).registrar(any(), anyString(), anyInt(), anyString(), any(), any());
    }
}
