package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.beans.property.*;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ManagerMainPageController implements Initializable {

    @FXML private TextField txtSearch;

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, String> colSupplier;
    @FXML private TableColumn<Product, Void> colActions;

    @FXML private Button btnAddProduct, btnDeleteProduct, btnUpdateProduct, btnSaveProduct, btnCancel;
    @FXML private StackPane productModal, notificationBadge;
    @FXML private Label lblNotificationCount;

    @FXML private TextField txtProductName, txtSku, txtPrice, txtStock, txtSupplier;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private ComboBox<Integer> cmbCategoryId;
    @FXML private ComboBox<Integer> cmbSupplierId;
    @FXML private ImageView productImage;

    private final Image placeholderImage = new Image(Objects.requireNonNull(getClass().getResource("/com/example/testing2/images/placeholder.jpg")).toExternalForm());
    private Product selectedProduct;
    private final Map<Integer, Product> allProducts = new HashMap<>();
    private final Map<String, Integer> categoryNameToId = new HashMap<>();
    private final Map<String, Integer> supplierNameToId = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTableColumns();
        setupEventHandlers();
        loadProductsFromDatabase();
        loadCategories();
        loadSuppliers();
        checkLowStockNotifications();
    }

    private void initializeTableColumns() {
        try {
            if (colId != null) {
                colId.setCellValueFactory(cellData -> cellData.getValue().productIdProperty().asObject());
            }

            if (colName != null) {
                colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
            }

            if (colCategory != null) {
                colCategory.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
            }

            if (colPrice != null) {
                colPrice.setCellValueFactory(cellData -> cellData.getValue().priceProperty().asObject());
            }

            if (colStock != null) {
                colStock.setCellValueFactory(cellData -> cellData.getValue().stockLevelProperty().asObject());
            }

            if (colSupplier != null) {
                colSupplier.setCellValueFactory(cellData -> cellData.getValue().supplierProperty());
            }

            if (colActions != null) {
                colActions.setCellFactory(param -> new TableCell<Product, Void>() {
                    private final Button editBtn = new Button("Edit");
                    private final Button deleteBtn = new Button("Delete");

                    {
                        editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 12px;");
                        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 12px;");

                        editBtn.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            editProduct(product);
                        });

                        deleteBtn.setOnAction(event -> {
                            Product product = getTableView().getItems().get(getIndex());
                            deleteProduct(product);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, editBtn, deleteBtn);
                            setGraphic(buttons);
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error initializing table columns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupEventHandlers() {
        // Search functionality
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterProducts(newValue));
        }

        // Table selection
        if (productsTable != null) {
            productsTable.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldValue, newValue) -> {
                        selectedProduct = newValue;
                        if (btnDeleteProduct != null) btnDeleteProduct.setDisable(newValue == null);
                        if (btnUpdateProduct != null) btnUpdateProduct.setDisable(newValue == null);
                    });
        }

        // Button actions
        if (btnAddProduct != null) btnAddProduct.setOnAction(e -> handleAddProduct());
        if (btnDeleteProduct != null) btnDeleteProduct.setOnAction(e -> handleDeleteProduct());
        if (btnUpdateProduct != null) btnUpdateProduct.setOnAction(e -> handleUpdateProduct());
        if (btnSaveProduct != null) btnSaveProduct.setOnAction(e -> handleSaveProduct());
        if (btnCancel != null) btnCancel.setOnAction(e -> handleCancel());
    }

    private void loadProductsFromDatabase() {
        try {
            ResultSet rs = DBHelper.executeFunction("GetAllProducts");

            List<Product> products = new ArrayList<>();
            allProducts.clear();

            while (rs != null && rs.next()) {
                try {
                    int productId = rs.getInt("productid");
                    String sku = rs.getString("sku");
                    String productName = rs.getString("productname");
                    String categoryName = rs.getString("categoryname");
                    int stockLevel = rs.getInt("stocklevel");
                    double price = rs.getDouble("price");
                    String description = rs.getString("description");
                    String supplierName = rs.getString("suppliername");
                    String warehouse = rs.getString("warehouse");

                    Product product = new Product(
                            productId, sku, productName, categoryName, stockLevel,
                            price, description, supplierName, warehouse
                    );
                    products.add(product);
                    allProducts.put(productId, product);

                } catch (SQLException e) {
                    System.err.println("Error reading product row: " + e.getMessage());
                }
            }

            if (productsTable != null) {
                productsTable.getItems().setAll(products);
                System.out.println("Loaded " + products.size() + " products");
            }

        } catch (SQLException e) {
            System.err.println("Failed to load products: " + e.getMessage());
            showAlert("Database Error", "Failed to load products: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCategories() {
        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");
            List<String> categories = new ArrayList<>();
            List<Integer> categoryIds = new ArrayList<>();
            categoryNameToId.clear();

            while (rs != null && rs.next()) {
                int categoryId = rs.getInt("categoryid");
                String categoryName = rs.getString("name");

                if (categoryName != null && !categoryName.trim().isEmpty()) {
                    categories.add(categoryName);
                    categoryIds.add(categoryId);
                    categoryNameToId.put(categoryName, categoryId);
                }
            }

            if (cmbCategory != null) {
                cmbCategory.getItems().setAll(categories);
                if (!categories.isEmpty()) {
                    cmbCategory.setValue(categories.get(0));
                }
            }

            if (cmbCategoryId != null) {
                cmbCategoryId.getItems().setAll(categoryIds);
            }

        } catch (SQLException e) {
            System.err.println("Failed to load categories: " + e.getMessage());
            showAlert("Database Error", "Failed to load categories: " + e.getMessage());
        }
    }

    private void loadSuppliers() {
        try {
            Connection conn = DBHelper.getConnection();
            String query = "SELECT supplierid, name FROM supplier ORDER BY name";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            List<String> supplierNames = new ArrayList<>();
            List<Integer> supplierIds = new ArrayList<>();
            supplierNameToId.clear();

            while (rs.next()) {
                int supplierId = rs.getInt("supplierid");
                String supplierName = rs.getString("name");

                if (supplierName != null && !supplierName.trim().isEmpty()) {
                    supplierNames.add(supplierName);
                    supplierIds.add(supplierId);
                    supplierNameToId.put(supplierName, supplierId);
                }
            }

            if (cmbSupplierId != null) {
                cmbSupplierId.getItems().setAll(supplierIds);
            }

            System.out.println("Loaded " + supplierNames.size() + " suppliers");

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("Failed to load suppliers: " + e.getMessage());
            showAlert("Database Error", "Failed to load suppliers: " + e.getMessage());
        }
    }

    private void filterProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (productsTable != null) {
                productsTable.getItems().setAll(allProducts.values());
            }
            return;
        }

        query = query.toLowerCase();
        List<Product> filteredProducts = new ArrayList<>();

        for (Product product : allProducts.values()) {
            if (product.getName().toLowerCase().contains(query) ||
                    product.getSku().toLowerCase().contains(query) ||
                    product.getCategory().toLowerCase().contains(query) ||
                    product.getSupplier().toLowerCase().contains(query)) {
                filteredProducts.add(product);
            }
        }

        if (productsTable != null) {
            productsTable.getItems().setAll(filteredProducts);
        }
    }

    private void checkLowStockNotifications() {
        try {
            ResultSet rs = DBHelper.executeFunction("GetLowStockProducts");
            int lowStockCount = 0;

            while (rs != null && rs.next()) {
                lowStockCount++;
            }

            if (notificationBadge != null && lblNotificationCount != null) {
                if (lowStockCount > 0) {
                    notificationBadge.setVisible(true);
                    lblNotificationCount.setText(String.valueOf(lowStockCount));
                } else {
                    notificationBadge.setVisible(false);
                }
            }

        } catch (SQLException e) {
            System.err.println("Failed to check low stock: " + e.getMessage());
        }
    }

    private void handleAddProduct() {
        selectedProduct = null;
        clearForm();
        if (productModal != null) {
            productModal.setVisible(true);
        }
    }

    private void handleUpdateProduct() {
        if (selectedProduct != null) {
            editProduct(selectedProduct);
        }
    }

    private void handleDeleteProduct() {
        if (selectedProduct != null) {
            deleteProduct(selectedProduct);
        }
    }

    private void handleSaveProduct() {
        if (validateForm()) {
            try {
                if (selectedProduct == null) {
                    addProductToDatabase();
                } else {
                    updateProductInDatabase();
                }

                if (productModal != null) {
                    productModal.setVisible(false);
                }
                loadProductsFromDatabase();
                checkLowStockNotifications();

            } catch (SQLException e) {
                System.err.println("Failed to save product: " + e.getMessage());
                showAlert("Database Error", "Failed to save product: " + e.getMessage());
            }
        }
    }

    private void handleCancel() {
        if (productModal != null) {
            productModal.setVisible(false);
        }
    }

    private void addProductToDatabase() throws SQLException {
        String sku = txtSku.getText();
        String name = txtProductName.getText();
        Integer categoryId = categoryNameToId.get(cmbCategory.getValue());
        double price = Double.parseDouble(txtPrice.getText());
        String description = txtDescription.getText();
        Integer supplierId = null;

        // Try to get supplier ID from name or use default
        if (txtSupplier.getText() != null && !txtSupplier.getText().isEmpty()) {
            supplierId = supplierNameToId.get(txtSupplier.getText());
            if (supplierId == null) {
                // Create new supplier if not exists
                Connection conn = DBHelper.getConnection();
                String insertSupplier = "INSERT INTO supplier (name) VALUES (?) RETURNING supplierid";
                PreparedStatement stmt = conn.prepareStatement(insertSupplier);
                stmt.setString(1, txtSupplier.getText());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    supplierId = rs.getInt(1);
                    supplierNameToId.put(txtSupplier.getText(), supplierId);
                }
                rs.close();
                stmt.close();
            }
        }

        if (categoryId == null) {
            showAlert("Error", "Please select a valid category");
            return;
        }

        if (supplierId == null) {
            // Use first supplier as default
            if (!supplierNameToId.isEmpty()) {
                supplierId = supplierNameToId.values().iterator().next();
            } else {
                showAlert("Error", "No supplier available. Please add a supplier first.");
                return;
            }
        }

        DBHelper.executeProcedure(
                "AddProduct",
                sku,
                name,
                categoryId,
                price,
                supplierId,
                description
        );

        System.out.println("Product added successfully!");
    }

    private void updateProductInDatabase() throws SQLException {
        if (selectedProduct == null) return;

        String sku = txtSku.getText();
        String name = txtProductName.getText();
        Integer categoryId = categoryNameToId.get(cmbCategory.getValue());
        double price = Double.parseDouble(txtPrice.getText());
        String description = txtDescription.getText();
        Integer supplierId = null;

        // Try to get supplier ID from name
        if (txtSupplier.getText() != null && !txtSupplier.getText().isEmpty()) {
            supplierId = supplierNameToId.get(txtSupplier.getText());
        }

        if (categoryId == null) {
            showAlert("Error", "Please select a valid category");
            return;
        }

        DBHelper.executeProcedure(
                "UpdateProduct",
                selectedProduct.getProductId(),
                sku,
                name,
                categoryId,
                price,
                description,
                supplierId,
                true // isactive
        );

        System.out.println("Product updated successfully!");
    }

    private void editProduct(Product product) {
        selectedProduct = product;
        if (txtSku != null) txtSku.setText(product.getSku());
        if (txtProductName != null) txtProductName.setText(product.getName());
        if (cmbCategory != null) cmbCategory.setValue(product.getCategory());
        if (txtPrice != null) txtPrice.setText(String.valueOf(product.getPrice()));
        if (txtStock != null) txtStock.setText(String.valueOf(product.getStockLevel()));
        if (txtDescription != null) txtDescription.setText(product.getDescription());
        if (txtSupplier != null) txtSupplier.setText(product.getSupplier());
        if (productModal != null) productModal.setVisible(true);
    }

    private void deleteProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Product");
        alert.setHeaderText("Delete " + product.getName());
        alert.setContentText("Are you sure you want to permanently delete this product?\n\nWarning: This action cannot be undone and may affect related orders and stock records.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // First, check if product can be deleted (no dependent records)
                if (hasDependentRecords(product.getProductId())) {
                    showAlert("Cannot Delete",
                            "Cannot delete product because it has dependent records (orders, stock, etc.).\n" +
                                    "Please delete related records first or use deactivate instead.");
                    return;
                }

                // Delete product using direct SQL (cascade delete)
                deleteProductFromDatabase(product.getProductId());

                // Remove from local collections
                allProducts.remove(product.getProductId());
                productsTable.getItems().remove(product);

                checkLowStockNotifications();

                // Show success message
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Success");
                successAlert.setHeaderText("Product Deleted");
                successAlert.setContentText(product.getName() + " has been permanently deleted from the system.");
                successAlert.showAndWait();

                System.out.println("Product deleted successfully!");

            } catch (SQLException e) {
                System.err.println("Failed to delete product: " + e.getMessage());
                e.printStackTrace();
                showAlert("Database Error", "Failed to delete product: " + e.getMessage());
            }
        }
    }

    private boolean hasDependentRecords(int productId) throws SQLException {
        Connection conn = DBHelper.getConnection();

        // Check if product exists in orders
        String checkOrdersQuery = "SELECT COUNT(*) FROM orderitem WHERE productid = ?";
        PreparedStatement stmt1 = conn.prepareStatement(checkOrdersQuery);
        stmt1.setInt(1, productId);
        ResultSet rs1 = stmt1.executeQuery();
        rs1.next();
        int orderCount = rs1.getInt(1);
        rs1.close();
        stmt1.close();

        // Check if product exists in stock
        String checkStockQuery = "SELECT COUNT(*) FROM stock WHERE productid = ?";
        PreparedStatement stmt2 = conn.prepareStatement(checkStockQuery);
        stmt2.setInt(1, productId);
        ResultSet rs2 = stmt2.executeQuery();
        rs2.next();
        int stockCount = rs2.getInt(1);
        rs2.close();
        stmt2.close();

        // Check if product exists in batchlot
        String checkBatchQuery = "SELECT COUNT(*) FROM batchlot WHERE productid = ?";
        PreparedStatement stmt3 = conn.prepareStatement(checkBatchQuery);
        stmt3.setInt(1, productId);
        ResultSet rs3 = stmt3.executeQuery();
        rs3.next();
        int batchCount = rs3.getInt(1);
        rs3.close();
        stmt3.close();

        // Check if product exists in productmovement
        String checkMovementQuery = "SELECT COUNT(*) FROM productmovement WHERE productid = ?";
        PreparedStatement stmt4 = conn.prepareStatement(checkMovementQuery);
        stmt4.setInt(1, productId);
        ResultSet rs4 = stmt4.executeQuery();
        rs4.next();
        int movementCount = rs4.getInt(1);
        rs4.close();
        stmt4.close();

        return (orderCount > 0 || stockCount > 0 || batchCount > 0 || movementCount > 0);
    }

    private void deleteProductFromDatabase(int productId) throws SQLException {
        Connection conn = DBHelper.getConnection();

        try {
            conn.setAutoCommit(false); // Start transaction

            // Delete from dependent tables first (in reverse order of foreign key dependencies)

            // 1. Delete from batchlot
            String deleteBatchQuery = "DELETE FROM batchlot WHERE productid = ?";
            PreparedStatement stmt1 = conn.prepareStatement(deleteBatchQuery);
            stmt1.setInt(1, productId);
            stmt1.executeUpdate();
            stmt1.close();

            // 2. Delete from stock
            String deleteStockQuery = "DELETE FROM stock WHERE productid = ?";
            PreparedStatement stmt2 = conn.prepareStatement(deleteStockQuery);
            stmt2.setInt(1, productId);
            stmt2.executeUpdate();
            stmt2.close();

            // 3. Delete from orderitem (if any)
            String deleteOrderItemQuery = "DELETE FROM orderitem WHERE productid = ?";
            PreparedStatement stmt3 = conn.prepareStatement(deleteOrderItemQuery);
            stmt3.setInt(1, productId);
            stmt3.executeUpdate();
            stmt3.close();

            // 4. Delete from productmovement
            String deleteMovementQuery = "DELETE FROM productmovement WHERE productid = ?";
            PreparedStatement stmt4 = conn.prepareStatement(deleteMovementQuery);
            stmt4.setInt(1, productId);
            stmt4.executeUpdate();
            stmt4.close();

            // 5. Finally delete from product table
            String deleteProductQuery = "DELETE FROM product WHERE productid = ?";
            PreparedStatement stmt5 = conn.prepareStatement(deleteProductQuery);
            stmt5.setInt(1, productId);
            int rowsDeleted = stmt5.executeUpdate();
            stmt5.close();

            if (rowsDeleted > 0) {
                conn.commit(); // Commit transaction
                System.out.println("Product and all related records deleted successfully.");
            } else {
                conn.rollback(); // Rollback if no rows deleted
                throw new SQLException("Product not found");
            }

        } catch (SQLException e) {
            conn.rollback(); // Rollback on error
            throw e;
        } finally {
            conn.setAutoCommit(true); // Restore auto-commit
        }
    }

    private void tryAlternativeDeactivate(Product product) {
        try {
            // Alternative: Use a direct SQL update
            Connection conn = DBHelper.getConnection();
            String query = "UPDATE product SET isactive = false WHERE productid = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, product.getProductId());
            stmt.executeUpdate();
            stmt.close();

            System.out.println("Product deactivated using direct SQL update!");

        } catch (SQLException e) {
            System.err.println("Alternative deactivate also failed: " + e.getMessage());
            showAlert("Database Error", "Failed to deactivate product: " + e.getMessage());
        }
    }

    private boolean validateForm() {
        // Validate required fields
        if (txtSku == null || txtSku.getText().isEmpty() ||
                txtProductName == null || txtProductName.getText().isEmpty() ||
                cmbCategory == null || cmbCategory.getValue() == null ||
                txtPrice == null || txtPrice.getText().isEmpty()) {
            showAlert("Validation Error", "Please fill in all required fields (SKU, Name, Category, Price).");
            return false;
        }

        // Validate numeric fields
        try {
            double price = Double.parseDouble(txtPrice.getText());
            if (price <= 0) {
                showAlert("Validation Error", "Price must be greater than 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid number for price.");
            return false;
        }

        return true;
    }

    private void clearForm() {
        if (txtSku != null) txtSku.clear();
        if (txtProductName != null) txtProductName.clear();
        if (cmbCategory != null && cmbCategory.getItems().size() > 0)
            cmbCategory.setValue(cmbCategory.getItems().get(0));
        if (txtPrice != null) txtPrice.clear();
        if (txtStock != null) txtStock.clear();
        if (txtSupplier != null) txtSupplier.clear();
        if (txtDescription != null) txtDescription.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Product Model Class
    public static class Product {
        private final IntegerProperty productId;
        private final StringProperty sku;
        private final StringProperty name;
        private final StringProperty category;
        private final IntegerProperty stockLevel;
        private final DoubleProperty price;
        private final StringProperty description;
        private final StringProperty supplier;
        private final StringProperty warehouse;

        public Product(int productId, String sku, String name, String category,
                       int stockLevel, double price, String description,
                       String supplier, String warehouse) {
            this.productId = new SimpleIntegerProperty(productId);
            this.sku = new SimpleStringProperty(sku != null ? sku : "");
            this.name = new SimpleStringProperty(name != null ? name : "");
            this.category = new SimpleStringProperty(category != null ? category : "Uncategorized");
            this.stockLevel = new SimpleIntegerProperty(stockLevel);
            this.price = new SimpleDoubleProperty(price);
            this.description = new SimpleStringProperty(description != null ? description : "");
            this.supplier = new SimpleStringProperty(supplier != null ? supplier : "");
            this.warehouse = new SimpleStringProperty(warehouse != null ? warehouse : "");
        }

        // Getters
        public int getProductId() { return productId.get(); }
        public String getSku() { return sku.get(); }
        public String getName() { return name.get(); }
        public String getCategory() { return category.get(); }
        public int getStockLevel() { return stockLevel.get(); }
        public double getPrice() { return price.get(); }
        public String getDescription() { return description.get(); }
        public String getSupplier() { return supplier.get(); }
        public String getWarehouse() { return warehouse.get(); }

        // Property getters
        public IntegerProperty productIdProperty() { return productId; }
        public StringProperty skuProperty() { return sku; }
        public StringProperty nameProperty() { return name; }
        public StringProperty categoryProperty() { return category; }
        public IntegerProperty stockLevelProperty() { return stockLevel; }
        public DoubleProperty priceProperty() { return price; }
        public StringProperty descriptionProperty() { return description; }
        public StringProperty supplierProperty() { return supplier; }
        public StringProperty warehouseProperty() { return warehouse; }
    }

    // Sidebar Button Handlers (for other manager features)
    @FXML
    private void handleCategorizeProducts() {
        // Open categorize products modal
        System.out.println("Categorize Products clicked");
    }

    @FXML
    private void handleMaintainStock() {
        // Open stock management page
        System.out.println("Maintain Stock clicked");
    }

    @FXML
    private void handleApprovePurchases() {
        // Open purchase approval page
        System.out.println("Approve Purchases clicked");
    }

    @FXML
    private void handleGenerateReports() {
        // Open reports generation page
        System.out.println("Generate Reports clicked");
    }

    @FXML
    private void handleLowStockNotifications() {
        // Show low stock details
        try {
            ResultSet rs = DBHelper.executeFunction("GetLowStockProducts");
            StringBuilder sb = new StringBuilder();
            sb.append("Low Stock Products:\n\n");

            while (rs != null && rs.next()) {
                String productName = rs.getString("productname");
                int quantity = rs.getInt("quantityavailable");
                int threshold = rs.getInt("minthreshold");
                sb.append(productName).append(": ")
                        .append(quantity).append(" (Threshold: ").append(threshold).append(")\n");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Low Stock Details");
            alert.setHeaderText("Low Stock Products");
            alert.setContentText(sb.toString());
            alert.showAndWait();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load low stock details: " + e.getMessage());
        }
    }
}