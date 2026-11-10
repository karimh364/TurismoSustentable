package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.CotizacionDolarDTO;
import DSII.TurismoSustentable.exceptions.CotizacionNoDisponibleException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CotizacionDolarServiceTest {

    private static final String URL =
            "https://dolarapi.com/v1/dolares/tarjeta";

    private static final String FECHA_JSON =
            "\"2026-11-10T12:00:00Z\"";

    private static final Instant AHORA =
            Instant.parse("2026-11-10T12:05:00Z");

    private RestTemplateBuilder builder;
    private RestTemplate restTemplate;
    private CotizacionDolarService servicio;

    @BeforeEach
    void preparar() {
        builder = mock(RestTemplateBuilder.class);
        restTemplate = mock(RestTemplate.class);

        when(builder.setConnectTimeout(any(Duration.class)))
                .thenReturn(builder);

        when(builder.setReadTimeout(any(Duration.class)))
                .thenReturn(builder);

        when(builder.build()).thenReturn(restTemplate);

        servicio = new CotizacionDolarService(
                builder,
                new ObjectMapper(),
                Clock.fixed(AHORA, ZoneOffset.UTC),
                7
        );
    }

    @Test
    void conservaValorDecimalFuenteYDesplazamientoHorario() {
        for (String fecha : new String[]{
                "2026-11-10T12:00:00Z",
                "2026-11-10T09:00:00-03:00"
        }) {
            when(restTemplate.getForObject(URL, String.class))
                    .thenReturn(respuesta(
                            "1530.12345678",
                            "\"" + fecha + "\""
                    ));

            CotizacionDolarDTO resultado = servicio.obtenerCotizacion();

            assertEquals(
                    new BigDecimal("1530.12345678"),
                    resultado.getCotizacionArsPorUsd()
            );

            assertEquals(
                    "DolarAPI - dolar tarjeta - venta",
                    resultado.getFuente()
            );

            assertEquals(
                    OffsetDateTime.parse(fecha),
                    resultado.getFechaActualizacionFuente()
            );
        }
    }

    @Test
    void permiteActualizacionAusenteONulaSinInventarla() {
        String sinFecha =
                "{\"moneda\":\"USD\",\"casa\":\"tarjeta\",\"venta\":1530}";

        for (String cuerpo : new String[]{
                sinFecha,
                respuesta("1530", "null")
        }) {
            when(restTemplate.getForObject(URL, String.class))
                    .thenReturn(cuerpo);

            CotizacionDolarDTO resultado = servicio.obtenerCotizacion();

            assertEquals(
                    new BigDecimal("1530"),
                    resultado.getCotizacionArsPorUsd()
            );
            assertNull(resultado.getFechaActualizacionFuente());
        }
    }

    @Test
    void rechazaCuerpoVacioMalformadoOIncompleto() {
        for (String cuerpo : new String[]{
                null,
                "",
                "   ",
                "null",
                "[]",
                "no es JSON",
                "{\"moneda\":\"USD\",\"casa\":\"tarjeta\"}"
        }) {
            when(restTemplate.getForObject(URL, String.class))
                    .thenReturn(cuerpo);

            comprobarRechazo();
        }
    }

    @Test
    void rechazaMonedaOTipoDeDolarIncorrectos() {
        String correcta = respuesta("1530", FECHA_JSON);

        for (String cuerpo : new String[]{
                correcta.replace("\"USD\"", "\"EUR\""),
                correcta.replace("\"tarjeta\"", "\"blue\"")
        }) {
            when(restTemplate.getForObject(URL, String.class))
                    .thenReturn(cuerpo);

            comprobarRechazo();
        }
    }

    @Test
    void rechazaVentaNoNumericaNoPositivaOFueraDePrecision() {
        for (String venta : new String[]{
                "null",
                "\"1530\"",
                "0",
                "-1",
                "1.123456789",
                "1000000000000"
        }) {
            when(restTemplate.getForObject(URL, String.class))
                    .thenReturn(respuesta(venta, FECHA_JSON));

            comprobarRechazo();
        }
    }

    @Test
    void rechazaFechaInformadaPeroInvalida() {
        for (String fecha : new String[]{
                "\"\"",
                "\"ayer\"",
                "\"2026-11-10T12:00:00\"",
                "123"
        }) {
            when(restTemplate.getForObject(URL, String.class))
                    .thenReturn(respuesta("1530", fecha));

            comprobarRechazo();
        }
    }

    @Test
    void convierteErrorHttpSinDevolverRespaldoNiReintentar() {
        when(restTemplate.getForObject(URL, String.class))
                .thenThrow(
                        new HttpServerErrorException(HttpStatus.BAD_GATEWAY)
                );

        comprobarRechazo();

        verify(restTemplate, times(1))
                .getForObject(URL, String.class);
    }

    @Test
    void convierteErrorDeConexionSinDevolverRespaldoNiReintentar() {
        when(restTemplate.getForObject(URL, String.class))
                .thenThrow(new ResourceAccessException("Conexión fallida."));

        comprobarRechazo();

        verify(restTemplate, times(1))
                .getForObject(URL, String.class);
    }

    @Test
    void noReutilizaLaCotizacionAnteriorSiLaSiguienteConsultaFalla() {
        when(restTemplate.getForObject(URL, String.class))
                .thenReturn(respuesta("1530", FECHA_JSON))
                .thenThrow(new ResourceAccessException("Conexión fallida."));

        assertEquals(
                new BigDecimal("1530"),
                servicio.obtenerCotizacion().getCotizacionArsPorUsd()
        );

        comprobarRechazo();

        verify(restTemplate, times(2))
                .getForObject(URL, String.class);
    }

    @Test
    void configuraLosTimeoutsSinConsultarDuranteLaConstruccion() {
        verify(builder).setConnectTimeout(Duration.ofSeconds(3));
        verify(builder).setReadTimeout(Duration.ofSeconds(5));
        verify(builder).build();

        verifyNoInteractions(restTemplate);
    }

    @Test
    void aceptaLosLimitesTemporalesExactos() {
        for (Instant fecha : new Instant[]{
                AHORA.minus(Duration.ofDays(7)),
                AHORA.plus(Duration.ofMinutes(5))
        }) {
            when(restTemplate.getForObject(URL, String.class))
                    .thenReturn(respuesta(
                            "1530",
                            "\"" + fecha + "\""
                    ));

            assertDoesNotThrow(servicio::obtenerCotizacion);
        }
    }

    @Test
    void rechazaFechasApenasFueraDeLosLimites() {
        for (Instant fecha : new Instant[]{
                AHORA.minus(Duration.ofDays(7)).minusNanos(1),
                AHORA.plus(Duration.ofMinutes(5)).plusNanos(1)
        }) {
            when(restTemplate.getForObject(URL, String.class))
                    .thenReturn(respuesta(
                            "1530",
                            "\"" + fecha + "\""
                    ));

            comprobarRechazo();
        }
    }

    @Test
    void respetaLaAntiguedadMaximaConfigurada() {
        Instant fecha = AHORA.minus(Duration.ofDays(2));

        when(restTemplate.getForObject(URL, String.class))
                .thenReturn(respuesta(
                        "1530",
                        "\"" + fecha + "\""
                ));

        CotizacionDolarService servicioUnDia = new CotizacionDolarService(
                builder,
                new ObjectMapper(),
                Clock.fixed(AHORA, ZoneOffset.UTC),
                1
        );

        // Con máximo de siete días, dos días de antigüedad son válidos.
        assertDoesNotThrow(servicio::obtenerCotizacion);

        // Con máximo de un día, esa misma respuesta debe rechazarse.
        assertThrows(
                CotizacionNoDisponibleException.class,
                servicioUnDia::obtenerCotizacion
        );
    }

    private String respuesta(String venta, String fecha) {
        return "{\"moneda\":\"USD\","
                + "\"casa\":\"tarjeta\","
                + "\"venta\":" + venta + ","
                + "\"fechaActualizacion\":" + fecha + "}";
    }

    private void comprobarRechazo() {
        CotizacionNoDisponibleException error = assertThrows(
                CotizacionNoDisponibleException.class,
                servicio::obtenerCotizacion
        );

        assertNotNull(error.getCause());

        assertEquals(
                "No pudimos obtener una cotización válida. "
                        + "Intentá nuevamente en unos momentos.",
                error.getMessage()
        );
    }
}
