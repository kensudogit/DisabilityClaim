"use client";

/**
 * 画面右下のドラッグ可能な利用手順パネル（localStorage で位置・開閉を保存）。
 * 障害福祉サービス（計画相談支援・障害児相談支援）請求 MVP の
 * アーキテクチャ・業務フロー・計算/検証エンジン・制限事項を表示する。
 * デザインは Data Engineer Pilot / Ecosystem Platform の UsageGuidePanel と共通（CSS はそのまま流用）。
 */
import { useCallback, useEffect, useRef, useState } from "react";
import styles from "./UsageGuidePanel.module.css";

const STORAGE_KEY = "disability-claim-usage-guide-v1";
const PANEL_WIDTH = 440;

type GuideStep = {
  title: string;
  body: string;
  items?: readonly string[];
};

type FeaturedBlock = {
  badge: string;
  title: string;
  body: string;
  items?: readonly string[];
  variant?:
    | "architecture"
    | "rag"
    | "image"
    | "embed"
    | "guard"
    | "prompt"
    | "eval"
    | "agent"
    | "deploy"
    | "pipeline"
    | "cortex";
};

const valueFeatured: FeaturedBlock = {
  badge: "Value",
  title: "本パッケージの位置づけ",
  body:
    "計画相談支援・障害児相談支援の月次請求業務（利用者・受給者証の管理 → 実施記録 → 請求対象抽出 → 報酬計算 → 返戻防止チェック → 国保連向け出力）を、1本の業務フローとして通しで確認できる MVP です。最大の設計方針は「公式の報酬告示・国保連インタフェース仕様が手元に無い状態で、金額を推測して埋めない」こと。単価が未設定の項目は必ず「制度マスタ未設定」と表示され、計算エンジンは NEEDS_RULE_DATA として明示的に停止します。",
  variant: "agent",
  items: [
    "偽の金額を出さない — 報酬単価・加算単位が未設定なら金額欄は空にせず「制度マスタ未設定」と明示表示",
    "確定をブロックする設計 — ERROR 相当の検証結果が1件でもあればバッチ確定・出力に進めない（返戻の事前防止）",
    "根拠を残す — 計算はステップ単位で billing_calculation_traces に inputs/steps/result を JSON で保存し、画面から追跡可能",
    "個人情報の最小化 — 氏名等は必須入力にせず、匿名コード・受給者番号中心で運用できるデータモデル",
    "差し替え前提の構造 — 報酬ルールは fee_rule_sets / fee_rules に外出しし、告示改定は「新しい有効期間のルールセット追加」で対応",
    "国保連 I/F は未確定を明示 — 出力アダプタは差し替え可能なインタフェースにし、仕様入手までは UNSUPPORTED を返す",
  ],
};

const architectureFeatured: FeaturedBlock = {
  badge: "Architecture",
  title: "Next.js (BFF) + Spring Boot + PostgreSQL",
  body:
    "ブラウザは常に同一オリジンだけを見ます。Next.js の Route Handler（/api/v1/[...path]）が Spring Boot :8080 へプロキシし、JWT を Authorization ヘッダーでそのまま中継します。バックエンドは Flyway でスキーマを管理し、起動時に V1〜V6 のマイグレーションを適用します。",
  variant: "architecture",
  items: [
    "Frontend — Next.js 15 / React 19 / TypeScript / CSS Modules（外部 UI ライブラリなし）",
    "BFF プロキシ — frontend/src/app/api/v1/[...path]/route.ts（バックエンド未起動時は 503 と原因を JSON で返す）",
    "Backend — Spring Boot 3.5 / Java 21 / Spring Security（JWT・ステートレス）/ Spring Data JPA",
    "DB — PostgreSQL 16（Flyway V1〜V6 で 25 テーブル）",
    "計算 — CalculationEngine + 4 つの Strategy（BASE / ADDITION / REDUCTION / CASE_LOAD_REDUCTION）",
    "検証 — ValidationEngine + 8 ルール（ERROR / WARNING の 2 段階）",
    "疎通確認 — /api/diag（バックエンド健全性と環境変数の設定有無を返す）· /actuator/health",
  ],
};

const beneficiaryFeatured: FeaturedBlock = {
  badge: "1",
  title: "利用者・受給者証管理（/beneficiaries）",
  body:
    "請求の起点となる利用者（障害者＝ADULT／障害児＝CHILD）と、その受給者証（有効期間・サービス種別・支給決定量）を管理します。受給者証は同一利用者で有効期間が重複する登録を BeneficiaryService.assertNoOverlap() で拒否します。期間の重複は請求の二重計上に直結するため、登録時点で止める設計です。",
  variant: "rag",
  items: [
    "一覧・新規登録 — GET / POST /api/v1/beneficiaries",
    "詳細 — /beneficiaries/[id] で受給者証の一覧を表示",
    "受給者証 — GET / POST /api/v1/beneficiaries/{id}/certificates（有効期間の重複はエラー）",
    "区分 — ADULT（障害者・計画相談支援）/ CHILD（障害児・障害児相談支援）",
    "市町村 — municipalities マスタで管理（デモ用コード 999001 / 999002 を投入済み）",
    "個人情報 — 氏名は必須にしていません。匿名コード運用を前提に設計しています",
  ],
};

