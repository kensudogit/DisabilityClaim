import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import BillingPage from "./page";

const api = vi.fn();

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => api(...args),
}));

describe("BillingPage", () => {
  beforeEach(() => {
    api.mockReset();
  });

  it("loads candidates for selected month", async () => {
    api.mockResolvedValue([
      {
        beneficiaryId: "b1",
        anonymizedCode: "U-0001",
        category: "ADULT",
        municipalityCode: "999001",
      },
    ]);
    render(<BillingPage />);
    await userEvent.click(screen.getByRole("button", { name: /候補を抽出/ }));
    expect(await screen.findByText("U-0001")).toBeInTheDocument();
    expect(api).toHaveBeenCalledWith(
      expect.stringMatching(/^\/api\/v1\/billing\/candidates\?month=/),
    );
  });

  it("creates batch from selected candidates", async () => {
    api
      .mockResolvedValueOnce([
        {
          beneficiaryId: "b1",
          anonymizedCode: "U-0001",
          category: "ADULT",
          municipalityCode: "999001",
        },
      ])
      .mockResolvedValueOnce({
        id: "batch-1",
        billingMonth: "2026-08",
        status: "DRAFT",
      });
    render(<BillingPage />);
    await userEvent.click(screen.getByRole("button", { name: /候補を抽出/ }));
    await screen.findByText("U-0001");
    await userEvent.click(screen.getByRole("button", { name: /請求バッチを作成/ }));
    await waitFor(() => {
      expect(api).toHaveBeenCalledWith(
        "/api/v1/billing/batches",
        expect.objectContaining({
          method: "POST",
          body: expect.objectContaining({
            beneficiaryIds: ["b1"],
          }),
        }),
      );
    });
    expect(await screen.findByText(/請求バッチを作成しました/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /バッチ詳細へ/ })).toHaveAttribute(
      "href",
      "/billing/batch-1",
    );
  });

  it("shows error when candidate load fails", async () => {
    api.mockRejectedValue(new Error("候補の取得に失敗"));
    render(<BillingPage />);
    await userEvent.click(screen.getByRole("button", { name: /候補を抽出/ }));
    expect(await screen.findByText("候補の取得に失敗")).toBeInTheDocument();
  });
});
