package com.example.testing2.utils;

import java.sql.*;

/**
 * DBHelper: Utility class for NeonDB (PostgreSQL) connections and procedure/function calls.
 * Uses hardcoded credentials for testing.
 */
public class DBHelper {

    // 🔥 Hardcoded NeonDB credentials (replace when needed)
    private static final String HOST = "ep-bold-feather-ahvtkpm2-pooler.c-3.us-east-1.aws.neon.tech";
    private static final String USER = "neondb_owner";
    private static final String PASSWORD = "npg_Sx1EknRHBrT7";
    private static final String DATABASE = "neondb";
    private static final String PORT = "5432";

    // JDBC URL with SSL
    private static final String URL = String.format(
            "jdbc:postgresql://%s:%s/%s?sslmode=require",
            HOST, PORT, DATABASE
    );

    private static Connection connection;

    /**
     * Get a singleton connection to NeonDB
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to NeonDB!");
        }
        return connection;
    }

    /**
     * Execute a stored procedure (PostgreSQL ≥11) without returning a result
     */
    public static void executeProcedure(String procName, Object... params) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            sb.append("?");
            if (i < params.length - 1) sb.append(",");
        }

        String query = String.format("CALL %s(%s)", procName, sb);

        try (CallableStatement stmt = getConnection().prepareCall(query)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.execute();
        }
    }

    /**
     * Execute a PostgreSQL function that returns rows (ResultSet)
     */
    public static ResultSet executeFunction(String funcName, Object... params) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.length; i++) {
            sb.append("?");
            if (i < params.length - 1) sb.append(",");
        }

        String query = String.format("SELECT * FROM %s(%s)", funcName, sb);

        PreparedStatement stmt = getConnection().prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }

        return stmt.executeQuery();
    }

    /**
     * Close the singleton connection
     */
    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("✅ NeonDB connection closed!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
