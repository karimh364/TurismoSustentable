package DSII.TurismoSustentable.services;

import DSII.TurismoSustentable.models.Usuario;
import DSII.TurismoSustentable.models.Persona;
import DSII.TurismoSustentable.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecuperacionPasswordServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CorreoService correoService;

    @InjectMocks
    private RecuperacionPasswordService recuperacionPasswordService;

    @Test
    void solicitaParaCuentaActivaConservandoElEnvioYElTokenProtegido() {
        Persona persona = new Persona();
        persona.setNombre("Ana");
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@example.com");
        usuario.setPersona(persona);
        usuario.setActivo(true);
        ReflectionTestUtils.setField(recuperacionPasswordService, "baseUrl", "https://turismo.example/");
        when(usuarioRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(usuario));

        LocalDateTime antes = LocalDateTime.now();
        recuperacionPasswordService.solicitarRecuperacion(" ANA@EXAMPLE.COM ");
        LocalDateTime despues = LocalDateTime.now();

        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarRecuperacionPassword(eq("ana@example.com"), eq("Ana"), enlace.capture());
        assertTrue(enlace.getValue().startsWith("https://turismo.example/restablecer-password.html?token="));
        String tokenOriginal = enlace.getValue().substring(enlace.getValue().indexOf("?token=") + 7);
        assertTrue(tokenOriginal.matches("[A-Za-z0-9_-]{43}"));
        assertTrue(usuario.getTokenRecuperacion().matches("[a-f0-9]{64}"));
        assertNotEquals(tokenOriginal, usuario.getTokenRecuperacion());
        assertFalse(usuario.getVencimientoTokenRecuperacion().isBefore(antes.plusMinutes(30)));
        assertFalse(usuario.getVencimientoTokenRecuperacion().isAfter(despues.plusMinutes(30)));
        verify(usuarioRepository).saveAndFlush(same(usuario));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void cuentaInexistenteOInactivaNoEnviaCorreoNiGuardaToken() {
        Usuario inactivo = new Usuario();
        inactivo.setActivo(false);
        when(usuarioRepository.findByEmail("ana@example.com"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(inactivo));

        recuperacionPasswordService.solicitarRecuperacion("ana@example.com");
        recuperacionPasswordService.solicitarRecuperacion("ana@example.com");

        assertNull(inactivo.getTokenRecuperacion());
        verify(usuarioRepository, times(2)).findByEmail("ana@example.com");
        verify(usuarioRepository, never()).saveAndFlush(any(Usuario.class));
        verifyNoInteractions(passwordEncoder, correoService);
    }

    @Test
    void rechazaFormatoInvalidoSinConsultarNiConsumirUnToken() {
        String[] passwords = {
                null, "", "Abcd?", "ABCDEF?", "abcdef?", "Abcdeñ",
                "Abc de?", "Abcde?\u00a0", "Abcde?\u2003",
                "Abcde?\u0085", "Abcde?\t", "Abcde?\n",
                "Aa?" + "a".repeat(62),
                "Aa?" + "a".repeat(56) + "\uD83D\uDE00".repeat(3)
        };

        for (String password : passwords) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> recuperacionPasswordService.restablecerPassword(
                            "token-de-prueba", password
                    )
            );
        }

        verifyNoInteractions(
                usuarioRepository, passwordEncoder, correoService
        );
    }

    @Test
    void rechazaPasswordSuperiorA72BytesAntesDeConsultarElToken() {
        String password = "Aa?" + "é".repeat(34) + "aX";
        assertEquals(39, password.length());
        assertEquals(73, password.getBytes(StandardCharsets.UTF_8).length);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> recuperacionPasswordService.restablecerPassword(
                        "token-de-prueba", password
                )
        );

        assertEquals(
                "La contraseña es demasiado larga. Probá con una más corta.",
                error.getMessage()
        );
        verifyNoInteractions(
                usuarioRepository, passwordEncoder, correoService
        );
    }

    @Test
    void aceptaLosNuevosSimbolosYElLimiteSinModificarLaPassword() {
        Usuario usuario = new Usuario();
        usuario.setActivo(true);

        when(usuarioRepository.findByTokenRecuperacion(anyString()))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString()))
                .thenReturn("hash-simulado-para-test");

        String[] passwords = {
                "Abcde?", "Abcde%", "Abcde_", "Abcde+", "Abcde€",
                "Abcde.", "Abcde\\", "Abcde\"", "Abcde\uD83D\uDE00",
                "Aa?" + "é".repeat(34) + "a"
        };

        for (String password : passwords) {
            usuario.setTokenRecuperacion("hash-token-simulado");
            usuario.setVencimientoTokenRecuperacion(
                    LocalDateTime.now().plusMinutes(5)
            );

            recuperacionPasswordService.restablecerPassword(
                    "token-de-prueba", password
            );

            assertEquals("hash-simulado-para-test", usuario.getPassword());
            assertNull(usuario.getTokenRecuperacion());
            assertNull(usuario.getVencimientoTokenRecuperacion());
        }

        ArgumentCaptor<String> passwordCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder, times(passwords.length))
                .encode(passwordCaptor.capture());
        assertEquals(Arrays.asList(passwords), passwordCaptor.getAllValues());
        verify(usuarioRepository, times(passwords.length))
                .saveAndFlush(same(usuario));
        verifyNoInteractions(correoService);
    }
}
