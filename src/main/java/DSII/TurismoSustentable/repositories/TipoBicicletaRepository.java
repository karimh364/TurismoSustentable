package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.TipoBicicleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoBicicletaRepository
        extends JpaRepository<TipoBicicleta, Integer> {

    Optional<TipoBicicleta> findByCodigo(String codigo);
}