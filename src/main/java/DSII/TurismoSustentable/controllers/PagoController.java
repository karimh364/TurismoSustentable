package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.ResultadoAnticipoDTO;
import DSII.TurismoSustentable.dto.ResultadoAdicionalDTO;
import DSII.TurismoSustentable.dto.SaldoSimuladoDTO;
import DSII.TurismoSustentable.services.PagoService;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("refactor")
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/saldo-simulado")
    public SaldoSimuladoDTO consultarSaldoSimulado() {
        return pagoService.consultarSaldoSimulado();
    }

    @PostMapping("/anticipo/{alquilerId}")
    public ResponseEntity<?> iniciarAnticipo(
            @PathVariable Integer alquilerId,
            @RequestHeader("Idempotency-Key") String claveSolicitud
    ) {
        try {
            ResultadoAnticipoDTO resultado =
                    pagoService.iniciarAnticipo(
                            alquilerId,
                            claveSolicitud
                    );

            return ResponseEntity.ok(resultado);

        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatus())
                    .body(ex.getReason());
        }
    }

    @PostMapping("/adicional/{alquilerId}")
    public ResponseEntity<?> iniciarAdicional(
            @PathVariable("alquilerId") Integer alquilerId,
            @RequestHeader("Idempotency-Key") String claveSolicitud
    ) {
        try {
            ResultadoAdicionalDTO resultado = pagoService.iniciarAdicional(alquilerId, claveSolicitud);
            return ResponseEntity.ok(resultado);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getReason());
        }
    }

    @PostMapping("/adicional/{alquilerId}/resolver")
    public ResponseEntity<?> resolverAdicional(
            @PathVariable("alquilerId") Integer alquilerId,
            @RequestHeader("Idempotency-Key") String claveSolicitud
    ) {
        try {
            ResultadoAdicionalDTO resultado = pagoService.resolverAdicional(alquilerId, claveSolicitud);
            return ResponseEntity.ok(resultado);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getReason());
        }
    }

    @PostMapping("/anticipo/{alquilerId}/resolver")
    public ResponseEntity<?> resolverAnticipo(
            @PathVariable Integer alquilerId,
            @RequestHeader("Idempotency-Key") String claveSolicitud
    ) {
        try {
            ResultadoAnticipoDTO resultado =
                    pagoService.resolverAnticipo(
                            alquilerId,
                            claveSolicitud
                    );

            return ResponseEntity.ok(resultado);

        } catch (ResponseStatusException ex) {
            return ResponseEntity
                    .status(ex.getStatus())
                    .body(ex.getReason());
        }
    }
}
