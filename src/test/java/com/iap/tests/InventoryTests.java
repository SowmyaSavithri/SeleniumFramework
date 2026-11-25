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
        inventory.addProduct("Sauce Labs Backpack");

        Assert.assertTrue(inventory.waitForCartCount(1), "Cart badge should show 1");
    }

    @Test(description = "TC09 [Boundary] Adding all 6 products shows 6, removing them clears the badge")
    public void addAllProductsThenRemoveAll() {
        InventoryPage inventory = loginAsStandardUser();

        inventory.addFirstProducts(TOTAL_PRODUCTS);
        Assert.assertTrue(inventory.waitForCartCount(TOTAL_PRODUCTS), "Cart badge should show 6");

        inventory.removeAllProducts();
        Assert.assertTrue(inventory.waitForCartCount(0), "Cart badge should disappear when the cart is empty");
    }

    @Test(description = "TC10 [Positive] Sorting by price (low to high) orders the products correctly")
    public void sortByPriceLowToHigh() {
        InventoryPage inventory = loginAsStandardUser();
        inventory.sortBy("Price (low to high)");

        Assert.assertTrue(inventory.arePricesSortedLowToHigh(),
                "Prices should be in ascending order: " + inventory.getPrices());
    }

    @Test(description = "TC11 [Positive] Removing an item on the cart page empties the cart")
    public void removeItemFromCart() {
        InventoryPage inventory = loginAsStandardUser();
        inventory.addProduct("Sauce Labs Bike Light");
        CartPage cart = inventory.openCart();

        Assert.assertEquals(cart.getItemCount(), 1, "Cart should hold one item before removal");
        cart.removeFirstItem();
        Assert.assertEquals(cart.getItemCount(), 0, "Cart should be empty after removal");
    }
}
