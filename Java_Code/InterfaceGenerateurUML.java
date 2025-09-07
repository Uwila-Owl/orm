
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InterfaceGenerateurUML extends Application {

    private int currentSchemaId = -1; // -1 pour un nouveau schéma
    private String currentSchemaName = "Nouveau Schéma";
    private final Map<TextField, String> labelStore = new HashMap<>();
    private ZoneModelisation ZoneModelisation;

    private ToggleButton btnUML;
    private ToggleButton btnERD;
    private Stage primaryStage;
    private BarreOutils leftBar;

    public InterfaceGenerateurUML(int schemaId, String schemaName) {
        this.currentSchemaId = schemaId;
        this.currentSchemaName = schemaName;
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        MenuBar menuBar = NavigationMenu.createMenuBar(primaryStage);
        menuBar.setStyle("-fx-background-color: #3E5871;");

        ToggleGroup group = new ToggleGroup();
        btnUML = new ToggleButton("UML");
        btnERD = new ToggleButton("ERD");
        btnUML.setToggleGroup(group);
        btnERD.setToggleGroup(group);
        btnUML.setSelected(true);

        HBox onglets = new HBox(10, btnUML, btnERD);
        onglets.setPadding(new Insets(8));
        onglets.setAlignment(Pos.CENTER);
        onglets.setStyle("-fx-background-color: #D6E3F3;");

        VBox top = new VBox(menuBar, onglets);

        ZoneModelisation zoneModelisation = new ZoneModelisation(currentSchemaId);
        this.ZoneModelisation = zoneModelisation;
        zoneModelisation.setStyle("-fx-padding: 50; -fx-border-color: gray;");
        StackPane centerPane = new StackPane(zoneModelisation);
        centerPane.setStyle("-fx-background-color: white;");
        centerPane.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE); // occuper toute la place

        leftBar = (BarreOutils) createBarreOutils();
        leftBar.setMinWidth(250);
        leftBar.setPrefWidth(290);

        PanneauProprietes rightPanel = new PanneauProprietes(zoneModelisation);
        rightPanel.setMinWidth(250);
        rightPanel.setPrefWidth(290);

        zoneModelisation.setSelectionListener(entite -> rightPanel.remplirPanneau(entite));

        group.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && oldValue != null) {
                boolean oldIsUML = (oldValue == btnUML);
                boolean newIsUML = (newValue == btnUML);

                if (oldIsUML != newIsUML) {
                    promptWarnAndSwitch(oldIsUML, newIsUML);
                }
            }
        });

        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(5));
        statusBar.setStyle("-fx-background-color: #EAECEE;");
        Label lblProjet = new Label("Projet : MyProject.uml");
        Label lblZoom = new Label("Zoom : 100%");
        Label lblStatut = new Label("Statut : OK");
        statusBar.getChildren().addAll(lblProjet, lblZoom, lblStatut);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setLeft(leftBar);
        root.setCenter(centerPane);
        root.setRight(rightPanel);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 700);
        primaryStage.setTitle("Générateur UML/ERD/Code");
        primaryStage.setScene(scene);

        // 🔹 ouverture en plein écran
        primaryStage.setMaximized(true);
        primaryStage.setFullScreenExitHint("");

        primaryStage.show();
    }

    private VBox createBarreOutils() {
        return new BarreOutils(ZoneModelisation, this);
    }

    public ToggleButton getBtnUML() {
        return btnUML;
    }

    public ToggleButton getBtnERD() {
        return btnERD;
    }

    private void promptWarnAndSwitch(boolean wasUML, boolean willBeUML) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Changer de type de schéma");
        alert.setHeaderText("Vous êtes sur le point de changer de type de schéma.");
        alert.setContentText("Attention : Le schéma actuel ne sera pas sauvegardé.\n"
                + "Si vous continuez, vous perdrez toutes les données non sauvegardées.\n"
                + "Voulez-vous continuer ?");

        ButtonType buttonTypeContinue = new ButtonType("Continuer");
        ButtonType buttonTypeCancel = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeContinue, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == buttonTypeContinue) {
                // 🔹 Changement du type de schéma
                ZoneModelisation.setTypeSchema(willBeUML);

                // 🔹 Actualiser la barre d’outils
                BarreOutils barreOutils = (BarreOutils) leftBar; // leftBar est bien la BarreOutils
                barreOutils.actualiserComboBoxEntites();

            } else {
                if (wasUML) {
                    btnUML.setSelected(true);
                } else {
                    btnERD.setSelected(true);
                }
            }
        } else {
            if (wasUML) {
                btnUML.setSelected(true);
            } else {
                btnERD.setSelected(true);
            }
        }

    }
}
