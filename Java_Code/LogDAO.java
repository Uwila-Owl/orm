
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogDAO {

    private static final Logger LOGGER = Logger.getLogger(LogDAO.class.getName());

    /**
     * Insère un message de log dans la table 'logs'.
     *
     * @param userId L'ID de l'utilisateur associé au log. Peut être null pour
     * les logs système.
     * @param message Le message de log.
     * @param logLevel Le niveau de log (e.g., "INFO", "SEVERE", "WARNING").
     */
    public void insertLog(String userId, String message, String logLevel) {
        String SQL = "INSERT INTO logs(id_user, message_log, date_log, log_level) VALUES(?, ?, ?, ?)";

        try (Connection conn = ConnexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            if (userId != null) {
                pstmt.setString(1, userId);
            } else {
                pstmt.setNull(1, java.sql.Types.VARCHAR);
            }
            pstmt.setString(2, message);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(4, logLevel);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Log l'erreur sur la console car la base de données pourrait être inaccessible
            LOGGER.log(Level.SEVERE, "Erreur lors de l'insertion du log en base de données : " + e.getMessage(), e);
        }
    }
}
