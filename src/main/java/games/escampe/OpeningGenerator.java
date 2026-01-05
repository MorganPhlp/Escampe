package games.escampe;

import iialib.games.algs.AIPlayer;
import iialib.games.algs.algorithms.AlphaBeta;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Générateur d'ouvertures pour le jeu Escampe.
 * Ce programme calcule les meilleures ouvertures pour les Noirs (premier joueur)
 * en utilisant AlphaBeta, puis calcule les meilleures réponses Blanches.
 */
public class OpeningGenerator {

    private static final String OPENINGS_FILE = ".\\data\\openings.txt";

    // Paramètres de performance optimisés avec parallélisation
    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors(); // Utiliser tous les cœurs CPU
    private static final int QUICK_FILTER_DEPTH = 2; // Profondeur pour le filtrage rapide
    private static final int FINAL_DEPTH = 4; // Profondeur finale pour les meilleurs candidats
    private static final int INITIAL_CANDIDATES = 100; // Nombre de candidats après filtrage rapide
    private static final int TOP_N_OPENINGS = 5; // Nombre d'ouvertures Noires à calculer
    private static final int TOP_WHITE_RESPONSES = 2; // Nombre de réponses Blanches à calculer par ouverture Noire
    private static final int WHITE_SAMPLE_SIZE = 200; // Nombre de placements blancs à échantillonner

