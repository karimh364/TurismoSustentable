package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.AlquilerDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlquilerDetalleRepository
        extends JpaRepository<AlquilerDetalle, Integer> {
}