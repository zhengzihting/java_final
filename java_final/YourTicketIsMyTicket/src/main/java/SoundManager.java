import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 負責所有音效相關的狀態管理。
 * 不持有任何 UI 控制項的直接引用（除了 soundPathLabel），
 * 視覺切換由 {@link SoundToggleSwitch} 負責，兩者透過 MainApp 協作。
 *
 * <h3>音效庫資料夾</h3>
 * <p>自訂音效統一存放於 {@link #SOUNDS_DIR}（{@code ~/.yourticket/sounds/}）。
 * tasks.json 中只儲存相對檔名（例如 {@code alert.aiff}），
 * 實際播放路徑由 {@link #resolveSelectedSoundPath()} 動態組合。
 * 使用者可呼叫 {@link #openSoundsFolder()} 在 Finder 中開啟該資料夾複製音效。</p>
 *
 * <h3>試聽播放管理</h3>
 * <ul>
 *   <li>呼叫 {@link #previewSound()} 播放試聽；若已在播放，先停舊的再開始新的。</li>
 *   <li>停止音效統一透過 {@link #stopAllSounds()} 或 {@link #stopPreview()}。</li>
 *   <li>切換音效路徑時會自動停止正在試聽的音效。</li>
 * </ul>
 */
public class SoundManager {

    public static final String DEFAULT_LABEL = "🔔 預設音效";
    public static final String CUSTOM_LABEL  = "📂 自訂檔案...";

    /** 音效庫資料夾：Maven 專案根目錄下的 sounds/ */
    public static final Path SOUNDS_DIR =
            Path.of(System.getProperty("user.dir"), "sounds");

    private final Label soundPathLabel;
    private final SystemNotificationService notificationService;
    private final Consumer<String> logger;

    private boolean customSelected  = false;
    /** 相對檔名，例如 "alert.aiff"；搭配 {@link #SOUNDS_DIR} 組成完整路徑。 */
    private String  customSoundPath = null;

    /** 正在執行的試聽 afplay 行程；null 代表未在播放。 */
    private volatile Process previewProcess = null;

    public SoundManager(Label soundPathLabel,
                        SystemNotificationService notificationService,
                        Consumer<String> logger) {
        this.soundPathLabel      = soundPathLabel;
        this.notificationService = notificationService;
        this.logger              = logger;
        initSoundsDir();
    }

    // ── 音效庫資料夾 ──────────────────────────────────────────────────────────

    /** 確保音效庫資料夾存在（初次啟動時建立）。 */
    private void initSoundsDir() {
        try {
            Files.createDirectories(SOUNDS_DIR);
        } catch (Exception e) {
            logger.accept("無法建立音效資料夾：" + e.getMessage());
        }
    }

    /**
     * 在 Finder 中開啟 {@link #SOUNDS_DIR}，讓使用者方便複製音效檔案進去。
     */
    public void openSoundsFolder() {
        try {
            new ProcessBuilder("open", SOUNDS_DIR.toString()).start();
            logger.accept("已開啟音效資料夾：" + SOUNDS_DIR);
        } catch (Exception e) {
            logger.accept("無法開啟音效資料夾：" + e.getMessage());
        }
    }

    // ── 音效路徑管理 ─────────────────────────────────────────────────────────

    /**
     * 切換至自訂音效時的處理邏輯：
     * <ol>
     *   <li>若 {@link #SOUNDS_DIR} 內有音效檔案，自動選擇第一個（字母序）。</li>
     *   <li>若目錄為空，在 Finder 中開啟該資料夾提示使用者複製音效後再試，
     *       並回傳 {@code false}（滑塊留在「預設」）。</li>
     * </ol>
     * 若目前正在試聽，會先停止。
     *
     * @return true 代表成功選定音效；false 代表目錄為空（未切換）。
     */
    public boolean handleCustomSelection() {
        stopPreview();

        List<File> available = listSoundFiles();

        if (!available.isEmpty()) {
            // ── 有音效：自動選第一個（字母序） ──────────────────────────────
            File file = available.get(0);
            customSoundPath = file.getName();
            customSelected  = true;
            String resolvedPath = resolveSelectedSoundPath();
            soundPathLabel.setText(resolvedPath);
            notificationService.setSoundPath(resolvedPath);
            logger.accept("已自動選擇音效：" + resolvedPath);
            return true;
        }

        // ── 沒有音效：開啟資料夾讓使用者複製後再試 ──────────────────────────
        openSoundsFolder();
        logger.accept("音效資料夾為空，請將音效檔案放入後再切換。");
        return false;
    }

    /**
     * 開啟檔案選擇器選擇特定的自訂音效檔案（右鍵點擊「自訂」時觸發）。
     * 若選取的檔案不在 {@link #SOUNDS_DIR} 內，會自動將其複製到資料夾。
     *
     * @return true 代表成功選定；false 代表取消。
     */
    public boolean chooseSpecificCustomSound(Stage stage) {
        stopPreview();

        FileChooser fc = new FileChooser();
        fc.setTitle("選擇音效檔案");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("音效檔案", "*.aiff", "*.aif", "*.wav", "*.mp3", "*.m4a"),
                new FileChooser.ExtensionFilter("所有檔案", "*.*")
        );

        File soundsDirFile = SOUNDS_DIR.toFile();
        if (soundsDirFile.exists() && soundsDirFile.isDirectory()) {
            fc.setInitialDirectory(soundsDirFile);
        }

        File file = fc.showOpenDialog(stage);
        if (file != null) {
            Path source = file.toPath();
            Path target = SOUNDS_DIR.resolve(file.getName());

            try {
                if (!Files.isSameFile(source, target)) {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    logger.accept("已將音效複製至音效資料夾：" + file.getName());
                }
            } catch (Exception e) {
                logger.accept("複製音效檔案失敗：" + e.getMessage());
                // 若複製失敗但至少我們知道要用這個名稱（可能會播放失敗）
            }

            customSoundPath = file.getName();
            customSelected = true;
            String resolvedPath = resolveSelectedSoundPath();
            soundPathLabel.setText(resolvedPath);
            notificationService.setSoundPath(resolvedPath);
            logger.accept("已設定自訂音效：" + resolvedPath);
            return true;
        }
        return false;
    }

    /**
     * 列舉 {@link #SOUNDS_DIR} 中所有支援的音效檔案，依字母排序。
     */
    private List<File> listSoundFiles() {
        File dir = SOUNDS_DIR.toFile();
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();

        File[] files = dir.listFiles((d, name) -> {
            String n = name.toLowerCase();
            return n.endsWith(".aiff") || n.endsWith(".aif")
                || n.endsWith(".wav")  || n.endsWith(".mp3")
                || n.endsWith(".m4a");
        });

        if (files == null || files.length == 0) return Collections.emptyList();
        Arrays.sort(files); // 字母排序，確保每次結果一致
        return Arrays.asList(files);
    }

    /**
     * 切換回預設音效，清除自訂路徑。
     * 若目前正在試聽，會先停止。
     */
    public void selectDefault() {
        stopPreview();
        customSelected  = false;
        customSoundPath = null;
        soundPathLabel.setText(SystemNotificationService.DEFAULT_MAC_SOUND);
        notificationService.setSoundPath(SystemNotificationService.DEFAULT_MAC_SOUND);
    }

    /**
     * 取得目前選擇的音效完整路徑（供 afplay 與儲存任務使用）。
     *
     * <p>新版：{@link #SOUNDS_DIR} + 相對檔名 → 完整絕對路徑。</p>
     */
    public String resolveSelectedSoundPath() {
        if (!customSelected || customSoundPath == null) {
            return SystemNotificationService.DEFAULT_MAC_SOUND;
        }
        // ── 新版：組合音效庫資料夾 + 相對檔名 ──────────────────────────────
        return SOUNDS_DIR.resolve(customSoundPath).toString();
        // ── 舊版：直接回傳儲存的絕對路徑 ────────────────────────────────────
        // return customSoundPath;
    }

    /**
     * 從 tasks.json 儲存值還原音效狀態。
     * 儲存值為相對檔名（新版）；預設音效則為空或 DEFAULT_MAC_SOUND。
     *
     * @return true 代表還原為自訂路徑；false 代表還原為預設。
     */
    public boolean restoreSoundPath(String savedPath) {
        if (savedPath == null || savedPath.isBlank()
                || savedPath.equals(SystemNotificationService.DEFAULT_MAC_SOUND)) {
            selectDefault();
            return false;
        }
        customSoundPath = savedPath; // 相對檔名
        customSelected  = true;
        // ── 新版：解析並顯示完整路徑 ─────────────────────────────────────────
        String resolvedPath = resolveSelectedSoundPath();
        soundPathLabel.setText(resolvedPath);
        notificationService.setSoundPath(resolvedPath);
        // ── 舊版：savedPath 本身即絕對路徑 ──────────────────────────────────
        // soundPathLabel.setText(savedPath);
        // notificationService.setSoundPath(savedPath);
        return true;
    }

    // ── 試聽播放控制 ─────────────────────────────────────────────────────────

    /**
     * 試聽目前選擇的音效。
     * 若已有試聽正在播放，先將其停止再重新播放。
     * 停止音效請使用 {@link #stopAllSounds()} 或 {@link #stopPreview()}。
     */
    public void previewSound() {
        stopPreview();
        String path = resolveSelectedSoundPath();
        Thread t = new Thread(() -> {
            try {
                previewProcess = new ProcessBuilder("afplay", path)
                        .redirectErrorStream(true)
                        .start();
                previewProcess.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                logger.accept("試聽失敗：" + ex.getMessage());
            } finally {
                previewProcess = null;
            }
        }, "sound-preview");
        t.setDaemon(true);
        t.start();
    }

    /**
     * 強制終止正在播放的試聽音效。
     * 若未在播放則無動作。
     */
    public void stopPreview() {
        Process p = previewProcess;
        if (p != null && p.isAlive()) {
            p.destroy();
        }
    }

    /**
     * 停止所有音效（試聽 + 通知音效）。
     * 供「🔇 靜音」按鈕與視窗關閉時呼叫。
     */
    public void stopAllSounds() {
        stopPreview();
        notificationService.stopNotifySound();
    }
}
