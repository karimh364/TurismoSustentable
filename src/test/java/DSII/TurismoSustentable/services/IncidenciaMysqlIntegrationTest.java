package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import javax.persistence.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@EnabledIfEnvironmentVariable(named="TURISMO_TEST_MYSQL",matches="true")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "spring.datasource.url=jdbc:mariadb://localhost:3306/turismo_sustentable?sslMode=disable",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.jpa.open-in-view=false",
        "mantenimiento.preventivo.automatico=false","alquiler.vencimientos.automaticos=false",
        "pagos.simulados.saldo-inicial-ars=1000000.00",
        "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout=10"})
@ActiveProfiles("refactor") @AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class IncidenciaMysqlIntegrationTest {
    @Autowired IncidenciaService servicio;@Autowired AlquilerService alquiler;@Autowired PagoService pagos;
    @Autowired LiquidacionAlquilerService liquidacion;@Autowired MantenimientoService mantenimiento;
    @Autowired IncidenciaRepository incidencias;@Autowired HistorialIncidenciaRepository historias;
    @Autowired UsuarioRepository usuarios;
    @Autowired PlatformTransactionManager txManager;@Autowired PasswordEncoder encoder;@Autowired MockMvc http;
    @PersistenceContext EntityManager em;@MockBean Clock reloj;@MockBean CotizacionDolarService cotizacion;
    @SpyBean TrazabilidadInventarioService traza;
    @org.springframework.boot.web.server.LocalServerPort int puerto;
    static AtomicInteger documentos=new AtomicInteger(30000000);
    TransactionTemplate tx;Instant instante;TrazabilidadInventarioService espia;
    int clienteId,adminId,otroId,tecId,publicaId,tallerId,bici1,bici2;
    Map<Integer,String> emails,nicks,roles;
    @BeforeEach void preparar(){
        tx=new TransactionTemplate(txManager);instante=Instant.now().truncatedTo(ChronoUnit.MICROS);
        when(reloj.instant()).thenAnswer(inv->instante);when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
        when(cotizacion.obtenerCotizacion()).thenAnswer(inv->new CotizacionDolarDTO(new BigDecimal("1530.00"),"QA_INCIDENCIAS",instante.atOffset(ZoneOffset.UTC)));
        espia=AopTestUtils.getUltimateTargetObject(traza);emails=new HashMap<>();nicks=new HashMap<>();roles=new HashMap<>();
        tx.executeWithoutResult(t->{
            catalogo(TipoBicicleta.class,"MONTANA").setPrecioPorHoraUsd(new BigDecimal("10.00"));
            clienteId=usuario("CLIENTE");adminId=usuario("ADMIN");otroId=usuario("CLIENTE");tecId=usuario("TECNICO");
            Estacion p=new Estacion("Plaza Inc "+clienteId,"Calle QA",4,-31.4,-64.2);p.setEsPublica(true);em.persist(p);
            Estacion e=new Estacion("Taller Inc "+clienteId,"Base QA",4,-31.4,-64.2);e.setEsPublica(false);em.persist(e);em.flush();publicaId=p.getId();tallerId=e.getId();
            Bicicleta b=nuevaBici(p,"A");bici1=b.getId();b=nuevaBici(p,"B");bici2=b.getId();
        });sesion(clienteId);
    }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();reset(espia);}

    @Test void bloqueoDeCuentaNoPrecargaCabecerasAntesDeAdquirirSuBloqueo(){
        retirada(false);
        tx.executeWithoutResult(t->{var u=usuarios.buscarActivoPorEmailConBloqueo(emails.get(clienteId)).orElseThrow();
            assertFalse(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(u,"alquileres"),"La consulta de cuenta no debe cargar versiones de alquiler antes de bloquear la cabecera.");});
    }
    @Test void reporteSqlConservaRelacionFechasHistoriaYReintentos(){
        var a=retirada(false);var dto=reporte(a,0,"AVERIA");long anteriores=contar("HistorialIncidencia");
        var i=servicio.reportar(dto);var repetida=servicio.reportar(dto);assertEquals(i.id,repetida.id);assertEquals(anteriores+1,contar("HistorialIncidencia"));
        tx.executeWithoutResult(t->{var real=em.find(Incidencia.class,i.id);assertEquals(a.getDetalles().get(0).getId(),real.getAlquilerDetalle().getId());assertEquals(clienteId,real.getRegistradoPor().getId());assertEquals("PENDIENTE",real.getEstadoRevision());assertEquals("EN_USO",real.getAlquilerDetalle().getBicicleta().getEstado().getCodigo());assertNull(real.getFechaHoraCorteValidado());});
        assertEquals("SIN_LIQUIDAR",liquidacion.consultarLiquidacion(a.getId()).getSituacionFinanciera());
        sesion(otroId);assertThrows(AccessDeniedException.class,()->servicio.consultar(i.id));assertThrows(AccessDeniedException.class,()->servicio.historial(i.id));
        sesion(tecId);assertThrows(AccessDeniedException.class,()->servicio.listar());
    }
    @Test void devuelveAntesDeRevisarYSoloLiquidaCorteConSnapshots(){
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"AVERIA"));avanzar(15);
        alquiler.devolverBicicleta(a.getId(),i.detalleId,devolver());
        var recibida=servicio.consultar(i.id);assertNotNull(recibida.fechaHoraRecepcionFisica);assertEquals(publicaId,recibida.estacionRecepcionId);assertNotNull(recibida.ordenMantenimientoId);
        assertEquals("SIN_LIQUIDAR",liquidacion.consultarLiquidacion(a.getId()).getSituacionFinanciera());
        tx.executeWithoutResult(t->{AlquilerDetalle d=em.find(AlquilerDetalle.class,i.detalleId);assertEquals("DEVUELTO",d.getEstado().getCodigo());assertNull(d.getSubtotalRealUsd());em.find(Bicicleta.class,bici1).getTipo().setPrecioPorHoraUsd(new BigDecimal("90.00"));});
        avanzar(60);sesion(adminId);var resuelta=servicio.resolver(i.id,decision(recibida,"VALIDADA"));
        var resumen=liquidacion.consultarLiquidacion(a.getId());assertEquals(new BigDecimal("7.50"),resumen.getCostoFinalUsd());assertEquals(new BigDecimal("11475.00"),resumen.getCostoFinalArs());assertEquals("REEMBOLSO_PENDIENTE",resumen.getSituacionFinanciera());
        tx.executeWithoutResult(t->{var d=em.find(AlquilerDetalle.class,i.detalleId);assertEquals("DEVUELTO",d.getEstado().getCodigo());assertEquals(publicaId,d.getEstacionDevolucion().getId());assertTrue(d.getFechaHoraFinUso().isBefore(d.getFechaHoraDevolucionReal()));assertEquals(i.id,d.getIncidenciaCorte().getId());});
        assertEquals(recibida.fechaHoraRecepcionFisica,resuelta.fechaHoraRecepcionFisica);long eventos=historias.count();servicio.resolver(i.id,decision(resuelta,"VALIDADA"));assertEquals(eventos,historias.count());
    }
    @Test void descartarTrasDevolverUsaHoraFisicaNoLaRevision(){
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"AVERIA"));avanzar(15);alquiler.devolverBicicleta(a.getId(),i.detalleId,devolver());avanzar(90);
        sesion(adminId);servicio.resolver(i.id,decision(servicio.consultar(i.id),"DESCARTADA"));
        var resumen=liquidacion.consultarLiquidacion(a.getId());assertEquals(new BigDecimal("10.00"),resumen.getCostoFinalUsd());assertEquals("SALDADO",resumen.getSituacionFinanciera());
        tx.executeWithoutResult(t->{var d=em.find(AlquilerDetalle.class,i.detalleId);assertNull(d.getIncidenciaCorte());assertEquals(d.getFechaHoraDevolucionReal(),d.getFechaHoraFinUso());assertEquals("MANTENIMIENTO",d.getBicicleta().getEstado().getCodigo());});
    }
    @Test void variasIncidenciasEsperanTodasLasDecisionesSinCambiarElFundamento(){
        var a=retirada(false);var primero=reporte(a,0,"AVERIA");var i=servicio.reportar(primero);var segundo=reporte(a,0,"ACCIDENTE");segundo.descripcion="Segundo reporte del mismo uso";var j=servicio.reportar(segundo);
        sesion(adminId);servicio.resolver(i.id,decision(i,"VALIDADA"));assertEquals("SIN_LIQUIDAR",liquidacion.consultarLiquidacion(a.getId()).getSituacionFinanciera());
        servicio.resolver(j.id,decision(servicio.consultar(j.id),"DESCARTADA"));assertNotEquals("SIN_LIQUIDAR",liquidacion.consultarLiquidacion(a.getId()).getSituacionFinanciera());
        tx.executeWithoutResult(t->assertEquals(i.id,em.find(AlquilerDetalle.class,i.detalleId).getIncidenciaCorte().getId()));
    }
    @Test void perdidaNoCierraOtrasUnidadesYRecuperacionNoCobraOtraVez(){
        var a=retirada(true);var i=servicio.reportar(reporte(a,0,"ROBO"));sesion(adminId);i=servicio.resolver(i.id,decision(i,"VALIDADA"));assertEquals("EXTRAVIADA",i.estadoBicicleta);
        final var cerrada=i;tx.executeWithoutResult(t->{assertEquals("EN_PROGRESO",em.find(Alquiler.class,a.getId()).getEstadoContratacion().getCodigo());assertNull(em.find(Bicicleta.class,bici1).getUbicacionActual());assertNull(em.find(AlquilerDetalle.class,cerrada.detalleId).getFechaHoraDevolucionReal());});
        sesion(clienteId);avanzar(15);alquiler.devolverBicicleta(a.getId(),a.getDetalles().get(1).getId(),devolver());var fin=liquidacion.consultarLiquidacion(a.getId());long pagos=contar("Pago"),reintegros=contar("Reembolso");
        sesion(adminId);avanzar(40);var recuperada=servicio.recibir(i.id,recepcion(servicio.consultar(i.id),tallerId));assertEquals("MANTENIMIENTO",recuperada.estadoBicicleta);assertEquals(tallerId,recuperada.estacionRecepcionId);assertNotNull(recuperada.ordenMantenimientoId);
        assertEquals(fin.getCostoFinalArs(),liquidacion.consultarLiquidacion(a.getId()).getCostoFinalArs());assertEquals(pagos,contar("Pago"));assertEquals(reintegros,contar("Reembolso"));
    }
    @Test void averiaFueraDeEstacionExigeRecepcionAntesDeTrabajoTecnico(){
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"ACCIDENTE"));sesion(adminId);i=servicio.resolver(i.id,decision(i,"VALIDADA"));assertNull(i.fechaHoraRecepcionFisica);
        var o=mantenimiento.consultar(i.ordenMantenimientoId);var comando=new OperacionMantenimientoDTO();comando.setVersion(o.version);comando.setTecnicoId(tecId);o=mantenimiento.operar(o.id,"asignar",comando);
        final var asignada=o;sesion(tecId);assertEquals(409,assertThrows(ResponseStatusException.class,()->mantenimiento.operar(asignada.id,"iniciar",comando(asignada))).getStatus().value());
        sesion(adminId);var recibir=recepcion(servicio.consultar(i.id),null);recibir.lugarRecepcion="Base móvil de asistencia";i=servicio.recibir(i.id,recibir);assertNull(i.estacionRecepcionId);
        sesion(tecId);assertEquals("EN_PROCESO",mantenimiento.operar(o.id,"iniciar",comando(o)).estado);
    }
    @Test void recepcionExternaLuegoIngresoYRevisionPermitenHabilitacionSinReescribirRecepcion(){
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"AVERIA"));sesion(adminId);i=servicio.resolver(i.id,decision(i,"VALIDADA"));
        var r=recepcion(i,null);r.lugarRecepcion="Base móvil";i=servicio.recibir(i.id,r);LocalDateTime original=i.fechaHoraRecepcionFisica;
        var o=mantenimiento.consultar(i.ordenMantenimientoId);var asignar=comando(o);asignar.setTecnicoId(tecId);o=mantenimiento.operar(o.id,"asignar",asignar);
        sesion(tecId);o=mantenimiento.operar(o.id,"iniciar",comando(o));var trabajo=comando(o);trabajo.setDiagnostico("Cable dañado");trabajo.setDescripcionTrabajo("Cable reemplazado y probado");trabajo.setResultadoTecnico("APTA");o=mantenimiento.operar(o.id,"finalizar",trabajo);
        sesion(adminId);avanzar(10);i=servicio.ubicar(i.id,recepcion(servicio.consultar(i.id),tallerId));assertEquals(original,i.fechaHoraRecepcionFisica);assertNull(i.estacionRecepcionId);assertEquals("Base móvil",i.lugarRecepcion);assertEquals(tallerId,i.estacionActualId);
        var habilitar=comando(o);habilitar.setVersionBicicleta(i.versionBicicleta);habilitar.setMotivo("Revisión apta y presencia física verificada");assertEquals("DISPONIBLE",mantenimiento.operar(o.id,"habilitar",habilitar).estadoBicicleta);
        assertEquals(new BigDecimal("11475.00"),liquidacion.consultarLiquidacion(a.getId()).getCostoFinalArs());
    }
    @Test void falloSqlDeRecepcionRevierteUbicacionOrdenEHistoria(){
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"ROBO"));sesion(adminId);i=servicio.resolver(i.id,decision(i,"VALIDADA"));final var perdida=i;long h=historias.count(),ordenesAntes=contar("OrdenMantenimiento");
        doAnswer(inv->{inv.callRealMethod();em.flush();throw new IllegalStateException("QA tras recepción real");}).when(espia).administracion(any(),eq("BICICLETA"),eq(bici1),eq("RECUPERAR_INCIDENCIA"),anyString(),anyString(),anyString(),any(LocalDateTime.class));
        assertThrows(IllegalStateException.class,()->servicio.recibir(perdida.id,recepcion(perdida,tallerId)));assertEquals(h,historias.count());assertEquals(ordenesAntes,contar("OrdenMantenimiento"));
        var actual=servicio.consultar(i.id);assertNull(actual.fechaHoraRecepcionFisica);assertNull(actual.estacionActualId);assertEquals("EXTRAVIADA",actual.estadoBicicleta);
    }
    @Test void bajaPorPerdidaYEliminacionNoInventanCuposNiRecepcion(){
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"PERDIDA"));sesion(adminId);i=servicio.resolver(i.id,decision(i,"VALIDADA"));i=servicio.perdidaDefinitiva(i.id,perdida(i),false);assertEquals("DE_BAJA",i.estadoBicicleta);assertFalse(i.borradoBici);assertTrue(i.perdidaDefinitiva);
        i=servicio.perdidaDefinitiva(i.id,perdida(i),true);assertTrue(i.borradoBici);assertNull(i.fechaHoraRecepcionFisica);final var eliminada=i;
        tx.executeWithoutResult(t->{assertNull(em.find(Bicicleta.class,bici1).getUbicacionActual());assertEquals(0L,em.createQuery("select count(b) from Bicicleta b where b.id=:id and b.ubicacionActual is not null",Long.class).setParameter("id",bici1).getSingleResult());});
        long h=historias.count();servicio.perdidaDefinitiva(i.id,perdida(i),true);assertEquals(h,historias.count());assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.recibir(eliminada.id,recepcion(eliminada,tallerId))).getStatus().value());
        sesion(clienteId);assertTrue(servicio.consultar(i.id).borradoBici);assertTrue(servicio.historial(i.id).stream().anyMatch(v->v.accion.equals("ELIMINAR_PERDIDA")));
    }
    @Test void falloTrasEscriturasSqlRevierteDecisionCierreBicicletaHistoriasYDinero(){
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"AVERIA"));sesion(adminId);long eventos=historias.count(),auditorias=contar("AuditoriaSistema"),pagos=contar("Pago");
        doAnswer(inv->{inv.callRealMethod();em.flush();throw new IllegalStateException("QA después de escrituras reales");}).when(espia).administracion(any(),eq("INCIDENCIA"),eq(i.id),eq("RESOLVER"),anyString(),anyString(),anyString(),any(LocalDateTime.class));
        assertThrows(IllegalStateException.class,()->servicio.resolver(i.id,decision(i,"VALIDADA")));assertEquals(eventos,historias.count());assertEquals(auditorias,contar("AuditoriaSistema"));assertEquals(pagos,contar("Pago"));
        tx.executeWithoutResult(t->{assertEquals("PENDIENTE",em.find(Incidencia.class,i.id).getEstadoRevision());assertEquals("EN_CURSO",em.find(AlquilerDetalle.class,i.detalleId).getEstado().getCodigo());assertEquals("EN_USO",em.find(Bicicleta.class,bici1).getEstado().getCodigo());assertNull(em.find(Alquiler.class,a.getId()).getFechaHoraLiquidacion());});
    }
    @Test void falloDelReporteSqlNoDejaIncidenciaSinHistoria(){
        var a=retirada(false);sesion(adminId);var dto=reporte(a,0,"AVERIA");dto.informanteId=clienteId;long antes=incidencias.count(),eventos=historias.count();
        doAnswer(inv->{inv.callRealMethod();em.flush();throw new IllegalStateException("QA tras creación");}).when(espia).administracion(any(),eq("INCIDENCIA"),anyInt(),eq("REPORTAR"),isNull(),anyString(),anyString(),any(LocalDateTime.class));
        assertThrows(IllegalStateException.class,()->servicio.reportar(dto));assertEquals(antes,incidencias.count());assertEquals(eventos,historias.count());
    }
    @Test void decisionesSqlSimultaneasNoDuplicanCorteNiLiquidacion()throws Exception{
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"AVERIA"));var dto=decision(i,"VALIDADA");long antes=historias.count();
        var resultados=competir(()->{sesion(adminId);return servicio.resolver(i.id,dto).estadoRevision;},()->{sesion(adminId);return servicio.resolver(i.id,dto).estadoRevision;});
        assertEquals(List.of("VALIDADA","VALIDADA"),resultados);assertEquals(antes+1,historias.count());sesion(adminId);var fin=liquidacion.consultarLiquidacion(a.getId());assertEquals(new BigDecimal("11475.00"),fin.getCostoFinalArs());
    }
    @Test void reportesSqlIdenticosConcurrentesConservanUno()throws Exception{
        var a=retirada(false);var dto=reporte(a,0,"AVERIA");long antes=incidencias.count();
        var r=competir(()->{sesion(clienteId);return servicio.reportar(dto).id.toString();},()->{sesion(clienteId);return servicio.reportar(dto).id.toString();});assertEquals(r.get(0),r.get(1));assertEquals(antes+1,incidencias.count());
    }
    @Test void carreraDevolucionRevisionConservaUnCierreYRecepcionReal()throws Exception{
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"AVERIA"));var r=competir(()->{sesion(clienteId);try{alquiler.devolverBicicleta(a.getId(),i.detalleId,devolver());return "DEVUELTO";}catch(IllegalStateException ex){return "CERRADO";}},
            ()->{sesion(adminId);try{return servicio.resolver(i.id,decision(i,"VALIDADA")).estadoRevision;}catch(ResponseStatusException ex){assertEquals(409,ex.getStatus().value());return "RECARGAR";}});
        sesion(adminId);var actual=servicio.consultar(i.id);if(actual.estadoRevision.equals("PENDIENTE"))actual=servicio.resolver(i.id,decision(actual,"VALIDADA"));
        assertEquals("VALIDADA",actual.estadoRevision);assertEquals(r.contains("DEVUELTO")?"DEVUELTO":"CERRADO_POR_INCIDENCIA",actual.estadoDetalle);
        assertEquals(new BigDecimal("11475.00"),liquidacion.consultarLiquidacion(a.getId()).getCostoFinalArs());
    }
    @Test void recepcionesSqlConcurrentesNoSuperanCapacidad()throws Exception{
        var a=retirada(true);var i=servicio.reportar(reporte(a,0,"ROBO"));var j=servicio.reportar(reporte(a,1,"PERDIDA"));sesion(adminId);
        i=servicio.resolver(i.id,decision(i,"VALIDADA"));j=servicio.resolver(j.id,decision(servicio.consultar(j.id),"VALIDADA"));
        tx.executeWithoutResult(t->em.find(Estacion.class,tallerId).setCapacidad(1));final var uno=i;final var dos=j;
        var r=competir(()->recibirEnHilo(uno),()->recibirEnHilo(dos));assertEquals(1,r.stream().filter(v->v.equals("RECIBIDA")).count());assertEquals(1,r.stream().filter(v->v.equals("LLENA")).count());
        tx.executeWithoutResult(t->assertEquals(1L,em.createQuery("select count(b) from Bicicleta b where b.ubicacionActual.id=:id",Long.class).setParameter("id",tallerId).getSingleResult()));
    }
    @Test void httpSqlNoExponeReporteAjenoYRevalidaRolPersistido()throws Exception{
        var a=retirada(false);var i=servicio.reportar(reporte(a,0,"AVERIA"));
        http.perform(get("/api/incidencias/"+i.id).with(user(emails.get(otroId)).authorities(new SimpleGrantedAuthority("CLIENTE")))).andExpect(status().isForbidden());
        tx.executeWithoutResult(t->em.createQuery("delete from UsuarioRol ur where ur.usuario.id=:id").setParameter("id",adminId).executeUpdate());
        http.perform(get("/api/incidencias").with(user(emails.get(adminId)).authorities(new SimpleGrantedAuthority("ADMIN")))).andExpect(status().isForbidden());
    }

    @Test @EnabledIfEnvironmentVariable(named="TURISMO_TEST_WEB",matches="true")
    void navegadorClienteYAdminCompletanReporteCorteYRecuperacion()throws Exception{
        var a=retirada(false);int detalle=a.getDetalles().get(0).getId();String base="http://localhost:"+puerto;List<String> errores=new ArrayList<>();
        try(var pw=com.microsoft.playwright.Playwright.create();var browser=pw.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
            var cli=browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions().setViewportSize(360,800).setTimezoneId("America/Argentina/Buenos_Aires"));
            var adm=browser.newContext(new com.microsoft.playwright.Browser.NewContextOptions().setViewportSize(1366,900).setTimezoneId("America/Argentina/Buenos_Aires"))){
            login(cli,base,clienteId);login(adm,base,adminId);var p=cli.newPage();p.setDefaultTimeout(20000);p.onPageError(errores::add);
            p.navigate(base+"/incidencias.html?alquilerId="+a.getId()+"&detalleId="+detalle);p.waitForSelector("#inc-descripcion");p.fill("#inc-descripcion","Robo informado desde la pantalla");p.selectOption("#inc-tipo","ROBO");p.fill("#inc-hecho",horaLocal(instante));p.fill("#inc-lugar","Última ubicación conocida");p.click(".swal2-confirm");p.waitForSelector(".swal2-title:text-is('Operación registrada')");p.click(".swal2-confirm");p.waitForSelector("article[data-incidencia]");
            assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth <= innerWidth"));assertEquals(0,p.locator("button:has-text('Revisar solicitud')").count());
            sesion(adminId);var inc=servicio.listar().stream().filter(v->v.detalleId==detalle).findFirst().orElseThrow();
            var q=adm.newPage();q.setDefaultTimeout(20000);q.onPageError(errores::add);q.navigate(base+"/incidencias.html?bicicletaId="+bici1);q.locator("article[data-incidencia='"+inc.id+"'] button:has-text('Revisar solicitud')").click();q.selectOption("#inc-decision","VALIDADA");q.fill("#inc-motivo","Fecha y hecho revisados por administración");q.click(".swal2-confirm");q.waitForSelector(".swal2-title:text-is('Operación registrada')");q.click(".swal2-confirm");
            q.locator("article[data-incidencia='"+inc.id+"'] button:has-text('Recibir / recuperar')").click();q.selectOption("#inc-estacion",String.valueOf(tallerId));q.fill("#inc-motivo","Bicicleta recuperada físicamente");q.check("#inc-confirmar");q.click(".swal2-confirm");q.waitForSelector(".swal2-title:text-is('Operación registrada')");q.click(".swal2-confirm");
            assertEquals("MANTENIMIENTO",servicio.consultar(inc.id).estadoBicicleta);assertNotNull(servicio.consultar(inc.id).fechaHoraRecepcionFisica);
            p.locator("#inc-actualizar").click();p.waitForSelector("article:has-text('Cerrado por incidencia')");
            p.locator("article[data-incidencia='"+inc.id+"'] button:has-text('Consultar liquidación')").click();
            p.waitForSelector(".swal2-title:has-text('Liquidación del alquiler')");assertTrue(p.locator(".swal2-html-container").innerText().contains("REEMBOLSO PENDIENTE"));p.click(".swal2-confirm");
            p.waitForFunction("!document.querySelector('#inc-actualizar').disabled");assertTrue(errores.isEmpty(),errores.toString());
            java.nio.file.Path carpeta=java.nio.file.Path.of("build/reports/tests/incidencias-web");java.nio.file.Files.createDirectories(carpeta);
            // Capturar una entrada normal a la página, no el scroll retenido por un diálogo en cierre.
            p.navigate(base+"/incidencias.html");q.navigate(base+"/incidencias.html?bicicletaId="+bici1);
            p.waitForSelector("article[data-incidencia='"+inc.id+"']");q.waitForSelector("article[data-incidencia='"+inc.id+"']");
            assertEquals(0,p.locator("nav").boundingBox().y,1);assertEquals(0,q.locator("nav").boundingBox().y,1);
            p.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setPath(carpeta.resolve("cliente-360.png")).setFullPage(true));
            q.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions().setPath(carpeta.resolve("admin-1366.png")).setFullPage(true));
        }
    }

    private String recibirEnHilo(IncidenciaDTO i){sesion(adminId);try{servicio.recibir(i.id,recepcion(i,tallerId));return "RECIBIDA";}catch(ResponseStatusException ex){assertEquals(409,ex.getStatus().value());return "LLENA";}}
    private AlquilerCabeceraDTO retirada(boolean dos){sesion(clienteId);var s=new CrearAlquilerDTO();s.setEstacionRetiroId(publicaId);s.setBicicletaIds(dos?List.of(bici1,bici2):List.of(bici1));s.setDuracionEstimadaSegundos(3600);int id=alquiler.crearReserva(s);String clave=UUID.randomUUID().toString();pagos.iniciarAnticipo(id,clave);assertEquals("APROBADO",pagos.resolverAnticipo(id,clave).getEstadoPago());
        var a=alquiler.obtenerAlquiler(id);instante=instante.plusSeconds(1);for(var d:a.getDetalles())alquiler.retirarBicicleta(id,d.getId());avanzar(45);return alquiler.obtenerAlquiler(id);}
    private ReportarIncidenciaDTO reporte(AlquilerCabeceraDTO a,int pos,String tipo){var d=a.getDetalles().get(pos);var r=new ReportarIncidenciaDTO();r.alquilerId=a.getId();r.detalleId=d.getId();r.versionDetalle=d.getVersion();r.tipo=tipo;r.descripcion="Interrupción real QA";r.fechaHoraHechoInformada=instante.atOffset(ZoneOffset.UTC);return r;}
    private ResolverIncidenciaDTO decision(IncidenciaDTO i,String decision){var r=new ResolverIncidenciaDTO();r.version=i.version;r.versionBicicleta=i.versionBicicleta;r.decision=decision;r.motivo="Hechos y cronología comprobados";if(decision.equals("VALIDADA"))r.fechaHoraCorteValidado=i.fechaHoraHechoInformada.atOffset(ZoneOffset.UTC);return r;}
    private RecibirIncidenciaDTO recepcion(IncidenciaDTO i,Integer estacion){var r=new RecibirIncidenciaDTO();r.version=i.version;r.versionBicicleta=i.versionBicicleta;r.estacionId=estacion;r.motivo="Recuperación real QA";r.recepcionFisicaConfirmada=true;return r;}
    private PerdidaDefinitivaDTO perdida(IncidenciaDTO i){var r=new PerdidaDefinitivaDTO();r.version=i.version;r.versionBicicleta=i.versionBicicleta;r.motivo="Pérdida definitiva comprobada";r.perdidaDefinitivaConfirmada=true;return r;}
    private DevolverBicicletaDTO devolver(){var d=new DevolverBicicletaDTO();d.setEstacionDevolucionId(publicaId);d.setFallaReportada(false);return d;}
    private OperacionMantenimientoDTO comando(OrdenMantenimientoDTO o){var d=new OperacionMantenimientoDTO();d.setVersion(o.version);return d;}
    private void avanzar(int minutos){instante=instante.plusSeconds(minutos*60L);}
    private long contar(String entidad){return tx.execute(t->em.createQuery("select count(x) from "+entidad+" x",Long.class).getSingleResult());}
    private Bicicleta nuevaBici(Estacion e,String sufijo){var b=new Bicicleta("INC-SQL-"+clienteId+"-"+sufijo,catalogo(TipoBicicleta.class,"MONTANA"),catalogo(EstadoBicicleta.class,"DISPONIBLE"),e);b.setFechaAlta(LocalDate.ofInstant(instante,ZoneOffset.UTC));em.persist(b);em.flush();return b;}
    private int usuario(String rol){int dni=documentos.incrementAndGet();var p=new Persona("Persona","Prueba",LocalDate.of(1990,1,1),catalogo(Genero.class,"MASCULINO"),catalogo(TipoDocumento.class,"DNI"),String.valueOf(dni),catalogo(Pais.class,"AR"));em.persist(p);String nick="qa.inc."+dni;var u=new Usuario(p,nick,nick+"@example.com",encoder.encode("PruebaInc?1"));em.persist(u);em.flush();em.persist(new UsuarioRol(u,catalogo(Rol.class,rol)));emails.put(u.getId(),u.getEmail());nicks.put(u.getId(),nick);roles.put(u.getId(),rol);return u.getId();}
    private <T>T catalogo(Class<T> tipo,String codigo){return em.createQuery("select x from "+tipo.getSimpleName()+" x where x.codigo=:codigo",tipo).setParameter("codigo",codigo).getSingleResult();}
    private void sesion(int id){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(emails.get(id),null,List.of(new SimpleGrantedAuthority(roles.get(id)))));}
    private void login(com.microsoft.playwright.BrowserContext c,String base,int id){assertEquals(200,c.request().post(base+"/api/login",com.microsoft.playwright.options.RequestOptions.create().setForm(com.microsoft.playwright.options.FormData.create().set("nickname",nicks.get(id)).set("password","PruebaInc?1"))).status());}
    private String horaLocal(Instant i){return LocalDateTime.ofInstant(i,ZoneId.of("America/Argentina/Buenos_Aires")).truncatedTo(ChronoUnit.SECONDS).toString();}
    private List<String> competir(Callable<String> a,Callable<String> b)throws Exception{ExecutorService e=Executors.newFixedThreadPool(2);CountDownLatch salida=new CountDownLatch(1);try{var f=e.submit(()->{try{salida.await();return a.call();}finally{SecurityContextHolder.clearContext();}});var g=e.submit(()->{try{salida.await();return b.call();}finally{SecurityContextHolder.clearContext();}});salida.countDown();return List.of(f.get(25,TimeUnit.SECONDS),g.get(25,TimeUnit.SECONDS));}finally{e.shutdownNow();}}
}
