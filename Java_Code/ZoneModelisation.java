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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import java.util.Date;

// AJOUT DES IMPORTS POUR LE MENU CONTEXTUEL
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

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

    //AJOUT DU SYSTÈME D'HISTORIQUE POUR UNDO/REDO
    private List<Map<String, Object>> history = new ArrayList<>();
    private int currentHistoryIndex = -1;
    private static final int MAX_HISTORY_SIZE = 50;

    // VARIABLES POUR LE MENU CONTEXTUEL ET CLIPBOARD
    private ContextMenu contextMenu;
    private MenuItem copyItem;
    private MenuItem cutItem;
    private MenuItem pasteItem;
    private Map<String, Object> clipboardData;
    private String clipboardOperation; // "copy" ou "cut"
    private Map<String, Object> lastSelectedEntity;

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

        // INITIALISATION DU MENU CONTEXTUEL
        setupContextMenu();

        //SAUVEGARDE INITIALE DANS L'HISTORIQUE
        saveToHistory();
    }

    // MÉTHODE POUR INITIALISER LE MENU CONTEXTUEL
    private void setupContextMenu() {
        contextMenu = new ContextMenu();
        
        copyItem = new MenuItem("Copier");
        cutItem = new MenuItem("Couper");
        pasteItem = new MenuItem("Coller");
        
        // Style des items du menu
        copyItem.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
        cutItem.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
        pasteItem.setStyle("-fx-font-size: 12px; -fx-padding: 5 10 5 10;");
        
        contextMenu.getItems().addAll(copyItem, cutItem, new SeparatorMenuItem(), pasteItem);
        
        // Actions des menus
        copyItem.setOnAction(e -> copySelectedEntity());
        cutItem.setOnAction(e -> cutSelectedEntity());
        pasteItem.setOnAction(e -> pasteEntity());
        
        // Gérer l'affichage du menu contextuel
        this.setOnContextMenuRequested(event -> {
            // Vérifier si on clique sur une entité
            Node clickedNode = event.getPickResult().getIntersectedNode();
            Map<String, Object> clickedEntity = findEntityFromNode(clickedNode);
            
            if (clickedEntity != null) {
                // Clic sur une entité - activer copier/couper
                copyItem.setDisable(false);
                cutItem.setDisable(false);
                lastSelectedEntity = clickedEntity;
                
                // Notifier le sélection listener
                if (selectionListener != null) {
                    selectionListener.onSelection(clickedEntity);
                }
            } else {
                // Clic dans le vide - désactiver copier/couper
                copyItem.setDisable(true);
                cutItem.setDisable(true);
                lastSelectedEntity = null;
            }
            
            // Activer/désactiver coller selon le contenu du clipboard
            pasteItem.setDisable(clipboardData == null);
            
            contextMenu.show(this, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        
        // Cacher le menu contextuel quand on clique ailleurs
        this.setOnMousePressed(event -> {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });

        // Configurer les raccourcis clavier
        setupKeyboardShortcuts();
    }

    // MÉTHODE POUR CONFIGURER LES RACCOURCIS CLAVIER
    private void setupKeyboardShortcuts() {
        this.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown()) {
                switch (event.getCode()) {
                    case C:
                        if (lastSelectedEntity != null) {
                            copySelectedEntity();
                            event.consume();
                        }
                        break;
                    case X:
                        if (lastSelectedEntity != null) {
                            cutSelectedEntity();
                            event.consume();
                        }
                        break;
                    case V:
                        if (clipboardData != null) {
                            pasteEntity();
                            event.consume();
                        }
                        break;
                }
            }
        });
    }

    // MÉTHODE POUR TROUVER L'ENTITÉ CORRESPONDANT À UN NODE
    private Map<String, Object> findEntityFromNode(Node node) {
        if (node == null) return null;
        
        // Remonter dans la hiérarchie parentale pour trouver le Group de l'entité
        Node currentNode = node;
        while (currentNode != null && currentNode != this) {
            if (currentNode instanceof Group) {
                for (Map.Entry<Integer, Group> entry : entiteToGroup.entrySet()) {
                    if (entry.getValue() == currentNode) {
                        return entiteById.get(entry.getKey());
                    }
                }
            }
            currentNode = currentNode.getParent();
        }
        return null;
    }

    // MÉTHODE POUR COPIER UNE ENTITÉ
    private void copySelectedEntity() {
        if (lastSelectedEntity != null) {
            // SAUVEGARDE HISTORIQUE
            saveToHistory();
            
            // Copier les données de l'entité
            clipboardData = deepCopyEntity(lastSelectedEntity);
            clipboardOperation = "copy";
            
            logDAO.insertLog(userId, "Entité copiée: " + lastSelectedEntity.get("nom"), "INFO");
            
            // Afficher un message de confirmation
            showStatusMessage("Entité '" + lastSelectedEntity.get("nom") + "' copiée");
        }
    }

    // MÉTHODE POUR COUPER UNE ENTITÉ
    private void cutSelectedEntity() {
        if (lastSelectedEntity != null) {
            // SAUVEGARDE HISTORIQUE
            saveToHistory();
            
            // Copier les données de l'entité
            clipboardData = deepCopyEntity(lastSelectedEntity);
            clipboardOperation = "cut";
            
            String entityName = (String) lastSelectedEntity.get("nom");
            
            // Supprimer l'entité originale si c'est un cut
            supprimerEntite((Integer) lastSelectedEntity.get("id"));
            
            logDAO.insertLog(userId, "Entité coupée: " + entityName, "INFO");
            
            // Afficher un message de confirmation
            showStatusMessage("Entité '" + entityName + "' coupée");
            
            lastSelectedEntity = null;
        }
    }

    // MÉTHODE POUR COLLER UNE ENTITÉ
    private void pasteEntity() {
        if (clipboardData != null) {
            // SAUVEGARDE HISTORIQUE
            saveToHistory();
            
            // Créer une nouvelle entité basée sur les données du clipboard
            Map<String, Object> newEntity = deepCopyEntity(clipboardData);
            
            // Décaler la position pour éviter le chevauchement
            double originalX = (double) newEntity.get("position_x");
            double originalY = (double) newEntity.get("position_y");
            newEntity.put("position_x", originalX + 30);
            newEntity.put("position_y", originalY + 30);
            
            // Générer un nouveau nom si nécessaire pour éviter les doublons
            String originalName = (String) newEntity.get("nom");
            String newName = generateUniqueName(originalName);
            newEntity.put("nom", newName);
            
            // Réinitialiser l'ID pour créer une nouvelle entité
            newEntity.put("id", -1);
            
            // Ajouter la nouvelle entité
            ajouterEntite(newEntity);
            
            // Si c'était un cut, vider le clipboard après le collage
            if ("cut".equals(clipboardOperation)) {
                clipboardData = null;
                clipboardOperation = null;
            }
            
            logDAO.insertLog(userId, "Entité collée: " + newName, "INFO");
            
            // Afficher un message de confirmation
            showStatusMessage("Entité '" + newName + "' collée");
        }
    }

    // MÉTHODE POUR GÉNÉRER UN NOM UNIQUE
    private String generateUniqueName(String baseName) {
        String newName = baseName;
        int counter = 1;
        
        while (entiteNameExists(newName)) {
            newName = baseName + "_" + counter;
            counter++;
        }
        
        return newName;
    }

    // MÉTHODE POUR VÉRIFIER SI UN NOM D'ENTITÉ EXISTE DÉJÀ
    private boolean entiteNameExists(String name) {
        for (Map<String, Object> entite : entiteById.values()) {
            if (name.equals(entite.get("nom"))) {
                return true;
            }
        }
        return false;
    }

    // MÉTHODE POUR FAIRE UNE COPIE PROFONDE D'UNE ENTITÉ
    private Map<String, Object> deepCopyEntity(Map<String, Object> original) {
        Map<String, Object> copy = new HashMap<>();
        
        // Copier les propriétés de base
        copy.put("id", original.get("id"));
        copy.put("nom", original.get("nom"));
        copy.put("position_x", original.get("position_x"));
        copy.put("position_y", original.get("position_y"));
        copy.put("type_schema", original.get("type_schema"));
        
        // Copier les attributs
        List<Map<String, Object>> originalAttributs = (List<Map<String, Object>>) original.get("attributs");
        if (originalAttributs != null) {
            List<Map<String, Object>> copiedAttributs = new ArrayList<>();
            for (Map<String, Object> attribut : originalAttributs) {
                Map<String, Object> copiedAttribut = new HashMap<>();
                copiedAttribut.put("nom", attribut.get("nom"));
                copiedAttribut.put("cle_primaire", attribut.get("cle_primaire"));
                copiedAttribut.put("cle_etrangere", attribut.get("cle_etrangere"));
                copiedAttribut.put("type_schema", attribut.get("type_schema"));
                copiedAttributs.add(copiedAttribut);
            }
            copy.put("attributs", copiedAttributs);
        }
        
        return copy;
    }

    // MÉTHODE POUR AFFICHER UN MESSAGE DE STATUT
    private void showStatusMessage(String message) {
        // Cette méthode pourrait être connectée à votre barre de statut
        System.out.println("STATUS: " + message);
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
        //SAUVEGARDE AVANT MODIFICATION
        saveToHistory();

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

    //AJOUT DE LA MÉTHODE getSchemaId() MANQUANTE
    public int getSchemaId() {
        return (loadedSchemaId != -1) ? loadedSchemaId : schemaId;
    }

    public void creerLienEntreEntitesAvecCardinalites(Map<String, Object> source, Map<String, Object> cible, String typeLien,
            String cardSource, String cardCible, String relationNom) {
        // SAUVEGARDE AVANT MODIFICATION
        saveToHistory();

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

        if (typeLien.equals("Héritage")) {
            // Bloc UML (héritage ou association UML)
            System.out.println(">>> Entrée dans bloc UML (typeLien='Héritage') <<<");
            
            // AJOUT : Vérification d'existence en BDD avant insertion (pour éviter doublons)
            try {
                RelationDAO relationDAOCheck = new RelationDAO();
                // Note : Adaptez si getRelationBySourceId n'existe pas ; utilisez une requête personnalisée
                // Exemple : Map<String, Object> existingRel = relationDAOCheck.getRelationBySourceAndTarget((Integer) source.get("id"), (Integer) cible.get("id"));
                Map<String, Object> existingRel = relationDAOCheck.getRelationBySourceId((Integer) source.get("id"));
                if (existingRel != null && ((Integer) existingRel.get("entite_cible_id")).equals((Integer) cible.get("id"))) {
                    System.err.println("ERREUR : Héritage UML existe déjà en BDD. Ignorée.");
                    return;  // Éviter création si doublon en BDD
                }
            } catch (Exception checkEx) {
                System.err.println("ERREUR vérification existence UML en BDD : " + checkEx.getMessage());
                checkEx.printStackTrace();
                // Optionnel : Continuer sans vérif BDD si échec (mais risqué)
            }

            // Création visuelle d'abord (code existant, avec try-catch pour isoler)
            LigneAssociee la = null;  // Pour rollback si besoin
            try {
                Line ligne = new Line();
                ligne.setStroke(Color.GREEN); // Vert pour héritage
                ligne.setStrokeWidth(3);

                la = new LigneAssociee(ligne, source, cible, cardSource, cardCible);
                lignesAssociees.add(la);

                la.MajPosition();
                contentGroup.getChildren().add(0, ligne);
                contentGroup.getChildren().addAll(la.cardinaliteSourceText, la.cardinaliteCibleText);

                System.out.println("Ajout UML réussi. Taille contentGroup après UML : " + contentGroup.getChildren().size());
            } catch (Exception e) {
                System.err.println("ERREUR dans bloc UML : " + e.getMessage());
                e.printStackTrace();
                return;  // Arrêter si visuel échoue
            }

            // AJOUT : Insertion en BDD pour UML (après création visuelle réussie)
            int relationDbId = -1;
            try {
                RelationDAO relationDAO = new RelationDAO();
                relationDbId = relationDAO.insertRelation("Héritage", (Integer) source.get("id"), (Integer) cible.get("id"), cardSource, cardCible, "UML");
                if (relationDbId == -1) {
                    System.err.println("ERREUR : Échec de l'insertion de l'héritage UML en base de données.");
                    // Rollback visuel si échec BDD (supprimer la ligne et textes ajoutés)
                    if (la != null) {
                        contentGroup.getChildren().remove(la.ligne);
                        contentGroup.getChildren().removeAll(la.cardinaliteSourceText, la.cardinaliteCibleText);
                        lignesAssociees.remove(la);
                    }
                    return;  // Ne pas continuer
                }
                logDAO.insertLog(userId, "Héritage UML créé en BDD avec ID: " + relationDbId, "INFO");
                System.out.println("Héritage UML inséré en BDD avec ID: " + relationDbId);
            } catch (Exception dbEx) {
                System.err.println("ERREUR insertion UML en BDD : " + dbEx.getMessage());
                dbEx.printStackTrace();
                // Rollback visuel en cas d'erreur BDD
                if (la != null) {
                    contentGroup.getChildren().remove(la.ligne);
                    contentGroup.getChildren().removeAll(la.cardinaliteSourceText, la.cardinaliteCibleText);
                    lignesAssociees.remove(la);
                }
                return;
            }

        } else {
            // Bloc ERD (pour typeLien='Relation' ou autre)
            System.out.println(">>> Entrée dans bloc ERD (typeLien != 'Héritage') <<<");

            // Force le mode ERD si mismatch (sécurité)
            if (isUML) {
                System.out.println("AVERTISSEMENT : isUML=true mais typeLien indique ERD. Forçage temporaire à ERD.");
                isUML = false; // Temporaire pour ce traitement ; reset si besoin après
            }
            
            
            // MODIFICATION : Vérification d'existence en BDD avant insertion (cohérent avec UML)
            try {
                RelationDAO relationDAOCheck = new RelationDAO();
                // Note : Adaptez si getRelationBySourceId n'existe pas
                Map<String, Object> existingRel = relationDAOCheck.getRelationBySourceId((Integer) source.get("id"));
                if (existingRel != null && ((Integer) existingRel.get("entite_cible_id")).equals((Integer) cible.get("id"))) {
                    System.err.println("ERREUR : Relation ERD existe déjà en BDD. Ignorée.");
                    return;  // Éviter création si doublon en BDD
                }
            } catch (Exception checkEx) {
                System.err.println("ERREUR vérification existence ERD en BDD : " + checkEx.getMessage());
                checkEx.printStackTrace();
                // Optionnel : Continuer sans vérif BDD si échec
            }


            // Étape 1 : Vérification relation existante
            if (relationExiste(source, cible, typeLien)) {
                System.err.println("ERREUR : Relation ERD existe déjà. Ignorée.");
                return;
            }

            int relationDbId = -1;
            try {
                RelationDAO relationDAO = new RelationDAO();
                relationDbId = relationDAO.insertRelation(relationNom, (Integer) source.get("id"), (Integer) cible.get("id"), cardSource, cardCible, "ERD");
                if (relationDbId == -1) {
                    System.err.println("ERREUR : Échec de l'insertion de la relation ERD en base de données.");
                    return; // Ne pas créer la relation visuelle si l'insertion BDD échoue
                }
                // MODIFICATION : Ajout log après insertion réussie (cohérent avec UML)
                logDAO.insertLog(userId, "Relation ERD créée en BDD avec ID: " + relationDbId + " (nom: " + relationNom + ")", "INFO");
                System.out.println("Relation ERD insérée en BDD avec ID: " + relationDbId);
            } catch (Exception e) {
                System.err.println("ERREUR lors de l'insertion de la relation ERD en base de données : " + e.getMessage());
                e.printStackTrace();
                return;
            }

            // Étape 2 : Création RelationERD
            RelationERD relationERD = null;
            try {
                System.out.println("Création de RelationERD...");
                relationERD = new RelationERD(relationDbId, source, cible, entiteToGroup, relationNom, cardSource, cardCible);
                System.out.println("RelationERD créée." + relationDbId);
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

            // ERIC: SAUVEGARDE DANS L'HISTORIQUE APRÈS DRAG
            saveToHistory();
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
        // ERIC: SAUVEGARDE AVANT MODIFICATION
        saveToHistory();

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
        //SAUVEGARDE AVANT MODIFICATION
        saveToHistory();

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
                if (la.relationGroup != null) { // Pour les cas où relationGroup est utilisé (ERD)
                    contentGroup.getChildren().remove(la.relationGroup);
                }
                return true;
            }
            return false;
        });

        relationsERD.removeIf(relERD -> {
            Map<String, Object> src = relERD.getEntiteSource();
            Map<String, Object> dst = relERD.getEntiteCible();
            if ((int) src.get("id") == entiteId || (int) dst.get("id") == entiteId) {
                // Supprimer tous les composants visuels de la relation ERD
                contentGroup.getChildren().remove(relERD.getLigne1());
                contentGroup.getChildren().remove(relERD.getLigne2());
                contentGroup.getChildren().remove(relERD.getRelationGroup()); // Ellipse et son texte
                contentGroup.getChildren().remove(relERD.getCardinaliteSourceText());
                contentGroup.getChildren().remove(relERD.getCardinaliteCibleText());
                return true;
            }
            return false;
        });

        entiteDAO.supprimerEntite(entiteId);
        logDAO.insertLog(userId, "Entité supprimée (ID=" + entiteId + ")", "INFO");

    }

    //AJOUT DES MÉTHODES MANQUANTES POUR NAVIGATIONMENU
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
        history.clear();
        currentHistoryIndex = -1;
        saveToHistory();
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
                    
                    boolean belongsToSchema = false;
		    try (java.sql.Connection conn = ConnexionBdd.getConnection(); java.sql.PreparedStatement pstmt = conn.prepareStatement(
		            "SELECT COUNT(*) FROM entites WHERE id IN (?, ?) AND schema_id = ?")) {
		        pstmt.setInt(1, sourceId);
		        pstmt.setInt(2, cibleId);
		        pstmt.setInt(3, schemaId);
		        java.sql.ResultSet rs = pstmt.executeQuery();
		        if (rs.next() && rs.getInt(1) == 2) {  // Les deux entités doivent être dans le schéma
		            belongsToSchema = true;
		        }
		    } catch (SQLException e) {
		        logDAO.insertLog(userId, "Erreur vérification schéma pour relation: " + e.getMessage(), "WARNING");
		    }

		    if (!belongsToSchema) {
		        continue;  // Ignorer si pas du bon schéma

		    }
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

        //SAUVEGARDE DANS L'HISTORIQUE APRÈS CHARGEMENT
        saveToHistory();
    }


    //MÉTHODES POUR GÉRER L'HISTORIQUE - DÉBUT
    public void saveToHistory() {
        // Supprimer les états futurs si on ajoute un nouvel état après avoir fait undo
        if (currentHistoryIndex < history.size() - 1) {
            history = new ArrayList<>(history.subList(0, currentHistoryIndex + 1));
        }

        // Créer un snapshot de l'état actuel
        Map<String, Object> snapshot = new HashMap<>();
        
        // Sauvegarder les entités avec leurs positions et attributs
        List<Map<String, Object>> entitesSnapshot = new ArrayList<>();
        for (Map<String, Object> entite : entiteById.values()) {
            Map<String, Object> entiteCopy = new HashMap<>(entite);
            entitesSnapshot.add(entiteCopy);
        }
        snapshot.put("entites", entitesSnapshot);
        
        snapshot.put("isUML", isUML);
        snapshot.put("schemaId", schemaId);
        
        // Sauvegarder les relations UML
        List<Map<String, Object>> relationsSnapshot = new ArrayList<>();
        for (LigneAssociee ligne : lignesAssociees) {
            Map<String, Object> relationData = new HashMap<>();
            relationData.put("sourceId", ligne.e1.get("id"));
            relationData.put("cibleId", ligne.e2.get("id"));
            relationData.put("typeLien", ligne.ligne.getStroke().equals(Color.GREEN) ? "Héritage" : "Relation");
            relationData.put("cardSource", ligne.cardSource);
            relationData.put("cardCible", ligne.cardCible);
            relationsSnapshot.add(relationData);
        }
        snapshot.put("relations", relationsSnapshot);

        history.add(snapshot);
        currentHistoryIndex = history.size() - 1;

        // Limiter la taille de l'historique
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(0);
            currentHistoryIndex--;
        }

        System.out.println("Historique sauvegardé. Taille: " + history.size() + ", Index courant: " + currentHistoryIndex);
    }

    public boolean canUndo() {
        return currentHistoryIndex > 0;
    }

    public boolean canRedo() {
        return currentHistoryIndex < history.size() - 1;
    }

    public void undo() {
        if (!canUndo()) {
            System.out.println("Impossible d'annuler - historique vide ou au début");
            return;
        }

        currentHistoryIndex--;
        restoreFromHistory(currentHistoryIndex);
        logDAO.insertLog(userId, "Action annulée (Undo)", "INFO");
    }

    public void redo() {
        if (!canRedo()) {
            System.out.println("Impossible de rétablir - historique à la fin");
            return;
        }

        currentHistoryIndex++;
        restoreFromHistory(currentHistoryIndex);
        logDAO.insertLog(userId, "Action rétablie (Redo)", "INFO");
    }

    private void restoreFromHistory(int index) {
        if (index < 0 || index >= history.size()) {
            System.out.println("Index d'historique invalide: " + index);
            return;
        }

        Map<String, Object> snapshot = history.get(index);

        // Nettoyer l'état actuel
        contentGroup.getChildren().clear();
        entiteToGroup.clear();
        entiteById.clear();
        lignesAssociees.clear();
        relationsERD.clear();

        // Restaurer le type de schéma
        Boolean savedIsUML = (Boolean) snapshot.get("isUML");
        if (savedIsUML != null) {
            this.isUML = savedIsUML;
        }

        // Restaurer les entités
        List<Map<String, Object>> savedEntites = (List<Map<String, Object>>) snapshot.get("entites");
        if (savedEntites != null) {
            for (Map<String, Object> entite : savedEntites) {
                entiteById.put((Integer) entite.get("id"), entite);
                if (isUML) {
                    ajouterEntiteUML(entite);
                } else {
                    ajouterEntiteERD(entite);
                }
            }
        }

        // Restaurer les relations
        List<Map<String, Object>> savedRelations = (List<Map<String, Object>>) snapshot.get("relations");
        if (savedRelations != null) {
            for (Map<String, Object> relation : savedRelations) {
                try {
                    Integer sourceId = (Integer) relation.get("sourceId");
                    Integer cibleId = (Integer) relation.get("cibleId");
                    String typeLien = (String) relation.get("typeLien");
                    String cardSource = (String) relation.get("cardSource");
                    String cardCible = (String) relation.get("cardCible");

                    Map<String, Object> source = entiteById.get(sourceId);
                    Map<String, Object> cible = entiteById.get(cibleId);

                    if (source != null && cible != null) {
                        creerLienEntreEntitesAvecCardinalites(source, cible, typeLien, cardSource, cardCible, "Relation restaurée");
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors de la restauration d'une relation: " + e.getMessage());
                }
            }
        }

        System.out.println("État restauré depuis l'historique. Index: " + index);
    }

}