package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.ResultadoAnticipoDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InicioAnticipoServiceTest {

    private static final String EMAIL = "qa.anticipo@example.com";

    private static final String CLAVE =
            "123e4567-e89b-42d3-a456-426614174000";

    private static final String CLAVE_DB = "ANTICIPO:1:" + CLAVE;

    private static final String CLAVE_ANTERIOR =
            "ANTICIPO:1:123e4567-e89b-42d3-a456-426614174001";

    private static final LocalDateTime CREACION =
            LocalDateTime.of(2026, 11, 15, 12, 0);

    private static final LocalDateTime AHORA =
            CREACION.plusMinutes(5);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LiquidacionAlquilerService liquidacionAlquilerService;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReembolsoRepository reembolsoRepository;
    @Mock private PasarelaSimulada pasarelaSimulada;
    @Mock private AlquilerRepository alquilerRepository;
    @Mock private AlquilerDetalleRepository alquilerDetalleRepository;
    @Mock private EstadoPagoRepository estadoPagoRepository;
    @Mock private HistorialEstadoPagoRepository historialRepository;

    @Mock
    private EstadoAlquilerDetalleRepository estadoAlquilerDetalleRepository;

    @Mock
    private HistorialEstadoDetalleRepository historialEstadoDetalleRepository;

    @Mock
    private HistorialEstadoReembolsoRepository historialEstadoReembolsoRepository;

    private PagoService pagoService;

    @BeforeEach
    void preparar() {
        pagoService = crearServicio(AHORA);
        autenticar("CLIENTE");
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creaIntentoConHistorialYRepetirNoDuplica() {
        Alquiler alquiler = prepararAlquiler();
        AlquilerDetalle primero = detalle(alquiler, "RESERVADO", "100.00");
        AlquilerDetalle segundo = detalle(alquiler, "RESERVADO", "200.00");

        prepararDetalles(primero, segundo);
        habilitarGuardado();

        ResultadoAnticipoDTO resultado = pagoService.iniciarAnticipo(
                7,
                " " + CLAVE.toUpperCase(Locale.ROOT) + " "
        );

        ArgumentCaptor<Pago> pagoCaptor =
                ArgumentCaptor.forClass(Pago.class);

        ArgumentCaptor<HistorialEstadoPago> historialCaptor =
                ArgumentCaptor.forClass(HistorialEstadoPago.class);

        verify(pagoRepository).saveAndFlush(pagoCaptor.capture());
        verify(historialRepository).save(historialCaptor.capture());

        Pago pago = pagoCaptor.getValue();
        HistorialEstadoPago historial = historialCaptor.getValue();

        assertSame(alquiler, pago.getAlquiler());
        assertEquals("ANTICIPO", pago.getFinalidad());
        assertEquals(new BigDecimal("300.00"), pago.getMontoArs());
        assertEquals("ARS", pago.getMoneda());
        assertEquals("CREADO", pago.getEstadoPago().getCodigo());
        assertEquals("SIMULADA", pago.getPlataforma());
        assertEquals("PRUEBA", pago.getEntorno());
        assertEquals(CLAVE_DB, pago.getClaveIdempotencia());
        assertEquals(AHORA, pago.getFechaCreacion());
        assertNull(pago.getFechaConfirmacion());
        assertNull(pago.getReferenciaExterna());

        assertSame(pago, historial.getPago());
        assertNull(historial.getEstadoAnterior());
        assertEquals("CREADO", historial.getEstadoNuevo().getCodigo());
        assertSame(alquiler.getUsuario(), historial.getActorUsuario());
        assertEquals("USUARIO", historial.getOrigen());
        assertEquals("SOLICITUD_ANTICIPO", historial.getFuente());
        assertEquals(AHORA, historial.getFechaHoraCambio());
        assertNull(historial.getFechaHoraEventoExterno());
        assertNull(historial.getReferenciaEventoExterno());

        assertEquals(Integer.valueOf(11), resultado.getPagoId());
        assertEquals("CREADO", resultado.getEstadoPago());
        assertEquals(CLAVE, resultado.getClaveSolicitud());
        assertFalse(resultado.isSinImporteACobrar());

        assertEquals(new BigDecimal("300.00"), alquiler.getCostoEstimadoArs());
        assertEquals(new BigDecimal("1530.00"), alquiler.getCotizacionArsPorUsd());
        assertEquals(CREACION.plusMinutes(15),
                alquiler.getFechaHoraVencimientoReserva());
        assertEquals("RESERVADO", alquiler.getEstadoContratacion().getCodigo());
        assertEquals("RESERVADO", primero.getEstado().getCodigo());
        assertEquals("RESERVADO", segundo.getEstado().getCodigo());
        assertNull(primero.getFechaHoraRetiro());
        assertNull(segundo.getFechaHoraRetiro());

        InOrder orden = inOrder(
                usuarioRepository,
                alquilerRepository,
                pagoRepository,
                historialRepository
        );

        orden.verify(usuarioRepository)
                .buscarActivoPorEmailConBloqueo(EMAIL);
        orden.verify(alquilerRepository).buscarPorIdConBloqueo(7);
        orden.verify(pagoRepository).saveAndFlush(any(Pago.class));
        orden.verify(historialRepository)
                .save(any(HistorialEstadoPago.class));

        when(pagoRepository.findByClaveIdempotencia(CLAVE_DB))
                .thenReturn(Optional.of(pago));

        ResultadoAnticipoDTO repetido =
                pagoService.iniciarAnticipo(7, CLAVE);

        assertEquals(resultado.getPagoId(), repetido.getPagoId());
        assertEquals(resultado.getMontoArs(), repetido.getMontoArs());

        verify(pagoRepository, times(1)).saveAndFlush(any(Pago.class));
        verify(historialRepository, times(1))
                .save(any(HistorialEstadoPago.class));
        verify(historialRepository, times(1)).flush();
        verifyNoInteractions(pasarelaSimulada, reembolsoRepository);
    }

    @Test
    void calculaSoloUnidadesReservadasSinReescribirEstimacion() {
        Alquiler alquiler = prepararAlquiler();
        alquiler.setCostoEstimadoArs(new BigDecimal("600.00"));

        prepararDetalles(
                detalle(alquiler, "RESERVADO", "100.00"),
                detalle(alquiler, "CANCELADO", "200.00"),
                detalle(alquiler, "VENCIDO", "300.00")
        );
        habilitarGuardado();

        ResultadoAnticipoDTO resultado =
                pagoService.iniciarAnticipo(7, CLAVE);

        assertEquals(new BigDecimal("100.00"), resultado.getMontoArs());
        assertEquals(new BigDecimal("600.00"), alquiler.getCostoEstimadoArs());
        assertEquals(CREACION.plusMinutes(15),
                alquiler.getFechaHoraVencimientoReserva());
    }

    @Test
    void importeCeroNoCreaPagoNiHistorial() {
        Alquiler alquiler = prepararAlquiler();
        alquiler.setCostoEstimadoArs(new BigDecimal("0.00"));

        prepararDetalles(detalle(alquiler, "RESERVADO", "0.00"));

        ResultadoAnticipoDTO resultado =
                pagoService.iniciarAnticipo(7, CLAVE);

        assertTrue(resultado.isSinImporteACobrar());
        assertEquals(new BigDecimal("0.00"), resultado.getMontoArs());
        assertNull(resultado.getPagoId());
        assertNull(resultado.getEstadoPago());
        assertNull(resultado.getFechaCreacionPago());
        assertNull(resultado.getFechaConfirmacionPago());

        verificarSinEscrituras();
        verifyNoInteractions(estadoPagoRepository);
    }

    @Test
    void devuelveIntentoOriginalTrasCierreYVencimientoSinReactivarlo() {
        Alquiler alquiler = prepararAlquiler();
        alquiler.setEstadoContratacion(new EstadoAlquiler("CANCELADO"));
        alquiler.setBorradoAlquiler(true);

        pagoService = crearServicio(CREACION.plusMinutes(16));

        Pago original = pagoExistente(
                alquiler, "APROBADO", "300.00", CLAVE_DB
        );

        when(pagoRepository.findByClaveIdempotencia(CLAVE_DB))
                .thenReturn(Optional.of(original));

        ResultadoAnticipoDTO resultado =
                pagoService.iniciarAnticipo(7, CLAVE);

        assertEquals(original.getId(), resultado.getPagoId());
        assertEquals(new BigDecimal("300.00"), resultado.getMontoArs());
        assertEquals("APROBADO", resultado.getEstadoPago());
        assertEquals("CANCELADO",
                alquiler.getEstadoContratacion().getCodigo());
        assertTrue(alquiler.isBorradoAlquiler());

        verificarSinEscrituras();
        verifyNoInteractions(alquilerDetalleRepository, estadoPagoRepository);
    }

    @Test
    void mismaClaveNoPuedeUsarseParaOtroAlquiler() {
        Alquiler alquiler = prepararAlquiler();

        Alquiler otro = new Alquiler();
        otro.setId(8);
        otro.setUsuario(alquiler.getUsuario());

        Pago existente = pagoExistente(
                otro, "CREADO", "300.00", CLAVE_DB
        );

        when(pagoRepository.findByClaveIdempotencia(CLAVE_DB))
                .thenReturn(Optional.of(existente));

        comprobarConflicto();
        verifyNoInteractions(alquilerDetalleRepository, estadoPagoRepository);
    }

    @Test
    void anticipoCreadoPendienteOAprobadoImpideOtroIntento() {
        Alquiler alquiler = prepararAlquiler();

        for (String estado : List.of("CREADO", "PENDIENTE", "APROBADO")) {
            Pago existente = pagoExistente(
                    alquiler, estado, "300.00", CLAVE_ANTERIOR
            );

            when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                    .thenReturn(List.of(existente));

            comprobarConflicto();
        }

        verifyNoInteractions(alquilerDetalleRepository, estadoPagoRepository);
    }

    @Test
    void rechazoPermiteNuevoIntentoDentroDelPlazoOriginal() {
        comprobarReintento("RECHAZADO");
    }

    @Test
    void cancelacionDelPagoPermiteNuevoIntentoDentroDelPlazoOriginal() {
        comprobarReintento("CANCELADO");
    }

    @Test
    void clienteNoPuedeIniciarAnticipoDeAlquilerAjeno() {
        Alquiler alquiler = prepararAlquiler();
        alquiler.setUsuario(usuario(2, "CLIENTE"));

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.iniciarAnticipo(7, CLAVE)
        );

        verifyNoInteractions(
                pagoRepository,
                alquilerDetalleRepository,
                estadoPagoRepository
        );
        verificarSinEscrituras();
    }

    @Test
    void sinSesionOConAdminSoloNoConsultaDatos() {
        SecurityContextHolder.clearContext();

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.iniciarAnticipo(7, CLAVE)
        );

        autenticar("ADMIN");

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.iniciarAnticipo(7, CLAVE)
        );

        verifyNoInteractions(
                usuarioRepository,
                alquilerRepository,
                pagoRepository,
                alquilerDetalleRepository,
                estadoPagoRepository,
                historialRepository,
                pasarelaSimulada,
                reembolsoRepository
        );
    }

    @Test
    void exigeCuentaActivaYClienteVigenteEnBase() {
        Usuario sinCliente = usuario(1, "ADMIN");

        Usuario inactivo = usuario(1, "CLIENTE");
        inactivo.setActivo(false);

        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL))
                .thenReturn(Optional.of(sinCliente))
                .thenReturn(Optional.of(inactivo));

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.iniciarAnticipo(7, CLAVE)
        );

        assertThrows(
                AccessDeniedException.class,
                () -> pagoService.iniciarAnticipo(7, CLAVE)
        );

        verifyNoInteractions(
                alquilerRepository,
                pagoRepository,
                alquilerDetalleRepository,
                estadoPagoRepository
        );
        verificarSinEscrituras();
    }

    @Test
    void enElInstanteDelVencimientoNoPermiteNuevoIntento() {
        Alquiler alquiler = prepararAlquiler();
        prepararDetalles(detalle(alquiler, "RESERVADO", "100.00"));

        pagoService = crearServicio(CREACION.plusMinutes(15));

        comprobarConflicto();

        assertEquals(CREACION.plusMinutes(15),
                alquiler.getFechaHoraVencimientoReserva());
        verifyNoInteractions(estadoPagoRepository);
    }

    @Test
    void rechazaIdOClaveInvalidosSinConsultarDatos() {
        for (Integer id : new Integer[]{null, 0, -1}) {
            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> pagoService.iniciarAnticipo(id, CLAVE)
            );
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        }

        for (String clave : new String[]{
                null, "", "no-es-uuid", "0-0-0-0-0"
        }) {
            ResponseStatusException error = assertThrows(
                    ResponseStatusException.class,
                    () -> pagoService.iniciarAnticipo(7, clave)
            );
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        }

        verifyNoInteractions(
                usuarioRepository,
                alquilerRepository,
                pagoRepository,
                alquilerDetalleRepository,
                estadoPagoRepository,
                historialRepository,
                pasarelaSimulada,
                reembolsoRepository
        );
    }

    @Test
    void rechazaAlquilerAnteriorCanceladoUOculto() {
        Alquiler alquiler = prepararAlquiler();

        alquiler.setEstadoContratacion(null);
        comprobarConflicto();

        alquiler.setEstadoContratacion(new EstadoAlquiler("CANCELADO"));
        comprobarConflicto();

        alquiler.setEstadoContratacion(new EstadoAlquiler("RESERVADO"));
        alquiler.setBorradoAlquiler(true);
        comprobarConflicto();

        verifyNoInteractions(alquilerDetalleRepository, estadoPagoRepository);
    }

    @Test
    void sinUnidadesReservadasNoCreaIntento() {
        Alquiler alquiler = prepararAlquiler();

        prepararDetalles(
                detalle(alquiler, "CANCELADO", "100.00"),
                detalle(alquiler, "VENCIDO", "200.00")
        );

        comprobarConflicto();
        verifyNoInteractions(estadoPagoRepository);
    }

    @Test
    void catalogoFaltanteEsFalloInternoYNoGuardaPago() {
        Alquiler alquiler = prepararAlquiler();
        prepararDetalles(detalle(alquiler, "RESERVADO", "100.00"));

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> pagoService.iniciarAnticipo(7, CLAVE)
        );

        verify(estadoPagoRepository).findByCodigo("CREADO");
        verificarSinEscrituras();
    }

    private PagoService crearServicio(LocalDateTime ahora) {
        Clock reloj = Clock.fixed(
                ahora.toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC
        );

        return new PagoService(
                usuarioRepository,
                pagoRepository,
                reembolsoRepository,
                pasarelaSimulada,
                alquilerRepository,
                alquilerDetalleRepository,
                estadoPagoRepository,
                historialRepository,
                estadoAlquilerDetalleRepository,
                historialEstadoDetalleRepository,
                historialEstadoReembolsoRepository,
                liquidacionAlquilerService,
                reloj
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

    private Usuario usuario(int id, String... roles) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNickname("qa.anticipo" + id);
        usuario.setEmail(id == 1 ? EMAIL : "qa.otro@example.com");
        usuario.setActivo(true);

        int rolId = 1;

        for (String codigo : roles) {
            Rol rol = new Rol(codigo);
            ReflectionTestUtils.setField(rol, "id", rolId++);

            usuario.getUsuarioRoles().add(
                    new UsuarioRol(usuario, rol)
            );
        }

        return usuario;
    }

    private Alquiler prepararAlquiler() {
        Usuario usuario = usuario(1, "CLIENTE");

        Alquiler alquiler = new Alquiler();
        alquiler.setId(7);
        alquiler.setUsuario(usuario);
        alquiler.setEstadoContratacion(new EstadoAlquiler("RESERVADO"));
        alquiler.setFechaHoraCreacion(CREACION);
        alquiler.setFechaHoraVencimientoReserva(CREACION.plusMinutes(15));
        alquiler.setCostoEstimadoArs(new BigDecimal("300.00"));
        alquiler.setCotizacionArsPorUsd(new BigDecimal("1530.00"));

        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL))
                .thenReturn(Optional.of(usuario));

        when(alquilerRepository.buscarPorIdConBloqueo(7))
                .thenReturn(Optional.of(alquiler));

        return alquiler;
    }

    private AlquilerDetalle detalle(
            Alquiler alquiler,
            String estado,
            String importe
    ) {
        AlquilerDetalle detalle = new AlquilerDetalle();
        detalle.setAlquiler(alquiler);
        detalle.setEstado(new EstadoAlquilerDetalle(estado));
        detalle.setSubtotalEstimadoArs(new BigDecimal(importe));
        return detalle;
    }

    private void prepararDetalles(AlquilerDetalle... detalles) {
        when(alquilerDetalleRepository.buscarPorAlquilerConDatos(7))
                .thenReturn(List.of(detalles));
    }

    private void habilitarGuardado() {
        EstadoPago creado = new EstadoPago("CREADO");
        ReflectionTestUtils.setField(creado, "id", 1);

        when(estadoPagoRepository.findByCodigo("CREADO"))
                .thenReturn(Optional.of(creado));

        when(pagoRepository.saveAndFlush(any(Pago.class)))
                .thenAnswer(invocacion -> {
                    Pago pago = invocacion.getArgument(0);
                    ReflectionTestUtils.setField(pago, "id", 11);
                    return pago;
                });
    }

    private Pago pagoExistente(
            Alquiler alquiler,
            String estado,
            String importe,
            String clave
    ) {
        Pago pago = new Pago(
                alquiler,
                new EstadoPago("CREADO"),
                "ANTICIPO",
                new BigDecimal(importe),
                CREACION.plusMinutes(1),
                "SIMULADA",
                "PRUEBA",
                clave
        );

        ReflectionTestUtils.setField(pago, "id", 11);
        pago.setEstadoPago(new EstadoPago(estado));

        if ("APROBADO".equals(estado)) {
            pago.setFechaConfirmacion(CREACION.plusMinutes(2));
        }

        return pago;
    }

    private void comprobarReintento(String estadoAnterior) {
        Alquiler alquiler = prepararAlquiler();

        Pago anterior = pagoExistente(
                alquiler, estadoAnterior, "300.00", CLAVE_ANTERIOR
        );

        when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(anterior));

        prepararDetalles(
                detalle(alquiler, "RESERVADO", "100.00"),
                detalle(alquiler, "CANCELADO", "200.00")
        );
        habilitarGuardado();

        ResultadoAnticipoDTO resultado =
                pagoService.iniciarAnticipo(7, CLAVE);

        assertEquals("CREADO", resultado.getEstadoPago());
        assertEquals(new BigDecimal("100.00"), resultado.getMontoArs());
        assertEquals(estadoAnterior, anterior.getEstadoPago().getCodigo());
        assertEquals(new BigDecimal("300.00"), anterior.getMontoArs());
        assertEquals(new BigDecimal("300.00"), alquiler.getCostoEstimadoArs());
        assertEquals(CREACION.plusMinutes(15),
                alquiler.getFechaHoraVencimientoReserva());
    }

    private void comprobarConflicto() {
        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> pagoService.iniciarAnticipo(7, CLAVE)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        verificarSinEscrituras();
    }

    private void verificarSinEscrituras() {
        verify(pagoRepository, never()).saveAndFlush(any(Pago.class));
        verifyNoInteractions(
                historialRepository,
                pasarelaSimulada,
                reembolsoRepository
        );
    }
}
