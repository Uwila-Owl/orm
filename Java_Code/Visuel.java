
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.input.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javafx.geometry.VPos;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.FontPosture;

public class Visuel {

    private LogDAO logDAO = new LogDAO();
    String userId = UserSession.getInstance().getUserId();

    private double zoomFactor = 1.0;

    public void ajouterEntite(Group entiteVisuelle, Map<String, Object> entite, String typeSchema) {
        String nom = (String) entite.get("nom");
        double positionX = (double) entite.get("position_x");
        double positionY = (double) entite.get("position_y");
        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");
        List<Map<String, Object>> operations = (List<Map<String, Object>>) entite.get("operations");

        // Dimensions
        int largeur = 160;
        int hauteurNom = 30;
        int hauteurAttrib = (attributs != null ? attributs.size() : 0) * 20;
        int hauteurTotale = hauteurNom + hauteurAttrib + 10;

        // Rectangle fond blanc + bordure noire 2px
        Rectangle rect = new Rectangle(largeur, hauteurTotale);
        rect.setFill(Color.WHITE);
        rect.setStroke(Color.BLACK);
        rect.setStrokeWidth(2);

        // Texte nom centré horizontalement et verticalement dans la zone nom
        Text nomText = new Text(nom);
        nomText.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        nomText.setFill(Color.BLACK);
        nomText.setTextAlignment(TextAlignment.CENTER);
        nomText.setTextOrigin(VPos.CENTER);

        // Centrer horizontalement en positionnant X à la moitié moins la moitié de la largeur du texte
        double textWidth = nomText.getLayoutBounds().getWidth();
        nomText.setX((largeur - textWidth) / 2);
        nomText.setY(hauteurNom / 2.0);
        nomText.setTextOrigin(VPos.CENTER);

        // Ligne séparatrice sous le nom
        Line separateur = new Line(5, hauteurNom, largeur - 5, hauteurNom);
        separateur.setStroke(Color.BLACK);

        entiteVisuelle.getChildren().clear();
        entiteVisuelle.getChildren().addAll(rect, nomText, separateur);

        // Ajout des attributs
        int y = hauteurNom + 20;
        if (attributs != null) {
            // Trier pour que clés primaires en premier
            attributs.sort(Comparator.comparing(attrib -> !(boolean) attrib.get("cle_primaire")));

            for (Map<String, Object> attribut : attributs) {
                String attrNom = (String) attribut.get("nom");
                boolean clePrimaire = (boolean) attribut.get("cle_primaire");
                boolean cleEtrangere = (boolean) attribut.get("cle_etrangere");

                String prefix = "";
                if (clePrimaire) {
                    prefix += "PK ";
                }
                if (cleEtrangere) {
                    prefix += "FK ";
                }

                Text attrText = new Text(prefix + attrNom);
                attrText.setX(10);
                attrText.setY(y);
                attrText.setFill(Color.BLACK);

                // Style : gras si clé primaire, italique si clé étrangère
                FontWeight fw = clePrimaire ? FontWeight.BOLD : FontWeight.NORMAL;
                FontPosture fp = cleEtrangere ? FontPosture.ITALIC : FontPosture.REGULAR;
                attrText.setFont(Font.font("Arial", fw, fp, 12));

                entiteVisuelle.getChildren().add(attrText);
                y += 18;
            }
        }

        //--Ajout Lyna
// --- Ajout des opérations (SEULEMENT pour UML) ---
        if ("UML".equals(typeSchema) && operations != null && !operations.isEmpty()) {
            // Ligne séparatrice entre attributs et opérations
            Line sepOps = new Line(5, y - 10, largeur - 5, y - 10);
            sepOps.setStroke(Color.BLACK);
            entiteVisuelle.getChildren().add(sepOps);

            y += 5; // petit espace avant la première opération

            for (Map<String, Object> op : operations) {
                String opNom = (String) op.get("nom");
                Text opText = new Text("+ " + opNom + "() "); // ajout du "+" et "()"
                opText.setX(10);
                opText.setY(y);
                opText.setFill(Color.DARKBLUE);
                opText.setFont(Font.font("Arial", FontPosture.ITALIC, 12));

                entiteVisuelle.getChildren().add(opText);
                y += 18;
            }
        }

        // Ajuster la hauteur du rectangle
        rect.setHeight(y + 5);

        //--
        // Positionnement du groupe
        entiteVisuelle.setLayoutX(positionX);
        entiteVisuelle.setLayoutY(positionY);
    }

    public void zoomSouris(ScrollEvent event, Node node) {
        if (event.isControlDown()) {
            if (event.getDeltaY() > 0) {
                zoomIn(node);
            } else {
                zoomOut(node);
            }
            event.consume();
        }
    }

    private void zoomIn(Node node) {
        zoomFactor *= 1.1;
        applyZoom(node);
    }

    private void zoomOut(Node node) {
        zoomFactor /= 1.1;
        applyZoom(node);
    }

    private void applyZoom(Node node) {
        // Le 'node' passé ici est la ZoneModelisation.
        // Nous devons accéder à son contentGroup pour appliquer le zoom.
        if (node instanceof ZoneModelisation) {
            Group contentGroup = ((ZoneModelisation) node).getContentGroup();
            contentGroup.setScaleX(zoomFactor);
            contentGroup.setScaleY(zoomFactor);
        } else {
            // Fallback ou log d'erreur si le type de nœud n'est pas celui attendu
            node.setScaleX(zoomFactor);
            node.setScaleY(zoomFactor);
        }
    }

    public void toucheClavAppui(KeyEvent event) {
        if (event.isControlDown()) {
            if (event.getCode() == KeyCode.Z) {
                annulAction();
            } else if (event.getCode() == KeyCode.Y) {
                retabAction();
            }
        }
    }

    private void annulAction() {
        logDAO.insertLog(userId, "Action annulée", "INFO");
    }

    private void retabAction() {
        logDAO.insertLog(userId, "Action rétablie", "INFO");
    }
}
