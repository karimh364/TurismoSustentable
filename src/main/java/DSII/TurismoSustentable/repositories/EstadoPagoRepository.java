package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EstadoPagoRepository
        extends JpaRepository<EstadoPago, Integer> {

    Optional<EstadoPago> findByCodigo(String codigo);
}
