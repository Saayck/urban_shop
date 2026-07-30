package com.urban_shop.backend.common.security;

import java.util.regex.Pattern;

public final class XssSanitizer {

    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SRC_PATTERN = Pattern.compile("src[\r\n]*=[\r\n]*['\"](?:[^'\"]*)['\"]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LONELY_SCRIPT_TAG_PATTERN = Pattern.compile("</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern START_SCRIPT_TAG_PATTERN = Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EVAL_PATTERN = Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern VBSTATIC_PATTERN = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern ONLOAD_PATTERN = Pattern.compile("onload(.*?)=\\s*['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ONERROR_PATTERN = Pattern.compile("onerror(.*?)=\\s*['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private XssSanitizer() {
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value;
        sanitized = SCRIPT_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = LONELY_SCRIPT_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = START_SCRIPT_TAG_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = SRC_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EVAL_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = EXPRESSION_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = VBSTATIC_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONLOAD_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = ONERROR_PATTERN.matcher(sanitized).replaceAll("");

        return sanitized;
    }
}
