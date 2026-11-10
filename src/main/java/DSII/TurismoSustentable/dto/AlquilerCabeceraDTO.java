package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.AlquilerDetalle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AlquilerCabeceraDTO {

    private final int id;
    private final Integer version;
    private final int usuarioId;
    private final String estado;

    private final int estacionRetiroId;
    private final String estacionRetiro;

    private final LocalDateTime fechaHoraCreacion;
    private final LocalDateTime fechaHoraVencimientoReserva;
    private final LocalDateTime fechaHoraCierre;
    private final LocalDateTime fechaHoraCancelacion;
    private final LocalDateTime fechaHoraLiquidacion;

    private final BigDecimal cotizacionArsPorUsd;
    private final String fuenteCotizacion;
    private final LocalDateTime fechaHoraAplicacionCotizacion;
    private final LocalDateTime fechaHoraActualizacionFuente;

    private final BigDecimal costoEstimadoUsd;
    private final BigDecimal costoEstimadoArs;
    private final BigDecimal costoFinalUsd;
    private final BigDecimal costoFinalArs;

    private final List<AlquilerDetalleDTO> detalles;

    public AlquilerCabeceraDTO(
            Alquiler alquiler,
            List<AlquilerDetalle> detalles
    ) {
        if (alquiler == null
                || alquiler.getEstadoContratacion() == null
                || detalles == null) {
            throw new ConfiguracionSistemaException(
                    "Faltan datos para consultar el alquiler."
            );
        }

        this.id = alquiler.getId();
        this.version = alquiler.getVersion();
        this.usuarioId = alquiler.getUsuario().getId();
        this.estado =
                alquiler.getEstadoContratacion().getCodigo();

        this.estacionRetiroId =
                alquiler.getUbicacionRetiro().getId();
        this.estacionRetiro =
                alquiler.getUbicacionRetiro().getNombre();

        this.fechaHoraCreacion = alquiler.getFechaHoraCreacion();
        this.fechaHoraVencimientoReserva =
                alquiler.getFechaHoraVencimientoReserva();
        this.fechaHoraCierre = alquiler.getFechaHoraCierre();
        this.fechaHoraCancelacion =
                alquiler.getFechaHoraCancelacion();
        this.fechaHoraLiquidacion =
                alquiler.getFechaHoraLiquidacion();

        // Cotización histórica aplicada a esta contratación.
        this.cotizacionArsPorUsd = alquiler.getCotizacionArsPorUsd();
        this.fuenteCotizacion = alquiler.getFuenteCotizacion();
        this.fechaHoraAplicacionCotizacion =
                alquiler.getFechaHoraAplicacionCotizacion();
        this.fechaHoraActualizacionFuente =
                alquiler.getFechaHoraActualizacionFuente();

        this.costoEstimadoUsd = alquiler.getCostoEstimadoUsd();
        this.costoEstimadoArs = alquiler.getCostoEstimadoArs();
        this.costoFinalUsd = alquiler.getCostoFinalUsd();
        this.costoFinalArs = alquiler.getCostoFinalArs();

        this.detalles = detalles.stream()
                .map(AlquilerDetalleDTO::new)
                .collect(Collectors.toUnmodifiableList());
    }

    public int getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public String getEstado() {
        return estado;
    }

    public int getEstacionRetiroId() {
        return estacionRetiroId;
    }

    public String getEstacionRetiro() {
        return estacionRetiro;
    }

    public LocalDateTime getFechaHoraCreacion() {
        return fechaHoraCreacion;
    }

    public LocalDateTime getFechaHoraVencimientoReserva() {
        return fechaHoraVencimientoReserva;
    }

    public LocalDateTime getFechaHoraCierre() {
        return fechaHoraCierre;
    }

    public LocalDateTime getFechaHoraCancelacion() {
        return fechaHoraCancelacion;
    }

    public LocalDateTime getFechaHoraLiquidacion() {
        return fechaHoraLiquidacion;
    }

    public BigDecimal getCotizacionArsPorUsd() {
        return cotizacionArsPorUsd;
    }

    public String getFuenteCotizacion() {
        return fuenteCotizacion;
    }

    public LocalDateTime getFechaHoraAplicacionCotizacion() {
        return fechaHoraAplicacionCotizacion;
    }

    public LocalDateTime getFechaHoraActualizacionFuente() {
        return fechaHoraActualizacionFuente;
    }

    public BigDecimal getCostoEstimadoUsd() {
        return costoEstimadoUsd;
    }

    public BigDecimal getCostoEstimadoArs() {
        return costoEstimadoArs;
    }

    public BigDecimal getCostoFinalUsd() {
        return costoFinalUsd;
    }

    public BigDecimal getCostoFinalArs() {
        return costoFinalArs;
    }

    public List<AlquilerDetalleDTO> getDetalles() {
        return detalles;
    }
}
