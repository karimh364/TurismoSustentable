package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.models.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidenciaRepository
        extends JpaRepository<Incidencia, Integer> {
    @org.springframework.data.jpa.repository.Query("select count(i) from Incidencia i where i.alquilerDetalle.bicicleta.id=:bicicletaId and i.estadoRevision='PENDIENTE'")
    long contarPendientesPorBicicleta(@org.springframework.data.repository.query.Param("bicicletaId") Integer bicicletaId);

    List<Incidencia> findByAlquilerDetalleIdOrderByIdAsc(Integer detalleId);

    @org.springframework.data.jpa.repository.Query("select i.alquilerDetalle.alquiler.id from Incidencia i where i.id=:id")
    java.util.Optional<Integer> buscarAlquilerId(@org.springframework.data.repository.query.Param("id") Integer id);

    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Incidencia i where i.id=:id")
    java.util.Optional<Incidencia> buscarPorIdConBloqueo(@org.springframework.data.repository.query.Param("id") Integer id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths={"alquilerDetalle","alquilerDetalle.bicicleta","alquilerDetalle.alquiler"})
    @org.springframework.data.jpa.repository.Query("select i from Incidencia i where (:usuarioId is null or i.alquilerDetalle.alquiler.usuario.id=:usuarioId) order by i.id desc")
    List<Incidencia> listarVisibles(@org.springframework.data.repository.query.Param("usuarioId") Integer usuarioId);

    @org.springframework.data.jpa.repository.Query("select count(i) from Incidencia i where i.ordenMantenimiento.id=:ordenId "
            + "and i.fechaHoraRecepcionFisica is not null and i.recibidoPor is not null "
            + "and (i.estacionRecepcion is not null or i.lugarRecepcion is not null)")
    long contarRecepcionesDeOrden(@org.springframework.data.repository.query.Param("ordenId") Integer ordenId);
}
