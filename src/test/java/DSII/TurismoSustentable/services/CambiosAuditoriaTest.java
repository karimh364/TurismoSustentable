package DSII.TurismoSustentable.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CambiosAuditoriaTest {
    private static final String ESTACION = """
            {"id":2,"nombre":"Taller de prueba","direccion":"dirección reservada","capacidad":10,
             "esPublica":false,"borradoEstacion":false,"latitud":-53.770000,"longitud":-67.71}
            """;

    private final CambiosAuditoria interprete = new CambiosAuditoria(new ObjectMapper());

    @Test
    void inactivacionUsaLosDosValoresHistoricosExactos() {
        assertThat(interprete.interpretar("USUARIO", "INACTIVAR", "activo=true", "activo=false"))
                .extracting(c -> c.campo, c -> c.antes, c -> c.despues, c -> c.valoresReservados)
                .containsExactly(tuple("activo", true, false, false));
        assertThat(interprete.interpretar("USUARIO", "INACTIVAR", "activo=true;token=privado", "activo=false"))
                .isEmpty();
    }

    @Test
    void edicionDeUsuarioSoloExponeLosNombresDeCamposPermitidosQueCambiaron() {
        String antes = """
                {"id":8,"nombre":"Anterior","apellido":"Igual","email":"anterior@example.test",
                 "fechaNacimiento":null,"generoId":1,"passwordHash":"hash-inicial"}
                """;
        String despues = """
                {"id":8,"nombre":"Nuevo","apellido":"Igual","email":"nuevo@example.test",
                 "fechaNacimiento":"2000-03-19","generoId":1,"passwordHash":"hash-nuevo"}
                """;
        assertThat(interprete.interpretar("USUARIO", "ACTUALIZAR", antes, despues))
                .extracting(c -> c.campo, c -> c.antes, c -> c.despues, c -> c.valoresReservados)
                .containsExactly(tuple("email", null, null, true), tuple("nombre", null, null, true),
                        tuple("fechaNacimiento", null, null, true));
    }

    @Test
    void altaDeEstacionTieneDatosOperativosPeroNoUnEstadoAnteriorInventado() {
        assertThat(interprete.interpretar("ESTACION", "CREAR", null, ESTACION))
                .extracting(c -> c.campo, c -> c.antes, c -> c.despues, c -> c.valoresReservados)
                .containsExactly(tuple("capacidad", null, 10, false), tuple("esPublica", null, false, false),
                        tuple("borradoEstacion", null, false, false));
    }

    @Test
    void edicionDeEstacionIgnoraDiferenciasDeEscalaYReservaLaDireccion() {
        String nuevos = ESTACION.replace("10", "15").replace("dirección reservada", "otra dirección reservada")
                .replace("-53.770000", "-53.77");
        assertThat(interprete.interpretar("ESTACION", "ACTUALIZAR", ESTACION, nuevos))
                .extracting(c -> c.campo, c -> c.antes, c -> c.despues, c -> c.valoresReservados)
                .containsExactly(tuple("capacidad", 10, 15, false), tuple("direccion", null, null, true));
    }

    @Test
    void bajaLogicaDeEstacionSoloMuestraElCambioDeEstado() {
        String baja = ESTACION.replace("\"borradoEstacion\":false", "\"borradoEstacion\":true");
        assertThat(interprete.interpretar("ESTACION", "ELIMINAR", ESTACION, baja))
                .extracting(c -> c.campo, c -> c.antes, c -> c.despues, c -> c.valoresReservados)
                .containsExactly(tuple("borradoEstacion", false, true, false));
        assertThat(interprete.interpretar("ESTACION", "ELIMINAR", ESTACION, ESTACION)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "2147483648", "10.5", "\"10\"", "{\"valor\": 10}", "null"})
    void capacidadNoEnteraONoValidaNoEsInterpretable(String capacidad) {
        assertThat(interprete.interpretar("ESTACION", "ACTUALIZAR", ESTACION,
                ESTACION.replace("\"capacidad\":10", "\"capacidad\":" + capacidad))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"roles\":[\"CLIENTE\",\"token-privado\"]}",
            "{\"roles\":[\"CLIENTE\",\"CLIENTE\"]}", "{\"roles\":\"CLIENTE\"}", "{\"roles\":[null]}",
            "{\"roles\":[\"TECNICO\"],\"roles\":[\"CLIENTE\"]}", "{\"roles\":[\"TECNICO\"]} {}", ""})
    void formatosAmbiguosORolesNoReconocidosNoExponenDetalle(String nuevos) {
        assertThat(interprete.interpretar("USUARIO", "CAMBIAR_ROLES", "{\"roles\":[\"CLIENTE\"]}", nuevos))
                .isEmpty();
    }

    @Test
    void noConfundeUnAntesDesconocidoConUnAlta() {
        assertThat(interprete.interpretar("ESTACION", "ACTUALIZAR", null, ESTACION)).isEmpty();
        assertThat(interprete.interpretar("ESTACION", "CREAR", "{}", ESTACION)).isEmpty();
    }
}
