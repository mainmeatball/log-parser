package sample;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    StyleClassedTextArea textArea = new StyleClassedTextArea();
    VirtualizedScrollPane<StyleClassedTextArea> vsPane = new VirtualizedScrollPane<>(textArea);

    private Pattern pattern;
    private String patternText;
    private String extension = "log";
    private String path = "/";

    //16x16 png Images for treeView icons
    Image folderImage = new Image(
            getClass().getResourceAsStream("/resources/closedFolder.png"));
    Image fileImage = new Image(
            getClass().getResourceAsStream("/resources/file.png"));

    final private ExecutorService executor = Executors.newSingleThreadExecutor(threadExecutor -> {
        Thread thread = new Thread(threadExecutor);
        thread.setDaemon(true);
        return thread;
    });

    @FXML
    public void initialize() {
        folderField.setText("/Users/Meatball/Desktop/test");
        textField.setText("hello");
        textArea.setEditable(false);
        textArea.setWrapText(true);
        GridPane.setVgrow(vsPane, Priority.ALWAYS);
        GridPane.setHgrow(vsPane, Priority.ALWAYS);
        textArea.setPadding(new Insets(8));
        textArea.getStyleClass().add("text-area");
        gridPane.add(vsPane, 1, 3);

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (textArea.getText().isEmpty()) return;
            try {
                pattern = Pattern.compile(newValue);
                applyHighlighting(computeHighlightingAsync(textArea.getText()));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    protected void showFileAndHighlightMatches() {
        if (treeView.getSelectionModel().getSelectedItem() != null) {
            TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
            if (item.isLeaf()) {
                try {
                    RandomAccessFile file = new RandomAccessFile(path + pathFor(item), "r");
                    // adding text from file to textArea
                    patternText = textField.getText();
                    patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
                    pattern = Pattern.compile(patternText);
                    addFileToTextArea(file, textArea);
                    applyHighlighting(computeHighlightingAsync(textArea.getText()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addFileToTextArea(RandomAccessFile file, StyleClassedTextArea textArea) throws IOException {
        FileChannel fc = file.getChannel();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        StringBuilder sb = new StringBuilder();
        while (fc.read(buf) != -1) {
            buf.flip();
            sb.append(Charset.defaultCharset().decode(buf));
            buf.clear();
        }
        textArea.appendText(sb.toString());
        fc.close();
    }

    LinkedList<Pair<Integer,Integer>> matches = new LinkedList<>();

    private void applyHighlighting(HighlighterResult highlighterResult) {
        textArea.setStyleSpans(0, highlighterResult.getStyleSpans());
        textArea.selectRange(highlighterResult.getSelectionBorders().getStart(), highlighterResult.getSelectionBorders().getEnd());
        textArea.requestFollowCaret();
        if (highlighterResult.getCount() == 0) {
            textArea.moveTo(0);
        }
    }

    //find and highlight all matches
    private HighlighterResult computeHighlightingAsync(String text) throws InterruptedException, ExecutionException {
        Highlighter highlighter = new Highlighter(text, pattern, matches);
        Future<HighlighterResult> task = executor.submit(highlighter);
        return task.get();
    }

    @FXML
    protected void nextMatch() {
        highlightMatchesFor(textArea.getText());
        Pair<Integer, Integer> selectionBounds = getSelectionBounds(matches, Direction.NEXT, textArea.getCaretPosition());
        if (selectionBounds.isEqual()) {
            selectionBounds = getSelectionBounds(matches, Direction.NEXT, -1);
        }
        textArea.selectRange(selectionBounds.getStart(), selectionBounds.getEnd());
        textArea.requestFollowCaret();
    }

    @FXML
    protected void previousMatch() {
        highlightMatchesFor(textArea.getText());
        Pair<Integer, Integer> selectionBounds = getSelectionBounds(matches, Direction.PREV, textArea.getCaretPosition());
        if (selectionBounds.isEqual()) {
            selectionBounds = getSelectionBounds(matches, Direction.PREV, -1);
        }
        textArea.selectRange(selectionBounds.getStart(), selectionBounds.getEnd());
        textArea.requestFollowCaret();
    }

    private void highlightMatchesFor(String entry) {
        Window owner = textField.getScene().getWindow();
        if (matches.isEmpty()) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите файл!");
            return;
        }
        // add backslashes to all occurrences of special characters in search word to make them regular characters
        patternText = textField.getText().replaceAll("([^0-9a-zA-Z])", "\\\\$1");
        if (!pattern.toString().equals(patternText)) {
            pattern = Pattern.compile(patternText);
            try {
                applyHighlighting(computeHighlightingAsync(entry));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private Pair<Integer, Integer> getSelectionBounds(LinkedList<Pair<Integer, Integer>> matches, Direction direction, int caretPos) {
        Iterator<Pair<Integer,Integer>> iterator = direction.equals(Direction.NEXT) ?  matches.iterator() : matches.descendingIterator();
        BiPredicate<Pair<Integer, Integer>, Integer> predicate = direction.equals(Direction.NEXT)
                                                                 ? (range, position) -> range.getEnd() > position
                                                                 : (range, position) -> range.getEnd() < position;
        while (iterator.hasNext()) {
            Pair<Integer, Integer> matchRange = iterator.next();
            if (predicate.test(matchRange, caretPos)) return matchRange;
        }
        return new Pair<>(0, 0);
    }

    @FXML
    protected void selectAll() {
        Window owner = textField.getScene().getWindow();
        if (textArea.getLength() == 0) {
            Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите файл!"));
            return;
        }
        textArea.selectRange(0, textArea.getLength());
    }

    @FXML
    protected void submit() throws ExecutionException, InterruptedException {
        Window owner = textField.getScene().getWindow();
        String text = textField.getText();
        String userPath = folderField.getText();
        String userExtension = extensionField.getText();
        treeView.setRoot(null);
        textArea.deleteText(0, textArea.getLength());
        if (userPath != null && !userPath.trim().isEmpty()) {
            path = userPath;
        } else {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                            "Выберите папку!");
            return;
        }
        if (text.isEmpty()) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите искомое слово!");
            return;
        }
        if (!userExtension.isEmpty()) {
            extension = userExtension;
        }
        pattern = Pattern.compile(text);
        DirectorySearcher dirSearcher = new DirectorySearcher(path, extension, text);
        Future<TreeItem<String>> task = executor.submit(dirSearcher);
        TreeItem<String> root = task.get();
        treeView.setRoot(root);
        initializeTreeIcons(root);
    }

    @FXML
    protected void openFileBrowser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(folderField.getScene().getWindow());
        if (selectedDirectory != null) {
            folderField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    public String pathFor(TreeItem<String> item) {
        StringBuilder fullPath = new StringBuilder();
        while (item.getParent() != null) {
            fullPath.insert(0, "/" + item.getValue());
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