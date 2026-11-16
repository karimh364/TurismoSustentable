package DSII.TurismoSustentable.web;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

// Verificación de la vista con HTTP controlado, sin Spring, JPA, MySQL ni usuarios reales.
@EnabledIfEnvironmentVariable(named="TURISMO_TEST_WEB",matches="true")
class MantenimientoVistaWebTest {
    static HttpServer servidor;
    static Playwright playwright;
    static Browser navegador;
    static String base;
    static final ObjectMapper JSON=new ObjectMapper();
    @BeforeAll static void iniciar()throws Exception{
        servidor=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        Set<String> archivos=Set.of("/mantenimiento.html","/js/auth.js","/js/mantenimiento.js","/css/estilos.css");
        servidor.createContext("/",peticion->{
            String ruta=peticion.getRequestURI().getPath();
            try {
                if(!archivos.contains(ruta)){peticion.sendResponseHeaders(404,-1);return;}
                try(var recurso=MantenimientoVistaWebTest.class.getResourceAsStream("/static"+ruta)){
                    if(recurso==null){peticion.sendResponseHeaders(404,-1);return;}
                    byte[] contenido=recurso.readAllBytes();
                    String tipo=ruta.endsWith(".css")?"text/css":ruta.endsWith(".js")?"application/javascript":"text/html";
                    peticion.getResponseHeaders().set("Content-Type",tipo+"; charset=UTF-8");
                    peticion.sendResponseHeaders(200,contenido.length);peticion.getResponseBody().write(contenido);
                }
            }finally{peticion.close();}
        });
        servidor.start();base="http://localhost:"+servidor.getAddress().getPort();
        playwright=Playwright.create();
        navegador=playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }
    @AfterAll static void cerrar(){
        if(navegador!=null)navegador.close();if(playwright!=null)playwright.close();if(servidor!=null)servidor.stop(0);
    }
    static class Escenario implements AutoCloseable {
        BrowserContext contexto;Page pagina;ArrayNode ordenes=JSON.createArrayNode();
        List<String> errores=new ArrayList<>();Route retenida;boolean retenerPost=false,retenerGet=false;int escrituras;
        Escenario(String rol,int ancho,int alto){
            contexto=navegador.newContext(new Browser.NewContextOptions().setViewportSize(ancho,alto)
                    .setTimezoneId("America/Argentina/Buenos_Aires"));
            ordenes.add(orden(7,"CREADA"));ordenes.add(orden(8,"EN_PROCESO"));ordenes.add(orden(9,"FINALIZADA"));
            contexto.route("**/api/**",ruta->{
                String path=URI.create(ruta.request().url()).getPath();
                if(!"GET".equals(ruta.request().method())){
                    escrituras++;
                    if(retenerPost){retenida=ruta;return;}
                    responder(ruta,"{}");return;
                }
                if(path.equals("/api/usuarios/actual")){
                    ObjectNode perfil=JSON.createObjectNode();perfil.put("id",rol.equals("ADMIN")?1:2);
                    perfil.put("nombre","María de los Ángeles Pérez de la Fuente");perfil.putArray("roles").add(rol);
                    responder(ruta,perfil.toString());
                }else if(path.equals("/api/mantenimiento/ordenes")){
                    if(retenerGet){retenida=ruta;return;}
                    responder(ruta,ordenes.toString());
                }else if(path.endsWith("/historial")){
                    ObjectNode h=JSON.createObjectNode();h.put("id",1);h.put("accion","ASIGNAR");h.put("origen","USUARIO");h.put("actorUsuarioId",1);
                    h.put("fechaHoraCambio","2026-11-16T15:00:00");h.putNull("estadoAnterior");h.putNull("estadoNuevo");
                    h.put("observaciones","Asignación de una persona responsable.");
                    h.put("valoresAnteriores",JSON.createObjectNode().putNull("tecnicoId").toString());
                    h.put("valoresNuevos",JSON.createObjectNode().put("tecnicoId",2).toString());
                    responder(ruta,JSON.createArrayNode().add(h).toString());
                }
                else if(path.equals("/api/mantenimiento/tecnicos"))responder(ruta,"[{\"id\":2,\"nombre\":\"Persona técnica asignada\"}]");
                else responder(ruta,"[]");
            });
            pagina=contexto.newPage();pagina.setDefaultTimeout(20000);pagina.onPageError(errores::add);
            pagina.navigate(base+"/mantenimiento.html");
            pagina.waitForSelector("tr[data-orden-mantenimiento='7']");
            pagina.waitForFunction("document.querySelector('#panel-ordenes').getAttribute('aria-busy') === 'false'");
        }
        public void close(){contexto.close();}
    }
    static ObjectNode orden(int id,String estado){
        ObjectNode o=JSON.createObjectNode();o.put("id",id);o.put("version",3);o.put("bicicletaId",id+10);o.put("versionBicicleta",2);
        o.put("codigoBicicleta","MANTENIMIENTO-PRUEBA-"+id);o.put("estado",""+estado);o.put("estadoBicicleta","MANTENIMIENTO");
        o.put("tipo","CORRECTIVO");o.put("tecnicoId",2);o.put("tecnicoNombre","Persona técnica con nombre compuesto");
        o.put("estacionId",1);o.put("estacionPublica",false);o.put("ubicacion","Estación/taller interno de mantenimiento y revisión");
        o.put("diagnostico","Revisión del freno y comprobación del cable.");o.put("descripcionTrabajo","Cable reemplazado y ajustes comprobados.");
        o.put("observaciones","Sin cambios en la ubicación física.");o.put("costoRepuestos",1500.50);
        o.put("fechaCreacion","2026-11-16T15:00:00");o.putNull("fechaInicio");o.putNull("fechaFin");
        if(estado.equals("FINALIZADA"))o.put("resultadoTecnico","APTA");else o.putNull("resultadoTecnico");
        return o;
    }
    static void responder(Route ruta,String contenido){ruta.fulfill(new Route.FulfillOptions().setStatus(200).setContentType("application/json").setBody(contenido));}
    static void sinDesborde(Page p){
        assertTrue((Boolean)p.evaluate("document.documentElement.scrollWidth <= window.innerWidth"),
                "La página desborda horizontalmente: "+p.evaluate("JSON.stringify({ancho:innerWidth,contenido:document.documentElement.scrollWidth})"));
    }
    void comprobarVista(String rol,int ancho,int alto)throws Exception{
        try(var e=new Escenario(rol,ancho,alto)){
            Page p=e.pagina;sinDesborde(p);
            assertTrue((Boolean)p.evaluate("document.querySelector('.maintenance-header').getBoundingClientRect().top >= document.querySelector('nav').getBoundingClientRect().bottom"),
                    "La navegación no debe tapar el encabezado, incluso con nombres largos.");
            assertEquals(rol.equals("ADMIN"),p.locator("#crear-orden").isVisible());
            assertEquals(rol.equals("ADMIN"),p.locator("#configurar-preventivo").isVisible());
            if(ancho<992){
                p.locator(".navbar-toggler").click();p.waitForSelector("#navbarNav.show");sinDesborde(p);
                assertTrue(p.locator("#navbar-username").isVisible());
                p.locator(".navbar-toggler").click();
                p.locator("#navbarNav").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
            }
            p.locator("tr[data-orden-mantenimiento='7'] summary").click();sinDesborde(p);
            assertTrue(p.locator("tr[data-orden-mantenimiento='7']").innerText().contains("12:00"),"Debe interpretar UTC y mostrar la hora local.");
            assertFalse(p.locator("#contenido-mantenimiento").innerText().contains("EN_PROCESO"));
            if(ancho<768){
                assertEquals("grid",p.locator("tr[data-orden-mantenimiento='7'] td").first().evaluate("e=>getComputedStyle(e).display"));
                assertTrue(p.locator("tr[data-orden-mantenimiento='7'] button").first().boundingBox().height>=44);
            }
            if(rol.equals("ADMIN"))assertEquals(0,p.locator("button[data-accion='iniciar']").count());
            else assertEquals(0,p.locator("button[data-accion='asignar']").count());
            assertTrue(e.errores.isEmpty(),e.errores.toString());
            Path carpeta=Path.of("build/reports/tests/mantenimiento-ux");Files.createDirectories(carpeta);
            p.evaluate("window.scrollTo(0,0)");p.waitForFunction("window.scrollY === 0");
            p.screenshot(new Page.ScreenshotOptions().setPath(carpeta.resolve("mantenimiento-"+rol.toLowerCase()+"-"+ancho+".png")).setFullPage(true));
        }
    }
    @ParameterizedTest @CsvSource({"320,700","360,800","768,1024","1366,900"})
    void vistaAdminSinDesbordesYConNavegacionCoherente(int ancho,int alto)throws Exception{comprobarVista("ADMIN",ancho,alto);}
    @ParameterizedTest @CsvSource({"320,700","360,800","768,1024","1366,900"})
    void vistaTecnicaRespetaRolesYSeAdaptaAlAncho(int ancho,int alto)throws Exception{comprobarVista("TECNICO",ancho,alto);}

