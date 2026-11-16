package com.tests;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

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
        page = context.newPage();
    }

    @AfterEach
    void tearDown() {
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