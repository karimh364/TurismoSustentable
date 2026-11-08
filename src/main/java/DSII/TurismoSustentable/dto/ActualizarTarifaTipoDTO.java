package DSII.TurismoSustentable.dto;
import javax.validation.constraints.*;
import java.math.BigDecimal;

public class ActualizarTarifaTipoDTO {
    @NotNull(message = "El precio por hora es obligatorio.")
    @DecimalMin(value = "0.01", message = "El precio debe ser de al menos USD 0.01.")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 dígitos enteros y 2 decimales.")
    private BigDecimal precioPorHoraUsd;
    @NotNull(message = "Recargá la tarifa antes de editarla.")
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal precioAnterior;
    public BigDecimal getPrecioPorHoraUsd() { return precioPorHoraUsd; }
    public void setPrecioPorHoraUsd(BigDecimal precio) { precioPorHoraUsd = precio; }
    public BigDecimal getPrecioAnterior() { return precioAnterior; }
    public void setPrecioAnterior(BigDecimal precio) { precioAnterior = precio; }
}
