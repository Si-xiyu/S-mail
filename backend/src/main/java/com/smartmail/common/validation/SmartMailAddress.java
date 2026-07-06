package com.smartmail.common.validation;

import java.util.Locale;
import java.util.regex.Pattern;

public final class SmartMailAddress {
    public static final String DOMAIN = "smail.com";
    public static final String REGEX = "(?i)^[a-z0-9._%+-]+@smail\\.com$";
    private static final Pattern ALLOWED = Pattern.compile(REGEX);

    private SmartMailAddress() {
    }

    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isAllowed(String email) {
        return ALLOWED.matcher(normalize(email)).matches();
    }
}