    @Test void tecladoPermiteSaltarContenidoYVolverDelDialogoSinEscrituras(){
        try(var e=new Escenario("TECNICO",360,800)){
            Page p=e.pagina;p.keyboard().press("Tab");
            assertEquals("#contenido-mantenimiento",p.locator(":focus").getAttribute("href"));
            p.keyboard().press("Enter");p.keyboard().press("Tab");
            assertEquals("actualizar-ordenes",p.locator(":focus").getAttribute("id"));
            var boton=p.locator("tr[data-orden-mantenimiento='7'] button[data-accion='iniciar']");
            boton.focus();p.keyboard().press("Enter");p.waitForSelector(".swal2-popup");
            assertEquals("Iniciar trabajo",p.locator(".swal2-confirm").innerText());assertEquals("Cancelar",p.locator(".swal2-cancel").innerText());
            p.keyboard().press("Escape");p.locator(".swal2-container").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
            p.waitForFunction("document.activeElement?.getAttribute('data-accion') === 'iniciar'");
            assertEquals(0,e.escrituras);
        }
    }
    @Test void escrituraMuestraEstadoOcupadoNoRepiteYDevuelveFocoALaFila(){
        try(var e=new Escenario("TECNICO",1366,900)){
            Page p=e.pagina;e.retenerPost=true;
            p.locator("tr[data-orden-mantenimiento='7'] button[data-accion='iniciar']").click();p.click(".swal2-confirm");
            p.waitForSelector("#mensaje-mantenimiento:has-text('Guardando')");
            assertTrue(p.locator("#actualizar-ordenes").isDisabled());
            assertTrue(p.locator("#carga-mantenimiento").isVisible());
            assertEquals("true",p.locator("#panel-ordenes").getAttribute("aria-busy"));
            ((ObjectNode)e.ordenes.get(0)).put("estado","EN_PROCESO").put("version",4);
            responder(e.retenida,"{}");
            p.waitForSelector(".swal2-title:text-is('Operación registrada')");p.click(".swal2-confirm");
            p.locator(".swal2-container").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
            p.waitForFunction("document.activeElement?.getAttribute('data-orden-mantenimiento') === '7'");
            assertEquals(1,e.escrituras);assertFalse(p.locator("#actualizar-ordenes").isDisabled());
        }
    }
    @Test void formulariosEHistorialCabenEnMovilYUsanTextoLegible(){
        try(var e=new Escenario("TECNICO",320,700)){
            Page p=e.pagina;p.locator("tr[data-orden-mantenimiento='8'] button[data-accion='finalizar']").click();
            p.waitForSelector("#mant-diagnostico");sinDesborde(p);
            assertTrue((Boolean)p.locator(".swal2-popup").evaluate("e=>e.getBoundingClientRect().width <= innerWidth"));
            assertEquals("Finalizar orden",p.locator(".swal2-confirm").innerText());
            p.locator("#mant-diagnostico").fill("");p.click(".swal2-confirm");
            assertTrue(p.locator(".swal2-validation-message").isVisible());assertEquals(0,e.escrituras);
            p.click(".swal2-cancel");p.locator(".swal2-container").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
            p.locator("tr[data-orden-mantenimiento='7'] button").first().click();
            p.waitForSelector(".swal2-title:has-text('Historial')");sinDesborde(p);
            assertTrue(p.locator(".swal2-html-container").innerText().contains("sin cambio de estado"));
            assertFalse(p.locator(".swal2-html-container").innerText().contains("tecnicoId"));
            p.locator(".swal2-html-container summary").click();sinDesborde(p);
            assertTrue(p.locator(".swal2-html-container").innerText().contains("tecnicoId"));
        }
    }
}
