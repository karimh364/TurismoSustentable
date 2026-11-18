package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AuditoriaDetalleDTO.Cambio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.MissingNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Interpreta únicamente los formatos de auditoría conocidos; no reenvía valores libres. */
final class CambiosAuditoria {
    private static final Set<String> ROLES = Set.of("ADMIN", "CLIENTE", "TECNICO");
    private static final int MAXIMO_CARACTERES_SNAPSHOT = 8192;
    private final ObjectReader lector;

    CambiosAuditoria(ObjectMapper json) {
        lector = json.reader()
                .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    List<Cambio> interpretar(String entidad, String accion, String anteriores, String nuevos) {
        if (accion == null) return List.of();
        if ("USUARIO".equals(entidad)) {
            switch (accion) {
                case "CAMBIAR_ROLES":
                    List<String> antes = roles(leer(anteriores).path("roles"));
                    List<String> despues = roles(leer(nuevos).path("roles"));
                    return antes == null || despues == null || antes.equals(despues)
                            ? List.of() : List.of(new Cambio("roles", antes, despues, false));
                case "INACTIVAR":
                    return "activo=true".equals(anteriores) && "activo=false".equals(nuevos)
                            ? List.of(new Cambio("activo", true, false, false)) : List.of();
                case "ACTUALIZAR":
                    List<Cambio> cambios = new ArrayList<>();
                    return marcarReservados(cambios, leer(anteriores), leer(nuevos),
                            "email", "nombre", "apellido", "fechaNacimiento", "generoId")
                            ? cambios : List.of();
                default:
                    return List.of();
            }
        }
        if ("ESTACION".equals(entidad)) {
            switch (accion) {
                case "CREAR":
                    return anteriores == null ? estacion(null, leer(nuevos)) : List.of();
                case "ACTUALIZAR":
                    return estacion(leer(anteriores), leer(nuevos));
                case "ELIMINAR":
                    return BooleanNode.FALSE.equals(leer(anteriores).path("borradoEstacion"))
                            && BooleanNode.TRUE.equals(leer(nuevos).path("borradoEstacion"))
                            ? List.of(new Cambio("borradoEstacion", false, true, false)) : List.of();
                default:
                    return List.of();
            }
        }
        return List.of();
    }

    private List<Cambio> estacion(JsonNode antes, JsonNode despues) {
        if (!estacionValida(despues) || (antes != null && !estacionValida(antes))) return List.of();

        List<Cambio> cambios = new ArrayList<>();
        comparar(cambios, "capacidad", antes == null ? null : antes.path("capacidad").intValue(),
                despues.path("capacidad").intValue());
        for (String campo : List.of("esPublica", "borradoEstacion")) {
            comparar(cambios, campo, antes == null ? null : antes.path(campo).booleanValue(),
                    despues.path(campo).booleanValue());
        }
        return antes == null || marcarReservados(cambios, antes, despues,
                "nombre", "direccion", "latitud", "longitud") ? cambios : List.of();
    }

    private boolean estacionValida(JsonNode datos) {
        JsonNode capacidad = datos.path("capacidad");
        return capacidad.isIntegralNumber() && capacidad.canConvertToInt() && capacidad.intValue() >= 0
                && datos.path("esPublica").isBoolean() && datos.path("borradoEstacion").isBoolean();
    }

    private void comparar(List<Cambio> cambios, String campo, Object antes, Object despues) {
        if (!despues.equals(antes)) cambios.add(new Cambio(campo, antes, despues, false));
    }

    private boolean marcarReservados(List<Cambio> cambios, JsonNode antes, JsonNode despues, String... campos) {
        for (String campo : campos) {
            JsonNode previo = antes.path(campo);
            JsonNode nuevo = despues.path(campo);
            // Un campo omitido no es lo mismo que uno explícitamente nulo.
            if (!previo.isValueNode() || !nuevo.isValueNode()) return false;
            boolean iguales = previo.isNumber() && nuevo.isNumber()
                    ? Double.compare(previo.doubleValue(), nuevo.doubleValue()) == 0
                    : previo.equals(nuevo);
            if (!iguales) cambios.add(new Cambio(campo, null, null, true));
        }
        return true;
    }

    private List<String> roles(JsonNode datos) {
        if (!datos.isArray() || datos.size() > ROLES.size()) return null;
        Set<String> codigos = new TreeSet<>();
        for (JsonNode valor : datos) {
            if (!valor.isTextual() || !ROLES.contains(valor.textValue()) || !codigos.add(valor.textValue())) {
                return null;
            }
        }
        return List.copyOf(codigos);
    }

    private JsonNode leer(String datos) {
        if (datos != null && datos.length() <= MAXIMO_CARACTERES_SNAPSHOT) {
            try {
                JsonNode valor = lector.readTree(datos);
                if (valor != null && valor.isObject()) return valor;
            } catch (JsonProcessingException ignorado) {
                // Un registro no interpretable conserva sus metadatos; no se loguea el contenido.
            }
        }
        return MissingNode.getInstance();
    }
}
