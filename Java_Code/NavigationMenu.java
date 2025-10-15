import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.scene.Scene;
import java.io.File;
import java.util.Optional;
import java.util.Map;
import javafx.scene.Group;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import com.uml.generator.export.DiagramExporter;
import com.uml.generator.export.DiagramImporter;

public class NavigationMenu {

    private static LogDAO logDAO = new LogDAO();
    
    private static boolean modificationsNonSauvegardees = false;
    private static Stage fullScreenStage; // Pour le mode étendu
    
    public static MenuBar createMenuBar(Stage primaryStage, ZoneModelisation zoneModelisation) {
        MenuBar menuBar = new MenuBar();
        
        menuBar.setStyle("-fx-background-color: #149FB6;");

        String userId = UserSession.getInstance().getUserId();

        // ---- Fichier ----
        Menu fileMenu = new Menu("Fichier");
        fileMenu.setStyle("-fx-text-fill: white;");
        MenuItem newItem = new MenuItem("Nouveau");
        MenuItem saveItem = new MenuItem("Enregistrer");
        MenuItem importItem = new MenuItem("Importer");
        MenuItem exitItem = new MenuItem("Quitter");
        fileMenu.getItems().addAll(newItem, saveItem, importItem, new SeparatorMenuItem(), exitItem);
        
        // ---- Édition ----
        Menu editMenu = new Menu("Édition");
        editMenu.setStyle("-fx-text-fill: white;");
        
        // MODIFICATION - GARDER SEULEMENT "RETOUR" ET "AVANCER"
        MenuItem undoItem = new MenuItem("Retour");
        MenuItem redoItem = new MenuItem("Avancer");
        
        //AJOUT DES RACCOURCIS CLAVIER
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));
        
        editMenu.getItems().addAll(undoItem, redoItem);

        // ---- Vue ----
        Menu viewMenu = new Menu("Vue");
        viewMenu.setStyle("-fx-text-fill: white;");
        MenuItem expandItem = new MenuItem("Etendre");
        MenuItem zoomItem = new MenuItem("Zoom");
        viewMenu.getItems().addAll(expandItem, zoomItem);

        // ---- Aide ----
        Menu helpMenu = new Menu("Aide");
        helpMenu.setStyle("-fx-text-fill: white;");
        MenuItem aboutItem = new MenuItem("À propos");
        MenuItem docsItem = new MenuItem("Manuel d'utilisation");
        MenuItem bugItem = new MenuItem("Soumettre un bug");
        MenuItem discordItem = new MenuItem("Chatbot Discord");
        helpMenu.getItems().addAll(aboutItem, docsItem, bugItem, discordItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, helpMenu);

        // ---- Actions de menu ----

        // Nouveau : Créer un nouveau schéma avec confirmation
        newItem.setOnAction(e -> {
            if (modificationsNonSauvegardees) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Nouveau schéma");
                alert.setHeaderText("Modifications non sauvegardées");
                alert.setContentText("Voulez-vous vraiment créer un nouveau schéma ? Les modifications non sauvegardées seront perdues.");
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return;
                }
            }
            
            if (zoneModelisation != null) {
                // REMPLACEMENT DE clearInstance() PAR clear()
                zoneModelisation.clear();
                modificationsNonSauvegardees = false;
                logDAO.insertLog(userId, "Nouveau schéma créé", "INFO");
            }
        });

        // Enregistrer : Sauvegarder en XML ou PNG
        saveItem.setOnAction(e -> {
            if (zoneModelisation == null || zoneModelisation.getAllEntities().isEmpty()) {
                showErrorAlert("Sauvegarde impossible", "Aucun diagramme à sauvegarder");
                return;
            }
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le diagramme");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Fichiers XML", "*.xml"),
                    new FileChooser.ExtensionFilter("Fichiers PNG", "*.png"));
            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            
            if (selectedFile != null) {
                try {
                    String fileName = selectedFile.getName().toLowerCase();
                    if (fileName.endsWith(".xml") || !fileName.contains(".")) {
                        if (!fileName.endsWith(".xml")) {
                            selectedFile = new File(selectedFile.getParent(), selectedFile.getName() + ".xml");
                        }
                        //CORRECTION DE L'APPEL À DiagramExporter.exportToXML
                        DiagramExporter.exportToXML(selectedFile, zoneModelisation.getDiagramData());
                        modificationsNonSauvegardees = false;
                    } else if (fileName.endsWith(".png")) {
                        //CORRECTION DE L'APPEL À DiagramExporter.exportToPNG
                        DiagramExporter.exportToPNG(selectedFile, zoneModelisation);
                    }
                    logDAO.insertLog(userId, "Diagramme enregistré : " + selectedFile.getAbsolutePath(), "INFO");
                } catch (Exception ex) {
                    showErrorAlert("Erreur lors de la sauvegarde", "Impossible de sauvegarder: " + ex.getMessage());
                }
            }
        });

        // Importer : Garde l'alerte de mise en garde
        importItem.setOnAction(e -> {
            if (modificationsNonSauvegardees) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Importer un fichier");
                alert.setHeaderText("Modifications non sauvegardées");
                alert.setContentText("Voulez-vous vraiment ouvrir un nouveau fichier ? Les modifications non sauvegardées seront perdues.");
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return;
                }
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Importer un fichier XML");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Fichiers XML", "*.xml"));
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            
            if (selectedFile != null) {
                try {
                    //CORRECTION DE L'APPEL À DiagramImporter.importFromXML
                    Map<String, Object> diagramData = DiagramImporter.importFromXML(selectedFile);
                    zoneModelisation.loadDiagramData(diagramData);
                    modificationsNonSauvegardees = false;
                    logDAO.insertLog(userId, "Fichier importé : " + selectedFile.getAbsolutePath(), "INFO");
                } catch (Exception ex) {
                    showErrorAlert("Erreur lors de l'import", "Impossible d'importer le fichier: " + ex.getMessage());
                }
            }
        });

        // Quitter : Vérifier les modifications non sauvegardées
        exitItem.setOnAction(e -> {
            if (modificationsNonSauvegardees) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Quitter l'application");
                alert.setHeaderText("Modifications non sauvegardées");
                alert.setContentText("Voulez-vous vraiment quitter ? Les modifications non sauvegardées seront perdues.");
                
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isEmpty() || result.get() != ButtonType.OK) {
                    return;
                }
            }
            
            // Fermer la fenêtre étendue si elle est ouverte
            if (fullScreenStage != null) {
                fullScreenStage.close();
            }
            primaryStage.close();
            logDAO.insertLog(userId, "Application fermée", "INFO");
        });

        // ACTIONS POUR UNDO/REDO
        undoItem.setOnAction(e -> {
            if (zoneModelisation != null && zoneModelisation.canUndo()) {
                zoneModelisation.undo();
                modificationsNonSauvegardees = true;
                markModified();
            }
        });

        redoItem.setOnAction(e -> {
            if (zoneModelisation != null && zoneModelisation.canRedo()) {
                zoneModelisation.redo();
                modificationsNonSauvegardees = true;
                markModified();
            }
        });

        // ---- Actions du menu Vue ----

        // Etendre : Mode plein écran pour la zone de modélisation uniquement
        expandItem.setOnAction(e -> {
            createFullScreenMode(primaryStage, zoneModelisation);
            logDAO.insertLog(userId, "Mode étendu activé pour la zone de modélisation", "INFO");
        });


        // Zoom : Définir le pourcentage manuellement avec changement visuel instantané
        zoomItem.setOnAction(e -> {
            showZoomDialog(primaryStage, zoneModelisation);
        });

        // ---- Actions du menu Aide ----
        
        aboutItem.setOnAction(e -> showAboutDialog(primaryStage));
        docsItem.setOnAction(e -> showManualDialog(primaryStage));
        bugItem.setOnAction(e -> showBugReportDialog(primaryStage));
        discordItem.setOnAction(e -> openDiscordChatbot(primaryStage));

        // AJOUT DES ÉCOUTEURS CLAVIER POUR UNDO/REDO 
        if (primaryStage.getScene() != null) {
            primaryStage.getScene().setOnKeyPressed(event -> {
                if (event.isControlDown()) {
                    if (event.getCode() == KeyCode.Z && zoneModelisation != null && zoneModelisation.canUndo()) {
                        zoneModelisation.undo();
                        modificationsNonSauvegardees = true;
                        markModified();
                        event.consume();
                    } else if (event.getCode() == KeyCode.Y && zoneModelisation != null && zoneModelisation.canRedo()) {
                        zoneModelisation.redo();
                        modificationsNonSauvegardees = true;
                        markModified();
                        event.consume();
                    }
                }
            });
        }

        return menuBar;
    }

    /**
     * Crée un mode plein écran pour la zone de modélisation uniquement
     */
    private static void createFullScreenMode(Stage primaryStage, ZoneModelisation zoneModelisation) {
        //CORRECTION - FERMER LA FENÊTRE ÉTENDUE EXISTANTE SI ELLE EST OUVERTE
        if (fullScreenStage != null && fullScreenStage.isShowing()) {
            fullScreenStage.close();
        }
        
        fullScreenStage = new Stage();
        fullScreenStage.setTitle("Zone de Modélisation - Mode Étendu");
        fullScreenStage.setFullScreen(true);
        fullScreenStage.setFullScreenExitHint("Appuyez sur ESC ou F11 pour quitter le mode étendu");

        // Créer une copie de la zone de modélisation pour le mode étendu
        ZoneModelisation fullScreenZone = new ZoneModelisation(zoneModelisation.getSchemaId());
        fullScreenZone.setTypeSchema(zoneModelisation.isUML());
        
        // Copier les données du diagramme actuel
        Map<String, Object> diagramData = zoneModelisation.getDiagramData();
        fullScreenZone.loadDiagramData(diagramData);

        ScrollPane scrollPane = new ScrollPane(fullScreenZone);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);

        StackPane root = new StackPane(scrollPane);
        root.setStyle("-fx-background-color: white;");

        Scene fullScreenScene = new Scene(root);
        fullScreenStage.setScene(fullScreenScene);

        //AMÉLIORATION - GESTION DES TOUCHES POUR LE MODE ÉTENDU
        fullScreenScene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ESCAPE:
                case F11:
                    fullScreenStage.close();
                    fullScreenStage = null;
                    break;
            }
        });

        // AJOUT - GESTION DE LA FERMETURE DE LA FENÊTRE ÉTENDUE
        fullScreenStage.setOnCloseRequest(event -> {
            fullScreenStage = null;
        });

        fullScreenStage.show();
    }

    /**
     * Affiche la boîte de dialogue pour définir le zoom manuellement avec changement instantané
     */
    private static void showZoomDialog(Stage owner, ZoneModelisation zoneModelisation) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Réglage du zoom");
        dialog.setHeaderText("Définir le niveau de zoom");
        dialog.initOwner(owner);

        // Création du sélecteur de pourcentage
        Slider zoomSlider = new Slider(10, 400, 100);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setMajorTickUnit(50);
        zoomSlider.setBlockIncrement(10);
        zoomSlider.setSnapToTicks(true);

        Label valueLabel = new Label("100%");
        
        // Mise à jour du label ET application du zoom en temps réel
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int percentValue = newVal.intValue();
            valueLabel.setText(String.format("%d%%", percentValue));
            
            // Appliquer le zoom instantanément
            applyZoomToZone(zoneModelisation, percentValue);
        });

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
            new Label("Niveau de zoom :"),
            zoomSlider,
            new HBox(5, valueLabel, new Label("(10% à 400%)")),
            new Label("Le zoom s'applique instantanément")
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // Appliquer le zoom initial
        applyZoomToZone(zoneModelisation, 100);

        dialog.showAndWait();
        
        // Journalisation après fermeture de la boîte de dialogue
        logDAO.insertLog(UserSession.getInstance().getUserId(), 
            "Zoom défini à " + (int)zoomSlider.getValue() + "%", "INFO");
    }

    /**
     * Applique le zoom à la zone de modélisation
     */
    private static void applyZoomToZone(ZoneModelisation zoneModelisation, int percent) {
        if (zoneModelisation != null) {
            double zoomFactor = percent / 100.0;
            Group contentGroup = zoneModelisation.getContentGroup();
            if (contentGroup != null) {
                contentGroup.setScaleX(zoomFactor);
                contentGroup.setScaleY(zoomFactor);
            }
        }
    }

    /**
     * Affiche la fenêtre "À propos"
     */
    private static void showAboutDialog(Stage owner) {
        Alert aboutAlert = new Alert(Alert.AlertType.INFORMATION);
        aboutAlert.setTitle("À propos");
        aboutAlert.setHeaderText("Générateur UML/ERD/Code");
        aboutAlert.setContentText("Version 1.0\n\n" +
                "Développé par l'équipe des étudiants de l'IED Paris :\n" +
                "• Léa MOULINNEUF - Développeuse fullstack \n" +
                "• Lyna BOUZEFRANE - Développeuse fullstack\n" +
                "• Eric NDIKUBWAYO- Développeur fullstack\n\n" +
                "© 2025 Tous droits réservés");
        
        aboutAlert.initOwner(owner);
        aboutAlert.showAndWait();
    }

    /**
     * Affiche le manuel d'utilisation avec arborescence
     */
    private static void showManualDialog(Stage owner) {
        Dialog<Void> manualDialog = new Dialog<>();
        manualDialog.setTitle("Manuel d'utilisation");
        manualDialog.initOwner(owner);
        manualDialog.setResizable(true);
        manualDialog.getDialogPane().setPrefSize(800, 600);

        // Création de l'arborescence
        TreeView<String> treeView = new TreeView<>();
        TreeItem<String> root = new TreeItem<>("Manuel du Générateur UML/ERD");
        root.setExpanded(true);

        // Sections principales
        TreeItem<String> introduction = new TreeItem<>("Introduction");
        TreeItem<String> installation = new TreeItem<>("Installation");
        TreeItem<String> interfaceUtilisateur = new TreeItem<>("Interface Utilisateur");
        TreeItem<String> creationDiagrammes = new TreeItem<>("Création de Diagrammes");
        TreeItem<String> fonctionsAvancees = new TreeItem<>("Fonctions Avancées");
        TreeItem<String> exportImport = new TreeItem<>("Export/Import");
        TreeItem<String> depannage = new TreeItem<>("Dépannage");

        // Sous-sections
        TreeItem<String> introWelcome = new TreeItem<>("Bienvenue");
        TreeItem<String> introFeatures = new TreeItem<>("Fonctionnalités principales");
        TreeItem<String> installRequirements = new TreeItem<>("Prérequis système");
        TreeItem<String> installSteps = new TreeItem<>("Guide d'installation");
        TreeItem<String> uiOverview = new TreeItem<>("Vue d'ensemble");
        TreeItem<String> uiMenus = new TreeItem<>("Menus et Barres d'outils");
        TreeItem<String> uiZones = new TreeItem<>("Zones de travail");
        TreeItem<String> createUML = new TreeItem<>("Diagrammes UML");
        TreeItem<String> createERD = new TreeItem<>("Diagrammes ERD");
        TreeItem<String> createElements = new TreeItem<>("Éléments et Relations");
        TreeItem<String> advancedCodeGen = new TreeItem<>("Génération de code");
        TreeItem<String> advancedTemplates = new TreeItem<>("Modèles personnalisés");
        TreeItem<String> exportFormats = new TreeItem<>("Formats d'export");
        TreeItem<String> importFormats = new TreeItem<>("Formats d'import");
        TreeItem<String> troubleshootCommon = new TreeItem<>("Problèmes courants");
        TreeItem<String> troubleshootErrors = new TreeItem<>("Messages d'erreur");

        // Construction de l'arborescence
        introduction.getChildren().addAll(introWelcome, introFeatures);
        installation.getChildren().addAll(installRequirements, installSteps);
        interfaceUtilisateur.getChildren().addAll(uiOverview, uiMenus, uiZones);
        creationDiagrammes.getChildren().addAll(createUML, createERD, createElements);
        fonctionsAvancees.getChildren().addAll(advancedCodeGen, advancedTemplates);
        exportImport.getChildren().addAll(exportFormats, importFormats);
        depannage.getChildren().addAll(troubleshootCommon, troubleshootErrors);

        root.getChildren().addAll(introduction, installation, interfaceUtilisateur, 
                                creationDiagrammes, fonctionsAvancees, exportImport, depannage);

        treeView.setRoot(root);

        // Zone de contenu avec TextArea au lieu de WebView
        TextArea contentView = new TextArea();
        contentView.setEditable(false);
        contentView.setWrapText(true);
        contentView.setStyle("-fx-font-family: Arial; -fx-font-size: 14px; -fx-control-inner-background: white;");
        contentView.setText(getDefaultManualContentText());

        // Gestion de la sélection dans l'arborescence
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String content = getManualContentForItemText(newVal.getValue());
                contentView.setText(content);
            }
        });

        // Layout
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(treeView, contentView);
        splitPane.setDividerPositions(0.3);

        manualDialog.getDialogPane().setContent(splitPane);
        manualDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        manualDialog.showAndWait();
    }

    /**
     * Retourne le contenu par défaut du manuel
     */
    private static String getDefaultManualContentText() {
        return "MANUEL D'UTILISATION\n" +
               "=====================================\n\n" +
               "Sélectionnez une section dans l'arborescence à gauche pour afficher son contenu.\n\n" +
               "NAVIGATION\n" +
               "----------\n" +
               "• Utilisez l'arborescence pour naviguer entre les sections\n" +
               "• Cliquez sur les flèches pour développer/réduire les sections\n" +
               "• Le contenu s'affiche automatiquement à droite\n\n" +
               "SECTIONS DISPONIBLES\n" +
               "--------------------\n" +
               "• Introduction : Présentation de l'application\n" +
               "• Installation : Guide d'installation et prérequis\n" +
               "• Interface Utilisateur : Description de l'interface\n" +
               "• Création de Diagrammes : Guide de création UML/ERD\n" +
               "• Fonctions Avancées : Fonctionnalités avancées\n" +
               "• Export/Import : Gestion des fichiers\n" +
               "• Dépannage : Résolution des problèmes courants\n";
    }

    /**
     * Retourne le contenu spécifique pour chaque item du manuel
     */
    private static String getManualContentForItemText(String itemName) {
        switch (itemName) {
            case "Bienvenue":
                return "BIENVENUE\n" +
                       "=====================================\n\n" +
                       "Bienvenue dans le Générateur UML/ERD/Code.\n\n" +
                       "Cet outil vous permet de créer des diagrammes UML et ERD de manière intuitive " +
                       "et de générer automatiquement du code à partir de vos modèles.\n\n" +
                       "L'application est conçue pour faciliter la modélisation de bases de données " +
                       "et la conception orientée objet.\n";
                       
            case "Fonctionnalités principales":
                return "FONCTIONNALITÉS PRINCIPALES\n" +
                       "=====================================\n\n" +
                       "• Création de diagrammes UML\n" +
                       "  - Diagrammes de classes\n" +
                       "  - Relations d'héritage\n" +
                       "  - Associations entre classes\n\n" +
                       "• Création de diagrammes ERD\n" +
                       "  - Entités et attributs\n" +
                       "  - Relations avec cardinalités\n" +
                       "  - Clés primaires et étrangères\n\n" +
                       "• Génération de code automatique\n" +
                       "  - Export SQL\n" +
                       "  - Export Java\n\n" +
                       "• Export en multiple formats\n" +
                       "  - XML pour sauvegarde\n" +
                       "  - PNG pour documentation\n\n" +
                       "• Interface utilisateur intuitive\n" +
                       "  - Drag & drop\n" +
                       "  - Zoom et navigation\n" +
                       "  - Propriétés personnalisables\n";
                       
            case "Diagrammes UML":
                return "DIAGRAMMES UML\n" +
                       "=====================================\n\n" +
                       "Les diagrammes UML (Unified Modeling Language) permettent de modéliser " +
                       "la structure orientée objet de votre application.\n\n" +
                       "CRÉATION D'UNE CLASSE\n" +
                       "---------------------\n" +
                       "1. Cliquez sur le bouton 'Ajouter Entité' dans la barre d'outils\n" +
                       "2. Donnez un nom à votre classe\n" +
                       "3. Ajoutez des attributs avec leurs types\n" +
                       "4. Ajoutez des méthodes si nécessaire\n\n" +
                       "RELATIONS\n" +
                       "---------\n" +
                       "• Héritage : Utilisez le type 'Héritage' pour créer une hiérarchie de classes\n" +
                       "• Association : Créez des liens entre classes pour modéliser les relations\n";
                       
            case "Diagrammes ERD":
                return "DIAGRAMMES ERD\n" +
                       "=====================================\n\n" +
                       "Les diagrammes ERD (Entity-Relationship Diagram) permettent de modéliser " +
                       "la structure de votre base de données.\n\n" +
                       "CRÉATION D'UNE ENTITÉ\n" +
                       "---------------------\n" +
                       "1. Passez en mode ERD en cliquant sur l'onglet 'ERD'\n" +
                       "2. Cliquez sur 'Ajouter Entité'\n" +
                       "3. Nommez votre table\n" +
                       "4. Ajoutez des attributs (colonnes)\n" +
                       "5. Définissez les clés primaires et étrangères\n\n" +
                       "CARDINALITÉS\n" +
                       "------------\n" +
                       "• 1,1 : Relation un à un\n" +
                       "• 1,N : Relation un à plusieurs\n" +
                       "• N,N : Relation plusieurs à plusieurs\n";
                       
            default:
                return itemName.toUpperCase() + "\n" +
                       "=====================================\n\n" +
                       "Contenu de la section " + itemName + " en cours de développement.\n\n" +
                       "Cette section sera complétée dans les prochaines versions du manuel.\n";
        }
    }

    /**
     * Affiche la fenêtre de soumission de bug
     */
    private static void showBugReportDialog(Stage owner) {
        Dialog<Void> bugDialog = new Dialog<>();
        bugDialog.setTitle("Soumettre un rapport de bug");
        bugDialog.initOwner(owner);
        bugDialog.setHeaderText("Signaler un problème");

        VBox form = new VBox(10);
        form.setPadding(new Insets(20));

        UserSession session = UserSession.getInstance();

        TextField nameField = new TextField(session.getFullName());
        TextField emailField = new TextField(session.getEmail());
        ComboBox<String> severityCombo = new ComboBox<>();
        severityCombo.getItems().addAll("Critique", "Élevée", "Moyenne", "Faible");
        severityCombo.setValue("Moyenne");
        
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Décrivez le problème en détail...");
        descriptionArea.setPrefRowCount(8);

        form.getChildren().addAll(
            new Label("Nom:"), nameField,
            new Label("Email:"), emailField,
            new Label("Sévérité:"), severityCombo,
            new Label("Description du problème:"), descriptionArea
        );

        bugDialog.getDialogPane().setContent(form);
        bugDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        bugDialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                sendBugReport(
                    nameField.getText(),
                    emailField.getText(),
                    severityCombo.getValue(),
                    descriptionArea.getText()
                );
            }
            return null;
        });

        bugDialog.showAndWait();
    }

    /**
     * Envoie le rapport de bug via mailto
     */
    private static void sendBugReport(String name, String email, String severity, String description) {
        try {
            String subject = "[BUG] Rapport de bug - " + severity;
            String body = "Nouveau rapport de bug:%0A%0A" +
                         "Nom: " + name + "%0A" +
                         "Email: " + email + "%0A" +
                         "Sévérité: " + severity + "%0A" +
                         "Description:%0A" + description.replace("\n", "%0A") + "%0A%0A" +
                         "Environnement utilisateur:%0A" +
                         "ID: " + UserSession.getInstance().getUserId() + "%0A" +
                         "Statut: " + (UserSession.getInstance().isSuperviseur() ? "Admin" : "Utilisateur");
            
            String mailtoUri = "mailto:support@example.com?subject=" + 
                              subject.replace(" ", "%20") + 
                              "&body=" + body;
            
            java.awt.Desktop.getDesktop().mail(java.net.URI.create(mailtoUri));
            
            showInfoAlert("Rapport préparé", "Votre client email s'ouvre avec le rapport de bug pré-rempli. Il ne reste plus qu'à l'envoyer.");
            
        } catch (Exception e) {
            showErrorAlert("Erreur", "Impossible d'ouvrir le client email: " + e.getMessage());
        }
    }

    /**
     * Ouvre le chatbot Discord
     */
    private static void openDiscordChatbot(Stage primaryStage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Support Discord");
        alert.setHeaderText("Rejoindre le support Discord");
        alert.setContentText("Pour accéder au support Discord, veuillez ouvrir votre navigateur et vous rendre sur:\n\n" +
                            "https://discord.com/app\n\n" +
                            "Notre serveur de support est disponible 24/7.");
        
        ButtonType openBrowserButton = new ButtonType("Ouvrir dans le navigateur");
        ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(openBrowserButton, closeButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openBrowserButton) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://discord.com/app"));
            } catch (Exception e) {
                showErrorAlert("Erreur", "Impossible d'ouvrir le navigateur: " + e.getMessage());
            }
        }
    }

    /**
     * Affiche une alerte d'erreur
     */
    private static void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Affiche une alerte d'information
     */
    private static void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Marque qu'il y a des modifications non sauvegardées
     */
    public static void markModified() {
        modificationsNonSauvegardees = true;
    }
}