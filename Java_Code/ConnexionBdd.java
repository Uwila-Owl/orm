import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnexionBdd {
    private static final Logger LOGGER = Logger.getLogger(ConnexionBdd.class.getName());

    public static Connection getConnection() {
        try {
            Properties props = new Properties();
            InputStream input = null;
            
            // Méthode 1: Essayer de lire comme ressource depuis le classpath (JAR)
            try {
                input = ConnexionBdd.class.getClassLoader().getResourceAsStream("config.properties");
                if (input != null) {
                    LOGGER.info("Fichier config.properties trouvé comme ressource (JAR)");
                }
            } catch (Exception e) {
                LOGGER.fine("Impossible de lire config.properties comme ressource: " + e.getMessage());
            }
            
            // Méthode 2: Essayer de lire depuis le répertoire courant
            if (input == null) {
                try {
                    input = new FileInputStream("config.properties");
                    LOGGER.info("Fichier config.properties trouvé dans le répertoire courant");
                } catch (Exception e) {
                    LOGGER.fine("Impossible de lire config.properties depuis le répertoire courant: " + e.getMessage());
                }
            }
            
            // Méthode 3: Essayer de lire depuis le dossier Encryption
            if (input == null) {
                try {
                    input = new FileInputStream("Encryption/config.properties");
                    LOGGER.info("Fichier config.properties trouvé dans le dossier Encryption");
                } catch (Exception e) {
                    LOGGER.fine("Impossible de lire config.properties depuis Encryption/: " + e.getMessage());
                }
            }
            
            if (input == null) {
                throw new RuntimeException("Fichier config.properties introuvable dans tous les emplacements testés");
            }
            
            // Charger les propriétés
            try (InputStream configStream = input) {
                props.load(configStream);
            }

            String url = CryptoUtils.decrypt(props.getProperty("db.url"));
            String user = CryptoUtils.decrypt(props.getProperty("db.user"));
            String password = CryptoUtils.decrypt(props.getProperty("db.password"));

            Connection connection = DriverManager.getConnection(url, user, password);
            LOGGER.info("Connexion à la base de données réussie !");
            return connection;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Échec de la connexion à la base de données : " + e.getMessage(), e);
            return null;
        }
    }

    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("✅ Connexion réussie !");
            } else {
                System.out.println("❌ Échec de la connexion !");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}