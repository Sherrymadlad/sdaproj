package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.beans.property.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class ManagerMainPageController implements Initializable, SidebarListener {

    @FXML private TextField txtSearch;

    @FXML private TableView<Product> productsTable;
    @FXML private TableColumn<Product, Integer> colId;
    @FXML private TableColumn<Product, String> colSku;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, String> colCategory;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, String> colDescription;
    private int currentUserId;

    @FXML private Button btnAddProduct, btnDeleteProduct, btnUpdateProduct, btnSaveProduct, btnCancel;
    @FXML private StackPane productModal, notificationBadge;
    @FXML private Label lblNotificationCount;

    @FXML private TextField txtProductName, txtPrice, txtStock;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbCategory;
    @FXML private ComboBox<Integer> cmbCategoryId;
    @FXML private ComboBox<String> cmbSupplierName;

    private Product selectedProduct;
    private final Map<Integer, Product> allProducts = new HashMap<>();
    private final Map<String, Integer> categoryNameToId = new HashMap<>();
    private final Map<String, Integer> supplierNameToId = new HashMap<>();
    private ManagerSidebarController managerSidebarController;
    @FXML private AnchorPane sidebarContainer;
    @FXML private AnchorPane mainContent;
    @FXML private TextField txtLowStock;
    private CustomModalController customModalController;
    @FXML AnchorPane root;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSidebar();
        setupModal();
        setCurrentUserId(20);
        initializeTableColumns();
        setupEventHandlers();
        loadProductsFromDatabase();
        loadCategories();
        loadSuppliers();
    }

    private void setupModal(){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomModal.fxml"));
            StackPane modal = loader.load();
            customModalController = loader.getController();

            root.getChildren().add(modal);
            AnchorPane.setTopAnchor(modal, 0.0);
            AnchorPane.setBottomAnchor(modal, 0.0);
            AnchorPane.setLeftAnchor(modal, 0.0);
            AnchorPane.setRightAnchor(modal, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void setupSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/ManagerSidebar.fxml"));
            AnchorPane sidebar = loader.load();
            managerSidebarController = loader.getController();
            managerSidebarController.setSidebarListener(this);

            sidebarContainer.getChildren().add(sidebar);
            AnchorPane.setTopAnchor(sidebar, 0.0);
            AnchorPane.setBottomAnchor(sidebar, 0.0);
            AnchorPane.setLeftAnchor(sidebar, 0.0);
            AnchorPane.setRightAnchor(sidebar, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPageSelected(String pageName) {
        try {
            mainContent.getChildren().removeIf(node -> "overlay".equals(node.getUserData()));

            switch (pageName) {
                case "Products" -> {
                    txtSearch.clear();
                    productsTable.getSelectionModel().clearSelection();
                    loadProductsFromDatabase();
                    return;
                }
                case "Orders" -> loadPage("/com/example/testing2/StaffOrdersPage.fxml");
                case "Reports" -> loadPage("/com/example/testing2/StaffReportsPage.fxml");
                case "Notifications" -> loadPage("/com/example/testing2/NotificationsPage.fxml");
                case "Profile" -> {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomerProfilePage.fxml"));
                    AnchorPane page = loader.load();
                    CustomerProfilePageController controller = loader.getController();
                    controller.setCurrentUserId(currentUserId);
                    addOverlay(page);
                }
                default -> System.out.println("Unknown page: " + pageName);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadPage(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        AnchorPane page = loader.load();
        addOverlay(page);
    }

    private void addOverlay(AnchorPane page) {
        page.setUserData("overlay");
        mainContent.getChildren().add(page);
        AnchorPane.setTopAnchor(page, 0.0);
        AnchorPane.setBottomAnchor(page, 0.0);
        AnchorPane.setLeftAnchor(page, 0.0);
        AnchorPane.setRightAnchor(page, 0.0);
    }

    public void setCurrentUserId(int userId) { this.currentUserId = userId; }

    private void initializeTableColumns() {
        try {
            colId.setCellValueFactory(cellData -> cellData.getValue().productIdProperty().asObject());
            colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
            colCategory.setCellValueFactory(cellData -> cellData.getValue().categoryProperty());
            colPrice.setCellValueFactory(cellData -> cellData.getValue().priceProperty().asObject());
            colStock.setCellValueFactory(cellData -> cellData.getValue().stockLevelProperty().asObject());
            colDescription.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
            Platform.runLater(() -> {
                colDescription.prefWidthProperty().bind(
                        productsTable.widthProperty()
                                .subtract(colId.getWidth()
                                        + colName.getWidth()
                                        + colCategory.getWidth()
                                        + colPrice.getWidth()
                                        + colStock.getWidth() + 20)   // padding
                );
            });

        } catch (Exception e) {
            System.err.println("Error initializing table columns: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupEventHandlers() {
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterProducts(newVal));
        productsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    selectedProduct = newVal;
                    btnDeleteProduct.setDisable(newVal == null);
                    btnUpdateProduct.setDisable(newVal == null);
                });

        btnAddProduct.setOnAction(e -> handleAddProduct());
        btnDeleteProduct.setOnAction(e -> handleDeleteProduct());
        btnUpdateProduct.setOnAction(e -> handleUpdateProduct());
        btnSaveProduct.setOnAction(e -> handleSaveProduct());
        btnCancel.setOnAction(e -> handleCancel());
    }

    private void loadProductsFromDatabase() {
        javafx.concurrent.Task<List<Product>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<Product> call() throws Exception {
                List<Product> products = new ArrayList<>();
                // Keep DBHelper call
                var rs = DBHelper.executeFunction("GetAllProducts");

                allProducts.clear();

                while (rs != null && rs.next()) {
                    // Populate product objects
                    Product product = new Product(
                            rs.getInt("productid"),
                            rs.getString("sku"),
                            rs.getString("productname"),
                            rs.getString("categoryname"),
                            rs.getInt("stocklevel"),
                            rs.getDouble("price"),
                            rs.getString("description"),
                            rs.getString("suppliername"),
                            rs.getString("warehouse")
                    );
                    products.add(product);
                    allProducts.put(product.getProductId(), product);
                }
                return products;
            }
        };

        task.setOnSucceeded(e -> productsTable.getItems().setAll(task.getValue()));
        task.setOnFailed(e -> showCustomModal("Database Error!\nFailed to load products: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    private void loadCategories() {
        try {
            var rs = DBHelper.executeFunction("GetAllCategories");
            List<String> categories = new ArrayList<>();
            categoryNameToId.clear();

            while (rs != null && rs.next()) {
                int categoryId = rs.getInt("categoryid");
                String categoryName = rs.getString("name");

                if (categoryName != null && !categoryName.trim().isEmpty()) {
                    categories.add(categoryName);
                    categoryNameToId.put(categoryName, categoryId);
                }
            }

            if (cmbCategory != null) {
                cmbCategory.getItems().setAll(categories);
                if (!categories.isEmpty()) {
                    cmbCategory.setValue(categories.get(0)); // select first by default
                }
            }

            System.out.println("Loaded " + categories.size() + " categories");

        } catch (SQLException e) {
            System.err.println("Failed to load categories: " + e.getMessage());
            showCustomModal("Database Error\nFailed to load categories: " + e.getMessage());
        }
    }

    private void loadSuppliers() {
        try {
            var rs = DBHelper.executeFunction("GetAllSuppliers"); // Your PSQL function
            List<String> supplierNames = new ArrayList<>();
            supplierNameToId.clear(); // reset the map

            while (rs != null && rs.next()) {
                int supplierId = rs.getInt("supplierid");
                String supplierName = rs.getString("name");

                if (supplierName != null && !supplierName.trim().isEmpty()) {
                    supplierNames.add(supplierName);
                    supplierNameToId.put(supplierName, supplierId); // Map name -> ID
                }
            }

            if (cmbSupplierName != null) {
                cmbSupplierName.getItems().setAll(supplierNames);
                if (!supplierNames.isEmpty()) {
                    cmbSupplierName.setValue(supplierNames.get(0)); // Select first by default
                }
            }

            System.out.println("Loaded " + supplierNames.size() + " suppliers");

        } catch (SQLException e) {
            System.err.println("Failed to load suppliers: " + e.getMessage());
            showCustomModal("Database Error\nFailed to load suppliers: " + e.getMessage());
        }
    }

    private void filterProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            productsTable.getItems().setAll(allProducts.values());
            return;
        }
        query = query.toLowerCase();
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts.values()) {
            if (p.getName().toLowerCase().contains(query) ||
                    p.getSku().toLowerCase().contains(query) ||
                    p.getCategory().toLowerCase().contains(query) ||
                    p.getSupplier().toLowerCase().contains(query)) {
                filtered.add(p);
            }
        }
        productsTable.getItems().setAll(filtered);
    }

    private void handleAddProduct() { selectedProduct = null; clearForm(); productModal.setVisible(true); }
    private void handleUpdateProduct() { if (selectedProduct != null) editProduct(selectedProduct); }
    private void handleDeleteProduct() { if (selectedProduct != null) deleteProduct(selectedProduct); }
    private void handleSaveProduct() {
        if (validateForm()) {
            try {
                if (selectedProduct == null) addProductToDatabase();
                else updateProductInDatabase();

                productModal.setVisible(false);
                loadProductsFromDatabase();

            } catch (SQLException e) { showCustomModal("Database Error\nFailed to save product: " + e.getMessage()); }
        }
    }
    private void handleCancel() { productModal.setVisible(false); }

    private String generateUniqueSku() {
        long timestamp = System.currentTimeMillis(); // current time in ms
        int random = new Random().nextInt(900) + 100; // random 100-999
        return "PROD" + timestamp + random;
    }


    private void addProductToDatabase() throws SQLException {
        // 1. Auto-generate SKU
        String sku = generateUniqueSku();

        // 2. Get other form values
        String name = txtProductName.getText().trim();
        String description = txtDescription.getText().trim();

        // Category ID mapping
        String categoryName = cmbCategory.getValue();
        if (categoryName == null || !categoryNameToId.containsKey(categoryName)) {
            showCustomModal("Validation Error\nPlease select a valid category.");
            return;
        }
        int categoryId = categoryNameToId.get(categoryName);

        // Supplier ID mapping
        String supplierName = cmbSupplierName.getValue();
        if (supplierName == null || !supplierNameToId.containsKey(supplierName)) {
            showCustomModal("Validation Error\nPlease select a valid supplier.");
            return;
        }
        int supplierId = supplierNameToId.get(supplierName);

        // Price validation
        BigDecimal price;
        try {
            price = new BigDecimal(txtPrice.getText().trim());
        } catch (NumberFormatException e) {
            showCustomModal("Validation Error\nPrice must be numeric.");
            return;
        }

        // Stock quantity validation
        int stockQuantity;
        try { stockQuantity = Integer.parseInt(txtStock.getText().trim()); }
        catch (NumberFormatException e) { showCustomModal("Validation Error\nStock must be numeric."); return; }

        // Low stock threshold validation
        int lowStock;
        try {
            String lowStockText = txtLowStock.getText().trim();
            lowStock = lowStockText.isEmpty() ? 5 : Integer.parseInt(lowStockText);
        } catch (NumberFormatException e) { showCustomModal("Validation Error\nLow Stock must be numeric."); return; }

        // 3. Call AddProductWithStock function
        var rs = DBHelper.executeFunction("AddProduct", sku, name, categoryId, price, supplierId, description, stockQuantity, lowStock);

        // 4. Read result message
        if (rs != null && rs.next()) {
            String result = rs.getString(1);
            if (result.startsWith("SUCCESS")) {
                showCustomModal("Success\n"+result);
            } else {
                showCustomModal("Database Error\n"+result);
            }
        } else {
            showCustomModal("Database Error\nNo response from AddProduct function.");
        }
    }

    private void updateProductInDatabase() throws SQLException {
        if (selectedProduct == null) return;

        int productId = selectedProduct.getProductId();

        // 1. Gather form data
        String name = txtProductName.getText().trim();
        String description = txtDescription.getText().trim();
        String categoryName = cmbCategory.getValue();
        int categoryId = categoryNameToId.getOrDefault(categoryName, selectedProduct.getProductId());

        String supplierName = cmbSupplierName.getValue();
        int supplierId = supplierNameToId.getOrDefault(supplierName, selectedProduct.getProductId());

        BigDecimal price;
        try { price = new BigDecimal(txtPrice.getText().trim()); }
        catch (NumberFormatException e) { showCustomModal("Validation Error\nPrice must be numeric."); return; }

        int stockQuantity;
        try { stockQuantity = Integer.parseInt(txtStock.getText().trim()); }
        catch (NumberFormatException e) { showCustomModal("Validation Error\nStock must be numeric."); return; }

        int lowStock;
        try {
            String lowStockText = txtLowStock.getText().trim();
            lowStock = lowStockText.isEmpty() ? 5 : Integer.parseInt(lowStockText);
        } catch (NumberFormatException e) { showCustomModal("Validation Error\nLow Stock must be numeric."); return; }

        // 2. Update Product Details
        DBHelper.executeFunction("EditProductDetails", productId, null, name, categoryId, price, description, supplierId, null);

        // 3. Calculate stock change
        int stockChange = stockQuantity - selectedProduct.getStockLevel();

        // 4. Update Stock (assuming warehouse id = 1 for simplicity)
        DBHelper.executeFunction("UpdateStock", productId, 1, stockChange);

        // 5. Update Low Stock Threshold
        DBHelper.executeFunction("UpdateLowStockThreshold", productId, 1, lowStock);

        // 6. Show success and reload products
        showCustomModal("Success\nProduct updated successfully!");
        loadProductsFromDatabase();
    }

    private void editProduct(Product product) {
        selectedProduct = product;

        // Basic product info
        txtProductName.setText(product.getName());
        cmbCategory.setValue(product.getCategory());
        txtPrice.setText(String.valueOf(product.getPrice()));
        txtStock.setText(String.valueOf(product.getStockLevel()));
        txtDescription.setText(product.getDescription());
        cmbSupplierName.setValue(product.getSupplier());

        // Fetch low stock threshold from DB
        try {
            var rs = DBHelper.executeFunction("GetProductLowStock", product.getProductId(), 1); // assuming warehouseId = 1
            if (rs != null && rs.next()) {
                int lowStock = rs.getInt(1);
                txtLowStock.setText(String.valueOf(lowStock));
            } else {
                txtLowStock.setText("5"); // default if not found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            txtLowStock.setText("5"); // fallback default
        }

        // Show modal
        productModal.setVisible(true);
    }

    private void deleteProduct(Product product) {
        if (product == null) return;

        // Show confirmation using custom modal with optional callback
        showCustomModalConfirmation(
                "Delete Product\nAre you sure you want to delete " + product.getName() + "?",
                confirmed -> {
                    if (confirmed) {
                        try {
                            // Call the soft delete function
                            var rs = DBHelper.executeFunction("SoftDeleteProduct", product.getProductId());
                            if (rs != null && rs.next()) {
                                String message = rs.getString(1);
                                showCustomModal(message); // regular modal for success/error message
                            }

                            // Reload products to reflect the deletion
                            loadProductsFromDatabase();

                        } catch (SQLException e) {
                            showCustomModal("Database Error\nFailed to delete product: " + e.getMessage());
                        }
                    }
                }
        );
    }


    private void showCustomModalConfirmation(String message, java.util.function.Consumer<Boolean> callback) {
        if (customModalController != null) {
            customModalController.showConfirmation(message, callback);
        } else {
            // Fallback: auto-confirm if modal not initialized
            callback.accept(true);
        }
    }


    private boolean validateForm() {
        if (txtProductName.getText().isEmpty() ||
                cmbCategory.getValue() == null || txtPrice.getText().isEmpty()) {
            showCustomModal("Validation Error\nPlease fill all required fields.");
            return false;
        }

        try { return Double.parseDouble(txtPrice.getText()) > 0; }
        catch (NumberFormatException e) { showCustomModal("Validation Error\nPrice must be numeric."); return false; }
    }

    private void clearForm() {
        txtProductName.clear();
        txtPrice.clear();
        txtStock.clear();
        txtDescription.clear();
        txtLowStock.clear();

        if (!cmbCategory.getItems().isEmpty()) cmbCategory.setValue(cmbCategory.getItems().get(0));
        if (!cmbSupplierName.getItems().isEmpty()) cmbSupplierName.setValue(cmbSupplierName.getItems().get(0));
    }

    private void showCustomModal(String message) {
        if (customModalController != null) {
            customModalController.showMessage(message);
        } else {
            // fallback
            System.err.println("Modal not initialized: " + message);
        }
    }

    public static class Product {
        private final IntegerProperty productId;
        private final StringProperty sku, name, category, description, supplier, warehouse;
        private final IntegerProperty stockLevel;
        private final DoubleProperty price;

        public Product(int productId, String sku, String name, String category, int stockLevel, double price,
                       String description, String supplier, String warehouse) {
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

        public int getProductId() { return productId.get(); }
        public String getSku() { return sku.get(); }
        public String getName() { return name.get(); }
        public String getCategory() { return category.get(); }
        public int getStockLevel() { return stockLevel.get(); }
        public double getPrice() { return price.get(); }
        public String getDescription() { return description.get(); }
        public String getSupplier() { return supplier.get(); }
        public String getWarehouse() { return warehouse.get(); }

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
