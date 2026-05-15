package com.khiconjk.rootadbenabler;

public final class AdbCommands {
    private AdbCommands() {}

    private static final String TRUST_DIR = "/data/adb/root_adb_enabler";
    private static final String TRUST_KEY = TRUST_DIR + "/adbkey.pub";
    private static final String SYSTEM_KEY = "/system/etc/adbkey.pub";
    private static final String SYSTEM_KEY_FALLBACK = "/system.etc/adbkey.pub";

    public static String enableUsbAdb() {
        return "set -e\n" +
                enableUsbAdbBody() +
                "echo DONE_ENABLE_USB_ADB\n";
    }

    public static String enableTcpAdb() {
        return "set -e\n" +
                "settings put global development_settings_enabled 1 || true\n" +
                "settings put global adb_enabled 1 || true\n" +
                "setprop service.adb.tcp.port 5555 || true\n" +
                "setprop persist.adb.tcp.port 5555 || true\n" +
                "stop adbd || true\n" +
                "sleep 1\n" +
                "start adbd || true\n" +
                "ip -4 addr show 2>/dev/null | grep -oE 'inet ([0-9]{1,3}\\.){3}[0-9]{1,3}' | awk '{print $2}' || true\n" +
                "echo DONE_ENABLE_TCP_5555\n";
    }

    public static String disableTcpAdb() {
        return "set -e\n" +
                "setprop service.adb.tcp.port -1 || true\n" +
                "setprop persist.adb.tcp.port -1 || true\n" +
                "stop adbd || true\n" +
                "sleep 1\n" +
                "start adbd || true\n" +
                "echo DONE_DISABLE_TCP_ADB\n";
    }

    public static String restartAdbd() {
        return "stop adbd || true\n" +
                "sleep 1\n" +
                "start adbd || true\n" +
                "echo DONE_RESTART_ADBD\n";
    }

    public static String status() {
        return "echo '===== id ====='\n" +
                "id || true\n" +
                "echo '===== settings ====='\n" +
                "echo -n 'development_settings_enabled='; settings get global development_settings_enabled || true\n" +
                "echo -n 'adb_enabled='; settings get global adb_enabled || true\n" +
                "echo '===== props ====='\n" +
                "echo -n 'sys.usb.config='; getprop sys.usb.config || true\n" +
                "echo -n 'persist.sys.usb.config='; getprop persist.sys.usb.config || true\n" +
                "echo -n 'service.adb.tcp.port='; getprop service.adb.tcp.port || true\n" +
                "echo -n 'persist.adb.tcp.port='; getprop persist.adb.tcp.port || true\n" +
                "echo '===== system adbkey source ====='\n" +
                "ls -lZ " + SYSTEM_KEY + " 2>/dev/null || true\n" +
                "ls -lZ " + SYSTEM_KEY_FALLBACK + " 2>/dev/null || true\n" +
                "echo -n 'system_key_sha256='; sha256sum " + SYSTEM_KEY + " 2>/dev/null | awk '{print $1}' || true\n" +
                "echo -n 'system_key_fallback_sha256='; sha256sum " + SYSTEM_KEY_FALLBACK + " 2>/dev/null | awk '{print $1}' || true\n" +
                "echo '===== adb_keys ====='\n" +
                "ls -ldZ /data/misc/adb 2>/dev/null || true\n" +
                "ls -lZ /data/misc/adb/adb_keys 2>/dev/null || true\n" +
                "echo -n 'adb_keys_lines='; wc -l /data/misc/adb/adb_keys 2>/dev/null | awk '{print $1}' || true\n" +
                "echo -n 'adb_keys_sha256='; sha256sum /data/misc/adb/adb_keys 2>/dev/null | awk '{print $1}' || true\n" +
                "echo '===== saved trust key ====='\n" +
                "ls -ldZ " + TRUST_DIR + " 2>/dev/null || true\n" +
                "ls -lZ " + TRUST_KEY + " 2>/dev/null || true\n" +
                "echo -n 'saved_key_sha256='; sha256sum " + TRUST_KEY + " 2>/dev/null | awk '{print $1}' || true\n" +
                "echo '===== adbd pid ====='\n" +
                "pidof adbd 2>/dev/null || ps -A | grep '[a]dbd' || true\n";
    }

