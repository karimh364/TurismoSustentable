package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.AlquilerCabeceraDTO;
import DSII.TurismoSustentable.services.EstadisticasService;
import DSII.TurismoSustentable.services.AlquilerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Importación clave
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/estadisticas")
// --- CLAVE: Restringe el acceso a todo este controlador solo al rol ADMIN ---
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    @Autowired
    private EstadisticasService estadisticasService;
    @Autowired
    private AlquilerService alquilerService;

    @GetMapping("/resumen")
    public ResponseEntity<Map<String, Object>> getResumenEstadistico() {

        Map<String, Object> resumen = new HashMap<>();

        // ... (Llamadas al servicio) ...
        List<Object[]> preferencias = estadisticasService.getPreferenciasBicicletaPorGenero();
        resumen.put("preferencias_por_genero_y_tipo", preferencias);

        Long alquiladas = estadisticasService.getBicicletasAlquiladasActuales();
        resumen.put("bicicletas_alquiladas_actualmente", alquiladas);

        List<Object[]> estados = estadisticasService.getBicicletasPorEstado();
        resumen.put("estados_de_bicicletas", estados);

        return ResponseEntity.ok(resumen);
    }

    //Metodo del admin para hacer GET a todos los alquileres
    @GetMapping("/alquileres-todos")
    public List<AlquilerCabeceraDTO> getAllAlquileres() {
        return alquilerService.listarAlquileresAdministracion();
    }

}
