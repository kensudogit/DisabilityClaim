# CalculationEngine 設計

## 目的

請求ケースごとに、制度 Rule Version に基づく基本報酬・加算・減算・逓減・単価換算を実行し、再現可能な計算結果とトレースを残す。

## 境界

```text
BillingService
  → CalculationEngine.calculate(input) → CalculationResult
  → 結果を billing_cases / billing_case_items / billing_calculation_traces に永続化
```

国保連ファイル生成は `KokuhoExportAdapter` の責務であり、本エンジンの範囲外。

## 入力（CalculationInput）

| 項目 | 説明 |
|------|------|
| `billingMonth` | 請求年月（YYYY-MM） |
| `beneficiarySnapshot` | 利用者区分・市町村・状態など |
| `certificateSnapshot` | 受給者証・有効期間・サービス区分 |
| `activityRecords` | 実施日・種別・担当者 |
| `officeAttributes` | 事業所体制・地域区分など |
| `staffCaseLoad` | 担当件数（逓減用） |
| `ruleVersion` | 適用する `fee_rule_sets` のバージョン（または自動解決キー） |

## 出力（CalculationResult）

| 項目 | 説明 |
|------|------|
| `baseServiceCode` | 基本サービスコード |
| `baseUnits` | 基本単位 |
| `additions` / `deductions` | 加減算明細 |
| `reductionResults` | 逓減適用結果 |
| `totalUnits` | 合計単位 |
| `unitPrice` / `conversionFactor` | 単価・換算係数 |
| `billedAmount` | 請求額 |
| `calculationTrace` | 根拠ステップ一覧 |
| `ruleSetId` / `ruleVersion` | 使用した制度バージョン |

値が解決できない場合は `null` を返し、UI は「制度マスタ未設定」を表示する（偽の金額を埋めない）。

## Strategy パターン

```text
CalculationEngine
  ├── ServiceCodeResolverStrategy
  ├── BaseFeeStrategy          （計画相談 / 障害児相談で切替）
  ├── AdditionStrategy
  ├── DeductionStrategy
  ├── CaseloadReductionStrategy
  └── UnitPriceStrategy
```

- 各 Strategy は `fee_rules` / 各マスタから条件と値を読み取る。
- `service_category`（成人/障害児）ごとに Rule Set を分離可能。
- 新制度は Rule Set 追加 + Strategy 差し替えで追随する。

## Rule バージョニング

1. 請求年月と `effective_from` / `effective_to` で Rule Set を解決する。
2. 解決結果の `source_document` / `source_version` をケースに記録する。
3. 同一入力 + 同一 Rule Version → 同一結果（再現性）。
4. 改定時は既存 Rule を上書きせず、新期間のレコードを追加する。

## ハードコードしないもの

- 基本報酬の単位数
- 加算・減算の単位・同時算定不可条件の閾値（条件定義は Rule/Validation）
- 逓減の件数閾値・適用単位
- 地域区分に応じた単価・換算係数
- サービスコード体系の断定値

これらはすべてマスタ + 公式資料トレーサビリティで管理する。

## ハードコードしてよいもの（運用ロジック）

- バッチ状態遷移のガード
- 「マスタ未解決時は null + 検証 ERROR」
- Strategy の実行順序（仕様で順序が定義されている場合は公式に従う）

## 未確定・公式資料が必要な項目（Open）

- 対象年度の報酬告示・留意事項・Q&A
- 計画相談 / 障害児相談それぞれのサービスコード表
- 加算の自動判定条件と人手確認条件の境界
- 逓減の集計単位（専門員単位/事業所単位）と並び順
- 端数処理・四捨五入ルール
- 地域区分と単価表

詳細チェックリストは [open-items.md](./open-items.md) を参照。
