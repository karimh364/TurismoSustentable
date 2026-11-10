package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerCabeceraDTO;
import DSII.TurismoSustentable.dto.CotizacionDolarDTO;
import DSII.TurismoSustentable.dto.CrearAlquilerDTO;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(
        named = "TURISMO_TEST_MYSQL",
        matches = "true"
)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.datasource.url=jdbc:mysql://localhost:3306/turismo_sustentable?useSSL=false&serverTimezone=UTC",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false"
        }
)
@ActiveProfiles("refactor")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AlquilerMysqlIntegrationTest {

    private static final AtomicInteger DOCUMENTOS =
            new AtomicInteger(10000000);

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
    private PlatformTransactionManager transactionManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @MockBean
    private CotizacionDolarService cotizacionDolarService;

    @SpyBean
    private TrazabilidadInventarioService trazabilidadInventario;

    private TrazabilidadInventarioService trazabilidadEspiada;
    private TransactionTemplate transaccion;

    private String email;
    private int usuarioId;
    private int estacionId;
    private int bicicleta1Id;
    private int bicicleta2Id;

    @BeforeEach
    void prepararDatosReales() {
        SecurityContextHolder.clearContext();

        trazabilidadEspiada =
                AopTestUtils.getUltimateTargetObject(
                        trazabilidadInventario
                );

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
                    -31.4,
                    -64.2
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

        if (trazabilidadEspiada != null) {
            reset(trazabilidadEspiada);
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
                anyString()
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
                        historialBicicletaRepository.count()
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
}
