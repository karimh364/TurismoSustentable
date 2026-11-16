package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.models.Reembolso;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReembolsoDTO {
    private final Integer id;
    private final Integer pagoId;
    private final BigDecimal montoArs;
    private final String estado;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaConfirmacion;

    public ReembolsoDTO(Reembolso r) {
        id = r.getId(); pagoId = r.getPago().getId(); montoArs = r.getMontoArs();
        estado = r.getEstado(); fechaCreacion = r.getFechaCreacion(); fechaConfirmacion = r.getFechaConfirmacion();
    }
    public Integer getId() { return id; }
    public Integer getPagoId() { return pagoId; }
    public BigDecimal getMontoArs() { return montoArs; }
    public String getMoneda() { return "ARS"; }
    public String getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
}
