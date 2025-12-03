package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import com.example.testing2.utils.Router;
import com.example.testing2.utils.DataReceiver;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.text.Text;
import javafx.event.ActionEvent;
import javafx.stage.Stage;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField txtLoginName;
    @FXML private TextField txtLoginPassword;
    @FXML private Button btnLogin;
    @FXML private Text txtGoToSignup;

    @FXML
    private void initialize() {
        btnLogin.setOnAction(event -> handleLogin(event));

        // --- Routing to SignupPage ---
        txtGoToSignup.setOnMouseClicked(e -> {
            try {
                // Use Router to open SignupPage
                Router.openNewWindow("SignupPage.fxml", "Signup", 800, 600);

                // Close current login window
                Stage current = (Stage) txtGoToSignup.getScene().getWindow();
                current.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Navigation Error", "Unable to open Signup page.");
            }
        });
    }

    private void handleLogin(ActionEvent event) {
        String username = txtLoginName.getText().trim();
        String password = txtLoginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "Please enter both username and password.");
            return;
        }

        String hashedPassword = hashPassword(password);

        try (ResultSet rs = DBHelper.executeFunction("Login", username, hashedPassword)) {

            if (rs.next()) {
                String role = rs.getString("role"); // assuming DB returns role column
                int userId = rs.getInt("userid");

                showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + username + "!");
                clearFields();

                // Wrap Router calls in try-catch to handle Exception
                try {
                    switch (role.toLowerCase()) {
                        case "admin":
                            Router.navigate(event, "AdminDashboardPage.fxml");
                            break;
                        case "staff":
                            Router.navigate(event, "StaffDashboard.fxml");
                            break;
                        case "customer":
                            Router.navigateWithData(event, "CustomerMainPage.fxml", userId);
                            break;
                        default:
                            showAlert(Alert.AlertType.ERROR, "Error", "Invalid role");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "Navigation Error", "Failed to open the page.");
                }

            } else {
                showAlert(Alert.AlertType.ERROR, "Invalid Credentials", "Incorrect username or password.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while trying to log in.");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            BigInteger number = new BigInteger(1, hashBytes);

            StringBuilder hex = new StringBuilder(number.toString(16));
            while (hex.length() < 64) {
                hex.insert(0, '0');
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available.", e);
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        txtLoginName.clear();
        txtLoginPassword.clear();
    }
}
