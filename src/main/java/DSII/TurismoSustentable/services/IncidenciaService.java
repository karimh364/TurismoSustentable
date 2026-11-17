package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Validator;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
public class IncidenciaService {
    private static final Set<String> TERMINALES=Set.of("DEVUELTO","CANCELADO","VENCIDO","CERRADO_POR_INCIDENCIA");
    private final IncidenciaRepository incidencias;
    private final HistorialIncidenciaRepository historias;
    private final AlquilerRepository alquileres;
    private final AlquilerDetalleRepository detalles;
    private final BicicletaRepository bicicletas;
    private final UsuarioRepository usuarios;
    private final EstacionRepository estaciones;
    private final EstadoAlquilerDetalleRepository estadosDetalle;
    private final EstadoAlquilerRepository estadosAlquiler;
    private final EstadoBicicletaRepository estadosBici;
    private final HistorialEstadoDetalleRepository historiasDetalle;
    private final OrdenMantenimientoRepository ordenes;
    private final HistorialOrdenMantenimientoRepository historiasOrden;
    private final TipoMantenimientoRepository tiposOrden;
    private final EstadoOrdenMantenimientoRepository estadosOrden;
    private final TrazabilidadInventarioService traza;
    private final CalculadoraAlquiler calculadora;
    private final LiquidacionAlquilerService liquidacion;
    private final Validator validator;
    private final Clock reloj;
    private final ObjectMapper json;
    @Autowired(required=false) private ReembolsoService reembolsos;

    public IncidenciaService(IncidenciaRepository incidencias,HistorialIncidenciaRepository historias,
            AlquilerRepository alquileres,AlquilerDetalleRepository detalles,BicicletaRepository bicicletas,
            UsuarioRepository usuarios,EstacionRepository estaciones,EstadoAlquilerDetalleRepository estadosDetalle,
            EstadoAlquilerRepository estadosAlquiler,EstadoBicicletaRepository estadosBici,
            HistorialEstadoDetalleRepository historiasDetalle,OrdenMantenimientoRepository ordenes,
            HistorialOrdenMantenimientoRepository historiasOrden,TipoMantenimientoRepository tiposOrden,
            EstadoOrdenMantenimientoRepository estadosOrden,TrazabilidadInventarioService traza,
            CalculadoraAlquiler calculadora,LiquidacionAlquilerService liquidacion,Validator validator,Clock reloj,ObjectMapper json) {
        this.incidencias=incidencias;this.historias=historias;this.alquileres=alquileres;this.detalles=detalles;this.bicicletas=bicicletas;
        this.usuarios=usuarios;this.estaciones=estaciones;this.estadosDetalle=estadosDetalle;this.estadosAlquiler=estadosAlquiler;
        this.estadosBici=estadosBici;this.historiasDetalle=historiasDetalle;this.ordenes=ordenes;this.historiasOrden=historiasOrden;
        this.tiposOrden=tiposOrden;this.estadosOrden=estadosOrden;this.traza=traza;this.calculadora=calculadora;
        this.liquidacion=liquidacion;this.validator=validator;this.reloj=reloj;this.json=json;
    }

    @PreAuthorize("hasAnyAuthority('CLIENTE','ADMIN')")
    public List<IncidenciaDTO> listar() {
        Usuario actor=cuenta(false);
        return incidencias.listarVisibles(esAdmin(actor)?null:actor.getId()).stream().map(i->salida(i,esAdmin(actor))).collect(Collectors.toList());
    }
    @PreAuthorize("hasAnyAuthority('CLIENTE','ADMIN')")
    public IncidenciaDTO consultar(Integer id) {Usuario actor=cuenta(false);return salida(visible(id,actor),esAdmin(actor));}
    @PreAuthorize("hasAnyAuthority('CLIENTE','ADMIN')")
    public List<HistorialIncidenciaDTO> historial(Integer id) {
        Usuario actor=cuenta(false);visible(id,actor);return historias.findByIncidenciaIdOrderByIdAsc(id).stream().map(h->new HistorialIncidenciaDTO(h,esAdmin(actor))).collect(Collectors.toList());
    }

