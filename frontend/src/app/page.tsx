import Link from "next/link";
import styles from "./page.module.css";

const TILES = [
  {
    href: "/beneficiaries",
    title: "利用者",
    desc: "匿名化された利用者情報の登録・参照",
  },
  {
    href: "/beneficiaries",
    title: "受給者証",
    desc: "利用者詳細から受給者証を管理",
  },
  {
    href: "/imports",
    title: "Excel移行",
    desc: "初期データの検証付きインポート",
  },
  {
    href: "/billing",
    title: "月次請求",
    desc: "候補抽出・バッチ作成・確認",
  },
  {
    href: "/billing",
    title: "監査",
    desc: "請求状態・操作履歴の確認（MVPは請求画面経由）",
  },
];

export default function DashboardPage() {
  return (
    <div>
      <h1>ダッシュボード</h1>
      <p className="muted">
        計画相談支援・障害児相談支援向け請求業務 MVP。報酬額・国保連仕様は公式資料投入後に有効化されます。
      </p>
      <div className={styles.grid}>
        {TILES.map((tile) => (
          <Link key={tile.title} href={tile.href} className={styles.tile}>
            <h2>{tile.title}</h2>
            <p>{tile.desc}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
