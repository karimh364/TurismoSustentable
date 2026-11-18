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
import java.util.function.Supplier;
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
        "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout=10"})
@ActiveProfiles("refactor") @AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class MantenimientoMysqlIntegrationTest {
    @Autowired MantenimientoService servicio;
    @Autowired UsuarioService usuarioService;
    @Autowired BicicletaService inventario;
    @Autowired OrdenMantenimientoRepository ordenes;
    @Autowired HistorialOrdenMantenimientoRepository historias;
    @Autowired PlatformTransactionManager txManager;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc http;
    @PersistenceContext EntityManager em;
    @MockBean Clock reloj;
    @SpyBean TrazabilidadInventarioService traza;
    @org.springframework.boot.web.server.LocalServerPort int puerto;
    TransactionTemplate tx;
    static AtomicInteger documentos=new AtomicInteger(20000000);
    int adminId,tecId,otroId,clienteId,estacionId;
    List<Integer> bicis;
    Map<Integer,String> emails,nicks;
    Instant instante;
    TrazabilidadInventarioService espia;
    @BeforeEach void preparar(){
        tx=new TransactionTemplate(txManager);instante=Instant.now().truncatedTo(ChronoUnit.MICROS);
        when(reloj.instant()).thenReturn(instante);when(reloj.getZone()).thenReturn(ZoneOffset.UTC);
        espia=AopTestUtils.getUltimateTargetObject(traza);
        emails=new HashMap<>();nicks=new HashMap<>();bicis=new ArrayList<>();
        tx.executeWithoutResult(t->{
            adminId=usuario("ADMIN");tecId=usuario("TECNICO");otroId=usuario("TECNICO");clienteId=usuario("CLIENTE");
            Estacion e=new Estacion("Taller QA "+adminId,"Calle QA",10,new BigDecimal("-31.4"),new BigDecimal("-64.2"));e.setEsPublica(false);em.persist(e);em.flush();estacionId=e.getId();
            for(int i=0;i<4;i++){
                Bicicleta b=new Bicicleta("MANT-SQL-"+adminId+"-"+i,catalogo(TipoBicicleta.class,"MONTANA"),catalogo(EstadoBicicleta.class,"DISPONIBLE"),e);
                b.setFechaAlta(LocalDate.ofInstant(instante,ZoneOffset.UTC));em.persist(b);em.flush();bicis.add(b.getId());
            }
        });sesion(adminId,"ADMIN");
    }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();reset(espia);}

    @Test void circuitoSqlConReasignacionTrabajoHistorialYHabilitacion(){
        var o=crear(bicis.get(0),"CORRECTIVO",tecId);
        assertEquals("CREADA",o.estado);assertEquals("MANTENIMIENTO",o.estadoBicicleta);
        sesion(tecId,"TECNICO");o=operar(o,"iniciar",comando(o));
        LocalDateTime inicio=o.fechaInicio;
        var datos=comando(o);datos.setDiagnostico("Cable gastado");datos.setDescripcionTrabajo("Cambio de cable");datos.setCostoRepuestos(new BigDecimal("1250.50"));
        o=operar(o,"trabajo",datos);
        datos=comando(o);datos.setMotivo("Esperar repuesto");o=operar(o,"pausar",datos);
        sesion(adminId,"ADMIN");datos=comando(o);datos.setTecnicoId(otroId);o=operar(o,"asignar",datos);
        assertEquals("PAUSADA",o.estado);assertEquals(inicio,o.fechaInicio);
        final var pausada=o;sesion(tecId,"TECNICO");
        assertThrows(AccessDeniedException.class,()->servicio.operar(pausada.id,"reanudar",comando(pausada)));
        sesion(otroId,"TECNICO");o=operar(o,"reanudar",comando(o));
        datos=comando(o);datos.setResultadoTecnico("APTA");o=operar(o,"finalizar",datos);
        assertEquals("MANTENIMIENTO",o.estadoBicicleta);assertEquals(new BigDecimal("1250.50"),o.costoRepuestos);assertEquals("ARS",o.monedaRepuestos);
        sesion(adminId,"ADMIN");o=operar(o,"habilitar",habilitar(o));
        assertEquals("DISPONIBLE",o.estadoBicicleta);
        var eventos=servicio.historial(o.id);
        assertEquals(List.of("CREAR","INICIAR","TRABAJO","PAUSAR","ASIGNAR","REANUDAR","FINALIZAR","HABILITAR"),
                eventos.stream().map(h->h.accion).collect(java.util.stream.Collectors.toList()));
        assertNull(eventos.get(4).estadoAnterior);assertNull(eventos.get(4).estadoNuevo);
        assertTrue(eventos.get(4).valoresAnteriores.contains("\"tecnicoId\":"+tecId));
        assertTrue(eventos.get(4).valoresNuevos.contains("\"tecnicoId\":"+otroId));
        final var habilitada=o;assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.operar(habilitada.id,"habilitar",habilitar(habilitada))).getStatus().value());
        assertEquals(8,servicio.historial(o.id).size());
    }
    @Test void preventivaAutomaticaSqlNoDuplicaYEsSistemaSinActor(){
        hacerAntigua(bicis.get(0));
        SecurityContextHolder.clearContext();
        assertTrue(servicio.aplicarPreventiva(bicis.get(0)));assertFalse(servicio.aplicarPreventiva(bicis.get(0)));
        sesion(adminId,"ADMIN");var o=servicio.listar().get(0);
        assertEquals("PREVENTIVO",o.tipo);assertNull(o.tecnicoId);
        var h=servicio.historial(o.id).get(0);assertEquals("SISTEMA",h.origen);assertNull(h.actorUsuarioId);
        var d=comando(o);d.setTecnicoId(tecId);o=operar(o,"asignar",d);o=finalizar(o,"APTA");
        sesion(adminId,"ADMIN");o=operar(o,"habilitar",habilitar(o));assertEquals("DISPONIBLE",o.estadoBicicleta);
        assertFalse(servicio.aplicarPreventiva(o.bicicletaId));
    }
    @Test void correctivoSqlNoReiniciaCalendarioYGeneraPreventivaPendiente(){
        hacerAntigua(bicis.get(0));var o=crear(bicis.get(0),"CORRECTIVO",tecId);o=finalizar(o,"APTA");
        sesion(adminId,"ADMIN");final var correctivo=o;
        assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.operar(correctivo.id,"habilitar",habilitar(correctivo))).getStatus().value());
        var preventivas=servicio.listar().stream().filter(v->v.bicicletaId.equals(correctivo.bicicletaId)&&v.tipo.equals("PREVENTIVO")).collect(java.util.stream.Collectors.toList());
        assertEquals(1,preventivas.size());var p=preventivas.get(0);assertEquals("CREADA",p.estado);
        var d=comando(p);d.setTecnicoId(tecId);p=operar(p,"asignar",d);p=finalizar(p,"APTA");
        sesion(adminId,"ADMIN");p=operar(p,"habilitar",habilitar(p));assertEquals("DISPONIBLE",p.estadoBicicleta);
    }
    @Test void bajaTecnicoSqlEsAtomicaConservaCerradasYTrabajo(){
        var creada=crear(bicis.get(0),"CORRECTIVO",tecId);
        var proceso=crear(bicis.get(1),"CORRECTIVO",tecId);sesion(tecId,"TECNICO");proceso=operar(proceso,"iniciar",comando(proceso));
        var trabajo=comando(proceso);trabajo.setDiagnostico("No borrar");trabajo.setDescripcionTrabajo("Ajustar");trabajo.setCostoRepuestos(new BigDecimal("99.50"));proceso=operar(proceso,"trabajo",trabajo);
        sesion(adminId,"ADMIN");var pausada=crear(bicis.get(2),"CORRECTIVO",tecId);
        sesion(tecId,"TECNICO");pausada=operar(pausada,"iniciar",comando(pausada));var motivo=comando(pausada);motivo.setMotivo("Esperar");pausada=operar(pausada,"pausar",motivo);
        sesion(adminId,"ADMIN");var cerrada=crear(bicis.get(3),"CORRECTIVO",tecId);cerrada=finalizar(cerrada,"APTA");
        sesion(adminId,"ADMIN");usuarioService.inactivarUsuario(tecId,"Licencia del técnico");
        var c=servicio.consultar(creada.id);var p=servicio.consultar(proceso.id);var pa=servicio.consultar(pausada.id);var fin=servicio.consultar(cerrada.id);
        assertEquals("CREADA",c.estado);assertEquals("PAUSADA",p.estado);assertEquals("PAUSADA",pa.estado);
        assertNull(c.tecnicoId);assertNull(p.tecnicoId);assertNull(pa.tecnicoId);assertEquals(tecId,fin.tecnicoId);
        assertEquals(proceso.fechaInicio,p.fechaInicio);assertEquals("No borrar",p.diagnostico);assertEquals(new BigDecimal("99.50"),p.costoRepuestos);
        assertEquals("Esperar",pa.motivoPausaCancelacion);assertNull(p.fechaFin);
        tx.executeWithoutResult(t->assertFalse(em.find(Usuario.class,tecId).isActivo()));
        var h=servicio.historial(p.id);assertEquals(adminId,h.get(h.size()-1).actorUsuarioId);assertEquals("USUARIO",h.get(h.size()-1).origen);
        sesion(tecId,"TECNICO");assertThrows(AccessDeniedException.class,()->servicio.listar());
    }
    @Test void falloSqlTrasEscriturasRevierteBajaPausaAsignacionEHistorias(){
        var o=crear(bicis.get(0),"CORRECTIVO",tecId);sesion(tecId,"TECNICO");o=operar(o,"iniciar",comando(o));
        sesion(adminId,"ADMIN");int id=o.id;long antes=contarHistorias();long auditorias=contarAuditoria();
        doAnswer(inv->{inv.callRealMethod();em.flush();throw new IllegalStateException("Fallo QA posterior a las escrituras");}).when(espia)
                .administracion(any(),eq("USUARIO"),eq(tecId),eq("INACTIVAR"),anyString(),anyString(),any(),any(LocalDateTime.class));
        assertThrows(IllegalStateException.class,()->usuarioService.inactivarUsuario(tecId,"Licencia"));
        assertEquals(antes,contarHistorias());assertEquals(auditorias,contarAuditoria());
        tx.executeWithoutResult(t->{assertTrue(em.find(Usuario.class,tecId).isActivo());assertEquals(tecId,em.find(OrdenMantenimiento.class,id).getTecnico().getId());});
        assertEquals("EN_PROCESO",servicio.consultar(id).estado);
    }
    @Test void solicitudesSqlConcurrentesNoDuplicanOrdenNiHistorias() throws Exception {
        long antes=contarHistorias();
        List<String> resultados=competir(()->{sesion(adminId,"ADMIN");try{crear(bicis.get(0),"CORRECTIVO",null);return "CREADA";}catch(ResponseStatusException e){return String.valueOf(e.getStatus().value());}},
                ()->{sesion(adminId,"ADMIN");try{crear(bicis.get(0),"CORRECTIVO",null);return "CREADA";}catch(ResponseStatusException e){return String.valueOf(e.getStatus().value());}});
        assertEquals(1,resultados.stream().filter("CREADA"::equals).count());assertTrue(resultados.contains("409"));
        sesion(adminId,"ADMIN");assertEquals(1,servicio.listar().stream().filter(o->o.bicicletaId.equals(bicis.get(0))).count());assertEquals(antes+1,contarHistorias());
    }
    @Test void inactivacionSqlContraInicioNoDejaTecnicoInactivoTrabajando() throws Exception {
        var o=crear(bicis.get(0),"CORRECTIVO",tecId);
        competir(()->{sesion(adminId,"ADMIN");usuarioService.inactivarUsuario(tecId,"Licencia");return "BAJA";},
                ()->{sesion(tecId,"TECNICO");try{servicio.operar(o.id,"iniciar",comando(o));return "INICIO";}catch(AccessDeniedException|ResponseStatusException e){return "RECHAZADO";}});
        sesion(adminId,"ADMIN");var finalizada=servicio.consultar(o.id);
        assertNull(finalizada.tecnicoId);assertTrue(Set.of("CREADA","PAUSADA").contains(finalizada.estado));
        tx.executeWithoutResult(t->assertFalse(em.find(Usuario.class,tecId).isActivo()));
    }
    @Test void httpSqlUsaRolPersistidoAsignacionYRechazaPayloadAjeno() throws Exception {
        var o=crear(bicis.get(0),"CORRECTIVO",tecId);
        var admin=user(emails.get(adminId)).authorities(new SimpleGrantedAuthority("ADMIN"));
        var tecnico=user(emails.get(tecId)).authorities(new SimpleGrantedAuthority("TECNICO"));
        var ajeno=user(emails.get(otroId)).authorities(new SimpleGrantedAuthority("TECNICO"));
        http.perform(post("/api/mantenimiento/ordenes/"+o.id+"/iniciar").with(admin).contentType("application/json").content("{\"version\":"+o.version+"}")).andExpect(status().isForbidden());
        http.perform(get("/api/mantenimiento/ordenes/"+o.id).with(ajeno)).andExpect(status().isForbidden());
        http.perform(post("/api/mantenimiento/ordenes/"+o.id+"/iniciar").with(tecnico).contentType("application/json").content("{\"version\":"+o.version+",\"estado\":\"FINALIZADA\"}")).andExpect(status().isBadRequest());
        http.perform(post("/api/mantenimiento/ordenes/"+o.id+"/iniciar").with(tecnico).contentType("application/json").content("{\"version\":"+o.version+"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("EN_PROCESO")).andExpect(jsonPath("$.ubicacion").exists()).andExpect(jsonPath("$.password").doesNotExist());
        tx.executeWithoutResult(t->em.createQuery("delete from UsuarioRol ur where ur.usuario.id=:id").setParameter("id",tecId).executeUpdate());
        http.perform(get("/api/mantenimiento/ordenes").with(tecnico)).andExpect(status().isForbidden());
    }
    @Test void revisionAnteriorSqlNoHabilitaUnaFallaPosterior(){
        var o=crear(bicis.get(0),"CORRECTIVO",tecId);o=finalizar(o,"APTA");sesion(adminId,"ADMIN");
        var nueva=crear(bicis.get(0),"CORRECTIVO",null);var d=comando(nueva);d.setMotivo("Clasificación incorrecta");operar(nueva,"cancelar",d);
        final var anterior=o;assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.operar(anterior.id,"habilitar",habilitar(anterior))).getStatus().value());
        assertEquals("MANTENIMIENTO",servicio.consultar(anterior.id).estadoBicicleta);
    }
    @Test void inventarioSqlNoReactivaPreventivaVencidaNiBorraConOrdenAbierta(){
        hacerAntigua(bicis.get(0));
        tx.executeWithoutResult(t->em.find(Bicicleta.class,bicis.get(0)).setEstado(catalogo(EstadoBicicleta.class,"INACTIVA")));
        var d=new OperacionBicicletaDTO();d.setVersion(versionBici(bicis.get(0)));d.setMotivo("Levantar pausa administrativa");
        assertEquals("MANTENIMIENTO",inventario.cambiarEstado(bicis.get(0),"DISPONIBLE",d).getEstado());
        var baja=new OperacionBicicletaDTO();baja.setVersion(versionBici(bicis.get(0)));baja.setMotivo("Retirar");
        assertThrows(ResponseStatusException.class,()->inventario.cambiarEstado(bicis.get(0),"DE_BAJA",baja));
        assertEquals(1,servicio.listar().stream().filter(o->o.bicicletaId.equals(bicis.get(0))).count());
    }
    @Test void intervaloSqlNoBorraRevisionesNiAceptaValorAnteriorFalso(){
        int tipoId=tx.execute(t->em.find(Bicicleta.class,bicis.get(0)).getTipo().getId());
        Integer anterior=tx.execute(t->em.find(TipoBicicleta.class,tipoId).getIntervaloPreventivoMeses());
        var d=new ActualizarIntervaloPreventivoDTO();d.setMesesAnteriores(anterior);d.setMeses(anterior+1);d.setMotivo("Ajuste QA");
        assertEquals(anterior+1,servicio.actualizarIntervalo(tipoId,d).getIntervaloPreventivoMeses());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->servicio.actualizarIntervalo(tipoId,d)).getStatus().value());
    }

    @Test @EnabledIfEnvironmentVariable(named="TURISMO_TEST_WEB",matches="true")
    void navegadorAdminYTecnicoCompletanOrdenSinHabilitacionAutomatica(){
        String base="http://localhost:"+puerto;
        var o=crear(bicis.get(0),"CORRECTIVO",tecId);
        try(var pw=com.microsoft.playwright.Playwright.create();var browser=pw.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions().setHeadless(true));
            var admin=browser.newContext();var tecnico=browser.newContext()){
            login(admin,base,adminId);login(tecnico,base,tecId);
            var p=tecnico.newPage();p.setDefaultTimeout(20000);List<String> errores=new ArrayList<>();p.onPageError(errores::add);
            p.navigate(base+"/mantenimiento.html");
            p.locator("button[data-accion='iniciar']").click();p.click(".swal2-confirm");p.waitForSelector(".swal2-title:text-is('Operación registrada')");p.click(".swal2-confirm");
            p.locator("button[data-accion='finalizar']").click();p.fill("#mant-diagnostico","Cable gastado");p.fill("#mant-trabajo","Cable reemplazado y probado");
            p.fill("#mant-costo","1500.50");p.selectOption("#mant-resultado","APTA");p.click(".swal2-confirm");
            p.waitForSelector(".swal2-title:text-is('Operación registrada')");p.click(".swal2-confirm");
            assertEquals(0,p.locator("button[data-accion='habilitar']").count());
            sesion(adminId,"ADMIN");assertEquals("MANTENIMIENTO",servicio.consultar(o.id).estadoBicicleta);
            var a=admin.newPage();a.setDefaultTimeout(20000);a.onPageError(errores::add);a.navigate(base+"/mantenimiento.html");
            a.locator("tr[data-orden-mantenimiento='"+o.id+"'] button[data-accion='habilitar']").click();a.fill(".swal2-textarea","Revisión conforme y ubicación verificada");a.click(".swal2-confirm");
            a.waitForSelector(".swal2-title:text-is('Operación registrada')");a.click(".swal2-confirm");
            assertEquals("DISPONIBLE",servicio.consultar(o.id).estadoBicicleta);assertEquals(new BigDecimal("1500.50"),servicio.consultar(o.id).costoRepuestos);
            assertTrue(errores.isEmpty(),errores.toString());
        }
    }
    private void login(com.microsoft.playwright.BrowserContext c,String base,int id){
        assertEquals(200,c.request().post(base+"/api/login",com.microsoft.playwright.options.RequestOptions.create()
                .setForm(com.microsoft.playwright.options.FormData.create().set("nickname",nicks.get(id)).set("password","PruebaMant?1"))).status());
    }
    private OrdenMantenimientoDTO crear(int bici,String tipo,Integer tecnico){var d=new CrearOrdenMantenimientoDTO();d.setBicicletaId(bici);d.setTipo(tipo);d.setTecnicoId(tecnico);d.setMotivo("Revisión QA");return servicio.crear(d);}
    private OperacionMantenimientoDTO comando(OrdenMantenimientoDTO o){var d=new OperacionMantenimientoDTO();d.setVersion(o.version);return d;}
    private OperacionMantenimientoDTO habilitar(OrdenMantenimientoDTO o){var d=comando(o);d.setVersionBicicleta(o.versionBicicleta);d.setMotivo("Revisión conforme");return d;}
    private OrdenMantenimientoDTO operar(OrdenMantenimientoDTO o,String accion,OperacionMantenimientoDTO d){return servicio.operar(o.id,accion,d);}
    private OrdenMantenimientoDTO finalizar(OrdenMantenimientoDTO o,String resultado){
        sesion(o.tecnicoId,"TECNICO");o=operar(o,"iniciar",comando(o));var d=comando(o);d.setDiagnostico("Comprobado");d.setDescripcionTrabajo("Revisar y probar");d.setResultadoTecnico(resultado);return operar(o,"finalizar",d);
    }
    private void hacerAntigua(int bici){tx.executeWithoutResult(t->em.createNativeQuery("update bicicleta set fecha_alta=:f where id=:id")
            .setParameter("f",LocalDate.ofInstant(instante,ZoneOffset.UTC).minusYears(10)).setParameter("id",bici).executeUpdate());}
    private int versionBici(int id){return tx.execute(t->em.find(Bicicleta.class,id).getVersion());}
    private long contarHistorias(){return tx.execute(t->em.createQuery("select count(h) from HistorialOrdenMantenimiento h",Long.class).getSingleResult());}
    private long contarAuditoria(){return tx.execute(t->em.createQuery("select count(a) from AuditoriaSistema a",Long.class).getSingleResult());}
    private int usuario(String rol){
        int dni=documentos.incrementAndGet();var p=new Persona("Persona","Prueba",LocalDate.of(1990,1,1),catalogo(Genero.class,"MASCULINO"),catalogo(TipoDocumento.class,"DNI"),String.valueOf(dni),catalogo(Pais.class,"AR"));
        em.persist(p);String nick="qa.mant."+dni;var u=new Usuario(p,nick,nick+"@example.com",encoder.encode("PruebaMant?1"));em.persist(u);em.flush();
        em.persist(new UsuarioRol(u,catalogo(Rol.class,rol)));emails.put(u.getId(),u.getEmail());nicks.put(u.getId(),nick);return u.getId();
    }
    private <T>T catalogo(Class<T> tipo,String codigo){return em.createQuery("select x from "+tipo.getSimpleName()+" x where x.codigo=:codigo",tipo).setParameter("codigo",codigo).getSingleResult();}
    private void sesion(int id,String rol){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(emails.get(id),null,List.of(new SimpleGrantedAuthority(rol))));}
    private List<String> competir(Callable<String> a,Callable<String> b)throws Exception{
        ExecutorService e=Executors.newFixedThreadPool(2);CountDownLatch inicio=new CountDownLatch(1);
        try{
            Future<String> f=e.submit(()->{try{inicio.await();return a.call();}finally{SecurityContextHolder.clearContext();}});
            Future<String> g=e.submit(()->{try{inicio.await();return b.call();}finally{SecurityContextHolder.clearContext();}});
            inicio.countDown();return List.of(f.get(25,TimeUnit.SECONDS),g.get(25,TimeUnit.SECONDS));
        }finally{e.shutdownNow();}
    }
}
