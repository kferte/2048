import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

public class GameBoard {

    public static final int ROWS = 4;
    public static final int COLUMNS = 4;

    private final int startingTiles = 2;
    private Tile[][] board;
    private boolean dead;
    private boolean won;
    private boolean hasStarted;
    private BufferedImage gameBoard;
    private BufferedImage finalBoard;
    private int x;
    private int y;

    private static int SPACING = 5;
    public static int BOARD_WIDTH = (COLUMNS + 1) * SPACING + COLUMNS * Tile.WIDTH;
    public static int BOARD_HEIGHT = (ROWS + 1) * SPACING + ROWS * Tile.HEIGHT;

    public GameBoard(int x, int y){
        this.x = x;
        this.y = y;
        board = new Tile[ROWS][COLUMNS];
        gameBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        finalBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);

        createBoardImage();
        start();
    }

    private void createBoardImage() {
        Graphics2D g = (Graphics2D) gameBoard.getGraphics();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        g.setColor(Color.LIGHT_GRAY);

        for(int row = 0; row < ROWS; row++){
            for(int col = 0; col < COLUMNS; col++){
                int x = SPACING + SPACING * col + Tile.WIDTH * col;
                int y = SPACING + SPACING * row + Tile.HEIGHT * row;
                g.fillRoundRect(x, y, Tile.WIDTH, Tile.HEIGHT, Tile.ARC_WIDTH, Tile.ARC_HEIGHT);
            }
        }
    }

    private void start(){
        for(int i = 0; i < startingTiles; i++){
            spawnRandom();
        }
    }

    private void spawnRandom(){
        Random random = new Random();
        boolean notValid = true;

        while (notValid){
            int location = random.nextInt(ROWS * COLUMNS);
            int row = location / ROWS;
            int col = location % COLUMNS;
            Tile current = board[row][col];
            if(current == null){
                int value = random.nextInt(10) < 9 ? 2 : 4;
                Tile tile = new Tile(value, getTileX(col), getTileY(row));
                board[row][col] = tile;
                notValid = false;
            }
        }
    }

    public int getTileX(int col){
        return SPACING + col * Tile.WIDTH + col * SPACING;
    }

    public int getTileY(int row){
        return SPACING + row * Tile.HEIGHT + row * SPACING;
    }

    public void render(Graphics2D g){
        Graphics2D g2d = (Graphics2D)finalBoard.getGraphics();
        g2d.drawImage(gameBoard, 0 ,0, null);
        for(int row = 0; row < ROWS; row++){
            for(int col = 0; col < COLUMNS; col++){
                Tile current = board[row][col];
                if(current == null) continue;
                current.render(g2d);
            }
        }
        g.drawImage(finalBoard, x, y, null);
        g2d.dispose();
    }

    public void update(){
        checkKeys();
        for(int row = 0; row < ROWS; row++){
            for(int col = 0; col < COLUMNS; col++){
                Tile current = board[row][col];
                if(current == null) continue;
                current.update();
                resetPosition(current, row, col);
                if(current.getValue() == 2048)
                    won = true;
            }
        }
    }

    private void resetPosition(Tile current, int row, int col){
        if(current == null) return;
        int x = getTileX(col);
        int y = getTileY(row);
        int distX = current.getX() - x;
        int distY = current.getY() - y;
        if(Math.abs(distX) < Tile.SLIDE_SPEED){
            current.setX(current.getX() - distX);
        }
        if(Math.abs(distY) < Tile.SLIDE_SPEED){
            current.setY(current.getY() - distY);
        }
        if(distX < 0){
            current.setX(current.getX() + Tile.SLIDE_SPEED);
        }
        if(distY < 0){
            current.setY(current.getY() + Tile.SLIDE_SPEED);
        }
        if(distX > 0){
            current.setX(current.getX() - Tile.SLIDE_SPEED);
        }
        if(distY > 0){
            current.setY(current.getY() - Tile.SLIDE_SPEED);
        }
    }

    private boolean move(int row, int col, int horizontalDirection, int verticalDirection, Direction dir){
        boolean canMove = false;
        Tile current = board[row][col];
        if(current == null) return false;
        boolean move = true;
        int newColumn = col;
        int newRow = row;
        while (move){
            newColumn += horizontalDirection;
            newRow += verticalDirection;
            if(checkOutOfBounds(dir, newRow, newColumn)) break;
            if(board[newRow][newColumn] == null){
                board[newRow][newColumn] = current;
                board[newRow - verticalDirection][newColumn - horizontalDirection] = null;
                board[newRow][newColumn].setSlideTo(new Point(newRow, newColumn));
                canMove = true;
            } else if(board[newRow][newColumn].getValue() == current.getValue() && board[newRow][newColumn].CanCombine()){
                board[newRow][newColumn].setCanCombine(false);
                board[newRow][newColumn].setValue(board[newRow][newColumn].getValue() * 2);
                canMove = true;
                board[newRow - verticalDirection][newColumn - horizontalDirection] = null;
                board[newRow][newColumn].setSlideTo(new Point(newRow, newColumn));
                //board[newRow][newColumn].setCombineAnimation(true);
                //add to score
            } else {
                move = false;
            }
        }
        return canMove;
    }

    private boolean checkOutOfBounds(Direction dir, int row, int col) {
        if(dir == Direction.LEFT){
            return col < 0;
        } else if(dir == Direction.RIGHT){
            return col > COLUMNS - 1;
        } else if(dir == Direction.UP){
            return row < 0;
        } else if(dir == Direction.DOWN){
            return row > ROWS - 1;
        }
        return false;
    }

    private void moveTiles(Direction dir){
        boolean canMove = false;
        int horizontalDirection = 0;
        int verticalDirection = 0;

        if(dir == Direction.LEFT){
            horizontalDirection = -1;
            for(int row = 0; row < ROWS; row++){
                for(int col = 0; col < COLUMNS; col++){
                    if(!canMove){
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                    else move(row, col, horizontalDirection, verticalDirection, dir);
                }
            }
        } else if(dir == Direction.RIGHT){
            horizontalDirection = 1;
            for(int row = 0; row < ROWS; row++){
                for(int col = COLUMNS - 1; col >= 0; col--){
                    if(!canMove){
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                    else move(row, col, horizontalDirection, verticalDirection, dir);
                }
            }
        } else if(dir == Direction.UP){
            verticalDirection = -1;
            for(int row = 0; row < ROWS; row++){
                for(int col = 0; col < COLUMNS; col++){
                    if(!canMove){
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                    else move(row, col, horizontalDirection, verticalDirection, dir);
                }
            }
        } else if(dir == Direction.DOWN){
            verticalDirection = 1;
            for(int row = ROWS - 1; row >= 0; row--){
                for(int col = 0; col < COLUMNS; col++){
                    if(!canMove){
                        canMove = move(row, col, horizontalDirection, verticalDirection, dir);
                    }
                    else move(row, col, horizontalDirection, verticalDirection, dir);
                }
            }
        } else {
            System.out.println(dir + " is not a valid direction.");
        }

        for(int row = 0; row < ROWS; row++){
            for(int col = 0; col < COLUMNS; col++){
                Tile current = board[row][col];
                if(current == null) continue;
                current.setCanCombine(true);
            }
        }

        if(canMove){
            spawnRandom();
            checkDead();
        }
    }

    private void checkDead(){
        for(int row = 0; row < ROWS; row++){
            for (int col = 0; col < COLUMNS; col++){
                if(board[row][col] == null) return;
                if(checkSurroundingTiles(row, col, board[row][col])){
                    return;
                }
            }
        }

        dead = true;
        //setHighScore(score);
    }

    private boolean checkSurroundingTiles(int row, int col, Tile current){
        if(row > 0){
            Tile check = board[row - 1][col];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        } else if(row < ROWS - 1){
            Tile check = board[row + 1][col];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        } else if(col > 0){
            Tile check = board[row][col - 1];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        } else if(row < COLUMNS - 1){
            Tile check = board[row][col + 1];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        return false;
    }

    private void checkKeys() {
        if(Keyboard.typed(KeyEvent.VK_LEFT)){
            moveTiles(Direction.LEFT);
            if(!hasStarted) hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_RIGHT)){
            moveTiles(Direction.RIGHT);
            if(!hasStarted) hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_UP)){
            moveTiles(Direction.UP);
            if(!hasStarted) hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_DOWN)){
            moveTiles(Direction.DOWN);
            if(!hasStarted) hasStarted = true;
        }
    }
}
