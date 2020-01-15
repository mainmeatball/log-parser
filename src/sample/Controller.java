package sample;

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
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

    InlineCssTextArea textArea = new InlineCssTextArea();
    VirtualizedScrollPane<InlineCssTextArea> vsPane = new VirtualizedScrollPane<>(textArea);

    private Pattern pattern;
    private String patternText;
    private String extension = "log";
    private String path = "/";


    //16x16 png Images for treeView icons
    Image closedFolderImage = new Image(
            getClass().getResourceAsStream("/resources/closedFolder.png"));
    Image openedFolderImage = new Image(
            getClass().getResourceAsStream("/resources/openedFolder.png"));
    Image fileImage = new Image(
            getClass().getResourceAsStream("/resources/file.png"));

    @FXML
    public void initialize() {
        folderField.setText("/Users/Meatball/Desktop/test");
        textField.setText("bla");
        textArea.setEditable(false);
        textArea.setWrapText(true);

        GridPane.setVgrow(vsPane, Priority.ALWAYS);
        GridPane.setHgrow(vsPane, Priority.ALWAYS);
        textArea.setPadding(new Insets(6));
        textArea.getStyleClass().add("text-area");
        gridPane.add(vsPane, 1, 2);

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
                            highlightAllMatches(s);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
    }

    List<Integer> startMatches = new ArrayList<>();
    List<Integer> endMatches = new ArrayList<>();

    //find and highlight all matches
    public void highlightAllMatches(String s) {
        startMatches.clear();
        endMatches.clear();
        textArea.setStyle(0, textArea.getLength(), "-rtfx-background-color: white;");
        patternText = textField.getText();
        patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
        pattern = Pattern.compile(patternText);
        Matcher matcher = pattern.matcher(s);
        int count = 0;
        while (matcher.find()) {
            startMatches.add(matcher.start());
            endMatches.add(matcher.end());
            count++;
            if (count == 1) {
                textArea.selectRange(matcher.start(), matcher.start() + textField.getText().length());
                textArea.requestFollowCaret();
            }
            textArea.setStyle(matcher.start(), matcher.end(), "-rtfx-background-color: lightblue;");
        }
    }

    @FXML
    protected void handleNextMatchButtonAction() {
        boolean found = false;
        patternText = textField.getText();
        patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
        if (!pattern.toString().equals(patternText)) {
            textArea.setStyle(0, textArea.getLength(), "-rtfx-background-color: white;");
            highlightAllMatches(textArea.getText());
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
        boolean found = false;
        patternText = textField.getText();
        patternText = patternText.replaceAll("([^0-9a-zA-Z])", "\\\\$1");
        if (!pattern.toString().equals(patternText)) {
            textArea.setStyle(0, textArea.getLength(), "-rtfx-background-color: white;");
            highlightAllMatches(textArea.getText());
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
        textArea.selectRange(0, textArea.getLength());
    }

    @FXML
    protected void handleSubmitButtonAction() {
        Window owner = textField.getScene().getWindow();
        String text = textField.getText();
        String userPath = folderField.getText();
        String userExtension = extensionField.getText();
        treeView.setRoot(null);
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
        pattern = Pattern.compile(text);
        if (!userExtension.isEmpty()) {
            extension = userExtension;
        }
        //Path p = Paths.get(path);

        try (Stream<Path> walk = Files.walk(Paths.get(path))) {
            String finalPath = path;

            // Find all files which end with extension and put them into list
            List<String> result = walk.map(Path::toString)
                    .filter(f -> f.endsWith("." + extension))
                    .map(s -> s.substring(finalPath.length() + 1))
                    .collect(Collectors.toList());

            if (result.isEmpty()) {
                treeView.setRoot(null);
                return;
            }

            // Add tree root folder
            String[] rootFolder = path.split("/");
            TreeItem<String> root = new TreeItem<>(rootFolder[rootFolder.length - 1]);
            root.setExpanded(true);

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
                            node.expandedProperty().addListener(a -> setAllGraphics(node));
                            tempRoot.getChildren().add(node);
                            tempRoot.setExpanded(true);
                            tempRoot = node;
                        }
                    }
                }
            }
            setAllGraphics(root);
            textArea.deleteText(0, textArea.getLength());
            root.expandedProperty().addListener(f -> setAllGraphics(root));
            treeView.setRoot(root);
            if (root.isLeaf()) {
                treeView.setRoot(null);
                AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                        "Искомого текста не найдено в файлах указанной директории.");
            }
        } catch (NoSuchFileException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выбранной папки не существует!");
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            if (root.isExpanded()) {
                root.setGraphic(new ImageView(openedFolderImage));
            } else {
                root.setGraphic(new ImageView(closedFolderImage));
            }
        }
        for (TreeItem<String> node : root.getChildren()) {
            if (node.isLeaf()) {
                node.setGraphic(new ImageView(fileImage));
            } else if (node.isExpanded()) {
                node.setGraphic(new ImageView(openedFolderImage));
                setAllGraphics(node);
            } else {
                node.setGraphic(new ImageView(closedFolderImage));
                setAllGraphics(node);
            }
        }
    }

}