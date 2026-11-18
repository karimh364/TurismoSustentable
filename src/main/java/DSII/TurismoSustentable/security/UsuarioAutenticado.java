package DSII.TurismoSustentable.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public final class UsuarioAutenticado extends User {

    private static final long serialVersionUID = 1L;

    private final int usuarioId;

    public UsuarioAutenticado(
            int usuarioId,
            String email,
            String passwordHash,
            boolean activo,
            Collection<? extends GrantedAuthority> autoridades
    ) {
        super(
                email,
                passwordHash,
                activo,
                true,
                true,
                true,
                autoridades
        );

        if (usuarioId <= 0) {
            throw new IllegalArgumentException(
                    "El usuario autenticado debe tener un ID válido."
            );
        }

        this.usuarioId = usuarioId;
    }

    public int getUsuarioId() {
        return usuarioId;
    }
}