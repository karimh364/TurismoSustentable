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
class MantenimientoServiceTest {
    @Mock OrdenMantenimientoRepository ordenes;
    @Mock HistorialOrdenMantenimientoRepository historias;
    @Mock BicicletaRepository bicis;
    @Mock UsuarioRepository usuarios;
    @Mock TipoMantenimientoRepository tipos;
    @Mock EstadoOrdenMantenimientoRepository estados;
    @Mock EstadoBicicletaRepository estadosBici;
    @Mock EstacionRepository estaciones;
    @Mock TipoBicicletaRepository tiposBici;
    @Mock AlquilerDetalleRepository detalles;
    @Mock IncidenciaRepository incidencias;
    @Mock TrazabilidadInventarioService traza;
    static ValidatorFactory factory;
    MantenimientoService servicio;
    final LocalDateTime ahora=LocalDateTime.of(2026,11,16,15,0);
    Usuario admin,tec,otro,cliente;
    Bicicleta bici;
    OrdenMantenimiento orden;
    Estacion estacion;
    List<OrdenMantenimiento> lista;
    Map<Integer,Usuario> cuentas;
    @BeforeAll static void validar(){factory=Validation.buildDefaultValidatorFactory();}
    @AfterAll static void cerrar(){factory.close();}
    @BeforeEach void preparar(){
        servicio = new MantenimientoService(
                ordenes,
                historias,
                bicis,
                usuarios,
                tipos,
                estados,
                estadosBici,
                estaciones,
                tiposBici,
                detalles,
                incidencias,
                traza,
                Clock.fixed(
                        ahora.toInstant(ZoneOffset.UTC),
                        ZoneOffset.UTC
                ),
                factory.getValidator(),
                new ObjectMapper()
        );
        admin=usuario(1,"ADMIN"); tec=usuario(2,"TECNICO"); otro=usuario(3,"TECNICO"); cliente=usuario(4,"CLIENTE");
        cuentas=Map.of(1,admin,2,tec,3,otro,4,cliente);
        estacion=new Estacion("Taller","Calle 1",10,new BigDecimal("-31.4"),new BigDecimal("-64.2")); ReflectionTestUtils.setField(estacion,"id",5); estacion.setEsPublica(false);
        TipoBicicleta tipo=new TipoBicicleta("PASEO",new BigDecimal("8.00"),3);
        bici=new Bicicleta("MANT-01",tipo,new EstadoBicicleta("MANTENIMIENTO"),estacion);
        ReflectionTestUtils.setField(bici,"id",20); ReflectionTestUtils.setField(bici,"version",0); bici.setFechaAlta(ahora.toLocalDate().minusMonths(1));
        orden=new OrdenMantenimiento(bici,new TipoMantenimiento("CORRECTIVO","Correctivo"),new EstadoOrdenMantenimiento("CREADA"),ahora.minusDays(1));
        ReflectionTestUtils.setField(orden,"id",10); ReflectionTestUtils.setField(orden,"version",0); orden.setTecnico(tec); lista=new ArrayList<>(List.of(orden));
        lenient().when(usuarios.buscarIdPorEmail(anyString())).thenAnswer(i->cuentas.values().stream().filter(u->u.getEmail().equals(i.getArgument(0))).map(Usuario::getId).findFirst());
        lenient().when(usuarios.buscarPorIdConBloqueo(anyInt())).thenAnswer(i->Optional.ofNullable(cuentas.get(i.getArgument(0))));
        lenient().when(usuarios.buscarPorEmailConDatosYRoles(anyString())).thenAnswer(i->cuentas.values().stream().filter(u->u.getEmail().equals(i.getArgument(0))).findFirst());
        lenient().when(bicis.buscarActivaPorIdConBloqueo(20)).thenReturn(Optional.of(bici));
        lenient().when(ordenes.buscarBicicletaId(10)).thenReturn(Optional.of(20));
        lenient().when(ordenes.buscarPorIdConBloqueo(10)).thenReturn(Optional.of(orden));
        lenient().when(ordenes.findById(10)).thenReturn(Optional.of(orden));
        lenient().when(ordenes.buscarAbiertasPorBicicleta(20)).thenAnswer(i->lista.stream().filter(o->Set.of("CREADA","PAUSADA","EN_PROCESO").contains(o.getEstado().getCodigo())).collect(java.util.stream.Collectors.toList()));
        lenient().when(ordenes.findByBicicletaIdOrderByIdDesc(20)).thenAnswer(i->{var r=new ArrayList<>(lista);r.sort(Comparator.comparing(OrdenMantenimiento::getId).reversed());return r;});
        lenient().when(estados.findByCodigo(anyString())).thenAnswer(i->Optional.of(new EstadoOrdenMantenimiento(i.getArgument(0))));
        lenient().when(estadosBici.findByCodigo(anyString())).thenAnswer(i->Optional.of(new EstadoBicicleta(i.getArgument(0))));
        lenient().when(tipos.findByCodigo(anyString())).thenAnswer(i->Optional.of(new TipoMantenimiento(i.getArgument(0),"Tipo QA")));
        lenient().when(ordenes.saveAndFlush(any())).thenAnswer(i->{OrdenMantenimiento o=i.getArgument(0);ReflectionTestUtils.setField(o,"id",30);ReflectionTestUtils.setField(o,"version",0);lista.add(o);return o;});
        lenient().when(estaciones.buscarActivaPorIdConBloqueo(5)).thenReturn(Optional.of(estacion));
        sesion(admin,"ADMIN");
    }
    @AfterEach void limpiar(){SecurityContextHolder.clearContext();}
    @Test void creaUnaOrdenYAuditaSinHacerlaDisponible(){
        lista.clear(); bici.setEstado(new EstadoBicicleta("DISPONIBLE"));
        var d=new CrearOrdenMantenimientoDTO();d.setBicicletaId(20);d.setTipo("CORRECTIVO");d.setMotivo("Falla");d.setTecnicoId(2);
        var r=servicio.crear(d);
        assertEquals("CREADA",r.estado);assertEquals(2,r.tecnicoId);assertEquals("MANTENIMIENTO",r.estadoBicicleta);
        assertEquals(5,bici.getUbicacionActual().getId());
        verify(historias).save(argThat(h->h.getEstadoAnterior()==null&&"CREADA".equals(h.getEstadoNuevo().getCodigo())&&h.getValoresNuevos().contains("\"tecnicoId\":2")));
    }
    @Test void noDuplicaOrdenAbierta(){
        var d=new CrearOrdenMantenimientoDTO();d.setBicicletaId(20);d.setTipo("CORRECTIVO");d.setMotivo("Falla");
        assertCodigo(409,()->servicio.crear(d));verify(ordenes,never()).saveAndFlush(any());
    }
    @Test void noCreaOrdenAunqueEstadoVisibleMientaSiHayAlquilerActivo(){
        lista.clear();bici.setEstado(new EstadoBicicleta("DISPONIBLE"));when(detalles.contarAsignacionesActivas(20)).thenReturn(1L);
        var d=new CrearOrdenMantenimientoDTO();d.setBicicletaId(20);d.setTipo("CORRECTIVO");d.setMotivo("Revisión");
        assertCodigo(409,()->servicio.crear(d));verify(ordenes,never()).saveAndFlush(any());
    }
    @Test void reactivarConRevisionDesfavorableDerivaCorrectivoSinInventarAptitud(){
        finalizada("NO_APTA");bici.setEstado(new EstadoBicicleta("INACTIVA"));
        assertTrue(servicio.requiereMantenimientoEnReactivacion(bici));
        assertEquals("CORRECTIVO",lista.get(1).getTipoMantenimiento().getCodigo());assertEquals("INACTIVA",bici.getEstado().getCodigo());
    }
    @ParameterizedTest @ValueSource(strings={"EN_USO","RESERVADA","DE_BAJA","EXTRAVIADA"})
    void noEludeEstadosProtegidos(String codigo){
        lista.clear();bici.setEstado(new EstadoBicicleta(codigo));
        var d=new CrearOrdenMantenimientoDTO();d.setBicicletaId(20);d.setTipo("PREVENTIVO");d.setMotivo("Revisión");
        assertCodigo(409,()->servicio.crear(d));verifyNoInteractions(historias);
    }
    @Test void noIniciaTrabajoDeUnaIncidenciaSinRecepcionFisica(){
        bici.setUbicacionActual(null);sesion(tec,"TECNICO");
        assertCodigo(409,()->servicio.operar(10,"iniciar",comando()));verifyNoInteractions(historias);
    }
    @Test void recepcionRealFueraDeEstacionPermiteTrabajarPeroNoHabilitar(){
        bici.setUbicacionActual(null);sesion(tec,"TECNICO");when(incidencias.contarRecepcionesDeOrden(10)).thenReturn(1L);
        assertEquals("EN_PROCESO",servicio.operar(10,"iniciar",comando()).estado);
    }
    @Test void iniciarPausarYReanudarConservaPrimerInicio(){
        sesion(tec,"TECNICO");servicio.operar(10,"iniciar",comando());
        LocalDateTime inicio=orden.getFechaInicio();assertEquals(ahora,inicio);
        var pausa=comando();pausa.setMotivo("Esperar repuesto");servicio.operar(10,"pausar",pausa);
        servicio.operar(10,"reanudar",comando());
        assertEquals(inicio,orden.getFechaInicio());assertNull(orden.getFechaFin());assertEquals("EN_PROCESO",orden.getEstado().getCodigo());
        verify(historias,times(3)).save(any());verify(traza,never()).administracion(any(),any(),anyInt(),any(),any(),any(),any(),any());
    }
    @Test void asignarNoIniciaNiReanuda(){
        orden.setEstado(new EstadoOrdenMantenimiento("PAUSADA"));orden.setFechaInicio(ahora.minusHours(1));
        var d=comando();d.setTecnicoId(3);servicio.operar(10,"asignar",d);
        assertEquals(otro,orden.getTecnico());assertEquals("PAUSADA",orden.getEstado().getCodigo());
        verify(historias).save(argThat(h->h.getEstadoAnterior()==null&&h.getEstadoNuevo()==null&&h.getValoresAnteriores().contains("\"tecnicoId\":2")));
    }
    @Test void noReasignaEnProceso(){enProceso();var d=comando();d.setTecnicoId(3);assertCodigo(409,()->servicio.operar(10,"asignar",d));}
    @Test void noAsignaUsuarioInactivo(){otro.setActivo(false);var d=comando();d.setTecnicoId(3);assertCodigo(409,()->servicio.operar(10,"asignar",d));}
    @ParameterizedTest @ValueSource(strings={"iniciar","reanudar","pausar","trabajo","finalizar"})
    void adminNoSustituyeTecnico(String accion){assertThrows(AccessDeniedException.class,()->servicio.operar(10,accion,comando()));}
    @Test void tecnicoNoTrabajaOrdenAjena(){sesion(otro,"TECNICO");assertThrows(AccessDeniedException.class,()->servicio.operar(10,"iniciar",comando()));}
    @Test void tecnicoRevocadoNoActuaConSesionAnterior(){tec.getUsuarioRoles().clear();sesion(tec,"TECNICO");assertThrows(AccessDeniedException.class,()->servicio.operar(10,"iniciar",comando()));}
    @Test void cuentaInactivaNoActua(){tec.setActivo(false);sesion(tec,"TECNICO");assertThrows(AccessDeniedException.class,()->servicio.operar(10,"iniciar",comando()));}
    @Test void versionViejaNoEscribe(){var d=comando();d.setVersion(2);assertCodigo(409,()->servicio.operar(10,"cancelar",d));verifyNoInteractions(historias);}
    @Test void noAceptaCamposDeOtraAccion(){sesion(tec,"TECNICO");var d=comando();d.setResultadoTecnico("APTA");assertCodigo(400,()->servicio.operar(10,"iniciar",d));}
    @Test void pausaRequiereMotivo(){enProceso();sesion(tec,"TECNICO");assertCodigo(400,()->servicio.operar(10,"pausar",comando()));}
    @ParameterizedTest @ValueSource(strings={"-1.00","1.001","10000000000.00"})
    void costoFueraDeLimiteNoEscribe(String monto){enProceso();sesion(tec,"TECNICO");var d=comando();d.setCostoRepuestos(new BigDecimal(monto));assertCodigo(400,()->servicio.operar(10,"trabajo",d));verifyNoInteractions(historias);}
    @Test void trabajoGuardaSnapshotsYCostoInterno(){
        enProceso();sesion(tec,"TECNICO");var d=comando();d.setDiagnostico("Freno");d.setDescripcionTrabajo("Cambiar cable");d.setCostoRepuestos(new BigDecimal("0.00"));
        servicio.operar(10,"trabajo",d);assertEquals("ARS",orden.getMonedaRepuestos());
        verify(historias).save(argThat(h->h.getEstadoNuevo()==null&&h.getValoresNuevos().contains("Cambiar cable")&&h.getActorUsuario()==tec));
    }

