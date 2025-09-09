
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.Alert.AlertType;
import javafx.geometry.Pos;

public class BarreOutils extends VBox {

    private ZoneModelisation zoneModelisation;
    private InsertionDonnees dbManager;
    private InterfaceGenerateurUML interfaceRef;
    private Map<String, Map<String, Object>> entitesCreees = new HashMap<>();
    private Map<String, Map<String, Object>> entiteCourante = new HashMap<>();
    private VBox relationContainer;
    private VBox heritageContainer;
    private VBox unitsList;
    private LogDAO logDAO = new LogDAO();
    String userId = UserSession.getInstance().getUserId();

    // Nouveaux champs pour création lien
    private ComboBox<String> cbEntiteSource;
    private ComboBox<String> cbEntiteCible;
    private Button btnCreerLien;

    public BarreOutils(ZoneModelisation zoneModelisation, InterfaceGenerateurUML interfaceRef) {
        this.zoneModelisation = zoneModelisation;
        this.interfaceRef = interfaceRef;
        this.dbManager = new InsertionDonnees(); // Initialiser le gestionnaire de base de données
        setupUI();
        setupVisibilityListeners();
    }

    private void setupUI() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: #F4F6F7;");
        this.setPrefWidth(200);
        this.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Unité");
        title.setFont(Font.font("Arial", 14));

        unitsList = new VBox(10);
        unitsList.setAlignment(Pos.TOP_CENTER);

        String[] unitLabels = {"Entité", "Attribut", "Relation", "Clé primaire", "Clé étrangère", "Héritage"};
        for (String label : unitLabels) {
            VBox container = new VBox(5);
            container.setAlignment(Pos.TOP_CENTER);

            Label lblUnit = new Label(label);
            lblUnit.setFont(Font.font("Arial", 12));

            VBox addedItems = new VBox(3);
            addedItems.setAlignment(Pos.TOP_CENTER);

            Button btnAdd = new Button("+ Add");
            btnAdd.setPrefWidth(150);
            btnAdd.setOnAction(e -> {
                HBox nouvelElement = createUnitItem(label, addedItems, container);
                addedItems.getChildren().add(nouvelElement);
            });

            container.getChildren().addAll(lblUnit, btnAdd, addedItems);

            if (label.equals("Relation")) {
                relationContainer = container;
            }
            if (label.equals("Héritage")) {
                heritageContainer = container;
            }

            unitsList.getChildren().add(container);
        }

        // Bouton génération BLOC
        Button btnGenererBloc = new Button("GÉNÉRER BLOC");
        btnGenererBloc.setPrefWidth(150);
        btnGenererBloc.setOnAction(e -> {
            for (javafx.scene.Node node : unitsList.getChildren()) {
                if (node instanceof VBox container) {
                    VBox addedItems = (VBox) container.getChildren().get(2);
                    for (javafx.scene.Node itemNode : addedItems.getChildren()) {
                        if (itemNode instanceof HBox hbox) {
                            TextField tf = (TextField) hbox.getChildren().get(1);
                            String valeur = tf.getText().trim();
                            if (!valeur.isEmpty()) {
                                switch (((Label) container.getChildren().get(0)).getText()) {
                                    case "Entité" ->
                                        creerEntite(valeur);
                                    case "Attribut" ->
                                        ajouterAttribut(valeur);
                                    case "Clé primaire" ->
                                        ajouterClePrimaire(valeur);
                                    case "Clé étrangère" ->
                                        ajouterCleEtrangere(valeur);
                                    case "Relation" ->
                                        creerRelation(valeur);
                                    case "Héritage" ->
                                        creerHeritage(valeur);
                                }
                            }
                        }
                    }
                }
            }
            showAlert("Bloc généré", "Tous les éléments ont été ajoutés à la zone de modélisation.");
            purgerBarreOutils();
        });

        this.getChildren().addAll(title, unitsList, btnGenererBloc);

        // --- Section création de lien ---
        Label lblLien = new Label("Créer Relation / Héritage");
        lblLien.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");

        cbEntiteSource = new ComboBox<>();
        cbEntiteSource.setPromptText("Source");

        cbEntiteCible = new ComboBox<>();
        cbEntiteCible.setPromptText("Cible");

// --- Cardinalités ---
        ComboBox<String> cbCardinaliteSource = new ComboBox<>();
        cbCardinaliteSource.getItems().addAll("1,1", "0,1", "0,N", "1,N");
        cbCardinaliteSource.setPromptText("Cardinalité");

        ComboBox<String> cbCardinaliteCible = new ComboBox<>();
        cbCardinaliteCible.getItems().addAll("1,1", "0,1", "0,N", "1,N");
        cbCardinaliteCible.setPromptText("Cardinalité");

