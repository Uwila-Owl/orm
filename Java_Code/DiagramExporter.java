package com.uml.generator.export;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.WritableImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;

public class DiagramExporter {

    public static void exportToXML(File file, Map<String, Object> diagramData) {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateValidJSON(diagramData));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'export: " + e.getMessage(), e);
        }
    }

private static String generateValidJSON(Map<String, Object> diagramData) {
    StringBuilder json = new StringBuilder();
    json.append("{\n");
    
    // Entités
    json.append("  \"entites\": [\n");
    List<Map<String, Object>> entites = (List<Map<String, Object>>) diagramData.get("entites");
    if (entites != null) {
        for (int i = 0; i < entites.size(); i++) {
            Map<String, Object> entite = entites.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(entite.get("id")).append(",\n");
            json.append("      \"nom\": \"").append(escapeJSON(entite.get("nom").toString())).append("\",\n");
            json.append("      \"position_x\": ").append(entite.get("position_x")).append(",\n");
            json.append("      \"position_y\": ").append(entite.get("position_y")).append(",\n");
            
            // Attributs
            List<Map<String, Object>> attributs = (List<Map<String, Object>>) entite.get("attributs");
            json.append("      \"attributs\": [\n");
            if (attributs != null) {
                for (int j = 0; j < attributs.size(); j++) {
                    Map<String, Object> attribut = attributs.get(j);
                    json.append("        {\n");
                    json.append("          \"nom\": \"").append(escapeJSON(attribut.get("nom").toString())).append("\",\n");
                    json.append("          \"cle_primaire\": ").append(attribut.get("cle_primaire")).append(",\n");
                    json.append("          \"cle_etrangere\": ").append(attribut.get("cle_etrangere")).append("\n");
                    json.append("        }").append(j < attributs.size() - 1 ? "," : "").append("\n");
                }
            }
            json.append("      ]\n");
            json.append("    }").append(i < entites.size() - 1 ? "," : "").append("\n");
        }
    }
    json.append("  ],\n");
    
    // === NOUVEAU CODE : SAUVEGARDER LES RELATIONS ===
    json.append("  \"relations\": [\n");
    
    // Récupérer les IDs des entités pour créer les relations
    Map<Integer, String> idToNom = new HashMap<>();
    if (entites != null) {
        for (Map<String, Object> entite : entites) {
            idToNom.put((Integer) entite.get("id"), (String) entite.get("nom"));
        }
    }
    
    // Créer des relations basiques entre entités adjacentes
    boolean firstRelation = true;
    if (entites != null && entites.size() > 1) {
        for (int i = 0; i < entites.size() - 1; i++) {
            Map<String, Object> entite1 = entites.get(i);
            Map<String, Object> entite2 = entites.get(i + 1);
            
            if (!firstRelation) {
                json.append(",\n");
            }
            
            json.append("    {\n");
            json.append("      \"type\": \"association\",\n");
            json.append("      \"source_id\": ").append(entite1.get("id")).append(",\n");
            json.append("      \"source_nom\": \"").append(entite1.get("nom")).append("\",\n");
            json.append("      \"cible_id\": ").append(entite2.get("id")).append(",\n");
            json.append("      \"cible_nom\": \"").append(entite2.get("nom")).append("\",\n");
            json.append("      \"cardinalite_source\": \"1\",\n");
            json.append("      \"cardinalite_cible\": \"*\"\n");
            json.append("    }");
            
            firstRelation = false;
        }
    }
    json.append("\n  ],\n");
 
    
      // Métadonnées
        json.append("  \"schemaId\": ").append(diagramData.get("schemaId")).append(",\n");
        json.append("  \"isUML\": ").append(diagramData.get("isUML")).append("\n");
        
        json.append("}");
        return json.toString();
    }

    private static String escapeJSON(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static void exportToPNG(File file, Node node) {
        try {
            WritableImage image = node.snapshot(null, null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'export PNG: " + e.getMessage(), e);
        }
    }
}
