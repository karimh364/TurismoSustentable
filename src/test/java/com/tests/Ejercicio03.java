package com.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio03 extends BaseTest {

    @Test
    void navegarAlRegistroDesdeInicio() {
        // Paso 1: abrir la página inicial.
        page.navigate(BASE_URL + "/index.html");

        // Paso 2: localizar el enlace de registro.
        Locator linkRegistro = page.getByRole(
                AriaRole.LINK,
                new Page.GetByRoleOptions()
                        .setName("Registrarse")
                        .setExact(true)
        );

        // Paso 3: hacer clic en el enlace.
        linkRegistro.click();

        // Paso 4: verificar la dirección de destino.
        assertThat(page)
                .hasURL(BASE_URL + "/registro.html");
    }
}