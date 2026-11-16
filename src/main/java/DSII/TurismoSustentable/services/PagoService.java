package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.SaldoSimuladoDTO;
import DSII.TurismoSustentable.dto.LiquidacionAlquilerDTO;
import DSII.TurismoSustentable.dto.ResultadoAdicionalDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.Pago;
import DSII.TurismoSustentable.models.Reembolso;
import DSII.TurismoSustentable.models.Usuario;
import DSII.TurismoSustentable.repositories.PagoRepository;
import DSII.TurismoSustentable.repositories.ReembolsoRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.dto.ResultadoAnticipoDTO;
import DSII.TurismoSustentable.models.Alquiler;
import DSII.TurismoSustentable.models.AlquilerDetalle;
import DSII.TurismoSustentable.models.EstadoPago;
import DSII.TurismoSustentable.models.HistorialEstadoPago;
import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.repositories.AlquilerDetalleRepository;
import DSII.TurismoSustentable.repositories.EstadoPagoRepository;
import DSII.TurismoSustentable.repositories.HistorialEstadoPagoRepository;
import DSII.TurismoSustentable.models.EstadoAlquilerDetalle;
import DSII.TurismoSustentable.models.HistorialEstadoDetalle;
import DSII.TurismoSustentable.models.HistorialEstadoReembolso;
import DSII.TurismoSustentable.repositories.EstadoAlquilerDetalleRepository;
import DSII.TurismoSustentable.repositories.HistorialEstadoDetalleRepository;
import DSII.TurismoSustentable.repositories.HistorialEstadoReembolsoRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("refactor")
public class PagoService {

    private final UsuarioRepository usuarioRepository;
    private final PagoRepository pagoRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final PasarelaSimulada pasarelaSimulada;
    private final AlquilerRepository alquilerRepository;
    private final AlquilerDetalleRepository alquilerDetalleRepository;
    private final EstadoPagoRepository estadoPagoRepository;
    private final HistorialEstadoPagoRepository historialEstadoPagoRepository;
    private final EstadoAlquilerDetalleRepository estadoAlquilerDetalleRepository;
    private final HistorialEstadoDetalleRepository historialEstadoDetalleRepository;
    private final HistorialEstadoReembolsoRepository historialEstadoReembolsoRepository;
    private final LiquidacionAlquilerService liquidacionAlquilerService;
    private final Clock reloj;

    public PagoService(
            UsuarioRepository usuarioRepository,
            PagoRepository pagoRepository,
            ReembolsoRepository reembolsoRepository,
            PasarelaSimulada pasarelaSimulada,
            AlquilerRepository alquilerRepository,
            AlquilerDetalleRepository alquilerDetalleRepository,
            EstadoPagoRepository estadoPagoRepository,
            HistorialEstadoPagoRepository historialEstadoPagoRepository,
            EstadoAlquilerDetalleRepository estadoAlquilerDetalleRepository,
            HistorialEstadoDetalleRepository historialEstadoDetalleRepository,
            HistorialEstadoReembolsoRepository historialEstadoReembolsoRepository,
            LiquidacionAlquilerService liquidacionAlquilerService,
            Clock reloj
    ) {
        this.usuarioRepository = usuarioRepository;
        this.pagoRepository = pagoRepository;
        this.reembolsoRepository = reembolsoRepository;
        this.pasarelaSimulada = pasarelaSimulada;
        this.alquilerRepository = alquilerRepository;
        this.alquilerDetalleRepository = alquilerDetalleRepository;
        this.estadoPagoRepository = estadoPagoRepository;
        this.historialEstadoPagoRepository = historialEstadoPagoRepository;
        this.estadoAlquilerDetalleRepository = estadoAlquilerDetalleRepository;
        this.historialEstadoDetalleRepository = historialEstadoDetalleRepository;
        this.historialEstadoReembolsoRepository = historialEstadoReembolsoRepository;
        this.liquidacionAlquilerService = liquidacionAlquilerService;
        this.reloj = reloj;
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public SaldoSimuladoDTO consultarSaldoSimulado() {
        return calcularSaldoSimulado(obtenerClienteActual());
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public ResultadoAnticipoDTO iniciarAnticipo(
            Integer alquilerId,
            String claveSolicitud
    ) {
        if (alquilerId == null || alquilerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del alquiler no es válido."
            );
        }

        String claveNormalizada = validarClaveSolicitud(claveSolicitud);

        // Serializa las operaciones financieras de esta cuenta.
        Usuario usuario = obtenerClienteActual(true);

        Alquiler alquiler = alquilerRepository
                .buscarPorIdConBloqueo(alquilerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El alquiler no existe."
                ));

        if (alquiler.getUsuario() == null) {
            throw new ConfiguracionSistemaException(
                    "El alquiler no tiene un usuario asociado."
            );
        }

        if (alquiler.getUsuario().getId() != usuario.getId()) {
            throw new AccessDeniedException(
                    "No podés iniciar pagos de un alquiler ajeno."
            );
        }

        if (alquiler.getEstadoContratacion() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este alquiler pertenece al circuito anterior."
            );
        }

        // Evita colisiones entre cuentas que reutilicen el mismo UUID.
        String clavePersistida = "ANTICIPO:"
                + usuario.getId()
                + ":"
                + claveNormalizada;

        Optional<Pago> intentoExistente = pagoRepository
                .findByClaveIdempotencia(clavePersistida);

