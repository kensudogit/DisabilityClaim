# Excel 初期移行

## 目的

現行 Excel からの転記を廃止するため、列マッピング定義に従いステージングへ取り込み、検証後に本登録する。

## 列マッピングテンプレート（論理名のみ）

実列名は受領 Excel に合わせて設定する。ここでは論理項目のみ定義する（推測で公式コード値は埋めない）。

### 利用者シート（例）

| 論理名 | 必須 | 型 | 備考 |
|--------|------|-----|------|
| anonymized_code | ○ | string | 匿名コード |
| category | ○ | enum | ADULT / CHILD |
| municipality_code | ○ | string | 支給決定市町村 |
| recipient_number | △ | string | 受給者番号 |
| status | ○ | enum | ACTIVE / INACTIVE / SUSPENDED |
| start_date | △ | date | 利用開始 |
| end_date | △ | date | 利用終了 |
| staff_code | △ | string | 担当相談支援専門員 |
| address_postal_code | △ | string | 郵便番号 |
| address_line | △ | string | 住所（匿名化方針に従う） |

### 受給者証シート（例）

| 論理名 | 必須 | 型 | 備考 |
|--------|------|-----|------|
| anonymized_code | ○ | string | 利用者突合キー |
| certificate_number | ○ | string | 受給者証番号 |
| municipality_code | ○ | string | |
| valid_from | ○ | date | |
| valid_to | ○ | date | |
| service_category | ○ | string | 計画相談/障害児相談など論理区分 |
| monitoring_period_months | △ | int | モニタリング期間 |
| decision_detail | △ | string | 支給決定内容（構造化は後続） |

### 実施記録シート（例）

| 論理名 | 必須 | 型 | 備考 |
|--------|------|-----|------|
| anonymized_code | ○ | string | |
| activity_date | ○ | date | 実施日 |
| activity_type | ○ | string | サービス利用支援/継続 等（コードはマスタ） |
| staff_code | △ | string | |
| note | △ | string | |

## ステージングフロー

```text
1. POST /api/v1/imports/excel
2. import_jobs 作成
3. 行を import_staging_rows へ格納（生値保持）
4. バリデーション実行
5. エラー行は本登録対象外（行番号・列名・理由を返却）
6. 全必須検証成功分のみトランザクションで本登録
7. 移行結果レポート（件数・成功・エラー）
```

## 検証項目

| 区分 | 内容 |
|------|------|
| 必須 | 必須論理列の欠落 |
| 型 | 日付・数値・列挙のパース |
| コード | 市町村コード等がマスタに存在するか |
| 日付範囲 | valid_from ≤ valid_to、請求月との整合は本登録後ルールでも再評価 |
| 重複 | 同一匿名コード＋証番号＋期間重複など |
| 参照整合 | 受給者証の利用者キーが利用者シート/DBに存在 |

## API / UI

- API: `POST /api/v1/imports/excel`（multipart）
- UI `/imports`: アップロード後、`row` / `column` / `reason` の一覧を表示

## 注意

- 個人情報を含む実ファイルを Git にコミットしない。
- テストは匿名化サンプルのみ使用する。
