package DSII.TurismoSustentable.dto;
import DSII.TurismoSustentable.models.*;
import java.time.LocalDateTime;

/** Respuesta escalar: no expone usuarios/JPA, credenciales ni datos privados de terceros. */
public class IncidenciaDTO {
    public final Integer id,version,alquilerId,detalleId,versionDetalle,bicicletaId,versionBicicleta;
    public final Integer registradoPorId,informanteId,resueltoPorId,ordenMantenimientoId,estacionRecepcionId,recibidoPorId;
    public final Integer estacionActualId;
    public final String tipo,estadoRevision,descripcion,lugarReportado,motivoDecision,lugarRecepcion,codigoBicicleta;
    public final String estadoDetalle,estadoBicicleta,estacionRecepcion;
    public final String ubicacionActual;
    public final boolean requiereRevisionEconomica,borradoBici,esCorteAplicado,perdidaDefinitiva;
    public final LocalDateTime fechaHoraHechoInformada,fechaHoraRegistro,fechaHoraDecision,fechaHoraCorteValidado,fechaHoraRecepcionFisica;
    public IncidenciaDTO(Incidencia i,boolean perdidaDefinitiva,boolean vistaAdministrativa) {
        AlquilerDetalle d=i.getAlquilerDetalle();Bicicleta b=d.getBicicleta();
        id=i.getId();version=i.getVersion();alquilerId=d.getAlquiler().getId();detalleId=d.getId();versionDetalle=d.getVersion();
        bicicletaId=b.getId();versionBicicleta=b.getVersion();codigoBicicleta=b.getCodigoBicicleta();
        estadoDetalle=d.getEstado().getCodigo();estadoBicicleta=b.getEstado().getCodigo();borradoBici=b.isBorradoBici();
        estacionActualId=!vistaAdministrativa||b.getUbicacionActual()==null?null:b.getUbicacionActual().getId();
        ubicacionActual=!vistaAdministrativa||b.getUbicacionActual()==null?null:b.getUbicacionActual().getNombre();
        esCorteAplicado=d.getIncidenciaCorte()!=null&&id.equals(d.getIncidenciaCorte().getId());
        registradoPorId=i.getRegistradoPor().getId();informanteId=i.getInformante()==null?null:i.getInformante().getId();
        resueltoPorId=i.getResueltoPor()==null?null:i.getResueltoPor().getId();
        ordenMantenimientoId=i.getOrdenMantenimiento()==null?null:i.getOrdenMantenimiento().getId();
        boolean interna=i.getEstacionRecepcion()!=null&&!i.getEstacionRecepcion().isEsPublica();
        estacionRecepcionId=i.getEstacionRecepcion()==null||(!vistaAdministrativa&&interna)?null:i.getEstacionRecepcion().getId();
        estacionRecepcion=i.getEstacionRecepcion()==null?null:!vistaAdministrativa&&interna?"Recibida por el servicio en una ubicación interna":i.getEstacionRecepcion().getNombre();
        recibidoPorId=i.getRecibidoPor()==null?null:i.getRecibidoPor().getId();
        tipo=i.getTipo();estadoRevision=i.getEstadoRevision();descripcion=i.getDescripcion();lugarReportado=i.getLugarReportado();
        motivoDecision=i.getMotivoDecision();lugarRecepcion=i.getLugarRecepcion();requiereRevisionEconomica=i.isRequiereRevisionEconomica();
        fechaHoraHechoInformada=i.getFechaHoraHechoInformada();fechaHoraRegistro=i.getFechaHoraRegistro();fechaHoraDecision=i.getFechaHoraDecision();
        fechaHoraCorteValidado=i.getFechaHoraCorteValidado();fechaHoraRecepcionFisica=i.getFechaHoraRecepcionFisica();
        this.perdidaDefinitiva=perdidaDefinitiva;
    }
}
