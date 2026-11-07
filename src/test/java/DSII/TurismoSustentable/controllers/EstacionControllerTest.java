package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.ActualizarEstacionDTO;
import DSII.TurismoSustentable.dto.CrearEstacionDTO;
import DSII.TurismoSustentable.dto.EstacionDTO;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.models.Bicicleta;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import DSII.TurismoSustentable.services.EstacionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EstacionControllerTest {

    @Mock
    private EstacionService estacionService;

    @Mock
    private EstacionRepository estacionRepository;

    @Mock
    private BicicletaRepository bicicletaRepository;

    @InjectMocks
    private EstacionController controlador;

    private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void prepararMvcSinBaseDeDatos() {
        mvc = MockMvcBuilders.standaloneSetup(controlador)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void devuelve200YEnviaLosDatosNormalizadosAlServicio()
            throws Exception {

        Estacion estacion = new Estacion(
                "Plaza Nueva", "Calle 2", 8, 0.0, 0.0
        );

        ReflectionTestUtils.setField(estacion, "id", 1);
        ReflectionTestUtils.setField(estacion, "version", 0);

        when(estacionService.actualizarEstacion(
                eq(1), any(ActualizarEstacionDTO.class)
        )).thenReturn(new EstacionDTO(estacion));

        enviar(datosValidos())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Plaza Nueva"))
                .andExpect(jsonPath("$.direccion").value("Calle 2"))
                .andExpect(jsonPath("$.capacidad").value(8))
                .andExpect(jsonPath("$.esPublica").value(true))
                .andExpect(jsonPath("$.latitud").value(0.0))
                .andExpect(jsonPath("$.longitud").value(0.0))
                .andExpect(jsonPath("$.version").value(0));

        ArgumentCaptor<ActualizarEstacionDTO> captor =
                ArgumentCaptor.forClass(ActualizarEstacionDTO.class);

        verify(estacionService).actualizarEstacion(
                eq(1), captor.capture()
        );

        ActualizarEstacionDTO recibido = captor.getValue();

        assertEquals("Plaza Nueva", recibido.getNombre());
        assertEquals("Calle 2", recibido.getDireccion());
        assertEquals(Integer.valueOf(0), recibido.getVersion());
    }

    @Test
    void rechazaCadaCampoObligatorioAusente() throws Exception {
        for (String campo : new String[]{
                "nombre", "direccion", "capacidad",
                "latitud", "longitud", "version"
        }) {
            Map<String, Object> datos = datosValidos();
            datos.remove(campo);

            enviar(datos)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(
                            "$.validationErrors",
                            hasItem(startsWith(campo + ":"))
                    ));
        }

        verifyNoInteractions(estacionService);
    }

    @Test
    void rechazaValoresInvalidosAntesDeLlamarAlServicio()
            throws Exception {

        Map<String, Object> datos = datosValidos();

        datos.put("nombre", "   ");
        datos.put("direccion", "   ");
        datos.put("capacidad", 0);
        datos.put("latitud", 91.0);
        datos.put("longitud", 181.0);
        datos.put("version", -1);

        ResultActions respuesta = enviar(datos)
                .andExpect(status().isBadRequest());

        for (String campo : new String[]{
                "nombre", "direccion", "capacidad",
                "latitud", "longitud", "version"
        }) {
            respuesta.andExpect(jsonPath(
                    "$.validationErrors",
                    hasItem(startsWith(campo + ":"))
            ));
        }

        verifyNoInteractions(estacionService);
    }

    @Test
    void rechazaCuerpoAusenteNuloOMalformado() throws Exception {
        for (String cuerpo : new String[]{"", "null", "{"}) {
            mvc.perform(put("/api/estaciones/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(estacionService);
    }

    @Test
    void devuelve404CuandoLaEstacionNoExiste() throws Exception {
        doThrow(new EntityNotFoundException(
                "La estación no existe o está inactiva."
        )).when(estacionService).actualizarEstacion(
                eq(1), any(ActualizarEstacionDTO.class)
        );

        enviar(datosValidos())
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        "La estación no existe o está inactiva."
                ));
    }

    @Test
    void devuelve409ParaConflictosDeNombreOCapacidad()
            throws Exception {

        for (String mensaje : new String[]{
                "Ya existe otra estación con ese nombre.",
                "La capacidad no puede ser menor que la ocupación."
        }) {
            doThrow(new IllegalStateException(mensaje))
                    .when(estacionService).actualizarEstacion(
                            eq(1), any(ActualizarEstacionDTO.class)
                    );

            enviar(datosValidos())
                    .andExpect(status().isConflict())
                    .andExpect(content().string(mensaje));
        }
    }

    @Test
    void devuelve409ParaAmbasExcepcionesDeVersion()
            throws Exception {

        for (RuntimeException error : new RuntimeException[]{
                new OptimisticLockException("Detalle interno"),
                new OptimisticLockingFailureException("Detalle interno")
        }) {
            doThrow(error)
                    .when(estacionService).actualizarEstacion(
                            eq(1), any(ActualizarEstacionDTO.class)
                    );

            enviar(datosValidos())
                    .andExpect(status().isConflict())
                    .andExpect(content().string(
                            "La estación fue modificada por otra operación. " +
                                    "Recargá sus datos e intentá nuevamente."
                    ));
        }
    }

    @Test
    void devuelve409PorIntegridadSinExponerDetallesInternos()
            throws Exception {

        doThrow(new DataIntegrityViolationException(
                "SQL: detalle interno de la restricción"
        )).when(estacionService).actualizarEstacion(
                eq(1), any(ActualizarEstacionDTO.class)
        );

        enviar(datosValidos())
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Los datos entran en conflicto con otra estación. " +
                                "Revisá el nombre."
                ));
    }

    @Test
    void devuelve400PorArgumentosInvalidos() throws Exception {
        doThrow(new IllegalArgumentException(
                "Los datos no son válidos."
        )).when(estacionService).actualizarEstacion(
                eq(1), any(ActualizarEstacionDTO.class)
        );

        enviar(datosValidos())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Los datos no son válidos."
                ));
    }

    @Test
    void devuelveEsPublicaFalseParaUnaEstacionInterna()
            throws Exception {

        Estacion taller = new Estacion(
                "Taller QA", "Calle 2", 8, 0.0, 0.0
        );

        taller.setEsPublica(false);

        ReflectionTestUtils.setField(taller, "id", 1);
        ReflectionTestUtils.setField(taller, "version", 0);

        when(estacionService.actualizarEstacion(
                eq(1), any(ActualizarEstacionDTO.class)
        )).thenReturn(new EstacionDTO(taller));

        Map<String, Object> datos = datosValidos();
        datos.put("nombre", "Taller QA");

        enviar(datos)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Taller QA"))
                .andExpect(jsonPath("$.esPublica").value(false));
    }

    @Test
    void getPublicoDelegaEnLaLecturaPublica() throws Exception {
        when(estacionService.obtenerEstacionesPublicas())
                .thenReturn(List.of(
                        estacionParaListado(1, "Plaza QA", true)
                ));

        mvc.perform(get("/api/estaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Plaza QA"))
                .andExpect(jsonPath("$[0].esPublica").value(true));

        verify(estacionService).obtenerEstacionesPublicas();

        verify(estacionService, never())
                .obtenerEstacionesAdministracion();
    }

    @Test
    void getAdministrativoDevuelvePublicasEInternas()
            throws Exception {

        when(estacionService.obtenerEstacionesAdministracion())
                .thenReturn(List.of(
                        estacionParaListado(1, "Plaza QA", true),
                        estacionParaListado(2, "Taller QA", false)
                ));

        mvc.perform(get("/api/admin/estaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].esPublica").value(true))
                .andExpect(jsonPath("$[1].nombre").value("Taller QA"))
                .andExpect(jsonPath("$[1].esPublica").value(false));

        verify(estacionService).obtenerEstacionesAdministracion();

        verify(estacionService, never())
                .obtenerEstacionesPublicas();
    }

    @Test
    void deleteRechazaEstacionConBicicletaArchivada()
            throws Exception {

        Estacion estacion = new Estacion(
                "Taller QA", "Calle QA", 6, 0.0, 0.0
        );
        estacion.setEsPublica(false);
        ReflectionTestUtils.setField(estacion, "id", 1);

        Bicicleta archivada = new Bicicleta();
        archivada.setCodigoBicicleta("QA-ARCHIVADA");
        archivada.setBorradoBici(true);
        archivada.setUbicacionActual(estacion);
        estacion.getBicicletasEnEstacion().add(archivada);

        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(estacion));

        when(bicicletaRepository.countByUbicacionActual(estacion))
                .thenReturn(1L);

        mvc.perform(delete("/api/estaciones/1"))
                .andExpect(status().isConflict())
                .andExpect(content().string(startsWith(
                        "No se puede eliminar la estación porque contiene "
                )));

        assertFalse(estacion.isBorradoEstacion());
        assertTrue(archivada.isBorradoBici());

        verify(estacionRepository)
                .buscarActivaPorIdConBloqueo(1);
        verify(bicicletaRepository)
                .countByUbicacionActual(estacion);

        verify(bicicletaRepository, never())
                .countByUbicacionActualAndBorradoBiciFalse(estacion);

        verifyNoMoreInteractions(
                estacionRepository,
                bicicletaRepository
        );
        verifyNoInteractions(estacionService);
    }

    @Test
    void deleteEstacionVaciaRealizaSoloBajaLogica()
            throws Exception {

        Estacion estacion = new Estacion(
                "Taller QA", "Calle QA", 6, 0.0, 0.0
        );
        estacion.setEsPublica(false);
        ReflectionTestUtils.setField(estacion, "id", 1);

        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.of(estacion));

        when(bicicletaRepository.countByUbicacionActual(estacion))
                .thenReturn(0L);

        mvc.perform(delete("/api/estaciones/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertTrue(estacion.isBorradoEstacion());

        verify(estacionRepository)
                .buscarActivaPorIdConBloqueo(1);
        verify(bicicletaRepository)
                .countByUbicacionActual(estacion);
        verify(estacionRepository).save(estacion);

        // Ninguna eliminación física ni otra operación adicional.
        verifyNoMoreInteractions(
                estacionRepository,
                bicicletaRepository
        );
        verifyNoInteractions(estacionService);
    }

    @Test
    void deleteDevuelve404CuandoNoExisteUnaEstacionActiva()
            throws Exception {

        when(estacionRepository.buscarActivaPorIdConBloqueo(1))
                .thenReturn(Optional.empty());

        mvc.perform(delete("/api/estaciones/1"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        "La estación con ID 1 no existe o está inactiva."
                ));

        verify(estacionRepository)
                .buscarActivaPorIdConBloqueo(1);
        verifyNoMoreInteractions(estacionRepository);

        verifyNoInteractions(
                bicicletaRepository,
                estacionService
        );
    }

    @Test
    void postCreaEstacionPublicaCon201() throws Exception {
        comprobarAlta201(true);
    }

    @Test
    void postCreaEstacionInternaCon201() throws Exception {
        comprobarAlta201(false);
    }

    @Test
    void postRechazaCamposObligatoriosAusentesONulos()
            throws Exception {

        for (String campo : new String[]{
                "nombre", "direccion", "capacidad",
                "latitud", "longitud", "esPublica"
        }) {
            for (boolean enviarNulo : new boolean[]{false, true}) {
                Map<String, Object> alta = datosAltaValidos(true);

                if (enviarNulo) {
                    alta.put(campo, null);
                } else {
                    alta.remove(campo);
                }

                enviarAlta(alta)
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath(
                                "$.validationErrors",
                                hasItem(startsWith(campo + ":"))
                        ));
            }
        }

        verifyNoInteractions(
                estacionService,
                estacionRepository,
                bicicletaRepository
        );
    }

    @Test
    void postRechazaValoresInvalidos() throws Exception {
        Object[][] casos = {
                {"nombre", "   "},
                {"nombre", "x".repeat(256)},
                {"direccion", "   "},
                {"direccion", "x".repeat(256)},
                {"capacidad", 0},
                {"capacidad", -1},
                {"latitud", -91.0},
                {"latitud", 91.0},
                {"longitud", -181.0},
                {"longitud", 181.0}
        };

        for (Object[] caso : casos) {
            String campo = (String) caso[0];
            Map<String, Object> alta = datosAltaValidos(true);
            alta.put(campo, caso[1]);

            enviarAlta(alta)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath(
                            "$.validationErrors",
                            hasItem(startsWith(campo + ":"))
                    ));
        }

        verifyNoInteractions(
                estacionService,
                estacionRepository,
                bicicletaRepository
        );
    }

    @Test
    void postRechazaCuerpoAusenteNuloOIncompatible()
            throws Exception {

        for (String cuerpo : new String[]{"", "null", "{"}) {
            mvc.perform(post("/api/estaciones")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isBadRequest());
        }

        for (String campo : new String[]{
                "capacidad", "latitud", "longitud", "esPublica"
        }) {
            Map<String, Object> alta = datosAltaValidos(true);
            alta.put(campo, "valor inválido");

            enviarAlta(alta)
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(
                estacionService,
                estacionRepository,
                bicicletaRepository
        );
    }

    @Test
    void postDevuelve409PorNombreDuplicado() throws Exception {
        when(estacionService.crearEstacion(any(CrearEstacionDTO.class)))
                .thenThrow(new IllegalStateException(
                        "Ya existe una estación con ese nombre."
                ));

        enviarAlta(datosAltaValidos(true))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Ya existe una estación con ese nombre."
                ));

        verify(estacionService)
                .crearEstacion(any(CrearEstacionDTO.class));
        verifyNoInteractions(estacionRepository, bicicletaRepository);
    }

    @Test
    void postDevuelve409PorIntegridadSinExponerSQL()
            throws Exception {

        when(estacionService.crearEstacion(any(CrearEstacionDTO.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "SQL: detalle interno de la restricción"
                ));

        enviarAlta(datosAltaValidos(true))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Los datos entran en conflicto con otra estación. " +
                                "Revisá el nombre."
                ));

        verify(estacionService)
                .crearEstacion(any(CrearEstacionDTO.class));
        verifyNoInteractions(estacionRepository, bicicletaRepository);
    }

    private Map<String, Object> datosAltaValidos(boolean esPublica) {
        return new HashMap<>(Map.of(
                "nombre", "  Plaza   QA  ",
                "direccion", "  Calle   QA  ",
                "capacidad", 6,
                "latitud", 0.0,
                "longitud", 0.0,
                "esPublica", esPublica
        ));
    }

    private ResultActions enviarAlta(Map<String, Object> alta)
            throws Exception {

        return mvc.perform(post("/api/estaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(alta)));
    }

    private void comprobarAlta201(boolean esPublica)
            throws Exception {

        when(estacionService.crearEstacion(any(CrearEstacionDTO.class)))
                .thenReturn(estacionParaListado(
                        2, "Plaza QA", esPublica
                ));

        enviarAlta(datosAltaValidos(esPublica))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nombre").value("Plaza QA"))
                .andExpect(jsonPath("$.esPublica").value(esPublica));

        ArgumentCaptor<CrearEstacionDTO> captor =
                ArgumentCaptor.forClass(CrearEstacionDTO.class);

        verify(estacionService).crearEstacion(captor.capture());

        CrearEstacionDTO recibido = captor.getValue();

        assertEquals("Plaza QA", recibido.getNombre());
        assertEquals("Calle QA", recibido.getDireccion());
        assertEquals(Integer.valueOf(6), recibido.getCapacidad());
        assertEquals(Double.valueOf(0.0), recibido.getLatitud());
        assertEquals(Double.valueOf(0.0), recibido.getLongitud());
        assertEquals(Boolean.valueOf(esPublica), recibido.getEsPublica());

        verifyNoMoreInteractions(estacionService);
        verifyNoInteractions(estacionRepository, bicicletaRepository);
    }

    private EstacionDTO estacionParaListado(
            int id,
            String nombre,
            boolean esPublica
    ) {
        Estacion estacion = new Estacion(
                nombre, "Calle QA", 6, 0.0, 0.0
        );

        estacion.setEsPublica(esPublica);
        ReflectionTestUtils.setField(estacion, "id", id);
        ReflectionTestUtils.setField(estacion, "version", 0);

        return new EstacionDTO(estacion);
    }

    private Map<String, Object> datosValidos() {
        return new HashMap<>(Map.of(
                "nombre", " Plaza Nueva ",
                "direccion", " Calle 2 ",
                "capacidad", 8,
                "latitud", 0.0,
                "longitud", 0.0,
                "version", 0
        ));
    }

    private ResultActions enviar(Map<String, Object> datos)
            throws Exception {

        return mvc.perform(put("/api/estaciones/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(datos)));
    }
}
