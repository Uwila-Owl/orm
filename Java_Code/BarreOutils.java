import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BarreOutils extends VBox {

    private ZoneModelisation zoneModelisation;
    private Map<String, Map<String, Object>> entitesCreees = new HashMap<>();
    private Map<String, Map<String, Object>> entiteCourante = new HashMap<>();
    private VBox unitsList;
    private InsertionDonnees dbManager; // Ajout d'InsertionDonnees - Léa
    private InterfaceGenerateurUML interfaceRef;
    private VBox relationContainer;
    private VBox heritageContainer;

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

        Label title = new Label("Unité");
        title.setFont(Font.font("Arial", 14));

        unitsList = new VBox(5);
        String[] unitLabels = { "Entité", "Attribut", "Relation", "Clé primaire", "Clé étrangère", "Héritage" };
        for (String label : unitLabels) {
            VBox container = new VBox(3);
            MenuButton menuButton = new MenuButton(label);
            menuButton.setPrefWidth(150);
            VBox addedItems = new VBox(3);

            MenuItem addItem = new MenuItem("+ Ajouter " + label);
            addItem.setOnAction(e -> {
                HBox nouvelElement = createUnitItem(label, addedItems, container);
                addedItems.getChildren().add(nouvelElement);
            });

            menuButton.getItems().add(addItem);
            container.getChildren().addAll(menuButton, addedItems);

            if (label.equals("Relation")) {
                relationContainer = container;
            } else if (label.equals("Héritage")) {
                heritageContainer = container;
            }

            unitsList.getChildren().add(container);
        }

        Button purgerButton = new Button("Purger");
        purgerButton.setPrefWidth(150);
        purgerButton.setOnAction(e -> purgerBarreOutils());

        this.getChildren().addAll(title, unitsList, purgerButton);
        zoneModelisation.setSelectionListener(this::updateToolbarForEntity);
        updateVisibility();
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
        // Vérifiez si l'entité existe déjà
        if (entitesCreees.containsKey(nom)) {
            System.out.println("L'entité '" + nom + "' existe déjà.");
         return; // Ne pas créer une nouvelle entité
        }   

        Map<String, Object> entite = new HashMap<>();
        entite.put("nom", nom);
        entite.put("position_x", 100.0);
        entite.put("position_y", 100.0);
        entite.put("attributs", new ArrayList<Map<String, Object>>());

        zoneModelisation.ajouterEntite(entite);
        entitesCreees.put(nom, entite);
        entiteCourante.put("entite", entite);

        // Insérer l'entité dans la base de données
        dbManager.insertEntity(entite, (List<Map<String, Object>>) entite.get("attributs"), "UML");
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

                // Insérer l'attribut dans la base de données
                dbManager.insertEntity(entite, attributs, "UML");
            } else {
                System.out.println("L'attribut '" + nomAttribut + "' existe déjà pour cette entité.");
            }
        } else {
            System.out.println("Aucune entité sélectionnée. Veuillez d'abord créer une entité.");
        }
    }

    private void ajouterClePrimaire(String nomClePrimaire) {
        if (entiteCourante.containsKey("entite")) {
            Map<String, Object> entite = (Map<String, Object>) entiteCourante.get("entite");
            List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");

            // Chercher si l'attribut existe déjà
            Map<String, Object> attributExistant = attributs.stream()
                    .filter(attr -> attr.get("nom").equals(nomClePrimaire))
                    .findFirst()
                    .orElse(null);

            if (attributExistant != null) {
                // Modifier l'attribut existant
                attributExistant.put("cle_primaire", true);
            } else {
                // Créer un nouvel attribut avec le statut de clé primaire
                Map<String, Object> attribut = new HashMap<>();
                attribut.put("nom", nomClePrimaire);
                attribut.put("cle_primaire", true);
                attribut.put("cle_etrangere", false);
                attribut.put("type", "");
                attributs.add(attribut);
            }

            // Mettre à jour l'affichage
            zoneModelisation.mettreAJourEntite(entite);
        } else {
            System.out.println("Aucune entité sélectionnée.");
        }
    }

    private void ajouterCleEtrangere(String nomCleEtrangere) {
        if (entiteCourante.containsKey("entite")) {
            Map<String, Object> entite = (Map<String, Object>) entiteCourante.get("entite");
            List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");

            // Chercher si l'attribut existe déjà
            Map<String, Object> attributExistant = attributs.stream()
                    .filter(attr -> attr.get("nom").equals(nomCleEtrangere))
                    .findFirst()
                    .orElse(null);

            if (attributExistant != null) {
                // Modifier l'attribut existant
                attributExistant.put("cle_etrangere", true);
            } else {
                // Créer un nouvel attribut avec le statut de clé étrangère
                Map<String, Object> attribut = new HashMap<>();
                attribut.put("nom", nomCleEtrangere);
                attribut.put("cle_primaire", false);
                attribut.put("cle_etrangere", true);
                attribut.put("type", "");
                attributs.add(attribut);
            }

            // Mettre à jour l'affichage
            zoneModelisation.mettreAJourEntite(entite);
        } else {
            System.out.println("Aucune entité sélectionnée.");
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
}
