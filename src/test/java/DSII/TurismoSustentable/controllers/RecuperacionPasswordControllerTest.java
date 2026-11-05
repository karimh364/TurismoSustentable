package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.exceptions.GlobalExceptionHandler;
import DSII.TurismoSustentable.services.RecuperacionPasswordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordControllerTest {

    @Mock
    private RecuperacionPasswordService servicio;

    private MockMvc mvc;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void prepararMvcSinBaseDeDatos() {
        mvc = MockMvcBuilders.standaloneSetup(new RecuperacionPasswordController(servicio))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rechazaEmailAusenteOVacioAntesDelServicio() throws Exception {
        for (String json : new String[]{"{}", "{\"email\":null}", "{\"email\":\"\"}", "{\"email\":\"   \"}"}) {
            mvc.perform(post("/api/password/solicitar")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors",
                            hasItem("email: El email no puede estar vacío.")));
        }
        verifyNoInteractions(servicio);
    }

    @Test
    void rechazaFormatoIncorrectoEnUnaPeticionDirecta() throws Exception {
        for (String email : new String[]{"hola", "ana@ejemplo", "@mail@gmail.com", "a..b@example.com"}) {
            mvc.perform(post("/api/password/solicitar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("email", email))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.validationErrors",
                            hasItem("email: Ingresá una dirección de correo válida.")));
        }
        verifyNoInteractions(servicio);
    }

    @Test
    void rechazaMasDe254CaracteresAntesDelServicio() throws Exception {
        mvc.perform(post("/api/password/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", "a" + emailDe254()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors", hasItem(containsString("254 caracteres"))));
        verifyNoInteractions(servicio);
    }

    @Test
    void aceptaElLimiteDe254Caracteres() throws Exception {
        mvc.perform(post("/api/password/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", emailDe254()))))
                .andExpect(status().isOk())
                .andExpect(content().string("Si el correo está registrado, recibirás las instrucciones."));
        verify(servicio).solicitarRecuperacion(emailDe254());
    }

    @Test
    void normalizaCorreoYConservaLaRespuestaGenerica() throws Exception {
        mvc.perform(post("/api/password/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", " ANA@EXAMPLE.COM "))))
                .andExpect(status().isOk())
                .andExpect(content().string("Si el correo está registrado, recibirás las instrucciones."));
        verify(servicio).solicitarRecuperacion("ana@example.com");
    }

    @Test
    void rechazaCuerpoAusenteNuloOMalformadoSinInvocarElServicio() throws Exception {
        for (String json : new String[]{"", "null", "{"}) {
            mvc.perform(post("/api/password/solicitar")
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(servicio);
    }

    @Test
    void conservaElContratoDelEndpointDeRestablecimiento() throws Exception {
        doThrow(new IllegalArgumentException("El enlace de recuperación venció."))
                .when(servicio).restablecerPassword("token-simulado", "Clave?123");

        mvc.perform(post("/api/password/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "token", "token-simulado", "password", "Clave?123"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El enlace de recuperación venció."));
        verify(servicio).restablecerPassword("token-simulado", "Clave?123");
    }

    private String emailDe254() {
        return "a@" + "b".repeat(63) + "." + "c".repeat(63)
                + "." + "d".repeat(63) + "." + "e".repeat(56) + ".com";
    }
}
