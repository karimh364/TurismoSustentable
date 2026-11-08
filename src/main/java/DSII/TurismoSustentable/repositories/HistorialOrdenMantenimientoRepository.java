package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.HistorialOrdenMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialOrdenMantenimientoRepository
        extends JpaRepository<HistorialOrdenMantenimiento, Integer> {
}