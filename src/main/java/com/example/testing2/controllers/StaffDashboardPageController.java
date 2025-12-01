package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.control.Button;


import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class StaffDashboardPageController implements Initializable {

    @FXML private VBox categoriesContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFilterCategory;
    @FXML private ComboBox<String> cmbSortBy;

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

    @FXML private Button btnCloseModal;

    private final Image placeholderImage =
            new Image(getClass().getResource("/com/example/testing2/images/placeholder.jpg").toExternalForm());

    private final Map<TilePane, List<AnchorPane>> categoryItemsMap = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

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
                label.setStyle("-fx-font-size: 22px; -fx-fill: #35597A; -fx-font-weight: bold;");
                label.setTranslateX(60);

                TilePane itemsPane = new TilePane();
                itemsPane.setHgap(20);
                itemsPane.setVgap(20);
                itemsPane.setPadding(new Insets(10, 0, 10, 60));
                itemsPane.setPrefTileWidth(210);
                itemsPane.setPrefTileHeight(224);

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
        clone.setPrefWidth(itemCard.getPrefWidth());
        clone.setPrefHeight(itemCard.getPrefHeight());
        clone.setStyle(itemCard.getStyle());

        ImageView img = new ImageView(placeholderImage);
        img.setFitWidth(205);
        img.setFitHeight(112);

        Text itemName = new Text(name);
        itemName.setStyle("-fx-font-size: 15px; -fx-fill: #35597A;");
        itemName.setLayoutX(8);
        itemName.setLayoutY(160);

        Text itemPrice = new Text("Rs " + price);
        itemPrice.setStyle("-fx-font-size: 15px; -fx-fill: #35597A;");
        itemPrice.setLayoutX(140);
        itemPrice.setLayoutY(160);

        clone.getChildren().addAll(img, itemName, itemPrice);

        clone.setUserData(id);
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
            ResultSet rs;

            if (category == null || category.equals("All")) {
                loadCategoriesWithItems();
                return;
            }

            rs = DBHelper.executeFunction("FilterByCategory", category);

            if (sort != null) {
                switch (sort) {
                    case "Price Ascending" -> rs = DBHelper.executeFunction("SortByPriceAsc", category);
                    case "Price Descending" -> rs = DBHelper.executeFunction("SortByPriceDesc", category);
                    case "Alphabetical A-Z" -> rs = DBHelper.executeFunction("SortByNameAsc", category);
                    case "Alphabetical Z-A" -> rs = DBHelper.executeFunction("SortByNameDesc", category);
                }
            }

            displayFilteredItems(rs, category);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayFilteredItems(ResultSet rs, String categoryName) throws SQLException {
        categoriesContainer.getChildren().clear();
        categoryItemsMap.clear();

        VBox categoryBox = new VBox(5);

        Text label = new Text(categoryName);
        label.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-fill: #35597A;");
        label.setTranslateX(60);

        TilePane pane = new TilePane();
        pane.setHgap(20);
        pane.setVgap(20);
        pane.setPadding(new Insets(10, 0, 10, 60));
        pane.setPrefTileWidth(210);
        pane.setPrefTileHeight(224);

        List<AnchorPane> cards = new ArrayList<>();

        while (rs.next()) {
            int id = rs.getInt("productid");
            String name = rs.getString("name");
            double price = rs.getDouble("price");

            AnchorPane card = createItemCard(id, name, price);
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

}
