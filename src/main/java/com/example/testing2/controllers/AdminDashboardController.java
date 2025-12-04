package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {

    @FXML private Button tabMetrics;
    @FXML private Button tabUsers;
    @FXML private Button tabProducts;

    @FXML private Button btnReports;
    @FXML private Button btnAudit;
    @FXML private Button btnAdminProfile;

    @FXML private AnchorPane mainContent;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // --- Load Metrics page initially ---
        selectTab(tabMetrics, "/com/example/testing2/MetricsPage.fxml");

        // --- Top Tab Buttons ---
        tabMetrics.setOnAction(e -> selectTab(tabMetrics, "/com/example/testing2/MetricsPage.fxml"));
        tabUsers.setOnAction(e -> selectTab(tabUsers, "/com/example/testing2/UsersPage.fxml"));
        tabProducts.setOnAction(e -> selectTab(tabProducts, "/com/example/testing2/ProductPage.fxml"));

        // --- Sidebar Buttons ---
        btnReports.setOnAction(e -> loadPageIntoMain("/com/example/testing2/ReportsPage.fxml"));
        btnAudit.setOnAction(e -> loadPageIntoMain("/com/example/testing2/AuditPage.fxml"));
        btnAdminProfile.setOnAction(e -> loadPageIntoMain("/com/example/testing2/AdminsProfilePage.fxml"));
    }

    // ---------------------- SELECT TAB --------------------
    private void selectTab(Button selectedTab, String fxmlPath) {
        highlightTab(selectedTab);
        loadPageIntoMain(fxmlPath);
    }

    // ---------------------- LOAD PAGES INTO mainContent --------------------
    private void loadPageIntoMain(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();

            // Clear previous content before adding new
            mainContent.getChildren().clear();
            mainContent.getChildren().add(page);

            // Anchor the page to fill mainContent completely
            AnchorPane.setTopAnchor(page, 0.0);
            AnchorPane.setBottomAnchor(page, 0.0);
            AnchorPane.setLeftAnchor(page, 0.0);
            AnchorPane.setRightAnchor(page, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------------------- HIGHLIGHT ACTIVE TAB --------------------
    private void highlightTab(Button selected) {
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
