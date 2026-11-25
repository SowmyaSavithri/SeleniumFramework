package com.iap.base;

import com.iap.pages.InventoryPage;
import com.iap.pages.LoginPage;
import com.iap.utils.ConfigReader;
import com.iap.utils.DriverFactory;
import com.iap.utils.HtmlReportListener;
import com.iap.utils.ReportStep;
import com.iap.utils.ScreenshotUtil;
import com.iap.utils.TestCaseFilter;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Starts a fresh browser for every test (isolated tests) and always closes it, even on failure.
 * The listeners are declared here (not in testng.xml) so they also work with -Dtest=... runs.
 */
@Listeners({HtmlReportListener.class, TestCaseFilter.class})
public abstract class BaseTest {

    /** Name of the ITestResult attribute that holds the screenshot path (read by the HTML report). */
    public static final String SCREENSHOT_ATTRIBUTE = "screenshotPath";

    /** Name of the ITestResult attribute that holds the list of recorded steps (read by the HTML report). */
    public static final String STEPS_ATTRIBUTE = "reportSteps";

    protected WebDriver driver;

    /**
     * Records a step for the report: a plain-English description plus a screenshot taken right now.
     * Call it AFTER the action (and after any wait/check that confirms it), so the screenshot shows the result.
     */
    @SuppressWarnings("unchecked")
    protected void step(String description) {
        ITestResult result = Reporter.getCurrentTestResult();
        if (result == null) {
            return;
        }
        List<ReportStep> steps = (List<ReportStep>) result.getAttribute(STEPS_ATTRIBUTE);
        if (steps == null) {
            steps = new ArrayList<>();
            result.setAttribute(STEPS_ATTRIBUTE, steps);
        }
        steps.add(new ReportStep(description, ScreenshotUtil.thumbnailBase64(driver)));
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        driver = DriverFactory.createDriver(
                ConfigReader.get("browser"),
                ConfigReader.getBoolean("headless"));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (driver == null) {
            return;
        }
        try {
            attachScreenshot(result);
        } finally {
            driver.quit();
        }
    }

    /** Takes one screenshot per test (pass, fail or skip) and attaches it to the TestNG results. */
    private void attachScreenshot(ITestResult result) {
        Path png = ScreenshotUtil.capture(driver, getClass().getSimpleName(), result.getName(), statusName(result));
        if (png == null) {
            return;
        }
        result.setAttribute(SCREENSHOT_ATTRIBUTE, png.toAbsolutePath().toString());
        try {
            // Also embed in TestNG's own HTML report ("Reporter output"), so it is visible there too.
            String data = Base64.getEncoder().encodeToString(Files.readAllBytes(png));
            Reporter.setCurrentTestResult(result);
            Reporter.log("<br/><img src=\"data:image/png;base64," + data
                    + "\" style=\"max-width:640px;border:1px solid #999\" alt=\"screenshot\"/>");
        } catch (Exception e) {
            System.err.println("Could not embed screenshot in TestNG report: " + e.getMessage());
        }
    }

    private static String statusName(ITestResult result) {
        switch (result.getStatus()) {
            case ITestResult.SUCCESS:
                return "PASS";
            case ITestResult.FAILURE:
                return "FAIL";
            default:
                return "SKIP";
        }
    }

    /** Shared precondition for tests that start after login. */
    protected InventoryPage loginAsStandardUser() {
        LoginPage login = new LoginPage(driver).open();
        return login.loginAs(
                ConfigReader.get("valid.username"),
                ConfigReader.get("valid.password"));
    }
}
