package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class UpdateStatusController {

    @FXML private TextField txtOrderId;
    @FXML private ChoiceBox<String> choiceStatus;
    @FXML private Label lblResult;

    @FXML
    private void initialize() {
        choiceStatus.getItems().addAll("PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");
    }

    @FXML
    private void handleUpdateStatus(ActionEvent event) {
        lblResult.setText("Order " + txtOrderId.getText() + " updated to " + choiceStatus.getValue());
    }

    @FXML
    private void handleGoBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(
                    "/com/example/testing2/StaffDashboard.fxml"
            ));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to navigate back.");
        }
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
