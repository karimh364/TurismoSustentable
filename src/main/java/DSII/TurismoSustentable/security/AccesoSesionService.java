package DSII.TurismoSustentable.security;

import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.repositories.projections.AccesoUsuarioProjection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class AccesoSesionService {

    private static final Set<String> ROLES_VALIDOS =
            Set.of("CLIENTE", "ADMIN", "TECNICO");

    private final UsuarioRepository usuarioRepository;

    public AccesoSesionService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Authentication> comprobar(
            Authentication autenticacion
    ) {
        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal()
                     instanceof UsuarioAutenticado)) {
            return Optional.empty();
        }

        UsuarioAutenticado principal =
                (UsuarioAutenticado) autenticacion.getPrincipal();

        List<AccesoUsuarioProjection> filas =
                usuarioRepository.buscarAccesoVigentePorId(
                        principal.getUsuarioId()
                );

        if (filas == null) {
            throw configuracionInvalida();
        }

        if (filas.isEmpty()) {
            return Optional.empty();
        }

        AccesoUsuarioProjection cuenta = filas.get(0);

        if (cuenta == null
                || cuenta.getActivo() == null
                || cuenta.getEmail() == null
                || cuenta.getEmail().isBlank()) {
            throw configuracionInvalida();
        }

        Set<String> roles = new TreeSet<>();

        for (AccesoUsuarioProjection fila : filas) {
            if (fila == null
                    || !Objects.equals(
                            fila.getUsuarioId(),
                            principal.getUsuarioId()
                    )
                    || !Objects.equals(
                            fila.getEmail(),
                            cuenta.getEmail()
                    )
                    || !Objects.equals(
                            fila.getActivo(),
                            cuenta.getActivo()
                    )) {
                throw configuracionInvalida();
            }

            String codigo = fila.getRolCodigo();

            // LEFT JOIN devuelve rol nulo si la cuenta no tiene roles.
            if (codigo == null) {
                continue;
            }

            if (!ROLES_VALIDOS.contains(codigo)) {
                throw configuracionInvalida();
            }

            roles.add(codigo);
        }

        if (!cuenta.getActivo()
                || !Objects.equals(
                        principal.getUsername(),
                        cuenta.getEmail()
                )) {
            return Optional.empty();
        }

        UsuarioAutenticado vigente = new UsuarioAutenticado(
                principal.getUsuarioId(),
                cuenta.getEmail(),
                "",
                true,
                AuthorityUtils.createAuthorityList(
                        roles.toArray(new String[0])
                )
        );

        vigente.eraseCredentials();

        UsernamePasswordAuthenticationToken actualizada =
                new UsernamePasswordAuthenticationToken(
                        vigente,
                        null,
                        vigente.getAuthorities()
                );

        actualizada.setDetails(autenticacion.getDetails());

        return Optional.of(actualizada);
    }

    private ConfiguracionSistemaException configuracionInvalida() {
        return new ConfiguracionSistemaException(
                "No se pudo comprobar la configuración de acceso de la cuenta."
        );
    }
}
