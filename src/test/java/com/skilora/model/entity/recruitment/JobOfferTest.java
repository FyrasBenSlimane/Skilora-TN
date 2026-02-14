package com.skilora.model.entity.recruitment;

import com.skilora.model.enums.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JobOffer entity
 */
class JobOfferTest {

    private JobOffer jobOffer;

    @BeforeEach
    void setUp() {
        jobOffer = new JobOffer();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(jobOffer);
        assertNotNull(jobOffer.getRequiredSkills());
        assertTrue(jobOffer.getRequiredSkills().isEmpty());
        assertEquals(JobStatus.DRAFT, jobOffer.getStatus());
        assertNotNull(jobOffer.getPostedDate());
    }

    @Test
    void testConstructorWithBasicFields() {
        JobOffer offer = new JobOffer(1, "Software Engineer", "Tunis");
        
        assertEquals(1, offer.getEmployerId());
        assertEquals("Software Engineer", offer.getTitle());
        assertEquals("Tunis", offer.getLocation());
        assertNotNull(offer.getRequiredSkills());
        assertEquals(JobStatus.DRAFT, offer.getStatus());
    }

    @Test
    void testConstructorWithAllFields() {
        List<String> skills = Arrays.asList("Java", "Spring", "MySQL");
        JobOffer offer = new JobOffer(1, "Software Engineer", "Description", "Tunis",
                2000.0, 3000.0, skills, JobStatus.ACTIVE);
        
        assertEquals(1, offer.getEmployerId());
        assertEquals("Software Engineer", offer.getTitle());
        assertEquals("Description", offer.getDescription());
        assertEquals("Tunis", offer.getLocation());
        assertEquals(2000.0, offer.getSalaryMin());
        assertEquals(3000.0, offer.getSalaryMax());
        assertEquals(skills, offer.getRequiredSkills());
        assertEquals(JobStatus.ACTIVE, offer.getStatus());
    }

    @Test
    void testConstructorWithNullSkills() {
        JobOffer offer = new JobOffer(1, "Software Engineer", "Description", "Tunis",
                2000.0, 3000.0, null, JobStatus.ACTIVE);
        
        assertNotNull(offer.getRequiredSkills());
        assertTrue(offer.getRequiredSkills().isEmpty());
    }

    @Test
    void testGettersAndSetters() {
        jobOffer.setId(1);
        jobOffer.setEmployerId(10);
        jobOffer.setTitle("Senior Developer");
        jobOffer.setDescription("We are looking for a senior developer");
        jobOffer.setLocation("Sfax");
        jobOffer.setSalaryMin(3000.0);
        jobOffer.setSalaryMax(5000.0);
        jobOffer.setCurrency("TND");
        jobOffer.setWorkType("Full-time");
        List<String> skills = Arrays.asList("Java", "Spring Boot");
        jobOffer.setRequiredSkills(skills);
        jobOffer.setStatus(JobStatus.ACTIVE);
        LocalDate postedDate = LocalDate.of(2024, 1, 15);
        jobOffer.setPostedDate(postedDate);
        LocalDate deadline = LocalDate.of(2024, 2, 15);
        jobOffer.setDeadline(deadline);
        jobOffer.setCompanyName("Tech Solutions");

        assertEquals(1, jobOffer.getId());
        assertEquals(10, jobOffer.getEmployerId());
        assertEquals("Senior Developer", jobOffer.getTitle());
        assertEquals("We are looking for a senior developer", jobOffer.getDescription());
        assertEquals("Sfax", jobOffer.getLocation());
        assertEquals(3000.0, jobOffer.getSalaryMin());
        assertEquals(5000.0, jobOffer.getSalaryMax());
        assertEquals("TND", jobOffer.getCurrency());
        assertEquals("Full-time", jobOffer.getWorkType());
        assertEquals(skills, jobOffer.getRequiredSkills());
        assertEquals(JobStatus.ACTIVE, jobOffer.getStatus());
        assertEquals(postedDate, jobOffer.getPostedDate());
        assertEquals(deadline, jobOffer.getDeadline());
        assertEquals("Tech Solutions", jobOffer.getCompanyName());
    }

    @Test
    void testGetSalaryRange() {
        jobOffer.setSalaryMin(2000.0);
        jobOffer.setSalaryMax(3000.0);
        assertEquals("2000 - 3000", jobOffer.getSalaryRange());

        jobOffer.setSalaryMin(2000.0);
        jobOffer.setSalaryMax(0.0);
        assertEquals("From 2000", jobOffer.getSalaryRange());

        jobOffer.setSalaryMin(0.0);
        jobOffer.setSalaryMax(3000.0);
        assertEquals("Up to 3000", jobOffer.getSalaryRange());

        jobOffer.setSalaryMin(0.0);
        jobOffer.setSalaryMax(0.0);
        assertEquals("Negotiable", jobOffer.getSalaryRange());
    }

    @Test
    void testIsActive() {
        jobOffer.setStatus(JobStatus.ACTIVE);
        assertTrue(jobOffer.isActive());

        jobOffer.setStatus(JobStatus.DRAFT);
        assertFalse(jobOffer.isActive());

        jobOffer.setStatus(JobStatus.CLOSED);
        assertFalse(jobOffer.isActive());
    }

    @Test
    void testAddRequiredSkill() {
        jobOffer.addRequiredSkill("Java");
        assertTrue(jobOffer.getRequiredSkills().contains("Java"));
        assertEquals(1, jobOffer.getRequiredSkills().size());

        jobOffer.addRequiredSkill("Spring");
        assertEquals(2, jobOffer.getRequiredSkills().size());
        assertTrue(jobOffer.getRequiredSkills().contains("Spring"));

        jobOffer.addRequiredSkill("Java");
        assertEquals(2, jobOffer.getRequiredSkills().size());
    }

    @Test
    void testAddRequiredSkillWithNullList() {
        jobOffer.setRequiredSkills(null);
        jobOffer.addRequiredSkill("Java");
        assertNotNull(jobOffer.getRequiredSkills());
        assertTrue(jobOffer.getRequiredSkills().contains("Java"));
    }

    @Test
    void testEquals() {
        JobOffer offer1 = new JobOffer();
        offer1.setId(1);
        
        JobOffer offer2 = new JobOffer();
        offer2.setId(1);
        
        JobOffer offer3 = new JobOffer();
        offer3.setId(2);
        
        assertEquals(offer1, offer2);
        assertNotEquals(offer1, offer3);
        assertEquals(offer1, offer1);
        assertNotEquals(offer1, null);
        assertNotEquals(offer1, "not a job offer");
    }

    @Test
    void testHashCode() {
        JobOffer offer1 = new JobOffer();
        offer1.setId(1);
        
        JobOffer offer2 = new JobOffer();
        offer2.setId(1);
        
        assertEquals(offer1.hashCode(), offer2.hashCode());
    }

    @Test
    void testToString() {
        jobOffer.setId(1);
        jobOffer.setEmployerId(10);
        jobOffer.setTitle("Software Engineer");
        jobOffer.setLocation("Tunis");
        jobOffer.setSalaryMin(2000.0);
        jobOffer.setSalaryMax(3000.0);
        jobOffer.setStatus(JobStatus.ACTIVE);
        jobOffer.setRequiredSkills(Arrays.asList("Java", "Spring"));
        
        String result = jobOffer.toString();
        assertTrue(result.contains("JobOffer"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("employerId=10"));
        assertTrue(result.contains("title='Software Engineer'"));
        assertTrue(result.contains("location='Tunis'"));
    }
}

