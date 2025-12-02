package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper; // your DB helper class
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.text.Font;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;

public class UsersPageController {

    @FXML
    private VBox usersList;

    @FXML
    private ComboBox<String> cbFilterRole;

    @FXML
    private Button btnClearFilter;

    public void initialize() {
        // Populate filter dropdown
        cbFilterRole.getItems().addAll("All", "Admin", "Manager", "Staff", "Customer");
        cbFilterRole.setValue("All"); // default

        // Reload users whenever filter changes
        cbFilterRole.setOnAction(e -> loadUsers());

        btnClearFilter.setOnAction(e -> {
            cbFilterRole.setValue("All");
            loadUsers();
        });

        loadUsers();
    }
    private void loadUsers() {
        usersList.getChildren().clear(); // clear existing users

        String selectedRole = cbFilterRole.getValue();
        String sql;
        if ("All".equals(selectedRole)) {
            sql = "SELECT * FROM GetAllUsers()";
        } else {
            sql = "SELECT * FROM GetUsersByRole('" + selectedRole.replace("'", "''") + "')";
        }

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) { // ✅ use dynamic SQL
            while (rs.next()) {
                int userid = rs.getInt("userid");
                String username = rs.getString("username");
                String role = rs.getString("rolename");
                String address = rs.getString("address") != null ? rs.getString("address") : "-";
                String phone = rs.getString("phone") != null ? rs.getString("phone") : "-";
                String email = rs.getString("email") != null ? rs.getString("email") : "-";

                // ===================== TITLED PANE =====================
                TitledPane tp = new TitledPane();
                tp.setExpanded(false);
                tp.setStyle("-fx-background-color: #f3e6f7;" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;");

                // ===================== HEADER =====================
                HBox header = new HBox(10);
                header.setPadding(new Insets(10));
                header.setStyle("-fx-background-color: #f3e6f7;");

                Label lblUsername = new Label(username);
                lblUsername.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Role ComboBox
                ComboBox<String> cbRole = new ComboBox<>();
                cbRole.getItems().addAll("Admin", "Manager", "Staff", "Customer");
                cbRole.setValue(role); // default to current role
                cbRole.setOnAction(e -> updateUserRole(userid, cbRole.getValue()));

                // Edit Info Button
                Button btnEdit = new Button("Edit Info");
                btnEdit.setOnAction(e -> openEditUserDialog(userid, username, address, phone, email));

                header.getChildren().addAll(lblUsername, spacer, cbRole, btnEdit);
                tp.setGraphic(header);

                // ===================== CONTENT =====================
                VBox content = new VBox(12);
                content.setPadding(new Insets(10, 15, 10, 15));
                content.setStyle("-fx-background-color: #f3e6f7;");

                HBox hAddress = new HBox(10, new Label("Address:"), new Label(address));
                hAddress.getChildren().get(0).setStyle("-fx-font-weight: bold;");

                HBox hPhone = new HBox(10, new Label("Phone:"), new Label(phone));
                hPhone.getChildren().get(0).setStyle("-fx-font-weight: bold;");

                HBox hEmail = new HBox(10, new Label("Email:"), new Label(email));
                hEmail.getChildren().get(0).setStyle("-fx-font-weight: bold;");

                content.getChildren().addAll(hAddress, hPhone, hEmail);
                tp.setContent(content);

                // Add TitledPane to VBox
                usersList.getChildren().add(tp);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void openEditUserDialog(int userid, String username, String address, String phone, String email) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Edit User Info");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        TextField tfUsername = new TextField(username);
        tfUsername.setPromptText("Username");

        TextField tfAddress = new TextField(address);
        tfAddress.setPromptText("Address");

        TextField tfPhone = new TextField(phone);
        tfPhone.setPromptText("Phone");

        TextField tfEmail = new TextField(email);
        tfEmail.setPromptText("Email");

        Button btnSave = new Button("Save Changes");
        btnSave.setOnAction(e -> {
            updateUserDetails(userid,
                    tfUsername.getText(),
                    tfAddress.getText(),
                    tfPhone.getText(),
                    tfEmail.getText());
            stage.close();
            usersList.getChildren().clear();
            loadUsers(); // reload users
        });

        root.getChildren().addAll(new Label("Username:"), tfUsername,
                new Label("Address:"), tfAddress,
                new Label("Phone:"), tfPhone,
                new Label("Email:"), tfEmail,
                btnSave);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.showAndWait();
    }
    private void updateUserDetails(int userid, String username, String address, String phone, String email) {
        String sql = "SELECT UpdateUserDetails(" +
                userid + ", " +
                "'" + username.replace("'", "''") + "', " +
                "NULL, " + // password hash not updated here
                "'" + address.replace("'", "''") + "', " +
                "'" + phone.replace("'", "''") + "', " +
                "'" + email.replace("'", "''") + "'" +
                ");";

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            // Success alert
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("User details updated successfully!");
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();

            // Error alert
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to update user details: " + ex.getMessage());
            alert.showAndWait();
        }
    }
    private void updateUserRole(int userid, String role) {
        String sql = "SELECT UpdateUserRole(" + userid + ", '" + role.replace("'", "''") + "');";
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            // Success alert
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("User role updated to " + role + " successfully!");
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();

            // Error alert
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to update user role: " + ex.getMessage());
            alert.showAndWait();
        }
    }


}
