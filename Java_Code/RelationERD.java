
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import java.util.Map;

/**
 * Classe pour gérer les relations ERD avec ellipse centrale et cardinalités
 */
public class RelationERD {

    private Line ligne1;  // Ligne de l'entité source vers l'ellipse
    private Line ligne2;  // Ligne de l'ellipse vers l'entité cible
    private Ellipse ellipse;
    private Text relationText;
    private Text cardinaliteSourceText;
    private Text cardinaliteCibleText;
    private Group relationGroup;

    private Map<String, Object> entiteSource;
    private Map<String, Object> entiteCible;
    private Map<Integer, Group> entiteToGroup;

    private String cardinaliteSource;
    private String cardinaliteCible;
    private int id;
    private String relationNom;

    public RelationERD(int id, Map<String, Object> entiteSource, Map<String, Object> entiteCible,
            Map<Integer, Group> entiteToGroup, String relationNom,
            String cardSource, String cardCible) {
        this.id = id;
        this.entiteSource = entiteSource;
        this.entiteCible = entiteCible;
        this.entiteToGroup = entiteToGroup;
        this.relationNom = relationNom;
        this.cardinaliteSource = cardSource;
        this.cardinaliteCible = cardCible;

        // Création des lignes
        ligne1 = new Line();
        ligne1.setStroke(Color.BLACK);
        ligne1.setStrokeWidth(1);

        ligne2 = new Line();
        ligne2.setStroke(Color.BLACK);
        ligne2.setStrokeWidth(1);

        // Création de l'ellipse
        ellipse = new Ellipse(0, 0, 50, 25);
        ellipse.setFill(Color.LIGHTGRAY);
        ellipse.setStroke(Color.BLACK);
        ellipse.setStrokeWidth(1);

        // Texte de la relation (centré dans l'ellipse)
        relationText = new Text(relationNom != null ? relationNom : "Relation");
        relationText.setX(-relationText.getBoundsInLocal().getWidth() / 2);
        relationText.setY(5); // Légèrement au-dessus du centre pour un meilleur rendu

        // Group pour l'ellipse et son texte
        relationGroup = new Group(ellipse, relationText);

        // Textes des cardinalités
        cardinaliteSourceText = new Text(cardSource != null ? cardSource : "C_SOURCE");
        cardinaliteCibleText = new Text(cardCible != null ? cardCible : "C_CIBLE");

        // Mise à jour initiale des positions
        mettreAJourPositions();
    }

    public String getRelationNom() {
        return this.relationNom;
    }

//--AjoutLyna
// Getter pour l'id
    public int getId() {
        return this.id;
    }
    //

    /**
     * Met à jour toutes les positions (lignes, ellipse, cardinalités)
     */
    public void mettreAJourPositions() {
        Group groupeSource = entiteToGroup.get((Integer) entiteSource.get("id"));
        Group groupeCible = entiteToGroup.get((Integer) entiteCible.get("id"));

        if (groupeSource == null || groupeCible == null) {
            return;
        }

        // Calcul des centres des entités
        double centerX1 = groupeSource.getLayoutX() + groupeSource.getBoundsInParent().getWidth() / 2;
        double centerY1 = groupeSource.getLayoutY() + groupeSource.getBoundsInParent().getHeight() / 2;
        double centerX2 = groupeCible.getLayoutX() + groupeCible.getBoundsInParent().getWidth() / 2;
        double centerY2 = groupeCible.getLayoutY() + groupeCible.getBoundsInParent().getHeight() / 2;

        // Calcul des points de connexion sur les bords des rectangles
        double[] pointSource = calculerPointConnexion(groupeSource, centerX2, centerY2);
        double[] pointCible = calculerPointConnexion(groupeCible, centerX1, centerY1);

        // Position du centre de l'ellipse (au milieu de la ligne conceptuelle)
        double centreEllipseX = (pointSource[0] + pointCible[0]) / 2;
        double centreEllipseY = (pointSource[1] + pointCible[1]) / 2;

        // Positionnement du groupe ellipse
        relationGroup.setLayoutX(centreEllipseX);
        relationGroup.setLayoutY(centreEllipseY);

        // Mise à jour des lignes
        // Ligne 1 : du point source vers le centre de l'ellipse
        ligne1.setStartX(pointSource[0]);
        ligne1.setStartY(pointSource[1]);
        ligne1.setEndX(centreEllipseX);
        ligne1.setEndY(centreEllipseY);

        // Ligne 2 : du centre de l'ellipse vers le point cible
        ligne2.setStartX(centreEllipseX);
        ligne2.setStartY(centreEllipseY);
        ligne2.setEndX(pointCible[0]);
        ligne2.setEndY(pointCible[1]);

        // Mise à jour des positions des cardinalités
        positionnerCardinalites(pointSource, pointCible, centreEllipseX, centreEllipseY);
    }

