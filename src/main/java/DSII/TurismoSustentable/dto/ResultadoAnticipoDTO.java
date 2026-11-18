package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.time.FechaAlquilerLocalSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import DSII.TurismoSustentable.models.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ResultadoAnticipoDTO {

    private final int alquilerId;
    private final BigDecimal montoArs;
    private final Integer pagoId;
    private final String estadoPago;
    private final String claveSolicitud;
    @JsonSerialize(using = FechaAlquilerLocalSerializer.class)
    private final LocalDateTime fechaCreacionPago;
    @JsonSerialize(using = FechaAlquilerLocalSerializer.class)
    private final LocalDateTime fechaConfirmacionPago;

    public ResultadoAnticipoDTO(
            int alquilerId,
            BigDecimal montoArs,
            Pago pago,
            String claveSolicitud
    ) {
        this.alquilerId = alquilerId;
        this.montoArs = montoArs;
        this.pagoId = pago == null ? null : pago.getId();
        this.estadoPago = pago == null
                ? null
                : pago.getEstadoPago().getCodigo();
        this.claveSolicitud = claveSolicitud;
        this.fechaCreacionPago = pago == null
                ? null
                : pago.getFechaCreacion();
        this.fechaConfirmacionPago = pago == null
                ? null
                : pago.getFechaConfirmacion();
    }

    public int getAlquilerId() {
        return alquilerId;
    }

    public BigDecimal getMontoArs() {
        return montoArs;
    }

    public Integer getPagoId() {
        return pagoId;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public String getClaveSolicitud() {
        return claveSolicitud;
    }

    public LocalDateTime getFechaCreacionPago() {
        return fechaCreacionPago;
    }

    public LocalDateTime getFechaConfirmacionPago() {
        return fechaConfirmacionPago;
    }

    public String getMoneda() {
        return "ARS";
    }

    public boolean isSinImporteACobrar() {
        return montoArs.signum() == 0;
    }
}
