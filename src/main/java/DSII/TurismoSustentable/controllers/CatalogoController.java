package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.CatalogoDTO;
import DSII.TurismoSustentable.repositories.GeneroRepository;
import DSII.TurismoSustentable.repositories.PaisRepository;
import DSII.TurismoSustentable.repositories.TipoDocumentoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    private static final Map<String, String> NOMBRES_GENERO = Map.of(
            "FEMENINO", "Femenino",
            "MASCULINO", "Masculino",
            "NO_BINARIO", "No Binario",
            "OTRO", "Otro",
            "PREFIERO_NO_DECIR", "Prefiero no decir"
    );

    private final GeneroRepository generoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PaisRepository paisRepository;

    public CatalogoController(
            GeneroRepository generoRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            PaisRepository paisRepository
    ) {
        this.generoRepository = generoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.paisRepository = paisRepository;
    }

    @GetMapping("/generos")
    public List<CatalogoDTO> getGeneros() {
        return generoRepository.findAll(Sort.by("codigo"))
                .stream()
                .map(genero -> new CatalogoDTO(
                        genero.getId(),
                        genero.getCodigo(),
                        NOMBRES_GENERO.getOrDefault(
                                genero.getCodigo(),
                                genero.getCodigo()
                        )
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/tipos-documento")
    public List<CatalogoDTO> getTiposDocumento() {
        return tipoDocumentoRepository
                .findAll(Sort.by("nombre", "codigo"))
                .stream()
                .map(tipo -> new CatalogoDTO(
                        tipo.getId(),
                        tipo.getCodigo(),
                        tipo.getNombre()
                ))
                .collect(Collectors.toList());
    }

    @GetMapping("/paises")
    public List<CatalogoDTO> getPaises() {
        return paisRepository.findAll(Sort.by("nombre", "codigo"))
                .stream()
                .map(pais -> new CatalogoDTO(
                        pais.getId(),
                        pais.getCodigo(),
                        pais.getNombre()
                ))
                .collect(Collectors.toList());
    }
}
