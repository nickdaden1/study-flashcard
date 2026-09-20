# Android Cloud Test — Test app Android không cần máy khỏe

Máy bạn: Win 10, RAM 8GB (trống ~1GB), chưa có ADB/emulator → test cloud là hợp lý nhất.
Không cần cài Android Studio, không tốn RAM.

## 1. Chuẩn bị (5 phút)

1. Có file app: `*.apk` (hoặc `*.aab` cho Robo test). Nếu bạn build từ source Flutter/React Native/Android native thì lấy file ở `build/app/outputs/.../*.apk`.
2. Có tài khoản Google.
3. Tạo project Firebase (miễn phí): https://console.firebase.google.com → Add project → tắt Google Analytics cho nhanh cũng được.

> Quota miễn phí Test Lab (gói Spark): **10 lượt test thiết bị ảo/ngày, 5 lượt thiết bị vật lý/ngày**.
> Gói Blaze: miễn phí 60 phút thiết bị ảo/ngày và 30 phút thiết bị vật lý/ngày, sau đó 1 USD/giờ (ảo) và 5 USD/giờ (vật lý), làm tròn lên từng phút.
> Nguồn: [Firebase Pricing](https://firebase.google.com/pricing)

## 2. Cách nhanh nhất — không cài gì (khuyên dùng lần đầu)

1. Mở https://console.firebase.google.com → chọn project → **Test Lab** (menu trái, mục Release & Monitor).
2. **Run a test → Instrumentation / Robo → Browse** → upload file `.apk`.
3. Chọn device: ví dụ `Pixel 7, Android 14, vi_VN, Portrait`.
4. Bấm **Start tests** → đợi 3–10 phút → xem video, screenshot, log, performance ngay trên web.

- **Robo test**: tự động bấm app như người dùng, không cần viết test. Phù hợp smoke test.
- **Instrumentation test**: nếu bạn có test Espresso/UIAutomator (`app.apk` + `test.apk`).

## 3. Cách lặp lại nhiều lần — dùng gcloud CLI

Cài 1 lần, sau đó chạy 1 lệnh là test.

**Cài gcloud trên Windows (PowerShell, quyền admin):**

```powershell
winget install Google.CloudSDK
gcloud init
gcloud auth login
gcloud config set project <YOUR_PROJECT_ID>
```

Kiểm tra:

```bash
gcloud firebase test android models list
```

**Chạy test (Git Bash / PowerShell):**

Copy file `.apk` của bạn vào thư mục này, rồi:

```bash
# Robo test — tự động crawl app 3 phút trên Pixel 7 Android 14
./run-testlab.sh app-debug.apk
```

hoặc trên CMD:

```bat
run-testlab.bat app-debug.apk
```

Kết quả trả về link Firebase Console chứa video + log + screenshot.

## 4. Phương án khác (không cần Firebase)

- **BrowserStack App Live / App Automate**: upload APK lên web, điều khiển máy thật từ xa trực tiếp trên trình duyệt. Có trial miễn phí, phù hợp test tay. https://www.browserstack.com/app-live
- **AWS Device Farm**: trả theo phút, nhiều máy lạ. https://aws.amazon.com/device-farm/

## 5. Bạn đang ở đâu?

- [ ] Chưa có APK → nói mình biết source của bạn là gì (Flutter / React Native / native / link GitHub), mình hướng dẫn build ra APK.
- [ ] Đã có APK → copy vào thư mục này rồi chạy script, hoặc upload lên Console theo mục 2.
- [ ] Muốn test tự động sâu (login, mua hàng...) → mình viết thêm Robo script + Espresso mẫu cho bạn.

Thư mục này:
- `run-testlab.sh` — script cho Git Bash
- `run-testlab.bat` — script cho CMD/PowerShell
- ` robo-script.json` — (tùy chọn) kịch bản Robo đăng nhập tự động, xem mẫu tại https://firebase.google.com/docs/test-lab/android/robo-ux-test
