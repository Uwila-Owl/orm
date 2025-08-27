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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Visuel {

    private double zoomFactor = 1.0;

    public void ajouterEntiteUML(Group entiteVisuelle, Map<String, Object> entite) {
        String nom = (String) entite.get("nom");
        double positionX = (double) entite.get("position_x");
        double positionY = (double) entite.get("position_y");
        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");

        Rectangle rect = new Rectangle(140, 30 + (attributs != null ? attributs.size() : 0) * 20 + 20);
        rect.setFill(Color.LIGHTBLUE);
        rect.setStroke(Color.DARKBLUE);
        rect.setStrokeWidth(2);

        Text nomText = new Text(nom);
        nomText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nomText.setFill(Color.DARKBLUE);
        nomText.setX(10);
        nomText.setY(20);

        Line separateur = new Line(5, 25, 135, 25);
        separateur.setStroke(Color.DARKBLUE);

        entiteVisuelle.getChildren().addAll(rect, nomText, separateur);

        int yAttrib = 45;
        if (attributs != null) {
            // Trier les attributs pour que les clés primaires apparaissent en premier
            attributs.sort(Comparator.comparing(attribut -> !(boolean) attribut.get("cle_primaire")));

            for (Map<String, Object> attribut : attributs) {
                String attrNom = (String) attribut.get("nom");
                boolean clePrimaire = (boolean) attribut.get("cle_primaire");
                boolean cleEtrangere = (boolean) attribut.get("cle_etrangere");

                String prefix = "";
                if (clePrimaire) prefix += "PK ";
                if (cleEtrangere) prefix += "FK ";

                Text attrText = new Text(prefix + attrNom);
                attrText.setX(10);
                attrText.setY(yAttrib);
                attrText.setFill(clePrimaire ? Color.RED : (cleEtrangere ? Color.ORANGE : Color.BLACK));
                attrText.setFont(Font.font("Arial", clePrimaire ? FontWeight.BOLD : FontWeight.NORMAL, 11));

                entiteVisuelle.getChildren().add(attrText);
                yAttrib += 18;
            }
        }
    }

    public void ajouterEntiteERD(Group entiteVisuelle, Map<String, Object> entite) {
        String nom = (String) entite.get("nom");
        double positionX = (double) entite.get("position_x");
        double positionY = (double) entite.get("position_y");
        List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");

        Rectangle rectPrincipal = new Rectangle(120, 60);
        rectPrincipal.setFill(Color.WHITE);
        rectPrincipal.setStroke(Color.BLACK);
        rectPrincipal.setStrokeWidth(2);

        Text nomText = new Text(nom);
        nomText.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        nomText.setFill(Color.BLACK);
        nomText.setX(60 - nomText.getBoundsInLocal().getWidth() / 2);
        nomText.setY(35);

        entiteVisuelle.getChildren().addAll(rectPrincipal, nomText);

        double angleStep = 360.0 / Math.max((attributs != null ? attributs.size() : 0), 1);
        double rayon = 80;

        if (attributs != null) {
            for (int i = 0; i < attributs.size(); i++) {
                Map<String, Object> attribut = attributs.get(i);
                String attrNom = (String) attribut.get("nom");
                boolean clePrimaire = (boolean) attribut.get("cle_primaire");
                boolean cleEtrangere = (boolean) attribut.get("cle_etrangere");

                double angle = Math.toRadians(i * angleStep);
                double attrX = 60 + rayon * Math.cos(angle);
                double attrY = 30 + rayon * Math.sin(angle);

                Ellipse ellipseAttr = new Ellipse(25, 15);
                ellipseAttr.setFill(clePrimaire ? Color.GOLD : (cleEtrangere ? Color.LIGHTCORAL : Color.LIGHTGREEN));
                ellipseAttr.setStroke(Color.BLACK);
                ellipseAttr.setStrokeWidth(clePrimaire ? 3 : 1);
                ellipseAttr.setCenterX(attrX);
                ellipseAttr.setCenterY(attrY);

                Text attrText = new Text(attrNom);
                attrText.setFont(Font.font("Arial", clePrimaire ? FontWeight.BOLD : FontWeight.NORMAL, 10));
                attrText.setFill(Color.BLACK);
                attrText.setX(attrX - attrText.getBoundsInLocal().getWidth() / 2);
                attrText.setY(attrY + 3);

                Line ligneAttr = new Line();
                ligneAttr.setStartX(60);
                ligneAttr.setStartY(30);
                ligneAttr.setEndX(attrX);
                ligneAttr.setEndY(attrY);
                ligneAttr.setStroke(Color.BLACK);

                entiteVisuelle.getChildren().addAll(ligneAttr, ellipseAttr, attrText);
            }
        }
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
        node.setScaleX(zoomFactor);
        node.setScaleY(zoomFactor);
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
        System.out.println("Action annulée");
    }

    private void retabAction() {
        System.out.println("Action rétablie");
    }
}
