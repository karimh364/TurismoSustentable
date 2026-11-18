package DSII.TurismoSustentable.configurations;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("refactor")
public class ConvencionHoraAlquilerConfig {
    @Bean
    SmartInitializingSingleton verificarConvencionHoraAlquiler(JdbcTemplate jdbc) {
        // Antes de que se inicien las tareas programadas: no leer una BD UTC como hora local.
        return () -> {
            final String revision;
            try {
                revision = jdbc.queryForObject("select revision from v_convencion_hora_alquiler", String.class);
            } catch (DataAccessException ex) {
                throw new IllegalStateException("Falta verificar la migración 06 de hora local de alquiler. "
                        + "No se puede arrancar con fechas sin migrar.", ex);
            }
            if (!"ALQUILER_UTC3_V1".equals(revision)) {
                throw new IllegalStateException("La convención horaria de alquiler de la BD no coincide con el código.");
            }
        };
    }
}
