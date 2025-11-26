package games.escampe;

public class EscampeBoard implements Partie1 {

    /** Initialise un plateau à partir d’un fichier texte
     * @param fileName le nom du fichier à lire
     */
    @Override
    public void setFromFile(String fileName) {
        // TODO

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
