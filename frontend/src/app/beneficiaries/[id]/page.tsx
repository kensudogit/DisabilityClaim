"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { Beneficiary, RecipientCertificate } from "@/lib/types";
import styles from "./detail.module.css";

const CATEGORY_LABEL: Record<string, string> = {
  ADULT: "障害者（成人）",
  CHILD: "障害児",
};

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "利用中",
  INACTIVE: "終了",
  SUSPENDED: "休止",
};

export default function BeneficiaryDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;

  const [beneficiary, setBeneficiary] = useState<Beneficiary | null>(null);
  const [certificates, setCertificates] = useState<RecipientCertificate[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [b, certs] = await Promise.all([
        api<Beneficiary>(`/api/v1/beneficiaries/${id}`),
        api<RecipientCertificate[] | { items: RecipientCertificate[] }>(
          `/api/v1/beneficiaries/${id}/certificates`,
        ),
      ]);
      setBeneficiary(b);
      setCertificates(Array.isArray(certs) ? certs : certs.items ?? []);
    } catch (err) {
      setError(err instanceof Error ? err.message : "取得に失敗しました");
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  if (loading) {
    return <p className="muted">読み込み中…</p>;
  }

  if (error) {
    return (
      <div>
        <div className="errorBox">{error}</div>
        <Link href="/beneficiaries">← 一覧へ</Link>
      </div>
    );
  }

  if (!beneficiary) {
    return <p className="muted">利用者が見つかりません。</p>;
  }

  return (
    <div>
      <p>
        <Link href="/beneficiaries">← 利用者一覧</Link>
      </p>
      <h1>利用者詳細</h1>
      <p className="muted">匿名コード: {beneficiary.anonymizedCode}</p>

      <section className={`card ${styles.section}`}>
        <h2>基本情報</h2>
        <dl className={styles.dl}>
          <div>
            <dt>区分</dt>
            <dd>{CATEGORY_LABEL[beneficiary.category] ?? beneficiary.category}</dd>
          </div>
          <div>
            <dt>市町村</dt>
            <dd>
              {beneficiary.municipalityName
                ? `${beneficiary.municipalityName} (${beneficiary.municipalityCode})`
                : beneficiary.municipalityCode}
            </dd>
          </div>
          <div>
            <dt>受給者番号</dt>
            <dd>{beneficiary.recipientNumber || "—"}</dd>
          </div>
          <div>
            <dt>状態</dt>
            <dd>
              <span className="badge">
                {STATUS_LABEL[beneficiary.status] ?? beneficiary.status}
              </span>
            </dd>
          </div>
          <div>
            <dt>利用開始日</dt>
            <dd>{beneficiary.startDate || "—"}</dd>
          </div>
          <div>
            <dt>利用終了日</dt>
            <dd>{beneficiary.endDate || "—"}</dd>
          </div>
          <div>
            <dt>担当相談支援専門員</dt>
            <dd>{beneficiary.staffName || "—"}</dd>
          </div>
        </dl>
      </section>

      <section className={`card ${styles.section}`}>
        <h2>受給者証</h2>
        {certificates.length === 0 ? (
          <p className="muted">受給者証がありません。</p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>証番号</th>
                <th>市町村</th>
                <th>有効期間</th>
                <th>サービス区分</th>
                <th>モニタリング</th>
              </tr>
            </thead>
            <tbody>
              {certificates.map((c) => (
                <tr key={c.id}>
                  <td>{c.certificateNumber}</td>
                  <td>
                    {c.municipalityName
                      ? `${c.municipalityName} (${c.municipalityCode})`
                      : c.municipalityCode}
                  </td>
                  <td>
                    {c.validFrom} 〜 {c.validTo}
                  </td>
                  <td>{c.serviceCategory || "—"}</td>
                  <td>
                    {c.monitoringPeriodMonths != null
                      ? `${c.monitoringPeriodMonths}ヶ月`
                      : "—"}
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
