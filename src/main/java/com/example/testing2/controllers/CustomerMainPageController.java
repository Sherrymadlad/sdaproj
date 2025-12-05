package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class CustomerMainPageController implements Initializable, SidebarListener {

    @FXML private AnchorPane itemCard;
    @FXML private VBox categoriesContainer;
    @FXML private TextField txtSearch;
    @FXML private Button btnAddToCart;
    @FXML private Button btnCart;
    @FXML private Label cartBadge;
    @FXML private AnchorPane sidebarContainer;
    @FXML private StackPane itemDetailsPanel;
    @FXML private ImageView modalItemImage;
    @FXML private Text modalItemName;
    @FXML private Text modalItemPrice;
    @FXML private Text modalItemDescription;
    @FXML private Text txtQuantity;
    @FXML private Button btnIncrease;
    @FXML private Button btnDecrease;
    @FXML private Button btnCloseModal;
    @FXML private ComboBox<String> cmbFilterCategory;
    @FXML private ComboBox<String> cmbSortBy;
    @FXML private AnchorPane mainContent;
    @FXML private AnchorPane header;
    @FXML private ScrollPane scrollPane;

    private SidebarController sidebarController;

    private final Image placeholderImage = new Image(getClass().getResource("/com/example/testing2/images/placeholder.png").toExternalForm());
    private final Map<TilePane, List<AnchorPane>> categoryProductsMap = new HashMap<>();
    private final Map<Integer, Integer> cart = new HashMap<>();
    private int currentQuantity = 1;
    private int currentUserId;
    private int currentMaxStock = 0;

    // Cached data
    private final Map<Integer, String> categoryNames = new HashMap<>();
    private final List<Product> allProducts = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSidebar();
        setupUI();
    }

    private void setupSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/Sidebar.fxml"));
            AnchorPane sidebar = loader.load();
            sidebarController = loader.getController();
            sidebarController.setSidebarListener(this);

            sidebarContainer.getChildren().add(sidebar);
            AnchorPane.setTopAnchor(sidebar, 0.0);
            AnchorPane.setBottomAnchor(sidebar, 0.0);
            AnchorPane.setLeftAnchor(sidebar, 0.0);
            AnchorPane.setRightAnchor(sidebar, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        itemCard.setVisible(false);
        categoriesContainer.setPadding(new Insets(70, 0, 0, 0));

        txtSearch.textProperty().addListener((obs, oldText, newText) -> applyFilterAndSort());

        btnCloseModal.setOnAction(e -> itemDetailsPanel.setVisible(false));
        btnIncrease.setOnAction(e -> {
            if (currentQuantity < currentMaxStock) { // limit to max stock
                currentQuantity++;
                txtQuantity.setText(String.valueOf(currentQuantity));
            }
        });

        btnDecrease.setOnAction(e -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                txtQuantity.setText(String.valueOf(currentQuantity));
            }
        });

        btnAddToCart.setOnAction(e -> addToCart());
        btnCart.setOnAction(e -> openCartPage());

        loadCategoryFilters();
    }

    private void loadCategoryFilters() {
        cmbFilterCategory.getItems().clear();
        cmbFilterCategory.getItems().add("All");
        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");
            while (rs.next()) {
                int catId = rs.getInt("categoryid");
                String name = rs.getString("name");
                cmbFilterCategory.getItems().add(name);
                categoryNames.put(catId, name);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        cmbSortBy.getItems().clear();
        cmbSortBy.getItems().addAll("Price Ascending", "Price Descending", "Alphabetical A-Z", "Alphabetical Z-A");

        cmbFilterCategory.setOnAction(e -> applyFilterAndSort());
        cmbSortBy.setOnAction(e -> applyFilterAndSort());
    }

    @Override
    public void onPageSelected(String pageName) {
        try {
            mainContent.getChildren().removeIf(node -> "overlay".equals(node.getUserData()));

            if (pageName.equals("Items")) {
                header.setVisible(true);
                scrollPane.setVisible(true);
                return;
            }

            FXMLLoader loader = null;
            AnchorPane page = null;

            switch (pageName) {
                case "Orders" -> {
                    loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomerOrdersPage.fxml"));
                    page = loader.load();
                    CustomerOrdersPageController controller = loader.getController();
                    controller.setCurrentUserId(currentUserId);
                }
                case "Profile" -> {
                    loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomerProfilePage.fxml"));
                    page = loader.load();
                    CustomerProfilePageController controller = loader.getController();
                    controller.setCurrentUserId(currentUserId);
                }
                case "Logout" -> {
                    // Handle logout here
                    loader = new FXMLLoader(getClass().getResource("/com/example/testing2/LoginPage.fxml"));
                    AnchorPane loginPage = loader.load();
                    mainContent.getScene().setRoot(loginPage);
                }
                default -> System.out.println("Unknown page: " + pageName);
            }

            if (page != null) {
                page.setUserData("overlay");
                header.setVisible(false);
                scrollPane.setVisible(false);

                mainContent.getChildren().add(page);
                AnchorPane.setTopAnchor(page, 0.0);
                AnchorPane.setBottomAnchor(page, 0.0);
                AnchorPane.setLeftAnchor(page, 0.0);
                AnchorPane.setRightAnchor(page, 0.0);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        loadCategoriesWithProducts();
    }

    private void loadCategoriesWithProducts() {
        categoriesContainer.getChildren().clear();
        categoryProductsMap.clear();
        allProducts.clear();

        try {
            // Single query to fetch products and categories
            ResultSet rs = DBHelper.executeQuery("""
                SELECT\s
                            p.productid,
                            p.name AS product_name,
                            p.price,
                            c.categoryid,
                            c.name AS category_name,
                            COALESCE(SUM(s.quantityavailable), 0) AS max_stock_available
                        FROM product p
                        JOIN category c ON p.categoryid = c.categoryid
                        LEFT JOIN stock s ON p.productid = s.productid
                        WHERE p.isactive = TRUE
                        GROUP BY p.productid, p.name, p.price, c.categoryid, c.name
                        ORDER BY c.name, p.name;
            """);

            while (rs.next()) {
                int productId = rs.getInt("productid");
                String productName = rs.getString("product_name");
                double price = rs.getDouble("price");
                int categoryId = rs.getInt("categoryid");
                int maxStock = rs.getInt("max_stock_available");
                allProducts.add(new Product(productId, productName, price, categoryId,maxStock));
            }

            applyFilterAndSort();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void applyFilterAndSort() {
        String category = cmbFilterCategory.getValue();
        String sort = cmbSortBy.getValue();
        String searchQuery = txtSearch.getText() != null ? txtSearch.getText().toLowerCase() : "";

        List<Product> filtered = new ArrayList<>(allProducts);

        // Filter by category
        if (category != null && !category.equals("All")) {
            filtered.removeIf(p -> !category.equals(categoryNames.get(p.categoryId)));
        }

        // Filter by search query
        if (!searchQuery.isEmpty()) {
            filtered.removeIf(p -> !p.name.toLowerCase().contains(searchQuery));
        }

        // Sort
        if (sort != null) {
            switch (sort) {
                case "Price Ascending" -> filtered.sort(Comparator.comparingDouble(p -> p.price));
                case "Price Descending" -> filtered.sort(Comparator.comparingDouble(p -> -p.price));
                case "Alphabetical A-Z" -> filtered.sort(Comparator.comparing(p -> p.name.toLowerCase()));
                case "Alphabetical Z-A" -> filtered.sort(Comparator.comparing((Product p) -> p.name.toLowerCase()).reversed());
            }
        }

        displayFilteredProducts(filtered);
    }

    private void displayFilteredProducts(List<Product> products) {
        categoriesContainer.getChildren().clear();
        categoryProductsMap.clear();

        // Group products by category
        Map<Integer, List<Product>> grouped = new HashMap<>();
        for (Product p : products) grouped.computeIfAbsent(p.categoryId, k -> new ArrayList<>()).add(p);

        for (Map.Entry<Integer, List<Product>> entry : grouped.entrySet()) {
            int catId = entry.getKey();
            List<Product> catProducts = entry.getValue();

            VBox categoryBox = new VBox(8);
            Label categoryLabel = new Label(categoryNames.getOrDefault(catId, "Unknown"));
            categoryLabel.setPadding(new Insets(0,0,0,60));
            categoryLabel.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #532386;");
            categoryBox.getChildren().add(categoryLabel);

            TilePane productPane = new TilePane();
            productPane.setHgap(20);
            productPane.setVgap(20);
            productPane.setPadding(new Insets(10,0,10,60));
            productPane.setPrefColumns(4);
            productPane.setPrefTileWidth(itemCard.getPrefWidth() * 1.5);
            productPane.setPrefTileHeight(itemCard.getPrefHeight() * 1.3);

            List<AnchorPane> productCards = new ArrayList<>();
            for (Product p : catProducts) {
                AnchorPane card = createItemCard(p.id, p.name, p.price);
                productPane.getChildren().add(card);
                productCards.add(card);
            }

            categoryProductsMap.put(productPane, productCards);
            categoryBox.getChildren().add(productPane);
            categoriesContainer.getChildren().add(categoryBox);
        }
    }

    private AnchorPane createItemCard(int productId, String name, double price) {
        AnchorPane clone = new AnchorPane();
        clone.setPrefWidth(itemCard.getPrefWidth() * 1.5);
        clone.setPrefHeight(itemCard.getPrefHeight() * 1.5);
        clone.setStyle("-fx-background-color: #f5f0fa; -fx-border-color: #c8b8d1; -fx-border-radius: 12; -fx-background-radius: 12; -fx-effect: dropshadow(two-pass-box, rgba(0,0,0,0.15), 6, 0, 0, 3);");

        ImageView img = new ImageView(placeholderImage);
        img.setFitWidth(clone.getPrefWidth() - 10);
        img.setFitHeight(clone.getPrefHeight() * 0.6);
        img.setLayoutX(5); img.setLayoutY(10);

        Text itemName = new Text(name);
        itemName.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-fill: #4a2375; -fx-font-family: 'Verdana';");
        itemName.setWrappingWidth(clone.getPrefWidth() - 20);
        itemName.setLayoutX(10); itemName.setLayoutY(img.getFitHeight() + 40);

        Text itemPrice = new Text("Rs " + price);
        itemPrice.setStyle("-fx-font-size: 20px; -fx-font-weight: semi-bold; -fx-fill: #8b6fa1; -fx-font-family: 'Tahoma';");
        itemPrice.setLayoutX(10); itemPrice.setLayoutY(img.getFitHeight() + 75);

        clone.getChildren().addAll(img, itemName, itemPrice);
        clone.setUserData(productId);

        clone.setOnMouseClicked(e -> openItemModal(productId));
        clone.setOnMouseEntered(e -> { clone.setScaleX(1.05); clone.setScaleY(1.05); });
        clone.setOnMouseExited(e -> { clone.setScaleX(1.0); clone.setScaleY(1.0); });

        return clone;
    }

    private void openItemModal(int productId) {
        try {
            ResultSet rs = DBHelper.executeFunction("GetProductDetails", productId);
            if (rs.next()) {
                modalItemName.setText(rs.getString("name"));
                modalItemPrice.setText("Rs " + rs.getDouble("price"));
                modalItemDescription.setText(rs.getString("description"));
                modalItemImage.setImage(placeholderImage);

                // Get max stock from cached allProducts list
                Optional<Product> productOpt = allProducts.stream()
                        .filter(p -> p.id == productId)
                        .findFirst();
                currentMaxStock = productOpt.map(p -> p.maxStock).orElse(0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        itemDetailsPanel.setUserData(productId);
        currentQuantity = 1;
        txtQuantity.setText("1");
        itemDetailsPanel.setVisible(true);
    }

    private void addToCart() {
        try {
            int productId = (int) itemDetailsPanel.getUserData();
            cart.put(productId, cart.getOrDefault(productId, 0) + currentQuantity);
            animateCartButton();
            updateCartBadge();
            currentQuantity = 1;
            txtQuantity.setText("1");
            itemDetailsPanel.setVisible(false);
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    public void updateCartBadge() {
        int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
        cartBadge.setText(String.valueOf(totalItems));
        cartBadge.setVisible(totalItems > 0);
    }

    private void animateCartButton() {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), btnCart);
        st.setFromX(1.0); st.setFromY(1.0); st.setToX(1.3); st.setToY(1.3);
        st.setCycleCount(2); st.setAutoReverse(true); st.play();
    }

    private static class Product {
        int id;
        String name;
        double price;
        int categoryId;
        int maxStock;

        Product(int id, String name, double price, int categoryId, int maxStock) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.categoryId = categoryId;
            this.maxStock = maxStock;
        }
    }
    private void openCartPage() {
        try {
            if (cart.isEmpty()) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CartPage.fxml"));
            StackPane cartPage = loader.load();
            CartPageController controller = loader.getController();

            // Pass data to the cart controller
            controller.setParentController(this);
            controller.setCurrentUserId(currentUserId);

            Map<Integer, String> productNames = new HashMap<>();
            Map<Integer, Double> productPrices = new HashMap<>();

            for (Map.Entry<TilePane, List<AnchorPane>> entry : categoryProductsMap.entrySet()) {
                for (AnchorPane card : entry.getValue()) {
                    int pid = (int) card.getUserData();
                    Text nameText = null, priceText = null;
                    for (Node n : card.getChildren()) {
                        if (n instanceof Text t) {
                            if (nameText == null) nameText = t;
                            else if (priceText == null) priceText = t;
                        }
                    }
                    if (nameText != null) productNames.put(pid, nameText.getText());
                    if (priceText != null) productPrices.put(pid, Double.parseDouble(priceText.getText().replace("Rs ", "").trim()));
                }
            }
            Map<Integer, Integer> productMaxStock = new HashMap<>();
            for (Product p : allProducts) {
                if (cart.containsKey(p.id)) { // only add products in cart
                    productMaxStock.put(p.id, p.maxStock);
                }
            }
            controller.setCartData(cart, productNames, productPrices, productMaxStock);

            // Add as overlay to main content
            cartPage.setUserData("overlay"); // mark as overlay
            mainContent.getChildren().removeIf(node -> "overlay".equals(node.getUserData()));
            mainContent.getChildren().add(cartPage);

            // Anchor the StackPane to fill mainContent
            AnchorPane.setTopAnchor(cartPage, 0.0);
            AnchorPane.setBottomAnchor(cartPage, 0.0);
            AnchorPane.setLeftAnchor(cartPage, 0.0);
            AnchorPane.setRightAnchor(cartPage, 0.0);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void clearCart() { cart.clear(); updateCartBadge(); }

    @FXML private void handleCardHoverIn(MouseEvent event) { AnchorPane card = (AnchorPane) event.getSource(); card.setScaleX(1.05); card.setScaleY(1.05); }
    @FXML private void handleCardHoverOut(MouseEvent event) { AnchorPane card = (AnchorPane) event.getSource(); card.setScaleX(1.0); card.setScaleY(1.0); }
}
