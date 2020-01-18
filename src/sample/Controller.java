package sample;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
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

    private ExecutorService executor;

    @FXML
    public void initialize() {
        folderField.setText("/Users/Meatball/Desktop/test");
        textField.setText("bla");
        textArea.setEditable(false);
        textArea.setWrapText(true);


        GridPane.setVgrow(vsPane, Priority.ALWAYS);
        GridPane.setHgrow(vsPane, Priority.ALWAYS);
        textArea.setPadding(new Insets(8));
        textArea.getStyleClass().add("text-area");
        gridPane.add(vsPane, 1, 3);
        executor = Executors.newSingleThreadExecutor();

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (textArea.getText().isEmpty()) return;
            try {
                pattern = Pattern.compile(newValue);
                applyHighlighting(computeHighlightingAsync().get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

        //set event when selecting file with double click
        treeView.setOnMouseClicked(mouseEvent -> {
            if(mouseEvent.getClickCount() == 2)
            {
                if (treeView.getSelectionModel().getSelectedItem() != null) {
                    TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
                    if (item.isLeaf()) {
                        try {
                            String s = new String(Files.readAllBytes(Paths.get(path + getFullPath(item))));
                            textArea.replaceText(s);
                            patternText = textField.getText();
                            patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
                            pattern = Pattern.compile(patternText);
                            applyHighlighting(computeHighlightingAsync().get());

                        } catch (IOException | InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    List<Integer> startMatches = new ArrayList<>();
    List<Integer> endMatches = new ArrayList<>();

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        textArea.setStyleSpans(0, highlighting);
    }

    //find and highlight all matches
    public Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        String s = textArea.getText();
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() {
                return computeHighlighting(s);
            }
        };
        executor.execute(task);
        return task;
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        int lastKwEnd = 0;
        startMatches.clear();
        endMatches.clear();
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while (matcher.find()) {
            int ms = matcher.start();
            int me = matcher.end();
            startMatches.add(ms);
            endMatches.add(me);
            spansBuilder.add(Collections.singleton("white"), ms - lastKwEnd);
            spansBuilder.add(Collections.singleton("lightblue"), me - ms);

            lastKwEnd = me;
            count++;
            if (count == 1) {
                Platform.runLater(() -> {
                    textArea.selectRange(ms, ms + textField.getText().length());
                    textArea.requestFollowCaret();
                });

            }
        }
        if (count == 0) {
            Platform.runLater(() -> textArea.moveTo(0));
            spansBuilder.add(Collections.singleton("white"), text.length());
            return spansBuilder.create();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    @FXML
    protected void handleNextMatchButtonAction() {
        Window owner = textField.getScene().getWindow();
        if (startMatches.isEmpty()) {
            Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите файл!"));
            return;
        }
        boolean found = false;
        patternText = textField.getText();
        patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
        if (!pattern.toString().equals(patternText)) {
            pattern = Pattern.compile(patternText);
            try {
                applyHighlighting(computeHighlightingAsync().get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return;
        }
        for (Integer i : startMatches) {
            if (i >= textArea.getCaretPosition() && textArea.getSelection().getEnd() != i + textField.getText().length()) {
                found = true;
                textArea.selectRange(i, i + textField.getText().length());
                textArea.requestFollowCaret();
                break;
            }
        }
        if (!found) {
            textArea.moveTo(0);
            handleNextMatchButtonAction();
        }
    }

    @FXML
    protected void handlePrevMatchButtonAction() {
        Window owner = textField.getScene().getWindow();
        if (endMatches.isEmpty()) {
            Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите файл!"));
            return;
        }
        boolean found = false;
        patternText = textField.getText();
        patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
        if (!pattern.toString().equals(patternText)) {
            textArea.setStyleClass(0, textArea.getLength(), "white");
            try {
                applyHighlighting(computeHighlightingAsync().get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return;
        }
        for (int i = endMatches.size() - 1; i >= 0; i--) {
            if (endMatches.get(i) <= textArea.getCaretPosition() && textArea.getSelection().getStart() != endMatches.get(i) - textField.getText().length()) {
                found = true;
                textArea.selectRange(endMatches.get(i), endMatches.get(i) - textField.getText().length());
                textArea.requestFollowCaret();
                break;
            }
        }
        if (!found) {
            textArea.moveTo(textArea.getLength());
            handlePrevMatchButtonAction();
        }
    }

    @FXML
    protected void handleSelectAllButtonAction() {
        Window owner = textField.getScene().getWindow();
        if (textArea.getLength() == 0) {
            Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите файл!"));
            return;
        }
        textArea.selectRange(0, textArea.getLength());
    }

    @FXML
    protected void handleSubmitButtonAction() {
        Window owner = textField.getScene().getWindow();
        String text = textField.getText();
        String userPath = folderField.getText();
        String userExtension = extensionField.getText();
        treeView.setRoot(null);
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() {
                Platform.runLater(() -> textArea.deleteText(0, textArea.getLength()));
                if (userPath != null && !userPath.trim().isEmpty()) {
                    path = userPath;
                } else {
                    Platform.runLater(() ->
                    AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                            "Выберите папку!"));
                    return null;
                }
                if (text.isEmpty()) {
                    Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                            "Выберите искомое слово!"));
                    return null;
                }
                pattern = Pattern.compile(text);
                if (!userExtension.isEmpty()) {
                    extension = userExtension;
                }
                try (Stream<Path> walk = Files.walk(Paths.get(path))) {
                    String finalPath = path;

                    // Find all files which end with extension and put them into list
                    List<String> result = walk.map(Path::toString)
                            .filter(f -> f.endsWith("." + extension))
                            .map(s -> s.substring(finalPath.length() + 1))
                            .collect(Collectors.toList());

                    if (result.isEmpty()) {
                        Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                                "Выбранной папки не существует!"));
                        return null;
                    }

                    // Add tree root folder
                    String[] rootFolder = path.split("/");
                    TreeItem<String> root = new TreeItem<>(rootFolder[rootFolder.length - 1]);
                    root.setExpanded(true);

                    boolean noChildren = true;
                    // Loop through list files
                    for (String p : result) {
                        byte[] fileContent = Files.readAllBytes(Paths.get(path + "/" + p));
                        if (KMPMatch.indexOf(fileContent, text.getBytes()) != -1) {

                            // split path by slashes and add as children
                            String[] s = p.split("/");
                            TreeItem<String> tempRoot = root;
                            for (String f : s) {
                                TreeItem<String> findNode = getTreeViewItem(tempRoot, f);
                                if (findNode != null) {
                                    tempRoot = findNode;
                                } else {
                                    TreeItem<String> node = new TreeItem<>(f);
                                    tempRoot.getChildren().add(node);
                                    tempRoot.setExpanded(true);
                                    tempRoot = node;
                                }
                            }
                            noChildren = false;
                        }
                    }
                    if (noChildren) {
                        Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                                "Искомого текста не найдено в файлах указанной директории."));
                        return null;
                    }
                    setAllGraphics(root);
                    Platform.runLater(() -> treeView.setRoot(root));
                } catch (NoSuchFileException e) {
                    Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                            "Выбранной папки не существует!"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        executor.execute(task);
    }

    @FXML
    protected void chooseFolderButtonAction() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(folderField.getScene().getWindow());
        if (selectedDirectory != null) {
            folderField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    public TreeItem<String> getTreeViewItem(TreeItem<String> item, String value) {
        if (item != null && item.getValue().equals(value))
            return  item;
        if (item == null) return null;
        for (TreeItem<String> child : item.getChildren()){
            TreeItem<String> s = getTreeViewItem(child, value);
            if (s != null)
                return s;
        }
        return null;
    }

    public String getFullPath(TreeItem<String> item) {
        StringBuilder fullPath = new StringBuilder();
        while (item.getParent() != null) {
            fullPath.insert(0, "/" + item.getValue());
            item = item.getParent();
        }
        return fullPath.toString();
    }

    public void setAllGraphics(TreeItem<String> root) {
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
                setAllGraphics(node);
            }
        }
    }

}