package com.tradematrix;
import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseManager.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM transactions");
            System.out.println("Transactions in DB:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", UserID: " + rs.getInt("user_id") + ", Ticker: " + rs.getString("ticker") + ", Type: " + rs.getString("transaction_type") + ", Qty: " + rs.getDouble("quantity"));
            }
            
            rs = stmt.executeQuery("SELECT * FROM user_profile");
            System.out.println("\nUsers in DB:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Username: " + rs.getString("username"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
