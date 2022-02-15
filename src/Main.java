import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

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

    // Representation of the entire board using a 2d array
    // 1-8  = number of mines surrounding the square
    // 0    = empty square
    // -100 = mine square
    // -1   = unopened square
    static int[][] squareState = null;
    static boolean[][] bombSquare = null;

    static Robot robot = null;

    public static void main(String[] args) throws Throwable{
        robot = new Robot();
        calibrateParameters();

        // Check for failed calibration
        if (boardWidth < 8 || boardWidth > 30 || boardHeight < 8 || boardHeight > 30) {
            System.out.println("Failed calibration process!");
            return;
        }

        // Init 2d array
        squareState = new int[boardWidth][boardHeight];
        bombSquare = new boolean[boardWidth][boardHeight];
        // No mines to start with so set array
        for (int x = 0; x < boardWidth; x++) for (int y = 0; y < boardHeight; y++) bombSquare[x][y] = false;

        // Move mouse and click on the first square! THE GAME BEGINS!
        clickFirst();

        tryFlagging();

        updateBoard();
    }

    static int squareState(int x, int y) {
        if (x < 0 || y < 0 || x > boardWidth || y > boardHeight) return -10;
        return squareState[x][y];
    }

    static void tryFlagging() throws Throwable {

        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                if (squareState(x, y) >= 1) {
                   int number = squareState[x][y];

                   if (number == countFreeSquares(squareState, x, y)) {
                       // FANCY MATH
                       for (int yy = 0; yy < boardHeight; yy++) {
                           for (int xx = 0; xx <boardWidth; xx++) {
                               if (Math.abs(yy-y) <= 1 && Math.abs(xx-x) <= 1) {
                                   if (squareState(xx, yy) == -1 && !bombSquare[xx][yy]) {
                                       bombSquare[xx][yy] = true;
                                       flagSquare(xx, yy);
                                       System.out.println("FLAGGING SQURE!");
                                   }
                               }
                           }
                       }
                   }
                }
            }
        }
    }

    static int updateBoard() {
        BufferedImage img = fullScreenImage();

        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                int cell = getValue(img, x, y);
                if (cell == -420) return cell;
                squareState[x][y] = cell;

                if (cell == -3 || bombSquare[x][y]) {
                    bombSquare[x][y] = true;
                    squareState[x][y] = -1000;
                }
            }
        }
        dumpBoard();
        return -1;
    }

    static int getValue(BufferedImage img, int x, int y) {

        int mouseX = boardTopX + (int)(x * boardPixels);
        int mouseY = boardTopY + (int)(y * boardPixels);

        // Look for a 15x15 area
        int[] areaPixels = new int[255];
        int cnt = 0;

        for (int xx = mouseX-7; xx <= mouseX+7; xx++) {
            for (int yy = mouseY-7; yy <= mouseY+7; yy++) {
                areaPixels[cnt] = img.getRGB(xx, yy);
                cnt++;
            }
        }

        boolean isBlank = false;
        boolean colorOfOne = false;
        boolean relativelyBlank = true;

        for (int rgb : areaPixels) {
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            if (colDiff(red, green, blue, 110, 110, 110) < 20) return -1000;
            if (colDiff(red, green, blue, 255, 0,0) < 30) return -3;
            if (colDiff(red, green, blue, 65, 79, 188) < 10) colorOfOne = true;

            if (blue > red && blue > green && colDiff(red, green, blue, 220, 220, 255) < 200) {
                isBlank = true;
            }

            if (colDiff(red, green, blue, 167, 3, 5) < 20) return check3or7(areaPixels);
            if (colDiff(red, green, blue, 29,103,4) < 20) return 2;
            if (colDiff(red, green, blue, 0,0,138) < 20) return 4;
            if (colDiff(red, green, blue, 124,1,3) < 20) return 5;
            if (colDiff(red, green, blue, 7,122,131) < 20) return 6;
        }

        int rgb00 = areaPixels[0];
        int red00 = (rgb00 >> 16) & 0xFF;
        int green00 = (rgb00 >> 8) & 0xFF;
        int blue00 = rgb00 & 0xFF;
        for(int rgb : areaPixels){
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;
            if(colDiff(red, green, blue, red00, green00, blue00) > 60){
                relativelyBlank = false;
                break;
            }
        }

        if(colorOfOne && isBlank)
                return 1;

        if(isBlank && relativelyBlank)
                return 0;

        return -1;
    }

    static void dumpBoard() {
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                int number = squareState(x, y);
                if (bombSquare[x][y]) System.out.print(".");
                else if (number >= 1) System.out.print(number);
                else if (number == 0) System.out.print(" ");
                else System.out.print("#");
            }
            System.out.println();
        }
        System.out.println();
    }

    static int check3or7(int[] areaPixels) {
        boolean[][] redx = new boolean[15][15];
        for(int k=0; k<225; k++) {
            int i = k % 15;
            int j = k / 15;
            int rgb = areaPixels[k];
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

            if (colDiff(red, green, blue, 170, 0, 0) < 100)
                redx[i][j] = true;
        }

        for(int i = 0; i < 13; i++){
            for(int j = 0; j < 13; j++){
                if(!redx[i][j] && !redx[i][j+1] && !redx[i][j+2] && redx[i+1][j+1])
                    return 3;
            }
        }
        return 7;
    }

    static int colDiff(int r1, int g1, int b1, int r2, int g2, int b2) {
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }


    static void flagSquare(int x, int y) throws Throwable {
        int mouseX = boardTopX * (int)(x*boardPixels);
        int mouseY = boardTopY * (int)(y*boardPixels);
        moveMouse(mouseX, mouseY);

        robot.mousePress(4);
        Thread.sleep(5);
        robot.mouseRelease(4);
        Thread.sleep(5);
    }

    static int countFreeSquares(int[][] board, int x, int y) {
        int free = 0;

        if(squareState[x-1][y+1] == -1) free++;
        if(squareState[x-1][y] == -1) free++;
        if(squareState[x-1][y-1] == -1) free++;
        if(squareState[x][y+1] == -1) free++;
        if(squareState[x][y-1] == -1) free++;
        if(squareState[x+1][y+1] == -1) free++;
        if(squareState[x+1][y] == -1) free++;
        if(squareState[x+1][y-1] == -1) free++;

        return free;
    }

    static void clickFirst() throws Throwable {
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

    // Create screenshot and determine board position (Inspired by luckyToilet)
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
