package com.tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BaseTest {

    protected static final String BASE_URL = "http://localhost:8080";

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();

        browser = playwright.chromium()
                .launch(new BrowserType.LaunchOptions()
                        .setHeadless(false));

        context = browser.newContext();
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
        page = context.newPage();
    }

    @AfterEach
    void tearDown(TestInfo testInfo) throws IOException {
        try {
            if (context != null) {
                Path carpetaTraces = Paths.get("traces");
                Files.createDirectories(carpetaTraces);

                String nombreArchivo = getClass().getSimpleName()
                        + "-" + testInfo.getTestMethod().orElseThrow().getName()
                        + "-trace.zip";

                Path rutaTrace = carpetaTraces.resolve(nombreArchivo);

                context.tracing().stop(new Tracing.StopOptions()
                        .setPath(rutaTrace));

                System.out.println("Traza guardada: " + rutaTrace);
            }
        } finally {
            if (context != null) {
                context.close();
            }

            if (browser != null) {
                browser.close();
            }

            if (playwright != null) {
                playwright.close();
            }
        }
    }
}