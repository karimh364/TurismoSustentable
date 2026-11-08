package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.EstadoAlquilerDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoAlquilerDetalleRepository
        extends JpaRepository<EstadoAlquilerDetalle, Integer> {

    Optional<EstadoAlquilerDetalle> findByCodigo(String codigo);
}