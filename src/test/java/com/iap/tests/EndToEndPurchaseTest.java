package com.iap.tests;

import com.iap.base.BaseTest;
import com.iap.pages.CartPage;
import com.iap.pages.CheckoutCompletePage;
import com.iap.pages.CheckoutInfoPage;
import com.iap.pages.CheckoutOverviewPage;
import com.iap.pages.InventoryPage;
import com.iap.utils.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;

/** TC15 and TC16: complete purchase journeys using only page-class methods (no locators in the test). */
public class EndToEndPurchaseTest extends BaseTest {

    @Test(description = "TC15 [E2E] Login -> add 2 products -> checkout -> order confirmation")
    public void completePurchaseFlow() {
        // 1. Login
        InventoryPage inventory = loginAsStandardUser();
        boolean loaded = inventory.isLoaded();
        step("Log in as the valid user; the Products page is displayed");
        Assert.assertTrue(loaded, "Products page should load after login");

        // 2. Add two products
        inventory.addProduct("Sauce Labs Backpack");
        inventory.addProduct("Sauce Labs Bike Light");
        boolean badgeShowsTwo = inventory.waitForCartCount(2);
        step("Add \"Sauce Labs Backpack\" and \"Sauce Labs Bike Light\"; the cart badge shows 2");
        Assert.assertTrue(badgeShowsTwo, "Cart badge should show 2");

        // 3. Review the cart
        CartPage cart = inventory.openCart();
        int itemCount = cart.getItemCount();
        step("Open the cart; it contains " + itemCount + " products");
        Assert.assertEquals(itemCount, 2, "Cart should contain both products");

        // 4. Checkout details
        CheckoutInfoPage info = cart.proceedToCheckout();
        info.isLoaded();
        step("Click Checkout; the checkout information form is displayed");

        CheckoutOverviewPage overview = info
                .enterDetails(
                        ConfigReader.get("checkout.firstName"),
                        ConfigReader.get("checkout.lastName"),
                        ConfigReader.get("checkout.postalCode"))
                .continueToOverview();
        boolean overviewLoaded = overview.isLoaded();
        String itemTotal = overview.getItemTotalText();
        step("Enter the customer details and click Continue; the overview shows \"" + itemTotal + "\"");
        Assert.assertTrue(overviewLoaded, "Checkout overview should be displayed");
        Assert.assertTrue(itemTotal.contains("$39.98"),
                "Item total should be 29.99 + 9.99 but was: " + itemTotal);

        // 5. Finish
        CheckoutCompletePage complete = overview.finish();
        boolean completeLoaded = complete.isLoaded();
        String confirmation = complete.getConfirmationMessage();
        step("Click Finish; the order is confirmed with: \"" + confirmation + "\"");
        Assert.assertTrue(completeLoaded, "Order complete page should be displayed");
        Assert.assertEquals(confirmation, Messages.ORDER_CONFIRMATION, "Confirmation");
    }

    /**
     * TC16: sort by price, add the 3 cheapest products, drop one in the cart, and check that the checkout
     * total equals the sum of the two remaining prices read from the page (no hardcoded amounts).
     */
    @Test(description = "TC16 [E2E] Login -> sort by price -> add 3 cheapest -> remove 1 in cart -> checkout -> verify total -> order confirmation")
    public void sortedCheapestProductsPurchaseFlow() {
        // 1. Login
        InventoryPage inventory = loginAsStandardUser();
        boolean loaded = inventory.isLoaded();
        step("Log in as the valid user; the Products page is displayed");
        Assert.assertTrue(loaded, "Products page should load after login");

        // 2. Sort low-to-high and remember the three cheapest prices
        inventory.sortBy("Price (low to high)");
        boolean sorted = inventory.arePricesSortedLowToHigh();
        List<Double> cheapest = inventory.getPrices().subList(0, 3);
        step("Sort by \"Price (low to high)\"; the three cheapest prices are " + cheapest);
        Assert.assertTrue(sorted, "Products should be sorted low to high");

        // 3. Add the three cheapest products
        inventory.addFirstProducts(3);
        boolean badgeShowsThree = inventory.waitForCartCount(3);
        step("Add the three cheapest products; the cart badge shows 3");
        Assert.assertTrue(badgeShowsThree, "Cart badge should show 3");

        // 4. In the cart, remove the first item (the cheapest one) and keep the other two
        CartPage cart = inventory.openCart();
        int before = cart.getItemCount();
        step("Open the cart; it contains " + before + " products");
        Assert.assertEquals(before, 3, "Cart should contain 3 products");

        cart.removeFirstItem();
        int after = cart.getItemCount();
        step("Remove the first (cheapest) item; the cart now contains " + after + " products");
        Assert.assertEquals(after, 2, "Cart should contain 2 products after removing one");

        // 5. Checkout and verify the item total against the prices read earlier
        CheckoutInfoPage info = cart.proceedToCheckout();
        info.isLoaded();
        step("Click Checkout; the checkout information form is displayed");

        CheckoutOverviewPage overview = info
                .enterDetails(
                        ConfigReader.get("checkout.firstName"),
                        ConfigReader.get("checkout.lastName"),
                        ConfigReader.get("checkout.postalCode"))
                .continueToOverview();
        boolean overviewLoaded = overview.isLoaded();
        double expectedTotal = cheapest.get(1) + cheapest.get(2);
        String expectedText = String.format(Locale.US, "$%.2f", expectedTotal);
        String itemTotal = overview.getItemTotalText();
        step("Enter the customer details and click Continue; the overview shows \"" + itemTotal
                + "\" (expected " + expectedText + ")");
        Assert.assertTrue(overviewLoaded, "Checkout overview should be displayed");
        Assert.assertTrue(itemTotal.contains(expectedText),
                "Item total should be " + expectedText + " but was: " + itemTotal);

        // 6. Finish
        CheckoutCompletePage complete = overview.finish();
        boolean completeLoaded = complete.isLoaded();
        String confirmation = complete.getConfirmationMessage();
        step("Click Finish; the order is confirmed with: \"" + confirmation + "\"");
        Assert.assertTrue(completeLoaded, "Order complete page should be displayed");
        Assert.assertEquals(confirmation, Messages.ORDER_CONFIRMATION, "Confirmation");
    }
}
