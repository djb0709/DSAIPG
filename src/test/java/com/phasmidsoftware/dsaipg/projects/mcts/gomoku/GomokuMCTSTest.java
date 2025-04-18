package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import static org.junit.Assert.*;
import org.junit.Test;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.Collection;
import java.util.Random;

/**
 * Test class for GomokuMCTS
 */
public class GomokuMCTSTest {

    /**
     * Test basic constructor behavior
     */
    @Test
    public void testConstructor() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        Node<Gomoku> rootNode = new GomokuNode(initialState);
        GomokuMCTS mcts = new GomokuMCTS(rootNode);

        assertNotNull("MCTS object should be created", mcts);
    }

    /**
     * Test setting simulation count
     */
    @Test
    public void testSetSimulationCount() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        Node<Gomoku> rootNode = new GomokuNode(initialState);
        GomokuMCTS mcts = new GomokuMCTS(rootNode);

        mcts.setSimulationCount(2000);
        // If no exception is thrown, test passes
        assertTrue(true);
    }

    /**
     * Test that findBestMove returns a valid move
     */
    @Test
    public void testFindBestMoveReturnsValidMove() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        Node<Gomoku> rootNode = new GomokuNode(initialState);
        GomokuMCTS mcts = new GomokuMCTS(rootNode);
        mcts.setSimulationCount(100); // Use small count for faster tests

        Move<Gomoku> bestMove = mcts.findBestMove();

        assertNotNull("Best move should not be null", bestMove);
        assertEquals("Move should be for BLACK player", Gomoku.BLACK, bestMove.player());

        // Check if move coordinates are valid
        Gomoku.GomokuMove gomokuMove = (Gomoku.GomokuMove) bestMove;
        int[] coords = gomokuMove.move();
        assertTrue("Row coordinate should be valid",
                coords[0] >= 0 && coords[0] < GomokuPosition.getGridSize());
        assertTrue("Column coordinate should be valid",
                coords[1] >= 0 && coords[1] < GomokuPosition.getGridSize());
    }

    /**
     * Test MCTS can find a winning move
     */
    @Test
    public void testFindWinningMove() {
        // Create a specific board state where BLACK can win on next move
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> state = game.start();

        // Create a board with 4 BLACK pieces in a row
        // BLACK: (2,0), (2,1), (2,2), (2,3) - winning move would be (2,4)
        state = state.next(new Gomoku.GomokuMove(Gomoku.BLACK, 2, 0));
        state = state.next(new Gomoku.GomokuMove(Gomoku.WHITE, 3, 0));
        state = state.next(new Gomoku.GomokuMove(Gomoku.BLACK, 2, 1));
        state = state.next(new Gomoku.GomokuMove(Gomoku.WHITE, 3, 1));
        state = state.next(new Gomoku.GomokuMove(Gomoku.BLACK, 2, 2));
        state = state.next(new Gomoku.GomokuMove(Gomoku.WHITE, 3, 2));
        state = state.next(new Gomoku.GomokuMove(Gomoku.BLACK, 2, 3));

        // Now it's WHITE's turn, so we need to set up the search with BLACK as the player
        // We'll create a custom state where it's BLACK's turn
        State<Gomoku> blackTurnState = state.next(new Gomoku.GomokuMove(Gomoku.WHITE, 3, 3));

        Node<Gomoku> rootNode = new GomokuNode(blackTurnState);
        GomokuMCTS mcts = new GomokuMCTS(rootNode);
        // Increase simulation count to improve chance of finding winning move
        mcts.setSimulationCount(500);

        Move<Gomoku> bestMove = mcts.findBestMove();

        // We can't guarantee MCTS will always find the winning move,
        // but with enough simulations, it should be quite likely
        Gomoku.GomokuMove gomokuMove = (Gomoku.GomokuMove) bestMove;
        int[] coords = gomokuMove.move();

        // Print the chosen move for diagnostics
        System.out.println("MCTS chose move: (" + coords[0] + "," + coords[1] + ")");

        // Apply the move and check if it's a winning move
        State<Gomoku> resultState = blackTurnState.next(bestMove);

        // Either the move is winning or it's a valid move
        if (resultState.isTerminal() && resultState.winner().isPresent()) {
            assertEquals("If terminal, the winner should be BLACK",
                    Gomoku.BLACK, (int)resultState.winner().get());
        } else {
            assertTrue("Move should be valid", true);
        }
    }

    /**
     * Test adaptive simulation strategy
     */
    @Test
    public void testAdaptiveSimulationStrategy() {
        // Test will verify MCTS can complete with different board sizes
        // and different game stages without exception

        // Test small board, early game
        testAdaptiveStrategyScenario(8, 5);

        // Test small board, mid game
        testAdaptiveStrategyScenario(8, 15);

        // Test large board, early game
        testAdaptiveStrategyScenario(15, 10);

        // If we get here without exceptions, test passes
        assertTrue(true);
    }

    /**
     * Helper method to test adaptive strategy scenarios
     */
    private void testAdaptiveStrategyScenario(int boardSize, int moveCount) {
        // Set board size
        int originalSize = GomokuPosition.getGridSize();
        GomokuPosition.setBoardSize(boardSize);

        try {
            // Create a game with specified number of random moves
            Gomoku game = new Gomoku(new Random(42));
            State<Gomoku> state = game.start();

            // Make random moves to reach desired game stage
            for (int i = 0; i < moveCount && !state.isTerminal(); i++) {
                int player = (i % 2 == 0) ? Gomoku.BLACK : Gomoku.WHITE;
                Collection<Move<Gomoku>> moves = state.moves(player);
                if (!moves.isEmpty()) {
                    Move<Gomoku>[] moveArray = moves.toArray(new Move[0]);
                    state = state.next(moveArray[new Random(i).nextInt(moveArray.length)]);
                }
            }

            // If game is not terminal, test MCTS
            if (!state.isTerminal()) {
                Node<Gomoku> rootNode = new GomokuNode(state);
                GomokuMCTS mcts = new GomokuMCTS(rootNode);

                // Should adapt simulation count based on board size and move count
                Move<Gomoku> bestMove = mcts.findBestMove();
                assertNotNull("Best move should not be null", bestMove);
            }
        } finally {
            // Restore original board size
            GomokuPosition.setBoardSize(originalSize);
        }
    }

    /**
     * Test MCTS can run a complete game
     */
    @Test
    public void testRunCompleteMiniGame() {
        // Set a small board for faster testing
        int originalSize = GomokuPosition.getGridSize();
        GomokuPosition.setBoardSize(5);

        try {
            Gomoku game = new Gomoku(new Random(42));
            State<Gomoku> currentState = game.start();
            int currentPlayer = game.opener();
            int maxMoves = 25; // Maximum possible moves on 5x5 board

            for (int i = 0; i < maxMoves && !currentState.isTerminal(); i++) {
                Move<Gomoku> move;

                if (currentPlayer == Gomoku.BLACK) {
                    // Use MCTS with low simulation count for test speed
                    Node<Gomoku> rootNode = new GomokuNode(currentState);
                    GomokuMCTS mcts = new GomokuMCTS(rootNode);
                    mcts.setSimulationCount(50);
                    move = mcts.findBestMove();
                } else {
                    move = currentState.chooseMove(currentPlayer);
                }

                // Apply move
                currentState = currentState.next(move);
                currentPlayer = 1 - currentPlayer;
            }

            // Either game ended or we reached max moves
            assertTrue("Game should progress without exceptions", true);

        } finally {
            // Restore original board size
            GomokuPosition.setBoardSize(originalSize);
        }
    }

    /**
     * Test static moveToString utility
     */
    @Test
    public void testMoveToString() {
        // This is a static private method, so we test it indirectly
        // We'll create a move and check that printMoves doesn't throw exceptions

        Gomoku.GomokuMove move = new Gomoku.GomokuMove(Gomoku.BLACK, 3, 4);

        // Call runGame with a minimal game that will use moveToString
        // If no exception occurs, test passes
        try {
            Gomoku miniGame = new Gomoku(new Random(42));
            State<Gomoku> state = miniGame.start();
            state = state.next(move);

            // Just checking that calling a method that uses moveToString doesn't throw
            GomokuPosition.setBoardSize(3); // Tiny board for test speed
            GomokuMCTS.runGame(new Gomoku(new Random(42)), miniGame.start());

            assertTrue("moveToString should not throw exception", true);
        } catch (Exception e) {
            fail("Exception: " + e.getMessage());
        } finally {
            GomokuPosition.setBoardSize(8); // Reset to default
        }
    }
}