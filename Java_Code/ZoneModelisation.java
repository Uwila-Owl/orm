import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.geometry.VPos;
import javafx.scene.text.TextBoundsType;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
// AJOUT: Imports manquants pour les méthodes
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.util.Date;

public class ZoneModelisation extends Pane {

    private int loadedSchemaId = -1;
    private int schemaId;
    private Map<Integer, Group> entiteToGroup = new HashMap<>();
    private Map<Integer, Map<String, Object>> entiteById = new HashMap<>();
    private List<LigneAssociee> lignesAssociees = new ArrayList<>();
    private boolean isUML = true;
    private SelectionListener selectionListener;
    private static final Logger LOGGER = Logger.getLogger(ZoneModelisation.class.getName());
    private Canvas gridCanvas;
    private Group contentGroup;
    private Group positionCurseurGroup;
    private Text positionCurseurText;
    private Rectangle positionCurseurBackground;
    private boolean snapActive = false; // Snap désactivé par défaut
    private EntiteDAO entiteDAO;
    private AttributDAO attributDAO;
    private Visuel visuel;
    private List<RelationERD> relationsERD = new ArrayList<>();
    private LogDAO logDAO = new LogDAO();
    
;

    String userId = UserSession.getInstance().getUserId();

    public interface SelectionListener {

        void onSelection(Map<String, Object> entite);
    }

    public void setSnapActive(boolean active) {
        this.snapActive = active;
    }

    public ZoneModelisation(int schemaId) {
        super();
        this.loadedSchemaId = schemaId;
        this.schemaId = schemaId;
        this.entiteDAO = new EntiteDAO();
        this.attributDAO = new AttributDAO();
        this.visuel = new Visuel();

        // Initialisation du canvas pour le quadrillage
        gridCanvas = new Canvas();
        this.getChildren().add(0, gridCanvas); // Ajout en fond (index 0)

        contentGroup = new Group();
        this.getChildren().add(contentGroup);

        // Ecouteurs pour redimensionnement
        this.widthProperty().addListener((obs, oldVal, newVal) -> {
            gridCanvas.setWidth(newVal.doubleValue());
            drawGrid();
        });
        this.heightProperty().addListener((obs, oldVal, newVal) -> {
            gridCanvas.setHeight(newVal.doubleValue());
            drawGrid();
        });

        ChargerEntites(isUML ? "UML" : "ERD", loadedSchemaId);
        this.addEventFilter(ScrollEvent.SCROLL, this::zoomSouris);
        this.addEventFilter(KeyEvent.KEY_PRESSED, this::toucheClavAppui);

        // --- Initialisation du rectangle position curseur ---
        positionCurseurText = new Text("X: 0 Y: 0");
        positionCurseurText.setFont(Font.font("Arial", 12));
        positionCurseurText.setFill(Color.BLACK);
        positionCurseurText.setTextOrigin(VPos.TOP);
        positionCurseurText.setBoundsType(TextBoundsType.VISUAL);
        positionCurseurBackground = new Rectangle();
        positionCurseurBackground.setFill(Color.rgb(255, 255, 255, 0.7));
        positionCurseurBackground.setStroke(Color.GRAY);
        positionCurseurBackground.setStrokeWidth(1);
        positionCurseurBackground.setArcWidth(8);
        positionCurseurBackground.setArcHeight(8);
        positionCurseurGroup = new Group(positionCurseurBackground, positionCurseurText);
        this.getChildren().add(positionCurseurGroup);
        this.widthProperty().addListener((obs, oldVal, newVal) -> repositionnerPositionCurseur());
        this.heightProperty().addListener((obs, oldVal, newVal) -> repositionnerPositionCurseur());
        this.setOnMouseMoved(event -> {
            int x = (int) event.getX();
            int y = (int) event.getY();
            positionCurseurText.setText("X: " + x + " Y: " + y);
            double padding = 6;
            double textWidth = positionCurseurText.getLayoutBounds().getWidth();
            double textHeight = positionCurseurText.getLayoutBounds().getHeight();
            positionCurseurBackground.setWidth(textWidth + 2 * padding);
            positionCurseurBackground.setHeight(textHeight + 2 * padding);
            positionCurseurText.setLayoutX(padding);
            positionCurseurText.setLayoutY(padding);
            repositionnerPositionCurseur();
        });
    }

