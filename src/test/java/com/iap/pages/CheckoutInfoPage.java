package com.iap.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class CheckoutInfoPage extends BasePage {

    private static final By TITLE = By.cssSelector("[data-test='title']");
    private static final By FIRST_NAME = By.cssSelector("[data-test='firstName']");
    private static final By LAST_NAME = By.cssSelector("[data-test='lastName']");
    private static final By POSTAL_CODE = By.cssSelector("[data-test='postalCode']");
    private static final By CONTINUE_BUTTON = By.cssSelector("[data-test='continue']");
    private static final By ERROR = By.cssSelector("[data-test='error']");

    public CheckoutInfoPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return textBecomes(TITLE, "Checkout: Your Information");
    }

    public CheckoutInfoPage enterDetails(String firstName, String lastName, String postalCode) {
        type(FIRST_NAME, firstName);
        type(LAST_NAME, lastName);
        type(POSTAL_CODE, postalCode);
        return this;
    }

    public CheckoutOverviewPage continueToOverview() {
        click(CONTINUE_BUTTON);
        return new CheckoutOverviewPage(driver);
    }

    /** Clicks Continue when the form is expected to be rejected (negative tests). */
    public CheckoutInfoPage clickContinue() {
        click(CONTINUE_BUTTON);
        return this;
    }

    public boolean isErrorDisplayed() {
        return isVisible(ERROR);
    }

    public String getErrorMessage() {
        return getText(ERROR);
    }
}
