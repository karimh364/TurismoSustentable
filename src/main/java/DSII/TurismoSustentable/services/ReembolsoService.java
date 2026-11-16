package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Profile("refactor")
public class ReembolsoService {
    private static final BigDecimal CERO = new BigDecimal("0.00");
    private final ReembolsoRepository reembolsos;
    private final PagoRepository pagos;
    private final AlquilerRepository alquileres;
    private final UsuarioRepository usuarios;
    private final HistorialEstadoReembolsoRepository historias;
    private final LiquidacionAlquilerService liquidaciones;
    private final PasarelaSimulada pasarela;
    private final Clock reloj;

    public ReembolsoService(ReembolsoRepository reembolsos, PagoRepository pagos,
            AlquilerRepository alquileres, UsuarioRepository usuarios,
            HistorialEstadoReembolsoRepository historias, LiquidacionAlquilerService liquidaciones,
            PasarelaSimulada pasarela, Clock reloj) {
        this.reembolsos = reembolsos; this.pagos = pagos; this.alquileres = alquileres;
        this.usuarios = usuarios; this.historias = historias; this.liquidaciones = liquidaciones;
        this.pasarela = pasarela; this.reloj = reloj;
    }

    // No es un endpoint: requiere cabecera bloqueada por el caso de uso que cierra o cobra.
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarPendientesSiCorresponde(Alquiler alquiler) {
        if (alquiler.getFechaHoraLiquidacion() == null) return;
        LiquidacionAlquilerDTO resumen = liquidaciones.resumenInterno(alquiler);
        BigDecimal debido = importe(resumen.getReembolsoPendienteArs());
        List<Reembolso> existentes = reembolsos.findByPagoAlquilerIdOrderByIdAsc(alquiler.getId());
        Map<Integer, BigDecimal> ocupadosPorPago = new HashMap<>();
        BigDecimal representado = CERO;
        for (Reembolso r : existentes) {
            validarRegistro(r, alquiler);
            ocupadosPorPago.merge(r.getPago().getId(), importe(r.getMontoArs()), BigDecimal::add);
            if (!"CONFIRMADO".equals(r.getEstado())) representado = representado.add(importe(r.getMontoArs()));
        }
        if (representado.compareTo(debido) > 0) {
            throw interno("Los reembolsos sin completar superan la obligación del alquiler.");
        }
        BigDecimal faltante = debido.subtract(representado);
        for (Pago pago : pagos.findByAlquilerIdOrderByIdAsc(alquiler.getId())) {
            if (pago.getEstadoPago() == null || !"APROBADO".equals(pago.getEstadoPago().getCodigo())) continue;
            BigDecimal disponible = importe(pago.getMontoArs()).subtract(
                    ocupadosPorPago.getOrDefault(pago.getId(), CERO));
            if (disponible.signum() < 0) throw interno("Los reembolsos superan el pago de origen.");
            BigDecimal monto = faltante.min(disponible);
            if (monto.signum() > 0) {
                Reembolso r = reembolsos.saveAndFlush(new Reembolso(pago, monto, ahora(),
                        "Ajuste de la liquidación del alquiler " + alquiler.getId(),
                        "AJUSTE:" + alquiler.getId() + ":" + pago.getId() + ":" + UUID.randomUUID()));
                historias.save(new HistorialEstadoReembolso(r, null, "PENDIENTE", r.getFechaCreacion(),
                        null, null, "SISTEMA", "AJUSTE_LIQUIDACION", null,
                        "Obligación registrada; todavía no se devolvió dinero."));
                faltante = faltante.subtract(monto);
            }
        }
        if (faltante.signum() != 0) throw interno("No hay pagos confirmados suficientes para respaldar el reembolso.");
        reembolsos.flush();
    }

