package com.skilora.model.entity.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Application entity
 */
class ApplicationTest {

    private Application application;

    @BeforeEach
    void setUp() {
        application = new Application();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(application);
        assertEquals(Application.Status.PENDING, application.getStatus());
        assertNotNull(application.getAppliedDate());
    }

    @Test
    void testConstructorWithParameters() {
        Application app = new Application(1, 2);
        assertEquals(1, app.getJobOfferId());
        assertEquals(2, app.getCandidateProfileId());
        assertEquals(Application.Status.PENDING, app.getStatus());
        assertNotNull(app.getAppliedDate());
    }

    @Test
    void testGettersAndSetters() {
        application.setId(1);
        application.setJobOfferId(10);
        application.setCandidateProfileId(20);
        application.setStatus(Application.Status.ACCEPTED);
        application.setCoverLetter("Test cover letter");
        application.setCustomCvUrl("http://example.com/cv.pdf");
        application.setJobTitle("Software Engineer");
        application.setCompanyName("Tech Corp");
        application.setCandidateName("John Doe");
        application.setJobLocation("Tunis");

        assertEquals(1, application.getId());
        assertEquals(10, application.getJobOfferId());
        assertEquals(20, application.getCandidateProfileId());
        assertEquals(Application.Status.ACCEPTED, application.getStatus());
        assertEquals("Test cover letter", application.getCoverLetter());
        assertEquals("http://example.com/cv.pdf", application.getCustomCvUrl());
        assertEquals("Software Engineer", application.getJobTitle());
        assertEquals("Tech Corp", application.getCompanyName());
        assertEquals("John Doe", application.getCandidateName());
        assertEquals("Tunis", application.getJobLocation());
    }

    @Test
    void testStatusEnum() {
        Application.Status[] statuses = Application.Status.values();
        assertEquals(6, statuses.length);
        
        assertEquals("En attente", Application.Status.PENDING.getDisplayName());
        assertEquals("En cours", Application.Status.REVIEWING.getDisplayName());
        assertEquals("Entretien", Application.Status.INTERVIEW.getDisplayName());
        assertEquals("Offre", Application.Status.OFFER.getDisplayName());
        assertEquals("Refusé", Application.Status.REJECTED.getDisplayName());
        assertEquals("Accepté", Application.Status.ACCEPTED.getDisplayName());
    }

    @Test
    void testSetAppliedDate() {
        LocalDateTime customDate = LocalDateTime.of(2024, 1, 15, 10, 30);
        application.setAppliedDate(customDate);
        assertEquals(customDate, application.getAppliedDate());
    }

    @Test
    void testEquals() {
        Application app1 = new Application();
        app1.setId(1);
        
        Application app2 = new Application();
        app2.setId(1);
        
        Application app3 = new Application();
        app3.setId(2);
        
        assertEquals(app1, app2);
        assertNotEquals(app1, app3);
        assertEquals(app1, app1);
        assertNotEquals(app1, null);
        assertNotEquals(app1, "not an application");
    }

    @Test
    void testHashCode() {
        Application app1 = new Application();
        app1.setId(1);
        
        Application app2 = new Application();
        app2.setId(1);
        
        assertEquals(app1.hashCode(), app2.hashCode());
    }

    @Test
    void testToString() {
        application.setId(1);
        application.setJobOfferId(10);
        application.setCandidateProfileId(20);
        application.setStatus(Application.Status.PENDING);
        
        String result = application.toString();
        assertTrue(result.contains("Application"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("jobOfferId=10"));
        assertTrue(result.contains("candidateProfileId=20"));
        assertTrue(result.contains("status=PENDING"));
    }
}

