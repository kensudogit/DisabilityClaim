import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import TestReportsPage from "./page";

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

describe("TestReportsPage", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("renders hub with manifest summary", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      json: async () => ({
        generatedAt: "2026-08-13T09:00:00.000Z",
        backend: {
          surefireIndex: "/qa-reports/backend/surefire/index.html",
          jacocoIndex: "/qa-reports/backend/jacoco/index.html",
          summary: { tests: 59, failures: 0, errors: 0, skipped: 0 },
        },
        frontend: {
          vitestIndex: "/qa-reports/frontend/vitest/index.html",
          coverageIndex: "/qa-reports/frontend/coverage/index.html",
          summary: { numTotalTests: 13, numPassedTests: 13, numFailedTests: 0 },
        },
      }),
    });
    render(<TestReportsPage />);
    expect(screen.getByRole("heading", { name: "テスト結果" })).toBeInTheDocument();
    expect(await screen.findByText(/tests: 59/)).toBeInTheDocument();
    expect(screen.getByText(/passed: 13/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Surefire/ })).toHaveAttribute(
      "href",
      "/qa-reports/backend/surefire/index.html",
    );
    expect(fetch).toHaveBeenCalledWith("/qa-reports/manifest.json", expect.any(Object));
  });

  it("shows guidance when manifest is missing", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: false,
      status: 404,
    });
    render(<TestReportsPage />);
    expect(
      await screen.findByText(/レポートがまだ生成されていません/),
    ).toBeInTheDocument();
  });
});
