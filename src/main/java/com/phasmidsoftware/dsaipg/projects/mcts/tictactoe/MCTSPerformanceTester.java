package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.Date;
import java.util.Optional;

/**
 * Simplified MCTS Performance Tester
 */
public class MCTSPerformanceTester {

    // Total games to run for each simulation count configuration
    private static final int GAMES_PER_CONFIG = 1000;

    // Simulation counts to test
    private static final int[] SIMULATION_COUNTS = {100, 1000, 10000};

    /**
     * Run a single game (MCTS vs Random)
     * @param simulationCount MCTS simulations per move
     * @return 0:Random player wins, 1:MCTS wins, 2:Draw
     */
    public static int runSingleGame(int simulationCount) {
        TicTacToe game = new TicTacToe();
        State<TicTacToe> currentState = game.start();
        int mctsPlayer = TicTacToe.X; // MCTS always plays as X
        int currentPlayer = game.opener(); // Starting player (X)

        while (!currentState.isTerminal()) {
            Move<TicTacToe> move;

            if (currentPlayer == mctsPlayer) {
                // MCTS player's turn
                Node<TicTacToe> rootNode = new TicTacToeNode(currentState);
                MCTS mcts = new MCTS(rootNode);
                mcts.setSimulationCount(simulationCount);
                move = mcts.findBestMove();
            } else {
                // Random player's turn
                move = currentState.chooseMove(currentPlayer);
            }

            // Apply the move
            currentState = currentState.next(move);

            // Switch players
            currentPlayer = 1 - currentPlayer;
        }

        // Determine game result
        Optional<Integer> winner = currentState.winner();
        if (winner.isPresent()) {
            return winner.get() == mctsPlayer ? 1 : 0; // 1=MCTS wins, 0=Random wins
        } else {
            return 2; // Draw
        }
    }

    /**
     * Test a specific simulation count configuration
     * @param simulationCount Simulation count to test
     */
    public static void testConfiguration(int simulationCount) {
        System.out.println("\nStarting test: SimulationCount=" + simulationCount + ", Games=" + GAMES_PER_CONFIG);

        int mctsWins = 0;
        int randomWins = 0;
        int draws = 0;

        long startTime = System.currentTimeMillis();

        // Run all games
        for (int i = 0; i < GAMES_PER_CONFIG; i++) {
            int result = runSingleGame(simulationCount);

            switch (result) {
                case 0: randomWins++; break;
                case 1: mctsWins++; break;
                case 2: draws++; break;
            }

            // Print progress every 100 games
            if ((i + 1) % 100 == 0) {
                int percent = (i + 1) * 100 / GAMES_PER_CONFIG;

                // Calculate estimated remaining time
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - startTime;
                long estimatedTotalTime = (long)(elapsedTime * (100.0 / percent));
                long remainingTime = estimatedTotalTime - elapsedTime;

                System.out.printf("Progress: %d%% (%d/%d) - Time elapsed: %s - Est. remaining: %s\n",
                        percent, i + 1, GAMES_PER_CONFIG,
                        formatTime(elapsedTime), formatTime(remainingTime));
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Calculate statistics
        double mctsWinRate = mctsWins * 100.0 / GAMES_PER_CONFIG;
        double randomWinRate = randomWins * 100.0 / GAMES_PER_CONFIG;
        double drawRate = draws * 100.0 / GAMES_PER_CONFIG;
        double avgTimePerGame = totalTime / (double)GAMES_PER_CONFIG;

        // Print results
        System.out.println("\nResults Summary (SimulationCount=" + simulationCount + "):");
        System.out.println("  MCTS Wins: " + mctsWins + " (" + String.format("%.1f%%", mctsWinRate) + ")");
        System.out.println("  Random Player Wins: " + randomWins + " (" + String.format("%.1f%%", randomWinRate) + ")");
        System.out.println("  Draws: " + draws + " (" + String.format("%.1f%%", drawRate) + ")");
        System.out.println("  Total Time: " + formatTime(totalTime));
        System.out.println("  Average Time Per Game: " + String.format("%.2f", avgTimePerGame) + "ms");
    }

    /**
     * Format time as a readable string
     */
    private static String formatTime(long timeInMs) {
        long hours = timeInMs / (1000 * 60 * 60);
        long minutes = (timeInMs % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (timeInMs % (1000 * 60)) / 1000;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        System.out.println("MCTS Performance Test - " + new Date());
        System.out.println("Running " + GAMES_PER_CONFIG + " games for each simulation count configuration");

        // Test each simulation count configuration
        for (int simCount : SIMULATION_COUNTS) {
            testConfiguration(simCount);
        }

        System.out.println("\nAll tests completed!");
    }
}