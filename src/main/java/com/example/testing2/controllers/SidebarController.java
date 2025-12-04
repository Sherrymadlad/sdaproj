package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

import java.net.URL;
import java.util.ResourceBundle;

public class SidebarController implements Initializable {

    @FXML private Button btnItems;
    @FXML private Button btnOrders;
    @FXML private Button btnProfile;

    private final String baseColor = "#8b6fa1";
    private final String hoverColor = "#a281c3";
    private final String activeColor = "#6d4c7d";

    private Button activeButton; // Track which button is active

    // Listener for page routing
    private SidebarListener listener;

    public void setSidebarListener(SidebarListener listener) {
        this.listener = listener;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupButton(btnItems, "Items");
        setupButton(btnOrders, "Orders");
        setupButton(btnProfile, "Profile");
        setActiveButton(btnItems);
    }

    private void setupButton(Button button, String pageName) {
        button.setStyle(getBaseStyle());

        // Hover effects
        button.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (button != activeButton) button.setStyle(getHoverStyle());
            button.setScaleX(1.05); button.setScaleY(1.05);
        });

        button.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            if (button != activeButton) button.setStyle(getBaseStyle());
            button.setScaleX(1.0); button.setScaleY(1.0);
        });

        // Click sets active and notifies listener
        button.setOnAction(e -> {
            setActiveButton(button);
            if (listener != null) listener.onPageSelected(pageName);
        });
    }

    public void setActiveButton(Button button) {
        if (activeButton != null) activeButton.setStyle(getBaseStyle());
        activeButton = button;
        activeButton.setStyle(getActiveStyle());
    }

    // Styles
    private String getBaseStyle() {
        return "-fx-background-color:" + baseColor + ";" +
                "-fx-text-fill:white;-fx-font-size:25px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-cursor:hand;";
    }
    private String getHoverStyle() {
        return "-fx-background-color:" + hoverColor + ";" +
                "-fx-text-fill:white;-fx-font-size:25px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-cursor:hand;";
    }
    private String getActiveStyle() {
        return "-fx-background-color:" + activeColor + ";" +
                "-fx-text-fill:white;-fx-font-size:25px;-fx-font-weight:bold;" +
                "-fx-background-radius:20;-fx-cursor:hand;";
    }

    // Getters (optional if needed by main)
    public Button getItemsButton() { return btnItems; }
    public Button getOrdersButton() { return btnOrders; }
    public Button getProfileButton() { return btnProfile; }
}
