package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerRequestDTO;
import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.Bicicleta;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.models.EstadoAlquiler;
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

        origen = crearEstacion(1, "Plaza QA", true);
        destino = crearEstacion(2, "Destino QA", true);

        bicicleta = new Bicicleta(
                "QA-B01",
                TipoBicicleta.MONTANA,
                EstadoBicicleta.DISPONIBLE,
                PRECIO,
                origen
        );
        bicicleta.setId(1);

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
        assertEquals(EstadoBicicleta.DISPONIBLE, bicicleta.getEstado());
        assertEquals(TipoBicicleta.MONTANA, bicicleta.getTipo());
        assertEquals(PRECIO, bicicleta.getPrecioPorHora());
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
        bicicleta.setEstado(EstadoBicicleta.ALQUILADA);
        bicicleta.setUbicacionActual(null);

        Alquiler alquiler = new Alquiler(
                usuario,
                bicicleta,
                INICIO,
                FIN_ESTIMADO,
                COSTO_ESTIMADO,
                EstadoAlquiler.INICIADO,
                origen,
                null
        );
        alquiler.setId(10);
        alquiler.setPrecioPorHoraAplicado(PRECIO);

        when(alquilerRepository.findById(10))
                .thenReturn(Optional.of(alquiler));

        return alquiler;
    }

    private void comprobarDevolucionSinCambios(Alquiler alquiler) {
        assertEquals(EstadoAlquiler.INICIADO, alquiler.getEstado());
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
        assertEquals(EstadoBicicleta.ALQUILADA, bicicleta.getEstado());
        assertEquals(TipoBicicleta.MONTANA, bicicleta.getTipo());
        assertEquals(PRECIO, bicicleta.getPrecioPorHora());
        assertNull(bicicleta.getUbicacionActual());
        assertFalse(bicicleta.isBorradoBici());

        verify(usuarioRepository).findByEmail(EMAIL);
        verify(alquilerRepository).findById(10);
        verify(estacionRepository)
                .buscarActivaPorNombreConBloqueo("Destino QA");

        verify(alquilerRepository, never()).save(any(Alquiler.class));
        verify(bicicletaRepository, never()).save(any(Bicicleta.class));
        verifyNoInteractions(correoService);
        verify(alquilerService, never()).obtenerCotizacionDolar();
    }
}