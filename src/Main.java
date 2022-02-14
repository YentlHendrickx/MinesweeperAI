import javax.imageio.ImageIO;
import javax.sound.midi.Receiver;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;

public class Main {
    // Window settings
    static int screenWidth = 1920;
    static int screenHeight = 1080;

    static int boardWidth = 9;
    static int boardHeight = 9;
    static double boardPixels = 35f;
    static int boardTopX = 200;
    static int boardTopY = 150;
    static int mouseLocationX = screenWidth / 2;
    static int mouseLocationY = screenHeight /2;


    static Robot robot = null;

    public static void main(String[] args) throws Throwable{
        robot = new Robot();
        calibrateParameters();

        // Check for failed calibration
        if (boardWidth < 5 || boardWidth > 30 || boardHeight < 5 || boardHeight > 30) {
            System.out.println("Failed calibration process!");
            return;
        }

        calibratedImage();
        // Move mouse and click on the first square! THE GAME BEGINS!
        clickFirst();
    }

    static void clickFirst() throws Throwable {
        // Check if it's the first square
        robot.mouseMove(0,0 );
        Thread.sleep(20);
        clickPosition(boardHeight / 2 - 1, boardWidth / 2 - 1);
        clickPosition(boardHeight / 2 - 1, boardWidth / 2 - 1);
        Thread.sleep(200);
    }

    // Click a square
    static void clickPosition(int y, int x) throws Throwable {
        int mouseX = boardTopX + (int)(x*boardPixels);
        int mouseY = boardTopY + (int)(y*boardPixels);
        moveMouse(mouseX, mouseY);

        robot.mousePress(16);
        Thread.sleep(5);
        robot.mouseRelease(16);
        Thread.sleep(10);
    }

    // Create screenshot and determine board position
    static void calibrateParameters() {
        System.out.println("Calibration started");

        BufferedImage fs = fullScreenImage();
        assert fs != null;
        fs.createGraphics();
        Graphics2D graphics = (Graphics2D)fs.getGraphics();

        // We will look for crosses in the grid
        int hh = 0;
        int firstX = 0;
        int firstY = 0;
        int lastX = 0;
        int lastY = 0;
        int total = 0;

        for (int width = 0; width < screenWidth; width++) {
            for (int height = 0; height < screenHeight; height++) {
                int rgb = fs.getRGB(width, height);


                if (isDark(rgb)){
                    // Check boundaries
                    if (width < 10 || height < 10 || width > screenWidth - 10 || height > screenHeight - 10) continue;

                    // Look for cross shape
                    // For a cross we need to have:
                    // A dark pixel
                    // Four pixels to the N,S,E,W dark and the pixels to NE,SE,NW,SW not dark
                    // I KNOW THIS LOOKS AWFUL, DON'T JUDGE
                    if(isDark(fs.getRGB(width + 7, height)))
                        if(isDark(fs.getRGB(width - 7, height)))
                            if(isDark(fs.getRGB(width, height + 7)))
                                if(isDark(fs.getRGB(width, height - 7)))
                                    if(isDark(fs.getRGB(width + 3, height)))
                                        if(isDark(fs.getRGB(width -3, height)))
                                            if(isDark(fs.getRGB(width, height + 3)))
                                                if(isDark(fs.getRGB(width, height - 3)))
                                                    if(!isDark(fs.getRGB(width - 7, height - 7)))
                                                        if(!isDark(fs.getRGB(width + 7, height - 7)))
                                                            if(!isDark(fs.getRGB(width - 7, height + 7)))
                                                                if(!isDark(fs.getRGB(width + 7, height + 7)))
                                                                    if(!isDark(fs.getRGB(width - 3, height - 3)))
                                                                        if(!isDark(fs.getRGB(width + 3, height - 3)))
                                                                            if(!isDark(fs.getRGB(width - 3, height + 3)))
                                                                                if(!isDark(fs.getRGB(width + 3, height + 3))) {

                                                                                    // For calibration.png
                                                                                    graphics.setColor(Color.YELLOW);
                                                                                    graphics.fillRect(width - 3, height -3, 7, 7);
                                                                                    total++;
                                                                                    boardHeight ++;

                                                                                    // Set the position of the first cross
                                                                                    if (firstY == 0) {
                                                                                        firstY = height;
                                                                                        firstX = width;
                                                                                    }

                                                                                    // Set the position of the last cross
                                                                                    lastY = height;
                                                                                    lastX = width;
                                                                                }
                }
            }
            if (boardHeight > 1) {
                hh = boardHeight;
                boardHeight = 1;
            }
        }

        // Calculate board width from total and board height
        boardHeight = hh;
        if (total % (boardHeight - 1) == 0) {
            boardWidth = total / (boardHeight - 1) + 1;
        } else {
            boardWidth = 0;
        }

        // Calculate board position by taking average
        boardPixels = 0.5*((double)(lastX - firstX) / (double)(boardHeight - 2))
                + 0.5*((double)(lastY - firstY) / (double)(boardHeight - 2));

        // Calculate first cell position
        int boardHalf = (int)boardPixels / 2;
        boardTopX = firstX - boardHalf + 3;
        boardTopY = firstY - boardHalf + 3;

        System.out.printf("Board Width:%d, Board Height:%d, Board Pixels:%f\n", boardWidth, boardHeight, boardPixels);
        System.out.printf("Board Top X:%d, Board Top Y:%d\n", boardTopX, boardTopY);
    }

    // Control mouse
    static void moveMouse(int targetX, int targetY) throws Throwable {
        /// Move mouse smoothly to target
        // Use some math to calculate the distance
        int distance = Math.max(Math.abs(targetX - mouseLocationX), Math.abs(targetY - mouseLocationY));
        int steps = (distance / 4) / 5 ;

        double stepX = (double)(targetX - mouseLocationX) / (double)steps;
        double stepY = (double)(targetY - mouseLocationY) / (double)steps;

        for (int step = 0; step < steps; step++) {
            robot.mouseMove(mouseLocationX + (int)(step*stepX), mouseLocationY + (int)(step*stepY));
            Thread.sleep(5);
        }
        robot.mouseMove(targetX, targetY);
        mouseLocationX = targetX;
        mouseLocationY = targetY;
    }

    // Return true if a specific rgb value is considered dark (total red + green + blue value less than 120)
    static boolean isDark(int rgb) {
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        return red + green + blue < 120;
    }

    static BufferedImage calibratedImage() {
        try {
            Rectangle size = new Rectangle(boardTopX - boardWidth, boardTopY - boardHeight, (int)boardPixels * boardWidth, (int)boardPixels * boardHeight);

            BufferedImage bufImage = robot.createScreenCapture(size);
            File imageF = new File("screenshot2.png");
            ImageIO.write(bufImage, "png", imageF);
            return bufImage;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static BufferedImage fullScreenImage() {
        try {
            Rectangle size = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            screenWidth = size.width;
            screenHeight = size.height;

            BufferedImage bufImage = robot.createScreenCapture(size);
            File imageF = new File("screenshot.png");
            ImageIO.write(bufImage, "png", imageF);
            return bufImage;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
