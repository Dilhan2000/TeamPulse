-- V3: Seed initial Admin user (C1-T02)
-- Default credentials: admin@weeklyreport.local / ChangeMe123!

INSERT INTO users (full_name, email, password_hash, role, status, approved_at)
VALUES ('Admin', 'admin@weeklyreport.local', '$2a$10$6kIm0mXmAfiY8LBYNMbfEuZcXQx3.jqZR7Hg0oapppRXqzskDn5g.', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP);
