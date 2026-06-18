package app.util;

import java.nio.file.Path;

/**
 * 集中管理應用程式資料目錄，支援 macOS、Windows、Linux。
 *
 * <h3>開發模式（IDE / {@code mvn javafx:run}）</h3>
 * <p>使用專案根目錄（{@code user.dir}），行為與原來完全相同。</p>
 *
 * <h3>打包後（jpackage 產生的原生安裝程式）</h3>
 * <p>由 jpackage 的 {@code --java-options -Dapp.packaged=true} 啟用，
 * 依作業系統選擇標準應用程式資料目錄：</p>
 * <ul>
 *   <li><b>macOS</b>：{@code ~/Library/Application Support/YourTicketIsMyTicket/}</li>
 *   <li><b>Windows</b>：{@code %APPDATA%\YourTicketIsMyTicket\}
 *       （即 {@code C:\Users\<user>\AppData\Roaming\YourTicketIsMyTicket\}）</li>
 *   <li><b>Linux</b>：{@code ~/.local/share/YourTicketIsMyTicket/}
 *       （遵循 XDG Base Directory 規範）</li>
 * </ul>
 */
public final class AppDirs {

    private static final String APP_NAME = "YourTicketIsMyTicket";

    /** 應用程式資料根目錄（懶載入，thread-safe） */
    private static volatile Path dataRoot;

    private AppDirs() {}

    /**
     * 取得應用程式資料根目錄。
     * 首次呼叫時依平台與啟動模式決定，後續呼叫直接回傳快取值。
     */
    public static Path dataRoot() {
        if (dataRoot == null) {
            synchronized (AppDirs.class) {
                if (dataRoot == null) {
                    dataRoot = resolveDataRoot();
                }
            }
        }
        return dataRoot;
    }

    /** {@code tasks.json} 完整路徑 */
    public static Path tasksJson() {
        return dataRoot().resolve("tasks.json");
    }

    /**
     * SQLite 資料庫 JDBC URL（含完整絕對路徑）。
     * <p>使用絕對路徑避免 SQLite 在不同 CWD 下建立多份資料庫。</p>
     */
    public static String dbUrl() {
        return "jdbc:sqlite:" + dataRoot().resolve("ticket_monitor.db").toAbsolutePath();
    }

    /** {@code sounds/} 目錄完整路徑 */
    public static Path soundsDir() {
        return dataRoot().resolve("sounds");
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static Path resolveDataRoot() {
        // 開發模式：JVM property 未設定 → 使用專案根目錄（行為不變）
        if (!Boolean.getBoolean("app.packaged")) {
            return Path.of(System.getProperty("user.dir"));
        }

        // 打包後：依 OS 選擇標準資料目錄
        String os  = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            // Windows：%APPDATA%\YourTicketIsMyTicket\
            // 優先使用 APPDATA 環境變數；若不存在則 fallback 到 user.home\AppData\Roaming
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isBlank()) {
                appData = home + "\\AppData\\Roaming";
            }
            return Path.of(appData, APP_NAME);

        } else if (os.contains("mac")) {
            // macOS：~/Library/Application Support/YourTicketIsMyTicket/
            return Path.of(home, "Library", "Application Support", APP_NAME);

        } else {
            // Linux / 其他 Unix：~/.local/share/YourTicketIsMyTicket/（XDG 規範）
            String xdgData = System.getenv("XDG_DATA_HOME");
            if (xdgData == null || xdgData.isBlank()) {
                xdgData = home + "/.local/share";
            }
            return Path.of(xdgData, APP_NAME);
        }
    }
}

