package com.iap.tests;

import com.iap.base.BaseTest;
import com.iap.pages.CartPage;
import com.iap.pages.CheckoutCompletePage;
import com.iap.pages.CheckoutOverviewPage;
import com.iap.pages.InventoryPage;
import com.iap.utils.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.Test;

/** TC15: complete purchase journey using only page-class methods (no locators in the test). */
public class EndToEndPurchaseTest extends BaseTest {

    @Test(description = "TC15 [E2E] Login -> add 2 products -> checkout -> order confirmation")
    public void completePurchaseFlow() {
        // 1. Login
        InventoryPage inventory = loginAsStandardUser();
        Assert.assertTrue(inventory.isLoaded(), "Products page should load after login");

        // 2. Add two products
        inventory.addProduct("Sauce Labs Backpack");
        inventory.addProduct("Sauce Labs Bike Light");
        Assert.assertTrue(inventory.waitForCartCount(2), "Cart badge should show 2");

        // 3. Review the cart
        CartPage cart = inventory.openCart();
        Assert.assertEquals(cart.getItemCount(), 2, "Cart should contain both products");

        // 4. Checkout details
        CheckoutOverviewPage overview = cart.proceedToCheckout()
                .enterDetails(
                        ConfigReader.get("checkout.firstName"),
                        ConfigReader.get("checkout.lastName"),
                        ConfigReader.get("checkout.postalCode"))
                .continueToOverview();
        Assert.assertTrue(overview.isLoaded(), "Checkout overview should be displayed");
        Assert.assertTrue(overview.getItemTotalText().contains("$39.98"),
                "Item total should be 29.99 + 9.99 but was: " + overview.getItemTotalText());

        // 5. Finish
        CheckoutCompletePage complete = overview.finish();
        Assert.assertTrue(complete.isLoaded(), "Order complete page should be displayed");
        Assert.assertEquals(complete.getConfirmationMessage(), Messages.ORDER_CONFIRMATION, "Confirmation");
    }
}
