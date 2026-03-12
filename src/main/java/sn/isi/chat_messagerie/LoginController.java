package sn.isi.chat_messagerie;

import sn.isi.chat_messagerie.client.ServerConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.logging.Logger;

/**
 * Contrôleur de la page de connexion / inscription.
 * Après un login réussi, ouvre la fenêtre Chat et la passe le rôle.
 */
public class LoginController {

    private static final Logger logger = Logger.getLogger(LoginController.class.getName());

    // ── Onglet Connexion
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         statusLabel;

    // ── Onglet Inscription
    @FXML private TextField     usernameRegField;
    @FXML private PasswordField passwordRegField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label         statusRegLabel;

    // ── TabPane (pour switcher d'onglet si besoin)
    @FXML private TabPane tabPane;

    private final ServerConnection conn = ServerConnection.getInstance();

    // ── Rôle reçu du serveur lors du login
    private String receivedRole = "MEMBRE";

    // ================================================================
    // FXML init — remplir la ComboBox des rôles
    // ================================================================

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (roleComboBox != null) {
                roleComboBox.getItems().setAll("MEMBRE", "BENEVOLE", "ORGANISATEUR");
                roleComboBox.getSelectionModel().selectFirst();
            }
        });
    }

    // ================================================================
    // Connexion
    // ================================================================

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Remplissez tous les champs.");
            return;
        }

        try {
            // (Re)connexion au serveur si nécessaire
            conn.connect();

            // Écouter la réponse du serveur
            conn.setMessageListener(response -> {
                Platform.runLater(() -> handleLoginResponse(response, username));
            });

            conn.send("LOGIN|" + username + "|" + password);

        } catch (Exception e) {
            showError("Connexion serveur impossible : " + e.getMessage());
        }
    }

    // Flag pour attendre OK puis ROLE avant d'ouvrir le chat
    private boolean loginOkReceived = false;

    private void handleLoginResponse(String response, String username) {
        String[] parts = response.split("\\|", -1);
        String   type  = parts[0];

        switch (type) {
            case "OK" -> {
                loginOkReceived = true;
                receivedRole = "MEMBRE"; // valeur par défaut si ROLE n'arrive pas
                // On attend le message ROLE juste après, mais on ouvre quand même
                logger.info("[RG12] Login OK : " + username);
                // Petit délai pour laisser arriver le message ROLE
                new Thread(() -> {
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> openChatWindow(username, receivedRole));
                }).start();
            }
            case "ROLE" -> {
                // Reçu juste après OK — mettre à jour le rôle
                receivedRole = parts.length > 1 ? parts[1] : "MEMBRE";
                logger.info("[RG12] Rôle reçu : " + username + " = " + receivedRole);
            }
            case "ERROR" -> {
                loginOkReceived = false;
                String msg = parts.length > 1 ? parts[1] : "Identifiants incorrects.";
                showError("❌ " + msg);
            }
        }
    }

    // ================================================================
    // Inscription
    // ================================================================

    @FXML
    private void handleRegister() {
        String username = usernameRegField.getText().trim();
        String password = passwordRegField.getText();
        String role     = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || role == null) {
            showRegError("Remplissez tous les champs.");
            return;
        }
        if (password.length() < 4) {
            showRegError("Mot de passe trop court (min 4 caractères).");
            return;
        }

        try {
            conn.connect();

            conn.setMessageListener(response -> {
                Platform.runLater(() -> {
                    String[] parts = response.split("\\|", -1);
                    if ("OK".equals(parts[0])) {
                        showRegSuccess("✅ Compte créé ! Connectez-vous.");
                    } else {
                        String msg = parts.length > 1 ? parts[1] : "Erreur inscription.";
                        showRegError("❌ " + msg);
                    }
                });
            });

            conn.send("REGISTER|" + username + "|" + password + "|" + role);

        } catch (Exception e) {
            showRegError("Connexion serveur impossible : " + e.getMessage());
        }
    }

    // ================================================================
    // Ouvrir la fenêtre Chat avec le rôle
    // ================================================================

    private void openChatWindow(String username, String role) {
        try {
            java.net.URL fxmlUrl = getClass().getResource("/sn/isi/chat_messagerie/hello-view.fxml");
            if (fxmlUrl == null) fxmlUrl = getClass().getResource("/fxml/Chat.fxml");
            if (fxmlUrl == null) {
                showError("Chat.fxml introuvable.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Scene      scene  = new Scene(loader.load(), 960, 640);

            java.net.URL cssUrl = getClass().getResource("/sn/isi/chat_messagerie/css/style.css");
            if (cssUrl == null) cssUrl = getClass().getResource("/css/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());

            ChatController chatController = loader.getController();
            chatController.init(username, role);   // ← passage du rôle

            Stage chatStage = new Stage();
            chatStage.setTitle("Messagerie — " + username + " (" + role + ")");
            chatStage.setScene(scene);
            chatStage.setOnCloseRequest(e -> conn.disconnect());
            chatStage.show();

            // Fermer la fenêtre de connexion
            ((Stage) usernameField.getScene().getWindow()).close();

        } catch (Exception e) {
            showError("Erreur : " + e.getClass().getSimpleName() + " — " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================================================================
    // Helpers UI
    // ================================================================

    private void showError(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    private void showRegError(String msg) {
        if (statusRegLabel != null) {
            statusRegLabel.setText(msg);
            statusRegLabel.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    private void showRegSuccess(String msg) {
        if (statusRegLabel != null) {
            statusRegLabel.setText(msg);
            statusRegLabel.setStyle("-fx-text-fill: #22c55e;");
        }
    }
}