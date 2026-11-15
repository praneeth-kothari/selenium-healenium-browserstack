package com.praneeth.browserstack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two-phase healing demo - but the browser session runs on BrowserStack while
 * the healing store runs locally (docker-compose.yml). Each test gets its own
 * BrowserStack session; healing persists across sessions because the store is
 * external to the browser.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LoginHealingTest extends BaseTest {

    private static final String BASE_URL = "https://www.saucedemo.com/";

    @Test
    @Order(1)
    @DisplayName("Phase 1 - healthy locators on BrowserStack register element info")
    void healthyLogin() {
        driver.get(BASE_URL);
        driver.findElement(By.cssSelector("[data-test='username']")).sendKeys("standard_user");
        driver.findElement(By.cssSelector("[data-test='password']")).sendKeys("secret_sauce");
        driver.findElement(By.cssSelector("[data-test='login-button']")).click();
        assertTrue(driver.getCurrentUrl().contains("inventory"), "Login should reach inventory");
    }

    @Test
    @Order(2)
    @DisplayName("Phase 2 - broken locators are healed while running on BrowserStack")
    void brokenLocatorsAreHealed() {
        driver.manage().deleteAllCookies(); // fresh session; stored element info persists in the backend
        driver.get(BASE_URL);

        // Deliberately broken locators - none of these exist in the DOM
        driver.findElement(By.id("user-name-wrong")).sendKeys("standard_user");
        driver.findElement(By.id("password-wrong")).sendKeys("secret_sauce");
        driver.findElement(By.id("login-button-wrong")).click();

        assertTrue(driver.getCurrentUrl().contains("inventory"),
                "Healenium should heal the broken locators even on a cloud session");
    }
}
