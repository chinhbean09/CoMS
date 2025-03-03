-- V1__init.sql
-- Migration script tạo các bảng và ràng buộc theo thiết kế Entity

---------------------------------------------------------------------
-- Table: role
---------------------------------------------------------------------
CREATE TABLE roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(255)
);

---------------------------------------------------------------------
-- Table: users (sử dụng "users" thay vì "user" vì "user" là keyword của PostgreSQL)
---------------------------------------------------------------------
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(100),
    phone_number VARCHAR(10),
    address VARCHAR(200),
    email VARCHAR(255),
    password VARCHAR(200) NOT NULL,
    is_active BOOLEAN,
    role_id BIGINT,
    modified_by VARCHAR(255),
    facebook_account_id VARCHAR(255),
    google_account_id VARCHAR(255),
    avatar VARCHAR(255),
    is_ceo BOOLEAN DEFAULT FALSE,
    failed_login_attempts INTEGER,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(role_id)
);

---------------------------------------------------------------------
-- Table: contract_type
---------------------------------------------------------------------
CREATE TABLE contract_types (
    contract_type_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    isDeleted BOOLEAN NOT NULL
);

---------------------------------------------------------------------
-- Table: party
---------------------------------------------------------------------
CREATE TABLE parties (
    party_id SERIAL PRIMARY KEY,
    partner_code VARCHAR(255),
    party_type VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    spokesman_name VARCHAR(100),
    address VARCHAR(300),
    tax_code VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    note VARCHAR(1000),
    is_deleted BOOLEAN NOT NULL
);

---------------------------------------------------------------------
-- Table: bank
---------------------------------------------------------------------
CREATE TABLE banks (
    id SERIAL PRIMARY KEY,
    bank_name VARCHAR(255) NOT NULL,
    branch_name VARCHAR(255) NOT NULL,
    party_id INTEGER,
    CONSTRAINT fk_bank_party FOREIGN KEY (party_id) REFERENCES parties(party_id)
);

---------------------------------------------------------------------
-- Table: type_term
---------------------------------------------------------------------
CREATE TABLE type_term (
    type_term_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    identifier VARCHAR(255) NOT NULL
);

---------------------------------------------------------------------
-- Table: term
---------------------------------------------------------------------
CREATE TABLE terms (
    term_id SERIAL PRIMARY KEY,
    label TEXT NOT NULL UNIQUE,
    value TEXT NOT NULL,
    clause_code VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    type_term_id INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    CONSTRAINT fk_term_type_term FOREIGN KEY (type_term_id) REFERENCES type_term(type_term_id)
);

---------------------------------------------------------------------
-- Table: contract_template
---------------------------------------------------------------------
CREATE TABLE contract_templates (
    template_id SERIAL PRIMARY KEY,
    contract_title VARCHAR(200) NOT NULL,
    party_info TEXT,
    special_termsA TEXT,
    special_termsB TEXT,
    appendix_enabled BOOLEAN DEFAULT FALSE,
    transfer_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    violate BOOLEAN DEFAULT FALSE,
    suspend BOOLEAN DEFAULT FALSE,
    suspend_content VARCHAR(500),
    contract_content TEXT,
    auto_add_vat BOOLEAN DEFAULT FALSE,
    vat_percentage INTEGER,
    is_date_late_checked BOOLEAN DEFAULT FALSE,
    max_date_late INTEGER,
    auto_renew BOOLEAN DEFAULT FALSE,
    contract_type_id INTEGER NOT NULL,
    original_template_id INTEGER,
    duplicate_version INTEGER,
    CONSTRAINT fk_contract_template_contract_type FOREIGN KEY (contract_type_id) REFERENCES contract_types(contract_type_id)
);

---------------------------------------------------------------------
-- Join table: contract_template_legal_basis
---------------------------------------------------------------------
CREATE TABLE contract_template_legal_basis (
    template_id INTEGER NOT NULL,
    term_id INTEGER NOT NULL,
    PRIMARY KEY (template_id, term_id),
    CONSTRAINT fk_ctlb_template FOREIGN KEY (template_id) REFERENCES contract_templates(template_id),
    CONSTRAINT fk_ctlb_term FOREIGN KEY (term_id) REFERENCES terms(term_id)
);

---------------------------------------------------------------------
-- Join table: contract_template_general_terms
---------------------------------------------------------------------
CREATE TABLE contract_template_general_terms (
    template_id INTEGER NOT NULL,
    term_id INTEGER NOT NULL,
    PRIMARY KEY (template_id, term_id),
    CONSTRAINT fk_ctgt_template FOREIGN KEY (template_id) REFERENCES contract_templates(template_id),
    CONSTRAINT fk_ctgt_term FOREIGN KEY (term_id) REFERENCES terms(term_id)
);

---------------------------------------------------------------------
-- Join table: contract_template_other_terms
---------------------------------------------------------------------
CREATE TABLE contract_template_other_terms (
    template_id INTEGER NOT NULL,
    term_id INTEGER NOT NULL,
    PRIMARY KEY (template_id, term_id),
    CONSTRAINT fk_ctot_template FOREIGN KEY (template_id) REFERENCES contract_templates(template_id),
    CONSTRAINT fk_ctot_term FOREIGN KEY (term_id) REFERENCES terms(term_id)
);

