package DSII.TurismoSustentable.configurations;

import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.services.AlquilerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.*;

@Configuration
@Profile("refactor")
@EnableScheduling
@ConditionalOnProperty(name = "alquiler.vencimientos.automaticos", havingValue = "true", matchIfMissing = true)
public class VencimientoReservasJob {
    private static final Logger LOG = LoggerFactory.getLogger(VencimientoReservasJob.class);
    private final AlquilerRepository alquileres;
    private final AlquilerService servicio;
    private final Clock reloj;
    public VencimientoReservasJob(AlquilerRepository alquileres, AlquilerService servicio, Clock reloj) {
        this.alquileres = alquileres; this.servicio = servicio; this.reloj = reloj;
    }
    @Scheduled(fixedDelayString = "${alquiler.vencimientos.intervalo-ms:10000}", initialDelay = 5000)
    public void procesar() {
        for (Integer id : alquileres.buscarReservasVencidas(LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC), PageRequest.of(0, 100))) {
            try { servicio.vencerReserva(id); }
            catch (RuntimeException e) { LOG.error("No se pudo vencer la reserva {}. Se reintentará en el próximo ciclo.", id, e); }
        }
    }
}
