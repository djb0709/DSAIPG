package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;
import com.phasmidsoftware.dsaipg.projects.mcts.gomoku.Gomoku;
import com.phasmidsoftware.dsaipg.projects.mcts.gomoku.GomokuMCTS;
import com.phasmidsoftware.dsaipg.projects.mcts.gomoku.GomokuNode;
import com.phasmidsoftware.dsaipg.projects.mcts.gomoku.GomokuPosition;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Field;

/**
 * Gomoku game UI - AI Battle (MCTS vs Random)
 */
public class GomokuUI extends JFrame {
    private static final int CELL_SIZE = 35;
    private static final int MARGIN = 20;

    private int boardSize = 15; // Default board size
    private final boolean mctsMoveFirst = true; // MCTS always plays first (BLACK)

    private Gomoku game;
    private State<Gomoku> currentState;
    private List<State<Gomoku>> gameHistory = new ArrayList<>();
    private int currentHistoryIndex = -1;

    // Game components
    private BoardPanel boardPanel;
    private JLabel statusLabel;
    private JButton newGameButton;
    private JButton nextMoveButton;
    private JButton prevMoveButton;
    private JComboBox<String> boardSizeComboBox;

    // Score tracking
    private int mctsWins = 0;
    private int randomWins = 0;
    private int draws = 0;
    private JLabel mctsScoreLabel;
    private JLabel randomScoreLabel;
    private JLabel drawsLabel;

    /**
     * Main constructor
     */
    public GomokuUI() {
        setTitle("Gomoku AI Battle: MCTS (BLACK) vs Random (WHITE)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        createGame();
        initializeUI();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Initialize a new game
     */
    private void createGame() {
        GomokuPosition.setBoardSize(boardSize);
        game = new Gomoku();
        currentState = game.start();
        gameHistory.clear();
        currentHistoryIndex = -1;
        addToHistory(currentState); // Add initial state
    }

    /**
     * Set up the UI components
     */
    private void initializeUI() {
        // Board Panel
        boardPanel = new BoardPanel();
        boardPanel.setPreferredSize(new Dimension(
                boardSize * CELL_SIZE + 2 * MARGIN,
                boardSize * CELL_SIZE + 2 * MARGIN));

        // Status Panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Game Ready - Click 'Next Move' to start");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusPanel.add(statusLabel, BorderLayout.NORTH);

        // Score Panel
        JPanel scorePanel = new JPanel();
        mctsScoreLabel = new JLabel("MCTS: 0");
        randomScoreLabel = new JLabel("Random: 0");
        drawsLabel = new JLabel("Draws: 0");

        scorePanel.add(mctsScoreLabel);
        scorePanel.add(new JLabel(" | "));
        scorePanel.add(randomScoreLabel);
        scorePanel.add(new JLabel(" | "));
        scorePanel.add(drawsLabel);
        statusPanel.add(scorePanel, BorderLayout.SOUTH);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        newGameButton = new JButton("New Game");
        prevMoveButton = new JButton("Previous Move");
        nextMoveButton = new JButton("Next Move");

        String[] boardSizes = {"8×8", "15×15"};
        boardSizeComboBox = new JComboBox<>(boardSizes);
        boardSizeComboBox.setSelectedIndex(1); // Default: 15×15

        controlPanel.add(newGameButton);
        controlPanel.add(prevMoveButton);
        controlPanel.add(nextMoveButton);
        controlPanel.add(new JLabel("  Board Size:"));
        controlPanel.add(boardSizeComboBox);

        // Main Layout
        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        add(controlPanel, BorderLayout.NORTH);

        // Set up event handlers
        setupEventHandlers();

        // Initial button states
        updateButtonStates();
    }

    /**
     * Set up event handlers for UI components
     */
    private void setupEventHandlers() {
        newGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int newSize = (boardSizeComboBox.getSelectedIndex() == 0) ? 8 : 15;
                if (newSize != boardSize) {
                    boardSize = newSize;
                    GomokuPosition.setBoardSize(boardSize);
                    boardPanel.setPreferredSize(new Dimension(
                            boardSize * CELL_SIZE + 2 * MARGIN,
                            boardSize * CELL_SIZE + 2 * MARGIN));
                    pack();
                }

                createGame();
                statusLabel.setText("MCTS (BLACK) to play. Press 'Next Move' to continue.");
                updateButtonStates();
                boardPanel.repaint();
            }
        });

        nextMoveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentHistoryIndex < gameHistory.size() - 1) {
                    // Navigate forward in history
                    currentHistoryIndex++;
                    currentState = gameHistory.get(currentHistoryIndex);
                    updateUI();
                } else {
                    // Calculate a new move
                    makeNextMove();
                }
            }
        });

        prevMoveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentHistoryIndex > 0) {
                    currentHistoryIndex--;
                    currentState = gameHistory.get(currentHistoryIndex);

                    // Update status
                    int currentPlayer = currentState.player();
                    boolean isMCTSTurn = (mctsMoveFirst && currentPlayer == Gomoku.BLACK) ||
                            (!mctsMoveFirst && currentPlayer == Gomoku.WHITE);
                    String playerName = isMCTSTurn ? "MCTS" : "Random";
                    String colorName = (currentPlayer == Gomoku.BLACK) ? "BLACK" : "WHITE";
                    statusLabel.setText(playerName + "'s turn (" + colorName + ")");

                    updateUI();
                }
            }
        });
    }

    /**
     * Make the next move in the game
     */
    private void makeNextMove() {
        if (currentState.isTerminal()) return;

        int currentPlayer = currentState.player();
        boolean isMCTSTurn = (mctsMoveFirst && currentPlayer == Gomoku.BLACK) ||
                (!mctsMoveFirst && currentPlayer == Gomoku.WHITE);

        Move<Gomoku> nextMove;
        String playerName;
        long startTime = System.currentTimeMillis();

        try {
            if (isMCTSTurn) {
                // MCTS's turn
                playerName = "MCTS";
                Node<Gomoku> rootNode = new GomokuNode(currentState);
                GomokuMCTS mcts = new GomokuMCTS(rootNode);

                // Adjust simulation count based on board size
                int simulationCount = (boardSize <= 8) ? 1000 : 1500;
                mcts.setSimulationCount(simulationCount);

                nextMove = mcts.findBestMove();
            } else {
                // Random's turn
                playerName = "Random";
                nextMove = currentState.chooseMove(currentPlayer);
            }

            long endTime = System.currentTimeMillis();
            long timeTaken = endTime - startTime;

            // Apply the move
            currentState = currentState.next(nextMove);

            // Update history
            addToHistory(currentState);

            // Update status
            String colorName = (currentPlayer == Gomoku.BLACK) ? "BLACK" : "WHITE";
            statusLabel.setText(playerName + " (" + colorName + ") moved in " + timeTaken + "ms");

            // Check if game ended
            if (currentState.isTerminal()) {
                handleGameOver();
            }

            updateUI();
        } catch (Exception ex) {
            statusLabel.setText("Error making move: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Add the current state to history
     */
    private void addToHistory(State<Gomoku> state) {
        // If we made a new move from a previous point in history,
        // truncate the future history
        if (currentHistoryIndex < gameHistory.size() - 1) {
            gameHistory = new ArrayList<>(gameHistory.subList(0, currentHistoryIndex + 1));
        }

        gameHistory.add(state);
        currentHistoryIndex = gameHistory.size() - 1;
    }

    /**
     * Update UI after a move
     */
    private void updateUI() {
        boardPanel.repaint();
        updateButtonStates();
    }

    /**
     * Update button states based on current game state
     */
    private void updateButtonStates() {
        prevMoveButton.setEnabled(currentHistoryIndex > 0);
        nextMoveButton.setEnabled(!currentState.isTerminal() ||
                currentHistoryIndex < gameHistory.size() - 1);
    }

    /**
     * Handle game over state
     */
    private void handleGameOver() {
        Optional<Integer> winner = currentState.winner();
        if (winner.isPresent()) {
            int winnerPlayer = winner.get();
            boolean mctsWon = (mctsMoveFirst && winnerPlayer == Gomoku.BLACK) ||
                    (!mctsMoveFirst && winnerPlayer == Gomoku.WHITE);

            if (mctsWon) {
                mctsWins++;
                mctsScoreLabel.setText("MCTS: " + mctsWins);
                statusLabel.setText("Game Over - MCTS wins!");
            } else {
                randomWins++;
                randomScoreLabel.setText("Random: " + randomWins);
                statusLabel.setText("Game Over - Random wins!");
            }
        } else {
            draws++;
            drawsLabel.setText("Draws: " + draws);
            statusLabel.setText("Game Over - It's a draw!");
        }
    }

    /**
     * Helper method to get stone at a specific position
     */
    private int getStone(GomokuPosition position, int row, int col) {
        try {
            // 尝试使用反射访问grid字段
            Field gridField = GomokuPosition.class.getDeclaredField("grid");
            gridField.setAccessible(true);
            int[][] grid = (int[][]) gridField.get(position);
            return grid[row][col];
        } catch (Exception e) {
            // 如果反射失败，使用位置检测方法
            return detectStoneAtPosition(position, row, col);
        }
    }

    /**
     * detect the stone at a specific position
     */
    private int detectStoneAtPosition(GomokuPosition position, int row, int col) {
        try {
            // create a copy of the position to test the move
            // if the move is valid, it will be empty
            GomokuPosition copy = copyPosition(position);
            copy.move(Gomoku.BLACK, row, col);
            return Gomoku.EMPTY;
        } catch (RuntimeException e) {
            // if the move is invalid, it means the position is occupied
            if (e.getMessage().contains("occupied")) {
                // to check the color of the stone
                // because the move is invalid, we can check the last player
                if (gameHistory.size() > 0) {
                    // odd/even move number

                    int moveNumber = currentHistoryIndex;
                    boolean isBlackMove = moveNumber % 2 == 0;
                    // if random first, the color of the stone is not reversed
                    // if mcts first, the color of the stone is reversed
                    return isBlackMove ? Gomoku.BLACK : Gomoku.WHITE;
                }
            }

            return Gomoku.EMPTY;
        }
    }

    /**
     * create a copy of the position
     */
    private GomokuPosition copyPosition(GomokuPosition original) {
        try {
            // get the last player and count
            Field lastField = GomokuPosition.class.getDeclaredField("last");
            Field countField = GomokuPosition.class.getDeclaredField("count");
            lastField.setAccessible(true);
            countField.setAccessible(true);
            int last = (int) lastField.get(original);
            int count = (int) countField.get(original);

            // get the grid size
            Field gridField = GomokuPosition.class.getDeclaredField("grid");
            gridField.setAccessible(true);
            int[][] originalGrid = (int[][]) gridField.get(original);

            // copy the grid
            int[][] gridCopy = new int[boardSize][boardSize];
            for (int i = 0; i < boardSize; i++) {
                System.arraycopy(originalGrid[i], 0, gridCopy[i], 0, boardSize);
            }

            // create a new position with the copied grid
            return new GomokuPosition(gridCopy, count, last);
        } catch (Exception e) {
            // if reflection fails, return a new starting position
            return GomokuPosition.startingPosition(Gomoku.EMPTY);
        }
    }

    /**
     * Board Panel for drawing the Gomoku board
     */
    private class BoardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw board background
            g2d.setColor(new Color(210, 180, 140)); // Light wood color
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Draw grid lines
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));

            for (int i = 0; i < boardSize; i++) {
                // Draw horizontal lines
                g2d.drawLine(
                        MARGIN,
                        MARGIN + i * CELL_SIZE,
                        MARGIN + (boardSize - 1) * CELL_SIZE,
                        MARGIN + i * CELL_SIZE
                );

                // Draw vertical lines
                g2d.drawLine(
                        MARGIN + i * CELL_SIZE,
                        MARGIN,
                        MARGIN + i * CELL_SIZE,
                        MARGIN + (boardSize - 1) * CELL_SIZE
                );
            }

            // Draw stones
            if (currentState != null) {
                GomokuPosition position = ((Gomoku.GomokuState) currentState).position();

                for (int row = 0; row < boardSize; row++) {
                    for (int col = 0; col < boardSize; col++) {
                        int stone = getStone(position, row, col);
                        int x = MARGIN + col * CELL_SIZE;
                        int y = MARGIN + row * CELL_SIZE;

                        if (stone == Gomoku.BLACK) {
                            // Black stone
                            g2d.setColor(Color.BLACK);
                            g2d.fillOval(x - CELL_SIZE/3, y - CELL_SIZE/3,
                                    2*CELL_SIZE/3, 2*CELL_SIZE/3);
                        } else if (stone == Gomoku.WHITE) {
                            // White stone
                            g2d.setColor(Color.WHITE);
                            g2d.fillOval(x - CELL_SIZE/3, y - CELL_SIZE/3,
                                    2*CELL_SIZE/3, 2*CELL_SIZE/3);
                            g2d.setColor(Color.BLACK);
                            g2d.drawOval(x - CELL_SIZE/3, y - CELL_SIZE/3,
                                    2*CELL_SIZE/3, 2*CELL_SIZE/3);
                        }
                    }
                }
            }
        }
    }

    /**
     * Main method
     */
    public static void main(String[] args) {
        // set the look and feel to system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //start
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GomokuUI();
            }
        });
    }
}