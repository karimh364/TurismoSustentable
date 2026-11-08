package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlquilerRepository extends JpaRepository<Alquiler, Integer> {

    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Alquiler a WHERE a.id = :id")
    java.util.Optional<Alquiler> buscarPorIdConBloqueo(
            @org.springframework.data.repository.query.Param("id") Integer id);

    boolean existsByBicicletaAndEstado(
            DSII.TurismoSustentable.models.Bicicleta bicicleta,
            DSII.TurismoSustentable.models.CodigoEstadoAlquiler estado);

    /**
     * Agrupa alquileres finalizados por tipo de bicicleta
     * y código de género de la persona.
     *
     * Cada fila contiene:
     * [String codigoTipoBicicleta, String codigoGenero, Long cantidad].
     */
    @Query("SELECT a.bicicleta.tipo.codigo, a.usuario.persona.genero.codigo, COUNT(a.id) " +
            "FROM Alquiler a " +
            "WHERE a.estado = DSII.TurismoSustentable.models.CodigoEstadoAlquiler.FINALIZADO " +
            "GROUP BY a.bicicleta.tipo.codigo, a.usuario.persona.genero.codigo " +
            "ORDER BY a.usuario.persona.genero.codigo, COUNT(a.id) DESC")
    List<Object[]> contarPreferenciasBicicletaPorGenero();

    // ----------------------------------------------------------------------------------------------------

    /**
     * 2. CANTIDAD DE BICICLETAS ALQUILADAS
     * * Devuelve la cantidad de alquileres que están actualmente en curso (estado INICIADO).
     */
    @Query("SELECT COUNT(a) FROM Alquiler a WHERE a.estado = DSII.TurismoSustentable.models.CodigoEstadoAlquiler.INICIADO")
    Long contarBicicletasAlquiladasActuales();

    // ----------------------------------------------------------------------------------------------------

    // MÉTODOS DE BÚSQUEDA PERSONALIZADOS
    List<Alquiler> findByBorradoAlquilerFalse();
    List<Alquiler> findByUsuarioAndBorradoAlquilerFalse(Usuario usuario);
}
