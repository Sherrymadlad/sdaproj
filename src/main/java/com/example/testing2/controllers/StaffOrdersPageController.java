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

public class StaffOrdersPageController {

    @FXML private VBox ordersContainer;
    @FXML private ScrollPane scrollOrders;
    @FXML private Button btnOngoingOrders;
    @FXML private Button btnPastOrders;
    @FXML private Button btnRefundPendingOrders;
    @FXML private CustomModalController customModalController;
    @FXML private AnchorPane Root;


    public void initialize() {
        setupModal();
        highlightOngoingOrders();
        showOngoingOrders();

        btnOngoingOrders.setOnAction(e -> showOngoingOrders());
        btnPastOrders.setOnAction(e -> showPastOrders());
        btnRefundPendingOrders.setOnAction(e -> showRefundPendingOrders());
    }


    private void highlightOngoingOrders() {
        btnOngoingOrders.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 20;");
        btnPastOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
        btnRefundPendingOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
    }

    private void highlightPastOrders() {
        btnPastOrders.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 20;");
        btnOngoingOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
        btnRefundPendingOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
    }

    private void highlightRefundPendingOrders() {
        btnRefundPendingOrders.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 20;");
        btnOngoingOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
        btnPastOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 20;");
    }


    private void showOngoingOrders() {
        highlightOngoingOrders();
        loadOrders("StaffViewOngoingSalesOrders",true);
    }

    private void showPastOrders() {
        highlightPastOrders();
        loadOrders("StaffViewPastSalesOrders",false);
    }

    private void showRefundPendingOrders() {
        highlightRefundPendingOrders();
        loadOrders("StaffViewRefundPendingOrders",false);
    }


    private void loadOrders(String functionName, boolean showUpdateButton) {
        ordersContainer.getChildren().clear();

        try (ResultSet rsOrders = DBHelper.executeFunction(functionName)) {
            boolean hasOrders = false;

            while (rsOrders.next()) {
                hasOrders = true;

                int orderId = rsOrders.getInt("order_id");
                String status = rsOrders.getString("status");
                String orderDate = rsOrders.getTimestamp("order_date").toString();
                String customerName = rsOrders.getString("customer_name");
                int customerId = rsOrders.getInt("customer_id");
                double totalPrice = rsOrders.getDouble("total_price");

                addOrderPanel(orderId, status, orderDate, customerName, customerId, totalPrice, showUpdateButton);
            }

            if (!hasOrders) {
                addOrderPanel(0, "", "", "No Orders", 0, 0, false);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            addOrderPanel(0, "", "", "Error loading orders", 0, 0, false);
        }
    }

    private void addOrderPanel(int orderId, String status, String orderDate, String customerName, int customerId, double totalPrice, boolean showUpdateButton) {
        AnchorPane panel = new AnchorPane();
        panel.setPrefHeight(160);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #cbbcd9; -fx-border-radius: 12;");
        panel.setPadding(new Insets(20));

        Label lblOrderId = new Label(orderId == 0 ? customerName : "Order #" + orderId);
        lblOrderId.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #6d4c7d;");
        lblOrderId.setLayoutX(20);
        lblOrderId.setLayoutY(20);

        Label lblCustomer = new Label(orderId == 0 ? "" : customerName + " (ID: " + customerId + ")");
        lblCustomer.setStyle("-fx-font-size: 18px;");
        lblCustomer.setLayoutX(20);
        lblCustomer.setLayoutY(60);

        Label lblStatus = new Label(orderId == 0 ? "" : "Status: " + status);
        lblStatus.setStyle("-fx-font-size: 18px; -fx-text-fill: #7a5d8a;");
        lblStatus.setLayoutX(20);
        lblStatus.setLayoutY(95);

        Label lblDate = new Label(orderId == 0 ? "" : "Date: " + orderDate);
        lblDate.setStyle("-fx-font-size: 18px; -fx-text-fill: #7a5d8a;");
        lblDate.setLayoutX(20);
        lblDate.setLayoutY(125);

        Label lblTotal = new Label(orderId == 0 ? "" : String.format("Total: $%.2f", totalPrice));
        lblTotal.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #6d4c7d;");
        AnchorPane.setTopAnchor(lblTotal, 20.0);
        AnchorPane.setRightAnchor(lblTotal, 20.0);

        panel.getChildren().addAll(lblOrderId, lblCustomer, lblStatus, lblDate, lblTotal);

        if (orderId != 0) {
            if (showUpdateButton) {

                String nextStatus = switch (status) {
                    case "Pending" -> "Processing";
                    case "Processing" -> "Dispatched";
                    case "Dispatched" -> "Delivered";
                    default -> ""; // Delivered, Cancelled, Returned
                };

                if (!nextStatus.isEmpty()) {
                    Button btnUpdate = new Button("Update Status → " + nextStatus);
                    btnUpdate.setStyle("-fx-background-color: #4a90e2; -fx-text-fill: white; -fx-font-size: 16px;");
                    AnchorPane.setTopAnchor(btnUpdate, 60.0);
                    AnchorPane.setRightAnchor(btnUpdate, 20.0);

                    btnUpdate.setOnAction(e -> {
                        showCustomModalConfirmation("Are you sure you want to update the status to " + nextStatus + "?", confirmed -> {
                            if (confirmed) {
                                try {
                                    DBHelper.executeFunction("UpdateCustomerOrderStatus", orderId, nextStatus);
                                    showCustomModal("Order status updated to " + nextStatus + "!");
                                    showOngoingOrders();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                    });



                    panel.getChildren().add(btnUpdate);
                }
            } else if (status.equals("Refund Pending")) {
                Button btnApproveRefund = new Button("Approve Refund → Returned");
                btnApproveRefund.setStyle("-fx-background-color: #e24a4a; -fx-text-fill: white; -fx-font-size: 16px;");
                AnchorPane.setTopAnchor(btnApproveRefund, 60.0);
                AnchorPane.setRightAnchor(btnApproveRefund, 20.0);

                btnApproveRefund.setOnAction(e -> {
                    showCustomModalConfirmation("Are you sure you want to approve this refund?", confirmed -> {
                        if (confirmed) {
                            try {
                                DBHelper.executeFunction("UpdateCustomerOrderStatus", orderId, "Returned");
                                showCustomModal("Refund has been approved and order marked as Returned.");
                                showRefundPendingOrders(); // refresh refund tab
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                });


                panel.getChildren().add(btnApproveRefund);
            }
        }



        ordersContainer.getChildren().add(panel);
    }



    private void showCustomModalConfirmation(String message, java.util.function.Consumer<Boolean> callback) {
        if (customModalController != null) {
            customModalController.showConfirmation(message, callback);
        } else {

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
