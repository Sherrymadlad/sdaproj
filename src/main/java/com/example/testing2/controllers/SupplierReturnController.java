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

public class SupplierReturnController {


    @FXML private TableView<SupplierRefundOrder> tableSupplierRefunds;
    @FXML private TableColumn<SupplierRefundOrder, Integer> colOrderID;
    @FXML private TableColumn<SupplierRefundOrder, Integer> colSupplierID;
    @FXML private TableColumn<SupplierRefundOrder, String> colStatus;
    @FXML private TableColumn<SupplierRefundOrder, Double> colTotalPrice;
    @FXML private Button btnAccept;
    @FXML private Button btnReject;


    @FXML
    private void initialize() {
        colOrderID.setCellValueFactory(new PropertyValueFactory<>("orderid"));
        colSupplierID.setCellValueFactory(new PropertyValueFactory<>("supplierid"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colTotalPrice.setCellValueFactory(new PropertyValueFactory<>("totalprice"));

        loadSupplierRefunds();
    }


    private void loadSupplierRefunds() {
        ObservableList<SupplierRefundOrder> list = FXCollections.observableArrayList();
        try {
            ResultSet rs = DBHelper.executeFunction("StaffViewSupplierRefunds");
            while (rs.next()) {
                int orderid = rs.getInt("orderid");
                int supplierid = rs.getInt("supplierid");
                String status = rs.getString("status");
                double total = rs.getDouble("totalprice");

                list.add(new SupplierRefundOrder(orderid, supplierid, status, total));
            }
            tableSupplierRefunds.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load supplier refunds.");
        }
    }


    @FXML
    private void acceptRefund() {
        SupplierRefundOrder selected = tableSupplierRefunds.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an order to accept.");
            return;
        }

        try {
            DBHelper.executeFunction("StaffAcceptSupplierRefund", selected.getOrderid());
            showAlert("Success", "Refund accepted for order ID: " + selected.getOrderid());
            loadSupplierRefunds(); // Refresh table
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to accept refund.");
        }
    }


    @FXML
    private void rejectRefund() {
        SupplierRefundOrder selected = tableSupplierRefunds.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select an order to reject.");
            return;
        }

        try {
            DBHelper.executeFunction("StaffRejectSupplierRefund", selected.getOrderid());
            showAlert("Success", "Refund rejected for order ID: " + selected.getOrderid());
            loadSupplierRefunds(); // Refresh table
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to reject refund.");
        }
    }


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


    public static class SupplierRefundOrder {
        private final int orderid;
        private final int supplierid;
        private final String status;
        private final double totalprice;

        public SupplierRefundOrder(int orderid, int supplierid, String status, double totalprice) {
            this.orderid = orderid;
            this.supplierid = supplierid;
            this.status = status;
            this.totalprice = totalprice;
        }

        public int getOrderid() { return orderid; }
        public int getSupplierid() { return supplierid; }
        public String getStatus() { return status; }
        public double getTotalprice() { return totalprice; }
    }
}