        if (intentoExistente.isPresent()) {
            Pago pago = intentoExistente.get();

            if (pago.getAlquiler().getId() != alquilerId.intValue()
                    || !"ANTICIPO".equals(pago.getFinalidad())
                    || !"SIMULADA".equals(pago.getPlataforma())
                    || !"PRUEBA".equals(pago.getEntorno())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La clave de solicitud ya corresponde a otra operación."
                );
            }

            if (pago.getEstadoPago() == null
                    || !Set.of(
                    "CREADO",
                    "PENDIENTE",
                    "APROBADO",
                    "RECHAZADO",
                    "CANCELADO"
            ).contains(pago.getEstadoPago().getCodigo())) {
                throw new ConfiguracionSistemaException(
                        "El intento existente tiene un estado inválido."
                );
            }

            BigDecimal importeOriginal =
                    normalizarNoNegativo(pago.getMontoArs());

            if (importeOriginal.signum() == 0) {
                throw new ConfiguracionSistemaException(
                        "El intento existente tiene un importe inválido."
                );
            }

            // Repetir devuelve el mismo intento, incluso si después venció
            // o se canceló el alquiler. No crea ni modifica operaciones.
            return new ResultadoAnticipoDTO(
                    alquiler.getId(),
                    importeOriginal,
                    pago,
                    claveNormalizada
            );
        }

        if (alquiler.isBorradoAlquiler()
                || !"RESERVADO".equals(
                alquiler.getEstadoContratacion().getCodigo()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El alquiler ya no admite un nuevo anticipo."
            );
        }

        List<Pago> pagosDelAlquiler = pagoRepository
                .findByAlquilerIdOrderByIdAsc(alquilerId);

        for (Pago pago : pagosDelAlquiler) {
            if (!"ANTICIPO".equals(pago.getFinalidad())) {
                continue;
            }

            if (pago.getEstadoPago() == null
                    || pago.getEstadoPago().getCodigo() == null) {
                throw new ConfiguracionSistemaException(
                        "Un anticipo existente no tiene un estado válido."
                );
            }

            switch (pago.getEstadoPago().getCodigo()) {
                case "CREADO":
                case "PENDIENTE":
                case "APROBADO":
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Ya existe un anticipo sin resolver o aprobado. "
                                    + "No corresponde crear otro intento."
                    );

                case "RECHAZADO":
                case "CANCELADO":
                    break;

                default:
                    throw new ConfiguracionSistemaException(
                            "Un anticipo existente tiene un estado desconocido."
                    );
            }
        }

        List<AlquilerDetalle> detalles = alquilerDetalleRepository
                .buscarPorAlquilerConDatos(alquilerId);

        if (detalles.isEmpty()) {
            throw new ConfiguracionSistemaException(
                    "El alquiler no tiene detalles."
            );
        }

        BigDecimal importe = new BigDecimal("0.00");
        int unidadesReservadas = 0;

        for (AlquilerDetalle detalle : detalles) {
            if (detalle.getEstado() == null
                    || detalle.getEstado().getCodigo() == null) {
                throw new ConfiguracionSistemaException(
                        "Un detalle del alquiler no tiene un estado válido."
                );
            }

            if (detalle.getFechaHoraRetiro() != null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No corresponde iniciar otro anticipo después del retiro."
                );
            }

            switch (detalle.getEstado().getCodigo()) {
                case "RESERVADO":
                    unidadesReservadas++;
                    importe = importe.add(
                            normalizarNoNegativo(
                                    detalle.getSubtotalEstimadoArs()
                            )
                    );
                    break;

                case "CANCELADO":
                case "VENCIDO":
                    // No se cobra el anticipo de estas unidades.
                    break;

                case "PENDIENTE_RETIRO":
                case "EN_CURSO":
                case "DEVUELTO":
                case "CERRADO_POR_INCIDENCIA":
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "El estado de las unidades no admite otro anticipo."
                    );

                default:
                    throw new ConfiguracionSistemaException(
                            "Un detalle tiene un estado desconocido."
                    );
            }
        }

        if (unidadesReservadas == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No quedan bicicletas reservadas para iniciar el anticipo."
            );
        }

        importe = normalizarNoNegativo(importe);

        LocalDateTime creacion = alquiler.getFechaHoraCreacion();
        LocalDateTime vencimiento =
                alquiler.getFechaHoraVencimientoReserva();

        LocalDateTime ahora = LocalDateTime.ofInstant(
                reloj.instant(),
                ZoneOffset.UTC
        );

        if (creacion == null
                || vencimiento == null
                || !vencimiento.equals(creacion.plusMinutes(15))
                || ahora.isBefore(creacion)) {
            throw new ConfiguracionSistemaException(
                    "Las fechas de la reserva son inconsistentes."
            );
        }

        if (!ahora.isBefore(vencimiento)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva venció. No se puede iniciar un nuevo anticipo."
            );
        }

        if (importe.signum() == 0) {
            // No crear pagos de importe cero ni una aprobación ficticia.
            return new ResultadoAnticipoDTO(
                    alquiler.getId(),
                    importe,
                    null,
                    claveNormalizada
            );
        }

        EstadoPago estadoCreado = estadoPagoRepository
                .findByCodigo("CREADO")
                .orElseThrow(() -> new ConfiguracionSistemaException(
                        "Falta el estado de pago CREADO."
                ));

        Pago pago = pagoRepository.saveAndFlush(
                new Pago(
                        alquiler,
                        estadoCreado,
                        "ANTICIPO",
                        importe,
                        ahora,
                        "SIMULADA",
                        "PRUEBA",
                        clavePersistida
                )
        );

        historialEstadoPagoRepository.save(
                new HistorialEstadoPago(
                        pago,
                        null,
                        estadoCreado,
                        ahora,
                        null,
                        usuario,
                        "USUARIO",
                        "SOLICITUD_ANTICIPO",
                        null,
                        "Intento registrado localmente; "
                                + "todavía sin resultado del proveedor."
                )
        );

        historialEstadoPagoRepository.flush();

        return new ResultadoAnticipoDTO(
                alquiler.getId(),
                importe,
                pago,
                claveNormalizada
        );
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public ResultadoAnticipoDTO resolverAnticipo(
            Integer alquilerId,
            String claveSolicitud
    ) {
        if (alquilerId == null || alquilerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del alquiler no es válido."
            );
        }

        String claveNormalizada = validarClaveSolicitud(claveSolicitud);
        Usuario usuario = obtenerClienteActual(true);

        Alquiler alquiler = alquilerRepository
                .buscarPorIdConBloqueo(alquilerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El alquiler no existe."
                ));

        if (alquiler.getUsuario() == null) {
            throw new ConfiguracionSistemaException(
                    "El alquiler no tiene un usuario asociado."
            );
        }

        if (alquiler.getUsuario().getId() != usuario.getId()) {
            throw new AccessDeniedException(
                    "No podés resolver pagos de un alquiler ajeno."
            );
        }

        if (alquiler.getEstadoContratacion() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este alquiler pertenece al circuito anterior."
            );
        }

        String clavePersistida = "ANTICIPO:"
                + usuario.getId()
                + ":"
                + claveNormalizada;

        Pago encontrado = pagoRepository
                .findByClaveIdempotencia(clavePersistida)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un intento de anticipo con esa clave."
                ));

        if (encontrado.getId() == null) {
            throw new ConfiguracionSistemaException(
                    "El intento de pago no tiene identidad."
            );
        }

        Pago pago = pagoRepository
                .buscarPorIdConBloqueo(encontrado.getId())
                .orElseThrow(() -> new ConfiguracionSistemaException(
                        "El intento de pago dejó de estar disponible."
                ));

        if (pago.getAlquiler().getId() != alquilerId.intValue()
                || !"ANTICIPO".equals(pago.getFinalidad())
                || !"SIMULADA".equals(pago.getPlataforma())
                || !"PRUEBA".equals(pago.getEntorno())
                || !"ARS".equals(pago.getMoneda())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La clave no corresponde a este anticipo simulado."
            );
        }

        String estadoActual = pago.getEstadoPago() == null
                ? null
                : pago.getEstadoPago().getCodigo();

        if (estadoActual == null
                || !Set.of(
                        "CREADO",
                        "PENDIENTE",
                        "APROBADO",
                        "RECHAZADO",
                        "CANCELADO"
                ).contains(estadoActual)) {
            throw new ConfiguracionSistemaException(
                    "El intento de pago tiene un estado inválido."
            );
        }

        BigDecimal monto = normalizarNoNegativo(pago.getMontoArs());

        if (monto.signum() == 0) {
            throw new ConfiguracionSistemaException(
                    "Un intento de pago debe tener importe positivo."
            );
        }

        if ("APROBADO".equals(estadoActual)) {
            importeConfirmado(
                    monto,
                    pago.getMoneda(),
                    pago.getFechaCreacion(),
                    pago.getFechaConfirmacion()
            );
        } else if (pago.getFechaConfirmacion() != null) {
            throw new ConfiguracionSistemaException(
                    "Un pago no aprobado tiene fecha de confirmación."
            );
        }

        // Los resultados definitivos no vuelven a evaluarse.
        if (Set.of("APROBADO", "RECHAZADO", "CANCELADO")
                .contains(estadoActual)) {
            return new ResultadoAnticipoDTO(
                    alquiler.getId(),
                    monto,
                    pago,
                    claveNormalizada
            );
        }

        LocalDateTime ahora = LocalDateTime.ofInstant(
                reloj.instant(),
                ZoneOffset.UTC
        );

        LocalDateTime creacion = alquiler.getFechaHoraCreacion();
        LocalDateTime vencimiento =
                alquiler.getFechaHoraVencimientoReserva();

        if (creacion == null
                || vencimiento == null
                || !vencimiento.equals(creacion.plusMinutes(15))
                || ahora.isBefore(creacion)
                || pago.getFechaCreacion() == null
                || pago.getFechaCreacion().isBefore(creacion)
                || !pago.getFechaCreacion().isBefore(vencimiento)
                || ahora.isBefore(pago.getFechaCreacion())) {
            throw new ConfiguracionSistemaException(
                    "Las fechas del anticipo o de la reserva son inconsistentes."
            );
        }

        List<AlquilerDetalle> detalles = alquilerDetalleRepository
                .buscarPorAlquilerConDatos(alquilerId);

        if (detalles.isEmpty()) {
            throw new ConfiguracionSistemaException(
                    "El alquiler no tiene detalles."
            );
        }

        BigDecimal requerido = importeReservado(detalles);

        boolean quedanReservadas = detalles.stream().anyMatch(
                detalle -> "RESERVADO".equals(
                        detalle.getEstado().getCodigo()
                )
        );

        if ("CREADO".equals(estadoActual)) {
            if (pago.getReferenciaExterna() != null) {
                throw new ConfiguracionSistemaException(
                        "No se puede presumir local un intento con referencia externa."
                );
            }

            // En este simulador, CREADO sin referencia todavía no fue enviado.
            // Si cambió la selección, se conserva y cancela el intento original.
            if (!reservaVigente(alquiler, ahora)
                    || !quedanReservadas
                    || huboUso(detalles)
                    || requerido.compareTo(monto) != 0) {
                cambiarEstadoPago(
                        pago,
                        "CANCELADO",
                        ahora,
                        "CANCELACION_LOCAL",
                        "Intento local cancelado sin envío ni cobro: "
                                + "cambió la selección o la vigencia de la reserva."
                );

                pagoRepository.flush();

                return new ResultadoAnticipoDTO(
                        alquiler.getId(),
                        monto,
                        pago,
                        claveNormalizada
                );
            }

            pago.setReferenciaExterna("SIM-PAGO-" + pago.getId());

            cambiarEstadoPago(
                    pago,
                    "PENDIENTE",
                    ahora,
                    "ENVIO_SIMULADO",
                    "Operación iniciada en el proveedor simulado."
            );
        } else if (pago.getReferenciaExterna() == null) {
            throw new ConfiguracionSistemaException(
                    "El pago pendiente no tiene correlación con el simulador."
            );
        }

        BigDecimal disponible = calcularSaldoSimulado(usuario)
                .getSaldoDisponibleArs();

        String resultado = pasarelaSimulada.evaluarPago(
                pago,
                disponible
        );

        if (resultado == null
                || !Set.of(
                        "PENDIENTE",
                        "APROBADO",
                        "RECHAZADO",
                        "CANCELADO"
                ).contains(resultado)) {
            throw new ConfiguracionSistemaException(
                    "El simulador devolvió un resultado inválido."
            );
        }

        LocalDateTime fechaRespuesta = LocalDateTime.ofInstant(
                reloj.instant(),
                ZoneOffset.UTC
        );

        if (fechaRespuesta.isBefore(ahora)) {
            throw new ConfiguracionSistemaException(
                    "El reloj retrocedió durante la resolución del pago."
            );
        }

        cambiarEstadoPago(
                pago,
                resultado,
                fechaRespuesta,
                "PASARELA_SIMULADA",
                "Respuesta validada del proveedor simulado."
        );

        if ("APROBADO".equals(resultado)) {
            aplicarAprobacionAnticipo(
                    alquiler,
                    pago,
                    detalles,
                    fechaRespuesta
            );
        }

        // Confirma conjuntamente pago, historiales y cambios relacionados.
        pagoRepository.flush();

        return new ResultadoAnticipoDTO(
                alquiler.getId(),
                monto,
                pago,
                claveNormalizada
        );
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultadoAdicionalDTO iniciarAdicional(Integer alquilerId, String claveSolicitud) {
        String clave = validarSolicitudAdicional(alquilerId, claveSolicitud);
        Usuario usuario = obtenerClienteActual(true);
        Alquiler alquiler = obtenerAlquilerParaAdicional(alquilerId, usuario);
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        String clavePersistida = "ADICIONAL:" + usuario.getId() + ":" + clave;

        Optional<Pago> existente = pagoRepository.findByClaveIdempotencia(clavePersistida);
        if (existente.isPresent()) {
            validarIntentoAdicional(existente.get(), alquiler, ahora);
            return resultadoAdicional(alquiler, existente.get(), clave);
        }

        LiquidacionAlquilerDTO liquidacion = consultarSaldoParaAdicional(alquiler, ahora);
        BigDecimal saldo = normalizarNoNegativo(liquidacion.getSaldoPendienteArs());
        if (saldo.signum() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El alquiler no tiene saldo pendiente para pagar.");
        }

        // Un resultado incierto debe conciliarse antes de iniciar otro cobro.
        // También se considera un anticipo tardío todavía en gestión.
        for (Pago anterior : pagoRepository.findByAlquilerIdOrderByIdAsc(alquilerId)) {
            if (anterior == null) {
                throw new ConfiguracionSistemaException("La consulta contiene un pago inválido.");
            }
            String estado = anterior.getEstadoPago() == null
                    ? null : anterior.getEstadoPago().getCodigo();
            if (estado == null || !Set.of("CREADO", "PENDIENTE", "APROBADO",
                    "RECHAZADO", "CANCELADO").contains(estado)) {
                throw new ConfiguracionSistemaException("Un pago existente tiene un estado inválido.");
            }
            if ("CREADO".equals(estado) || "PENDIENTE".equals(estado)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un pago sin resolver para este alquiler. Consultá ese intento antes de crear otro.");
            }
        }

        EstadoPago creado = estadoPagoRepository.findByCodigo("CREADO")
                .orElseThrow(() -> new ConfiguracionSistemaException("Falta el estado de pago CREADO."));
        Pago pago = pagoRepository.saveAndFlush(new Pago(alquiler, creado, "ADICIONAL",
                saldo, ahora, "SIMULADA", "PRUEBA", clavePersistida));
        historialEstadoPagoRepository.save(new HistorialEstadoPago(pago, null, creado,
                ahora, null, usuario, "USUARIO", "SOLICITUD_ADICIONAL", null,
                "Pago solicitado por el cliente por el saldo pendiente liquidado; todavía no cobrado."));
        historialEstadoPagoRepository.flush();

        return new ResultadoAdicionalDTO(pago, clave, liquidacion);
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public ResultadoAdicionalDTO resolverAdicional(Integer alquilerId, String claveSolicitud) {
        String clave = validarSolicitudAdicional(alquilerId, claveSolicitud);
        Usuario usuario = obtenerClienteActual(true);
        Alquiler alquiler = obtenerAlquilerParaAdicional(alquilerId, usuario);
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        String clavePersistida = "ADICIONAL:" + usuario.getId() + ":" + clave;

        Pago encontrado = pagoRepository.findByClaveIdempotencia(clavePersistida)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un intento de pago adicional con esa clave."));
        if (encontrado.getId() == null) {
            throw new ConfiguracionSistemaException("El intento de pago no tiene identidad.");
        }
        Pago pago = pagoRepository.buscarPorIdConBloqueo(encontrado.getId())
                .orElseThrow(() -> new ConfiguracionSistemaException("El intento de pago dejó de estar disponible."));
        BigDecimal monto = validarIntentoAdicional(pago, alquiler, ahora);
        String estado = pago.getEstadoPago().getCodigo();

        if (Set.of("APROBADO", "RECHAZADO", "CANCELADO").contains(estado)) {
            return resultadoAdicional(alquiler, pago, clave);
        }

        LiquidacionAlquilerDTO liquidacion = consultarSaldoParaAdicional(alquiler, ahora);
        if ("CREADO".equals(estado)) {
            if (pago.getReferenciaExterna() != null) {
                throw new ConfiguracionSistemaException("Un intento local tiene una referencia externa inesperada.");
            }
            if (normalizarNoNegativo(liquidacion.getSaldoPendienteArs()).compareTo(monto) < 0) {
                // Aún no hubo envío: cancelar el intento local, nunca alterar su importe.
                cambiarEstadoPago(pago, "CANCELADO", ahora, "CANCELACION_LOCAL",
                        "Intento adicional local cancelado sin cobro: el saldo pendiente disminuyó.");
                pagoRepository.flush();
                return resultadoAdicional(alquiler, pago, clave);
            }
            pago.setReferenciaExterna("SIM-PAGO-" + pago.getId());
            cambiarEstadoPago(pago, "PENDIENTE", ahora, "ENVIO_SIMULADO",
                    "Pago adicional iniciado en el proveedor simulado.");
        } else if (!("SIM-PAGO-" + pago.getId()).equals(pago.getReferenciaExterna())) {
            throw new ConfiguracionSistemaException("El adicional pendiente no tiene correlación con el simulador.");
        }

        String resultado = pasarelaSimulada.evaluarPago(pago,
                calcularSaldoSimulado(usuario).getSaldoDisponibleArs());
        if (resultado == null || !Set.of("PENDIENTE", "APROBADO", "RECHAZADO", "CANCELADO").contains(resultado)) {
            throw new ConfiguracionSistemaException("El simulador devolvió un resultado inválido.");
        }
        LocalDateTime fechaRespuesta = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        if (fechaRespuesta.isBefore(ahora)) {
            throw new ConfiguracionSistemaException("El reloj retrocedió durante la resolución del pago.");
        }
        cambiarEstadoPago(pago, resultado, fechaRespuesta, "PASARELA_SIMULADA",
                "Respuesta del pago adicional validada; no modifica el uso ni la liquidación histórica.");
        pagoRepository.flush();

        // Una confirmación en gestión se conserva incluso si otro hecho redujo la deuda.
        // Un eventual excedente se refleja como reembolso pendiente, no como un nuevo uso.
        return resultadoAdicional(alquiler, pago, clave);
    }

    private String validarSolicitudAdicional(Integer alquilerId, String claveSolicitud) {
        if (alquilerId == null || alquilerId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del alquiler no es válido.");
        }
        return validarClaveSolicitud(claveSolicitud);
    }

    private Alquiler obtenerAlquilerParaAdicional(Integer alquilerId, Usuario usuario) {
        Alquiler alquiler = alquilerRepository.buscarPorIdConBloqueo(alquilerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El alquiler no existe."));
        if (alquiler.getUsuario() == null) {
            throw new ConfiguracionSistemaException("El alquiler no tiene un usuario asociado.");
        }
        if (alquiler.getUsuario().getId() != usuario.getId()) {
            throw new AccessDeniedException("No podés pagar el saldo de un alquiler ajeno.");
        }
        if (alquiler.getEstadoContratacion() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este alquiler pertenece al circuito anterior.");
        }
        // El borrado del listado no extingue la deuda ni impide al titular pagarla.
        return alquiler;
    }

    private LiquidacionAlquilerDTO consultarSaldoParaAdicional(Alquiler alquiler, LocalDateTime ahora) {
        LiquidacionAlquilerDTO liquidacion = liquidacionAlquilerService.consultarLiquidacion(alquiler.getId());
        if (liquidacion.getFechaHoraLiquidacion() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El alquiler todavía no está liquidado.");
        }
        if (liquidacion.getFechaHoraLiquidacion().isAfter(ahora)) {
            throw new ConfiguracionSistemaException("La fecha de liquidación es posterior al pago solicitado.");
        }
        return liquidacion;
    }

    private BigDecimal validarIntentoAdicional(Pago pago, Alquiler alquiler, LocalDateTime ahora) {
        if (pago == null || pago.getId() == null || pago.getId() <= 0 || pago.getAlquiler() == null) {
            throw new ConfiguracionSistemaException("El intento adicional no tiene identidad o alquiler.");
        }
        if (pago.getAlquiler().getId() != alquiler.getId() || !"ADICIONAL".equals(pago.getFinalidad())
                || !"SIMULADA".equals(pago.getPlataforma()) || !"PRUEBA".equals(pago.getEntorno())
                || !"ARS".equals(pago.getMoneda())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave no corresponde a este pago adicional simulado.");
        }
        String estado = pago.getEstadoPago() == null ? null : pago.getEstadoPago().getCodigo();
        if (estado == null || !Set.of("CREADO", "PENDIENTE", "APROBADO", "RECHAZADO", "CANCELADO").contains(estado)) {
            throw new ConfiguracionSistemaException("El intento adicional tiene un estado inválido.");
        }
        BigDecimal monto = normalizarNoNegativo(pago.getMontoArs());
        if (monto.signum() == 0 || pago.getFechaCreacion() == null
                || alquiler.getFechaHoraLiquidacion() == null
                || pago.getFechaCreacion().isBefore(alquiler.getFechaHoraLiquidacion())
                || pago.getFechaCreacion().isAfter(ahora)) {
            throw new ConfiguracionSistemaException("El intento adicional tiene importe o fechas inconsistentes.");
        }
        if ("APROBADO".equals(estado)) {
            importeConfirmado(monto, pago.getMoneda(), pago.getFechaCreacion(), pago.getFechaConfirmacion());
            if (pago.getFechaConfirmacion().isAfter(ahora)) {
                throw new ConfiguracionSistemaException("El pago tiene una confirmación futura.");
            }
        } else if (pago.getFechaConfirmacion() != null) {
            throw new ConfiguracionSistemaException("Un pago no aprobado tiene fecha de confirmación.");
        }
        return monto;
    }

    private ResultadoAdicionalDTO resultadoAdicional(Alquiler alquiler, Pago pago, String clave) {
        return new ResultadoAdicionalDTO(pago, clave,
                liquidacionAlquilerService.consultarLiquidacion(alquiler.getId()));
    }

    private SaldoSimuladoDTO calcularSaldoSimulado(Usuario usuario) {

        // Estas consultas incluyen movimientos de alquileres ocultos.
        List<Pago> pagos = pagoRepository
                .findByAlquilerUsuarioIdAndPlataformaAndEntornoOrderByIdAsc(
                        usuario.getId(),
                        "SIMULADA",
                        "PRUEBA"
                );

        List<Reembolso> reembolsos = reembolsoRepository
                .findByPagoAlquilerUsuarioIdAndPagoPlataformaAndPagoEntornoOrderByIdAsc(
                        usuario.getId(),
                        "SIMULADA",
                        "PRUEBA"
                );

        BigDecimal inicial = normalizarNoNegativo(
                pasarelaSimulada.obtenerSaldoInicial(
                        usuario.getNickname()
                )
        );

        BigDecimal totalPagos = new BigDecimal("0.00");
        BigDecimal totalReembolsos = new BigDecimal("0.00");

        Map<Integer, BigDecimal> pagosConfirmados = new HashMap<>();

        for (Pago pago : pagos) {
            if (pago == null
                    || pago.getEstadoPago() == null
                    || pago.getEstadoPago().getCodigo() == null) {
                throw new ConfiguracionSistemaException(
                        "Un pago no tiene un estado válido."
                );
            }

            switch (pago.getEstadoPago().getCodigo()) {
                case "CREADO":
                case "PENDIENTE":
                case "RECHAZADO":
                case "CANCELADO":
                    continue;

                case "APROBADO":
                    break;

                default:
                    throw new ConfiguracionSistemaException(
                            "Se encontró un estado de pago desconocido."
                    );
            }

            BigDecimal importe = importeConfirmado(
                    pago.getMontoArs(),
                    pago.getMoneda(),
                    pago.getFechaCreacion(),
                    pago.getFechaConfirmacion()
            );

            Integer id = pago.getId();

            if (id == null
                    || id <= 0
                    || pagosConfirmados.putIfAbsent(id, importe) != null) {
                throw new ConfiguracionSistemaException(
                        "La consulta contiene un pago sin identidad o duplicado."
                );
            }

            totalPagos = totalPagos.add(importe);
        }

        Map<Integer, BigDecimal> reembolsadoPorPago = new HashMap<>();
        Set<Integer> reembolsosContados = new HashSet<>();

        for (Reembolso reembolso : reembolsos) {
            if (reembolso == null || reembolso.getEstado() == null) {
                throw new ConfiguracionSistemaException(
                        "Un reembolso no tiene un estado válido."
                );
            }

            switch (reembolso.getEstado()) {
                case "PENDIENTE":
                case "EN_PROCESO":
                case "FALLIDO":
                    continue;

                case "CONFIRMADO":
                    break;

                default:
                    throw new ConfiguracionSistemaException(
                            "Se encontró un estado de reembolso desconocido."
                    );
            }

            Integer id = reembolso.getId();

            if (id == null
                    || id <= 0
                    || !reembolsosContados.add(id)) {
                throw new ConfiguracionSistemaException(
                        "La consulta contiene un reembolso sin identidad o duplicado."
                );
            }

            if (reembolso.getPago() == null) {
                throw new ConfiguracionSistemaException(
                        "Un reembolso no tiene su pago de origen."
                );
            }

            Integer pagoId = reembolso.getPago().getId();
            BigDecimal pagoOriginal = pagosConfirmados.get(pagoId);

            if (pagoOriginal == null) {
                throw new ConfiguracionSistemaException(
                        "El reembolso confirmado no corresponde a un pago aprobado de esta cuenta."
                );
            }

            BigDecimal importe = importeConfirmado(
                    reembolso.getMontoArs(),
                    reembolso.getMoneda(),
                    reembolso.getFechaCreacion(),
                    reembolso.getFechaConfirmacion()
            );

            BigDecimal acumulado = reembolsadoPorPago.merge(
                    pagoId,
                    importe,
                    BigDecimal::add
            );

            if (acumulado.compareTo(pagoOriginal) > 0) {
                throw new ConfiguracionSistemaException(
                        "Los reembolsos confirmados superan el pago original."
                );
            }

            totalReembolsos = totalReembolsos.add(importe);
        }

        BigDecimal disponible = inicial
                .subtract(totalPagos)
                .add(totalReembolsos);

        if (disponible.signum() < 0) {
            throw new ConfiguracionSistemaException(
                    "El saldo inicial simulado es incompatible con los movimientos guardados."
            );
        }

        return new SaldoSimuladoDTO(
                usuario.getId(),
                usuario.getNickname(),
                inicial,
                totalPagos,
                totalReembolsos,
                disponible
        );
    }

    private Usuario obtenerClienteActual() {
        return obtenerClienteActual(false);
    }

    private Usuario obtenerClienteActual(boolean bloquear) {
        Authentication autenticacion =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || autenticacion.getAuthorities().stream().noneMatch(
                autoridad -> "CLIENTE".equals(
                        autoridad.getAuthority()
                )
        )) {
            throw new AccessDeniedException(
                    "Se requiere una sesión con rol CLIENTE."
            );
        }

        Optional<Usuario> encontrado = bloquear
                ? usuarioRepository.buscarActivoPorEmailConBloqueo(
                autenticacion.getName()
        )
                : usuarioRepository.buscarPorEmailConDatosYRoles(
                autenticacion.getName()
        );

        return encontrado
                .filter(Usuario::isActivo)
                .filter(usuario ->
                        usuario.getUsuarioRoles().stream().anyMatch(
                                asignacion -> asignacion.getRol() != null
                                        && "CLIENTE".equals(
                                        asignacion.getRol().getCodigo()
                                )
                        )
                )
                .orElseThrow(() -> new AccessDeniedException(
                        "La cuenta no existe, está inactiva "
                                + "o no tiene rol CLIENTE vigente."
                ));
    }

    private BigDecimal importeConfirmado(
            BigDecimal valor,
            String moneda,
            LocalDateTime creacion,
            LocalDateTime confirmacion
    ) {
        BigDecimal importe = normalizarNoNegativo(valor);

        if (importe.signum() == 0
                || !"ARS".equals(moneda)
                || creacion == null
                || confirmacion == null
                || confirmacion.isBefore(creacion)) {
            throw new ConfiguracionSistemaException(
                    "Un movimiento confirmado tiene importe, moneda o fechas inconsistentes."
            );
        }

        return importe;
    }

    private BigDecimal normalizarNoNegativo(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new ConfiguracionSistemaException(
                    "Hay un importe financiero inválido."
            );
        }

        final BigDecimal normalizado;

        try {
            normalizado = valor.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException ex) {
            throw new ConfiguracionSistemaException(
                    "Un importe financiero tiene decimales incompatibles."
            );
        }

        if (normalizado.precision() - normalizado.scale() > 19) {
            throw new ConfiguracionSistemaException(
                    "Un importe financiero supera la capacidad permitida."
            );
        }

        return normalizado;
    }

    private boolean reservaVigente(
            Alquiler alquiler,
            LocalDateTime ahora
    ) {
        return !alquiler.isBorradoAlquiler()
                && alquiler.getEstadoContratacion() != null
                && "RESERVADO".equals(
                        alquiler.getEstadoContratacion().getCodigo()
                )
                && alquiler.getFechaHoraVencimientoReserva() != null
                && ahora.isBefore(
                        alquiler.getFechaHoraVencimientoReserva()
                );
    }

    private BigDecimal importeReservado(
            List<AlquilerDetalle> detalles
    ) {
        BigDecimal total = new BigDecimal("0.00");

        for (AlquilerDetalle detalle : detalles) {
            String codigo = detalle.getEstado() == null
                    ? null
                    : detalle.getEstado().getCodigo();

            if (codigo == null
                    || !Set.of(
                            "RESERVADO",
                            "PENDIENTE_RETIRO",
                            "EN_CURSO",
                            "DEVUELTO",
                            "CANCELADO",
                            "VENCIDO",
                            "CERRADO_POR_INCIDENCIA"
                    ).contains(codigo)) {
                throw new ConfiguracionSistemaException(
                        "Un detalle tiene un estado inválido."
                );
            }

            if ("RESERVADO".equals(codigo)) {
                if (detalle.getFechaHoraRetiro() != null) {
                    throw new ConfiguracionSistemaException(
                            "Una unidad reservada tiene un retiro registrado."
                    );
                }

                total = total.add(
                        normalizarNoNegativo(
                                detalle.getSubtotalEstimadoArs()
                        )
                );
            }
        }

        return normalizarNoNegativo(total);
    }

    private boolean huboUso(List<AlquilerDetalle> detalles) {
        return detalles.stream().anyMatch(detalle ->
                detalle.getFechaHoraRetiro() != null
                        || Set.of(
                                "EN_CURSO",
                                "DEVUELTO",
                                "CERRADO_POR_INCIDENCIA"
                        ).contains(detalle.getEstado().getCodigo())
        );
    }

    private void cambiarEstadoPago(
            Pago pago,
            String codigoNuevo,
            LocalDateTime fecha,
            String fuente,
            String observaciones
    ) {
        EstadoPago anterior = pago.getEstadoPago();

        if (codigoNuevo.equals(anterior.getCodigo())) {
            return;
        }

        EstadoPago nuevo = estadoPagoRepository
                .findByCodigo(codigoNuevo)
                .orElseThrow(() -> new ConfiguracionSistemaException(
                        "Falta el estado de pago " + codigoNuevo + "."
                ));

        pago.setEstadoPago(nuevo);

        if ("APROBADO".equals(codigoNuevo)) {
            pago.setFechaConfirmacion(fecha);
        }

        historialEstadoPagoRepository.save(
                new HistorialEstadoPago(
                        pago,
                        anterior,
                        nuevo,
                        fecha,
                        null,
                        null,
                        "SISTEMA",
                        fuente,
                        null,
                        observaciones
                )
        );
    }

    private void aplicarAprobacionAnticipo(
            Alquiler alquiler,
            Pago pago,
            List<AlquilerDetalle> detalles,
            LocalDateTime ahora
    ) {
        boolean usoRegistrado = huboUso(detalles);

        boolean quedanReservadas = detalles.stream().anyMatch(
                detalle -> "RESERVADO".equals(
                        detalle.getEstado().getCodigo()
                )
        );

        BigDecimal requerido = importeReservado(detalles);

        if (reservaVigente(alquiler, ahora)
                && !usoRegistrado
                && quedanReservadas
                && pago.getMontoArs().compareTo(requerido) >= 0) {
            habilitarDetallesParaRetiro(detalles, ahora);
            return;
        }

        if (!usoRegistrado) {
            registrarReembolsoSinUso(pago, ahora);
        }
        // Si hubo uso, el ajuste corresponde a la liquidación conjunta.
    }

    private void habilitarDetallesParaRetiro(
            List<AlquilerDetalle> detalles,
            LocalDateTime ahora
    ) {
        EstadoAlquilerDetalle pendienteRetiro =
                estadoAlquilerDetalleRepository
                        .findByCodigo("PENDIENTE_RETIRO")
                        .orElseThrow(() -> new ConfiguracionSistemaException(
                                "Falta el estado de detalle PENDIENTE_RETIRO."
                        ));

        for (AlquilerDetalle detalle : detalles) {
            if (!"RESERVADO".equals(detalle.getEstado().getCodigo())) {
                continue;
            }

            EstadoAlquilerDetalle anterior = detalle.getEstado();
            detalle.setEstado(pendienteRetiro);

            historialEstadoDetalleRepository.save(
                    new HistorialEstadoDetalle(
                            detalle,
                            anterior,
                            pendienteRetiro,
                            null,
                            "SISTEMA",
                            "Anticipo aprobado dentro del plazo original; "
                                    + "todavía no se registró el retiro.",
                            ahora
                    )
            );
        }
    }

    private void registrarReembolsoSinUso(
            Pago pago,
            LocalDateTime ahora
    ) {
        BigDecimal montoPago = importeConfirmado(
                pago.getMontoArs(),
                pago.getMoneda(),
                pago.getFechaCreacion(),
                pago.getFechaConfirmacion()
        );

        String clave = "REEMBOLSO_SIN_USO:" + pago.getId();

        List<Reembolso> existentes = reembolsoRepository
                .findByPagoIdOrderByIdAsc(pago.getId());

        BigDecimal comprometido = new BigDecimal("0.00");
        boolean obligacionRegistrada = false;

        for (Reembolso reembolso : existentes) {
            if (reembolso.getPago() == null
                    || !pago.getId().equals(
                            reembolso.getPago().getId()
                    )) {
                throw new ConfiguracionSistemaException(
                        "Un reembolso no corresponde al pago consultado."
                );
            }

            String estado = reembolso.getEstado();

            if (estado == null
                    || !Set.of(
                            "PENDIENTE",
                            "EN_PROCESO",
                            "CONFIRMADO",
                            "FALLIDO"
                    ).contains(estado)) {
                throw new ConfiguracionSistemaException(
                        "Un reembolso tiene un estado inválido."
                );
            }

            BigDecimal importe = normalizarNoNegativo(
                    reembolso.getMontoArs()
            );

            if (importe.signum() == 0
                    || !"ARS".equals(reembolso.getMoneda())) {
                throw new ConfiguracionSistemaException(
                        "Un reembolso tiene importe o moneda inválidos."
                );
            }

            if (clave.equals(reembolso.getClaveIdempotencia())) {
                obligacionRegistrada = true;
            }

            if (!"FALLIDO".equals(estado)) {
                comprometido = comprometido.add(importe);
            }
        }

        if (comprometido.compareTo(montoPago) > 0) {
            throw new ConfiguracionSistemaException(
                    "Los reembolsos comprometidos superan el pago."
            );
        }

        if (obligacionRegistrada) {
            // Incluso si falló, no duplicar esa obligación.
            return;
        }

        BigDecimal restante = montoPago.subtract(comprometido);

        if (restante.signum() == 0) {
            return;
        }

        Reembolso reembolso = reembolsoRepository.saveAndFlush(
                new Reembolso(
                        pago,
                        restante,
                        ahora,
                        "Anticipo confirmado sin uso y sin unidades "
                                + "habilitables; devolución pendiente.",
                        clave
                )
        );

        historialEstadoReembolsoRepository.save(
                new HistorialEstadoReembolso(
                        reembolso,
                        null,
                        "PENDIENTE",
                        ahora,
                        null,
                        null,
                        "SISTEMA",
                        "CONCILIACION_ANTICIPO",
                        null,
                        "Obligación registrada; el dinero todavía "
                                + "no fue devuelto."
                )
        );
    }

    private String validarClaveSolicitud(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La clave de solicitud es obligatoria."
            );
        }

        String limpia = valor.trim();

        try {
            String normalizada = UUID.fromString(limpia).toString();

            if (normalizada.equalsIgnoreCase(limpia)) {
                return normalizada;
            }
        } catch (IllegalArgumentException ex) {
            // Se devuelve el mismo error para cualquier formato inválido.
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La clave de solicitud debe tener formato UUID."
        );
    }

}
