# Root ADB Enabler

App Android dùng quyền root để bật ADB mà không cần vào Developer options.

## Chức năng

- Bật ADB USB
- Bật ADB Wi-Fi/TCP port 5555
- Tắt ADB TCP 5555
- Import `adbkey.pub` của PC vào `/data/misc/adb/adb_keys` một lần
- Trust PC luôn: lưu public key của PC và tự chép lại vào `/data/misc/adb/adb_keys` sau reboot
- Khôi phục trust PC đã lưu
- Xóa key trust đã lưu trong app
- Restart `adbd`
- Kiểm tra trạng thái ADB/settings/property/key
- Tùy chọn tự bật ADB USB/TCP sau reboot

## Điều kiện

- Máy đã root bằng KernelSU / KernelSU Next / Magisk
- App được cấp quyền root vĩnh viễn hoặc ít nhất được cấp lại sau reboot
- Nếu PC bị `unauthorized`, hãy import file public key ADB của PC: `%USERPROFILE%\.android\adbkey.pub`

## Trust PC luôn hoạt động như thế nào?

Khi bấm `Trust PC luôn / tự khôi phục sau reboot`, app sẽ:

1. Chọn file `adbkey.pub` của PC.
2. Ghi key vào `/data/misc/adb/adb_keys`.
3. Lưu thêm một bản tại `/data/adb/root_adb_enabler/adbkey.pub`.
4. Bật `adb_enabled`.
5. Restart `adbd`.
6. Bật tùy chọn tự trust lại PC sau khi khởi động.

Sau reboot, app sẽ dùng quyền root để chép lại key vào `/data/misc/adb/adb_keys`, bật ADB, rồi restart `adbd`.

Lưu ý: cách này giữ trust sau reboot, nhưng không giữ sau factory reset nếu app/key nằm trong `/data`. Muốn giữ sau factory reset cần cài app/key thành system app hoặc dùng script/patch nằm ngoài `/data`.

## Cách build

Mở thư mục này bằng Android Studio, sau đó build APK:

`Build > Build Bundle(s) / APK(s) > Build APK(s)`

Hoặc dùng terminal trong Android Studio:

```bash
./gradlew assembleDebug
```

APK sau khi build nằm ở:

`app/build/outputs/apk/debug/app-debug.apk`

## Cách lấy adbkey.pub trên Windows

File public key thường nằm ở:

`C:\Users\TEN_USER\.android\adbkey.pub`

Copy file này vào máy Android, mở app, bấm `Trust PC luôn / tự khôi phục sau reboot`, chọn file đó.

## Kết nối ADB TCP

Sau khi bấm `Bật ADB Wi-Fi/TCP 5555`, trên PC chạy:

```bat
adb connect IP_MAY:5555
adb devices
```