    public static void main(String[] args) {
        System.out.println("=== Générateur d'Ouvertures Escampe (Parallélisé) ===");
        System.out.println("Nombre de threads : " + NUM_THREADS + " (cœurs CPU disponibles)\n");

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
            writer.write("% Généré automatiquement par OpeningGenerator avec AlphaBeta\n");
            writer.write("% Format: BLACK:placement ou WHITE:placementNoir:réponse\n");
            writer.write("% Profondeur AlphaBeta: " + FINAL_DEPTH + " (filtrage: " + QUICK_FILTER_DEPTH + ")\n");
            writer.write("%\n\n");

            // 1. Calculer les TOP_N meilleures ouvertures pour les Noirs (avec recherche exhaustive)
            System.out.println("Calcul des " + TOP_N_OPENINGS + " meilleures ouvertures NOIRES...");
            System.out.println("  Phase 1: Filtrage rapide à profondeur " + QUICK_FILTER_DEPTH);
            System.out.println("  Phase 2: Évaluation finale à profondeur " + FINAL_DEPTH);
            ArrayList<String> topBlackOpenings = findTopBlackOpenings();

            writer.write("% ====================================\n");
            writer.write("% Meilleures ouvertures pour les Noirs (premier joueur)\n");
            writer.write("% ====================================\n\n");

            for (String opening : topBlackOpenings) {
                writer.write("BLACK:" + opening + "\n");
            }
            System.out.println("\n✓ " + topBlackOpenings.size() + " meilleures ouvertures Noires calculées");

            // 2. Calculer les meilleures réponses Blanches pour chaque ouverture Noire
            System.out.println("\nCalcul des meilleures réponses BLANCHES (profondeur " + FINAL_DEPTH + ")...");
            writer.write("\n% ====================================\n");
            writer.write("% Meilleures réponses Blanches\n");
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
     * Un placement initial consiste en 6 pions (1 licorne + 5 paladins) sur les deux premières lignes.
     * Pour les Noirs : lignes 0 et 1 ou 4 et 5 (indices 0-11 ou 24-35)
     * Pour les Blancs : lignes 4 et 5 ou 0 et 1 suivant ce qu'aura choisi l'adversaire (indices 0-11 ou 24-35).
     * Cela représente environ 11000 placements possibles pour les Noirs et 5500 pour les Blancs.
     *
     * @param isBlack true pour les Noirs, false pour les Blancs
     * @param opponentChoiceIsTop true si l'adversaire a choisi le haut (lignes 0 et 1) pour lui-même, false sinon
     * @return liste de tous les placements possibles (format "A1/B2/C1/D2/E1/F2") avec le premier étant la licorne
     */
    private ArrayList<String> generateAllInitialPlacements(boolean isBlack, boolean opponentChoiceIsTop) {
        ArrayList<String> allPlacements = new ArrayList<>();

        // Définir les cases disponibles selon le rôle
        int[] availableCells;
        int[] availableCellsPart2 = new int[0];
        if (isBlack) {
            // Lignes 1 et 2 pour les Noirs (indices 0-11)
            availableCells = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
            availableCellsPart2 = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
        } else {
            // Lignes 5 et 6 pour les Blancs (indices 24-35)
            if(!opponentChoiceIsTop) {
                availableCells = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
            } else {
                availableCells = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
            }
        }

        // Générer toutes les combinaisons de 6 positions parmi 12 possibles
        generateCombinations(availableCells, 0, new int[6], 0, allPlacements);

        if(isBlack) {
            // Ajouter aussi les placements sur les lignes 5 et 6 pour les Noirs
            ArrayList<String> secondHalfPlacements = new ArrayList<>();
            generateCombinations(availableCellsPart2, 0, new int[6], 0, secondHalfPlacements);
            allPlacements.addAll(secondHalfPlacements);
        }

        return allPlacements;
    }

    /**
     * Génère récursivement toutes les combinaisons de six positions pour le placement.
     * La première position (licorne) est ordonnée, mais les cinq autres sont sans ordre.
     *
     * @param cells tableau des indices de cases disponibles
     * @param startIdx index de départ pour la combinaison
     * @param current tableau temporaire pour stocker la combinaison courante
     * @param currentSize taille actuelle de la combinaison
     * @param result liste pour stocker les combinaisons générées
     */
    private void generateCombinations(int[] cells, int startIdx, int[] current, int currentSize, ArrayList<String> result) {
        int n = cells.length;
        if (n < 6) return;

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

        // Si on choisit la première position (licorne)
        if (currentSize == 0) {
            for (int cell : cells) {
                current[0] = cell;
                // Générer toutes les combinaisons (sans ordre) des 5 restantes
                generateCombinations(cells, 0, current, 1, result);
            }
            return;
        }

        // Pour les 5 positions suivantes : combinaisons sans ordre, en ignorant la case de la licorne
        for (int i = startIdx; i < n; i++) {
            if (cells[i] == current[0]) continue; // ne pas réutiliser la licorne
            current[currentSize] = cells[i];
            generateCombinations(cells, i + 1, current, currentSize + 1, result);
        }
    }

    /**
     * Trouve les TOP_N meilleures ouvertures pour les Noirs en utilisant AlphaBeta.
     * Version PARALLÉLISÉE pour exploiter tous les cœurs CPU disponibles.
     * Les Noirs jouent en premier sur un plateau vide (sans adversaire).
     * Pour chaque placement Noir, on évalue contre un échantillon de placements Blancs.
     * On utilise une approche en deux phases pour optimiser les performances :
     * Phase 1: Filtrage rapide avec profondeur faible
     * Phase 2: Évaluation approfondie des meilleurs candidats
     *
     * @return liste des x meilleures ouvertures Noires
     */
    private ArrayList<String> findTopBlackOpenings() {
        System.out.println("\n=== RECHERCHE PARALLÉLISÉE DES MEILLEURES OUVERTURES NOIRES ===\n");

        // Générer tous les placements possibles pour les Noirs et les Blancs
        ArrayList<String> allBlackPlacements = generateAllInitialPlacements(true, false);
        ArrayList<String> allWhitePlacementsTop = generateAllInitialPlacements(false, false);
        ArrayList<String> allWhitePlacementsBottom = generateAllInitialPlacements(false, true);

        System.out.println("  Total placements Noirs: " + allBlackPlacements.size());
        System.out.println("  Total placements Blancs (top): " + allWhitePlacementsTop.size());
        System.out.println("  Total placements Blancs (bottom): " + allWhitePlacementsBottom.size());
        System.out.println("  Threads parallèles: " + NUM_THREADS);
        System.out.println();

        // PHASE 1: Filtrage rapide PARALLÉLISÉ
        System.out.println("=== PHASE 1: Filtrage rapide parallélisé (profondeur " + QUICK_FILTER_DEPTH + ") ===\n");
        final long startTimePhase1 = System.currentTimeMillis();

        AtomicInteger progressCounter = new AtomicInteger(0);
        int totalPlacements = allBlackPlacements.size();

        // Utiliser un Stream parallèle pour évaluer tous les placements en parallèle
        Map<String, Integer> quickScores = allBlackPlacements.parallelStream()
            .collect(Collectors.toConcurrentMap(
                blackPlacement -> blackPlacement,
                blackPlacement -> {
                    int score = quickEvaluateBlackPlacement(blackPlacement,
                                                            allWhitePlacementsTop,
                                                            allWhitePlacementsBottom);

                    // Afficher la progression de manière thread-safe
                    int current = progressCounter.incrementAndGet();
                    if (current % 500 == 0 || current == totalPlacements) {
                        synchronized (System.out) {
                            long elapsed = System.currentTimeMillis() - startTimePhase1;
                            long remaining = (elapsed * (totalPlacements - current)) / current;
                            System.out.printf("  [%d/%d] %.1f%% | Temps restant: %s\n",
                                current, totalPlacements, (100.0 * current / totalPlacements), formatTime(remaining));
                        }
                    }

                    return score;
                }
            ));

        // Trier et garder les INITIAL_CANDIDATES meilleurs
        ArrayList<Map.Entry<String, Integer>> sortedQuick = new ArrayList<>(quickScores.entrySet());
        sortedQuick.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        ArrayList<String> candidates = new ArrayList<>();
        for (int i = 0; i < Math.min(INITIAL_CANDIDATES, sortedQuick.size()); i++) {
            candidates.add(sortedQuick.get(i).getKey());
        }

        System.out.println("\n  ✓ Phase 1 terminée en " + formatTime(System.currentTimeMillis() - startTimePhase1));
        System.out.println("  → " + candidates.size() + " candidats sélectionnés\n");

        // PHASE 2: Évaluation approfondie PARALLÉLISÉE des meilleurs candidats
        System.out.println("=== PHASE 2: Évaluation approfondie parallélisée (profondeur " + FINAL_DEPTH + ") ===\n");
        final long startTimePhase2 = System.currentTimeMillis();
        progressCounter.set(0);

        // Utiliser un Stream parallèle pour la phase 2
        Map<String, Integer> finalScores = candidates.parallelStream()
            .collect(Collectors.toConcurrentMap(
                blackPlacement -> blackPlacement,
                blackPlacement -> {
                    int score = deepEvaluateBlackPlacement(blackPlacement,
                                                           allWhitePlacementsTop,
                                                           allWhitePlacementsBottom);

                    int current = progressCounter.incrementAndGet();
                    synchronized (System.out) {
                        System.out.printf("  [%d/%d] %s → score: %d\n",
                            current, candidates.size(), blackPlacement, score);
                    }

                    return score;
                }
            ));

        System.out.println("\n  ✓ Phase 2 terminée en " + formatTime(System.currentTimeMillis() - startTimePhase2));

        // Trier et retourner les TOP_N meilleures
        ArrayList<Map.Entry<String, Integer>> sortedFinal = new ArrayList<>(finalScores.entrySet());
        sortedFinal.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        System.out.println("\n=== TOP " + TOP_N_OPENINGS + " MEILLEURES OUVERTURES NOIRES ===\n");
        ArrayList<String> topOpenings = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_N_OPENINGS, sortedFinal.size()); i++) {
            topOpenings.add(sortedFinal.get(i).getKey());
            System.out.printf("  #%d: %s → score: %d\n",
                i+1, sortedFinal.get(i).getKey(), sortedFinal.get(i).getValue());
        }

