"use client";

import Link from "next/link";
import { FormEvent, useCallback, useState } from "react";
import { api } from "@/lib/api";
import type { BillingBatch, BillingCandidate } from "@/lib/types";
import styles from "./billing.module.css";

function currentMonth(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
}

export default function BillingPage() {
  const [month, setMonth] = useState(currentMonth());
  const [candidates, setCandidates] = useState<BillingCandidate[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [creating, setCreating] = useState(false);
  const [createdBatch, setCreatedBatch] = useState<BillingBatch | null>(null);

  const loadCandidates = useCallback(async () => {
    setLoading(true);
    setError(null);
    setMessage(null);
    setCreatedBatch(null);
    try {
      const data = await api<BillingCandidate[] | { items: BillingCandidate[] }>(
        `/api/v1/billing/candidates?month=${encodeURIComponent(month)}`,
      );
      const items = Array.isArray(data) ? data : data.items ?? [];
      setCandidates(items);
      setSelected(new Set(items.map((c) => c.beneficiaryId)));
    } catch (err) {
      setError(err instanceof Error ? err.message : "候補の取得に失敗しました");
      setCandidates([]);
    } finally {
      setLoading(false);
    }
  }, [month]);

  async function onCreateBatch(e: FormEvent) {
    e.preventDefault();
    if (selected.size === 0) {
      setError("請求対象を1件以上選択してください");
      return;
    }
    setCreating(true);
    setError(null);
    setMessage(null);
    try {
      const batch = await api<BillingBatch>("/api/v1/billing/batches", {
        method: "POST",
        body: {
          billingMonth: month,
          beneficiaryIds: Array.from(selected),
        },
      });
      setCreatedBatch(batch);
      setMessage(`請求バッチを作成しました（状態: ${batch.status}）`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "バッチ作成に失敗しました");
    } finally {
      setCreating(false);
    }
  }

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleAll(checked: boolean) {
    if (checked) {
      setSelected(new Set(candidates.map((c) => c.beneficiaryId)));
    } else {
      setSelected(new Set());
    }
  }

  return (
    <div>
      <h1>月次請求</h1>
      <p className="muted">請求年月を指定して候補を抽出し、請求バッチを作成します。</p>
      {error && <div className="errorBox">{error}</div>}
      {message && <div className="infoBox">{message}</div>}

      <section className={`card ${styles.section}`}>
        <h2>請求年月</h2>
        <div className={styles.row}>
          <div className="field">
            <label htmlFor="month">対象月</label>
            <input
              id="month"
              type="month"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
            />
          </div>
          <button className="btn" type="button" onClick={() => void loadCandidates()} disabled={loading}>
            {loading ? "抽出中…" : "候補を抽出"}
          </button>
        </div>
      </section>

      <section className={`card ${styles.section}`}>
        <h2>候補一覧</h2>
        {candidates.length === 0 ? (
          <p className="muted">候補がありません。年月を指定して抽出してください。</p>
        ) : (
          <form onSubmit={onCreateBatch}>
            <table className="table">
              <thead>
                <tr>
                  <th>
                    <input
                      type="checkbox"
                      checked={selected.size === candidates.length && candidates.length > 0}
                      onChange={(e) => toggleAll(e.target.checked)}
                      aria-label="全選択"
                    />
                  </th>
                  <th>匿名コード</th>
                  <th>区分</th>
                  <th>市町村</th>
                  <th>実施記録</th>
                  <th>既請求</th>
                </tr>
              </thead>
              <tbody>
                {candidates.map((c) => (
                  <tr key={c.beneficiaryId}>
                    <td>
                      <input
                        type="checkbox"
                        checked={selected.has(c.beneficiaryId)}
                        onChange={() => toggle(c.beneficiaryId)}
                        aria-label={c.anonymizedCode}
                      />
                    </td>
                    <td>{c.anonymizedCode}</td>
                    <td>{c.category === "CHILD" ? "障害児" : "障害者"}</td>
                    <td>
                      {c.municipalityName
                        ? `${c.municipalityName} (${c.municipalityCode})`
                        : c.municipalityCode}
                    </td>
                    <td>{c.activityCount ?? "—"}</td>
                    <td>{c.alreadyBilled ? "あり" : "なし"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className={styles.actions}>
              <button className="btn" type="submit" disabled={creating}>
                {creating ? "作成中…" : "請求バッチを作成"}
              </button>
              {createdBatch && (
                <Link href={`/billing/${createdBatch.id}`} className="btn btnSecondary">
                  バッチ詳細へ
                </Link>
              )}
            </div>
          </form>
        )}
      </section>
    </div>
  );
}
