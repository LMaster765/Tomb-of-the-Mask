import java.util.*;

public class LevelSet extends ArrayList<Level> {
    private String name;
    
    public LevelSet(String n) {
        name = n;
    }
    
    public String getName() {
        return name;
    }
}