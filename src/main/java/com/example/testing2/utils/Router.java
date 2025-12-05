package com.example.testing2.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class Router {


    public static void openNewWindow(String fxmlFile, String title, int width, int height) throws Exception {
        FXMLLoader loader = new FXMLLoader(Router.class.getResource("/com/example/testing2/" + fxmlFile));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root, width, height));
        stage.show();
    }


    public static void navigate(ActionEvent event, String fxmlFile) throws Exception {
        Stage stage;

        if (event != null) {
            stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        } else {
            stage = new Stage();
        }

        FXMLLoader loader = new FXMLLoader(Router.class.getResource("/com/example/testing2/" + fxmlFile));
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.show();
    }


    public static void navigateWithData(ActionEvent event, String fxmlFile, int data) throws Exception {
        Stage stage;

        if (event != null) {
            stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        } else {
            stage = new Stage();
        }

        FXMLLoader loader = new FXMLLoader(Router.class.getResource("/com/example/testing2/" + fxmlFile));
        Parent root = loader.load();

        Object controller = loader.getController();
        if (controller instanceof DataReceiver receiver) {
            receiver.setData(data);
        }

        stage.setScene(new Scene(root));
        stage.show();
    }
}