    @PreAuthorize("hasAnyAuthority('CLIENTE','ADMIN')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ResultadoReembolsoDTO consultar(Integer alquilerId) {
        Usuario actor = actor(false, true);
        Alquiler alquiler = obtener(alquilerId, false);
        if (!esRol(actor, "ADMIN") && alquiler.getUsuario().getId() != actor.getId()) {
            throw new AccessDeniedException("No podés consultar reembolsos de un alquiler ajeno.");
        }
        return respuesta(alquiler);
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultadoReembolsoDTO resolver(Integer alquilerId) {
        Usuario actor = actor(true, false);
        Alquiler alquiler = obtener(alquilerId, true);
        if (alquiler.getUsuario().getId() != actor.getId()) {
            throw new AccessDeniedException("Solo el cliente titular puede gestionar su reintegro.");
        }
        if (alquiler.getFechaHoraLiquidacion() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El alquiler todavía no está liquidado.");
        }
        registrarPendientesSiCorresponde(alquiler);
        List<Reembolso> lista = new ArrayList<>(reembolsos.findByPagoAlquilerIdOrderByIdAsc(alquilerId));
        lista.sort(Comparator.comparing((Reembolso r) -> r.getPago().getId()).thenComparing(Reembolso::getId));
        for (Reembolso candidato : lista) {
            Pago pago = pagos.buscarPorIdConBloqueo(candidato.getPago().getId())
                    .orElseThrow(() -> interno("Falta el pago del reembolso."));
            Reembolso r = reembolsos.buscarPorIdConBloqueo(candidato.getId())
                    .orElseThrow(() -> interno("Falta el reembolso solicitado."));
            validarRegistro(r, alquiler);
            if ("CONFIRMADO".equals(r.getEstado())) continue;
            if (!"SIMULADA".equals(pago.getPlataforma()) || !"PRUEBA".equals(pago.getEntorno())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta operación solo admite reembolsos simulados.");
            }
            if (!"EN_PROCESO".equals(r.getEstado())) {
                r.setReferenciaExterna("SIM-REEMBOLSO-" + r.getId());
                cambiar(r, "EN_PROCESO", "ENVIO_SIMULADO");
            } else if (!("SIM-REEMBOLSO-" + r.getId()).equals(r.getReferenciaExterna())) {
                throw interno("El reembolso en gestión no tiene una correlación válida.");
            }
            String resultado = pasarela.evaluarReembolso(r);
            if (!Set.of("EN_PROCESO", "CONFIRMADO", "FALLIDO").contains(resultado == null ? "" : resultado)) {
                throw interno("Resultado de reembolso desconocido.");
            }
            cambiar(r, resultado, "PASARELA_SIMULADA");
        }
        reembolsos.flush();
        return respuesta(alquiler);
    }

    private void cambiar(Reembolso r, String estado, String fuente) {
        if (estado.equals(r.getEstado())) return;
        String anterior = r.getEstado();
        LocalDateTime fecha = ahora();
        if (fecha.isBefore(r.getFechaCreacion())) throw interno("El reloj retrocedió durante el reembolso.");
        r.setEstado(estado);
        if ("CONFIRMADO".equals(estado)) r.setFechaConfirmacion(fecha);
        historias.save(new HistorialEstadoReembolso(r, anterior, estado, fecha, null, null,
                "SISTEMA", fuente, null, "Resultado simulado; se conserva el pago original."));
    }

    private void validarRegistro(Reembolso r, Alquiler alquiler) {
        if (r == null || r.getPago() == null || r.getPago().getAlquiler().getId() != alquiler.getId()
                || r.getPago().getEstadoPago() == null || !"APROBADO".equals(r.getPago().getEstadoPago().getCodigo())
                || r.getPago().getFechaConfirmacion() == null || !"ARS".equals(r.getMoneda())
                || !Set.of("PENDIENTE", "EN_PROCESO", "CONFIRMADO", "FALLIDO").contains(r.getEstado() == null ? "" : r.getEstado())
                || r.getFechaCreacion() == null || r.getFechaCreacion().isAfter(ahora())
                || r.getFechaCreacion().isBefore(r.getPago().getFechaConfirmacion())
                || importe(r.getMontoArs()).signum() == 0) throw interno("Registro de reembolso inconsistente.");
        if ("CONFIRMADO".equals(r.getEstado())) {
            if (r.getFechaConfirmacion() == null || r.getFechaConfirmacion().isBefore(r.getFechaCreacion())
                    || r.getFechaConfirmacion().isAfter(ahora())) throw interno("Confirmación de reembolso inválida.");
        } else if (r.getFechaConfirmacion() != null) throw interno("Un reembolso no confirmado tiene fecha de confirmación.");
    }

    private ResultadoReembolsoDTO respuesta(Alquiler a) {
        return new ResultadoReembolsoDTO(liquidaciones.resumenInterno(a),
                reembolsos.findByPagoAlquilerIdOrderByIdAsc(a.getId()).stream().map(ReembolsoDTO::new).collect(Collectors.toList()));
    }
    private Usuario actor(boolean bloquear, boolean lecturaAdmin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Debe iniciar sesión.");
        Usuario u = (bloquear ? usuarios.buscarActivoPorEmailConBloqueo(auth.getName())
                : usuarios.buscarPorEmailConDatosYRoles(auth.getName())).filter(Usuario::isActivo)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no está activa."));
        if (!esRol(u, "CLIENTE") && !(lecturaAdmin && esRol(u, "ADMIN"))) {
            throw new AccessDeniedException("La cuenta no tiene el rol vigente requerido.");
        }
        return u;
    }
    private boolean esRol(Usuario u, String rol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> rol.equals(a.getAuthority()))
                && u.getUsuarioRoles().stream().anyMatch(ur -> ur.getRol() != null && rol.equals(ur.getRol().getCodigo()));
    }
    private Alquiler obtener(Integer id, boolean bloquear) {
        if (id == null || id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del alquiler no es válido.");
        Alquiler a = (bloquear ? alquileres.buscarPorIdConBloqueo(id) : alquileres.findById(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El alquiler no existe."));
        if (a.getEstadoContratacion() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Este alquiler pertenece al circuito anterior.");
        if (a.getUsuario() == null) throw interno("Falta el titular del alquiler.");
        return a;
    }
    private BigDecimal importe(BigDecimal monto) {
        try {
            if (monto == null || monto.signum() < 0) throw interno("Importe financiero inválido.");
            return monto.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) { throw interno("Importe con precisión inválida."); }
    }
    private LocalDateTime ahora() { return LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC); }
    private ConfiguracionSistemaException interno(String mensaje) { return new ConfiguracionSistemaException(mensaje); }
}
