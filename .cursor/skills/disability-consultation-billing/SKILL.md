---
name: disability-consultation-billing
version: 1.0.0
description: 障害福祉サービス（計画相談支援・障害児相談支援）の利用者管理、受給者証管理、請求対象抽出、報酬・加算・逓減計算、国保連取込送信システム向け請求データ生成、返戻防止チェックを、Java/Spring Boot/PostgreSQL/React/Next.js/TypeScriptで設計・実装・テストするための開発Skill。
---

# Disability Consultation Billing Skill

## 目的
障害福祉サービスの相談支援事業所向け請求業務システムを、安全かつ制度改定に追随可能な形で設計・実装する。
Excelからの転記作業を廃止し、利用者・受給者証情報から月次請求対象を抽出し、報酬計算、チェック、国保連向けデータ出力、請求履歴管理まで一貫して支援する。

## 最重要原則
1. 国保連・電子請求受付システム・取込送信システムのファイル仕様、コード体系、必須項目、桁数、文字コード、レコード順序を推測しない。
2. 実装前に、対象請求年月に有効な公式インタフェース仕様書・マスタ・報酬告示・留意事項・Q&Aを確認する。
3. 基本報酬、加算、減算、逓減、地域区分、単価等をソースコードへ固定値として埋め込まない。effective_from/effective_toを持つ制度マスタとして管理する。
4. 計算結果には、使用した制度バージョン、根拠マスタ、入力値、計算過程を保存し、再現可能にする。
5. 国保連送信そのものは既存の「取込送信システム」を使用する。開発システムは取り込み可能な請求データを作成する境界までを原則とする。
6. 個人情報を扱うため、最小権限、監査ログ、暗号化、バックアップ、操作履歴、CSV/Excel出力制御を必須とする。

## 対象技術スタック
- Backend: Java 21+, Spring Boot 3.x
- Persistence: Spring Data JPA または MyBatis、PostgreSQL 16+
- Frontend: React 19 / Next.js / TypeScript
- API: REST/JSON、OpenAPI 3
- Migration: Flyway
- Test: JUnit 5, Testcontainers, Playwright
- Build: Maven または Gradle
- Optional: Docker / Docker Compose / CI/CD

## 機能スコープ
### 1. 利用者管理
- 利用者基本情報
- 障害者/障害児区分
- 住所、市町村、受給者番号等
- 利用開始・終了日
- 担当相談支援専門員
- 状態（利用中/終了/休止）

### 2. 受給者証管理
- 受給者証番号
- 支給決定市町村
- 有効期間
- サービス種別
- 支給決定内容
- モニタリング期間
- 更新履歴
- 同一利用者の期間重複チェック

### 3. Excel初期移行
- Excel列とDB項目のマッピング定義を作成
- import staging tableへ一旦格納
- 必須、型、コード、日付範囲、重複を検証
- エラー行は行番号・列名・理由つきで返却
- 本登録は検証成功後にトランザクション実行
- 移行結果レポートを出力

### 4. 月次請求対象者抽出
請求年月を指定し、以下を基礎条件として候補を抽出する。
- 対象月に受給者証が有効
- 計画作成/モニタリング等の請求根拠となる実施記録が存在
- サービス区分が対象制度と整合
- 既請求/取消/再請求状態を考慮
- 対象市町村が確定している

候補抽出後、人が確認して請求バッチを確定する。

### 5. 実施日入力
- サービス利用支援/継続サービス利用支援等の実施日
- 障害児相談支援の対応種別
- 実施担当者
- 補足メモ
- 同日・同月重複や制度上不整合な組合せをチェック

### 6. 報酬計算エンジン
Calculation Engineを独立ドメインとして実装する。
入力:
- billing_month
- beneficiary snapshot
- certificate snapshot
- activity records
- office attributes
- staff/case-load information
- rule version

出力:
- base service code
- base units
- additions/deductions
- reduction results
- total units
- unit price / applicable conversion factor
- billed amount
- calculation trace

