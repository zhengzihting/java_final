import java.util.function.Consumer;
import javafx.application.Platform;
import java.util.Timer;
import java.util.TimerTask;

public class TicketMonitor {
    private String url;
    private String keyword;
    private Consumer<String> callback;
    private Timer timer;

    // 建構子：接收網址、關鍵字和回傳訊息的工具
    public TicketMonitor(String url, String keyword, Consumer<String> callback) {
        this.url = url;
        this.keyword = keyword;
        this.callback = callback;
    }

    // 開始監控的模擬邏輯
    public void startMonitoring(int intervalSeconds) {
        timer = new Timer(true);
        callback.accept("模擬監控啟動：正在盯著 " + url);
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // 這裡模擬爬蟲抓取的情況
                Platform.runLater(() -> {
                    callback.accept("正在檢查是否有 [" + keyword + "] 的釋票...");
                });
            }
        }, 1000, intervalSeconds * 1000);
    }

    // 停止監控
    public void stopMonitoring() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}