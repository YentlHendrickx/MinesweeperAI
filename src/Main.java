import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

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
    static Random random = new Random();

    public static void main(String[] args) throws Throwable{
        robot = new Robot();
        calibrateParameters();

        // Check for failed calibration
        if (boardWidth < 8 || boardWidth > 30 || boardHeight < 8 || boardHeight > 30) {
            System.out.println("Failed calibration process!");
            return;
        }

        calibratedImage();

        // Init 2d array
        squareState = new int[boardWidth][boardHeight];
        bombSquare = new boolean[boardWidth][boardHeight];
        for (int i = 0; i < boardWidth; i++) for (int j = 0; j < boardHeight; j++) bombSquare[i][j] = false;

        // No mines to start with so set array
        clickFirst();

//        while (true) {
//            updateBoard();
//            if (System.in.read() > 0) {
//                System.out.println("Next Iteration");
//            }
//        }
        for (int g = 0; g < 20; g++) {
            int status = updateBoard();
            if (!checkConsistency()) {
                robot.mouseMove(0,0);
                status = updateBoard();
                robot.mouseMove(mouseLocationX, mouseLocationY);
                if (status == - 1000) exit();
                continue;
            }
            if (status == -1000) exit();
                tryFlagging();
            updateBoard();
            makeMove();
        }
    }

    public static void exit() {
        System.exit(0);
    }

    static boolean checkConsistency(){
        for (int y = 0; y<boardHeight; y++){
            for (int x = 0; x < boardWidth; x++){

                int freeSquares = countFreeSquares( x, y);
                int numFlags = countMines(bombSquare, x, y);

                if (squareState(x,y) == 0 && freeSquares > 0){
                    return false;
                }
                if ((squareState(x,y) - numFlags) > 0 && freeSquares == 0){
                    return false;
                }

            }
        }

        return true;
    }


    static int squareState(int x, int y) {
        if (x < 0 || y < 0 || x > boardWidth - 1 || y > boardHeight - 1) return -10;
        return squareState[x][y];
    }

    static void makeMove() throws Throwable{
        boolean success = false;

        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                if (squareState(x, y) >= 1) {
                    // Count number of mines surrounding
                    int num = squareState(x, y);

                    int mineCnt = countMines(bombSquare, x, y);
                    int freeSquares = countFreeSquares( x,y);

                    // Click on non-mine square
                    if (num == mineCnt && freeSquares > mineCnt) {
//                        System.out.println("x: " + x + " y: " + y + " has: " + mineCnt + " mines and " + freeSquares + " free");


                        // Chord
                        if (freeSquares - mineCnt > 1) {
                            chord(x, y);
                            squareState[x][y] = 0;
                            success = true;
                            continue;
                        }

                        for (int yy = 0; yy < boardHeight; yy++) {
                            for (int xx = 0; xx < boardWidth; xx++) {
                                if (Math.abs(yy-y)<=1 && Math.abs(xx-x)<=1) {
                                    if (squareState(xx, yy) == -1 && !bombSquare[xx][yy]) {
                                        clickPosition(yy,xx);
                                        squareState[xx][yy] = 0;
                                        success = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
//        System.out.println(success);
        if (success) return;
        System.out.println("RANDOM GUESS");
        randomGuess();
    }

    static void chord(int x, int y) throws  Throwable {
        int mouseX = boardTopX + (int)(x*boardPixels);
        int mouseY = boardTopY + (int)(y*boardPixels);
        moveMouse(mouseX, mouseY);

        robot.mousePress(4);
        robot.mousePress(16);
        Thread.sleep(5);
        robot.mouseRelease(4);
        robot.mouseRelease(16);
        Thread.sleep(5);
    }

    static int countMines(boolean[][] flag, int x, int y) {
        int mines = 0;

        boolean upperEdge = false, lowerEdge = false, leftEdge = false, rightEdge = false;

        if (x == 0) leftEdge = true;
        if (x == boardWidth - 1) rightEdge = true;

        if (y == 0) upperEdge = true;
        if (y == boardHeight - 1) lowerEdge = true;

        // Upper row
        if (!upperEdge && !leftEdge && flag[x-1][y-1]) mines++;
        if (!upperEdge && flag[x][y-1]) mines++;
        if (!upperEdge && !rightEdge && flag[x+1][y-1]) mines++;
        // Center row
        if (!leftEdge && flag[x-1][y]) mines++;
        if (!rightEdge && flag[x+1][y]) mines++;
        // Lower row
        if (!lowerEdge && !leftEdge && flag[x-1][y+1]) mines++;
        if (!lowerEdge && flag[x][y+1]) mines++;
        if (!lowerEdge && !rightEdge && flag[x+1][y+1]) mines++;

        return mines;
    }

    static void randomGuess() throws Throwable {
      while(true) {
          int k = random.nextInt(boardWidth * boardHeight);
          int x = k / boardWidth;
          int y = k % boardWidth;


          if (squareState(x, y) == -1 && !bombSquare[x][y]) {
              System.out.println(x + " " + y);
              clickPosition(y, x);
              return;
          }
      }
    }

    static void tryFlagging() throws Throwable {
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                if (squareState(x, y) >= 1) {
                   int number = squareState(x,y);

                   if (number == countFreeSquares(x, y)) {
                       // FANCY MATH
                       for (int yy = 0; yy < boardHeight; yy++) {
                           for (int xx = 0; xx <boardWidth; xx++) {
                               if (Math.abs(yy-y) <= 1 && Math.abs(xx-x) <= 1) {
                                   if (squareState(xx, yy) == -1 && !bombSquare[xx][yy]) {
                                           bombSquare[xx][yy] = true;
                                           flagSquare(xx, yy);
//                                           System.out.println("FLAGGING SQUARE!");
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

                if (cell == -1000) return cell;
                squareState[x][y] = cell;

                if (cell == -3 || bombSquare[x][y]) {
                    bombSquare[x][y] = true;
                    squareState[x][y] = -1;
                }
                if (cell == -1) {
                    bombSquare[x][y] = false;
                }
            }
        }
//        dumpBoard();

        return -1;
    }

    static int getValue(BufferedImage img, int x, int y) {

        int mouseX = boardTopX + (int)(x * boardPixels);
        int mouseY = boardTopY + (int)(y * boardPixels);

        // Look for a 13x13 area
        int[] areaPixels = new int[169];
        int cnt = 0;

        for (int xx = mouseX-6; xx <= mouseX+6; xx++) {
            for (int yy = mouseY-6; yy <= mouseY+6; yy++) {
                areaPixels[cnt] = img.getRGB(xx, yy);
                cnt++;
            }
        }

        boolean isFullBlank = false;
        boolean isPartialBlank = false;
        boolean checkFail = true;

        // Ambiguity flag; 5, 3 and flag all have the same red tones incorporated
        boolean flagAmbiguity = false;

        for (int rgb : areaPixels) {
            int red = (rgb >> 16) & 0xFF;
            int green = (rgb >> 8) & 0xFF;
            int blue = rgb & 0xFF;

//            if (colDiff(red, green, blue, 0, 0, 0) < 20) checkFail = true;
            if (colDiff(red, green, blue, 128, 0, 0) < 10) flagAmbiguity = true;
            if (colDiff(red, green, blue, 192, 192, 192) < 10) {
                isPartialBlank = true;
            }

            if (colDiff(red, green, blue, 255, 0, 0) < 10) flagAmbiguity = true;
            if (colDiff(red, green, blue, 0, 0, 255) < 20) return 1;
            if (colDiff(red, green, blue, 0,128,0) < 20) return 2;
            if (colDiff(red, green, blue, 0,0,138) < 20) return 4;

            if (colDiff(red, green, blue, 128,0,0) < 20) flagAmbiguity = true;
            if (colDiff(red, green, blue, 7,122,131) < 30) return 6;
        }

        // Get average pixel values
        int avg = 0;
        if (flagAmbiguity || isPartialBlank || checkFail) {
            int r1 = 0;
            int g1 = 0;
            int b1 = 0;
            for (int rgb : areaPixels) {
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                r1 += red;
                g1 +=green;
                b1 +=blue;
            }
            avg = (r1 + g1 + b1) /3;
        }
//        if (flagAmbiguity) {
//            System.out.println(avg);
//        }

        if (isPartialBlank) {
            if (avg > 30000) {
                isFullBlank = true;
            }
        }

        if (checkFail) {
//            System.out.println(avg);
            if (avg < 20000) {
                return -1000;
            }
        }

        if (flagAmbiguity) {
//            System.out.println(avg);
            if (avg < 25000) {
                return -3;
            } else if (avg < 27500) {
                return 5;
            } else {
                return 3;
            }
        }

        if(isFullBlank && isPartialBlank)
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

    static int colDiff(int r1, int g1, int b1, int r2, int g2, int b2) {
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }


    static void flagSquare(int x, int y) throws Throwable {
        int mouseX = boardTopX + (int)(x*boardPixels);
        int mouseY = boardTopY + (int)(y*boardPixels);
        moveMouse(mouseX, mouseY);
//System.out.println("moving to: " + mouseX + "," + mouseY);
        robot.mousePress(4);
        Thread.sleep(5);
        robot.mouseRelease(4);
        Thread.sleep(5);
    }

    static int countFreeSquares(int x, int y) {
        int free = 0;

        if(squareState(x-1,y+1) == -1) free++;
        if(squareState(x-1, y) == -1) free++;
        if(squareState(x-1, y-1) == -1) free++;
        if(squareState(x, y+1) == -1) free++;
        if(squareState(x, y-1) == -1) free++;
        if(squareState(x+1, y+1) == -1) free++;
        if(squareState(x+1, y) == -1) free++;
        if(squareState(x+1, y-1) == -1) free++;

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
        Thread.sleep(5);
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
//        boardPixels = 0.5*((double)(lastX - firstX) / (double)(boardHeight - 2))
//                + 0.5*((double)(lastY - firstY) / (double)(boardHeight - 2));
        boardPixels = 16;

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
        int steps = (distance / 4) / 5;

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
