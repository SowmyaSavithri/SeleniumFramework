package com.iap.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class InventoryPage extends BasePage {

    private static final By TITLE = By.cssSelector("[data-test='title']");
    private static final By ADD_BUTTONS = By.cssSelector("button[data-test^='add-to-cart']");
    private static final By REMOVE_BUTTONS = By.cssSelector("button[data-test^='remove']");
    private static final By CART_BADGE = By.cssSelector("[data-test='shopping-cart-badge']");
    private static final By CART_LINK = By.cssSelector("[data-test='shopping-cart-link']");
    private static final By SORT_DROPDOWN = By.cssSelector("[data-test='product-sort-container']");
    private static final By PRICES = By.cssSelector("[data-test='inventory-item-price']");

    /** XPath example: find the button that belongs to a product by the product's name. */
    private static By buttonForProduct(String productName) {
        return By.xpath("//div[@data-test='inventory-item-name' and normalize-space()='" + productName + "']"
                + "/ancestor::div[@data-test='inventory-item']//button");
    }

    public InventoryPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return textBecomes(TITLE, "Products");
    }

    public void addProduct(String productName) {
        click(buttonForProduct(productName));
    }

    /** Adds the first n products, waiting for the cart badge after every click so clicks never race. */
    public void addFirstProducts(int count) {
        for (int i = 1; i <= count; i++) {
            click(ADD_BUTTONS);
            waitForCartCount(i);
        }
    }

    public void removeAllProducts() {
        int remaining = driver.findElements(REMOVE_BUTTONS).size();
        for (int i = remaining - 1; i >= 0; i--) {
            click(REMOVE_BUTTONS);
            waitForCartCount(i);
        }
    }

    /** Waits until the cart badge shows 'expected' (0 means the badge disappears). */
    public boolean waitForCartCount(int expected) {
        return expected == 0 ? isGone(CART_BADGE) : textBecomes(CART_BADGE, String.valueOf(expected));
    }

    public void sortBy(String visibleText) {
        new Select(waitVisible(SORT_DROPDOWN)).selectByVisibleText(visibleText);
    }

    public List<Double> getPrices() {
        return driver.findElements(PRICES).stream()
                .map(WebElement::getText)
                .map(text -> Double.parseDouble(text.replace("$", "").trim()))
                .collect(Collectors.toList());
    }

    public boolean arePricesSortedLowToHigh() {
        try {
            return wait.until(d -> {
                List<Double> actual = getPrices();
                List<Double> expected = new ArrayList<>(actual);
                Collections.sort(expected);
                return actual.equals(expected);
            });
        } catch (TimeoutException e) {
            return false;
        }
    }

    public CartPage openCart() {
        click(CART_LINK);
        return new CartPage(driver);
    }
}
