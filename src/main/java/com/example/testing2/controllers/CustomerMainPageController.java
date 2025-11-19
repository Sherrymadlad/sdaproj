package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;


public class CustomerMainPageController implements Initializable {

    @FXML
    private AnchorPane itemCard;   // template card

    @FXML
    private AnchorPane itemsContainer; // AnchorPane inside ScrollPane

    private double yOffset = 10; // vertical offset for stacking elements

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

                // --- Category title ---
                Text categoryText = new Text(categoryName);
                categoryText.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-fill: #826478;");
                categoryText.setLayoutX(10);
                categoryText.setLayoutY(yOffset + 20); // leave top padding
                itemsContainer.getChildren().add(categoryText);

                yOffset += 40; // space after category

                // --- Load products ---
                try (ResultSet products = DBHelper.executeFunction("GetProductsByCategory", categoryId)) {
                    double xOffset = 10; // start left
                    for (; products.next(); ) {
                        String productName = products.getString("name");
                        double price = products.getDouble("price");

                        AnchorPane card = createItemCard(productName, price);
                        card.setLayoutX(xOffset);
                        card.setLayoutY(yOffset);

                        itemsContainer.getChildren().add(card);

                        xOffset += card.getPrefWidth() + 10; // next card

                        // wrap to next line if overflow
                        if (xOffset + card.getPrefWidth() > itemsContainer.getPrefWidth()) {
                            xOffset = 10;
                            yOffset += card.getPrefHeight() + 10;
                        }
                    }
                    yOffset += 240; // space after category products
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private AnchorPane createItemCard(String name, double price) {
        AnchorPane cardClone = new AnchorPane();
        cardClone.setPrefWidth(itemCard.getPrefWidth());
        cardClone.setPrefHeight(itemCard.getPrefHeight());
        cardClone.setStyle(itemCard.getStyle());

        // Upper half: placeholder image
        ImageView img = new ImageView();
        img.setFitWidth(205);
        img.setFitHeight(112);
        img.setPreserveRatio(false);

        // Load image from resources programmatically
        try {
            img.setImage(new Image(getClass().getResource("/com/example/testing2/images/placeholder.jpg").toExternalForm()));
        } catch (Exception e) {
            System.out.println("Image not found! " + e.getMessage());
        }

        // Lower half: name + price
        Text itemName = new Text(name);
        itemName.setLayoutX(10);
        itemName.setLayoutY(160);
        itemName.setStyle("-fx-font-size: 16; -fx-fill: #826478;");

        Text itemPrice = new Text("Rs " + price);
        itemPrice.setLayoutX(145);
        itemPrice.setLayoutY(160);
        itemPrice.setStyle("-fx-font-size: 16; -fx-fill: #826478;");

        cardClone.getChildren().addAll(img, itemName, itemPrice);
        return cardClone;
    }

}
