"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { clearToken, getToken } from "@/lib/api";
import styles from "./Header.module.css";

const NAV = [
  { href: "/", label: "ダッシュボード" },
  { href: "/beneficiaries", label: "利用者" },
  { href: "/imports", label: "Excel移行" },
  { href: "/billing", label: "月次請求" },
];

export function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const isLogin = pathname === "/login";
  const [loggedIn, setLoggedIn] = useState(false);

  useEffect(() => {
    setLoggedIn(!!getToken());
  }, [pathname]);

  if (isLogin) return null;

  function logout() {
    clearToken();
    setLoggedIn(false);
    router.push("/login");
  }

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link href="/" className={styles.brand}>
          障害相談請求
        </Link>
        <nav className={styles.nav}>
          {NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={
                pathname === item.href ||
                (item.href !== "/" && pathname.startsWith(item.href))
                  ? styles.active
                  : undefined
              }
            >
              {item.label}
            </Link>
          ))}
        </nav>
        <div className={styles.actions}>
          {loggedIn ? (
            <button type="button" className="btn btnSecondary" onClick={logout}>
              ログアウト
            </button>
          ) : (
            <Link href="/login" className="btn btnSecondary">
              ログイン
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}
