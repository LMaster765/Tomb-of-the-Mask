import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import javax.imageio.*;
import java.io.*;

public class PixelFont {
    private static String LOCATIONS = "abcdefghijklmnopqrstuvwxyz0123456789-:.?";
    private static int WIDTH = 6;
    private static BufferedImage FONT;

    private BufferedImage fontSheet;
    private int size;

    static {
        try {
            FONT = ImageIO.read(new File(Game.IMAGES + "Font.png"));
        } catch (IOException e) {
            System.out.println("font not found");
        }
    }

    public PixelFont(int height, Color color) {
        size = height;
        int scale = height / WIDTH;

        fontSheet = new BufferedImage(FONT.getWidth() * scale, FONT.getHeight() * scale, BufferedImage.TYPE_INT_ARGB);
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);

        AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        op.filter(FONT, fontSheet);

        for (int y = 0; y < fontSheet.getHeight(); y++)
            for (int x = 0; x < fontSheet.getWidth(); x++)
                if (fontSheet.getRGB(x, y) != 0)
                    fontSheet.setRGB(x, y, color.getRGB());
    }

    public void drawString(String str, int x, int y, Graphics g) {
        drawString(str, x, y, 0, 1, g);
    }
    
    public void drawString(String str, int x, int y, double xAlignment, Graphics g) {
        drawString(str, x, y, xAlignment, 1, g);
    }

    public void drawString(String str, int x, int y, double xAlignment, double yAlignment, Graphics g) {
        x -= (int)(str.length() * size * xAlignment);
        y -= (int)(size * yAlignment);
        for (int i = 0; i < str.length(); i++) {
            int imgIndex = LOCATIONS.indexOf(str.toLowerCase().charAt(i));
            if (imgIndex > -1) {
                BufferedImage ch = fontSheet.getSubimage(imgIndex * size, 0, size, size);
                g.drawImage(ch, x, y, null);
            }
            x += size;
        }
        g.setColor(Color.GREEN);
    }
}