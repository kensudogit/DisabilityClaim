-- V4: billing batches, cases, traces, validations, exports, returns

CREATE TABLE billing_batches (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    office_id           UUID NOT NULL REFERENCES office_profiles(id),
    billing_month       VARCHAR(7) NOT NULL, -- YYYY-MM
    status              VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    -- DRAFT / CALCULATED / VALIDATED / CONFIRMED / EXPORTED
    fee_rule_set_id     UUID,
    created_by          UUID REFERENCES users(id),
    confirmed_by        UUID REFERENCES users(id),
    confirmed_at        TIMESTAMPTZ,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_billing_batch_office_month UNIQUE (office_id, billing_month)
);

CREATE TABLE billing_cases (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id            UUID NOT NULL REFERENCES billing_batches(id) ON DELETE CASCADE,
    beneficiary_id      UUID NOT NULL REFERENCES beneficiaries(id),
    certificate_id      UUID REFERENCES recipient_certificates(id),
    municipality_id     UUID REFERENCES municipalities(id),
    service_category    VARCHAR(40),
    category            VARCHAR(20), -- ADULT / CHILD
    status              VARCHAR(40) NOT NULL DEFAULT 'CANDIDATE',
    -- CANDIDATE / CALCULATED / NEEDS_RULE_DATA / ERROR / CONFIRMED
    total_units         INT,
    billed_amount       NUMERIC(12, 0),
    confirmed           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE billing_case_items (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    billing_case_id     UUID NOT NULL REFERENCES billing_cases(id) ON DELETE CASCADE,
    item_type           VARCHAR(40) NOT NULL, -- BASE / ADDITION / REDUCTION / CASE_LOAD_REDUCTION
    service_code        VARCHAR(50),
    item_name           VARCHAR(200),
    units               INT,
    unit_price          NUMERIC(12, 2),
    amount              NUMERIC(12, 0),
    rule_code           VARCHAR(100),
    rule_version        VARCHAR(100),
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE billing_calculation_traces (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    billing_case_id     UUID NOT NULL REFERENCES billing_cases(id) ON DELETE CASCADE,
    rule_set_code       VARCHAR(100),
    rule_version        VARCHAR(100),
    source_document     VARCHAR(500),
    inputs_json         TEXT NOT NULL,
    steps_json          TEXT NOT NULL,
    result_json         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE billing_validations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id            UUID NOT NULL REFERENCES billing_batches(id) ON DELETE CASCADE,
    billing_case_id     UUID REFERENCES billing_cases(id) ON DELETE CASCADE,
    rule_code           VARCHAR(100) NOT NULL,
    severity            VARCHAR(20) NOT NULL, -- ERROR / WARNING / INFO
    message             TEXT NOT NULL,
    field_name          VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE billing_exports (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id            UUID NOT NULL REFERENCES billing_batches(id) ON DELETE CASCADE,
    export_type         VARCHAR(50) NOT NULL DEFAULT 'KOKUHO',
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    adapter_name        VARCHAR(100),
    error_message       TEXT,
    created_by          UUID REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ
);

CREATE TABLE billing_export_files (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    export_id           UUID NOT NULL REFERENCES billing_exports(id) ON DELETE CASCADE,
    file_name           VARCHAR(500) NOT NULL,
    content_hash        VARCHAR(128),
    byte_size           BIGINT,
    storage_path        VARCHAR(1000),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE billing_returns (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id            UUID REFERENCES billing_batches(id),
    billing_case_id     UUID REFERENCES billing_cases(id),
    return_code         VARCHAR(50),
    return_reason       TEXT,
    returned_at         DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_billing_cases_batch ON billing_cases(batch_id);
CREATE INDEX idx_billing_case_items_case ON billing_case_items(billing_case_id);
CREATE INDEX idx_billing_traces_case ON billing_calculation_traces(billing_case_id);
CREATE INDEX idx_billing_validations_batch ON billing_validations(batch_id);
CREATE INDEX idx_billing_exports_batch ON billing_exports(batch_id);
