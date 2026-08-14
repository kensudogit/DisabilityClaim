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
    // 静的ファイルは /qa-reports/*（App Router の /test-reports と衝突しない）
    fetch("/qa-reports/manifest.json", { cache: "no-store" })
      .then(async (res) => {
        if (!res.ok) {
          throw new Error(
            "レポートがまだ生成されていません。ローカルでは scripts/run-tests-and-publish.ps1 を実行するか、Railway へ再デプロイ（Docker ビルド時に自動生成）してください。",
          );
        }
        return res.json() as Promise<Manifest>;
      })
      .then(setManifest)
      .catch((err: unknown) => {
        setError(err instanceof Error ? err.message : "読み込みに失敗しました");
      });
  }, []);

  const backendSurefire = manifest?.backend?.surefireIndex ?? "/qa-reports/backend/surefire/index.html";
  const backendJacoco = manifest?.backend?.jacocoIndex ?? "/qa-reports/backend/jacoco/index.html";
  const frontendVitest = manifest?.frontend?.vitestIndex ?? "/qa-reports/frontend/vitest/index.html";
  const frontendCoverage =
    manifest?.frontend?.coverageIndex ?? "/qa-reports/frontend/coverage/index.html";

  return (
    <div className={styles.page}>
      <h1>テスト結果</h1>
      <p className={styles.lead}>
        Backend（JUnit / Surefire / JaCoCo）と Frontend（Vitest）の実行結果を Web から確認できます。
        静的レポートは <code>/qa-reports/</code> に配置されます（画面 URL は{" "}
        <code>/test-reports</code>）。
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
            <a href={backendSurefire} target="_blank" rel="noreferrer">
              Surefire HTML レポートを開く
            </a>
            <a href={backendJacoco} target="_blank" rel="noreferrer">
              JaCoCo カバレッジを開く
            </a>
          </div>
          {manifest ? (
            <iframe className={styles.frame} title="backend-surefire" src={backendSurefire} />
          ) : (
            <p className={styles.meta}>レポート読み込み待ち…</p>
          )}
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
            <a href={frontendVitest} target="_blank" rel="noreferrer">
              Vitest HTML レポートを開く
            </a>
            <a href={frontendCoverage} target="_blank" rel="noreferrer">
              カバレッジを開く
            </a>
          </div>
          {manifest ? (
            <iframe className={styles.frame} title="frontend-vitest" src={frontendVitest} />
          ) : (
            <p className={styles.meta}>レポート読み込み待ち…</p>
          )}
        </section>
      </div>

      <p className={styles.footer}>
        <Link href="/">ダッシュボードへ戻る</Link>
      </p>
    </div>
  );
}
