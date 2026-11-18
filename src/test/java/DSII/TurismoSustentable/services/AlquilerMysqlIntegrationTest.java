package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerCabeceraDTO;
import DSII.TurismoSustentable.dto.AlquilerDetalleDTO;
import DSII.TurismoSustentable.dto.CotizacionDolarDTO;
import DSII.TurismoSustentable.dto.CrearAlquilerDTO;
import DSII.TurismoSustentable.dto.DevolverBicicletaDTO;
import DSII.TurismoSustentable.dto.LiquidacionAlquilerDTO;
import DSII.TurismoSustentable.dto.ResultadoAdicionalDTO;
import DSII.TurismoSustentable.dto.ResultadoAnticipoDTO;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Disabled("Integración pausada para conservar los datos locales de demostración.")
@EnabledIfEnvironmentVariable(
        named = "TURISMO_TEST_MYSQL",
        matches = "true"
)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:mariadb://localhost:3306/turismo_sustentable?sslMode=disable",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.jpa.open-in-view=false",
                "alquiler.vencimientos.automaticos=false",
                "mantenimiento.preventivo.automatico=false",
                "pagos.simulados.saldo-inicial-ars=1000000.00",
                "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout=10"
        }
)
@ActiveProfiles("refactor")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AlquilerMysqlIntegrationTest {
    @Autowired private ReembolsoService reembolsoService;
    @Autowired private org.springframework.test.web.servlet.MockMvc http;
    @org.springframework.boot.web.server.LocalServerPort private int puertoWeb;

    private static final AtomicInteger DOCUMENTOS =
            new AtomicInteger(10000000);

    private static final String PROPIEDADES_PAGOS_MYSQL =
            "qa-pagos-mysql";

    @Autowired
    private AlquilerService alquilerService;

    @Autowired
    private AlquilerRepository alquilerRepository;

    @Autowired
    private AlquilerDetalleRepository alquilerDetalleRepository;

    @Autowired
    private BicicletaRepository bicicletaRepository;

    @Autowired
    private HistorialEstadoDetalleRepository historialDetalleRepository;

    @Autowired
    private HistorialEstadoBicicletaRepository historialBicicletaRepository;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ReembolsoRepository reembolsoRepository;

    @Autowired
    private HistorialEstadoPagoRepository historialPagoRepository;

    @Autowired
    private HistorialEstadoAlquilerRepository historialCabeceraRepository;

    @Autowired
    private HistorialEstadoReembolsoRepository historialReembolsoRepository;

    @Autowired
    private ConfigurableEnvironment entorno;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EstadisticasService estadisticasService;

    @PersistenceContext
    private EntityManager entityManager;

    @MockBean
    private CotizacionDolarService cotizacionDolarService;

    @MockBean
    private Clock reloj;

    @SpyBean
    private TrazabilidadInventarioService trazabilidadInventario;

    @SpyBean
    private LiquidacionAlquilerService liquidacionService;

    private TrazabilidadInventarioService trazabilidadEspiada;
    private LiquidacionAlquilerService liquidacionEspiada;
    private TransactionTemplate transaccion;

    private String email;
    private int usuarioId;
    private int estacionId;
    private int bicicleta1Id;
    private int bicicleta2Id;
    private Instant instantePagos;

    @BeforeEach
    void prepararDatosReales() {
        SecurityContextHolder.clearContext();

        trazabilidadEspiada =
                AopTestUtils.getUltimateTargetObject(
                        trazabilidadInventario
                );

        liquidacionEspiada =
                AopTestUtils.getUltimateTargetObject(liquidacionService);

        transaccion =
                new TransactionTemplate(transactionManager);

        int documento = DOCUMENTOS.incrementAndGet();
        email = "qa.mysql." + documento + "@example.com";

        // Esta transacción prepara y confirma los datos de prueba.
        transaccion.executeWithoutResult(estado -> {

            Persona persona = new Persona(
                    "Cliente",
                    "Prueba",
                    LocalDate.of(1990, 1, 1),
                    catalogo(Genero.class, "MASCULINO"),
                    catalogo(TipoDocumento.class, "DNI"),
                    String.valueOf(documento),
                    catalogo(Pais.class, "AR")
            );
            entityManager.persist(persona);

            Usuario usuario = new Usuario(
                    persona,
                    "qa.mysql." + documento,
                    email,
                    passwordEncoder.encode("PruebaMysql?1")
            );
            entityManager.persist(usuario);
            entityManager.flush();

            entityManager.persist(
                    new UsuarioRol(
                            usuario,
                            catalogo(Rol.class, "CLIENTE")
                    )
            );

            Estacion estacion = new Estacion(
                    "Plaza MySQL " + documento,
                    "Calle QA 100",
                    3,
                    new BigDecimal("-31.4"),
                    new BigDecimal("-64.2")
            );
            estacion.setEsPublica(true);
            entityManager.persist(estacion);

            TipoBicicleta montana =
                    catalogo(TipoBicicleta.class, "MONTANA");
            TipoBicicleta paseo =
                    catalogo(TipoBicicleta.class, "PASEO");

            montana.setPrecioPorHoraUsd(new BigDecimal("10.00"));
            paseo.setPrecioPorHoraUsd(new BigDecimal("8.00"));

            EstadoBicicleta disponible =
                    catalogo(EstadoBicicleta.class, "DISPONIBLE");

            Bicicleta bicicleta1 = new Bicicleta(
                    "MYSQL-" + documento + "-1",
                    montana,
                    disponible,
                    estacion
            );
            bicicleta1.setFechaAlta(LocalDate.now(ZoneOffset.UTC));
            entityManager.persist(bicicleta1);

            Bicicleta bicicleta2 = new Bicicleta(
                    "MYSQL-" + documento + "-2",
                    paseo,
                    disponible,
                    estacion
            );
            bicicleta2.setFechaAlta(LocalDate.now(ZoneOffset.UTC));
            entityManager.persist(bicicleta2);

            entityManager.flush();

            usuarioId = usuario.getId();
            estacionId = estacion.getId();
            bicicleta1Id = bicicleta1.getId();
            bicicleta2Id = bicicleta2.getId();
        });

        // MySQL conserva microsegundos; el reloj avanza solo en el caso tardío.
        instantePagos = Instant.now().truncatedTo(ChronoUnit.MICROS);
        when(reloj.instant()).thenReturn(instantePagos);
        when(reloj.getZone()).thenReturn(ZoneOffset.UTC);

        when(cotizacionDolarService.obtenerCotizacion())
                .thenReturn(
                        new CotizacionDolarDTO(
                                new BigDecimal("1530.00"),
                                "Cotización QA MySQL",
                                OffsetDateTime.now(ZoneOffset.UTC)
                                        .minusMinutes(1)
                        )
                );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("CLIENTE"))
                )
        );
    }

    @AfterEach
    void limpiarSesionYFalloSimulado() {
        SecurityContextHolder.clearContext();
        entorno.getPropertySources().remove(PROPIEDADES_PAGOS_MYSQL);

        if (trazabilidadEspiada != null) {
            reset(trazabilidadEspiada);
        }

        if (liquidacionEspiada != null) {
            reset(liquidacionEspiada);
        }
    }

    @Test
    void guardaReservaCompletaConHistoriasYUbicacion() {
        long[] antes = contarRegistros();

        Integer alquilerId =
                alquilerService.crearReserva(solicitud());

        assertNotNull(alquilerId);
        assertTrue(alquilerId > 0);

        long[] despues = contarRegistros();

        assertEquals(antes[0] + 1, despues[0]);
        assertEquals(antes[1] + 2, despues[1]);
        assertEquals(antes[2] + 2, despues[2]);
        assertEquals(antes[3] + 2, despues[3]);
        assertEquals(antes[4] + 1, despues[4]);

        var eventos = alquilerService.historialAlquiler(alquilerId);
        assertEquals(1, eventos.size());
        assertNull(eventos.get(0).estadoAnterior);
        assertEquals("RESERVADO", eventos.get(0).estadoNuevo);
        assertEquals(usuarioId, eventos.get(0).actorUsuarioId);
        assertEquals("USUARIO", eventos.get(0).origen);

        // Nueva transacción: no reutilizamos entidades de la reserva.
        transaccion.executeWithoutResult(estado -> {
            Alquiler alquiler = alquilerRepository
                    .findById(alquilerId)
                    .orElseThrow();

            assertEquals(
                    "RESERVADO",
                    alquiler.getEstadoContratacion().getCodigo()
            );
            assertEquals(usuarioId, alquiler.getUsuario().getId());
            assertEquals(
                    estacionId,
                    alquiler.getUbicacionRetiro().getId()
            );
            assertEquals(
                    alquiler.getFechaHoraCreacion().plusMinutes(15),
                    alquiler.getFechaHoraVencimientoReserva()
            );

            // JDBC nativo observa el DATETIME sin pasar por el conversor JPA.
            String fechaFisica = (String) entityManager.createNativeQuery(
                            "select cast(fecha_hora_creacion as char) from alquiler where id = :id")
                    .setParameter("id", alquilerId).getSingleResult();
            assertEquals(alquiler.getFechaHoraCreacion().minusHours(3),
                    LocalDateTime.parse(fechaFisica.replace(' ', 'T')));

            // El filtro JPQL recibe UTC y debe comparar contra la columna local, sin vencer antes.
            LocalDateTime venceUtc = alquiler.getFechaHoraVencimientoReserva();
            var pagina = org.springframework.data.domain.PageRequest.of(0, 100);
            assertFalse(alquilerRepository.buscarReservasVencidas(venceUtc.minusSeconds(1), pagina).contains(alquilerId));
            assertTrue(alquilerRepository.buscarReservasVencidas(venceUtc, pagina).contains(alquilerId));

            List<AlquilerDetalle> detalles =
                    alquilerDetalleRepository
                            .buscarPorAlquilerConDatos(alquilerId);

            assertEquals(2, detalles.size());

            for (AlquilerDetalle detalle : detalles) {
                assertEquals(
                        alquilerId.intValue(),
                        detalle.getAlquiler().getId()
                );
                assertEquals(
                        "RESERVADO",
                        detalle.getEstado().getCodigo()
                );
                assertEquals(
                        Integer.valueOf(2700),
                        detalle.getDuracionEstimadaSegundos()
                );
                assertNull(detalle.getFechaHoraRetiro());
                assertNull(detalle.getSubtotalRealUsd());
            }

            comprobarBicicletas("RESERVADA");
        });
    }

    @Test
    void consultaLoGuardadoSinRecalcularTarifasHistoricas() {
        Integer alquilerId =
                alquilerService.crearReserva(solicitud());

        // Cambia el catálogo después de confirmar el alquiler.
        transaccion.executeWithoutResult(estado -> {
            catalogo(TipoBicicleta.class, "MONTANA")
                    .setPrecioPorHoraUsd(new BigDecimal("99.00"));

            catalogo(TipoBicicleta.class, "PASEO")
                    .setPrecioPorHoraUsd(new BigDecimal("88.00"));
        });

        // No hay transacción de test ni sesión web envolvente.
        AlquilerCabeceraDTO resultado =
                alquilerService.obtenerAlquiler(alquilerId);

        assertEquals(alquilerId.intValue(), resultado.getId());
        assertEquals(2, resultado.getDetalles().size());

        assertEquals(
                new BigDecimal("13.50"),
                resultado.getCostoEstimadoUsd()
        );
        assertEquals(
                new BigDecimal("20655.00"),
                resultado.getCostoEstimadoArs()
        );
        assertEquals(
                0,
                new BigDecimal("1530").compareTo(
                        resultado.getCotizacionArsPorUsd()
                )
        );

        assertEquals(
                new BigDecimal("10.00"),
                resultado.getDetalles().get(0)
                        .getPrecioPorHoraAplicadoUsd()
        );
        assertEquals(
                new BigDecimal("8.00"),
                resultado.getDetalles().get(1)
                        .getPrecioPorHoraAplicadoUsd()
        );

        assertNull(resultado.getCostoFinalUsd());
        assertNull(resultado.getFechaHoraLiquidacion());

        verify(cotizacionDolarService, times(1))
                .obtenerCotizacion();
    }

    @Test
    void falloTrasEscriturasRealesRevierteTodaLaReserva() {
        long[] antes = contarRegistros();

        // Se guarda la historia real y se fuerza su envío a MySQL.
        // Después se provoca el fallo que debe revertir la operación.
        doAnswer(invocacion -> {
            invocacion.callRealMethod();
            entityManager.flush();

            throw new IllegalStateException(
                    "FALLO_MYSQL_QA_CONTROLADO"
            );
        }).when(trazabilidadEspiada).estado(
                any(Bicicleta.class),
                any(EstadoBicicleta.class),
                any(Usuario.class),
                anyString(),
                any(LocalDateTime.class)
        );

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> alquilerService.crearReserva(solicitud())
        );

        assertEquals(
                "FALLO_MYSQL_QA_CONTROLADO",
                error.getMessage()
        );

        // Consultas posteriores al rollback, desde otra transacción.
        assertArrayEquals(antes, contarRegistros());

        transaccion.executeWithoutResult(estado ->
                comprobarBicicletas("DISPONIBLE")
        );
    }

    @Test
    void listadoPropioMysqlFiltraUsuarioBorradoYOrdenaCabeceras() {
        int reservaId = alquilerService.crearReserva(solicitud());

        int finalizadoId =
                crearAlquilerFinalizadoListado(usuarioId, false);
        int borradoId =
                crearAlquilerFinalizadoListado(usuarioId, true);

        int usuarioAjenoId = crearUsuarioAjenoListado();
        int ajenoId =
                crearAlquilerFinalizadoListado(usuarioAjenoId, false);

        long[] antes = contarRegistros();
        clearInvocations(cotizacionDolarService);

        // Servicio real, fuera de una transacción de test.
        List<AlquilerCabeceraDTO> resultado =
                alquilerService.listarMisAlquileres();

        List<Integer> ids = idsListado(resultado);

        assertEquals(List.of(finalizadoId, reservaId), ids);
        assertFalse(ids.contains(borradoId));
        assertFalse(ids.contains(ajenoId));

        AlquilerCabeceraDTO finalizado = resultado.get(0);
        AlquilerCabeceraDTO reservado = resultado.get(1);

        assertEquals("DETALLES", finalizado.getModelo());
        assertEquals("FINALIZADO", finalizado.getEstado());
        assertEquals(1, finalizado.getDetalles().size());
        assertEquals(
                new BigDecimal("7.50"),
                finalizado.getCostoFinalUsd()
        );

        assertEquals("DETALLES", reservado.getModelo());
        assertEquals("RESERVADO", reservado.getEstado());
        assertEquals(2, reservado.getDetalles().size());

        // La fila borrada lógicamente sigue existiendo físicamente.
        transaccion.executeWithoutResult(estado ->
                assertTrue(
                        alquilerRepository.existsById(borradoId)
                )
        );

        assertArrayEquals(antes, contarRegistros());
        verifyNoInteractions(cotizacionDolarService);
    }

    @Test
    void listadoAdministrativoMysqlIncluyeAjenosPeroNoBorrados() {
        int reservaId = alquilerService.crearReserva(solicitud());

        int finalizadoId =
                crearAlquilerFinalizadoListado(usuarioId, false);

        int usuarioAjenoId = crearUsuarioAjenoListado();
        int ajenoId =
                crearAlquilerFinalizadoListado(usuarioAjenoId, false);

        int borradoPropioId =
                crearAlquilerFinalizadoListado(usuarioId, true);
        int borradoAjenoId =
                crearAlquilerFinalizadoListado(usuarioAjenoId, true);

        transaccion.executeWithoutResult(estado ->
                entityManager.persist(
                        new UsuarioRol(
                                entityManager.find(
                                        Usuario.class, usuarioId
                                ),
                                catalogo(Rol.class, "ADMIN")
                        )
                )
        );

        autenticarListado("CLIENTE", "ADMIN");

        long[] antes = contarRegistros();
        clearInvocations(cotizacionDolarService);

        List<Integer> generales = idsListado(
                alquilerService.listarAlquileresAdministracion()
        );

        assertTrue(generales.contains(reservaId));
        assertTrue(generales.contains(finalizadoId));
        assertTrue(generales.contains(ajenoId));
        assertFalse(generales.contains(borradoPropioId));
        assertFalse(generales.contains(borradoAjenoId));

        // Puede haber filas de otros métodos: no asumir tabla vacía.
        // Orden estrictamente descendente también descarta duplicados.
        for (int i = 1; i < generales.size(); i++) {
            assertTrue(
                    generales.get(i - 1) > generales.get(i),
                    "El listado debe ordenar por ID descendente."
            );
        }

        // Tener ADMIN no convierte la lista personal en una lista general.
        assertEquals(
                List.of(finalizadoId, reservaId),
                idsListado(alquilerService.listarMisAlquileres())
        );

        assertArrayEquals(antes, contarRegistros());
        verifyNoInteractions(cotizacionDolarService);
    }

    @Test
    void listadoPorLoteMysqlConservaDetallesOrdenYSnapshots() {
        Integer terceraBicicletaId =
                transaccion.execute(estado -> {
                    Bicicleta tercera = new Bicicleta(
                            "MYSQL-LISTA-" + usuarioId + "-3",
                            catalogo(TipoBicicleta.class, "MONTANA"),
                            catalogo(EstadoBicicleta.class, "DISPONIBLE"),
                            entityManager.find(Estacion.class, estacionId)
                    );
                    tercera.setFechaAlta(
                            LocalDate.now(ZoneOffset.UTC)
                    );

                    entityManager.persist(tercera);
                    entityManager.flush();
                    return tercera.getId();
                });

        int primeraReservaId =
                alquilerService.crearReserva(solicitud());

        CrearAlquilerDTO otraSolicitud = solicitud();
        otraSolicitud.setBicicletaIds(
                List.of(terceraBicicletaId)
        );

        int segundaReservaId =
                alquilerService.crearReserva(otraSolicitud);

        // El precio actual cambia, pero no el snapshot del alquiler.
        transaccion.executeWithoutResult(estado -> {
            catalogo(TipoBicicleta.class, "MONTANA")
                    .setPrecioPorHoraUsd(new BigDecimal("99.00"));
            catalogo(TipoBicicleta.class, "PASEO")
                    .setPrecioPorHoraUsd(new BigDecimal("88.00"));
        });

        long[] antes = contarRegistros();
        clearInvocations(cotizacionDolarService);

        List<AlquilerCabeceraDTO> resultado =
                alquilerService.listarMisAlquileres();

        assertEquals(
                List.of(segundaReservaId, primeraReservaId),
                idsListado(resultado)
        );

        AlquilerCabeceraDTO segunda = resultado.get(0);
        AlquilerCabeceraDTO primera = resultado.get(1);

        assertEquals(1, segunda.getDetalles().size());
        assertEquals(
                terceraBicicletaId.intValue(),
                segunda.getDetalles().get(0).getBicicletaId()
        );
        assertEquals(
                new BigDecimal("7.50"),
                segunda.getCostoEstimadoUsd()
        );
        assertEquals(
                new BigDecimal("11475.00"),
                segunda.getCostoEstimadoArs()
        );

        assertEquals(2, primera.getDetalles().size());
        assertEquals(
                bicicleta1Id,
                primera.getDetalles().get(0).getBicicletaId()
        );
        assertEquals(
                bicicleta2Id,
                primera.getDetalles().get(1).getBicicletaId()
        );
        assertTrue(
                primera.getDetalles().get(0).getId()
                        < primera.getDetalles().get(1).getId()
        );

        assertEquals(
                new BigDecimal("10.00"),
                primera.getDetalles().get(0)
                        .getPrecioPorHoraAplicadoUsd()
        );
        assertEquals(
                new BigDecimal("8.00"),
                primera.getDetalles().get(1)
                        .getPrecioPorHoraAplicadoUsd()
        );
        assertEquals(
                new BigDecimal("13.50"),
                primera.getCostoEstimadoUsd()
        );
        assertEquals(
                new BigDecimal("20655.00"),
                primera.getCostoEstimadoArs()
        );
        assertEquals(
                0,
                new BigDecimal("1530").compareTo(
                        primera.getCotizacionArsPorUsd()
                )
        );

        assertNull(primera.getCostoFinalUsd());
        assertNull(primera.getFechaHoraLiquidacion());

        assertArrayEquals(antes, contarRegistros());
        verifyNoInteractions(cotizacionDolarService);
    }

    @Test
    void permisosDeListadoMysqlUsanSesionRolVigenteYCuentaActiva() {
        // Sesión CLIENTE: el listado administrativo está prohibido.
        assertThrows(
                AccessDeniedException.class,
                () -> alquilerService.listarAlquileresAdministracion()
        );

        // Autoridad de sesión no respaldada por el rol vigente.
        autenticarListado("CLIENTE", "ADMIN");

        assertThrows(
                AccessDeniedException.class,
                () -> alquilerService.listarAlquileresAdministracion()
        );

        transaccion.executeWithoutResult(estado ->
                entityManager.persist(
                        new UsuarioRol(
                                entityManager.find(
                                        Usuario.class, usuarioId
                                ),
                                catalogo(Rol.class, "ADMIN")
                        )
                )
        );

        // La BD ya tiene ADMIN, pero la sesión todavía no:
        // la autorización del método debe impedir la llamada.
        autenticarListado("CLIENTE");

        assertThrows(
                AccessDeniedException.class,
                () -> alquilerService.listarAlquileresAdministracion()
        );

        autenticarListado("CLIENTE", "ADMIN");

        assertNotNull(
                alquilerService.listarAlquileresAdministracion()
        );

        // Simular retiro de la asignación creada por esta prueba.
        transaccion.executeWithoutResult(estado -> {
            UsuarioRol asignacion = entityManager.createQuery(
                            "select ur from UsuarioRol ur "
                                    + "where ur.usuario.id = :usuarioId "
                                    + "and ur.rol.codigo = :codigo",
                            UsuarioRol.class
                    )
                    .setParameter("usuarioId", usuarioId)
                    .setParameter("codigo", "ADMIN")
                    .getSingleResult();

            entityManager.remove(asignacion);
        });

        // La sesión conserva ADMIN, pero la BD ya no.
        assertThrows(
                AccessDeniedException.class,
                () -> alquilerService.listarAlquileresAdministracion()
        );

        // CLIENTE sigue vigente y permite consultar la lista propia.
        assertTrue(
                alquilerService.listarMisAlquileres().isEmpty()
        );

        transaccion.executeWithoutResult(estado ->
                entityManager.find(Usuario.class, usuarioId)
                        .setActivo(false)
        );

        assertThrows(
                AccessDeniedException.class,
                () -> alquilerService.listarMisAlquileres()
        );
        assertThrows(
                AccessDeniedException.class,
                () -> alquilerService.listarAlquileresAdministracion()
        );

        verifyNoInteractions(cotizacionDolarService);
    }

    @Test
    void anticipoMysqlGuardaRelacionesHistoriasYSinDuplicados() {
        int alquilerId = alquilerService.crearReserva(solicitud());
        String clave = UUID.randomUUID().toString();

        ResultadoAnticipoDTO inicio =
                pagoService.iniciarAnticipo(alquilerId, clave);

        assertEquals("CREADO", inicio.getEstadoPago());

        ResultadoAnticipoDTO aprobado =
                pagoService.resolverAnticipo(alquilerId, clave);

        assertEquals("APROBADO", aprobado.getEstadoPago());
        assertEquals(inicio.getPagoId(), aprobado.getPagoId());
        assertEquals(
                inicio.getPagoId(),
                pagoService.iniciarAnticipo(alquilerId, clave).getPagoId()
        );
        assertEquals(
                inicio.getPagoId(),
                pagoService.resolverAnticipo(alquilerId, clave).getPagoId()
        );

        comprobarAnticipoMysql(alquilerId, aprobado.getPagoId(), clave);

        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void rollbackDelBloqueFinancieroRevierteSusEscrituras() {
        int alquilerId = alquilerService.crearReserva(solicitud());
        String clave = UUID.randomUUID().toString();
        ResultadoAnticipoDTO inicio =
                pagoService.iniciarAnticipo(alquilerId, clave);

        // La resolución participa de esta unidad de trabajo externa explícita.
        TransactionTemplate bloque =
                new TransactionTemplate(transactionManager);
        bloque.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED
        );

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> bloque.executeWithoutResult(estado -> {
                    pagoService.resolverAnticipo(alquilerId, clave);
                    entityManager.flush();
                    throw new IllegalStateException(
                            "FALLO_FINANCIERO_MYSQL_QA"
                    );
                })
        );

        assertEquals("FALLO_FINANCIERO_MYSQL_QA", error.getMessage());

        // Nueva transacción después del rollback: no consultar entidades viejas.
        transaccion.executeWithoutResult(estado -> {
            Pago pago = pagoRepository.findById(inicio.getPagoId())
                    .orElseThrow();

            assertEquals("CREADO", pago.getEstadoPago().getCodigo());
            assertNull(pago.getFechaConfirmacion());
            assertNull(pago.getReferenciaExterna());
            assertEquals(
                    1,
                    historialPagoRepository
                            .findByPagoIdOrderByIdAsc(pago.getId()).size()
            );

            List<AlquilerDetalle> detalles = alquilerDetalleRepository
                    .buscarPorAlquilerConDatos(alquilerId);

            assertEquals(2, detalles.size());
            for (AlquilerDetalle detalle : detalles) {
                assertEquals("RESERVADO", detalle.getEstado().getCodigo());
                assertNull(detalle.getFechaHoraRetiro());
            }

            assertEquals(2, historiasDetalleMysql(alquilerId).size());
            assertTrue(reembolsoRepository
                    .findByPagoAlquilerIdOrderByIdAsc(alquilerId).isEmpty());
            comprobarBicicletas("RESERVADA");
        });

        assertEquals(
                new BigDecimal("1000000.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void solicitudesConcurrentesNoDuplicanPagoNiConfirmacion()
            throws Exception {
        int alquilerId = alquilerService.crearReserva(solicitud());
        String clave = UUID.randomUUID().toString();

        List<ResultadoAnticipoDTO> inicios = ejecutarDosOperaciones(
                () -> pagoService.iniciarAnticipo(alquilerId, clave)
        );

        assertEquals(inicios.get(0).getPagoId(), inicios.get(1).getPagoId());

        List<ResultadoAnticipoDTO> resultados = ejecutarDosOperaciones(
                () -> pagoService.resolverAnticipo(alquilerId, clave)
        );

        assertEquals("APROBADO", resultados.get(0).getEstadoPago());
        assertEquals("APROBADO", resultados.get(1).getEstadoPago());
        assertEquals(
                inicios.get(0).getPagoId(), resultados.get(0).getPagoId()
        );
        assertEquals(
                resultados.get(0).getPagoId(), resultados.get(1).getPagoId()
        );

        comprobarAnticipoMysql(
                alquilerId, resultados.get(0).getPagoId(), clave
        );

        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void aprobacionTardiaMysqlGuardaReembolsoSinHabilitarRetiro() {
        int alquilerId = alquilerService.crearReserva(solicitud());
        String clave = UUID.randomUUID().toString();
        configurarPagoPendienteMysql();

        ResultadoAnticipoDTO inicio =
                pagoService.iniciarAnticipo(alquilerId, clave);

        assertEquals(
                "PENDIENTE",
                pagoService.resolverAnticipo(alquilerId, clave).getEstadoPago()
        );
        assertEquals(
                new BigDecimal("1000000.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );

        when(reloj.instant()).thenReturn(
                instantePagos.plus(16, ChronoUnit.MINUTES)
        );

        assertEquals(
                "APROBADO",
                pagoService.resolverAnticipo(alquilerId, clave).getEstadoPago()
        );
        assertEquals(
                inicio.getPagoId(),
                pagoService.resolverAnticipo(alquilerId, clave).getPagoId()
        );

        transaccion.executeWithoutResult(estado -> {
            Pago pago = pagoRepository.findById(inicio.getPagoId())
                    .orElseThrow();

            assertEquals("APROBADO", pago.getEstadoPago().getCodigo());
            assertNotNull(pago.getFechaConfirmacion());

            List<Reembolso> reembolsos = reembolsoRepository
                    .findByPagoIdOrderByIdAsc(pago.getId());
            assertEquals(1, reembolsos.size());

            Reembolso reembolso = reembolsos.get(0);
            assertEquals(pago.getId(), reembolso.getPago().getId());
            assertEquals(new BigDecimal("20655.00"), reembolso.getMontoArs());
            assertEquals("ARS", reembolso.getMoneda());
            assertEquals("PENDIENTE", reembolso.getEstado());
            assertNull(reembolso.getFechaConfirmacion());

            List<HistorialEstadoReembolso> historias =
                    historialReembolsoRepository
                            .findByReembolsoIdOrderByIdAsc(reembolso.getId());
            assertEquals(1, historias.size());
            assertEquals(reembolso.getId(), historias.get(0).getReembolso().getId());
            assertNull(historias.get(0).getEstadoAnterior());
            assertEquals("PENDIENTE", historias.get(0).getEstadoNuevo());
            assertEquals("SISTEMA", historias.get(0).getOrigen());
            assertNull(historias.get(0).getActorUsuario());

            List<AlquilerDetalle> detalles = alquilerDetalleRepository
                    .buscarPorAlquilerConDatos(alquilerId);

            assertEquals(2, detalles.size());
            for (AlquilerDetalle detalle : detalles) {
                // El vencimiento operativo pertenece a otro bloque pendiente.
                assertEquals("RESERVADO", detalle.getEstado().getCodigo());
                assertNull(detalle.getFechaHoraRetiro());
            }

            assertEquals(2, historiasDetalleMysql(alquilerId).size());
            comprobarBicicletas("RESERVADA");
        });

        // PENDIENTE no significa dinero efectivamente reembolsado.
        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void mysqlRechazaClaveDuplicadaYAlquilerInexistenteEnPago() {
        int alquilerId = alquilerService.crearReserva(solicitud());
        String clave = UUID.randomUUID().toString();
        ResultadoAnticipoDTO inicio =
                pagoService.iniciarAnticipo(alquilerId, clave);

        String claveGuardada = transaccion.execute(estado ->
                pagoRepository.findById(inicio.getPagoId())
                        .orElseThrow().getClaveIdempotencia()
        );

        // Inserción directa deliberada: comprobar la UK física, no solo el servicio.
        assertThrows(
                DataIntegrityViolationException.class,
                () -> transaccion.executeWithoutResult(estado ->
                        pagoRepository.saveAndFlush(new Pago(
                                entityManager.getReference(Alquiler.class, alquilerId),
                                catalogo(EstadoPago.class, "CREADO"),
                                "ANTICIPO",
                                new BigDecimal("20655.00"),
                                LocalDateTime.ofInstant(instantePagos, ZoneOffset.UTC),
                                "SIMULADA",
                                "PRUEBA",
                                claveGuardada
                        ))
                )
        );

        assertFalse(alquilerRepository.existsById(Integer.MAX_VALUE));

        // Otra transacción: una FK inválida no puede dejar un pago huérfano.
        assertThrows(
                DataIntegrityViolationException.class,
                () -> transaccion.executeWithoutResult(estado ->
                        pagoRepository.saveAndFlush(new Pago(
                                entityManager.getReference(
                                        Alquiler.class, Integer.MAX_VALUE
                                ),
                                catalogo(EstadoPago.class, "CREADO"),
                                "ANTICIPO",
                                new BigDecimal("100.00"),
                                LocalDateTime.ofInstant(instantePagos, ZoneOffset.UTC),
                                "SIMULADA",
                                "PRUEBA",
                                "QA-FK-" + UUID.randomUUID()
                        ))
                )
        );

        transaccion.executeWithoutResult(estado -> {
            assertEquals(1, pagoRepository
                    .findByAlquilerIdOrderByIdAsc(alquilerId).size());
            assertEquals(1, historialPagoRepository
                    .findByPagoIdOrderByIdAsc(inicio.getPagoId()).size());
        });
    }

    @Test
    void retiroParcialMysqlGuardaFechasUbicacionesYReintentos() {
        AlquilerCabeceraDTO contrato = crearReservaPagadaMysql();

        Integer primeroId = contrato.getDetalles().get(0).getId();
        Integer segundoId = contrato.getDetalles().get(1).getId();

        Instant primerInstante =
                instantePagos.plus(2, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(primerInstante);

        AlquilerDetalleDTO primero = alquilerService.retirarBicicleta(
                contrato.getId(), primeroId
        );

        comprobarRetiroMysql(
                contrato, primero, primerInstante, 1L, 5
        );

        transaccion.executeWithoutResult(estado -> {
            AlquilerDetalle pendiente = alquilerDetalleRepository
                    .findById(segundoId).orElseThrow();

            assertEquals(
                    "PENDIENTE_RETIRO",
                    pendiente.getEstado().getCodigo()
            );
            assertNull(pendiente.getFechaHoraRetiro());
            assertEquals(
                    contrato.getDetalles().get(1).getVersion(),
                    pendiente.getVersion()
            );
            assertEquals(
                    "RESERVADA",
                    pendiente.getBicicleta().getEstado().getCodigo()
            );
            assertEquals(
                    estacionId,
                    pendiente.getBicicleta().getUbicacionActual().getId()
            );
        });

        Instant segundoInstante =
                instantePagos.plus(4, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(segundoInstante);

        AlquilerDetalleDTO segundo = alquilerService.retirarBicicleta(
                contrato.getId(), segundoId
        );

        comprobarRetiroMysql(
                contrato, segundo, segundoInstante, 0L, 6
        );

        var historia = alquilerService.historialAlquiler(contrato.getId());
        assertEquals(2, historia.size());
        assertEquals("RESERVADO", historia.get(0).estadoNuevo);
        assertEquals("RESERVADO", historia.get(1).estadoAnterior);
        assertEquals("EN_PROGRESO", historia.get(1).estadoNuevo);
        assertEquals(LocalDateTime.ofInstant(primerInstante, ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS),
                historia.get(1).fechaHoraCambio);

        // Repetir después del plazo no inicia un retiro nuevo.
        long[] antesDelReintento = contarRegistros();
        when(reloj.instant()).thenReturn(
                instantePagos.plus(16, ChronoUnit.MINUTES)
        );

        AlquilerDetalleDTO repetido = alquilerService.retirarBicicleta(
                contrato.getId(), primeroId
        );

        assertEquals(primero.getVersion(), repetido.getVersion());
        assertEquals(
                primero.getFechaHoraRetiro(),
                repetido.getFechaHoraRetiro()
        );
        assertArrayEquals(antesDelReintento, contarRegistros());

        comprobarRetiroMysql(
                contrato, repetido, primerInstante, 0L, 6
        );

        // En este escenario se creó un único intento de pago.
        transaccion.executeWithoutResult(estado -> {
            List<Pago> pagos = pagoRepository
                    .findByAlquilerIdOrderByIdAsc(contrato.getId());

            assertEquals(1, pagos.size());
            assertEquals(
                    3,
                    historialPagoRepository
                            .findByPagoIdOrderByIdAsc(pagos.get(0).getId())
                            .size()
            );
        });

        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void retirosSimultaneosMysqlNoDuplicanUsoNiHistorias()
            throws Exception {
        AlquilerCabeceraDTO contrato = crearReservaPagadaMysql();
        Integer detalleId = contrato.getDetalles().get(0).getId();

        Instant instante = instantePagos.plus(2, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(instante);

        List<AlquilerDetalleDTO> resultados = ejecutarDosOperaciones(
                () -> alquilerService.retirarBicicleta(
                        contrato.getId(), detalleId
                )
        );

        assertEquals(resultados.get(0).getId(), resultados.get(1).getId());
        assertEquals(
                resultados.get(0).getVersion(),
                resultados.get(1).getVersion()
        );
        assertEquals(
                resultados.get(0).getFechaHoraRetiro(),
                resultados.get(1).getFechaHoraRetiro()
        );

        comprobarRetiroMysql(
                contrato, resultados.get(0), instante, 1L, 5
        );

        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void falloInternoDelRetiroMysqlRevierteEstadoUbicacionEHistorias() {
        AlquilerCabeceraDTO contrato = crearReservaPagadaMysql();
        Integer detalleId = contrato.getDetalles().get(0).getId();

        when(reloj.instant()).thenReturn(
                instantePagos.plus(2, ChronoUnit.MINUTES)
        );

        long[] antes = contarRegistros();

        // Guardar historia real y enviar las escrituras a MySQL.
        // El fallo ocurre dentro de retirarBicicleta, no después de su commit.
        doAnswer(invocacion -> {
            invocacion.callRealMethod();
            entityManager.flush();

            throw new IllegalStateException("FALLO_RETIRO_MYSQL_QA");
        }).when(trazabilidadEspiada).estado(
                any(Bicicleta.class),
                any(EstadoBicicleta.class),
                any(Usuario.class),
                anyString(),
                any(LocalDateTime.class)
        );

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> alquilerService.retirarBicicleta(
                        contrato.getId(), detalleId
                )
        );

        assertEquals("FALLO_RETIRO_MYSQL_QA", error.getMessage());
        assertArrayEquals(antes, contarRegistros());

        // Nueva transacción, posterior al rollback.
        transaccion.executeWithoutResult(estado -> {
            Alquiler alquiler = alquilerRepository
                    .findById(contrato.getId()).orElseThrow();

            assertEquals(
                    "RESERVADO",
                    alquiler.getEstadoContratacion().getCodigo()
            );
            assertEquals(contrato.getVersion(), alquiler.getVersion());

            List<AlquilerDetalle> detalles = alquilerDetalleRepository
                    .buscarPorAlquilerConDatos(contrato.getId());

            assertEquals(2, detalles.size());

            for (int i = 0; i < detalles.size(); i++) {
                AlquilerDetalle detalle = detalles.get(i);

                assertEquals(
                        "PENDIENTE_RETIRO",
                        detalle.getEstado().getCodigo()
                );
                assertEquals(
                        contrato.getDetalles().get(i).getVersion(),
                        detalle.getVersion()
                );
                assertNull(detalle.getFechaHoraRetiro());
                assertNull(detalle.getFechaHoraDevolucionEstimada());
            }

            comprobarBicicletas("RESERVADA");
            assertEquals(2L, ocupacionEstacionMysql());
            assertEquals(4, historiasDetalleMysql(contrato.getId()).size());
            assertEquals(1, historiasBicicletaMysql(bicicleta1Id).size());
            assertEquals(1, historiasBicicletaMysql(bicicleta2Id).size());
        });

        // El pago anterior al retiro sigue confirmado.
        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void retiroMysqlEnElLimiteSeRechazaSinEscrituras() {
        AlquilerCabeceraDTO contrato = crearReservaPagadaMysql();
        Integer detalleId = contrato.getDetalles().get(0).getId();

        when(reloj.instant()).thenReturn(
                contrato.getFechaHoraVencimientoReserva()
                        .toInstant(ZoneOffset.UTC)
        );

        long[] antes = contarRegistros();

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> alquilerService.retirarBicicleta(
                        contrato.getId(), detalleId
                )
        );

        assertTrue(error.getMessage().contains("venció"));
        assertArrayEquals(antes, contarRegistros());

        transaccion.executeWithoutResult(estado -> {
            comprobarBicicletas("RESERVADA");
            assertEquals(2L, ocupacionEstacionMysql());

            for (AlquilerDetalle detalle : alquilerDetalleRepository
                    .buscarPorAlquilerConDatos(contrato.getId())) {
                assertEquals(
                        "PENDIENTE_RETIRO",
                        detalle.getEstado().getCodigo()
                );
                assertNull(detalle.getFechaHoraRetiro());
            }

            // El procesamiento operativo del vencimiento es otro bloque.
            assertEquals(
                    "RESERVADO",
                    alquilerRepository.findById(contrato.getId())
                            .orElseThrow()
                            .getEstadoContratacion().getCodigo()
            );
        });
    }

    @Test
    void retiroMysqlConAnticipoCeroNoInventaPago() {
        transaccion.executeWithoutResult(estado -> {
            catalogo(TipoBicicleta.class, "MONTANA")
                    .setPrecioPorHoraUsd(new BigDecimal("0.01"));
            catalogo(TipoBicicleta.class, "PASEO")
                    .setPrecioPorHoraUsd(new BigDecimal("0.01"));
        });

        CrearAlquilerDTO solicitudCero = solicitud();
        solicitudCero.setDuracionEstimadaSegundos(1);

        int alquilerId = alquilerService.crearReserva(solicitudCero);
        AlquilerCabeceraDTO contrato =
                alquilerService.obtenerAlquiler(alquilerId);

        assertEquals(
                new BigDecimal("0.00"),
                contrato.getCostoEstimadoArs()
        );

        Instant instante = instantePagos.plus(2, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(instante);

        AlquilerDetalleDTO resultado = alquilerService.retirarBicicleta(
                alquilerId, contrato.getDetalles().get(0).getId()
        );

        comprobarRetiroMysql(
                contrato, resultado, instante, 1L, 4
        );

        transaccion.executeWithoutResult(estado -> {
            assertTrue(pagoRepository
                    .findByAlquilerIdOrderByIdAsc(alquilerId).isEmpty());
            assertTrue(reembolsoRepository
                    .findByPagoAlquilerIdOrderByIdAsc(alquilerId).isEmpty());

            AlquilerDetalle pendiente = alquilerDetalleRepository
                    .findById(contrato.getDetalles().get(1).getId())
                    .orElseThrow();

            assertEquals("RESERVADO", pendiente.getEstado().getCodigo());
            assertNull(pendiente.getFechaHoraRetiro());

            HistorialEstadoDetalle habilitacion =
                    historiasDetalleMysql(alquilerId).stream()
                            .filter(historia ->
                                    historia.getAlquilerDetalle().getId()
                                            .equals(resultado.getId())
                                            && "PENDIENTE_RETIRO".equals(
                                            historia.getEstadoNuevo().getCodigo()
                                    )
                            )
                            .findFirst().orElseThrow();

            assertEquals("SISTEMA", habilitacion.getOrigen());
            assertNull(habilitacion.getActorUsuario());
            assertEquals(
                    resultado.getFechaHoraRetiro(),
                    habilitacion.getFechaHoraCambio()
            );
        });

        assertEquals(
                new BigDecimal("1000000.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void devolucionParcialMysqlGuardaCostosYCierraUsoConLiquidacion() {
        AlquilerCabeceraDTO reserva = crearReservaPagadaMysql();
        AlquilerDetalleDTO primerRetiro =
                retirarParaDevolucionMysql(reserva);

        AlquilerDetalleDTO segundoRetiro =
                alquilerService.retirarBicicleta(
                        reserva.getId(),
                        reserva.getDetalles().get(1).getId()
                );

        // Cambiar la tarifa vigente no debe alterar la ya acordada.
        transaccion.executeWithoutResult(estado -> {
            catalogo(TipoBicicleta.class, "MONTANA")
                    .setPrecioPorHoraUsd(new BigDecimal("99.00"));
            catalogo(TipoBicicleta.class, "PASEO")
                    .setPrecioPorHoraUsd(new BigDecimal("99.00"));
        });

        clearInvocations(cotizacionDolarService);

        Instant primeraRecepcion = instantePagos
                .plus(32, ChronoUnit.MINUTES)
                .plus(900000, ChronoUnit.MICROS);
        when(reloj.instant()).thenReturn(primeraRecepcion);

        AlquilerDetalleDTO primera = alquilerService.devolverBicicleta(
                reserva.getId(),
                primerRetiro.getId(),
                solicitudDevolucionMysql(false)
        );

        assertEquals(new BigDecimal("5.00"), primera.getSubtotalRealUsd());
        assertEquals(new BigDecimal("7650.00"), primera.getSubtotalRealArs());

        comprobarRecepcionMysql(
                reserva, primerRetiro, primera, primeraRecepcion,
                "DISPONIBLE", 1L, "EN_PROGRESO"
        );

        Instant segundaRecepcion =
                instantePagos.plus(62, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(segundaRecepcion);

        AlquilerDetalleDTO segunda = alquilerService.devolverBicicleta(
                reserva.getId(),
                segundoRetiro.getId(),
                solicitudDevolucionMysql(false)
        );

        assertEquals(new BigDecimal("8.00"), segunda.getSubtotalRealUsd());
        assertEquals(new BigDecimal("12240.00"), segunda.getSubtotalRealArs());

        comprobarRecepcionMysql(
                reserva, segundoRetiro, segunda, segundaRecepcion,
                "DISPONIBLE", 2L, "FINALIZADO"
        );

        // Repetir después del cierre no modifica fechas ni agrega escrituras.
        long[] antesDelReintento = contarEscriturasDevolucionMysql();
        when(reloj.instant()).thenReturn(
                instantePagos.plus(90, ChronoUnit.MINUTES)
        );

        AlquilerDetalleDTO repetida = alquilerService.devolverBicicleta(
                reserva.getId(),
                primerRetiro.getId(),
                solicitudDevolucionMysql(false)
        );

        assertEquals(primera.getVersion(), repetida.getVersion());
        assertEquals(
                primera.getFechaHoraDevolucionReal(),
                repetida.getFechaHoraDevolucionReal()
        );
        assertArrayEquals(
                antesDelReintento,
                contarEscriturasDevolucionMysql()
        );

        transaccion.executeWithoutResult(estado -> {
            Alquiler guardado = alquilerRepository
                    .findById(reserva.getId()).orElseThrow();

            assertEquals(
                    new BigDecimal("13.00"),
                    guardado.getCostoFinalUsd()
            );
            assertEquals(
                    new BigDecimal("19890.00"),
                    guardado.getCostoFinalArs()
            );
            assertEquals(
                    LocalDateTime.ofInstant(segundaRecepcion, ZoneOffset.UTC),
                    guardado.getFechaHoraLiquidacion()
            );
        });

        verifyNoInteractions(cotizacionDolarService);
        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void devolucionesConcurrentesMysqlNoDuplicanOrdenNiHistorias()
            throws Exception {
        AlquilerCabeceraDTO reserva = crearReservaPagadaMysql();
        AlquilerDetalleDTO retiro = retirarParaDevolucionMysql(reserva);

        Instant recepcion = instantePagos.plus(32, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(recepcion);
        long[] antes = contarEscriturasDevolucionMysql();

        List<AlquilerDetalleDTO> resultados = ejecutarDosOperaciones(
                () -> alquilerService.devolverBicicleta(
                        reserva.getId(),
                        retiro.getId(),
                        solicitudDevolucionMysql(true)
                )
        );

        assertEquals(resultados.get(0).getVersion(), resultados.get(1).getVersion());
        assertEquals(
                resultados.get(0).getFechaHoraDevolucionReal(),
                resultados.get(1).getFechaHoraDevolucionReal()
        );
        assertEquals(
                new BigDecimal("7650.00"),
                resultados.get(0).getSubtotalRealArs()
        );

        comprobarRecepcionMysql(
                reserva, retiro, resultados.get(0), recepcion,
                "MANTENIMIENTO", 2L, "EN_PROGRESO"
        );
        comprobarOrdenDevolucionMysql(
                retiro.getId(),
                "CORRECTIVO",
                LocalDateTime.ofInstant(recepcion, ZoneOffset.UTC)
        );

        long[] esperados = antes.clone();
        esperados[2]++; // HistorialEstadoDetalle.
        esperados[3]++; // HistorialEstadoBicicleta.
        esperados[4]++; // OrdenMantenimiento.
        esperados[5]++; // HistorialOrdenMantenimiento.
        assertArrayEquals(esperados, contarEscriturasDevolucionMysql());

        transaccion.executeWithoutResult(estado -> {
            AlquilerDetalle detalle = alquilerDetalleRepository
                    .findById(retiro.getId()).orElseThrow();

            assertTrue(detalle.isFallaReportada());
            assertEquals("No frena.", detalle.getDescripcionFalla());
            assertEquals(usuarioId, detalle.getReportadoPor().getId());
            assertEquals(
                    LocalDateTime.ofInstant(recepcion, ZoneOffset.UTC),
                    detalle.getFechaReporteFalla()
            );
        });
    }

    @Test
    void devolucionMysqlGeneraPreventivaCuandoVenceElCalendario() {
        AlquilerCabeceraDTO reserva = crearReservaPagadaMysql();
        AlquilerDetalleDTO retiro = retirarParaDevolucionMysql(reserva);

        // Avanzar el reloj de prueba hasta el vencimiento real de la preventiva.
        // No se modifica ni inventa la fecha de alta.
        LocalDate vencimiento = transaccion.execute(estado -> {
            Bicicleta bicicleta = entityManager.find(
                    Bicicleta.class, retiro.getBicicletaId()
            );
            return bicicleta.getFechaAlta().plusMonths(
                    bicicleta.getTipo().getIntervaloPreventivoMeses()
            );
        });

        Instant recepcion = vencimiento.atTime(12, 0)
                .toInstant(ZoneOffset.UTC);
        when(reloj.instant()).thenReturn(recepcion);

        AlquilerDetalleDTO resultado = alquilerService.devolverBicicleta(
                reserva.getId(),
                retiro.getId(),
                solicitudDevolucionMysql(false)
        );

        assertFalse(resultado.isFallaReportada());
        comprobarRecepcionMysql(
                reserva, retiro, resultado, recepcion,
                "MANTENIMIENTO", 2L, "EN_PROGRESO"
        );
        comprobarOrdenDevolucionMysql(
                retiro.getId(),
                "PREVENTIVO",
                LocalDateTime.ofInstant(recepcion, ZoneOffset.UTC)
        );
    }

    @Test
    void falloInternoDeDevolucionMysqlRevierteTambienLaOrden() {
        AlquilerCabeceraDTO reserva = crearReservaPagadaMysql();
        AlquilerDetalleDTO retiro = retirarParaDevolucionMysql(reserva);

        when(reloj.instant()).thenReturn(
                instantePagos.plus(32, ChronoUnit.MINUTES)
        );
        long[] antes = contarEscriturasDevolucionMysql();

        // El fallo ocurre dentro de devolverBicicleta, después de escrituras reales.
        doAnswer(invocacion -> {
            invocacion.callRealMethod();
            entityManager.flush();
            throw new IllegalStateException("FALLO_DEVOLUCION_MYSQL_QA");
        }).when(trazabilidadEspiada).estado(
                any(Bicicleta.class),
                any(EstadoBicicleta.class),
                any(Usuario.class),
                anyString(),
                any(LocalDateTime.class)
        );

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> alquilerService.devolverBicicleta(
                        reserva.getId(),
                        retiro.getId(),
                        solicitudDevolucionMysql(true)
                )
        );

        assertEquals("FALLO_DEVOLUCION_MYSQL_QA", error.getMessage());
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());

        // Relectura posterior al rollback, desde otra transacción.
        transaccion.executeWithoutResult(estado -> {
            AlquilerDetalle detalle = alquilerDetalleRepository
                    .findById(retiro.getId()).orElseThrow();

            assertEquals("EN_CURSO", detalle.getEstado().getCodigo());
            assertEquals(retiro.getVersion(), detalle.getVersion());
            assertEquals(retiro.getFechaHoraRetiro(), detalle.getFechaHoraRetiro());
            assertNull(detalle.getFechaHoraDevolucionReal());
            assertNull(detalle.getFechaHoraFinUso());
            assertNull(detalle.getFechaHoraCierreDetalle());
            assertNull(detalle.getEstacionDevolucion());
            assertNull(detalle.getSubtotalRealUsd());
            assertNull(detalle.getSubtotalRealArs());
            assertNull(detalle.getOrdenMantenimiento());
            assertFalse(detalle.isFallaReportada());
            assertNull(detalle.getDescripcionFalla());
            assertNull(detalle.getFechaReporteFalla());
            assertNull(detalle.getReportadoPor());

            assertEquals("EN_USO", detalle.getBicicleta().getEstado().getCodigo());
            assertNull(detalle.getBicicleta().getUbicacionActual());
            assertEquals(1L, ocupacionEstacionMysql());
        });

        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado().getSaldoDisponibleArs()
        );
    }

    @Test
    void devolucionMysqlRechazaDestinoInternoOLlenoSinEscrituras() {
        AlquilerCabeceraDTO reserva = crearReservaPagadaMysql();
        AlquilerDetalleDTO retiro = retirarParaDevolucionMysql(reserva);

        when(reloj.instant()).thenReturn(
                instantePagos.plus(32, ChronoUnit.MINUTES)
        );

        transaccion.executeWithoutResult(estado ->
                entityManager.find(Estacion.class, estacionId)
                        .setEsPublica(false)
        );

        long[] antes = contarEscriturasDevolucionMysql();

        assertThrows(
                IllegalStateException.class,
                () -> alquilerService.devolverBicicleta(
                        reserva.getId(), retiro.getId(),
                        solicitudDevolucionMysql(false)
                )
        );
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());

        // La otra bicicleta sigue físicamente allí: capacidad 1 significa llena.
        transaccion.executeWithoutResult(estado -> {
            Estacion estacion = entityManager.find(Estacion.class, estacionId);
            estacion.setEsPublica(true);
            estacion.setCapacidad(1);
        });

        assertThrows(
                IllegalStateException.class,
                () -> alquilerService.devolverBicicleta(
                        reserva.getId(), retiro.getId(),
                        solicitudDevolucionMysql(false)
                )
        );
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());

        transaccion.executeWithoutResult(estado -> {
            AlquilerDetalle detalle = alquilerDetalleRepository
                    .findById(retiro.getId()).orElseThrow();

            assertEquals("EN_CURSO", detalle.getEstado().getCodigo());
            assertNull(detalle.getFechaHoraDevolucionReal());
            assertNull(detalle.getBicicleta().getUbicacionActual());
            assertEquals(1L, ocupacionEstacionMysql());
        });
    }

    @Test
    void devolucionMysqlConIncidenciaPendienteNoFijaCostoDefinitivo() {
        AlquilerCabeceraDTO reserva = crearReservaRetiradaMysql();
        AlquilerDetalleDTO retiro = reserva.getDetalles().get(0);

        // Fixture: el flujo de revisión de incidencias todavía no está implementado.
        Integer incidenciaId = transaccion.execute(estado -> {
            Incidencia incidencia = new Incidencia();

            incidencia.setAlquilerDetalle(entityManager.getReference(
                    AlquilerDetalle.class, retiro.getId()
            ));
            incidencia.setRegistradoPor(
                    entityManager.getReference(Usuario.class, usuarioId)
            );
            incidencia.setTipo("AVERIA");
            incidencia.setDescripcion("La falla interrumpió el recorrido.");
            incidencia.setRequiereRevisionEconomica(true);
            incidencia.setFechaHoraHechoInformada(
                    retiro.getFechaHoraRetiro().plusMinutes(10)
            );

            ReflectionTestUtils.setField(
                    incidencia,
                    "fechaHoraRegistro",
                    retiro.getFechaHoraRetiro().plusMinutes(11)
            );

            entityManager.persist(incidencia);
            entityManager.flush();
            return incidencia.getId();
        });

        Instant recepcion = instantePagos.plus(32, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(recepcion);

        AlquilerDetalleDTO primera = alquilerService.devolverBicicleta(
                reserva.getId(),
                retiro.getId(),
                solicitudDevolucionMysql(false)
        );

        comprobarRecepcionMysql(
                reserva, retiro, primera, recepcion,
                "MANTENIMIENTO", 1L, "EN_PROGRESO"
        );

        assertNull(primera.getSubtotalRealUsd());
        assertNull(primera.getSubtotalRealArs());

        AlquilerDetalleDTO segunda = alquilerService.devolverBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(1).getId(),
                solicitudDevolucionMysql(false)
        );

        assertEquals(new BigDecimal("4.00"), segunda.getSubtotalRealUsd());
        assertEquals(new BigDecimal("6120.00"), segunda.getSubtotalRealArs());

        transaccion.executeWithoutResult(estado -> {
            LocalDateTime fecha =
                    LocalDateTime.ofInstant(recepcion, ZoneOffset.UTC);

            Alquiler alquiler = entityManager.find(
                    Alquiler.class, reserva.getId()
            );

            assertEquals(
                    "FINALIZADO",
                    alquiler.getEstadoContratacion().getCodigo()
            );
            assertEquals(fecha, alquiler.getFechaHoraCierre());
            assertNull(alquiler.getCostoFinalUsd());
            assertNull(alquiler.getCostoFinalArs());
            assertNull(alquiler.getFechaHoraLiquidacion());

            AlquilerDetalle primero = entityManager.find(
                    AlquilerDetalle.class, retiro.getId()
            );
            AlquilerDetalle segundo = entityManager.find(
                    AlquilerDetalle.class, segunda.getId()
            );

            assertEquals("DEVUELTO", primero.getEstado().getCodigo());
            assertEquals("DEVUELTO", segundo.getEstado().getCodigo());
            assertEquals(fecha, primero.getFechaHoraDevolucionReal());
            assertEquals(fecha, primero.getFechaHoraFinUso());
            assertNull(primero.getSubtotalRealUsd());
            assertNull(primero.getSubtotalRealArs());
            assertEquals(2L, ocupacionEstacionMysql());

            Incidencia incidencia = entityManager.find(
                    Incidencia.class, incidenciaId
            );

            assertEquals("PENDIENTE", incidencia.getEstadoRevision());
            assertNull(incidencia.getFechaHoraDecision());
            assertNull(incidencia.getFechaHoraCorteValidado());
        });

        assertEquals(
                "SIN_LIQUIDAR",
                liquidacionService.consultarLiquidacion(reserva.getId())
                        .getSituacionFinanciera()
        );
        assertEquals(0L, contarDeudasMysql());
    }

    @Test
    void deudaMysqlImpideNuevosAlquileresAunqueElAnteriorEsteOculto() {
        AlquilerCabeceraDTO reserva = crearReservaRetiradaMysql();

        when(reloj.instant()).thenReturn(
                instantePagos.plus(62, ChronoUnit.MINUTES)
        );

        for (AlquilerDetalleDTO detalle : reserva.getDetalles()) {
            alquilerService.devolverBicicleta(
                    reserva.getId(),
                    detalle.getId(),
                    solicitudDevolucionMysql(false)
            );
        }

        LiquidacionAlquilerDTO resumen =
                liquidacionService.consultarLiquidacion(reserva.getId());

        assertEquals("SALDO_PENDIENTE", resumen.getSituacionFinanciera());
        assertEquals(new BigDecimal("27540.00"), resumen.getCostoFinalArs());
        assertEquals(
                new BigDecimal("20655.00"),
                resumen.getPagosAprobadosArs()
        );
        assertEquals(
                new BigDecimal("20655.00"),
                resumen.getNetoConfirmadoArs()
        );
        assertEquals(
                new BigDecimal("6885.00"),
                resumen.getSaldoPendienteArs()
        );
        assertEquals(1L, contarDeudasMysql());

        // Ocultarlo del listado no debe hacer desaparecer su deuda.
        transaccion.executeWithoutResult(estado -> {
            entityManager.find(Alquiler.class, reserva.getId())
                    .setBorradoAlquiler(true);

        });

        assertEquals(1L, contarDeudasMysql());

        long[] antes = contarEscriturasDevolucionMysql();

        IllegalStateException reservaRechazada = assertThrows(
                IllegalStateException.class,
                () -> alquilerService.crearReserva(solicitud())
        );

        assertTrue(
                reservaRechazada.getMessage().contains("saldo pendiente")
        );

        assertArrayEquals(antes, contarEscriturasDevolucionMysql());
        assertEquals(1L, contarDeudasMysql());

        transaccion.executeWithoutResult(estado ->
                comprobarBicicletas("DISPONIBLE")
        );
    }

    @ParameterizedTest
    @CsvSource({
            "32,13770.00,REEMBOLSO_PENDIENTE",
            "47,20655.00,SALDADO"
    })
    void liquidacionMysqlSinDeudaPermiteUnaNuevaReserva(
            int minuto,
            String costoArs,
            String situacion
    ) {
        AlquilerCabeceraDTO reserva = crearReservaRetiradaMysql();

        when(reloj.instant()).thenReturn(
                instantePagos.plus(minuto, ChronoUnit.MINUTES)
        );

        for (AlquilerDetalleDTO detalle : reserva.getDetalles()) {
            alquilerService.devolverBicicleta(
                    reserva.getId(),
                    detalle.getId(),
                    solicitudDevolucionMysql(false)
            );
        }

        LiquidacionAlquilerDTO resumen =
                liquidacionService.consultarLiquidacion(reserva.getId());

        assertEquals(situacion, resumen.getSituacionFinanciera());
        assertEquals(
                new BigDecimal(costoArs),
                resumen.getCostoFinalArs()
        );
        assertEquals(
                new BigDecimal("20655.00"),
                resumen.getNetoConfirmadoArs()
        );
        assertEquals(
                new BigDecimal("0.00"),
                resumen.getSaldoPendienteArs()
        );
        assertEquals(
                new BigDecimal(
                        "REEMBOLSO_PENDIENTE".equals(situacion)
                                ? "6885.00"
                                : "0.00"
                ),
                resumen.getReembolsoPendienteArs()
        );
        assertEquals(0L, contarDeudasMysql());

        int nuevoId = alquilerService.crearReserva(solicitud());

        assertNotEquals(reserva.getId(), nuevoId);
        assertEquals(
                "RESERVADO",
                alquilerService.obtenerAlquiler(nuevoId).getEstado()
        );
    }

    @Test
    void devolucionesFinalesConcurrentesMysqlLiquidanUnaSolaVez()
            throws Exception {

        AlquilerCabeceraDTO reserva = crearReservaRetiradaMysql();
        Instant recepcion = instantePagos.plus(32, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(recepcion);

        long[] antes = contarEscriturasDevolucionMysql();
        AtomicInteger siguiente = new AtomicInteger();

        List<AlquilerDetalleDTO> resultados = ejecutarDosOperaciones(
                () -> alquilerService.devolverBicicleta(
                        reserva.getId(),
                        reserva.getDetalles()
                                .get(siguiente.getAndIncrement()).getId(),
                        solicitudDevolucionMysql(false)
                )
        );

        assertNotEquals(
                resultados.get(0).getId(),
                resultados.get(1).getId()
        );

        for (AlquilerDetalleDTO resultado : resultados) {
            assertEquals("DEVUELTO", resultado.getEstado());
        }

        transaccion.executeWithoutResult(estado -> {
            LocalDateTime fecha =
                    LocalDateTime.ofInstant(recepcion, ZoneOffset.UTC);

            Alquiler alquiler = entityManager.find(
                    Alquiler.class, reserva.getId()
            );

            assertEquals(
                    "FINALIZADO",
                    alquiler.getEstadoContratacion().getCodigo()
            );
            assertEquals(
                    new BigDecimal("9.00"),
                    alquiler.getCostoFinalUsd()
            );
            assertEquals(
                    new BigDecimal("13770.00"),
                    alquiler.getCostoFinalArs()
            );
            assertEquals(fecha, alquiler.getFechaHoraCierre());
            assertEquals(fecha, alquiler.getFechaHoraLiquidacion());
            assertTrue(alquiler.getVersion() > reserva.getVersion());

            assertEquals(2L, ocupacionEstacionMysql());
            assertEquals(
                    8,
                    historiasDetalleMysql(reserva.getId()).size()
            );

            for (AlquilerDetalleDTO original : reserva.getDetalles()) {
                AlquilerDetalle detalle = entityManager.find(
                        AlquilerDetalle.class, original.getId()
                );

                assertEquals("DEVUELTO", detalle.getEstado().getCodigo());
                assertEquals(fecha, detalle.getFechaHoraDevolucionReal());
                assertEquals(fecha, detalle.getFechaHoraFinUso());
                assertEquals(
                        3,
                        historiasBicicletaMysql(original.getBicicletaId())
                                .size()
                );
            }
        });

        verify(liquidacionEspiada, times(1))
                .liquidarSiCorresponde(any(Alquiler.class));

        long[] esperados = antes.clone();
        esperados[2] += 2; // HistorialEstadoDetalle
        esperados[3] += 2; // HistorialEstadoBicicleta
        esperados[7]++; // Obligación de reintegro al cerrar con excedente.
        esperados[9]++; // Historia inicial del reembolso.

        assertArrayEquals(esperados, contarEscriturasDevolucionMysql());
        assertEquals(0L, contarDeudasMysql());
    }

    @Test
    void falloTrasLiquidarMysqlRevierteCierreCostosEHistorias() {
        AlquilerCabeceraDTO reserva = crearReservaRetiradaMysql();

        Instant primeraRecepcion =
                instantePagos.plus(32, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(primeraRecepcion);

        alquilerService.devolverBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(0).getId(),
                solicitudDevolucionMysql(false)
        );

        Integer versionAntes = transaccion.execute(estado ->
                entityManager.find(Alquiler.class, reserva.getId())
                        .getVersion()
        );
        long[] antes = contarEscriturasDevolucionMysql();

        Instant segundaRecepcion =
                instantePagos.plus(62, ChronoUnit.MINUTES);
        when(reloj.instant()).thenReturn(segundaRecepcion);

        doAnswer(invocacion -> {
            assertTrue((Boolean) invocacion.callRealMethod());

            Alquiler alquiler = invocacion.getArgument(0);

            assertEquals(
                    new BigDecimal("13.00"),
                    alquiler.getCostoFinalUsd()
            );
            assertEquals(
                    new BigDecimal("19890.00"),
                    alquiler.getCostoFinalArs()
            );
            assertNotNull(alquiler.getFechaHoraLiquidacion());

            entityManager.flush();

            throw new IllegalStateException(
                    "FALLO_LIQUIDACION_MYSQL_QA"
            );
        }).when(liquidacionEspiada)
                .liquidarSiCorresponde(any(Alquiler.class));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> alquilerService.devolverBicicleta(
                        reserva.getId(),
                        reserva.getDetalles().get(1).getId(),
                        solicitudDevolucionMysql(false)
                )
        );

        assertEquals("FALLO_LIQUIDACION_MYSQL_QA", error.getMessage());
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());

        transaccion.executeWithoutResult(estado -> {
            Alquiler alquiler = entityManager.find(
                    Alquiler.class, reserva.getId()
            );

            assertEquals(
                    "EN_PROGRESO",
                    alquiler.getEstadoContratacion().getCodigo()
            );
            assertEquals(versionAntes, alquiler.getVersion());
            assertNull(alquiler.getFechaHoraCierre());
            assertNull(alquiler.getCostoFinalUsd());
            assertNull(alquiler.getCostoFinalArs());
            assertNull(alquiler.getFechaHoraLiquidacion());

            AlquilerDetalle primero = entityManager.find(
                    AlquilerDetalle.class,
                    reserva.getDetalles().get(0).getId()
            );
            AlquilerDetalle segundo = entityManager.find(
                    AlquilerDetalle.class,
                    reserva.getDetalles().get(1).getId()
            );

            assertEquals("DEVUELTO", primero.getEstado().getCodigo());
            assertEquals(
                    LocalDateTime.ofInstant(
                            primeraRecepcion, ZoneOffset.UTC
                    ),
                    primero.getFechaHoraDevolucionReal()
            );
            assertEquals(
                    new BigDecimal("5.00"),
                    primero.getSubtotalRealUsd()
            );
            assertEquals(
                    new BigDecimal("7650.00"),
                    primero.getSubtotalRealArs()
            );
            assertEquals(
                    "DISPONIBLE",
                    primero.getBicicleta().getEstado().getCodigo()
            );
            assertEquals(
                    estacionId,
                    primero.getBicicleta().getUbicacionActual().getId()
            );

            assertEquals("EN_CURSO", segundo.getEstado().getCodigo());
            assertEquals(
                    reserva.getDetalles().get(1).getVersion(),
                    segundo.getVersion()
            );
            assertNull(segundo.getFechaHoraDevolucionReal());
            assertNull(segundo.getFechaHoraFinUso());
            assertNull(segundo.getFechaHoraCierreDetalle());
            assertNull(segundo.getEstacionDevolucion());
            assertNull(segundo.getSubtotalRealUsd());
            assertNull(segundo.getSubtotalRealArs());
            assertEquals(
                    "EN_USO",
                    segundo.getBicicleta().getEstado().getCodigo()
            );
            assertNull(segundo.getBicicleta().getUbicacionActual());
            assertEquals(1L, ocupacionEstacionMysql());
        });

        // Quitamos el fallo: el mismo pedido debe poder completarse.
        reset(liquidacionEspiada);

        alquilerService.devolverBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(1).getId(),
                solicitudDevolucionMysql(false)
        );

        LiquidacionAlquilerDTO resumen =
                liquidacionService.consultarLiquidacion(reserva.getId());

        assertEquals(
                new BigDecimal("13.00"),
                resumen.getCostoFinalUsd()
        );
        assertEquals(
                new BigDecimal("19890.00"),
                resumen.getCostoFinalArs()
        );
        assertEquals(
                LocalDateTime.ofInstant(segundaRecepcion, ZoneOffset.UTC),
                resumen.getFechaHoraLiquidacion()
        );
        assertEquals(
                "REEMBOLSO_PENDIENTE",
                resumen.getSituacionFinanciera()
        );
        assertEquals(
                new BigDecimal("765.00"),
                resumen.getReembolsoPendienteArs()
        );

        long[] esperados = antes.clone();
        esperados[2]++;
        esperados[3]++;
        esperados[7]++;
        esperados[9]++;
        assertArrayEquals(esperados, contarEscriturasDevolucionMysql());
        assertEquals(
                new BigDecimal("979345.00"),
                pagoService.consultarSaldoSimulado()
                        .getSaldoDisponibleArs()
        );
    }

    @Test
    void liquidacionMysqlExigeUnaTransaccionExistente() {
        assertThrows(
                IllegalTransactionStateException.class,
                () -> liquidacionService.liquidarSiCorresponde(null)
        );
    }

    @Test
    void adicionalMysqlSaldaDeudaOcultaSinRehacerLiquidacionYPermiteReservar() {
        AlquilerCabeceraDTO alquiler = crearAlquilerConDeudaMysql();
        LocalDateTime fechaLiquidacion = alquiler.getFechaHoraLiquidacion();
        transaccion.executeWithoutResult(estado -> entityManager.find(Alquiler.class, alquiler.getId())
                .setBorradoAlquiler(true));
        assertEquals(1L, contarDeudasMysql());
        long[] antes = contarEscriturasDevolucionMysql();
        String clave = UUID.randomUUID().toString();

        ResultadoAdicionalDTO inicio = pagoService.iniciarAdicional(alquiler.getId(), clave);
        ResultadoAdicionalDTO repetido = pagoService.iniciarAdicional(alquiler.getId(), clave.toUpperCase());
        assertEquals(inicio.getPagoId(), repetido.getPagoId());
        assertEquals("CREADO", inicio.getEstadoPago());
        assertEquals(new BigDecimal("6885.00"), inicio.getMontoArs());
        assertEquals(1L, contarDeudasMysql());

        ResultadoAdicionalDTO aprobado = pagoService.resolverAdicional(alquiler.getId(), clave);
        assertEquals("APROBADO", aprobado.getEstadoPago());
        assertEquals("SALDADO", aprobado.getLiquidacion().getSituacionFinanciera());
        assertEquals(new BigDecimal("0.00"), aprobado.getLiquidacion().getSaldoPendienteArs());
        assertEquals(0L, contarDeudasMysql());
        long[] despues = contarEscriturasDevolucionMysql();
        long[] esperados = antes.clone();
        esperados[6]++;
        esperados[8] += 3;
        assertArrayEquals(esperados, despues);
        pagoService.resolverAdicional(alquiler.getId(), clave);
        assertArrayEquals(despues, contarEscriturasDevolucionMysql());
        comprobarAdicionalAprobadoMysql(alquiler.getId(), aprobado.getPagoId(), fechaLiquidacion);
        assertEquals(new BigDecimal("972460.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());

        int nuevaReserva = alquilerService.crearReserva(solicitud());
        assertNotEquals(alquiler.getId(), nuevaReserva);
        assertEquals("RESERVADO", alquilerService.obtenerAlquiler(nuevaReserva).getEstado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECHAZADO", "PENDIENTE"})
    void adicionalMysqlSinConfirmacionConservaDeudaYTrataReintentos(String modo) {
        AlquilerCabeceraDTO alquiler = crearAlquilerConDeudaMysql();
        String nickname = transaccion.execute(estado -> entityManager.find(Usuario.class, usuarioId).getNickname());
        String prefijo = "pagos.simulados.cuentas." + nickname + ".";
        entorno.getPropertySources().addFirst(new MapPropertySource(PROPIEDADES_PAGOS_MYSQL,
                Map.<String, Object>of(prefijo + "resultado", modo, prefijo + "demora-segundos", "20")));
        String clave = UUID.randomUUID().toString();
        ResultadoAdicionalDTO inicial = pagoService.iniciarAdicional(alquiler.getId(), clave);
        ResultadoAdicionalDTO respuesta = pagoService.resolverAdicional(alquiler.getId(), clave);
        assertEquals(modo, respuesta.getEstadoPago());
        assertNull(respuesta.getFechaConfirmacionPago());
        assertEquals("SALDO_PENDIENTE", respuesta.getLiquidacion().getSituacionFinanciera());
        assertEquals(1L, contarDeudasMysql());
        assertEquals(new BigDecimal("979345.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
        Integer pagoFinal;

        if ("PENDIENTE".equals(modo)) {
            long[] antesReintento = contarEscriturasDevolucionMysql();
            org.springframework.web.server.ResponseStatusException conflicto = assertThrows(
                    org.springframework.web.server.ResponseStatusException.class,
                    () -> pagoService.iniciarAdicional(alquiler.getId(), UUID.randomUUID().toString()));
            assertEquals(org.springframework.http.HttpStatus.CONFLICT, conflicto.getStatus());
            pagoService.resolverAdicional(alquiler.getId(), clave);
            assertArrayEquals(antesReintento, contarEscriturasDevolucionMysql());
            when(reloj.instant()).thenReturn(instantePagos.plus(62, ChronoUnit.MINUTES).plusSeconds(21));
            ResultadoAdicionalDTO confirmado = pagoService.resolverAdicional(alquiler.getId(), clave);
            assertEquals(inicial.getPagoId(), confirmado.getPagoId());
            pagoFinal = confirmado.getPagoId();
        } else {
            entorno.getPropertySources().remove(PROPIEDADES_PAGOS_MYSQL);
            String otraClave = UUID.randomUUID().toString();
            ResultadoAdicionalDTO nuevo = pagoService.iniciarAdicional(alquiler.getId(), otraClave);
            assertNotEquals(inicial.getPagoId(), nuevo.getPagoId());
            pagoFinal = pagoService.resolverAdicional(alquiler.getId(), otraClave).getPagoId();
            transaccion.executeWithoutResult(estado -> assertEquals("RECHAZADO",
                    entityManager.find(Pago.class, inicial.getPagoId()).getEstadoPago().getCodigo()));
        }

        comprobarAdicionalAprobadoMysql(alquiler.getId(), pagoFinal, alquiler.getFechaHoraLiquidacion());
        assertEquals(0L, contarDeudasMysql());
        assertEquals(new BigDecimal("972460.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
    }

    @Test
    void adicionalesConcurrentesMysqlNoDuplicanCobroNiHistorias() throws Exception {
        AlquilerCabeceraDTO alquiler = crearAlquilerConDeudaMysql();
        String clave = UUID.randomUUID().toString();
        long[] antes = contarEscriturasDevolucionMysql();
        List<ResultadoAdicionalDTO> inicios = ejecutarDosOperaciones(
                () -> pagoService.iniciarAdicional(alquiler.getId(), clave));
        assertEquals(inicios.get(0).getPagoId(), inicios.get(1).getPagoId());
        List<ResultadoAdicionalDTO> resultados = ejecutarDosOperaciones(
                () -> pagoService.resolverAdicional(alquiler.getId(), clave));
        for (ResultadoAdicionalDTO resultado : resultados) {
            assertEquals("APROBADO", resultado.getEstadoPago());
            assertEquals("SALDADO", resultado.getLiquidacion().getSituacionFinanciera());
        }
        assertEquals(resultados.get(0).getPagoId(), resultados.get(1).getPagoId());
        long[] esperados = antes.clone();
        esperados[6]++;
        esperados[8] += 3;
        assertArrayEquals(esperados, contarEscriturasDevolucionMysql());
        comprobarAdicionalAprobadoMysql(alquiler.getId(), resultados.get(0).getPagoId(),
                alquiler.getFechaHoraLiquidacion());
        assertEquals(new BigDecimal("972460.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
        assertEquals(0L, contarDeudasMysql());
    }

    @Test
    void falloAdicionalMysqlRevierteConfirmacionHistoriasYDescuento() {
        AlquilerCabeceraDTO alquiler = crearAlquilerConDeudaMysql();
        String clave = UUID.randomUUID().toString();
        ResultadoAdicionalDTO inicio = pagoService.iniciarAdicional(alquiler.getId(), clave);
        long[] antes = contarEscriturasDevolucionMysql();

        // Falla al devolver el resumen, después del pago y sus historias ya enviados a SQL.
        doAnswer(invocacion -> {
            LiquidacionAlquilerDTO vista = (LiquidacionAlquilerDTO) invocacion.callRealMethod();
            if ("SALDADO".equals(vista.getSituacionFinanciera())) {
                throw new IllegalStateException("FALLO_ADICIONAL_MYSQL_QA");
            }
            return vista;
        }).when(liquidacionEspiada).consultarLiquidacion(alquiler.getId());

        IllegalStateException fallo = assertThrows(IllegalStateException.class,
                () -> pagoService.resolverAdicional(alquiler.getId(), clave));
        assertEquals("FALLO_ADICIONAL_MYSQL_QA", fallo.getMessage());
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());
        transaccion.executeWithoutResult(estado -> {
            Pago guardado = entityManager.find(Pago.class, inicio.getPagoId());
            assertEquals("CREADO", guardado.getEstadoPago().getCodigo());
            assertNull(guardado.getFechaConfirmacion());
            assertNull(guardado.getReferenciaExterna());
            assertEquals(1, historialPagoRepository.findByPagoIdOrderByIdAsc(guardado.getId()).size());
        });
        assertEquals(1L, contarDeudasMysql());
        assertEquals(new BigDecimal("979345.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());

        reset(liquidacionEspiada);
        assertEquals("APROBADO", pagoService.resolverAdicional(alquiler.getId(), clave).getEstadoPago());
        comprobarAdicionalAprobadoMysql(alquiler.getId(), inicio.getPagoId(), alquiler.getFechaHoraLiquidacion());
        assertEquals(0L, contarDeudasMysql());
    }

    @Test
    void reembolsoMysqlAcreditaUnaSolaVezYConservaPagoOriginal() throws Exception {
        AlquilerCabeceraDTO a = crearAlquilerConReintegroMysql();
        assertEquals(new BigDecimal("979345.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
        var pendiente = reembolsoService.consultar(a.getId());
        assertEquals(1, pendiente.getReembolsos().size());
        assertEquals("PENDIENTE", pendiente.getReembolsos().get(0).getEstado());
        assertEquals(new BigDecimal("6885.00"), pendiente.getLiquidacion().getReembolsoPendienteArs());
        var resultados = ejecutarDosOperaciones(() -> reembolsoService.resolver(a.getId()));
        assertEquals("SALDADO", resultados.get(0).getLiquidacion().getSituacionFinanciera());
        long[] antes = contarEscriturasDevolucionMysql();
        reembolsoService.resolver(a.getId());
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());
        assertEquals(new BigDecimal("986230.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
        transaccion.executeWithoutResult(tx -> {
            var pagos = pagoRepository.findByAlquilerIdOrderByIdAsc(a.getId());
            assertEquals(1, pagos.size());
            assertEquals("APROBADO", pagos.get(0).getEstadoPago().getCodigo());
            assertEquals(new BigDecimal("20655.00"), pagos.get(0).getMontoArs());
            var r = reembolsoRepository.findByPagoAlquilerIdOrderByIdAsc(a.getId()).get(0);
            assertEquals(3, historialReembolsoRepository.findByReembolsoIdOrderByIdAsc(r.getId()).size());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN_PROCESO", "FALLIDO"})
    void reembolsoMysqlInciertoOFallidoNoAcreditaYReintentaMismaObligacion(String modo) {
        AlquilerCabeceraDTO a = crearAlquilerConReintegroMysql();
        String nick = transaccion.execute(tx -> entityManager.find(Usuario.class, usuarioId).getNickname());
        String prefijo = "pagos.simulados.cuentas." + nick + ".";
        entorno.getPropertySources().addFirst(new MapPropertySource(PROPIEDADES_PAGOS_MYSQL,
                Map.<String,Object>of(prefijo + "reembolso-resultado", modo, prefijo + "demora-segundos", "9999")));
        var resultado = reembolsoService.resolver(a.getId());
        var r = resultado.getReembolsos().get(0);
        assertEquals(modo, r.getEstado());
        assertNull(r.getFechaConfirmacion());
        assertEquals(new BigDecimal("979345.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
        entorno.getPropertySources().remove(PROPIEDADES_PAGOS_MYSQL);
        var confirmado = reembolsoService.resolver(a.getId());
        assertEquals(r.getId(), confirmado.getReembolsos().get(0).getId());
        assertEquals("CONFIRMADO", confirmado.getReembolsos().get(0).getEstado());
        assertEquals(new BigDecimal("986230.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
    }

    @Test
    void falloTrasConfirmarReembolsoMysqlRevierteCreditoEHistorias() {
        AlquilerCabeceraDTO a = crearAlquilerConReintegroMysql();
        long[] antes = contarEscriturasDevolucionMysql();
        doAnswer(inv -> {
            entityManager.flush();
            var lista = reembolsoRepository.findByPagoAlquilerIdOrderByIdAsc(a.getId());
            if (lista.stream().anyMatch(r -> "CONFIRMADO".equals(r.getEstado()))) throw new IllegalStateException("Fallo QA tras confirmar");
            return inv.callRealMethod();
        }).when(liquidacionEspiada).resumenInterno(any(Alquiler.class));
        assertThrows(IllegalStateException.class, () -> reembolsoService.resolver(a.getId()));
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());
        reset(liquidacionEspiada);
        assertEquals("PENDIENTE", reembolsoService.consultar(a.getId()).getReembolsos().get(0).getEstado());
        assertEquals("SALDADO", reembolsoService.resolver(a.getId()).getLiquidacion().getSituacionFinanciera());
    }

    @Test
    void cancelarReservaPagadaMysqlConservaLugarYGeneraReintegroTotal() {
        AlquilerCabeceraDTO a = crearReservaPagadaMysql();
        AlquilerCabeceraDTO cancelado = alquilerService.cancelarReserva(a.getId(), null, null, false);
        assertEquals("CANCELADO", cancelado.getEstado());
        assertEquals(new BigDecimal("0.00"), cancelado.getCostoFinalArs());
        assertEquals(a.getCostoEstimadoArs(), cancelado.getCostoEstimadoArs());
        assertTrue(cancelado.getDetalles().stream().allMatch(d -> d.getFechaHoraRetiro() == null));
        transaccion.executeWithoutResult(tx -> {
            comprobarBicicletas("DISPONIBLE");
            assertEquals(2L, ocupacionEstacionMysql());
        });
        assertEquals(new BigDecimal("20655.00"), reembolsoService.consultar(a.getId()).getLiquidacion().getReembolsoPendienteArs());
        reembolsoService.resolver(a.getId());
        assertEquals(new BigDecimal("1000000.00"), pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
        long[] antes = contarEscriturasDevolucionMysql();
        alquilerService.cancelarReserva(a.getId(), null, null, false);
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());
    }

    @Test
    void vencimientoMysqlEnLimiteEsAutomaticoIdempotenteYSinActorInventado() {
        int alquilerId = alquilerService.crearReserva(solicitud());
        when(reloj.instant()).thenReturn(instantePagos.plus(15, ChronoUnit.MINUTES).minusNanos(1000));
        assertEquals(0, alquilerService.vencerReserva(alquilerId));
        when(reloj.instant()).thenReturn(instantePagos.plus(15, ChronoUnit.MINUTES));
        SecurityContextHolder.clearContext();
        assertEquals(2, alquilerService.vencerReserva(alquilerId));
        long[] antes = contarEscriturasDevolucionMysql();
        assertEquals(0, alquilerService.vencerReserva(alquilerId));
        assertArrayEquals(antes, contarEscriturasDevolucionMysql());
        transaccion.executeWithoutResult(tx -> {
            Alquiler a = entityManager.find(Alquiler.class, alquilerId);
            assertEquals("VENCIDO", a.getEstadoContratacion().getCodigo());
            assertEquals(new BigDecimal("0.00"), a.getCostoFinalArs());
            assertTrue(pagoRepository.findByAlquilerIdOrderByIdAsc(alquilerId).isEmpty());
            historiasDetalleMysql(alquilerId).stream().filter(h -> "VENCIDO".equals(h.getEstadoNuevo().getCodigo())).forEach(h -> {
                assertEquals("SISTEMA", h.getOrigen()); assertNull(h.getActorUsuario());
            });
            for (int bici : List.of(bicicleta1Id, bicicleta2Id)) {
                var h = historiasBicicletaMysql(bici);
                assertEquals("SISTEMA", h.get(h.size()-1).getOrigen());
                assertNull(h.get(h.size()-1).getActorUsuario());
            }
            comprobarBicicletas("DISPONIBLE"); assertEquals(2L, ocupacionEstacionMysql());
        });
    }

    @Test
    void cancelacionParcialMysqlNoDetieneUsoYUltimaDevolucionLiquida() {
        AlquilerCabeceraDTO a = crearReservaPagadaMysql();
        retirarParaDevolucionMysql(a);
        var parcial = alquilerService.cancelarReserva(a.getId(), a.getDetalles().get(1).getId(), null, false);
        assertEquals("EN_PROGRESO", parcial.getEstado());
        assertNull(parcial.getFechaHoraLiquidacion());
        when(reloj.instant()).thenReturn(instantePagos.plus(32, ChronoUnit.MINUTES));
        alquilerService.devolverBicicleta(a.getId(), a.getDetalles().get(0).getId(), solicitudDevolucionMysql(false));
        var cerrado = alquilerService.obtenerAlquiler(a.getId());
        assertEquals("FINALIZADO", cerrado.getEstado());
        assertNotNull(cerrado.getFechaHoraLiquidacion());
        assertEquals(1, reembolsoService.consultar(a.getId()).getReembolsos().size());
    }

    @Test
    void httpMysqlCreaReservaNuevaYCancelaConAutorizacionReal() throws Exception {
        String cuerpo = "{\"estacionRetiroId\":" + estacionId + ",\"bicicletaIds\":[" + bicicleta1Id + "," + bicicleta2Id +
                "],\"duracionEstimadaSegundos\":2700}";
        var sesion = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(email).authorities(new SimpleGrantedAuthority("CLIENTE"));
        var respuesta = http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/alquileres")
                .with(sesion).contentType("application/json").content(cuerpo))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.estado").value("RESERVADO"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.detalles.length()").value(2)).andReturn();
        int alquilerId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(respuesta.getResponse().getContentAsString()).get("id").asInt();
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/alquileres/" + alquilerId + "/cancelar").with(sesion))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.estado").value("CANCELADO"));
        http.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/pagos/reembolsos/" + alquilerId).with(sesion))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.liquidacion.situacionFinanciera").value("SALDADO"));
    }

    @Test
    void preventivaVencidaMysqlBloqueaReservaYAlLiberarGeneraOrdenSinMoverLugar() {
        AlquilerCabeceraDTO a = crearReservaPagadaMysql();
        // Fixture SQL: fechaAlta es inmutable por JPA. No quitar updatable=false del modelo.
        transaccion.executeWithoutResult(tx -> entityManager.createNativeQuery("update bicicleta set fecha_alta=:fecha where id=:id")
                .setParameter("fecha",LocalDate.ofInstant(instantePagos,ZoneOffset.UTC).minusYears(2))
                .setParameter("id",bicicleta1Id).executeUpdate());
        alquilerService.cancelarReserva(a.getId(), null, null, false);
        transaccion.executeWithoutResult(tx -> {
            Bicicleta bici = entityManager.find(Bicicleta.class,bicicleta1Id);
            assertEquals("MANTENIMIENTO",bici.getEstado().getCodigo());
            assertEquals(estacionId,bici.getUbicacionActual().getId());
            var ordenes = entityManager.createQuery("select o from OrdenMantenimiento o where o.bicicleta.id=:b", OrdenMantenimiento.class)
                    .setParameter("b",bicicleta1Id).getResultList();
            assertEquals(1,ordenes.size());
            assertEquals("PREVENTIVO",ordenes.get(0).getTipoMantenimiento().getCodigo());
            // Aunque alguien olvidara cambiar el estado visible, el calendario no permite otra reserva.
            bici.setEstado(catalogo(EstadoBicicleta.class,"DISPONIBLE"));
        });
        assertThrows(IllegalStateException.class, () -> alquilerService.crearReserva(solicitud()));
    }

    @Test
    void pendientesFinancierosMysqlIncluyenReintegroAunqueAlquilerEsteOculto() {
        AlquilerCabeceraDTO a = crearAlquilerConReintegroMysql();
        // Preparar un registro ya ocultado por ADMIN; no concederle ese permiso al cliente del test.
        transaccion.executeWithoutResult(tx -> entityManager.find(Alquiler.class,a.getId()).setBorradoAlquiler(true));
        assertTrue(liquidacionService.listarPendientesPropios().stream().anyMatch(l -> l.getAlquilerId()==a.getId()));
        reembolsoService.resolver(a.getId());
        assertTrue(liquidacionService.listarPendientesPropios().isEmpty());
    }

    @Test
    void consultaDeVencimientosMysqlExcluyeTodosRetiradosYNoDependeDelNavegador() {
        AlquilerCabeceraDTO a = crearReservaRetiradaMysql();
        when(reloj.instant()).thenReturn(instantePagos.plus(16,ChronoUnit.MINUTES));
        var pagina = org.springframework.data.domain.PageRequest.of(0,100);
        assertFalse(alquilerRepository.buscarReservasVencidas(LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC),pagina).contains(a.getId()));
        // Las unidades siguen en uso: no hay cierre artificial a los quince minutos.
        assertEquals(0,alquilerService.vencerReserva(a.getId()));
        assertEquals("EN_PROGRESO",alquilerService.obtenerAlquiler(a.getId()).getEstado());
    }

    @Test
    void nuevaReservaMysqlRechazaPreventivaAunqueNoHayaOrdenNiCambioVisible() {
        transaccion.executeWithoutResult(tx -> entityManager.createNativeQuery("update bicicleta set fecha_alta=:fecha where id=:id")
                .setParameter("fecha",LocalDate.ofInstant(instantePagos,ZoneOffset.UTC).minusYears(2))
                .setParameter("id",bicicleta1Id).executeUpdate());
        long[] antes = contarEscriturasDevolucionMysql();
        assertThrows(IllegalStateException.class, () -> alquilerService.crearReserva(solicitud()));
        assertArrayEquals(antes,contarEscriturasDevolucionMysql());
    }

    @Test
    @EnabledIfEnvironmentVariable(named="TURISMO_TEST_WEB", matches="true")
    void navegadorRealReservaPagaRetiraDevuelveYRecibeReintegro() throws Exception {
        String base = "http://localhost:" + puertoWeb;
        String nickname = transaccion.execute(tx -> entityManager.find(Usuario.class,usuarioId).getNickname());
        try (com.microsoft.playwright.Playwright pw = com.microsoft.playwright.Playwright.create();
             com.microsoft.playwright.Browser browser = pw.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
             com.microsoft.playwright.BrowserContext contexto = browser.newContext()) {
            var login = contexto.request().post(base + "/api/login", com.microsoft.playwright.options.RequestOptions.create()
                    .setForm(com.microsoft.playwright.options.FormData.create().set("nickname",nickname).set("password","PruebaMysql?1")));
            assertEquals(200,login.status());
            var page = contexto.newPage();
            page.setDefaultTimeout(20000);
            List<String> erroresJs = new ArrayList<>();
            page.onPageError(erroresJs::add);
            page.navigate(base + "/alquiler.html");
            page.locator("#estaciones-container button[data-estacion-id='" + estacionId + "']").click();
            page.locator("#bicicletas-container input[data-bicicleta-id='" + bicicleta1Id + "']").check();
            page.locator("#bicicletas-container input[data-bicicleta-id='" + bicicleta2Id + "']").check();
            page.fill("#duracion-reserva","45");
            page.click("#crear-reserva"); page.click(".swal2-confirm");
            page.waitForSelector(".swal2-title:text-is('Reserva creada')");
            page.click(".swal2-confirm");
            page.waitForURL("**/mis_alquileres.html");
            page.waitForSelector("button:has-text('Pagar/consultar anticipo simulado')");
            int alquilerId = transaccion.execute(tx -> entityManager.createQuery(
                    "select a.id from Alquiler a where a.usuario.id=:u order by a.id desc",Integer.class)
                    .setParameter("u",usuarioId).setMaxResults(1).getSingleResult());
            page.click("button:has-text('Pagar/consultar anticipo simulado')");
            page.waitForSelector(".swal2-html-container:has-text('Pago SIMULADO')");
            page.click(".swal2-confirm");
            page.waitForSelector(".swal2-html-container:has-text('APROBADO')");
            page.click(".swal2-confirm");
            AlquilerCabeceraDTO a = alquilerService.obtenerAlquiler(alquilerId);
            when(reloj.instant()).thenReturn(instantePagos.plus(2,ChronoUnit.MINUTES));
            page.locator("details[data-alquiler-detalles='" + alquilerId + "'] summary").click();
            for (var d : a.getDetalles()) {
                page.click("button[onclick='OperacionesAlquiler.retirar(" + alquilerId + "," + d.getId() + ")']");
                page.click(".swal2-confirm");
                page.waitForSelector(".swal2-title:text-is('Operación registrada')"); page.click(".swal2-confirm");
            }
            when(reloj.instant()).thenReturn(instantePagos.plus(32,ChronoUnit.MINUTES));
            for (var d : a.getDetalles()) {
                page.click("button[onclick='OperacionesAlquiler.devolver(" + alquilerId + "," + d.getId() + ")']");
                page.selectOption(".swal2-select",String.valueOf(estacionId)); page.click(".swal2-confirm");
                page.waitForSelector(".swal2-title:text-is('¿La bicicleta tiene una falla?')"); page.click(".swal2-deny");
                page.waitForSelector(".swal2-title:text-is('Bicicleta devuelta')"); page.click(".swal2-confirm");
            }
            page.locator("button[onclick='OperacionesAlquiler.finanzas(" + alquilerId + ")']").first().click();
            page.waitForSelector(".swal2-html-container:has-text('REEMBOLSO_PENDIENTE')");
            page.click(".swal2-deny");
            page.waitForSelector(".swal2-html-container:has-text('CONFIRMADO')"); page.click(".swal2-confirm");
            page.waitForSelector(".swal2-container",new com.microsoft.playwright.Page.WaitForSelectorOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN));
            page.evaluate("window.scrollTo(0,0)");
            page.waitForFunction("window.scrollY === 0");
            assertEquals("FINALIZADO",alquilerService.obtenerAlquiler(alquilerId).getEstado());
            assertEquals(new BigDecimal("986230.00"),pagoService.consultarSaldoSimulado().getSaldoDisponibleArs());
            assertTrue(erroresJs.isEmpty(),erroresJs.toString());
            java.nio.file.Path carpeta = java.nio.file.Path.of("build/reports/tests/navegador");
            java.nio.file.Files.createDirectories(carpeta);
            page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setPath(carpeta.resolve("alquiler-circuito-completo.png")).setFullPage(true));
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named="TURISMO_TEST_WEB", matches="true")
    void navegadorAdminSinClienteCancelaConMotivoYConsultaDineroSinCobrar() {
        AlquilerCabeceraDTO a = crearReservaPagadaMysql();
        String nickname = transaccion.execute(tx -> {
            Usuario u = entityManager.find(Usuario.class,usuarioId);
            entityManager.createQuery("delete from UsuarioRol ur where ur.usuario.id=:u").setParameter("u",usuarioId).executeUpdate();
            entityManager.persist(new UsuarioRol(u,catalogo(Rol.class,"ADMIN")));
            return u.getNickname();
        });
        autenticarListado("ADMIN");
        String base = "http://localhost:" + puertoWeb;
        try (com.microsoft.playwright.Playwright pw = com.microsoft.playwright.Playwright.create();
             com.microsoft.playwright.Browser browser = pw.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
             com.microsoft.playwright.BrowserContext contexto = browser.newContext()) {
            var login = contexto.request().post(base + "/api/login",com.microsoft.playwright.options.RequestOptions.create()
                    .setForm(com.microsoft.playwright.options.FormData.create().set("nickname",nickname).set("password","PruebaMysql?1")));
            assertEquals(200,login.status());
            var page = contexto.newPage(); page.setDefaultTimeout(20000);
            List<String> errores = new ArrayList<>(); page.onPageError(errores::add);
            page.navigate(base + "/admin.html");
            // La tabla de alquileres está en su pestaña, no en la vista inicial de usuarios.
            page.locator("[data-bs-target='#tab-alquileres']").click();
            page.locator("details[data-alquiler-detalles='" + a.getId() + "'] summary").click();
            assertEquals(0,page.locator("#tabla-alquileres-body button[onclick*='.anticipo(']").count());
            assertEquals(0,page.locator("#tabla-alquileres-body button[onclick*='.retirar(']").count());
            for (var d : a.getDetalles()) {
                page.click("button[onclick='OperacionesAlquiler.cancelarAdmin(" + a.getId() + "," + d.getId() + ")']");
                page.fill(".swal2-textarea","Falla operativa detectada por administración.");
                page.click(".swal2-confirm");
                page.selectOption(".swal2-select","INACTIVA"); page.click(".swal2-confirm");
                page.waitForSelector(".swal2-title:text-is('Cancelación registrada')"); page.click(".swal2-confirm");
            }
            page.locator("button[onclick='OperacionesAlquiler.finanzas(" + a.getId() + ")']").click();
            page.waitForSelector(".swal2-html-container:has-text('REEMBOLSO_PENDIENTE')");
            assertFalse(page.locator(".swal2-deny").isVisible(),"ADMIN no puede resolver pagos ni reintegros de clientes.");
            page.click(".swal2-confirm");
            assertEquals("CANCELADO",alquilerService.obtenerAlquiler(a.getId()).getEstado());
            transaccion.executeWithoutResult(tx -> {
                for (int bici : List.of(bicicleta1Id,bicicleta2Id)) {
                    Bicicleta b = entityManager.find(Bicicleta.class,bici);
                    assertEquals("INACTIVA",b.getEstado().getCodigo());
                    assertEquals(estacionId,b.getUbicacionActual().getId());
                }
            });
            assertEquals(403,contexto.request().post(base + "/api/pagos/reembolsos/" + a.getId() + "/resolver").status());
            assertTrue(errores.isEmpty(),errores.toString());
        }
    }

    private AlquilerCabeceraDTO crearAlquilerConReintegroMysql() {
        AlquilerCabeceraDTO a = crearReservaRetiradaMysql();
        when(reloj.instant()).thenReturn(instantePagos.plus(32, ChronoUnit.MINUTES));
        for (AlquilerDetalleDTO d : a.getDetalles()) alquilerService.devolverBicicleta(a.getId(), d.getId(), solicitudDevolucionMysql(false));
        return alquilerService.obtenerAlquiler(a.getId());
    }

    private AlquilerCabeceraDTO crearAlquilerConDeudaMysql() {
        AlquilerCabeceraDTO reserva = crearReservaRetiradaMysql();
        when(reloj.instant()).thenReturn(instantePagos.plus(62, ChronoUnit.MINUTES));
        for (AlquilerDetalleDTO detalle : reserva.getDetalles()) {
            alquilerService.devolverBicicleta(reserva.getId(), detalle.getId(), solicitudDevolucionMysql(false));
        }
        assertEquals(1L, contarDeudasMysql());
        return alquilerService.obtenerAlquiler(reserva.getId());
    }

    private void comprobarAdicionalAprobadoMysql(int alquilerId, int pagoId, LocalDateTime fechaLiquidacion) {
        transaccion.executeWithoutResult(estado -> {
            Pago pago = entityManager.find(Pago.class, pagoId);
            assertEquals(alquilerId, pago.getAlquiler().getId());
            assertEquals("ADICIONAL", pago.getFinalidad());
            assertEquals("APROBADO", pago.getEstadoPago().getCodigo());
            assertEquals("SIMULADA", pago.getPlataforma());
            assertEquals("PRUEBA", pago.getEntorno());
            assertEquals("ARS", pago.getMoneda());
            assertEquals(new BigDecimal("6885.00"), pago.getMontoArs());
            assertNotNull(pago.getFechaConfirmacion());
            List<HistorialEstadoPago> eventos = historialPagoRepository.findByPagoIdOrderByIdAsc(pagoId);
            assertEquals(3, eventos.size());
            assertEquals("CREADO", eventos.get(0).getEstadoNuevo().getCodigo());
            assertEquals("USUARIO", eventos.get(0).getOrigen());
            assertEquals(usuarioId, eventos.get(0).getActorUsuario().getId());
            assertEquals("PENDIENTE", eventos.get(1).getEstadoNuevo().getCodigo());
            assertEquals("APROBADO", eventos.get(2).getEstadoNuevo().getCodigo());
            assertEquals("SISTEMA", eventos.get(2).getOrigen());
            assertNull(eventos.get(2).getActorUsuario());
            Alquiler guardado = entityManager.find(Alquiler.class, alquilerId);
            assertEquals("FINALIZADO", guardado.getEstadoContratacion().getCodigo());
            assertEquals(new BigDecimal("18.00"), guardado.getCostoFinalUsd());
            assertEquals(new BigDecimal("27540.00"), guardado.getCostoFinalArs());
            assertEquals(fechaLiquidacion, guardado.getFechaHoraLiquidacion());
            assertEquals(fechaLiquidacion, guardado.getFechaHoraCierre());
            comprobarBicicletas("DISPONIBLE");
        });
        assertEquals("SALDADO", liquidacionService.consultarLiquidacion(alquilerId).getSituacionFinanciera());
    }

    private AlquilerCabeceraDTO crearReservaRetiradaMysql() {
        AlquilerCabeceraDTO reserva = crearReservaPagadaMysql();

        retirarParaDevolucionMysql(reserva);

        alquilerService.retirarBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(1).getId()
        );

        // Releer para conservar fechas y versiones posteriores a ambos retiros.
        return alquilerService.obtenerAlquiler(reserva.getId());
    }

    private long contarDeudasMysql() {
        return transaccion.execute(estado ->
                alquilerRepository.contarAlquileresConSaldoPendiente(
                        usuarioId
                )
        );
    }

    private AlquilerDetalleDTO retirarParaDevolucionMysql(
            AlquilerCabeceraDTO reserva
    ) {
        when(reloj.instant()).thenReturn(
                instantePagos.plus(2, ChronoUnit.MINUTES)
        );
        return alquilerService.retirarBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(0).getId()
        );
    }

    private DevolverBicicletaDTO solicitudDevolucionMysql(boolean falla) {
        DevolverBicicletaDTO solicitud = new DevolverBicicletaDTO();
        solicitud.setEstacionDevolucionId(estacionId);
        solicitud.setFallaReportada(falla);
        if (falla) {
            solicitud.setDescripcionFalla("No frena.");
        }
        return solicitud;
    }

    private long[] contarEscriturasDevolucionMysql() {
        return transaccion.execute(estado -> {
            List<String> entidades = List.of(
                    "Alquiler", "AlquilerDetalle",
                    "HistorialEstadoDetalle", "HistorialEstadoBicicleta",
                    "OrdenMantenimiento", "HistorialOrdenMantenimiento",
                    "Pago", "Reembolso",
                    "HistorialEstadoPago", "HistorialEstadoReembolso"
            );
            long[] cantidades = new long[entidades.size()];
            for (int i = 0; i < entidades.size(); i++) {
                cantidades[i] = entityManager.createQuery(
                        "select count(e) from " + entidades.get(i) + " e",
                        Long.class
                ).getSingleResult();
            }
            return cantidades;
        });
    }

    private void comprobarRecepcionMysql(
            AlquilerCabeceraDTO reserva,
            AlquilerDetalleDTO retiro,
            AlquilerDetalleDTO respuesta,
            Instant instante,
            String estadoBicicleta,
            long ocupacion,
            String estadoCabecera
    ) {
        transaccion.executeWithoutResult(estado -> {
            LocalDateTime fecha =
                    LocalDateTime.ofInstant(instante, ZoneOffset.UTC);
            AlquilerDetalle detalle = alquilerDetalleRepository
                    .findById(respuesta.getId()).orElseThrow();

            assertEquals(reserva.getId(), detalle.getAlquiler().getId());
            assertEquals(retiro.getBicicletaId(), detalle.getBicicleta().getId());
            assertEquals("DEVUELTO", detalle.getEstado().getCodigo());
            assertEquals(respuesta.getVersion(), detalle.getVersion());
            assertTrue(detalle.getVersion() > retiro.getVersion());
            assertEquals(retiro.getFechaHoraRetiro(), detalle.getFechaHoraRetiro());
            assertEquals(
                    retiro.getFechaHoraDevolucionEstimada(),
                    detalle.getFechaHoraDevolucionEstimada()
            );
            assertEquals(fecha, detalle.getFechaHoraDevolucionReal());
            assertEquals(fecha, detalle.getFechaHoraFinUso());
            assertEquals(fecha, detalle.getFechaHoraCierreDetalle());
            assertEquals(fecha, respuesta.getFechaHoraDevolucionReal());
            assertEquals(estacionId, detalle.getEstacionDevolucion().getId());
            assertEquals("CLIENTE", detalle.getOrigenCierre());

            assertEquals(
                    retiro.getPrecioPorHoraAplicadoUsd(),
                    detalle.getPrecioPorHoraAplicadoUsd()
            );
            assertEquals(
                    retiro.getSubtotalEstimadoArs(),
                    detalle.getSubtotalEstimadoArs()
            );
            assertEquals(respuesta.getSubtotalRealUsd(), detalle.getSubtotalRealUsd());
            assertEquals(respuesta.getSubtotalRealArs(), detalle.getSubtotalRealArs());

            assertEquals(
                    estadoBicicleta,
                    detalle.getBicicleta().getEstado().getCodigo()
            );
            assertEquals(
                    estacionId,
                    detalle.getBicicleta().getUbicacionActual().getId()
            );
            assertEquals(ocupacion, ocupacionEstacionMysql());

            Alquiler alquiler = detalle.getAlquiler();
            assertEquals(estadoCabecera, alquiler.getEstadoContratacion().getCodigo());
            assertEquals(reserva.getCotizacionArsPorUsd(), alquiler.getCotizacionArsPorUsd());
            assertEquals(reserva.getCostoEstimadoArs(), alquiler.getCostoEstimadoArs());
            if ("FINALIZADO".equals(estadoCabecera)) {
                assertNotNull(alquiler.getCostoFinalUsd());
                assertNotNull(alquiler.getCostoFinalArs());
                assertEquals(fecha, alquiler.getFechaHoraCierre());
                assertEquals(fecha, alquiler.getFechaHoraLiquidacion());
            } else {
                assertNull(alquiler.getCostoFinalUsd());
                assertNull(alquiler.getCostoFinalArs());
                assertNull(alquiler.getFechaHoraLiquidacion());
                assertNull(alquiler.getFechaHoraCierre());
            }

            List<HistorialEstadoDetalle> historias = historiasDetalleMysql(reserva.getId())
                    .stream()
                    .filter(h -> h.getAlquilerDetalle().getId().equals(detalle.getId()))
                    .collect(Collectors.toList());

            assertEquals(4, historias.size());
            HistorialEstadoDetalle devolucion = historias.get(3);
            assertEquals("EN_CURSO", devolucion.getEstadoAnterior().getCodigo());
            assertEquals("DEVUELTO", devolucion.getEstadoNuevo().getCodigo());
            assertEquals("USUARIO", devolucion.getOrigen());
            assertEquals(usuarioId, devolucion.getActorUsuario().getId());
            assertEquals(fecha, devolucion.getFechaHoraCambio());

            boolean demora = fecha.isAfter(retiro.getFechaHoraDevolucionEstimada());
            assertTrue(devolucion.getObservaciones().contains(
                    demora ? "con demora" : "dentro del tiempo estimado"
            ));

            List<HistorialEstadoBicicleta> historiasBici =
                    historiasBicicletaMysql(retiro.getBicicletaId());
            assertEquals(3, historiasBici.size());
            HistorialEstadoBicicleta ultima = historiasBici.get(2);
            assertEquals("EN_USO", ultima.getEstadoAnterior().getCodigo());
            assertEquals(estadoBicicleta, ultima.getEstadoNuevo().getCodigo());
            assertEquals(usuarioId, ultima.getActorUsuario().getId());
            assertEquals(fecha.minusHours(3), ultima.getFechaHoraCambio());
        });
    }

    private void comprobarOrdenDevolucionMysql(
            Integer detalleId,
            String tipo,
            LocalDateTime fecha
    ) {
        transaccion.executeWithoutResult(estado -> {
            AlquilerDetalle detalle = alquilerDetalleRepository
                    .findById(detalleId).orElseThrow();
            OrdenMantenimiento orden = detalle.getOrdenMantenimiento();

            assertNotNull(orden);
            assertNotNull(orden.getId());
            assertEquals(detalle.getBicicleta().getId(), orden.getBicicleta().getId());
            assertEquals(tipo, orden.getTipoMantenimiento().getCodigo());
            assertEquals("CREADA", orden.getEstado().getCodigo());
            assertNull(orden.getTecnico());
            assertEquals(fecha, orden.getFechaCreacion());

            long cantidad = entityManager.createQuery(
                    "select count(o) from OrdenMantenimiento o "
                            + "where o.bicicleta.id = :id",
                    Long.class
            ).setParameter("id", detalle.getBicicleta().getId()).getSingleResult();
            assertEquals(1L, cantidad);

            List<HistorialOrdenMantenimiento> historias = entityManager.createQuery(
                    "select h from HistorialOrdenMantenimiento h "
                            + "where h.ordenMantenimiento.id = :id order by h.id",
                    HistorialOrdenMantenimiento.class
            ).setParameter("id", orden.getId()).getResultList();

            assertEquals(1, historias.size());
            assertEquals("CREAR", historias.get(0).getAccion());
            assertEquals("SISTEMA", historias.get(0).getOrigen());
            assertNull(historias.get(0).getActorUsuario());
            assertEquals(fecha, historias.get(0).getFechaHoraCambio());
        });
    }

    private void comprobarAnticipoMysql(
            int alquilerId,
            Integer pagoId,
            String clave
    ) {
        // Las relaciones e historias se leen desde una transacción nueva.
        transaccion.executeWithoutResult(estado -> {
            List<Pago> pagos = pagoRepository
                    .findByAlquilerIdOrderByIdAsc(alquilerId);
            assertEquals(1, pagos.size());

            Pago pago = pagos.get(0);
            assertEquals(pagoId, pago.getId());
            assertEquals(alquilerId, pago.getAlquiler().getId());
            assertEquals(usuarioId, pago.getAlquiler().getUsuario().getId());
            assertEquals("ANTICIPO", pago.getFinalidad());
            assertEquals("APROBADO", pago.getEstadoPago().getCodigo());
            assertEquals("ARS", pago.getMoneda());
            assertEquals("SIMULADA", pago.getPlataforma());
            assertEquals("PRUEBA", pago.getEntorno());
            assertEquals(new BigDecimal("20655.00"), pago.getMontoArs());
            assertEquals(
                    "ANTICIPO:" + usuarioId + ":" + clave,
                    pago.getClaveIdempotencia()
            );
            assertNotNull(pago.getFechaConfirmacion());
            assertNotNull(pago.getVersion());

            List<HistorialEstadoPago> historias = historialPagoRepository
                    .findByPagoIdOrderByIdAsc(pagoId);
            assertEquals(
                    List.of("CREADO", "PENDIENTE", "APROBADO"),
                    historias.stream()
                            .map(evento -> evento.getEstadoNuevo().getCodigo())
                            .collect(Collectors.toList())
            );
            assertNull(historias.get(0).getEstadoAnterior());
            assertEquals("USUARIO", historias.get(0).getOrigen());
            assertEquals(usuarioId, historias.get(0).getActorUsuario().getId());
            assertEquals("CREADO", historias.get(1).getEstadoAnterior().getCodigo());
            assertEquals("PENDIENTE", historias.get(2).getEstadoAnterior().getCodigo());

            for (HistorialEstadoPago historia : historias) {
                assertEquals(pagoId, historia.getPago().getId());
                assertNotNull(historia.getFechaHoraCambio());
                if (!"CREADO".equals(historia.getEstadoNuevo().getCodigo())) {
                    assertEquals("SISTEMA", historia.getOrigen());
                    assertNull(historia.getActorUsuario());
                }
            }

            Alquiler alquiler = alquilerRepository.findById(alquilerId)
                    .orElseThrow();
            assertEquals("RESERVADO", alquiler.getEstadoContratacion().getCodigo());
            assertEquals(new BigDecimal("20655.00"), alquiler.getCostoEstimadoArs());
            assertEquals(0, new BigDecimal("1530").compareTo(
                    alquiler.getCotizacionArsPorUsd()
            ));
            assertEquals(
                    alquiler.getFechaHoraCreacion().plusMinutes(15),
                    alquiler.getFechaHoraVencimientoReserva()
            );

            List<AlquilerDetalle> detalles = alquilerDetalleRepository
                    .buscarPorAlquilerConDatos(alquilerId);
            assertEquals(2, detalles.size());
            for (AlquilerDetalle detalle : detalles) {
                assertEquals(alquilerId, detalle.getAlquiler().getId());
                assertEquals("PENDIENTE_RETIRO", detalle.getEstado().getCodigo());
                assertNull(detalle.getFechaHoraRetiro());
                assertNull(detalle.getSubtotalRealArs());
            }

            List<HistorialEstadoDetalle> historiasDetalle =
                    historiasDetalleMysql(alquilerId);
            assertEquals(4, historiasDetalle.size());
            assertEquals(2L, historiasDetalle.stream().filter(historia ->
                    "RESERVADO".equals(historia.getEstadoNuevo().getCodigo())
            ).count());
            assertEquals(2L, historiasDetalle.stream().filter(historia ->
                    "PENDIENTE_RETIRO".equals(historia.getEstadoNuevo().getCodigo())
            ).count());

            for (HistorialEstadoDetalle historia : historiasDetalle) {
                assertEquals(alquilerId,
                        historia.getAlquilerDetalle().getAlquiler().getId());
                if ("PENDIENTE_RETIRO".equals(historia.getEstadoNuevo().getCodigo())) {
                    assertEquals("RESERVADO", historia.getEstadoAnterior().getCodigo());
                    assertEquals("SISTEMA", historia.getOrigen());
                    assertNull(historia.getActorUsuario());
                    assertEquals(pago.getFechaConfirmacion(), historia.getFechaHoraCambio());
                } else {
                    assertNull(historia.getEstadoAnterior());
                    assertEquals("USUARIO", historia.getOrigen());
                    assertEquals(usuarioId, historia.getActorUsuario().getId());
                    assertEquals(alquiler.getFechaHoraCreacion(), historia.getFechaHoraCambio());
                }
            }

            assertTrue(reembolsoRepository.findByPagoIdOrderByIdAsc(pagoId).isEmpty());
            comprobarBicicletas("RESERVADA");
        });
    }

    private List<HistorialEstadoDetalle> historiasDetalleMysql(int alquilerId) {
        return entityManager.createQuery(
                        "select h from HistorialEstadoDetalle h "
                                + "where h.alquilerDetalle.alquiler.id = :id "
                                + "order by h.id",
                        HistorialEstadoDetalle.class
                )
                .setParameter("id", alquilerId)
                .getResultList();
    }

    private void configurarPagoPendienteMysql() {
        String nickname = transaccion.execute(estado ->
                entityManager.find(Usuario.class, usuarioId).getNickname()
        );
        String prefijo = "pagos.simulados.cuentas." + nickname + ".";

        entorno.getPropertySources().addFirst(new MapPropertySource(
                PROPIEDADES_PAGOS_MYSQL,
                Map.<String, Object>of(
                        prefijo + "resultado", "PENDIENTE",
                        prefijo + "demora-segundos", "20"
                )
        ));
    }

    private <T> List<T> ejecutarDosOperaciones(
            Callable<T> operacion
    ) throws Exception {
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);

        Callable<T> tarea = () -> {
            try {
                autenticarListado("CLIENTE");
                listos.countDown();

                if (!salida.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "No se liberaron las solicitudes de prueba."
                    );
                }

                return operacion.call();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        Future<T> primero = ejecutor.submit(tarea);
        Future<T> segundo = ejecutor.submit(tarea);

        try {
            assertTrue(
                    listos.await(5, TimeUnit.SECONDS),
                    "Las dos solicitudes deben estar listas."
            );
            salida.countDown();

            return List.of(
                    primero.get(20, TimeUnit.SECONDS),
                    segundo.get(20, TimeUnit.SECONDS)
            );
        } finally {
            salida.countDown();
            ejecutor.shutdownNow();

            assertTrue(
                    ejecutor.awaitTermination(5, TimeUnit.SECONDS),
                    "Las solicitudes de prueba deben finalizar."
            );
        }
    }

    private AlquilerCabeceraDTO crearReservaPagadaMysql() {
        int alquilerId = alquilerService.crearReserva(solicitud());
        String clave = UUID.randomUUID().toString();

        ResultadoAnticipoDTO inicio =
                pagoService.iniciarAnticipo(alquilerId, clave);
        ResultadoAnticipoDTO aprobado =
                pagoService.resolverAnticipo(alquilerId, clave);

        assertEquals("CREADO", inicio.getEstadoPago());
        assertEquals("APROBADO", aprobado.getEstadoPago());
        assertEquals(inicio.getPagoId(), aprobado.getPagoId());

        comprobarAnticipoMysql(
                alquilerId, aprobado.getPagoId(), clave
        );

        return alquilerService.obtenerAlquiler(alquilerId);
    }

    private void comprobarRetiroMysql(
            AlquilerCabeceraDTO contrato,
            AlquilerDetalleDTO resultado,
            Instant instante,
            long ocupacionEsperada,
            int historiasEsperadas
    ) {
        transaccion.executeWithoutResult(estado -> {
            LocalDateTime fecha =
                    LocalDateTime.ofInstant(instante, ZoneOffset.UTC);

            AlquilerDetalle detalle = alquilerDetalleRepository
                    .findById(resultado.getId()).orElseThrow();

            AlquilerDetalleDTO original = contrato.getDetalles().stream()
                    .filter(unidad -> unidad.getId().equals(resultado.getId()))
                    .findFirst().orElseThrow();

            assertEquals(contrato.getId(), detalle.getAlquiler().getId());
            assertEquals(resultado.getBicicletaId(), detalle.getBicicleta().getId());
            assertEquals("EN_CURSO", detalle.getEstado().getCodigo());
            assertEquals(fecha, detalle.getFechaHoraRetiro());
            assertEquals(
                    fecha.plusSeconds(detalle.getDuracionEstimadaSegundos()),
                    detalle.getFechaHoraDevolucionEstimada()
            );
            assertEquals(resultado.getVersion(), detalle.getVersion());
            assertTrue(detalle.getVersion() > original.getVersion());

            assertEquals(
                    original.getTipoBicicletaContratadoId(),
                    detalle.getTipoBicicletaContratado().getId()
            );
            assertEquals(
                    original.getPrecioPorHoraAplicadoUsd(),
                    detalle.getPrecioPorHoraAplicadoUsd()
            );
            assertEquals(
                    original.getDuracionEstimadaSegundos(),
                    detalle.getDuracionEstimadaSegundos()
            );
            assertEquals(
                    original.getSubtotalEstimadoUsd(),
                    detalle.getSubtotalEstimadoUsd()
            );
            assertEquals(
                    original.getSubtotalEstimadoArs(),
                    detalle.getSubtotalEstimadoArs()
            );

            assertNull(detalle.getFechaHoraDevolucionReal());
            assertNull(detalle.getFechaHoraFinUso());
            assertNull(detalle.getFechaHoraCierreDetalle());
            assertNull(detalle.getEstacionDevolucion());
            assertNull(detalle.getSubtotalRealUsd());
            assertNull(detalle.getSubtotalRealArs());

            assertEquals(
                    "EN_USO",
                    detalle.getBicicleta().getEstado().getCodigo()
            );
            assertNull(detalle.getBicicleta().getUbicacionActual());
            assertEquals(ocupacionEsperada, ocupacionEstacionMysql());

            Alquiler alquiler = detalle.getAlquiler();

            assertEquals(
                    "EN_PROGRESO",
                    alquiler.getEstadoContratacion().getCodigo()
            );
            assertEquals(usuarioId, alquiler.getUsuario().getId());
            assertEquals(
                    contrato.getEstacionRetiroId(),
                    alquiler.getUbicacionRetiro().getId()
            );
            assertEquals(
                    contrato.getFechaHoraCreacion(),
                    alquiler.getFechaHoraCreacion()
            );
            assertEquals(
                    contrato.getFechaHoraVencimientoReserva(),
                    alquiler.getFechaHoraVencimientoReserva()
            );
            assertEquals(
                    contrato.getCotizacionArsPorUsd(),
                    alquiler.getCotizacionArsPorUsd()
            );
            assertEquals(
                    contrato.getCostoEstimadoUsd(),
                    alquiler.getCostoEstimadoUsd()
            );
            assertEquals(
                    contrato.getCostoEstimadoArs(),
                    alquiler.getCostoEstimadoArs()
            );
            assertNull(alquiler.getCostoFinalArs());
            assertNull(alquiler.getFechaHoraLiquidacion());
            assertNull(alquiler.getFechaHoraCierre());

            List<HistorialEstadoDetalle> todas =
                    historiasDetalleMysql(contrato.getId());
            assertEquals(historiasEsperadas, todas.size());

            List<HistorialEstadoDetalle> historiasUnidad = todas.stream()
                    .filter(historia ->
                            historia.getAlquilerDetalle().getId()
                                    .equals(resultado.getId())
                    )
                    .collect(Collectors.toList());

            assertEquals(
                    List.of("RESERVADO", "PENDIENTE_RETIRO", "EN_CURSO"),
                    historiasUnidad.stream()
                            .map(historia -> historia.getEstadoNuevo().getCodigo())
                            .collect(Collectors.toList())
            );

            HistorialEstadoDetalle retiro = historiasUnidad.get(2);

            assertEquals(
                    "PENDIENTE_RETIRO",
                    retiro.getEstadoAnterior().getCodigo()
            );
            assertEquals("USUARIO", retiro.getOrigen());
            assertEquals(usuarioId, retiro.getActorUsuario().getId());
            assertEquals(fecha, retiro.getFechaHoraCambio());

            List<HistorialEstadoBicicleta> historiasBici =
                    historiasBicicletaMysql(resultado.getBicicletaId());

            assertEquals(2, historiasBici.size());

            HistorialEstadoBicicleta historiaBici = historiasBici.get(1);

            assertEquals(
                    resultado.getBicicletaId(),
                    historiaBici.getBicicleta().getId()
            );
            assertEquals(
                    "RESERVADA",
                    historiaBici.getEstadoAnterior().getCodigo()
            );
            assertEquals(
                    "EN_USO",
                    historiaBici.getEstadoNuevo().getCodigo()
            );
            assertEquals("USUARIO", historiaBici.getOrigen());
            assertEquals(usuarioId, historiaBici.getActorUsuario().getId());
            assertEquals(fecha.minusHours(3), historiaBici.getFechaHoraCambio());
        });
    }

    private List<HistorialEstadoBicicleta> historiasBicicletaMysql(
            int bicicletaId
    ) {
        return entityManager.createQuery(
                        "select h from HistorialEstadoBicicleta h "
                                + "where h.bicicleta.id = :id order by h.id",
                        HistorialEstadoBicicleta.class
                )
                .setParameter("id", bicicletaId)
                .getResultList();
    }

    private long ocupacionEstacionMysql() {
        return bicicletaRepository.countByUbicacionActual(
                entityManager.getReference(Estacion.class, estacionId)
        );
    }

    private int crearUsuarioAjenoListado() {
        return transaccion.execute(estado -> {
            int documento = DOCUMENTOS.incrementAndGet();

            Persona persona = new Persona(
                    "Otro",
                    "Cliente",
                    LocalDate.of(1990, 1, 1),
                    catalogo(Genero.class, "MASCULINO"),
                    catalogo(TipoDocumento.class, "DNI"),
                    String.valueOf(documento),
                    catalogo(Pais.class, "AR")
            );
            entityManager.persist(persona);

            Usuario usuario = new Usuario(
                    persona,
                    "qa.mysql.ajeno." + documento,
                    "qa.mysql.ajeno." + documento + "@example.com",
                    passwordEncoder.encode("PruebaMysql?1")
            );
            entityManager.persist(usuario);
            entityManager.flush();

            entityManager.persist(
                    new UsuarioRol(
                            usuario,
                            catalogo(Rol.class, "CLIENTE")
                    )
            );

            return usuario.getId();
        });
    }

    private int crearAlquilerFinalizadoListado(
            int propietarioId,
            boolean borrado
    ) {
        return transaccion.execute(estado -> {
            LocalDateTime retiro =
                    LocalDateTime.now(ZoneOffset.UTC)
                            .minusHours(2)
                            .withNano(0);
            LocalDateTime creacion = retiro.minusMinutes(5);
            LocalDateTime devolucion = retiro.plusMinutes(45);

            Estacion estacion =
                    entityManager.find(Estacion.class, estacionId);
            Bicicleta bicicleta =
                    entityManager.find(Bicicleta.class, bicicleta1Id);

            BigDecimal usd = new BigDecimal("7.50");
            BigDecimal ars = new BigDecimal("11475.00");

            Alquiler alquiler = new Alquiler();
            alquiler.setUsuario(
                    entityManager.find(Usuario.class, propietarioId)
            );
            alquiler.setEstadoContratacion(
                    catalogo(EstadoAlquiler.class, "FINALIZADO")
            );
            alquiler.setUbicacionRetiro(estacion);
            alquiler.setFechaHoraCreacion(creacion);
            alquiler.setFechaHoraVencimientoReserva(
                    creacion.plusMinutes(15)
            );
            alquiler.setFechaHoraCierre(devolucion);
            alquiler.setFechaHoraLiquidacion(devolucion);
            alquiler.setCotizacionArsPorUsd(new BigDecimal("1530.00"));
            alquiler.setFuenteCotizacion("QA_LISTADOS_TEST");
            alquiler.setFechaHoraAplicacionCotizacion(creacion);
            alquiler.setCostoEstimadoUsd(usd);
            alquiler.setCostoEstimadoArs(ars);
            alquiler.setCostoFinalUsd(usd);
            alquiler.setCostoFinalArs(ars);
            alquiler.setBorradoAlquiler(borrado);

            entityManager.persist(alquiler);

            // Fixture de consulta, no ejecución del circuito operativo.
            AlquilerDetalle detalle = new AlquilerDetalle();
            detalle.setAlquiler(alquiler);
            detalle.setBicicleta(bicicleta);
            detalle.setTipoBicicletaContratado(bicicleta.getTipo());
            detalle.setEstado(
                    catalogo(EstadoAlquilerDetalle.class, "DEVUELTO")
            );
            detalle.setEstacionDevolucion(estacion);
            detalle.setPrecioPorHoraAplicadoUsd(new BigDecimal("10.00"));
            detalle.setDuracionEstimadaSegundos(2700);
            detalle.setFechaHoraRetiro(retiro);
            detalle.setFechaHoraDevolucionEstimada(devolucion);
            detalle.setFechaHoraDevolucionReal(devolucion);
            detalle.setFechaHoraFinUso(devolucion);
            detalle.setFechaHoraCierreDetalle(devolucion);
            detalle.setSubtotalEstimadoUsd(usd);
            detalle.setSubtotalEstimadoArs(ars);
            detalle.setSubtotalRealUsd(usd);
            detalle.setSubtotalRealArs(ars);
            detalle.setOrigenCierre("SISTEMA");

            entityManager.persist(detalle);
            entityManager.flush();
            return alquiler.getId();
        });
    }

    private List<Integer> idsListado(List<AlquilerCabeceraDTO> listado) {
        List<Integer> ids = new ArrayList<>();

        for (AlquilerCabeceraDTO fila : listado) {
            ids.add(fila.getId());
        }

        return ids;
    }

    private void autenticarListado(String... codigos) {
        List<SimpleGrantedAuthority> autoridades =
                new ArrayList<>();

        for (String codigo : codigos) {
            autoridades.add(new SimpleGrantedAuthority(codigo));
        }

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        autoridades
                )
        );
    }

    private CrearAlquilerDTO solicitud() {
        CrearAlquilerDTO dto = new CrearAlquilerDTO();

        dto.setEstacionRetiroId(estacionId);
        dto.setBicicletaIds(
                List.of(bicicleta2Id, bicicleta1Id)
        );
        dto.setDuracionEstimadaSegundos(2700);

        return dto;
    }

    private long[] contarRegistros() {
        return transaccion.execute(estado ->
                new long[]{
                        alquilerRepository.count(),
                        alquilerDetalleRepository.count(),
                        historialDetalleRepository.count(),
                        historialBicicletaRepository.count(),
                        historialCabeceraRepository.count()
                }
        );
    }

    private void comprobarBicicletas(String codigoEstado) {
        for (Integer id : List.of(bicicleta1Id, bicicleta2Id)) {
            Bicicleta bicicleta = bicicletaRepository
                    .findById(id)
                    .orElseThrow();

            assertEquals(
                    codigoEstado,
                    bicicleta.getEstado().getCodigo()
            );
            assertNotNull(bicicleta.getUbicacionActual());
            assertEquals(
                    estacionId,
                    bicicleta.getUbicacionActual().getId()
            );
        }
    }

    private <T> T catalogo(Class<T> tipo, String codigo) {
        return entityManager.createQuery(
                        "select c from " + tipo.getSimpleName()
                                + " c where c.codigo = :codigo",
                        tipo
                )
                .setParameter("codigo", codigo)
                .getSingleResult();
    }

    @Test
    void estadisticasMysqlCuentaBicicletasEnUsoNoReservasNiCabeceras() {
        long inicial = estadisticasService
                .getBicicletasAlquiladasActuales();

        AlquilerCabeceraDTO reserva = crearReservaPagadaMysql();

        // Reservar y pagar todavía no significa retirar.
        assertEquals(inicial, estadisticasService
                .getBicicletasAlquiladasActuales().longValue());

        retirarParaDevolucionMysql(reserva);

        assertEquals(inicial + 1, estadisticasService
                .getBicicletasAlquiladasActuales().longValue());

        alquilerService.retirarBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(1).getId()
        );

        // Una cabecera, pero dos bicicletas efectivamente en uso.
        assertEquals(inicial + 2, estadisticasService
                .getBicicletasAlquiladasActuales().longValue());

        when(reloj.instant()).thenReturn(
                instantePagos.plus(32, ChronoUnit.MINUTES)
        );

        alquilerService.devolverBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(0).getId(),
                solicitudDevolucionMysql(false)
        );

        assertEquals(inicial + 1, estadisticasService
                .getBicicletasAlquiladasActuales().longValue());

        alquilerService.devolverBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(1).getId(),
                solicitudDevolucionMysql(false)
        );

        assertEquals(inicial, estadisticasService
                .getBicicletasAlquiladasActuales().longValue());
    }

    @Test
    void estadisticasMysqlConservaTipoContratadoYUsosHistoricos() {
        long montanaInicial = cantidadPreferenciasMysql("MONTANA");
        long paseoInicial = cantidadPreferenciasMysql("PASEO");

        AlquilerCabeceraDTO reserva = crearReservaRetiradaMysql();

        // Los usos todavía abiertos no cuentan como usos concluidos.
        assertEquals(montanaInicial, cantidadPreferenciasMysql("MONTANA"));
        assertEquals(paseoInicial, cantidadPreferenciasMysql("PASEO"));

        when(reloj.instant()).thenReturn(
                instantePagos.plus(32, ChronoUnit.MINUTES)
        );

        alquilerService.devolverBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(0).getId(),
                solicitudDevolucionMysql(false)
        );

        // Cuenta la unidad devuelta aunque la otra siga circulando.
        assertEquals(montanaInicial + 1, cantidadPreferenciasMysql("MONTANA"));
        assertEquals(paseoInicial, cantidadPreferenciasMysql("PASEO"));

        // Cambio controlado del dato actual para comprobar el snapshot contratado.
        transaccion.executeWithoutResult(estado -> {
            Bicicleta bicicleta = entityManager.find(
                    Bicicleta.class, bicicleta1Id
            );
            bicicleta.setTipo(catalogo(TipoBicicleta.class, "PASEO"));
        });

        assertEquals(montanaInicial + 1, cantidadPreferenciasMysql("MONTANA"));
        assertEquals(paseoInicial, cantidadPreferenciasMysql("PASEO"));

        alquilerService.devolverBicicleta(
                reserva.getId(),
                reserva.getDetalles().get(1).getId(),
                solicitudDevolucionMysql(false)
        );

        // Ocultar el alquiler no borra los usos históricos.
        transaccion.executeWithoutResult(estado ->
                entityManager.find(Alquiler.class, reserva.getId())
                        .setBorradoAlquiler(true)
        );

        assertEquals(montanaInicial + 1, cantidadPreferenciasMysql("MONTANA"));
        assertEquals(paseoInicial + 1, cantidadPreferenciasMysql("PASEO"));
    }

    private long cantidadPreferenciasMysql(String tipo) {
        return estadisticasService.getPreferenciasBicicletaPorGenero()
                .stream()
                .filter(fila -> tipo.equals(fila[0])
                        && "MASCULINO".equals(fila[1]))
                .mapToLong(fila -> ((Number) fila[2]).longValue())
                .sum();
    }

}
