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
        InventoryPage inventory = new LoginPage(driver).open()
                .loginAs(ConfigReader.get("valid.username"), ConfigReader.get("valid.password"));

        Assert.assertTrue(inventory.isLoaded(), "Products page should be displayed after a valid login");
    }

    @Test(description = "TC02 [Negative] Wrong password is rejected with an error")
    public void invalidPasswordShowsError() {
        LoginPage login = new LoginPage(driver).open()
                .submit(ConfigReader.get("valid.username"), ConfigReader.get("invalid.password"));

        Assert.assertTrue(login.isErrorDisplayed(), "Error banner should be displayed");
        Assert.assertEquals(login.getErrorMessage(), Messages.LOGIN_INVALID, "Error message");
    }

    @Test(description = "TC03 [Negative] Locked-out user cannot log in")
    public void lockedOutUserIsBlocked() {
        LoginPage login = new LoginPage(driver).open()
                .submit(ConfigReader.get("locked.username"), ConfigReader.get("valid.password"));

        Assert.assertEquals(login.getErrorMessage(), Messages.LOGIN_LOCKED_OUT, "Error message");
    }

    @Test(description = "TC04 [Negative] Empty username is rejected")
    public void emptyUsernameShowsError() {
        LoginPage login = new LoginPage(driver).open()
                .submit("", ConfigReader.get("valid.password"));

        Assert.assertEquals(login.getErrorMessage(), Messages.LOGIN_USERNAME_REQUIRED, "Error message");
    }

    @Test(description = "TC05 [Negative] Empty password is rejected")
    public void emptyPasswordShowsError() {
        LoginPage login = new LoginPage(driver).open()
                .submit(ConfigReader.get("valid.username"), "");

        Assert.assertEquals(login.getErrorMessage(), Messages.LOGIN_PASSWORD_REQUIRED, "Error message");
    }

    @Test(description = "TC06 [Boundary] A 256-character username is handled gracefully")
    public void veryLongUsernameIsRejectedGracefully() {
        String longUsername = "a".repeat(LONG_USERNAME_LENGTH);
        LoginPage login = new LoginPage(driver).open()
                .submit(longUsername, ConfigReader.get("valid.password"));

        Assert.assertEquals(login.getErrorMessage(), Messages.LOGIN_INVALID, "Error message");
    }

    @Test(description = "TC07 [Edge] SQL-injection style input does not bypass login")
    public void sqlInjectionStyleInputIsRejected() {
        LoginPage login = new LoginPage(driver).open()
                .submit("' OR '1'='1", "' OR '1'='1");

        Assert.assertEquals(login.getErrorMessage(), Messages.LOGIN_INVALID, "Error message");
    }
}
