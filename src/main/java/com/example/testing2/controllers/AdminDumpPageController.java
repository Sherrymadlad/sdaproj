package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

import java.io.FileWriter;
import java.sql.*;
import java.util.function.Consumer;

public class AdminDumpPageController {

    @FXML
    private Button btnDownloadDump;

    @FXML
    private AnchorPane rootPane; // The root pane of this FXML

    private CustomModalController modalController;

    // Database connection info
    private final String DB_URL =
            "jdbc:postgresql://ep-bold-feather-ahvtkpm2-pooler.c-3.us-east-1.aws.neon.tech:5432/neondb?sslmode=require";
    private final String DB_USER = "neondb_owner";
    private final String DB_PASS = "npg_Sx1EknRHBrT7";

    @FXML
    public void initialize() {
        setupModal();

        // Button style + hover
        String baseColor = "#8b6fa1";
        String hoverColor = "#a281c3";

        btnDownloadDump.setStyle(
                "-fx-background-color: " + baseColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 12;" +
                        "-fx-cursor: hand;"
        );

        btnDownloadDump.setOnMouseEntered(e -> {
            btnDownloadDump.setScaleX(1.05);
            btnDownloadDump.setScaleY(1.05);
            btnDownloadDump.setStyle(
                    "-fx-background-color: " + hoverColor + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 18px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 12;" +
                            "-fx-cursor: hand;"
            );
        });

        btnDownloadDump.setOnMouseExited(e -> {
            btnDownloadDump.setScaleX(1.0);
            btnDownloadDump.setScaleY(1.0);
            btnDownloadDump.setStyle(
                    "-fx-background-color: " + baseColor + ";" +
                            "-fx-text-fill: white;" +
                            "-fx-font-size: 18px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 12;" +
                            "-fx-cursor: hand;"
            );
        });

        // Click action with confirmation
        btnDownloadDump.setOnAction(e -> {
            modalController.showConfirmation(
                    "Are you sure you want to generate a database backup?",
                    this::handleBackupConfirmation
            );
        });
    }

    /**
     * Load custom modal FXML and attach it to rootPane
     */
    private void setupModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/testing2/CustomModal.fxml"));
            StackPane modalRoot = loader.load();
            modalController = loader.getController();

            // Add modal to root of this page
            rootPane.getChildren().add(modalRoot);
            AnchorPane.setTopAnchor(modalRoot, 0.0);
            AnchorPane.setBottomAnchor(modalRoot, 0.0);
            AnchorPane.setLeftAnchor(modalRoot, 0.0);
            AnchorPane.setRightAnchor(modalRoot, 0.0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Called when user clicks Yes on confirmation modal
     */
    private void handleBackupConfirmation(Boolean confirmed) {
        if (confirmed) {
            try {
                String filename = generateSqlBackup();
                modalController.showMessage("✅ Backup created successfully:\n" + filename);
            } catch (Exception ex) {
                modalController.showMessage("❌ Backup failed:\n" + ex.getMessage());
            }
        }
    }

    /**
     * Generate SQL backup and return filename
     */
    private String generateSqlBackup() throws Exception {
        String filename = "backup_" + System.currentTimeMillis() + ".sql";

        try (
                Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                FileWriter writer = new FileWriter(filename)
        ) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, "public", "%", new String[]{"TABLE"});

            while (tables.next()) {
                String table = tables.getString("TABLE_NAME");

                writer.write("\n-- ===============================\n");
                writer.write("-- Table: " + table + "\n");
                writer.write("-- ===============================\n");

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM " + table);
                ResultSetMetaData rsmd = rs.getMetaData();
                int cols = rsmd.getColumnCount();

                while (rs.next()) {
                    StringBuilder insert = new StringBuilder(
                            "INSERT INTO \"" + table + "\" VALUES ("
                    );

                    for (int i = 1; i <= cols; i++) {
                        Object val = rs.getObject(i);
                        if (val == null) insert.append("NULL");
                        else insert.append("'").append(val.toString().replace("'", "''")).append("'");

                        if (i < cols) insert.append(", ");
                    }

                    insert.append(");\n");
                    writer.write(insert.toString());
                }
            }

            System.out.println("Backup created successfully: " + filename);
            return filename;

        } catch (Exception ex) {
            System.err.println("Backup failed:");
            ex.printStackTrace();
            throw ex;
        }
    }
}
