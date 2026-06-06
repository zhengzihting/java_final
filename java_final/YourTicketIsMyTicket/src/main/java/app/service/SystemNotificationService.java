package app.service;
import dorkbox.notify.Notify;
import dorkbox.notify.Position;
import java.awt.*;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import kotlin.Unit;

public class SystemNotificationService {
    private final Consumer<String> logger;
    private final SoundPlayer soundPlayer = new SoundPlayer();

    /**
     * 當前使用的音效路徑。
     * 空字串（{@link SoundPlayer#DEFAULT_SENTINEL}）代表使用 OS 預設音效。
     */
    private String soundPath = SoundPlayer.DEFAULT_SENTINEL;



    /** 通知 icon，從 resources 載入，失敗時為 null */
    private static final Image NOTIFY_ICON = loadIcon();

    private boolean popupEnabled = true;

    public SystemNotificationService(Consumer<String> logger) {
        this.logger = logger;
    }

    public void setPopupEnabled(boolean enabled) {
        this.popupEnabled = enabled;
    }

    public boolean isPopupEnabled() {
        return popupEnabled;
    }

    // ── 音效設定 ──────────────────────────────────────────────────────────────

    /**
     * 設定通知音效路徑。
     * @param path 音效檔完整路徑；null 或空字串代表使用 OS 預設音效
     */
    public void setSoundPath(String path) {
        this.soundPath = (path == null || path.isBlank())
                ? SoundPlayer.DEFAULT_SENTINEL
                : path;
    }

    public String getSoundPath() {
        return soundPath;
    }

    // ── 主要公開方法 ──────────────────────────────────────────────────────────

    public void notifyTicketAvailable(String ticketUrl, String details) {

        // 1. 播放音效（非同步，不阻塞 UI；空字串 → OS 預設音效）
        soundPlayer.playAsync(soundPath, this::log);

        // 2. 顯示視覺通知（dorkbox Notify），如果開啟彈出式通知
        if (popupEnabled) {
            try {

                var builder = Notify.Companion.create()
                        .title("YourTicketIsMyTicket")
                        .text("偵測到符合條件的釋票，點擊通知開啟購票頁面。")
                        .hideAfter(12000)
                        .position(Position.TOP_RIGHT)
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
        } else {
            log("偵測到釋票，但彈出式通知已關閉。詳細資訊：" + details);
        }
    }

    // ── 音效控制（公開） ──────────────────────────────────────────────────────

    /**
     * 強制停止正在播放的通知音效。
     * 若無音效在播放則無動作。
     */
    public void stopNotifySound() {
        soundPlayer.stopAll();
    }

    /** 目前通知音效是否正在播放。 */
    public boolean isNotifySoundPlaying() {
        return soundPlayer.isPlaying();
    }

    // ── Icon 載入 ──────────────────────────────────────────────────────────────

    private static Image loadIcon() {
        try (InputStream is = SystemNotificationService.class
                .getResourceAsStream("/notify_icon.png")) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (Exception e) {
            return null;
        }
    }

    // ── 開啟票務頁面 ──────────────────────────────────────────────────────────

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

    // ── 日誌 ──────────────────────────────────────────────────────────────────

    private void log(String message) {
        if (logger != null) {
            logger.accept(message);
        }
    }
}
