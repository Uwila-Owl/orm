
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AttributDAO {

    private static final Logger LOGGER = Logger.getLogger(AttributDAO.class.getName());
    private LogDAO logDAO = new LogDAO();
    String userId = UserSession.getInstance().getUserId();

    /**
     * Insère un nouvel attribut dans la table 'attributs'.
     */
    public void insertAttribut(String nom, boolean clePrimaire, boolean cleEtrangere, int entiteId, String typeSchema) {
        String SQL = "INSERT INTO attributs(nom, cle_primaire, cle_etrangere, entite_id, type_schema) VALUES(?, ?, ?, ?, ?)";

        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setString(1, nom);
            pstmt.setBoolean(2, clePrimaire);
            pstmt.setBoolean(3, cleEtrangere);
            pstmt.setInt(4, entiteId);
            pstmt.setString(5, typeSchema);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logDAO.insertLog(userId, "Insertion réussie dans la table 'attributs'.", "INFO");
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de l'insertion dans la table 'attributs' : " + e.getMessage(), "SEVERE");
        }
    }

    /**
     * Supprime tous les attributs associés à une entité spécifique.
     *
     * @param entiteId L'ID de l'entité dont on veut supprimer les attributs.
     */
    public void supprimerAttributsEntite(int entiteId) {
        String SQL = "DELETE FROM attributs WHERE entite_id = ?";

        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, entiteId);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logDAO.insertLog(userId, "Suppression réussie de " + affectedRows + " attribut(s) pour l'entité ID : " + entiteId, "INFO");
            } else {
                logDAO.insertLog(userId, "Aucun attribut trouvé pour l'entité ID : " + entiteId, "INFO");
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la suppression des attributs : " + e.getMessage(), "SEVERE");
        }
    }

    /**
     * Met à jour tous les attributs d'une entité en supprimant les anciens et
     * insérant les nouveaux.
     */
    public void updateAttributsForEntite(int entiteId, List<Map<String, Object>> attributs, String typeSchema) {
        try (Connection conn = ConnexionBdd.getConnection()) {
            conn.setAutoCommit(false); // Démarrer une transaction

            try {
                // 1. Supprimer tous les attributs existants de cette entité
                deleteAttributsByEntiteId(entiteId, typeSchema, conn);

                // 2. Insérer les nouveaux attributs
                String insertSQL = "INSERT INTO attributs(nom, cle_primaire, cle_etrangere, entite_id, type_schema) VALUES(?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    for (Map<String, Object> attribut : attributs) {
                        pstmt.setString(1, (String) attribut.get("nom"));
                        pstmt.setBoolean(2, (boolean) attribut.getOrDefault("cle_primaire", false));
                        pstmt.setBoolean(3, (boolean) attribut.getOrDefault("cle_etrangere", false));
                        pstmt.setInt(4, entiteId);
                        pstmt.setString(5, typeSchema);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }

                conn.commit(); // Valider la transaction
                logDAO.insertLog(userId, "Mise à jour réussie des attributs pour l'entité ID : " + entiteId, "INFO");

            } catch (SQLException e) {
                conn.rollback(); // Annuler en cas d'erreur
                throw e;
            } finally {
                conn.setAutoCommit(true); // Restaurer le mode auto-commit
            }

        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la mise à jour des attributs : " + e.getMessage(), "SEVERE");
        }
    }

    /**
     * Méthode utilitaire pour supprimer les attributs (utilisée en interne).
     * Version privée qui utilise une connexion existante.
     */
    private void deleteAttributsByEntiteId(int entiteId, String typeSchema, Connection conn) throws SQLException {
        String SQL = "DELETE FROM attributs WHERE entite_id = ? AND type_schema = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setInt(1, entiteId);
            pstmt.setString(2, typeSchema);
            pstmt.executeUpdate();
        }
    }
}
