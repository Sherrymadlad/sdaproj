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
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.application.Platform;
import java.math.BigDecimal;

public class ManagerMainPageController implements Initializable {

    @FXML private TextField txtSearch;
    @FXML private TextField txtLowStock; // Added for low stock threshold

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, String> colSupplier;
    @FXML private TableColumn<Product, String> colStatus; // Added for low stock status
    @FXML private TableColumn<Product, Void> colActions;

    // Sidebar buttons from FXML
    @FXML private Button btnAddDeleteProducts, btnCategorizeProducts, btnMaintainStock;
    @FXML private Button btnApprovePurchases, btnGenerateReports, btnLowStockNotifications;
    @FXML private Button btnCloseModal, btnUploadImage;

    // Modal buttons
    @FXML private Button btnAddProduct, btnDeleteProduct, btnUpdateProduct, btnSaveProduct, btnCancel;

    // UI components
    @FXML private StackPane productModal, notificationBadge;
    @FXML private Label lblNotificationCount;
    @FXML private TextField txtProductName, txtSku, txtPrice, txtStock, txtSupplier;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private ComboBox<Integer> cmbCategoryId;
    @FXML private ComboBox<Integer> cmbSupplierId;
    @FXML private ImageView productImage;

    private final Image placeholderImage = new Image(Objects.requireNonNull(getClass().getResource("/com/example/testing2/images/placeholder.png")).toExternalForm());
    private Product selectedProduct;
    private final Map<Integer, Product> allProducts = new HashMap<>();
    private final Map<String, Integer> categoryNameToId = new HashMap<>();
    private final Map<String, Integer> supplierNameToId = new HashMap<>();
    private byte[] productImageData;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        initializeTableColumns();
        setupEventHandlers();
        loadProductsFromDatabase();
        loadCategories();
        loadSuppliers();
        checkLowStockNotifications();

        // Set image placeholder
        if (productImage != null) {
            productImage.setImage(placeholderImage);
        }

        // Set close modal button handler
        if (btnCloseModal != null) {
            btnCloseModal.setOnAction(e -> handleCancel());
        }

        // Set upload image button handler
        if (btnUploadImage != null) {
            btnUploadImage.setOnAction(e -> handleUploadImage());
        }

        // Set sidebar button handlers
        if (btnAddDeleteProducts != null) {
            btnAddDeleteProducts.setOnAction(e -> handleAddProduct());
        }

        if (btnCategorizeProducts != null) {
            btnCategorizeProducts.setOnAction(e -> handleCategorizeProducts());
        }

        if (btnMaintainStock != null) {
            btnMaintainStock.setOnAction(e -> handleMaintainStock());
        }

        if (btnApprovePurchases != null) {
            btnApprovePurchases.setOnAction(e -> handleApprovePurchases());
        }

        if (btnGenerateReports != null) {
            btnGenerateReports.setOnAction(e -> handleGenerateReports());
        }

