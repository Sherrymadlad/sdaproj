package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ManagerMainPageController implements Initializable {

    @FXML
    private AnchorPane itemCard;
    @FXML
    private VBox categoriesContainer;
    @FXML
    private TextField txtSearch;
    @FXML
    private AnchorPane sidebarContainer;
    @FXML
    private AnchorPane mainContent;
    @FXML
    private AnchorPane header;
    private Integer editingProductId = null; // null means adding new product
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private ComboBox<String> cmbFilterCategory;
    @FXML
    private ComboBox<String> cmbSortBy;
    @FXML
    private StackPane productModal;
    @FXML
    private AnchorPane modalContent;
    @FXML
    private ImageView productModalImage;
    @FXML
    private TextField txtModalProductName;
    @FXML
    private ComboBox<String> cmbModalCategory;
    @FXML
    private TextField txtModalPrice;
    @FXML
    private TextField txtModalStock;
    @FXML
    private TextField txtModalLowStock;
    @FXML
    private TextField txtModalSupplier;
    @FXML
    private TextArea txtModalDescription;
    @FXML
    private Button btnCloseProductModal;
    @FXML
    private Button btnModalCancel;
    @FXML
    private Button btnModalSaveProduct;
    @FXML
    private Button btnAddProduct;


    private final Map<TilePane, List<AnchorPane>> categoryProductsMap = new HashMap<>();



    private final Map<TilePane, List<AnchorPane>> categoryItemsMap = new HashMap<>();
    private final Image placeholderImage = new Image(getClass().getResource("/com/example/testing2/images/placeholder.png").toExternalForm());

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSidebar();
        setupUI();
        loadCategoriesWithItems();
        btnCloseProductModal.setOnAction(e -> productModal.setVisible(false));
        btnModalCancel.setOnAction(e -> productModal.setVisible(false));
        btnModalSaveProduct.setOnAction(e -> saveProduct());
        btnAddProduct.setOnAction(e -> openAddProductModal());


    }

    private void loadCategoriesForModal() {
        cmbModalCategory.getItems().clear();
        try (ResultSet rs = DBHelper.executeFunction("GetAllCategories")) {
            while (rs != null && rs.next()) {
                String categoryName = rs.getString("name");
                cmbModalCategory.getItems().add(categoryName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openAddProductModal() {
        // Clear all fields for adding new product
        txtModalProductName.clear();
        txtModalPrice.clear();
        txtModalStock.clear();
        txtModalLowStock.clear();
        txtModalSupplier.clear();
        txtModalDescription.clear();
        cmbModalCategory.getSelectionModel().clearSelection();
        productModalImage.setImage(placeholderImage);

        // Load categories into ComboBox
        loadCategoriesForModal();

        // Show modal
        productModal.setVisible(true);
    }

    private void saveProduct() {
        String name = txtModalProductName.getText().trim();
        String categoryName = cmbModalCategory.getSelectionModel().getSelectedItem();
        String priceStr = txtModalPrice.getText().trim();
        String supplierStr = txtModalSupplier.getText().trim();
        String description = txtModalDescription.getText().trim();

        if (name.isEmpty() || categoryName == null || categoryName.isEmpty() ||
                priceStr.isEmpty() || supplierStr.isEmpty()) {
            showAlert("Validation Error", "Please fill all required fields.");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            int supplierId = Integer.parseInt(supplierStr);

            // Get categoryId
            int categoryId = 0;
            try (ResultSet rs = DBHelper.executeFunction("getcategorybyname", categoryName)) {
                if (rs != null && rs.next()) {
                    categoryId = rs.getInt(1);
                } else {
                    showAlert("Error", "Category not found: " + categoryName);
                    return;
                }
            }

            int editingProductId = (int) productModal.getUserData(); // set this when opening modal

            // Call EditProductDetails function
            try (ResultSet rs = DBHelper.executeFunction(
                    "EditProductDetails",
                    editingProductId,
                    null,        // sku (null = no change)
                    name,
                    categoryId,
                    price,
                    description,
                    supplierId,
                    null         // isactive (null = no change)
            )) {
                showAlert("Success", "Product updated successfully!");
            }

            productModal.setVisible(false);
            loadCategoriesWithItems();

        } catch (NumberFormatException ex) {
            showAlert("Input Error", "Invalid number format for price or supplier ID.");
            ex.printStackTrace();
        } catch (SQLException ex) {
            showAlert("Database Error", ex.getMessage());
            ex.printStackTrace();
        }
    }


    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/Sidebar.fxml"));
            AnchorPane sidebar = loader.load();
            sidebarContainer.getChildren().add(sidebar);
            AnchorPane.setTopAnchor(sidebar, 0.0);
            AnchorPane.setBottomAnchor(sidebar, 0.0);
            AnchorPane.setLeftAnchor(sidebar, 0.0);
            AnchorPane.setRightAnchor(sidebar, 0.0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        itemCard.setVisible(false);
        categoriesContainer.setPadding(new Insets(70, 0, 0, 0));
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterItems(newVal));

        cmbFilterCategory.setOnAction(e -> applyFilterAndSort());
        cmbSortBy.setOnAction(e -> applyFilterAndSort());
    }

    private void loadCategoriesWithItems() {
        categoriesContainer.getChildren().clear();
        categoryProductsMap.clear();

        try {
            // 1. Get all categories
            ResultSet rsCategories = DBHelper.executeFunction("GetAllCategories");
            while (rsCategories.next()) {
                int categoryId = rsCategories.getInt("categoryid");
                String categoryName = rsCategories.getString("name");

                // VBox for category
                VBox categoryBox = new VBox(8);
                categoryBox.setPadding(new Insets(0, 0, 0, 0));

                Label categoryLabel = new Label(categoryName);
                categoryLabel.setPadding(new Insets(0, 0, 0, 60));
                categoryLabel.setStyle("-fx-font-size: 34px; -fx-font-weight: bold; -fx-text-fill: #532386; -fx-font-family: 'Arial Black';");
                categoryBox.getChildren().add(categoryLabel);

                // TilePane for products
                TilePane productPane = new TilePane();
                productPane.setHgap(20);
                productPane.setVgap(20);
                productPane.setPadding(new Insets(10, 0, 10, 60));
                productPane.setPrefColumns(4);
                productPane.setPrefTileWidth(itemCard.getPrefWidth() * 1.5);
                productPane.setPrefTileHeight(itemCard.getPrefHeight() * 1.3);

                List<AnchorPane> productCards = new ArrayList<>();

                // 2. Get products by category using stored function
                ResultSet rsProducts = DBHelper.executeFunction("GetProductsByCategory", categoryId);
                while (rsProducts.next()) {
                    int productId = rsProducts.getInt("productid");
                    String productName = rsProducts.getString("name");
                    double price = rsProducts.getDouble("price");

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
    private AnchorPane createItemCard(int itemId, String name, double stock){
        AnchorPane clone = new AnchorPane();
        clone.setPrefWidth(itemCard.getPrefWidth());
        clone.setPrefHeight(itemCard.getPrefHeight());
        clone.setStyle("-fx-background-color: #fff; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10,0,0,3);");
        clone.setPickOnBounds(true); // allow clicks anywhere on pane

        ImageView img = new ImageView(placeholderImage);
        img.setFitWidth(clone.getPrefWidth() - 10);
        img.setFitHeight(clone.getPrefHeight() * 0.6);
        img.setLayoutX(5);
        img.setLayoutY(10);
        img.setMouseTransparent(true); // pass clicks to parent

        Text txtName = new Text(name);
        txtName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-fill: #532386;");
        txtName.setLayoutX(10);
        txtName.setLayoutY(img.getFitHeight() + 40);
        txtName.setMouseTransparent(true);

        Text txtStock = new Text("Stock: " + stock);
        txtStock.setStyle("-fx-font-size: 16px; -fx-fill: #8b6fa1;");
        txtStock.setLayoutX(10);
        txtStock.setLayoutY(img.getFitHeight() + 70);
        txtStock.setMouseTransparent(true);

        clone.getChildren().addAll(img, txtName, txtStock);
        clone.setUserData(itemId);

        // Add click handler
        clone.setOnMouseClicked(e -> openProductModal(itemId));

        return clone;
    }
    private void openProductModal(int productId) {
        if (productId <= 0) return;

        editingProductId = productId; // mark modal as "edit mode"

        try (ResultSet rs = DBHelper.executeFunction("getproductdetails", productId)) {
            if (rs != null && rs.next()) {
                txtModalProductName.setText(rs.getString("name") != null ? rs.getString("name") : "");
                txtModalPrice.setText(rs.getString("price") != null ? rs.getString("price") : "0");
                txtModalDescription.setText(rs.getString("description") != null ? rs.getString("description") : "");
                txtModalSupplier.setText(String.valueOf(rs.getInt("supplierid")));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        // Load categories into ComboBox
        loadCategoriesForModal();

        try (ResultSet rsCat = DBHelper.executeFunction("GetProductCategory", productId)) {
            if (rsCat != null && rsCat.next()) {
                String categoryName = rsCat.getString("category_name");
                cmbModalCategory.getSelectionModel().select(categoryName);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        productModal.setVisible(true);
    }


    private void filterItems(String query) {
        query = query.toLowerCase();
        for (Node node : categoriesContainer.getChildren()) {
            if (!(node instanceof VBox categoryBox)) continue;
            TilePane itemPane = (TilePane) categoryBox.getChildren().get(1);
            List<AnchorPane> cards = categoryProductsMap.get(itemPane);
            List<AnchorPane> filtered = new ArrayList<>();
            for (AnchorPane card : cards) {
                Text nameText = (Text) card.getChildren().stream().filter(n -> n instanceof Text t && t.getText().startsWith("Stock") == false).findFirst().orElse(null);
                if (nameText != null && nameText.getText().toLowerCase().contains(query)) filtered.add(card);
            }
            itemPane.getChildren().setAll(filtered);
            categoryBox.setVisible(!filtered.isEmpty());
            categoryBox.setManaged(!filtered.isEmpty());
        }
    }

    private void applyFilterAndSort() {
        // Placeholder: implement manager-specific sorting/filtering here
    }
}
