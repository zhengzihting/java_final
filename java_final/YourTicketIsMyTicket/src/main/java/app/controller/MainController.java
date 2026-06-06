package app.controller;

import app.model.MonitorEvent;
import app.service.DatabaseManager;
import app.service.SoundManager;
import app.service.SystemNotificationService;
import app.service.TicketMonitor;
import app.util.NotificationTestHelper;
import app.view.HistoryTableManager;
import app.view.MainView;
import app.view.TrayUIManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class MainController {

    public enum UiState { IDLE, WAITING_LOGIN, RUNNING }

    private UiState uiState = UiState.IDLE;
    private final MainView view;
    private final Stage primaryStage;

    private TrayUIManager trayUIManager;
    private TicketMonitor monitor;
    private SystemNotificationService notificationService;
    private HistoryTableManager historyTableManager;
    private SoundManager soundManager;
    private TaskUIBridge taskUIBridge;
    
    private PauseTransition notifyBtnPause;

    public MainController(MainView view, Stage primaryStage) {
        this.view = view;
        this.primaryStage = primaryStage;
    }

    public void initialize() {
        // 1. 初始化資料庫
        DatabaseManager.initializeDatabase();

        trayUIManager = new TrayUIManager(
                primaryStage,
                () -> {
                    primaryStage.show();
                    primaryStage.toFront();
                },
                this::handleStop,
                this::exitApplication
        );

        // 2. 初始化協作物件
        notificationService = new SystemNotificationService(this::appendLog);
        soundManager = new SoundManager(view.getSoundPathLabel(), notificationService, this::appendLog);
        taskUIBridge = new TaskUIBridge(view.getUrlInput(), view.getAreaInput(), view.getPriceInput(), this::appendLog);

        // 3. 歷史紀錄表格區
        historyTableManager = new HistoryTableManager(taskUIBridge::parseAndRestoreRecord);
        view.addHistorySection(historyTableManager.getView());

        // 4. 還原上次任務（含音效）
        taskUIBridge.restoreLatestTask(savedPath -> {
            boolean isCustom = soundManager.restoreSoundPath(savedPath);
            view.getSoundToggle().setCustom(isCustom);
        });

        // 5. 事件處理
        setupEventHandlers();
        setUIState(UiState.IDLE);
    }

    private void setupEventHandlers() {
        // 滑塊點擊：由 Controller 決定是否真的切換
        view.getSoundToggle().setOnToggleClicked(() -> {
            if (!view.getSoundToggle().isCustomSelected()) {
                boolean ok = soundManager.handleCustomSelection();
                if (ok) view.getSoundToggle().setCustom(true);
            } else {
                soundManager.selectDefault();
                view.getSoundToggle().setCustom(false);
            }
        });

        view.getSoundToggle().setOnRightClicked(() -> {
            boolean ok = soundManager.chooseSpecificCustomSound(primaryStage);
            if (ok) view.getSoundToggle().setCustom(true);
        });

        view.getPreviewSoundBtn().setOnAction(e -> soundManager.previewSound());
        view.getMuteAllBtn().setOnAction(e -> soundManager.stopAllSounds());
        view.getNotifyToggleBtn().setOnAction(e -> handleNotifyToggle());

        // UI 狀態綁定 startBtn / stopBtn 在 setUIState 處理
    }

    public void exitApplication() {
        if (soundManager != null) soundManager.stopAllSounds();
        if (monitor != null) monitor.stopMonitoring();
        if (taskUIBridge != null) taskUIBridge.saveCurrentTask("STOPPED", soundManager.resolveSelectedSoundPath());
        Platform.exit();
        System.exit(0);
    }

    private void handleStart() {
        String url = view.getUrlInput().getText().trim();
        String keyword = String.format("%s %s",
                view.getAreaInput().getText(), view.getPriceInput().getText()).trim();

        if (url.isEmpty() || !url.startsWith("http")) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "錯誤：請輸入有效的售票網址！", ButtonType.OK);
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        trayUIManager.clearLog();
        trayUIManager.showFloatingLogIfNotShowing();

        setUIState(UiState.WAITING_LOGIN);
        appendLog("正在開啟瀏覽器，請先完成登入...");

        String inputRecord = String.format("網址：%s　區域：%s　票價：%s",
                url,
                view.getAreaInput().getText().trim(),
                view.getPriceInput().getText().trim());
        DatabaseManager.saveLog(inputRecord);
        historyTableManager.refreshHistoryList();

        monitor = new TicketMonitor(url, keyword, (MonitorEvent event) -> {
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

        monitor.prepareMonitoring(() -> {
            // 已就緒
        });
    }

    private void handleLoginConfirmed() {
        if (monitor == null || uiState != UiState.WAITING_LOGIN) return;

        setUIState(UiState.RUNNING);
        appendLog("登入已確認，正式開始監控...");
        taskUIBridge.saveCurrentTask("RUNNING", soundManager.resolveSelectedSoundPath());
        monitor.beginMonitoring(10);
    }

    private void handleStop() {
        if (monitor != null) {
            monitor.stopMonitoring();
            monitor = null;
        }
        appendLog("已停止目前的監控任務。");
        taskUIBridge.saveCurrentTask("STOPPED", soundManager.resolveSelectedSoundPath());
        setUIState(UiState.IDLE);
        historyTableManager.refreshHistoryList();
    }

    private void handleNotifyToggle() {
        boolean isEnabled = !notificationService.isPopupEnabled();
        notificationService.setPopupEnabled(isEnabled);
        if (isEnabled) {
            view.getNotifyToggleBtn().setText("通知已開啟");
            handleTestNotification();
        } else {
            view.getNotifyToggleBtn().setText("通知已關閉");
        }
        
        if (notifyBtnPause != null) {
            notifyBtnPause.stop();
        }
        notifyBtnPause = new PauseTransition(Duration.seconds(2));
        notifyBtnPause.setOnFinished(e -> view.getNotifyToggleBtn().setText("彈出式通知"));
        notifyBtnPause.play();
    }

    private void handleTestNotification() {
        NotificationTestHelper.NotificationPayload payload = NotificationTestHelper.build(
            view.getUrlInput().getText().trim(),
            view.getAreaInput().getText().trim(),
            view.getPriceInput().getText().trim()
        );

        appendLog("▶ 發送模擬通知測試...");

        Thread notifyThread = new Thread(
            () -> notificationService.notifyTicketAvailable(payload.url, payload.details),
            "test-notify-thread"
        );
        notifyThread.setDaemon(true);
        notifyThread.start();
    }

    private void setUIState(UiState state) {
        uiState = state;
        switch (state) {
            case IDLE:
                view.getStartBtn().setText("開啟瀏覽器");
                view.getStartBtn().setOnAction(e -> handleStart());
                view.getStartBtn().setDisable(false);
                view.getStopBtn().setOnAction(e -> handleStop());
                view.getStopBtn().setDisable(true);
                view.getUrlInput().setDisable(false);
                view.getAreaInput().setDisable(false);
                view.getPriceInput().setDisable(false);
                break;

            case WAITING_LOGIN:
                view.getStartBtn().setText("開始監控");
                view.getStartBtn().setOnAction(e -> handleLoginConfirmed());
                view.getStartBtn().setDisable(false);
                view.getStopBtn().setOnAction(e -> handleStop());
                view.getStopBtn().setDisable(false);
                view.getUrlInput().setDisable(false);
                view.getAreaInput().setDisable(false);
                view.getPriceInput().setDisable(false);
                break;

            case RUNNING:
                view.getStartBtn().setText("監控中");
                view.getStartBtn().setOnAction(e -> handleStart());
                view.getStartBtn().setDisable(true);
                view.getStopBtn().setOnAction(e -> handleStop());
                view.getStopBtn().setDisable(false);
                view.getUrlInput().setDisable(true);
                view.getAreaInput().setDisable(true);
                view.getPriceInput().setDisable(true);
                break;
        }
    }

    private void appendLog(String message) {
        if (trayUIManager != null) {
            trayUIManager.appendLog(message);
        }
    }
}
