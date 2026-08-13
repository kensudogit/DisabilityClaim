"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { Fragment, useCallback, useEffect, useMemo, useState } from "react";
import { api, masterOrUnset } from "@/lib/api";
import type { BillingBatchDetail, BillingCase, BillingBatchStatus } from "@/lib/types";
import styles from "./batch.module.css";

const STATUS_LABEL: Record<BillingBatchStatus, string> = {
  DRAFT: "下書き",
  CALCULATED: "計算済",
  VALIDATED: "検証済",
  CONFIRMED: "確定",
  EXPORTED: "出力済",
};

type CategoryFilter = "ALL" | "ADULT" | "CHILD";
type IssueFilter = "ALL" | "ERROR" | "WARNING";

function AmountCell({ value }: { value: number | string | null | undefined }) {
  const text = masterOrUnset(value);
  if (text === "制度マスタ未設定") {
    return <span className="unset">{text}</span>;
  }
  return <>{text}</>;
}

export default function BillingBatchPage() {
  const params = useParams<{ batchId: string }>();
  const batchId = params.batchId;

  const [batch, setBatch] = useState<BillingBatchDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const [municipality, setMunicipality] = useState("");
  const [category, setCategory] = useState<CategoryFilter>("ALL");
  const [issue, setIssue] = useState<IssueFilter>("ALL");

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api<BillingBatchDetail>(`/api/v1/billing/batches/${batchId}`);
      setBatch(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "取得に失敗しました");
    } finally {
      setLoading(false);
    }
  }, [batchId]);

  useEffect(() => {
    void load();
  }, [load]);

  const municipalities = useMemo(() => {
    if (!batch) return [];
    const codes = new Map<string, string>();
    for (const c of batch.cases ?? []) {
      codes.set(c.municipalityCode, c.municipalityName || c.municipalityCode);
    }
    return Array.from(codes.entries()).sort((a, b) => a[0].localeCompare(b[0]));
  }, [batch]);

  const filteredCases: BillingCase[] = useMemo(() => {
    if (!batch?.cases) return [];
    return batch.cases.filter((c) => {
      if (municipality && c.municipalityCode !== municipality) return false;
      if (category !== "ALL" && c.category !== category) return false;
      if (issue === "ERROR" && !c.hasError) return false;
      if (issue === "WARNING" && !c.hasWarning) return false;
      return true;
    });
  }, [batch, municipality, category, issue]);

  async function runAction(
    path: string,
    successMessage: string,
    options?: { expectBlockedExport?: boolean },
  ) {
    setBusy(true);
    setError(null);
    setMessage(null);
    try {
      const res = await api<{ message?: string; status?: string } | BillingBatchDetail>(path, {
        method: "POST",
      });
      if (options?.expectBlockedExport) {
        setMessage(
          "国保連仕様未提供で出力不可。公式I/F仕様・アダプタ実装後に有効化されます。",
        );
      } else {
        setMessage(
          (typeof res === "object" && res && "message" in res && res.message) ||
            successMessage,
        );
      }
      await load();
    } catch (err) {
      const msg = err instanceof Error ? err.message : "操作に失敗しました";
      if (options?.expectBlockedExport) {
        setMessage(
          "国保連仕様未提供で出力不可。公式I/F仕様・アダプタ実装後に有効化されます。",
        );
        setError(null);
      } else {
        setError(msg);
      }
    } finally {
      setBusy(false);
    }
  }

  function toggleTrace(id: string) {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  if (loading) {
    return <p className="muted">読み込み中…</p>;
  }

  if (!batch) {
    return (
      <div>
        {error && <div className="errorBox">{error}</div>}
        <Link href="/billing">← 月次請求へ</Link>
      </div>
    );
  }

  return (
    <div>
      <p>
        <Link href="/billing">← 月次請求</Link>
      </p>
      <h1>請求バッチ</h1>
      <p className="muted">
        請求年月: {batch.billingMonth} ／ 状態:{" "}
        <span className="badge">{STATUS_LABEL[batch.status] ?? batch.status}</span> ／ ID:{" "}
        {batch.id}
      </p>

      {error && <div className="errorBox">{error}</div>}
      {message && <div className="infoBox">{message}</div>}

      <section className={`card ${styles.toolbar}`}>
        <div className={styles.filters}>
          <div className="field">
            <label htmlFor="muni">市町村</label>
            <select
              id="muni"
              value={municipality}
              onChange={(e) => setMunicipality(e.target.value)}
            >
              <option value="">すべて</option>
              {municipalities.map(([code, name]) => (
                <option key={code} value={code}>
                  {name}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="cat">区分</label>
            <select
              id="cat"
              value={category}
              onChange={(e) => setCategory(e.target.value as CategoryFilter)}
            >
              <option value="ALL">すべて</option>
              <option value="ADULT">障害者（成人）</option>
              <option value="CHILD">障害児</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="issue">エラー/警告</label>
            <select
              id="issue"
              value={issue}
              onChange={(e) => setIssue(e.target.value as IssueFilter)}
            >
              <option value="ALL">すべて</option>
              <option value="ERROR">エラーのみ</option>
              <option value="WARNING">警告のみ</option>
            </select>
          </div>
        </div>
        <div className={styles.actions}>
          <button
            className="btn btnSecondary"
            type="button"
            disabled={busy}
            onClick={() =>
              void runAction(
                `/api/v1/billing/batches/${batchId}/validate`,
                "検証を実行しました",
              )
            }
          >
            検証
          </button>
          <button
            className="btn"
            type="button"
            disabled={busy}
            onClick={() =>
              void runAction(
                `/api/v1/billing/batches/${batchId}/confirm`,
                "請求を確定しました",
              )
            }
          >
            確定
          </button>
          <button
            className="btn btnSecondary"
            type="button"
            disabled={busy}
            onClick={() =>
              void runAction(
                `/api/v1/billing/batches/${batchId}/exports/kokuho`,
                "",
                { expectBlockedExport: true },
              )
            }
          >
            国保連出力
          </button>
        </div>
      </section>

      <section className={`card ${styles.section}`}>
        <h2>
          ケース一覧（{filteredCases.length} / {batch.cases?.length ?? 0}）
        </h2>
        {filteredCases.length === 0 ? (
          <p className="muted">該当するケースがありません。</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th></th>
                <th>匿名コード</th>
                <th>区分</th>
                <th>市町村</th>
                <th>基本単位</th>
                <th>加算</th>
                <th>減算</th>
                <th>合計単位</th>
                <th>単価</th>
                <th>請求額</th>
                <th>状態</th>
              </tr>
            </thead>
            <tbody>
              {filteredCases.map((c) => (
                <Fragment key={c.id}>
                  <tr>
                    <td>
                      <button
                        type="button"
                        className={styles.expandBtn}
                        onClick={() => toggleTrace(c.id)}
                        aria-expanded={expanded.has(c.id)}
                      >
                        {expanded.has(c.id) ? "▼" : "▶"}
                      </button>
                    </td>
                    <td>{c.anonymizedCode}</td>
                    <td>{c.category === "CHILD" ? "障害児" : "障害者"}</td>
                    <td>
                      {c.municipalityName
                        ? `${c.municipalityName}`
                        : c.municipalityCode}
                    </td>
                    <td>
                      <AmountCell value={c.baseUnits} />
                    </td>
                    <td>
                      <AmountCell value={c.additionUnits} />
                    </td>
                    <td>
                      <AmountCell value={c.deductionUnits} />
                    </td>
                    <td>
                      <AmountCell value={c.totalUnits} />
                    </td>
                    <td>
                      <AmountCell value={c.unitPrice} />
                    </td>
                    <td>
                      <AmountCell value={c.billedAmount} />
                    </td>
                    <td>
                      {c.hasError && <span className="badge badgeError">ERROR</span>}{" "}
                      {c.hasWarning && (
                        <span className="badge badgeWarning">WARN</span>
                      )}
                      {!c.hasError && !c.hasWarning && (
                        <span className="badge">OK</span>
                      )}
                    </td>
                  </tr>
                  {expanded.has(c.id) && (
                    <tr className={styles.traceRow}>
                      <td colSpan={11}>
                        <div className={styles.trace}>
                          <strong>計算トレース</strong>
                          {!c.calculationTrace || c.calculationTrace.length === 0 ? (
                            <p className="muted">トレースなし（未計算、または制度Rule未適用）</p>
                          ) : (
                            <ol>
                              {c.calculationTrace.map((step, i) => (
                                <li key={i}>
                                  <span className={styles.traceStep}>{step.step}</span>
                                  {step.detail}
                                  {step.value !== undefined && step.value !== null && (
                                    <>
                                      {" — "}
                                      <AmountCell value={step.value} />
                                    </>
                                  )}
                                </li>
                              ))}
                            </ol>
                          )}
                          {c.baseServiceCode != null && (
                            <p className="muted">
                              サービスコード: {c.baseServiceCode || "制度マスタ未設定"}
                            </p>
                          )}
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