        btnCreerLien = new Button("Créer Lien");
        btnCreerLien.setOnAction(e -> {
            String sourceNom = cbEntiteSource.getValue();
            String cibleNom = cbEntiteCible.getValue();
            String cardSource = cbCardinaliteSource.getValue();
            String cardCible = cbCardinaliteCible.getValue();

            if (sourceNom == null || cibleNom == null || cardSource == null || cardCible == null) {
                showAlert("Sélection invalide", "Veuillez sélectionner une entité source, une entité cible et les cardinalités.");
                return;
            }
            if (sourceNom.equals(cibleNom)) {
                showAlert("Sélection invalide", "La source et la cible doivent être différentes.");
                return;
            }

            Map<String, Object> source = zoneModelisation.getEntiteParNom(sourceNom);
            Map<String, Object> cible = zoneModelisation.getEntiteParNom(cibleNom);
            String typeLien = zoneModelisation.isUML() ? "Héritage" : "Relation";

            if (relationExiste(source, cible, typeLien)) {
                showAlert("Relation existante", "Un " + typeLien + " existe déjà entre ces deux entités.");
                return;
            }

            // Passer les cardinalités à la zone
            zoneModelisation.creerLienEntreEntitesAvecCardinalites(source, cible, typeLien, cardSource, cardCible);
        });

// HBox pour source + cardinalité
        HBox hboxSource = new HBox(5, cbEntiteSource, cbCardinaliteSource);
        hboxSource.setAlignment(Pos.CENTER_LEFT);

// HBox pour cible + cardinalité
        HBox hboxCible = new HBox(5, cbEntiteCible, cbCardinaliteCible);
        hboxCible.setAlignment(Pos.CENTER_LEFT);

// VBox lien
        VBox vboxLien = new VBox(10, lblLien, hboxSource, hboxCible, btnCreerLien);
        vboxLien.setPadding(new Insets(10, 0, 0, 0));
        vboxLien.setAlignment(Pos.TOP_CENTER);

        this.getChildren().add(vboxLien);

