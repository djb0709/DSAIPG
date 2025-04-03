/*
 * Copyright (c) 2024. Robin Hillyard
 */

package com.phasmidsoftware.dsaipg.projects.mcts.tictactoe;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.*;

/**
 * Class to represent a Monte Carlo Tree Search for TicTacToe.
 */
public class MCTS {
    private final Node<TicTacToe> root;
    private  final  double explorationParameter  = Math.sqrt(2);
    private  int simulationCount = 1000;

    //track the relationship between parent and child nodes
    private final Map<Node<TicTacToe>, Node<TicTacToe>> parentMap = new HashMap<>();

    // keep the data of nodes
    private final Map<Node<TicTacToe>, NodeStats> statsMap = new HashMap<>();

    //data
    private static class NodeStats{
        int wins;
        int playouts;

        // Constructor
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
     * set the simulation count
     * @param simulationCount
     */
    public void setSimulationCount(int simulationCount) {
        this.simulationCount = simulationCount;
    }

    public MCTS(Node<TicTacToe> root) {
        this.root = root;
        statsMap.put(root, new NodeStats());
    }

    public static void main(String[] args) {

        TicTacToe game = new TicTacToe();
        State<TicTacToe> initialState = game.start();


        runGame(game, initialState);

        // This is where you process the MCTS to try to win the game.
    }


    public static void runGame(TicTacToe game, State<TicTacToe> initialState) {

        State<TicTacToe> currentState = initialState;
        int currentPlayer = game.opener();//x go first

        System.out.println("game started , X goes first use mcts, O goes second use random");

        while(!currentState.isTerminal()) {

            System.out.println("\n current board:");
            System.out.println(((TicTacToe.TicTacToeState) currentState).position().render());

            Move<TicTacToe> bestMove;

            if (currentPlayer == TicTacToe.X) {
                // Use MCTS to find the best move for player X
                Node<TicTacToe> rootNode = new TicTacToeNode(currentState);
                MCTS currentMCTS = new MCTS(rootNode);

                bestMove = currentMCTS.findBestMove();
                System.out.println("AI X chooses move: " + moveToString(bestMove));

            } else {
                bestMove = currentState.chooseMove(currentPlayer);
                System.out.println("Random O chooses move: " + moveToString(bestMove));

            }
            // Apply the best move to the current state
            currentState = currentState.next(bestMove);
            // Update the current player
            currentPlayer = 1 - currentPlayer;
        }
        // Print the final result
        System.out.println("Final board:");
        System.out.println(((TicTacToe.TicTacToeState) currentState).position().render());

        Optional<Integer> winner = currentState.winner();
        if (winner.isPresent()) {
            System.out.println("Winner: " + (winner.get() == TicTacToe.X ? "X" : "O"));
        } else {
            System.out.println("It's a draw!");
        }

    }

    /**
     * This method converts a move to a string representation.
     *
     * @param move the move to convert.
     * @return the string representation of the move.
     */
    private static String moveToString(Move<TicTacToe> move) {
      TicTacToe.TicTacToeMove ticTacToeMove = (TicTacToe.TicTacToeMove) move;
      int[] coordinates = ticTacToeMove.move();

      return "("+ coordinates[0] + ", " + coordinates[1] + ")";

    }

    /**
     * This method finds the best move using MCTS.
     *
     * @return the best move for the current player.
     */
    public Move<TicTacToe> findBestMove() {
        if (root.state().isTerminal()){
            throw new IllegalStateException("Cannot find best move from a terminal state");
        }
        expandNode(root);

        // Perform MCTS simulations
        for (int i = 0; i < simulationCount; i++) {

            List<Node<TicTacToe>> selectionPath = new ArrayList<>();

            //choose a best node to simulate
            Node<TicTacToe> selected = select(root,selectionPath);

            Node<TicTacToe> expanded = expand(selected);

            //add the expanded node to the selection path
            if (expanded != selected){
                selectionPath.add(expanded);
            }

            //simulate the game from the expanded node
            int simulationResult  = simulate(expanded.state());

            // backpropagate the result to the root node
            backpropagate(selectionPath, simulationResult);
        }

        // choose the best child node based on the root node
        return getBestMove();
    }

    /**
     * This method checks if the node is fully expanded.
     *
     * @param node the node to check.
     * @param path the path of nodes.
     * @return true if the node is fully expanded; false otherwise.
     */
    private Node<TicTacToe> select(Node<TicTacToe> node, List<Node<TicTacToe>> path) {
        path.add(node);
        if (node.isLeaf() || !isFullyExpanded(node)) {
            return node;
        }
        Node<TicTacToe> bestChild = null;
        double bestUCT  = Double.NEGATIVE_INFINITY;

        for (Node<TicTacToe> child : node.children()) {
            double uctValue = calculateUct(node,child);
            if (uctValue > bestUCT) {
                bestUCT = uctValue;
                bestChild = child;
            }
        }
        return select(bestChild, path);
    }

