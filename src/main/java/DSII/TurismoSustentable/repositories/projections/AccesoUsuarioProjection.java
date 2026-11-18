package DSII.TurismoSustentable.repositories.projections;

public interface AccesoUsuarioProjection {

    Integer getUsuarioId();

    String getEmail();

    Boolean getActivo();

    String getRolCodigo();
}
