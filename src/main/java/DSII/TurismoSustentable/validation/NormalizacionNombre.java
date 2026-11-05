package DSII.TurismoSustentable.validation;

import java.text.Normalizer;

public final class NormalizacionNombre {

    public static final String PATRON =
            "^$|^\\p{L}[\\p{L}\\p{M}]*(?:[ '\\-]\\p{L}[\\p{L}\\p{M}]*)*$";

    private NormalizacionNombre() {
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }

        return Normalizer.normalize(valor, Normalizer.Form.NFC)
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u02BC', '\'')
                .replace('\u2010', '-')
                .replace('\u2011', '-')
                .replaceAll("(?U)\\s+", " ")
                .trim();
    }
}
