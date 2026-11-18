package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.dto.AuditoriaAdministradorDTO;
import DSII.TurismoSustentable.dto.AuditoriaDetalleDTO;
import DSII.TurismoSustentable.dto.AuditoriaResumenDTO;
import DSII.TurismoSustentable.repositories.AuditoriaSistemaRepository;
import DSII.TurismoSustentable.repositories.AuditoriaSistemaRepository.DetalleRegistrado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class ConsultaAuditoriaService {
    private final AuditoriaSistemaRepository repositorio;
    private final CambiosAuditoria cambiosAuditoria;

    public ConsultaAuditoriaService(AuditoriaSistemaRepository repositorio, ObjectMapper json) {
        this.repositorio = repositorio;
        this.cambiosAuditoria = new CambiosAuditoria(json);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public Page<AuditoriaResumenDTO> listar(String entidad, String accion, Integer administradorId,
                                           int pagina, int tamano) {
        if (pagina < 0 || pagina > 10_000 || tamano < 1 || tamano > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La página debe estar entre 0 y 10000 y el tamaño entre 1 y 100.");
        }
        if (administradorId != null && administradorId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del administrador no es válido.");
        }

        return repositorio.consultarResumen(
                normalizar(entidad, 80, "entidad"),
                normalizar(accion, 50, "acción"),
                administradorId,
                PageRequest.of(pagina, tamano)
        );
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public List<AuditoriaAdministradorDTO> administradores() {
        return repositorio.consultarAdministradores();
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @Transactional(readOnly = true)
    public AuditoriaDetalleDTO detalle(int id) {
        if (id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del evento no es válido.");
        }
        DetalleRegistrado registro = repositorio.consultarDetalle(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "El evento de auditoría no existe."));
        AuditoriaResumenDTO evento = new AuditoriaResumenDTO(registro.getId(), registro.getAdministradorId(),
                registro.getAdministradorNickname(), registro.getEntidadAfectada(), registro.getEntidadId(),
                registro.getAccion(), registro.getFechaOperacion(), registro.getReferenciaEntidad());
        return new AuditoriaDetalleDTO(evento, registro.getMotivoRegistrado(), cambiosAuditoria.interpretar(
                registro.getEntidadAfectada(), registro.getAccion(),
                registro.getValoresAnteriores(), registro.getValoresNuevos()));
    }

    private String normalizar(String entrada, int maximo, String nombre) {
        if (entrada == null || entrada.isBlank()) return null;

        String valor = entrada.strip().toUpperCase(Locale.ROOT);
        if (valor.length() > maximo || !valor.matches("[A-Z][A-Z0-9_]*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El filtro de " + nombre + " no es válido.");
        }
        return valor;
    }
}
