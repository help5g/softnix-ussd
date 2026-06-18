package com.softnix.ussd;

/**
 * Callback interface for USSD operations.
 *
 * The app using this library implements this to receive USSD results.
 * This library does NOT do anything with the data (no server, no storage).
 * Whatever you get here, you decide what to do with it.
 */
public interface UssdCallback {

    /**
     * Called when the USSD dialog shows an intermediate menu/response
     * that still expects more input (multi-step USSD).
     * From here you can call dialer.respond("1", callback) to continue.
     *
     * @param message the text currently shown on the USSD dialog
     */
    void onResponse(String message);

    /**
     * Called when the USSD session is finished (final message, no more input).
     *
     * @param message the final USSD response text
     */
    void onFinish(String message);

    /**
     * Called when something goes wrong (no permission, accessibility off,
     * empty USSD code, failed to start dial, etc.)
     *
     * @param error a short description of what went wrong
     */
    void onError(String error);
}