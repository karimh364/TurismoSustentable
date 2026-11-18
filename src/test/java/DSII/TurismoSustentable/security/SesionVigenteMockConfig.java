package DSII.TurismoSustentable.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class SesionVigenteMockConfig {

    @Bean
    public AccesoSesionService accesoSesionServiceSimulado() {
        AccesoSesionService servicio =
                mock(AccesoSesionService.class);

        when(servicio.comprobar(any(Authentication.class)))
                .thenAnswer(invocacion -> Optional.ofNullable(
                        invocacion.getArgument(0, Authentication.class)
                ));

        return servicio;
    }
}
