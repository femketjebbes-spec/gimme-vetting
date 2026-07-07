CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(128) NOT NULL UNIQUE,
    debtor_name VARCHAR(256) NOT NULL,
    address VARCHAR(512) NOT NULL,
    bank_account_number VARCHAR(34) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    poc_status VARCHAR(32) NOT NULL,
    rejection_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL
);
