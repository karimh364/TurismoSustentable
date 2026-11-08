package DSII.TurismoSustentable.configurations;

import DSII.TurismoSustentable.models.*;
import DSII.TurismoSustentable.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import java.math.BigDecimal;
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
    @Mock private TipoBicicletaRepository tipoBicicletaRepository;
    @Mock private EstadoBicicletaRepository estadoBicicletaRepository;
    @Mock private EstadoAlquilerDetalleRepository estadoAlquilerDetalleRepository;
    @Mock private EstadoAlquilerRepository estadoAlquilerRepository;
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

    @Test
    void cargaTiposConLosValoresAprobados() {
        cargador.run();

        ArgumentCaptor<TipoBicicleta> captor =
                ArgumentCaptor.forClass(TipoBicicleta.class);

        verify(tipoBicicletaRepository, times(3))
                .save(captor.capture());

        Map<String, TipoBicicleta> tipos = new HashMap<>();
        captor.getAllValues().forEach(
                tipo -> tipos.put(tipo.getCodigo(), tipo)
        );

        assertEquals(
                Set.of("MONTANA", "PASEO", "ELECTRICA"),
                tipos.keySet()
        );

        assertEquals(
                new BigDecimal("10.00"),
                tipos.get("MONTANA").getPrecioPorHoraUsd()
        );
        assertEquals(
                new BigDecimal("8.00"),
                tipos.get("PASEO").getPrecioPorHoraUsd()
        );
        assertEquals(
                new BigDecimal("15.00"),
                tipos.get("ELECTRICA").getPrecioPorHoraUsd()
        );

        assertEquals(
                Integer.valueOf(3),
                tipos.get("MONTANA").getIntervaloPreventivoMeses()
        );
        assertEquals(
                Integer.valueOf(2),
                tipos.get("PASEO").getIntervaloPreventivoMeses()
        );
        assertEquals(
                Integer.valueOf(1),
                tipos.get("ELECTRICA").getIntervaloPreventivoMeses()
        );
    }

    @Test
    void repetirLaCargaNoDuplicaNiSobrescribeTiposExistentes() {
        Map<String, TipoBicicleta> tipos = new HashMap<>();

        // Simula valores que ya fueron modificados después de la primera carga.
        TipoBicicleta montanaExistente = new TipoBicicleta(
                "MONTANA",
                new BigDecimal("11.50"),
                4
        );
        tipos.put("MONTANA", montanaExistente);

        when(tipoBicicletaRepository.findByCodigo(anyString()))
                .thenAnswer(invocacion -> Optional.ofNullable(
                        tipos.get(invocacion.getArgument(0))
                ));

        when(tipoBicicletaRepository.save(any(TipoBicicleta.class)))
                .thenAnswer(invocacion -> {
                    TipoBicicleta tipo = invocacion.getArgument(0);
                    tipos.put(tipo.getCodigo(), tipo);
                    return tipo;
                });

        cargador.run();
        cargador.run();

        assertEquals(3, tipos.size());
        assertSame(montanaExistente, tipos.get("MONTANA"));
        assertEquals(
                new BigDecimal("11.50"),
                montanaExistente.getPrecioPorHoraUsd()
        );
        assertEquals(
                Integer.valueOf(4),
                montanaExistente.getIntervaloPreventivoMeses()
        );

        // Solo deben guardarse PASEO y ELECTRICA, una vez cada uno.
        verify(tipoBicicletaRepository, times(2))
                .save(any(TipoBicicleta.class));

        verify(tipoBicicletaRepository, never())
                .delete(any(TipoBicicleta.class));
    }

    @Test
    void cargaSoloEstadosAprobadosYValidos() {
        cargador.run();

        ArgumentCaptor<EstadoBicicleta> captor =
                ArgumentCaptor.forClass(EstadoBicicleta.class);

        verify(estadoBicicletaRepository, times(7))
                .save(captor.capture());

        Set<String> codigos = new HashSet<>();

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            for (EstadoBicicleta estado : captor.getAllValues()) {
                codigos.add(estado.getCodigo());
                assertTrue(
                        factory.getValidator().validate(estado).isEmpty()
                );
            }

            EstadoBicicleta normalizado =
                    new EstadoBicicleta(" disponible ");

            assertEquals("DISPONIBLE", normalizado.getCodigo());
            assertTrue(
                    factory.getValidator().validate(normalizado).isEmpty()
            );

            String[] invalidos = {
                    null, "", "   ", "INVENTADO",
                    "ALQUILADA", "X".repeat(21)
            };

            for (String codigo : invalidos) {
                assertFalse(
                        factory.getValidator()
                                .validate(new EstadoBicicleta(codigo))
                                .isEmpty(),
                        "Debería rechazar el código: " + codigo
                );
            }
        }

        assertEquals(
                Set.of(
                        "DISPONIBLE", "RESERVADA", "EN_USO",
                        "MANTENIMIENTO", "INACTIVA",
                        "DE_BAJA", "EXTRAVIADA"
                ),
                codigos
        );
    }

    @Test
    void repetirLaCargaNoDuplicaNiReemplazaEstadosExistentes() {
        Map<String, EstadoBicicleta> estados = new HashMap<>();

        EstadoBicicleta disponible =
                new EstadoBicicleta("DISPONIBLE");

        estados.put("DISPONIBLE", disponible);

        when(estadoBicicletaRepository.findByCodigo(anyString()))
                .thenAnswer(invocacion -> Optional.ofNullable(
                        estados.get(invocacion.getArgument(0))
                ));

        when(estadoBicicletaRepository.save(any(EstadoBicicleta.class)))
                .thenAnswer(invocacion -> {
                    EstadoBicicleta estado = invocacion.getArgument(0);
                    estados.put(estado.getCodigo(), estado);
                    return estado;
                });

        cargador.run();
        cargador.run();

        assertEquals(7, estados.size());
        assertSame(disponible, estados.get("DISPONIBLE"));

        // DISPONIBLE ya existía: solo se guardan los otros seis.
        verify(estadoBicicletaRepository, times(6))
                .save(any(EstadoBicicleta.class));

        verify(estadoBicicletaRepository, never())
                .delete(any(EstadoBicicleta.class));
    }

    @Test
    void cargaEstadosDetalleAprobadosYValidos() {
        cargador.run();

        ArgumentCaptor<EstadoAlquilerDetalle> captor =
                ArgumentCaptor.forClass(EstadoAlquilerDetalle.class);

        verify(estadoAlquilerDetalleRepository, times(7))
                .save(captor.capture());

        Set<String> codigos = new HashSet<>();

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            for (EstadoAlquilerDetalle estado : captor.getAllValues()) {
                codigos.add(estado.getCodigo());
                assertTrue(
                        factory.getValidator().validate(estado).isEmpty()
                );
            }

            EstadoAlquilerDetalle normalizado =
                    new EstadoAlquilerDetalle(" pendiente_retiro ");

            assertEquals("PENDIENTE_RETIRO", normalizado.getCodigo());
            assertTrue(
                    factory.getValidator().validate(normalizado).isEmpty()
            );

            String[] invalidos = {
                    null, "", "   ", "INVENTADO",
                    "EN_PROGRESO", "DEVUELTA_OK", "X".repeat(31)
            };

            for (String codigo : invalidos) {
                assertFalse(
                        factory.getValidator()
                                .validate(new EstadoAlquilerDetalle(codigo))
                                .isEmpty(),
                        "Debería rechazar el código: " + codigo
                );
            }
        }

        assertEquals(
                Set.of(
                        "RESERVADO", "PENDIENTE_RETIRO", "EN_CURSO",
                        "DEVUELTO", "CANCELADO", "VENCIDO",
                        "CERRADO_POR_INCIDENCIA"
                ),
                codigos
        );
    }

    @Test
    void repetirLaCargaNoDuplicaNiReemplazaEstadosDetalleExistentes() {
        Map<String, EstadoAlquilerDetalle> estados = new HashMap<>();

        EstadoAlquilerDetalle reservado =
                new EstadoAlquilerDetalle("RESERVADO");

        estados.put("RESERVADO", reservado);

        when(estadoAlquilerDetalleRepository.findByCodigo(anyString()))
                .thenAnswer(invocacion -> Optional.ofNullable(
                        estados.get(invocacion.getArgument(0))
                ));

        when(estadoAlquilerDetalleRepository.save(
                any(EstadoAlquilerDetalle.class)
        )).thenAnswer(invocacion -> {
            EstadoAlquilerDetalle estado = invocacion.getArgument(0);
            estados.put(estado.getCodigo(), estado);
            return estado;
        });

        cargador.run();
        cargador.run();

        assertEquals(7, estados.size());
        assertSame(reservado, estados.get("RESERVADO"));

        // RESERVADO ya existía: solo se guardan los otros seis.
        verify(estadoAlquilerDetalleRepository, times(6))
                .save(any(EstadoAlquilerDetalle.class));

        verify(estadoAlquilerDetalleRepository, never())
                .delete(any(EstadoAlquilerDetalle.class));
    }

    @Test
    void cargaEstadosCabeceraAprobadosYValidos() {
        cargador.run();

        ArgumentCaptor<EstadoAlquiler> captor =
                ArgumentCaptor.forClass(EstadoAlquiler.class);

        verify(estadoAlquilerRepository, times(5))
                .save(captor.capture());

        Set<String> codigos = new HashSet<>();

        try (ValidatorFactory factory =
                     Validation.buildDefaultValidatorFactory()) {

            for (EstadoAlquiler estado : captor.getAllValues()) {
                codigos.add(estado.getCodigo());
                assertTrue(
                        factory.getValidator().validate(estado).isEmpty()
                );
            }

            EstadoAlquiler normalizado =
                    new EstadoAlquiler(" en_progreso ");

            assertEquals("EN_PROGRESO", normalizado.getCodigo());
            assertTrue(
                    factory.getValidator().validate(normalizado).isEmpty()
            );

            String[] invalidos = {
                    null, "", "   ", "INVENTADO",
                    "INICIADO", "RECHAZADO", "EN_CURSO", "X".repeat(21)
            };

            for (String codigo : invalidos) {
                assertFalse(
                        factory.getValidator()
                                .validate(new EstadoAlquiler(codigo))
                                .isEmpty(),
                        "Debería rechazar el código: " + codigo
                );
            }
        }

        assertEquals(
                Set.of(
                        "RESERVADO", "EN_PROGRESO", "FINALIZADO",
                        "CANCELADO", "VENCIDO"
                ),
                codigos
        );
    }

    @Test
    void repetirLaCargaNoDuplicaNiReemplazaEstadosCabeceraExistentes() {
        Map<String, EstadoAlquiler> estados = new HashMap<>();

        EstadoAlquiler reservado = new EstadoAlquiler("RESERVADO");
        estados.put("RESERVADO", reservado);

        when(estadoAlquilerRepository.findByCodigo(anyString()))
                .thenAnswer(invocacion -> Optional.ofNullable(
                        estados.get(invocacion.getArgument(0))
                ));

        when(estadoAlquilerRepository.save(any(EstadoAlquiler.class)))
                .thenAnswer(invocacion -> {
                    EstadoAlquiler estado = invocacion.getArgument(0);
                    estados.put(estado.getCodigo(), estado);
                    return estado;
                });

        cargador.run();
        cargador.run();

        assertEquals(5, estados.size());
        assertSame(reservado, estados.get("RESERVADO"));

        // RESERVADO ya existía: solo se guardan los otros cuatro.
        verify(estadoAlquilerRepository, times(4))
                .save(any(EstadoAlquiler.class));

        verify(estadoAlquilerRepository, never())
                .delete(any(EstadoAlquiler.class));
    }
}