const importFeatured: FeaturedBlock = {
  badge: "2",
  title: "Excel 初期移行（/imports）",
  body:
    "既存台帳（Excel）からの初期データ移行機能です。アップロードしたブックはまず import_staging_rows へ「生データのまま」取り込まれ、その後に検証を行います。検証を通らなかった行は行番号・列名・理由の3点セットで画面に返るため、Excel 側を直して再アップロードできます。allowPartial を指定した場合のみ、有効行だけを確定登録します。",
  variant: "embed",
  items: [
    "アップロード — POST /api/v1/imports/excel（multipart: file, officeId, allowPartial）",
    "取込方式 — Apache POI 5.4 で .xlsx を解析 → ステージング → 検証 → 確定（2段階コミット）",
    "列マッピング — ColumnMapping.defaultBeneficiaryMapping()（日本語ヘッダー: 姓 / 名 / 区分 / 受給者番号 / 市町村コード 等）",
    "エラー表示 — 行番号・列・理由を一覧表示。部分取込は allowPartial=true のときのみ",
    "ジョブ管理 — import_jobs に総行数 / 有効行数 / 確定有無を記録",
    "現状の対象 — 利用者シートのみ。受給者証シート・実施記録シートは docs/excel-import.md に様式のみ定義（未実装）",
  ],
};

const billingFeatured: FeaturedBlock = {
  badge: "3",
  title: "月次請求バッチ（/billing）",
  body:
    "請求月と事業所を指定して対象を抽出し、バッチとして DRAFT → CALCULATED → VALIDATED → CONFIRMED → EXPORTED のステートマシンで進めます。抽出条件は「その月に有効な受給者証があること」かつ「その月の実施記録（support_activities）があること」です。実施記録が無い利用者は候補に上がらず、上がった後も MISSING_ACTIVITY として検証で弾かれます。",
  variant: "eval",
  items: [
    "候補抽出 — GET /api/v1/billing/candidates?month=YYYY-MM&officeId=...",
    "バッチ作成 — POST /api/v1/billing/batches（officeId, month）→ DRAFT",
    "計算 — POST /api/v1/billing/batches/{id}/calculate → CALCULATED",
    "検証 — POST /api/v1/billing/batches/{id}/validate → VALIDATED",
    "確定 — POST /api/v1/billing/batches/{id}/confirm（ERROR が1件でもあれば拒否）",
    "差戻し — POST /api/v1/billing/batches/{id}/reopen で DRAFT に戻す",
    "明細 — GET /api/v1/billing/batches/{id} で { batch, cases } を取得。ケースごとに計算トレースを展開表示",
  ],
};

const calculationFeatured: FeaturedBlock = {
  badge: "4",
  title: "報酬計算エンジン（CalculationEngine）",
  body:
    "請求ケースごとに Strategy を順に適用し、billing_case_items（BASE / ADDITION / REDUCTION / CASE_LOAD_REDUCTION）を積み上げます。重要なのは、参照する fee_rule_sets が未確定（source_document=PENDING_OFFICIAL_SPEC）または単位数・金額が NULL の場合、金額をゼロや推定値で埋めずに NEEDS_RULE_DATA を立てて停止することです。これにより「動くが数字が嘘」という最悪の状態を構造的に防ぎます。",
  variant: "prompt",
  items: [
    "BaseFeeRuleStrategy — 基本報酬。BASE ルールの単位数が NULL なら NEEDS_RULE_DATA",
    "AdditionRuleStrategy — 加算。公式の適用条件（condition_json）が無い限り自動適用しない",
    "ReductionRuleStrategy — 減算。存在しない減算を勝手に作らない",
    "CaseLoadReductionRuleStrategy — 取扱件数による逓減。閾値はマスタからのみ読む（骨格実装）",
    "トレース — inputs_json / steps_json / result_json を billing_calculation_traces に保存し画面から追跡",
    "ルール差し替え — fee_rule_sets に有効期間付きで登録。告示改定は新セット追加で対応（既存バッチの再計算は不要）",
  ],
};

const validationFeatured: FeaturedBlock = {
  badge: "5",
  title: "返戻防止バリデーション（ValidationEngine）",
  body:
    "国保連からの返戻は事後対応コストが大きいため、確定前に機械的に検出します。ERROR は確定・出力をブロックし、WARNING は担当者の確認を促すのみで進行は止めません。結果は billing_validations に rule_code / severity / message で永続化され、/billing/[batchId] で絞り込み表示できます。",
  variant: "guard",
  items: [
    "MISSING_REQUIRED_FIELD（ERROR）— 受給者番号・市町村など必須項目の欠落",
    "CERTIFICATE_EXPIRED（ERROR）— 請求月時点で受給者証が有効期限切れ",
    "PERIOD_MISMATCH（ERROR）— 請求月と受給者証の有効期間が整合しない",
    "MISSING_ACTIVITY（ERROR）— 当月の実施記録が無いのに請求対象になっている",
    "SERVICE_CATEGORY_MISMATCH（ERROR）— 受給者証のサービス種別と請求内容の不一致",
    "CALCULATION_INCONSISTENCY（ERROR）— 明細合計と請求ケース合計の不一致",
    "NEEDS_RULE_DATA（ERROR）— 報酬ルール未設定のまま金額を出そうとしている",
    "DUPLICATE_BILLING_SUSPECT（WARNING）— 同一利用者・同一月の重複請求の疑い",
  ],
};

