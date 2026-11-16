package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.LiquidacionAlquilerDTO;
import DSII.TurismoSustentable.dto.ResultadoAdicionalDTO;
import DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoAdicionalServiceTest {

    private static final String EMAIL = "qa.adicional@example.com";
    private static final String CLAVE = "123e4567-e89b-42d3-a456-426614174000";
    private static final String CLAVE_DB = "ADICIONAL:1:" + CLAVE;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 8, 0);
    private static final LocalDateTime LIQUIDACION = AHORA.minusMinutes(10);

    @Mock private UsuarioRepository usuarios;
    @Mock private PagoRepository pagos;
    @Mock private ReembolsoRepository reembolsos;
    @Mock private PasarelaSimulada pasarela;
    @Mock private AlquilerRepository alquileres;
    @Mock private AlquilerDetalleRepository detalles;
    @Mock private EstadoPagoRepository estados;
    @Mock private HistorialEstadoPagoRepository historias;
    @Mock private EstadoAlquilerDetalleRepository estadosDetalle;
    @Mock private HistorialEstadoDetalleRepository historiasDetalle;
    @Mock private HistorialEstadoReembolsoRepository historiasReembolso;
    @Mock private LiquidacionAlquilerService liquidaciones;
    @Mock private ReembolsoService reintegros;

    private PagoService servicio;
    private Usuario usuario;
    private Alquiler alquiler;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.setId(1);
        usuario.setEmail(EMAIL);
        usuario.setNickname("qa.adicional");
        usuario.setActivo(true);
        Rol rol = new Rol("CLIENTE");
        ReflectionTestUtils.setField(rol, "id", 1);
        usuario.getUsuarioRoles().add(new UsuarioRol(usuario, rol));

        alquiler = new Alquiler();
        alquiler.setId(7);
        alquiler.setUsuario(usuario);
        alquiler.setEstadoContratacion(new EstadoAlquiler("FINALIZADO"));
        alquiler.setFechaHoraCierre(LIQUIDACION);
        alquiler.setFechaHoraLiquidacion(LIQUIDACION);
        alquiler.setCostoFinalUsd(new BigDecimal("2.00"));
        alquiler.setCostoFinalArs(new BigDecimal("300.00"));

        servicio = new PagoService(usuarios, pagos, reembolsos, pasarela, alquileres,
                detalles, estados, historias, estadosDetalle, historiasDetalle,
                historiasReembolso, liquidaciones, reintegros,
                Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        autenticar("CLIENTE");
    }

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void inicioUsaSaldoDelServidorInclusoSiElAlquilerEstaOculto(boolean oculto) {
        alquiler.setBorradoAlquiler(oculto);
        prepararInicio(List.of());
        catalogo("CREADO");
        when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            ReflectionTestUtils.setField(pago, "id", 11);
            return pago;
        });

        ResultadoAdicionalDTO resultado = servicio.iniciarAdicional(7, CLAVE);

        assertEquals(11, resultado.getPagoId());
        assertEquals("CREADO", resultado.getEstadoPago());
        assertEquals("ADICIONAL", resultado.getFinalidad());
        assertEquals("ARS", resultado.getMoneda());
        assertEquals(new BigDecimal("200.00"), resultado.getMontoArs());
        assertEquals("SALDO_PENDIENTE", resultado.getLiquidacion().getSituacionFinanciera());
        ArgumentCaptor<Pago> guardado = ArgumentCaptor.forClass(Pago.class);
        verify(pagos).saveAndFlush(guardado.capture());
        assertEquals(CLAVE_DB, guardado.getValue().getClaveIdempotencia());
        assertEquals("SIMULADA", guardado.getValue().getPlataforma());
        assertNull(guardado.getValue().getReferenciaExterna());
        assertNull(guardado.getValue().getFechaConfirmacion());
        ArgumentCaptor<HistorialEstadoPago> historia = ArgumentCaptor.forClass(HistorialEstadoPago.class);
        verify(historias).save(historia.capture());
        assertEquals("USUARIO", historia.getValue().getOrigen());
        assertSame(usuario, historia.getValue().getActorUsuario());
        assertEquals("SOLICITUD_ADICIONAL", historia.getValue().getFuente());
        verifyNoInteractions(pasarela, reembolsos, detalles, historiasDetalle, historiasReembolso);
        comprobarLiquidacionInmutable();
    }

    @Test
    void repetirInicioDevuelveElIntentoSinCrearOtroAunqueAhoraEsteSaldado() {
        prepararCabecera();
        Pago pago = intento("APROBADO");
        when(pagos.findByClaveIdempotencia(CLAVE_DB)).thenReturn(Optional.of(pago));
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen("300.00"));
        ResultadoAdicionalDTO resultado = servicio.iniciarAdicional(7, CLAVE.toUpperCase());
        assertEquals(11, resultado.getPagoId());
        assertEquals(CLAVE, resultado.getClaveSolicitud());
        assertEquals("SALDADO", resultado.getLiquidacion().getSituacionFinanciera());
        verify(pagos, never()).saveAndFlush(any());
        verifyNoInteractions(historias, pasarela);
    }

    @Test
    void sinLiquidacionNoSeCreaPago() {
        prepararCabecera();
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(new LiquidacionAlquilerDTO(
                7, 0, "SIN_LIQUIDAR", null, null, null, new BigDecimal("100.00"),
                BigDecimal.ZERO, new BigDecimal("100.00"), null, null));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarAdicional(7, CLAVE)).getStatus());
        verify(pagos, never()).saveAndFlush(any());
        verifyNoInteractions(historias, pasarela);
    }

    @ParameterizedTest
    @ValueSource(strings = {"300.00", "400.00"})
    void noCreaPagoCeroSiNoHayDeuda(String neto) {
        prepararCabecera();
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen(neto));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarAdicional(7, CLAVE)).getStatus());
        verify(pagos, never()).saveAndFlush(any());
        verifyNoInteractions(historias, pasarela);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREADO", "PENDIENTE"})
    void intentoSinResolverBloqueaOtraClaveInclusoSiEraAnticipo(String estado) {
        Pago anterior = new Pago(alquiler, new EstadoPago("CREADO"), "ANTICIPO",
                new BigDecimal("100.00"), LIQUIDACION.minusMinutes(20),
                "SIMULADA", "PRUEBA", "ANTICIPO:1:otra");
        anterior.setEstadoPago(new EstadoPago(estado));
        prepararInicio(List.of(anterior));
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarAdicional(7, CLAVE)).getStatus());
        verify(pagos, never()).saveAndFlush(any());
        verifyNoInteractions(historias, pasarela);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECHAZADO", "CANCELADO"})
    void resultadoNegativoAnteriorPermiteUnIntentoNuevo(String estado) {
        Pago anterior = intento(estado);
        ReflectionTestUtils.setField(anterior, "claveIdempotencia",
                "ADICIONAL:1:123e4567-e89b-42d3-a456-426614174001");
        prepararInicio(List.of(anterior));
        catalogo("CREADO");
        when(pagos.saveAndFlush(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            ReflectionTestUtils.setField(pago, "id", 12);
            return pago;
        });
        assertEquals(12, servicio.iniciarAdicional(7, CLAVE).getPagoId());
        verify(historias).save(any(HistorialEstadoPago.class));
        verifyNoInteractions(pasarela);
    }

    @Test
    void aprobacionReduceDeudaSinModificarLiquidacionNiInventarioYNoSeDuplica() {
        Pago pago = prepararResolucion("CREADO");
        when(liquidaciones.consultarLiquidacion(7))
                .thenReturn(resumen("100.00"), resumen("300.00"));
        prepararProveedor(pago, "APROBADO");
        catalogo("PENDIENTE", "APROBADO");

        ResultadoAdicionalDTO resultado = servicio.resolverAdicional(7, CLAVE);
        assertEquals("APROBADO", resultado.getEstadoPago());
        assertEquals(AHORA, resultado.getFechaConfirmacionPago());
        assertEquals("SALDADO", resultado.getLiquidacion().getSituacionFinanciera());
        assertEquals("SIM-PAGO-11", pago.getReferenciaExterna());
        ArgumentCaptor<HistorialEstadoPago> eventos = ArgumentCaptor.forClass(HistorialEstadoPago.class);
        verify(historias, times(2)).save(eventos.capture());
        assertEquals("PENDIENTE", eventos.getAllValues().get(0).getEstadoNuevo().getCodigo());
        assertEquals("APROBADO", eventos.getAllValues().get(1).getEstadoNuevo().getCodigo());
        for (HistorialEstadoPago evento : eventos.getAllValues()) {
            assertEquals("SISTEMA", evento.getOrigen());
            assertNull(evento.getActorUsuario());
        }
        assertEquals(resultado.getPagoId(), servicio.resolverAdicional(7, CLAVE).getPagoId());
        verify(pasarela, times(1)).evaluarPago(pago, new BigDecimal("1000.00"));
        verify(historias, times(2)).save(any());
        verifyNoInteractions(detalles, estadosDetalle, historiasDetalle, historiasReembolso);
        comprobarLiquidacionInmutable();
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECHAZADO", "CANCELADO", "PENDIENTE"})
    void resultadoSinCobroConservaLaDeuda(String resultadoProveedor) {
        Pago pago = prepararResolucion("CREADO");
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen("100.00"));
        prepararProveedor(pago, resultadoProveedor);
        catalogo("PENDIENTE");
        if (!"PENDIENTE".equals(resultadoProveedor)) catalogo(resultadoProveedor);
        ResultadoAdicionalDTO respuesta = servicio.resolverAdicional(7, CLAVE);
        assertEquals(resultadoProveedor, respuesta.getEstadoPago());
        assertNull(respuesta.getFechaConfirmacionPago());
        assertEquals(new BigDecimal("200.00"), respuesta.getLiquidacion().getSaldoPendienteArs());
        comprobarLiquidacionInmutable();
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROBADO", "RECHAZADO", "CANCELADO"})
    void resolverUnResultadoTerminalNoConsultaProveedorNiEscribe(String estado) {
        prepararResolucion(estado);
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen(
                "APROBADO".equals(estado) ? "300.00" : "100.00"));
        assertEquals(estado, servicio.resolverAdicional(7, CLAVE).getEstadoPago());
        verifyNoInteractions(pasarela, historias);
        verify(pagos, never()).flush();
    }

    @Test
    void intentoLocalNoEnviadoSeCancelaSiYaNoSeDebeEseImporte() {
        prepararResolucion("CREADO");
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen("300.00"));
        catalogo("CANCELADO");
        ResultadoAdicionalDTO resultado = servicio.resolverAdicional(7, CLAVE);
        assertEquals("CANCELADO", resultado.getEstadoPago());
        assertEquals(new BigDecimal("200.00"), resultado.getMontoArs());
        assertNull(resultado.getFechaConfirmacionPago());
        verifyNoInteractions(pasarela, reembolsos);
        verify(historias).save(any());
    }

    @Test
    void confirmacionYaEnGestionSeConciliaAunqueHayaQuedadoUnExcedente() {
        Pago pago = prepararResolucion("PENDIENTE");
        when(liquidaciones.consultarLiquidacion(7))
                .thenReturn(resumen("300.00"), resumen("500.00"));
        prepararProveedor(pago, "APROBADO");
        catalogo("APROBADO");
        ResultadoAdicionalDTO resultado = servicio.resolverAdicional(7, CLAVE);
        assertEquals("APROBADO", resultado.getEstadoPago());
        assertEquals("REEMBOLSO_PENDIENTE", resultado.getLiquidacion().getSituacionFinanciera());
        assertEquals(new BigDecimal("200.00"), resultado.getLiquidacion().getReembolsoPendienteArs());
        verify(historias).save(any());
        verify(reembolsos, never()).save(any());
        comprobarLiquidacionInmutable();
    }

    @Test
    void consultarPendienteOtraVezNoDuplicaSuHistoria() {
        Pago pago = prepararResolucion("PENDIENTE");
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen("100.00"));
        prepararProveedor(pago, "PENDIENTE");
        servicio.resolverAdicional(7, CLAVE);
        verifyNoInteractions(historias, estados);
        assertNull(pago.getFechaConfirmacion());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void idInvalidoSeRechazaAntesDeConsultarLaBase(Integer id) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarAdicional(id, CLAVE)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> servicio.resolverAdicional(id, CLAVE)).getStatus());
        verifyNoInteractions(usuarios, alquileres, pagos, liquidaciones);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "abc", "1-1-1-1-1"})
    void claveInvalidaSeRechazaSinEscribir(String clave) {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarAdicional(7, clave)).getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> servicio.resolverAdicional(7, clave)).getStatus());
        verifyNoInteractions(usuarios, alquileres, pagos, liquidaciones);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "TECNICO"})
    void rolSinClienteNoPuedePagar(String rol) {
        autenticar(rol);
        assertThrows(AccessDeniedException.class, () -> servicio.iniciarAdicional(7, CLAVE));
        assertThrows(AccessDeniedException.class, () -> servicio.resolverAdicional(7, CLAVE));
        verifyNoInteractions(usuarios, alquileres, pagos, liquidaciones);
    }

    @Test
    void sesionAusenteNoPuedePagar() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> servicio.iniciarAdicional(7, CLAVE));
        assertThrows(AccessDeniedException.class, () -> servicio.resolverAdicional(7, CLAVE));
        verifyNoInteractions(usuarios, alquileres, pagos, liquidaciones);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cuentaInactivaORolRevocadoNoPuedePagar(boolean inactiva) {
        if (inactiva) usuario.setActivo(false);
        else usuario.getUsuarioRoles().clear();
        when(usuarios.buscarActivoPorEmailConBloqueo(EMAIL)).thenReturn(Optional.of(usuario));
        assertThrows(AccessDeniedException.class, () -> servicio.iniciarAdicional(7, CLAVE));
        assertThrows(AccessDeniedException.class, () -> servicio.resolverAdicional(7, CLAVE));
        verifyNoInteractions(alquileres, pagos, liquidaciones);
    }

    @Test
    void alquilerAjenoNoSePuedePagarAunqueLaSesionTambienSeaAdmin() {
        autenticar("ADMIN", "CLIENTE");
        prepararCabecera();
        Usuario otro = new Usuario();
        otro.setId(2);
        alquiler.setUsuario(otro);
        assertThrows(AccessDeniedException.class, () -> servicio.iniciarAdicional(7, CLAVE));
        assertThrows(AccessDeniedException.class, () -> servicio.resolverAdicional(7, CLAVE));
        verifyNoInteractions(pagos, liquidaciones, pasarela);
    }

    @Test
    void alquilerInexistenteConserva404() {
        when(usuarios.buscarActivoPorEmailConBloqueo(EMAIL)).thenReturn(Optional.of(usuario));
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarAdicional(7, CLAVE)).getStatus());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> servicio.resolverAdicional(7, CLAVE)).getStatus());
        verifyNoInteractions(pagos, liquidaciones);
    }

    @Test
    void legadoConserva409SinInventarPagos() {
        prepararCabecera();
        alquiler.setEstadoContratacion(null);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> servicio.iniciarAdicional(7, CLAVE)).getStatus());
        verifyNoInteractions(pagos, liquidaciones, pasarela);
    }

    @Test
    void resolverClaveInexistenteConserva404() {
        prepararCabecera();
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> servicio.resolverAdicional(7, CLAVE)).getStatus());
        verifyNoInteractions(pasarela, historias, liquidaciones);
    }

    @ParameterizedTest
    @ValueSource(strings = {"moneda", "finalidad", "plataforma", "entorno", "alquiler"})
    void claveNoPuedeUsarseParaOtraOperacion(String campo) {
        Pago pago = prepararResolucion("CREADO");
        if ("alquiler".equals(campo)) {
            Alquiler otro = new Alquiler();
            otro.setId(8);
            ReflectionTestUtils.setField(pago, campo, otro);
        } else {
            ReflectionTestUtils.setField(pago, campo, "OTRO");
        }
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> servicio.resolverAdicional(7, CLAVE)).getStatus());
        verifyNoInteractions(pasarela, historias, liquidaciones);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-1.00", "0.001"})
    void importeInconsistenteEsFalloInterno(String valor) {
        Pago pago = prepararResolucion("CREADO");
        ReflectionTestUtils.setField(pago, "montoArs", new BigDecimal(valor));
        assertThrows(ConfiguracionSistemaException.class, () -> servicio.resolverAdicional(7, CLAVE));
        verifyNoInteractions(pasarela, historias, liquidaciones);
    }

    @Test
    void correlacionInvalidaDelPendienteNoSeEnviaDeNuevo() {
        Pago pago = prepararResolucion("PENDIENTE");
        pago.setReferenciaExterna("OTRA_REFERENCIA");
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen("100.00"));
        assertThrows(ConfiguracionSistemaException.class, () -> servicio.resolverAdicional(7, CLAVE));
        verifyNoInteractions(pasarela, historias);
    }

    @Test
    void resultadoDesconocidoDelProveedorNoSeConfirma() {
        Pago pago = prepararResolucion("PENDIENTE");
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen("100.00"));
        prepararProveedor(pago, "DESCONOCIDO");
        assertThrows(ConfiguracionSistemaException.class, () -> servicio.resolverAdicional(7, CLAVE));
        assertNull(pago.getFechaConfirmacion());
        verifyNoInteractions(historias);
    }

    private void prepararCabecera() {
        when(usuarios.buscarActivoPorEmailConBloqueo(EMAIL)).thenReturn(Optional.of(usuario));
        when(alquileres.buscarPorIdConBloqueo(7)).thenReturn(Optional.of(alquiler));
    }

    private void prepararInicio(List<Pago> anteriores) {
        prepararCabecera();
        when(liquidaciones.consultarLiquidacion(7)).thenReturn(resumen("100.00"));
        when(pagos.findByAlquilerIdOrderByIdAsc(7)).thenReturn(anteriores);
    }

    private Pago prepararResolucion(String estado) {
        prepararCabecera();
        Pago pago = intento(estado);
        when(pagos.findByClaveIdempotencia(CLAVE_DB)).thenReturn(Optional.of(pago));
        when(pagos.buscarPorIdConBloqueo(11)).thenReturn(Optional.of(pago));
        return pago;
    }

    private Pago intento(String estado) {
        Pago pago = new Pago(alquiler, new EstadoPago("CREADO"), "ADICIONAL",
                new BigDecimal("200.00"), AHORA.minusMinutes(5), "SIMULADA", "PRUEBA", CLAVE_DB);
        ReflectionTestUtils.setField(pago, "id", 11);
        pago.setEstadoPago(new EstadoPago(estado));
        if (!"CREADO".equals(estado) && !"CANCELADO".equals(estado)) pago.setReferenciaExterna("SIM-PAGO-11");
        if ("APROBADO".equals(estado)) pago.setFechaConfirmacion(AHORA.minusMinutes(1));
        return pago;
    }

    private void prepararProveedor(Pago pago, String resultado) {
        when(pagos.findByAlquilerUsuarioIdAndPlataformaAndEntornoOrderByIdAsc(1, "SIMULADA", "PRUEBA"))
                .thenReturn(List.of(pago));
        when(pasarela.obtenerSaldoInicial("qa.adicional")).thenReturn(new BigDecimal("1000.00"));
        when(pasarela.evaluarPago(pago, new BigDecimal("1000.00"))).thenReturn(resultado);
    }

    private void catalogo(String... codigos) {
        for (String codigo : codigos) when(estados.findByCodigo(codigo)).thenReturn(Optional.of(new EstadoPago(codigo)));
    }

    private LiquidacionAlquilerDTO resumen(String netoTexto) {
        BigDecimal neto = new BigDecimal(netoTexto);
        BigDecimal diferencia = new BigDecimal("300.00").subtract(neto);
        String situacion = diferencia.signum() > 0 ? "SALDO_PENDIENTE"
                : diferencia.signum() < 0 ? "REEMBOLSO_PENDIENTE" : "SALDADO";
        return new LiquidacionAlquilerDTO(7, 0, situacion, LIQUIDACION,
                new BigDecimal("2.00"), new BigDecimal("300.00"), neto,
                new BigDecimal("0.00"), neto,
                diferencia.max(new BigDecimal("0.00")), diferencia.negate().max(new BigDecimal("0.00")));
    }

    private void comprobarLiquidacionInmutable() {
        assertEquals("FINALIZADO", alquiler.getEstadoContratacion().getCodigo());
        assertEquals(LIQUIDACION, alquiler.getFechaHoraLiquidacion());
        assertEquals(LIQUIDACION, alquiler.getFechaHoraCierre());
        assertEquals(new BigDecimal("300.00"), alquiler.getCostoFinalArs());
        assertEquals(new BigDecimal("2.00"), alquiler.getCostoFinalUsd());
    }

    private void autenticar(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                EMAIL, null, AuthorityUtils.createAuthorityList(roles)));
    }
}
