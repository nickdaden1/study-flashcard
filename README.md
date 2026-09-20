# Study Flashcard — App học đề cương (Android native)

App Android giúp ghi nhớ đề cương Word/PDF của thầy cô để đi thi thật:
AI (Gemini) sinh flashcard + quiz → học lặp lại ngắt quãng (SM-2) → thi thử bấm giờ.

- Spec chi tiết: [docs/superpowers/specs/2026-09-20-study-flashcard-design.md](docs/superpowers/specs/2026-09-20-study-flashcard-design.md)
- Test trên Firebase Test Lab: [android-cloud-test/](android-cloud-test/)

## Quy trình làm việc (máy dev yếu, không cài Android Studio)

1. Sửa code bằng VS Code, push lên GitHub.
2. GitHub Actions (cloud) build ra `app-debug.apk` → tải về từ tab Actions.
3. Cài APK lên máy thật test tay, hoặc ném lên Firebase Test Lab bằng script trong `android-cloud-test/`.

## Trạng thái

- [x] Phase 0: git + CI sanity
- [ ] Phase 1: scaffold Gradle project (app rỗng cài được)
- [ ] Phase 2: data layer (Room + parser + chunking)
- [ ] Phase 3: AI layer (Gemini REST + retry)
- [ ] Phase 4: SM-2 + repository
- [ ] Phase 5: UI Compose (5 màn hình)
- [ ] Phase 6: nhắc học + xuất/nhập JSON
- [ ] Phase 7: hardening + Test Lab + bàn giao
