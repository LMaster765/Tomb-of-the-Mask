public class Camera {
    public static final int DEFAULT_SCALE = 36;
    public static final double EASING_RATE = 7 / (double) Game.TPS;

    private int scale = DEFAULT_SCALE;
    private int xPos = 0;
    private int yPos = 0;

    private int minX = Integer.MIN_VALUE;
    private int minY = Integer.MIN_VALUE;
    private int maxX = Integer.MAX_VALUE;
    private int maxY = Integer.MAX_VALUE;

    public Camera(int playerX, int playerY) {
        xPos = playerX * scale + (scale - Game.WIDTH) / 2;
        yPos = playerY * scale + (scale - Game.HEIGHT) / 2;
    }

    public int getScale() {
        return scale;
    }

    public int getX() {
        return xPos;
    }

    public int getY() {
        return yPos;
    }

    public void move(double playerX, double playerY) {
        int targetX = (int) (playerX * scale + (scale - Game.WIDTH) / 2);
        int targetY = (int) (playerY * scale + (scale - Game.HEIGHT) / 2);
        xPos += (int) ((targetX - xPos) * EASING_RATE);
        yPos += (int) ((targetY - yPos) * EASING_RATE);
        xPos = Math.min(Math.max(xPos, minX), maxX);
        yPos = Math.min(Math.max(yPos, minY), maxY);
    }

    public void setPos(int x, int y) {
        xPos = Math.min(Math.max(x * scale + (scale - Game.WIDTH) / 2, minX), maxX);
        yPos = Math.min(Math.max(y * scale + (scale - Game.HEIGHT) / 2, minY), maxY);
    }

    public boolean isVisible(double gridX, double gridY) {
        return (gridX + 1) * scale > xPos &&
                (gridY + 1) * scale > yPos &&
                gridX * scale < xPos + Game.WIDTH &&
                gridY * scale < yPos + Game.HEIGHT;
    }

    public void setMinX(int value) {
        minX = value;
    }

    public void setMaxX(int value) {
        maxX = value;
    }

    public void setScale(int value) {
        scale = value;
    }
    
    public void offsetY(int value) {
        yPos += value;
    }
}