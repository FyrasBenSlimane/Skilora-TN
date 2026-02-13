package tn.esprit.skylora.tests;

import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.ServiceTicket;

public class TestCRUDTicket {

    public static void main(String[] args) {

        ServiceTicket st = new ServiceTicket();

        try {

            // 🔵 TEST INSERT
            Ticket t = new Ticket(
                    1,
                    "Bug Login",
                    "Bug Login",
                    "HAUTE",
                    "OUVERT",
                    "Impossible de se connecter à la plateforme");

            st.ajouter(t);
            System.out.println("✅ Ticket ajouté !");

            // 🔵 TEST SELECT
            System.out.println("📋 Liste Tickets :");
            st.afficher().forEach(System.out::println);

            // 🔵 TEST UPDATE
            t.setId(1); // ⚠️ mets un id existant
            t.setCategorie("Bug Paiement");
            st.modifier(t);
            System.out.println("✏️ Ticket modifié !");

            // 🔵 TEST DELETE
            st.supprimer(2); // ⚠️ mets un id existant
            System.out.println("🗑️ Ticket supprimé !");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
