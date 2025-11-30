package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Text txtTotalUsers;
    @FXML private Text txtTotalProducts;
    @FXML private Text txtTotalSales;
    @FXML private Text txtTotalRevenue;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        loadDashboardMetrics();
    }

    private void loadDashboardMetrics() {
        try {
            ResultSet rs = DBHelper.executeFunction("GetDashboardMetrics");

            if (rs.next()) {
                int totalUsers = rs.getInt("total_users");
                int totalProducts = rs.getInt("total_products");
                int totalSales = rs.getInt("total_sales_orders");
                double totalRevenue = rs.getDouble("total_revenue");

                txtTotalUsers.setText(String.valueOf(totalUsers));
                txtTotalProducts.setText(String.valueOf(totalProducts));
                txtTotalSales.setText(String.valueOf(totalSales));
                txtTotalRevenue.setText("Rs " + totalRevenue);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("❌ Failed to load dashboard metrics");
        }
    }
}
