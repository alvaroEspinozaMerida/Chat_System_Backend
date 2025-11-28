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

//TEMP "LOGIN"
        int port = 1234;
        UserDao userDao = new UserDao();         
        User currentUser;

        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Enter username you’d like to use: ");
        String username = scanner.nextLine().trim();
        
//        Built off old version of the CLI program; need to make sure to add wornings as 
//        pop ups instead of print statements 
        Client client = null; 
        
        try {
            currentUser = userDao.findOrCreateByUsername(username);

            currentUser.friends = new ArrayList<>(userDao.getFriends(currentUser.userID));

            System.out.println("Found / created user: " + currentUser.userName +
                    " (id " + currentUser.userID + ")");
        } catch (SQLException e) {
            System.out.println("Failed to connect to database. Exiting.");
            e.printStackTrace();
            return;
        }

        try {
            Socket socket = new Socket("localhost", port);
            client =  new Client(socket, currentUser, userDao);
            
        } catch (IOException e) {
            System.out.println("Could not connect to server on port " + port);
            e.printStackTrace();
        }

        
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("main_menu.fxml")
        );
        
        
        Parent root = loader.load();
        MainMenuController controller = loader.getController();
        controller.init(client);

        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Java TCP Chat - Alice");
        stage.setScene(scene);
        stage.show();
    }



    public static void main(String[] args) {
        launch(args);
    }


}
