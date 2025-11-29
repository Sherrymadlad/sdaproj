package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.*;
import java.util.Map;
import java.util.ResourceBundle;

public class CartPageController implements Initializable {


    @FXML
    private VBox cartItemsContainer;

    @FXML
    private Text txtTotal;

    @FXML
    private TextField txtAddress;

    @FXML
    private CheckBox chkCard;

    @FXML
    private CheckBox chkCash;

    @FXML
    private Button btnPlaceOrder;

    private Map<Integer, Integer> cart;        // productId -> quantity
    private Map<Integer, String> productNames; // productId -> product name
    private Map<Integer, Double> productPrices;// productId -> product price

    private int currentUserId = -1; // set when opening cart

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Set PlaceOrder button handler
        currentUserId = 1;
        btnPlaceOrder.setOnAction(e -> handlePlaceOrder());
    }

    // Must be called immediately after loading FXML
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        fetchUserAddress(); // fetch address immediately
    }

    public void setCartData(Map<Integer, Integer> cart,
                            Map<Integer, String> productNames,
                            Map<Integer, Double> productPrices) {

        this.cart = cart;
        this.productNames = productNames;
        this.productPrices = productPrices;

        populateCartItems();
    }

    private void fetchUserAddress() {
        if (currentUserId <= 0) return; // safety

        String sql = "SELECT * FROM GetUserDetails(?)";

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, currentUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String address = rs.getString("address");
                    txtAddress.setText(address != null ? address : "");
                    System.out.println("Loaded address: " + address);
                } else {
                    txtAddress.setText("");
                    System.out.println("No user found with ID: " + currentUserId);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            txtAddress.setText("Error fetching address");
        }
    }

    private void populateCartItems() {
        cartItemsContainer.getChildren().clear();
        double total = 0;

        if (cart == null || cart.isEmpty()) {
            Text emptyText = new Text("Your cart is empty!");
            emptyText.setFont(Font.font("System", FontWeight.BOLD, 20));
            cartItemsContainer.getChildren().add(emptyText);
            txtTotal.setText("Total: Rs 0.0");
            return;
        }

        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();

            String name = productNames.get(productId);
            double price = productPrices.get(productId);

            HBox itemRow = new HBox(20);
            itemRow.setStyle("-fx-background-color:white; -fx-padding:10; -fx-border-radius:5; -fx-background-radius:5;");

            Text nameText = new Text(name);
            nameText.setFont(Font.font("System", FontWeight.BOLD, 16));

            Text qtyText = new Text("Qty: " + quantity);
            Text priceText = new Text("Rs " + (price * quantity));

            itemRow.getChildren().addAll(nameText, qtyText, priceText);
            cartItemsContainer.getChildren().add(itemRow);

            total += price * quantity;
        }

        txtTotal.setText("Total: Rs " + total);
    }

    private void handlePlaceOrder() {
        String paymentMethod;

        if (chkCard.isSelected()) {
            paymentMethod = "Card";
        } else if (chkCash.isSelected()) {
            paymentMethod = "Cash";
        } else {
            System.out.println("Please select a payment method!");
            return;
        }

        System.out.println("Placing order...");
        System.out.println("Address: " + txtAddress.getText());
        System.out.println("Payment: " + paymentMethod);
        System.out.println("Items: " + cart);

        System.out.println("Order placed successfully!");
    }


}