    public static String copyTrillWebviewImg() {
        return "set -e\n" +
                "SRC='/data/data/com.ss.android.ugc.trill/app_webview/Default/Cookies'\n" +
                "BASE='/storage/emulated/0/RootAdbEnabler'\n" +
                "STAMP=\"$(date +%Y%m%d_%H%M%S 2>/dev/null || echo now)\"\n" +
                "DEST=\"$BASE/trill_webview_Cookies_$STAMP\"\n" +
                "echo '===== COPY TRILL WEBVIEW Cookies ====='\n" +
                "echo \"SRC=$SRC\"\n" +
                "echo \"DEST=$DEST\"\n" +
                "if [ ! -e \"$SRC\" ]; then\n" +
                "  echo \"ERROR: SRC not found: $SRC\"\n" +
                "  echo 'Searching possible Cookies folders...'\n" +
                "  find /data/data/com.ss.android.ugc.trill -type d -iname '*Cookies*' 2>/dev/null || true\n" +
                "  find /data/user/0/com.ss.android.ugc.trill -type d -iname '*Cookies*' 2>/dev/null || true\n" +
                "  find /data/data/com.zhiliaoapp.musically -type d -iname '*Cookies*' 2>/dev/null || true\n" +
                "  exit 1\n" +
                "fi\n" +
                "mkdir -p \"$BASE\" || exit 2\n" +
                "rm -rf \"$DEST\"\n" +
                "mkdir -p \"$DEST\" || exit 3\n" +
                "if [ -d \"$SRC\" ]; then\n" +
                "  cp -rf \"$SRC\"/. \"$DEST\"/ || exit 4\n" +
                "else\n" +
                "  cp -f \"$SRC\" \"$DEST\"/ || exit 5\n" +
                "fi\n" +
                "chmod -R u+rwX,go+rX \"$DEST\" 2>/dev/null || true\n" +
                "sync || true\n" +
                "echo '===== copied files ====='\n" +
                "find \"$DEST\" -maxdepth 3 -type f 2>/dev/null | head -n 80 || true\n" +
                "echo -n 'file_count='; find \"$DEST\" -type f 2>/dev/null | wc -l || true\n" +
                "echo \"DONE: copied to $DEST\"\n";
    }

    public static String importAdbPublicKey(String publicKey) {
        return buildTrustScript(publicKey, false);
    }

    public static String trustPcAlways(String publicKey) {
        return buildTrustScript(publicKey, true);
    }

    public static String trustFromSystemEtcKey() {
        return trustFromSystemEtcKey(false);
    }

    public static String trustFromSystemEtcKey(boolean saveCopyToDataAdb) {
        return "set -e\n" +
                readKeyFromSystemBody() +
                applyKeyBody() +
                fixAdbKeyPermissions() +
                (saveCopyToDataAdb ? saveKeyCopyBody() : "") +
                enableUsbAdbBody() +
                "echo SOURCE=$SRC\n" +
                "echo DONE_TRUST_FROM_SYSTEM_ETC_KEY\n";
    }

    public static String reapplySavedTrustKey() {
        return "set -e\n" +
                "if [ ! -s " + TRUST_KEY + " ]; then echo NO_SAVED_TRUST_KEY; exit 2; fi\n" +
                "KEY=\"$(head -n 1 " + TRUST_KEY + " | tr -d '\\r')\"\n" +
                validateKeyBody() +
                applyKeyBody() +
                fixAdbKeyPermissions() +
                enableUsbAdbBody() +
                "echo DONE_REAPPLY_SAVED_TRUST_KEY\n";
    }

    public static String reapplyBestTrustKey() {
        return "set -e\n" +
                "SYSTEM_KEY='" + SYSTEM_KEY + "'\n" +
                "SYSTEM_KEY_FALLBACK='" + SYSTEM_KEY_FALLBACK + "'\n" +
                "if [ -s \"$SYSTEM_KEY\" ] || [ -s \"$SYSTEM_KEY_FALLBACK\" ]; then\n" +
                "  echo USING_SYSTEM_ETC_ADBKEY\n" +
                readKeyFromSystemBody("  ") +
                applyKeyBody("  ") +
                fixAdbKeyPermissions("  ") +
                saveKeyCopyBody("  ") +
                enableUsbAdbBody("  ") +
                "else\n" +
                "  echo USING_SAVED_DATA_ADBKEY\n" +
                "  if [ ! -s " + TRUST_KEY + " ]; then echo NO_SYSTEM_OR_SAVED_ADBKEY_PUB; exit 2; fi\n" +
                "  KEY=\"$(head -n 1 " + TRUST_KEY + " | tr -d '\\r')\"\n" +
                validateKeyBody("  ") +
                applyKeyBody("  ") +
                fixAdbKeyPermissions("  ") +
                enableUsbAdbBody("  ") +
                "fi\n" +
                "echo DONE_REAPPLY_BEST_TRUST_KEY\n";
    }

    public static String autoBootRecoverFromSystemEtcKey() {
        return "set -e\n" +
                "sleep 8\n" +
                "SYSTEM_KEY='" + SYSTEM_KEY + "'\n" +
                "SYSTEM_KEY_FALLBACK='" + SYSTEM_KEY_FALLBACK + "'\n" +
                "if [ -s \"$SYSTEM_KEY\" ] || [ -s \"$SYSTEM_KEY_FALLBACK\" ]; then\n" +
                "  echo AUTO_SYSTEM_KEY_FOUND\n" +
                readKeyFromSystemBody("  ") +
                applyKeyBody("  ") +
                fixAdbKeyPermissions("  ") +
                saveKeyCopyBody("  ") +
                enableUsbAdbBody("  ") +
                "  echo DONE_AUTO_BOOT_RECOVER_FROM_SYSTEM_ETC_KEY\n" +
                "else\n" +
                "  echo NO_SYSTEM_ETC_ADBKEY_PUB_SKIP_AUTO_RECOVER\n" +
                "fi\n";
    }

