@echo off
echo ========================================================
echo MF650 Manager v0.1.0 Debug APK 一键安装脚本
echo ========================================================
echo.

adb devices
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] 无法找到 adb 命令，请确保 Android SDK Platform-tools 已加入系统 PATH。
    pause
    exit /b 1
)

echo.
echo [*] 正在将 MF650_Manager_v0.1.0_debug.apk 安装至已连接设备...
adb install -r "%~dp0MF650_Manager_v0.1.0_debug.apk"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] 安装成功！正在启动应用...
    adb shell am start -n com.mf650.manager/.MainActivity
) else (
    echo.
    echo [FAIL] 安装失败，请检查手机是否开启 USB 调试与安装权限。
)

echo.
pause
