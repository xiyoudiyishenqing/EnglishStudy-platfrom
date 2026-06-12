USE english_study_platform;

ALTER TABLE resources
    ADD COLUMN file_name VARCHAR(255) NULL AFTER type,
    ADD COLUMN stored_name VARCHAR(255) NULL AFTER file_name,
    ADD COLUMN content_type VARCHAR(100) NULL AFTER stored_name,
    ADD COLUMN file_size BIGINT NULL AFTER content_type;
