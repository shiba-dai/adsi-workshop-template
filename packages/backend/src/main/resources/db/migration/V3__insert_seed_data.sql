-- Seed data for development
-- Password: password123 (BCrypt hash)
INSERT INTO employees (employee_code, name, email, password_hash, role, enabled, version, created_at, updated_at)
VALUES
    ('EMP001', '田中太郎', 'tanaka@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'EMPLOYEE', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('EMP002', '佐藤花子', 'sato@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'MANAGER', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('EMP003', '鈴木一郎', 'suzuki@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
