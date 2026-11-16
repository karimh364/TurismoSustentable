package com.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio07 extends BaseTest {

    @Test
    void capturarInicioYLoginExitoso() {
        // Datos de la cuenta de prueba válida.
        String usuario = "parcial_playwright";
        String password = "Prueba123*";

        // Abrir y verificar la página inicial.
        page.navigate(BASE_URL + "/index.html");
        assertThat(page)
                .hasTitle("Turismo Sustentable - ¡Pedalea y Explora!");

        // Esperar que el formulario esté disponible para ingresar.
        Locator botonLogin = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName("Acceder")
                        .setExact(true)
        );

        assertThat(botonLogin).isEnabled();

        // Guardar la primera captura: página inicial completa.
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("parcial-ej07-inicio.png"))
                .setFullPage(true));

        System.out.println(
                "Captura guardada: parcial-ej07-inicio.png"
        );

        // Completar las credenciales y enviar el formulario.
        page.getByLabel("Nombre de usuario (nickname)").fill(usuario);
        page.getByLabel("Contraseña").fill(password);
        botonLogin.click();

        // Verificar la llegada a la página posterior al login.
        assertThat(page).hasURL(BASE_URL + "/alquiler.html");

        Locator tituloInventario = page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName("Elegí tu compañera de ruta")
                        .setExact(true)
        );

        assertThat(tituloInventario).isVisible();

        // Guardar la segunda captura: login exitoso.
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("parcial-ej07-login-exitoso.png"))
                .setFullPage(true));

        System.out.println(
                "Captura guardada: parcial-ej07-login-exitoso.png"
        );
    }

    @Test
    void capturarLoginFallido() {
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

        // Verificar el rechazo y la permanencia en el formulario.
        Locator mensajeError = page.getByRole(AriaRole.ALERT);

        assertThat(mensajeError)
                .containsText("Nombre de usuario o contraseña incorrectos.");

        assertThat(mensajeError).isVisible();
        assertThat(page).hasURL(BASE_URL + "/index.html");

        // Volver al inicio para ubicar correctamente la barra fija.
        page.evaluate(
                "window.scrollTo({top: 0, left: 0, behavior: 'instant'})"
        );
        page.waitForFunction("window.scrollY === 0");

        // Guardar la tercer captura completa con el mensaje de error.
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("parcial-ej07-login-error.png"))
                .setFullPage(true));

        System.out.println(
                "Captura guardada: parcial-ej07-login-error.png"
        );
    }
}