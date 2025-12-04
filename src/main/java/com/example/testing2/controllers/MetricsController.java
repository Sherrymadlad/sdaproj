package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.scene.text.Text;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MetricsController {

    @FXML private Text txtTotalUsers;
    @FXML private Text txtTotalProducts;
    @FXML private Text txtTotalSales;
    @FXML private Text txtTotalRevenue;

    @FXML
    public void initialize() {
        loadDashboardMetrics();
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
        }
    }
}
