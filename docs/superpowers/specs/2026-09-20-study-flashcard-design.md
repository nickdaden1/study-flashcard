# Spec: App học đề cương — Flashcard + Thi thử (Android native)

Ngày: 2026-09-20
Trạng thái: Chờ duyệt
Hướng chốt: A — Native Kotlin + Jetpack Compose, build APK trên cloud

## 1. Bối cảnh & mục tiêu

### Vấn đề
Sinh viên nhận đề cương ôn thi từ thầy cô (file Word/PDF, ~10-50 trang),
cần ghi nhớ để áp dụng vào thi thật. Đọc chay dễ quên, không biết mình thuộc tới đâu.

### Mục tiêu
- Import file `.docx` / `.pdf` đề cương → AI sinh bộ flashcard + quiz (trắc nghiệm + tự luận).
- Học chính bằng flashcard lặp lại ngắt quãng (SM-2): nhớ/chưa nhớ/thuộc, tự nhắc lại câu hay quên.
- Kiểm tra bằng thi thử bấm giờ; trắc nghiệm chấm tự động, tự luận AI chấm tham khảo.
- Theo dõi tiến độ từng môn + nhắc học hằng ngày.

### Tiêu chí thành công (MVP)
- Import đề cương ~20 trang → ra bộ thẻ dưới 5 phút (mạng ổn định).
- Học/thi offline được sau khi đã sinh thẻ.
- APK debug dưới 25MB.
- Robo test trên Firebase Test Lab (project `awsum`, Pixel 7 Android 14) qua hết, không crash.
- Unit test các hàm thuần (SM-2, tách text, parse JSON) xanh trên CI.

### Không làm ở MVP
- OCR ảnh chụp đề cương giấy.
- Đồng bộ đa thiết bị / tài khoản / share deck online (chỉ xuất/nhập JSON file).
- Đọc to TTS, widget home, dark-mode tùy biến sâu (dùng Material 3 dynamic là đủ).

## 2. Ràng buộc đã chốt với người dùng

| Quyết định | Giá trị |
|---|---|
| Hình thức thi thật | Hỗn hợp trắc nghiệm + tự luận |
| Đầu vào đề cương | File Word (.docx) / PDF |
| Cách tạo câu hỏi | AI sinh (Gemini), cho sửa tay thẻ lỗi |
| Cách học chính | Flashcard ghi nhớ; thi thử là phụ |
| Nền tảng | App Android native, luôn online khi cần AI |
| Máy dev | Win 10, RAM 8GB (trống ~1GB), chưa có Android SDK → build trên GitHub Actions |
| Môi trường test | Firebase Test Lab project `awsum` (gói Spark), script sẵn ở `android-cloud-test/` |
| Ngôn ngữ báo lỗi | Tiếng Việt, không crash |

## 3. Kiến trúc tổng

```
UI (Compose + Material 3)
  → ViewModel (màn hình Home / Sinh thẻ / Học / Thi / Tiến độ)
    → Repository (DeckRepository, StudyRepository)
      → Room (local) + Gemini DataSource (remote) + File Parser
```

- Ngôn ngữ: Kotlin. UI: Jetpack Compose + Material 3. Kiến trúc: MVVM một module (`:app`).
- MinSdk 26, TargetSdk 34 (tương thích Pixel 7 Android 14 trên Test Lab).
- Không server riêng. App gọi thẳng Gemini Developer API (`gemini-2.0-flash`).
- DI: manual hoặc Hilt (chốt lúc lập plan; ưu tiên Hilt nếu không phình scope).
- JSON: Kotlinx Serialization. Coroutines + Flow cho async.

### Thư viện dự kiến
- `androidx.room` — lưu deck/card/attempt.
- `androidx.security:security-crypto` — EncryptedSharedPreferences cho API key + giờ nhắc học.
- `com.tom_roush:pdfbox-android` — đọc PDF trên máy.
- `org.apache.poi:poi-ooxml` — đọc `.docx` (không hỗ trợ `.doc` đời cũ → báo convert).
- Retrofit/OkHttp hoặc Ktor client — gọi Gemini REST (chốt lúc plan; ưu tiên OkHttp+Retrofit vì phổ biến).
- WorkManager — nhắc học hằng ngày.
- JUnit4 + Turbine (nếu dùng Flow) cho unit test.

