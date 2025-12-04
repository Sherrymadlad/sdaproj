package com.example.testing2.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.io.FileWriter;
import java.sql.*;

public class AdminDumpPageController {

    @FXML
    private Button btnDownloadDump;

    // ✅ YOUR NEON JDBC DETAILS (APPLIED)
    private final String DB_URL =
            "jdbc:postgresql://ep-bold-feather-ahvtkpm2-pooler.c-3.us-east-1.aws.neon.tech:5432/neondb?sslmode=require";
    private final String DB_USER = "neondb_owner";
    private final String DB_PASS = "npg_Sx1EknRHBrT7";  // ⚠️ Rotate this after testing

    @FXML
    public void initialize() {
        btnDownloadDump.setOnAction(e -> generateSqlBackup());
    }

    private void generateSqlBackup() {
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

                        if (val == null) {
                            insert.append("NULL");
                        } else {
                            insert.append("'")
                                    .append(val.toString().replace("'", "''"))
                                    .append("'");
                        }

                        if (i < cols) insert.append(", ");
                    }

                    insert.append(");\n");
                    writer.write(insert.toString());
                }
            }

            System.out.println("✅ Backup created successfully: " + filename);

        } catch (Exception ex) {
            System.err.println("❌ Backup failed:");
            ex.printStackTrace();
        }
    }
}