    @Test
    void historialFiltraSnapshotsSegunRolSinBorrarLaHistoria() {
        String antes = "{\"diagnostico\":null}";
        String despues = "{\"diagnostico\":\"Freno revisado\"}";

        var historia = new HistorialOrdenMantenimiento(
                orden, "TRABAJO", null, null, tec, "USUARIO",
                "Revisión del freno", antes, despues, ahora
        );

        when(historias.findByOrdenMantenimientoIdOrderByIdAsc(10))
                .thenReturn(List.of(historia));

        sesion(tec, "TECNICO");

        var tecnico = servicio.historial(10).get(0);

        assertEquals("TRABAJO", tecnico.accion);
        assertEquals("Revisión del freno", tecnico.observaciones);
        assertNull(tecnico.valoresAnteriores);
        assertNull(tecnico.valoresNuevos);

        var mapper = new ObjectMapper().findAndRegisterModules();
        var jsonTecnico = mapper.valueToTree(tecnico);

        assertFalse(jsonTecnico.has("valoresAnteriores"));
        assertFalse(jsonTecnico.has("valoresNuevos"));

        sesion(admin, "ADMIN");

        var administrador = servicio.historial(10).get(0);

        assertEquals(antes, administrador.valoresAnteriores);
        assertEquals(despues, administrador.valoresNuevos);
        assertEquals(antes, historia.getValoresAnteriores());
        assertEquals(despues, historia.getValoresNuevos());
    }

