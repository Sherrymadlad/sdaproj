package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.awt.event.ActionEvent;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML private AnchorPane mainRoot; // Root of login page to overlay modal
    @FXML private TextField txtLoginName;
    @FXML private TextField txtLoginPassword;
    @FXML private Button btnLogin;
    @FXML private Text txtGoToSignup;

    private CustomModalController customModalController;

    @FXML
    private void initialize() {
        btnLogin.setOnAction(this::handleLogin);
        txtGoToSignup.setOnMouseClicked(e -> openSignupPage());

        // Load Custom Modal
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomModal.fxml"));
            StackPane modalRoot = loader.load();
            customModalController = loader.getController();

            // Add modal to main root and anchor it to fill the parent
            mainRoot.getChildren().add(modalRoot);
            AnchorPane.setTopAnchor(modalRoot, 0.0);
            AnchorPane.setBottomAnchor(modalRoot, 0.0);
            AnchorPane.setLeftAnchor(modalRoot, 0.0);
            AnchorPane.setRightAnchor(modalRoot, 0.0);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openSignupPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/SignupPage.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Signup");
            stage.show();

            Stage current = (Stage) txtGoToSignup.getScene().getWindow();
            current.close();

        } catch (Exception e) {
            e.printStackTrace();
            showCustomModal("Unable to open signup page.");
        }
    }

    private void handleLogin(javafx.event.ActionEvent event) {
        String username = txtLoginName.getText().trim();
        String password = txtLoginPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showCustomModal("Please enter both username and password.");
            return;
        }

        btnLogin.setText("Logging in...");
        btnLogin.setDisable(true);

        String hashedPassword = hashPassword(password);

        Task<Boolean> loginTask = new Task<>() {
            private int userId = -1;
            private String name = null;

            @Override
            protected Boolean call() {
                try (ResultSet rs = DBHelper.executeFunction("Login", username, hashedPassword)) {
                    if (rs.next()) {
                        name = rs.getString("username");
                        userId = rs.getInt("userid");
                        return true;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
                return false;
            }

            @Override
            protected void succeeded() {
                boolean success = getValue();
                if (success) {
                    showCustomModal("Welcome, " + name + "!");
                    clearFields();
                    openCustomerMainPage(userId);
                } else {
                    showCustomModal("Incorrect username or password.");
                }
                btnLogin.setText("Login");
                btnLogin.setDisable(false);
            }

            @Override
            protected void failed() {
                showCustomModal("An error occurred while trying to log in.");
                btnLogin.setText("Login");
                btnLogin.setDisable(false);
            }
        };

        new Thread(loginTask).start();
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

    private void clearFields() {
        txtLoginName.clear();
        txtLoginPassword.clear();
    }

    private void openCustomerMainPage(int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomerMainPage.fxml"));
            Parent root = loader.load();

            CustomerMainPageController controller = loader.getController();
            controller.setCurrentUserId(userId);

            Stage primaryStage = (Stage) btnLogin.getScene().getWindow();
            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setMinHeight(600);
            primaryStage.setMinWidth(800);
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showCustomModal("Unable to load main page.");
        }
    }

    private void showCustomModal(String message) {
        if (customModalController != null) {
            customModalController.showMessage(message);
        } else {
            System.err.println("Custom modal not initialized: " + message);
        }
    }
}
