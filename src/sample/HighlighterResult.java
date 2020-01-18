package sample;

import org.fxmisc.richtext.model.StyleSpans;

import java.util.Collection;

public class HighlighterResult {
    private StyleSpans<Collection<String>> styleSpans;
    private Pair<Integer, Integer> selectionBorders;
    private int count;

    public HighlighterResult(StyleSpans<Collection<String>> styleSpans, Pair<Integer, Integer> selectionBorders, int count) {
        this.styleSpans = styleSpans;
        this.selectionBorders = selectionBorders;
        this.count = count;
    }

    public StyleSpans<Collection<String>> getStyleSpans() {
        return styleSpans;
    }

    public Pair<Integer, Integer> getSelectionBorders() {
        return selectionBorders;
    }

    public int getCount() {
        return count;
    }
}