    @PreAuthorize("hasAnyAuthority('CLIENTE','ADMIN')")
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public IncidenciaDTO reportar(ReportarIncidenciaDTO dto) {
        validar(dto);Usuario actor=cuenta(true);
        Alquiler a=cabecera(dto.alquilerId);propietario(a,actor);
        AlquilerDetalle d=detalles.buscarPorAlquilerYDetalleConBloqueo(a.getId(),dto.detalleId).orElseThrow(()->noExiste("La bicicleta no pertenece a este alquiler."));
        String descripcion=dto.descripcion.trim(),lugar=limpiar(dto.lugarReportado);
        LocalDateTime hecho=utc(dto.fechaHoraHechoInformada),ahora=ahora();
        Usuario informante=actor;
        if(esAdmin(actor)) informante=dto.informanteId==null?null:usuarios.findById(dto.informanteId).orElseThrow(()->malo("El informante no existe."));
        else if(dto.informanteId!=null && dto.informanteId!=actor.getId()) throw new AccessDeniedException("No podés elegir otro informante.");
        // Una repetición idéntica conserva el reporte original, aun si ya fue resuelto.
        for(Incidencia previa:incidencias.findByAlquilerDetalleIdOrderByIdAsc(d.getId())) {
            if(dto.tipo.equals(previa.getTipo()) && descripcion.equals(previa.getDescripcion())
                    && Objects.equals(lugar,previa.getLugarReportado()) && hecho.equals(previa.getFechaHoraHechoInformada())
                    && previa.getRegistradoPor().getId()==actor.getId()
                    && Objects.equals(informante==null?null:informante.getId(),previa.getInformante()==null?null:previa.getInformante().getId())) return salida(previa,esAdmin(actor));
        }
        version(dto.versionDetalle,d.getVersion(),"El detalle");
        if(a.isBorradoAlquiler() || a.getFechaHoraLiquidacion()!=null || !"EN_CURSO".equals(d.getEstado().getCodigo())
                || !"EN_PROGRESO".equals(a.getEstadoContratacion().getCodigo())) throw conflicto("Solo podés reportar una incidencia durante el uso de esa bicicleta.");
        if(d.getFechaHoraRetiro()==null || hecho.isBefore(d.getFechaHoraRetiro()) || hecho.isAfter(ahora))
            throw malo("El hecho debe estar entre el retiro y el momento actual, con zona horaria válida.");
        Bicicleta b=bicicleta(d);
        if(b.isBorradoBici() || !"EN_USO".equals(b.getEstado().getCodigo()) || b.getUbicacionActual()!=null) throw conflicto("La bicicleta no figura en uso fuera de estación.");
        Incidencia i=new Incidencia();i.setAlquilerDetalle(d);i.setRegistradoPor(actor);i.setInformante(informante);
        i.setTipo(dto.tipo);i.setDescripcion(descripcion);i.setLugarReportado(lugar);i.setFechaHoraHechoInformada(hecho);
        i.setFechaHoraRegistro(ahora);i.setEstadoRevision("PENDIENTE");i.setRequiereRevisionEconomica(true);
        incidencias.saveAndFlush(i);evento(i,"REPORTAR",null,"PENDIENTE",actor,"Reporte recibido; el corte todavía requiere revisión administrativa.",null,ahora);
        incidencias.flush();return salida(i,esAdmin(actor));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public IncidenciaDTO resolver(Integer id,ResolverIncidenciaDTO dto) {
        validar(dto);Contexto c=contexto(id);Incidencia i=c.i;AlquilerDetalle d=c.d;Alquiler a=c.a;
        LocalDateTime corte=dto.fechaHoraCorteValidado==null?null:utc(dto.fechaHoraCorteValidado),ahora=ahora();
        if(!"PENDIENTE".equals(i.getEstadoRevision())) {
            if(dto.decision.equals(i.getEstadoRevision()) && Objects.equals(corte,i.getFechaHoraCorteValidado())
                    && dto.motivo.trim().equals(i.getMotivoDecision())) return salida(i);
            throw conflicto("La incidencia ya fue resuelta; no se reabre ni se sustituye su decisión.");
        }
        versiones(c,dto.version,dto.versionBicicleta);
        if(a.getFechaHoraLiquidacion()!=null) throw conflicto("No puede modificarse una liquidación definitiva.");
        if(!Set.of("EN_CURSO","DEVUELTO","CERRADO_POR_INCIDENCIA").contains(d.getEstado().getCodigo())) throw conflicto("El detalle no admite esta revisión.");
        if(i.getFechaHoraRegistro()==null || ahora.isBefore(i.getFechaHoraRegistro())) throw interno("Las fechas de revisión son inconsistentes.");
        boolean validar="VALIDADA".equals(dto.decision);
        if(validar) {
            if(corte==null || d.getFechaHoraRetiro()==null || corte.isBefore(d.getFechaHoraRetiro()) || corte.isAfter(i.getFechaHoraRegistro())
                    || corte.isAfter(ahora) || (d.getFechaHoraDevolucionReal()!=null && corte.isAfter(d.getFechaHoraDevolucionReal())))
                throw malo("El corte debe estar entre el retiro y el registro del reporte, sin superar la recepción física ni la decisión.");
            if(d.getIncidenciaCorte()!=null && !d.getIncidenciaCorte().getId().equals(i.getId())
                    && !Objects.equals(d.getFechaHoraFinUso(),corte)) throw conflicto("Ya existe un corte aplicado. Otra revisión no puede reemplazarlo por uno distinto.");
        } else if(corte!=null) throw malo("Descartar la solicitud no admite un corte validado.");
        String antes=datos(i);
        i.setEstadoRevision(dto.decision);i.setResueltoPor(c.actor);i.setFechaHoraDecision(ahora);
        i.setMotivoDecision(dto.motivo.trim());i.setFechaHoraCorteValidado(corte);
        if(validar && d.getIncidenciaCorte()==null) {
            d.setIncidenciaCorte(i);d.setFechaHoraFinUso(corte);
            if("EN_CURSO".equals(d.getEstado().getCodigo())) {
                if(!"EN_USO".equals(c.b.getEstado().getCodigo()) || c.b.getUbicacionActual()!=null) throw conflicto("El viaje no coincide con la situación física de la bicicleta.");
                EstadoAlquilerDetalle anterior=d.getEstado();d.setEstado(estadoDetalle("CERRADO_POR_INCIDENCIA"));
                d.setFechaHoraCierreDetalle(ahora);d.setOrigenCierre("ADMIN");d.setMotivoCierre(dto.motivo.trim());
                historiasDetalle.save(new HistorialEstadoDetalle(d,anterior,d.getEstado(),c.actor,"USUARIO","Cierre excepcional por incidencia #"+i.getId()+". "+dto.motivo.trim(),ahora));
                cambiarBici(c,Set.of("PERDIDA","ROBO").contains(i.getTipo())?"EXTRAVIADA":"MANTENIMIENTO",dto.motivo.trim(),ahora);
                if("MANTENIMIENTO".equals(c.b.getEstado().getCodigo())) vincularOrden(i,orden(c.b,i,ahora));
            }
        }
        // Si volvió antes de decidir, DEVUELTO y su recepción real se conservan.
        if(d.getOrdenMantenimiento()!=null && i.getOrdenMantenimiento()==null) i.setOrdenMantenimiento(d.getOrdenMantenimiento());
        evento(i,"RESOLVER","PENDIENTE",dto.decision,c.actor,dto.motivo.trim(),antes,ahora);
        normalizarImportes(d);cerrarYLiquidar(a,ahora);incidencias.flush();return salida(i);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public IncidenciaDTO recibir(Integer id,RecibirIncidenciaDTO dto) {
        validar(dto);Contexto c=contexto(id);String lugar=limpiar(dto.lugarRecepcion);
        if((dto.estacionId==null)==(lugar==null)) throw malo("Elegí una estación real o indicá el lugar de recepción, no ambos.");
        if(c.i.getFechaHoraRecepcionFisica()!=null) {
            if(Objects.equals(dto.estacionId,c.i.getEstacionRecepcion()==null?null:c.i.getEstacionRecepcion().getId()) && Objects.equals(lugar,c.i.getLugarRecepcion())) return salida(c.i);
            throw conflicto("La recepción ya está registrada; no puede reemplazarse.");
        }
        versiones(c,dto.version,dto.versionBicicleta);
        if(!"CERRADO_POR_INCIDENCIA".equals(c.d.getEstado().getCodigo()) || c.d.getIncidenciaCorte()==null)
            throw conflicto("Mientras siga en uso, registrá la devolución normal o resolvé primero el cierre excepcional.");
        if(!Set.of("EXTRAVIADA","MANTENIMIENTO").contains(c.b.getEstado().getCodigo()) || c.b.getUbicacionActual()!=null || c.b.isBorradoBici())
            throw conflicto("La bicicleta no está pendiente de recepción o ya tiene una ubicación registrada.");
        sinOperaciones(c.b);LocalDateTime ahora=ahora();Estacion destino=null;
        if(dto.estacionId!=null) {
            destino=estaciones.buscarActivaPorIdConBloqueo(dto.estacionId).orElseThrow(()->conflicto("La estación está inactiva o fue eliminada."));
            if(bicicletas.countByUbicacionActual(destino)>=destino.getCapacidad()) throw conflicto("La estación de recepción no tiene espacios libres.");
        }
        String antesBici=traza.datos(c.b);c.b.setUbicacionActual(destino);
        cambiarBici(c,"MANTENIMIENTO",dto.motivo.trim(),ahora);
        OrdenMantenimiento orden=orden(c.b,c.i,ahora);c.d.setOrdenMantenimiento(orden);
        for(Incidencia reporte:incidencias.findByAlquilerDetalleIdOrderByIdAsc(c.d.getId())) {
            if(reporte.getFechaHoraRecepcionFisica()!=null) continue;
            String antes=datos(reporte);recepcion(reporte,destino,lugar,c.actor,ahora);vincularOrden(reporte,orden);
            evento(reporte,"RECIBIR",null,null,c.actor,dto.motivo.trim(),antes,ahora);
        }
        traza.administracion(c.actor,"BICICLETA",c.b.getId(),"RECUPERAR_INCIDENCIA",antesBici,traza.datos(c.b),dto.motivo.trim(),ahora);
        // No cambia DEVUELTO/cierre, tiempos, importes, pagos o liquidación anterior.
        incidencias.flush();return salida(c.i);
    }

    /** Ingreso real posterior a una recepción fuera de estación; conserva aquel hecho original. */
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public IncidenciaDTO ubicar(Integer id,RecibirIncidenciaDTO dto) {
        validar(dto);Contexto c=contexto(id);
        if(dto.estacionId==null || limpiar(dto.lugarRecepcion)!=null) throw malo("Elegí únicamente la estación de ingreso real.");
        if(c.i.getFechaHoraRecepcionFisica()==null || c.i.getRecibidoPor()==null || c.i.getLugarRecepcion()==null
                || c.i.getEstacionRecepcion()!=null || !"CERRADO_POR_INCIDENCIA".equals(c.d.getEstado().getCodigo())
                || c.b.isBorradoBici() || !"MANTENIMIENTO".equals(c.b.getEstado().getCodigo()))
            throw conflicto("Este ingreso requiere una recepción previa fuera de estación y una bicicleta en mantenimiento.");
        if(c.b.getUbicacionActual()!=null) {
            if(c.b.getUbicacionActual().getId()==dto.estacionId) return salida(c.i);
            throw conflicto("La bicicleta ya tiene una ubicación registrada.");
        }
        versiones(c,dto.version,dto.versionBicicleta);sinOperaciones(c.b);
        Estacion destino=estaciones.buscarActivaPorIdConBloqueo(dto.estacionId).orElseThrow(()->conflicto("La estación está inactiva o fue eliminada."));
        if(bicicletas.countByUbicacionActual(destino)>=destino.getCapacidad()) throw conflicto("La estación de ingreso no tiene espacios libres.");
        LocalDateTime ahora=ahora();String antes=datos(c.i),antesBici=traza.datos(c.b);c.b.setUbicacionActual(destino);
        evento(c.i,"UBICAR_RECUPERADA",null,null,c.actor,dto.motivo.trim(),antes,ahora);
        traza.administracion(c.actor,"BICICLETA",c.b.getId(),"UBICAR_RECUPERADA",antesBici,traza.datos(c.b),dto.motivo.trim(),ahora);
        bicicletas.flush();return salida(c.i);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public IncidenciaDTO perdidaDefinitiva(Integer id,PerdidaDefinitivaDTO dto,boolean eliminar) {
        validar(dto);Contexto c=contexto(id);
        boolean respaldada=historias.existsByIncidenciaIdAndAccion(id,"BAJA_PERDIDA");
        if(eliminar && c.b.isBorradoBici() && respaldada) return salida(c.i);
        if(!eliminar && respaldada && "DE_BAJA".equals(c.b.getEstado().getCodigo())) return salida(c.i);
        versiones(c,dto.version,dto.versionBicicleta);
        if(!"VALIDADA".equals(c.i.getEstadoRevision()) || !Set.of("PERDIDA","ROBO").contains(c.i.getTipo())
                || !"CERRADO_POR_INCIDENCIA".equals(c.d.getEstado().getCodigo())
                || c.d.getIncidenciaCorte()==null || !c.i.getId().equals(c.d.getIncidenciaCorte().getId())
                || c.i.getResueltoPor()==null || c.i.getFechaHoraCorteValidado()==null
                || c.i.getFechaHoraRecepcionFisica()!=null || c.b.getUbicacionActual()!=null || c.b.isBorradoBici())
            throw conflicto("Falta el respaldo de una pérdida validada y cerrada, sin recepción física.");
        sinOperaciones(c.b);
        if(!ordenes.buscarAbiertasPorBicicleta(c.b.getId()).isEmpty() || incidencias.contarPendientesPorBicicleta(c.b.getId())>0)
            throw conflicto("Hay órdenes o incidencias pendientes incompatibles con el retiro definitivo.");
        if(eliminar?(!respaldada || !"DE_BAJA".equals(c.b.getEstado().getCodigo())):!"EXTRAVIADA".equals(c.b.getEstado().getCodigo()))
            throw conflicto(eliminar?"Primero debe existir una baja por pérdida definitiva respaldada.":"La bicicleta debe estar EXTRAVIADA.");
        LocalDateTime ahora=ahora();String antes=datos(c.i),antesBici=traza.datos(c.b);
        if(eliminar) c.b.setBorradoBici(true);else cambiarBici(c,"DE_BAJA",dto.motivo.trim(),ahora);
        evento(c.i,eliminar?"ELIMINAR_PERDIDA":"BAJA_PERDIDA",null,null,c.actor,dto.motivo.trim(),antes,ahora);
        traza.administracion(c.actor,"BICICLETA",c.b.getId(),eliminar?"ELIMINAR_PERDIDA":"BAJA_PERDIDA",antesBici,traza.datos(c.b),
                "Incidencia #"+id+". "+dto.motivo.trim(),ahora);
        bicicletas.flush();return salida(c.i);
    }

    /** Llamada interna desde la devolución autorizada, con cabecera/detalle/bicicleta bloqueados. */
    @Transactional(propagation=Propagation.MANDATORY,readOnly=false)
    public void registrarDevolucion(AlquilerDetalle detalle,Usuario actor,LocalDateTime ahora) {
        for(Incidencia i:incidencias.findByAlquilerDetalleIdOrderByIdAsc(detalle.getId())) {
            if(i.getFechaHoraRecepcionFisica()!=null) continue;
            String antes=datos(i);recepcion(i,detalle.getEstacionDevolucion(),null,actor,ahora);
            i.setOrdenMantenimiento(detalle.getOrdenMantenimiento());
            evento(i,"RECEPCION_DEVOLUCION",null,null,actor,"Recepción real por devolución del detalle.",antes,ahora);
        }
    }

    private void normalizarImportes(AlquilerDetalle d) {
        if(!TERMINALES.contains(d.getEstado().getCodigo())) return;
        if(incidencias.findByAlquilerDetalleIdOrderByIdAsc(d.getId()).stream().anyMatch(i->"PENDIENTE".equals(i.getEstadoRevision())&&i.isRequiereRevisionEconomica())) {
            d.setSubtotalRealUsd(null);d.setSubtotalRealArs(null);return;
        }
        LocalDateTime fin=d.getIncidenciaCorte()==null?d.getFechaHoraDevolucionReal():d.getIncidenciaCorte().getFechaHoraCorteValidado();
        if(fin==null || d.getFechaHoraRetiro()==null || fin.isBefore(d.getFechaHoraRetiro())) throw interno("Falta un fin de uso coherente.");
        d.setFechaHoraFinUso(fin);long segundos=calculadora.calcularSegundosCompletos(d.getFechaHoraRetiro(),fin);
        BigDecimal usd,ars;
        try {
            usd=calculadora.calcularSubtotalUsd(d.getPrecioPorHoraAplicadoUsd(),segundos);
            ars=calculadora.calcularSubtotalArs(d.getPrecioPorHoraAplicadoUsd(),segundos,d.getAlquiler().getCotizacionArsPorUsd());
        } catch(IllegalArgumentException|ArithmeticException ex) {
            throw interno("Los valores congelados del alquiler no son válidos para calcular el uso.");
        }
        if(usd.precision()-usd.scale()>18 || ars.precision()-ars.scale()>18) throw interno("El uso supera la capacidad monetaria del detalle.");
        d.setSubtotalRealUsd(usd);d.setSubtotalRealArs(ars);
    }
    private void cerrarYLiquidar(Alquiler a,LocalDateTime ahora) {
        List<AlquilerDetalle> lista=detalles.buscarPorAlquiler(a.getId());
        if(lista.isEmpty()) throw interno("El alquiler no tiene detalles.");
        if(lista.stream().allMatch(d->TERMINALES.contains(d.getEstado().getCodigo()))) {
            if(a.getFechaHoraCierre()==null) {
                a.setEstadoContratacion(estadosAlquiler.findByCodigo("FINALIZADO").orElseThrow(()->interno("Falta FINALIZADO.")));
                a.setFechaHoraCierre(ahora);
            }
            liquidacion.liquidarSiCorresponde(a);
            if(reembolsos!=null) reembolsos.registrarPendientesSiCorresponde(a);
        }
    }
    private OrdenMantenimiento orden(Bicicleta b,Incidencia i,LocalDateTime ahora) {
        List<OrdenMantenimiento> abiertas=ordenes.buscarAbiertasPorBicicleta(b.getId());
        if(abiertas.size()>1) throw interno("Hay más de una orden abierta para la bicicleta.");
        if(!abiertas.isEmpty()) return abiertas.get(0);
        OrdenMantenimiento o=ordenes.saveAndFlush(new OrdenMantenimiento(b,
                tiposOrden.findByCodigo("CORRECTIVO").orElseThrow(()->interno("Falta CORRECTIVO.")),
                estadosOrden.findByCodigo("CREADA").orElseThrow(()->interno("Falta CREADA.")),ahora));
        historiasOrden.save(new HistorialOrdenMantenimiento(o,"CREAR",null,o.getEstado(),null,"SISTEMA",
                "Revisión por incidencia #"+i.getId()+"; crear la orden no prueba recepción física.",null,"incidencia_id="+i.getId(),ahora));
        return o;
    }
    private void vincularOrden(Incidencia i,OrdenMantenimiento o) {i.setOrdenMantenimiento(o);i.getAlquilerDetalle().setOrdenMantenimiento(o);}
    private void recepcion(Incidencia i,Estacion estacion,String lugar,Usuario actor,LocalDateTime ahora) {
        i.setFechaHoraRecepcionFisica(ahora);i.setEstacionRecepcion(estacion);i.setLugarRecepcion(lugar);i.setRecibidoPor(actor);
    }
    private void cambiarBici(Contexto c,String codigo,String motivo,LocalDateTime ahora) {
        if(codigo.equals(c.b.getEstado().getCodigo())) return;
        EstadoBicicleta anterior=c.b.getEstado();String antes=traza.datos(c.b);
        c.b.setEstado(estadosBici.findByCodigo(codigo).orElseThrow(()->interno("Falta el estado "+codigo)));
        traza.estado(c.b,anterior,c.actor,"Incidencia #"+c.i.getId()+". "+motivo,ahora);
        traza.administracion(c.actor,"BICICLETA",c.b.getId(),"ESTADO_POR_INCIDENCIA",antes,traza.datos(c.b),motivo,ahora);
    }
    private void sinOperaciones(Bicicleta b) {
        if (detalles.contarAsignacionesActivas(b.getId()) > 0) {
            throw conflicto(
                    "La bicicleta tiene una reserva o viaje activo."
            );
        }
    }
    private void evento(Incidencia i,String accion,String anterior,String nuevo,Usuario actor,String motivo,String antes,LocalDateTime ahora) {
        String despues=datos(i);
        historias.save(new HistorialIncidencia(i,accion,anterior,nuevo,actor,"USUARIO",motivo,antes,despues,ahora));
        if(esAdmin(actor)) traza.administracion(actor,"INCIDENCIA",i.getId(),accion,antes,despues,motivo,ahora);
    }
    private String datos(Incidencia i) {
        Map<String,Object> m=new LinkedHashMap<>();AlquilerDetalle d=i.getAlquilerDetalle();
        m.put("detalleId",d.getId());m.put("bicicletaId",d.getBicicleta().getId());m.put("tipo",i.getTipo());m.put("estadoRevision",i.getEstadoRevision());
        m.put("descripcion",i.getDescripcion());m.put("lugarReportado",i.getLugarReportado());m.put("requiereRevisionEconomica",i.isRequiereRevisionEconomica());
        m.put("fechaHoraHechoInformada",cadena(i.getFechaHoraHechoInformada()));m.put("fechaHoraRegistro",cadena(i.getFechaHoraRegistro()));
        m.put("registradoPorId",i.getRegistradoPor().getId());m.put("informanteId",i.getInformante()==null?null:i.getInformante().getId());
        m.put("resueltoPorId",i.getResueltoPor()==null?null:i.getResueltoPor().getId());m.put("motivoDecision",i.getMotivoDecision());
        m.put("fechaHoraDecision",cadena(i.getFechaHoraDecision()));m.put("fechaHoraCorteValidado",cadena(i.getFechaHoraCorteValidado()));
        m.put("fechaHoraRecepcionFisica",cadena(i.getFechaHoraRecepcionFisica()));m.put("estacionRecepcionId",i.getEstacionRecepcion()==null?null:i.getEstacionRecepcion().getId());
        m.put("lugarRecepcion",i.getLugarRecepcion());m.put("recibidoPorId",i.getRecibidoPor()==null?null:i.getRecibidoPor().getId());
        m.put("ordenMantenimientoId",i.getOrdenMantenimiento()==null?null:i.getOrdenMantenimiento().getId());
        m.put("estadoBicicleta",d.getBicicleta().getEstado().getCodigo());m.put("borradoBici",d.getBicicleta().isBorradoBici());
        m.put("estacionActualId",d.getBicicleta().getUbicacionActual()==null?null:d.getBicicleta().getUbicacionActual().getId());
        m.put("estadoDetalle",d.getEstado().getCodigo());m.put("fechaHoraFinUso",cadena(d.getFechaHoraFinUso()));
        m.put("incidenciaCorteId",d.getIncidenciaCorte()==null?null:d.getIncidenciaCorte().getId());
        try{return json.writeValueAsString(m);}catch(JsonProcessingException e){throw interno("No se pudo guardar la historia de incidencia.");}
    }
    private Contexto contexto(Integer id) {
        Usuario actor=cuenta(true);if(!esAdmin(actor)) throw new AccessDeniedException("Se requiere ADMIN vigente.");
        Integer alquilerId=incidencias.buscarAlquilerId(positivo(id)).orElseThrow(()->noExiste("La incidencia no existe."));
        Alquiler a=cabecera(alquilerId);
        Incidencia referencia=incidencias.findById(id).orElseThrow(()->noExiste("La incidencia no existe."));
        AlquilerDetalle d=detalles.buscarPorAlquilerYDetalleConBloqueo(a.getId(),referencia.getAlquilerDetalle().getId()).orElseThrow(()->interno("La incidencia no pertenece a este alquiler."));
        Bicicleta b=bicicleta(d);
        Incidencia i=incidencias.buscarPorIdConBloqueo(id).orElseThrow(()->noExiste("La incidencia no existe."));
        if(!i.getAlquilerDetalle().getId().equals(d.getId())) throw interno("La incidencia cambió de detalle.");
        return new Contexto(i,a,d,b,actor);
    }
    private Incidencia visible(Integer id,Usuario actor) {
        Incidencia i=incidencias.findById(positivo(id)).orElseThrow(()->noExiste("La incidencia no existe."));propietario(i.getAlquilerDetalle().getAlquiler(),actor);return i;
    }
    private IncidenciaDTO salida(Incidencia i) {return salida(i,true);}
    private IncidenciaDTO salida(Incidencia i,boolean administrativa) {return new IncidenciaDTO(i,historias.existsByIncidenciaIdAndAccion(i.getId(),"BAJA_PERDIDA"),administrativa);}
    private Alquiler cabecera(Integer id) {
        Alquiler a=alquileres.buscarPorIdConBloqueo(positivo(id)).orElseThrow(()->noExiste("El alquiler no existe."));
        if(a.getEstadoContratacion()==null) throw conflicto("Este alquiler pertenece al circuito anterior.");return a;
    }
    // Permite leer el respaldo de una eliminada; las operaciones comprueban su estado.
    private Bicicleta bicicleta(AlquilerDetalle d) {return bicicletas.buscarPorIdConBloqueo(d.getBicicleta().getId()).orElseThrow(()->noExiste("La bicicleta no existe."));}
    private void propietario(Alquiler a,Usuario actor) {
        if(!esAdmin(actor) && (a.getUsuario()==null || a.getUsuario().getId()!=actor.getId())) throw new AccessDeniedException("No podés acceder a incidencias de otro cliente.");
    }
    private Usuario cuenta(boolean bloqueo) {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated()) throw new AccessDeniedException("Debe iniciar sesión.");
        Usuario actor=(bloqueo?usuarios.buscarActivoPorEmailConBloqueo(auth.getName()):usuarios.buscarPorEmailConDatosYRoles(auth.getName()))
                .filter(Usuario::isActivo).orElseThrow(()->new AccessDeniedException("La cuenta no está activa."));
        if(!rol(actor,"ADMIN")&&!rol(actor,"CLIENTE")) throw new AccessDeniedException("Se requiere CLIENTE o ADMIN vigente.");return actor;
    }
    private boolean esAdmin(Usuario u) {return rol(u,"ADMIN");}
    private boolean rol(Usuario u,String codigo) {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        return u.isActivo()&&auth!=null&&auth.getAuthorities().stream().anyMatch(a->codigo.equals(a.getAuthority()))
                &&u.getUsuarioRoles().stream().anyMatch(ur->codigo.equals(ur.getRol().getCodigo()));
    }
    private void versiones(Contexto c,Integer version,Integer versionBici) {version(version,c.i.getVersion(),"La incidencia");version(versionBici,c.b.getVersion(),"La bicicleta");}
    private void version(Integer enviada,Integer actual,String entidad) {if(enviada==null||!enviada.equals(actual)) throw conflicto(entidad+" fue modificada. Recargá los datos.");}
    private void validar(Object dto) {if(dto==null||!validator.validate(dto).isEmpty()) throw malo("Revisá los datos obligatorios, formato, longitudes y versiones.");}
    private Integer positivo(Integer id){if(id==null||id<=0)throw malo("Identificador inválido.");return id;}
    private EstadoAlquilerDetalle estadoDetalle(String c){return estadosDetalle.findByCodigo(c).orElseThrow(()->interno("Falta "+c));}
    private LocalDateTime ahora(){return LocalDateTime.ofInstant(reloj.instant(),ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MICROS);}
    private LocalDateTime utc(OffsetDateTime fecha){return LocalDateTime.ofInstant(fecha.toInstant(),ZoneOffset.UTC).truncatedTo(java.time.temporal.ChronoUnit.MICROS);}
    private String cadena(LocalDateTime d){return d==null?null:d.toString();}
    private String limpiar(String s){return s==null||s.isBlank()?null:s.trim();}
    private ResponseStatusException malo(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
    private ResponseStatusException conflicto(String s){return new ResponseStatusException(HttpStatus.CONFLICT,s);}
    private ResponseStatusException noExiste(String s){return new ResponseStatusException(HttpStatus.NOT_FOUND,s);}
    private ConfiguracionSistemaException interno(String s){return new ConfiguracionSistemaException(s);}
    private static class Contexto {
        final Incidencia i;final Alquiler a;final AlquilerDetalle d;final Bicicleta b;final Usuario actor;
        Contexto(Incidencia i,Alquiler a,AlquilerDetalle d,Bicicleta b,Usuario actor){this.i=i;this.a=a;this.d=d;this.b=b;this.actor=actor;}
    }
}
