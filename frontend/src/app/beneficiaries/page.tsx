"use client";

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { api } from "@/lib/api";
import type { Beneficiary, BeneficiaryCategory, BeneficiaryStatus } from "@/lib/types";
import styles from "./beneficiaries.module.css";

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "利用中",
  INACTIVE: "終了",
  CLOSED: "終了",
  SUSPENDED: "休止",
};

const CATEGORY_LABEL: Record<BeneficiaryCategory, string> = {
  ADULT: "障害者（成人）",
  CHILD: "障害児",
};

export default function BeneficiariesPage() {
  const [list, setList] = useState<Beneficiary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [query, setQuery] = useState("");
  const [submittedQuery, setSubmittedQuery] = useState("");

  const [anonymizedCode, setAnonymizedCode] = useState("");
  const [category, setCategory] = useState<BeneficiaryCategory>("ADULT");
  const [municipalityCode, setMunicipalityCode] = useState("999001");
  const [status, setStatus] = useState<BeneficiaryStatus>("ACTIVE");

  const load = useCallback(async (q: string) => {
    setLoading(true);
    setError(null);
    try {
      const path =
        q.trim().length > 0
          ? `/api/v1/beneficiaries?q=${encodeURIComponent(q.trim())}`
          : "/api/v1/beneficiaries";
      const data = await api<Beneficiary[] | { items: Beneficiary[] }>(path);
      setList(Array.isArray(data) ? data : data.items ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "取得に失敗しました");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load("");
  }, [load]);

  async function onSearch(e: FormEvent) {
    e.preventDefault();
    setSubmittedQuery(query);
    await load(query);
  }

  async function onCreate(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api("/api/v1/beneficiaries", {
        method: "POST",
        body: {
          anonymizedCode,
          category,
          municipalityCode,
          status: status === "INACTIVE" ? "CLOSED" : status,
          statusCode: status,
        },
      });
      setAnonymizedCode("");
      setMunicipalityCode("999001");
      setCategory("ADULT");
      setStatus("ACTIVE");
      setSubmittedQuery("");
      setQuery("");
      await load("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "登録に失敗しました");
    } finally {
      setSaving(false);
    }
  }

  const resultLabel = useMemo(() => {
    if (submittedQuery) {
      return `検索結果: 「${submittedQuery}」（${list.length}件）`;
    }
    return `一覧（${list.length}件）`;
  }, [submittedQuery, list.length]);

  return (
    <div>
      <h1>利用者</h1>
      <p className="muted">個人を特定できる氏名等は扱わず、匿名コードで管理します。</p>
      {error && <div className="errorBox">{error}</div>}

      <section className={`card ${styles.section}`}>
        <h2>検索</h2>
        <form className={styles.form} onSubmit={onSearch}>
          <div className="field">
            <label htmlFor="q">キーワード</label>
            <input
              id="q"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="匿名コード / 市町村コード / 区分 / 状態"
            />
          </div>
          <button className="btn" type="submit" disabled={loading}>
            検索
          </button>
          <button
            className="btn btnSecondary"
            type="button"
            disabled={loading}
            onClick={() => {
              setQuery("");
              setSubmittedQuery("");
              void load("");
            }}
          >
            クリア
          </button>
        </form>
      </section>

      <section className={`card ${styles.section}`}>
        <h2>新規登録</h2>
        <form className={styles.form} onSubmit={onCreate}>
          <div className="field">
            <label htmlFor="code">匿名コード</label>
            <input
              id="code"
              required
              value={anonymizedCode}
              onChange={(e) => setAnonymizedCode(e.target.value)}
              placeholder="例: U-0001"
            />
          </div>
          <div className="field">
            <label htmlFor="category">区分</label>
            <select
              id="category"
              value={category}
              onChange={(e) => setCategory(e.target.value as BeneficiaryCategory)}
            >
              <option value="ADULT">障害者（成人）</option>
              <option value="CHILD">障害児</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="muni">市町村コード</label>
            <input
              id="muni"
              required
              value={municipalityCode}
              onChange={(e) => setMunicipalityCode(e.target.value)}
              placeholder="例: 999001"
            />
          </div>
          <div className="field">
            <label htmlFor="status">状態</label>
            <select
              id="status"
              value={status}
              onChange={(e) => setStatus(e.target.value as BeneficiaryStatus)}
            >
              <option value="ACTIVE">利用中</option>
              <option value="SUSPENDED">休止</option>
              <option value="INACTIVE">終了</option>
            </select>
          </div>
          <button className="btn" type="submit" disabled={saving}>
            {saving ? "登録中…" : "登録"}
          </button>
        </form>
      </section>

      <section className={`card ${styles.section}`}>
        <h2>{resultLabel}</h2>
        {loading ? (
          <p className="muted">読み込み中…</p>
        ) : list.length === 0 ? (
          <p className="muted">
            {submittedQuery ? "該当する利用者がいません。" : "利用者がまだいません。"}
          </p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>匿名コード</th>
                <th>区分</th>
                <th>市町村</th>
                <th>状態</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {list.map((b) => (
                <tr key={b.id}>
                  <td>{b.anonymizedCode}</td>
                  <td>{CATEGORY_LABEL[b.category] ?? b.category}</td>
                  <td>
                    {b.municipalityName
                      ? `${b.municipalityName} (${b.municipalityCode})`
                      : b.municipalityCode}
                  </td>
                  <td>
                    <span className="badge">{STATUS_LABEL[b.status] ?? b.status}</span>
                  </td>
                  <td>
                    <Link href={`/beneficiaries/${b.id}`}>詳細</Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
