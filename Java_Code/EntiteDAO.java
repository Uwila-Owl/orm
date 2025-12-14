
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntiteDAO {

    private static final Logger LOGGER = Logger.getLogger(EntiteDAO.class.getName());
    private AttributDAO attributDAO = new AttributDAO();
    private LogDAO logDAO = new LogDAO();
    String userId = UserSession.getInstance().getUserId();

    /**
     * Insère une nouvelle entité dans la table 'entites' et retourne l'ID
     * généré.
     */
    public int insertEntite(String nom, int positionX, int positionY, String typeSchema, int schemaId) {
        String SQL = "INSERT INTO entites(nom, position_x, position_y, type_schema, schema_id) VALUES(?, ?, ?, ?, ?) RETURNING id"; // Ajout de schema_id
        int id = -1;

        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nom);
            pstmt.setInt(2, positionX);
            pstmt.setInt(3, positionY);
            pstmt.setString(4, typeSchema);
            pstmt.setInt(5, schemaId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                    }
                }
                logDAO.insertLog(userId, "Insertion réussie dans la table 'entites' avec l'ID : " + id, "INFO");
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de l'insertion dans la table 'entites' : " + e.getMessage(), "SEVERE");
        }
        return id;
    }

    /**
     * Met à jour la position d'une entité existante dans la base de données.
     */
    public void updateEntitePosition(int entiteId, int positionX, int positionY) {
        String SQL = "UPDATE entites SET position_x = ?, position_y = ? WHERE id = ?";

        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, positionX);
            pstmt.setInt(2, positionY);
            pstmt.setInt(3, entiteId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logDAO.insertLog(userId, "Position mise à jour pour l'entité ID : " + entiteId, "INFO");
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la mise à jour de la position : " + e.getMessage(), "SEVERE");
        }
    }

    /**
     * Supprime une entité de la base de données.
     *
     * @param entiteId L'ID de l'entité à supprimer.
     */
    public void supprimerEntite(int entiteId) {
        String SQL = "DELETE FROM entites WHERE id = ?";

        try (Connection conn = ConnexionBdd.getConnection()) {
            conn.setAutoCommit(false); // Début de la transaction

            try (PreparedStatement pstmt = conn.prepareStatement(SQL)) {
                // Supprimer les attributs liés à cette entité
                attributDAO.supprimerAttributsEntite(entiteId); // Appel à la méthode de AttributDAO

                RelationDAO relationDAO = new RelationDAO();
                relationDAO.supprimerRelationsEntite(entiteId); // Supprimer les relations liées à cette entité

                pstmt.setInt(1, entiteId);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    logDAO.insertLog(userId, "Suppression réussie de l'entité avec l'ID : " + entiteId, "INFO");
                    conn.commit(); // Valider la transaction
                } else {
                    logDAO.insertLog(userId, "Aucune entité trouvée avec l'ID : " + entiteId, "WARNING");
                    conn.rollback(); // Annuler la transaction
                }
            } catch (SQLException e) {
                conn.rollback(); // Annuler la transaction en cas d'erreur
                throw e; // Re-lancer l'exception pour la gestion d'erreur supérieure
            } finally {
                conn.setAutoCommit(true); // Rétablir l'auto-commit
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la suppression de l'entité : " + e.getMessage(), "SEVERE");
        }
    }
}
