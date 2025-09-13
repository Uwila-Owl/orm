
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class Contact extends JDialog {

    private JTextField emailField;
    private JComboBox<String> sujetComboBox;
    private JTextArea messageArea;
    private JButton envoyerButton;

    // webhook Discord
    private static final Map<String, WebhookInfo> WEBHOOK_CONFIG = new LinkedHashMap<>();

    static {
        WEBHOOK_CONFIG.put("Incident avec la Zone de Modélisation", new WebhookInfo(
                "https://discord.com/api/webhooks/1416483826657661120/oMc0GOVAaZcRqniBAFwJK8w9AI8M1M-IilGwi5qKPj1kZrpB_AIj696lP6CAWRRTcEko",
                "<@432885672500658198>" // Tag pour la zone de modélisation
        ));
        WEBHOOK_CONFIG.put("Incident avec la Menu de navigation", new WebhookInfo(
                "https://discord.com/api/webhooks/1416484119243915314/JoZ8yeybwdPREIprtB69H_2GvRxD50Hxf6vwmT0w5QfmjFV6hIr5WCt914a6Z_75GsQU",
                "<@1049405425860218921>"
        ));
        WEBHOOK_CONFIG.put("Incident avec la Barre d'outils", new WebhookInfo(
                "https://discord.com/api/webhooks/1416483699905789973/AINp_5DUUpxd73Yu32csWTM472XsO0HHymJy3AJGpeD-aHeH2OMgw7QmIn11p0hmajiw",
                "<@824979914775592962>"
        ));
        WEBHOOK_CONFIG.put("Incident avec le Panneau de propriétés", new WebhookInfo(
                "https://discord.com/api/webhooks/1416483917304823900/mTmt6skDycnZdJMv3FwIQt_pCQSauEi6PDQn5CpB3RMUEhOWc5zV-74pNW9s3kYwE-_g",
                "<@824979914775592962>"
        ));
        WEBHOOK_CONFIG.put("Problème de Connexion à la BDD", new WebhookInfo(
                "https://discord.com/api/webhooks/1416484014369538149/HpDl0KcTAPUrhKtpASJ8YiDcNNcNk4SpJ6CodL3GwaypcPsx21DqfKPc3rZdo5ZHbJFo",
                "<@432885672500658198>"
        ));
        WEBHOOK_CONFIG.put("Suggestion de Fonctionnalité", new WebhookInfo(
                "https://dim/api/webhooks/1416484294033150052/HeTkzqGDyT6fC8IV2iSpOcdPTVDc4FmH9c8BTKwFDJXerv41Cy2ugfLSs5rEt6n9UT-q",
                "<@everyone>"
        ));
        WEBHOOK_CONFIG.put("Autre Sujet", new WebhookInfo(
                "https://discord.com/api/webhooks/1416484198000361522/Lr9Gm9S3Q5Ael7VuHPF74B6IKchyCa-gwv7eMGQwW03Ineyf_Ld5zm5KpFJ65poqVvFy",
                "<@everyone>"
        ));
    }

    // Classe interne pour stocker les informations du webhook
    private static class WebhookInfo {

        String url;
        String tag;

        WebhookInfo(String url, String tag) {
            this.url = url;
            this.tag = tag;
        }
    }

    public Contact(JFrame parent) {
        super(parent, "Nous contacter", true);
        setSize(450, 400);
        setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.decode("#D6E3F3"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Email
        JLabel emailLabel = new JLabel("Votre email:");
        emailField = new JTextField(25);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        panel.add(emailLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(emailField, gbc);

        // Sujet
        JLabel sujetLabel = new JLabel("Sujet:");
        sujetComboBox = new JComboBox<>(WEBHOOK_CONFIG.keySet().toArray(new String[0]));
        sujetComboBox.setSelectedIndex(0);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(sujetLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(sujetComboBox, gbc);

        // Message
        JLabel messageLabel = new JLabel("Votre message:");
        messageArea = new JTextArea(8, 25);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(messageLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(scrollPane, gbc);

        // Bouton Envoyer
        envoyerButton = new JButton("Envoyer");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(envoyerButton, gbc);

        envoyerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                envoyerMessage();
            }
        });

        setContentPane(panel);
        setVisible(true);
    }

    private void envoyerMessage() {
        String email = emailField.getText().trim();
        String selectedSujet = (String) sujetComboBox.getSelectedItem();
        String message = messageArea.getText().trim();

        if (email.isEmpty() || selectedSujet == null || message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        WebhookInfo webhookInfo = WEBHOOK_CONFIG.get(selectedSujet);
        if (webhookInfo == null) {
            JOptionPane.showMessageDialog(this, "Configuration de webhook introuvable pour le sujet sélectionné.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Construire le contenu du message à envoyer sur Discord
        boolean success = envoyerMessageDiscord(webhookInfo.url,
                "**Nouveau Ticket**\n"
                + "**Sujet:** " + selectedSujet + "\n"
                + "**Email:** " + email + "\n"
                + "**Message:**\n" + message,
                webhookInfo.tag);

        if (success) {
            JOptionPane.showMessageDialog(this, "Message envoyé avec succès !");
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'envoi du message.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean envoyerMessageDiscord(String webhookUrl, String content, String tag) {
        try {
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Construire le contenu complet avec le tag (ex: <@USER_ID> ou @everyone)
            String fullContent = content + "\n" + tag;

            // Échapper les guillemets, antislashs et retours à la ligne dans fullContent
            String escapedContent = fullContent
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n");

            // Construire le JSON avec allowed_mentions pour autoriser les mentions
            String jsonPayload = "{"
                    + "\"content\": \"" + escapedContent + "\","
                    + "\"allowed_mentions\": {\"parse\": [\"users\", \"everyone\"]}"
                    + "}";

            byte[] out = jsonPayload.getBytes(StandardCharsets.UTF_8);
            connection.getOutputStream().write(out);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            return responseCode >= 200 && responseCode < 300;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
