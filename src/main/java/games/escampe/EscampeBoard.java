package games.escampe;

import java.io.*;
import java.io.FileReader;

public class EscampeBoard implements Partie1 {

    // ------------ Constantes ------------

    // Masques pour les liserés (Bit 0 = case libre, Bit 1 = liseré)
    private static final long LISERE_1 = 0b100010_010100_001010_010001_101001_000100L; // 0b pour binaire et L pour long
    private static final long LISERE_2 = 0b011001_000001_100100_100100_000001_011001L;
    private static final long LISERE_3 = 0b000100_101010_010001_001010_010100_100010L;

    private static final String[] COORD_CACHE = new String[36]; // Cache des coordonnées des cases pour éviter de les recalculer

    // ------------ Variables d'etat ------------

    private static long whitePaladins, blackPaladins, whiteUnicorn, blackUnicorn; // Positions des pièces sur le plateau
    private static int currentTurn; // 0 = blanc, 1 = noir
    private static int nextMoveConstraint; // 0 = aucun, 1 = liseré1, 2 = liseré2, 3 = liseré3

    // ------------ Initialisation statique ------------

    static {
        for (int i = 0; i < 36; i++) {
            char colChar = (char) ('A' + (i % 6));
            char ligneChar = (char) ('1' + (i / 6));
            COORD_CACHE[i] = new String(new char[]{colChar, ligneChar}); // Évite une concaténation plus coûteuse
        }
    }

    // ------------ Outils de conversion ------------

    /** Convertit une portion de chaîne (ex: "A1" dans "A1-B2") en index entier.
     * @param s la chaîne complète (ex: "A1-B2")
     * @param offset l'endroit où commence la case (0 pour le début, 3 pour la seconde partie)
     * @return l'index entier (0-35)
     */
    private static int stringToIndex(String s, int offset) {
        char colChar = s.charAt(offset);
        char rowChar = s.charAt(offset + 1);

        return (rowChar - '1') * 6 + (colChar - 'A');
    }

    /** Convertit un index entier en une chaîne de caractères représentant une case (ex : "A1")
     * @param index l'index entier
     * @return la chaîne de caractères correspondante
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
        // Reset les positions
        whitePaladins = 0L; whiteUnicorn = 0L;
        blackPaladins = 0L; blackUnicorn = 0L;

        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) { // Ouvrir le fichier avec un buffer
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

                    if(c == '-'){ // Si pas de pion on passe
                        colCounter++;
                        continue;
                    }

                    if(c == 'N' || c == 'B' || c == 'n' || c == 'b'){ // Un pion
                        long mask = 1L << lineCounter * 6 + colCounter; // Créer un masque avec l'index

                        switch(c){ // Selon le caractère, on place la pièce correspondante
                            case 'B' : blackUnicorn |= mask; break;
                            case 'N' : whiteUnicorn |= mask; break;
                            case 'b' : blackPaladins |= mask; break;
                            case 'n' : whitePaladins |= mask; break;
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

    /** Sauvegarde la configuration de l’état courant (plateau et pièces restantes) dans un fichier
     * @param fileName le nom du fichier à sauvegarder
     * Le format doit être compatible avec celui utilisé pour la lecture.
     */
    @Override
    public void saveToFile(String fileName) {
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))){
            bw.write("%  ABCDEF"); // Premier commentaire pour afficher les numéros de colonnes
            bw.newLine();

            char[] line = new char[12]; // Liste de char pour réutiliser ceux qui ne changent pas

            line[0] = '0';
            line[2] = ' ';
            line[9] = ' ';
            line[10] = '0';

            for(int row = 0; row < 6; row++){ // Boucle sur les 6 lignes
                char rowChar = (char) ('1' + row); // Calcul du chiffre de la ligne
                line[1] = rowChar;
                line[11] = rowChar;

                int rowStartIndex = row * 6;

                for(int col = 0; col < 6; col++){ // Boucle sur les 6 colonnes
                    long mask = 1L << (rowStartIndex + col);

                    char c = '-';

                    // Test si un pion sur cette case
                    if((whitePaladins & mask) != 0) c = 'b';
                    else if ((blackPaladins & mask) != 0) c = 'n';
                    else if((whiteUnicorn & mask) != 0) c = 'B';
                    else if((blackUnicorn & mask) != 0) c = 'N';

                    line[3 + col] = c;
                }

                bw.write(line);
                bw.newLine();
            }
            bw.write("%  ABCDEF");
            bw.newLine();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /** Indique si le coup <move> est valide pour le joueur <player> sur le plateau courant
     * @param move le coup à jouer,
     * sous la forme "B1-D1" en général,
     * sous la forme "C6/A6/B5/D5/E6/F5" pour le coup qui place les pièces
     * @param player le joueur qui joue, représenté par "noir" ou "blanc".
     */
    @Override
    public boolean isValidMove(String move, String player) {
        if(move.length() > 5) return isValidPlacementMove(move, player); // Si pas sous format "A1-B2" alors Placement (1er coup)

        int from = stringToIndex(move, 0); // récupérer l'index de départ
        int to = stringToIndex(move, 3); // récupérer l'index d'arrivée

        return isValidGameplayMove(from, to, player); // Sinon forme générale
    }


    private boolean isValidPlacementMove(String move, String player) {
        // TODO

        return false;
    }

    private boolean isValidGameplayMove(int from, int to, String player) {
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
