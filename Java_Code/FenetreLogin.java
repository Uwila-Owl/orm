
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
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FenetreLogin extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(FenetreLogin.class.getName());
    private JTextField idField;
    private JPasswordField passwordField;
    private ConnexionBdd connexionBdd;
    private JPanel connectionIndicator; // indicateur lumineux
    private JLabel messageLabel; // label pour afficher messages

    public FenetreLogin() {
        super("Authentification");
        connexionBdd = new ConnexionBdd();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 380);
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

        // Lien "Mot de passe oublié ?"
        JLabel forgotPasswordLabel = new JLabel("<HTML><U>Mot de passe oublié ?</U></HTML>");
        forgotPasswordLabel.setForeground(Color.BLUE);
        forgotPasswordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        forgotPasswordLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        GridBagConstraints gbcForgot = new GridBagConstraints();
        gbcForgot.gridx = 0;
        gbcForgot.gridy = 7;
        gbcForgot.gridwidth = 2;
        gbcForgot.insets = new Insets(5, 10, 10, 10);
        gbcForgot.fill = GridBagConstraints.HORIZONTAL;
        panel.add(forgotPasswordLabel, gbcForgot);

        forgotPasswordLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // Ouvre la fenêtre MdpOublie
                SwingUtilities.invokeLater(() -> {
                    new MdpOublie(FenetreLogin.this, connexionBdd);
                });
            }
        });

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

        // Label message (vide au départ)
        messageLabel = new JLabel(" ");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setForeground(Color.BLUE);
        GridBagConstraints gbcMessage = new GridBagConstraints();
        gbcMessage.gridx = 0;
        gbcMessage.gridy = 6;
        gbcMessage.gridwidth = 2;
        gbcMessage.insets = new Insets(5, 10, 10, 10);
        gbcMessage.fill = GridBagConstraints.HORIZONTAL;
        panel.add(messageLabel, gbcMessage);

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

        // Tester la connexion et définir la couleur et tooltip
        try (Connection testConn = connexionBdd.getConnection()) {
            if (testConn != null && !testConn.isClosed()) {
                connectionIndicator.setBackground(Color.GREEN);
                connectionIndicator.setToolTipText("Connexion à la Base de donnée : ok");
            } else {
                connectionIndicator.setBackground(Color.RED);
                connectionIndicator.setToolTipText("Connexion à la Base de donnée : ko");
            }
        } catch (SQLException e) {
            connectionIndicator.setBackground(Color.RED);
            connectionIndicator.setToolTipText("Connexion à la Base de donnée : ko");
        }

        // Ajouter l'indicateur dans la couche supérieure (layered pane)
        JLayeredPane layeredPane = getLayeredPane();
        layeredPane.add(connectionIndicator, JLayeredPane.PALETTE_LAYER);
        connectionIndicator.setLocation(5, 5); // position en haut à gauche avec un petit décalage

        // === Ajout du lien "Nous Contacter" en haut à droite ===
        final int margin = 10;
        JLabel contactLabel = new JLabel("<HTML><U>Nous Contacter</U></HTML>");
        contactLabel.setForeground(Color.BLUE);
        contactLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        layeredPane.add(contactLabel, JLayeredPane.PALETTE_LAYER);

// fonction pour bien placer le label
        Runnable placeContact = () -> {
            Dimension pref = contactLabel.getPreferredSize();
            contactLabel.setSize(pref); // indispensable pour éviter le troncage
            int x = layeredPane.getWidth() - pref.width - margin; // largeur réelle du layeredPane
            int y = margin;
            contactLabel.setLocation(Math.max(margin, x), y);
        };

// position initiale après rendu
        SwingUtilities.invokeLater(placeContact);

// repositionnement dynamique si la fenêtre est redimensionnée
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                placeContact.run();
            }
        });

