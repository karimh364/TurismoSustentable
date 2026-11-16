package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReembolsoServiceTest {
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    @Mock ReembolsoRepository reembolsos;
    @Mock PagoRepository pagos;
    @Mock AlquilerRepository alquileres;
    @Mock UsuarioRepository usuarios;
    @Mock HistorialEstadoReembolsoRepository historias;
    @Mock LiquidacionAlquilerService liquidaciones;
    @Mock PasarelaSimulada pasarela;
    private ReembolsoService servicio;
    private Usuario cliente;
    private Alquiler alquiler;
    private Pago pago;

    @BeforeEach void preparar() {
        cliente = new Usuario(); cliente.setId(1); cliente.setEmail("qa.reembolso@example.com");
        cliente.setNickname("qa.reembolso"); cliente.setActivo(true);
        Rol rol = new Rol("CLIENTE"); ReflectionTestUtils.setField(rol, "id", 1);
        cliente.getUsuarioRoles().add(new UsuarioRol(cliente, rol));
        alquiler = new Alquiler(); alquiler.setId(7); alquiler.setUsuario(cliente);
        alquiler.setEstadoContratacion(new EstadoAlquiler("FINALIZADO"));
        alquiler.setFechaHoraLiquidacion(AHORA.minusMinutes(5));
        pago = pago(11, "2000.00");
        servicio = new ReembolsoService(reembolsos, pagos, alquileres, usuarios, historias,
                liquidaciones, pasarela, Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        sesion("CLIENTE");
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void registraSoloDiferenciaConHistoriaSinCobrarNiModificarPago() {
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("1000.00"));
        when(pagos.findByAlquilerIdOrderByIdAsc(7)).thenReturn(List.of(pago));
        AtomicInteger id = new AtomicInteger();
        when(reembolsos.saveAndFlush(any())).thenAnswer(inv -> {
            Reembolso r = inv.getArgument(0); ReflectionTestUtils.setField(r, "id", id.incrementAndGet()); return r;
        });
        servicio.registrarPendientesSiCorresponde(alquiler);
        ArgumentCaptor<Reembolso> captura = ArgumentCaptor.forClass(Reembolso.class);
        verify(reembolsos).saveAndFlush(captura.capture());
        assertEquals(new BigDecimal("1000.00"), captura.getValue().getMontoArs());
        assertEquals("PENDIENTE", captura.getValue().getEstado());
        assertSame(pago, captura.getValue().getPago());
        assertNull(captura.getValue().getFechaConfirmacion());
        assertEquals("APROBADO", pago.getEstadoPago().getCodigo());
        assertEquals(new BigDecimal("2000.00"), pago.getMontoArs());
        verify(historias).save(any(HistorialEstadoReembolso.class));
        verifyNoInteractions(pasarela);
    }

    @Test void distribuyeSinExcederCadaPago() {
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("1500.00"));
        when(pagos.findByAlquilerIdOrderByIdAsc(7)).thenReturn(List.of(pago(11,"1000.00"), pago(12,"1000.00")));
        when(reembolsos.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        servicio.registrarPendientesSiCorresponde(alquiler);
        ArgumentCaptor<Reembolso> captura = ArgumentCaptor.forClass(Reembolso.class);
        verify(reembolsos, times(2)).saveAndFlush(captura.capture());
        assertEquals(new BigDecimal("1000.00"), captura.getAllValues().get(0).getMontoArs());
        assertEquals(new BigDecimal("500.00"), captura.getAllValues().get(1).getMontoArs());
    }

    @ParameterizedTest @ValueSource(strings={"PENDIENTE","EN_PROCESO","FALLIDO"})
    void obligacionExistenteNoSeDuplicaAunqueHayaFallado(String estado) {
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("1000.00"));
        when(pagos.findByAlquilerIdOrderByIdAsc(7)).thenReturn(List.of(pago));
        when(reembolsos.findByPagoAlquilerIdOrderByIdAsc(7)).thenReturn(List.of(reembolso(estado)));
        servicio.registrarPendientesSiCorresponde(alquiler);
        verify(reembolsos, never()).saveAndFlush(any()); verifyNoInteractions(historias,pasarela);
    }

    @Test void sinLiquidacionNoInventaObligacion() {
        alquiler.setFechaHoraLiquidacion(null);
        servicio.registrarPendientesSiCorresponde(alquiler);
        verifyNoInteractions(liquidaciones,reembolsos,pagos,historias,pasarela);
    }

    @Test void noGeneraMovimientoCero() {
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("0.00"));
        when(pagos.findByAlquilerIdOrderByIdAsc(7)).thenReturn(List.of(pago));
        servicio.registrarPendientesSiCorresponde(alquiler);
        verify(reembolsos, never()).saveAndFlush(any()); verifyNoInteractions(historias,pasarela);
    }

    @Test void topeConjuntoRechazaDuplicadosEntrePagos() {
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("500.00"));
        when(reembolsos.findByPagoAlquilerIdOrderByIdAsc(7)).thenReturn(List.of(reembolso("PENDIENTE")));
        assertThrows(ConfiguracionSistemaException.class, () -> servicio.registrarPendientesSiCorresponde(alquiler));
        verifyNoInteractions(pasarela,historias);
    }

    @ParameterizedTest @ValueSource(strings={"CONFIRMADO","EN_PROCESO","FALLIDO"})
    void resolucionConservaObligacionHastaConfirmar(String resultado) {
        Reembolso r = prepararResolucion("PENDIENTE");
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("1000.00"),
                resumen("CONFIRMADO".equals(resultado) ? "0.00" : "1000.00"));
        when(pasarela.evaluarReembolso(r)).thenReturn(resultado);
        ResultadoReembolsoDTO respuesta = servicio.resolver(7);
        assertEquals(resultado, r.getEstado());
        assertEquals("CONFIRMADO".equals(resultado), r.getFechaConfirmacion()!=null);
        assertEquals(new BigDecimal("CONFIRMADO".equals(resultado)?"0.00":"1000.00"),
                respuesta.getLiquidacion().getReembolsoPendienteArs());
        assertEquals("SIM-REEMBOLSO-21", r.getReferenciaExterna());
        assertEquals(new BigDecimal("2000.00"), pago.getMontoArs());
        assertEquals("APROBADO", pago.getEstadoPago().getCodigo());
        verify(historias, times("EN_PROCESO".equals(resultado)?1:2)).save(any());
    }

    @Test void confirmadoNoSeEvaluaNiDuplicaCredito() {
        prepararResolucion("CONFIRMADO");
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("0.00"));
        servicio.resolver(7);
        verifyNoInteractions(pasarela,historias);
    }

    @Test void pendienteEnGestionSeConsultaSinRepetirTransicion() {
        Reembolso r = prepararResolucion("EN_PROCESO");
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("1000.00"));
        when(pasarela.evaluarReembolso(r)).thenReturn("EN_PROCESO");
        servicio.resolver(7); verifyNoInteractions(historias);
    }

    @Test void reintentaFallidoConMismaIdentidad() {
        Reembolso r = prepararResolucion("FALLIDO");
        when(liquidaciones.resumenInterno(alquiler)).thenReturn(resumen("1000.00"),resumen("0.00"));
        when(pasarela.evaluarReembolso(r)).thenReturn("CONFIRMADO");
        assertEquals(21, servicio.resolver(7).getReembolsos().get(0).getId());
        verify(reembolsos,never()).saveAndFlush(any());
        verify(historias,times(2)).save(any());
    }

    @Test void clienteNoPuedeGestionarAlquilerAjenoAunqueTambienSeaAdmin() {
        prepararActor(); sesion("CLIENTE","ADMIN");
        Usuario otro=new Usuario(); otro.setId(2); alquiler.setUsuario(otro);
        assertThrows(AccessDeniedException.class,()->servicio.resolver(7));
        verifyNoInteractions(reembolsos,pagos,liquidaciones,pasarela);
    }

    @ParameterizedTest @ValueSource(strings={"ADMIN","TECNICO"})
    void rolSinClienteNoPuedeResolver(String rol) {
        sesion(rol);
        when(usuarios.buscarActivoPorEmailConBloqueo(cliente.getEmail())).thenReturn(Optional.of(cliente));
        assertThrows(AccessDeniedException.class,()->servicio.resolver(7));
        verifyNoInteractions(alquileres,reembolsos,pagos,pasarela);
    }

    @Test void noPuedeResolverAntesDeLiquidar() {
        prepararActor(); alquiler.setFechaHoraLiquidacion(null);
        assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,()->servicio.resolver(7)).getStatus());
        verifyNoInteractions(liquidaciones,reembolsos,pasarela);
    }

    private void prepararActor() {
        when(usuarios.buscarActivoPorEmailConBloqueo(cliente.getEmail())).thenReturn(Optional.of(cliente));
        when(alquileres.buscarPorIdConBloqueo(7)).thenReturn(Optional.of(alquiler));
    }
    private Reembolso prepararResolucion(String estado) {
        prepararActor(); Reembolso r=reembolso(estado);
        when(reembolsos.findByPagoAlquilerIdOrderByIdAsc(7)).thenReturn(List.of(r));
        when(pagos.findByAlquilerIdOrderByIdAsc(7)).thenReturn(List.of(pago));
        when(pagos.buscarPorIdConBloqueo(11)).thenReturn(Optional.of(pago));
        when(reembolsos.buscarPorIdConBloqueo(21)).thenReturn(Optional.of(r));
        return r;
    }
    private Pago pago(int id,String monto) {
        Pago p=new Pago(alquiler,new EstadoPago("CREADO"),"ANTICIPO",new BigDecimal(monto),
                AHORA.minusMinutes(20),"SIMULADA","PRUEBA","ANTICIPO:1:"+id);
        ReflectionTestUtils.setField(p,"id",id); p.setEstadoPago(new EstadoPago("APROBADO"));
        p.setFechaConfirmacion(AHORA.minusMinutes(19)); return p;
    }
    private Reembolso reembolso(String estado) {
        Reembolso r=new Reembolso(pago,new BigDecimal("1000.00"),AHORA.minusMinutes(4),"Ajuste","AJUSTE:7:11:21");
        ReflectionTestUtils.setField(r,"id",21); r.setEstado(estado);
        if ("EN_PROCESO".equals(estado)) r.setReferenciaExterna("SIM-REEMBOLSO-21");
        if ("CONFIRMADO".equals(estado)) r.setFechaConfirmacion(AHORA.minusMinutes(1));
        return r;
    }
    private LiquidacionAlquilerDTO resumen(String debido) {
        BigDecimal d=new BigDecimal(debido);
        return new LiquidacionAlquilerDTO(7,0,d.signum()>0?"REEMBOLSO_PENDIENTE":"SALDADO",
                AHORA.minusMinutes(5),BigDecimal.ONE,new BigDecimal("1000.00"),new BigDecimal("2000.00"),
                new BigDecimal("1000.00").subtract(d),new BigDecimal("1000.00").add(d),BigDecimal.ZERO,d);
    }
    private void sesion(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                cliente.getEmail(),null,AuthorityUtils.createAuthorityList(roles)));
    }
}