const exportFeatured: FeaturedBlock = {
  badge: "6",
  title: "国保連取込送信システム向け出力（未提供・意図的な未実装）",
  body:
    "国保連（国民健康保険団体連合会）取込送信システムのレコード様式・桁数・コード体系は公式インタフェース仕様書に基づく必要があります。本 MVP では仕様書が未入手のため、推測で CSV を生成することを明確に避けています。出力アダプタは KokuhoExportAdapter インタフェースとして切り出してあり、仕様入手後に実装クラスを1つ追加するだけで差し替えられます。",
  variant: "image",
  items: [
    "実行 — POST /api/v1/billing/batches/{id}/exports/kokuho",
    "現在の応答 — UnsupportedKokuhoExportAdapter が「公式I/F仕様未提供」として拒否し、billing_exports に status=UNSUPPORTED を記録",
    "ファイルは生成されません — billing_export_files テーブルは用意済みだが未使用",
    "差し替え手順 — KokuhoExportAdapter を実装した @Component を追加し、アダプタ名で選択させる",
    "必要な入手物 — docs/open-items.md に、実装再開に必要な公式資料をチェックリスト化",
  ],
};

const securityFeatured: FeaturedBlock = {
  badge: "Security",
  title: "認証・ロール・監査ログ",
  body:
    "JWT（HS256）によるステートレス認証です。ログインで得たトークンをブラウザの localStorage（キー: dc_access_token）に保持し、以降のリクエストは Bearer で送ります。権限はロールベースで、コントローラのメソッドに @PreAuthorize を付与しています。",
  variant: "pipeline",
  items: [
    "ADMIN（管理者）— 全操作",
    "BILLING_MANAGER（請求管理者）— 確定・差戻し・出力まで実行可",
    "BILLING_OPERATOR（請求担当）— 登録・取込・計算・検証まで（確定は不可）",
    "VIEWER（閲覧者）— 参照のみ",
    "デモユーザー — admin / password123（ADMIN）· billing / password123（BILLING_OPERATOR）",
    "監査 — AuditService が登録・更新・取込・バッチ操作を audit_logs に記録（閲覧 API・画面は未実装）",
    "本番前必須 — JWT_SECRET を 32 バイト以上のランダム値に変更し、デモユーザーを削除すること",
  ],
};

const deployFeatured: FeaturedBlock = {
  badge: "Deploy",
  title: "ローカル開発と Railway デプロイ",
  body:
    "ローカルは PostgreSQL のみ Docker で起動し、バックエンド・フロントエンドはホストで直接動かす構成が最速です。ホストポート 5433 を使うのは、他案件の 5432 と衝突させないためです。Railway は1サービス1コンテナのため、ルートの Dockerfile で backend と frontend を同一イメージに同梱し、start.sh が両プロセスを起動します。",
  variant: "deploy",
  items: [
    "DB — docker compose up -d db（ホスト 5433 → コンテナ 5432、DB 名 disability_claim）",
    "Backend — cd backend && .\\gradlew.bat bootRun（:8080）",
    "Frontend — cd frontend && npm install && npm run dev（:3000）",
    "テスト — cd backend && .\\gradlew.bat test（Testcontainers 利用テストは Docker 未起動時は自動スキップ）",
    "Railway — ルート Dockerfile + start.sh。公開ポートは Next.js、Spring Boot は同一コンテナ内 127.0.0.1:8080",
    "Railway 変数 — PostgreSQL プラグイン追加後、DATABASE_URL と JWT_SECRET を設定（start.sh が postgres:// を JDBC へ変換）",
  ],
};

const techStack = [
  "Java 21 · Spring Boot 3.5",
  "Spring Security（JWT / HS256・ステートレス）",
  "Spring Data JPA · Hibernate",
  "Flyway（V1〜V6・25テーブル）",
  "PostgreSQL 16（Docker、ホスト 5433）",
  "Apache POI 5.4（Excel .xlsx 取込）",
  "JJWT 0.12.6",
  "Next.js 15 · React 19",
  "TypeScript · CSS Modules",
  "Next.js Route Handler による BFF プロキシ",
  "JUnit 5 · Testcontainers（PostgreSQL）",
  "Gradle Wrapper",
  "Docker Compose",
  "Railway（単一コンテナ統合デプロイ）",
] as const;

