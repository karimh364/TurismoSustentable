package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.ActualizarEstacionDTO;
import DSII.TurismoSustentable.dto.CrearEstacionDTO;
import DSII.TurismoSustentable.dto.EstacionDTO;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.services.EstacionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EstacionController {

    @Autowired
    private EstacionRepository estacionRepository;

    @Autowired
    private BicicletaRepository bicicletaRepository;

    @Autowired
    private EstacionService estacionService;

    // Lectura pública: únicamente estaciones públicas activas.
    @GetMapping("/estaciones")
    public List<EstacionDTO> getEstaciones() {
        return estacionService.obtenerEstacionesPublicas();
    }

    // Lectura administrativa: incluye el Taller.
    @GetMapping("/admin/estaciones")
    public List<EstacionDTO> getEstacionesAdministracion() {
        return estacionService.obtenerEstacionesAdministracion();
    }

    // GET por ID
    @GetMapping("/estaciones/{id}")
    public ResponseEntity<EstacionDTO> getEstacionPorId(@PathVariable Integer id) {

        Optional<Estacion> estacionOpt = estacionRepository.findById(id);

        if (estacionOpt.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Estacion estacion = estacionOpt.get();

        // Filtro de seguridad: Si está borrada lógicamente, devuelve 404
        if (estacion.isBorradoEstacion()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(new EstacionDTO(estacion));
    }

    // POST /api/estaciones: Crear nueva estación (Solo ADMIN)
    @PostMapping("/estaciones")
    public ResponseEntity<Object> crearEstacion(
            @Valid @RequestBody CrearEstacionDTO datos
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(estacionService.crearEstacion(datos));

        } catch (ConstraintViolationException e) {
            String mensaje = e.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(" "));

            return ResponseEntity.badRequest().body(mensaje);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Los datos entran en conflicto con otra estación. " +
                                    "Revisá el nombre."
                    );
        }
    }

    // DELETE /api/estaciones/{id}: Borrado Lógico (Solo ADMIN)
    @DeleteMapping("/estaciones/{id}")
    @Transactional
    public ResponseEntity<Object> eliminarEstacionLogico(
            @PathVariable Integer id
    ) {

        /*
         * Bloquear la estación para que ninguna bicicleta pueda
         * ingresar mientras se está comprobando y eliminando.
         */
        Optional<Estacion> estacionOptional = estacionRepository
                .buscarActivaPorIdConBloqueo(id);

        if (estacionOptional.isEmpty()) {
            return new ResponseEntity<>(
                    "La estación con ID "
                            + id
                            + " no existe o está inactiva.",
                    HttpStatus.NOT_FOUND
            );
        }

        Estacion estacion = estacionOptional.get();

        // Contar todas las bicicletas que permanecen físicamente allí.
        long bicicletasFisicas = bicicletaRepository
                .countByUbicacionActual(estacion);

        if (bicicletasFisicas > 0) {
            return new ResponseEntity<>(
                    "No se puede eliminar la estación porque contiene "
                            + bicicletasFisicas
                            + " bicicletas. "
                            + "Primero debe moverlas a otra estación.",
                    HttpStatus.CONFLICT
            );
        }

        estacion.setBorradoEstacion(true);
        estacionRepository.save(estacion);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // PUT /api/estaciones/{id}: Actualizar estación (Solo ADMIN)
    @PutMapping("/estaciones/{id}")
    public ResponseEntity<Object> actualizarEstacion(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarEstacionDTO datos
    ) {
        try {
            return ResponseEntity.ok(
                    estacionService.actualizarEstacion(id, datos)
            );

        } catch (ConstraintViolationException e) {
            String mensaje = e.getConstraintViolations().stream()
                    .map(v -> v.getMessage())
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(" "));

            return ResponseEntity.badRequest().body(mensaje);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());

        } catch (OptimisticLockingFailureException
                 | OptimisticLockException e) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "La estación fue modificada por otra operación. " +
                                    "Recargá sus datos e intentá nuevamente."
                    );

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            "Los datos entran en conflicto con otra estación. " +
                                    "Revisá el nombre."
                    );
        }
    }
}
