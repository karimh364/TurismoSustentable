package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.models.HistorialOrdenMantenimiento;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

public class HistorialMantenimientoDTO {

    public final Integer id, actorUsuarioId;
    public final String accion, estadoAnterior, estadoNuevo;
    public final String origen, observaciones;
    public final LocalDateTime fechaHoraCambio;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final String valoresAnteriores;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public final String valoresNuevos;

    public HistorialMantenimientoDTO(
            HistorialOrdenMantenimiento h,
            boolean incluirAuditoria
    ) {
        id = h.getId();
        actorUsuarioId = h.getActorUsuario() == null
                ? null : h.getActorUsuario().getId();

        accion = h.getAccion();
        estadoAnterior = h.getEstadoAnterior() == null
                ? null : h.getEstadoAnterior().getCodigo();
        estadoNuevo = h.getEstadoNuevo() == null
                ? null : h.getEstadoNuevo().getCodigo();

        origen = h.getOrigen();
        observaciones = h.getObservaciones();
        fechaHoraCambio = h.getFechaHoraCambio();

        valoresAnteriores = incluirAuditoria
                ? h.getValoresAnteriores() : null;
        valoresNuevos = incluirAuditoria
                ? h.getValoresNuevos() : null;
    }
}
