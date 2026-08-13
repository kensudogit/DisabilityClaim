-- V3: support activities and Excel import staging

CREATE TABLE support_activities (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id      UUID NOT NULL REFERENCES beneficiaries(id) ON DELETE CASCADE,
    activity_type       VARCHAR(50) NOT NULL,
    activity_date       DATE NOT NULL,
    billing_month       VARCHAR(7) NOT NULL, -- YYYY-MM
    staff_id            UUID REFERENCES staff(id),
    service_category    VARCHAR(40),
    memo                TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE import_jobs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type            VARCHAR(50) NOT NULL DEFAULT 'EXCEL_BENEFICIARY',
    file_name           VARCHAR(500),
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    total_rows          INT NOT NULL DEFAULT 0,
    valid_rows          INT NOT NULL DEFAULT 0,
    error_rows          INT NOT NULL DEFAULT 0,
    allow_partial       BOOLEAN NOT NULL DEFAULT FALSE,
    committed           BOOLEAN NOT NULL DEFAULT FALSE,
    created_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE TABLE import_staging_rows (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    import_job_id       UUID NOT NULL REFERENCES import_jobs(id) ON DELETE CASCADE,
    row_number          INT NOT NULL,
    raw_json            TEXT NOT NULL,
    mapped_json         TEXT,
    valid               BOOLEAN NOT NULL DEFAULT FALSE,
    error_column        VARCHAR(100),
    error_reason        TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activities_beneficiary ON support_activities(beneficiary_id);
CREATE INDEX idx_activities_billing_month ON support_activities(billing_month);
CREATE INDEX idx_import_staging_job ON import_staging_rows(import_job_id);
