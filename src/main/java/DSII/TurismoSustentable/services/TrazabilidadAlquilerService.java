package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.HistorialEstadoAlquilerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;

/** Comparte la transacción de reserva/retiro/cierre; no abre una transacción independiente. */
@Service
@Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
public class TrazabilidadAlquilerService {
    private final HistorialEstadoAlquilerRepository historias;

    public TrazabilidadAlquilerService(HistorialEstadoAlquilerRepository historias) {
        this.historias = historias;
    }

    // Se invoca una vez, después de persistir la nueva cabecera y antes de confirmar la reserva.
    public void registrarCreacion(Alquiler alquiler, Usuario actor, LocalDateTime ahora) {
        comprobarPersistido(alquiler);
        historias.save(new HistorialEstadoAlquiler(alquiler, null, alquiler.getEstadoContratacion(),
                actor, "USUARIO", "Reserva creada.", ahora));
    }

    // El llamador mantiene el bloqueo de cabecera. Un segundo retiro no vuelve a EN_PROGRESO.
    public void cambiarEstado(Alquiler alquiler, EstadoAlquiler nuevo, Usuario actor,
            String observaciones, LocalDateTime ahora) {
        comprobarPersistido(alquiler);
        EstadoAlquiler anterior = alquiler.getEstadoContratacion();
        if (anterior == null || nuevo == null) {
            throw new ConfiguracionSistemaException("No se puede registrar una transición sin estados de cabecera.");
        }
        if (Objects.equals(anterior.getCodigo(), nuevo.getCodigo())) return;
        historias.save(new HistorialEstadoAlquiler(alquiler, anterior, nuevo, actor,
                actor == null ? "SISTEMA" : "USUARIO", observaciones, ahora));
        alquiler.setEstadoContratacion(nuevo);
    }

    private void comprobarPersistido(Alquiler alquiler) {
        if (alquiler == null || alquiler.getId() <= 0) {
            throw new IllegalArgumentException("El alquiler debe estar guardado antes de registrar su historial.");
        }
    }
}