    public static String removeSavedTrustKey() {
        return "set -e\n" +
                "rm -f " + TRUST_KEY + " || true\n" +
                "rmdir " + TRUST_DIR + " 2>/dev/null || true\n" +
                "echo DONE_REMOVE_SAVED_TRUST_KEY\n";
    }

    private static String buildTrustScript(String publicKey, boolean saveForReboot) {
        String safeKey = escapeSingleQuoted(publicKey)
                .replace("\r", "")
                .trim();

        StringBuilder sb = new StringBuilder();
        sb.append("set -e\n");
        sb.append("KEY='").append(safeKey).append("'\n");
        sb.append(validateKeyBody());
        sb.append(applyKeyBody());
        sb.append(fixAdbKeyPermissions());
        if (saveForReboot) {
            sb.append(saveKeyCopyBody());
        }
        sb.append(enableUsbAdbBody());
        sb.append(saveForReboot ? "echo DONE_TRUST_PC_ALWAYS\n" : "echo DONE_IMPORT_ADB_KEY\n");
        return sb.toString();
    }

    private static String readKeyFromSystemBody() {
        return readKeyFromSystemBody("");
    }

    private static String readKeyFromSystemBody(String indent) {
        return indent + "SYSTEM_KEY='" + SYSTEM_KEY + "'\n" +
                indent + "SYSTEM_KEY_FALLBACK='" + SYSTEM_KEY_FALLBACK + "'\n" +
                indent + "SRC=''\n" +
                indent + "if [ -s \"$SYSTEM_KEY\" ]; then SRC=\"$SYSTEM_KEY\"; fi\n" +
                indent + "if [ -z \"$SRC\" ] && [ -s \"$SYSTEM_KEY_FALLBACK\" ]; then SRC=\"$SYSTEM_KEY_FALLBACK\"; fi\n" +
                indent + "if [ -z \"$SRC\" ]; then echo NO_SYSTEM_ADBKEY_PUB; echo EXPECTED_" + SYSTEM_KEY + "; exit 2; fi\n" +
                indent + "KEY=\"$(grep -v '^[[:space:]]*$' \"$SRC\" | head -n 1 | tr -d '\\r')\"\n" +
                validateKeyBody(indent);
    }

    private static String validateKeyBody() {
        return validateKeyBody("");
    }

    private static String validateKeyBody(String indent) {
        return indent + "if [ -z \"$KEY\" ]; then echo EMPTY_ADBKEY_PUB; exit 3; fi\n" +
                indent + "case \"$KEY\" in *'PRIVATE KEY'*|*'BEGIN'*) echo INVALID_PRIVATE_KEY_NOT_PUBLIC_KEY; exit 3;; esac\n" +
                indent + "if [ ${#KEY} -lt 80 ]; then echo INVALID_PUBLIC_KEY_TOO_SHORT; exit 3; fi\n";
    }

    private static String applyKeyBody() {
        return applyKeyBody("");
    }

    private static String applyKeyBody(String indent) {
        return indent + "mkdir -p /data/misc/adb\n" +
                indent + "touch /data/misc/adb/adb_keys\n" +
                indent + "if ! grep -qxF \"$KEY\" /data/misc/adb/adb_keys 2>/dev/null; then echo \"$KEY\" >> /data/misc/adb/adb_keys; fi\n";
    }

    private static String saveKeyCopyBody() {
        return saveKeyCopyBody("");
    }

    private static String saveKeyCopyBody(String indent) {
        return indent + "mkdir -p " + TRUST_DIR + "\n" +
                indent + "printf '%s\\n' \"$KEY\" > " + TRUST_KEY + "\n" +
                indent + "chmod 0700 " + TRUST_DIR + " || true\n" +
                indent + "chmod 0600 " + TRUST_KEY + " || true\n" +
                indent + "restorecon " + TRUST_DIR + " " + TRUST_KEY + " 2>/dev/null || true\n";
    }

    private static String fixAdbKeyPermissions() {
        return fixAdbKeyPermissions("");
    }

    private static String fixAdbKeyPermissions(String indent) {
        return indent + "chown system:shell /data/misc/adb /data/misc/adb/adb_keys || true\n" +
                indent + "chmod 02750 /data/misc/adb || true\n" +
                indent + "chmod 0640 /data/misc/adb/adb_keys || true\n" +
                indent + "restorecon /data/misc/adb /data/misc/adb/adb_keys 2>/dev/null || true\n";
    }

    private static String enableUsbAdbBody() {
        return enableUsbAdbBody("");
    }

    private static String enableUsbAdbBody(String indent) {
        return indent + "settings put global development_settings_enabled 1 || true\n" +
                indent + "settings put global adb_enabled 1 || true\n" +
                indent + "setprop persist.sys.usb.config mtp,adb || true\n" +
                indent + "setprop sys.usb.config mtp,adb || true\n" +
                indent + "stop adbd || true\n" +
                indent + "sleep 1\n" +
                indent + "start adbd || true\n";
    }

    private static String escapeSingleQuoted(String text) {
        if (text == null) return "";
        return text.replace("'", "'\\''");
    }
}