        zoneModelisation.setSelectionListener(this::updateToolbarForEntity);
        updateVisibility();
        updateListeEntites();
    }

    private HBox createUnitItem(String type, VBox parentContainer, VBox parentMenuContainer) {
        Label icon = new Label("\u2022");
        icon.setStyle("-fx-text-fill: #0078D7; -fx-font-weight: bold;");

        TextField editableLabel = new TextField(type);
        editableLabel.setPrefWidth(100);

        editableLabel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                editableLabel.setEditable(true);
                editableLabel.requestFocus();
            }
        });

        editableLabel.setOnAction(e -> {
            String nomSaisi = editableLabel.getText().trim();
            switch (type) {
                case "Entité":
                    creerEntite(nomSaisi);
                    break;
                case "Attribut":
                    ajouterAttribut(nomSaisi);
                    break;
                case "Relation":
                    creerRelation(nomSaisi);
                    break;
                case "Clé primaire":
                    ajouterClePrimaire(nomSaisi);
                    break;
                case "Clé étrangère":
                    ajouterCleEtrangere(nomSaisi);
                    break;
                case "Héritage":
                    creerHeritage(nomSaisi);
                    break;
            }
            editableLabel.setEditable(false);
        });

        Button deleteButton = new Button("X");
        deleteButton.setStyle("-fx-text-fill: red;");
        deleteButton.setOnAction(e -> parentContainer.getChildren().remove(deleteButton.getParent()));

        HBox hbox = new HBox(5, icon, editableLabel, deleteButton);
        hbox.setPadding(new Insets(2));
        return hbox;
    }

    private void creerEntite(String nom) {
        if (entitesCreees.containsKey(nom)) {
            logDAO.insertLog(userId, "L'entité '" + nom + "' existe déjà.", "INFO");
            return;
        }

        Map<String, Object> entite = new HashMap<>();
        entite.put("nom", nom);
        entite.put("position_x", 100.0);
        entite.put("position_y", 100.0);
        entite.put("attributs", new ArrayList<Map<String, Object>>());

        zoneModelisation.ajouterEntite(entite);
        entitesCreees.put(nom, entite);
        entiteCourante.put("entite", entite);

        dbManager.insertEntity(entite, (List<Map<String, Object>>) entite.get("attributs"), "UML");
        updateListeEntites();
    }

    private void ajouterAttribut(String nomAttribut) {
        if (entiteCourante.containsKey("entite")) {
            Map<String, Object> entite = (Map<String, Object>) entiteCourante.get("entite");
            List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");

            boolean attributExiste = attributs.stream()
                    .anyMatch(attr -> attr.get("nom").equals(nomAttribut));

            if (!attributExiste) {
                Map<String, Object> attribut = new HashMap<>();
                attribut.put("nom", nomAttribut);
                attribut.put("cle_primaire", false);
                attribut.put("cle_etrangere", false);
                attribut.put("type", "");

                attributs.add(attribut);
                zoneModelisation.mettreAJourEntite(entite);

                dbManager.insertEntity(entite, attributs, "UML");
            } else {
                logDAO.insertLog(userId, "L'attribut '" + nomAttribut + "' existe déjà pour cette entité.", "INFO");
            }
        } else {
            logDAO.insertLog(userId, "Aucune entité sélectionnée. Veuillez d'abord créer une entité.", "INFO");
        }
    }

    private void ajouterClePrimaire(String nomClePrimaire) {
        if (entiteCourante.containsKey("entite")) {
            Map<String, Object> entite = (Map<String, Object>) entiteCourante.get("entite");
            List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");

            Map<String, Object> attributExistant = attributs.stream()
                    .filter(attr -> attr.get("nom").equals(nomClePrimaire))
                    .findFirst()
                    .orElse(null);

            if (attributExistant != null) {
                attributExistant.put("cle_primaire", true);
            } else {
                Map<String, Object> attribut = new HashMap<>();
                attribut.put("nom", nomClePrimaire);
                attribut.put("cle_primaire", true);
                attribut.put("cle_etrangere", false);
                attribut.put("type", "");
                attributs.add(attribut);
            }

            zoneModelisation.mettreAJourEntite(entite);
        } else {
            logDAO.insertLog(userId, "Aucune entité sélectionnée.", "INFO");
        }
    }

    private void ajouterCleEtrangere(String nomCleEtrangere) {
        if (entiteCourante.containsKey("entite")) {
            Map<String, Object> entite = (Map<String, Object>) entiteCourante.get("entite");
            List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");

            Map<String, Object> attributExistant = attributs.stream()
                    .filter(attr -> attr.get("nom").equals(nomCleEtrangere))
                    .findFirst()
                    .orElse(null);

            if (attributExistant != null) {
                attributExistant.put("cle_etrangere", true);
            } else {
                Map<String, Object> attribut = new HashMap<>();
                attribut.put("nom", nomCleEtrangere);
                attribut.put("cle_primaire", false);
                attribut.put("cle_etrangere", true);
                attribut.put("type", "");
                attributs.add(attribut);
            }

            zoneModelisation.mettreAJourEntite(entite);
        } else {
            logDAO.insertLog(userId, "Aucune entité sélectionnée.", "INFO");
        }
    }

    private void creerRelation(String nomEntiteMere) {
        /* ... */
    }

    private void creerHeritage(String nomEntiteMere) {
        /* ... */
    }

    private void updateToolbarForEntity(Map<String, Object> entite) {
        entiteCourante.put("entite", entite);
    }

    private void purgerBarreOutils() {
        for (javafx.scene.Node node : unitsList.getChildren()) {
            if (node instanceof VBox) {
                VBox container = (VBox) node;
                for (javafx.scene.Node subNode : container.getChildren()) {
                    if (subNode instanceof VBox && subNode.getId() == null) {
                        ((VBox) subNode).getChildren().clear();
                    }
                }
            }
        }
        entitesCreees.clear();
        entiteCourante.clear();
        updateListeEntites();
    }

    private void setupVisibilityListeners() {
        if (interfaceRef.getBtnUML() != null) {
            interfaceRef.getBtnUML().selectedProperty().addListener((obs, oldVal, newVal) -> updateVisibility());
        }
        if (interfaceRef.getBtnERD() != null) {
            interfaceRef.getBtnERD().selectedProperty().addListener((obs, oldVal, newVal) -> updateVisibility());
        }
    }

    private void updateVisibility() {
        if (relationContainer != null && heritageContainer != null && interfaceRef != null) {
            boolean isERDSelected = interfaceRef.getBtnERD() != null && interfaceRef.getBtnERD().isSelected();
            relationContainer.setVisible(isERDSelected);
            relationContainer.setManaged(isERDSelected);

            boolean isUMLSelected = interfaceRef.getBtnUML() != null && interfaceRef.getBtnUML().isSelected();
            heritageContainer.setVisible(isUMLSelected);
            heritageContainer.setManaged(isUMLSelected);
        }
    }

    private void updateListeEntites() {
        List<String> noms = zoneModelisation.getAllEntities().stream()
                .map(ent -> (String) ent.get("nom"))
                .toList();

        cbEntiteSource.setItems(FXCollections.observableArrayList(noms));
        cbEntiteCible.setItems(FXCollections.observableArrayList(noms));
    }

    private boolean relationExiste(Map<String, Object> source, Map<String, Object> cible, String typeLien) {
        return zoneModelisation.relationExiste(source, cible, typeLien);
    }

    private void showAlert(String titre, String message) {
        Alert alert = new Alert(AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void actualiserComboBoxEntites() {
        cbEntiteSource.getItems().clear();
        cbEntiteCible.getItems().clear();

        if (zoneModelisation == null) {
            return;
        }

        for (Map<String, Object> entite : zoneModelisation.getAllEntities()) {
            // Filtrage par type UML/ERD
            if ((zoneModelisation.isUML() && "UML".equals(entite.get("type_schema")))
                    || (!zoneModelisation.isUML() && "ERD".equals(entite.get("type_schema")))) {
                String nom = (String) entite.get("nom");
                cbEntiteSource.getItems().add(nom);
                cbEntiteCible.getItems().add(nom);
            }
        }

        if (!cbEntiteSource.getItems().isEmpty()) {
            cbEntiteSource.getSelectionModel().selectFirst();
        }
        if (!cbEntiteCible.getItems().isEmpty()) {
            cbEntiteCible.getSelectionModel().selectFirst();
        }
    }

}
