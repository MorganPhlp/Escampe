package games.escampe;

import iialib.games.algs.AIPlayer;
import iialib.games.algs.algorithms.AlphaBeta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Générateur d'ouvertures pour le jeu Escampe.
 * Ce programme calcule les meilleures ouvertures pour les Noirs (premier joueur)
 * en utilisant AlphaBeta, puis calcule les meilleures réponses Blanches.
 */
public class OpeningGenerator {

    private static final String OPENINGS_FILE = ".\\data\\openings.txt";
    private static final int OPENING_DEPTH = 4; // Profondeur AlphaBeta (4=1-2h, 5=3-5h, 6=6-24h)
    private static final int TOP_N_OPENINGS = 5; // Nombre d'ouvertures Noires à calculer
    private static final int TOP_WHITE_RESPONSES = 10; // Nombre de réponses Blanches à calculer par ouverture Noire

    public static void main(String[] args) {
        System.out.println("=== Générateur d'Ouvertures Escampe ===\n");

        OpeningGenerator generator = new OpeningGenerator();
        generator.generateOpenings();

        System.out.println("\n=== Génération terminée ===");
        System.out.println("Les ouvertures ont été sauvegardées dans : " + OPENINGS_FILE);
    }

    /**
     * Génère les ouvertures et les sauvegarde dans le fichier openings.txt
     */
    public void generateOpenings() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OPENINGS_FILE))) {
            writer.write("% Fichier d'ouvertures pour le jeu Escampe\n");
            writer.write("% Généré automatiquement par OpeningGenerator avec AlphaBeta exhaustif\n");
            writer.write("% Format: BLACK:placement ou WHITE:placementNoir:réponse\n");
            writer.write("% NOTE: Les NOIRS jouent en PREMIER\n");
            writer.write("% Profondeur AlphaBeta: " + OPENING_DEPTH + "\n");
            writer.write("% Recherche exhaustive minimax\n");
            writer.write("%\n\n");

            // 1. Calculer les TOP_N meilleures ouvertures pour les Noirs (avec recherche exhaustive)
            System.out.println("Calcul des " + TOP_N_OPENINGS + " meilleures ouvertures NOIRES (profondeur " + OPENING_DEPTH + ")...");
            ArrayList<String> topBlackOpenings = findTopBlackOpenings();

            writer.write("% ====================================\n");
            writer.write("% Meilleures ouvertures pour les Noirs (premier joueur)\n");
            writer.write("% Évaluées avec recherche minimax exhaustive\n");
            writer.write("% ====================================\n\n");

            for (String opening : topBlackOpenings) {
                writer.write("BLACK:" + opening + "\n");
            }
            System.out.println("\n✓ " + topBlackOpenings.size() + " meilleures ouvertures Noires calculées");

            // 2. Calculer les meilleures réponses Blanches pour chaque ouverture Noire
            System.out.println("\nCalcul des meilleures réponses BLANCHES (profondeur " + OPENING_DEPTH + ")...");
            writer.write("\n% ====================================\n");
            writer.write("% Meilleures réponses Blanches\n");
            writer.write("% Format: WHITE:ouvertureNoire:réponseBlanche\n");
            writer.write("% ====================================\n\n");

            int totalResponses = 0;
            for (String blackOpening : topBlackOpenings) {
                ArrayList<String> whiteResponses = findBestWhiteResponses(blackOpening);

                for (String whiteResponse : whiteResponses) {
                    writer.write("WHITE:" + blackOpening + ":" + whiteResponse + "\n");
                    totalResponses++;
                }
            }
            System.out.println("✓ " + totalResponses + " réponses Blanches calculées");

        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Génère tous les placements initiaux possibles pour un joueur.
     * Un placement initial consiste en 6 pions (1 licorne + 5 paladins) sur les 2 premières lignes.
     * Pour les Noirs: lignes 0 et 1 (indices 0-11)
     * Pour les Blancs: lignes 4 et 5 (indices 24-35)
     */
    private ArrayList<String> generateAllInitialPlacements(boolean isBlack) {
        ArrayList<String> allPlacements = new ArrayList<>();

        // Définir les cases disponibles selon le rôle
        int[] availableCells;
        if (isBlack) {
            // Lignes 1 et 2 pour les Noirs (indices 0-11)
            availableCells = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        } else {
            // Lignes 5 et 6 pour les Blancs (indices 24-35)
            availableCells = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
        }

        // Générer toutes les combinaisons de 6 positions parmi 12 possibles
        generateCombinations(availableCells, 0, new int[6], 0, allPlacements);

        return allPlacements;
    }

    /**
     * Génère récursivement toutes les combinaisons de 6 positions.
     */
    private void generateCombinations(int[] cells, int startIdx, int[] current, int currentSize, ArrayList<String> result) {
        if (currentSize == 6) {
            // Convertir les indices en format "A1/B2/C1/D2/E1/F2"
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                int idx = current[i];
                char col = (char) ('A' + (idx % 6));
                char row = (char) ('1' + (idx / 6));
                sb.append(col).append(row);
                if (i < 5) sb.append('/');
            }
            result.add(sb.toString());
            return;
        }

        for (int i = startIdx; i < cells.length; i++) {
            current[currentSize] = cells[i];
            generateCombinations(cells, i + 1, current, currentSize + 1, result);
        }
    }

    /**
     * Trouve les TOP_N meilleures ouvertures pour les Noirs en utilisant AlphaBeta.
     * Les Noirs jouent en premier sur un plateau vide (sans adversaire).
     * Pour chaque placement Noir, on évalue contre TOUS les placements Blancs possibles
     * et on utilise AlphaBeta pour obtenir un score robuste.
     *
     * Cette méthode est exhaustive et peut prendre beaucoup de temps (~2h).
     *
     * @return liste des x meilleures ouvertures Noires
     */
    private ArrayList<String> findTopBlackOpenings() {
        System.out.println("\n=== RECHERCHE EXHAUSTIVE DES MEILLEURES OUVERTURES NOIRES ===\n");

        // Générer tous les placements possibles pour les Noirs et les Blancs
        ArrayList<String> allBlackPlacements = generateAllInitialPlacements(true);
        ArrayList<String> allWhitePlacements = generateAllInitialPlacements(false);

        System.out.println("  " + allBlackPlacements.size() + " placements Noirs × " +
                          allWhitePlacements.size() + " placements Blancs = " +
                          (allBlackPlacements.size() * allWhitePlacements.size()) + " positions à évaluer");
        System.out.println("  Profondeur AlphaBeta: " + OPENING_DEPTH);
        System.out.println("  Estimation: 1-2h de calcul\n");

        // Évaluer chaque placement Noir avec une approche minimax
        Map<String, Integer> placementScores = new HashMap<>();
        int totalPositions = allBlackPlacements.size();
        int positionCount = 0;
        long startTime = System.currentTimeMillis();
        long totalEvaluations = 0; // Compteur du nombre total d'évaluations

        for (String blackPlacementStr : allBlackPlacements) {
            positionCount++;

            // 1. Placer les Noirs
            EscampeBoard boardWithBlack = new EscampeBoard();
            boardWithBlack.clearBoard();
            EscampeMove blackMove = new EscampeMove(blackPlacementStr);
            boardWithBlack.playVoid(blackMove, EscampeRole.BLACK);

            // 2. Pour ce placement Noir, trouver le PIRE cas (meilleur placement Blanc)
            // C'est une approche minimax: les Noirs maximisent, les Blancs minimisent le score Noir
            int worstCaseScore = Integer.MAX_VALUE; // Pire cas du point de vue des Noirs
            int whiteCount = 0; // Compteur pour ce placement Noir

            for (String whitePlacementStr : allWhitePlacements) {
                whiteCount++;
                totalEvaluations++;

                EscampeBoard fullBoard = new EscampeBoard(boardWithBlack);
                EscampeMove whiteMove = new EscampeMove(whitePlacementStr);
                fullBoard.playVoid(whiteMove, EscampeRole.WHITE);

                // 3. Évaluer cette position complète avec AlphaBeta
                // Les Noirs commencent à jouer (après les placements)
                fullBoard.switchTurn(); // S'assurer que c'est au tour des Blancs (ils jouent en premier après placement)

                // Évaluer la position en faisant jouer AlphaBeta
                // On simule quelques coups pour obtenir une évaluation robuste
                int score = evaluatePositionWithAlphaBeta(fullBoard, EscampeRole.BLACK);

                // Minimax: les Blancs choisissent le placement qui minimise le score des Noirs
                if (score < worstCaseScore) {
                    worstCaseScore = score;
                }
            }

            // Le score de ce placement Noir est son score dans le pire cas
            placementScores.put(blackPlacementStr, worstCaseScore);

            // Affichage de la progression
            if (positionCount % 10 == 0 || positionCount == totalPositions) {
                long elapsed = System.currentTimeMillis() - startTime;
                long avgTimePerPosition = elapsed / positionCount;
                long remainingTime = avgTimePerPosition * (totalPositions - positionCount);

                System.out.printf("  [%d/%d] %s → score minimax: %d (%d positions Blanches évaluées)\n",
                    positionCount, totalPositions, blackPlacementStr, worstCaseScore, whiteCount);
                System.out.printf("           Total: %d évaluations | Temps restant: %s\n",
                    totalEvaluations, formatTime(remainingTime));
            }
        }

        System.out.println("\n  ✓ Tous les placements Noirs évalués !");
        System.out.println("  Total d'évaluations: " + totalEvaluations + " positions");
        System.out.println("  Temps total: " + formatTime(System.currentTimeMillis() - startTime));

        // Trier et retourner les TOP_N meilleures
        ArrayList<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(placementScores.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue())); // Tri décroissant

        System.out.println("\n=== TOP " + TOP_N_OPENINGS + " MEILLEURES OUVERTURES NOIRES ===\n");
        ArrayList<String> topOpenings = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_N_OPENINGS, sortedEntries.size()); i++) {
            topOpenings.add(sortedEntries.get(i).getKey());
            System.out.printf("  #%d: %s → score minimax: %d\n",
                i+1, sortedEntries.get(i).getKey(), sortedEntries.get(i).getValue());
        }

        return topOpenings;
    }

    /**
     * Évalue une position avec AlphaBeta en simulant des coups à la profondeur OPENING_DEPTH
     * Cette méthode utilise VRAIMENT AlphaBeta pour une évaluation robuste.
     */
    private int evaluatePositionWithAlphaBeta(EscampeBoard board, EscampeRole role) {
        // Créer une copie pour ne pas modifier le plateau original
        EscampeBoard testBoard = new EscampeBoard(board);

        // Créer l'algorithme AlphaBeta avec la profondeur configurée
        AlphaBeta<EscampeMove, EscampeRole, EscampeBoard> alphabeta =
            new AlphaBeta<>(EscampeRole.BLACK, EscampeRole.WHITE, EscampeHeuristics.hBlack, OPENING_DEPTH);

        // Créer le joueur AI qui utilise AlphaBeta
        AIPlayer<EscampeMove, EscampeRole, EscampeBoard> ai =
            new AIPlayer<>(role, alphabeta);

        // Lancer l'évaluation AlphaBeta (cela va explorer l'arbre de jeu en profondeur)
        EscampeMove bestMove = ai.bestMove(testBoard);

        // Après l'appel à bestMove, on évalue la position résultante
        // Si on peut jouer le meilleur coup, on évalue la position après ce coup
        if (bestMove != null && !bestMove.isPass()) {
            EscampeBoard afterMove = testBoard.play(bestMove, role);
            return EscampeHeuristics.hBlack.eval(afterMove, role);
        }

        // Sinon, évaluer la position actuelle
        return EscampeHeuristics.hBlack.eval(testBoard, role);
    }

    /**
     * Formate un temps en millisecondes en format lisible
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %02dmin", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dmin %02ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Trouve les meilleures réponses Blanches pour une ouverture Noire donnée.
     * Utilise AlphaBeta pour évaluer toutes les réponses possibles.
     * @param blackOpening le placement Noir (format "C1/A1/B2/D2/E1/F2")
     * @return les TOP_WHITE_RESPONSES meilleures réponses Blanches
     */
    private ArrayList<String> findBestWhiteResponses(String blackOpening) {
        System.out.println("\n  Calcul des meilleures réponses Blanches pour: " + blackOpening);

        // Créer un plateau avec le placement Noir
        EscampeBoard boardWithBlack = new EscampeBoard();
        boardWithBlack.clearBoard();
        EscampeMove blackMove = new EscampeMove(blackOpening);
        boardWithBlack.playVoid(blackMove, EscampeRole.BLACK);

        // Générer tous les placements Blancs possibles
        ArrayList<String> allWhitePlacements = generateAllInitialPlacements(false);
        Map<String, Integer> whitePlacementScores = new HashMap<>();

        // Évaluer chaque placement Blanc
        for (String whitePlacementStr : allWhitePlacements) {
            EscampeBoard fullBoard = new EscampeBoard(boardWithBlack);
            EscampeMove whiteMove = new EscampeMove(whitePlacementStr);
            fullBoard.playVoid(whiteMove, EscampeRole.WHITE);

            // Évaluer du point de vue des Blancs
            fullBoard.switchTurn(); // S'assurer que c'est au tour des Blancs
            int score = EscampeHeuristics.hWhite.eval(fullBoard, EscampeRole.WHITE);

            whitePlacementScores.put(whitePlacementStr, score);
        }

        // Trier par score décroissant (meilleur pour les Blancs)
        ArrayList<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(whitePlacementScores.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // Garder les TOP_WHITE_RESPONSES meilleures
        ArrayList<String> topResponses = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_WHITE_RESPONSES, sortedEntries.size()); i++) {
            topResponses.add(sortedEntries.get(i).getKey());
            System.out.printf("    #%d: %s → score: %d\n",
                i+1, sortedEntries.get(i).getKey(), sortedEntries.get(i).getValue());
        }

        return topResponses;
    }
}

