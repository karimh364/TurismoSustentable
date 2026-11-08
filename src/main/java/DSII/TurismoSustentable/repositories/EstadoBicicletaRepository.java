package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.EstadoBicicleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoBicicletaRepository
        extends JpaRepository<EstadoBicicleta, Integer> {

    Optional<EstadoBicicleta> findByCodigo(String codigo);
}