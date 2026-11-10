package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.CotizacionDolarDTO;
import DSII.TurismoSustentable.exceptions.CotizacionNoDisponibleException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;

@Service
public class CotizacionDolarService {

    private static final String URL =
            "https://dolarapi.com/v1/dolares/tarjeta";

    private static final String FUENTE =
            "DolarAPI - dolar tarjeta - venta";

    private static final Duration TOLERANCIA_FECHA_FUTURA =
            Duration.ofMinutes(5);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Clock reloj;
    private final Duration antiguedadMaxima;

    public CotizacionDolarService(
            RestTemplateBuilder builder,
            ObjectMapper objectMapper,
            Clock reloj,
            @Value("${cotizacion.max-antiguedad-dias:7}")
            long maxAntiguedadDias
    ) {
        if (maxAntiguedadDias <= 0) {
            throw new IllegalArgumentException(
                    "La antigüedad máxima debe ser mayor que cero."
            );
        }

        this.reloj = Objects.requireNonNull(
                reloj,
                "El reloj de la aplicación es obligatorio."
        );

        this.antiguedadMaxima = Duration.ofDays(maxAntiguedadDias);

        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();

        this.objectMapper = objectMapper.copy()
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    }

    public CotizacionDolarDTO obtenerCotizacion() {
        try {
            String cuerpo = restTemplate.getForObject(URL, String.class);

            if (cuerpo == null || cuerpo.isBlank()) {
                throw new IllegalArgumentException(
                        "El proveedor devolvió una respuesta vacía."
                );
            }

            JsonNode respuesta = objectMapper.readTree(cuerpo);

            if (respuesta == null || !respuesta.isObject()) {
                throw new IllegalArgumentException(
                        "La respuesta del proveedor no es un objeto válido."
                );
            }

            if (!"USD".equals(respuesta.path("moneda").asText())
                    || !"tarjeta".equals(respuesta.path("casa").asText())) {
                throw new IllegalArgumentException(
                        "La respuesta no corresponde al dólar tarjeta en USD."
                );
            }

            JsonNode venta = respuesta.get("venta");

            if (venta == null || !venta.isNumber()) {
                throw new IllegalArgumentException(
                        "El proveedor no informó un valor de venta numérico."
                );
            }

            OffsetDateTime fechaActualizacion = null;
            JsonNode fecha = respuesta.get("fechaActualizacion");

            if (fecha != null && !fecha.isNull()) {
                if (!fecha.isTextual() || fecha.asText().isBlank()) {
                    throw new IllegalArgumentException(
                            "La fecha de actualización informada no es válida."
                    );
                }

                fechaActualizacion = OffsetDateTime.parse(
                        fecha.asText().trim()
                );
            }

            validarFechaActualizacion(fechaActualizacion);

            return new CotizacionDolarDTO(
                    venta.decimalValue(),
                    FUENTE,
                    fechaActualizacion
            );

        } catch (RestClientException
                 | JsonProcessingException
                 | DateTimeParseException
                 | IllegalArgumentException ex) {

            throw new CotizacionNoDisponibleException(ex);
        }
    }

    private void validarFechaActualizacion(OffsetDateTime fecha) {
        if (fecha == null) {
            return;
        }

        Instant ahora = reloj.instant();
        Instant actualizacion = fecha.toInstant();

        if (actualizacion.isBefore(ahora.minus(antiguedadMaxima))) {
            throw new IllegalArgumentException(
                    "La cotización supera la antigüedad máxima permitida."
            );
        }

        if (actualizacion.isAfter(ahora.plus(TOLERANCIA_FECHA_FUTURA))) {
            throw new IllegalArgumentException(
                    "La fecha de cotización está demasiado adelantada."
            );
        }
    }
}
