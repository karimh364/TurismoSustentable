package DSII.TurismoSustentable.services;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class TrazabilidadInventarioService {
    private final HistorialEstadoBicicletaRepository historial;
    private final AuditoriaSistemaRepository auditoria;
    public TrazabilidadInventarioService(HistorialEstadoBicicletaRepository historial,
                                         AuditoriaSistemaRepository auditoria) {
        this.historial = historial;
        this.auditoria = auditoria;
    }
    public void estado(Bicicleta bicicleta, EstadoBicicleta anterior, Usuario actor, String motivo) {
        historial.save(new HistorialEstadoBicicleta(
                bicicleta, anterior, bicicleta.getEstado(), actor, motivo));
    }
    // Se llama exclusivamente desde operaciones autorizadas para ADMIN.
    public void administracion(Usuario admin, String entidad, int id, String accion,
                               String anteriores, String nuevos, String motivo) {
        auditoria.save(new AuditoriaSistema(admin, entidad, id, accion, anteriores, nuevos, motivo));
    }
    public String datos(Bicicleta b) {
        // Lista explícita de datos de inventario: nunca serializar al usuario ni credenciales.
        return "codigo=" + b.getCodigoBicicleta() + ";tipoId=" + b.getTipo().getId()
                + ";estado=" + b.getEstado().getCodigo() + ";estacionId="
                + (b.getUbicacionActual() == null ? null : b.getUbicacionActual().getId())
                + ";borrado=" + b.isBorradoBici();
    }
}
