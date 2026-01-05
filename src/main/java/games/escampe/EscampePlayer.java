package games.escampe;

import iialib.games.algs.AIPlayer;
import iialib.games.algs.GameAlgorithm;
import iialib.games.algs.algorithms.AlphaBeta;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EscampePlayer implements IJoueur{

    public static final String PLATEAU_FILE = ".\\data\\plateau.txt";
    private static final String OPENINGS_FILE = ".\\data\\openings.txt";
    private final EscampeBoard board;
    private int myColour;
    private EscampeRole myRole;
    private AIPlayer<EscampeMove, EscampeRole, EscampeBoard> aiPlayer;

    // Stockage des ouvertures pré-calculées
    private String bestBlackOpening = null; // Meilleure ouverture pour les Noirs (premier joueur)
    private final Map<String, String> whiteOpenings = new HashMap<>(); // Réponses Blanches selon placement Noir

    // Constructeur
    public EscampePlayer() {
        board = new EscampeBoard();
    }


    // Charge les ouvertures pré-calculées depuis le fichier openings.txt
    // Format: BLACK:placement (meilleure ouverture Noire)
    //         WHITE:placementNoir:réponse (réponses Blanches)
    private void loadOpenings() {
        try (BufferedReader br = new BufferedReader(new FileReader(OPENINGS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("%")) continue;

                String[] parts = line.split(":");
                if (parts.length < 2) continue;

                if (parts[0].equals("BLACK")) {
                    // Format: BLACK:placement (meilleure ouverture pour les Noirs)
                    bestBlackOpening = parts[1];
                } else if (parts[0].equals("WHITE") && parts.length >= 3) {
                    // Format: WHITE:placementNoir:réponseBlanche
                    whiteOpenings.put(parts[1], parts[2]);
                }
            }
            System.out.println("Ouvertures chargées: " + (bestBlackOpening != null ? "1 Noir" : "0 Noir")
                             + ", " + whiteOpenings.size() + " Blancs");
        } catch (IOException e) {
            System.err.println("Impossible de charger les ouvertures: " + e.getMessage());
        }
    }

    // Le joueur initialise son rôle et son algorithme IA en fonction de la couleur assignée.
    @Override
    public void initJoueur(int myColour) {
        // Charger les ouvertures pré-calculées
        loadOpenings();

        this.myColour = myColour;

        // Déterminer le rôle
        if (myColour == BLANC) {
            myRole = EscampeRole.WHITE;
        } else {
            myRole = EscampeRole.BLACK;
        }

        // ---- Nettoyer plateau avant de commencer ----
        board.clearBoard(); // vide les bitboards
        board.clearPlateauFile(PLATEAU_FILE); // vide plateau.txt

        // Donner le rôle adverse
        EscampeRole opponentRole = (myRole == EscampeRole.WHITE) ? EscampeRole.BLACK : EscampeRole.WHITE;

        // Choisir l'heuristique appropriée selon ma couleur
        GameAlgorithm<EscampeMove, EscampeRole, EscampeBoard> algorithm;
        if (myRole == EscampeRole.WHITE) {
            algorithm = new AlphaBeta<>(myRole, opponentRole, EscampeHeuristics.hWhite, 4);
        } else {
            algorithm = new AlphaBeta<>(myRole, opponentRole, EscampeHeuristics.hBlack, 4);
        }

        // Initialiser le joueur IA avec l'algorithme choisi
        aiPlayer = new AIPlayer<>(myRole, algorithm);

        // Charger l'état actuel du plateau depuis le fichier
        board.setFromFile(PLATEAU_FILE);
    }

    // Retourner la couleur du joueur
    @Override
    public int getNumJoueur() {
        return myColour;
    }

    // Choisir le meilleur mouvement à jouer
    @Override
    public String choixMouvement() {
        // Recharger le plateau depuis le fichier pour avoir l'état le plus récent
        board.setFromFile(PLATEAU_FILE);

        // Vérifier si c'est un placement initial
        long myPieces = (myRole == EscampeRole.WHITE)
                        ? (board.getWhiteUnicorn() | board.getWhitePaladins())
                        : (board.getBlackUnicorn() | board.getBlackPaladins());

        if (myPieces == 0L) {
            // C'est un placement initial - utiliser les ouvertures pré-calculées
            String openingMove = useOpeningBook();
            if (openingMove != null) {
                System.out.println("Utilisation de l'ouverture pré-calculée: " + openingMove);
                EscampeMove move = new EscampeMove(openingMove);
                board.playVoid(move, myRole);
                board.saveToFile(PLATEAU_FILE);
                return openingMove;
            }
        }

        // Utiliser l'IA pour trouver le meilleur coup
        EscampeMove bestMove = aiPlayer.bestMove(board);

        if (bestMove == null) {
            return "E";
        }

        // Jouer le coup sur notre copie du plateau (avec playVoid qui est optimisée)
        board.playVoid(bestMove, myRole);

        // Sauvegarder l'état mis à jour
        board.saveToFile(PLATEAU_FILE);

        // Retourner le coup au format string
        System.out.println("[DEBUG] Coup envoyé : " + bestMove);
        return bestMove.toString();
    }

    // Utilise le livre d'ouvertures pour choisir le meilleur placement initial
    private String useOpeningBook() {
        if (myRole == EscampeRole.BLACK) {
            // Pour les Noirs (premier joueur), utiliser la meilleure ouverture précalculée
            if (bestBlackOpening != null) {
                System.out.println("  → Utilisation de l'ouverture Noire du livre");
                return bestBlackOpening;
            }
        } else {
            // Pour les Blancs, chercher une réponse au placement Noir
            long blackPieces = board.getBlackUnicorn() | board.getBlackPaladins();

            if (blackPieces != 0L) {
                // Les Noirs ont déjà placé leurs pièces
                // Reconstruire le placement Noir pour chercher dans le dictionnaire
                String blackPlacement = reconstructPlacementSorted(board, EscampeRole.BLACK);

                System.out.println("  → Placement Noir détecté: " + blackPlacement);

                // Chercher dans le livre d'ouvertures
                if (blackPlacement != null && whiteOpenings.containsKey(blackPlacement)) {
                    System.out.println("  → Réponse Blanche trouvée dans le livre !");
                    return whiteOpenings.get(blackPlacement);
                } else {
                    System.out.println("  → Placement Noir non référencé dans le livre");
                    System.out.println("  → Recherche du meilleur placement Blanc avec AlphaBeta (profondeur 2)...");
                    // Le placement adverse n'est pas dans le livre
                    // Utiliser AlphaBeta pour trouver un bon placement (profondeur réduite pour rapidité)
                    return findBestPlacementWithAlphaBeta();
                }
            }
        }

        return null;
    }

    /**
     * Trouve le meilleur placement initial en utilisant AlphaBeta avec une basse profondeur
     */
    private String findBestPlacementWithAlphaBeta() {
        // Générer tous les placements possibles pour les Blancs
        java.util.ArrayList<EscampeMove> possiblePlacements = generateInitialPlacements(myRole);

        if (possiblePlacements.isEmpty()) {
            return null;
        }

        // Évaluer chaque placement possible avec AlphaBeta et garder le meilleur
        EscampeMove bestPlacement = null;
        int bestScore = Integer.MIN_VALUE;

        System.out.println("  → Évaluation de " + possiblePlacements.size() + " placements avec AlphaBeta...");

        for (EscampeMove placement : possiblePlacements) {
            EscampeBoard testBoard = new EscampeBoard(board);
            testBoard.playVoid(placement, myRole);

            // Utiliser l'aiPlayer existant pour trouver le meilleur coup après ce placement
            EscampeMove bestNextMove = aiPlayer.bestMove(testBoard);

            // Évaluer la position résultante
            int score;
            if (bestNextMove != null && !bestNextMove.isPass()) {
                EscampeBoard afterMove = testBoard.play(bestNextMove, myRole);
                if (myRole == EscampeRole.WHITE) {
                    score = EscampeHeuristics.hWhite.eval(afterMove, myRole);
                } else {
                    score = EscampeHeuristics.hBlack.eval(afterMove, myRole);
                }
            } else {
                // Si pas de coup possible, évaluer directement
                if (myRole == EscampeRole.WHITE) {
                    score = EscampeHeuristics.hWhite.eval(testBoard, myRole);
                } else {
                    score = EscampeHeuristics.hBlack.eval(testBoard, myRole);
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestPlacement = placement;
            }
        }

        System.out.println("  → Meilleur placement trouvé avec score: " + bestScore);
        return bestPlacement != null ? bestPlacement.toString() : null;
    }

    /**
     * Génère tous les placements initiaux possibles pour un rôle donné
     */
    private java.util.ArrayList<EscampeMove> generateInitialPlacements(EscampeRole role) {
        java.util.ArrayList<EscampeMove> placements = new java.util.ArrayList<>();

        // Définir les cases disponibles selon le rôle
        int startRow = (role == EscampeRole.BLACK) ? 0 : 4;
        int endRow = (role == EscampeRole.BLACK) ? 1 : 5;

        // Générer plusieurs placements stratégiques variés
        // Important: diversifier les liserés pour ne pas être bloqué

        // Pattern 1: Distribution équilibrée sur les deux lignes
        placements.add(new EscampeMove(generatePlacementString(startRow, endRow, 0)));

        // Pattern 2: Mix entre les deux lignes - variant 1
        placements.add(new EscampeMove(generatePlacementString(startRow, endRow, 1)));

        // Pattern 3: Mix entre les deux lignes - variant 2
        placements.add(new EscampeMove(generatePlacementString(startRow, endRow, 2)));

        return placements;
    }

    /**
     * Génère une chaîne de placement selon un pattern
     */
    private String generatePlacementString(int row1, int row2, int pattern) {
        StringBuilder sb = new StringBuilder();
        int[] positions;

        // Différents patterns de placement
        switch (pattern) {
            case 0: // Mix équilibré 3-3
                positions = new int[]{row1*6, row1*6+1, row1*6+2, row2*6+3, row2*6+4, row2*6+5};
                break;
            case 1: // Mix 4-2 (majorité row1)
                positions = new int[]{row1*6, row1*6+1, row1*6+3, row1*6+4, row2*6+2, row2*6+5};
                break;
            case 2: // Mix 2-4 (majorité row2)
                positions = new int[]{row1*6+1, row1*6+4, row2*6, row2*6+2, row2*6+3, row2*6+5};
                break;
            default: // Toutes sur row1
                positions = new int[]{row1*6, row1*6+1, row1*6+2, row1*6+3, row1*6+4, row1*6+5};
        }

        for (int i = 0; i < positions.length; i++) {
            sb.append(EscampeBoard.indexToString(positions[i]));
            if (i < positions.length - 1) sb.append("/");
        }

        return sb.toString();
    }

    /**
     * Reconstruit le placement d'un joueur depuis le plateau en TRIANT par index
     * Cela garantit que le format correspond à celui du fichier openings.txt
     */
    private String reconstructPlacementSorted(EscampeBoard board, EscampeRole role) {
        long unicorn = (role == EscampeRole.WHITE) ? board.getWhiteUnicorn() : board.getBlackUnicorn();
        long paladins = (role == EscampeRole.WHITE) ? board.getWhitePaladins() : board.getBlackPaladins();
        long allPieces = unicorn | paladins;

        // Collecter tous les indices des pièces
        java.util.ArrayList<Integer> indices = new java.util.ArrayList<>();
        for (int i = 0; i < 36; i++) {
            if ((allPieces & (1L << i)) != 0) {
                indices.add(i);
            }
        }

        // Trier les indices (ordre croissant)
        java.util.Collections.sort(indices);

        // Construire la chaîne de placement
        StringBuilder placement = new StringBuilder();
        for (int i = 0; i < indices.size(); i++) {
            placement.append(EscampeBoard.indexToString(indices.get(i)));
            if (i < indices.size() - 1) {
                placement.append("/");
            }
        }

        return placement.length() > 0 ? placement.toString() : null;
    }

    @Override
    public void declareLeVainqueur(int colour) {
        if (colour == myColour) {
            System.out.println("Victoire ! J'ai gagné !");
        } else if (colour == 0) { // VIDE = 0 (match nul)
            System.out.println("Match nul.");
        } else {
            System.out.println("Défaite... L'adversaire a gagné.");
        }
    }

    @Override
    public void mouvementEnnemi(String coup) {
        // Recharger le plateau depuis le fichier
        board.setFromFile(PLATEAU_FILE);

        // Créer le mouvement de l'ennemi
        EscampeMove ennemyMove = new EscampeMove(coup);

        // Déterminer le rôle adverse
        EscampeRole ennemyRole = (myRole == EscampeRole.WHITE) ? EscampeRole.BLACK : EscampeRole.WHITE;

        // Appliquer le mouvement ennemi sur notre plateau
        board.playVoid(ennemyMove, ennemyRole);

        // Sauvegarder l'état mis à jour
        board.saveToFile(PLATEAU_FILE);
    }

    @Override
    public String binoName() {
        return "Morgan_Elodie";
    }
}
