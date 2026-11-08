package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BicicletaServiceTest {
    @Mock BicicletaRepository bicicletas;
    @Mock TipoBicicletaRepository tipos;
    @Mock EstadoBicicletaRepository estados;
    @Mock EstacionRepository estaciones;
    @Mock AlquilerRepository alquileres;
    @Mock UsuarioRepository usuarios;
    @Mock HistorialEstadoBicicletaRepository historial;
    @Mock AuditoriaSistemaRepository auditoria;
    private BicicletaService service;
    private Bicicleta bici;
    private TipoBicicleta tipo;
    private Estacion origen, taller;
    private ActualizarBicicletaDTO solicitud;
    private Usuario admin;

    @BeforeEach void preparar() {
        service = new BicicletaService(bicicletas, tipos, estados, estaciones, alquileres, usuarios,
                new TrazabilidadInventarioService(historial, auditoria));
        admin = new Usuario();
        admin.setId(1); admin.setEmail("admin.qa@example.com"); admin.setActivo(true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin.getEmail(), null, List.of(new SimpleGrantedAuthority("ADMIN"))));
        lenient().when(usuarios.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
        tipo = new TipoBicicleta("MONTANA", new BigDecimal("10.00"), 3);
        ReflectionTestUtils.setField(tipo, "id", 1);
        origen = estacion(1, true);
        taller = estacion(2, false);
        bici = new Bicicleta("QA-01", tipo, new EstadoBicicleta("DISPONIBLE"), origen);
        bici.setId(1); bici.setFechaAlta(LocalDate.of(2026, 11, 7));
        ReflectionTestUtils.setField(bici, "version", 0);
        lenient().when(bicicletas.buscarActivaPorIdConBloqueo(1)).thenReturn(Optional.of(bici));
        lenient().when(tipos.findById(1)).thenReturn(Optional.of(tipo));
        lenient().when(estaciones.buscarActivaPorIdConBloqueo(1)).thenReturn(Optional.of(origen));
        lenient().when(estaciones.buscarActivaPorIdConBloqueo(2)).thenReturn(Optional.of(taller));
        lenient().when(bicicletas.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        solicitud = new ActualizarBicicletaDTO();
        solicitud.setCodigoBicicleta("QA-02"); solicitud.setTipoBicicletaId(1);
        solicitud.setVersion(0); solicitud.setEstacionId(2); solicitud.setMotivo("Traslado al taller.");
    }
    @AfterEach void limpiar() { SecurityContextHolder.clearContext(); }

    @Test void altaUsaCatalogosFechaRealEHistorialInicial() {
        when(estados.findByCodigo("DISPONIBLE")).thenReturn(Optional.of(new EstadoBicicleta("DISPONIBLE")));
        CrearBicicletaDTO dto = new CrearBicicletaDTO();
        dto.setCodigoBicicleta("NUEVA"); dto.setTipoBicicletaId(1); dto.setEstacionId(2);
        when(bicicletas.saveAndFlush(any())).thenAnswer(inv -> {
            Bicicleta nueva = inv.getArgument(0); nueva.setId(9); return nueva;
        });
        BicicletaDTO salida = service.crear(dto);
        assertEquals(LocalDate.now(), salida.getFechaAlta());
        assertEquals(new BigDecimal("10.00"), salida.getPrecio());
        ArgumentCaptor<HistorialEstadoBicicleta> captor = ArgumentCaptor.forClass(HistorialEstadoBicicleta.class);
        verify(historial).save(captor.capture());
        assertNull(captor.getValue().getEstadoAnterior());
        assertEquals("DISPONIBLE", captor.getValue().getEstadoNuevo().getCodigo());
        assertSame(admin, captor.getValue().getActorUsuario());
        assertEquals("USUARIO", captor.getValue().getOrigen());
        verify(auditoria).save(any(AuditoriaSistema.class));
    }
    @Test void destinoInexistenteNoModificaDatos() {
        when(estaciones.buscarActivaPorIdConBloqueo(2)).thenReturn(Optional.empty());
        rechazaSinCambios(HttpStatus.BAD_REQUEST, () -> service.actualizar(1, solicitud));
    }
    @Test void capacidadCuentaTodasLasUnidadesFisicasYRechazaSinCambios() {
        when(bicicletas.countByUbicacionActual(taller)).thenReturn(2L);
        rechazaSinCambios(HttpStatus.CONFLICT, () -> service.actualizar(1, solicitud));
        verify(bicicletas).countByUbicacionActual(taller);
        verify(bicicletas, never()).countByUbicacionActualAndBorradoBiciFalse(any());
    }
    @Test void mismoDestinoNoConsumeOtroLugar() {
        solicitud.setEstacionId(1);
        service.actualizar(1, solicitud);
        assertSame(origen, bici.getUbicacionActual());
        verify(bicicletas, never()).countByUbicacionActual(any());
        verifyNoInteractions(historial);
        verify(auditoria).save(any(AuditoriaSistema.class));
    }
    @Test void destinoNuloNoVaciaUbicacion() {
        solicitud.setEstacionId(null);
        service.actualizar(1, solicitud);
        assertSame(origen, bici.getUbicacionActual());
    }
    @Test void trasladoExigeMotivoAntesDeCambiarDatos() {
        solicitud.setMotivo(" ");
        rechazaSinCambios(HttpStatus.BAD_REQUEST, () -> service.actualizar(1, solicitud));
    }
    @Test void trasladoInternoConservaEstadoYRegistraAdministrador() {
        service.actualizar(1, solicitud);
        assertSame(taller, bici.getUbicacionActual());
        assertEquals("DISPONIBLE", bici.getEstado().getCodigo());
        ArgumentCaptor<AuditoriaSistema> captor = ArgumentCaptor.forClass(AuditoriaSistema.class);
        verify(auditoria).save(captor.capture());
        assertSame(admin, captor.getValue().getAdministrador());
        assertEquals("Traslado al taller.", captor.getValue().getMotivo());
        verifyNoInteractions(historial);
    }
    @Test void versionObsoletaNoSobrescribe() {
        solicitud.setVersion(7);
        rechazaSinCambios(HttpStatus.CONFLICT, () -> service.actualizar(1, solicitud));
    }
    @Test void codigoDuplicadoNoModificaDatos() {
        when(bicicletas.existsByCodigoBicicletaIgnoreCaseAndIdNot("QA-02", 1)).thenReturn(true);
        rechazaSinCambios(HttpStatus.CONFLICT, () -> service.actualizar(1, solicitud));
    }
    @Test void alquilerAsociadoImpideEditarAunqueEstadoFueraIncoherente() {
        when(alquileres.existsByBicicletaAndEstado(bici, CodigoEstadoAlquiler.INICIADO)).thenReturn(true);
        rechazaSinCambios(HttpStatus.CONFLICT, () -> service.actualizar(1, solicitud));
    }
    @Test void estadosDeProcesoNoSeLiberanDesdeEdicionGeneral() {
        for (String codigo : List.of("EN_USO", "RESERVADA", "MANTENIMIENTO", "EXTRAVIADA", "DE_BAJA")) {
            bici.setEstado(new EstadoBicicleta(codigo));
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.actualizar(1, solicitud));
            assertEquals(HttpStatus.CONFLICT, error.getStatus());
            assertEquals(codigo, bici.getEstado().getCodigo());
            assertEquals("QA-01", bici.getCodigoBicicleta());
        }
        verify(bicicletas, never()).saveAndFlush(any());
        verifyNoInteractions(historial, auditoria);
    }
    @Test void eliminarDesdePublicaRetiraUnidadConOrigenTrazadoSinExigirCupoLibre() {
        EstadoBicicleta anterior = bici.getEstado();
        when(estados.findByCodigo("DE_BAJA")).thenReturn(Optional.of(new EstadoBicicleta("DE_BAJA")));
        service.eliminarLogico(1, eliminacion());
        assertTrue(bici.isBorradoBici());
        assertNull(bici.getUbicacionActual());
        assertEquals("DE_BAJA", bici.getEstado().getCodigo());
        assertEquals(1, bici.getId());
        assertSame(tipo, bici.getTipo());
        assertEquals(LocalDate.of(2026, 11, 7), bici.getFechaAlta());
        verify(estaciones).buscarActivaPorIdConBloqueo(1);
        verify(bicicletas, never()).countByUbicacionActual(any());
        verify(bicicletas).saveAndFlush(bici);
        verify(bicicletas, never()).delete(any(Bicicleta.class));
        verify(bicicletas, never()).deleteById(anyInt());
        ArgumentCaptor<HistorialEstadoBicicleta> evento = ArgumentCaptor.forClass(HistorialEstadoBicicleta.class);
        verify(historial).save(evento.capture());
        assertSame(anterior, evento.getValue().getEstadoAnterior());
        assertEquals("DE_BAJA", evento.getValue().getEstadoNuevo().getCodigo());
        assertSame(admin, evento.getValue().getActorUsuario());
        ArgumentCaptor<AuditoriaSistema> registro = ArgumentCaptor.forClass(AuditoriaSistema.class);
        verify(auditoria).save(registro.capture());
        assertEquals("ELIMINAR_SALIDA_DEFINITIVA", registro.getValue().getAccion());
        assertSame(admin, registro.getValue().getAdministrador());
        assertEquals("Motivo QA", registro.getValue().getMotivo());
        assertNotNull(registro.getValue().getFechaOperacion());
        assertTrue(registro.getValue().getValoresAnteriores().contains("estacionId=1"));
        assertTrue(registro.getValue().getValoresNuevos().contains("estacionId=null"));
        assertTrue(registro.getValue().getValoresNuevos().contains("estado=DE_BAJA"));
        assertTrue(registro.getValue().getValoresNuevos().contains("borrado=true"));
    }
    @Test void eliminarDesdePublicaInhabilitadaOInexistenteNoModificaDatos() {
        when(estaciones.buscarActivaPorIdConBloqueo(1)).thenReturn(Optional.empty());
        rechazaSinCambios(HttpStatus.BAD_REQUEST, () -> service.eliminarLogico(1, eliminacion()));
        verify(estaciones).buscarActivaPorIdConBloqueo(1);
    }
    @Test void darDeBajaSinEliminarDesdePublicaSigueRechazado() {
        rechazaSinCambios(HttpStatus.CONFLICT, () -> service.cambiarEstado(1, "DE_BAJA", operacion()));
        verify(estaciones).buscarActivaPorIdConBloqueo(1);
    }
    @Test void eliminarDesdeTallerRetiraUnidadYConservaTrazabilidadSinBorrarFilas() {
        bici.setUbicacionActual(taller);
        EstadoBicicleta anterior = bici.getEstado();
        EstadoBicicleta baja = new EstadoBicicleta("DE_BAJA");
        when(estados.findByCodigo("DE_BAJA")).thenReturn(Optional.of(baja));
        service.eliminarLogico(1, eliminacion());
        assertTrue(bici.isBorradoBici());
        assertNull(bici.getUbicacionActual());
        assertSame(baja, bici.getEstado());
        assertEquals(1, bici.getId());
        assertSame(tipo, bici.getTipo());
        assertEquals(LocalDate.of(2026, 11, 7), bici.getFechaAlta());
        verify(bicicletas).saveAndFlush(bici);
        verify(bicicletas, never()).delete(any(Bicicleta.class));
        verify(bicicletas, never()).deleteById(anyInt());
        ArgumentCaptor<HistorialEstadoBicicleta> evento = ArgumentCaptor.forClass(HistorialEstadoBicicleta.class);
        verify(historial).save(evento.capture());
        assertSame(anterior, evento.getValue().getEstadoAnterior());
        assertSame(baja, evento.getValue().getEstadoNuevo());
        assertSame(admin, evento.getValue().getActorUsuario());
        assertEquals("USUARIO", evento.getValue().getOrigen());
        ArgumentCaptor<AuditoriaSistema> registro = ArgumentCaptor.forClass(AuditoriaSistema.class);
        verify(auditoria).save(registro.capture());
        assertEquals("ELIMINAR_SALIDA_DEFINITIVA", registro.getValue().getAccion());
        assertSame(admin, registro.getValue().getAdministrador());
        assertEquals("BICICLETA", registro.getValue().getEntidadAfectada());
        assertEquals(1, registro.getValue().getEntidadId());
        assertEquals("Motivo QA", registro.getValue().getMotivo());
        assertNotNull(registro.getValue().getFechaOperacion());
        assertTrue(registro.getValue().getValoresAnteriores().contains("estacionId=2"));
        assertTrue(registro.getValue().getValoresAnteriores().contains("estado=DISPONIBLE"));
        assertTrue(registro.getValue().getValoresNuevos().contains("estacionId=null"));
        assertTrue(registro.getValue().getValoresNuevos().contains("estado=DE_BAJA"));
        assertTrue(registro.getValue().getValoresNuevos().contains("borrado=true"));
    }
    @Test void eliminarUnaBajaNoDuplicaTransicionDeEstado() {
        bici.setUbicacionActual(taller);
        EstadoBicicleta baja = new EstadoBicicleta("DE_BAJA");
        bici.setEstado(baja);
        service.eliminarLogico(1, eliminacion());
        assertSame(baja, bici.getEstado());
        assertTrue(bici.isBorradoBici());
        assertNull(bici.getUbicacionActual());
        verifyNoInteractions(historial, estados);
        verify(auditoria).save(any(AuditoriaSistema.class));
    }
    @Test void eliminarUnaInactivaNoExigePulsarDarDeBajaAntes() {
        bici.setUbicacionActual(taller);
        bici.setEstado(new EstadoBicicleta("INACTIVA"));
        when(estados.findByCodigo("DE_BAJA")).thenReturn(Optional.of(new EstadoBicicleta("DE_BAJA")));
        service.eliminarLogico(1, eliminacion());
        assertEquals("DE_BAJA", bici.getEstado().getCodigo());
        assertTrue(bici.isBorradoBici());
        assertNull(bici.getUbicacionActual());
        verify(historial).save(any(HistorialEstadoBicicleta.class));
    }
    @Test void eliminarSinConfirmacionRealSeRechazaAntesDeMutar() {
        for (Boolean confirmacion : Arrays.asList(null, Boolean.FALSE)) {
            EliminarBicicletaDTO dto = eliminacion();
            dto.setSalidaFisicaConfirmada(confirmacion);
            rechazaSinCambios(HttpStatus.BAD_REQUEST, () -> service.eliminarLogico(1, dto));
        }
    }
    @Test void eliminarSinMotivoSeRechazaSinCambios() {
        EliminarBicicletaDTO dto = eliminacion();
        dto.setMotivo(" ");
        rechazaSinCambios(HttpStatus.BAD_REQUEST, () -> service.eliminarLogico(1, dto));
    }
    @Test void eliminarConVersionObsoletaNoLiberaUnLugar() {
        EliminarBicicletaDTO dto = eliminacion();
        dto.setVersion(7);
        rechazaSinCambios(HttpStatus.CONFLICT, () -> service.eliminarLogico(1, dto));
    }
    @Test void eliminarConAlquilerAsociadoNoCierraElViajeNiLiberaLaBici() {
        when(alquileres.existsByBicicletaAndEstado(bici, CodigoEstadoAlquiler.INICIADO)).thenReturn(true);
        rechazaSinCambios(HttpStatus.CONFLICT, () -> service.eliminarLogico(1, eliminacion()));
        verify(alquileres, never()).save(any());
    }
    @Test void eliminarNoEludeLosEstadosDeProcesosPendientes() {
        for (String codigo : List.of("EN_USO", "RESERVADA", "MANTENIMIENTO", "EXTRAVIADA")) {
            bici.setEstado(new EstadoBicicleta(codigo));
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.eliminarLogico(1, eliminacion()));
            assertEquals(HttpStatus.CONFLICT, error.getStatus());
            assertEquals(codigo, bici.getEstado().getCodigo());
            assertSame(origen, bici.getUbicacionActual());
            assertFalse(bici.isBorradoBici());
        }
        verify(bicicletas, never()).saveAndFlush(any());
        verifyNoInteractions(historial, auditoria);
    }
    @Test void eliminarSinUbicacionNoInventaUnTaller() {
        bici.setUbicacionActual(null);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.eliminarLogico(1, eliminacion()));
        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        assertNull(bici.getUbicacionActual());
        assertEquals("DISPONIBLE", bici.getEstado().getCodigo());
        assertFalse(bici.isBorradoBici());
        verify(bicicletas, never()).saveAndFlush(any());
        verifyNoInteractions(historial, auditoria);
    }
    @Test void eliminarDesdeTallerInhabilitadoNoModificaDatos() {
        bici.setUbicacionActual(taller);
        when(estaciones.buscarActivaPorIdConBloqueo(2)).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.eliminarLogico(1, eliminacion()));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        assertSame(taller, bici.getUbicacionActual());
        assertFalse(bici.isBorradoBici());
        assertEquals("DISPONIBLE", bici.getEstado().getCodigo());
        verify(bicicletas, never()).saveAndFlush(any());
        verifyNoInteractions(historial, auditoria);
    }
    @Test void catalogoDeBajaFaltanteNoDejaEliminacionParcial() {
        bici.setUbicacionActual(taller);
        when(estados.findByCodigo("DE_BAJA")).thenReturn(Optional.empty());
        assertThrows(DSII.TurismoSustentable.exceptions.ConfiguracionSistemaException.class,
                () -> service.eliminarLogico(1, eliminacion()));
        assertSame(taller, bici.getUbicacionActual());
        assertEquals("DISPONIBLE", bici.getEstado().getCodigo());
        assertFalse(bici.isBorradoBici());
        verify(bicicletas, never()).saveAndFlush(any());
        verifyNoInteractions(historial, auditoria);
    }
    @Test void eliminadaPreviaNoSeReinterpretaNiSeRestaura() {
        bici.setBorradoBici(true);
        bici.setUbicacionActual(taller);
        when(bicicletas.buscarActivaPorIdConBloqueo(1)).thenReturn(Optional.empty());
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.eliminarLogico(1, eliminacion()));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        assertTrue(bici.isBorradoBici());
        assertSame(taller, bici.getUbicacionActual());
        verify(bicicletas, never()).saveAndFlush(any());
        verifyNoInteractions(historial, auditoria);
    }
    @Test void eliminarRequiereAdminNoAlcanzaClienteOTecnico() {
        for (String rol : List.of("CLIENTE", "TECNICO")) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                    admin.getEmail(), null, List.of(new SimpleGrantedAuthority(rol))));
            assertThrows(AccessDeniedException.class, () -> service.eliminarLogico(1, eliminacion()));
        }
        verifyNoInteractions(bicicletas, historial, auditoria);
    }
    @Test void bajaSinEliminarConservaUbicacionYRegistroVigente() {
        bici.setUbicacionActual(taller);
        when(estados.findByCodigo("DE_BAJA")).thenReturn(Optional.of(new EstadoBicicleta("DE_BAJA")));
        service.cambiarEstado(1, "DE_BAJA", operacion());
        assertEquals("DE_BAJA", bici.getEstado().getCodigo());
        assertFalse(bici.isBorradoBici());
        assertSame(taller, bici.getUbicacionActual());
        verify(historial).save(any(HistorialEstadoBicicleta.class));
    }
    @Test void inactivarYHabilitarRegistranAmbasTransiciones() {
        when(estados.findByCodigo("INACTIVA")).thenReturn(Optional.of(new EstadoBicicleta("INACTIVA")));
        when(estados.findByCodigo("DISPONIBLE")).thenReturn(Optional.of(new EstadoBicicleta("DISPONIBLE")));
        service.cambiarEstado(1, "INACTIVA", operacion());
        assertEquals("INACTIVA", bici.getEstado().getCodigo());
        service.cambiarEstado(1, "DISPONIBLE", operacion());
        assertEquals("DISPONIBLE", bici.getEstado().getCodigo());
        verify(historial, times(2)).save(any(HistorialEstadoBicicleta.class));
    }
    @Test void noPermiteCrearViajeReservaPerdidaOMantenimientoManualmente() {
        for (String codigo : List.of("EN_USO", "RESERVADA", "EXTRAVIADA", "MANTENIMIENTO")) {
            rechazaSinCambios(HttpStatus.CONFLICT, () -> service.cambiarEstado(1, codigo, operacion()));
        }
    }
    @Test void tarifaSeEditaEnElTipoNoEnCadaBicicleta() {
        when(tipos.buscarPorIdConBloqueo(1)).thenReturn(Optional.of(tipo));
        ActualizarTarifaTipoDTO dto = tarifa("12.50", "10.00");
        assertEquals(new BigDecimal("12.50"), service.actualizarTarifa(1, dto).getPrecioPorHoraUsd());
        verify(tipos).saveAndFlush(tipo);
        verify(auditoria).save(any(AuditoriaSistema.class));
        verifyNoInteractions(bicicletas, historial);
    }
    @Test void tarifaConcurrenteSeRechazaSinSobrescribir() {
        when(tipos.buscarPorIdConBloqueo(1)).thenReturn(Optional.of(tipo));
        assertThrows(ResponseStatusException.class, () -> service.actualizarTarifa(1, tarifa("12.50", "9.00")));
        assertEquals(new BigDecimal("10.00"), tipo.getPrecioPorHoraUsd());
        verify(tipos, never()).saveAndFlush(any());
        verifyNoInteractions(auditoria);
    }
    @Test void clienteNoPuedeEjecutarUnaOperacionAdministrativa() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                admin.getEmail(), null, List.of(new SimpleGrantedAuthority("CLIENTE"))));
        assertThrows(AccessDeniedException.class, () -> service.actualizar(1, solicitud));
        verifyNoInteractions(bicicletas, auditoria, historial);
    }
    @Test void consultaHabitualExcluyeEliminadasDesdeElRepositorio() {
        when(bicicletas.findByBorradoBiciFalse()).thenReturn(List.of(bici));
        List<BicicletaDTO> resultado = service.listar(false);
        assertEquals(1, resultado.size());
        assertFalse(resultado.get(0).isBorradoBici());
        verify(bicicletas).findByBorradoBiciFalse();
        verify(bicicletas, never()).findAll();
    }
    @Test void consultaAdministrativaPuedeIncluirEliminadas() {
        bici.setBorradoBici(true);
        when(bicicletas.findAll()).thenReturn(List.of(bici));
        assertTrue(service.listar(true).get(0).isBorradoBici());
        verify(bicicletas, never()).findByBorradoBiciFalse();
    }

    private Estacion estacion(int id, boolean publica) {
        Estacion e = new Estacion("QA-" + id, "Calle QA", 2, 0.0, 0.0);
        e.setEsPublica(publica);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }
    private OperacionBicicletaDTO operacion() {
        OperacionBicicletaDTO dto = new OperacionBicicletaDTO();
        dto.setVersion(0); dto.setMotivo("Motivo QA");
        return dto;
    }
    private EliminarBicicletaDTO eliminacion() {
        EliminarBicicletaDTO dto = new EliminarBicicletaDTO();
        dto.setVersion(0);
        dto.setMotivo("Motivo QA");
        dto.setSalidaFisicaConfirmada(true);
        return dto;
    }
    private ActualizarTarifaTipoDTO tarifa(String nuevo, String anterior) {
        ActualizarTarifaTipoDTO dto = new ActualizarTarifaTipoDTO();
        dto.setPrecioPorHoraUsd(new BigDecimal(nuevo));
        dto.setPrecioAnterior(new BigDecimal(anterior)); return dto;
    }
    private void rechazaSinCambios(HttpStatus status, org.junit.jupiter.api.function.Executable accion) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, accion);
        assertEquals(status, error.getStatus());
        assertEquals("QA-01", bici.getCodigoBicicleta());
        assertSame(tipo, bici.getTipo()); assertSame(origen, bici.getUbicacionActual());
        assertEquals("DISPONIBLE", bici.getEstado().getCodigo());
        assertFalse(bici.isBorradoBici());
        verify(bicicletas, never()).saveAndFlush(any());
        verifyNoInteractions(historial, auditoria);
    }
}
