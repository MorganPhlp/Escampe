package iialib.games.algs.algorithms;

import iialib.games.algs.GameAlgorithm;
import iialib.games.algs.IHeuristic;
import iialib.games.model.IBoard;
import iialib.games.model.IMove;
import iialib.games.model.IRole;

import java.util.ArrayList;

public class AlphaBeta<Move extends IMove, Role extends IRole, Board extends IBoard<Move, Role, Board>>
        implements GameAlgorithm<Move, Role, Board> {

    // Constants
    /** Default value for depth limit */
    private final static int DEPTH_MAX_DEFAUT = 4;

    // Attributes
    /** Role of the max player */
    private final Role playerMaxRole;

    /** Role of the min player */
    private final Role playerMinRole;

    /** Algorithm max depth */
    private int depthMax = DEPTH_MAX_DEFAUT;

    /** Heuristic used by the max player */
    private IHeuristic<Board, Role> h;

    /** Number of internally visited (developed) nodes (for stats) */
    private int nbNodes;

    /** Number of leaves nodes (for stats) */
    private int nbLeaves;

    /** Use negamax version if true, classic version if false */
    private boolean useNegamax;

    // --------- Constructors ---------

    public AlphaBeta(Role playerMaxRole, Role playerMinRole, IHeuristic<Board, Role> h) {
        this.playerMaxRole = playerMaxRole;
        this.playerMinRole = playerMinRole;
        this.h = h;
        this.useNegamax = false; // Classic version by default
    }

    public AlphaBeta(Role playerMaxRole, Role playerMinRole, IHeuristic<Board, Role> h, int depthMax) {
        this(playerMaxRole, playerMinRole, h);
        this.depthMax = depthMax;
    }

    public AlphaBeta(Role playerMaxRole, Role playerMinRole, IHeuristic<Board, Role> h, int depthMax, boolean useNegamax) {
        this(playerMaxRole, playerMinRole, h, depthMax);
        this.useNegamax = useNegamax;
    }

    /*
     * IAlgo METHODS =============
     */

    @Override
    public Move bestMove(Board board, Role playerRole) {
        // System.out.println("[AlphaBeta" + (useNegamax ? "-Negamax" : "") + "]");

        // Reset statistics
        nbNodes = 0;
        nbLeaves = 0;

        // Get all possible moves for the current player
        ArrayList<Move> moves = board.possibleMoves(playerRole);

        // If no moves are possible, return null
        if (moves == null || moves.isEmpty()) {
            return null;
        }

        Move bestMove = null;

        if (useNegamax) {
            // Negamax version
            int bestValue = Integer.MIN_VALUE;
            int alpha = Integer.MIN_VALUE;
            int beta = Integer.MAX_VALUE;

            for (Move move : moves) { // Explore all possible moves
                Board nextBoard = board.play(move, playerRole); // Get the next board state
                int value = -negamax(nextBoard, 1, -beta, -alpha, playerRole); // Negate the value for the opponent

                if (value > bestValue) { // Update best value and move if necessary
                    bestValue = value;
                    bestMove = move;
                }
                alpha = Math.max(alpha, value); // Update alpha
            }
        } else {
            // Classic version
            if (playerRole.equals(playerMaxRole)) {
                // MAX player: maximize the value
                int bestValue = Integer.MIN_VALUE;
                for (Move move : moves) { // Explore all possible moves
                    Board nextBoard = board.play(move, playerRole); // Get the next board state
                    int value = minMaxAB(nextBoard, 1, Integer.MIN_VALUE, Integer.MAX_VALUE); // Get the value from MIN's perspective
                    if (value > bestValue) {
                        bestValue = value;
                        bestMove = move;
                    }
                }
            } else {
                // MIN player: minimize the value
                int bestValue = Integer.MAX_VALUE;
                for (Move move : moves) { // Explore all possible moves
                    Board nextBoard = board.play(move, playerRole); // Get the next board state
                    int value = maxMinAB(nextBoard, 1, Integer.MIN_VALUE, Integer.MAX_VALUE); // Get the value from MAX's perspective
                    if (value < bestValue) {
                        bestValue = value;
                        bestMove = move;
                    }
                }
            }
        }

        return bestMove;
    }

    /*
     * PUBLIC METHODS ==============
     */

    public String toString() {
        return "AlphaBeta(ProfMax=" + depthMax + ", " + (useNegamax ? "Negamax" : "Classic") + ")";
    }

    /**
     * Returns the number of nodes developed during the search
     * @return number of internally visited nodes
     */
    public int getNbNodes() {
        return nbNodes;
    }

    /**
     * Returns the number of leaves visited during the search
     * @return number of leaf nodes
     */
    public int getNbLeaves() {
        return nbLeaves;
    }

    /*
     * PRIVATE METHODS - CLASSIC VERSION ===============
     */

    /**
     * MaxMin recursive method with Alpha-Beta pruning (for the MAX player)
     * @param board current board state
     * @param depth current depth in the search tree
     * @param alpha best value for MAX along the path
     * @param beta best value for MIN along the path
     * @return the best value for the MAX player
     */
    private int maxMinAB(Board board, int depth, int alpha, int beta) {
        nbNodes++;

        // Terminal conditions: game over or max depth reached
        if (board.isGameOver() || depth >= depthMax) {
            nbLeaves++;
            return h.eval(board, playerMaxRole);
        }

        ArrayList<Move> moves = board.possibleMoves(playerMaxRole);

        if (moves == null || moves.isEmpty()) {
            // No possible moves for MAX player, evaluate the board
            nbLeaves++;
            return h.eval(board, playerMaxRole);
        }

        int maxValue = Integer.MIN_VALUE;

        // Explore all possible moves for MAX player
        for (Move move : moves) {
            Board nextBoard = board.play(move, playerMaxRole);
            int value = minMaxAB(nextBoard, depth + 1, alpha, beta);
            maxValue = Math.max(maxValue, value);
            alpha = Math.max(alpha, value);

            // Beta cutoff
            if (alpha >= beta) {
                break; // Pruning
            }
        }

        return maxValue;
    }

    /**
     * MinMax recursive method with Alpha-Beta pruning (for the MIN player)
     * @param board current board state
     * @param depth current depth in the search tree
     * @param alpha best value for MAX along the path
     * @param beta best value for MIN along the path
     * @return the best value for the MIN player (worst for MAX)
     */
    private int minMaxAB(Board board, int depth, int alpha, int beta) {
        nbNodes++;

        // Terminal conditions: game over or max depth reached
        if (board.isGameOver() || depth >= depthMax) {
            nbLeaves++;
            return h.eval(board, playerMaxRole);
        }

        ArrayList<Move> moves = board.possibleMoves(playerMinRole);

        if (moves == null || moves.isEmpty()) {
            // No possible moves for MIN player, evaluate the board
            nbLeaves++;
            return h.eval(board, playerMaxRole);
        }

        int minValue = Integer.MAX_VALUE;

        // Explore all possible moves for MIN player
        for (Move move : moves) {
            Board nextBoard = board.play(move, playerMinRole);
            int value = maxMinAB(nextBoard, depth + 1, alpha, beta);
            minValue = Math.min(minValue, value);
            beta = Math.min(beta, value);

            // Alpha cutoff
            if (alpha >= beta) {
                break; // Pruning
            }
        }

        return minValue;
    }

    /*
     * PRIVATE METHODS - NEGAMAX VERSION ===============
     */

    /**
     * Negamax recursive method with Alpha-Beta pruning
     * @param board current board state
     * @param depth current depth in the search tree
     * @param alpha alpha value for pruning
     * @param beta beta value for pruning
     * @param currentRole the current player's role
     * @return the best value for the current player
     */
    private int negamax(Board board, int depth, int alpha, int beta, Role currentRole) {
        nbNodes++;

        // Terminal conditions: game over or max depth reached
        if (board.isGameOver() || depth >= depthMax) {
            nbLeaves++;
            int eval = h.eval(board, playerMaxRole);
            // Return negated value if current player is MIN
            return currentRole.equals(playerMaxRole) ? eval : -eval;
        }

        ArrayList<Move> moves = board.possibleMoves(currentRole);

        if (moves == null || moves.isEmpty()) {
            // No possible moves, evaluate the board
            nbLeaves++;
            int eval = h.eval(board, playerMaxRole);
            return currentRole.equals(playerMaxRole) ? eval : -eval;
        }

        int maxValue = Integer.MIN_VALUE;
        Role opponentRole = currentRole.equals(playerMaxRole) ? playerMinRole : playerMaxRole;

        // Explore all possible moves for current player
        for (Move move : moves) {
            Board nextBoard = board.play(move, currentRole);
            int value = -negamax(nextBoard, depth + 1, -beta, -alpha, opponentRole);
            maxValue = Math.max(maxValue, value);
            alpha = Math.max(alpha, value);

            // Beta cutoff (pruning)
            if (alpha >= beta) {
                break;
            }
        }

        return maxValue;
    }
}
