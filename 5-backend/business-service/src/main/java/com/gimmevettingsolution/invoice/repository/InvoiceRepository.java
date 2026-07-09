package com.gimmevettingsolution.invoice.repository;

import com.gimmevettingsolution.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * JPA repository for the Invoice entity.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
}
