package com.uml.generator.export;

import java.io.*;
import java.util.*;
import java.nio.file.Files;

public class DiagramImporter {

    public static Map<String, Object> importFromXML(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));
            return parseJSON(content);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'import: " + e.getMessage());
            return createEmptyDiagram();
        }
    }

    private static Map<String, Object> parseJSON(String jsonContent) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Parsing basique - à améliorer avec une vraie librairie JSON plus tard
            if (jsonContent.contains("\"entites\":")) {
                // Extraction simplifiée des entités
                result.put("entites", parseEntites(jsonContent));
                result.put("relations", parseRelations(jsonContent));
                result.put("schemaId", extractIntValue(jsonContent, "schemaId"));
                result.put("isUML", extractBooleanValue(jsonContent, "isUML"));
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing JSON: " + e.getMessage());
        }
        
        return result;
    }

    private static List<Map<String, Object>> parseEntites(String json) {
        List<Map<String, Object>> entites = new ArrayList<>();
        
        // Parsing simplifié - cherche les blocs d'entités
        String[] entityBlocks = json.split("\\{");
        for (String block : entityBlocks) {
            if (block.contains("\"nom\":")) {
                Map<String, Object> entite = new HashMap<>();
                
                // Extraire le nom
                String nom = extractStringValue(block, "nom");
                if (nom != null) {
                    entite.put("nom", nom);
                    entite.put("id", extractIntValue(block, "id"));
                    entite.put("position_x", extractDoubleValue(block, "position_x"));
                    entite.put("position_y", extractDoubleValue(block, "position_y"));
                    entite.put("attributs", parseAttributs(block));
                    
                    entites.add(entite);
                }
            }
        }
        
        return entites;
    }


    private static List<Map<String, Object>> parseRelations(String json) {
    List<Map<String, Object>> relations = new ArrayList<>();
    
    // Parsing simplifié des relations
    String[] relationBlocks = json.split("\\{");
    for (String block : relationBlocks) {
        if (block.contains("\"type\":")) {
            Map<String, Object> relation = new HashMap<>();
            
            relation.put("type", extractStringValue(block, "type"));
            relation.put("source_id", extractIntValue(block, "source_id"));
            relation.put("source_nom", extractStringValue(block, "source_nom"));
            relation.put("cible_id", extractIntValue(block, "cible_id"));
            relation.put("cible_nom", extractStringValue(block, "cible_nom"));
            relation.put("cardinalite_source", extractStringValue(block, "cardinalite_source"));
            relation.put("cardinalite_cible", extractStringValue(block, "cardinalite_cible"));
            
            relations.add(relation);
        }
    }
    
    return relations;
}
// === FIN DE LA NOUVELLE MÉTHODE ===

    private static List<Map<String, Object>> parseAttributs(String block) {
        List<Map<String, Object>> attributs = new ArrayList<>();
        
        // Parsing simplifié des attributs
        if (block.contains("\"attributs\":")) {
            String[] attrBlocks = block.split("\\{");
            for (String attrBlock : attrBlocks) {
                if (attrBlock.contains("\"nom\":")) {
                    Map<String, Object> attribut = new HashMap<>();
                    attribut.put("nom", extractStringValue(attrBlock, "nom"));
                    attribut.put("cle_primaire", extractBooleanValue(attrBlock, "cle_primaire"));
                    attribut.put("cle_etrangere", extractBooleanValue(attrBlock, "cle_etrangere"));
                    attributs.add(attribut);
                }
            }
        }
        
        return attributs;
    }

    // Méthodes d'extraction simplifiées
    private static String extractStringValue(String text, String key) {
        try {
            String pattern = "\"" + key + "\":\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return m.group(1);
        } catch (Exception e) {}
        return null;
    }

    private static int extractIntValue(String text, String key) {
        try {
            String pattern = "\"" + key + "\":\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return Integer.parseInt(m.group(1));
        } catch (Exception e) {}
        return -1;
    }

    private static double extractDoubleValue(String text, String key) {
        try {
            String pattern = "\"" + key + "\":\\s*(\\d+\\.?\\d*)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return Double.parseDouble(m.group(1));
        } catch (Exception e) {}
        return 0.0;
    }

    private static boolean extractBooleanValue(String text, String key) {
        try {
            String pattern = "\"" + key + "\":\\s*(true|false)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) return Boolean.parseBoolean(m.group(1));
        } catch (Exception e) {}
        return false;
    }

    private static Map<String, Object> createEmptyDiagram() {
        Map<String, Object> result = new HashMap<>();
        result.put("entites", new ArrayList<Map<String, Object>>());
        result.put("relationsERD", new ArrayList<Map<String, Object>>());
        result.put("schemaId", -1);
        result.put("isUML", true);
        return result;
    }
}
