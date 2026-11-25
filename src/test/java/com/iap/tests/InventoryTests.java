package com.iap.tests;

import com.iap.base.BaseTest;
import com.iap.pages.CartPage;
import com.iap.pages.InventoryPage;
import org.testng.Assert;
import org.testng.annotations.Test;

/** TC08 - TC11: product list and cart behaviour. */
public class InventoryTests extends BaseTest {

    private static final int TOTAL_PRODUCTS = 6;

    @Test(description = "TC08 [Positive] Adding one product updates the cart badge to 1")
    public void addSingleProductUpdatesBadge() {
        InventoryPage inventory = loginAsStandardUser();
        step("Log in as the valid user; the Products page is displayed");

        inventory.addProduct("Sauce Labs Backpack");
        boolean badgeShowsOne = inventory.waitForCartCount(1);
        step("Add \"Sauce Labs Backpack\" to the cart; the cart badge shows 1");

        Assert.assertTrue(badgeShowsOne, "Cart badge should show 1");
    }

    @Test(description = "TC09 [Boundary] Adding all 6 products shows 6, removing them clears the badge")
    public void addAllProductsThenRemoveAll() {
        InventoryPage inventory = loginAsStandardUser();
        step("Log in as the valid user; the Products page is displayed");

        inventory.addFirstProducts(TOTAL_PRODUCTS);
        boolean badgeShowsAll = inventory.waitForCartCount(TOTAL_PRODUCTS);
        step("Add all " + TOTAL_PRODUCTS + " products to the cart; the cart badge shows " + TOTAL_PRODUCTS);
        Assert.assertTrue(badgeShowsAll, "Cart badge should show 6");

        inventory.removeAllProducts();
        boolean badgeGone = inventory.waitForCartCount(0);
        step("Remove all products; the cart badge disappears");
        Assert.assertTrue(badgeGone, "Cart badge should disappear when the cart is empty");
    }

    @Test(description = "TC10 [Positive] Sorting by price (low to high) orders the products correctly")
    public void sortByPriceLowToHigh() {
        InventoryPage inventory = loginAsStandardUser();
        step("Log in as the valid user; the Products page is displayed");

        inventory.sortBy("Price (low to high)");
        boolean sorted = inventory.arePricesSortedLowToHigh();
        step("Sort the products by \"Price (low to high)\"; prices shown: " + inventory.getPrices());

        Assert.assertTrue(sorted, "Prices should be in ascending order: " + inventory.getPrices());
    }

    @Test(description = "TC11 [Positive] Removing an item on the cart page empties the cart")
    public void removeItemFromCart() {
        InventoryPage inventory = loginAsStandardUser();
        step("Log in as the valid user; the Products page is displayed");

        inventory.addProduct("Sauce Labs Bike Light");
        inventory.waitForCartCount(1);
        step("Add \"Sauce Labs Bike Light\" to the cart; the cart badge shows 1");

        CartPage cart = inventory.openCart();
        int before = cart.getItemCount();
        step("Open the cart; it holds " + before + " item");
        Assert.assertEquals(before, 1, "Cart should hold one item before removal");

        cart.removeFirstItem();
        int after = cart.getItemCount();
        step("Remove the item on the cart page; the cart now holds " + after + " items");
        Assert.assertEquals(after, 0, "Cart should be empty after removal");
    }
}
