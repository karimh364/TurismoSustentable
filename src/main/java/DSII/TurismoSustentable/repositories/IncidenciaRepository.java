package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidenciaRepository
        extends JpaRepository<Incidencia, Integer> {
}