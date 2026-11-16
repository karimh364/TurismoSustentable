package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.ResultadoAnticipoDTO;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolverAnticipoServiceTest {

    private static final String EMAIL = "qa.resolver@example.com";

    private static final String CLAVE =
            "123e4567-e89b-42d3-a456-426614174000";

    private static final String CLAVE_DB = "ANTICIPO:1:" + CLAVE;

    private static final LocalDateTime CREACION =
            LocalDateTime.of(2026, 11, 15, 12, 0);

    private static final LocalDateTime AHORA =
            CREACION.plusMinutes(5);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LiquidacionAlquilerService liquidacionAlquilerService;
    @Mock private ReembolsoService reembolsoService;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReembolsoRepository reembolsoRepository;
    @Mock private PasarelaSimulada pasarelaSimulada;
    @Mock private AlquilerRepository alquilerRepository;
    @Mock private AlquilerDetalleRepository alquilerDetalleRepository;
    @Mock private EstadoPagoRepository estadoPagoRepository;
    @Mock private HistorialEstadoPagoRepository historialPagoRepository;
    @Mock private EstadoAlquilerDetalleRepository estadoDetalleRepository;
    @Mock private HistorialEstadoDetalleRepository historialDetalleRepository;
    @Mock private HistorialEstadoReembolsoRepository historialReembolsoRepository;

    private PagoService servicio;
    private Usuario usuario;
    private Alquiler alquiler;
    private AlquilerDetalle primero;
    private AlquilerDetalle segundo;
    private List<AlquilerDetalle> detalles;

    @BeforeEach
    void preparar() {
        usuario = crearUsuario(1);

        alquiler = new Alquiler();
        alquiler.setId(7);
        alquiler.setUsuario(usuario);
        alquiler.setEstadoContratacion(new EstadoAlquiler("RESERVADO"));
        alquiler.setFechaHoraCreacion(CREACION);
        alquiler.setFechaHoraVencimientoReserva(CREACION.plusMinutes(15));
        alquiler.setCostoEstimadoArs(new BigDecimal("300.00"));
        alquiler.setCotizacionArsPorUsd(new BigDecimal("1530.00"));

        primero = crearDetalle(1, "100.00");
        segundo = crearDetalle(2, "200.00");
        detalles = List.of(primero, segundo);

        servicio = crearServicio(relojFijo(AHORA));
        autenticar("CLIENTE");
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void aprobacionHabilitaSinIniciarUsoYNoDuplicaElDescuento() {
        Pago pago = prepararIntento("CREADO");
        prepararProveedor(pago, "APROBADO", "1000.00");
        estadosPago("PENDIENTE", "APROBADO");
        habilitarCatalogoRetiro();

        ResultadoAnticipoDTO resultado = servicio.resolverAnticipo(7, CLAVE);

        assertEquals("APROBADO", resultado.getEstadoPago());
        assertEquals(AHORA, pago.getFechaConfirmacion());
        assertEquals("SIM-PAGO-11", pago.getReferenciaExterna());

        assertEquals("PENDIENTE_RETIRO", primero.getEstado().getCodigo());
        assertEquals("PENDIENTE_RETIRO", segundo.getEstado().getCodigo());
        assertNull(primero.getFechaHoraRetiro());
        assertNull(segundo.getFechaHoraRetiro());

        assertEquals("RESERVADO",
                alquiler.getEstadoContratacion().getCodigo());
        assertEquals(CREACION.plusMinutes(15),
                alquiler.getFechaHoraVencimientoReserva());
        assertEquals(new BigDecimal("300.00"), alquiler.getCostoEstimadoArs());
        assertEquals(new BigDecimal("1530.00"), alquiler.getCotizacionArsPorUsd());

        ArgumentCaptor<HistorialEstadoPago> pagos =
                ArgumentCaptor.forClass(HistorialEstadoPago.class);

        verify(historialPagoRepository, times(2)).save(pagos.capture());

        assertEquals("CREADO",
                pagos.getAllValues().get(0).getEstadoAnterior().getCodigo());
        assertEquals("PENDIENTE",
                pagos.getAllValues().get(0).getEstadoNuevo().getCodigo());
        assertEquals("PENDIENTE",
                pagos.getAllValues().get(1).getEstadoAnterior().getCodigo());
        assertEquals("APROBADO",
                pagos.getAllValues().get(1).getEstadoNuevo().getCodigo());

        for (HistorialEstadoPago evento : pagos.getAllValues()) {
            assertEquals("SISTEMA", evento.getOrigen());
            assertNull(evento.getActorUsuario());
            assertNull(evento.getFechaHoraEventoExterno());
        }

        ArgumentCaptor<HistorialEstadoDetalle> cambios =
                ArgumentCaptor.forClass(HistorialEstadoDetalle.class);

        verify(historialDetalleRepository, times(2)).save(cambios.capture());

        for (HistorialEstadoDetalle evento : cambios.getAllValues()) {
            assertEquals("RESERVADO", evento.getEstadoAnterior().getCodigo());
            assertEquals("PENDIENTE_RETIRO",
                    evento.getEstadoNuevo().getCodigo());
            assertEquals("SISTEMA", evento.getOrigen());
            assertNull(evento.getActorUsuario());
            assertEquals(AHORA, evento.getFechaHoraCambio());
        }

        ResultadoAnticipoDTO repetido = servicio.resolverAnticipo(7, CLAVE);

        assertEquals(resultado.getPagoId(), repetido.getPagoId());
        verify(pasarelaSimulada, times(1))
                .evaluarPago(pago, new BigDecimal("1000.00"));
        verify(historialPagoRepository, times(2))
                .save(any(HistorialEstadoPago.class));
        verify(historialDetalleRepository, times(2))
                .save(any(HistorialEstadoDetalle.class));

        when(usuarioRepository.buscarPorEmailConDatosYRoles(EMAIL))
                .thenReturn(Optional.of(usuario));

        assertEquals(
                new BigDecimal("700.00"),
                servicio.consultarSaldoSimulado().getSaldoDisponibleArs()
        );

        verificarSinReembolso();
        verify(pagoRepository, never()).saveAndFlush(any(Pago.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECHAZADO", "CANCELADO"})
    void resultadoNegativoNoCancelaLaReserva(String resultadoProveedor) {
        Pago pago = prepararIntento("CREADO");
        prepararProveedor(pago, resultadoProveedor, "1000.00");
        estadosPago("PENDIENTE", resultadoProveedor);

        ResultadoAnticipoDTO resultado = servicio.resolverAnticipo(7, CLAVE);

        assertEquals(resultadoProveedor, resultado.getEstadoPago());
        assertNull(pago.getFechaConfirmacion());
        assertEquals("RESERVADO",
                alquiler.getEstadoContratacion().getCodigo());
        assertEquals("RESERVADO", primero.getEstado().getCodigo());
        assertEquals("RESERVADO", segundo.getEstado().getCodigo());
        assertEquals(CREACION.plusMinutes(15),
                alquiler.getFechaHoraVencimientoReserva());

        verify(historialPagoRepository, times(2))
                .save(any(HistorialEstadoPago.class));
        verifyNoInteractions(estadoDetalleRepository, historialDetalleRepository);
        verificarSinReembolso();
    }

    @Test
    void pendienteRepetidoNoDuplicaHistoriaYLuegoPuedeAprobarse() {
        Pago pago = prepararIntento("CREADO");
        prepararDetalles();
        prepararSaldo("1000.00", List.of(pago));
        estadosPago("PENDIENTE", "APROBADO");
        habilitarCatalogoRetiro();

        when(pasarelaSimulada.evaluarPago(pago, new BigDecimal("1000.00")))
                .thenReturn("PENDIENTE")
                .thenReturn("PENDIENTE")
                .thenReturn("APROBADO");

        assertEquals("PENDIENTE",
                servicio.resolverAnticipo(7, CLAVE).getEstadoPago());

        servicio = crearServicio(relojFijo(AHORA.plusSeconds(10)));

        assertEquals("PENDIENTE",
                servicio.resolverAnticipo(7, CLAVE).getEstadoPago());

        verify(historialPagoRepository, times(1))
                .save(any(HistorialEstadoPago.class));
        verifyNoInteractions(historialDetalleRepository);
        assertNull(pago.getFechaConfirmacion());

        servicio = crearServicio(relojFijo(AHORA.plusSeconds(30)));

        assertEquals("APROBADO",
                servicio.resolverAnticipo(7, CLAVE).getEstadoPago());

        assertEquals(AHORA.plusSeconds(30), pago.getFechaConfirmacion());
        assertEquals("PENDIENTE_RETIRO", primero.getEstado().getCodigo());

        verify(historialPagoRepository, times(2))
                .save(any(HistorialEstadoPago.class));
        verificarSinReembolso();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "VENCIDA", "CANCELADA", "SELECCION_CAMBIADA"
    })
    void creadoLocalIncompatibleSeCancelaSinEnviar(String escenario) {
        Pago pago = prepararIntento("CREADO");
        prepararDetalles();
        estadosPago("CANCELADO");

        if ("VENCIDA".equals(escenario)) {
            servicio = crearServicio(relojFijo(CREACION.plusMinutes(16)));
        } else if ("CANCELADA".equals(escenario)) {
            alquiler.setEstadoContratacion(new EstadoAlquiler("CANCELADO"));
            primero.setEstado(new EstadoAlquilerDetalle("CANCELADO"));
            segundo.setEstado(new EstadoAlquilerDetalle("CANCELADO"));
        } else {
            segundo.setEstado(new EstadoAlquilerDetalle("CANCELADO"));
        }

        ResultadoAnticipoDTO resultado = servicio.resolverAnticipo(7, CLAVE);

        assertEquals("CANCELADO", resultado.getEstadoPago());
        assertEquals(new BigDecimal("300.00"), pago.getMontoArs());
        assertNull(pago.getReferenciaExterna());
        assertNull(pago.getFechaConfirmacion());

        ArgumentCaptor<HistorialEstadoPago> evento =
                ArgumentCaptor.forClass(HistorialEstadoPago.class);

        verify(historialPagoRepository).save(evento.capture());
        assertEquals("CANCELACION_LOCAL", evento.getValue().getFuente());
        assertEquals("CREADO",
                evento.getValue().getEstadoAnterior().getCodigo());
        assertEquals("CANCELADO",
                evento.getValue().getEstadoNuevo().getCodigo());

        verifyNoInteractions(
                pasarelaSimulada,
                reembolsoRepository,
                estadoDetalleRepository,
                historialDetalleRepository,
                historialReembolsoRepository
        );
    }

    @Test
    void aprobacionTardiaRegistraReembolsoPendienteUnaSolaVez() {
        Pago pago = prepararIntento("PENDIENTE");
        alquiler.setEstadoContratacion(new EstadoAlquiler("VENCIDO"));
        primero.setEstado(new EstadoAlquilerDetalle("VENCIDO"));
        segundo.setEstado(new EstadoAlquilerDetalle("VENCIDO"));

        LocalDateTime tardia = CREACION.plusMinutes(16);
        servicio = crearServicio(relojFijo(tardia));

        prepararProveedor(pago, "APROBADO", "1000.00");
        estadosPago("APROBADO");
        habilitarGuardadoReembolso();

        ResultadoAnticipoDTO resultado = servicio.resolverAnticipo(7, CLAVE);

        assertEquals("APROBADO", resultado.getEstadoPago());
        assertEquals(tardia, pago.getFechaConfirmacion());
        assertEquals("VENCIDO", primero.getEstado().getCodigo());
        assertEquals("VENCIDO", segundo.getEstado().getCodigo());
        assertEquals("VENCIDO",
                alquiler.getEstadoContratacion().getCodigo());

        verificarReembolsoPendiente(pago, tardia);

        servicio.resolverAnticipo(7, CLAVE);

        verify(pasarelaSimulada, times(1))
                .evaluarPago(pago, new BigDecimal("1000.00"));
        verify(reembolsoRepository, times(1))
                .saveAndFlush(any(Reembolso.class));
        verify(historialReembolsoRepository, times(1))
                .save(any(HistorialEstadoReembolso.class));
    }

    @Test
    void cancelacionTotalOcultaNoImpideConciliarElPago() {
        Pago pago = prepararIntento("PENDIENTE");
        alquiler.setEstadoContratacion(new EstadoAlquiler("CANCELADO"));
        alquiler.setBorradoAlquiler(true);
        primero.setEstado(new EstadoAlquilerDetalle("CANCELADO"));
        segundo.setEstado(new EstadoAlquilerDetalle("CANCELADO"));

        prepararProveedor(pago, "APROBADO", "1000.00");
        estadosPago("APROBADO");
        habilitarGuardadoReembolso();

        servicio.resolverAnticipo(7, CLAVE);

        assertEquals("CANCELADO", primero.getEstado().getCodigo());
        assertEquals("CANCELADO", segundo.getEstado().getCodigo());
        assertTrue(alquiler.isBorradoAlquiler());
        verificarReembolsoPendiente(pago, AHORA);
    }

    @Test
    void cancelacionParcialConservaMontoYSoloHabilitaReservadas() {
        Pago pago = prepararIntento("PENDIENTE");
        segundo.setEstado(new EstadoAlquilerDetalle("CANCELADO"));

        prepararProveedor(pago, "APROBADO", "1000.00");
        estadosPago("APROBADO");
        habilitarCatalogoRetiro();

        servicio.resolverAnticipo(7, CLAVE);

        assertEquals(new BigDecimal("300.00"), pago.getMontoArs());
        assertEquals(new BigDecimal("300.00"), alquiler.getCostoEstimadoArs());
        assertEquals("PENDIENTE_RETIRO", primero.getEstado().getCodigo());
        assertEquals("CANCELADO", segundo.getEstado().getCodigo());

        ArgumentCaptor<HistorialEstadoDetalle> evento =
                ArgumentCaptor.forClass(HistorialEstadoDetalle.class);

        verify(historialDetalleRepository).save(evento.capture());
        assertSame(primero, evento.getValue().getAlquilerDetalle());

        verificarSinReembolso();
    }

    @Test
    void respuestaEnElLimiteNoHabilitaAunqueEmpezoAntes() {
        Pago pago = prepararIntento("PENDIENTE");
        prepararProveedor(pago, "APROBADO", "1000.00");
        estadosPago("APROBADO");
        habilitarGuardadoReembolso();

        LocalDateTime vencimiento = CREACION.plusMinutes(15);

        Clock reloj = mock(Clock.class);
        when(reloj.instant())
                .thenReturn(vencimiento.minusSeconds(1).toInstant(ZoneOffset.UTC))
                .thenReturn(vencimiento.toInstant(ZoneOffset.UTC));

        servicio = crearServicio(reloj);
        servicio.resolverAnticipo(7, CLAVE);

        assertEquals("RESERVADO", primero.getEstado().getCodigo());
        assertEquals("RESERVADO", segundo.getEstado().getCodigo());
        assertNull(primero.getFechaHoraRetiro());
        verificarReembolsoPendiente(pago, vencimiento);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROBADO", "RECHAZADO", "CANCELADO"})
    void estadoDefinitivoNoVuelveAConsultarProveedor(String estado) {
        Pago pago = prepararIntento(estado);

        ResultadoAnticipoDTO resultado = servicio.resolverAnticipo(7, CLAVE);

        assertEquals(estado, resultado.getEstadoPago());
        assertEquals(pago.getId(), resultado.getPagoId());

        verifyNoInteractions(
                pasarelaSimulada,
                reembolsoRepository,
                alquilerDetalleRepository,
                estadoPagoRepository,
                estadoDetalleRepository,
                historialPagoRepository,
                historialDetalleRepository,
                historialReembolsoRepository
        );
        verify(pagoRepository, never()).flush();
    }

    @Test
    void evaluaSaldoRestanteEnLugarDeReponerElInicial() {
        Pago pago = prepararIntento("PENDIENTE");
        prepararDetalles();

        Alquiler anterior = new Alquiler();
        anterior.setId(8);
        anterior.setUsuario(usuario);

        Pago yaCobrado = crearPago(anterior, 22, "APROBADO", "900.00");

        prepararSaldo("1000.00", List.of(yaCobrado, pago));
        estadosPago("RECHAZADO");

        when(pasarelaSimulada.evaluarPago(pago, new BigDecimal("100.00")))
                .thenReturn("RECHAZADO");

        servicio.resolverAnticipo(7, CLAVE);

        assertEquals("RECHAZADO", pago.getEstadoPago().getCodigo());
        verify(pasarelaSimulada)
                .evaluarPago(pago, new BigDecimal("100.00"));
        verificarSinReembolso();
    }

    @Test
    void clienteNoPuedeResolverAlquilerAjeno() {
        prepararAcceso();
        alquiler.setUsuario(crearUsuario(2));

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.resolverAnticipo(7, CLAVE)
        );

        verifyNoInteractions(
                pagoRepository,
                pasarelaSimulada,
                reembolsoRepository,
                historialPagoRepository,
                historialDetalleRepository,
                historialReembolsoRepository
        );
    }

    @Test
    void intentoInexistenteDevuelve404SinMovimientos() {
        prepararAcceso();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> servicio.resolverAnticipo(7, CLAVE)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        verify(pagoRepository).findByClaveIdempotencia(CLAVE_DB);
        verify(pagoRepository, never()).buscarPorIdConBloqueo(anyInt());

        verifyNoInteractions(
                pasarelaSimulada,
                reembolsoRepository,
                historialPagoRepository,
                historialDetalleRepository,
                historialReembolsoRepository
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TECNICO"})
    void sesionSinClienteNoConsultaDatos(String rol) {
        autenticar(rol);

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.resolverAnticipo(7, CLAVE)
        );

        verifyNoInteractions(
                usuarioRepository,
                alquilerRepository,
                pagoRepository,
                pasarelaSimulada,
                reembolsoRepository
        );
    }

    @Test
    void saldoCeroPuedeRechazarseSinSerFalloInterno() {
        Pago pago = prepararIntento("PENDIENTE");
        prepararProveedor(pago, "RECHAZADO", "0.00");
        estadosPago("RECHAZADO");

        ResultadoAnticipoDTO resultado = servicio.resolverAnticipo(7, CLAVE);

        assertEquals("RECHAZADO", resultado.getEstadoPago());
        assertNull(pago.getFechaConfirmacion());
        assertEquals("RESERVADO", primero.getEstado().getCodigo());
        verificarSinReembolso();
    }

    private PagoService crearServicio(Clock reloj) {
        return new PagoService(
                usuarioRepository,
                pagoRepository,
                reembolsoRepository,
                pasarelaSimulada,
                alquilerRepository,
                alquilerDetalleRepository,
                estadoPagoRepository,
                historialPagoRepository,
                estadoDetalleRepository,
                historialDetalleRepository,
                historialReembolsoRepository,
                liquidacionAlquilerService,
                reembolsoService,
                reloj
        );
    }

    private Clock relojFijo(LocalDateTime fecha) {
        return Clock.fixed(fecha.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
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

    private Usuario crearUsuario(int id) {
        Usuario cuenta = new Usuario();
        cuenta.setId(id);
        cuenta.setNickname(id == 1 ? "qa.resolver" : "qa.otro");
        cuenta.setEmail(id == 1 ? EMAIL : "qa.otro@example.com");
        cuenta.setActivo(true);

        Rol rol = new Rol("CLIENTE");
        ReflectionTestUtils.setField(rol, "id", 1);
        cuenta.getUsuarioRoles().add(new UsuarioRol(cuenta, rol));

        return cuenta;
    }

    private AlquilerDetalle crearDetalle(int id, String importe) {
        AlquilerDetalle detalle = new AlquilerDetalle();
        ReflectionTestUtils.setField(detalle, "id", id);
        detalle.setAlquiler(alquiler);
        detalle.setEstado(new EstadoAlquilerDetalle("RESERVADO"));
        detalle.setSubtotalEstimadoArs(new BigDecimal(importe));
        return detalle;
    }

    private Pago crearPago(
            Alquiler propietario,
            int id,
            String estado,
            String importe
    ) {
        String clave = id == 11
                ? CLAVE_DB
                : "ANTICIPO:1:123e4567-e89b-42d3-a456-426614174001";

        Pago pago = new Pago(
                propietario,
                new EstadoPago("CREADO"),
                "ANTICIPO",
                new BigDecimal(importe),
                CREACION.plusMinutes(1),
                "SIMULADA",
                "PRUEBA",
                clave
        );

        ReflectionTestUtils.setField(pago, "id", id);
        pago.setEstadoPago(new EstadoPago(estado));

        if (!"CREADO".equals(estado)) {
            pago.setReferenciaExterna("SIM-PAGO-" + id);
        }

        if ("APROBADO".equals(estado)) {
            pago.setFechaConfirmacion(CREACION.plusMinutes(2));
        }

        return pago;
    }

    private void prepararAcceso() {
        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL))
                .thenReturn(Optional.of(usuario));

        when(alquilerRepository.buscarPorIdConBloqueo(7))
                .thenReturn(Optional.of(alquiler));
    }

    private Pago prepararIntento(String estado) {
        prepararAcceso();

        Pago pago = crearPago(alquiler, 11, estado, "300.00");

        when(pagoRepository.findByClaveIdempotencia(CLAVE_DB))
                .thenReturn(Optional.of(pago));

        when(pagoRepository.buscarPorIdConBloqueo(11))
                .thenReturn(Optional.of(pago));

        return pago;
    }

    private void prepararDetalles() {
        when(alquilerDetalleRepository.buscarPorAlquilerConDatos(7))
                .thenReturn(detalles);
    }

    private void prepararSaldo(String inicial, List<Pago> pagos) {
        when(pasarelaSimulada.obtenerSaldoInicial("qa.resolver"))
                .thenReturn(new BigDecimal(inicial));

        when(pagoRepository
                .findByAlquilerUsuarioIdAndPlataformaAndEntornoOrderByIdAsc(
                        1, "SIMULADA", "PRUEBA"
                ))
                .thenReturn(pagos);

        when(reembolsoRepository
                .findByPagoAlquilerUsuarioIdAndPagoPlataformaAndPagoEntornoOrderByIdAsc(
                        1, "SIMULADA", "PRUEBA"
                ))
                .thenReturn(List.of());
    }

    private void prepararProveedor(
            Pago pago,
            String resultado,
            String saldo
    ) {
        prepararDetalles();
        prepararSaldo(saldo, List.of(pago));

        when(pasarelaSimulada.evaluarPago(pago, new BigDecimal(saldo)))
                .thenReturn(resultado);
    }

    private void estadosPago(String... codigos) {
        for (String codigo : codigos) {
            when(estadoPagoRepository.findByCodigo(codigo))
                    .thenReturn(Optional.of(new EstadoPago(codigo)));
        }
    }

    private void habilitarCatalogoRetiro() {
        when(estadoDetalleRepository.findByCodigo("PENDIENTE_RETIRO"))
                .thenReturn(Optional.of(
                        new EstadoAlquilerDetalle("PENDIENTE_RETIRO")
                ));
    }

    private void habilitarGuardadoReembolso() {
        when(reembolsoRepository.saveAndFlush(any(Reembolso.class)))
                .thenAnswer(invocacion -> {
                    Reembolso reembolso = invocacion.getArgument(0);
                    ReflectionTestUtils.setField(reembolso, "id", 21);
                    return reembolso;
                });
    }

    private void verificarSinReembolso() {
        verify(reembolsoRepository, never())
                .saveAndFlush(any(Reembolso.class));
        verifyNoInteractions(historialReembolsoRepository);
    }

    private void verificarReembolsoPendiente(
            Pago pago,
            LocalDateTime fecha
    ) {
        ArgumentCaptor<Reembolso> reembolsos =
                ArgumentCaptor.forClass(Reembolso.class);

        ArgumentCaptor<HistorialEstadoReembolso> eventos =
                ArgumentCaptor.forClass(HistorialEstadoReembolso.class);

        verify(reembolsoRepository).saveAndFlush(reembolsos.capture());
        verify(historialReembolsoRepository).save(eventos.capture());

        Reembolso reembolso = reembolsos.getValue();
        HistorialEstadoReembolso evento = eventos.getValue();

        assertSame(pago, reembolso.getPago());
        assertEquals(new BigDecimal("300.00"), reembolso.getMontoArs());
        assertEquals("ARS", reembolso.getMoneda());
        assertEquals("PENDIENTE", reembolso.getEstado());
        assertEquals(fecha, reembolso.getFechaCreacion());
        assertNull(reembolso.getFechaConfirmacion());
        assertNull(reembolso.getReferenciaExterna());
        assertEquals("REEMBOLSO_SIN_USO:11",
                reembolso.getClaveIdempotencia());

        assertSame(reembolso, evento.getReembolso());
        assertNull(evento.getEstadoAnterior());
        assertEquals("PENDIENTE", evento.getEstadoNuevo());
        assertEquals(fecha, evento.getFechaHoraCambio());
        assertEquals("SISTEMA", evento.getOrigen());
        assertNull(evento.getActorUsuario());
        assertNull(evento.getFechaHoraEventoExterno());

        verifyNoInteractions(estadoDetalleRepository, historialDetalleRepository);
    }
}
