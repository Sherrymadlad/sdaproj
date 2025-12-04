package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.util.function.Consumer;

public class CustomModalController {

    @FXML private StackPane modalRoot;
    @FXML private Text modalMessage;
    @FXML private Button btnOk;
    @FXML private Button btnYes;
    @FXML private Button btnNo;
    @FXML private HBox confirmationButtons;

    @FXML
    private void initialize() {
        // Hide modal by default
        modalRoot.setVisible(false);

        // OK button closes modal
        btnOk.setOnAction(e -> modalRoot.setVisible(false));

        // Yes/No buttons default actions
        btnYes.setOnAction(e -> hideModal());
        btnNo.setOnAction(e -> hideModal());
    }

    private void hideModal() {
        modalRoot.setVisible(false);
    }

    /**
     * Shows a simple message with OK button.
     */
    public void showMessage(String message) {
        modalMessage.setText(message);
        btnOk.setVisible(true);
        confirmationButtons.setVisible(false);
        modalRoot.setVisible(true);
    }

    /**
     * Shows a confirmation dialog with Yes/No buttons.
     * Callback returns true if Yes clicked, false if No clicked.
     */
    public void showConfirmation(String message, Consumer<Boolean> callback) {
        modalMessage.setText(message);

        // Show Yes/No, hide OK
        btnOk.setVisible(false);
        confirmationButtons.setVisible(true);

        // Set button actions
        btnYes.setOnAction(e -> {
            hideModal();
            callback.accept(true);
        });

        btnNo.setOnAction(e -> {
            hideModal();
            callback.accept(false);
        });

        modalRoot.setVisible(true);
    }

    public StackPane getRoot() {
        return modalRoot;
    }
}
