-- V2__insert_initial_data.sql
-- Script chèn dữ liệu khởi tạo cho Role, TypeTerm và Users với mật khẩu đã mã hóa bằng Bcrypt

---------------------------------------------------------------------
-- Insert dữ liệu cho bảng role
---------------------------------------------------------------------
INSERT INTO roles (role_id, role_name) VALUES (1, 'ADMIN');
INSERT INTO roles (role_id, role_name) VALUES (2, 'MANAGER');
INSERT INTO roles (role_id, role_name) VALUES (3, 'STAFF');

---------------------------------------------------------------------
-- Insert dữ liệu cho bảng type_term
-- Các bản ghi theo enum TypeTermIdentifier:
-- ADDITIONAL_TERMS, LEGAL_BASIS, GENERAL_TERMS, OTHER_TERMS
---------------------------------------------------------------------
INSERT INTO type_term (name, identifier) VALUES ('Điều khoản bổ sung', 'ADDITIONAL_TERMS');
INSERT INTO type_term (name, identifier) VALUES ('Điều khoản Quyền và nghĩa vụ các bên', 'ADDITIONAL_TERMS');
INSERT INTO type_term (name, identifier) VALUES ('Điều khoản Bảo hành và bảo trì', 'ADDITIONAL_TERMS');
INSERT INTO type_term (name, identifier) VALUES ('Điều khoản vi phạm và thiệt hại', 'ADDITIONAL_TERMS');
INSERT INTO type_term (name, identifier) VALUES ('Điều khoản chấm dứt hợp đồng', 'ADDITIONAL_TERMS');
INSERT INTO type_term (name, identifier) VALUES ('Điều khoản giải quyết tranh chấp', 'ADDITIONAL_TERMS');
INSERT INTO type_term (name, identifier) VALUES ('Điều khoản bảo mật', 'ADDITIONAL_TERMS');
INSERT INTO type_term (name, identifier) VALUES ('Căn cứ pháp lí', 'LEGAL_BASIS');
INSERT INTO type_term (name, identifier) VALUES ('Điều khoản chung', 'GENERAL_TERMS');
INSERT INTO type_term (name, identifier) VALUES ('Các điều khoản khác', 'OTHER_TERMS');

---------------------------------------------------------------------
-- Insert dữ liệu cho bảng users
-- Sử dụng hash Bcrypt cho mật khẩu "12345"
---------------------------------------------------------------------
INSERT INTO users (full_name, phone_number, address, email, password, is_active, role_id)
VALUES ('Admin', '0949905590', 'Tokyo', 'chinh0726@gmail.com', '$2a$10$tK7u/Thtce.T4NRVdY.xNeC2LEoOxyVq2GUdB.ZK49MHaOKF7wMeG', true, 1);

INSERT INTO users (full_name, phone_number, address, email, password, is_active, role_id)
VALUES ('Manager', '1111111111', 'Tokyo', 'manager@gmail.com', '$2a$10$tK7u/Thtce.T4NRVdY.xNeC2LEoOxyVq2GUdB.ZK49MHaOKF7wMeG', true, 2);

INSERT INTO users (full_name, phone_number, address, email, password, is_active, role_id)
VALUES ('Staff', '2222222222', 'Tokyo', 'staff@gmail.com', '$2a$10$tK7u/Thtce.T4NRVdY.xNeC2LEoOxyVq2GUdB.ZK49MHaOKF7wMeG', true, 3);
