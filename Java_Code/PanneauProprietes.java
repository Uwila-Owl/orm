
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

    private TextField tfNom;
    private TextField tfNomRelation; // Nouveau champ
    private TextField tfAttrNom;
    private TextField tfCardSource;
    private TextField tfCardCible;
    private ToggleGroup tgCle;
    private VBox attrBox;
    private Map<String, Object> entiteCourante;
    private ZoneModelisation zone;
    private ComboBox<String> cbAttrType;
    private ComboBox<String> cbLien;
    private Button btnCreerLien;
    private Button btnAjoutAttr;
    private RadioButton rbPK, rbFK;

    public PanneauProprietes(ZoneModelisation zone) {
        this.zone = zone;
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

        Label lblAttr = new Label("Liste des attributs :");
        tfAttrNom = new TextField();
        tfAttrNom.setPromptText("Nom de l'attribut");

        cbAttrType = new ComboBox<>();
        cbAttrType.getItems().addAll("texte", "int", "float", "bool", "date");
        cbAttrType.setPromptText("Type");

        tgCle = new ToggleGroup();
        rbPK = new RadioButton("PK");
        rbFK = new RadioButton("FK");
        rbPK.setToggleGroup(tgCle);
        rbFK.setToggleGroup(tgCle);

        HBox attrInput = new HBox(5, tfAttrNom, cbAttrType, rbPK, rbFK);
        attrInput.setSpacing(10);

        btnAjoutAttr = new Button("Ajouter attribut");
        btnAjoutAttr.setOnAction(e -> ajouterAttribut());

        attrBox = new VBox(5);

        cbLien = new ComboBox<>();
        cbLien.setPromptText("Sélectionner entité");

        // Champs pour la cardinalité
        tfCardSource = new TextField();
        tfCardSource.setPromptText("Card. source [x,x]");

        tfCardCible = new TextField();
        tfCardCible.setPromptText("Card. cible [x,x]");

        btnCreerLien = new Button();
        btnCreerLien.setOnAction(e -> {
            String cibleNom = cbLien.getValue();
            if (cibleNom != null && entiteCourante != null) {
                Map<String, Object> cible = zone.getEntiteParNom(cibleNom);
                if (cible != null) {
                    String typeLien = zone.isUML() ? "Héritage" : "Relation";

                    // Insertion en base via RelationDAO
                    try {
                        int entiteSourceId = (int) entiteCourante.get("id");
                        int entiteCibleId = (int) cible.get("id");
                        String typeSchema = (String) entiteCourante.get("type_schema");

                        String cardSource = tfCardSource.getText().trim();
                        String cardCible = tfCardCible.getText().trim();

                        RelationDAO relationDAO = new RelationDAO();
                        int relationId = relationDAO.insertRelation(
                                typeLien,
                                entiteSourceId,
                                entiteCibleId,
                                cardSource,
                                cardCible,
                                typeSchema
                        );

                        LOGGER.info("Relation créée en BDD avec ID = " + relationId);
                        zone.creerLienEntreEntites(entiteCourante, cible, typeLien);

                    } catch (Exception ex) {
                        LOGGER.severe("Erreur lors de la création de la relation : " + ex.getMessage());
                    }
                }
            }
        });

        HBox lienBox = new HBox(5, cbLien, btnCreerLien);
        VBox cardBox = new VBox(5, new Label("Cardinalités :"), tfCardSource, tfCardCible);

        // Ajout des éléments dans l'ordre, avec Nom relation avant la liste des attributs
        this.getChildren().addAll(lblNom, tfNom, lblNomRelation, tfNomRelation, lblAttr, attrInput, btnAjoutAttr, attrBox, cardBox, lienBox);
    }

    public void remplirPanneau(Map<String, Object> entite) {
        this.entiteCourante = entite;
        tfNom.setText((String) entite.get("nom"));

        // Remplir Nom relation
        tfNomRelation.setText((String) entite.getOrDefault("nom_relation", ""));

        attrBox.getChildren().clear();

        tfCardSource.clear();
        tfCardCible.clear();

        // Récupérer cardinalités depuis la BDD si relation existe
        if (entiteCourante.containsKey("id")) {
            int entiteSourceId = (int) entiteCourante.get("id");
            RelationDAO relationDAO = new RelationDAO();
            Map<String, Object> relation = relationDAO.getRelationBySourceId(entiteSourceId);

            if (relation != null) {
                tfCardSource.setText((String) relation.get("cardinalite_source"));
                tfCardCible.setText((String) relation.get("cardinalite_cible"));
            }
        }

        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");
        if (attributs != null) {
            for (Map<String, Object> attr : attributs) {
                String prefix = "";
                if ((boolean) attr.getOrDefault("cle_primaire", false)) {
                    prefix += "PK ";
                }
                if ((boolean) attr.getOrDefault("cle_etrangere", false)) {
                    prefix += "FK ";
                }
                String type = (String) attr.getOrDefault("type", "");
                Label lbl = new Label(prefix + attr.get("nom") + (type.isEmpty() ? "" : " : " + type));

                Button btnSuppr = new Button("X");
                btnSuppr.setOnAction(e -> {
                    attributs.remove(attr);
                    zone.mettreAJourEntite(entiteCourante);
                    remplirPanneau(entiteCourante);
                });

                HBox hbox = new HBox(5, lbl, btnSuppr);
                attrBox.getChildren().add(hbox);
            }
        }

        List<String> autresEntites = zone.getNomsEntitesExcluant(entite);
        cbLien.setItems(FXCollections.observableArrayList(autresEntites));

        if (zone.isUML()) {
            btnCreerLien.setText("Créer Héritage");
            cbLien.setPromptText("Héritage vers...");
        } else {
            btnCreerLien.setText("Créer Relation");
            cbLien.setPromptText("Relation vers...");
        }
    }

    private void ajouterAttribut() {
        if (entiteCourante == null) {
            return;
        }

        String nom = tfAttrNom.getText().trim();
        String type = cbAttrType.getValue();

        if (nom.isEmpty() || type == null) {
            return;
        }

        Map<String, Object> attribut = new java.util.HashMap<>();
        attribut.put("nom", nom);
        attribut.put("type", type);
        attribut.put("cle_primaire", rbPK.isSelected());
        attribut.put("cle_etrangere", rbFK.isSelected());

        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entiteCourante.get("attributs");
        attributs.add(attribut);

        zone.mettreAJourEntite(entiteCourante);
        remplirPanneau(entiteCourante);

        tfAttrNom.clear();
        cbAttrType.setValue(null);
        cbAttrType.setPromptText("Type");
        rbPK.setSelected(false);
        rbFK.setSelected(false);
    }
}
