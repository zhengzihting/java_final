package app.view;

import app.service.SoundPlayer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainView {

    private final VBox root;

    private final TextField urlInput;
    private final TextField areaInput;
    private final TextField priceInput;

    private final Button startBtn;
    private final Button stopBtn;
    private final Button muteAllBtn;
    private final Button testNotifyBtn;
    private final Button previewSoundBtn;

    private final SoundToggleSwitch soundToggle;
    private final Label soundPathLabel;

    public MainView() {
        // --- 標題區域 ---
        Label titleLabel = new Label("YourTicketIsMyTicket");
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // --- 輸入欄位區域 ---
        GridPane inputGrid = new GridPane();
        inputGrid.setHgap(15);
        inputGrid.setVgap(12);
        inputGrid.setAlignment(Pos.CENTER);

        urlInput = new TextField();
        urlInput.setPromptText("https://...");
        urlInput.setPrefWidth(350);
        addGridRow(inputGrid, "售票網址：", urlInput, 0);

        areaInput = new TextField();
        areaInput.setPromptText("例如：特A區");
        addGridRow(inputGrid, "查詢區域：", areaInput, 1);

        priceInput = new TextField();
        priceInput.setPromptText("例如：4800");
        addGridRow(inputGrid, "查詢票價：", priceInput, 2);

        // --- 音效選擇列 ---
        soundPathLabel = new Label(SoundPlayer.defaultDisplayLabel());
        soundPathLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");
        soundPathLabel.setMaxWidth(350);
        soundPathLabel.setWrapText(false);

        previewSoundBtn = new Button("▶ 試聽");
        previewSoundBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;" +
                "-fx-background-color: #e8eaf6; -fx-border-color: #5c6bc0;");

        muteAllBtn = new Button("停止音效");
        muteAllBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 10;" +
                "-fx-background-color: #f5f5f5; -fx-border-color: #9e9e9e;");

        soundToggle = new SoundToggleSwitch();

        HBox soundControlRow = new HBox(10, soundToggle, previewSoundBtn, muteAllBtn);
        soundControlRow.setAlignment(Pos.CENTER_LEFT);
        addGridRow(inputGrid, "通知音效：", soundControlRow, 4);
        inputGrid.add(soundPathLabel, 1, 5);

        // --- 按鈕區域 ---
        startBtn = new Button("開啟瀏覽器");
        stopBtn = new Button("停止");
        testNotifyBtn = new Button("測試通知");

        String btnStyle = "-fx-font-size: 14px; -fx-padding: 8 15;";
        startBtn.setStyle(btnStyle + "-fx-background-color: #d4edda; -fx-border-color: #28a745;");
        stopBtn.setStyle(btnStyle + "-fx-background-color: #f8d7da; -fx-border-color: #dc3545;");
        testNotifyBtn.setStyle(btnStyle + "-fx-background-color: #fff3cd; -fx-border-color: #ffc107;");
        stopBtn.setDisable(true);

        HBox buttonBar = new HBox(15, startBtn, stopBtn, testNotifyBtn);
        buttonBar.setAlignment(Pos.CENTER);

        // --- 佈局設定 ---
        root = new VBox(20, titleLabel, inputGrid, buttonBar);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #ffffff;");
    }

    private void addGridRow(GridPane grid, String labelText, Node control, int row) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 14px;");
        grid.add(label, 0, row);
        grid.add(control, 1, row);
    }

    public void addHistorySection(VBox historySection) {
        root.getChildren().add(historySection);
    }

    public VBox getRoot() { return root; }

    public TextField getUrlInput() { return urlInput; }
    public TextField getAreaInput() { return areaInput; }
    public TextField getPriceInput() { return priceInput; }

    public Button getStartBtn() { return startBtn; }
    public Button getStopBtn() { return stopBtn; }
    public Button getMuteAllBtn() { return muteAllBtn; }
    public Button getTestNotifyBtn() { return testNotifyBtn; }
    public Button getPreviewSoundBtn() { return previewSoundBtn; }

    public SoundToggleSwitch getSoundToggle() { return soundToggle; }
    public Label getSoundPathLabel() { return soundPathLabel; }
}
