import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.*;

public class InterfaceGenerateurUML extends Application {

    private final Map<TextField, String> labelStore = new HashMap<>();
    private ZoneModelisation ZoneModelisation;

    // Ajout des champs de classe pour btnUML et btnERD
    private ToggleButton btnUML;
    private ToggleButton btnERD;

    @Override
    public void start(Stage primaryStage) {
        // ---- Barre de menus via NavigationMenu ----
        MenuBar menuBar = NavigationMenu.createMenuBar(primaryStage);
        menuBar.setStyle("-fx-background-color: #3E5871;");

        // ---- Onglets UML / ERD ----
        ToggleGroup group = new ToggleGroup();
        btnUML = new ToggleButton("UML"); // Maintenant c'est un champ de classe
        btnERD = new ToggleButton("ERD"); // Maintenant c'est un champ de classe
        btnUML.setToggleGroup(group);
        btnERD.setToggleGroup(group);
        btnUML.setSelected(true);

        HBox onglets = new HBox(10, btnUML, btnERD);
        onglets.setPadding(new Insets(8));
        onglets.setAlignment(Pos.CENTER);
        onglets.setStyle("-fx-background-color: #D6E3F3;");

        VBox top = new VBox(menuBar, onglets);

        // ---- Zone de modélisation ----
        ZoneModelisation zoneModelisation = new ZoneModelisation();
        this.ZoneModelisation = zoneModelisation;
        zoneModelisation.setStyle("-fx-padding: 50; -fx-border-color: gray;");
        StackPane centerPane = new StackPane(zoneModelisation);
        centerPane.setStyle("-fx-background-color: white;");

        // ---- Barre latérale gauche ---- (maintenant avec this en paramètre)
        VBox leftBar = createBarreOutils();

        // ---- Panneau de propriétés ----
        PanneauProprietes rightPanel = new PanneauProprietes(zoneModelisation);

        // ---- Listener pour la sélection d'entité ----
        zoneModelisation.setSelectionListener(new ZoneModelisation.SelectionListener() {
            @Override
            public void onSelection(Map<String, Object> entite) {
                rightPanel.remplirPanneau(entite);
            }
        });

        // ---- Gestion des onglets UML/ERD ----
        group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == btnUML) {
                zoneModelisation.setTypeSchema(true);
            } else if (newValue == btnERD) {
                zoneModelisation.setTypeSchema(false);
            }
        });

        // ---- Barre de statut ----
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #EAECEE;");
        Label lblProjet = new Label("Projet : MyProject.uml");
        Label lblZoom = new Label("Zoom : 100%");
        Label lblStatut = new Label("Statut : OK");
        statusBar.getChildren().addAll(lblProjet, lblZoom, lblStatut);

        // ---- Layout principal ----
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setLeft(leftBar);
        root.setCenter(centerPane);
        root.setRight(rightPanel);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 700);
        primaryStage.setTitle("Générateur UML/ERD/Code");
        primaryStage.setScene(scene);

        primaryStage.show();
    }

    // ---- Barre d'outils ---- (maintenant passe this en paramètre)
    private VBox createBarreOutils() {
        return new BarreOutils(ZoneModelisation, this); // Passe this pour accéder aux boutons
    }

    // Getters pour accéder aux boutons depuis BarreOutils
    public ToggleButton getBtnUML() {
        return btnUML;
    }

    public ToggleButton getBtnERD() {
        return btnERD;
    }

    private HBox createUnitItem(String label, VBox parentContainer) {
        Label icon = new Label("\u2022");
        icon.setStyle("-fx-text-fill: #0078D7; -fx-font-weight: bold;");

        TextField editableLabel = new TextField(label);
        editableLabel.setPrefWidth(100);
        labelStore.put(editableLabel, label);

        editableLabel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                editableLabel.setEditable(true);
                editableLabel.requestFocus();
            }
        });

        editableLabel.setOnAction(e -> {
            labelStore.put(editableLabel, editableLabel.getText());
            editableLabel.setEditable(false);
        });

        editableLabel.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                labelStore.put(editableLabel, editableLabel.getText());
                editableLabel.setEditable(false);
            }
        });

        Button deleteButton = new Button("X");
        deleteButton.setStyle("-fx-text-fill: red;");
        deleteButton.setOnAction(e -> {
            labelStore.remove(editableLabel);
            parentContainer.getChildren().remove(deleteButton.getParent());
        });

        HBox hbox = new HBox(5, icon, editableLabel, deleteButton);
        hbox.setPadding(new Insets(2));
        return hbox;
    }
}