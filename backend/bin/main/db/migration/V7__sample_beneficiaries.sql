-- V7: anonymized sample beneficiaries for UI list/search demos.
-- No real PII. Codes only.

INSERT INTO beneficiaries (
    id, office_id, category, recipient_number,
    family_name, given_name, family_name_kana, given_name_kana,
    status, service_start_date, municipality_id, primary_staff_id, notes
) VALUES
(
    '77777777-7777-7777-7777-777777777001',
    '33333333-3333-3333-3333-333333333001',
    'ADULT',
    'U-0001',
    'U-0001', '匿名', 'ユーゼロイチ', 'トクメイ',
    'ACTIVE', '2025-04-01',
    '22222222-2222-2222-2222-222222222001',
    '55555555-5555-5555-5555-555555555001',
    'anonymizedCode=U-0001'
),
(
    '77777777-7777-7777-7777-777777777002',
    '33333333-3333-3333-3333-333333333001',
    'ADULT',
    'U-0002',
    'U-0002', '匿名', 'ユーゼロニ', 'トクメイ',
    'ACTIVE', '2025-06-01',
    '22222222-2222-2222-2222-222222222001',
    '55555555-5555-5555-5555-555555555001',
    'anonymizedCode=U-0002'
),
(
    '77777777-7777-7777-7777-777777777003',
    '33333333-3333-3333-3333-333333333001',
    'CHILD',
    'U-0003',
    'U-0003', '匿名', 'ユーゼロサン', 'トクメイ',
    'ACTIVE', '2025-09-01',
    '22222222-2222-2222-2222-222222222002',
    '55555555-5555-5555-5555-555555555001',
    'anonymizedCode=U-0003'
),
(
    '77777777-7777-7777-7777-777777777004',
    '33333333-3333-3333-3333-333333333001',
    'ADULT',
    'U-0004',
    'U-0004', '匿名', 'ユーゼロヨン', 'トクメイ',
    'SUSPENDED', '2024-04-01',
    '22222222-2222-2222-2222-222222222002',
    NULL,
    'anonymizedCode=U-0004'
),
(
    '77777777-7777-7777-7777-777777777005',
    '33333333-3333-3333-3333-333333333001',
    'CHILD',
    'U-0005',
    'U-0005', '匿名', 'ユーゼロゴ', 'トクメイ',
    'CLOSED', '2023-04-01',
    '22222222-2222-2222-2222-222222222001',
    NULL,
    'anonymizedCode=U-0005; service_end_date set'
);

UPDATE beneficiaries
SET service_end_date = '2025-03-31'
WHERE id = '77777777-7777-7777-7777-777777777005';

INSERT INTO recipient_certificates (
    id, beneficiary_id, certificate_number, municipality_id,
    valid_from, valid_to, service_category, monitoring_months, notes
) VALUES
(
    '88888888-8888-8888-8888-888888888001',
    '77777777-7777-7777-7777-777777777001',
    'CERT-U0001-01',
    '22222222-2222-2222-2222-222222222001',
    '2025-04-01', '2027-03-31',
    'PLAN_CONSULTATION', 6,
    'demo certificate'
),
(
    '88888888-8888-8888-8888-888888888002',
    '77777777-7777-7777-7777-777777777001',
    'CERT-U0001-02',
    '22222222-2222-2222-2222-222222222001',
    '2026-04-01', '2027-03-31',
    'MONITORING', 6,
    'demo monitoring certificate'
),
(
    '88888888-8888-8888-8888-888888888003',
    '77777777-7777-7777-7777-777777777002',
    'CERT-U0002-01',
    '22222222-2222-2222-2222-222222222001',
    '2025-06-01', '2027-03-31',
    'PLAN_CONSULTATION', 6,
    'demo certificate'
),
(
    '88888888-8888-8888-8888-888888888004',
    '77777777-7777-7777-7777-777777777003',
    'CERT-U0003-01',
    '22222222-2222-2222-2222-222222222002',
    '2025-09-01', '2027-03-31',
    'CHILD_CONSULTATION', 3,
    'demo child certificate'
),
(
    '88888888-8888-8888-8888-888888888005',
    '77777777-7777-7777-7777-777777777004',
    'CERT-U0004-01',
    '22222222-2222-2222-2222-222222222002',
    '2024-04-01', '2025-03-31',
    'PLAN_CONSULTATION', 6,
    'expired demo certificate'
);

-- Current month activities so billing candidates can resolve for ACTIVE users
INSERT INTO support_activities (
    id, beneficiary_id, activity_type, activity_date, billing_month,
    staff_id, service_category, memo
) VALUES
(
    '99999999-9999-9999-9999-999999999001',
    '77777777-7777-7777-7777-777777777001',
    'SERVICE_UTILIZATION_SUPPORT',
    DATE_TRUNC('month', CURRENT_DATE)::date + 5,
    TO_CHAR(CURRENT_DATE, 'YYYY-MM'),
    '55555555-5555-5555-5555-555555555001',
    'PLAN_CONSULTATION',
    'demo activity for current month'
),
(
    '99999999-9999-9999-9999-999999999002',
    '77777777-7777-7777-7777-777777777002',
    'CONTINUOUS_SERVICE_UTILIZATION_SUPPORT',
    DATE_TRUNC('month', CURRENT_DATE)::date + 8,
    TO_CHAR(CURRENT_DATE, 'YYYY-MM'),
    '55555555-5555-5555-5555-555555555001',
    'PLAN_CONSULTATION',
    'demo activity for current month'
),
(
    '99999999-9999-9999-9999-999999999003',
    '77777777-7777-7777-7777-777777777003',
    'CHILD_CONSULTATION_SUPPORT',
    DATE_TRUNC('month', CURRENT_DATE)::date + 3,
    TO_CHAR(CURRENT_DATE, 'YYYY-MM'),
    '55555555-5555-5555-5555-555555555001',
    'CHILD_CONSULTATION',
    'demo child activity for current month'
);
