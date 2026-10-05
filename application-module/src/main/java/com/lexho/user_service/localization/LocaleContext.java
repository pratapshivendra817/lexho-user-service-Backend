package com.lexho.user_service.localization;

public final class LocaleContext {

    private static final ThreadLocal<String> CURRENT_LOCALE = new ThreadLocal<>();

    private LocaleContext() {
    }

    public static void setLocale(String locale) {
        CURRENT_LOCALE.set(locale);
    }

    public static String getLocale() {
        return CURRENT_LOCALE.get();
    }

    public static void clear() {
        CURRENT_LOCALE.remove();
    }
}
