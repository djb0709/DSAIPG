package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.Optional;

/**
 * Gomoku MCTS Optimization Test
 */
public class GomokuTest {

    // Test parameters
    private static final int GAMES_TO_RUN = 10;

    public static void main(String[] args) {
        System.out.println("===== Optimized MCTS vs Random Play =====");
        System.out.println("Running " + GAMES_TO_RUN + " games to test optimization effects");

        int mctsWins = 0;
        int randomWins = 0;
        int draws = 0;

        long startTime = System.currentTimeMillis();

        // Run multiple games
        for (int i = 0; i < GAMES_TO_RUN; i++) {
            System.out.println("\nStarting Game " + (i+1));

            Gomoku game = new Gomoku(i); // Use different seeds
            State<Gomoku> finalState = runGame(game);

            // Record results
            Optional<Integer> winner = finalState.winner();
            if (winner.isPresent()) {
                if (winner.get() == Gomoku.BLACK) {
                    mctsWins++;
                    System.out.println("Result: MCTS(Black) wins!");
                } else {
                    randomWins++;
                    System.out.println("Result: Random(White) wins!");
                }
            } else {
                draws++;
                System.out.println("Result: Draw!");
            }
        }

        long endTime = System.currentTimeMillis();

        // Print statistics
        System.out.println("\n===== Test Results =====");
        System.out.println("Total games: " + GAMES_TO_RUN);
        System.out.println("MCTS wins: " + mctsWins + " (" + (mctsWins * 100.0 / GAMES_TO_RUN) + "%)");
        System.out.println("Random wins: " + randomWins + " (" + (randomWins * 100.0 / GAMES_TO_RUN) + "%)");
        System.out.println("Draws: " + draws + " (" + (draws * 100.0 / GAMES_TO_RUN) + "%)");
        System.out.println("Total time: " + (endTime - startTime) / 1000.0 + " seconds");
    }

    /**
     * Run a game
     */
    private static State<Gomoku> runGame(Gomoku game) {
        State<Gomoku> currentState = game.start();
        int currentPlayer = game.opener();

        while (!currentState.isTerminal()) {
            Move<Gomoku> bestMove;

            if (currentPlayer == Gomoku.BLACK) {// MCTS player could change either white or black
                // MCTS player
                Node<Gomoku> rootNode = new GomokuNode(currentState);
                GomokuMCTS mcts = new GomokuMCTS(rootNode);
                bestMove = mcts.findBestMove();
            } else {
                // Random player
                bestMove = currentState.chooseMove(currentPlayer);
            }

            // Apply move
            currentState = currentState.next(bestMove);

            // Print current state
            System.out.println(((Gomoku.GomokuState) currentState).position().render());

            // Switch player
            currentPlayer = 1 - currentPlayer;
        }

        return currentState;
    }
}