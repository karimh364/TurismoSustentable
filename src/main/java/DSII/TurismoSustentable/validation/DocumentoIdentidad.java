package DSII.TurismoSustentable.validation;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;

import java.util.Locale;

public final class DocumentoIdentidad {

    public static final int LONGITUD_MAXIMA = 50;
    public static final int LONGITUD_MAXIMA_DNI = 8;

    private DocumentoIdentidad() {
    }

    public static String normalizarBase(String valor) {
        return valor == null ? null : valor
                .replaceAll("(?U)\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    public static String normalizar(String valor, String tipo) {
        String numero = normalizarBase(valor);
        return numero != null && "DNI".equals(tipo)
                ? numero.replace(".", "")
                : numero;
    }

    public static String validarYNormalizar(
            String valor, String tipo, String paisEmisor
    ) {
        if (!"DNI".equals(tipo) && !"PASAPORTE".equals(tipo)) {
            throw new ConfiguracionSistemaException(
                    "El catálogo contiene un tipo de documento no soportado."
            );
        }

        String numero = normalizar(valor, tipo);
        if (numero == null || numero.isEmpty()) {
            throw new IllegalArgumentException(
                    "El número de documento es obligatorio."
            );
        }
        if ("DNI".equals(tipo)) {
            if (!"AR".equals(paisEmisor)) {
                throw new IllegalArgumentException(
                        "El DNI solo se admite con Argentina como país emisor."
                );
            }
            if (!numero.matches("[0-9]+")) {
                throw new IllegalArgumentException(
                        "El DNI debe contener únicamente números."
                );
            }
            if (numero.length() > LONGITUD_MAXIMA_DNI) {
                throw new IllegalArgumentException(
                        "El DNI no puede superar los 8 dígitos."
                );
            }
        } else {
            if (numero.length() > LONGITUD_MAXIMA) {
                throw new IllegalArgumentException(
                        "El pasaporte no puede superar los 50 caracteres."
                );
            }
            if (!numero.matches("[A-Z0-9]+")) {
                throw new IllegalArgumentException(
                        "El pasaporte solo puede contener letras de A a Z y números."
                );
            }
        }

        return numero;
    }
}
