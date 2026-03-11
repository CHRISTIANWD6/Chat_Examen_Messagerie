
module sn.isi.chat_messagerie {

        // JavaFX
        requires javafx.controls;
        requires javafx.fxml;

        // Hibernate & JPA
        requires jakarta.persistence;
        requires org.hibernate.orm.core;

        // BCrypt
        requires jbcrypt;

        // Logging Java
        requires java.logging;
    requires static lombok;

    // Nécessaire pour Hibernate (réflexion sur les entités)
        opens sn.isi.chat_messagerie.entity to org.hibernate.orm.core, javafx.base;
        opens sn.isi.chat_messagerie.dao to org.hibernate.orm.core;

        // Ouverture des contrôleurs JavaFX au FXMLLoader
        opens sn.isi.chat_messagerie.client to javafx.fxml;
        opens sn.isi.chat_messagerie to javafx.fxml;

        // Export des packages principaux
        exports sn.isi.chat_messagerie;
        exports sn.isi.chat_messagerie.entity;
        exports sn.isi.chat_messagerie.dao;
        exports sn.isi.chat_messagerie.server;
        exports sn.isi.chat_messagerie.client;
        }