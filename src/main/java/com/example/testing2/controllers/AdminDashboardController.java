package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Text txtTotalUsers;
    @FXML private Text txtTotalProducts;
    @FXML private Text txtTotalSales;
    @FXML private Text txtTotalRevenue;
    @FXML private Button btnReports;
    @FXML private Button tabMetrics;
    @FXML private Button tabUsers;
    @FXML private Button tabProducts;
    @FXML private Button btnAudit; // Add this if not already declared

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Load metrics on default
        loadDashboardMetrics();
        highlightTab(tabMetrics); // Metrics tab highlighted by default

        // Reports button uses openReportPage()
        btnReports.setOnAction(event -> openReportPage());
        btnAudit.setOnAction(event -> openPage("/com/example/testing2/AuditPage.fxml"));

        // Top tabs
        tabMetrics.setOnAction(event -> {
            // Already on AdminDashboard, just highlight tab
            highlightTab(tabMetrics);
        });

        tabUsers.setOnAction(event -> {
            highlightTab(tabUsers);
            openPage("/com/example/testing2/UsersPage.fxml");
        });

        tabProducts.setOnAction(event -> {
            highlightTab(tabProducts);
            openPage("/com/example/testing2/ProductPage.fxml");
        });
    }



    private void loadDashboardMetrics() {
        try {
            ResultSet rs = DBHelper.executeFunction("GetDashboardMetrics");
            if (rs.next()) {
                txtTotalUsers.setText(String.valueOf(rs.getInt("total_users")));
                txtTotalProducts.setText(String.valueOf(rs.getInt("total_products")));
                txtTotalSales.setText(String.valueOf(rs.getInt("total_sales_orders")));
                txtTotalRevenue.setText("Rs " + rs.getDouble("total_revenue"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to load dashboard metrics");
        }
    }

    private void openPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) tabMetrics.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to load page: " + fxmlPath);
        }
    }

    private void highlightTab(Button selectedTab) {
        tabMetrics.setStyle("-fx-background-color: #c2add3; -fx-text-fill: #4b355a; -fx-font-size: 16px; -fx-background-radius: 5;");
        tabUsers.setStyle("-fx-background-color: #c2add3; -fx-text-fill: #4b355a; -fx-font-size: 16px; -fx-background-radius: 5;");
        tabProducts.setStyle("-fx-background-color: #c2add3; -fx-text-fill: #4b355a; -fx-font-size: 16px; -fx-background-radius: 5;");

        selectedTab.setStyle("-fx-background-color: #8b6fa1; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 5;");
    }
    // Method to open reports page
    private void openReportPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/ReportsPage.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnReports.getScene().getWindow(); // get current window
            stage.getScene().setRoot(root); // replace current scene content
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to load Report Page");
        }
    }
}
