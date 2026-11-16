package DSII.TurismoSustentable.dto;
import DSII.TurismoSustentable.models.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// Solo escalares; nunca serializa entidades, contraseñas o datos personales del cliente.
public class OrdenMantenimientoDTO {
    public final Integer id, version, bicicletaId, versionBicicleta, tecnicoId, estacionId;
    public final String codigoBicicleta, estadoBicicleta, tipo, estado, tecnicoNombre, ubicacion;
    public final Boolean estacionPublica;
    public final String diagnostico, descripcionTrabajo, observaciones, resultadoTecnico, motivoPausaCancelacion, monedaRepuestos;
    public final BigDecimal costoRepuestos;
    public final LocalDateTime fechaCreacion, fechaInicio, fechaFin;
    public OrdenMantenimientoDTO(OrdenMantenimiento o) {
        id=o.getId(); version=o.getVersion(); bicicletaId=o.getBicicleta().getId();
        versionBicicleta=o.getBicicleta().getVersion(); codigoBicicleta=o.getBicicleta().getCodigoBicicleta();
        estadoBicicleta=o.getBicicleta().getEstado().getCodigo();
        tipo=o.getTipoMantenimiento().getCodigo(); estado=o.getEstado().getCodigo();
        tecnicoId=o.getTecnico()==null?null:o.getTecnico().getId();
        tecnicoNombre=o.getTecnico()==null?null:o.getTecnico().getPersona().getNombre()+" "+o.getTecnico().getPersona().getApellido();
        Estacion e=o.getBicicleta().getUbicacionActual();
        estacionId=e==null?null:e.getId(); ubicacion=e==null?null:e.getNombre(); estacionPublica=e==null?null:e.isEsPublica();
        diagnostico=o.getDiagnostico(); descripcionTrabajo=o.getDescripcionTrabajo(); observaciones=o.getObservaciones();
        resultadoTecnico=o.getResultadoTecnico(); costoRepuestos=o.getCostoRepuestos(); monedaRepuestos=o.getMonedaRepuestos();
        motivoPausaCancelacion=o.getMotivoPausaCancelacion();
        fechaCreacion=o.getFechaCreacion(); fechaInicio=o.getFechaInicio(); fechaFin=o.getFechaFin();
    }
}
