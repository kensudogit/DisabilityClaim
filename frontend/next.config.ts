import type { NextConfig } from "next";

// Railway 統合コンテナでは BACKEND_INTERNAL_URL=http://localhost:8080
// ローカル開発では BACKEND_URL または既定の localhost:8080
const backendUrl =
  process.env.BACKEND_INTERNAL_URL ||
  process.env.BACKEND_URL ||
  "http://localhost:8080";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
      {
        source: "/actuator/:path*",
        destination: `${backendUrl}/actuator/:path*`,
      },
    ];
  },
};

export default nextConfig;
