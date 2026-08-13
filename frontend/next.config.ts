import type { NextConfig } from "next";

// Railway 統合コンテナでは BACKEND_INTERNAL_URL=http://127.0.0.1:8080
// ローカル開発では BACKEND_URL または既定の localhost:8080
const backendUrl =
  process.env.BACKEND_INTERNAL_URL ||
  process.env.BACKEND_URL ||
  "http://localhost:8080";

// /api/v1/* は app/api/v1/[...path]/route.ts のプロキシが処理する
// （接続失敗時に原因を返すため rewrites ではなく Route Handler を使う）
const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/actuator/:path*",
        destination: `${backendUrl}/actuator/:path*`,
      },
    ];
  },
};

export default nextConfig;
