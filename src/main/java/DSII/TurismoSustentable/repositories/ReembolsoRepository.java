package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Reembolso;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ReembolsoRepository
        extends JpaRepository<Reembolso, Integer> {

    Optional<Reembolso> findByClaveIdempotencia(String claveIdempotencia);

    List<Reembolso> findByPagoIdOrderByIdAsc(Integer pagoId);

    List<Reembolso> findByPagoAlquilerIdOrderByIdAsc(Integer alquilerId);

    List<Reembolso>
    findByPagoAlquilerUsuarioIdAndPagoPlataformaAndPagoEntornoOrderByIdAsc(
            Integer usuarioId,
            String plataforma,
            String entorno
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reembolso r where r.id = :id")
    Optional<Reembolso> buscarPorIdConBloqueo(@Param("id") Integer id);
}
