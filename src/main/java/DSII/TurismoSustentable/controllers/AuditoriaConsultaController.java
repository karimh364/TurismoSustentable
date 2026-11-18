package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.AuditoriaAdministradorDTO;
import DSII.TurismoSustentable.dto.AuditoriaDetalleDTO;
import DSII.TurismoSustentable.dto.AuditoriaResumenDTO;
import DSII.TurismoSustentable.services.ConsultaAuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auditoria")
public class AuditoriaConsultaController {
    private final ConsultaAuditoriaService servicio;

    public AuditoriaConsultaController(ConsultaAuditoriaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public Map<String, Object> listar(
            @RequestParam(name = "entidad", required = false) String entidad,
            @RequestParam(name = "accion", required = false) String accion,
            @RequestParam(name = "administradorId", required = false) Integer administradorId,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamano", defaultValue = "20") int tamano
    ) {
        Page<AuditoriaResumenDTO> resultado = servicio.listar(entidad, accion, administradorId, pagina, tamano);
        return Map.of(
                "eventos", resultado.getContent(),
                "pagina", resultado.getNumber(),
                "tamano", resultado.getSize(),
                "totalEventos", resultado.getTotalElements(),
                "totalPaginas", resultado.getTotalPages(),
                "referenciaHoraria", "NO_DECLARADA"
        );
    }

    @GetMapping("/administradores")
    public List<AuditoriaAdministradorDTO> administradores() {
        return servicio.administradores();
    }

    @GetMapping("/{id}")
    public AuditoriaDetalleDTO detalle(@PathVariable("id") int id) {
        return servicio.detalle(id);
    }
}
