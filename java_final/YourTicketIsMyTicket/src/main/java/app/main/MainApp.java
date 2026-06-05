package app.main;

import app.controller.MainController;
import app.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private MainController controller;

    @Override
    public void start(Stage primaryStage) {
        Platform.setImplicitExit(false);

        // 初始化 View
        MainView view = new MainView();

        // 初始化 Controller 並綁定
        controller = new MainController(view, primaryStage);
        controller.initialize();

        // 視窗設定
        Scene scene = new Scene(view.getRoot(), 620, 680);
        primaryStage.setTitle("Ticket Monitor 監控系統 v5.0");
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest(e -> {
            primaryStage.hide();
            e.consume();
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