    // Méthode pour dessiner le quadrillage
    private void drawGrid() {
        double width = gridCanvas.getWidth();
        double height = gridCanvas.getHeight();
        GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setStroke(Color.rgb(100, 149, 237, 0.2)); // Bleu clair avec alpha 20%
        gc.setLineWidth(1);
        // Quadrillage tous les 10 pixels
        for (int x = 0; x <= width; x += 10) {
            gc.strokeLine(x, 0, x, height);
        }
        for (int y = 0; y <= height; y += 10) {
            gc.strokeLine(0, y, width, y);
        }
    }
    
   
    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
    }

    public Group getContentGroup() {
        return contentGroup;
    }

    public void setTypeSchema(boolean isUML) {
        this.isUML = isUML;

        // Supprimer uniquement les entités et lignes, pas le quadrillage ni le curseur
        contentGroup.getChildren().clear();
        //List<Node> nodesToRemove = new ArrayList<>();
        //for (Node node : this.getChildren()) {
        //    if (node != gridCanvas && node != positionCurseurGroup) {
        //        nodesToRemove.add(node);
        //    }
        //}
        //this.getChildren().removeAll(nodesToRemove);

        entiteToGroup.clear();
        entiteById.clear();
        lignesAssociees.clear();
        relationsERD.clear(); // Nettoyer les relations ERD

        if (isUML) {
            ChargerEntites("UML", this.schemaId);
        } else {
            ChargerEntites("ERD", this.schemaId);
        }
    }

    public void ajouterEntite(Map<String, Object> entite) {
        if (!entite.containsKey("id") || ((Integer) entite.get("id")) == -1) {
            String nom = (String) entite.get("nom");
            int positionX = ((Double) entite.get("position_x")).intValue();
            int positionY = ((Double) entite.get("position_y")).intValue();
            String typeSchema = isUML ? "UML" : "ERD";

            int entiteId = entiteDAO.insertEntite(nom, positionX, positionY, typeSchema, loadedSchemaId); // Ajout de loadedSchemaId
            if (entiteId != -1) {
                entite.put("id", entiteId);
                logDAO.insertLog(userId, "Nouvelle entité insérée avec ID : " + entiteId, "INFO");

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
                logDAO.insertLog(userId, "Impossible d'insérer l'entité dans la base de données.", "SEVERE");
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

    public void ChargerEntites(String typeSchema, int schemaId) {
        List<Map<String, Object>> entitiesFromDb = new ArrayList<>();
        try (java.sql.Connection conn = ConnexionBdd.getConnection(); java.sql.PreparedStatement pstmtEntites = conn.prepareStatement("SELECT id, nom, position_x, position_y, type_schema FROM entites WHERE type_schema = ? AND schema_id = ?"); // Ajout du filtre schema_id
                 java.sql.PreparedStatement pstmtAttributs = conn.prepareStatement("SELECT nom, cle_primaire, cle_etrangere, type_schema FROM attributs WHERE entite_id = ? AND type_schema = ?")) {
            pstmtEntites.setString(1, typeSchema);

            pstmtEntites.setInt(2, schemaId); // Définir le paramètre schema_id
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
            logDAO.insertLog(userId, "Erreur lors du chargement des entités : " + e.getMessage(), "SEVERE");
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
        chargerRelationsDepuisBDD(schemaId);

        // Forcer la mise à jour des positions des lignes après chargement
        for (LigneAssociee la : lignesAssociees) {
            la.MajPosition();
        }

    }

    public void chargerRelationsDepuisBDD(int schemaId) {
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

                String relationNom = (String) rel.getOrDefault("nom_relation", "Relation");
                if (!relationExiste(source, cible, typeLien)) {
                    creerLienEntreEntitesAvecCardinalites(source, cible, typeLien, cardSource, cardCible, relationNom);
                }
            }
        }
    }

    // ERIC: AJOUT DE LA MÉTHODE getSchemaId() MANQUANTE
    public int getSchemaId() {
        return (loadedSchemaId != -1) ? loadedSchemaId : schemaId;
    }

    public void creerLienEntreEntitesAvecCardinalites(Map<String, Object> source, Map<String, Object> cible, String typeLien,
            String cardSource, String cardCible, String relationNom) {
        if (source == null || cible == null || source.equals(cible)) {
            System.err.println("ERREUR : Source ou cible null ou identiques. Retour prématuré.");
            return;
        }
        if (relationNom == null || relationNom.isEmpty()) {
            relationNom = "Relation";
        }

        Group g1 = entiteToGroup.get((Integer) source.get("id"));
        Group g2 = entiteToGroup.get((Integer) cible.get("id"));
        if (g1 == null || g2 == null) {
            System.err.println("ERREUR : Group source ou cible non trouvé. Retour prématuré.");
            return;
        }

        // Log avant d'entrer dans les blocs mode
        System.out.println("Mode actuel : isUML = " + isUML + " (ERD attendu si typeLien='Relation': false)");
        System.out.println("TypeLien reçu : '" + typeLien + "' (UML si 'Héritage', ERD si 'Relation')");
        System.out.println("CardSource : '" + cardSource + "', CardCible : '" + cardCible + "', RelationNom : '" + relationNom + "'");
        System.out.println("Taille du contentGroup avant traitement : " + contentGroup.getChildren().size());

        // ====== Décision basée sur typeLien (plus robuste que isUML) ======
        if (typeLien.equals("Héritage")) {
            // Bloc UML (héritage ou association UML)
            System.out.println(">>> Entrée dans bloc UML (typeLien='Héritage') <<<");
            try {
                Line ligne = new Line();
                ligne.setStroke(Color.GREEN); // Vert pour héritage
                ligne.setStrokeWidth(3);

                LigneAssociee la = new LigneAssociee(ligne, source, cible, cardSource, cardCible);
                lignesAssociees.add(la);

                la.MajPosition();
                contentGroup.getChildren().add(0, ligne);
                contentGroup.getChildren().addAll(la.cardinaliteSourceText, la.cardinaliteCibleText);

                System.out.println("Ajout UML réussi. Taille contentGroup après UML : " + contentGroup.getChildren().size());
            } catch (Exception e) {
                System.err.println("ERREUR dans bloc UML : " + e.getMessage());
                e.printStackTrace();
            }

        } else {
            // Bloc ERD (pour typeLien='Relation' ou autre)
            System.out.println(">>> Entrée dans bloc ERD (typeLien != 'Héritage') <<<");

            // Force le mode ERD si mismatch (sécurité)
            if (isUML) {
                System.out.println("AVERTISSEMENT : isUML=true mais typeLien indique ERD. Forçage temporaire à ERD.");
                isUML = false; // Temporaire pour ce traitement ; reset si besoin après
            }

            // Étape 1 : Vérification relation existante
            if (relationExiste(source, cible, typeLien)) {
                System.err.println("ERREUR : Relation ERD existe déjà. Ignorée.");
                return;
            }

            // Étape 2 : Création RelationERD
            RelationERD relationERD = null;
            try {
                System.out.println("Création de RelationERD...");
                relationERD = new RelationERD(source, cible, entiteToGroup, relationNom, cardSource, cardCible);
                System.out.println("RelationERD créée.");
            } catch (Exception e) {
                System.err.println("ERREUR création RelationERD : " + e.getMessage());
                e.printStackTrace();
                return;
            }

            // Étape 3 : Récupération et config éléments
            try {
                Line ligne1 = relationERD.getLigne1();
                Line ligne2 = relationERD.getLigne2();
                // Ellipse : Commentez si getEllipse() n'existe pas
                // Ellipse ellipse = relationERD.getEllipse();
                Group relationGroup = relationERD.getRelationGroup();
                Text cardinaliteSourceText = relationERD.getCardinaliteSourceText();
                Text cardinaliteCibleText = relationERD.getCardinaliteCibleText();

                System.out.println("Éléments - Ligne1 null? " + (ligne1 == null) + ", Ligne2 null? " + (ligne2 == null)
                        + ", RelationGroup null? " + (relationGroup == null)
                        + ", CardSource null? " + (cardinaliteSourceText == null)
                        + ", CardCible null? " + (cardinaliteCibleText == null));

                // Initialisation si null (exemple)
                if (ligne1 == null) {
                    ligne1 = new Line();
                    ligne1.setStroke(Color.BLACK);
                    ligne1.setStrokeWidth(2);
                    ligne1.setVisible(true);
                }
                if (ligne2 == null) {
                    ligne2 = new Line();
                    ligne2.setStroke(Color.BLACK);
                    ligne2.setStrokeWidth(2);
                    ligne2.setVisible(true);
                }

                // Visibilité forcée
                ligne1.setVisible(true);
                ligne1.setOpacity(1.0);
                ligne2.setVisible(true);
                ligne2.setOpacity(1.0);
                // if (ellipse != null) { ellipse.setVisible(true); ellipse.setOpacity(1.0); }
                relationGroup.setVisible(true);
                relationGroup.setOpacity(1.0);
                if (cardinaliteSourceText != null) {
                    cardinaliteSourceText.setVisible(true);
                    cardinaliteSourceText.setOpacity(1.0);
                    cardinaliteSourceText.setFill(Color.BLACK);
                    System.out.println("Card source text: '" + cardinaliteSourceText.getText() + "'");
                }
                if (cardinaliteCibleText != null) {
                    cardinaliteCibleText.setVisible(true);
                    cardinaliteCibleText.setOpacity(1.0);
                    cardinaliteCibleText.setFill(Color.BLACK);
                    System.out.println("Card cible text: '" + cardinaliteCibleText.getText() + "'");
                }

            } catch (Exception e) {
                System.err.println("ERREUR config éléments : " + e.getMessage());
                e.printStackTrace();
                return;
            }

            // Étape 4 : Mise à jour positions
            try {
                System.out.println("Mise à jour positions...");
                relationERD.mettreAJourPositions();
                System.out.println("Positions mises à jour.");
            } catch (Exception e) {
                System.err.println("ERREUR positions : " + e.getMessage());
                e.printStackTrace();
                return;
            }

            // Étape 5 : Ajout séquentiel
            System.out.println("Taille avant ajout ERD : " + contentGroup.getChildren().size());
            try {
                contentGroup.getChildren().add(relationERD.getLigne1());
                contentGroup.getChildren().add(relationERD.getLigne2());
                // contentGroup.getChildren().add(relationERD.getEllipse()); // Si existe
                contentGroup.getChildren().add(relationERD.getRelationGroup());
                if (relationERD.getCardinaliteSourceText() != null) {
                    contentGroup.getChildren().add(relationERD.getCardinaliteSourceText());
                }
                if (relationERD.getCardinaliteCibleText() != null) {
                    contentGroup.getChildren().add(relationERD.getCardinaliteCibleText());
                }

                relationsERD.add(relationERD);
                source.put("relation_text", relationERD.getRelationGroup().getChildren().get(1));

                System.out.println("Taille après ajout ERD : " + contentGroup.getChildren().size());
                System.out.println(">>> ERD réussi ! <<<");
            } catch (Exception e) {
                System.err.println("ERREUR ajout : " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        System.out.println("Fin de creerLienEntreEntitesAvecCardinalites.");
    }

    /**
     * Crée un lien entre deux entités avec des cardinalités par défaut [ ].
     */
    public void creerLienEntreEntites(Map<String, Object> source, Map<String, Object> cible, String typeLien) {
        String relationNom = "Relation";
        creerLienEntreEntitesAvecCardinalites(source, cible, typeLien, "[ ]", "[ ]", relationNom);

    }

    private void ajouterEntiteUML(Map<String, Object> entite) {
        Integer entiteId = (Integer) entite.get("id");
        Group entiteVisuelle = entiteToGroup.get(entiteId);

        if (entiteVisuelle == null) {
            entiteVisuelle = new Group();
            visuel.ajouterEntite(entiteVisuelle, entite, "UML");
            setupEntiteInteraction(entiteVisuelle, entite);
            contentGroup.getChildren().add(entiteVisuelle);
            entiteToGroup.put(entiteId, entiteVisuelle);
        } else {
            // Mettre à jour le contenu sans recréer ni réajouter
            visuel.ajouterEntite(entiteVisuelle, entite, "UML");
            // Ne rien faire d'autre, surtout ne pas réajouter dans contentGroup
        }
    }

    private void ajouterEntiteERD(Map<String, Object> entite) {
        Integer entiteId = (Integer) entite.get("id");
        Group entiteVisuelle = entiteToGroup.get(entiteId);

        if (entiteVisuelle == null) {
            entiteVisuelle = new Group();
            visuel.ajouterEntite(entiteVisuelle, entite, "ERD");
            setupEntiteInteraction(entiteVisuelle, entite);
            contentGroup.getChildren().add(entiteVisuelle);
            entiteToGroup.put(entiteId, entiteVisuelle);
        } else {
            visuel.ajouterEntite(entiteVisuelle, entite, "ERD");
        }
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
            double newX = event.getSceneX() + dragDelta.x;
            double newY = event.getSceneY() + dragDelta.y;

            if (snapActive) {
                // Snap magnétique sur grille 10 px
                newX = Math.round(newX / 10) * 10;
                newY = Math.round(newY / 10) * 10;
            }

            entiteVisuelle.setLayoutX(newX);
            entiteVisuelle.setLayoutY(newY);

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

        // Mise à jour des lignes UML
        for (LigneAssociee la : lignesAssociees) {
            if (la.e1.equals(entite) || la.e2.equals(entite)) {
                la.MajPosition();
            }
        }

        // Mise à jour des relations ERD
        for (RelationERD relationERD : relationsERD) {
            if (relationERD.concerneEntite(entite)) {
                relationERD.mettreAJourPositions();
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
            contentGroup.getChildren().remove(oldGroup);  // <-- retirer de contentGroup, pas de this.getChildren()
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
        // Vérifier dans les lignes UML
        for (LigneAssociee ligne : lignesAssociees) {
            boolean memeType = (typeLien.equals("Héritage") && ligne.ligne.getStroke().equals(Color.GREEN))
                    || (typeLien.equals("Relation") && ligne.ligne.getStroke().equals(Color.BLACK));
            boolean memeCouple = (ligne.e1.equals(source) && ligne.e2.equals(cible))
                    || (ligne.e1.equals(cible) && ligne.e2.equals(source));
            if (memeType && memeCouple) {
                return true;
            }
        }

        // Vérifier dans les relations ERD
        for (RelationERD relationERD : relationsERD) {
            boolean memeCouple = (relationERD.getEntiteSource().equals(source) && relationERD.getEntiteCible().equals(cible))
                    || (relationERD.getEntiteSource().equals(cible) && relationERD.getEntiteCible().equals(source));
            if (memeCouple) {
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
        // Intercepter la touche S pour toggle snap
        if (event.getCode() == KeyCode.S) {
            snapActive = !snapActive;
            logDAO.insertLog(userId, "Snap magnétique " + (snapActive ? "activé" : "désactivé"), "INFO");
            event.consume(); // optionnel : empêche propagation si besoin
            return; // on ne transmet pas à visuel car c'est une touche spécifique ici
        }
        // Sinon, déléguer à visuel pour Ctrl+Z / Ctrl+Y
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
        Group relationGroup;

        // Pour UML (pas de Text fourni)
        public LigneAssociee(Line l, Map<String, Object> e1, Map<String, Object> e2,
                String cardSource, String cardCible) {
            this.ligne = l;
            this.e1 = e1;
            this.e2 = e2;
            this.cardSource = cardSource;
            this.cardCible = cardCible;
            this.cardinaliteSourceText = new Text(cardSource != null ? cardSource : "");
            this.cardinaliteCibleText = new Text(cardCible != null ? cardCible : "");
        }

        // Pour ERD (Text fourni)
        public LigneAssociee(Line l, Map<String, Object> e1, Map<String, Object> e2,
                String cardSource, String cardCible,
                Text cardSourceText, Text cardCibleText, Group relationGroup) {
            this.ligne = l;
            this.e1 = e1;
            this.e2 = e2;
            this.cardSource = cardSource;
            this.cardCible = cardCible;
            this.cardinaliteSourceText = cardSourceText;  // <- il manquait ça
            this.cardinaliteCibleText = cardCibleText;   // <- il manquait ça
            this.relationGroup = relationGroup;
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

                // Positionner l'ellipse et le texte au milieu de la ligne
                if (relationGroup != null) {
                    double midX = (point1[0] + point2[0]) / 2;
                    double midY = (point1[1] + point2[1]) / 2;

                    // Centrer le group sur midX/midY
                    double groupWidth = relationGroup.getBoundsInParent().getWidth();
                    double groupHeight = relationGroup.getBoundsInParent().getHeight();
                    relationGroup.setLayoutX(midX - groupWidth / 2);
                    relationGroup.setLayoutY(midY - groupHeight / 2);
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

    private void repositionnerPositionCurseur() {
        double paddingFromEdge = 10;
        double width = this.getWidth();
        double height = this.getHeight();

        double groupWidth = positionCurseurBackground.getWidth();
        double groupHeight = positionCurseurBackground.getHeight();

        if (width > 0 && height > 0) {
            positionCurseurGroup.setLayoutX(width - groupWidth - paddingFromEdge);
            positionCurseurGroup.setLayoutY(height - groupHeight - paddingFromEdge);
        }
    }

    public void supprimerEntite(int entiteId) {
        // Supprimer l'entité des maps
        Map<String, Object> entite = entiteById.remove(entiteId);
        Group group = entiteToGroup.remove(entiteId);

        if (group != null) {
            contentGroup.getChildren().remove(group);
        }

        // Supprimer les liens associés
        lignesAssociees.removeIf(la -> {
            Map<String, Object> src = la.e1;
            Map<String, Object> dst = la.e2;
            if ((int) src.get("id") == entiteId || (int) dst.get("id") == entiteId) {
                contentGroup.getChildren().remove(la.ligne);
                contentGroup.getChildren().removeAll(la.cardinaliteSourceText, la.cardinaliteCibleText);
                if (la.relationGroup != null) {
                    contentGroup.getChildren().remove(la.relationGroup);
                }
                return true;
            }
            return false;
        });

    }

    // ERIC: AJOUT DES MÉTHODES MANQUANTES POUR NAVIGATIONMENU - DÉBUT
    /**
     * Réinitialise complètement le diagramme
     */
    public void clear() {
        contentGroup.getChildren().clear();
        entiteToGroup.clear();
        entiteById.clear();
        lignesAssociees.clear();
        relationsERD.clear();
        loadedSchemaId = -1;
        schemaId = -1;
    }
    
    /**
     * Vérifie si le diagramme contient des données
     */
    public boolean isEmpty() {
        return entiteById.isEmpty() && relationsERD.isEmpty() && lignesAssociees.isEmpty();
    }
    
    /**
     * Récupère toutes les données du diagramme pour l'export
     */
    public Map<String, Object> getDiagramData() {
        Map<String, Object> data = new HashMap<>();
        data.put("entites", new ArrayList<>(entiteById.values()));
        data.put("relationsERD", new ArrayList<>(relationsERD));
        data.put("lignesAssociees", new ArrayList<>(lignesAssociees));
        data.put("schemaId", schemaId);
        data.put("isUML", isUML);
        data.put("loadedSchemaId", loadedSchemaId);
        data.put("dateExport", new Date());
        
        return data;
    }
    
    public void loadDiagramData(Map<String, Object> data) {
        // Confirmer avant de perdre les modifications
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Chargement de diagramme");
        alert.setHeaderText("Vous êtes sur le point de charger un nouveau diagramme");
        alert.setContentText("Les modifications non sauvegardées seront perdues. Continuer ?");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }
        
        // Nettoyer l'état actuel
        contentGroup.getChildren().clear();
        entiteToGroup.clear();
        entiteById.clear();
        lignesAssociees.clear();
        relationsERD.clear();
        
        // Charger les nouvelles données
        List<Map<String, Object>> entites = (List<Map<String, Object>>) data.get("entites");
        
        if (entites != null) {
            for (Map<String, Object> entite : entites) {
                ajouterEntite(entite);
            }
        }
        
        // Charger les relations
        List<Map<String, Object>> relations = (List<Map<String, Object>>) data.get("relations");
        if (relations != null && !relations.isEmpty()) {
            for (Map<String, Object> relation : relations) {
                try {
                    String type = (String) relation.get("type");
                    int sourceId = (int) relation.get("source_id");
                    int cibleId = (int) relation.get("cible_id");
                    
                    Map<String, Object> source = entiteById.get(sourceId);
                    Map<String, Object> cible = entiteById.get(cibleId);
                    
                    if (source != null && cible != null) {
                        String cardSource = (String) relation.get("cardinalite_source");
                        String cardCible = (String) relation.get("cardinalite_cible");
                        String relationNom = "Relation importée";
                        
                        creerLienEntreEntitesAvecCardinalites(source, cible, type, cardSource, cardCible, relationNom);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement d'une relation: " + e.getMessage());
                }
            }
        }
        
        // Mettre à jour le type de schéma si nécessaire
        Boolean isUMLData = (Boolean) data.get("isUML");
        if (isUMLData != null && isUMLData != this.isUML) {
            setTypeSchema(isUMLData);
        }
        
        logDAO.insertLog(userId, "Diagramme chargé depuis fichier", "INFO");
    }
    // ERIC: AJOUT DES MÉTHODES MANQUANTES POUR NAVIGATIONMENU - FIN
}
