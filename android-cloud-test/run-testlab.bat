@echo off
REM Chay Firebase Test Lab Robo test tu CMD/PowerShell
REM Cach dung: run-testlab.bat app-debug.apk
if "%~1"=="" (
  echo Cach dung: run-testlab.bat ^<file.apk^>
  exit /b 1
)
if not exist "%~1" (
  echo Khong tim thay file: %~1 (copy APK vao thu muc nay truoc)
  exit /b 1
)
gcloud firebase test android run --app "%~1" --type robo --device "model=Pixel7,version=34,locale=vi_VN,orientation=portrait" --timeout 5m
echo.
echo Xong. Mo link Firebase Console o tren de xem video + log.
