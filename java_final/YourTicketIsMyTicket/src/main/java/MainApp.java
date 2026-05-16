import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class MainApp extends Application {

    private TicketMonitor  monitor;
    private TextArea logArea;
    
    // 依據草稿設計的輸入欄位
    private TextField urlInput;
    private TextField dateInput;
    private TextField areaInput;
    private TextField priceInput;
    
    private Button startBtn;
    private Button stopBtn;

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

        // --- 3. 按鈕區域 (HBox 橫向排列) ---
        startBtn = new Button("開始監控");
        stopBtn = new Button("停止");
        Button historyBtn = new Button("歷史紀錄");
        Button clearBtn = new Button("清除");

        // 設定按鈕樣式與寬度
        String btnStyle = "-fx-font-size: 14px; -fx-padding: 8 15;";
        startBtn.setStyle(btnStyle + "-fx-background-color: #d4edda; -fx-border-color: #28a745;");
        stopBtn.setStyle(btnStyle + "-fx-background-color: #f8d7da; -fx-border-color: #dc3545;");
        stopBtn.setDisable(true);

        HBox buttonBar = new HBox(15, startBtn, stopBtn, historyBtn, clearBtn);
        buttonBar.setAlignment(Pos.CENTER);

        // --- 4. 日誌顯示區 ---
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(250);
        logArea.setPromptText("等待啟動中...");
        logArea.setStyle("-fx-font-family: 'Consolas', 'Microsoft JhengHei'; -fx-font-size: 13px;");

        // --- 5. 佈局設定 ---
        VBox mainLayout = new VBox(20, titleLabel, inputGrid, buttonBar, logArea);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setAlignment(Pos.TOP_CENTER);
        mainLayout.setStyle("-fx-background-color: #ffffff;");

        // --- 6. 事件處理 ---
        startBtn.setOnAction(e -> handleStart());
        stopBtn.setOnAction(e -> handleStop());
        historyBtn.setOnAction(e -> appendLog("\n" + DatabaseManager.getRecentHistory()));
        clearBtn.setOnAction(e -> logArea.clear());

        // 視窗設定
        Scene scene = new Scene(mainLayout, 600, 680);
        primaryStage.setTitle("Ticket Monitor 監控系統 v1.0");
        primaryStage.setScene(scene);
        
        primaryStage.setOnCloseRequest(e -> {
            if (monitor != null) monitor.stopMonitoring();
            Platform.exit();
            System.exit(0);
        });
        
        primaryStage.show();
    }

    /**
     * 輔助方法：快速添加標籤與輸入框到 GridPane
     */
    private void addGridRow(GridPane grid, String labelText, TextField textField, int row) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px;");
        grid.add(label, 0, row);
        grid.add(textField, 1, row);
    }

    private void handleStart() {
        String url = urlInput.getText().trim();
        String keyword = String.format("%s %s %s", dateInput.getText(), areaInput.getText(), priceInput.getText()).trim();

        if (url.isEmpty() || !url.startsWith("http")) {
            appendLog("錯誤：請輸入有效的售票網址！");
            return;
        }

        setUIRunning(true);
        appendLog("正在啟動監控... 目標條件：[" + keyword + "]");

        monitor = new TicketMonitor(url, keyword, message -> {
            appendLog(message);
            DatabaseManager.saveLog(message);
            
            // 情況一：如果訊息包含成功或釋票，跳出置頂對話框，關閉後恢復介面
            if (message.contains("成功") || message.contains("釋票")) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("YourTicketIsMyTicket 系統通知");
                    alert.setHeaderText("偵測到座位釋出！");
                    alert.setContentText("系統已發現符合條件的門票，請立刻前往網站購票。\n詳細資訊：" + message);
                    
                    ((Stage)alert.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true);
                    alert.showAndWait();
                    
                    // 點擊關閉對話框後，恢復按鈕可用狀態
                    setUIRunning(false);
                });
            } 
            // 情況二：如果是手動停止或其他停止訊號，直接恢復介面，不跳出對話框
            else if (message.contains("停止")) {
                Platform.runLater(() -> setUIRunning(false));
            }
        });

        monitor.startMonitoring(10);
    }

    private void handleStop() {
        if (monitor != null) {
            monitor.stopMonitoring();
            appendLog("使用者手動停止監控。");
            setUIRunning(false);
        }
    }

    private void setUIRunning(boolean isRunning) {
        startBtn.setDisable(isRunning);
        stopBtn.setDisable(!isRunning);
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