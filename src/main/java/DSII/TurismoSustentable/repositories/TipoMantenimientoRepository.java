package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.TipoMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoMantenimientoRepository
        extends JpaRepository<TipoMantenimiento, Integer> {

    Optional<TipoMantenimiento> findByCodigo(String codigo);
}