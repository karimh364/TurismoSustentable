package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.ActualizarEstacionDTO;
import DSII.TurismoSustentable.dto.CrearEstacionDTO;
import DSII.TurismoSustentable.dto.EstacionDTO;
import DSII.TurismoSustentable.models.Estacion;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import DSII.TurismoSustentable.repositories.EstacionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.persistence.EntityNotFoundException;
import javax.persistence.OptimisticLockException;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Validated
public class EstacionService {

    private final EstacionRepository estacionRepository;
    private final BicicletaRepository bicicletaRepository;

    public EstacionService(
            EstacionRepository estacionRepository,
            BicicletaRepository bicicletaRepository
    ) {
        this.estacionRepository = estacionRepository;
        this.bicicletaRepository = bicicletaRepository;
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public EstacionDTO crearEstacion(
            @NotNull(message = "Los datos de creación son obligatorios.")
            @Valid CrearEstacionDTO datos
    ) {

        if (estacionRepository.existsByNombreIgnoreCase(
                datos.getNombre()
        )) {
            throw new IllegalStateException(
                    "Ya existe una estación con ese nombre."
            );
        }

        Estacion nuevaEstacion = new Estacion(
                datos.getNombre(),
                datos.getDireccion(),
                datos.getCapacidad(),
                datos.getLatitud(),
                datos.getLongitud()
        );

        nuevaEstacion.setEsPublica(datos.getEsPublica());

        Estacion estacionGuardada =
                estacionRepository.saveAndFlush(nuevaEstacion);

        return new EstacionDTO(estacionGuardada);
    }

    @Transactional(readOnly = true)
    public List<EstacionDTO> obtenerEstacionesPublicas() {
        return estacionRepository
                .findByBorradoEstacionFalseAndEsPublicaTrue()
                .stream()
                .map(EstacionDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ADMIN')")
    public List<EstacionDTO> obtenerEstacionesAdministracion() {
        return estacionRepository
                .findByBorradoEstacionFalse()
                .stream()
                .map(EstacionDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    @PreAuthorize("hasAuthority('ADMIN')")
    public EstacionDTO actualizarEstacion(
            @NotNull(message = "El identificador de la estación es obligatorio.")
            @Positive(message = "El identificador de la estación debe ser positivo.")
            Integer id,

            @NotNull(message = "Los datos de actualización son obligatorios.")
            @Valid ActualizarEstacionDTO datos
    ) {

        // Coordinar la edición con ingresos y devoluciones.
        Estacion estacion = estacionRepository
                .buscarActivaPorIdConBloqueo(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "La estación no existe o está inactiva."
                ));

        // Detectar un formulario cargado antes de otra modificación.
        if (!Objects.equals(
                estacion.getVersion(),
                datos.getVersion()
        )) {
            throw new OptimisticLockException(
                    "La estación fue modificada desde que abriste el formulario. " +
                            "Recargá los datos antes de guardar."
            );
        }

        if (estacionRepository.existsByNombreIgnoreCaseAndIdNot(
                datos.getNombre(),
                id
        )) {
            throw new IllegalStateException(
                    "Ya existe otra estación con ese nombre."
            );
        }

        long bicicletasFisicas = bicicletaRepository
                .countByUbicacionActual(estacion);

        if (datos.getCapacidad() < bicicletasFisicas) {
            throw new IllegalStateException(
                    "La capacidad no puede ser menor que las " +
                            bicicletasFisicas +
                            " bicicletas que permanecen en la estación."
            );
        }

        // Modificar únicamente los campos permitidos.
        estacion.setNombre(datos.getNombre());
        estacion.setDireccion(datos.getDireccion());
        estacion.setCapacidad(datos.getCapacidad());
        estacion.setLatitud(datos.getLatitud());
        estacion.setLongitud(datos.getLongitud());

        // Hibernate persiste los cambios y actualiza la versión.
        estacionRepository.flush();

        return new EstacionDTO(estacion);
    }
}
