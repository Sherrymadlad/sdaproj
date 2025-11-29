package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;



public class CustomerMainPageController implements Initializable {

    @FXML
    private AnchorPane itemCard;

    @FXML
    private VBox categoriesContainer;

    @FXML
    private TextField txtSearch;

    @FXML
    private Button btnAddToCart;

    @FXML
    private Button btnCart; // also needed for the animation
    @FXML
    private Label cartBadge;

    private final Image placeholderImage = new Image(getClass().getResource("/com/example/testing2/images/placeholder.jpg").toExternalForm());
    private final Map<TilePane, List<AnchorPane>> categoryProductsMap = new HashMap<>();
    @FXML private StackPane itemDetailsPanel;
    @FXML private ImageView modalItemImage;
    @FXML private Text modalItemName;
    @FXML private Text modalItemPrice;
    @FXML private Text modalItemDescription;
    @FXML private Text txtQuantity;
    @FXML private Button btnIncrease;
    @FXML private Button btnDecrease;
    @FXML private Button btnCloseModal;
    private final Map<Integer, Integer> cart = new HashMap<>();

    private int currentQuantity = 1;
    private int currentUserId;  // <--- add this at the top with other fields
    @FXML private Button btnItems;
    @FXML private Button btnOrders;
    @FXML private AnchorPane mainContent;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        itemCard.setVisible(false);
        categoriesContainer.setPadding(new Insets(70, 0, 0, 0));
        this.currentUserId = 7;
        loadCategoriesWithProducts();

        // Client-side search
        txtSearch.textProperty().addListener((obs, oldText, newText) -> filterProducts(newText));
        btnCloseModal.setOnAction(e -> itemDetailsPanel.setVisible(false));

        btnIncrease.setOnAction(e -> {
            currentQuantity++;
            txtQuantity.setText(String.valueOf(currentQuantity));
        });

        btnDecrease.setOnAction(e -> {
            if (currentQuantity > 1) {
                currentQuantity--;
                txtQuantity.setText(String.valueOf(currentQuantity));
            }
        });
        btnAddToCart.setOnAction(e -> {
            addToCart();
        });

