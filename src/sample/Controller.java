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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    final private ExecutorService executor = Executors.newSingleThreadExecutor();

    private enum Direction {
        PREV,
        NEXT
    }

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
                applyHighlighting(computeHighlightingAsync());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });

//        //set event when selecting file with double click
//        treeView.setOnMouseClicked(mouseEvent -> {
//            if(mouseEvent.getClickCount() == 2) {
//                if (treeView.getSelectionModel().getSelectedItem() != null) {
//                    TreeItem<String> item = treeView.getSelectionModel().getSelectedItem();
//                    if (item.isLeaf()) {
//                        try {
//                            RandomAccessFile file = new RandomAccessFile(path + pathFor(item), "r");
//
//                            // adding text from file to textArea
//                            patternText = textField.getText();
//                            patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
//                            pattern = Pattern.compile(patternText);
//                            addFileToTextArea(file, textArea);
//                            applyHighlighting(computeHighlightingAsync());
//
////                            String s = new String(Files.readAllBytes(Paths.get(path + getFullPath(item))));
////                            textArea.replaceText(s);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }
//        });
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
                    applyHighlighting(computeHighlightingAsync());
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

    LinkedList<Integer> startMatches = new LinkedList<>();
    LinkedList<Integer> endMatches = new LinkedList<>();

    private void applyHighlighting(HighlighterResult highlighterResult) {
        textArea.setStyleSpans(0, highlighterResult.getStyleSpans());
        textArea.selectRange(highlighterResult.getSelectionBorders().getStart(), highlighterResult.getSelectionBorders().getEnd());
        textArea.requestFollowCaret();
        if (highlighterResult.getCount() == 0) {
            textArea.moveTo(0);
        }
    }

    //find and highlight all matches
    public HighlighterResult computeHighlightingAsync() throws InterruptedException, ExecutionException {
        Highlighter highlighter = new Highlighter(textArea.getText(), pattern, startMatches, endMatches);
        Future<HighlighterResult> task = executor.submit(highlighter);
        while(!task.isDone()) {
            Thread.sleep(1);
        }
        return task.get();
    }

    @FXML
    protected void nextMatch() {
        highlightMatchesIn(textArea.getText());
        if (!foundIn(endMatches, Direction.NEXT, textArea.getCaretPosition())) {
            foundIn(endMatches, Direction.NEXT, 0);
        }
    }

    @FXML
    protected void previousMatch() {
        highlightMatchesIn(textArea.getText());
        if (!foundIn(endMatches, Direction.PREV, textArea.getCaretPosition())) {
            foundIn(endMatches, Direction.PREV, textArea.getLength());
        }
    }

    private void highlightMatchesIn(String s) {
        Window owner = textField.getScene().getWindow();
        if (startMatches.isEmpty()) {
            Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите файл!"));
            return;
        }
        patternText = textField.getText();
        patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
        if (!pattern.toString().equals(patternText)) {
            pattern = Pattern.compile(patternText);
            try {
                applyHighlighting(computeHighlightingAsync());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean foundIn(LinkedList<Integer> collection, Direction direction, int from) {
        boolean found = false;
        Iterator<Integer> iterator = direction.equals(Direction.NEXT) ?  collection.iterator() : collection.descendingIterator();
        while (iterator.hasNext()) {
            int value = iterator.next();
            if (direction.equals(Direction.NEXT)) {
                if (value >= from && textArea.getSelection().getEnd() != value + textField.getLength()) {
                    textArea.selectRange(value, value + textField.getLength());
                    textArea.requestFollowCaret();
                    found = true;
                    break;
                }
            } else {
                if (value <= from && textArea.getSelection().getStart() != value - textField.getLength()) {
                    textArea.selectRange(value, value - textField.getLength());
                    textArea.requestFollowCaret();
                    found = true;
                    break;
                }
            }
        }
        return found;
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
    protected void submit() {
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
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws IOException {
                try (Stream<Path> walk = Files.walk(Paths.get(path))) {
                    String finalPath = path;

                    // find all files which end with extension and put them into list
                    List<String> result = walk.map(Path::toString)
                            .filter(f -> f.endsWith("." + extension))
                            .map(s -> s.substring(finalPath.length() + 1))
                            .collect(Collectors.toList());

                    if (result.isEmpty()) {
                        Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                                "Файлов с данным расширением в выбранной директории не найдено!"));
                        return null;
                    }

                    // add tree root folder
                    String[] rootFolder = path.split("/");
                    TreeItem<String> root = new TreeItem<>(rootFolder[rootFolder.length - 1]);
                    root.setExpanded(true);

                    boolean noChildren = true;

                    // loop through files list
                    for (String p : result) {
                        // read a file into byte array (does not work with big files)
                        //byte[] fileContent = Files.readAllBytes(Paths.get(path + "/" + p));

                        // read a file using buffer (works with big files)
                        FileReader file = new FileReader(path + "/" + p);
                        BufferedReader reader = new BufferedReader(file);
                        String line;
                        while((line = reader.readLine()) != null) {
                            if((line.contains(text))) {
                                String[] s = p.split("/");
                                TreeItem<String> tempRoot = root;
                                for (String f : s) {
                                    // check if there is a file with the same name in the folder
                                    TreeItem<String> findNode = findItemIn(tempRoot, f);
                                    if (findNode != null) {
                                        tempRoot = findNode;
                                    } else {
                                        TreeItem<String> node = new TreeItem<>(f);
                                        tempRoot.getChildren().add(node);
                                        tempRoot.setExpanded(true);
                                        tempRoot = node;
                                    }
                                }
                                System.out.println("found pattern in " + path + "/" + p);
                                noChildren = false;
                                break;
                            }
                        }


                        //


//                        if (KMPMatch.indexOf(fileContent, text.getBytes()) != -1) {
//
//                            // split path by slashes and add as children
//                            String[] s = p.split("/");
//                            TreeItem<String> tempRoot = root;
//                            for (String f : s) {
//                                // check if there is a file with the same name in the folder
//                                TreeItem<String> findNode = getTreeViewItem(tempRoot, f);
//                                if (findNode != null) {
//                                    tempRoot = findNode;
//                                } else {
//                                    TreeItem<String> node = new TreeItem<>(f);
//                                    tempRoot.getChildren().add(node);
//                                    tempRoot.setExpanded(true);
//                                    tempRoot = node;
//                                }
//                            }
//                            noChildren = false;
//                        }
                    }
                    if (noChildren) {
                        Platform.runLater(() -> AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                                "Искомого текста не найдено в файлах указанной директории."));
                        return null;
                    }
                    initializeTreeIcons(root);
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
    protected void openFileBrowser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(folderField.getScene().getWindow());
        if (selectedDirectory != null) {
            folderField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    public TreeItem<String> findItemIn(TreeItem<String> container, String predicate) {
        if (container != null && container.getValue().equals(predicate))
            return container;
        if (container == null) return null;
        for (TreeItem<String> child : container.getChildren()){
            TreeItem<String> s = findItemIn(child, predicate);
            if (s != null)
                return s;
        }
        return null;
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