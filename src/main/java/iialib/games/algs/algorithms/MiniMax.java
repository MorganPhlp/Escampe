package iialib.games.algs.algorithms;

import iialib.games.algs.GameAlgorithm;
import iialib.games.algs.IHeuristic;
import iialib.games.model.IBoard;
import iialib.games.model.IMove;
import iialib.games.model.IRole;

import java.util.ArrayList;

public class MiniMax<Move extends IMove,Role extends IRole,Board extends IBoard<Move,Role,Board>> implements GameAlgorithm<Move,Role,Board> {

	// Constants
	/** Defaut value for depth limit 
     */
	private final static int DEPTH_MAX_DEFAUT = 4;

	// Attributes
	/** Role of the max player 
     */
	private final Role playerMaxRole;

	/** Role of the min player 
     */
	private final Role playerMinRole;

	/** Algorithm max depth
     */
	private int depthMax = DEPTH_MAX_DEFAUT;

	
	/** Heuristic used by the max player 
     */
	private IHeuristic<Board, Role> h;

	//
	/** number of internally visited (developed) nodes (for stats)
     */
	private int nbNodes;
	
	/** number of leaves nodes (for stats)
     */
	private int nbLeaves;

	// --------- Constructors ---------

	public MiniMax(Role playerMaxRole, Role playerMinRole, IHeuristic<Board, Role> h) {
		this.playerMaxRole = playerMaxRole;
		this.playerMinRole = playerMinRole;
		this.h = h;
	}

	//
	public MiniMax(Role playerMaxRole, Role playerMinRole, IHeuristic<Board, Role> h, int depthMax) {
		this(playerMaxRole, playerMinRole, h);
		this.depthMax = depthMax;
	}

	/*
	 * IAlgo METHODS =============
	 */

	@Override
	public Move bestMove(Board board, Role playerRole) {
		System.out.println("[MiniMax]");

		// Reset statistics
		nbNodes = 0;
		nbLeaves = 0;

        // Get all possible moves for the current player
        ArrayList<Move> moves = board.possibleMoves(playerRole);

        // If no moves are possible, return null
        if(moves == null || moves.isEmpty()) {
            return null;
        }

        Move bestMove = null;

        if(playerRole == playerMaxRole) {
            // MAX player: maximize the value
            int bestValue = Integer.MIN_VALUE;
            for(Move move : moves) {
                Board nextBoard = board.play(move, playerRole);
                int value = minMax(nextBoard, 1);
                if(value > bestValue) {
                    bestValue = value;
                    bestMove = move;
                }
            }
        } else {
            // MIN player: minimize the value
            int bestValue = Integer.MAX_VALUE;
            for(Move move : moves) {
                Board nextBoard = board.play(move, playerRole);
                int value = maxMin(nextBoard, 1);
                if(value < bestValue) {
                    bestValue = value;
                    bestMove = move;
                }
            }
        }

		return bestMove;
	}

	/*
	 * PUBLIC METHODS ==============
	 */

	public String toString() {
		return "MiniMax(ProfMax=" + depthMax + ")";
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
	 * PRIVATE METHODS ===============
	 */

	/**
	 * MaxMin recursive method (for the MAX player)
	 * @param board current board state
	 * @param depth current depth in the search tree
	 * @return the best value for the MAX player
	 */
	private int maxMin(Board board, int depth) {
		nbNodes++;

		// Terminal conditions: game over or max depth reached
		if (board.isGameOver() || depth >= depthMax) {
			nbLeaves++;
			return h.eval(board, playerMaxRole);
		}

		int maxValue = Integer.MIN_VALUE;
        ArrayList<Move> moves = board.possibleMoves(playerMaxRole);

        if(moves == null || moves.isEmpty()) {
            // No possible moves for MAX player, evaluate the board
            nbLeaves++;
            return h.eval(board, playerMaxRole);
        }

		// Explore all possible moves for MAX player
		for (Move move : board.possibleMoves(playerMaxRole)) {
			Board nextBoard = board.play(move, playerMaxRole);
			int value = minMax(nextBoard, depth + 1);
			maxValue = Math.max(maxValue, value);
		}

		return maxValue;
	}

	/**
	 * MinMax recursive method (for the MIN player)
	 * @param board current board state
	 * @param depth current depth in the search tree
	 * @return the best value for the MIN player (worst for MAX)
	 */
	private int minMax(Board board, int depth) {
		nbNodes++;

		// Terminal conditions: game over or max depth reached
		if (board.isGameOver() || depth >= depthMax) {
			nbLeaves++;
			return h.eval(board, playerMaxRole);
		}

		int minValue = Integer.MAX_VALUE;
        ArrayList<Move> moves = board.possibleMoves(playerMinRole);

        if(moves == null || moves.isEmpty()) {
            // No possible moves for MIN player, evaluate the board
            nbLeaves++;
            return h.eval(board, playerMaxRole);
        }

		// Explore all possible moves for MIN player
		for (Move move : board.possibleMoves(playerMinRole)) {
			Board nextBoard = board.play(move, playerMinRole);
			int value = maxMin(nextBoard, depth + 1);
			minValue = Math.min(minValue, value);
		}

		return minValue;
	}
}