const archDiagram = `Browser（相談支援専門員 / 請求担当）
    │ HTTPS（同一オリジンのみ）
    ▼
Next.js :3000
    ├─ /login /  /beneficiaries /beneficiaries/[id]
    ├─ /imports  /billing  /billing/[batchId]
    ├─ /api/v1/[...path]  ← Route Handler プロキシ（Bearer をそのまま中継）
    │        └─ 到達不可なら 503 + 原因JSON（素の500にしない）
    ├─ /api/diag          ← バックエンド疎通と環境変数の設定有無
    └─ /actuator/*        ← rewrites でバックエンドへ
              │
              ▼
Spring Boot :8080（JWT 認証・@PreAuthorize によるロール制御）
    ├─ AuthController          /api/v1/auth/login
    ├─ BeneficiaryController   /api/v1/beneficiaries（+ /{id}/certificates）
    ├─ ImportController        /api/v1/imports/excel
    └─ BillingController       /api/v1/billing
             ├─ /candidates            月次の請求対象抽出
             ├─ /batches               バッチ作成（DRAFT）
             ├─ /batches/{id}/calculate → CalculationEngine
             │        └─ BASE / ADDITION / REDUCTION / CASE_LOAD_REDUCTION
             │           単価未設定なら NEEDS_RULE_DATA で停止（推測しない）
             ├─ /batches/{id}/validate  → ValidationEngine（8ルール）
             │        └─ ERROR が1件でもあれば確定・出力をブロック
             ├─ /batches/{id}/confirm   → CONFIRMED
             └─ /batches/{id}/exports/kokuho
                      └─ UnsupportedKokuhoExportAdapter
                         「公式I/F仕様未提供」→ status=UNSUPPORTED（ファイル生成なし）
              │
              ▼
PostgreSQL 16（Flyway V1〜V6）
    ├─ 認証・組織 — users / roles / office_profiles / staff / audit_logs
    ├─ 利用者    — beneficiaries / recipient_certificates / certificate_service_details
    ├─ 実施記録  — support_activities
    ├─ 取込      — import_jobs / import_staging_rows
    ├─ 請求      — billing_batches / billing_cases / billing_case_items
    │              billing_calculation_traces / billing_validations
    │              billing_exports / billing_export_files / billing_returns
    └─ 制度マスタ — fee_rule_sets / fee_rules / service_code_masters
                   addition_masters / reduction_rules / unit_price_masters
                   ★ 現在は PENDING_OFFICIAL_SPEC（単位数・金額は NULL）

バッチ状態遷移:
DRAFT → CALCULATED → VALIDATED → CONFIRMED → EXPORTED
   ▲                                  │
   └────── reopen ────────────────────┘`;

type GuideSection = {
  label: string;
  steps: readonly GuideStep[];
};

