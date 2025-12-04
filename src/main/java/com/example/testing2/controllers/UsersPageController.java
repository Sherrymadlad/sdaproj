package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

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
        usersList.getChildren().clear();

        String selectedRole = cbFilterRole.getValue();
        String sql = "All".equals(selectedRole) ? "SELECT * FROM GetAllUsers()"
                : "SELECT * FROM GetUsersByRole('" + selectedRole.replace("'", "''") + "')";

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int userid = rs.getInt("userid");
                String username = rs.getString("username");
                String role = rs.getString("rolename");
                String address = rs.getString("address") != null ? rs.getString("address") : "-";
                String phone = rs.getString("phone") != null ? rs.getString("phone") : "-";
                String email = rs.getString("email") != null ? rs.getString("email") : "-";

                // ===================== USER CARD =====================
                VBox card = new VBox(10);
                card.setPadding(new Insets(10));
                card.setStyle("-fx-background-color: #f3e6f7; " +
                        "-fx-border-color: #8b6fa1; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10;");

                // Header HBox
                HBox header = new HBox(10);
                header.setPadding(new Insets(5));
                header.setStyle("-fx-background-color: #f3e6f7;");

                Label lblUsername = new Label(username);
                lblUsername.setFont(new Font("Arial Bold", 16));
                lblUsername.setStyle("-fx-text-fill: #4b355a;"); // dark purple

                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                // Role ComboBox
                ComboBox<String> cbRole = new ComboBox<>();
                cbRole.getItems().addAll("Admin", "Manager", "Staff", "Customer");
                cbRole.setValue(role); // current role
                cbRole.setOnAction(e -> updateUserRole(userid, cbRole.getValue()));

                // Edit Info Button
                Button btnEdit = new Button("Edit Info");
                btnEdit.setOnAction(e -> openEditUserDialog(userid, username, address, phone, email));

                header.getChildren().addAll(lblUsername, spacer, cbRole, btnEdit);

                // Content VBox
                VBox content = new VBox(8);
                content.setPadding(new Insets(5));

                HBox hAddress = new HBox(10, new Label("Address:"), new Label(address));
                hAddress.getChildren().get(0).setStyle("-fx-font-weight: bold; -fx-text-fill: #4b355a;");
                hAddress.getChildren().get(1).setStyle("-fx-text-fill: #4b355a;");

                HBox hPhone = new HBox(10, new Label("Phone:"), new Label(phone));
                hPhone.getChildren().get(0).setStyle("-fx-font-weight: bold; -fx-text-fill: #4b355a;");
                hPhone.getChildren().get(1).setStyle("-fx-text-fill: #4b355a;");

                HBox hEmail = new HBox(10, new Label("Email:"), new Label(email));
                hEmail.getChildren().get(0).setStyle("-fx-font-weight: bold; -fx-text-fill: #4b355a;");
                hEmail.getChildren().get(1).setStyle("-fx-text-fill: #4b355a;");

                content.getChildren().addAll(hAddress, hPhone, hEmail);

                card.getChildren().addAll(header, content);

                usersList.getChildren().add(card);
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
        TextField tfAddress = new TextField(address);
        TextField tfPhone = new TextField(phone);
        TextField tfEmail = new TextField(email);

        Button btnSave = new Button("Save Changes");
        btnSave.setOnAction(e -> {
            updateUserDetails(userid,
                    tfUsername.getText(),
                    tfAddress.getText(),
                    tfPhone.getText(),
                    tfEmail.getText());
            stage.close();
            loadUsers();
        });

        root.getChildren().addAll(
                new Label("Username:"), tfUsername,
                new Label("Address:"), tfAddress,
                new Label("Phone:"), tfPhone,
                new Label("Email:"), tfEmail,
                btnSave
        );

        stage.setScene(new javafx.scene.Scene(root));
        stage.showAndWait();
    }

    private void updateUserDetails(int userid, String username, String address, String phone, String email) {
        String sql = "SELECT UpdateUserDetails(" +
                userid + ", '" + username.replace("'", "''") + "', NULL, '" +
                address.replace("'", "''") + "', '" + phone.replace("'", "''") + "', '" +
                email.replace("'", "''") + "');";

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            new Alert(Alert.AlertType.INFORMATION, "User details updated successfully!").showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update user details: " + ex.getMessage()).showAndWait();
        }
    }

    private void updateUserRole(int userid, String role) {
        String sql = "SELECT UpdateUserRole(" + userid + ", '" + role.replace("'", "''") + "');";
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            new Alert(Alert.AlertType.INFORMATION, "User role updated to " + role + " successfully!").showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to update user role: " + ex.getMessage()).showAndWait();
        }
    }
}
