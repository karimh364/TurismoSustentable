package com.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio08 extends BaseTest {

    @Test
    void navegarEntrePaginasVolverYRecargar() {
        // Paso 1: abrir el inicio y verificar su título.
        page.navigate(BASE_URL + "/index.html");

        assertThat(page)
                .hasTitle("Turismo Sustentable - ¡Pedalea y Explora!");

        // Paso 2: navegar mediante el enlace a la página A.
        Locator linkRegistro = page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("Registrarse")
                        .setExact(true)
        );

        linkRegistro.click();

        assertThat(page).hasURL(BASE_URL + "/registro.html");

        // Paso 3: volver atrás y verificar el regreso al inicio.
        page.goBack();

        assertThat(page).hasURL(BASE_URL + "/index.html");

        // Paso 4: navegar mediante el enlace a la página B.
        Locator linkRecuperarPassword = page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("¿Olvidaste tu contraseña?")
                        .setExact(true)
        );

        linkRecuperarPassword.click();

        assertThat(page)
                .hasURL(BASE_URL + "/recuperar-password.html");

        // Paso 5: recargar y verificar que seguimos en la página B.
        page.reload();

        assertThat(page)
                .hasURL(BASE_URL + "/recuperar-password.html");
    }
}