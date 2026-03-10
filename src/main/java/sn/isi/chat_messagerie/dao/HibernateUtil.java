package sn.isi.chat_messagerie.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;


public class HibernateUtil {
    /**
     * Utilitaire Hibernate — fournit un EntityManagerFactory unique (singleton).
     * Le nom "messageriePU" doit correspondre à celui dans persistence.xml.
     */

        private static final EntityManagerFactory emf =
                Persistence.createEntityManagerFactory("messageriePU");

        private HibernateUtil() {}

        public static EntityManager getEntityManager() {
            return emf.createEntityManager();
        }

        public static void close() {
            if (emf != null && emf.isOpen()) {
                emf.close();
            }
        }
    }


