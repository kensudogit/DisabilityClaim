-- V6: anonymized seed data only. NO real PII.
-- Fee master amounts intentionally NULL / PENDING_OFFICIAL_SPEC.

INSERT INTO roles (id, code, name) VALUES
    ('11111111-1111-1111-1111-111111111001', 'ADMIN', '管理者'),
    ('11111111-1111-1111-1111-111111111002', 'BILLING_MANAGER', '請求管理者'),
    ('11111111-1111-1111-1111-111111111003', 'BILLING_OPERATOR', '請求担当'),
    ('11111111-1111-1111-1111-111111111004', 'VIEWER', '閲覧者');

INSERT INTO municipalities (id, code, name, prefecture_code) VALUES
    ('22222222-2222-2222-2222-222222222001', '999001', 'デモ市A（匿名）', '99'),
    ('22222222-2222-2222-2222-222222222002', '999002', 'デモ市B（匿名）', '99');

INSERT INTO office_profiles (
    id, office_code, office_name, corporation_name, postal_code, address, phone, region_category_code
) VALUES (
    '33333333-3333-3333-3333-333333333001',
    'DEMO-OFFICE-001',
    'デモ相談支援事業所',
    'デモ社会福祉法人',
    '000-0000',
    'デモ県デモ市デモ町1-1',
    '000-0000-0000',
    'PENDING_REGION'
);

-- password = password123 (bcrypt)
INSERT INTO users (id, username, password_hash, display_name, email, enabled) VALUES
    ('44444444-4444-4444-4444-444444444001', 'admin',
     '$2a$10$McEHHifPyU.CJ9aESGQ7u.Dfw.OOBb9DZxLOv8KCJdc7QRTlhut9C',
     'デモ管理者', 'admin@example.invalid', TRUE),
    ('44444444-4444-4444-4444-444444444002', 'billing',
     '$2a$10$McEHHifPyU.CJ9aESGQ7u.Dfw.OOBb9DZxLOv8KCJdc7QRTlhut9C',
     'デモ請求担当', 'billing@example.invalid', TRUE);

INSERT INTO user_roles (user_id, role_id) VALUES
    ('44444444-4444-4444-4444-444444444001', '11111111-1111-1111-1111-111111111001'),
    ('44444444-4444-4444-4444-444444444002', '11111111-1111-1111-1111-111111111003');

INSERT INTO staff (id, user_id, office_id, staff_code, display_name, is_consultant, active) VALUES
    ('55555555-5555-5555-5555-555555555001',
     '44444444-4444-4444-4444-444444444002',
     '33333333-3333-3333-3333-333333333001',
     'STAFF-DEMO-01', 'デモ相談員A', TRUE, TRUE);

-- Placeholder fee rule set: amounts NULL until official documents are provided
INSERT INTO fee_rule_sets (
    id, code, name, service_category, effective_from, effective_to, source_document, source_version, notes
) VALUES (
    '66666666-6666-6666-6666-666666666001',
    'PLACEHOLDER_CONSULTATION',
    '制度マスタ未提供プレースホルダ',
    NULL,
    '2024-04-01',
    NULL,
    'PENDING_OFFICIAL_SPEC',
    'PENDING',
    'Do not use for production billing. Load official fee tables before calculation.'
);

INSERT INTO fee_rules (
    id, rule_set_id, rule_code, rule_type, description, units, amount,
    effective_from, effective_to, source_document, source_version
) VALUES (
    '66666666-6666-6666-6666-666666666101',
    '66666666-6666-6666-6666-666666666001',
    'BASE_PLACEHOLDER',
    'BASE',
    '基本報酬プレースホルダ（単位未設定）',
    NULL,
    NULL,
    '2024-04-01',
    NULL,
    'PENDING_OFFICIAL_SPEC',
    'PENDING'
);

INSERT INTO service_code_masters (
    id, service_code, service_name, service_category, units,
    effective_from, effective_to, source_document, source_version
) VALUES (
    '66666666-6666-6666-6666-666666666201',
    'PENDING_CODE',
    'サービスコード未提供',
    'PLAN_CONSULTATION',
    NULL,
    '2024-04-01',
    NULL,
    'PENDING_OFFICIAL_SPEC',
    'PENDING'
);

INSERT INTO addition_masters (
    id, addition_code, addition_name, units, amount, auto_applicable, requires_manual_confirm,
    effective_from, effective_to, source_document, source_version
) VALUES (
    '66666666-6666-6666-6666-666666666301',
    'PENDING_ADDITION',
    '加算マスタ未提供',
    NULL,
    NULL,
    FALSE,
    TRUE,
    '2024-04-01',
    NULL,
    'PENDING_OFFICIAL_SPEC',
    'PENDING'
);

INSERT INTO reduction_rules (
    id, reduction_code, reduction_name, threshold_value, reduction_units, reduction_rate,
    effective_from, effective_to, source_document, source_version
) VALUES (
    '66666666-6666-6666-6666-666666666401',
    'PENDING_REDUCTION',
    '減算・逓減マスタ未提供',
    NULL,
    NULL,
    NULL,
    '2024-04-01',
    NULL,
    'PENDING_OFFICIAL_SPEC',
    'PENDING'
);

INSERT INTO unit_price_masters (
    id, region_category_code, unit_price, conversion_factor,
    effective_from, effective_to, source_document, source_version
) VALUES (
    '66666666-6666-6666-6666-666666666501',
    'PENDING_REGION',
    NULL,
    NULL,
    '2024-04-01',
    NULL,
    'PENDING_OFFICIAL_SPEC',
    'PENDING'
);
