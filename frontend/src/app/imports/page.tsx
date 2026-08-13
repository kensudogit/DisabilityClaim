"use client";

import { FormEvent, useState } from "react";
import { getToken } from "@/lib/api";
import type { ImportJobResult, ImportValidationError } from "@/lib/types";
import styles from "./imports.module.css";

export default function ImportsPage() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ImportJobResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!file) {
      setError("Excelファイルを選択してください");
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const form = new FormData();
      form.append("file", file);
      const headers: HeadersInit = {};
      const token = getToken();
      if (token) headers.Authorization = `Bearer ${token}`;

      const res = await fetch("/api/v1/imports/excel", {
        method: "POST",
        headers,
        body: form,
      });

      const text = await res.text();
      let data: unknown = null;
      if (text) {
        try {
          data = JSON.parse(text);
        } catch {
          data = text;
        }
      }

      if (!res.ok) {
        const message =
          typeof data === "object" &&
          data !== null &&
          "message" in data &&
          typeof (data as { message: unknown }).message === "string"
            ? (data as { message: string }).message
            : `アップロードに失敗しました (${res.status})`;
        throw new Error(message);
      }

      setResult(data as ImportJobResult);
    } catch (err) {
      setError(err instanceof Error ? err.message : "アップロードに失敗しました");
    } finally {
      setLoading(false);
    }
  }

  const errors: ImportValidationError[] = result?.errors ?? [];

  return (
    <div>
      <h1>Excel移行</h1>
      <p className="muted">
        ステージングへ取り込み、必須・型・重複などを検証します。エラーは行・列・理由で表示されます。
      </p>
      {error && <div className="errorBox">{error}</div>}

      <section className={`card ${styles.section}`}>
        <h2>ファイルアップロード</h2>
        <form onSubmit={onSubmit} className={styles.form}>
          <div className="field">
            <label htmlFor="file">Excelファイル（.xlsx）</label>
            <input
              id="file"
              type="file"
              accept=".xlsx,.xls"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </div>
          <button className="btn" type="submit" disabled={loading || !file}>
            {loading ? "検証中…" : "アップロードして検証"}
          </button>
        </form>
      </section>

      {result && (
        <section className={`card ${styles.section}`}>
          <h2>検証結果</h2>
          <div className={styles.summary}>
            <span>ジョブID: {result.jobId}</span>
            <span>総行数: {result.totalRows}</span>
            <span>成功: {result.successRows}</span>
            <span>エラー: {result.errorRows}</span>
          </div>

          {errors.length === 0 ? (
            <div className="infoBox">検証エラーはありません。本登録はバックエンド側で実行されます。</div>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>行</th>
                  <th>列</th>
                  <th>理由</th>
                </tr>
              </thead>
              <tbody>
                {errors.map((err, i) => (
                  <tr key={`${err.row}-${err.column}-${i}`}>
                    <td>{err.row}</td>
                    <td>{err.column}</td>
                    <td>{err.reason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </div>
  );
}
