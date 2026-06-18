@echo off
REM =============================================================================
REM package-win.bat — 在 Windows 上將專案打包成 .exe 安裝程式
REM
REM 用法（在 CMD 或 PowerShell 執行）：
REM   package-win.bat          產生 .exe 安裝程式
REM   package-win.bat --msi    改產生 .msi 安裝程式
REM
REM 前置需求：
REM   - JDK 17+（含 jpackage）
REM   - WiX Toolset 3.x（產生 .msi 需要）https://wixtoolset.org/
REM =============================================================================
setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
REM 移除尾部反斜線
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

set "APP_NAME=YourTicketIsMyTicket"
set "APP_VERSION=1.0"
set "MAIN_JAR=%APP_NAME%-1.0-SNAPSHOT.jar"
set "MAIN_CLASS=app.main.Launcher"

set "INPUT_DIR=%PROJECT_DIR%\target\jpackage-input"
set "OUTPUT_DIR=%PROJECT_DIR%\target\dist"

REM 圖示檔（Windows 需要 .ico 格式）
REM 可用 https://www.icoconverter.com 將 notify_icon.png 轉為 app.ico 後放在此路徑
set "ICON=%PROJECT_DIR%\src\main\resources\app.ico"

echo.
echo ============================================================
echo  YourTicketIsMyTicket - Windows Packager
echo ============================================================

REM ─── 1. Maven 打包成 Fat JAR ─────────────────────────────────────────────
echo [1/3] 執行 mvn clean package ...
cd /d "%PROJECT_DIR%"
call mvn clean package -q
if errorlevel 1 (
    echo [ERROR] Maven 打包失敗，請檢查錯誤訊息。
    exit /b 1
)
echo       ✓ Fat JAR 已產生：target\%MAIN_JAR%

REM ─── 2. 準備 jpackage 輸入目錄 ─────────────────────────────────────────
echo [2/3] 準備輸入目錄 ...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
copy /y "target\%MAIN_JAR%" "%INPUT_DIR%\" >nul
echo       ✓ 輸入目錄準備完成

REM ─── 3. 執行 jpackage ──────────────────────────────────────────────────
echo [3/3] 執行 jpackage ...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"

REM 決定打包類型
set "PKG_TYPE=app-image"

set "ICON_ARG="
if exist "%ICON%" set "ICON_ARG=--icon "%ICON%""

"%LocalAppData%\Programs\Microsoft\jdk-25.0.3.9-hotspot\bin\jpackage" ^
    --type %PKG_TYPE% ^
    --name "%APP_NAME%" ^
    --app-version "%APP_VERSION%" ^
    --input "%INPUT_DIR%" ^
    --main-jar "%MAIN_JAR%" ^
    --main-class "%MAIN_CLASS%" ^
    --dest "%OUTPUT_DIR%" ^
    %ICON_ARG% ^
    --java-options "-Dapp.packaged=true" ^
    --java-options "--enable-native-access=ALL-UNNAMED" ^
    --java-options "-Xmx512m" ^
    --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo [ERROR] jpackage 失敗。
    echo 若要產生 .msi，請確認已安裝 WiX Toolset 3.x
    exit /b 1
)

echo.
echo ✅ 完成！安裝程式位於：
echo    %OUTPUT_DIR%\
echo.
echo 📂 使用者資料將儲存於：
echo    %%APPDATA%%\%APP_NAME%\
echo    （包含 tasks.json、ticket_monitor.db、sounds\）
echo.
pause
endlocal
