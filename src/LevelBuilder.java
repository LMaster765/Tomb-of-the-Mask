import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

public class LevelBuilder implements KeyListener, MouseListener, MouseMotionListener {
    public static String EXPORT_LOCATION = "bin/Level Stuff/exportedLevel.txt";

    private DrawingPanel frame;
    private Graphics g;
    private ArrayList<ArrayList<Cell>> level;
    private ArrayList<ArrayList<Cell>> oldLevel;
    private BufferedImage rendered;

    private int scale = 36;
    private int width = scale * 21;
    private int height = scale * 31;
    private int[] cam = new int[] { 0, 0 };
    private String selected = "w";
    private boolean trial;
    private boolean on = true;

    public static void main(String[] args) {
        new LevelBuilder();
    }

    public LevelBuilder() {
        frame = new DrawingPanel(width, height);
        g = frame.getGraphics();
        frame.addMouseListener(this);
        frame.addKeyListener(this);

        System.out.println("01234   cell value");
        System.out.println("^>v<   direction");
        System.out.println("");
        System.out.println("_   empty   (+value)");
        System.out.println("w   wall");
        System.out.println("k   kill");
        System.out.println("t   trap   (+value)");
        System.out.println("f   arrow launcher   (+dir)");
        System.out.println("j   spring   (+dir)");
        System.out.println("b   bat   (+dir)");
        System.out.println("f   pufferfish");
        System.out.println("g   goal");
        System.out.println("p   player");

        g.setFont(new Font("Dialog", Font.BOLD, scale));
        importLevel();
        initDraw(0, 0);

        while (on) {
            if (trial) {
                DrawingPanel f = new DrawingPanel(Game.WIDTH, Game.HEIGHT);
                Level l = new Level(EXPORT_LOCATION, f);
                l.play();
                f.getFrame().dispose();
                trial = false;
            }
            frame.sleep(10);
        }
    }

    public void resetLevel() {
        oldLevel = level;
        level = new ArrayList<>();
        for (int y = 0; y < 5; y++) {
            ArrayList<Cell> row = new ArrayList<>();
            for (int x = 0; x < 5; x++) {
                row.add(new Cell(x, y, "0"));
            }
            level.add(row);
        }
        initDraw(0, 0);
    }

    public void restoreLevel() {
        ArrayList<ArrayList<Cell>> temp = level;
        level = oldLevel;
        oldLevel = temp;
        initDraw(0, 0);
    }

    public boolean isValid(int x, int y) {
        return y >= 0 && y < level.size() && x >= 0 && x < level.get(y).size();
    }

    public void makeValid(int x, int y) {
        int newX = Math.min(x, 0);
        int newY = Math.min(y, 0);

        // add to bottom
        while (level.size() <= y) {
            ArrayList<Cell> row = new ArrayList<>();
            for (int x1 = 0; x1 < level.get(0).size(); x1++) {
                row.add(new Cell(x1, level.size(), "0"));
            }
            level.add(row);
        }

        // add to top
        while (y < 0) {
            ArrayList<Cell> row = new ArrayList<>();
            for (int x1 = 0; x1 < level.get(0).size(); x1++) {
                row.add(new Cell(x1, level.size(), "0"));
            }
            level.add(0, row);
            y++;
            cam[1] += scale;
        }

        // add to right
        while (level.get(y).size() <= x) {
            for (int y1 = 0; y1 < level.size(); y1++) {
                level.get(y1).add(new Cell(x, y1, "0"));
            }
        }

        // add to left
        while (x < 0) {
            for (int y1 = 0; y1 < level.size(); y1++) {
                level.get(y1).add(0, new Cell(x, y1, "0"));
            }
            x++;
            cam[0] += scale;
        }

        initDraw(newX, newY);
    }

    public void initDraw(int newRows, int newCols) {
        rendered = new BufferedImage(level.get(0).size() * scale, level.size() * scale, BufferedImage.TYPE_INT_RGB);
        Graphics g2 = rendered.getGraphics();
        g2.setFont(new Font("Dialog", Font.BOLD, scale));

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);

