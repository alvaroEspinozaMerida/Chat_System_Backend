package com.espinozameridaal.ui;

import com.espinozameridaal.Client;
import com.espinozameridaal.Database.UserDao;
import com.espinozameridaal.Models.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;

public class ChatApp extends Application {


    @Override
    public void start(Stage stage) throws Exception {

        stage.setOnCloseRequest(event -> {
            System.out.println("Window close requested — shutting down JVM.");
            System.exit(0);
        });

//        int port = 1234;
//        UserDao userDao = new UserDao();
//        User currentUser;
//
//        Scanner scanner = new Scanner(System.in);
//
//        System.out.println("Enter username: ");
//        String username = scanner.nextLine().trim();
//
//
//        Client client = null;
//
//        try {
//            currentUser = userDao.findOrCreateByUsername(username);
//
//            currentUser.friends = new ArrayList<>(userDao.getFriends(currentUser.userID));
//
//            System.out.println("Found / created user: " + currentUser.userName +
//                    " (id " + currentUser.userID + ")");
//        } catch (SQLException e) {
//            System.out.println("Failed to connect to database. Exiting.");
//            e.printStackTrace();
//            return;
//        }
//
//        try {
//            Socket socket = new Socket("localhost", port);
//            client = new Client(socket, currentUser, userDao);
//
//        } catch (Exception e) {
//            System.out.println("Could not connect to server on port " + port);
//            System.out.println("Try again when the server is up.");
//            System.exit(0);
//        }
//
//
//        FXMLLoader loader = new FXMLLoader(
//                getClass().getResource("main_menu.fxml")
//        );
//
//
//        Parent root = loader.load();
//        MainMenuController controller = loader.getController();
//        controller.init(client);
//
//        Scene scene = new Scene(root, 800, 600);
//        scene.getStylesheets().add(getClass().getResource("css/style.css").toExternalForm());
//        stage.setTitle("Java TCP Chat");
//        stage.setScene(scene);
//        stage.show();

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
