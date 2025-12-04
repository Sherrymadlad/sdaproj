package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class HandleReturnController {

    @FXML
    private void goToCustomerReturns(ActionEvent event) {
        loadFXML(event, "CustomerReturn.fxml");
    }

    @FXML
    private void goToSupplierReturns(ActionEvent event) {
        loadFXML(event, "SupplierReturn.fxml");
    }

    @FXML
    private void goBack(ActionEvent event) {
        loadFXML(event, "StaffDashboard.fxml");
    }

    private void loadFXML(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/testing2/" + fxmlFile)
            );

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to load: " + fxmlFile);
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
