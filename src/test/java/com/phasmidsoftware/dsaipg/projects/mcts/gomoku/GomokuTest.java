package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import static org.junit.Assert.*;
import org.junit.Test;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.Collection;
import java.util.Optional;
import java.util.Random;

public class GomokuTest {

    @Test
    public void testStartingPosition() {
        GomokuPosition position = Gomoku.startingPosition();
        assertEquals(Gomoku.EMPTY, position.last);
        // Board should be empty
        assertFalse(position.winner().isPresent());
    }

    @Test
    public void testOpener() {
        Gomoku game = new Gomoku(new Random(42));
        assertEquals(Gomoku.BLACK, game.opener());
    }

    @Test
    public void testStart() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> state = game.start();
        assertNotNull(state);
        assertFalse(state.isTerminal());
        assertEquals(Gomoku.BLACK, state.player());
    }

    @Test
    public void testGomokuMove() {
        Gomoku.GomokuMove move = new Gomoku.GomokuMove(Gomoku.BLACK, 3, 4);
        assertEquals(Gomoku.BLACK, move.player());
        assertArrayEquals(new int[]{3, 4}, move.move());
    }

    @Test
    public void testGomokuState() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> initialState = game.start();

        // Test initial state
        assertFalse(initialState.isTerminal());
        assertEquals(Gomoku.BLACK, initialState.player());
        assertFalse(initialState.winner().isPresent());

        // Test available moves
        Collection<Move<Gomoku>> moves = initialState.moves(Gomoku.BLACK);
        int boardSize = GomokuPosition.getGridSize();
        assertEquals(1, moves.size());

        // Test move execution
        Move<Gomoku> move = new Gomoku.GomokuMove(Gomoku.BLACK, 3, 4);
        State<Gomoku> nextState = initialState.next(move);

        // Check player alternation
        assertEquals(Gomoku.WHITE, nextState.player());
    }

    @Test
    public void testWinCondition() {
        Gomoku game = new Gomoku(new Random(42));
        State<Gomoku> state = game.start();

        // Create a winning condition for BLACK (5 in a row horizontally)
        for (int i = 0; i < 5; i++) {
            Move<Gomoku> blackMove = new Gomoku.GomokuMove(Gomoku.BLACK, 3, i);
            state = state.next(blackMove);

            if (i < 4) { // Need to alternate moves except after the winning move
                Move<Gomoku> whiteMove = new Gomoku.GomokuMove(Gomoku.WHITE, 4, i);
                state = state.next(whiteMove);
            }
        }

        // Test terminal state and winner
        assertTrue(state.isTerminal());
        Optional<Integer> winner = state.winner();
        assertTrue(winner.isPresent());
        assertEquals(Gomoku.BLACK, (int)winner.get());
    }
}