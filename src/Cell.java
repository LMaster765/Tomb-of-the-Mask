import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

public class Cell {
    public static Color COLOR = Color.BLACK;
    public static String TYPE = "_";
    private static BufferedImage TILESHEET;
    private static BufferedImage VALUESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }

        File valuePath = new File(Game.IMAGES + "dots/tilesheet.png");
        try {
            VALUESHEET = ImageIO.read(valuePath);
        } catch (IOException e) {
            System.out.println("File '" + valuePath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    private BufferedImage tileSheet;
    private boolean kill;
    private boolean solid;
    private boolean needsRedraw = true;
    private boolean needsDotRedraw = true;
    private String type;
    private String imgCode;
    private int x;
    private int y;
    private int value;
    private int valueTick;
    private Color cellColor;
    private String[] neighbors;

    public Cell() {
        this(0, 0, "0");
    }

    public Cell(int xPos, int yPos, String data) {
        this(xPos, yPos, "_", data, false, false, COLOR, new String[] {}, TILESHEET);
    }

    public Cell(int xPos, int yPos, String t, String data, boolean isSolid, boolean isKill, Color c,
            String[] ns, BufferedImage tSheet) {
        x = xPos;
        y = yPos;
        if (data.length() > 0) {
            value = Integer.parseInt(data);
            if (value == 1) {
                if (Math.random() < 0.5)
                    value = 2;
                else
                    value = 1;
            }
        } else {
            value = 0;
        }
        kill = isKill;
        solid = isSolid;
        type = t;
        cellColor = c;
        neighbors = ns;
        tileSheet = tSheet;
    }

    public static Cell newCell(String type, int xPos, int yPos) {
        String data = type.substring(1);
        type = type.substring(0, 1);
        switch (type) {
            case "w":
                return new Wall(xPos, yPos, data);
            case "k":
                return new Kill(xPos, yPos, data);
            case "t":
                return new Trap(xPos, yPos, data);
            case "s":
                return new Spike(xPos, yPos, data);
            case "g":
                return new Goal(xPos, yPos, data);
            case "_":
                return new Cell(xPos, yPos, data);
            case "a":
                return new Shooter(xPos, yPos, data);
            case "f":
                return new PufferBlock(xPos, yPos, data);
            case "j":
                return new Spring(xPos, yPos, data);
            // # placeholder entities for map making
            case "p":
                return new PlayerCell(xPos, yPos, data);
            case "b":
                return new BatCell(xPos, yPos, data);
        }
        return null;
    }

    public static String simplifyCode(String code) {
        int rotation = 0; // number of 90 degree clockwise rotations. Negative = counterclockwise

        // sets any unnecessary corners to 0
        for (int i = 0; i < 4; i++) {
            if (!(code.charAt(0) == '1' && code.charAt(2) == '1'))
                code = code.substring(0, 1) + "0" + code.substring(2, 8);
            code = code.substring(2, 8) + code.substring(0, 2);
        }

        // sets a "11" to the first pair if one is found. If this succeeds, there must
        // be a side in the next pair too.
        for (int i = 0; i < 4; i++) {
            if (!code.substring(0, 2).equals("11")) {
                code = code.substring(2, 8) + code.substring(0, 2);
                rotation--; // rotates counterclockwise
            }
        }
        // if the previous loop found a "11" pair, check if the last pair also has a
        // side. If so, shift to make that last pair first.
        if (code.substring(0, 2).equals("11")) {
            for (int i = 0; i < 4; i++) {
                if (code.substring(6, 8).equals("11")) {
                    code = code.substring(6, 8) + code.substring(0, 6);
                    rotation++; // clockwise rotation
                }
            }
        } else { // if there is no "11", find a "10" to be the first pair
            for (int i = 0; i < 4; i++) {
                if (!code.substring(0, 2).equals("10")) {
                    code = code.substring(2, 8) + code.substring(0, 2);
                    rotation--; // rotates counterclockwise
                }
            }

            if (code.substring(0, 2).equals("10")) {
                // if the previous loop found a "10" pair make sure it's the leftmost pair
                for (int i = 0; i < 4; i++) {
                    if (code.substring(6, 8).equals("10")) {
                        code = code.substring(6, 8) + code.substring(0, 6);
                        rotation++; // clockwise rotation
                    }
                }
            } else {
                // otherwise, find a "00" and then rotate counterclockwise until a "01" is found
                // (or nothing 4 times)
                for (int i = 0; i < 4; i++) {
                    if (!code.substring(0, 2).equals("00")) {
                        code = code.substring(2, 8) + code.substring(0, 2);
                        rotation--; // rotates counterclockwise
                    }
                }

                for (int i = 0; i < 4; i++) {
                    if (!code.substring(0, 2).equals("01")) {
                        code = code.substring(2, 8) + code.substring(0, 2);
                        rotation--; // rotates counterclockwise
                    }
                }
            }
        }
        // after all that, the code should be one of the standard codes and rotation has
        // the needed rotation for the image
        rotation = ((rotation % 4) + 4) % 4; // keeps rotation positive and between 0 and 4 (0 to 270 degree rotation)

        return code + rotation;
    }

    public static BufferedImage transformImage(BufferedImage img, double rotation, double scale) {
        return transformImage(img, rotation, scale, false);
    }

    public static BufferedImage transformImage(BufferedImage img, double rotation, double scale, boolean mirrored) {
        double sin = Math.abs(Math.sin(rotation));
        double cos = Math.abs(Math.cos(rotation));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage output = new BufferedImage((int) (newWidth * scale), (int) (newHeight * scale), img.getType());
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);
        at.rotate(rotation, w / 2, h / 2);

        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        op.filter(img, output);
        return output;
    }

    public void update(Level l) {
        int newTick = valueTick;
        switch (value) {
            case 1:
                newTick = l.getAnimationKey(4, 1);
                break;
            case 2:
                newTick = l.getAnimationKey(1, 4);
                break;
            case 3:
                newTick = l.getAnimationKey(4, 1);
                break;
            case 4:
                newTick = l.getAnimationKey(4, 1);
                break;
        }
        if (valueTick != newTick) {
            valueTick = newTick;
            needsDotRedraw = true;
        }
    }

    public boolean touch(Player p) {
        if (value > 0) {
            p.addValue(value);
            value = 0;
            needsDotRedraw = true;
        }
        if (kill)
            p.setAlive(false);
        return solid;
    }

    public boolean needsRedraw() {
        if (needsRedraw == true) {
            needsRedraw = false;
            return true;
        }
        return false;
    }

    public boolean needsDotRedraw() {
        if (needsDotRedraw == true) {
            needsDotRedraw = false;
            return true;
        }
        return false;
    }

    public void setNeedsRedraw(boolean value) {
        needsRedraw = value;
    }

    public void setNeedsDotRedraw(boolean value) {
        needsDotRedraw = value;
    }

    public Color getColor() {
        return cellColor;
    }

    public String getCode() {
        return imgCode.substring(0, 8);
    }

    public String getImagePath() {
        return Game.IMAGES + type + "/tilesheet.png";
        // return Game.IMAGES + type + "/" + imgCode.substring(0, 8) + "-0.png";
    }

    /**
     * @return a non-transformed BufferedImage of the cell without dots
     */
    public BufferedImage getImage(double scale) {
        if (tileSheet != null) {
            int[] coords = getImageCoords();

            return transformImage(tileSheet.getSubimage(coords[0] * Game.IMAGE_SIZE, coords[1] * Game.IMAGE_SIZE, Game.IMAGE_SIZE,
                    Game.IMAGE_SIZE), 0, scale / Game.IMAGE_SIZE);
        }
        return null;
    }

    /**
     * @return a non-transformed BufferedImage of the cell's dot
     */
    public BufferedImage getDot(double scale) {
        if (VALUESHEET != null && value > 0) {
            return transformImage(VALUESHEET.getSubimage(valueTick * Game.IMAGE_SIZE, (value - 1) * Game.IMAGE_SIZE, Game.IMAGE_SIZE,
                    Game.IMAGE_SIZE), 0, scale / Game.IMAGE_SIZE);
        }
        return null;
    }

    public int[] getImageCoords() {
        return getImageCoords(0);
    }

    public int[] getImageCoords(int y) {
        int x = 0;
        switch (getCode()) {
            case "00000000":
                x = 0;
                break;
            case "10000000":
                x = 1;
                break;
            case "10001000":
                x = 2;
                break;
            case "10100000":
                x = 3;
                break;
            case "10101000":
                x = 4;
                break;
            case "10101010":
                x = 5;
                break;
            case "11100000":
                x = 6;
                break;
            case "11100010":
                x = 7;
                break;
            case "11101000":
                x = 8;
                break;
            case "11101010":
                x = 9;
                break;
            case "11101110":
                x = 10;
                break;
            case "11111000":
                x = 11;
                break;
            case "11111010":
                x = 12;
                break;
            case "11111110":
                x = 13;
                break;
            case "11111111":
                x = 14;
                break;
        }
        return new int[] { x, y };
    }

    public int getRotation() {
        return Integer.parseInt(imgCode.substring(8));
    }

    public int getValue() {
        return value;
    }

    public void setValue(int newValue) {
        value = newValue;
    }

    public int getValueTick() {
        return valueTick;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getType() {
        return type;
    }

    public void setKill(boolean value) {
        kill = value;
    }

    public void setSolid(boolean value) {
        solid = value;
    }

    public void setCode(String code) {
        imgCode = code;
    }

    public boolean isKill() {
        return kill;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isType(String t) {
        return type.equals(t);
    }

    public boolean isIrregular() {
        return false;
    }

    public boolean isNeighbor(String type) {
        for (String n : neighbors) {
            if (n.equals(type))
                return true;
        }
        return false;
    }

    public String toString() {
        if (value > 0) {
            return type + value;
        }
        return type;
    }
}

class Wall extends Cell {
    public static Color COLOR = new Color(202, 0, 255);
    public static String TYPE = "w";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    public Wall(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", true, false, COLOR, new String[] { "w", "t", "k" }, TILESHEET);
    }
}

class Kill extends Cell {
    public static Color COLOR = Color.CYAN;
    public static String TYPE = "k";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    public Kill(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", true, true, COLOR, new String[] { "w", "t", "k" }, TILESHEET);
    }
}

class Trap extends Cell {
    public static Color COLOR = Color.CYAN;
    public static String TYPE = "t";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    private int lastTick;

    public Trap(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", true, false, COLOR, new String[] { "w", "t", "k" }, TILESHEET);
    }

    public void update(Level l) {
        super.update(l);

        int currentTick = (l.getAnimationKey(4, 2) + 1) % 2;
        if (currentTick != lastTick) {
            setNeedsRedraw(true);
            lastTick = currentTick;
        }
    }

    public int[] getImageCoords() {
        return super.getImageCoords(lastTick);
    }
}

class Spike extends Cell {
    public static Color COLOR = Color.CYAN;
    public static String TYPE = "s";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    private int tick = -1;
    private int imgType;

    public Spike(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, data, false, false, COLOR, new String[] { "t" }, TILESHEET);
    }

    public boolean touch(Player p) {
        if (tick == -1)
            tick = 0;
        return super.touch(p);
    }

    public void update(Level l) {
        super.update(l);
        if (tick == -1 && imgType != 0) {
            imgType = 0;
            setNeedsRedraw(true);
        }

        if (tick >= 0) {
            tick++;
            switch (tick) {
                case Game.TPS / 15:
                    imgType = 1;
                    setNeedsRedraw(true);
                    break;
                case Game.TPS / 2:
                    setKill(true);
                    imgType = 3;
                    setNeedsRedraw(true);
                    break;
                case (Game.TPS / 2) + Game.TPS / 15:
                    setKill(true);
                    imgType = 2;
                    setNeedsRedraw(true);
                    break;
                case (int) (Game.TPS * 1.5) - Game.TPS / 15:
                    setKill(false);
                    imgType = 1;
                    setNeedsRedraw(true);
                    break;
                case (int) (Game.TPS * 1.5):
                    tick = -1;
            }
        }
    }

    public Color getColor() {
        if (isKill())
            return super.getColor();
        return Color.BLACK;
    }

    public int[] getImageCoords() {
        return super.getImageCoords(imgType);
    }

    public boolean isIrregular() {
        return isKill();
    }
}

class Goal extends Cell {
    public static Color COLOR = Color.ORANGE;
    public static String TYPE = "g";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    private int animTick;

    public Goal(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", false, false, COLOR, new String[] {}, TILESHEET);
    }

    public void update(Level l) {
        super.update(l);
        int tick = l.getAnimationKey(1, 4);
        if (tick != animTick) {
            animTick = tick;
            setNeedsRedraw(true);
        }
    }

    public boolean touch(Player p) {
        if (p.getX() == (double) getX() && p.getY() == (double) getY())
            p.setWin(true);
        return super.touch(p);
    }

    public int[] getImageCoords() {
        return super.getImageCoords(animTick);
    }
}

class Shooter extends Cell {
    public static Color COLOR = Wall.COLOR;
    public static String TYPE = "a";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    private int tick = 0;
    private int img = 0;
    private String dir;

    public Shooter(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", true, false, COLOR, new String[] {}, TILESHEET);
        dir = data;
    }

    public void update(Level l) {
        super.update(l);
        tick++;
        if (img == 1 && tick % (Game.TPS / 5) == 0) {
            img = 0;
            setNeedsRedraw(true);
        }
        if (tick >= Game.TPS) {
            tick = 0;
            int x = getX();
            int y = getY();
            switch (dir) {
                case "^":
                    y--;
                    break;
                case ">":
                    x++;
                    break;
                case "v":
                    y++;
                    break;
                case "<":
                    x--;
                    break;
            }
            l.spawnArrow(x, y, dir);
            img = 1;
            setNeedsRedraw(true);
        }
    }

    public int[] getImageCoords() {
        return super.getImageCoords(img);
    }

    public int getRotation() {
        switch (dir) {
            case ">":
                return 2;
            case "v":
                return 1;
            case "<":
                return 0;
            case "^":
                return 3;
        }
        return 0;
    }

    public String toString() {
        return super.toString() + dir;
    }
}

class PufferBlock extends Cell {
    public static String TYPE = "f";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    public PufferBlock(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", true, false, Color.GRAY, new String[] {}, TILESHEET);
        setNeedsRedraw(false);
    }
}

class Spring extends Cell {
    public static Color COLOR = Color.ORANGE;
    public static String TYPE = "j";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    String dir;
    int tick = -1;
    int imgType = 0;

    public Spring(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", false, false, COLOR, new String[] {}, TILESHEET);
        dir = data;
    }

    public void update(Level l) {
        if (tick >= 0) {
            tick++;
            switch (tick) {
                case Game.TPS / 10:
                    imgType = 2;
                    setNeedsRedraw(true);
                    break;
                case Game.TPS / 2:
                    imgType = 0;
                    setNeedsRedraw(true);
                    break;
            }
        }
    }

    public boolean touch(Player p) {
        if (getX() == p.getX() && getY() == p.getY()) {
            double x = p.getXDir();
            double y = p.getYDir();

            switch (dir) {
                case ">":
                case "<":
                    p.setDirection(y, x);
                    break;
                case "v":
                case "^":
                    p.setDirection(-y, -x);
                    break;
            }
        }
        tick = 0;
        imgType = 1;
        setNeedsRedraw(true);

        return super.touch(p);
    }

    public String getDir() {
        return dir;
    }

    public String toString() {
        return super.toString() + dir;
    }

    public int getRotation() {
        switch (dir) {
            case ">":
                return 0;
            case "v":
                return -1;
            case "<":
                return -2;
            case "^":
                return -3;
        }
        return 0;
    }

    public int[] getImageCoords() {
        return super.getImageCoords(imgType);
    }
}

class PlayerCell extends Cell {
    public static Color COLOR = Color.YELLOW;
    public static String TYPE = "p";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    public PlayerCell(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", false, false, COLOR, new String[] {}, TILESHEET);
    }
}

class BatCell extends Cell {
    public static Color COLOR = Color.CYAN;
    public static String TYPE = "b";
    private static BufferedImage TILESHEET;

    static {
        File imgPath = new File(Game.IMAGES + TYPE + "/tilesheet.png");
        try {
            TILESHEET = ImageIO.read(imgPath);
        } catch (IOException e) {
            System.out.println("File '" + imgPath.getPath() + "' not found");
            e.printStackTrace();
        }
    }

    private String dir;

    public BatCell(int xPos, int yPos, String data) {
        super(xPos, yPos, TYPE, "0", false, false, Color.CYAN, new String[] {}, TILESHEET);
        dir = data;
        if ("><v^".indexOf(dir) == -1)
            dir = ">";
    }

    public String getDir() {
        return dir;
    }

    public String toString() {
        return super.toString() + dir;
    }
}