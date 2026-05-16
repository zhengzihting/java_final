import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    // 設定 SQLite 資料庫檔案名稱，會在專案根目錄自動生成這個檔案
    private static final String DB_URL = "jdbc:sqlite:ticket_monitor.db";

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
        
        String sql = "SELECT timestamp, message FROM history ORDER BY id DESC LIMIT 20";
        
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
}