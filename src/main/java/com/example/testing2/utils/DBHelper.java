package com.example.testing2.utils;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.*;

public class DBHelper {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String HOST = dotenv.get("DB_HOST");
    private static final String PORT = dotenv.get("DB_PORT");
    private static final String DATABASE = dotenv.get("DB_NAME");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");
    private static final String SSLMODE = dotenv.get("DB_SSLMODE");

    private static final String URL = String.format(
            "jdbc:postgresql://%s:%s/%s?sslmode=%s",
            HOST, PORT, DATABASE, SSLMODE
    );

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to NeonDB!");
        }
        return connection;
    }

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

    // In DBHelper.java
    public static int getCustomerIdByUserId(int userId) {
        int customerId = 0;
        try {
            String query = "SELECT customerid FROM customer WHERE userid = ?";
            var conn = getConnection(); // your existing connection method
            var pst = conn.prepareStatement(query);
            pst.setInt(1, userId);
            var rs = pst.executeQuery();
            if (rs.next()) {
                customerId = rs.getInt("customerid");
            }
            rs.close();
            pst.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return customerId;
    }

    public static ResultSet executeQuery(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = getConnection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
        return stmt.executeQuery();
    }

}