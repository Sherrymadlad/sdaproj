package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerOrdersPageController {

    @FXML private VBox ordersContainer;
    @FXML private ScrollPane scrollOrders;
    @FXML private javafx.scene.control.Button btnCurrentOrders;
    @FXML private javafx.scene.control.Button btnPastOrders;

    private final int currentUserId = 1; // hardcoded for testing

    @FXML
    public void initialize() {
        highlightCurrentOrders();
        showCurrentOrders(); // load current orders by default

        btnCurrentOrders.setOnAction(e -> showCurrentOrders());
        btnPastOrders.setOnAction(e -> showPastOrders());
        showCurrentOrders();
    }

    // -------------------------------
    // TAB HIGHLIGHT
    // -------------------------------
    private void highlightCurrentOrders() {
        btnCurrentOrders.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 20;");
        btnPastOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
    }

    private void highlightPastOrders() {
        btnPastOrders.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 20;");
        btnCurrentOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
    }

    // -------------------------------
    // SHOW ORDERS
    // -------------------------------
    public void showCurrentOrders() {
        highlightCurrentOrders();
        ordersContainer.getChildren().clear();

        try {
            // Get all current orders for user 1
            ResultSet rsOrders = DBHelper.executeFunction("ViewCurrentOrders", currentUserId);
            boolean hasOrders = false;

            while (rsOrders.next()) {
                hasOrders = true;
                int orderIdInt = rsOrders.getInt("orderid");
                String orderId = "Order #" + orderIdInt;
                String status = rsOrders.getString("status");

                // Get all items for this order
                StringBuilder itemsList = new StringBuilder();
                ResultSet rsItems = DBHelper.executeFunction("ViewOrderItems", orderIdInt);
                while (rsItems.next()) {
                    String productName = rsItems.getString("productname"); // make sure your SP returns productname
                    int qty = rsItems.getInt("quantity");
                    itemsList.append(productName).append(" x").append(qty).append(", ");
                }
                rsItems.close();

                // Remove trailing comma
                if (itemsList.length() > 0) {
                    itemsList.setLength(itemsList.length() - 2);
                } else {
                    itemsList.append("No items");
                }

                addOrderPanel(orderId, itemsList.toString(), status);
            }

            rsOrders.close();

            if (!hasOrders) {
                addOrderPanel("No Orders", "You have no current orders", "");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            addOrderPanel("Error", "Could not load orders", "");
        }
    }

    public void showPastOrders() {
        highlightPastOrders();
        ordersContainer.getChildren().clear();

        try {
            ResultSet rsOrders = DBHelper.executeFunction("ViewPastOrderHistory", currentUserId);
            boolean hasOrders = false;

            while (rsOrders.next()) {
                hasOrders = true;
                int orderIdInt = rsOrders.getInt("orderid");
                String orderId = "Order #" + orderIdInt;
                String status = rsOrders.getString("status");

                // Get items for this order
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

                addOrderPanel(orderId, itemsList.toString(), status);
            }

            rsOrders.close();

            if (!hasOrders) {
                addOrderPanel("No Orders", "You have no past orders", "");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            addOrderPanel("Error", "Could not load past orders", "");
        }
    }

    // -------------------------------
    // CREATE ORDER PANEL
    // -------------------------------
    private void addOrderPanel(String orderId, String items, String status) {
        AnchorPane panel = new AnchorPane();
        panel.setPrefHeight(140);
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
        ordersContainer.getChildren().add(panel);
    }
}
