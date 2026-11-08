package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Bicicleta;
import DSII.TurismoSustentable.models.Estacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface BicicletaRepository extends JpaRepository<Bicicleta, Integer> {

    /**
     * 3. ESTADOS DE LAS BICICLETAS (Preferencias/Estados de Bicis en general)
     * * Consulta que utiliza la tabla Bicicleta (no Alquiler) para contar cuántas bicicletas
     * están en cada estado. Resultado: [String código, Long cantidad].
     */

    @Query("SELECT b.estado.codigo, COUNT(b.id) FROM Bicicleta b GROUP BY b.estado.codigo")
    List<Object[]> contarBicicletasPorEstado();

    Optional<Bicicleta> findByCodigoBicicleta(String codigoBicicleta);
    boolean existsByCodigoBicicletaIgnoreCaseAndIdNot(String codigoBicicleta, int id);

    @Override
    @EntityGraph(attributePaths = {"tipo", "estado", "ubicacionActual"})
    List<Bicicleta> findAll();

    @Override
    @EntityGraph(attributePaths = {"tipo", "estado", "ubicacionActual"})
    Optional<Bicicleta> findById(Integer id);

    @EntityGraph(attributePaths = {"tipo", "estado", "ubicacionActual"})
    List<Bicicleta> findByBorradoBiciFalse();
    @EntityGraph(attributePaths = {"tipo", "estado", "ubicacionActual"})
    @Query(
            "SELECT b FROM Bicicleta b " +
                    "JOIN b.ubicacionActual e " +
                    "WHERE b.borradoBici = false " +
                    "AND b.estado.codigo = :estado " +
                    "AND e.borradoEstacion = false " +
                    "AND e.esPublica = true"
    )
    List<Bicicleta> buscarPorEstadoEnEstacionesPublicas(
            @Param("estado") String estado
    );
    @EntityGraph(attributePaths = {"tipo", "estado", "ubicacionActual"})
    List<Bicicleta> findByBorradoBiciFalseAndTipo_Codigo(String codigo);

    // Cuenta las bicicletas que están físicamente en una estación y que no fueron eliminadas lógicamente
    long countByUbicacionActualAndBorradoBiciFalse(
            Estacion ubicacionActual
    );

    // Cuenta todas las bicicletas físicamente asociadas a esa estación, incluso las dadas de baja lógicamente
    long countByUbicacionActual(
            Estacion ubicacionActual
    );

    // Bloqueo de la bici mientras se crea el alquiler
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT b FROM Bicicleta b " +
                    "WHERE b.id = :id " +
                    "AND b.borradoBici = false"
    )
    Optional<Bicicleta> buscarActivaPorIdConBloqueo(
            @Param("id") Integer id
    );
}
