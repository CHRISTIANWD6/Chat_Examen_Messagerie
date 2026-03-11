# 💬 Chat_Messagerie — Application de Messagerie pour Associations

Application de messagerie instantanée client-serveur développée en Java 17 avec JavaFX, Hibernate et PostgreSQL.

---

## 📋 Prérequis

Avant de lancer le projet, assurez-vous d'avoir installé :

| Outil | Version | Lien |
|-------|---------|------|
| JDK | 17 ou supérieur | https://www.oracle.com/java/technologies/downloads/#java17 |
| PostgreSQL | 14 ou supérieur | https://www.postgresql.org/download/ |
| IntelliJ IDEA | Toute version | https://www.jetbrains.com/idea/download/ |
| Git | Toute version | https://git-scm.com/ |

---

## 🚀 Installation

### 1. Cloner le projet

```bash
git clone https://github.com/votre-compte/Chat_Messagerie.git
cd Chat_Messagerie
```

### 2. Créer la base de données PostgreSQL

Ouvrez **pgAdmin** ou **psql** et créez la base :

```sql
CREATE DATABASE messagerie_db;
```

### 3. Configurer la connexion à la base de données

Ouvrez le fichier :
```
src/main/resources/META-INF/persistence.xml
```

Modifiez ces deux lignes avec vos identifiants PostgreSQL :

```xml
<property name="jakarta.persistence.jdbc.user"     value="postgres"/>
<property name="jakarta.persistence.jdbc.password" value="VOTRE_MOT_DE_PASSE"/>
```

### 4. Ouvrir dans IntelliJ IDEA

1. **File → Open** → sélectionnez le dossier du projet
2. IntelliJ détecte automatiquement le `pom.xml` Maven
3. Cliquez **"Load Maven Project"** si demandé
4. Attendez que Maven télécharge toutes les dépendances (~1 minute)

---

## ▶️ Lancer l'application

> ⚠️ **Important** : toujours lancer le Serveur **avant** le Client.

### Étape 1 — Créer la configuration du Serveur

1. Cliquez sur **"Edit Configurations..."** (menu en haut à droite)
2. Cliquez sur **"+"** → **"Application"**
3. Remplissez :
   - **Name** : `Serveur`
   - **Main class** : `sn.isi.chat_messagerie.server.Server`
4. Cliquez **OK**

### Étape 2 — Lancer dans l'ordre

```
1. ▶ Serveur          → attendre "Serveur prêt. En attente de connexions..."
2. ▶ HelloApplication → l'interface de connexion s'ouvre
```

Les tables `users` et `messages` sont créées **automatiquement** par Hibernate au premier lancement.

---

## 🗂️ Structure du projet

```
src/main/
├── java/sn/isi/chat_messagerie/
│   ├── server/
│   │   ├── Server.java           # Serveur principal (port 5000)
│   │   ├── ClientHandler.java    # Gestion d'un client par thread
│   │   └── PasswordUtil.java     # Hachage BCrypt des mots de passe
│   ├── dao/
│   │   ├── HibernateUtil.java    # Factory EntityManager (singleton)
│   │   ├── UserDAO.java          # Opérations CRUD sur User
│   │   └── MessageDAO.java       # Opérations CRUD sur Message
│   ├── entity/
│   │   ├── User.java             # Entité JPA Utilisateur
│   │   └── Message.java          # Entité JPA Message
│   └── client/
│       ├── ServerConnection.java         # Connexion Socket (singleton)
│       └── controller/
│           ├── LoginController.java      # Écran connexion/inscription
│           └── ChatController.java       # Écran principal de chat
└── resources/
    ├── chat_messagerie/
    │   ├── Login.fxml            # Interface de connexion
    │   └── hello-view.fxml             # Interface de chat
    ├── css/
    │   └── style.css             # Thème sombre
    └── META-INF/
        └── persistence.xml       # Configuration Hibernate/PostgreSQL
```

---

## 🔧 Technologies utilisées

| Technologie | Rôle |
|-------------|------|
| Java 17 | Langage principal |
| JavaFX 17 | Interface graphique |
| Sockets Java | Communication client-serveur temps réel |
| Hibernate 6 / JPA | Persistance des données (ORM) |
| PostgreSQL | Base de données |
| BCrypt | Hachage des mots de passe |
| Maven | Gestion des dépendances |

---

## 📡 Protocole de communication

L'application utilise un protocole texte simple via Sockets :

| Commande | Description | Accès |
|----------|-------------|-------|
| `REGISTER\|username\|password\|role` | Inscription | Tous |
| `LOGIN\|username\|password` | Connexion | Tous |
| `LOGOUT` | Déconnexion | Connectés |
| `SEND\|destinataire\|contenu` | Envoyer un message | Connectés |
| `HISTORY\|username` | Historique d'une conversation | Connectés |
| `MEMBERS` | Liste de tous les membres | Connectés |
| `LIST` | Liste complète avec rôles | ORGANISATEUR uniquement |

---

## 👥 Rôles utilisateurs

| Rôle | Droits |
|------|--------|
| `MEMBRE` | Connexion, envoi/réception de messages |
| `BENEVOLE` | Connexion, envoi/réception de messages |
| `ORGANISATEUR` | Tous les droits + liste complète des membres (RG13) |

---

## ⚙️ Configuration réseau

Par défaut le serveur tourne sur **localhost:5000**.

Pour changer l'adresse/port, modifiez ces constantes :

- **Serveur** : `Server.java` → `private static final int PORT = 5000;`
- **Client** : `ServerConnection.java` → `private static final String HOST = "localhost";` et `PORT = 5000;`

---

## 🧑‍💻 Auteur

Projet réalisé dans le cadre du cours de Génie Logiciel — ISI Sénégal.
