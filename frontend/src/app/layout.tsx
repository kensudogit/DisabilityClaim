import type { ReactNode } from "react";
import { Header } from "@/components/Header";
import "./globals.css";

export const metadata = {
  title: "障害相談請求 MVP",
  description: "計画相談支援・障害児相談支援向け請求業務 MVP",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ja">
      <body>
        <Header />
        <main>{children}</main>
      </body>
    </html>
  );
}
