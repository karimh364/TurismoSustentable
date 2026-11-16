package DSII.TurismoSustentable.configurations;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.services.MantenimientoService;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.slf4j.*;
import java.util.List;

@Configuration @Profile("refactor") @EnableScheduling
@ConditionalOnProperty(name="mantenimiento.preventivo.automatico",havingValue="true",matchIfMissing=true)
public class PreventivoMantenimientoJob {
    private static final Logger LOG=LoggerFactory.getLogger(PreventivoMantenimientoJob.class);
    private final BicicletaRepository bicicletas; private final MantenimientoService servicio;
    public PreventivoMantenimientoJob(BicicletaRepository bicicletas,MantenimientoService servicio){this.bicicletas=bicicletas;this.servicio=servicio;}
    @Scheduled(fixedDelayString="${mantenimiento.preventivo.intervalo-ms:60000}",initialDelay=10000)
    public void procesar(){
        int desde=0;
        while(true){
            List<Integer> ids=bicicletas.buscarCandidatasPreventiva(desde,PageRequest.of(0,100));
            if(ids.isEmpty()) return;
            for(Integer id:ids){
                try {servicio.aplicarPreventiva(id);}
                catch(RuntimeException e){LOG.error("No se pudo revisar la preventiva de la bicicleta {}. Se reintentará.",id,e);}
                desde=id;
            }
            if(ids.size()<100) return;
        }
    }
}
