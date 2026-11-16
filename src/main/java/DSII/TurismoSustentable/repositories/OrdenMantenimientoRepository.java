package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.OrdenMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

public interface OrdenMantenimientoRepository
        extends JpaRepository<OrdenMantenimiento, Integer> {

    // El bloqueo de Bicicleta serializa la comprobación y la creación de una orden.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenMantenimiento o where o.bicicleta.id = :bicicletaId "
            + "and o.estado.codigo in ('CREADA', 'EN_PROCESO', 'PAUSADA') order by o.id")
    List<OrdenMantenimiento> buscarAbiertasPorBicicleta(
            @Param("bicicletaId") Integer bicicletaId
    );

    @Query("select max(o.fechaFin) from OrdenMantenimiento o "
            + "where o.bicicleta.id = :bicicletaId "
            + "and o.tipoMantenimiento.codigo = 'PREVENTIVO' "
            + "and o.estado.codigo = 'FINALIZADA' and o.resultadoTecnico = 'APTA'")
    LocalDateTime buscarUltimaPreventivaApta(
            @Param("bicicletaId") Integer bicicletaId
    );
}
