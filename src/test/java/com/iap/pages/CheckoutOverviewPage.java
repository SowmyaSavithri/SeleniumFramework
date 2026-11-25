package com.iap.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CheckoutOverviewPage extends BasePage {

    private static final By TITLE = By.cssSelector("[data-test='title']");
    private static final By ITEM_TOTAL = By.cssSelector("[data-test='subtotal-label']");
    private static final By FINISH_BUTTON = By.cssSelector("[data-test='finish']");

    public CheckoutOverviewPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return textBecomes(TITLE, "Checkout: Overview");
    }

    public String getItemTotalText() {
        return getText(ITEM_TOTAL);
    }

    public CheckoutCompletePage finish() {
        click(FINISH_BUTTON);
        return new CheckoutCompletePage(driver);
    }
}
