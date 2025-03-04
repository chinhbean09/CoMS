-- V1__add_approval_workflow_tables.sql
-- Migration script tạo các bảng và ràng buộc theo thiết kế Entity

---------------------------------------------------------------------

-- Tạo bảng approval_workflows

CREATE TABLE approval_workflows (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    custom_stages_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL
);

---------------------------------------------------------------------
-- Table: approval_stages
---------------------------------------------------------------------
CREATE TABLE approval_stages (
    id SERIAL PRIMARY KEY,
    stage_order INTEGER NOT NULL,
    approver_id INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    approved_at TIMESTAMP,
    workflow_id INTEGER NOT NULL,
    CONSTRAINT fk_approval_stage_workflow FOREIGN KEY (workflow_id) REFERENCES approval_workflows(id),
    CONSTRAINT fk_approval_stage_user FOREIGN KEY (approver_id) REFERENCES users(id)
);

ALTER TABLE contracts
ADD COLUMN approval_workflow_id INTEGER;

ALTER TABLE contracts
ADD CONSTRAINT fk_contract_approval_workflow FOREIGN KEY (approval_workflow_id) REFERENCES approval_workflows(id);