## 4. Mô hình dữ liệu (Room)

### Bảng `decks`
- `id: Long (PK)`, `name: String` (tên môn), `sourceFileName: String?`, `createdAt: Long`, `cardCount: Int` (denormalized để hiện nhanh).

### Bảng `cards`
- `id: Long (PK)`, `deckId: Long (FK → decks, cascade delete)`.
- `type: String` (`MCQ` | `ESSAY`).
- `question: String`, `answer: String` (tự luận: đáp án mẫu; trắc nghiệm: đáp án đúng).
- `choicesJson: String?` (MCQ: 4 lựa chọn dạng JSON list).
- SM-2: `easiness: Float (mặc định 2.5)`, `intervalDays: Int (mặc định 0)`, `repetitions: Int (mặc định 0)`, `dueAt: Long` (timestamp đến hạn ôn).
- `lastGrade: String?` (`AGAIN` | `GOOD` | `EASY` — lần đánh giá gần nhất).

### Bảng `attempts`
- `id`, `deckId`, `startedAt`, `finishedAt`, `totalQuestions`, `correctMcq`, `essayScoreAvg: Float?`, `durationSec`.

### Lưu ngoài Room (EncryptedSharedPreferences)
- `gemini_api_key`, `reminder_hour` (mặc định 21), `reminder_enabled` (mặc định true).

### Xuất/nhập
- Xuất 1 deck ra JSON (deck + cards) → share qua Zalo/Drive. Nhập JSON cùng schema → tạo deck mới.

## 5. Màn hình + luồng

### 5.1 Home — Bộ đề
- List deck: tên môn, số thẻ, % đã thuộc (`repetitions>0 && dueAt > now` / tổng).
- Nút `+ Import đề cương` → Storage Access Framework (ACTION_OPEN_DOCUMENT, mime docx/pdf/json).
- Vuốt/sửa: đổi tên, xóa (confirm), xuất JSON.

### 5.2 Sinh thẻ (AI)
1. Parse file → text thô (hiện preview ~2000 ký tự đầu để user kiểm tra).
2. Cắt chunk ~4000 ký tự/chunk, giữ nguyên thứ tự.
3. User chọn: số thẻ mục tiêu + tỉ lệ MCQ/Essay (mặc định 60/40).
4. Gọi Gemini nối tiếp từng chunk (có progress bar + delay tránh rate-limit).
5. Gom kết quả → list thẻ nháp; thẻ lỗi format gắn cờ → user sửa tay hoặc xóa.
6. Bấm Lưu → insert Room → về Home.

### 5.3 Học flashcard (màn hình chính)
- Lấy các thẻ đến hạn (`dueAt <= now`), trộn thứ tự.
- Mặt trước: câu hỏi (+ 4 lựa chọn nếu MCQ nhưng che đáp án).
- Bấm lật → hiện đáp án → user tự đánh: **Chưa nhớ / Nhớ / Thuộc**.
- Cập nhật SM-2:
  - Chưa nhớ: `repetitions=0, interval=0, dueAt=now` (hiện lại ngay trong buổi).
  - Nhớ: repetitions+1, interval = 1 / 6 / round(prev*easiness) theo SM-2 chuẩn.
  - Thuộc: như Nhớ + easiness +0.15 (cap 1.3–2.5+).
- Cuối buổi: thống kê đã thuộc bao nhiêu %, còn bao nhiêu thẻ đến hạn.

### 5.4 Thi thử (phụ)
- Setup: chọn deck, số câu, thời gian (mặc định 15 câu / 20 phút), tỉ lệ MCQ/Essay.
- Làm bài bấm giờ, hết giờ tự nộp.
- MCQ chấm tự động. Essay: hiện đáp án mẫu + nút **AI chấm tham khảo** (gửi câu hỏi + đáp án mẫu + câu trả lời → điểm 0-10 + nhận xét thiếu ý) + cho user tự sửa điểm.
- Kết quả lưu `attempts`; màn hình xem lại câu sai.

### 5.5 Tiến độ + nhắc học
- % thuộc từng môn (bar/progress), số thẻ đến hạn hôm nay.
- WorkManager + notification local hằng ngày giờ đã chọn (mặc định 21h). Xin `POST_NOTIFICATIONS` runtime.

