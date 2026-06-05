package app.controller;

import app.main.MainApp;
import app.service.TaskStorageService;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/**
 * 橋接 UI 輸入欄位與 TaskStorageService 的協調器。
 * 負責從 UI 讀取資料存入 Storage，以及從 Storage 還原資料回 UI。
 */
public class TaskUIBridge {

    private final TaskStorageService taskStorageService = new TaskStorageService();

    private final TextField urlInput;
    private final TextField areaInput;
    private final TextField priceInput;

    private final Consumer<String> logger;

    public TaskUIBridge(TextField urlInput,
                        TextField areaInput,
                        TextField priceInput,
                        Consumer<String> logger) {
        this.urlInput   = urlInput;
        this.areaInput  = areaInput;
        this.priceInput = priceInput;
        this.logger     = logger;
    }

    /**
     * 從 tasks.json 還原上次的任務設定到 UI 欄位。
     *
     * @param onSoundPathRestored 當有已儲存的音效路徑時，將路徑回傳給呼叫方（MainApp）處理
     */
    public void restoreLatestTask(Consumer<String> onSoundPathRestored) {
        try {
            taskStorageService.loadLatestTask().ifPresentOrElse(task -> {
                urlInput.setText(task.url);
                areaInput.setText(task.area);
                priceInput.setText(task.price);
                if (task.soundPath != null && !task.soundPath.isBlank()) {
                    onSoundPathRestored.accept(task.soundPath);
                }
            }, () -> {});
        } catch (Exception e) {
            logger.accept("無法讀取本地任務記憶：" + e.getMessage());
        }
    }

    /**
     * 解析監控條件字串並直接填回輸入欄位。
     * 格式：網址：xxx　日期：xxx　區域：xxx　票價：xxx
     */
    public void parseAndRestoreRecord(String message) {
        try {
            String[] parts = message.split("　");
            String url   = "";
            String area  = "";
            String price = "";

            for (String part : parts) {
                if (part.startsWith("網址："))       url   = part.substring(3).trim();
                else if (part.startsWith("區域：")) area  = part.substring(3).trim();
                else if (part.startsWith("票價：")) price = part.substring(3).trim();
            }

            if (url.isEmpty()) return;

            final String fUrl = url, fArea = area, fPrice = price;
            javafx.application.Platform.runLater(() -> {
                urlInput.setText(fUrl);
                areaInput.setText(fArea);
                priceInput.setText(fPrice);
            });
        } catch (Exception ignored) {
            // 格式不符則靜默忽略
        }
    }

    /**
     * 將目前 UI 欄位的值及狀態寫入 tasks.json。
     *
     * @param status    任務狀態，例如 "RUNNING"、"STOPPED"、"TICKET_FOUND"
     * @param soundPath 目前選擇的音效路徑
     */
    public void saveCurrentTask(String status, String soundPath) {
        String url = urlInput.getText().trim();
        if (url.isEmpty()) {
            return;
        }

        TaskStorageService.StoredTask task = TaskStorageService.StoredTask.fromInputs(
                url,
                areaInput.getText(),
                priceInput.getText(),
                status,
                soundPath
        );

        try {
            taskStorageService.upsertTask(task);

        } catch (Exception e) {
            logger.accept("儲存任務記憶失敗：" + e.getMessage());
        }
    }
}
