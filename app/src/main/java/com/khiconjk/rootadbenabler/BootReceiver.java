package com.khiconjk.rootadbenabler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String action = intent.getAction();

        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("root_adb_enabler", Context.MODE_PRIVATE);

        boolean bootUsb = prefs.getBoolean(MainActivity.PREF_BOOT_USB, false);
        boolean bootTrustPc = prefs.getBoolean(MainActivity.PREF_BOOT_TRUST_PC, false);
        String savedKey = prefs.getString(MainActivity.PREF_SAVED_PC_KEY, "");

        if (!bootUsb && !bootTrustPc) {
            return;
        }

        StringBuilder script = new StringBuilder();
        script.append("sleep 8\n");

        if (bootTrustPc) {
            if (!TextUtils.isEmpty(savedKey)) {
                script.append(AdbCommands.trustPcAlways(savedKey));
            } else {
                script.append(AdbCommands.autoBootRecoverFromSystemEtcKey());
            }
        }

        if (bootUsb) {
            script.append(AdbCommands.enableUsbAdb());
        }

        SuRunner.run(script.toString());
    }
}
