package com.phasmidsoftware.dsaipg.projects.mcts.gomoku;

import com.phasmidsoftware.dsaipg.projects.mcts.core.Game;
import com.phasmidsoftware.dsaipg.projects.mcts.core.Move;
import com.phasmidsoftware.dsaipg.projects.mcts.core.State;

import java.util.*;

/**
 * show the logic of Gomoku
 */
public class Gomoku implements Game<Gomoku> {
    public static final int BLACK = 1;  // black
    public static final int WHITE = 0;  // white
    public static final int EMPTY = -1; // empty

    /**
     * one game to show the winner
     */
    public static void main(String[] args) {
        State<Gomoku> state = new Gomoku().runGame();
        if (state.winner().isPresent())
            System.out.println("Gomoku: winner is: " + (state.winner().get() == BLACK ? "black" : "white"));
        else
            System.out.println("Gomoku: draw");
    }

    /**
     * get the starting position
     */
    static GomokuPosition startingPosition() {
        return GomokuPosition.startingPosition(EMPTY);
    }

    /**
     * run a game
     */
    State<Gomoku> runGame() {
        State<Gomoku> state = start();
        int player = opener();
        while (!state.isTerminal()) {
            state = state.next(state.chooseMove(player));
            player = 1 - player;
        }
        return state;
    }

    /**
     * who is the first player
     */
    @Override
    public int opener() {
        return BLACK;  // 黑棋先行
    }

    /**
     * state
     */
    @Override
    public State<Gomoku> start() {
        return new GomokuState();
    }

    public Gomoku(Random random) {
        this.random = random;
    }

    public Gomoku(long seed) {
        this(new Random(seed));
    }

    public Gomoku() {
        this(System.currentTimeMillis());
    }

    private final Random random;

    /**
     * gomoku  moves
     */
    static class GomokuMove implements Move<Gomoku> {
        /**
         * get player
         */
        @Override
        public int player() {
            return player;
        }


        public GomokuMove(int player, int i, int j) {
            this.player = player;
            this.i = i;
            this.j = j;
        }

        /**
         * get move
         */
        public int[] move() {
            return new int[]{i, j};
        }

        private final int player;
        private final int i;
        private final int j;
    }

    /**
     * inner class state
     */
    class GomokuState implements State<Gomoku> {

        @Override
        public Gomoku game() {
            return Gomoku.this;
        }

        /**
         * get current player
         */
        @Override
        public int player() {
            return switch (position.last) {
                case 0, -1 -> BLACK;
                case 1 -> WHITE;
                default -> EMPTY;
            };
        }

        /**
         * get position
         */
        public GomokuPosition position() {
            return this.position;
        }

        /**
         * get winner
         */
        @Override
        public Optional<Integer> winner() {
            return position.winner();
        }


        @Override
        public Random random() {
            return random;
        }

        /**
         * get all the moves
         */
        @Override
        public Collection<Move<Gomoku>> moves(int player) {
            if (player == position.last)
                throw new RuntimeException("error with: " + player);

            List<int[]> moves = position.moves(player);
            ArrayList<Move<Gomoku>> list = new ArrayList<>();
            for (int[] coordinates : moves)
                list.add(new GomokuMove(player, coordinates[0], coordinates[1]));
            return list;
        }

        /**
         * move and get the next state
         */
        @Override
        public State<Gomoku> next(Move<Gomoku> move) {
            GomokuMove gomokuMove = (GomokuMove) move;
            int[] ints = gomokuMove.move();
            return new GomokuState(position.move(move.player(), ints[0], ints[1]));
        }

        /**
         * if the game is over
         */
        @Override
        public boolean isTerminal() {
            return position.full() || position.winner().isPresent();
        }


        public GomokuState(GomokuPosition position) {
            this.position = position;
        }

        public GomokuState() {
            this(startingPosition());
        }

        private final GomokuPosition position;
    }
}