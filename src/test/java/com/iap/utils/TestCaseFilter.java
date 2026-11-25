package com.iap.utils;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs only the test cases named in -Dtc, matched against the "TCnn" at the start of each test's description.
 * Examples:  -Dtc=TC01   -Dtc=TC01,TC05   -Dtc=TC08-TC11   -Dtc=TC01,TC08-TC11,TC15
 * With no -Dtc, every test runs.
 */
public class TestCaseFilter implements IMethodInterceptor {

    private static final Pattern TC_ID = Pattern.compile("(?i)^\\s*TC0*(\\d+)");
    private static final Pattern RANGE = Pattern.compile("(?i)^TC0*(\\d+)\\s*-\\s*(?:TC)?0*(\\d+)$");
    private static final Pattern SINGLE = Pattern.compile("(?i)^(?:TC)?0*(\\d+)$");

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        String selection = System.getProperty("tc");
        if (selection == null || selection.trim().isEmpty()) {
            return methods;
        }
        Set<Integer> wanted = parse(selection);

        List<IMethodInstance> selected = new ArrayList<>();
        for (IMethodInstance m : methods) {
            Integer id = idOf(m.getMethod().getDescription());
            if (id != null && wanted.contains(id)) {
                selected.add(m);
            }
        }
        System.out.println("Test-case filter -Dtc=" + selection + " -> running " + selected.size() + " test(s)");
        return selected;
    }

    private static Integer idOf(String description) {
        if (description == null) {
            return null;
        }
        Matcher m = TC_ID.matcher(description);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    private static Set<Integer> parse(String selection) {
        Set<Integer> ids = new HashSet<>();
        for (String raw : selection.split(",")) {
            String part = raw.trim();
            Matcher range = RANGE.matcher(part);
            Matcher single = SINGLE.matcher(part);
            if (range.matches()) {
                int from = Integer.parseInt(range.group(1));
                int to = Integer.parseInt(range.group(2));
                for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
                    ids.add(i);
                }
            } else if (single.matches()) {
                ids.add(Integer.parseInt(single.group(1)));
            } else if (!part.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid -Dtc value '" + part + "'. Use e.g. TC01, TC01,TC05 or TC08-TC11.");
            }
        }
        return ids;
    }
}
