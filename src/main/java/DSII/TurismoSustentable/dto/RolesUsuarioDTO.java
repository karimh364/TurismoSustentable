package DSII.TurismoSustentable.dto;

import DSII.TurismoSustentable.models.Usuario;
import java.util.List;
import java.util.stream.Collectors;

/** Vista específica de permisos; no serializa Persona, credenciales ni entidades JPA. */
public class RolesUsuarioDTO {
    private final int usuarioId;
    private final String nickname;
    private final Integer version;
    private final List<String> roles;

    public RolesUsuarioDTO(Usuario usuario) {
        usuarioId = usuario.getId();
        nickname = usuario.getNickname();
        version = usuario.getVersion();
        roles = usuario.getUsuarioRoles().stream().map(ur -> ur.getRol().getCodigo())
                .sorted().collect(Collectors.toUnmodifiableList());
    }

    public int getUsuarioId() { return usuarioId; }
    public String getNickname() { return nickname; }
    public Integer getVersion() { return version; }
    public List<String> getRoles() { return roles; }
}
