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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.*;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Optional;

public class CartPageController implements Initializable {

    @FXML private VBox cartItemsContainer;
    @FXML private Text txtTotal;
    @FXML private TextField txtAddress;
    @FXML private CheckBox chkCard;
    @FXML private CheckBox chkCash;
    @FXML private Button btnPlaceOrder;

    private Map<Integer, Integer> cart;        // productId -> quantity
    private Map<Integer, String> productNames; // productId -> product name
    private Map<Integer, Double> productPrices;// productId -> product price
    private int currentUserId = -7; // set when opening cart

    private CustomerMainPageController parentController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnPlaceOrder.setOnAction(e -> handlePlaceOrder());
    }

    public void setParentController(CustomerMainPageController controller) {
        this.parentController = controller;
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        fetchUserAddress();
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
        if (currentUserId <= 0) return;
        String sql = "SELECT * FROM GetUserDetails(?)";
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, currentUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) txtAddress.setText(rs.getString("address"));
                else txtAddress.setText("");
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
        if (cart == null || cart.isEmpty()) {
            showAlert(AlertType.WARNING, "Cart is empty", "Please add items before placing an order.");
            return;
        }

        String paymentMethod;
        if (chkCard.isSelected()) paymentMethod = "Card";
        else if (chkCash.isSelected()) paymentMethod = "Cash";
        else {
            showAlert(AlertType.WARNING, "Payment Method", "Please select a payment method!");
            return;
        }

        // Confirmation modal
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Order");
        confirm.setHeaderText("Are you sure you want to place this order?");
        confirm.setContentText("Total: " + txtTotal.getText() + "\nPayment: " + paymentMethod);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // Place order in DB
        try (Connection conn = DBHelper.getConnection()) {
            conn.setAutoCommit(false); // transactional

            // Get customer ID
            int customerId;
            String customerSql = "SELECT customerid FROM customer WHERE userid = ?";
            try (PreparedStatement stmt = conn.prepareStatement(customerSql)) {
                stmt.setInt(1, currentUserId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) customerId = rs.getInt("customerid");
                else throw new SQLException("No customer found for user " + currentUserId);
            }

            // Insert order
            String orderSql = "INSERT INTO orders(ordertype, customerid, status) VALUES (?, ?, ?) RETURNING orderid";
            int orderId;
            try (PreparedStatement stmt = conn.prepareStatement(orderSql)) {
                stmt.setString(1, "Sales");
                stmt.setInt(2, customerId);
                stmt.setString(3, "Pending");
                ResultSet rs = stmt.executeQuery();
                rs.next();
                orderId = rs.getInt("orderid");
            }

            // Insert order items
            String itemSql = "INSERT INTO orderitem(orderid, productid, quantity, unitprice) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(itemSql)) {
                for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                    int pid = entry.getKey();
                    int qty = entry.getValue();
                    double price = productPrices.get(pid);
                    stmt.setInt(1, orderId);
                    stmt.setInt(2, pid);
                    stmt.setInt(3, qty);
                    stmt.setDouble(4, price);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();

            // Success alert
            showAlert(Alert.AlertType.INFORMATION, "Order Placed", "Your order has been placed successfully!");

            // Clear cart locally and update UI
            cart.clear();
            populateCartItems();
            txtTotal.setText("Total: Rs 0.0");

            // Reset main page cart badge
            if (parentController != null) {
                parentController.clearCart();
            }

            // Close cart window
            Stage stage = (Stage) btnPlaceOrder.getScene().getWindow();
            stage.close();

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to place order. Please try again.");
        }
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

}
