package DSII.TurismoSustentable.dto;

import java.util.List;

public class ResultadoReembolsoDTO {
    private final LiquidacionAlquilerDTO liquidacion;
    private final List<ReembolsoDTO> reembolsos;

    public ResultadoReembolsoDTO(LiquidacionAlquilerDTO liquidacion, List<ReembolsoDTO> reembolsos) {
        this.liquidacion = liquidacion;
        this.reembolsos = List.copyOf(reembolsos);
    }
    public LiquidacionAlquilerDTO getLiquidacion() { return liquidacion; }
    public List<ReembolsoDTO> getReembolsos() { return reembolsos; }
}
