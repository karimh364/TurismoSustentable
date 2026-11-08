package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.services.BicicletaService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BicicletaController {
    private final BicicletaService service;
    public BicicletaController(BicicletaService service) { this.service = service; }

    @GetMapping("/bicicletas")
    public List<BicicletaDTO> getBicicletas(
            @RequestParam(name = "incluirEliminadas", defaultValue = "false") boolean incluirEliminadas) {
        return service.listar(incluirEliminadas);
    }

    @GetMapping("/bicicletas/disponibles")
    public List<BicicletaDTO> getBicicletasDisponibles() { return service.disponibles(); }

    @GetMapping("/bicicletas/tipo/{tipo}")
    public List<BicicletaDTO> getBicicletasPorTipo(@PathVariable String tipo) {
        return service.porTipo(tipo);
    }

    @GetMapping("/bicicletas/{id}")
    public BicicletaDTO getBicicletaPorId(@PathVariable Integer id) { return service.obtener(id); }

    @PostMapping("/bicicletas")
    public ResponseEntity<BicicletaDTO> crearBicicleta(@Valid @RequestBody CrearBicicletaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }

    @PutMapping("/bicicletas/{id}")
    public BicicletaDTO actualizarBicicleta(@PathVariable Integer id,
                                          @Valid @RequestBody ActualizarBicicletaDTO dto) {
        return service.actualizar(id, dto);
    }

    @DeleteMapping("/bicicletas/{id}")
    public ResponseEntity<Void> eliminarBicicletaLogico(@PathVariable Integer id,
                                                      @Valid @RequestBody EliminarBicicletaDTO dto) {
        service.eliminarLogico(id, dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/bicicletas/{id}/estado/{codigo}")
    public BicicletaDTO cambiarEstado(@PathVariable Integer id, @PathVariable String codigo,
                                      @Valid @RequestBody OperacionBicicletaDTO dto) {
        return service.cambiarEstado(id, codigo, dto);
    }

    @GetMapping("/admin/tipos-bicicleta")
    public List<TipoBicicletaDTO> tipos() { return service.listarTipos(); }

    @PutMapping("/admin/tipos-bicicleta/{id}/tarifa")
    public TipoBicicletaDTO actualizarTarifa(@PathVariable Integer id,
                                            @Valid @RequestBody ActualizarTarifaTipoDTO dto) {
        return service.actualizarTarifa(id, dto);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> negocio(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatus()).body(error.getReason());
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<String> version(ObjectOptimisticLockingFailureException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Los datos fueron modificados. Recargá antes de guardar.");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> integridad(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Los datos entran en conflicto con un registro existente.");
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> solicitud(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body("La solicitud contiene datos o campos no permitidos. Recargá el formulario.");
    }
}
