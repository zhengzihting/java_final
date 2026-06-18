@echo off
REM =============================================================================
REM package-win.bat — Package the project into an .exe installer on Windows
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

REM ─── 1. Package into Fat JAR using Maven ───────────────────────────────────
echo [1/3] Executing mvn clean package ...
cd /d "%PROJECT_DIR%"
call mvn clean package -q
if errorlevel 1 (
    echo [ERROR] Maven build failed, please check the error message.
    pause
    exit /b 1
)
echo       Fat JAR generated: target\%MAIN_JAR%

REM ─── 2. Prepare jpackage input directory ─────────────────────────────────────
echo [2/3] Preparing input directory ...
if exist "%INPUT_DIR%" rmdir /s /q "%INPUT_DIR%"
mkdir "%INPUT_DIR%"
copy /y "target\%MAIN_JAR%" "%INPUT_DIR%\" >nul
echo       Input directory prepared

REM ─── 3. Auto-detect jpackage path and execute ──────────────────────────────
echo [3/3] Searching for jpackage and executing ...
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

echo [ERROR] Cannot auto-detect jpackage. Please ensure JDK is installed or JAVA_HOME is set.
pause
exit /b 1

:ExecuteJPackage
echo       Using jpackage path: "!JPACKAGE_CMD!"

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
    echo [ERROR] jpackage execution failed.
    pause
    exit /b 1
)

echo.
echo Done! Installer is located at:
echo    %OUTPUT_DIR%\
echo.
echo User data will be saved at:
echo    %%APPDATA%%\%APP_NAME%\
echo    (includes tasks.json, ticket_monitor.db, sounds\)
echo.
pause
endlocal