計算式はRuleテーブル/Strategy実装に分離し、制度改定時に差し替え可能にする。

### 7. 加算・減算
- 加算マスタにコード、名称、適用期間、単位、適用条件を保持
- 自動判定可能な条件と人の確認が必要な条件を区別
- 根拠となる事業所体制・資格・実績を履歴化
- 同時算定不可、上限、重複などの排他ルールをValidation Ruleとして管理

### 8. 担当件数に応じた逓減
- 相談支援専門員、事業所、対象期間の件数を集計
- 対象制度の公式ルールに基づき閾値と適用単位をRule masterから取得
- 並び順・算定順等が仕様に影響する場合、根拠仕様に忠実に処理
- 閾値をコードにハードコードしない
- calculation_traceに件数、閾値、適用結果を保存

### 9. 市町村別請求データ
- 請求バッチから支給決定市町村単位にグルーピング
- 市町村コードを公式マスタで検証
- 件数、単位数、請求額のサマリーを表示
- 市町村単位の再生成を可能にする

### 10. 障害児相談支援
成人の計画相談支援と同じテーブルへ無理に押し込まず、共通BillingCase + service_categoryで差異を明示する。
制度別のサービスコード、報酬、加算、チェックルールを個別Rule Setとして持つ。

### 11. 国保連請求用データ出力
Adapter Patternで `KokuhoExportAdapter` を設計する。

必須フロー:
1. 対象請求年月の公式仕様バージョンを確定
2. 内部Billing DTOを公式レコードモデルへ変換
3. 必須項目、コード、桁数、形式、関連整合性を検証
4. 仕様どおりのファイル形式・文字コード・レコード順で生成
5. checksum/件数/合計等、仕様にある制御情報を生成
6. export manifestを保存
7. 出力ファイルhashを保存し監査可能にする

注意: 仕様書が提供されていない状態では、Exporterのinterface、DTO、テストfixtureまで作成し、実ファイルレイアウトを仮定して完成扱いにしてはいけない。

### 12. 請求確認画面
Next.jsで以下を提供する。
- 請求年月選択
- 対象者一覧
- 市町村フィルタ
- 障害者/障害児フィルタ
- エラー/警告フィルタ
- 基本報酬、加算、減算、単位、金額表示
- 計算根拠の展開表示
- 確認済みフラグ
- 請求確定
- 国保連ファイル出力

### 13. 返戻防止チェック
Validation Severityを ERROR / WARNING / INFO に分類する。
ERRORが1件でも存在する請求バッチは出力不可。

チェック例:
- 必須項目欠落
- 受給者証期限切れ
- 対象月との期間不整合
- 市町村コード不正
- 受給者番号形式不正
- 実施日なし
- 重複請求疑い
- サービス種別不整合
- 加算要件不足
- 排他的加算の同時算定
- 単位・金額計算結果不整合
- 既送信請求の二重生成

返戻理由が得られる場合、返戻コードを蓄積しValidation Rule改善へ利用する。

## 推奨DBモデル
- users
- roles
- staff
- beneficiaries
- beneficiary_addresses
- recipient_certificates
- certificate_service_details
- municipalities
- support_activities
- office_profiles
- office_qualification_histories
- billing_batches
- billing_cases
- billing_case_items
- billing_calculation_traces
- billing_validations
- billing_exports
- billing_export_files
- billing_returns
- fee_rule_sets
- fee_rules
- service_code_masters
- addition_masters
- reduction_rules
- unit_price_masters
- import_jobs
- import_staging_rows
- audit_logs

全ての制度依存マスタは原則として `effective_from`, `effective_to`, `source_document`, `source_version` を持つ。

## API例
- GET /api/v1/beneficiaries
- POST /api/v1/beneficiaries
- GET /api/v1/beneficiaries/{id}/certificates
- POST /api/v1/imports/excel
- GET /api/v1/billing/candidates?month=YYYY-MM
- POST /api/v1/billing/batches
- POST /api/v1/billing/batches/{id}/calculate
- POST /api/v1/billing/batches/{id}/validate
- GET /api/v1/billing/batches/{id}
- POST /api/v1/billing/batches/{id}/confirm
- POST /api/v1/billing/batches/{id}/exports/kokuho
- GET /api/v1/billing/batches/{id}/validations