---------------------------------------------------------------------
-- Table: contract_template_additional_term_detail
---------------------------------------------------------------------
CREATE TABLE contract_template_additional_term_details (
    additional_term_id SERIAL PRIMARY KEY,
    template_id INTEGER NOT NULL,
    type_term_id INTEGER NOT NULL,
    CONSTRAINT fk_ctatd_template FOREIGN KEY (template_id) REFERENCES contract_templates(template_id)
);

---------------------------------------------------------------------
-- Element collection cho ContractTemplateAdditionalTermDetail
---------------------------------------------------------------------
CREATE TABLE ct_additional_common (
    additional_term_id INTEGER NOT NULL,
    term_id INTEGER,
    PRIMARY KEY (additional_term_id, term_id),
    CONSTRAINT fk_ctac_additional_term FOREIGN KEY (additional_term_id) REFERENCES contract_template_additional_term_details(additional_term_id)
);

CREATE TABLE ct_additional_a (
    additional_term_id INTEGER NOT NULL,
    term_id INTEGER,
    PRIMARY KEY (additional_term_id, term_id),
    CONSTRAINT fk_ctaa_additional_term FOREIGN KEY (additional_term_id) REFERENCES contract_template_additional_term_details(additional_term_id)
);

CREATE TABLE ct_additional_b (
    additional_term_id INTEGER NOT NULL,
    term_id INTEGER,
    PRIMARY KEY (additional_term_id, term_id),
    CONSTRAINT fk_ctab_additional_term FOREIGN KEY (additional_term_id) REFERENCES contract_template_additional_term_details(additional_term_id)
);

---------------------------------------------------------------------
-- Table: contract
---------------------------------------------------------------------
CREATE TABLE contracts (
    contract_id SERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    contract_number VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    special_terms_a TEXT,
    special_terms_b TEXT,
    status VARCHAR(50) NOT NULL,
    start_date TIMESTAMP,
    created_by VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    scope VARCHAR(500),
    configuration VARCHAR(500),
    sla VARCHAR(500),
    confidentiality VARCHAR(500),
    obligations VARCHAR(500),
    amount DOUBLE PRECISION,
    user_id INTEGER NOT NULL,
    is_date_late_checked BOOLEAN DEFAULT FALSE,
    max_date_late INTEGER,
    template_id INTEGER,
    party_id INTEGER NOT NULL,
    appendix_enabled BOOLEAN DEFAULT FALSE,
    transfer_enabled BOOLEAN DEFAULT FALSE,
    auto_add_vat BOOLEAN DEFAULT FALSE,
    vat_percentage INTEGER,
    auto_renew BOOLEAN DEFAULT FALSE,
    contract_content TEXT,
    CONSTRAINT fk_contract_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_contract_template FOREIGN KEY (template_id) REFERENCES contract_templates(template_id),
    CONSTRAINT fk_contract_party FOREIGN KEY (party_id) REFERENCES parties(party_id)
);

---------------------------------------------------------------------
-- Table: audit_trail
---------------------------------------------------------------------
CREATE TABLE audit_trail (
    id SERIAL PRIMARY KEY,
    contract_id INTEGER NOT NULL,
    changed_by VARCHAR(100) NOT NULL,
    change_timestamp TIMESTAMP NOT NULL,
    change_summary VARCHAR(1000),
    old_value TEXT,
    new_value TEXT,
    CONSTRAINT fk_audit_trail_contract FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
);

---------------------------------------------------------------------
-- Table: contract_additional_term_detail
---------------------------------------------------------------------
CREATE TABLE contract_additional_term_details (
    additional_term_id SERIAL PRIMARY KEY,
    contract_id INTEGER NOT NULL,
    type_term_id INTEGER NOT NULL,
    CONSTRAINT fk_catd_contract FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
);

---------------------------------------------------------------------
-- Element collection cho ContractAdditionalTermDetail
---------------------------------------------------------------------
CREATE TABLE contract_ct_additional_common (
    additional_term_id INTEGER NOT NULL,
    snapshot TEXT,
    PRIMARY KEY (additional_term_id, snapshot),
    CONSTRAINT fk_ccac_additional_term FOREIGN KEY (additional_term_id) REFERENCES contract_additional_term_details(additional_term_id)
);

CREATE TABLE contract_ct_additional_a (
    additional_term_id INTEGER NOT NULL,
    snapshot TEXT,
    PRIMARY KEY (additional_term_id, snapshot),
    CONSTRAINT fk_ccaa_additional_term FOREIGN KEY (additional_term_id) REFERENCES contract_additional_term_details(additional_term_id)
);

CREATE TABLE contract_ct_additional_b (
    additional_term_id INTEGER NOT NULL,
    snapshot TEXT,
    PRIMARY KEY (additional_term_id, snapshot),
    CONSTRAINT fk_ccab_additional_term FOREIGN KEY (additional_term_id) REFERENCES contract_additional_term_details(additional_term_id)
);

