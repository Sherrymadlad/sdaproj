package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.sql.SQLException;

public class RecordOrderController {

    @FXML private TextField txtProductID;
    @FXML private TextField txtCustomerID;
    @FXML private TextField txtQuantity;
    @FXML private TextField txtUnitPrice;
    @FXML private TextField txtTotalPrice;

    @FXML
    private void initialize() {

        ChangeListener<String> listener = (ObservableValue<? extends String> obs, String oldVal, String newVal) -> calculateTotal();
        txtQuantity.textProperty().addListener(listener);
        txtUnitPrice.textProperty().addListener(listener);
    }

    private void calculateTotal() {
        try {
            double quantity = txtQuantity.getText().isEmpty() ? 0 : Double.parseDouble(txtQuantity.getText());
            double unitPrice = txtUnitPrice.getText().isEmpty() ? 0 : Double.parseDouble(txtUnitPrice.getText());
            double total = quantity * unitPrice;
            txtTotalPrice.setText(String.format("%.2f", total));
        } catch (NumberFormatException e) {
            txtTotalPrice.setText("0.00");
        }
    }

    @FXML
    private void submitOrder() {
        String productId = txtProductID.getText().trim();
        String customerId = txtCustomerID.getText().trim();
        String quantityStr = txtQuantity.getText().trim();
        String unitPriceStr = txtUnitPrice.getText().trim();

        if (productId.isEmpty() || customerId.isEmpty() || quantityStr.isEmpty() || unitPriceStr.isEmpty()) {
            showAlert("Error", "All fields must be filled.");
            return;
        }

        int quantity;
        double unitPrice;

        try {
            quantity = Integer.parseInt(quantityStr);
            unitPrice = Double.parseDouble(unitPriceStr);
        } catch (NumberFormatException e) {
            showAlert("Error", "Quantity and Unit Price must be numeric.");
            return;
        }

        double totalPrice = quantity * unitPrice;

        try {
            // Call your stored procedure or function to insert order
            DBHelper.executeProcedure("RecordNewOrder", productId, customerId, quantity, unitPrice, totalPrice);
            showAlert("Success", "Order recorded successfully!");
            clearFields();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to record order. Check database connection.");
        }
    }

    @FXML
    private void goBack(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/testing2/StaffDashboard.fxml"));
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to go back.");
        }
    }

    private void clearFields() {
        txtProductID.clear();
        txtCustomerID.clear();
        txtQuantity.clear();
        txtUnitPrice.clear();
        txtTotalPrice.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
