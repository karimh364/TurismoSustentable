package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaRepository
        extends JpaRepository<Incidencia, Integer> {

    List<Incidencia> findByAlquilerDetalleIdOrderByIdAsc(Integer detalleId);
}
