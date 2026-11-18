package DSII.TurismoSustentable.models;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class HistorialEstadoAlquilerTest {
    private final Alquiler alquiler = new Alquiler();
    private final Usuario actor = new Usuario();
    private final LocalDateTime fecha = LocalDateTime.of(2026, 11, 18, 15, 0, 0, 123456000);

    @Test void creacionConservaLaFechaYElActorReales() {
        HistorialEstadoAlquiler h = new HistorialEstadoAlquiler(alquiler, null,
                new EstadoAlquiler("RESERVADO"), actor, " usuario ", " Reserva creada. ", fecha);
        assertSame(alquiler, h.getAlquiler());
        assertNull(h.getEstadoAnterior());
        assertEquals("RESERVADO", h.getEstadoNuevo().getCodigo());
        assertSame(actor, h.getActorUsuario());
        assertEquals("USUARIO", h.getOrigen());
        assertEquals("Reserva creada.", h.getObservaciones());
        assertEquals(fecha, h.getFechaHoraCambio());
    }

    @Test void sistemaNoInventaUnUsuario() {
        HistorialEstadoAlquiler h = new HistorialEstadoAlquiler(alquiler, new EstadoAlquiler("RESERVADO"),
                new EstadoAlquiler("VENCIDO"), null, "SISTEMA", "  ", fecha);
        assertNull(h.getActorUsuario());
        assertNull(h.getObservaciones());
    }

    @Test void rechazaActorIncoherenteUOrigenDesconocido() {
        assertThrows(IllegalArgumentException.class, () -> evento(null, "USUARIO"));
        assertThrows(IllegalArgumentException.class, () -> evento(actor, "SISTEMA"));
        assertThrows(IllegalArgumentException.class, () -> evento(actor, "ADMIN"));
        assertThrows(IllegalArgumentException.class, () -> evento(actor, null));
    }

    @Test void noRegistraUnEstadoQueNoCambio() {
        assertThrows(IllegalArgumentException.class, () -> new HistorialEstadoAlquiler(alquiler,
                new EstadoAlquiler("EN_PROGRESO"), new EstadoAlquiler("EN_PROGRESO"), actor,
                "USUARIO", "Reintento", fecha));
    }

    @Test void estadoAnteriorNuloEsExclusivoDeLaCreacionEnReserva() {
        assertThrows(IllegalArgumentException.class, () -> new HistorialEstadoAlquiler(alquiler,
                null, new EstadoAlquiler("FINALIZADO"), actor, "USUARIO", null, fecha));
    }

    @Test void requiereCabeceraEstadoYFecha() {
        assertThrows(NullPointerException.class, () -> new HistorialEstadoAlquiler(null, null,
                new EstadoAlquiler("RESERVADO"), actor, "USUARIO", null, fecha));
        assertThrows(NullPointerException.class, () -> new HistorialEstadoAlquiler(alquiler, null,
                null, actor, "USUARIO", null, fecha));
        assertThrows(NullPointerException.class, () -> new HistorialEstadoAlquiler(alquiler, null,
                new EstadoAlquiler("RESERVADO"), actor, "USUARIO", null, null));
    }

    private HistorialEstadoAlquiler evento(Usuario usuario, String origen) {
        return new HistorialEstadoAlquiler(alquiler, new EstadoAlquiler("RESERVADO"),
                new EstadoAlquiler("CANCELADO"), usuario, origen, null, fecha);
    }
}
