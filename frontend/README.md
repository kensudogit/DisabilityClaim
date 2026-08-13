# Disability Claim Frontend (MVP)

障害福祉サービス（計画相談支援・障害児相談支援）向け請求業務システムのフロントエンドです。

## スタック

- Next.js 15 (App Router)
- React 19
- TypeScript
- CSS Modules（UIライブラリなし）

## セットアップ

```bash
cd frontend
npm install
npm run dev
```

ブラウザで [http://localhost:3000](http://localhost:3000) を開きます。

## 環境変数

| 変数 | 説明 | 既定値 |
|------|------|--------|
| `BACKEND_URL` | Spring Boot API のベースURL | `http://localhost:8080` |

`next.config.ts` で `/api/*` を `${BACKEND_URL}/api/*` へリライトします。

## 画面

| パス | 内容 |
|------|------|
| `/login` | メール/パスワードログイン → `POST /api/v1/auth/login` |
| `/` | ダッシュボード |
| `/beneficiaries` | 利用者一覧・登録 |
| `/beneficiaries/[id]` | 利用者詳細・受給者証 |
| `/imports` | Excel移行（行/列/理由の検証エラー表示） |
| `/billing` | 月次候補抽出・バッチ作成 |
| `/billing/[batchId]` | ケース一覧・フィルタ・検証/確定/出力 |

## 認証

JWT は `localStorage`（キー: `dc_access_token`）に保存し、`src/lib/api.ts` が `Authorization: Bearer` を付与します。

## 表示方針

- 単位・金額が `null` の場合は **「制度マスタ未設定」** を表示します（偽の報酬額は出しません）。
- 国保連出力は仕様未提供のため、画面上で **「国保連仕様未提供で出力不可」** を明示します。

## 注意

本フロントは MVP 用です。公式の報酬告示・国保連I/F仕様が投入されるまで、計算結果の金額表示・ファイル出力は完成扱いしません。
