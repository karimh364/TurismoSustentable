package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.HistorialEstadoReembolso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialEstadoReembolsoRepository
        extends JpaRepository<HistorialEstadoReembolso, Integer> {

    List<HistorialEstadoReembolso> findByReembolsoIdOrderByIdAsc(
            Integer reembolsoId
    );
}
