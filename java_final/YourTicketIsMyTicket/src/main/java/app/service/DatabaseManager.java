package app.service;

import app.main.MainApp;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // 設定 SQLite 資料庫檔案名稱，會在專案根目錄自動生成這個檔案
    private static final String DB_URL = "jdbc:sqlite:" + getSafeDatabasePath();

    private static String getSafeDatabasePath() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();
        File appDataFolder;

        if (os.contains("mac")) {
            appDataFolder = new File(userHome, "Library/Application Support/YourTicketIsMyTicket");
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            appDataFolder = new File(appData != null ? appData : userHome, "YourTicketIsMyTicket");
        } else {
            appDataFolder = new File(userHome, ".YourTicketIsMyTicket");
        }

        // 如果專屬資料夾不存在，先幫它建立起來
        if (!appDataFolder.exists()) {
            appDataFolder.mkdirs();
        }

        return new File(appDataFolder, "ticket_monitor.db").getAbsolutePath();
    }

    /**
     * 結構化歷史紀錄物件
     */
    public static class HistoryEntry {
        public final int id;
        public final String timestamp;
        public final String message;

        public HistoryEntry(int id, String timestamp, String message) {
            this.id = id;
            this.timestamp = timestamp;
            this.message = message;
        }

        @Override
        public String toString() {
            return "[" + timestamp + "]  " + message;
        }
    }

    /**
     * 1. 初始化資料庫：建立儲存紀錄的資料表
     */
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // 如果表不存在，就建立一個名為 history 的資料表
            String sql = "CREATE TABLE IF NOT EXISTS history (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
                         "message TEXT NOT NULL" +
                         ");";
            stmt.execute(sql);
            System.out.println("QLite 資料庫初始化成功！");
        } catch (Exception e) {
            System.err.println("資料庫初始化失敗: " + e.getMessage());
        }
    }

    /**
     * 2. 新增紀錄：讓 TicketMonitor 抓到資料時可以存進來
     */
    public static void saveLog(String message) {
        String sql = "INSERT INTO history(message) VALUES(?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, message);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("儲存歷史紀錄失敗: " + e.getMessage());
        }
    }

    /**
     * 3. 讀取紀錄：讓 MainApp 的「歷史紀錄」按鈕可以撈出資料
     */
    public static String getRecentHistory() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 歷史監控紀錄 (最近20筆) =====\n");
        
        String sql = "SELECT datetime(timestamp, 'localtime') AS timestamp, message FROM history ORDER BY id DESC LIMIT 20";
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                // 格式化輸出：[時間] 訊息內容
                sb.append("[").append(rs.getString("timestamp")).append("] ")
                  .append(rs.getString("message")).append("\n");
            }
            
            if (!hasData) {
                return "目前本地資料庫尚無紀錄。";
            }
            
        } catch (Exception e) {
            return "無法讀取歷史紀錄: " + e.getMessage();
        }
        
        return sb.toString();
    }

    /**
     * 4. 只撈出監控條件格式的紀錄（包含「網址：」關鍵字），最近 50 筆
     *    供歷史紀錄彈出視窗使用
     */
    public static List<HistoryEntry> getMonitoringHistory() {
        List<HistoryEntry> entries = new ArrayList<>();
        String sql = "SELECT id, datetime(timestamp, 'localtime') AS timestamp, message " +
                     "FROM history WHERE message LIKE '%網址：%' " +
                     "ORDER BY id DESC LIMIT 50";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                entries.add(new HistoryEntry(
                    rs.getInt("id"),
                    rs.getString("timestamp"),
                    rs.getString("message")
                ));
            }
        } catch (Exception e) {
            System.err.println("讀取監控歷史失敗: " + e.getMessage());
        }

        return entries;
    }

    /**
     * 5. 清除所有歷史紀錄
     */
    public static void clearHistory() {
        String sql = "DELETE FROM history";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.err.println("清除歷史紀錄失敗: " + e.getMessage());
        }
    }

    /**
     * 6. 刪除指定的歷史紀錄
     */
    public static void deleteHistoryEntry(int id) {
        String sql = "DELETE FROM history WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("刪除單筆紀錄失敗: " + e.getMessage());
        }
    }
}