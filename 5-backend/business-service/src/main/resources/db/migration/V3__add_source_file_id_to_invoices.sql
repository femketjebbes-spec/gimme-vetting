ALTER TABLE invoices ADD COLUMN source_file_id VARCHAR(64) NULL COMMENT 'UUID of the source Excel file';
ALTER TABLE invoices ADD COLUMN source_filename VARCHAR(256) NULL COMMENT 'Original filename the client uploaded';
