package DSII.TurismoSustentable.dto;
import DSII.TurismoSustentable.models.TipoBicicleta;
import java.math.BigDecimal;

public class TipoBicicletaDTO {
    private final Integer id;
    private final String codigo;
    private final BigDecimal precioPorHoraUsd;
    private final Integer intervaloPreventivoMeses;
    public TipoBicicletaDTO(TipoBicicleta tipo) {
        id = tipo.getId();
        codigo = tipo.getCodigo();
        precioPorHoraUsd = tipo.getPrecioPorHoraUsd();
        intervaloPreventivoMeses = tipo.getIntervaloPreventivoMeses();
    }
    public Integer getId() { return id; }
    public String getCodigo() { return codigo; }
    public BigDecimal getPrecioPorHoraUsd() { return precioPorHoraUsd; }
    public Integer getIntervaloPreventivoMeses() { return intervaloPreventivoMeses; }
}
