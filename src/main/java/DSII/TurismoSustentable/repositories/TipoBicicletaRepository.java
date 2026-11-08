package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.TipoBicicleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;

import java.util.Optional;

public interface TipoBicicletaRepository
        extends JpaRepository<TipoBicicleta, Integer> {

    Optional<TipoBicicleta> findByCodigo(String codigo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TipoBicicleta t WHERE t.id = :id")
    Optional<TipoBicicleta> buscarPorIdConBloqueo(@Param("id") Integer id);
}
