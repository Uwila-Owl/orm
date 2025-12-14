import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InterfaceGenerateurUML extends Application {

    private int currentSchemaId = -1;
    private String currentSchemaName = "Nouveau Schéma";
    private final Map<TextField, String> labelStore = new HashMap<>();
    private ZoneModelisation ZoneModelisation;
    private LogDAO logDAO = new LogDAO();
    String userId = UserSession.getInstance().getUserId();

    private ToggleButton btnUML;
    private ToggleButton btnERD;
    private Stage primaryStage;
    private BarreOutils leftBar;

    public InterfaceGenerateurUML() {
    }

    public InterfaceGenerateurUML(int schemaId, String schemaName) {
        this.currentSchemaId = schemaId;
        this.currentSchemaName = schemaName;
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        //CRÉATION DE LA ZONEMODELISATION AVANT LA BARRE DE MENU
        ZoneModelisation zoneModelisation = new ZoneModelisation(currentSchemaId);
        zoneModelisation.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        this.ZoneModelisation = zoneModelisation;
        zoneModelisation.setStyle("-fx-padding: 50; -fx-border-color: gray;");

        // CORRECTION DE L'APPEL À createMenuBar AVEC LE BON NOMBRE DE PARAMÈTRES
        MenuBar menuBar = NavigationMenu.createMenuBar(primaryStage, zoneModelisation);

        // Créer la zone utilisateur avec les vraies informations
        Label userLabel = createUserLabel();

        // Créer un HBox pour la barre de menu avec l'utilisateur
        HBox menuContainer = new HBox();
        menuContainer.setAlignment(Pos.CENTER_LEFT);
        
        // MODIFICATION: Appliquer le style bleu au conteneur aussi pour cohérence
        menuContainer.setStyle("-fx-background-color: #149FB6;");

        // Espacer pour pousser l'utilisateur à droite
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        menuContainer.getChildren().addAll(menuBar, spacer, userLabel);

        // Créer les onglets UML/ERD
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

        VBox top = new VBox(menuContainer, onglets);

        // DÉPLACEMENT DE LA CRÉATION DE ZoneModelisation PLUS HAUT (DÉJÀ FAIT)

        // Encapsuler la ZoneModelisation dans un ScrollPane
        ScrollPane scrollPane = new ScrollPane(zoneModelisation);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        StackPane centerPane = new StackPane(scrollPane);
        centerPane.setStyle("-fx-background-color: white;");
        centerPane.setPrefSize(800, 600);

        leftBar = (BarreOutils) createBarreOutils();
        leftBar.setMinWidth(250);
        leftBar.setPrefWidth(290);

        PanneauProprietes rightPanel = new PanneauProprietes(zoneModelisation, this);
        rightPanel.setMinWidth(250);
        rightPanel.setPrefWidth(290);

        zoneModelisation.setSelectionListener(entite -> rightPanel.remplirPanneau(entite));

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
        primaryStage.setMaximized(true);
        primaryStage.setFullScreenExitHint("");

        //CONFIGURATION DES ÉVÉNEMENTS POUR LES BOUTONS UML/ERD
        setupUMLERDToggles();

        // Configuration de la vérification de session
        setupSessionCheck();

        // AJOUT DU SUIVI DES MODIFICATIONS
        setupModificationTracking();

        primaryStage.show();
    }

    //MÉTHODE POUR CONFIGURER LES BOUTONS UML/ERD 
    private void setupUMLERDToggles() {
        btnUML.setOnAction(e -> {
            if (!ZoneModelisation.isUML()) {
                promptWarnAndSwitch(false, true);
            }
        });

        btnERD.setOnAction(e -> {
            if (ZoneModelisation.isUML()) {
                promptWarnAndSwitch(true, false);
            }
        });
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

    // ERIC: MÉTHODE POUR CRÉER LE LABEL UTILISATEUR AVEC VRAIES INFORMATIONS - DÉBUT
    private Label createUserLabel() {
        UserSession session = UserSession.getInstance();
        String displayName = session.getFullName();
        String role = session.isSuperviseur() ? " (Admin)" : "";

        Label userLabel = new Label("👤 " + displayName + role);
        userLabel.setStyle(
                "-fx-text-fill: white; "
                + "-fx-font-size: 14px; "
                + "-fx-font-weight: bold; "
                + "-fx-padding: 8 15 8 15; "
                + "-fx-background-color: rgba(255,255,255,0.15); "
                + "-fx-background-radius: 15; "
                + "-fx-cursor: hand;"
        );

        // Effet de survol
        userLabel.setOnMouseEntered(e -> {
            userLabel.setStyle(
                    "-fx-text-fill: white; "
                    + "-fx-font-size: 14px; "
                    + "-fx-font-weight: bold; "
                    + "-fx-padding: 8 15 8 15; "
                    + "-fx-background-color: rgba(255,255,255,0.25); "
                    + "-fx-background-radius: 15; "
                    + "-fx-cursor: hand;"
            );
        });

        userLabel.setOnMouseExited(e -> {
            userLabel.setStyle(
                    "-fx-text-fill: white; "
                    + "-fx-font-size: 14px; "
                    + "-fx-font-weight: bold; "
                    + "-fx-padding: 8 15 8 15; "
                    + "-fx-background-color: rgba(255,255,255,0.15); "
                    + "-fx-background-radius: 15; "
                    + "-fx-cursor: hand;"
            );
        });

        // Menu contextuel au clic
        userLabel.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                showUserMenu(userLabel);
            }
        });

        return userLabel;
    }
    // ERIC: MÉTHODE POUR CRÉER LE LABEL UTILISATEUR AVEC VRAIES INFORMATIONS - FIN

    // Configuration de la vérification de session
    private void setupSessionCheck() {
        Timeline sessionChecker = new Timeline(
                new KeyFrame(Duration.minutes(5), e -> checkSession())
        );
        sessionChecker.setCycleCount(Timeline.INDEFINITE);
        sessionChecker.play();
    }

    private void checkSession() {
        UserSession session = UserSession.getInstance();
        if (session.getUserId() == null || session.isSessionExpired()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Session expirée");
            alert.setHeaderText("Votre session a expiré");
            alert.setContentText("Pour des raisons de sécurité, vous devez vous reconnecter.");
            alert.showAndWait();

            session.clear();
            primaryStage.close();
        }
    }

    // ERIC: MENU CONTEXTUEL UTILISATEUR AVEC VRAIES INFORMATIONS - DÉBUT
    private void showUserMenu(Label userLabel) {
        ContextMenu userMenu = new ContextMenu();
        UserSession session = UserSession.getInstance();

        MenuItem profileItem = new MenuItem("👤 Profil");
        MenuItem sessionInfoItem = new MenuItem("⏰ Info Session");
        MenuItem settingsItem = new MenuItem("⚙️ Paramètres");
        MenuItem logoutItem = new MenuItem("🚪 Déconnexion");

        // Action profil avec vraies informations
        profileItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Profil utilisateur");
            alert.setHeaderText("Informations utilisateur");

            StringBuilder info = new StringBuilder();
            info.append("ID: ").append(session.getUserId()).append("\n");
            info.append("Nom: ").append(session.getNom() != null ? session.getNom() : "N/A").append("\n");
            info.append("Prénom: ").append(session.getPrenom() != null ? session.getPrenom() : "N/A").append("\n");
            info.append("Email: ").append(session.getEmail() != null ? session.getEmail() : "N/A").append("\n");
            info.append("Statut: ").append(session.isSuperviseur() ? "Superviseur" : "Utilisateur");

            alert.setContentText(info.toString());
            alert.showAndWait();
        });

        // Informations de session
        sessionInfoItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Informations de session");
            alert.setHeaderText("Détails de votre session");

            long remainingMinutes = session.getRemainingSessionMinutes();
            String timeLeft = String.format("%d heures %d minutes",
                    remainingMinutes / 60, remainingMinutes % 60);

            alert.setContentText("Temps de session restant: " + timeLeft);
            alert.showAndWait();
        });

        settingsItem.setOnAction(e -> {
            logDAO.insertLog(userId, "Ouvrir paramètres pour: " + session.getUserId(), "INFO");
        });

        logoutItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Déconnexion");
            confirm.setHeaderText("Confirmer la déconnexion");
            confirm.setContentText("Voulez-vous vraiment vous déconnecter, " + session.getPrenom() + " ?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                session.clear();
                primaryStage.close();
            }
        });

        userMenu.getItems().addAll(profileItem, sessionInfoItem, settingsItem,
                new SeparatorMenuItem(), logoutItem);

        userMenu.show(userLabel,
                userLabel.localToScreen(userLabel.getBoundsInLocal()).getMinX(),
                userLabel.localToScreen(userLabel.getBoundsInLocal()).getMaxY());
    }
    // ERIC: MENU CONTEXTUEL UTILISATEUR AVEC VRAIES INFORMATIONS - FIN
    
    // ERIC: MÉTHODE POUR GÉRER LE CHANGEMENT DE TYPE DE SCHÉMA - DÉBUT
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
                ZoneModelisation.setTypeSchema(willBeUML);
                BarreOutils barreOutils = (BarreOutils) leftBar;
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
    // ERIC: MÉTHODE POUR GÉRER LE CHANGEMENT DE TYPE DE SCHÉMA - FIN

    //AJOUT DU SUIVI DES MODIFICATIONS POUR UNDO/REDO
    private void setupModificationTracking() {
        if (ZoneModelisation != null) {
            ZoneModelisation.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
                NavigationMenu.markModified();
                updateWindowTitle(true);
                
                // ERIC: SAUVEGARDE AUTOMATIQUE DANS L'HISTORIQUE PENDANT LE DRAG
                if (e.isPrimaryButtonDown()) {
                    ZoneModelisation.saveToHistory();
                }
            });
        }
    }
    
    private void updateWindowTitle(boolean modified) {
        String title = "Générateur UML/ERD/Code";
        if (modified) {
            title += " *";
        }
        if (primaryStage != null) {
            primaryStage.setTitle(title);
        }
    }   
}