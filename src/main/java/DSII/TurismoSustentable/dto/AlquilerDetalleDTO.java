package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.models.AlquilerDetalle;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AlquilerDetalleDTO {

    private final Integer id;
    private final Integer version;

    private final int bicicletaId;
    private final String codigoBicicleta;

    private final Integer tipoBicicletaContratadoId;
    private final String tipoBicicletaContratado;
    private final String estado;

    private final BigDecimal precioPorHoraAplicadoUsd;
    private final Integer duracionEstimadaSegundos;

    private final LocalDateTime fechaHoraRetiro;
    private final LocalDateTime fechaHoraDevolucionEstimada;
    private final LocalDateTime fechaHoraDevolucionReal;
    private final LocalDateTime fechaHoraFinUso;
    private final LocalDateTime fechaHoraCierreDetalle;

    private final Integer estacionDevolucionId;
    private final String estacionDevolucion;

    private final BigDecimal subtotalEstimadoUsd;
    private final BigDecimal subtotalEstimadoArs;
    private final BigDecimal subtotalRealUsd;
    private final BigDecimal subtotalRealArs;

    private final boolean fallaReportada;

    public AlquilerDetalleDTO(AlquilerDetalle detalle) {
        this.id = detalle.getId();
        this.version = detalle.getVersion();

        this.bicicletaId = detalle.getBicicleta().getId();
        this.codigoBicicleta =
                detalle.getBicicleta().getCodigoBicicleta();

        this.tipoBicicletaContratadoId =
                detalle.getTipoBicicletaContratado().getId();

        this.tipoBicicletaContratado =
                detalle.getTipoBicicletaContratado().getCodigo();

        this.estado = detalle.getEstado().getCodigo();

        // Importes contratados, no la tarifa actual del tipo.
        this.precioPorHoraAplicadoUsd =
                detalle.getPrecioPorHoraAplicadoUsd();

        this.duracionEstimadaSegundos =
                detalle.getDuracionEstimadaSegundos();

        this.fechaHoraRetiro = detalle.getFechaHoraRetiro();
        this.fechaHoraDevolucionEstimada =
                detalle.getFechaHoraDevolucionEstimada();
        this.fechaHoraDevolucionReal =
                detalle.getFechaHoraDevolucionReal();
        this.fechaHoraFinUso = detalle.getFechaHoraFinUso();
        this.fechaHoraCierreDetalle =
                detalle.getFechaHoraCierreDetalle();

        this.estacionDevolucionId =
                detalle.getEstacionDevolucion() == null
                        ? null
                        : detalle.getEstacionDevolucion().getId();

        this.estacionDevolucion =
                detalle.getEstacionDevolucion() == null
                        ? null
                        : detalle.getEstacionDevolucion().getNombre();

        this.subtotalEstimadoUsd = detalle.getSubtotalEstimadoUsd();
        this.subtotalEstimadoArs = detalle.getSubtotalEstimadoArs();
        this.subtotalRealUsd = detalle.getSubtotalRealUsd();
        this.subtotalRealArs = detalle.getSubtotalRealArs();

        this.fallaReportada = detalle.isFallaReportada();
    }

    public Integer getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
    }

    public int getBicicletaId() {
        return bicicletaId;
    }

    public String getCodigoBicicleta() {
        return codigoBicicleta;
    }

    public Integer getTipoBicicletaContratadoId() {
        return tipoBicicletaContratadoId;
    }

    public String getTipoBicicletaContratado() {
        return tipoBicicletaContratado;
    }

    public String getEstado() {
        return estado;
    }

    public BigDecimal getPrecioPorHoraAplicadoUsd() {
        return precioPorHoraAplicadoUsd;
    }

    public Integer getDuracionEstimadaSegundos() {
        return duracionEstimadaSegundos;
    }

    public LocalDateTime getFechaHoraRetiro() {
        return fechaHoraRetiro;
    }

    public LocalDateTime getFechaHoraDevolucionEstimada() {
        return fechaHoraDevolucionEstimada;
    }

    public LocalDateTime getFechaHoraDevolucionReal() {
        return fechaHoraDevolucionReal;
    }

    public LocalDateTime getFechaHoraFinUso() {
        return fechaHoraFinUso;
    }

    public LocalDateTime getFechaHoraCierreDetalle() {
        return fechaHoraCierreDetalle;
    }

    public Integer getEstacionDevolucionId() {
        return estacionDevolucionId;
    }

    public String getEstacionDevolucion() {
        return estacionDevolucion;
    }

    public BigDecimal getSubtotalEstimadoUsd() {
        return subtotalEstimadoUsd;
    }

    public BigDecimal getSubtotalEstimadoArs() {
        return subtotalEstimadoArs;
    }

    public BigDecimal getSubtotalRealUsd() {
        return subtotalRealUsd;
    }

    public BigDecimal getSubtotalRealArs() {
        return subtotalRealArs;
    }

    public boolean isFallaReportada() {
        return fallaReportada;
    }
}
