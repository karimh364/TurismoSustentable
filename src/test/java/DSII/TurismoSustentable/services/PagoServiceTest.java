package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.SaldoSimuladoDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    private static final String EMAIL = "qa.saldo@example.com";

    private static final LocalDateTime FECHA =
            LocalDateTime.of(2026, 11, 15, 12, 0);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LiquidacionAlquilerService liquidacionAlquilerService;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReembolsoRepository reembolsoRepository;
    @Mock private PasarelaSimulada pasarelaSimulada;

    @InjectMocks private PagoService pagoService;

    @BeforeEach
    void iniciarSesion() {
        autenticar("CLIENTE");
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saldoSinMovimientosConservaInicial() {
        Usuario usuario = prepararUsuario(
                "qa.listados", "CLIENTE", "ADMIN"
        );

        prepararMovimientos(
                usuario, "1000000.00", List.of(), List.of()
        );

        SaldoSimuladoDTO saldo = pagoService.consultarSaldoSimulado();

        assertEquals(1, saldo.getUsuarioId());
        assertEquals("qa.listados", saldo.getNickname());
        assertEquals("ARS", saldo.getMoneda());
        assertEquals("SIMULADA", saldo.getPlataforma());
        assertEquals("PRUEBA", saldo.getEntorno());
        assertEquals(
                new BigDecimal("1000000.00"),
                saldo.getSaldoDisponibleArs()
        );
        assertEquals(
                new BigDecimal("0.00"),
                saldo.getPagosAprobadosArs()
        );

        verify(pagoRepository)
                .findByAlquilerUsuarioIdAndPlataformaAndEntornoOrderByIdAsc(
                        1, "SIMULADA", "PRUEBA"
                );
    }

    @Test
    void saldoInicialCeroEsValido() {
        Usuario usuario = prepararUsuario("qa.otro", "CLIENTE");

        prepararMovimientos(
                usuario, "0.00", List.of(), List.of()
        );

        SaldoSimuladoDTO saldo = pagoService.consultarSaldoSimulado();

        assertEquals(
                new BigDecimal("0.00"),
                saldo.getSaldoInicialArs()
        );
        assertEquals(
                new BigDecimal("0.00"),
                saldo.getSaldoDisponibleArs()
        );
    }

    @Test
    void consideraSoloMovimientosConfirmados() {
        Usuario usuario = prepararUsuario("qa.listados", "CLIENTE");

        Pago aprobado = crearPago(usuario, 11, "APROBADO", "100.00");
        aprobado.getAlquiler().setBorradoAlquiler(true);

        List<Pago> pagos = List.of(
                aprobado,
                crearPago(usuario, 12, "RECHAZADO", "30.00"),
                crearPago(usuario, 13, "PENDIENTE", "20.00"),
                crearPago(usuario, 14, "CANCELADO", "10.00"),
                crearPago(usuario, 15, "CREADO", "5.00")
        );

        List<Reembolso> reembolsos = List.of(
                crearReembolso(aprobado, 21, "CONFIRMADO", "25.00"),
                crearReembolso(aprobado, 22, "PENDIENTE", "10.00"),
                crearReembolso(aprobado, 23, "EN_PROCESO", "5.00"),
                crearReembolso(aprobado, 24, "FALLIDO", "10.00")
        );

        prepararMovimientos(
                usuario, "1000000.00", pagos, reembolsos
        );

        SaldoSimuladoDTO saldo = pagoService.consultarSaldoSimulado();

        assertEquals(
                new BigDecimal("100.00"),
                saldo.getPagosAprobadosArs()
        );
        assertEquals(
                new BigDecimal("25.00"),
                saldo.getReembolsosConfirmadosArs()
        );
        assertEquals(
                new BigDecimal("999925.00"),
                saldo.getSaldoDisponibleArs()
        );
    }

    @Test
    void sinSesionNoConsultaDatos() {
        SecurityContextHolder.clearContext();

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.consultarSaldoSimulado()
        );

        verifyNoInteractions(
                usuarioRepository,
                pagoRepository,
                reembolsoRepository,
                pasarelaSimulada
        );
    }

    @Test
    void exigeClienteEnSesionYEnBase() {
        prepararUsuario("qa.listados", "ADMIN");

        // La sesión dice CLIENTE, pero ese rol ya no está en la BD.
        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.consultarSaldoSimulado()
        );

        // ADMIN solo tampoco puede acceder.
        autenticar("ADMIN");

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.consultarSaldoSimulado()
        );

        verify(usuarioRepository, times(1))
                .buscarPorEmailConDatosYRoles(EMAIL);

        verifyNoInteractions(
                pagoRepository,
                reembolsoRepository,
                pasarelaSimulada
        );
    }

    @Test
    void rechazaUsuarioInactivo() {
        Usuario usuario = prepararUsuario("qa.listados", "CLIENTE");
        usuario.setActivo(false);

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.consultarSaldoSimulado()
        );

        verifyNoInteractions(
                pagoRepository,
                reembolsoRepository,
                pasarelaSimulada
        );
    }

    @Test
    void rechazaReembolsoQueSuperaPagoOriginal() {
        Usuario usuario = prepararUsuario("qa.listados", "CLIENTE");
        Pago pago = crearPago(usuario, 11, "APROBADO", "100.00");

        prepararMovimientos(
                usuario,
                "1000000.00",
                List.of(pago),
                List.of(
                        crearReembolso(pago, 21, "CONFIRMADO", "60.00"),
                        crearReembolso(pago, 22, "CONFIRMADO", "50.00")
                )
        );

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pagoService.consultarSaldoSimulado()
        );
    }

    @Test
    void rechazaSaldoNegativoPorConfiguracionIncompatible() {
        Usuario usuario = prepararUsuario("qa.listados", "CLIENTE");
        Pago pago = crearPago(usuario, 11, "APROBADO", "100.00");

        prepararMovimientos(
                usuario, "10.00", List.of(pago), List.of()
        );

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pagoService.consultarSaldoSimulado()
        );
    }

    @Test
    void rechazaPagoAprobadoSinFechaDeConfirmacion() {
        Usuario usuario = prepararUsuario("qa.listados", "CLIENTE");
        Pago pago = crearPago(usuario, 11, "APROBADO", "100.00");
        pago.setFechaConfirmacion(null);

        prepararMovimientos(
                usuario, "1000000.00", List.of(pago), List.of()
        );

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pagoService.consultarSaldoSimulado()
        );

        pago.setFechaConfirmacion(FECHA.minusSeconds(1));

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pagoService.consultarSaldoSimulado()
        );
    }

    private void autenticar(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL,
                        "no-utilizada",
                        AuthorityUtils.createAuthorityList(roles)
                )
        );
    }

    private Usuario prepararUsuario(
            String nickname,
            String... roles
    ) {
        Usuario usuario = new Usuario();
        usuario.setId(1);
        usuario.setNickname(nickname);
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);

        int idRol = 1;

        for (String codigo : roles) {
            Rol rol = new Rol(codigo);
            ReflectionTestUtils.setField(rol, "id", idRol++);

            usuario.getUsuarioRoles().add(
                    new UsuarioRol(usuario, rol)
            );
        }

        when(usuarioRepository.buscarPorEmailConDatosYRoles(EMAIL))
                .thenReturn(Optional.of(usuario));

        return usuario;
    }

    private void prepararMovimientos(
            Usuario usuario,
            String inicial,
            List<Pago> pagos,
            List<Reembolso> reembolsos
    ) {
        when(pasarelaSimulada.obtenerSaldoInicial(usuario.getNickname()))
                .thenReturn(new BigDecimal(inicial));

        when(pagoRepository
                .findByAlquilerUsuarioIdAndPlataformaAndEntornoOrderByIdAsc(
                        usuario.getId(), "SIMULADA", "PRUEBA"
                ))
                .thenReturn(pagos);

        when(reembolsoRepository
                .findByPagoAlquilerUsuarioIdAndPagoPlataformaAndPagoEntornoOrderByIdAsc(
                        usuario.getId(), "SIMULADA", "PRUEBA"
                ))
                .thenReturn(reembolsos);
    }

    private Pago crearPago(
            Usuario usuario,
            int id,
            String estado,
            String importe
    ) {
        Alquiler alquiler = new Alquiler();
        alquiler.setId(id + 100);
        alquiler.setUsuario(usuario);

        Pago pago = new Pago(
                alquiler,
                new EstadoPago("CREADO"),
                "ANTICIPO",
                new BigDecimal(importe),
                FECHA,
                "SIMULADA",
                "PRUEBA",
                "pago-qa-" + id
        );

        ReflectionTestUtils.setField(pago, "id", id);
        pago.setEstadoPago(new EstadoPago(estado));

        if ("APROBADO".equals(estado)) {
            pago.setFechaConfirmacion(FECHA);
        }

        return pago;
    }

    private Reembolso crearReembolso(
            Pago pago,
            int id,
            String estado,
            String importe
    ) {
        Reembolso reembolso = new Reembolso(
                pago,
                new BigDecimal(importe),
                FECHA,
                "Prueba unitaria",
                "reembolso-qa-" + id
        );

        ReflectionTestUtils.setField(reembolso, "id", id);
        reembolso.setEstado(estado);

        if ("CONFIRMADO".equals(estado)) {
            reembolso.setFechaConfirmacion(FECHA);
        }

        return reembolso;
    }
}
