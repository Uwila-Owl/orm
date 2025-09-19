import javafx.embed.swing.JFXPanel;
import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FenetreLogin extends JFrame {

    private static final Logger LOGGER = Logger.getLogger(FenetreLogin.class.getName());
    private JTextField idField;
    private JPasswordField passwordField;
    private ConnexionBdd connexionBdd;
    private JPanel connectionIndicator;
    private JLabel messageLabel;
    private LogDAO logDAO = new LogDAO();
    String userId = UserSession.getInstance().getUserId();

    public FenetreLogin() {
        super("Authentification");
        connexionBdd = new ConnexionBdd();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 420);
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

        // Lien "Mot de passe oublié ?"
        JLabel forgotPasswordLink = new JLabel("<html><a href=''>Mot de passe oublié ?</a></html>");
        forgotPasswordLink.setForeground(Color.BLUE);
        forgotPasswordLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotPasswordLink.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(forgotPasswordLink, gbc);

        forgotPasswordLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                FenetreLogin.this.setVisible(false);
                SwingUtilities.invokeLater(() -> new MdpOublie(FenetreLogin.this, connexionBdd));
            }
        });

        // Boutons OK, Créer, Annuler
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
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(okButton, gbc);

        gbc.gridx = 1;
        panel.add(createButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        panel.add(cancelButton, gbc);

        // Label message
        messageLabel = new JLabel(" ");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setForeground(Color.BLUE);
        GridBagConstraints gbcMessage = new GridBagConstraints();
        gbcMessage.gridx = 0;
        gbcMessage.gridy = 7;
        gbcMessage.gridwidth = 2;
        gbcMessage.insets = new Insets(5, 10, 10, 10);
        gbcMessage.fill = GridBagConstraints.HORIZONTAL;
        panel.add(messageLabel, gbcMessage);

        // Panel top avec lien "Nous contacter"
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(bgColor);

        JLabel contactLink = new JLabel("<html><a href=''>Nous contacter</a></html>");
        contactLink.setForeground(Color.BLUE);
        contactLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        contactLink.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

        topPanel.add(contactLink, BorderLayout.EAST);

        contactLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new Contact(FenetreLogin.this);
            }
        });

        // Remplacer setContentPane(panel) par un BorderLayout avec topPanel en haut
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(panel, BorderLayout.CENTER);

        // Indicateur connexion (inchangé)
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

        JLayeredPane layeredPane = getLayeredPane();
        layeredPane.add(connectionIndicator, JLayeredPane.PALETTE_LAYER);
        connectionIndicator.setLocation(5, 5);

        // Action commune pour OK et Entrée (inchangé)
        Action okAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String id = idField.getText().trim();
                String password = new String(passwordField.getPassword()).trim();

                if (id.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(FenetreLogin.this, "Veuillez remplir l'ID et le mot de passe.");
                    return;
                }

                AuthResult result = authentifierEtVerifierSupvis(id, password);
                if (result == null) {
                    JOptionPane.showMessageDialog(FenetreLogin.this, "Authentification échouée.");
                    return;
                }

                if (result.resetRequired) {
                    ouvrirFenetreChangementMotDePasse(id);
                    return;
                }

                messageLabel.setText("Authentification réussie! Fermeture automatique dans 3 secondes.");
                idField.setEnabled(false);
                passwordField.setEnabled(false);
                okButton.setEnabled(false);
                createButton.setEnabled(false);
                cancelButton.setEnabled(false);

                Timer timer = new Timer(3000, evt -> {
                    FenetreLogin.this.dispose();
                    SwingUtilities.invokeLater(() -> {
                        if (result.isSuperviseur) {
                            new Supervision(connexionBdd, id);
                        } else {
                            new Ventilation(FenetreLogin.this, connexionBdd, id);
                        }
                    });
                });
                timer.setRepeats(false);
                timer.start();
            }
        };

        okButton.addActionListener(okAction);
        idField.addActionListener(okAction);
        passwordField.addActionListener(okAction);

        createButton.addActionListener(e -> ouvrirFenetreCreation());
        cancelButton.addActionListener(e -> System.exit(0));

        setVisible(true);
    }

    // ===== Résultat authentification =====
    private class AuthResult {

        Boolean isSuperviseur;
        Boolean resetRequired;

        AuthResult(Boolean isSuperviseur, Boolean resetRequired) {
            this.isSuperviseur = isSuperviseur;
            this.resetRequired = resetRequired;
        }
    }

    private AuthResult authentifierEtVerifierSupvis(String id, String password) {
        try (Connection connection = connexionBdd.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT supvis, password_reset_required, nom, prenom, email "
                + "FROM utilisateurs "
                + "WHERE ID = ? AND Password = crypt(?, Password) AND active = TRUE")) {

            preparedStatement.setString(1, id);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // Stocker les informations utilisateur dans la session
                UserSession.getInstance().setUserInfo(
                        id,
                        resultSet.getString("nom"),
                        resultSet.getString("prenom"),
                        resultSet.getString("email"),
                        resultSet.getBoolean("supvis")
                );

                return new AuthResult(
                        resultSet.getBoolean("supvis"),
                        resultSet.getBoolean("password_reset_required")
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de l'authentification: " + e.getMessage(), "SEVERE");
            JOptionPane.showMessageDialog(this, "Erreur lors de la vérification de l'utilisateur.");
            return null;
        }
    }

    // ===== Fenêtre changement mot de passe =====
    private void ouvrirFenetreChangementMotDePasse(String userId) {
        JDialog dialog = new JDialog(this, "Changer votre mot de passe", true);
        dialog.setSize(400, 200);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel newPassLabel = new JLabel("Nouveau mot de passe :");
        JPasswordField newPassField = new JPasswordField(15);

        JLabel confirmPassLabel = new JLabel("Confirmer :");
        JPasswordField confirmPassField = new JPasswordField(15);

        JButton saveButton = new JButton("Enregistrer");

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(newPassLabel, gbc);
        gbc.gridx = 1;
        panel.add(newPassField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(confirmPassLabel, gbc);
        gbc.gridx = 1;
        panel.add(confirmPassField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        panel.add(saveButton, gbc);

        dialog.setContentPane(panel);

        saveButton.addActionListener(e -> {
            String pass1 = new String(newPassField.getPassword()).trim();
            String pass2 = new String(confirmPassField.getPassword()).trim();

            if (pass1.isEmpty() || pass2.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Veuillez remplir les deux champs.");
                return;
            }
            if (!pass1.equals(pass2)) {
                JOptionPane.showMessageDialog(dialog, "Les mots de passe ne correspondent pas.");
                return;
            }

            if (mettreAJourMotDePasse(userId, pass1)) {
                JOptionPane.showMessageDialog(dialog, "Mot de passe mis à jour. Veuillez vous reconnecter.");
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Erreur lors de la mise à jour du mot de passe.");
            }
        });

        dialog.setVisible(true);
    }

    private boolean mettreAJourMotDePasse(String id, String newPassword) {
        try (Connection connection = connexionBdd.getConnection(); PreparedStatement ps = connection.prepareStatement(
                "UPDATE utilisateurs SET Password = crypt(?, gen_salt('bf')), password_reset_required = FALSE WHERE ID = ?")) {

            ps.setString(1, newPassword);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la mise à jour du mot de passe: " + e.getMessage(), "SEVERE");
            return false;
        }
    }

    // ===== Fenêtre création utilisateur =====
    private void ouvrirFenetreCreation() {
        JDialog creationDialog = new JDialog(this, "Création d'utilisateur", true);
        creationDialog.setSize(450, 550); // Ajuster la taille pour les labels de validation
        creationDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#D6E3F3"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel idLabel = new JLabel("ID:");
        JTextField idField = new JTextField(15);

        JLabel nomLabel = new JLabel("Nom:");
        JTextField nomField = new JTextField(15);

        JLabel prenomLabel = new JLabel("Prénom:");
        JTextField prenomField = new JTextField(15);

        JLabel emailLabel = new JLabel("Email:");
        JTextField emailField = new JTextField(15);

        JLabel idDiscordLabel = new JLabel("ID Discord:");
        JTextField idDiscordField = new JTextField(15);

        JLabel passwordLabel = new JLabel("Mot de passe:");
        JPasswordField passwordField = new JPasswordField(15);

        // Labels de validation du mot de passe
        JLabel lengthLabel = new JLabel("<html>&#x274C; 12 caractères minimum</html>");
        JLabel uppercaseLabel = new JLabel("<html>&#x274C; 1 majuscule minimum</html>");
        JLabel digitLabel = new JLabel("<html>&#x274C; 1 chiffre minimum</html>");
        JLabel specialCharLabel = new JLabel("<html>&#x274C; 1 caractère spécial minimum (!@#$%^&*)</html>");

        lengthLabel.setForeground(Color.RED);
        uppercaseLabel.setForeground(Color.RED);
        digitLabel.setForeground(Color.RED);
        specialCharLabel.setForeground(Color.RED);

        // Listener pour la validation en temps réel du mot de passe
        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                validatePassword();
            }

            public void removeUpdate(DocumentEvent e) {
                validatePassword();
            }

            public void insertUpdate(DocumentEvent e) {
                validatePassword();
            }

            private void validatePassword() {
                String password = new String(passwordField.getPassword());

                // Longueur
                if (password.length() >= 12) {
                    lengthLabel.setText("<html>&#x2705; 12 caractères minimum</html>");
                    lengthLabel.setForeground(new Color(0, 100, 0));
                } else {
                    lengthLabel.setText("<html>&#x274C; 12 caractères minimum</html>");
                    lengthLabel.setForeground(Color.RED);
                }

                // Majuscule
                if (password.matches(".*[A-Z].*")) {
                    uppercaseLabel.setText("<html>&#x2705; 1 majuscule minimum</html>");
                    uppercaseLabel.setForeground(new Color(0, 100, 0));
                } else {
                    uppercaseLabel.setText("<html>&#x274C; 1 majuscule minimum</html>");
                    uppercaseLabel.setForeground(Color.RED);
                }

                // Chiffre
                if (password.matches(".*[0-9].*")) {
                    digitLabel.setText("<html>&#x2705; 1 chiffre minimum</html>");
                    digitLabel.setForeground(new Color(0, 100, 0));
                } else {
                    digitLabel.setText("<html>&#x274C; 1 chiffre minimum</html>");
                    digitLabel.setForeground(Color.RED);
                }

                // Caractère spécial
                if (password.matches(".*[!@#$%^&*].*")) {
                    specialCharLabel.setText("<html>&#x2705; 1 caractère spécial minimum (!@#$%^&*)</html>");
                    specialCharLabel.setForeground(new Color(0, 100, 0));
                } else {
                    specialCharLabel.setText("<html>&#x274C; 1 caractère spécial minimum (!@#$%^&*)</html>");
                    specialCharLabel.setForeground(Color.RED);
                }
            }
        });


        JButton createButton = new JButton("Créer");

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(idLabel, gbc);
        gbc.gridx = 1;
        panel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(nomLabel, gbc);
        gbc.gridx = 1;
        panel.add(nomField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(prenomLabel, gbc);
        gbc.gridx = 1;
        panel.add(prenomField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(emailLabel, gbc);
        gbc.gridx = 1;
        panel.add(emailField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(idDiscordLabel, gbc);
        gbc.gridx = 1;
        panel.add(idDiscordField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(passwordLabel, gbc);
        gbc.gridx = 1;
        panel.add(passwordField, gbc);

        // Ajout des labels de validation
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        panel.add(lengthLabel, gbc);

        gbc.gridy = 7;
        panel.add(uppercaseLabel, gbc);

        gbc.gridy = 8;
        panel.add(digitLabel, gbc);

        gbc.gridy = 9;
        panel.add(specialCharLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 10; // Nouvelle position pour le bouton Créer
        gbc.gridwidth = 2;
        panel.add(createButton, gbc);

        creationDialog.setContentPane(panel);

        createButton.addActionListener(e -> {
            String id = idField.getText().trim();
            String nom = nomField.getText().trim();
            String prenom = prenomField.getText().trim();
            String email = emailField.getText().trim();
            String idDiscord = idDiscordField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();

            if (id.isEmpty() || nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(creationDialog, "Veuillez remplir tous les champs obligatoires.");
                return;
            }

            if (!isValidLogin(id)) {
                JOptionPane.showMessageDialog(creationDialog, "Le login doit contenir uniquement des chiffres et faire entre 6 et 10 caractères.", "Erreur login", JOptionPane.ERROR_MESSAGE);
                return;
            }


            // Validation du mot de passe avant la création
            if (!isValidPassword(password)) {
                JOptionPane.showMessageDialog(creationDialog, "Le mot de passe ne respecte pas les critères de sécurité.", "Erreur de mot de passe", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!isValidEmail(email)) {
                JOptionPane.showMessageDialog(creationDialog, "Veuillez saisir une adresse email valide.", "Erreur email", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (creerUtilisateur(id, nom, prenom, email, idDiscord, password)) {
                JOptionPane.showMessageDialog(creationDialog, "Utilisateur créé avec succès!");
                creationDialog.dispose();
                this.idField.setText(id);
                this.passwordField.setText(password);
            } else {
                JOptionPane.showMessageDialog(creationDialog, "Erreur lors de la création de l'utilisateur.");
            }
        });

        creationDialog.setVisible(true);
    }

    // Nouvelle méthode pour valider le mot de passe
    private boolean isValidPassword(String password) {
        // 12 caractères minimum
        if (password.length() < 12) {
            return false;
        }
        // 1 majuscule minimum
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        // 1 chiffre minimum
        if (!password.matches(".*[0-9].*")) {
            return false;
        }
        // 1 caractère spécial minimum (!@#$%^&*)
        if (!password.matches(".*[!@#$%^&*].*")) {
            return false;
        }
        return true;
    }

    private boolean isValidLogin(String login) {
        // Regex : uniquement chiffres, longueur 6 à 10
        return login.matches("^\\d{6,10}$");
    }


    private boolean isValidEmail(String email) {
        // Expression régulière simple pour valider un email basique
        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }


    private boolean creerUtilisateur(String id, String nom, String prenom, String email, String idDiscord, String password) {
        try (Connection connection = connexionBdd.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(
                "INSERT INTO utilisateurs (ID, Password, active, supvis, date, nom, prenom, email, id_discord, password_reset_required) "
                + "VALUES (?, crypt(?, gen_salt('bf')), ?, ?, ?, ?, ?, ?, ?, ?)")) {

            preparedStatement.setString(1, id);
            preparedStatement.setString(2, password);
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
            preparedStatement.setBoolean(10, false); // par défaut pas de reset demandé

            return preparedStatement.executeUpdate() > 0;
        } catch (SQLException e) {
            logDAO.insertLog(userId, "Erreur lors de la création de l'utilisateur: " + e.getMessage(), "SEVERE");
            JOptionPane.showMessageDialog(this, "Erreur lors de la création de l'utilisateur: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        new JFXPanel();
        SwingUtilities.invokeLater(FenetreLogin::new);
    }
}
