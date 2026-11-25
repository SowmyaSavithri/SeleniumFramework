package com.iap.tests;

import com.iap.base.BaseTest;
import com.iap.pages.InventoryPage;
import com.iap.pages.LoginPage;
import com.iap.utils.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.Test;

/** TC01 - TC07: login scenarios (positive, negative, boundary, edge). */
public class LoginTests extends BaseTest {

    private static final int LONG_USERNAME_LENGTH = 256;

    @Test(description = "TC01 [Positive] Valid credentials open the Products page")
    public void validLoginShowsInventory() {
        LoginPage login = new LoginPage(driver).open();
        step("Open the SauceDemo login page");

        InventoryPage inventory = login.loginAs(ConfigReader.get("valid.username"), ConfigReader.get("valid.password"));
        boolean loaded = inventory.isLoaded();
        step("Log in with the valid username and password; the Products page is displayed");

        Assert.assertTrue(loaded, "Products page should be displayed after a valid login");
    }

    @Test(description = "TC02 [Negative] Wrong password is rejected with an error")
    public void invalidPasswordShowsError() {
        LoginPage login = new LoginPage(driver).open();
        step("Open the SauceDemo login page");

        login.submit(ConfigReader.get("valid.username"), ConfigReader.get("invalid.password"));
        boolean shown = login.isErrorDisplayed();
        String message = login.getErrorMessage();
        step("Enter the valid username with a wrong password and click Login; error banner: \"" + message + "\"");

        Assert.assertTrue(shown, "Error banner should be displayed");
        Assert.assertEquals(message, Messages.LOGIN_INVALID, "Error message");
    }

    @Test(description = "TC03 [Negative] Locked-out user cannot log in")
    public void lockedOutUserIsBlocked() {
        LoginPage login = new LoginPage(driver).open();
        step("Open the SauceDemo login page");

        login.submit(ConfigReader.get("locked.username"), ConfigReader.get("valid.password"));
        String message = login.getErrorMessage();
        step("Enter the locked-out username with the valid password and click Login; error banner: \"" + message + "\"");

        Assert.assertEquals(message, Messages.LOGIN_LOCKED_OUT, "Error message");
    }

    @Test(description = "TC04 [Negative] Empty username is rejected")
    public void emptyUsernameShowsError() {
        LoginPage login = new LoginPage(driver).open();
        step("Open the SauceDemo login page");

        login.submit("", ConfigReader.get("valid.password"));
        String message = login.getErrorMessage();
        step("Leave the username empty, enter the password and click Login; error banner: \"" + message + "\"");

        Assert.assertEquals(message, Messages.LOGIN_USERNAME_REQUIRED, "Error message");
    }

    @Test(description = "TC05 [Negative] Empty password is rejected")
    public void emptyPasswordShowsError() {
        LoginPage login = new LoginPage(driver).open();
        step("Open the SauceDemo login page");

        login.submit(ConfigReader.get("valid.username"), "");
        String message = login.getErrorMessage();
        step("Enter the username, leave the password empty and click Login; error banner: \"" + message + "\"");

        Assert.assertEquals(message, Messages.LOGIN_PASSWORD_REQUIRED, "Error message");
    }

    @Test(description = "TC06 [Boundary] A 256-character username is handled gracefully")
    public void veryLongUsernameIsRejectedGracefully() {
        LoginPage login = new LoginPage(driver).open();
        step("Open the SauceDemo login page");

        String longUsername = "a".repeat(LONG_USERNAME_LENGTH);
        login.submit(longUsername, ConfigReader.get("valid.password"));
        String message = login.getErrorMessage();
        step("Enter a " + LONG_USERNAME_LENGTH + "-character username with the valid password and click Login; "
                + "error banner: \"" + message + "\"");

        Assert.assertEquals(message, Messages.LOGIN_INVALID, "Error message");
    }

    @Test(description = "TC07 [Edge] SQL-injection style input does not bypass login")
    public void sqlInjectionStyleInputIsRejected() {
        LoginPage login = new LoginPage(driver).open();
        step("Open the SauceDemo login page");

        login.submit("' OR '1'='1", "' OR '1'='1");
        String message = login.getErrorMessage();
        step("Enter the SQL-injection style text \"' OR '1'='1\" as username and password and click Login; "
                + "login is refused with: \"" + message + "\"");

        Assert.assertEquals(message, Messages.LOGIN_INVALID, "Error message");
    }
}
