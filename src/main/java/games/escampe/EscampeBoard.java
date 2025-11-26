package games.escampe;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class EscampeBoard implements Partie1 {

    // ------------ Constantes ------------

    // Masques pour les liserés (Bit 0 = case libre, Bit 1 = liseré)
    private static final long lISERE_1 = 0b100010_010100_001010_010001_101001_000100L; // 0b pour binaire et L pour long
    private static final long lISERE_2 = 0b011001_000001_100100_100100_000001_011001L;
    private static final long lISERE_3 = 0b000100_101010_010001_001010_010100_100010L;

    private static final String[] COORD_CACHE = new String[36]; // Cache des coordonnées des cases pour éviter de les recalculer

    // ------------ Variables d'etat ------------

    private static long whitePaladins, blackPaladins, whiteUnicorn, blackUnicorn; // Positions des pièces sur le plateau
    private static int currentTurn; // 0 = blanc, 1 = noir
    private static int nextMoveConstraint; // 0 = aucun, 1 = lisere1, 2 = lisere2, 3 = lisere3

    // ------------ Initialisation statique ------------

    static {
        for (int i = 0; i < 36; i++) {
            int ligne = i / 6;
            int col = i % 6;
            char colChar = (char) ('A' + col);
            char ligneChar = (char) ('1' + ligne);
            COORD_CACHE[i] = new String(new char[]{colChar, ligneChar}); // Evite une concaténation plus coûteuse
        }
    }

    // ------------ Outils de conversion ------------

    /** Convertit une chaîne de caractères représentant une case (ex: "A1") en un index entier
     * @param caseName la chaîne de caractères représentant la case
     * @return l'index entier correspondant
     */
    private static int stringToIndex(String caseName) {
        char colChar = caseName.charAt(0);
        char ligneChar = caseName.charAt(1);
        int col = colChar - 'A';
        int ligne = ligneChar - '1';
        return ligne * 6 + col;
    }

    /** Convertit un index entier en une chaîne de caractères représentant une case (ex: "A1")
     * @param index l'index entier
     * @return la chaîne de caractères correspondant
     */
    private static String indexToString(int index) {
        return COORD_CACHE[index];
    }

    // --------------------- Gestion des fichiers ---------------------

    /** Initialise un plateau à partir d’un fichier texte
     * @param fileName le nom du fichier à lire
     */
    @Override
    public void setFromFile(String fileName) {
        // Reset
        whitePaladins = 0L; whiteUnicorn = 0L;
        blackPaladins = 0L; blackUnicorn = 0L;

        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineCounter = 0; // Pour suivre la ligne du plateau

            while((line = br.readLine()) != null && lineCounter < 6) {
                line = line.trim();

                if(line.isEmpty() || line.charAt(0) == '%'){ // Ignore les lignes vides ou les commentaires (commencent par %)
                    continue;
                }
                if(!Character.isDigit(line.charAt(0))){ // Ignore les lignes qui ne commencent pas par un chiffre
                    continue;
                }

                int colCounter = 0; // Pour suivre la colonne du plateau
                for(int i = 0; i < line.length() && colCounter < 6; i++){
                    char c = line.charAt(i);

                    if(c == 'N' || c == 'B' || c == 'n' || c == 'b' || c == '-'){ // Que les caractères valides
                        int index = lineCounter * 6 + colCounter; // Calcul de l'index
                        long mask = 1L << index; // Masque pour la position actuelle

                        switch(c){ // Selon le caractère, on place la pièce correspondante
                            case 'B' : blackUnicorn |= mask; break;
                            case 'N' : whiteUnicorn |= mask; break;
                            case 'b' : blackPaladins |= mask; break;
                            case 'n' : whitePaladins |= mask; break;
                            case '-' : break; // Case vide
                        }
                        colCounter++;
                    }
                }
                if(colCounter == 6){
                    lineCounter++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /** Sauve la configuration de l’état courant (plateau et pièces restantes) dans un fichier
     * @param fileName le nom du fichier à sauvegarder
     * Le format doit être compatible avec celui utilisé pour la lecture.
     */
    @Override
    public void saveToFile(String fileName) {
        // TODO

    }

    /** Indique si le coup <move> est valide pour le joueur <player> sur le plateau courant
     * @param move le coup à jouer,
     * sous la forme "B1-D1" en général,
     * sous la forme "C6/A6/B5/D5/E6/F5" pour le coup qui place les pièces
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     */
    @Override
    public boolean isValidMove(String move, String player) {
        // TODO
        return false;
    }

    /** Calcule les coups possibles pour le joueur <player> sur le plateau courant
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     */
    @Override
    public String[] possiblesMoves(String player) {
        // TODO
        return null;
    }

    /** Modifie le plateau en jouant le coup move avec la pi`ece choose
     * @param move le coup à jouer, sous la forme "C1-D1" ou "C6/A6/B5/D5/E6/F5"
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     */
    @Override
    public void play(String move, String player) {
        // TODO

    }

    /** Vrai lorsque le plateau correspond à une fin de partie.
     */
    @Override
    public boolean gameOver() {
        // TODO
        return false;
    }
}
