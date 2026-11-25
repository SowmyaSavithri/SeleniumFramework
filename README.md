# IAP Week 1 – Selenium Foundations (Java + Maven + TestNG)

Application under test: https://www.saucedemo.com (public practice site; replace if your lead assigns another application).

## Prerequisites
- JDK 17+  (`java -version`)
- Maven 3.9+ (`mvn -version`)
- Chrome and Edge (or Firefox) installed. No driver downloads needed: Selenium Manager handles them (internet required on first run).

## Run
```bash
mvn clean test                                   # Chrome, default
mvn clean test -Dbrowser=edge                    # Edge
mvn clean test -Dbrowser=firefox                 # Firefox
mvn clean test -Dheadless=true                   # no visible browser
```
Three consecutive runs with saved reports (this is your "90% pass rate on three consecutive runs" evidence):
```bash
./run_three_times.sh chrome        # macOS / Linux / Git Bash
.\run_three_times.ps1 -Browser chrome   # Windows PowerShell
```
TestNG report: `target/surefire-reports/index.html` (saved per run under `run-reports/`).
Failure screenshots: `target/screenshots/`.

## Structure
```
src/test/java/com/iap/
  base/BaseTest.java        browser start/stop, screenshot on failure
  pages/                    Page Object classes (all locators live here)
  utils/                    ConfigReader, DriverFactory, ScreenshotUtil
  tests/                    LoginTests, InventoryTests, CheckoutTests, EndToEndPurchaseTest
src/test/resources/config.properties   URL, browser, timeouts, test data
testng.xml                  suite definition
templates/                  daily status mail, prompt log, walkthrough notes
```

## Scenario coverage (15 automated tests, 1 of them E2E)
| ID | Type | Scenario |
|----|------|----------|
| TC01 | Positive | Valid login opens Products page |
| TC02 | Negative | Wrong password -> error |
| TC03 | Negative | Locked-out user blocked |
| TC04 | Negative | Empty username -> error |
| TC05 | Negative | Empty password -> error |
| TC06 | Boundary | 256-character username handled gracefully |
| TC07 | Edge | SQL-injection style input rejected |
| TC08 | Positive | Add one product -> badge = 1 |
| TC09 | Boundary | Add all 6 products -> badge = 6; remove all -> badge gone |
| TC10 | Positive | Sort by price low to high |
| TC11 | Positive | Remove item on cart page |
| TC12 | Negative | Checkout without first name |
| TC13 | Negative | Checkout without postal code |
| TC14 | Edge | Apostrophe/hyphen names and 1-character postal code accepted |
| TC15 | E2E | Login -> 2 products -> checkout -> confirmation |

## Before you demo: verify and make it yours
This project was generated with AI assistance and I could not run it against the live site, so
**run it once yourself and fix anything that fails** (for example if SauceDemo changed a locator).
The lead expects to see what you reviewed and improved. Candidate improvements to evaluate and
implement yourself (pick at least three that you genuinely understand and can explain):
- Add a test: opening `/inventory.html` directly without logging in must redirect to login with an error (edge case missing today).
- Convert TC04/TC05/TC06/TC07 into one `@DataProvider`-driven negative test.
- Replace the string-built XPath in `InventoryPage.buttonForProduct` with a safer approach, or compare it with a pure CSS equivalent and explain the trade-off.
- Add a `SoftAssert` where a test checks several things.
- Add TestNG `groups` (smoke / negative) so you can run subsets.
- Check which locators are weakest (positional, text-based) and make them more robust.
- Add a custom wait condition or log message that makes failures easier to diagnose.