        if (btnLowStockNotifications != null) {
            btnLowStockNotifications.setOnAction(e -> handleLowStockNotifications());
        }
    }

    private void initializeTableColumns() {
        try {
            if (colId != null) {
                colId.setCellValueFactory(cellData -> cellData.getValue().productIdProperty().asObject());
            }

            if (colSku != null) {
                colSku.setCellValueFactory(cellData -> cellData.getValue().skuProperty());
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

            if (colStatus != null) {
                colStatus.setCellValueFactory(cellData -> {
                    Product product = cellData.getValue();
                    // Check if stock is low (less than or equal to 5)
                    boolean isLowStock = product.getStockLevel() <= 5;
                    return new SimpleStringProperty(isLowStock ? "⚠ Low" : "OK");
                });

                // Add cell factory for styling low stock cells
                colStatus.setCellFactory(column -> new TableCell<Product, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            if (item.equals("⚠ Low")) {
                                setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            } else {
                                setStyle("-fx-text-fill: green;");
                            }
                        }
                    }
                });
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
            // Using the GetAllProducts function from your database
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
            Connection conn = DBHelper.getConnection();
            String query = "SELECT categoryid, name FROM category ORDER BY name";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            List<String> categories = new ArrayList<>();
            List<Integer> categoryIds = new ArrayList<>();
            categoryNameToId.clear();

            while (rs.next()) {
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

            rs.close();
            stmt.close();

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
            // Bring to front and show
            productModal.toFront();
            productModal.setVisible(true);
            System.out.println("Add Product modal shown");
        }
    }

    private void handleUpdateProduct() {
        if (selectedProduct != null) {
            editProduct(selectedProduct);
        } else {
            showAlert("Selection Error", "Please select a product to update.");
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

                showAlert("Success", "Product saved successfully!");

            } catch (SQLException e) {
                System.err.println("Failed to save product: " + e.getMessage());
                showAlert("Database Error", "Failed to save product: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void handleCancel() {
        if (productModal != null) {
            productModal.setVisible(false);
        }
        clearForm();
    }

    private void addProductToDatabase() throws SQLException {
        String sku = txtSku.getText();
        String name = txtProductName.getText();
        Integer categoryId = categoryNameToId.get(cmbCategory.getValue());

        // Use BigDecimal instead of double
        BigDecimal price;
        try {
            price = new BigDecimal(txtPrice.getText());
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter a valid price.");
            return;
        }

        String description = txtDescription.getText();
        Integer supplierId = null;

        // Get supplier ID from name or create new supplier
        String supplierName = txtSupplier.getText();
        if (supplierName != null && !supplierName.trim().isEmpty()) {
            supplierId = supplierNameToId.get(supplierName);
            if (supplierId == null) {
                // Create new supplier if not exists
                Connection conn = DBHelper.getConnection();
                String insertSupplier = "INSERT INTO supplier (name) VALUES (?) RETURNING supplierid";
                PreparedStatement stmt = conn.prepareStatement(insertSupplier);
                stmt.setString(1, supplierName);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    supplierId = rs.getInt(1);
                    supplierNameToId.put(supplierName, supplierId);
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

        // Execute AddProduct function using prepared statement directly
        try (Connection conn = DBHelper.getConnection()) {
            String sql = "SELECT AddProduct(?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sku);
            stmt.setString(2, name);
            stmt.setInt(3, categoryId);
            stmt.setBigDecimal(4, price);  // Using setBigDecimal
            stmt.setInt(5, supplierId);
            stmt.setString(6, description);
            stmt.execute();
        }



        // Get warehouse ID (use first warehouse)
        int warehouseId = 1; // Default to first warehouse

        // Execute AddProduct function
        DBHelper.executeProcedure(
                "AddProduct",
                sku,
                name,
                categoryId,
                price,
                supplierId,
                description
        );

        // Get the newly created product ID
        Connection conn = DBHelper.getConnection();
        String getProductIdQuery = "SELECT productid FROM product WHERE sku = ?";
        PreparedStatement stmt = conn.prepareStatement(getProductIdQuery);
        stmt.setString(1, sku);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            int newProductId = rs.getInt("productid");

            // Create stock entry
            String insertStockQuery = "INSERT INTO stock (productid, warehouseid, quantityavailable, minthreshold) VALUES (?, ?, ?, ?)";
            PreparedStatement stockStmt = conn.prepareStatement(insertStockQuery);
            stockStmt.setInt(1, newProductId);
            stockStmt.setInt(2, warehouseId);

            // Set stock quantity if provided
            int stockQuantity = 0;
            try {
                stockQuantity = Integer.parseInt(txtStock.getText());
            } catch (NumberFormatException e) {
                stockQuantity = 0;
            }
            stockStmt.setInt(3, stockQuantity);

            // Set min threshold if provided
            int minThreshold = 5; // Default
            try {
                minThreshold = Integer.parseInt(txtLowStock.getText());
            } catch (NumberFormatException e) {
                minThreshold = 5;
            }
            stockStmt.setInt(4, minThreshold);

            stockStmt.executeUpdate();
            stockStmt.close();
        }

        rs.close();
        stmt.close();

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

        // Get supplier ID from name
        String supplierName = txtSupplier.getText();
        if (supplierName != null && !supplierName.trim().isEmpty()) {
            supplierId = supplierNameToId.get(supplierName);
        }

        if (categoryId == null) {
            showAlert("Error", "Please select a valid category");
            return;
        }

        // Update product using UpdateProduct function
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

        // Update stock if quantity changed
        try {
            int newStock = Integer.parseInt(txtStock.getText());
            Connection conn = DBHelper.getConnection();
            String updateStockQuery = "UPDATE stock SET quantityavailable = ? WHERE productid = ?";
            PreparedStatement stmt = conn.prepareStatement(updateStockQuery);
            stmt.setInt(1, newStock);
            stmt.setInt(2, selectedProduct.getProductId());
            stmt.executeUpdate();
            stmt.close();
        } catch (NumberFormatException e) {
            // Stock field not a number, skip update
        }

        // Update min threshold if provided
        try {
            int minThreshold = Integer.parseInt(txtLowStock.getText());
            Connection conn = DBHelper.getConnection();
            String updateThresholdQuery = "UPDATE stock SET minthreshold = ? WHERE productid = ?";
            PreparedStatement stmt = conn.prepareStatement(updateThresholdQuery);
            stmt.setInt(1, minThreshold);
            stmt.setInt(2, selectedProduct.getProductId());
            stmt.executeUpdate();
            stmt.close();
        } catch (NumberFormatException e) {
            // Threshold field not a number, skip update
        }

        System.out.println("Product updated successfully!");
    }

    private void editProduct(Product product) {
        selectedProduct = product;

        // Debug: Print what we're trying to set
        System.out.println("=== Editing Product ===");
        System.out.println("Product SKU: " + product.getSku());
        System.out.println("Product Name: " + product.getName());
        System.out.println("Product Category: " + product.getCategory());
        System.out.println("Product Price: " + product.getPrice());

        if (txtSku != null) {
            txtSku.setText(product.getSku());
            System.out.println("Set SKU to: " + txtSku.getText());
        }

        if (txtProductName != null) {
            txtProductName.setText(product.getName());
            System.out.println("Set Product Name to: " + txtProductName.getText());
        }

        if (cmbCategory != null) {
            // Find the category in the combo box
            String category = product.getCategory();
            if (category != null && !category.trim().isEmpty()) {
                // Try to find exact match
                for (String cat : cmbCategory.getItems()) {
                    if (cat.equalsIgnoreCase(category)) {
                        cmbCategory.setValue(cat);
                        System.out.println("Set Category to: " + cmbCategory.getValue());
                        break;
                    }
                }
                // If not found, set the first item
                if (cmbCategory.getValue() == null && cmbCategory.getItems().size() > 0) {
                    cmbCategory.setValue(cmbCategory.getItems().get(0));
                    System.out.println("Set Category to first item: " + cmbCategory.getValue());
                }
            }
        }

        if (txtPrice != null) {
            txtPrice.setText(String.valueOf(product.getPrice()));
            System.out.println("Set Price to: " + txtPrice.getText());
        }

        if (txtStock != null) {
            txtStock.setText(String.valueOf(product.getStockLevel()));
            System.out.println("Set Stock to: " + txtStock.getText());
        }

        if (txtDescription != null) {
            txtDescription.setText(product.getDescription());
        }

        if (txtSupplier != null) {
            txtSupplier.setText(product.getSupplier());
        }

        // Load low stock threshold from database
        try {
            Connection conn = DBHelper.getConnection();
            String query = "SELECT minthreshold FROM stock WHERE productid = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, product.getProductId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int minThreshold = rs.getInt("minthreshold");
                if (txtLowStock != null) {
                    txtLowStock.setText(String.valueOf(minThreshold));
                    System.out.println("Set Low Stock Threshold to: " + minThreshold);
                }
            } else {
                // Default threshold
                if (txtLowStock != null) {
                    txtLowStock.setText("5");
                    System.out.println("Set Low Stock Threshold to default: 5");
                }
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Failed to load low stock threshold: " + e.getMessage());
            if (txtLowStock != null) {
                txtLowStock.setText("5"); // Default value
            }
        }

        if (productModal != null) {
            productModal.setVisible(true);
            System.out.println("Modal set to visible");
        }
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
                                    "You can deactivate the product instead.");

                    // Offer to deactivate instead
                    Alert deactivateAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    deactivateAlert.setTitle("Deactivate Product");
                    deactivateAlert.setHeaderText("Deactivate " + product.getName());
                    deactivateAlert.setContentText("Do you want to deactivate this product instead? It will be hidden but not deleted.");

                    Optional<ButtonType> deactivateResult = deactivateAlert.showAndWait();
                    if (deactivateResult.isPresent() && deactivateResult.get() == ButtonType.OK) {
                        deactivateProduct(product.getProductId());
                        loadProductsFromDatabase();
                        checkLowStockNotifications();
                    }
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

    private void deactivateProduct(int productId) {
        try {
            DBHelper.executeProcedure("DeactivateProduct", productId);
            System.out.println("Product deactivated successfully!");
            showAlert("Success", "Product has been deactivated.");
        } catch (SQLException e) {
            System.err.println("Failed to deactivate product: " + e.getMessage());
            showAlert("Database Error", "Failed to deactivate product: " + e.getMessage());
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

            // 1. Delete from productmovement
            String deleteMovementQuery = "DELETE FROM productmovement WHERE productid = ?";
            PreparedStatement stmt4 = conn.prepareStatement(deleteMovementQuery);
            stmt4.setInt(1, productId);
            stmt4.executeUpdate();
            stmt4.close();

            // 2. Delete from batchlot
            String deleteBatchQuery = "DELETE FROM batchlot WHERE productid = ?";
            PreparedStatement stmt1 = conn.prepareStatement(deleteBatchQuery);
            stmt1.setInt(1, productId);
            stmt1.executeUpdate();
            stmt1.close();

            // 3. Delete from stock
            String deleteStockQuery = "DELETE FROM stock WHERE productid = ?";
            PreparedStatement stmt2 = conn.prepareStatement(deleteStockQuery);
            stmt2.setInt(1, productId);
            stmt2.executeUpdate();
            stmt2.close();

            // 4. Delete from orderitem
            String deleteOrderItemQuery = "DELETE FROM orderitem WHERE productid = ?";
            PreparedStatement stmt3 = conn.prepareStatement(deleteOrderItemQuery);
            stmt3.setInt(1, productId);
            stmt3.executeUpdate();
            stmt3.close();

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

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Product Image");

        // Set extension filters
        FileChooser.ExtensionFilter imageFilter = new FileChooser.ExtensionFilter(
                "Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        fileChooser.getExtensionFilters().add(imageFilter);

        // Show open file dialog
        File file = fileChooser.showOpenDialog(productImage.getScene().getWindow());

        if (file != null) {
            try {
                // Load the image
                Image image = new Image(file.toURI().toString());
                productImage.setImage(image);

                // Read image file to byte array (alternative method)
                productImageData = readFileToBytes(file);

                System.out.println("Image loaded: " + file.getName());
            } catch (Exception e) {
                System.err.println("Failed to load image: " + e.getMessage());
                showAlert("Image Error", "Failed to load image: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // Helper method to read file to byte array
    private byte[] readFileToBytes(File file) {
        try {
            // Using FileInputStream to read bytes
            java.io.FileInputStream fis = new java.io.FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            return data;
        } catch (java.io.IOException e) {
            System.err.println("Error reading file to bytes: " + e.getMessage());
            return null;
        }
    }

    private boolean validateForm() {
        // Debug: Print current values
        System.out.println("=== Validating Form ===");
        System.out.println("SKU: " + (txtSku != null ? txtSku.getText() : "txtSku is null"));
        System.out.println("Name: " + (txtProductName != null ? txtProductName.getText() : "txtProductName is null"));
        System.out.println("Category: " + (cmbCategory != null ? cmbCategory.getValue() : "cmbCategory is null"));
        System.out.println("Price: " + (txtPrice != null ? txtPrice.getText() : "txtPrice is null"));

        // Check if any required field is null
        if (txtSku == null) {
            System.err.println("Error: txtSku is null in FXML!");
            showAlert("Form Error", "SKU field is not properly initialized.");
            return false;
        }
        if (txtProductName == null) {
            System.err.println("Error: txtProductName is null in FXML!");
            showAlert("Form Error", "Product Name field is not properly initialized.");
            return false;
        }
        if (cmbCategory == null) {
            System.err.println("Error: cmbCategory is null in FXML!");
            showAlert("Form Error", "Category field is not properly initialized.");
            return false;
        }
        if (txtPrice == null) {
            System.err.println("Error: txtPrice is null in FXML!");
            showAlert("Form Error", "Price field is not properly initialized.");
            return false;
        }

        // Now validate the content
        StringBuilder errorMessage = new StringBuilder();

        if (txtSku.getText() == null || txtSku.getText().trim().isEmpty()) {
            errorMessage.append("• SKU is required\n");
        }

        if (txtProductName.getText() == null || txtProductName.getText().trim().isEmpty()) {
            errorMessage.append("• Product Name is required\n");
        }

        if (cmbCategory.getValue() == null || cmbCategory.getValue().trim().isEmpty()) {
            errorMessage.append("• Category is required\n");
        }

        if (txtPrice.getText() == null || txtPrice.getText().trim().isEmpty()) {
            errorMessage.append("• Price is required\n");
        } else {
            try {
                double price = Double.parseDouble(txtPrice.getText().trim());
                if (price <= 0) {
                    errorMessage.append("• Price must be greater than 0\n");
                }
            } catch (NumberFormatException e) {
                errorMessage.append("• Price must be a valid number\n");
            }
        }

        // If there are errors, show them
        if (errorMessage.length() > 0) {
            showAlert("Validation Error", "Please correct the following errors:\n\n" + errorMessage.toString());
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
        if (txtLowStock != null) txtLowStock.clear();
        if (txtDescription != null) txtDescription.clear();
        if (productImage != null) {
            productImage.setImage(placeholderImage);
        }
        productImageData = null;
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Sidebar Button Handlers
    @FXML
    private void handleCategorizeProducts() {
        // Open categorize products modal
        System.out.println("Categorize Products clicked");
        // You can implement category management here
    }

    @FXML
    private void handleMaintainStock() {
        // Open stock management page
        System.out.println("Maintain Stock clicked");
        // You can implement stock adjustment functionality here
    }

    @FXML
    private void handleApprovePurchases() {
        // Open purchase approval page
        System.out.println("Approve Purchases clicked");
        // You can implement purchase order approval here
    }

    @FXML
    private void handleGenerateReports() {
        // Open reports generation page
        System.out.println("Generate Reports clicked");
        try {
            // Example: Show stock report
            ResultSet rs = DBHelper.executeFunction("GetStockReport");
            StringBuilder report = new StringBuilder();
            report.append("Stock Report:\n\n");

            int count = 0;
            while (rs != null && rs.next()) {
                String productName = rs.getString("productname");
                String warehouseName = rs.getString("warehousename");
                int quantity = rs.getInt("quantityavailable");
                report.append(productName).append(" at ").append(warehouseName)
                        .append(": ").append(quantity).append(" units\n");
                count++;
            }

            if (count == 0) {
                report.append("No stock data available.");
            }

            TextArea textArea = new TextArea(report.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setPrefRowCount(20);

            ScrollPane scrollPane = new ScrollPane(textArea);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefSize(600, 400);

            Stage stage = new Stage();
            stage.setTitle("Stock Report");
            stage.setScene(new Scene(scrollPane));
            stage.show();

        } catch (SQLException e) {
            showErrorAlert("Database Error", "Failed to generate report: " + e.getMessage());
            e.printStackTrace();
        }
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
                sb.append("• ").append(productName).append(": ")
                        .append(quantity).append(" units (Threshold: ").append(threshold).append(")\n");
            }

            if (sb.toString().equals("Low Stock Products:\n\n")) {
                sb.append("No low stock products found.");
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Low Stock Details");
            alert.setHeaderText("Low Stock Products");
            alert.setContentText(sb.toString());
            alert.getDialogPane().setPrefSize(400, 300);
            alert.showAndWait();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load low stock details: " + e.getMessage());
        }
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
}