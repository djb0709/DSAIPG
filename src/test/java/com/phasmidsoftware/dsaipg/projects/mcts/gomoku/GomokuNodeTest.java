package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import static org.junit.Assert.*;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import org.junit.Test;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Node;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.Collection;
import java.util.Random;

/**
 * Test class for GomokuNode
 */
public class GomokuNodeTest {

    /**
     * Test node construction
     */
    @Test
    public void testConstructor() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        GomokuNode node = new GomokuNode(initialState);

        assertNotNull("Node should be created", node);
        assertEquals("Initial state should be stored", initialState, node.state());
        assertTrue("Initial node should be for BLACK", node.white());
        assertFalse("Initial node should not be a leaf", node.isLeaf());
    }

    /**
     * Test node isLeaf property
     */
    @Test
    public void testIsLeaf() {
        // Create a non-terminal state
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> nonTerminalState = game.start();
        GomokuNode nonTerminalNode = new GomokuNode(nonTerminalState);

        assertFalse("Non-terminal node should not be a leaf", nonTerminalNode.isLeaf());

        // Create a terminal state (this is a bit tricky, we need to create a winning position)
        State<Gomoku> state = game.start();

        // Create a sequence of moves to reach a terminal state
        // BLACK places 5 in a row to win
        for (int i = 0; i < 5; i++) {
            state = state.next(new Gomoku.GomokuMove(Gomoku.BLACK, 3, i));
            if (i < 4) { // Need WHITE moves except after winning move
                state = state.next(new Gomoku.GomokuMove(Gomoku.WHITE, 4, i));
            }
        }

        GomokuNode terminalNode = new GomokuNode(state);
        assertTrue("Terminal node should be a leaf", terminalNode.isLeaf());
    }

    /**
     * Test node state retrieval
     */
    @Test
    public void testState() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        GomokuNode node = new GomokuNode(initialState);

        assertEquals("State should be retrievable", initialState, node.state());
    }

    /**
     * Test white/black determination
     */
    @Test
    public void testWhite() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> blackState = game.start(); // BLACK goes first
        GomokuNode blackNode = new GomokuNode(blackState);

        assertTrue("First player should be 'white'", blackNode.white());

        // Make a move to get a state where WHITE is to play
        State<Gomoku> whiteState = blackState.next(new Gomoku.GomokuMove(Gomoku.BLACK, 3, 3));
        GomokuNode whiteNode = new GomokuNode(whiteState);

        assertFalse("Second player should not be 'white'", whiteNode.white());
    }

    /**
     * Test children collection initially empty
     */
    @Test
    public void testChildrenInitiallyEmpty() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        GomokuNode node = new GomokuNode(initialState);

        Collection<Node<Gomoku>> children = node.children();
        assertNotNull("Children collection should not be null", children);
        assertTrue("Initial children collection should be empty", children.isEmpty());
    }

    /**
     * Test adding child nodes
     */
    @Test
    public void testAddChild() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        GomokuNode node = new GomokuNode(initialState);

        // Create a child state
        State<Gomoku> childState = initialState.next(new Gomoku.GomokuMove(Gomoku.BLACK, 3, 3));

        // Add child
        node.addChild(childState);

        Collection<Node<Gomoku>> children = node.children();
        assertFalse("Children collection should not be empty after adding child", children.isEmpty());
        assertEquals("Children collection should have one element", 1, children.size());

        // Verify child state
        Node<Gomoku> childNode = children.iterator().next();
        assertEquals("Child node should have the correct state", childState, childNode.state());
    }

    /**
     * Test node exploration
     */
    @Test
    public void testExplore() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        GomokuNode node = new GomokuNode(initialState);

        // Before exploration, children should be empty
        assertTrue("Before exploration, children should be empty", node.children().isEmpty());

        // Explore
        node.explore();

        // After exploration, children should not be empty if state has moves
        Collection<Move<Gomoku>> possibleMoves = initialState.moves(Gomoku.BLACK);
        assertEquals("After exploration, should have same number of children as possible moves",
                possibleMoves.size(), node.children().size());
    }

    /**
     * Test backPropagate
     */
    @Test
    public void testBackPropagate() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();
        GomokuNode node = new GomokuNode(initialState);

        // Initial values
        assertEquals("Initial playouts should be 0", 0, node.playouts());
        assertEquals("Initial wins should be 0", 0, node.wins());

        // Add some children and update their stats manually
        State<Gomoku> childState1 = initialState.next(new Gomoku.GomokuMove(Gomoku.BLACK, 3, 3));
        State<Gomoku> childState2 = initialState.next(new Gomoku.GomokuMove(Gomoku.BLACK, 4, 4));

        node.addChild(childState1);
        node.addChild(childState2);

        // Get actual child nodes
        Node<Gomoku>[] childNodes = node.children().toArray(new Node[0]);
        assertEquals("Should have two child nodes", 2, childNodes.length);

        // Create a terminal state for the first child
        State<Gomoku> terminalState = childState1;
        for (int i = 0; i < 4; i++) { // Add moves to make it terminal
            terminalState = terminalState.next(new Gomoku.GomokuMove((i%2 == 0) ? Gomoku.WHITE : Gomoku.BLACK, 5+i, 5+i));
        }

        // Make the second node a leaf by replacing its state with a terminal state
        childNodes[0] = new GomokuNode(terminalState);


        // Back propagate
        node.backPropagate();


    }
}