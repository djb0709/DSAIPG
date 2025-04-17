package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import java.util.*;

/**
 * checkerboard for Gomoku
 */
public class GomokuPosition {
    private final int[][] grid;
    final int last;  // last player
    private final int count;  // chess count on the board
    private  static int gridSize = 8;
    // board size now is 8x8

   // create a new board
    public static GomokuPosition startingPosition(final int last) {
        int[][] matrix = new int[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                matrix[i][j] = -1;  // all cells are empty
            }
        }
        return new GomokuPosition(matrix, 0, last);
    }
    /**
     * Set the board size
     */
    public static void setBoardSize(int size) {
        gridSize = size;
    }
    /**
     * moves for player
     */
    public GomokuPosition move(int player, int x, int y) {
        if (player == last) throw new RuntimeException("error with: " + player);
        int[][] matrix = copyGrid();
        if (matrix[x][y] < 0) {
            matrix[x][y] = player;
            return new GomokuPosition(matrix, count + 1, player);
        }
        throw new RuntimeException("the position is occupied: " + x + ", " + y);
    }

    /**
     * get all  the moves
     */
    public List<int[]> moves(int player) {
        if (player == last) throw new RuntimeException("error with : " + player);

        // optimation: if the board is empty, return the center
        if (count==0){
            List<int[]> result = new ArrayList<>();
            result.add(new int[]{(gridSize / 2), gridSize / 2});
            return result;
        }
        Set<String> validMoves = new HashSet<>();
        List<int[]> result = new ArrayList<>();

        //choose the location near the existing chess
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (grid[i][j] != -1) {
                    //check the 2x2 area around the chess
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            // skip the self
                            if (x == 0 && y == 0) continue;
                            int newX = i + x;
                            int newY = j + y;
                            if (newX >= 0 && newX < gridSize && newY >= 0 && newY < gridSize) {

                                String key = newX + "," + newY;
                                if (!validMoves.contains(key) && grid[newX][newY] < 0) {
                                    validMoves.add(key);
                                    result.add(new int[]{newX, newY});
                                }
                            }
                        }
                    }
                }
            }
        }


        return result;
    }

    /**
     * if there is a winner in current board
     */
    public Optional<Integer> winner() {
        if (count > 8 && fiveInARow()) return Optional.of(last);
        return Optional.empty();
    }

    /**
     * if there is a five in a board
     */
    boolean fiveInARow() {
        // rows
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j <= gridSize - 5; j++) {
                if (grid[i][j] == last && grid[i][j+1] == last && grid[i][j+2] == last
                        && grid[i][j+3] == last && grid[i][j+4] == last)
                    return true;
            }
        }

        // columns
        for (int i = 0; i <= gridSize - 5; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (grid[i][j] == last && grid[i+1][j] == last && grid[i+2][j] == last
                        && grid[i+3][j] == last && grid[i+4][j] == last)
                    return true;
            }
        }

        // diagonals from left to right
        for (int i = 0; i <= gridSize - 5; i++) {
            for (int j = 0; j <= gridSize - 5; j++) {
                if (grid[i][j] == last && grid[i+1][j+1] == last && grid[i+2][j+2] == last
                        && grid[i+3][j+3] == last && grid[i+4][j+4] == last)
                    return true;
            }
        }

        // diagonals from right to left
        for (int i = 0; i <= gridSize - 5; i++) {
            for (int j = 4; j < gridSize; j++) {
                if (grid[i][j] == last && grid[i+1][j-1] == last && grid[i+2][j-2] == last
                        && grid[i+3][j-3] == last && grid[i+4][j-4] == last)
                    return true;
            }
        }

        return false;
    }

    /**
     * if the board is full
     */
    boolean full() {
        return count == gridSize * gridSize;
    }

    /**
     * to string
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        // add column labels
        sb.append("  ");
        for (int j = 0; j < gridSize; j++) {
            sb.append(j).append(" ");
        }
        sb.append("\n");

        for (int i = 0; i < gridSize; i++) {
            sb.append(i).append(" ");
            // add row labels
            for (int j = 0; j < gridSize; j++) {
                // add cell
                sb.append(render(grid[i][j]));
                if (j < gridSize - 1) sb.append(" ");
            }
            if (i < gridSize - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * a cell render
     */
    private char render(int x) {
        return switch (x) {
            case 0 -> 'O';  // 白棋
            case 1 -> 'X';  // 黑棋
            default -> '.'; // 空位
        };
    }

    /**
     * copy
     */
    private int[][] copyGrid() {
        int[][] result = new int[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++)
            result[i] = Arrays.copyOf(grid[i], gridSize);
        return result;
    }

    public GomokuPosition(int[][] grid, int count, int last) {
        this.grid = grid;
        this.count = count;
        this.last = last;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GomokuPosition position)) return false;
        return Arrays.deepEquals(grid, position.grid);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(grid);
    }


    public static int getGridSize() {
        return gridSize;
    }


    public int getCount() {
        return count;
    }
}