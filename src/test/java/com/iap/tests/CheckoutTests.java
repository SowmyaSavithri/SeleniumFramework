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
        inventory.addProduct("Sauce Labs Backpack");
        return inventory.openCart().proceedToCheckout();
    }

    @Test(description = "TC12 [Negative] Missing first name blocks checkout")
    public void missingFirstNameShowsError() {
        CheckoutInfoPage info = openCheckoutWithOneItem()
                .enterDetails("", ConfigReader.get("checkout.lastName"), ConfigReader.get("checkout.postalCode"))
                .clickContinue();

        Assert.assertEquals(info.getErrorMessage(), Messages.CHECKOUT_FIRST_NAME_REQUIRED, "Error message");
    }

    @Test(description = "TC13 [Negative] Missing postal code blocks checkout")
    public void missingPostalCodeShowsError() {
        CheckoutInfoPage info = openCheckoutWithOneItem()
                .enterDetails(ConfigReader.get("checkout.firstName"), ConfigReader.get("checkout.lastName"), "")
                .clickContinue();

        Assert.assertEquals(info.getErrorMessage(), Messages.CHECKOUT_POSTAL_CODE_REQUIRED, "Error message");
    }

    @Test(description = "TC14 [Edge] Apostrophes and hyphens in a name, and a short postal code, are accepted")
    public void specialCharactersInNameAreAccepted() {
        CheckoutOverviewPage overview = openCheckoutWithOneItem()
                .enterDetails("D'Souza", "Rao-Iyer", "0")
                .continueToOverview();

        Assert.assertTrue(overview.isLoaded(), "Overview page should open for names with special characters");
    }
}
