package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;

public class ProductsPageController {

    @FXML
    private VBox productsList;

    @FXML
    private ComboBox<String> cbStockFilter;

    @FXML
    private Button btnClearFilter;

    public void initialize() {
        // Stock filter options
        cbStockFilter.getItems().addAll("All", "Low (<10)", "Medium (10-50)", "High (>50)");
        cbStockFilter.setValue("All");

        cbStockFilter.setOnAction(e -> loadProducts());
        btnClearFilter.setOnAction(e -> {
            cbStockFilter.setValue("All");
            loadProducts();
        });

        loadProducts();
    }

    private void loadProducts() {
        productsList.getChildren().clear();

        String filter = cbStockFilter.getValue();
        String sql = "SELECT * FROM GetAllProducts()";

        try (Connection conn = DBHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int productId = rs.getInt("productid");
                String name = rs.getString("productname");
                String category = rs.getString("categoryname");
                int stock = rs.getInt("stocklevel");
                double price = rs.getDouble("price");
                String description = rs.getString("description");

                // Filter by stock if needed
                if (!"All".equals(filter)) {
                    if (filter.startsWith("Low") && stock >= 10) continue;
                    if (filter.startsWith("Medium") && (stock < 10 || stock > 50)) continue;
                    if (filter.startsWith("High") && stock <= 50) continue;
                }

                // ===================== TITLED PANE =====================
                TitledPane tp = new TitledPane();
                tp.setExpanded(false);
                tp.setStyle("-fx-background-color: #f3e6f7; -fx-border-width: 2; -fx-background-radius: 10; -fx-border-radius: 10;");

                // ===================== HEADER =====================
                HBox header = new HBox(10);
                header.setPadding(new Insets(10));
                header.setStyle("-fx-background-color: #f3e6f7;");

                Label lblName = new Label(name);
                lblName.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

                Pane spacer = new Pane();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                Label lblStock = new Label("Stock: " + stock);

                Button btnEdit = new Button("Edit");
                btnEdit.setOnAction(e -> openEditDialog(productId));

                header.getChildren().addAll(lblName, spacer, lblStock, btnEdit);
                tp.setGraphic(header);

                // ===================== CONTENT =====================
                VBox content = new VBox(12);
                content.setPadding(new Insets(10, 15, 10, 15));
                content.setStyle("-fx-background-color: #f3e6f7;");

                HBox hCategory = new HBox(10, new Label("Category:"), new Label(category));
                hCategory.getChildren().get(0).setStyle("-fx-font-weight: bold;");

                HBox hPrice = new HBox(10, new Label("Price:"), new Label(String.valueOf(price)));
                hPrice.getChildren().get(0).setStyle("-fx-font-weight: bold;");

                HBox hDesc = new HBox(10, new Label("Description:"), new Label(description != null ? description : "-"));
                hDesc.getChildren().get(0).setStyle("-fx-font-weight: bold;");

                content.getChildren().addAll(hCategory, hPrice, hDesc);
                tp.setContent(content);

                productsList.getChildren().add(tp);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openEditDialog(int productId) {
        try (Connection conn = DBHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT p.name AS productname, c.name AS categoryname, p.price, p.description, p.categoryid " +
                             "FROM product p LEFT JOIN category c ON p.categoryid = c.categoryid WHERE p.productid = ?")) {

            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            String currentName = rs.getString("productname");
            int currentCategoryId = rs.getInt("categoryid");
            double currentPrice = rs.getDouble("price");
            String currentDesc = rs.getString("description");

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Edit Product");

            // Buttons
            ButtonType btnSave = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(btnSave, ButtonType.CANCEL);

            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));

            TextField tfName = new TextField(currentName);
            TextField tfPrice = new TextField(String.valueOf(currentPrice));
            TextArea taDesc = new TextArea(currentDesc);

            // Load categories dynamically
            ComboBox<String> cbCategory = new ComboBox<>();
            try (Statement catStmt = conn.createStatement();
                 ResultSet catRs = catStmt.executeQuery("SELECT categoryid, name FROM category")) {
                while (catRs.next()) {
                    cbCategory.getItems().add(catRs.getInt("categoryid") + " - " + catRs.getString("name"));
                    if (catRs.getInt("categoryid") == currentCategoryId) {
                        cbCategory.setValue(catRs.getInt("categoryid") + " - " + catRs.getString("name"));
                    }
                }
            }

            vbox.getChildren().addAll(new Label("Name:"), tfName,
                    new Label("Price:"), tfPrice,
                    new Label("Description:"), taDesc,
                    new Label("Category:"), cbCategory);

            dialog.getDialogPane().setContent(vbox);

            dialog.setResultConverter(button -> {
                if (button == btnSave) {
                    try (PreparedStatement updatePs = conn.prepareStatement(
                            "UPDATE product SET name=?, price=?, description=?, categoryid=? WHERE productid=?")) {
                        updatePs.setString(1, tfName.getText());
                        updatePs.setDouble(2, Double.parseDouble(tfPrice.getText()));
                        updatePs.setString(3, taDesc.getText());
                        int selectedCatId = Integer.parseInt(cbCategory.getValue().split(" - ")[0]);
                        updatePs.setInt(4, selectedCatId);
                        updatePs.setInt(5, productId);
                        updatePs.executeUpdate();
                        loadProducts(); // refresh list
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                return null;
            });

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
