package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.Pago;
import DSII.TurismoSustentable.models.Reembolso;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;

@Service
@Profile("refactor")
public class PasarelaSimulada {

    private static final String PREFIJO = "pagos.simulados.";

    private final Environment propiedades;
    private final Clock reloj;
    private final BigDecimal saldoPredeterminado;

    public PasarelaSimulada(
            Environment propiedades,
            Clock reloj
    ) {
        this.propiedades = Objects.requireNonNull(propiedades);
        this.reloj = Objects.requireNonNull(reloj);

        this.saldoPredeterminado = leerMonto(
                PREFIJO + "saldo-inicial-ars",
                "1000000.00"
        );
    }

    public BigDecimal obtenerSaldoInicial(String nickname) {
        return leerMonto(
                prefijoCuenta(nickname) + "saldo-inicial-ars",
                saldoPredeterminado.toPlainString()
        );
    }

    /*
     * Devuelve una respuesta del proveedor simulado.
     * No modifica Pago, Alquiler, saldos ni historiales.
     * El saldo disponible debe calcularlo el servicio financiero.
     */
    public String evaluarPago(
            Pago pago,
            BigDecimal saldoDisponibleArs
    ) {
        if (pago == null) {
            throw new ConfiguracionSistemaException(
                    "Falta el pago que debe evaluar el simulador."
            );
        }

        if (!"SIMULADA".equals(pago.getPlataforma())
                || !"PRUEBA".equals(pago.getEntorno())
                || !"ARS".equals(pago.getMoneda())) {
            throw new ConfiguracionSistemaException(
                    "El simulador solo admite pagos SIMULADA/PRUEBA en ARS."
            );
        }

        BigDecimal importe = validarMonto(
                pago.getMontoArs(),
                false
        );

        String estadoActual = pago.getEstadoPago() == null
                ? null
                : pago.getEstadoPago().getCodigo();

        // No reevaluar un resultado que ya fue registrado como terminal.
        if ("APROBADO".equals(estadoActual)
                || "RECHAZADO".equals(estadoActual)
                || "CANCELADO".equals(estadoActual)) {
            return estadoActual;
        }

        if (!"CREADO".equals(estadoActual)
                && !"PENDIENTE".equals(estadoActual)) {
            throw new ConfiguracionSistemaException(
                    "El pago no tiene un estado válido para su evaluación."
            );
        }

        BigDecimal disponible = validarMonto(
                saldoDisponibleArs,
                true
        );

        if (pago.getAlquiler() == null
                || pago.getAlquiler().getUsuario() == null) {
            throw new ConfiguracionSistemaException(
                    "El pago no tiene un cliente identificado."
            );
        }

        String prefijo = prefijoCuenta(
                pago.getAlquiler().getUsuario().getNickname()
        );

        String modo = propiedades.getProperty(
                prefijo + "resultado",
                "AUTOMATICO"
        ).trim().toUpperCase(Locale.ROOT);

        switch (modo) {
            case "RECHAZADO":
                return "RECHAZADO";

            case "CANCELADO":
                return "CANCELADO";

            case "PENDIENTE":
                LocalDateTime ahora = LocalDateTime.ofInstant(
                        reloj.instant(),
                        ZoneOffset.UTC
                );

                if (pago.getFechaCreacion() == null
                        || ahora.isBefore(pago.getFechaCreacion())) {
                    throw new ConfiguracionSistemaException(
                            "La fecha de creación del pago no es válida."
                    );
                }

                long segundosTranscurridos = Duration.between(
                        pago.getFechaCreacion(),
                        ahora
                ).getSeconds();

                if (segundosTranscurridos < leerDemora(prefijo)) {
                    return "PENDIENTE";
                }

                // Al volver a consultar después de la demora,
                // resolver según los fondos disponibles en ese momento.
                break;

            case "AUTOMATICO":
                break;

            default:
                throw new ConfiguracionSistemaException(
                        "El resultado configurado para el simulador no es válido."
                );
        }

        return disponible.compareTo(importe) >= 0
                ? "APROBADO"
                : "RECHAZADO";
    }