        btnCart.setOnAction(e -> openCartPage());
        btnItems.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white;");
        btnItems.setOnAction(e -> showItemsPage());

    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("Logged-in user ID = " + userId);
    }
    private void resetSidebarButtons() {
        btnItems.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white;");
        btnOrders.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white;");
    }

    private void showItemsPage() {
        resetSidebarButtons();
        btnItems.setStyle("-fx-background-color: #6d4c7d; -fx-text-fill: white;");
        // Already on items page — no need to reload anything
    }



    public void clearCart() {
        cart.clear();
        updateCartBadge();
    }

    private void openCartPage() {
        try {
            if (cart == null || cart.isEmpty()) {
                System.out.println("Cart is empty.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CartPage.fxml"));
            Parent root = loader.load();
            CartPageController controller = loader.getController();

            // Pass parent controller
            controller.setParentController(this);

            // Set current user ID first
            controller.setCurrentUserId(this.currentUserId);

            // Prepare product names and prices from current products
            Map<Integer, String> productNames = new HashMap<>();
            Map<Integer, Double> productPrices = new HashMap<>();

            for (Map.Entry<TilePane, List<AnchorPane>> entry : categoryProductsMap.entrySet()) {
                for (AnchorPane card : entry.getValue()) {
                    int pid = (int) card.getUserData();

                    Text nameText = null;
                    Text priceText = null;

                    for (Node n : card.getChildren()) {
                        if (n instanceof Text t) {
                            if (nameText == null) nameText = t;
                            else if (priceText == null) priceText = t;
                        }
                    }

                    if (nameText != null) productNames.put(pid, nameText.getText());

                    if (priceText != null) {
                        String txt = priceText.getText().replace("Rs ", "").trim();
                        productPrices.put(pid, Double.parseDouble(txt));
                    }
                }
            }

            // Pass cart data
            controller.setCartData(cart, productNames, productPrices);

            Stage stage = new Stage();
            stage.setScene(new Scene(root, 1600, 900));
            stage.setTitle("My Cart");
            stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }




    private void loadCategoriesWithProducts() {
        categoriesContainer.getChildren().clear();
        categoryProductsMap.clear();

        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");

            while (rs.next()) {
                int categoryId = rs.getInt("categoryid");
                String categoryName = rs.getString("name");

                // --- Outer VBox for category ---
                VBox categoryBox = new VBox(5);
                categoryBox.setPadding(new Insets(0,0,0,0));

                // Category label
                Label categoryLabel = new Label(categoryName);
                categoryLabel.setPadding(new Insets(0, 0, 0, 60));
                categoryLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #826478;");
                categoryBox.getChildren().add(categoryLabel);

                // TilePane for products
                TilePane productPane = new TilePane();
                productPane.setHgap(20);
                productPane.setVgap(20);
                productPane.setPadding(new Insets(10, 0, 10, 60));
                productPane.setPrefColumns(4);
                productPane.setPrefTileWidth(210);
                productPane.setPrefTileHeight(224);

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

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void filterProducts(String query) {
        query = query.toLowerCase(Locale.ROOT);

        for (Node node : categoriesContainer.getChildren()) {
            if (!(node instanceof VBox categoryBox)) continue;

            TilePane productPane = (TilePane) categoryBox.getChildren().get(1);
            List<AnchorPane> cards = categoryProductsMap.get(productPane);

            // Filter cards that match the query
            List<AnchorPane> filteredCards = new ArrayList<>();
            for (AnchorPane card : cards) {
                Text nameText = (Text) card.getChildren().stream()
                        .filter(n -> n instanceof Text)
                        .findFirst().orElse(null);

                if (nameText != null && nameText.getText().toLowerCase(Locale.ROOT).contains(query)) {
                    filteredCards.add(card);
                }
            }

            // Update the TilePane to only show matching cards
            productPane.getChildren().setAll(filteredCards);

            // Show/hide the whole category depending on if any cards match
            boolean anyVisible = !filteredCards.isEmpty();
            categoryBox.setVisible(anyVisible);
            categoryBox.setManaged(anyVisible);
        }
    }

    private void animateCartButton() {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), btnCart);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.3);
        st.setToY(1.3);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    }
    private void addToCart() {
        try {
            int productId = (int) itemDetailsPanel.getUserData();

            // Add quantity to cart
            cart.put(productId, cart.getOrDefault(productId, 0) + currentQuantity);

            System.out.println("Cart contents: " + cart);

            // Animate cart button
            animateCartButton();

            // Update badge
            updateCartBadge();

            // Reset quantity and close modal
            currentQuantity = 1;
            txtQuantity.setText("1");
            itemDetailsPanel.setVisible(false);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateCartBadge() {
        int totalItems = cart.values().stream().mapToInt(Integer::intValue).sum();
        cartBadge.setText(String.valueOf(totalItems));
        cartBadge.setVisible(totalItems > 0);
    }



    private AnchorPane createItemCard(int productId, String name, double price) {
        AnchorPane clone = new AnchorPane();
        clone.setPrefWidth(itemCard.getPrefWidth());
        clone.setPrefHeight(itemCard.getPrefHeight());
        clone.setStyle(itemCard.getStyle());

        ImageView img = new ImageView(placeholderImage);
        img.setFitWidth(205);
        img.setFitHeight(112);
        img.setLayoutX(0);
        img.setLayoutY(0);

        Text itemName = new Text(name);
        itemName.setStyle("-fx-font-size: 15px; -fx-fill: #826478;");
        itemName.setLayoutX(8);
        itemName.setLayoutY(160);

        Text itemPrice = new Text("Rs " + price);
        itemPrice.setStyle("-fx-font-size: 15px; -fx-fill: #826478;");
        itemPrice.setLayoutX(140);
        itemPrice.setLayoutY(160);

        clone.getChildren().addAll(img, itemName, itemPrice);

        // Store productId in userData
        clone.setUserData(productId);

        // CLICK LISTENER TO OPEN MODAL
        clone.setOnMouseClicked(e -> openItemModal(productId));


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
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        itemDetailsPanel.setUserData(productId); // <--- important
        currentQuantity = 1;
        txtQuantity.setText("1");
        itemDetailsPanel.setVisible(true);
    }


}
