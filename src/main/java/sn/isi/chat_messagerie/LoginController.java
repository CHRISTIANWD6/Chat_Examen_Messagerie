package sn.isi.chat_messagerie;


import sn.isi.chat_messagerie.client.ServerConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Contrôleur de l'écran de connexion / inscription.
 */
public class LoginController {

    // Champs onglet Connexion
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    // Champs onglet Inscription (fx:id distincts pour éviter les conflits FXML)
    @FXML private Tab registerTab;
    @FXML private TextField usernameRegField;
    @FXML private PasswordField passwordRegField;
    @FXML private ComboBox<String> roleComboBox;

    @FXML private Label statusLabel;
    @FXML private TabPane tabPane;

    private final ServerConnection conn = ServerConnection.getInstance();

    @FXML
    public void initialize() {
        // Forcer le chargement de l'onglet Inscription pour que roleComboBox soit injecté
        // JavaFX initialise les contrôles dans les Tab en lazy — on force le rendu
        javafx.application.Platform.runLater(() -> {
            tabPane.getSelectionModel().select(1); // Aller sur Inscription
            tabPane.getSelectionModel().select(0); // Revenir sur Connexion

            // Remplir les rôles maintenant que le Tab est rendu
            if (roleComboBox != null) {
                roleComboBox.getItems().addAll("MEMBRE", "BENEVOLE", "ORGANISATEUR");
                roleComboBox.setValue("MEMBRE");
            }
        });

        // Connexion au serveur
        if (!conn.isConnected()) {
            boolean ok = conn.connect();
            if (!ok) {
                statusLabel.setText("❌ Impossible de joindre le serveur.");
                statusLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        }

        // Écoute des réponses du serveur
        conn.setMessageListener(this::handleServerResponse);
    }

    // -------------------------
    // Bouton Connexion
    // -------------------------

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        conn.send("LOGIN|" + username + "|" + password);
    }

    // -------------------------
    // Bouton Inscription
    // -------------------------

    @FXML
    private void handleRegister() {
        String username = usernameRegField.getText().trim();
        String password = passwordRegField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        conn.send("REGISTER|" + username + "|" + password + "|" + role);
    }

    // -------------------------
    // Traitement des réponses serveur
    // -------------------------

    private void handleServerResponse(String response) {
        String[] parts = response.split("\\|", -1);
        String type = parts[0];

        switch (type) {
            case "OK" -> {
                if (parts.length > 1 && parts[1].startsWith("Bienvenue")) {
                    // Connexion réussie → ouvrir la fenêtre principale
                    openChatWindow(usernameField.getText().trim());
                } else {
                    showSuccess(parts.length > 1 ? parts[1] : "Succès.");
                }
            }
            case "ERROR" -> showError(parts.length > 1 ? parts[1] : "Erreur inconnue.");
        }
    }

    // -------------------------
    // Ouverture de la fenêtre de chat
    // -------------------------

    private void openChatWindow(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sn/isi/chat_messagerie/hello-view.fxml"));
            Scene scene = new Scene(loader.load(), 900, 620);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            ChatController chatController = loader.getController();
            chatController.init(username);

            Stage stage = new Stage();
            stage.setTitle("Messagerie — " + username);
            stage.setScene(scene);
            stage.setOnCloseRequest(e -> conn.disconnect());
            stage.show();

            // Fermer la fenêtre de login
            ((Stage) usernameField.getScene().getWindow()).close();
        } catch (Exception e) {
            showError("Erreur ouverture chat : " + e.getMessage());
        }
    }

    // -------------------------
    // Utilitaires UI
    // -------------------------

    private void showError(String msg) {
        statusLabel.setText("❌ " + msg);
        statusLabel.setStyle("-fx-text-fill: #e74c3c;");
    }

    private void showSuccess(String msg) {
        statusLabel.setText("✅ " + msg);
        statusLabel.setStyle("-fx-text-fill: #27ae60;");
    }
}