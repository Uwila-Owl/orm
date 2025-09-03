
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Contact extends JDialog {

    private JTextField emailField;
    private JTextField sujetField;
    private JTextArea messageArea;
    private JButton envoyerButton;

    // Votre webhook Discord
    private static final String DISCORD_WEBHOOK_URL = "https://discord.com/api/webhooks/1412842742433714287/FcLVK4rAB0JQtLw3yepzvEOduawbC06sjexbtMw0dtw6kSlf49KLEinp0AjjK9n1Zvyg";

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
        sujetField = new JTextField(25);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(sujetLabel, gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        panel.add(sujetField, gbc);

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
        String sujet = sujetField.getText().trim();
        String message = messageArea.getText().trim();

        if (email.isEmpty() || sujet.isEmpty() || message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Veuillez remplir tous les champs.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Construire le contenu du message à envoyer sur Discord
        String discordMessage = "**Nouveau message de contact**\n"
                + "**Email:** " + email + "\n"
                + "**Sujet:** " + sujet + "\n"
                + "**Message:**\n" + message;

        boolean success = envoyerMessageDiscord(discordMessage);

        if (success) {
            JOptionPane.showMessageDialog(this, "Message envoyé avec succès !");
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'envoi du message.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean envoyerMessageDiscord(String content) {
        try {
            URL url = new URL(DISCORD_WEBHOOK_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            // Échappement simple des guillemets dans le contenu JSON
            String escapedContent = content.replace("\"", "\\\"").replace("\n", "\\n");

            String jsonPayload = "{\"content\": \"" + escapedContent + "\"}";

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
