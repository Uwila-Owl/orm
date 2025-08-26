import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntiteDAO {
    private static final Logger LOGGER = Logger.getLogger(EntiteDAO.class.getName());

    /**
     * Insère une nouvelle entité dans la table 'entites' et retourne l'ID généré.
     */
    public int insertEntite(String nom, int positionX, int positionY, String typeSchema) {
        String SQL = "INSERT INTO entites(nom, position_x, position_y, type_schema) VALUES(?, ?, ?, ?)";
        int id = -1;

        try (Connection conn = ConnexionBdd.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, nom);
            pstmt.setInt(2, positionX);
            pstmt.setInt(3, positionY);
            pstmt.setString(4, typeSchema);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getInt(1);
                    }
                }
                LOGGER.info("Insertion réussie dans la table 'entites' avec l'ID : " + id);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'insertion dans la table 'entites' : " + e.getMessage(), e);
        }
        return id;
    }

    /**
     * Met à jour la position d'une entité existante dans la base de données.
     */
    public void updateEntitePosition(int entiteId, int positionX, int positionY) {
        String SQL = "UPDATE entites SET position_x = ?, position_y = ? WHERE id = ?";

        try (Connection conn = ConnexionBdd.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, positionX);
            pstmt.setInt(2, positionY);
            pstmt.setInt(3, entiteId);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                LOGGER.info("Position mise à jour pour l'entité ID : " + entiteId);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la mise à jour de la position : " + e.getMessage(), e);
        }
    }

    /**
     * Supprime une entité de la base de données.
     *
     * @param entiteId L'ID de l'entité à supprimer.
     */
    public void supprimerEntite(int entiteId) {
        String SQL = "DELETE FROM entites WHERE id = ?";

        try (Connection conn = ConnexionBdd.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, entiteId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                LOGGER.info("Suppression réussie de l'entité avec l'ID : " + entiteId);
            } else {
                LOGGER.warning("Aucune entité trouvée avec l'ID : " + entiteId);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression de l'entité : " + e.getMessage(), e);
        }
    }
}