// Action au clic → ouverture de Contact.java
        contactLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    new Contact(FenetreLogin.this); // adapte si ton constructeur diffère
                });
            }
        });

        // Action au clic pour ouvrir la fenêtre Contact
        contactLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    new Contact(FenetreLogin.this);
                    // ou new Contact() si ton constructeur n'attend pas de paramètre
                });
            }
        });

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
                    messageLabel.setText("Authentification réussie! Fermeture automatique dans 5 secondes.");
                    idField.setEnabled(false);
                    passwordField.setEnabled(false);
                    okButton.setEnabled(false);
                    createButton.setEnabled(false);
                    cancelButton.setEnabled(false);

                    Timer timer = new Timer(5000, evt -> {
                        FenetreLogin.this.dispose();
                        SwingUtilities.invokeLater(() -> {
                            Platform.runLater(() -> {
                                new InterfaceGenerateurUML().start(new javafx.stage.Stage());
                            });
                        });
                    });
                    timer.setRepeats(false);
                    timer.start();

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
        creationDialog.setSize(400, 350);
        creationDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#D6E3F3"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Labels et champs
        JLabel idLabel = new JLabel("ID:");
        JTextField idField = new JTextField(15);

        // Icône avec infobulle
        Icon questionIcon = UIManager.getIcon("OptionPane.questionIcon");
        JLabel infoIcon = new JLabel(questionIcon);
        infoIcon.setToolTipText("L'ID doit être le numéro étudiant");
        infoIcon.setForeground(Color.BLUE);
        infoIcon.setFont(new Font("Arial", Font.BOLD, 16));

        JLabel nomLabel = new JLabel("Nom:");
        JTextField nomField = new JTextField(15);

        JLabel prenomLabel = new JLabel("Prénom:");
        JTextField prenomField = new JTextField(15);

        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField(15);

        JLabel idDiscordLabel = new JLabel("ID Discord:");
        JTextField idDiscordField = new JTextField(15);

        JButton createButton = new JButton("Créer");

        // Positionnement avec GridBagLayout
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        panel.add(idLabel, gbc);

        gbc.gridx = 1;
        panel.add(idField, gbc);

        gbc.gridx = 2;
        panel.add(infoIcon, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(nomLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(nomField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        panel.add(prenomLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(prenomField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        panel.add(emailLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        panel.add(idDiscordLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(idDiscordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(createButton, gbc);

        creationDialog.setContentPane(panel);

        createButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String idDiscord = idDiscordField.getText().trim();

            // Validation des champs obligatoires
            if (id.isEmpty() || nom.isEmpty() || prenom.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(creationDialog, "Veuillez remplir tous les champs obligatoires (ID, Nom, Prénom, Email).");
                return;
            }

            if (creerUtilisateur(id, nom, prenom, email, idDiscord)) {
                JOptionPane.showMessageDialog(creationDialog, "Utilisateur créé avec succès!");
                creationDialog.dispose();
                idField.setText(id);
                this.idField.setText(id);
                this.passwordField.setText(""); // mot de passe non modifié ici
            } else {
                JOptionPane.showMessageDialog(creationDialog, "Erreur lors de la création de l'utilisateur.");
            }
        });

        creationDialog.setVisible(true);
    }

    private boolean creerUtilisateur(String id, String nom, String prenom, String email, String idDiscord) {
        // Ici on génère un mot de passe temporaire ou on peut demander un mot de passe dans la création (à adapter)
        String passwordTemporaire = "changeme"; // ou générer un mot de passe aléatoire

        try (Connection connection = connexionBdd.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO utilisateurs (ID, Password, active, supvis, date, nom, prenom, email, id_discord) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, passwordTemporaire);
            preparedStatement.setBoolean(3, true);
            preparedStatement.setBoolean(4, false);
            preparedStatement.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            preparedStatement.setString(6, nom);
            preparedStatement.setString(7, prenom);
            preparedStatement.setString(8, email);
            if (idDiscord.isEmpty()) {
                preparedStatement.setNull(9, java.sql.Types.VARCHAR);
            } else {
                preparedStatement.setString(9, idDiscord);
            }

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
