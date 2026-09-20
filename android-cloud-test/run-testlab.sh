#!/usr/bin/env bash
# Chạy Firebase Test Lab Robo test từ Git Bash
# Cách dùng: ./run-testlab.sh <file.apk> [device-model] [android-version]
# Ví dụ: ./run-testlab.sh app-debug.apk Pixel7 34
set -euo pipefail

APK="${1:-}"
MODEL="${2:-Pixel7}"
VERSION="${3:-34}"

if [[ -z "$APK" ]]; then
  echo "Cách dùng: ./run-testlab.sh <file.apk>"
  exit 1
fi
if [[ ! -f "$APK" ]]; then
  echo "Không tìm thấy file: $APK (copy APK vào thư mục này trước)"
  exit 1
fi

echo "==> Upload $APK lên Test Lab (model=$MODEL, android=$VERSION)..."
gcloud firebase test android run \
  --app "$APK" \
  --type robo \
  --device "model=$MODEL,version=$VERSION,locale=vi_VN,orientation=portrait" \
  --timeout 5m \
  --robo-directives text:username=testuser,text:password=123456 || true

echo ""
echo "Xong. Mở link Firebase Console ở trên để xem video + log."
