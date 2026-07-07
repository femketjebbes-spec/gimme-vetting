package com.gimmevettingsolution;

import com.gimmevettingsolution.intake.service.IntakeService;
import com.gimmevettingsolution.poc.PoCStoreService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic context test for the Business Service.
 */
@SpringBootTest
@ActiveProfiles("test")
class BusinessServiceApplicationTest {

    @MockBean
    private PoCStoreService pocStoreService;

    @MockBean
    private IntakeService intakeService;

    @Test
    void contextLoads() {
        // Verifies that the Spring context loads successfully
    }
}
