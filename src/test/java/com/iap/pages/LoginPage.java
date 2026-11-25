package com.iap.pages;

import com.iap.utils.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

public class LoginPage extends BasePage {

    private static final By USERNAME = By.cssSelector("#user-name");
    private static final By PASSWORD = By.cssSelector("#password");
    private static final By LOGIN_BUTTON = By.cssSelector("[data-test='login-button']");
    private static final By ERROR = By.cssSelector("[data-test='error']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(ConfigReader.get("base.url"));
        waitVisible(LOGIN_BUTTON);
        return this;
    }

    /** Happy-path login: returns the page the user lands on. */
    public InventoryPage loginAs(String username, String password) {
        submit(username, password);
        return new InventoryPage(driver);
    }

    /** Fills the form and clicks Login without assuming it will succeed (used by negative tests). */
    public LoginPage submit(String username, String password) {
        type(USERNAME, username);
        type(PASSWORD, password);
        click(LOGIN_BUTTON);
        return this;
    }

    public boolean isErrorDisplayed() {
        return isVisible(ERROR);
    }

    public String getErrorMessage() {
        return getText(ERROR);
    }
}
