package sample;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import sample.exceptions.NoSuchDirectoryException;
import sample.exceptions.NoSuchExtensionFileException;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiPredicate;
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
    private Button selectAllButton;

    @FXML
    private Button prevMatchButton;

    @FXML
    private Button nextMatchButton;

    private StyleClassedTextArea textArea = new StyleClassedTextArea();
    private VirtualizedScrollPane<StyleClassedTextArea> vsPane = new VirtualizedScrollPane<>(textArea);

    private Pattern pattern;
    private String extension = "log";

    private LinkedList<Pair<Integer,Integer>> matches = new LinkedList<>();

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
        textArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (textArea.getLength() == 0) {
                selectAllButton.setDisable(true);
                prevMatchButton.setDisable(true);
                nextMatchButton.setDisable(true);
            } else {
                selectAllButton.setDisable(false);
                prevMatchButton.setDisable(false);
                nextMatchButton.setDisable(false);
            }
        });

        textArea.setEditable(false);
        textArea.setWrapText(true);
        selectAllButton.setDisable(true);
        prevMatchButton.setDisable(true);
        nextMatchButton.setDisable(true);
        submitButton.setDisable(true);
        GridPane.setVgrow(vsPane, Priority.ALWAYS);
        GridPane.setHgrow(vsPane, Priority.ALWAYS);
        textArea.setPadding(new Insets(8));
        textArea.getStyleClass().add("text-area");
        gridPane.add(vsPane, 1, 3);

        PauseTransition pause = new PauseTransition(Duration.millis(100));
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            pause.setOnFinished(event -> {
                if (textField.getLength() == 0) {
                    submitButton.setDisable(true);
                    return;
                }
                submitButton.setDisable(false);
                pattern = Pattern.compile(Pattern.quote(newValue));
                highlight(textArea, pattern);
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
            addFileToTextArea(folderField.getText() + pathFor(item), textArea);
            highlight(textArea, pattern);
        } catch (Exception e) {
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
    protected void selectAll() {
        Window owner = textField.getScene().getWindow();
        if (textArea.getLength() == 0) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите файл!");
            return;
        }
        textArea.selectRange(0, textArea.getLength());
    }

    @FXML
    protected void nextMatch() { getMatch(Direction.NEXT); }

    @FXML
    protected void previousMatch() { getMatch(Direction.PREV); }

    private void getMatch(Direction direction) {
        if (matches.isEmpty()) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, textField.getScene().getWindow(), "Ошибка!",
                    "Выберите файл!");
            return;
        }
        Pair<Integer, Integer> selectionBounds = getSelectionBounds(matches, direction, textArea.getCaretPosition());
        int caretPosition = direction == Direction.NEXT ? -1 : textArea.getLength() + 1;
        if (selectionBounds.isEqual()) {
            selectionBounds = getSelectionBounds(matches, direction, caretPosition);
        }
        textArea.selectRange(selectionBounds.getStart(), selectionBounds.getEnd());
        textArea.requestFollowCaret();
    }

    @FXML
    protected void submit() {
        Window owner = textField.getScene().getWindow();
        if (folderField.getText() == null || folderField.getText().trim().isEmpty()) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите папку!");
            return;
        }
        if (textField.getText().isEmpty()) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите искомое слово!");
            return;
        }
        if (!extensionField.getText().isEmpty()) {
            extension = extensionField.getText();
        }
        treeView.setRoot(null);
        textArea.clear();
        pattern = Pattern.compile(Pattern.quote(textField.getText()));
        computeSearchingIn(folderField.getText(), extension, textField.getText());
    }

    private void addFileToTextArea(String path, StyleClassedTextArea textArea) throws IOException {
        textArea.clear();
        FileHandler fileHandler = new FileHandler(path);
        textArea.appendText(fileHandler.getText());
    }

    private void highlight(StyleClassedTextArea textArea, Pattern pattern) {
        Highlighter highlighter = new Highlighter(textArea.getText(), pattern);
        highlighter.setOnSucceeded(event -> {
            HighlighterResult highlighterResult = highlighter.getValue();
            textArea.setStyleSpans(0, highlighterResult.getStyleSpans());
            textArea.selectRange(highlighterResult.getSelectionBounds().getStart(), highlighterResult.getSelectionBounds().getEnd());
            textArea.requestFollowCaret();
            if (highlighterResult.getCount() == 0) {
                textArea.moveTo(0);
            }
            matches = highlighterResult.getMatches();
        });
        executor.execute(highlighter);
    }

    private Pair<Integer, Integer> getSelectionBounds(LinkedList<Pair<Integer, Integer>> matches, Direction direction, int caretPos) {
        Iterator<Pair<Integer,Integer>> iterator = direction.equals(Direction.NEXT) ?  matches.iterator() : matches.descendingIterator();
        BiPredicate<Pair<Integer, Integer>, Integer> predicate = direction.equals(Direction.NEXT)
                                                                 ? (range, position) -> range.getEnd() > position
                                                                 : (range, position) -> range.getEnd() < position;
        while (iterator.hasNext()) {
            Pair<Integer, Integer> matchRange = iterator.next();
            if (predicate.test(matchRange, caretPos)) { return matchRange; }
        }
        return new Pair<>(0, 0);
    }

    //find and highlight all matches
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