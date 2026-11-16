package com.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio06 extends BaseTest {

    private Locator mensajeEstado;

    @BeforeEach
    void prepararLocalizadorDelMensaje() {
        mensajeEstado = page.getByRole(AriaRole.ALERT);
    }

    @Test
    void verificarMensajeDeLoginExitoso() {
        // Datos de la cuenta de prueba válida.
        String usuario = "parcial_playwright";
        String password = "Prueba123*";

        // Abrir el formulario de inicio de sesión.
        page.navigate(BASE_URL + "/index.html");

        // Completar las credenciales válidas.
        page.getByLabel("Nombre de usuario (nickname)").fill(usuario);
        page.getByLabel("Contraseña").fill(password);

        // Enviar el formulario.
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Acceder")
                        .setExact(true)
        ).click();

        // Esperar el mensaje real de éxito.
        assertThat(mensajeEstado)
                .containsText("¡Acceso concedido!");

        // Comprobar que el contenedor sea visible.
        assertThat(mensajeEstado).isVisible();
    }

    @Test
    void verificarMensajeDeLoginFallido() {
        // Mismo usuario válido, con una contraseña incorrecta.
        String usuario = "parcial_playwright";
        String password = "ClaveIncorrectaParaPrueba!";

        // Abrir el formulario en una sesión independiente.
        page.navigate(BASE_URL + "/index.html");

        // Completar las credenciales del caso fallido.
        page.getByLabel("Nombre de usuario (nickname)").fill(usuario);
        page.getByLabel("Contraseña").fill(password);

        // Enviar el formulario.
        page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Acceder")
                        .setExact(true)
        ).click();

        // Esperar el mensaje real de rechazo.
        assertThat(mensajeEstado)
                .containsText("Nombre de usuario o contraseña incorrectos.");

        // Comprobar que el contenedor sea visible.
        assertThat(mensajeEstado).isVisible();
    }
}