import { defineConfig } from "vitest/config";
import path from "node:path";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.{test,spec}.{ts,tsx}", "next.config.test.ts"],
    reporters: ["default", "html", "json"],
    outputFile: {
      // App Router の /test-reports と衝突しない静的パス
      html: "./public/qa-reports/frontend/vitest/index.html",
      json: "./public/qa-reports/frontend/vitest/results.json",
    },
    coverage: {
      provider: "v8",
      reporter: ["text", "html", "json-summary"],
      reportsDirectory: "./public/qa-reports/frontend/coverage",
      include: ["src/**/*.{ts,tsx}", "next.config.ts"],
      exclude: [
        "src/**/*.{test,spec}.{ts,tsx}",
        "src/test/**",
        "src/**/*.d.ts",
      ],
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
