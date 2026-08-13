-- V5: fee masters — NEVER hardcode amounts in application code.
-- All rows must carry effective_from/effective_to/source_document/source_version.

CREATE TABLE fee_rule_sets (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(100) NOT NULL,
    name                VARCHAR(200) NOT NULL,
    service_category    VARCHAR(40),
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    source_document     VARCHAR(500) NOT NULL,
    source_version      VARCHAR(100) NOT NULL,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fee_rule_sets_code_ver UNIQUE (code, source_version, effective_from)
);

CREATE TABLE fee_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_set_id         UUID NOT NULL REFERENCES fee_rule_sets(id) ON DELETE CASCADE,
    rule_code           VARCHAR(100) NOT NULL,
    rule_type           VARCHAR(50) NOT NULL, -- BASE / ADDITION / REDUCTION / CASE_LOAD_REDUCTION
    description         TEXT,
    -- amounts/units intentionally nullable until official spec is loaded
    units               INT,
    amount              NUMERIC(12, 2),
    condition_json      TEXT,
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    source_document     VARCHAR(500) NOT NULL,
    source_version      VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE service_code_masters (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_code        VARCHAR(50) NOT NULL,
    service_name        VARCHAR(200) NOT NULL,
    service_category    VARCHAR(40),
    units               INT,
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    source_document     VARCHAR(500) NOT NULL,
    source_version      VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE addition_masters (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    addition_code       VARCHAR(50) NOT NULL,
    addition_name       VARCHAR(200) NOT NULL,
    units               INT,
    amount              NUMERIC(12, 2),
    auto_applicable     BOOLEAN NOT NULL DEFAULT FALSE,
    requires_manual_confirm BOOLEAN NOT NULL DEFAULT TRUE,
    condition_json      TEXT,
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    source_document     VARCHAR(500) NOT NULL,
    source_version      VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE reduction_rules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reduction_code      VARCHAR(50) NOT NULL,
    reduction_name      VARCHAR(200) NOT NULL,
    -- threshold intentionally NULL until official spec
    threshold_value     INT,
    reduction_units     INT,
    reduction_rate      NUMERIC(8, 4),
    condition_json      TEXT,
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    source_document     VARCHAR(500) NOT NULL,
    source_version      VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE unit_price_masters (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    region_category_code VARCHAR(20) NOT NULL,
    unit_price          NUMERIC(12, 2),
    conversion_factor   NUMERIC(10, 6),
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    source_document     VARCHAR(500) NOT NULL,
    source_version      VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fee_rule_sets_period ON fee_rule_sets(effective_from, effective_to);
CREATE INDEX idx_fee_rules_set ON fee_rules(rule_set_id);
CREATE INDEX idx_service_codes_period ON service_code_masters(effective_from, effective_to);
CREATE INDEX idx_additions_period ON addition_masters(effective_from, effective_to);
CREATE INDEX idx_reductions_period ON reduction_rules(effective_from, effective_to);
CREATE INDEX idx_unit_prices_period ON unit_price_masters(effective_from, effective_to);

ALTER TABLE billing_batches
    ADD CONSTRAINT fk_billing_batches_fee_rule_set
    FOREIGN KEY (fee_rule_set_id) REFERENCES fee_rule_sets(id);
