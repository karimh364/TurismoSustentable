package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.configurations.SecurityConfig;
import DSII.TurismoSustentable.configurations.WebAuthorization;
import DSII.TurismoSustentable.models.Genero;
import DSII.TurismoSustentable.models.Pais;
import DSII.TurismoSustentable.models.TipoDocumento;
import DSII.TurismoSustentable.repositories.GeneroRepository;
import DSII.TurismoSustentable.repositories.PaisRepository;
import DSII.TurismoSustentable.repositories.TipoDocumentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CatalogoController.class)
@ContextConfiguration(classes = {
        CatalogoController.class,
        WebAuthorization.class,
        SecurityConfig.class
})
class CatalogoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeneroRepository generoRepository;

    @MockBean
    private TipoDocumentoRepository tipoDocumentoRepository;

    @MockBean
    private PaisRepository paisRepository;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void permiteConsultarGenerosSinIniciarSesion() throws Exception {
        Genero femenino = conId(new Genero("FEMENINO"), 7);
        Genero noBinario = conId(new Genero("NO_BINARIO"), 19);

        when(generoRepository.findAll(Sort.by("codigo")))
                .thenReturn(List.of(femenino, noBinario));

        mockMvc.perform(get("/api/catalogos/generos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].codigo").value("FEMENINO"))
                .andExpect(jsonPath("$[0].nombre").value("Femenino"))
                .andExpect(jsonPath("$[1].id").value(19))
                .andExpect(jsonPath("$[1].codigo").value("NO_BINARIO"))
                .andExpect(jsonPath("$[1].nombre").value("No Binario"));

        verify(generoRepository).findAll(Sort.by("codigo"));

        verifyNoMoreInteractions(
                generoRepository,
                tipoDocumentoRepository,
                paisRepository
        );
    }

    @Test
    void permiteConsultarTiposDocumentoSinIniciarSesion()
            throws Exception {

        TipoDocumento dni = conId(
                new TipoDocumento(
                        "DNI",
                        "Documento Nacional de Identidad"
                ),
                21
        );

        TipoDocumento pasaporte = conId(
                new TipoDocumento("PASAPORTE", "Pasaporte"),
                39
        );

        when(tipoDocumentoRepository.findAll(Sort.by("nombre", "codigo")))
                .thenReturn(List.of(dni, pasaporte));

        mockMvc.perform(get("/api/catalogos/tipos-documento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].codigo").value("DNI"))
                .andExpect(
                        jsonPath("$[0].nombre")
                                .value("Documento Nacional de Identidad")
                )
                .andExpect(jsonPath("$[1].id").value(39))
                .andExpect(jsonPath("$[1].codigo").value("PASAPORTE"))
                .andExpect(jsonPath("$[1].nombre").value("Pasaporte"));

        verify(tipoDocumentoRepository)
                .findAll(Sort.by("nombre", "codigo"));

        verifyNoMoreInteractions(
                generoRepository,
                tipoDocumentoRepository,
                paisRepository
        );
    }

    @Test
    void permiteConsultarPaisesSinIniciarSesion() throws Exception {
        Pais argentina = conId(new Pais("AR", "Argentina"), 42);
        Pais chile = conId(new Pais("CL", "Chile"), 57);

        when(paisRepository.findAll(Sort.by("nombre", "codigo")))
                .thenReturn(List.of(argentina, chile));

        mockMvc.perform(get("/api/catalogos/paises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[0].codigo").value("AR"))
                .andExpect(jsonPath("$[0].nombre").value("Argentina"))
                .andExpect(jsonPath("$[1].id").value(57))
                .andExpect(jsonPath("$[1].codigo").value("CL"))
                .andExpect(jsonPath("$[1].nombre").value("Chile"));

        verify(paisRepository).findAll(Sort.by("nombre", "codigo"));

        verifyNoMoreInteractions(
                generoRepository,
                tipoDocumentoRepository,
                paisRepository
        );
    }

    @Test
    void noAbreOtrosCatalogosNiEscriturasSinSesion() throws Exception {
        mockMvc.perform(get("/api/catalogos/roles"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/catalogos/generos"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                generoRepository,
                tipoDocumentoRepository,
                paisRepository
        );
    }

    @Test
    @WithMockUser(authorities = "CLIENTE")
    void clienteNoPuedeModificarCatalogos() throws Exception {
        mockMvc.perform(post("/api/catalogos/generos"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/catalogos/paises"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/catalogos/tipos-documento"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                generoRepository,
                tipoDocumentoRepository,
                paisRepository
        );
    }

    private <T> T conId(T entidad, int id) {
        ReflectionTestUtils.setField(entidad, "id", id);
        return entidad;
    }
}
