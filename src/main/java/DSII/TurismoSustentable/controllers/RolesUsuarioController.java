package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.ActualizarRolesUsuarioDTO;
import DSII.TurismoSustentable.services.RolesUsuarioService;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PessimisticLockException;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/usuarios/{id}/roles")
@PreAuthorize("hasAuthority('ADMIN')")
public class RolesUsuarioController {
    private final RolesUsuarioService roles;
    public RolesUsuarioController(RolesUsuarioService roles) { this.roles = roles; }

    @GetMapping
    public ResponseEntity<?> consultar(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(roles.consultar(id));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getReason());
        }
    }

    @PutMapping
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @Valid @RequestBody ActualizarRolesUsuarioDTO datos) {
        try {
            return ResponseEntity.ok(roles.actualizar(id, datos));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatus()).body(e.getReason());
        } catch (ConcurrencyFailureException | OptimisticLockException | PessimisticLockException | LockTimeoutException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("El usuario está siendo modificado. Recargá los roles antes de guardar.");
        }
    }
}
