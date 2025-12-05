package com.espinozameridaal.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private Label errorLabel;

    @FXML
    private void onLoginClicked() {
        String username = usernameField.getText().trim();
        // validate, connect to server, then switch to main_menu.fxml
    }
}
