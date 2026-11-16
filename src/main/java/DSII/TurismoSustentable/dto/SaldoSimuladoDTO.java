package DSII.TurismoSustentable.dto;

import java.math.BigDecimal;

public class SaldoSimuladoDTO {

    private final int usuarioId;
    private final String nickname;
    private final BigDecimal saldoInicialArs;
    private final BigDecimal pagosAprobadosArs;
    private final BigDecimal reembolsosConfirmadosArs;
    private final BigDecimal saldoDisponibleArs;

    public SaldoSimuladoDTO(
            int usuarioId,
            String nickname,
            BigDecimal saldoInicialArs,
            BigDecimal pagosAprobadosArs,
            BigDecimal reembolsosConfirmadosArs,
            BigDecimal saldoDisponibleArs
    ) {
        this.usuarioId = usuarioId;
        this.nickname = nickname;
        this.saldoInicialArs = saldoInicialArs;
        this.pagosAprobadosArs = pagosAprobadosArs;
        this.reembolsosConfirmadosArs = reembolsosConfirmadosArs;
        this.saldoDisponibleArs = saldoDisponibleArs;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getMoneda() {
        return "ARS";
    }

    public String getPlataforma() {
        return "SIMULADA";
    }

    public String getEntorno() {
        return "PRUEBA";
    }

    public BigDecimal getSaldoInicialArs() {
        return saldoInicialArs;
    }

    public BigDecimal getPagosAprobadosArs() {
        return pagosAprobadosArs;
    }

    public BigDecimal getReembolsosConfirmadosArs() {
        return reembolsosConfirmadosArs;
    }

    public BigDecimal getSaldoDisponibleArs() {
        return saldoDisponibleArs;
    }
}