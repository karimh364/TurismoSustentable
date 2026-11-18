package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidenciaServiceTest {
    @Mock IncidenciaRepository incidencias;@Mock HistorialIncidenciaRepository historias;
    @Mock AlquilerRepository alquileres;@Mock AlquilerDetalleRepository detalles;@Mock BicicletaRepository bicicletas;
    @Mock UsuarioRepository usuarios;@Mock EstacionRepository estaciones;@Mock EstadoAlquilerDetalleRepository estadosDetalle;
    @Mock EstadoAlquilerRepository estadosAlquiler;@Mock EstadoBicicletaRepository estadosBici;
    @Mock HistorialEstadoDetalleRepository historiasDetalle;@Mock OrdenMantenimientoRepository ordenes;
    @Mock HistorialOrdenMantenimientoRepository historiasOrden;@Mock TipoMantenimientoRepository tiposOrden;
    @Mock EstadoOrdenMantenimientoRepository estadosOrden;@Mock TrazabilidadInventarioService traza;
    @Mock LiquidacionAlquilerService liquidacion;@Mock ReembolsoService reembolsos;
    static ValidatorFactory factory;IncidenciaService servicio;
    final LocalDateTime ahora=LocalDateTime.of(2026,11,16,15,0);
    Usuario cliente,admin,otro,tecnico;Alquiler a;AlquilerDetalle d;Bicicleta b;Incidencia i;Estacion e;
    List<Incidencia> reportes;List<OrdenMantenimiento> abiertas;
    @BeforeAll static void iniciar(){factory=Validation.buildDefaultValidatorFactory();}
    @AfterAll static void cerrar(){factory.close();}
    @BeforeEach void preparar(){
        servicio=new IncidenciaService(incidencias,historias,alquileres,detalles,bicicletas,usuarios,estaciones,estadosDetalle,estadosAlquiler,estadosBici,historiasDetalle,
                ordenes,historiasOrden,tiposOrden,estadosOrden,traza,new CalculadoraAlquiler(),liquidacion,factory.getValidator(),Clock.fixed(ahora.toInstant(ZoneOffset.UTC),ZoneOffset.UTC),new ObjectMapper());
        ReflectionTestUtils.setField(servicio,"reembolsos",reembolsos);
        cliente=usuario(1,"CLIENTE");admin=usuario(2,"ADMIN");otro=usuario(3,"CLIENTE");tecnico=usuario(4,"TECNICO");
        e=new Estacion("Taller","Calle 1",2,new BigDecimal("-31.4"),new BigDecimal("-64.2"));ReflectionTestUtils.setField(e,"id",5);e.setEsPublica(false);
        TipoBicicleta tipo=new TipoBicicleta("PASEO",new BigDecimal("8.00"),3);ReflectionTestUtils.setField(tipo,"id",2);
        b=new Bicicleta("INC-01",tipo,new EstadoBicicleta("EN_USO"),null);ReflectionTestUtils.setField(b,"id",20);ReflectionTestUtils.setField(b,"version",0);
        a=new Alquiler();ReflectionTestUtils.setField(a,"id",100);a.setUsuario(cliente);a.setEstadoContratacion(new EstadoAlquiler("EN_PROGRESO"));a.setCotizacionArsPorUsd(new BigDecimal("1530.00"));
        d=new AlquilerDetalle();ReflectionTestUtils.setField(d,"id",10);ReflectionTestUtils.setField(d,"version",0);d.setAlquiler(a);d.setBicicleta(b);d.setTipoBicicletaContratado(tipo);
        d.setEstado(new EstadoAlquilerDetalle("EN_CURSO"));d.setPrecioPorHoraAplicadoUsd(new BigDecimal("8.00"));d.setFechaHoraRetiro(ahora.minusHours(2));
        i=new Incidencia();ReflectionTestUtils.setField(i,"id",7);ReflectionTestUtils.setField(i,"version",0);i.setAlquilerDetalle(d);i.setRegistradoPor(cliente);i.setInformante(cliente);
        i.setTipo("AVERIA");i.setDescripcion("Freno dañado");i.setFechaHoraHechoInformada(ahora.minusMinutes(75));i.setFechaHoraRegistro(ahora.minusHours(1));i.setRequiereRevisionEconomica(true);
        reportes=new ArrayList<>(List.of(i));abiertas=new ArrayList<>();
        Map<String,Usuario> cuentas=Map.of(cliente.getEmail(),cliente,admin.getEmail(),admin,otro.getEmail(),otro,tecnico.getEmail(),tecnico);
        lenient().when(usuarios.buscarActivoPorEmailConBloqueo(anyString())).thenAnswer(c->Optional.ofNullable(cuentas.get(c.getArgument(0))));
        lenient().when(usuarios.buscarPorEmailConDatosYRoles(anyString())).thenAnswer(c->Optional.ofNullable(cuentas.get(c.getArgument(0))));
        lenient().when(usuarios.findById(anyInt())).thenAnswer(c->cuentas.values().stream().filter(u->u.getId()==(Integer)c.getArgument(0)).findFirst());
        lenient().when(alquileres.buscarPorIdConBloqueo(100)).thenReturn(Optional.of(a));
        lenient().when(detalles.buscarPorAlquilerYDetalleConBloqueo(100,10)).thenReturn(Optional.of(d));
        lenient().when(detalles.buscarPorAlquiler(100)).thenReturn(List.of(d));
        lenient().when(bicicletas.findById(20)).thenReturn(Optional.of(b));lenient().when(bicicletas.buscarPorIdConBloqueo(20)).thenReturn(Optional.of(b));
        lenient().when(incidencias.findById(7)).thenReturn(Optional.of(i));lenient().when(incidencias.buscarPorIdConBloqueo(7)).thenReturn(Optional.of(i));
        lenient().when(incidencias.buscarAlquilerId(7)).thenReturn(Optional.of(100));
        lenient().when(incidencias.findByAlquilerDetalleIdOrderByIdAsc(10)).thenAnswer(c->reportes);
        lenient().when(incidencias.saveAndFlush(any())).thenAnswer(c->{Incidencia n=c.getArgument(0);ReflectionTestUtils.setField(n,"id",8);ReflectionTestUtils.setField(n,"version",0);reportes.add(n);return n;});
        lenient().when(estadosDetalle.findByCodigo(anyString())).thenAnswer(c->Optional.of(new EstadoAlquilerDetalle(c.getArgument(0))));
        lenient().when(estadosAlquiler.findByCodigo(anyString())).thenAnswer(c->Optional.of(new EstadoAlquiler(c.getArgument(0))));
        lenient().when(estadosBici.findByCodigo(anyString())).thenAnswer(c->Optional.of(new EstadoBicicleta(c.getArgument(0))));
        lenient().when(tiposOrden.findByCodigo("CORRECTIVO")).thenReturn(Optional.of(new TipoMantenimiento("CORRECTIVO","Correctivo")));
        lenient().when(estadosOrden.findByCodigo("CREADA")).thenReturn(Optional.of(new EstadoOrdenMantenimiento("CREADA")));
        lenient().when(ordenes.buscarAbiertasPorBicicleta(20)).thenAnswer(c->abiertas);
        lenient().when(ordenes.saveAndFlush(any())).thenAnswer(c->{OrdenMantenimiento o=c.getArgument(0);ReflectionTestUtils.setField(o,"id",30);abiertas.add(o);return o;});
        lenient().when(estaciones.buscarActivaPorIdConBloqueo(5)).thenReturn(Optional.of(e));
        sesion(cliente,"CLIENTE");
    }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    @Test void reportarConservaHechoYServidorSinCerrarNiCongelar(){
        reportes.clear();var r=servicio.reportar(reporte());
        assertEquals("PENDIENTE",r.estadoRevision);assertEquals(ahora,r.fechaHoraRegistro);assertEquals(ahora.minusMinutes(75),r.fechaHoraHechoInformada);
        assertTrue(r.requiereRevisionEconomica);assertEquals("EN_CURSO",d.getEstado().getCodigo());assertEquals("EN_USO",b.getEstado().getCodigo());assertNull(d.getFechaHoraFinUso());
        verify(historias).save(argThat(h->h.getEstadoAnterior()==null&&"PENDIENTE".equals(h.getEstadoNuevo())&&h.getActorUsuario()==cliente&&h.getValoresNuevos().contains("Freno dañado")));
        verifyNoInteractions(liquidacion,reembolsos,historiasDetalle,historiasOrden);
    }
    @Test void clienteAjenoNoReportaNiConsulta(){sesion(otro,"CLIENTE");assertThrows(AccessDeniedException.class,()->servicio.reportar(reporte()));assertThrows(AccessDeniedException.class,()->servicio.consultar(7));verifyNoInteractions(historias);}
    @Test void adminReportaOtroCanalConInformanteSeparado(){reportes.clear();sesion(admin,"ADMIN");var r=reporte();r.informanteId=1;var salida=servicio.reportar(r);assertEquals(2,salida.registradoPorId);assertEquals(1,salida.informanteId);verify(traza).administracion(eq(admin),eq("INCIDENCIA"),anyInt(),eq("REPORTAR"),isNull(),anyString(),anyString(),any());}
    @Test void clienteNoEligeOtroInformante(){var r=reporte();r.informanteId=3;assertThrows(AccessDeniedException.class,()->servicio.reportar(r));}
    @Test void tecnicoSoloNoPuedeConsultar(){sesion(tecnico,"TECNICO");assertThrows(AccessDeniedException.class,()->servicio.listar());}
    @Test void clienteInactivoNoActua(){cliente.setActivo(false);assertThrows(AccessDeniedException.class,()->servicio.reportar(reporte()));}
    @Test void sesionConRolRevocadoNoActua(){cliente.getUsuarioRoles().clear();assertThrows(AccessDeniedException.class,()->servicio.reportar(reporte()));}
    @Test void rolPersistidoSinAutoridadDeSesionNoActua(){sesion(cliente,"TECNICO");assertThrows(AccessDeniedException.class,()->servicio.reportar(reporte()));}
    @Test void reporteIdenticoEsIdempotenteInclusoTrasCerrar(){d.setEstado(new EstadoAlquilerDetalle("CERRADO_POR_INCIDENCIA"));assertEquals(7,servicio.reportar(reporte()).id);verify(incidencias,never()).saveAndFlush(any());verify(historias,never()).save(any());}
    @Test void versionDetalleDesactualizadaNoCrea(){reportes.clear();var r=reporte();r.versionDetalle=99;codigo(409,()->servicio.reportar(r));}
    @Test void noReportaUnidadSinRetiro(){reportes.clear();d.setEstado(new EstadoAlquilerDetalle("RESERVADO"));codigo(409,()->servicio.reportar(reporte()));}
    @Test void fechaAnteriorAlRetiroNoCrea(){reportes.clear();var r=reporte();r.fechaHoraHechoInformada=ahora.minusHours(3).atOffset(ZoneOffset.UTC);codigo(400,()->servicio.reportar(r));}
    @Test void fechaFuturaNoCrea(){reportes.clear();var r=reporte();r.fechaHoraHechoInformada=ahora.plusSeconds(1).atOffset(ZoneOffset.UTC);codigo(400,()->servicio.reportar(r));}
    @Test void formatoYLongitudesSeValidanEnServicio(){var r=reporte();r.descripcion=" ";codigo(400,()->servicio.reportar(r));r.descripcion="x".repeat(10001);codigo(400,()->servicio.reportar(r));}
    @ParameterizedTest @ValueSource(strings={"AVERIA","ACCIDENTE","PERDIDA","ROBO"})
    void validarCierraSoloUsoYSeparaSituacionFisica(String tipo){i.setTipo(tipo);sesion(admin,"ADMIN");var r=servicio.resolver(7,decision("VALIDADA"));
        assertEquals("CERRADO_POR_INCIDENCIA",r.estadoDetalle);assertTrue(r.esCorteAplicado);assertEquals(i,d.getIncidenciaCorte());assertNull(d.getFechaHoraDevolucionReal());assertNull(d.getEstacionDevolucion());
        assertEquals(ahora.minusMinutes(75),d.getFechaHoraFinUso());assertEquals(new BigDecimal("6.00"),d.getSubtotalRealUsd());assertEquals(new BigDecimal("9180.00"),d.getSubtotalRealArs());
        assertEquals(Set.of("PERDIDA","ROBO").contains(tipo)?"EXTRAVIADA":"MANTENIMIENTO",b.getEstado().getCodigo());assertNull(b.getUbicacionActual());
        assertEquals("FINALIZADO",a.getEstadoContratacion().getCodigo());verify(liquidacion).liquidarSiCorresponde(a);verify(reembolsos).registrarPendientesSiCorresponde(a);
        verify(historias).save(argThat(h->"PENDIENTE".equals(h.getEstadoAnterior())&&"VALIDADA".equals(h.getEstadoNuevo())&&h.getFechaHoraCambio().equals(ahora)));
        if(tipo.equals("AVERIA")||tipo.equals("ACCIDENTE")){assertEquals(1,abiertas.size());assertEquals("CREADA",abiertas.get(0).getEstado().getCodigo());}
        else verifyNoInteractions(historiasOrden);
    }
    @Test void clienteNoResuelveNiRecibeNiDaDeBaja(){assertThrows(AccessDeniedException.class,()->servicio.resolver(7,decision("VALIDADA")));assertThrows(AccessDeniedException.class,()->servicio.recibir(7,recepcion()));assertThrows(AccessDeniedException.class,()->servicio.perdidaDefinitiva(7,perdida(),false));}
    @Test void versionViejaDeIncidenciaSeRechaza(){sesion(admin,"ADMIN");var r=decision("VALIDADA");r.version=8;codigo(409,()->servicio.resolver(7,r));verifyNoInteractions(historias);}
    @Test void versionViejaDeBicicletaSeRechaza(){sesion(admin,"ADMIN");var r=decision("VALIDADA");r.versionBicicleta=8;codigo(409,()->servicio.resolver(7,r));verifyNoInteractions(historias);}
    @Test void validacionExigeCorteNoPosteriorAlReporte(){sesion(admin,"ADMIN");var r=decision("VALIDADA");r.fechaHoraCorteValidado=ahora.minusMinutes(59).atOffset(ZoneOffset.UTC);codigo(400,()->servicio.resolver(7,r));r.fechaHoraCorteValidado=null;codigo(400,()->servicio.resolver(7,r));}
    @Test void descartarNoAdmiteCorte(){sesion(admin,"ADMIN");var r=decision("DESCARTADA");r.fechaHoraCorteValidado=ahora.minusMinutes(75).atOffset(ZoneOffset.UTC);codigo(400,()->servicio.resolver(7,r));}
    @Test void descartarNoCierraUsoNiDeclaraCulpa(){sesion(admin,"ADMIN");servicio.resolver(7,decision("DESCARTADA"));assertEquals("EN_CURSO",d.getEstado().getCodigo());assertNull(d.getFechaHoraFinUso());assertEquals("EN_USO",b.getEstado().getCodigo());verifyNoInteractions(liquidacion,historiasDetalle);}
    @Test void repeticionDeDecisionNoDuplicaEventos(){sesion(admin,"ADMIN");var r=decision("VALIDADA");servicio.resolver(7,r);servicio.resolver(7,r);verify(historias,times(1)).save(any());verify(historiasDetalle,times(1)).save(any());}
    @Test void decisionDistintaNoSobrescribeLaAnterior(){sesion(admin,"ADMIN");servicio.resolver(7,decision("VALIDADA"));codigo(409,()->servicio.resolver(7,decision("DESCARTADA")));}
    @Test void devolucionPreviaConservaFisicoYCobraHastaCorte(){devuelta();sesion(admin,"ADMIN");servicio.resolver(7,decision("VALIDADA"));assertEquals("DEVUELTO",d.getEstado().getCodigo());assertEquals(ahora.minusHours(1),d.getFechaHoraDevolucionReal());assertEquals(e,d.getEstacionDevolucion());assertEquals(new BigDecimal("6.00"),d.getSubtotalRealUsd());assertEquals(ahora.minusHours(1),a.getFechaHoraCierre());verifyNoInteractions(historiasDetalle);}
    @Test void devolucionDescartadaUsaRecepcionNoHoraDeDecision(){devuelta();sesion(admin,"ADMIN");servicio.resolver(7,decision("DESCARTADA"));assertNull(d.getIncidenciaCorte());assertEquals(new BigDecimal("8.00"),d.getSubtotalRealUsd());assertEquals(ahora.minusHours(1),d.getFechaHoraFinUso());}
    @Test void liquidacionDefinitivaNoSeReabre(){devuelta();a.setFechaHoraLiquidacion(ahora.minusMinutes(30));sesion(admin,"ADMIN");codigo(409,()->servicio.resolver(7,decision("VALIDADA")));verifyNoInteractions(historias);}
    @Test void variasRevisionesMantienenCostoProvisional(){Incidencia otra=new Incidencia();ReflectionTestUtils.setField(otra,"id",8);otra.setEstadoRevision("PENDIENTE");otra.setRequiereRevisionEconomica(true);reportes.add(otra);sesion(admin,"ADMIN");servicio.resolver(7,decision("VALIDADA"));assertNull(d.getSubtotalRealUsd());assertNull(d.getSubtotalRealArs());}
    @Test void noCambiaOtraBicicletaNiCierraCabeceraSiOtraSigueEnUso(){AlquilerDetalle otro=new AlquilerDetalle();otro.setEstado(new EstadoAlquilerDetalle("EN_CURSO"));when(detalles.buscarPorAlquiler(100)).thenReturn(List.of(d,otro));sesion(admin,"ADMIN");servicio.resolver(7,decision("VALIDADA"));assertEquals("EN_PROGRESO",a.getEstadoContratacion().getCodigo());verifyNoInteractions(liquidacion);}
    @Test void otraIncidenciaNoReemplazaCorteDistinto(){Incidencia anterior=new Incidencia();ReflectionTestUtils.setField(anterior,"id",9);d.setIncidenciaCorte(anterior);d.setFechaHoraFinUso(ahora.minusMinutes(80));sesion(admin,"ADMIN");codigo(409,()->servicio.resolver(7,decision("VALIDADA")));}
    @Test void recepcionNormalGuardaHechoYVinculoSinResolver(){devuelta();servicio.registrarDevolucion(d,cliente,ahora.minusHours(1));assertEquals("PENDIENTE",i.getEstadoRevision());assertEquals(e,i.getEstacionRecepcion());assertEquals(cliente,i.getRecibidoPor());verify(historias).save(argThat(h->h.getEstadoAnterior()==null&&h.getEstadoNuevo()==null));}
    @Test void recuperacionRealDeExtraviadaExigeRevisionSinReabrirUso(){cerrada("PERDIDA");sesion(admin,"ADMIN");var r=servicio.recibir(7,recepcion());assertEquals("MANTENIMIENTO",b.getEstado().getCodigo());assertEquals(e,b.getUbicacionActual());assertEquals(ahora,r.fechaHoraRecepcionFisica);assertEquals("CERRADO_POR_INCIDENCIA",d.getEstado().getCodigo());assertNull(d.getFechaHoraDevolucionReal());assertEquals(1,abiertas.size());verifyNoInteractions(liquidacion,reembolsos);}
    @Test void recepcionFueraDeEstacionNoInventaUnCupo(){cerrada("AVERIA");sesion(admin,"ADMIN");var r=recepcion();r.estacionId=null;r.lugarRecepcion="  Base móvil de asistencia  ";servicio.recibir(7,r);assertNull(b.getUbicacionActual());assertEquals("Base móvil de asistencia",i.getLugarRecepcion());verifyNoInteractions(estaciones);}
    @Test void recepcionRechazaEstacionLlena(){cerrada("PERDIDA");sesion(admin,"ADMIN");when(bicicletas.countByUbicacionActual(e)).thenReturn(2L);codigo(409,()->servicio.recibir(7,recepcion()));assertNull(i.getFechaHoraRecepcionFisica());assertEquals("EXTRAVIADA",b.getEstado().getCodigo());}
    @Test void recepcionNoAceptaDosUbicacionesNiConfirmacionFalsa(){cerrada("PERDIDA");sesion(admin,"ADMIN");var r=recepcion();r.lugarRecepcion="Taller";codigo(400,()->servicio.recibir(7,r));r.lugarRecepcion=null;r.recepcionFisicaConfirmada=false;codigo(400,()->servicio.recibir(7,r));}
    @Test void recepcionRepetidaNoDuplicaNiVuelveAFacturar(){cerrada("PERDIDA");sesion(admin,"ADMIN");servicio.recibir(7,recepcion());servicio.recibir(7,recepcion());verify(historias,times(1)).save(any());verifyNoInteractions(liquidacion,reembolsos);}
    @Test void ingresoPosteriorConservaLaRecepcionOriginalFueraDeEstacion(){cerrada("AVERIA");i.setFechaHoraRecepcionFisica(ahora.minusMinutes(5));i.setLugarRecepcion("Base móvil");i.setRecibidoPor(admin);sesion(admin,"ADMIN");servicio.ubicar(7,recepcion());assertEquals(e,b.getUbicacionActual());assertNull(i.getEstacionRecepcion());assertEquals("Base móvil",i.getLugarRecepcion());assertEquals(ahora.minusMinutes(5),i.getFechaHoraRecepcionFisica());verifyNoInteractions(liquidacion,reembolsos);}
    @Test void noUbicaSinRecepcionPrevia(){cerrada("AVERIA");sesion(admin,"ADMIN");codigo(409,()->servicio.ubicar(7,recepcion()));}
    @Test void ingresoPosteriorNoSuperaCapacidad(){cerrada("AVERIA");i.setFechaHoraRecepcionFisica(ahora.minusMinutes(5));i.setLugarRecepcion("Base móvil");i.setRecibidoPor(admin);sesion(admin,"ADMIN");when(bicicletas.countByUbicacionActual(e)).thenReturn(2L);codigo(409,()->servicio.ubicar(7,recepcion()));assertNull(b.getUbicacionActual());}
    @Test void encontrarUnaBajaNoLaRestaura(){cerrada("PERDIDA");b.setEstado(new EstadoBicicleta("DE_BAJA"));sesion(admin,"ADMIN");codigo(409,()->servicio.recibir(7,recepcion()));}
    @Test void perdidaDefinitivaYEliminacionConservanHistoriaSinSalidaFicticia(){cerrada("ROBO");sesion(admin,"ADMIN");servicio.perdidaDefinitiva(7,perdida(),false);assertEquals("DE_BAJA",b.getEstado().getCodigo());assertFalse(b.isBorradoBici());assertNull(b.getUbicacionActual());
        when(historias.existsByIncidenciaIdAndAccion(7,"BAJA_PERDIDA")).thenReturn(true);servicio.perdidaDefinitiva(7,perdida(),true);assertTrue(b.isBorradoBici());assertNull(b.getUbicacionActual());verifyNoInteractions(estaciones,liquidacion,reembolsos);verify(historias,times(2)).save(any());}
    @Test void soloDeBajaSinRespaldoNoPermiteEliminar(){cerrada("PERDIDA");b.setEstado(new EstadoBicicleta("DE_BAJA"));sesion(admin,"ADMIN");codigo(409,()->servicio.perdidaDefinitiva(7,perdida(),true));assertFalse(b.isBorradoBici());}
    @Test void reportePendienteNoPermiteBajaPorPerdida(){i.setTipo("PERDIDA");sesion(admin,"ADMIN");codigo(409,()->servicio.perdidaDefinitiva(7,perdida(),false));}
    @Test void perdidaConOtroProcesoActivoNoPermiteBaja(){cerrada("PERDIDA");sesion(admin,"ADMIN");when(detalles.contarAsignacionesActivas(20)).thenReturn(1L);codigo(409,()->servicio.perdidaDefinitiva(7,perdida(),false));}
    @Test void averiaNoSeBorraComoPerdida(){cerrada("AVERIA");sesion(admin,"ADMIN");codigo(409,()->servicio.perdidaDefinitiva(7,perdida(),false));}
    @Test void recepcionAnteriorImpideDeclararNoRecuperada(){cerrada("PERDIDA");i.setFechaHoraRecepcionFisica(ahora.minusMinutes(1));sesion(admin,"ADMIN");codigo(409,()->servicio.perdidaDefinitiva(7,perdida(),false));}
    @Test void clienteRecibeSeguimientoSinNombresNiIdsDeEstacionesInternas(){devuelta();i.setEstacionRecepcion(e);var r=servicio.consultar(7);assertNull(r.estacionRecepcionId);assertNull(r.estacionActualId);assertNull(r.ubicacionActual);assertFalse(r.estacionRecepcion.contains(e.getNombre()));sesion(admin,"ADMIN");assertEquals(e.getNombre(),servicio.consultar(7).estacionRecepcion);}
    @Test void historialClienteConservaEventosSinSnapshotsInternos(){var h=new HistorialIncidencia(i,"RECIBIR",null,null,admin,"USUARIO","Recibida",null,"{\"estacionRecepcionId\":5}",ahora);when(historias.findByIncidenciaIdOrderByIdAsc(7)).thenReturn(List.of(h));assertNull(servicio.historial(7).get(0).valoresNuevos);sesion(admin,"ADMIN");assertNotNull(servicio.historial(7).get(0).valoresNuevos);}
    @Test void historialAjenoNoSeExpone(){sesion(otro,"CLIENTE");assertThrows(AccessDeniedException.class,()->servicio.historial(7));verify(historias,never()).findByIncidenciaIdOrderByIdAsc(anyInt());}
    @Test void listadoPropioNoSeConvierteEnAdministrativo(){when(incidencias.listarVisibles(1)).thenReturn(reportes);assertEquals(1,servicio.listar().size());verify(incidencias).listarVisibles(1);}
    @Test void valorCongeladoInvalidoEsFalloInternoNoErrorDelVisitante(){sesion(admin,"ADMIN");a.setCotizacionArsPorUsd(null);assertThrows(ConfiguracionSistemaException.class,()->servicio.resolver(7,decision("VALIDADA")));}
    @Test void configuracionFaltanteNoSeInventa(){sesion(admin,"ADMIN");when(estadosDetalle.findByCodigo("CERRADO_POR_INCIDENCIA")).thenReturn(Optional.empty());assertThrows(ConfiguracionSistemaException.class,()->servicio.resolver(7,decision("VALIDADA")));}

    void devuelta(){d.setEstado(new EstadoAlquilerDetalle("DEVUELTO"));d.setFechaHoraDevolucionReal(ahora.minusHours(1));d.setFechaHoraCierreDetalle(ahora.minusHours(1));d.setFechaHoraFinUso(ahora.minusHours(1));d.setEstacionDevolucion(e);b.setEstado(new EstadoBicicleta("MANTENIMIENTO"));b.setUbicacionActual(e);a.setEstadoContratacion(new EstadoAlquiler("FINALIZADO"));a.setFechaHoraCierre(ahora.minusHours(1));}
    void cerrada(String tipo){i.setTipo(tipo);i.setEstadoRevision("VALIDADA");i.setResueltoPor(admin);i.setFechaHoraDecision(ahora.minusMinutes(10));i.setMotivoDecision("Comprobado");i.setFechaHoraCorteValidado(ahora.minusMinutes(75));d.setEstado(new EstadoAlquilerDetalle("CERRADO_POR_INCIDENCIA"));d.setIncidenciaCorte(i);d.setFechaHoraFinUso(i.getFechaHoraCorteValidado());d.setFechaHoraCierreDetalle(ahora.minusMinutes(10));b.setEstado(new EstadoBicicleta(Set.of("PERDIDA","ROBO").contains(tipo)?"EXTRAVIADA":"MANTENIMIENTO"));}
    ReportarIncidenciaDTO reporte(){var r=new ReportarIncidenciaDTO();r.alquilerId=100;r.detalleId=10;r.versionDetalle=0;r.tipo="AVERIA";r.descripcion="Freno dañado";r.fechaHoraHechoInformada=ahora.minusMinutes(75).atOffset(ZoneOffset.UTC);return r;}
    ResolverIncidenciaDTO decision(String s){var r=new ResolverIncidenciaDTO();r.version=0;r.versionBicicleta=0;r.decision=s;r.motivo="Revisión comprobada";if(s.equals("VALIDADA"))r.fechaHoraCorteValidado=ahora.minusMinutes(75).atOffset(ZoneOffset.UTC);return r;}
    RecibirIncidenciaDTO recepcion(){var r=new RecibirIncidenciaDTO();r.version=0;r.versionBicicleta=0;r.estacionId=5;r.motivo="Recuperada en calle";r.recepcionFisicaConfirmada=true;return r;}
    PerdidaDefinitivaDTO perdida(){var r=new PerdidaDefinitivaDTO();r.version=0;r.versionBicicleta=0;r.motivo="Búsqueda finalizada, pérdida definitiva confirmada";r.perdidaDefinitivaConfirmada=true;return r;}
    Usuario usuario(int id,String rol){Usuario u=new Usuario();ReflectionTestUtils.setField(u,"id",id);u.setEmail("inc"+id+"@qa.test");u.setActivo(true);Rol r=new Rol(rol);ReflectionTestUtils.setField(r,"id",id);u.getUsuarioRoles().add(new UsuarioRol(u,r));return u;}
    void sesion(Usuario u,String rol){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getEmail(),"QA",List.of(new SimpleGrantedAuthority(rol))));}
    void codigo(int esperado,Runnable r){assertEquals(esperado,assertThrows(ResponseStatusException.class,r::run).getStatus().value());}
}
