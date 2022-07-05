import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

public class Game extends MouseAdapter implements KeyListener {
    public static final int IMAGE_SIZE = 12;
    public static final int WIDTH = Camera.DEFAULT_SCALE * 13;
    public static final int HEIGHT = Camera.DEFAULT_SCALE * 26;
    public static final int TPS = 120; // I WANT TO INCREASE THIS TO 60+ IF I CAN FIX THE LAG
    public static final String IMAGES = "bin/Images/";
    public static final String LEVELS = "bin/Levels";

    private DrawingPanel frame;
    private Graphics g;
    private int selX = 0;
    private int selY = 0;
    private Level playingLevel;
    private boolean active = true;
    private ArrayList<LevelSet> levelSets;
    private Camera cam;
    private int coinTotal;

    private int topBanner = Camera.DEFAULT_SCALE * 4;
    private int paddingX = Camera.DEFAULT_SCALE * 1;
    private int paddingY = Camera.DEFAULT_SCALE;
    private int lvlWidth = (int)(Camera.DEFAULT_SCALE * 1);
    private int lvlHeight = (int)(Camera.DEFAULT_SCALE * 1);
    private int lvlSelectHeight = 2 * (paddingY + lvlHeight);
    private PixelFont creditFont = new PixelFont(12, Color.BLACK);
    private PixelFont smallFont = new PixelFont(Camera.DEFAULT_SCALE / 2, Color.BLACK);
    private PixelFont largeFont = new PixelFont(Camera.DEFAULT_SCALE, Player.COLOR);

    public static void main(String[] args) {
        new Game();
    }

    public Game() {
        frame = new DrawingPanel(WIDTH, HEIGHT);
        g = frame.getGraphics();

        g.setColor(Color.BLACK);

        levelSets = new ArrayList<>();
        File root = new File(LEVELS);
        String[] levelSetList = root.list();
        for (String levelSetName : levelSetList) {
            LevelSet levelSet = new LevelSet(levelSetName);
            File lsFile = new File(LEVELS + "/" + levelSetName);
            String[] levelList = lsFile.list();
            boolean first = true;
            for (String levelName : levelList) {
                Level level = new Level(LEVELS + "/" + levelSetName + "/" + levelName, getName(levelName), frame);
                if (first) {
                    level.setUnlocked(true);
                    first = false;
                }
                levelSet.add(level);
            }
            levelSets.add(levelSet);
        }

        cam = new Camera(0, 0);
        cam.setMinX(-paddingX / 2);
        cam.setMaxX((levelSets.get(0).size() / 2 + 1) * (paddingX + lvlWidth) - WIDTH + paddingX / 2);
        cam.setScale(paddingX + lvlWidth);
        cam.setPos(0, 0);

        // intro();

        frame.addMouseListener(this);
        frame.addKeyListener(this);

        draw();

        play();
    }

    public void intro() {
        Image top = frame.loadImage(IMAGES + "Top Menu.png");
        Image bottom = frame.loadImage(IMAGES + "Bottom Menu.png");
        g.drawImage(bottom, 0, 0, frame);
        g.drawImage(top, 0, 0, frame);
        frame.sleep(2000);
        for (double x = 0; x < 1; x += 0.005) {
            g.drawImage(bottom, 0, (int)(easeOutBounce(x) * Camera.DEFAULT_SCALE * 14), frame);
            g.drawImage(top, 0, 0, frame);
            frame.sleep(10);
        }
    }

    public void play() {
        while (active) {
            frame.sleep(20);
            if (playingLevel != null) {
                int score = playingLevel.play();

                // System.out.println("\nLevel Score: " + score);
                if (score >= 0) {
                    int nextLvl = levelSets.get(selY).indexOf(playingLevel) + 1;
                    if (levelSets.get(selY).size() > nextLvl)
                        levelSets.get(selY).get(nextLvl).setUnlocked(true);
                }
                coinTotal += playingLevel.getCoinCount();

                // System.out.println("Best Score: " + playingLevel.getBestScore());
                // System.out.println("Best Time: " + String.format("%.2f", playingLevel.getBestTime()));
                playingLevel = null;
                draw();
            }
        }
    }

