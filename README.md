# DisabilityClaim

障害福祉サービス（**計画相談支援・障害児相談支援**）事業所向けの請求業務システム MVP です。

利用者・受給者証管理、Excel 初期移行、月次請求対象抽出、報酬計算・検証、国保連向けデータ出力（Adapter）までを一貫して支援することを目指します。

## 重要なお断り（Disclaimer）

- **本リポジトリは公式ソフトウェアではありません。** 国・自治体・国保連が提供する公式システムや告示そのものではありません。
- **報酬単位・加算・逓減閾値・単価・国保連ファイル仕様は推測して埋め込みません。** 対象請求年月に有効な公式資料（報酬告示・留意事項・国保連/電子請求 I/F 仕様等）を供給したうえで実装・検証してください。
- 国保連への送信は、原則として本システムが生成したファイルを **国提供の取込送信システム** 経由で行います。
- 仕様未提供の状態では、Exporter は interface / DTO までとし、実ファイル出力は「国保連仕様未提供で出力不可」とします。

## スタック

| 層 | 技術 |
|----|------|
| Frontend | Next.js 15 / React 19 / TypeScript / CSS Modules |
| Backend | Java 21+ / Spring Boot 3.x（別途実装） |
| DB | PostgreSQL 16+ |
| API | REST/JSON |
| 開発Skill | `.cursor/skills/disability-consultation-billing` |

## ディレクトリ

```text
DisabilityClaim/
  frontend/     Next.js MVP 画面
  docs/         ER・計算エンジン・移行・Validation・見積り・未確定事項
  .cursor/skills/disability-consultation-billing/   開発Skill
```

## ドキュメント

| ファイル | 内容 |
|----------|------|
| [docs/ER.md](docs/ER.md) | テーブル概要・関係・状態遷移 |
| [docs/calculation-engine.md](docs/calculation-engine.md) | CalculationEngine 設計 |
| [docs/excel-import.md](docs/excel-import.md) | Excel 列論理名・ステージング |
| [docs/validation-rules.md](docs/validation-rules.md) | 検証ルールと確定ブロック |
| [docs/estimate-wbs.md](docs/estimate-wbs.md) | MVP/本番 WBS 人日見積り |
| [docs/open-items.md](docs/open-items.md) | 必要な公式資料チェックリスト |
| [frontend/README.md](frontend/README.md) | フロントの起動方法 |

## Skill 参照

開発時は Skill `disability-consultation-billing` に従うこと。

- パス: `.cursor/skills/disability-consultation-billing/SKILL.md`
- 原則: 制度値のハードコード禁止、計算の再現性、ERROR 時の出力禁止、個人情報の取扱い

## 起動方法

### フロントのみ

```bash
cd frontend
npm install
npm run dev
```

- UI: http://localhost:3000
- API リライト先: `BACKEND_URL`（既定 `http://localhost:8080`）

### Docker Compose（PostgreSQL）

5432 は他プロジェクトで使われがちなので、本アプリの Postgres は **5433** です。

```bash
docker compose up -d db
```

| サービス | ポート | 認証 |
|----------|--------|------|
| postgres (`db`) | **5433** | `disability` / `disability` / `disability_claim` |
| backend (`bootRun`) | 8080 | — |
| frontend | 3000 | — |

```bash
cd backend
.\gradlew.bat bootRun
```

### Railway（単一サービス）

リポジトリ直下の `Dockerfile` / `railway.toml` / `start.sh` で、Spring Boot + Next.js を同一コンテナで起動します。
公開ポートは Next.js（`PORT`）、backend は内部 `8080`。`/api`・`/actuator` は Next がプロキシします。

必要な Variables 例（Postgres プラグイン参照）:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
JWT_SECRET=（本番用ランダム値）
```

停止:

```bash
docker compose down
```

> フロントだけ開発する場合: `cd frontend && npm install && npm run dev`（API は `http://localhost:8080` へリライト）

## 画面一覧（MVP）

- `/login` — ログイン
- `/` — ダッシュボード（利用者 / 受給者証 / Excel移行 / 月次請求 / 監査）
- `/beneficiaries` — 利用者一覧・登録
- `/beneficiaries/[id]` — 詳細・受給者証
- `/imports` — Excel アップロードと検証エラー
- `/billing` — 月次候補・バッチ作成
- `/billing/[batchId]` — フィルタ・計算トレース・検証/確定/出力

単位・金額が未設定の場合、画面には **「制度マスタ未設定」** と表示します。
