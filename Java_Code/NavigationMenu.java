import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class NavigationMenu {

    public static MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        // ---- Fichier ----
        Menu fileMenu = new Menu("Fichier");
        MenuItem newItem = new MenuItem("Nouveau");
        MenuItem openItem = new MenuItem("Ouvrir");
        MenuItem saveItem = new MenuItem("Enregistrer");
        MenuItem exitItem = new MenuItem("Quitter");
        fileMenu.getItems().addAll(newItem, openItem, saveItem, new SeparatorMenuItem(), exitItem);

        // ---- Édition ----
        Menu editMenu = new Menu("Édition");
        MenuItem cutItem = new MenuItem("Coller");
        MenuItem copyItem = new MenuItem("Retour");
        MenuItem pasteItem = new MenuItem("Avancer");
        editMenu.getItems().addAll(cutItem, copyItem, pasteItem);

        // ---- Vue ----
        Menu viewMenu = new Menu("Vue");
        MenuItem zoomInItem = new MenuItem("Zoom avant (+)");
        MenuItem zoomOutItem = new MenuItem("Zoom arrière (-)");
        MenuItem resetViewItem = new MenuItem("Réinitialiser la vue");
        viewMenu.getItems().addAll(zoomInItem, zoomOutItem, resetViewItem);

        // ---- Générer ----
        Menu generateMenu = new Menu("Générer");
        MenuItem reportItem = new MenuItem("UML");
        MenuItem exportItem = new MenuItem("Diagramme E & R");
        generateMenu.getItems().addAll(reportItem, exportItem);

        // ---- Aide ----
        Menu helpMenu = new Menu("Aide");
        MenuItem aboutItem = new MenuItem("À propos");
        MenuItem docsItem = new MenuItem("Documentation");
        helpMenu.getItems().addAll(aboutItem, docsItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, generateMenu, helpMenu);

        // ---- Actions de menu ----
        exitItem.setOnAction(e -> primaryStage.close());

        saveItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le fichier");
            String userHome = System.getProperty("user.home");
            fileChooser.setInitialDirectory(new File(userHome + File.separator + "Documents"));
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Fichiers texte", "*.txt"),
                    new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"),
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            if (selectedFile != null) {
                System.out.println("Fichier enregistré : " + selectedFile.getAbsolutePath());
            }
        });

        return menuBar;
    }
}