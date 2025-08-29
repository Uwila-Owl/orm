
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ZoneModelisation extends Pane {

    private Map<Integer, Group> entiteToGroup = new HashMap<>();
    private Map<Integer, Map<String, Object>> entiteById = new HashMap<>();
    private List<LigneAssociee> lignesAssociees = new ArrayList<>();
    private boolean isUML = true;
    private SelectionListener selectionListener;
    private static final Logger LOGGER = Logger.getLogger(ZoneModelisation.class.getName());

    private EntiteDAO entiteDAO;
    private AttributDAO attributDAO;
    private Visuel visuel;

    public interface SelectionListener {

        void onSelection(Map<String, Object> entite);
    }

    public ZoneModelisation() {
        super();
        this.entiteDAO = new EntiteDAO();
        this.attributDAO = new AttributDAO();
        this.visuel = new Visuel();
        ChargerEntites("UML");
        this.addEventFilter(ScrollEvent.SCROLL, this::zoomSouris);
        this.addEventFilter(KeyEvent.KEY_PRESSED, this::toucheClavAppui);
    }

    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    public void setTypeSchema(boolean isUML) {
        this.isUML = isUML;
        this.getChildren().clear();
        entiteToGroup.clear();
        entiteById.clear();
        lignesAssociees.clear();

        if (isUML) {
            ChargerEntites("UML");
        } else {
            ChargerEntites("ERD");
        }
    }

    public void ajouterEntite(Map<String, Object> entite) {
        if (!entite.containsKey("id") || ((Integer) entite.get("id")) == -1) {
            String nom = (String) entite.get("nom");
            int positionX = ((Double) entite.get("position_x")).intValue();
            int positionY = ((Double) entite.get("position_y")).intValue();
            String typeSchema = isUML ? "UML" : "ERD";

            int entiteId = entiteDAO.insertEntite(nom, positionX, positionY, typeSchema);
            if (entiteId != -1) {
                entite.put("id", entiteId);
                LOGGER.info("Nouvelle entité insérée avec ID : " + entiteId);

                List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");
                if (attributs != null) {
                    for (Map<String, Object> attribut : attributs) {
                        attributDAO.insertAttribut(
                                (String) attribut.get("nom"),
                                (boolean) attribut.get("cle_primaire"),
                                (boolean) attribut.get("cle_etrangere"),
                                entiteId,
                                typeSchema
                        );
                    }
                }
            } else {
                LOGGER.log(Level.SEVERE, "Impossible d'insérer l'entité dans la base de données.");
                return;
            }
        }

        Integer entiteId = (Integer) entite.get("id");
        entiteById.put(entiteId, entite);

        if (isUML) {
            ajouterEntiteUML(entite);
        } else {
            ajouterEntiteERD(entite);
        }
    }

    public void ChargerEntites(String typeSchema) {
        List<Map<String, Object>> entitiesFromDb = new ArrayList<>();
        try (java.sql.Connection conn = ConnexionBdd.getConnection(); java.sql.PreparedStatement pstmtEntites = conn.prepareStatement("SELECT id, nom, position_x, position_y, type_schema FROM entites WHERE type_schema = ?"); java.sql.PreparedStatement pstmtAttributs = conn.prepareStatement("SELECT nom, cle_primaire, cle_etrangere, type_schema FROM attributs WHERE entite_id = ? AND type_schema = ?")) {

            pstmtEntites.setString(1, typeSchema);
            java.sql.ResultSet rsEntites = pstmtEntites.executeQuery();

            while (rsEntites.next()) {
                Map<String, Object> entite = new HashMap<>();
                int entiteId = rsEntites.getInt("id");
                entite.put("id", entiteId);
                entite.put("nom", rsEntites.getString("nom"));
                entite.put("position_x", (double) rsEntites.getInt("position_x"));
                entite.put("position_y", (double) rsEntites.getInt("position_y"));
                entite.put("type_schema", rsEntites.getString("type_schema"));

                pstmtAttributs.setInt(1, entiteId);
                pstmtAttributs.setString(2, typeSchema);
                java.sql.ResultSet rsAttributs = pstmtAttributs.executeQuery();
                List<Map<String, Object>> attributs = new ArrayList<>();
                while (rsAttributs.next()) {
                    Map<String, Object> attribut = new HashMap<>();
                    attribut.put("nom", rsAttributs.getString("nom"));
                    attribut.put("cle_primaire", rsAttributs.getBoolean("cle_primaire"));
                    attribut.put("cle_etrangere", rsAttributs.getBoolean("cle_etrangere"));
                    attribut.put("type_schema", rsAttributs.getString("type_schema"));
                    attributs.add(attribut);
                }
                entite.put("attributs", attributs);
                entitiesFromDb.add(entite);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des entités : " + e.getMessage(), e);
        }

        for (Map<String, Object> entity : entitiesFromDb) {
            Integer entiteId = (Integer) entity.get("id");
            entiteById.put(entiteId, entity);

            if (isUML) {
                ajouterEntiteUML(entity);
            } else {
                ajouterEntiteERD(entity);
            }
        }

        // Charger relations depuis la BDD
        chargerRelationsDepuisBDD();
    }

    public void chargerRelationsDepuisBDD() {
        RelationDAO relationDAO = new RelationDAO();
        List<Map<String, Object>> relations = relationDAO.getAllRelations();

        for (Map<String, Object> rel : relations) {
            int sourceId = (int) rel.get("entite_source_id");
            int cibleId = (int) rel.get("entite_cible_id");

            Map<String, Object> source = entiteById.get(sourceId);
            Map<String, Object> cible = entiteById.get(cibleId);

            if (source != null && cible != null) {
                String typeLien = rel.get("type_schema").equals("UML") ? "Héritage" : "Relation";
                String cardSource = (String) rel.getOrDefault("cardinalite_source", "[ ]");
                String cardCible = (String) rel.getOrDefault("cardinalite_cible", "[ ]");

                if (!relationExiste(source, cible, typeLien)) {
                    creerLienEntreEntitesAvecCardinalites(source, cible, typeLien, cardSource, cardCible);
                }
            }
        }
    }

    public void creerLienEntreEntitesAvecCardinalites(Map<String, Object> source, Map<String, Object> cible, String typeLien,
            String cardSource, String cardCible) {
        if (source == null || cible == null || source.equals(cible)) {
            return;
        }

        Group g1 = entiteToGroup.get((Integer) source.get("id"));
        Group g2 = entiteToGroup.get((Integer) cible.get("id"));
        if (g1 == null || g2 == null) {
            return;
        }

        Line ligne = new Line();
        ligne.setStroke(typeLien.equals("Héritage") ? Color.GREEN : Color.BLACK);
        ligne.setStrokeWidth(typeLien.equals("Héritage") ? 3 : 1);

        LigneAssociee la = new LigneAssociee(ligne, source, cible, cardSource, cardCible);
        lignesAssociees.add(la);

        la.MajPosition();
        this.getChildren().add(0, ligne);
    }

    /**
     * Crée un lien entre deux entités avec des cardinalités par défaut [ ].
     */
    public void creerLienEntreEntites(Map<String, Object> source, Map<String, Object> cible, String typeLien) {
        creerLienEntreEntitesAvecCardinalites(source, cible, typeLien, "[ ]", "[ ]");
    }

    private void ajouterEntiteUML(Map<String, Object> entite) {
        Group entiteVisuelle = new Group();
        visuel.ajouterEntiteUML(entiteVisuelle, entite);
        setupEntiteInteraction(entiteVisuelle, entite);
    }

    private void ajouterEntiteERD(Map<String, Object> entite) {
        Group entiteVisuelle = new Group();
        visuel.ajouterEntiteERD(entiteVisuelle, entite);
        setupEntiteInteraction(entiteVisuelle, entite);
    }

    private void setupEntiteInteraction(Group entiteVisuelle, Map<String, Object> entite) {
    // Position initiale
    entiteVisuelle.setLayoutX((double) entite.get("position_x"));
    entiteVisuelle.setLayoutY((double) entite.get("position_y"));

    final Delta dragDelta = new Delta();

    // Clic sur l'entité
    entiteVisuelle.setOnMousePressed(event -> {
        dragDelta.x = entiteVisuelle.getLayoutX() - event.getSceneX();
        dragDelta.y = entiteVisuelle.getLayoutY() - event.getSceneY();

        if (selectionListener != null) {
            selectionListener.onSelection(entite);
        }
    });

    // Drag & drop visuel
    entiteVisuelle.setOnMouseDragged(event -> {
        entiteVisuelle.setLayoutX(event.getSceneX() + dragDelta.x);
        entiteVisuelle.setLayoutY(event.getSceneY() + dragDelta.y);

        // Mise à jour temporaire du modèle (Map)
        entite.put("position_x", entiteVisuelle.getLayoutX());
        entite.put("position_y", entiteVisuelle.getLayoutY());

        // Mise à jour graphique des liens
        MajLien(entiteVisuelle);
    });

    // Mise à jour DB uniquement à la fin du drag
    entiteVisuelle.setOnMouseReleased(event -> {
        entiteDAO.updateEntitePosition(
            (Integer) entite.get("id"),
            (int) entiteVisuelle.getLayoutX(),
            (int) entiteVisuelle.getLayoutY()
        );
    });

    // Ajout dans la scène et cache
    this.getChildren().add(entiteVisuelle);
    Integer entiteId = (Integer) entite.get("id");
    entiteToGroup.put(entiteId, entiteVisuelle);
}


    private void MajLien(Node node) {
        Map<String, Object> entite = null;
        for (Map.Entry<Integer, Group> entry : entiteToGroup.entrySet()) {
            if (entry.getValue() == node) {
                entite = entiteById.get(entry.getKey());
                break;
            }
        }
        if (entite == null) {
            return;
        }

        for (LigneAssociee la : lignesAssociees) {
            if (la.e1.equals(entite) || la.e2.equals(entite)) {
                la.MajPosition();
            }
        }
    }

    private static class Delta {

        double x, y;
    }

    // ----------------- Méthodes publiques -----------------
    public Map<String, Object> getEntiteParNom(String nom) {
        for (Map<String, Object> ent : entiteById.values()) {
            if (nom.equals(ent.get("nom"))) {
                return ent;
            }
        }
        return null;
    }

    public void mettreAJourEntite(Map<String, Object> entite) {
        Integer entiteId = (Integer) entite.get("id");
        Group oldGroup = entiteToGroup.get(entiteId);

        if (oldGroup != null) {
            this.getChildren().remove(oldGroup);
            entiteToGroup.remove(entiteId);
        }

        entiteById.put(entiteId, entite);

        String typeSchema = isUML ? "UML" : "ERD";
        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");
        if (attributs != null) {
            attributDAO.updateAttributsForEntite(entiteId, attributs, typeSchema);
        }

        if (isUML) {
            ajouterEntiteUML(entite);
        } else {
            ajouterEntiteERD(entite);
        }
    }

    public boolean isUML() {
        return this.isUML;
    }

    public List<String> getNomsEntitesExcluant(Map<String, Object> entiteExclue) {
        List<String> noms = new ArrayList<>();
        for (Map<String, Object> ent : entiteById.values()) {
            if (!ent.equals(entiteExclue)) {
                noms.add((String) ent.get("nom"));
            }
        }
        return noms;
    }

    public List<Map<String, Object>> getAllEntities() {
        return new ArrayList<>(entiteById.values());
    }

    public boolean relationExiste(Map<String, Object> source, Map<String, Object> cible, String typeLien) {
        for (LigneAssociee ligne : lignesAssociees) {
            boolean memeType = (typeLien.equals("Héritage") && ligne.ligne.getStroke().equals(Color.GREEN))
                    || (typeLien.equals("Relation") && ligne.ligne.getStroke().equals(Color.BLACK));
            boolean memeCouple = (ligne.e1.equals(source) && ligne.e2.equals(cible))
                    || (ligne.e1.equals(cible) && ligne.e2.equals(source));
            if (memeType && memeCouple) {
                return true;
            }
        }
        return false;
    }

    private void MajPositionLigne(Line ligne, Group g1, Group g2) {
        double x1 = g1.getLayoutX() + g1.getBoundsInParent().getWidth() / 2;
        double y1 = g1.getLayoutY() + g1.getBoundsInParent().getHeight() / 2;
        double x2 = g2.getLayoutX() + g2.getBoundsInParent().getWidth() / 2;
        double y2 = g2.getLayoutY() + g2.getBoundsInParent().getHeight() / 2;

        ligne.setStartX(x1);
        ligne.setStartY(y1);
        ligne.setEndX(x2);
        ligne.setEndY(y2);
    }

    private void zoomSouris(ScrollEvent event) {
        visuel.zoomSouris(event, this);
    }

    private void toucheClavAppui(KeyEvent event) {
        visuel.toucheClavAppui(event);
    }

    // ----------------- Classe LigneAssociee -----------------
    private class LigneAssociee {

        Line ligne;
        Map<String, Object> e1;
        Map<String, Object> e2;
        String cardSource;
        String cardCible;
        Text cardinaliteSourceText;
        Text cardinaliteCibleText;

        public LigneAssociee(Line l, Map<String, Object> e1, Map<String, Object> e2, String cardSource, String cardCible) {
            this.ligne = l;
            this.e1 = e1;
            this.e2 = e2;
            this.cardSource = cardSource;
            this.cardCible = cardCible;

            this.cardinaliteSourceText = new Text(cardSource);
            this.cardinaliteCibleText = new Text(cardCible);
            ZoneModelisation.this.getChildren().addAll(cardinaliteSourceText, cardinaliteCibleText);
        }

        // Dans la classe LigneAssociee de ZoneModelisation.java
        // Remplacer la méthode MajPosition() par celle-ci :

        public void MajPosition() {
        Group g1 = entiteToGroup.get((Integer) e1.get("id"));
        Group g2 = entiteToGroup.get((Integer) e2.get("id"));
        if (g1 != null && g2 != null) {
        // Calculer les centres des entités
        double centerX1 = g1.getLayoutX() + g1.getBoundsInParent().getWidth() / 2;
        double centerY1 = g1.getLayoutY() + g1.getBoundsInParent().getHeight() / 2;
        double centerX2 = g2.getLayoutX() + g2.getBoundsInParent().getWidth() / 2;
        double centerY2 = g2.getLayoutY() + g2.getBoundsInParent().getHeight() / 2;

        // Calculer les points de connexion sur les bords des rectangles
        double[] point1 = calculerPointConnexion(g1, centerX2, centerY2);
        double[] point2 = calculerPointConnexion(g2, centerX1, centerY1);

        // Mettre à jour la ligne
        ligne.setStartX(point1[0]);
        ligne.setStartY(point1[1]);
        ligne.setEndX(point2[0]);
        ligne.setEndY(point2[1]);

        // Positionner les cardinalités près des bords des entités
        
        // Cardinalité source (près de l'entité e1)
        double offsetX1 = (point2[0] - point1[0]) * 0.1; // 10% de la ligne depuis e1
        double offsetY1 = (point2[1] - point1[1]) * 0.1;
   

        // Cardinalité cible (près de l'entité e2)
        double offsetX2 = (point1[0] - point2[0]) * 0.2; // 10% de la ligne depuis e2
        double offsetY2 = (point1[1] - point2[1]) * 0.1;
      
        // Détermination de l'orientation de la ligne
        double dx = point2[0] - point1[0];
        double dy = point2[1] - point1[1];
        double adx = Math.abs(dx);
        double ady = Math.abs(dy);

        if (adx > ady * 2) {
            // Cas ligne principalement horizontale
            cardinaliteSourceText.setX(point1[0] + offsetX1 -10);
            cardinaliteSourceText.setY(point1[1] + 15);  // au-dessus

            cardinaliteCibleText.setX(point2[0] + offsetX2);
            cardinaliteCibleText.setY(point2[1] - 15);  // en-dessous

        } else if (ady > adx * 2) {
            // Cas ligne principalement verticale
            cardinaliteSourceText.setX(point1[0] + 5);  // à droite
            cardinaliteSourceText.setY(point1[1] + offsetY1);

            cardinaliteCibleText.setX(point2[0] - cardinaliteCibleText.getLayoutBounds().getWidth() - 5); // à gauche
            cardinaliteCibleText.setY(point2[1] + offsetY2);

        } else {
            // Cas ligne diagonale
            double norm = Math.sqrt(dx * dx + dy * dy);
            double ux = dx / norm;
            double uy = dy / norm;

            // vecteur perpendiculaire pour décaler la cardinalité
            double px = -uy;
            double py = ux;

            // Source
            cardinaliteSourceText.setX(point1[0] + offsetX1 + px * 15);
            cardinaliteSourceText.setY(point1[1] + offsetY1 + py * 15);

            // Cible
            cardinaliteCibleText.setX(point2[0] + offsetX2 + px * 15 - cardinaliteCibleText.getLayoutBounds().getWidth() / 2);
            cardinaliteCibleText.setY(point2[1] + offsetY2 + py * 15);
        }


        }
        }

        // Ajouter cette méthode helper dans la classe LigneAssociee :
        private double[] calculerPointConnexion(Group entite, double targetX, double targetY) {
        double entiteX = entite.getLayoutX();
        double entiteY = entite.getLayoutY();
        double entiteWidth = entite.getBoundsInParent().getWidth();
        double entiteHeight = entite.getBoundsInParent().getHeight();

        double centerX = entiteX + entiteWidth / 2;
        double centerY = entiteY + entiteHeight / 2;

        // Calculer la direction vers le point cible
        double dx = targetX - centerX;
        double dy = targetY - centerY;

        // Points d'intersection avec les bords du rectangle
        double[] point = new double[2];

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

       
    }       
}
