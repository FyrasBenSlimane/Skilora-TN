package com.skilora.model.entity.recruitment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JobOpportunity entity
 */
class JobOpportunityTest {

    private JobOpportunity jobOpportunity;

    @BeforeEach
    void setUp() {
        jobOpportunity = new JobOpportunity();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(jobOpportunity);
    }

    @Test
    void testGettersAndSetters() {
        jobOpportunity.setSource("LinkedIn");
        jobOpportunity.setTitle("Software Engineer");
        jobOpportunity.setUrl("https://linkedin.com/jobs/123");
        jobOpportunity.setDescription("We are looking for a software engineer");
        jobOpportunity.setLocation("Tunis");
        jobOpportunity.setPostedDate("2024-01-15");
        jobOpportunity.setRawId("linkedin_123");
        jobOpportunity.setId(1);
        jobOpportunity.setCompany("Tech Corp");
        jobOpportunity.setType("Full-time");

        assertEquals("LinkedIn", jobOpportunity.getSource());
        assertEquals("Software Engineer", jobOpportunity.getTitle());
        assertEquals("https://linkedin.com/jobs/123", jobOpportunity.getUrl());
        assertEquals("We are looking for a software engineer", jobOpportunity.getDescription());
        assertEquals("Tunis", jobOpportunity.getLocation());
        assertEquals("2024-01-15", jobOpportunity.getPostedDate());
        assertEquals("linkedin_123", jobOpportunity.getRawId());
        assertEquals(1, jobOpportunity.getId());
        assertEquals("Tech Corp", jobOpportunity.getCompany());
        assertEquals("Full-time", jobOpportunity.getType());
    }

    @Test
    void testNullValues() {
        jobOpportunity.setSource(null);
        jobOpportunity.setTitle(null);
        jobOpportunity.setUrl(null);
        jobOpportunity.setDescription(null);
        jobOpportunity.setLocation(null);
        jobOpportunity.setPostedDate(null);
        jobOpportunity.setRawId(null);
        jobOpportunity.setCompany(null);
        jobOpportunity.setType(null);

        assertNull(jobOpportunity.getSource());
        assertNull(jobOpportunity.getTitle());
        assertNull(jobOpportunity.getUrl());
        assertNull(jobOpportunity.getDescription());
        assertNull(jobOpportunity.getLocation());
        assertNull(jobOpportunity.getPostedDate());
        assertNull(jobOpportunity.getRawId());
        assertNull(jobOpportunity.getCompany());
        assertNull(jobOpportunity.getType());
    }

    @Test
    void testIdField() {
        jobOpportunity.setId(0);
        assertEquals(0, jobOpportunity.getId());

        jobOpportunity.setId(100);
        assertEquals(100, jobOpportunity.getId());

        jobOpportunity.setId(-1);
        assertEquals(-1, jobOpportunity.getId());
    }

    @Test
    void testEmptyStrings() {
        jobOpportunity.setSource("");
        jobOpportunity.setTitle("");
        jobOpportunity.setUrl("");
        jobOpportunity.setDescription("");
        jobOpportunity.setLocation("");
        jobOpportunity.setPostedDate("");
        jobOpportunity.setRawId("");
        jobOpportunity.setCompany("");
        jobOpportunity.setType("");

        assertEquals("", jobOpportunity.getSource());
        assertEquals("", jobOpportunity.getTitle());
        assertEquals("", jobOpportunity.getUrl());
        assertEquals("", jobOpportunity.getDescription());
        assertEquals("", jobOpportunity.getLocation());
        assertEquals("", jobOpportunity.getPostedDate());
        assertEquals("", jobOpportunity.getRawId());
        assertEquals("", jobOpportunity.getCompany());
        assertEquals("", jobOpportunity.getType());
    }

    @Test
    void testLongStrings() {
        String longString = "A".repeat(1000);
        jobOpportunity.setTitle(longString);
        jobOpportunity.setDescription(longString);
        jobOpportunity.setUrl(longString);

        assertEquals(longString, jobOpportunity.getTitle());
        assertEquals(longString, jobOpportunity.getDescription());
        assertEquals(longString, jobOpportunity.getUrl());
    }
}

