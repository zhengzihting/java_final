@echo off
REM =============================================================================
REM package-win.bat — 在 Windows 上將專案打包成 .exe 安裝程式
REM =============================================================================
setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

set "APP_NAME=YourTicketIsMyTicket"
set "APP_VERSION=1.0"
set "MAIN_JAR=%APP_NAME%-1.0-SNAPSHOT.jar"
set "MAIN_CLASS=app.main.Launcher"

set "INPUT_DIR=%PROJECT_DIR%\target\jpackage-input"
set "OUTPUT_DIR=%PROJECT_DIR%\target\dist"

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
    pause
    exit /b 1
)
echo       ✓ Fat JAR 已產生：target\%MAIN_JAR%

REM ─── 2. 準備 jpackage 輸入目錄 ─────────────────────────────────────────
echo [2/3] 準備輸入目錄 ...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
copy /y "target\%MAIN_JAR%" "%INPUT_DIR%\" >nul
echo       ✓ 輸入目錄準備完成

REM ─── 3. 自動偵測 jpackage 路徑並執行 ──────────────────────────────────
echo [3/3] 尋找 jpackage 並執行 ...
if exist "%OUTPUT_DIR%" rmdir /s /q "%OUTPUT_DIR%"
mkdir "%OUTPUT_DIR%"

set "PKG_TYPE=app-image"

set "ICON_ARG="
if exist "%ICON%" set "ICON_ARG=--icon "%ICON%""

set "JPACKAGE_CMD="

where jpackage >nul 2>&1
if %errorlevel% equ 0 (
    set "JPACKAGE_CMD=jpackage"
    goto :ExecuteJPackage
)

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\jpackage.exe" (
        set "JPACKAGE_CMD=%JAVA_HOME%\bin\jpackage.exe"
        goto :ExecuteJPackage
    )
)

for /d %%i in ("%LocalAppData%\Programs\Microsoft\jdk*") do (
    if exist "%%i\bin\jpackage.exe" (
        set "JPACKAGE_CMD=%%i\bin\jpackage.exe"
        goto :ExecuteJPackage
    )
)

echo [ERROR] 無法自動偵測到 jpackage。請確認已安裝 JDK，或設定 JAVA_HOME 變數。
pause
exit /b 1

:ExecuteJPackage
echo       使用的 jpackage 路徑: "!JPACKAGE_CMD!"

"!JPACKAGE_CMD!" ^
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
    echo [ERROR] jpackage 執行失敗。
    pause
    exit /b 1
)

echo.
echo 完成！安裝程式位於：
echo    %OUTPUT_DIR%\
echo.
echo 使用者資料將儲存於：
echo    %%APPDATA%%\%APP_NAME%\
echo    （包含 tasks.json、ticket_monitor.db、sounds\）
echo.
pause
endlocal