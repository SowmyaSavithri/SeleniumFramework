package com.iap.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a WebDriver for the requested browser.
 * Selenium Manager (built into Selenium 4.6+) resolves the matching driver binary automatically.
 * Note: no implicit wait is set on purpose - we use explicit waits only.
 *
 * Chrome/Edge are started with a throw-away profile and extra startup flags, which avoids the
 * "session not created: DevToolsActivePort file doesn't exist" error seen on managed laptops.
 */
public final class DriverFactory {

    private static final String[] CHROMIUM_STARTUP_FLAGS = {
            "--disable-notifications",
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--remote-allow-origins=*",
//            "--remote-debugging-port=0",
            "--no-first-run",
            "--no-default-browser-check",
            "--disable-extensions",
            "--disable-sync",

    };

    private DriverFactory() {
    }

    public static WebDriver createDriver(String browser, boolean headless) {
        WebDriver driver;
        switch (browser.toLowerCase().trim()) {
            case "chrome":
                driver = new ChromeDriver(chromeOptions(headless));
                break;
            case "edge":
                driver = new EdgeDriver(edgeOptions(headless));
                break;
            case "firefox":
                driver = new FirefoxDriver(firefoxOptions(headless));
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported browser '" + browser + "'. Use chrome, edge or firefox.");
        }
        driver.manage().timeouts().pageLoadTimeout(
                Duration.ofSeconds(ConfigReader.getInt("page.load.timeout.seconds")));
        if (!headless) {
            driver.manage().window().maximize();
        }
        return driver;
    }

    /** Stops Chrome/Edge "save password" and "password leak" pop-ups from covering the page. */
    private static Map<String, Object> noPasswordPrompts() {
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        return prefs;
    }

    /** A clean, throw-away browser profile avoids locked or policy-restricted default profiles. */
    private static String freshProfileDir(String prefix) {
        try {
            return Files.createTempDirectory("iap-" + prefix + "-").toString();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create a temporary browser profile", e);
        }
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        // Chrome for Testing: the managed Chrome install blocks Selenium (RemoteDebuggingAllowed=0).
        String chromeBinary = ConfigReader.getOrDefault("chrome.binary", "");
        String chromeDriver = ConfigReader.getOrDefault("chrome.driver", "");
        if (!chromeDriver.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", chromeDriver);
        }
        ChromeOptions options = new ChromeOptions();
        if (!chromeBinary.isEmpty()) {
            options.setBinary(chromeBinary);
        }
        options.setExperimentalOption("prefs", noPasswordPrompts());
        options.addArguments(CHROMIUM_STARTUP_FLAGS);
        options.addArguments("--user-data-dir=" + freshProfileDir("chrome"));
        if (headless) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.setExperimentalOption("prefs", noPasswordPrompts());
        options.addArguments(CHROMIUM_STARTUP_FLAGS);
        options.addArguments("--user-data-dir=" + freshProfileDir("edge"));
        if (headless) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless", "--width=1920", "--height=1080");
        }
        return options;
    }
}