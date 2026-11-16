package DSII.TurismoSustentable.dto;
import DSII.TurismoSustentable.models.Usuario;
public class TecnicoMantenimientoDTO {
    public final int id; public final String nombre;
    public TecnicoMantenimientoDTO(Usuario u) {id=u.getId(); nombre=u.getPersona().getNombre()+" "+u.getPersona().getApellido()+" ("+u.getNickname()+")";}
}
