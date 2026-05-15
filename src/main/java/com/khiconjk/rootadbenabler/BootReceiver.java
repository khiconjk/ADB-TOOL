package com.khiconjk.rootadbenabler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

public class BootReceiver extends BroadcastReceiver {
    private static final String PREFS = "root_adb_enabler";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        boolean bootAction = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        if (!bootAction) return;

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean bootUsb = prefs.getBoolean(MainActivity.PREF_BOOT_USB, false);
        boolean bootTcp = prefs.getBoolean(MainActivity.PREF_BOOT_TCP, false);
        boolean bootTrustPc = prefs.getBoolean(MainActivity.PREF_BOOT_TRUST_PC, false);
        String savedKey = prefs.getString(MainActivity.PREF_SAVED_PC_KEY, "");

        StringBuilder cmd = new StringBuilder();

        // Luôn thử khôi phục từ /system/etc/adbkey.pub khi app còn tồn tại.
        // Sau factory reset, SharedPreferences trong /data có thể mất, nên không phụ thuộc vào switch.
        cmd.append(AdbCommands.autoBootRecoverFromSystemEtcKey()).append('\n');

        if (bootTrustPc) {
            if (!TextUtils.isEmpty(savedKey)) {
                cmd.append(AdbCommands.trustPcAlways(savedKey)).append('\n');
            } else {
                cmd.append(AdbCommands.reapplyBestTrustKey()).append('\n');
            }
        }
        if (bootUsb) {
            cmd.append(AdbCommands.enableUsbAdb()).append('\n');
        }
        if (bootTcp) {
            cmd.append(AdbCommands.enableTcpAdb()).append('\n');
        }

        SuRunner.run(cmd.toString());
    }
}
