
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import javax.imageio.ImageIO;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;

public class TrayUIManager {

    private final Stage primaryStage;
    private final Runnable showMainAction;
    private final Runnable stopMonitorAction;
    private final Runnable exitAction;
    private Stage monitorLogStage;
    private Circle statusLight;
    private Label statusLabel;
    private Label detailsLabel;
    private FadeTransition breathAnimation;

    public TrayUIManager(Stage primaryStage, Runnable showMainAction, Runnable stopMonitorAction, Runnable exitAction) {
        this.primaryStage = primaryStage;
        this.showMainAction = showMainAction;
        this.stopMonitorAction = stopMonitorAction;
        this.exitAction = exitAction;
        
        initFloatingLogWindow();
        setupSystemTray();
    }

    public void clearLog() {
        Platform.runLater(() -> {
            statusLabel.setText("目前無監控活動...");
            detailsLabel.setText("請至主視窗設定條件並開始");
            statusLight.setFill(Color.web("#95a5a6")); // 灰色
            if (breathAnimation != null) breathAnimation.stop();
            statusLight.setOpacity(1.0);
        });
    }

    public void appendLog(String message) {
        Platform.runLater(() -> {
            if (statusLabel == null || detailsLabel == null) return;

            // 1. 把舊的訊息推到下方詳細欄，新訊息放上方主標題
            detailsLabel.setText(statusLabel.getText());
            statusLabel.setText(message);

            // 2. 智慧燈號判斷
            if (message.contains("啟動") || message.contains("開始")) {
                statusLight.setFill(Color.web("#2ecc71")); // 亮綠色
                startBreathAnimation();
            } else if (message.contains("成功") || message.contains("釋票")) {
                statusLight.setFill(Color.web("#e74c3c")); // 警戒紅（搶到票了！）
                if (breathAnimation != null) breathAnimation.stop();
                statusLight.setOpacity(1.0); // 恆亮不呼吸，提醒肉眼注意
            } else if (message.contains("冷卻") || message.contains("等待")) {
                statusLight.setFill(Color.web("#f1c40f")); // 警告黃
                startBreathAnimation();
            } else if (message.contains("停止") || message.contains("手動")) {
                statusLight.setFill(Color.web("#95a5a6")); // 停機灰
                if (breathAnimation != null) breathAnimation.stop();
                statusLight.setOpacity(1.0);
            }
        });
    }

    private void startBreathAnimation() {
        if (breathAnimation == null) {
            breathAnimation = new FadeTransition(Duration.seconds(1.2), statusLight);
            breathAnimation.setFromValue(1.0);
            breathAnimation.setToValue(0.3);
            breathAnimation.setCycleCount(Timeline.INDEFINITE);
            breathAnimation.setAutoReverse(true);
        }
        breathAnimation.play();
    }
    
    public void showFloatingLogIfNotShowing() {
        if (monitorLogStage != null && !monitorLogStage.isShowing()) {
            toggleFloatingLogWindow();
        }
    }