    @Test
    void historialDeOrdenAjenaNoSeEntregaAlTecnico() {
        sesion(otro, "TECNICO");

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.historial(10)
        );

        verify(historias, never())
                .findByOrdenMantenimientoIdOrderByIdAsc(anyInt());
    }

    @ParameterizedTest @ValueSource(strings={"APTA","NO_APTA"})
    void finalizarNoHabilitaAutomaticamente(String resultado){
        enProceso();sesion(tec,"TECNICO");servicio.operar(10,"finalizar",trabajoFinal(resultado));
        assertEquals("FINALIZADA",orden.getEstado().getCodigo());assertEquals(ahora,orden.getFechaFin());
        assertEquals("MANTENIMIENTO",bici.getEstado().getCodigo());assertNull(orden.getCostoRepuestos());
    }
    @Test void noFinalizaSinDiagnostico(){enProceso();sesion(tec,"TECNICO");var d=comando();d.setResultadoTecnico("APTA");assertCodigo(400,()->servicio.operar(10,"finalizar",d));}
    @Test void cancelarNoPruebaAptitud(){var d=comando();d.setMotivo("Clasificación errónea");servicio.operar(10,"cancelar",d);assertNull(orden.getResultadoTecnico());assertEquals("MANTENIMIENTO",bici.getEstado().getCodigo());}
    @Test void habilitaSoloRevisionVigenteEnUbicacionReal(){finalizada("APTA");servicio.operar(10,"habilitar",habilitar());assertEquals("DISPONIBLE",bici.getEstado().getCodigo());verify(traza).estado(eq(bici),any(),eq(admin),eq("Revisión conforme"),eq(ahora));}
    @Test void resultadoNoAptoImpideHabilitar(){finalizada("NO_APTA");assertCodigo(409,()->servicio.operar(10,"habilitar",habilitar()));}
    @Test void nuevaOrdenImpideReutilizarRevisionVieja(){finalizada("APTA");var otra=new OrdenMantenimiento(bici,new TipoMantenimiento("CORRECTIVO","Correctivo"),new EstadoOrdenMantenimiento("CANCELADA"),ahora);ReflectionTestUtils.setField(otra,"id",11);lista.add(otra);assertCodigo(409,()->servicio.operar(10,"habilitar",habilitar()));}
    @Test void fallaNuevaImpideHabilitar(){finalizada("APTA");when(detalles.contarFallasPosteriores(20,ahora)).thenReturn(1L);assertCodigo(409,()->servicio.operar(10,"habilitar",habilitar()));}
    @Test void incidenciaPendienteNoSeResuelveDesdeMantenimiento(){finalizada("APTA");when(incidencias.contarPendientesPorBicicleta(20)).thenReturn(1L);assertCodigo(409,()->servicio.operar(10,"habilitar",habilitar()));}
    @Test void sinPresenciaFisicaNoInventaEstacion(){finalizada("APTA");bici.setUbicacionActual(null);assertCodigo(409,()->servicio.operar(10,"habilitar",habilitar()));assertNull(bici.getUbicacionActual());}
    @Test void estacionInactivaImpideHabilitar(){finalizada("APTA");when(estaciones.buscarActivaPorIdConBloqueo(5)).thenReturn(Optional.empty());assertCodigo(409,()->servicio.operar(10,"habilitar",habilitar()));}
    @Test void versionBicicletaViejaImpideHabilitar(){finalizada("APTA");var d=habilitar();d.setVersionBicicleta(5);assertCodigo(409,()->servicio.operar(10,"habilitar",d));}
    @Test void preventivaVencidaNoUsaCorrectivoComoInspeccionPreventiva(){
        enProceso();sesion(tec,"TECNICO");bici.setFechaAlta(ahora.toLocalDate().minusYears(1));
        servicio.operar(10,"finalizar",trabajoFinal("APTA"));
        assertEquals(2,lista.size());assertEquals("PREVENTIVO",lista.get(1).getTipoMantenimiento().getCodigo());assertNull(lista.get(1).getTecnico());
    }
    @Test void automatismoNoTocaReserva(){bici.setEstado(new EstadoBicicleta("RESERVADA"));assertFalse(servicio.aplicarPreventiva(20));verifyNoInteractions(historias);}
    @ParameterizedTest @ValueSource(strings={"INACTIVA","DE_BAJA","EXTRAVIADA"})
    void calendarioNoLevantaRestriccionesAdministrativas(String estado){bici.setEstado(new EstadoBicicleta(estado));assertFalse(servicio.aplicarPreventiva(20));verifyNoInteractions(historias);}
    @Test void automatismoRegistraOrigenSistemaYSinActor(){
        lista.clear();bici.setEstado(new EstadoBicicleta("DISPONIBLE"));bici.setFechaAlta(ahora.toLocalDate().minusYears(1));
        assertTrue(servicio.aplicarPreventiva(20));assertFalse(servicio.aplicarPreventiva(20));
        verify(historias).save(argThat(h->h.getActorUsuario()==null&&"SISTEMA".equals(h.getOrigen())));assertEquals("MANTENIMIENTO",bici.getEstado().getCodigo());
    }
    @Test void inactivarPausaYDesasignaSinBorrarTrabajo(){
        enProceso();orden.setDiagnostico("Conservar");orden.setCostoRepuestos(new BigDecimal("12.50"));
        when(ordenes.buscarBicicletasAbiertasDeTecnico(2)).thenReturn(List.of(20));
        servicio.inactivarUsuario(2,"Ausencia");
        assertFalse(tec.isActivo());assertNull(orden.getTecnico());assertEquals("PAUSADA",orden.getEstado().getCodigo());
        assertEquals("Conservar",orden.getDiagnostico());assertEquals(ahora.minusHours(1),orden.getFechaInicio());assertNull(orden.getFechaFin());
        verify(historias).save(argThat(h->h.getActorUsuario()==admin&&h.getValoresAnteriores().contains("\"tecnicoId\":2")&&h.getValoresNuevos().contains("\"tecnicoId\":null")));
    }
    @ParameterizedTest @ValueSource(strings={"CREADA","PAUSADA"})
    void desasignarNoInventaTransicionMismoEstado(String estado){
        orden.setEstado(new EstadoOrdenMantenimiento(estado));if(estado.equals("PAUSADA"))orden.setFechaInicio(ahora.minusHours(1));
        when(ordenes.buscarBicicletasAbiertasDeTecnico(2)).thenReturn(List.of(20));
        servicio.inactivarUsuario(2,"Ausencia");
        assertEquals(estado,orden.getEstado().getCodigo());verify(historias).save(argThat(h->h.getEstadoAnterior()==null&&h.getEstadoNuevo()==null));
    }
    @Test void sinMotivoNoInactivaTecnicoConOrdenes(){enProceso();when(ordenes.buscarBicicletasAbiertasDeTecnico(2)).thenReturn(List.of(20));assertCodigo(400,()->servicio.inactivarUsuario(2,null));assertTrue(tec.isActivo());assertNotNull(orden.getTecnico());}
    @Test void adminProtegidoAunqueSeaTambienTecnico(){Rol r=new Rol("TECNICO");ReflectionTestUtils.setField(r,"id",9);admin.getUsuarioRoles().add(new UsuarioRol(admin,r));assertThrows(AccessDeniedException.class,()->servicio.inactivarUsuario(1,"Ausencia"));assertTrue(admin.isActivo());}
    @Test void noInactivaUsuarioYaInactivo(){cliente.setActivo(false);assertCodigo(400,()->servicio.inactivarUsuario(4,null));verifyNoInteractions(historias);}
    @Test void inactivarUsuarioInexistenteDa404(){assertCodigo(404,()->servicio.inactivarUsuario(99,"QA"));verifyNoInteractions(historias);}
    @Test void inactivaClienteConservandoSusDatos(){
        String email=cliente.getEmail();var persona=cliente.getPersona();servicio.inactivarUsuario(4,null);
        assertFalse(cliente.isActivo());assertEquals(email,cliente.getEmail());assertSame(persona,cliente.getPersona());verifyNoInteractions(historias);verify(usuarios,never()).delete(any());
    }
    @Test void consultaTecnicaNoExponeOtraOrden(){sesion(otro,"TECNICO");assertThrows(AccessDeniedException.class,()->servicio.consultar(10));verifyNoInteractions(historias);}
    private void enProceso(){orden.setEstado(new EstadoOrdenMantenimiento("EN_PROCESO"));orden.setFechaInicio(ahora.minusHours(1));}
    private void finalizada(String resultado){enProceso();orden.setEstado(new EstadoOrdenMantenimiento("FINALIZADA"));orden.setFechaFin(ahora);orden.setResultadoTecnico(resultado);}
    private OperacionMantenimientoDTO comando(){var d=new OperacionMantenimientoDTO();d.setVersion(0);return d;}
    private OperacionMantenimientoDTO habilitar(){var d=comando();d.setVersionBicicleta(0);d.setMotivo("Revisión conforme");return d;}
    private OperacionMantenimientoDTO trabajoFinal(String resultado){var d=comando();d.setDiagnostico("Revisado");d.setDescripcionTrabajo("Ajuste y comprobación");d.setResultadoTecnico(resultado);return d;}
    private Usuario usuario(int id,String rol){
        var p=new Persona("Persona","Prueba",LocalDate.of(1990,1,1),new Genero("MASCULINO"),new TipoDocumento("DNI","DNI"),"12345678",new Pais("AR","Argentina"));
        var u=new Usuario(p,"mant."+id,"mant."+id+"@example.com","hash");u.setId(id);var r=new Rol(rol);ReflectionTestUtils.setField(r,"id",id);u.getUsuarioRoles().add(new UsuarioRol(u,r));return u;
    }
    private void sesion(Usuario u,String rol){SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u.getEmail(),null,List.of(new SimpleGrantedAuthority(rol))));}
    private void assertCodigo(int codigo,org.junit.jupiter.api.function.Executable accion){assertEquals(codigo,assertThrows(ResponseStatusException.class,accion).getStatus().value());}
}
