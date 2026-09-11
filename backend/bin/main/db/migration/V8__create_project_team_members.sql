-- V8: Create project_team_members table (C5-B01)

CREATE TABLE project_team_members (
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT fk_ptm_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_ptm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
