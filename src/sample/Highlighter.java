package sample;

import javafx.concurrent.Task;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Highlighter extends Task<HighlighterResult> {
    String text;
    Pattern pattern;

    public Highlighter(String text, Pattern pattern) {
        this.text = text;
        this.pattern = pattern;
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
                end = me;
            }
        }
        spansBuilder.add(Collections.singleton("white"), text.length() - lastKwEnd);
        return new HighlighterResult(spansBuilder.create(), matches, new Pair<>(start, end), count);
    }

    public static Pair<Integer, Integer> getSelectionBounds(LinkedList<Pair<Integer, Integer>> matches, Direction direction, int caretPos) {
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
}
