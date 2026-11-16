package com.tests;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Ejercicio04 extends BaseTest {

    @Test
    void completarCamposDelFormulario() {
        // Paso 1: abrir la página que contiene el formulario.
        page.navigate(BASE_URL + "/index.html");

        // Paso 2: localizar los dos campos por sus etiquetas.
        Locator campoUsuario =
                page.getByLabel("Nombre de usuario (nickname)");

        Locator campoPassword =
                page.getByLabel("Contraseña");

        // Paso 3: completar ambos campos con datos ficticios.
        campoUsuario.fill("usuario_prueba");
        campoPassword.fill("ClavePrueba123!");

        // Paso 4: comprobar los valores ingresados.
        assertThat(campoUsuario).hasValue("usuario_prueba");
        assertThat(campoPassword).hasValue("ClavePrueba123!");

        // Paso 5: leer e imprimir solamente el usuario.
        String usuarioIngresado = campoUsuario.inputValue();
        System.out.println("Usuario ingresado: " + usuarioIngresado);
    }
}