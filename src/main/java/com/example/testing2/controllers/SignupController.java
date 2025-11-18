package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper; // <-- correct import
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class SignupController {

    @FXML private TextField txtName;
    @FXML private TextField txtPassword;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhoneNo;
    @FXML private TextField txtAddress;

    @FXML private CheckBox btnAdmin;
    @FXML private CheckBox btnManager;
    @FXML private CheckBox btnStaff;
    @FXML private CheckBox btnCustomer;

    @FXML private Button btnSignup;

    @FXML
    private void initialize() {
        // Ensure only one role can be selected at a time
        btnAdmin.setOnAction(e -> deselectOthers(btnAdmin));
        btnManager.setOnAction(e -> deselectOthers(btnManager));
        btnStaff.setOnAction(e -> deselectOthers(btnStaff));
        btnCustomer.setOnAction(e -> deselectOthers(btnCustomer));

        // Handle signup button click
        btnSignup.setOnAction(e -> handleSignup());
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

        // Validate fields
        if (name.isEmpty() || password.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Error", "Please fill in all fields.");
            return;
        }

        String role = null;
        if (btnAdmin.isSelected()) role = "Admin";
        else if (btnManager.isSelected()) role = "Manager";
        else if (btnStaff.isSelected()) role = "Staff";
        else if (btnCustomer.isSelected()) role = "Customer";

        if (role == null) {
            showAlert(AlertType.WARNING, "Validation Error", "Please select a role.");
            return;
        }

        // Hash the password
        String hashedPassword = hashPassword(password);

        try {
            // Call stored procedure using the new DBHelper
            DBHelper.executeFunction("SignUpUser", name, hashedPassword, address, phone, email, role);
            showAlert(AlertType.INFORMATION, "Success", "User signed up successfully!");
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Failed to signup user. Check console for details.");
        }
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            BigInteger number = new BigInteger(1, hashBytes);
            StringBuilder hexString = new StringBuilder(number.toString(16));

            // Pad with leading zeros if necessary
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
}
