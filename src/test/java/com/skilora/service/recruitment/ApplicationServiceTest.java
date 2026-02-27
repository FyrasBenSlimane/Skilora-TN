package com.skilora.service.recruitment;

import com.skilora.config.DatabaseConfig;
import com.skilora.model.entity.recruitment.Application;
import com.skilora.model.entity.recruitment.JobOffer;
import com.skilora.model.enums.JobStatus;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de test pour ApplicationService
 * 
 * Cette classe se trouve dans : src/test/java/com/skilora/service/recruitment
 * 
 * Objectif de cette classe :
 * Vérifier automatiquement que les méthodes CRUD fonctionnent sur la base de données MySQL.
 * 
 * Nous allons tester :
 * - apply(int, int, String, String) - ajouter une candidature
 * - getApplicationsByProfile(int) - afficher les candidatures d'un candidat
 * - getApplicationsByJobOffer(int) - afficher les candidatures pour une offre
 * - findApplicationById(int) - afficher une candidature par ID
 * - updateStatus(int, Status) - modifier le statut d'une candidature
 * - delete(int) - supprimer une candidature
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationServiceTest {

    private static ApplicationService applicationService;
    private static JobService jobService;
    private static int testCompanyId;
    private static int testJobOfferId;
    private static int testProfileId;
    private static int createdApplicationId;

    @BeforeAll
    static void setUpBeforeAll() throws SQLException {
        applicationService = ApplicationService.getInstance();
        jobService = JobService.getInstance();

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            // Créer une entreprise de test
            String checkCompanySql = "SELECT id FROM companies WHERE name = 'Test Company for ApplicationService' LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(checkCompanySql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    testCompanyId = rs.getInt("id");
                } else {
                    String insertCompanySql = "INSERT INTO companies (name, owner_id) VALUES (?, 1)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertCompanySql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        insertStmt.setString(1, "Test Company for ApplicationService");
                        insertStmt.executeUpdate();
                        try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                            if (keys.next()) {
                                testCompanyId = keys.getInt(1);
                            }
                        }
                    }
                }
            }

            // Créer une offre d'emploi de test
            JobOffer testJobOffer = new JobOffer();
            testJobOffer.setEmployerId(testCompanyId);
            testJobOffer.setTitle("Test Job Offer for Application");
            testJobOffer.setDescription("Description de test");
            testJobOffer.setLocation("Tunis");
            testJobOffer.setStatus(JobStatus.OPEN);
            testJobOfferId = jobService.createJobOffer(testJobOffer);

            // Créer un utilisateur de test d'abord
            int testUserId = 999;
            String checkUserSql = "SELECT id FROM users WHERE id = ? LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(checkUserSql)) {
                stmt.setInt(1, testUserId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        // Créer l'utilisateur de test
                        String insertUserSql = "INSERT INTO users (id, username, email, password, role, full_name, is_verified, is_active) VALUES (?, 'test_candidate', 'test@test.com', ?, 'CANDIDATE', 'Test Candidate', TRUE, TRUE)";
                        try (PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql)) {
                            insertUserStmt.setInt(1, testUserId);
                            insertUserStmt.setString(2, "$2a$10$dummy"); // Hash BCrypt factice pour les tests
                            insertUserStmt.executeUpdate();
                        }
                    }
                }
            }

            // Créer un profil de test
            String checkProfileSql = "SELECT id FROM profiles WHERE user_id = ? LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(checkProfileSql)) {
                stmt.setInt(1, testUserId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        testProfileId = rs.getInt("id");
                    } else {
                        String insertProfileSql = "INSERT INTO profiles (user_id, first_name, last_name) VALUES (?, 'Test', 'Candidate')";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertProfileSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                            insertStmt.setInt(1, testUserId);
                            insertStmt.executeUpdate();
                            try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                                if (keys.next()) {
                                    testProfileId = keys.getInt(1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @AfterAll
    static void tearDownAfterAll() throws SQLException {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            // Supprimer la candidature de test
            if (createdApplicationId > 0) {
                String deleteSql = "DELETE FROM applications WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                    stmt.setInt(1, createdApplicationId);
                    stmt.executeUpdate();
                }
            }

            // Supprimer l'offre de test
            if (testJobOfferId > 0) {
                jobService.deleteJobOffer(testJobOfferId);
            }

            // Supprimer le profil de test
            if (testProfileId > 0) {
                String deleteProfileSql = "DELETE FROM profiles WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteProfileSql)) {
                    stmt.setInt(1, testProfileId);
                    stmt.executeUpdate();
                }
            }

            // Supprimer l'utilisateur de test
            String deleteUserSql = "DELETE FROM users WHERE id = 999";
            try (PreparedStatement stmt = conn.prepareStatement(deleteUserSql)) {
                stmt.executeUpdate();
            }

            // Supprimer l'entreprise de test
            String deleteCompanySql = "DELETE FROM companies WHERE name = 'Test Company for ApplicationService'";
            try (PreparedStatement stmt = conn.prepareStatement(deleteCompanySql)) {
                stmt.executeUpdate();
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test ajouter(Application) - Créer une nouvelle candidature")
    void testApply() throws SQLException {
        // Arrange : Préparer les données
        String coverLetter = "Je suis très intéressé par ce poste et j'aimerais postuler.";
        String cvUrl = "uploads/cvs/test_cv.pdf";

        // Act : Exécuter la méthode à tester
        int applicationId = applicationService.apply(testJobOfferId, testProfileId, coverLetter, cvUrl);

        // Assert : Vérifier les résultats
        assertTrue(applicationId > 0, "L'ID de la candidature créée doit être supérieur à 0");
        createdApplicationId = applicationId;

        // Vérifier que la candidature existe dans la base de données
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            String sql = "SELECT * FROM applications WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, applicationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertTrue(rs.next(), "La candidature doit exister dans la base de données");
                    assertEquals(testJobOfferId, rs.getInt("job_offer_id"));
                    assertEquals(testProfileId, rs.getInt("candidate_profile_id"));
                    assertEquals("PENDING", rs.getString("status"));
                    assertEquals(coverLetter, rs.getString("cover_letter"));
                    assertEquals(cvUrl, rs.getString("custom_cv_url"));
                }
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("Test afficher() - Récupérer les candidatures d'un profil")
    void testGetApplicationsByProfile() throws SQLException {
        // Act : Exécuter la méthode à tester
        List<Application> applications = applicationService.getApplicationsByProfile(testProfileId);

        // Assert : Vérifier les résultats
        assertNotNull(applications, "La liste des candidatures ne doit pas être null");
        assertFalse(applications.isEmpty(), "La liste doit contenir au moins la candidature créée");

        // Vérifier que la candidature créée est dans la liste
        boolean found = applications.stream()
                .anyMatch(app -> app.getId() == createdApplicationId);
        assertTrue(found, "La candidature créée doit être dans la liste");

        // Vérifier les informations de la candidature
        Application foundApp = applications.stream()
                .filter(app -> app.getId() == createdApplicationId)
                .findFirst()
                .orElse(null);
        assertNotNull(foundApp);
        assertEquals(Application.Status.PENDING, foundApp.getStatus());
    }

    @Test
    @Order(3)
    @DisplayName("Test getApplicationsByJobOffer(int) - Récupérer les candidatures pour une offre")
    void testGetApplicationsByJobOffer() throws SQLException {
        // Act : Exécuter la méthode à tester
        List<Application> applications = applicationService.getApplicationsByJobOffer(testJobOfferId);

        // Assert : Vérifier les résultats
        assertNotNull(applications, "La liste des candidatures ne doit pas être null");
        assertFalse(applications.isEmpty(), "La liste doit contenir au moins la candidature créée");

        // Vérifier que la candidature créée est dans la liste
        boolean found = applications.stream()
                .anyMatch(app -> app.getId() == createdApplicationId);
        assertTrue(found, "La candidature créée doit être dans la liste");
    }

    @Test
    @Order(4)
    @DisplayName("Test getApplicationById(int) - Récupérer une candidature par son ID")
    void testFindApplicationById() throws SQLException {
        // Act : Exécuter la méthode à tester
        Application foundApplication = applicationService.getApplicationById(createdApplicationId);

        // Assert : Vérifier les résultats
        assertNotNull(foundApplication, "La candidature trouvée ne doit pas être null");
        assertEquals(createdApplicationId, foundApplication.getId());
        assertEquals(testJobOfferId, foundApplication.getJobOfferId());
        assertEquals(testProfileId, foundApplication.getCandidateProfileId());
        assertEquals(Application.Status.PENDING, foundApplication.getStatus());
        assertNotNull(foundApplication.getCoverLetter());
        assertNotNull(foundApplication.getCustomCvUrl());
    }

    @Test
    @Order(5)
    @DisplayName("Test modifier(Application) - Modifier le statut d'une candidature")
    void testUpdateStatus() throws SQLException {
        // Act : Exécuter la méthode à tester - Changer le statut à REVIEWING
        boolean updated = applicationService.updateStatus(createdApplicationId, Application.Status.REVIEWING);

        // Assert : Vérifier les résultats
        assertTrue(updated, "La mise à jour doit réussir");

        // Vérifier que le statut est modifié dans la base de données
        Application updatedApplication = applicationService.getApplicationById(createdApplicationId);
        assertNotNull(updatedApplication);
        assertEquals(Application.Status.REVIEWING, updatedApplication.getStatus());

        // Tester un autre changement de statut
        applicationService.updateStatus(createdApplicationId, Application.Status.ACCEPTED);
        Application acceptedApplication = applicationService.getApplicationById(createdApplicationId);
        assertNotNull(acceptedApplication);
        assertEquals(Application.Status.ACCEPTED, acceptedApplication.getStatus());
    }

    @Test
    @Order(6)
    @DisplayName("Test supprimer(int) - Supprimer une candidature")
    void testDelete() throws SQLException {
        // Act : Exécuter la méthode à tester
        boolean deleted = applicationService.delete(createdApplicationId);

        // Assert : Vérifier les résultats
        assertTrue(deleted, "La suppression doit réussir");

        // Vérifier que la candidature n'existe plus dans la base de données
        Application deletedApplication = applicationService.getApplicationById(createdApplicationId);
        assertNull(deletedApplication, "La candidature supprimée ne doit plus exister");

        // Vérifier directement dans la base de données
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            String sql = "SELECT * FROM applications WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, createdApplicationId);
                try (ResultSet rs = stmt.executeQuery()) {
                    assertFalse(rs.next(), "La candidature ne doit plus exister dans la base de données");
                }
            }
        }

        // Réinitialiser l'ID pour éviter les erreurs dans tearDown
        createdApplicationId = 0;
    }

    @Test
    @DisplayName("Test hasApplied - Vérifier si un candidat a déjà postulé")
    void testHasApplied() throws SQLException {
        // Créer une nouvelle candidature pour le test
        int newApplicationId = applicationService.apply(testJobOfferId, testProfileId, "Test", null);
        assertTrue(newApplicationId > 0);

        // Act : Vérifier si le candidat a postulé
        boolean hasApplied = applicationService.hasApplied(testJobOfferId, testProfileId);

        // Assert : Vérifier les résultats
        assertTrue(hasApplied, "Le candidat doit avoir postulé");

        // Nettoyer
        applicationService.delete(newApplicationId);
    }

    @Test
    @DisplayName("Test getApplicationById avec ID inexistant")
    void testGetApplicationByIdNotFound() throws SQLException {
        // Act : Chercher une candidature avec un ID qui n'existe pas
        Application application = applicationService.getApplicationById(999999);

        // Assert : Vérifier que null est retourné
        assertNull(application, "Une candidature inexistante doit retourner null");
    }
}

