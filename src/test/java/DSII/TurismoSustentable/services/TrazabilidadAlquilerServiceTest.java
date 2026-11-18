package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.HistorialEstadoAlquilerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrazabilidadAlquilerServiceTest {
    @Mock HistorialEstadoAlquilerRepository historias;
    TrazabilidadAlquilerService servicio;
    Alquiler alquiler;
    Usuario actor;
    final LocalDateTime ahora = LocalDateTime.of(2026, 11, 18, 15, 0);

    @BeforeEach void preparar() {
        servicio = new TrazabilidadAlquilerService(historias);
        alquiler = new Alquiler(); alquiler.setId(7);
        alquiler.setEstadoContratacion(new EstadoAlquiler("RESERVADO"));
        actor = new Usuario(); actor.setId(1);
    }

    @Test void registraInicioUnaVezEnLaCreacionDeLaCabecera() {
        servicio.registrarCreacion(alquiler, actor, ahora);
        verify(historias).save(argThat(h -> h.getAlquiler() == alquiler && h.getEstadoAnterior() == null
                && "RESERVADO".equals(h.getEstadoNuevo().getCodigo()) && h.getActorUsuario() == actor
                && ahora.equals(h.getFechaHoraCambio())));
    }

    @Test void cambiaConAnteriorNuevoFechaYActor() {
        servicio.cambiarEstado(alquiler, new EstadoAlquiler("EN_PROGRESO"), actor, "Primer retiro.", ahora);
        assertEquals("EN_PROGRESO", alquiler.getEstadoContratacion().getCodigo());
        verify(historias).save(argThat(h -> "RESERVADO".equals(h.getEstadoAnterior().getCodigo())
                && "EN_PROGRESO".equals(h.getEstadoNuevo().getCodigo()) && h.getActorUsuario() == actor));
    }

    @Test void segundoRetiroOReintentoNoDuplicaLaTransicion() {
        servicio.cambiarEstado(alquiler, new EstadoAlquiler("EN_PROGRESO"), actor, "Primero", ahora);
        servicio.cambiarEstado(alquiler, new EstadoAlquiler("EN_PROGRESO"), actor, "Segundo", ahora.plusMinutes(1));
        verify(historias, times(1)).save(any());
    }

    @Test void vencimientoEsSistemaSinActorHumano() {
        servicio.cambiarEstado(alquiler, new EstadoAlquiler("VENCIDO"), null, "Plazo vencido.", ahora);
        verify(historias).save(argThat(h -> "SISTEMA".equals(h.getOrigen()) && h.getActorUsuario() == null));
    }

    @Test void errorAlGuardarHistoriaNoSeIgnoraNiAnunciaCambio() {
        doThrow(new IllegalStateException("fallo al insertar historia")).when(historias).save(any());
        assertThrows(IllegalStateException.class, () -> servicio.cambiarEstado(alquiler,
                new EstadoAlquiler("CANCELADO"), actor, "Cancelación", ahora));
        assertEquals("RESERVADO", alquiler.getEstadoContratacion().getCodigo());
    }

    @Test void requiereCabeceraPersistida() {
        alquiler.setId(0);
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarCreacion(alquiler, actor, ahora));
        verifyNoInteractions(historias);
    }

    @Test void exigeLaTransaccionDelLlamadorNoUnaHistoriaIndependiente() {
        Transactional tx = TrazabilidadAlquilerService.class.getAnnotation(Transactional.class);
        assertEquals(Propagation.MANDATORY, tx.propagation());
        assertArrayEquals(new Class<?>[]{Exception.class}, tx.rollbackFor());
    }
}
