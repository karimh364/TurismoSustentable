package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.EstadoAlquiler;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoAlquilerRepository
        extends JpaRepository<EstadoAlquiler, Integer> {

    Optional<EstadoAlquiler> findByCodigo(String codigo);
}