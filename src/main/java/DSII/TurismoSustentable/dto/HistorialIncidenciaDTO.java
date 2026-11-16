package DSII.TurismoSustentable.dto;
import DSII.TurismoSustentable.models.HistorialIncidencia;
import java.time.LocalDateTime;
public class HistorialIncidenciaDTO {
    public final Integer id,actorUsuarioId;
    public final String accion,estadoAnterior,estadoNuevo,origen,observaciones,valoresAnteriores,valoresNuevos;
    public final LocalDateTime fechaHoraCambio;
    public HistorialIncidenciaDTO(HistorialIncidencia h,boolean administrativa) {
        id=h.getId();actorUsuarioId=h.getActorUsuario()==null?null:h.getActorUsuario().getId();
        accion=h.getAccion();estadoAnterior=h.getEstadoAnterior();estadoNuevo=h.getEstadoNuevo();origen=h.getOrigen();
        observaciones=h.getObservaciones();valoresAnteriores=administrativa?h.getValoresAnteriores():null;
        valoresNuevos=administrativa?h.getValoresNuevos():null;fechaHoraCambio=h.getFechaHoraCambio();
    }
}