const guideSections: readonly GuideSection[] = [
  {
    label: "クイックスタート",
    steps: [
      {
        title: "パネル操作・画面遷移",
        body: "本パネルは全画面で表示されます。PC ではヘッダーをドラッグして位置を変更でき、▼▲ で折りたたみ可能です。",
        items: [
          "PC — ヘッダーをドラッグで移動 · ▼▲ で開閉 · 位置と開閉状態はブラウザに自動保存",
          "ナビ — ダッシュボード · 利用者 · Excel移行 · 月次請求",
          "推奨フロー — ログイン → 利用者登録 → 受給者証登録 → 月次請求（候補抽出 → 計算 → 検証）",
        ],
      },
      {
        title: "起動（ローカル）",
        body: "DB のみ Docker、バックエンドとフロントエンドはホストで起動するのが最も速い構成です。",
        items: [
          "① DB — docker compose up -d db（ホストポート 5433。他案件の 5432 と衝突させないため）",
          "② Backend — cd backend && .\\gradlew.bat bootRun（起動時に Flyway が V1〜V6 を適用）",
          "③ Frontend — cd frontend && npm install && npm run dev",
          "④ 画面 — http://localhost:3000 · 疎通確認 — http://localhost:3000/api/diag",
        ],
      },
      {
        title: "ログイン（/login）",
        body: "JWT を取得して localStorage（キー dc_access_token）に保存します。トークンが無い状態で保護画面を開くと自動的にこの画面へ戻されます。",
        items: [
          "admin / password123 — ADMIN（確定・出力まで全操作可）",
          "billing / password123 — BILLING_OPERATOR（登録・取込・計算・検証まで。確定は不可）",
          "有効期限 — 既定 24 時間（JWT_EXPIRATION_MS で変更可）",
          "本番前必須 — デモユーザーの削除と JWT_SECRET の変更",
        ],
      },
    ],
  },
  {
    label: "業務フロー（請求までの通し手順）",
    steps: [
      {
        title: "① 利用者を登録する（/beneficiaries）",
        body: "請求の起点です。障害者（ADULT・計画相談支援）と障害児（CHILD・障害児相談支援）を区分で分けます。",
        items: [
          "所属事業所・区分・市町村を指定して登録",
          "市町村は municipalities マスタから選択（デモコード 999001 / 999002 を投入済み）",
          "氏名は必須にしていません — 匿名コード中心の運用が可能",
          "既存台帳がある場合は個別登録ではなく Excel 移行（次項）を使用",
        ],
      },
      {
        title: "② 受給者証を登録する（/beneficiaries/[id]）",
        body: "受給者証の有効期間が請求可否を決めます。同一利用者で期間が重複する登録は登録時点で拒否されます。",
        items: [
          "受給者証番号・有効期間（valid_from / valid_to）・サービス種別を登録",
          "期間重複は BeneficiaryService.assertNoOverlap() がエラーにする（二重計上の予防）",
          "支給決定量は certificate_service_details に明細として保持",
          "請求月が有効期間外だと、後の検証で CERTIFICATE_EXPIRED / PERIOD_MISMATCH になる",
        ],
      },
      {
        title: "③ 実施記録を登録する（support_activities）",
        body: "「その月に支援を実施した事実」が無ければ請求対象になりません。これは返戻理由として頻出するため、抽出条件そのものに組み込んでいます。",
        items: [
          "活動種別・実施日・請求対象月（billing_month）を記録",
          "候補抽出の必須条件 — 当月に有効な受給者証があること かつ 当月の実施記録があること",
          "注意 — 実施記録の登録 API / 画面は本 MVP では未実装。現状は DB へ直接投入するか Excel 移行の拡張が必要",
        ],
      },
      {
        title: "④ 請求対象を抽出する（/billing）",
        body: "請求月（YYYY-MM）と事業所を指定して候補を一覧化します。ここで想定件数と合っているかを必ず目視確認してください。",
        items: [
          "GET /api/v1/billing/candidates?month=YYYY-MM&officeId=...",
          "件数が想定より少ない — 受給者証の有効期間切れ、または当月の実施記録が無い",
          "件数が想定より多い — 終了済み利用者の状態更新漏れ、受給者証の期間重複を疑う",
        ],
      },
      {
        title: "⑤ バッチを作成して計算する",
        body: "抽出結果をバッチ（DRAFT）として固定し、報酬計算を実行します。",
        items: [
          "POST /api/v1/billing/batches（officeId, month）→ DRAFT",
          "POST /api/v1/billing/batches/{id}/calculate → CALCULATED",
          "各ケースに BASE / ADDITION / REDUCTION / CASE_LOAD_REDUCTION の明細が積み上がる",
          "計算過程は billing_calculation_traces に保存され、/billing/[batchId] で展開表示できる",
          "現状 — 報酬マスタが未確定のため、多くのケースが NEEDS_RULE_DATA になるのが正常動作",
        ],
      },
      {
        title: "⑥ 検証して確定する",
        body: "確定前に返戻要因を機械的に洗い出します。ERROR が1件でも残っていると確定できません。",
        items: [
          "POST /api/v1/billing/batches/{id}/validate → VALIDATED",
          "GET /api/v1/billing/batches/{id}/validations で rule_code / severity / message を確認",
          "ERROR — 原因データ（受給者証・実施記録・マスタ）を直して再計算・再検証",
          "WARNING — 進行は止まらないが、重複請求の疑いなどは必ず目視確認",
          "POST /api/v1/billing/batches/{id}/confirm → CONFIRMED（ADMIN / BILLING_MANAGER のみ）",
          "誤って確定した場合 — POST /api/v1/billing/batches/{id}/reopen で DRAFT に戻す",
        ],
      },
      {
        title: "⑦ 国保連向けに出力する（現在は未提供）",
        body: "公式インタフェース仕様書が未入手のため、意図的にファイルを生成しません。推測したフォーマットで出力すると、返戻どころか取込自体が失敗するためです。",
        items: [
          "POST /api/v1/billing/batches/{id}/exports/kokuho を実行すると「公式I/F仕様未提供」で拒否される",
          "billing_exports に status=UNSUPPORTED として記録は残る",
          "実装再開に必要な資料は docs/open-items.md のチェックリストを参照",
        ],
      },
    ],
  },
  {
    label: "Excel 初期移行の手順（/imports）",
    steps: [
      {
        title: "① ファイルを準備する",
        body: "既存台帳の .xlsx を、日本語ヘッダー行を持つ表形式で用意します。列の順序は問いません（ヘッダー名で対応付けます）。",
        items: [
          "対応ヘッダー — 姓 / 名 / 区分 / 受給者番号 / 市町村コード など",
          "列定義の詳細 — docs/excel-import.md を参照",
          "対象 — 利用者シートのみ（受給者証・実施記録シートは様式定義のみで未実装）",
        ],
      },
      {
        title: "② アップロードして検証結果を見る",
        body: "アップロード直後は確定登録されません。まず生データのまま import_staging_rows へ取り込み、検証結果を返します。",
        items: [
          "POST /api/v1/imports/excel（multipart: file, officeId, allowPartial）",
          "エラーは行番号・列名・理由の3点で表示されるため、Excel 側を直接修正できる",
          "import_jobs に総行数・有効行数・確定有無が残る",
        ],
      },
      {
        title: "③ 確定登録する",
        body: "既定では「全行が有効なときのみ」確定します。部分取込は明示的に指定した場合だけです。",
        items: [
          "allowPartial=false（既定）— 1行でもエラーがあれば1件も登録しない",
          "allowPartial=true — 有効行のみ登録し、エラー行はステージングに残す",
          "確定後の内容は /beneficiaries で確認",
        ],
      },
    ],
  },
  {
    label: "権限とロール",
    steps: [
      {
        title: "ロール別にできること",
        body: "コントローラのメソッド単位で @PreAuthorize を付けています。確定・差戻し・出力は管理者権限が必要です。",
        items: [
          "ADMIN — 全操作",
          "BILLING_MANAGER — 参照・登録・取込・計算・検証・確定・差戻し・出力",
          "BILLING_OPERATOR — 参照・登録・取込・計算・検証まで（確定 / 差戻し / 出力は 403）",
          "VIEWER — 参照のみ",
          "403 が返る場合は権限不足です。ログインユーザーのロールを確認してください",
        ],
      },
      {
        title: "監査ログ",
        body: "誰が何をしたかを audit_logs に記録します。請求業務では操作の追跡可能性が求められるためです。",
        items: [
          "記録対象 — 利用者の登録・更新、Excel 取込、バッチ操作",
          "項目 — action / entity_type / entity_id / actor_username",
          "現状 — 閲覧用の API・画面は未実装（DB を直接参照）",
        ],
      },
    ],
  },
  {
    label: "デプロイ（Railway）",
    steps: [
      {
        title: "① 単一コンテナ構成の理由",
        body: "Railway は1サービス＝1コンテナのため、ルートの Dockerfile で backend の JAR と frontend のビルド成果物を同一イメージに同梱し、start.sh が両方を起動します。",
        items: [
          "公開されるのは Next.js（$PORT）のみ。Spring Boot は 127.0.0.1:8080 で内部待ち受け",
          "start.sh のウォッチドッグ — バックエンドが落ちてもコンテナ全体は落とさず再起動を試みる",
          "ヘルスチェック — railway.toml で /login を指定",
        ],
      },
      {
        title: "② 必要な環境変数",
        body: "PostgreSQL プラグインを追加すると DATABASE_URL が自動で入ります。start.sh が postgres:// 形式を JDBC 形式へ変換し、ユーザー名・パスワードを分離して渡します。",
        items: [
          "DATABASE_URL — Railway PostgreSQL プラグインが自動設定",
          "JWT_SECRET — 32 バイト以上のランダム値（必須。既定値のままにしない）",
          "SERVER_PORT / PORT — 通常は変更不要",
          "確認 — デプロイ後に /api/diag を開き、backendStatus と envConfigured を見る",
        ],
      },
    ],
  },
  {
    label: "制限事項（必ず読む）",
    steps: [
      {
        title: "金額が出ないのは仕様です",
        body: "報酬告示に基づく公式の単位数・単価が未入手のため、制度マスタは意図的に空（PENDING_OFFICIAL_SPEC）で出荷しています。推測値を入れれば画面は「動いて」見えますが、その数字は業務上まったく信用できません。",
        items: [
          "画面表示 — 単価未設定の項目は「制度マスタ未設定」と表示（0 円とは表示しない）",
          "計算結果 — NEEDS_RULE_DATA（ERROR）となり確定に進めない",
          "解除方法 — fee_rule_sets / fee_rules に公式告示の単位数を登録し、source_document を実際の告示名・版に更新",
          "addition_masters / reduction_rules / unit_price_masters はテーブルのみ用意し、計算エンジンからは未接続",
        ],
      },
      {
        title: "未実装の機能",
        body: "MVP のスコープ外として意図的に未着手の領域です。",
        items: [
          "国保連向けファイル生成 — 公式 I/F 仕様未入手のため未実装（アダプタの差し替え口のみ用意）",
          "実施記録（support_activities）の登録 API・画面 — テーブルのみ",
          "受給者証の追加フォーム — 詳細画面は一覧表示のみ（API は実装済み）",
          "バッチ一覧 API — 個別取得のみのため、バッチ ID を控えておく必要がある",
          "返戻記録（billing_returns）— テーブルとリポジトリのみ",
          "監査ログの閲覧 API・画面",
          "Excel 取込の受給者証・実施記録シート",
        ],
      },
      {
        title: "検証済みの範囲",
        body: "何がテストで担保されていて、何が未検証かの区別です。",
        items: [
          "自動テスト — CalculationEngineTest / ValidationEngineTest / CertificateOverlapTest / DisabilityClaimApplicationIT（Testcontainers）",
          "未カバー — REST API 統合テスト、Excel 取込、請求バッチの E2E、フロントエンドのテスト",
          "国保連出力 — いかなる公式仕様に対しても未検証（そもそも出力しない）",
          "本番利用前に — docs/open-items.md の公式資料チェックリストを必ず消し込むこと",
        ],
      },
    ],
  },
  {
    label: "よくあるエラーと対処",
    steps: [
      {
        title: "画面にエラーが出るとき",
        body: "まず /api/diag を開いて、フロントエンドとバックエンドのどちらの問題かを切り分けます。",
        items: [
          "APIエラー (503) — バックエンド未起動。/api/diag の backendError と envConfigured を確認",
          "APIエラー (401) / ログイン画面に戻される — トークン期限切れ。再ログイン",
          "APIエラー (403) — 権限不足。確定・出力は ADMIN / BILLING_MANAGER のみ",
          "APIエラー (500) — バックエンドのログを確認（DB 接続・Flyway 適用失敗が多い）",
        ],
      },
      {
        title: "起動に失敗するとき",
        body: "起動時の失敗はほぼ DB 接続かポート衝突です。",
        items: [
          "password authentication failed — docker-compose.yml の認証情報（disability / disability）と application.yml の不一致",
          "port 5432 is already allocated — 本プロジェクトはホスト 5433 を使用。他案件のコンテナと衝突していないか確認",
          "Flyway のマイグレーション失敗 — 既存ボリュームが古い場合は docker compose down -v でボリュームごと作り直す",
          "Railway で 502 — /api/diag を開き、DATABASE_URL と JWT_SECRET が設定されているかを確認",
        ],
      },
      {
        title: "請求データが期待どおりでないとき",
        body: "抽出条件と検証ルールを順に確認します。",
        items: [
          "候補に出てこない — 当月に有効な受給者証があるか、当月の実施記録があるかを確認",
          "確定ボタンが効かない — ERROR の検証結果が残っている。/billing/[batchId] で絞り込んで解消",
          "全件 NEEDS_RULE_DATA — 制度マスタ未設定の正常動作（上記「金額が出ないのは仕様です」を参照）",
          "重複請求の警告 — DUPLICATE_BILLING_SUSPECT。同一利用者・同一月のケースを目視確認",
        ],
      },
    ],
  },
];

