package com.softnix.ussd;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Configuration holder for the USSD library.
 *
 * Contains the keyword sets used to detect "waiting/loading" and "error" dialogs,
 * and the list of known USSD dialog class names across manufacturers.
 *
 * Everything here has sensible defaults. You only need to touch this if you
 * want to add keywords for your language/operator, or support a new device.
 */
public class UssdConfig {

    // Words that indicate a "waiting/loading" dialog (harmless first screen).
    private HashSet<String> loginKeywords = new HashSet<>(Arrays.asList(
            "espere", "waiting", "loading", "esperando", "wait", "please wait"
    ));

    // Words that indicate an error dialog.
    private HashSet<String> errorKeywords = new HashSet<>(Arrays.asList(
            "problema", "problem", "error", "null", "failed", "unable", "invalid"
    ));

    // Known USSD/telephony dialog class names across different manufacturers.
    private List<String> ussdDialogClasses = Arrays.asList(
            "amigo.app.AmigoAlertDialog",
            "android.app.AlertDialog",
            "androidx.appcompat.app.e",
            "androidx.appcompat.app.AlertDialog",
            "com.android.phone.oppo.settings.LocalAlertDialog",
            "com.zte.mifavor.widget.AlertDialog",
            "color.support.v7.app.AlertDialog",
            "com.transsion.widgetslib.dialog.PromptDialog",
            "miuix.appcompat.app.AlertDialog",
            "com.mediatek.phone.UssdAlertActivity",
            "com.samsung.android.app.dialertab.DialerActivity"
    );

    public HashSet<String> getLoginKeywords() {
        return loginKeywords;
    }

    public void setLoginKeywords(HashSet<String> loginKeywords) {
        this.loginKeywords = loginKeywords;
    }

    public HashSet<String> getErrorKeywords() {
        return errorKeywords;
    }

    public void setErrorKeywords(HashSet<String> errorKeywords) {
        this.errorKeywords = errorKeywords;
    }

    public List<String> getUssdDialogClasses() {
        return ussdDialogClasses;
    }

    public void setUssdDialogClasses(List<String> ussdDialogClasses) {
        this.ussdDialogClasses = ussdDialogClasses;
    }

    /** Check if a given class name is a known USSD dialog. */
    public boolean isUssdDialogClass(CharSequence className) {
        if (className == null) return false;
        String name = className.toString();
        for (String c : ussdDialogClasses) {
            if (c.equals(name)) return true;
        }
        return false;
    }
}