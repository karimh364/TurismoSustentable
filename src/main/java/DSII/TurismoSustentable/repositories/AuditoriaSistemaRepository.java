package DSII.TurismoSustentable.repositories;

import DSII.TurismoSustentable.dto.AuditoriaAdministradorDTO;
import DSII.TurismoSustentable.dto.AuditoriaResumenDTO;
import DSII.TurismoSustentable.models.AuditoriaSistema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuditoriaSistemaRepository extends JpaRepository<AuditoriaSistema, Integer> {

    // El tipo distingue objetivos con el mismo ID. Los LEFT JOIN conservan eventos sin objetivo disponible.
    String ORIGEN_CON_REFERENCIA = "from AuditoriaSistema a join a.administrador operador "
            + "left join Usuario usuarioObjetivo on a.entidadAfectada = 'USUARIO' and usuarioObjetivo.id = a.entidadId "
            + "left join Bicicleta biciObjetivo on a.entidadAfectada = 'BICICLETA' and biciObjetivo.id = a.entidadId "
            + "left join Estacion estacionObjetivo on a.entidadAfectada = 'ESTACION' and estacionObjetivo.id = a.entidadId ";
    String REFERENCIA_ENTIDAD = "coalesce(usuarioObjetivo.nickname, biciObjetivo.codigoBicicleta, estacionObjetivo.nombre)";
    String FILTROS_RESUMEN = "where (:entidad is null or a.entidadAfectada = :entidad) "
            + "and (:accion is null or a.accion = :accion) "
            + "and (:administradorId is null or a.administrador.id = :administradorId)";

    // Los datos libres y los snapshots quedan fuera de la consulta, no solo del JSON.
    @Query(
            value = "select new DSII.TurismoSustentable.dto.AuditoriaResumenDTO("
                    + "a.id, operador.id, operador.nickname, a.entidadAfectada, a.entidadId, "
                    + "a.accion, a.fechaOperacion, " + REFERENCIA_ENTIDAD + ") "
                    + ORIGEN_CON_REFERENCIA
                    + FILTROS_RESUMEN + " order by a.id desc",
            countQuery = "select count(a.id) from AuditoriaSistema a " + FILTROS_RESUMEN
    )
    Page<AuditoriaResumenDTO> consultarResumen(
            @Param("entidad") String entidad, @Param("accion") String accion,
            @Param("administradorId") Integer administradorId, Pageable pageable
    );

    // Solo actores con eventos. Sus roles o su cuenta actual no borran el hecho histórico.
    @Query("select distinct new DSII.TurismoSustentable.dto.AuditoriaAdministradorDTO(operador.id, operador.nickname) "
            + "from AuditoriaSistema a join a.administrador operador order by operador.nickname, operador.id")
    List<AuditoriaAdministradorDTO> consultarAdministradores();

    // Proyección interna de un evento: no carga el motivo libre ni entidades completas.
    @Query("select a.id as id, operador.id as administradorId, operador.nickname as administradorNickname, "
            + "a.entidadAfectada as entidadAfectada, a.entidadId as entidadId, a.accion as accion, "
            + "a.fechaOperacion as fechaOperacion, a.valoresAnteriores as valoresAnteriores, "
            + "a.valoresNuevos as valoresNuevos, " + REFERENCIA_ENTIDAD + " as referenciaEntidad, "
            + "case when length(trim(a.motivo)) > 0 then true else false end as motivoRegistrado "
            + ORIGEN_CON_REFERENCIA + "where a.id = :id")
    Optional<DetalleRegistrado> consultarDetalle(@Param("id") int id);

    interface DetalleRegistrado {
        Integer getId();
        Integer getAdministradorId();
        String getAdministradorNickname();
        String getEntidadAfectada();
        Integer getEntidadId();
        String getReferenciaEntidad();
        String getAccion();
        LocalDateTime getFechaOperacion();
        String getValoresAnteriores();
        String getValoresNuevos();
        boolean getMotivoRegistrado();
    }
}
