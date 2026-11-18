package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.ActualizarUsuarioDTO;
import DSII.TurismoSustentable.dto.UsuarioDTO;
import DSII.TurismoSustentable.models.Genero;
import DSII.TurismoSustentable.models.Persona;
import DSII.TurismoSustentable.models.Usuario;
import DSII.TurismoSustentable.repositories.GeneroRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.LockModeType;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
@Validated
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final GeneroRepository generoRepository;
    private final EntityManager entityManager;
    private final AuditoriaAdministrativaService auditoria;
    private final MantenimientoService mantenimiento;
    private final Clock reloj;

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void inactivarUsuario(Integer id, String motivo) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador no es válido.");
        }
        String razon = motivo == null ? null : motivo.trim();
        if (razon != null && razon.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo admite hasta 2000 caracteres.");
        }

        Authentication sesion = SecurityContextHolder.getContext().getAuthentication();
        if (sesion == null || !sesion.isAuthenticated()
                || sesion.getAuthorities().stream().noneMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Se requiere una sesión con permiso ADMIN.");
        }
        Integer actorId = usuarioRepository.buscarIdPorEmail(sesion.getName())
                .orElseThrow(() -> new AccessDeniedException("La cuenta no existe."));

        // Mismo orden que mantenimiento: cuentas por ID, luego bicicletas y órdenes.
        SortedSet<Integer> ids = new TreeSet<>();
        ids.add(actorId);
        ids.add(id);
        Map<Integer, Usuario> cuentas = new TreeMap<>();
        for (Integer cuentaId : ids) {
            cuentas.put(cuentaId, usuarioRepository.buscarPorIdConBloqueo(cuentaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe.")));
        }
        Usuario administrador = cuentas.get(actorId);
        if (!administrador.isActivo() || !sesion.getName().equals(administrador.getEmail())
                || !tieneRol(administrador, "ADMIN")) {
            throw new AccessDeniedException("Se requiere una cuenta activa con rol ADMIN vigente.");
        }

        Usuario objetivo = cuentas.get(id);
        if (tieneRol(objetivo, "ADMIN")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede inactivar a un usuario Administrador.");
        }
        if (!objetivo.isActivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya se encuentra inactivo.");
        }

        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        mantenimiento.prepararOrdenesPorInactivacion(objetivo, administrador, razon, ahora);
        objetivo.setActivo(false);
        usuarioRepository.flush();
        auditoria.registrarInactivacionUsuario(administrador, objetivo.getId(), razon, ahora);
    }

    private boolean tieneRol(Usuario usuario, String codigo) {
        return usuario.getUsuarioRoles().stream()
                .anyMatch(ur -> codigo.equals(ur.getRol().getCodigo()));
    }

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            GeneroRepository generoRepository,
            EntityManager entityManager,
            AuditoriaAdministrativaService auditoria,
            MantenimientoService mantenimiento,
            Clock reloj
    ) {
        this.usuarioRepository = usuarioRepository;
        this.generoRepository = generoRepository;
        this.entityManager = entityManager;
        this.auditoria = auditoria;
        this.mantenimiento = mantenimiento;
        this.reloj = reloj;
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public UsuarioDTO actualizarUsuario(
            @NotNull(message = "El identificador del usuario es obligatorio.")
            @Positive(message = "El identificador del usuario debe ser positivo.")
            Integer id,

            @NotNull(message = "Los datos de actualización son obligatorios.")
            @Valid ActualizarUsuarioDTO datos
    ) {
        Usuario administrador = auditoria.administradorActual();
        Usuario usuario = usuarioRepository.findById(id)
                .filter(Usuario::isActivo)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "El usuario no existe o está inactivo."
                        )
                );

        Persona persona = usuario.getPersona();

        if (persona == null) {
            throw new IllegalStateException(
                    "El usuario no tiene una persona asociada."
            );
        }

        Genero genero = generoRepository.findById(datos.getGeneroId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El género seleccionado no existe."
                        )
                );

        boolean emailOcupado =
                usuarioRepository.existsByEmailIgnoreCaseAndIdNot(
                        datos.getEmail(),
                        id
                );

        if (emailOcupado) {
            throw new IllegalStateException(
                    "Ya existe otro usuario registrado con ese email."
            );
        }

        String anteriores = auditoria.datos(usuario);

        // Controlar concurrencia incluso si solo cambia Persona.
        entityManager.lock(
                usuario,
                LockModeType.OPTIMISTIC_FORCE_INCREMENT
        );

        // El email pertenece a la cuenta.
        if (!usuario.getEmail().equals(datos.getEmail())) {
            usuario.setEmail(datos.getEmail());

            // Invalidar enlaces anteriores si cambia el email.
            usuario.setTokenRecuperacion(null);
            usuario.setVencimientoTokenRecuperacion(null);
        }

        // Los datos personales pertenecen a Persona.
        persona.setNombre(datos.getNombre());
        persona.setApellido(datos.getApellido());
        persona.setFechaNacimiento(datos.getFechaNacimiento());
        persona.setGenero(genero);

        // Persistir los cambios de las entidades administradas.
        usuarioRepository.flush();

        auditoria.registrar(administrador, "USUARIO", usuario.getId(),
                "ACTUALIZAR", anteriores, auditoria.datos(usuario));

        return new UsuarioDTO(usuario);
    }
}
