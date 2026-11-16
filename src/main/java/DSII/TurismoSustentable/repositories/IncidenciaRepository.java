package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaRepository
        extends JpaRepository<Incidencia, Integer> {
    @org.springframework.data.jpa.repository.Query("select count(i) from Incidencia i where i.alquilerDetalle.bicicleta.id=:bicicletaId and i.estadoRevision='PENDIENTE'")
    long contarPendientesPorBicicleta(@org.springframework.data.repository.query.Param("bicicletaId") Integer bicicletaId);

    List<Incidencia> findByAlquilerDetalleIdOrderByIdAsc(Integer detalleId);
}
