-- V1: auth, org, municipalities, audit

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE municipalities (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(10) NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    prefecture_code VARCHAR(2),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(100) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(200) NOT NULL,
    email           VARCHAR(255),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL
);

CREATE TABLE user_roles (
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id         UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE office_profiles (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    office_code             VARCHAR(20) NOT NULL UNIQUE,
    office_name             VARCHAR(200) NOT NULL,
    corporation_name        VARCHAR(200),
    postal_code             VARCHAR(10),
    address                 VARCHAR(500),
    phone                   VARCHAR(30),
    region_category_code    VARCHAR(20),
    -- region_category_code is a placeholder key; amounts come from fee masters
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE staff (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    office_id       UUID NOT NULL REFERENCES office_profiles(id),
    staff_code      VARCHAR(50),
    display_name    VARCHAR(200) NOT NULL,
    is_consultant   BOOLEAN NOT NULL DEFAULT FALSE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE office_qualification_histories (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    office_id           UUID NOT NULL REFERENCES office_profiles(id) ON DELETE CASCADE,
    qualification_code  VARCHAR(50) NOT NULL,
    qualification_name  VARCHAR(200) NOT NULL,
    effective_from      DATE NOT NULL,
    effective_to        DATE,
    source_document     VARCHAR(500),
    source_version      VARCHAR(100),
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id   UUID,
    actor_username  VARCHAR(100),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100),
    entity_id       VARCHAR(100),
    before_value    TEXT,
    after_value     TEXT,
    ip_address      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_staff_office ON staff(office_id);
CREATE INDEX idx_office_qual_office ON office_qualification_histories(office_id);