---------------------------------------------------------------------
-- Table: contract_term
---------------------------------------------------------------------
CREATE TABLE contract_terms_snapshot (
    id SERIAL PRIMARY KEY,
    term_label_snapshot VARCHAR(200),
    term_value_snapshot TEXT,
    term_type VARCHAR(50) NOT NULL,
    original_term_id INTEGER,
    contract_id INTEGER NOT NULL,
    CONSTRAINT fk_contract_term_contract FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
);

---------------------------------------------------------------------
-- Table: forgot_password
---------------------------------------------------------------------
CREATE TABLE forgot_passwords (
    id SERIAL PRIMARY KEY,
    otp INTEGER NOT NULL,
    expiration_date TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL,
    reset_token VARCHAR(255),
    otp_attempts INTEGER,
    user_id INTEGER NOT NULL,
    CONSTRAINT fk_forgot_password_user FOREIGN KEY (user_id) REFERENCES users(id)
);

---------------------------------------------------------------------
-- Table: notification
---------------------------------------------------------------------
CREATE TABLE notifications (
    id SERIAL PRIMARY KEY,
    message TEXT NOT NULL,
    is_read BOOLEAN,
    created_at TIMESTAMP NOT NULL,
    user_id INTEGER NOT NULL,
    contract_id SERIAL,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id)
);

---------------------------------------------------------------------
-- Table: payment_one_time
---------------------------------------------------------------------
CREATE TABLE payment_one_time (
    payment_id SERIAL PRIMARY KEY,
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL,
    due_date TIMESTAMP NOT NULL,
    status VARCHAR(50),
    contract_id INTEGER NOT NULL UNIQUE,
    CONSTRAINT fk_payment_one_time_contract FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
);

---------------------------------------------------------------------
-- Table: payment_schedule
---------------------------------------------------------------------
CREATE TABLE payment_schedules (
    payment_id SERIAL PRIMARY KEY,
    payment_order INTEGER NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL,
    due_date TIMESTAMP NOT NULL,
    status VARCHAR(50),
    description VARCHAR(500),
    reminder_email_sent BOOLEAN,
    overdue_email_sent BOOLEAN,
    contract_id INTEGER NOT NULL,
    CONSTRAINT fk_payment_schedule_contract FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
);

---------------------------------------------------------------------
-- Table: report
---------------------------------------------------------------------
CREATE TABLE reports (
    report_id SERIAL PRIMARY KEY,
    contract_id INTEGER NOT NULL,
    report_type VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    create_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_report_contract FOREIGN KEY (contract_id) REFERENCES contracts(contract_id)
);

---------------------------------------------------------------------
-- Table: task
---------------------------------------------------------------------
--CREATE TABLE tasks (
--    id SERIAL PRIMARY KEY,
--    task_name VARCHAR(255) NOT NULL,
--    description VARCHAR(500) NOT NULL,
--    created_by INTEGER NOT NULL,
--    assigned_to INTEGER NOT NULL,
--    created_at TIMESTAMP NOT NULL,
--    due_date DATE NOT NULL,
--    task_status VARCHAR(50) NOT NULL,
--    last_viewed_at TIMESTAMP,
--    last_viewed_by VARCHAR(255),
--    updated_at TIMESTAMP,
--    CONSTRAINT fk_task_created_by FOREIGN KEY (created_by) REFERENCES users(id),
--    CONSTRAINT fk_task_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(id)
--);

---------------------------------------------------------------------
-- Join table: task_supervisors (Many-to-Many giữa task và users)
---------------------------------------------------------------------
--CREATE TABLE task_supervisors (
--    task_id INTEGER NOT NULL,
--    user_id INTEGER NOT NULL,
--    PRIMARY KEY (task_id, user_id),
--    CONSTRAINT fk_task_supervisors_task FOREIGN KEY (task_id) REFERENCES tasks(id),
--    CONSTRAINT fk_task_supervisors_user FOREIGN KEY (user_id) REFERENCES users(id)
--);

---------------------------------------------------------------------
-- Table: token
---------------------------------------------------------------------
CREATE TABLE tokens (
    id SERIAL PRIMARY KEY,
    token VARCHAR(1000),
    refresh_token VARCHAR(255),
    token_type VARCHAR(50),
    expiration_date TIMESTAMP,
    refresh_expiration_date TIMESTAMP,
    is_mobile BOOLEAN,
    revoked BOOLEAN,
    expired BOOLEAN,
    user_id INTEGER,
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);

---------------------------------------------------------------------
-- Table: workflow
---------------------------------------------------------------------
CREATE TABLE workflows (
    workflow_id SERIAL PRIMARY KEY,
    contract_id INTEGER NOT NULL,
    updated_by INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    comment VARCHAR(500),
    create_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_workflow_contract FOREIGN KEY (contract_id) REFERENCES contracts(contract_id),
    CONSTRAINT fk_workflow_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);
