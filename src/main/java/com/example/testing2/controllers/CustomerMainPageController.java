package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import com.example.testing2.utils.DataReceiver;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.input.MouseEvent;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSidebar();
        setupUI();
        setCurrentUserId(11); // temporary
    }

    private void setupSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/Sidebar.fxml"));
            AnchorPane sidebar = loader.load();
            sidebarController = loader.getController();
            sidebarController.setSidebarListener(this); // link interface

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

        txtSearch.textProperty().addListener((obs, oldText, newText) -> filterProducts(newText));

        btnCloseModal.setOnAction(e -> itemDetailsPanel.setVisible(false));
        btnIncrease.setOnAction(e -> { currentQuantity++; txtQuantity.setText(String.valueOf(currentQuantity)); });
        btnDecrease.setOnAction(e -> { if (currentQuantity > 1) { currentQuantity--; txtQuantity.setText(String.valueOf(currentQuantity)); } });
        btnAddToCart.setOnAction(e -> addToCart());
        btnCart.setOnAction(e -> openCartPage());

        loadCategoryFilters();
    }

    private void loadCategoryFilters() {
        cmbFilterCategory.getItems().clear();
        cmbFilterCategory.getItems().add("All");
        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");
            while (rs.next()) cmbFilterCategory.getItems().add(rs.getString("name"));
        } catch (SQLException ex) { ex.printStackTrace(); }

        cmbSortBy.getItems().clear();
        cmbSortBy.getItems().addAll("Price Ascending", "Price Descending", "Alphabetical A-Z", "Alphabetical Z-A");

        cmbFilterCategory.setOnAction(e -> applyFilterAndSort());
        cmbSortBy.setOnAction(e -> applyFilterAndSort());
    }

    @Override
    public void onPageSelected(String pageName) {
        try {
            // Remove any existing page overlays
            mainContent.getChildren().removeIf(node -> node.getUserData() != null && node.getUserData().equals("overlay"));

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
            }

            if (page != null) {
                page.setUserData("overlay"); // mark it so we can remove it later
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

    private void resetSidebarButtons() {
        sidebarController.getItemsButton().setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white;");
        sidebarController.getOrdersButton().setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white;");
        sidebarController.getProfileButton().setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white;");
    }

    private void showItemsPage() {
        resetSidebarButtons();
        sidebarController.getItemsButton().setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white;");
        // Already on items page
    }

    private void showOrdersPage() {
        resetSidebarButtons();
        sidebarController.getOrdersButton().setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white;");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomerOrdersPage.fxml"));
            AnchorPane ordersPage = loader.load();
            CustomerOrdersPageController controller = loader.getController();
            controller.setCurrentUserId(currentUserId);

            mainContent.getChildren().setAll(ordersPage);
            AnchorPane.setTopAnchor(ordersPage, 0.0);
            AnchorPane.setBottomAnchor(ordersPage, 0.0);
            AnchorPane.setLeftAnchor(ordersPage, 0.0);
            AnchorPane.setRightAnchor(ordersPage, 0.0);

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void showProfilePage() {
        resetSidebarButtons();
        sidebarController.getProfileButton().setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white;");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomerProfilePage.fxml"));
            AnchorPane profilePage = loader.load();
            CustomerProfilePageController controller = loader.getController();
            controller.setCurrentUserId(currentUserId);

            mainContent.getChildren().setAll(profilePage);
            AnchorPane.setTopAnchor(profilePage, 0.0);
            AnchorPane.setBottomAnchor(profilePage, 0.0);
            AnchorPane.setLeftAnchor(profilePage, 0.0);
            AnchorPane.setRightAnchor(profilePage, 0.0);

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadCustomerData() { loadCategoriesWithProducts(); }

    private void loadCategoriesWithProducts() {
        categoriesContainer.getChildren().clear();
        categoryProductsMap.clear();

        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");
            while (rs.next()) {
                int categoryId = rs.getInt("categoryid");
                String categoryName = rs.getString("name");

                VBox categoryBox = new VBox(8);
                categoryBox.setPadding(new Insets(0,0,0,0));

                Label categoryLabel = new Label(categoryName);
                categoryLabel.setPadding(new Insets(0,0,0,60));
                categoryLabel.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #532386; -fx-font-family: 'Arial Black';");
                categoryBox.getChildren().add(categoryLabel);

                TilePane productPane = new TilePane();
                productPane.setHgap(20); productPane.setVgap(20); productPane.setPadding(new Insets(10, 0, 10, 60));
                productPane.setPrefColumns(4);
                productPane.setPrefTileWidth(itemCard.getPrefWidth() * 1.5);
                productPane.setPrefTileHeight(itemCard.getPrefHeight() * 1.3);

                List<AnchorPane> productCards = new ArrayList<>();
                ResultSet products = DBHelper.executeFunction("GetProductsByCategory", categoryId);
                while (products.next()) {
                    int productId = products.getInt("productid");
                    String productName = products.getString("name");
                    double price = products.getDouble("price");
                    AnchorPane card = createItemCard(productId, productName, price);
                    productPane.getChildren().add(card);
                    productCards.add(card);
                }

                categoryProductsMap.put(productPane, productCards);
                categoryBox.getChildren().add(productPane);
                categoriesContainer.getChildren().add(categoryBox);
            }
        } catch (SQLException e) { e.printStackTrace(); }
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

    private void filterProducts(String query) {
        query = query.toLowerCase(Locale.ROOT);
        for (Node node : categoriesContainer.getChildren()) {
            if (!(node instanceof VBox categoryBox)) continue;
            TilePane productPane = (TilePane) categoryBox.getChildren().get(1);
            List<AnchorPane> cards = categoryProductsMap.get(productPane);
            List<AnchorPane> filteredCards = new ArrayList<>();
            for (AnchorPane card : cards) {
                Text nameText = (Text) card.getChildren().stream().filter(n -> n instanceof Text).findFirst().orElse(null);
                if (nameText != null && nameText.getText().toLowerCase(Locale.ROOT).contains(query)) filteredCards.add(card);
            }
            productPane.getChildren().setAll(filteredCards);
            categoryBox.setVisible(!filteredCards.isEmpty());
            categoryBox.setManaged(!filteredCards.isEmpty());
        }
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

    private void openItemModal(int productId) {
        try {
            ResultSet rs = DBHelper.executeFunction("GetProductDetails", productId);
            if (rs.next()) {
                modalItemName.setText(rs.getString("name"));
                modalItemPrice.setText("Rs " + rs.getDouble("price"));
                modalItemDescription.setText(rs.getString("description"));
                modalItemImage.setImage(placeholderImage);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        itemDetailsPanel.setUserData(productId);
        currentQuantity = 1;
        txtQuantity.setText("1");
        itemDetailsPanel.setVisible(true);
    }

    @FXML private void handleCardHoverIn(MouseEvent event) { AnchorPane card = (AnchorPane) event.getSource(); card.setScaleX(1.05); card.setScaleY(1.05); }
    @FXML private void handleCardHoverOut(MouseEvent event) { AnchorPane card = (AnchorPane) event.getSource(); card.setScaleX(1.0); card.setScaleY(1.0); }

    public void clearCart() { cart.clear(); updateCartBadge(); }

    private void openCartPage() {
        try {
            if (cart.isEmpty()) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CartPage.fxml"));
            Parent root = loader.load();
            CartPageController controller = loader.getController();
            controller.setParentController(this);
            controller.setCurrentUserId(currentUserId);

            Map<Integer, String> productNames = new HashMap<>();
            Map<Integer, Double> productPrices = new HashMap<>();

            for (Map.Entry<TilePane, List<AnchorPane>> entry : categoryProductsMap.entrySet()) {
                for (AnchorPane card : entry.getValue()) {
                    int pid = (int) card.getUserData();
                    Text nameText = null; Text priceText = null;
                    for (Node n : card.getChildren()) {
                        if (n instanceof Text t) { if (nameText == null) nameText = t; else if (priceText == null) priceText = t; }
                    }
                    if (nameText != null) productNames.put(pid, nameText.getText());
                    if (priceText != null) productPrices.put(pid, Double.parseDouble(priceText.getText().replace("Rs ", "").trim()));
                }
            }

            controller.setCartData(cart, productNames, productPrices);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1600, 900));
            stage.setTitle("My Cart");
            stage.setMaximized(true);
            stage.show();

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void applyFilterAndSort() {
        String category = cmbFilterCategory.getValue();
        String sort = cmbSortBy.getValue();
        try {
            ResultSet rs;
            if (category == null || category.equals("All")) { loadCategoriesWithProducts(); return; }
            rs = DBHelper.executeFunction("FilterByCategory", category);
            if (sort != null) {
                switch (sort) {
                    case "Price Ascending" -> rs = DBHelper.executeFunction("SortByPriceAsc", category);
                    case "Price Descending" -> rs = DBHelper.executeFunction("SortByPriceDesc", category);
                    case "Alphabetical A-Z" -> rs = DBHelper.executeFunction("SortByNameAsc", category);
                    case "Alphabetical Z-A" -> rs = DBHelper.executeFunction("SortByNameDesc", category);
                }
            }
            displayProducts(rs, category);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void displayProducts(ResultSet rs, String categoryName) throws SQLException {
        categoriesContainer.getChildren().clear();
        categoryProductsMap.clear();

        // Category VBox
        VBox categoryBox = new VBox(5);
        categoryBox.setPadding(new Insets(0, 0, 0, 0));

        // Category label
        Label categoryLabel = new Label(categoryName);
        categoryLabel.setPadding(new Insets(0, 0, 0, 60));
        categoryLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #826478;");
        categoryBox.getChildren().add(categoryLabel);

        // Product TilePane
        TilePane productPane = new TilePane();
        productPane.setHgap(20);
        productPane.setVgap(20);
        productPane.setPadding(new Insets(10, 0, 10, 60));
        productPane.setPrefColumns(4);
        productPane.setPrefTileWidth(210);
        productPane.setPrefTileHeight(224);

        List<AnchorPane> productCards = new ArrayList<>();
        while (rs.next()) {
            int productId = rs.getInt("productid");
            String productName = rs.getString("name");
            double price = rs.getDouble("price");
            AnchorPane card = createItemCard(productId, productName, price);
            productPane.getChildren().add(card);
            productCards.add(card);
        }

        categoryProductsMap.put(productPane, productCards);
        categoryBox.getChildren().add(productPane);

        // Add category to the scrollable container
        categoriesContainer.getChildren().add(categoryBox);
    }
}
