package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerReturnController {

    // TableView and Columns
    @FXML private TableView<CustomerRefundOrder> tableCustomerRefunds;
    @FXML private TableColumn<CustomerRefundOrder, Integer> colOrderID;
    @FXML private TableColumn<CustomerRefundOrder, Integer> colCustomerID;
    @FXML private TableColumn<CustomerRefundOrder, String> colStatus;
    @FXML private TableColumn<CustomerRefundOrder, Double> colTotalPrice;

    // Buttons
    @FXML private Button btnAccept;
    @FXML private Button btnReject;

    // Initialize TableView
    @FXML
    private void initialize() {
        colOrderID.setCellValueFactory(new PropertyValueFactory<>("orderid"));
        colCustomerID.setCellValueFactory(new PropertyValueFactory<>("customerid"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTotalPrice.setCellValueFactory(new PropertyValueFactory<>("totalprice"));

        loadCustomerRefunds();
    }

    // Load pending customer refunds from DB
    private void loadCustomerRefunds() {
        ObservableList<CustomerRefundOrder> list = FXCollections.observableArrayList();
        try {
            ResultSet rs = DBHelper.executeFunction("StaffViewCustomerRefunds");
            while (rs.next()) {
                int orderid = rs.getInt("orderid");
                int customerid = rs.getInt("customerid");
                String status = rs.getString("status");
                double total = rs.getDouble("totalprice");

                list.add(new CustomerRefundOrder(orderid, customerid, status, total));
            }
            tableCustomerRefunds.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load customer refunds.");
        }
    }

    // Accept selected refund
    @FXML
    private void acceptRefund() {
        CustomerRefundOrder selected = tableCustomerRefunds.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an order to accept.");
            return;
        }

        try {
            DBHelper.executeProcedure("StaffAcceptCustomerRefund", selected.getOrderid());
            showAlert("Success", "Refund accepted for order ID: " + selected.getOrderid());
            loadCustomerRefunds(); // Refresh table
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to accept refund.");
        }

    }

    // Reject selected refund
    @FXML
    private void rejectRefund() {
        CustomerRefundOrder selected = tableCustomerRefunds.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an order to reject.");
            return;
        }

        try {
            DBHelper.executeFunction("StaffRejectCustomerRefund", selected.getOrderid());
            showAlert("Success", "Refund rejected for order ID: " + selected.getOrderid());
            loadCustomerRefunds(); // Refresh table
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to reject refund.");
        }
    }

    // Go back to HandleReturn page
    @FXML
    private void goBack(ActionEvent event) {
        loadFXML(event, "HandleReturn.fxml");
    }

    // Generic method to load FXML pages
    private void loadFXML(ActionEvent event, String file) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/testing2/" + file));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load " + file);
        }
    }

    // Show alert
    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // Inner class for table data
    public static class CustomerRefundOrder {
        private final int orderid;
        private final int customerid;
        private final String status;
        private final double totalprice;

        public CustomerRefundOrder(int orderid, int customerid, String status, double totalprice) {
            this.orderid = orderid;
            this.customerid = customerid;
            this.status = status;
            this.totalprice = totalprice;
        }

        public int getOrderid() { return orderid; }
        public int getCustomerid() { return customerid; }
        public String getStatus() { return status; }
        public double getTotalprice() { return totalprice; }
    }
}
