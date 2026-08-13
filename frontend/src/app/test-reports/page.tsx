"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import styles from "./test-reports.module.css";

type Manifest = {
  generatedAt: string;
  backend?: {
    surefireIndex: string;
    jacocoIndex: string;
    summary?: { tests?: number; failures?: number; errors?: number; skipped?: number };
  };
  frontend?: {
    vitestIndex: string;
    coverageIndex?: string;
    summary?: { numTotalTests?: number; numPassedTests?: number; numFailedTests?: number };
  };
};

export default function TestReportsPage() {
  const [manifest, setManifest] = useState<Manifest | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch("/test-reports/manifest.json", { cache: "no-store" })
      .then(async (res) => {
        if (!res.ok) {
          throw new Error(
            "レポートがまだ生成されていません。リポジトリルートで scripts/run-tests-and-publish.ps1 を実行してください。",
          );
        }
        return res.json() as Promise<Manifest>;
      })
      .then(setManifest)
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : "読み込みに失敗しました");
      });
  }, []);

  return (
    <div className={styles.page}>
      <h1>テスト結果</h1>
      <p className={styles.lead}>
        Backend（JUnit / Surefire / JaCoCo）と Frontend（Vitest）の実行結果を Web から確認できます。
        レポートは <code>scripts/run-tests-and-publish.ps1</code> 実行時に
        <code>frontend/public/test-reports/</code> へ出力されます。
      </p>

      {error && <div className={styles.error}>{error}</div>}

      {manifest && (
        <p className={styles.meta}>生成日時: {new Date(manifest.generatedAt).toLocaleString("ja-JP")}</p>
      )}

      <div className={styles.grid}>
        <section className={styles.card}>
          <h2>Backend (Java)</h2>
          {manifest?.backend?.summary && (
            <ul className={styles.stats}>
              <li>tests: {manifest.backend.summary.tests ?? "-"}</li>
              <li>failures: {manifest.backend.summary.failures ?? 0}</li>
              <li>errors: {manifest.backend.summary.errors ?? 0}</li>
              <li>skipped: {manifest.backend.summary.skipped ?? 0}</li>
            </ul>
          )}
          <div className={styles.links}>
            <a href="/test-reports/backend/surefire/index.html" target="_blank" rel="noreferrer">
              Surefire HTML レポートを開く
            </a>
            <a href="/test-reports/backend/jacoco/index.html" target="_blank" rel="noreferrer">
              JaCoCo カバレッジを開く
            </a>
          </div>
          <iframe
            className={styles.frame}
            title="backend-surefire"
            src="/test-reports/backend/surefire/index.html"
          />
        </section>

        <section className={styles.card}>
          <h2>Frontend (TypeScript / Vitest)</h2>
          {manifest?.frontend?.summary && (
            <ul className={styles.stats}>
              <li>total: {manifest.frontend.summary.numTotalTests ?? "-"}</li>
              <li>passed: {manifest.frontend.summary.numPassedTests ?? "-"}</li>
              <li>failed: {manifest.frontend.summary.numFailedTests ?? 0}</li>
            </ul>
          )}
          <div className={styles.links}>
            <a href="/test-reports/frontend/vitest/index.html" target="_blank" rel="noreferrer">
              Vitest HTML レポートを開く
            </a>
            <a href="/test-reports/frontend/coverage/index.html" target="_blank" rel="noreferrer">
              カバレッジを開く
            </a>
          </div>
          <iframe
            className={styles.frame}
            title="frontend-vitest"
            src="/test-reports/frontend/vitest/index.html"
          />
        </section>
      </div>

      <p className={styles.footer}>
        <Link href="/">ダッシュボードへ戻る</Link>
      </p>
    </div>
  );
}
