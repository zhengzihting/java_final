package app.view;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

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
            if (!statusLabel.getText().equals("目前無監控活動...")) {
                detailsLabel.setText(statusLabel.getText());
            } else {
                detailsLabel.setText("系統正在背景穩定執行中...");
            }
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

        // 右側雙行文字區域
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

        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");

        if (!isMac) {
            // Windows：Swing 選單由 Java 自行繪製，setFont() 有效，可正確顯示中文。
            // AWT PopupMenu 在 Windows 上是 Win32 原生選單，Java setFont() 無效，中文必定顯示為方塊。
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        SystemTray tray = SystemTray.getSystemTray();
        try {
            InputStream is = getClass().getResourceAsStream("/notify_icon.png");
            if (is == null) return;
            java.awt.Image image = ImageIO.read(is);
            TrayIcon trayIcon = new TrayIcon(image, "Ticket Monitor");
            trayIcon.setImageAutoSize(true);

            if (!isMac) {
                // ── Windows 分支：使用 JPopupMenu（Swing，支援 setFont 中文）──
                java.awt.Font chineseFont = resolveCjkFont(12);

                // 建立隱形的 JDialog 作為 JPopupMenu 的依附視窗
                JDialog hiddenDialog = new JDialog();
                hiddenDialog.setUndecorated(true);
                hiddenDialog.setSize(0, 0);
                hiddenDialog.setAlwaysOnTop(true);

                JPopupMenu popup = new JPopupMenu();

                JMenuItem logItem = new JMenuItem("顯示監控日誌");
                logItem.setFont(chineseFont);
                logItem.addActionListener(e -> Platform.runLater(() -> {
                    if (!monitorLogStage.isShowing()) toggleFloatingLogWindow();
                    else monitorLogStage.toFront();
                }));

                JMenuItem mainItem = new JMenuItem("顯示主視窗");
                mainItem.setFont(chineseFont);
                mainItem.addActionListener(e -> Platform.runLater(() -> showMainAction.run()));

                JMenuItem stopItem = new JMenuItem("停止監控");
                stopItem.setFont(chineseFont);
                stopItem.addActionListener(e -> Platform.runLater(() -> stopMonitorAction.run()));

                JMenuItem exitItem = new JMenuItem("結束程式");
                exitItem.setFont(chineseFont);
                exitItem.addActionListener(e -> Platform.runLater(() -> exitAction.run()));

                popup.add(logItem);
                popup.add(mainItem);
                popup.add(stopItem);
                popup.addSeparator();
                popup.add(exitItem);

                // 選單失去焦點時隱藏錨點 Dialog
                popup.addPopupMenuListener(new PopupMenuListener() {
                    @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
                    @Override
                    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                        hiddenDialog.setVisible(false);
                    }
                    @Override
                    public void popupMenuCanceled(PopupMenuEvent e) {
                        hiddenDialog.setVisible(false);
                    }
                });

                trayIcon.addMouseListener(new MouseAdapter() {
                    private void maybeShowPopup(MouseEvent e) {
                        // Windows 的 popup trigger 在 mouseReleased；同時接受 BUTTON3
                        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                            SwingUtilities.invokeLater(() -> {
                                java.awt.Point mouseLoc = java.awt.MouseInfo.getPointerInfo().getLocation();
                                hiddenDialog.setLocation(mouseLoc.x, mouseLoc.y);
                                hiddenDialog.setVisible(true);
                                // JPopupMenu 會自動處理超出螢幕邊界
                                int yOffset = -popup.getPreferredSize().height;
                                popup.show(hiddenDialog, 0, yOffset);
                            });
                        }
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        maybeShowPopup(e);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        maybeShowPopup(e);
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // 左鍵點擊顯示浮動日誌視窗
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            Platform.runLater(() -> toggleFloatingLogWindow());
                        }
                    }
                });

            } else {
                // ── macOS 分支：使用 AWT PopupMenu（macOS 原生選單原生支援中文）──
                // 必須使用隱形 Frame 避開左鍵事件被原生選單吃掉的問題
                java.awt.PopupMenu popup = new java.awt.PopupMenu();

                java.awt.MenuItem logItem = new java.awt.MenuItem("顯示監控日誌");
                logItem.addActionListener(e -> Platform.runLater(() -> {
                    if (!monitorLogStage.isShowing()) toggleFloatingLogWindow();
                    else monitorLogStage.toFront();
                }));

                java.awt.MenuItem mainItem = new java.awt.MenuItem("顯示主視窗");
                mainItem.addActionListener(e -> Platform.runLater(() -> showMainAction.run()));

                java.awt.MenuItem stopItem = new java.awt.MenuItem("停止監控");
                stopItem.addActionListener(e -> Platform.runLater(() -> stopMonitorAction.run()));

                java.awt.MenuItem exitItem = new java.awt.MenuItem("結束程式");
                exitItem.addActionListener(e -> Platform.runLater(() -> exitAction.run()));

                popup.add(logItem);
                popup.add(mainItem);
                popup.add(stopItem);
                popup.addSeparator();
                popup.add(exitItem);

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

    /**
     * 解析可用的 CJK 字型（僅 Windows 分支使用）。
     * JPopupMenu / JMenuItem 的 setFont() 由 Swing 自行繪製，可正確套用，
     * 依序嘗試：SimHei、Microsoft YaHei、Dialog（JVM 通用 fallback）。
     */
    private java.awt.Font resolveCjkFont(int size) {
        String[] candidates = {
            "Microsoft JhengHei", // Windows 微軟正黑（繁體）
            "PingFang TC",        // macOS 蘋方（備援）
            "Noto Sans CJK TC",   // Linux
            "Dialog"              // JVM 通用 fallback
        };
        java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> available = new java.util.HashSet<>(
            java.util.Arrays.asList(ge.getAvailableFontFamilyNames())
        );
        for (String name : candidates) {
            if (available.contains(name)) {
                return new java.awt.Font(name, java.awt.Font.PLAIN, size);
            }
        }
        return new java.awt.Font("Dialog", java.awt.Font.PLAIN, size);
    }
}