    public void draw() {
        g.setColor(Cell.COLOR);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        //# draw top
        Image top = frame.loadImage(IMAGES + "Top Menu.png");
        g.drawImage(top, 0, 0, frame);
        /*
        g.setColor(Wall.COLOR);
        g.fillRect(0, 0, WIDTH, topBanner);
        g.setColor(Cell.COLOR);
        largeFont.drawString("Tomb of", WIDTH / 2, 0, 0.5, 0, g);
        largeFont.drawString("the Mask", WIDTH / 2, Camera.DEFAULT_SCALE, 0.5, 0, g);
        smallFont.drawString("Coins: " + coinTotal, Camera.DEFAULT_SCALE / 4, topBanner - Camera.DEFAULT_SCALE / 4, 0, g);
         */

        //# draw level specific menu
        int startY = 3 * lvlSelectHeight + topBanner + 2 * paddingY;
        /*g.setColor(Wall.COLOR);

        g.fillRect(0, startY, WIDTH, HEIGHT - startY);*/
        Image bottom = frame.loadImage(IMAGES + "Bottom Menu.png");
        g.drawImage(bottom, 0, Camera.DEFAULT_SCALE * 14, frame);

        g.setColor(Cell.COLOR);
        Level selLvl = levelSets.get(selY).get(selX);
        largeFont.drawString(selLvl.getName(), WIDTH / 2, startY + Camera.DEFAULT_SCALE / 4, 0.5, 0, g);

        int bestScore = selLvl.getBestScore();
        String scoreStr = "Best Score: ";
        if (bestScore > -1) {
            scoreStr += bestScore / 2 + " Stars";
        } else {
            scoreStr += "None";
        }

        smallFont.drawString(scoreStr, Camera.DEFAULT_SCALE / 4, startY + Camera.DEFAULT_SCALE * 2, 0, 0, g);
        String time;
        if (selLvl.getBestScore() != -1)
            time = "Best Time: " + String.format("%.2f", selLvl.getBestTime());
        else
            time = "No Best Time";
        smallFont.drawString(time, Camera.DEFAULT_SCALE / 4, startY + Camera.DEFAULT_SCALE * 3, 0, 0, g);
        creditFont.drawString("Level By: " + selLvl.getCreator(), Camera.DEFAULT_SCALE / 4, HEIGHT - Camera.DEFAULT_SCALE / 4, 0, 1, g);

        //# draw level select
        for (int y = 0; y < levelSets.size(); y++) {
            int x = 0;
            LevelSet levels = levelSets.get(y);
            largeFont.drawString(levels.getName(), WIDTH / 2, topBanner + paddingY + y * (lvlSelectHeight + 2 * paddingY), 0.5, 0, g);

            for (int j = 0; j < levels.size(); j++) {
                int[] pos = indexToPos(x);
                pos[1] += y * (lvlSelectHeight + 2 * paddingY) + 2 * paddingY;

                if (x == selX && y == selY) {
                    g.setColor(Color.WHITE);
                    int borderSize = 8;
                    g.fillRect(pos[0] - borderSize, pos[1] - borderSize, lvlWidth + 2 * borderSize,
                        lvlHeight + 2 * borderSize);
                }
                if (j > 0) {
                    g.setColor(Color.GRAY);
                    if (levels.get(j).isUnlocked() && levels.get(j - 1).isUnlocked())
                        g.setColor(Wall.COLOR);
                    if (levels.get(j).getBestScore() == 7 && levels.get(j - 1).getBestScore() == 7)
                        g.setColor(Player.COLOR);

                    int lineWidth = Camera.DEFAULT_SCALE / 4;
                    switch (x % 4) {
                        case 1:
                            g.fillRect(pos[0] + (lvlWidth - lineWidth) / 2, pos[1] - paddingY, lineWidth, paddingY);
                            break;
                        case 0:
                        case 2:
                            g.fillRect(pos[0] - paddingX, pos[1] + (lvlHeight - lineWidth) / 2, paddingX, lineWidth);
                            break;
                        case 3:
                            g.fillRect(pos[0] + (lvlWidth - lineWidth) / 2, pos[1] + lvlHeight, lineWidth, paddingY);
                            break;
                    }
                }
                g.setColor(Color.GRAY); 
                if (levels.get(j).isUnlocked())
                    g.setColor(Wall.COLOR);
                if (levels.get(j).getBestScore() == 7)
                    g.setColor(Player.COLOR);
                g.fillRect(pos[0], pos[1], lvlWidth, lvlHeight);
                g.setColor(Cell.COLOR);
                int score = levels.get(j).getBestScore();
                if (score == -1 || score % 2 == 0) {
                    g.fillOval(pos[0] + Camera.DEFAULT_SCALE - 12, pos[1] + 4, 8, 8);
                }

                x++;
            }
        }
    }

