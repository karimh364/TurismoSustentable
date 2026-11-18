package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByCodigo(String codigo);

    // Cerrojo común de la gestión de roles: protege el último ADMIN activo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rol r where r.codigo = :codigo")
    Optional<Rol> buscarPorCodigoConBloqueo(@Param("codigo") String codigo);
}
