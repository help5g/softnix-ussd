package com.softnix.ussd;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

/**
 * The main public API of the SoftNix USSD library.
 *
 * Basic usage:
 *
 *   UssdDialer.getInstance(context).dial("*123#", 0, new UssdCallback() {
 *       public void onResponse(String message) { ... menu, optionally respond("1", this) ... }
 *       public void onFinish(String message)   { ... final result -- do whatever you want ... }
 *       public void onError(String error)      { ... handle error ... }
 *   });
 *
 * For multi-step USSD, call respond("1", callback) from inside onResponse().
 *
 * This class does NOT touch any server or storage. It only dials and returns
 * results through the callback. What you do with the result is entirely yours.
 */
public class UssdDialer {

    private static UssdDialer instance;

    private final Context appContext;
    private static UssdConfig config = new UssdConfig();

    private static volatile boolean running = false;
    private static volatile boolean awaitingReply = false;

    // Guard: prevents the same on-screen menu from firing the callback more
    // than once (the system sends several accessibility events per dialog).
    private static volatile boolean menuDelivered = false;

    private static UssdCallback invokeCallback;  // for dial() / onResponse / onFinish
    private static UssdCallback replyCallback;   // for respond()

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private UssdDialer(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /** Get the singleton instance. */
    public static synchronized UssdDialer getInstance(Context context) {
        if (instance == null) {
            instance = new UssdDialer(context);
        }
        return instance;
    }

    /** Replace the default config (keywords, dialog classes). Optional. */
    public void setConfig(UssdConfig newConfig) {
        if (newConfig != null) {
            config = newConfig;
        }
    }

    /** Get the current config so you can tweak keywords/dialog classes. */
    public UssdConfig getConfigInstance() {
        return config;
    }

    /**
     * Dial a USSD code on the given SIM slot.
     *
     * @param ussdCode e.g. "*123#"
     * @param simSlot  0 = SIM1, 1 = SIM2
     * @param callback receives onResponse / onFinish / onError
     */
    @SuppressLint("MissingPermission")
    public void dial(String ussdCode, int simSlot, UssdCallback callback) {
        invokeCallback = callback;
        replyCallback = null;
        awaitingReply = false;
        menuDelivered = false;

        // Validate USSD code.
        if (ussdCode == null || ussdCode.trim().isEmpty()) {
            deliverError("USSD code is empty");
            return;
        }

        // Check CALL_PHONE permission.
        if (appContext.checkSelfPermission(Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            deliverError("CALL_PHONE permission not granted");
            return;
        }

        // Check accessibility service is enabled.
        if (!UssdPermissionHelper.isAccessibilityEnabled(appContext)) {
            deliverError("Accessibility service not enabled");
            return;
        }

        running = true;

        try {
            Intent intent = SimSlotHelper.buildDialIntent(appContext, ussdCode, simSlot);
            appContext.startActivity(intent);
        } catch (Exception e) {
            running = false;
            deliverError("Failed to start dial: " + e.getMessage());
        }
    }

    /**
     * Reply to a multi-step USSD menu (call this from onResponse).
     *
     * @param text     the option to send, e.g. "1"
     * @param callback receives the next onResponse / onFinish / onError
     */
    public void respond(String text, UssdCallback callback) {
        replyCallback = callback;
        awaitingReply = true;
        menuDelivered = false;   // ready to receive the NEXT menu
        UssdAccessibilityService.sendReply(text);
    }

    /**
     * Reply to a multi-step USSD menu after a small delay (milliseconds).
     * Useful if a device shows the next menu slightly late and you want a
     * little breathing room before sending the next option.
     *
     * @param text       the option to send, e.g. "1"
     * @param delayMs    delay in milliseconds before sending
     * @param callback   receives the next onResponse / onFinish / onError
     */
    public void respondDelayed(final String text, long delayMs, final UssdCallback callback) {
        mainHandler.postDelayed(new Runnable() {
            public void run() {
                respond(text, callback);
            }
        }, delayMs);
    }

    /** Cancel the current USSD session and close any open dialog. */
    public void cancel() {
        UssdAccessibilityService.cancelDialog();
        running = false;
        awaitingReply = false;
        menuDelivered = false;
    }

    // ---------- internal state accessors used by the accessibility service ----------

    static boolean isRunning() {
        return running;
    }

    static boolean isAwaitingReply() {
        return awaitingReply;
    }

    static UssdConfig getConfig() {
        return config;
    }

    static Context getAppContext() {
        return instance != null ? instance.appContext : null;
    }

    // ---------- result delivery (always on main thread) ----------

    static void deliverResponse(final String message) {
        if (menuDelivered) return;       // ignore duplicate events for the same menu
        menuDelivered = true;
        final UssdCallback cb = invokeCallback;
        if (cb != null) {
            postToMain(new Runnable() {
                public void run() { cb.onResponse(message); }
            });
        }
    }

    static void deliverReplyResponse(final String message) {
        if (menuDelivered) return;       // ignore duplicate events for the same menu
        menuDelivered = true;
        final UssdCallback cb = (replyCallback != null) ? replyCallback : invokeCallback;
        if (cb != null) {
            postToMain(new Runnable() {
                public void run() { cb.onResponse(message); }
            });
        }
    }

    static void deliverFinish(final String message) {
        running = false;
        awaitingReply = false;
        menuDelivered = false;
        // Make sure no popup stays open at the end of the session.
        UssdAccessibilityService.cancelDialog();
        final UssdCallback cb = (replyCallback != null) ? replyCallback : invokeCallback;
        if (cb != null) {
            postToMain(new Runnable() {
                public void run() { cb.onFinish(message); }
            });
        }
    }

    static void deliverError(final String error) {
        running = false;
        awaitingReply = false;
        menuDelivered = false;
        // Close any leftover popup on error too.
        UssdAccessibilityService.cancelDialog();
        final UssdCallback cb = (replyCallback != null) ? replyCallback : invokeCallback;
        if (cb != null) {
            postToMain(new Runnable() {
                public void run() { cb.onError(error); }
            });
        }
    }

    private static void postToMain(Runnable r) {
        new Handler(Looper.getMainLooper()).post(r);
    }
}