    public String evaluarReembolso(Reembolso reembolso) {
        if (reembolso == null || reembolso.getPago() == null || reembolso.getFechaCreacion() == null
                || reembolso.getPago().getAlquiler() == null || reembolso.getPago().getAlquiler().getUsuario() == null) {
            throw new ConfiguracionSistemaException("Faltan datos para evaluar el reembolso simulado.");
        }
        Pago pago = reembolso.getPago();
        if (!"SIMULADA".equals(pago.getPlataforma()) || !"PRUEBA".equals(pago.getEntorno())
                || !"ARS".equals(reembolso.getMoneda())) {
            throw new ConfiguracionSistemaException("El simulador solo procesa reembolsos SIMULADA/PRUEBA en ARS.");
        }
        validarMonto(reembolso.getMontoArs(), false);
        String prefijo = prefijoCuenta(pago.getAlquiler().getUsuario().getNickname());
        String modo = propiedades.getProperty(prefijo + "reembolso-resultado", "AUTOMATICO")
                .trim().toUpperCase(Locale.ROOT);
        if ("FALLIDO".equals(modo)) return "FALLIDO";
        if ("EN_PROCESO".equals(modo)) {
            LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
            if (ahora.isBefore(reembolso.getFechaCreacion())) {
                throw new ConfiguracionSistemaException("La fecha del reembolso está en el futuro.");
            }
            if (Duration.between(reembolso.getFechaCreacion(), ahora).getSeconds() < leerDemora(prefijo)) {
                return "EN_PROCESO";
            }
        } else if (!"AUTOMATICO".equals(modo)) {
            throw new ConfiguracionSistemaException("El resultado de reembolso simulado no es válido.");
        }
        return "CONFIRMADO";
    }

    private String prefijoCuenta(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new ConfiguracionSistemaException(
                    "Falta el nickname de la cuenta de prueba."
            );
        }

        return PREFIJO + "cuentas."
                + nickname.trim().toLowerCase(Locale.ROOT)
                + ".";
    }

    private long leerDemora(String prefijo) {
        String texto = propiedades.getProperty(
                prefijo + "demora-segundos",
                "20"
        );

        try {
            long segundos = Long.parseLong(texto.trim());

            if (segundos <= 0) {
                throw new ConfiguracionSistemaException(
                        "La demora simulada debe ser mayor que cero."
                );
            }

            return segundos;

        } catch (NumberFormatException ex) {
            throw new ConfiguracionSistemaException(
                    "La demora simulada no tiene un formato válido."
            );
        }
    }

    private BigDecimal leerMonto(
            String propiedad,
            String predeterminado
    ) {
        String texto = propiedades.getProperty(
                propiedad,
                predeterminado
        );

        try {
            return validarMonto(
                    new BigDecimal(texto.trim()),
                    true
            );

        } catch (NumberFormatException ex) {
            throw new ConfiguracionSistemaException(
                    "Un saldo inicial simulado no tiene un formato válido."
            );
        }
    }

    private BigDecimal validarMonto(
            BigDecimal valor,
            boolean admiteCero
    ) {
        if (valor == null
                || valor.signum() < 0
                || (!admiteCero && valor.signum() == 0)) {
            throw new ConfiguracionSistemaException(
                    "El importe o saldo recibido por el simulador no es válido."
            );
        }

        final BigDecimal normalizado;

        try {
            normalizado = valor.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );

        } catch (ArithmeticException ex) {
            throw new ConfiguracionSistemaException(
                    "Los importes simulados deben admitir dos decimales sin redondeo."
            );
        }

        if (normalizado.precision() - normalizado.scale() > 19) {
            throw new ConfiguracionSistemaException(
                    "El importe simulado supera la capacidad permitida."
            );
        }

        return normalizado;
    }
}
