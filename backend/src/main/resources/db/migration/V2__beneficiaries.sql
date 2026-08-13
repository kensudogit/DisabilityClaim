-- V2: beneficiaries and certificates
-- Period overlap for certificates is enforced in application (BeneficiaryService).
-- Optional DB exclusion constraint can be added when official rules are confirmed.

CREATE TABLE beneficiaries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    office_id           UUID NOT NULL REFERENCES office_profiles(id),
    category            VARCHAR(20) NOT NULL, -- ADULT / CHILD
    recipient_number    VARCHAR(20),
    family_name         VARCHAR(100) NOT NULL,
    given_name          VARCHAR(100) NOT NULL,
    family_name_kana    VARCHAR(100),
    given_name_kana     VARCHAR(100),
    birth_date          DATE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE / SUSPENDED / CLOSED
    service_start_date  DATE,
    service_end_date    DATE,
    municipality_id     UUID REFERENCES municipalities(id),
    primary_staff_id    UUID REFERENCES staff(id),
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE beneficiary_addresses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id      UUID NOT NULL REFERENCES beneficiaries(id) ON DELETE CASCADE,
    postal_code         VARCHAR(10),
    prefecture          VARCHAR(40),
    city                VARCHAR(100),
    address_line        VARCHAR(300),
    is_primary          BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from      DATE,
    effective_to        DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE recipient_certificates (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    beneficiary_id          UUID NOT NULL REFERENCES beneficiaries(id) ON DELETE CASCADE,
    certificate_number      VARCHAR(30) NOT NULL,
    municipality_id         UUID NOT NULL REFERENCES municipalities(id),
    valid_from              DATE NOT NULL,
    valid_to                DATE NOT NULL,
    service_category        VARCHAR(40) NOT NULL,
    monitoring_months       INT,
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cert_period CHECK (valid_to >= valid_from)
);

CREATE TABLE certificate_service_details (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificate_id          UUID NOT NULL REFERENCES recipient_certificates(id) ON DELETE CASCADE,
    service_code_placeholder VARCHAR(50), -- NOT an official code; official codes come from masters
    service_name            VARCHAR(200),
    decided_units           INT,
    decided_from            DATE,
    decided_to              DATE,
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_beneficiaries_office ON beneficiaries(office_id);
CREATE INDEX idx_beneficiaries_municipality ON beneficiaries(municipality_id);
CREATE INDEX idx_certificates_beneficiary ON recipient_certificates(beneficiary_id);
CREATE INDEX idx_certificates_period ON recipient_certificates(valid_from, valid_to);
CREATE INDEX idx_cert_details_cert ON certificate_service_details(certificate_id);
CREATE INDEX idx_beneficiary_addresses_ben ON beneficiary_addresses(beneficiary_id);

COMMENT ON TABLE recipient_certificates IS
    'Period overlap for same beneficiary+service_category must be checked in app (or via exclusion constraint once official rules are fixed).';
