package com.skilora.model.entity.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Interview entity
 */
class InterviewTest {

    private Interview interview;

    @BeforeEach
    void setUp() {
        interview = new Interview();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(interview);
        assertEquals(Interview.InterviewType.IN_PERSON, interview.getInterviewType());
        assertEquals(Interview.InterviewStatus.SCHEDULED, interview.getStatus());
        assertNotNull(interview.getCreatedAt());
        assertNotNull(interview.getUpdatedAt());
    }

    @Test
    void testConstructorWithParameters() {
        LocalDateTime interviewDate = LocalDateTime.of(2024, 2, 15, 14, 30);
        Interview interview = new Interview(1, interviewDate);
        
        assertEquals(1, interview.getApplicationId());
        assertEquals(interviewDate, interview.getInterviewDate());
        assertEquals(Interview.InterviewType.IN_PERSON, interview.getInterviewType());
        assertEquals(Interview.InterviewStatus.SCHEDULED, interview.getStatus());
    }

    @Test
    void testGettersAndSetters() {
        interview.setId(1);
        interview.setApplicationId(10);
        LocalDateTime interviewDate = LocalDateTime.of(2024, 2, 15, 14, 30);
        interview.setInterviewDate(interviewDate);
        interview.setLocation("Office Building A, Room 101");
        interview.setInterviewType(Interview.InterviewType.VIDEO);
        interview.setNotes("Technical interview focusing on Java and Spring");
        interview.setStatus(Interview.InterviewStatus.COMPLETED);
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        interview.setCreatedAt(createdAt);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 11, 0);
        interview.setUpdatedAt(updatedAt);
        interview.setCandidateName("John Doe");
        interview.setJobTitle("Software Engineer");
        interview.setCompanyName("Tech Corp");

        assertEquals(1, interview.getId());
        assertEquals(10, interview.getApplicationId());
        assertEquals(interviewDate, interview.getInterviewDate());
        assertEquals("Office Building A, Room 101", interview.getLocation());
        assertEquals(Interview.InterviewType.VIDEO, interview.getInterviewType());
        assertEquals("Technical interview focusing on Java and Spring", interview.getNotes());
        assertEquals(Interview.InterviewStatus.COMPLETED, interview.getStatus());
        assertEquals(createdAt, interview.getCreatedAt());
        assertEquals(updatedAt, interview.getUpdatedAt());
        assertEquals("John Doe", interview.getCandidateName());
        assertEquals("Software Engineer", interview.getJobTitle());
        assertEquals("Tech Corp", interview.getCompanyName());
    }

    @Test
    void testInterviewTypeEnum() {
        Interview.InterviewType[] types = Interview.InterviewType.values();
        assertEquals(4, types.length);
        
        assertEquals("En personne", Interview.InterviewType.IN_PERSON.getDisplayName());
        assertEquals("Vidéo", Interview.InterviewType.VIDEO.getDisplayName());
        assertEquals("Téléphone", Interview.InterviewType.PHONE.getDisplayName());
        assertEquals("En ligne", Interview.InterviewType.ONLINE.getDisplayName());
    }

    @Test
    void testInterviewStatusEnum() {
        Interview.InterviewStatus[] statuses = Interview.InterviewStatus.values();
        assertEquals(4, statuses.length);
        
        assertEquals("Planifié", Interview.InterviewStatus.SCHEDULED.getDisplayName());
        assertEquals("Terminé", Interview.InterviewStatus.COMPLETED.getDisplayName());
        assertEquals("Annulé", Interview.InterviewStatus.CANCELLED.getDisplayName());
        assertEquals("Reprogrammé", Interview.InterviewStatus.RESCHEDULED.getDisplayName());
    }

    @Test
    void testEquals() {
        Interview interview1 = new Interview();
        interview1.setId(1);
        
        Interview interview2 = new Interview();
        interview2.setId(1);
        
        Interview interview3 = new Interview();
        interview3.setId(2);
        
        assertEquals(interview1, interview2);
        assertNotEquals(interview1, interview3);
        assertEquals(interview1, interview1);
        assertNotEquals(interview1, null);
        assertNotEquals(interview1, "not an interview");
    }

    @Test
    void testHashCode() {
        Interview interview1 = new Interview();
        interview1.setId(1);
        
        Interview interview2 = new Interview();
        interview2.setId(1);
        
        assertEquals(interview1.hashCode(), interview2.hashCode());
    }

    @Test
    void testToString() {
        interview.setId(1);
        interview.setApplicationId(10);
        LocalDateTime interviewDate = LocalDateTime.of(2024, 2, 15, 14, 30);
        interview.setInterviewDate(interviewDate);
        interview.setLocation("Office A");
        interview.setInterviewType(Interview.InterviewType.VIDEO);
        interview.setStatus(Interview.InterviewStatus.SCHEDULED);
        
        String result = interview.toString();
        assertTrue(result.contains("Interview"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("applicationId=10"));
        assertTrue(result.contains("interviewType=VIDEO"));
        assertTrue(result.contains("status=SCHEDULED"));
    }
}

