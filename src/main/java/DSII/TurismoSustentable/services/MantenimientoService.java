package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Validator;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
public class MantenimientoService {
    private final OrdenMantenimientoRepository ordenes;
    private final HistorialOrdenMantenimientoRepository historias;
    private final BicicletaRepository bicicletas;
    private final UsuarioRepository usuarios;
    private final TipoMantenimientoRepository tipos;
    private final EstadoOrdenMantenimientoRepository estados;
    private final EstadoBicicletaRepository estadosBicicleta;
    private final EstacionRepository estaciones;
    private final TipoBicicletaRepository tiposBicicleta;
    private final AlquilerDetalleRepository detalles;
    private final IncidenciaRepository incidencias;
    private final TrazabilidadInventarioService traza;
    private final Clock reloj;
    private final Validator validator;
    private final ObjectMapper json;

    public MantenimientoService(
            OrdenMantenimientoRepository ordenes,
            HistorialOrdenMantenimientoRepository historias,
            BicicletaRepository bicicletas,
            UsuarioRepository usuarios,
            TipoMantenimientoRepository tipos,
            EstadoOrdenMantenimientoRepository estados,
            EstadoBicicletaRepository estadosBicicleta,
            EstacionRepository estaciones,
            TipoBicicletaRepository tiposBicicleta,
            AlquilerDetalleRepository detalles,
            IncidenciaRepository incidencias,
            TrazabilidadInventarioService traza,
            Clock reloj,
            Validator validator,
            ObjectMapper json
    ) {
        this.ordenes = ordenes;
        this.historias = historias;
        this.bicicletas = bicicletas;
        this.usuarios = usuarios;
        this.tipos = tipos;
        this.estados = estados;
        this.estadosBicicleta = estadosBicicleta;
        this.estaciones = estaciones;
        this.tiposBicicleta = tiposBicicleta;
        this.detalles = detalles;
        this.incidencias = incidencias;
        this.traza = traza;
        this.reloj = reloj;
        this.validator = validator;
        this.json = json;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','TECNICO')")
    public List<OrdenMantenimientoDTO> listar() {
        Usuario actor=lector();
        return ordenes.listarPorTecnico(rol(actor,"ADMIN")?null:actor.getId()).stream().map(OrdenMantenimientoDTO::new).collect(Collectors.toList());
    }
    @PreAuthorize("hasAnyAuthority('ADMIN','TECNICO')")
    public OrdenMantenimientoDTO consultar(Integer id) { return new OrdenMantenimientoDTO(visible(id,lector())); }
    @PreAuthorize("hasAnyAuthority('ADMIN','TECNICO')")
    public List<HistorialMantenimientoDTO> historial(Integer id) {
        Usuario actor = lector();
        visible(id, actor);

        boolean incluirAuditoria = rol(actor, "ADMIN");

        return historias.findByOrdenMantenimientoIdOrderByIdAsc(id)
                .stream()
                .map(h -> new HistorialMantenimientoDTO(
                        h, incluirAuditoria
                ))
                .collect(Collectors.toList());
    }
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<TecnicoMantenimientoDTO> tecnicos() {
        exigirRol(lector(),"ADMIN");
        return usuarios.buscarTecnicosActivos().stream().map(TecnicoMantenimientoDTO::new).collect(Collectors.toList());
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public OrdenMantenimientoDTO crear(CrearOrdenMantenimientoDTO dto) {
        validar(dto); Map<Integer,Usuario> cuentas=cuentas("ADMIN",dto.getTecnicoId());
        Usuario actor=actorEn(cuentas), tecnico=dto.getTecnicoId()==null?null:cuentas.get(dto.getTecnicoId());
        if(tecnico!=null) tecnicoValido(tecnico);
        Bicicleta b=bicicleta(dto.getBicicletaId()); sinAlquiler(b);
        if(!Set.of("DISPONIBLE","INACTIVA","MANTENIMIENTO").contains(b.getEstado().getCodigo()))
            throw conflicto("La bicicleta no admite una orden desde su estado actual.");
        if(!abiertas(b).isEmpty()) throw conflicto("La bicicleta ya tiene una orden abierta.");
        LocalDateTime ahora=ahora();
        OrdenMantenimiento o=nuevaOrden(b,dto.getTipo(),ahora);
        o.setTecnico(tecnico);
        // La asignación inicial también queda en el snapshot del evento CREAR.
        registrar(o,"CREAR",null,o.getEstado(),actor,dto.getMotivo(),null,ahora);
        aMantenimiento(b,actor,dto.getMotivo(),ahora);
        ordenes.flush();
        return new OrdenMantenimientoDTO(o);
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','TECNICO')")
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public OrdenMantenimientoDTO operar(Integer id,String accion,OperacionMantenimientoDTO dto) {
        validar(dto); boolean tecnica=Set.of("iniciar","reanudar","pausar","trabajo","finalizar").contains(accion);
        if(!tecnica && !Set.of("asignar","cancelar","habilitar").contains(accion)) throw malo("Acción de mantenimiento inválida.");
        campos(dto,accion);
        Map<Integer,Usuario> cuentas=cuentas(tecnica?"TECNICO":"ADMIN","asignar".equals(accion)?dto.getTecnicoId():null);
        Usuario actor=actorEn(cuentas);
        Integer biciId=ordenes.buscarBicicletaId(positivo(id)).orElseThrow(()->noExiste("La orden no existe."));
        Bicicleta b=bicicleta(biciId);
        OrdenMantenimiento o=ordenes.buscarPorIdConBloqueo(id).orElseThrow(()->noExiste("La orden no existe."));
        if(!Objects.equals(o.getBicicleta().getId(),b.getId())) throw interno("La orden cambió de bicicleta.");
        if(!Objects.equals(o.getVersion(),dto.getVersion())) throw conflicto("La orden fue modificada. Recargá los datos.");
        LocalDateTime ahora=ahora(); fechas(o,ahora);
        if(tecnica && (o.getTecnico()==null || o.getTecnico().getId()!=actor.getId()))
            throw new AccessDeniedException("Solo el técnico activo asignado puede trabajar esta orden.");
        if("habilitar".equals(accion)) return habilitar(o,b,actor,dto,ahora);
        sinAlquiler(b);
        if(!"MANTENIMIENTO".equals(b.getEstado().getCodigo())) throw conflicto("La bicicleta no está en mantenimiento.");
        if(tecnica && !"pausar".equals(accion) && b.getUbicacionActual()==null
                && incidencias.contarRecepcionesDeOrden(o.getId())==0)
            throw conflicto("Primero debe registrarse la recepción física de la bicicleta; una orden creada no prueba que haya sido recuperada.");
        String previo=datos(o); EstadoOrdenMantenimiento anterior=o.getEstado();
        String estado=anterior.getCodigo();
        switch(accion) {
            case "asignar":
                if(!Set.of("CREADA","PAUSADA").contains(estado)) throw conflicto("Solo se reasignan órdenes creadas o pausadas.");
                if(dto.getTecnicoId()==null) throw malo("Elegí un técnico activo.");
                Usuario tecnico=cuentas.get(dto.getTecnicoId()); tecnicoValido(tecnico);
                if(o.getTecnico()!=null && o.getTecnico().getId()==tecnico.getId()) return new OrdenMantenimientoDTO(o);
                o.setTecnico(tecnico); break;
            case "iniciar":
                exigirEstado(o,"CREADA"); if(o.getFechaInicio()!=null) throw interno("Una orden creada ya tiene inicio.");
                o.setFechaInicio(ahora); o.setEstado(estado("EN_PROCESO")); break;
            case "reanudar":
                exigirEstado(o,"PAUSADA"); if(o.getFechaInicio()==null) throw interno("La orden pausada no tiene inicio.");
                o.setEstado(estado("EN_PROCESO")); break;
            case "pausar":
                exigirEstado(o,"EN_PROCESO"); motivo(dto.getMotivo());
                o.setMotivoPausaCancelacion(dto.getMotivo()); o.setEstado(estado("PAUSADA")); break;
            case "trabajo":
                exigirEstado(o,"EN_PROCESO"); actualizarTrabajo(o,dto);
                if(previo.equals(datos(o))) return new OrdenMantenimientoDTO(o);
                break;
            case "finalizar":
                exigirEstado(o,"EN_PROCESO"); actualizarTrabajo(o,dto);
                if(vacio(o.getDiagnostico()) || vacio(o.getDescripcionTrabajo()) || dto.getResultadoTecnico()==null)
                    throw malo("Para finalizar se requiere diagnóstico, trabajo realizado y resultado técnico.");
                o.setResultadoTecnico(dto.getResultadoTecnico()); o.setFechaFin(ahora); o.setEstado(estado("FINALIZADA")); break;
            case "cancelar":
                if(!Set.of("CREADA","EN_PROCESO","PAUSADA").contains(estado)) throw conflicto("La orden ya está cerrada.");
                motivo(dto.getMotivo()); o.setMotivoPausaCancelacion(dto.getMotivo());
                o.setFechaFin(ahora); o.setEstado(estado("CANCELADA")); break;
            default: throw malo("Acción inválida.");
        }
        registrar(o,accion.toUpperCase(Locale.ROOT),anterior,o.getEstado(),actor,dto.getMotivo(),previo,ahora);
        ordenes.flush();
        if(Set.of("finalizar","cancelar").contains(accion) && "CORRECTIVO".equals(o.getTipoMantenimiento().getCodigo()) && preventiva(b,ahora)) {
            garantizarPreventiva(b,"Preventiva pendiente después del cierre correctivo.",ahora);
        }
        return new OrdenMantenimientoDTO(o);
    }

    private OrdenMantenimientoDTO habilitar(OrdenMantenimiento o,Bicicleta b,Usuario actor,OperacionMantenimientoDTO dto,LocalDateTime ahora) {
        motivo(dto.getMotivo()); sinAlquiler(b);
        if(dto.getVersionBicicleta()==null || !dto.getVersionBicicleta().equals(b.getVersion()))
            throw conflicto("La bicicleta fue modificada. Recargá los datos.");
        if(!"MANTENIMIENTO".equals(b.getEstado().getCodigo())) throw conflicto("La bicicleta no está pendiente de habilitación.");
        if(!"FINALIZADA".equals(o.getEstado().getCodigo()) || !"APTA".equals(o.getResultadoTecnico()) || o.getFechaFin()==null)
            throw conflicto("La orden no tiene una revisión técnica favorable finalizada.");
        List<OrdenMantenimiento> ultimas=ordenes.findByBicicletaIdOrderByIdDesc(b.getId());
        if(ultimas.isEmpty() || !ultimas.get(0).getId().equals(o.getId())) throw conflicto("Existe una intervención posterior; no puede reutilizarse esta revisión.");
        if(!abiertas(b).isEmpty() || detalles.contarFallasPosteriores(b.getId(),o.getFechaFin())>0
                || incidencias.contarPendientesPorBicicleta(b.getId())>0)
            throw conflicto("Hay órdenes, fallas o incidencias pendientes que impiden habilitar.");
        if(preventiva(b,ahora)) throw conflicto("Falta completar una preventiva vigente antes de habilitar.");
        if(b.getUbicacionActual()==null) throw conflicto("Debe registrar la presencia física antes de habilitar.");
        estaciones.buscarActivaPorIdConBloqueo(b.getUbicacionActual().getId())
                .orElseThrow(()->conflicto("La estación actual está inactiva o fue eliminada."));
        String antes=traza.datos(b); EstadoBicicleta anterior=b.getEstado();
        b.setEstado(estadoBici("DISPONIBLE")); bicicletas.flush();
        traza.estado(b,anterior,actor,dto.getMotivo(),ahora);
        traza.administracion(actor,"BICICLETA",b.getId(),"HABILITAR_MANTENIMIENTO",antes,traza.datos(b),dto.getMotivo(),ahora);
        registrar(o,"HABILITAR",null,null,actor,dto.getMotivo(),datos(o),ahora);
        return new OrdenMantenimientoDTO(o);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public TipoBicicletaDTO actualizarIntervalo(Integer id,ActualizarIntervaloPreventivoDTO dto) {
        validar(dto); Usuario actor=actorEn(cuentas("ADMIN"));
        TipoBicicleta tipo=tiposBicicleta.buscarPorIdConBloqueo(positivo(id)).orElseThrow(()->noExiste("El tipo de bicicleta no existe."));
        if(!Objects.equals(tipo.getIntervaloPreventivoMeses(),dto.getMesesAnteriores())) throw conflicto("El intervalo fue modificado. Recargá los datos.");
        if(!tipo.getIntervaloPreventivoMeses().equals(dto.getMeses())) {
            String antes=String.valueOf(tipo.getIntervaloPreventivoMeses()); tipo.setIntervaloPreventivoMeses(dto.getMeses());
            tiposBicicleta.flush();
            traza.administracion(actor,"TIPO_BICICLETA",id,"ACTUALIZAR_PREVENTIVO",antes,String.valueOf(dto.getMeses()),dto.getMotivo(),ahora());
        }
        return new TipoBicicletaDTO(tipo);
    }

    // UsuarioService ya bloqueó actor/objetivo y validó la inactivación.
    // Solo prepara órdenes; no modifica la cuenta ni registra su auditoría.
    @Transactional(propagation = Propagation.MANDATORY, readOnly = false)
    public void prepararOrdenesPorInactivacion(Usuario objetivo, Usuario actor,
                                             String razon, LocalDateTime ahora) {
        exigirRol(actor, "ADMIN");
        Integer id = positivo(objetivo.getId());
        List<OrdenMantenimiento> afectadas = new ArrayList<>();
        for (Integer biciId : ordenes.buscarBicicletasAbiertasDeTecnico(id)) {
            Bicicleta bicicleta = bicicleta(biciId); // Bloquear antes de sus órdenes.
            for (OrdenMantenimiento orden : abiertas(bicicleta)) {
                if (orden.getTecnico() != null && orden.getTecnico().getId() == id) {
                    afectadas.add(orden);
                }
            }
        }
        if (!afectadas.isEmpty()) motivo(razon);
        for (OrdenMantenimiento orden : afectadas) {
            fechas(orden, ahora);
            String antes = datos(orden);
            EstadoOrdenMantenimiento anterior = orden.getEstado();
            if ("EN_PROCESO".equals(anterior.getCodigo())) {
                orden.setEstado(estado("PAUSADA"));
                orden.setMotivoPausaCancelacion(razon);
            }
            orden.setTecnico(null);
            registrar(orden, "DESASIGNAR_POR_INACTIVACION", anterior, orden.getEstado(),
                    actor, razon, antes, ahora);
        }
    }

    // El proceso automático bloquea la bicicleta; nunca altera reservas, viajes o INACTIVA.
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public boolean aplicarPreventiva(Integer id) {
        Bicicleta b=bicicletas.buscarActivaPorIdConBloqueo(positivo(id)).orElse(null);
        if(b==null || !"DISPONIBLE".equals(b.getEstado().getCodigo())) return false;
        sinAlquiler(b); LocalDateTime ahora=ahora();
        if(!preventiva(b,ahora)) return false;
        garantizarPreventiva(b,"Revisión preventiva vencida por calendario UTC.",ahora);
        aMantenimiento(b,null,"Revisión preventiva vencida por calendario UTC.",ahora);
        bicicletas.flush(); return true;
    }

    // Inventario ya tiene bloqueada la bicicleta y validada la ubicación de la reactivación.
    @Transactional(propagation=Propagation.MANDATORY,readOnly=false)
    public boolean requiereMantenimientoEnReactivacion(Bicicleta b) {
        exigirRol(lector(),"ADMIN");
        LocalDateTime ahora=ahora();
        if(!abiertas(b).isEmpty()) return true;
        List<OrdenMantenimiento> anteriores=ordenes.findByBicicletaIdOrderByIdDesc(b.getId());
        if(incidencias.contarPendientesPorBicicleta(b.getId())>0) throw conflicto("Hay incidencias pendientes que impiden reactivar.");
        OrdenMantenimiento ultima=anteriores.isEmpty()?null:anteriores.get(0);
        boolean falla=ultima==null ? detalles.contarFallasReportadas(b.getId())>0
                : !"APTA".equals(ultima.getResultadoTecnico()) || ultima.getFechaFin()==null
                    || detalles.contarFallasPosteriores(b.getId(),ultima.getFechaFin())>0;
        if(falla) {
            OrdenMantenimiento o=nuevaOrden(b,"CORRECTIVO",ahora);
            registrar(o,"CREAR",null,o.getEstado(),null,"Revisión correctiva requerida al reactivar.",null,ahora);
            ordenes.flush(); return true;
        }
        if(preventiva(b,ahora)) { garantizarPreventiva(b,"Preventiva requerida al reactivar.",ahora); return true; }
        return false;
    }

    private void garantizarPreventiva(Bicicleta b,String razon,LocalDateTime ahora) {
        if(!abiertas(b).isEmpty()) return;
        OrdenMantenimiento o=nuevaOrden(b,"PREVENTIVO",ahora);
        registrar(o,"CREAR",null,o.getEstado(),null,razon,null,ahora);
        ordenes.flush();
    }
    private OrdenMantenimiento nuevaOrden(Bicicleta b,String tipo,LocalDateTime ahora) {
        TipoMantenimiento t=tipos.findByCodigo(tipo).orElseThrow(()->interno("Falta el tipo de mantenimiento "+tipo));
        return ordenes.saveAndFlush(new OrdenMantenimiento(b,t,estado("CREADA"),ahora));
    }
    private void aMantenimiento(Bicicleta b,Usuario actor,String razon,LocalDateTime ahora) {
        if("MANTENIMIENTO".equals(b.getEstado().getCodigo())) return;
        EstadoBicicleta anterior=b.getEstado(); String antes=traza.datos(b); b.setEstado(estadoBici("MANTENIMIENTO"));
        if(actor==null) traza.estadoSistema(b,anterior,razon,ahora);
        else {
            traza.estado(b,anterior,actor,razon,ahora);
            traza.administracion(actor,"BICICLETA",b.getId(),"DERIVAR_MANTENIMIENTO",antes,traza.datos(b),razon,ahora);
        }
    }
    private boolean preventiva(Bicicleta b,LocalDateTime ahora) {
        return CalendarioPreventivo.vencida(b.getFechaAlta(),ordenes.buscarUltimaPreventivaApta(b.getId()),
                b.getTipo()==null?null:b.getTipo().getIntervaloPreventivoMeses(),ahora);
    }
    private List<OrdenMantenimiento> abiertas(Bicicleta b) {
        List<OrdenMantenimiento> lista=ordenes.buscarAbiertasPorBicicleta(b.getId());
        if(lista.size()>1) throw interno("La bicicleta tiene más de una orden abierta.");
        return lista;
    }
    private void sinAlquiler(Bicicleta b) {
        if (detalles.contarAsignacionesActivas(b.getId()) > 0) {
            throw conflicto(
                    "La bicicleta tiene una reserva o viaje activo; "
                            + "debe resolverlo por su circuito."
            );
        }
    }
    private Bicicleta bicicleta(Integer id) {
        return bicicletas.buscarActivaPorIdConBloqueo(positivo(id)).orElseThrow(()->noExiste("La bicicleta no existe o fue eliminada."));
    }
    private void actualizarTrabajo(OrdenMantenimiento o,OperacionMantenimientoDTO d) {
        if(d.getDiagnostico()!=null) o.setDiagnostico(d.getDiagnostico());
        if(d.getDescripcionTrabajo()!=null) o.setDescripcionTrabajo(d.getDescripcionTrabajo());
        if(d.getObservaciones()!=null) o.setObservaciones(d.getObservaciones());
        if(d.getCostoRepuestos()!=null) {o.setCostoRepuestos(d.getCostoRepuestos()); o.setMonedaRepuestos("ARS");}
    }
    private OrdenMantenimiento visible(Integer id,Usuario actor) {
        OrdenMantenimiento o=ordenes.findById(positivo(id)).orElseThrow(()->noExiste("La orden no existe."));
        if(!rol(actor,"ADMIN") && (o.getTecnico()==null || o.getTecnico().getId()!=actor.getId()))
            throw new AccessDeniedException("No puede consultar órdenes ajenas o sin asignación.");
        return o;
    }
    private Usuario lector() {
        Authentication a=autenticacion();
        Usuario u=usuarios.buscarPorEmailConDatosYRoles(a.getName()).filter(Usuario::isActivo)
                .orElseThrow(()->new AccessDeniedException("La cuenta no está activa."));
        if(!rol(u,"ADMIN") && !rol(u,"TECNICO")) throw new AccessDeniedException("Se requiere rol ADMIN o TECNICO vigente.");
        return u;
    }
    private Map<Integer,Usuario> cuentas(String permiso,Integer... extras) {
        Authentication a=autenticacion();
        Integer actor=usuarios.buscarIdPorEmail(a.getName()).orElseThrow(()->new AccessDeniedException("La cuenta no existe."));
        SortedSet<Integer> ids=new TreeSet<>(); ids.add(actor);
        for(Integer extra:extras) if(extra!=null) ids.add(positivo(extra));
        Map<Integer,Usuario> lista=new HashMap<>();
        for(Integer id:ids) lista.put(id,usuarios.buscarPorIdConBloqueo(id).orElseThrow(()->noExiste("El usuario no existe.")));
        exigirRol(lista.get(actor),permiso); return lista;
    }
    private Usuario actorEn(Map<Integer,Usuario> cuentas) {
        return cuentas.values().stream().filter(u->u.getEmail().equals(autenticacion().getName())).findFirst().orElseThrow(()->new AccessDeniedException("La cuenta no existe."));
    }
    private Authentication autenticacion() {
        Authentication a=SecurityContextHolder.getContext().getAuthentication();
        if(a==null || !a.isAuthenticated()) throw new AccessDeniedException("Debe iniciar sesión."); return a;
    }
    private boolean tieneRol(Usuario u,String rol) {return u.getUsuarioRoles().stream().anyMatch(ur->rol.equals(ur.getRol().getCodigo()));}
    private boolean rol(Usuario u,String rol) {return u.isActivo() && tieneRol(u,rol) && autenticacion().getAuthorities().stream().anyMatch(a->rol.equals(a.getAuthority()));}
    private void exigirRol(Usuario u,String rol) {if(!rol(u,rol)) throw new AccessDeniedException("Se requiere una cuenta activa con rol "+rol+" vigente.");}
    private void tecnicoValido(Usuario u) {if(u==null || !u.isActivo() || !tieneRol(u,"TECNICO")) throw conflicto("El técnico debe estar activo y tener rol TECNICO vigente.");}
    private void exigirEstado(OrdenMantenimiento o,String codigo) {if(!codigo.equals(o.getEstado().getCodigo())) throw conflicto("La acción no corresponde al estado actual de la orden.");}
    private EstadoOrdenMantenimiento estado(String c) {return estados.findByCodigo(c).orElseThrow(()->interno("Falta el estado de orden "+c));}
    private EstadoBicicleta estadoBici(String c) {return estadosBicicleta.findByCodigo(c).orElseThrow(()->interno("Falta el estado de bicicleta "+c));}
    private void fechas(OrdenMantenimiento o,LocalDateTime ahora) {
        String codigo=o.getEstado().getCodigo();
        if((Set.of("EN_PROCESO","PAUSADA").contains(codigo) && (o.getFechaInicio()==null || o.getFechaFin()!=null))
                || ("CREADA".equals(codigo) && (o.getFechaInicio()!=null || o.getFechaFin()!=null))
                || (Set.of("FINALIZADA","CANCELADA").contains(codigo) && o.getFechaFin()==null))
            throw interno("El estado y las fechas de la orden no son coherentes.");
        if(o.getFechaCreacion()==null || o.getFechaCreacion().isAfter(ahora)
                || (o.getFechaInicio()!=null && (o.getFechaInicio().isBefore(o.getFechaCreacion()) || o.getFechaInicio().isAfter(ahora)))
                || (o.getFechaFin()!=null && (o.getFechaFin().isBefore(o.getFechaInicio()==null?o.getFechaCreacion():o.getFechaInicio()) || o.getFechaFin().isAfter(ahora))))
            throw interno("Las fechas de la orden son inconsistentes.");
    }
    private void registrar(OrdenMantenimiento o,String accion,EstadoOrdenMantenimiento anterior,EstadoOrdenMantenimiento nuevo,
            Usuario actor,String razon,String antes,LocalDateTime ahora) {
        boolean transicion=anterior!=null && nuevo!=null && !anterior.getCodigo().equals(nuevo.getCodigo());
        boolean creacion="CREAR".equals(accion);
        String despues=datos(o);
        historias.save(new HistorialOrdenMantenimiento(o,accion,transicion?anterior:null,creacion||transicion?nuevo:null,
                actor,actor==null?"SISTEMA":"USUARIO",razon,antes,despues,ahora));
        if(actor!=null && rol(actor,"ADMIN") && !Set.of("INICIAR","REANUDAR","PAUSAR","TRABAJO","FINALIZAR").contains(accion))
            traza.administracion(actor,"ORDEN_MANTENIMIENTO",o.getId(),accion,antes,despues,razon,ahora);
    }
    private String datos(OrdenMantenimiento o) {
        Map<String,Object> m=new LinkedHashMap<>(); m.put("bicicletaId",o.getBicicleta().getId());
        m.put("estado",o.getEstado().getCodigo()); m.put("tipo",o.getTipoMantenimiento().getCodigo());
        m.put("tecnicoId",o.getTecnico()==null?null:o.getTecnico().getId());
        m.put("diagnostico",o.getDiagnostico()); m.put("descripcionTrabajo",o.getDescripcionTrabajo());
        m.put("observaciones",o.getObservaciones()); m.put("costoRepuestos",o.getCostoRepuestos());
        m.put("monedaRepuestos",o.getMonedaRepuestos()); m.put("resultadoTecnico",o.getResultadoTecnico());
        m.put("motivoPausaCancelacion",o.getMotivoPausaCancelacion());
        m.put("fechaInicio",o.getFechaInicio()==null?null:o.getFechaInicio().toString());
        m.put("fechaFin",o.getFechaFin()==null?null:o.getFechaFin().toString());
        try {return json.writeValueAsString(m);} catch(JsonProcessingException e){throw interno("No se pudo registrar la historia de mantenimiento.");}
    }
    private void campos(OperacionMantenimientoDTO d,String accion) {
        Set<String> permitidos=new HashSet<>();
        if("asignar".equals(accion)) permitidos.add("tecnicoId");
        if(Set.of("pausar","cancelar","habilitar").contains(accion)) permitidos.add("motivo");
        if("habilitar".equals(accion)) permitidos.add("versionBicicleta");
        if(Set.of("trabajo","finalizar").contains(accion)) permitidos.addAll(Set.of("diagnostico","descripcionTrabajo","observaciones","costoRepuestos"));
        if("finalizar".equals(accion)) permitidos.add("resultadoTecnico");
        Map<String,Object> datos=new LinkedHashMap<>();
        datos.put("tecnicoId",d.getTecnicoId()); datos.put("motivo",d.getMotivo()); datos.put("versionBicicleta",d.getVersionBicicleta());
        datos.put("diagnostico",d.getDiagnostico()); datos.put("descripcionTrabajo",d.getDescripcionTrabajo());
        datos.put("observaciones",d.getObservaciones()); datos.put("costoRepuestos",d.getCostoRepuestos()); datos.put("resultadoTecnico",d.getResultadoTecnico());
        if(datos.entrySet().stream().anyMatch(e->e.getValue()!=null && !permitidos.contains(e.getKey()))) throw malo("La acción contiene campos que no le corresponden.");
    }
    private void validar(Object dto) {if(dto==null || !validator.validate(dto).isEmpty()) throw malo("Revisá los datos obligatorios, longitudes, importes y versión.");}
    private boolean vacio(String s) {return s==null || s.isBlank();}
    private void motivo(String s) {if(vacio(s) || s.length()>2000) throw malo("Ingresá un motivo de hasta 2000 caracteres.");}
    private Integer positivo(Integer i) {if(i==null || i<=0) throw malo("El identificador no es válido."); return i;}
    private LocalDateTime ahora() {return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC);}
    private ResponseStatusException conflicto(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);}
    private ResponseStatusException malo(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
    private ResponseStatusException noExiste(String s){return new ResponseStatusException(HttpStatus.NOT_FOUND,s);}
    private ConfiguracionSistemaException interno(String s){return new ConfiguracionSistemaException(s);}
}