## 状態遷移
DRAFT -> CALCULATED -> VALIDATED -> CONFIRMED -> EXPORTED

ERRORがある場合 VALIDATED -> CONFIRMED を禁止する。
CONFIRMED後の元データ編集は直接反映せず、請求バッチを再オープンまたは再計算する明示操作を要求する。

## セキュリティ
- Spring SecurityでRBAC
- ADMIN / BILLING_MANAGER / BILLING_OPERATOR / VIEWER等
- TLS前提
- DBバックアップ暗号化
- 機微なエクスポートのアクセス制御
- パスワードをログ出力しない
- 利用者データの変更前後値を監査ログへ記録（過剰な機微情報はマスキング）
- CSRF/XSS/SQL Injection/IDOR対策
- セッション/Tokenの期限管理

## テスト戦略
### Unit
- 各報酬Rule
- 加算/減算判定
- 逓減境界値
- 丸め
- 期間判定
- Validator

### Integration
- PostgreSQLをTestcontainersで起動
- Excel import -> billing candidate -> calculate -> validate -> exportまでのシナリオ

### Golden Master
公式仕様で検証済みのサンプル請求データをfixture化し、生成ファイルのbyte/record単位比較を行う。
制度バージョン別にfixtureを分離する。

### E2E
Playwrightで、利用者登録、実施日入力、月次請求作成、エラー修正、確認、出力までを検証する。

## 実装の進め方
1. 公式仕様・現行Excel・実際の請求サンプル・返戻例を収集
2. As-Is/To-Be業務フロー作成
3. Excel項目棚卸しとER設計
4. 制度Rule Catalog作成
5. 国保連I/F項目マッピング作成
6. MVP: 利用者/受給者証/Excel移行/実施記録/月次抽出
7. 報酬計算・Validation
8. 国保連Exporter
9. 画面・監査・権限
10. 実データ匿名化テスト
11. 並行稼働でExcel/既存手入力結果との突合
12. 本番移行

## 見積りを求められた場合の分解
以下のWBSで算出する。
- 要件定義・現行Excel/請求業務分析
- 外部仕様調査・国保連I/F定義
- UI/UX・基本設計
- DB/API詳細設計
- マスタ/制度Rule実装
- 利用者/受給者証
- Excel移行
- 月次請求・計算
- 逓減・加算
- Validation
- 国保連Exporter
- 障害児相談支援
- 認証/権限/監査
- 単体/結合/E2E
- UAT・並行稼働
- 導入・操作説明

MVPと本番完全版を分けて見積もり、制度仕様が未提供の部分にはリスクバッファを設定する。

## Definition of Done
- 対象月の公式制度資料とのトレーサビリティがある
- 同じ入力・Rule Versionで同じ計算結果を再現できる
- ERROR状態で国保連データを出力できない
- Excel初期移行結果を件数・エラー数で照合できる
- 匿名化した代表ケースで既存請求結果と一致する
- 国保連取込送信システムで受入確認済みのfixtureがある
- 監査ログで「誰が・いつ・何を変更/確定/出力したか」を追跡できる

## AIへの禁止事項
- 未確認の報酬単位数・加算値・逓減閾値を断定して実装しない
- 非公式ブログのみを根拠に制度ロジックを確定しない
- 国保連のファイル拡張子、CSVレイアウト、文字コードを推測しない
- 個人情報をテストコードやGitへ固定値でコミットしない
- 金額差異を丸め誤差として安易に無視しない

## AIへの出力指示
実装依頼を受けたら、最初に以下を提示する。
1. 対象機能
2. 前提となる制度/仕様バージョン
3. DB変更
4. API変更
5. Backend実装
6. Frontend実装
7. Validation
8. Test
9. 未確定事項/公式資料が必要な項目

その後、コンパイル可能な単位でコードを生成する。
