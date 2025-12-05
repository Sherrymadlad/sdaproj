package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.scene.control.PasswordField;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class SignupController {

    @FXML private AnchorPane mainRoot;
    @FXML private TextField txtName;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhoneNo;
    @FXML private TextField txtAddress;

    @FXML private CheckBox btnAdmin;
    @FXML private CheckBox btnManager;
    @FXML private CheckBox btnStaff;
    @FXML private CheckBox btnCustomer;

    @FXML private Button btnSignup;
    @FXML private Text txtGoToLogin;

    private CustomModalController customModalController;

    @FXML
    private void initialize() {
        // Ensure only one role can be selected at a time
        btnAdmin.setOnAction(e -> deselectOthers(btnAdmin));
        btnManager.setOnAction(e -> deselectOthers(btnManager));
        btnStaff.setOnAction(e -> deselectOthers(btnStaff));
        btnCustomer.setOnAction(e -> deselectOthers(btnCustomer));

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomModal.fxml"));
            StackPane modalRoot = loader.load();
            customModalController = loader.getController();


            mainRoot.getChildren().add(modalRoot);
            AnchorPane.setTopAnchor(modalRoot, 0.0);
            AnchorPane.setBottomAnchor(modalRoot, 0.0);
            AnchorPane.setLeftAnchor(modalRoot, 0.0);
            AnchorPane.setRightAnchor(modalRoot, 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }


        btnSignup.setOnAction(e -> handleSignup());
        txtGoToLogin.setOnMouseClicked(e -> openLoginPage());
    }

    private void openLoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/LoginPage.fxml"));
            AnchorPane root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("Login");
            stage.show();

            Stage current = (Stage) txtGoToLogin.getScene().getWindow();
            current.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            showCustomModal("Unable to open login page.");
        }
    }

    private void deselectOthers(CheckBox selected) {
        if (selected != btnAdmin) btnAdmin.setSelected(false);
        if (selected != btnManager) btnManager.setSelected(false);
        if (selected != btnStaff) btnStaff.setSelected(false);
        if (selected != btnCustomer) btnCustomer.setSelected(false);
    }

    private void handleSignup() {
        String name = txtName.getText().trim();
        String password = txtPassword.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhoneNo.getText().trim();
        String address = txtAddress.getText().trim();


        if (name.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showCustomModal("Please fill in all fields.");
            return;
        }


        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showCustomModal("Invalid email format.");
            return;
        }

        if (!phone.matches("\\d{10,15}")) {
            showCustomModal("Phone number must be 10–15 digits.");
            return;
        }


        if (password.length() < 8) {
            showCustomModal("Password must be at least 8 characters long.");
            return;
        }


        String role = null;
        if (btnAdmin.isSelected()) role = "Admin";
        else if (btnManager.isSelected()) role = "Manager";
        else if (btnStaff.isSelected()) role = "Staff";
        else if (btnCustomer.isSelected()) role = "Customer";

        if (role == null) {
            showCustomModal("Please select a role.");
            return;
        }


        String hashedPassword = hashPassword(password);

        try {
            DBHelper.executeFunction("SignUpUser", name, hashedPassword, address, phone, email, role);
            showCustomModal("User signed up successfully!");
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
            showCustomModal("Failed to signup user. Check console for details.");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            BigInteger number = new BigInteger(1, hashBytes);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 64) hexString.insert(0, '0');
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void clearFields() {
        txtName.clear();
        txtPassword.clear();
        txtEmail.clear();
        txtPhoneNo.clear();
        txtAddress.clear();
        btnAdmin.setSelected(false);
        btnManager.setSelected(false);
        btnStaff.setSelected(false);
        btnCustomer.setSelected(false);
    }

    private void showCustomModal(String message) {
        if (customModalController != null) {
            customModalController.showMessage(message);
        } else {
            System.err.println("Custom modal not initialized: " + message);
        }
    }
}



