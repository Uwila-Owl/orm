
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

public class FenetreLogin extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(FenetreLogin.class.getName());
    private JTextField idField;
    private JPasswordField passwordField;
    private ConnexionBdd connexionBdd;

    public FenetreLogin() {
        super("Authentification");
        connexionBdd = new ConnexionBdd();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);

        // Couleurs
        Color bgColor = Color.decode("#D6E3F3");
        Color btnColor = Color.decode("#3E5871");
        Color textColor = Color.decode("#EAECEE");

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(bgColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Espacement
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel idLabel = new JLabel("ID:");
        idLabel.setHorizontalAlignment(SwingConstants.CENTER);
        idField = new JTextField(15);

        JLabel passwordLabel = new JLabel("Mot de passe:");
        passwordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        passwordField = new JPasswordField(15);

        JButton okButton = new JButton("OK");
        JButton createButton = new JButton("Créer");
        JButton cancelButton = new JButton("Annuler");

        // Style des boutons
        JButton[] buttons = {okButton, createButton, cancelButton};
        for (JButton b : buttons) {
            b.setBackground(btnColor);
            b.setForeground(textColor);
            b.setFocusPainted(false);
            b.setFont(new Font("Arial", Font.BOLD, 14));
        }

        // Placement des composants
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(idLabel, gbc);

        gbc.gridy = 1;
        panel.add(idField, gbc);

        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        gbc.gridy = 3;
        panel.add(passwordField, gbc);

        gbc.gridy = 4;
        gbc.gridwidth = 1;
        panel.add(okButton, gbc);

        gbc.gridx = 1;
        panel.add(createButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(cancelButton, gbc);

        add(panel);

        // Actions
        okButton.addActionListener(e -> {
            String id = idField.getText();
            String password = new String(passwordField.getPassword());
            if (authentifier(id, password)) {
                JOptionPane.showMessageDialog(FenetreLogin.this, "Authentification réussie!");
                FenetreLogin.this.dispose();
                SwingUtilities.invokeLater(() -> {
                    Platform.runLater(() -> {
                        new InterfaceGenerateurUML().start(new javafx.stage.Stage());
                    });
                });
            } else {
                JOptionPane.showMessageDialog(FenetreLogin.this, "Authentification échouée.");
            }
        });

        createButton.addActionListener(e -> ouvrirFenetreCreation());
        cancelButton.addActionListener(e -> System.exit(0));

        setVisible(true);
    }

    private boolean authentifier(String id, String password) {
        try (Connection connection = connexionBdd.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT * FROM utilisateurs WHERE ID = ? AND Password = ? AND active = TRUE")) {
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'authentification: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Erreur lors de la vérification de l'utilisateur.");
            return false;
        }
    }

    private void ouvrirFenetreCreation() {
        JDialog creationDialog = new JDialog(this, "Création d'utilisateur", true);
        creationDialog.setLayout(new GridLayout(3, 2, 10, 10));
        creationDialog.setSize(350, 180);
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
        creationDialog.add(new JLabel());
        creationDialog.add(createButton);

        createButton.addActionListener(e -> {
            String newId = newIdField.getText();
            String newPassword = new String(newPasswordField.getPassword());
            if (creerUtilisateur(newId, newPassword)) {
                JOptionPane.showMessageDialog(creationDialog, "Utilisateur créé avec succès!");
                creationDialog.dispose();
                idField.setText(newId);
                passwordField.setText(newPassword);
            } else {
                JOptionPane.showMessageDialog(creationDialog, "Erreur lors de la création de l'utilisateur.");
            }
        });

        creationDialog.setVisible(true);
    }

    private boolean creerUtilisateur(String id, String password) {
        try (Connection connection = connexionBdd.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO utilisateurs (ID, Password, active, supvis, date) VALUES (?, ?, ?, ?, ?)")) {
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, password);
            preparedStatement.setBoolean(3, true);
            preparedStatement.setBoolean(4, false);
            preparedStatement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            int rowsAffected = preparedStatement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la création de l'utilisateur: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(this, "Erreur lors de la création de l'utilisateur: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        new JFXPanel();
        SwingUtilities.invokeLater(FenetreLogin::new);
    }
}