## 6. Tích hợp AI (Gemini)

- Model: `gemini-2.0-flash` qua REST. Key do user tự tạo tại AI Studio, nhập 1 lần trong Cài đặt, lưu mã hóa.
- **Prompt sinh thẻ:** yêu cầu trả về JSON array strict-schema:
  `{type, question, choices[4]?, answer}`. Mỗi chunk yêu cầu số lượng thẻ cụ thể.
- **Prompt chấm tự luận:** input `{question, modelAnswer, userAnswer}` → output `{score 0-10, missingPoints[], feedback}`. UI ghi rõ "AI chấm tham khảo".
- Chống lỗi: parse fail → retry 1 lần → vẫn fail thì giữ text thô cho sửa tay. Rate-limit/quota → hiện "hết lượt miễn phí, thử lại sau", giữ lại phần đã sinh được.
- Bảo mật: key chỉ nằm trên máy + gọi tới Google; cảnh báo không dùng máy đã root cho key quan trọng.

## 7. File parser

- PDF: PdfBox-Android, đọc theo trang, nối text, chuẩn hóa whitespace.
- DOCX: Apache POI (`XWPFDocument`), đọc paragraph + table theo thứ tự.
- `.doc` đời cũ: chặn + báo "hãy convert sang .docx".
- File quá dài: không giới hạn cứng, nhưng chunk + progress; file >100MB thì cảnh báo trước khi parse.
- JSON import: validate schema version, báo rõ nếu file lạ.

## 8. Quyền + nền tảng

- `INTERNET` (gọi Gemini).
- Mở file qua SAF → không cần `READ_EXTERNAL_STORAGE` trên Android mới.
- `POST_NOTIFICATIONS` — xin runtime khi bật nhắc học.
- MinSdk 26 / TargetSdk 34. Compose + Material 3, hỗ trợ xoay dọc/mặc định portrait lúc test.

## 9. Build trên cloud + test (khớp máy dev yếu)

1. Viết code bằng VS Code trên máy dev (chỉ cần JDK 27 đã có + extension Kotlin). Không cài Android Studio/SDK/emulator.
2. GitHub Actions build `app-debug.apk` mỗi lần push (setup JDK + Android SDK trên runner cloud, `./gradlew assembleDebug + testDebugUnitTest`).
3. Tải APK từ trang Actions → cài máy thật qua USB/tải trực tiếp để test tay.
4. Robo test trên Test Lab project `awsum`: dùng script `android-cloud-test/run-testlab.sh|.bat`,
   device `Pixel7, Android 14, vi_VN, portrait`, timeout 5 phút. Kỳ vọng Robo đi qua Home → Import → flashcard không crash.
5. Quota Spark: 10 lượt máy ảo/ngày, 5 lượt máy thật/ngày — đủ cho MVP.

## 10. Xử lý lỗi (tiếng Việt, không crash)

| Tình huống | Hành vi |
|---|---|
| File `.doc` đời cũ | Báo convert sang `.docx` |
| Mất mạng lúc sinh thẻ / chấm | Báo mất mạng, giữ phần đã làm được |
| AI trả JSON sai | Retry 1 lần → cho sửa tay |
| Hết quota Gemini miễn phí | Báo "thử lại sau", không mất dữ liệu |
| Mất mạng lúc học/thi | Học tiếp bình thường (dữ liệu local) |
| File quá to | Cảnh báo + progress lúc parse |

## 11. Kiểm thử

- Unit (JUnit, chạy trên CI): SM-2 (3 ca: again/good/easy), cắt chunk giữ thứ tự, parse JSON thẻ (đúng/sai/thiếu trường), tính % thuộc.
- Không UI test ở MVP (Robo trên Test Lab thay thế smoke test).
- Manual checklist: import docx 20 trang, sinh thẻ, học 1 buổi, thi thử 1 đề, xuất/nhập JSON, nhắc học.

## 12. Rủi ro đã biết

- Quota Gemini miễn phí thay đổi → đã có thông báo rõ + retry/delay.
- POI/PdfBox làm APK phình → bật R8/minify, mục tiêu <25MB; nếu vượt thì tách parser ra dynamic-feature (ngoài MVP).
- Key lưu trên máy → cảnh báo root; không gửi key đi đâu ngoài Google.
