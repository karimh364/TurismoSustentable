package com.tests;

import org.junit.jupiter.api.Test;

public class Ejercicio01 extends BaseTest {

    @Test
    void abrirSitioTurismoSustentable() {
        // Paso 1: navegar al inicio del proyecto.
        page.navigate(BASE_URL + "/index.html");

        // Paso 2: obtener e imprimir el título real.
        System.out.println(page.title());
    }
}