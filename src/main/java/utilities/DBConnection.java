package utilities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static String dbhost = "jdbc:mysql://localhost:3306/school_db";
    private static String username = "root";
    private static String password = "root";
    private static Connection conn;

    public static Connection createNewDBconnection() {

        ReadConfigFile configs = new ReadConfigFile();
        dbhost = configs.getProperty("CONNECTION.URL");
        username = configs.getProperty("CONNECTION.USERNAME");
        password = configs.getProperty("CONNECTION.PASSWORD");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            conn = DriverManager.getConnection(
                    dbhost, username, password);
        } catch (SQLException e) {
            System.out.println("Error : Cannot create database connection");
            e.printStackTrace();
        } finally {
            return conn;
        }
    }
}