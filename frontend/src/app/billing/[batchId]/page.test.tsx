import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import BillingBatchPage from "./page";

const api = vi.fn();

vi.mock("next/navigation", () => ({
  useParams: () => ({ batchId: "batch-1" }),
}));

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => api(...args),
  masterOrUnset: (value: number | string | null | undefined) =>
    value === null || value === undefined || value === "" ? "制度マスタ未設定" : String(value),
}));

describe("BillingBatchPage", () => {
  beforeEach(() => {
    api.mockReset();
  });

  it("renders batch detail and unset amount label", async () => {
    api.mockResolvedValue({
      id: "batch-1",
      billingMonth: "2026-08",
      status: "CALCULATED",
      cases: [
        {
          id: "case-1",
          beneficiaryId: "b1",
          anonymizedCode: "U-0001",
          category: "ADULT",
          municipalityCode: "999001",
          billedAmount: null,
          hasError: true,
          hasWarning: false,
          calculationTrace: [{ step: "BASE", detail: "NEEDS_RULE_DATA" }],
        },
      ],
    });
    render(<BillingBatchPage />);
    expect(await screen.findByText("U-0001")).toBeInTheDocument();
    expect(screen.getAllByText("制度マスタ未設定").length).toBeGreaterThan(0);
    expect(screen.getByText("計算済")).toBeInTheDocument();
  });

  it("runs validate action", async () => {
    api
      .mockResolvedValueOnce({
        id: "batch-1",
        billingMonth: "2026-08",
        status: "CALCULATED",
        cases: [],
      })
      .mockResolvedValueOnce({
        id: "batch-1",
        billingMonth: "2026-08",
        status: "VALIDATED",
        cases: [],
      })
      .mockResolvedValueOnce({
        id: "batch-1",
        billingMonth: "2026-08",
        status: "VALIDATED",
        cases: [],
      });
    render(<BillingBatchPage />);
    expect(await screen.findByRole("heading", { name: "請求バッチ" })).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "検証" }));
    expect(api).toHaveBeenCalledWith(
      "/api/v1/billing/batches/batch-1/validate",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("shows load error", async () => {
    api.mockRejectedValue(new Error("batch not found"));
    render(<BillingBatchPage />);
    expect(await screen.findByText("batch not found")).toBeInTheDocument();
  });
});
