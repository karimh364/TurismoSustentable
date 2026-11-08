package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrdenMantenimientoTest {

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
    void creaOrdenConDatosInicialesValidos() {
        OrdenMantenimiento orden = datosValidos();

        assertTrue(validator.validate(orden).isEmpty());
        assertEquals("CREADA", orden.getEstado().getCodigo());
        assertEquals("PREVENTIVO", orden.getTipoMantenimiento().getCodigo());
        assertNotNull(orden.getBicicleta());
        assertNotNull(orden.getFechaCreacion());

        assertNull(orden.getId());
        assertNull(orden.getVersion());
        assertNull(orden.getTecnico());
        assertNull(orden.getFechaInicio());
        assertNull(orden.getFechaFin());
        assertNull(orden.getResultadoTecnico());
        assertNull(orden.getCostoRepuestos());
        assertNull(orden.getMonedaRepuestos());
        assertNull(orden.getDiagnostico());
        assertNull(orden.getDescripcionTrabajo());
    }

    @Test
    void rechazaRelacionesObligatoriasAusentes() {
        OrdenMantenimiento orden = new OrdenMantenimiento();

        comprobarError(orden, "bicicleta");
        comprobarError(orden, "tipoMantenimiento");
        comprobarError(orden, "estado");
    }

    @Test
    void aceptaCostosConocidosDentroDeLosLimites() {
        String[] costos = {
                "0.00", "0.01", "1500.50", "9999999999.99"
        };

        for (String costo : costos) {
            OrdenMantenimiento orden = datosValidos();
            orden.setCostoRepuestos(new BigDecimal(costo));
            orden.setMonedaRepuestos("ARS");

            assertTrue(
                    validator.validate(orden).isEmpty(),
                    "Debería aceptar el costo: " + costo
            );
        }
    }

    @Test
    void rechazaCostosInvalidosSinRedondearlos() {
        String[] costos = {
                "-0.01", "1.001", "10000000000.00"
        };

        for (String costo : costos) {
            OrdenMantenimiento orden = datosValidos();
            BigDecimal importe = new BigDecimal(costo);

            orden.setCostoRepuestos(importe);
            orden.setMonedaRepuestos("ARS");

            assertEquals(importe, orden.getCostoRepuestos());
            comprobarError(orden, "costoRepuestos");
        }
    }

    @Test
    void normalizaYValidaResultadoYMoneda() {
        OrdenMantenimiento orden = datosValidos();

        for (String resultado : new String[]{" apta ", " no_apta "}) {
            orden.setResultadoTecnico(resultado);
            orden.setMonedaRepuestos(" ars ");

            assertTrue(validator.validate(orden).isEmpty());
            assertEquals("ARS", orden.getMonedaRepuestos());
        }

        assertEquals("NO_APTA", orden.getResultadoTecnico());

        orden.setResultadoTecnico("DISPONIBLE");
        comprobarError(orden, "resultadoTecnico");

        orden.setResultadoTecnico("APTA");
        orden.setMonedaRepuestos("USD");
        comprobarError(orden, "monedaRepuestos");
    }

    @Test
    void eliminaEspaciosExterioresDeLosTextos() {
        OrdenMantenimiento orden = datosValidos();

        orden.setDiagnostico(" Freno desajustado ");
        orden.setDescripcionTrabajo(" Ajuste y comprobación del freno ");
        orden.setObservaciones(" Revisión completada ");
        orden.setMotivoPausaCancelacion(" Espera de repuesto ");

        assertEquals("Freno desajustado", orden.getDiagnostico());
        assertEquals(
                "Ajuste y comprobación del freno",
                orden.getDescripcionTrabajo()
        );
        assertEquals("Revisión completada", orden.getObservaciones());
        assertEquals("Espera de repuesto", orden.getMotivoPausaCancelacion());
        assertTrue(validator.validate(orden).isEmpty());
    }

    private OrdenMantenimiento datosValidos() {
        return new OrdenMantenimiento(
                new Bicicleta(),
                new TipoMantenimiento("PREVENTIVO", "Revisión de rutina"),
                new EstadoOrdenMantenimiento("CREADA")
        );
    }

    private void comprobarError(OrdenMantenimiento orden, String campo) {
        assertTrue(
                validator.validate(orden).stream().anyMatch(
                        error -> error.getPropertyPath()
                                .toString().equals(campo)
                ),
                "Debería detectar un error en: " + campo
        );
    }
}