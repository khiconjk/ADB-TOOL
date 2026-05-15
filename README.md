# Root ADB Enabler SystemKey

App Android dùng quyền root để bật ADB và tự trust PC bằng public key đặt sẵn trong `/system/etc/adbkey.pub`.

## Chỉ dùng file nào trên Windows?

Trong thư mục:

```text
C:\Users\TUNG PC\.android
```

thường có 2 file:

```text
adbkey      = private key của PC, KHÔNG chép vào Android
adbkey.pub  = public key của PC, CHÉP vào Android
```

Android chỉ cần public key. File phải đặt đúng là:

```text
/system/etc/adbkey.pub
```

App cũng có fallback cho đường dẫn gõ nhầm:

```text
/system.etc/adbkey.pub
```

nhưng nên dùng `/system/etc/adbkey.pub`.

## Chức năng chính

- Bật ADB USB không cần vào Developer options.
- Bật ADB TCP 5555.
- Import `adbkey.pub` một lần.
- Trust PC luôn bằng key chọn từ file.
- Trust trực tiếp từ `/system/etc/adbkey.pub`.
- Tự thử phục hồi trust từ `/system/etc/adbkey.pub` mỗi lần boot, kể cả khi mất cấu hình app trong `/data`.
- Restart `adbd`.
- Kiểm tra trạng thái ADB/key/property.

## Cơ chế tự phục hồi từ /system

Khi máy boot, `BootReceiver` sẽ luôn thử:

1. Kiểm tra `/system/etc/adbkey.pub`.
2. Nếu có, đọc dòng public key đầu tiên.
3. Ghi key vào `/data/misc/adb/adb_keys` nếu chưa có.
4. Set quyền:

```sh
chown system:shell /data/misc/adb /data/misc/adb/adb_keys
chmod 02750 /data/misc/adb
chmod 0640 /data/misc/adb/adb_keys
restorecon /data/misc/adb /data/misc/adb/adb_keys
```

5. Bật ADB:

```sh
settings put global development_settings_enabled 1
settings put global adb_enabled 1
setprop persist.sys.usb.config mtp,adb
setprop sys.usb.config mtp,adb
stop adbd
start adbd
```

## Lưu ý về factory reset

Đặt key trong `/system/etc/adbkey.pub` giúp key không mất sau factory reset, nhưng để tự phục hồi hoàn toàn thì app cũng phải còn sau reset.

Muốn vậy, cài app dạng system app/priv-app, ví dụ:

```sh
mkdir -p /system/priv-app/RootAdbEnablerSystemKey
cp /sdcard/RootAdbEnablerSystemKey.apk /system/priv-app/RootAdbEnablerSystemKey/RootAdbEnablerSystemKey.apk
chmod 0644 /system/priv-app/RootAdbEnablerSystemKey/RootAdbEnablerSystemKey.apk
restorecon -R /system/priv-app/RootAdbEnablerSystemKey
```

Sau đó reboot. Nếu chỉ cài app bình thường trong `/data/app`, factory reset sẽ xóa app.

## Cách build

Mở project bằng Android Studio rồi build APK:

```bash
./gradlew assembleDebug
```

Nếu project chưa có Gradle Wrapper, dùng nút Build trong Android Studio hoặc tạo wrapper từ Android Studio.

APK debug nằm tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Cách đặt key vào /system/etc

Từ Windows, chỉ copy nội dung file:

```text
C:\Users\TUNG PC\.android\adbkey.pub
```

vào Android tại:

```text
/system/etc/adbkey.pub
```

Không dùng file `adbkey` vì đó là private key.


## Chức năng copy Trill WebView img

Bản này thêm nút:

```text
Copy Trill WebView img ra /sdcard
```

Nguồn copy:

```text
/data/data/com.ss.android.ugc.trill/app_webview/Default/img
```

Nơi xuất file:

```text
/sdcard/RootAdbEnabler/trill_webview_img_YYYYMMDD_HHMMSS/
```

Nút này cần root và chỉ chạy khi người dùng bấm thủ công trong app.
