package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerDetalleDTO;
import DSII.TurismoSustentable.dto.DevolverBicicletaDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityNotFoundException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevolucionAlquilerServiceTest {

    private static final String EMAIL = "qa.devolucion@example.com";
    private static final LocalDateTime RETIRO = LocalDateTime.of(2026, 11, 16, 10, 0, 0, 900_000_000);
    private static final LocalDateTime AHORA = RETIRO.plusSeconds(1800).plusNanos(800_000_000);
    private static ValidatorFactory validadores;

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AlquilerRepository alquilerRepository;
    @Mock private AlquilerDetalleRepository detalleRepository;
    @Mock private BicicletaRepository bicicletaRepository;
    @Mock private EstacionRepository estacionRepository;
    @Mock private EstadoAlquilerRepository estadoAlquilerRepository;
    @Mock private EstadoAlquilerDetalleRepository estadoDetalleRepository;
    @Mock private EstadoBicicletaRepository estadoBicicletaRepository;
    @Mock private IncidenciaRepository incidenciaRepository;
    @Mock private IncidenciaService incidenciaService;
    @Mock private OrdenMantenimientoRepository ordenRepository;
    @Mock private TipoMantenimientoRepository tipoOrdenRepository;
    @Mock private EstadoOrdenMantenimientoRepository estadoOrdenRepository;
    @Mock private HistorialOrdenMantenimientoRepository historiaOrdenRepository;
    @Mock private HistorialEstadoDetalleRepository historiaDetalleRepository;
    @Mock private TrazabilidadInventarioService trazabilidad;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReembolsoRepository reembolsoRepository;
    @Mock private CotizacionDolarService cotizacionDolarService;
    @Mock private LiquidacionAlquilerService liquidacionAlquilerService;
    @InjectMocks private AlquilerService servicio;

    private Usuario actor;
    private Alquiler alquiler;
    private AlquilerDetalle detalle;
    private AlquilerDetalle pendiente;
    private Bicicleta bicicleta;
    private Estacion destino;
    private DevolverBicicletaDTO solicitud;

    @BeforeAll
    static void crearValidadores() {
        validadores = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void cerrarValidadores() {
        validadores.close();
    }

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(servicio, "validator", validadores.getValidator());
        ReflectionTestUtils.setField(servicio, "calculadoraAlquiler", new CalculadoraAlquiler());
        fijarReloj(AHORA);
        actor = new Usuario();
        actor.setId(1);
        actor.setEmail(EMAIL);
        actor.setActivo(true);
        agregarRol("CLIENTE", 1);
        autenticar("CLIENTE");
        alquiler = new Alquiler();
        alquiler.setId(7);
        alquiler.setUsuario(actor);
        alquiler.setEstadoContratacion(new EstadoAlquiler("EN_PROGRESO"));
        alquiler.setCotizacionArsPorUsd(new BigDecimal("1530.00"));
        alquiler.setCostoEstimadoUsd(new BigDecimal("18.00"));
        alquiler.setCostoEstimadoArs(new BigDecimal("27540.00"));
        destino = new Estacion("Destino QA", "Calle QA", 2, 0.0, 0.0);
        destino.setEsPublica(true);
        ReflectionTestUtils.setField(destino, "id", 200);
        TipoBicicleta tipo = new TipoBicicleta("PASEO", new BigDecimal("99.00"), 3);
        bicicleta = new Bicicleta("QA-DEV-01", tipo, new EstadoBicicleta("EN_USO"), null);
        bicicleta.setId(101);
        bicicleta.setFechaAlta(RETIRO.toLocalDate());
        detalle = new AlquilerDetalle();
        ReflectionTestUtils.setField(detalle, "id", 31);
        ReflectionTestUtils.setField(detalle, "version", 1);
        detalle.setAlquiler(alquiler);
        detalle.setBicicleta(bicicleta);
        detalle.setTipoBicicletaContratado(tipo);
        detalle.setEstado(new EstadoAlquilerDetalle("EN_CURSO"));
        detalle.setFechaHoraRetiro(RETIRO);
        detalle.setFechaHoraDevolucionEstimada(RETIRO.plusSeconds(3600));
        detalle.setPrecioPorHoraAplicadoUsd(new BigDecimal("10.00"));
        detalle.setDuracionEstimadaSegundos(3600);
        pendiente = new AlquilerDetalle();
        pendiente.setEstado(new EstadoAlquilerDetalle("PENDIENTE_RETIRO"));
        solicitud = new DevolverBicicletaDTO();
        solicitud.setEstacionDevolucionId(200);
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void devuelveSinMultaDentroODespuesDelEstimado(boolean demora) {
        if (demora) {
            detalle.setFechaHoraDevolucionEstimada(RETIRO.plusSeconds(60));
        }
        prepararDevolucion("DISPONIBLE");
        AlquilerDetalleDTO resultado = servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals("DEVUELTO", resultado.getEstado());
        assertEquals(AHORA, resultado.getFechaHoraDevolucionReal());
        assertEquals(AHORA, resultado.getFechaHoraFinUso());
        assertEquals(AHORA, resultado.getFechaHoraCierreDetalle());
        assertEquals(new BigDecimal("5.00"), resultado.getSubtotalRealUsd());
        assertEquals(new BigDecimal("7650.00"), resultado.getSubtotalRealArs());
        assertSame(destino, bicicleta.getUbicacionActual());
        assertEquals("DISPONIBLE", bicicleta.getEstado().getCodigo());
        assertEquals("CLIENTE", detalle.getOrigenCierre());
        assertEquals("EN_PROGRESO", alquiler.getEstadoContratacion().getCodigo());
        assertNull(alquiler.getFechaHoraCierre());
        assertNull(alquiler.getCostoFinalArs());
        ArgumentCaptor<HistorialEstadoDetalle> historia = ArgumentCaptor.forClass(HistorialEstadoDetalle.class);
        verify(historiaDetalleRepository).save(historia.capture());
        assertTrue(historia.getValue().getObservaciones().contains(demora ? "con demora" : "dentro del tiempo"));
        assertEquals(AHORA, historia.getValue().getFechaHoraCambio());
        assertSame(actor, historia.getValue().getActorUsuario());
        verify(detalleRepository).flush();
        verifyNoInteractions(pagoRepository, reembolsoRepository, cotizacionDolarService);
    }

    @Test
    void ultimaDevolucionCierraUsoYDelegaLiquidacionSinInventarPagos() {
        pendiente.setEstado(new EstadoAlquilerDetalle("VENCIDO"));
        prepararDevolucion("DISPONIBLE");
        when(estadoAlquilerRepository.findByCodigo("FINALIZADO"))
                .thenReturn(Optional.of(new EstadoAlquiler("FINALIZADO")));

        doAnswer(invocacion -> {
            Alquiler recibido = invocacion.getArgument(0);
            assertSame(alquiler, recibido);
            assertEquals("FINALIZADO", recibido.getEstadoContratacion().getCodigo());
            assertEquals(AHORA, recibido.getFechaHoraCierre());
            return true;
        }).when(liquidacionAlquilerService).liquidarSiCorresponde(alquiler);

        servicio.devolverBicicleta(7, 31, solicitud);

        verify(liquidacionAlquilerService).liquidarSiCorresponde(alquiler);
        verifyNoInteractions(pagoRepository, reembolsoRepository);
    }

    @Test
    void calculaArsSinConvertirElUsdYaRedondeadoYDescartaSoloLaFraccionDelIntervalo() {
        fijarReloj(RETIRO.plusSeconds(1).plusNanos(900_000_000));
        detalle.setPrecioPorHoraAplicadoUsd(new BigDecimal("1.00"));
        alquiler.setCotizacionArsPorUsd(new BigDecimal("1000.00"));
        prepararDevolucion("DISPONIBLE");
        AlquilerDetalleDTO resultado = servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals(new BigDecimal("0.00"), resultado.getSubtotalRealUsd());
        assertEquals(new BigDecimal("0.28"), resultado.getSubtotalRealArs());
    }

    @Test
    void usoDeMenosDeUnSegundoNoTieneMinimoComercial() {
        fijarReloj(RETIRO.plusNanos(999_000_000));
        prepararDevolucion("DISPONIBLE");
        AlquilerDetalleDTO resultado = servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals(new BigDecimal("0.00"), resultado.getSubtotalRealUsd());
        assertEquals(new BigDecimal("0.00"), resultado.getSubtotalRealArs());
        assertEquals("DEVUELTO", resultado.getEstado());
    }

    @Test
    void fallaGeneraUnaCorrectivaCreadaSinTecnicoYConAutorRealDelReporte() {
        solicitud.setFallaReportada(true);
        solicitud.setDescripcionFalla("  El freno no responde.  ");
        prepararOrdenNueva("CORRECTIVO");
        prepararDevolucion("MANTENIMIENTO");
        servicio.devolverBicicleta(7, 31, solicitud);
        assertTrue(detalle.isFallaReportada());
        assertEquals("El freno no responde.", detalle.getDescripcionFalla());
        assertSame(actor, detalle.getReportadoPor());
        assertEquals(AHORA, detalle.getFechaReporteFalla());
        OrdenMantenimiento orden = detalle.getOrdenMantenimiento();
        assertEquals("CORRECTIVO", orden.getTipoMantenimiento().getCodigo());
        assertEquals("CREADA", orden.getEstado().getCodigo());
        assertNull(orden.getTecnico());
        assertEquals(AHORA, orden.getFechaCreacion());
        assertEquals(new BigDecimal("7650.00"), detalle.getSubtotalRealArs());
        ArgumentCaptor<HistorialOrdenMantenimiento> historia = ArgumentCaptor.forClass(HistorialOrdenMantenimiento.class);
        verify(historiaOrdenRepository).save(historia.capture());
        assertEquals("SISTEMA", historia.getValue().getOrigen());
        assertNull(historia.getValue().getActorUsuario());
        assertEquals(AHORA, historia.getValue().getFechaHoraCambio());
    }

    @Test
    void ordenAbiertaSeReutilizaSinCambiarSuTipoYNoSeBorraUnaFallaPrevia() {
        detalle.setFallaReportada(true);
        detalle.setDescripcionFalla("Reporte previo");
        detalle.setFechaReporteFalla(RETIRO.plusMinutes(5));
        detalle.setReportadoPor(actor);
        OrdenMantenimiento abierta = new OrdenMantenimiento(bicicleta,
                new TipoMantenimiento("PREVENTIVO", "Revisión preventiva"), new EstadoOrdenMantenimiento("PAUSADA"));
        when(ordenRepository.buscarAbiertasPorBicicleta(101)).thenReturn(List.of(abierta));
        prepararDevolucion("MANTENIMIENTO");
        servicio.devolverBicicleta(7, 31, solicitud);
        assertSame(abierta, detalle.getOrdenMantenimiento());
        assertEquals("PREVENTIVO", abierta.getTipoMantenimiento().getCodigo());
        assertEquals("Reporte previo", detalle.getDescripcionFalla());
        assertEquals(RETIRO.plusMinutes(5), detalle.getFechaReporteFalla());
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void preventivaVenceEnLaFechaCalendarioYGeneraOrdenSinFallaInventada() {
        bicicleta.setFechaAlta(AHORA.toLocalDate().minusMonths(3));
        prepararOrdenNueva("PREVENTIVO");
        prepararDevolucion("MANTENIMIENTO");
        servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals("PREVENTIVO", detalle.getOrdenMantenimiento().getTipoMantenimiento().getCodigo());
        assertFalse(detalle.isFallaReportada());
    }

    @Test
    void ultimaPreventivaAptaReemplazaElAltaComoReferencia() {
        bicicleta.setFechaAlta(AHORA.toLocalDate().minusYears(2));
        when(ordenRepository.buscarUltimaPreventivaApta(101)).thenReturn(AHORA.minusMonths(1));
        prepararDevolucion("DISPONIBLE");
        servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals("DISPONIBLE", bicicleta.getEstado().getCodigo());
        verify(ordenRepository, never()).save(any());
    }

    @Test
    void incidenciaEconomicaPendientePermiteRecibirPeroNoFijaUnCostoDefinitivo() {
        Incidencia incidencia = new Incidencia();
        incidencia.setAlquilerDetalle(detalle);
        incidencia.setTipo("AVERIA");
        incidencia.setRequiereRevisionEconomica(true);
        when(incidenciaRepository.findByAlquilerDetalleIdOrderByIdAsc(31)).thenReturn(List.of(incidencia));
        prepararOrdenNueva("CORRECTIVO");
        prepararDevolucion("MANTENIMIENTO");
        servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals("DEVUELTO", detalle.getEstado().getCodigo());
        assertEquals(AHORA, detalle.getFechaHoraFinUso());
        assertEquals(AHORA, detalle.getFechaHoraDevolucionReal());
        assertNull(detalle.getSubtotalRealArs());
        assertNull(detalle.getSubtotalRealUsd());
        assertNull(alquiler.getFechaHoraLiquidacion());
        assertEquals("PENDIENTE", incidencia.getEstadoRevision());
    }

    @Test
    void corteValidadoConservaLaRecepcionRealPeroCobraSoloHastaElCorte() {
        Incidencia incidencia = new Incidencia();
        incidencia.setAlquilerDetalle(detalle);
        incidencia.setTipo("AVERIA");
        incidencia.setEstadoRevision("VALIDADA");
        incidencia.setFechaHoraCorteValidado(RETIRO.plusMinutes(10));
        ReflectionTestUtils.setField(incidencia, "fechaHoraRegistro", RETIRO.plusMinutes(11));
        incidencia.setFechaHoraDecision(RETIRO.plusMinutes(20));
        incidencia.setResueltoPor(actor);
        detalle.setIncidenciaCorte(incidencia);
        when(incidenciaRepository.findByAlquilerDetalleIdOrderByIdAsc(31)).thenReturn(List.of(incidencia));
        prepararOrdenNueva("CORRECTIVO");
        prepararDevolucion("MANTENIMIENTO");
        servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals(RETIRO.plusMinutes(10), detalle.getFechaHoraFinUso());
        assertEquals(AHORA, detalle.getFechaHoraDevolucionReal());
        assertEquals(new BigDecimal("2550.00"), detalle.getSubtotalRealArs());
        assertEquals("DEVUELTO", detalle.getEstado().getCodigo());
    }

    @Test
    void sinReferenciaDeAltaRequierePreventivaInicialNoUnaFechaInventada() {
        bicicleta.setFechaAlta(null);
        prepararOrdenNueva("PREVENTIVO");
        prepararDevolucion("MANTENIMIENTO");
        servicio.devolverBicicleta(7, 31, solicitud);
        assertNull(bicicleta.getFechaAlta());
        assertEquals("PREVENTIVO", detalle.getOrdenMantenimiento().getTipoMantenimiento().getCodigo());
    }

    @Test
    void noInventaUnaReferenciaCuandoHayDosOrdenesAbiertas() {
        prepararAcceso();
        when(bicicletaRepository.buscarActivaPorIdConBloqueo(101)).thenReturn(Optional.of(bicicleta));
        when(estacionRepository.buscarActivaPorIdConBloqueo(200)).thenReturn(Optional.of(destino));
        when(estadoDetalleRepository.findByCodigo("DEVUELTO"))
                .thenReturn(Optional.of(new EstadoAlquilerDetalle("DEVUELTO")));
        when(ordenRepository.buscarAbiertasPorBicicleta(101))
                .thenReturn(List.of(new OrdenMantenimiento(), new OrdenMantenimiento()));
        assertThrows(ConfiguracionSistemaException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        assertNull(detalle.getFechaHoraDevolucionReal());
        verifyNoInteractions(historiaDetalleRepository, historiaOrdenRepository);
    }

    @Test
    void reintentoNoTocaLaBicicletaNiBorraElReporteAunqueYaEsteEnOtroAlquiler() {
        prepararAcceso();
        detalle.setEstado(new EstadoAlquilerDetalle("DEVUELTO"));
        detalle.setEstacionDevolucion(destino);
        detalle.setFechaHoraDevolucionReal(AHORA);
        detalle.setFallaReportada(true);
        detalle.setDescripcionFalla("No borrar");
        alquiler.setEstadoContratacion(new EstadoAlquiler("FINALIZADO"));
        AlquilerDetalleDTO resultado = servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals(AHORA, resultado.getFechaHoraDevolucionReal());
        assertTrue(resultado.isFallaReportada());
        assertEquals("No borrar", detalle.getDescripcionFalla());
        verifyNoInteractions(bicicletaRepository, estacionRepository, ordenRepository,
                historiaDetalleRepository, historiaOrdenRepository, trazabilidad);
        verify(detalleRepository, never()).flush();
    }

    @Test
    void reintentoConOtroDestinoNoTrasladaLaUnidad() {
        prepararAcceso();
        detalle.setEstado(new EstadoAlquilerDetalle("DEVUELTO"));
        detalle.setEstacionDevolucion(destino);
        solicitud.setEstacionDevolucionId(300);
        assertThrows(IllegalStateException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        verifyNoInteractions(bicicletaRepository, estacionRepository);
    }

    @Test
    void reintentoNoPuedeIntroducirUnReporteNuevoSilenciosamente() {
        prepararAcceso();
        detalle.setEstado(new EstadoAlquilerDetalle("DEVUELTO"));
        detalle.setEstacionDevolucion(destino);
        solicitud.setFallaReportada(true);
        solicitud.setDescripcionFalla("Reporte distinto");
        assertThrows(IllegalStateException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        assertFalse(detalle.isFallaReportada());
        verifyNoInteractions(bicicletaRepository, ordenRepository);
    }

    @Test
    void adminVigentePuedeRecibirLoAjenoSinNecesitarRolCliente() {
        actor.getUsuarioRoles().clear();
        agregarRol("ADMIN", 2);
        autenticar("ADMIN");
        Usuario titular = new Usuario();
        titular.setId(9);
        alquiler.setUsuario(titular);
        prepararDevolucion("DISPONIBLE");
        servicio.devolverBicicleta(7, 31, solicitud);
        assertEquals("ADMIN", detalle.getOrigenCierre());
    }

    @Test
    void clienteNoPuedeDevolverLoAjeno() {
        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL)).thenReturn(Optional.of(actor));
        when(alquilerRepository.buscarPorIdConBloqueo(7)).thenReturn(Optional.of(alquiler));
        Usuario otro = new Usuario();
        otro.setId(9);
        alquiler.setUsuario(otro);
        assertThrows(AccessDeniedException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        verifyNoInteractions(detalleRepository, bicicletaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TECNICO", "ADMIN_REVOCADO", "INACTIVO", "SIN_SESION"})
    void rechazaSesionOCuentaSinPermisoVigente(String caso) {
        if ("SIN_SESION".equals(caso)) {
            SecurityContextHolder.clearContext();
        } else {
            when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL)).thenReturn(Optional.of(actor));
            if ("INACTIVO".equals(caso)) actor.setActivo(false);
            else autenticar("TECNICO".equals(caso) ? "TECNICO" : "ADMIN");
        }
        assertThrows(AccessDeniedException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        verifyNoInteractions(alquilerRepository, detalleRepository, bicicletaRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESERVADO", "PENDIENTE_RETIRO", "VENCIDO", "CANCELADO", "CERRADO_POR_INCIDENCIA"})
    void rechazaDetallesSinUsoAbierto(String estado) {
        prepararAcceso();
        detalle.setEstado(new EstadoAlquilerDetalle(estado));
        assertThrows(IllegalStateException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        verifyNoInteractions(bicicletaRepository, historiaDetalleRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INTERNA", "ELIMINADA", "LLENA"})
    void destinoNoValidoNoProduceUnaRecepcionExitosa(String caso) {
        prepararAcceso();
        when(bicicletaRepository.buscarActivaPorIdConBloqueo(101)).thenReturn(Optional.of(bicicleta));
        if ("ELIMINADA".equals(caso)) {
            when(estacionRepository.buscarActivaPorIdConBloqueo(200)).thenReturn(Optional.empty());
        } else {
            destino.setEsPublica(!"INTERNA".equals(caso));
            when(estacionRepository.buscarActivaPorIdConBloqueo(200)).thenReturn(Optional.of(destino));
            if ("LLENA".equals(caso)) when(bicicletaRepository.countByUbicacionActual(destino)).thenReturn(2L);
        }
        assertThrows(IllegalStateException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        assertNull(detalle.getFechaHoraDevolucionReal());
        assertNull(bicicleta.getUbicacionActual());
        verifyNoInteractions(historiaDetalleRepository, trazabilidad, ordenRepository);
    }

    @Test
    void detalleFueraDelAlquilerNoSePuedeRecibir() {
        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL)).thenReturn(Optional.of(actor));
        when(alquilerRepository.buscarPorIdConBloqueo(7)).thenReturn(Optional.of(alquiler));
        when(detalleRepository.buscarPorAlquilerYDetalleConBloqueo(7, 31)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
    }

    @Test
    void faltaDeTarifaCongeladaEsFalloInternoNoErrorDelCliente() {
        prepararAcceso();
        detalle.setPrecioPorHoraAplicadoUsd(null);
        assertThrows(ConfiguracionSistemaException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        verifyNoInteractions(bicicletaRepository, historiaDetalleRepository);
    }

    @Test
    void dtoIncompletoSeRechazaTambienAlInvocarElServicioDirectamente() {
        solicitud.setFallaReportada(true);
        assertThrows(IllegalArgumentException.class, () -> servicio.devolverBicicleta(7, 31, solicitud));
        verifyNoInteractions(usuarioRepository, alquilerRepository);
    }

    private void prepararAcceso() {
        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL)).thenReturn(Optional.of(actor));
        when(alquilerRepository.buscarPorIdConBloqueo(7)).thenReturn(Optional.of(alquiler));
        when(detalleRepository.buscarPorAlquilerYDetalleConBloqueo(7, 31)).thenReturn(Optional.of(detalle));
    }

    private void prepararDevolucion(String estadoBicicleta) {
        prepararAcceso();
        when(bicicletaRepository.buscarActivaPorIdConBloqueo(101)).thenReturn(Optional.of(bicicleta));
        when(estacionRepository.buscarActivaPorIdConBloqueo(200)).thenReturn(Optional.of(destino));
        when(estadoDetalleRepository.findByCodigo("DEVUELTO"))
                .thenReturn(Optional.of(new EstadoAlquilerDetalle("DEVUELTO")));
        when(estadoBicicletaRepository.findByCodigo(estadoBicicleta))
                .thenReturn(Optional.of(new EstadoBicicleta(estadoBicicleta)));
        when(detalleRepository.buscarPorAlquiler(7)).thenReturn(List.of(detalle, pendiente));
    }

    private void prepararOrdenNueva(String tipo) {
        when(tipoOrdenRepository.findByCodigo(tipo)).thenReturn(Optional.of(new TipoMantenimiento(tipo, "Revisión QA")));
        when(estadoOrdenRepository.findByCodigo("CREADA"))
                .thenReturn(Optional.of(new EstadoOrdenMantenimiento("CREADA")));
        when(ordenRepository.save(any(OrdenMantenimiento.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void agregarRol(String codigo, int id) {
        Rol rol = new Rol(codigo);
        ReflectionTestUtils.setField(rol, "id", id);
        actor.getUsuarioRoles().add(new UsuarioRol(actor, rol));
    }

    private void autenticar(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                EMAIL, null, AuthorityUtils.createAuthorityList(roles)));
    }

    private void fijarReloj(LocalDateTime ahora) {
        ReflectionTestUtils.setField(servicio, "reloj", Clock.fixed(ahora.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }
}
