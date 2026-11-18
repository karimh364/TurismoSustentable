package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerDetalleDTO;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetiroAlquilerServiceTest {

    private static final String EMAIL = "qa.retiro@example.com";

    private static final LocalDateTime CREACION =
            LocalDateTime.of(2026, 11, 15, 12, 0);

    private static final LocalDateTime AHORA =
            CREACION.plusMinutes(5);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AlquilerRepository alquilerRepository;
    @Mock private AlquilerDetalleRepository detalleRepository;
    @Mock private BicicletaRepository bicicletaRepository;
    @Mock private EstacionRepository estacionRepository;
    @Mock private EstadoAlquilerRepository estadoAlquilerRepository;
    @Mock private EstadoAlquilerDetalleRepository estadoDetalleRepository;
    @Mock private EstadoBicicletaRepository estadoBicicletaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReembolsoRepository reembolsoRepository;
    @Mock private HistorialEstadoDetalleRepository historialRepository;
    @Mock private TrazabilidadInventarioService trazabilidad;

    @InjectMocks
    private AlquilerService servicio;

    private Usuario usuario;
    private Estacion estacion;
    private Alquiler alquiler;
    private AlquilerDetalle primero;
    private AlquilerDetalle segundo;
    private Pago aprobado;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();
        fijarReloj(AHORA);

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setNickname("qa.retiro");
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);
        agregarRol("CLIENTE", 1);

        estacion = new Estacion(
                "Plaza QA", "Calle QA", 10, new BigDecimal("0.0"), new BigDecimal("0.0")
        );
        estacion.setEsPublica(true);
        ReflectionTestUtils.setField(estacion, "id", 1);

        alquiler = new Alquiler();
        alquiler.setId(7);
        alquiler.setUsuario(usuario);
        alquiler.setUbicacionRetiro(estacion);
        alquiler.setEstadoContratacion(
                new EstadoAlquiler("RESERVADO")
        );
        alquiler.setFechaHoraCreacion(CREACION);
        alquiler.setFechaHoraVencimientoReserva(
                CREACION.plusMinutes(15)
        );
        alquiler.setCostoEstimadoUsd(new BigDecimal("13.50"));
        alquiler.setCostoEstimadoArs(new BigDecimal("20655.00"));
        alquiler.setCotizacionArsPorUsd(new BigDecimal("1530.00"));

        primero = crearDetalle(
                31, 101, "MONTANA", "10.00", "7.50", "11475.00"
        );
        segundo = crearDetalle(
                32, 102, "PASEO", "8.00", "6.00", "9180.00"
        );

        aprobado = crearPago("APROBADO");
        autenticar("CLIENTE");
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void retiraSoloLaSeleccionadaYConservaSnapshots(
            boolean tambienEsAdmin
    ) {
        if (tambienEsAdmin) {
            agregarRol("ADMIN", 2);
            autenticar("ADMIN", "CLIENTE");
        }

        prepararCompleto();

        AlquilerDetalleDTO resultado =
                servicio.retirarBicicleta(7, 31);

        assertEquals(Integer.valueOf(31), resultado.getId());
        assertEquals("EN_CURSO", resultado.getEstado());
        assertEquals(AHORA, resultado.getFechaHoraRetiro());
        assertEquals(
                AHORA.plusSeconds(2700),
                resultado.getFechaHoraDevolucionEstimada()
        );

        assertEquals(
                "EN_USO",
                primero.getBicicleta().getEstado().getCodigo()
        );
        assertNull(primero.getBicicleta().getUbicacionActual());

        assertEquals(
                "PENDIENTE_RETIRO",
                segundo.getEstado().getCodigo()
        );
        assertNull(segundo.getFechaHoraRetiro());
        assertSame(
                estacion,
                segundo.getBicicleta().getUbicacionActual()
        );
        assertEquals(
                "RESERVADA",
                segundo.getBicicleta().getEstado().getCodigo()
        );

        assertEquals(
                "EN_PROGRESO",
                alquiler.getEstadoContratacion().getCodigo()
        );
        assertSame(estacion, alquiler.getUbicacionRetiro());
        assertEquals(
                CREACION.plusMinutes(15),
                alquiler.getFechaHoraVencimientoReserva()
        );
        assertEquals(
                new BigDecimal("20655.00"),
                alquiler.getCostoEstimadoArs()
        );
        assertEquals(
                new BigDecimal("1530.00"),
                alquiler.getCotizacionArsPorUsd()
        );
        assertEquals(
                new BigDecimal("10.00"),
                primero.getPrecioPorHoraAplicadoUsd()
        );
        assertNull(primero.getSubtotalRealArs());
        assertNull(alquiler.getCostoFinalArs());

        ArgumentCaptor<HistorialEstadoDetalle> captor =
                ArgumentCaptor.forClass(HistorialEstadoDetalle.class);

        verify(historialRepository).save(captor.capture());

        HistorialEstadoDetalle historia = captor.getValue();
        assertSame(primero, historia.getAlquilerDetalle());
        assertEquals(
                "PENDIENTE_RETIRO",
                historia.getEstadoAnterior().getCodigo()
        );
        assertEquals("EN_CURSO", historia.getEstadoNuevo().getCodigo());
        assertEquals(AHORA, historia.getFechaHoraCambio());
        assertEquals("USUARIO", historia.getOrigen());
        assertSame(usuario, historia.getActorUsuario());

        verify(trazabilidad).estado(
                same(primero.getBicicleta()),
                argThat(estado ->
                        "RESERVADA".equals(estado.getCodigo())
                ),
                same(usuario),
                anyString(),
                eq(AHORA)
        );

        verify(detalleRepository).flush();
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(pagoRepository, never()).saveAndFlush(any(Pago.class));
        verify(reembolsoRepository, never())
                .saveAndFlush(any(Reembolso.class));
    }

    @Test
    void repetirDespuesDelPlazoNoReiniciaHoraNiDuplicaHistorias() {
        prepararCompleto();

        servicio.retirarBicicleta(7, 31);
        fijarReloj(AHORA.plusHours(1));

        AlquilerDetalleDTO repetido =
                servicio.retirarBicicleta(7, 31);

        assertEquals(AHORA, repetido.getFechaHoraRetiro());
        assertEquals(
                AHORA.plusSeconds(2700),
                repetido.getFechaHoraDevolucionEstimada()
        );

        verify(historialRepository, times(1))
                .save(any(HistorialEstadoDetalle.class));
        verify(trazabilidad, times(1)).estado(
                any(Bicicleta.class),
                any(EstadoBicicleta.class),
                any(Usuario.class),
                anyString(),
                any(LocalDateTime.class)
        );
        verify(detalleRepository, times(1)).flush();
        verify(pagoRepository, times(1))
                .findByAlquilerIdOrderByIdAsc(7);
    }

    @Test
    void segundaBicicletaTieneSuPropiaHoraDeRetiro() {
        prepararCompleto();
        servicio.retirarBicicleta(7, 31);

        LocalDateTime segundoRetiro = AHORA.plusMinutes(2);
        fijarReloj(segundoRetiro);

        when(detalleRepository
                .buscarPorAlquilerYDetalleConBloqueo(7, 32))
                .thenReturn(Optional.of(segundo));

        when(bicicletaRepository
                .buscarActivaPorIdConBloqueo(102))
                .thenReturn(Optional.of(segundo.getBicicleta()));

        servicio.retirarBicicleta(7, 32);

        assertEquals(AHORA, primero.getFechaHoraRetiro());
        assertEquals(segundoRetiro, segundo.getFechaHoraRetiro());
        assertEquals(
                segundoRetiro.plusSeconds(2700),
                segundo.getFechaHoraDevolucionEstimada()
        );
        assertNull(segundo.getBicicleta().getUbicacionActual());
        assertEquals(
                "EN_PROGRESO",
                alquiler.getEstadoContratacion().getCodigo()
        );

        verify(historialRepository, times(2))
                .save(any(HistorialEstadoDetalle.class));
        verify(detalleRepository, times(2)).flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIN_SESION", "ADMIN", "TECNICO"})
    void sesionSinClienteNoConsultaDatos(String rol) {
        if ("SIN_SESION".equals(rol)) {
            SecurityContextHolder.clearContext();
        } else {
            autenticar(rol);
        }

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.retirarBicicleta(7, 31)
        );

        verifyNoInteractions(
                usuarioRepository,
                alquilerRepository,
                detalleRepository,
                bicicletaRepository,
                pagoRepository,
                reembolsoRepository,
                historialRepository,
                trazabilidad
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"INACTIVO", "SIN_CLIENTE"})
    void exigeCuentaActivaYRolVigente(String escenario) {
        if ("INACTIVO".equals(escenario)) {
            usuario.setActivo(false);
        } else {
            usuario.getUsuarioRoles().clear();
            agregarRol("ADMIN", 2);
        }

        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL))
                .thenReturn(Optional.of(usuario));

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.retirarBicicleta(7, 31)
        );

        verifyNoInteractions(alquilerRepository);
        verificarSinEscrituras();
    }

    @Test
    void noPermiteRetirarDeUnAlquilerAjeno() {
        Usuario otro = new Usuario();
        otro.setId(2);
        alquiler.setUsuario(otro);

        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL))
                .thenReturn(Optional.of(usuario));

        when(alquilerRepository.buscarPorIdConBloqueo(7))
                .thenReturn(Optional.of(alquiler));

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.retirarBicicleta(7, 31)
        );

        verifyNoInteractions(detalleRepository);
        verificarSinEscrituras();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SIN_PAGO", "CREADO", "PENDIENTE", "RECHAZADO", "CANCELADO"
    })
    void sinAnticipoAprobadoNoIniciaUso(String estadoPago) {
        List<Pago> pagos = "SIN_PAGO".equals(estadoPago)
                ? List.of()
                : List.of(crearPago(estadoPago));

        prepararCobertura(pagos, List.of());

        assertThrows(
                IllegalStateException.class,
                () -> servicio.retirarBicicleta(7, 31)
        );

        assertEquals(
                "PENDIENTE_RETIRO",
                primero.getEstado().getCodigo()
        );
        verificarSinEscrituras();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1})
    void respetaElInstanteExactoDeVencimiento(int segundosDesdeLimite) {
        LocalDateTime instante = CREACION.plusMinutes(15)
                .plusSeconds(segundosDesdeLimite);

        fijarReloj(instante);
        prepararCompleto();

        if (segundosDesdeLimite < 0) {
            servicio.retirarBicicleta(7, 31);
            assertEquals(instante, primero.getFechaHoraRetiro());
        } else {
            assertThrows(
                    IllegalStateException.class,
                    () -> servicio.retirarBicicleta(7, 31)
            );
            verificarSinEscrituras();
        }
    }

    @Test
    void siLaEsperaAlcanzaElLimiteSeRechazaAntesDeMutar() {
        prepararCompleto();

        Clock cambiante = mock(Clock.class);
        when(cambiante.instant()).thenReturn(
                AHORA.toInstant(ZoneOffset.UTC),
                CREACION.plusMinutes(15).toInstant(ZoneOffset.UTC)
        );
        ReflectionTestUtils.setField(servicio, "reloj", cambiante);

        assertThrows(
                IllegalStateException.class,
                () -> servicio.retirarBicicleta(7, 31)
        );

        verify(cambiante, times(2)).instant();
        verificarSinEscrituras();
    }

    @Test
    void anticipoCeroHabilitaSinInventarPago() {
        for (AlquilerDetalle detalle : List.of(primero, segundo)) {
            detalle.setEstado(new EstadoAlquilerDetalle("RESERVADO"));
            detalle.setDuracionEstimadaSegundos(1);
            detalle.setPrecioPorHoraAplicadoUsd(new BigDecimal("0.01"));
            detalle.setSubtotalEstimadoUsd(new BigDecimal("0.00"));
            detalle.setSubtotalEstimadoArs(new BigDecimal("0.00"));
            detalle.getBicicleta().getTipo()
                    .setPrecioPorHoraUsd(new BigDecimal("0.01"));
        }
        alquiler.setCostoEstimadoUsd(new BigDecimal("0.00"));
        alquiler.setCostoEstimadoArs(new BigDecimal("0.00"));

        prepararCobertura(List.of(), List.of());
        prepararCatalogos();

        servicio.retirarBicicleta(7, 31);

        assertEquals("EN_CURSO", primero.getEstado().getCodigo());
        assertEquals(
                AHORA.plusSeconds(1),
                primero.getFechaHoraDevolucionEstimada()
        );
        assertEquals("RESERVADO", segundo.getEstado().getCodigo());
        assertNull(segundo.getFechaHoraRetiro());

        ArgumentCaptor<HistorialEstadoDetalle> captor =
                ArgumentCaptor.forClass(HistorialEstadoDetalle.class);

        verify(historialRepository, times(2)).save(captor.capture());

        HistorialEstadoDetalle habilitacion =
                captor.getAllValues().get(0);
        HistorialEstadoDetalle retiro =
                captor.getAllValues().get(1);

        assertEquals("RESERVADO",
                habilitacion.getEstadoAnterior().getCodigo());
        assertEquals("PENDIENTE_RETIRO",
                habilitacion.getEstadoNuevo().getCodigo());
        assertEquals("SISTEMA", habilitacion.getOrigen());
        assertNull(habilitacion.getActorUsuario());

        assertEquals("PENDIENTE_RETIRO",
                retiro.getEstadoAnterior().getCodigo());
        assertEquals("EN_CURSO", retiro.getEstadoNuevo().getCodigo());
        assertEquals("USUARIO", retiro.getOrigen());
        assertSame(usuario, retiro.getActorUsuario());

        assertEquals(AHORA, habilitacion.getFechaHoraCambio());
        assertEquals(AHORA, retiro.getFechaHoraCambio());

        verify(pagoRepository, never()).save(any(Pago.class));
        verify(pagoRepository, never()).saveAndFlush(any(Pago.class));
        verifyNoInteractions(reembolsoRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDIENTE", "EN_PROCESO", "CONFIRMADO"})
    void dineroComprometidoEnReembolsoNoFinanciaOtroRetiro(
            String estadoReembolso
    ) {
        Reembolso reembolso = crearReembolso(
                estadoReembolso, "1000.00"
        );

        prepararCobertura(
                List.of(aprobado),
                List.of(reembolso)
        );

        assertThrows(
                IllegalStateException.class,
                () -> servicio.retirarBicicleta(7, 31)
        );

        verificarSinEscrituras();
    }

    @Test
    void cancelacionParcialPermiteRetirarSiLaCoberturaRestanteAlcanza() {
        // Antecedente preparado: no estamos ejecutando la cancelación.
        segundo.setEstado(new EstadoAlquilerDetalle("CANCELADO"));
        segundo.setFechaHoraCierreDetalle(CREACION.plusMinutes(3));
        segundo.setSubtotalRealUsd(new BigDecimal("0.00"));
        segundo.setSubtotalRealArs(new BigDecimal("0.00"));
        segundo.getBicicleta().setEstado(
                new EstadoBicicleta("DISPONIBLE")
        );

        Reembolso reembolso = crearReembolso(
                "PENDIENTE", "9180.00"
        );

        prepararCobertura(
                List.of(aprobado),
                List.of(reembolso)
        );
        prepararCatalogos();

        servicio.retirarBicicleta(7, 31);

        assertEquals("EN_CURSO", primero.getEstado().getCodigo());
        assertEquals("CANCELADO", segundo.getEstado().getCodigo());
        assertNull(segundo.getFechaHoraRetiro());
        assertSame(
                estacion,
                segundo.getBicicleta().getUbicacionActual()
        );
        assertEquals(
                new BigDecimal("20655.00"),
                aprobado.getMontoArs()
        );
        assertEquals("PENDIENTE", reembolso.getEstado());
    }

    @Test
    void estacionInternaImpideRetiroSinCambios() {
        prepararBicicleta();
        estacion.setEsPublica(false);

        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(estacion));

        assertThrows(
                IllegalStateException.class,
                () -> servicio.retirarBicicleta(7, 31)
        );

        verificarSinEscrituras();
    }

    @Test
    void bicicletaEnMantenimientoNoSeRetiraAunqueElDetalleEstePendiente() {
        prepararBicicleta();
        primero.getBicicleta().setEstado(
                new EstadoBicicleta("MANTENIMIENTO")
        );

        assertThrows(
                IllegalStateException.class,
                () -> servicio.retirarBicicleta(7, 31)
        );

        assertEquals(
                "MANTENIMIENTO",
                primero.getBicicleta().getEstado().getCodigo()
        );
        verifyNoInteractions(estacionRepository);
        verificarSinEscrituras();
    }

    private void prepararBicicleta() {
        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL))
                .thenReturn(Optional.of(usuario));

        when(alquilerRepository.buscarPorIdConBloqueo(7))
                .thenReturn(Optional.of(alquiler));

        when(detalleRepository
                .buscarPorAlquilerYDetalleConBloqueo(7, 31))
                .thenReturn(Optional.of(primero));

        when(bicicletaRepository.buscarActivaPorIdConBloqueo(101))
                .thenReturn(Optional.of(primero.getBicicleta()));
    }

    private void prepararCobertura(
            List<Pago> pagos,
            List<Reembolso> reembolsos
    ) {
        prepararBicicleta();

        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(estacion));

        when(detalleRepository.buscarPorAlquilerConDatos(7))
                .thenReturn(List.of(primero, segundo));

        when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                .thenReturn(pagos);

        for (Pago pago : pagos) {
            if ("APROBADO".equals(pago.getEstadoPago().getCodigo())) {
                when(reembolsoRepository
                        .findByPagoIdOrderByIdAsc(pago.getId()))
                        .thenReturn(reembolsos);
            }
        }
    }

    private void prepararCompleto() {
        prepararCobertura(List.of(aprobado), List.of());
        prepararCatalogos();
    }

    private void prepararCatalogos() {
        when(estadoDetalleRepository.findByCodigo("PENDIENTE_RETIRO"))
                .thenReturn(Optional.of(
                        new EstadoAlquilerDetalle("PENDIENTE_RETIRO")
                ));

        when(estadoDetalleRepository.findByCodigo("EN_CURSO"))
                .thenReturn(Optional.of(
                        new EstadoAlquilerDetalle("EN_CURSO")
                ));

        when(estadoAlquilerRepository.findByCodigo("EN_PROGRESO"))
                .thenReturn(Optional.of(
                        new EstadoAlquiler("EN_PROGRESO")
                ));

        when(estadoBicicletaRepository.findByCodigo("EN_USO"))
                .thenReturn(Optional.of(
                        new EstadoBicicleta("EN_USO")
                ));
    }

    private AlquilerDetalle crearDetalle(
            int id,
            int bicicletaId,
            String codigoTipo,
            String tarifa,
            String subtotalUsd,
            String subtotalArs
    ) {
        TipoBicicleta tipo = new TipoBicicleta(
                codigoTipo, new BigDecimal(tarifa), 3
        );
        ReflectionTestUtils.setField(tipo, "id", bicicletaId);

        Bicicleta bicicleta = new Bicicleta(
                "QA-" + bicicletaId,
                tipo,
                new EstadoBicicleta("RESERVADA"),
                estacion
        );
        bicicleta.setId(bicicletaId);
        bicicleta.setFechaAlta(CREACION.toLocalDate());

        AlquilerDetalle detalle = new AlquilerDetalle();
        ReflectionTestUtils.setField(detalle, "id", id);
        ReflectionTestUtils.setField(detalle, "version", 0);
        detalle.setAlquiler(alquiler);
        detalle.setBicicleta(bicicleta);
        detalle.setTipoBicicletaContratado(tipo);
        detalle.setEstado(
                new EstadoAlquilerDetalle("PENDIENTE_RETIRO")
        );
        detalle.setPrecioPorHoraAplicadoUsd(new BigDecimal(tarifa));
        detalle.setDuracionEstimadaSegundos(2700);
        detalle.setSubtotalEstimadoUsd(new BigDecimal(subtotalUsd));
        detalle.setSubtotalEstimadoArs(new BigDecimal(subtotalArs));
        return detalle;
    }

    private Pago crearPago(String estado) {
        Pago pago = new Pago(
                alquiler,
                new EstadoPago("CREADO"),
                "ANTICIPO",
                new BigDecimal("20655.00"),
                CREACION.plusMinutes(1),
                "SIMULADA",
                "PRUEBA",
                "ANTICIPO:1:123e4567-e89b-42d3-a456-426614174000"
        );
        ReflectionTestUtils.setField(pago, "id", 11);
        pago.setEstadoPago(new EstadoPago(estado));

        if ("APROBADO".equals(estado)) {
            pago.setFechaConfirmacion(CREACION.plusMinutes(2));
            pago.setReferenciaExterna("SIM-PAGO-11");
        }

        return pago;
    }

    private Reembolso crearReembolso(String estado, String importe) {
        Reembolso reembolso = new Reembolso(
                aprobado,
                new BigDecimal(importe),
                CREACION.plusMinutes(3),
                "Ajuste preparado para comprobar cobertura.",
                "QA-REEMBOLSO-11"
        );
        reembolso.setEstado(estado);

        if ("CONFIRMADO".equals(estado)) {
            reembolso.setFechaConfirmacion(CREACION.plusMinutes(4));
        }

        return reembolso;
    }

    private void agregarRol(String codigo, int id) {
        Rol rol = new Rol(codigo);
        ReflectionTestUtils.setField(rol, "id", id);
        usuario.getUsuarioRoles().add(new UsuarioRol(usuario, rol));
    }

    private void autenticar(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL,
                        null,
                        AuthorityUtils.createAuthorityList(roles)
                )
        );
    }

    private void fijarReloj(LocalDateTime fecha) {
        ReflectionTestUtils.setField(
                servicio,
                "reloj",
                Clock.fixed(
                        fecha.toInstant(ZoneOffset.UTC),
                        ZoneOffset.UTC
                )
        );
    }

    private void verificarSinEscrituras() {
        assertNull(primero.getFechaHoraRetiro());
        assertNull(primero.getFechaHoraDevolucionEstimada());
        assertSame(
                estacion,
                primero.getBicicleta().getUbicacionActual()
        );
        assertEquals(
                "RESERVADO",
                alquiler.getEstadoContratacion().getCodigo()
        );

        verifyNoInteractions(historialRepository, trazabilidad);
        verify(detalleRepository, never()).flush();
    }
}
