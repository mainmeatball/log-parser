package sample;

import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Highlighter implements Callable<HighlighterResult> {
    String text;
    Pattern pattern;
    Collection<Pair<Integer,Integer>> matches;

    public Highlighter(String text, Pattern pattern, Collection<Pair<Integer,Integer>> matches) {
        this.text = text;
        this.pattern = pattern;
        this.matches = matches;
    }

    @Override
    public HighlighterResult call() {
        return computeHighlighting(text);
    }

    private HighlighterResult computeHighlighting(String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        int lastKwEnd = 0;
        int start = 0;
        int end = 0;
        matches.clear();
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
                end = ms + pattern.toString().length();
            }
        }
        if (count == 0) {
            spansBuilder.add(Collections.singleton("white"), text.length());
            return new HighlighterResult(spansBuilder.create(), new Pair<>(start, end), 0);
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return new HighlighterResult(spansBuilder.create(), new Pair<>(start, end), count);
    }
}
