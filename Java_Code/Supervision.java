
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;

public class Supervision extends JFrame {

    private ConnexionBdd connexionBdd = new ConnexionBdd();
    private String idSuperviseur;
    private LogDAO logDAO = new LogDAO();
    String userId = UserSession.getInstance().getUserId();

    private final Color bgColor = Color.decode("#D6E3F3");
    private final Color btnColor = Color.decode("#3E5871");
    private final Color textColor = Color.decode("#EAECEE");
    private final Font labelFont = new Font("Arial", Font.BOLD, 14);
    private final Font buttonFont = new Font("Arial", Font.BOLD, 14);

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    private JTable userTable;
    private UtilisateurTableModel utilisateurTableModel;

    private JTable schemaTable;
    private SchemaTableModel schemaTableModel;

    public Supervision(ConnexionBdd connexionBdd, String idSuperviseur) {
        super("Supervision");
        this.connexionBdd = connexionBdd;
        this.idSuperviseur = idSuperviseur;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        JPanel choixPanel = new JPanel(new GridBagLayout());
        choixPanel.setBackground(bgColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        JLabel label = new JLabel("Choisissez une option :");
        label.setFont(labelFont);
        label.setForeground(btnColor);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        choixPanel.add(label, gbc);

        JButton panneauButton = new JButton("Panneau Superviseur");
        JButton umlButton = new JButton("GenerateurUML");

        JButton[] buttons = {panneauButton, umlButton};
        for (JButton b : buttons) {
            b.setBackground(btnColor);
            b.setForeground(textColor);
            b.setFocusPainted(false);
            b.setFont(buttonFont);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setPreferredSize(new Dimension(200, 40));
        }

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        choixPanel.add(panneauButton, gbc);

        gbc.gridx = 1;
        choixPanel.add(umlButton, gbc);

        mainPanel.add(choixPanel, "choix");

        JPanel supervisionPanel = creerPanelSupervision();
        mainPanel.add(supervisionPanel, "supervision");

        setContentPane(mainPanel);

        panneauButton.addActionListener(e -> {
            chargerUtilisateurs();
            cardLayout.show(mainPanel, "supervision");
        });

        umlButton.addActionListener(e -> {
            if (idSuperviseur != null) {
                dispose();
                SwingUtilities.invokeLater(() -> {
                    new Ventilation(this, connexionBdd, this.idSuperviseur);
                });
            } else {
                JOptionPane.showMessageDialog(this, "Vous n'avez pas les droits superviseur pour accéder à cette fonctionnalité.");
            }
        });

        setVisible(true);
    }

    private JPanel creerPanelSupervision() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);

