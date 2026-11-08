package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.BicicletaDTO;
import DSII.TurismoSustentable.models.Bicicleta;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.models.CodigoEstadoBicicleta;
import DSII.TurismoSustentable.models.CodigoTipoBicicleta;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BicicletaControllerTest {

    private static final BigDecimal PRECIO_ORIGINAL =
            new BigDecimal("9.00");

    private static final BigDecimal PRECIO_NUEVO =
            new BigDecimal("12.00");

    @Mock
    private BicicletaRepository bicicletaRepository;

    @Mock
    private EstacionRepository estacionRepository;

    @InjectMocks
    private BicicletaController controlador;

    private Estacion origen;
    private Estacion destino;
    private Bicicleta bicicleta;
    private Bicicleta solicitud;

    @BeforeEach
    void prepararDatos() {
        origen = crearEstacion(1, "Plaza QA", 4, true);
        destino = crearEstacion(2, "Depósito QA", 1, false);

        bicicleta = new Bicicleta(
                "QA-B01",
                CodigoTipoBicicleta.MONTANA,
                CodigoEstadoBicicleta.DISPONIBLE,
                PRECIO_ORIGINAL,
                origen
        );
        bicicleta.setId(1);

        solicitud = new Bicicleta(
                "QA-NUEVO",
                CodigoTipoBicicleta.PASEO,
                CodigoEstadoBicicleta.MANTENIMIENTO,
                PRECIO_NUEVO,
                destino
        );
    }

    @Test
    void rechazaEstadoAlquiladaSinCambiarDatos() {
        prepararBicicletaEncontrada();
        solicitud.setEstado(CodigoEstadoBicicleta.ALQUILADA);

        ResponseEntity<Object> respuesta =
                controlador.actualizarBicicleta(1, solicitud);

        comprobarRechazoSinCambios(respuesta, HttpStatus.CONFLICT);

        verifyNoMoreInteractions(bicicletaRepository);
        verifyNoInteractions(estacionRepository);
    }

    @Test
    void rechazaDestinoInvalidoSinCambiarDatos() {
        prepararBicicletaEncontrada();
        solicitud.setUbicacionActual(
                crearEstacion(0, "Destino inválido", 1, false)
        );

        ResponseEntity<Object> respuesta =
                controlador.actualizarBicicleta(1, solicitud);

        comprobarRechazoSinCambios(respuesta, HttpStatus.BAD_REQUEST);

        verifyNoMoreInteractions(bicicletaRepository);
        verifyNoInteractions(estacionRepository);
    }

    @Test
    void rechazaDestinoInexistenteSinCambiarDatos() {
        prepararBicicletaEncontrada();

        when(estacionRepository.buscarActivaPorIdConBloqueo(2))
                .thenReturn(Optional.empty());

        ResponseEntity<Object> respuesta =
                controlador.actualizarBicicleta(1, solicitud);

        comprobarRechazoSinCambios(respuesta, HttpStatus.BAD_REQUEST);

        verify(estacionRepository).buscarActivaPorIdConBloqueo(2);
        verifyNoMoreInteractions(
                bicicletaRepository,
                estacionRepository
        );
    }

    @Test
    void rechazaDestinoLlenoContandoArchivadasSinCambiarDatos() {
        prepararBicicletaEncontrada();

        Bicicleta archivada = new Bicicleta(
                "QA-ARCHIVADA",
                CodigoTipoBicicleta.MONTANA,
                CodigoEstadoBicicleta.INACTIVA,
                PRECIO_ORIGINAL,
                destino
        );
        archivada.setBorradoBici(true);
        destino.getBicicletasEnEstacion().add(archivada);

        when(estacionRepository.buscarActivaPorIdConBloqueo(2))
                .thenReturn(Optional.of(destino));

        when(bicicletaRepository.countByUbicacionActual(destino))
                .thenReturn(1L);

        ResponseEntity<Object> respuesta =
                controlador.actualizarBicicleta(1, solicitud);

        comprobarRechazoSinCambios(respuesta, HttpStatus.CONFLICT);

        verify(estacionRepository).buscarActivaPorIdConBloqueo(2);
        verify(bicicletaRepository).countByUbicacionActual(destino);
        verifyNoMoreInteractions(
                bicicletaRepository,
                estacionRepository
        );
    }

    @Test
    void aplicaCambiosCuandoElTrasladoEsValido() {
        prepararBicicletaEncontrada();

        when(estacionRepository.buscarActivaPorIdConBloqueo(2))
                .thenReturn(Optional.of(destino));

        when(bicicletaRepository.countByUbicacionActual(destino))
                .thenReturn(0L);

        when(bicicletaRepository.save(any(Bicicleta.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ResponseEntity<Object> respuesta =
                controlador.actualizarBicicleta(1, solicitud);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("QA-NUEVO", bicicleta.getCodigoBicicleta());
        assertEquals(CodigoEstadoBicicleta.MANTENIMIENTO, bicicleta.getEstado());
        assertEquals(CodigoTipoBicicleta.PASEO, bicicleta.getTipo());
        assertEquals(PRECIO_NUEVO, bicicleta.getPrecioPorHora());
        assertSame(destino, bicicleta.getUbicacionActual());

        BicicletaDTO dto = (BicicletaDTO) respuesta.getBody();
        assertNotNull(dto);
        assertEquals(1, dto.getId());
        assertEquals("QA-NUEVO", dto.getCodigoBicicleta());
        assertEquals("MANTENIMIENTO", dto.getEstado());
        assertEquals("PASEO", dto.getTipo());
        assertEquals("Depósito QA", dto.getUbicacion());
        assertEquals(PRECIO_NUEVO, dto.getPrecio());

        verify(bicicletaRepository).findById(1);
        verify(estacionRepository).buscarActivaPorIdConBloqueo(2);
        verify(bicicletaRepository).countByUbicacionActual(destino);
        verify(bicicletaRepository).save(same(bicicleta));
        verifyNoMoreInteractions(
                bicicletaRepository,
                estacionRepository
        );
    }

    @Test
    void alquiladaConservaSusDatosYPermiteCambiarPrecio() {
        bicicleta.setEstado(CodigoEstadoBicicleta.ALQUILADA);
        bicicleta.setUbicacionActual(null);
        prepararBicicletaEncontrada();

        when(bicicletaRepository.save(any(Bicicleta.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ResponseEntity<Object> respuesta =
                controlador.actualizarBicicleta(1, solicitud);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("QA-B01", bicicleta.getCodigoBicicleta());
        assertEquals(CodigoEstadoBicicleta.ALQUILADA, bicicleta.getEstado());
        assertEquals(CodigoTipoBicicleta.MONTANA, bicicleta.getTipo());
        assertEquals(PRECIO_NUEVO, bicicleta.getPrecioPorHora());
        assertNull(bicicleta.getUbicacionActual());
        assertFalse(bicicleta.isBorradoBici());

        BicicletaDTO dto = (BicicletaDTO) respuesta.getBody();
        assertNotNull(dto);
        assertEquals("QA-B01", dto.getCodigoBicicleta());
        assertEquals("ALQUILADA", dto.getEstado());
        assertEquals("MONTANA", dto.getTipo());
        assertEquals("Sin Asignar", dto.getUbicacion());
        assertEquals(PRECIO_NUEVO, dto.getPrecio());

        verify(bicicletaRepository).findById(1);
        verify(bicicletaRepository).save(same(bicicleta));
        verifyNoMoreInteractions(bicicletaRepository);
        verifyNoInteractions(estacionRepository);
    }

    private Estacion crearEstacion(
            int id,
            String nombre,
            int capacidad,
            boolean esPublica
    ) {
        Estacion estacion = new Estacion(
                nombre, "Calle QA", capacidad, 0.0, 0.0
        );
        estacion.setEsPublica(esPublica);
        ReflectionTestUtils.setField(estacion, "id", id);
        ReflectionTestUtils.setField(estacion, "version", 0);
        return estacion;
    }

    private void prepararBicicletaEncontrada() {
        when(bicicletaRepository.findById(1))
                .thenReturn(Optional.of(bicicleta));
    }

    private void comprobarRechazoSinCambios(
            ResponseEntity<Object> respuesta,
            HttpStatus esperado
    ) {
        assertEquals(esperado, respuesta.getStatusCode());
        assertEquals(1, bicicleta.getId());
        assertEquals("QA-B01", bicicleta.getCodigoBicicleta());
        assertEquals(CodigoEstadoBicicleta.DISPONIBLE, bicicleta.getEstado());
        assertEquals(CodigoTipoBicicleta.MONTANA, bicicleta.getTipo());
        assertEquals(PRECIO_ORIGINAL, bicicleta.getPrecioPorHora());
        assertSame(origen, bicicleta.getUbicacionActual());
        assertFalse(bicicleta.isBorradoBici());

        verify(bicicletaRepository).findById(1);
        verify(bicicletaRepository, never()).save(any(Bicicleta.class));
    }
}
