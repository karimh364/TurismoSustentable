package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.HistorialEstadoPago;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistorialEstadoPagoRepository
        extends JpaRepository<HistorialEstadoPago, Integer> {

    @EntityGraph(attributePaths = {"estadoAnterior", "estadoNuevo"})
    List<HistorialEstadoPago> findByPagoIdOrderByIdAsc(Integer pagoId);
}