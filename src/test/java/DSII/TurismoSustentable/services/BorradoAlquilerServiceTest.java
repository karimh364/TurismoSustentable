package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.CodigoEstadoAlquiler;
import DSII.TurismoSustentable.models.EstadoAlquiler;
import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.repositories.AlquilerDetalleRepository;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.HistorialEstadoDetalleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorradoAlquilerServiceTest {

    @Mock
    private AlquilerRepository alquilerRepository;

    @Mock
    private AlquilerDetalleRepository alquilerDetalleRepository;

    @Mock
    private BicicletaRepository bicicletaRepository;

    @Mock
    private HistorialEstadoDetalleRepository historialEstadoDetalleRepository;

    @Mock
    private TrazabilidadInventarioService trazabilidadInventario;

    @Mock
    private CorreoService correoService;

    @InjectMocks
    private AlquilerService servicio;

    @ParameterizedTest
    @ValueSource(strings = {"RESERVADO", "EN_PROGRESO"})
    void rechazaContratacionNuevaActiva(String codigo) {
        Alquiler alquiler = prepararAlquiler();

        alquiler.setEstadoContratacion(new EstadoAlquiler(codigo));

        // El estado nuevo debe protegerla incluso si el legado
        // contiene un valor que antes permitía ocultarla.
        alquiler.setEstado(CodigoEstadoAlquiler.FINALIZADO);

        assertThrows(
                IllegalStateException.class,
                () -> servicio.borrarAlquilerLogico(50)
        );

        assertFalse(alquiler.isBorradoAlquiler());
        assertEquals(
                codigo,
                alquiler.getEstadoContratacion().getCodigo()
        );

        comprobarSinGuardado();
        comprobarSinEfectosLaterales();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "FINALIZADO", "CANCELADO", "VENCIDO"
    })
    void ocultaContratacionNuevaCerradaSinAlterarImportes(
            String codigo
    ) throws Exception {
        Alquiler alquiler = prepararAlquiler();

        alquiler.setEstadoContratacion(new EstadoAlquiler(codigo));
        alquiler.setCostoEstimadoArs(new BigDecimal("1800.00"));
        alquiler.setCostoFinalArs(new BigDecimal("1200.00"));

        servicio.borrarAlquilerLogico(50);

        assertTrue(alquiler.isBorradoAlquiler());
        assertEquals(
                codigo,
                alquiler.getEstadoContratacion().getCodigo()
        );
        assertEquals(
                new BigDecimal("1800.00"),
                alquiler.getCostoEstimadoArs()
        );
        assertEquals(
                new BigDecimal("1200.00"),
                alquiler.getCostoFinalArs()
        );

        // Ocultar no inventa una liquidación ni una cancelación.
        assertNull(alquiler.getFechaHoraLiquidacion());
        assertNull(alquiler.getFechaHoraCancelacion());

        verify(alquilerRepository).buscarPorIdConBloqueo(50);
        verify(alquilerRepository).save(alquiler);
        verifyNoMoreInteractions(alquilerRepository);

        comprobarSinEfectosLaterales();
    }

    @Test
    void rechazaAlquilerLegadoEnCurso() {
        Alquiler alquiler = prepararAlquiler();
        alquiler.setEstado(CodigoEstadoAlquiler.INICIADO);

        assertThrows(
                IllegalStateException.class,
                () -> servicio.borrarAlquilerLogico(50)
        );

        assertFalse(alquiler.isBorradoAlquiler());
        assertEquals(
                CodigoEstadoAlquiler.INICIADO,
                alquiler.getEstado()
        );

        comprobarSinGuardado();
        comprobarSinEfectosLaterales();
    }

    @ParameterizedTest
    @EnumSource(
            value = CodigoEstadoAlquiler.class,
            names = {"FINALIZADO", "CANCELADO", "RECHAZADO"}
    )
    void conservaBorradoDelLegadoCerrado(
            CodigoEstadoAlquiler estado
    ) throws Exception {
        Alquiler alquiler = prepararAlquiler();
        alquiler.setEstado(estado);

        servicio.borrarAlquilerLogico(50);

        assertTrue(alquiler.isBorradoAlquiler());
        assertEquals(estado, alquiler.getEstado());
        assertNull(alquiler.getEstadoContratacion());

        verify(alquilerRepository).save(alquiler);
        comprobarSinEfectosLaterales();
    }

    @Test
    void rechazaRegistroSinNingunEstado() {
        Alquiler alquiler = prepararAlquiler();

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> servicio.borrarAlquilerLogico(50)
        );

        assertFalse(alquiler.isBorradoAlquiler());

        comprobarSinGuardado();
        comprobarSinEfectosLaterales();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rechazaRegistroInexistenteOYaOculto(
            boolean existePeroEstaOculto
    ) {
        Alquiler alquiler = new Alquiler();
        alquiler.setId(50);
        alquiler.setBorradoAlquiler(true);

        when(alquilerRepository.buscarPorIdConBloqueo(50))
                .thenReturn(
                        existePeroEstaOculto
                                ? Optional.of(alquiler)
                                : Optional.empty()
                );

        Exception error = assertThrows(
                Exception.class,
                () -> servicio.borrarAlquilerLogico(50)
        );

        assertEquals(
                "El alquiler con ID 50 no existe.",
                error.getMessage()
        );

        comprobarSinGuardado();
        comprobarSinEfectosLaterales();
    }

    private Alquiler prepararAlquiler() {
        Alquiler alquiler = new Alquiler();
        alquiler.setId(50);

        when(alquilerRepository.buscarPorIdConBloqueo(50))
                .thenReturn(Optional.of(alquiler));

        return alquiler;
    }

    private void comprobarSinGuardado() {
        verify(alquilerRepository, never())
                .save(any(Alquiler.class));
    }

    private void comprobarSinEfectosLaterales() {
        verifyNoInteractions(
                alquilerDetalleRepository,
                bicicletaRepository,
                historialEstadoDetalleRepository,
                trazabilidadInventario,
                correoService
        );

        verify(alquilerRepository, never())
                .delete(any(Alquiler.class));

        verify(alquilerRepository, never()).deleteById(anyInt());
    }
}
