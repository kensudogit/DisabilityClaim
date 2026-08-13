# ER概要（推奨モデル）

Skill `disability-consultation-billing` の推奨DBモデルに基づく。制度依存マスタは原則として `effective_from` / `effective_to` / `source_document` / `source_version` を持つ。

## エンティティ一覧

| 領域 | テーブル | 概要 |
|------|----------|------|
| 認証 | `users`, `roles` | ログインユーザーとロール（ADMIN / BILLING_MANAGER 等） |
| 事業所 | `staff`, `office_profiles`, `office_qualification_histories` | 相談支援専門員・体制・資格履歴 |
| 利用者 | `beneficiaries`, `beneficiary_addresses` | 匿名化前提の利用者と住所 |
| 受給者証 | `recipient_certificates`, `certificate_service_details` | 証・有効期間・支給決定内容 |
| マスタ | `municipalities` | 市町村コード |
| 実施 | `support_activities` | 計画作成/モニタリング等の実施記録 |
| 請求 | `billing_batches`, `billing_cases`, `billing_case_items` | 月次バッチとケース・明細 |
| 計算根拠 | `billing_calculation_traces` | 計算過程の再現用トレース |
| 検証 | `billing_validations` | ERROR/WARNING/INFO |
| 出力 | `billing_exports`, `billing_export_files` | 国保連向け出力マニフェスト・ファイル |
| 返戻 | `billing_returns` | 返戻コード蓄積 |
| 制度 | `fee_rule_sets`, `fee_rules`, `service_code_masters`, `addition_masters`, `reduction_rules`, `unit_price_masters` | 報酬・加算・逓減・単価（コードに埋め込まない） |
| 移行 | `import_jobs`, `import_staging_rows` | Excelステージング |
| 監査 | `audit_logs` | 誰がいつ何を変更/確定/出力したか |

## 主要リレーション

```text
users ──< roles（多対多想定）
staff ──< beneficiaries（担当）
beneficiaries ──< beneficiary_addresses
beneficiaries ──< recipient_certificates
recipient_certificates ──< certificate_service_details
municipalities ── beneficiaries / recipient_certificates
beneficiaries ──< support_activities
office_profiles ──< office_qualification_histories

billing_batches ──< billing_cases
billing_cases ── beneficiaries / recipient_certificates / support_activities（スナップショット参照可）
billing_cases ──< billing_case_items
billing_cases ──< billing_calculation_traces
billing_cases ──< billing_validations
billing_batches ──< billing_exports ──< billing_export_files
billing_cases ──< billing_returns

fee_rule_sets ──< fee_rules
fee_rule_sets / service_code_masters / addition_masters / reduction_rules / unit_price_masters
  → CalculationEngine が billing_month 時点で解決

import_jobs ──< import_staging_rows
```

## 請求バッチ状態遷移

```text
DRAFT → CALCULATED → VALIDATED → CONFIRMED → EXPORTED
```

| 遷移 | 条件 |
|------|------|
| DRAFT → CALCULATED | CalculationEngine 実行成功 |
| CALCULATED → VALIDATED | ValidationEngine 実行完了（ERROR有無は問わないが結果を保存） |
| VALIDATED → CONFIRMED | **ERROR が0件**であること。ERRORがある場合は禁止 |
| CONFIRMED → EXPORTED | `KokuhoExportAdapter` による出力成功（仕様提供後） |

補足:

- CONFIRMED 後の元データ編集は直接反映しない。再オープンまたは再計算の明示操作が必要。
- 仕様未提供時は EXPORTED へ進めず、UI上「国保連仕様未提供で出力不可」とする。

## 設計メモ

- 成人（計画相談）と障害児相談は同一 `billing_cases` に載せ、`service_category` / `category` で差異を明示する。
- 制度値（単位・加算・逓減閾値・単価）は常にマスタ参照。ソース固定禁止。
- 計算結果には使用した Rule Version と trace を必ず残す。
