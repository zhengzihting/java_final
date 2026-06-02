import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainApp extends Application {

    private TicketMonitor monitor;
    private TextArea logArea;
    private SystemNotificationService notificationService;

    // 依據草稿設計的輸入欄位
    private TextField urlInput;
    private TextField dateInput;
    private TextField areaInput;
    private TextField priceInput;

    private Button startBtn;
    private Button stopBtn;
    private Button muteAllBtn;

    // 音效選擇元件
    private SoundToggleSwitch soundToggle;
    private Label soundPathLabel;

    // 拆分出去的協作物件
    private SoundManager soundManager;
    private TaskUIBridge taskUIBridge;

    @Override
    public void start(Stage primaryStage) {
        // 1. 初始化資料庫
        DatabaseManager.initializeDatabase();

        // --- 標題區域 ---
        Label titleLabel = new Label("YourTicketIsMyTicket");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // --- 2. 輸入欄位區域 (GridPane 垂直排列) ---
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(15);
        inputGrid.setVgap(12);
        inputGrid.setAlignment(Pos.CENTER);

        // 售票網址
        urlInput = new TextField();
        urlInput.setPromptText("https://...");
        urlInput.setPrefWidth(350);
        addGridRow(inputGrid, "售票網址：", urlInput, 0);

        // 查詢日期
        dateInput = new TextField();
        dateInput.setPromptText("例如：2026-05-20");
        addGridRow(inputGrid, "查詢日期：", dateInput, 1);

        // 查詢區域
        areaInput = new TextField();
        areaInput.setPromptText("例如：特A區");
        addGridRow(inputGrid, "查詢區域：", areaInput, 2);

        // 查詢票價
        priceInput = new TextField();
        priceInput.setPromptText("例如：4800");
        addGridRow(inputGrid, "查詢票價：", priceInput, 3);

        // --- 3. 音效選擇列（放入 inputGrid 對齊其他欄位）---
        soundPathLabel = new Label(SoundPlayer.defaultDisplayLabel());
        soundPathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        soundPathLabel.setMaxWidth(350);
        soundPathLabel.setWrapText(false);

        Button previewSoundBtn = new Button("▶ 試聽");
        previewSoundBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;" +
                "-fx-background-color: #e8eaf6; -fx-border-color: #5c6bc0;");

        muteAllBtn = new Button("停止音效");
        muteAllBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;" +
                "-fx-background-color: #f5f5f5; -fx-border-color: #9e9e9e;");

        soundToggle = new SoundToggleSwitch();

        // row 4：滑塊 + 試聽 + 靜音（對齊 TextField 欄）
        HBox soundControlRow = new HBox(10, soundToggle, previewSoundBtn, muteAllBtn);
        soundControlRow.setAlignment(Pos.CENTER_LEFT);
        addGridRow(inputGrid, "通知音效：", soundControlRow, 4);

        // row 5：路徑提示（縮排對齊 col 1）
        inputGrid.add(soundPathLabel, 1, 5);

        // --- 4. 按鈕區域 (HBox 橫向排列) ---
        startBtn = new Button("開始監控");
        stopBtn = new Button("停止");
        Button historyBtn = new Button("歷史紀錄");
        Button clearBtn = new Button("清除");
        Button testNotifyBtn = new Button("測試通知");

        // 設定按鈕樣式與寬度
        String btnStyle = "-fx-font-size: 14px; -fx-padding: 8 15;";
        startBtn.setStyle(btnStyle + "-fx-background-color: #d4edda; -fx-border-color: #28a745;");
        stopBtn.setStyle(btnStyle + "-fx-background-color: #f8d7da; -fx-border-color: #dc3545;");
        historyBtn.setStyle(btnStyle + "-fx-background-color: #f5f5f5; -fx-border-color: #9e9e9e;");
        clearBtn.setStyle(btnStyle + "-fx-background-color: #f5f5f5; -fx-border-color: #9e9e9e;");
        testNotifyBtn.setStyle(btnStyle + "-fx-background-color: #fff3cd; -fx-border-color: #ffc107;");
        stopBtn.setDisable(true);

        HBox buttonBar = new HBox(15, startBtn, stopBtn, historyBtn, clearBtn, testNotifyBtn);
        buttonBar.setAlignment(Pos.CENTER);

        // --- 5. 日誌顯示區 ---
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(250);
        logArea.setPromptText("等待啟動中...");
        logArea.setStyle("-fx-font-family: 'Consolas', 'Microsoft JhengHei'; -fx-font-size: 13px;");

        // --- 初始化協作物件 ---
        notificationService = new SystemNotificationService(this::appendLog);
        soundManager  = new SoundManager(soundPathLabel, notificationService, this::appendLog);
        taskUIBridge  = new TaskUIBridge(urlInput, dateInput, areaInput, priceInput, this::appendLog);

        // 還原上次任務（含音效）
        taskUIBridge.restoreLatestTask(savedPath -> {
            boolean isCustom = soundManager.restoreSoundPath(savedPath);
            soundToggle.setCustom(isCustom);
        });

        // --- 6. 佈局設定 ---
        VBox mainLayout = new VBox(20, titleLabel, inputGrid, buttonBar, logArea);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setStyle("-fx-background-color: #ffffff;");

        // --- 7. 事件處理 ---
        // 滑塊點擊：由 MainApp 決定是否真的切換（FileChooser 取消時保持原狀）
        soundToggle.setOnToggleClicked(() -> {
            if (!soundToggle.isCustomSelected()) {
                boolean ok = soundManager.handleCustomSelection();
                if (ok) soundToggle.setCustom(true);
                // 使用者取消 FileChooser → 滑塊保持在「預設」
            } else {
                soundManager.selectDefault();
                soundToggle.setCustom(false);
            }
        });

        soundToggle.setOnRightClicked(() -> {
            boolean ok = soundManager.chooseSpecificCustomSound(primaryStage);
            if (ok) soundToggle.setCustom(true);
        });

        previewSoundBtn.setOnAction(e -> soundManager.previewSound());
        muteAllBtn.setOnAction(e -> soundManager.stopAllSounds());

        startBtn.setOnAction(e -> handleStart());
        stopBtn.setOnAction(e -> handleStop());
        historyBtn.setOnAction(e -> appendLog("\n" + DatabaseManager.getRecentHistory()));
        clearBtn.setOnAction(e -> logArea.clear());
        testNotifyBtn.setOnAction(e -> handleTestNotification());

        // 視窗設定
        Scene scene = new Scene(mainLayout, 620, 730);
        primaryStage.setTitle("Ticket Monitor 監控系統 v2.1");
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            soundManager.stopAllSounds(); // 關閉時先停止所有音效
            if (monitor != null) monitor.stopMonitoring();
            taskUIBridge.saveCurrentTask("STOPPED", soundManager.resolveSelectedSoundPath());
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    // -------------------------------------------------------------------------
    // 業務邏輯處理
    // -------------------------------------------------------------------------

    private void handleStart() {
        String url = urlInput.getText().trim();
        String keyword = String.format("%s %s %s",
                dateInput.getText(), areaInput.getText(), priceInput.getText()).trim();

        if (url.isEmpty() || !url.startsWith("http")) {
            appendLog("錯誤：請輸入有效的售票網址！");
            return;
        }

        setUIRunning(true);
        appendLog("正在啟動監控... 目標條件：[" + keyword + "]");
        taskUIBridge.saveCurrentTask("RUNNING", soundManager.resolveSelectedSoundPath());

        // 存入使用者這次的監控條件
        String inputRecord = String.format("網址：%s　日期：%s　區域：%s　票價：%s",
                url,
                dateInput.getText().trim(),
                areaInput.getText().trim(),
                priceInput.getText().trim());
        DatabaseManager.saveLog(inputRecord);

        monitor = new TicketMonitor(url, keyword, message -> {
            appendLog(message);

            if (message.contains("成功") || message.contains("釋票")) {
                Platform.runLater(() ->
                    taskUIBridge.saveCurrentTask("TICKET_FOUND", soundManager.resolveSelectedSoundPath())
                );
                Thread notifyThread = new Thread(
                    () -> notificationService.notifyTicketAvailable(url, message),
                    "notify-thread"
                );
                notifyThread.setDaemon(true);
                notifyThread.start();
            }
        });

        monitor.startMonitoring(10);
    }

    private void handleStop() {
        if (monitor != null) {
            monitor.stopMonitoring();
            appendLog("使用者手動停止監控。");
            taskUIBridge.saveCurrentTask("STOPPED", soundManager.resolveSelectedSoundPath());
            setUIRunning(false);
        }
    }

    private void handleTestNotification() {
        NotificationTestHelper.NotificationPayload payload = NotificationTestHelper.build(
            urlInput.getText().trim(),
            areaInput.getText().trim(),
            priceInput.getText().trim(),
            dateInput.getText().trim()
        );

        appendLog("▶ 發送模擬通知測試...");

        Thread notifyThread = new Thread(
            () -> notificationService.notifyTicketAvailable(payload.url, payload.details),
            "test-notify-thread"
        );
        notifyThread.setDaemon(true);
        notifyThread.start();
    }

    // -------------------------------------------------------------------------
    // UI 工具方法
    // -------------------------------------------------------------------------

    /**
     * 輔助方法：快速添加標籤與任意控制項到 GridPane
     */
    private void addGridRow(GridPane grid, String labelText, javafx.scene.Node control, int row) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px;");
        grid.add(label, 0, row);
        grid.add(control, 1, row);
    }

    private void setUIRunning(boolean isRunning) {
        startBtn.setDisable(isRunning);
        stopBtn.setDisable(!isRunning);  // 開始監控 → enable；停止監控 → disable
        urlInput.setDisable(isRunning);
        dateInput.setDisable(isRunning);
        areaInput.setDisable(isRunning);
        priceInput.setDisable(isRunning);
    }

    private void appendLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
