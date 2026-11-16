package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerDTO;
import DSII.TurismoSustentable.dto.AlquilerDetalleDTO;
import DSII.TurismoSustentable.dto.DevolverBicicletaDTO;
import DSII.TurismoSustentable.dto.CancelarDetalleDTO;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.repositories.TipoBicicletaRepository;
import DSII.TurismoSustentable.repositories.EstadoBicicletaRepository;
import DSII.TurismoSustentable.repositories.PagoRepository;
import DSII.TurismoSustentable.repositories.ReembolsoRepository;
import DSII.TurismoSustentable.repositories.IncidenciaRepository;
import DSII.TurismoSustentable.repositories.OrdenMantenimientoRepository;
import DSII.TurismoSustentable.repositories.TipoMantenimientoRepository;
import DSII.TurismoSustentable.repositories.EstadoOrdenMantenimientoRepository;
import DSII.TurismoSustentable.repositories.HistorialOrdenMantenimientoRepository;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import DSII.TurismoSustentable.dto.CrearAlquilerDTO;
import DSII.TurismoSustentable.dto.CotizacionDolarDTO;
import DSII.TurismoSustentable.repositories.AlquilerDetalleRepository;
import DSII.TurismoSustentable.repositories.EstadoAlquilerRepository;
import DSII.TurismoSustentable.repositories.EstadoAlquilerDetalleRepository;
import DSII.TurismoSustentable.repositories.HistorialEstadoDetalleRepository;
import DSII.TurismoSustentable.dto.AlquilerCabeceraDTO;
import javax.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
public class AlquilerService {

    @Autowired
    private AlquilerRepository alquilerRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BicicletaRepository bicicletaRepository;

    @Autowired
    private EstacionRepository estacionRepository;

    @Autowired
    private CorreoService correoService;

    @Autowired
    private TipoBicicletaRepository tipoBicicletaRepository;
    @Autowired
    private EstadoBicicletaRepository estadoBicicletaRepository;
    @Autowired
    private TrazabilidadInventarioService trazabilidadInventario;

    @Autowired
    private EstadoAlquilerRepository estadoAlquilerRepository;

    @Autowired
    private EstadoAlquilerDetalleRepository estadoAlquilerDetalleRepository;

    @Autowired
    private AlquilerDetalleRepository alquilerDetalleRepository;

    @Autowired
    private HistorialEstadoDetalleRepository historialEstadoDetalleRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private ReembolsoRepository reembolsoRepository;

    @Autowired
    private IncidenciaRepository incidenciaRepository;

    @Autowired
    private IncidenciaService incidenciaService;

    @Autowired
    private OrdenMantenimientoRepository ordenMantenimientoRepository;

    @Autowired
    private TipoMantenimientoRepository tipoMantenimientoRepository;

    @Autowired
    private EstadoOrdenMantenimientoRepository estadoOrdenMantenimientoRepository;

    @Autowired
    private HistorialOrdenMantenimientoRepository historialOrdenMantenimientoRepository;

    @Autowired
    private CotizacionDolarService cotizacionDolarService;

    @Autowired
    private CalculadoraAlquiler calculadoraAlquiler;

    @Autowired
    private Validator validator;

    @Autowired
    private Clock reloj;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private LiquidacionAlquilerService liquidacionAlquilerService;

    // La simulación financiera está habilitada únicamente en el perfil refactor.
    @Autowired(required = false)
    private ReembolsoService reembolsoService;

    @Value("${alquiler.max-bicicletas-por-contratacion:10}")
    private int maxBicicletasPorContratacion;


    private void comprobarSinSaldoPendiente(Usuario usuario) {
        if (alquilerRepository.contarAlquileresConSaldoPendiente(
                usuario.getId()
        ) > 0) {
            throw new IllegalStateException(
                    "Tenés un saldo pendiente de un alquiler anterior. " +
                            "Debés pagarlo antes de iniciar otro alquiler."
            );
        }
    }

    private EstadoBicicleta estadoBicicleta(String codigo) {
        return estadoBicicletaRepository.findByCodigo(codigo).orElseThrow(() ->
                new ConfiguracionSistemaException("Falta el estado de bicicleta: " + codigo));
    }

    private static final BigDecimal SEGUNDOS_EN_HORA =
            new BigDecimal("3600");


