package app.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 負責將 JAR 內嵌的預設音效解壓縮到使用者的 sounds/ 資料夾。
 *
 * <h3>設計原則</h3>
 * <ul>
 *   <li>只在目標檔案<b>不存在</b>時才複製，不覆蓋使用者已有的音效。</li>
 *   <li>音效清單由 {@code /sounds/manifest.txt} 維護（每行一個檔名）。</li>
 *   <li>新增音效時：把檔案放入 {@code src/main/resources/sounds/}，
 *       並在 {@code manifest.txt} 加一行即可。</li>
 * </ul>
 */
public final class BundledSoundsInstaller {

    /** JAR 內音效資源的根路徑 */
    private static final String RESOURCE_PREFIX = "/sounds/";

    /** 清單檔路徑（JAR 內） */
    private static final String MANIFEST = RESOURCE_PREFIX + "manifest.txt";

    private BundledSoundsInstaller() {}

    /**
     * 將 manifest.txt 中列出的所有音效複製到 {@link AppDirs#soundsDir()}。
     * <p>已存在的檔案不會被覆蓋，確保使用者的自訂音效不受影響。</p>
     *
     * @param logger 日誌回呼（可傳 {@code System.out::println}）
     */
    public static void installIfNeeded(java.util.function.Consumer<String> logger) {
        Path soundsDir = AppDirs.soundsDir();

        // 確保 sounds/ 目錄存在
        try {
            Files.createDirectories(soundsDir);
        } catch (IOException e) {
            logger.accept("無法建立音效目錄：" + e.getMessage());
            return;
        }

        // 讀取清單
        List<String> filenames = readManifest(logger);
        if (filenames.isEmpty()) return;

        // 逐一複製（跳過已存在的）
        for (String filename : filenames) {
            Path target = soundsDir.resolve(filename);
            if (Files.exists(target)) {
                // 已存在，不覆蓋
                continue;
            }
            String resourcePath = RESOURCE_PREFIX + filename;
            try (InputStream in = BundledSoundsInstaller.class.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    logger.accept("找不到內建音效資源：" + resourcePath);
                    continue;
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                logger.accept("已安裝預設音效：" + filename);
            } catch (IOException e) {
                logger.accept("複製音效失敗 [" + filename + "]：" + e.getMessage());
            }
        }
    }

    /** 讀取 manifest.txt，回傳所有非空非註解的行。 */
    private static List<String> readManifest(java.util.function.Consumer<String> logger) {
        List<String> result = new ArrayList<>();
        try (InputStream in = BundledSoundsInstaller.class.getResourceAsStream(MANIFEST)) {
            if (in == null) {
                logger.accept("找不到音效清單：" + MANIFEST);
                return result;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        result.add(line);
                    }
                }
            }
        } catch (IOException e) {
            logger.accept("讀取音效清單失敗：" + e.getMessage());
        }
        return result;
    }
}
