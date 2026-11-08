package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.OrdenMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenMantenimientoRepository
        extends JpaRepository<OrdenMantenimiento, Integer> {
}