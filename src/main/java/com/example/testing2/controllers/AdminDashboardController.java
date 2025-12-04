package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable, SidebarListener {

    @FXML private BorderPane rootPane;
    @FXML private AnchorPane mainContent;

    @FXML private SidebarController sidebarController;

    @FXML private javafx.scene.control.Button tabMetrics;
    @FXML private javafx.scene.control.Button tabUsers;
    @FXML private javafx.scene.control.Button tabProducts;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Connect Sidebar → Dashboard
        if (sidebarController != null) {
            sidebarController.setSidebarListener(this);
        }

        // Load default page
        selectTab(tabMetrics, "/com/example/testing2/MetricsPage.fxml");

        // Top tabs
        tabMetrics.setOnAction(e -> selectTab(tabMetrics, "/com/example/testing2/MetricsPage.fxml"));
        tabUsers.setOnAction(e -> selectTab(tabUsers, "/com/example/testing2/UsersPage.fxml"));
        tabProducts.setOnAction(e -> selectTab(tabProducts, "/com/example/testing2/ProductPage.fxml"));
    }

    // Sidebar click handler
    @Override
    public void onPageSelected(String pageName) {
        switch (pageName) {
            case "Reports" -> loadPageIntoMain("/com/example/testing2/ReportsPage.fxml");
            case "Audit" -> loadPageIntoMain("/com/example/testing2/AuditPage.fxml");
            case "Profile" -> loadPageIntoMain("/com/example/testing2/AdminsProfilePage.fxml");
            default -> System.out.println("Unknown sidebar page: " + pageName);
        }
    }

    // Select top tabs
    private void selectTab(javafx.scene.control.Button selectedTab, String fxmlPath) {
        highlightTab(selectedTab);
        loadPageIntoMain(fxmlPath);
    }

    // Load FXML into main area
    private void loadPageIntoMain(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();

            mainContent.getChildren().setAll(page);

            AnchorPane.setTopAnchor(page, 0.0);
            AnchorPane.setBottomAnchor(page, 0.0);
            AnchorPane.setLeftAnchor(page, 0.0);
            AnchorPane.setRightAnchor(page, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Highlight active top tab
    private void highlightTab(javafx.scene.control.Button selected) {
        tabMetrics.setStyle(defaultStyle());
        tabUsers.setStyle(defaultStyle());
        tabProducts.setStyle(defaultStyle());

        selected.setStyle(activeStyle());
    }

    private String defaultStyle() {
        return "-fx-background-color: #c2add3; -fx-text-fill: #4b355a; "
                + "-fx-font-size: 16px; -fx-background-radius: 5;";
    }

    private String activeStyle() {
        return "-fx-background-color: #8b6fa1; -fx-text-fill: white; "
                + "-fx-font-size: 16px; -fx-background-radius: 5;";
    }
}
