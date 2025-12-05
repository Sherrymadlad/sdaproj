package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.control.ComboBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class StaffDashboardPageController implements Initializable, SidebarListener {

    @FXML private VBox categoriesContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterCategory;

    private int currentUserId;


    @FXML private AnchorPane itemCard; // TEMPLATE card from FXML (hidden)
    @FXML private StackPane productViewPanel;

    @FXML private ImageView viewItemImage;
    @FXML private Text viewItemName;
    @FXML private Text viewItemPrice;
    @FXML private Text viewItemDescription;

    @FXML private StackPane itemDetailsPanel;
    @FXML private ImageView modalItemImage;
    @FXML private Text modalItemName;
    @FXML private Text modalItemPrice;
    @FXML private Text modalItemDescription;
    @FXML private VBox warehousesContainer;
    @FXML private AnchorPane mainContent;


    @FXML private Button btnCloseModal;
    @FXML private ComboBox<String> cmbSortBy;
    @FXML private void handleCardHoverIn(MouseEvent event) { AnchorPane card = (AnchorPane) event.getSource(); card.setScaleX(1.05); card.setScaleY(1.05); }
    @FXML private void handleCardHoverOut(MouseEvent event) { AnchorPane card = (AnchorPane) event.getSource(); card.setScaleX(1.0); card.setScaleY(1.0); }

    private final Image placeholderImage =
            new Image(getClass().getResource("/com/example/testing2/images/placeholder.png").toExternalForm());

    private final Map<TilePane, List<AnchorPane>> categoryItemsMap = new HashMap<>();

    private StaffSidebarController staffSidebarController;
    @FXML private AnchorPane sidebarContainer;
    @FXML private ScrollPane scrollPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSidebar();
        itemCard.setVisible(false);
        categoriesContainer.setPadding(new Insets(70, 0, 0, 0));

        // SEARCH LISTENER
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterItems(newVal));

        // LOAD categories in filter dropdown
        cmbFilterCategory.getItems().add("All");
        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");
            while (rs.next()) {
                cmbFilterCategory.getItems().add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // SORT OPTIONS
        cmbSortBy.getItems().addAll(
                "Price Ascending",
                "Price Descending",
                "Alphabetical A-Z",
                "Alphabetical Z-A"
        );

        cmbFilterCategory.setOnAction(e -> applyFilterAndSort());
        cmbSortBy.setOnAction(e -> applyFilterAndSort());

        // Load initial data
        loadCategoriesWithItems();
        btnCloseModal.setOnAction(e -> itemDetailsPanel.setVisible(false));
    }

    private void setupSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/StaffSidebar.fxml"));
            AnchorPane sidebar = loader.load();
            staffSidebarController = loader.getController();
            staffSidebarController.setSidebarListener(this);

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
                case "Dashboard" -> {
                    resetDashboard();
                }
                case "Orders" -> loadPage("/com/example/testing2/StaffOrdersPage.fxml");
                case "Reports" -> loadPage("/com/example/testing2/StaffReportsPage.fxml");
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

    private void resetDashboard() {
        // Ensure UI changes happen on FX thread
        javafx.application.Platform.runLater(() -> {
            try {
                // Clear search and filters
                if (txtSearch != null) txtSearch.clear();
                if (cmbFilterCategory != null) cmbFilterCategory.setValue("All");
                if (cmbSortBy != null) cmbSortBy.setValue(null);

                // Hide modals / detail panels
                if (itemDetailsPanel != null) {
                    itemDetailsPanel.setVisible(false);
                    itemDetailsPanel.setUserData(null);
                }
                if (productViewPanel != null) productViewPanel.setVisible(false);

                // Clear any dynamic lists / containers
                if (warehousesContainer != null) warehousesContainer.getChildren().clear();

                // Clear categories UI cache then reload fresh content
                if (categoriesContainer != null) {
                    categoriesContainer.getChildren().clear();
                }
                categoryItemsMap.clear();

                // Re-load categories + items (this method repopulates categoriesContainer)
                loadCategoriesWithItems();

                // Scroll to top of list
                if (scrollPane != null) {
                    scrollPane.setVvalue(0.0);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /* ---------------------------------------------------------
       LOAD CATEGORIES AND ITEMS
       --------------------------------------------------------- */
    private void loadCategoriesWithItems() {
        categoriesContainer.getChildren().clear();
        categoryItemsMap.clear();

        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");

            while (rs.next()) {
                int categoryId = rs.getInt("categoryid");
                String categoryName = rs.getString("name");

                VBox categoryBox = new VBox(5);
                categoryBox.setPadding(new Insets(0));

                Text label = new Text(categoryName);
                label.setStyle("-fx-font-size: 34px; -fx-fill: #4a2c6e; -fx-font-weight: bold;");
                label.setTranslateX(60);

                TilePane itemsPane = new TilePane();
                itemsPane.setHgap(20);
                itemsPane.setVgap(20);
                itemsPane.setPadding(new Insets(10, 0, 10, 60));
                itemsPane.setPrefTileWidth(itemCard.getPrefWidth() * 1.5);
                itemsPane.setPrefTileHeight(itemCard.getPrefHeight() * 1.3);



                List<AnchorPane> cards = new ArrayList<>();

                ResultSet items = DBHelper.executeFunction("GetProductsByCategory", categoryId);
                while (items.next()) {
                    int itemId = items.getInt("productid");
                    String itemName = items.getString("name");
                    double itemPrice = items.getDouble("price");

                    AnchorPane card = createItemCard(itemId, itemName, itemPrice);
                    itemsPane.getChildren().add(card);
                    cards.add(card);
                }

                categoryItemsMap.put(itemsPane, cards);

                categoryBox.getChildren().addAll(label, itemsPane);
                categoriesContainer.getChildren().add(categoryBox);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* ---------------------------------------------------------
       CREATE ITEM CARD (VIEW-ONLY)
       --------------------------------------------------------- */
    private AnchorPane createItemCard(int id, String name, double price) {

        AnchorPane clone = new AnchorPane();
        clone.setPrefWidth(itemCard.getPrefWidth() * 1.5);
        clone.setPrefHeight(itemCard.getPrefHeight() * 1.5);

        // Match customer card styling
        clone.setStyle("-fx-background-color: #f5f0fa; " +
                "-fx-border-color: #c8b8d1; " +
                "-fx-border-radius: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.15), 6, 0, 0, 3);");

        // IMAGE (same as customer)
        ImageView img = new ImageView(placeholderImage);
        img.setFitWidth(clone.getPrefWidth() - 10);
        img.setFitHeight(clone.getPrefHeight() * 0.6);
        img.setLayoutX(5);
        img.setLayoutY(10);

        // NAME TEXT (match customer)
        Text itemName = new Text(name);
        itemName.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-fill: #4a2375; -fx-font-family: 'Verdana';");
        itemName.setWrappingWidth(clone.getPrefWidth() - 20);
        itemName.setLayoutX(10);
        itemName.setLayoutY(img.getFitHeight() + 40);

        // PRICE TEXT (match customer)
        Text itemPrice = new Text("Rs " + price);
        itemPrice.setStyle("-fx-font-size: 20px; -fx-font-weight: semi-bold; -fx-fill: #8b6fa1; -fx-font-family: 'Tahoma';");
        itemPrice.setLayoutX(10);
        itemPrice.setLayoutY(img.getFitHeight() + 75);

        clone.getChildren().addAll(img, itemName, itemPrice);

        // Keep staff-side behavior
        clone.setUserData(id);
        clone.setOnMouseEntered(this::handleCardHoverIn);
        clone.setOnMouseExited(this::handleCardHoverOut);
        clone.setOnMouseClicked(e -> openItemModal(id));

        return clone;
    }

    /* ---------------------------------------------------------
       OPEN PRODUCT VIEW (VIEW-ONLY)
       --------------------------------------------------------- */
    private void openProductView(int productId) {
        try {
            ResultSet rs = DBHelper.executeFunction("GetProductDetails", productId);

            if (rs.next()) {
                viewItemName.setText(rs.getString("name"));
                viewItemPrice.setText("Rs " + rs.getDouble("price"));
                viewItemDescription.setText(rs.getString("description"));
                viewItemImage.setImage(placeholderImage); // always placeholder
            }

            // Clear previous warehouses
            warehousesContainer.getChildren().removeIf(node -> node instanceof Text && node != warehousesContainer.getChildren().get(0));

            // Load warehouses for this product
            ResultSet warehouses = DBHelper.executeFunction("GetWarehousesForProduct", productId);
            while (warehouses.next()) {
                String warehouseName = warehouses.getString("name");
                Text warehouseText = new Text("- " + warehouseName);
                warehouseText.setStyle("-fx-font-size: 14px;");
                warehousesContainer.getChildren().add(warehouseText);
            }

            productViewPanel.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* ---------------------------------------------------------
       FILTER ITEMS
       --------------------------------------------------------- */
    private void filterItems(String query) {
        query = query.toLowerCase(Locale.ROOT);

        for (Node n : categoriesContainer.getChildren()) {
            if (!(n instanceof VBox categoryBox)) continue;

            TilePane pane = (TilePane) categoryBox.getChildren().get(1);
            List<AnchorPane> cards = categoryItemsMap.get(pane);

            List<AnchorPane> filtered = new ArrayList<>();

            for (AnchorPane card : cards) {
                Text nameText = (Text) card.getChildren().get(1);
                if (nameText.getText().toLowerCase().contains(query)) {
                    filtered.add(card);
                }
            }

            pane.getChildren().setAll(filtered);

            boolean visible = !filtered.isEmpty();
            categoryBox.setVisible(visible);
            categoryBox.setManaged(visible);
        }
    }

    /* ---------------------------------------------------------
       FILTER + SORT LOGIC
       --------------------------------------------------------- */
    private void applyFilterAndSort() {
        String category = cmbFilterCategory.getValue();
        String sort = cmbSortBy.getValue();

        try {
            List<Product> products = new ArrayList<>();

            if (category == null || category.equals("All")) {
                // Get all products
                ResultSet rs = DBHelper.executeFunction("GetAllProducts");
                while (rs.next()) {
                    int id = rs.getInt("productid");
                    String name = rs.getString("productname");
                    double price = rs.getDouble("price");
                    int categoryId = rs.getInt("categoryid");
                    products.add(new Product(id, name, price,categoryId));
                }

                // For "All", we'll group by category later
            } else {
                // Get products for selected category
                ResultSet rs = DBHelper.executeFunction("FilterByCategory", category);
                while (rs.next()) {
                    int id = rs.getInt("productid");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    int categoryid = rs.getInt("categoryid");
                    products.add(new Product(id, name, price,categoryid));
                }
            }

            // Apply sorting
            if (sort != null) {
                switch (sort) {
                    case "Price Ascending" -> products.sort(Comparator.comparingDouble(p -> p.price));
                    case "Price Descending" -> products.sort(Comparator.comparingDouble(p -> -p.price));
                    case "Alphabetical A-Z" -> products.sort(Comparator.comparing(p -> p.name.toLowerCase()));
                    case "Alphabetical Z-A" -> products.sort(Comparator.comparing((Product p) -> p.name.toLowerCase()).reversed());
                }
            }

            // Display results
            if (category == null || category.equals("All")) {
                displayAllCategories(products); // NEW METHOD
            } else {
                displayFilteredItems(products, category);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayAllCategories(List<Product> products) {
        categoriesContainer.getChildren().clear();
        categoryItemsMap.clear();

        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");

            while (rs.next()) {
                int categoryId = rs.getInt("categoryid");
                String categoryName = rs.getString("name");

                VBox categoryBox = new VBox(5);
                categoryBox.setPadding(new Insets(0));

                Text label = new Text(categoryName);
                label.setStyle("-fx-font-size: 34px; -fx-fill: #35597A; -fx-font-weight: bold;");
                label.setTranslateX(60);

                TilePane itemsPane = new TilePane();
                itemsPane.setHgap(20);
                itemsPane.setVgap(20);
                itemsPane.setPadding(new Insets(10, 0, 10, 60));
                itemsPane.setPrefTileWidth(itemCard.getPrefWidth() * 1.5);
                itemsPane.setPrefTileHeight(itemCard.getPrefHeight() * 1.3);

                List<AnchorPane> cards = new ArrayList<>();
                for (Product p : products) {
                    // You need to get category for each product from DB if necessary
                    // For simplicity, let's assume we have categoryId in Product (add field if needed)
                    if (p.categoryId == categoryId) {
                        AnchorPane card = createItemCard(p.id, p.name, p.price);
                        itemsPane.getChildren().add(card);
                        cards.add(card);
                    }
                }

                if (!cards.isEmpty()) {
                    categoryItemsMap.put(itemsPane, cards);
                    categoryBox.getChildren().addAll(label, itemsPane);
                    categoriesContainer.getChildren().add(categoryBox);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Helper class for sorting
    private static class Product {
        int id;
        String name;
        double price;
        int categoryId;
        Product(int id, String name, double price, int categoryId) { this.id = id; this.name = name; this.price = price; this.categoryId = categoryId; }
    }


    private void displayFilteredItems(List<Product> products, String categoryName) {
        categoriesContainer.getChildren().clear();
        categoryItemsMap.clear();

        VBox categoryBox = new VBox(5);
        Text label = new Text(categoryName);
        label.setStyle("-fx-font-size: 34px; -fx-fill: #35597A; -fx-font-weight: bold;");
        label.setTranslateX(60);

        TilePane pane = new TilePane();
        pane.setHgap(20);
        pane.setVgap(20);
        pane.setPadding(new Insets(10, 0, 10, 60));
        pane.setPrefTileWidth(itemCard.getPrefWidth() * 1.5);
        pane.setPrefTileHeight(itemCard.getPrefHeight() * 1.3);

        List<AnchorPane> cards = new ArrayList<>();
        for (Product p : products) {
            AnchorPane card = createItemCard(p.id, p.name, p.price);
            pane.getChildren().add(card);
            cards.add(card);
        }

        categoryItemsMap.put(pane, cards);
        categoryBox.getChildren().addAll(label, pane);
        categoriesContainer.getChildren().add(categoryBox);
    }

    private void openItemModal(int productId) {
        try {
            // Load product details
            ResultSet rs = DBHelper.executeFunction("GetProductDetails", productId);
            if (rs.next()) {
                modalItemName.setText(rs.getString("name"));
                modalItemPrice.setText("Rs " + rs.getDouble("price"));
                modalItemDescription.setText(rs.getString("description"));
                modalItemImage.setImage(placeholderImage); // placeholder or actual image
            }

            // Clear previous warehouse list
            warehousesContainer.getChildren().clear();

            // Load warehouses for this product
            ResultSet warehouses = DBHelper.executeFunction("GetWarehousesForProduct", productId);
            while (warehouses.next()) {
                String warehouseName = warehouses.getString("warehouse_name");
                int qty = warehouses.getInt("quantity_available");

                Text warehouseText = new Text("- " + warehouseName + " (Qty: " + qty + ")");
                warehouseText.setStyle("-fx-font-size: 14px;");
                warehousesContainer.getChildren().add(warehouseText);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Show modal
        itemDetailsPanel.setUserData(productId);
        itemDetailsPanel.setVisible(true);
    }
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("StaffDashboard loaded for userId: " + userId);
        // You can use this ID to load staff-specific data if needed
    }

}
