package com.khiconjk.rootadbenabler;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_PICK_ADB_KEY_IMPORT_ONLY = 1001;
    private static final int REQ_PICK_ADB_KEY_TRUST_ALWAYS = 1002;

    private static final String PREFS = "root_adb_enabler";
    static final String PREF_BOOT_USB = "boot_usb_adb";
    static final String PREF_BOOT_TRUST_PC = "boot_trust_pc";
    static final String PREF_SAVED_PC_KEY = "saved_pc_adb_pub_key";

    private TextView logView;
    private Button btnEnableUsb;
    private Button btnImportKey;
    private Button btnTrustAlways;
    private Button btnTrustSystemKey;
    private Button btnReapplyTrust;
    private Button btnRestart;
    private Button btnStatus;
    private Button btnCopyTrillImg;
    private Switch swBootUsb;
    private Switch swBootTrust;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();

        appendLog("Root ADB Enabler\n");
        appendLog("- Cấp root cho app khi KernelSU/Magisk hỏi.\n");
        appendLog("- Đã bỏ chức năng ADB Wi-Fi/TCP 5555.\n");
        appendLog("- Đã bỏ chức năng xóa key trust đã lưu.\n");
        appendLog("- Nếu đã đặt key tại /system/etc/adbkey.pub: bấm Trust từ /system/etc/adbkey.pub.\n");
        appendLog("- Copy Trill WebView img sẽ xuất ra /data/media/0/RootAdbEnabler.\n");
        appendLog("- Sau factory reset: key trong /system còn, nhưng app cũng phải được cài dạng system app/priv-app để tự chạy lại.\n\n");
    }

    private void buildUi() {
        int pad = dp(14);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Root ADB Enabler SystemKey");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(10));
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        btnEnableUsb = makeButton("Bật ADB USB");
        btnImportKey = makeButton("Import adbkey.pub 1 lần");
        btnTrustAlways = makeButton("Trust PC luôn / tự khôi phục sau reboot");
        btnTrustSystemKey = makeButton("Trust từ /system/etc/adbkey.pub");
        btnReapplyTrust = makeButton("Khôi phục trust PC tự động");
        btnRestart = makeButton("Restart adbd");
        btnStatus = makeButton("Kiểm tra trạng thái");
        btnCopyTrillImg = makeButton("Copy Trill WebView img ra bộ nhớ trong");

        root.addView(btnEnableUsb);
        root.addView(btnImportKey);
        root.addView(btnTrustAlways);
        root.addView(btnTrustSystemKey);
        root.addView(btnReapplyTrust);
        root.addView(btnRestart);
        root.addView(btnStatus);
        root.addView(btnCopyTrillImg);

        swBootUsb = makeSwitch("Tự bật ADB USB sau khi khởi động");
        swBootTrust = makeSwitch("Tự phục hồi trust PC sau khi khởi động");

        swBootUsb.setChecked(prefs.getBoolean(PREF_BOOT_USB, false));
        swBootTrust.setChecked(prefs.getBoolean(PREF_BOOT_TRUST_PC, false));

        root.addView(swBootUsb);
        root.addView(swBootTrust);

        logView = new TextView(this);
        logView.setTextSize(13);
        logView.setTextIsSelectable(true);
        logView.setPadding(dp(10), dp(10), dp(10), dp(10));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(logView, new ScrollView.LayoutParams(-1, -2));

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
        scrollParams.setMargins(0, dp(10), 0, 0);
        root.addView(scroll, scrollParams);

        setContentView(root);

        btnEnableUsb.setOnClickListener(v -> runRoot("Bật ADB USB", AdbCommands.enableUsbAdb()));
        btnImportKey.setOnClickListener(v -> pickAdbKey(REQ_PICK_ADB_KEY_IMPORT_ONLY));
        btnTrustAlways.setOnClickListener(v -> pickAdbKey(REQ_PICK_ADB_KEY_TRUST_ALWAYS));
        btnTrustSystemKey.setOnClickListener(v -> trustFromSystemKey());
        btnReapplyTrust.setOnClickListener(v -> reapplySavedTrust());
        btnRestart.setOnClickListener(v -> runRoot("Restart adbd", AdbCommands.restartAdbd()));
        btnStatus.setOnClickListener(v -> runRoot("Kiểm tra trạng thái", AdbCommands.status()));
        btnCopyTrillImg.setOnClickListener(v -> runRoot("Copy Trill WebView img ra bộ nhớ trong", AdbCommands.copyTrillWebviewImg()));

        swBootUsb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(PREF_BOOT_USB, isChecked).apply();
                appendLog("Auto USB after boot: " + isChecked + "\n");
            }
        });

        swBootTrust.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(PREF_BOOT_TRUST_PC, isChecked).apply();
                appendLog("Auto trust PC after boot: " + isChecked + "\n");
            }
        });
    }

    private Button makeButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(4), 0, dp(4));
        b.setLayoutParams(lp);

        return b;
    }

    private Switch makeSwitch(String text) {
        Switch s = new Switch(this);
        s.setText(text);
        s.setTextSize(14);
        s.setPadding(0, dp(8), 0, dp(4));
        s.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return s;
    }

    private void pickAdbKey(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        try {
            startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            Toast.makeText(this, "Không mở được trình chọn file", Toast.LENGTH_LONG).show();
            appendLog("ERROR open picker: " + e.getMessage() + "\n");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == REQ_PICK_ADB_KEY_IMPORT_ONLY || requestCode == REQ_PICK_ADB_KEY_TRUST_ALWAYS)
                && resultCode == RESULT_OK
                && data != null) {

            Uri uri = data.getData();

            if (uri == null) {
                appendLog("Không lấy được URI adbkey.pub\n");
                return;
            }

            try {
                String key = readTextFromUri(uri).trim();
                key = firstNonEmptyLine(key);

                if (!looksLikeAdbPublicKey(key)) {
                    appendLog("File không giống adbkey.pub hợp lệ. Nội dung cần là public key ADB của PC.\n");
                    return;
                }

                if (requestCode == REQ_PICK_ADB_KEY_TRUST_ALWAYS) {
                    prefs.edit()
                            .putString(PREF_SAVED_PC_KEY, key)
                            .putBoolean(PREF_BOOT_TRUST_PC, true)
                            .putBoolean(PREF_BOOT_USB, true)
                            .apply();

                    swBootTrust.setChecked(true);
                    swBootUsb.setChecked(true);

                    runRoot("Trust PC luôn", AdbCommands.trustPcAlways(key));
                } else {
                    runRoot("Import adbkey.pub 1 lần", AdbCommands.importAdbPublicKey(key));
                }
            } catch (Exception e) {
                appendLog("ERROR read adbkey.pub: " + e.getMessage() + "\n");
            }
        }
    }

    private void trustFromSystemKey() {
        prefs.edit()
                .putBoolean(PREF_BOOT_TRUST_PC, true)
                .putBoolean(PREF_BOOT_USB, true)
                .apply();

        swBootTrust.setChecked(true);
        swBootUsb.setChecked(true);

        runRoot("Trust từ /system/etc/adbkey.pub", AdbCommands.trustFromSystemEtcKey(true));
    }

    private void reapplySavedTrust() {
        String key = prefs.getString(PREF_SAVED_PC_KEY, "");

        if (!TextUtils.isEmpty(key) && looksLikeAdbPublicKey(key)) {
            runRoot("Khôi phục trust PC đã lưu trong app", AdbCommands.trustPcAlways(key));
        } else {
            runRoot("Khôi phục trust PC tự động", AdbCommands.reapplyBestTrustKey());
        }
    }

    private String readTextFromUri(Uri uri) throws Exception {
        StringBuilder sb = new StringBuilder();

        try (InputStream in = getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {

            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }

        return sb.toString();
    }

    private String firstNonEmptyLine(String text) {
        if (text == null) return "";

        String[] lines = text.replace("\r", "").split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }

        return "";
    }

    private boolean looksLikeAdbPublicKey(String key) {
        if (TextUtils.isEmpty(key)) return false;
        if (key.length() < 80) return false;
        if (key.contains("-----BEGIN")) return false;
        if (key.contains("PRIVATE KEY")) return false;
        if (key.contains("\n") || key.contains("\r")) return false;

        return true;
    }

    private void runRoot(String title, String command) {
        setButtonsEnabled(false);
        appendLog("\n===== " + title + " =====\n");

        new Thread(() -> {
            SuRunner.Result result = SuRunner.run(command);

            runOnUiThread(() -> {
                appendLog(result.output);
                appendLog("Exit code: " + result.exitCode + "\n");

                if (!result.ok()) {
                    appendLog("FAILED. Kiểm tra app đã được cấp root chưa.\n");
                }

                setButtonsEnabled(true);
            });
        }).start();
    }

    private void setButtonsEnabled(boolean enabled) {
        Button[] buttons = {
                btnEnableUsb,
                btnImportKey,
                btnTrustAlways,
                btnTrustSystemKey,
                btnReapplyTrust,
                btnRestart,
                btnStatus,
                btnCopyTrillImg
        };

        for (Button b : buttons) {
            if (b != null) {
                b.setEnabled(enabled);
            }
        }
    }

    private void appendLog(String text) {
        if (logView == null) return;

        logView.append(text);

        final View parent = (View) logView.getParent();

        if (parent instanceof ScrollView) {
            parent.post(() -> ((ScrollView) parent).fullScroll(View.FOCUS_DOWN));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
