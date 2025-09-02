
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

public class FenetreLogin extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(FenetreLogin.class.getName());
    private JTextField idField;
    private JPasswordField passwordField;
    private ConnexionBdd connexionBdd;
    private JPanel connectionIndicator; // indicateur lumineux

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

        // Panel principal avec GridBagLayout
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(bgColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label ID
        JLabel idLabel = new JLabel("ID:");
        idLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(idLabel, gbc);

        // Champ ID
        idField = new JTextField(15);
        gbc.gridy = 1;
        panel.add(idField, gbc);

        // Label mot de passe
        JLabel passwordLabel = new JLabel("Mot de passe:");
        passwordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 2;
        panel.add(passwordLabel, gbc);

        // Champ mot de passe
        passwordField = new JPasswordField(15);
        gbc.gridy = 3;
        panel.add(passwordField, gbc);

        // Boutons
        JButton okButton = new JButton("OK");
        JButton createButton = new JButton("Créer");
        JButton cancelButton = new JButton("Annuler");

        JButton[] buttons = {okButton, createButton, cancelButton};
        for (JButton b : buttons) {
            b.setBackground(btnColor);
            b.setForeground(textColor);
            b.setFocusPainted(false);
            b.setFont(new Font("Arial", Font.BOLD, 14));
        }

        gbc.gridwidth = 1;
        gbc.gridy = 4;
        gbc.gridx = 0;
        panel.add(okButton, gbc);

        gbc.gridx = 1;
        panel.add(createButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        panel.add(cancelButton, gbc);

        // Ajout du panel principal à la JFrame
        setContentPane(panel);

        // Création de l'indicateur lumineux (20x20 px)
        connectionIndicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(getBackground());
                g.fillOval(0, 0, getWidth(), getHeight());
            }
        };
        connectionIndicator.setSize(20, 20);
        connectionIndicator.setOpaque(false);

        // Tester la connexion et définir la couleur
        try (Connection testConn = connexionBdd.getConnection()) {
            if (testConn != null && !testConn.isClosed()) {
                connectionIndicator.setBackground(Color.GREEN);
            } else {
                connectionIndicator.setBackground(Color.RED);
            }
        } catch (SQLException e) {
            connectionIndicator.setBackground(Color.RED);
        }

        // Ajouter l'indicateur dans la couche supérieure (layered pane)
        JLayeredPane layeredPane = getLayeredPane();
        layeredPane.add(connectionIndicator, JLayeredPane.PALETTE_LAYER);
        connectionIndicator.setLocation(5, 5); // position en haut à gauche avec un petit décalage

        // Action commune pour OK et Entrée
        Action okAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = idField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                if (id.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(FenetreLogin.this, "Veuillez remplir l'ID et le mot de passe.");
                    return;
                }

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
            }
        };

        // Associer action au bouton et aux champs
        okButton.addActionListener(okAction);
        idField.addActionListener(okAction);
        passwordField.addActionListener(okAction);

        // Actions autres boutons
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
            String newId = newIdField.getText().trim();
            String newPassword = new String(newPasswordField.getPassword()).trim();
            if (newId.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(creationDialog, "Veuillez remplir l'ID et le mot de passe.");
                return;
            }
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
        new JFXPanel(); // Initialisation JavaFX
        SwingUtilities.invokeLater(FenetreLogin::new);
    }
}
