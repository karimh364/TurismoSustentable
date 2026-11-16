package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.models.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class ResultadoAdicionalDTO {

    private final int alquilerId;
    private final Integer pagoId;
    private final BigDecimal montoArs;
    private final String estadoPago;
    private final String claveSolicitud;
    private final LocalDateTime fechaCreacionPago;
    private final LocalDateTime fechaConfirmacionPago;
    private final LiquidacionAlquilerDTO liquidacion;

    public ResultadoAdicionalDTO(
            Pago pago,
            String claveSolicitud,
            LiquidacionAlquilerDTO liquidacion
    ) {
        Objects.requireNonNull(pago);
        this.alquilerId = pago.getAlquiler().getId();
        this.pagoId = pago.getId();
        this.montoArs = pago.getMontoArs();
        this.estadoPago = pago.getEstadoPago().getCodigo();
        this.claveSolicitud = claveSolicitud;
        this.fechaCreacionPago = pago.getFechaCreacion();
        this.fechaConfirmacionPago = pago.getFechaConfirmacion();
        this.liquidacion = Objects.requireNonNull(liquidacion);
    }

    public int getAlquilerId() { return alquilerId; }
    public Integer getPagoId() { return pagoId; }
    public BigDecimal getMontoArs() { return montoArs; }
    public String getEstadoPago() { return estadoPago; }
    public String getClaveSolicitud() { return claveSolicitud; }
    public LocalDateTime getFechaCreacionPago() { return fechaCreacionPago; }
    public LocalDateTime getFechaConfirmacionPago() { return fechaConfirmacionPago; }
    public LiquidacionAlquilerDTO getLiquidacion() { return liquidacion; }
    public String getMoneda() { return "ARS"; }
    public String getFinalidad() { return "ADICIONAL"; }
}
