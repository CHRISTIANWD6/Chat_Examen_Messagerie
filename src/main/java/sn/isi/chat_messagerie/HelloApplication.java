package sn.isi.chat_messagerie;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/sn/isi/chat_messagerie/login.fxml"));
        Scene scene = new Scene(loader.load(), 420, 520);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("Messagerie — Association");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
