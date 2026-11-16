package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.LiquidacionAlquilerDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class LiquidacionAlquilerService {

    private static final BigDecimal CERO = new BigDecimal("0.00");

    private static final Set<String> TERMINALES = Set.of(
            "DEVUELTO",
            "CANCELADO",
            "VENCIDO",
            "CERRADO_POR_INCIDENCIA"
    );

    private static final Set<String> ESTADOS_DETALLE = Set.of(
            "RESERVADO",
            "PENDIENTE_RETIRO",
            "EN_CURSO",
            "DEVUELTO",
            "CANCELADO",
            "VENCIDO",
            "CERRADO_POR_INCIDENCIA"
    );

    private static final Set<String> CABECERAS_CERRADAS = Set.of(
            "FINALIZADO", "CANCELADO", "VENCIDO"
    );

    private final AlquilerRepository alquilerRepository;
    private final AlquilerDetalleRepository detalleRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock reloj;

    public LiquidacionAlquilerService(
            AlquilerRepository alquilerRepository,
            AlquilerDetalleRepository detalleRepository,
            IncidenciaRepository incidenciaRepository,
            PagoRepository pagoRepository,
            ReembolsoRepository reembolsoRepository,
            UsuarioRepository usuarioRepository,
            Clock reloj
    ) {
        this.alquilerRepository = alquilerRepository;
        this.detalleRepository = detalleRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.pagoRepository = pagoRepository;
        this.reembolsoRepository = reembolsoRepository;
        this.usuarioRepository = usuarioRepository;
        this.reloj = reloj;
    }

    /**
     * Operación interna del backend.
     *
     * El caso de uso autorizado debe entregar una cabecera administrada
     * por JPA y bloqueada para escritura, dentro de su transacción.
     * No debe publicarse como una petición que reciba una entidad del navegador.
     *
     * Devuelve false cuando todavía no corresponde liquidar.
     */
    @Transactional(
            propagation = Propagation.MANDATORY,
            rollbackFor = Exception.class
    )
    public boolean liquidarSiCorresponde(Alquiler alquiler) {
        if (alquiler == null
                || alquiler.getId() <= 0
                || alquiler.getEstadoContratacion() == null) {
            throw inconsistencia("Falta la cabecera del nuevo circuito.");
        }

        List<AlquilerDetalle> detalles =
                detalleRepository.buscarPorAlquiler(alquiler.getId());

        if (detalles.isEmpty()) {
            throw inconsistencia("El alquiler no tiene detalles.");
        }

        boolean quedanPendientes = false;
        boolean revisionEconomicaPendiente = false;
        Set<Integer> identificadores = new HashSet<>();

        for (AlquilerDetalle detalle : detalles) {
            if (detalle == null
                    || detalle.getId() == null
                    || !identificadores.add(detalle.getId())
                    || detalle.getAlquiler() == null
                    || detalle.getAlquiler().getId() != alquiler.getId()
                    || detalle.getEstado() == null) {
                throw inconsistencia("Hay detalles inválidos o duplicados.");
            }

            String estado = detalle.getEstado().getCodigo();

            if (estado == null || !ESTADOS_DETALLE.contains(estado)) {
                throw inconsistencia("Un detalle tiene un estado desconocido.");
            }

            if (!TERMINALES.contains(estado)) {
                quedanPendientes = true;
            }

            for (Incidencia incidencia : incidenciaRepository
                    .findByAlquilerDetalleIdOrderByIdAsc(detalle.getId())) {

                String revision = incidencia.getEstadoRevision();

                if (revision == null || !Set.of(
                        "PENDIENTE", "VALIDADA", "DESCARTADA"
                ).contains(revision)) {
                    throw inconsistencia(
                            "Una incidencia tiene una revisión inválida."
                    );
                }

                if ("PENDIENTE".equals(revision)
                        && incidencia.isRequiereRevisionEconomica()) {
                    revisionEconomicaPendiente = true;
                }
            }
        }

        boolean cabeceraCerrada = CABECERAS_CERRADAS.contains(
                alquiler.getEstadoContratacion().getCodigo()
        );

        if (!cabeceraCerrada
                || quedanPendientes
                || revisionEconomicaPendiente) {

            if (alquiler.getFechaHoraLiquidacion() != null
                    || alquiler.getCostoFinalUsd() != null
                    || alquiler.getCostoFinalArs() != null) {
                throw inconsistencia(
                        "Hay una liquidación incompatible con asuntos pendientes."
                );
            }

            return false;
        }

        LocalDateTime ahora = LocalDateTime.ofInstant(
                reloj.instant(), ZoneOffset.UTC
        );

        if (alquiler.getFechaHoraCierre() == null
                || alquiler.getFechaHoraCierre().isAfter(ahora)) {
            throw inconsistencia("La fecha de cierre es inválida.");
        }

        boolean huboRetiro = detalles.stream()
                .anyMatch(d -> d.getFechaHoraRetiro() != null);

        boolean finalizado = "FINALIZADO".equals(
                alquiler.getEstadoContratacion().getCodigo()
        );

        if (huboRetiro != finalizado) {
            throw inconsistencia(
                    "El estado de cabecera no coincide con los retiros."
            );
        }

        BigDecimal totalUsd = CERO;
        BigDecimal totalArs = CERO;

        for (AlquilerDetalle detalle : detalles) {
            validarCierreDetalle(detalle, ahora);

            BigDecimal subtotalUsd = importe(
                    detalle.getSubtotalRealUsd(), 18
            );
            BigDecimal subtotalArs = importe(
                    detalle.getSubtotalRealArs(), 18
            );

            if (Set.of("CANCELADO", "VENCIDO").contains(
                    detalle.getEstado().getCodigo()
            ) && (subtotalUsd.signum() != 0 || subtotalArs.signum() != 0)) {
                throw inconsistencia(
                        "Una unidad sin retiro no puede tener costo de uso."
                );
            }

            totalUsd = totalUsd.add(subtotalUsd);
            totalArs = totalArs.add(subtotalArs);
        }

        totalUsd = importe(totalUsd, 19);
        totalArs = importe(totalArs, 19);

        // Repetir no altera la fecha ni los importes ya fijados.
        if (alquiler.getFechaHoraLiquidacion() != null) {
            if (importe(alquiler.getCostoFinalUsd(), 19)
                    .compareTo(totalUsd) != 0
                    || importe(alquiler.getCostoFinalArs(), 19)
                    .compareTo(totalArs) != 0
                    || alquiler.getFechaHoraLiquidacion()
                    .isBefore(alquiler.getFechaHoraCierre())
                    || alquiler.getFechaHoraLiquidacion().isAfter(ahora)) {
                throw inconsistencia(
                        "La liquidación guardada no coincide con sus detalles."
                );
            }

            return true;
        }

        if (alquiler.getCostoFinalUsd() != null
                || alquiler.getCostoFinalArs() != null) {
            throw inconsistencia(
                    "Hay importes finales sin fecha de liquidación."
            );
        }

        alquiler.setCostoFinalUsd(totalUsd);
        alquiler.setCostoFinalArs(totalArs);
        alquiler.setFechaHoraLiquidacion(ahora);

        alquilerRepository.flush();
        return true;
    }

    @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')")
    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public LiquidacionAlquilerDTO consultarLiquidacion(Integer alquilerId) {
        if (alquilerId == null || alquilerId <= 0) {
            throw new IllegalArgumentException(
                    "Debe indicar un alquiler válido."
            );
        }

        Usuario usuario = obtenerUsuarioConsulta();

        Alquiler alquiler = alquilerRepository.findById(alquilerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "El alquiler no existe."
                ));

        if (alquiler.getUsuario() == null) {
            throw inconsistencia("El alquiler no tiene titular.");
        }

        if (!tieneRolVigente(usuario, "ADMIN")
                && alquiler.getUsuario().getId() != usuario.getId()) {
            throw new AccessDeniedException(
                    "No podés consultar la liquidación de un alquiler ajeno."
            );
        }

        if (alquiler.getEstadoContratacion() == null) {
            throw new IllegalStateException(
                    "Este alquiler pertenece al circuito anterior."
            );
        }

        return crearResumen(alquiler);
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<LiquidacionAlquilerDTO> listarPendientesPropios() {
        Usuario usuario = obtenerUsuarioConsulta();
        if (!tieneRolVigente(usuario, "CLIENTE")) throw new AccessDeniedException("Se requiere rol CLIENTE vigente.");
        List<LiquidacionAlquilerDTO> pendientes = new java.util.ArrayList<>();
        for (Alquiler alquiler : alquilerRepository.buscarLiquidacionesPorUsuario(usuario.getId())) {
            LiquidacionAlquilerDTO resumen = crearResumen(alquiler);
            if (!"SALDADO".equals(resumen.getSituacionFinanciera())) pendientes.add(resumen);
        }
        return pendientes;
    }

    // Uso interno con cabecera bloqueada: scheduler y cierres no inventan una sesión humana.
    @Transactional(propagation = Propagation.MANDATORY)
    public LiquidacionAlquilerDTO resumenInterno(Alquiler alquiler) {
        if (alquiler == null || alquiler.getId() <= 0 || alquiler.getEstadoContratacion() == null) {
            throw inconsistencia("Falta la cabecera del nuevo circuito.");
        }
        return crearResumen(alquiler);
    }

    private LiquidacionAlquilerDTO crearResumen(Alquiler alquiler) {
        BigDecimal pagosAprobados = CERO;
        Map<Integer, BigDecimal> aprobadosPorPago = new HashMap<>();
        Set<Integer> pagosContados = new HashSet<>();

        for (Pago pago : pagoRepository
                .findByAlquilerIdOrderByIdAsc(alquiler.getId())) {

            if (pago == null
                    || pago.getId() == null
                    || !pagosContados.add(pago.getId())
                    || pago.getAlquiler() == null
                    || pago.getAlquiler().getId() != alquiler.getId()
                    || pago.getEstadoPago() == null) {
                throw inconsistencia("La consulta contiene pagos inválidos.");
            }

            String estado = pago.getEstadoPago().getCodigo();

            if (estado == null || !Set.of(
                    "CREADO", "PENDIENTE", "APROBADO",
                    "RECHAZADO", "CANCELADO"
            ).contains(estado)) {
                throw inconsistencia("Un pago tiene un estado desconocido.");
            }

            if ("APROBADO".equals(estado)) {
                BigDecimal monto = movimientoConfirmado(
                        pago.getMontoArs(),
                        pago.getMoneda(),
                        pago.getFechaCreacion(),
                        pago.getFechaConfirmacion()
                );

                aprobadosPorPago.put(pago.getId(), monto);
                pagosAprobados = pagosAprobados.add(monto);
            }
        }

        BigDecimal reembolsosConfirmados = CERO;
        Map<Integer, BigDecimal> devueltoPorPago = new HashMap<>();
        Set<Integer> reembolsosContados = new HashSet<>();

        for (Reembolso reembolso : reembolsoRepository
                .findByPagoAlquilerIdOrderByIdAsc(alquiler.getId())) {

            if (reembolso == null
                    || reembolso.getId() == null
                    || !reembolsosContados.add(reembolso.getId())
                    || reembolso.getPago() == null
                    || reembolso.getPago().getAlquiler() == null
                    || reembolso.getPago().getAlquiler().getId()
                    != alquiler.getId()) {
                throw inconsistencia(
                        "La consulta contiene reembolsos inválidos."
                );
            }

            String estado = reembolso.getEstado();

            if (estado == null || !Set.of(
                    "PENDIENTE", "EN_PROCESO", "CONFIRMADO", "FALLIDO"
            ).contains(estado)) {
                throw inconsistencia(
                        "Un reembolso tiene un estado desconocido."
                );
            }

            if (!"CONFIRMADO".equals(estado)) {
                continue;
            }

            Integer pagoId = reembolso.getPago().getId();
            BigDecimal pagoOriginal = aprobadosPorPago.get(pagoId);

            if (pagoOriginal == null) {
                throw inconsistencia(
                        "El reembolso no corresponde a un pago aprobado."
                );
            }

            BigDecimal monto = movimientoConfirmado(
                    reembolso.getMontoArs(),
                    reembolso.getMoneda(),
                    reembolso.getFechaCreacion(),
                    reembolso.getFechaConfirmacion()
            );

            BigDecimal acumulado = devueltoPorPago
                    .getOrDefault(pagoId, CERO)
                    .add(monto);

            if (acumulado.compareTo(pagoOriginal) > 0) {
                throw inconsistencia(
                        "Los reembolsos confirmados superan el pago original."
                );
            }

            devueltoPorPago.put(pagoId, acumulado);
            reembolsosConfirmados = reembolsosConfirmados.add(monto);
        }

        BigDecimal neto = pagosAprobados.subtract(reembolsosConfirmados);

        if (alquiler.getFechaHoraLiquidacion() == null) {
            if (alquiler.getCostoFinalUsd() != null
                    || alquiler.getCostoFinalArs() != null) {
                throw inconsistencia(
                        "Hay costos finales sin fecha de liquidación."
                );
            }

            return new LiquidacionAlquilerDTO(
                    alquiler.getId(),
                    alquiler.getVersion(),
                    "SIN_LIQUIDAR",
                    null,
                    null,
                    null,
                    pagosAprobados,
                    reembolsosConfirmados,
                    neto,
                    null,
                    null
            );
        }

        if (!CABECERAS_CERRADAS.contains(
                alquiler.getEstadoContratacion().getCodigo()
        )) {
            throw inconsistencia(
                    "La liquidación requiere una cabecera cerrada."
            );
        }

        BigDecimal costoUsd = importe(alquiler.getCostoFinalUsd(), 19);
        BigDecimal costoArs = importe(alquiler.getCostoFinalArs(), 19);
        BigDecimal diferencia = costoArs.subtract(neto);

        String situacion = diferencia.signum() > 0
                ? "SALDO_PENDIENTE"
                : diferencia.signum() < 0
                ? "REEMBOLSO_PENDIENTE"
                : "SALDADO";

        return new LiquidacionAlquilerDTO(
                alquiler.getId(),
                alquiler.getVersion(),
                situacion,
                alquiler.getFechaHoraLiquidacion(),
                costoUsd,
                costoArs,
                pagosAprobados,
                reembolsosConfirmados,
                neto,
                diferencia.max(CERO),
                diferencia.negate().max(CERO)
        );
    }

    private void validarCierreDetalle(
            AlquilerDetalle detalle,
            LocalDateTime ahora
    ) {
        LocalDateTime cierre = detalle.getFechaHoraCierreDetalle();

        if (cierre == null || cierre.isAfter(ahora)) {
            throw inconsistencia("La fecha de cierre de un detalle es inválida.");
        }

        String estado = detalle.getEstado().getCodigo();

        if (Set.of("CANCELADO", "VENCIDO").contains(estado)) {
            if (detalle.getFechaHoraRetiro() != null
                    || detalle.getFechaHoraFinUso() != null
                    || detalle.getFechaHoraDevolucionReal() != null) {
                throw inconsistencia(
                        "Un detalle sin retiro tiene fechas de uso."
                );
            }
            return;
        }

        LocalDateTime retiro = detalle.getFechaHoraRetiro();
        LocalDateTime fin = detalle.getFechaHoraFinUso();

        if (retiro == null || fin == null
                || fin.isBefore(retiro) || cierre.isBefore(fin)) {
            throw inconsistencia("Las fechas del uso son inconsistentes.");
        }

        if ("DEVUELTO".equals(estado)) {
            LocalDateTime recepcion = detalle.getFechaHoraDevolucionReal();

            if (recepcion == null
                    || detalle.getEstacionDevolucion() == null
                    || fin.isAfter(recepcion)
                    || cierre.isBefore(recepcion)) {
                throw inconsistencia(
                        "Faltan datos coherentes de la devolución."
                );
            }

            if (fin.isBefore(recepcion) || detalle.getIncidenciaCorte() != null) {
                validarCorte(detalle, ahora);
            }
        } else {
            if (detalle.getFechaHoraDevolucionReal() != null
                    || detalle.getEstacionDevolucion() != null) {
                throw inconsistencia(
                        "Un cierre excepcional no debe inventar una devolución."
                );
            }
            validarCorte(detalle, ahora);
        }
    }

    private void validarCorte(
            AlquilerDetalle detalle,
            LocalDateTime ahora
    ) {
        Incidencia incidencia = detalle.getIncidenciaCorte();

        if (incidencia == null
                || !"VALIDADA".equals(incidencia.getEstadoRevision())
                || incidencia.getAlquilerDetalle() == null
                || !Objects.equals(
                incidencia.getAlquilerDetalle().getId(),
                detalle.getId()
        )
                || incidencia.getFechaHoraCorteValidado() == null
                || !incidencia.getFechaHoraCorteValidado()
                .equals(detalle.getFechaHoraFinUso())
                || incidencia.getFechaHoraRegistro() == null
                || incidencia.getFechaHoraDecision() == null
                || incidencia.getResueltoPor() == null
                || incidencia.getMotivoDecision() == null
                || incidencia.getMotivoDecision().isBlank()
                || incidencia.getFechaHoraCorteValidado()
                .isAfter(incidencia.getFechaHoraRegistro())
                || incidencia.getFechaHoraRegistro()
                .isAfter(incidencia.getFechaHoraDecision())
                || incidencia.getFechaHoraDecision().isAfter(ahora)) {
            throw inconsistencia(
                    "El corte de uso no tiene una decisión válida."
            );
        }
    }

    private Usuario obtenerUsuarioConsulta() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Debe iniciar sesión.");
        }

        Usuario usuario = usuarioRepository
                .buscarPorEmailConDatosYRoles(auth.getName())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new AccessDeniedException(
                        "La cuenta no existe o está inactiva."
                ));

        if (!tieneRolVigente(usuario, "CLIENTE")
                && !tieneRolVigente(usuario, "ADMIN")) {
            throw new AccessDeniedException(
                    "No tiene permiso vigente para consultar liquidaciones."
            );
        }

        return usuario;
    }

    private boolean tieneRolVigente(Usuario usuario, String codigo) {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        return auth != null
                && auth.getAuthorities().stream()
                .anyMatch(a -> codigo.equals(a.getAuthority()))
                && usuario.getUsuarioRoles().stream()
                .anyMatch(ur -> ur.getRol() != null
                        && codigo.equals(ur.getRol().getCodigo()));
    }

    private BigDecimal movimientoConfirmado(
            BigDecimal monto,
            String moneda,
            LocalDateTime creacion,
            LocalDateTime confirmacion
    ) {
        BigDecimal normalizado = importe(monto, 19);

        if (normalizado.signum() == 0
                || !"ARS".equals(moneda)
                || creacion == null
                || confirmacion == null
                || confirmacion.isBefore(creacion)) {
            throw inconsistencia(
                    "Un movimiento confirmado tiene datos inconsistentes."
            );
        }

        return normalizado;
    }

    private BigDecimal importe(BigDecimal valor, int enteros) {
        if (valor == null || valor.signum() < 0) {
            throw inconsistencia("Hay un importe nulo o negativo.");
        }

        final BigDecimal normalizado;
        try {
            normalizado = valor.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw inconsistencia("Un importe tiene decimales incompatibles.");
        }

        if (normalizado.precision() - normalizado.scale() > enteros) {
            throw inconsistencia("Un importe supera la capacidad permitida.");
        }

        return normalizado;
    }

    private ConfiguracionSistemaException inconsistencia(String mensaje) {
        return new ConfiguracionSistemaException(mensaje);
    }
}
