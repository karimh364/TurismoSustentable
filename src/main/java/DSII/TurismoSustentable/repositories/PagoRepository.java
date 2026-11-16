package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Pago;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Integer> {

    Optional<Pago> findByClaveIdempotencia(String claveIdempotencia);

    @EntityGraph(attributePaths = "estadoPago")
    List<Pago> findByAlquilerIdOrderByIdAsc(Integer alquilerId);

    @EntityGraph(attributePaths = "estadoPago")
    List<Pago> findByAlquilerUsuarioIdAndPlataformaAndEntornoOrderByIdAsc(
            Integer usuarioId,
            String plataforma,
            String entorno
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.id = :id")
    Optional<Pago> buscarPorIdConBloqueo(@Param("id") Integer id);
}
