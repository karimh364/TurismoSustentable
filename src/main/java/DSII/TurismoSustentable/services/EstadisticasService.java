package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.repositories.AlquilerDetalleRepository;
import DSII.TurismoSustentable.repositories.BicicletaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstadisticasService {

    @Autowired
    private AlquilerDetalleRepository alquilerDetalleRepository;

    @Autowired
    private BicicletaRepository bicicletaRepository;

    public List<Object[]> getPreferenciasBicicletaPorGenero() {
        return alquilerDetalleRepository.contarPreferenciasBicicletaPorGenero();
    }

    public Long getBicicletasAlquiladasActuales() {
        return alquilerDetalleRepository.contarBicicletasAlquiladasActuales();
    }

    public List<Object[]> getBicicletasPorEstado() {
        return bicicletaRepository.contarBicicletasPorEstado();
    }
}