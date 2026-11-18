package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.models.Usuario;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import org.springframework.security.core.authority.AuthorityUtils;
import DSII.TurismoSustentable.security.UsuarioAutenticado;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nickname) {

        if (nickname == null || nickname.trim().isEmpty()) {
            throw new UsernameNotFoundException(
                    "Usuario desconocido."
            );
        }

        String nicknameNormalizado =
                nickname.trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository
                .findByNicknameIgnoreCase(nicknameNormalizado)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario desconocido."
                ));

        String[] autoridades = usuario.getUsuarioRoles().stream()
                .map(asignacion -> asignacion.getRol().getCodigo())
                .distinct()
                .toArray(String[]::new);

        if (autoridades.length == 0) {
            throw new UsernameNotFoundException(
                    "La cuenta no tiene roles asignados."
            );
        }

        // Conservamos el email utilizado por los servicios y añadimos el ID estable.
        return new UsuarioAutenticado(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPassword(),
                usuario.isActivo(),
                AuthorityUtils.createAuthorityList(autoridades)
        );
    }
}
