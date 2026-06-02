import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 跨平台音效播放工具類（macOS / Windows / Linux）。
 *
 * <ul>
 *   <li>WAV / AIFF：{@code javax.sound.sampled}（Java 內建，不需外部指令）</li>
 *   <li>MP3 / M4A：{@code javafx.scene.media.MediaPlayer}</li>
 *   <li>預設音效：依 OS 自動選擇系統內建聲音路徑；
 *       無對應平台（Linux 等）使用 {@code Toolkit.beep()}</li>
 * </ul>
 *
 * <h3>儲存慣例</h3>
 * <p>tasks.json 的 {@code soundPath} 欄位：
 * <ul>
 *   <li>空字串 / null → 使用 OS 預設音效（{@link #DEFAULT_SENTINEL}）</li>
 *   <li>相對檔名（如 {@code alert.mp3}）→ 自訂音效</li>
 * </ul>
 */
public class SoundPlayer {

    /**
     * 「使用系統預設音效」的儲存用 sentinel 值；跨平台一致，不含 OS 路徑。
     * 寫入 tasks.json 時使用此空字串代表預設，避免跨平台路徑不相容。
     */
    public static final String DEFAULT_SENTINEL = "";

    private final AtomicReference<Clip>        currentClip  = new AtomicReference<>();
    private final AtomicReference<MediaPlayer> currentMedia = new AtomicReference<>();

    // ─── 靜態工具 ──────────────────────────────────────────────────────────────

    /**
     * 取得當前 OS 的預設系統音效完整路徑。
     * Linux 等無對應系統音效的平台回傳 {@code null}（會改用 beep()）。
     */
    public static String resolveDefaultSoundPath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return "/System/Library/Sounds/Glass.aiff";
        if (os.contains("win")) return "C:\\Windows\\Media\\Windows Notify System Generic.wav";
        return null; // Linux / 其他 → beep()
    }

    /**
     * UI 顯示用標籤：顯示系統音效實際路徑，
     * 若平台無對應路徑則顯示 {@code "(系統 beep)"}。
     */
    public static String defaultDisplayLabel() {
        String p = resolveDefaultSoundPath();
        return (p != null) ? p : "(系統 beep)";
    }

    // ─── 公開 API ──────────────────────────────────────────────────────────────

    /**
     * 非同步播放音效；若有音效正在播放會先停止。
     *
     * @param path    完整路徑，null 或空字串代表使用 OS 預設音效
     * @param onError 失敗時的 Consumer（在背景執行緒或 FX 執行緒回呼）
     */
    public void playAsync(String path, Consumer<String> onError) {
        stopAll();
        if (path == null || path.isBlank()) {
            playDefaultAsync(onError);
            return;
        }
        String lower = path.toLowerCase();
        if (lower.endsWith(".mp3") || lower.endsWith(".m4a")) {
            playWithMediaPlayer(path, onError);
        } else {
            // WAV / AIFF → javax.sound.sampled（不需外部行程）
            Thread t = new Thread(() -> playFileClip(path, onError), "sound-player");
            t.setDaemon(true);
            t.start();
        }
    }

    /**
     * 播放 OS 預設系統音效；無預設路徑的平台（Linux）使用 beep()。
     */
    public void playDefaultAsync(Consumer<String> onError) {
        stopAll();
        String defaultPath = resolveDefaultSoundPath();
        if (defaultPath == null) {
            beep();
            return;
        }
        Thread t = new Thread(() -> playFileClip(defaultPath, onError), "sound-default");
        t.setDaemon(true);
        t.start();
    }

    /** 停止所有正在播放的音效（Clip + MediaPlayer）。 */
    public void stopAll() {
        Clip clip = currentClip.getAndSet(null);
        if (clip != null) {
            clip.stop();
            clip.close();
        }
        MediaPlayer mp = currentMedia.getAndSet(null);
        if (mp != null) {
            Platform.runLater(mp::dispose);
        }
    }

    /** 是否有音效正在播放中。 */
    public boolean isPlaying() {
        Clip clip = currentClip.get();
        if (clip != null && clip.isRunning()) return true;
        MediaPlayer mp = currentMedia.get();
        return mp != null && mp.getStatus() == MediaPlayer.Status.PLAYING;
    }

    // ─── 私有實作 ──────────────────────────────────────────────────────────────

    private void playFileClip(String path, Consumer<String> onError) {
        File file = new File(path);
        if (!file.exists()) {
            if (onError != null) onError.accept("音效檔案不存在：" + path + "，改用系統 beep");
            beep();
            return;
        }
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
            playAudioStream(ais, onError);
        } catch (Exception e) {
            if (onError != null) onError.accept("播放音效失敗：" + e.getMessage());
            beep();
        }
    }

    private void playAudioStream(AudioInputStream ais, Consumer<String> onError) throws Exception {
        AudioFormat format = ais.getFormat();
        DataLine.Info info  = new DataLine.Info(Clip.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            // 嘗試轉換為標準 16-bit PCM（部分 AIFF 或特殊 WAV 格式需要）
            AudioFormat pcm = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(), 16,
                    format.getChannels(), format.getChannels() * 2,
                    format.getSampleRate(), false);
            AudioInputStream converted = AudioSystem.getAudioInputStream(pcm, ais);
            DataLine.Info pcmInfo = new DataLine.Info(Clip.class, pcm);
            if (!AudioSystem.isLineSupported(pcmInfo)) {
                if (onError != null) onError.accept("此平台不支援音訊格式，改用系統 beep");
                beep();
                return;
            }
            openAndPlayClip((Clip) AudioSystem.getLine(pcmInfo), converted);
            return;
        }

        openAndPlayClip((Clip) AudioSystem.getLine(info), ais);
    }

    private void openAndPlayClip(Clip clip, AudioInputStream ais) throws Exception {
        clip.open(ais);
        currentClip.set(clip);
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                clip.close();
                currentClip.compareAndSet(clip, null);
            }
        });
        clip.start();
        // drain() 阻塞背景執行緒直到播放完畢；若 stopAll() 被呼叫則提早返回
        clip.drain();
    }

    private void playWithMediaPlayer(String path, Consumer<String> onError) {
        Platform.runLater(() -> {
            try {
                Media media = new Media(new File(path).toURI().toString());
                MediaPlayer mp = new MediaPlayer(media);
                currentMedia.set(mp);
                mp.setOnEndOfMedia(() -> {
                    mp.dispose();
                    currentMedia.compareAndSet(mp, null);
                });
                mp.setOnError(() -> {
                    String msg = mp.getError() != null ? mp.getError().getMessage() : "未知錯誤";
                    if (onError != null) onError.accept("MP3 播放失敗：" + msg);
                    currentMedia.compareAndSet(mp, null);
                });
                mp.play();
            } catch (Exception e) {
                if (onError != null) onError.accept("無法載入媒體：" + e.getMessage());
            }
        });
    }

    private static void beep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }
}
