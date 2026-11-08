package DSII.TurismoSustentable.configurations;

import DSII.TurismoSustentable.models.EstadoBicicleta;
import DSII.TurismoSustentable.models.Genero;
import DSII.TurismoSustentable.models.Pais;
import DSII.TurismoSustentable.models.Rol;
import DSII.TurismoSustentable.models.TipoBicicleta;
import DSII.TurismoSustentable.models.TipoDocumento;
import DSII.TurismoSustentable.repositories.EstadoBicicletaRepository;
import DSII.TurismoSustentable.repositories.GeneroRepository;
import DSII.TurismoSustentable.repositories.PaisRepository;
import DSII.TurismoSustentable.repositories.RolRepository;
import DSII.TurismoSustentable.repositories.TipoBicicletaRepository;
import DSII.TurismoSustentable.repositories.TipoDocumentoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Locale;

@Component
@Profile("refactor")
public class CargaCatalogosRefactor implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final GeneroRepository generoRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PaisRepository paisRepository;
    private final TipoBicicletaRepository tipoBicicletaRepository;
    private final EstadoBicicletaRepository estadoBicicletaRepository;

    public CargaCatalogosRefactor(
            RolRepository rolRepository,
            GeneroRepository generoRepository,
            TipoDocumentoRepository tipoDocumentoRepository,
            PaisRepository paisRepository,
            TipoBicicletaRepository tipoBicicletaRepository,
            EstadoBicicletaRepository estadoBicicletaRepository
    ) {
        this.rolRepository = rolRepository;
        this.generoRepository = generoRepository;
        this.tipoDocumentoRepository = tipoDocumentoRepository;
        this.paisRepository = paisRepository;
        this.tipoBicicletaRepository = tipoBicicletaRepository;
        this.estadoBicicletaRepository = estadoBicicletaRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        // Tipos de bicicleta: valores iniciales para pruebas.
        crearTipoBicicletaSiFalta("MONTANA", "10.00", 3);
        crearTipoBicicletaSiFalta("PASEO", "8.00", 2);
        crearTipoBicicletaSiFalta("ELECTRICA", "15.00", 1);

        // Estados de bicicleta aprobados en el diccionario.
        String[] estadosBicicleta = {
                "DISPONIBLE",
                "RESERVADA",
                "EN_USO",
                "MANTENIMIENTO",
                "INACTIVA",
                "DE_BAJA",
                "EXTRAVIADA"
        };

        for (String codigo : estadosBicicleta) {
            if (estadoBicicletaRepository.findByCodigo(codigo).isEmpty()) {
                estadoBicicletaRepository.save(
                        new EstadoBicicleta(codigo)
                );
            }
        }

        // Roles del sistema.
        String[] roles = {
                "ADMIN",
                "CLIENTE",
                "TECNICO"
        };

        for (String codigo : roles) {
            if (rolRepository.findByCodigo(codigo).isEmpty()) {
                rolRepository.save(new Rol(codigo));
            }
        }

        // Catálogo de géneros.
        String[] generos = {
                "FEMENINO",
                "MASCULINO",
                "NO_BINARIO",
                "OTRO",
                "PREFIERO_NO_DECIR"
        };

        for (String codigo : generos) {
            if (generoRepository.findByCodigo(codigo).isEmpty()) {
                generoRepository.save(new Genero(codigo));
            }
        }

        // Tipos de documento aprobados.
        crearTipoDocumentoSiFalta(
                "DNI",
                "Documento Nacional de Identidad"
        );

        crearTipoDocumentoSiFalta(
                "PASAPORTE",
                "Pasaporte"
        );

        // Países y territorios ISO alpha-2 disponibles en el JDK, en español.
        Locale idioma = new Locale("es", "AR");
        for (String codigo : Locale.getISOCountries()) {
            if (paisRepository.findByCodigo(codigo).isEmpty()) {
                String nombre = new Locale("", codigo).getDisplayCountry(idioma);
                paisRepository.save(new Pais(codigo, nombre));
            }
        }
    }

    private void crearTipoBicicletaSiFalta(
            String codigo,
            String precioPorHoraUsd,
            int intervaloPreventivoMeses
    ) {
        if (tipoBicicletaRepository.findByCodigo(codigo).isEmpty()) {
            tipoBicicletaRepository.save(
                    new TipoBicicleta(
                            codigo,
                            new BigDecimal(precioPorHoraUsd),
                            intervaloPreventivoMeses
                    )
            );
        }
    }

    private void crearTipoDocumentoSiFalta(
            String codigo,
            String nombre
    ) {
        if (tipoDocumentoRepository.findByCodigo(codigo).isEmpty()) {
            tipoDocumentoRepository.save(
                    new TipoDocumento(codigo, nombre)
            );
        }
    }
}
