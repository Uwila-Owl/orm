
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PanneauProprietes extends VBox {

    private static final Logger LOGGER = Logger.getLogger(PanneauProprietes.class.getName());
    private LogDAO logDAO = new LogDAO();
    private TextField tfNom;
    private TextField tfNomRelation; // Nouveau champ
    private TextField tfAttrNom;
    private ToggleGroup tgCle;
    private VBox attrBox;
    private Map<String, Object> entiteCourante;
    private ZoneModelisation zone;
    private ComboBox<String> cbLien;
    private InterfaceGenerateurUML interfaceRef;
   
    private Button btnAjoutAttr;
    private RadioButton rbPK, rbFK;
    private Button btnSupprimerBloc;
    private Label labelVide = new Label("Aucune entité sélectionnée");


    // --- Zone Info Bloc ---
private Label lblNomBloc;
private Label lblPK;
private Label lblFK;


private VBox attributsBox;   // Zone 2 : liste des attributs
private TextField tfEditNom;
private RadioButton rbEditPK;
private RadioButton rbEditFK;
private RadioButton rbEditSimple;
private Button btnAppliquerModif;
private VBox editBox;

private Label lblZone3;
// Zone 3 : liste des relations
private VBox relationBox; // contiendra les relations et leurs boutons
private RelationDAO relationDAO = new RelationDAO(); // DAO pour manipuler les relations


    String userId = UserSession.getInstance().getUserId();

    public PanneauProprietes(ZoneModelisation zone, InterfaceGenerateurUML interfaceRef) {
        this.zone = zone;
        this.interfaceRef = interfaceRef;
        this.setPadding(new Insets(10));
        this.setSpacing(10);
        this.setPrefWidth(300);

        Label lblNom = new Label("Nom de l'entité :");
        tfNom = new TextField();
        tfNom.setOnAction(e -> {
            if (entiteCourante != null) {
                entiteCourante.put("nom", tfNom.getText());
                zone.mettreAJourEntite(entiteCourante);
            }
        });
        
        // --- Zone Info Bloc ---
Label titreInfo = new Label("Info bloc");
titreInfo.setStyle("-fx-font-weight: bold; -fx-background-color: #e6e6e6; -fx-padding: 5; -fx-font-size: 12px;");

lblNomBloc = new Label("Nom du bloc : ");
lblPK = new Label("Clé primaire : ");
lblFK = new Label("Clé étrangère : ");


VBox infoBloc = new VBox(5, titreInfo, lblNomBloc, lblPK, lblFK);
infoBloc.setPadding(new Insets(10));
infoBloc.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #ccc; -fx-border-radius: 5;");

// Ajouter la zone Info bloc en haut du panneau
this.getChildren().add(infoBloc);


        // Label + champ Nom relation
        Label lblNomRelation = new Label("Nom relation :");
        tfNomRelation = new TextField();
        tfNomRelation.setPromptText("Nom de la relation");
        tfNomRelation.setOnAction(e -> {
            if (entiteCourante != null) {
                String nouveauNom = tfNomRelation.getText();
                entiteCourante.put("nom_relation", nouveauNom);

                // Mise à jour visuelle si ERD
                Text relationText = (Text) entiteCourante.get("relation_text");
                if (relationText != null) {
                    relationText.setText(nouveauNom);
                }

                zone.mettreAJourEntite(entiteCourante);
            }
        });
        
        // --- Zone 2 : Liste des attributs ---
Label lblZone2 = new Label("Liste des attributs :");
lblZone2.setStyle("-fx-font-weight: bold; -fx-background-color: #e6e6e6; -fx-padding: 5; -fx-font-size: 12px;");

attributsBox = new VBox(5);  // contiendra la liste des attributs

// Zone d'édition de l'attribut sélectionné
tfEditNom = new TextField();
tfEditNom.setPromptText("Nom de l'attribut");


rbEditPK = new RadioButton("PK");
rbEditFK = new RadioButton("FK");
rbEditSimple = new RadioButton("Neutre");
ToggleGroup tgEdit = new ToggleGroup();
rbEditSimple.setToggleGroup(tgEdit);
rbEditPK.setToggleGroup(tgEdit);
rbEditFK.setToggleGroup(tgEdit);

btnAppliquerModif = new Button("Appliquer");

editBox = new VBox(5, new Label("Modifier l'attribut :"), tfEditNom, rbEditPK, rbEditFK, rbEditSimple, btnAppliquerModif);

editBox.setVisible(false); // caché par défaut
editBox.setManaged(false);




        Label lblAttr = new Label("Ajouter Attribut :");
        tfAttrNom = new TextField();
        tfAttrNom.setPromptText("Nom de l'attribut");

        

        tgCle = new ToggleGroup();
        rbPK = new RadioButton("PK");
        rbFK = new RadioButton("FK");
        rbPK.setToggleGroup(tgCle);
        rbFK.setToggleGroup(tgCle);

        HBox attrInput = new HBox(5, tfAttrNom,  rbPK, rbFK);
        attrInput.setSpacing(10);

        btnAjoutAttr = new Button("Ajouter attribut");
        btnAjoutAttr.setOnAction(e -> ajouterAttribut());

        attrBox = new VBox(5);

        cbLien = new ComboBox<>();
        cbLien.setPromptText("Sélectionner entité");
        
        // --- Zone 3 : Liste des relations de l'entité ---
lblZone3 = new Label("Relations de l'entité :");
lblZone3.setStyle("-fx-font-weight: bold; -fx-background-color: #e6e6e6; -fx-padding: 5; -fx-font-size: 12px;");

relationBox = new VBox(5);
relationBox.setPadding(new Insets(5));
relationBox.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #ccc; -fx-border-radius: 5;");

        

       
        
        

        
 btnSupprimerBloc = new Button("Supprimer ce bloc");
btnSupprimerBloc.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-font-weight: bold;");
btnSupprimerBloc.setOnAction(e -> {
    if (entiteCourante != null) {
        int entiteId = (int) entiteCourante.get("id");
        zone.supprimerEntite(entiteId); // Supprime graphiquement l'entité de la zone
        entiteCourante = null;

        // Réinitialisation des champs du panneau sans supprimer le panneau
        tfNom.clear();
        tfNomRelation.clear();
        attrBox.getChildren().clear();
        rbPK.setSelected(false);
        rbFK.setSelected(false);
        

        // Réinitialisation des labels Info Bloc
        lblNomBloc.setText("Nom du bloc : ");
        lblPK.setText("Clé primaire : ");
        lblFK.setText("Clé étrangère : ");
      
    }
});

// Initialiser la visibilité selon le mode courant
updateVisibility();

// Écouter les changements UML/ERD
setupVisibilityListeners();




        // Ajout des éléments dans l'ordre, avec Nom relation avant la liste des attributs
        this.getChildren().addAll(lblNom, tfNom, lblNomRelation, tfNomRelation, lblZone2, attributsBox, editBox, lblAttr, attrInput, btnAjoutAttr, lblZone3, relationBox, btnSupprimerBloc);
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
    if (lblZone3 != null && relationBox != null && interfaceRef != null) {
        boolean isERDSelected = interfaceRef.getBtnERD() != null && interfaceRef.getBtnERD().isSelected();

        lblZone3.setVisible(isERDSelected);
        lblZone3.setManaged(isERDSelected);
        relationBox.setVisible(isERDSelected);
        relationBox.setManaged(isERDSelected);
    }
}


    private void afficherRelations(int entiteSourceId) {
    relationBox.getChildren().clear();

    List<Map<String, Object>> relations = relationDAO.getAllRelations();
    for (Map<String, Object> rel : relations) {
        int relId = (int) rel.get("id");
        int srcId = (int) rel.get("entite_source_id");

        if (srcId != entiteSourceId) continue;

        String nom = (String) rel.get("nom");
        String srcCard = (String) rel.get("cardinalite_source");
        String dstCard = (String) rel.get("cardinalite_cible");

        Label lblNomRel = new Label("Relation : " + nom);
        lblNomRel.setStyle("-fx-font-weight: bold;");

        TextField tfSrc = new TextField(srcCard);
        tfSrc.setPrefWidth(50);

        TextField tfDst = new TextField(dstCard);
        tfDst.setPrefWidth(50);

        Button btnSave = new Button("✔");
        btnSave.setOnAction(e -> {
            String newSrc = tfSrc.getText().trim();
            String newDst = tfDst.getText().trim();
            relationDAO.updateRelation(relId, nom, newSrc, newDst);
            
        });

        Button btnDel = new Button("✘");
        btnDel.setOnAction(e -> {
            relationDAO.supprimerRelation(relId);
            
            afficherRelations(entiteSourceId);
        });

        HBox cardBox = new HBox(5, new Label("Source:"), tfSrc, new Label("Cible:"), tfDst);
        HBox actionBox = new HBox(5, btnSave, btnDel);

        VBox relationBloc = new VBox(5, lblNomRel, cardBox, actionBox);
        relationBloc.setPadding(new Insets(5));
        relationBloc.setStyle("-fx-background-color: #f1f1f1; -fx-border-color: #ccc; -fx-border-radius: 5;");

        relationBox.getChildren().add(relationBloc);
    }
}


    public void remplirPanneau(Map<String, Object> entite) {
        this.entiteCourante = entite;
        
        // --- Mise à jour Info bloc ---
lblNomBloc.setText("Nom du bloc : " + (String) entite.get("nom"));

// Chercher les PK et FK dans les attributs
List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");
String pk = "";
String fk = "";
if (attributs != null) {
    for (Map<String, Object> attr : attributs) {
        if ((boolean) attr.getOrDefault("cle_primaire", false)) {
            pk = (String) attr.get("nom");
        }
        if ((boolean) attr.getOrDefault("cle_etrangere", false)) {
            fk = (String) attr.get("nom");
        }
    }
}
lblPK.setText("Clé primaire : " + (pk.isEmpty() ? "Aucune" : pk));
lblFK.setText("Clé étrangère : " + (fk.isEmpty() ? "Aucune" : fk));





        tfNom.setText((String) entite.get("nom"));

        // Remplir Nom relation
        tfNomRelation.setText((String) entite.getOrDefault("nom_relation", ""));

        attrBox.getChildren().clear();

        

        // Récupérer cardinalités depuis la BDD si relation existe
        if (entiteCourante.containsKey("id")) {
            int entiteSourceId = (int) entiteCourante.get("id");
            RelationDAO relationDAO = new RelationDAO();
            Map<String, Object> relation = relationDAO.getRelationBySourceId(entiteSourceId);

            
        }

        

        List<String> autresEntites = zone.getNomsEntitesExcluant(entite);
        cbLien.setItems(FXCollections.observableArrayList(autresEntites));

        
        mettreAJourAttributs();
        // Afficher les relations pour l'entité sélectionnée
if (entiteCourante.containsKey("id")) {
    if (zone.isUML()) {
        relationBox.setVisible(false);
    } else {
        relationBox.setVisible(true);
        int entiteSourceId = (int) entiteCourante.get("id");
        afficherRelations(entiteSourceId);
    }
}
if (editBox != null) {
    editBox.setVisible(false);
    editBox.setManaged(false);
}
}

 
    
    private void mettreAJourAttributs() {
    attributsBox.getChildren().clear();
    List<Map<String, Object>> attributs = (List<Map<String, Object>>) entiteCourante.get("attributs");
    if (attributs != null) {
        for (Map<String, Object> attr : attributs) {
            Label lbl = new Label(attr.get("nom") + " : " +
                ((boolean) attr.getOrDefault("cle_primaire", false) ? " PK" : "") +
                ((boolean) attr.getOrDefault("cle_etrangere", false) ? " FK" : "")
            );

            Button btnEdit = new Button("Modifier");
            Button btnSuppr = new Button("Supprimer");

            HBox hbox = new HBox(5, lbl, btnEdit, btnSuppr);
            attributsBox.getChildren().add(hbox);

            // Action Modifier
            btnEdit.setOnAction(e -> {
            editBox.setVisible(true);
            editBox.setManaged(true);
                tfEditNom.setText((String) attr.get("nom"));
         
                rbEditPK.setSelected((boolean) attr.getOrDefault("cle_primaire", false));
                rbEditFK.setSelected((boolean) attr.getOrDefault("cle_etrangere", false));

                btnAppliquerModif.setOnAction(ev -> {
                    attr.put("nom", tfEditNom.getText().trim());
                    if (rbEditPK.isSelected()) {
        attr.put("cle_primaire", true);
        attr.put("cle_etrangere", false);
    } else if (rbEditFK.isSelected()) {
        attr.put("cle_primaire", false);
        attr.put("cle_etrangere", true);
    } else if (rbEditSimple.isSelected()) {
        attr.put("cle_primaire", false);
        attr.put("cle_etrangere", false);
    }

                    zone.mettreAJourEntite(entiteCourante);
                    mettreAJourAttributs(); // rafraîchir la liste
                    editBox.setVisible(false); 
                    editBox.setManaged(false);
                });
            });

            // Action Supprimer
            btnSuppr.setOnAction(e -> {
                attributs.remove(attr);
                zone.mettreAJourEntite(entiteCourante);
                mettreAJourAttributs();
            });
        }
    }
}


    private void ajouterAttribut() {
        if (entiteCourante == null) {
            return;
        }

        String nom = tfAttrNom.getText().trim();
        

        Map<String, Object> attribut = new java.util.HashMap<>();
        attribut.put("nom", nom);
        
        attribut.put("cle_primaire", rbPK.isSelected());
        attribut.put("cle_etrangere", rbFK.isSelected());

        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entiteCourante.get("attributs");
        attributs.add(attribut);

        zone.mettreAJourEntite(entiteCourante);
         mettreAJourAttributs();

        tfAttrNom.clear();
        rbPK.setSelected(false);
        rbFK.setSelected(false);
    }
}