    /**
     * Positionne les textes de cardinalités près des entités
     */
    private void positionnerCardinalites(double[] pointSource, double[] pointCible,
            double centreEllipseX, double centreEllipseY) {

        // Cardinalité source : entre l'entité source et l'ellipse (plus près de l'entité)
        double offsetSourceX = (centreEllipseX - pointSource[0]) * 0.2; // 20% du chemin vers l'ellipse
        double offsetSourceY = (centreEllipseY - pointSource[1]) * 0.2;

        // Calcul de l'orientation de la ligne pour un décalage perpendiculaire
        double dx = centreEllipseX - pointSource[0];
        double dy = centreEllipseY - pointSource[1];
        double longueur = Math.sqrt(dx * dx + dy * dy);

        if (longueur > 0) {
            // Vecteur unitaire perpendiculaire
            double perpX = -dy / longueur;
            double perpY = dx / longueur;

            // Décalage perpendiculaire pour éviter que le texte soit sur la ligne
            double decalage = 15; // Décalage pour la cardinalité source

            // Position cardinalité source
            cardinaliteSourceText.setX(pointSource[0] + offsetSourceX + perpX * decalage);
            cardinaliteSourceText.setY(pointSource[1] + offsetSourceY + perpY * decalage);

            // Calculer le point milieu entre l'ellipse et la cible
            double midPointToCibleX = centreEllipseX + (pointCible[0] - centreEllipseX) * 0.5;
            double midPointToCibleY = centreEllipseY + (pointCible[1] - centreEllipseY) * 0.5;

            // Décalage pour la cardinalité cible (par exemple, vers le bas)
            double decalageCible = 15; // Ajustez cette valeur si nécessaire

            cardinaliteCibleText.setX(midPointToCibleX + perpX * decalageCible);
            cardinaliteCibleText.setY(midPointToCibleY + perpY * decalageCible);
        }
    }

    /**
     * Calcule le point de connexion sur le bord d'une entité vers un point
     * cible
     */
    private double[] calculerPointConnexion(Group entite, double targetX, double targetY) {
        double entiteX = entite.getLayoutX();
        double entiteY = entite.getLayoutY();
        double entiteWidth = entite.getBoundsInParent().getWidth();
        double entiteHeight = entite.getBoundsInParent().getHeight();

        double centerX = entiteX + entiteWidth / 2;
        double centerY = entiteY + entiteHeight / 2;

        // Direction vers le point cible
        double dx = targetX - centerX;
        double dy = targetY - centerY;

        double[] point = new double[2];

        // Détermine sur quel bord du rectangle se trouve le point d'intersection
        if (Math.abs(dx) / entiteWidth > Math.abs(dy) / entiteHeight) {
            // Intersection avec le bord gauche ou droit
            if (dx > 0) {
                // Bord droit
                point[0] = entiteX + entiteWidth;
                point[1] = centerY + dy * (entiteWidth / 2) / Math.abs(dx);
            } else {
                // Bord gauche
                point[0] = entiteX;
                point[1] = centerY + dy * (entiteWidth / 2) / Math.abs(dx);
            }
        } else {
            // Intersection avec le bord haut ou bas
            if (dy > 0) {
                // Bord bas
                point[0] = centerX + dx * (entiteHeight / 2) / Math.abs(dy);
                point[1] = entiteY + entiteHeight;
            } else {
                // Bord haut
                point[0] = centerX + dx * (entiteHeight / 2) / Math.abs(dy);
                point[1] = entiteY;
            }
        }

        return point;
    }

    // Getters pour accéder aux éléments visuels
    public Line getLigne1() {
        return ligne1;
    }

    public Line getLigne2() {
        return ligne2;
    }

    public Group getRelationGroup() {
        return relationGroup;
    }

    public Text getCardinaliteSourceText() {
        return cardinaliteSourceText;
    }

    public Text getCardinaliteCibleText() {
        return cardinaliteCibleText;
    }

    // Getters pour les entités
    public Map<String, Object> getEntiteSource() {
        return entiteSource;
    }

    public Map<String, Object> getEntiteCible() {
        return entiteCible;
    }

    // Méthodes pour modifier les cardinalités
    public void setCardinaliteSource(String cardinalite) {
        this.cardinaliteSource = cardinalite;
        cardinaliteSourceText.setText(cardinalite);
    }

    public void setCardinaliteCible(String cardinalite) {
        this.cardinaliteCible = cardinalite;
        cardinaliteCibleText.setText(cardinalite);
    }

    // Méthode pour modifier le nom de la relation
    public void setNomRelation(String nom) {
        relationText.setText(nom);
        relationText.setX(-relationText.getBoundsInLocal().getWidth() / 2);
    }

    /**
     * Vérifie si cette relation concerne une entité donnée
     */
    public boolean concerneEntite(Map<String, Object> entite) {
        return entiteSource.equals(entite) || entiteCible.equals(entite);
    }
}
