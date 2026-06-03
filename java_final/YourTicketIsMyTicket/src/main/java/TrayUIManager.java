
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

    private TextArea logArea;
    private Stage monitorLogStage;

    public TrayUIManager(Stage primaryStage, Runnable showMainAction, Runnable stopMonitorAction, Runnable exitAction) {
        this.primaryStage = primaryStage;
        this.showMainAction = showMainAction;
        this.stopMonitorAction = stopMonitorAction;
        this.exitAction = exitAction;
        
        initFloatingLogWindow();
        setupSystemTray();
    }

    public void clearLog() {
        if (logArea != null) {
            logArea.clear();
        }
    }

    public void appendLog(String message) {
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(message + "\n");
                logArea.setScrollTop(Double.MAX_VALUE);
            }
        });
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

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(60); 
        logArea.setPrefWidth(300);
        logArea.setPromptText("目前無監控活動...");
        logArea.setStyle("-fx-font-family: 'Consolas', 'Microsoft JhengHei'; -fx-font-size: 13px; -fx-control-inner-background: #ffffff; -fx-background-color: transparent;");
        logArea.setWrapText(true);

        VBox layout = new VBox(logArea);
        layout.setPadding(new Insets(5));
        layout.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8; -fx-border-color: #9e9e9e; -fx-border-radius: 8; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 0);");

        final double[] xOffset = {0};
        final double[] yOffset = {0};

        logArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        logArea.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            monitorLogStage.setX(event.getScreenX() - xOffset[0]);
            monitorLogStage.setY(event.getScreenY() - yOffset[0]);
            event.consume(); 
        });

        layout.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
        });
        layout.setOnMouseDragged(event -> {
            monitorLogStage.setX(event.getScreenX() - xOffset[0]);
            monitorLogStage.setY(event.getScreenY() - yOffset[0]);
        });

        Scene scene = new Scene(layout);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        monitorLogStage.setScene(scene);
    }

    private void toggleFloatingLogWindow() {
        if (monitorLogStage.isShowing()) {
            monitorLogStage.hide();
        } else {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            monitorLogStage.setX(bounds.getMaxX() - 315);
            monitorLogStage.setY(bounds.getMinY());
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
