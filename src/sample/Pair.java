package sample;

public class Pair<L, R> {

    private L start;
    private R end;

    public Pair(L start, R end) {
        this.start = start;
        this.end = end;
    }

    public L getStart() {
        return start;
    }

    public R getEnd() {
        return end;
    }

    public boolean isEqual() {
        return start == end;
    }
}
