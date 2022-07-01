import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

public class Entity {
    private BufferedImage tileSheet;
    private int frame;
    private double xPos;
    private double yPos;
    private double xDir;
    private double yDir;
    private String type;
    private boolean alive = true;
    private int wait = 0;

    public Entity(double x, double y, double xD, double yD, BufferedImage sheet) {
        xPos = x;
        yPos = y;
        xDir = xD;
        yDir = yD;
        tileSheet = sheet;
    }

    public static Entity newEntity(String type, double x, double y) {
        String data = type.substring(1);
        type = type.substring(0, 1);
        Entity e = null;
        switch (type) {
            case Bat.TYPE:
                e = new Bat(x, y, data);
                break;
            case Arrow.TYPE:
                e = new Arrow(x, y, data);
                break;
            case Puffer.TYPE:
                e = new Puffer(x, y, data);
                break;
        }
        if (e == null)
            return null;
        e.setType(type);
        return e;
    }

    public void setDirection(double x, double y) {
        xDir = x;
        yDir = y;
    }

    public void update(Player p) {
        if (wait <= 0) {
            xPos = round(xPos + xDir, 5);
            yPos = round(yPos + yDir, 5);
        } else {
            wait--;
        }
        if ((Math.floor(xPos) == p.getX() || Math.ceil(xPos) == p.getX()) &&
                (Math.floor(yPos) == p.getY() || Math.ceil(yPos) == p.getY())) {
            p.setAlive(false);
        }
    }

    public void updateGraphics(Level l) {
        frame = l.getAnimationKey(2, 4);
    }

    public void updateGraphics(int fr) {
        frame = fr;
    }

    public int[] attemptMove() {
        double newX = xPos + xDir;
        double newY = yPos + yDir;

        if (Math.signum(xDir) > 0)
            newX = Math.ceil(newX);
        if (Math.signum(yDir) > 0)
            newY = Math.ceil(newY);

        return new int[] { (int) newX, (int) newY };
    }

    public void touch() {
        xDir *= -1;
        yDir *= -1;
        wait = Game.TPS;
    }

    public void setAlive(boolean value) {
        alive = value;
    }

    public double getX() {
        return xPos;
    }

    public double getXDir() {
        return xDir;
    }

    public double getY() {
        return yPos;
    }

    public double getYDir() {
        return yDir;
    }

    public void setType(String t) {
        type = t;
    }

    public String getType() {
        return type;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isMoving() {
        return xDir == 0 && yDir == 0;
    }

    public String getImagePath() {
        return Game.IMAGES + type + "/" + 0;
    }

    public String getImagePath(String name) {
        return Game.IMAGES + type + "/" + name;
    }

    public BufferedImage getImage(int scale) {
        return null;
    }

    public String getDirChar() {
        if (xDir > 0)
            return ">";
        if (xDir < 0)
            return "<";
        if (yDir > 0)
            return "v";
        if (yDir < 0)
            return "^";
        return ">";
    }

    public int getFrame() {
        return frame;
    }

    public BufferedImage getTileSheet() {
        return tileSheet;
    }

    public static double round(double value, int decimalPlaces) {
        return (int) (value * Math.pow(10, decimalPlaces) + 0.5) / (double) Math.pow(10, decimalPlaces);
    }
}

class Bat extends Entity {
    public static final String TYPE = "b";
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

    public Bat(double x, double y, String data) {
        super(x, y, 0, 0, TILESHEET);
        double speed = 7.5 / (double) Game.TPS;
        switch (data) {
            case "^":
                setDirection(0, -speed);
                break;
            case ">":
                setDirection(speed, 0);
                break;
            case "v":
                setDirection(0, speed);
                break;
            case "<":
                setDirection(-speed, 0);
                break;
        }
    }

