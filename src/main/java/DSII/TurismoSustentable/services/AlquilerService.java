package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AlquilerDTO;
import DSII.TurismoSustentable.dto.AlquilerRequestDTO;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.AlquilerRepository;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.repositories.TipoBicicletaRepository;
import DSII.TurismoSustentable.repositories.EstadoBicicletaRepository;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.client.RestTemplate;
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
    private CotizacionDolarService cotizacionDolarService;

    @Autowired
    private CalculadoraAlquiler calculadoraAlquiler;

    @Autowired
    private Validator validator;

    @Autowired
    private Clock reloj;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${alquiler.max-bicicletas-por-contratacion:10}")
    private int maxBicicletasPorContratacion;

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
                            "Reserva creada."
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

    // CREAR UN ALQUILER
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public AlquilerDTO crearAlquiler(
            AlquilerRequestDTO alquilerRequest
    ) throws Exception {

        // 1. Obtener el usuario de la sesión
        Usuario usuario = obtenerUsuarioAutenticado();

        boolean clienteVigente = usuario.getUsuarioRoles().stream()
                .map(UsuarioRol::getRol)
                .anyMatch(rol -> "CLIENTE".equals(rol.getCodigo()));

        if (!clienteVigente) {
            throw new AccessDeniedException(
                    "La cuenta debe tener rol CLIENTE para contratar."
            );
        }

        // 2. Buscar y bloquear la bicicleta durante el alquiler
        Bicicleta bicicleta = bicicletaRepository
                .buscarActivaPorIdConBloqueo(
                        alquilerRequest.getBicicletaId()
                )
                .orElseThrow(() ->
                        new Exception("Bicicleta no encontrada.")
                );

        // 3. Verificar disponibilidad
        if (!"DISPONIBLE".equals(bicicleta.getEstado().getCodigo())) {
            throw new IllegalStateException(
                    "La bicicleta no está disponible."
            );
        }

        // Guardamos la estación antes de sacar la bicicleta
        Estacion estacionRetiro = bicicleta.getUbicacionActual();

        if (estacionRetiro == null) {
            throw new IllegalStateException(
                    "La bicicleta no tiene una estación de retiro asignada."
            );
        }

        if (estacionRetiro.isBorradoEstacion()
                || !estacionRetiro.isEsPublica()) {

            throw new IllegalStateException(
                    "La bicicleta no está ubicada en una estación pública " +
                            "habilitada para alquilar."
            );
        }

        // 4. Verificar duración
        if (alquilerRequest.getDuracionEnHoras() <= 0) {
            throw new IllegalArgumentException(
                    "La duración debe ser mayor que cero."
            );
        }

        // También se valida en el backend el máximo mostrado en el frontend
        if (alquilerRequest.getDuracionEnHoras() > 24) {
            throw new IllegalArgumentException(
                    "La duración máxima del alquiler es de 24 horas."
            );
        }

        // 5. Calcular fechas
        LocalDateTime fechaInicio = LocalDateTime.now();

        LocalDateTime fechaFinEstimada = fechaInicio.plusHours(
                alquilerRequest.getDuracionEnHoras()
        );

        // 6. Calcular precio estimado
        // Mismo bloqueo que la edición de tarifa: la contratación toma un valor consistente.
        TipoBicicleta tipoContratado = tipoBicicletaRepository
                .buscarPorIdConBloqueo(bicicleta.getTipo().getId())
                .orElseThrow(() -> new ConfiguracionSistemaException("Falta el tipo de bicicleta."));
        BigDecimal precioPorHora = tipoContratado.getPrecioPorHoraUsd();
        if (precioPorHora == null || precioPorHora.signum() <= 0) {
            throw new ConfiguracionSistemaException("El tipo de bicicleta no tiene una tarifa válida.");
        }
        EstadoBicicleta enUso = estadoBicicleta("EN_USO");

        BigDecimal duracion = BigDecimal.valueOf(
                alquilerRequest.getDuracionEnHoras()
        );

        BigDecimal costoEstimado = precioPorHora
                .multiply(duracion)
                .setScale(2, RoundingMode.HALF_UP);

        // 7. Crear alquiler
        Alquiler nuevoAlquiler = new Alquiler(
                usuario,
                bicicleta,
                fechaInicio,
                fechaFinEstimada,
                costoEstimado,
                CodigoEstadoAlquiler.INICIADO,
                estacionRetiro,
                null
        );

        // se guardan tanto la tarifa congelada como el costo estimado inicial
        nuevoAlquiler.setPrecioPorHoraAplicado(precioPorHora);
        nuevoAlquiler.setCostoTotal(costoEstimado);

        // 8. Cambiar estado y sacar físicamente la bicicleta de la estación
        EstadoBicicleta estadoAnterior = bicicleta.getEstado();
        bicicleta.setEstado(enUso);
        bicicleta.setUbicacionActual(null);
        bicicletaRepository.save(bicicleta);
        trazabilidadInventario.estado(bicicleta, estadoAnterior, usuario, "Inicio de alquiler.");

        // 9. Guardar alquiler
        Alquiler alquilerGuardado = alquilerRepository.save(nuevoAlquiler);
        correoService.enviarComprobanteInicio(alquilerGuardado);
        return new AlquilerDTO(alquilerGuardado);
    }


    // BORRAR LÓGICAMENTE UN ALQUILER
    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public void borrarAlquilerLogico(Integer id) throws Exception {

        Alquiler alquiler = alquilerRepository
                .buscarPorIdConBloqueo(id)
                .filter(a -> !a.isBorradoAlquiler())
                .orElseThrow(() ->
                        new Exception(
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
                        "No se puede eliminar una contratación "
                                + "que todavía no está cerrada."
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

        try {
            RestTemplate restTemplate = new RestTemplate();

            String url =
                    "https://dolarapi.com/v1/dolares/tarjeta";

            Map<?, ?> response =
                    restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("venta") != null) {
                return new BigDecimal(
                        response.get("venta").toString()
                );
            }

        } catch (Exception e) {
            System.err.println(
                    "Error al obtener la cotización: "
                            + e.getMessage()
            );
        }

        // Valor de respaldo
        return new BigDecimal("1400");
    }
}
