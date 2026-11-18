package DSII.TurismoSustentable.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Metadatos del evento. No expone entidades completas, snapshots ni motivo libre. */
public class AuditoriaResumenDTO {
    public final Integer id;
    public final Integer administradorId;
    public final String administradorNickname;
    public final String entidadAfectada;
    public final Integer entidadId;
    /** Identificador legible actual, si existe; no representa un valor histórico. */
    public final String referenciaEntidad;
    public final String accion;
    public final String fechaRegistrada;

    public AuditoriaResumenDTO(Integer id, Integer administradorId, String administradorNickname,
            String entidadAfectada, Integer entidadId, String accion, LocalDateTime fechaOperacion,
            String referenciaEntidad) {
        this.id = id;
        this.administradorId = administradorId;
        this.administradorNickname = administradorNickname;
        this.entidadAfectada = entidadAfectada;
        this.entidadId = entidadId;
        this.referenciaEntidad = referenciaEntidad;
        this.accion = accion;
        this.fechaRegistrada = fechaOperacion.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
