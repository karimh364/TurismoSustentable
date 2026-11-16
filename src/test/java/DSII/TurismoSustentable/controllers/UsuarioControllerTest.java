package DSII.TurismoSustentable.controllers;

import DSII.TurismoSustentable.dto.InactivarUsuarioDTO;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import DSII.TurismoSustentable.services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

// Las reglas de baja viven en el servicio transaccional; aquí se prueba delegación/HTTP.
@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {
    @Mock UsuarioRepository usuarioRepository;
    @Mock CorreoService correoService;
    @Mock UsuarioService usuarioService;
    @Mock RegistroUsuarioService registroUsuarioService;
    @InjectMocks UsuarioController controller;

    @Test void delegaBajaSinBorrarDirectamenteDesdeElController() {
        assertEquals(HttpStatus.NO_CONTENT,controller.eliminarUsuarioLogico(17,null).getStatusCode());
        verify(usuarioService).inactivarUsuario(17,null);
        verifyNoInteractions(usuarioRepository);
    }
    @Test void transmiteMotivoDePausaDelTecnico() {
        InactivarUsuarioDTO d=new InactivarUsuarioDTO(); d.setMotivo(" Ausencia administrativa ");
        assertEquals(HttpStatus.NO_CONTENT,controller.eliminarUsuarioLogico(17,d).getStatusCode());
        verify(usuarioService).inactivarUsuario(17,"Ausencia administrativa");
        verifyNoInteractions(usuarioRepository);
    }
    @ParameterizedTest @CsvSource({"400,Ya inactivo","404,No existe","409,Recargá los datos"})
    void conservaErroresDelServicio(int codigo,String mensaje) {
        doThrow(new ResponseStatusException(HttpStatus.valueOf(codigo),mensaje)).when(usuarioService).inactivarUsuario(17,null);
        var r=controller.eliminarUsuarioLogico(17,null);
        assertEquals(codigo,r.getStatusCodeValue()); assertEquals(mensaje,r.getBody());
        verifyNoInteractions(usuarioRepository);
    }
    @Test void noOcultaProteccionDeCuentasAdmin() {
        doThrow(new AccessDeniedException("No se puede eliminar a un usuario Administrador.")).when(usuarioService).inactivarUsuario(17,null);
        assertEquals(HttpStatus.FORBIDDEN,controller.eliminarUsuarioLogico(17,null).getStatusCode());
        verifyNoInteractions(usuarioRepository);
    }
}
