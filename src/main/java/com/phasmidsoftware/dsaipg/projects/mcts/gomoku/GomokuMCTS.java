package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.*;

/**
 * MCTS for Gomoku
 */
public class GomokuMCTS {
    private final Node<Gomoku> root;
    private final double explorationParameter = Math.sqrt(2);
    private int simulationCount = 1000;

    // get the parent and child relationship
    private final Map<Node<Gomoku>, Node<Gomoku>> parentMap = new HashMap<>();

    // store the node statistics
    private final Map<Node<Gomoku>, NodeStats> statsMap = new HashMap<>();

    // node
    private static class NodeStats {
        int wins;
        int playouts;

        NodeStats() {
            this.wins = 0;
            this.playouts = 0;
        }

        NodeStats(int wins, int playouts) {
            this.wins = wins;
            this.playouts = playouts;
        }
    }

    /**
     * set the number of simulations
     */
    public void setSimulationCount(int simulationCount) {
        this.simulationCount = simulationCount;
    }


    public GomokuMCTS(Node<Gomoku> root) {
        this.root = root;
        statsMap.put(root, new NodeStats());
    }

    /**
     * run a game with MCTS and board
     */
    public static void main(String[] args) {
        Gomoku game = new Gomoku();
        State<Gomoku> initialState = game.start();

        runGame(game, initialState);
    }

    /**
     * run game
     */
    public static void runGame(Gomoku game, State<Gomoku> initialState) {
        State<Gomoku> currentState = initialState;
        int currentPlayer = game.opener(); // black first

        System.out.println("Game start，Black(X) go first with Mcts，White (O)next with random");

        while (!currentState.isTerminal()) {
            System.out.println("\ncurrent board:");
            System.out.println(((Gomoku.GomokuState) currentState).position().render());

            Move<Gomoku> bestMove;

            if (currentPlayer == Gomoku.BLACK) { //can switch to white
                // find best move with MCTS
                Node<Gomoku> rootNode = new GomokuNode(currentState);
                GomokuMCTS currentMCTS = new GomokuMCTS(rootNode);

                bestMove = currentMCTS.findBestMove();
                System.out.println("MCT black to move: " + moveToString(bestMove));
            } else {
                bestMove = currentState.chooseMove(currentPlayer);
                System.out.println("Random white to move: " + moveToString(bestMove));
            }

            // apply the move
            currentState = currentState.next(bestMove);
            // update the current player
            currentPlayer = 1 - currentPlayer;
        }

        // print the final board
        System.out.println("\nthe final board:");
        System.out.println(((Gomoku.GomokuState) currentState).position().render());

        Optional<Integer> winner = currentState.winner();
        if (winner.isPresent()) {
            System.out.println("Winner: " + (winner.get() == Gomoku.BLACK ? "Black(X)" : "White(O)"));
        } else {
            System.out.println("draw!");
        }
    }

    /**
     * tostring for move
     */
    private static String moveToString(Move<Gomoku> move) {
        Gomoku.GomokuMove gomokuMove = (Gomoku.GomokuMove) move;
        int[] coordinates = gomokuMove.move();

        return "(" + coordinates[0] + ", " + coordinates[1] + ")";
    }

    /**
     * find the best move
     */
    public Move<Gomoku> findBestMove() {
        if (root.state().isTerminal()) {
            throw new IllegalStateException("Cannot find best move on terminal state");
        }

        expandNode(root);

        //apply the simalation with the chess board size and stage
        int movesMade = ((Gomoku.GomokuState)root.state()).position().getCount();
        int boardSize = GomokuPosition.getGridSize();

        // adjust the simulation count based on the board size and moves made
        if (boardSize <= 8) {
            // small chess board strategy
            if (movesMade < 10) {
                simulationCount = 800;  // early stage
            } else if (movesMade < 30) {
                simulationCount = 1000; // mid stage
            } else {
                simulationCount = 1200; // late stage
            }
        } else {
            // big chess board strategy
            if (movesMade < 20) {
                simulationCount = 1000; // early stage
            } else if (movesMade < 60) {
                simulationCount = 1500;
            } else {
                simulationCount = 2000;
            }
        }

        // simulate the game
        for (int i = 0; i < simulationCount; i++) {
            List<Node<Gomoku>> selectionPath = new ArrayList<>();
            Node<Gomoku> selected = select(root, selectionPath);
            Node<Gomoku> expanded = expand(selected);

            if (expanded != selected) {
                selectionPath.add(expanded);
            }

            int simulationResult = simulate(expanded.state());
            backpropagate(selectionPath, simulationResult);
        }

        return getBestMove();
    }

    /**
     * node selection
     */
    private Node<Gomoku> select(Node<Gomoku> node, List<Node<Gomoku>> path) {
        path.add(node);
        if (node.isLeaf() || !isFullyExpanded(node)) {
            return node;
        }

        Node<Gomoku> bestChild = null;
        double bestUCT = Double.NEGATIVE_INFINITY;

        for (Node<Gomoku> child : node.children()) {
            double uctValue = calculateUct(node, child);
            if (uctValue > bestUCT) {
                bestUCT = uctValue;
                bestChild = child;
            }
        }

        return select(bestChild, path);
    }

