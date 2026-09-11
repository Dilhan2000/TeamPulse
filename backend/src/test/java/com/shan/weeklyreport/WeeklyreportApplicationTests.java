package com.shan.weeklyreport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the Spring context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class WeeklyreportApplicationTests {

    @Test
    void contextLoads() {
        // If the Spring context starts, this test passes.
    }
}
