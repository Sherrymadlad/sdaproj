package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class CustomerMainPageController implements Initializable {

    @FXML
    private AnchorPane itemCard; // Template card (hidden in FXML)

    @FXML
    private VBox categoriesContainer; // VBox inside ScrollPane to hold categories

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        itemCard.setVisible(false); // hide template
        loadCategoriesWithProducts();
    }

    private void loadCategoriesWithProducts() {
        try {
            ResultSet rs = DBHelper.executeFunction("GetAllCategories");

            while (rs.next()) {
                int categoryId = rs.getInt("categoryid");
                String categoryName = rs.getString("name");

                // --- Category label ---
                Label categoryLabel = new Label(categoryName);
                categoryLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #826478;");

                // Add category label to VBox
                categoriesContainer.getChildren().add(categoryLabel);

                // --- TilePane for products ---
                TilePane productPane = new TilePane();
                productPane.setHgap(20);
                productPane.setVgap(20);
                productPane.setPadding(new Insets(10, 0, 10, 0));
                productPane.setPrefColumns(4); // number of cards per row

                // Load products for this category
                ResultSet products = DBHelper.executeFunction("GetProductsByCategory", categoryId);

                while (products.next()) {
                    String productName = products.getString("name");
                    double price = products.getDouble("price");

                    AnchorPane card = createItemCard(productName, price);
                    productPane.getChildren().add(card);
                }

                // Add TilePane under category label
                categoriesContainer.getChildren().add(productPane);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private AnchorPane createItemCard(String name, double price) {
        AnchorPane clone = new AnchorPane();
        clone.setPrefWidth(itemCard.getPrefWidth());
        clone.setPrefHeight(itemCard.getPrefHeight());
        clone.setStyle(itemCard.getStyle()); // copy background/style

        // --- Product Image ---
        ImageView img = new ImageView();
        img.setFitWidth(205);
        img.setFitHeight(112);
        img.setLayoutX(0);
        img.setLayoutY(0);

        try {
            img.setImage(new Image(getClass().getResource("/com/example/testing2/images/placeholder.jpg").toExternalForm()));
        } catch (Exception e) {
            System.out.println("Image missing: " + e.getMessage());
        }

        // --- Product Name ---
        Text itemName = new Text(name);
        itemName.setStyle("-fx-font-size: 15px; -fx-fill: #826478;");
        itemName.setLayoutX(8);
        itemName.setLayoutY(160);

        // --- Product Price ---
        Text itemPrice = new Text("Rs " + price);
        itemPrice.setStyle("-fx-font-size: 15px; -fx-fill: #826478;");
        itemPrice.setLayoutX(140);
        itemPrice.setLayoutY(160);

        clone.getChildren().addAll(img, itemName, itemPrice);
        return clone;
    }
}
