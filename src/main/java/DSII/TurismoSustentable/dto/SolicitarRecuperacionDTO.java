package DSII.TurismoSustentable.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Locale;

public class SolicitarRecuperacionDTO {

    @NotBlank(message = "El email no puede estar vacío.")
    @Size(max = 254, message = "El email no puede superar los 254 caracteres.")
    @Email(
            regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Ingresá una dirección de correo válida."
    )
    private String email;

    public SolicitarRecuperacionDTO() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null
                ? null
                : email.trim().toLowerCase(Locale.ROOT);
    }
}
