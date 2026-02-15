package tn.esprit.skylora.tests;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import tn.esprit.skylora.entities.Ticket;
import tn.esprit.skylora.services.ServiceTicket;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JUnit 5 Test Class for Ticket CRUD Operations
 * Fixed version - no changes made to ServiceTicket
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestCRUDTicket {

    private static ServiceTicket serviceTicket;
    private static int insertedTicketId;

    @BeforeAll
    static void setUpBeforeClass() {
        System.out.println("=== Starting Ticket CRUD Tests ===");
        serviceTicket = new ServiceTicket();
    }

    @AfterAll
    static void tearDownAfterClass() {
        System.out.println("=== Ticket CRUD Tests Completed ===");
    }

    @BeforeEach
    void setUp() {
        System.out.println("Preparing test...");
    }

    @AfterEach
    void tearDown() {
        System.out.println("Test completed.\n");
    }

    // ==================== CREATE TESTS ====================

    @Test
    @Order(1)
    @DisplayName("Test 1: Create a new ticket")
    void testAjouterTicket() {
        try {
            Ticket testTicket = new Ticket(
                    1,
                    "Cannot login to system",
                    "Technique",
                    "Haute",
                    "Ouvert",
                    "User is unable to login using correct credentials. Error message: Invalid username or password."
            );

            serviceTicket.ajouter(testTicket);

            List<Ticket> allTickets = serviceTicket.afficher();
            Ticket createdTicket = allTickets.stream()
                    .filter(t -> "Cannot login to system".equals(t.getSubject()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(createdTicket, "Ticket should be created and retrievable");
            insertedTicketId = createdTicket.getId();
            System.out.println("✓ Ticket created with ID: " + insertedTicketId);

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test 2: Create ticket with missing required fields")
    void testAjouterTicketInvalide() {
        try {
            Ticket invalidTicket = new Ticket(
                    1,
                    null,
                    "Technique",
                    "Moyenne",
                    "Ouvert",
                    "Description with null subject - " + System.currentTimeMillis()
            );

            serviceTicket.ajouter(invalidTicket);

            System.out.println("→ Database allows NULL subject");

            // Cleanup
            List<Ticket> tickets = serviceTicket.afficher();
            tickets.stream()
                    .filter(t -> t.getSubject() == null &&
                            t.getDescription() != null &&
                            t.getDescription().startsWith("Description with null subject"))
                    .findFirst()
                    .ifPresent(t -> {
                        try {
                            serviceTicket.supprimer(t.getId());
                        } catch (SQLException ignored) {}
                    });

        } catch (SQLException e) {
            System.out.println("→ Database rejected NULL subject: " + e.getMessage());
        }
    }

    // ==================== READ TESTS ====================

    @Test
    @Order(3)
    @DisplayName("Test 3: Retrieve all tickets")
    void testAfficherTickets() {
        try {
            List<Ticket> tickets = serviceTicket.afficher();

            assertNotNull(tickets, "Tickets list should not be null");
            assertFalse(tickets.isEmpty(), "Tickets list should not be empty");

            System.out.println("✓ Retrieved " + tickets.size() + " ticket(s)");

            boolean found = tickets.stream()
                    .anyMatch(t -> t.getId() == insertedTicketId);
            assertTrue(found, "Our test ticket should be in the list");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    @DisplayName("Test 4: Retrieve tickets by user ID")
    void testGetTicketsByUserId() {
        try {
            List<Ticket> userTickets = serviceTicket.getTicketsByUserId(1);

            assertNotNull(userTickets);
            assertFalse(userTickets.isEmpty());

            userTickets.forEach(ticket ->
                    assertEquals(1, ticket.getUtilisateurId())
            );

            System.out.println("✓ Retrieved " + userTickets.size() + " ticket(s) for user ID 1");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test 5: Retrieve tickets by non-existent user")
    void testGetTicketsByNonExistentUser() {
        try {
            List<Ticket> userTickets = serviceTicket.getTicketsByUserId(99999);

            assertNotNull(userTickets);
            assertTrue(userTickets.isEmpty());

            System.out.println("✓ Non-existent user properly handled (empty list)");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @Order(6)
    @DisplayName("Test 6: Update ticket information")
    void testModifierTicket() {
        try {
            assumeTrue(insertedTicketId > 0);

            List<Ticket> tickets = serviceTicket.afficher();
            Ticket ticketToUpdate = tickets.stream()
                    .filter(t -> t.getId() == insertedTicketId)
                    .findFirst()
                    .orElse(null);

            assertNotNull(ticketToUpdate);

            ticketToUpdate.setSubject("Login issue - UPDATED");
            ticketToUpdate.setStatut("En cours");
            ticketToUpdate.setPriorite("Critique");
            ticketToUpdate.setDescription("Updated description");

            serviceTicket.modifier(ticketToUpdate);

            List<Ticket> updatedTickets = serviceTicket.afficher();
            Ticket updatedTicket = updatedTickets.stream()
                    .filter(t -> t.getId() == insertedTicketId)
                    .findFirst()
                    .orElse(null);

            assertNotNull(updatedTicket);
            assertEquals("Login issue - UPDATED", updatedTicket.getSubject());
            assertEquals("En cours", updatedTicket.getStatut());
            assertEquals("Critique", updatedTicket.getPriorite());

            System.out.println("✓ Ticket updated successfully");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("Test 7: Update ticket status")
    void testUpdateStatus() {
        try {
            assumeTrue(insertedTicketId > 0);

            serviceTicket.updateStatus(insertedTicketId, "RESOLVED");

            List<Ticket> tickets = serviceTicket.afficher();
            Ticket updated = tickets.stream()
                    .filter(t -> t.getId() == insertedTicketId)
                    .findFirst()
                    .orElse(null);

            assertNotNull(updated);
            assertEquals("RESOLVED", updated.getStatut());
            assertNotNull(updated.getDateResolution());

            System.out.println("✓ Status updated to RESOLVED");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Test 8: Update ticket priority")
    void testUpdatePriority() {
        try {
            assumeTrue(insertedTicketId > 0);

            String newPriority = "Basse";
            serviceTicket.updatePriority(insertedTicketId, newPriority);

            List<Ticket> tickets = serviceTicket.afficher();
            Ticket updated = tickets.stream()
                    .filter(t -> t.getId() == insertedTicketId)
                    .findFirst()
                    .orElse(null);

            assertNotNull(updated);
            assertEquals(newPriority, updated.getPriorite());

            System.out.println("✓ Priority updated");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    @DisplayName("Test 9: Assign agent to ticket")
    void testAssignTicket() {
        try {
            assumeTrue(insertedTicketId > 0);

            int agentId = 5;
            serviceTicket.assignTicket(insertedTicketId, agentId);

            List<Ticket> tickets = serviceTicket.afficher();
            Ticket updated = tickets.stream()
                    .filter(t -> t.getId() == insertedTicketId)
                    .findFirst()
                    .orElse(null);

            assertNotNull(updated);
            assertEquals(agentId, updated.getAgentId());

            System.out.println("✓ Agent assigned");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    @DisplayName("Test 10: Update non-existent ticket")
    void testModifierTicketNonExistant() {
        Ticket fake = new Ticket(
                999999,
                1,
                "Fake",
                "Technique",
                "Basse",
                "Ouvert",
                "Fake desc",
                LocalDateTime.now(),
                null,
                null
        );

        assertDoesNotThrow(() -> serviceTicket.modifier(fake));
        System.out.println("✓ Non-existent update handled (no exception)");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @Order(11)
    @DisplayName("Test 11: Delete ticket")
    void testSupprimerTicket() {
        try {
            assumeTrue(insertedTicketId > 0);

            serviceTicket.supprimer(insertedTicketId);

            List<Ticket> tickets = serviceTicket.afficher();
            boolean exists = tickets.stream()
                    .anyMatch(t -> t.getId() == insertedTicketId);

            assertFalse(exists);

            System.out.println("✓ Ticket deleted");

        } catch (SQLException e) {
            fail("SQLException occurred: " + e.getMessage());
        }
    }

    @Test
    @Order(12)
    @DisplayName("Test 12: Delete non-existent ticket")
    void testSupprimerTicketNonExistant() {
        assertDoesNotThrow(() -> serviceTicket.supprimer(999999));
        System.out.println("✓ Non-existent delete handled");
    }

    // ==================== VALIDATION TESTS ====================

    @Test
    @Order(13)
    @DisplayName("Test 13: Validate ticket priority values")
    void testValidatePriorite() {
        String[] priorities = {"Basse", "Moyenne", "Haute", "Critique"};

        for (String prio : priorities) {
            try {
                String subject = "Prio-Test-" + prio + "-" + System.currentTimeMillis();

                Ticket t = new Ticket(1, subject, "Technique", prio, "Ouvert", "Test");
                serviceTicket.ajouter(t);

                List<Ticket> list = serviceTicket.afficher();
                boolean found = list.stream().anyMatch(x -> subject.equals(x.getSubject()));
                assertTrue(found);

                list.stream()
                        .filter(x -> subject.equals(x.getSubject()))
                        .findFirst()
                        .ifPresent(x -> {
                            try { serviceTicket.supprimer(x.getId()); } catch (Exception ignored) {}
                        });

            } catch (SQLException e) {
                fail("Priority " + prio + " failed: " + e.getMessage());
            }
        }
        System.out.println("✓ Priorities validated");
    }

    @Test
    @Order(14)
    @DisplayName("Test 14: Validate ticket status values")
    void testValidateStatut() {
        String[] statuses = {"Ouvert", "En cours", "Résolu", "Fermé"};

        for (String stat : statuses) {
            try {
                String subject = "Status-Test-" + stat + "-" + System.currentTimeMillis();

                Ticket t = new Ticket(1, subject, "Technique", "Moyenne", stat, "Test");
                serviceTicket.ajouter(t);

                List<Ticket> list = serviceTicket.afficher();
                boolean found = list.stream().anyMatch(x -> subject.equals(x.getSubject()));
                assertTrue(found);

                list.stream()
                        .filter(x -> subject.equals(x.getSubject()))
                        .findFirst()
                        .ifPresent(x -> {
                            try { serviceTicket.supprimer(x.getId()); } catch (Exception ignored) {}
                        });

            } catch (SQLException e) {
                fail("Status " + stat + " failed: " + e.getMessage());
            }
        }
        System.out.println("✓ Statuses validated");
    }

    @Test
    @Order(15)
    @DisplayName("Test 15: Test date creation auto-generation")
    void testDateCreationAutoGeneration() {
        try {
            String subject = "DateTest-" + System.currentTimeMillis();

            Ticket t = new Ticket(1, subject, "Technique", "Basse", "Ouvert", "Auto date test");
            serviceTicket.ajouter(t);

            List<Ticket> list = serviceTicket.afficher();
            Ticket created = list.stream()
                    .filter(x -> subject.equals(x.getSubject()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(created);
            assertNotNull(created.getDateCreation());
            assertTrue(created.getDateCreation().isBefore(LocalDateTime.now().plusSeconds(10)));

            serviceTicket.supprimer(created.getId());

            System.out.println("✓ Date creation auto-generated");

        } catch (SQLException e) {
            fail("SQLException: " + e.getMessage());
        }
    }

    @Test
    @Order(16)
    @DisplayName("Test 16: Test multiple ticket operations in sequence")
    void testMultipleOperationsSequence() {
        try {
            String prefix = "SeqTest-" + System.currentTimeMillis() + "-";

            Ticket t1 = new Ticket(1, prefix + "A", "Technique", "Haute", "Ouvert", "Seq 1");
            Ticket t2 = new Ticket(1, prefix + "B", "Facturation", "Moyenne", "Ouvert", "Seq 2");

            serviceTicket.ajouter(t1);
            serviceTicket.ajouter(t2);

            List<Ticket> tickets = serviceTicket.afficher();

            long count = tickets.stream()
                    .filter(t -> t.getSubject() != null && t.getSubject().startsWith(prefix))
                    .count();

            assertEquals(2, count, "Should have 2 tickets");

            Ticket toUpdate = tickets.stream()
                    .filter(t -> t.getSubject() != null && t.getSubject().equals(prefix + "A"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Ticket A not found"));

            serviceTicket.updatePriority(toUpdate.getId(), "Basse");
            serviceTicket.updateStatus(toUpdate.getId(), "En cours");

            // Delete
            tickets = serviceTicket.afficher();
            tickets.stream()
                    .filter(t -> t.getSubject() != null && t.getSubject().startsWith(prefix))
                    .forEach(t -> {
                        try {
                            serviceTicket.supprimer(t.getId());
                        } catch (SQLException ex) {
                            fail("Delete failed: " + ex.getMessage());
                        }
                    });

            tickets = serviceTicket.afficher();
            count = tickets.stream()
                    .filter(t -> t.getSubject() != null && t.getSubject().startsWith(prefix))
                    .count();

            assertEquals(0, count, "All sequence tickets should be gone");

            System.out.println("✓ Sequence operations completed");

        } catch (SQLException e) {
            fail("SQLException: " + e.getMessage());
        }
    }
}