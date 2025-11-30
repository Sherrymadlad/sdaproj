package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class CustomerProfilePageController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtPassword;
    @FXML private TextField txtAddress;
    @FXML private TextField txtPhone;
    @FXML private TextField txtEmail;

    @FXML private Button btnEditUsername;
    @FXML private Button btnEditPassword;
    @FXML private Button btnEditAddress;
    @FXML private Button btnEditPhone;
    @FXML private Button btnEditEmail;
    private int currentUserId; // dynamic

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadUserDetails(); // load profile once userId is set
    }

    @FXML
    public void initialize() {
        btnEditUsername.setOnAction(e -> editField("username", txtUsername));
        btnEditPassword.setOnAction(e -> editPassword());
        btnEditAddress.setOnAction(e -> editField("address", txtAddress));
        btnEditPhone.setOnAction(e -> editField("phone", txtPhone));
        btnEditEmail.setOnAction(e -> editField("email", txtEmail));
    }

    private void loadUserDetails() {
        if (currentUserId == 0) return; // safety check
        try {
            ResultSet rs = DBHelper.executeFunction("GetUserDetails", currentUserId);
            if (rs.next()) {
                txtUsername.setText(rs.getString("username"));
                txtPassword.setText("********"); // hide password
                txtAddress.setText(rs.getString("address"));
                txtPhone.setText(rs.getString("phone"));
                txtEmail.setText(rs.getString("email"));
            }
            rs.close();

            // Disable direct editing
            txtUsername.setEditable(false);
            txtPassword.setEditable(false);
            txtAddress.setEditable(false);
            txtPhone.setEditable(false);
            txtEmail.setEditable(false);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not load user details.");
        }
    }

    // Only send the values you want to update, pass null for the rest
    private void updateUserField(String username, String passwordhash, String address, String phone, String email) {
        String sql = "SELECT UpdateUserDetails(?, ?, ?, ?, ?, ?)";
        try (var conn = DBHelper.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, currentUserId);
            stmt.setString(2, username);
            stmt.setString(3, passwordhash);
            stmt.setString(4, address);
            stmt.setString(5, phone);
            stmt.setString(6, email);

            stmt.execute();  // Use execute() for functions that return VOID

            loadUserDetails();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Could not update user details.");
        }
    }



    private void editField(String fieldName, TextField field) {
        TextInputDialog dialog = new TextInputDialog(field.getText());
        dialog.setTitle("Edit " + fieldName);
        dialog.setHeaderText(null);
        dialog.setContentText("Enter new " + fieldName + ":");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newValue -> {
            switch (fieldName.toLowerCase()) {
                case "username" -> updateUserField(newValue, null, null, null, null);
                case "address" -> updateUserField(null, null, newValue, null, null);
                case "phone" -> updateUserField(null, null, null, newValue, null);
                case "email" -> updateUserField(null, null, null, null, newValue);
            }
            field.setText(newValue);
        });
    }

    private void editPassword() {
        Dialog<String[]> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText(null);

        ButtonType changeButtonType = new ButtonType("Change", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(changeButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        PasswordField oldPassword = new PasswordField();
        oldPassword.setPromptText("Old Password");
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("New Password");

        grid.add(new Label("Old Password:"), 0, 0);
        grid.add(oldPassword, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPassword, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == changeButtonType) {
                return new String[]{oldPassword.getText(), newPassword.getText()};
            }
            return null;
        });

        Optional<String[]> result = dialog.showAndWait();
        result.ifPresent(passwords -> {
            String oldPass = passwords[0];
            String newPass = passwords[1];

            try {
                String oldHashed = hashPassword(oldPass);

                // Verify old password
                ResultSet rs = DBHelper.executeFunction("VerifyUserPassword", currentUserId, oldHashed);
                if (rs.next() && rs.getBoolean("is_valid")) {
                    String newHashed = hashPassword(newPass);
                    updateUserField(null, newHashed, null, null, null);
                    txtPassword.setText("********");
                } else {
                    showAlert("Error", "Old password is incorrect.");
                }
                rs.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Could not update password.");
            }
        });
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
