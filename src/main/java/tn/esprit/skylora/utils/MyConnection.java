package tn.esprit.skylora.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {

    private static MyConnection instance;
    private Connection cnx;

    private String url = "jdbc:mysql://localhost:3306/skilora";
    private String user = "root";
    private String password = "";

    private MyConnection() {
        try {
            cnx = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Connexion base de données établie !");
        } catch (SQLException e) {
            System.out.println("❌ Erreur connexion !");
            e.printStackTrace();
        }
    }

    public static MyConnection getInstance() {
        if(instance == null) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return cnx;
    }
}
