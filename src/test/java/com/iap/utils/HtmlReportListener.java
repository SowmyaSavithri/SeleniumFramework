package com.iap.utils;

import com.iap.base.BaseTest;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * Writes one self-contained HTML report (target/reports/test-report.html): a summary plus one card per
 * test case with its status, duration, failure message and the screenshot taken at the end of the test.
 * Screenshots are embedded, so the single file can be e-mailed or opened from anywhere.
 */
public class HtmlReportListener implements IReporter {

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        List<ITestResult> all = new ArrayList<>();
        for (ISuite suite : suites) {
            for (ISuiteResult sr : suite.getResults().values()) {
                all.addAll(sr.getTestContext().getPassedTests().getAllResults());
                all.addAll(sr.getTestContext().getFailedTests().getAllResults());
                all.addAll(sr.getTestContext().getSkippedTests().getAllResults());
            }
        }
        all.sort(Comparator.comparing((ITestResult r) -> r.getTestClass().getName())
                .thenComparingLong(ITestResult::getStartMillis));

        long pass = all.stream().filter(r -> r.getStatus() == ITestResult.SUCCESS).count();
        long fail = all.stream().filter(r -> r.getStatus() == ITestResult.FAILURE).count();
        long skip = all.size() - pass - fail;

        StringBuilder html = new StringBuilder(64 * 1024);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>Test Report</title><style>").append(css()).append("</style></head><body><main>");
        html.append("<h1>Selenium Test Report</h1><p class=\"meta\">Browser: ")
                .append(esc(ConfigReader.getOrDefault("browser", "chrome"))).append(" &middot; Generated ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")))
                .append("</p>");
        html.append("<div class=\"summary\"><div class=\"tile\"><b>").append(all.size())
                .append("</b>Total</div><div class=\"tile pass\"><b>").append(pass)
                .append("</b>Passed</div><div class=\"tile fail\"><b>").append(fail)
                .append("</b>Failed</div><div class=\"tile skip\"><b>").append(skip)
                .append("</b>Skipped</div></div>");

        String currentClass = "";
        for (ITestResult r : all) {
            String cls = r.getTestClass().getRealClass().getSimpleName();
            if (!cls.equals(currentClass)) {
                html.append("<h2>").append(esc(cls)).append("</h2>");
                currentClass = cls;
            }
            appendCard(html, r);
        }
        html.append("</main></body></html>");

        try {
            Path out = Paths.get("target", "reports", "test-report.html");
            Files.createDirectories(out.getParent());
            Files.write(out, html.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("HTML report with screenshots: " + out.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not write HTML report: " + e.getMessage());
        }
    }

    private static void appendCard(StringBuilder html, ITestResult r) {
        String status = r.getStatus() == ITestResult.SUCCESS ? "pass"
                : r.getStatus() == ITestResult.FAILURE ? "fail" : "skip";
        String description = r.getMethod().getDescription();
        String title = description != null && !description.isEmpty() ? description : r.getName();
        long ms = r.getEndMillis() - r.getStartMillis();

        html.append("<section class=\"card ").append(status).append("\"><div class=\"head\"><span class=\"badge\">")
                .append(status.toUpperCase()).append("</span><span class=\"title\">").append(esc(title))
                .append("</span><span class=\"dur\">").append(String.format("%.1f s", ms / 1000.0))
                .append("</span></div>");
        html.append("<div class=\"method\">").append(esc(r.getName())).append("()</div>");

        Throwable t = r.getThrowable();
        if (t != null) {
            html.append("<pre class=\"err\">").append(esc(String.valueOf(t.getMessage()))).append("</pre>");
        }

        Object shot = r.getAttribute(BaseTest.SCREENSHOT_ATTRIBUTE);
        if (shot != null) {
            try {
                String data = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(shot.toString())));
                html.append("<a href=\"data:image/png;base64,").append(data).append("\" target=\"_blank\">")
                        .append("<img alt=\"Screenshot\" src=\"data:image/png;base64,").append(data).append("\"></a>");
            } catch (IOException e) {
                html.append("<p class=\"meta\">Screenshot file missing.</p>");
            }
        } else {
            html.append("<p class=\"meta\">No screenshot (browser did not start).</p>");
        }
        html.append("</section>");
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String css() {
        return ":root{--bg:#f3f4f7;--surface:#fff;--ink:#1b2430;--muted:#5b6472;--border:#dde1e7;"
                + "--pass:#2a7a4b;--fail:#b3261e;--skip:#8a6d1a}"
                + "@media(prefers-color-scheme:dark){:root{--bg:#11151b;--surface:#1a202a;--ink:#e7ecf2;"
                + "--muted:#96a1b0;--border:#2b3340;--pass:#5cc58a;--fail:#f08a83;--skip:#e0bf5c}}"
                + "*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);"
                + "font-family:system-ui,Segoe UI,sans-serif;line-height:1.5;padding:0 16px}"
                + "main{max-width:900px;margin:0 auto;padding:32px 0 64px}h1{margin:0 0 4px}"
                + "h2{font-size:15px;color:var(--muted);text-transform:uppercase;letter-spacing:.04em;margin:28px 0 10px}"
                + ".meta{color:var(--muted);font-size:14px;margin:0 0 16px}"
                + ".summary{display:flex;gap:12px;flex-wrap:wrap}"
                + ".tile{flex:1;min-width:110px;background:var(--surface);border:1px solid var(--border);"
                + "border-radius:12px;padding:12px 16px;color:var(--muted);font-size:13px}"
                + ".tile b{display:block;font-size:26px;color:var(--ink)}"
                + ".tile.pass b{color:var(--pass)}.tile.fail b{color:var(--fail)}.tile.skip b{color:var(--skip)}"
                + ".card{background:var(--surface);border:1px solid var(--border);border-left-width:5px;"
                + "border-radius:12px;padding:14px 16px;margin-bottom:14px}"
                + ".card.pass{border-left-color:var(--pass)}.card.fail{border-left-color:var(--fail)}"
                + ".card.skip{border-left-color:var(--skip)}"
                + ".head{display:flex;gap:10px;align-items:baseline;flex-wrap:wrap}"
                + ".badge{font-size:11px;font-weight:700;letter-spacing:.05em;padding:2px 8px;border-radius:999px;"
                + "border:1px solid currentColor}.pass .badge{color:var(--pass)}.fail .badge{color:var(--fail)}"
                + ".skip .badge{color:var(--skip)}.title{font-weight:600;flex:1;min-width:0}"
                + ".dur{color:var(--muted);font-size:13px}.method{color:var(--muted);font-size:13px;margin:2px 0 10px;"
                + "font-family:ui-monospace,Consolas,monospace}"
                + ".err{background:rgba(179,38,30,.1);color:var(--fail);padding:10px;border-radius:8px;"
                + "white-space:pre-wrap;font-size:13px;margin:0 0 10px}"
                + ".card img{display:block;max-width:100%;border:1px solid var(--border);border-radius:8px}";
    }
}
