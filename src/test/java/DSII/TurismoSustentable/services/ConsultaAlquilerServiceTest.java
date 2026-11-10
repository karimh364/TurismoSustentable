package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerCabeceraDTO;
import DSII.TurismoSustentable.dto.AlquilerDetalleDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import javax.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaAlquilerServiceTest {

    private static final String EMAIL = "consulta.qa@example.com";

    private static final LocalDateTime CREACION =
            LocalDateTime.of(2026, 11, 10, 12, 0);

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AlquilerRepository alquilerRepository;

    @Mock
    private AlquilerDetalleRepository alquilerDetalleRepository;

    @Mock
    private BicicletaRepository bicicletaRepository;

    @Mock
    private CotizacionDolarService cotizacionDolarService;

    @Mock
    private CalculadoraAlquiler calculadoraAlquiler;

    @Mock
    private CorreoService correoService;

    @Mock
    private TrazabilidadInventarioService trazabilidadInventario;

    @InjectMocks
    private AlquilerService servicio;

    private Usuario usuario;
    private Estacion estacion;
    private TipoBicicleta tipo;
    private Alquiler alquiler;
    private List<AlquilerDetalle> detalles;

    @BeforeEach
    void preparar() {
        SecurityContextHolder.clearContext();

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);

        asignarRoles("CLIENTE");
        autenticar("CLIENTE");

        estacion = new Estacion(
                "Plaza QA", "Calle QA", 10, 0.0, 0.0
        );
        estacion.setEsPublica(true);
        ReflectionTestUtils.setField(estacion, "id", 1);

        // El catálogo actual cuesta distinto de la tarifa contratada.
        tipo = new TipoBicicleta(
                "MONTANA", new BigDecimal("90.00"), 3
        );
        ReflectionTestUtils.setField(tipo, "id", 1);

        alquiler = new Alquiler();
        alquiler.setId(50);
        ReflectionTestUtils.setField(alquiler, "version", 3);

        alquiler.setUsuario(usuario);
        alquiler.setEstadoContratacion(
                new EstadoAlquiler("RESERVADO")
        );
        alquiler.setUbicacionRetiro(estacion);
        alquiler.setFechaHoraCreacion(CREACION);
        alquiler.setFechaHoraVencimientoReserva(
                CREACION.plusMinutes(15)
        );

        alquiler.setCotizacionArsPorUsd(
                new BigDecimal("1530.00")
        );
        alquiler.setFuenteCotizacion("Fuente histórica QA");
        alquiler.setFechaHoraAplicacionCotizacion(CREACION);
        alquiler.setFechaHoraActualizacionFuente(
                CREACION.minusMinutes(5)
        );

        alquiler.setCostoEstimadoUsd(new BigDecimal("30.00"));
        alquiler.setCostoEstimadoArs(new BigDecimal("45900.00"));

        detalles = new ArrayList<>(List.of(
                crearDetalle(101, 1),
                crearDetalle(102, 2)
        ));
    }

    @AfterEach
    void limpiarYComprobarLecturaSinEscrituras() {
        SecurityContextHolder.clearContext();

        verify(alquilerRepository, never())
                .save(any(Alquiler.class));
        verify(alquilerRepository, never())
                .delete(any(Alquiler.class));
        verify(alquilerRepository, never()).deleteById(anyInt());

        verify(alquilerDetalleRepository, never())
                .save(any(AlquilerDetalle.class));
        verify(alquilerDetalleRepository, never())
                .delete(any(AlquilerDetalle.class));
        verify(alquilerDetalleRepository, never()).deleteById(anyInt());

        verifyNoInteractions(
                bicicletaRepository,
                cotizacionDolarService,
                calculadoraAlquiler,
                correoService,
                trazabilidadInventario
        );
    }

    @Test
    void propietarioConsultaCabeceraYDetallesHistoricos() {
        prepararConsultaCompleta();

        AlquilerCabeceraDTO resultado =
                servicio.obtenerAlquiler(50);

        assertEquals(50, resultado.getId());
        assertEquals(Integer.valueOf(3), resultado.getVersion());
        assertEquals(1, resultado.getUsuarioId());
        assertEquals("RESERVADO", resultado.getEstado());

        assertEquals(1, resultado.getEstacionRetiroId());
        assertEquals("Plaza QA", resultado.getEstacionRetiro());

        assertEquals(CREACION, resultado.getFechaHoraCreacion());
        assertEquals(
                CREACION.plusMinutes(15),
                resultado.getFechaHoraVencimientoReserva()
        );

        assertEquals(
                new BigDecimal("1530.00"),
                resultado.getCotizacionArsPorUsd()
        );
        assertEquals(
                "Fuente histórica QA",
                resultado.getFuenteCotizacion()
        );
        assertEquals(
                CREACION,
                resultado.getFechaHoraAplicacionCotizacion()
        );
        assertEquals(
                CREACION.minusMinutes(5),
                resultado.getFechaHoraActualizacionFuente()
        );

        assertEquals(
                new BigDecimal("30.00"),
                resultado.getCostoEstimadoUsd()
        );
        assertEquals(
                new BigDecimal("45900.00"),
                resultado.getCostoEstimadoArs()
        );

        assertNull(resultado.getCostoFinalUsd());
        assertNull(resultado.getCostoFinalArs());
        assertNull(resultado.getFechaHoraCierre());
        assertNull(resultado.getFechaHoraCancelacion());
        assertNull(resultado.getFechaHoraLiquidacion());

        assertEquals(2, resultado.getDetalles().size());

        AlquilerDetalleDTO primero =
                resultado.getDetalles().get(0);

        assertEquals(Integer.valueOf(101), primero.getId());
        assertEquals(Integer.valueOf(2), primero.getVersion());
        assertEquals(1, primero.getBicicletaId());
        assertEquals("QA-LECT-1", primero.getCodigoBicicleta());
        assertEquals(
                Integer.valueOf(1),
                primero.getTipoBicicletaContratadoId()
        );
        assertEquals(
                "MONTANA",
                primero.getTipoBicicletaContratado()
        );
        assertEquals("RESERVADO", primero.getEstado());

        // Debe mostrar USD 10 contratados, no USD 90 del catálogo.
        assertEquals(
                new BigDecimal("10.00"),
                primero.getPrecioPorHoraAplicadoUsd()
        );
        assertEquals(
                Integer.valueOf(5400),
                primero.getDuracionEstimadaSegundos()
        );
        assertEquals(
                new BigDecimal("15.00"),
                primero.getSubtotalEstimadoUsd()
        );
        assertEquals(
                new BigDecimal("22950.00"),
                primero.getSubtotalEstimadoArs()
        );

        assertNull(primero.getFechaHoraRetiro());
        assertNull(primero.getFechaHoraDevolucionEstimada());
        assertNull(primero.getFechaHoraDevolucionReal());
        assertNull(primero.getFechaHoraFinUso());
        assertNull(primero.getFechaHoraCierreDetalle());
        assertNull(primero.getEstacionDevolucionId());
        assertNull(primero.getEstacionDevolucion());
        assertNull(primero.getSubtotalRealUsd());
        assertNull(primero.getSubtotalRealArs());
        assertFalse(primero.isFallaReportada());

        assertEquals(
                2,
                resultado.getDetalles().get(1).getBicicletaId()
        );

        var orden = inOrder(
                usuarioRepository,
                alquilerRepository,
                alquilerDetalleRepository
        );

        orden.verify(usuarioRepository).findByEmail(EMAIL);
        orden.verify(alquilerRepository)
                .buscarAlquilerVisiblePorId(50);
        orden.verify(alquilerDetalleRepository)
                .buscarPorAlquilerConDatos(50);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void adminPuedeConsultarAlquilerAjeno(
            boolean tambienEsCliente
    ) {
        if (tambienEsCliente) {
            asignarRoles("ADMIN", "CLIENTE");
            autenticar("ADMIN", "CLIENTE");
        } else {
            asignarRoles("ADMIN");
            autenticar("ADMIN");
        }

        Usuario propietario = new Usuario();
        propietario.setId(2);
        alquiler.setUsuario(propietario);

        prepararConsultaCompleta();

        AlquilerCabeceraDTO resultado =
                servicio.obtenerAlquiler(50);

        assertEquals(2, resultado.getUsuarioId());
        assertEquals(2, resultado.getDetalles().size());
    }

    @Test
    void clienteAjenoNoPuedeConsultarDetalles() {
        Usuario propietario = new Usuario();
        propietario.setId(2);
        alquiler.setUsuario(propietario);

        prepararUsuario();

        when(alquilerRepository.buscarAlquilerVisiblePorId(50))
                .thenReturn(Optional.of(alquiler));

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.obtenerAlquiler(50)
        );

        verifyNoInteractions(alquilerDetalleRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "ADMIN", "TECNICO"})
    void rolesDeSesionNoSustituyenPermisosVigentes(
            String autoridadSesion
    ) {
        autenticar(autoridadSesion);

        // En BD solo queda TECNICO.
        asignarRoles("TECNICO");

        prepararUsuario();

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.obtenerAlquiler(50)
        );

        verifyNoInteractions(
                alquilerRepository,
                alquilerDetalleRepository
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rechazaCuentaInexistenteOInactiva(boolean existe) {
        usuario.setActivo(false);

        when(usuarioRepository.findByEmail(EMAIL))
                .thenReturn(
                        existe
                                ? Optional.of(usuario)
                                : Optional.empty()
                );

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.obtenerAlquiler(50)
        );

        verifyNoInteractions(
                alquilerRepository,
                alquilerDetalleRepository
        );
    }

    @Test
    void rechazaConsultaSinSesion() {
        SecurityContextHolder.clearContext();

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.obtenerAlquiler(50)
        );

        verifyNoInteractions(
                usuarioRepository,
                alquilerRepository,
                alquilerDetalleRepository
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rechazaIdInvalido(Integer id) {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.obtenerAlquiler(id)
        );

        verifyNoInteractions(
                usuarioRepository,
                alquilerRepository,
                alquilerDetalleRepository
        );
    }

    @Test
    void rechazaAlquilerNoDisponibleParaConsulta() {
        prepararUsuario();

        when(alquilerRepository.buscarAlquilerVisiblePorId(50))
                .thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> servicio.obtenerAlquiler(50)
        );

        verifyNoInteractions(alquilerDetalleRepository);
    }

    @Test
    void cabeceraSinDetallesEsProblemaInterno() {
        prepararUsuario();

        when(alquilerRepository.buscarAlquilerVisiblePorId(50))
                .thenReturn(Optional.of(alquiler));

        when(alquilerDetalleRepository.buscarPorAlquilerConDatos(50))
                .thenReturn(List.of());

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> servicio.obtenerAlquiler(50)
        );
    }

    @Test
    void dtoNoComparteColeccionNiValoresMutablesDeOrigen() {
        prepararConsultaCompleta();

        AlquilerCabeceraDTO resultado =
                servicio.obtenerAlquiler(50);

        detalles.get(0).setPrecioPorHoraAplicadoUsd(
                new BigDecimal("99.00")
        );
        alquiler.setCotizacionArsPorUsd(
                new BigDecimal("2000.00")
        );
        detalles.clear();

        assertEquals(2, resultado.getDetalles().size());
        assertEquals(
                new BigDecimal("10.00"),
                resultado.getDetalles().get(0)
                        .getPrecioPorHoraAplicadoUsd()
        );
        assertEquals(
                new BigDecimal("1530.00"),
                resultado.getCotizacionArsPorUsd()
        );

        assertThrows(
                UnsupportedOperationException.class,
                () -> resultado.getDetalles().clear()
        );
    }

    @Test
    void muestraDevolucionYCostosFinalesSinConfundirDetalles() {
        AlquilerDetalle devuelto = detalles.get(0);

        LocalDateTime retiro = CREACION.plusMinutes(5);
        LocalDateTime devolucion = retiro.plusMinutes(45);

        alquiler.setEstadoContratacion(
                new EstadoAlquiler("EN_PROGRESO")
        );

        devuelto.setEstado(
                new EstadoAlquilerDetalle("DEVUELTO")
        );
        devuelto.setFechaHoraRetiro(retiro);
        devuelto.setFechaHoraDevolucionEstimada(
                retiro.plusSeconds(5400)
        );
        devuelto.setFechaHoraDevolucionReal(devolucion);
        devuelto.setFechaHoraFinUso(devolucion);
        devuelto.setFechaHoraCierreDetalle(devolucion);
        devuelto.setEstacionDevolucion(estacion);
        devuelto.setSubtotalRealUsd(new BigDecimal("7.50"));
        devuelto.setSubtotalRealArs(new BigDecimal("11475.00"));
        devuelto.setFallaReportada(true);

        detalles.get(1).setEstado(
                new EstadoAlquilerDetalle("EN_CURSO")
        );

        prepararConsultaCompleta();

        AlquilerCabeceraDTO resultado =
                servicio.obtenerAlquiler(50);

        AlquilerDetalleDTO primero = resultado.getDetalles().get(0);
        AlquilerDetalleDTO segundo = resultado.getDetalles().get(1);

        assertEquals("EN_PROGRESO", resultado.getEstado());
        assertEquals("DEVUELTO", primero.getEstado());
        assertEquals("EN_CURSO", segundo.getEstado());

        assertEquals(retiro, primero.getFechaHoraRetiro());
        assertEquals(
                retiro.plusSeconds(5400),
                primero.getFechaHoraDevolucionEstimada()
        );
        assertEquals(
                devolucion, primero.getFechaHoraDevolucionReal()
        );
        assertEquals(devolucion, primero.getFechaHoraFinUso());
        assertEquals(
                devolucion, primero.getFechaHoraCierreDetalle()
        );
        assertEquals(
                Integer.valueOf(1), primero.getEstacionDevolucionId()
        );
        assertEquals("Plaza QA", primero.getEstacionDevolucion());
        assertEquals(
                new BigDecimal("7.50"), primero.getSubtotalRealUsd()
        );
        assertEquals(
                new BigDecimal("11475.00"), primero.getSubtotalRealArs()
        );
        assertTrue(primero.isFallaReportada());

        // El segundo detalle y el total final siguen pendientes.
        assertNull(segundo.getFechaHoraDevolucionReal());
        assertNull(segundo.getSubtotalRealUsd());
        assertNull(resultado.getCostoFinalUsd());
        assertNull(resultado.getFechaHoraLiquidacion());
    }

    private void prepararUsuario() {
        when(usuarioRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(usuario));
    }

    private void prepararConsultaCompleta() {
        prepararUsuario();

        when(alquilerRepository.buscarAlquilerVisiblePorId(50))
                .thenReturn(Optional.of(alquiler));

        when(alquilerDetalleRepository.buscarPorAlquilerConDatos(50))
                .thenReturn(detalles);
    }

    private AlquilerDetalle crearDetalle(int id, int bicicletaId) {
        Bicicleta bicicleta = new Bicicleta(
                "QA-LECT-" + bicicletaId,
                tipo,
                new EstadoBicicleta("RESERVADA"),
                estacion
        );
        bicicleta.setId(bicicletaId);

        AlquilerDetalle detalle = new AlquilerDetalle();
        ReflectionTestUtils.setField(detalle, "id", id);
        ReflectionTestUtils.setField(detalle, "version", 2);

        detalle.setAlquiler(alquiler);
        detalle.setBicicleta(bicicleta);
        detalle.setTipoBicicletaContratado(tipo);
        detalle.setEstado(new EstadoAlquilerDetalle("RESERVADO"));

        detalle.setPrecioPorHoraAplicadoUsd(
                new BigDecimal("10.00")
        );
        detalle.setDuracionEstimadaSegundos(5400);
        detalle.setSubtotalEstimadoUsd(new BigDecimal("15.00"));
        detalle.setSubtotalEstimadoArs(new BigDecimal("22950.00"));

        return detalle;
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

    private void autenticar(String... autoridades) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL,
                        null,
                        Arrays.stream(autoridades)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList())
                )
        );
    }
}