        // # update neighbors first
        for (int y = 0; y < level.size(); y++) {
            for (int x = 0; x < level.get(y).size(); x++) {
                int[][] directions = new int[][] { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 },
                        { 0, 1 }, { -1, 1 }, { -1, 0 }, { -1, -1 } };
                String code = "";
                for (int[] dir : directions) {
                    if (isValid(x + dir[0], y + dir[1])) {
                        if (level.get(y).get(x).isNeighbor(level.get(y + dir[1]).get(x + dir[0]).getType())) {
                            code += "1";
                        } else {
                            code += "0";
                        }
                    } else {
                        code += "1";
                    }
                }
                code = Cell.simplifyCode(code);
                level.get(y).get(x).setCode(code);
            }
        }

        for (int y = 0; y < level.size(); y++) {
            for (int x = 0; x < level.get(y).size(); x++) {
                Cell c = level.get(y).get(x);
                int xScreen = x * scale;
                int yScreen = y * scale;
                // draw tile
                BufferedImage imgTest = c.getImage(1);
                ((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
                g2.fillRect(xScreen, yScreen, scale, scale);
                ((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                g2.drawImage(Cell.transformImage(imgTest, -c.getRotation() * Math.PI / 2,
                        scale / (double) Game.IMAGE_SIZE), xScreen, yScreen, null);

                // draw dots
                g2.drawImage(Cell.transformImage(c.getDot(scale), 0, scale / (double) Game.IMAGE_SIZE),
                        xScreen, yScreen, null);
                System.out.println(c.getValue());
            }
        }
        draw();
    }

    public void draw() {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        int minX = Math.max(cam[0], 0);
        int minY = Math.max(cam[1], 0);
        int w = Math.min(rendered.getWidth() - minX, width);
        int h = Math.min(rendered.getHeight() - minY, height);

        g.drawImage(rendered.getSubimage(minX, minY, w, h), Math.max(-cam[0], 0), Math.max(-cam[1], 0), frame);

        g.setColor(Color.GRAY);
        // # grid lines
        for (int y = 1; y < level.size(); y++)
            g.drawLine(-cam[0], y * scale - cam[1], level.get(0).size() * scale - cam[0], y * scale - cam[1]);
        for (int x = 1; x < level.get(0).size(); x++)
            g.drawLine(x * scale - cam[0], -cam[1], x * scale - cam[0], level.size() * scale - cam[1]);

        g.setColor(Color.WHITE);
        for (int i = -2; i < 2; i++) {
            g.drawRect(4 * scale + i, 4 * scale + i, 13 * scale - 2 * i, 26 * scale - 2 * i);
        }

        // # top display

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, scale * 3);
        g.setColor(Color.WHITE);
        g.fillRect(0, scale * 3 - 2, width, 2);
        g.drawString("Selected: " + selected, scale, scale);
        g.drawString("Escape to Save and Exit", width / 2 + scale, scale);
        g.drawString("Enter to Save and Test", width / 2 + scale, scale * 2);
    }

    public void edit(MouseEvent e) {
        if (e.getY() < scale * 3)
            return;

        int xPos = e.getX() + cam[0];
        int yPos = e.getY() + cam[1];

        if (xPos >= 0)
            xPos /= scale;
        else
            xPos = xPos / scale - 1;
        if (yPos >= 0)
            yPos /= scale;
        else
            yPos = yPos / scale - 1;

        if (!isValid(xPos, yPos))
            makeValid(xPos, yPos);

        xPos = Math.max(xPos, 0);
        yPos = Math.max(yPos, 0);

        if (isValid(xPos, yPos)) {
            // cannot have 2 players
            if ("p".indexOf(selected.substring(0, 1)) != -1)
                for (int y = 0; y < level.size(); y++)
                    for (int x = 0; x < level.get(yPos).size(); x++)
                        if (level.get(yPos).get(xPos).isType("p"))
                            level.get(yPos).set(xPos, new Cell(xPos, yPos, "0"));
            level.get(yPos).set(xPos, Cell.newCell(selected, xPos, yPos));
            if (selected.length() > 1 && "0123456789".indexOf(selected.substring(1, 2)) != -1)
                level.get(yPos).get(xPos).setValue(Integer.parseInt(selected.substring(1, 2)));

            // # update neighbors of (posX, posY) first
            int[][] directions = new int[][] { { 0, 0 }, { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 },
                    { -1, 0 }, { -1, -1 } };
            int[][] neighbors = new int[][] { { 0, -1 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 }, { -1, 0 },
                    { -1, -1 } };
            for (int[] dir : directions) {
                int x = xPos + dir[0];
                int y = yPos + dir[1];
                if (isValid(x, y)) {
                    String code = "";
                    for (int[] dir2 : neighbors) {
                        if (isValid(x + dir2[0], y + dir2[1])) {
                            if (level.get(y).get(x).isNeighbor(level.get(y + dir2[1]).get(x + dir2[0]).getType())) {
                                code += "1";
                            } else {
                                code += "0";
                            }
                        } else {
                            code += "1";
                        }
                    }
                    code = Cell.simplifyCode(code);
                    level.get(y).get(x).setCode(code);
                }
            }

            Graphics g2 = rendered.getGraphics();
            g2.setFont(new Font("Dialog", Font.BOLD, scale));

            // # update drawing of (posX, posY) and neighbors
            for (int[] dir : directions) {
                int x = xPos + dir[0];
                int y = yPos + dir[1];
                if (isValid(x, y)) {
                    Cell c = level.get(y).get(x);
                    int xScreen = x * scale;
                    int yScreen = y * scale;
                    // draw tile
                    BufferedImage imgTest = c.getImage(1);
                    ((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
                    g2.fillRect(xScreen, yScreen, scale, scale);
                    ((Graphics2D) g2).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
                    g2.drawImage(Cell.transformImage(imgTest, -c.getRotation() * Math.PI / 2,
                            scale / (double) Game.IMAGE_SIZE), xScreen, yScreen, null);
                    // draw dots
                    g2.drawImage(Cell.transformImage(c.getDot(scale), 0, scale / (double) Game.IMAGE_SIZE),
                            xScreen, yScreen, null);
                    System.out.println(c.getValue());
                }
            }
        }
        draw();
    }

    public void importLevel() {
        level = new ArrayList<>();
        try {
            Scanner file = new Scanner(new File(EXPORT_LOCATION));
            while (file.hasNextLine()) {
                String line = file.nextLine();
                while (line.indexOf("  ") >= 0)
                    line = line.replaceAll("  ", " ");

                String[] list = line.split(" ");
                ArrayList<Cell> row = new ArrayList<>();
                for (String cell : list) {
                    Cell c = Cell.newCell(cell, 0, 0);
                    if (c.getValue() == 2 && cell.substring(1, 2).equals("1"))
                        c.setValue(1);
                    row.add(c);
                }
                level.add(row);
            }
        } catch (FileNotFoundException e) {
            System.out.println("File read failed");
        }
    }

    public void exportLevel() {
        String levelFile = "";
        for (ArrayList<Cell> row : level) {
            for (Cell c : row) {
                String cell = c.toString();
                if (cell.length() == 1)
                    cell += " ";
                levelFile += cell + " ";
            }
            levelFile += "\n";
        }

        try {
            FileWriter file = new FileWriter(EXPORT_LOCATION);
            file.write(levelFile);
            file.close();

            trial = true;
        } catch (IOException e) {
            System.out.println("An error occurred while writing the file");
            e.printStackTrace();
            System.out.println(levelFile);
        }
    }

    public void mouseClicked(MouseEvent e) {
        edit(e);
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        edit(e);
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        String bases = "_wktbapfgj";
        String mods = "0123456789^><v";
        String input = ("" + e.getKeyChar()).toLowerCase();
        if ((bases + mods).indexOf(input) != -1) {
            if (mods.indexOf(input) != -1) {
                selected = selected.substring(0, 1) + input;
            } else {
                selected = input;
            }
        }
        int code = e.getKeyCode();
        switch (code) {
            case KeyEvent.VK_UP:
                cam[1] -= scale;
                break;
            case KeyEvent.VK_RIGHT:
                cam[0] += scale;
                break;
            case KeyEvent.VK_DOWN:
                cam[1] += scale;
                break;
            case KeyEvent.VK_LEFT:
                cam[0] -= scale;
                break;
            case KeyEvent.VK_ENTER:
                exportLevel();
                break;
            case KeyEvent.VK_ESCAPE:
                on = false;
                exportLevel();
                frame.getFrame().dispose();
                break;
            case KeyEvent.VK_DELETE:
            case KeyEvent.VK_BACK_SPACE:
                resetLevel();
                break;
            case KeyEvent.VK_INSERT:
                // case KeyEvent.VK_EQUALS:
                restoreLevel();
                break;
        }
        draw();
    }

    public void keyReleased(KeyEvent e) {
    }
}