    public String getImagePath() {
        switch (getDirChar()) {
            case "^":
            case ">":
                return getImagePath("0-" + getFrame() + ".png");
            case "v":
            case "<":
                return getImagePath("1-" + getFrame() + ".png");
        }
        return getImagePath("0-0.png");
    }
    public BufferedImage getImage(int scale) {
        BufferedImage sheet = getTileSheet();
        BufferedImage img;
        switch (getDirChar()) {
            case "^":
            case ">":
            default:
                img = sheet.getSubimage(getFrame() * Game.IMAGE_SIZE, 0, Game.IMAGE_SIZE, Game.IMAGE_SIZE);
                break;
            case "v":
            case "<":
                img = sheet.getSubimage(getFrame() * Game.IMAGE_SIZE, Game.IMAGE_SIZE, Game.IMAGE_SIZE,
                        Game.IMAGE_SIZE);
                break;
        }
        return Cell.transformImage(img, 0, scale / Game.IMAGE_SIZE);
    }

    public void updateGraphics(Level l) {
        updateGraphics(l.getAnimationKey(2, 5));
    }
}

class Arrow extends Entity {
    public static final String TYPE = "l";
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

    public Arrow(double x, double y, String data) {
        super(x, y, 0, 0, TILESHEET);
        double speed = 15 / (double) Game.TPS;
        switch (data) {
            case "^":
                setDirection(0, -speed);
                break;
            case ">":
                setDirection(speed, 0);
                break;
            case "v":
                setDirection(0, speed);
                break;
            case "<":
                setDirection(-speed, 0);
                break;
        }
    }

    public void touch() {
        setAlive(false);
    }

    public String getImagePath() {
        switch (getDirChar()) {
            case ">":
                return getImagePath("0.png");
            case "v":
                return getImagePath("1.png");
            case "<":
                return getImagePath("2.png");
            case "^":
                return getImagePath("3.png");
        }
        return getImagePath("0.png");
    }

    public BufferedImage getImage(int scale) {
        int x = 0;
        switch (getDirChar()) {
            case ">":
                x = 0;
                break;
            case "v":
                x = 1;
                break;
            case "<":
                x = 2;
                break;
            case "^":
                x = 3;
                break;
        }
        return Cell.transformImage(getTileSheet().getSubimage(x * Game.IMAGE_SIZE, 0, Game.IMAGE_SIZE, Game.IMAGE_SIZE), 0, scale / Game.IMAGE_SIZE);
    }
}

class Puffer extends Entity {
    public static final String TYPE = "f";
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
    private int[] frame = new int[] { 0, 0 };

    public Puffer(double x, double y, String data) {
        super(x, y, 0, 0, TILESHEET);
    }

    public void update(Player p) {
        if (tick >= Game.TPS * 1.5) {
            if (getX() - 1 <= p.getX() && getX() + 1 >= p.getX() && getY() - 1 <= p.getY() && getY() + 1 >= p.getY()) {
                p.setAlive(false);
            }
        }
        tick = (tick + 1) % (int) (Game.TPS * 2.25);
    }

    public String getImagePath() {
        return getImagePath(frame[0] + "-" + frame[1] + ".png");
    }

    public BufferedImage getImage(int scale) {
        BufferedImage sheet = getTileSheet();
        int size = 3 * Game.IMAGE_SIZE;
        return Cell.transformImage(sheet.getSubimage(frame[1] * size, frame[0] * size, size, size), 0, scale / Game.IMAGE_SIZE);
    }

    public void updateGraphics(Level l) {
        if (tick < Game.TPS / 8 && l.getAnimationTick() > 2) {
            frame[0] = 1;
            frame[1] = 1;
        } else if (tick < 1.5 * Game.TPS / 8 && l.getAnimationTick() > 2) {
            frame[0] = 1;
            frame[1] = 0;
        } else if (tick < 1.5 * Game.TPS - 2 * Game.TPS / 8) {
            frame[0] = 0;
            frame[1] = l.getAnimationKey(1, 4);
        } else if (tick < 1.5 * Game.TPS - Game.TPS / 8) {
            frame[0] = 1;
            frame[1] = 0;
        } else if (tick < 1.5 * Game.TPS) {
            frame[0] = 1;
            frame[1] = 1;
        } else if (tick < 1.5 * Game.TPS + Game.TPS / 8) {
            frame[0] = 1;
            frame[1] = 2;
        } else if (tick < 2.25 * Game.TPS - Game.TPS / 8) {
            frame[0] = 2;
            frame[1] = l.getAnimationKey(1, 2);
        } else {
            frame[0] = 1;
            frame[1] = 2;
        }
    }

    public boolean isBig() {
        return tick >= Game.TPS * 2;
    }
}