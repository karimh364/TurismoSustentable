package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AlquilerDetalleTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void prepararValidaciones() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidaciones() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    void aceptaDetalleInicialSinInventarRetiroDevolucionOCostoReal() {
        AlquilerDetalle detalle = datosValidos();

        assertTrue(validator.validate(detalle).isEmpty());
        assertEquals("RESERVADO", detalle.getEstado().getCodigo());

        assertNull(detalle.getId());
        assertNull(detalle.getVersion());
        assertNull(detalle.getFechaHoraRetiro());
        assertNull(detalle.getFechaHoraDevolucionEstimada());
        assertNull(detalle.getFechaHoraDevolucionReal());
        assertNull(detalle.getFechaHoraFinUso());
        assertNull(detalle.getFechaHoraCierreDetalle());
        assertNull(detalle.getEstacionDevolucion());
        assertNull(detalle.getOrdenMantenimiento());
        assertNull(detalle.getIncidenciaCorte());
        assertNull(detalle.getSubtotalRealUsd());
        assertNull(detalle.getSubtotalRealArs());
        assertFalse(detalle.isFallaReportada());
    }

    @Test
    void rechazaRelacionesObligatoriasAusentes() {
        AlquilerDetalle detalle = datosValidos();

        detalle.setAlquiler(null);
        detalle.setBicicleta(null);
        detalle.setTipoBicicletaContratado(null);
        detalle.setEstado(null);

        comprobarError(detalle, "alquiler");
        comprobarError(detalle, "bicicleta");
        comprobarError(detalle, "tipoBicicletaContratado");
        comprobarError(detalle, "estado");
    }

    @Test
    void rechazaCamposNumericosObligatoriosAusentes() {
        AlquilerDetalle detalle = datosValidos();

        detalle.setPrecioPorHoraAplicadoUsd(null);
        detalle.setDuracionEstimadaSegundos(null);
        detalle.setSubtotalEstimadoUsd(null);
        detalle.setSubtotalEstimadoArs(null);

        comprobarError(detalle, "precioPorHoraAplicadoUsd");
        comprobarError(detalle, "duracionEstimadaSegundos");
        comprobarError(detalle, "subtotalEstimadoUsd");
        comprobarError(detalle, "subtotalEstimadoArs");
    }

    @Test
    void rechazaTarifasInvalidasSinRedondearlas() {
        String[] valores = {
                "0.00", "-0.01", "1.001", "100000000.00"
        };

        for (String valor : valores) {
            AlquilerDetalle detalle = datosValidos();
            BigDecimal importe = new BigDecimal(valor);

            detalle.setPrecioPorHoraAplicadoUsd(importe);

            assertEquals(importe, detalle.getPrecioPorHoraAplicadoUsd());
            comprobarError(detalle, "precioPorHoraAplicadoUsd");
        }
    }

    @Test
    void rechazaDuracionCeroONegativa() {
        for (int duracion : new int[]{0, -1}) {
            AlquilerDetalle detalle = datosValidos();
            detalle.setDuracionEstimadaSegundos(duracion);

            comprobarError(detalle, "duracionEstimadaSegundos");
        }
    }

    @Test
    void rechazaSubtotalesNegativosConExcesoDeDecimalesODeCapacidad() {
        String[] valores = {
                "-0.01",
                "1.001",
                "1000000000000000000.00"
        };

        for (String valor : valores) {
            AlquilerDetalle detalle = datosValidos();
            BigDecimal importe = new BigDecimal(valor);

            detalle.setSubtotalEstimadoUsd(importe);
            detalle.setSubtotalEstimadoArs(importe);
            detalle.setSubtotalRealUsd(importe);
            detalle.setSubtotalRealArs(importe);

            assertEquals(importe, detalle.getSubtotalRealUsd());

            comprobarError(detalle, "subtotalEstimadoUsd");
            comprobarError(detalle, "subtotalEstimadoArs");
            comprobarError(detalle, "subtotalRealUsd");
            comprobarError(detalle, "subtotalRealArs");
        }
    }

    @Test
    void aceptaSubtotalesCeroSinConfundirlosConImportesPendientes() {
        AlquilerDetalle detalle = datosValidos();
        BigDecimal cero = new BigDecimal("0.00");

        detalle.setSubtotalEstimadoUsd(cero);
        detalle.setSubtotalEstimadoArs(cero);
        detalle.setSubtotalRealUsd(cero);
        detalle.setSubtotalRealArs(cero);

        assertTrue(validator.validate(detalle).isEmpty());
        assertEquals(cero, detalle.getSubtotalRealUsd());
        assertEquals(cero, detalle.getSubtotalRealArs());
    }

    @Test
    void validaCodigosDelOrigenDeCierreYPermiteAusenciaAntesDelCierre() {
        AlquilerDetalle detalle = datosValidos();

        for (String origen : new String[]{"CLIENTE", "ADMIN", "SISTEMA"}) {
            detalle.setOrigenCierre(origen);
            assertTrue(validator.validate(detalle).isEmpty());
        }

        for (String origen : new String[]{"", "OTRO", "admin"}) {
            detalle.setOrigenCierre(origen);
            comprobarError(detalle, "origenCierre");
        }

        detalle.setOrigenCierre(null);
        assertTrue(validator.validate(detalle).isEmpty());
    }

    private AlquilerDetalle datosValidos() {
        AlquilerDetalle detalle = new AlquilerDetalle();

        detalle.setAlquiler(new Alquiler());
        detalle.setBicicleta(new Bicicleta());
        detalle.setTipoBicicletaContratado(new TipoBicicleta());
        detalle.setEstado(new EstadoAlquilerDetalle("RESERVADO"));
        detalle.setPrecioPorHoraAplicadoUsd(new BigDecimal("10.00"));
        detalle.setDuracionEstimadaSegundos(7200);
        detalle.setSubtotalEstimadoUsd(new BigDecimal("20.00"));
        detalle.setSubtotalEstimadoArs(new BigDecimal("30600.00"));

        return detalle;
    }

    private void comprobarError(AlquilerDetalle detalle, String campo) {
        assertTrue(
                validator.validate(detalle).stream()
                        .anyMatch(error ->
                                error.getPropertyPath().toString().equals(campo)
                        ),
                "Se esperaba un error en el campo: " + campo
        );
    }
}
