package com.gimmevettingsolution.intake;

import com.gimmevettingsolution.intake.dto.IntakeRequest;
import com.gimmevettingsolution.intake.dto.PoCVerifiedResponse;
import com.gimmevettingsolution.intake.dto.RejectedTypeAResponse;
import com.gimmevettingsolution.intake.dto.ValidationErrorResponse;
import com.gimmevettingsolution.intake.service.IntakeService;
import com.gimmevettingsolution.intake.service.RejectedTypeAException;
import com.gimmevettingsolution.intake.service.ValidationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller handling invoice intake submissions.
 * <p>
 * NOTE: Authentication is absent for the PoC phase. This endpoint is
 * unauthenticated and should be protected in a future work item.
 */
@RestController
@RequestMapping("/api/v1")
public class IntakeController {

    private final IntakeService intakeService;

    public IntakeController(IntakeService intakeService) {
        this.intakeService = intakeService;
    }

    @PostMapping("/intake")
    public ResponseEntity<?> intake(@RequestBody IntakeRequest request) {
        try {
            PoCVerifiedResponse response = intakeService.process(request);
            return ResponseEntity.accepted().body(response);
        } catch (RejectedTypeAException e) {
            RejectedTypeAResponse response = new RejectedTypeAResponse(
                    "REJECTED_TYPE_A",
                    "No PoC linked to invoice " + e.getInvoiceNumber(),
                    true
            );
            return ResponseEntity.badRequest().body(response);
        } catch (ValidationException e) {
            ValidationErrorResponse response = new ValidationErrorResponse(
                    "VALIDATION_ERROR",
                    e.getMessage(),
                    false
            );
            return ResponseEntity.badRequest().body(response);
        }
    }
}
