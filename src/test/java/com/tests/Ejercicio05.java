package com.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio05 extends BaseTest {

    @Test
    void loginExitoso() {
        // Datos válidos de la cuenta exclusiva de prueba.
        String usuario = "parcial_playwright";
        String password = "Prueba123*";

        // Paso 1: abrir la página del formulario.
        page.navigate(BASE_URL + "/index.html");

        // Paso 2: localizar los campos y el botón de envío.
        Locator campoUsuario =
                page.getByLabel("Nombre de usuario (nickname)");

        Locator campoPassword =
                page.getByLabel("Contraseña");

        Locator botonLogin = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Acceder")
                        .setExact(true)
        );

        // Paso 3: completar las credenciales válidas.
        campoUsuario.fill(usuario);
        campoPassword.fill(password);

        // Paso 4: enviar el formulario.
        botonLogin.click();

        // Paso 5: verificar la página de acceso exitoso.
        assertThat(page)
                .hasURL(BASE_URL + "/alquiler.html");
    }

    @Test
    void loginFallido() {
        // Mismo usuario válido, pero usaremos una contraseña incorrecta.
        String usuario = "parcial_playwright";

        // Paso 1: abrir la página del formulario de forma independiente.
        page.navigate(BASE_URL + "/index.html");

        // Paso 2: localizar los campos y el botón de envío.
        Locator campoUsuario =
                page.getByLabel("Nombre de usuario (nickname)");

        Locator campoPassword =
                page.getByLabel("Contraseña");

        Locator botonLogin = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Acceder")
                        .setExact(true)
        );

        // Paso 3: completar una combinación de credenciales inválida.
        campoUsuario.fill(usuario);
        campoPassword.fill("ClaveIncorrectaParaPrueba!");

        // Paso 4: enviar y esperar el rechazo real del intento.
        botonLogin.click();

        Locator mensajeEstado = page.getByRole(AriaRole.ALERT);

        assertThat(mensajeEstado)
                .containsText("Nombre de usuario o contraseña incorrectos.");

        // Paso 5: verificar que permanece en la página del formulario.
        assertThat(page)
                .hasURL(BASE_URL + "/index.html");
    }
}