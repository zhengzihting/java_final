import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public class MainApp extends Application {

    // 監控系統列與懸浮日誌管理器
    private TrayUIManager trayUIManager;

    private Stage primaryStage;
    private TicketMonitor monitor;
    private SystemNotificationService notificationService;

    // 輸入欄位
    private TextField urlInput;
    private TextField areaInput;
    private TextField priceInput;

    private Button startBtn;
    private Button stopBtn;
    private Button muteAllBtn;

    // 音效選擇元件
    private SoundToggleSwitch soundToggle;
    private Label soundPathLabel;

    // 歷史紀錄表格（顯示於主視窗底部）
    private TableView<DatabaseManager.HistoryEntry> historyTableView;

    // 拆分出去的協作物件
    private SoundManager soundManager;
    private TaskUIBridge taskUIBridge;

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);
        this.primaryStage = primaryStage;

        trayUIManager = new TrayUIManager(
                primaryStage,
                () -> {
                    primaryStage.show();
                    primaryStage.toFront();
                },
                this::handleStop,
                this::exitApplication
        );

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

        // 查詢區域
        areaInput = new TextField();
        areaInput.setPromptText("例如：特A區");
        addGridRow(inputGrid, "查詢區域：", areaInput, 1);

        // 查詢票價
        priceInput = new TextField();
        priceInput.setPromptText("例如：4800");
        addGridRow(inputGrid, "查詢票價：", priceInput, 2);

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
        Button testNotifyBtn = new Button("測試通知");

        // 設定按鈕樣式
        String btnStyle = "-fx-font-size: 14px; -fx-padding: 8 15;";
        startBtn.setStyle(btnStyle + "-fx-background-color: #d4edda; -fx-border-color: #28a745;");
        stopBtn.setStyle(btnStyle + "-fx-background-color: #f8d7da; -fx-border-color: #dc3545;");
        testNotifyBtn.setStyle(btnStyle + "-fx-background-color: #fff3cd; -fx-border-color: #ffc107;");
        stopBtn.setDisable(true);

        HBox buttonBar = new HBox(15, startBtn, stopBtn, testNotifyBtn);
        buttonBar.setAlignment(Pos.CENTER);

        // --- 5. 歷史紀錄表格區（取代原 logArea）---
        Label historyLabel = new Label("歷史監控紀錄");
        historyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #555;");

        historyTableView = new TableView<>();
        historyTableView.setPrefHeight(210);
        // 不設定 CONSTRAINED_RESIZE_POLICY 以避免因 ScrollBar 預留空間導致標題列一開始偏移的問題
        historyTableView.setPlaceholder(new Label("目前尚無監控條件紀錄"));

        // 欄位定義

        TableColumn<DatabaseManager.HistoryEntry, String> colArea = new TableColumn<>("區域");
        colArea.setCellValueFactory(d -> new SimpleStringProperty(
                parseField(d.getValue().message, "區域：")));
        colArea.setPrefWidth(70);

        TableColumn<DatabaseManager.HistoryEntry, String> colPrice = new TableColumn<>("票價");
        colPrice.setCellValueFactory(d -> new SimpleStringProperty(
                parseField(d.getValue().message, "票價：")));
        colPrice.setPrefWidth(60);

        TableColumn<DatabaseManager.HistoryEntry, String> colUrl = new TableColumn<>("售票網址");
        colUrl.setCellValueFactory(d -> new SimpleStringProperty(
                parseField(d.getValue().message, "網址：")));
        colUrl.setPrefWidth(190);

        TableColumn<DatabaseManager.HistoryEntry, String> colTime = new TableColumn<>("時間");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().timestamp));
        colTime.setPrefWidth(140);

       colUrl.setPrefWidth(245); 

        // 欄位加入表格
        historyTableView.getColumns().addAll(colArea, colPrice, colTime, colUrl);
        refreshHistoryList();

        // 修正點 2：設定 TableView 的總寬度，剛剛好卡死所有欄位 + 滾動條寬度
        historyTableView.setPrefWidth(550);
        historyTableView.setMaxWidth(550);

        // 修正點 3：乾淨的視覺修正，將沒用的灰色填充塊隱形，讓滑軌無縫貼近
        historyTableView.setStyle(
            "-fx-background-color: transparent; " +
            ".table-view .filler { -fx-background-color: transparent; -fx-border-color: transparent; }"
        );

        // 點選即套用（單擊）
        historyTableView.setOnMouseClicked(e -> {
            DatabaseManager.HistoryEntry selected =
                    historyTableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                parseAndRestoreRecord(selected.message);
            }
        });

        VBox historySection = new VBox(8, historyLabel, historyTableView);

        // --- 初始化協作物件 ---
        notificationService = new SystemNotificationService(this::appendLog);
        soundManager  = new SoundManager(soundPathLabel, notificationService, this::appendLog);
        taskUIBridge  = new TaskUIBridge(urlInput, areaInput, priceInput, this::appendLog);

        // 還原上次任務（含音效）
        taskUIBridge.restoreLatestTask(savedPath -> {
            boolean isCustom = soundManager.restoreSoundPath(savedPath);
            soundToggle.setCustom(isCustom);
        });

        // --- 6. 佈局設定 ---
        VBox mainLayout = new VBox(20, titleLabel, inputGrid, buttonBar, historySection);
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
        testNotifyBtn.setOnAction(e -> handleTestNotification());

        // 視窗設定
        Scene scene = new Scene(mainLayout, 620, 680);
        primaryStage.setTitle("Ticket Monitor 監控系統 v3.0");
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            primaryStage.hide();
            e.consume();
        });

        primaryStage.show();
    }

    private void exitApplication() {
        soundManager.stopAllSounds(); // 關閉時先停止所有音效
        if (monitor != null) monitor.stopMonitoring();
        taskUIBridge.saveCurrentTask("STOPPED", soundManager.resolveSelectedSoundPath());
        Platform.exit();
        System.exit(0);
    }
    // -------------------------------------------------------------------------
    // 業務邏輯處理
    // -------------------------------------------------------------------------

    private void handleStart() {
        String url = urlInput.getText().trim();
        String keyword = String.format("%s %s",
                areaInput.getText(), priceInput.getText()).trim();

        if (url.isEmpty() || !url.startsWith("http")) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "錯誤：請輸入有效的售票網址！", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        setUIRunning(true);

        trayUIManager.clearLog();
        trayUIManager.showFloatingLogIfNotShowing();

        appendLog("正在啟動監控... 目標條件：[" + keyword + "]");
        taskUIBridge.saveCurrentTask("RUNNING", soundManager.resolveSelectedSoundPath());

        // 存入使用者這次的監控條件
        String inputRecord = String.format("網址：%s　區域：%s　票價：%s",
                url,
                areaInput.getText().trim(),
                priceInput.getText().trim());
        DatabaseManager.saveLog(inputRecord);

        // 開始後刷新歷史清單（顯示本次新增的紀錄）
        refreshHistoryList();

        monitor = new TicketMonitor(url, keyword, (MonitorEvent event) -> {
            // 完美咬合：直接讀取公開變數 event.message 與 event.ticketFound 欄位！
            appendLog(event.message);

            if (event.ticketFound) {
                Platform.runLater(() ->
                    taskUIBridge.saveCurrentTask("TICKET_FOUND", soundManager.resolveSelectedSoundPath())
                );
                Thread notifyThread = new Thread(
                    () -> notificationService.notifyTicketAvailable(url, event.message),
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
            // 停止後刷新歷史清單
            refreshHistoryList();
        }
    }

    private void handleTestNotification() {
        NotificationTestHelper.NotificationPayload payload = NotificationTestHelper.build(
            urlInput.getText().trim(),
            areaInput.getText().trim(),
            priceInput.getText().trim()
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

    // -------------------------------------------------------------------------
    // 歷史清單相關
    // -------------------------------------------------------------------------

    /** 從資料庫重新載入監控條件清單至主視窗的 TableView */
    private void refreshHistoryList() {
        List<DatabaseManager.HistoryEntry> entries = DatabaseManager.getMonitoringHistory();
        Platform.runLater(() -> historyTableView.getItems().setAll(entries));
    }

    /**
     * 從 message 字串中擷取指定前綴的欄位值。
     * 格式：網址：xxx　日期：xxx　區域：xxx　票價：xxx
     */
    private String parseField(String message, String prefix) {
        for (String part : message.split("　")) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    /**
     * 解析監控條件字串並直接填回 4 個輸入欄位。
     * 格式：網址：xxx　日期：xxx　區域：xxx　票價：xxx
     */
    private void parseAndRestoreRecord(String message) {
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
            Platform.runLater(() -> {
                urlInput.setText(fUrl);
                areaInput.setText(fArea);
                priceInput.setText(fPrice);
            });
        } catch (Exception ignored) {
            // 格式不符則靜默忽略
        }
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
        areaInput.setDisable(isRunning);
        priceInput.setDisable(isRunning);
    }

    private void appendLog(String message) {
        if (trayUIManager != null) {
            trayUIManager.appendLog(message);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
