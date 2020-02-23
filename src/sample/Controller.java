package sample;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import sample.exceptions.NoSuchDirectoryException;
import sample.exceptions.NoSuchExtensionFileException;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class Controller {
    @FXML
    private GridPane gridPane;

    @FXML
    private TextField textField;

    @FXML
    private TextField folderField;

    @FXML
    private TextField extensionField;

    @FXML
    private TreeView<String> treeView;

    @FXML
    private Button submitButton;

    @FXML
    private TabPane tabPane;

    private Pattern pattern;
    private String extension = "log";

    private final MenuItem menuItem = new MenuItem("Open in new tab");
    private final ContextMenu contextMenu = new ContextMenu(menuItem);

    //16x16 png Images for treeView icons
    private final Image folderImage = new Image(
            getClass().getResourceAsStream("/resources/folder.png"));
    private final Image fileImage = new Image(
            getClass().getResourceAsStream("/resources/file.png"));

    final private ExecutorService executor = Executors.newSingleThreadExecutor(threadExecutor -> {
        Thread thread = new Thread(threadExecutor);
        thread.setDaemon(true);
        return thread;
    });

    @FXML
    public void initialize() {
        submitButton.setDisable(true);
        contextMenu.setAutoHide(true);

        PauseTransition pause = new PauseTransition(Duration.millis(100));
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            pause.setOnFinished(event -> {
                if (textField.getLength() == 0) {
                    submitButton.setDisable(true);
                    return;
                }
                submitButton.setDisable(false);
                pattern = Pattern.compile(Pattern.quote(newValue));
                FileTab tab = ((FileTab)tabPane.getSelectionModel().getSelectedItem());
                if (tab != null) {
                    tab.highlight(pattern);
                }
            });
            pause.playFromStart();
        });

        extensionField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (extensionField.getLength() == 0) {
                extension = "log";
            } else {
                extension = newValue;
            }
        });

        treeView.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown()) {
                contextMenu.show(treeView, event.getScreenX(), event.getScreenY());
            } else {
                if (contextMenu.isShowing()) {
                    contextMenu.hide();
                }
            }
        });

        menuItem.setOnAction(e -> {
            if (treeView.getSelectionModel().getSelectedItem() == null) {
                return;
            }
            TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
            if (!item.isLeaf()) {
                return;
            }
            addFileToNewTab(folderField.getText() + pathFor(item));
        });

        gridPane.setOnMouseClicked(e -> {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });
    }

    @FXML
    protected void showFileAndHighlightMatches(MouseEvent event) {
        if (event.getClickCount() != 2) {
            return;
        }
        if (treeView.getSelectionModel().getSelectedItem() == null) {
            return;
        }
        TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
        if (!item.isLeaf()) {
            return;
        }
        try {
            // Escape special regex characters
            pattern = Pattern.compile(Pattern.quote(textField.getText()));
            FileTab tab = ((FileTab)tabPane.getSelectionModel().getSelectedItem());
            if (tab == null) {
                tab = new FileTab(item.getValue());
                tabPane.getTabs().add(tab);
            }
            tab.setText(item.getValue());
            tab.addFile(folderField.getText() + pathFor(item));
            tab.highlight(pattern);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFileToNewTab(String path) {
        try {
            FileTab tab = new FileTab(path.substring(path.lastIndexOf(File.separator) + 1));
            tab.addFile(path);
            tabPane.getTabs().add(tab);
            tab.highlight(pattern);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    protected void openFileBrowser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(folderField.getScene().getWindow());
        if (selectedDirectory != null) {
            folderField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    protected void submit() {
        if (!extensionField.getText().isEmpty()) {
            extension = extensionField.getText();
        }
        treeView.setRoot(null);
        // Clear or not clear tabs implement here
        pattern = Pattern.compile(Pattern.quote(textField.getText()));
        computeSearchingIn(folderField.getText(), extension, textField.getText());
    }


    // Find and highlight all matches
    private void computeSearchingIn(String path, String extension, String text) {
        DirectorySearcher directorySearcher = new DirectorySearcher(path, extension, text);

        directorySearcher.setOnFailed(event -> {
            if (directorySearcher.getException() instanceof NoSuchExtensionFileException) {
                AlertHelper.showAlert(Alert.AlertType.ERROR, textField.getScene().getWindow(), "Ошибка!",
                        "Файлов с указанным расширением в выбранной директории не найдено.");
            } else if (directorySearcher.getException() instanceof NoSuchDirectoryException) {
                AlertHelper.showAlert(Alert.AlertType.ERROR, textField.getScene().getWindow(), "Ошибка!",
                        "Выбранной директории не существует.");
            } else if (directorySearcher.getException() instanceof NoSuchFileException) {
                AlertHelper.showAlert(Alert.AlertType.ERROR, textField.getScene().getWindow(), "Ошибка!",
                        "Файлов с данным искомым словом в выбранной директории не найдено.");
            } else {
                AlertHelper.showAlert(Alert.AlertType.ERROR, textField.getScene().getWindow(), "Ошибка!",
                        Arrays.toString(directorySearcher.getException().getStackTrace()));
            }
        });

        directorySearcher.setOnSucceeded(event -> {
            TreeItem<String> root = directorySearcher.getValue();
            treeView.setRoot(root);
            initializeTreeIcons(root);
        });

        executor.execute(directorySearcher);
    }

    public String pathFor(TreeItem<String> item) {
        StringBuilder fullPath = new StringBuilder();
        while (item.getParent() != null) {
            fullPath.insert(0,  File.separator + item.getValue());
            item = item.getParent();
        }
        return fullPath.toString();
    }

    public void initializeTreeIcons(TreeItem<String> root) {
        if (root == null) return;
        if (root.isLeaf()) {
            root.setGraphic(new ImageView(fileImage));
        } else {
            root.setGraphic(new ImageView(folderImage));
        }
        for (TreeItem<String> node : root.getChildren()) {
            if (node.isLeaf()) {
                node.setGraphic(new ImageView(fileImage));
            } else {
                node.setGraphic(new ImageView(folderImage));
                initializeTreeIcons(node);
            }
        }
    }

}