package com.softnix.ussd;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * The background accessibility service that reads the system USSD dialog
 * and clicks/types on it automatically.
 *
 * IMPORTANT: this works even when the screen is locked/off, because the USSD
 * dialog is a system telephony window that wakes the screen when shown.
 *
 * This service is fully internal. The app integrating this library does NOT
 * declare it -- it is auto-merged from the library manifest. The user only
 * needs to enable the accessibility service once from system settings.
 *
 * Driven by UssdDialer (the live session state lives there).
 */
public class UssdAccessibilityService extends AccessibilityService {

    // The current live event, kept so we can send replies into the same dialog.
    private static AccessibilityEvent currentEvent;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        currentEvent = event;

        // If no USSD session is running, ignore all events.
        if (!UssdDialer.isRunning()) {
            return;
        }

        UssdConfig config = UssdDialer.getConfig();
        if (config == null) {
            return;
        }

        // Only react to USSD/telephony dialogs.
        if (!config.isUssdDialogClass(event.getClassName())) {
            return;
        }

        String response = event.getText() == null ? "" : event.getText().toString();
        String firstLine = "";
        if (event.getText() != null && !event.getText().isEmpty()
                && event.getText().get(0) != null) {
            firstLine = event.getText().get(0).toString();
        }

        boolean hasInput = hasInputField(event);

        // Case 1: a "waiting/loading" dialog with no input -> dismiss, treat as over.
        if (config.getLoginKeywords().contains(firstLine) && !hasInput) {
            clickButton(event, 0);
            UssdDialer.deliverFinish(response);
            return;
        }

        // Case 2: an error dialog -> dismiss and report finish.
        if (config.getErrorKeywords().contains(firstLine)) {
            clickButton(event, 1);
            UssdDialer.deliverFinish(response);
            return;
        }

        // Case 3: a normal USSD dialog.
        if (!hasInput) {
            // No input field = final message. Press OK and finish.
            clickButton(event, 0);
            UssdDialer.deliverFinish(response);
        } else {
            // Has an input field = menu expecting more input.
            if (UssdDialer.isAwaitingReply()) {
                UssdDialer.deliverReplyResponse(response);
            } else {
                UssdDialer.deliverResponse(response);
            }
        }
    }

    @Override
    public void onInterrupt() {
        // Required override. Nothing to do.
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    // ---------- static helpers used by UssdDialer to send replies ----------

    /** Type text into the USSD input field and press send (button index 1). */
    public static void sendReply(String text) {
        if (currentEvent == null) return;
        setTextIntoField(currentEvent, text);
        clickButton(currentEvent, 1);
    }

    /** Cancel the current USSD dialog (press the first/negative button). */
    public static void cancelDialog() {
        if (currentEvent == null) return;
        clickButton(currentEvent, 0);
    }

    // ---------- internal node helpers ----------

    private static void setTextIntoField(AccessibilityEvent event, String data) {
        Bundle args = new Bundle();
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, data);

        for (AccessibilityNodeInfo leaf : getLeaves(event)) {
            if (leaf.getClassName() != null
                    && leaf.getClassName().equals("android.widget.EditText")) {
                boolean ok = leaf.performAction(
                        AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                if (!ok) {
                    // Fallback: copy to clipboard and paste.
                    Context ctx = UssdDialer.getAppContext();
                    if (ctx != null) {
                        ClipboardManager cm = (ClipboardManager)
                                ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null) {
                            cm.setPrimaryClip(ClipData.newPlainText("ussd", data));
                            leaf.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                        }
                    }
                }
            }
        }
    }

    private static boolean hasInputField(AccessibilityEvent event) {
        for (AccessibilityNodeInfo leaf : getLeaves(event)) {
            if (leaf.getClassName() != null
                    && leaf.getClassName().equals("android.widget.EditText")) {
                return true;
            }
        }
        return false;
    }

    private static void clickButton(AccessibilityEvent event, int index) {
        int count = -1;
        for (AccessibilityNodeInfo leaf : getLeaves(event)) {
            if (leaf.getClassName() != null
                    && leaf.getClassName().toString().toLowerCase().contains("button")) {
                count++;
                if (count == index) {
                    leaf.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }
    }

    private static List<AccessibilityNodeInfo> getLeaves(AccessibilityEvent event) {
        List<AccessibilityNodeInfo> leaves = new ArrayList<>();
        if (event != null && event.getSource() != null) {
            collectLeaves(leaves, event.getSource());
        }
        return leaves;
    }

    private static void collectLeaves(List<AccessibilityNodeInfo> leaves,
                                      AccessibilityNodeInfo node) {
        if (node == null) return;
        if (node.getChildCount() == 0) {
            leaves.add(node);
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            collectLeaves(leaves, node.getChild(i));
        }
    }
}