package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.CotizacionDolarDTO;
import DSII.TurismoSustentable.dto.CrearAlquilerDTO;
import DSII.TurismoSustentable.exceptions.CotizacionNoDisponibleException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrearReservaServiceTest {

    private static final String EMAIL = "reserva.qa@example.com";

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 10, 12, 0);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BicicletaRepository bicicletaRepository;

    @Mock
    private EstacionRepository estacionRepository;

    @Mock
    private TipoBicicletaRepository tipoBicicletaRepository;

    @Mock
    private EstadoBicicletaRepository estadoBicicletaRepository;

    @Mock
    private EstadoAlquilerRepository estadoAlquilerRepository;

    @Mock
    private EstadoAlquilerDetalleRepository estadoAlquilerDetalleRepository;

    @Mock
    private AlquilerRepository alquilerRepository;

    @Mock
    private AlquilerDetalleRepository alquilerDetalleRepository;

    @Mock
    private HistorialEstadoDetalleRepository historialEstadoDetalleRepository;

    @Mock
    private CotizacionDolarService cotizacionDolarService;

    @Mock
    private TrazabilidadInventarioService trazabilidadInventario;

    @Mock
    private CorreoService correoService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private AlquilerService servicio;

    private ValidatorFactory validatorFactory;
    private SimpleTransactionStatus transaccion;

    private Usuario usuario;
    private Estacion estacion;
    private TipoBicicleta montana;
    private TipoBicicleta paseo;
    private Bicicleta bicicleta1;
    private Bicicleta bicicleta2;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();

        validatorFactory =
                Validation.buildDefaultValidatorFactory();

        ReflectionTestUtils.setField(
                servicio, "validator",
                validatorFactory.getValidator()
        );

        ReflectionTestUtils.setField(
                servicio, "calculadoraAlquiler",
                new CalculadoraAlquiler()
        );

        ReflectionTestUtils.setField(
                servicio, "reloj",
                Clock.fixed(
                        Instant.parse("2026-11-10T12:00:00Z"),
                        ZoneOffset.UTC
                )
        );

        ReflectionTestUtils.setField(
                servicio, "maxBicicletasPorContratacion", 10
        );

        transaccion = new SimpleTransactionStatus();

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);
        asignarRoles("CLIENTE");

        estacion = new Estacion(
                "Plaza QA", "Calle QA", 10, 0.0, 0.0
        );
        estacion.setEsPublica(true);
        ReflectionTestUtils.setField(estacion, "id", 1);

        montana = new TipoBicicleta(
                "MONTANA", new BigDecimal("10.00"), 3
        );
        paseo = new TipoBicicleta(
                "PASEO", new BigDecimal("8.00"), 2
        );

        ReflectionTestUtils.setField(montana, "id", 1);
        ReflectionTestUtils.setField(paseo, "id", 2);

        bicicleta1 = new Bicicleta(
                "QA-01", montana,
                new EstadoBicicleta("DISPONIBLE"), estacion
        );
        bicicleta1.setId(1);

        bicicleta2 = new Bicicleta(
                "QA-02", paseo,
                new EstadoBicicleta("DISPONIBLE"), estacion
        );
        bicicleta2.setId(2);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL,
                        null,
                        List.of(new SimpleGrantedAuthority("CLIENTE"))
                )
        );
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
        validatorFactory.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reservaDosBicicletasYConservaCondiciones(
            boolean tambienEsAdmin
    ) {
        if (tambienEsAdmin) {
            asignarRoles("ADMIN", "CLIENTE");

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            EMAIL,
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("ADMIN"),
                                    new SimpleGrantedAuthority("CLIENTE")
                            )
                    )
            );
        }

        prepararReservaCompleta();

        Integer id = servicio.crearReserva(
                solicitud(List.of(2, 1), 1, 2700)
        );

        assertEquals(Integer.valueOf(50), id);

        ArgumentCaptor<Alquiler> cabeceraCaptor =
                ArgumentCaptor.forClass(Alquiler.class);

        verify(alquilerRepository).save(cabeceraCaptor.capture());

        Alquiler cabecera = cabeceraCaptor.getValue();

        assertSame(usuario, cabecera.getUsuario());
        assertSame(estacion, cabecera.getUbicacionRetiro());
        assertEquals(
                "RESERVADO",
                cabecera.getEstadoContratacion().getCodigo()
        );

        assertEquals(AHORA, cabecera.getFechaHoraCreacion());
        assertEquals(
                AHORA.plusMinutes(15),
                cabecera.getFechaHoraVencimientoReserva()
        );

        assertEquals(
                new BigDecimal("1530.00"),
                cabecera.getCotizacionArsPorUsd()
        );
        assertEquals("Fuente QA", cabecera.getFuenteCotizacion());
        assertEquals(
                AHORA,
                cabecera.getFechaHoraAplicacionCotizacion()
        );
        assertEquals(
                LocalDateTime.of(2026, 11, 10, 11, 55),
                cabecera.getFechaHoraActualizacionFuente()
        );

        assertEquals(
                new BigDecimal("13.50"),
                cabecera.getCostoEstimadoUsd()
        );
        assertEquals(
                new BigDecimal("20655.00"),
                cabecera.getCostoEstimadoArs()
        );

        assertNull(cabecera.getCostoFinalUsd());
        assertNull(cabecera.getCostoFinalArs());
        assertNull(cabecera.getFechaInicio());
        assertNull(cabecera.getFechaHoraLiquidacion());

        ArgumentCaptor<AlquilerDetalle> detalleCaptor =
                ArgumentCaptor.forClass(AlquilerDetalle.class);

        verify(alquilerDetalleRepository, times(2))
                .save(detalleCaptor.capture());

        List<AlquilerDetalle> detalles =
                detalleCaptor.getAllValues();

        assertSame(bicicleta1, detalles.get(0).getBicicleta());
        assertSame(bicicleta2, detalles.get(1).getBicicleta());

        for (AlquilerDetalle detalle : detalles) {
            assertSame(cabecera, detalle.getAlquiler());
            assertEquals(
                    "RESERVADO", detalle.getEstado().getCodigo()
            );
            assertEquals(
                    Integer.valueOf(2700),
                    detalle.getDuracionEstimadaSegundos()
            );
            assertNull(detalle.getFechaHoraRetiro());
            assertNull(detalle.getFechaHoraDevolucionReal());
            assertNull(detalle.getSubtotalRealUsd());
            assertNull(detalle.getSubtotalRealArs());
        }

        assertSame(
                montana, detalles.get(0).getTipoBicicletaContratado()
        );
        assertSame(
                paseo, detalles.get(1).getTipoBicicletaContratado()
        );

        assertEquals(
                new BigDecimal("7.50"),
                detalles.get(0).getSubtotalEstimadoUsd()
        );
        assertEquals(
                new BigDecimal("11475.00"),
                detalles.get(0).getSubtotalEstimadoArs()
        );
        assertEquals(
                new BigDecimal("6.00"),
                detalles.get(1).getSubtotalEstimadoUsd()
        );
        assertEquals(
                new BigDecimal("9180.00"),
                detalles.get(1).getSubtotalEstimadoArs()
        );

        montana.setPrecioPorHoraUsd(new BigDecimal("99.00"));
        paseo.setPrecioPorHoraUsd(new BigDecimal("88.00"));

        assertEquals(
                new BigDecimal("10.00"),
                detalles.get(0).getPrecioPorHoraAplicadoUsd()
        );
        assertEquals(
                new BigDecimal("8.00"),
                detalles.get(1).getPrecioPorHoraAplicadoUsd()
        );

        ArgumentCaptor<HistorialEstadoDetalle> historialCaptor =
                ArgumentCaptor.forClass(HistorialEstadoDetalle.class);

        verify(historialEstadoDetalleRepository, times(2))
                .save(historialCaptor.capture());

        for (HistorialEstadoDetalle historial
                : historialCaptor.getAllValues()) {
            assertTrue(detalles.contains(historial.getAlquilerDetalle()));
            assertNull(historial.getEstadoAnterior());
            assertEquals(
                    "RESERVADO",
                    historial.getEstadoNuevo().getCodigo()
            );
            assertSame(usuario, historial.getActorUsuario());
            assertEquals("USUARIO", historial.getOrigen());
        }

        for (Bicicleta bicicleta : List.of(bicicleta1, bicicleta2)) {
            assertEquals(
                    "RESERVADA", bicicleta.getEstado().getCodigo()
            );
            assertSame(estacion, bicicleta.getUbicacionActual());

            verify(bicicletaRepository).save(bicicleta);
            verify(trazabilidadInventario).estado(
                    eq(bicicleta),
                    argThat(anterior ->
                            "DISPONIBLE".equals(anterior.getCodigo())
                    ),
                    eq(usuario),
                    eq("Reserva del alquiler 50.")
            );
        }

        var orden = inOrder(
                cotizacionDolarService,
                transactionManager,
                usuarioRepository,
                bicicletaRepository
        );

        orden.verify(cotizacionDolarService).obtenerCotizacion();
        orden.verify(transactionManager)
                .getTransaction(any(TransactionDefinition.class));
        orden.verify(usuarioRepository)
                .buscarActivoPorEmailConBloqueo(EMAIL);
        orden.verify(bicicletaRepository)
                .buscarActivaPorIdConBloqueo(1);
        orden.verify(bicicletaRepository)
                .buscarActivaPorIdConBloqueo(2);

        verify(transactionManager).commit(transaccion);
        verify(transactionManager, never()).rollback(any());

        verifyNoInteractions(correoService);

        verify(trazabilidadInventario, never()).administracion(
                any(), anyString(), anyInt(),
                anyString(), any(), any(), any()
        );
    }

    @ParameterizedTest
    @MethodSource("solicitudesInvalidas")
    void rechazaDatosInvalidosAntesDeConsultarServicios(
            List<Integer> ids,
            Integer estacionId,
            Integer duracion
    ) {
        assertThrows(
                ConstraintViolationException.class,
                () -> servicio.crearReserva(
                        solicitud(ids, estacionId, duracion)
                )
        );

        verifyNoInteractions(
                usuarioRepository,
                cotizacionDolarService,
                transactionManager,
                bicicletaRepository,
                estacionRepository
        );

        comprobarSinEscrituras();
    }

    static Stream<Arguments> solicitudesInvalidas() {
        return Stream.of(
                Arguments.of(List.of(1, 1), 1, 2700),
                Arguments.of(List.of(), 1, 2700),
                Arguments.of(List.of(0), 1, 2700),
                Arguments.of(Arrays.asList(1, null), 1, 2700),
                Arguments.of(List.of(1), null, 2700),
                Arguments.of(List.of(1), 1, 0)
        );
    }

    @Test
    void rechazaMasDeDiezBicicletas() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crearReserva(
                        solicitud(
                                List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                                1,
                                2700
                        )
                )
        );

        verifyNoInteractions(
                usuarioRepository,
                cotizacionDolarService,
                transactionManager,
                bicicletaRepository
        );

        comprobarSinEscrituras();
    }

    @Test
    void rechazaUsuarioSinSesion() {
        SecurityContextHolder.clearContext();

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.crearReserva(
                        solicitud(List.of(1), 1, 2700)
                )
        );

        verifyNoInteractions(
                usuarioRepository,
                cotizacionDolarService,
                transactionManager,
                bicicletaRepository
        );

        comprobarSinEscrituras();
    }

    @Test
    void rechazaSiClienteFueRetiradoDeLaCuenta() {
        prepararUsuario();
        asignarRoles("ADMIN");

        // La sesión conserva CLIENTE, pero en BD ya no lo tiene.
        assertThrows(
                AccessDeniedException.class,
                () -> servicio.crearReserva(
                        solicitud(List.of(1), 1, 2700)
                )
        );

        verifyNoInteractions(
                bicicletaRepository,
                estacionRepository,
                tipoBicicletaRepository
        );

        comprobarSinEscrituras();

        verify(transactionManager).rollback(transaccion);
        verify(transactionManager, never()).commit(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "RESERVADA",
            "EN_USO",
            "MANTENIMIENTO",
            "INACTIVA",
            "DE_BAJA",
            "EXTRAVIADA"
    })
    void rechazaBicicletaNoDisponible(String codigoEstado) {
        prepararUsuario();

        bicicleta1.setEstado(new EstadoBicicleta(codigoEstado));

        when(bicicletaRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(bicicleta1));

        assertThrows(
                IllegalStateException.class,
                () -> servicio.crearReserva(
                        solicitud(List.of(1), 1, 2700)
                )
        );

        assertEquals(codigoEstado, bicicleta1.getEstado().getCodigo());
        assertSame(estacion, bicicleta1.getUbicacionActual());

        comprobarSinEscrituras();

        verifyNoInteractions(
                estacionRepository, tipoBicicletaRepository
        );
        verify(transactionManager).rollback(transaccion);
    }

    @Test
    void rechazaBicicletaDeOtraEstacion() {
        prepararUsuario();

        when(bicicletaRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(bicicleta1));

        assertThrows(
                IllegalStateException.class,
                () -> servicio.crearReserva(
                        solicitud(List.of(1), 2, 2700)
                )
        );

        comprobarSinEscrituras();
        verifyNoInteractions(estacionRepository);
        verify(transactionManager).rollback(transaccion);
    }

    @Test
    void rechazaEstacionInterna() {
        prepararUsuario();

        when(bicicletaRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(bicicleta1));

        estacion.setEsPublica(false);

        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(estacion));

        assertThrows(
                IllegalStateException.class,
                () -> servicio.crearReserva(
                        solicitud(List.of(1), 1, 2700)
                )
        );

        comprobarSinEscrituras();
        verifyNoInteractions(tipoBicicletaRepository);
        verify(transactionManager).rollback(transaccion);
    }

    @Test
    void falloDeCotizacionNoIniciaTransaccionNiReserva() {
        when(usuarioRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(usuario));

        when(cotizacionDolarService.obtenerCotizacion())
                .thenThrow(
                        new CotizacionNoDisponibleException(
                                new IllegalStateException("Proveedor no disponible")
                        )
                );

        assertThrows(
                CotizacionNoDisponibleException.class,
                () -> servicio.crearReserva(
                        solicitud(List.of(1), 1, 2700)
                )
        );

        verifyNoInteractions(
                transactionManager,
                bicicletaRepository,
                estacionRepository,
                tipoBicicletaRepository
        );

        verify(usuarioRepository, never())
                .buscarActivoPorEmailConBloqueo(anyString());

        comprobarSinEscrituras();
    }

    @Test
    void falloDeHistorialSolicitaRollbackYNoCommit() {
        prepararReservaCompleta();

        when(historialEstadoDetalleRepository.save(
                any(HistorialEstadoDetalle.class)
        )).thenThrow(new IllegalStateException("Fallo al guardar historial"));

        assertThrows(
                IllegalStateException.class,
                () -> servicio.crearReserva(
                        solicitud(List.of(2, 1), 1, 2700)
                )
        );

        // El fallo ocurre después de solicitar estas escrituras.
        verify(alquilerRepository).save(any(Alquiler.class));
        verify(alquilerDetalleRepository)
                .save(any(AlquilerDetalle.class));

        verify(transactionManager).rollback(transaccion);
        verify(transactionManager, never()).commit(any());

        verify(bicicletaRepository, never())
                .save(any(Bicicleta.class));

        verifyNoInteractions(correoService, trazabilidadInventario);
    }

    private void prepararUsuario() {
        when(usuarioRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(usuario));

        when(cotizacionDolarService.obtenerCotizacion())
                .thenReturn(
                        new CotizacionDolarDTO(
                                new BigDecimal("1530.00"),
                                "Fuente QA",
                                OffsetDateTime.parse(
                                        "2026-11-10T08:55:00-03:00"
                                )
                        )
                );

        when(transactionManager.getTransaction(
                any(TransactionDefinition.class)
        )).thenReturn(transaccion);

        when(usuarioRepository.buscarActivoPorEmailConBloqueo(EMAIL))
                .thenReturn(Optional.of(usuario));
    }

    private void prepararReservaCompleta() {
        prepararUsuario();

        when(bicicletaRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(bicicleta1));
        when(bicicletaRepository.buscarActivaPorIdConBloqueo(2))
                .thenReturn(Optional.of(bicicleta2));

        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(estacion));

        when(tipoBicicletaRepository.buscarPorIdConBloqueo(1))
                .thenReturn(Optional.of(montana));
        when(tipoBicicletaRepository.buscarPorIdConBloqueo(2))
                .thenReturn(Optional.of(paseo));

        when(estadoAlquilerRepository.findByCodigo("RESERVADO"))
                .thenReturn(
                        Optional.of(new EstadoAlquiler("RESERVADO"))
                );

        when(estadoAlquilerDetalleRepository.findByCodigo("RESERVADO"))
                .thenReturn(
                        Optional.of(new EstadoAlquilerDetalle("RESERVADO"))
                );

        when(estadoBicicletaRepository.findByCodigo("RESERVADA"))
                .thenReturn(
                        Optional.of(new EstadoBicicleta("RESERVADA"))
                );

        when(alquilerRepository.save(any(Alquiler.class)))
                .thenAnswer(invocacion -> {
                    Alquiler alquiler = invocacion.getArgument(0);
                    alquiler.setId(50);
                    return alquiler;
                });

        when(alquilerDetalleRepository.save(any(AlquilerDetalle.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private CrearAlquilerDTO solicitud(
            List<Integer> ids,
            Integer estacionId,
            Integer duracion
    ) {
        CrearAlquilerDTO dto = new CrearAlquilerDTO();
        dto.setBicicletaIds(ids);
        dto.setEstacionRetiroId(estacionId);
        dto.setDuracionEstimadaSegundos(duracion);
        return dto;
    }

    private void asignarRoles(String... codigos) {
        usuario.getUsuarioRoles().clear();

        int idRol = 1;

        for (String codigo : codigos) {
            Rol rol = new Rol(codigo);
            ReflectionTestUtils.setField(rol, "id", idRol++);

            usuario.getUsuarioRoles().add(
                    new UsuarioRol(usuario, rol)
            );
        }
    }

    private void comprobarSinEscrituras() {
        verifyNoInteractions(
                alquilerRepository,
                alquilerDetalleRepository,
                historialEstadoDetalleRepository,
                trazabilidadInventario,
                correoService
        );

        verify(bicicletaRepository, never())
                .save(any(Bicicleta.class));
    }
}
