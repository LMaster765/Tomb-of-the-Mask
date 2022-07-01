import java.util.*;

public class TimeTracker {
    private static ArrayList<Pair> stuff;
    private static int selected = -1;

    static {
        stuff = new ArrayList<>();
    }

    public static void reset() {
        stuff = new ArrayList<>();
        selected = -1;
    }

    public static void start(String name) {
        // deselect the last tracker
        if (selected >= 0) {
            stuff.get(selected).deselect();
        }

        // select the new tracker
        boolean exists = false;
        for (int i = 0; i < stuff.size(); i++) {
            if (stuff.get(i).is(name)) {
                exists = true;
                stuff.get(i).select();
                selected = i;
            }
        }

        // add the tracker if it doesn't exist
        if (!exists) {
            Pair p = new Pair(name);
            stuff.add(p);
            p.select();
        }
    }

    public static String getValues() {
        int entriesPerRow = 6;

        String rtn = "Time Tracker:";
        for (int i = 0; i < stuff.size(); i++) {
            if (i % entriesPerRow == 0) {
                rtn += "\n" + stuff.get(i).toString();
            } else {
                rtn += " | " + stuff.get(i).toString();
            }
        }
        return rtn;
    }
}

class Pair {
    private String name;
    private long startTime;
    private ArrayList<Integer> times;
    private int sum;

    public Pair(String n) {
        name = n;
        times = new ArrayList<>();
        times.add(0);
    }

    public void select() {
        startTime = System.currentTimeMillis();
    }

    public void deselect() {
        times.add(0, (int) (System.currentTimeMillis() - startTime));
        sum += times.get(0);
        while (times.size() > Game.TPS * 5) {
            sum -= times.remove(times.size() - 1);
        }
    }

    public boolean is(String n) {
        return n.equals(name);
    }

    public String toString() {
        return name + ": " + times.get(0) + "/" + String.format("%.2f", (double) sum / times.size()) + " ms";
    }
}