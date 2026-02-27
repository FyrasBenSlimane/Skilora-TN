package com.skilora.service.recruitment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JobServiceTest {
    @Test
    void getInstance() {
        assertNotNull(JobService.getInstance());
    }
}
