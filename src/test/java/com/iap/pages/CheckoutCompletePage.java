package com.iap.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CheckoutCompletePage extends BasePage {

    private static final By TITLE = By.cssSelector("[data-test='title']");
    private static final By CONFIRMATION = By.cssSelector("[data-test='complete-header']");

    public CheckoutCompletePage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return textBecomes(TITLE, "Checkout: Complete!");
    }

    public String getConfirmationMessage() {
        return getText(CONFIRMATION);
    }
}
