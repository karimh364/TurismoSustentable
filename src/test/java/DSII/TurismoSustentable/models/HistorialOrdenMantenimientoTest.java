package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.*;

class HistorialOrdenMantenimientoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private final OrdenMantenimiento orden = new OrdenMantenimiento(
            new Bicicleta(),
            new TipoMantenimiento("CORRECTIVO", "Revisión de una falla"),
            new EstadoOrdenMantenimiento("CREADA")
    );

    private final Usuario actor = new Usuario();

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
    void registraCreacionHumanaYNormalizaCodigos() {
        EstadoOrdenMantenimiento creada = orden.getEstado();
        String datosIniciales = "{\"estado\":\"CREADA\"}";

        HistorialOrdenMantenimiento evento = new HistorialOrdenMantenimiento(
                orden, " crear ", null, creada, actor, " usuario ",
                " Alta de orden ", null, datosIniciales
        );

        assertTrue(validator.validate(evento).isEmpty());
        assertNull(evento.getId());
        assertSame(orden, evento.getOrdenMantenimiento());
        assertEquals("CREAR", evento.getAccion());
        assertNull(evento.getEstadoAnterior());
        assertSame(creada, evento.getEstadoNuevo());
        assertSame(actor, evento.getActorUsuario());
        assertEquals("USUARIO", evento.getOrigen());
        assertEquals("Alta de orden", evento.getObservaciones());
        assertNotNull(evento.getFechaHoraCambio());
        assertNull(evento.getValoresAnteriores());
        assertEquals(datosIniciales, evento.getValoresNuevos());
    }

    @Test
    void registraCreacionAutomaticaSinInventarActorHumano() {
        HistorialOrdenMantenimiento evento = new HistorialOrdenMantenimiento(
                orden, "CREAR", null, orden.getEstado(), null, "SISTEMA",
                null, null, "{\"estado\":\"CREADA\"}"
        );

        assertTrue(validator.validate(evento).isEmpty());
        assertEquals("SISTEMA", evento.getOrigen());
        assertNull(evento.getActorUsuario());
        assertNotNull(evento.getFechaHoraCambio());
    }

    @Test
    void conservaEstadosDeUnaTransicionReal() {
        EstadoOrdenMantenimiento anterior = orden.getEstado();
        EstadoOrdenMantenimiento nuevo =
                new EstadoOrdenMantenimiento("EN_PROCESO");

        HistorialOrdenMantenimiento evento = evento(
                "CAMBIAR_ESTADO", anterior, nuevo, actor, "USUARIO"
        );

        assertTrue(validator.validate(evento).isEmpty());
        assertSame(anterior, evento.getEstadoAnterior());
        assertSame(nuevo, evento.getEstadoNuevo());
        assertSame(actor, evento.getActorUsuario());
    }

    @Test
    void conservaCambiosDeDatosSinInventarTransicion() {
        String anteriores = "{\"diagnostico\":\"Pendiente\"}";
        String nuevos = "{\"diagnostico\":\"Freno desajustado\"}";

        HistorialOrdenMantenimiento evento = new HistorialOrdenMantenimiento(
                orden, "ACTUALIZAR_DATOS", null, null, actor, "USUARIO",
                "Actualización de diagnóstico", anteriores, nuevos
        );

        assertTrue(validator.validate(evento).isEmpty());
        assertNull(evento.getEstadoAnterior());
        assertNull(evento.getEstadoNuevo());
        assertEquals(anteriores, evento.getValoresAnteriores());
        assertEquals(nuevos, evento.getValoresNuevos());
        assertSame(actor, evento.getActorUsuario());
    }

    @Test
    void rechazaOrigenYActorIncoherentes() {
        String[] origenesInvalidos = {
                null, "", "   ", "ADMIN", "CLIENTE", "OTRO"
        };

        for (String origen : origenesInvalidos) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> evento(
                            "ACTUALIZAR_DATOS", null, null, actor, origen
                    )
            );
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> evento(
                        "ACTUALIZAR_DATOS", null, null, null, "USUARIO"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> evento(
                        "ACTUALIZAR_DATOS", null, null, actor, "SISTEMA"
                )
        );
    }

    @Test
    void rechazaEstadosIncoherentesOTransicionesFicticias() {
        EstadoOrdenMantenimiento creada = orden.getEstado();

        assertThrows(
                IllegalArgumentException.class,
                () -> evento(
                        "CAMBIAR_ESTADO", creada, null, actor, "USUARIO"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> evento(
                        "CAMBIAR_ESTADO", creada, creada, actor, "USUARIO"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> evento(
                        "CAMBIAR_ESTADO",
                        creada,
                        new EstadoOrdenMantenimiento("CREADA"),
                        actor,
                        "USUARIO"
                )
        );
    }

    @Test
    void rechazaEventoSinOrden() {
        assertThrows(
                NullPointerException.class,
                () -> new HistorialOrdenMantenimiento(
                        null, "CREAR", null, orden.getEstado(),
                        actor, "USUARIO", null, null,
                        "{\"estado\":\"CREADA\"}"
                )
        );
    }

    @Test
    void validaAccionObligatoriaYLimiteDeCaracteres() {
        String[] accionesInvalidas = {
                null, "", "   ", "X".repeat(51)
        };

        for (String accion : accionesInvalidas) {
            HistorialOrdenMantenimiento evento = evento(
                    accion, null, null, actor, "USUARIO"
            );

            assertTrue(
                    validator.validate(evento).stream().anyMatch(
                            error -> error.getPropertyPath()
                                    .toString().equals("accion")
                    ),
                    "Debería detectar un error en la acción"
            );
        }

        HistorialOrdenMantenimiento limiteValido = evento(
                "X".repeat(50),
                orden.getEstado(),
                new EstadoOrdenMantenimiento("EN_PROCESO"),
                actor,
                "USUARIO"
        );

        assertTrue(validator.validate(limiteValido).isEmpty());
    }

    private HistorialOrdenMantenimiento evento(
            String accion,
            EstadoOrdenMantenimiento anterior,
            EstadoOrdenMantenimiento nuevo,
            Usuario usuario,
            String origen
    ) {
        return new HistorialOrdenMantenimiento(
                orden, accion, anterior, nuevo, usuario, origen,
                null, null, null
        );
    }
}