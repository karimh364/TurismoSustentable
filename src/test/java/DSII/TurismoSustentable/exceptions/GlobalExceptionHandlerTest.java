package DSII.TurismoSustentable.exceptions;

import DSII.TurismoSustentable.controllers.UsuarioController;
import DSII.TurismoSustentable.dto.RegistroUsuarioDTO;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.services.CorreoService;
import DSII.TurismoSustentable.services.RegistroUsuarioService;
import DSII.TurismoSustentable.services.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CorreoService correoService;
    @Mock private UsuarioService usuarioService;
    @Mock private RegistroUsuarioService registroUsuarioService;

    private MockMvc mockMvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void prepararMvcSinBaseDeDatos() {
        UsuarioController controller = new UsuarioController(
                usuarioRepository, correoService, usuarioService, registroUsuarioService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void configuracionIncorrectaDevuelve500SinExponerDetalles() throws Exception {
        when(registroUsuarioService.registrarUsuario(any()))
                .thenThrow(new ConfiguracionSistemaException("El rol CLIENTE no está configurado."));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datosValidos())))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        "No se pudo completar la operación por un problema interno. " +
                                "Intentá nuevamente más tarde."
                ))
                .andExpect(content().string(not(containsString("CLIENTE"))));

        verifyNoInteractions(correoService, usuarioRepository, usuarioService);
    }

    @Test
    void unDuplicadoSigueSiendo409YNoUnErrorDeConfiguracion() throws Exception {
        when(registroUsuarioService.registrarUsuario(any()))
                .thenThrow(new IllegalStateException("El nickname ya está en uso."));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datosValidos())))
                .andExpect(status().isConflict())
                .andExpect(content().string("El nickname ya está en uso."));
        verifyNoInteractions(correoService);
    }

    @Test
    void formatoIncorrectoSigueProduciendoLaLista400AntesDelServicio() throws Exception {
        Map<String, Object> datos = datosValidos();
        datos.put("nombre", "Ana123");

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors", hasItem(startsWith("nombre: "))));
        verifyNoInteractions(registroUsuarioService, correoService);
    }

    @Test
    void normalizaLosNombresAntesDeInvocarElServicio() throws Exception {
        Map<String, Object> datos = datosValidos();
        datos.put("nombre", " Jose\u0301  María ");
        datos.put("apellido", " O\u2019Connor ");
        when(registroUsuarioService.registrarUsuario(any()))
                .thenThrow(new IllegalArgumentException("Fin controlado de la prueba."));

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(datos)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Fin controlado de la prueba."));

        ArgumentCaptor<RegistroUsuarioDTO> captor = ArgumentCaptor.forClass(RegistroUsuarioDTO.class);
        verify(registroUsuarioService).registrarUsuario(captor.capture());
        assertEquals("José María", captor.getValue().getNombre());
        assertEquals("O'Connor", captor.getValue().getApellido());
        verifyNoInteractions(correoService);
    }

    private Map<String, Object> datosValidos() {
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", "Ana");
        datos.put("apellido", "Perez");
        datos.put("nickname", "ana.perez");
        datos.put("email", "ana@example.com");
        datos.put("fechaNacimiento", "1995-05-20");
        datos.put("generoId", 7);
        datos.put("tipoDocumentoId", 21);
        datos.put("numeroDocumento", "00123456");
        datos.put("paisEmisorId", 42);
        datos.put("password", "Clave?123");
        return datos;
    }
}
