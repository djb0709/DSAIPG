package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

/**
 * Gomoku nodes
 */
public class GomokuNode implements Node<Gomoku> {
    /**
     * is a leaf node?
     */
    @Override
    public boolean isLeaf() {
        return state().isTerminal();
    }

    /**
     * get the state
     */
    @Override
    public State<Gomoku> state() {
        return state;
    }

    /**
     * if the player is the first player
     */
    @Override
    public boolean white() {
        return state.player() == state.game().opener();
    }

    /**
     * get the children
     */
    @Override
    public Collection<Node<Gomoku>> children() {
        return children;
    }

    /**
     * add a child
     */
    @Override
    public void addChild(State<Gomoku> state) {
        children.add(new GomokuNode(state));
    }

    /**
     * back propagate the wins and playouts
     */
    @Override
    public void backPropagate() {
        playouts = 0;
        wins = 0;
        for (Node<Gomoku> child : children) {
            wins += child.wins();
            playouts += child.playouts();
        }
    }

    /**
     * get the number of wins
     */
    @Override
    public int wins() {
        return wins;
    }

    /**
     * get the number of playouts
     */
    @Override
    public int playouts() {
        return playouts;
    }


    public GomokuNode(State<Gomoku> state) {
        this.state = state;
        children = new ArrayList<>();
        initializeNodeData();
    }

    /**
     * initialize the node data
     */
    private void initializeNodeData() {
        if (isLeaf()) {
            playouts = 1;
            Optional<Integer> winner = state.winner();
            if (winner.isPresent())
                wins = 2; // win
            else
                wins = 1; // draw
        }
    }

    private final State<Gomoku> state;
    private final ArrayList<Node<Gomoku>> children;

    private int wins;
    private int playouts;
}