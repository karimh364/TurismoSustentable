package DSII.TurismoSustentable.controllers;
import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.services.MantenimientoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/mantenimiento")
public class MantenimientoController {
    private final MantenimientoService servicio;
    public MantenimientoController(MantenimientoService servicio){this.servicio=servicio;}
    @GetMapping("/ordenes") public Object listar(){return servicio.listar();}
    @GetMapping("/ordenes/{id}") public ResponseEntity<?> obtener(@PathVariable Integer id){return respuesta(()->servicio.consultar(id));}
    @GetMapping("/ordenes/{id}/historial") public ResponseEntity<?> historial(@PathVariable Integer id){return respuesta(()->servicio.historial(id));}
    @GetMapping("/tecnicos") public Object tecnicos(){return servicio.tecnicos();}
    @PostMapping("/ordenes") public ResponseEntity<?> crear(@Valid @RequestBody CrearOrdenMantenimientoDTO datos){
        try {return ResponseEntity.status(201).body(servicio.crear(datos));}
        catch(ResponseStatusException e){return ResponseEntity.status(e.getStatus()).body(e.getReason());}
    }
    @PostMapping("/ordenes/{id}/{accion}") public ResponseEntity<?> operar(@PathVariable Integer id,@PathVariable String accion,
            @Valid @RequestBody OperacionMantenimientoDTO datos){return respuesta(()->servicio.operar(id,accion,datos));}
    @PutMapping("/tipos-bicicleta/{id}/intervalo") public ResponseEntity<?> intervalo(@PathVariable Integer id,
            @Valid @RequestBody ActualizarIntervaloPreventivoDTO datos){return respuesta(()->servicio.actualizarIntervalo(id,datos));}
    private ResponseEntity<?> respuesta(Supplier<?> accion){
        try {return ResponseEntity.ok(accion.get());}
        catch(ResponseStatusException e){return ResponseEntity.status(e.getStatus()).body(e.getReason());}
    }
}
