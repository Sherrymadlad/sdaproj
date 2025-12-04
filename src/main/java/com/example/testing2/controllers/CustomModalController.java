package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

public class CustomModalController {

    @FXML private StackPane modalRoot;
    @FXML private Text modalMessage;
    @FXML private Button btnOk;

    @FXML
    private void initialize() {
        btnOk.setOnAction(e -> modalRoot.setVisible(false));
    }

    public void showMessage(String message) {
        modalMessage.setText(message);
        modalRoot.setVisible(true);
    }

    public StackPane getRoot() {
        return modalRoot;
    }
}
