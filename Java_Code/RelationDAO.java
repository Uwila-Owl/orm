
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RelationDAO {

    private static final Logger LOGGER = Logger.getLogger(RelationDAO.class.getName());
    private LogDAO logDAO = new LogDAO();
    String userId = UserSession.getInstance().getUserId();

    public List<Map<String, Object>> getAllRelations() {
        List<Map<String, Object>> relations = new ArrayList<>();
        String SQL = "SELECT * FROM relations";
        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL); ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> relation = new HashMap<>();
                relation.put("id", rs.getInt("id"));
                relation.put("nom", rs.getString("nom"));
                relation.put("entite_source_id", rs.getInt("entite_source_id"));
                relation.put("entite_cible_id", rs.getInt("entite_cible_id"));
                relation.put("cardinalite_source", rs.getString("cardinalite_source"));
                relation.put("cardinalite_cible", rs.getString("cardinalite_cible"));
                relation.put("type_schema", rs.getString("type_schema"));
                relations.add(relation);
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la récupération des relations : " + e.getMessage(), "SEVERE");
        }
        return relations;
    }

    /**
     * Insère une nouvelle relation et retourne l'ID généré.
     */
    public int insertRelation(String nom, int entiteSourceId, int entiteCibleId,
            String cardinaliteSource, String cardinaliteCible,
            String typeSchema) {
        String SQL = "INSERT INTO relations(nom, entite_source_id, entite_cible_id, cardinalite_source, cardinalite_cible, type_schema) "
                + "VALUES(?, ?, ?, ?, ?, ?)";
        int id = -1;

        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nom);
            pstmt.setInt(2, entiteSourceId);
            pstmt.setInt(3, entiteCibleId);
            pstmt.setString(4, cardinaliteSource);
            pstmt.setString(5, cardinaliteCible);
            pstmt.setString(6, typeSchema);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                    }
                }
                logDAO.insertLog(userId, "Insertion réussie dans la table 'relations' avec l'ID : " + id, "INFO");
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de l'insertion dans la table 'relations' : " + e.getMessage(), "SEVERE");
        }
        return id;
    }

    /**
     * Met à jour une relation.
     */
    public void updateRelation(int relationId, String nom,
            String cardinaliteSource, String cardinaliteCible) {
        String SQL = "UPDATE relations SET nom = ?, cardinalite_source = ?, cardinalite_cible = ? WHERE id = ?";

        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setString(1, nom);
            pstmt.setString(2, cardinaliteSource);
            pstmt.setString(3, cardinaliteCible);
            pstmt.setInt(4, relationId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logDAO.insertLog(userId, "Mise à jour réussie de la relation ID : " + relationId, "INFO");
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la mise à jour de la relation : " + e.getMessage(), "SEVERE");
        }
    }

    /**
     * Supprime une relation.
     */
    public void supprimerRelation(int relationId) {
        String SQL = "DELETE FROM relations WHERE id = ?";

        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, relationId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logDAO.insertLog(userId, "Suppression réussie de la relation avec l'ID : " + relationId, "INFO");
            } else {
                logDAO.insertLog(userId, "Aucune relation trouvée avec l'ID : " + relationId, "WARNING");
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la suppression de la relation : " + e.getMessage(), "SEVERE");
        }
    }

    /**
     * Supprime toutes les relations associées à un schéma donné.
     *
     * @param schemaId L'ID du schéma dont les relations doivent être
     * supprimées.
     */
    public void supprimerRelationsParSchema(int schemaId) {
        String SQL = "DELETE FROM relations WHERE entite_source_id IN (SELECT id FROM entites WHERE schema_id = ?) OR entite_cible_id IN (SELECT id FROM entites WHERE schema_id = ?)";
        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, schemaId);
            pstmt.setInt(2, schemaId);
            int affectedRows = pstmt.executeUpdate();
            logDAO.insertLog(userId, "Suppression de " + affectedRows + " relations pour le schéma ID : " + schemaId, "INFO");
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la suppression des relations par schéma : " + e.getMessage(), "SEVERE");
        }
    }

    public void supprimerRelationsEntite(int entiteId) {
        String SQL = "DELETE FROM relations WHERE entite_source_id = ? OR entite_cible_id = ?";
        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, entiteId);
            pstmt.setInt(2, entiteId);
            int affectedRows = pstmt.executeUpdate();
            logDAO.insertLog(userId, "Suppression de " + affectedRows + " relation(s) pour l'entité ID : " + entiteId, "INFO");
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la suppression des relations par entité : " + e.getMessage(), "SEVERE");
        }
    }

    //--AjoutLyna
    /**
     * Met à jour seulement les cardinalités d'une relation
     */
    public void updateCardinalites(int relationId, String cardSource, String cardCible) {
        String SQL = "UPDATE relations SET cardinalite_source = ?, cardinalite_cible = ? WHERE id = ?";
        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setString(1, cardSource);
            pstmt.setString(2, cardCible);
            pstmt.setInt(3, relationId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                logDAO.insertLog(userId, "Mise à jour des cardinalités de la relation ID : " + relationId, "INFO");
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la mise à jour des cardinalités : " + e.getMessage(), "SEVERE");
        }
    }
//

    /**
     * Récupère une relation à partir de l'ID de l'entité source.
     */
    public Map<String, Object> getRelationBySourceId(int entiteSourceId) {
        String SQL = "SELECT * FROM relations WHERE entite_source_id = ?";
        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, entiteSourceId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> relation = new HashMap<>();
                    relation.put("id", rs.getInt("id"));
                    relation.put("nom", rs.getString("nom"));
                    relation.put("entite_source_id", rs.getInt("entite_source_id"));
                    relation.put("entite_cible_id", rs.getInt("entite_cible_id"));
                    relation.put("cardinalite_source", rs.getString("cardinalite_source"));
                    relation.put("cardinalite_cible", rs.getString("cardinalite_cible"));
                    relation.put("type_schema", rs.getString("type_schema"));
                    return relation;
                }
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la récupération de la relation : " + e.getMessage(), "SEVERE");
        }
        return null;
    }
}
