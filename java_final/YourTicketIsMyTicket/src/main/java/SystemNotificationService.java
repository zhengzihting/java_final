import dorkbox.notify.Notify;
import dorkbox.notify.Position;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Desktop;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;
import kotlin.Unit;

public class SystemNotificationService {
    private final Consumer<String> logger;

    /** 預設 macOS 系統音效 */
    public static final String DEFAULT_MAC_SOUND = "/System/Library/Sounds/Glass.aiff";

    /** 當前使用的音效路徑；空字串或 null 代表使用預設 */
    private String soundPath = DEFAULT_MAC_SOUND;

    /** 正在播放的通知音效行程；null 代表未在播放 */
    private volatile Process notifyProcess = null;

    /** 通知 icon，從 resources 載入，失敗時為 null */
    private static final Image NOTIFY_ICON = loadIcon();

    public SystemNotificationService(Consumer<String> logger) {
        this.logger = logger;
    }

    // -------------------------------------------------------------------------
    // 音效設定
    // -------------------------------------------------------------------------

    /**
     * 設定通知音效路徑。
     * @param path 音效檔路徑（aiff/wav/mp3 等），null 或空字串還原為預設音效
     */
    public void setSoundPath(String path) {
        this.soundPath = (path == null || path.isBlank()) ? DEFAULT_MAC_SOUND : path;
    }

    public String getSoundPath() {
        return soundPath;
    }

    // -------------------------------------------------------------------------
    // 主要公開方法
    // -------------------------------------------------------------------------

    public void notifyTicketAvailable(String ticketUrl, String details) {
        // 1. 播放音效（非同步，不阻塞 UI）
        playAlertSound();

        // 2. 顯示視覺通知（dorkbox Notify）
        try {
            var builder = Notify.Companion.create()
                    .title("YourTicketIsMyTicket")
                    .text("偵測到符合條件的釋票，點擊通知開啟購票頁面。")
                    .hideAfter(12000)
                    .position(Position.BOTTOM_RIGHT)
                    .onClickAction(notification -> {
                        openTicketUrl(ticketUrl);
                        notification.close();
                        return Unit.INSTANCE;
                    });

            if (NOTIFY_ICON != null) {
                // ⚠️ 不能用 showInformation()，那會覆蓋掉我們的 image！
                builder.image(NOTIFY_ICON).show();
            } else {
                builder.showInformation();
            }
            log("已發送系統通知。詳細資訊：" + details);
        } catch (Exception e) {
            log("系統通知發送失敗：" + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 音效播放
    // -------------------------------------------------------------------------

    /**
     * 在 macOS 上使用 afplay 播放音效；其他平台 beep。
     */
    private void playAlertSound() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            playMacSound(soundPath);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * 使用 afplay 播放指定路徑的音效（背景執行）。
     * 若前一個通知音效仍在播放，會先將其停止再開始新的，
     * 確保同一時間只有一個通知音效在播放。
     */
    private void playMacSound(String path) {
        if (path == null || path.isBlank()) path = DEFAULT_MAC_SOUND;
        // 停止上一個尚未播完的通知音效
        stopNotifySound();
        try {
            notifyProcess = new ProcessBuilder("afplay", path)
                    .redirectErrorStream(true)
                    .start();
        } catch (Exception e) {
            log("播放音效失敗：" + e.getMessage());
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * 強制停止正在播放的通知音效。
     * 若無音效在播放則無動作。
     */
    public void stopNotifySound() {
        Process p = notifyProcess;
        if (p != null && p.isAlive()) {
            p.destroy();
        }
    }

    /** 目前通知音效是否正在播放。 */
    public boolean isNotifySoundPlaying() {
        return notifyProcess != null && notifyProcess.isAlive();
    }

    // -------------------------------------------------------------------------
    // Icon 載入
    // -------------------------------------------------------------------------

    private static Image loadIcon() {
        try (InputStream is = SystemNotificationService.class
                .getResourceAsStream("/notify_icon.png")) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // 開啟票務頁面
    // -------------------------------------------------------------------------

    private void openTicketUrl(String ticketUrl) {
        try {
            if (!Desktop.isDesktopSupported()) {
                log("無法開啟購票頁面：此系統不支援 Desktop API。");
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                log("無法開啟購票頁面：此系統不支援預設瀏覽器跳轉。");
                return;
            }
            desktop.browse(URI.create(ticketUrl));
            log("已開啟預設瀏覽器：" + ticketUrl);
        } catch (IllegalArgumentException e) {
            log("無法開啟購票頁面：網址格式不合法。");
        } catch (Exception e) {
            log("無法開啟購票頁面：" + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // 日誌
    // -------------------------------------------------------------------------

    private void log(String message) {
        if (logger != null) {
            logger.accept(message);
        }
    }
}
