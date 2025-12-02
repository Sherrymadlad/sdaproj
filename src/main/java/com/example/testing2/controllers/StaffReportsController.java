package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.ResultSet;

public class StaffReportsController {

    // -------------------------------
    // Products Stock Tab
    // -------------------------------
    @FXML private TableView<ProductStock> tblProductsStock;
    @FXML private TableColumn<ProductStock, Integer> colProductId;
    @FXML private TableColumn<ProductStock, String> colSKU;
    @FXML private TableColumn<ProductStock, String> colProductName;
    @FXML private TableColumn<ProductStock, Double> colPrice;
    @FXML private TableColumn<ProductStock, Integer> colTotalStock;

    // -------------------------------
    // Warehouse Stock Tab
    // -------------------------------
    @FXML private TableView<WarehouseStock> tblWarehouseStock;
    @FXML private TableColumn<WarehouseStock, String> colProductNameWH;
    @FXML private TableColumn<WarehouseStock, String> colWarehouseName;
    @FXML private TableColumn<WarehouseStock, Integer> colQuantityAvailable;

    public void initialize() {
        setupProductsStockTable();
        loadProductsStock();

        setupWarehouseStockTable();
        loadWarehouseStock();
    }

    // -------------------------------
    // Products Stock
    // -------------------------------
    private void setupProductsStockTable() {
        colProductId.setCellValueFactory(new PropertyValueFactory<>("productId"));
        colSKU.setCellValueFactory(new PropertyValueFactory<>("sku"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colTotalStock.setCellValueFactory(new PropertyValueFactory<>("totalStock"));
    }

    private void loadProductsStock() {
        ObservableList<ProductStock> list = FXCollections.observableArrayList();
        try (ResultSet rs = DBHelper.executeFunction("GetAllProductsStock")) {
            while (rs.next()) {
                list.add(new ProductStock(
                        rs.getInt("product_id"),
                        rs.getString("sku"),
                        rs.getString("product_name"),
                        rs.getDouble("price"),
                        rs.getInt("total_available_stock")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        tblProductsStock.setItems(list);
    }

    // -------------------------------
    // Warehouse Stock
    // -------------------------------
    private void setupWarehouseStockTable() {
        colProductNameWH.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colWarehouseName.setCellValueFactory(new PropertyValueFactory<>("warehouseName"));
        colQuantityAvailable.setCellValueFactory(new PropertyValueFactory<>("quantityAvailable"));
    }

    private void loadWarehouseStock() {
        ObservableList<WarehouseStock> list = FXCollections.observableArrayList();
        try (ResultSet rs = DBHelper.executeFunction("GetStockReport")) {
            while (rs.next()) {
                list.add(new WarehouseStock(
                        rs.getString("productname"),
                        rs.getString("warehousename"),
                        rs.getInt("quantityavailable")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        tblWarehouseStock.setItems(list);
    }

    // -------------------------------
    // Data Models
    // -------------------------------
    public static class ProductStock {
        private final int productId;
        private final String sku;
        private final String productName;
        private final double price;
        private final int totalStock;

        public ProductStock(int productId, String sku, String productName, double price, int totalStock) {
            this.productId = productId;
            this.sku = sku;
            this.productName = productName;
            this.price = price;
            this.totalStock = totalStock;
        }

        public int getProductId() { return productId; }
        public String getSku() { return sku; }
        public String getProductName() { return productName; }
        public double getPrice() { return price; }
        public int getTotalStock() { return totalStock; }
    }

    public static class WarehouseStock {
        private final String productName;
        private final String warehouseName;
        private final int quantityAvailable;

        public WarehouseStock(String productName, String warehouseName, int quantityAvailable) {
            this.productName = productName;
            this.warehouseName = warehouseName;
            this.quantityAvailable = quantityAvailable;
        }

        public String getProductName() { return productName; }
        public String getWarehouseName() { return warehouseName; }
        public int getQuantityAvailable() { return quantityAvailable; }
    }
}