    private void initFloatingLogWindow() {
        monitorLogStage = new Stage();
        monitorLogStage.initStyle(StageStyle.TRANSPARENT);
        monitorLogStage.setAlwaysOnTop(true);

        statusLight = new Circle(6);
        statusLight.setFill(Color.web("#95a5a6"));

        // 2. 右側雙行文字區域
        statusLabel = new Label("目前無監控活動...");
        statusLabel.setStyle("-fx-font-family: 'Helvetica', 'Microsoft JhengHei'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        statusLabel.setWrapText(true);
        statusLabel.setPrefWidth(330);

        detailsLabel = new Label("請至主視窗設定條件並開始");
        detailsLabel.setStyle("-fx-font-family: 'Helvetica', 'Microsoft JhengHei'; -fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        detailsLabel.setWrapText(true);
        detailsLabel.setPrefWidth(330);

        VBox textContainer = new VBox(4, statusLabel, detailsLabel);
        textContainer.setAlignment(Pos.CENTER_LEFT);
        
        HBox layout = new HBox(14, statusLight, textContainer);
        layout.setAlignment(Pos.CENTER_LEFT);
        layout.setPadding(new Insets(12, 18, 12, 18)); // 上下減薄、左右加寬的黃金比例
        layout.setPrefWidth(380);
        layout.setPrefHeight(60);

        layout.setStyle(
            "-fx-background-color: #ffffff;" + 
            "-fx-background-radius: 20;" + 
            "-fx-border-color: rgba(0,0,0,0.06);" + 
            "-fx-border-radius: 20;" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 4);" // 極為溫和的環境陰影
        );

        Scene scene = new Scene(layout);
        scene.setFill(Color.TRANSPARENT);
        monitorLogStage.setScene(scene);

        // 滑鼠拖曳邏輯（直接綁定在 HBox 卡片上）
        final double[] xOffset = {0};
        final double[] yOffset = {0};
        layout.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        layout.setOnMouseDragged(event -> {
            monitorLogStage.setX(event.getScreenX() - xOffset[0]);
            monitorLogStage.setY(event.getScreenY() - yOffset[0]);
        });
    }

    private void toggleFloatingLogWindow() {
        if (monitorLogStage.isShowing()) {
            monitorLogStage.hide();
        } else {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            monitorLogStage.setX(bounds.getMaxX() - 400);
            monitorLogStage.setY(bounds.getMinY() + 50); 
            monitorLogStage.show();
            monitorLogStage.toFront();
            monitorLogStage.requestFocus();
        }
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;

        SystemTray tray = SystemTray.getSystemTray();
        try {
            InputStream is = getClass().getResourceAsStream("/notify_icon.png");
            if (is == null) return;
            java.awt.Image image = ImageIO.read(is);
            TrayIcon trayIcon = new TrayIcon(image, "Ticket Monitor");
            trayIcon.setImageAutoSize(true);

            java.awt.PopupMenu popup = new java.awt.PopupMenu();
            
            java.awt.MenuItem logItem = new java.awt.MenuItem("顯示監控日誌");
            logItem.addActionListener(e -> Platform.runLater(() -> {
                if (!monitorLogStage.isShowing()) toggleFloatingLogWindow();
                else monitorLogStage.toFront();
            }));
            
            java.awt.MenuItem mainItem = new java.awt.MenuItem("顯示主視窗");
            mainItem.addActionListener(e -> Platform.runLater(() -> {
                showMainAction.run();
            }));

            java.awt.MenuItem stopItem = new java.awt.MenuItem("停止監控");
            stopItem.addActionListener(e -> Platform.runLater(() -> {
                stopMonitorAction.run();
            }));

            java.awt.MenuItem exitItem = new java.awt.MenuItem("結束程式");
            exitItem.addActionListener(e -> Platform.runLater(() -> {
                exitAction.run();
            }));

            popup.add(logItem);
            popup.add(mainItem);
            popup.add(stopItem);
            popup.addSeparator();
            popup.add(exitItem);

            boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");

            if (!isMac) {
                // Windows 或其他系統：原生支援「左鍵點擊事件」與「右鍵原生選單」並存
                trayIcon.setPopupMenu(popup);
                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            Platform.runLater(() -> toggleFloatingLogWindow());
                        }
                    }
                });
            } else {
                // Mac：必須使用隱形 Frame 避開左鍵事件被原生選單吃掉的問題
                java.awt.Frame hiddenFrame = new java.awt.Frame();
                hiddenFrame.setUndecorated(true);
                hiddenFrame.setSize(0, 0);
                hiddenFrame.add(popup);

                trayIcon.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // 判斷是否為右鍵，或是左鍵+任意修飾鍵
                        boolean isRightClick = (e.getButton() == MouseEvent.BUTTON3) || e.isPopupTrigger();
                        boolean hasModifier = e.isMetaDown() || e.isControlDown() || e.isShiftDown() || e.isAltDown();

                        if (isRightClick || hasModifier) {
                            java.awt.Point mouseLoc = java.awt.MouseInfo.getPointerInfo().getLocation();
                            hiddenFrame.setLocation(mouseLoc.x, mouseLoc.y);
                            hiddenFrame.setVisible(true);
                            popup.show(hiddenFrame, 0, 0);
                        } else if (e.getButton() == MouseEvent.BUTTON1) {
                            Platform.runLater(() -> toggleFloatingLogWindow());
                        }
                    }
                });
            }

            tray.add(trayIcon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
