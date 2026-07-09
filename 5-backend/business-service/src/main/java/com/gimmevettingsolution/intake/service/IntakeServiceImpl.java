package com.gimmevettingsolution.intake.service;

import com.gimmevettingsolution.intake.dto.IntakeRequest;
import com.gimmevettingsolution.intake.dto.PoCVerifiedResponse;
import com.gimmevettingsolution.invoice.entity.Invoice;
import com.gimmevettingsolution.invoice.repository.InvoiceRepository;
import com.gimmevettingsolution.poc.PoCStoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of IntakeService that performs PoC verification,
 * validates request fields, and persists the Invoice entity.
 */
@Service
public class IntakeServiceImpl implements IntakeService {

    private static final String SAFE_PATTERN = "^[A-Za-z0-9\\-_.]+$";

    private final PoCStoreService pocStoreService;
    private final InvoiceRepository invoiceRepository;

    public IntakeServiceImpl(PoCStoreService pocStoreService, InvoiceRepository invoiceRepository) {
        this.pocStoreService = pocStoreService;
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    @Transactional
    public PoCVerifiedResponse process(IntakeRequest request) throws RejectedTypeAException, ValidationException {
        validateRequest(request);

        String invoiceNumber = request.getInvoiceNumber();

        if (!invoiceNumber.matches(SAFE_PATTERN)) {
            throw new ValidationException("invoiceNumber", "contains invalid characters. Only alphanumeric, hyphens, underscores, and periods are allowed.");
        }

        if (pocStoreService.hasMatchingPoC(invoiceNumber)) {
            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber(invoiceNumber);
            invoice.setDebtorName(request.getDebtorName());
            invoice.setAddress(request.getAddress());
            invoice.setBankAccountNumber(request.getBankAccountNumber());
            invoice.setPhoneNumber(request.getPhoneNumber());
            invoice.setPoCStatus("VERIFIED");
            invoice.setRejectionType("NONE");
            invoice.setStatus("QUEUED");
            invoice.setResubmissionCount(0);

            Invoice saved = invoiceRepository.save(invoice);

            String invoiceId = saved.getId().toString();
            return new PoCVerifiedResponse("POC_VERIFIED", "BUSINESS_RULE_CHECK", invoiceId);
        } else {
            throw new RejectedTypeAException(invoiceNumber);
        }
    }

    private void validateRequest(IntakeRequest request) throws ValidationException {
        if (request == null) {
            throw new ValidationException("request", "must not be null");
        }

        if (request.getInvoiceNumber() == null || request.getInvoiceNumber().isEmpty()) {
            throw new ValidationException("invoiceNumber", "must not be empty");
        }

        if (request.getDebtorName() == null || request.getDebtorName().isEmpty()) {
            throw new ValidationException("debtorName", "must not be empty");
        }

        if (request.getAddress() == null || request.getAddress().isEmpty()) {
            throw new ValidationException("address", "must not be empty");
        }

        if (request.getBankAccountNumber() == null || request.getBankAccountNumber().isEmpty()) {
            throw new ValidationException("bankAccountNumber", "must not be empty");
        }

        if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
            throw new ValidationException("phoneNumber", "must not be empty");
        }
    }
}
