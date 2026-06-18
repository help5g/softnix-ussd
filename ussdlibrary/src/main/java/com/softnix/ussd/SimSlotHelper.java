package com.softnix.ussd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;

import java.util.List;

/**
 * Builds the ACTION_CALL intent for dialing a USSD code on a specific SIM slot.
 *
 * Handles the many manufacturer-specific "sim slot" intent extras so that
 * dual-SIM devices pick the right SIM. This is internal; you normally don't
 * call this directly -- UssdDialer uses it.
 */
public class SimSlotHelper {

    // Different manufacturers read the SIM slot from different extra keys.
    // We set them all; the right device reads the right one.
    private static final String[] SIM_SLOT_KEYS = {
            "extra_asus_dial_use_dualsim",
            "com.android.phone.extra.slot",
            "slot",
            "simslot",
            "sim_slot",
            "subscription",
            "Subscription",
            "phone",
            "com.android.phone.DialingMode",
            "simSlot",
            "slot_id",
            "simId",
            "simnum",
            "phone_type",
            "slotId",
            "slotIdx"
    };

    /**
     * Build a dial intent for a USSD code on a given SIM slot.
     *
     * @param context  any context
     * @param ussdCode the USSD code, e.g. "*123#"
     * @param simSlot  0 for SIM1, 1 for SIM2
     * @return an Intent ready to startActivity()
     */
    @SuppressLint("MissingPermission")
    public static Intent buildDialIntent(Context context, String ussdCode, int simSlot) {
        // Encode the '#' so the dialer keeps it.
        String encodedHash = Uri.encode("#");
        String dialCode = ussdCode.replace("#", encodedHash);
        Uri uri = Uri.parse("tel:" + dialCode);

        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("com.android.phone.force.slot", true);
        intent.putExtra("Cdma_Supp", true);

        // Set every known SIM-slot extra key.
        for (String key : SIM_SLOT_KEYS) {
            intent.putExtra(key, simSlot);
        }

        // The reliable, modern way: attach the PhoneAccountHandle for the slot.
        TelecomManager telecomManager =
                (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            try {
                List<PhoneAccountHandle> handles = telecomManager.getCallCapablePhoneAccounts();
                if (handles != null && simSlot >= 0 && handles.size() > simSlot) {
                    intent.putExtra(
                            "android.telecom.extra.PHONE_ACCOUNT_HANDLE",
                            handles.get(simSlot));
                }
            } catch (SecurityException e) {
                // READ_PHONE_STATE not granted; the extras above still work as fallback.
            }
        }

        return intent;
    }
}