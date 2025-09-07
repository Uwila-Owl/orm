import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.*;

public class MdpOublie extends JDialog {

    private ConnexionBdd connexionBdd;
    private JFrame parent;

    // Couleurs et polices (identiques à FenetreLogin)
    private final Color bgColor = Color.decode("#D6E3F3");
    private final Color btnColor = Color.decode("#3E5871");
    private final Color textColor = Color.decode("#EAECEE");
    private final Font labelFont = new Font("Arial", Font.BOLD, 14);
    private final Font buttonFont = new Font("Arial", Font.BOLD, 14);

    public MdpOublie(JFrame parent, ConnexionBdd connexionBdd) {
        super(parent, "Mot de passe oublié", true);
        this.parent = parent;
        this.connexionBdd = connexionBdd;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(500, 250);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(bgColor);

        JLabel questionLabel = new JLabel("Que souhaitez-vous faire ?");
        questionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        questionLabel.setFont(labelFont);
        questionLabel.setForeground(btnColor);
        questionLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        mainPanel.add(questionLabel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(bgColor);

        JButton knowIdButton = new JButton("Je connais mon ID");
        JButton dontKnowIdButton = new JButton("Je ne connais pas mon ID");

        JButton[] buttons = {knowIdButton, dontKnowIdButton};
        for (JButton b : buttons) {
            b.setBackground(btnColor);
            b.setForeground(textColor);
            b.setFocusPainted(false);
            b.setFont(buttonFont);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setPreferredSize(new Dimension(250, 35));
        }

        buttonPanel.add(knowIdButton);
        buttonPanel.add(dontKnowIdButton);

        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);

        knowIdButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new ConnaitreIdDialog(parent, connexionBdd));
        });

        dontKnowIdButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new NePasConnaitreIdDialog(parent, connexionBdd));
        });

        setResizable(false);
        setVisible(true);
    }

    // Fenêtre "Je connais mon ID" avec design harmonisé et sécurité améliorée
    private class ConnaitreIdDialog extends JDialog {

        private JTextField idField;
        private JTextField emailField;
        private JPasswordField newPasswordField;

        public ConnaitreIdDialog(JFrame parent, ConnexionBdd connexionBdd) {
            super(parent, "Réinitialisation du mot de passe", true);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(bgColor);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel idLabel = new JLabel("ID:");
            idLabel.setFont(labelFont);
            idLabel.setForeground(btnColor);
            idField = new JTextField(20);

            JLabel emailLabel = new JLabel("Email:");
            emailLabel.setFont(labelFont);
            emailLabel.setForeground(btnColor);
            emailField = new JTextField(20);

            JLabel newPasswordLabel = new JLabel("Nouveau mot de passe:");
            newPasswordLabel.setFont(labelFont);
            newPasswordLabel.setForeground(btnColor);
            newPasswordField = new JPasswordField(20);

            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(idLabel, gbc);
            gbc.gridx = 1;
            panel.add(idField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(emailLabel, gbc);
            gbc.gridx = 1;
            panel.add(emailField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            panel.add(newPasswordLabel, gbc);
            gbc.gridx = 1;
            panel.add(newPasswordField, gbc);

            JButton okButton = new JButton("OK");
            JButton retourButton = new JButton("Retour au login");

            JButton[] buttons = {okButton, retourButton};
            for (JButton b : buttons) {
                b.setBackground(btnColor);
                b.setForeground(textColor);
                b.setFocusPainted(false);
                b.setFont(buttonFont);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.setPreferredSize(new Dimension(200, 30));
            }

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setBackground(bgColor);
            buttonsPanel.add(okButton);
            buttonsPanel.add(retourButton);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            panel.add(buttonsPanel, gbc);

            setContentPane(panel);
            pack();
            setLocationRelativeTo(parent);
            setResizable(false);

            okButton.addActionListener(this::handleOk);
            retourButton.addActionListener(e -> {
                dispose();
                parent.setVisible(true);
            });

            setVisible(true);
        }

        private void handleOk(ActionEvent e) {
            String id = idField.getText().trim();
            String email = emailField.getText().trim();
            String newPassword = new String(newPasswordField.getPassword()).trim();

            if (id.isEmpty() || email.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.");
                return;
            }

            // Validation mot de passe
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(this, "Le mot de passe doit contenir au moins 6 caractères.");
                return;
            }

            try (Connection connection = connexionBdd.getConnection(); 
                 PreparedStatement ps = connection.prepareStatement(
                    "SELECT nom, prenom FROM utilisateurs WHERE ID = ? AND email = ? AND active = TRUE")) {
                ps.setString(1, id);
                ps.setString(2, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    
                    // Mise à jour sécurisée avec cryptage
                    try (PreparedStatement updatePs = connection.prepareStatement(
                            "UPDATE utilisateurs SET Password = crypt(?, gen_salt('bf')), password_reset_required = FALSE WHERE ID = ?")) {
                        updatePs.setString(1, newPassword);
                        updatePs.setString(2, id);
                        int updated = updatePs.executeUpdate();
                        if (updated > 0) {
                            String userName = (prenom != null ? prenom + " " : "") + (nom != null ? nom : "");
                            JOptionPane.showMessageDialog(this, 
                                "Mot de passe mis à jour avec succès" + 
                                (!userName.trim().isEmpty() ? " pour " + userName.trim() : "") + ".");
                            dispose();
                            parent.setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(this, "Erreur lors de la mise à jour du mot de passe.");
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Couple ID/Email non trouvé ou compte inactif.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erreur base de données: " + ex.getMessage());
            }
        }
    }

    // Fenêtre "Je ne connais pas mon ID" avec design harmonisé et validation améliorée
    private class NePasConnaitreIdDialog extends JDialog {

        private JTextField emailField;
        private JLabel resultLabel;

        public NePasConnaitreIdDialog(JFrame parent, ConnexionBdd connexionBdd) {
            super(parent, "Recherche d'ID", true);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(bgColor);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel emailLabel = new JLabel("Email:");
            emailLabel.setFont(labelFont);
            emailLabel.setForeground(btnColor);
            emailField = new JTextField(20);

            JButton searchButton = new JButton("Rechercher");
            JButton retourButton = new JButton("Retour");

            JButton[] buttons = {searchButton, retourButton};
            for (JButton b : buttons) {
                b.setBackground(btnColor);
                b.setForeground(textColor);
                b.setFocusPainted(false);
                b.setFont(buttonFont);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.setPreferredSize(new Dimension(200, 30));
            }

            resultLabel = new JLabel(" ");
            resultLabel.setHorizontalAlignment(SwingConstants.CENTER);

            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(emailLabel, gbc);
            gbc.gridx = 1;
            panel.add(emailField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.CENTER;
            panel.add(searchButton, gbc);

            gbc.gridy = 2;
            panel.add(resultLabel, gbc);

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setBackground(bgColor);
            buttonsPanel.add(retourButton);

            gbc.gridy = 3;
            panel.add(buttonsPanel, gbc);

            setContentPane(panel);
            pack();
            setLocationRelativeTo(parent);
            setResizable(false);

            searchButton.addActionListener(e -> rechercherId());
            retourButton.addActionListener(e -> {
                dispose();
                SwingUtilities.invokeLater(() -> new MdpOublie(parent, connexionBdd));
            });

            setVisible(true);
        }

        private boolean isValidEmail(String email) {
            return email != null && email.contains("@") && email.contains(".") 
                   && email.length() > 5 && !email.startsWith("@") && !email.endsWith("@");
        }

        private void rechercherId() {
            String email = emailField.getText().trim();
            if (email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Veuillez saisir un email.");
                return;
            }
            
            // Validation email
            if (!isValidEmail(email)) {
                JOptionPane.showMessageDialog(this, "Format d'email invalide.");
                return;
            }

            try (Connection connection = connexionBdd.getConnection(); 
                 PreparedStatement ps = connection.prepareStatement(
                    "SELECT ID, nom, prenom FROM utilisateurs WHERE email = ? AND active = TRUE")) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String id = rs.getString("ID");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    
                    String displayName = "";
                    if (prenom != null && nom != null) {
                        displayName = " (" + prenom + " " + nom + ")";
                    }
                    
                    resultLabel.setForeground(new Color(0, 128, 0));
                    resultLabel.setText("ID trouvé : " + id + displayName);
                } else {
                    resultLabel.setForeground(Color.RED);
                    resultLabel.setText("<html>Email non trouvé ou compte inactif.<br>Contactez les administrateurs.</html>");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erreur base de données: " + ex.getMessage());
            }
        }
    }
}

