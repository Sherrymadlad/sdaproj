package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ReportsPageController {

    @FXML private VBox reportsContainer;

    @FXML private Button btnStockReport;
    @FXML private Button btnSupplierReport;
    @FXML private Button btnSalesStockReport;
    @FXML private Button btnMovementReport;

    @FXML
    public void initialize() {
        btnStockReport.setOnAction(e -> loadStockReport());
        btnSupplierReport.setOnAction(e -> loadSupplierReport());
        btnSalesStockReport.setOnAction(e -> loadSalesStockReport());
        btnMovementReport.setOnAction(e -> loadMovementReport());

        // Load default report
        loadStockReport();
        highlightButton(btnStockReport);
        addHoverEffect(btnStockReport);
        addHoverEffect(btnSupplierReport);
        addHoverEffect(btnSalesStockReport);
    }

    private void addHoverEffect(Button btn) {
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.05);
            btn.setScaleY(1.05);
        });

        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
    }

    private void highlightButton(Button active) {
        Button[] buttons = {btnStockReport, btnSupplierReport, btnSalesStockReport, btnMovementReport};
        for (Button b : buttons) {
            if (b == active) {
                b.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white; -fx-font-size: 18;");
            } else {
                b.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 18;");
            }
        }
    }

    private void loadStockReport() {
        highlightButton(btnStockReport);
        reportsContainer.getChildren().clear();
        populateReport("SELECT * FROM GetStockReport()", rs -> {
            String product = rs.getString("productname");
            String warehouse = rs.getString("warehousename");
            int qty = rs.getInt("quantityavailable");
            return "Product: " + product + "\nWarehouse: " + warehouse + "\nQuantity: " + qty;
        });
    }

    private void loadSupplierReport() {
        highlightButton(btnSupplierReport);
        reportsContainer.getChildren().clear();

        // Use GetSupplierPerformance() function
        populateReport("SELECT * FROM GetSupplierReport()", rs -> {
            int supplierId = rs.getInt("supplierid");
            String supplierName = rs.getString("suppliername");
            int totalOrders = rs.getInt("totalorders");
            double totalValue = rs.getDouble("totalvalue");

            return "Supplier: " + supplierName + " (ID: " + supplierId + ")\n" +
                    "Total Orders: " + totalOrders + "\n" +
                    "Total Value: $" + String.format("%.2f", totalValue);
        });
    }

    private void loadSalesStockReport() {
        highlightButton(btnSalesStockReport);
        reportsContainer.getChildren().clear();

        // Call the PostgreSQL function GetSalesVsStock()
        populateReport("SELECT * FROM GetSalesVsStock()", rs -> {
            int productId = rs.getInt("productid");
            String productName = rs.getString("productname");
            int unitsSold = rs.getInt("unitssold");
            int stockAvailable = rs.getInt("stockavailable");

            return "Product: " + productName + " (ID: " + productId + ")\n" +
                    "Units Sold: " + unitsSold + "\n" +
                    "Stock Available: " + stockAvailable;
        });
    }

    private void loadMovementReport() {
        highlightButton(btnMovementReport);
        reportsContainer.getChildren().clear();

        populateReport("SELECT * FROM GetFastSlowMovingItems()", rs -> {
            int productId = rs.getInt("productid");
            String productName = rs.getString("productname");
            long unitsSold = rs.getLong("units_sold");        // BIGINT -> getLong
            long stockAvailable = rs.getLong("stock_available");
            String movementClass = rs.getString("movement_class");

            return "Product: " + productName + " (ID: " + productId + ")\n" +
                    "Units Sold: " + unitsSold + "\n" +
                    "Stock Available: " + stockAvailable + "\n" +
                    "Movement: " + movementClass;
        });
    }

    private void populateReport(String sql, ReportFormatter formatter) {
        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                AnchorPane panel = new AnchorPane();
                panel.setPrefHeight(120);
                panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #d0d0d0;");
                panel.setPadding(new Insets(15));

                Label lbl = new Label(formatter.format(rs));
                lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");
                lbl.setWrapText(true);
                lbl.setLayoutX(15);
                lbl.setLayoutY(15);

                panel.getChildren().add(lbl);
                reportsContainer.getChildren().add(panel);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    interface ReportFormatter {
        String format(ResultSet rs) throws Exception;
    }
}
