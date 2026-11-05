package DSII.TurismoSustentable.configurations;

import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CargaCatalogosRefactorTest {

    @Mock private RolRepository rolRepository;
    @Mock private GeneroRepository generoRepository;
    @Mock private TipoDocumentoRepository tipoDocumentoRepository;
    @Mock private PaisRepository paisRepository;
    @InjectMocks private CargaCatalogosRefactor cargador;

    private final Map<String, Pais> paises = new HashMap<>();

    @BeforeEach
    void prepararRepositoriosEnMemoria() {
        when(rolRepository.findByCodigo(anyString()))
                .thenAnswer(i -> Optional.of(new Rol(i.getArgument(0))));
        when(generoRepository.findByCodigo(anyString()))
                .thenAnswer(i -> Optional.of(new Genero(i.getArgument(0))));
        when(tipoDocumentoRepository.findByCodigo(anyString()))
                .thenAnswer(i -> Optional.of(new TipoDocumento(i.getArgument(0), "Existente")));
        when(paisRepository.findByCodigo(anyString()))
                .thenAnswer(i -> Optional.ofNullable(paises.get(i.getArgument(0))));
        when(paisRepository.save(any(Pais.class))).thenAnswer(i -> {
            Pais pais = i.getArgument(0);
            paises.put(pais.getCodigo(), pais);
            return pais;
        });
    }

    @Test
    void cargaLosCodigosIsoConNombresEnEspanolYCamposValidos() {
        cargador.run();

        assertEquals(new HashSet<>(Arrays.asList(Locale.getISOCountries())), paises.keySet());
        assertEquals("Argentina", paises.get("AR").getNombre());
        assertEquals("Chile", paises.get("CL").getNombre());
        assertEquals("España", paises.get("ES").getNombre());
        assertArrayEquals(new String[]{"refactor"},
                CargaCatalogosRefactor.class.getAnnotation(Profile.class).value());

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            paises.values().forEach(pais ->
                    assertTrue(factory.getValidator().validate(pais).isEmpty()));
        }
    }

    @Test
    void repetirLaCargaNoDuplicaNiSobrescribePaisesExistentes() {
        Pais argentina = new Pais("AR", "Argentina existente");
        paises.put("AR", argentina);

        cargador.run();
        cargador.run();

        assertEquals(Locale.getISOCountries().length, paises.size());
        assertSame(argentina, paises.get("AR"));
        assertEquals("Argentina existente", paises.get("AR").getNombre());
        verify(paisRepository, times(Locale.getISOCountries().length - 1))
                .save(any(Pais.class));
        verify(paisRepository, never()).delete(any(Pais.class));
    }
}
