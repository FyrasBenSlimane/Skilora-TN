package com.skilora.model.entity.recruitment;

import com.skilora.model.enums.WorkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JobPreference entity
 */
class JobPreferenceTest {

    private JobPreference jobPreference;

    @BeforeEach
    void setUp() {
        jobPreference = new JobPreference();
    }

    @Test
    void testDefaultConstructor() {
        assertNotNull(jobPreference);
        assertEquals(WorkType.FULL_TIME, jobPreference.getWorkType());
        assertFalse(jobPreference.isRemoteWork());
    }

    @Test
    void testConstructorWithProfileId() {
        JobPreference pref = new JobPreference(1);
        assertEquals(1, pref.getProfileId());
        assertEquals(WorkType.FULL_TIME, pref.getWorkType());
        assertFalse(pref.isRemoteWork());
    }

    @Test
    void testConstructorWithEssentialFields() {
        JobPreference pref = new JobPreference(1, "Software Engineer", WorkType.PART_TIME);
        assertEquals(1, pref.getProfileId());
        assertEquals("Software Engineer", pref.getDesiredPosition());
        assertEquals(WorkType.PART_TIME, pref.getWorkType());
    }

    @Test
    void testConstructorWithAllFields() {
        JobPreference pref = new JobPreference(1, "Software Engineer", 2000.0, 3000.0,
                WorkType.CONTRACT, "Tunis", true);
        
        assertEquals(1, pref.getProfileId());
        assertEquals("Software Engineer", pref.getDesiredPosition());
        assertEquals(2000.0, pref.getMinSalary());
        assertEquals(3000.0, pref.getMaxSalary());
        assertEquals(WorkType.CONTRACT, pref.getWorkType());
        assertEquals("Tunis", pref.getLocationPreference());
        assertTrue(pref.isRemoteWork());
    }

    @Test
    void testGettersAndSetters() {
        jobPreference.setId(1);
        jobPreference.setProfileId(10);
        jobPreference.setDesiredPosition("Senior Developer");
        jobPreference.setMinSalary(3000.0);
        jobPreference.setMaxSalary(5000.0);
        jobPreference.setWorkType(WorkType.FREELANCE);
        jobPreference.setLocationPreference("Sfax");
        jobPreference.setRemoteWork(true);

        assertEquals(1, jobPreference.getId());
        assertEquals(10, jobPreference.getProfileId());
        assertEquals("Senior Developer", jobPreference.getDesiredPosition());
        assertEquals(3000.0, jobPreference.getMinSalary());
        assertEquals(5000.0, jobPreference.getMaxSalary());
        assertEquals(WorkType.FREELANCE, jobPreference.getWorkType());
        assertEquals("Sfax", jobPreference.getLocationPreference());
        assertTrue(jobPreference.isRemoteWork());
    }

    @Test
    void testGetExpectedSalaryRange() {
        jobPreference.setMinSalary(2000.0);
        jobPreference.setMaxSalary(3000.0);
        assertEquals("2000 - 3000", jobPreference.getExpectedSalaryRange());

        jobPreference.setMinSalary(2000.0);
        jobPreference.setMaxSalary(0.0);
        assertEquals("From 2000", jobPreference.getExpectedSalaryRange());

        jobPreference.setMinSalary(0.0);
        jobPreference.setMaxSalary(3000.0);
        assertEquals("Up to 3000", jobPreference.getExpectedSalaryRange());

        jobPreference.setMinSalary(0.0);
        jobPreference.setMaxSalary(0.0);
        assertEquals("Negotiable", jobPreference.getExpectedSalaryRange());
    }

    @Test
    void testWorkTypeValues() {
        jobPreference.setWorkType(WorkType.FULL_TIME);
        assertEquals(WorkType.FULL_TIME, jobPreference.getWorkType());

        jobPreference.setWorkType(WorkType.PART_TIME);
        assertEquals(WorkType.PART_TIME, jobPreference.getWorkType());

        jobPreference.setWorkType(WorkType.CONTRACT);
        assertEquals(WorkType.CONTRACT, jobPreference.getWorkType());

        jobPreference.setWorkType(WorkType.FREELANCE);
        assertEquals(WorkType.FREELANCE, jobPreference.getWorkType());
    }

    @Test
    void testRemoteWork() {
        jobPreference.setRemoteWork(true);
        assertTrue(jobPreference.isRemoteWork());

        jobPreference.setRemoteWork(false);
        assertFalse(jobPreference.isRemoteWork());
    }

    @Test
    void testEquals() {
        JobPreference pref1 = new JobPreference();
        pref1.setId(1);
        
        JobPreference pref2 = new JobPreference();
        pref2.setId(1);
        
        JobPreference pref3 = new JobPreference();
        pref3.setId(2);
        
        assertEquals(pref1, pref2);
        assertNotEquals(pref1, pref3);
        assertEquals(pref1, pref1);
        assertNotEquals(pref1, null);
        assertNotEquals(pref1, "not a job preference");
    }

    @Test
    void testHashCode() {
        JobPreference pref1 = new JobPreference();
        pref1.setId(1);
        
        JobPreference pref2 = new JobPreference();
        pref2.setId(1);
        
        assertEquals(pref1.hashCode(), pref2.hashCode());
    }

    @Test
    void testToString() {
        jobPreference.setId(1);
        jobPreference.setProfileId(10);
        jobPreference.setDesiredPosition("Software Engineer");
        jobPreference.setMinSalary(2000.0);
        jobPreference.setMaxSalary(3000.0);
        jobPreference.setWorkType(WorkType.FULL_TIME);
        jobPreference.setLocationPreference("Tunis");
        jobPreference.setRemoteWork(false);
        
        String result = jobPreference.toString();
        assertTrue(result.contains("JobPreference"));
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("profileId=10"));
        assertTrue(result.contains("desiredPosition='Software Engineer'"));
        assertTrue(result.contains("workType=FULL_TIME"));
    }
}

