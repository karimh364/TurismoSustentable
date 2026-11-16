package DSII.TurismoSustentable.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import DSII.TurismoSustentable.models.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import javax.persistence.LockModeType;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    @Query("select u.id from Usuario u where u.email=:email")
    Optional<Integer> buscarIdPorEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id=:id")
    Optional<Usuario> buscarPorIdConBloqueo(@Param("id") Integer id);

    @Query("select distinct u from Usuario u join u.usuarioRoles ur where u.activo=true and ur.rol.codigo='TECNICO' order by u.id")
    List<Usuario> buscarTecnicosActivos();

    List<Usuario> findByActivoTrue();
    Optional<Usuario> findByEmail(String email);

    // Para comprobar duplicados durante el registro
    boolean existsByEmailIgnoreCase(String email);

    // Para comprobar duplicados al editar,
    // excluyendo al mismo usuario
    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Integer id
    );

    Optional<Usuario> findByTokenRecuperacion(
            String tokenRecuperacion
    );

    @EntityGraph(attributePaths = {
            "persona.genero",
            "usuarioRoles.rol"
    })
    Optional<Usuario> findByNicknameIgnoreCase(String nickname);

    boolean existsByNicknameIgnoreCase(String nickname);

    boolean existsByNicknameIgnoreCaseAndIdNot(
            String nickname,
            Integer id
    );

    @EntityGraph(attributePaths = {
            "persona.genero",
            "usuarioRoles.rol"
    })
    @Query("select distinct u from Usuario u where u.activo = true")
    List<Usuario> buscarActivosConDatosYRoles();

    @EntityGraph(attributePaths = {
            "persona.genero",
            "usuarioRoles.rol"
    })
    @Query("select distinct u from Usuario u where u.id = :id")
    Optional<Usuario> buscarPorIdConDatosYRoles(
            @Param("id") Integer id
    );

    @EntityGraph(attributePaths = {
            "persona.genero",
            "usuarioRoles.rol"
    })
    @Query("select distinct u from Usuario u where u.email = :email")
    Optional<Usuario> buscarPorEmailConDatosYRoles(
            @Param("email") String email
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // La cuenta se bloquea antes que la cabecera. No precargar alquileres EAGER:
    // podrían quedar en el contexto con una versión anterior mientras esperamos su bloqueo.
    @EntityGraph(type=EntityGraph.EntityGraphType.FETCH,attributePaths={"persona"})
    @Query("select u from Usuario u " +
            "where u.email = :email and u.activo = true")
    Optional<Usuario> buscarActivoPorEmailConBloqueo(
            @Param("email") String email
    );

}
