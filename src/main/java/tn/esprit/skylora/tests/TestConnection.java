package tn.esprit.skylora.tests;

import tn.esprit.skylora.utils.MyConnection;

public class TestConnection {

    public static void main(String[] args) {

        MyConnection mc = MyConnection.getInstance();

        if (mc.getConnection() != null) {
            System.out.println("Connexion réussie !");
        } else {
            System.out.println("Connexion échouée !");
        }
    }
}
