package DSII.TurismoSustentable.controllers;
import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.services.IncidenciaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController {
    private final IncidenciaService servicio;
    public IncidenciaController(IncidenciaService servicio){this.servicio=servicio;}
    @GetMapping public Object listar(){return servicio.listar();}
    @GetMapping("/{id}") public ResponseEntity<?> consultar(@PathVariable Integer id){return respuesta(()->servicio.consultar(id));}
    @GetMapping("/{id}/historial") public ResponseEntity<?> historial(@PathVariable Integer id){return respuesta(()->servicio.historial(id));}
    @PostMapping public ResponseEntity<?> reportar(@Valid @RequestBody ReportarIncidenciaDTO dto){return respuesta(()->servicio.reportar(dto));}
    @PostMapping("/{id}/resolver") public ResponseEntity<?> resolver(@PathVariable Integer id,@Valid @RequestBody ResolverIncidenciaDTO dto){return respuesta(()->servicio.resolver(id,dto));}
    @PostMapping("/{id}/recibir") public ResponseEntity<?> recibir(@PathVariable Integer id,@Valid @RequestBody RecibirIncidenciaDTO dto){return respuesta(()->servicio.recibir(id,dto));}
    @PostMapping("/{id}/ubicar") public ResponseEntity<?> ubicar(@PathVariable Integer id,@Valid @RequestBody RecibirIncidenciaDTO dto){return respuesta(()->servicio.ubicar(id,dto));}
    @PostMapping("/{id}/baja-perdida") public ResponseEntity<?> baja(@PathVariable Integer id,@Valid @RequestBody PerdidaDefinitivaDTO dto){return respuesta(()->servicio.perdidaDefinitiva(id,dto,false));}
    @PostMapping("/{id}/eliminar-perdida") public ResponseEntity<?> eliminar(@PathVariable Integer id,@Valid @RequestBody PerdidaDefinitivaDTO dto){return respuesta(()->servicio.perdidaDefinitiva(id,dto,true));}
    private ResponseEntity<?> respuesta(Supplier<?> accion){
        try{return ResponseEntity.ok(accion.get());}
        catch(ResponseStatusException e){return ResponseEntity.status(e.getStatus()).body(e.getReason());}
    }
}