const L = {
  title: "利用手順",
  subtitle: "Billing & Ops",
  dragHint: "ドラッグで移動",
  expand: "開く",
  collapse: "閉じる",
  heroTitle: "障害福祉サービス請求 MVP",
  heroLead:
    "計画相談支援・障害児相談支援の月次請求業務を、利用者・受給者証の管理から請求対象抽出・報酬計算・返戻防止チェック・国保連向け出力まで通しで確認できる MVP です。公式の報酬告示・国保連 I/F 仕様が未入手の項目は、推測で埋めずに「制度マスタ未設定」「公式I/F仕様未提供」として明示的に停止します。",
  stackLabel: "Tech stack",
  diagramLabel: "Service topology",
  workflowLabel: "詳細利用手順",
  scrollHint: "↓ 請求までの通し手順・Excel 移行・権限・制限事項・トラブル対処は下へ",
  footer:
    "▼▲ で開閉 · PC はヘッダーをドラッグして移動 · スマホは画面下部のボトムシート · 表示状態は自動保存されます。本パッケージは業務検証用の MVP であり、公式資料の適用と検証を経ずに実際の請求業務へ使用しないでください。",
} as const;

type SavedState = {
  x: number;
  y: number;
  expanded: boolean;
};

function defaultPosition(mobile = false) {
  if (typeof window === "undefined") return { x: 24, y: 24 };
  if (mobile || window.innerWidth < 768) {
    return { x: 8, y: Math.max(72, window.innerHeight - 72) };
  }
  const x = Math.max(16, window.innerWidth - PANEL_WIDTH - 24);
  const y = Math.max(72, window.innerHeight - 520);
  return { x, y };
}

