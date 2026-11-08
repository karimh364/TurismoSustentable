package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.*;
import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.services.BicicletaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BicicletaControllerTest {
    @Mock private BicicletaService service;
    private MockMvc mvc;
    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new BicicletaController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @Test void altaValidaEnviaSoloIdsAlServicio() throws Exception {
        mvc.perform(post("/api/bicicletas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoBicicleta\":\" QA-01 \",\"tipoBicicletaId\":1,\"estacionId\":2}"))
                .andExpect(status().isCreated());
        ArgumentCaptor<CrearBicicletaDTO> captor = ArgumentCaptor.forClass(CrearBicicletaDTO.class);
        verify(service).crear(captor.capture());
        assertEquals("QA-01", captor.getValue().getCodigoBicicleta());
        assertEquals(1, captor.getValue().getTipoBicicletaId());
        assertEquals(2, captor.getValue().getEstacionId());
    }
    @Test void altaIncompletaMuestraListaDeErrores() throws Exception {
        mvc.perform(post("/api/bicicletas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray());
        verifyNoInteractions(service);
    }
    @Test void rechazaEstadoOPrecioInyectadosEnAlta() throws Exception {
        for (String extra : new String[]{"\"estado\":\"EN_USO\"", "\"precioPorHora\":1", "\"borradoBici\":true"}) {
            mvc.perform(post("/api/bicicletas").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"codigoBicicleta\":\"QA-01\",\"tipoBicicletaId\":1,\"estacionId\":2," + extra + "}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void actualizarExigeVersionYDestinoPositivo() throws Exception {
        mvc.perform(put("/api/bicicletas/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoBicicleta\":\"QA-01\",\"tipoBicicletaId\":1,\"estacionId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray());
        verifyNoInteractions(service);
    }
    @Test void actualizarNoPermiteCambiarEstadoNiPrecio() throws Exception {
        mvc.perform(put("/api/bicicletas/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoBicicleta\":\"QA-01\",\"tipoBicicletaId\":1,\"version\":0,\"estado\":\"DISPONIBLE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void versionObsoletaDevuelveConflictoConMensaje() throws Exception {
        when(service.actualizar(eq(1), any())).thenThrow(new ResponseStatusException(
                HttpStatus.CONFLICT, "La bicicleta fue modificada. Recargá los datos."));
        mvc.perform(put("/api/bicicletas/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigoBicicleta\":\"QA-01\",\"tipoBicicletaId\":1,\"version\":0}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("La bicicleta fue modificada. Recargá los datos."));
    }
    @Test void eliminarExigeMotivo() throws Exception {
        mvc.perform(delete("/api/bicicletas/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"motivo\":\"  \",\"salidaFisicaConfirmada\":true}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void eliminarDelegaConfirmacionVersionYMotivoYDevuelveSinContenido() throws Exception {
        mvc.perform(delete("/api/bicicletas/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"motivo\":\"Fin de vida útil\",\"salidaFisicaConfirmada\":true}"))
                .andExpect(status().isNoContent());
        ArgumentCaptor<EliminarBicicletaDTO> captor = ArgumentCaptor.forClass(EliminarBicicletaDTO.class);
        verify(service).eliminarLogico(eq(1), captor.capture());
        assertEquals(0, captor.getValue().getVersion());
        assertEquals("Fin de vida útil", captor.getValue().getMotivo());
        assertEquals(Boolean.TRUE, captor.getValue().getSalidaFisicaConfirmada());
    }
    @Test void eliminarSinConfirmacionOConConfirmacionNegativaNoLlegaAlServicio() throws Exception {
        for (String confirmacion : new String[]{"", ",\"salidaFisicaConfirmada\":false"}) {
            mvc.perform(delete("/api/bicicletas/1").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"version\":0,\"motivo\":\"Retiro definitivo\"" + confirmacion + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors").isArray());
        }
        verifyNoInteractions(service);
    }
    @Test void eliminarExigeLaVersionHeredadaDelDtoDeOperacion() throws Exception {
        mvc.perform(delete("/api/bicicletas/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Retiro definitivo\",\"salidaFisicaConfirmada\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isArray());
        verifyNoInteractions(service);
    }
    @Test void eliminarObsoletoDevuelveConflictoSinExponerUnErrorInterno() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "La bicicleta fue modificada. Recargá los datos."))
                .when(service).eliminarLogico(eq(1), any(EliminarBicicletaDTO.class));
        mvc.perform(delete("/api/bicicletas/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":0,\"motivo\":\"Retiro definitivo\",\"salidaFisicaConfirmada\":true}"))
                .andExpect(status().isConflict())
                .andExpect(content().string("La bicicleta fue modificada. Recargá los datos."));
    }
    @Test void listadoHabitualNoIncluyeEliminadasPorDefecto() throws Exception {
        when(service.listar(false)).thenReturn(List.of());
        mvc.perform(get("/api/bicicletas"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).listar(false);
    }
    @Test void consultaExplicitaPermiteIncluirEliminadas() throws Exception {
        when(service.listar(true)).thenReturn(List.of());
        mvc.perform(get("/api/bicicletas").param("incluirEliminadas", "true"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).listar(true);
    }
    @Test void tarifaRechazaRedondearTresDecimales() throws Exception {
        mvc.perform(put("/api/admin/tipos-bicicleta/1/tarifa").contentType(MediaType.APPLICATION_JSON)
                .content("{\"precioPorHoraUsd\":10.125,\"precioAnterior\":10.00}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void tarifaValidaVaAlTipoNoALaBicicleta() throws Exception {
        mvc.perform(put("/api/admin/tipos-bicicleta/1/tarifa").contentType(MediaType.APPLICATION_JSON)
                .content("{\"precioPorHoraUsd\":12.50,\"precioAnterior\":10.00}"))
                .andExpect(status().isOk());
        ArgumentCaptor<ActualizarTarifaTipoDTO> captor = ArgumentCaptor.forClass(ActualizarTarifaTipoDTO.class);
        verify(service).actualizarTarifa(eq(1), captor.capture());
        assertEquals(0, new BigDecimal("12.50").compareTo(captor.getValue().getPrecioPorHoraUsd()));
    }
}
