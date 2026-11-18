package DSII.TurismoSustentable.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.validation.constraints.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ActualizarRolesUsuarioDTO {
    @NotNull(message = "La versión es obligatoria.")
    @PositiveOrZero(message = "La versión no es válida.")
    private Integer version;

    @NotEmpty(message = "La cuenta debe conservar al menos un rol.")
    @Size(max = 3, message = "Solo se admiten los tres roles del sistema.")
    private List<@NotBlank @Pattern(regexp = "CLIENTE|ADMIN|TECNICO",
            message = "El rol debe ser CLIENTE, ADMIN o TECNICO.") String> roles;

    @NotBlank(message = "El motivo es obligatorio.")
    @Size(max = 2000, message = "El motivo admite hasta 2000 caracteres.")
    private String motivo;

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles == null ? null : new ArrayList<>(roles); }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo == null ? null : motivo.trim(); }

    @AssertTrue(message = "No se puede repetir un rol.")
    @JsonIgnore
    public boolean isRolesSinRepetir() {
        return roles == null || new HashSet<>(roles).size() == roles.size();
    }

    @JsonAnySetter
    public void rechazarCampo(String campo, Object valor) {
        throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");
    }
}
