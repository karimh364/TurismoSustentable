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
    @Query("select count(d) from AlquilerDetalle d where d.bicicleta.id=:bicicletaId and d.estado.codigo in ('RESERVADO','PENDIENTE_RETIRO','EN_CURSO')")
    long contarAsignacionesActivas(@Param("bicicletaId") Integer bicicletaId);

    @Query("select count(d) from AlquilerDetalle d where d.bicicleta.id=:bicicletaId and d.fallaReportada=true "
            + "and (d.fechaReporteFalla is null or d.fechaReporteFalla > :fecha)")
    long contarFallasPosteriores(@Param("bicicletaId") Integer bicicletaId, @Param("fecha") java.time.LocalDateTime fecha);
    @Query("select count(d) from AlquilerDetalle d where d.bicicleta.id=:bicicletaId and d.fallaReportada=true")
    long contarFallasReportadas(@Param("bicicletaId") Integer bicicletaId);

    @Query("select case when count(d) > 0 then true else false end from AlquilerDetalle d "
            + "where d.bicicleta.id = :bicicletaId and d.id <> :detalleId "
            + "and d.estado.codigo in ('RESERVADO','PENDIENTE_RETIRO','EN_CURSO')")
    boolean existeOtraAsignacionVigente(@Param("bicicletaId") Integer bicicletaId,
                                       @Param("detalleId") Integer detalleId);

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

    // Cuenta bicicletas efectivamente en uso, no cabeceras ni reservas.
    @Query("select count(d) from AlquilerDetalle d "
            + "where d.estado.codigo = 'EN_CURSO'")
    Long contarBicicletasAlquiladasActuales();

    // Cada fila: código de tipo contratado, código de género y cantidad de usos.
    @Query("select t.codigo, g.codigo, count(d.id) "
            + "from AlquilerDetalle d "
            + "join d.tipoBicicletaContratado t "
            + "join d.alquiler a "
            + "join a.usuario u "
            + "join u.persona p "
            + "join p.genero g "
            + "where d.estado.codigo in ('DEVUELTO', 'CERRADO_POR_INCIDENCIA') "
            + "and d.fechaHoraRetiro is not null "
            + "group by t.codigo, g.codigo "
            + "order by g.codigo, count(d.id) desc, t.codigo")
    List<Object[]> contarPreferenciasBicicletaPorGenero();

}
