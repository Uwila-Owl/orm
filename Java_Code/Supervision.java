
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;

public class Supervision extends JFrame {

    private ConnexionBdd connexionBdd = new ConnexionBdd();

    // Couleurs et polices (harmonisés avec MdpOublie)
    private final Color bgColor = Color.decode("#D6E3F3");
    private final Color btnColor = Color.decode("#3E5871");
    private final Color textColor = Color.decode("#EAECEE");
    private final Font labelFont = new Font("Arial", Font.BOLD, 14);
    private final Font buttonFont = new Font("Arial", Font.BOLD, 14);

    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);

    public Supervision() {
        super("Supervision");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);

        // Panel choix initial
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

        // Panel tableau utilisateurs
        JPanel tableauPanel = creerPanelTableauUtilisateurs();
        mainPanel.add(tableauPanel, "tableau");

        setContentPane(mainPanel);

        panneauButton.addActionListener(e -> {
            // Afficher le tableau des utilisateurs
            actualiserTableauUtilisateurs();
            cardLayout.show(mainPanel, "tableau");
        });

        umlButton.addActionListener(e -> {
            dispose();
            Platform.runLater(() -> {
                try {
                    InterfaceGenerateurUML app = new InterfaceGenerateurUML();
                    javafx.stage.Stage stage = new javafx.stage.Stage();
                    app.start(stage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Erreur lors du lancement de InterfaceGenerateurUML : " + ex.getMessage());
                }
            });
        });

        setVisible(true);
    }

    // Composants du tableau
    private JTable table;
    private UtilisateurTableModel tableModel;

    private JPanel creerPanelTableauUtilisateurs() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);

        JLabel titleLabel = new JLabel("Liste des utilisateurs");
        titleLabel.setFont(labelFont);
        titleLabel.setForeground(btnColor);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        tableModel = new UtilisateurTableModel();
        table = new JTable(tableModel) {
            // Pour centrer les cellules sauf la colonne active (checkbox)
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                if (column == 5) { // colonne active (checkbox)
                    return super.getCellRenderer(row, column);
                }
                return new DefaultTableCellRenderer() {
                    {
                        setHorizontalAlignment(SwingConstants.CENTER);
                    }
                };
            }
        };

        // Colonne active en checkbox
        table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        table.getColumnModel().getColumn(5).setCellRenderer(table.getDefaultRenderer(Boolean.class));

        // Ajuster largeur colonnes
        TableColumn colId = table.getColumnModel().getColumn(0);
        colId.setPreferredWidth(80);
        TableColumn colNom = table.getColumnModel().getColumn(1);
        colNom.setPreferredWidth(120);
        TableColumn colPrenom = table.getColumnModel().getColumn(2);
        colPrenom.setPreferredWidth(120);
        TableColumn colEmail = table.getColumnModel().getColumn(3);
        colEmail.setPreferredWidth(180);
        TableColumn colDiscord = table.getColumnModel().getColumn(4);
        colDiscord.setPreferredWidth(120);
        TableColumn colActive = table.getColumnModel().getColumn(5);
        colActive.setPreferredWidth(60);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Boutons en bas
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setBackground(bgColor);

        JButton saveButton = new JButton("Enregistrer les modifications");
        JButton retourButton = new JButton("Retour");

        JButton[] buttons = {saveButton, retourButton};
        for (JButton b : buttons) {
            b.setBackground(btnColor);
            b.setForeground(textColor);
            b.setFocusPainted(false);
            b.setFont(buttonFont);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setPreferredSize(new Dimension(220, 35));
            buttonsPanel.add(b);
        }

        panel.add(buttonsPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> enregistrerModifications());
        retourButton.addActionListener(e -> cardLayout.show(mainPanel, "choix"));

        return panel;
    }

    private void actualiserTableauUtilisateurs() {
        List<Utilisateur> utilisateurs = new ArrayList<>();
        try (Connection connection = connexionBdd.getConnection(); PreparedStatement ps = connection.prepareStatement(
                "SELECT ID, nom, prenom, email, id_discord, active FROM utilisateurs ORDER BY ID")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Utilisateur u = new Utilisateur(
                        rs.getString("ID"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("id_discord"),
                        rs.getBoolean("active")
                );
                utilisateurs.add(u);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la récupération des utilisateurs : " + ex.getMessage());
        }
        tableModel.setUtilisateurs(utilisateurs);
    }

    private void enregistrerModifications() {
        List<Utilisateur> utilisateurs = tableModel.getUtilisateurs();
        try (Connection connection = connexionBdd.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE utilisateurs SET active = ? WHERE ID = ?")) {
                for (Utilisateur u : utilisateurs) {
                    ps.setBoolean(1, u.isActive());
                    ps.setString(2, u.getId());
                    ps.addBatch();
                }
                ps.executeBatch();
                connection.commit();
                JOptionPane.showMessageDialog(this, "Modifications enregistrées avec succès.");
            } catch (SQLException ex) {
                connection.rollback();
                JOptionPane.showMessageDialog(this, "Erreur lors de l'enregistrement : " + ex.getMessage());
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erreur base de données : " + ex.getMessage());
        }
    }

    // Classe modèle pour le tableau
    private static class UtilisateurTableModel extends AbstractTableModel {

        private final String[] colonnes = {"ID", "Nom", "Prénom", "Email", "ID Discord", "Actif"};
        private List<Utilisateur> utilisateurs = new ArrayList<>();

        public void setUtilisateurs(List<Utilisateur> utilisateurs) {
            this.utilisateurs = utilisateurs;
            fireTableDataChanged();
        }

        public List<Utilisateur> getUtilisateurs() {
            return utilisateurs;
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
            if (columnIndex == 5) {
                return Boolean.class;
            }
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // Seule la colonne "Actif" est éditable
            return columnIndex == 5;
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
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 5) {
                Utilisateur u = utilisateurs.get(rowIndex);
                u.setActive((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    // Classe Utilisateur simple
    private static class Utilisateur {

        private String id;
        private String nom;
        private String prenom;
        private String email;
        private String idDiscord;
        private boolean active;

        public Utilisateur(String id, String nom, String prenom, String email, String idDiscord, boolean active) {
            this.id = id;
            this.nom = nom;
            this.prenom = prenom;
            this.email = email;
            this.idDiscord = idDiscord;
            this.active = active;
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

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}
