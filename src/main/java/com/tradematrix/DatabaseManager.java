package com.tradematrix;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // Requires MySQL running locally
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tradematrix?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "123456";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static List<Holding> getHoldings(int userId) {
        List<Holding> holdings = new ArrayList<>();
        String sql = "SELECT ticker, SUM(CASE WHEN transaction_type = 'Buy' THEN quantity ELSE -quantity END) AS net_qty, AVG(price_per_share) AS avg_cost " +
                     "FROM transactions WHERE user_id = ? GROUP BY ticker HAVING net_qty > 0";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String ticker = rs.getString("ticker");
                    double netQty = rs.getDouble("net_qty");
                    double avgCost = rs.getDouble("avg_cost");

                    Holding holding = new Holding(ticker);
                    holding.setQuantity(netQty);
                    holding.setAvgCost(avgCost);
                    holding.setLtp(avgCost);
                    holding.setCurrentValue(holding.getInvested());
                    holding.setPnl(0);
                    holdings.add(holding);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Rows fetched from DB: " + holdings.size());
        return holdings;
    }

    public static void initializeDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Driver not found in Classpath!");
        }
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
             
            String createUsers = "CREATE TABLE IF NOT EXISTS user_profile (" +
                                 "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                 "username VARCHAR(100), " +
                                 "email VARCHAR(255), " +
                                 "full_name VARCHAR(255), " +
                                 "password VARCHAR(255), " +
                                 "mobile_number VARCHAR(20), " +
                                 "base_currency VARCHAR(10) DEFAULT 'INR', " +
                                 "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(createUsers);

            String createTx = "CREATE TABLE IF NOT EXISTS transactions (" +
                              "id INT AUTO_INCREMENT PRIMARY KEY, " +
                              "user_id INT DEFAULT 0, " +
                              "ticker VARCHAR(100) NOT NULL, " +
                              "transaction_type ENUM('Buy', 'Sell') NOT NULL, " +
                              "quantity DOUBLE NOT NULL, " +
                              "price_per_share DOUBLE NOT NULL, " +
                              "transaction_date DATE NOT NULL, " +
                              "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(createTx);

            // Migrate existing schema if column is missing
            if (!columnExists(conn, "user_profile", "username")) {
                stmt.execute("ALTER TABLE user_profile ADD COLUMN username VARCHAR(100)");
            }
            if (!columnExists(conn, "user_profile", "email")) {
                stmt.execute("ALTER TABLE user_profile ADD COLUMN email VARCHAR(255)");
            }
            if (!columnExists(conn, "user_profile", "password")) {
                stmt.execute("ALTER TABLE user_profile ADD COLUMN password VARCHAR(255)");
            }
            if (!columnExists(conn, "user_profile", "base_currency")) {
                stmt.execute("ALTER TABLE user_profile ADD COLUMN base_currency VARCHAR(10) DEFAULT 'INR'");
            }
            if (!columnExists(conn, "transactions", "user_id")) {
                stmt.execute("ALTER TABLE transactions ADD COLUMN user_id INT DEFAULT 0");
            }
            
            System.out.println("MySQL database 'tradematrix' initialized successfully.");
        } catch (SQLException e) {
            System.err.println("MySQL Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }
}

