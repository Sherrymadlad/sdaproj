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

public class StaffDashboardController {

    @FXML
    private void goToHandleReturn(ActionEvent event) {
        loadFXML(event, "handle_return.fxml");
    }

    @FXML
    private void goToUpdateStatus(ActionEvent event) {
        loadFXML(event, "UpdateStatus.fxml");
    }

    @FXML
    private void goToRecordOrder(ActionEvent event) {
        loadFXML(event, "RecordOrder.fxml");
    }

    @FXML
    private void goToViewStocks(ActionEvent event) {
        loadFXML(event, "ViewStocks.fxml");
    }

    private void loadFXML(ActionEvent event, String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(
                    "/com/example/testing2/" + fxmlFile
            ));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "Failed to load " + fxmlFile);
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
