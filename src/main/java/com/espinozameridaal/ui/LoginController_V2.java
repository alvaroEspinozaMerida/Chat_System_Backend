package com.espinozameridaal.ui;

//import com.espinozameridaal.modernui.services.AuthService;
import com.espinozameridaal.Client;
import com.espinozameridaal.Database.UserDao;
import com.espinozameridaal.Models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;

public class LoginController_V2 {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

//    private final AuthService authService = new AuthService();
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    @FXML
    private void handleLogin() throws SQLException {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        UserDao userDao = new UserDao();


        boolean ok = userDao.existsByUsername(username);
        Client client = null;

        if (!ok) {
            if (errorLabel != null) {
                errorLabel.setText("Invalid username or password");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
            return;
        }

        User currentUser;



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



//        create client
        int port = 1234;

        try {
            Socket socket = new Socket("localhost", port);
            client = new Client(socket, currentUser, userDao);

        } catch (Exception e) {
            System.out.println("Could not connect to server on port " + port);
            System.out.println("Try again when the server is up.");
            System.exit(0);
        }




        // Success -> open dashboard
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("main_menu.fxml")
            );
            Parent root = loader.load();
            MainMenuController controller = loader.getController();
            controller.init(client);


//            controller.setCurrentUsername(username);

            Scene scene = new Scene(root, 1100, 700);
//            scene.getStylesheets().add(
//                    getClass().getResource("/com/espinozameridaal/ui/css/style.css").toExternalForm()
//            );

            scene.getStylesheets().add(getClass().getResource("css/style.css").toExternalForm());


            if (stage == null) {
                stage = (Stage) usernameField.getScene().getWindow();
            }
            stage.setTitle(" Chat - " + username);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            if (errorLabel != null) {
                errorLabel.setText("Failed to load dashboard UI");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
            }
        }
    }
}
