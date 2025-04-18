package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.List;
import java.util.Optional;

/**
 * Test class for GomokuPosition
 */
public class GomokuPositionTest {

    private int originalSize;

    @Before
    public void setUp() {
        originalSize = GomokuPosition.getGridSize();
    }

    @After
    public void tearDown() {
        GomokuPosition.setBoardSize(originalSize);
    }

    /**
     * Test starting position creation
     */
    @Test
    public void testStartingPosition() {
        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        assertEquals("Last player should be EMPTY", Gomoku.EMPTY, position.last);
        assertEquals("Count should be 0", 0, position.getCount());
        assertFalse("Should not have a winner", position.winner().isPresent());
    }

    /**
     * Test setting board size
     */
    @Test
    public void testSetBoardSize() {
        int testSize = 10;
        GomokuPosition.setBoardSize(testSize);

        assertEquals("Board size should be updated", testSize, GomokuPosition.getGridSize());

        // Create a new position with the updated board size
        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // First move should be at the center
        List<int[]> firstMoves = position.moves(Gomoku.BLACK);
        assertEquals("Should have 1 move", 1, firstMoves.size());
        assertArrayEquals("Should be center position", new int[]{testSize/2, testSize/2}, firstMoves.get(0));
    }

    /**
     * Test making moves
     */
    @Test
    public void testMove() {
        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // First move is Black at center
        position = position.move(Gomoku.BLACK, GomokuPosition.getGridSize()/2, GomokuPosition.getGridSize()/2);
        assertEquals("Last player should be BLACK", Gomoku.BLACK, position.last);
        assertEquals("Count should be 1", 1, position.getCount());

        // Second move is White
        position = position.move(Gomoku.WHITE, GomokuPosition.getGridSize()/2 - 1, GomokuPosition.getGridSize()/2);
        assertEquals("Last player should be WHITE", Gomoku.WHITE, position.last);
        assertEquals("Count should be 2", 2, position.getCount());

        try {
            // Try making a move on an occupied cell
            position.move(Gomoku.BLACK, GomokuPosition.getGridSize()/2 - 1, GomokuPosition.getGridSize()/2);
            fail("Should throw exception for move on occupied cell");
        } catch (RuntimeException e) {
            assertTrue("Exception message should mention occupied position",
                    e.getMessage().contains("occupied"));
        }

        try {
            // Try making a move by the same player
            position.move(Gomoku.WHITE, GomokuPosition.getGridSize()/2 - 2, GomokuPosition.getGridSize()/2);
            fail("Should throw exception for consecutive moves by same player");
        } catch (RuntimeException e) {
            assertTrue("Exception message should mention error with player",
                    e.getMessage().contains("error with"));
        }
    }

    /**
     * Test moves method for different board sizes and states
     */
    @Test
    public void testMoves() {
        // Test on a small board (8x8)
        GomokuPosition.setBoardSize(8);

        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // Initial board should return center position only
        List<int[]> initialMoves = position.moves(Gomoku.BLACK);
        assertEquals("Initial moves on 8x8 should be 1 (center)", 1, initialMoves.size());
        assertArrayEquals("Center should be (4,4)", new int[]{4, 4}, initialMoves.get(0));

        // Make a move at the center
        position = position.move(Gomoku.BLACK, 4, 4);

        // After first move, should return all empty positions for small board
        List<int[]> smallBoardMoves = position.moves(Gomoku.WHITE);
        assertEquals("Small board should return all empty positions",
                (8*8)-1, smallBoardMoves.size());  // 64 positions minus 1 that's occupied

        // Test on a large board (15x15)
        GomokuPosition.setBoardSize(15);

        position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // Initial large board should also return center only
        initialMoves = position.moves(Gomoku.BLACK);
        assertEquals("Initial moves on 15x15 should be 1 (center)", 1, initialMoves.size());
        assertArrayEquals("Center should be (7,7)", new int[]{7, 7}, initialMoves.get(0));

        // Make a move at the center
        position = position.move(Gomoku.BLACK, 7, 7);

        // After first move on large board, should return only neighboring positions
        List<int[]> largeBoardMoves = position.moves(Gomoku.WHITE);
        assertTrue("Large board should return only neighboring positions",
                largeBoardMoves.size() <= 8); // At most 8 surrounding positions
    }