    public void drawStringCentered(String str, int x, int y) {
        g.drawString(str, x - str.length() * g.getFont().getSize() / 4, y);
    }

    public int[] indexToPos(int i) {
        int[] pos = new int[2];
        pos[0] = i / 2;
        int[] yPos = new int[] { 0, 1, 1, 0 };
        pos[1] = yPos[i % 4];

        pos[0] = pos[0] * (paddingX + lvlWidth) - cam.getX() + lvlWidth / 2;
        pos[1] = pos[1] * (paddingY + lvlHeight) + topBanner + paddingY;

        return pos;
    }

    public int posToIndex(int[] pos) {
        pos[0] = (pos[0] + cam.getX() - paddingX) / (paddingX + lvlWidth);
        pos[1] = (pos[1] - 128 - paddingY) / (paddingY + lvlHeight);

        int i = 0;
        if (pos[0] % 2 == 0) {
            i = pos[0] * 2;
            if (pos[1] == 1)
                i++;
        } else {
            i = pos[0] * 2;
            if (pos[1] != 1)
                i++;
        }
        return i;
    }

    public String getName(String fileName) {
        return fileName.substring(fileName.indexOf(".") + 1, fileName.lastIndexOf("."));
    }

    public int getPos(String fileName) {
        return Integer.parseInt(fileName.substring(0, fileName.indexOf(".")));
    }

    /*
     * I'll be honest, this isn't my code
     * https://easings.net/#easeOutBounce
     */
    public double easeOutBounce(double x) {
        double n1 = 7.5625;
        double d1 = 2.75;

        if (x < 1 / d1) {
            return n1 * x * x;
        } else if (x < 2 / d1) {
            return n1 * (x -= 1.5 / d1) * x + 0.75;
        } else if (x < 2.5 / d1) {
            return n1 * (x -= 2.25 / d1) * x + 0.9375;
        } else {
            return n1 * (x -= 2.625 / d1) * x + 0.984375;
        }
    }

    public void keyPressed(KeyEvent e) {
        if (playingLevel == null) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    active = false;
                    break;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    selX = Math.max(selX - 1, 0);
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    selX = Math.min(levelSets.get(selY).size() - 1, selX + 1);
                    break;
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    selY = Math.max(0, selY - 1);
                    break;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    selY = Math.min(levelSets.size() - 1, selY + 1);
                    selX = Math.min(levelSets.get(selY).size() - 1, selX);
                    break;
                case KeyEvent.VK_ENTER:
                    if (levelSets.get(selY).get(selX).isUnlocked())
                        playingLevel = levelSets.get(selY).get(selX);
                    break;
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                active = false;
                frame.getFrame().dispose();
            }
            if (e.getKeyCode() == KeyEvent.VK_U) {
                for (Level l : levelSets.get(selY)) {
                    l.setUnlocked(true);
                }
            }
            cam.setPos(selX / 2, 0);
            draw();
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }
}