    // OBTIENE EL USUARIO AUTENTICADO
    private Usuario obtenerUsuarioAutenticado() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Debe iniciar sesión.");
        }

        return usuarioRepository.findByEmail(auth.getName())
                .filter(Usuario::isActivo)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "El usuario autenticado no existe o está inactivo."
                        )
                );
    }


    // COMPRUEBA SI EL USUARIO ACTUAL ES ADMINISTRADOR
    private boolean usuarioActualEsAdmin() {
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(autoridad ->
                        "ADMIN".equals(autoridad.getAuthority())
                );
    }

    @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')")
    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public List<Object> listarMisAlquileres() {

        Usuario usuario = obtenerUsuarioAutenticado();

        boolean autorizado = usuario.getUsuarioRoles().stream()
                .map(UsuarioRol::getRol)
                .anyMatch(rol ->
                        "CLIENTE".equals(rol.getCodigo())
                                || "ADMIN".equals(rol.getCodigo())
                );

        if (!autorizado) {
            throw new AccessDeniedException(
                    "La cuenta no tiene permiso para consultar alquileres."
            );
        }

        return prepararListado(
                alquilerRepository.buscarVisiblesPorUsuarioConDatos(
                        usuario.getId()
                )
        );
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public List<Object> listarAlquileresAdministracion() {

        Usuario usuario = obtenerUsuarioAutenticado();

        boolean administradorVigente =
                usuario.getUsuarioRoles().stream()
                        .map(UsuarioRol::getRol)
                        .anyMatch(rol ->
                                "ADMIN".equals(rol.getCodigo())
                        );

        if (!administradorVigente) {
            throw new AccessDeniedException(
                    "La consulta requiere rol ADMIN vigente."
            );
        }

        return prepararListado(
                alquilerRepository.buscarVisiblesAdministracionConDatos()
        );
    }

    /*
     * Compatibilidad temporal de lectura.
     * Retirar la rama ANTERIOR al reemplazar sus productores y consumidores.
     * No convierte registros antiguos ni inventa detalles o cotizaciones.
     */
    private List<Object> prepararListado(List<Alquiler> alquileres) {

        List<Integer> idsConDetalles = new ArrayList<>();

        for (Alquiler alquiler : alquileres) {
            if (alquiler.getEstadoContratacion() != null) {
                idsConDetalles.add(alquiler.getId());
            }
        }

        Map<Integer, List<AlquilerDetalle>> detallesPorAlquiler =
                new TreeMap<>();

        // Evitar una consulta IN con una colección vacía.
        if (!idsConDetalles.isEmpty()) {

            List<AlquilerDetalle> detalles =
                    alquilerDetalleRepository.buscarPorAlquileresConDatos(
                            idsConDetalles
                    );

            for (AlquilerDetalle detalle : detalles) {
                detallesPorAlquiler.computeIfAbsent(
                        detalle.getAlquiler().getId(),
                        id -> new ArrayList<>()
                ).add(detalle);
            }
        }

        List<Object> resultado = new ArrayList<>();

        for (Alquiler alquiler : alquileres) {

            if (alquiler.getEstadoContratacion() != null) {

                List<AlquilerDetalle> detalles =
                        detallesPorAlquiler.get(alquiler.getId());

                if (detalles == null || detalles.isEmpty()) {
                    throw new ConfiguracionSistemaException(
                            "El alquiler con ID "
                                    + alquiler.getId()
                                    + " no tiene detalles registrados."
                    );
                }

                resultado.add(
                        new AlquilerCabeceraDTO(alquiler, detalles)
                );

            } else {

                if (alquiler.getEstado() == null
                        || alquiler.getBicicleta() == null) {
                    throw new ConfiguracionSistemaException(
                            "El alquiler anterior con ID "
                                    + alquiler.getId()
                                    + " tiene datos incompletos."
                    );
                }

                resultado.add(new AlquilerDTO(alquiler));
            }
        }

        return resultado;
    }

    @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')")
    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public AlquilerCabeceraDTO obtenerAlquiler(Integer id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Debe indicar un alquiler válido."
            );
        }

        Usuario usuario = obtenerUsuarioAutenticado();

        // Comprobar los roles vigentes, no solamente los de la sesión.
        boolean esAdmin = usuario.getUsuarioRoles().stream()
                .map(UsuarioRol::getRol)
                .anyMatch(rol -> "ADMIN".equals(rol.getCodigo()));

        boolean esCliente = usuario.getUsuarioRoles().stream()
                .map(UsuarioRol::getRol)
                .anyMatch(rol -> "CLIENTE".equals(rol.getCodigo()));

        if (!esAdmin && !esCliente) {
            throw new AccessDeniedException(
                    "La cuenta no tiene permiso para consultar alquileres."
            );
        }

        Alquiler alquiler = alquilerRepository
                .buscarAlquilerVisiblePorId(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "El alquiler no existe."
                        )
                );

        boolean esPropietario =
                alquiler.getUsuario().getId() == usuario.getId();

        if (!esAdmin && !esPropietario) {
            throw new AccessDeniedException(
                    "No tiene permiso para consultar este alquiler."
            );
        }

        // Los detalles se consultan después de comprobar el acceso.
        List<AlquilerDetalle> detalles = alquilerDetalleRepository
                .buscarPorAlquilerConDatos(id);

        if (detalles.isEmpty()) {
            throw new ConfiguracionSistemaException(
                    "El alquiler no tiene detalles registrados."
            );
        }

        return new AlquilerCabeceraDTO(alquiler, detalles);
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Integer crearReserva(CrearAlquilerDTO solicitud) {

        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "Los datos del alquiler son obligatorios."
            );
        }

        if (maxBicicletasPorContratacion <= 0) {
            throw new ConfiguracionSistemaException(
                    "El máximo de bicicletas por alquiler debe ser positivo."
            );
        }

        Set<ConstraintViolation<CrearAlquilerDTO>> errores =
                validator.validate(solicitud);

        if (!errores.isEmpty()) {
            throw new ConstraintViolationException(errores);
        }

        if (solicitud.getBicicletaIds().size()
                > maxBicicletasPorContratacion) {
            throw new IllegalArgumentException(
                    "Podés contratar hasta "
                            + maxBicicletasPorContratacion
                            + " bicicletas por alquiler."
            );
        }

        String email = obtenerUsuarioAutenticado().getEmail();

        Integer estacionId = solicitud.getEstacionRetiroId();
        int duracionSegundos = solicitud.getDuracionEstimadaSegundos();

        List<Integer> bicicletaIds =
                new ArrayList<>(solicitud.getBicicletaIds());

        Collections.sort(bicicletaIds);

        // La consulta externa se realiza sin mantener bloqueos en la BD.
        CotizacionDolarDTO cotizacion =
                cotizacionDolarService.obtenerCotizacion();

        TransactionTemplate transaccion =
                new TransactionTemplate(transactionManager);

        transaccion.setIsolationLevel(
                TransactionDefinition.ISOLATION_READ_COMMITTED
        );

        return transaccion.execute(estado ->
                guardarReserva(
                        email,
                        estacionId,
                        bicicletaIds,
                        duracionSegundos,
                        cotizacion
                )
        );
    }

    private Integer guardarReserva(
            String email,
            Integer estacionId,
            List<Integer> bicicletaIds,
            int duracionSegundos,
            CotizacionDolarDTO cotizacion
    ) {

        Usuario usuario = usuarioRepository
                .buscarActivoPorEmailConBloqueo(email)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "El usuario autenticado no existe o está inactivo."
                        )
                );

        // Se comprueba también el rol vigente en la base de datos.
        boolean clienteVigente = usuario.getUsuarioRoles().stream()
                .map(UsuarioRol::getRol)
                .anyMatch(rol -> "CLIENTE".equals(rol.getCodigo()));

        if (!clienteVigente) {
            throw new AccessDeniedException(
                    "La cuenta debe tener rol CLIENTE para contratar."
            );
        }

        comprobarSinSaldoPendiente(usuario);

        List<Bicicleta> bicicletas = new ArrayList<>();
        Set<Integer> tipoIds = new TreeSet<>();

        // Los bloqueos se solicitan en orden de ID.
        for (Integer bicicletaId : bicicletaIds) {

            Bicicleta bicicleta = bicicletaRepository
                    .buscarActivaPorIdConBloqueo(bicicletaId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Una de las bicicletas seleccionadas no existe."
                            )
                    );

            if (bicicleta.getEstado() == null
                    || !"DISPONIBLE".equals(
                    bicicleta.getEstado().getCodigo()
            )) {
                throw new IllegalStateException(
                        "Una de las bicicletas seleccionadas ya no está disponible."
                );
            }

            if (bicicleta.getUbicacionActual() == null
                    || !Objects.equals(
                    bicicleta.getUbicacionActual().getId(),
                    estacionId
            )) {
                throw new IllegalStateException(
                        "Todas las bicicletas deben estar en la estación elegida."
                );
            }

            if (bicicleta.getTipo() == null) {
                throw new ConfiguracionSistemaException(
                        "Una bicicleta no tiene un tipo configurado."
                );
            }

            if (!ordenMantenimientoRepository.buscarAbiertasPorBicicleta(bicicleta.getId()).isEmpty()
                    || preventivaVencidaEnDevolucion(bicicleta, LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC))) {
                throw new IllegalStateException("Una bicicleta requiere mantenimiento preventivo antes de otra reserva.");
            }

            bicicletas.add(bicicleta);
            tipoIds.add(bicicleta.getTipo().getId());
        }

        Estacion estacion = estacionRepository
                .buscarActivaPorIdConBloqueo(estacionId)
                .filter(Estacion::isEsPublica)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Debés elegir una estación pública habilitada."
                        )
                );

        Map<Integer, TipoBicicleta> tiposContratados = new TreeMap<>();

        for (Integer tipoId : tipoIds) {

            TipoBicicleta tipo = tipoBicicletaRepository
                    .buscarPorIdConBloqueo(tipoId)
                    .orElseThrow(() ->
                            new ConfiguracionSistemaException(
                                    "No se encontró el tipo de una bicicleta."
                            )
                    );

            if (tipo.getPrecioPorHoraUsd() == null
                    || tipo.getPrecioPorHoraUsd().compareTo(
                    new BigDecimal("0.01")
            ) < 0) {
                throw new ConfiguracionSistemaException(
                        "Un tipo de bicicleta no tiene una tarifa válida."
                );
            }

            tiposContratados.put(tipoId, tipo);
        }

        EstadoAlquiler estadoReserva = estadoAlquilerRepository
                .findByCodigo("RESERVADO")
                .orElseThrow(() ->
                        new ConfiguracionSistemaException(
                                "Falta el estado de alquiler RESERVADO."
                        )
                );

        EstadoAlquilerDetalle estadoDetalleReserva =
                estadoAlquilerDetalleRepository
                        .findByCodigo("RESERVADO")
                        .orElseThrow(() ->
                                new ConfiguracionSistemaException(
                                        "Falta el estado de detalle RESERVADO."
                                )
                        );

        EstadoBicicleta estadoBicicletaReservada =
                estadoBicicleta("RESERVADA");

        LocalDateTime ahora = LocalDateTime.now(reloj);

        Alquiler cabecera = new Alquiler();

        cabecera.setUsuario(usuario);
        cabecera.setEstadoContratacion(estadoReserva);
        cabecera.setUbicacionRetiro(estacion);
        cabecera.setFechaHoraCreacion(ahora);
        cabecera.setFechaHoraVencimientoReserva(ahora.plusMinutes(15));

        cabecera.setCotizacionArsPorUsd(
                cotizacion.getCotizacionArsPorUsd()
        );
        cabecera.setFuenteCotizacion(cotizacion.getFuente());
        cabecera.setFechaHoraAplicacionCotizacion(ahora);

        if (cotizacion.getFechaActualizacionFuente() != null) {
            cabecera.setFechaHoraActualizacionFuente(
                    cotizacion.getFechaActualizacionFuente()
                            .withOffsetSameInstant(ZoneOffset.UTC)
                            .toLocalDateTime()
            );
        }

        List<AlquilerDetalle> detalles = new ArrayList<>();

        BigDecimal totalUsd = new BigDecimal("0.00");
        BigDecimal totalArs = new BigDecimal("0.00");

        for (Bicicleta bicicleta : bicicletas) {

            TipoBicicleta tipo = tiposContratados.get(
                    bicicleta.getTipo().getId()
            );

            BigDecimal subtotalUsd =
                    calculadoraAlquiler.calcularSubtotalUsd(
                            tipo.getPrecioPorHoraUsd(),
                            duracionSegundos
                    );

            BigDecimal subtotalArs =
                    calculadoraAlquiler.calcularSubtotalArs(
                            tipo.getPrecioPorHoraUsd(),
                            duracionSegundos,
                            cotizacion.getCotizacionArsPorUsd()
                    );

            AlquilerDetalle detalle = new AlquilerDetalle();

            detalle.setAlquiler(cabecera);
            detalle.setBicicleta(bicicleta);
            detalle.setTipoBicicletaContratado(tipo);
            detalle.setEstado(estadoDetalleReserva);
            detalle.setPrecioPorHoraAplicadoUsd(
                    tipo.getPrecioPorHoraUsd()
            );
            detalle.setDuracionEstimadaSegundos(duracionSegundos);
            detalle.setSubtotalEstimadoUsd(subtotalUsd);
            detalle.setSubtotalEstimadoArs(subtotalArs);

            if (!validator.validate(detalle).isEmpty()) {
                throw new IllegalArgumentException(
                        "La duración y las tarifas generan importes "
                                + "fuera de los límites admitidos."
                );
            }

            detalles.add(detalle);

            totalUsd = totalUsd.add(subtotalUsd);
            totalArs = totalArs.add(subtotalArs);
        }

        cabecera.setCostoEstimadoUsd(totalUsd);
        cabecera.setCostoEstimadoArs(totalArs);

        if (!validator.validate(cabecera).isEmpty()) {
            throw new IllegalArgumentException(
                    "La contratación genera un importe "
                            + "fuera del límite admitido."
            );
        }

        Alquiler cabeceraGuardada = alquilerRepository.save(cabecera);

        for (AlquilerDetalle detalle : detalles) {

            detalle.setAlquiler(cabeceraGuardada);

            AlquilerDetalle detalleGuardado =
                    alquilerDetalleRepository.save(detalle);

            historialEstadoDetalleRepository.save(
                    new HistorialEstadoDetalle(
                            detalleGuardado,
                            null,
                            estadoDetalleReserva,
                            usuario,
                            "USUARIO",
                            "Reserva creada.",
                            ahora
                    )
            );

            Bicicleta bicicleta = detalleGuardado.getBicicleta();
            EstadoBicicleta estadoAnterior = bicicleta.getEstado();

            bicicleta.setEstado(estadoBicicletaReservada);

            // Reservar no implica retirar físicamente la bicicleta.
            bicicletaRepository.save(bicicleta);

            trazabilidadInventario.estado(
                    bicicleta,
                    estadoAnterior,
                    usuario,
                    "Reserva del alquiler "
                            + cabeceraGuardada.getId()
                            + "."
            );
        }

        return cabeceraGuardada.getId();
    }

    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public AlquilerDetalleDTO retirarBicicleta(
            Integer alquilerId,
            Integer detalleId
    ) {
        if (alquilerId == null || alquilerId <= 0
                || detalleId == null || detalleId <= 0) {
            throw new IllegalArgumentException(
                    "Los identificadores del alquiler y la bicicleta "
                            + "del alquiler deben ser válidos."
            );
        }

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getAuthorities().stream().noneMatch(
                autoridad -> "CLIENTE".equals(autoridad.getAuthority())
        )) {
            throw new AccessDeniedException(
                    "Se requiere una sesión con rol CLIENTE."
            );
        }

        // Mismo orden inicial de bloqueo que las operaciones de pago.
        Usuario usuario = usuarioRepository
                .buscarActivoPorEmailConBloqueo(auth.getName())
                .filter(Usuario::isActivo)
                .filter(cuenta -> cuenta.getUsuarioRoles().stream()
                        .anyMatch(asignacion ->
                                asignacion.getRol() != null
                                        && "CLIENTE".equals(
                                        asignacion.getRol().getCodigo()
                                )
                        )
                )
                .orElseThrow(() -> new AccessDeniedException(
                        "La cuenta está inactiva o no tiene rol CLIENTE vigente."
                ));

        Alquiler alquiler = alquilerRepository
                .buscarPorIdConBloqueo(alquilerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "El alquiler no existe."
                ));

        if (alquiler.getUsuario() == null
                || alquiler.getUsuario().getId() != usuario.getId()) {
            throw new AccessDeniedException(
                    "No podés retirar bicicletas de un alquiler ajeno."
            );
        }

        if (alquiler.isBorradoAlquiler()
                || alquiler.getEstadoContratacion() == null
                || !Set.of("RESERVADO", "EN_PROGRESO").contains(
                alquiler.getEstadoContratacion().getCodigo()
        )) {
            throw new IllegalStateException(
                    "El alquiler no admite retiros."
            );
        }

        AlquilerDetalle detalle = alquilerDetalleRepository
                .buscarPorAlquilerYDetalleConBloqueo(
                        alquilerId,
                        detalleId
                )
                .orElseThrow(() -> new EntityNotFoundException(
                        "La bicicleta indicada no pertenece a este alquiler."
                ));

        if (detalle.getEstado() == null) {
            throw new ConfiguracionSistemaException(
                    "El detalle no tiene un estado configurado."
            );
        }

        String estadoActual = detalle.getEstado().getCodigo();

        // Un reintento no vuelve a iniciar el contador ni agrega historias.
        if ("EN_CURSO".equals(estadoActual)) {
            return new AlquilerDetalleDTO(detalle);
        }

        if (!"RESERVADO".equals(estadoActual)
                && !"PENDIENTE_RETIRO".equals(estadoActual)) {
            throw new IllegalStateException(
                    "Esta bicicleta ya no está pendiente de retiro."
            );
        }

        if (detalle.getFechaHoraRetiro() != null
                || detalle.getFechaHoraDevolucionReal() != null
                || detalle.getFechaHoraFinUso() != null
                || detalle.getFechaHoraCierreDetalle() != null
                || detalle.getDuracionEstimadaSegundos() == null
                || detalle.getDuracionEstimadaSegundos() <= 0) {
            throw new ConfiguracionSistemaException(
                    "Las fechas o la duración del detalle son inconsistentes."
            );
        }

        if (detalle.isFallaReportada()
                || detalle.getOrdenMantenimiento() != null
                || detalle.getIncidenciaCorte() != null) {
            throw new IllegalStateException(
                    "La bicicleta tiene una revisión o incidencia "
                            + "que impide su retiro."
            );
        }

        Bicicleta bicicleta = bicicletaRepository
                .buscarActivaPorIdConBloqueo(
                        detalle.getBicicleta().getId()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "La bicicleta no está habilitada para el retiro."
                ));

        if (bicicleta.getEstado() == null
                || !"RESERVADA".equals(bicicleta.getEstado().getCodigo())
                || alquiler.getUbicacionRetiro() == null
                || bicicleta.getUbicacionActual() == null
                || bicicleta.getUbicacionActual().getId()
                != alquiler.getUbicacionRetiro().getId()) {
            throw new IllegalStateException(
                    "La bicicleta no está reservada en la estación de origen."
            );
        }

        estacionRepository.buscarActivaPorIdConBloqueo(
                        alquiler.getUbicacionRetiro().getId()
                )
                .filter(Estacion::isEsPublica)
                .orElseThrow(() -> new IllegalStateException(
                        "La estación de retiro no está habilitada."
                ));

        BigDecimal anticipoRequerido =
                comprobarAnticipoParaRetiro(alquiler);

        if ("RESERVADO".equals(estadoActual)
                && anticipoRequerido.signum() > 0) {
            throw new IllegalStateException(
                    "El anticipo todavía no habilitó el retiro."
            );
        }

        EstadoAlquilerDetalle pendienteRetiro =
                estadoAlquilerDetalleRepository
                        .findByCodigo("PENDIENTE_RETIRO")
                        .orElseThrow(() -> new ConfiguracionSistemaException(
                                "Falta el estado PENDIENTE_RETIRO."
                        ));

        EstadoAlquilerDetalle enCurso =
                estadoAlquilerDetalleRepository
                        .findByCodigo("EN_CURSO")
                        .orElseThrow(() -> new ConfiguracionSistemaException(
                                "Falta el estado EN_CURSO."
                        ));

        EstadoAlquiler enProgreso = estadoAlquilerRepository
                .findByCodigo("EN_PROGRESO")
                .orElseThrow(() -> new ConfiguracionSistemaException(
                        "Falta el estado EN_PROGRESO."
                ));

        EstadoBicicleta enUso = estadoBicicleta("EN_USO");

        // Comprobar el plazo después de adquirir bloqueos y consultar datos.
        LocalDateTime ahora = LocalDateTime.ofInstant(
                reloj.instant(),
                ZoneOffset.UTC
        );

        LocalDateTime creacion = alquiler.getFechaHoraCreacion();
        LocalDateTime vencimiento =
                alquiler.getFechaHoraVencimientoReserva();

        if (creacion == null || vencimiento == null
                || !vencimiento.equals(creacion.plusMinutes(15))
                || ahora.isBefore(creacion)) {
            throw new ConfiguracionSistemaException(
                    "Las fechas de la reserva son inconsistentes."
            );
        }

        if (!ahora.isBefore(vencimiento)) {
            throw new IllegalStateException(
                    "La reserva venció. Ya no podés retirar esta bicicleta."
            );
        }

        EstadoAlquilerDetalle anteriorDetalle = detalle.getEstado();

        // Anticipo requerido cero: habilitar sin inventar un pago aprobado.
        if ("RESERVADO".equals(estadoActual)) {
            detalle.setEstado(pendienteRetiro);

            historialEstadoDetalleRepository.save(
                    new HistorialEstadoDetalle(
                            detalle,
                            anteriorDetalle,
                            pendienteRetiro,
                            null,
                            "SISTEMA",
                            "Retiro habilitado sin pago: anticipo requerido cero.",
                            ahora
                    )
            );

            anteriorDetalle = pendienteRetiro;
        }

        detalle.setEstado(enCurso);
        detalle.setFechaHoraRetiro(ahora);
        detalle.setFechaHoraDevolucionEstimada(
                ahora.plusSeconds(detalle.getDuracionEstimadaSegundos())
        );

        EstadoBicicleta anteriorBicicleta = bicicleta.getEstado();

        bicicleta.setEstado(enUso);
        bicicleta.setUbicacionActual(null);

        alquiler.setEstadoContratacion(enProgreso);

        historialEstadoDetalleRepository.save(
                new HistorialEstadoDetalle(
                        detalle,
                        anteriorDetalle,
                        enCurso,
                        usuario,
                        "USUARIO",
                        "Retiro confirmado por el cliente propietario.",
                        ahora
                )
        );

        trazabilidadInventario.estado(
                bicicleta,
                anteriorBicicleta,
                usuario,
                "Retiro del alquiler " + alquilerId
                        + ", detalle " + detalleId + ".",
                ahora
        );

        // Las entidades consultadas están administradas por JPA.
        // Flush persiste conjuntamente sus cambios y las historias.
        alquilerDetalleRepository.flush();

        return new AlquilerDetalleDTO(detalle);
    }

    @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public AlquilerDetalleDTO devolverBicicleta(
            Integer alquilerId, Integer detalleId, DevolverBicicletaDTO solicitud
    ) {
        if (alquilerId == null || alquilerId <= 0 || detalleId == null || detalleId <= 0
                || solicitud == null) {
            throw new IllegalArgumentException("Debe identificar el alquiler, su bicicleta y la devolución.");
        }
        Set<ConstraintViolation<DevolverBicicletaDTO>> errores = validator.validate(solicitud);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(errores.stream().map(ConstraintViolation::getMessage)
                    .sorted().collect(java.util.stream.Collectors.joining(" ")));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Debe iniciar sesión para devolver una bicicleta.");
        }
        Usuario actor = usuarioRepository.buscarActivoPorEmailConBloqueo(auth.getName())
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new AccessDeniedException("La cuenta no está activa."));
        // El permiso debe existir tanto en la sesión como en los roles vigentes de la BD.
        boolean esAdmin = tieneRolVigenteParaDevolucion(actor, auth, "ADMIN");
        boolean esCliente = tieneRolVigenteParaDevolucion(actor, auth, "CLIENTE");
        if (!esAdmin && !esCliente) {
            throw new AccessDeniedException("No tiene permiso vigente para devolver bicicletas.");
        }

        // Mismo orden que retiro: usuario -> cabecera -> detalle -> bicicleta -> estación.
        Alquiler alquiler = alquilerRepository.buscarPorIdConBloqueo(alquilerId)
                .orElseThrow(() -> new EntityNotFoundException("El alquiler no existe."));
        if (!esAdmin && (alquiler.getUsuario() == null
                || alquiler.getUsuario().getId() != actor.getId())) {
            throw new AccessDeniedException("No podés devolver bicicletas de un alquiler ajeno.");
        }
        if (alquiler.isBorradoAlquiler() || alquiler.getEstadoContratacion() == null) {
            throw new IllegalStateException("El alquiler no admite esta devolución por detalle.");
        }
        AlquilerDetalle detalle = alquilerDetalleRepository
                .buscarPorAlquilerYDetalleConBloqueo(alquilerId, detalleId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "La bicicleta indicada no pertenece a este alquiler."));
        if (detalle.getEstado() == null) {
            throw new ConfiguracionSistemaException("El detalle no tiene estado configurado.");
        }

        // No tocar otra vez la bici: podría haberse alquilado de nuevo o trasladado.
        if ("DEVUELTO".equals(detalle.getEstado().getCodigo())) {
            if (detalle.getEstacionDevolucion() == null
                    || detalle.getEstacionDevolucion().getId() != solicitud.getEstacionDevolucionId()) {
                throw new IllegalStateException("La devolución ya fue registrada en otra estación.");
            }
            if (Boolean.TRUE.equals(solicitud.getFallaReportada())
                    && (!detalle.isFallaReportada()
                    || !Objects.equals(solicitud.getDescripcionFalla(), detalle.getDescripcionFalla()))) {
                throw new IllegalStateException("La devolución ya fue registrada. Un reintento no puede agregar ni reemplazar su reporte.");
            }
            return new AlquilerDetalleDTO(detalle);
        }
        if (!"EN_PROGRESO".equals(alquiler.getEstadoContratacion().getCodigo())
                || !"EN_CURSO".equals(detalle.getEstado().getCodigo())) {
            throw new IllegalStateException("Solo puede devolverse una bicicleta que está en uso.");
        }
        if (alquiler.getFechaHoraLiquidacion() != null || alquiler.getCostoFinalArs() != null
                || alquiler.getCostoFinalUsd() != null || alquiler.getFechaHoraCierre() != null
                || detalle.getFechaHoraRetiro() == null || detalle.getFechaHoraDevolucionEstimada() == null
                || detalle.getFechaHoraDevolucionReal() != null || detalle.getFechaHoraFinUso() != null
                || detalle.getFechaHoraCierreDetalle() != null) {
            throw new ConfiguracionSistemaException("El alquiler presenta fechas o importes incompatibles con un uso abierto.");
        }
        if (detalle.getPrecioPorHoraAplicadoUsd() == null
                || detalle.getPrecioPorHoraAplicadoUsd().compareTo(new BigDecimal("0.01")) < 0
                || alquiler.getCotizacionArsPorUsd() == null || alquiler.getCotizacionArsPorUsd().signum() <= 0) {
            throw new ConfiguracionSistemaException("Faltan los valores congelados del alquiler.");
        }

        Bicicleta bicicleta = bicicletaRepository.buscarActivaPorIdConBloqueo(detalle.getBicicleta().getId())
                .orElseThrow(() -> new IllegalStateException("La bicicleta no está habilitada para esta devolución."));
        if (bicicleta.getEstado() == null || !"EN_USO".equals(bicicleta.getEstado().getCodigo())
                || bicicleta.getUbicacionActual() != null) {
            throw new IllegalStateException("La bicicleta no figura en uso y fuera de estación.");
        }
        Estacion destino = estacionRepository.buscarActivaPorIdConBloqueo(solicitud.getEstacionDevolucionId())
                .filter(Estacion::isEsPublica)
                .orElseThrow(() -> new IllegalStateException("La estación de devolución no está habilitada para clientes."));
        if (bicicletaRepository.countByUbicacionActual(destino) >= destino.getCapacidad()) {
            throw new IllegalStateException("La estación de devolución no tiene espacios libres.");
        }

        List<Incidencia> incidencias = incidenciaRepository.findByAlquilerDetalleIdOrderByIdAsc(detalleId);
        boolean revisionEconomicaPendiente = incidencias.stream().anyMatch(i ->
                "PENDIENTE".equals(i.getEstadoRevision()) && i.isRequiereRevisionEconomica());
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        if (ahora.isBefore(detalle.getFechaHoraRetiro())
                || detalle.getFechaHoraDevolucionEstimada().isBefore(detalle.getFechaHoraRetiro())) {
            throw new ConfiguracionSistemaException("Las fechas registradas del uso son inconsistentes.");
        }
        LocalDateTime finUso = finUsoEnDevolucion(detalle, ahora);
        BigDecimal subtotalUsd = null;
        BigDecimal subtotalArs = null;
        if (!revisionEconomicaPendiente) {
            long segundos = calculadoraAlquiler.calcularSegundosCompletos(detalle.getFechaHoraRetiro(), finUso);
            subtotalUsd = calculadoraAlquiler.calcularSubtotalUsd(detalle.getPrecioPorHoraAplicadoUsd(), segundos);
            subtotalArs = calculadoraAlquiler.calcularSubtotalArs(detalle.getPrecioPorHoraAplicadoUsd(), segundos,
                    alquiler.getCotizacionArsPorUsd());
            if (subtotalUsd.precision() - subtotalUsd.scale() > 18
                    || subtotalArs.precision() - subtotalArs.scale() > 18) {
                throw new ConfiguracionSistemaException("El subtotal de uso excede la capacidad monetaria del detalle.");
            }
        }

        EstadoAlquilerDetalle devuelto = estadoAlquilerDetalleRepository.findByCodigo("DEVUELTO")
                .orElseThrow(() -> new ConfiguracionSistemaException("Falta el estado DEVUELTO."));
        boolean nuevaFalla = Boolean.TRUE.equals(solicitud.getFallaReportada()) && !detalle.isFallaReportada();
        boolean requiereRevision = detalle.isFallaReportada() || nuevaFalla || !incidencias.isEmpty();
        OrdenMantenimiento orden = prepararMantenimientoDevolucion(bicicleta, detalle, requiereRevision, ahora);
        EstadoBicicleta estadoDestino = estadoBicicleta(orden == null ? "DISPONIBLE" : "MANTENIMIENTO");
        EstadoAlquilerDetalle anteriorDetalle = detalle.getEstado();
        EstadoBicicleta anteriorBicicleta = bicicleta.getEstado();

        detalle.setEstado(devuelto);
        detalle.setEstacionDevolucion(destino);
        detalle.setFechaHoraDevolucionReal(ahora);
        detalle.setFechaHoraFinUso(finUso);
        detalle.setFechaHoraCierreDetalle(ahora);
        detalle.setSubtotalRealUsd(subtotalUsd);
        detalle.setSubtotalRealArs(subtotalArs);
        detalle.setOrigenCierre(esAdmin ? "ADMIN" : "CLIENTE");
        detalle.setMotivoCierre("Devolución física confirmada" + (esAdmin ? " por ADMIN." : " por el titular."));
        if (nuevaFalla) {
            detalle.setFallaReportada(true);
            detalle.setDescripcionFalla(solicitud.getDescripcionFalla());
            detalle.setFechaReporteFalla(ahora);
            detalle.setReportadoPor(actor);
        }
        if (orden != null) {
            detalle.setOrdenMantenimiento(orden);
        }
        bicicleta.setUbicacionActual(destino);
        bicicleta.setEstado(estadoDestino);

        if (!incidencias.isEmpty()) {
            incidenciaService.registrarDevolucion(detalle, actor, ahora);
        }

        String condiciones = "Devolución " + (ahora.isAfter(detalle.getFechaHoraDevolucionEstimada())
                ? "con demora" : "dentro del tiempo estimado")
                + (detalle.isFallaReportada() ? "; falla reportada" : "; sin falla reportada")
                + "; estación " + destino.getId()
                + (revisionEconomicaPendiente ? "; importe pendiente de revisión económica." : "; costo por uso real.");
        historialEstadoDetalleRepository.save(new HistorialEstadoDetalle(
                detalle, anteriorDetalle, devuelto, actor, "USUARIO", condiciones, ahora));
        trazabilidadInventario.estado(bicicleta, anteriorBicicleta, actor,
                "Devolución del alquiler " + alquilerId + ", detalle " + detalleId + ". " + condiciones, ahora);

        cerrarUsoDelConjuntoSiCorresponde(alquiler, ahora);
        // La liquidación se decide al cerrar el conjunto.
        // Devolver no genera cobros ni reembolsos.
        alquilerDetalleRepository.flush();
        return new AlquilerDetalleDTO(detalle);
    }

    @PreAuthorize("hasAnyAuthority('CLIENTE','ADMIN')")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public AlquilerCabeceraDTO cancelarReserva(Integer alquilerId, Integer detalleId,
                                               CancelarDetalleDTO solicitud, boolean administrativa) {
        if (alquilerId == null || alquilerId <= 0 || (detalleId != null && detalleId <= 0)) {
            throw new IllegalArgumentException("Debe indicar un alquiler y detalle válidos.");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Debe iniciar sesión.");
        Usuario actor = usuarioRepository.buscarActivoPorEmailConBloqueo(auth.getName())
                .filter(Usuario::isActivo).orElseThrow(() -> new AccessDeniedException("La cuenta no está activa."));
        if (!tieneRolVigenteParaDevolucion(actor, auth, administrativa ? "ADMIN" : "CLIENTE")) {
            throw new AccessDeniedException("La cuenta no tiene el rol vigente requerido.");
        }
        Alquiler alquiler = alquilerRepository.buscarPorIdConBloqueo(alquilerId)
                .orElseThrow(() -> new EntityNotFoundException("El alquiler no existe."));
        if (!administrativa && (alquiler.getUsuario() == null || alquiler.getUsuario().getId() != actor.getId())) {
            throw new AccessDeniedException("No podés cancelar un alquiler ajeno.");
        }
        if (alquiler.getEstadoContratacion() == null) throw new IllegalStateException("Este alquiler pertenece al circuito anterior.");
        if (administrativa && (solicitud == null || !validator.validate(solicitud).isEmpty())) {
            throw new IllegalArgumentException("Debe indicar un motivo y un estado operativo válidos.");
        }
        List<AlquilerDetalle> todos = alquilerDetalleRepository.buscarPorAlquiler(alquilerId);
        if (detalleId != null) {
            AlquilerDetalle elegido = todos.stream().filter(d -> detalleId.equals(d.getId())).findFirst()
                    .orElseThrow(() -> new EntityNotFoundException("El detalle no pertenece a este alquiler."));
            if (Set.of("CANCELADO","VENCIDO").contains(elegido.getEstado().getCodigo())) {
                return new AlquilerCabeceraDTO(alquiler, todos);
            }
            if (!pendienteDeRetiro(elegido)) throw new IllegalStateException("Una bicicleta retirada debe devolverse; no puede cancelarse.");
        }
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        comprobarFechasReserva(alquiler, ahora);
        boolean vencida = !ahora.isBefore(alquiler.getFechaHoraVencimientoReserva());
        int cambios = terminarPendientes(alquiler, vencida ? null : detalleId,
                vencida ? null : actor, vencida ? "VENCIDO" : "CANCELADO",
                vencida ? null : solicitud, !vencida && administrativa, ahora);
        if (cambios == 0 && todos.stream().anyMatch(d -> "EN_CURSO".equals(d.getEstado().getCodigo()))) {
            throw new IllegalStateException("No quedan unidades sin retirar; debe devolver las que están en uso.");
        }
        return new AlquilerCabeceraDTO(alquiler, alquilerDetalleRepository.buscarPorAlquilerConDatos(alquilerId));
    }

    // Uso interno del programador; sin endpoint que permita elegir el estado del conjunto.
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public int vencerReserva(Integer alquilerId) {
        Alquiler alquiler = alquilerRepository.buscarPorIdConBloqueo(alquilerId).orElse(null);
        if (alquiler == null || alquiler.getEstadoContratacion() == null
                || !Set.of("RESERVADO","EN_PROGRESO").contains(alquiler.getEstadoContratacion().getCodigo())) return 0;
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        comprobarFechasReserva(alquiler, ahora);
        if (ahora.isBefore(alquiler.getFechaHoraVencimientoReserva())) return 0;
        return terminarPendientes(alquiler, null, null, "VENCIDO", null, false, ahora);
    }

    private void comprobarFechasReserva(Alquiler alquiler, LocalDateTime ahora) {
        if (alquiler.getFechaHoraCreacion() == null || alquiler.getFechaHoraVencimientoReserva() == null
                || !alquiler.getFechaHoraVencimientoReserva().equals(alquiler.getFechaHoraCreacion().plusMinutes(15))
                || ahora.isBefore(alquiler.getFechaHoraCreacion())) {
            throw new ConfiguracionSistemaException("Las fechas de reserva son inconsistentes.");
        }
    }

    private boolean pendienteDeRetiro(AlquilerDetalle detalle) {
        return detalle.getEstado() != null && Set.of("RESERVADO","PENDIENTE_RETIRO").contains(detalle.getEstado().getCodigo());
    }

    private int terminarPendientes(Alquiler alquiler, Integer seleccion, Usuario actor, String estado,
            CancelarDetalleDTO solicitud, boolean administrativa, LocalDateTime ahora) {
        List<AlquilerDetalle> pendientes = alquilerDetalleRepository.buscarPorAlquiler(alquiler.getId()).stream()
                .filter(this::pendienteDeRetiro).filter(d -> seleccion == null || seleccion.equals(d.getId()))
                .sorted(java.util.Comparator.comparing(d -> d.getBicicleta().getId())).collect(java.util.stream.Collectors.toList());
        if (pendientes.isEmpty()) return 0;
        EstadoAlquilerDetalle cerrado = estadoAlquilerDetalleRepository.findByCodigo(estado)
                .orElseThrow(() -> new ConfiguracionSistemaException("Falta el estado de detalle " + estado));
        for (AlquilerDetalle d : pendientes) {
            Bicicleta bici = bicicletaRepository.buscarActivaPorIdConBloqueo(d.getBicicleta().getId())
                    .orElseThrow(() -> new ConfiguracionSistemaException("La bicicleta reservada no está disponible en inventario."));
            if (d.getFechaHoraRetiro() != null || !"RESERVADA".equals(bici.getEstado().getCodigo())
                    || bici.getUbicacionActual() == null || alquiler.getUbicacionRetiro() == null
                    || bici.getUbicacionActual().getId() != alquiler.getUbicacionRetiro().getId()
                    || alquilerDetalleRepository.existeOtraAsignacionVigente(bici.getId(), d.getId())) {
                throw new ConfiguracionSistemaException("La reserva no coincide con la asignación actual de la bicicleta.");
            }
            String motivo = "VENCIDO".equals(estado) ? "Venció el plazo original de quince minutos sin retiro."
                    : administrativa ? solicitud.getMotivo() : "Cancelación voluntaria del cliente antes del retiro.";
            boolean preventiva = preventivaVencidaEnDevolucion(bici, ahora);
            boolean ordenAbierta = !ordenMantenimientoRepository.buscarAbiertasPorBicicleta(bici.getId()).isEmpty();
            String destino = administrativa ? solicitud.getEstadoBicicleta()
                    : preventiva || ordenAbierta ? "MANTENIMIENTO" : "DISPONIBLE";
            if ("MANTENIMIENTO".equals(destino)) {
                d.setOrdenMantenimiento(prepararMantenimientoDevolucion(bici, d, administrativa, ahora));
            }
            EstadoBicicleta anteriorBici = bici.getEstado();
            bici.setEstado(estadoBicicleta(destino));
            EstadoAlquilerDetalle anteriorDetalle = d.getEstado();
            d.setEstado(cerrado); d.setSubtotalRealUsd(new BigDecimal("0.00"));
            d.setSubtotalRealArs(new BigDecimal("0.00")); d.setFechaHoraCierreDetalle(ahora);
            d.setOrigenCierre(actor == null ? "SISTEMA" : administrativa ? "ADMIN" : "CLIENTE");
            d.setMotivoCierre(motivo);
            historialEstadoDetalleRepository.save(new HistorialEstadoDetalle(d, anteriorDetalle, cerrado,
                    actor, actor == null ? "SISTEMA" : "USUARIO", motivo, ahora));
            if (actor == null) trazabilidadInventario.estadoSistema(bici, anteriorBici, motivo, ahora);
            else trazabilidadInventario.estado(bici, anteriorBici, actor, motivo, ahora);
            if (administrativa) trazabilidadInventario.administracion(actor, "ALQUILER_DETALLE", d.getId(),
                    "CANCELAR", anteriorDetalle.getCodigo(), cerrado.getCodigo() + ";bicicleta=" + destino, motivo);
        }
        cerrarUsoDelConjuntoSiCorresponde(alquiler, ahora);
        alquilerDetalleRepository.flush();
        return pendientes.size();
    }

    private boolean tieneRolVigenteParaDevolucion(Usuario actor, Authentication auth, String codigo) {
        return auth.getAuthorities().stream().anyMatch(a -> codigo.equals(a.getAuthority()))
                && actor.getUsuarioRoles().stream().anyMatch(ur ->
                ur.getRol() != null && codigo.equals(ur.getRol().getCodigo()));
    }

    private LocalDateTime finUsoEnDevolucion(AlquilerDetalle detalle, LocalDateTime recepcion) {
        Incidencia corte = detalle.getIncidenciaCorte();
        if (corte == null) {
            return recepcion;
        }
        LocalDateTime fecha = corte.getFechaHoraCorteValidado();
        if (!"VALIDADA".equals(corte.getEstadoRevision()) || corte.getAlquilerDetalle() == null
                || !Objects.equals(corte.getAlquilerDetalle().getId(), detalle.getId())
                || fecha == null || corte.getFechaHoraRegistro() == null || corte.getFechaHoraDecision() == null
                || corte.getResueltoPor() == null || fecha.isBefore(detalle.getFechaHoraRetiro())
                || fecha.isAfter(corte.getFechaHoraRegistro())
                || corte.getFechaHoraRegistro().isAfter(corte.getFechaHoraDecision())
                || corte.getFechaHoraDecision().isAfter(recepcion)) {
            throw new ConfiguracionSistemaException("El corte de uso no tiene una decisión válida para este detalle.");
        }
        return fecha;
    }

    private OrdenMantenimiento prepararMantenimientoDevolucion(
            Bicicleta bicicleta, AlquilerDetalle detalle, boolean revisionCorrectiva, LocalDateTime ahora
    ) {
        List<OrdenMantenimiento> abiertas = ordenMantenimientoRepository.buscarAbiertasPorBicicleta(bicicleta.getId());
        if (abiertas.size() > 1) {
            throw new ConfiguracionSistemaException("La bicicleta tiene más de una orden abierta.");
        }
        boolean preventivaVencida = preventivaVencidaEnDevolucion(bicicleta, ahora);
        if (abiertas.isEmpty() && !revisionCorrectiva && !preventivaVencida) {
            return null;
        }
        OrdenMantenimiento orden;
        String accion;
        if (abiertas.isEmpty()) {
            String tipo = revisionCorrectiva ? "CORRECTIVO" : "PREVENTIVO";
            TipoMantenimiento tipoOrden = tipoMantenimientoRepository.findByCodigo(tipo)
                    .orElseThrow(() -> new ConfiguracionSistemaException("Falta el tipo de mantenimiento " + tipo));
            EstadoOrdenMantenimiento creada = estadoOrdenMantenimientoRepository.findByCodigo("CREADA")
                    .orElseThrow(() -> new ConfiguracionSistemaException("Falta el estado de orden CREADA."));
            orden = ordenMantenimientoRepository.save(new OrdenMantenimiento(bicicleta, tipoOrden, creada, ahora));
            accion = "CREAR";
        } else {
            orden = abiertas.get(0);
            accion = "VINCULAR_DEVOLUCION";
        }
        // La orden la genera/vincula el sistema; el actor humano del reporte se conserva en el detalle.
        historialOrdenMantenimientoRepository.save(new HistorialOrdenMantenimiento(
                orden, accion, null, "CREAR".equals(accion) ? orden.getEstado() : null,
                null, "SISTEMA", "Devolución del detalle " + detalle.getId()
                + (revisionCorrectiva ? "; requiere revisión por reporte o incidencia." : "; preventiva vencida.")
                + (preventivaVencida ? " La necesidad preventiva permanece pendiente." : ""),
                null, "detalle_id=" + detalle.getId(), ahora));
        return orden;
    }

    private boolean preventivaVencidaEnDevolucion(Bicicleta bicicleta, LocalDateTime ahora) {
        Integer meses = bicicleta.getTipo() == null ? null : bicicleta.getTipo().getIntervaloPreventivoMeses();
        LocalDateTime ultima = ordenMantenimientoRepository.buscarUltimaPreventivaApta(bicicleta.getId());
        return CalendarioPreventivo.vencida(bicicleta.getFechaAlta(), ultima, meses, ahora);
    }

    private void cerrarUsoDelConjuntoSiCorresponde(
            Alquiler alquiler,
            LocalDateTime ahora
    ) {
        List<AlquilerDetalle> detalles =
                alquilerDetalleRepository.buscarPorAlquiler(alquiler.getId());

        Set<String> terminales = Set.of(
                "DEVUELTO", "CANCELADO", "VENCIDO", "CERRADO_POR_INCIDENCIA"
        );

        Set<String> conocidos = Set.of(
                "RESERVADO", "PENDIENTE_RETIRO", "EN_CURSO",
                "DEVUELTO", "CANCELADO", "VENCIDO", "CERRADO_POR_INCIDENCIA"
        );

        if (detalles.isEmpty() || detalles.stream().anyMatch(
                detalle -> detalle.getEstado() == null
                        || !conocidos.contains(detalle.getEstado().getCodigo())
        )) {
            throw new ConfiguracionSistemaException(
                    "No se puede determinar el estado del conjunto."
            );
        }

        boolean todosTerminales = detalles.stream().allMatch(
                detalle -> terminales.contains(detalle.getEstado().getCodigo())
        );

        if (todosTerminales) {
            boolean huboRetiro = detalles.stream().anyMatch(d -> d.getFechaHoraRetiro() != null);
            String estadoCierre = huboRetiro ? "FINALIZADO"
                    : detalles.stream().anyMatch(d -> "VENCIDO".equals(d.getEstado().getCodigo())) ? "VENCIDO" : "CANCELADO";
            EstadoAlquiler finalizado = estadoAlquilerRepository
                    .findByCodigo(estadoCierre)
                    .orElseThrow(() -> new ConfiguracionSistemaException(
                            "Falta el estado " + estadoCierre + "."
                    ));

            alquiler.setEstadoContratacion(finalizado);
            alquiler.setFechaHoraCierre(ahora);
            if ("CANCELADO".equals(estadoCierre)) alquiler.setFechaHoraCancelacion(ahora);

            // Misma transacción y cabecera bloqueada.
            // Si hay revisión económica pendiente, todavía no fija la liquidación.
            liquidacionAlquilerService.liquidarSiCorresponde(alquiler);
            if (reembolsoService != null) reembolsoService.registrarPendientesSiCorresponde(alquiler);
        }
    }

    private BigDecimal comprobarAnticipoParaRetiro(Alquiler alquiler) {
        List<AlquilerDetalle> detalles = alquilerDetalleRepository
                .buscarPorAlquilerConDatos(alquiler.getId());

        if (detalles.isEmpty()) {
            throw new ConfiguracionSistemaException(
                    "El alquiler no tiene detalles."
            );
        }

        BigDecimal requerido = new BigDecimal("0.00");

        for (AlquilerDetalle unidad : detalles) {
            String estado = unidad.getEstado() == null
                    ? null
                    : unidad.getEstado().getCodigo();

            if (estado == null || !Set.of(
                    "RESERVADO", "PENDIENTE_RETIRO", "EN_CURSO",
                    "DEVUELTO", "CANCELADO", "VENCIDO",
                    "CERRADO_POR_INCIDENCIA"
            ).contains(estado)) {
                throw new ConfiguracionSistemaException(
                        "Un detalle tiene un estado inválido."
                );
            }

            if (!"CANCELADO".equals(estado) && !"VENCIDO".equals(estado)) {
                requerido = requerido.add(
                        importeValidoParaRetiro(
                                unidad.getSubtotalEstimadoArs()
                        )
                );
            }
        }

        BigDecimal disponible = new BigDecimal("0.00");
        LocalDateTime referencia = LocalDateTime.ofInstant(
                reloj.instant(),
                ZoneOffset.UTC
        );

        for (Pago pago : pagoRepository
                .findByAlquilerIdOrderByIdAsc(alquiler.getId())) {
            if (!"ANTICIPO".equals(pago.getFinalidad())) {
                continue;
            }

            String estado = pago.getEstadoPago() == null
                    ? null
                    : pago.getEstadoPago().getCodigo();

            if (estado == null || !Set.of(
                    "CREADO", "PENDIENTE", "APROBADO",
                    "RECHAZADO", "CANCELADO"
            ).contains(estado)) {
                throw new ConfiguracionSistemaException(
                        "Un anticipo tiene un estado inválido."
                );
            }

            if (!"APROBADO".equals(estado)) {
                continue;
            }

            BigDecimal neto = importeValidoParaRetiro(pago.getMontoArs());

            if (neto.signum() == 0 || !"ARS".equals(pago.getMoneda())
                    || pago.getFechaCreacion() == null
                    || pago.getFechaConfirmacion() == null
                    || pago.getFechaConfirmacion().isBefore(
                    pago.getFechaCreacion()
            )
                    || pago.getFechaConfirmacion().isAfter(referencia)) {
                throw new ConfiguracionSistemaException(
                        "Un anticipo aprobado tiene datos inconsistentes."
                );
            }

            for (Reembolso reembolso : reembolsoRepository
                    .findByPagoIdOrderByIdAsc(pago.getId())) {
                String estadoReembolso = reembolso.getEstado();

                if (estadoReembolso == null || !Set.of(
                        "PENDIENTE", "EN_PROCESO", "CONFIRMADO", "FALLIDO"
                ).contains(estadoReembolso)) {
                    throw new ConfiguracionSistemaException(
                            "Un reembolso tiene un estado inválido."
                    );
                }

                if (!"FALLIDO".equals(estadoReembolso)) {
                    BigDecimal importe = importeValidoParaRetiro(
                            reembolso.getMontoArs()
                    );

                    if (importe.signum() == 0
                            || !"ARS".equals(reembolso.getMoneda())) {
                        throw new ConfiguracionSistemaException(
                                "Un reembolso tiene un importe inválido."
                        );
                    }

                    // No reutilizar dinero ya devuelto o comprometido.
                    neto = neto.subtract(importe);
                }
            }

            if (neto.signum() < 0) {
                throw new ConfiguracionSistemaException(
                        "Los reembolsos comprometidos superan el anticipo."
                );
            }

            disponible = disponible.add(neto);
        }

        if (disponible.compareTo(requerido) < 0) {
            throw new IllegalStateException(
                    "El anticipo confirmado no cubre las bicicletas del alquiler."
            );
        }

        return requerido;
    }

    private BigDecimal importeValidoParaRetiro(BigDecimal importe) {
        if (importe == null || importe.signum() < 0) {
            throw new ConfiguracionSistemaException(
                    "Hay un importe de alquiler inválido."
            );
        }

        try {
            return importe.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException ex) {
            throw new ConfiguracionSistemaException(
                    "Un importe tiene decimales incompatibles."
            );
        }
    }



    // BORRAR LÓGICAMENTE UN ALQUILER
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public void borrarAlquilerLogico(Integer id) {

        Alquiler alquiler = alquilerRepository
                .buscarPorIdConBloqueo(id)
                .filter(a -> !a.isBorradoAlquiler())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "El alquiler con ID " + id + " no existe."
                        )
                );

        if (alquiler.getEstado() == CodigoEstadoAlquiler.INICIADO) {
            throw new IllegalStateException(
                    "No se puede eliminar un alquiler en curso. "
                            + "Primero debe finalizarse indicando "
                            + "una estación de devolución."
            );
        }

        EstadoAlquiler estadoContratacion =
                alquiler.getEstadoContratacion();

        if (estadoContratacion != null) {

            String codigo = estadoContratacion.getCodigo();

            boolean contratacionCerrada =
                    "FINALIZADO".equals(codigo)
                            || "CANCELADO".equals(codigo)
                            || "VENCIDO".equals(codigo);

            if (!contratacionCerrada) {
                throw new IllegalStateException(
                        "No se puede eliminar un alquiler "
                                + "que todavía no está cerrado."
                );
            }

        } else if (alquiler.getEstado() == null) {

            throw new ConfiguracionSistemaException(
                    "El alquiler no tiene un estado configurado."
            );
        }

        // Solo cambia la visibilidad; no cancela ni liquida.
        alquiler.setBorradoAlquiler(true);
        alquilerRepository.save(alquiler);
    }

    /*
     * FINALIZAR UN ALQUILER
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public AlquilerDTO finalizarAlquiler(
            Integer id,
            String nombreEstacionDestino
    ) throws Exception {

        // 1. Obtener el usuario autenticado
        Usuario usuarioAutenticado = obtenerUsuarioAutenticado();

        // 2. Buscar el alquiler
        Alquiler alquiler = alquilerRepository.buscarPorIdConBloqueo(id)
                .filter(a -> !a.isBorradoAlquiler())
                .orElseThrow(() ->
                        new Exception("El alquiler no existe.")
                );

        // 3. Prevenir IDOR
        boolean esAdmin = usuarioActualEsAdmin();

        boolean esPropietario =
                alquiler.getUsuario().getId()
                        == usuarioAutenticado.getId();

        if (!esAdmin && !esPropietario) {
            throw new AccessDeniedException(
                    "No tiene permiso para finalizar este alquiler."
            );
        }

        // 4. Comprobar el estado
        if (alquiler.getEstado() != CodigoEstadoAlquiler.INICIADO) {
            throw new IllegalStateException(
                    "El alquiler ya fue finalizado o cancelado."
            );
        }

        // 5. Validar el nombre de la estación
        if (nombreEstacionDestino == null
                || nombreEstacionDestino.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Debe especificar una estación de destino."
            );
        }

        // 6. Buscar la estación de destino

        // Buscar y bloquear la estación durante esta devolución
        Estacion nuevaEstacion = estacionRepository
                .buscarActivaPorNombreConBloqueo(
                        nombreEstacionDestino.trim()
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La estación de destino no existe."
                        )
                );

        if (!nuevaEstacion.isEsPublica()) {
            throw new IllegalArgumentException(
                    "La devolución debe realizarse en una estación pública habilitada."
            );
        }

        // Contar todas las bicicletas físicamente presentes, incluso archivadas.
        long espaciosOcupados = bicicletaRepository
                .countByUbicacionActual(nuevaEstacion);

        // Impedir que la devolución supere la capacidad
        if (espaciosOcupados >= nuevaEstacion.getCapacidad()) {
            throw new IllegalStateException(
                    "La estación seleccionada no tiene espacios libres."
            );
        }

        BigDecimal precioPorHora = alquiler.getPrecioPorHoraAplicado();
        if (precioPorHora == null || precioPorHora.signum() <= 0) {
            // No reconstruir contratos antiguos con una tarifa actual diferente.
            throw new ConfiguracionSistemaException("El alquiler no tiene una tarifa contratada válida.");
        }
        EstadoBicicleta disponible = estadoBicicleta("DISPONIBLE");

        // 7. Registrar la hora real de devolución
        LocalDateTime fechaDevolucion = LocalDateTime.now();

        alquiler.setFechaFin(fechaDevolucion);
        alquiler.setEstado(CodigoEstadoAlquiler.FINALIZADO);
        alquiler.setUbicacionDevolucion(nuevaEstacion);

        // 8. Calcular duración real
        long segundosReales = ChronoUnit.SECONDS.between(
                alquiler.getFechaInicio(),
                fechaDevolucion
        );

        if (segundosReales < 0) {
            segundosReales = 0;
        }

        // 9. Calcular costo final en USD
        BigDecimal precioPorSegundo = precioPorHora.divide(
                SEGUNDOS_EN_HORA,
                10,
                RoundingMode.HALF_UP
        );

        BigDecimal costoFinal = precioPorSegundo
                .multiply(BigDecimal.valueOf(segundosReales))
                .setScale(2, RoundingMode.HALF_UP);

        alquiler.setCostoTotal(costoFinal);

        // 10. Obtener y congelar costo en ARS
        BigDecimal tasaCambio = obtenerCotizacionDolar();

        BigDecimal costoArs = costoFinal
                .multiply(tasaCambio)
                .setScale(0, RoundingMode.HALF_UP);

        alquiler.setCostoTotalArs(costoArs);

        // 11. Liberar y trasladar la bicicleta
        Bicicleta bicicleta = alquiler.getBicicleta();

        EstadoBicicleta estadoAnterior = bicicleta.getEstado();
        bicicleta.setEstado(disponible);
        bicicleta.setUbicacionActual(nuevaEstacion);

        bicicletaRepository.save(bicicleta);
        trazabilidadInventario.estado(bicicleta, estadoAnterior, usuarioAutenticado,
                "Devolución de alquiler.");
        // Tener ADMIN además de CLIENTE no convierte su propio viaje en una acción administrativa.
        if (esAdmin && !esPropietario) {
            trazabilidadInventario.administracion(usuarioAutenticado, "BICICLETA", bicicleta.getId(),
                    "FINALIZAR_ALQUILER", "estado=" + estadoAnterior.getCodigo(),
                    trazabilidadInventario.datos(bicicleta), "Devolución de alquiler.");
        }

        // 12. Guardar alquiler finalizado
        Alquiler alquilerFinalizado = alquilerRepository.save(alquiler);
        correoService.enviarComprobanteFinalizacion(alquilerFinalizado);
        return new AlquilerDTO(alquilerFinalizado);
    }

    /*
     * OBTENER COTIZACIÓN DEL DÓLAR
     */
    public BigDecimal obtenerCotizacionDolar() {
        return cotizacionDolarService.obtenerCotizacion().getCotizacionArsPorUsd();
    }
}
