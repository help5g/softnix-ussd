package com.softnix.ussd;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * Helper for checking and requesting the permissions this library needs.
 * Keeps integration simple for the app developer.
 */
public class UssdPermissionHelper {

    /**
     * Check whether THIS app's USSD accessibility service is enabled.
     */
    public static boolean isAccessibilityEnabled(Context context) {
        int enabled = 0;
        try {
            enabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }

        if (enabled != 1) {
            return false;
        }

        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (enabledServices == null) {
            return false;
        }

        // Our service id contains this app's package name + the service class name.
        String packageName = context.getPackageName();
        TextUtils.SimpleStringSplitter splitter =
                new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabledServices);
        while (splitter.hasNext()) {
            String service = splitter.next();
            if (service.toLowerCase().contains(packageName.toLowerCase())
                    && service.toLowerCase().contains("ussdaccessibilityservice")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Open the system Accessibility settings screen so the user can enable the service.
     * Call this if isAccessibilityEnabled() returns false.
     */
    public static void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}