    /**
     * Calculate UCT value
     */
    private double calculateUct(Node<Gomoku> parent, Node<Gomoku> child) {
        NodeStats parentStats = statsMap.get(parent);
        NodeStats childStats = statsMap.get(child);

        if (childStats.playouts == 0) {
            return Double.POSITIVE_INFINITY; // Node not visited yet
        }

        double exploitation = (double) childStats.wins / childStats.playouts;

        // if the current player is black, find the best, else find the worst(1-exploitation)
        boolean isCurrentPlayer = parent.state().player() == child.state().player();
        if (!isCurrentPlayer) {
            exploitation = 1 - exploitation;
        }

        double exploration = explorationParameter * Math.sqrt(Math.log(parentStats.playouts) / childStats.playouts);

        return exploitation + exploration;
    }

    /**
     * check if the node is fully expanded
     */
    private boolean isFullyExpanded(Node<Gomoku> node) {
        int possibleMoves = node.state().moves(node.state().player()).size();
        return node.children().size() == possibleMoves;
    }

    /**
     * node expansion
     */
    private Node<Gomoku> expand(Node<Gomoku> node) {
        if (node.state().isTerminal()) {
            return node;
        }

        if (node.children().isEmpty()) {
            expandNode(node);
        }

        if (isFullyExpanded(node)) {
            return node;
        }

        // find an unvisited child
        int player = node.state().player();
        for (Move<Gomoku> move : node.state().moves(player)) {
            State<Gomoku> nextState = node.state().next(move);

            // is the child already exists?
            boolean alreadyExists = false;
            for (Node<Gomoku> child : node.children()) {
                if (statesAreEqual(child.state(), nextState)) {
                    alreadyExists = true;
                    break;
                }
            }

            // if not, add the child
            if (!alreadyExists) {
                node.addChild(nextState);
                Node<Gomoku> newChild = getLastAddedChild(node);

                parentMap.put(newChild, node);
                statsMap.put(newChild, new NodeStats());

                return newChild;
            }
        }

        return node;
    }

    /**
     * get the last added child
     */
    private Node<Gomoku> getLastAddedChild(Node<Gomoku> node) {
        Collection<Node<Gomoku>> children = node.children();
        if (children.isEmpty()) {
            return null;
        }

        return children.toArray(new Node[0])[children.size() - 1];
    }

    /**
     * expand the node
     */
    private void expandNode(Node<Gomoku> node) {
        if (node.state().isTerminal()) {
            return;
        }

        // expand
        node.explore();

        // create parent and child relationship
        for (Node<Gomoku> child : node.children()) {
            parentMap.put(child, node);
            statsMap.put(child, new NodeStats());
        }
    }

    /**
     * state is equal?
     */
    private boolean statesAreEqual(State<Gomoku> state1, State<Gomoku> state2) {
        if (state1 == state2) {
            return true;
        }

        if (state1 instanceof Gomoku.GomokuState && state2 instanceof Gomoku.GomokuState) {
            GomokuPosition pos1 = ((Gomoku.GomokuState) state1).position();
            GomokuPosition pos2 = ((Gomoku.GomokuState) state2).position();
            return pos1.equals(pos2);
        }

        return state1.equals(state2);
    }

    /**
     * simulate
     */
    private int simulate(State<Gomoku> state) {
        State<Gomoku> currentState = state;
        int currentPlayer = state.player();

        // loop
        while(!currentState.isTerminal()) {
            // choose a random move
            Move<Gomoku> randomMove = currentState.chooseMove(currentPlayer);

            // apply the move
            currentState = currentState.next(randomMove);

            // switch player
            currentPlayer = 1 - currentPlayer;
        }

        Optional<Integer> winner = currentState.winner();
        return winner.orElse(-1); // -1 for draw
    }

    /**
     * backpropagation
     */
    private void backpropagate(List<Node<Gomoku>> path, int winner) {
        for (Node<Gomoku> node : path) {
            NodeStats stats = statsMap.get(node);

            // add the playouts
            stats.playouts++;

            // if winner
            if (winner != -1) {
                // add the wins
                if ((node.white() && winner == Gomoku.BLACK) || (!node.white() && winner == Gomoku.WHITE)) {
                    stats.wins++;
                }
            } else {
                // if draw
                stats.wins += 0.5; // add half a win
            }
        }
    }

    /**
     * find the best move
     */
    private Move<Gomoku> getBestMove() {
        int currentPlayer = root.state().player();
        Move<Gomoku> bestMove = null;
        int maxPlayouts = -1;

        // iterate through the moves
        for (Move<Gomoku> move : root.state().moves(currentPlayer)) {
            State<Gomoku> nextState = root.state().next(move);

            // get child
            for (Node<Gomoku> child : root.children()) {
                if (statesAreEqual(child.state(), nextState)) {
                    NodeStats stats = statsMap.get(child);

                    // find node with max visits
                    if (stats.playouts > maxPlayouts) {
                        maxPlayouts = stats.playouts;
                        bestMove = move;
                    }
                    break;
                }
            }
        }

        // if can not find，randomly choose a move
        if (bestMove == null) {
            Collection<Move<Gomoku>> moves = root.state().moves(currentPlayer);
            Move<Gomoku>[] moveArray = moves.toArray(new Move[0]);
            bestMove = moveArray[new Random().nextInt(moveArray.length)];
        }

        return bestMove;
    }
}