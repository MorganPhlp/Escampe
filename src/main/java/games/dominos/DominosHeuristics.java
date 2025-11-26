package games.dominos;

import iialib.games.algs.IHeuristic;

public class DominosHeuristics {

	public static IHeuristic<DominosBoard,DominosRole>  hVertical = (board,role) -> {

		// Différence entre les coups possibles pour Vertical et Horizontal
		int verticalMoves = board.nbVerticalMoves();
		int horizontalMoves = board.nbHorizontalMoves();

		return verticalMoves - horizontalMoves;
	};


	public static IHeuristic<DominosBoard,DominosRole> hHorizontal = (board,role) -> {

		// Différence entre les coups possibles pour Horizontal et Vertical
		int horizontalMoves = board.nbHorizontalMoves();
		int verticalMoves = board.nbVerticalMoves();

		return horizontalMoves - verticalMoves;
	};
   
}
