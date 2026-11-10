package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.AlquilerDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlquilerDetalleRepository
        extends JpaRepository<AlquilerDetalle, Integer> {

    @EntityGraph(attributePaths = {
            "bicicleta",
            "tipoBicicletaContratado",
            "estado",
            "estacionDevolucion"
    })
    @Query(
            "select d from AlquilerDetalle d "
                    + "where d.alquiler.id = :alquilerId "
                    + "order by d.id asc"
    )
    List<AlquilerDetalle> buscarPorAlquilerConDatos(
            @Param("alquilerId") Integer alquilerId
    );

}