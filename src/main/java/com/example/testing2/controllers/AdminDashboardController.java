package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable, SidebarListener {

    @FXML private Button tabMetrics;
    @FXML private Button tabUsers;
    @FXML private Button tabProducts;

    private int currentUserId;
    AdminSidebarController  adminSidebarController;
    @FXML private AnchorPane sidebarContainer;
    @FXML private AnchorPane mainContent;
    @FXML private HBox header;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupSidebar();

        // --- Load Metrics page initially ---
        selectTab(tabMetrics, "/com/example/testing2/MetricsPage.fxml");

        tabMetrics.setOnAction(e -> selectTab(tabMetrics,"/com/example/testing2/MetricsPage.fxml"));
        tabUsers.setOnAction(e -> selectTab(tabUsers,"/com/example/testing2/UsersPage.fxml"));
        tabProducts.setOnAction(e -> selectTab(tabProducts,"/com/example/testing2/ProductPage.fxml"));

        addHoverEffect(tabMetrics);
        addHoverEffect(tabUsers);
        addHoverEffect(tabProducts);
    }

    private void addHoverEffect(Button btn) {
        btn.setOnMouseEntered(e -> {
            btn.setScaleX(1.05);
            btn.setScaleY(1.05);
        });

        btn.setOnMouseExited(e -> {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        });
    }

    private void setupSidebar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/AdminSidebar.fxml"));
            AnchorPane sidebar = loader.load();
            adminSidebarController = loader.getController();
            adminSidebarController.setSidebarListener(this);

            sidebarContainer.getChildren().add(sidebar);
            AnchorPane.setTopAnchor(sidebar, 0.0);
            AnchorPane.setBottomAnchor(sidebar, 0.0);
            AnchorPane.setLeftAnchor(sidebar, 0.0);
            AnchorPane.setRightAnchor(sidebar, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPageSelected(String pageName) {
        try {
            mainContent.getChildren().removeIf(node -> "overlay".equals(node.getUserData()));

            switch (pageName) {
                case "Metrics" -> {
                    header.setVisible(true);
                    highlightTab(tabMetrics); // reset tab styles
                    loadPageIntoMain("/com/example/testing2/MetricsPage.fxml"); // load content
                }
                case "Audits" -> loadPage("/com/example/testing2/AuditPage.fxml");
                case "Reports" -> loadPage("/com/example/testing2/ReportsPage.fxml");
                case "Notifications" -> loadPage("/com/example/testing2/NotificationsPage.fxml");
                case "Backup" -> loadPage("/com/example/testing2/AdminDumpPage.fxml");
                case "Logout" -> {
                    // Handle logout here
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/LoginPage.fxml"));
                    AnchorPane loginPage = loader.load();
                    mainContent.getScene().setRoot(loginPage);
                }
                default -> System.out.println("Unknown page: " + pageName);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void loadPage(String fxmlPath) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        AnchorPane page = loader.load();
        addOverlay(page);
    }

    private void addOverlay(AnchorPane page) {
        page.setUserData("overlay");
        mainContent.getChildren().clear();
        header.setVisible(false);
        mainContent.getChildren().add(page);
        AnchorPane.setTopAnchor(page, 0.0);
        AnchorPane.setBottomAnchor(page, 0.0);
        AnchorPane.setLeftAnchor(page, 0.0);
        AnchorPane.setRightAnchor(page, 0.0);
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
                + "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 5;";
    }

    private String activeStyle() {
        return "-fx-background-color: #8b6fa1; -fx-text-fill: white; "
                + "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 5;";
    }
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        System.out.println("✅ AdminDashboard loaded for userId: " + userId);
        // You can also use this ID to load user-specific data if needed
    }

}
