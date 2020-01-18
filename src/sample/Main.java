package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/resources/sample.fxml"));
        primaryStage.setTitle("Log parser");
        primaryStage.setMinHeight(500);
        primaryStage.setMinWidth(540);
        Scene scene = new Scene(root, 1000, 600);
        scene.getStylesheets().add("/resources/text-area.css");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
