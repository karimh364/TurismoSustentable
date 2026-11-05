package DSII.TurismoSustentable.validation;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoIdentidadTest {

    @Test
    void unificaDniConPuntosYEspaciosConservandoLosCeros() {
        assertEquals("00123456", DocumentoIdentidad.validarYNormalizar(
                " 00.123.\u00a0456 ", "DNI", "AR"
        ));
        assertEquals("00123456", DocumentoIdentidad.validarYNormalizar(
                "00123456", "DNI", "AR"
        ));
    }

    @Test
    void normalizaPasaporteConLetrasYNumerosParaPaisesExtranjerosOArgentina() {
        assertEquals("AB0012", DocumentoIdentidad.validarYNormalizar(
                " ab 0012 ", "PASAPORTE", "CL"
        ));
        assertEquals("AB0012", DocumentoIdentidad.validarYNormalizar(
                "ab0012", "PASAPORTE", "AR"
        ));
        assertEquals("0012", DocumentoIdentidad.validarYNormalizar(
                "0012", "PASAPORTE", "UY"
        ));
    }

    @Test
    void rechazaDniExtranjeroYValoresQueNoSonNumericos() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentoIdentidad.validarYNormalizar("12345678", "DNI", "CL"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentoIdentidad.validarYNormalizar("AB123", "DNI", "AR"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentoIdentidad.validarYNormalizar("12-345", "DNI", "AR"));
    }

    @Test
    void rechazaSimbolosDelPasaporteSinCambiarLaIdentificacion() {
        for (String valor : new String[]{"AB-12", "AB.12", "AB!?", "Ñ123"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> DocumentoIdentidad.validarYNormalizar(valor, "PASAPORTE", "CL"));
        }
    }

    @Test
    void mantieneLaCapacidadDelPasaporteDe50SinTruncar() {
        String numero = "0".repeat(50);
        assertEquals(numero, DocumentoIdentidad.validarYNormalizar(numero, "PASAPORTE", "AR"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentoIdentidad.validarYNormalizar(numero + "0", "PASAPORTE", "AR"));
    }

    @Test
    void limitaDniAOchoDigitosDespuesDeNormalizarSinRecortarlo() {
        assertEquals("1234567", DocumentoIdentidad.validarYNormalizar("1.234.567", "DNI", "AR"));
        assertEquals("00123456", DocumentoIdentidad.validarYNormalizar("00.123.456", "DNI", "AR"));
        for (String numero : new String[]{"123456789", "12.345.6789", "0".repeat(50)}) {
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> DocumentoIdentidad.validarYNormalizar(numero, "DNI", "AR"));
            assertEquals("El DNI no puede superar los 8 dígitos.", error.getMessage());
        }
    }

    @Test
    void separaDatosObligatoriosDeCatalogosNoSoportados() {
        assertThrows(IllegalArgumentException.class,
                () -> DocumentoIdentidad.validarYNormalizar(null, "DNI", "AR"));
        assertThrows(IllegalArgumentException.class,
                () -> DocumentoIdentidad.validarYNormalizar(" . . ", "DNI", "AR"));
        assertThrows(ConfiguracionSistemaException.class,
                () -> DocumentoIdentidad.validarYNormalizar("123", "OTRO", "AR"));
    }
}
