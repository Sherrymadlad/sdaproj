package com.example.testing2.controllers;

import com.example.testing2.utils.DBHelper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.sql.ResultSet;

public class AuditPageController {

    @FXML
    private Button loadAuditButton; // optional: can keep for refresh

    @FXML
    private VBox auditList;

    @FXML
    public void initialize() {
        // Automatically load audit logs when page opens
        loadAuditLogs();

        // Optional: allow manual refresh via button
        if (loadAuditButton != null) {
            loadAuditButton.setOnAction(e -> loadAuditLogs());
        }
    }

    private void loadAuditLogs() {
        String selectSQL = "SELECT logid, userid, action, timestamp FROM auditlog ORDER BY timestamp DESC";
        try (ResultSet rs = DBHelper.getConnection().createStatement().executeQuery(selectSQL)) {

            auditList.getChildren().clear();
            while (rs.next()) {
                int logUserId = rs.getInt("userid");
                String action = rs.getString("action");
                String timestamp = rs.getTimestamp("timestamp").toString();
                int logId = rs.getInt("logid");

                Label logEntry = new Label("[" + logId + "] " + timestamp +
                        " | UserID: " + logUserId +
                        " | Action: " + action);
                logEntry.setWrapText(true);
                logEntry.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-family: Consolas;");
                auditList.getChildren().add(logEntry);
            }

            rs.getStatement().close();

        } catch (Exception ex) {
            ex.printStackTrace();
            Label errorLabel = new Label("Error loading audit logs: " + ex.getMessage());
            auditList.getChildren().add(errorLabel);
        }
    }
}
