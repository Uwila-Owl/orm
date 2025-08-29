
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import java.sql.SQLException;
import java.sql.ResultSet;

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
    private Visuel visuel; // Nouvelle instance de Visuel

    public interface SelectionListener {

        void onSelection(Map<String, Object> entite);
    }

    public ZoneModelisation() {
        super();
        this.entiteDAO = new EntiteDAO();
        this.attributDAO = new AttributDAO();
        this.visuel = new Visuel(); // Initialisation de Visuel
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
    }

    private void ajouterEntiteUML(Map<String, Object> entite) {
        Group entiteVisuelle = new Group();
        visuel.ajouterEntiteUML(entiteVisuelle, entite); // Appel à la méthode de Visuel
        setupEntiteInteraction(entiteVisuelle, entite);
    }

    private void ajouterEntiteERD(Map<String, Object> entite) {
        Group entiteVisuelle = new Group();
        visuel.ajouterEntiteERD(entiteVisuelle, entite); // Appel à la méthode de Visuel
        setupEntiteInteraction(entiteVisuelle, entite);
    }

    private void setupEntiteInteraction(Group entiteVisuelle, Map<String, Object> entite) {
        entiteVisuelle.setLayoutX((double) entite.get("position_x"));
        entiteVisuelle.setLayoutY((double) entite.get("position_y"));

        final Delta dragDelta = new Delta();

        entiteVisuelle.setOnMousePressed(event -> {
            dragDelta.x = entiteVisuelle.getLayoutX() - event.getSceneX();
            dragDelta.y = entiteVisuelle.getLayoutY() - event.getSceneY();

            if (selectionListener != null) {
                selectionListener.onSelection(entite);
            }
        });

        entiteVisuelle.setOnMouseDragged(event -> {
            entiteVisuelle.setLayoutX(event.getSceneX() + dragDelta.x);
            entiteVisuelle.setLayoutY(event.getSceneY() + dragDelta.y);

            entite.put("position_x", entiteVisuelle.getLayoutX());
            entite.put("position_y", entiteVisuelle.getLayoutY());

            entiteDAO.updateEntitePosition((Integer) entite.get("id"), (int) entiteVisuelle.getLayoutX(), (int) entiteVisuelle.getLayoutY());

            MajLien(entiteVisuelle);
        });

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

    private class LigneAssociee {

        Line ligne;
        Map<String, Object> e1;
        Map<String, Object> e2;

        public LigneAssociee(Line l, Map<String, Object> e1, Map<String, Object> e2) {
            this.ligne = l;
            this.e1 = e1;
            this.e2 = e2;
        }

        public void MajPosition() {
            Integer id1 = (Integer) e1.get("id");
            Integer id2 = (Integer) e2.get("id");
            Group g1 = entiteToGroup.get(id1);
            Group g2 = entiteToGroup.get(id2);
            if (g1 != null && g2 != null) {
                MajPositionLigne(ligne, g1, g2);
            }
        }
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

    public void mettreAJourEntite(Map<String, Object> entite) {
        Integer entiteId = (Integer) entite.get("id");
        Group oldGroup = entiteToGroup.get(entiteId);

        if (oldGroup != null) {
            this.getChildren().remove(oldGroup);
            entiteToGroup.remove(entiteId);
        }

        entiteById.put(entiteId, entite);

        if (entiteId != null && entiteId != -1) {
            List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");
            String typeSchema = isUML ? "UML" : "ERD";

            if (attributs != null) {
                attributDAO.updateAttributsForEntite(entiteId, attributs, typeSchema);
            }
        }

        if (isUML) {
            ajouterEntiteUML(entite);
        } else {
            ajouterEntiteERD(entite);
        }
    }

    // Retourne la liste des noms d'entités existantes, optionnellement en excluant une entité donnée
    public List<String> getNomsEntitesExcluant(Map<String, Object> entiteExclue) {
        List<String> noms = new ArrayList<>();
        for (Map<String, Object> ent : entiteById.values()) {
            if (!ent.equals(entiteExclue)) {
                noms.add((String) ent.get("nom"));
            }
        }
        return noms;
    }

    // Récupère une entité par son nom
    public Map<String, Object> getEntiteParNom(String nom) {
        for (Map<String, Object> ent : entiteById.values()) {
            if (nom.equals(ent.get("nom"))) {
                return ent;
            }
        }
        return null;
    }

    // Crée un lien visuel (relation ou héritage) entre deux entités
    public void creerLienEntreEntites(Map<String, Object> source, Map<String, Object> cible, String typeLien) {
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

        MajPositionLigne(ligne, g1, g2);

        this.getChildren().add(0, ligne);
        lignesAssociees.add(new LigneAssociee(ligne, source, cible));
    }

    public List<Map<String, Object>> getAllEntities() {
        return new ArrayList<>(entiteById.values());
    }

    public boolean isUML() {
        return this.isUML;
    }

    public boolean relationExiste(Map<String, Object> source, Map<String, Object> cible, String typeLien) {
        for (LigneAssociee ligne : lignesAssociees) {
            boolean memeType = (typeLien.equals("Héritage") && ligne.ligne.getStroke().equals(javafx.scene.paint.Color.GREEN))
                    || (typeLien.equals("Relation") && ligne.ligne.getStroke().equals(javafx.scene.paint.Color.BLACK));
            boolean memeCouple = (ligne.e1.equals(source) && ligne.e2.equals(cible))
                    || (ligne.e1.equals(cible) && ligne.e2.equals(source));
            if (memeType && memeCouple) {
                return true;
            }
        }
        return false;
    }

    private void zoomSouris(ScrollEvent event) {
        visuel.zoomSouris(event, this); // Appel à la méthode de Visuel
    }

    private void toucheClavAppui(KeyEvent event) {
        visuel.toucheClavAppui(event); // Appel à la méthode de Visuel
    }
}
