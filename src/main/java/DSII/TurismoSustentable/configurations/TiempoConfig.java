package DSII.TurismoSustentable.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TiempoConfig {

    @Bean
    public Clock relojAplicacion() {
        return Clock.systemUTC();
    }
}