    /**
     * This method use to calculate the UCT value of a node.
     * UCT = wins / playouts + explorationConstant * sqrt(log(parentPlayouts) / playouts)
     */
    private double calculateUct(Node<TicTacToe> parent,Node<TicTacToe> child) {

        NodeStats parentStats = statsMap.get(parent);
        NodeStats childStats = statsMap.get(child);

        if (childStats.playouts == 0) {
            return Double.POSITIVE_INFINITY;
            // the node which has not been played yet will be selected
        }
        double exploitation = (double) childStats.wins / childStats.playouts;

        // if current node player, choose the max value uct!!!!!!!!!!!!!!!!!!!!!
        //if not current node player, choose the min value uct
        boolean isCurrentPlayer = parent.state().player() == child.state().player();
        if (!isCurrentPlayer) {
            exploitation = 1 - exploitation;
        }

        double exploration = explorationParameter  * Math.sqrt(Math.log(parentStats.playouts) / childStats.playouts);


       return  exploration + exploitation;

    }


    //check if the node is fully expanded
    private boolean isFullyExpanded(Node<TicTacToe> node) {
        int possibleMoves = node.state().moves(node.state().player()).size();
        return node.children().size() == possibleMoves;
    }

    //expand the node
    private Node<TicTacToe> expand(Node<TicTacToe> node){
        //checks
        if (node.state().isTerminal()) {
            return node;
        }
        //if no expanded children, expand the node
        if (node.children().isEmpty()) {
            expandNode(node);
        }
        if (isFullyExpanded(node)) {
            return node;
        }

        //find a node to expand, expand the child
        int player = node.state().player();
        for (Move<TicTacToe> move : node.state().moves(player)) {
            State<TicTacToe> nextState = node.state().next(move);

            //check if the child node already exists
            boolean alreadyExists = false;
            for (Node<TicTacToe> child : node.children()) {
                if (statesAreEuqal(child.state(), nextState)) {
                    alreadyExists = true;
                    break;
                }
            }
            //if the child node does not exist, create new child node
            if (!alreadyExists) {
                node.addChild(nextState);
                Node<TicTacToe> newChild = getLastAddedChild(node);

               // add the new child to the parent map
                parentMap.put(newChild, node);

                // add the new child to the stats map
                statsMap.put(newChild, new NodeStats());

                return newChild;
            }
        }

        return node;
    }


   //get the last added child node
    private Node<TicTacToe> getLastAddedChild(Node<TicTacToe> node) {
        Collection<Node<TicTacToe>> children = node.children();
        if (children.isEmpty()) {
            return null;
        }

        // convert the collection to an array and get the last element
        return children.toArray(new Node[0])[children.size() - 1];
    }


    //get the moves from expanded nodes
    private void expandNode(Node<TicTacToe> node) {
        if (node.state().isTerminal()) {
            return;
        }

        // expand all the nodes
        node.explore();

        // get the relationship between parent and child nodes and initialize the stats map
        for (Node<TicTacToe> child : node.children()) {
            parentMap.put(child, node);
            statsMap.put(child, new NodeStats());
        }
    }

    /**
     * This method checks if two states are equal.
     *
     * @param state1 the first state.
     * @param state2 the second state.
     * @return true if the states are equal; false otherwise.
     */
    private boolean statesAreEuqal(State<TicTacToe> state1, State<TicTacToe> state2) {
        if (state1 == state2) {
            return true;
        }

        if(state1 instanceof TicTacToe.TicTacToeState && state2 instanceof TicTacToe.TicTacToeState) {
            Position pos1 = ((TicTacToe.TicTacToeState) state1).position();
            Position pos2 = ((TicTacToe.TicTacToeState) state2).position();
            return pos1.equals(pos2);
        }
        return state1.equals(state2);
    }

    /**
     * This method simulates the game from the given node.
     *
     * @param state the state to simulate from.
     * @return the winner of the game 0 ,1, -1 for draw.
     */
    private int simulate(State<TicTacToe> state) {
        State<TicTacToe> currentState = state;
        int currentPlayer = state.player();

        // Simulate until the game is over
        while(!currentState.isTerminal()){
            //choose a move
            Move<TicTacToe> randomMove = currentState.chooseMove(currentPlayer);

            //apply the move
            currentState = currentState.next(randomMove);

            //switch player
            currentPlayer = 1- currentPlayer;
        }

        Optional<Integer> winner = currentState.winner();
        return winner.orElse(-1);//draw
    }

