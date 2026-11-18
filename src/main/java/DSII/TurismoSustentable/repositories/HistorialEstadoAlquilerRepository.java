package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.HistorialEstadoAlquiler;
import DSII.TurismoSustentable.dto.HistorialEstadoAlquilerDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HistorialEstadoAlquilerRepository extends JpaRepository<HistorialEstadoAlquiler, Integer> {
    // El ID desempata eventos de igual fecha y conserva el orden de escritura bajo bloqueo de cabecera.
    // DTO directo: no carga Usuario.alquileres (EAGER) ni credenciales para mostrar un actor.
    @Query("select new DSII.TurismoSustentable.dto.HistorialEstadoAlquilerDTO("
            + "h.id,h.alquiler.id,anterior.codigo,nuevo.codigo,actor.id,actor.nickname,"
            + "h.origen,h.observaciones,h.fechaHoraCambio) from HistorialEstadoAlquiler h "
            + "left join h.estadoAnterior anterior join h.estadoNuevo nuevo "
            + "left join h.actorUsuario actor where h.alquiler.id=:alquilerId order by h.id asc")
    List<HistorialEstadoAlquilerDTO> consultarPorAlquiler(@Param("alquilerId") Integer alquilerId);
}
