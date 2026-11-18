package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.models.Persona;
import DSII.TurismoSustentable.models.Usuario;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Auditoría de estaciones y cambios de usuarios, sin serializar entidades completas. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class AuditoriaAdministrativaService {

    private final UsuarioRepository usuarios;
    private final TrazabilidadInventarioService trazabilidad;
    private final ObjectMapper json;
    private final Clock reloj;

    public AuditoriaAdministrativaService(
            UsuarioRepository usuarios,
            TrazabilidadInventarioService trazabilidad,
            ObjectMapper json,
            Clock reloj
    ) {
        this.usuarios = usuarios;
        this.trazabilidad = trazabilidad;
        this.json = json;
        this.reloj = reloj;
    }

    // Capturar antes de editar: el administrador puede modificar su propio email.
    public Usuario administradorActual() {
        Authentication sesion = sesionAdministrativa();
        Usuario administrador = usuarios.buscarPorEmailConDatosYRoles(sesion.getName())
                .orElseThrow(() -> new AccessDeniedException("La cuenta no existe."));
        validarAdministrador(administrador);
        return administrador;
    }

    public String datos(Estacion estacion) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", estacion.getId());
        datos.put("nombre", estacion.getNombre());
        datos.put("direccion", estacion.getDireccion());
        datos.put("capacidad", estacion.getCapacidad());
        datos.put("latitud", estacion.getLatitud());
        datos.put("longitud", estacion.getLongitud());
        datos.put("esPublica", estacion.isEsPublica());
        datos.put("borradoEstacion", estacion.isBorradoEstacion());
        return serializar(datos);
    }

    public String datos(Usuario usuario) {
        Persona persona = usuario.getPersona();
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", usuario.getId());
        datos.put("email", usuario.getEmail());
        datos.put("nombre", persona == null ? null : persona.getNombre());
        datos.put("apellido", persona == null ? null : persona.getApellido());
        datos.put("fechaNacimiento", persona == null || persona.getFechaNacimiento() == null
                ? null : persona.getFechaNacimiento().toString());
        datos.put("generoId", persona == null || persona.getGenero() == null
                ? null : persona.getGenero().getId());
        return serializar(datos);
    }

    // El actor proviene de administradorActual(), nunca del cuerpo de la petición.
    public void registrar(Usuario administrador, String entidad, int id, String accion,
                          String anteriores, String nuevos) {
        sesionAdministrativa();
        validarAdministrador(administrador);
        if (Objects.equals(anteriores, nuevos)) return;

        trazabilidad.administracion(administrador, entidad, id, accion,
                anteriores, nuevos, null,
                LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC));
    }

    // Mantiene el formato histórico y la misma fecha que las órdenes afectadas.
    public void registrarInactivacionUsuario(Usuario administrador, int usuarioId,
                                            String motivo, LocalDateTime fecha) {
        sesionAdministrativa();
        validarAdministrador(administrador);
        trazabilidad.administracion(administrador, "USUARIO", usuarioId, "INACTIVAR",
                "activo=true", "activo=false", motivo, fecha);
    }

    private Authentication sesionAdministrativa() {
        Authentication sesion = SecurityContextHolder.getContext().getAuthentication();
        if (sesion == null || !sesion.isAuthenticated()
                || sesion.getAuthorities().stream().noneMatch(a -> "ADMIN".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Se requiere una sesión con permiso ADMIN.");
        }
        return sesion;
    }

    private void validarAdministrador(Usuario administrador) {
        if (administrador == null || !administrador.isActivo()
                || administrador.getUsuarioRoles().stream()
                .noneMatch(ur -> "ADMIN".equals(ur.getRol().getCodigo()))) {
            throw new AccessDeniedException("Se requiere una cuenta activa con rol ADMIN vigente.");
        }
    }

    private String serializar(Map<String, Object> datos) {
        try {
            return json.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new ConfiguracionSistemaException("No se pudo preparar la auditoría administrativa.");
        }
    }
}
