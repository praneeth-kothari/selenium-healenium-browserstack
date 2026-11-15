package com.praneeth.browserstack;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Sanity check: plain healthy run on BrowserStack, no healing involved. */
public class InventoryTest extends BaseTest {

    @Test
    @DisplayName("Inventory shows 6 products after login")
    void inventoryShowsSixProducts() {
        driver.get("https://www.saucedemo.com/");
        driver.findElement(By.cssSelector("[data-test='username']")).sendKeys("standard_user");
        driver.findElement(By.cssSelector("[data-test='password']")).sendKeys("secret_sauce");
        driver.findElement(By.cssSelector("[data-test='login-button']")).click();
        assertEquals(6, driver.findElements(By.className("inventory_item")).size(),
                "Sauce Demo inventory should list 6 products");
    }
}
