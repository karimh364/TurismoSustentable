package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Validated
@Transactional(readOnly = true)
public class BicicletaService {
    private final BicicletaRepository bicicletas;
    private final TipoBicicletaRepository tipos;
    private final EstadoBicicletaRepository estados;
    private final EstacionRepository estaciones;
    private final AlquilerRepository alquileres;
    private final UsuarioRepository usuarios;
    private final TrazabilidadInventarioService trazabilidad;

    public BicicletaService(BicicletaRepository bicicletas, TipoBicicletaRepository tipos,
                            EstadoBicicletaRepository estados, EstacionRepository estaciones,
                            AlquilerRepository alquileres, UsuarioRepository usuarios,
                            TrazabilidadInventarioService trazabilidad) {
        this.bicicletas = bicicletas; this.tipos = tipos; this.estados = estados;
        this.estaciones = estaciones; this.alquileres = alquileres;
        this.usuarios = usuarios; this.trazabilidad = trazabilidad;
    }

    public List<BicicletaDTO> listar(boolean incluirEliminadas) {
        // Esta consulta es administrativa; las eliminadas se incluyen solo a pedido.
        List<Bicicleta> resultado = incluirEliminadas
                ? bicicletas.findAll() : bicicletas.findByBorradoBiciFalse();
        return resultado.stream().map(BicicletaDTO::new).collect(Collectors.toList());
    }
    public List<BicicletaDTO> disponibles() {
        return bicicletas.buscarPorEstadoEnEstacionesPublicas("DISPONIBLE")
                .stream().map(BicicletaDTO::new).collect(Collectors.toList());
    }
    public List<BicicletaDTO> porTipo(String codigo) {
        return bicicletas.findByBorradoBiciFalseAndTipo_Codigo(codigo.toUpperCase(Locale.ROOT))
                .stream().map(BicicletaDTO::new).collect(Collectors.toList());
    }
    public BicicletaDTO obtener(Integer id) {
        return new BicicletaDTO(bicicletas.findById(id).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "La bicicleta no existe.")));
    }
    public List<TipoBicicletaDTO> listarTipos() {
        return tipos.findAll().stream().map(TipoBicicletaDTO::new).collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BicicletaDTO crear(@Valid CrearBicicletaDTO dto) {
        Usuario admin = administrador();
        comprobarCodigo(dto.getCodigoBicicleta(), 0);
        TipoBicicleta tipo = tipo(dto.getTipoBicicletaId());
        EstadoBicicleta disponible = estado("DISPONIBLE");
        Estacion destino = destino(dto.getEstacionId());
        comprobarEspacio(destino);
        Bicicleta bicicleta = new Bicicleta(dto.getCodigoBicicleta(), tipo, disponible, destino);
        bicicleta.setFechaAlta(LocalDate.now());
        bicicletas.saveAndFlush(bicicleta);
        trazabilidad.estado(bicicleta, null, admin, "Alta de inventario.");
        auditar(admin, bicicleta, "CREAR", null, "Alta de inventario.");
        return new BicicletaDTO(bicicleta);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BicicletaDTO actualizar(Integer id, @Valid ActualizarBicicletaDTO dto) {
        Usuario admin = administrador();
        Bicicleta bicicleta = activa(id, dto.getVersion());
        comprobarEditable(bicicleta);
        comprobarCodigo(dto.getCodigoBicicleta(), id);
        TipoBicicleta tipo = tipo(dto.getTipoBicicletaId());
        Estacion destino = bicicleta.getUbicacionActual();
        if (dto.getEstacionId() != null && (destino == null
                || destino.getId() != dto.getEstacionId())) {
            exigirMotivo(dto.getMotivo());
            destino = destino(dto.getEstacionId());
            comprobarEspacio(destino);
        }
        String anterior = trazabilidad.datos(bicicleta);
        bicicleta.setCodigoBicicleta(dto.getCodigoBicicleta());
        bicicleta.setTipo(tipo);
        bicicleta.setUbicacionActual(destino);
        String nuevo = trazabilidad.datos(bicicleta);
        if (!Objects.equals(anterior, nuevo)) {
            bicicletas.saveAndFlush(bicicleta);
            auditar(admin, bicicleta, "ACTUALIZAR", anterior, dto.getMotivo());
        }
        return new BicicletaDTO(bicicleta);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BicicletaDTO cambiarEstado(Integer id, String codigo, @Valid OperacionBicicletaDTO dto) {
        Usuario admin = administrador();
        Bicicleta bicicleta = activa(id, dto.getVersion());
        comprobarEditable(bicicleta);
        exigirMotivo(dto.getMotivo());
        String actual = bicicleta.getEstado().getCodigo();
        // Viajes, reservas, pérdidas y mantenimiento pertenecen a sus flujos específicos.
        boolean cambioOperativo = ("DISPONIBLE".equals(actual) && "INACTIVA".equals(codigo))
                || ("INACTIVA".equals(actual) && "DISPONIBLE".equals(codigo));
        boolean baja = "DE_BAJA".equals(codigo);
        if (!cambioOperativo && !baja) {
            throw error(HttpStatus.CONFLICT,
                    "Ese cambio de estado debe realizarse desde su proceso específico.");
        }
        if (baja) exigirTaller(bicicleta);
        if ("DISPONIBLE".equals(codigo)) {
            if (bicicleta.getUbicacionActual() == null) {
                throw error(HttpStatus.CONFLICT, "La bicicleta no tiene una ubicación física registrada.");
            }
            destino(bicicleta.getUbicacionActual().getId());
        }
        EstadoBicicleta nuevo = estado(codigo);
        EstadoBicicleta anteriorEstado = bicicleta.getEstado();
        String anterior = trazabilidad.datos(bicicleta);
        bicicleta.setEstado(nuevo);
        bicicletas.saveAndFlush(bicicleta);
        trazabilidad.estado(bicicleta, anteriorEstado, admin, dto.getMotivo());
        auditar(admin, bicicleta, "CAMBIAR_ESTADO", anterior, dto.getMotivo());
        return new BicicletaDTO(bicicleta);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void eliminarLogico(Integer id, @Valid EliminarBicicletaDTO dto) {
        Usuario admin = administrador();
        Bicicleta bicicleta = activa(id, dto.getVersion());
        if (!"DE_BAJA".equals(bicicleta.getEstado().getCodigo())) comprobarEditable(bicicleta);
        else comprobarSinAlquiler(bicicleta);
        if (!Boolean.TRUE.equals(dto.getSalidaFisicaConfirmada())) {
            throw error(HttpStatus.BAD_REQUEST,
                    "Debe confirmar la salida física definitiva de la bicicleta.");
        }
        exigirMotivo(dto.getMotivo());
        exigirTaller(bicicleta);
        EstadoBicicleta anteriorEstado = bicicleta.getEstado();
        boolean cambiaEstado = !"DE_BAJA".equals(anteriorEstado.getCodigo());
        // Resolver el catálogo antes de mutar; no inventar estados si falta configuración.
        EstadoBicicleta baja = cambiaEstado ? estado("DE_BAJA") : anteriorEstado;
        String anterior = trazabilidad.datos(bicicleta);
        bicicleta.setEstado(baja);
        bicicleta.setBorradoBici(true);
        // Eliminar confirma una retirada real; dar de baja por separado NO vacía ubicación.
        bicicleta.setUbicacionActual(null);
        bicicletas.saveAndFlush(bicicleta);
        if (cambiaEstado) {
            trazabilidad.estado(bicicleta, anteriorEstado, admin, dto.getMotivo());
        }
        auditar(admin, bicicleta, "ELIMINAR_SALIDA_DEFINITIVA", anterior, dto.getMotivo());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TipoBicicletaDTO actualizarTarifa(Integer id, @Valid ActualizarTarifaTipoDTO dto) {
        Usuario admin = administrador();
        TipoBicicleta tipo = tipos.buscarPorIdConBloqueo(id).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "El tipo de bicicleta no existe."));
        if (tipo.getPrecioPorHoraUsd().compareTo(dto.getPrecioAnterior()) != 0) {
            throw error(HttpStatus.CONFLICT, "La tarifa fue modificada. Recargá los datos.");
        }
        String anterior = tipo.getPrecioPorHoraUsd().toPlainString();
        if (tipo.getPrecioPorHoraUsd().compareTo(dto.getPrecioPorHoraUsd()) != 0) {
            tipo.setPrecioPorHoraUsd(dto.getPrecioPorHoraUsd());
            tipos.saveAndFlush(tipo);
            trazabilidad.administracion(admin, "TIPO_BICICLETA", id, "ACTUALIZAR_TARIFA",
                    anterior, tipo.getPrecioPorHoraUsd().toPlainString(), null);
        }
        return new TipoBicicletaDTO(tipo);
    }

    private Bicicleta activa(Integer id, Integer version) {
        Bicicleta bicicleta = bicicletas.buscarActivaPorIdConBloqueo(id).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "La bicicleta no existe o fue eliminada."));
        if (!Objects.equals(version, bicicleta.getVersion())) {
            throw error(HttpStatus.CONFLICT, "La bicicleta fue modificada. Recargá los datos.");
        }
        return bicicleta;
    }
    private void comprobarEditable(Bicicleta bicicleta) {
        comprobarSinAlquiler(bicicleta);
        String codigo = bicicleta.getEstado().getCodigo();
        if (!"DISPONIBLE".equals(codigo) && !"INACTIVA".equals(codigo)) {
            throw error(HttpStatus.CONFLICT,
                    "La bicicleta tiene un proceso activo o un estado no editable desde inventario.");
        }
    }
    private void comprobarSinAlquiler(Bicicleta bicicleta) {
        if (alquileres.existsByBicicletaAndEstado(bicicleta, EstadoAlquiler.INICIADO)) {
            throw error(HttpStatus.CONFLICT,
                    "La bicicleta tiene un alquiler activo. Debe finalizarse antes de editarla.");
        }
    }
    private void comprobarCodigo(String codigo, int id) {
        if (bicicletas.existsByCodigoBicicletaIgnoreCaseAndIdNot(codigo, id)) {
            throw error(HttpStatus.CONFLICT, "El código de bicicleta ya está registrado.");
        }
    }
    private TipoBicicleta tipo(Integer id) {
        return tipos.findById(id).orElseThrow(() ->
                error(HttpStatus.BAD_REQUEST, "Debe seleccionar un tipo de bicicleta válido."));
    }
    private EstadoBicicleta estado(String codigo) {
        return estados.findByCodigo(codigo).orElseThrow(() ->
                new ConfiguracionSistemaException("Falta el estado de bicicleta: " + codigo));
    }
    private Estacion destino(Integer id) {
        return estaciones.buscarActivaPorIdConBloqueo(id).orElseThrow(() ->
                error(HttpStatus.BAD_REQUEST, "La estación no existe o está inactiva."));
    }
    private void comprobarEspacio(Estacion estacion) {
        if (bicicletas.countByUbicacionActual(estacion) >= estacion.getCapacidad()) {
            throw error(HttpStatus.CONFLICT, "La estación de destino no tiene espacios libres.");
        }
    }
    private void exigirTaller(Bicicleta bicicleta) {
        Estacion ubicacion = bicicleta.getUbicacionActual();
        if (ubicacion == null) {
            throw error(HttpStatus.CONFLICT, "La bicicleta debe estar en una estación/taller interno habilitado.");
        }
        Estacion taller = destino(ubicacion.getId());
        if (taller.isEsPublica()) {
            throw error(HttpStatus.CONFLICT,
                    "Primero debe trasladar la bicicleta a una estación/taller interno habilitado.");
        }
    }
    private void exigirMotivo(String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, "Debe indicar el motivo de la operación.");
        }
    }
    private Usuario administrador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("La operación requiere un administrador.");
        }
        return usuarios.findByEmail(auth.getName()).filter(Usuario::isActivo)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no está habilitada."));
    }
    private void auditar(Usuario admin, Bicicleta bicicleta, String accion, String anterior, String motivo) {
        trazabilidad.administracion(admin, "BICICLETA", bicicleta.getId(), accion,
                anterior, trazabilidad.datos(bicicleta), motivo);
    }
    private ResponseStatusException error(HttpStatus status, String mensaje) {
        return new ResponseStatusException(status, mensaje);
    }
}
