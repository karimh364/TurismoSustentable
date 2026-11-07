package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.ActualizarEstacionDTO;
import DSII.TurismoSustentable.dto.CrearEstacionDTO;
import DSII.TurismoSustentable.dto.EstacionDTO;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @InjectMocks
    private EstacionService estacionService;

    private Estacion estacion;
    private ActualizarEstacionDTO datos;

    @BeforeEach
    void prepararDatos() {
        estacion = new Estacion(
                "Plaza Original",
                "Calle 1",
                6,
                -34.0,
                -58.0
        );

        ReflectionTestUtils.setField(estacion, "id", 1);
        ReflectionTestUtils.setField(estacion, "version", 0);

        datos = new ActualizarEstacionDTO();
        datos.setNombre(" Plaza Nueva ");
        datos.setDireccion(" Calle 2 ");
        datos.setCapacidad(8);
        datos.setLatitud(0.0);
        datos.setLongitud(0.0);
        datos.setVersion(0);
    }

    @Test
    void actualizaCamposPermitidosConVersionCero() {
        prepararEstacionEncontrada();

        when(bicicletaRepository.countByUbicacionActual(estacion))
                .thenReturn(0L);

        EstacionDTO resultado =
                estacionService.actualizarEstacion(1, datos);

        assertEquals(1, resultado.getId());
        assertEquals("Plaza Nueva", resultado.getNombre());
        assertEquals("Calle 2", resultado.getDireccion());
        assertEquals(8, resultado.getCapacidad());
        assertEquals(Double.valueOf(0.0), resultado.getLatitud());
        assertEquals(Double.valueOf(0.0), resultado.getLongitud());
        assertEquals(Integer.valueOf(0), resultado.getVersion());

        assertFalse(estacion.isBorradoEstacion());

        verify(estacionRepository)
                .existsByNombreIgnoreCaseAndIdNot("Plaza Nueva", 1);

        verify(bicicletaRepository)
                .countByUbicacionActual(estacion);

        verify(bicicletaRepository, never())
                .countByUbicacionActualAndBorradoBiciFalse(any());

        verify(estacionRepository).flush();
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
                "Taller QA", "Calle 2", 8, 0.0, 0.0
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
    }

    private CrearEstacionDTO datosDeAlta(boolean esPublica) {
        CrearEstacionDTO alta = new CrearEstacionDTO();
        alta.setNombre("  Estación   QA  ");
        alta.setDireccion("  Calle   QA  ");
        alta.setCapacidad(4);
        alta.setLatitud(0.0);
        alta.setLongitud(0.0);
        alta.setEsPublica(esPublica);
        return alta;
    }

    private void comprobarAlta(boolean esPublica) {
        CrearEstacionDTO alta = datosDeAlta(esPublica);

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
        assertEquals(Double.valueOf(0.0), resultado.getLatitud());
        assertEquals(Double.valueOf(0.0), resultado.getLongitud());
        assertEquals(esPublica, resultado.isEsPublica());
        assertEquals(Integer.valueOf(0), resultado.getVersion());
        assertEquals(0, resultado.getBicicletasDisponibles());
        assertEquals(4, resultado.getEspaciosLibres());

        verify(estacionRepository)
                .existsByNombreIgnoreCase("Estación QA");
        verify(estacionRepository).saveAndFlush(any(Estacion.class));
        verifyNoMoreInteractions(estacionRepository);
        verifyNoInteractions(bicicletaRepository);
    }

    private void prepararEstacionEncontrada() {
        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(estacion));
    }

    private void comprobarQueNoSeModifico() {
        assertEquals("Plaza Original", estacion.getNombre());
        assertEquals("Calle 1", estacion.getDireccion());
        assertEquals(6, estacion.getCapacidad());
        assertEquals(Double.valueOf(-34.0), estacion.getLatitud());
        assertEquals(Double.valueOf(-58.0), estacion.getLongitud());
        assertEquals(Integer.valueOf(0), estacion.getVersion());
        assertFalse(estacion.isBorradoEstacion());

        verify(estacionRepository, never()).flush();
    }
}
