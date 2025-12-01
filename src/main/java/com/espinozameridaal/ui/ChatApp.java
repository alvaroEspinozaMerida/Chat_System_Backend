package com.espinozameridaal.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class ChatApp extends Application {


    @Override
    public void start(Stage stage) throws Exception {

        stage.setOnCloseRequest(event -> {
            System.out.println("Window close requested — shutting down JVM.");
            System.exit(0);
        });

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("login_v2.fxml")
        );
        Parent root = loader.load();
        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(
                getClass().getResource("css/style.css").toExternalForm()
        );
        stage.setTitle("Modern Chat - Login");
        stage.setScene(scene);
        stage.show();


    }

    // fix app not closing
    @Override
    public void stop() throws Exception {
        System.out.println("Stopping ChatApp...");
        System.exit(0);
    }


    public static void main(String[] args) {
        launch(args);
    }


}
