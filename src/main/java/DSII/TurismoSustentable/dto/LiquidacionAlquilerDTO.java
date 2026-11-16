package DSII.TurismoSustentable.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LiquidacionAlquilerDTO {

    private final int alquilerId;
    private final Integer version;
    private final String situacionFinanciera;
    private final LocalDateTime fechaHoraLiquidacion;

    private final BigDecimal costoFinalUsd;
    private final BigDecimal costoFinalArs;

    private final BigDecimal pagosAprobadosArs;
    private final BigDecimal reembolsosConfirmadosArs;
    private final BigDecimal netoConfirmadoArs;

    private final BigDecimal saldoPendienteArs;
    private final BigDecimal reembolsoPendienteArs;

    public LiquidacionAlquilerDTO(
            int alquilerId,
            Integer version,
            String situacionFinanciera,
            LocalDateTime fechaHoraLiquidacion,
            BigDecimal costoFinalUsd,
            BigDecimal costoFinalArs,
            BigDecimal pagosAprobadosArs,
            BigDecimal reembolsosConfirmadosArs,
            BigDecimal netoConfirmadoArs,
            BigDecimal saldoPendienteArs,
            BigDecimal reembolsoPendienteArs
    ) {
        this.alquilerId = alquilerId;
        this.version = version;
        this.situacionFinanciera = situacionFinanciera;
        this.fechaHoraLiquidacion = fechaHoraLiquidacion;
        this.costoFinalUsd = costoFinalUsd;
        this.costoFinalArs = costoFinalArs;
        this.pagosAprobadosArs = pagosAprobadosArs;
        this.reembolsosConfirmadosArs = reembolsosConfirmadosArs;
        this.netoConfirmadoArs = netoConfirmadoArs;
        this.saldoPendienteArs = saldoPendienteArs;
        this.reembolsoPendienteArs = reembolsoPendienteArs;
    }

    public int getAlquilerId() {
        return alquilerId;
    }

    public Integer getVersion() {
        return version;
    }

    public String getSituacionFinanciera() {
        return situacionFinanciera;
    }

    public LocalDateTime getFechaHoraLiquidacion() {
        return fechaHoraLiquidacion;
    }

    public BigDecimal getCostoFinalUsd() {
        return costoFinalUsd;
    }

    public BigDecimal getCostoFinalArs() {
        return costoFinalArs;
    }

    public BigDecimal getPagosAprobadosArs() {
        return pagosAprobadosArs;
    }

    public BigDecimal getReembolsosConfirmadosArs() {
        return reembolsosConfirmadosArs;
    }

    public BigDecimal getNetoConfirmadoArs() {
        return netoConfirmadoArs;
    }

    public BigDecimal getSaldoPendienteArs() {
        return saldoPendienteArs;
    }

    public BigDecimal getReembolsoPendienteArs() {
        return reembolsoPendienteArs;
    }

    public String getMoneda() {
        return "ARS";
    }
}
