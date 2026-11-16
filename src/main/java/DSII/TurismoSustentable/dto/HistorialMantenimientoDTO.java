package DSII.TurismoSustentable.dto;
import DSII.TurismoSustentable.models.HistorialOrdenMantenimiento;
import java.time.LocalDateTime;
public class HistorialMantenimientoDTO {
    public final Integer id, actorUsuarioId;
    public final String accion, estadoAnterior, estadoNuevo, origen, observaciones, valoresAnteriores, valoresNuevos;
    public final LocalDateTime fechaHoraCambio;
    public HistorialMantenimientoDTO(HistorialOrdenMantenimiento h) {
        id=h.getId(); actorUsuarioId=h.getActorUsuario()==null?null:h.getActorUsuario().getId();
        accion=h.getAccion(); estadoAnterior=h.getEstadoAnterior()==null?null:h.getEstadoAnterior().getCodigo();
        estadoNuevo=h.getEstadoNuevo()==null?null:h.getEstadoNuevo().getCodigo(); origen=h.getOrigen();
        observaciones=h.getObservaciones(); valoresAnteriores=h.getValoresAnteriores(); valoresNuevos=h.getValoresNuevos();
        fechaHoraCambio=h.getFechaHoraCambio();
    }
}
