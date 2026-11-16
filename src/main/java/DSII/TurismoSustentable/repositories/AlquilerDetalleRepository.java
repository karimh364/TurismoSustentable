package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.AlquilerDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    @EntityGraph(attributePaths = {
            "bicicleta",
            "tipoBicicletaContratado",
            "estado",
            "estacionDevolucion"
    })
    @Query(
            "select d from AlquilerDetalle d "
                    + "where d.alquiler.id in :alquilerIds "
                    + "order by d.alquiler.id asc, d.id asc"
    )
    List<AlquilerDetalle> buscarPorAlquileresConDatos(
            @Param("alquilerIds") Collection<Integer> alquilerIds
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select d from AlquilerDetalle d "
                    + "where d.id = :detalleId "
                    + "and d.alquiler.id = :alquilerId"
    )
    Optional<AlquilerDetalle> buscarPorAlquilerYDetalleConBloqueo(
            @Param("alquilerId") Integer alquilerId,
            @Param("detalleId") Integer detalleId
    );

    // La cabecera debe estar bloqueada antes de modificar cualquiera de sus detalles.
    @Query("select d from AlquilerDetalle d where d.alquiler.id = :alquilerId order by d.id")
    List<AlquilerDetalle> buscarPorAlquiler(
            @Param("alquilerId") Integer alquilerId
    );

}