        JButton retourButton = new JButton("Retour au menu principal");
        retourButton.setBackground(btnColor);
        retourButton.setForeground(textColor);
        retourButton.setFocusPainted(false);
        retourButton.setFont(buttonFont);
        retourButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        retourButton.addActionListener(e -> cardLayout.show(mainPanel, "choix"));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(bgColor);
        topPanel.add(retourButton);
        panel.add(topPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(350);

        // Partie gauche : JTable utilisateurs avec colonne superviseur
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBackground(bgColor);
        JLabel userLabel = new JLabel("Utilisateurs");
        userLabel.setFont(labelFont);
        userLabel.setForeground(btnColor);
        userLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userPanel.add(userLabel, BorderLayout.NORTH);

        utilisateurTableModel = new UtilisateurTableModel();
        userTable = new JTable(utilisateurTableModel);
        userTable.setFont(new Font("Arial", Font.PLAIN, 14));
        userTable.setBackground(bgColor);
        userTable.setRowHeight(25);

        // Colonne superviseur en checkbox
        TableColumn superviseurColumn = userTable.getColumnModel().getColumn(6);
        superviseurColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()));
        superviseurColumn.setCellRenderer(userTable.getDefaultRenderer(Boolean.class));

        JScrollPane userScrollPane = new JScrollPane(userTable);
        userPanel.add(userScrollPane, BorderLayout.CENTER);

        // Création du menu contextuel
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem modifierUtilisateurItem = new JMenuItem("Modifier l'utilisateur");
        modifierUtilisateurItem.addActionListener(ev -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow >= 0) {
                String userId = (String) utilisateurTableModel.getValueAt(selectedRow, 0);
                ouvrirFenetreModificationUtilisateur(userId);
            }
        });

        JMenuItem modifierMotDePasseItem = new JMenuItem("Réinitialiser le mot de passe");
        modifierMotDePasseItem.addActionListener(ev -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow >= 0) {
                String userId = (String) utilisateurTableModel.getValueAt(selectedRow, 0);
                ouvrirFenetreModificationMotDePasse(userId);
            }
        });

        JMenuItem supprimerUtilisateurItem = new JMenuItem("Supprimer l'utilisateur");
        supprimerUtilisateurItem.addActionListener(ev -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow >= 0) {
                String userId = (String) utilisateurTableModel.getValueAt(selectedRow, 0);
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Êtes-vous sûr de vouloir supprimer l'utilisateur '" + userId + "' ?",
                        "Confirmer suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    supprimerUtilisateur(userId);
                    chargerUtilisateurs(); // rafraîchir la liste
                }
            }
        });

        popupMenu.add(modifierUtilisateurItem);
        popupMenu.add(modifierMotDePasseItem);
        popupMenu.addSeparator();
        popupMenu.add(supprimerUtilisateurItem);

        // Ajout du listener clic droit sur la table utilisateur
        userTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                int row = userTable.rowAtPoint(e.getPoint());
                int col = userTable.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 0) { // Colonne ID
                    userTable.setRowSelectionInterval(row, row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        splitPane.setLeftComponent(userPanel);

        // Partie droite : JTable schémas avec case à cocher suppression
        JPanel schemaPanel = new JPanel(new BorderLayout());
        schemaPanel.setBackground(bgColor);
        JLabel schemaLabel = new JLabel("Schémas");
        schemaLabel.setFont(labelFont);
        schemaLabel.setForeground(btnColor);
        schemaLabel.setHorizontalAlignment(SwingConstants.CENTER);
        schemaPanel.add(schemaLabel, BorderLayout.NORTH);

        schemaTableModel = new SchemaTableModel();
        schemaTable = new JTable(schemaTableModel);
        schemaTable.setFont(new Font("Arial", Font.PLAIN, 14));
        schemaTable.setBackground(bgColor);
        schemaTable.setRowHeight(25);

        // Colonne suppression en checkbox
        TableColumn supprimerColumn = schemaTable.getColumnModel().getColumn(3);
        supprimerColumn.setCellEditor(new DefaultCellEditor(new JCheckBox()));
        supprimerColumn.setCellRenderer(schemaTable.getDefaultRenderer(Boolean.class));
        supprimerColumn.setMaxWidth(80);

        JScrollPane schemaScrollPane = new JScrollPane(schemaTable);
        schemaPanel.add(schemaScrollPane, BorderLayout.CENTER);

        // Bouton supprimer schémas cochés
        JButton supprimerSchemasButton = new JButton("Supprimer les schémas sélectionnés");
        supprimerSchemasButton.setBackground(btnColor);
        supprimerSchemasButton.setForeground(textColor);
        supprimerSchemasButton.setFocusPainted(false);
        supprimerSchemasButton.setFont(buttonFont);
        supprimerSchemasButton.addActionListener(e -> supprimerSchemasSelectionnes());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(bgColor);
        btnPanel.add(supprimerSchemasButton);
        schemaPanel.add(btnPanel, BorderLayout.SOUTH);

        splitPane.setRightComponent(schemaPanel);

        panel.add(splitPane, BorderLayout.CENTER);

        // Listener sélection utilisateur pour filtrer schémas
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String userId = (String) utilisateurTableModel.getValueAt(selectedRow, 0);
                    actualiserTableauSchemas(userId);
                } else {
                    actualiserTableauSchemas(null);
                }
            }
        });

        // Listener modification superviseur (case à cocher)
        utilisateurTableModel.addTableModelListener(e -> {
            if (e.getColumn() == 6) { // Colonne superviseur
                int row = e.getFirstRow();
                Boolean isSuperviseur = (Boolean) utilisateurTableModel.getValueAt(row, 6);
                String userId = (String) utilisateurTableModel.getValueAt(row, 0);
                modifierDroitSuperviseur(userId, isSuperviseur);
            }
        });

        return panel;
    }

    private void chargerUtilisateurs() {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        try (Connection connection = connexionBdd.getConnection(); PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, nom, prenom, email, id_discord, active, supvis FROM utilisateurs ORDER BY nom, prenom")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Utilisateur u = new Utilisateur(
                        rs.getString("ID"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("id_discord"),
                        rs.getBoolean("active"),
                        rs.getBoolean("supvis")
                );
                utilisateurs.add(u);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des utilisateurs : " + ex.getMessage());
        }
        utilisateurTableModel.setUtilisateurs(utilisateurs);

        if (!utilisateurs.isEmpty()) {
            userTable.setRowSelectionInterval(0, 0);
        } else {
            actualiserTableauSchemas(null);
        }
    }

    private void ouvrirFenetreModificationUtilisateur(String userId) {
        try (Connection conn = connexionBdd.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT nom, prenom, email, id_discord, active, supvis FROM utilisateurs WHERE ID = ?")) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                JDialog dialog = new JDialog(this, "Modifier utilisateur " + userId, true);
                JPanel panel = new JPanel(new GridBagLayout());
                panel.setBackground(Color.decode("#D6E3F3"));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(8, 10, 8, 10);
                gbc.fill = GridBagConstraints.HORIZONTAL;

                JLabel nomLabel = new JLabel("Nom:");
                JTextField nomField = new JTextField(rs.getString("nom"), 15);
                JLabel prenomLabel = new JLabel("Prénom:");
                JTextField prenomField = new JTextField(rs.getString("prenom"), 15);
                JLabel emailLabel = new JLabel("Email:");
                JTextField emailField = new JTextField(rs.getString("email"), 15);
                JLabel idDiscordLabel = new JLabel("ID Discord:");
                JTextField idDiscordField = new JTextField(rs.getString("id_discord"), 15);
                JCheckBox activeCheck = new JCheckBox("Actif", rs.getBoolean("active"));
                JCheckBox supvisCheck = new JCheckBox("Superviseur", rs.getBoolean("supvis"));

                JButton saveButton = new JButton("Enregistrer");
                JButton cancelButton = new JButton("Annuler");

                gbc.gridx = 0;
                gbc.gridy = 0;
                panel.add(nomLabel, gbc);
                gbc.gridx = 1;
                panel.add(nomField, gbc);
                gbc.gridx = 0;
                gbc.gridy = 1;
                panel.add(prenomLabel, gbc);
                gbc.gridx = 1;
                panel.add(prenomField, gbc);
                gbc.gridx = 0;
                gbc.gridy = 2;
                panel.add(emailLabel, gbc);
                gbc.gridx = 1;
                panel.add(emailField, gbc);
                gbc.gridx = 0;
                gbc.gridy = 3;
                panel.add(idDiscordLabel, gbc);
                gbc.gridx = 1;
                panel.add(idDiscordField, gbc);
                gbc.gridx = 0;
                gbc.gridy = 4;
                panel.add(activeCheck, gbc);
                gbc.gridx = 1;
                panel.add(supvisCheck, gbc);
                gbc.gridx = 0;
                gbc.gridy = 5;
                panel.add(saveButton, gbc);
                gbc.gridx = 1;
                panel.add(cancelButton, gbc);

                dialog.setContentPane(panel);
                dialog.pack();
                dialog.setLocationRelativeTo(this);

                saveButton.addActionListener(e -> {
                    try (Connection conn2 = connexionBdd.getConnection(); PreparedStatement psUpdate = conn2.prepareStatement(
                            "UPDATE utilisateurs SET nom = ?, prenom = ?, email = ?, id_discord = ?, active = ?, supvis = ? WHERE ID = ?")) {
                        psUpdate.setString(1, nomField.getText().trim());
                        psUpdate.setString(2, prenomField.getText().trim());
                        psUpdate.setString(3, emailField.getText().trim());
                        String idDiscordVal = idDiscordField.getText().trim();
                        if (idDiscordVal.isEmpty()) {
                            psUpdate.setNull(4, java.sql.Types.VARCHAR);
                        } else {
                            psUpdate.setString(4, idDiscordVal);
                        }
                        psUpdate.setBoolean(5, activeCheck.isSelected());
                        psUpdate.setBoolean(6, supvisCheck.isSelected());
                        psUpdate.setString(7, userId);

                        int updated = psUpdate.executeUpdate();
                        if (updated > 0) {
                            JOptionPane.showMessageDialog(dialog, "Utilisateur modifié avec succès.");
                            dialog.dispose();
                            chargerUtilisateurs();
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Erreur lors de la modification.");
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(dialog, "Erreur SQL: " + ex.getMessage());
                    }
                });

                cancelButton.addActionListener(e -> dialog.dispose());

                dialog.setVisible(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur SQL: " + ex.getMessage());
        }
    }

    private void ouvrirFenetreModificationMotDePasse(String userId) {
        JPasswordField passwordField = new JPasswordField();
        Object[] message = {
            "Nouveau mot de passe provisoire:", passwordField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Modifier / Réinitialiser mot de passe pour " + userId, JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String newPassword = new String(passwordField.getPassword()).trim();
            if (newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Le mot de passe ne peut pas être vide.");
                return;
            }
            try (Connection conn = connexionBdd.getConnection(); PreparedStatement ps = conn.prepareStatement(
                    "UPDATE utilisateurs SET Password = crypt(?, gen_salt('bf')), password_reset_required = TRUE WHERE ID = ?")) {
                ps.setString(1, newPassword);
                ps.setString(2, userId);
                int updated = ps.executeUpdate();
                if (updated > 0) {
                    JOptionPane.showMessageDialog(this, "Mot de passe modifié avec succès. L'utilisateur devra le changer à la prochaine connexion.");
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur lors de la modification du mot de passe.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erreur SQL: " + ex.getMessage());
            }
        }
    }

    private void supprimerUtilisateur(String userId) {
        try (Connection conn = connexionBdd.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM utilisateurs WHERE ID = ?")) {
            ps.setString(1, userId);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                JOptionPane.showMessageDialog(this, "Utilisateur supprimé avec succès.");
            } else {
                JOptionPane.showMessageDialog(this, "Utilisateur non trouvé.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur SQL: " + ex.getMessage());
        }
    }

    private void actualiserTableauSchemas(String userId) {
        List<Schema> schemas = new ArrayList<>();
        String sql;
        try (Connection conn = connexionBdd.getConnection()) {
            if (userId == null) {
                sql = "SELECT schema_id, nom_schema, commentaires, date_creation FROM ventilation ORDER BY nom_schema";
                try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        schemas.add(new Schema(
                                rs.getInt("schema_id"),
                                rs.getString("nom_schema"),
                                rs.getString("commentaires"),
                                rs.getTimestamp("date_creation"),
                                false));
                    }
                }
            } else {
                sql = "SELECT schema_id, nom_schema, commentaires, date_creation FROM ventilation WHERE user_id = ? ORDER BY nom_schema";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            schemas.add(new Schema(
                                    rs.getInt("schema_id"),
                                    rs.getString("nom_schema"),
                                    rs.getString("commentaires"),
                                    rs.getTimestamp("date_creation"),
                                    false));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur récupération schémas : " + e.getMessage());
        }
        schemaTableModel.setSchemas(schemas);
    }

    private void modifierDroitSuperviseur(String userId, boolean estSuperviseur) {
        String sql = "UPDATE utilisateurs SET supvis = ? WHERE ID = ?";
        try (Connection conn = connexionBdd.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, estSuperviseur);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur mise à jour droit superviseur : " + e.getMessage());
        }
    }

    private void supprimerSchemasSelectionnes() {
        List<Schema> schemasASupprimer = new ArrayList<>();
        for (int i = 0; i < schemaTableModel.getRowCount(); i++) {
            Boolean aSupprimer = (Boolean) schemaTableModel.getValueAt(i, 3);
            if (aSupprimer != null && aSupprimer) {
                schemasASupprimer.add(schemaTableModel.getSchemaAt(i));
            }
        }
        if (schemasASupprimer.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez cocher au moins un schéma à supprimer.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Êtes-vous sûr de vouloir supprimer les " + schemasASupprimer.size() + " schéma(s) sélectionné(s) ?\n"
                + "Cette action est irréversible et supprimera aussi toutes les entités et relations associées.",
                "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = connexionBdd.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (Schema s : schemasASupprimer) {
                    int schemaId = s.getId();

                    // Supprimer relations associées
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "DELETE FROM relations WHERE entite_source_id IN (SELECT id FROM entites WHERE schema_id = ?) OR entite_cible_id IN (SELECT id FROM entites WHERE schema_id = ?)")) {
                        pstmt.setInt(1, schemaId);
                        pstmt.setInt(2, schemaId);
                        pstmt.executeUpdate();
                    }

                    // Supprimer attributs des entités
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "DELETE FROM attributs WHERE entite_id IN (SELECT id FROM entites WHERE schema_id = ?)")) {
                        pstmt.setInt(1, schemaId);
                        pstmt.executeUpdate();
                    }

                    // Supprimer entités
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "DELETE FROM entites WHERE schema_id = ?")) {
                        pstmt.setInt(1, schemaId);
                        pstmt.executeUpdate();
                    }

                    // Supprimer schéma
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "DELETE FROM ventilation WHERE schema_id = ?")) {
                        pstmt.setInt(1, schemaId);
                        pstmt.executeUpdate();
                    }
                }
                conn.commit();
                JOptionPane.showMessageDialog(this, "Schémas supprimés avec succès !");
                // Recharger la liste des schémas selon utilisateur sélectionné
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow >= 0) {
                    String userId = (String) utilisateurTableModel.getValueAt(selectedRow, 0);
                    actualiserTableauSchemas(userId);
                } else {
                    actualiserTableauSchemas(null);
                }
            } catch (SQLException ex) {
                conn.rollback();
                JOptionPane.showMessageDialog(this, "Erreur lors de la suppression : " + ex.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur base de données : " + ex.getMessage());
        }
    }

    // Classes internes
    private static class Utilisateur {

        private String id, nom, prenom, email, idDiscord;
        private boolean active, supvis;

        public Utilisateur(String id, String nom, String prenom, String email, String idDiscord, boolean active, boolean supvis) {
            this.id = id;
            this.nom = nom;
            this.prenom = prenom;
            this.email = email;
            this.idDiscord = idDiscord;
            this.active = active;
            this.supvis = supvis;
        }

        public String getId() {
            return id;
        }

        public String getNom() {
            return nom;
        }

        public String getPrenom() {
            return prenom;
        }

        public String getEmail() {
            return email;
        }

        public String getIdDiscord() {
            return idDiscord;
        }

        public boolean isActive() {
            return active;
        }

        public boolean isSupvis() {
            return supvis;
        }

        public void setSupvis(boolean supvis) {
            this.supvis = supvis;
        }
    }

    private static class UtilisateurTableModel extends AbstractTableModel {

        private final String[] colonnes = {"ID", "Nom", "Prénom", "Email", "ID Discord", "Actif", "Superviseur"};
        private List<Utilisateur> utilisateurs = new ArrayList<>();

        public void setUtilisateurs(List<Utilisateur> utilisateurs) {
            this.utilisateurs = utilisateurs;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return utilisateurs.size();
        }

        @Override
        public int getColumnCount() {
            return colonnes.length;
        }

        @Override
        public String getColumnName(int column) {
            return colonnes[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 5 || columnIndex == 6) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Seule la colonne "Superviseur" est éditable
            return columnIndex == 6;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Utilisateur u = utilisateurs.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return u.getId();
                case 1:
                    return u.getNom();
                case 2:
                    return u.getPrenom();
                case 3:
                    return u.getEmail();
                case 4:
                    return u.getIdDiscord();
                case 5:
                    return u.isActive();
                case 6:
                    return u.isSupvis();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 6) {
                Utilisateur u = utilisateurs.get(rowIndex);
                u.setSupvis((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    private static class Schema {

        private int id;
        private String nom;
        private String commentaires;
        private Timestamp dateCreation;
        private boolean supprimer;

        public Schema(int id, String nom, String commentaires, Timestamp dateCreation, boolean supprimer) {
            this.id = id;
            this.nom = nom;
            this.commentaires = commentaires;
            this.dateCreation = dateCreation;
            this.supprimer = supprimer;
        }

        public int getId() {
            return id;
        }

        public String getNom() {
            return nom;
        }

        public String getCommentaires() {
            return commentaires;
        }

        public Timestamp getDateCreation() {
            return dateCreation;
        }

        public boolean isSupprimer() {
            return supprimer;
        }

        public void setSupprimer(boolean supprimer) {
            this.supprimer = supprimer;
        }
    }

    private static class SchemaTableModel extends AbstractTableModel {

        private final String[] colonnes = {"Nom Schéma", "Commentaires", "Date Création", "Supprimer"};
        private List<Schema> schemas = new ArrayList<>();

        public void setSchemas(List<Schema> schemas) {
            this.schemas = schemas;
            fireTableDataChanged();
        }

        public Schema getSchemaAt(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < schemas.size()) {
                return schemas.get(rowIndex);
            }
            return null;
        }

        @Override
        public int getRowCount() {
            return schemas.size();
        }

        @Override
        public int getColumnCount() {
            return colonnes.length;
        }

        @Override
        public String getColumnName(int column) {
            return colonnes[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3) {
                return Boolean.class;
            }
            if (columnIndex == 2) {
                return Timestamp.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Seule la colonne "Supprimer" est éditable
            return columnIndex == 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Schema s = schemas.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return s.getNom();
                case 1:
                    return s.getCommentaires();
                case 2:
                    return s.getDateCreation();
                case 3:
                    return s.isSupprimer();
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 3) {
                Schema s = schemas.get(rowIndex);
                s.setSupprimer((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }
}
