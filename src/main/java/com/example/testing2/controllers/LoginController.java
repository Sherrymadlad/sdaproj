package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper; // <-- corrected import
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML private TextField txtLoginName;
    @FXML private TextField txtLoginPassword;
    @FXML private Button btnLogin;

    @FXML
    private void initialize() {
        btnLogin.setOnAction(e -> handleLogin());
    }

    private void handleLogin() {
        String username = txtLoginName.getText().trim();
        String password = txtLoginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please enter both username and password.");
            return;
        }

        String hashedPassword = hashPassword(password);

        try (ResultSet rs = DBHelper.executeFunction("Login", username, hashedPassword)) { // <-- updated
            if (rs.next()) {
                String name = rs.getString("username");
                showAlert(AlertType.INFORMATION, "Login Successful", "Welcome, " + name + "!");
                clearFields();
                // TODO: redirect to dashboard
            } else {
                showAlert(AlertType.ERROR, "Login Failed", "Incorrect username or password.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "An error occurred while logging in.");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            BigInteger number = new BigInteger(1, hashBytes);
            StringBuilder hexString = new StringBuilder(number.toString(16));

            while (hexString.length() < 64) {
                hexString.insert(0, '0');
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void showAlert(AlertType type, String title, String message) {
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
