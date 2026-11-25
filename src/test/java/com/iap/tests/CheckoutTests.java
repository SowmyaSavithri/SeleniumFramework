package com.iap.tests;

import com.iap.base.BaseTest;
import com.iap.pages.CheckoutInfoPage;
import com.iap.pages.CheckoutOverviewPage;
import com.iap.pages.InventoryPage;
import com.iap.utils.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.Test;

/** TC12 - TC14: checkout form validation. */
public class CheckoutTests extends BaseTest {

    private CheckoutInfoPage openCheckoutWithOneItem() {
        InventoryPage inventory = loginAsStandardUser();
        step("Log in as the valid user; the Products page is displayed");

        inventory.addProduct("Sauce Labs Backpack");
        inventory.waitForCartCount(1);
        step("Add \"Sauce Labs Backpack\" to the cart; the cart badge shows 1");

        CheckoutInfoPage info = inventory.openCart().proceedToCheckout();
        info.isLoaded();
        step("Open the cart and click Checkout; the checkout information form is displayed");
        return info;
    }

    @Test(description = "TC12 [Negative] Missing first name blocks checkout")
    public void missingFirstNameShowsError() {
        CheckoutInfoPage info = openCheckoutWithOneItem()
                .enterDetails("", ConfigReader.get("checkout.lastName"), ConfigReader.get("checkout.postalCode"))
                .clickContinue();
        String message = info.getErrorMessage();
        step("Leave the first name empty, fill the last name and postal code, click Continue; error: \""
                + message + "\"");

        Assert.assertEquals(message, Messages.CHECKOUT_FIRST_NAME_REQUIRED, "Error message");
    }

    @Test(description = "TC13 [Negative] Missing postal code blocks checkout")
    public void missingPostalCodeShowsError() {
        CheckoutInfoPage info = openCheckoutWithOneItem()
                .enterDetails(ConfigReader.get("checkout.firstName"), ConfigReader.get("checkout.lastName"), "")
                .clickContinue();
        String message = info.getErrorMessage();
        step("Fill the first and last name, leave the postal code empty, click Continue; error: \""
                + message + "\"");

        Assert.assertEquals(message, Messages.CHECKOUT_POSTAL_CODE_REQUIRED, "Error message");
    }

    @Test(description = "TC14 [Edge] Apostrophes and hyphens in a name, and a short postal code, are accepted")
    public void specialCharactersInNameAreAccepted() {
        CheckoutOverviewPage overview = openCheckoutWithOneItem()
                .enterDetails("D'Souza", "Rao-Iyer", "0")
                .continueToOverview();
        boolean loaded = overview.isLoaded();
        step("Enter first name D'Souza, last name Rao-Iyer and postal code 0, click Continue; "
                + "the checkout overview page opens");

        Assert.assertTrue(loaded, "Overview page should open for names with special characters");
    }
}
