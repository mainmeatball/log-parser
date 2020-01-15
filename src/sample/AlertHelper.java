package sample;

import javafx.scene.control.Alert;
import javafx.stage.Window;

import java.util.List;

public class AlertHelper {

    public static void showAlert(Alert.AlertType alertType, Window owner, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(owner);
        alert.show();
    }

    public static void showAlert(Alert.AlertType alertType, Window owner, String title, List<?> message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message.toString());
        alert.initOwner(owner);
        alert.show();
    }
}