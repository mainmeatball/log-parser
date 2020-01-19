package sample;

import javafx.concurrent.Task;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Highlighter extends Task<HighlighterResult> {
    String text;
    Pattern pattern;
    int length;

    public Highlighter(String text, Pattern pattern, int length) {
        this.text = text;
        this.pattern = pattern;
        this.length = length;
    }

    @Override
    public HighlighterResult call() { return computeHighlighting(text); }

    private HighlighterResult computeHighlighting(String text) {
        Matcher matcher = pattern.matcher(text);
        LinkedList<Pair<Integer, Integer>> matches = new LinkedList<>();
        int count = 0;
        int lastKwEnd = 0;
        int start = 0;
        int end = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        while (matcher.find()) {
            int ms = matcher.start();
            int me = matcher.end();
            matches.add(new Pair<>(ms, me));
            spansBuilder.add(Collections.singleton("white"), ms - lastKwEnd);
            spansBuilder.add(Collections.singleton("lightblue"), me - ms);
            lastKwEnd = me;
            count++;
            if (count == 1) {
                start = ms;
                end = ms + length;
            }
        }
        spansBuilder.add(Collections.singleton("white"), text.length() - lastKwEnd);
        return new HighlighterResult(spansBuilder.create(), matches, new Pair<>(start, end), count);
    }
}