function clampPosition(x: number, y: number, width: number, height: number) {
  const maxX = Math.max(8, window.innerWidth - width - 8);
  const maxY = Math.max(8, window.innerHeight - height - 8);
  return {
    x: Math.min(Math.max(8, x), maxX),
    y: Math.min(Math.max(8, y), maxY),
  };
}

const variantClass: Record<NonNullable<FeaturedBlock["variant"]>, string> = {
  architecture: styles.featuredArchitecture,
  rag: styles.featuredRag,
  image: styles.featuredImage,
  embed: styles.featuredEmbed,
  guard: styles.featuredGuard,
  prompt: styles.featuredPrompt,
  eval: styles.featuredEval,
  agent: styles.featuredAgent,
  deploy: styles.featuredDeploy,
  pipeline: styles.featuredPipeline,
  cortex: styles.featuredCortex,
};

function FeaturedSection({ block }: { block: FeaturedBlock }) {
  const variant = block.variant ?? "architecture";
  return (
    <section className={`${styles.featured} ${variantClass[variant]}`} aria-label={block.title}>
      <div className={styles.featuredHead}>
        <span className={styles.featuredBadge}>{block.badge}</span>
        <strong>{block.title}</strong>
      </div>
      <p>{block.body}</p>
      {block.items?.length ? (
        <ul className={styles.items}>
          {block.items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}

export function UsageGuidePanel() {
  const panelRef = useRef<HTMLDivElement>(null);
  const dragRef = useRef<{
    pointerId: number;
    startX: number;
    startY: number;
    originX: number;
    originY: number;
  } | null>(null);

  const [ready, setReady] = useState(false);
  const [expanded, setExpanded] = useState(true);
  const [pos, setPos] = useState({ x: 24, y: 24 });
  const [dragging, setDragging] = useState(false);
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const mobile = window.innerWidth < 768;
    setIsMobile(mobile);
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try {
        const parsed = JSON.parse(saved) as SavedState;
        setPos(mobile ? defaultPosition(true) : { x: parsed.x, y: parsed.y });
        setExpanded(mobile ? false : parsed.expanded);
      } catch {
        setPos(defaultPosition(mobile));
        if (mobile) setExpanded(false);
      }
    } else {
      setPos(defaultPosition(mobile));
      if (mobile) setExpanded(false);
    }
    setReady(true);
  }, []);

  useEffect(() => {
    if (!ready) return;
    const payload: SavedState = { ...pos, expanded };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
  }, [pos, expanded, ready]);

  useEffect(() => {
    if (!ready) return;
    const onResize = () => {
      const mobile = window.innerWidth < 768;
      setIsMobile(mobile);
      if (mobile) return;
      const el = panelRef.current;
      if (!el) return;
      setPos((current) => clampPosition(current.x, current.y, el.offsetWidth, el.offsetHeight));
    };
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, [ready]);

  const onHeaderPointerDown = useCallback(
    (e: React.PointerEvent<HTMLElement>) => {
      if (isMobile) return;
      if ((e.target as HTMLElement).closest("button")) return;
      dragRef.current = {
        pointerId: e.pointerId,
        startX: e.clientX,
        startY: e.clientY,
        originX: pos.x,
        originY: pos.y,
      };
      setDragging(true);
      e.currentTarget.setPointerCapture(e.pointerId);
    },
    [pos.x, pos.y, isMobile],
  );

  const onHeaderPointerMove = useCallback((e: React.PointerEvent<HTMLElement>) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== e.pointerId) return;
    const el = panelRef.current;
    const width = el?.offsetWidth ?? PANEL_WIDTH;
    const height = el?.offsetHeight ?? 120;
    setPos(
      clampPosition(
        drag.originX + (e.clientX - drag.startX),
        drag.originY + (e.clientY - drag.startY),
        width,
        height,
      ),
    );
  }, []);

  const onHeaderPointerUp = useCallback((e: React.PointerEvent<HTMLElement>) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== e.pointerId) return;
    dragRef.current = null;
    setDragging(false);
    e.currentTarget.releasePointerCapture(e.pointerId);
  }, []);

  if (!ready) return null;

  return (
    <div
      ref={panelRef}
      className={[
        styles.panel,
        expanded ? styles.expanded : styles.collapsed,
        dragging ? styles.dragging : "",
      ]
        .filter(Boolean)
        .join(" ")}
      style={isMobile ? undefined : { left: pos.x, top: pos.y, width: PANEL_WIDTH }}
      role="dialog"
      aria-label={L.title}
      aria-modal="false"
    >
      <header
        className={styles.header}
        onPointerDown={onHeaderPointerDown}
        onPointerMove={onHeaderPointerMove}
        onPointerUp={onHeaderPointerUp}
        onPointerCancel={onHeaderPointerUp}
      >
        <div className={styles.headerText}>
          <span className={styles.dragIcon} aria-hidden>
            ☰
          </span>
          <div className={styles.headerTitles}>
            <strong>{L.title}</strong>
            <span className={styles.headerSub}>{L.subtitle}</span>
          </div>
          <span className={styles.dragHint}>{L.dragHint}</span>
        </div>
        <button
          type="button"
          className={styles.toggle}
          aria-label={expanded ? L.collapse : L.expand}
          aria-expanded={expanded}
          onClick={() => setExpanded((open) => !open)}
        >
          {expanded ? "▼" : "▲"}
        </button>
      </header>

      {expanded ? (
        <div className={styles.body}>
          <div className={styles.hero}>
            <p className={styles.heroKicker}>Disability Claim</p>
            <h2 className={styles.heroTitle}>{L.heroTitle}</h2>
            <p className={styles.heroLead}>{L.heroLead}</p>
            <div className={styles.stack} aria-label={L.stackLabel}>
              {techStack.map((tag) => (
                <span key={tag} className={styles.stackPill}>
                  {tag}
                </span>
              ))}
            </div>
          </div>

          <FeaturedSection block={valueFeatured} />
          <FeaturedSection block={architectureFeatured} />

          <figure className={styles.diagram} aria-label={L.diagramLabel}>
            <figcaption>{L.diagramLabel}</figcaption>
            <pre>{archDiagram}</pre>
          </figure>

          <FeaturedSection block={beneficiaryFeatured} />
          <FeaturedSection block={importFeatured} />
          <FeaturedSection block={billingFeatured} />
          <FeaturedSection block={calculationFeatured} />
          <FeaturedSection block={validationFeatured} />
          <FeaturedSection block={exportFeatured} />
          <FeaturedSection block={securityFeatured} />
          <FeaturedSection block={deployFeatured} />

          <p className={styles.scrollHint}>{L.scrollHint}</p>
          <h3 className={styles.workflowTitle}>{L.workflowLabel}</h3>
          {guideSections.map((section) => (
            <div key={section.label} className={styles.section}>
              <p className={styles.sectionLabel}>{section.label}</p>
              <ol className={styles.steps}>
                {section.steps.map((step) => (
                  <li key={step.title}>
                    <strong>{step.title}</strong>
                    <p>{step.body}</p>
                    {step.items?.length ? (
                      <ul className={styles.items}>
                        {step.items.map((item) => (
                          <li key={item}>{item}</li>
                        ))}
                      </ul>
                    ) : null}
                  </li>
                ))}
              </ol>
            </div>
          ))}
          <p className={styles.footer}>{L.footer}</p>
        </div>
      ) : null}
    </div>
  );
}
