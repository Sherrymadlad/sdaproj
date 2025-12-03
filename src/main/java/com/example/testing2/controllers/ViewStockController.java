package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;

public class ViewStockController {

    @FXML private TextArea txtStockList;

    @FXML
    private void initialize() {
        // Populate sample stock data
        StringBuilder sb = new StringBuilder();
        sb.append("Product A - Quantity: 50 - Status: Available\n");
        sb.append("Product B - Quantity: 20 - Status: Low Stock\n");
        sb.append("Product C - Quantity: 0 - Status: Out of Stock\n");
        sb.append("Product D - Quantity: 75 - Status: Available\n");
        txtStockList.setText(sb.toString());
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
