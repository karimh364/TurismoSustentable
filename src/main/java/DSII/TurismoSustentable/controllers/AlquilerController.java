package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.AlquilerDTO;
import DSII.TurismoSustentable.dto.CrearAlquilerDTO;
import DSII.TurismoSustentable.dto.CancelarDetalleDTO;
import DSII.TurismoSustentable.dto.DevolverBicicletaDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.services.AlquilerService; // <-- 1. IMPORTA EL SERVICIO
import DSII.TurismoSustentable.services.LiquidacionAlquilerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AlquilerController {

    // --- Inyectamos el Servicio ---
    @Autowired
    private AlquilerService alquilerService;

    @Autowired
    private LiquidacionAlquilerService liquidacionAlquilerService;

    @GetMapping("/alquileres")
    public List<Object> getAlquiler() {
        return alquilerService.listarMisAlquileres();
    }

    @GetMapping("/alquileres/{id}")
    public ResponseEntity<Object> obtenerAlquiler(
            @PathVariable("id") Integer id
    ) {
        try {
            return ResponseEntity.ok(
                    alquilerService.obtenerAlquiler(id)
            );

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/alquileres/{id}/liquidacion")
    public ResponseEntity<Object> consultarLiquidacion(
            @PathVariable("id") Integer id
    ) {
        try {
            return ResponseEntity.ok(
                    liquidacionAlquilerService.consultarLiquidacion(id)
            );

        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ex.getMessage());

        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }

    /**
     * CREAR ALQUILER
     * Ahora solo llama al servicio y maneja la respuesta.
     */
    @PostMapping("/alquileres")
    public ResponseEntity<Object> crearAlquiler(@Valid @RequestBody CrearAlquilerDTO alquilerRequest) {
        try {
            // 2. Llama al servicio (que es transaccional)
            Integer alquilerId = alquilerService.crearReserva(alquilerRequest);

            // 3. Devuelve la respuesta OK
            return ResponseEntity.status(HttpStatus.CREATED).body(alquilerService.obtenerAlquiler(alquilerId));

        } catch (AccessDeniedException
                 | ConfiguracionSistemaException
                 | DataAccessException e) {
            throw e;

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());

        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/alquileres/pendientes-financieros")
    public Object pendientesFinancieros() { return liquidacionAlquilerService.listarPendientesPropios(); }

    @PostMapping({"/alquileres/{alquilerId}/cancelar", "/alquileres/{alquilerId}/detalles/{detalleId}/cancelar"})
    public ResponseEntity<Object> cancelarReserva(
            @PathVariable("alquilerId") Integer alquilerId,
            @PathVariable(value = "detalleId", required = false) Integer detalleId) {
        return cancelar(alquilerId, detalleId, null, false);
    }

    @PostMapping("/admin/alquileres/{alquilerId}/detalles/{detalleId}/cancelar")
    public ResponseEntity<Object> cancelarAdministrativamente(
            @PathVariable("alquilerId") Integer alquilerId, @PathVariable("detalleId") Integer detalleId,
            @Valid @RequestBody CancelarDetalleDTO solicitud) {
        return cancelar(alquilerId, detalleId, solicitud, true);
    }

    private ResponseEntity<Object> cancelar(Integer alquilerId, Integer detalleId,
            CancelarDetalleDTO solicitud, boolean administrativa) {
        try { return ResponseEntity.ok(alquilerService.cancelarReserva(alquilerId, detalleId, solicitud, administrativa)); }
        catch (EntityNotFoundException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage()); }
        catch (IllegalArgumentException e) { return ResponseEntity.badRequest().body(e.getMessage()); }
        catch (IllegalStateException e) { return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage()); }
    }

    // BORRAR ALQUILER
    @DeleteMapping("/alquileres/{id}")
    public ResponseEntity<Object> borrarAlquilerLogico(
            @PathVariable("id") Integer id
    ) {
        try {
            alquilerService.borrarAlquilerLogico(id);

            return ResponseEntity.noContent().build();

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/alquileres/{alquilerId}/detalles/{detalleId}/retirar")
    public ResponseEntity<Object> retirarBicicleta(
            @PathVariable("alquilerId") Integer alquilerId,
            @PathVariable("detalleId") Integer detalleId
    ) {
        try {
            return ResponseEntity.ok(
                    alquilerService.retirarBicicleta(
                            alquilerId,
                            detalleId
                    )
            );

        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ex.getMessage());

        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ex.getMessage());
        }
    }

    @PostMapping("/alquileres/{alquilerId}/detalles/{detalleId}/devolver")
    public ResponseEntity<Object> devolverBicicleta(
            @PathVariable("alquilerId") Integer alquilerId,
            @PathVariable("detalleId") Integer detalleId,
            @Valid @RequestBody DevolverBicicletaDTO solicitud
    ) {
        try {
            return ResponseEntity.ok(alquilerService.devolverBicicleta(
                    alquilerId, detalleId, solicitud
            ));
        } catch (EntityNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }
    }

    // FINALIZAR ALQUILER
    @PatchMapping("/alquileres/finalizar/{id}")
    public ResponseEntity<Object> finalizarAlquiler(
            @PathVariable Integer id,
            @RequestBody Map<String, String> payload) {

        try {
            String ubicacionDestino = payload.get("ubicacionDestino");

            if (ubicacionDestino == null
                    || ubicacionDestino.trim().isEmpty()) {

                return new ResponseEntity<>(
                        "Debe especificar la estación de destino.",
                        HttpStatus.BAD_REQUEST
                );
            }

            AlquilerDTO alquilerFinalizado =
                    alquilerService.finalizarAlquiler(
                            id,
                            ubicacionDestino
                    );

            return ResponseEntity.ok(alquilerFinalizado);

        } catch (ConfiguracionSistemaException e) {
            throw e;
        } catch (AccessDeniedException e) {
            // Intentó finalizar el alquiler de otro usuario
            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.FORBIDDEN
            );

        } catch (IllegalStateException e) {
            // Alquiler finalizado, cancelado o estación llena
            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.CONFLICT
            );

        } catch (IllegalArgumentException e) {
            // Estación inexistente o datos inválidos
            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );

        } catch (Exception e) {
            // Cualquier otro error no previsto
            return new ResponseEntity<>(
                    e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    /**
     * OBTENER COTIZACIÓN DEL DÓLAR
     * El frontend llama a este endpoint para mostrar precios estimativos,
     * evitando consumir APIs externas desde el navegador.
     */
    @GetMapping("/cotizacion")
    public ResponseEntity<BigDecimal> getCotizacion() {
        // Llama al servicio centralizado y devuelve el número
        BigDecimal cotizacion = alquilerService.obtenerCotizacionDolar();
        return ResponseEntity.ok(cotizacion);
    }

}
