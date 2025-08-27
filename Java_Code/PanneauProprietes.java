import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PanneauProprietes extends VBox {

    private TextField tfNom;
    private VBox attrBox;
    private TextField tfAttrNom;
    private ComboBox<String> cbAttrType;
    private RadioButton rbPK, rbFK;
    private ToggleGroup tgCle;
    private Button btnAjoutAttr;

    private Map<String, Object> entiteCourante;
    private ZoneModelisation zone;
    private InsertionDonnees dbManager; // Ajout de InsertionDonnees - Léa

    public PanneauProprietes(ZoneModelisation zone) {
        this.zone = zone;
        this.dbManager = new InsertionDonnees(); // Initialiser le gestionnaire de base de données
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

        Label lblAttr = new Label("Liste des attributs :");
        tfAttrNom = new TextField();
        tfAttrNom.setPromptText("Nom de l'attribut");

        cbAttrType = new ComboBox<>();
        cbAttrType.getItems().addAll("texte", "int", "float", "bool", "date");
        cbAttrType.setPromptText("Type");
        cbAttrType.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                cbAttrType.setPromptText(newVal);
        });

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
        this.getChildren().addAll(lblNom, tfNom, lblAttr, attrInput, btnAjoutAttr, attrBox);
    }

    public void remplirPanneau(Map<String, Object> entite) {
        this.entiteCourante = entite;
        tfNom.setText((String) entite.get("nom"));
        attrBox.getChildren().clear();

        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");
        for (Map<String, Object> attr : attributs) {
            String prefix = "";
            if ((boolean) attr.getOrDefault("cle_primaire", false))
                prefix += "PK ";
            if ((boolean) attr.getOrDefault("cle_etrangere", false))
                prefix += "FK ";
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

    private void ajouterAttribut() {
        if (entiteCourante == null)
            return;

        String nom = tfAttrNom.getText().trim();
        String type = cbAttrType.getValue();

        if (nom.isEmpty() || type == null)
            return;

        Map<String, Object> attribut = new HashMap<>();
        attribut.put("nom", nom);
        attribut.put("type", type);
        attribut.put("cle_primaire", rbPK.isSelected());
        attribut.put("cle_etrangere", rbFK.isSelected());

        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entiteCourante.get("attributs");
        attributs.add(attribut);

        zone.mettreAJourEntite(entiteCourante);
        remplirPanneau(entiteCourante);

        // Insérer l'attribut dans la base de données
        dbManager.insertEntity(entiteCourante, attributs, "UML");

        tfAttrNom.clear();
        cbAttrType.setValue(null);
        cbAttrType.setPromptText("Type");
        rbPK.setSelected(false);
        rbFK.setSelected(false);
    }
}
