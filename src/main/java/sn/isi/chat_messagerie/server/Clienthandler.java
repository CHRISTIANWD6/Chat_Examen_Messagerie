package sn.isi.chat_messagerie.server;
import com.association.messagerie.dao.MessageDAO;
import sn.isi.chat_messagerie.entity.UserDAO;
import sn.isi.chat_messagerie.entity.Message;
import sn.isi.chat_messagerie.entity.User;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;


public class Clienthandler {

    /**
     * Gère la communication avec UN client dans son propre thread (RG11).
     * Protocole texte simple : COMMANDE|param1|param2|...
     */
    public class ClientHandler implements Runnable {

        private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

        private final Socket socket;
        private final ConcurrentHashMap<String, ClientHandler> connectedClients;

        private BufferedReader in;
        private PrintWriter out;

        private User currentUser;

        private final UserDAO userDAO = new UserDAO();
        private final MessageDAO messageDAO = new MessageDAO();

        public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> connectedClients) {
            this.socket = socket;
            this.connectedClients = connectedClients;
        }

        // -------------------------
        // Boucle principale
        // -------------------------

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                String line;
                while ((line = in.readLine()) != null) {
                    handleCommand(line.trim());
                }

            } catch (IOException e) {
                logger.warning("Connexion perdue pour " +
                        (currentUser != null ? currentUser.getUsername() : "inconnu") + " : " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        // -------------------------
        // Routeur de commandes
        // -------------------------

        private void handleCommand(String line) {
            if (line.isEmpty()) return;

            String[] parts = line.split("\\|", -1);
            String command = parts[0].toUpperCase();

            switch (command) {
                case "REGISTER" -> handleRegister(parts);
                case "LOGIN"    -> handleLogin(parts);
                case "LOGOUT"   -> handleLogout();
                case "SEND"     -> handleSend(parts);
                case "HISTORY"  -> handleHistory(parts);
                case "LIST"     -> handleListUsers();
                default         -> send("ERROR|Commande inconnue : " + command);
            }
        }

        // -------------------------
        // REGISTER|username|password|role
        // -------------------------

        private void handleRegister(String[] parts) {
            if (parts.length < 4) { send("ERROR|Usage: REGISTER|username|password|role"); return; }

            String username = parts[1];
            String password = parts[2];
            String roleStr  = parts[3].toUpperCase();

            // Vérification du rôle
            User.Role role;
            try {
                role = User.Role.valueOf(roleStr);
            } catch (IllegalArgumentException e) {
                send("ERROR|Rôle invalide. Valeurs: ORGANISATEUR, MEMBRE, BENEVOLE");
                return;
            }

            // RG1 : username unique
            if (userDAO.findByUsername(username) != null) {
                send("ERROR|Ce nom d'utilisateur est déjà pris.");
                return;
            }

            // RG9 : hachage du mot de passe
            String hashedPassword = PasswordUtil.hash(password);
            User user = new User(username, hashedPassword, role);
            userDAO.save(user);

            logger.info("Inscription : " + username + " (" + role + ")");
            send("OK|Inscription réussie. Vous pouvez vous connecter.");
        }

        // -------------------------
        // LOGIN|username|password
        // -------------------------

        private void handleLogin(String[] parts) {
            if (parts.length < 3) { send("ERROR|Usage: LOGIN|username|password"); return; }

            String username = parts[1];
            String password = parts[2];

            User user = userDAO.findByUsername(username);

            // RG2 : vérification credentials
            if (user == null || !PasswordUtil.verify(password, user.getPassword())) {
                send("ERROR|Identifiants incorrects.");
                return;
            }

            // RG3 : un seul login à la fois
            if (connectedClients.containsKey(username)) {
                send("ERROR|Cet utilisateur est déjà connecté.");
                return;
            }

            // RG4 : statut ONLINE
            currentUser = user;
            currentUser.setStatus(User.Status.ONLINE);
            userDAO.update(currentUser);

            connectedClients.put(username, this);
            logger.info("Connexion : " + username + " — RG12");

            send("OK|Bienvenue " + username + "!");

            // Livraison des messages en attente (RG6)
            deliverPendingMessages();
        }

        // -------------------------
        // LOGOUT
        // -------------------------

        private void handleLogout() {
            if (currentUser == null) { send("ERROR|Vous n'êtes pas connecté."); return; }
            send("OK|Déconnexion réussie.");
            disconnect();
        }

        // -------------------------
        // SEND|receiverUsername|contenu
        // -------------------------

        private void handleSend(String[] parts) {
            // RG2 : authentification requise
            if (currentUser == null) { send("ERROR|Vous devez être connecté pour envoyer un message."); return; }
            if (parts.length < 3)    { send("ERROR|Usage: SEND|destinataire|contenu"); return; }

            String receiverUsername = parts[1];
            String contenu = parts[2];

            // RG7 : contenu non vide, max 1000 caractères
            if (contenu == null || contenu.isBlank()) {
                send("ERROR|Le message ne peut pas être vide."); return;
            }
            if (contenu.length() > 1000) {
                send("ERROR|Le message dépasse 1000 caractères."); return;
            }

            // RG5 : destinataire doit exister
            User receiver = userDAO.findByUsername(receiverUsername);
            if (receiver == null) {
                send("ERROR|Destinataire introuvable : " + receiverUsername); return;
            }

            // Création et sauvegarde du message
            Message message = new Message(currentUser, receiver, contenu);
            messageDAO.save(message);
            logger.info("Message envoyé de " + currentUser.getUsername() + " à " + receiverUsername + " — RG12");

            send("OK|Message envoyé.");

            // RG6 : livraison immédiate si destinataire connecté
            ClientHandler receiverHandler = connectedClients.get(receiverUsername);
            if (receiverHandler != null) {
                receiverHandler.send("MESSAGE|" + currentUser.getUsername() + "|" + contenu);
                message.setStatut(Message.Statut.RECU);
                messageDAO.update(message);
            }
            // Sinon, le message reste en BDD avec statut ENVOYE, livré à la prochaine connexion (RG6)
        }

        // -------------------------
        // HISTORY|otherUsername
        // -------------------------

        private void handleHistory(String[] parts) {
            // RG2
            if (currentUser == null) { send("ERROR|Vous devez être connecté."); return; }
            if (parts.length < 2)    { send("ERROR|Usage: HISTORY|username"); return; }

            String otherUsername = parts[1];
            User other = userDAO.findByUsername(otherUsername);
            if (other == null) { send("ERROR|Utilisateur introuvable."); return; }

            // RG8 : ordre chronologique
            List<Message> history = messageDAO.findConversation(currentUser, other);
            send("HISTORY_START|" + history.size());
            for (Message m : history) {
                send("MSG|" + m.getSender().getUsername() + "|" + m.getContenu() + "|" + m.getDateEnvoi());
            }
            send("HISTORY_END");
        }

        // -------------------------
        // LIST (RG13 : ORGANISATEUR uniquement)
        // -------------------------

        private void handleListUsers() {
            if (currentUser == null) { send("ERROR|Vous devez être connecté."); return; }

            if (currentUser.getRole() != User.Role.ORGANISATEUR) {
                send("ERROR|Accès refusé. Réservé aux ORGANISATEURS (RG13).");
                return;
            }

            List<User> users = userDAO.findAll();
            send("LIST_START|" + users.size());
            for (User u : users) {
                send("USER|" + u.getUsername() + "|" + u.getRole() + "|" + u.getStatus());
            }
            send("LIST_END");
        }

        // -------------------------
        // Livraison messages en attente (RG6)
        // -------------------------

        private void deliverPendingMessages() {
            List<Message> pending = messageDAO.findPendingMessages(currentUser);
            if (pending.isEmpty()) return;

            send("PENDING|" + pending.size() + " message(s) en attente.");
            for (Message m : pending) {
                send("MESSAGE|" + m.getSender().getUsername() + "|" + m.getContenu());
                m.setStatut(Message.Statut.RECU);
                messageDAO.update(m);
            }
        }

        // -------------------------
        // Déconnexion propre (RG4, RG10)
        // -------------------------

        private void disconnect() {
            if (currentUser != null) {
                currentUser.setStatus(User.Status.OFFLINE);
                userDAO.update(currentUser);
                connectedClients.remove(currentUser.getUsername());
                logger.info("Déconnexion : " + currentUser.getUsername() + " → OFFLINE — RG12");
                currentUser = null;
            }
            try { socket.close(); } catch (IOException ignored) {}
        }

        // -------------------------
        // Envoi au client
        // -------------------------

        public void send(String message) {
            if (out != null) out.println(message);
        }
    }
}
