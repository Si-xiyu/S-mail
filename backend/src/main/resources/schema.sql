-- Drop existing tables to ensure clean schema
DROP TABLE IF EXISTS ai_analysis_task;
DROP TABLE IF EXISTS mail_ai_result;
DROP TABLE IF EXISTS mail_category_assignment;
DROP TABLE IF EXISTS mail_category;
DROP TABLE IF EXISTS mail_attachment;
DROP TABLE IF EXISTS pending_attachment;
DROP TABLE IF EXISTS mailbox_item;
DROP TABLE IF EXISTS mail_recipient;
DROP TABLE IF EXISTS mail_message;
DROP TABLE IF EXISTS user_setting;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(128) NOT NULL UNIQUE,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    ai_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    agent_auto_write_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mail_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_no VARCHAR(64) NOT NULL UNIQUE,
    sender_id BIGINT NOT NULL,
    sender_email VARCHAR(128) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    content_text TEXT,
    content_html TEXT,
    has_attachment BOOLEAN NOT NULL DEFAULT FALSE,
    thread_id BIGINT,
    parent_mail_id BIGINT,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mail_recipient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_id BIGINT NOT NULL,
    recipient_id BIGINT,
    recipient_email VARCHAR(128) NOT NULL,
    recipient_type VARCHAR(10) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mailbox_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mail_id BIGINT NOT NULL,
    folder VARCHAR(20) NOT NULL,
    original_folder VARCHAR(20),
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    star_flag BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_flag BOOLEAN NOT NULL DEFAULT FALSE,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pending_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uploader_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(128),
    file_size BIGINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(128),
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADED',
    bound_mail_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mail_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_id BIGINT,
    uploader_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(128),
    file_size BIGINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mail_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    color VARCHAR(20) NOT NULL DEFAULT '#64748b',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mail_category_assignment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    mail_id BIGINT NOT NULL,
    assignment_source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mail_ai_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mail_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    result_type VARCHAR(40) NOT NULL,
    result_json TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ai_analysis_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    mail_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    task_type VARCHAR(40) NOT NULL DEFAULT 'FULL_ANALYSIS',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mailbox_user_folder ON mailbox_item(user_id, folder, deleted_flag);
CREATE INDEX idx_mailbox_user_received ON mailbox_item(user_id, received_at, deleted_flag);
CREATE INDEX idx_recipient_mail ON mail_recipient(mail_id);
CREATE INDEX idx_mail_thread ON mail_message(thread_id);
CREATE INDEX idx_mail_parent ON mail_message(parent_mail_id);
CREATE INDEX idx_ai_mail_user_type ON mail_ai_result(mail_id, user_id, result_type);
CREATE INDEX idx_pending_uploader ON pending_attachment(uploader_id, status);
CREATE INDEX idx_attachment_mail ON mail_attachment(mail_id);
CREATE INDEX idx_category_user ON mail_category(user_id);
CREATE INDEX idx_category_user_name ON mail_category(user_id, name);
CREATE INDEX idx_assignment_user_mail ON mail_category_assignment(user_id, mail_id);
CREATE INDEX idx_assignment_category ON mail_category_assignment(category_id);
CREATE UNIQUE INDEX idx_assignment_unique ON mail_category_assignment(user_id, mail_id);
CREATE INDEX idx_analysis_task_pending ON ai_analysis_task(status, created_at);
CREATE INDEX idx_analysis_task_item ON ai_analysis_task(item_id, user_id, status);
CREATE INDEX idx_analysis_task_mail_user ON ai_analysis_task(mail_id, user_id);
