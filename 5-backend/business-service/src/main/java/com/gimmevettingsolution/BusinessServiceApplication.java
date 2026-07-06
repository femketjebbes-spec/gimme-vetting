package com.gimmevettingsolution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the Business Service.
 * This service handles backend logic specific to the Case Analyst-facing application.
 */
@SpringBootApplication
public class BusinessServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessServiceApplication.class, args);
    }
}
