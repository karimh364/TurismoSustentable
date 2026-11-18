package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.ActualizarRolesUsuarioDTO;
import DSII.TurismoSustentable.dto.RolesUsuarioDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.util.*;
import java.util.stream.Collectors;

/** Gestión de USUARIO_ROL; independiente de la edición de Persona y mantenimiento. */
@Service
@PreAuthorize("hasAuthority('ADMIN')")
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public class RolesUsuarioService {
    private static final Set<String> PERMITIDOS = Set.of("ADMIN", "CLIENTE", "TECNICO");
    private final UsuarioRepository usuarios;
    private final UsuarioRolRepository asignaciones;
    private final RolRepository catalogo;
    private final AlquilerRepository alquileres;
    private final OrdenMantenimientoRepository ordenes;
    private final AuditoriaAdministrativaService auditoria;
    private final EntityManager em;

    public RolesUsuarioService(UsuarioRepository usuarios, UsuarioRolRepository asignaciones,
                               RolRepository catalogo, AlquilerRepository alquileres,
                               OrdenMantenimientoRepository ordenes,
                               AuditoriaAdministrativaService auditoria, EntityManager em) {
        this.usuarios = usuarios;
        this.asignaciones = asignaciones;
        this.catalogo = catalogo;
        this.alquileres = alquileres;
        this.ordenes = ordenes;
        this.auditoria = auditoria;
        this.em = em;
    }

    public RolesUsuarioDTO consultar(Integer id) {
        positivo(id);
        Authentication sesion = sesionAdmin();
        Integer actorId = identificar(sesion);
        Usuario actor = usuarios.buscarPorIdConDatosYRoles(actorId).orElseThrow(this::sinCuenta);
        validarActor(actor, sesion);
        Usuario objetivo = id.equals(actorId) ? actor
                : usuarios.buscarPorIdConDatosYRoles(id).orElseThrow(this::noExiste);
        validarObjetivo(objetivo);
        return new RolesUsuarioDTO(objetivo);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public RolesUsuarioDTO actualizar(Integer id, ActualizarRolesUsuarioDTO datos) {
        positivo(id);
        validarSolicitud(datos);
        Authentication sesion = sesionAdmin();
        Integer actorId = identificar(sesion);

        // Todos los cambios de roles toman primero este mismo cerrojo del catálogo.
        // Serializa el conteo del último ADMIN sin bloquear una colección variable.
        Rol rolAdmin = catalogo.buscarPorCodigoConBloqueo("ADMIN")
                .orElseThrow(() -> new ConfiguracionSistemaException("Falta el rol ADMIN."));
        SortedSet<Integer> ids = new TreeSet<>(List.of(actorId, id));
        Map<Integer, Usuario> cuentas = new HashMap<>();
        for (Integer cuentaId : ids) {
            cuentas.put(cuentaId, usuarios.buscarCuentaParaRolesConBloqueo(cuentaId)
                    .orElseThrow(this::noExiste));
        }
        Usuario actor = cuentas.get(actorId);
        Usuario objetivo = cuentas.get(id);
        validarActor(actor, sesion);
        validarObjetivo(objetivo);
        if (!objetivo.getVersion().equals(datos.getVersion())) {
            throw conflicto("El usuario fue modificado. Recargá los roles antes de guardar.");
        }
        Set<String> antes = codigos(objetivo);
        Set<String> nuevos = new TreeSet<>(datos.getRoles());
        if (antes.equals(nuevos)) return new RolesUsuarioDTO(objetivo);

        if (antes.contains("ADMIN") && !nuevos.contains("ADMIN")
                && usuarios.contarAdministradoresActivos() <= 1) {
            throw conflicto("No se puede retirar ADMIN al último administrador activo. Asigná otro primero.");
        }
        if (antes.contains("CLIENTE") && !nuevos.contains("CLIENTE")
                && (alquileres.contarOperacionesPendientesParaRetirarCliente(id) > 0
                    || alquileres.contarLiquidacionesSinSaldar(id) > 0)) {
            throw conflicto("No se puede retirar CLIENTE: hay reservas, viajes o gestiones financieras pendientes.");
        }
        if (antes.contains("TECNICO") && !nuevos.contains("TECNICO")
                && !ordenes.buscarBicicletasAbiertasDeTecnico(id).isEmpty()) {
            throw conflicto("No se puede retirar TECNICO: primero finalizá o reasigná sus órdenes abiertas.");
        }
        Map<String, Rol> rolesNuevos = new HashMap<>();
        for (String codigo : nuevos) {
            rolesNuevos.put(codigo, "ADMIN".equals(codigo) ? rolAdmin
                    : catalogo.findByCodigo(codigo).orElseThrow(() ->
                        new ConfiguracionSistemaException("Falta un rol requerido en el catálogo.")));
        }

        // Auditar antes de retirar ADMIN al propio actor; el commit sigue siendo único.
        auditoria.registrarCambioRoles(actor, id, antes, nuevos, datos.getMotivo());
        // Cambiar la tabla intermedia por sí solo no incrementa @Version de Usuario.
        em.lock(objetivo, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        Iterator<UsuarioRol> it = objetivo.getUsuarioRoles().iterator();
        while (it.hasNext()) {
            UsuarioRol asignacion = it.next();
            if (!nuevos.contains(asignacion.getRol().getCodigo())) {
                asignaciones.delete(asignacion);
                it.remove();
            }
        }
        for (String codigo : nuevos) {
            if (!antes.contains(codigo)) {
                UsuarioRol asignacion = asignaciones.save(new UsuarioRol(objetivo, rolesNuevos.get(codigo)));
                objetivo.getUsuarioRoles().add(asignacion);
            }
        }
        asignaciones.flush();
        usuarios.flush();
        return new RolesUsuarioDTO(objetivo);
    }

    private Set<String> codigos(Usuario usuario) {
        return usuario.getUsuarioRoles().stream().map(ur -> ur.getRol().getCodigo())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void validarSolicitud(ActualizarRolesUsuarioDTO datos) {
        if (datos == null || datos.getVersion() == null || datos.getVersion() < 0
                || datos.getRoles() == null || datos.getRoles().isEmpty() || datos.getRoles().size() > 3
                || datos.getRoles().stream().anyMatch(r -> !PERMITIDOS.contains(r == null ? "" : r))
                || new HashSet<>(datos.getRoles()).size() != datos.getRoles().size()
                || datos.getMotivo() == null || datos.getMotivo().isBlank() || datos.getMotivo().length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indicá versión, motivo de hasta 2000 caracteres y al menos un rol válido, sin repetir.");
        }
    }

    private Authentication sesionAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()
                || a.getAuthorities().stream().noneMatch(r -> "ADMIN".equals(r.getAuthority()))) {
            throw sinCuenta();
        }
        return a;
    }

    private Integer identificar(Authentication a) {
        return usuarios.buscarIdPorEmail(a.getName()).orElseThrow(this::sinCuenta);
    }

    private void validarActor(Usuario actor, Authentication sesion) {
        if (!actor.isActivo() || !actor.getEmail().equals(sesion.getName()) || !codigos(actor).contains("ADMIN")) {
            throw sinCuenta();
        }
    }

    private void validarObjetivo(Usuario usuario) {
        if (!usuario.isActivo()) throw conflicto("No se pueden cambiar roles de una cuenta inactiva.");
        if (usuario.getVersion() == null || !PERMITIDOS.containsAll(codigos(usuario))) {
            throw new ConfiguracionSistemaException("La cuenta tiene una configuración de roles inválida.");
        }
    }

    private void positivo(Integer id) {
        if (id == null || id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario no es válido.");
    }
    private AccessDeniedException sinCuenta() { return new AccessDeniedException("Se requiere ADMIN activo y vigente."); }
    private ResponseStatusException noExiste() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe."); }
    private ResponseStatusException conflicto(String mensaje) { return new ResponseStatusException(HttpStatus.CONFLICT, mensaje); }
}
