#!/bin/bash
# =============================================================================
# package-mac.sh — 在 macOS 上將專案打包成 .app（與可選的 .dmg）
#
# 用法：
#   chmod +x package-mac.sh
#   ./package-mac.sh          # 產生 .app
#   ./package-mac.sh --dmg    # 額外產生 .dmg 安裝檔
# =============================================================================
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_NAME="YourTicketIsMyTicket"
APP_VERSION="1.0"
MAIN_JAR="${APP_NAME}-1.0-SNAPSHOT.jar"
MAIN_CLASS="app.main.Launcher"

INPUT_DIR="${PROJECT_DIR}/target/jpackage-input"
OUTPUT_DIR="${PROJECT_DIR}/target/dist"

# 圖示檔（macOS 使用 .icns）
ICON="${PROJECT_DIR}/src/main/resources/app.icns"

# ─── 1. Maven 打包成 Fat JAR ─────────────────────────────────────────────────
echo "📦 [1/3] 執行 mvn clean package ..."
cd "${PROJECT_DIR}"
mvn clean package -q
echo "    ✓ Fat JAR 已產生：target/${MAIN_JAR}"

# ─── 2. 準備 jpackage 輸入目錄 ───────────────────────────────────────────────
echo "📁 [2/3] 準備輸入目錄 ..."
rm -rf "${INPUT_DIR}"
mkdir -p "${INPUT_DIR}"

# 複製 Fat JAR
cp "target/${MAIN_JAR}" "${INPUT_DIR}/"

# sounds/ 資料夾：使用者資料（打包後存放於 ~/Library/Application Support/）
# 注意：不將 sounds/ 打進 .app，讓使用者自行管理音效
echo "    ✓ 輸入目錄準備完成"

# ─── 3. 執行 jpackage ─────────────────────────────────────────────────────────
echo "🔧 [3/3] 執行 jpackage ..."
rm -rf "${OUTPUT_DIR}"
mkdir -p "${OUTPUT_DIR}"

JPACKAGE_ARGS=(
    --type app-image
    --name "${APP_NAME}"
    --app-version "${APP_VERSION}"
    --input "${INPUT_DIR}"
    --main-jar "${MAIN_JAR}"
    --main-class "${MAIN_CLASS}"
    --dest "${OUTPUT_DIR}"
    # 標記為已打包，讓 AppDirs 使用正確的資料目錄
    --java-options "-Dapp.packaged=true"
    --java-options "--enable-native-access=ALL-UNNAMED"
    --java-options "-Xmx512m"
    --java-options "-Dfile.encoding=UTF-8"
)

# 若圖示存在則加入
if [ -f "${ICON}" ]; then
    JPACKAGE_ARGS+=(--icon "${ICON}")
fi

jpackage "${JPACKAGE_ARGS[@]}"

echo ""
echo "✅ 完成！.app 位於："
echo "   ${OUTPUT_DIR}/${APP_NAME}.app"
echo ""

# 可選：另外產生 .dmg
if [[ "$1" == "--dmg" ]]; then
    echo "💿 產生 .dmg 安裝檔 ..."
    jpackage \
        --type dmg \
        --name "${APP_NAME}" \
        --app-version "${APP_VERSION}" \
        --input "${INPUT_DIR}" \
        --main-jar "${MAIN_JAR}" \
        --main-class "${MAIN_CLASS}" \
        --dest "${OUTPUT_DIR}" \
        --java-options "-Dapp.packaged=true" \
        --java-options "--enable-native-access=ALL-UNNAMED" \
        --java-options "-Xmx512m" \
        --java-options "-Dfile.encoding=UTF-8"
    echo "✅ .dmg 位於：${OUTPUT_DIR}/${APP_NAME}-${APP_VERSION}.dmg"
fi

echo ""
echo "📂 使用者資料將儲存於："
echo "   ~/Library/Application Support/${APP_NAME}/"
echo "   （包含 tasks.json、ticket_monitor.db、sounds/）"
