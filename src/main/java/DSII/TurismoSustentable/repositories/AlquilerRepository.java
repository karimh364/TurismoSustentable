package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface AlquilerRepository extends JpaRepository<Alquiler, Integer> {

    @Query("select a.id from Alquiler a where a.estadoContratacion.codigo in ('RESERVADO','EN_PROGRESO') "
            + "and a.fechaHoraVencimientoReserva <= :ahora "
            + "and exists (select d.id from AlquilerDetalle d where d.alquiler.id = a.id "
            + "and d.estado.codigo in ('RESERVADO','PENDIENTE_RETIRO')) order by a.id")
    List<Integer> buscarReservasVencidas(@Param("ahora") java.time.LocalDateTime ahora,
                                        org.springframework.data.domain.Pageable pagina);

    @Query("select a from Alquiler a where a.usuario.id = :usuarioId "
            + "and a.fechaHoraLiquidacion is not null order by a.id desc")
    List<Alquiler> buscarLiquidacionesPorUsuario(@Param("usuarioId") Integer usuarioId);

    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Alquiler a WHERE a.id = :id")
    java.util.Optional<Alquiler> buscarPorIdConBloqueo(
            @org.springframework.data.repository.query.Param("id") Integer id);

    boolean existsByBicicletaAndEstado(
            DSII.TurismoSustentable.models.Bicicleta bicicleta,
            DSII.TurismoSustentable.models.CodigoEstadoAlquiler estado);

    // MÉTODOS DE BÚSQUEDA PERSONALIZADOS
    List<Alquiler> findByBorradoAlquilerFalse();
    List<Alquiler> findByUsuarioAndBorradoAlquilerFalse(Usuario usuario);

    @EntityGraph(attributePaths = {
            "usuario",
            "estadoContratacion",
            "ubicacionRetiro"
    })
    @Query(
            "select a from Alquiler a "
                    + "where a.id = :id "
                    + "and a.borradoAlquiler = false "
                    + "and a.estadoContratacion is not null"
    )
    Optional<Alquiler> buscarAlquilerVisiblePorId(
            @Param("id") Integer id
    );

    @EntityGraph(attributePaths = {
            "usuario",
            "estadoContratacion",
            "ubicacionRetiro",
            "bicicleta",
            "ubicacionDevolucion"
    })
    @Query(
            "select a from Alquiler a "
                    + "where a.usuario.id = :usuarioId "
                    + "and a.borradoAlquiler = false "
                    + "order by a.id desc"
    )
    List<Alquiler> buscarVisiblesPorUsuarioConDatos(
            @Param("usuarioId") Integer usuarioId
    );

    @EntityGraph(attributePaths = {
            "usuario",
            "estadoContratacion",
            "ubicacionRetiro",
            "bicicleta",
            "ubicacionDevolucion"
    })
    @Query(
            "select a from Alquiler a "
                    + "where a.borradoAlquiler = false "
                    + "order by a.id desc"
    )
    List<Alquiler> buscarVisiblesAdministracionConDatos();

    @Query(
            value =
                    "select count(*) from alquiler a " +
                    "where a.usuario_id = :usuarioId " +
                    "and a.estado_alquiler_id is not null " +
                    "and a.fecha_hora_liquidacion is not null " +
                    "and a.costo_final_ars > (" +
                    "coalesce((" +
                    "select sum(p.monto_ars) " +
                    "from pago p " +
                    "join estado_pago ep on ep.id = p.estado_pago_id " +
                    "where p.alquiler_id = a.id " +
                    "and ep.codigo = 'APROBADO' " +
                    "and p.moneda = 'ARS' " +
                    "and p.fecha_confirmacion is not null" +
                    "), 0) - " +
                    "coalesce((" +
                    "select sum(r.monto_ars) " +
                    "from reembolso r " +
                    "join pago p2 on p2.id = r.pago_id " +
                    "where p2.alquiler_id = a.id " +
                    "and r.estado = 'CONFIRMADO' " +
                    "and r.moneda = 'ARS' " +
                    "and r.fecha_confirmacion is not null" +
                    "), 0))",
            nativeQuery = true
    )
    long contarAlquileresConSaldoPendiente(
            @Param("usuarioId") Integer usuarioId
    );

}
