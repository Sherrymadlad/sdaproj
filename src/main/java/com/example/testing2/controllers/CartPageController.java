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
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import java.math.BigDecimal;
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.AnchorPane;


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
    private CustomModalController customModalController;
    @FXML private StackPane mainRoot;

    private Map<Integer, Integer> cart;        // productId -> quantity
    private Map<Integer, String> productNames; // productId -> product name
    private Map<Integer, Double> productPrices;// productId -> product price
    private int currentUserId; // set when opening cart

    private CustomerMainPageController parentController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        btnPlaceOrder.setOnAction(e -> handlePlaceOrder());
        // Load Custom Modal
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomModal.fxml"));
            StackPane modalRoot = loader.load();
            customModalController = loader.getController();

            // Add modal to main root and anchor it to fill the parent
            mainRoot.getChildren().add(modalRoot);
            AnchorPane.setTopAnchor(modalRoot, 0.0);
            AnchorPane.setBottomAnchor(modalRoot, 0.0);
            AnchorPane.setLeftAnchor(modalRoot, 0.0);
            AnchorPane.setRightAnchor(modalRoot, 0.0);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void setParentController(CustomerMainPageController controller) {
        this.parentController = controller;
    }

    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("CartPageController: currentUserId = " + currentUserId);
        fetchUserAddress();
        if (cart != null) populateCartItems();
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
            itemRow.setStyle("-fx-background-color: #f5e6ff; -fx-padding: 15; -fx-background-radius: 12;");
            itemRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Pane spacer = new Pane();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);


            // Product name - Bigger & bolder
            Text nameText = new Text(name);
            nameText.setFont(Font.font("System", FontWeight.BOLD, 20));
            nameText.setStyle("-fx-fill: #4b0082;"); // dark purple shade (looks premium)


// - Button (Bigger, Rounded)
            Button btnDecrease = new Button("-");
            btnDecrease.setStyle(
                    "-fx-font-size: 22;" +
                            "-fx-background-color: #f8f1fd;" +   // soft red ->purple
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 5 12;"
            );


// Quantity text - Bigger
            Text qtyText = new Text(String.valueOf(quantity));
            qtyText.setFont(Font.font("System", FontWeight.BOLD, 20));
            qtyText.setStyle("-fx-fill: #000000;");


// + Button (Bigger, Rounded)
            Button btnIncrease = new Button("+");
            btnIncrease.setStyle(
                    "-fx-font-size: 22;" +
                            "-fx-background-color: #f8f1fd;" +   // soft green->purple
                            "-fx-background-radius: 8;" +
                            "-fx-padding: 5 12;"
            );


// Price text - Eye catchy (Pinkish gradient feel)
            Text priceText = new Text("Rs " + (price * quantity));
            priceText.setFont(Font.font("System", FontWeight.BOLD, 22));
            priceText.setStyle("-fx-fill: #4b0082;");   // hot pink


            // Handlers for +/- buttons
            btnIncrease.setOnAction(e -> {
                cart.put(productId, cart.get(productId) + 1);
                qtyText.setText(String.valueOf(cart.get(productId)));
                priceText.setText("Rs " + (price * cart.get(productId)));
                updateTotal();
                if (parentController != null) parentController.updateCartBadge();
            });

            btnDecrease.setOnAction(e -> {
                int currentQty = cart.get(productId) - 1;
                if (currentQty <= 0) {
                    cart.remove(productId);
                    cartItemsContainer.getChildren().remove(itemRow);
                } else {
                    cart.put(productId, currentQty);
                    qtyText.setText(String.valueOf(currentQty));
                    priceText.setText("Rs " + (price * currentQty));
                }
                updateTotal();
                if (parentController != null) parentController.updateCartBadge();
            });

            itemRow.getChildren().addAll(
                    nameText,
                    btnDecrease,
                    qtyText,
                    btnIncrease,
                    spacer,      // <-- pushes price to the extreme right
                    priceText
            );

            cartItemsContainer.getChildren().add(itemRow);

            total += price * quantity;
        }

        txtTotal.setText("Total: Rs " + total);
    }


    private void updateTotal() {
        double total = 0;
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            int pid = entry.getKey();
            total += productPrices.get(pid) * entry.getValue();
        }
        txtTotal.setText("Total: Rs " + total);
    }

    private void handlePlaceOrder() {
        if (cart == null || cart.isEmpty()) {
            showCustomModal("Cart is empty, Please add items before placing an order.");
            return;
        }

        String paymentMethod;
        if (chkCard.isSelected()) paymentMethod = "Card";
        else if (chkCash.isSelected()) paymentMethod = "Cash";
        else {
            showCustomModal("Please select a payment method!");
            return;
        }

        // Show confirmation modal
        showCustomModalConfirmation(
                "Place Order\nAre you sure you want to place the order?",
                confirmed -> {
                    if (!confirmed) return;

                    try (Connection conn = DBHelper.getConnection()) {
                        conn.setAutoCommit(false); // start transaction

                        // Get customer ID
                        int customerId;
                        String customerSql = "SELECT customerid FROM customer WHERE userid = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(customerSql)) {
                            stmt.setInt(1, currentUserId);
                            ResultSet rs = stmt.executeQuery();
                            if (rs.next()) customerId = rs.getInt("customerid");
                            else throw new SQLException("No customer found for user " + currentUserId);
                        }

                        // Call AddSalesOrder stored procedure for each product in cart
                        String procSql = "SELECT AddSalesOrder(?, ?, ?, ?, ?)";
                        try (PreparedStatement stmt = conn.prepareStatement(procSql)) {
                            int warehouseId = 1; // replace with actual logic if needed
                            for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
                                int productId = entry.getKey();
                                int qty = entry.getValue();
                                double price = productPrices.get(productId);

                                stmt.setInt(1, customerId);
                                stmt.setInt(2, warehouseId);
                                stmt.setInt(3, productId);
                                stmt.setInt(4, qty);
                                stmt.setBigDecimal(5, BigDecimal.valueOf(price));

                                stmt.execute();
                            }
                        }

                        conn.commit();

                        // Success modal
                        showCustomModal("Your order has been placed successfully!");

                        // Clear cart locally and update UI
                        cart.clear();
                        populateCartItems();
                        txtTotal.setText("Total: Rs 0.0");

                        // Reset main page cart badge
                        if (parentController != null) {
                            parentController.clearCart();
                        }

                        mainRoot.getChildren().remove(mainRoot);

                    } catch (SQLException e) {
                        e.printStackTrace();
                        showCustomModal("Failed to place order. Please try again.\n" + e.getMessage());
                    }
                }
        );
    }


    private void updateParentCartBadge() {
        if (parentController != null) {
            parentController.updateCartBadge();
        }
    }

    private void showCustomModal(String message) {
        if (customModalController != null) {
            customModalController.showMessage(message);
        } else {
            System.err.println("Custom modal not initialized: " + message);
        }
    }

    private void showCustomModalConfirmation(String message, java.util.function.Consumer<Boolean> callback) {
        if (customModalController != null) {
            customModalController.showConfirmation(message, callback);
        } else {
            // Fallback: auto-confirm if modal not initialized
            callback.accept(true);
        }
    }

}
