CREATE TABLE IF NOT EXISTS cleanup_owned_table (
    id BIGINT PRIMARY KEY
);

ALTER TABLE shared_cleanup_table
    ADD COLUMN first_flag VARCHAR(20) NULL,
    ADD COLUMN second_flag INT NULL;
