module sn.isi.chat_messagerie {
    requires javafx.controls;
    requires javafx.fxml;


    opens sn.isi.chat_messagerie to javafx.fxml;
    exports sn.isi.chat_messagerie;
}