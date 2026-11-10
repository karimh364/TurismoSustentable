package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class IncidenciaTest {

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
    void creaReportePendienteSinInventarDecisionCorteORecepcion() {
        Incidencia incidencia = datosValidos();

        assertTrue(validator.validate(incidencia).isEmpty());
        assertEquals("PENDIENTE", incidencia.getEstadoRevision());
        assertNotNull(incidencia.getFechaHoraRegistro());

        assertNull(incidencia.getId());
        assertNull(incidencia.getVersion());
        assertNull(incidencia.getInformante());
        assertNull(incidencia.getResueltoPor());
        assertNull(incidencia.getOrdenMantenimiento());
        assertNull(incidencia.getFechaHoraDecision());
        assertNull(incidencia.getFechaHoraCorteValidado());
        assertNull(incidencia.getMotivoDecision());
        assertNull(incidencia.getFechaHoraRecepcionFisica());
        assertNull(incidencia.getEstacionRecepcion());
        assertNull(incidencia.getRecibidoPor());
        assertNull(incidencia.getLugarRecepcion());
    }

    @Test
    void rechazaReferenciasObligatoriasAusentes() {
        Incidencia incidencia = datosValidos();

        incidencia.setAlquilerDetalle(null);
        incidencia.setRegistradoPor(null);

        comprobarError(incidencia, "alquilerDetalle");
        comprobarError(incidencia, "registradoPor");
    }

    @Test
    void aceptaLosCuatroCodigosDeTipo() {
        Incidencia incidencia = datosValidos();

        for (String tipo : new String[]{
                "AVERIA", "ACCIDENTE", "PERDIDA", "ROBO"
        }) {
            incidencia.setTipo(tipo);

            assertTrue(
                    validator.validateProperty(incidencia, "tipo").isEmpty(),
                    "Debería aceptar el código: " + tipo
            );
        }
    }

    @Test
    void rechazaTipoAusenteVacioODesconocido() {
        for (String tipo : new String[]{
                null, "", "   ", "OTRO", "averia"
        }) {
            Incidencia incidencia = datosValidos();
            incidencia.setTipo(tipo);

            comprobarError(incidencia, "tipo");
        }
    }

    @Test
    void aceptaLosTresCodigosDeRevision() {
        Incidencia incidencia = datosValidos();

        for (String estado : new String[]{
                "PENDIENTE", "VALIDADA", "DESCARTADA"
        }) {
            incidencia.setEstadoRevision(estado);

            assertTrue(
                    validator.validateProperty(
                            incidencia,
                            "estadoRevision"
                    ).isEmpty(),
                    "Debería aceptar el código: " + estado
            );
        }
    }

    @Test
    void rechazaRevisionAusenteVaciaODesconocida() {
        for (String estado : new String[]{
                null, "", "   ", "FINALIZADA", "pendiente"
        }) {
            Incidencia incidencia = datosValidos();
            incidencia.setEstadoRevision(estado);

            comprobarError(incidencia, "estadoRevision");
        }
    }

    @Test
    void rechazaDescripcionAusenteOVacia() {
        for (String descripcion : new String[]{null, "", "   "}) {
            Incidencia incidencia = datosValidos();
            incidencia.setDescripcion(descripcion);

            comprobarError(incidencia, "descripcion");
        }
    }

    @Test
    void exigeFechaDelHechoAunqueExistaFechaDeRegistro() {
        Incidencia incidencia = datosValidos();
        incidencia.setFechaHoraHechoInformada(null);

        assertNotNull(incidencia.getFechaHoraRegistro());
        comprobarError(incidencia, "fechaHoraHechoInformada");
    }

    private Incidencia datosValidos() {
        Incidencia incidencia = new Incidencia();

        incidencia.setAlquilerDetalle(new AlquilerDetalle());
        incidencia.setRegistradoPor(new Usuario());
        incidencia.setTipo("AVERIA");
        incidencia.setDescripcion(
                "El freno trasero dejó de responder durante el viaje."
        );
        incidencia.setFechaHoraHechoInformada(
                LocalDateTime.of(2026, 11, 8, 10, 30)
        );

        return incidencia;
    }

    private void comprobarError(Incidencia incidencia, String campo) {
        assertTrue(
                validator.validate(incidencia).stream()
                        .anyMatch(error ->
                                error.getPropertyPath().toString().equals(campo)
                        ),
                "Se esperaba un error en el campo: " + campo
        );
    }
}
