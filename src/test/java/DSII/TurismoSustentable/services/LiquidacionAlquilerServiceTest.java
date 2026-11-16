package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.LiquidacionAlquilerDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiquidacionAlquilerServiceTest {

    private static final String EMAIL = "qa.liquidacion@example.com";

    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 16, 12, 0, 0, 123456000);

    private static final LocalDateTime CIERRE = AHORA.minusMinutes(5);
    private static final LocalDateTime RETIRO = CIERRE.minusHours(3);

    @Mock private AlquilerRepository alquilerRepository;
    @Mock private AlquilerDetalleRepository detalleRepository;
    @Mock private IncidenciaRepository incidenciaRepository;
    @Mock private PagoRepository pagoRepository;
    @Mock private ReembolsoRepository reembolsoRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private LiquidacionAlquilerService servicio;
    private Usuario usuario;
    private Alquiler alquiler;
    private AlquilerDetalle primero;
    private AlquilerDetalle segundo;
    private Estacion estacion;

    @BeforeEach
    void preparar() {
        servicio = crearServicio(AHORA);

        usuario = new Usuario();
        usuario.setId(1);
        usuario.setNickname("qa.liquidacion");
        usuario.setEmail(EMAIL);
        usuario.setActivo(true);
        agregarRol(usuario, "CLIENTE");
        autenticar("CLIENTE");

        estacion = new Estacion(
                "Destino QA", "Calle QA", 5, 0.0, 0.0
        );
        estacion.setEsPublica(true);
        ReflectionTestUtils.setField(estacion, "id", 9);

        alquiler = new Alquiler();
        alquiler.setId(7);
        alquiler.setUsuario(usuario);
        alquiler.setUbicacionRetiro(estacion);
        alquiler.setEstadoContratacion(new EstadoAlquiler("FINALIZADO"));
        alquiler.setFechaHoraCreacion(RETIRO.minusMinutes(10));
        alquiler.setFechaHoraVencimientoReserva(RETIRO.plusMinutes(5));
        alquiler.setFechaHoraCierre(CIERRE);
        alquiler.setCotizacionArsPorUsd(dinero("1000.00"));
        alquiler.setCostoEstimadoUsd(dinero("4.00"));
        alquiler.setCostoEstimadoArs(dinero("4000.00"));
        ReflectionTestUtils.setField(alquiler, "version", 0);

        primero = detalleDevuelto(11, 45, "0.75", "750.00");
        segundo = detalleDevuelto(12, 180, "3.00", "3000.00");
    }

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void liquidaSumandoSubtotalesGuardadosSinMoverDinero() {
        prepararLiquidacion();

        assertTrue(servicio.liquidarSiCorresponde(alquiler));

        assertEquals(dinero("3.75"), alquiler.getCostoFinalUsd());
        assertEquals(dinero("3750.00"), alquiler.getCostoFinalArs());
        assertEquals(AHORA, alquiler.getFechaHoraLiquidacion());
        assertEquals("FINALIZADO", alquiler.getEstadoContratacion().getCodigo());

        // La tarifa vigente es 99; la aplicada al viaje sigue siendo 1.
        assertEquals(
                dinero("99.00"),
                primero.getBicicleta().getTipo().getPrecioPorHoraUsd()
        );
        assertEquals(dinero("1.00"), primero.getPrecioPorHoraAplicadoUsd());

        verify(alquilerRepository).flush();
        verifyNoInteractions(pagoRepository, reembolsoRepository);
    }

    @Test
    void sumaCentavosPorLineaSinReconvertirElTotalUsd() {
        prepararLiquidacion();
        alquiler.setCotizacionArsPorUsd(dinero("10.00"));

        LocalDateTime fin = RETIRO.plusSeconds(180);
        alquiler.setFechaHoraCierre(fin);

        for (AlquilerDetalle detalle : List.of(primero, segundo)) {
            detalle.setFechaHoraDevolucionReal(fin);
            detalle.setFechaHoraFinUso(fin);
            detalle.setFechaHoraCierreDetalle(fin);
        }

        primero.setPrecioPorHoraAplicadoUsd(dinero("2.01"));
        primero.setSubtotalRealUsd(dinero("0.10"));
        primero.setSubtotalRealArs(dinero("1.01"));

        segundo.setPrecioPorHoraAplicadoUsd(dinero("4.01"));
        segundo.setSubtotalRealUsd(dinero("0.20"));
        segundo.setSubtotalRealArs(dinero("2.01"));

        assertTrue(servicio.liquidarSiCorresponde(alquiler));

        assertEquals(dinero("0.30"), alquiler.getCostoFinalUsd());
        assertEquals(dinero("3.02"), alquiler.getCostoFinalArs());
    }

    @Test
    void repetirConservaFechaImportesYNoVuelveAEscribir() {
        prepararLiquidacion();
        assertTrue(servicio.liquidarSiCorresponde(alquiler));

        servicio = crearServicio(AHORA.plusHours(1));
        assertTrue(servicio.liquidarSiCorresponde(alquiler));

        assertEquals(AHORA, alquiler.getFechaHoraLiquidacion());
        assertEquals(dinero("3.75"), alquiler.getCostoFinalUsd());
        assertEquals(dinero("3750.00"), alquiler.getCostoFinalArs());
        verify(alquilerRepository, times(1)).flush();
    }

    @ParameterizedTest
    @ValueSource(strings = {"RESERVADO", "PENDIENTE_RETIRO", "EN_CURSO"})
    void noLiquidaMientrasHayUsoORetiroPendiente(String estado) {
        prepararLiquidacion();

        alquiler.setEstadoContratacion(new EstadoAlquiler("EN_PROGRESO"));
        alquiler.setFechaHoraCierre(null);

        primero.setEstado(new EstadoAlquilerDetalle(estado));
        primero.setFechaHoraDevolucionReal(null);
        primero.setEstacionDevolucion(null);
        primero.setFechaHoraFinUso(null);
        primero.setFechaHoraCierreDetalle(null);
        primero.setSubtotalRealUsd(null);
        primero.setSubtotalRealArs(null);

        if (!"EN_CURSO".equals(estado)) {
            primero.setFechaHoraRetiro(null);
        }

        assertFalse(servicio.liquidarSiCorresponde(alquiler));
        verificarSinLiquidacion();
    }

    @Test
    void revisionEconomicaPendienteDifiereSinPerderLaDevolucion() {
        prepararLiquidacion();
        Incidencia incidencia = incidencia(primero, true);

        when(incidenciaRepository.findByAlquilerDetalleIdOrderByIdAsc(11))
                .thenReturn(List.of(incidencia));

        LocalDateTime recepcion = primero.getFechaHoraDevolucionReal();

        assertFalse(servicio.liquidarSiCorresponde(alquiler));

        assertEquals("DEVUELTO", primero.getEstado().getCodigo());
        assertEquals(recepcion, primero.getFechaHoraDevolucionReal());
        assertEquals("PENDIENTE", incidencia.getEstadoRevision());
        verificarSinLiquidacion();
    }

    @Test
    void revisionTecnicaNoImpideLiquidarElUsoDefinitivo() {
        prepararLiquidacion();

        primero.setOrdenMantenimiento(new OrdenMantenimiento(
                primero.getBicicleta(),
                new TipoMantenimiento("CORRECTIVO", "Revisión QA"),
                new EstadoOrdenMantenimiento("CREADA"),
                primero.getFechaHoraDevolucionReal()
        ));

        when(incidenciaRepository.findByAlquilerDetalleIdOrderByIdAsc(11))
                .thenReturn(List.of(incidencia(primero, false)));

        assertTrue(servicio.liquidarSiCorresponde(alquiler));
        assertEquals(dinero("3750.00"), alquiler.getCostoFinalArs());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELADO", "VENCIDO"})
    void sinRetirosLiquidaCostoCeroSinInventarPagos(String estado) {
        prepararLiquidacion();
        alquiler.setEstadoContratacion(new EstadoAlquiler(estado));

        dejarSinRetiro(primero, estado);
        dejarSinRetiro(segundo, estado);

        assertTrue(servicio.liquidarSiCorresponde(alquiler));

        assertEquals(dinero("0.00"), alquiler.getCostoFinalUsd());
        assertEquals(dinero("0.00"), alquiler.getCostoFinalArs());
        assertEquals(AHORA, alquiler.getFechaHoraLiquidacion());
        verifyNoInteractions(pagoRepository, reembolsoRepository);
    }

    @Test
    void cierrePorIncidenciaConservaElCorteValidado() {
        prepararLiquidacion();

        LocalDateTime corte = primero.getFechaHoraFinUso();
        Incidencia incidencia = incidencia(primero, true);
        incidencia.setEstadoRevision("VALIDADA");
        incidencia.setFechaHoraCorteValidado(corte);
        ReflectionTestUtils.setField(
                incidencia, "fechaHoraRegistro", corte.plusMinutes(1)
        );
        incidencia.setFechaHoraDecision(corte.plusMinutes(2));
        incidencia.setMotivoDecision("Corte validado por ADMIN.");

        Usuario administrador = new Usuario();
        administrador.setId(2);
        administrador.setActivo(true);
        agregarRol(administrador, "ADMIN");
        incidencia.setResueltoPor(administrador);

        primero.setEstado(new EstadoAlquilerDetalle("CERRADO_POR_INCIDENCIA"));
        primero.setIncidenciaCorte(incidencia);
        primero.setFechaHoraDevolucionReal(null);
        primero.setEstacionDevolucion(null);
        primero.setFechaHoraCierreDetalle(corte.plusMinutes(2));

        when(incidenciaRepository.findByAlquilerDetalleIdOrderByIdAsc(11))
                .thenReturn(List.of(incidencia));

        assertTrue(servicio.liquidarSiCorresponde(alquiler));

        assertEquals(corte, primero.getFechaHoraFinUso());
        assertNull(primero.getFechaHoraDevolucionReal());
        assertEquals(dinero("3750.00"), alquiler.getCostoFinalArs());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "IMPORTE_NULO", "NEGATIVO", "DECIMALES", "FECHA_FUTURA"
    })
    void rechazaDatosInconsistentesSinGuardar(String caso) {
        prepararLiquidacion();

        switch (caso) {
            case "IMPORTE_NULO":
                primero.setSubtotalRealArs(null);
                break;
            case "NEGATIVO":
                primero.setSubtotalRealArs(dinero("-1.00"));
                break;
            case "DECIMALES":
                primero.setSubtotalRealArs(dinero("750.001"));
                break;
            default:
                primero.setFechaHoraCierreDetalle(AHORA.plusSeconds(1));
        }

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> servicio.liquidarSiCorresponde(alquiler)
        );
        verificarSinLiquidacion();
    }

    @Test
    void rechazaDetallesDuplicados() {
        when(detalleRepository.buscarPorAlquiler(7))
                .thenReturn(List.of(primero, primero));

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> servicio.liquidarSiCorresponde(alquiler)
        );
        verificarSinLiquidacion();
    }

    @Test
    void noSobrescribeUnaLiquidacionSiDespuesCambianLosDetalles() {
        prepararLiquidacion();
        assertTrue(servicio.liquidarSiCorresponde(alquiler));

        primero.setSubtotalRealArs(dinero("750.01"));

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> servicio.liquidarSiCorresponde(alquiler)
        );

        assertEquals(dinero("3750.00"), alquiler.getCostoFinalArs());
        assertEquals(AHORA, alquiler.getFechaHoraLiquidacion());
        verify(alquilerRepository, times(1)).flush();
    }

    @Test
    void consultaSinLiquidarNoInventaCostosNiDiferenciasCero() {
        prepararConsulta();
        when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(pago(21, "APROBADO", "4000.00")));

        LiquidacionAlquilerDTO resultado = servicio.consultarLiquidacion(7);

        assertEquals("SIN_LIQUIDAR", resultado.getSituacionFinanciera());
        assertEquals(dinero("4000.00"), resultado.getNetoConfirmadoArs());
        assertNull(resultado.getFechaHoraLiquidacion());
        assertNull(resultado.getCostoFinalUsd());
        assertNull(resultado.getCostoFinalArs());
        assertNull(resultado.getSaldoPendienteArs());
        assertNull(resultado.getReembolsoPendienteArs());
        verify(alquilerRepository, never()).flush();
    }

    @ParameterizedTest
    @CsvSource({
            "5000.00,SALDO_PENDIENTE,1000.00,0.00",
            "4000.00,SALDADO,0.00,0.00",
            "3500.00,REEMBOLSO_PENDIENTE,0.00,500.00"
    })
    void consultaDistingueLosTresResultadosFinancieros(
            String costo,
            String situacion,
            String saldo,
            String reembolso
    ) {
        prepararConsulta();
        fijarLiquidacion(costo);
        when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(pago(21, "APROBADO", "4000.00")));

        LiquidacionAlquilerDTO resultado = servicio.consultarLiquidacion(7);

        assertEquals(situacion, resultado.getSituacionFinanciera());
        assertEquals(dinero(costo), resultado.getCostoFinalArs());
        assertEquals(dinero(saldo), resultado.getSaldoPendienteArs());
        assertEquals(dinero(reembolso), resultado.getReembolsoPendienteArs());
        assertEquals("ARS", resultado.getMoneda());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREADO", "PENDIENTE", "RECHAZADO", "CANCELADO"})
    void pagosNoAprobadosNoCubrenElSaldo(String estado) {
        prepararConsulta();
        fijarLiquidacion("3750.00");
        when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(pago(21, estado, "4000.00")));

        LiquidacionAlquilerDTO resultado = servicio.consultarLiquidacion(7);

        assertEquals(dinero("0.00"), resultado.getPagosAprobadosArs());
        assertEquals(dinero("3750.00"), resultado.getSaldoPendienteArs());
        assertEquals("SALDO_PENDIENTE", resultado.getSituacionFinanciera());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDIENTE", "EN_PROCESO", "FALLIDO"})
    void reembolsosNoConfirmadosNoSeDescuentanDelNeto(String estado) {
        prepararConsulta();
        fijarLiquidacion("3750.00");
        Pago aprobado = pago(21, "APROBADO", "4000.00");

        when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(aprobado));
        when(reembolsoRepository.findByPagoAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(reembolso(aprobado, 31, estado, "250.00")));

        LiquidacionAlquilerDTO resultado = servicio.consultarLiquidacion(7);

        assertEquals(dinero("4000.00"), resultado.getNetoConfirmadoArs());
        assertEquals(dinero("0.00"), resultado.getReembolsosConfirmadosArs());
        assertEquals(dinero("250.00"), resultado.getReembolsoPendienteArs());
    }

    @Test
    void reembolsoConfirmadoActualizaSituacionSinRehacerLaLiquidacion() {
        prepararConsulta();
        fijarLiquidacion("3750.00");
        LocalDateTime fechaOriginal = alquiler.getFechaHoraLiquidacion();
        Pago aprobado = pago(21, "APROBADO", "4000.00");

        when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(aprobado));

        assertEquals(
                "REEMBOLSO_PENDIENTE",
                servicio.consultarLiquidacion(7).getSituacionFinanciera()
        );

        when(reembolsoRepository.findByPagoAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(
                        reembolso(aprobado, 31, "CONFIRMADO", "250.00")
                ));

        LiquidacionAlquilerDTO resultado = servicio.consultarLiquidacion(7);

        assertEquals("SALDADO", resultado.getSituacionFinanciera());
        assertEquals(dinero("3750.00"), resultado.getNetoConfirmadoArs());
        assertEquals(dinero("0.00"), resultado.getReembolsoPendienteArs());
        assertEquals(fechaOriginal, resultado.getFechaHoraLiquidacion());

        verify(alquilerRepository, never()).flush();
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(reembolsoRepository, never()).save(any(Reembolso.class));
    }

    @Test
    void rechazaReembolsosConfirmadosQueSuperanElPagoOriginal() {
        prepararConsulta();
        fijarLiquidacion("3750.00");
        Pago aprobado = pago(21, "APROBADO", "4000.00");

        when(pagoRepository.findByAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(aprobado));
        when(reembolsoRepository.findByPagoAlquilerIdOrderByIdAsc(7))
                .thenReturn(List.of(
                        reembolso(aprobado, 31, "CONFIRMADO", "2500.00"),
                        reembolso(aprobado, 32, "CONFIRMADO", "2000.00")
                ));

        assertThrows(
                ConfiguracionSistemaException.class,
                () -> servicio.consultarLiquidacion(7)
        );
    }

    @Test
    void clienteNoConsultaUnAlquilerAjeno() {
        prepararConsulta();
        cambiarTitular();

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.consultarLiquidacion(7)
        );
        verifyNoInteractions(pagoRepository, reembolsoRepository);
    }

    @Test
    void administradorVigentePuedeConsultarUnAlquilerAjeno() {
        usuario.getUsuarioRoles().clear();
        agregarRol(usuario, "ADMIN");
        autenticar("ADMIN");
        prepararConsulta();
        cambiarTitular();

        assertEquals(7, servicio.consultarLiquidacion(7).getAlquilerId());
    }

    @Test
    void adminRevocadoNoEludePropiedadAunqueConserveCliente() {
        autenticar("ADMIN", "CLIENTE");
        prepararConsulta();
        cambiarTitular();

        // La sesión conserva ADMIN, pero en BD solo existe CLIENTE.
        assertThrows(
                AccessDeniedException.class,
                () -> servicio.consultarLiquidacion(7)
        );
        verifyNoInteractions(pagoRepository, reembolsoRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SIN_SESION", "INACTIVA", "TECNICO", "ADMIN_REVOCADO"
    })
    void consultaExigeSesionCuentaActivaYRolVigente(String caso) {
        if ("SIN_SESION".equals(caso)) {
            SecurityContextHolder.clearContext();
        } else {
            when(usuarioRepository.buscarPorEmailConDatosYRoles(EMAIL))
                    .thenReturn(Optional.of(usuario));

            if ("INACTIVA".equals(caso)) {
                usuario.setActivo(false);
            } else if ("TECNICO".equals(caso)) {
                usuario.getUsuarioRoles().clear();
                agregarRol(usuario, "TECNICO");
                autenticar("TECNICO");
            } else {
                autenticar("ADMIN");
            }
        }

        assertThrows(
                AccessDeniedException.class,
                () -> servicio.consultarLiquidacion(7)
        );
        verifyNoInteractions(
                alquilerRepository, pagoRepository, reembolsoRepository
        );
    }

    private LiquidacionAlquilerService crearServicio(LocalDateTime instante) {
        return new LiquidacionAlquilerService(
                alquilerRepository,
                detalleRepository,
                incidenciaRepository,
                pagoRepository,
                reembolsoRepository,
                usuarioRepository,
                Clock.fixed(instante.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    private void prepararLiquidacion() {
        when(detalleRepository.buscarPorAlquiler(7))
                .thenReturn(List.of(primero, segundo));
    }

    private void prepararConsulta() {
        when(usuarioRepository.buscarPorEmailConDatosYRoles(EMAIL))
                .thenReturn(Optional.of(usuario));
        when(alquilerRepository.findById(7))
                .thenReturn(Optional.of(alquiler));
    }

    private AlquilerDetalle detalleDevuelto(
            int id, int minutos, String usd, String ars
    ) {
        TipoBicicleta tipo = new TipoBicicleta(
                "PASEO", dinero("99.00"), 3
        );
        Bicicleta bicicleta = new Bicicleta(
                "QA-" + id, tipo, new EstadoBicicleta("DISPONIBLE"), estacion
        );
        bicicleta.setId(100 + id);
        bicicleta.setFechaAlta(RETIRO.toLocalDate());

        LocalDateTime fin = RETIRO.plusMinutes(minutos);
        AlquilerDetalle detalle = new AlquilerDetalle();
        ReflectionTestUtils.setField(detalle, "id", id);
        ReflectionTestUtils.setField(detalle, "version", 0);
        detalle.setAlquiler(alquiler);
        detalle.setBicicleta(bicicleta);
        detalle.setTipoBicicletaContratado(tipo);
        detalle.setEstado(new EstadoAlquilerDetalle("DEVUELTO"));
        detalle.setPrecioPorHoraAplicadoUsd(dinero("1.00"));
        detalle.setDuracionEstimadaSegundos(7200);
        detalle.setSubtotalEstimadoUsd(dinero("2.00"));
        detalle.setSubtotalEstimadoArs(dinero("2000.00"));
        detalle.setFechaHoraRetiro(RETIRO);
        detalle.setFechaHoraDevolucionEstimada(RETIRO.plusHours(2));
        detalle.setFechaHoraDevolucionReal(fin);
        detalle.setFechaHoraFinUso(fin);
        detalle.setFechaHoraCierreDetalle(fin);
        detalle.setEstacionDevolucion(estacion);
        detalle.setSubtotalRealUsd(dinero(usd));
        detalle.setSubtotalRealArs(dinero(ars));
        return detalle;
    }

    private void dejarSinRetiro(AlquilerDetalle detalle, String estado) {
        detalle.setEstado(new EstadoAlquilerDetalle(estado));
        detalle.setFechaHoraRetiro(null);
        detalle.setFechaHoraDevolucionReal(null);
        detalle.setFechaHoraFinUso(null);
        detalle.setFechaHoraCierreDetalle(CIERRE);
        detalle.setEstacionDevolucion(null);
        detalle.setSubtotalRealUsd(dinero("0.00"));
        detalle.setSubtotalRealArs(dinero("0.00"));
    }

    private Incidencia incidencia(
            AlquilerDetalle detalle, boolean revisionEconomica
    ) {
        Incidencia incidencia = new Incidencia();
        ReflectionTestUtils.setField(incidencia, "id", 71);
        incidencia.setAlquilerDetalle(detalle);
        incidencia.setRegistradoPor(usuario);
        incidencia.setTipo("AVERIA");
        incidencia.setDescripcion("Incidencia QA.");
        incidencia.setRequiereRevisionEconomica(revisionEconomica);
        incidencia.setFechaHoraHechoInformada(RETIRO.plusMinutes(15));
        ReflectionTestUtils.setField(
                incidencia, "fechaHoraRegistro", RETIRO.plusMinutes(16)
        );
        return incidencia;
    }

    private void fijarLiquidacion(String costoArs) {
        alquiler.setCostoFinalArs(dinero(costoArs));
        alquiler.setCostoFinalUsd(
                dinero(costoArs).divide(dinero("1000.00")).setScale(2)
        );
        alquiler.setFechaHoraLiquidacion(AHORA.minusMinutes(3));
    }

    private Pago pago(int id, String estado, String monto) {
        Pago pago = new Pago(
                alquiler,
                new EstadoPago("CREADO"),
                "ANTICIPO",
                dinero(monto),
                RETIRO.minusMinutes(5),
                "SIMULADA",
                "PRUEBA",
                "QA-PAGO-" + id
        );
        ReflectionTestUtils.setField(pago, "id", id);
        pago.setEstadoPago(new EstadoPago(estado));
        if ("APROBADO".equals(estado)) {
            pago.setFechaConfirmacion(RETIRO.minusMinutes(1));
        }
        return pago;
    }

    private Reembolso reembolso(
            Pago pago, int id, String estado, String monto
    ) {
        Reembolso reembolso = new Reembolso(
                pago, dinero(monto), AHORA.minusMinutes(2),
                "Ajuste QA.", "QA-REEMBOLSO-" + id
        );
        ReflectionTestUtils.setField(reembolso, "id", id);
        reembolso.setEstado(estado);
        if ("CONFIRMADO".equals(estado)) {
            reembolso.setFechaConfirmacion(AHORA.minusMinutes(1));
        }
        return reembolso;
    }

    private void cambiarTitular() {
        Usuario otro = new Usuario();
        otro.setId(99);
        otro.setActivo(true);
        alquiler.setUsuario(otro);
    }

    private void agregarRol(Usuario cuenta, String codigo) {
        Rol rol = new Rol(codigo);
        int id = "ADMIN".equals(codigo) ? 2
                : "TECNICO".equals(codigo) ? 3 : 1;
        ReflectionTestUtils.setField(rol, "id", id);
        cuenta.getUsuarioRoles().add(new UsuarioRol(cuenta, rol));
    }

    private void autenticar(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        EMAIL, null, AuthorityUtils.createAuthorityList(roles)
                )
        );
    }

    private void verificarSinLiquidacion() {
        assertNull(alquiler.getCostoFinalUsd());
        assertNull(alquiler.getCostoFinalArs());
        assertNull(alquiler.getFechaHoraLiquidacion());
        verify(alquilerRepository, never()).flush();
        verifyNoInteractions(pagoRepository, reembolsoRepository);
    }

    private BigDecimal dinero(String valor) {
        return new BigDecimal(valor);
    }
}