package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.time.FechaAlquilerLocalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;

/** Solo datos de presentación; nunca se serializa la entidad Usuario ni sus credenciales. */
public class HistorialEstadoAlquilerDTO {
    public final Integer id, alquilerId, actorUsuarioId;
    public final String estadoAnterior, estadoNuevo, origen, actorNickname, observaciones;
    @JsonSerialize(using = FechaAlquilerLocalSerializer.class)
    public final LocalDateTime fechaHoraCambio;

    public HistorialEstadoAlquilerDTO(Integer id, Integer alquilerId, String estadoAnterior,
            String estadoNuevo, Integer actorUsuarioId, String actorNickname, String origen,
            String observaciones, LocalDateTime fechaHoraCambio) {
        this.id = id;
        this.alquilerId = alquilerId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.origen = origen;
        this.actorUsuarioId = actorUsuarioId;
        this.actorNickname = actorNickname;
        this.observaciones = observaciones;
        this.fechaHoraCambio = fechaHoraCambio;
    }
}
