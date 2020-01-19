package sample;

import org.fxmisc.richtext.model.StyleSpans;

import java.util.Collection;
import java.util.LinkedList;

public class HighlighterResult {
    private StyleSpans<Collection<String>> styleSpans;
    private LinkedList<Pair<Integer, Integer>> matches;
    private Pair<Integer, Integer> selectionBounds;
    private int count;

    public HighlighterResult(StyleSpans<Collection<String>> styleSpans, LinkedList<Pair<Integer, Integer>> matches, Pair<Integer, Integer> selectionBounds, int count) {
        this.styleSpans = styleSpans;
        this.matches = matches;
        this.selectionBounds = selectionBounds;
        this.count = count;
    }

    public StyleSpans<Collection<String>> getStyleSpans() {
        return styleSpans;
    }

    public LinkedList<Pair<Integer, Integer>> getMatches() {
        return matches;
    }

    public Pair<Integer, Integer> getSelectionBounds() {
        return selectionBounds;
    }

    public int getCount() {
        return count;
    }
}
