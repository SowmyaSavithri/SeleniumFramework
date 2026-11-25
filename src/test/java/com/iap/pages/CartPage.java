package com.iap.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CartPage extends BasePage {

    private static final By TITLE = By.cssSelector("[data-test='title']");
    private static final By CART_ITEMS = By.cssSelector("[data-test='inventory-item']");
    private static final By REMOVE_BUTTONS = By.cssSelector("button[data-test^='remove']");
    private static final By CHECKOUT_BUTTON = By.cssSelector("[data-test='checkout']");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return textBecomes(TITLE, "Your Cart");
    }

    public int getItemCount() {
        isLoaded();
        return driver.findElements(CART_ITEMS).size();
    }

    public void removeFirstItem() {
        int before = driver.findElements(CART_ITEMS).size();
        click(REMOVE_BUTTONS);
        wait.until(d -> d.findElements(CART_ITEMS).size() == before - 1);
    }

    public CheckoutInfoPage proceedToCheckout() {
        click(CHECKOUT_BUTTON);
        return new CheckoutInfoPage(driver);
    }
}