    /**
     * This method backpropagates the result of the simulation to the root node.
     *
     * @param path the path of nodes from the root to the leaf node.
     * @param winner the winner of the game.
     */
    private void backpropagate(List<Node<TicTacToe>> path , int winner){

       // iteration the path
        for (Node<TicTacToe> node : path) {
            NodeStats stats = statsMap.get(node);

            //add the playouts
            stats.playouts++;

            //if terminal
            if (winner != -1){
                //if X goes first, we assume X is the winner
                //if O goes first, we assume O is the winner
                if ((node.white() && winner == TicTacToe.X) || (!node.white() && winner == TicTacToe.O)) {
                    stats.wins++;
                }
            }else {
                //if draw
                stats.wins ++;
            }
        }
    }

    private Move<TicTacToe> getBestMove() {
        int currentPlayer = root.state().player();
        Move<TicTacToe> bestMove = null;
        int maxPlayouts = -1;

        // iteration all the possible moves
        for (Move<TicTacToe> move : root.state().moves(currentPlayer)) {
            State<TicTacToe> nextState = root.state().next(move);

            // find the child node
            for (Node<TicTacToe> child : root.children()) {
                if (statesAreEuqal(child.state(), nextState)) {
                    NodeStats stats = statsMap.get(child);

                    // choose the node with the max access
                    if (stats.playouts > maxPlayouts) {
                        maxPlayouts = stats.playouts;
                        bestMove = move;
                    }
                    break;
                }
            }
        }

        //when could not find the best move, random choose
        if (bestMove == null) {
            Collection<Move<TicTacToe>> moves = root.state().moves(currentPlayer);
            Move<TicTacToe>[] moveArray = moves.toArray(new Move[0]);
            bestMove = moveArray[new Random().nextInt(moveArray.length)];
        }

        return bestMove;
    }
    //get the parent node
    private Node<TicTacToe> getParent(Node<TicTacToe> node) {
        return parentMap.get(node);
    }

    //get the stats of the node
    public NodeStats getNodeStats(Node<TicTacToe> node) {
        return statsMap.get(node);
    }

    //get all the children of the node ucts
    public Map<Node<TicTacToe>, Double> getChildrenUCT(Node<TicTacToe> node) {
        Map<Node<TicTacToe>, Double> uctValues = new HashMap<>();

        for (Node<TicTacToe> child : node.children()) {
            uctValues.put(child, calculateUct(node, child));
        }

        return uctValues;
    }


    //print the tree
    public void printTree() {
        printNode(root, 0);
    }

    private void printNode(Node<TicTacToe> node, int depth) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            indent.append("  ");
        }

        NodeStats stats = statsMap.get(node);
        String nodeInfo = String.format("%sNode: W/P=%d/%d (%.2f%%)",
                indent.toString(),
                stats.wins,
                stats.playouts,
                stats.playouts > 0 ? 100.0 * stats.wins / stats.playouts : 0);

        System.out.println(nodeInfo);

        for (Node<TicTacToe> child : node.children()) {
            printNode(child, depth + 1);
        }
    }





//    private Move<TicTacToe> selectBestMoveFromRoot() {
//      int currentPlayer = root.state().player();
//      Move<TicTacToe> bestMove = null;
//      double bestScore = Double.NEGATIVE_INFINITY;
//
//      for (Move<TicTacToe> move :root.state().moves(currentPlayer)){
//          //create a new state for the move
//          State<TicTacToe> newState = root.state().next(move);
//
//          //find the child node for the new state
//          Node<TicTacToe> childNode = null;
//          for (Node<TicTacToe> child : root.children()){
//              if (statesAreEuqal(child.state(), newState)){
//                  childNode = child;
//                  break;
//              }
//          }
//          if (childNode == null){
//              continue;
//          }
//          double score = (double) childNode.playouts();
//
//          if (score > bestScore){
//              bestScore = score;
//              bestMove = move;
//          }
//      }
//      if (bestMove == null){
//          System.out.println("no best move found, randomly choose one");
//          Collection<Move<TicTacToe>> moves = root.state().moves(currentPlayer);
//          Move<TicTacToe>[] moveArray = moves.toArray(new Move[0]);
//          bestMove = moveArray[new Random().nextInt(moveArray.length)];
//
//      }
//      return bestMove;
//    }
//
//    /**
//     * This method selects a random child node from the given node.
//     *
//     * @param node the node to select a random child from.
//     * @return a random child node.
//     */
//    private Node<TicTacToe> selectRandomChild(Node<TicTacToe> node) {
//        Collection<Node<TicTacToe>> children = node.children();
//        if (children.isEmpty()) {
//            return node;
//        }
//        Node<TicTacToe>[] childArray = children.toArray(new Node[0]);
//        return childArray[new Random().nextInt(childArray.length)];
//    }


}