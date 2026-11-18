package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AlquilerCabeceraValidacionTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidaciones() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidaciones() {
        factory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "usuario",
            "estadoContratacion",
            "ubicacionRetiro",
            "cotizacionArsPorUsd",
            "fuenteCotizacion",
            "fechaHoraAplicacionCotizacion",
            "fechaHoraCreacion",
            "fechaHoraVencimientoReserva",
            "costoEstimadoUsd",
            "costoEstimadoArs"
    })
    void rechazaCampoObligatorioAusente(String campo) {
        Alquiler alquiler = new Alquiler();

        assertFalse(
                validator.validateProperty(alquiler, campo).isEmpty(),
                "Debe ser obligatorio: " + campo
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "fechaHoraActualizacionFuente",
            "fechaHoraCierre",
            "fechaHoraCancelacion",
            "fechaHoraLiquidacion",
            "costoFinalUsd",
            "costoFinalArs"
    })
    void permiteDatosLegitimamentePendientes(String campo) {
        Alquiler alquiler = new Alquiler();

        assertTrue(
                validator.validateProperty(alquiler, campo).isEmpty(),
                "Debe admitir un valor pendiente: " + campo
        );
    }

    @Test
    void aceptaCabeceraCompletaSinInventarCierreNiLiquidacion() {
        LocalDateTime creacion =
                LocalDateTime.of(2026, 11, 17, 10, 0);

        Alquiler alquiler = new Alquiler();
        alquiler.setUsuario(new Usuario());
        alquiler.setEstadoContratacion(
                new EstadoAlquiler("RESERVADO")
        );
        alquiler.setUbicacionRetiro(
                new Estacion(
                        "Plaza QA",
                        "Calle Prueba 100",
                        5,
                        -53.7876,
                        -67.7002
                )
        );
        alquiler.setCotizacionArsPorUsd(
                new BigDecimal("2047.50")
        );
        alquiler.setFuenteCotizacion("FUENTE_PRUEBA");
        alquiler.setFechaHoraAplicacionCotizacion(creacion);
        alquiler.setFechaHoraCreacion(creacion);
        alquiler.setFechaHoraVencimientoReserva(
                creacion.plusMinutes(15)
        );
        alquiler.setCostoEstimadoUsd(new BigDecimal("8.00"));
        alquiler.setCostoEstimadoArs(new BigDecimal("16380.00"));

        assertTrue(validator.validate(alquiler).isEmpty());

        assertNull(alquiler.getFechaHoraActualizacionFuente());
        assertNull(alquiler.getFechaHoraCierre());
        assertNull(alquiler.getFechaHoraCancelacion());
        assertNull(alquiler.getFechaHoraLiquidacion());
        assertNull(alquiler.getCostoFinalUsd());
        assertNull(alquiler.getCostoFinalArs());
        assertNull(alquiler.getVersion());
    }
}
