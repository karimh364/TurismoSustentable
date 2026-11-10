package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HistorialEstadoDetalleTest {

    @Test
    void registraEstadoInicialConActorHumano() {
        AlquilerDetalle detalle = new AlquilerDetalle();
        EstadoAlquilerDetalle estado = new EstadoAlquilerDetalle("RESERVADO");
        Usuario actor = new Usuario();

        HistorialEstadoDetalle evento = new HistorialEstadoDetalle(
                detalle,
                null,
                estado,
                actor,
                " usuario ",
                "  Contratación confirmada.  "
        );

        assertNull(evento.getId());
        assertSame(detalle, evento.getAlquilerDetalle());
        assertNull(evento.getEstadoAnterior());
        assertSame(estado, evento.getEstadoNuevo());
        assertSame(actor, evento.getActorUsuario());
        assertEquals("USUARIO", evento.getOrigen());
        assertEquals(
                "Contratación confirmada.",
                evento.getObservaciones()
        );
        assertNotNull(evento.getFechaHoraCambio());
    }

    @Test
    void conservaAmbosEstadosDeUnaTransicion() {
        EstadoAlquilerDetalle anterior =
                new EstadoAlquilerDetalle("PENDIENTE_RETIRO");

        EstadoAlquilerDetalle nuevo =
                new EstadoAlquilerDetalle("EN_CURSO");

        HistorialEstadoDetalle evento = new HistorialEstadoDetalle(
                new AlquilerDetalle(),
                anterior,
                nuevo,
                new Usuario(),
                "USUARIO",
                "Retiro registrado."
        );

        assertSame(anterior, evento.getEstadoAnterior());
        assertSame(nuevo, evento.getEstadoNuevo());
        assertEquals("PENDIENTE_RETIRO", anterior.getCodigo());
        assertEquals("EN_CURSO", nuevo.getCodigo());
    }

    @Test
    void registraEventoAutomaticoSinInventarActorHumano() {
        HistorialEstadoDetalle evento = new HistorialEstadoDetalle(
                new AlquilerDetalle(),
                new EstadoAlquilerDetalle("RESERVADO"),
                new EstadoAlquilerDetalle("VENCIDO"),
                null,
                " sistema ",
                null
        );

        assertEquals("SISTEMA", evento.getOrigen());
        assertNull(evento.getActorUsuario());
        assertNull(evento.getObservaciones());
        assertNotNull(evento.getFechaHoraCambio());
    }

    @Test
    void rechazaOrigenAusenteVacioODesconocido() {
        for (String origen : new String[]{
                null, "", "   ", "ADMIN", "CLIENTE", "OTRO"
        }) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new HistorialEstadoDetalle(
                            new AlquilerDetalle(),
                            null,
                            new EstadoAlquilerDetalle("RESERVADO"),
                            new Usuario(),
                            origen,
                            null
                    )
            );
        }
    }

    @Test
    void rechazaIncoherenciasEntreOrigenYActor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new HistorialEstadoDetalle(
                        new AlquilerDetalle(),
                        null,
                        new EstadoAlquilerDetalle("RESERVADO"),
                        null,
                        "USUARIO",
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new HistorialEstadoDetalle(
                        new AlquilerDetalle(),
                        null,
                        new EstadoAlquilerDetalle("RESERVADO"),
                        new Usuario(),
                        "SISTEMA",
                        null
                )
        );
    }

    @Test
    void exigeDetalleYEstadoNuevo() {
        assertThrows(
                NullPointerException.class,
                () -> new HistorialEstadoDetalle(
                        null,
                        null,
                        new EstadoAlquilerDetalle("RESERVADO"),
                        new Usuario(),
                        "USUARIO",
                        null
                )
        );

        assertThrows(
                NullPointerException.class,
                () -> new HistorialEstadoDetalle(
                        new AlquilerDetalle(),
                        null,
                        null,
                        new Usuario(),
                        "USUARIO",
                        null
                )
        );
    }

    @Test
    void rechazaTransicionAlMismoCodigoAunqueSeanObjetosDistintos() {
        EstadoAlquilerDetalle anterior =
                new EstadoAlquilerDetalle("EN_CURSO");

        EstadoAlquilerDetalle otroConIgualCodigo =
                new EstadoAlquilerDetalle("EN_CURSO");

        for (EstadoAlquilerDetalle nuevo :
                new EstadoAlquilerDetalle[]{anterior, otroConIgualCodigo}) {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> new HistorialEstadoDetalle(
                            new AlquilerDetalle(),
                            anterior,
                            nuevo,
                            new Usuario(),
                            "USUARIO",
                            "No debería registrar una transición sin cambio."
                    )
            );
        }
    }
}