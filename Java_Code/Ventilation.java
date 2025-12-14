import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

public class Ventilation extends JDialog {

    private static final Logger LOGGER = Logger.getLogger(Ventilation.class.getName());
    private ConnexionBdd connexionBdd;
    private String userId;
    private String prenom;
    private EntiteDAO entiteDAO = new EntiteDAO(); // Ajouter cette ligne
    private RelationDAO relationDAO = new RelationDAO(); // Ajouter cette ligne
    private LogDAO logDAO = new LogDAO(); // Pour la journalisation

    // Couleurs et polices harmonisées
    private final Color bgColor = Color.decode("#D6E3F3");
    private final Color btnColor = Color.decode("#3E5871");
    private final Color textColor = Color.decode("#EAECEE");
    private final Font labelFont = new Font("Arial", Font.BOLD, 14);
    private final Font buttonFont = new Font("Arial", Font.BOLD, 14);

    public Ventilation(JFrame parent, ConnexionBdd connexionBdd, String userIdFromLogin) {
        super(parent, "Gestion des Schémas", true);
        this.connexionBdd = connexionBdd;
        this.prenom = UserSession.getInstance().getPrenom();
        this.userId = userIdFromLogin;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(bgColor);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Bienvenue, " + this.prenom + " !");
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(btnColor);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Panel pour les boutons d'action
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        buttonPanel.setBackground(bgColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JButton newSchemaButton = new JButton("Nouveau Schéma");
        JButton selectSchemaButton = new JButton("Choisir / Modifier Schéma");
        JButton deleteSchemaButton = new JButton("Supprimer Schéma");

        JButton[] buttons = {newSchemaButton, selectSchemaButton, deleteSchemaButton};
        for (JButton b : buttons) {
            b.setBackground(btnColor);
            b.setForeground(textColor);
            b.setFocusPainted(false);
            b.setFont(buttonFont);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            buttonPanel.add(b);
        }
        mainPanel.add(buttonPanel, BorderLayout.CENTER);

        // Actions des boutons
        newSchemaButton.addActionListener(this::creerNouveauSchema);
        selectSchemaButton.addActionListener(this::choisirModifierSchema);
        deleteSchemaButton.addActionListener(this::supprimerSchema);

        setContentPane(mainPanel);
        setVisible(true);
    }

    /**
     * Propose à l'utilisateur de créer un nouveau schéma.
     */
    private void creerNouveauSchema(ActionEvent e) {
        String nomSchema = JOptionPane.showInputDialog(this, "Nom du nouveau schéma :", "Nouveau Schéma", JOptionPane.PLAIN_MESSAGE);
        if (nomSchema != null && !nomSchema.trim().isEmpty()) {

            int currentSchemaCount = 0;
            try (Connection conn = connexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM ventilation WHERE user_id = ?")) {
                pstmt.setString(1, userId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        currentSchemaCount = rs.getInt(1);
                    }
                }
            } catch (SQLException ex) {
                logDAO.insertLog(userId, "Erreur SQL lors de la vérification du nombre de schémas: " + ex.getMessage(), "SEVERE");
                JOptionPane.showMessageDialog(this, "Erreur base de données lors de la vérification du nombre de schémas.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return; // Arrête le processus si la vérification échoue
            }

            final int MAX_SCHEMAS_PER_USER = 50; // Définir la limite de schéma ici

            if (currentSchemaCount >= MAX_SCHEMAS_PER_USER) {
                JOptionPane.showMessageDialog(this,
                        "Vous avez atteint la limite de " + MAX_SCHEMAS_PER_USER + " schémas. Veuillez supprimer des schémas existants pour en créer de nouveaux.",
                        "Limite de schémas atteinte", JOptionPane.WARNING_MESSAGE);
                logDAO.insertLog(userId, "Tentative de création de schéma échouée : limite de " + MAX_SCHEMAS_PER_USER + " schémas atteinte pour l'utilisateur " + userId, "WARNING");
                return; // Arrête le processus si la limite est atteinte
            }

            String commentaires = JOptionPane.showInputDialog(this, "Commentaires (optionnel) :", "Nouveau Schéma", JOptionPane.PLAIN_MESSAGE);
            if (commentaires == null) {
                commentaires = ""; // Gérer le cas où l'utilisateur annule les commentaires
            }
            try (Connection conn = connexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO ventilation (user_id, nom_schema, commentaires, date_creation, date_trash) VALUES (?, ?, ?, ?, ?) RETURNING schema_id")) {

                pstmt.setString(1, userId);
                pstmt.setString(2, nomSchema.trim());
                pstmt.setString(3, commentaires.trim());
                pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now().plusDays(365)));

                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int schemaId = rs.getInt("schema_id");
                    JOptionPane.showMessageDialog(this, "Schéma '" + nomSchema + "' créé avec succès ! ID: " + schemaId);
                    logDAO.insertLog(this.userId, "Nouveau schéma créé par " + userId + ": " + nomSchema + " (ID: " + schemaId + ")", "INFO");
                    lancerGenerateurUML(schemaId, nomSchema.trim());
                } else {
                    JOptionPane.showMessageDialog(this, "Erreur lors de la création du schéma.", "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                logDAO.insertLog(userId, "Erreur SQL lors de la création du schéma: " + ex.getMessage(), "SEVERE");
                JOptionPane.showMessageDialog(this, "Erreur base de données: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } else if (nomSchema != null) { // Si l'utilisateur a cliqué sur OK mais n'a rien entré
            JOptionPane.showMessageDialog(this, "Le nom du schéma ne peut pas être vide.", "Attention", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Permet à l'utilisateur de choisir un schéma existant pour le
     * modifier/continuer.
     */
    private void choisirModifierSchema(ActionEvent e) {
        List<Map<String, Object>> schemas = getSchemasForUser(userId);
        if (schemas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vous n'avez aucun schéma existant.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] schemaNames = schemas.stream()
                .map(s -> s.get("nom_schema") + " (ID: " + s.get("schema_id") + ")")
                .toArray(String[]::new);

        String selectedSchema = (String) JOptionPane.showInputDialog(
                this,
                "Choisissez un schéma à modifier :",
                "Modifier Schéma",
                JOptionPane.PLAIN_MESSAGE,
                null,
                schemaNames,
                schemaNames[0]);

        if (selectedSchema != null) {
            // Extraire l'ID du schéma sélectionné
            int schemaId = Integer.parseInt(selectedSchema.substring(selectedSchema.lastIndexOf("ID: ") + 4, selectedSchema.lastIndexOf(")")));
            String nomSchema = selectedSchema.substring(0, selectedSchema.lastIndexOf(" (ID:"));

            int choice = JOptionPane.showConfirmDialog(this,
                    "Voulez-vous modifier le schéma '" + nomSchema + "' (ID: " + schemaId + ") ?",
                    "Confirmation", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                lancerGenerateurUML(schemaId, nomSchema);
            }
        }
    }

    /**
     * Permet à l'utilisateur de supprimer un schéma existant.
     */
    private void supprimerSchema(ActionEvent e) {
        List<Map<String, Object>> schemas = getSchemasForUser(userId);
        if (schemas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vous n'avez aucun schéma à supprimer.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] schemaNames = schemas.stream()
                .map(s -> s.get("nom_schema") + " (ID: " + s.get("schema_id") + ")")
                .toArray(String[]::new);

        String selectedSchema = (String) JOptionPane.showInputDialog(
                this,
                "Choisissez un schéma à supprimer :",
                "Supprimer Schéma",
                JOptionPane.PLAIN_MESSAGE,
                null,
                schemaNames,
                schemaNames[0]);

        if (selectedSchema != null) {
            int schemaId = Integer.parseInt(selectedSchema.substring(selectedSchema.lastIndexOf("ID: ") + 4, selectedSchema.lastIndexOf(")")));
            String nomSchema = selectedSchema.substring(0, selectedSchema.lastIndexOf(" (ID:"));

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Êtes-vous sûr de vouloir supprimer le schéma '" + nomSchema + "' (ID: " + schemaId + ") ?\n"
                    + "Cette action est irréversible et supprimera aussi toutes les entités et relations associées.", // Texte mis à jour
                    "Confirmer Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = connexionBdd.getConnection()) {
                    conn.setAutoCommit(false); // Début de la transaction

                    try {
                        // 1. Supprimer les relations associées aux entités du schéma
                        relationDAO.supprimerRelationsParSchema(schemaId);

                        // 2. Récupérer les IDs des entités associées au schéma pour supprimer leurs attributs
                        List<Integer> entiteIds = new ArrayList<>();
                        try (PreparedStatement pstmtGetEntiteIds = conn.prepareStatement("SELECT id FROM entites WHERE schema_id = ?")) {
                            pstmtGetEntiteIds.setInt(1, schemaId);
                            ResultSet rs = pstmtGetEntiteIds.executeQuery();
                            while (rs.next()) {
                                entiteIds.add(rs.getInt("id"));
                            }
                        }

                        // 3. Supprimer les attributs de chaque entité
                        for (Integer entiteId : entiteIds) {
                            try (PreparedStatement pstmtAttributs = conn.prepareStatement("DELETE FROM attributs WHERE entite_id = ?")) {
                                pstmtAttributs.setInt(1, entiteId);
                                pstmtAttributs.executeUpdate();
                            }
                        }

                        // 4. Supprimer les entités associées au schéma
                        try (PreparedStatement pstmtEntites = conn.prepareStatement("DELETE FROM entites WHERE schema_id = ?")) {
                            pstmtEntites.setInt(1, schemaId);
                            pstmtEntites.executeUpdate();
                        }

                        // 5. Supprimer le schéma de la table ventilation
                        try (PreparedStatement pstmtVentilation = conn.prepareStatement("DELETE FROM ventilation WHERE schema_id = ?")) {
                            pstmtVentilation.setInt(1, schemaId);
                            int affectedRows = pstmtVentilation.executeUpdate();

                            if (affectedRows > 0) {
                                conn.commit(); // Valider la transaction
                                JOptionPane.showMessageDialog(this, "Schéma '" + nomSchema + "' supprimé avec succès !");
                                logDAO.insertLog(userId, "Schéma supprimé par " + userId + ": " + nomSchema + " (ID: " + schemaId + ")", "INFO");
                            } else {
                                conn.rollback(); // Annuler la transaction
                                JOptionPane.showMessageDialog(this, "Erreur lors de la suppression du schéma.", "Erreur", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } catch (SQLException ex) {
                        conn.rollback(); // Annuler la transaction en cas d'erreur
                        throw ex; // Re-lancer l'exception pour la gestion d'erreur supérieure
                    } finally {
                        conn.setAutoCommit(true); // Rétablir l'auto-commit
                    }
                } catch (SQLException ex) {
                    logDAO.insertLog(userId, "Erreur SQL lors de la suppression du schéma: " + ex.getMessage(), "SEVERE");
                    JOptionPane.showMessageDialog(this, "Erreur base de données: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private List<Map<String, Object>> getSchemasForUser(String userId) {
        List<Map<String, Object>> schemas = new ArrayList<>();
        try (Connection conn = connexionBdd.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT schema_id, nom_schema, commentaires FROM ventilation WHERE user_id = ? ORDER BY nom_schema")) {
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> schema = new HashMap<>();
                schema.put("schema_id", rs.getInt("schema_id"));
                schema.put("nom_schema", rs.getString("nom_schema"));
                schema.put("commentaires", rs.getString("commentaires"));
                schemas.add(schema);
            }
        } catch (SQLException ex) {
            logDAO.insertLog(userId, "Erreur SQL lors de la récupération des schémas: " + ex.getMessage(), "SEVERE");
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des schémas: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
        return schemas;
    }

    private void lancerGenerateurUML(int schemaId, String nomSchema) {
        dispose(); // Ferme la fenêtre de ventilation
        Platform.runLater(() -> {
            try {
                InterfaceGenerateurUML app = new InterfaceGenerateurUML(schemaId, nomSchema);
                javafx.stage.Stage stage = new javafx.stage.Stage();
                app.start(stage);
            } catch (Exception ex) {
                logDAO.insertLog(userId, "Erreur lors du lancement de InterfaceGenerateurUML : " + ex.getMessage(), "SEVERE");
                JOptionPane.showMessageDialog(null, "Erreur lors du lancement de l'application : " + ex.getMessage());
            }
        });
    }
}