        return topOpenings;
    }

    /**
     * Évaluation rapide d'un placement noir (Phase 1)
     * Utilise une heuristique simple et teste contre un échantillon limité de placements blancs
     */
    private int quickEvaluateBlackPlacement(String blackPlacement,
                                            ArrayList<String> whitePlacementsTop,
                                            ArrayList<String> whitePlacementsBottom) {
        // Déterminer si les noirs sont en haut ou en bas
        boolean blackIsTop = (blackPlacement.charAt(1) == '1' || blackPlacement.charAt(1) == '2');
        ArrayList<String> relevantWhitePlacements = blackIsTop ? whitePlacementsBottom : whitePlacementsTop;

        // Échantillonner seulement quelques placements blancs (tous les N-ièmes)
        int sampleStep = Math.max(1, relevantWhitePlacements.size() / 20); // ~20 échantillons
        int worstScore = Integer.MAX_VALUE;

        EscampeBoard boardWithBlack = new EscampeBoard();
        boardWithBlack.clearBoard();
        EscampeMove blackMove = new EscampeMove(blackPlacement);
        boardWithBlack.playVoid(blackMove, EscampeRole.BLACK);

        for (int i = 0; i < relevantWhitePlacements.size(); i += sampleStep) {
            String whitePlacement = relevantWhitePlacements.get(i);

            EscampeBoard fullBoard = new EscampeBoard(boardWithBlack);
            EscampeMove whiteMove = new EscampeMove(whitePlacement);
            fullBoard.playVoid(whiteMove, EscampeRole.WHITE);
            fullBoard.switchTurn();

            // Évaluation rapide à profondeur 1
            int score = evaluatePositionWithAlphaBeta(fullBoard, EscampeRole.BLACK, QUICK_FILTER_DEPTH);
            worstScore = Math.min(worstScore, score);
        }

        return worstScore;
    }

    /**
     * Évaluation approfondie d'un placement noir (Phase 2)
     * Teste contre un échantillon plus large avec une profondeur plus grande
     */
    private int deepEvaluateBlackPlacement(String blackPlacement,
                                           ArrayList<String> whitePlacementsTop,
                                           ArrayList<String> whitePlacementsBottom) {
        boolean blackIsTop = (blackPlacement.charAt(1) == '1' || blackPlacement.charAt(1) == '2');
        ArrayList<String> relevantWhitePlacements = blackIsTop ? whitePlacementsBottom : whitePlacementsTop;

        // Échantillonner WHITE_SAMPLE_SIZE placements blancs uniformément
        int sampleStep = Math.max(1, relevantWhitePlacements.size() / WHITE_SAMPLE_SIZE);
        int worstScore = Integer.MAX_VALUE;

        EscampeBoard boardWithBlack = new EscampeBoard();
        boardWithBlack.clearBoard();
        EscampeMove blackMove = new EscampeMove(blackPlacement);
        boardWithBlack.playVoid(blackMove, EscampeRole.BLACK);

        for (int i = 0; i < relevantWhitePlacements.size(); i += sampleStep) {
            String whitePlacement = relevantWhitePlacements.get(i);

            EscampeBoard fullBoard = new EscampeBoard(boardWithBlack);
            EscampeMove whiteMove = new EscampeMove(whitePlacement);
            fullBoard.playVoid(whiteMove, EscampeRole.WHITE);
            fullBoard.switchTurn();

            // Évaluation approfondie à profondeur finale
            int score = evaluatePositionWithAlphaBeta(fullBoard, EscampeRole.BLACK, FINAL_DEPTH);
            worstScore = Math.min(worstScore, score);
        }

        return worstScore;
    }

    /**
     * Évalue une position avec AlphaBeta en simulant des coups à la profondeur donnée
     *
     * @param board le plateau à évaluer
     * @param role le rôle du joueur à évaluer (Noir ou Blanc)
     * @param depth la profondeur de recherche AlphaBeta
     * @return le score évalué pour le joueur donné
     */
    private int evaluatePositionWithAlphaBeta(EscampeBoard board, EscampeRole role, int depth) {
        // Créer une copie pour ne pas modifier le plateau original
        EscampeBoard testBoard = new EscampeBoard(board);

        // Créer l'algorithme AlphaBeta avec la profondeur configurée
        var heuristic = (role == EscampeRole.BLACK) ? EscampeHeuristics.hBlack : EscampeHeuristics.hWhite;
        var roleOpp = (role == EscampeRole.BLACK) ? EscampeRole.WHITE : EscampeRole.BLACK;
        AlphaBeta<EscampeMove, EscampeRole, EscampeBoard> alphabeta =
                new AlphaBeta<>(role, roleOpp, heuristic, depth);

        // Créer le joueur IA qui utilise AlphaBeta
        AIPlayer<EscampeMove, EscampeRole, EscampeBoard> ai = new AIPlayer<>(role, alphabeta);

        // Lancer l'évaluation AlphaBeta
        EscampeMove bestMove = ai.bestMove(testBoard);

        // Après l'appel à bestMove, on évalue la position résultante
        // Si on peut jouer le meilleur coup, on évalue la position après ce coup.
        if (bestMove != null && !bestMove.isPass()) {
            EscampeBoard afterMove = testBoard.play(bestMove, role);
            return heuristic.eval(afterMove, role);
        }
        else if (bestMove == null) {
            System.out.println(" [Attention !] Aucun coup possible pour le rôle " + role + " lors de l'évaluation.");
        }

        // Sinon, évaluer la position actuelle
        return heuristic.eval(testBoard, role);
    }

    /**
     * Formate un temps en millisecondes en format lisible
     *
     * @param millis temps en millisecondes
     * @return chaîne formatée (ex: "1h 23min", "45min 30s", "15s")
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
     * Version PARALLÉLISÉE avec une approche en deux phases.
     *
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
        boolean blackIsTop = (blackOpening.charAt(1) == '1' || blackOpening.charAt(1) == '2');
        ArrayList<String> allWhitePlacements = generateAllInitialPlacements(false, blackIsTop);

        System.out.println("    Phase 1: Filtrage rapide parallélisé sur " + allWhitePlacements.size() + " placements...");

        // Phase 1: Filtrage rapide PARALLÉLISÉ
        int sampleStep = Math.max(1, allWhitePlacements.size() / 300); // Échantillonner ~300 placements
        ArrayList<String> sampledPlacements = new ArrayList<>();
        for (int i = 0; i < allWhitePlacements.size(); i += sampleStep) {
            sampledPlacements.add(allWhitePlacements.get(i));
        }

        Map<String, Integer> quickScores = sampledPlacements.parallelStream()
            .collect(Collectors.toConcurrentMap(
                whitePlacement -> whitePlacement,
                whitePlacement -> {
                    EscampeBoard fullBoard = new EscampeBoard(boardWithBlack);
                    EscampeMove whiteMove = new EscampeMove(whitePlacement);
                    fullBoard.playVoid(whiteMove, EscampeRole.WHITE);
                    fullBoard.switchTurn();

                    return evaluatePositionWithAlphaBeta(fullBoard, EscampeRole.WHITE, QUICK_FILTER_DEPTH);
                }
            ));

        // Sélectionner les meilleurs candidats
        ArrayList<Map.Entry<String, Integer>> sortedQuick = new ArrayList<>(quickScores.entrySet());
        sortedQuick.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        int candidatesCount = Math.min(30, sortedQuick.size()); // Garder top 30 pour phase 2
        ArrayList<String> candidates = new ArrayList<>();
        for (int i = 0; i < candidatesCount; i++) {
            candidates.add(sortedQuick.get(i).getKey());
        }

        System.out.println("    Phase 2: Évaluation approfondie parallélisée de " + candidates.size() + " candidats...");

        // Phase 2: Évaluation approfondie PARALLÉLISÉE
        Map<String, Integer> finalScores = candidates.parallelStream()
            .collect(Collectors.toConcurrentMap(
                whitePlacement -> whitePlacement,
                whitePlacement -> {
                    EscampeBoard fullBoard = new EscampeBoard(boardWithBlack);
                    EscampeMove whiteMove = new EscampeMove(whitePlacement);
                    fullBoard.playVoid(whiteMove, EscampeRole.WHITE);
                    fullBoard.switchTurn();

                    return evaluatePositionWithAlphaBeta(fullBoard, EscampeRole.WHITE, FINAL_DEPTH);
                }
            ));

        // Trier par score décroissant (meilleur pour les Blancs)
        ArrayList<Map.Entry<String, Integer>> sortedFinal = new ArrayList<>(finalScores.entrySet());
        sortedFinal.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // Garder les TOP_WHITE_RESPONSES meilleures
        ArrayList<String> topResponses = new ArrayList<>();
        for (int i = 0; i < Math.min(TOP_WHITE_RESPONSES, sortedFinal.size()); i++) {
            topResponses.add(sortedFinal.get(i).getKey());
            System.out.printf("    #%d: %s → score: %d\n",
                i+1, sortedFinal.get(i).getKey(), sortedFinal.get(i).getValue());
        }

        return topResponses;
    }
}

