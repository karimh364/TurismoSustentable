package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import javax.validation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperacionReservaServiceTest {
    private static final LocalDateTime AHORA=LocalDateTime.of(2026,11,16,12,0);
    @Mock AlquilerRepository alquilerRepository;
    @Mock AlquilerDetalleRepository alquilerDetalleRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock BicicletaRepository bicicletaRepository;
    @Mock EstadoAlquilerRepository estadoAlquilerRepository;
    @Mock EstadoAlquilerDetalleRepository estadoAlquilerDetalleRepository;
    @Mock EstadoBicicletaRepository estadoBicicletaRepository;
    @Mock HistorialEstadoDetalleRepository historialEstadoDetalleRepository;
    @Mock TrazabilidadInventarioService trazabilidadInventario;
    @Mock LiquidacionAlquilerService liquidacionAlquilerService;
    @Mock ReembolsoService reembolsoService;
    @Mock OrdenMantenimientoRepository ordenMantenimientoRepository;
    @Mock TipoMantenimientoRepository tipoMantenimientoRepository;
    @Mock EstadoOrdenMantenimientoRepository estadoOrdenMantenimientoRepository;
    @Mock HistorialOrdenMantenimientoRepository historialOrdenMantenimientoRepository;
    @InjectMocks AlquilerService servicio;
    private ValidatorFactory validadores;
    private Usuario usuario;
    private Alquiler alquiler;
    private List<AlquilerDetalle> detalles;

    @BeforeEach void preparar() {
        validadores=Validation.buildDefaultValidatorFactory();
        ReflectionTestUtils.setField(servicio,"validator",validadores.getValidator()); ponerHora(AHORA);
        usuario=new Usuario(); usuario.setId(1); usuario.setActivo(true); usuario.setEmail("qa.cancelar@example.com");
        Rol rol=new Rol("CLIENTE"); ReflectionTestUtils.setField(rol,"id",1);
        usuario.getUsuarioRoles().add(new UsuarioRol(usuario,rol)); sesion("CLIENTE");
        Estacion estacion=new Estacion("Plaza QA","Calle QA",4,new BigDecimal("0.0"),new BigDecimal("0.0"));
        estacion.setEsPublica(true); ReflectionTestUtils.setField(estacion,"id",1);
        alquiler=new Alquiler(); alquiler.setId(7); alquiler.setUsuario(usuario); alquiler.setUbicacionRetiro(estacion);
        alquiler.setEstadoContratacion(new EstadoAlquiler("RESERVADO")); alquiler.setFechaHoraCreacion(AHORA.minusMinutes(5));
        alquiler.setFechaHoraVencimientoReserva(AHORA.plusMinutes(10));
        alquiler.setCotizacionArsPorUsd(new BigDecimal("1530.00"));
        detalles=new ArrayList<>();
        for(int i=1;i<=2;i++) {
            TipoBicicleta tipo=new TipoBicicleta("PASEO",new BigDecimal("8.00"),3);
            ReflectionTestUtils.setField(tipo,"id",1);
            Bicicleta bici=new Bicicleta("QA-CANCEL-"+i,tipo,new EstadoBicicleta("RESERVADA"),estacion);
            bici.setId(i); bici.setFechaAlta(AHORA.toLocalDate().minusDays(2));
            AlquilerDetalle d=new AlquilerDetalle(); ReflectionTestUtils.setField(d,"id",i);
            d.setAlquiler(alquiler); d.setBicicleta(bici); d.setTipoBicicletaContratado(tipo);
            d.setEstado(new EstadoAlquilerDetalle("RESERVADO")); d.setPrecioPorHoraAplicadoUsd(new BigDecimal("8.00"));
            d.setDuracionEstimadaSegundos(3600); d.setSubtotalEstimadoUsd(new BigDecimal("8.00"));
            d.setSubtotalEstimadoArs(new BigDecimal("12240.00")); detalles.add(d);
            lenient().when(bicicletaRepository.buscarActivaPorIdConBloqueo(i)).thenReturn(Optional.of(bici));
        }
        lenient().when(usuarioRepository.buscarActivoPorEmailConBloqueo(usuario.getEmail())).thenReturn(Optional.of(usuario));
        lenient().when(alquilerRepository.buscarPorIdConBloqueo(7)).thenReturn(Optional.of(alquiler));
        lenient().when(alquilerDetalleRepository.buscarPorAlquiler(7)).thenReturn(detalles);
        lenient().when(alquilerDetalleRepository.buscarPorAlquilerConDatos(7)).thenReturn(detalles);
        lenient().when(estadoAlquilerDetalleRepository.findByCodigo(anyString())).thenAnswer(i->Optional.of(new EstadoAlquilerDetalle(i.getArgument(0))));
        lenient().when(estadoAlquilerRepository.findByCodigo(anyString())).thenAnswer(i->Optional.of(new EstadoAlquiler(i.getArgument(0))));
        lenient().when(estadoBicicletaRepository.findByCodigo(anyString())).thenAnswer(i->Optional.of(new EstadoBicicleta(i.getArgument(0))));
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); validadores.close(); }

    @Test void cancelarUnaLiberaExclusividadPeroConservaLugarYEstimacion() {
        servicio.cancelarReserva(7,1,null,false);
        assertEquals("CANCELADO",detalles.get(0).getEstado().getCodigo());
        assertEquals("CLIENTE",detalles.get(0).getOrigenCierre());
        assertEquals(new BigDecimal("0.00"),detalles.get(0).getSubtotalRealArs());
        assertNull(detalles.get(0).getFechaHoraRetiro()); assertNull(detalles.get(0).getFechaHoraFinUso());
        assertEquals(new BigDecimal("12240.00"),detalles.get(0).getSubtotalEstimadoArs());
        assertNotNull(detalles.get(0).getBicicleta().getUbicacionActual());
        assertEquals("DISPONIBLE",detalles.get(0).getBicicleta().getEstado().getCodigo());
        assertEquals("RESERVADO",detalles.get(1).getEstado().getCodigo());
        assertEquals("RESERVADO",alquiler.getEstadoContratacion().getCodigo());
        verifyNoInteractions(liquidacionAlquilerService,reembolsoService);
    }

    @Test void cancelacionTotalCierraCostoSinUsoYDelegaLiquidacionYObligacion() {
        servicio.cancelarReserva(7,null,null,false);
        assertEquals("CANCELADO",alquiler.getEstadoContratacion().getCodigo());
        assertEquals(AHORA,alquiler.getFechaHoraCierre());
        assertEquals(AHORA,alquiler.getFechaHoraCancelacion());
        verify(liquidacionAlquilerService).liquidarSiCorresponde(alquiler);
        verify(reembolsoService).registrarPendientesSiCorresponde(alquiler);
        verify(historialEstadoDetalleRepository,times(2)).save(any());
    }

    @Test void enUsoNoPuedeCancelarse() {
        detalles.get(0).setEstado(new EstadoAlquilerDetalle("EN_CURSO"));
        detalles.get(0).setFechaHoraRetiro(AHORA.minusMinutes(1));
        assertThrows(IllegalStateException.class,()->servicio.cancelarReserva(7,1,null,false));
        verifyNoInteractions(trazabilidadInventario,historialEstadoDetalleRepository,liquidacionAlquilerService);
    }

    @Test void cancelacionParcialNoDetieneLaOtraUnidadEnUso() {
        detalles.get(0).setEstado(new EstadoAlquilerDetalle("EN_CURSO"));
        detalles.get(0).setFechaHoraRetiro(AHORA.minusMinutes(1));
        alquiler.setEstadoContratacion(new EstadoAlquiler("EN_PROGRESO"));
        servicio.cancelarReserva(7,2,null,false);
        assertEquals("EN_CURSO",detalles.get(0).getEstado().getCodigo());
        assertEquals("EN_PROGRESO",alquiler.getEstadoContratacion().getCodigo());
        verifyNoInteractions(liquidacionAlquilerService,reembolsoService);
    }

    @Test void reintentoNoTocaUnaBiciYaAsignadaAOtroAlquiler() {
        servicio.cancelarReserva(7,1,null,false);
        clearInvocations(trazabilidadInventario,historialEstadoDetalleRepository,bicicletaRepository);
        detalles.get(0).getBicicleta().setEstado(new EstadoBicicleta("RESERVADA"));
        servicio.cancelarReserva(7,1,null,false);
        verifyNoInteractions(trazabilidadInventario,historialEstadoDetalleRepository,bicicletaRepository);
    }

    @Test void alcanzarLimiteVenceEnLugarDeCancelarYRegistraSistema() {
        ponerHora(alquiler.getFechaHoraVencimientoReserva());
        servicio.cancelarReserva(7,1,null,false);
        assertEquals("VENCIDO",alquiler.getEstadoContratacion().getCodigo());
        assertEquals("VENCIDO",detalles.get(1).getEstado().getCodigo());
        assertEquals("SISTEMA",detalles.get(0).getOrigenCierre());
        verify(trazabilidadInventario,times(2)).estadoSistema(any(),any(),anyString(),any());
        verify(trazabilidadInventario,never()).estado(any(),any(),any(),anyString(),any());
    }

    @Test void vencerAntesDelLimiteNoEscribe() {
        assertEquals(0,servicio.vencerReserva(7));
        verifyNoInteractions(bicicletaRepository,historialEstadoDetalleRepository,trazabilidadInventario);
    }

    @Test void vencimientoSoloCierraPendientesNoElViajeEnCurso() {
        detalles.get(0).setEstado(new EstadoAlquilerDetalle("EN_CURSO"));
        detalles.get(0).setFechaHoraRetiro(AHORA.minusMinutes(1));
        alquiler.setEstadoContratacion(new EstadoAlquiler("EN_PROGRESO"));
        ponerHora(alquiler.getFechaHoraVencimientoReserva());
        assertEquals(1,servicio.vencerReserva(7));
        assertEquals("EN_CURSO",detalles.get(0).getEstado().getCodigo());
        assertEquals("EN_PROGRESO",alquiler.getEstadoContratacion().getCodigo());
        verifyNoInteractions(liquidacionAlquilerService,reembolsoService);
    }

    @Test void cerrarUltimaPendienteTrasDevolucionPermiteLiquidarConjunto() {
        detalles.get(0).setEstado(new EstadoAlquilerDetalle("DEVUELTO"));
        detalles.get(0).setFechaHoraRetiro(AHORA.minusMinutes(2));
        alquiler.setEstadoContratacion(new EstadoAlquiler("EN_PROGRESO"));
        servicio.cancelarReserva(7,2,null,false);
        assertEquals("FINALIZADO",alquiler.getEstadoContratacion().getCodigo());
        verify(liquidacionAlquilerService).liquidarSiCorresponde(alquiler);
    }

    @Test void cancelacionAjenaSeRechazaAntesDeInventario() {
        Usuario otro=new Usuario(); otro.setId(2); alquiler.setUsuario(otro);
        assertThrows(AccessDeniedException.class,()->servicio.cancelarReserva(7,1,null,false));
        verifyNoInteractions(bicicletaRepository,historialEstadoDetalleRepository,trazabilidadInventario);
    }

    @Test void adminSinClienteNoPuedeHacerCancelacionVoluntaria() {
        agregarAdmin(); sesion("ADMIN");
        assertThrows(AccessDeniedException.class,()->servicio.cancelarReserva(7,1,null,false));
        verifyNoInteractions(bicicletaRepository,historialEstadoDetalleRepository,trazabilidadInventario);
    }

    @Test void administracionExigeMotivoYDestinoOperativo() {
        agregarAdmin(); sesion("ADMIN");
        assertThrows(IllegalArgumentException.class,()->servicio.cancelarReserva(7,1,new CancelarDetalleDTO(),true));
        verifyNoInteractions(bicicletaRepository,historialEstadoDetalleRepository,trazabilidadInventario);
    }

    @Test void cancelacionAdministrativaInactivaYAuditaSinBorrarLaUnidad() {
        agregarAdmin(); sesion("ADMIN");
        CancelarDetalleDTO solicitud=new CancelarDetalleDTO(); solicitud.setMotivo("Cierre operativo de la unidad");
        solicitud.setEstadoBicicleta("INACTIVA"); servicio.cancelarReserva(7,1,solicitud,true);
        assertEquals("ADMIN",detalles.get(0).getOrigenCierre());
        assertEquals("INACTIVA",detalles.get(0).getBicicleta().getEstado().getCodigo());
        assertFalse(detalles.get(0).getBicicleta().isBorradoBici());
        verify(trazabilidadInventario).administracion(eq(usuario),eq("ALQUILER_DETALLE"),eq(1),eq("CANCELAR"),anyString(),anyString(),eq(solicitud.getMotivo()));
    }

    @Test void asignacionDiferenteNoSeLibera() {
        when(alquilerDetalleRepository.existeOtraAsignacionVigente(1,1)).thenReturn(true);
        assertThrows(ConfiguracionSistemaException.class,()->servicio.cancelarReserva(7,1,null,false));
        verifyNoInteractions(historialEstadoDetalleRepository,trazabilidadInventario);
    }

    private void agregarAdmin() { Rol r=new Rol("ADMIN"); ReflectionTestUtils.setField(r,"id",2); usuario.getUsuarioRoles().add(new UsuarioRol(usuario,r)); }
    private void sesion(String...roles) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(usuario.getEmail(),null,AuthorityUtils.createAuthorityList(roles))); }
    private void ponerHora(LocalDateTime hora) { ReflectionTestUtils.setField(servicio,"reloj",Clock.fixed(hora.toInstant(ZoneOffset.UTC),ZoneOffset.UTC)); }
}
