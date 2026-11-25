package com.iap.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtil {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ScreenshotUtil() {
    }

    /**
     * Saves a PNG of the current browser view to target/screenshots/&lt;className&gt;/ and returns its path,
     * or null if the screenshot could not be taken.
     */
    public static Path capture(WebDriver driver, String className, String testName, String status) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Path dir = Paths.get("target", "screenshots", className);
            Files.createDirectories(dir);
            Path target = dir.resolve(testName + "_" + status + "_" + LocalDateTime.now().format(STAMP) + ".png");
            Files.write(target, png);
            return target;
        } catch (Exception e) {
            System.err.println("Could not capture screenshot: " + e.getMessage());
            return null;
        }
    }
}
