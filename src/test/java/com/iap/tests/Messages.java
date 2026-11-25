package com.iap.tests;

/** Expected application messages, kept in one place so tests stay readable. */
final class Messages {

    static final String LOGIN_INVALID =
            "Epic sadface: Username and password do not match any user in this service";
    static final String LOGIN_LOCKED_OUT = "Epic sadface: Sorry, this user has been locked out.";
    static final String LOGIN_USERNAME_REQUIRED = "Epic sadface: Username is required";
    static final String LOGIN_PASSWORD_REQUIRED = "Epic sadface: Password is required";

    static final String CHECKOUT_FIRST_NAME_REQUIRED = "Error: First Name is required";
    static final String CHECKOUT_POSTAL_CODE_REQUIRED = "Error: Postal Code is required";

    static final String ORDER_CONFIRMATION = "Thank you for your order!";

    private Messages() {
    }
}
