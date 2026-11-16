package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.EstadoPago;
import DSII.TurismoSustentable.models.Pago;
import DSII.TurismoSustentable.models.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasarelaSimuladaTest {

    private static final Instant INICIO =
            Instant.parse("2026-11-15T12:00:00Z");

    private static final Clock RELOJ =
            Clock.fixed(INICIO, ZoneOffset.UTC);

    @Test
    void permiteSaldosInicialesDistintosYCero() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty(
                        "pagos.simulados.cuentas.qa.otro.saldo-inicial-ars",
                        "0.00"
                )
                .withProperty(
                        "pagos.simulados.cuentas.qa.poco.saldo-inicial-ars",
                        "1000.00"
                );

        PasarelaSimulada pasarela =
                new PasarelaSimulada(entorno, RELOJ);

        assertEquals(
                new BigDecimal("1000000.00"),
                pasarela.obtenerSaldoInicial("qa.listados")
        );
        assertEquals(
                new BigDecimal("0.00"),
                pasarela.obtenerSaldoInicial(" QA.OTRO ")
        );
        assertEquals(
                new BigDecimal("1000.00"),
                pasarela.obtenerSaldoInicial("qa.poco")
        );
    }

    @Test
    void apruebaConSaldoSuficienteSinModificarElPago() {
        PasarelaSimulada pasarela =
                new PasarelaSimulada(new MockEnvironment(), RELOJ);

        Pago pago = crearPago("qa.listados", "27540.00");

        assertEquals(
                "APROBADO",
                pasarela.evaluarPago(
                        pago,
                        new BigDecimal("27540.00")
                )
        );

        assertEquals("CREADO", pago.getEstadoPago().getCodigo());
        assertNull(pago.getFechaConfirmacion());
    }

    @Test
    void rechazaSaldoCeroOInsuficiente() {
        PasarelaSimulada pasarela =
                new PasarelaSimulada(new MockEnvironment(), RELOJ);

        assertEquals(
                "RECHAZADO",
                pasarela.evaluarPago(
                        crearPago("qa.otro", "27540.00"),
                        new BigDecimal("0.00")
                )
        );
        assertEquals(
                "RECHAZADO",
                pasarela.evaluarPago(
                        crearPago("qa.poco", "27540.00"),
                        new BigDecimal("1000.00")
                )
        );
    }

    @Test
    void simulaRechazoYCancelacionConfigurados() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty(
                        "pagos.simulados.cuentas.qa.rechazado.resultado",
                        "RECHAZADO"
                )
                .withProperty(
                        "pagos.simulados.cuentas.qa.cancelado.resultado",
                        "CANCELADO"
                );

        PasarelaSimulada pasarela =
                new PasarelaSimulada(entorno, RELOJ);

        assertEquals(
                "RECHAZADO",
                pasarela.evaluarPago(
                        crearPago("qa.rechazado", "100.00"),
                        new BigDecimal("1000000.00")
                )
        );
        assertEquals(
                "CANCELADO",
                pasarela.evaluarPago(
                        crearPago("qa.cancelado", "100.00"),
                        new BigDecimal("1000000.00")
                )
        );
    }

    @Test
    void mantienePendienteHastaLaDemoraYResuelveAlConsultar() {
        MockEnvironment entorno = entornoPendiente();
        Pago pago = crearPago("qa.pendiente", "100.00");

        PasarelaSimulada inicial =
                new PasarelaSimulada(entorno, RELOJ);

        assertEquals(
                "PENDIENTE",
                inicial.evaluarPago(pago, new BigDecimal("1000.00"))
        );

        // Representa el estado que guardará el servicio financiero.
        pago.setEstadoPago(new EstadoPago("PENDIENTE"));

        PasarelaSimulada antesDelPlazo = new PasarelaSimulada(
                entorno,
                Clock.offset(RELOJ, Duration.ofSeconds(19))
        );
        assertEquals(
                "PENDIENTE",
                antesDelPlazo.evaluarPago(
                        pago,
                        new BigDecimal("1000.00")
                )
        );

        PasarelaSimulada despuesDelPlazo = new PasarelaSimulada(
                entorno,
                Clock.offset(RELOJ, Duration.ofSeconds(20))
        );
        assertEquals(
                "APROBADO",
                despuesDelPlazo.evaluarPago(
                        pago,
                        new BigDecimal("1000.00")
                )
        );
    }

    @Test
    void puedeRechazarAlResolverUnPendienteSinFondos() {
        PasarelaSimulada pasarela = new PasarelaSimulada(
                entornoPendiente(),
                Clock.offset(RELOJ, Duration.ofSeconds(20))
        );

        Pago pago = crearPago("qa.pendiente", "100.00");
        pago.setEstadoPago(new EstadoPago("PENDIENTE"));

        assertEquals(
                "RECHAZADO",
                pasarela.evaluarPago(pago, BigDecimal.ZERO)
        );
    }

    @Test
    void noReevaluaPagosYaResueltos() {
        MockEnvironment entorno = new MockEnvironment()
                .withProperty(
                        "pagos.simulados.cuentas.qa.listados.resultado",
                        "CANCELADO"
                );

        PasarelaSimulada pasarela =
                new PasarelaSimulada(entorno, RELOJ);

        for (String estado : List.of(
                "APROBADO", "RECHAZADO", "CANCELADO"
        )) {
            Pago pago = crearPago("qa.listados", "100.00");
            pago.setEstadoPago(new EstadoPago(estado));

            if ("APROBADO".equals(estado)) {
                pago.setFechaConfirmacion(
                        LocalDateTime.ofInstant(INICIO, ZoneOffset.UTC)
                );
            }

            assertEquals(
                    estado,
                    pasarela.evaluarPago(pago, BigDecimal.ZERO)
            );
        }
    }

    @Test
    void rechazaConfiguracionInvalidaComoFalloInterno() {
        for (String valor : List.of("-1", "abc", "1.001")) {
            MockEnvironment entorno = new MockEnvironment()
                    .withProperty(
                            "pagos.simulados.saldo-inicial-ars",
                            valor
                    );

            assertThrows(
                    ConfiguracionSistemaException.class,
                    () -> new PasarelaSimulada(entorno, RELOJ)
            );
        }

        MockEnvironment modoInvalido = new MockEnvironment()
                .withProperty(
                        "pagos.simulados.cuentas.qa.listados.resultado",
                        "APROBADO"
                );

        PasarelaSimulada pasarela =
                new PasarelaSimulada(modoInvalido, RELOJ);

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pasarela.evaluarPago(
                        crearPago("qa.listados", "100.00"),
                        new BigDecimal("1000.00")
                )
        );

        MockEnvironment demoraInvalida = entornoPendiente()
                .withProperty(
                        "pagos.simulados.cuentas.qa.pendiente.demora-segundos",
                        "0"
                );

        PasarelaSimulada conDemoraInvalida =
                new PasarelaSimulada(demoraInvalida, RELOJ);

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> conDemoraInvalida.evaluarPago(
                        crearPago("qa.pendiente", "100.00"),
                        new BigDecimal("1000.00")
                )
        );
    }

    @Test
    void rechazaDatosInvalidosYOtraPlataforma() {
        PasarelaSimulada pasarela =
                new PasarelaSimulada(new MockEnvironment(), RELOJ);

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pasarela.evaluarPago(
                        crearPago("qa.listados", "0.00"),
                        new BigDecimal("1000.00")
                )
        );

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pasarela.evaluarPago(
                        crearPago("qa.listados", "100.00"),
                        new BigDecimal("-1.00")
                )
        );

        Pago base = crearPago("qa.listados", "100.00");

        Pago deOtraPlataforma = new Pago(
                base.getAlquiler(),
                new EstadoPago("CREADO"),
                "ANTICIPO",
                base.getMontoArs(),
                base.getFechaCreacion(),
                "MERCADOPAGO",
                "PRODUCCION",
                "qa-no-procesar-como-simulado"
        );

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pasarela.evaluarPago(
                        deOtraPlataforma,
                        new BigDecimal("1000.00")
                )
        );
    }

    @Test
    void seLimitaAlPerfilRefactor() {
        assertArrayEquals(
                new String[]{"refactor"},
                PasarelaSimulada.class.getAnnotation(Profile.class).value()
        );
    }

    private MockEnvironment entornoPendiente() {
        return new MockEnvironment()
                .withProperty(
                        "pagos.simulados.cuentas.qa.pendiente.resultado",
                        "PENDIENTE"
                )
                .withProperty(
                        "pagos.simulados.cuentas.qa.pendiente.demora-segundos",
                        "20"
                );
    }

    private Pago crearPago(String nickname, String importe) {
        Usuario usuario = new Usuario();
        usuario.setNickname(nickname);

        Alquiler alquiler = new Alquiler();
        alquiler.setUsuario(usuario);

        return new Pago(
                alquiler,
                new EstadoPago("CREADO"),
                "ANTICIPO",
                new BigDecimal(importe),
                LocalDateTime.ofInstant(INICIO, ZoneOffset.UTC),
                "SIMULADA",
                "PRUEBA",
                "qa-" + nickname
        );
    }
}
