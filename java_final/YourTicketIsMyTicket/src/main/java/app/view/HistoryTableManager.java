package app.view;

import app.service.DatabaseManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * 負責管理與渲染歷史紀錄表格的元件
 */
public class HistoryTableManager {

    private final TableView<DatabaseManager.HistoryEntry> historyTableView;
    private final VBox historySection;
    private final Consumer<String> onRecordSelected;

    public HistoryTableManager(Consumer<String> onRecordSelected) {
        this.onRecordSelected = onRecordSelected;
        this.historyTableView = new TableView<>();
        this.historySection = createHistorySection();
        refreshHistoryList();
    }

    private VBox createHistorySection() {
        Label historyLabel = new Label("歷史監控紀錄");
        historyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #555;");

        Button deleteButton = new Button("刪除歷史紀錄");
        deleteButton.setStyle("-fx-font-size: 12px; -fx-text-fill: #888888; -fx-background-color: transparent; -fx-border-color: #888888; -fx-border-radius: 3px; -fx-padding: 3 8;");
        deleteButton.setOnAction(e -> {
            DatabaseManager.clearHistory();
            refreshHistoryList();
        });

        HBox headerBox = new HBox(10, historyLabel, deleteButton);
        headerBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        historyTableView.setPrefHeight(210);
        historyTableView.setPlaceholder(new Label("目前尚無監控條件紀錄"));

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
        colUrl.setPrefWidth(245); 

        TableColumn<DatabaseManager.HistoryEntry, String> colTime = new TableColumn<>("時間");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().timestamp));
        colTime.setPrefWidth(140);

        historyTableView.getColumns().add(colArea);
        historyTableView.getColumns().add(colPrice);
        historyTableView.getColumns().add(colTime);
        historyTableView.getColumns().add(colUrl);

        historyTableView.setPrefWidth(550);
        historyTableView.setMaxWidth(550);
        historyTableView.setStyle("-fx-background-color: transparent;");

        historyTableView.setOnMouseClicked(e -> {
            DatabaseManager.HistoryEntry selected =
                    historyTableView.getSelectionModel().getSelectedItem();
            if (selected != null && onRecordSelected != null) {
                onRecordSelected.accept(selected.message);
            }
        });

        return new VBox(8, headerBox, historyTableView);
    }

    public VBox getView() {
        return historySection;
    }

    public void refreshHistoryList() {
        List<DatabaseManager.HistoryEntry> entries = DatabaseManager.getMonitoringHistory();
        Platform.runLater(() -> historyTableView.getItems().setAll(entries));
    }

    private String parseField(String message, String prefix) {
        for (String part : message.split("　")) {
            if (part.startsWith(prefix)) {
                return part.substring(prefix.length()).trim();
            }
        }
        return "";
    }
}
