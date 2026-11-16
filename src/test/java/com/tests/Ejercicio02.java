package com.tests;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio02 extends BaseTest {

    @Test
    void verificarTituloExacto() {
        // Paso 1: abrir la página inicial.
        page.navigate(BASE_URL + "/index.html");

        // Paso 2: verificar el título completo.
        assertThat(page)
                .hasTitle("Turismo Sustentable - ¡Pedalea y Explora!");
    }

    @Test
    void verificarParteDelTituloConExpresionRegular() {
        // Paso 1: abrir la página inicial.
        page.navigate(BASE_URL + "/index.html");

        // Paso 2: verificar una parte del título.
        assertThat(page)
                .hasTitle(Pattern.compile(".*Turismo Sustentable.*"));
    }
}