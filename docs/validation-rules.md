# 返戻防止 Validation Rules

## Severity

| 区分 | 意味 | 確定（CONFIRMED） | 国保連出力 |
|------|------|-------------------|------------|
| ERROR | 返戻・不整合の可能性が高い | **ブロック** | **不可** |
| WARNING | 確認推奨 | 可能（確認済みフラグ推奨） | 仕様上必須でなければ可 |
| INFO | 参考情報 | 影響なし | 影響なし |

バッチに ERROR が1件でもある場合:

- `VALIDATED → CONFIRMED` を禁止
- `exports/kokuho` を禁止

## ルール一覧（MVP想定）

| ID | ルール | Severity | 確定ブロック |
|----|--------|----------|--------------|
| V-001 | 必須項目欠落（市町村・受給者番号・証期間など） | ERROR | ○ |
| V-002 | 受給者証の有効期間外（請求月と不整合） | ERROR | ○ |
| V-003 | 対象月との実施記録期間不整合 | ERROR | ○ |
| V-004 | 市町村コードがマスタに存在しない | ERROR | ○ |
| V-005 | 受給者番号形式不正（公式桁・パターン確定後） | ERROR | ○ |
| V-006 | 実施日なし（請求根拠となる activity 欠落） | ERROR | ○ |
| V-007 | 重複請求疑い（同月・同区分の既請求） | ERROR | ○ |
| V-008 | サービス種別不整合（成人/障害児と証・activity） | ERROR | ○ |
| V-009 | 加算要件不足（体制・資格履歴不足） | ERROR / WARNING | ERROR時○ |
| V-010 | 排他的加算の同時算定 | ERROR | ○ |
| V-011 | 単位・金額計算結果不整合 / 制度マスタ未解決（null） | ERROR | ○ |
| V-012 | 既送信請求の二重生成 | ERROR | ○ |
| V-013 | 担当件数逓減の集計根拠不足 | WARNING | × |
| V-014 | モニタリング周期と実施タイミングの乖離疑い | WARNING | × |
| V-015 | 住所・連絡先など任意項目の欠落 | INFO | × |

> V-005 の具体パターン、加算コード、排他組合せは公式仕様確定後にマスタ化する。未確定の値は実装に埋め込まない。

## 確定ブロック挙動

```text
validate(batch)
  → billing_validations に結果保存
  → status = VALIDATED

confirm(batch)
  if any validation.severity == ERROR for batch/cases:
    reject
  else:
    status = CONFIRMED
```

## 返戻フィードバック

`billing_returns` に返戻コードを蓄積し、Validation Rule の追加・severity見直しに利用する（本番完全版）。
