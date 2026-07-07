package com.gimmevettingsolution.intake.service;

import com.gimmevettingsolution.intake.dto.IntakeRequest;
import com.gimmevettingsolution.intake.dto.PoCVerifiedResponse;
import com.gimmevettingsolution.intake.dto.RejectedTypeAResponse;

/**
 * Service handling the intake pipeline logic for invoice submissions.
 * <p>
 * NOTE: Authentication is absent for the PoC phase. This endpoint is
 * unauthenticated and should be protected in a future work item.
 */
public interface IntakeService {

    /**
     * Processes an intake request.
     *
     * @param request the intake request
     * @return PoC verified response if PoC exists
     * @throws RejectedTypeAException if no PoC is found
     * @throws ValidationException if request fields are invalid
     */
    PoCVerifiedResponse process(IntakeRequest request) throws RejectedTypeAException, ValidationException;
}
