package com.iap.utils;

/** One recorded test step: what was done, and a (compressed) screenshot taken right after it. */
public record ReportStep(String description, String imageBase64) {
}
