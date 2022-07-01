import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;

public class Level implements KeyListener {
    private DrawingPanel frame;
    private Frame fr;
    private Graphics g;

    private String name;
    private String fileName;
    private String creator = "Tomb of the Mask";
    private boolean unlocked = false;
    private int bestScore = -1;
    private int bestTime = Integer.MAX_VALUE;

    private ArrayList<ArrayList<Cell>> map;
    private ArrayList<Entity> entities;
    private Player player;
    private Camera camera;
    private boolean waiting;
    private boolean playing = false;
    private boolean quickStop = false;
    private int[] maxScores;
    private int animationTick;
    private double waterSpeed;
    private double waterHeight;

    public static void main(String[] args) {
        DrawingPanel f = new DrawingPanel(Game.WIDTH, Game.HEIGHT);
        Level l = new Level(LevelBuilder.EXPORT_LOCATION, "Exported Level", f);
        l.play();
    }

    public Level(String file, String n, DrawingPanel f) {
        this(file, f);
        if (name.equals("")) {
            name = n;
        }
    }

    public Level(String file, DrawingPanel f) {
        name = "";
        frame = f;
        g = f.getGraphics();
        frame.addKeyListener(this);
        fileName = file;
        newLevel();
    }

    public void newLevel() {
        waterHeight = -9;
        waterSpeed = 0;

        // Builds the map from a file
        map = new ArrayList<>();
        entities = new ArrayList<>();
        maxScores = new int[4];
        quickStop = false;
        try {
            Scanner fileScanner = new Scanner(new File(fileName));
            int y = 0;

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine();
                while (line.indexOf("  ") >= 0)
                    line = line.replaceAll("  ", " ");

                if (line.startsWith("waterSpeed:")) {
                    waterSpeed = Double.parseDouble(line.substring(line.indexOf(" ") + 1));
                } else if (line.startsWith("creator:")) {
                    creator = line.substring(line.indexOf(" ") + 1);
                } else if (line.startsWith("name:")) {
                    name = line.substring(line.indexOf(" ") + 1);
                } else {
                    String[] list = line.split(" ");
                    ArrayList<Cell> row = new ArrayList<>();
                    for (int x = 0; x < list.length; x++) {
                        if (list[x].equals("p")) {
                            player = new Player(x, y);
                            row.add(new Cell(x, y, "0"));
                            camera = new Camera(x, y);
                        } else {
                            Entity e = Entity.newEntity(list[x], x, y);
                            if (e != null) {
                                entities.add(e);
                                if (!list[x].startsWith("f"))
                                    row.add(new Cell(x, y, "0"));
                            }
                            if (e == null || list[x].startsWith("f")) {
                                Cell c = Cell.newCell(list[x], x, y);
                                if (c != null) {
                                    row.add(c);
                                    int val = row.get(x).getValue();
                                    if (val > 0)
                                        maxScores[val - 1]++;
                                }
                            }
                        }
                    }
                    map.add(row);
                    y++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // adds spike cells in empty cells adjacent to trap cells
        for (int y = 0; y < map.size(); y++) {
            for (int x = 0; x < map.get(y).size(); x++) {
                int[][] directions = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
                if (map.get(y).get(x).isType("t")) {
                    for (int[] dir : directions) {
                        if (isValid(x + dir[0], y + dir[1])) {
                            if (map.get(y + dir[1]).get(x + dir[0]).isType("_")) {
                                map.get(y + dir[1]).set(x + dir[0], Cell
                                        .newCell("s" + map.get(y + dir[1]).get(x + dir[0]).getValue(), x + dir[0],
                                                y + dir[1]));
                            }
                        }
                    }
                }
            }
        }

        // sets each cell's image code after checking neighbors
        for (int y = 0; y < map.size(); y++) {
            for (int x = 0; x < map.get(y).size(); x++) {
                int[][] directions = new int[][] { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 },
                        { -1, 0 },
                        { -1, -1 } };
                String code = "";
                for (int[] dir : directions) {
                    if (isValid(x + dir[0], y + dir[1])) {
                        if (map.get(y).get(x).isNeighbor(map.get(y + dir[1]).get(x + dir[0]).getType())) {
                            code += "1";
                        } else {
                            code += "0";
                        }
                    } else {
                        code += "1";
                    }
                }
                code = Cell.simplifyCode(code);
                map.get(y).get(x).setCode(code);
            }
        }
        fr = new Frame(this);
    }

    public int play() {
        boolean printing = true;

        TimeTracker.reset();

        // INITIALIZE SOME IMPORTANT SETTINGS
        waiting = true;
        playing = true;
        animationTick = 0;

        newLevel();
        draw();
        while (waiting) {
            // wait for first player input
            frame.sleep(1);
        }
        ArrayList<Long> tickTimes = new ArrayList<>();
        int MSPT = 1000 / Game.TPS;
        long time = System.currentTimeMillis();
        long startTime = time;
        int tick = 0;
        int laggyTicks = 0;
        while (player.isAlive() && !player.isWin()) {
            if (time + MSPT < System.currentTimeMillis()) {
                time += MSPT;
                long tickStartTime = System.currentTimeMillis();

                if (time + MSPT < System.currentTimeMillis()) {
                    laggyTicks++;
                }

                if (tick % (Game.TPS / 10) == 0) {
                    animationTick++;
                }

                TimeTracker.start("level updates");

                // update the player
                int[] pPos = player.attemptMove();
                if (map.get(pPos[1]).get(pPos[0]).touch(player))
                    player.setMoving(false);
                else {
                    pPos = player.move();
                }
                map.get(pPos[1]).get(pPos[0]).touch(player);
                camera.move(player.getX(), player.getY());
                if (pPos[1] >= map.size() - Math.ceil(waterHeight)) {
                    player.setAlive(false);
                }
                player.setAnimationKey(getAnimationKey(2, 6));

                // secret endless level!
                if (name.equals("???")) {
                    if (player.getY() < 20) {
                        player.setY(player.getY() + 100);
                        camera.offsetY(100 * camera.getScale());
                    }
                }

                // update entities
                for (int i = 0; i < entities.size(); i++) {
                    Entity e = entities.get(i);
                    if (e.getType().equals("f")) {
                        e.update(player);
                    } else {
                        int[] ePos = e.attemptMove();
                        if (map.get(ePos[1]).get(ePos[0]).isSolid() || map.get(ePos[1]).get(ePos[0]).getValue() == 4) {
                            e.touch();
                        } else {
                            e.update(player);
                        }
                        if (!e.isAlive()) {
                            entities.remove(e);
                            i--;
                        }
                    }
                }

                // update cells
                for (ArrayList<Cell> row : map)
                    for (Cell c : row)
                        c.update(this);

                // update water
                if (waterSpeed != 0) {
                    waterHeight += waterSpeed / Game.TPS;
                }

                if (quickStop) {
                    player.setAlive(false);
                }

                draw();
                tick++;

                TimeTracker.start("tickTracker");

                // tick length tracking
                tickTimes.add(0, System.currentTimeMillis() - tickStartTime);
                if (tickTimes.size() > Game.TPS * 5) {
                    tickTimes.remove(tickTimes.size() - 1);
                }

                // print some stuff
                if (printing && tick % (Game.TPS / 15) == 0) {
                    int[] scores = player.getValues();

                    double sum = 0;
                    for (long t : tickTimes) {
                        sum += t;
                    }
                    sum /= tickTimes.size();
                    String percent = String.format("%.2f", (sum / MSPT) * 100);

                    System.out.println("\n\n\n" +
                            "\nDots:  " + scores[0] + "/" + maxScores[0] +
                            "\nCoins: " + scores[1] + "/" + maxScores[1] +
                            "\nStars: " + scores[2] + "/" + maxScores[2] +
                            "\nAverage Tick Length: " + String.format("%.3f", sum) + "/" + MSPT + " ms (" + percent
                            + "%)" +
                            "\nLaggy Tick Count: " + laggyTicks +
                            "\n" + TimeTracker.getValues());
                }

                TimeTracker.start("left-overs");
            }
        }
        playing = false;
        if (player.isAlive())
            bestTime = Math.min(bestTime, (int) (time - startTime));
        frame.sleep(500);

        if (!player.isAlive())
            return -1;

        int finalScore = 0;
        int[] scores = player.getValues();
        finalScore = scores[2] * 2;
        if ((maxScores[1] + maxScores[0]) == (scores[1] + scores[0])) {
            finalScore++;
        }

        if (bestScore < finalScore) {
            bestScore = finalScore;
        }
        return finalScore;
    }

    public int checkPosition(int x, int y) {
        Cell c = map.get(y).get(x);
        if (c.isKill())
            return 2;
        if (c.isSolid())
            return 1;
        return 0;
    }

    public void spawnArrow(int xPos, int yPos, String data) {
        entities.add(Entity.newEntity("l" + data, xPos, yPos));
    }

    public void draw() {
        BufferedImage preFrame = fr.buildFrame();
        Graphics gP = preFrame.getGraphics();

        if (waterSpeed != 0) {
            BufferedImage gradient = null;
            try {
                gradient = ImageIO.read(new File(Game.IMAGES + "water.png"));
            } catch (IOException e) {
                System.out.println("water not found");
            }

            gP.setColor(new Color(gradient.getRGB(getAnimationKey(0.5, gradient.getWidth()), 0)));
            int screenHeight = (int) (waterHeight * camera.getScale() + camera.getY());
            gP.fillRect(0, map.size() * camera.getScale() - screenHeight, Game.WIDTH, screenHeight);
        }

        TimeTracker.start("draw image");

        g.drawImage(preFrame, 0, 0, null);
    }

    public void fillRectScaled(double gridX, double gridY) {
        g.fillRect((int) (gridX * camera.getScale() - camera.getX()), (int) (gridY * camera.getScale() - camera.getY()),
                camera.getScale(), camera.getScale());
    }

    public String getName() {
        return name;
    }

    public void setUnlocked(boolean value) {
        unlocked = value;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public int getBestScore() {
        return bestScore;
    }

    public double getBestTime() {
        return (double) bestTime / 1000;
    }

    public int getCoinCount() {
        return player.getValues()[1];
    }

    public boolean isValid(int x, int y) {
        return y >= 0 && y < map.size() && x >= 0 && x < map.get(y).size();
    }

    public int getAnimationKey(double frameDuration, int frames) {
        return (int) (animationTick / frameDuration) % frames;
    }

    public int getAnimationTick() {
        return animationTick;
    }

    public double getWaterHeight() {
        return waterHeight;
    }

    public String getCreator() {
        return creator;
    }

    public ArrayList<ArrayList<Cell>> getMap() {
        return map;
    }

    public Camera getCamera() {
        return camera;
    }

    public ArrayList<Entity> getEntities() {
        return entities;
    }

    public Player getPlayer() {
        return player;
    }

    public void keyPressed(KeyEvent e) {
        if (playing) {
            int key = e.getKeyCode();
            player.queue(key);
            if (waiting)
                waiting = false;

            if (key == KeyEvent.VK_ESCAPE) {
                quickStop = true;
            }
        }
    }

    public void keyReleased(KeyEvent e) {
        if (playing) {
            if (player.getQueue() == e.getKeyCode())
                player.queue(0);
        }
    }

    public void keyTyped(KeyEvent e) {
    }
}