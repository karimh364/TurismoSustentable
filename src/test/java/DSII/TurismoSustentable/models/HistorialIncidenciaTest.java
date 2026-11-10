package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.*;

class HistorialIncidenciaTest {

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
    void registraEventoInicialYNormalizaCodigos() {
        Incidencia incidencia = new Incidencia();
        Usuario actor = new Usuario();

        HistorialIncidencia evento = new HistorialIncidencia(
                incidencia,
                " registrar ",
                null,
                " pendiente ",
                actor,
                " usuario ",
                "  Reporte recibido.  ",
                null,
                "{\"tipo\":\"AVERIA\"}"
        );

        assertTrue(validator.validate(evento).isEmpty());
        assertNull(evento.getId());
        assertSame(incidencia, evento.getIncidencia());
        assertSame(actor, evento.getActorUsuario());
        assertEquals("REGISTRAR", evento.getAccion());
        assertNull(evento.getEstadoAnterior());
        assertEquals("PENDIENTE", evento.getEstadoNuevo());
        assertEquals("USUARIO", evento.getOrigen());
        assertEquals("Reporte recibido.", evento.getObservaciones());
        assertNull(evento.getValoresAnteriores());
        assertEquals(
                "{\"tipo\":\"AVERIA\"}",
                evento.getValoresNuevos()
        );
        assertNotNull(evento.getFechaHoraCambio());
    }

    @Test
    void conservaEstadosDeUnaRevisionHumana() {
        HistorialIncidencia evento = crearEvento(
                "RESOLVER",
                " pendiente ",
                " validada ",
                new Usuario(),
                "USUARIO"
        );

        assertTrue(validator.validate(evento).isEmpty());
        assertEquals("PENDIENTE", evento.getEstadoAnterior());
        assertEquals("VALIDADA", evento.getEstadoNuevo());
    }

    @Test
    void conservaCambiosDeDatosSinSimularTransicionNiActorHumano() {
        String anteriores = " {\"ordenMantenimientoId\":null} ";
        String nuevos = " {\"ordenMantenimientoId\":7} ";

        HistorialIncidencia evento = new HistorialIncidencia(
                new Incidencia(),
                "ACTUALIZAR_VINCULO",
                null,
                null,
                null,
                " sistema ",
                null,
                anteriores,
                nuevos
        );

        assertTrue(validator.validate(evento).isEmpty());
        assertNull(evento.getEstadoAnterior());
        assertNull(evento.getEstadoNuevo());
        assertNull(evento.getActorUsuario());
        assertEquals("SISTEMA", evento.getOrigen());
        assertNull(evento.getObservaciones());
        assertEquals(anteriores, evento.getValoresAnteriores());
        assertEquals(nuevos, evento.getValoresNuevos());
    }

    @Test
    void rechazaOrigenInvalidoOIncoherenteConElActor() {
        for (String origen : new String[]{
                null, "", "   ", "ADMIN", "CLIENTE"
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> crearEvento(
                            "REGISTRAR",
                            null,
                            "PENDIENTE",
                            new Usuario(),
                            origen
                    )
            );
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> crearEvento(
                        "REGISTRAR", null, "PENDIENTE",
                        null, "USUARIO"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> crearEvento(
                        "REGISTRAR", null, "PENDIENTE",
                        new Usuario(), "SISTEMA"
                )
        );
    }

    @Test
    void exigeIncidencia() {
        assertThrows(
                NullPointerException.class,
                () -> new HistorialIncidencia(
                        null,
                        "REGISTRAR",
                        null,
                        "PENDIENTE",
                        new Usuario(),
                        "USUARIO",
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    void rechazaEstadoAnteriorSinNuevoYTransicionesSinCambio() {
        assertThrows(
                IllegalArgumentException.class,
                () -> crearEvento(
                        "RESOLVER", "PENDIENTE", null,
                        new Usuario(), "USUARIO"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> crearEvento(
                        "RESOLVER", " pendiente ", "PENDIENTE",
                        new Usuario(), "USUARIO"
                )
        );
    }

    @Test
    void rechazaCodigosDeEstadoNoPermitidos() {
        for (String codigo : new String[]{"", "   ", "FINALIZADA"}) {
            HistorialIncidencia anteriorInvalido = crearEvento(
                    "RESOLVER", codigo, "VALIDADA",
                    new Usuario(), "USUARIO"
            );

            comprobarError(anteriorInvalido, "estadoAnterior");

            HistorialIncidencia nuevoInvalido = crearEvento(
                    "REGISTRAR", null, codigo,
                    new Usuario(), "USUARIO"
            );

            comprobarError(nuevoInvalido, "estadoNuevo");
        }
    }

    @Test
    void validaObligatoriedadYLongitudDeLaAccion() {
        for (String accion : new String[]{
                null, "", "   ", "A".repeat(51)
        }) {
            HistorialIncidencia evento = crearEvento(
                    accion, null, "PENDIENTE",
                    new Usuario(), "USUARIO"
            );

            comprobarError(evento, "accion");
        }

        HistorialIncidencia limite = crearEvento(
                "A".repeat(50), null, "PENDIENTE",
                new Usuario(), "USUARIO"
        );

        assertTrue(
                validator.validateProperty(limite, "accion").isEmpty()
        );
    }

    private HistorialIncidencia crearEvento(
            String accion,
            String anterior,
            String nuevo,
            Usuario actor,
            String origen
    ) {
        return new HistorialIncidencia(
                new Incidencia(),
                accion,
                anterior,
                nuevo,
                actor,
                origen,
                "Evento de prueba.",
                null,
                null
        );
    }

    private void comprobarError(HistorialIncidencia evento, String campo) {
        assertTrue(
                validator.validate(evento).stream()
                        .anyMatch(error ->
                                error.getPropertyPath().toString().equals(campo)
                        ),
                "Se esperaba un error en el campo: " + campo
        );
    }
}
