package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerOrdersPageController {

    @FXML private VBox ordersContainer;
    @FXML private ScrollPane scrollOrders;
    @FXML private Button btnCurrentOrders;
    @FXML private Button btnPastOrders;
    @FXML private Button btnRefundedOrders;
    @FXML private CustomModalController customModalController;
    @FXML private AnchorPane Root;

    private int currentUserId; // hardcoded for testing

    public void setCurrentUserId(int userId) {
        // Fetch customerid first
        int customerId = DBHelper.getCustomerIdByUserId(userId);
        this.currentUserId = customerId; // now this is the correct customerid
        System.out.println("Orders Page CustomerID: " + customerId);

        // Load orders immediately after setting customerid
        highlightCurrentOrders();
        showCurrentOrders();
    }

    @FXML
    public void initialize() {
        setupModal();

        // Set button actions
        btnCurrentOrders.setOnAction(e -> showCurrentOrders());
        btnPastOrders.setOnAction(e -> showPastOrders());
        btnRefundedOrders.setOnAction(e -> showRefundedOrders());
    }


    // -------------------------------
    // TAB HIGHLIGHT
    // -------------------------------
    private void highlightCurrentOrders() {
        btnCurrentOrders.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 20;");
        btnPastOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
        btnRefundedOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
    }

    private void highlightPastOrders() {
        btnPastOrders.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 20;");
        btnCurrentOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
        btnRefundedOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
    }

    private void highlightRefundedOrders() {
        btnRefundedOrders.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 20;");
        btnCurrentOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
        btnPastOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
    }

    // -------------------------------
    // SHOW ORDERS
    // -------------------------------
    public void showCurrentOrders() {
        highlightCurrentOrders();
        loadOrders("ViewCurrentOrders", false); // no refund button
    }

    public void showPastOrders() {
        highlightPastOrders();
        loadOrders("ViewPastOrderHistory", true); // refund button only for Delivered
    }

    public void showRefundedOrders() {
        highlightRefundedOrders();
        loadOrders("ViewRefundedOrders", false); // no refund button
    }

    // -------------------------------
    // LOAD ORDERS HELPER
    // -------------------------------
    private void loadOrders(String functionName, boolean showRefundButton) {
        if (currentUserId == 0) return; // safety check
        ordersContainer.getChildren().clear();

        try {
            ResultSet rsOrders = DBHelper.executeFunction(functionName, currentUserId);
            boolean hasOrders = false;

            while (rsOrders.next()) {
                hasOrders = true;
                int orderIdInt = rsOrders.getInt("orderid");
                String orderId = "Order #" + orderIdInt;
                String status = rsOrders.getString("status");
                double totalPrice = rsOrders.getDouble("totalprice");

                // Get items
                StringBuilder itemsList = new StringBuilder();
                ResultSet rsItems = DBHelper.executeFunction("ViewOrderItems", orderIdInt);
                while (rsItems.next()) {
                    String productName = rsItems.getString("productname");
                    int qty = rsItems.getInt("quantity");
                    itemsList.append(productName).append(" x").append(qty).append(", ");
                }
                rsItems.close();

                if (itemsList.length() > 0) {
                    itemsList.setLength(itemsList.length() - 2);
                } else {
                    itemsList.append("No items");
                }

                addOrderPanel(orderId, itemsList.toString(), status, totalPrice,  showRefundButton, orderIdInt);
            }

            rsOrders.close();

            if (!hasOrders) {
                addOrderPanel("No Orders", "You have no orders", "", 0, false, 0);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            addOrderPanel("Error", "Could not load orders", "", 0,false, 0);
        }
    }

    // -------------------------------
// CREATE ORDER PANEL
// -------------------------------
    private void addOrderPanel(String orderId, String items, String status, double totalPrice, boolean showRefundButton, int orderIdInt) {
        AnchorPane panel = new AnchorPane();
        panel.setPrefHeight(160);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #cbbcd9; -fx-border-radius: 12;");
        panel.setPadding(new Insets(20));

        Label lblOrderId = new Label(orderId);
        lblOrderId.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #6d4c7d;");
        lblOrderId.setLayoutX(20);
        lblOrderId.setLayoutY(20);

        Label lblItems = new Label(items);
        lblItems.setStyle("-fx-font-size: 18px;");
        lblItems.setLayoutX(20);
        lblItems.setLayoutY(60);

        Label lblStatus = new Label("Status: " + status);
        lblStatus.setStyle("-fx-font-size: 18px; -fx-text-fill: #7a5d8a;");
        lblStatus.setLayoutX(20);
        lblStatus.setLayoutY(95);

        panel.getChildren().addAll(lblOrderId, lblItems, lblStatus);

        // --------------------------
        // REFUND BUTTON for Past Orders
        // --------------------------
        if (showRefundButton && status.equals("Delivered")) {
            Button btnRefund = new Button("Request Refund");
            btnRefund.setLayoutX(20);
            btnRefund.setLayoutY(125);
            btnRefund.setStyle("-fx-background-color: #d46a6a; -fx-text-fill: white; -fx-font-size: 16px;");

            btnRefund.setOnAction(e -> {
                showCustomModalConfirmation("Are you sure you want to request a refund for this order?", confirmed -> {
                    if (confirmed) {
                        try {
                            DBHelper.executeFunction("UpdateCustomerOrderStatus", orderIdInt, "Refund Pending");
                            showCustomModal("Hold tight while we prepare your refund :)");
                            // Refresh past orders
                            showPastOrders();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            javafx.scene.control.Alert error = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            error.setTitle("Error");
                            error.setHeaderText(null);
                            error.setContentText("Could not request refund. Please try again.");
                            error.showAndWait();
                        }
                    }
                });
            });

            panel.getChildren().add(btnRefund);
        }

        // -------------------------------
        // TOTAL PRICE
        Label lblTotalPrice = new Label("Total: $" + totalPrice);
        lblTotalPrice.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #6d4c7d;");
        AnchorPane.setTopAnchor(lblTotalPrice, 95.0);   // aligns vertically with status
        AnchorPane.setRightAnchor(lblTotalPrice, 20.0); // 20px from right edge
        panel.getChildren().add(lblTotalPrice);

        // --------------------------
// CANCEL BUTTON for Current Orders
// --------------------------
        if (!showRefundButton && status.equals("Pending")) { // only current orders
            Button btnCancel = new Button("Cancel Order");
            btnCancel.setLayoutX(20);
            btnCancel.setLayoutY(125);
            btnCancel.setStyle("-fx-background-color: #d46a6a; -fx-text-fill: white; -fx-font-size: 16px;");

            btnCancel.setOnAction(e -> {
                showCustomModalConfirmation("Are you sure you want to cancel this order?", confirmed -> {
                    if (confirmed) {
                        try {
                            DBHelper.executeFunction("UpdateCustomerOrderStatus", orderIdInt, "Cancelled");
                            javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
                            showCustomModal("Your order has been successfully cancelled!");
                            // Refresh current orders
                            showCurrentOrders();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            javafx.scene.control.Alert error = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            error.setTitle("Error");
                            error.setHeaderText(null);
                            error.setContentText("Could not cancel order. Please try again.");
                            error.showAndWait();
                        }
                    }
                });
            });

            panel.getChildren().add(btnCancel);
        }

        ordersContainer.getChildren().add(panel);
    }


    private void showCustomModalConfirmation(String message, java.util.function.Consumer<Boolean> callback) {
        if (customModalController != null) {
            customModalController.showConfirmation(message, callback);
        } else {
            // Fallback: auto-confirm if modal not initialized
            callback.accept(true);
        }
    }

    private void showCustomModal(String message) {
        if (customModalController != null) {
            customModalController.showMessage(message);
        } else {
            // fallback
            System.err.println("Modal not initialized: " + message);
        }
    }

    private void setupModal(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomModal.fxml"));
            StackPane modal = loader.load();
            customModalController = loader.getController();

            Root.getChildren().add(modal);
            AnchorPane.setTopAnchor(modal, 0.0);
            AnchorPane.setBottomAnchor(modal, 0.0);
            AnchorPane.setLeftAnchor(modal, 0.0);
            AnchorPane.setRightAnchor(modal, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
