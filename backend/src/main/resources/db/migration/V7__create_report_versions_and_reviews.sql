CREATE TABLE report_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    content_snapshot JSON NOT NULL,
    submitted_at DATETIME NOT NULL,
    CONSTRAINT fk_report_versions_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE,
    CONSTRAINT uq_report_versions_number UNIQUE (report_id, version_number)
);

CREATE TABLE report_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    version_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    comment VARCHAR(2000) NULL,
    reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_reviews_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_report_reviews_version FOREIGN KEY (version_id) REFERENCES report_versions(id),
    CONSTRAINT fk_report_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT chk_report_reviews_action CHECK (action IN ('APPROVED','CHANGES_REQUESTED'))
);
