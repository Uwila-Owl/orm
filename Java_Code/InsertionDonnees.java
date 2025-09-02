import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsertionDonnees {

    public void insertEntity(Map<String, Object> entity, List<Map<String, Object>> attributes, String schemaType) {
        String insertEntitySQL = "INSERT INTO entites (nom, position_x, position_y, type_schema) VALUES (?, ?, ?, ?) RETURNING id";
        String insertAttributeSQL = "INSERT INTO attributs (nom, cle_primaire, cle_etrangere, entite_id, type_schema) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = ConnexionBdd.getConnection()) {
            connection.setAutoCommit(false); // Démarrer une transaction

            // Insérer l'entité
            try (PreparedStatement pstmt = connection.prepareStatement(insertEntitySQL)) {
                pstmt.setString(1, (String) entity.get("nom"));
                pstmt.setDouble(2, (double) entity.get("position_x"));
                pstmt.setDouble(3, (double) entity.get("position_y"));
                pstmt.setString(4, schemaType); // Utiliser le type de schéma
                int entityId = pstmt.executeUpdate(); // Récupérer l'ID de l'entité insérée

                // Insérer les attributs
                try (PreparedStatement pstmtAttr = connection.prepareStatement(insertAttributeSQL)) {
                    for (Map<String, Object> attribute : attributes) {
                        pstmtAttr.setString(1, (String) attribute.get("nom"));
                        pstmtAttr.setBoolean(2, (boolean) attribute.get("cle_primaire"));
                        pstmtAttr.setBoolean(3, (boolean) attribute.get("cle_etrangere"));
                        pstmtAttr.setInt(4, entityId);
                        pstmtAttr.setString(5, schemaType); // Utiliser le type de schéma
                        pstmtAttr.addBatch();
                    }
                    pstmtAttr.executeBatch();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Map<String, Object>> loadEntities(String schemaType) {
        List<Map<String, Object>> entities = new ArrayList<>();
        String selectEntitiesSQL = "SELECT * FROM entites WHERE type_schema = ?";
        String selectAttributesSQL = "SELECT * FROM attributs WHERE entite_id = ? AND type_schema = ?";

        try (Connection connection = ConnexionBdd.getConnection()) {
            try (PreparedStatement pstmtEntity = connection.prepareStatement(selectEntitiesSQL)) {
                pstmtEntity.setString(1, schemaType);
                try (ResultSet rsEntity = pstmtEntity.executeQuery()) {
                    while (rsEntity.next()) {
                        Map<String, Object> entity = new HashMap<>();
                        entity.put("nom", rsEntity.getString("nom"));
                        entity.put("position_x", rsEntity.getDouble("position_x"));
                        entity.put("position_y", rsEntity.getDouble("position_y"));
                        entity.put("attributs", new ArrayList<Map<String, Object>>());

                        try (PreparedStatement pstmtAttribute = connection.prepareStatement(selectAttributesSQL)) {
                            pstmtAttribute.setInt(1, rsEntity.getInt("id"));
                            pstmtAttribute.setString(2, schemaType);
                            try (ResultSet rsAttribute = pstmtAttribute.executeQuery()) {
                                while (rsAttribute.next()) {
                                    Map<String, Object> attribute = new HashMap<>();
                                    attribute.put("nom", rsAttribute.getString("nom"));
                                    attribute.put("cle_primaire", rsAttribute.getBoolean("cle_primaire"));
                                    attribute.put("cle_etrangere", rsAttribute.getBoolean("cle_etrangere"));
                                    ((List<Map<String, Object>>) entity.get("attributs")).add(attribute);
                                }
                            }
                        }
                        entities.add(entity);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entities;
    }

    public void updateEntity(Map<String, Object> entity, List<Map<String, Object>> attributes, String schemaType) {
        String updateEntitySQL = "UPDATE entites SET nom = ?, position_x = ?, position_y = ? WHERE type_schema = ? AND nom = ?";
        String deleteAttributesSQL = "DELETE FROM attributs WHERE entite_id = (SELECT id FROM entites WHERE nom = ? AND type_schema = ?)";
        String insertAttributeSQL = "INSERT INTO attributs (nom, cle_primaire, cle_etrangere, entite_id, type_schema) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = ConnexionBdd.getConnection()) {
            connection.setAutoCommit(false); // Démarrer une transaction

            // Mettre à jour l'entité
            try (PreparedStatement pstmt = connection.prepareStatement(updateEntitySQL)) {
                pstmt.setString(1, (String) entity.get("nom"));
                pstmt.setDouble(2, (double) entity.get("position_x"));
                pstmt.setDouble(3, (double) entity.get("position_y"));
                pstmt.setString(4, schemaType);
                pstmt.setString(5, (String) entity.get("nom")); // Nom de l'entité à mettre à jour
                pstmt.executeUpdate();
            }

            // Supprimer les attributs existants
            try (PreparedStatement pstmtDelete = connection.prepareStatement(deleteAttributesSQL)) {
                pstmtDelete.setString(1, (String) entity.get("nom"));
                pstmtDelete.setString(2, schemaType);
                pstmtDelete.executeUpdate();
            }

            // Insérer les nouveaux attributs
            try (PreparedStatement pstmtAttr = connection.prepareStatement(insertAttributeSQL)) {
                for (Map<String, Object> attribute : attributes) {
                    pstmtAttr.setString(1, (String) attribute.get("nom"));
                    pstmtAttr.setBoolean(2, (boolean) attribute.get("cle_primaire"));
                    pstmtAttr.setBoolean(3, (boolean) attribute.get("cle_etrangere"));
                    pstmtAttr.setInt(4, getEntityId(entity.get("nom"), schemaType, connection)); // Récupérer l'ID de l'entité
                    pstmtAttr.setString(5, schemaType);
                    pstmtAttr.addBatch();
                }
                pstmtAttr.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getEntityId(Object entityName, String schemaType, Connection connection) throws SQLException {
        String selectIdSQL = "SELECT id FROM entites WHERE nom = ? AND type_schema = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(selectIdSQL)) {
            pstmt.setString(1, (String) entityName);
            pstmt.setString(2, schemaType);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1; // Retourner -1 si l'entité n'est pas trouvée
    }
}
