package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerRequestDTO;
import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.Bicicleta;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.models.CodigoEstadoAlquiler;
import DSII.TurismoSustentable.models.EstadoBicicleta;
import DSII.TurismoSustentable.models.TipoBicicleta;
import DSII.TurismoSustentable.models.Usuario;
import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import DSII.TurismoSustentable.models.Rol;
import DSII.TurismoSustentable.models.UsuarioRol;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlquilerServiceTest {

    private static final String EMAIL = "cliente.qa@example.com";

    private static final BigDecimal PRECIO =
            new BigDecimal("9.00");

    private static final BigDecimal COSTO_ESTIMADO =
            new BigDecimal("18.00");

    private static final LocalDateTime INICIO =
            LocalDateTime.of(2026, 11, 7, 10, 0);

    private static final LocalDateTime FIN_ESTIMADO =
            INICIO.plusHours(2);

    @Mock
    private AlquilerRepository alquilerRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BicicletaRepository bicicletaRepository;

    @Mock
    private EstacionRepository estacionRepository;

    @Mock
    private CorreoService correoService;

    @Mock
    private DSII.TurismoSustentable.repositories.TipoBicicletaRepository tipoBicicletaRepository;
    @Mock
    private DSII.TurismoSustentable.repositories.EstadoBicicletaRepository estadoBicicletaRepository;
    @Mock
    private TrazabilidadInventarioService trazabilidadInventario;

    @Spy
    @InjectMocks
    private AlquilerService alquilerService;

    private Usuario usuario;
    private Estacion origen;
    private Estacion destino;
    private Bicicleta bicicleta;

    @BeforeEach
    void prepararDatosYSesion() {
        SecurityContextHolder.clearContext();

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);
        asignarRolesUsuario("CLIENTE");

        origen = crearEstacion(1, "Plaza QA", true);
        destino = crearEstacion(2, "Destino QA", true);

        bicicleta = new Bicicleta(
                "QA-B01",
                new TipoBicicleta("MONTANA", PRECIO, 3),
                new EstadoBicicleta("DISPONIBLE"),
                origen
        );
        bicicleta.setId(1);
        ReflectionTestUtils.setField(bicicleta.getTipo(), "id", 1);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL,
                        null,
                        List.of(new SimpleGrantedAuthority("CLIENTE"))
                )
        );

        when(usuarioRepository.findByEmail(EMAIL))
                .thenReturn(Optional.of(usuario));

        // Evitar cualquier API real, incluso si una regresión deja avanzar.
        lenient().doReturn(BigDecimal.ONE)
                .when(alquilerService).obtenerCotizacionDolar();
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rechazaRetiroDesdeEstacionInterna() {
        origen.setEsPublica(false);
        comprobarRetiroRechazado();
    }

    @Test
    void rechazaRetiroDesdeEstacionDadaDeBaja() {
        origen.setBorradoEstacion(true);
        comprobarRetiroRechazado();
    }

    @Test
    void rechazaDevolucionEnEstacionInterna() {
        Alquiler alquiler = prepararAlquilerEnCurso();
        destino.setEsPublica(false);

        when(estacionRepository.buscarActivaPorNombreConBloqueo(
                "Destino QA"
        )).thenReturn(Optional.of(destino));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> alquilerService.finalizarAlquiler(10, "Destino QA")
        );

        assertEquals(
                "La devolución debe realizarse en una estación pública habilitada.",
                error.getMessage()
        );

        comprobarDevolucionSinCambios(alquiler);

        verifyNoInteractions(bicicletaRepository);
        verifyNoMoreInteractions(
                usuarioRepository,
                alquilerRepository,
                estacionRepository
        );
    }

    @Test
    void rechazaDevolucionEnEstacionFisicamenteLlena() {
        Alquiler alquiler = prepararAlquilerEnCurso();

        when(estacionRepository.buscarActivaPorNombreConBloqueo(
                "Destino QA"
        )).thenReturn(Optional.of(destino));

        when(bicicletaRepository.countByUbicacionActual(destino))
                .thenReturn(2L);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> alquilerService.finalizarAlquiler(10, "Destino QA")
        );

        assertEquals(
                "La estación seleccionada no tiene espacios libres.",
                error.getMessage()
        );

        comprobarDevolucionSinCambios(alquiler);

        verify(bicicletaRepository).countByUbicacionActual(destino);
        verifyNoMoreInteractions(
                usuarioRepository,
                alquilerRepository,
                estacionRepository,
                bicicletaRepository
        );
    }


    @Test
    void inicioUsaTarifaDelTipoYLaCongelaEnElContrato() throws Exception {
        asignarRolesUsuario("ADMIN", "CLIENTE");
        // Una cuenta con dos roles sigue actuando como cliente al contratar su propio viaje.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(EMAIL, null, List.of(
                        new SimpleGrantedAuthority("ADMIN"), new SimpleGrantedAuthority("CLIENTE"))));
        when(bicicletaRepository.buscarActivaPorIdConBloqueo(1)).thenReturn(Optional.of(bicicleta));
        when(tipoBicicletaRepository.buscarPorIdConBloqueo(1)).thenReturn(Optional.of(bicicleta.getTipo()));
        when(estadoBicicletaRepository.findByCodigo("EN_USO"))
                .thenReturn(Optional.of(new EstadoBicicleta("EN_USO")));
        when(alquilerRepository.save(any(Alquiler.class))).thenAnswer(inv -> {
            Alquiler a = inv.getArgument(0); a.setId(10); return a;
        });
        AlquilerRequestDTO request = new AlquilerRequestDTO();
        request.setBicicletaId(1); request.setDuracionEnHoras(2);
        alquilerService.crearAlquiler(request);
        org.mockito.ArgumentCaptor<Alquiler> captor = org.mockito.ArgumentCaptor.forClass(Alquiler.class);
        verify(alquilerRepository).save(captor.capture());
        bicicleta.getTipo().setPrecioPorHoraUsd(new BigDecimal("90.00"));
        assertEquals(PRECIO, captor.getValue().getPrecioPorHoraAplicado());
        assertEquals(COSTO_ESTIMADO, captor.getValue().getCostoTotal());
        assertEquals("EN_USO", bicicleta.getEstado().getCodigo());
        assertNull(bicicleta.getUbicacionActual());
        verify(trazabilidadInventario).estado(eq(bicicleta), any(EstadoBicicleta.class),
                eq(usuario), eq("Inicio de alquiler."));
        verifyNoInteractions(estacionRepository);
        verify(trazabilidadInventario, never()).administracion(any(), anyString(), anyInt(),
                anyString(), any(), any(), any());
    }

    @Test
    void rechazaCrearSiClienteFueRetiradoAunqueLaSesionLoConserve() {
        // La sesión del setup conserva CLIENTE, pero la cuenta ya solo tiene ADMIN.
        asignarRolesUsuario("ADMIN");

        AlquilerRequestDTO solicitud = new AlquilerRequestDTO();
        solicitud.setBicicletaId(1);
        solicitud.setDuracionEnHoras(1);

        assertThrows(
                AccessDeniedException.class,
                () -> alquilerService.crearAlquiler(solicitud)
        );

        assertEquals("DISPONIBLE", bicicleta.getEstado().getCodigo());
        assertSame(origen, bicicleta.getUbicacionActual());

        verifyNoInteractions(
                bicicletaRepository,
                alquilerRepository,
                estacionRepository,
                tipoBicicletaRepository,
                estadoBicicletaRepository,
                correoService,
                trazabilidadInventario
        );

        verify(alquilerService, never()).obtenerCotizacionDolar();
    }

    private void asignarRolesUsuario(String... codigos) {
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

    @Test
    void devolucionUsaTarifaCongeladaAunqueElTipoAhoraCuesteMas() throws Exception {
        Alquiler alquiler = prepararAlquilerEnCurso();
        alquiler.setFechaInicio(LocalDateTime.now().minusMinutes(45));
        bicicleta.getTipo().setPrecioPorHoraUsd(new BigDecimal("90.00"));
        when(estacionRepository.buscarActivaPorNombreConBloqueo("Destino QA"))
                .thenReturn(Optional.of(destino));
        when(estadoBicicletaRepository.findByCodigo("DISPONIBLE"))
                .thenReturn(Optional.of(new EstadoBicicleta("DISPONIBLE")));
        when(alquilerRepository.save(alquiler)).thenReturn(alquiler);
        alquilerService.finalizarAlquiler(10, "Destino QA");
        assertEquals(PRECIO, alquiler.getPrecioPorHoraAplicado());
        long segundos = java.time.temporal.ChronoUnit.SECONDS.between(
                alquiler.getFechaInicio(), alquiler.getFechaFin());
        BigDecimal esperado = PRECIO.divide(new BigDecimal("3600"), 10, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(segundos)).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(esperado, alquiler.getCostoTotal());
        assertEquals(CodigoEstadoAlquiler.FINALIZADO, alquiler.getEstado());
        assertEquals("DISPONIBLE", bicicleta.getEstado().getCodigo());
        assertSame(destino, bicicleta.getUbicacionActual());
        verify(trazabilidadInventario).estado(eq(bicicleta), any(EstadoBicicleta.class),
                eq(usuario), eq("Devolución de alquiler."));
        verify(tipoBicicletaRepository, never()).buscarPorIdConBloqueo(anyInt());
    }

    @Test
    void sinTarifaHistoricaNoRecalculaConLaActualNiMutaElAlquiler() {
        Alquiler alquiler = prepararAlquilerEnCurso();
        alquiler.setPrecioPorHoraAplicado(null);
        when(estacionRepository.buscarActivaPorNombreConBloqueo("Destino QA"))
                .thenReturn(Optional.of(destino));
        assertThrows(DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException.class,
                () -> alquilerService.finalizarAlquiler(10, "Destino QA"));
        assertEquals(CodigoEstadoAlquiler.INICIADO, alquiler.getEstado());
        assertEquals(FIN_ESTIMADO, alquiler.getFechaFin());
        assertNull(alquiler.getUbicacionDevolucion());
        assertEquals("EN_USO", bicicleta.getEstado().getCodigo());
        verify(alquilerRepository, never()).save(any());
        verify(bicicletaRepository, never()).save(any());
        verifyNoInteractions(estadoBicicletaRepository, correoService, trazabilidadInventario);
        verify(alquilerService, never()).obtenerCotizacionDolar();
    }

    @Test
    void devolucionDeOtroUsuarioSeRechazaAntesDeTocarInventario() {
        prepararAlquilerEnCurso();
        // El contrato sigue perteneciendo a id 1; la sesión resuelve ahora a id 2.
        Usuario otro = new Usuario();
        otro.setId(2); otro.setActivo(true);
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(otro));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> alquilerService.finalizarAlquiler(10, "Destino QA"));
        verifyNoInteractions(estacionRepository, bicicletaRepository, estadoBicicletaRepository,
                tipoBicicletaRepository, correoService, trazabilidadInventario);
        verify(alquilerRepository, never()).save(any());
    }

    private Estacion crearEstacion(
            int id,
            String nombre,
            boolean esPublica
    ) {
        Estacion estacion = new Estacion(
                nombre, "Calle QA", 2, 0.0, 0.0
        );
        estacion.setEsPublica(esPublica);
        ReflectionTestUtils.setField(estacion, "id", id);
        ReflectionTestUtils.setField(estacion, "version", 0);
        return estacion;
    }

    private void comprobarRetiroRechazado() {
        when(bicicletaRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(bicicleta));

        AlquilerRequestDTO solicitud = new AlquilerRequestDTO();
        solicitud.setBicicletaId(1);
        solicitud.setDuracionEnHoras(2);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> alquilerService.crearAlquiler(solicitud)
        );

        assertEquals(
                "La bicicleta no está ubicada en una estación pública " +
                        "habilitada para alquilar.",
                error.getMessage()
        );

        assertEquals("QA-B01", bicicleta.getCodigoBicicleta());
        assertEquals("DISPONIBLE", bicicleta.getEstado().getCodigo());
        assertEquals("MONTANA", bicicleta.getTipo().getCodigo());
        assertEquals(PRECIO, bicicleta.getTipo().getPrecioPorHoraUsd());
        assertSame(origen, bicicleta.getUbicacionActual());
        assertFalse(bicicleta.isBorradoBici());

        verify(usuarioRepository).findByEmail(EMAIL);
        verify(bicicletaRepository).buscarActivaPorIdConBloqueo(1);
        verify(bicicletaRepository, never()).save(any(Bicicleta.class));

        verifyNoMoreInteractions(
                usuarioRepository,
                bicicletaRepository
        );
        verifyNoInteractions(
                alquilerRepository,
                estacionRepository,
                correoService
        );

        verify(alquilerService, never()).obtenerCotizacionDolar();
    }

    private Alquiler prepararAlquilerEnCurso() {
        bicicleta.setEstado(new EstadoBicicleta("EN_USO"));
        bicicleta.setUbicacionActual(null);

        Alquiler alquiler = new Alquiler(
                usuario,
                bicicleta,
                INICIO,
                FIN_ESTIMADO,
                COSTO_ESTIMADO,
                CodigoEstadoAlquiler.INICIADO,
                origen,
                null
        );
        alquiler.setId(10);
        alquiler.setPrecioPorHoraAplicado(PRECIO);

        when(alquilerRepository.buscarPorIdConBloqueo(10))
                .thenReturn(Optional.of(alquiler));

        return alquiler;
    }

    private void comprobarDevolucionSinCambios(Alquiler alquiler) {
        assertEquals(CodigoEstadoAlquiler.INICIADO, alquiler.getEstado());
        assertEquals(INICIO, alquiler.getFechaInicio());
        assertEquals(FIN_ESTIMADO, alquiler.getFechaFin());
        assertEquals(COSTO_ESTIMADO, alquiler.getCostoTotal());
        assertNull(alquiler.getCostoTotalArs());
        assertEquals(PRECIO, alquiler.getPrecioPorHoraAplicado());
        assertSame(origen, alquiler.getUbicacionRetiro());
        assertNull(alquiler.getUbicacionDevolucion());
        assertSame(usuario, alquiler.getUsuario());
        assertSame(bicicleta, alquiler.getBicicleta());
        assertFalse(alquiler.isBorradoAlquiler());

        assertEquals("QA-B01", bicicleta.getCodigoBicicleta());
        assertEquals("EN_USO", bicicleta.getEstado().getCodigo());
        assertEquals("MONTANA", bicicleta.getTipo().getCodigo());
        assertEquals(PRECIO, bicicleta.getTipo().getPrecioPorHoraUsd());
        assertNull(bicicleta.getUbicacionActual());
        assertFalse(bicicleta.isBorradoBici());

        verify(usuarioRepository).findByEmail(EMAIL);
        verify(alquilerRepository).buscarPorIdConBloqueo(10);
        verify(estacionRepository)
                .buscarActivaPorNombreConBloqueo("Destino QA");

        verify(alquilerRepository, never()).save(any(Alquiler.class));
        verify(bicicletaRepository, never()).save(any(Bicicleta.class));
        verifyNoInteractions(correoService);
        verify(alquilerService, never()).obtenerCotizacionDolar();
    }
}
