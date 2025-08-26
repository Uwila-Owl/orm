import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnexionBdd {
    private static final String URL = "jdbc:postgresql://37.187.123.39:5432/GenerateurUML";
    private static final String USER = "ied_orm";
    private static final String PASSWORD = "i3d_0rm2025";
    private static final Logger LOGGER = Logger.getLogger(ConnexionBdd.class.getName());

    public static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
            LOGGER.info("Connexion à la base de données réussie !");
            return connection;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Échec de la connexion à la base de données : " + e.getMessage(), e);
            return null;
        }
    }
}
