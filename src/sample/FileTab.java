package sample;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class FileTab extends Tab {

    private StyleClassedTextArea textArea = new StyleClassedTextArea();

    private LinkedList<Pair<Integer,Integer>> matches = new LinkedList<>();

    final private ExecutorService executor = Executors.newSingleThreadExecutor(threadExecutor -> {
        Thread thread = new Thread(threadExecutor);
        thread.setDaemon(true);
        return thread;
    });

    public FileTab(String name) {
        super(name);

        HBox hbox = new HBox();
        VBox vbox = new VBox();

        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPadding(new Insets(8));
        textArea.getStyleClass().add("text-area");

        VirtualizedScrollPane<StyleClassedTextArea> vsPane = new VirtualizedScrollPane<>(textArea);
        VBox.setVgrow(vsPane, Priority.ALWAYS);

        hbox.setAlignment(Pos.CENTER);

        Button selectAllButton = new Button("Выбрать все");
        selectAllButton.setPrefSize(100, 30);
        selectAllButton.setDefaultButton(true);

        Button prevMatchButton = new Button("Пред.");
        prevMatchButton.setPrefSize(60, 30);
        prevMatchButton.setDefaultButton(true);

        Button nextMatchButton = new Button("След.");
        nextMatchButton.setPrefSize(60, 30);
        nextMatchButton.setDefaultButton(true);

        VBox.setMargin(hbox, new Insets(8, 0,0,0));
        HBox.setMargin(prevMatchButton, new Insets(0, 8, 0, 8));

        vbox.getChildren().add(vsPane);
        hbox.getChildren().addAll(selectAllButton, prevMatchButton, nextMatchButton);
        vbox.getChildren().add(hbox);
        this.setContent(vbox);

        selectAllButton.setOnAction(e ->
                selectAll());
        prevMatchButton.setOnAction(e ->
                getMatch(Direction.PREV));
        nextMatchButton.setOnAction(e ->
                getMatch(Direction.NEXT));
    }

    public void highlight(Pattern pattern) {
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

    // Make this in a new thread
    public void addFile(String path) throws IOException {
        textArea.clear();
        try (FileReader fileReader = new FileReader(path);
             BufferedReader reader = new BufferedReader(fileReader)) {
            char[] buf = new char[4096];
            StringBuilder sb = new StringBuilder();
            while ((reader.read(buf)) != -1) {
                sb.append(buf);
            }
            textArea.appendText(sb.toString());
        }
    }

    protected void selectAll() {
        Window owner = textArea.getScene().getWindow();
        if (textArea.getLength() == 0) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, owner, "Ошибка!",
                    "Выберите файл!");
            return;
        }
        textArea.selectRange(0, textArea.getLength());
    }

    private void getMatch(Direction direction) {
        Pair<Integer, Integer> selectionBounds = Highlighter.getSelectionBounds(matches, direction, textArea.getCaretPosition());
        int caretPosition = direction == Direction.NEXT ? -1 : textArea.getLength() + 1;
        if (selectionBounds.isEqual()) {
            selectionBounds = Highlighter.getSelectionBounds(matches, direction, caretPosition);
        }
        textArea.selectRange(selectionBounds.getStart(), selectionBounds.getEnd());
        textArea.requestFollowCaret();
    }
}
