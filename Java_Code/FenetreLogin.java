import javax.swing.*;  
import java.awt.*;  
import java.awt.event.ActionEvent;  
import java.awt.event.ActionListener;  
import java.sql.*;  
import java.util.logging.Level;  
import java.util.logging.Logger;  
import java.time.LocalDateTime;  
  
import javafx.application.Platform;  
import javafx.embed.swing.JFXPanel; // Import pour JFXPanel  
  
public class FenetreLogin extends JFrame {  
  
    private static final Logger LOGGER = Logger.getLogger(FenetreLogin.class.getName());  
    private JTextField idField;  
    private JPasswordField passwordField;  
    private ConnexionBdd connexionBdd; // Instance de votre classe de connexion  
  
    public FenetreLogin() {  
        super("Authentification");  
        connexionBdd = new ConnexionBdd(); // Initialisation de l'instance de ConnexionBdd  
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
        setSize(300, 200);  
        setLayout(new GridLayout(4, 2));  
        setLocationRelativeTo(null); // Centre la fenêtre  
  
        JLabel idLabel = new JLabel("ID:");  
        idField = new JTextField();  
  
        JLabel passwordLabel = new JLabel("Mot de passe:");  
        passwordField = new JPasswordField();  
  
        JButton okButton = new JButton("OK");  
        JButton createButton = new JButton("Créer");  
        JButton cancelButton = new JButton("Annuler");  
  
        add(idLabel);  
        add(idField);  
        add(passwordLabel);  
        add(passwordField);  
        add(okButton);  
        add(createButton);  
        add(cancelButton);  
  
  
        okButton.addActionListener(new ActionListener() {  
            @Override  
            public void actionPerformed(ActionEvent e) {  
                String id = idField.getText();  
                String password = new String(passwordField.getPassword());  
                if (authentifier(id, password)) {  
                    JOptionPane.showMessageDialog(FenetreLogin.this, "Authentification réussie!");  
                     // Lancer InterfaceGenerateurUML (JavaFX)  
                     FenetreLogin.this.dispose(); // Fermer la fenêtre de login  
                     // Lancer InterfaceGenerateurUML (JavaFX)  
                    SwingUtilities.invokeLater(() -> {  // Assurez-vous que cela s'exécute sur l'EDT Swing  
                        Platform.runLater(() -> { // Lance la partie JavaFX sur le thread JavaFX  
                            new InterfaceGenerateurUML().start(new javafx.stage.Stage());  
                        });  
                    });  
  
                } else {  
                    JOptionPane.showMessageDialog(FenetreLogin.this, "Authentification échouée.");  
                }  
            }  
        });  
  
        createButton.addActionListener(new ActionListener() {  
            @Override  
            public void actionPerformed(ActionEvent e) {  
                ouvrirFenetreCreation();  
            }  
        });  
  
        cancelButton.addActionListener(new ActionListener() {  
            @Override  
            public void actionPerformed(ActionEvent e) {  
                System.exit(0);  
            }  
        });  
  
        setVisible(true);  
    }  
  
    private boolean authentifier(String id, String password) {  
        try (Connection connection = connexionBdd.getConnection(); // Utiliser l'instance  
             PreparedStatement preparedStatement = connection.prepareStatement(  
                     "SELECT * FROM utilisateurs WHERE ID = ? AND Password = ? AND active = TRUE")) {  
            preparedStatement.setString(1, id);  
            preparedStatement.setString(2, password);  
            ResultSet resultSet = preparedStatement.executeQuery();  
            return resultSet.next(); // Retourne true si une correspondance est trouvée  
        } catch (SQLException e) {  
            LOGGER.log(Level.SEVERE, "Erreur lors de l'authentification: " + e.getMessage(), e);  
            JOptionPane.showMessageDialog(this, "Erreur lors de la vérification de l'utilisateur.");  
            return false;  
        }  
    }  
  
    private void ouvrirFenetreCreation() {  
        JDialog creationDialog = new JDialog(this, "Création d'utilisateur", true);  
        creationDialog.setLayout(new GridLayout(3, 2));  
        creationDialog.setSize(300, 150);  
        creationDialog.setLocationRelativeTo(this);  
  
        JLabel newIdLabel = new JLabel("Nouvel ID:");  
        JTextField newIdField = new JTextField();  
  
        JLabel newPasswordLabel = new JLabel("Nouveau mot de passe:");  
        JPasswordField newPasswordField = new JPasswordField();  
  
        JButton createButton = new JButton("Créer");  
  
        creationDialog.add(newIdLabel);  
        creationDialog.add(newIdField);  
        creationDialog.add(newPasswordLabel);  
        creationDialog.add(newPasswordField);  
        creationDialog.add(new JLabel()); // Espace  
        creationDialog.add(createButton);  
  
        createButton.addActionListener(new ActionListener() {  
            @Override  
            public void actionPerformed(ActionEvent e) {  
                String newId = newIdField.getText();  
                String newPassword = new String(newPasswordField.getPassword());  
                if (creerUtilisateur(newId, newPassword)) {  
                    JOptionPane.showMessageDialog(creationDialog, "Utilisateur créé avec succès!");  
                    creationDialog.dispose(); // Ferme la fenêtre de création  
                    idField.setText(newId);  // Preremplir les champs de la fenêtre principale  
                    passwordField.setText(newPassword);  
  
                } else {  
                    JOptionPane.showMessageDialog(creationDialog, "Erreur lors de la création de l'utilisateur.");  
                }  
            }  
        });  
  
        creationDialog.setVisible(true);  
    }  
  
     private boolean creerUtilisateur(String id, String password) {  
            try (Connection connection = connexionBdd.getConnection();  
                 PreparedStatement preparedStatement = connection.prepareStatement(  
                         "INSERT INTO utilisateurs (ID, Password, active, supvis, date) VALUES (?, ?, ?, ?, ?)")) {  
                preparedStatement.setString(1, id);  
                preparedStatement.setString(2, password);  
                preparedStatement.setBoolean(3, true);  // active = true  
                preparedStatement.setBoolean(4, false); // supvis = false (par défaut)  
                preparedStatement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now())); // date et heure actuelle  
  
                int rowsAffected = preparedStatement.executeUpdate();  
                return rowsAffected > 0; // Retourne true si l'insertion a réussi  
            } catch (SQLException e) {  
                LOGGER.log(Level.SEVERE, "Erreur lors de la création de l'utilisateur: " + e.getMessage(), e);  
                JOptionPane.showMessageDialog(this, "Erreur lors de la création de l'utilisateur: " + e.getMessage()); // Affiche l'erreur  
                return false;  
            }  
        }  
  
  
    public static void main(String[] args) {  
        // Initialize JavaFX Toolkit (required for Platform.runLater)  
        new JFXPanel(); // Initialize the JavaFX toolkit.  
        SwingUtilities.invokeLater(new Runnable() {  
            public void run() {  
                new FenetreLogin();  
            }  
        });  
    }  
}  
