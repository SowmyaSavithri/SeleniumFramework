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
import java.util.Locale;

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
        // Order by test-case number (TC01, TC02 ... TC16), regardless of which class a test lives in.
        all.sort(Comparator.comparingInt(HtmlReportListener::tcNumber)
                .thenComparingLong(ITestResult::getStartMillis));

        long pass = all.stream().filter(r -> r.getStatus() == ITestResult.SUCCESS).count();
        long fail = all.stream().filter(r -> r.getStatus() == ITestResult.FAILURE).count();
        long skip = all.size() - pass - fail;
        double rate = all.isEmpty() ? 0.0 : pass * 100.0 / all.size();

        String browser = ConfigReader.getOrDefault("browser", "chrome");
        String tcFilter = System.getProperty("tc");
        String scope = tcFilter == null || tcFilter.trim().isEmpty()
                ? "full" : "tc=" + tcFilter.trim().replace(',', ';');
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<String[]> history = appendHistory(stamp, browser, scope, all.size(), pass, fail, skip, rate);

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
                .append("</b>Skipped</div><div class=\"tile ").append(rate >= PASS_RATE_TARGET ? "pass" : "fail")
                .append("\"><b>").append(String.format(Locale.US, "%.1f%%", rate))
                .append("</b>Pass rate (target &ge; ").append((int) PASS_RATE_TARGET).append("%)</div></div>");

        appendSummaryTable(html, all);

        html.append("<h2>Details and screenshots</h2><p class=\"meta\">One card per test case, in TC order. ")
                .append("Each test lists its steps in plain English, with the screenshot taken right after the step. ")
                .append("Failed tests also show a screenshot of the moment of failure.</p>");
        for (ITestResult r : all) {
            appendCard(html, r);
        }

        appendHistorySection(html, history);
        html.append("</main></body></html>");

        try {
            Path out = Paths.get("target", "reports", "test-report.html");
            Files.createDirectories(out.getParent());
            Files.write(out, html.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("HTML report with screenshots: " + out.toAbsolutePath());

            // Timestamped copy outside target/, so "mvn clean" never deletes earlier runs.
            Path copy = Paths.get("run-reports",
                    stamp.replace(':', '-').replace(' ', '_') + "_" + scope.replace('=', '-').replace(';', '_')
                            + "_test-report.html");
            Files.createDirectories(copy.getParent());
            Files.copy(out, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println(String.format(Locale.US, "Pass rate: %.1f%% (%d of %d)  |  saved copy: %s",
                    rate, pass, all.size(), copy.toAbsolutePath()));
        } catch (IOException e) {
            System.err.println("Could not write HTML report: " + e.getMessage());
        }
    }

    /** Number from the "TCnn" prefix of the test description; tests without one sort last. */
    private static int tcNumber(ITestResult r) {
        String d = r.getMethod().getDescription();
        if (d != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)^\\s*TC0*(\\d+)").matcher(d);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        return Integer.MAX_VALUE;
    }

    private static final double PASS_RATE_TARGET = 90.0;
    private static final Path HISTORY_FILE = Paths.get("run-reports", "run-history.csv");

    /** Appends this run to run-reports/run-history.csv and returns all rows (including this run). */
    private static List<String[]> appendHistory(String stamp, String browser, String scope,
                                                long total, long pass, long fail, long skip, double rate) {
        List<String[]> rows = new ArrayList<>();
        try {
            Files.createDirectories(HISTORY_FILE.getParent());
            if (Files.exists(HISTORY_FILE)) {
                for (String line : Files.readAllLines(HISTORY_FILE, StandardCharsets.UTF_8)) {
                    if (!line.trim().isEmpty() && !line.startsWith("timestamp")) {
                        rows.add(line.split(",", -1));
                    }
                }
            }
            rows.add(new String[]{stamp, browser, scope, String.valueOf(total), String.valueOf(pass),
                    String.valueOf(fail), String.valueOf(skip), String.format(Locale.US, "%.1f", rate)});
            List<String> out = new ArrayList<>();
            out.add("timestamp,browser,scope,total,passed,failed,skipped,passRate");
            for (String[] row : rows) {
                out.add(String.join(",", row));
            }
            Files.write(HISTORY_FILE, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not update run history: " + e.getMessage());
        }
        return rows;
    }

    /** Shows the last three FULL-suite runs and whether the 90%-on-three-consecutive-runs target is met. */
    private static void appendHistorySection(StringBuilder html, List<String[]> history) {
        List<String[]> full = new ArrayList<>();
        for (String[] row : history) {
            if (row.length >= 8 && "full".equals(row[2])) {
                full.add(row);
            }
        }
        html.append("<h2>Three-run evidence: pass rate of the last 3 full-suite runs</h2>");
        if (full.isEmpty()) {
            html.append("<p class=\"meta\">No full-suite run recorded yet (this was a filtered -Dtc run).</p>");
            return;
        }
        List<String[]> last = full.subList(Math.max(0, full.size() - 3), full.size());
        boolean allMet = last.size() == 3;
        html.append("<div class=\"card\"><table><tr><th>Run</th><th>Browser</th><th>Passed / Total</th>")
                .append("<th>Pass rate</th><th>vs target</th></tr>");
        for (String[] row : last) {
            double r = Double.parseDouble(row[7]);
            boolean ok = r >= PASS_RATE_TARGET;
            allMet &= ok;
            html.append("<tr><td>").append(esc(row[0])).append("</td><td>").append(esc(row[1]))
                    .append("</td><td>").append(esc(row[4])).append(" / ").append(esc(row[3]))
                    .append("</td><td>").append(esc(row[7])).append("%</td><td>")
                    .append(ok ? "Met" : "Below").append("</td></tr>");
        }
        html.append("</table><p class=\"meta\" style=\"margin-top:10px\">");
        if (allMet) {
            html.append("<b>Target met:</b> pass rate is at least ").append((int) PASS_RATE_TARGET)
                    .append("% on 3 consecutive full-suite runs.");
        } else {
            html.append("Target not met yet: ").append(last.size()).append(" of 3 full-suite runs recorded");
            html.append(last.size() == 3 ? ", and at least one is below " : "; each must be at least ")
                    .append((int) PASS_RATE_TARGET).append("%.");
        }
        html.append("</p></div>");
    }

    private static String statusOf(ITestResult r) {
        return r.getStatus() == ITestResult.SUCCESS ? "pass" : r.getStatus() == ITestResult.FAILURE ? "fail" : "skip";
    }

    private static String descriptionOf(ITestResult r) {
        String d = r.getMethod().getDescription();
        return d != null && !d.isEmpty() ? d : r.getName();
    }

    /** "TC05" from "TC05 [Negative] Empty password is rejected". */
    private static String idOf(ITestResult r) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)^\\s*(TC\\d+)").matcher(descriptionOf(r));
        return m.find() ? m.group(1).toUpperCase() : "-";
    }

    /** "Negative" from "TC05 [Negative] Empty password is rejected". */
    private static String typeOf(ITestResult r) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[([^\\]]+)\\]").matcher(descriptionOf(r));
        return m.find() ? m.group(1) : "-";
    }

    /** "Empty password is rejected" from "TC05 [Negative] Empty password is rejected". */
    private static String titleOf(ITestResult r) {
        return descriptionOf(r).replaceFirst("(?i)^\\s*TC\\d+\\s*", "").replaceFirst("^\\[[^\\]]+\\]\\s*", "");
    }

    /** One-page overview: every test case in TC order with its result; links jump to the details below. */
    private static void appendSummaryTable(StringBuilder html, List<ITestResult> all) {
        html.append("<h2>Results at a glance</h2><div class=\"card\"><table class=\"results\"><tr><th>TC</th>")
                .append("<th>Type</th><th>Scenario</th><th>Result</th><th>Time</th><th></th></tr>");
        for (ITestResult r : all) {
            String status = statusOf(r);
            String id = idOf(r);
            html.append("<tr><td><b>").append(esc(id)).append("</b></td><td>").append(esc(typeOf(r)))
                    .append("</td><td>").append(esc(titleOf(r))).append("</td><td class=\"")
                    .append(status).append("\"><span class=\"badge\">").append(status.toUpperCase())
                    .append("</span></td><td>")
                    .append(String.format(Locale.US, "%.1f s", (r.getEndMillis() - r.getStartMillis()) / 1000.0))
                    .append("</td><td><a href=\"#").append(esc(id)).append("\">Steps</a></td></tr>");
        }
        html.append("</table></div>");
    }

    private static void appendCard(StringBuilder html, ITestResult r) {
        String status = statusOf(r);
        long ms = r.getEndMillis() - r.getStartMillis();

        html.append("<section class=\"card ").append(status).append("\" id=\"").append(esc(idOf(r)))
                .append("\"><div class=\"head\"><span class=\"badge\">")
                .append(status.toUpperCase()).append("</span><span class=\"title\">").append(esc(idOf(r)))
                .append(" &middot; ").append(esc(titleOf(r)))
                .append("</span><span class=\"dur\">").append(String.format(Locale.US, "%.1f s", ms / 1000.0))
                .append("</span></div>");
        html.append("<div class=\"method\">").append(esc(r.getTestClass().getRealClass().getSimpleName()))
                .append(".").append(esc(r.getName())).append("()</div>");

        Throwable t = r.getThrowable();
        if (t != null) {
            html.append("<pre class=\"err\">").append(esc(String.valueOf(t.getMessage()))).append("</pre>");
        }

        // Numbered steps, each with its description and the screenshot taken right after it.
        @SuppressWarnings("unchecked")
        List<ReportStep> steps = (List<ReportStep>) r.getAttribute(BaseTest.STEPS_ATTRIBUTE);
        boolean hasSteps = steps != null && !steps.isEmpty();
        if (hasSteps) {
            html.append("<ol class=\"steps\">");
            for (ReportStep s : steps) {
                html.append("<li><div class=\"step-text\">").append(esc(s.description())).append("</div>");
                if (s.imageBase64() != null) {
                    html.append("<img alt=\"Step screenshot\" src=\"data:image/jpeg;base64,")
                            .append(s.imageBase64()).append("\">");
                }
                html.append("</li>");
            }
            html.append("</ol>");
        }

        // Final-state screenshot: always for failed/skipped tests, and for tests that recorded no steps.
        Object shot = r.getAttribute(BaseTest.SCREENSHOT_ATTRIBUTE);
        if (shot != null && (r.getStatus() != ITestResult.SUCCESS || !hasSteps)) {
            try {
                String data = Base64.getEncoder().encodeToString(Files.readAllBytes(Paths.get(shot.toString())));
                html.append("<details open><summary>")
                        .append(r.getStatus() == ITestResult.SUCCESS ? "Screenshot at the end of the test"
                                : "Screenshot at the moment of failure")
                        .append("</summary><img alt=\"Screenshot\" src=\"data:image/png;base64,").append(data)
                        .append("\"></details>");
            } catch (IOException e) {
                html.append("<p class=\"meta\">Screenshot file missing.</p>");
            }
        } else if (shot == null && !hasSteps) {
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
                + "table{width:100%;border-collapse:collapse;font-size:14px}"
                + "th,td{text-align:left;padding:6px 8px;border-bottom:1px solid var(--border)}"
                + "th{color:var(--muted);font-size:12px;text-transform:uppercase}"
                + "table.results td.pass .badge{color:var(--pass)}table.results td.fail .badge{color:var(--fail)}"
                + "table.results td.skip .badge{color:var(--skip)}a{color:inherit}"
                + "details summary{cursor:pointer;color:var(--muted);font-size:13px;margin:4px 0 8px}"
                + ".card{scroll-margin-top:12px}"
                + "ol.steps{list-style:none;counter-reset:s;margin:8px 0 0;padding:0}"
                + "ol.steps>li{counter-increment:s;display:flex;gap:14px;align-items:flex-start;flex-wrap:wrap;"
                + "padding:10px 0;border-top:1px solid var(--border)}"
                + ".step-text{flex:1 1 220px;min-width:0;font-size:14px}"
                + ".step-text::before{content:'Step ' counter(s) ': ';font-weight:700;color:var(--muted)}"
                + "ol.steps img{flex:0 1 460px;max-width:100%;border:1px solid var(--border);border-radius:8px}"
                + ".card img{display:block;max-width:100%;border:1px solid var(--border);border-radius:8px}";
    }
}
