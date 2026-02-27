package com.skilora.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to generate test/seed data for development or demo.
 */
public class TestDataGenerator {
    private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);

    /** Generate or seed test data (e.g. sample job offers, applications). No-op stub by default. */
    public static void generateTestData() {
        try {
            // Stub: can be implemented to insert sample jobs, applications, etc.
            logger.debug("TestDataGenerator.generateTestData() called (no-op)");
        } catch (Exception e) {
            logger.warn("generateTestData failed: {}", e.getMessage());
        }
    }
}
