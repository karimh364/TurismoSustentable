package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.EstadoOrdenMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoOrdenMantenimientoRepository
        extends JpaRepository<EstadoOrdenMantenimiento, Integer> {

    Optional<EstadoOrdenMantenimiento> findByCodigo(String codigo);
}