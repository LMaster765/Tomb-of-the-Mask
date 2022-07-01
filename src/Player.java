import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import javax.imageio.*;

public class Player {
    public static Color COLOR = new Color(255, 235, 13);
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

    private int queuedKey;
    private double xPos;
    private double yPos;
    private double xDir = 0;
    private double yDir = 0;
    private double xDirVis = 0;
    private double yDirVis = 0;
    private int animationKey = 0;
    private boolean moving = false;
    private boolean win = false;
    private boolean alive = true;
    private double speed = 30 / (double) Game.TPS;

    private int[] scores = new int[4];

    public Player(int x, int y) {
        xPos = x;
        yPos = y;
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
    
    public void setY(double pos) {
        yPos = pos;
    }

    public double getYDir() {
        return yDir;
    }

    public Color getColor() {
        return COLOR;
    }

    public void queue(int key) {
        queuedKey = key;
    }

    public void setDirection(int key) {
        if (!moving) {
            switch (key) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    xDir = 0;
                    yDir = -speed;
                    xDirVis = xDir;
                    yDirVis = yDir;
                    moving = true;
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    xDir = speed;
                    yDir = 0;
                    xDirVis = xDir;
                    yDirVis = yDir;
                    moving = true;
                    break;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    xDir = 0;
                    yDir = speed;
                    xDirVis = xDir;
                    yDirVis = yDir;
                    moving = true;
                    break;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    xDir = -speed;
                    yDir = 0;
                    xDirVis = xDir;
                    yDirVis = yDir;
                    moving = true;
                    break;
            }
        }
    }

    public void setDirection(double x, double y) {
        xDir = x;
        yDir = y;
        xDirVis = xDir;
        yDirVis = yDir;
    }

    public int[] attemptMove() {
        if (!moving) {
            setDirection(queuedKey);
        }
        double newX = xPos + xDir;
        double newY = yPos + yDir;

        if (Math.signum(xDir) > 0)
            newX = Math.ceil(newX);
        if (Math.signum(yDir) > 0)
            newY = Math.ceil(newY);

        return new int[] { (int) newX, (int) newY };
    }

    public int[] move() {
        xPos = round(xPos + xDir, 5);
        yPos = round(yPos + yDir, 5);

        return new int[] { (int) xPos, (int) yPos };
    }

    public void setMoving(boolean value) {
        moving = value;
        if (!moving) {
            xDir = 0;
            yDir = 0;
        }
    }

    public void setWin(boolean w) {
        win = w;
    }

    public void setAlive(boolean a) {
        alive = a;
    }

    public boolean isWin() {
        return win;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isMoving() {
        return moving;
    }

    public void addValue(int value) {
        if (value > 0)
            scores[value - 1]++;
    }

    public int[] getValues() {
        return scores;
    }

    public int getQueue() {
        return queuedKey;
    }

    public int getDirVis() {
        if (xDirVis > 0) {
            return 3;
        } else if (yDirVis > 0) {
            return 0;
        } else if (xDirVis < 0) {
            return 1;
        } else if (yDirVis < 0) {
            return 2;
        }
        return 0;
    }

    public BufferedImage getImage(int scale) {
        int x = 0;
        int y = 1;
        if (!moving) {
            y = 0;
            x = animationKey;
        }

        return Cell.transformImage(TILESHEET.getSubimage(x * Game.IMAGE_SIZE, y * Game.IMAGE_SIZE, Game.IMAGE_SIZE, Game.IMAGE_SIZE), getDirVis() * Math.PI / 2, scale / Game.IMAGE_SIZE);
    }

    public void setAnimationKey(int value) {
        animationKey = value;
    }

    public static double round(double value, int decimalPlaces) {
        return (int) (value * Math.pow(10, decimalPlaces) + 0.5) / (double) Math.pow(10, decimalPlaces);
    }
}