package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NotificationsPageController {

    @FXML
    private VBox notificationsContainer;

    public void initialize() {
        loadNotifications();
    }

    private void loadNotifications() {
        notificationsContainer.getChildren().clear();

        String query = "SELECT * FROM GetLowStockProducts()";

        try (Connection conn = DBHelper.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            boolean hasResults = false;

            while (rs.next()) {
                hasResults = true;

                String product = rs.getString("product_name");           // updated
                int qty = rs.getInt("total_available_stock");            // updated
                int min = rs.getInt("minthreshold");                     // same as before

                VBox card = createNotificationCard(product, qty, min);
                notificationsContainer.getChildren().add(card);
            }

            if (!hasResults) {
                Label empty = new Label("No low-stock products 🎉");
                empty.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
                notificationsContainer.getChildren().add(empty);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createNotificationCard(String product, int qty, int min) {
        VBox card = new VBox();
        card.setPadding(new Insets(15));
        card.setSpacing(8);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 4);"
        );
        card.setPrefWidth(780);

        Label title = new Label(product);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2B2B2B;");

        Label qtyLabel = new Label("Available: " + qty);
        qtyLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #444;");

        Label minLabel = new Label("Minimum Required: " + min);
        minLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #444;");

        HBox bottomLine = new HBox();
        Label warning = new Label("⚠ Low Stock");
        warning.setStyle("-fx-text-fill: #E63946; -fx-font-size: 14px; -fx-font-weight: bold;");
        bottomLine.getChildren().add(warning);

        VBox.setVgrow(card, Priority.NEVER);

        card.getChildren().addAll(title, qtyLabel, minLabel, bottomLine);

        return card;
    }
}
