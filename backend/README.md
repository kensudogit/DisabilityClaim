# Disability Claim Backend

Spring Boot 3.5 / Java 21 / Gradle / PostgreSQL / Flyway backend for
計画相談支援・障害児相談支援 consultation billing (MVP).

## Critical constraints

- Fee units, addition amounts, reduction thresholds, and 国保連 file layouts are **never hardcoded**.
- Fee masters always carry `effective_from`, `effective_to`, `source_document`, `source_version`.
- Seed fee rows use `source_document=PENDING_OFFICIAL_SPEC` with **NULL** amounts.
- `KokuhoExportAdapter` stub throws `IllegalStateException("公式I/F仕様未提供")` until official specs arrive.
- Seed/test data is anonymized only (no real PII).

## Prerequisites

- JDK 21+
- Gradle 8.10.2 (or use wrapper after generating jar: `gradle wrapper --gradle-version 8.10.2`)
- Docker (for PostgreSQL / Testcontainers)

## Start database

From repo root `C:\devlop\DisabilityClaim`:

```bash
docker compose up -d postgres
```

## Run application

```bash
cd backend
# if gradle-wrapper.jar is missing:
gradle wrapper --gradle-version 8.10.2

set DB_URL=jdbc:postgresql://localhost:5432/disability_claim
set DB_USER=disability
set DB_PASSWORD=disability

./gradlew.bat bootRun
```

Demo users (password: `password123`):

| username | role |
|----------|------|
| admin    | ADMIN |
| billing  | BILLING_OPERATOR |

## Auth

```bash
curl -X POST http://localhost:8080/api/v1/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"password123\"}"
```

Use `Authorization: Bearer <accessToken>` on subsequent calls.

Also supports HTTP Basic for MVP.

## Main APIs

- `GET/POST /api/v1/beneficiaries`
- `GET/POST /api/v1/beneficiaries/{id}/certificates`
- `POST /api/v1/imports/excel`
- `GET /api/v1/billing/candidates?month=YYYY-MM`
- `POST /api/v1/billing/batches`
- `POST /api/v1/billing/batches/{id}/calculate`
- `POST /api/v1/billing/batches/{id}/validate`
- `GET /api/v1/billing/batches/{id}`
- `POST /api/v1/billing/batches/{id}/confirm`
- `POST /api/v1/billing/batches/{id}/exports/kokuho`
- `GET /api/v1/billing/batches/{id}/validations`

Batch status flow: `DRAFT -> CALCULATED -> VALIDATED -> CONFIRMED -> EXPORTED`.
Any validation **ERROR** blocks confirm/export.

## Tests

```bash
./gradlew.bat test
```

Unit tests cover ValidationEngine, CalculationEngine (empty/PENDING masters → `NEEDS_RULE_DATA`), and certificate overlap.
Integration smoke uses Testcontainers PostgreSQL when Docker is available.

## Optional full stack compose

```bash
docker compose --profile full up --build
```

Note: backend Docker image expects Gradle wrapper jar; generate it before building, or run the JAR locally.

## Loading official fee masters

Replace/insert rows into:

- `fee_rule_sets`, `fee_rules`
- `service_code_masters`
- `addition_masters`
- `reduction_rules`
- `unit_price_masters`

Always set `source_document` / `source_version` to the official notice identifiers for the billing month.
