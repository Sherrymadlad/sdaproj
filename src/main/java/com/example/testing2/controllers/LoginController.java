package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class LoginController {

    @FXML private TextField txtLoginName;
    @FXML private TextField txtLoginPassword;
    @FXML private Button btnLogin;

    @FXML
    private void initialize() {
        btnLogin.setOnAction(event -> handleLogin());
    }

    private void handleLogin() {
        String username = txtLoginName.getText().trim();
        String password = txtLoginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Fields", "Please enter both username and password.");
            return;
        }

        String hashedPassword = hashPassword(password);

        try (ResultSet rs = DBHelper.executeFunction("Login", username, hashedPassword)) {

            if (rs.next()) {
                String name = rs.getString("username");

                showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome, " + name + "!");
                clearFields();
                int userId = rs.getInt("userid");
                openCustomerMainPage(userId);

                // TODO: Open the next page here
                // openDashboard(userId);

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
    private void openCustomerMainPage(int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomerMainPage.fxml"));
            Parent root = loader.load();

            // Get controller of the next page
            CustomerMainPageController controller = loader.getController();
            controller.setCurrentUserId(userId); // pass logged in user id

            // Open new window
            Stage stage = new Stage();
            stage.setTitle("Customer Main Page");
            stage.setScene(new Scene(root));
            stage.show();

            // Close login window
            Stage current = (Stage) btnLogin.getScene().getWindow();
            current.close();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Unable to load main page.");
        }
    }

}
