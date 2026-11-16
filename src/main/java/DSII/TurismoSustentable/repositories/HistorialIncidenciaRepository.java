package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.HistorialIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialIncidenciaRepository
        extends JpaRepository<HistorialIncidencia, Integer> {
    java.util.List<HistorialIncidencia> findByIncidenciaIdOrderByIdAsc(Integer incidenciaId);
    boolean existsByIncidenciaIdAndAccion(Integer incidenciaId, String accion);
}
