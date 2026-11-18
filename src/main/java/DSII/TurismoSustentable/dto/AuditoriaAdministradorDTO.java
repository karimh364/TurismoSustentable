package DSII.TurismoSustentable.dto;

/** Opción del filtro de auditoría: solo ID y nickname del actor. */
public final class AuditoriaAdministradorDTO {
    public final Integer administradorId;
    public final String administradorNickname;

    public AuditoriaAdministradorDTO(Integer administradorId, String administradorNickname) {
        this.administradorId = administradorId;
        this.administradorNickname = administradorNickname;
    }
}