    /**
     * Test winner detection with horizontal win pattern
     */
    @Test
    public void testWinnerHorizontal() {
        // Use a board size that can accommodate our test
        GomokuPosition.setBoardSize(10);

        // Create a sequence that leads to BLACK winning with 5 in a row horizontally
        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // Start alternating moves, with BLACK eventually getting 5 in a row
        // BLACK's first move at center
        position = position.move(Gomoku.BLACK, 5, 5);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 4, 4);

        // BLACK's 1st piece in winning line
        position = position.move(Gomoku.BLACK, 3, 3);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 2, 2);

        // BLACK's 2nd piece in winning line
        position = position.move(Gomoku.BLACK, 3, 4);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 2, 4);

        // BLACK's 3rd piece in winning line
        position = position.move(Gomoku.BLACK, 3, 5);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 2, 5);

        // BLACK's 4th piece in winning line
        position = position.move(Gomoku.BLACK, 3, 6);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 2, 6);

        // BLACK's 5th piece in winning line
        position = position.move(Gomoku.BLACK, 3, 7);

        // Check for winner
        Optional<Integer> winner = position.winner();
        assertTrue("Should have a winner", winner.isPresent());
        assertEquals("Winner should be BLACK", Gomoku.BLACK, (int)winner.get());
    }

    /**
     * Test winner detection with vertical win pattern
     */
    @Test
    public void testWinnerVertical() {
        // Use a board size that can accommodate our test
        GomokuPosition.setBoardSize(10);

        // Create a sequence that leads to WHITE winning with 5 in a row vertically
        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // BLACK's first move at center
        position = position.move(Gomoku.BLACK, 5, 5);

        // WHITE's 1st piece in winning line
        position = position.move(Gomoku.WHITE, 3, 3);

        // BLACK's move
        position = position.move(Gomoku.BLACK, 2, 2);

        // WHITE's 2nd piece in winning line
        position = position.move(Gomoku.WHITE, 4, 3);

        // BLACK's move
        position = position.move(Gomoku.BLACK, 2, 4);

        // WHITE's 3rd piece in winning line
        position = position.move(Gomoku.WHITE, 5, 3);

        // BLACK's move
        position = position.move(Gomoku.BLACK, 2, 5);

        // WHITE's 4th piece in winning line
        position = position.move(Gomoku.WHITE, 6, 3);

        // BLACK's move
        position = position.move(Gomoku.BLACK, 2, 6);

        // WHITE's 5th piece in winning line
        position = position.move(Gomoku.WHITE, 7, 3);

        // Check for winner
        Optional<Integer> winner = position.winner();
        assertTrue("Should have a winner", winner.isPresent());
        assertEquals("Winner should be WHITE", Gomoku.WHITE, (int)winner.get());
    }

    /**
     * Test winner detection with diagonal win pattern
     */
    @Test
    public void testWinnerDiagonal() {
        // Use a board size that can accommodate our test
        GomokuPosition.setBoardSize(10);

        // Create a sequence that leads to BLACK winning with 5 in a row diagonally
        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // BLACK's first move at center
        position = position.move(Gomoku.BLACK, 5, 5);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 1, 1);

        // BLACK's 1st piece in winning line
        position = position.move(Gomoku.BLACK, 2, 2);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 1, 2);

        // BLACK's 2nd piece in winning line
        position = position.move(Gomoku.BLACK, 3, 3);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 1, 3);

        // BLACK's 3rd piece in winning line
        position = position.move(Gomoku.BLACK, 4, 4);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 1, 4);

        // BLACK's 4th piece in winning line
        position = position.move(Gomoku.BLACK, 6, 6);

        // WHITE's move
        position = position.move(Gomoku.WHITE, 1, 5);

        // BLACK's 5th piece in winning line
        position = position.move(Gomoku.BLACK, 7, 7);

        // Check for winner
        Optional<Integer> winner = position.winner();
        assertTrue("Should have a winner", winner.isPresent());
        assertEquals("Winner should be BLACK", Gomoku.BLACK, (int)winner.get());
    }

    /**
     * Test winner detection with anti-diagonal win pattern
     */
    @Test
    public void testWinnerAntiDiagonal() {
        // Use a board size that can accommodate our test
        GomokuPosition.setBoardSize(10);

        // Create a sequence that leads to WHITE winning with 5 in a row anti-diagonally
        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // BLACK's first move at center
        position = position.move(Gomoku.BLACK, 5, 5);

        // WHITE's 1st piece in winning line
        position = position.move(Gomoku.WHITE, 7, 2);

        // BLACK's move
        position = position.move(Gomoku.BLACK, 1, 1);

        // WHITE's 2nd piece in winning line
        position = position.move(Gomoku.WHITE, 6, 3);

        // BLACK's move
        position = position.move(Gomoku.BLACK, 1, 2);

        // WHITE's 3rd piece in winning line
        position = position.move(Gomoku.WHITE, 5, 4);

        // BLACK's move
        position = position.move(Gomoku.BLACK, 1, 3);

        // WHITE's 4th piece in winning line
        position = position.move(Gomoku.WHITE, 4, 5);

        // BLACK's move
        position = position.move(Gomoku.BLACK, 1, 4);

        // WHITE's 5th piece in winning line
        position = position.move(Gomoku.WHITE, 3, 6);

        // Check for winner
        Optional<Integer> winner = position.winner();
        assertTrue("Should have a winner", winner.isPresent());
        assertEquals("Winner should be WHITE", Gomoku.WHITE, (int)winner.get());
    }

    /**
     * Test full board detection
     */
    @Test
    public void testFull() {
        // Use a tiny board for easier testing
        GomokuPosition.setBoardSize(2);

        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);
        assertFalse("Empty board should not be full", position.full());

        // Fill the board with alternating players
        position = position.move(Gomoku.BLACK, 1, 1);
        position = position.move(Gomoku.WHITE, 0, 0);
        position = position.move(Gomoku.BLACK, 0, 1);
        position = position.move(Gomoku.WHITE, 1, 0);

        assertTrue("Completely filled board should be full", position.full());
    }

    /**
     * Test rendering
     */
    @Test
    public void testRender() {
        // Use a small board for easier testing
        GomokuPosition.setBoardSize(3);

        GomokuPosition position = GomokuPosition.startingPosition(Gomoku.EMPTY);

        // Add some pieces with alternating players
        position = position.move(Gomoku.BLACK, 1, 1);
        position = position.move(Gomoku.WHITE, 0, 0);

        String rendered = position.render();

        // Check for expected characters in rendering
        assertTrue("Rendering should include column headers", rendered.contains("0 1 2"));
        assertTrue("Rendering should include 'X' for BLACK", rendered.contains("X"));
        assertTrue("Rendering should include 'O' for WHITE", rendered.contains("O"));
        assertTrue("Rendering should include '.' for empty", rendered.contains("."));
    }

    /**
     * Test equals and hashCode
     */
    @Test
    public void testEqualsAndHashCode() {
        GomokuPosition position1 = GomokuPosition.startingPosition(Gomoku.EMPTY);
        GomokuPosition position2 = GomokuPosition.startingPosition(Gomoku.EMPTY);

        assertEquals("Identical positions should be equal", position1, position2);
        assertEquals("Identical positions should have same hash code",
                position1.hashCode(), position2.hashCode());

        // Modify position1
        position1 = position1.move(Gomoku.BLACK, GomokuPosition.getGridSize()/2, GomokuPosition.getGridSize()/2);

        assertNotEquals("Different positions should not be equal", position1, position2);
    }
}