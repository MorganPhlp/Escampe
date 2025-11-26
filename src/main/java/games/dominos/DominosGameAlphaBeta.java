package games.dominos;

import java.util.ArrayList;

import iialib.games.algs.AIPlayer;
import iialib.games.algs.AbstractGame;
import iialib.games.algs.GameAlgorithm;
import iialib.games.algs.algorithms.AlphaBeta;
import iialib.games.algs.algorithms.MiniMax;

public class DominosGameAlphaBeta extends AbstractGame<DominosMove, DominosRole, DominosBoard> {

	DominosGameAlphaBeta(ArrayList<AIPlayer<DominosMove, DominosRole, DominosBoard>> players, DominosBoard board) {
		super(players, board);
	}

	public static void main(String[] args) {

		DominosRole roleV = DominosRole.VERTICAL;
		DominosRole roleH = DominosRole.HORIZONTAL;

		// Test version classique AlphaBeta
		System.out.println("=== Test AlphaBeta version Classique ===\n");

		GameAlgorithm<DominosMove, DominosRole, DominosBoard> algV =
			new AlphaBeta<>(roleV, roleH, DominosHeuristics.hVertical, 4, false); // Classique

		GameAlgorithm<DominosMove, DominosRole, DominosBoard> algH =
			new AlphaBeta<>(roleH, roleV, DominosHeuristics.hHorizontal, 4, false); // Classique

		AIPlayer<DominosMove, DominosRole, DominosBoard> playerV =
			new AIPlayer<>(roleV, algV);

		AIPlayer<DominosMove, DominosRole, DominosBoard> playerH =
			new AIPlayer<>(roleH, algH);

		ArrayList<AIPlayer<DominosMove, DominosRole, DominosBoard>> players = new ArrayList<>();
		players.add(playerV);
		players.add(playerH);

		DominosBoard initialBoard = new DominosBoard();

		DominosGameAlphaBeta game = new DominosGameAlphaBeta(players, initialBoard);
		game.runGame();

		// Afficher les statistiques
		if (algV instanceof AlphaBeta) {
			AlphaBeta<DominosMove, DominosRole, DominosBoard> ab =
				(AlphaBeta<DominosMove, DominosRole, DominosBoard>) algV;
			System.out.println("\nStatistiques VERTICAL (dernière recherche):");
			System.out.println("  Noeuds développés: " + ab.getNbNodes());
			System.out.println("  Feuilles visitées: " + ab.getNbLeaves());
		}

		System.out.println("\n=== Test AlphaBeta version Negamax ===\n");

		// Test version Negamax
		GameAlgorithm<DominosMove, DominosRole, DominosBoard> algV2 =
			new AlphaBeta<>(roleV, roleH, DominosHeuristics.hVertical, 4, true); // Negamax

		GameAlgorithm<DominosMove, DominosRole, DominosBoard> algH2 =
			new AlphaBeta<>(roleH, roleV, DominosHeuristics.hHorizontal, 4, true); // Negamax

		AIPlayer<DominosMove, DominosRole, DominosBoard> playerV2 =
			new AIPlayer<>(roleV, algV2);

		AIPlayer<DominosMove, DominosRole, DominosBoard> playerH2 =
			new AIPlayer<>(roleH, algH2);

		ArrayList<AIPlayer<DominosMove, DominosRole, DominosBoard>> players2 = new ArrayList<>();
		players2.add(playerV2);
		players2.add(playerH2);

		DominosBoard initialBoard2 = new DominosBoard();

		DominosGameAlphaBeta game2 = new DominosGameAlphaBeta(players2, initialBoard2);
		game2.runGame();

		// Afficher les statistiques
		if (algV2 instanceof AlphaBeta) {
			AlphaBeta<DominosMove, DominosRole, DominosBoard> ab =
				(AlphaBeta<DominosMove, DominosRole, DominosBoard>) algV2;
			System.out.println("\nStatistiques VERTICAL Negamax (dernière recherche):");
			System.out.println("  Noeuds développés: " + ab.getNbNodes());
			System.out.println("  Feuilles visitées: " + ab.getNbLeaves());
		}

		System.out.println("\n=== Comparaison MiniMax vs AlphaBeta ===\n");

		// Test MiniMax pour comparaison
		GameAlgorithm<DominosMove, DominosRole, DominosBoard> algVMinimax =
			new MiniMax<>(roleV, roleH, DominosHeuristics.hVertical, 4);

		GameAlgorithm<DominosMove, DominosRole, DominosBoard> algHMinimax =
			new MiniMax<>(roleH, roleV, DominosHeuristics.hHorizontal, 4);

		AIPlayer<DominosMove, DominosRole, DominosBoard> playerVMinimax =
			new AIPlayer<>(roleV, algVMinimax);

		AIPlayer<DominosMove, DominosRole, DominosBoard> playerHMinimax =
			new AIPlayer<>(roleH, algHMinimax);

		ArrayList<AIPlayer<DominosMove, DominosRole, DominosBoard>> playersMinimax = new ArrayList<>();
		playersMinimax.add(playerVMinimax);
		playersMinimax.add(playerHMinimax);

		DominosBoard initialBoard3 = new DominosBoard();

		DominosGameAlphaBeta game3 = new DominosGameAlphaBeta(playersMinimax, initialBoard3);
		game3.runGame();

		// Afficher les statistiques de comparaison
		if (algVMinimax instanceof MiniMax) {
			MiniMax<DominosMove, DominosRole, DominosBoard> mm =
				(MiniMax<DominosMove, DominosRole, DominosBoard>) algVMinimax;
			System.out.println("\nStatistiques VERTICAL MiniMax (dernière recherche):");
			System.out.println("  Noeuds développés: " + mm.getNbNodes());
			System.out.println("  Feuilles visitées: " + mm.getNbLeaves());
		}
	}

}

