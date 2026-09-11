-- V6: Create reports and child tables (C2-T02)

CREATE TABLE reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    notes VARCHAR(2000) NULL,
    submitted_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_reports_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT uq_reports_user_week UNIQUE (user_id, week_start_date),
    CONSTRAINT chk_reports_status CHECK (status IN ('DRAFT','SUBMITTED','NEEDS_CORRECTION','APPROVED'))
);

CREATE TABLE report_task_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    priority VARCHAR(10) NOT NULL,
    planned_percent TINYINT NOT NULL DEFAULT 0,
    actual_percent TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    time_planned_hours DECIMAL(5,2) NOT NULL DEFAULT 0,
    time_spent_hours DECIMAL(5,2) NOT NULL DEFAULT 0,
    deliverable VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_task_items_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_items_planned_pct CHECK (planned_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_task_items_actual_pct CHECK (actual_percent BETWEEN 0 AND 100)
);

CREATE TABLE report_next_week_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_next_week_tasks_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

CREATE TABLE report_blockers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    is_key_issue BOOLEAN NOT NULL DEFAULT FALSE,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_blockers_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

CREATE TABLE report_achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    is_key_achievement BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_achievements_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

CREATE TABLE report_hours_by_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    task_type VARCHAR(20) NOT NULL,
    hours DECIMAL(5,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_hours_by_type_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE,
    CONSTRAINT uq_hours_by_type UNIQUE (report_id, task_type)
);
